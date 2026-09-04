package com.sukashawarma.superapp.feature.distribusi.domain

/**
 * Siklus hidup surat jalan, cermin CHECK constraint `surat_jalan_status_check`:
 * draft -> dikirim -> diterima_lengkap/diterima_sebagian -> selesai.
 *
 * `dikirim_lengkap` adalah varian lama dari `dikirim` yang masih ada di data
 * produksi; keduanya diperlakukan sama persis, termasuk labelnya.
 */
enum class StatusSuratJalan(val nilai: String, val label: String) {
    DRAFT("draft", "Draft"),
    DIKIRIM("dikirim", "Dalam Transit"),
    DIKIRIM_LENGKAP("dikirim_lengkap", "Dalam Transit"),
    DITERIMA_SEBAGIAN("diterima_sebagian", "Diterima Sebagian"),
    DITERIMA_LENGKAP("diterima_lengkap", "Diterima Lengkap"),
    SELESAI("selesai", "Selesai");

    companion object {
        /** Nilai tak dikenal mengembalikan null, bukan melempar: satu baris lama
         *  di database tidak boleh membuat seluruh layar gagal dimuat. */
        fun dari(nilai: String?): StatusSuratJalan? = entries.find { it.nilai == nilai }
    }
}

/** Surat jalan yang masih bisa dibuka di layar verifikasi penerimaan. Cermin
 *  gerbang status di dalam RPC `sign_receipt_surat_jalan`. */
val StatusSuratJalan.bolehDiverifikasi: Boolean
    get() = this == StatusSuratJalan.DIKIRIM ||
        this == StatusSuratJalan.DIKIRIM_LENGKAP ||
        this == StatusSuratJalan.DITERIMA_SEBAGIAN

/** Sudah pernah diverifikasi outlet — termasuk yang sudah ditutup pusat. */
val StatusSuratJalan.sudahDiterima: Boolean
    get() = this == StatusSuratJalan.DITERIMA_LENGKAP ||
        this == StatusSuratJalan.DITERIMA_SEBAGIAN ||
        this == StatusSuratJalan.SELESAI

/** Boleh ditutup jadi `selesai` oleh area/regional manager. */
val StatusSuratJalan.bolehDitutup: Boolean
    get() = this == StatusSuratJalan.DITERIMA_LENGKAP ||
        this == StatusSuratJalan.DITERIMA_SEBAGIAN

/** Kontrak minimal yang dibutuhkan `adaSelisih` — dipenuhi `SuratJalanItem`
 *  maupun proyeksi ringkas yang dipakai daftar. */
interface PenandaSelisih {
    val qtyDikirim: Double
    val qtyTerima: Double?
    val kondisi: String?
}

/**
 * Cermin `has_problem` di `useSuratJalanList.ts` dan `useRiwayatList.ts`:
 * item rusak, atau qty terima kurang dari qty dikirim.
 *
 * `qtyTerima == null` berarti belum diverifikasi, bukan kurang — pemeriksaan
 * null harus mendahului perbandingan.
 */
fun adaSelisih(items: List<PenandaSelisih>): Boolean = items.any { item ->
    val terima = item.qtyTerima
    item.kondisi == "rusak" || (terima != null && terima < item.qtyDikirim)
}
