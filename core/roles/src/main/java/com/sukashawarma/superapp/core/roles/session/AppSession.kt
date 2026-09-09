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

    /** Mulai false: tidak ada lagi auto-login yang perlu ditunggu sebelum UI
     *  tampil. Sesi dibuka lewat gerbang biometrik atau password di layar login. */
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    /** true selama retryLoadMitraProfile() sedang berjalan — dipakai UI untuk menonaktifkan
     *  tombol "Coba Lagi" dan mencegah dua permintaan retry tumpang tindih (lihat komentar
     *  di retryLoadMitraProfile()). */
    private val _mitraRetrying = MutableStateFlow(false)
    val mitraRetrying: StateFlow<Boolean> = _mitraRetrying

    /** Hook untuk kerja yang harus berhenti saat sesi habis (mis. foreground service
     *  pelacakan lokasi). Dipasang dari layer app: core:roles tidak boleh mengenal modul
     *  yang bergantung padanya. */
    var onSignOut: (() -> Unit)? = null

    /** Username tanpa '@' -> pseudo-email <username>@outlet.local — cermin ADR-008 web. */
    private fun normalizeIdentifier(identifier: String): String {
        val id = identifier.trim()
        return if (id.contains("@")) id else "$id@outlet.local"
    }

    /**
     * Membuka sesi memakai refresh token tersimpan, setelah sidik jari cocok.
     *
     * Token diberikan pemanggil — layar login membacanya dari AuthPrefs hanya
     * setelah BiometricPrompt berhasil — jadi tidak ada jalur yang bisa memakai
    * token tersimpan tanpa melewati prompt biometrik lebih dulu.
     */
    suspend fun loginWithBiometric(refreshToken: String): LoginResult {
        // Jangan gunakan access token yang mungkin tertinggal dari akun lain
        // ketika proses aplikasi masih hidup. Jalur ini harus membangun sesi
        // ulang hanya dari refresh token milik binding biometrik akun ini.
        SessionTokenHolder.clear()
        SessionTokenHolder.refreshToken = refreshToken
        val ok = AuthSessionManager.ensureAuthenticated()
        if (!ok) {
            return LoginResult.Failure(
                "Sesi biometrik perlu diperbarui. Silakan login dengan password; " +
                    "pengaturan sidik jari tetap tersimpan."
            )
        }
        // Sesi yang terbuka harus milik akun yang sidik jarinya didaftarkan.
        // Tanpa pemeriksaan ini, token sisa milik akun lain bisa membuka sesi
        // yang salah di perangkat yang dipakai bergantian.
        if (AuthPrefs.getBiometricUserId() != currentUserId()) {
            signOut()
            return LoginResult.Failure("Credential biometrik tidak cocok dengan akun ini.")
        }
        return loadStaffOrSignOut()
    }

    /** Refresh token hanya menetap di disk untuk akun yang mengaktifkan biometrik. */
    private fun persistBiometricTokenAfterPasswordLogin(userId: String?, refreshToken: String) {
        AuthPrefs.setRefreshTokenForUser(userId, refreshToken)
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
            val result = loadStaffOrSignOut()
            if (result is LoginResult.Success) {
                persistBiometricTokenAfterPasswordLogin(currentUserId(), body.refresh_token)
            }
            return result
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
        // Dipakai layar login untuk tahu apakah tombol sidik jari boleh
        // ditawarkan sebelum ada sesi aktif.
        AuthPrefs.setLastActiveUserId(staff.id)
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
            val profile = MitraRepository.getProfile(staff.id)
            // Kalau sesi sudah berubah (sign-out, atau user lain login) selagi request ini
            // masih di jalan, buang hasilnya — jangan tulis balik ke AppSession yang sudah
            // bukan milik staff ini (lihat komentar retryLoadMitraProfile()).
            if (_staff.value?.id != staff.id) return
            _mitraProfile.value = profile
            _mitraLoadFailed.value = false
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Recreate activity (ganti tema/locale/ukuran font) membatalkan LaunchedEffect ini
            // di tengah jalan — itu BUKAN kegagalan jaringan, jangan tampilkan layar galat.
            throw e
        } catch (e: Exception) {
            android.util.Log.e("AppSession", "loadMitraProfileIfNeeded() gagal", e)
            if (_staff.value?.id != staff.id) return
            _mitraProfile.value = null
            _mitraLoadFailed.value = true
        }
    }

    /** Retry khusus layar galat mitra: HANYA memuat ulang profil, tak menyentuh sesi staff.
     *  Memakai jalur login penuh di sini akan men-sign-out mitra begitu jaringan masih mati —
     *  kebalikan dari maksud desainnya (sinyal jelek tidak boleh menghukum pengguna).
     *
     *  Guard in-flight: tanpa ini, tap ganda pada link goyah bisa membuat request B (sukses)
     *  ditimpa request A (gagal, datang belakangan) sehingga user dilempar balik ke layar
     *  galat. `_mitraRetrying` juga memberi sinyal visual di tombol supaya tap kedua saat
     *  gagal lagi tidak terlihat seperti tombol mati (MutableStateFlow meng-conflate nilai
     *  sama, jadi _mitraLoadFailed=true->true tak pernah memicu recomposition). */
    suspend fun retryLoadMitraProfile() {
        if (_mitraRetrying.value) return
        val current = _staff.value ?: return
        _mitraRetrying.value = true
        try {
            loadMitraProfileIfNeeded(current)
        } finally {
            _mitraRetrying.value = false
        }
    }

    /**
     * Memuat ulang baris staff milik sesi yang sedang berjalan.
     *
     * Dipakai setelah profil disunting sendiri (nama tampilan/username tampilan/
     * foto), supaya seluruh layar yang membaca [staff] ikut berubah tanpa perlu
     * login ulang. Sengaja TIDAK memakai jalur login: kegagalan jaringan di sini
     * hanya berarti "layar belum ikut berubah", bukan alasan menutup sesi.
     *
     * Hasil dibuang bila sesi sudah berpindah ke akun lain selagi request berjalan
     * — alasan yang sama dengan loadMitraProfileIfNeeded().
     */
    suspend fun refreshStaff() {
        val current = _staff.value ?: return
        try {
            val segar = StaffRepository.getOutletStaff(current.id) ?: return
            if (_staff.value?.id != current.id) return
            _staff.value = segar
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("AppSession", "refreshStaff() gagal", e)
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

