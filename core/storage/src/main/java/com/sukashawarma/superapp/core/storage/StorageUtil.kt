package com.sukashawarma.superapp.core.storage

import com.sukashawarma.superapp.data.remote.SupabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Upload biner ke Supabase Storage lewat `SupabaseClient.okHttpClient` — host storage
 * sama dengan REST (`SupabaseClient.HOST`), jadi interceptor auth yang sudah ada
 * otomatis menyisipkan Authorization (access token user aktif) tanpa kode tambahan.
 */
object StorageUtil {
    private val jpegMedia = "image/jpeg".toMediaType()

    /** Upload JPEG ke `bucket/path` (upsert). Kembalikan `bucket/path` yang sama —
     *  itu format yang dipakai kolom `ref_photo_url`/`ref_photo_url_mobile` di
     *  `outlet_staff` (path objek storage, bukan URL publik penuh). */
    suspend fun uploadJpeg(bucket: String, path: String, bytes: ByteArray): String = withContext(Dispatchers.IO) {
        val url = "${SupabaseClient.BASE_URL}storage/v1/object/$bucket/$path"
        val req = Request.Builder()
            .url(url)
            .header("x-upsert", "true")
            .post(bytes.toRequestBody(jpegMedia))
            .build()
        SupabaseClient.okHttpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw java.io.IOException("Upload gagal (${resp.code}): ${resp.body?.string().orEmpty()}")
            }
        }
        "$bucket/$path"
    }
}
