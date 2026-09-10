package com.sukashawarma.superapp.feature.chat.data

import com.google.gson.JsonObject
import com.sukashawarma.superapp.core.storage.StorageUtil
import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.SupabaseClient
import java.time.Instant
import java.util.UUID

object ChatRepository {

    const val TABLE = "chat_messages"
    const val TABLE_REAKSI = "chat_message_reactions"
    const val TABLE_PENGATURAN = "chat_settings"
    private const val BUCKET = "chat-media"

    /** Role yang boleh mengubah pengaturan grup. Cermin `chat_is_pengelola()` di
     *  database — daftar di sini hanya menentukan apa yang TAMPIL; penegakannya
     *  tetap di RLS, jadi menyamakan keduanya wajib tapi tidak menggantikannya. */
    val ROLE_PENGELOLA = setOf("developer", "admin", "admin_hr")

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
        StorageUtil.uploadWebp(BUCKET, "$senderId/${UUID.randomUUID()}.webp", webp, upsert = false)

    /**
     * Semua reaksi untuk pesan yang sedang tampil.
     *
     * Tabelnya kecil (ikut terhapus bersama pesannya dalam 24 jam), jadi diambil
     * utuh sekali jalan alih-alih per pesan — satu permintaan, bukan puluhan.
     */
    suspend fun ambilReaksi(): List<ReaksiPesan> {
        val rows = Postgrest.select(TABLE_REAKSI, listOf("select" to "*"))
        return rows.mapNotNull { it?.asJsonObject?.let(::parseReaksi) }
    }

    /**
     * Pasang, ganti, atau cabut reaksi. Emoji yang sama dengan yang sedang
     * terpasang berarti mencabut — perilaku WhatsApp.
     */
    suspend fun setReaksi(messageId: String, emoji: String?, userId: String) {
        if (emoji == null) {
            Postgrest.delete(
                TABLE_REAKSI,
                listOf("message_id" to "eq.$messageId", "user_id" to "eq.$userId"),
            )
            return
        }
        val row = JsonObject().apply {
            addProperty("message_id", messageId)
            addProperty("user_id", userId)
            addProperty("emoji", emoji)
        }
        // on_conflict pada kunci utama: satu orang satu reaksi per pesan, memilih
        // emoji lain menggantikan yang lama alih-alih gagal duplikat.
        Postgrest.upsert(TABLE_REAKSI, row, onConflict = "message_id,user_id")
    }

    /**
     * Anggota grup, lewat RPC `chat_daftar_anggota`.
     *
     * BUKAN select ke `outlet_staff`: RLS tabel itu hanya membuka baris sendiri
     * (dan outlet binaan bagi sebagian peran), jadi kru biasa akan menerima
     * daftar berisi satu orang. RPC-nya SECURITY DEFINER dan hanya membuka
     * kolom tampilan — lihat migrasi 20300213000000.
     */
    suspend fun ambilAnggota(): List<AnggotaGrup> {
        val hasil = Postgrest.rpc("chat_daftar_anggota")
        if (!hasil.isJsonArray) return emptyList()
        return hasil.asJsonArray.mapNotNull { it?.asJsonObject?.let(::parseAnggota) }
    }

    suspend fun ambilPengaturan(): PengaturanGrup {
        val row = Postgrest.selectOne(TABLE_PENGATURAN, listOf("select" to "*", "id" to "eq.1"))
        return row?.let(::parsePengaturan) ?: PengaturanGrup()
    }

    /**
     * Unggah foto grup ke bucket `avatars`, BUKAN `chat-media`.
     *
     * `chat-media` disapu job pembersih setiap jam, jadi foto grup yang ditaruh
     * di sana akan lenyap sehari kemudian. Folder pertama tetap harus id
     * pengunggah — itu syarat policy `avatars_insert_self`.
     */
    suspend fun unggahFotoGrup(userId: String, jpeg: ByteArray): String =
        StorageUtil.uploadJpeg("avatars", "$userId/grup-${UUID.randomUUID()}.jpg", jpeg)

    /** RLS `chat_settings_update_pengelola` menolak pemanggil di luar allowlist. */
    suspend fun simpanPengaturan(
        nama: String,
        deskripsi: String,
        hanyaAdmin: Boolean,
        olehNama: String,
        fotoGrup: String? = null,
    ) {
        val patch = JsonObject().apply {
            addProperty("nama_grup", nama.trim().ifBlank { "Chat Tim" })
            addProperty("deskripsi", deskripsi.trim())
            addProperty("hanya_admin", hanyaAdmin)
            addProperty("diubah_oleh", olehNama)
            addProperty("diubah_pada", java.time.Instant.now().toString())
            // null = jangan sentuh foto yang sudah ada; string kosong = hapus.
            // Tanpa pembedaan ini, menyimpan nama grup akan ikut menghapus fotonya.
            fotoGrup?.let { addProperty("foto_grup", it.ifBlank { null }) }
        }
        val hasil = Postgrest.update(TABLE_PENGATURAN, listOf("id" to "eq.1"), patch)
        // PostgREST membalas 200 dengan array kosong ketika RLS menyaring habis
        // barisnya. Tanpa pemeriksaan ini, penolakan izin terlihat seperti sukses.
        if (hasil.size() == 0) {
            throw IllegalStateException("Perubahan ditolak: akun Anda tidak berhak mengubah pengaturan grup.")
        }
    }

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
