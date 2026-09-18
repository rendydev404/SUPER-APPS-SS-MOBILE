package com.sukashawarma.superapp.feature.absensi.shift

/**
 * Pilihan dua shift per outlet (toggle `pilih_shift_aktif` di Jam Khusus Cabang).
 * Shift 1 = jam_masuk/jam_keluar outlet, Shift 2 = shift2_jam_masuk/shift2_jam_keluar.
 *
 * Terjemahan langsung `apps/absensi/src/lib/attendance/shift.ts` (web) — aturannya
 * wajib identik, karena server (RPC `submit_attendance`) menegakkan ulang semuanya.
 */
data class ShiftOption(
    val ke: Int,
    /** "HH:MM" */
    val jamMasuk: String,
    /** "HH:MM" */
    val jamKeluar: String,
) {
    val rentang: String get() = "$jamMasuk – $jamKeluar"
    val nama: String get() = namaShift(jamMasuk)
}

data class ShiftConfig(
    val jamMasuk: String?,
    val jamKeluar: String?,
    val pilihShiftAktif: Boolean?,
    val shift2JamMasuk: String?,
    val shift2JamKeluar: String?,
    val shift3JamMasuk: String? = null,
    val shift3JamKeluar: String? = null,
)

private fun hhmm(t: String) = t.take(5)

/**
 * Daftar shift yang wajib dipilih crew/staf, atau null bila outlet hanya satu shift.
 * Bila role == "driver", otomatis diberikan 3 pilihan shift (termasuk 09:00–18:00).
 */
fun shiftOptions(cfg: ShiftConfig?, role: String? = null): List<ShiftOption>? {
    if (cfg?.pilihShiftAktif != true) return null
    val masuk1 = cfg.jamMasuk?.takeIf { it.isNotBlank() } ?: return null
    val keluar1 = cfg.jamKeluar?.takeIf { it.isNotBlank() } ?: return null
    val masuk2 = cfg.shift2JamMasuk?.takeIf { it.isNotBlank() } ?: return null
    val keluar2 = cfg.shift2JamKeluar?.takeIf { it.isNotBlank() } ?: return null

    val options = mutableListOf(
        ShiftOption(1, hhmm(masuk1), hhmm(keluar1)),
        ShiftOption(2, hhmm(masuk2), hhmm(keluar2)),
    )

    if (role == "driver") {
        val s3Masuk = cfg.shift3JamMasuk?.takeIf { it.isNotBlank() }?.let { hhmm(it) } ?: "09:00"
        val s3Keluar = cfg.shift3JamKeluar?.takeIf { it.isNotBlank() }?.let { hhmm(it) } ?: "18:00"
        options.add(ShiftOption(3, s3Masuk, s3Keluar))
    } else if (!cfg.shift3JamMasuk.isNullOrBlank() && !cfg.shift3JamKeluar.isNullOrBlank()) {
        options.add(ShiftOption(3, hhmm(cfg.shift3JamMasuk), hhmm(cfg.shift3JamKeluar)))
    }

    return options
}

/** Sebutan shift dari jam masuknya: Pagi (<11), Siang (<15), selain itu Malam. */
fun namaShift(jamMasuk: String): String {
    val h = jamMasuk.take(2).toIntOrNull() ?: 0
    return when {
        h < 11 -> "Shift Pagi"
        h < 15 -> "Shift Siang"
        else -> "Shift Malam"
    }
}

fun isShiftKe(v: Int?): Boolean = v == 1 || v == 2 || v == 3

private fun menit(t: String): Int = (t.take(2).toIntOrNull() ?: 0) * 60 + (t.substring(3, 5).toIntOrNull() ?: 0)

/** Menit jam pulang; shift yang pulang lewat tengah malam (keluar < masuk) dihitung hari berikutnya. */
private fun menitPulang(o: ShiftOption): Int {
    val keluar = menit(o.jamKeluar)
    return if (keluar < menit(o.jamMasuk)) keluar + 24 * 60 else keluar
}

/**
 * Apakah crew dengan jam pulang [jamKeluarShift] adalah shift PENUTUP outlet — shift
 * yang pulang paling akhir. Hanya shift penutup yang wajib menunggu laci kasir ditutup,
 * pesanan selesai, dan checklist penutupan.
 *
 * Outlet satu shift, absen tanpa jejak shift, atau jam yang tak cocok dengan shift mana
 * pun → true (aturan lama: semua yang absen pulang dianggap menutup).
 */
fun isShiftPenutup(opsi: List<ShiftOption>?, jamKeluarShift: String?): Boolean {
    if (opsi.isNullOrEmpty() || jamKeluarShift.isNullOrBlank()) return true
    val milik = opsi.firstOrNull { it.jamKeluar == hhmm(jamKeluarShift) } ?: return true
    return menitPulang(milik) == opsi.maxOf { menitPulang(it) }
}
