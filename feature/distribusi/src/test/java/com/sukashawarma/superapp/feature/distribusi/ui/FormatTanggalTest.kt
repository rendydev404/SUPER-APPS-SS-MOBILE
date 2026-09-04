package com.sukashawarma.superapp.feature.distribusi.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTanggalTest {

    @Test
    fun `iso dari postgrest jadi tanggal Indonesia`() {
        assertEquals("4 Sep 2026", formatTanggal("2026-09-04T03:15:00+00:00"))
    }

    @Test
    fun `iso dengan Z juga dikenali`() {
        assertEquals("4 Sep 2026", formatTanggal("2026-09-04T03:15:00Z"))
    }

    /** Baris lama bisa punya timestamp tanpa zona waktu. */
    @Test
    fun `timestamp tanpa zona tetap terbaca`() {
        assertEquals("4 Sep 2026", formatTanggal("2026-09-04T03:15:00"))
    }

    @Test
    fun `null dan teks rusak jadi tanda hubung, bukan lemparan`() {
        assertEquals("-", formatTanggal(null))
        assertEquals("-", formatTanggal(""))
        assertEquals("-", formatTanggal("bukan tanggal"))
    }
}
