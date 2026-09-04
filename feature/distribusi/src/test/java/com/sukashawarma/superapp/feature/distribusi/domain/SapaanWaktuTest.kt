package com.sukashawarma.superapp.feature.distribusi.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class SapaanWaktuTest {

    @Test
    fun `ambang sapaan sama dengan web`() {
        assertEquals("Selamat Pagi", salamUntukJam(0))
        assertEquals("Selamat Pagi", salamUntukJam(10))
        assertEquals("Selamat Siang", salamUntukJam(11))
        assertEquals("Selamat Siang", salamUntukJam(14))
        assertEquals("Selamat Sore", salamUntukJam(15))
        assertEquals("Selamat Sore", salamUntukJam(17))
        assertEquals("Selamat Malam", salamUntukJam(18))
        assertEquals("Selamat Malam", salamUntukJam(23))
    }

    @Test
    fun `tanggal panjang berbahasa Indonesia`() {
        assertEquals("Jumat, 4 September 2026", tanggalPanjang(LocalDate.of(2026, 9, 4)))
        assertEquals("Senin, 1 Januari 2024", tanggalPanjang(LocalDate.of(2024, 1, 1)))
        assertEquals("Minggu, 31 Desember 2023", tanggalPanjang(LocalDate.of(2023, 12, 31)))
    }

    /** Indeks larik hari dan bulan gampang meleset satu langkah; kedua ujungnya
     *  diuji supaya kesalahan itu ketahuan, bukan diam-diam salah sepanjang tahun. */
    @Test
    fun `ujung larik hari dan bulan tidak meleset`() {
        // 2 Maret 2026 adalah Senin, hari pertama larik.
        assertEquals("Senin, 2 Maret 2026", tanggalPanjang(LocalDate.of(2026, 3, 2)))
        // 8 Maret 2026 adalah Minggu, hari terakhir larik.
        assertEquals("Minggu, 8 Maret 2026", tanggalPanjang(LocalDate.of(2026, 3, 8)))
    }
}
