package com.sukashawarma.superapp.data.location

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat

/** Titik masuk tunggal untuk menyalakan/mematikan pelacakan — UI dan Application tidak
 *  perlu tahu soal Intent action atau aturan foreground service. */
object LocationTracking {
    private const val TAG = "LocTracking"

    const val ACTION_WATCHDOG = "com.sukashawarma.superapp.location.WATCHDOG"
    const val ACTION_RESTART = "com.sukashawarma.superapp.location.RESTART"

    /** Selang jaring pengaman. 15 menit adalah granularitas terkecil yang masih dihormati
     *  sistem saat perangkat dalam Doze; lebih rapat dari ini hanya menghabiskan baterai
     *  tanpa dieksekusi lebih sering. */
    private const val WATCHDOG_INTERVAL_MS = 15 * 60 * 1000L

    private const val RESTART_DELAY_MS = 2_000L

    private const val REQ_WATCHDOG = 91
    private const val REQ_RESTART = 92

    fun hasForegroundPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    fun hasBackgroundPermission(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            // Sebelum Android 10 izin lokasi biasa sudah mencakup background.
            hasForegroundPermission(context)
        }

    fun hasNotificationPermission(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    fun isEnabled(context: Context): Boolean = LocationTrackingPrefs.isEnabled(context)

    fun start(context: Context) {
        if (!hasForegroundPermission(context)) return
        val intent = Intent(context, LocationTrackingService::class.java)
            .setAction(LocationTrackingService.ACTION_START)
        try {
            ContextCompat.startForegroundService(context, intent)
        } catch (e: Exception) {
            // ForegroundServiceStartNotAllowedException (Android 12+) saat app tidak di
            // foreground. Bukan kondisi fatal: alarm watchdog akan mencoba lagi, dan
            // percobaan pasti berhasil begitu app dibuka.
            Log.w(TAG, "Gagal memulai service pelacakan", e)
        }
    }

    fun stop(context: Context) {
        LocationTrackingPrefs.setEnabled(context, false)
        cancelWatchdog(context)
        val intent = Intent(context, LocationTrackingService::class.java)
            .setAction(LocationTrackingService.ACTION_STOP)
        try {
            ContextCompat.startForegroundService(context, intent)
        } catch (e: Exception) {
            context.stopService(Intent(context, LocationTrackingService::class.java))
        }
    }

    /** Dipanggil saat app kembali dibuka: user yang pernah menyetujui tidak perlu
     *  menyalakan ulang secara manual setelah HP restart atau service dimatikan OEM.
     *  Ini juga satu-satunya konteks yang PASTI boleh memulai foreground service lokasi
     *  di semua versi Android, jadi sekaligus dipakai untuk memulihkan rantai watchdog. */
    fun restartIfEnabled(context: Context) {
        if (!LocationTrackingPrefs.isEnabled(context)) return
        if (!hasForegroundPermission(context)) return
        scheduleWatchdog(context)
        start(context)
    }

    // --- Jaring pengaman: alarm yang menghidupkan ulang service bila dibunuh sistem/OEM ---

    private fun alarmManager(context: Context): AlarmManager? =
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    private fun pendingIntent(context: Context, requestCode: Int, action: String): PendingIntent =
        PendingIntent.getBroadcast(
            context.applicationContext,
            requestCode,
            Intent(context.applicationContext, LocationRestartReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    /**
     * Menjadwalkan SATU pemeriksaan berikutnya, bukan alarm berulang: setiap kali receiver
     * menyala ia menjadwalkan yang berikutnya lagi. Rantai satu-per-satu ini memakai
     * `setAndAllowWhileIdle` yang tetap dieksekusi di dalam Doze — `setRepeating` biasa
     * justru ditahan sistem persis saat kita paling membutuhkannya (layar mati semalaman).
     */
    fun scheduleWatchdog(context: Context) {
        schedule(context, REQ_WATCHDOG, ACTION_WATCHDOG, WATCHDOG_INTERVAL_MS)
    }

    /** Dipakai [LocationTrackingService.onTaskRemoved]: jeda pendek supaya sistem selesai
     *  membersihkan task sebelum service dibangunkan kembali. */
    fun scheduleImmediateRestart(context: Context) {
        schedule(context, REQ_RESTART, ACTION_RESTART, RESTART_DELAY_MS)
    }

    private fun schedule(context: Context, requestCode: Int, action: String, delayMs: Long) {
        val am = alarmManager(context) ?: return
        val at = SystemClock.elapsedRealtime() + delayMs
        val pi = pendingIntent(context, requestCode, action)
        // Alarm PERSIS dipakai bila sistem sudah mengizinkannya, karena hanya alarm persis
        // yang memberi app jendela izin untuk memulai foreground service dari latar
        // belakang di Android 12+. Kalau tidak diizinkan, alarm biasa tetap dipasang:
        // tidak sekuat itu, tapi jauh lebih baik daripada tidak mencoba sama sekali.
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && am.canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, at, pi)
                return
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Alarm persis $action ditolak sistem", e)
        }
        am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, at, pi)
    }

    fun cancelWatchdog(context: Context) {
        val am = alarmManager(context) ?: return
        am.cancel(pendingIntent(context, REQ_WATCHDOG, ACTION_WATCHDOG))
        am.cancel(pendingIntent(context, REQ_RESTART, ACTION_RESTART))
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    /** Hanya membuka layar pengaturan — pengecualian optimisasi baterai ditawarkan,
     *  tidak dipaksa, supaya tidak melanggar kebijakan Play. */
    fun openBatteryOptimizationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            Log.w(TAG, "Layar optimisasi baterai tidak tersedia", e)
        }
    }

    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.fromParts("package", context.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.w(TAG, "Layar detail aplikasi tidak tersedia", e)
        }
    }
}
