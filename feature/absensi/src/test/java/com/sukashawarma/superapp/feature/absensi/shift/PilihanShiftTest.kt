package com.sukashawarma.superapp.feature.absensi.shift

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PilihanShiftTest {

    private val cfgBnr = ShiftConfig("08:00:00", "17:00:00", true, "13:00:00", "22:00:00")
    private val opsiBnr = shiftOptions(cfgBnr)
    private val opsiLewatMalam = listOf(ShiftOption(1, "10:00", "22:00"), ShiftOption(2, "18:00", "02:00"))

    @Test
    fun `toggle aktif dengan jam lengkap menghasilkan dua pilihan berformat HH-MM`() {
        assertEquals(
            listOf(ShiftOption(1, "08:00", "17:00"), ShiftOption(2, "13:00", "22:00")),
            opsiBnr,
        )
    }

    @Test
    fun `toggle mati atau config kosong berarti outlet satu shift`() {
        assertNull(shiftOptions(null))
        assertNull(shiftOptions(cfgBnr.copy(pilihShiftAktif = false)))
        assertNull(shiftOptions(cfgBnr.copy(pilihShiftAktif = null)))
    }

    @Test
    fun `jam shift yang belum lengkap tidak menampilkan pilihan setengah jadi`() {
        assertNull(shiftOptions(cfgBnr.copy(shift2JamKeluar = null)))
        assertNull(shiftOptions(cfgBnr.copy(jamMasuk = "")))
    }

    @Test
    fun `sebutan shift mengikuti jam masuk`() {
        assertEquals("Shift Pagi", namaShift("08:00"))
        assertEquals("Shift Pagi", namaShift("10:59"))
        assertEquals("Shift Siang", namaShift("11:00"))
        assertEquals("Shift Siang", namaShift("14:30"))
        assertEquals("Shift Malam", namaShift("15:00"))
    }

    @Test
    fun `hanya angka satu atau dua yang sah`() {
        assertTrue(isShiftKe(1))
        assertTrue(isShiftKe(2))
        assertFalse(isShiftKe(3))
        assertFalse(isShiftKe(null))
    }

    @Test
    fun `F1 shift yang pulang paling akhir adalah penutup`() {
        assertTrue(isShiftPenutup(opsiBnr, "22:00"))
        assertTrue(isShiftPenutup(opsiBnr, "22:00:00"))
    }

    @Test
    fun `F2 shift pagi bukan penutup`() {
        assertFalse(isShiftPenutup(opsiBnr, "17:00"))
    }

    @Test
    fun `F3 outlet satu shift atau tanpa jejak shift dianggap penutup`() {
        assertTrue(isShiftPenutup(null, "17:00"))
        assertTrue(isShiftPenutup(opsiBnr, null))
        assertTrue(isShiftPenutup(opsiBnr, ""))
    }

    @Test
    fun `jam pulang yang tidak cocok dengan shift mana pun dianggap penutup`() {
        assertTrue(isShiftPenutup(opsiBnr, "19:00"))
    }

    @Test
    fun `F4 shift yang pulang lewat tengah malam dihitung besok`() {
        assertTrue(isShiftPenutup(opsiLewatMalam, "02:00"))
        assertFalse(isShiftPenutup(opsiLewatMalam, "22:00"))
    }
}
