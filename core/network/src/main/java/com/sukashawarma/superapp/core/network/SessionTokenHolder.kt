package com.sukashawarma.superapp.data.remote

/** Token akses sesi aktif, dipegang di memori. Sumber kebenaran untuk interceptor REST. */
object SessionTokenHolder {
    @Volatile var accessToken: String? = null

    /**
     * Refresh token Supabase sekali pakai: setiap refresh menerbitkan pasangan baru dan
     * yang lama hangus. Salinan di luar memori (antrean service lokasi) yang tertinggal
     * satu rotasi saja sudah cukup untuk celaka — memakainya ulang membuat GoTrue
     * mencatat "Possible abuse attempt" lalu mencabut SELURUH sesi pemiliknya.
     *
     * Karena itu setiap token baru, dari jalur mana pun (login, refresh, biometrik),
     * diteruskan ke [onRefreshTokenBerubah]. Access token harus sudah diisi lebih dulu
     * supaya pasangan yang diteruskan konsisten.
     */
    @Volatile var refreshToken: String? = null
        set(value) {
            field = value
            if (value != null) onRefreshTokenBerubah?.invoke(accessToken, value)
        }

    /** Dipasang dari layer app; core:network tidak mengenal modul penyimpan salinannya. */
    @Volatile var onRefreshTokenBerubah: ((accessToken: String?, refreshToken: String) -> Unit)? = null

    fun clear() {
        accessToken = null
        refreshToken = null
    }
}
