package com.sukashawarma.superapp.presentation.absensi.clock

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Memampatkan selfie absensi sebelum disimpan/diunggah ke bucket `selfies`.
 *
 * Frame dari `captureJpeg` adalah JPEG resolusi penuh sensor (3–5 MB). Untuk audit
 * absensi cukup sisi terpanjang [sisiMaks] px dengan mutu [mutu] — hasilnya kira-kira
 * 50–100 KB, jauh lebih ringan untuk kuota storage dan sinyal outlet.
 *
 * JPEG kamera tidak memutar piksel, hanya menandai orientasi di EXIF. Decode ke
 * Bitmap membuang EXIF itu, jadi rotasi/cermin diterapkan ke piksel di sini — tanpa
 * ini foto tersimpan miring 90 derajat. Bila apa pun gagal, byte asli dikembalikan
 * supaya absen tidak pernah batal gara-gara kompresi.
 */
suspend fun kompresSelfie(bytes: ByteArray, sisiMaks: Int = 800, mutu: Int = 75): ByteArray =
    withContext(Dispatchers.Default) {
        runCatching { kompres(bytes, sisiMaks, mutu) }.getOrNull() ?: bytes
    }

private fun kompres(bytes: ByteArray, sisiMaks: Int, mutu: Int): ByteArray? {
    val batas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, batas)
    if (batas.outWidth <= 0 || batas.outHeight <= 0) return null

    // inSampleSize dulu supaya tidak pernah memegang bitmap 12 MP utuh di memori.
    var sampel = 1
    while (maxOf(batas.outWidth, batas.outHeight) / (sampel * 2) >= sisiMaks) sampel *= 2
    val asal = BitmapFactory.decodeByteArray(
        bytes, 0, bytes.size,
        BitmapFactory.Options().apply { inSampleSize = sampel },
    ) ?: return null

    val skala = minOf(1f, sisiMaks.toFloat() / maxOf(asal.width, asal.height))
    val matriks = matriksOrientasi(bytes).apply { postScale(skala, skala) }
    val hasil = Bitmap.createBitmap(asal, 0, 0, asal.width, asal.height, matriks, true)

    return ByteArrayOutputStream().use { keluaran ->
        hasil.compress(Bitmap.CompressFormat.JPEG, mutu, keluaran)
        if (hasil !== asal) hasil.recycle()
        asal.recycle()
        keluaran.toByteArray()
    }
}

private fun matriksOrientasi(bytes: ByteArray): Matrix {
    val orientasi = runCatching {
        ExifInterface(bytes.inputStream()).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

    return Matrix().apply {
        when (orientasi) {
            ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { postRotate(90f); postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> { postRotate(270f); postScale(-1f, 1f) }
        }
    }
}
