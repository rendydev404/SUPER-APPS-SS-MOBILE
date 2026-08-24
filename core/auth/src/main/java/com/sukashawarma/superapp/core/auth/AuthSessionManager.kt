package com.sukashawarma.superapp.data.remote


import com.sukashawarma.superapp.data.local.AuthPrefs
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Menjaga access token tetap hidup. Token Supabase kedaluwarsa ~1 jam. */

object AuthSessionManager {
    private val mutex = Mutex()
    private var lastRefreshAt = 0L

    suspend fun ensureAuthenticated(): Boolean {
        if (isUsable(SessionTokenHolder.accessToken)) return true
        return refresh()
    }

    suspend fun refresh(): Boolean = mutex.withLock {
        if (isUsable(SessionTokenHolder.accessToken)) return@withLock true
        if (System.currentTimeMillis() - lastRefreshAt < 60_000) {
            return@withLock SessionTokenHolder.accessToken != null
        }

        val refreshToken = SessionTokenHolder.refreshToken
            ?: AuthPrefs.getRefreshToken()
            ?: return@withLock false

        try {
            lastRefreshAt = System.currentTimeMillis()
            val res = authApi.refreshSession(payload = RefreshTokenPayload(refreshToken))
            val body = res.body()
            if (res.isSuccessful && body != null) {
                SessionTokenHolder.accessToken = body.access_token
                SessionTokenHolder.refreshToken = body.refresh_token
                AuthPrefs.setRefreshToken(body.refresh_token)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
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

