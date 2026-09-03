package com.sukashawarma.superapp.feature.stok.domain

import com.sukashawarma.superapp.feature.stok.data.model.MonitoringRow

/**
 * Satu saran pemindahan stok. Murni bantuan pengambilan keputusan — tidak ada
 * transaksi yang dijalankan dari sini, dan Fase 1 tidak menulis apa pun ke database.
 */
data class SaranTransfer(
    val bahanBakuId: String,
    val bahanNama: String,
    val dariOutletId: String,
    val dariOutletNama: String,
    val keOutletId: String,
    val keOutletNama: String,
    /** Jumlah pada satuan terkecil. */
    val qtyNorm: Double,
    val meta: UnitMeta,
) {
    val qtyTampil: String
        get() = UnitScale.formatBerjenjang(qtyNorm, meta) ?: qtyNorm.toLongString()
}

/**
 * Alokasi surplus antar outlet secara greedy, untuk satu bahan.
 *
 * Penerima adalah outlet yang saldonya di bawah threshold; kebutuhannya sebesar
 * selisih menuju threshold. Donor adalah outlet yang saldonya di atas threshold,
 * dan surplus yang boleh diberikan hanya kelebihan di atas threshold-nya sendiri —
 * memindahkan lebih dari itu hanya memindahkan masalah, bukan menyelesaikannya.
 *
 * Seluruh perhitungan memakai nilai yang sudah dinormalisasi ke satuan terkecil.
 * Baris yang skalanya tidak dapat dipercaya dikeluarkan dari perhitungan, bukan
 * diperlakukan seolah faktornya 1.
 */
object TransferSuggester {

    fun untukBahan(barisLintasOutlet: List<MonitoringRow>): List<SaranTransfer> {
        if (barisLintasOutlet.size < 2) return emptyList()

        data class Sisi(val row: MonitoringRow, val saldo: Double, val threshold: Double)

        val terpakai = barisLintasOutlet.mapNotNull { row ->
            val saldo = row.saldoNorm ?: return@mapNotNull null
            val threshold = row.thresholdNorm ?: return@mapNotNull null
            Sisi(row, saldo, threshold)
        }

        val penerima = terpakai
            .filter { it.saldo < it.threshold }
            .map { it to (it.threshold - it.saldo) }
            .sortedByDescending { it.second }
            .toMutableList()

        val donor = terpakai
            .filter { it.saldo > it.threshold }
            .map { it to (it.saldo - it.threshold) }
            .sortedByDescending { it.second }
            .toMutableList()

        if (penerima.isEmpty() || donor.isEmpty()) return emptyList()

        val hasil = mutableListOf<SaranTransfer>()
        var iDonor = 0
        var sisaDonor = donor.getOrNull(0)?.second ?: 0.0

        for (pasangan in penerima) {
            var kurang = pasangan.second
            val target = pasangan.first
            while (kurang > 0.0 && iDonor < donor.size) {
                if (sisaDonor <= 0.0) {
                    iDonor++
                    sisaDonor = donor.getOrNull(iDonor)?.second ?: 0.0
                    continue
                }
                val sumber = donor[iDonor].first
                if (sumber.row.outletId == target.row.outletId) {
                    iDonor++
                    sisaDonor = donor.getOrNull(iDonor)?.second ?: 0.0
                    continue
                }
                val dipindah = minOf(kurang, sisaDonor)
                hasil += SaranTransfer(
                    bahanBakuId = target.row.bahanBakuId,
                    bahanNama = target.row.itemName,
                    dariOutletId = sumber.row.outletId,
                    dariOutletNama = sumber.row.outletName,
                    keOutletId = target.row.outletId,
                    keOutletNama = target.row.outletName,
                    qtyNorm = dipindah,
                    meta = target.row.meta,
                )
                kurang -= dipindah
                sisaDonor -= dipindah
            }
            if (iDonor >= donor.size) break
        }
        return hasil
    }
}
