package com.sukashawarma.superapp.data.face

import android.graphics.Bitmap
import android.graphics.Rect
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream

object FaceCropUtil {
    /** Crop [bitmap] ke [box] + margin (wajah utuh dgn sedikit ruang, bukan mepet dahi/dagu —
     *  cocok utk model face-embedding yg dilatih pada crop longgar). Diklem ke batas bitmap. */
    fun cropToFace(bitmap: Bitmap, box: Rect, marginRatio: Float = 0.25f): Bitmap? {
        val marginX = (box.width() * marginRatio).toInt()
        val marginY = (box.height() * marginRatio).toInt()
        val left = (box.left - marginX).coerceIn(0, bitmap.width - 1)
        val top = (box.top - marginY).coerceIn(0, bitmap.height - 1)
        val right = (box.right + marginX).coerceIn(left + 1, bitmap.width)
        val bottom = (box.bottom + marginY).coerceIn(top + 1, bitmap.height)
        val w = right - left
        val h = bottom - top
        if (w <= 0 || h <= 0) return null
        return Bitmap.createBitmap(bitmap, left, top, w, h)
    }

    /** Dipakai alur enrollment: 1 foto JPEG utuh (bukan tiap-frame) → deteksi wajah sekali,
     *  lalu crop. ACCURATE mode karena bukan real-time (dijalankan sekali per capture). */
    suspend fun detectAndCrop(bitmap: Bitmap): Bitmap? {
        val detector = FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .build()
        )
        return try {
            val faces = detector.process(InputImage.fromBitmap(bitmap, 0)).await()
            val face = faces.firstOrNull() ?: return null
            cropToFace(bitmap, face.boundingBox)
        } catch (e: Exception) {
            null
        } finally {
            detector.close()
        }
    }

    /** ImageProxy (YUV_420_888) -> Bitmap yang SUDAH dirotasi sesuai `rotationDegrees`, supaya
     *  koordinat pixel-nya sejajar dengan `Face.boundingBox` dari ML Kit (yang dihitung pada
     *  frame ter-rotasi, bukan buffer sensor mentah). Lewat NV21 + JPEG karena tidak butuh
     *  dependency tambahan & cukup cepat untuk resolusi kecil (480x640, lihat FaceCameraPreview). */
    @OptIn(ExperimentalGetImage::class)
    fun ImageProxy.toRotatedBitmapOrNull(): Bitmap? {
        val image = this.image ?: return null
        return try {
            val nv21 = yuv420ToNv21(image)
            val yuvImage = android.graphics.YuvImage(nv21, android.graphics.ImageFormat.NV21, image.width, image.height, null)
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 90, out)
            val jpegBytes = out.toByteArray()
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size) ?: return null
            val rotation = imageInfo.rotationDegrees
            if (rotation == 0) return bitmap
            val matrix = android.graphics.Matrix().apply { postRotate(rotation.toFloat()) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            null
        }
    }

    private fun yuv420ToNv21(image: android.media.Image): ByteArray {
        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

        val ySize = yPlane.buffer.remaining()
        val uSize = uPlane.buffer.remaining()
        val vSize = vPlane.buffer.remaining()
        val nv21 = ByteArray(ySize + uSize + vSize)

        yPlane.buffer.get(nv21, 0, ySize)

        // NV21 = Y kemudian VU interleaved. Plane U/V ML Kit umumnya punya pixelStride=2
        // (sudah interleaved semi-planar) — copy langsung lebih cepat drpd loop per-pixel
        // kalau stride cocok, fallback ke loop kalau tidak (device tertentu beda layout).
        val vBuffer = vPlane.buffer
        val uBuffer = uPlane.buffer
        if (vPlane.pixelStride == 2 && uPlane.pixelStride == 2) {
            vBuffer.get(nv21, ySize, vSize)
        } else {
            var pos = ySize
            val width = image.width
            val height = image.height
            val uvHeight = height / 2
            val uvWidth = width / 2
            for (row in 0 until uvHeight) {
                for (col in 0 until uvWidth) {
                    val vIndex = row * vPlane.rowStride + col * vPlane.pixelStride
                    val uIndex = row * uPlane.rowStride + col * uPlane.pixelStride
                    if (vIndex < vBuffer.remaining() && uIndex < uBuffer.remaining()) {
                        nv21[pos++] = vBuffer.get(vIndex)
                        nv21[pos++] = uBuffer.get(uIndex)
                    }
                }
            }
        }
        return nv21
    }
}
