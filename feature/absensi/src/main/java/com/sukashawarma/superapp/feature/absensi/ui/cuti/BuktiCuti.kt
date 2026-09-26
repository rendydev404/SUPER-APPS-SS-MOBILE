package com.sukashawarma.superapp.presentation.absensi.cuti

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import com.sukashawarma.superapp.core.camera.keJpeg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/** Jenis cuti yang WAJIB berlampiran bukti (surat dokter / foto obat). */
const val JENIS_WAJIB_BUKTI = "sakit"

/**
 * Foto bukti pengajuan sakit, disiapkan di HP sebelum dikirim.
 *
 * Disimpan di `filesDir`, BUKAN `cacheDir`: pengajuan yang diajukan saat offline menunggu
 * di antrean Outbox bersama berkas ini, dan sistem boleh membersihkan cache kapan saja —
 * lampirannya akan hilang sebelum sempat terunggah.
 *
 * Sisi terpanjang 1600px, JPEG mutu 80 (~200-400 KB): tulisan tangan dokter dan nama obat
 * masih terbaca jelas, tetapi unggahan tetap ringan di sinyal outlet.
 */
object BuktiCuti {
    private const val SISI_MAKS = 1600
    private const val MUTU = 80

    private fun folder(konteks: Context) =
        File(konteks.filesDir, "bukti_cuti").apply { mkdirs() }

    suspend fun dariBitmap(konteks: Context, foto: Bitmap): File? = withContext(Dispatchers.Default) {
        runCatching { tulis(konteks, foto.keJpeg(SISI_MAKS, MUTU)) }.getOrNull()
    }

    suspend fun dariUri(konteks: Context, uri: Uri): File? = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = konteks.contentResolver
            val batas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, batas) }
            // Decode sedekat mungkin ke 1600px tanpa memuat foto 12 MP utuh ke memori.
            var contoh = 1
            while (maxOf(batas.outWidth, batas.outHeight) / (contoh * 2) >= SISI_MAKS) contoh *= 2
            val mentah = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = contoh })
            } ?: return@runCatching null
            // Foto kamera menyimpan arah di EXIF; tanpa ini surat dokter bisa terkirim miring.
            val derajat = resolver.openInputStream(uri)?.use { masuk ->
                when (ExifInterface(masuk).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
            val tegak = if (derajat == 0f) mentah
            else Bitmap.createBitmap(mentah, 0, 0, mentah.width, mentah.height, Matrix().apply { postRotate(derajat) }, true)
            tulis(konteks, tegak.keJpeg(SISI_MAKS, MUTU))
        }.getOrNull()
    }

    private fun tulis(konteks: Context, jpeg: ByteArray): File =
        File(folder(konteks), "${UUID.randomUUID()}.jpg").apply { writeBytes(jpeg) }
}
