package com.sukashawarma.superapp.notif

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.sukashawarma.superapp.R
import com.sukashawarma.superapp.data.remote.SupabaseClient
import com.sukashawarma.superapp.presentation.MainActivity
import okhttp3.Request
import kotlin.math.absoluteValue

/**
 * Notifikasi obrolan pribadi (1-on-1), terpisah dari notifikasi Chat Tim.
 *
 * Dipisah, BUKAN menumpang [ChatNotifikasi]: yang itu memakai satu id dan satu
 * pintasan percakapan untuk seluruh grup, sehingga pesan pribadi dari dua orang
 * berbeda akan saling menimpa di dalam percakapan "Chat Tim".
 *
 * Meniru WhatsApp:
 *  - Satu notifikasi per lawan bicara, dengan foto profilnya sebagai ikon
 *    percakapan, nama orangnya sebagai judul, dan jumlah pesan belum dibaca.
 *  - Ditumpuk dalam GRUP NOTIFIKASI sendiri ("Pesan Pribadi") dengan ringkasan,
 *    jadi tidak pernah bercampur dengan tumpukan Chat Tim.
 *  - Tombol "Balas" langsung dari bilah notifikasi dan "Tandai dibaca".
 *  - Mengetuknya membuka LANGSUNG percakapan orang itu.
 *  - Di layar kunci, isi pesan disembunyikan: orang yang lewat hanya melihat
 *    "Nama · 1 pesan baru", bukan isinya.
 */
object ChatPribadiNotifikasi {

    /**
     * Bersufiks `_v2`: pengaturan saluran (getar, lampu) dikunci Android begitu
     * dibuat, jadi perubahan gaya menuntut saluran baru. Saluran lama dihapus.
     */
    const val SALURAN = "suka_chat_pribadi_v2"
    private const val SALURAN_LAMA = "suka_chat_pribadi"

    /** Basis id notifikasi; id sesungguhnya diturunkan dari id lawan bicara.
     *  Yang sudah dipakai: 4210 AbsenReminder, 4711 lokasi, 5171 chat grup,
     *  5200–5299 chat area. */
    private const val BASIS_ID = 5300
    private const val RENTANG_ID = 500

    /** Id notifikasi ringkasan tumpukan pesan pribadi. */
    private const val ID_RINGKASAN = BASIS_ID + RENTANG_ID
    private const val KUNCI_GRUP = "suka_pesan_pribadi"

    const val KUNCI_BALASAN = "balasan_chat_pribadi"
    const val AKSI_BALAS = "com.sukashawarma.superapp.BALAS_CHAT_PRIBADI"
    const val AKSI_TANDAI_DIBACA = "com.sukashawarma.superapp.TANDAI_DIBACA_CHAT_PRIBADI"
    const val EXTRA_PARTNER_ID = "partner_id"
    const val EXTRA_PARTNER_NAMA = "partner_nama"
    const val EXTRA_PARTNER_AVATAR = "partner_avatar"

    /** Warna aksen notifikasi: biru yang sama dengan bubble chat pribadi di aplikasi. */
    private const val WARNA_AKSEN = 0xFF007AFF.toInt()

    private data class Baris(val dariSaya: Boolean, val teks: String, val waktu: Long)

    /** Keadaan satu percakapan yang sedang tampil di bilah notifikasi. */
    private class Percakapan(
        val partnerId: String,
        var nama: String,
        var avatarPath: String?,
    ) {
        val riwayat = ArrayDeque<Baris>()
        /** Pesan masuk sejak notifikasi terakhir ditutup — lencana angka. */
        var belumDibaca = 0
    }

    private const val MAKS_RIWAYAT = 8

    /** Per lawan bicara, hanya di memori — isinya toh berumur 24 jam. */
    private val percakapan = HashMap<String, Percakapan>()

    /** Foto profil yang sudah diunduh, supaya tidak diambil ulang tiap pesan. */
    private val cacheFoto = HashMap<String, Bitmap?>()

    fun siapkanSaluran(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manajer = context.getSystemService(NotificationManager::class.java) ?: return
        if (manajer.getNotificationChannel(SALURAN_LAMA) != null) {
            manajer.deleteNotificationChannel(SALURAN_LAMA)
        }
        if (manajer.getNotificationChannel(SALURAN) != null) return
        val saluran = NotificationChannel(
            SALURAN,
            "Pesan Pribadi",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Obrolan pribadi satu lawan satu antar staf."
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                    .build(),
            )
            enableVibration(true)
            // Getar dua ketukan pendek — beda rasa dari getar tunggal Chat Tim.
            vibrationPattern = longArrayOf(0, 120, 80, 120)
            enableLights(true)
            lightColor = WARNA_AKSEN
            setShowBadge(true)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
        }
        manajer.createNotificationChannel(saluran)
    }

    /**
     * Tampilkan pesan pribadi baru dari [pengirimId], menumpuknya ke notifikasi
     * percakapan orang yang sama.
     *
     * [avatarPath] adalah path objek di bucket `avatars`; boleh null — tanpa
     * foto, notifikasi tetap tampil dengan logo aplikasi.
     */
    fun tampilkan(
        context: Context,
        pengirimId: String,
        pengirimNama: String,
        isi: String,
        avatarPath: String? = null,
    ) {
        siapkanSaluran(context)

        val p = synchronized(percakapan) {
            percakapan.getOrPut(pengirimId) { Percakapan(pengirimId, pengirimNama, avatarPath) }.also {
                if (pengirimNama.isNotBlank()) it.nama = pengirimNama
                if (!avatarPath.isNullOrBlank()) it.avatarPath = avatarPath
                it.riwayat.addLast(Baris(false, isi, System.currentTimeMillis()))
                while (it.riwayat.size > MAKS_RIWAYAT) it.riwayat.removeFirst()
                it.belumDibaca += 1
            }
        }

        val foto = fotoProfil(p.avatarPath)
        pastikanPintasan(context, p, foto)

        if (!bolehTampil(context)) return
        val manajer = NotificationManagerCompat.from(context)
        manajer.notify(idUntuk(pengirimId), bangun(context, p, foto).setOnlyAlertOnce(false).build())
        manajer.notify(ID_RINGKASAN, bangunRingkasan(context).build())
    }

    /**
     * Menyusun ulang notifikasi setelah balasan dari bilah notifikasi terkirim,
     * dengan balasan itu ikut tampil. Wajib: notifikasi ber-RemoteInput terus
     * memperlihatkan pemintal "mengirim…" sampai diperbarui atau ditutup.
     */
    fun tampilkanBalasanTerkirim(context: Context, partnerId: String, teks: String) {
        val p = synchronized(percakapan) {
            percakapan[partnerId]?.also {
                it.riwayat.addLast(Baris(true, teks, System.currentTimeMillis()))
                while (it.riwayat.size > MAKS_RIWAYAT) it.riwayat.removeFirst()
                // Membalas berarti sudah membaca.
                it.belumDibaca = 0
            }
        } ?: return
        if (!bolehTampil(context)) return
        NotificationManagerCompat.from(context)
            .notify(idUntuk(partnerId), bangun(context, p, fotoProfil(p.avatarPath)).setSilent(true).build())
    }

    /** Pesan galat singkat di notifikasi yang sama — dipakai saat balasan gagal. */
    fun tampilkanGagal(context: Context, partnerId: String, alasan: String) {
        siapkanSaluran(context)
        val p = synchronized(percakapan) { percakapan[partnerId] }
        val notif = NotificationCompat.Builder(context, SALURAN)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setColor(WARNA_AKSEN)
            .setContentTitle("Balasan ke ${p?.nama ?: "rekan"} gagal terkirim")
            .setContentText(alasan)
            .setStyle(NotificationCompat.BigTextStyle().bigText(alasan))
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setGroup(KUNCI_GRUP)
            .setAutoCancel(true)
            .setSilent(true)
            .setContentIntent(intentBuka(context, partnerId, p?.nama ?: "Rekan kerja", p?.avatarPath))
            .build()
        if (!bolehTampil(context)) return
        NotificationManagerCompat.from(context).notify(idUntuk(partnerId), notif)
    }

    /** Tutup notifikasi percakapan pribadi tertentu — dipanggil saat obrolannya dibuka. */
    fun tutup(context: Context, partnerId: String) {
        val sisa = synchronized(percakapan) {
            percakapan.remove(partnerId)
            percakapan.size
        }
        val manajer = NotificationManagerCompat.from(context)
        manajer.cancel(idUntuk(partnerId))
        if (sisa == 0) {
            manajer.cancel(ID_RINGKASAN)
        } else if (bolehTampil(context)) {
            manajer.notify(ID_RINGKASAN, bangunRingkasan(context).build())
        }
    }

    private fun idUntuk(partnerId: String): Int =
        BASIS_ID + (partnerId.hashCode().absoluteValue % RENTANG_ID)

    private fun orang(p: Percakapan, foto: Bitmap?): Person =
        Person.Builder()
            .setName(p.nama)
            .setKey(p.partnerId)
            .setImportant(true)
            .apply { if (foto != null) setIcon(IconCompat.createWithAdaptiveBitmap(foto)) }
            .build()

    private fun bangun(context: Context, p: Percakapan, foto: Bitmap?): NotificationCompat.Builder {
        val aku = Person.Builder().setName("Anda").setKey("aku").build()
        val dia = orang(p, foto)

        // setGroupConversation(false) + tanpa judul percakapan: obrolan berdua.
        // Sistem menaruh nama lawan bicara sebagai kepala notifikasi tanpa
        // mengulangnya di tiap baris — tampilan 1:1 yang sama dengan WhatsApp.
        val gaya = NotificationCompat.MessagingStyle(aku).setGroupConversation(false)
        val terakhir: Baris?
        synchronized(percakapan) {
            p.riwayat.forEach { baris ->
                gaya.addMessage(baris.teks, baris.waktu, if (baris.dariSaya) null else dia)
            }
            terakhir = p.riwayat.lastOrNull()
        }

        // Versi layar kunci: nama dan jumlah saja, isinya tidak dibocorkan ke
        // siapa pun yang kebetulan melihat layar.
        val jumlah = p.belumDibaca
        val publik = NotificationCompat.Builder(context, SALURAN)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setColor(WARNA_AKSEN)
            .setContentTitle(p.nama)
            .setContentText(if (jumlah > 1) "$jumlah pesan baru" else "1 pesan baru")
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .build()

        return NotificationCompat.Builder(context, SALURAN)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setColor(WARNA_AKSEN)
            .setLargeIcon(foto)
            .setStyle(gaya)
            .addPerson(dia)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publik)
            .setNumber(jumlah)
            .setWhen(terakhir?.waktu ?: System.currentTimeMillis())
            .setShowWhen(true)
            .setGroup(KUNCI_GRUP)
            .setAutoCancel(true)
            .setShortcutId(pintasanId(p.partnerId))
            .setContentIntent(intentBuka(context, p.partnerId, p.nama, p.avatarPath))
            .addAction(aksiBalas(context, p))
            .addAction(aksiTandaiDibaca(context, p))
    }

    /**
     * Ringkasan tumpukan: "Pesan Pribadi · 3 percakapan". Tanpa ini Android
     * tidak menumpuk notifikasi per orang, dan bilah notifikasi bisa penuh
     * oleh satu obrolan ramai.
     */
    private fun bangunRingkasan(context: Context): NotificationCompat.Builder {
        val gaya = NotificationCompat.InboxStyle()
        var totalPesan = 0
        val jumlahOrang: Int
        synchronized(percakapan) {
            jumlahOrang = percakapan.size
            percakapan.values
                .sortedByDescending { it.riwayat.lastOrNull()?.waktu ?: 0L }
                .forEach { p ->
                    totalPesan += p.belumDibaca
                    val cuplikan = p.riwayat.lastOrNull { !it.dariSaya }?.teks ?: ""
                    gaya.addLine("${p.nama}: $cuplikan")
                }
        }
        val keterangan = "$totalPesan pesan dari $jumlahOrang percakapan"
        gaya.setBigContentTitle("Pesan Pribadi").setSummaryText(keterangan)

        return NotificationCompat.Builder(context, SALURAN)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setColor(WARNA_AKSEN)
            .setContentTitle("Pesan Pribadi")
            .setContentText(keterangan)
            .setStyle(gaya)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setGroup(KUNCI_GRUP)
            .setGroupSummary(true)
            // Anaknya yang berbunyi; ringkasan diam supaya tidak dobel.
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
            .setAutoCancel(true)
            .setContentIntent(intentBukaTabPribadi(context))
    }

    private fun pintasanId(partnerId: String) = "chat_pribadi_$partnerId"

    /**
     * Pintasan percakapan per lawan bicara: syarat agar Android 11+ menaruh
     * notifikasinya di bagian "Percakapan" lengkap dengan foto profilnya.
     */
    private fun pastikanPintasan(context: Context, p: Percakapan, foto: Bitmap?) {
        try {
            val pintasan = ShortcutInfoCompat.Builder(context, pintasanId(p.partnerId))
                .setShortLabel(p.nama)
                .setLongLived(true)
                .setIntent(niatBuka(context, p.partnerId, p.nama, p.avatarPath))
                .setPerson(orang(p, foto))
                .apply {
                    if (foto != null) setIcon(IconCompat.createWithAdaptiveBitmap(foto))
                    else setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
                }
                .setCategories(setOf("android.shortcut.conversation"))
                .build()

            ShortcutManagerCompat.pushDynamicShortcut(context, pintasan)
        } catch (e: Exception) {
            android.util.Log.w("ChatPribadiNotif", "pintasan percakapan gagal: ${e.message}")
        }
    }

    /**
     * Foto profil pengirim untuk ikon percakapan.
     *
     * Bucket `avatars` privat, jadi diunduh lewat klien OkHttp aplikasi yang
     * sudah membawa token sesi. Gagal unduh = notifikasi tampil tanpa foto;
     * notifikasi tidak boleh menunggu jaringan demi sebuah lingkaran kecil.
     */
    private fun fotoProfil(path: String?): Bitmap? {
        if (path.isNullOrBlank()) return null
        synchronized(cacheFoto) { if (cacheFoto.containsKey(path)) return cacheFoto[path] }

        val hasil = try {
            val objek = path.removePrefix("avatars/")
            val url = "${SupabaseClient.BASE_URL}storage/v1/object/authenticated/avatars/$objek"
            val permintaan = Request.Builder().url(url).get().build()
            SupabaseClient.okHttpClient.newCall(permintaan).execute().use { resp ->
                if (!resp.isSuccessful) null
                else resp.body?.bytes()?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
            }
        } catch (e: Exception) {
            android.util.Log.w("ChatPribadiNotif", "foto profil gagal dimuat: ${e.message}")
            null
        }
        // Kegagalan tidak di-cache: percobaan berikutnya (sesi sudah hidup)
        // masih boleh mengunduhnya.
        if (hasil != null) synchronized(cacheFoto) { cacheFoto[path] = hasil }
        return hasil
    }

    private fun niatBuka(context: Context, partnerId: String, nama: String, avatar: String?): Intent =
        Intent(context, MainActivity::class.java)
            .setAction(Intent.ACTION_VIEW)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(NotifikasiTujuan.EXTRA_RUTE, NotifikasiTujuan.CHAT_PRIBADI)
            .putExtra(NotifikasiTujuan.EXTRA_PARTNER_ID, partnerId)
            .putExtra(NotifikasiTujuan.EXTRA_PARTNER_NAMA, nama)
            .putExtra(NotifikasiTujuan.EXTRA_PARTNER_AVATAR, avatar)

    private fun intentBuka(context: Context, partnerId: String, nama: String, avatar: String?): PendingIntent =
        PendingIntent.getActivity(
            context,
            idUntuk(partnerId),
            niatBuka(context, partnerId, nama, avatar),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun intentBukaTabPribadi(context: Context): PendingIntent {
        val niat = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(NotifikasiTujuan.EXTRA_RUTE, NotifikasiTujuan.CHAT)
        return PendingIntent.getActivity(
            context, ID_RINGKASAN, niat,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun niatReceiver(context: Context, aksi: String, p: Percakapan): Intent =
        Intent(context, BalasChatPribadiReceiver::class.java)
            .setAction(aksi)
            .putExtra(EXTRA_PARTNER_ID, p.partnerId)
            .putExtra(EXTRA_PARTNER_NAMA, p.nama)
            .putExtra(EXTRA_PARTNER_AVATAR, p.avatarPath)

    private fun aksiBalas(context: Context, p: Percakapan): NotificationCompat.Action {
        val masukan = RemoteInput.Builder(KUNCI_BALASAN)
            .setLabel("Balas ${p.nama}…")
            .build()

        // MUTABLE wajib: sistem menyisipkan teks balasan ke dalam Intent ini.
        // Request code per orang, supaya PendingIntent dua percakapan tidak
        // saling menimpa extras-nya.
        val tertunda = PendingIntent.getBroadcast(
            context,
            idUntuk(p.partnerId),
            niatReceiver(context, AKSI_BALAS, p),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )

        return NotificationCompat.Action.Builder(R.mipmap.ic_launcher, "Balas", tertunda)
            .addRemoteInput(masukan)
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
            .setShowsUserInterface(false)
            .setAllowGeneratedReplies(true)
            .build()
    }

    private fun aksiTandaiDibaca(context: Context, p: Percakapan): NotificationCompat.Action {
        val tertunda = PendingIntent.getBroadcast(
            context,
            idUntuk(p.partnerId) + RENTANG_ID + 1,
            niatReceiver(context, AKSI_TANDAI_DIBACA, p),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Action.Builder(R.mipmap.ic_launcher, "Tandai dibaca", tertunda)
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ)
            .setShowsUserInterface(false)
            .build()
    }

    private fun bolehTampil(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        val punya = ActivityCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!punya) {
            android.util.Log.w("ChatPribadiNotif", "Notifikasi pesan pribadi dibatalkan: izin POST_NOTIFICATIONS belum ada.")
        }
        return punya
    }
}
