package com.sukashawarma.superapp.feature.distribusi.domain

import com.sukashawarma.superapp.data.remote.Postgrest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DistribusiErrorTest {

    @Test
    fun `403 jadi pesan akses outlet`() {
        val e = Postgrest.PostgrestException(403, """{"message":"permission denied"}""")
        assertEquals(
            "Anda tidak punya akses ke outlet ini. Hubungi atasan bila ini keliru.",
            distribusiErrorMessage(e),
        )
    }

    @Test
    fun `401 jadi pesan sesi berakhir`() {
        val e = Postgrest.PostgrestException(401, "JWT expired")
        assertEquals("Sesi Anda berakhir. Silakan masuk kembali.", distribusiErrorMessage(e))
    }

    /** Pesan RPC sudah berbahasa Indonesia dan sudah ditujukan ke pengguna —
     *  meneruskannya apa adanya lebih berguna daripada pesan generik. */
    @Test
    fun `pesan dari RPC diteruskan apa adanya`() {
        val e = Postgrest.PostgrestException(
            400,
            """{"code":"P0001","message":"Supir sudah menandatangani penerimaan"}""",
        )
        assertEquals("Supir sudah menandatangani penerimaan", distribusiErrorMessage(e))
    }

    @Test
    fun `body tanpa message jatuh ke pesan generik server`() {
        val e = Postgrest.PostgrestException(500, "Internal Server Error")
        assertEquals(
            "Server sedang bermasalah. Coba lagi beberapa saat lagi.",
            distribusiErrorMessage(e),
        )
    }

    @Test
    fun `tidak ada koneksi`() {
        assertEquals(
            "Tidak ada koneksi internet. Periksa jaringan Wi-Fi atau data seluler Anda.",
            distribusiErrorMessage(java.net.UnknownHostException("supabase.co")),
        )
    }

    @Test
    fun `timeout`() {
        assertEquals(
            "Server tidak merespons. Coba lagi.",
            distribusiErrorMessage(java.net.SocketTimeoutException()),
        )
    }

    @Test
    fun `exception lain tetap menghasilkan kalimat yang bisa dibaca`() {
        val pesan = distribusiErrorMessage(IllegalStateException("boom"))
        assertEquals("Terjadi kesalahan tak terduga. Coba lagi.", pesan)
        assertTrue(pesan.endsWith("."))
    }
}
