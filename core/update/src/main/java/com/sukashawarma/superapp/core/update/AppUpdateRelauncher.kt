package com.sukashawarma.superapp.core.update

import android.app.ActivityOptions
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat

/**
 * Komponen sentral untuk meluncurkan kembali Super App secara otomatis setelah update APK selesai.
 * Menggabungkan 4 jalur redundan untuk menembus batasan Background Activity Launch (BAL) Android 10-15:
 * 1. pendingIntent.send() dengan ActivityOptions BAL privilege (Android 13/14+)
 * 2. direct context.startActivity() dengan FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK
 * 3. High-Priority FullScreenIntent Heads-Up Notification (resmi OS Android untuk wake-up foreground)
 * 4. AlarmManager watchdog untuk mengulang percobaan jika proses sempat ditidurkan oleh sistem
 */
object AppUpdateRelauncher {

    private const val TAG = "AppUpdateRelauncher"
    const val NOTIFICATION_ID = 99125
    const val CHANNEL_ID = "app_update_relaunch_channel"
    const val ACTION_RELAUNCH_ALARM = "com.sukashawarma.superapp.action.RELAUNCH_WATCHDOG"

    fun relaunchApp(context: Context) {
        val appCtx = context.applicationContext
        Log.i(TAG, "Memulai proses auto-relaunch Superapp...")

        val pm = appCtx.packageManager
        val launchIntent = (pm.getLaunchIntentForPackage(appCtx.packageName) ?: Intent()).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            component = pm.getLaunchIntentForPackage(appCtx.packageName)?.component
                ?: ComponentName(appCtx, "com.sukashawarma.superapp.presentation.MainActivity")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK or
                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        }

        val optionsBundle = createBalOptionsBundle()

        val pendingIntent = if (Build.VERSION.SDK_INT >= 34 && optionsBundle != null) {
            PendingIntent.getActivity(
                appCtx,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                optionsBundle
            )
        } else {
            PendingIntent.getActivity(
                appCtx,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )
        }

        // Jalur 1: PendingIntent.send dengan SENDER options (BAL exemption Android 14+)
        try {
            if (optionsBundle != null) {
                pendingIntent.send(appCtx, 0, null, null, null, null, optionsBundle)
            } else {
                pendingIntent.send()
            }
            Log.i(TAG, "Jalur 1 (pendingIntent.send) dieksekusi")
        } catch (e: Throwable) {
            Log.w(TAG, "Jalur 1 gagal: ${e.message}")
        }

        // Jalur 2: Direct context.startActivity dengan ActivityOptions
        try {
            if (optionsBundle != null) {
                appCtx.startActivity(launchIntent, optionsBundle)
            } else {
                appCtx.startActivity(launchIntent)
            }
            Log.i(TAG, "Jalur 2 (startActivity) dieksekusi")
        } catch (e: Throwable) {
            Log.w(TAG, "Jalur 2 gagal: ${e.message}")
        }

        // Jalur 3: High-Priority FullScreenIntent Heads-Up Notification
        try {
            val notificationManager = appCtx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Pembaruan Selesai",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Membuka kembali aplikasi setelah update berhasil"
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                    enableLights(true)
                    enableVibration(false)
                    setSound(null, null)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val notification = NotificationCompat.Builder(appCtx, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("SUKA Superapp")
                .setContentText("Pembaruan selesai dipasang. Membuka aplikasi...")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setContentIntent(pendingIntent)
                .setFullScreenIntent(pendingIntent, true)
                .setAutoCancel(true)
                .setSilent(true)
                .build()

            notificationManager.notify(NOTIFICATION_ID, notification)
            Log.i(TAG, "Jalur 3 (FullScreenIntent notification) diposting")
        } catch (e: Throwable) {
            Log.w(TAG, "Jalur 3 gagal: ${e.message}")
        }
    }

    fun scheduleWatchdog(context: Context) {
        val appCtx = context.applicationContext
        try {
            val alarmIntent = Intent(appCtx, AppUpdateRelaunchReceiver::class.java).apply {
                action = ACTION_RELAUNCH_ALARM
            }
            val pendingAlarm = PendingIntent.getBroadcast(
                appCtx,
                99126,
                alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )
            val alarmManager = appCtx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val triggerAt = System.currentTimeMillis() + 1500L

            val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else true

            if (canExact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingAlarm)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingAlarm)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingAlarm)
            }
            Log.i(TAG, "Watchdog alarm dijadwalkan (+1500ms)")
        } catch (e: Throwable) {
            Log.w(TAG, "Gagal jadwalkan watchdog alarm: ${e.message}")
        }
    }

    private fun createBalOptionsBundle(): android.os.Bundle? {
        return try {
            val options = ActivityOptions.makeBasic()
            // Mode 1: MODE_BACKGROUND_ACTIVITY_START_ALLOWED
            try {
                val m1 = options.javaClass.getMethod("setPendingIntentBackgroundActivityStartMode", Int::class.javaPrimitiveType)
                m1.invoke(options, 1)
            } catch (_: Throwable) {}
            try {
                val m2 = options.javaClass.getMethod("setPendingIntentCreatorBackgroundActivityStartMode", Int::class.javaPrimitiveType)
                m2.invoke(options, 1)
            } catch (_: Throwable) {}
            try {
                val m3 = options.javaClass.getMethod("setPendingIntentBackgroundActivityLaunchAllowed", Boolean::class.javaPrimitiveType)
                m3.invoke(options, true)
            } catch (_: Throwable) {}
            options.toBundle()
        } catch (_: Throwable) {
            null
        }
    }
}
