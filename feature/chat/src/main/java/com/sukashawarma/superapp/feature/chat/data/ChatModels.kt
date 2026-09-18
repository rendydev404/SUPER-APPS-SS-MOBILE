package com.sukashawarma.superapp.feature.chat.data

import androidx.compose.runtime.Immutable
import com.google.gson.JsonObject
import java.time.OffsetDateTime

/**
 * Satu pesan chat, persis satu baris `chat_messages`.
 *
 * `senderName`/`senderAvatar` adalah SNAPSHOT saat pesan dikirim (diisi trigger
 * database, bukan join saat baca) — lihat migrasi 20300209000000. Kutipan reply
 * juga snapshot, supaya tetap tampil walau pesan asalnya sudah disapu job 24 jam.
 */
@Immutable
data class PesanChat(
    val id: String,
    val senderId: String,
    val senderName: String,
    val senderAvatar: String?,
    val body: String,
    val imagePath: String?,
    /** Path rekaman suara di bucket `chat-media`. null = bukan pesan suara. */
    val audioPath: String? = null,
    /** Durasi rekaman (ms) menurut perekam — dipakai menggambar bubble tanpa
     *  mengunduh berkasnya lebih dulu. */
    val audioMs: Int? = null,
    /** Amplitudo rekaman, satu digit '0'-'9' per bilah waveform. Kosong = belum
     *  ada sampel (pesan lama), bubble menggambar bilah rata. */
    val audioWave: String? = null,
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
    /**
     * Kapan pesan ini dihapus pengirimnya. null = masih utuh.
     *
     * Barisnya sengaja tetap ada. Menghapusnya sungguhan membuat percakapan
     * berlubang: balasan di bawahnya menunjuk pesan yang tidak ada lagi, dan
     * orang yang sudah membacanya melihat jawaban tanpa pertanyaan.
     */
    val deletedAtMs: Long? = null,
    /**
     * Nama pengelola yang menghapus pesan ini, bila BUKAN pengirimnya sendiri.
     *
     * null pada pesan yang dihapus pemiliknya. Dipakai membedakan nisan biasa
     * dari nisan hasil moderasi — tanpa itu, pengirimnya akan mengira dirinya
     * sendiri yang menghapus.
     */
    val deletedByName: String? = null,
)

/**
 * Satu orang yang disebut, beserta NAMA SAAT DISEBUT.
 *
 * Namanya ikut disimpan, bukan dicari ulang dari daftar anggota: pemiliknya bisa
 * menggantinya kapan saja, dan potongan teks "@Budi" di pesan lama harus tetap
 * tersorot walau orangnya kini bernama lain.
 */
@Immutable
data class Sebutan(val id: String, val nama: String)

/** Satu reaksi emoji pada sebuah pesan. Satu orang hanya punya satu per pesan. */
@Immutable
data class ReaksiPesan(
    val messageId: String,
    val userId: String,
    val userName: String,
    val emoji: String,
)

/** Pengaturan grup, baris tunggal `chat_settings`. */
@Immutable
data class PengaturanGrup(
    val namaGrup: String = "Chat Tim",
    val deskripsi: String = "Ruang obrolan seluruh tim. Pesan terhapus otomatis setiap 03:00 AM.",
    val hanyaAdmin: Boolean = false,
    val diubahOleh: String? = null,
    /** Path objek di bucket `avatars` — bukan `chat-media`, yang disapu tiap jam. */
    val fotoGrup: String? = null,
    /** ID wallpaper pilihan. Bawaan "default" (latar putih polos). */
    val wallpaper: String = "default",
)

/** Satu catatan pembacaan pesan (baris `chat_message_reads`). */
@Immutable
data class BacaanPesan(
    val messageId: String,
    val userId: String,
    val userName: String = "",
    val readAtMs: Long = 0L,
)

/** Satu anggota pembaca pada lembar Info Pesan. */
@Immutable
data class PembacaPesan(
    val userId: String,
    val nama: String,
    val displayUsername: String?,
    val avatarUrl: String?,
    val role: String?,
    val outletNama: String?,
    val readAtMs: Long?,
) {
    val namaTampil: String
        get() = displayUsername?.takeIf { it.isNotBlank() } ?: nama
}

/** Detail lengkap info pesan untuk lembar Info Pesan ala WhatsApp. */
@Immutable
data class DetailInfoPesan(
    val messageId: String,
    val senderId: String,
    val createdAtMs: Long,
    val dibaca: List<PembacaPesan>,
    val belumDibaca: List<PembacaPesan>,
)

fun parseBacaan(o: JsonObject): BacaanPesan? {
    fun teks(k: String): String? = o.get(k)?.takeIf { !it.isJsonNull }?.asString
    val messageId = teks("message_id") ?: return null
    val userId = teks("user_id") ?: return null
    val readAt = teks("read_at")
    val readAtMs = readAt?.let {
        runCatching { OffsetDateTime.parse(it).toInstant().toEpochMilli() }.getOrNull()
    } ?: 0L
    return BacaanPesan(
        messageId = messageId,
        userId = userId,
        userName = teks("user_name").orEmpty(),
        readAtMs = readAtMs,
    )
}

fun parsePembacaPesan(o: JsonObject): PembacaPesan? {
    fun teks(k: String): String? = o.get(k)?.takeIf { !it.isJsonNull }?.asString
    val userId = teks("user_id") ?: return null
    val readAt = teks("read_at")
    val readAtMs = readAt?.let {
        runCatching { OffsetDateTime.parse(it).toInstant().toEpochMilli() }.getOrNull()
    }
    return PembacaPesan(
        userId = userId,
        nama = teks("nama")?.ifBlank { null } ?: "Tanpa Nama",
        displayUsername = teks("display_username"),
        avatarUrl = teks("avatar_url"),
        role = teks("role"),
        outletNama = teks("outlet_nama"),
        readAtMs = readAtMs,
    )
}

fun parseDetailInfoPesan(o: JsonObject): DetailInfoPesan? {
    fun teks(k: String): String? = o.get(k)?.takeIf { !it.isJsonNull }?.asString
    val messageId = teks("message_id") ?: return null
    val senderId = teks("sender_id") ?: return null
    val createdAt = teks("created_at")
    val createdAtMs = createdAt?.let {
        runCatching { OffsetDateTime.parse(it).toInstant().toEpochMilli() }.getOrNull()
    } ?: 0L

    val dibacaList = o.get("dibaca")?.takeIf { it.isJsonArray }?.asJsonArray?.mapNotNull {
        it?.takeIf { el -> el.isJsonObject }?.asJsonObject?.let(::parsePembacaPesan)
    }.orEmpty()

    val belumDibacaList = o.get("belum_dibaca")?.takeIf { it.isJsonArray }?.asJsonArray?.mapNotNull {
        it?.takeIf { el -> el.isJsonObject }?.asJsonObject?.let(::parsePembacaPesan)
    }.orEmpty()

    return DetailInfoPesan(
        messageId = messageId,
        senderId = senderId,
        createdAtMs = createdAtMs,
        dibaca = dibacaList,
        belumDibaca = belumDibacaList,
    )
}

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
        wallpaper = teks("wallpaper")?.ifBlank { null } ?: "default",
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
        audioPath = teks("audio_path"),
        audioMs = o.get("audio_ms")?.takeIf { !it.isJsonNull }?.asInt,
        audioWave = teks("audio_wave"),
        replyToId = teks("reply_to_id"),
        replyToName = teks("reply_to_name"),
        replyToSnippet = teks("reply_to_snippet"),
        replyToImage = teks("reply_to_image"),
        createdAtMs = createdAtMs,
        mentions = o.get("mentions")?.takeIf { it.isJsonArray }?.asJsonArray?.mapNotNull { m ->
            val obj = m?.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
            val mId = obj.get("id")?.takeIf { !it.isJsonNull }?.asString ?: return@mapNotNull null
            val nama = obj.get("nama")?.takeIf { !it.isJsonNull }?.asString ?: return@mapNotNull null
            Sebutan(mId, nama)
        }.orEmpty(),
        deletedByName = teks("deleted_by_name")?.ifBlank { null },
        deletedAtMs = teks("deleted_at")?.let {
            runCatching { OffsetDateTime.parse(it).toInstant().toEpochMilli() }.getOrNull()
        },
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
@Immutable
data class AnggotaGrup(
    val id: String,
    val nama: String,
    val username: String?,
    val avatar: String?,
    val role: String?,
    val outlet: String?,
) {
    /**
     * Nama yang tampil ke layar: username jika diisi, atau nama resmi bila kosong.
     */
    val namaTampil: String
        get() = username?.takeIf { it.isNotBlank() } ?: nama
}

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
    "driver" -> "Driver"
    "korlap" -> "Korlap"
    "kepala_outlet" -> "Kepala Outlet"
    else -> role.replace('_', ' ').replaceFirstChar { it.uppercase() }
}
