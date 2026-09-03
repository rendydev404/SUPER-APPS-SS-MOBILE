package com.sukashawarma.superapp.feature.stok.data.model

import com.sukashawarma.superapp.feature.stok.domain.UnitMeta

// ------------------------------------------------------------------- ledger

/**
 * Satu kejadian pada `ledger_transaksi_ringkas` — beberapa baris ledger yang
 * berasal dari satu dokumen (mis. satu order) sudah digabung jadi satu baris.
 */
data class LedgerTransaksi(
    val transaksiKey: String,
    val outletId: String,
    val createdAt: String?,
    val jumlahBahan: Int,
    val refOrderId: String?,
    val refOpnameId: String?,
    val refShipmentId: String?,
    val refTransferId: String?,
    val singleBahanBakuId: String?,
    val singleTipe: String?,
    val singleQty: Double?,
    val singleCatatan: String?,
    val singleSaldoSesudah: Double?,
    // Pengayaan dari tabel lain, sama seperti `useLedger.ts`.
    val orderNumber: Long? = null,
    val orderItemsNames: String? = null,
    val opnameTanggal: String? = null,
    val opnameTipe: String? = null,
    val shipmentDestOutletName: String? = null,
) {
    val gabungan: Boolean get() = jumlahBahan > 1

    /** Waste yang masih menunggu approval belum menyentuh ledger — ditandai khusus. */
    val wastePending: Boolean get() = singleTipe == "waste_pending"
}

/** Satu baris ledger di layar detail transaksi. */
data class LedgerDetailRow(
    val id: String,
    val tipe: String,
    val qty: Double,
    val catatan: String?,
    val saldoSebelum: Double?,
    val saldoSesudah: Double?,
    val createdAt: String?,
    val bahanBakuId: String,
    val namaBahan: String?,
    val meta: UnitMeta,
    /** Diambil dari `stok_balance` per outlet+bahan, tidak tersedia di `ledger_stok`. */
    val saldoIsGram: Boolean = false,
)

// ------------------------------------------------------------------- opname

/** Status opname — cermin kolom `opname.status` di web. */
enum class StatusOpname(val nilai: String, val label: String) {
    DRAFT("draft", "Draft"),
    PENDING_APPROVAL("pending_approval", "Menunggu persetujuan"),
    FINALIZED("finalized", "Selesai"),
    APPROVED("approved", "Disetujui"),
    REJECTED("rejected", "Ditolak");

    companion object {
        fun dari(nilai: String?): StatusOpname =
            entries.firstOrNull { it.nilai == nilai } ?: DRAFT
    }
}

data class OpnameHeader(
    val id: String,
    val outletId: String,
    val tanggal: String?,
    val tipe: String?,
    val status: StatusOpname,
    val createdBy: String?,
    val createdAt: String?,
    val outletName: String? = null,
    val creatorName: String? = null,
    val jumlahItem: Int = 0,
    val jumlahFlagged: Int = 0,
)

/**
 * Satu baris hitung fisik.
 *
 * Nama kolom di database adalah `qty_system` dan `flagged` — bukan `qty_sistem`
 * atau `is_flagged` seperti yang sempat terlihat masuk akal.
 */
data class OpnameItemRow(
    val bahanBakuId: String,
    val namaBahan: String,
    val kategori: String?,
    val meta: UnitMeta,
    /** Saldo sistem pada satuan terkecil. */
    val qtySystemSmallest: Double,
    val saldoIsGram: Boolean,
    /** Bahan terukur memakai toleransi 5%; bahan hitungan memakai 0%. */
    val terukur: Boolean,
    // Masukan pengguna, dalam tiga jenjang satuan.
    val besar: String = "",
    val tengah: String = "",
    val kecil: String = "",
    val catatan: String = "",
) {
    val adaMasukan: Boolean
        get() = besar.isNotBlank() || tengah.isNotBlank() || kecil.isNotBlank()
}

// --------------------------------------------------------------- permintaan

enum class StatusPermintaan(val nilai: String, val label: String) {
    MENUNGGU("menunggu", "Menunggu"),
    DISETUJUI("disetujui", "Disetujui"),
    DITOLAK("ditolak", "Ditolak"),
    DIBATALKAN("dibatalkan", "Dibatalkan");

    companion object {
        fun dari(nilai: String?): StatusPermintaan =
            entries.firstOrNull { it.nilai == nilai } ?: MENUNGGU
    }
}

data class PermintaanItem(
    val id: String?,
    val bahanBakuId: String,
    val namaBahan: String?,
    val satuan: String?,
    val qtyDiminta: Double,
    val qtyDisetujui: Double?,
    val hargaSnapshot: Double?,
)

data class Permintaan(
    val id: String,
    val outletId: String,
    val outletName: String?,
    val status: StatusPermintaan,
    val createdAt: String?,
    val dibuatOleh: String?,
    val pembuatNama: String?,
    val alasanPenolakan: String?,
    val suratJalanId: String?,
    val items: List<PermintaanItem>,
)

/** Bahan berstatus tidak aman yang disarankan untuk diminta. */
data class SaranPermintaan(
    val bahanBakuId: String,
    val itemName: String,
    val satuan: String?,
    val currentQty: Double,
    val saldoIsGram: Boolean,
    val threshold: Double,
    val status: String,
)

// -------------------------------------------------------------------- mutasi

enum class StatusMutasi(val nilai: String, val label: String) {
    MENUNGGU_PERSETUJUAN("menunggu_persetujuan", "Menunggu persetujuan"),
    MENUNGGU_PENGIRIMAN("menunggu_pengiriman", "Menunggu pengiriman"),
    DIKIRIM("dikirim", "Dikirim"),
    SELESAI("selesai", "Selesai"),
    DITOLAK("ditolak", "Ditolak");

    companion object {
        fun dari(nilai: String?): StatusMutasi =
            entries.firstOrNull { it.nilai == nilai } ?: MENUNGGU_PERSETUJUAN
    }
}

data class MutasiItem(
    val id: String,
    val bahanBakuId: String,
    val namaBahan: String?,
    val satuan: String?,
    val qtyDiajukan: Double,
    val qtyDikirim: Double?,
    val qtyDiterima: Double?,
    val kondisiDiterima: String?,
)

data class Mutasi(
    val id: String,
    val outletAsalId: String,
    val outletTujuanId: String,
    val outletAsalNama: String?,
    val outletTujuanNama: String?,
    val status: StatusMutasi,
    val catatan: String?,
    val catatanPenolakan: String?,
    val createdAt: String?,
    val pembuatNama: String?,
    val approverNama: String?,
    val penerimaNama: String?,
    val items: List<MutasiItem>,
)
