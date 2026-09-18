package com.sukashawarma.superapp.feature.stok.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DropShipTest {

    private val sayur = FaktorBahanImpl(
        faktorTengah = null,
        faktorTampilan = 1000.0, // 1 kg = 1000 gram
    )

    private val saus = FaktorBahanImpl(
        faktorTengah = 5.0, // 1 jeriken = 5 botol
        faktorTampilan = 2500.0, // 1 jeriken = 2500 ml
    )

    @Test
    fun `keSatuanBesar mengonversi satuan kecil dan tengah dengan benar`() {
        // Satuan besar: qty langsung
        assertEquals(5.0, DropShip.keSatuanBesar(5.0, SatuanTingkat.BESAR, sayur), 0.0001)

        // Satuan kecil (gram ke kg)
        assertEquals(2.5, DropShip.keSatuanBesar(2500.0, SatuanTingkat.KECIL, sayur), 0.0001)

        // Satuan tengah (botol ke jeriken)
        assertEquals(2.0, DropShip.keSatuanBesar(10.0, SatuanTingkat.TENGAH, saus), 0.0001)
    }

    @Test
    fun `keSatuanBesar melempar exception bila faktor satuan tidak ada atau tidak valid`() {
        // Sayur tidak punya faktor tengah
        val ex = assertThrows(IllegalArgumentException::class.java) {
            DropShip.keSatuanBesar(3.0, SatuanTingkat.TENGAH, sayur)
        }
        assertTrue(ex.message!!.contains("tidak punya satuan tengah"))

        val bahanCacat = FaktorBahanImpl(faktorTengah = 0.0, faktorTampilan = -1.0)
        assertThrows(IllegalArgumentException::class.java) {
            DropShip.keSatuanBesar(1.0, SatuanTingkat.TENGAH, bahanCacat)
        }
        assertThrows(IllegalArgumentException::class.java) {
            DropShip.keSatuanBesar(1.0, SatuanTingkat.KECIL, bahanCacat)
        }
    }

    @Test
    fun `perluKonfirmasiJumlah mendeteksi nilai di atas 2 juta rupiah`() {
        val hargaPerKg = 25_000.0
        // 50 kg * 25.000 = 1.250.000 (tidak butuh konfirmasi)
        assertFalse(DropShip.perluKonfirmasiJumlah(50.0, hargaPerKg, rataPakaiHarian = 20.0))

        // 90 kg * 25.000 = 2.250.000 (> 2.000.000 -> butuh konfirmasi)
        assertTrue(DropShip.perluKonfirmasiJumlah(90.0, hargaPerKg, rataPakaiHarian = 30.0))
    }

    @Test
    fun `perluKonfirmasiJumlah mendeteksi kuantitas di atas 5 kali rata-rata pemakaian`() {
        val hargaPerKg = 10_000.0
        val rataHarian = 10.0

        // 40 kg (< 5x rata-rata dan nilai 400.000 < 2 jt)
        assertFalse(DropShip.perluKonfirmasiJumlah(40.0, hargaPerKg, rataPakaiHarian = rataHarian))

        // 55 kg (> 5x 10 kg rata-rata -> butuh konfirmasi meski nilai Rp 550.000 < 2 jt)
        assertTrue(DropShip.perluKonfirmasiJumlah(55.0, hargaPerKg, rataPakaiHarian = rataHarian))
    }

    @Test
    fun `perluKonfirmasiJumlah menangani qty nol dan rata-rata pemakaian null atau nol`() {
        assertFalse(DropShip.perluKonfirmasiJumlah(0.0, 25_000.0, 10.0))
        assertFalse(DropShip.perluKonfirmasiJumlah(-5.0, 25_000.0, 10.0))

        // Jika rataPakaiHarian null, hanya mengandalkan ambang rupiah
        assertFalse(DropShip.perluKonfirmasiJumlah(10.0, 25_000.0, null))
        assertTrue(DropShip.perluKonfirmasiJumlah(100.0, 25_000.0, null))
    }

    @Test
    fun `pilihanTanggalTerima mencakup hari ini sampai 3 hari lalu`() {
        val hariIni = LocalDate.of(2026, 9, 17)
        val pilihan = DropShip.pilihanTanggalTerima(hariIni)

        assertEquals(4, pilihan.size)
        assertEquals("2026-09-17", pilihan[0].value)
        assertEquals("Hari ini", pilihan[0].label)

        assertEquals("2026-09-16", pilihan[1].value)
        assertEquals("Kemarin", pilihan[1].label)

        assertEquals("2026-09-15", pilihan[2].value)
        assertEquals("2 hari lalu", pilihan[2].label)

        assertEquals("2026-09-14", pilihan[3].value)
        assertEquals("3 hari lalu", pilihan[3].label)
    }

    @Test
    fun `bisaTerimaVendor memeriksa kepemilikan outlet id`() {
        assertTrue(StokAkses.bisaTerimaVendor("43b7bbd1-1fd4-44b5-87ca-b07a271151af"))
        assertFalse(StokAkses.bisaTerimaVendor(null))
        assertFalse(StokAkses.bisaTerimaVendor(""))
        assertFalse(StokAkses.bisaTerimaVendor("   "))
    }
}
