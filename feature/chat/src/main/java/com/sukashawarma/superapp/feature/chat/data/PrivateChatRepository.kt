package com.sukashawarma.superapp.feature.chat.data

import com.google.gson.JsonObject
import com.sukashawarma.superapp.core.storage.StorageUtil
import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.SupabaseClient
import java.util.UUID

/**
 * Repositori obrolan pribadi (1-on-1) dan pengawasan siluman developer.
 */
object PrivateChatRepository {

    private const val BUCKET = "chat-media"

    /**
     * Mengambil daftar percakapan aktif milik user yang sedang login
     * (pesan sejak 03:00 AM WIB siklus aktif).
     */
    suspend fun ambilDaftarPercakapan(): List<PercakapanPribadiItem> {
        val res = Postgrest.rpc("private_chat_daftar_percakapan")
        if (!res.isJsonArray) return emptyList()
        return res.asJsonArray.mapNotNull { it?.asJsonObject?.let(::parsePercakapanPribadiItem) }
    }

    /**
     * Mengambil seluruh riwayat pesan 1-on-1 dengan partner tertentu.
     */
    suspend fun ambilPesan(partnerId: String): List<PesanPribadi> {
        val payload = JsonObject().apply {
            addProperty("p_partner_id", partnerId)
        }
        val res = Postgrest.rpc("private_chat_ambil_pesan", payload)
        if (!res.isJsonArray) return emptyList()
        return res.asJsonArray.mapNotNull { it?.asJsonObject?.let(::parsePesanPribadi) }
    }

    /**
     * Mengirim pesan chat pribadi ke partner.
     */
    suspend fun kirimPesan(
        recipientId: String,
        body: String,
        imagePath: String? = null,
        replyToId: String? = null,
        audioPath: String? = null,
        audioMs: Int? = null,
        audioWave: String? = null,
    ): PesanPribadi {
        val payload = JsonObject().apply {
            addProperty("p_recipient_id", recipientId)
            addProperty("p_body", body.trim())
            imagePath?.let { addProperty("p_image_path", it) }
            replyToId?.let { addProperty("p_reply_to_id", it) }
            audioPath?.let { addProperty("p_audio_path", it) }
            audioMs?.let { addProperty("p_audio_ms", it) }
            audioWave?.takeIf { it.isNotBlank() }?.let { addProperty("p_audio_wave", it.take(56)) }
        }
        val res = Postgrest.rpc("private_chat_kirim", payload)
        if (!res.isJsonObject) {
            throw IllegalStateException("Server tidak mengembalikan format pesan yang valid.")
        }
        return parsePesanPribadi(res.asJsonObject)
            ?: throw IllegalStateException("Gagal memproses data pesan terkirim.")
    }

    /**
     * Menandai pesan dari partner sebagai telah tersampaikan ke HP penerima (centang 2 abu).
     */
    suspend fun tandaiTersampaikan(senderId: String): Int {
        val payload = JsonObject().apply {
            addProperty("p_sender_id", senderId)
        }
        val res = Postgrest.rpc("private_chat_tandai_tersampaikan", payload)
        return runCatching { res.asInt }.getOrDefault(0)
    }

    /**
     * Menandai seluruh pesan masuk yang belum tersampaikan sebagai telah sampai di HP ini.
     */
    suspend fun tandaiSemuaTersampaikan(): Int {
        val res = Postgrest.rpc("private_chat_tandai_semua_tersampaikan")
        return runCatching { res.asInt }.getOrDefault(0)
    }

    /**
     * Menandai pesan dari partner sebagai telah dibaca (centang 2 biru).
     */
    suspend fun tandaiDibaca(senderId: String): Int {
        val payload = JsonObject().apply {
            addProperty("p_sender_id", senderId)
        }
        val res = Postgrest.rpc("private_chat_tandai_dibaca", payload)
        return runCatching { res.asInt }.getOrDefault(0)
    }

    /**
     * Mengambil reaksi emoji untuk pesan dalam obrolan dengan partner ini.
     */
    suspend fun ambilReaksi(partnerId: String): List<ReaksiPesan> {
        val payload = JsonObject().apply {
            addProperty("p_partner_id", partnerId)
        }
        val res = Postgrest.rpc("private_chat_ambil_reaksi", payload)
        if (!res.isJsonArray) return emptyList()
        return res.asJsonArray.mapNotNull { it?.asJsonObject?.let(::parseReaksi) }
    }

    /**
     * Pasang, ganti, atau cabut reaksi emoji pada pesan obrolan pribadi.
     */
    suspend fun setReaksi(messageId: String, emoji: String?) {
        val payload = JsonObject().apply {
            addProperty("p_message_id", messageId)
            if (emoji != null) addProperty("p_emoji", emoji)
        }
        Postgrest.rpc("private_chat_set_reaksi", payload)
    }

    /**
     * Hapus pesan milik sendiri (soft-delete / nisan).
     */
    suspend fun hapusPesan(messageId: String) {
        val payload = JsonObject().apply {
            addProperty("p_id", messageId)
        }
        Postgrest.rpc("private_chat_hapus_pesan", payload)
    }

    /**
     * Mengunggah foto chat pribadi ke bucket chat-media.
     */
    suspend fun unggahFoto(senderId: String, webp: ByteArray): String =
        StorageUtil.uploadWebp(BUCKET, "$senderId/${UUID.randomUUID()}.webp", webp, upsert = false)

    fun urlFoto(path: String?): String? {
        if (path.isNullOrBlank()) return null
        val objek = path.removePrefix("$BUCKET/")
        return "${SupabaseClient.BASE_URL}storage/v1/object/authenticated/$BUCKET/$objek"
    }

    /** Tandai pesan suara sudah didengar; hanya berlaku bagi penerimanya. */
    suspend fun tandaiSuaraDiputar(messageId: String) {
        Postgrest.rpc(
            "private_chat_tandai_suara_diputar",
            JsonObject().apply { addProperty("p_id", messageId) },
        )
    }

    /** URL rekaman suara — bucket dan aturan auth yang sama dengan [urlFoto]. */
    fun urlSuara(path: String?): String? = urlFoto(path)

    /** Unggah rekaman suara chat pribadi ke bucket chat-media. */
    suspend fun unggahSuara(senderId: String, m4a: ByteArray): String =
        StorageUtil.uploadM4a(BUCKET, "$senderId/${UUID.randomUUID()}.m4a", m4a)

    // ====================================================================
    // MODE PENGAWASAN KHUSUS DEVELOPER (SILUMAN / GOD-MODE)
    // ====================================================================

    /**
     * Khusus Developer: Mengambil seluruh percakapan aktif antar seluruh user.
     */
    suspend fun developerAmbilSemuaPercakapan(): List<PercakapanPengawasanItem> {
        val res = Postgrest.rpc("developer_private_chat_semua_percakapan")
        if (!res.isJsonArray) return emptyList()
        return res.asJsonArray.mapNotNull { it?.asJsonObject?.let(::parsePercakapanPengawasanItem) }
    }

    /**
     * Khusus Developer: Membaca pesan antara User A dan User B secara SILUMAN
     * (tidak mengubah read_at ataupun delivered_at sehingga kedua pihak tidak tahu).
     */
    suspend fun developerAmbilPesanSiluman(userA: String, userB: String): List<PesanPribadi> {
        val payload = JsonObject().apply {
            addProperty("p_user_a", userA)
            addProperty("p_user_b", userB)
        }
        val res = Postgrest.rpc("developer_private_chat_ambil_pesan", payload)
        if (!res.isJsonArray) return emptyList()
        return res.asJsonArray.mapNotNull { it?.asJsonObject?.let(::parsePesanPribadi) }
    }
}
