package com.sukashawarma.superapp.feature.stok.data.model

import com.sukashawarma.superapp.feature.stok.domain.DistribusiUnit
import com.sukashawarma.superapp.feature.stok.domain.StatusTopUp
import com.sukashawarma.superapp.feature.stok.domain.StokStatus
import com.sukashawarma.superapp.feature.stok.domain.UnitMeta
import com.sukashawarma.superapp.feature.stok.domain.UnitScale

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
    /** Kapan draft terakhir disentuh — dipakai penanda "draft tersimpan" di form. */
    val updatedAt: String? = null,
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
    /** Sudah pernah tersimpan ke server sebagai draft — baris ini tidak polos lagi. */
    val tersimpanDraft: Boolean = false,
) {
    val adaMasukan: Boolean
        get() = besar.isNotBlank() || tengah.isNotBlank() || kecil.isNotBlank()
}

/**
 * Satu baris `opname_item` untuk layar detail — model BACA, bukan masukan form.
 *
 * Sengaja terpisah dari [OpnameItemRow]: yang itu membawa tiga field String hasil
 * ketikan pengguna, sedangkan yang ini membawa angka apa adanya dari database.
 *
 * Ketiga angkanya ([qtySystem], [qtyFisik], [selisih]) ada pada **satuan terkecil**
 * — `OpnameForm` web menghitungnya begitu tanpa kecuali, berbeda dari
 * `stok_balance.saldo` yang skalanya campuran. Menempelkan satuan besar langsung ke
 * angka mentahnya adalah bug 1000× yang sama seperti pada SPVTable web dulu.
 */
data class OpnameItemDetail(
    val id: String,
    val bahanBakuId: String,
    val namaBahan: String?,
    val meta: UnitMeta,
    val qtySystem: Double,
    /** Null berarti "belum terhitung" — berbeda dari nol, dan tidak boleh disamakan. */
    val qtyFisik: Double?,
    /** Kolom generated di database (`qty_fisik - qty_system`). Baca saja, jangan ditulis. */
    val selisih: Double,
    val flagged: Boolean,
    val catatan: String?,
) {
    /** Bahan yang sudah tidak aktif di master tetap punya baris; tampilkan ala web. */
    val namaTampil: String
        get() = namaBahan?.takeIf { it.isNotBlank() } ?: "Bahan ${bahanBakuId.take(8)}"

    val terhitung: Boolean get() = qtyFisik != null
}

// --------------------------------------------------------------------- waste

/** Status laporan waste — cermin `stok_waste_reports.status` (huruf besar semua). */
enum class StatusWaste(val nilai: String, val label: String) {
    PENDING("PENDING", "Menunggu"),
    APPROVED("APPROVED", "Disetujui"),
    REJECTED("REJECTED", "Ditolak");

    companion object {
        fun dari(nilai: String?): StatusWaste =
            entries.firstOrNull { it.nilai == nilai } ?: PENDING
    }
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
     * Satuan yang dipakai saat memesan, mengikuti `satuan_distribusi` seperti web —
     * mis. BAWANG dipesan per "kg" walau satuan besarnya "Bal".
     *
     * Harga `bahan_baku_harga.harga_beli` berharga per SATUAN BESAR, jadi qty pesanan
     * WAJIB dikonversi lewat [faktorDistribusi] sebelum dikalikan harga — lihat
     * `PermintaanRepository.estimasiNilai`. Web sempat melewatkan konversi itu
     * sehingga 1 kg bawang terbaca Rp 650.000 (harga 1 Bal), dan kini sudah diperbaiki
     * di sisi web juga.
     */
    val satuanPesan: String get() = satuanDistribusi ?: satuan ?: ""

    /** Berapa satuan pesan dalam satu satuan besar; 1.0 bila keduanya sama. */
    val faktorDistribusi: Double
        get() = DistribusiUnit.faktor(
            satuan = satuan,
            satuanTengah = satuanTengah,
            faktorTengah = faktorTengah,
            satuanKecil = satuanKecil,
            faktorTampilan = faktorTampilan,
            satuanDistribusi = satuanDistribusi,
        )
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
 * Satu pengajuan top-up saldo outlet — cermin baris `outlet_budget_topup_requests`.
 * Alurnya dua tahap: diajukan (`pending_am`) → disetujui AM (`pending_finance`) →
 * disetujui Finance (`approved`), atau ditolak di tahap mana pun.
 */
data class TopUpRequest(
    val id: String,
    val outletId: String,
    val nominal: Double,
    /** `weekday` atau `weekend`. */
    val kategoriPeriode: String,
    val status: StatusTopUp,
    val pemohonNama: String?,
    val amNama: String?,
    val financeNama: String?,
    val catatan: String?,
    val createdAt: String?,
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

/**
 * Satu baris saldo `monitoring_view_crew` sebagai kandidat permintaan.
 *
 * `statusView` disimpan apa adanya untuk penelusuran, tetapi keputusan "perlu
 * diminta" TIDAK boleh bersandar padanya sendirian — lihat [perluDiminta].
 */
data class SaranPermintaan(
    val bahanBakuId: String,
    val itemName: String,
    val satuan: String?,
    val currentQty: Double,
    val saldoIsGram: Boolean,
    val threshold: Double,
    val statusView: String?,
) {
    /**
     * Status di atas skala ternormalisasi, sama seperti `MonitoringRow.status`.
     *
     * View membandingkan `saldo` mentah dengan `threshold`, padahal saldo bisa
     * berada di satuan terkecil (`saldo_is_gram`) sementara threshold selalu di
     * satuan besar. Untuk bahan berfaktor bukan 1 perbandingan itu meleset
     * sebesar faktor_tampilan, dan selalu ke arah yang sama: bahan yang benar-benar
     * kritis terbaca `ok`.
     */
    fun status(meta: UnitMeta): StokStatus = UnitScale.status(
        UnitScale.normalizeSaldo(currentQty, saldoIsGram, meta),
        UnitScale.normalizeThreshold(threshold, meta),
    )

    /**
     * Perlu ditawarkan di katalog permintaan?
     *
     * Gabungan dua sumber, bukan salah satu saja:
     * - status ternormalisasi, yang memperbaiki perbandingan satuan campuran;
     * - `statusView`, yang membawa aturan porsi resep (`marquee_warning_threshold`)
     *   yang hanya dihitung di server dan tidak tersedia di layar ini.
     *
     * Menggabungkan keduanya hanya bisa MENAMBAH bahan, tidak pernah menyembunyikan
     * yang sebelumnya tampil — itu yang membuatnya aman. Juga menjadi jaring bila
     * [status] mengembalikan UNKNOWN karena faktor satuan tidak dapat dipercaya.
     */
    /**
     * Benar-benar kritis, memakai definisi yang sama persis dengan KPI "Kritis" di
     * layar Dashboard: `StokStatus.BELOW`, atau `below` menurut view (yang membawa
     * aturan porsi resep `marquee_warning_threshold` dari server).
     *
     * Sengaja lebih sempit daripada [perluDiminta]. Katalog permintaan harus tetap
     * menawarkan bahan yang baru menipis, tetapi menyebut semuanya "kritis" membuat
     * angka di layar ini berselisih dengan Dashboard tanpa ada yang salah pada data.
     */
    fun kritis(meta: UnitMeta): Boolean = status(meta) == StokStatus.BELOW || statusView == "below"

    fun perluDiminta(meta: UnitMeta): Boolean {
        val s = status(meta)
        return s == StokStatus.BELOW ||
            s == StokStatus.WARNING ||
            statusView == "below" ||
            statusView == "warning"
    }
}

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
