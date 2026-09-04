package com.sukashawarma.superapp.feature.distribusi.domain

/**
 * Kondisi item sebagaimana dilihat crew, dan nilai yang ditulis ke kolom
 * `surat_jalan_item.kondisi`.
 *
 * Layar hanya menawarkan dua pilihan, sama seperti web: "Baik" dan "Tidak
 * Sesuai". Nilai `hilang_qty` ada di CHECK constraint database tapi tidak pernah
 * ditulis web, jadi native juga tidak menulisnya — bentuk baris hasil kedua
 * aplikasi harus identik.
 */
enum class KondisiItem(val nilaiDb: String) {
    BAIK("baik"),
    TIDAK_SESUAI("rusak"),
}

/** Isian crew untuk satu item. `qtyTerima` dalam SATUAN DISTRIBUSI. */
data class IsianVerifikasi(
    val qtyTerima: Double?,
    val kondisi: KondisiItem,
    val catatan: String,
    val fotoPath: String?,
)

sealed interface HasilValidasi {
    data object Lolos : HasilValidasi
    data class Tolak(val pesan: String) : HasilValidasi
}

/**
 * Aturan boleh-lanjut per item. Cermin `handleBaik`, `handleTidakSesuaiConfirm`,
 * dan `handleAdvance` di `VerifikasiForm.tsx`.
 */
object ValidasiVerifikasi {

    /**
     * Dipanggil saat crew menekan tombol konfirmasi kondisi.
     * `qtyDikirimTampil` adalah qty kiriman yang sudah dikonversi ke satuan
     * distribusi — bandingannya harus pada satuan yang sama dengan yang diketik.
     */
    fun konfirmasiKondisi(isian: IsianVerifikasi, qtyDikirimTampil: Long): HasilValidasi {
        val qty = isian.qtyTerima
        if (qty == null || (qty == 0.0 && isian.kondisi == KondisiItem.BAIK)) {
            return HasilValidasi.Tolak("Isi jumlah fisik yang diterima terlebih dahulu.")
        }
        if (qty < 0) {
            return HasilValidasi.Tolak("Jumlah terima tidak boleh kurang dari 0.")
        }
        if (qty > qtyDikirimTampil) {
            return HasilValidasi.Tolak("Jumlah terima tidak boleh melebihi jumlah yang dikirim.")
        }
        if (isian.kondisi == KondisiItem.TIDAK_SESUAI && isian.catatan.isBlank()) {
            return HasilValidasi.Tolak("Item tidak sesuai wajib disertai catatan alasan.")
        }
        return HasilValidasi.Lolos
    }

    /** Foto bukti tidak bisa dilewati — inilah yang membuat selisih bisa
     *  ditelusuri belakangan. */
    fun bolehLanjut(isian: IsianVerifikasi): HasilValidasi =
        if (isian.fotoPath.isNullOrBlank()) {
            HasilValidasi.Tolak("Foto bukti wajib diambil sebelum lanjut ke item berikutnya.")
        } else {
            HasilValidasi.Lolos
        }
}
