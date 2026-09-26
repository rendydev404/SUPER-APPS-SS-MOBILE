package com.sukashawarma.superapp.feature.chat.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FavoritStikerTest {

    @Test
    fun `json bolak-balik tidak mengubah isi`() {
        val daftar = listOf(
            StikerKlipy("a", "Kucing", "https://static.klipy.com/xs/a.webp", "https://static.klipy.com/sm/a.webp", 1.5f),
            FavoritStiker.dariPesan("https://static.klipy.com/sm/b.webp"),
        )
        assertEquals(daftar, FavoritStiker.dariJson(FavoritStiker.keJson(daftar)))
    }

    @Test
    fun `data rusak atau kosong menjadi daftar kosong`() {
        assertTrue(FavoritStiker.dariJson(null).isEmpty())
        assertTrue(FavoritStiker.dariJson("").isEmpty())
        assertTrue(FavoritStiker.dariJson("""[{"judul":"tanpa url"}, 5]""").isEmpty())
    }

    @Test
    fun `favorit dari pesan memakai url yang sama untuk pratinjau dan kirim`() {
        val s = FavoritStiker.dariPesan("https://static.klipy.com/sm/c.webp")
        assertEquals(s.urlKirim, s.urlPratinjau)
        assertEquals(s.urlKirim, s.slug)
    }
}
