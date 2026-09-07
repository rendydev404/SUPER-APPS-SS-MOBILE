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

        // Urutan penerima mengikuti web: yang KRITIS lebih dulu, baru dari kekurangan
        // terbesar. Tanpa peringkat kegentingan, outlet yang cuma menipis tetapi
        // gapnya besar bisa menghabiskan surplus donor sebelum outlet kritis kebagian.
        val penerima = terpakai
            .filter { it.saldo < it.threshold }
            .map { it to (it.threshold - it.saldo) }
            .sortedWith(
                compareBy<Pair<Sisi, Double>> { (sisi, _) ->
                    if (sisi.saldo < sisi.threshold / 2.0) 0 else 1
                }.thenByDescending { it.second }
            )
            .toMutableList()

        val donor = terpakai
            .filter { it.saldo > it.threshold }
            .map { it to (it.saldo - it.threshold) }
            .sortedByDescending { it.second }
            .toMutableList()

        if (penerima.isEmpty() || donor.isEmpty()) return emptyList()

        val hasil = mutableListOf<SaranTransfer>()
        // Sisa surplus tiap donor dilacak terpisah dan dipakai lintas penerima.
        // Versi sebelumnya memakai satu penunjuk yang hanya maju, sehingga donor yang
        // dilewati untuk satu penerima ikut hilang untuk penerima berikutnya.
        val sisaDonor = DoubleArray(donor.size) { donor[it].second }

        for ((target, kebutuhan) in penerima) {
            var kurang = kebutuhan
            for (i in donor.indices) {
                if (kurang <= 0.0) break
                if (sisaDonor[i] <= 0.0) continue
                val sumber = donor[i].first
                // Satu outlet tidak bisa sekaligus di atas dan di bawah threshold,
                // jadi cabang ini defensif — tetapi melewatinya tidak boleh membuang
                // donornya untuk penerima lain.
                if (sumber.row.outletId == target.row.outletId) continue
                val dipindah = minOf(kurang, sisaDonor[i])
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
                sisaDonor[i] -= dipindah
            }
        }
        return hasil
    }
}
