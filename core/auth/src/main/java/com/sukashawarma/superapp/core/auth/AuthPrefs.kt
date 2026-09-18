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

    private const val KEY_SESI_USER_ID = "sesi_user_id"
    private const val KEY_SESI_STAFF_JSON = "sesi_staff_json"
    private const val KEY_SESI_MITRA_JSON = "sesi_mitra_json"
    private const val KEY_SESI_TERAKHIR_ONLINE_MS = "sesi_terakhir_online_ms"

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

    private const val KEY_BIOMETRIC_GUIDELINE_DISMISSED_PREFIX = "biometric_guideline_dismissed_"

    fun isBiometricGuidelineDismissed(userId: String?): Boolean {
        if (!::prefs.isInitialized || userId.isNullOrBlank()) return false
        return prefs.getBoolean(KEY_BIOMETRIC_GUIDELINE_DISMISSED_PREFIX + userId, false)
    }

    fun setBiometricGuidelineDismissed(userId: String?, dismissed: Boolean = true) {
        if (!::prefs.isInitialized || userId.isNullOrBlank()) return
        prefs.edit()
            .putBoolean(KEY_BIOMETRIC_GUIDELINE_DISMISSED_PREFIX + userId, dismissed)
            .apply()
    }

    /**
     * Salinan profil sesi terakhir yang berhasil dimuat dari server, dipakai membuka
     * aplikasi saat internet mati.
     *
     * Profilnya disimpan sebagai JSON mentah supaya modul ini tidak perlu mengenal
     * StaffProfile maupun MitraProfile — keduanya milik `core:roles`, yang justru
     * bergantung pada modul ini.
     */
    data class SnapshotSesi(
        val userId: String,
        val staffJson: String,
        val mitraJson: String?,
        val terakhirOnlineMs: Long,
    )

    /**
     * Dipanggil setiap kali profil berhasil dimuat dari server, bukan hanya saat login.
     * [terakhirOnlineMs] itulah yang membatasi berapa lama perangkat boleh dipakai tanpa
     * pernah menyentuh server sama sekali.
     */
    fun simpanSnapshotSesi(userId: String, staffJson: String, mitraJson: String?) {
        if (!::prefs.isInitialized) return
        prefs.edit()
            .putString(KEY_SESI_USER_ID, userId)
            .putString(KEY_SESI_STAFF_JSON, staffJson)
            .putString(KEY_SESI_MITRA_JSON, mitraJson)
            .putLong(KEY_SESI_TERAKHIR_ONLINE_MS, System.currentTimeMillis())
            .apply()
    }

    fun snapshotSesi(): SnapshotSesi? {
        if (!::prefs.isInitialized) return null
        val userId = prefs.getString(KEY_SESI_USER_ID, null) ?: return null
        val staffJson = prefs.getString(KEY_SESI_STAFF_JSON, null) ?: return null
        return SnapshotSesi(
            userId = userId,
            staffJson = staffJson,
            mitraJson = prefs.getString(KEY_SESI_MITRA_JSON, null),
            terakhirOnlineMs = prefs.getLong(KEY_SESI_TERAKHIR_ONLINE_MS, 0L),
        )
    }

    fun hapusSnapshotSesi() {
        if (!::prefs.isInitialized) return
        prefs.edit()
            .remove(KEY_SESI_USER_ID)
            .remove(KEY_SESI_STAFF_JSON)
            .remove(KEY_SESI_MITRA_JSON)
            .remove(KEY_SESI_TERAKHIR_ONLINE_MS)
            .apply()
    }

    fun enableBiometric(userId: String, refreshToken: String) {
        if (!::prefs.isInitialized) return
        prefs.edit()
            .putBoolean(KEY_BIOMETRIC_ENABLED, true)
            .putString(KEY_BIOMETRIC_USER_ID, userId)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putBoolean(KEY_BIOMETRIC_GUIDELINE_DISMISSED_PREFIX + userId, true)
            .apply()
    }

    /**
     * Snapshot sesi ikut dibuang: tanpa biometrik tidak ada lagi gerbang yang bisa
     * memverifikasi pemilik perangkat saat offline, jadi menyimpannya hanya menyisakan
     * profil staf di disk tanpa ada yang bisa memakainya.
     */
    fun disableBiometric() {
        if (!::prefs.isInitialized) return
        prefs.edit()
            .remove(KEY_BIOMETRIC_ENABLED)
            .remove(KEY_BIOMETRIC_USER_ID)
            .remove(KEY_REFRESH_TOKEN)
            .apply()
        hapusSnapshotSesi()
    }

    /**
     * Dipanggil saat sesi berakhir. Refresh token DIPERTAHANKAN bila biometrik
     * aktif — itulah yang membuat buka-dengan-sidik-jari masih bisa dipakai
     * setelah aplikasi ditutup. Tanpa biometrik, token dibuang.
     *
     * Snapshot sesi selalu ikut dibuang, bahkan saat biometrik aktif: keluar dari akun
     * berarti mengakhiri sesi, dan masuk kembali harus melewati server sekali. Kalau
     * snapshot dibiarkan, logout jadi tidak berarti apa-apa selama perangkat offline.
     */
    fun clear() {
        if (!::prefs.isInitialized) return
        hapusSnapshotSesi()
        if (isBiometricEnabled()) return
        prefs.edit().remove(KEY_REFRESH_TOKEN).apply()
    }
}
