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
    /**
     * Nama kolomnya di database memang `catatan_kitchen` — diisi alasan penolakan
     * saat ditolak, atau catatan otomatis saat dibatalkan sistem (stale >12 jam).
     */
    val catatanKitchen: String?,
    val suratJalanId: String?,
    /** `target_metadata` — target penjualan yang melatarbelakangi permintaan, bila ada. */
    val targetJual: List<TargetJual> = emptyList(),
    val items: List<PermintaanItem>,
) {
    /** Kode ringkas ala web: `#REQ-XXXX` dari 4 karakter pertama id. */
    val kodeReq: String get() = "#REQ-${id.take(4).uppercase()}"

    val omzetTarget: Double get() = targetJual.sumOf { it.omzet }
}

/** Satu baris `target_metadata` pada permintaan; `resepId` dipakai kalkulasi kebutuhan. */
data class TargetJual(
    val resepId: String?,
    val nama: String,
    val qty: Double,
    val hargaJual: Double,
) {
    val omzet: Double get() = qty * hargaJual
}

/**
 * Satu baris master `bahan_baku` untuk katalog permintaan — cermin `useBahanBaku` web.
 * Hanya kolom yang dipakai form yang ditarik.
 */
data class BahanBaku(
    val id: String,
    val nama: String,
    val kategori: String?,
    val satuan: String?,
    val satuanTengah: String?,
    val satuanKecil: String?,
    val faktorTengah: Double?,
    val faktorTampilan: Double?,
    /** Satuan yang dipakai orang saat memesan; null = sama dengan satuan besar. */
    val satuanDistribusi: String?,
) {
    val meta: UnitMeta
        get() = UnitMeta(
            satuan = satuan,
            satuanTengah = satuanTengah,
            satuanKecil = satuanKecil,
            faktorTengah = faktorTengah,
            faktorTampilan = faktorTampilan,
        )

    /**
     * Satuan yang dipakai saat memesan di aplikasi ini: SELALU satuan besar.
     *
     * Kolom `satuan_distribusi` sengaja diabaikan (keputusan 2026-09-04). Untuk 19
     * bahan, kolom itu berisi satuan tengah/kecil — mis. BAWANG besar "Bal" tetapi
     * `satuan_distribusi` "kg" — sedangkan `bahan_baku_harga.harga_beli` berharga per
     * satuan besar (BAWANG Rp 650.000 = 1 Bal = 20 kg). Menampilkan satuan pesan "kg"
     * bersama harga per Bal membuat layar berbohong: "1 kg = Rp 650.000".
     *
     * Memesan dalam satuan besar membuat qty, harga, dan `terpakai` di budget berada
     * pada skala yang sama, tanpa mengubah data produksi yang dipakai bersama web.
     * Konsekuensinya pesanan minimum adalah 1 satuan besar penuh, dan web tetap boleh
     * memesan pecahan — qty tersimpan sama-sama pada satuan besar, jadi keduanya tetap
     * bisa saling membaca.
     */
    val satuanPesan: String get() = satuan ?: satuanDistribusi ?: ""
}

/** Saldo satu bahan pada satu outlet untuk crosscheck di layar persetujuan. */
data class CrosscheckSaldo(val currentQty: Double, val saldoIsGram: Boolean)

/**
 * Status plafon budget satu outlet — cermin `BudgetStatus` di `lib/stok/budget.ts`
 * web, hasil RPC `get_outlet_budget_status` (lewat pembungkus ber-scope).
 */
data class BudgetStatus(
    val outletId: String,
    val nominal: Double,
    /** `harian` | `mingguan` | `bulanan` | `custom`; null bila belum dikonfigurasi. */
    val periodType: String?,
    val periodStart: String?,
    val periodEnd: String?,
    val terpakai: Double,
    val sisa: Double,
    val hasConfig: Boolean,
    val customDays: Int?,
)

/**
 * Estimasi nilai Rupiah sekumpulan item — cermin `CartEstimateResult` web.
 * Dihitung di database supaya harga beli per bahan tidak perlu terbuka ke klien.
 */
data class EstimasiKeranjang(
    val totalNilai: Double = 0.0,
    val itemTanpaHarga: List<String> = emptyList(),
    val kategoriNilai: Map<String, Double> = emptyMap(),
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
