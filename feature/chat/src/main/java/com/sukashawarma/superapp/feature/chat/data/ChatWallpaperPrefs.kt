package com.sukashawarma.superapp.feature.chat.data

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Penyimpanan lokal untuk preferensi wallpaper chat (Chat Tim & Chat Pribadi).
 *
 * Mengelola berkas foto kustom dari galeri agar disimpan secara persisten
 * di `filesDir` privat aplikasi, menghindari masalah kedaluwarsa izin URI
 * pada Android.
 */
object ChatWallpaperPrefs {

    private const val PREFS_NAME = "chat_wallpaper_prefs"

    const val ID_BAWAAN = "bawaan"
    const val ID_DEFAULT = "default"

    private const val KEY_WALLPAPER_TIM = "key_wallpaper_tim"
    private const val KEY_DIMMING_TIM = "key_dimming_tim"

    private const val KEY_WALLPAPER_PRIBADI = "key_wallpaper_pribadi"
    private const val KEY_DIMMING_PRIBADI = "key_dimming_pribadi"

    private const val KEY_FOTO_KUSTOM_TERAKHIR = "key_foto_kustom_terakhir"

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // --- Preferensi Chat Tim ---

    /**
     * Mendapatkan ID wallpaper Chat Tim yang dipilih pengguna di perangkat ini.
     * Mengembalikan [ID_BAWAAN] jika pengguna memilih mengikuti wallpaper grup bawaan server.
     */
    fun getWallpaperTim(context: Context): String {
        return prefs(context).getString(KEY_WALLPAPER_TIM, ID_BAWAAN) ?: ID_BAWAAN
    }

    fun setWallpaperTim(context: Context, idWallpaper: String) {
        prefs(context).edit().putString(KEY_WALLPAPER_TIM, idWallpaper.trim()).apply()
    }

    fun getDimmingTim(context: Context): Float {
        return prefs(context).getFloat(KEY_DIMMING_TIM, 0f).coerceIn(0f, 0.8f)
    }

    fun setDimmingTim(context: Context, dimming: Float) {
        prefs(context).edit().putFloat(KEY_DIMMING_TIM, dimming.coerceIn(0f, 0.8f)).apply()
    }

    fun resetWallpaperTim(context: Context) {
        prefs(context).edit()
            .putString(KEY_WALLPAPER_TIM, ID_BAWAAN)
            .putFloat(KEY_DIMMING_TIM, 0f)
            .apply()
    }

    // --- Preferensi Chat Pribadi ---

    fun getWallpaperPribadi(context: Context): String {
        return prefs(context).getString(KEY_WALLPAPER_PRIBADI, ID_DEFAULT) ?: ID_DEFAULT
    }

    fun setWallpaperPribadi(context: Context, idWallpaper: String) {
        prefs(context).edit().putString(KEY_WALLPAPER_PRIBADI, idWallpaper.trim()).apply()
    }

    fun getDimmingPribadi(context: Context): Float {
        return prefs(context).getFloat(KEY_DIMMING_PRIBADI, 0f).coerceIn(0f, 0.8f)
    }

    fun setDimmingPribadi(context: Context, dimming: Float) {
        prefs(context).edit().putFloat(KEY_DIMMING_PRIBADI, dimming.coerceIn(0f, 0.8f)).apply()
    }

    fun resetWallpaperPribadi(context: Context) {
        prefs(context).edit()
            .putString(KEY_WALLPAPER_PRIBADI, ID_DEFAULT)
            .putFloat(KEY_DIMMING_PRIBADI, 0f)
            .apply()
    }

    // --- Manajemen Berkas Foto Kustom Lokal ---

    fun getFolderWallpaper(context: Context): File {
        val folder = File(context.applicationContext.filesDir, "chat_wallpapers")
        if (!folder.exists()) folder.mkdirs()
        return folder
    }

    /**
     * Membaca URI galeri, mengompresi ke resolusi optimal layar HP (maks 1920px),
     * dan menyimpannya ke berkas privat aplikasi agar aman dari pencabutan izin URI.
     *
     * @return Path berformat "file:/path/ke/berkas.jpg" atau null bila gagal.
     */
    fun simpanFotoKustom(context: Context, uri: Uri): String? {
        val appContext = context.applicationContext
        val folder = getFolderWallpaper(appContext)

        val bitmap = bacaDanKecilkan(appContext, uri, batasMaks = 1920) ?: return null

        val berkasBaru = File(folder, "custom_wp_${UUID.randomUUID().toString().take(8)}.jpg")
        return try {
            FileOutputStream(berkasBaru).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
            }
            bitmap.recycle()

            // Bersihkan berkas foto kustom lama jika ada
            val pathLama = prefs(appContext).getString(KEY_FOTO_KUSTOM_TERAKHIR, null)
            if (!pathLama.isNullOrBlank()) {
                val fileLama = File(pathLama.removePrefix("file:"))
                if (fileLama.exists() && fileLama.absolutePath != berkasBaru.absolutePath) {
                    fileLama.delete()
                }
            }

            val hasilPath = "file:${berkasBaru.absolutePath}"
            prefs(appContext).edit().putString(KEY_FOTO_KUSTOM_TERAKHIR, hasilPath).apply()
            hasilPath
        } catch (e: Exception) {
            android.util.Log.e("ChatWallpaperPrefs", "Gagal menyimpan foto kustom", e)
            null
        }
    }

    /**
     * Mendapatkan foto kustom terakhir yang tersimpan di perangkat ini jika berkasnya masih ada.
     */
    fun getFotoKustomTerakhir(context: Context): String? {
        val path = prefs(context).getString(KEY_FOTO_KUSTOM_TERAKHIR, null) ?: return null
        val berkas = File(path.removePrefix("file:"))
        return if (berkas.exists()) path else null
    }

    fun hapusFotoKustom(context: Context) {
        val path = prefs(context).getString(KEY_FOTO_KUSTOM_TERAKHIR, null)
        if (!path.isNullOrBlank()) {
            val berkas = File(path.removePrefix("file:"))
            if (berkas.exists()) berkas.delete()
        }
        prefs(context).edit().remove(KEY_FOTO_KUSTOM_TERAKHIR).apply()
    }

    private fun bacaDanKecilkan(context: Context, uri: Uri, batasMaks: Int): Bitmap? {
        val resolver = context.contentResolver
        val batas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        try {
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, batas) }
        } catch (e: Exception) {
            return null
        }
        if (batas.outWidth <= 0 || batas.outHeight <= 0) return null

        var sample = 1
        while (maxOf(batas.outWidth, batas.outHeight) / (sample * 2) >= batasMaks) {
            sample *= 2
        }

        val opsi = BitmapFactory.Options().apply { inSampleSize = sample }
        val mentah = try {
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opsi) }
        } catch (e: Exception) {
            null
        } ?: return null

        val skala = minOf(1f, batasMaks.toFloat() / maxOf(mentah.width, mentah.height))
        if (skala >= 1f) return mentah

        val hasil = Bitmap.createScaledBitmap(
            mentah,
            (mentah.width * skala).toInt().coerceAtLeast(1),
            (mentah.height * skala).toInt().coerceAtLeast(1),
            true,
        )
        if (hasil !== mentah) mentah.recycle()
        return hasil
    }
}
