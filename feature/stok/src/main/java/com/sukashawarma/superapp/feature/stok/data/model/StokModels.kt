package com.sukashawarma.superapp.feature.stok.data.model

import com.sukashawarma.superapp.feature.stok.domain.StokStatus
import com.sukashawarma.superapp.feature.stok.domain.UnitMeta
import com.sukashawarma.superapp.feature.stok.domain.UnitScale

/** Satu outlet yang boleh dilihat pengguna — hasil `accessible_outlet_ids()`. */
data class OutletRingkas(
    val id: String,
    val name: String,
    /** Batas porsi minimum sebelum bahan dianggap kritis di outlet ini. */
    val marqueeWarningThreshold: Int = UnitScale.DEFAULT_MARQUEE_WARNING,
)

/**
 * Satu baris `monitoring_view_scoped`, ditambah metadata satuan dari `bahan_baku`
 * yang ikut ditarik lewat embed PostgREST dalam query yang sama.
 *
 * `statusView` disimpan apa adanya untuk keperluan penelusuran, tetapi status yang
 * ditampilkan adalah [status] yang dihitung ulang di atas skala ternormalisasi.
 */
data class MonitoringRow(
    val outletId: String,
    val outletName: String,
    val bahanBakuId: String,
    val itemName: String,
    val currentQty: Double,
    val threshold: Double?,
    val statusView: String?,
    val isFlagged: Boolean,
    val saldoIsGram: Boolean,
    val lastOpnameDate: String?,
    val kategori: String?,
    val satuan: String?,
    val meta: UnitMeta,
) {
    /** Saldo pada satuan terkecil; null bila faktor konversi tidak dapat dipercaya. */
    val saldoNorm: Double? = UnitScale.normalizeSaldo(currentQty, saldoIsGram, meta)

    /** Threshold pada satuan terkecil. */
    val thresholdNorm: Double? = UnitScale.normalizeThreshold(threshold, meta)

    fun status(porsiTersisa: Int? = null, marqueeWarning: Int = UnitScale.DEFAULT_MARQUEE_WARNING): StokStatus =
        UnitScale.status(saldoNorm, thresholdNorm, porsiTersisa, marqueeWarning)

    /** Saldo siap tampil: berjenjang bila faktor memadai, mentah bila tidak. */
    val saldoTampil: String
        get() = saldoNorm?.let { UnitScale.formatBerjenjang(it, meta) }
            ?: "$currentQty ${satuan.orEmpty()}".trim()
}

/** Satu mutasi pada `ledger_stok`. */
data class LedgerEntry(
    val id: String,
    val tipe: String,
    val qty: Double,
    val saldoSebelum: Double?,
    val saldoSesudah: Double?,
    val catatan: String?,
    val createdAt: String?,
) {
    val menambah: Boolean get() = qty >= 0

    /** Label Indonesia untuk tipe mutasi; tipe tak dikenal ditampilkan apa adanya. */
    val tipeLabel: String
        get() = when (tipe) {
            "terima_kiriman" -> "Terima kiriman"
            "pembelian_supplier" -> "Pembelian supplier"
            "transfer_masuk" -> "Transfer masuk"
            "pemakaian" -> "Pemakaian"
            "waste" -> "Waste"
            "transfer_keluar" -> "Transfer keluar"
            "opname_selisih" -> "Selisih opname"
            "adjustment" -> "Penyesuaian"
            "rejected_kiriman" -> "Kiriman ditolak"
            else -> tipe.replace('_', ' ').replaceFirstChar { it.uppercase() }
        }
}

/** Satu resep beserta kebutuhan bahannya. */
data class ResepRingkas(
    val id: String,
    val nama: String,
    /** null = resep global; terisi = resep khusus outlet dan menang atas global. */
    val outletId: String?,
    val items: List<ResepItemRingkas>,
)

/**
 * Satu baris kebutuhan bahan pada resep.
 *
 * [satuanResep] dan [satuanBesarBahan] harus dibawa bersama angkanya, karena skala
 * [qtyPerPorsi] ditentukan oleh perbandingan keduanya — bukan oleh konvensi tetap.
 */
data class ResepItemRingkas(
    val bahanBakuId: String,
    val qtyPerPorsi: Double,
    val satuanResep: String? = null,
    val satuanBesarBahan: String? = null,
    val faktorKonversi: Double? = null,
) {
    /** Kebutuhan per porsi pada satuan terkecil; null bila skalanya tidak dapat dipastikan. */
    val kebutuhanSmallest: Double?
        get() = UnitScale.kebutuhanResepKeSmallest(
            qtyPerPorsi = qtyPerPorsi,
            satuanResep = satuanResep,
            satuanBesarBahan = satuanBesarBahan,
            faktorKonversi = faktorKonversi,
        )
}
