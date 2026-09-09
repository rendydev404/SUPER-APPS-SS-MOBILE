package com.sukashawarma.superapp.feature.stok.domain

import com.sukashawarma.superapp.data.remote.Postgrest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StokErrorTest {

    private fun galat(kode: Int, body: String) = Postgrest.PostgrestException(kode, body)

    /**
     * Regresi dari kejadian 9 September 2026: laporan waste gagal dengan SQLSTATE
     * 57014 (statement timeout) karena `ledger_stok.ref_waste_id` belum ter-index,
     * tetapi pengguna hanya melihat "Server sedang bermasalah".
     *
     * Timeout harus dibedakan karena akibatnya berbeda: datanya dijamin TIDAK
     * tersimpan, jadi mengulang aman — sedangkan "server bermasalah" membuat orang
     * ragu apakah kiriman pertamanya sudah masuk.
     */
    @Test
    fun `statement timeout memberi tahu bahwa data belum tersimpan`() {
        val pesan = stokErrorMessage(
            galat(500, """{"code":"57014","message":"canceling statement due to statement timeout"}"""),
        )
        assertTrue(pesan, pesan.contains("BELUM tersimpan"))
        assertTrue(pesan, pesan.contains("coba kirim lagi"))
    }

    /** SQLSTATE lain pada 5xx tetap memakai pesan server generik. */
    @Test
    fun `galat server lain tetap pesan generik`() {
        assertEquals(
            "Server sedang bermasalah. Coba lagi beberapa saat lagi.",
            stokErrorMessage(galat(500, """{"code":"XX000","message":"internal error"}""")),
        )
    }

    @Test
    fun `badan balasan yang bukan JSON tidak membuat crash`() {
        assertEquals(
            "Server sedang bermasalah. Coba lagi beberapa saat lagi.",
            stokErrorMessage(galat(503, "<html>Bad Gateway</html>")),
        )
    }

    @Test
    fun `penolakan akses dan tidak ditemukan tetap seperti semula`() {
        assertEquals(
            "Anda tidak punya akses ke data outlet ini.",
            stokErrorMessage(galat(403, """{"code":"42501"}""")),
        )
        assertEquals(
            "Data yang diminta tidak ditemukan di server.",
            stokErrorMessage(galat(404, """{"code":"PGRST116"}""")),
        )
    }
}
