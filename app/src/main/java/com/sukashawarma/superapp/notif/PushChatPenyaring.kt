package com.sukashawarma.superapp.notif

/**
 * Memutuskan nasib sebuah push chat SEBELUM digambar jadi notifikasi.
 *
 * Murni fungsi atas payload — tanpa Android — supaya aturannya bisa diuji.
 *
 * Latar belakang: pesan pribadi pernah tampil di HP SEMUA orang sebagai
 * notifikasi "Chat Tim". Sumbernya trigger lama yang menyiram seluruh token
 * lewat `send-push` (payload bertanda `type: broadcast`, url `/chat?from=`),
 * dan aplikasi menelan payload itu di jalur grup tanpa bertanya. Penyaring ini
 * menutup pintunya di sisi perangkat, terlepas dari keadaan trigger di server:
 *
 *  - Payload pribadi hanya digambar bila `recipient_id` (kalau ada) adalah
 *    pengguna yang sedang login.
 *  - Payload bertanda `broadcast`/`info` — ciri `send-push`, bukan
 *    `send-chat-push` — ditolak di jalur chat mana pun, karena tidak ada cara
 *    membedakan pesan grup dari pesan pribadi yang bocor di dalamnya.
 */
object PushChatPenyaring {

    enum class Jenis { PRIBADI, AREA, GRUP, BUKAN_CHAT }

    /**
     * @param jenis  Jalur yang cocok dengan payload.
     * @param tolak  Alasan tidak ditampilkan; null berarti boleh tampil.
     * @param akuiTersampaikan Pesan pribadi memang untuk pengguna ini (walau
     *   tidak ditampilkan karena percakapannya sedang terbuka), sehingga
     *   centang "tersampaikan" tetap boleh dikirim ke server.
     */
    data class Hasil(
        val jenis: Jenis,
        val pengirimId: String?,
        val tolak: String? = null,
        val akuiTersampaikan: Boolean = false,
    )

    fun putuskan(
        data: Map<String, String>,
        sayaId: String?,
        partnerTerbuka: String?,
        chatTerbuka: Boolean,
    ): Hasil {
        val tipe = data["type"]
        val rute = data["route"] ?: data["url"]
        val pengirimId = data["sender_id"]?.takeIf { it.isNotBlank() }
            ?: rute?.substringAfter("from=", "")?.substringBefore("&")?.takeIf { it.isNotBlank() }
        val legacySendPush = tipe == "broadcast" || tipe == "info"

        val pribadi = tipe == "private_chat" || rute?.startsWith("/chat/private") == true
        if (pribadi) {
            val penerima = data["recipient_id"]?.takeIf { it.isNotBlank() }
            val tolak = when {
                legacySendPush -> "payload legacy send-push (siaran ke semua perangkat)"
                pengirimId != null && pengirimId == sayaId -> "pesan dari diri sendiri"
                penerima != null && sayaId != null && penerima != sayaId -> "bukan penerima pesan ini"
                pengirimId != null && pengirimId == partnerTerbuka -> "percakapan dengan pengirim sedang terbuka"
                else -> null
            }
            val untukSaya = tolak == null || tolak.startsWith("percakapan")
            return Hasil(Jenis.PRIBADI, pengirimId, tolak, akuiTersampaikan = untukSaya)
        }

        if (tipe == "area_chat" || rute?.startsWith("/chat/area") == true) {
            val tolak = when {
                legacySendPush -> "payload legacy send-push"
                pengirimId != null && pengirimId == sayaId -> "pesan dari diri sendiri"
                chatTerbuka -> "layar chat sedang terlihat"
                else -> null
            }
            return Hasil(Jenis.AREA, pengirimId, tolak)
        }

        if (tipe == "chat" || rute?.startsWith("/chat") == true) {
            val tolak = when {
                legacySendPush -> "payload legacy send-push; bisa berisi pesan pribadi yang bocor"
                pengirimId != null && pengirimId == sayaId -> "pesan dari diri sendiri"
                chatTerbuka -> "layar chat sedang terlihat"
                else -> null
            }
            return Hasil(Jenis.GRUP, pengirimId, tolak)
        }

        return Hasil(Jenis.BUKAN_CHAT, pengirimId)
    }
}
