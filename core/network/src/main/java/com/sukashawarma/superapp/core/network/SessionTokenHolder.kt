package com.sukashawarma.superapp.data.remote

/** Token akses sesi aktif, dipegang di memori. Sumber kebenaran untuk interceptor REST. */
object SessionTokenHolder {
    @Volatile var accessToken: String? = null
    @Volatile var refreshToken: String? = null

    fun clear() {
        accessToken = null
        refreshToken = null
    }
}
