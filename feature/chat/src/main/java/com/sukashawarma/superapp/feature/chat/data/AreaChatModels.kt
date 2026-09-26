package com.sukashawarma.superapp.feature.chat.data

import androidx.compose.runtime.Immutable
import com.google.gson.JsonObject
import java.time.OffsetDateTime

/**
 * Metadata satu Area binaan Area Manager.
 */
@Immutable
data class AreaInfo(
    val areaId: String,
    val namaArea: String,
    val amName: String,
    val amAvatar: String? = null,
    val outlets: List<String> = emptyList(),
    val deskripsi: String = "Ruang obrolan tim Area Manager bersama crew dan leader.",
) {
    val ringkasanOutlet: String
        get() = if (outlets.isEmpty()) "Seluruh Outlet" else outlets.joinToString(" • ")
}

/**
 * Satu pesan dalam grup obrolan area.
 */
@Immutable
data class PesanAreaChat(
    val id: String,
    val areaId: String,
    val senderId: String,
    val senderName: String,
    val senderAvatar: String?,
    val senderRole: String?,
    val outletName: String?,
    val body: String,
    val imagePath: String?,
    val audioPath: String? = null,
    val audioMs: Int? = null,
    val audioWave: String? = null,
    /** URL media KLIPY (WebP animasi) bila pesan ini stiker. Dimuat langsung dari CDN
     *  KLIPY, tidak lewat storage kita. `body` pesan stiker berisi teks cadangan
     *  [TEKS_CADANGAN_STIKER] untuk app lama/web dan harus diabaikan saat ini terisi. */
    val stickerUrl: String? = null,
    val replyToId: String? = null,
    val replyToName: String? = null,
    val replyToSnippet: String? = null,
    val replyToImage: String? = null,
    val mentions: List<Sebutan> = emptyList(),
    val createdAtMs: Long,
    val editedAtMs: Long? = null,
    val deletedAtMs: Long? = null,
    val deletedByName: String? = null,
) {
    val isAreaManager: Boolean
        get() = senderRole.equals("area_manager", ignoreCase = true)

    val isLeader: Boolean
        get() = senderRole.equals("leader", ignoreCase = true)

    val isCrew: Boolean
        get() = senderRole.equals("crew", ignoreCase = true)
}

/**
 * Satu anggota tim dalam area (AM, Leader, atau Crew).
 */
@Immutable
data class AnggotaArea(
    val id: String,
    val nama: String,
    val username: String?,
    val avatar: String?,
    val role: String?,
    val outlet: String?,
) {
    val namaTampil: String
        get() = username?.takeIf { it.isNotBlank() } ?: nama

    val isAreaManager: Boolean
        get() = role.equals("area_manager", ignoreCase = true)

    val isLeader: Boolean
        get() = role.equals("leader", ignoreCase = true)
}

/** Parsing JsonObject ke [PesanAreaChat]. */
fun parsePesanAreaChat(o: JsonObject): PesanAreaChat? {
    fun teks(k: String): String? = o.get(k)?.takeIf { !it.isJsonNull }?.asString

    val id = teks("id") ?: return null
    val areaId = teks("area_id") ?: "umum"
    val senderId = teks("sender_id") ?: return null
    val createdAt = teks("created_at") ?: return null
    val createdAtMs = try {
        OffsetDateTime.parse(createdAt).toInstant().toEpochMilli()
    } catch (_: Exception) {
        return null
    }

    return PesanAreaChat(
        id = id,
        areaId = areaId,
        senderId = senderId,
        senderName = teks("sender_name").orEmpty().ifBlank { "Tanpa Nama" },
        senderAvatar = teks("sender_avatar"),
        senderRole = teks("sender_role"),
        outletName = teks("outlet_name"),
        body = teks("body").orEmpty(),
        imagePath = teks("image_path"),
        audioPath = teks("audio_path"),
        audioMs = o.get("audio_ms")?.takeIf { !it.isJsonNull }?.asInt,
        audioWave = teks("audio_wave"),
        stickerUrl = teks("sticker_url"),
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
 * Item tampilan obrolan area: Bubble pesan atau Pemisah tanggal.
 */
@Immutable
sealed interface ItemAreaChat {
    @Immutable
    data class Pemisah(val label: String) : ItemAreaChat

    @Immutable
    data class Bubble(
        val pesan: PesanAreaChat,
        val milikSendiri: Boolean,
        val posisi: com.sukashawarma.superapp.feature.chat.domain.PosisiGrup,
        val tampilkanIdentitas: Boolean,
        val jam: String = "",
    ) : ItemAreaChat
}

/**
 * Menyusun daftar item tampilan area chat dengan pengelompokan gelembung ala iOS/WhatsApp
 * dan pemisah tanggal cerdas.
 */
fun susunItemAreaChat(
    pesan: List<PesanAreaChat>,
    userId: String,
    nowMs: Long = System.currentTimeMillis(),
    zona: java.time.ZoneId = com.sukashawarma.superapp.feature.chat.domain.ZONA_WIB,
): List<ItemAreaChat> {
    if (pesan.isEmpty()) return emptyList()
    val hidup = pesan.sortedBy { it.createdAtMs }

    val hariIni = java.time.Instant.ofEpochMilli(nowMs).atZone(zona).toLocalDate()
    val hasil = ArrayList<ItemAreaChat>(hidup.size + 4)

    hidup.forEachIndexed { i, p ->
        val hari = java.time.Instant.ofEpochMilli(p.createdAtMs).atZone(zona).toLocalDate()
        val sebelum = hidup.getOrNull(i - 1)
        val sesudah = hidup.getOrNull(i + 1)

        val hariSebelum = if (sebelum != null) java.time.Instant.ofEpochMilli(sebelum.createdAtMs).atZone(zona).toLocalDate() else null
        if (hari != hariSebelum) {
            hasil += ItemAreaChat.Pemisah(com.sukashawarma.superapp.feature.chat.domain.labelTanggal(hari, hariIni))
        }

        val nyambungAtas = sebelum != null && sebelum.senderId == p.senderId &&
            hari == hariSebelum && p.createdAtMs - sebelum.createdAtMs <= com.sukashawarma.superapp.feature.chat.domain.JARAK_GRUP_MS
        val hariSesudah = if (sesudah != null) java.time.Instant.ofEpochMilli(sesudah.createdAtMs).atZone(zona).toLocalDate() else null
        val nyambungBawah = sesudah != null && sesudah.senderId == p.senderId &&
            hari == hariSesudah && sesudah.createdAtMs - p.createdAtMs <= com.sukashawarma.superapp.feature.chat.domain.JARAK_GRUP_MS

        val posisi = when {
            !nyambungAtas && !nyambungBawah -> com.sukashawarma.superapp.feature.chat.domain.PosisiGrup.TUNGGAL
            !nyambungAtas -> com.sukashawarma.superapp.feature.chat.domain.PosisiGrup.AWAL
            nyambungBawah -> com.sukashawarma.superapp.feature.chat.domain.PosisiGrup.TENGAH
            else -> com.sukashawarma.superapp.feature.chat.domain.PosisiGrup.AKHIR
        }

        hasil += ItemAreaChat.Bubble(
            pesan = p,
            milikSendiri = p.senderId == userId,
            posisi = posisi,
            tampilkanIdentitas = p.senderId != userId && !nyambungAtas,
            jam = com.sukashawarma.superapp.feature.chat.domain.formatJamWib(p.createdAtMs, zona),
        )
    }
    return hasil
}
