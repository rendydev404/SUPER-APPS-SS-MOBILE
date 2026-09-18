package com.sukashawarma.superapp.feature.chat.data

import androidx.compose.runtime.Immutable
import com.google.gson.JsonObject
import java.time.OffsetDateTime

/**
 * Status centang pada obrolan pribadi (1-on-1) persis ala WhatsApp.
 */
enum class StatusCentangPribadi {
    /** Belum sampai ke server / proses kirim (ikon jam kecil). */
    MENGIRIM,
    /** Sudah mendarat di database Supabase tapi belum sampai di HP penerima (centang satu abu-abu). */
    TERKIRIM,
    /** Sudah sampai di perangkat penerima (centang dua abu-abu). */
    TERSAMPAIKAN,
    /** Sudah dibuka dan dibaca oleh lawan bicara (centang dua biru). */
    DIBACA
}

/**
 * Entitas pesan dalam obrolan pribadi (1-on-1).
 */
@Immutable
data class PesanPribadi(
    val id: String,
    val senderId: String,
    val recipientId: String,
    val senderName: String,
    val senderAvatar: String?,
    val body: String,
    val imagePath: String?,
    /** Path rekaman suara di bucket `chat-media`. null = bukan pesan suara. */
    val audioPath: String? = null,
    /** Durasi rekaman (ms) menurut perekam. */
    val audioMs: Int? = null,
    /** Amplitudo rekaman, satu digit '0'-'9' per bilah waveform. */
    val audioWave: String? = null,
    /**
     * Kapan rekaman ini didengarkan PENERIMA. null = belum pernah.
     *
     * Satu kolom melayani dua sisi: di bubble sendiri ia berarti "lawan bicara
     * sudah mendengar", di bubble lawan ia berarti "saya sudah mendengar".
     */
    val audioPlayedAtMs: Long? = null,
    val replyToId: String?,
    val replyToName: String?,
    val replyToSnippet: String?,
    val createdAtMs: Long,
    val deliveredAtMs: Long? = null,
    val readAtMs: Long? = null,
    val editedAtMs: Long? = null,
    val deletedAtMs: Long? = null,
) {
    /** Status centang untuk pesan milik sendiri. */
    val statusCentang: StatusCentangPribadi
        get() = when {
            readAtMs != null -> StatusCentangPribadi.DIBACA
            deliveredAtMs != null -> StatusCentangPribadi.TERSAMPAIKAN
            else -> StatusCentangPribadi.TERKIRIM
        }
}

/**
 * Butir baris dalam daftar obrolan pribadi di layar utama Chat.
 */
@Immutable
data class PercakapanPribadiItem(
    val partnerId: String,
    val partnerName: String,
    val partnerDisplayName: String?,
    val partnerRole: String?,
    val partnerAvatar: String?,
    val partnerOutlet: String?,
    val lastMessageId: String?,
    val lastMessageBody: String,
    val lastMessageHasImage: Boolean,
    val lastMessageHasAudio: Boolean = false,
    val lastMessageAudioMs: Int? = null,
    val lastMessageAtMs: Long,
    val lastSenderId: String,
    val isSelfLastSender: Boolean,
    val deliveredAtMs: Long?,
    val readAtMs: Long?,
    val unreadCount: Int,
) {
    val namaTampil: String
        get() = partnerDisplayName?.takeIf { it.isNotBlank() } ?: partnerName

    val statusCentang: StatusCentangPribadi
        get() = when {
            readAtMs != null -> StatusCentangPribadi.DIBACA
            deliveredAtMs != null -> StatusCentangPribadi.TERSAMPAIKAN
            else -> StatusCentangPribadi.TERKIRIM
        }
}

/**
 * Butir percakapan untuk mode pantau siluman khusus role developer.
 */
@Immutable
data class PercakapanPengawasanItem(
    val userAId: String,
    val userAName: String,
    val userARole: String?,
    val userAOutlet: String?,
    val userAAvatar: String?,
    val userBId: String,
    val userBName: String,
    val userBRole: String?,
    val userBOutlet: String?,
    val userBAvatar: String?,
    val lastMessageId: String?,
    val lastMessageBody: String,
    val lastHasImage: Boolean,
    val lastHasAudio: Boolean = false,
    val lastAudioMs: Int? = null,
    val lastSenderId: String,
    val lastSenderName: String,
    val lastMessageAtMs: Long,
    val totalMessages: Int,
    val deliveredAtMs: Long?,
    val readAtMs: Long?,
)

// Helper parser JSON dari PostgREST / RPC

private fun parseIsoMs(iso: String?): Long? {
    if (iso.isNullOrBlank()) return null
    return runCatching { OffsetDateTime.parse(iso).toInstant().toEpochMilli() }.getOrNull()
}

fun parsePesanPribadi(json: JsonObject): PesanPribadi? {
    val id = json.get("id")?.takeIf { !it.isJsonNull }?.asString ?: return null
    val senderId = json.get("sender_id")?.takeIf { !it.isJsonNull }?.asString ?: return null
    val recipientId = json.get("recipient_id")?.takeIf { !it.isJsonNull }?.asString ?: return null
    val createdIso = json.get("created_at")?.takeIf { !it.isJsonNull }?.asString ?: return null
    val createdMs = parseIsoMs(createdIso) ?: return null

    return PesanPribadi(
        id = id,
        senderId = senderId,
        recipientId = recipientId,
        senderName = json.get("sender_name")?.takeIf { !it.isJsonNull }?.asString ?: "",
        senderAvatar = json.get("sender_avatar")?.takeIf { !it.isJsonNull }?.asString,
        body = json.get("body")?.takeIf { !it.isJsonNull }?.asString ?: "",
        imagePath = json.get("image_path")?.takeIf { !it.isJsonNull }?.asString,
        audioPath = json.get("audio_path")?.takeIf { !it.isJsonNull }?.asString,
        audioMs = json.get("audio_ms")?.takeIf { !it.isJsonNull }?.asInt,
        audioWave = json.get("audio_wave")?.takeIf { !it.isJsonNull }?.asString,
        audioPlayedAtMs = parseIsoMs(json.get("audio_played_at")?.takeIf { !it.isJsonNull }?.asString),
        replyToId = json.get("reply_to_id")?.takeIf { !it.isJsonNull }?.asString,
        replyToName = json.get("reply_to_name")?.takeIf { !it.isJsonNull }?.asString,
        replyToSnippet = json.get("reply_to_snippet")?.takeIf { !it.isJsonNull }?.asString,
        createdAtMs = createdMs,
        deliveredAtMs = parseIsoMs(json.get("delivered_at")?.takeIf { !it.isJsonNull }?.asString),
        readAtMs = parseIsoMs(json.get("read_at")?.takeIf { !it.isJsonNull }?.asString),
        editedAtMs = parseIsoMs(json.get("edited_at")?.takeIf { !it.isJsonNull }?.asString),
        deletedAtMs = parseIsoMs(json.get("deleted_at")?.takeIf { !it.isJsonNull }?.asString),
    )
}

fun parsePercakapanPribadiItem(json: JsonObject): PercakapanPribadiItem? {
    val partnerId = json.get("partner_id")?.takeIf { !it.isJsonNull }?.asString ?: return null
    val partnerName = json.get("partner_name")?.takeIf { !it.isJsonNull }?.asString ?: return null
    val lastAtIso = json.get("last_message_at")?.takeIf { !it.isJsonNull }?.asString
    val lastAtMs = parseIsoMs(lastAtIso) ?: System.currentTimeMillis()

    return PercakapanPribadiItem(
        partnerId = partnerId,
        partnerName = partnerName,
        partnerDisplayName = json.get("partner_display_name")?.takeIf { !it.isJsonNull }?.asString,
        partnerRole = json.get("partner_role")?.takeIf { !it.isJsonNull }?.asString,
        partnerAvatar = json.get("partner_avatar")?.takeIf { !it.isJsonNull }?.asString,
        partnerOutlet = json.get("partner_outlet")?.takeIf { !it.isJsonNull }?.asString,
        lastMessageId = json.get("last_message_id")?.takeIf { !it.isJsonNull }?.asString,
        lastMessageBody = json.get("last_message_body")?.takeIf { !it.isJsonNull }?.asString ?: "",
        lastMessageHasImage = json.get("last_message_has_image")?.takeIf { !it.isJsonNull }?.asBoolean ?: false,
        lastMessageHasAudio = json.get("last_message_has_audio")?.takeIf { !it.isJsonNull }?.asBoolean ?: false,
        lastMessageAudioMs = json.get("last_message_audio_ms")?.takeIf { !it.isJsonNull }?.asInt,
        lastMessageAtMs = lastAtMs,
        lastSenderId = json.get("last_sender_id")?.takeIf { !it.isJsonNull }?.asString ?: "",
        isSelfLastSender = json.get("is_self_last_sender")?.takeIf { !it.isJsonNull }?.asBoolean ?: false,
        deliveredAtMs = parseIsoMs(json.get("delivered_at")?.takeIf { !it.isJsonNull }?.asString),
        readAtMs = parseIsoMs(json.get("read_at")?.takeIf { !it.isJsonNull }?.asString),
        unreadCount = json.get("unread_count")?.takeIf { !it.isJsonNull }?.asInt ?: 0,
    )
}

fun parsePercakapanPengawasanItem(json: JsonObject): PercakapanPengawasanItem? {
    val userAId = json.get("user_a_id")?.takeIf { !it.isJsonNull }?.asString ?: return null
    val userBId = json.get("user_b_id")?.takeIf { !it.isJsonNull }?.asString ?: return null
    val lastAtIso = json.get("last_message_at")?.takeIf { !it.isJsonNull }?.asString
    val lastAtMs = parseIsoMs(lastAtIso) ?: System.currentTimeMillis()

    return PercakapanPengawasanItem(
        userAId = userAId,
        userAName = json.get("user_a_name")?.takeIf { !it.isJsonNull }?.asString ?: "",
        userARole = json.get("user_a_role")?.takeIf { !it.isJsonNull }?.asString,
        userAOutlet = json.get("user_a_outlet")?.takeIf { !it.isJsonNull }?.asString,
        userAAvatar = json.get("user_a_avatar")?.takeIf { !it.isJsonNull }?.asString,
        userBId = userBId,
        userBName = json.get("user_b_name")?.takeIf { !it.isJsonNull }?.asString ?: "",
        userBRole = json.get("user_b_role")?.takeIf { !it.isJsonNull }?.asString,
        userBOutlet = json.get("user_b_outlet")?.takeIf { !it.isJsonNull }?.asString,
        userBAvatar = json.get("user_b_avatar")?.takeIf { !it.isJsonNull }?.asString,
        lastMessageId = json.get("last_message_id")?.takeIf { !it.isJsonNull }?.asString,
        lastMessageBody = json.get("last_message_body")?.takeIf { !it.isJsonNull }?.asString ?: "",
        lastHasImage = json.get("last_has_image")?.takeIf { !it.isJsonNull }?.asBoolean ?: false,
        lastHasAudio = json.get("last_has_audio")?.takeIf { !it.isJsonNull }?.asBoolean ?: false,
        lastAudioMs = json.get("last_audio_ms")?.takeIf { !it.isJsonNull }?.asInt,
        lastSenderId = json.get("last_sender_id")?.takeIf { !it.isJsonNull }?.asString ?: "",
        lastSenderName = json.get("last_sender_name")?.takeIf { !it.isJsonNull }?.asString ?: "",
        lastMessageAtMs = lastAtMs,
        totalMessages = json.get("total_messages")?.takeIf { !it.isJsonNull }?.asInt ?: 0,
        deliveredAtMs = parseIsoMs(json.get("delivered_at")?.takeIf { !it.isJsonNull }?.asString),
        readAtMs = parseIsoMs(json.get("read_at")?.takeIf { !it.isJsonNull }?.asString),
    )
}
