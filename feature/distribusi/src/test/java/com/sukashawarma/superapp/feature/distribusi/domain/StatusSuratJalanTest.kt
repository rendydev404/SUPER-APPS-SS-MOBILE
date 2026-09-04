package com.sukashawarma.superapp.feature.distribusi.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private data class ItemUji(
    override val qtyDikirim: Double,
    override val qtyTerima: Double?,
    override val kondisi: String?,
) : PenandaSelisih

class StatusSuratJalanTest {

    @Test
    fun `keenam nilai status dikenali`() {
        assertEquals(StatusSuratJalan.DRAFT, StatusSuratJalan.dari("draft"))
        assertEquals(StatusSuratJalan.DIKIRIM, StatusSuratJalan.dari("dikirim"))
        assertEquals(StatusSuratJalan.DIKIRIM_LENGKAP, StatusSuratJalan.dari("dikirim_lengkap"))
        assertEquals(StatusSuratJalan.DITERIMA_SEBAGIAN, StatusSuratJalan.dari("diterima_sebagian"))
        assertEquals(StatusSuratJalan.DITERIMA_LENGKAP, StatusSuratJalan.dari("diterima_lengkap"))
        assertEquals(StatusSuratJalan.SELESAI, StatusSuratJalan.dari("selesai"))
    }

    @Test
    fun `nilai tak dikenal jadi null, bukan lempar`() {
        assertNull(StatusSuratJalan.dari("diterima"))
        assertNull(StatusSuratJalan.dari(null))
        assertNull(StatusSuratJalan.dari(""))
    }

    @Test
    fun `label berbahasa Indonesia`() {
        assertEquals("Draft", StatusSuratJalan.DRAFT.label)
        assertEquals("Dalam Transit", StatusSuratJalan.DIKIRIM.label)
        assertEquals("Dalam Transit", StatusSuratJalan.DIKIRIM_LENGKAP.label)
        assertEquals("Diterima Sebagian", StatusSuratJalan.DITERIMA_SEBAGIAN.label)
        assertEquals("Diterima Lengkap", StatusSuratJalan.DITERIMA_LENGKAP.label)
        assertEquals("Selesai", StatusSuratJalan.SELESAI.label)
    }

    @Test
    fun `hanya status transit dan diterima sebagian yang boleh diverifikasi`() {
        assertTrue(StatusSuratJalan.DIKIRIM.bolehDiverifikasi)
        assertTrue(StatusSuratJalan.DIKIRIM_LENGKAP.bolehDiverifikasi)
        assertTrue(StatusSuratJalan.DITERIMA_SEBAGIAN.bolehDiverifikasi)
        assertFalse(StatusSuratJalan.DRAFT.bolehDiverifikasi)
        assertFalse(StatusSuratJalan.DITERIMA_LENGKAP.bolehDiverifikasi)
        assertFalse(StatusSuratJalan.SELESAI.bolehDiverifikasi)
    }

    @Test
    fun `hanya yang sudah diterima yang boleh ditutup jadi selesai`() {
        assertTrue(StatusSuratJalan.DITERIMA_LENGKAP.bolehDitutup)
        assertTrue(StatusSuratJalan.DITERIMA_SEBAGIAN.bolehDitutup)
        assertFalse(StatusSuratJalan.SELESAI.bolehDitutup)
        assertFalse(StatusSuratJalan.DIKIRIM.bolehDitutup)
        assertFalse(StatusSuratJalan.DRAFT.bolehDitutup)
    }

    @Test
    fun `sudah diterima mencakup selesai`() {
        assertTrue(StatusSuratJalan.DITERIMA_LENGKAP.sudahDiterima)
        assertTrue(StatusSuratJalan.DITERIMA_SEBAGIAN.sudahDiterima)
        assertTrue(StatusSuratJalan.SELESAI.sudahDiterima)
        assertFalse(StatusSuratJalan.DIKIRIM.sudahDiterima)
    }

    @Test
    fun `item rusak dihitung selisih`() {
        assertTrue(adaSelisih(listOf(ItemUji(10.0, 10.0, "rusak"))))
    }

    @Test
    fun `qty terima kurang dari dikirim dihitung selisih`() {
        assertTrue(adaSelisih(listOf(ItemUji(10.0, 9.5, "baik"))))
    }

    @Test
    fun `qty terima pas tidak dihitung selisih`() {
        assertFalse(adaSelisih(listOf(ItemUji(10.0, 10.0, "baik"))))
    }

    /** Item yang belum diverifikasi (qty_terima null) BUKAN selisih. Kalau dihitung,
     *  setiap surat jalan yang baru dikirim akan tampak bermasalah di dashboard. */
    @Test
    fun `item belum diverifikasi bukan selisih`() {
        assertFalse(adaSelisih(listOf(ItemUji(10.0, null, null))))
    }

    @Test
    fun `satu item bermasalah menandai seluruh surat jalan`() {
        val items = listOf(
            ItemUji(10.0, 10.0, "baik"),
            ItemUji(5.0, 3.0, "baik"),
            ItemUji(2.0, 2.0, "baik"),
        )
        assertTrue(adaSelisih(items))
    }

    @Test
    fun `daftar kosong tidak bermasalah`() {
        assertFalse(adaSelisih(emptyList()))
    }
}
