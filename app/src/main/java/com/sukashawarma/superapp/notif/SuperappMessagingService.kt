package com.sukashawarma.superapp.notif

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.sukashawarma.superapp.R
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.feature.chat.ChatKehadiran
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
        Log.d(TAG, "FCM masuk: type=${message.data["type"]} url=${message.data["url"]}")
        val judul = message.data["title"] ?: message.notification?.title ?: "SUKA Kerja Superapps"
        val isi = message.data["body"] ?: message.notification?.body ?: return
        // `url` dibaca sebagai cadangan karena edge function `send-push` hanya
        // meneruskan title/body/type/url ke FCM — field lain di payload trigger
        // tidak sampai ke sini. Trigger karena itu menaruh rutenya di `url`.
        // `route` didahulukan supaya tetap benar bila edge function kelak
        // meneruskannya, tanpa perlu mengubah apa pun di sisi ini.
        val mentah = message.data["route"] ?: message.data["url"]

        // Push notifikasi pesan chat pribadi 1-on-1
        if (message.data["type"] == "private_chat" || mentah?.startsWith("/chat/private") == true) {
            val pengirimId = message.data["sender_id"]
                ?: mentah?.substringAfter("from=", "")?.takeIf { it.isNotBlank() }
            if (pengirimId != null && pengirimId == AppSession.staff.value?.id) {
                return
            }
            // Langsung akui tersampaikan ke Supabase di latar belakang agar pengirim mendapat centang 2 abu
            lingkup.launch {
                try {
                    com.sukashawarma.superapp.feature.chat.data.PrivateChatRepository.tandaiSemuaTersampaikan()
                } catch (e: Exception) {
                    Log.e(TAG, "Gagal mengakui chat pribadi tersampaikan", e)
                }
            }

            if (ChatKehadiran.terbuka) {
                Log.d(TAG, "Push chat pribadi dilewati: layar chat sedang terlihat.")
                return
            }

            val pengirim = message.data["sender"]?.takeIf { it.isNotBlank() }
                ?: message.data["title"]?.takeIf { it.isNotBlank() }
                ?: judul
            // Notifikasi percakapan TERSENDIRI per lawan bicara, bukan menumpang
            // notifikasi Chat Tim: pesan dari dua orang berbeda tidak boleh
            // saling menimpa, dan masing-masing membawa foto profilnya sendiri.
            ChatPribadiNotifikasi.tampilkan(
                context = this,
                pengirimId = pengirimId ?: pengirim,
                pengirimNama = pengirim,
                isi = isi,
                avatarPath = message.data["sender_avatar"]?.takeIf { it.isNotBlank() },
            )
            return
        }

        // Push notifikasi pesan obrolan grup area (Area Manager & Crew)
        if (message.data["type"] == "area_chat" || mentah?.startsWith("/chat/area") == true) {
            // Chat Area belum dirilis ke produksi: jangan tampilkan notifikasinya.
            if (!com.sukashawarma.superapp.BuildConfig.DEBUG) return
            val pengirimId = message.data["sender_id"]
                ?: mentah?.substringAfter("from=", "")?.takeIf { it.isNotBlank() }
            if (pengirimId != null && pengirimId == AppSession.staff.value?.id) {
                Log.d(TAG, "Push chat area dilewati: pesan dari diri sendiri.")
                return
            }
            if (ChatKehadiran.terbuka) {
                Log.d(TAG, "Push chat area dilewati: layar chat sedang terlihat.")
                return
            }

            val areaId = message.data["area_id"]
                ?: mentah?.substringAfter("id=", "")?.substringBefore("&")?.takeIf { it.isNotBlank() }
                ?: "area_umum"
            val namaArea = message.data["group_name"]
                ?: message.data["title"]?.takeIf { it.isNotBlank() }
                ?: "Obrolan Area"
            val pengirim = message.data["sender_name"]
                ?: message.data["sender"]?.takeIf { it.isNotBlank() }
                ?: judul
            val avatar = message.data["sender_avatar"]?.takeIf { it.isNotBlank() }
                ?: message.data["group_photo"]?.takeIf { it.isNotBlank() }

            ChatAreaNotifikasi.tampilkan(
                context = this,
                areaId = areaId,
                namaArea = namaArea,
                pengirim = pengirim,
                isi = isi,
                avatarPath = avatar,
                disebut = message.data["mention"] == "1",
            )
            return
        }

        // Chat datang lewat edge function `send-chat-push` dengan type 'chat'
        // dan membawa payload lengkap: nama grup, foto grup, dan nama pengirim.
        // (Jalur lama '/chat?from=' ikut dikenali supaya perangkat yang belum
        // memperbarui basis datanya tidak kehilangan notifikasi.)
        if (message.data["type"] == "chat" || mentah?.startsWith("/chat") == true) {
            val pengirimId = message.data["sender_id"]
                ?: mentah?.substringAfter("from=", "")?.takeIf { it.isNotBlank() }
            // Jangan memberi tahu seseorang tentang pesannya sendiri, dan jangan
            // berbunyi untuk percakapan yang sedang dibuka di layar.
            //
            // Kedua penolakan ini DICATAT. Tanpa jejaknya, notifikasi yang tidak
            // muncul tidak bisa dibedakan dari notifikasi yang tidak pernah
            // sampai — dan penelusurannya terpaksa menebak ke arah server.
            if (pengirimId != null && pengirimId == AppSession.staff.value?.id) {
                Log.d(TAG, "Push chat dilewati: pesan dari diri sendiri.")
                return
            }
            if (ChatKehadiran.terbuka) {
                Log.d(TAG, "Push chat dilewati: layar chat sedang terlihat.")
                return
            }
            // Seluruh kunci payload dicatat, bukan hanya yang dipakai. Penanda
            // sebutan lahir tiga lapis di atas sini (trigger -> edge function ->
            // FCM); tanpa jejak isi payload yang benar-benar sampai, "penanda
            // tidak muncul" mustahil dibedakan dari "penanda tidak pernah
            // dikirim".
            // Isi pesannya TIDAK ikut dicatat — jejak notifikasi tidak boleh
            // menumpahkan percakapan orang ke Logcat.
            Log.d(TAG, "Push chat diterima, disebut=" + (message.data["mention"] == "1"))

            val namaGrup = message.data["title"]?.takeIf { it.isNotBlank() } ?: ChatKehadiran.namaGrup
            val pengirim = message.data["sender"]?.takeIf { it.isNotBlank() } ?: judul
            ChatNotifikasi.tampilkan(
                context = this,
                pengirim = pengirim,
                isi = isi,
                namaGrup = namaGrup,
                fotoGrupPath = message.data["group_photo"]?.takeIf { it.isNotBlank() },
                disebut = message.data["mention"] == "1",
            )
            return
        }

        // Cek apakah push merupakan notifikasi petty cash yang membutuhkan tindakan segera
        val tipe = message.data["type"]
        val isPettyCash = tipe == "petty_cash_urgent" || mentah in setOf(
            NotifikasiTujuan.MANAGER_PETTY_CASH,
            NotifikasiTujuan.LEADER_PETTY_CASH
        )

        if (isPettyCash) {
            Log.i(TAG, "Push petty cash mendesak diterima, menyalakan alarm continuous.")
            tampilkanPettyCashUrgent(judul, isi, mentah)
            return
        }

        tampilkan(judul, isi, mentah)
    }

    private fun tampilkanPettyCashUrgent(judul: String, isi: String, rute: String?) {
        siapkanSaluranPettyCash(this)

        // 1. Siapkan intent buka halaman persetujuan saat notifikasi diklik
        val tujuan = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            if (!rute.isNullOrBlank()) putExtra(NotifikasiTujuan.EXTRA_RUTE, rute)
            putExtra(PettyCashAlarmManager.EXTRA_STOP_ALARM, true)
        }

        val buka = PendingIntent.getActivity(
            this,
            PettyCashAlarmManager.NOTIF_ID_PETTY_CASH,
            tujuan,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // 2. Bangun notifikasi murni pesan (tanpa tombol Tolak/Jawab) agar teks utuh & jelas terbaca
        val notifBuilder = NotificationCompat.Builder(this, SALURAN_PETTY_CASH)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(judul)
            .setContentText(isi)
            .setStyle(NotificationCompat.BigTextStyle().bigText(isi))
            .setFullScreenIntent(buka, true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            // Ongoing memastikan banner menetap di atas layar dan tidak bisa diswipe
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(buka)

        fun kirimNotif() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            NotificationManagerCompat.from(this).notify(
                PettyCashAlarmManager.NOTIF_ID_PETTY_CASH,
                notifBuilder.build()
            )
        }

        // 4. Nyalakan getar berkelanjutan (30s on / 2s off) & ringtone telepon berulang
        PettyCashAlarmManager.mulaiAlarm(this) {
            kirimNotif()
        }

        kirimNotif()
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
        private const val TAG = "SuperappMessaging"

        const val SALURAN = "suka_superapp_umum"
        const val SALURAN_PETTY_CASH = "suka_petty_cash_urgent_v2"

        private val RUTE_DIKENAL = setOf(
            NotifikasiTujuan.MANAGER_PERSETUJUAN,
            NotifikasiTujuan.MANAGER_WASTE,
            NotifikasiTujuan.MANAGER_PETTY_CASH,
            NotifikasiTujuan.LEADER_PETTY_CASH,
            NotifikasiTujuan.CHAT,
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

        fun siapkanSaluranPettyCash(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manajer = context.getSystemService(NotificationManager::class.java) ?: return
            if (manajer.getNotificationChannel(SALURAN_PETTY_CASH) != null) return
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val audioAttrs = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .build()
            manajer.createNotificationChannel(
                NotificationChannel(SALURAN_PETTY_CASH, "Petty Cash Mendesak", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Peringatan darurat pengajuan dan penyerahan petty cash."
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 1000, 500)
                    setSound(soundUri, audioAttrs)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                    setShowBadge(true)
                }
            )
        }
    }
}
