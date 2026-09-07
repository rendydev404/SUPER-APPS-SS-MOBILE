package com.sukashawarma.superapp.feature.manager.domain

import com.sukashawarma.superapp.domain.model.Role
import java.time.Duration
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

/**
 * Tahapan pengajuan dana operasional cabang.
 *
 * Nilainya persis seperti tersimpan di kolom `petty_cash_topups.status`. Alurnya
 * panjang karena melewati tiga meja: leader mengajukan, area manager meneruskan,
 * finance mencairkan, lalu dana diserahkan kembali ke bawah.
 */
enum class StatusTopup(val nilai: String, val label: String) {
    DIAJUKAN("pending", "Menunggu ACC"),
    DITERUSKAN_KE_AM("forwarded_to_area_manager", "Menunggu ACC"),
    DISETUJUI_FINANCE("approved_by_finance", "Siap diserahkan"),
    DITERUSKAN_FINANCE("forwarded_by_finance", "Siap diserahkan"),
    DITERUSKAN_KE_FINANCE("forwarded_to_finance", "Di Finance"),
    DISERAHKAN_AM("forwarded_by_area_manager", "Diserahkan ke Leader"),
    DISERAHKAN_LEADER("forwarded_by_leader", "Selesai"),
    SELESAI("completed", "Selesai"),
    DITOLAK("rejected", "Ditolak");

    companion object {
        fun dari(nilai: String?): StatusTopup? = entries.find { it.nilai == nilai }
    }
}

/** Status yang masih menuntut tindakan area manager — isi tab "Butuh Review". */
val STATUS_BUTUH_REVIEW: Set<StatusTopup> = setOf(
    StatusTopup.DIAJUKAN,
    StatusTopup.DITERUSKAN_KE_AM,
    StatusTopup.DISETUJUI_FINANCE,
    StatusTopup.DITERUSKAN_FINANCE,
)

/** Menunggu keputusan ACC/tolak. */
val STATUS_MENUNGGU_ACC: Set<StatusTopup> = setOf(
    StatusTopup.DIAJUKAN,
    StatusTopup.DITERUSKAN_KE_AM,
)

/** Dana sudah cair di finance dan tinggal diserahkan ke leader. */
val STATUS_SIAP_DISERAHKAN: Set<StatusTopup> = setOf(
    StatusTopup.DISETUJUI_FINANCE,
    StatusTopup.DITERUSKAN_FINANCE,
)

/** Tindakan yang tersedia pada sebuah pengajuan. */
enum class AksiTopup { ACC_ATAU_TOLAK, SERAHKAN, TIDAK_ADA }

fun aksiUntuk(status: StatusTopup): AksiTopup = when (status) {
    in STATUS_MENUNGGU_ACC -> AksiTopup.ACC_ATAU_TOLAK
    in STATUS_SIAP_DISERAHKAN -> AksiTopup.SERAHKAN
    else -> AksiTopup.TIDAK_ADA
}

/**
 * Role yang RPC `area_manager_process_petty_cash` dan `area_manager_forward_funds`
 * benar-benar terima. Disalin dari badan kedua RPC itu.
 *
 * `regional_manager` sengaja TIDAK ada di sini — database memang menolaknya. Web
 * tetap menampilkan tombolnya dan membiarkan RM menabrak galat server; native
 * memilih menonaktifkan tombol dan menjelaskan alasannya, karena galat mentah
 * "Not authorized to process..." tidak memberi tahu apa pun yang berguna.
 */
val ROLE_PEMROSES_PETTY_CASH: Set<Role> = setOf(
    Role.AREA_MANAGER,
    Role.KORLAP,
    Role.ADMIN,
    Role.ADMIN_FINANCE,
    Role.OWNER,
)

fun bolehMemprosesPettyCash(role: Role?): Boolean = role in ROLE_PEMROSES_PETTY_CASH

data class TopupPettyCash(
    val id: String,
    val outletId: String,
    val outletNama: String,
    val jumlah: Long,
    val deskripsi: String?,
    val status: StatusTopup,
    val dibuatPada: String,
    val pengajuNama: String,
    val namaBank: String?,
    val nomorRekening: String?,
    val atasNama: String?,
    val buktiTransferUrl: String?,
) {
    /** Delapan karakter pertama uuid — penanda ringkas yang dipakai web di kepala kartu. */
    val idRingkas: String get() = id.take(8)

    val rekeningTeks: String?
        get() = namaBank?.takeIf { it.isNotBlank() }?.let { bank ->
            buildString {
                append(bank)
                nomorRekening?.takeIf { it.isNotBlank() }?.let { append(" $it") }
                atasNama?.takeIf { it.isNotBlank() }?.let { append(" (a.n $it)") }
            }
        }
}

/**
 * Memisahkan catatan finance dari alasan pengajuan.
 *
 * Catatan itu ditempelkan ke kolom `description` oleh beberapa versi aplikasi yang
 * berbeda, masing-masing dengan pembungkus sendiri — emoji sematan, kurung siku,
 * kurung biasa, dan dua bentuk yang kurungnya tidak pernah ditutup. Keempat bentuk
 * diperiksa berurutan, sama seperti `parseFinanceNote` di web, karena data lama
 * tidak akan berubah bentuk hanya karena kodenya dirapikan.
 */
fun pisahCatatanFinance(deskripsi: String?): Pair<String, String?> {
    if (deskripsi.isNullOrBlank()) return "" to null

    val sematan = "📌 ["
    if (sematan in deskripsi) {
        val bagian = deskripsi.split(sematan, limit = 2)
        return bagian[0].trim() to bagian[1].removeSuffix("]").trim()
    }

    val pola = Regex("""(.*?)[\[(]Catatan Finance:\s*(.*?)[\])](.*)""", RegexOption.IGNORE_CASE)
    pola.find(deskripsi)?.let { cocok ->
        val (depan, catatan, belakang) = cocok.destructured
        return (depan + belakang).trim() to catatan.trim()
    }

    listOf("[Catatan Finance:" to "]", "(Catatan Finance:" to ")").forEach { (pembuka, penutup) ->
        if (pembuka in deskripsi) {
            val bagian = deskripsi.split(pembuka, limit = 2)
            return bagian[0].trim() to bagian[1].removeSuffix(penutup).trim()
        }
    }

    return deskripsi to null
}

/** Membersihkan awalan "Catatan Finance:" yang kadang ikut terbawa dua kali. */
fun rapikanCatatanFinance(catatan: String): String =
    catatan.replace(Regex("""^(Catatan Finance:\s*)+""", RegexOption.IGNORE_CASE), "")

enum class FilterReview(val label: String) {
    SEMUA("Semua Status"),
    BELUM_ACC("Belum di-ACC"),
    SIAP_DISERAHKAN("Siap Diserahkan"),
}

enum class FilterRiwayat(val label: String) {
    SEMUA("Semua Riwayat"),
    PROSES_FINANCE("Proses Finance"),
    SELESAI("Selesai"),
    DITOLAK("Ditolak"),
}

enum class FilterTanggal(val label: String) {
    SEMUA("Semua Tanggal"),
    HARI_INI("Hari Ini"),
    KEMARIN("Kemarin"),
    TUJUH_HARI("7 Hari Terakhir"),
    TIGA_PULUH_HARI("30 Hari Terakhir"),
}

/**
 * Apakah [tanggal] masuk penyaring [filter].
 *
 * Perhatikan batasnya: "7 Hari Terakhir" mencakup selisih 0..7 hari, jadi delapan
 * hari kalender, dan "30 Hari Terakhir" mencakup 0..30. Itu memang perilaku
 * `isDateInRange` di web dan disalin apa adanya — memperbaikinya di sini saja akan
 * membuat dua layar melaporkan jumlah pengajuan yang berbeda untuk filter yang
 * kelihatannya sama.
 */
fun dalamRentangTanggal(
    tanggal: LocalDate?,
    filter: FilterTanggal,
    hariIni: LocalDate = LocalDate.now(ZONA_JAKARTA),
): Boolean {
    if (filter == FilterTanggal.SEMUA) return true
    if (tanggal == null) return false
    val selisih = Duration.between(tanggal.atStartOfDay(), hariIni.atStartOfDay()).toDays()
    return when (filter) {
        FilterTanggal.HARI_INI -> selisih == 0L
        FilterTanggal.KEMARIN -> selisih == 1L
        FilterTanggal.TUJUH_HARI -> selisih in 0..7
        FilterTanggal.TIGA_PULUH_HARI -> selisih in 0..30
        FilterTanggal.SEMUA -> true
    }
}

/** Tanggal Jakarta sebuah cap waktu server, atau null kalau tak terbaca. */
fun tanggalJakarta(iso: String): LocalDate? = try {
    OffsetDateTime.parse(iso).atZoneSameInstant(ZONA_JAKARTA).toLocalDate()
} catch (e: DateTimeParseException) {
    null
}

/**
 * "Baru saja", "3 jam lalu", "Kemarin", "5 hari lalu" — cermin `formatRelativeTime` web.
 *
 * Selisihnya dihitung dari durasi mentah, BUKAN hari kalender. Artinya pengajuan
 * pukul 23.00 yang dilihat pukul 08.00 esok harinya tertulis "9 jam lalu", bukan
 * "Kemarin". Itu memang perilaku web; menyelaraskannya ke hari kalender di sini
 * saja akan membuat kartu yang sama berbunyi berbeda di HP dan di laptop.
 *
 * Jam perangkat yang lebih lambat dari server bisa menghasilkan durasi negatif;
 * itu dibaca sebagai "Baru saja", bukan angka minus.
 */
fun waktuRelatif(iso: String, sekarang: OffsetDateTime = OffsetDateTime.now()): String {
    val waktu = try {
        OffsetDateTime.parse(iso)
    } catch (e: DateTimeParseException) {
        return iso
    }
    val selisih = Duration.between(waktu, sekarang)
    val jam = selisih.toHours().coerceAtLeast(0)
    val hari = selisih.toDays().coerceAtLeast(0)
    return when {
        hari == 0L -> if (jam == 0L) "Baru saja" else "$jam jam lalu"
        hari == 1L -> "Kemarin"
        else -> "$hari hari lalu"
    }
}

/** Menyaring isi tab "Butuh Review". */
fun saringReview(
    daftar: List<TopupPettyCash>,
    filter: FilterReview,
    tanggal: FilterTanggal,
    hariIni: LocalDate = LocalDate.now(ZONA_JAKARTA),
): List<TopupPettyCash> = daftar
    .filter { it.status in STATUS_BUTUH_REVIEW }
    .filter {
        when (filter) {
            FilterReview.SEMUA -> true
            FilterReview.BELUM_ACC -> it.status in STATUS_MENUNGGU_ACC
            FilterReview.SIAP_DISERAHKAN -> it.status in STATUS_SIAP_DISERAHKAN
        }
    }
    .filter { dalamRentangTanggal(tanggalJakarta(it.dibuatPada), tanggal, hariIni) }

/** Menyaring isi tab "Riwayat". */
fun saringRiwayat(
    daftar: List<TopupPettyCash>,
    filter: FilterRiwayat,
    tanggal: FilterTanggal,
    hariIni: LocalDate = LocalDate.now(ZONA_JAKARTA),
): List<TopupPettyCash> = daftar
    .filterNot { it.status in STATUS_BUTUH_REVIEW }
    .filter {
        when (filter) {
            FilterRiwayat.SEMUA -> true
            FilterRiwayat.PROSES_FINANCE -> it.status == StatusTopup.DITERUSKAN_KE_FINANCE
            FilterRiwayat.SELESAI ->
                it.status == StatusTopup.SELESAI || it.status == StatusTopup.DISERAHKAN_LEADER
            FilterRiwayat.DITOLAK -> it.status == StatusTopup.DITOLAK
        }
    }
    .filter { dalamRentangTanggal(tanggalJakarta(it.dibuatPada), tanggal, hariIni) }
