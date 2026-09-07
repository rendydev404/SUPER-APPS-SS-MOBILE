package com.sukashawarma.superapp.feature.manager.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class PeriodeTest {

    private val kamis = LocalDate.of(2026, 9, 3)

    @Test
    fun `hari ini adalah rentang satu hari`() {
        val r = PresetPeriode.HARI_INI.rentang(kamis)
        assertEquals(RentangTanggal(kamis, kamis), r)
        assertEquals(1L, r.jumlahHari)
    }

    @Test
    fun `kemarin tidak menyertakan hari ini`() {
        val r = PresetPeriode.KEMARIN.rentang(kamis)
        assertEquals(RentangTanggal(LocalDate.of(2026, 9, 2), LocalDate.of(2026, 9, 2)), r)
    }

    @Test
    fun `tujuh hari mencakup hari ini plus enam hari ke belakang`() {
        val r = PresetPeriode.MINGGU.rentang(kamis)
        assertEquals(RentangTanggal(LocalDate.of(2026, 8, 28), kamis), r)
        assertEquals(7L, r.jumlahHari)
    }

    @Test
    fun `tiga puluh hari mencakup hari ini plus dua puluh sembilan hari ke belakang`() {
        assertEquals(30L, PresetPeriode.BULAN.rentang(kamis).jumlahHari)
    }

    @Test
    fun `alias 7d dan 30d menghasilkan rentang yang sama dengan week dan month`() {
        assertEquals(PresetPeriode.MINGGU.rentang(kamis), PresetPeriode.TUJUH_HARI.rentang(kamis))
        assertEquals(PresetPeriode.BULAN.rentang(kamis), PresetPeriode.TIGA_PULUH_HARI.rentang(kamis))
    }

    @Test
    fun `bulan ini mulai dari tanggal satu`() {
        val r = PresetPeriode.BULAN_INI.rentang(kamis)
        assertEquals(RentangTanggal(LocalDate.of(2026, 9, 1), kamis), r)
    }

    @Test
    fun `preset tak dikenal jatuh ke hari ini`() {
        assertEquals(PresetPeriode.HARI_INI, PresetPeriode.dari("bulan-depan"))
        assertEquals(PresetPeriode.HARI_INI, PresetPeriode.dari(null))
    }

    /** Badge "% dari periode sebelumnya" salah kalau pembandingnya bukan sepanjang rentang utama. */
    @Test
    fun `rentang sebelumnya sama panjang dan menempel di depan`() {
        val utama = PresetPeriode.MINGGU.rentang(kamis)
        val lalu = rentangSebelumnya(utama)
        assertEquals(utama.jumlahHari, lalu.jumlahHari)
        assertEquals(utama.dari.minusDays(1), lalu.sampai)
        assertEquals(LocalDate.of(2026, 8, 21), lalu.dari)
    }

    @Test
    fun `rentang sebelumnya dari hari ini adalah kemarin`() {
        val lalu = rentangSebelumnya(PresetPeriode.HARI_INI.rentang(kamis))
        assertEquals(RentangTanggal(LocalDate.of(2026, 9, 2), LocalDate.of(2026, 9, 2)), lalu)
    }

    @Test
    fun `batas iso memakai offset Jakarta`() {
        val r = PresetPeriode.HARI_INI.rentang(kamis)
        assertEquals("2026-09-03T00:00:00+07:00", r.awalIso())
        assertEquals("2026-09-03T23:59:59.999+07:00", r.akhirIso())
    }
}
