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
    /** Foto pesan yang dibalas, supaya kartu kutipan bisa menampilkan gambarnya
     *  — bukan sekadar teks keterangan yang membuat balasan foto tak dikenali. */
    val replyToImage: String?,
    val createdAtMs: Long,
    /** Kapan isi pesan terakhir disunting. null = belum pernah. */
    val editedAtMs: Long? = null,
    /** Orang yang disebut di pesan ini. Kosong = tidak menyebut siapa pun. */
    val mentions: List<Sebutan> = emptyList(),
)

/**
 * Satu orang yang disebut, beserta NAMA SAAT DISEBUT.
 *
 * Namanya ikut disimpan, bukan dicari ulang dari daftar anggota: pemiliknya bisa
 * menggantinya kapan saja, dan potongan teks "@Budi" di pesan lama harus tetap
 * tersorot walau orangnya kini bernama lain.
 */
data class Sebutan(val id: String, val nama: String)

/** Satu reaksi emoji pada sebuah pesan. Satu orang hanya punya satu per pesan. */
data class ReaksiPesan(
    val messageId: String,
    val userId: String,
    val userName: String,
    val emoji: String,
)

/** Pengaturan grup, baris tunggal `chat_settings`. */
data class PengaturanGrup(
    val namaGrup: String = "Chat Tim",
    val deskripsi: String = "Ruang obrolan seluruh tim. Pesan hilang setelah 24 jam.",
    val hanyaAdmin: Boolean = false,
    val diubahOleh: String? = null,
    /** Path objek di bucket `avatars` — bukan `chat-media`, yang disapu tiap jam. */
    val fotoGrup: String? = null,
)

fun parseReaksi(o: JsonObject): ReaksiPesan? {
    fun teks(k: String): String? = o.get(k)?.takeIf { !it.isJsonNull }?.asString
    return ReaksiPesan(
        messageId = teks("message_id") ?: return null,
        userId = teks("user_id") ?: return null,
        userName = teks("user_name").orEmpty(),
        emoji = teks("emoji") ?: return null,
    )
}

fun parsePengaturan(o: JsonObject): PengaturanGrup {
    fun teks(k: String): String? = o.get(k)?.takeIf { !it.isJsonNull }?.asString
    return PengaturanGrup(
        namaGrup = teks("nama_grup")?.ifBlank { null } ?: "Chat Tim",
        deskripsi = teks("deskripsi").orEmpty(),
        hanyaAdmin = o.get("hanya_admin")?.takeIf { !it.isJsonNull }?.asBoolean ?: false,
        diubahOleh = teks("diubah_oleh"),
        fotoGrup = teks("foto_grup"),
    )
}

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
        replyToImage = teks("reply_to_image"),
        createdAtMs = createdAtMs,
        mentions = o.get("mentions")?.takeIf { it.isJsonArray }?.asJsonArray?.mapNotNull { m ->
            val obj = m?.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
            val id = obj.get("id")?.takeIf { !it.isJsonNull }?.asString ?: return@mapNotNull null
            val nama = obj.get("nama")?.takeIf { !it.isJsonNull }?.asString ?: return@mapNotNull null
            Sebutan(id, nama)
        }.orEmpty(),
        editedAtMs = teks("edited_at")?.let {
            runCatching { OffsetDateTime.parse(it).toInstant().toEpochMilli() }.getOrNull()
        },
    )
}

/**
 * Satu anggota grup, sebagaimana dibuka RPC `chat_daftar_anggota`.
 *
 * Sengaja BUKAN `StaffProfile`: yang boleh dilihat lintas-outlet hanya enam
 * kolom tampilan ini. Memakai model kepegawaian yang penuh di sini akan
 * mengundang layar lain membaca kolom yang tidak pernah dikirim server.
 */
data class AnggotaGrup(
    val id: String,
    val nama: String,
    val username: String?,
    val avatar: String?,
    val role: String?,
    val outlet: String?,
)

fun parseAnggota(o: JsonObject): AnggotaGrup? {
    fun teks(k: String): String? = o.get(k)?.takeIf { !it.isJsonNull }?.asString
    return AnggotaGrup(
        id = teks("id") ?: return null,
        nama = teks("nama")?.ifBlank { null } ?: "Tanpa Nama",
        username = teks("display_username"),
        avatar = teks("avatar_url"),
        role = teks("role"),
        outlet = teks("outlet_nama"),
    )
}

/** Nama jabatan yang layak dibaca manusia, bukan kode mentah basis data. */
fun labelRole(role: String?): String = when (role) {
    null, "" -> "Anggota"
    "admin" -> "Admin"
    "admin_hr" -> "Admin HR"
    "admin_finance" -> "Admin Finance"
    "owner" -> "Owner"
    "spv" -> "Supervisor"
    "regional_manager" -> "Regional Manager"
    "area_manager" -> "Area Manager"
    "leader" -> "Leader"
    "crew" -> "Crew"
    "kitchen" -> "Kitchen"
    "kiosk" -> "Kiosk"
    "mitra" -> "Mitra"
    "staff_pusat" -> "Staff Pusat"
    "purchasing" -> "Purchasing"
    "developer" -> "Developer"
    "korlap" -> "Korlap"
    "kepala_outlet" -> "Kepala Outlet"
    else -> role.replace('_', ' ').replaceFirstChar { it.uppercase() }
}
