package com.sukashawarma.superapp.feature.stok.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Nilai harapan diambil dari perilaku `decomposeTriUnitRaw` di
 * `apps/stok/src/lib/format/compositeUnit.ts`, termasuk kasus nyata yang terlihat di
 * layar web: AYAM 33 Kg / 980 Gr, ES BATU 0 Bal / 6 Kg / 880 Gr, GAS 6 Tabung / 1200 Gr.
 */
class TriUnitTest {

    @Test
    fun `saldo satuan besar dengan satuan kecil saja`() {
        // AYAM: saldo 33.98 Kg, 1 Kg = 1000 Gram.
        val hasil = decomposeTriUnit(
            qty = 33.98,
            saldoIsGram = false,
            satuanTengah = null,
            faktorTengah = null,
            satuanKecil = "Gram",
            faktorTampilan = 1000.0,
        )
        assertEquals(33.0, hasil.besar, 0.0001)
        assertEquals(0.0, hasil.tengah, 0.0001)
        assertEquals(980.0, hasil.kecil, 0.0001)
    }

    @Test
    fun `saldo satuan besar dengan tiga jenjang`() {
        // ES BATU: 1 Bal = 10 Kg = 10000 Gram; saldo 0.688 Bal -> 6 Kg 880 Gr.
        val hasil = decomposeTriUnit(
            qty = 0.688,
            saldoIsGram = false,
            satuanTengah = "Kg",
            faktorTengah = 10.0,
            satuanKecil = "Gram",
            faktorTampilan = 10000.0,
        )
        assertEquals(0.0, hasil.besar, 0.0001)
        assertEquals(6.0, hasil.tengah, 0.0001)
        assertEquals(880.0, hasil.kecil, 0.0001)
    }

    @Test
    fun `saldo sudah satuan terkecil dipecah balik ke jenjang besar`() {
        // GAS 3Kg: 1 Tabung = 3000 Gram; saldo 19200 Gram -> 6 Tabung 1200 Gr.
        val hasil = decomposeTriUnit(
            qty = 19200.0,
            saldoIsGram = true,
            satuanTengah = null,
            faktorTengah = null,
            satuanKecil = "Gram",
            faktorTampilan = 3000.0,
        )
        assertEquals(6.0, hasil.besar, 0.0001)
        assertEquals(1200.0, hasil.kecil, 0.0001)
    }

    @Test
    fun `saldo satuan terkecil dengan tiga jenjang`() {
        val hasil = decomposeTriUnit(
            qty = 6880.0,
            saldoIsGram = true,
            satuanTengah = "Kg",
            faktorTengah = 10.0,
            satuanKecil = "Gram",
            faktorTampilan = 10000.0,
        )
        assertEquals(0.0, hasil.besar, 0.0001)
        assertEquals(6.0, hasil.tengah, 0.0001)
        assertEquals(880.0, hasil.kecil, 0.0001)
    }

    /** Saldo negatif harus tetap terbaca sebagai defisit, bukan berbalik arah. */
    @Test
    fun `saldo negatif mempertahankan tanda pada satuan besar`() {
        val hasil = decomposeTriUnit(
            qty = -10.56,
            saldoIsGram = false,
            satuanTengah = null,
            faktorTengah = null,
            satuanKecil = "Gram",
            faktorTampilan = 1000.0,
        )
        assertEquals(-10.0, hasil.besar, 0.0001)
        assertEquals(560.0, hasil.kecil, 0.0001)
    }

    @Test
    fun `tanpa faktor sama sekali saldo dibiarkan apa adanya`() {
        val hasil = decomposeTriUnit(
            qty = 42.0,
            saldoIsGram = false,
            satuanTengah = null,
            faktorTengah = null,
            satuanKecil = null,
            faktorTampilan = null,
        )
        assertEquals(42.0, hasil.besar, 0.0001)
        assertEquals(0.0, hasil.tengah, 0.0001)
        assertEquals(0.0, hasil.kecil, 0.0001)
    }

    @Test
    fun `label satuan diseragamkan`() {
        assertEquals("Gr", formatSatuan("gram"))
        assertEquals("Kg", formatSatuan("KG"))
        assertEquals("Pcs", formatSatuan("biji"))
        assertEquals("Tabung", formatSatuan("tabung"))
        assertEquals("", formatSatuan(null))
    }

    @Test
    fun `angka bulat tidak diberi ekor desimal`() {
        assertEquals("33", formatAngkaStok(33.0))
        assertEquals("0", formatAngkaStok(0.0))
        assertEquals("6.5", formatAngkaStok(6.5))
    }

    // ------------------------------------------------------------------- katalog

    @Test
    fun `kategori di luar daftar jatuh ke operasional`() {
        assertEquals(KategoriStok.FOOD_BEVERAGE, KategoriStok.dari("FOOD & BEVERAGE"))
        assertEquals(KategoriStok.BUMBU, KategoriStok.dari("bumbu"))
        assertEquals(KategoriStok.OPERASIONAL, KategoriStok.dari("ENTAH APA"))
        assertEquals(KategoriStok.OPERASIONAL, KategoriStok.dari(null))
    }

    @Test
    fun `bahan gudang pusat disembunyikan dari outlet biasa`() {
        assertEquals(false, bolehTampilDiOutlet("GARAM", "Outlet Empang"))
        assertEquals(true, bolehTampilDiOutlet("GARAM", "GUDANG PUSAT"))
        assertEquals(true, bolehTampilDiOutlet("AYAM", "Outlet Empang"))
    }

    @Test
    fun `lokasi penyimpanan mengikuti aturan web`() {
        assertEquals("Frozen Storage", lokasiPenyimpanan("FOOD & BEVERAGE", "AYAM"))
        assertEquals("Utility Area", lokasiPenyimpanan("OPERASIONAL", "GAS 3Kg"))
        assertEquals("Dry Storage", lokasiPenyimpanan("PACKAGING", "FOIL"))
    }
}
