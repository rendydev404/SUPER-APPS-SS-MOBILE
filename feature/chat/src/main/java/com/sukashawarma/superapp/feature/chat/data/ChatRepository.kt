package com.sukashawarma.superapp.feature.chat.data

import com.google.gson.JsonObject
import com.sukashawarma.superapp.core.storage.StorageUtil
import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.SupabaseClient
import java.time.Instant
import java.util.UUID

object ChatRepository {

    const val TABLE = "chat_messages"
    private const val BUCKET = "chat-media"

    /** Batas atas satu pengambilan. Grup seramai apa pun jarang menembus ini
     *  dalam 24 jam; kalau sampai, yang terpotong adalah pesan TERLAMA. */
    private const val MAKS_PESAN = 500

    /**
     * Seluruh pesan 24 jam terakhir, urut naik (terlama dulu).
     *
     * Filter 24 jam dikirim eksplisit walau RLS sudah menegakkannya — supaya
     * pemotongan `limit` menghitung dari jendela yang benar, dan supaya query
     * tetap benar seandainya policy-nya kelak berubah.
     */
    suspend fun ambilPesan(): List<PesanChat> {
        val batas = Instant.now().minusSeconds(24L * 60 * 60).toString()
        val rows = Postgrest.select(
            TABLE,
            listOf(
                "select" to "*",
                "created_at" to "gt.$batas",
                "order" to "created_at.desc",
                "limit" to MAKS_PESAN.toString(),
            ),
        )
        return rows.mapNotNull { it?.asJsonObject?.let(::parsePesanChat) }.sortedBy { p -> p.createdAtMs }
    }

    /**
     * Kirim pesan. Identitas pengirim + snapshot kutipan diisi TRIGGER database
     * dari sesi — klien sengaja tidak mengirim sender_*; apa pun yang dikirim
     * akan ditimpa server (lihat migrasi 20300209000000).
     */
    suspend fun kirim(body: String, imagePath: String? = null, replyToId: String? = null): PesanChat {
        val row = JsonObject().apply {
            addProperty("body", body)
            imagePath?.let { addProperty("image_path", it) }
            replyToId?.let { addProperty("reply_to_id", it) }
        }
        val res = Postgrest.insert(TABLE, row)
        return res.firstOrNull()?.asJsonObject?.let(::parsePesanChat)
            ?: throw IllegalStateException("Server tidak mengembalikan pesan yang tersimpan.")
    }

    /**
     * Unggah foto chat (sudah terkompres WebP) lalu kembalikan path untuk
     * kolom `image_path`. Folder pertama WAJIB id pengirim — policy
     * `chat_media_insert_self` dan trigger sama-sama menuntutnya.
     */
    suspend fun unggahFoto(senderId: String, webp: ByteArray): String =
        StorageUtil.uploadWebp(BUCKET, "$senderId/${UUID.randomUUID()}.webp", webp)

    /** Hapus pesan milik sendiri. RLS menolak diam-diam untuk pesan orang lain. */
    suspend fun hapus(id: String) {
        Postgrest.delete(TABLE, listOf("id" to "eq.$id"))
    }

    /** URL yang bisa dimuat Coil untuk `image_path` ("chat-media/<uid>/<file>.webp").
     *  Bucket privat: endpoint `authenticated` menuntut Authorization, yang sudah
     *  dibawa `AvatarStorage.imageLoader` lewat okHttpClient bersama. */
    fun urlFoto(path: String?): String? {
        if (path.isNullOrBlank()) return null
        val objek = path.removePrefix("$BUCKET/")
        return "${SupabaseClient.BASE_URL}storage/v1/object/authenticated/$BUCKET/$objek"
    }
}
