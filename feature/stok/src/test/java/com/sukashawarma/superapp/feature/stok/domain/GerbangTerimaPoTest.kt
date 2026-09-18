package com.sukashawarma.superapp.feature.stok.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Port `apps/stok/src/lib/stok/terimaGuard.test.ts`, kasus per kasus.
 *
 * Dua kejadian nyata 16 September 2026, dan keduanya hanya bisa diperbaiki manual
 * di database: `verifikasi_terima_po` memakai GREATEST(0, qty_baru - qty_lama),
 * jadi form terima TIDAK PERNAH bisa mengurangi.
 */
class GerbangTerimaPoTest {

    private fun cek(
        datang: Double,
        pesan: Double,
        sebelumnya: Double,
        stok: Double?,
    ) = GerbangTerimaPo.cek(datang, pesan, sebelumnya, stok)

    // ------------------------------------------------ dua kejadian nyata

    @Test
    fun `KULIT 32 stok dua pack diinput tujuh puluh tujuh pack`() {
        val w = cek(datang = 77.0, pesan = 300.0, sebelumnya = 0.0, stok = 2.0)
        assertEquals(1, w.size)
        val lompatan = w[0] as PeringatanTerima.LompatanStok
        assertEquals(38.5, lompatan.rasio, 0.0001)
        assertEquals(2.0, lompatan.stokSebelum, 0.0001)
    }

    @Test
    fun `TEPUNG seratus kilo diinput dua kali padahal pesanan sudah penuh`() {
        val w = cek(datang = 100.0, pesan = 100.0, sebelumnya = 100.0, stok = 136.0)
        assertEquals(1, w.size)
        val lebih = w[0] as PeringatanTerima.MelebihiPesanan
        assertEquals(100.0, lebih.kelebihan, 0.0001)
    }

    // --------------------------------- jalur mayoritas tidak boleh berisik

    @Test
    fun `kiriman bertahap yang wajar tidak memicu apa pun`() {
        assertTrue(cek(datang = 200.0, pesan = 1000.0, sebelumnya = 500.0, stok = 150.0).isEmpty())
    }

    @Test
    fun `terima pas sejumlah pesanan tidak memicu apa pun`() {
        assertTrue(cek(datang = 20.0, pesan = 20.0, sebelumnya = 0.0, stok = 5.0).isEmpty())
    }

    @Test
    fun `restock saat gudang benar-benar kosong bukan anomali`() {
        assertTrue(cek(datang = 500.0, pesan = 500.0, sebelumnya = 0.0, stok = 0.0).isEmpty())
    }

    @Test
    fun `stok belum termuat membuat aturan lompatan dilewati bukan ditebak`() {
        assertTrue(cek(datang = 500.0, pesan = 500.0, sebelumnya = 0.0, stok = null).isEmpty())
    }

    @Test
    fun `barang tidak datang tidak memicu apa pun`() {
        assertTrue(cek(datang = 0.0, pesan = 300.0, sebelumnya = 0.0, stok = 1.0).isEmpty())
    }

    @Test
    fun `tepat di ambang belum memicu sedikit di atasnya memicu`() {
        val tepat = cek(datang = 10.0 * GerbangTerimaPo.AMBANG_LOMPATAN_STOK, pesan = 999.0, sebelumnya = 0.0, stok = 10.0)
        assertTrue(tepat.isEmpty())
        val lewat = cek(datang = 10.0 * GerbangTerimaPo.AMBANG_LOMPATAN_STOK + 1, pesan = 999.0, sebelumnya = 0.0, stok = 10.0)
        assertTrue(lewat.single() is PeringatanTerima.LompatanStok)
    }

    @Test
    fun `kelebihan recehan dari pembulatan float tidak dianggap melebihi pesanan`() {
        assertTrue(cek(datang = 0.1 + 0.2, pesan = 0.3, sebelumnya = 0.0, stok = 5.0).isEmpty())
    }

    @Test
    fun `dua aturan bisa menyala bersamaan`() {
        val w = cek(datang = 90.0, pesan = 10.0, sebelumnya = 5.0, stok = 1.0)
        assertEquals(2, w.size)
        assertTrue(w.any { it is PeringatanTerima.LompatanStok })
        assertTrue(w.any { it is PeringatanTerima.MelebihiPesanan })
    }

    // ----------------------------------------------- akibat ke stok gudang

    @Test
    fun `stok setelah terima menjumlahkan stok berjalan dengan yang datang`() {
        assertEquals(79.0, GerbangTerimaPo.stokSetelah(2.0, 77.0)!!, 0.0001)
    }

    @Test
    fun `stok tak diketahui tetap tak diketahui bukan nol`() {
        assertNull(GerbangTerimaPo.stokSetelah(null, 77.0))
    }
}
