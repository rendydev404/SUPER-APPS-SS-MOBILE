package com.sukashawarma.superapp.feature.absensi.notif

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.sukashawarma.superapp.feature.absensi.R
import java.util.Calendar

/**
 * Pengingat absen harian pukul 12:10.
 *
 * Dijadwalkan lewat AlarmManager, bukan WorkManager, karena yang dijanjikan ke
 * staff adalah sebuah JAM — bukan "kira-kira sekali sehari". WorkManager bebas
 * menunda pekerjaan periodik sampai jendela pemeliharaan berikutnya, yang untuk
 * pengingat jam makan siang bisa berarti muncul jam dua.
 *
 * Alarm sekali-pakai, dijadwalkan ulang tiap kali berbunyi. Alarm berulang
 * (`setRepeating`) tidak dipakai karena Android sudah lama memaksa alarm berulang
 * jadi tidak presisi, dan jadwalnya juga hilang setelah perangkat dinyalakan
 * ulang — karena itu [AbsenReminderReceiver] ikut mendengar BOOT_COMPLETED.
 */
object AbsenReminder {

    private const val CHANNEL_ID = "absen_reminder"
    private const val GROUP_PENGINGAT = "suka_pengingat"
    private const val KEY_TERAKHIR = "terakhir_tayang_hari"
    private const val NOTIFICATION_ID = 4210
    private const val REQUEST_CODE = 4210

    const val JAM = 12
    const val MENIT = 10

    /**
     * Naskah notifikasi.
     *
     * Judul memakai kalimat asli yang diminta, apa adanya: register santai yang
     * dipakai orang di lapangan terbaca sebagai pesan dari rekan, bukan peringatan
     * sistem, dan itu yang membuatnya dibuka. Baris berikutnya baru menambahkan
     * hal yang membuat orang bergerak — jam yang konkret, biaya tindakan yang
     * kecil, dan akibat yang ingin dihindari.
     */
    private const val JUDUL = "Segera absenn broow, yokss jangan telatt"
    private const val RINGKAS = "Udah jam 12:10 nih, absen dulu yuk sebelum keburu lupa."
    private const val PANJANG =
        "Udah jam 12:10 nih. Absen sekarang mumpung inget — sekali ketuk doang, " +
            "terus kehadiran hari ini aman, nggak kecatat telat."

    /**
     * Channel dibuat ulang tanpa syarat: pada channel yang sudah ada, panggilan
     * ini tidak berefek, jadi aman dipanggil di tiap jalur masuk.
     *
     * IMPORTANCE_HIGH dipilih supaya notifikasi muncul melayang di atas layar
     * seperti pesan chat. Pengingat yang cuma mengendap di laci notifikasi akan
     * terbaca saat jam absen sudah lewat, dan pada saat itu ia tidak berguna lagi.
     */
    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Pengingat absen",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Pengingat harian untuk mengisi absensi tepat waktu."
            enableVibration(true)
            setShowBadge(true)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /**
     * Perlukah menayangkan susulan sekarang?
     *
     * Alarm tidak berbunyi saat perangkat mati, dan tidak ada yang menggantikannya
     * ketika menyala kembali — jadwal berikutnya melompat ke besok dan pengingat
     * hari itu hilang tanpa jejak. Staff yang HP-nya kehabisan baterai saat jam
     * makan siang justru yang paling butuh diingatkan.
     *
     * [terakhirTayang] adalah nomor hari epoch tayangan terakhir, -1 bila belum
     * pernah. Susulan hanya untuk HARI INI: kalau perangkat baru menyala besok,
     * pengingat kemarin sudah tidak ada gunanya.
     */
    internal fun perluSusulan(sekarang: Long, terakhirTayang: Long): Boolean {
        val kalender = Calendar.getInstance().apply { timeInMillis = sekarang }
        val hariIni = hariEpoch(kalender)
        if (terakhirTayang == hariIni) return false
        val jadwal = kalender.apply {
            set(Calendar.HOUR_OF_DAY, JAM); set(Calendar.MINUTE, MENIT)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return sekarang >= jadwal
    }

    private fun hariEpoch(kalender: Calendar): Long {
        val awal = (kalender.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        return awal.timeInMillis / 86_400_000L
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences("absen_reminder", Context.MODE_PRIVATE)

    /** Waktu tayang 12:10 berikutnya menurut jam perangkat. */
    internal fun waktuBerikutnya(sekarang: Long = System.currentTimeMillis()): Long {
        val target = Calendar.getInstance().apply {
            timeInMillis = sekarang
            set(Calendar.HOUR_OF_DAY, JAM)
            set(Calendar.MINUTE, MENIT)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.timeInMillis <= sekarang) target.add(Calendar.DAY_OF_YEAR, 1)
        return target.timeInMillis
    }

    fun schedule(context: Context) {
        ensureChannel(context)
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        val intent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, AbsenReminderReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val kapan = waktuBerikutnya()
        // Android 12+ boleh mencabut izin alarm presisi kapan saja. Kalau dicabut,
        // pengingat tetap jalan dengan toleransi beberapa menit — itu jauh lebih
        // baik daripada melempar SecurityException dan tidak mengingatkan sama sekali.
        val presisi = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()
        if (presisi) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, kapan, intent)
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, kapan, intent)
        }

        // Menyusul tayangan yang terlewat karena perangkat mati saat jam 12:10.
        // Dipanggil dari sini supaya tiap jalur yang memulihkan jadwal — boot,
        // pembaruan APK, dan pembukaan app — sekaligus jadi kesempatan menyusul.
        if (perluSusulan(System.currentTimeMillis(), prefs(context).getLong(KEY_TERAKHIR, -1))) {
            show(context)
        }
    }

    /**
     * Sengaja TIDAK disaring status login.
     *
     * Percobaan pertama menyaringnya dengan refresh token, dan itu keliru: token
     * hanya ditulis ke disk bila pengguna mengaktifkan biometrik, sehingga
     * pengingat mati untuk hampir semua staff — tanpa error, tanpa jejak. Status
     * login memang tidak bisa dibaca dari disk pada aplikasi ini, dan penyaring
     * yang salah pada pengingat harian gagal dengan cara paling buruk: senyap.
     * Aplikasi ini hanya terpasang di ponsel kerja, jadi menampilkannya tanpa
     * syarat adalah perilaku yang benar.
     */
    fun show(context: Context) {
        ensureChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        val buka = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val ketuk = buka?.let {
            PendingIntent.getActivity(
                context, REQUEST_CODE, it,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            // PNG logo BERWARNA, bukan siluet. One UI merender ikon kecil dari
            // resource bitmap apa adanya, jadi logo tampil utuh di bilah status —
            // diuji di Galaxy A07 (One UI 8.5). Pola yang sama dipakai aplikasi lain
            // seperti Kopi Kenangan. Di ROM yang menerapkan panduan Android secara
            // ketat, ikon ini turun jadi siluet putih; itu batas bawah yang bisa
            // diterima, bukan kerusakan.
            .setSmallIcon(R.drawable.ic_notif_logo)
            .setLargeIcon(logoAplikasi(context))
            .setColor(0xFFEA580C.toInt())
            .setContentTitle(JUDUL)
            .setContentText(RINGKAS)
            .setStyle(NotificationCompat.BigTextStyle().bigText(PANJANG))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            // Grup sendiri, terpisah dari notifikasi layanan lokasi yang menetap
            // sepanjang hari. Tanpa ini Android melipat pengingat di bawah notifikasi
            // lokasi dan yang terbaca staff justru "Izin lokasi aktif".
            .setGroup(GROUP_PENGINGAT)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
            .apply {
                ketuk?.let {
                    setContentIntent(it)
                    // Satu aksi saja. Menawarkan dua pilihan membuat orang berhenti
                    // untuk memilih; yang diinginkan di sini cuma satu tindakan.
                    addAction(R.drawable.ic_notif_logo, "Absen dulu yuk", it)
                }
            }
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notif)
        // Dicatat SETELAH benar-benar tayang, supaya tayangan yang gagal karena izin
        // dicabut tidak dianggap sudah lunas dan tetap bisa disusulkan.
        prefs(context).edit()
            .putLong(KEY_TERAKHIR, hariEpoch(Calendar.getInstance()))
            .apply()
    }

    /**
     * Logo berwarna penuh untuk ikon besar — inilah yang membuat notifikasi
     * dikenali sebagai milik SUKA di laci yang penuh, karena ikon bilah status
     * hanya boleh siluet. Diambil dari ikon aplikasi supaya tidak ada salinan
     * logo kedua yang bisa ketinggalan saat logo diganti.
     */
    private fun logoAplikasi(context: Context): Bitmap? = try {
        when (val icon = context.packageManager.getApplicationIcon(context.packageName)) {
            is BitmapDrawable -> icon.bitmap
            else -> Bitmap.createBitmap(
                icon.intrinsicWidth.coerceAtLeast(1),
                icon.intrinsicHeight.coerceAtLeast(1),
                Bitmap.Config.ARGB_8888,
            ).also { bitmap ->
                val canvas = Canvas(bitmap)
                icon.setBounds(0, 0, canvas.width, canvas.height)
                icon.draw(canvas)
            }
        }
    } catch (_: Exception) {
        null
    }
}
