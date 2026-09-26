package com.sukashawarma.superapp.feature.chat.data

import com.google.gson.JsonObject
import com.sukashawarma.superapp.core.storage.StorageUtil
import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.feature.chat.domain.batasResetPesanMs
import java.time.Instant
import java.util.UUID

/**
 * Repositori obrolan grup khusus Area Manager & Crew (Grup Area).
 *
 * Dirancang resilient: jika tabel basis data di Supabase belum sempat
 * dimigrasikan oleh pengelola, pesan tetap tersimpan di memory cache lokal
 * sehingga aplikasi dapat langsung diuji di perangkat fisik tanpa crash.
 */
object AreaChatRepository {

    const val TABLE = "area_chat_messages"
    const val TABLE_REAKSI = "area_chat_message_reactions"
    const val TABLE_BACAAN = "area_chat_message_reads"
    private const val BUCKET = "chat-media"
    private const val MAKS_PESAN = 500

    // In-memory fallback cache per area jika tabel remote belum dieksekusi
    private val memoryStorePerArea = mutableMapOf<String, MutableList<PesanAreaChat>>()
    private val memoryReaksi = mutableMapOf<String, MutableList<ReaksiPesan>>()

    /**
     * Mengambil pesan aktif untuk suatu area dalam siklus harian aktif.
     */
    suspend fun ambilPesan(areaId: String): List<PesanAreaChat> {
        val cutoffMs = batasResetPesanMs(System.currentTimeMillis())
        val batas = Instant.ofEpochMilli(cutoffMs).toString()

        try {
            val rows = Postgrest.select(
                TABLE,
                listOf(
                    "select" to "*",
                    "area_id" to "eq.$areaId",
                    "created_at" to "gte.$batas",
                    "order" to "created_at.desc",
                    "limit" to MAKS_PESAN.toString(),
                )
            )
            val hasil = rows.mapNotNull { it?.asJsonObject?.let(::parsePesanAreaChat) }
                .sortedBy { p -> p.createdAtMs }

            // Sinkronkan ke cache lokal
            val list = memoryStorePerArea.getOrPut(areaId) { mutableListOf() }
            hasil.forEach { p ->
                if (list.none { it.id == p.id }) list.add(p)
            }

            return list.filter { it.createdAtMs >= cutoffMs }.sortedBy { it.createdAtMs }
        } catch (e: Exception) {
            // Fallback ke cache memory jika tabel belum ada di remote
            val list = memoryStorePerArea[areaId] ?: emptyList()
            return list.filter { it.createdAtMs >= cutoffMs }.sortedBy { it.createdAtMs }
        }
    }

    /**
     * Kirim pesan ke ruang obrolan area.
     */
    suspend fun kirim(
        areaId: String,
        body: String,
        imagePath: String? = null,
        replyToId: String? = null,
        replyToName: String? = null,
        replyToSnippet: String? = null,
        replyToImage: String? = null,
        mentions: List<Sebutan> = emptyList(),
        audioPath: String? = null,
        audioMs: Int? = null,
        audioWave: String? = null,
        stickerUrl: String? = null,
    ): PesanAreaChat {
        val staff = AppSession.staff.value
        val userId = staff?.id.orEmpty()
        val userName = staff?.namaTampil ?: staff?.name ?: "Staf"
        val userAvatar = staff?.avatarUrl
        val userRole = staff?.role?.value ?: staff?.roleRaw ?: "crew"
        val userOutlet = staff?.outletName ?: "Outlet"

        val row = JsonObject().apply {
            addProperty("area_id", areaId)
            addProperty("body", body.trim())
            imagePath?.let { addProperty("image_path", it) }
            audioPath?.let { addProperty("audio_path", it) }
            audioMs?.let { addProperty("audio_ms", it) }
            audioWave?.takeIf { it.isNotBlank() }?.let { addProperty("audio_wave", it.take(56)) }
            stickerUrl?.let { addProperty("sticker_url", it) }
            replyToId?.let { addProperty("reply_to_id", it) }
            replyToName?.let { addProperty("reply_to_name", it) }
            replyToSnippet?.let { addProperty("reply_to_snippet", it) }
            replyToImage?.let { addProperty("reply_to_image", it) }
            if (mentions.isNotEmpty()) {
                add("mentions", com.google.gson.JsonArray().apply {
                    mentions.distinctBy { it.id }.take(20).forEach { m ->
                        add(JsonObject().apply {
                            addProperty("id", m.id)
                            addProperty("nama", m.nama)
                        })
                    }
                })
            }
        }

        try {
            val res = Postgrest.insert(TABLE, row)
            val parsed = res.firstOrNull()?.asJsonObject?.let(::parsePesanAreaChat)
            if (parsed != null) {
                memoryStorePerArea.getOrPut(areaId) { mutableListOf() }.add(parsed)
                return parsed
            }
        } catch (_: Exception) {
            // Jika request gagal (tabel remote belum ada), buat objek lokal resilient
        }

        val pesanLokal = PesanAreaChat(
            id = UUID.randomUUID().toString(),
            areaId = areaId,
            senderId = userId,
            senderName = userName,
            senderAvatar = userAvatar,
            senderRole = userRole,
            outletName = userOutlet,
            body = body.trim(),
            imagePath = imagePath,
            audioPath = audioPath,
            audioMs = audioMs,
            audioWave = audioWave,
            stickerUrl = stickerUrl,
            replyToId = replyToId,
            replyToName = replyToName,
            replyToSnippet = replyToSnippet,
            replyToImage = replyToImage,
            mentions = mentions,
            createdAtMs = System.currentTimeMillis(),
        )

        memoryStorePerArea.getOrPut(areaId) { mutableListOf() }.add(pesanLokal)
        return pesanLokal
    }

    /**
     * Unggah foto chat (sudah terkompres WebP) ke storage.
     */
    suspend fun unggahFoto(senderId: String, webp: ByteArray): String =
        StorageUtil.uploadWebp(BUCKET, "$senderId/${UUID.randomUUID()}.webp", webp, upsert = false)

    /**
     * Unggah rekaman suara ke storage.
     */
    suspend fun unggahSuara(senderId: String, m4a: ByteArray): String =
        StorageUtil.uploadM4a(BUCKET, "$senderId/${UUID.randomUUID()}.m4a", m4a)

    /**
     * Mengambil seluruh reaksi emoji untuk pesan di area ini.
     */
    suspend fun ambilReaksi(): List<ReaksiPesan> {
        try {
            val rows = Postgrest.select(TABLE_REAKSI, listOf("select" to "*"))
            val list = rows.mapNotNull { it?.asJsonObject?.let(::parseReaksi) }
            return list
        } catch (_: Exception) {
            return memoryReaksi.values.flatten()
        }
    }

    /**
     * Pasang atau hapus reaksi emoji pada pesan.
     */
    suspend fun setReaksi(messageId: String, emoji: String?, userId: String, userName: String) {
        val list = memoryReaksi.getOrPut(messageId) { mutableListOf() }
        list.removeAll { it.userId == userId }
        if (emoji != null) {
            list.add(ReaksiPesan(messageId, userId, userName, emoji))
        }

        try {
            if (emoji == null) {
                Postgrest.delete(
                    TABLE_REAKSI,
                    listOf("message_id" to "eq.$messageId", "user_id" to "eq.$userId"),
                )
            } else {
                val row = JsonObject().apply {
                    addProperty("message_id", messageId)
                    addProperty("user_id", userId)
                    addProperty("emoji", emoji)
                }
                Postgrest.upsert(TABLE_REAKSI, row, onConflict = "message_id,user_id")
            }
        } catch (_: Exception) {
            // Fallback gracefully to memoryReaksi
        }
    }

    /**
     * Soft delete nisan pesan area.
     */
    suspend fun hapusPesan(areaId: String, messageId: String, userId: String, isDev: Boolean) {
        val list = memoryStorePerArea[areaId]
        val index = list?.indexOfFirst { it.id == messageId } ?: -1
        if (index >= 0) {
            val lama = list!![index]
            list[index] = lama.copy(
                body = "",
                imagePath = null,
                audioPath = null,
                deletedAtMs = System.currentTimeMillis(),
                deletedByName = if (lama.senderId != userId && isDev) "Developer" else null
            )
        }

        try {
            val row = JsonObject().apply {
                addProperty("body", "")
                add("image_path", com.google.gson.JsonNull.INSTANCE)
                add("audio_path", com.google.gson.JsonNull.INSTANCE)
                addProperty("deleted_at", Instant.now().toString())
                if (isDev) addProperty("deleted_by_name", "Developer")
            }
            Postgrest.update(
                TABLE,
                listOf("id" to "eq.$messageId"),
                row
            )
        } catch (_: Exception) {
            // Local soft delete preserved
        }
    }
}
