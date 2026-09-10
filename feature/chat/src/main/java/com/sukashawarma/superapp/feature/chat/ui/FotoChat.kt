package com.sukashawarma.superapp.feature.chat.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import java.io.ByteArrayOutputStream

/**
 * Kompresi foto chat: sisi terpanjang 1600 px, WebP kualitas 85 — masih tajam
 * di layar ponsel mana pun tapi biasanya tinggal 100-300 KB. WEBP_LOSSY baru
 * ada di API 30; di bawah itu konstanta WEBP lama (juga lossy) yang dipakai.
 */
object FotoChat {
    private const val SISI_MAKS = 1600
    private const val KUALITAS = 85

    fun kompres(context: Context, uri: Uri): ByteArray? {
        val resolver = context.contentResolver

        // Baca dimensi dulu supaya foto kamera 12 MP tidak di-decode penuh
        // hanya untuk dikecilkan lagi.
        //
        // JANGAN tambahkan `?: return null` di baris pembacaan dimensi. Dengan
        // `inJustDecodeBounds = true`, decodeStream SELALU mengembalikan null —
        // itu memang kontraknya, hasilnya dititipkan ke `batas`. Menyambungnya
        // dengan elvis membuat setiap foto galeri ditolak sebagai "tidak bisa
        // dibaca" padahal berkasnya sehat. Kegagalan yang sebenarnya terbaca
        // dari outWidth/outHeight di bawah.
        val batas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        try {
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, batas) }
        } catch (e: Exception) {
            android.util.Log.e("FotoChat", "gagal membaca dimensi foto", e)
            return null
        }
        if (batas.outWidth <= 0 || batas.outHeight <= 0) return null

        var sampel = 1
        while (maxOf(batas.outWidth, batas.outHeight) / (sampel * 2) >= SISI_MAKS) sampel *= 2

        val opsi = BitmapFactory.Options().apply { inSampleSize = sampel }
        val kasar = try {
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opsi) }
        } catch (e: Exception) {
            android.util.Log.e("FotoChat", "gagal decode foto", e)
            null
        } ?: return null

        return kompres(kasar)
    }

    /** Jalur kamera dalam aplikasi: bitmap sudah di tangan, tinggal dikecilkan. */
    fun kompres(sumber: Bitmap): ByteArray? {
        val terpanjang = maxOf(sumber.width, sumber.height)
        val bitmap = if (terpanjang > SISI_MAKS) {
            val skala = SISI_MAKS.toFloat() / terpanjang
            Bitmap.createScaledBitmap(
                sumber,
                (sumber.width * skala).toInt().coerceAtLeast(1),
                (sumber.height * skala).toInt().coerceAtLeast(1),
                true,
            )
        } else {
            sumber
        }

        val keluaran = ByteArrayOutputStream()
        @Suppress("DEPRECATION")
        val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            Bitmap.CompressFormat.WEBP
        }
        val sukses = bitmap.compress(format, KUALITAS, keluaran)
        // Bitmap hasil penyekalaan boleh dibuang; sumbernya milik pemanggil
        // (jalur kamera masih memakainya untuk pratinjau).
        if (bitmap !== sumber) bitmap.recycle()
        return if (sukses) keluaran.toByteArray() else null
    }
}
