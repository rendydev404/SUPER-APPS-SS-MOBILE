package com.sukashawarma.superapp.feature.stok.domain

import com.sukashawarma.superapp.feature.stok.data.model.ResepItemRingkas
import com.sukashawarma.superapp.feature.stok.data.model.ResepRingkas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProduksiEstimatorTest {

    /** Kebutuhan yang ditulis pada satuan kecil: satuan resep beda dari satuan besar bahan. */
    private fun itemKecil(bahan: String, qty: Double) =
        ResepItemRingkas(bahan, qty, satuanResep = "gr", satuanBesarBahan = "kg", faktorKonversi = 1000.0)

    /** Kebutuhan yang ditulis pada satuan besar: satuan resep sama dengan satuan besar bahan. */
    private fun itemBesar(bahan: String, qty: Double) =
        ResepItemRingkas(bahan, qty, satuanResep = "kg", satuanBesarBahan = "kg", faktorKonversi = 1000.0)

    private fun resep(vararg item: ResepItemRingkas, id: String = "r1", nama: String = "Shawarma") =
        ResepRingkas(id, nama, null, item.toList())

    // ---------------------------------------------------------------- skala resep

    @Test
    fun `satuan resep berbeda dari satuan besar berarti angkanya sudah satuan terkecil`() {
        assertEquals(150.0, itemKecil("daging", 150.0).kebutuhanSmallest!!, 0.0001)
    }

    @Test
    fun `satuan resep sama dengan satuan besar berarti angkanya perlu dikali faktor konversi`() {
        assertEquals(150.0, itemBesar("daging", 0.15).kebutuhanSmallest!!, 0.0001)
    }

    @Test
    fun `satuan resep kosong diperlakukan sebagai satuan besar`() {
        val item = ResepItemRingkas("daging", 0.15, satuanResep = null, satuanBesarBahan = "kg", faktorKonversi = 1000.0)
        assertEquals(150.0, item.kebutuhanSmallest!!, 0.0001)
    }

    @Test
    fun `faktor konversi tidak dapat dipercaya menghasilkan null bukan tebakan`() {
        val item = ResepItemRingkas("daging", 0.15, satuanResep = "kg", satuanBesarBahan = "kg", faktorKonversi = null)
        assertNull(item.kebutuhanSmallest)
        assertNull(itemBesar("daging", 0.0).kebutuhanSmallest)
    }

    // ------------------------------------------------------------------- estimasi

    @Test
    fun `porsi dibatasi bahan paling sedikit dan bahan itu jadi penghambat`() {
        val hasil = ProduksiEstimator.estimasi(
            resep = resep(itemKecil("roti", 1.0), itemKecil("daging", 100.0)),
            saldoNormPerBahan = mapOf("roti" to 50.0, "daging" to 1200.0),
            namaPerBahan = mapOf("daging" to "Daging Sapi"),
        )
        assertEquals(12, hasil.porsi)
        assertEquals("daging", hasil.bottleneckBahanId)
        assertEquals("Daging Sapi", hasil.bottleneckNama)
        assertTrue(hasil.lengkap)
    }

    /**
     * Resep yang ditulis pada satuan besar harus menghasilkan porsi yang sama dengan
     * resep setara yang ditulis pada satuan kecil. Kalau ini gagal, angka porsi meleset
     * sebesar faktor konversi — persis bug yang ada di perhitungan web.
     */
    @Test
    fun `resep satuan besar dan satuan kecil yang setara menghasilkan porsi sama`() {
        val saldo = mapOf("daging" to 1200.0)
        val pakaiKecil = ProduksiEstimator.estimasi(resep(itemKecil("daging", 100.0)), saldo)
        val pakaiBesar = ProduksiEstimator.estimasi(resep(itemBesar("daging", 0.1)), saldo)
        assertEquals(12, pakaiKecil.porsi)
        assertEquals(pakaiKecil.porsi, pakaiBesar.porsi)
    }

    @Test
    fun `bahan tanpa data saldo tidak dianggap nol tapi menandai hasil belum utuh`() {
        val hasil = ProduksiEstimator.estimasi(
            resep = resep(itemKecil("roti", 1.0), itemKecil("saus", 10.0)),
            saldoNormPerBahan = mapOf("roti" to 30.0),
        )
        assertEquals(30, hasil.porsi)
        assertFalse(hasil.lengkap)
    }

    @Test
    fun `kebutuhan nol tidak menyebabkan pembagian nol`() {
        val hasil = ProduksiEstimator.estimasi(
            resep = resep(itemKecil("roti", 0.0), itemKecil("daging", 50.0)),
            saldoNormPerBahan = mapOf("roti" to 30.0, "daging" to 500.0),
        )
        assertEquals(10, hasil.porsi)
        assertFalse(hasil.lengkap)
    }

    @Test
    fun `saldo kurang dari satu porsi menghasilkan nol`() {
        val hasil = ProduksiEstimator.estimasi(
            resep = resep(itemKecil("daging", 100.0)),
            saldoNormPerBahan = mapOf("daging" to 40.0),
        )
        assertEquals(0, hasil.porsi)
    }

    // ------------------------------------------------------- pemilihan resep aktif

    @Test
    fun `resep khusus outlet mengalahkan resep global bernama sama`() {
        val global = ResepRingkas("g1", "Shawarma", null, listOf(itemKecil("daging", 100.0)))
        val outlet = ResepRingkas("o1", "shawarma", "outlet-a", listOf(itemKecil("daging", 50.0)))
        val terpilih = ProduksiEstimator.pilihResepBerlaku(listOf(global, outlet))
        assertEquals(1, terpilih.size)
        assertEquals("o1", terpilih.first().id)
    }

    @Test
    fun `resep tanpa padanan outlet tetap ikut`() {
        val a = ResepRingkas("g1", "Shawarma", null, emptyList())
        val b = ResepRingkas("g2", "Kebab", null, emptyList())
        assertEquals(2, ProduksiEstimator.pilihResepBerlaku(listOf(a, b)).size)
    }
}
