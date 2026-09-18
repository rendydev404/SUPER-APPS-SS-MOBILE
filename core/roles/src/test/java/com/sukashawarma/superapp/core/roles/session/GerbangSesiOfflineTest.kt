package com.sukashawarma.superapp.domain.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gerbang ini yang menentukan berapa lama perangkat hilang masih bisa membuka profil staf,
 * dan apakah profil orang lain bisa terbuka di perangkat yang dipakai bergantian. Keduanya
 * tidak akan pernah muncul di pengujian manual sehari-hari.
 */
class GerbangSesiOfflineTest {

    private val tujuHari = 7L * 24 * 60 * 60 * 1000
    private val sekarang = 1_800_000_000_000L

    private fun putuskan(
        snapshotUserId: String? = "staf-1",
        userIdTerverifikasi: String = "staf-1",
        terakhirOnlineMs: Long = sekarang,
    ) = GerbangSesiOffline.putuskan(
        snapshotUserId = snapshotUserId,
        userIdTerverifikasi = userIdTerverifikasi,
        terakhirOnlineMs = terakhirOnlineMs,
        sekarangMs = sekarang,
        umurMaksMs = tujuHari,
    )

    @Test
    fun `snapshot segar milik akun yang sama boleh dibuka`() {
        assertEquals(KeputusanSesiOffline.Boleh, putuskan(terakhirOnlineMs = sekarang - tujuHari + 1))
    }

    @Test
    fun `perangkat tanpa snapshot ditolak`() {
        val hasil = putuskan(snapshotUserId = null)
        assertTrue(hasil is KeputusanSesiOffline.Tolak)
        assertTrue((hasil as KeputusanSesiOffline.Tolak).pesan.contains("belum punya data sesi"))
    }

    @Test
    fun `snapshot milik akun lain ditolak`() {
        // Perangkat yang dipakai bergantian: mematikan data tidak boleh jadi cara
        // membuka profil orang sebelumnya.
        val hasil = putuskan(snapshotUserId = "staf-2", userIdTerverifikasi = "staf-1")
        assertTrue(hasil is KeputusanSesiOffline.Tolak)
        assertTrue((hasil as KeputusanSesiOffline.Tolak).pesan.contains("akun lain"))
    }

    @Test
    fun `lewat tujuh hari tanpa online ditolak`() {
        val hasil = putuskan(terakhirOnlineMs = sekarang - tujuHari - 1)
        assertTrue(hasil is KeputusanSesiOffline.Tolak)
        assertTrue((hasil as KeputusanSesiOffline.Tolak).pesan.contains("7 hari"))
    }

    @Test
    fun `tepat tujuh hari masih boleh`() {
        assertEquals(KeputusanSesiOffline.Boleh, putuskan(terakhirOnlineMs = sekarang - tujuHari))
    }

    @Test
    fun `jam perangkat dimundurkan jauh ditolak`() {
        // Tanpa ini, batas 7 hari bisa diperpanjang tanpa batas hanya dengan mengubah
        // tanggal perangkat — persis hal yang batas itu seharusnya cegah.
        val hasil = putuskan(terakhirOnlineMs = sekarang + 30L * 24 * 60 * 60 * 1000)
        assertTrue(hasil is KeputusanSesiOffline.Tolak)
        assertTrue((hasil as KeputusanSesiOffline.Tolak).pesan.contains("Tanggal & waktu"))
    }

    @Test
    fun `jam perangkat meleset sedikit tetap diterima`() {
        // Jam HP yang maju beberapa jam itu biasa; mengunci orang karenanya berlebihan.
        assertEquals(
            KeputusanSesiOffline.Boleh,
            putuskan(terakhirOnlineMs = sekarang + 3L * 60 * 60 * 1000),
        )
    }
}
