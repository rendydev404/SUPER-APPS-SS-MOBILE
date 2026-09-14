package com.sukashawarma.superapp.core.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Menerima siaran sistem Android saat APK versi baru telah selesai dipasang (ACTION_MY_PACKAGE_REPLACED)
 * atau saat alarm watchdog bangun (ACTION_RELAUNCH_ALARM).
 */
class AppUpdateRelaunchReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.i("AppUpdateRelaunch", "Broadcast diterima: $action. Menjalankan auto-relaunch...")
        if (action == Intent.ACTION_MY_PACKAGE_REPLACED || action == AppUpdateRelauncher.ACTION_RELAUNCH_ALARM) {
            AppUpdateRelauncher.relaunchApp(context)
        }
    }

    companion object {
        const val NOTIFICATION_ID = AppUpdateRelauncher.NOTIFICATION_ID
    }
}
