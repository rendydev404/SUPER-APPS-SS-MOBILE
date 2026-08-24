package com.sukashawarma.superapp.domain.face

import android.graphics.Bitmap

/**
 * Ekstraksi descriptor wajah (embedding) dari crop wajah frontal.
 *
 * BELUM DIISI: menunggu spike paritas model (plan §0.3 — MobileFaceNet/EdgeFace TFLite
 * yang embeddingnya HARUS sama dengan yang dipakai web setelah migrasi W4, plan §5.4).
 * Tanpa model itu terpasang, `extract()` selalu null — pipa clock-in di atasnya (deteksi,
 * liveness, GPS, submit, antrean offline) tetap utuh dan bisa diuji, cuma langkah
 * pencocokan identitas yang menunggu.
 */
interface FaceEmbeddingExtractor {
    /** null = wajah tak terdeteksi cukup jelas ATAU model belum terpasang. */
    suspend fun extract(faceCrop: Bitmap): FloatArray?
}

/** TODO(spike-model): ganti dengan implementasi TFLite begitu file model tersedia. */
class UnavailableFaceEmbeddingExtractor : FaceEmbeddingExtractor {
    override suspend fun extract(faceCrop: Bitmap): FloatArray? = null
}
