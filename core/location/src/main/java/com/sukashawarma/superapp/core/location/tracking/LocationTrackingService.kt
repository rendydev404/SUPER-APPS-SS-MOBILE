package com.sukashawarma.superapp.data.location

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import androidx.core.app.ServiceCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.sukashawarma.superapp.domain.session.AppSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant

/**
 * Foreground service pelacakan lokasi kerja (bagian SOP). Persetujuannya adalah dialog
 * izin lokasi Android itu sendiri — tidak ada sakelar di dalam app; staff yang ingin
 * mematikannya mencabut izin lokasi lewat Setelan sistem.
 *
 * Interval adaptif: sistem hanya perlu titik rapat saat staff benar-benar berpindah;
 * saat diam, 1 titik per menit sudah cukup untuk dashboard dan jauh lebih hemat baterai.
 *
 * Tiga lapis pertahanan supaya pelacakan tidak putus saat app ditutup, task disingkirkan
 * dari Recent Apps, atau layar mati:
 *  1. `START_STICKY` + `stopWithTask="false"` — sistem membangun ulang service sendiri.
 *  2. [onTaskRemoved] menjadwalkan alarm restart, untuk OEM yang membunuh proses saat
 *     task digeser dari Recent Apps tanpa menghormati START_STICKY.
 *  3. Alarm watchdog 15 menit ([LocationTracking.scheduleWatchdog]) sebagai jaring
 *     terakhir, plus watchdog internal yang memasang ulang langganan lokasi bila
 *     FusedLocationProvider berhenti mengirim fix diam-diam.
 */
class LocationTrackingService : Service() {

    companion object {
        const val ACTION_START = "com.sukashawarma.superapp.location.START"
        const val ACTION_STOP = "com.sukashawarma.superapp.location.STOP"

        /** Dikirim sistem saat user menggeser notifikasi. Sejak Android 14 notifikasi
         *  foreground service BISA disingkirkan dengan swipe (service-nya tetap jalan),
         *  jadi notifikasi ini dipasang ulang begitu dibuang: notifikasi tersebut adalah
         *  satu-satunya penanda ke staff bahwa lokasinya sedang direkam, dan menutupinya
         *  membuat pelacakan berjalan diam-diam. */
        const val ACTION_NOTIF_DISMISSED = "com.sukashawarma.superapp.location.NOTIF_DISMISSED"

        private const val TAG = "LocTracking"
        // v2 karena importance sebuah channel tidak bisa diturunkan setelah dibuat —
        // Android mengabaikan perubahannya. Channel lama dihapus di buildNotification
        // supaya tidak menyisakan entri usang di Setelan notifikasi.
        private const val CHANNEL_ID = "location_sharing_v2"
        private const val CHANNEL_ID_LAMA = "location_sharing"
        /** Grup terpisah dari notifikasi lain milik app, lihat buildNotification. */
        private const val GROUP_LAYANAN = "suka_layanan_latar"
        private const val NOTIF_ID = 4711

        private const val INTERVAL_MOVING_MS = 10_000L
        private const val INTERVAL_IDLE_MS = 60_000L

        /** Ambang "bergerak". Di bawah ini GPS umumnya hanya derau saat orang berdiri diam. */
        private const val MOVING_SPEED_MPS = 0.7f

        /** Batas akurasi yang masih layak disimpan. Di atas ini bukan fix GPS. */
        private const val MAX_ACCURACY_M = 150f

        private const val HEALTH_CHECK_MS = 60_000L

        /** Tiga kali interval diam. Lewat ini langganan dianggap mati diam-diam — kondisi
         *  yang benar-benar terjadi setelah Doze dalam atau saat Play services di-update. */
        private const val FIX_STALE_MS = 3 * INTERVAL_IDLE_MS
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val queueLock = Mutex()
    private lateinit var client: FusedLocationProviderClient

    /** Antrean in-memory yang dicerminkan ke prefs setiap perubahan, supaya titik tidak
     *  hilang kalau proses dibunuh sistem saat jaringan sedang mati. */
    private val pending = ArrayDeque<TrackPoint>()

    private var movingMode: Boolean? = null
    private var tracking = false
    private var lastFixAt = 0L
    private var healthJob: Job? = null
    private var sessionJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            lastFixAt = SystemClock.elapsedRealtime()

            // Fix di atas ambang ini berasal dari menara seluler/Wi-Fi, bukan GPS: posisinya
            // bisa meleset ratusan meter dan kalau ikut tersimpan, jejak di dashboard
            // menunjukkan staff "berpindah" padahal dia tidak beranjak. Dibuang di sumber
            // supaya tidak perlu dibersihkan berulang kali di sisi web.
            if (location.accuracy > MAX_ACCURACY_M) {
                Log.d(TAG, "Fix diabaikan, akurasi ${location.accuracy}m di atas ambang")
                return
            }

            val speed = if (location.hasSpeed()) location.speed else 0f
            val moving = speed > MOVING_SPEED_MPS
            if (moving != movingMode) requestUpdates(moving)

            val battery = readBattery()
            val point = TrackPoint(
                lat = location.latitude,
                lng = location.longitude,
                accuracyM = location.accuracy,
                speedMps = speed,
                headingDeg = if (location.hasBearing()) location.bearing else 0f,
                altitudeM = if (location.hasAltitude()) location.altitude else 0.0,
                batteryPct = battery.first,
                isCharging = battery.second,
                isMock = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) location.isMock else @Suppress("DEPRECATION") location.isFromMockProvider,
                isMoving = moving,
                provider = location.provider ?: "fused",
                recordedAt = Instant.ofEpochMilli(location.time).toString(),
            )
            enqueueAndFlush(point)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        client = LocationServices.getFusedLocationProviderClient(this)
        LocationTrackingPrefs.loadQueue(this).forEach { line ->
            LocationTrackingRepository.decode(line)?.let(pending::addLast)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_NOTIF_DISMISSED) {
            // Pasang ulang tanpa menyentuh langganan lokasi yang sedang berjalan.
            if (LocationTrackingPrefs.isEnabled(this)) {
                getSystemService(NotificationManager::class.java).notify(NOTIF_ID, buildNotification())
            }
            return START_STICKY
        }

        if (intent?.action == ACTION_STOP) {
            LocationTrackingPrefs.setEnabled(this, false)
            LocationTracking.cancelWatchdog(this)
            stopTracking()
            return START_NOT_STICKY
        }

        // `intent == null` berarti sistem yang menghidupkan ulang service ini setelah proses
        // dibunuh (START_STICKY). Yang menentukan boleh-tidaknya lanjut adalah flag prefs,
        // bukan asal intent: kalau user sudah mematikan berbagi lokasi, restart apa pun
        // harus berhenti di sini.
        val explicitStart = intent?.action == ACTION_START
        if (!explicitStart && !LocationTrackingPrefs.isEnabled(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (!LocationTracking.hasForegroundPermission(this)) {
            Log.w(TAG, "Izin lokasi tidak ada saat service dimulai")
            stopSelf()
            return START_NOT_STICKY
        }

        if (!startForegroundSafely()) return START_NOT_STICKY

        LocationTrackingPrefs.setEnabled(this, true)
        LocationTracking.scheduleWatchdog(this)
        acquireWakeLock()

        // Sesi staff belum tentu ada saat sistem membangun ulang proses (auto-login berjalan
        // di Activity yang mungkin tidak pernah dibuka). Service TIDAK berhenti karena itu:
        // GPS tetap direkam ke antrean dan auto-login dipicu dari sini, lalu seluruh antrean
        // ikut terkirim begitu sesi kembali. Menghentikan service di sini — seperti versi
        // sebelumnya — berarti setiap kali proses app mati, pelacakan padam sampai staff
        // membuka app secara manual, tanpa tahu apa pun sedang tidak berjalan.
        if (AppSession.staff.value == null) ensureSession()

        startTracking()
        return START_STICKY
    }

    /**
     * OEM Xiaomi/Oppo/Vivo/Realme membunuh proses saat task digeser dari Recent Apps,
     * tanpa menghormati START_STICKY. Alarm restart dijadwalkan LEBIH DULU di sini —
     * satu-satunya callback yang pasti dieksekusi sebelum proses hilang.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        if (LocationTrackingPrefs.isEnabled(this)) {
            LocationTracking.scheduleImmediateRestart(this)
            LocationTracking.scheduleWatchdog(this)
        }
        super.onTaskRemoved(rootIntent)
    }

    /** Android 12+ menolak `startForeground` bila service dimulai dari background.
     *  Kalau ditolak, service dimatikan diam-diam — flag prefs tetap menyala sehingga
     *  percobaan berikutnya (alarm watchdog atau saat app dibuka) berhasil. */
    private fun startForegroundSafely(): Boolean = try {
        ServiceCompat.startForeground(
            this,
            NOTIF_ID,
            buildNotification(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0,
        )
        true
    } catch (e: Exception) {
        Log.w(TAG, "startForeground ditolak sistem", e)
        stopSelf()
        false
    }

    /**
     * WakeLock parsial menjaga CPU tetap hidup saat layar mati. Foreground service saja
     * hanya melindungi proses dari pembunuhan — ia tidak mencegah Doze menidurkan CPU,
     * dan tanpa ini upload titik tertahan sampai layar dinyalakan lagi. Biayanya baterai,
     * dan itu memang konsekuensi yang diminta dari pelacakan "tidak putus".
     */
    @SuppressLint("WakelockTimeout")
    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "suka:location-tracking").apply {
            setReferenceCounted(false)
            try {
                acquire()
            } catch (e: Exception) {
                Log.w(TAG, "WakeLock gagal diambil", e)
            }
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.takeIf { it.isHeld }?.release()
        } catch (e: Exception) {
            Log.w(TAG, "WakeLock gagal dilepas", e)
        }
        wakeLock = null
    }

    /** Service hanya memakai sesi yang sudah aktif di memory. Service tidak pernah
     *  melakukan login atau membuka refresh token secara diam-diam. */
    private fun ensureSession() {
        if (sessionJob?.isActive == true) return
        sessionJob = scope.launch {
            if (AppSession.staff.value == null) {
                Log.w(TAG, "Sesi staff tidak aktif; titik tetap diantrekan")
            } else {
                flushQueue()
            }
        }
    }

    private fun startTracking() {
        if (!tracking) {
            tracking = true
            requestUpdates(moving = false)
        }
        startHealthCheck()
    }

    /**
     * FusedLocationProvider kadang berhenti mengirim fix tanpa error apa pun — Play services
     * di-update, Doze dalam, atau GPS dimatikan lalu dinyalakan lagi. Tidak ada callback
     * untuk kondisi itu, jadi satu-satunya cara mendeteksinya adalah memeriksa kapan fix
     * terakhir masuk dan memasang ulang langganan bila sudah basi.
     */
    private fun startHealthCheck() {
        if (healthJob?.isActive == true) return
        lastFixAt = SystemClock.elapsedRealtime()
        healthJob = scope.launch {
            while (isActive) {
                delay(HEALTH_CHECK_MS)
                if (!tracking) continue
                val idleFor = SystemClock.elapsedRealtime() - lastFixAt
                if (idleFor > FIX_STALE_MS) {
                    Log.w(TAG, "Tidak ada fix ${idleFor / 1000}s, langganan dipasang ulang")
                    lastFixAt = SystemClock.elapsedRealtime()
                    movingMode = null
                    requestUpdates(moving = false)
                }
                // Antrean menumpuk saat jaringan mati; coba kirim lagi tanpa menunggu
                // fix berikutnya, yang saat diam bisa satu menit lagi.
                if (pending.isNotEmpty()) flushQueue()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestUpdates(moving: Boolean) {
        val interval = if (moving) INTERVAL_MOVING_MS else INTERVAL_IDLE_MS
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, interval)
            .setMinUpdateIntervalMillis(interval / 2)
            .setWaitForAccurateLocation(true)
            // Filter jarak hanya saat bergerak. Saat diam, filter 15 m membuat perangkat
            // tidak mengirim apa pun sehingga posisi di dashboard terlihat basi/hilang.
            .setMinUpdateDistanceMeters(if (moving) 15f else 0f)
            .build()
        try {
            client.removeLocationUpdates(callback)
            client.requestLocationUpdates(request, callback, mainLooper)
            movingMode = moving
        } catch (e: SecurityException) {
            Log.w(TAG, "Izin lokasi dicabut saat tracking berjalan", e)
            LocationTrackingPrefs.setEnabled(this, false)
            LocationTracking.cancelWatchdog(this)
            stopTracking()
        }
    }

    private fun enqueueAndFlush(point: TrackPoint) {
        scope.launch {
            queueLock.withLock {
                pending.addLast(point)
                persistQueue()
            }
            flushQueue()
        }
    }

    private fun flushQueue() {
        scope.launch {
            queueLock.withLock {
                if (pending.isEmpty()) return@withLock
                val batch = pending.toList()
                try {
                    LocationTrackingRepository.push(batch, DeviceInfo.name(this@LocationTrackingService))
                    // Hanya titik yang benar-benar terkirim yang dibuang; fix baru yang masuk
                    // selama upload berjalan tidak ikut hilang.
                    repeat(batch.size.coerceAtMost(pending.size)) { pending.removeFirst() }
                    persistQueue()
                } catch (e: LocationTrackingRepository.NoStaffSessionException) {
                    // Sesi belum pulih. Titik ditahan di antrean (bukan dibuang, dan service
                    // TIDAK dimatikan) lalu auto-login dicoba di latar belakang.
                    ensureSession()
                } catch (e: Exception) {
                    // Jaringan mati / server error: titik tetap di antrean dan ikut terkirim
                    // pada percobaan berikutnya. Satu kegagalan tidak boleh mematikan service.
                    Log.w(TAG, "Upload posisi gagal, ${pending.size} titik menunggu", e)
                }
            }
        }
    }

    private fun persistQueue() {
        LocationTrackingPrefs.saveQueue(this, pending.map { LocationTrackingRepository.encode(it) })
    }

    private fun readBattery(): Pair<Int, Boolean> {
        val manager = getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val level = manager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        val status = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        return (if (level in 0..100) level else 0) to charging
    }

    private fun stopTracking() {
        tracking = false
        healthJob?.cancel()
        healthJob = null
        releaseWakeLock()
        try {
            client.removeLocationUpdates(callback)
        } catch (e: Exception) {
            Log.w(TAG, "removeLocationUpdates gagal", e)
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        tracking = false
        releaseWakeLock()
        try {
            client.removeLocationUpdates(callback)
        } catch (e: Exception) {
            Log.w(TAG, "removeLocationUpdates gagal saat destroy", e)
        }
        scope.cancel()
        super.onDestroy()
    }

    /** Logo berwarna penuh untuk ikon besar; ikon kecil di bilah status hanya bisa siluet. */
    private fun logoAplikasi(): android.graphics.Bitmap? = try {
        when (val icon = packageManager.getApplicationIcon(packageName)) {
            is android.graphics.drawable.BitmapDrawable -> icon.bitmap
            else -> android.graphics.Bitmap.createBitmap(
                icon.intrinsicWidth.coerceAtLeast(1),
                icon.intrinsicHeight.coerceAtLeast(1),
                android.graphics.Bitmap.Config.ARGB_8888,
            ).also { bitmap ->
                val canvas = android.graphics.Canvas(bitmap)
                icon.setBounds(0, 0, canvas.width, canvas.height)
                icon.draw(canvas)
            }
        }
    } catch (_: Exception) {
        null
    }

    private fun buildNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        // Dipanggil tanpa syarat: pada channel yang sudah ada, createNotificationChannel
        // memperbarui nama dan deskripsinya. Kalau dilewati saat channel sudah ada, teks
        // lama akan menetap di layar Setelan notifikasi sampai app di-uninstall.
        // IMPORTANCE_MIN sempat dicoba supaya ikonnya tidak menetap di bilah status.
        // Tidak bisa: Android menaikkan paksa notifikasi foreground service ke LOW
        // (terbukti di perangkat, importance tetap 2). Karena ikonnya memang tidak
        // bisa disembunyikan, yang dilakukan adalah menjadikannya logo kita sendiri.
        manager.deleteNotificationChannel(CHANNEL_ID_LAMA)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Akurasi lokasi absensi", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Menandakan izin lokasi untuk absensi sedang aktif."
                setShowBadge(false)
            }
        )

        val dismissIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, LocationTrackingService::class.java).setAction(ACTION_NOTIF_DISMISSED),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val openIntent = packageManager.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 1, it, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        val builder = Notification.Builder(this, CHANNEL_ID)
            // Logo SUKA berwarna, bukan ikon bawaan Android: notifikasi ini menetap
            // sepanjang hari, jadi ikon inilah yang paling sering dilihat staff di
            // bilah status. Lihat catatan ikon di AbsenReminder.show.
            .setSmallIcon(com.sukashawarma.superapp.core.location.R.drawable.ic_notif_logo)
            .setColor(0xFFEA580C.toInt())
            .setLargeIcon(logoAplikasi())
            .setContentTitle("Izin lokasi aktif")
            .setContentText("Absensi sekarang sudah akurat")
            // setOngoing: notifikasi tidak ikut terhapus oleh tombol "Hapus semua".
            // Swipe satuan tetap mungkin di Android 14+, itu ditangani deleteIntent.
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            // Grup sendiri supaya Android tidak menjadikannya ringkasan yang menelan
            // notifikasi lain milik app ini.
            .setGroup(GROUP_LAYANAN)
            .setDeleteIntent(dismissIntent)
        if (openIntent != null) builder.setContentIntent(openIntent)
        return builder.build()
    }
}
