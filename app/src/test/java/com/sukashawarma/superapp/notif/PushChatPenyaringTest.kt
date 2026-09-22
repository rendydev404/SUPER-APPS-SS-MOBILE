package com.sukashawarma.superapp.notif

import com.sukashawarma.superapp.notif.PushChatPenyaring.Jenis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PushChatPenyaringTest {

    private val saya = "staf-aku"

    private fun pribadi(vararg ekstra: Pair<String, String>) = mapOf(
        "type" to "private_chat",
        "sender_id" to "staf-lain",
        "sender" to "Budi",
        "body" to "halo",
        "url" to "/chat/private?from=staf-lain",
    ) + ekstra

    @Test
    fun `pesan pribadi untuk saya ditampilkan`() {
        val h = PushChatPenyaring.putuskan(pribadi("recipient_id" to saya), saya, null, chatTerbuka = true)
        assertEquals(Jenis.PRIBADI, h.jenis)
        assertNull(h.tolak)
        assertTrue(h.akuiTersampaikan)
    }

    @Test
    fun `pesan pribadi untuk orang lain ditolak dan tidak diakui tersampaikan`() {
        val h = PushChatPenyaring.putuskan(pribadi("recipient_id" to "staf-ketiga"), saya, null, false)
        assertEquals(Jenis.PRIBADI, h.jenis)
        assertNotNull(h.tolak)
        assertFalse(h.akuiTersampaikan)
    }

    @Test
    fun `pesan pribadi tanpa recipient_id tetap ditampilkan demi kompatibilitas`() {
        val h = PushChatPenyaring.putuskan(pribadi(), saya, null, false)
        assertNull(h.tolak)
    }

    @Test
    fun `pesan pribadi dibungkam hanya saat percakapan pengirim itu terbuka`() {
        val terbuka = PushChatPenyaring.putuskan(pribadi(), saya, partnerTerbuka = "staf-lain", chatTerbuka = true)
        assertNotNull(terbuka.tolak)
        assertTrue("centang tersampaikan tetap dikirim", terbuka.akuiTersampaikan)

        val orangLain = PushChatPenyaring.putuskan(pribadi(), saya, partnerTerbuka = "staf-ketiga", chatTerbuka = true)
        assertNull("sedang chat dengan orang lain: tetap berbunyi", orangLain.tolak)
    }

    @Test
    fun `siaran legacy send-push berisi pesan pribadi tidak masuk jalur grup`() {
        val bocor = mapOf(
            "type" to "broadcast",
            "title" to "Budi",
            "body" to "rahasia",
            "url" to "/chat?from=staf-lain",
        )
        val h = PushChatPenyaring.putuskan(bocor, saya, null, false)
        assertEquals(Jenis.GRUP, h.jenis)
        assertNotNull(h.tolak)
    }

    @Test
    fun `siaran legacy dengan url pribadi juga ditolak`() {
        val h = PushChatPenyaring.putuskan(
            mapOf("type" to "broadcast", "url" to "/chat/private?from=staf-lain", "body" to "x"),
            saya, null, false,
        )
        assertEquals(Jenis.PRIBADI, h.jenis)
        assertNotNull(h.tolak)
        assertFalse(h.akuiTersampaikan)
    }

    @Test
    fun `pesan grup dari send-chat-push tetap tampil`() {
        val h = PushChatPenyaring.putuskan(
            mapOf("type" to "chat", "sender_id" to "staf-lain", "url" to "/chat", "body" to "x"),
            saya, null, false,
        )
        assertEquals(Jenis.GRUP, h.jenis)
        assertNull(h.tolak)
    }

    @Test
    fun `pesan grup dari diri sendiri atau saat layar chat terbuka ditolak`() {
        val sendiri = PushChatPenyaring.putuskan(
            mapOf("type" to "chat", "sender_id" to saya, "url" to "/chat", "body" to "x"), saya, null, false,
        )
        assertNotNull(sendiri.tolak)
        val terbuka = PushChatPenyaring.putuskan(
            mapOf("type" to "chat", "sender_id" to "staf-lain", "url" to "/chat", "body" to "x"), saya, null, true,
        )
        assertNotNull(terbuka.tolak)
    }

    @Test
    fun `payload non-chat diteruskan ke jalur umum`() {
        val h = PushChatPenyaring.putuskan(mapOf("type" to "info", "url" to "/manager"), saya, null, false)
        assertEquals(Jenis.BUKAN_CHAT, h.jenis)
    }

    @Test
    fun `pengirim diambil dari url bila sender_id kosong`() {
        val h = PushChatPenyaring.putuskan(mapOf("type" to "chat", "url" to "/chat?from=abc&x=1"), saya, null, false)
        assertEquals("abc", h.pengirimId)
    }
}
