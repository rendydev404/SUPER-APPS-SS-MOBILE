package com.sukashawarma.superapp.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Jeda ini yang membatasi berapa kali perangkat bermasalah mengetuk server. Kalau batas
 * atasnya bocor, satu HP dengan sesi mati kembali membanjiri gateway seperti sebelumnya.
 */
class JedaCobaUlangTest {

    @Test
    fun `tanpa kegagalan tidak ada jeda`() {
        assertEquals(0L, JedaCobaUlang.untuk(0))
        assertEquals(0L, JedaCobaUlang.untuk(-3))
    }

    @Test
    fun `jeda berlipat dua setiap kegagalan`() {
        assertEquals(30_000L, JedaCobaUlang.untuk(1))
        assertEquals(60_000L, JedaCobaUlang.untuk(2))
        assertEquals(120_000L, JedaCobaUlang.untuk(3))
        assertEquals(480_000L, JedaCobaUlang.untuk(5))
    }

    @Test
    fun `jeda tidak pernah melewati batas atas`() {
        assertEquals(JedaCobaUlang.MAKS_MS, JedaCobaUlang.untuk(6))
        assertEquals(JedaCobaUlang.MAKS_MS, JedaCobaUlang.untuk(1_000))
    }
}
