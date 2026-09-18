package com.sukashawarma.superapp.feature.stok.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeliveryUnitsTest {

    @Test
    fun `mayonaise dikonversi ke kg dengan faktor 12`() {
        assertEquals("+12 kg", DeliveryUnits.format(1.0, "MAYONAISE", isSaldo = false))
        assertEquals("-24 kg", DeliveryUnits.format(-2.0, "Mayonaise", isSaldo = false))
        assertEquals("12 kg", DeliveryUnits.format(1.0, "MAYONES", isSaldo = true))
    }

    @Test
    fun `saos cabe dikonversi dengan faktor 16 koma 5`() {
        assertEquals("+16.5 kg", DeliveryUnits.format(1.0, "SAOS CABE", isSaldo = false))
        assertEquals("+33 kg", DeliveryUnits.format(2.0, "saos cabe", isSaldo = false))
    }

    @Test
    fun `keju dikonversi ke pack dengan faktor 24`() {
        assertEquals("+24 pack", DeliveryUnits.format(1.0, "KEJU", isSaldo = false))
        assertEquals("-48 pack", DeliveryUnits.format(-2.0, "Keju", isSaldo = false))
    }

    @Test
    fun `stiker dikonversi ke lembar dengan faktor 100`() {
        assertEquals("+100 lembar", DeliveryUnits.format(1.0, "STIKER", isSaldo = false))
    }

    @Test
    fun `bahan tanpa mapping menghasilkan null`() {
        assertNull(DeliveryUnits.format(1.0, "BAHAN_TIDAK_ADA", isSaldo = false))
        assertNull(DeliveryUnits.format(1.0, null, isSaldo = false))
        assertNull(DeliveryUnits.format(1.0, "", isSaldo = false))
    }

    @Test
    fun `hasMapping mendeteksi nama bahan dengan benar`() {
        assertTrue(DeliveryUnits.hasMapping("AYAM"))
        assertTrue(DeliveryUnits.hasMapping("sapi"))
        assertTrue(DeliveryUnits.hasMapping("KENTANG"))
        assertFalse(DeliveryUnits.hasMapping("UNREGISTERED_ITEM"))
    }
}
