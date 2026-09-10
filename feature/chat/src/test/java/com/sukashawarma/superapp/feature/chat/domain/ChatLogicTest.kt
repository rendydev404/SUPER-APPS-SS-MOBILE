package com.sukashawarma.superapp.feature.chat.domain

import com.google.gson.JsonParser
import com.sukashawarma.superapp.feature.chat.data.PesanChat
import com.sukashawarma.superapp.feature.chat.data.parsePesanChat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class ChatLogicTest {

    private val zona = ZoneId.of("Asia/Jakarta")

    private fun pesan(
        id: String,
        sender: String,
        atMs: Long,
        body: String = "halo",
        image: String? = null,
    ) = PesanChat(
        id = id, senderId = sender, senderName = "Nama $sender", senderAvatar = null,
        body = body, imagePath = image,
        replyToId = null, replyToName = null, replyToSnippet = null,
        createdAtMs = atMs,
    )

    // -- susunItemChat: filter 24 jam ---------------------------------------

    @Test
    fun `pesan lebih tua dari 24 jam dibuang, yang lebih muda dipertahankan`() {
        val now = 1_000_000_000_000L
        val items = susunItemChat(
            listOf(
                pesan("tua", "a", now - UMUR_PESAN_MS - 1),
                pesan("muda", "a", now - UMUR_PESAN_MS + 60_000),
            ),
            userId = "x", nowMs = now, zona = zona,
        )
        val bubbles = items.filterIsInstance<ItemChat.Bubble>()
        assertEquals(listOf("muda"), bubbles.map { it.pesan.id })
    }

    // -- susunItemChat: pengelompokan ---------------------------------------

    @Test
    fun `pesan beruntun pengirim sama dalam 5 menit jadi satu grup dengan identitas hanya di awal`() {
        val now = 1_000_000_000_000L
        val t = now - 60 * 60_000
        val items = susunItemChat(
            listOf(
                pesan("1", "a", t),
                pesan("2", "a", t + 60_000),
                pesan("3", "a", t + 120_000),
            ),
            userId = "x", nowMs = now, zona = zona,
        )
        val bubbles = items.filterIsInstance<ItemChat.Bubble>()
        assertEquals(listOf(PosisiGrup.AWAL, PosisiGrup.TENGAH, PosisiGrup.AKHIR), bubbles.map { it.posisi })
        assertEquals(listOf(true, false, false), bubbles.map { it.tampilkanIdentitas })
    }

    @Test
    fun `jeda lebih dari 5 menit memutus grup`() {
        val now = 1_000_000_000_000L
        val t = now - 60 * 60_000
        val items = susunItemChat(
            listOf(
                pesan("1", "a", t),
                pesan("2", "a", t + JARAK_GRUP_MS + 1),
            ),
            userId = "x", nowMs = now, zona = zona,
        )
        val bubbles = items.filterIsInstance<ItemChat.Bubble>()
        assertEquals(listOf(PosisiGrup.TUNGGAL, PosisiGrup.TUNGGAL), bubbles.map { it.posisi })
    }

    @Test
    fun `pengirim berbeda tidak pernah segrup dan pesan sendiri tanpa identitas`() {
        val now = 1_000_000_000_000L
        val t = now - 60 * 60_000
        val items = susunItemChat(
            listOf(pesan("1", "a", t), pesan("2", "saya", t + 30_000)),
            userId = "saya", nowMs = now, zona = zona,
        )
        val bubbles = items.filterIsInstance<ItemChat.Bubble>()
        assertFalse(bubbles[0].milikSendiri)
        assertTrue(bubbles[1].milikSendiri)
        assertFalse(bubbles[1].tampilkanIdentitas)
    }

    @Test
    fun `urutan masukan acak tetap tersusun menaik menurut waktu`() {
        val now = 1_000_000_000_000L
        val t = now - 60 * 60_000
        val items = susunItemChat(
            listOf(pesan("b", "a", t + 60_000), pesan("a", "a", t)),
            userId = "x", nowMs = now, zona = zona,
        )
        val bubbles = items.filterIsInstance<ItemChat.Bubble>()
        assertEquals(listOf("a", "b"), bubbles.map { it.pesan.id })
    }

    // -- pemisah tanggal -----------------------------------------------------

    @Test
    fun `pemisah tanggal disisipkan saat hari berganti`() {
        // 2026-09-10 00:30 WIB; dua jam sebelumnya jatuh di 9 September.
        val now = java.time.ZonedDateTime.of(2026, 9, 10, 0, 30, 0, 0, zona)
            .toInstant().toEpochMilli()
        val kemarinMalam = now - 2 * 60 * 60_000
        val items = susunItemChat(
            listOf(pesan("1", "a", kemarinMalam), pesan("2", "a", now - 60_000)),
            userId = "x", nowMs = now, zona = zona,
        )
        assertEquals(2, items.filterIsInstance<ItemChat.Pemisah>().size)
    }

    @Test
    fun `label tanggal hari ini dan kemarin berbahasa manusia`() {
        val hariIni = java.time.LocalDate.of(2026, 9, 10)
        assertEquals("Hari Ini", labelTanggal(hariIni, hariIni))
        assertEquals("Kemarin", labelTanggal(hariIni.minusDays(1), hariIni))
        assertEquals("8 September 2026", labelTanggal(hariIni.minusDays(2), hariIni))
    }

    // -- snippet & warna nama -----------------------------------------------

    @Test
    fun `snippet memakai body bila ada, foto bila tidak`() {
        assertEquals("halo", snippetPesan("halo", null))
        assertEquals("📷 Foto", snippetPesan("", "chat-media/u/f.webp"))
        assertEquals("", snippetPesan("", null))
    }

    @Test
    fun `indeks warna nama stabil dan dalam rentang`() {
        val a = indeksWarnaNama("user-abc", 8)
        assertEquals(a, indeksWarnaNama("user-abc", 8))
        assertTrue(a in 0 until 8)
    }

    // -- pelacak pengetik ----------------------------------------------------

    @Test
    fun `sinyal typing dibatasi sekali per 3 detik`() {
        val p = PelacakPengetik()
        assertTrue(p.bolehKirim(0))
        assertFalse(p.bolehKirim(2_999))
        assertTrue(p.bolehKirim(3_000))
    }

    @Test
    fun `pengetik hilang setelah 5 detik hening atau saat pesannya tiba`() {
        val p = PelacakPengetik()
        p.catat("a", "Budi", 0)
        p.catat("b", "Sari", 1_000)
        assertEquals(listOf("Budi", "Sari"), p.namaAktif(4_000))
        assertEquals(listOf("Sari"), p.namaAktif(5_500))
        p.selesai("b")
        assertEquals(emptyList<String>(), p.namaAktif(5_600))
    }

    @Test
    fun `label pengetik satu dua dan banyak orang`() {
        assertNull(labelPengetik(emptyList()))
        assertEquals("Budi sedang mengetik…", labelPengetik(listOf("Budi")))
        assertEquals("Budi dan Sari sedang mengetik…", labelPengetik(listOf("Budi", "Sari")))
        assertEquals("3 orang sedang mengetik…", labelPengetik(listOf("a", "b", "c")))
    }

    // -- parsing -------------------------------------------------------------

    @Test
    fun `parse baris lengkap termasuk snapshot reply`() {
        val o = JsonParser.parseString(
            """
            {"id":"m1","sender_id":"u1","sender_name":"Budi","sender_avatar":"avatars/u1/a.jpg",
             "body":"halo","image_path":null,
             "reply_to_id":"m0","reply_to_name":"Sari","reply_to_snippet":"hai",
             "created_at":"2026-09-10T03:04:05.123456+00:00"}
            """.trimIndent()
        ).asJsonObject
        val p = parsePesanChat(o)!!
        assertEquals("m1", p.id)
        assertEquals("Budi", p.senderName)
        assertEquals("Sari", p.replyToName)
        assertNull(p.imagePath)
        assertTrue(p.createdAtMs > 0)
    }

    @Test
    fun `baris tanpa id atau waktu rusak menghasilkan null, bukan crash`() {
        assertNull(parsePesanChat(JsonParser.parseString("""{"sender_id":"u"}""").asJsonObject))
        assertNull(
            parsePesanChat(
                JsonParser.parseString(
                    """{"id":"x","sender_id":"u","created_at":"bukan-tanggal"}"""
                ).asJsonObject
            )
        )
    }
}
