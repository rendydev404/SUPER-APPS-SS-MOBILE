package com.sukashawarma.superapp.feature.distribusi.ui.ttd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BatasTandaTanganTest {

    /** RPC `sign_receipt_surat_jalan` menyimpan gambar ke kolom jsonb. Web
     *  membatasinya di 50.000 karakter; native harus memakai batas yang sama
     *  supaya baris yang dihasilkan tidak berbeda bentuk. */
    @Test
    fun `batas sama dengan web`() {
        assertEquals(50_000, BATAS_TANDA_TANGAN)
    }

    @Test
    fun `gambar wajar lolos`() {
        assertFalse(tandaTanganTerlaluBesar("data:image/png;base64," + "A".repeat(1000)))
    }

    @Test
    fun `gambar tepat di batas lolos`() {
        assertFalse(tandaTanganTerlaluBesar("A".repeat(BATAS_TANDA_TANGAN)))
    }

    @Test
    fun `gambar melewati batas ditolak`() {
        assertTrue(tandaTanganTerlaluBesar("A".repeat(BATAS_TANDA_TANGAN + 1)))
    }
}
