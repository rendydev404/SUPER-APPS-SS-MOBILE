package com.sukashawarma.superapp.domain.face

import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.sqrt

/**
 * Port dari `mobile/native-superapp/.../utils/FaceRecognizer.kt` (app lama, sudah
 * dikalibrasi manusia 2026-07-20 — lihat [[MOBILE_MATCH_THRESHOLD]]) — model & threshold
 * SAMA PERSIS supaya tidak perlu kalibrasi ulang. Model: `facenet.tflite` (MobileFaceNet,
 * input 112x112x3, output 192d), di-load via mmap (asset WAJIB uncompressed — lihat
 * `noCompress += "tflite"` di build.gradle.kts).
 *
 * `interpreter` di-load sekali di konstruktor (bukan per-panggilan) — inference ~20-50ms
 * di CPU kelas menengah untuk model sekecil ini, aman dipanggil per beberapa ratus ms
 * (dibatasi throttle & `busy` flag di ViewModel pemanggil, bukan di kelas ini).
 */
class TfliteFaceEmbeddingExtractor(context: Context) : FaceEmbeddingExtractor {
    private var interpreter: Interpreter? = null
    private var inputImageSize = 112
    private var outputSize = 192

    var isModelLoaded: Boolean = false
        private set

    init {
        try {
            val assetManager = context.applicationContext.assets
            val fileDescriptor = assetManager.openFd("facenet.tflite")
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val modelBuffer = fileChannel.map(
                FileChannel.MapMode.READ_ONLY,
                fileDescriptor.startOffset,
                fileDescriptor.declaredLength,
            )
            val interp = Interpreter(modelBuffer)
            val inputShape = interp.getInputTensor(0).shape()
            if (inputShape.size >= 3) inputImageSize = inputShape[1]
            val outputShape = interp.getOutputTensor(0).shape()
            if (outputShape.size >= 2) outputSize = outputShape[1]
            interpreter = interp
            isModelLoaded = true
        } catch (e: Exception) {
            e.printStackTrace()
            isModelLoaded = false
        }
    }

    override suspend fun extract(faceCrop: Bitmap): FloatArray? = withContext(Dispatchers.Default) {
        val interp = interpreter ?: return@withContext null
        val resized = Bitmap.createScaledBitmap(faceCrop, inputImageSize, inputImageSize, true)
        val byteBuffer = convertBitmapToByteBuffer(resized)

        val output = Array(1) { FloatArray(outputSize) }
        interp.run(byteBuffer, output)

        val embedding = output[0]
        var sumSquares = 0f
        for (value in embedding) sumSquares += value * value
        val l2Norm = sqrt(sumSquares.toDouble()).toFloat()
        if (l2Norm > 0) {
            for (i in embedding.indices) embedding[i] = embedding[i] / l2Norm
        }
        embedding
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(1 * inputImageSize * inputImageSize * 3 * 4)
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(inputImageSize * inputImageSize)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        var pixel = 0
        for (i in 0 until inputImageSize) {
            for (j in 0 until inputImageSize) {
                val value = intValues[pixel++]
                // Normalisasi [0,255] -> [-1,1], standar keluarga model ArcFace/MobileFaceNet.
                byteBuffer.putFloat(((value shr 16 and 0xFF) - 127.5f) / 127.5f)
                byteBuffer.putFloat(((value shr 8 and 0xFF) - 127.5f) / 127.5f)
                byteBuffer.putFloat(((value and 0xFF) - 127.5f) / 127.5f)
            }
        }
        return byteBuffer
    }

    companion object {
        /**
         * Kalibrasi manusia 2026-07-20 (4 sample, app lama, model & preprocessing identik):
         * min same-person=0.6837, max different-person=0.5411. Threshold=0.65.
         * JANGAN ubah tanpa kalibrasi ulang di HP fisik.
         */
        const val MOBILE_MATCH_THRESHOLD = 0.65f
    }
}
