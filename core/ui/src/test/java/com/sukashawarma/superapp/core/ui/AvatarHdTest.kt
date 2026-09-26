package com.sukashawarma.superapp.core.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AvatarHdTest {

    @Test
    fun `pasangan HD berada di folder yang sama dengan akhiran _hd`() {
        assertEquals(
            "avatars/uid-1/abc.jpg".removeSuffix(".jpg") + "_hd.jpg",
            AvatarStorage.pathHd("avatars/uid-1/abc.jpg"),
        )
        // Path tanpa awalan bucket (bentuk yang dipakai saat mengunggah) juga didukung.
        assertEquals("uid-1/abc_hd.jpg", AvatarStorage.pathHd("uid-1/abc.jpg"))
    }

    @Test
    fun `path yang bukan unggahan app tidak punya pasangan HD`() {
        assertNull(AvatarStorage.pathHd(null))
        assertNull(AvatarStorage.pathHd(""))
        assertNull(AvatarStorage.pathHd("https://contoh.com/a.jpg"))
        assertNull(AvatarStorage.pathHd("avatars/uid-1/abc.png"))
        // Sudah HD: jangan jadi `_hd_hd.jpg`.
        assertNull(AvatarStorage.pathHd("avatars/uid-1/abc_hd.jpg"))
    }
}
