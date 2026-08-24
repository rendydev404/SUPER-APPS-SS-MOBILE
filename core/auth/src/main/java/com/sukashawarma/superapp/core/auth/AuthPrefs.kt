package com.sukashawarma.superapp.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Menyimpan refresh_token supaya app bisa re-auth diam-diam setelah proses dibunuh OS.
 * TIDAK menyimpan username/password (beda dari POS lama — lihat temuan X3 di plan):
 * kredensial plaintext di SharedPreferences adalah kebocoran kalau perangkat di-root/backup.
 */
object AuthPrefs {
    private const val PREFS_NAME = "auth_prefs"
    private const val KEY_REFRESH_TOKEN = "refresh_token"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        prefs = EncryptedSharedPreferences.create(
            context.applicationContext,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun setRefreshToken(token: String) {
        if (::prefs.isInitialized) prefs.edit().putString(KEY_REFRESH_TOKEN, token).apply()
    }

    fun getRefreshToken(): String? = if (::prefs.isInitialized) prefs.getString(KEY_REFRESH_TOKEN, null) else null

    fun clear() {
        if (::prefs.isInitialized) prefs.edit().remove(KEY_REFRESH_TOKEN).apply()
    }
}
