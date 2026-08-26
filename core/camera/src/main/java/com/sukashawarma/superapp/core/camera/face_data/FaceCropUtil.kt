package com.sukashawarma.superapp.data.face

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Rect
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceLandmark
import com.sukashawarma.superapp.domain.face.ArcFaceSimilarityTransform
import com.sukashawarma.superapp.domain.face.FacePoint
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream

object FaceCropUtil {
    private const val ARCFACE_IMAGE_SIZE = 112

    /** Mengubah wajah ke template ArcFace 112×112 melalui lima landmark wajah. */
    fun alignForArcFace(bitmap: Bitmap, face: Face): Bitmap? {
        val landmarks = listOf(
            FaceLandmark.LEFT_EYE,
            FaceLandmark.RIGHT_EYE,
            FaceLandmark.NOSE_BASE,
            FaceLandmark.MOUTH_LEFT,
            FaceLandmark.MOUTH_RIGHT,
        ).map { face.getLandmark(it)?.position ?: return null }
        val source = landmarks.map { FacePoint(it.x, it.y) }
        val target = listOf(
            FacePoint(38.2946f, 51.6963f), FacePoint(73.5318f, 51.5014f),
            FacePoint(56.0252f, 71.7366f), FacePoint(41.5493f, 92.3655f),
            FacePoint(70.7299f, 92.2041f),
        )
        val alignment = ArcFaceSimilarityTransform.fit(source, target) ?: return null
        val transform = Matrix().apply { setValues(alignment.asAndroidMatrixValues()) }
        return try {
            Bitmap.createBitmap(ARCFACE_IMAGE_SIZE, ARCFACE_IMAGE_SIZE, Bitmap.Config.ARGB_8888).also {
                Canvas(it).drawBitmap(bitmap, transform, null)
            }
        } catch (_: IllegalArgumentException) {
            null
        }
    }

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

    /** Dipakai enrollment: foto utuh → deteksi + five-point alignment sekali. */
    suspend fun detectAndAlignForArcFace(bitmap: Bitmap): Bitmap? {
        val detector = FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .build()
        )
        return try {
            val faces = detector.process(InputImage.fromBitmap(bitmap, 0)).await()
            if (faces.size != 1) return null
            alignForArcFace(bitmap, faces.first())
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
