package com.sukashawarma.superapp.feature.stok.data.model

import com.sukashawarma.superapp.feature.stok.domain.JenisLogistik
import com.sukashawarma.superapp.feature.stok.domain.StatusRetur

/**
 * Satu baris `retur_stok_item` — cermin `ReturStokItem` di `types/retur.ts`.
 *
 * [qtyKlaim] dan [qtyDiterimaKitchen] keduanya pada SATUAN BESAR, sama seperti
 * yang ditulis RPC. Jangan menampilkannya lewat konversi satuan pesan seperti
 * permintaan: retur ditimbang, bukan dipesan per pack.
 */
data class ReturItem(
    val id: String,
    val bahanBakuId: String,
    val namaBahan: String?,
    val satuan: String?,
    val qtyKlaim: Double,
    val qtyDiterimaKitchen: Double?,
    val fotoFisikUrl: String?,
    val fotoTimbanganUrl: String?,
    val alasan: String,
    val catatan: String?,
) {
    /**
     * Web mengunggah SATU foto dan menyimpannya ke kedua kolom, jadi dua thumbnail
     * yang sama tidak perlu ditampilkan dua kali. Lihat `ModalApproveManager.tsx`
     * yang memeriksa hal yang sama sebelum merender.
     */
    val fotoTerpisah: Boolean
        get() = !fotoTimbanganUrl.isNullOrBlank() && fotoTimbanganUrl != fotoFisikUrl
}

/** Satu tiket `retur_stok` beserta itemnya — cermin `ReturStok` di `types/retur.ts`. */
data class Retur(
    val id: String,
    val nomorRetur: String,
    val outletId: String,
    val outletName: String?,
    val status: StatusRetur,
    val createdAt: String?,
    val pembuatNama: String?,
    val catatanManager: String?,
    val jenisLogistik: JenisLogistik,
    val nomorResi: String?,
    val driverNama: String?,
    val driverKontak: String?,
    val driverPlat: String?,
    val fotoSerahTerimaUrl: String?,
    val catatanKitchen: String?,
    val suratJalanPenggantiId: String?,
    val nomorSuratJalanPengganti: String?,
    val items: List<ReturItem>,
)
