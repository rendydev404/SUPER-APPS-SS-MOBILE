package com.sukashawarma.superapp.domain.face

import android.graphics.Bitmap

/**
 * Ekstraksi descriptor wajah dari gambar 112×112 yang sudah *five-point aligned*.
 * Semua implementasi wajib menghasilkan embedding L2-normalized.
 */
interface FaceEmbeddingExtractor {
    /** null = gambar tidak layak, model gagal dimuat, atau inference gagal. */
    suspend fun extract(alignedFace: Bitmap): FloatArray?
}

class UnavailableFaceEmbeddingExtractor : FaceEmbeddingExtractor {
    override suspend fun extract(alignedFace: Bitmap): FloatArray? = null
}
