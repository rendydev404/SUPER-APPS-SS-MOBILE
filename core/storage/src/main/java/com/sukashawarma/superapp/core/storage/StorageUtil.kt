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
    private val webpMedia = "image/webp".toMediaType()

    /** Upload JPEG ke `bucket/path` (upsert). Kembalikan `bucket/path` yang sama —
     *  itu format yang dipakai kolom `ref_photo_url`/`ref_photo_url_mobile` di
     *  `outlet_staff` (path objek storage, bukan URL publik penuh). */
    suspend fun uploadJpeg(bucket: String, path: String, bytes: ByteArray): String =
        upload(bucket, path, bytes, jpegMedia)

    /**
     * Upload WebP — dipakai foto chat, yang dikompres WebP supaya ringan.
     *
     * [upsert] default false di sini, berbeda dengan [uploadJpeg]. Menimpa objek
     * menuntut policy UPDATE pada `storage.objects` (Supabase menjalankan
     * INSERT ... ON CONFLICT DO UPDATE saat header `x-upsert` dikirim), dan
     * bucket yang namanya per-pesan seperti `chat-media` sengaja TIDAK punya
     * policy UPDATE: nama berkasnya UUID baru tiap kirim, jadi tidak ada yang
     * perlu ditimpa — dan tanpa hak menimpa, foto yang sudah terkirim tidak bisa
     * diganti isinya di belakang layar.
     */
    suspend fun uploadWebp(bucket: String, path: String, bytes: ByteArray, upsert: Boolean = false): String =
        upload(bucket, path, bytes, webpMedia, upsert)

    private suspend fun upload(
        bucket: String,
        path: String,
        bytes: ByteArray,
        media: okhttp3.MediaType,
        upsert: Boolean = true,
    ): String = withContext(Dispatchers.IO) {
        val url = "${SupabaseClient.BASE_URL}storage/v1/object/$bucket/$path"
        val req = Request.Builder()
            .url(url)
            .apply { if (upsert) header("x-upsert", "true") }
            .post(bytes.toRequestBody(media))
            .build()
        SupabaseClient.okHttpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw java.io.IOException("Upload gagal (${resp.code}): ${resp.body?.string().orEmpty()}")
            }
        }
        "$bucket/$path"
    }
}
