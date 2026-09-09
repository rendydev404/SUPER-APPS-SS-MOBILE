package com.sukashawarma.superapp.notif

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.sukashawarma.superapp.R
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.presentation.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Menerima push dari edge function `send-push`.
 *
 * Payload dikirim sebagai `data`, bukan `notification` — pesan bertipe data
 * selalu sampai ke sini termasuk saat aplikasi di latar, sehingga judul dan
 * tujuan ketukan bisa ditentukan aplikasi. Pesan `notification` akan digambar
 * sistem sendiri tanpa melewati kode ini ketika app tidak di depan.
 */
class SuperappMessagingService : FirebaseMessagingService() {

    private val lingkup = CoroutineScope(Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Token bisa diputar Firebase kapan saja. Tanpa pendaftaran ulang di sini,
        // device diam-diam berhenti menerima notifikasi sampai login berikutnya.
        val staf = AppSession.staff.value ?: return
        lingkup.launch { FcmTokenRegistrar.daftarkan(applicationContext, staf.id, staf.outletId) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val judul = message.data["title"] ?: message.notification?.title ?: "SUKA Superapp"
        val isi = message.data["body"] ?: message.notification?.body ?: return
        // `url` dibaca sebagai cadangan karena edge function `send-push` hanya
        // meneruskan title/body/type/url ke FCM — field lain di payload trigger
        // tidak sampai ke sini. Trigger karena itu menaruh rutenya di `url`.
        // `route` didahulukan supaya tetap benar bila edge function kelak
        // meneruskannya, tanpa perlu mengubah apa pun di sisi ini.
        tampilkan(judul, isi, message.data["route"] ?: message.data["url"])
    }

    private fun tampilkan(judul: String, isi: String, rute: String?) {
        siapkanSaluran(this)

        val tujuan = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        // Hanya rute yang dikenali yang diteruskan. Payload datang dari jaringan,
        // jadi nilainya tidak boleh langsung dipercaya sebagai tujuan navigasi.
        if (rute in RUTE_DIKENAL) tujuan.putExtra(NotifikasiTujuan.EXTRA_RUTE, rute)

        val buka = PendingIntent.getActivity(
            this,
            // requestCode dibedakan per rute: PendingIntent yang sama persis akan
            // dipakai ulang, sehingga notifikasi kedua membawa tujuan yang pertama.
            (rute ?: "").hashCode(),
            tujuan,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notif = NotificationCompat.Builder(this, SALURAN)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(judul)
            .setContentText(isi)
            // Teks laporan waste memuat nama outlet, bahan, jumlah, dan pelapor —
            // satu baris memotongnya tepat di bagian yang menentukan.
            .setStyle(NotificationCompat.BigTextStyle().bigText(isi))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(buka)
            .build()

        // Android 13+ menolak notifikasi tanpa izin POST_NOTIFICATIONS; menembakkannya
        // tanpa cek akan melempar SecurityException dan mematikan service.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notif)
    }

    companion object {
        const val SALURAN = "suka_superapp_umum"

        private val RUTE_DIKENAL = setOf(
            NotifikasiTujuan.MANAGER_PERSETUJUAN,
            NotifikasiTujuan.MANAGER_WASTE,
        )

        fun siapkanSaluran(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manajer = context.getSystemService(NotificationManager::class.java) ?: return
            if (manajer.getNotificationChannel(SALURAN) != null) return
            manajer.createNotificationChannel(
                NotificationChannel(SALURAN, "Pemberitahuan", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Persetujuan waste, permintaan, dan kabar operasional lain."
                }
            )
        }
    }
}
