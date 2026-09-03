package com.sukashawarma.superapp.feature.stok.domain

import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Perhitungan selisih opname — port langsung dari `apps/stok/src/lib/stok/selisih.ts`.
 *
 * Angka-angka ini menentukan apakah sebuah opname perlu persetujuan leader, jadi
 * ambangnya tidak boleh ditafsir ulang. Disalin apa adanya, termasuk fallback 15%
 * untuk satuan yang tidak diketahui.
 */
object Selisih {

    private val SATUAN_TERUKUR = setOf("gram", "ml", "kg", "liter")

    /** Selisih = fisik dikurangi sistem. Fisik kosong dianggap nol, sama seperti web. */
    fun hitung(qtyFisik: Double?, qtySystem: Double): Double = (qtyFisik ?: 0.0) - qtySystem

    /**
     * Ambang toleransi dalam persen.
     *
     * - Satuan timbang/ukur (gram, kg, ml, liter) -> 5%
     * - Satuan hitung (pcs, pack, dus, dll) -> 0%, selisih sekecil apa pun ditandai
     * - Satuan tidak diketahui -> 15%
     *
     * Ada satu cabang halus yang mudah salah disederhanakan: bahan yang satuan
     * besarnya countable tetapi satuan kecilnya timbang (mis. SAPI = blok + gram)
     * tetap mendapat 5% — kecuali satuan besarnya `pcs`, seperti GAS yang dihitung
     * per tabung dan karenanya tetap 0%.
     */
    fun ambangPersen(satuan: String?, satuanKecil: String?): Int {
        if (satuan.isNullOrBlank()) return 15
        val s = satuan.lowercase()
        val sk = satuanKecil?.lowercase().orEmpty()

        if (s in SATUAN_TERUKUR) return 5
        if (sk in SATUAN_TERUKUR) {
            if (sk in setOf("gram", "ml", "liter") && s != "pcs") return 5
        }
        return 0
    }

    data class PersenSelisih(
        val persen: Double,
        val teks: String,
        val rugi: Boolean,
        val lebih: Boolean,
        val nol: Boolean,
    )

    /**
     * Persentase selisih terhadap saldo sistem.
     *
     * Saat saldo sistem nol, persentase tidak terdefinisi secara matematis; web
     * memilih menampilkan ±100% agar tetap terbaca sebagai penyimpangan penuh.
     */
    fun persen(selisih: Double, qtySystem: Double): PersenSelisih {
        if (qtySystem == 0.0) {
            return when {
                selisih == 0.0 -> PersenSelisih(0.0, "0.0%", rugi = false, lebih = false, nol = true)
                selisih > 0 -> PersenSelisih(100.0, "+100.0%", rugi = false, lebih = true, nol = false)
                else -> PersenSelisih(-100.0, "-100.0%", rugi = true, lebih = false, nol = false)
            }
        }
        val rasio = (selisih / abs(qtySystem)) * 100.0
        val dibulatkan = (rasio * 10).roundToLong() / 10.0
        val teks = (if (rasio > 0) "+" else "") + String.format(java.util.Locale.US, "%.1f", dibulatkan) + "%"
        return PersenSelisih(
            persen = dibulatkan,
            teks = teks,
            rugi = selisih < 0,
            lebih = selisih > 0,
            nol = selisih == 0.0,
        )
    }

    /**
     * Apakah selisih ini perlu persetujuan leader.
     *
     * Perhatikan perbandingannya memakai `>` bukan `>=`: selisih yang tepat sama
     * dengan ambang TIDAK ditandai. Itu perilaku web dan diuji secara eksplisit di
     * sana (selisih 5% pada toleransi 5% tidak di-flag).
     */
    fun perluDitandai(
        selisih: Double,
        qtySystem: Double,
        satuan: String?,
        satuanKecil: String?,
    ): Boolean {
        if (qtySystem == 0.0) return selisih != 0.0
        val ambang = ambangPersen(satuan, satuanKecil) / 100.0
        return abs(selisih) > ambang * abs(qtySystem)
    }
}
