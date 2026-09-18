package com.sukashawarma.superapp.notif

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * BroadcastReceiver untuk menangani aksi dari notifikasi Petty Cash
 * (misalnya tombol "Heningkan" atau "Tutup" pada banner panggilan masuk).
 */
class PettyCashActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        Log.i(TAG, "PettyCashActionReceiver menerima aksi: $action")
        if (action == ACTION_STOP_ALARM || action == Intent.ACTION_DELETE) {
            PettyCashAlarmManager.hentikan(context)
        }
    }

    companion object {
        private const val TAG = "PettyCashActionReceiver"
        const val ACTION_STOP_ALARM = "com.sukashawarma.superapp.ACTION_STOP_PETTY_CASH_ALARM"
    }
}
