package com.sukashawarma.superapp.notif

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import com.sukashawarma.superapp.R
import com.sukashawarma.superapp.presentation.MainActivity

/**
 * Notifikasi khusus chat: gaya percakapan, bisa dibalas langsung dari bilah
 * notifikasi seperti WhatsApp, dan berbunyi memakai nada notifikasi bawaan
 * perangkat pengguna.
 *
 * SALURAN SENDIRI, bukan saluran "Pemberitahuan" umum yang dipakai kabar
 * operasional (persetujuan, waste, pesan dari owner). Dipisah karena keduanya
 * beda sifat: kabar operasional datang sesekali dan boleh menginterupsi, chat
 * datang sering dan pengguna harus bisa mengatur — atau membisukan — nadanya
 * sendiri tanpa ikut membisukan hal yang penting.
 *
 * MessagingStyle dipakai supaya sistem menumpuk beberapa pesan dalam satu
 * notifikasi percakapan (dan, di Android 11+, memenuhi syarat munculnya
 * gelembung percakapan) alih-alih menumpuk baris terpisah yang saling menimpa.
 */
object ChatNotifikasi {

    const val SALURAN = "suka_chat_pesan"

    /** Satu id tetap: semua pesan chat menempati notifikasi percakapan yang sama. */
    const val ID_NOTIF = 4711

    const val KUNCI_BALASAN = "balasan_chat"
    const val AKSI_BALAS = "com.sukashawarma.superapp.BALAS_CHAT"

    /** Riwayat singkat untuk MessagingStyle. Hanya di memori — notifikasi tidak
     *  perlu bertahan melewati matinya proses, dan isinya toh berumur 24 jam. */
    private val riwayat = ArrayDeque<Triple<String, String, Long>>()
    private const val MAKS_RIWAYAT = 6

    fun siapkanSaluran(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manajer = context.getSystemService(NotificationManager::class.java) ?: return
        if (manajer.getNotificationChannel(SALURAN) != null) return
        val saluran = NotificationChannel(
            SALURAN,
            "Pesan Chat",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Pesan baru di ruang Chat Tim."
            // Nada bawaan perangkat, bukan berkas suara milik aplikasi: yang
            // pengguna kenali sebagai 'ada pesan' adalah nada pilihannya sendiri.
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

    /** Menampilkan pesan baru, menumpuknya ke percakapan yang sedang tampil. */
    fun tampilkan(context: Context, pengirim: String, isi: String, namaGrup: String) {
        siapkanSaluran(context)

        synchronized(riwayat) {
            riwayat.addLast(Triple(pengirim, isi, System.currentTimeMillis()))
            while (riwayat.size > MAKS_RIWAYAT) riwayat.removeFirst()
        }

        val aku = Person.Builder().setName("Anda").setKey("aku").build()
        val gaya = NotificationCompat.MessagingStyle(aku)
            .setConversationTitle(namaGrup)
            .setGroupConversation(true)
        synchronized(riwayat) {
            riwayat.forEach { (nama, teks, waktu) ->
                gaya.addMessage(
                    teks,
                    waktu,
                    Person.Builder().setName(nama).setKey(nama).build(),
                )
            }
        }

        val notif = NotificationCompat.Builder(context, SALURAN)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setStyle(gaya)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setContentIntent(intentBuka(context))
            .addAction(aksiBalas(context))
            .addAction(aksiTandaiDibaca(context))
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(context).notify(ID_NOTIF, notif)
    }

    /**
     * Menyusun ulang notifikasi setelah balasan terkirim, dengan balasan itu
     * ikut tampil. Wajib: notifikasi yang punya RemoteInput akan terus
     * memperlihatkan pemintal "mengirim…" sampai diperbarui atau ditutup.
     */
    fun tampilkanBalasanTerkirim(context: Context, teks: String, namaGrup: String) {
        synchronized(riwayat) {
            riwayat.addLast(Triple("Anda", teks, System.currentTimeMillis()))
            while (riwayat.size > MAKS_RIWAYAT) riwayat.removeFirst()
        }
        val aku = Person.Builder().setName("Anda").setKey("aku").build()
        val gaya = NotificationCompat.MessagingStyle(aku)
            .setConversationTitle(namaGrup)
            .setGroupConversation(true)
        synchronized(riwayat) {
            riwayat.forEach { (nama, isi, waktu) ->
                val orang = if (nama == "Anda") null
                else Person.Builder().setName(nama).setKey(nama).build()
                gaya.addMessage(isi, waktu, orang)
            }
        }
        val notif = NotificationCompat.Builder(context, SALURAN)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setStyle(gaya)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            // Balasan sendiri tidak perlu berbunyi lagi.
            .setSilent(true)
            .setContentIntent(intentBuka(context))
            .addAction(aksiBalas(context))
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(context).notify(ID_NOTIF, notif)
    }

    fun tutup(context: Context) {
        synchronized(riwayat) { riwayat.clear() }
        NotificationManagerCompat.from(context).cancel(ID_NOTIF)
    }

    /** Pesan galat singkat di notifikasi yang sama — dipakai saat balasan gagal. */
    fun tampilkanGagal(context: Context, alasan: String) {
        siapkanSaluran(context)
        val notif = NotificationCompat.Builder(context, SALURAN)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Balasan gagal terkirim")
            .setContentText(alasan)
            .setStyle(NotificationCompat.BigTextStyle().bigText(alasan))
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setSilent(true)
            .setContentIntent(intentBuka(context))
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(context).notify(ID_NOTIF, notif)
    }

    private fun intentBuka(context: Context): PendingIntent {
        val tujuan = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(NotifikasiTujuan.EXTRA_RUTE, NotifikasiTujuan.CHAT)
        return PendingIntent.getActivity(
            context,
            NotifikasiTujuan.CHAT.hashCode(),
            tujuan,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun aksiBalas(context: Context): NotificationCompat.Action {
        val masukan = RemoteInput.Builder(KUNCI_BALASAN)
            .setLabel("Balas…")
            .build()

        // MUTABLE wajib: sistem menyisipkan teks balasan ke dalam Intent ini.
        // PendingIntent immutable akan membuat balasannya selalu kosong.
        val niat = Intent(context, BalasChatReceiver::class.java).setAction(AKSI_BALAS)
        val tertunda = PendingIntent.getBroadcast(
            context,
            1,
            niat,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )

        return NotificationCompat.Action.Builder(
            R.mipmap.ic_launcher, "Balas", tertunda,
        )
            .addRemoteInput(masukan)
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
            .setShowsUserInterface(false)
            .build()
    }

    private fun aksiTandaiDibaca(context: Context): NotificationCompat.Action {
        val niat = Intent(context, BalasChatReceiver::class.java)
            .setAction(BalasChatReceiver.AKSI_TANDAI_DIBACA)
        val tertunda = PendingIntent.getBroadcast(
            context,
            2,
            niat,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Action.Builder(
            R.mipmap.ic_launcher, "Tandai dibaca", tertunda,
        )
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ)
            .setShowsUserInterface(false)
            .build()
    }
}
