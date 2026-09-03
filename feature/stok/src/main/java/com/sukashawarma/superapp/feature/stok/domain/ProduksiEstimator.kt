package com.sukashawarma.superapp.feature.stok.domain

import com.sukashawarma.superapp.feature.stok.data.model.ResepRingkas
import kotlin.math.floor

/**
 * Hasil estimasi satu resep.
 *
 * [lengkap] bernilai false bila ada bahan resep yang saldonya tidak diketahui
 * (tidak muncul di monitoring, atau faktor konversinya tidak dapat dipercaya).
 * Angka [porsi] tetap dihitung dari bahan yang diketahui, tetapi layar harus
 * menandainya sebagai perkiraan yang belum utuh, bukan menyajikannya sebagai pasti.
 */
data class EstimasiPorsi(
    val resepId: String,
    val resepNama: String,
    val porsi: Int,
    val bottleneckBahanId: String?,
    val bottleneckNama: String?,
    val lengkap: Boolean,
)

/**
 * Berapa porsi yang masih bisa dibuat dari stok yang ada.
 *
 * Porsi satu resep adalah nilai terkecil dari `floor(saldo / kebutuhan per porsi)`
 * pada seluruh bahannya; bahan yang menghasilkan nilai terkecil itulah penghambatnya.
 *
 * Skala kebutuhan resep ditentukan `UnitScale.kebutuhanResepKeSmallest`, mengikuti
 * aturan yang sama dengan `estimasi_produksi.ts` di web: `resep_item.satuan` yang
 * berbeda dari satuan besar bahan berarti angkanya sudah pada satuan terkecil.
 *
 * Bedanya dengan web: web membandingkan kebutuhan satuan besar terhadap `current_qty`
 * mentah yang skalanya campuran, sehingga porsi bisa meleset sebesar faktor konversi
 * untuk outlet yang saldonya sudah small-scale. Di sini kedua sisi dinormalisasi ke
 * satuan terkecil lebih dulu.
 */
object ProduksiEstimator {

    /**
     * Resep khusus outlet mengalahkan resep global bernama sama. Perbandingan nama
     * dilakukan tanpa peduli besar-kecil huruf dan spasi berlebih, karena kedua
     * baris itu ditulis oleh orang yang berbeda pada waktu yang berbeda.
     */
    fun pilihResepBerlaku(semua: List<ResepRingkas>): List<ResepRingkas> =
        semua.groupBy { it.nama.trim().lowercase() }
            .values
            .mapNotNull { sekelompok ->
                sekelompok.firstOrNull { it.outletId != null } ?: sekelompok.firstOrNull()
            }
            .sortedBy { it.nama }

    /**
     * @param saldoNormPerBahan saldo pada satuan terkecil, per `bahan_baku_id`.
     *        Bahan yang tidak ada di peta ini dianggap tidak diketahui, bukan nol —
     *        menganggapnya nol akan melaporkan "0 porsi" untuk resep yang sebenarnya
     *        bahannya tersedia tetapi tidak ikut terambil di halaman monitoring.
     */
    fun estimasi(
        resep: ResepRingkas,
        saldoNormPerBahan: Map<String, Double?>,
        namaPerBahan: Map<String, String> = emptyMap(),
    ): EstimasiPorsi {
        var porsiMin = Int.MAX_VALUE
        var bottleneckId: String? = null
        var adaYangTidakDiketahui = false
        var adaYangDihitung = false

        for (item in resep.items) {
            // Kebutuhan nol/negatif bukan "bahan gratis tak terbatas", melainkan data
            // resep yang belum benar. Faktor konversi yang tidak dapat dipercaya juga
            // menghasilkan null di sini, bukan tebakan. Keduanya dilewati dan membuat
            // resep ditandai tak lengkap.
            val kebutuhan = item.kebutuhanSmallest
            if (kebutuhan == null) {
                adaYangTidakDiketahui = true
                continue
            }
            val saldo = saldoNormPerBahan[item.bahanBakuId]
            if (saldo == null) {
                adaYangTidakDiketahui = true
                continue
            }
            adaYangDihitung = true
            val porsi = floor(saldo / kebutuhan).toInt().coerceAtLeast(0)
            if (porsi < porsiMin) {
                porsiMin = porsi
                bottleneckId = item.bahanBakuId
            }
        }

        val porsi = if (adaYangDihitung) porsiMin else 0
        return EstimasiPorsi(
            resepId = resep.id,
            resepNama = resep.nama,
            porsi = porsi,
            bottleneckBahanId = bottleneckId,
            bottleneckNama = bottleneckId?.let { namaPerBahan[it] },
            lengkap = adaYangDihitung && !adaYangTidakDiketahui,
        )
    }

    fun estimasiSemua(
        resep: List<ResepRingkas>,
        saldoNormPerBahan: Map<String, Double?>,
        namaPerBahan: Map<String, String> = emptyMap(),
    ): List<EstimasiPorsi> =
        pilihResepBerlaku(resep)
            .map { estimasi(it, saldoNormPerBahan, namaPerBahan) }
            .sortedBy { it.porsi }
}
