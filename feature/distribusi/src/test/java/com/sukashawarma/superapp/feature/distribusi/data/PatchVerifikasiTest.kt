package com.sukashawarma.superapp.feature.distribusi.data

import com.sukashawarma.superapp.feature.distribusi.domain.KondisiItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PatchVerifikasiTest {

    private val waktu = "2026-09-04T10:00:00Z"

    @Test
    fun `qty pas dan kondisi baik tidak ditandai`() {
        val patch = SuratJalanRepository.patchVerifikasi(
            qtyTerimaDasar = 10.0,
            qtyDikirimDasar = 10.0,
            kondisi = KondisiItem.BAIK,
            catatan = "",
            fotoPath = "sj1/item1.jpg",
            waktuIso = waktu,
        )
        assertEquals(10.0, patch.get("qty_terima").asDouble, 0.0001)
        assertEquals("baik", patch.get("kondisi").asString)
        assertFalse(patch.get("flagged").asBoolean)
        assertEquals("sj1/item1.jpg", patch.get("foto_path").asString)
        assertEquals(waktu, patch.get("verified_at").asString)
    }

    @Test
    fun `catatan kosong ditulis null, bukan string kosong`() {
        val patch = SuratJalanRepository.patchVerifikasi(
            10.0, 10.0, KondisiItem.BAIK, "  ", "sj1/item1.jpg", waktu,
        )
        assertTrue(patch.get("catatan").isJsonNull)
    }

    @Test
    fun `qty kurang menandai flagged walau kondisinya baik`() {
        val patch = SuratJalanRepository.patchVerifikasi(
            9.0, 10.0, KondisiItem.BAIK, "", "sj1/item1.jpg", waktu,
        )
        assertTrue(patch.get("flagged").asBoolean)
    }

    @Test
    fun `kondisi tidak sesuai menandai flagged walau qty pas`() {
        val patch = SuratJalanRepository.patchVerifikasi(
            10.0, 10.0, KondisiItem.TIDAK_SESUAI, "penyok", "sj1/item1.jpg", waktu,
        )
        assertTrue(patch.get("flagged").asBoolean)
        assertEquals("rusak", patch.get("kondisi").asString)
        assertEquals("penyok", patch.get("catatan").asString)
    }

    /** Kolom turunan dan kolom yang tidak ditulis web tidak boleh ikut terkirim.
     *  `selisih` ditolak Postgres, `harga_snapshot` milik trigger, dan
     *  `verified_by` tidak pernah diisi web — bentuk baris harus identik. */
    @Test
    fun `kolom terlarang tidak pernah disebut`() {
        val patch = SuratJalanRepository.patchVerifikasi(
            9.0, 10.0, KondisiItem.TIDAK_SESUAI, "kurang", "sj1/item1.jpg", waktu,
        )
        assertFalse(patch.has("selisih"))
        assertFalse(patch.has("harga_snapshot"))
        assertFalse(patch.has("verified_by"))
        assertFalse(patch.has("id"))
        assertFalse(patch.has("surat_jalan_id"))
        assertFalse(patch.has("qty_dikirim"))
    }

    @Test
    fun `patch memuat tepat enam kolom`() {
        val patch = SuratJalanRepository.patchVerifikasi(
            10.0, 10.0, KondisiItem.BAIK, "", "sj1/item1.jpg", waktu,
        )
        assertEquals(
            setOf("qty_terima", "kondisi", "catatan", "flagged", "foto_path", "verified_at"),
            patch.keySet(),
        )
    }

    @Test
    fun `foto path null tetap ditulis null`() {
        val patch = SuratJalanRepository.patchVerifikasi(
            10.0, 10.0, KondisiItem.BAIK, "", null, waktu,
        )
        assertTrue(patch.get("foto_path").isJsonNull)
    }

    @Test
    fun `peran tanda tangan persis seperti yang diterima RPC`() {
        assertEquals("Crew Penerima", SuratJalanRepository.PERAN_CREW)
        assertEquals("Supir", SuratJalanRepository.PERAN_SUPIR)
    }
}
