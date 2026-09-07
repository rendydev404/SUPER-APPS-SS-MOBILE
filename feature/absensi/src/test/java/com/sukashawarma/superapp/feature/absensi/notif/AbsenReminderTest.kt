package com.sukashawarma.superapp.feature.absensi.notif

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class AbsenReminderTest {

    private fun waktu(hari: Int, jam: Int, menit: Int, detik: Int = 0): Long =
        Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, hari, jam, menit, detik)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun bagian(millis: Long): Triple<Int, Int, Int> =
        Calendar.getInstance().apply { timeInMillis = millis }.let {
            Triple(it.get(Calendar.DAY_OF_MONTH), it.get(Calendar.HOUR_OF_DAY), it.get(Calendar.MINUTE))
        }

    @Test fun `pagi hari dijadwalkan siang ini juga`() {
        assertEquals(Triple(5, 12, 10), bagian(AbsenReminder.waktuBerikutnya(waktu(5, 8, 0))))
    }

    @Test fun `sesudah lewat dijadwalkan besok, bukan hari ini`() {
        assertEquals(Triple(6, 12, 10), bagian(AbsenReminder.waktuBerikutnya(waktu(5, 12, 11))))
    }

    /** Tepat pada detik tayang dianggap sudah lewat, supaya alarm yang baru saja
     *  berbunyi tidak menjadwalkan dirinya sendiri di detik yang sama dan berulang. */
    @Test fun `tepat pukul 12 10 melompat ke besok`() {
        assertEquals(Triple(6, 12, 10), bagian(AbsenReminder.waktuBerikutnya(waktu(5, 12, 10))))
    }

    @Test fun `beberapa detik sebelum tayang masih hari ini`() {
        assertEquals(Triple(5, 12, 10), bagian(AbsenReminder.waktuBerikutnya(waktu(5, 12, 9, 59))))
    }

    private fun hariEpoch(millis: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis / 86_400_000L

    @Test fun `perangkat menyala sesudah 12 10 menayangkan susulan`() {
        assertTrue(AbsenReminder.perluSusulan(waktu(5, 14, 30), terakhirTayang = -1))
    }

    @Test fun `menyala sebelum 12 10 tidak menayangkan apa-apa`() {
        assertFalse(AbsenReminder.perluSusulan(waktu(5, 9, 0), terakhirTayang = -1))
    }

    @Test fun `yang sudah tayang hari ini tidak diulang`() {
        val sekarang = waktu(5, 14, 30)
        assertFalse(AbsenReminder.perluSusulan(sekarang, terakhirTayang = hariEpoch(sekarang)))
    }

    /** Tayangan kemarin tidak boleh menahan susulan hari ini. */
    @Test fun `catatan kemarin tidak menahan susulan hari ini`() {
        val sekarang = waktu(5, 14, 30)
        assertTrue(AbsenReminder.perluSusulan(sekarang, terakhirTayang = hariEpoch(sekarang) - 1))
    }

    @Test fun `jadwal berikutnya selalu di masa depan`() {
        listOf(waktu(5, 0, 0), waktu(5, 12, 10), waktu(5, 23, 59)).forEach { sekarang ->
            assertTrue(AbsenReminder.waktuBerikutnya(sekarang) > sekarang)
        }
    }
}
