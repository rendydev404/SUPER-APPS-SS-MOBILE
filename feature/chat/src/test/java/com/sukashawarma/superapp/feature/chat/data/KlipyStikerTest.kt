package com.sukashawarma.superapp.feature.chat.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KlipyStikerTest {

    /** Bentuk respons mengikuti DTO demo app Android resmi KLIPY
     *  (result → data → data[] + has_next; file → xs/sm/md/hd → webp/gif). */
    private val contoh = """
        {"result": true, "data": {"has_next": true, "data": [
          {"type": "sticker", "slug": "kucing-senang", "title": "Kucing senang",
           "file": {
             "xs": {"webp": {"url": "https://static.klipy.com/xs/kucing.webp", "width": 90, "height": 90},
                    "gif":  {"url": "https://static.klipy.com/xs/kucing.gif",  "width": 90, "height": 90}},
             "sm": {"webp": {"url": "https://static.klipy.com/sm/kucing.webp", "width": 200, "height": 100}}
           }},
          {"type": "ad", "width": 300, "height": 250, "content": "<div>iklan</div>"},
          {"type": "sticker", "slug": "hanya-gif", "title": "",
           "file": {"sm": {"gif": {"url": "https://media.klipy.com/sm/a.gif", "width": 100, "height": 100}}}},
          {"type": "sticker", "slug": "domain-asing",
           "file": {"sm": {"webp": {"url": "https://evil.example.com/klipy.com/x.webp", "width": 1, "height": 1}}}},
          {"type": "sticker", "file": {"sm": {"webp": {"url": "https://static.klipy.com/tanpa-slug.webp"}}}}
        ]}}
    """.trimIndent()

    @Test
    fun `iklan, domain asing, dan item tanpa slug dibuang`() {
        val h = KlipyStiker.parseHalaman(contoh)
        assertEquals(listOf("kucing-senang", "hanya-gif"), h.isi.map { it.slug })
        assertTrue(h.adaLagi)
    }

    @Test
    fun `pratinjau memakai ukuran terkecil dan kirim memakai sm, webp didahulukan`() {
        val s = KlipyStiker.parseHalaman(contoh).isi.first()
        assertEquals("https://static.klipy.com/xs/kucing.webp", s.urlPratinjau)
        assertEquals("https://static.klipy.com/sm/kucing.webp", s.urlKirim)
        assertEquals(2f, s.rasio, 0.001f)
    }

    @Test
    fun `gif dipakai bila webp tidak ada`() {
        val s = KlipyStiker.parseHalaman(contoh).isi[1]
        assertEquals("https://media.klipy.com/sm/a.gif", s.urlKirim)
        assertEquals(s.urlKirim, s.urlPratinjau)
    }

    @Test
    fun `respons tanpa data tidak crash`() {
        val h = KlipyStiker.parseHalaman("""{"result": false}""")
        assertTrue(h.isi.isEmpty())
        assertFalse(h.adaLagi)
    }

    @Test
    fun `urlSah sama dengan CHECK database`() {
        assertTrue(KlipyStiker.urlSah("https://klipy.com/a.webp"))
        assertTrue(KlipyStiker.urlSah("https://static.klipy.com/a/b.webp"))
        assertFalse(KlipyStiker.urlSah("http://static.klipy.com/a.webp"))
        assertFalse(KlipyStiker.urlSah("https://klipy.com.evil.io/a.webp"))
        assertFalse(KlipyStiker.urlSah("https://evilklipy.com/a.webp"))
        assertFalse(KlipyStiker.urlSah("https://static.klipy.com/" + "a".repeat(500)))
        assertFalse(KlipyStiker.urlSah(null))
    }

    @Test
    fun `id pelanggan stabil dan tidak memuat id asli`() {
        val id = "3f1c2b7e-0000-4000-8000-000000000001"
        val a = KlipyStiker.idPelanggan(id)
        assertEquals(a, KlipyStiker.idPelanggan(id))
        assertEquals(32, a.length)
        assertFalse(a.contains("3f1c2b7e"))
    }
}
