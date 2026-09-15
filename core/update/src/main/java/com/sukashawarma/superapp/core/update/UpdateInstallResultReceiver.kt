package com.sukashawarma.superapp.core.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Status sesi PackageInstaller. Membuka kembali aplikasi BUKAN tugas receiver ini —
 * itu ditangani [AppUpdateRelaunchReceiver] lewat MY_PACKAGE_REPLACED, supaya tidak
 * ada dua jalur yang sama-sama meluncurkan activity.
 */
class UpdateInstallResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AppUpdateManager.handleInstallStatus(context.applicationContext, intent)
    }

    companion object {
        const val ACTION_INSTALL_STATUS = "com.sukashawarma.superapp.action.UPDATE_INSTALL_STATUS"
    }
}
