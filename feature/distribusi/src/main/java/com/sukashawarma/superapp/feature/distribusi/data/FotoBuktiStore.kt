package com.sukashawarma.superapp.feature.distribusi.data

import android.graphics.Bitmap
import com.sukashawarma.superapp.core.storage.StorageUtil
import com.sukashawarma.superapp.data.remote.SupabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.ByteArrayOutputStream

/**
 * Foto bukti penerimaan barang.
 *
 * Bucket `verif-foto-bahan` privat dengan `file_size_limit` 204800 byte yang
 * ditegakkan server: berkas yang lebih besar ditolak, bukan dipotong. Kompresi
 * karena itu wajib, dan strateginya meniru `compressImage` di `VerifikasiForm.tsx`
 * supaya mutu foto dari HP setara dengan yang dari browser.
 */
object FotoBuktiStore {

    const val BUCKET = "verif-foto-bahan"
    const val BATAS_BYTE = 204800
    private const val DIMENSI_MAKS = 1280

    /** Path objek storage, TANPA nama bucket — inilah yang masuk ke kolom
     *  `surat_jalan_item.foto_path`, sama persis dengan yang ditulis web. */
    fun pathUntuk(suratJalanId: String, itemId: String): String = "$suratJalanId/$itemId.jpg"

    /** Mengembalikan path yang harus disimpan ke `foto_path`. */
    suspend fun unggah(suratJalanId: String, itemId: String, bitmap: Bitmap): String {
        val path = pathUntuk(suratJalanId, itemId)
        val bytes = kompres(bitmap, BATAS_BYTE)
        // StorageUtil.uploadJpeg mengembalikan "bucket/path"; nilai itu sengaja
        // diabaikan supaya nama bucket tidak ikut tersimpan di kolom.
        StorageUtil.uploadJpeg(BUCKET, path, bytes)
        return path
    }

    /**
     * Mengambil foto dari bucket privat. Endpoint `authenticated` cukup karena
     * interseptor di `SupabaseClient.okHttpClient` sudah menyisipkan token
     * pengguna — tidak perlu signed URL.
     *
     * Mengembalikan null bila objeknya tidak ada, supaya satu foto yang hilang
     * tidak menggagalkan seluruh layar detail.
     */
    suspend fun ambil(path: String): ByteArray? = withContext(Dispatchers.IO) {
        val url = "${SupabaseClient.BASE_URL}storage/v1/object/authenticated/$BUCKET/$path"
        val req = Request.Builder().url(url).get().build()
        SupabaseClient.okHttpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) null else resp.body?.bytes()
        }
    }

    /**
     * Turunkan resolusi lalu turunkan mutu bertahap sampai muat. Mulai dari 85
     * dan turun 10 tiap putaran sampai 20, sama dengan web — di bawah itu foto
     * bukti sudah terlalu buruk untuk jadi bukti, jadi hasil terakhir dipakai
     * apa adanya dan pemanggil yang memutuskan menolaknya.
     */
    internal fun kompres(bitmap: Bitmap, batasByte: Int): ByteArray {
        val sumber = kecilkan(bitmap)
        var mutu = 85
        var keluaran = jpeg(sumber, mutu)
        while (keluaran.size > batasByte && mutu > 20) {
            mutu -= 10
            keluaran = jpeg(sumber, mutu)
        }
        return keluaran
    }

    private fun kecilkan(bitmap: Bitmap): Bitmap {
        val lebar = bitmap.width
        val tinggi = bitmap.height
        if (lebar <= DIMENSI_MAKS && tinggi <= DIMENSI_MAKS) return bitmap
        val rasio = minOf(DIMENSI_MAKS.toFloat() / lebar, DIMENSI_MAKS.toFloat() / tinggi)
        return Bitmap.createScaledBitmap(
            bitmap,
            Math.round(lebar * rasio),
            Math.round(tinggi * rasio),
            true,
        )
    }

    private fun jpeg(bitmap: Bitmap, mutu: Int): ByteArray {
        val keluaran = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, mutu, keluaran)
        return keluaran.toByteArray()
    }
}
