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
 * Notifikasi khusus obrolan grup Area Manager & Crew (Area Chat).
 *
 * Menggunakan saluran sendiri (`suka_chat_area`) dan id tersendiri per area
 * (`5200 + hash(area_id)`), sehingga tidak saling menimpa atau bercampur
 * dengan notifikasi Chat Tim Global (5171) maupun Chat Pribadi (5300+).
 */
object ChatAreaNotifikasi {

    const val SALURAN = "suka_chat_area"

    /** Basis id notifikasi; id sesungguhnya diturunkan dari areaId. */
    private const val BASIS_ID = 5200

    private data class Baris(
        val nama: String,
        val teks: String,
        val waktu: Long,
        val disebut: Boolean = false,
    )

    private const val MAKS_RIWAYAT = 6

    /** Riwayat singkat per grup area untuk MessagingStyle (hanya di memori). */
    private val riwayatPerArea = HashMap<String, ArrayDeque<Baris>>()

    /** Penanda sebutan terakhir per areaId. */
    private val disebutPerArea = HashMap<String, Boolean>()

    fun siapkanSaluran(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manajer = context.getSystemService(NotificationManager::class.java) ?: return
        if (manajer.getNotificationChannel(SALURAN) != null) return
        val saluran = NotificationChannel(
            SALURAN,
            "Obrolan Area",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Pemberitahuan percakapan ruang obrolan Area Manager dan kru cabang."
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
     * Tampilkan pesan grup area baru.
     */
    fun tampilkan(
        context: Context,
        areaId: String,
        namaArea: String,
        pengirim: String,
        isi: String,
        avatarPath: String? = null,
        disebut: Boolean = false,
    ) {
        siapkanSaluran(context)

        if (disebut) {
            disebutPerArea[areaId] = true
        }

        synchronized(riwayatPerArea) {
            val antrean = riwayatPerArea.getOrPut(areaId) { ArrayDeque() }
            antrean.addLast(Baris(pengirim, isi, System.currentTimeMillis(), disebut))
            while (antrean.size > MAKS_RIWAYAT) {
                val korban = antrean.indexOfFirst { !it.disebut }
                antrean.removeAt(if (korban >= 0) korban else 0)
            }
        }

        val foto = unduhFotoAvatar(context, avatarPath)
        val idPintasan = "chat_area_$areaId"
        pastikanPintasan(context, idPintasan, namaArea, foto, areaId)

        val notifId = BASIS_ID + (areaId.hashCode().absoluteValue % 100)
        val notif = bangun(context, areaId, namaArea, foto, idPintasan)
            .setOnlyAlertOnce(false)
            .build()

        if (!bolehTampil(context)) return
        NotificationManagerCompat.from(context).notify(notifId, notif)
    }

    private fun bangun(
        context: Context,
        areaId: String,
        namaArea: String,
        foto: Bitmap?,
        idPintasan: String,
    ): NotificationCompat.Builder {
        val aku = Person.Builder().setName("Anda").setKey("aku").build()
        val adaSebutan = disebutPerArea[areaId] == true
        val judul = if (adaSebutan) "$namaArea · Menyebut Anda" else namaArea

        val gaya = NotificationCompat.MessagingStyle(aku)
            .setConversationTitle(judul)
            .setGroupConversation(true)

        synchronized(riwayatPerArea) {
            val antrean = riwayatPerArea[areaId] ?: ArrayDeque()
            antrean.forEach { (nama, teks, waktu) ->
                val orang = if (nama == "Anda") null
                else Person.Builder().setName(nama).setKey(nama).build()
                gaya.addMessage(teks, waktu, orang)
            }
        }

        return NotificationCompat.Builder(context, SALURAN)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setLargeIcon(foto)
            .setStyle(gaya)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setShortcutId(idPintasan)
            .setContentIntent(intentBuka(context, areaId))
    }

    private fun pastikanPintasan(
        context: Context,
        idPintasan: String,
        namaArea: String,
        foto: Bitmap?,
        areaId: String,
    ) {
        try {
            val orangGrup = Person.Builder()
                .setName(namaArea)
                .setKey(idPintasan)
                .apply { if (foto != null) setIcon(IconCompat.createWithAdaptiveBitmap(foto)) }
                .build()

            val pintasan = ShortcutInfoCompat.Builder(context, idPintasan)
                .setShortLabel(namaArea)
                .setLongLived(true)
                .setIntent(
                    Intent(context, MainActivity::class.java)
                        .setAction(Intent.ACTION_VIEW)
                        .putExtra(NotifikasiTujuan.EXTRA_RUTE, NotifikasiTujuan.CHAT_AREA)
                        .putExtra(NotifikasiTujuan.EXTRA_AREA_ID, areaId)
                )
                .setPerson(orangGrup)
                .apply {
                    if (foto != null) setIcon(IconCompat.createWithAdaptiveBitmap(foto))
                    else setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
                }
                .setCategories(setOf("android.shortcut.conversation"))
                .build()

            ShortcutManagerCompat.pushDynamicShortcut(context, pintasan)
        } catch (e: Exception) {
            android.util.Log.w("ChatAreaNotifikasi", "pintasan area gagal: ${e.message}")
        }
    }

    private fun intentBuka(context: Context, areaId: String): PendingIntent {
        val niat = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(NotifikasiTujuan.EXTRA_RUTE, NotifikasiTujuan.CHAT_AREA)
            putExtra(NotifikasiTujuan.EXTRA_AREA_ID, areaId)
        }
        val reqCode = (BASIS_ID + (areaId.hashCode().absoluteValue % 100))
        return PendingIntent.getActivity(
            context,
            reqCode,
            niat,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun unduhFotoAvatar(context: Context, path: String?): Bitmap? {
        if (path.isNullOrBlank()) return null
        return try {
            val objek = path.removePrefix("avatars/")
            val url = "${SupabaseClient.BASE_URL}storage/v1/object/authenticated/avatars/$objek"
            val permintaan = Request.Builder().url(url).get().build()
            SupabaseClient.okHttpClient.newCall(permintaan).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val bytes = resp.body?.bytes() ?: return null
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun bolehTampil(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    /** Menutup notifikasi area tertentu saat ruang obrolan area tersebut dibuka. */
    fun tutup(context: Context, areaId: String) {
        val notifId = BASIS_ID + (areaId.hashCode().absoluteValue % 100)
        NotificationManagerCompat.from(context).cancel(notifId)
        disebutPerArea[areaId] = false
        synchronized(riwayatPerArea) {
            riwayatPerArea[areaId]?.clear()
        }
    }
}
