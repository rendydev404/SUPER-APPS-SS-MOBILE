package com.sukashawarma.superapp.core.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller

class UpdateInstallResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        if (status == PackageInstaller.STATUS_SUCCESS) {
            AppUpdateRelauncher.relaunchApp(context)
            AppUpdateManager.onUpdateSuccessfullyApplied()
        } else {
            AppUpdateManager.handleInstallStatus(context.applicationContext, intent)
        }
    }

    companion object {
        const val ACTION_INSTALL_STATUS = "com.sukashawarma.superapp.action.UPDATE_INSTALL_STATUS"
    }
}
