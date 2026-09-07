package com.sukashawarma.superapp.feature.absensi.notif

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Menerima alarm 12:10, lalu menjadwalkan tayangan besok.
 *
 * Alarm tidak selamat dari perangkat yang dimatikan maupun dari pembaruan APK,
 * jadi receiver ini juga mendengar BOOT_COMPLETED dan MY_PACKAGE_REPLACED. Tanpa
 * itu, pengingat berhenti diam-diam pada perangkat yang dimatikan tiap malam —
 * dan diam-diam adalah bagian terburuknya, karena tidak ada yang melapor.
 */
class AbsenReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Boot dan pembaruan hanya memulihkan jadwal; keduanya bukan waktu tayang.
        if (intent.action == null) AbsenReminder.show(context)
        AbsenReminder.schedule(context)
    }
}
