package com.sukashawarma.superapp.core.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat

/**
 * Membuka kembali Super App begitu APK baru selesai terpasang.
 *
 * Proses lama ikut mati saat dirinya sendiri di-update, jadi pemicunya adalah
 * `MY_PACKAGE_REPLACED` yang diterima proses versi BARU. Masalahnya, sejak Android 10
 * receiver di latar belakang tidak boleh membuka activity. Pengecualian yang bisa
 * dipakai aplikasi distribusi mandiri hanya izin "Tampil di atas aplikasi lain"
 * (SYSTEM_ALERT_WINDOW) — dan mulai Android 15 izin itu baru berlaku bila aplikasi
 * punya jendela overlay yang sedang tampil. Karena itu, sebelum membuka activity,
 * dipasang jendela overlay 1x1 transparan yang dilepas lagi begitu layar utama tampil.
 *
 * Tanpa izin overlay, activity tetap dicoba dibuka (lolos di Android 9 ke bawah dan
 * beberapa ROM), lalu bila layar utama belum juga tampil, dipasang notifikasi
 * "ketuk untuk membuka" sebagai jaring pengaman.
 *
 * Relaunch hanya terjadi bila update dipicu updater ini SAAT aplikasi sedang dilihat
 * user — update di latar belakang tidak boleh tiba-tiba menyerobot layar.
 */
object AppUpdateRelauncher {

    private const val TAG = "AppUpdateRelauncher"
    private const val PREFS_NAME = "superapp_update_relaunch"
    private const val KEY_PENDING_AT = "relaunch_pending_at"
    private const val KEY_OVERLAY_OFFERED_FOR = "overlay_offered_for_version"
    private const val PENDING_TTL_MS = 15 * 60_000L

    /** Jeda agar jendela overlay benar-benar tampil sebelum activity dibuka. */
    private const val OVERLAY_SETTLE_MS = 200L
    private const val RETRY_AFTER_MS = 1_000L
    private const val FALLBACK_AFTER_MS = 3_000L

    const val NOTIFICATION_ID = 99125
    private const val CHANNEL_ID = "app_update_relaunch_channel"

    @Volatile private var appVisible = false
    private var overlayView: View? = null

    fun onAppVisible(context: Context) {
        appVisible = true
        removeOverlay(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIFICATION_ID)
    }

    fun onAppHidden() {
        appVisible = false
    }

    /** Dipanggil tepat sebelum instalasi diserahkan ke sistem. */
    fun markPending(context: Context) {
        if (!appVisible) return
        // commit(), bukan apply(): proses bisa dibunuh installer sebelum apply() sempat menulis.
        prefs(context).edit().putLong(KEY_PENDING_AT, System.currentTimeMillis()).commit()
    }

    fun clearPending(context: Context) {
        prefs(context).edit().remove(KEY_PENDING_AT).apply()
    }

    fun onPackageReplaced(context: Context, pendingResult: BroadcastReceiver.PendingResult) {
        val appCtx = context.applicationContext
        val pendingAt = prefs(appCtx).getLong(KEY_PENDING_AT, 0L)
        clearPending(appCtx)
        if (System.currentTimeMillis() - pendingAt !in 0..PENDING_TTL_MS) {
            pendingResult.finish()
            return
        }

        val handler = Handler(Looper.getMainLooper())
        val overlayShown = showOverlay(appCtx)
        Log.i(TAG, "Update terpasang, membuka aplikasi (overlay=$overlayShown)")

        handler.postDelayed({
            startMainActivity(appCtx, clearTask = true)
        }, if (overlayShown) OVERLAY_SETTLE_MS else 0L)

        // Perangkat lambat kadang belum menganggap overlay tampil pada percobaan pertama.
        handler.postDelayed({
            if (!appVisible) startMainActivity(appCtx, clearTask = false)
        }, RETRY_AFTER_MS)

        handler.postDelayed({
            removeOverlay(appCtx)
            if (!appVisible) postFallbackNotification(appCtx)
            pendingResult.finish()
        }, FALLBACK_AFTER_MS)
    }

    /** Izin overlay perlu ditawarkan sekali per versi update, hanya di Android 10+. */
    fun shouldOfferOverlayPermission(context: Context, forVersionCode: Int): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            !Settings.canDrawOverlays(context) &&
            prefs(context).getInt(KEY_OVERLAY_OFFERED_FOR, 0) != forVersionCode

    fun markOverlayOffered(context: Context, forVersionCode: Int) {
        prefs(context).edit().putInt(KEY_OVERLAY_OFFERED_FOR, forVersionCode).apply()
    }

    fun overlayPermissionIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))

    private fun startMainActivity(context: Context, clearTask: Boolean) {
        val intent = launchIntent(context) ?: return
        if (clearTask) intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        runCatching { context.startActivity(intent) }
            .onFailure { Log.w(TAG, "Gagal membuka activity: ${it.message}") }
    }

    private fun launchIntent(context: Context): Intent? =
        context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        }

    private fun showOverlay(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || !Settings.canDrawOverlays(context)) return false
        return runCatching {
            val view = View(context)
            val params = WindowManager.LayoutParams(
                1,
                1,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT,
            ).apply { gravity = Gravity.TOP or Gravity.START }
            context.getSystemService(WindowManager::class.java).addView(view, params)
            overlayView = view
            true
        }.getOrElse {
            Log.w(TAG, "Gagal memasang overlay: ${it.message}")
            false
        }
    }

    private fun removeOverlay(context: Context) {
        val view = overlayView ?: return
        overlayView = null
        runCatching {
            context.applicationContext.getSystemService(WindowManager::class.java).removeViewImmediate(view)
        }
    }

    private fun postFallbackNotification(context: Context) {
        val intent = launchIntent(context) ?: return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Pembaruan Selesai", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Membuka kembali aplikasi setelah update berhasil"
                    setSound(null, null)
                    enableVibration(false)
                }
            )
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("SUKA Superapp sudah diperbarui")
            .setContentText("Ketuk untuk membuka kembali aplikasi")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true)
            .setAutoCancel(true)
            .setSilent(true)
            .build()
        // Tanpa izin POST_NOTIFICATIONS, notify() melempar SecurityException di beberapa ROM.
        runCatching { nm.notify(NOTIFICATION_ID, notification) }
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
