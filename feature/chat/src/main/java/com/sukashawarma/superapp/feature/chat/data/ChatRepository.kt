package com.sukashawarma.superapp.feature.chat.data

import com.google.gson.JsonObject
import com.sukashawarma.superapp.core.storage.StorageUtil
import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.SupabaseClient
import com.sukashawarma.superapp.feature.chat.domain.batasResetPesanMs
import java.time.Instant
import java.util.UUID

object ChatRepository {

    const val TABLE = "chat_messages"
    const val TABLE_REAKSI = "chat_message_reactions"
    const val TABLE_BACAAN = "chat_message_reads"
    const val TABLE_SUARA_PUTAR = "chat_voice_plays"
    const val TABLE_PENGATURAN = "chat_settings"
    private const val BUCKET = "chat-media"

    /** Role yang boleh mengubah pengaturan grup. Cermin `chat_is_pengelola()` di
     *  database — daftar di sini hanya menentukan apa yang TAMPIL; penegakannya
     *  tetap di RLS, jadi menyamakan keduanya wajib tapi tidak menggantikannya. */
    val ROLE_PENGELOLA = setOf("developer", "admin", "admin_hr")

    /** Batas atas satu pengambilan. Grup seramai apa pun jarang menembus ini
     *  dalam satu siklus harian; kalau sampai, yang terpotong adalah pesan TERLAMA. */
    private const val MAKS_PESAN = 500

    /**
     * Seluruh pesan dalam siklus aktif (sejak 03:00 AM WIB terakhir), urut naik (terlama dulu).
     */
    suspend fun ambilPesan(): List<PesanChat> {
        val cutoffMs = batasResetPesanMs(System.currentTimeMillis())
        val batas = Instant.ofEpochMilli(cutoffMs).toString()
        val rows = Postgrest.select(
            TABLE,
            listOf(
                "select" to "*",
                "created_at" to "gte.$batas",
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
    suspend fun kirim(
        body: String,
        imagePath: String? = null,
        replyToId: String? = null,
        mentions: List<Sebutan> = emptyList(),
        audioPath: String? = null,
        audioMs: Int? = null,
        audioWave: String? = null,
        stickerUrl: String? = null,
    ): PesanChat {
        val row = JsonObject().apply {
            addProperty("body", body)
            imagePath?.let { addProperty("image_path", it) }
            audioPath?.let { addProperty("audio_path", it) }
            audioMs?.let { addProperty("audio_ms", it) }
            audioWave?.takeIf { it.isNotBlank() }?.let { addProperty("audio_wave", it.take(56)) }
            stickerUrl?.let { addProperty("sticker_url", it) }
            replyToId?.let { addProperty("reply_to_id", it) }
            if (mentions.isNotEmpty()) {
                add("mentions", com.google.gson.JsonArray().apply {
                    // Dibatasi 20, sama dengan CHECK di database. Menahannya di
                    // sini membuat kelebihannya terpotong rapi alih-alih membuat
                    // seluruh pesan ditolak server.
                    mentions.distinctBy { it.id }.take(20).forEach { m ->
                        add(JsonObject().apply {
                            addProperty("id", m.id)
                            addProperty("nama", m.nama)
                        })
                    }
                })
            }
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

    /** Unggah rekaman suara. Folder pertama WAJIB id pengirim — syarat yang sama
     *  dengan foto, ditegakkan policy `chat_media_insert_self`. */
    suspend fun unggahSuara(senderId: String, m4a: ByteArray): String =
        StorageUtil.uploadM4a(BUCKET, "$senderId/${UUID.randomUUID()}.m4a", m4a)

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
        wallpaper: String? = null,
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
            wallpaper?.let { addProperty("wallpaper", it.trim().ifBlank { "default" }) }
        }
        val hasil = try {
            Postgrest.update(TABLE_PENGATURAN, listOf("id" to "eq.1"), patch)
        } catch (e: Exception) {
            // Jika kolom 'wallpaper' belum ada di remote table Supabase, lakukan fallback tanpa kolom wallpaper
            if (wallpaper != null && e.message?.contains("wallpaper", ignoreCase = true) == true) {
                patch.remove("wallpaper")
                Postgrest.update(TABLE_PENGATURAN, listOf("id" to "eq.1"), patch)
            } else {
                throw e
            }
        }
        // PostgREST membalas 200 dengan array kosong ketika RLS menyaring habis
        // barisnya. Tanpa pemeriksaan ini, penolakan izin terlihat seperti sukses.
        if (hasil.size() == 0) {
            throw IllegalStateException("Perubahan ditolak: akun Anda tidak berhak mengubah pengaturan grup.")
        }
    }

    /** Simpan perubahan wallpaper secara mandiri dan instan oleh pengelola. */
    suspend fun simpanWallpaper(wallpaper: String, olehNama: String) {
        val patch = JsonObject().apply {
            addProperty("wallpaper", wallpaper.trim().ifBlank { "default" })
            addProperty("diubah_oleh", olehNama)
            addProperty("diubah_pada", java.time.Instant.now().toString())
        }
        val hasil = Postgrest.update(TABLE_PENGATURAN, listOf("id" to "eq.1"), patch)
        if (hasil.size() == 0) {
            throw IllegalStateException("Perubahan ditolak: akun Anda tidak berhak mengubah pengaturan grup.")
        }
    }

    /**
     * Sunting isi pesan sendiri lewat RPC `chat_edit_pesan`.
     *
     * BUKAN PATCH ke tabel: `chat_messages` sengaja tanpa policy UPDATE, karena
     * policy per-baris akan sekaligus membuka `sender_id` dan `created_at` untuk
     * ditimpa. RPC-nya hanya menyentuh kolom `body` dan menegakkan sendiri batas
     * 15 menitnya — lihat migrasi 20300214000000.
     */
    suspend fun suntingPesan(id: String, body: String) {
        Postgrest.rpc(
            "chat_edit_pesan",
            JsonObject().apply {
                addProperty("p_id", id)
                addProperty("p_body", body)
            },
        )
    }

    /**
     * Hapus pesan milik sendiri — barisnya TETAP ADA sebagai nisan, isinya yang
     * dibuang. Lewat RPC, bukan DELETE: lihat migrasi 20300216000000.
     */
    suspend fun hapusPesan(id: String) {
        Postgrest.rpc("chat_hapus_pesan", JsonObject().apply { addProperty("p_id", id) })
    }

    /**
     * Ambil semua catatan bacaan pesan yang masih aktif (< 24 jam / sejak siklus aktif).
     */
    suspend fun ambilSemuaBacaan(): List<BacaanPesan> {
        val cutoffMs = batasResetPesanMs(System.currentTimeMillis())
        val batas = Instant.ofEpochMilli(cutoffMs).toString()
        val rows = Postgrest.select(
            TABLE_BACAAN,
            listOf(
                "select" to "*",
                "read_at" to "gte.$batas",
            ),
        )
        return rows.mapNotNull { it?.asJsonObject?.let(::parseBacaan) }
    }

    /**
     * Menandai batch pesan sebagai telah dibaca oleh user aktif lewat RPC `chat_tandai_dibaca`.
     */
    suspend fun tandaiDibaca(messageIds: List<String>): Int {
        if (messageIds.isEmpty()) return 0
        val payload = JsonObject().apply {
            add("p_message_ids", com.google.gson.JsonArray().apply {
                messageIds.distinct().forEach { add(it) }
            })
        }
        val res = Postgrest.rpc("chat_tandai_dibaca", payload)
        return runCatching { res.asInt }.getOrDefault(0)
    }

    /**
     * Ambil detail info pesan (dibaca dan belum dibaca) lewat RPC `chat_info_pesan`.
     */
    suspend fun ambilInfoPesan(messageId: String): DetailInfoPesan {
        val payload = JsonObject().apply {
            addProperty("p_message_id", messageId)
        }
        val res = Postgrest.rpc("chat_info_pesan", payload)
        if (!res.isJsonObject) {
            throw IllegalStateException("Server tidak mengembalikan format info pesan yang valid.")
        }
        return parseDetailInfoPesan(res.asJsonObject)
            ?: throw IllegalStateException("Gagal memproses data info pesan.")
    }

    /** URL yang bisa dimuat Coil untuk `image_path` ("chat-media/<uid>/<file>.webp").
     *  Bucket privat: endpoint `authenticated` menuntut Authorization, yang sudah
     *  dibawa `AvatarStorage.imageLoader` lewat okHttpClient bersama. */
    fun urlFoto(path: String?): String? {
        if (path.isNullOrBlank()) return null
        val objek = path.removePrefix("$BUCKET/")
        return "${SupabaseClient.BASE_URL}storage/v1/object/authenticated/$BUCKET/$objek"
    }

    /**
     * Tandai pesan suara sebagai sudah didengar oleh akun ini.
     *
     * RPC-nya sendiri yang menolak penanda dari pengirimnya — mendengar ulang
     * rekaman sendiri bukan kabar yang berguna bagi siapa pun.
     */
    suspend fun tandaiSuaraDiputar(messageId: String) {
        Postgrest.rpc(
            "chat_tandai_suara_diputar",
            JsonObject().apply { addProperty("p_message_id", messageId) },
        )
    }

    /** Daftar orang yang sudah mendengarkan sebuah pesan suara. */
    suspend fun ambilPendengarSuara(messageId: String): List<PembacaPesan> {
        val res = Postgrest.rpc(
            "chat_suara_pendengar",
            JsonObject().apply { addProperty("p_message_id", messageId) },
        )
        if (!res.isJsonArray) return emptyList()
        return res.asJsonArray.mapNotNull { it?.asJsonObject?.let(::parsePembacaPesan) }
    }

    /** URL rekaman suara — bucket dan aturan auth yang sama dengan [urlFoto]. */
    fun urlSuara(path: String?): String? = urlFoto(path)
}
