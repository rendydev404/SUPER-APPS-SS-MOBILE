package com.sukashawarma.superapp.data.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Satu-satunya jalan masuk untuk MENGHIDUPKAN ULANG pelacakan tanpa user membuka app.
 *
 * Tiga pemicu, semuanya bermuara ke hal yang sama:
 *  - `BOOT_COMPLETED` / `MY_PACKAGE_REPLACED`: proses app belum pernah hidup sejak HP
 *    menyala atau app baru di-update. Boot adalah salah satu dari sedikit konteks yang
 *    masih boleh memulai foreground service dari latar belakang di Android 12+.
 *  - Alarm watchdog (lihat [LocationTracking.scheduleWatchdog]): jaring pengaman untuk
 *    perangkat OEM (Xiaomi/Oppo/Vivo/Realme) yang membunuh service diam-diam tanpa
 *    menghormati START_STICKY.
 *  - Alarm restart setelah task disingkirkan dari Recent Apps.
 */
class LocationRestartReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        // Flag mati = user memang mematikan berbagi lokasi. Jangan pernah menghidupkannya
        // sendiri: itu pelacakan tanpa persetujuan.
        if (!LocationTrackingPrefs.isEnabled(context)) {
            LocationTracking.cancelWatchdog(context)
            return
        }
        Log.d("LocTracking", "Restart dipicu oleh ${intent?.action}")
        // Rantai alarm dilanjutkan lebih dulu: kalau start di bawah gagal (mis. ditolak
        // sistem), percobaan berikutnya tetap terjadwal dan pelacakan bisa pulih sendiri.
        LocationTracking.scheduleWatchdog(context)
        LocationTracking.start(context)
    }
}
