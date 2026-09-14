package com.sukashawarma.superapp.feature.absensi.ui.clock

import com.sukashawarma.superapp.domain.gps.GpsMath
import com.sukashawarma.superapp.presentation.absensi.clock.formatDistanceInMessage
import org.junit.Assert.assertEquals
import org.junit.Test

class ClockDistanceFormatTest {

    @Test
    fun formatDistance_underOneKilometer_returnsMeters() {
        assertEquals("0 m", GpsMath.formatDistance(0.0))
        assertEquals("50 m", GpsMath.formatDistance(50.4))
        assertEquals("250 m", GpsMath.formatDistance(250.0))
        assertEquals("999 m", GpsMath.formatDistance(999.0))
    }

    @Test
    fun formatDistance_oneKilometerOrAbove_returnsKilometers() {
        assertEquals("1 km", GpsMath.formatDistance(1000.0))
        assertEquals("1.5 km", GpsMath.formatDistance(1500.0))
        assertEquals("8.5 km", GpsMath.formatDistance(8502.0))
        assertEquals("10 km", GpsMath.formatDistance(10000.0))
        assertEquals("12.3 km", GpsMath.formatDistance(12345.0))
    }

    @Test
    fun formatDistanceShort_worksWithoutSpace() {
        assertEquals("250m", GpsMath.formatDistanceShort(250.0))
        assertEquals("8.5km", GpsMath.formatDistanceShort(8502.0))
        assertEquals("1km", GpsMath.formatDistanceShort(1000.0))
    }

    @Test
    fun formatDistanceInMessage_replacesMetersOver1000WithKm() {
        val input = "Di luar jangkauan (jarak 8502 m). Silakan mendekat ke area kasir."
        val expected = "Di luar jangkauan (jarak 8.5 km). Silakan mendekat ke area kasir."
        assertEquals(expected, formatDistanceInMessage(input))
    }

    @Test
    fun formatDistanceInMessage_leavesMetersUnder1000Intact() {
        val input = "Di luar jangkauan (jarak 250 m). Silakan mendekat ke area kasir."
        assertEquals(input, formatDistanceInMessage(input))
    }

    @Test
    fun formatDistanceInMessage_replacesHighAccuracyMeters() {
        val input = "Akurasi GPS terlalu rendah (1500 m). Aktifkan Lokasi Akurat."
        val expected = "Akurasi GPS terlalu rendah (1.5 km). Aktifkan Lokasi Akurat."
        assertEquals(expected, formatDistanceInMessage(input))
    }

    @Test
    fun formatDistanceInMessage_handlesNoDistanceString() {
        val input = "Lokasi Anda belum berada di area outlet."
        assertEquals(input, formatDistanceInMessage(input))
    }
}
