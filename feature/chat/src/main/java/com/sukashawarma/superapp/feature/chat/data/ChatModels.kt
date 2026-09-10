package com.sukashawarma.superapp.feature.chat.data

import com.google.gson.JsonObject
import java.time.OffsetDateTime

/**
 * Satu pesan chat, persis satu baris `chat_messages`.
 *
 * `senderName`/`senderAvatar` adalah SNAPSHOT saat pesan dikirim (diisi trigger
 * database, bukan join saat baca) — lihat migrasi 20300209000000. Kutipan reply
 * juga snapshot, supaya tetap tampil walau pesan asalnya sudah disapu job 24 jam.
 */
data class PesanChat(
    val id: String,
    val senderId: String,
    val senderName: String,
    val senderAvatar: String?,
    val body: String,
    val imagePath: String?,
    val replyToId: String?,
    val replyToName: String?,
    val replyToSnippet: String?,
    val createdAtMs: Long,
)

/** Parsing satu baris JSON PostgREST -> [PesanChat]. null bila baris tidak utuh. */
fun parsePesanChat(o: JsonObject): PesanChat? {
    fun teks(k: String): String? =
        o.get(k)?.takeIf { !it.isJsonNull }?.asString

    val id = teks("id") ?: return null
    val senderId = teks("sender_id") ?: return null
    val createdAt = teks("created_at") ?: return null
    val createdAtMs = try {
        OffsetDateTime.parse(createdAt).toInstant().toEpochMilli()
    } catch (_: Exception) {
        return null
    }
    return PesanChat(
        id = id,
        senderId = senderId,
        senderName = teks("sender_name").orEmpty().ifBlank { "Tanpa Nama" },
        senderAvatar = teks("sender_avatar"),
        body = teks("body").orEmpty(),
        imagePath = teks("image_path"),
        replyToId = teks("reply_to_id"),
        replyToName = teks("reply_to_name"),
        replyToSnippet = teks("reply_to_snippet"),
        createdAtMs = createdAtMs,
    )
}
