package com.sukashawarma.superapp.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Penyimpanan lokal terenkripsi untuk sesi dan kredensial login yang diingat.
 * Refresh token dipakai untuk auto-login; username/password hanya dipakai untuk
 * mengisi kembali form bila sesi perlu login ulang.
 */
object AuthPrefs {
    private const val PREFS_NAME = "auth_prefs"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_LOGIN_IDENTIFIER = "login_identifier"
    private const val KEY_LOGIN_PASSWORD = "login_password"

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

    /** Menyimpan kredensial di EncryptedSharedPreferences, bukan plaintext prefs. */
    fun setLoginCredentials(identifier: String, password: String) {
        if (!::prefs.isInitialized) return
        prefs.edit()
            .putString(KEY_LOGIN_IDENTIFIER, identifier)
            .putString(KEY_LOGIN_PASSWORD, password)
            .apply()
    }

    fun getLoginIdentifier(): String? =
        if (::prefs.isInitialized) prefs.getString(KEY_LOGIN_IDENTIFIER, null) else null

    fun getLoginPassword(): String? =
        if (::prefs.isInitialized) prefs.getString(KEY_LOGIN_PASSWORD, null) else null

    /** Dipanggil setelah password berhasil diubah dari halaman profil. */
    fun updateSavedPassword(password: String) {
        if (::prefs.isInitialized) prefs.edit().putString(KEY_LOGIN_PASSWORD, password).apply()
    }

    fun clear() {
        // Kredensial yang diingat sengaja dipertahankan saat logout agar form login
        // tetap terisi pada login berikutnya. Yang dihapus hanya sesi aktif.
        if (::prefs.isInitialized) prefs.edit().remove(KEY_REFRESH_TOKEN).apply()
    }
}
