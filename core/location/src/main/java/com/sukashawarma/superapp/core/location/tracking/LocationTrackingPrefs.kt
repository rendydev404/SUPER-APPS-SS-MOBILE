package com.sukashawarma.superapp.data.location

import android.content.Context
import android.content.SharedPreferences

/**
 * Flag "user pernah menyalakan berbagi lokasi" + antrean titik yang belum terkirim.
 *
 * Sengaja SharedPreferences biasa, bukan EncryptedSharedPreferences seperti AuthPrefs:
 * isinya bukan kredensial, dan service harus bisa membacanya saat proses baru dibangunkan
 * sistem — enkripsi di sini hanya menambah biaya start tanpa melindungi apa pun yang rahasia.
 */
object LocationTrackingPrefs {
    private const val PREFS_NAME = "location_tracking_prefs"
    private const val KEY_ENABLED = "sharing_enabled"
    private const val KEY_QUEUE = "pending_queue"

    /** Antrean dibatasi agar perangkat yang offline berhari-hari tidak menumpuk prefs
     *  sampai ratusan KB; titik terlama yang dibuang duluan karena paling tidak berguna. */
    private const val MAX_QUEUE = 500

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun require(context: Context): SharedPreferences {
        init(context)
        return prefs!!
    }

    fun isEnabled(context: Context): Boolean = require(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        require(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun loadQueue(context: Context): List<String> {
        val raw = require(context).getString(KEY_QUEUE, null) ?: return emptyList()
        return raw.split('\n').filter { it.isNotBlank() }
    }

    fun saveQueue(context: Context, lines: List<String>) {
        val trimmed = if (lines.size > MAX_QUEUE) lines.takeLast(MAX_QUEUE) else lines
        require(context).edit().putString(KEY_QUEUE, trimmed.joinToString("\n")).apply()
    }

    fun clearQueue(context: Context) {
        require(context).edit().remove(KEY_QUEUE).apply()
    }
}
