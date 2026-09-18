package com.sukashawarma.superapp.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Klasifikasi ini yang memutuskan apakah sebuah kegagalan boleh jatuh ke cache dan apakah
 * sebuah aksi boleh menunggu di antrean. Salah di sini berarti penolakan server disembunyikan
 * di balik data lama, atau kerja crew dibuang padahal cuma sinyalnya putus — dua-duanya baru
 * ketahuan saat sudah terjadi di lapangan.
 */
class PesanJaringanTest {

    @Test
    fun `putus jaringan dihitung sebagai galat jaringan`() {
        assertTrue(adalahGalatJaringan(UnknownHostException("tidak ada DNS")))
        assertTrue(adalahGalatJaringan(SocketTimeoutException("timeout")))
        assertTrue(adalahGalatJaringan(SSLException("handshake gagal")))
        assertTrue(adalahGalatJaringan(IOException("koneksi terputus")))
    }

    @Test
    fun `penolakan server bukan galat jaringan`() {
        assertFalse(adalahGalatJaringan(Postgrest.PostgrestException(400, "bad request")))
        assertFalse(adalahGalatJaringan(Postgrest.PostgrestException(401, "unauthorized")))
        assertFalse(adalahGalatJaringan(Postgrest.PostgrestException(403, "RLS menolak")))
        assertFalse(adalahGalatJaringan(Postgrest.PostgrestException(404, "not found")))
        assertFalse(adalahGalatJaringan(Postgrest.PostgrestException(409, "konflik")))
    }

    @Test
    fun `server bermasalah dan permintaan tunggu dianggap sementara`() {
        // 5xx: yang rusak servernya, bukan permintaannya — mengulang masuk akal.
        assertTrue(adalahGalatJaringan(Postgrest.PostgrestException(500, "internal")))
        assertTrue(adalahGalatJaringan(Postgrest.PostgrestException(503, "unavailable")))
        // 408 dan 429 secara eksplisit meminta dicoba lagi.
        assertTrue(adalahGalatJaringan(Postgrest.PostgrestException(408, "request timeout")))
        assertTrue(adalahGalatJaringan(Postgrest.PostgrestException(429, "too many requests")))
    }

    @Test
    fun `galat di luar jaringan tidak pernah dianggap jaringan`() {
        assertFalse(adalahGalatJaringan(IllegalStateException("bug kita sendiri")))
        assertFalse(adalahGalatJaringan(NullPointerException()))
    }

    @Test
    fun `setiap penyebab mendapat kalimat yang menyebut penyebabnya`() {
        assertEquals(
            "Tidak ada koneksi internet. Periksa jaringan Wi-Fi/data seluler Anda.",
            pesanGalatJaringan(UnknownHostException(), "riwayat"),
        )
        assertEquals(
            "Server tidak merespons (koneksi lambat). Coba lagi.",
            pesanGalatJaringan(SocketTimeoutException(), "riwayat"),
        )
        // Jam perangkat yang salah membuat TLS gagal. Menyuruh orang memeriksa internet
        // di kasus ini mengarahkan mereka ke hal yang keliru.
        assertEquals(
            "Gagal membangun koneksi aman. Pastikan tanggal & waktu perangkat Anda benar.",
            pesanGalatJaringan(SSLException("cert"), "riwayat"),
        )
        assertEquals(
            "Gagal terhubung ke server. Periksa koneksi internet.",
            pesanGalatJaringan(IOException(), "riwayat"),
        )
    }

    @Test
    fun `galat tak dikenal memakai nama hal yang gagal dimuat`() {
        assertEquals(
            "Gagal memuat riwayat opname. Coba lagi.",
            pesanGalatJaringan(IllegalStateException(), "riwayat opname"),
        )
    }
}
