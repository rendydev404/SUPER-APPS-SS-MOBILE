package com.sukashawarma.superapp.data.remote


import com.sukashawarma.superapp.data.local.AuthPrefs
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Menjaga access token tetap hidup. Token Supabase kedaluwarsa ~1 jam. */

object AuthSessionManager {
    private val mutex = Mutex()

    /**
     * Memastikan sesi aktif. Token yang tersimpan di disk tidak pernah dipakai
     * oleh jalur refresh API biasa; hanya login biometrik yang memasukkannya ke
     * [SessionTokenHolder] setelah BiometricPrompt berhasil.
     */
    suspend fun ensureAuthenticated(): Boolean = ensureAuthenticatedRinci() == HasilSesi.BERHASIL

    /**
     * Sama dengan [ensureAuthenticated], tetapi membedakan dua kegagalan yang selama ini
     * terlihat identik.
     *
     * Bedanya menentukan nasib pengguna: token yang benar-benar ditolak server berarti
     * harus login ulang dengan password, sedangkan tidak ada jaringan berarti tokennya
     * mungkin masih sah dan sesi boleh dibuka dari snapshot lokal. Sebelum ini keduanya
     * sama-sama berakhir di pesan "Sesi biometrik perlu diperbarui" — menyuruh crew di
     * lokasi tanpa sinyal melakukan hal yang justru mustahil dilakukan tanpa sinyal.
     */
    suspend fun ensureAuthenticatedRinci(): HasilSesi {
        if (isUsable(SessionTokenHolder.accessToken)) return HasilSesi.BERHASIL
        return refreshRinci()
    }

    enum class HasilSesi {
        BERHASIL,

        /** Server menjawab dan menolak: token dicabut, akun dinonaktifkan, dsb. */
        DITOLAK,

        /** Server tidak terjangkau. Belum tentu tokennya tidak sah. */
        TIDAK_ADA_JARINGAN,
    }

    /**
     * Dipanggil sekali saat server menolak refresh token secara permanen. Layer app
     * memakainya untuk membuang salinan token di luar memori (antrean service lokasi),
     * supaya salinan yang sudah hangus tidak dipulihkan lagi setelah proses dibangun ulang.
     */
    @Volatile var onSesiDitolak: (() -> Unit)? = null

    suspend fun refresh(): Boolean = refreshRinci() == HasilSesi.BERHASIL

    /** Ada sesuatu yang bisa dipakai untuk bicara ke server atas nama user: access token
     *  yang masih hidup, atau refresh token untuk menerbitkannya. */
    fun punyaSesiAktif(): Boolean =
        SessionTokenHolder.refreshToken != null || isUsable(SessionTokenHolder.accessToken)

    suspend fun refreshRinci(): HasilSesi = mutex.withLock {
        if (isUsable(SessionTokenHolder.accessToken)) return@withLock HasilSesi.BERHASIL
        tukarRefreshToken()
    }

    /**
     * Refresh setelah server menjawab 401 untuk request yang membawa [tokenGagal].
     *
     * Beda dengan [refreshRinci]: token yang menurut jam perangkat masih hidup tetap
     * di-refresh, karena server baru saja menolaknya (jam HP melenceng, atau sesinya
     * dicabut). Pemeriksaan kedaluwarsa di sini justru membuat 401 tidak pernah pulih.
     * Pengecualiannya: request lain sudah me-refresh selagi request ini menunggu mutex.
     */
    suspend fun refreshSetelah401(tokenGagal: String?): HasilSesi = mutex.withLock {
        val sekarang = SessionTokenHolder.accessToken
        if (sekarang != null && sekarang != tokenGagal && isUsable(sekarang)) {
            return@withLock HasilSesi.BERHASIL
        }
        tukarRefreshToken()
    }

    /** Wajib dipanggil di dalam [mutex]. */
    private suspend fun tukarRefreshToken(): HasilSesi {
        // Hanya token sesi aktif di memory yang boleh dipakai di sini. Refresh
        // token terenkripsi di disk dimasukkan oleh login biometrik setelah
        // autentikasi perangkat berhasil. Ini mencegah service/background
        // request membuka sesi kembali setelah user logout.
        val refreshToken = SessionTokenHolder.refreshToken
            ?: return HasilSesi.DITOLAK

        return try {
            val res = authApi.refreshSession(payload = RefreshTokenPayload(refreshToken))
            val body = res.body()
            if (res.isSuccessful && body != null) {
                SessionTokenHolder.accessToken = body.access_token
                SessionTokenHolder.refreshToken = body.refresh_token
                // Token hasil refresh hanya menetap di disk untuk akun yang
                // mengaktifkan biometrik; AuthPrefs yang memutuskan.
                AuthPrefs.setRefreshTokenForUser(
                    AuthPrefs.getBiometricUserId(),
                    body.refresh_token,
                )
                HasilSesi.BERHASIL
            } else if (res.code() >= 500 || res.code() == 429) {
                // Server sedang bermasalah atau membatasi laju, bukan tokennya. Memaksa
                // login ulang di sini berarti seluruh outlet terkunci setiap kali Supabase batuk.
                HasilSesi.TIDAK_ADA_JARINGAN
            } else {
                // 400/401/403: token sudah dipakai, dicabut, atau sesinya dihapus — permanen.
                // Token dibuang dari memori supaya tidak dicoba lagi: dulu setiap request
                // yang kena 401 mengulang refresh dengan token hangus yang sama, ribuan kali
                // sehari, dan tiap percobaan tercatat GoTrue sebagai "Possible abuse attempt".
                // Hanya dibuang kalau belum diganti jalur lain selagi request berjalan.
                if (SessionTokenHolder.refreshToken == refreshToken) {
                    SessionTokenHolder.refreshToken = null
                }
                onSesiDitolak?.invoke()
                HasilSesi.DITOLAK
            }
        } catch (e: Exception) {
            e.printStackTrace()
            if (adalahGalatJaringan(e)) HasilSesi.TIDAK_ADA_JARINGAN else HasilSesi.DITOLAK
        }
    }

    fun signOut() {
        SessionTokenHolder.clear()
        AuthPrefs.clear()
    }

    private fun isUsable(token: String?): Boolean =
        token != null && expiryMillis(token) - System.currentTimeMillis() > 5 * 60_000L

    private fun expiryMillis(token: String): Long = try {
        val payload = token.split(".")[1]
        val flags = android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP
        org.json.JSONObject(String(android.util.Base64.decode(payload, flags))).getLong("exp") * 1000L
    } catch (e: Exception) {
        0L
    }
}





val authApi: AuthApi by lazy { com.sukashawarma.superapp.data.remote.SupabaseClient.retrofit.create(AuthApi::class.java) }
