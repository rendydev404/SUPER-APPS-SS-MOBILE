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
 * berbeda akan saling menimpa di dalam percakapan "Chat Tim" — persis yang
 * membuat pesan pribadi sebelumnya tampil sebagai baris polos tanpa wajah
 * pengirimnya.
 *
 * Satu notifikasi per lawan bicara, masing-masing dengan foto profilnya sendiri
 * sebagai ikon percakapan dan logo aplikasi sebagai ikon kecil — seperti
 * WhatsApp, di mana setiap obrolan berdiri sendiri di bilah notifikasi.
 */
object ChatPribadiNotifikasi {

    const val SALURAN = "suka_chat_pribadi"

    /** Basis id notifikasi; id sesungguhnya diturunkan dari id lawan bicara.
     *  Yang sudah dipakai: 4210 AbsenReminder, 4711 lokasi, 5171 chat grup. */
    private const val BASIS_ID = 5300

    private data class Baris(val dariSaya: Boolean, val teks: String, val waktu: Long)

    private const val MAKS_RIWAYAT = 6

    /** Riwayat per lawan bicara, hanya di memori — isinya toh berumur 24 jam. */
    private val riwayat = HashMap<String, ArrayDeque<Baris>>()

    /** Foto profil yang sudah diunduh, supaya tidak diambil ulang tiap pesan. */
    private val cacheFoto = HashMap<String, Bitmap?>()

    fun siapkanSaluran(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manajer = context.getSystemService(NotificationManager::class.java) ?: return
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
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build(),
            )
            enableVibration(true)
            setShowBadge(true)
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

        synchronized(riwayat) {
            val antrean = riwayat.getOrPut(pengirimId) { ArrayDeque() }
            antrean.addLast(Baris(false, isi, System.currentTimeMillis()))
            while (antrean.size > MAKS_RIWAYAT) antrean.removeFirst()
        }

        val foto = fotoProfil(pengirimId, avatarPath)
        pastikanPintasan(context, pengirimId, pengirimNama, foto)

        if (!bolehTampil(context)) return
        NotificationManagerCompat.from(context)
            .notify(idUntuk(pengirimId), bangun(context, pengirimId, pengirimNama, foto).build())
    }

    /** Tutup notifikasi percakapan pribadi tertentu — dipanggil saat obrolannya dibuka. */
    fun tutup(context: Context, pengirimId: String) {
        synchronized(riwayat) { riwayat.remove(pengirimId) }
        NotificationManagerCompat.from(context).cancel(idUntuk(pengirimId))
    }

    private fun idUntuk(pengirimId: String): Int =
        BASIS_ID + (pengirimId.hashCode().absoluteValue % 500)

    private fun bangun(
        context: Context,
        pengirimId: String,
        pengirimNama: String,
        foto: Bitmap?,
    ): NotificationCompat.Builder {
        val aku = Person.Builder().setName("Anda").setKey("aku").build()
        val dia = Person.Builder()
            .setName(pengirimNama)
            .setKey(pengirimId)
            .apply { if (foto != null) setIcon(IconCompat.createWithAdaptiveBitmap(foto)) }
            .build()

        // setGroupConversation(false): obrolan berdua. Sistem lalu menaruh nama
        // lawan bicara sebagai kepala notifikasi tanpa mengulangnya di tiap baris.
        val gaya = NotificationCompat.MessagingStyle(aku)
            .setConversationTitle(pengirimNama)
            .setGroupConversation(false)

        synchronized(riwayat) {
            riwayat[pengirimId]?.forEach { baris ->
                gaya.addMessage(baris.teks, baris.waktu, if (baris.dariSaya) null else dia)
            }
        }

        return NotificationCompat.Builder(context, SALURAN)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setLargeIcon(foto)
            .setStyle(gaya)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setShortcutId(pintasanId(pengirimId))
            .setContentIntent(intentBuka(context, pengirimId))
    }

    private fun pintasanId(pengirimId: String) = "chat_pribadi_$pengirimId"

    /**
     * Pintasan percakapan per lawan bicara: syarat agar Android 11+ menaruh
     * notifikasinya di bagian "Percakapan" lengkap dengan foto profilnya.
     */
    private fun pastikanPintasan(
        context: Context,
        pengirimId: String,
        pengirimNama: String,
        foto: Bitmap?,
    ) {
        try {
            val orang = Person.Builder()
                .setName(pengirimNama)
                .setKey(pengirimId)
                .apply { if (foto != null) setIcon(IconCompat.createWithAdaptiveBitmap(foto)) }
                .build()

            val pintasan = ShortcutInfoCompat.Builder(context, pintasanId(pengirimId))
                .setShortLabel(pengirimNama)
                .setLongLived(true)
                .setIntent(
                    Intent(context, MainActivity::class.java)
                        .setAction(Intent.ACTION_VIEW)
                        .putExtra(NotifikasiTujuan.EXTRA_RUTE, NotifikasiTujuan.CHAT)
                )
                .setPerson(orang)
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
    private fun fotoProfil(pengirimId: String, path: String?): Bitmap? {
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
        synchronized(cacheFoto) { cacheFoto[path] = hasil }
        return hasil
    }

    private fun intentBuka(context: Context, pengirimId: String): PendingIntent {
        val tujuan = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            // Membuka layar Chat; tab Pribadi dipilih pengguna dari sana.
            // Rute per-percakapan belum ada di NotifikasiTujuan, dan rute yang
            // tidak dikenali akan diabaikan diam-diam — lebih baik mendarat di
            // Chat daripada tidak ke mana-mana.
            .putExtra(NotifikasiTujuan.EXTRA_RUTE, NotifikasiTujuan.CHAT)
        return PendingIntent.getActivity(
            context,
            idUntuk(pengirimId),
            tujuan,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun bolehTampil(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ActivityCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }
}
