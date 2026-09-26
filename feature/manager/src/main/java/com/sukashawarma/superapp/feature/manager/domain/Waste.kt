package com.sukashawarma.superapp.feature.manager.domain

import com.sukashawarma.superapp.domain.model.Role
import com.sukashawarma.superapp.feature.stok.domain.UnitMeta
import com.sukashawarma.superapp.feature.stok.domain.decomposeTriUnit
import kotlin.math.roundToLong

/**
 * Status laporan waste. Nilainya HURUF BESAR karena begitu tersimpan di kolom
 * `stok_waste_reports.status`; menuliskannya huruf kecil membuat filter diam-diam
 * tidak menemukan apa pun.
 */
enum class StatusWaste(val nilai: String) {
    MENUNGGU("PENDING"),
    DISETUJUI("APPROVED"),
    DITOLAK("REJECTED");

    companion object {
        fun dari(nilai: String?): StatusWaste? = entries.find { it.nilai == nilai }
    }
}

/** Format angka yang bersih dari artifak floating-point (misal 2.000000000000004 -> 2, 0.15000000000000002 -> 0.15). */
fun formatAngkaBersih(nilai: Double): String {
    if (!nilai.isFinite()) return "0"
    val bulat3 = kotlin.math.round(nilai * 1000.0) / 1000.0
    if (bulat3 % 1.0 == 0.0) return bulat3.toLong().toString()
    return String.format(java.util.Locale.US, "%.3f", bulat3)
        .trimEnd('0')
        .trimEnd('.')
}

/**
 * Format kuantitas ramah pengguna.
 * Mengubah satuan pecahan desimal (misal 0.004166666666666667 Dus -> 1 Lembar,
 * 12.806000000000001 kg -> 12 kg · 806 gr, 2.000000000000004 Pack -> 2 Pack).
 */
fun formatQtyRamah(qty: Double, meta: UnitMeta): String {
    val parts = decomposeTriUnit(
        qty = qty,
        saldoIsGram = false,
        satuanTengah = meta.satuanTengah,
        faktorTengah = meta.faktorTengah,
        satuanKecil = meta.satuanKecil,
        faktorTampilan = meta.faktorTampilan,
    )
    val daftar = listOf(
        parts.besar to meta.satuan,
        parts.tengah to meta.satuanTengah,
        parts.kecil to meta.satuanKecil,
    ).filter { it.first != 0.0 && !it.second.isNullOrBlank() }

    return if (daftar.isEmpty()) {
        "0 ${meta.satuan.orEmpty()}".trim()
    } else {
        daftar.joinToString(" · ") { "${formatAngkaBersih(it.first)} ${it.second}" }
    }
}

/** Satu laporan waste beserta nilai rupiah kerugiannya. */
data class LaporanWaste(
    val id: String,
    val outletId: String,
    val outletNama: String,
    val bahanId: String,
    val bahanNama: String,
    val satuan: String,
    val qty: Double,
    val hargaBeli: Double,
    val nilai: Long,
    val alasan: String,
    val fotoUrl: String?,
    val status: StatusWaste,
    val alasanPenolakan: String?,
    val pelaporId: String?,
    val pelaporNama: String,
    val penyetujuNama: String?,
    val dibuatPada: String,
    val meta: UnitMeta = UnitMeta(satuan = satuan),
) {
    /** Qty ditampilkan ramah bertingkat — misal "1 Lembar" bukan "0.004166666666666667 Dus". */
    val qtyLabel: String
        get() = formatQtyRamah(qty, meta)

    val qtyTeks: String
        get() = qtyLabel
}

/** Satu baris pada daftar bahan paling banyak terbuang. */
data class BahanTerbuang(
    val nama: String,
    val satuan: String,
    val qty: Double,
    val nilai: Long,
    val meta: UnitMeta = UnitMeta(satuan = satuan),
) {
    val qtyLabel: String
        get() = formatQtyRamah(qty, meta)

    val qtyTeks: String
        get() = qtyLabel
}

/** Angka ringkas untuk tab Riwayat & Analitik. */
data class RingkasanWaste(
    val totalNilai: Long,
    val totalInsiden: Int,
    val bahanTeratas: List<BahanTerbuang>,
    val jumlahMenunggu: Int,
) {
    companion object {
        val KOSONG = RingkasanWaste(0, 0, emptyList(), 0)
    }
}

/** Rupiah kerugian satu baris waste. Dibulatkan per baris, seperti web. */
fun nilaiWaste(qty: Double, hargaBeli: Double): Long = (qty * hargaBeli).roundToLong()

/**
 * Menjumlahkan laporan yang sudah disetujui menjadi angka ringkas.
 *
 * Bahan digabung berdasarkan id, bukan nama: dua bahan boleh bernama mirip, dan
 * menggabungkannya berdasarkan nama akan menyatukan yang sebenarnya berbeda.
 */
fun susunRingkasanWaste(
    disetujui: List<LaporanWaste>,
    jumlahMenunggu: Int,
): RingkasanWaste {
    val perBahan = LinkedHashMap<String, BahanTerbuang>()
    disetujui.forEach { laporan ->
        val ada = perBahan[laporan.bahanId]
        perBahan[laporan.bahanId] = if (ada == null) {
            BahanTerbuang(laporan.bahanNama, laporan.satuan, laporan.qty, laporan.nilai, laporan.meta)
        } else {
            ada.copy(qty = ada.qty + laporan.qty, nilai = ada.nilai + laporan.nilai)
        }
    }
    return RingkasanWaste(
        totalNilai = disetujui.sumOf { it.nilai },
        totalInsiden = disetujui.size,
        bahanTeratas = perBahan.values.sortedByDescending { it.nilai }.take(5),
        jumlahMenunggu = jumlahMenunggu,
    )
}

/**
 * Alasan mengapa sebuah laporan tidak boleh diproses, atau null kalau boleh.
 *
 * Rantai pemeriksaannya cermin `processWasteApproval` di actions/waste.ts web,
 * urutannya sengaja sama supaya pesan yang muncul di HP dan di laptop sama untuk
 * keadaan yang sama. Database tetap menjadi penjaga terakhir lewat RLS
 * `waste_reports_update`; ini hanya supaya kegagalan terjelaskan, bukan berupa
 * galat mentah.
 */
fun halanganMemproses(
    role: Role?,
    idPengguna: String?,
    laporan: LaporanWaste,
): String? = when {
    role !in ROLE_PEMROSES_WASTE ->
        "Akses ditolak: hanya manajer atau admin yang boleh memproses waste."
    laporan.status != StatusWaste.MENUNGGU ->
        "Laporan sudah diproses sebelumnya (status: ${laporan.status.nilai})."
    laporan.pelaporId != null && laporan.pelaporId == idPengguna ->
        "Anda tidak dapat menyetujui laporan yang Anda buat sendiri."
    else -> null
}

/**
 * Role yang boleh memproses waste. Modul ini hanya terbuka untuk AM dan RM, tapi
 * himpunannya tetap ditulis lengkap seperti `validRoles` di web supaya keduanya
 * bisa dibandingkan baris per baris kalau nanti berbeda.
 */
val ROLE_PEMROSES_WASTE: Set<Role> = setOf(
    Role.AREA_MANAGER,
    Role.REGIONAL_MANAGER,
    Role.ADMIN,
    Role.OWNER,
    Role.DEVELOPER,
)

/** Panjang minimum alasan penolakan — cermin guard 3 karakter di web. */
const val MIN_ALASAN_PENOLAKAN = 3

/** null kalau alasan sah; selain itu pesan yang bisa langsung ditampilkan. */
fun validasiAlasanPenolakan(alasan: String): String? =
    if (alasan.trim().length < MIN_ALASAN_PENOLAKAN) {
        "Alasan penolakan wajib diisi (minimal $MIN_ALASAN_PENOLAKAN karakter)."
    } else {
        null
    }
