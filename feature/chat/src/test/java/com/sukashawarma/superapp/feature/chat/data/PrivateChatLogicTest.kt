package com.sukashawarma.superapp.feature.chat.data

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PrivateChatLogicTest {

    @Test
    fun `status centang TERKIRIM jika belum delivered dan belum dibaca`() {
        val pesan = PesanPribadi(
            id = "msg-1",
            senderId = "user-a",
            recipientId = "user-b",
            senderName = "Budi",
            senderAvatar = null,
            body = "Halo Siti",
            imagePath = null,
            replyToId = null,
            replyToName = null,
            replyToSnippet = null,
            createdAtMs = 1700000000000L,
            deliveredAtMs = null,
            readAtMs = null,
        )
        assertEquals(StatusCentangPribadi.TERKIRIM, pesan.statusCentang)
    }

    @Test
    fun `status centang TERSAMPAIKAN dua abu jika sudah delivered tapi belum dibaca`() {
        val pesan = PesanPribadi(
            id = "msg-2",
            senderId = "user-a",
            recipientId = "user-b",
            senderName = "Budi",
            senderAvatar = null,
            body = "Halo Siti",
            imagePath = null,
            replyToId = null,
            replyToName = null,
            replyToSnippet = null,
            createdAtMs = 1700000000000L,
            deliveredAtMs = 1700000005000L,
            readAtMs = null,
        )
        assertEquals(StatusCentangPribadi.TERSAMPAIKAN, pesan.statusCentang)
    }

    @Test
    fun `status centang DIBACA dua biru jika readAt tidak null`() {
        val pesan = PesanPribadi(
            id = "msg-3",
            senderId = "user-a",
            recipientId = "user-b",
            senderName = "Budi",
            senderAvatar = null,
            body = "Halo Siti",
            imagePath = null,
            replyToId = null,
            replyToName = null,
            replyToSnippet = null,
            createdAtMs = 1700000000000L,
            deliveredAtMs = 1700000005000L,
            readAtMs = 1700000010000L,
        )
        assertEquals(StatusCentangPribadi.DIBACA, pesan.statusCentang)
    }

    @Test
    fun `parsePesanPribadi membaca JSON PostgREST dengan lengkap`() {
        val json = """
            {
                "id": "11111111-1111-1111-1111-111111111111",
                "sender_id": "22222222-2222-2222-2222-222222222222",
                "sender_name": "Ahmad Dani",
                "sender_avatar": "avatars/dani.jpg",
                "recipient_id": "33333333-3333-3333-3333-333333333333",
                "body": "Stok shawarma habis?",
                "image_path": "chat-media/stok.jpg",
                "reply_to_id": null,
                "reply_to_name": null,
                "reply_to_snippet": null,
                "created_at": "2026-09-16T10:00:00+07:00",
                "delivered_at": "2026-09-16T10:00:02+07:00",
                "read_at": "2026-09-16T10:00:05+07:00"
            }
        """.trimIndent()

        val parsed = parsePesanPribadi(JsonParser.parseString(json).asJsonObject)
        assertNotNull(parsed)
        assertEquals("11111111-1111-1111-1111-111111111111", parsed!!.id)
        assertEquals("Ahmad Dani", parsed.senderName)
        assertEquals("33333333-3333-3333-3333-333333333333", parsed.recipientId)
        assertEquals("Stok shawarma habis?", parsed.body)
        assertEquals("chat-media/stok.jpg", parsed.imagePath)
        assertNotNull(parsed.deliveredAtMs)
        assertNotNull(parsed.readAtMs)
        assertEquals(StatusCentangPribadi.DIBACA, parsed.statusCentang)
    }

    @Test
    fun `parsePercakapanPribadiItem membaca daftar thread dengan unread dan centang`() {
        val json = """
            {
                "partner_id": "33333333-3333-3333-3333-333333333333",
                "partner_name": "Siti Aminah",
                "partner_display_name": "sitishawarma",
                "partner_avatar": "avatars/siti.jpg",
                "partner_role": "crew",
                "partner_outlet": "Cabang Gejayan",
                "last_message_id": "msg-123",
                "last_message_body": "Siap kak, sedang disiapkan",
                "last_message_has_image": false,
                "last_message_at": "2026-09-16T11:15:00+07:00",
                "last_sender_id": "33333333-3333-3333-3333-333333333333",
                "is_self_last_sender": false,
                "unread_count": 2,
                "delivered_at": "2026-09-16T11:15:01+07:00",
                "read_at": null
            }
        """.trimIndent()

        val parsed = parsePercakapanPribadiItem(JsonParser.parseString(json).asJsonObject)
        assertNotNull(parsed)
        assertEquals("sitishawarma", parsed!!.namaTampil)
        assertEquals(2, parsed.unreadCount)
        assertEquals(false, parsed.isSelfLastSender)
        assertEquals("Siap kak, sedang disiapkan", parsed.lastMessageBody)
    }

    @Test
    fun `parsePercakapanPengawasanItem membaca thread staf untuk Developer Monitor`() {
        val json = """
            {
                "user_a_id": "user-1",
                "user_a_name": "Budi Santoso",
                "user_a_avatar": "avatars/budi.jpg",
                "user_a_role": "spv",
                "user_a_outlet": "Kaliurang",
                "user_b_id": "user-2",
                "user_b_name": "Riko Hendra",
                "user_b_avatar": null,
                "user_b_role": "crew",
                "user_b_outlet": "Kaliurang",
                "total_messages": 14,
                "last_message_id": "msg-999",
                "last_message_body": "Jadwal shift besok sudah diupdate?",
                "last_has_image": false,
                "last_sender_id": "user-1",
                "last_sender_name": "Budi Santoso",
                "last_message_at": "2026-09-16T12:00:00+07:00",
                "delivered_at": null,
                "read_at": null
            }
        """.trimIndent()

        val parsed = parsePercakapanPengawasanItem(JsonParser.parseString(json).asJsonObject)
        assertNotNull(parsed)
        assertEquals("Budi Santoso", parsed!!.userAName)
        assertEquals("Riko Hendra", parsed.userBName)
        assertEquals(14, parsed.totalMessages)
        assertEquals("Budi Santoso", parsed.lastSenderName)
        assertEquals("Jadwal shift besok sudah diupdate?", parsed.lastMessageBody)
    }
}