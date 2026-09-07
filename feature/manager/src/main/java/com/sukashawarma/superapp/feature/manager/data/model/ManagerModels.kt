package com.sukashawarma.superapp.feature.manager.data.model

/**
 * Baris mentah yang dibaca modul Manager dari PostgREST.
 *
 * Sengaja setipis mungkin — hanya kolom yang benar-benar dipakai layar — karena
 * rentang "30 Hari" bisa menarik puluhan ribu pesanan dan setiap kolom tambahan
 * dibayar di jaringan dan memori perangkat.
 */

data class OutletRingkas(
    val id: String,
    val nama: String,
    val aktif: Boolean,
)

/** Satu pesanan selesai: nilainya dan berapa porsi yang keluar di dalamnya. */
data class PesananRingkas(
    val outletId: String,
    val total: Long,
    val jumlahItem: Int,
)

/** Absen masuk pertama sebuah outlet pada hari berjalan — penanda outlet buka. */
data class AbsenMasuk(
    val outletId: String,
    /** Jam Jakarta terformat `08.15`, sudah dihitung saat pembacaan. */
    val jamBuka: String,
)

/** Baris waste berstatus APPROVED; rupiah kerugiannya dihitung terpisah dari harga bahan. */
data class WasteDisetujui(
    val bahanBakuId: String,
    val qty: Double,
)
