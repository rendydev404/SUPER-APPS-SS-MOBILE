package com.sukashawarma.superapp.feature.chat.domain

import androidx.compose.ui.graphics.Color
import com.sukashawarma.superapp.feature.chat.data.ChatWallpaperPrefs
import com.sukashawarma.superapp.feature.chat.data.ChatWallpapers
import com.sukashawarma.superapp.feature.chat.data.TipeWallpaper
import com.sukashawarma.superapp.domain.model.Role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ChatWallpaperLogicTest {

    @Test
    fun `cari mengembalikan DEFAULT untuk id null kosong atau tidak dikenal`() {
        assertEquals(ChatWallpapers.DEFAULT, ChatWallpapers.cari(null))
        assertEquals(ChatWallpapers.DEFAULT, ChatWallpapers.cari(""))
        assertEquals(ChatWallpapers.DEFAULT, ChatWallpapers.cari("   "))
        assertEquals(ChatWallpapers.DEFAULT, ChatWallpapers.cari("tidak_dikenal_xyz"))
    }

    @Test
    fun `cari mencocokkan id preset tanpa sensitif kapital`() {
        val hasil = ChatWallpapers.cari("WARM_IVORY")
        assertEquals("warm_ivory", hasil.id)
        assertNotNull(hasil.drawableRes)

        val cyber = ChatWallpapers.cari("Cyberpunk_Neon")
        assertEquals("cyberpunk_neon", cyber.id)
    }

    @Test
    fun `uraikan mengembalikan DefaultPutih untuk id kosong atau default`() {
        assertTrue(ChatWallpapers.uraikan(null) is TipeWallpaper.DefaultPutih)
        assertTrue(ChatWallpapers.uraikan("") is TipeWallpaper.DefaultPutih)
        assertTrue(ChatWallpapers.uraikan("default") is TipeWallpaper.DefaultPutih)
        assertTrue(ChatWallpapers.uraikan(ChatWallpaperPrefs.ID_BAWAAN) is TipeWallpaper.DefaultPutih)
    }

    @Test
    fun `uraikan mengenali warna solid berformat color hex`() {
        val tipe = ChatWallpapers.uraikan("color:#0B141A")
        assertTrue(tipe is TipeWallpaper.WarnaSolid)
        val warnaSolid = tipe as TipeWallpaper.WarnaSolid
        // Alpha channel FF, RGB 0B, 14, 1A
        assertEquals(Color(0xFF0B141A), warnaSolid.color)
    }

    @Test
    fun `uraikan fallback ke DefaultPutih jika format warna keliru`() {
        val tipe = ChatWallpapers.uraikan("color:bukan_hex")
        assertTrue(tipe is TipeWallpaper.DefaultPutih)
    }

    @Test
    fun `uraikan mengenali URL remote`() {
        val url = "https://supabase.co/storage/v1/object/authenticated/avatars/foto.jpg"
        val tipe = ChatWallpapers.uraikan(url)
        assertTrue(tipe is TipeWallpaper.RemoteUrl)
        assertEquals(url, (tipe as TipeWallpaper.RemoteUrl).url)
    }

    @Test
    fun `uraikan mengenali path avatars supabase storage dan mengonversinya ke RemoteUrl`() {
        val path = "avatars/user-123/grup-wp.jpg"
        val tipe = ChatWallpapers.uraikan(path)
        assertTrue("Harus dikenali sebagai RemoteUrl, aktual: $tipe", tipe is TipeWallpaper.RemoteUrl)
        val url = (tipe as TipeWallpaper.RemoteUrl).url
        assertTrue("URL harus mengandung endpoint authenticated storage", url.contains("storage/v1/object/authenticated/avatars/user-123/grup-wp.jpg"))
    }

    @Test
    fun `uraikan mengenali preset drawable yang terdaftar`() {
        val tipe = ChatWallpapers.uraikan("warm_ivory")
        assertTrue(tipe is TipeWallpaper.BawaanDrawable)
        val resId = (tipe as TipeWallpaper.BawaanDrawable).resId
        assertTrue(resId > 0)
    }

    @Test
    fun `uraikan berkas lokal mendeteksi file yang ada`() {
        // Buat file sementara
        val temp = File.createTempFile("test_wp", ".jpg")
        temp.deleteOnExit()

        val tipe = ChatWallpapers.uraikan("file:${temp.absolutePath}")
        assertTrue(tipe is TipeWallpaper.BerkasLokal)
        assertEquals(temp.absolutePath, (tipe as TipeWallpaper.BerkasLokal).file.absolutePath)
    }

    @Test
    fun `daftar warna solid memiliki format id yang seragam dan nama yang tidak kosong`() {
        assertTrue(ChatWallpapers.DAFTAR_WARNA.isNotEmpty())
        for (warna in ChatWallpapers.DAFTAR_WARNA) {
            assertTrue("ID warna harus diawali color:#, aktual: ${warna.id}", warna.id.startsWith("color:#"))
            assertTrue("Nama warna tidak boleh kosong", warna.nama.isNotBlank())
        }
    }

    @Test
    fun `pembatasan nilai peredup dimming berada dalam batas 0 hingga 0 koma 8`() {
        val batasBawah = (-0.5f).coerceIn(0f, 0.8f)
        assertEquals(0f, batasBawah, 0.001f)

        val batasAtas = (1.5f).coerceIn(0f, 0.8f)
        assertEquals(0.8f, batasAtas, 0.001f)

        val normal = (0.25f).coerceIn(0f, 0.8f)
        assertEquals(0.25f, normal, 0.001f)
    }

    @Test
    fun `bolehUbahWallpaper hanya mengizinkan role DEVELOPER`() {
        // True cases
        assertTrue(ChatWallpapers.bolehUbahWallpaper(Role.DEVELOPER, null))
        assertTrue(ChatWallpapers.bolehUbahWallpaper(null, "developer"))
        assertTrue(ChatWallpapers.bolehUbahWallpaper(null, "Developer"))
        assertTrue(ChatWallpapers.bolehUbahWallpaper(Role.DEVELOPER, "developer"))

        // False cases for regular user roles
        assertFalse(ChatWallpapers.bolehUbahWallpaper(Role.ADMIN, "admin"))
        assertFalse(ChatWallpapers.bolehUbahWallpaper(Role.OWNER, "owner"))
        assertFalse(ChatWallpapers.bolehUbahWallpaper(Role.LEADER, "leader"))
        assertFalse(ChatWallpapers.bolehUbahWallpaper(Role.AREA_MANAGER, "area_manager"))
        assertFalse(ChatWallpapers.bolehUbahWallpaper(Role.CREW, "crew"))
        assertFalse(ChatWallpapers.bolehUbahWallpaper(null, null))
    }
}
