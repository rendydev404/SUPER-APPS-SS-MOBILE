package com.sukashawarma.superapp.feature.distribusi.data

import org.junit.Assert.assertEquals
import org.junit.Test

class FotoBuktiPathTest {

    /**
     * Kolom `foto_path` di web berisi path TANPA nama bucket, sementara
     * `StorageUtil.uploadJpeg` mengembalikan "bucket/path". Kalau nama bucket
     * ikut tersimpan, foto yang diunggah dari native tidak akan bisa dibuka
     * dari web.
     */
    @Test
    fun `path tidak memuat nama bucket`() {
        val path = FotoBuktiStore.pathUntuk("sj-1", "item-9")
        assertEquals("sj-1/item-9.jpg", path)
    }

    @Test
    fun `batas byte sama dengan batas bucket`() {
        assertEquals(204800, FotoBuktiStore.BATAS_BYTE)
    }

    @Test
    fun `nama bucket persis seperti di database`() {
        assertEquals("verif-foto-bahan", FotoBuktiStore.BUCKET)
    }
}
