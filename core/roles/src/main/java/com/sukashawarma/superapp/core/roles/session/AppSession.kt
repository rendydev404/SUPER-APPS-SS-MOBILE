package com.sukashawarma.superapp.domain.session

import com.sukashawarma.superapp.data.local.AuthPrefs
import com.sukashawarma.superapp.data.remote.AuthSessionManager
import com.sukashawarma.superapp.data.remote.SessionTokenHolder
import com.sukashawarma.superapp.data.remote.SignInPayload
import com.sukashawarma.superapp.data.remote.SupabaseClient
import com.sukashawarma.superapp.data.repository.MitraRepository
import com.sukashawarma.superapp.data.repository.StaffRepository
import com.sukashawarma.superapp.domain.model.MitraProfile
import com.sukashawarma.superapp.domain.model.Role
import com.sukashawarma.superapp.domain.model.StaffProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

sealed interface LoginResult {
    data object Success : LoginResult
    /** Kredensial salah / staff tidak ditemukan / staff non-aktif — pesan sudah user-facing. */
    data class Failure(val message: String) : LoginResult
}

/** Sesi tunggal untuk seluruh app — analog `useAuth()` context di web. */
object AppSession {
    private val _staff = MutableStateFlow<StaffProfile?>(null)
    val staff: StateFlow<StaffProfile?> = _staff

    private val _mitraProfile = MutableStateFlow<MitraProfile?>(null)
    val mitraProfile: StateFlow<MitraProfile?> = _mitraProfile

    /** true = profil GAGAL dimuat (jaringan/server), BUKAN "tidak punya profil".
     *  Dibedakan supaya mitra bersinyal jelek tak disuruh menelepon admin pusat. */
    private val _mitraLoadFailed = MutableStateFlow(false)
    val mitraLoadFailed: StateFlow<Boolean> = _mitraLoadFailed

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    /** Username tanpa '@' -> pseudo-email <username>@outlet.local — cermin ADR-008 web. */
    private fun normalizeIdentifier(identifier: String): String {
        val id = identifier.trim()
        return if (id.contains("@")) id else "$id@outlet.local"
    }

    suspend fun tryAutoLogin() {
        _loading.value = true
        val refreshToken = AuthPrefs.getRefreshToken()
        if (refreshToken == null) {
            _loading.value = false
            return
        }
        SessionTokenHolder.refreshToken = refreshToken
        val ok = AuthSessionManager.ensureAuthenticated()
        if (ok) loadStaffOrSignOut() else signOut()
        _loading.value = false
    }

    suspend fun login(identifier: String, password: String): LoginResult {
        val email = normalizeIdentifier(identifier)
        try {
            val res = com.sukashawarma.superapp.data.remote.authApi.signInWithPassword(payload = SignInPayload(email, password))
            val body = res.body()
            if (!res.isSuccessful || body == null) {
                return LoginResult.Failure("Username atau password salah.")
            }
            SessionTokenHolder.accessToken = body.access_token
            SessionTokenHolder.refreshToken = body.refresh_token
            AuthPrefs.setRefreshToken(body.refresh_token)

            return loadStaffOrSignOut()
        } catch (e: Exception) {
            android.util.Log.e("AppSession", "login() gagal", e)
            return LoginResult.Failure(networkErrorMessage(e))
        }
    }

    private suspend fun loadStaffOrSignOut(): LoginResult {
        val userId = currentUserId() ?: return signOutWith("Sesi tidak valid, silakan login ulang.")
        val staff = try {
            StaffRepository.getOutletStaff(userId)
        } catch (e: Exception) {
            android.util.Log.e("AppSession", "loadStaffOrSignOut() gagal", e)
            return signOutWith(networkErrorMessage(e))
        }
        if (staff == null) {
            return signOutWith("Akun Anda belum terhubung dengan data staff outlet. Hubungi admin/SPV.")
        }
        if (!staff.isActive) {
            val reason = if (staff.status == "on_leave") "sedang cuti" else "non-aktif"
            return signOutWith("Akun Anda berstatus $reason. Hubungi admin/SPV.")
        }
        _staff.value = staff
        loadMitraProfileIfNeeded(staff)
        return LoginResult.Success
    }

    /** Dipanggil dari KEDUA jalur masuk. Kalau hanya dipasang di login(), app terlihat
     *  benar saat login pertama lalu jadi layar kosong keesokan harinya lewat auto-login. */
    private suspend fun loadMitraProfileIfNeeded(staff: StaffProfile) {
        if (staff.role != Role.MITRA) {
            _mitraProfile.value = null
            _mitraLoadFailed.value = false
            return
        }
        try {
            _mitraProfile.value = MitraRepository.getProfile(staff.id)
            _mitraLoadFailed.value = false
        } catch (e: Exception) {
            android.util.Log.e("AppSession", "loadMitraProfileIfNeeded() gagal", e)
            _mitraProfile.value = null
            _mitraLoadFailed.value = true
        }
    }

    private fun signOutWith(message: String): LoginResult {
        signOut()
        return LoginResult.Failure(message)
    }

    fun signOut() {
        AuthSessionManager.signOut()
        _staff.value = null
        _mitraProfile.value = null
        _mitraLoadFailed.value = false
    }

    /** Sebelumnya SEMUA exception (DNS gagal, timeout, TLS, JSON tak terduga dari server, dst)
     *  dilempar ke satu pesan generik "periksa koneksi internet" — jadi begitu penyebabnya
     *  BUKAN internet (mis. jam HP salah -> TLS gagal, atau server balas format tak terduga),
     *  user diarahkan mengecek hal yang salah. Dibedakan di sini per tipe exception supaya
     *  pesannya cocok dengan yang sebenarnya terjadi (detail asli tetap di Logcat via Log.e
     *  di pemanggil, bukan diekspos ke user). */
    private fun networkErrorMessage(e: Exception): String = when (e) {
        is java.net.UnknownHostException ->
            "Tidak ada koneksi internet. Periksa jaringan Wi-Fi/data seluler Anda."
        is java.net.SocketTimeoutException ->
            "Server tidak merespons (koneksi lambat/timeout). Coba lagi."
        is javax.net.ssl.SSLException ->
            "Gagal membangun koneksi aman ke server. Pastikan tanggal & waktu perangkat Anda benar."
        is java.io.IOException ->
            "Gagal terhubung ke server. Periksa koneksi internet."
        is com.google.gson.JsonParseException ->
            "Server mengirim balasan yang tak dikenali. Coba lagi beberapa saat lagi."
        else -> "Terjadi kesalahan tak terduga. Coba lagi."
    }

    private fun currentUserId(): String? {
        val token = SessionTokenHolder.accessToken ?: return null
        return try {
            val payload = token.split(".")[1]
            val flags = android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP
            org.json.JSONObject(String(android.util.Base64.decode(payload, flags))).getString("sub")
        } catch (e: Exception) {
            null
        }
    }
}


