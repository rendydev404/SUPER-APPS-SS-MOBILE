package com.sukashawarma.superapp.feature.stok.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Nilai harapan disalin dari `apps/stok/src/lib/stok/selisih.test.ts` di web. */
class SelisihTest {

    @Test
    fun `ambang 5 persen untuk satuan timbang`() {
        assertEquals(5, Selisih.ambangPersen("kg", "gram"))
        assertEquals(5, Selisih.ambangPersen("gram", null))
        assertEquals(5, Selisih.ambangPersen("liter", null))
    }

    @Test
    fun `ambang 0 persen untuk satuan hitung`() {
        assertEquals(0, Selisih.ambangPersen("pcs", "pcs"))
        assertEquals(0, Selisih.ambangPersen("dus", "lembar"))
    }

    /** Satuan besar countable tapi satuan kecil timbang tetap 5% — kecuali `pcs`. */
    @Test
    fun `blok dengan satuan kecil gram dapat toleransi 5 persen`() {
        assertEquals(5, Selisih.ambangPersen("blok", "gram"))
    }

    @Test
    fun `pcs dengan satuan kecil gram tetap 0 persen`() {
        assertEquals(0, Selisih.ambangPersen("pcs", "gram"))
    }

    @Test
    fun `satuan tidak diketahui memakai fallback 15 persen`() {
        assertEquals(15, Selisih.ambangPersen(null, null))
    }

    /** Selisih tepat pada ambang TIDAK ditandai — perbandingannya `>` bukan `>=`. */
    @Test
    fun `selisih tepat lima persen tidak ditandai`() {
        assertFalse(Selisih.perluDitandai(-500.0, 10000.0, "kg", "gram"))
    }

    @Test
    fun `selisih di atas ambang ditandai`() {
        assertTrue(Selisih.perluDitandai(-501.0, 10000.0, "kg", "gram"))
    }

    @Test
    fun `bahan hitungan menandai selisih sekecil apa pun`() {
        assertTrue(Selisih.perluDitandai(-1.0, 100.0, "pcs", "pcs"))
        assertFalse(Selisih.perluDitandai(0.0, 100.0, "pcs", "pcs"))
    }

    @Test
    fun `saldo sistem nol menandai setiap selisih bukan nol`() {
        assertTrue(Selisih.perluDitandai(5.0, 0.0, "kg", "gram"))
        assertFalse(Selisih.perluDitandai(0.0, 0.0, "kg", "gram"))
    }

    @Test
    fun `persen memakai absolut sistem dan menampilkan tanda`() {
        assertEquals("-5.0%", Selisih.persen(-500.0, 10000.0).teks)
        assertEquals("+2.5%", Selisih.persen(250.0, 10000.0).teks)
    }

    @Test
    fun `persen pada sistem nol memakai seratus persen`() {
        assertEquals("+100.0%", Selisih.persen(3.0, 0.0).teks)
        assertEquals("-100.0%", Selisih.persen(-3.0, 0.0).teks)
        assertEquals("0.0%", Selisih.persen(0.0, 0.0).teks)
    }

    @Test
    fun `selisih adalah fisik dikurangi sistem`() {
        assertEquals(-200.0, Selisih.hitung(800.0, 1000.0), 0.0001)
        assertEquals(-1000.0, Selisih.hitung(null, 1000.0), 0.0001)
    }

    // ------------------------------------------------------------ total fisik

    private val esBatu = UnitMeta(
        satuan = "Bal", satuanTengah = "Kg", satuanKecil = "Gram",
        faktorTengah = 10.0, faktorTampilan = 10000.0,
    )

    /**
     * 1 Bal = 10 Kg = 10.000 Gram, sehingga 1 Kg = 1.000 Gram.
     * Rumus dokumen (`tengah * faktor_tengah`) akan menghasilkan 60, bukan 6.000.
     */
    @Test
    fun `satuan tengah dikonversi lewat faktor tampilan dibagi faktor tengah`() {
        assertEquals(6880.0, OpnameHitung.totalFisikSmallest(0.0, 6.0, 880.0, esBatu), 0.0001)
        assertEquals(16880.0, OpnameHitung.totalFisikSmallest(1.0, 6.0, 880.0, esBatu), 0.0001)
    }

    @Test
    fun `tanpa satuan tengah hanya besar dan kecil yang dihitung`() {
        val ayam = UnitMeta(satuan = "Kg", satuanKecil = "Gram", faktorTampilan = 1000.0)
        assertEquals(33980.0, OpnameHitung.totalFisikSmallest(33.0, 0.0, 980.0, ayam), 0.0001)
    }

    @Test
    fun `tanpa satuan kecil hanya satuan besar yang dihitung`() {
        val tabung = UnitMeta(satuan = "Tabung")
        assertEquals(6.0, OpnameHitung.totalFisikSmallest(6.0, 9.0, 9.0, tabung), 0.0001)
    }

    @Test
    fun `saldo legacy dinaikkan ke satuan terkecil dan yang modern dibiarkan`() {
        val ayam = UnitMeta(satuan = "Kg", satuanKecil = "Gram", faktorTampilan = 1000.0)
        assertEquals(33980.0, OpnameHitung.saldoSistemSmallest(33.98, false, ayam), 0.0001)
        assertEquals(33980.0, OpnameHitung.saldoSistemSmallest(33980.0, true, ayam), 0.0001)
    }

    @Test
    fun `bahan tanpa satuan kecil tidak dikalikan faktor apa pun`() {
        val tabung = UnitMeta(satuan = "Tabung", faktorTampilan = 3000.0)
        assertEquals(6.0, OpnameHitung.saldoSistemSmallest(6.0, false, tabung), 0.0001)
    }
}
