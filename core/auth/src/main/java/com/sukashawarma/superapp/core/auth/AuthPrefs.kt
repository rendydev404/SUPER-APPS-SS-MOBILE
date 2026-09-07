package com.sukashawarma.superapp.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Penyimpanan lokal terenkripsi untuk sesi dan kunci biometrik.
 *
 * Refresh token hanya disimpan ke disk kalau pengguna mengaktifkan buka-dengan-sidik-jari.
 * Password tidak pernah lagi ditulis ke perangkat: kredensial yang dulu diingat
 * (`login_identifier` / `login_password`) dihapus saat init sebagai migrasi.
 */
object AuthPrefs {
    private const val PREFS_NAME = "auth_prefs"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    private const val KEY_BIOMETRIC_USER_ID = "biometric_user_id"
    private const val KEY_LAST_ACTIVE_USER_ID = "last_active_user_id"

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
        // Migrasi: kredensial login yang dulu disimpan tidak dipakai lagi dan
        // dibuang dari perangkat yang sudah terlanjur menyimpannya.
        prefs.edit()
            .remove("login_identifier")
            .remove("login_password")
            .apply()
    }

    /**
     * Menyimpan refresh token HANYA bila pengguna itu mengaktifkan biometrik.
     * Tanpa biometrik, sesi tidak dipertahankan di disk sama sekali.
     */
    fun setRefreshTokenForUser(userId: String?, token: String) {
        if (!::prefs.isInitialized) return
        if (!isBiometricEnabledFor(userId)) return
        prefs.edit().putString(KEY_REFRESH_TOKEN, token).apply()
    }

    fun getRefreshToken(): String? =
        if (::prefs.isInitialized) prefs.getString(KEY_REFRESH_TOKEN, null) else null

    fun isBiometricEnabled(): Boolean =
        if (::prefs.isInitialized) prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false) else false

    fun getBiometricUserId(): String? =
        if (::prefs.isInitialized) prefs.getString(KEY_BIOMETRIC_USER_ID, null) else null

    /** Biometrik terikat pada satu staff: kunci milik orang lain tidak boleh
     *  membuka sesi staff yang sedang tersimpan. */
    fun isBiometricEnabledFor(userId: String?): Boolean =
        !userId.isNullOrBlank() && isBiometricEnabled() && getBiometricUserId() == userId

    fun setLastActiveUserId(userId: String) {
        if (!::prefs.isInitialized) return
        prefs.edit().putString(KEY_LAST_ACTIVE_USER_ID, userId).apply()
    }

    /**
     * Staff terakhir yang login di perangkat ini, dan tetap ada setelah logout.
     *
     * Ini penanda "perangkat kerja milik staff", bukan "sedang login" — sesi
     * sendiri tidak disimpan ke disk kecuali biometrik aktif. Dipakai pengingat
     * absen harian untuk membedakan ponsel staff dari perangkat yang belum
     * pernah dipakai sama sekali.
     */
    fun getLastActiveUserId(): String? =
        if (::prefs.isInitialized) prefs.getString(KEY_LAST_ACTIVE_USER_ID, null) else null

    /** Dipakai layar login untuk memutuskan apakah tombol sidik jari ditawarkan
     *  sebelum ada sesi aktif. */
    fun isBiometricEnabledForLastActiveUser(): Boolean {
        val userId = if (::prefs.isInitialized) {
            prefs.getString(KEY_LAST_ACTIVE_USER_ID, null)
        } else {
            null
        }
        return isBiometricEnabledFor(userId)
    }

    fun enableBiometric(userId: String, refreshToken: String) {
        if (!::prefs.isInitialized) return
        prefs.edit()
            .putBoolean(KEY_BIOMETRIC_ENABLED, true)
            .putString(KEY_BIOMETRIC_USER_ID, userId)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .apply()
    }

    fun disableBiometric() {
        if (!::prefs.isInitialized) return
        prefs.edit()
            .remove(KEY_BIOMETRIC_ENABLED)
            .remove(KEY_BIOMETRIC_USER_ID)
            .remove(KEY_REFRESH_TOKEN)
            .apply()
    }

    /**
     * Dipanggil saat sesi berakhir. Refresh token DIPERTAHANKAN bila biometrik
     * aktif — itulah yang membuat buka-dengan-sidik-jari masih bisa dipakai
     * setelah aplikasi ditutup. Tanpa biometrik, token dibuang.
     */
    fun clear() {
        if (!::prefs.isInitialized) return
        if (isBiometricEnabled()) return
        prefs.edit().remove(KEY_REFRESH_TOKEN).apply()
    }
}
