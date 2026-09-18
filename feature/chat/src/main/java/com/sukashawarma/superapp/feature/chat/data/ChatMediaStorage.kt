package com.sukashawarma.superapp.feature.chat.data

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.sukashawarma.superapp.data.remote.SupabaseClient

/**
 * ImageLoader terisolasi untuk media obrolan (foto/gambar) dengan cache disk yang lebih besar
 * (100MB) agar tidak menghapus avatar staf (20MB) dan mendukung pemuatan gambar berukuran sedang.
 */
object ChatMediaStorage {

    @Volatile
    private var pemuat: ImageLoader? = null

    fun imageLoader(konteks: Context): ImageLoader = pemuat ?: synchronized(this) {
        pemuat ?: bangun(konteks.applicationContext).also { pemuat = it }
    }

    private fun bangun(konteks: Context): ImageLoader = ImageLoader.Builder(konteks)
        .callFactory { SupabaseClient.okHttpClient }
        .memoryCache {
            MemoryCache.Builder(konteks).maxSizePercent(0.15).build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(konteks.cacheDir.resolve("chat_media_cache"))
                .maxSizeBytes(100L * 1024 * 1024)
                .build()
        }
        .respectCacheHeaders(false)
        .crossfade(true)
        .build()
}
