package com.sukashawarma.superapp.presentation.absensi.clock

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.media.ExifInterface
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class KompresSelfieTest {

    /** Meniru frame kamera 12 MP: gradasi + derau halus supaya encoder tidak
     *  diuntungkan bidang warna rata yang tidak realistis. */
    private fun fotoKamera(lebar: Int = 4000, tinggi: Int = 3000, mutu: Int = 95): ByteArray {
        val bmp = Bitmap.createBitmap(lebar, tinggi, Bitmap.Config.ARGB_8888)
        val kanvas = Canvas(bmp)
        kanvas.drawPaint(Paint().apply {
            shader = LinearGradient(0f, 0f, lebar.toFloat(), tinggi.toFloat(), Color.rgb(180, 140, 110), Color.rgb(40, 60, 90), Shader.TileMode.CLAMP)
        })
        val acak = Random(42)
        val titik = Paint()
        repeat(40_000) {
            titik.color = Color.argb(60, acak.nextInt(256), acak.nextInt(256), acak.nextInt(256))
            kanvas.drawCircle(acak.nextFloat() * lebar, acak.nextFloat() * tinggi, 2f + acak.nextFloat() * 6f, titik)
        }
        return ByteArrayOutputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, mutu, it); it.toByteArray() }
    }

    private fun denganOrientasi(jpeg: ByteArray, orientasi: Int): ByteArray {
        val f = File.createTempFile("selfie", ".jpg").apply { writeBytes(jpeg) }
        ExifInterface(f.absolutePath).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, orientasi.toString())
            saveAttributes()
        }
        return f.readBytes().also { f.delete() }
    }

    private fun ukuran(jpeg: ByteArray): Pair<Int, Int> {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size, o)
        return o.outWidth to o.outHeight
    }

    @Test
    fun `foto 12 MP dipangkas ke sisi 800 dan jauh lebih kecil`() = runBlocking {
        val asli = fotoKamera()
        val hasil = kompresSelfie(asli)
        val (w, h) = ukuran(hasil)
        println("asli=${asli.size / 1024} KB ${ukuran(asli)} -> hasil=${hasil.size / 1024} KB ${w}x$h")

        assertEquals(800, maxOf(w, h))
        assertEquals(600, minOf(w, h))
        assertTrue("hasil ${hasil.size} B melebihi 150 KB", hasil.size < 150 * 1024)
        assertTrue("hasil tidak lebih kecil dari asli", hasil.size * 10 < asli.size)
    }

    @Test
    fun `orientasi EXIF rotate 90 diterapkan ke piksel`() = runBlocking {
        val asli = denganOrientasi(fotoKamera(), ExifInterface.ORIENTATION_ROTATE_90)
        val hasil = kompresSelfie(asli)

        assertEquals(600 to 800, ukuran(hasil)) // lanskap sensor jadi potret
        val exifHasil = ExifInterface(hasil.inputStream())
            .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        // Tidak boleh ada tanda rotasi tersisa, atau browser memutar dua kali.
        assertTrue(exifHasil == ExifInterface.ORIENTATION_NORMAL || exifHasil == ExifInterface.ORIENTATION_UNDEFINED)
    }

    @Test
    fun `foto yang sudah kecil tidak diperbesar`() = runBlocking {
        val hasil = kompresSelfie(fotoKamera(640, 480))
        assertEquals(640 to 480, ukuran(hasil))
    }

    @Test
    fun `byte rusak dikembalikan apa adanya supaya absen tidak batal`() = runBlocking {
        val rusak = byteArrayOf(1, 2, 3, 4, 5)
        assertArrayEquals(rusak, kompresSelfie(rusak))
    }
}
