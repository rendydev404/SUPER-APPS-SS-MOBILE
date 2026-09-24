package com.sukashawarma.superapp.feature.distribusi.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Kasus-kasus `alokasiVendor.test.ts` web, dipindah ke Kotlin. */
class AlokasiVendorTest {

    private val a = SaldoVendor("va", "Vendor A", 10.0, aktif = true)
    private val b = SaldoVendor("vb", "Vendor B", 25.0, aktif = true)
    private val c = SaldoVendor("vc", "Vendor C", 0.0, aktif = false)

    @Test
    fun `satu vendor langsung dipakai walau saldonya kurang`() {
        assertEquals(listOf(Alokasi("va", 50.0)), AlokasiVendor.alokasiAwal(50.0, listOf(a)))
    }

    @Test
    fun `beberapa vendor memilih sisa terbesar yang cukup`() {
        assertEquals(listOf(Alokasi("vb", 8.0)), AlokasiVendor.alokasiAwal(8.0, listOf(a, b)))
    }

    @Test
    fun `tidak ada vendor yang cukup menghasilkan kosong`() {
        assertTrue(AlokasiVendor.alokasiAwal(30.0, listOf(a, b)).isEmpty())
    }

    @Test
    fun `vendor belum dihitung tidak dipilih otomatis`() {
        assertTrue(AlokasiVendor.alokasiAwal(5.0, listOf(c, SaldoVendor("vd", "D", 1.0, true))).isEmpty())
    }

    @Test
    fun `satu vendor tidak perlu divalidasi`() {
        assertNull(AlokasiVendor.validasi(5.0, emptyList(), listOf(a)))
    }

    @Test
    fun `pesan validasi sama dengan web`() {
        val v = listOf(a, b, c)
        assertEquals("Pilih vendor dulu", AlokasiVendor.validasi(5.0, emptyList(), v))
        assertEquals("Vendor tidak dikenal", AlokasiVendor.validasi(5.0, listOf(Alokasi("x", 5.0)), v))
        assertEquals(
            "Vendor A dipilih dua kali",
            AlokasiVendor.validasi(5.0, listOf(Alokasi("va", 2.0), Alokasi("va", 3.0)), v),
        )
        assertEquals("Jumlah Vendor A harus lebih dari 0", AlokasiVendor.validasi(5.0, listOf(Alokasi("va", 0.0)), v))
        assertEquals("Sisa Vendor A tinggal 10", AlokasiVendor.validasi(12.0, listOf(Alokasi("va", 12.0)), v))
        assertEquals(
            "Jumlah per vendor (7) belum sama dengan 9",
            AlokasiVendor.validasi(9.0, listOf(Alokasi("va", 3.0), Alokasi("vb", 4.0)), v),
        )
    }

    @Test
    fun `vendor belum dihitung tidak dibatasi sisanya`() {
        assertNull(AlokasiVendor.validasi(100.0, listOf(Alokasi("vc", 100.0)), listOf(a, c)))
    }

    @Test
    fun `pecahan yang pas lolos`() {
        assertNull(AlokasiVendor.validasi(30.0, listOf(Alokasi("va", 10.0), Alokasi("vb", 20.0)), listOf(a, b)))
    }

    @Test
    fun `saldo satuan besar diubah ke satuan distribusi dan dibulatkan ke bawah`() {
        val v = SaldoVendor("va", "A", 1.23456, true)
        assertEquals(12.345, AlokasiVendor.keSatuanDistribusi(v, 10.0).sisa, 1e-9)
    }

    @Test
    fun `multi vendor dari jumlah baris atau penanda multi`() {
        assertFalse(AlokasiVendor.multiVendor(listOf(a)))
        assertTrue(AlokasiVendor.multiVendor(listOf(a, b)))
        assertTrue(AlokasiVendor.multiVendor(listOf(a.copy(multi = true))))
    }

    /** 0.29 × 100 = 28.999999… di floating point; tanpa toleransi qty 29 ditolak. */
    @Test
    fun `pembulatan saldo tidak memotong qty yang pas`() {
        val v = SaldoVendor("va", "A", 0.29, true)
        assertEquals(29.0, AlokasiVendor.keSatuanDistribusi(v, 100.0).sisa, 1e-9)
        assertNull(AlokasiVendor.validasi(29.0, listOf(Alokasi("va", 29.0)), listOf(AlokasiVendor.keSatuanDistribusi(v, 100.0), b)))
    }
}
