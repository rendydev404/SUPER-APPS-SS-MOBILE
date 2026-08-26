package com.sukashawarma.superapp.domain.face

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** InsightFace buffalo_sc ArcFace (w600k_mbf), dikonversi ke NCNN dan dibundel offline. */
class NcnnArcFaceEmbeddingExtractor(context: Context) : FaceEmbeddingExtractor {
    private val nativeHandle = nativeCreate(context.applicationContext.assets)

    val isModelLoaded: Boolean
        get() = nativeHandle != 0L

    override suspend fun extract(alignedFace: Bitmap): FloatArray? = withContext(Dispatchers.Default) {
        if (nativeHandle == 0L || alignedFace.width != INPUT_SIZE || alignedFace.height != INPUT_SIZE) return@withContext null
        nativeExtract(nativeHandle, alignedFace)?.takeIf { it.size == EMBEDDING_SIZE }
    }

    @Suppress("deprecation")
    protected fun finalize() {
        if (nativeHandle != 0L) nativeDestroy(nativeHandle)
    }

    private external fun nativeCreate(assetManager: AssetManager): Long
    private external fun nativeDestroy(handle: Long)
    private external fun nativeExtract(handle: Long, alignedFace: Bitmap): FloatArray?

    private companion object {
        const val INPUT_SIZE = 112
        const val EMBEDDING_SIZE = 512

        init { System.loadLibrary("arcface_ncnn") }
    }
}
