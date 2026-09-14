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

    private const val KEY_STAFF_ID = "tracking_staff_id"
    private const val KEY_OUTLET_ID = "tracking_outlet_id"
    private const val KEY_ACCESS_TOKEN = "tracking_access_token"
    private const val KEY_REFRESH_TOKEN = "tracking_refresh_token"

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

    fun saveSession(context: Context, staffId: String, outletId: String?, accessToken: String?, refreshToken: String?) {
        val editor = require(context).edit()
            .putString(KEY_STAFF_ID, staffId)
            .putString(KEY_OUTLET_ID, outletId)
        if (accessToken != null) editor.putString(KEY_ACCESS_TOKEN, accessToken)
        if (refreshToken != null) editor.putString(KEY_REFRESH_TOKEN, refreshToken)
        editor.apply()
    }

    fun getStaffId(context: Context): String? = require(context).getString(KEY_STAFF_ID, null)
    fun getOutletId(context: Context): String? = require(context).getString(KEY_OUTLET_ID, null)
    fun getAccessToken(context: Context): String? = require(context).getString(KEY_ACCESS_TOKEN, null)
    fun getRefreshToken(context: Context): String? = require(context).getString(KEY_REFRESH_TOKEN, null)

    fun updateTokens(context: Context, accessToken: String?, refreshToken: String?) {
        val editor = require(context).edit()
        if (accessToken != null) editor.putString(KEY_ACCESS_TOKEN, accessToken)
        if (refreshToken != null) editor.putString(KEY_REFRESH_TOKEN, refreshToken)
        editor.apply()
    }

    fun clearSession(context: Context) {
        require(context).edit()
            .remove(KEY_STAFF_ID)
            .remove(KEY_OUTLET_ID)
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .apply()
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
