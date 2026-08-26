package com.sukashawarma.superapp.data.face

import android.graphics.Bitmap
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.sukashawarma.superapp.data.face.FaceCropUtil.toRotatedBitmapOrNull
import com.sukashawarma.superapp.domain.liveness.FaceSignal

/** Hasil satu frame: sinyal untuk liveness + face ML Kit mentah + crop wajah siap pakai utk
 *  ekstraksi embedding (null kalau tak ada wajah, atau kamera IDLE-throttle skip crop).
 *  [faceBox]/[faceMesh] dinormalisasi jadi fraksi 0..1 relatif preview (sudah di-mirror utk
 *  kamera depan), dipakai UI utk overlay face-mesh yang presisi mengikuti bentuk wajah asli
 *  tiap frame — dihitung di sini (bukan di modul UI) supaya modul UI tidak perlu bergantung
 *  pada tipe ML Kit (`Face`/`Rect`/`FaceContour`) sama sekali. */
data class FrameFaceResult(
    val signal: FaceSignal,
    val face: Face?,
    val faceCrop: Bitmap? = null,
    val faceBox: NormalizedFaceBox? = null,
    val faceMesh: NormalizedFaceContours? = null,
)

/** Bounding box wajah dalam fraksi 0..1 relatif ke frame preview (sudah mirrored utk kamera depan). */
data class NormalizedFaceBox(val cx: Float, val cy: Float, val w: Float, val h: Float)

data class NormalizedPoint(val x: Float, val y: Float)

/**
 * Titik-titik kontur wajah asli (dari ML Kit `CONTOUR_MODE_ALL`), dinormalisasi jadi fraksi
 * 0..1 & di-mirror utk kamera depan. Ini yang bikin mesh di UI presisi mengikuti bentuk wajah
 * sebenarnya (alis, mata, hidung, bibir, garis rahang) — bukan oval sintetis.
 */
data class NormalizedFaceContours(
    val faceOval: List<NormalizedPoint>,
    val leftEyebrowTop: List<NormalizedPoint>,
    val leftEyebrowBottom: List<NormalizedPoint>,
    val rightEyebrowTop: List<NormalizedPoint>,
    val rightEyebrowBottom: List<NormalizedPoint>,
    val leftEye: List<NormalizedPoint>,
    val rightEye: List<NormalizedPoint>,
    val noseBridge: List<NormalizedPoint>,
    val noseBottom: List<NormalizedPoint>,
    val upperLipTop: List<NormalizedPoint>,
    val upperLipBottom: List<NormalizedPoint>,
    val lowerLipTop: List<NormalizedPoint>,
    val lowerLipBottom: List<NormalizedPoint>,
)

/**
 * Bungkus CameraX `ImageAnalysis.Analyzer` + ML Kit Face Detection. Throttle di ViewModel
 * (bukan di sini) supaya analyzer ini tetap dipakai apa adanya oleh kiosk 1:N maupun
 * panel 1:1 yang throttle-nya beda (mirror pola web: tick ~4 FPS, liveness ~5 FPS).
 */
class FaceDetectionAnalyzer(
    /** Dicek per-frame SEBELUM crop mahal (YUV->Bitmap) dikerjakan — false selama fase
     *  LIVENESS (cuma butuh yaw angle, bukan identitas) supaya tidak buang CPU/alokasi
     *  percuma di frame yang tak perlu identifikasi wajah. Default true = selalu crop. */
    private val needsCrop: () -> Boolean = { true },
    private val onResult: (FrameFaceResult) -> Unit,
) : ImageAnalysis.Analyzer {

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .enableTracking()
            .build()
    )

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        val rotation = imageProxy.imageInfo.rotationDegrees
        val (imgW, imgH) = if (rotation == 90 || rotation == 270) {
            mediaImage.height to mediaImage.width
        } else {
            mediaImage.width to mediaImage.height
        }
        val input = InputImage.fromMediaImage(mediaImage, rotation)
        detector.process(input)
            .addOnSuccessListener { faces ->
                if (faces.isEmpty()) {
                    onResult(FrameFaceResult(FaceSignal(yawDeg = 0f, faceCount = 0), null))
                } else {
                    val face = faces[0]
                    // Kamera depan: PreviewView otomatis mirror preview yang dilihat user (agar
                    // terasa natural seperti cermin), tapi buffer ImageAnalysis TIDAK ikut mirror
                    // (headEulerAngleY dihitung dari sensor mentah/unmirrored). Instruksi challenge
                    // ("tolehkan ke kiri") mengacu ke kiri user sendiri, yaitu sudut pandang preview
                    // yang mirrored — makanya sign-nya dibalik di sini supaya konsisten dengan yang
                    // user lihat & lakukan, bukan sudut pandang sensor mentah.
                    val signal = FaceSignal(yawDeg = -face.headEulerAngleY, faceCount = faces.size)
                    val crop = if (faces.size == 1 && needsCrop()) {
                        imageProxy.toRotatedBitmapOrNull()?.let { FaceCropUtil.alignForArcFace(it, face) }
                    } else null
                    val box = face.boundingBox
                    val faceBox = if (imgW > 0 && imgH > 0) NormalizedFaceBox(
                        cx = 1f - (box.left + box.right) / 2f / imgW.toFloat(), // mirror: preview kamera depan
                        cy = (box.top + box.bottom) / 2f / imgH.toFloat(),
                        w = (box.width().toFloat() / imgW).coerceIn(0.15f, 0.95f),
                        h = (box.height().toFloat() / imgH).coerceIn(0.15f, 0.95f),
                    ) else null
                    val faceMesh = if (imgW > 0 && imgH > 0) buildFaceMesh(face, imgW.toFloat(), imgH.toFloat()) else null
                    onResult(FrameFaceResult(signal, face, crop, faceBox, faceMesh))
                }
            }
            .addOnCompleteListener { imageProxy.close() }
    }

    private fun buildFaceMesh(face: Face, imgW: Float, imgH: Float): NormalizedFaceContours {
        fun points(type: Int): List<NormalizedPoint> =
            face.getContour(type)?.points.orEmpty().map { p -> NormalizedPoint(1f - p.x / imgW, p.y / imgH) }

        return NormalizedFaceContours(
            faceOval = points(FaceContour.FACE),
            leftEyebrowTop = points(FaceContour.LEFT_EYEBROW_TOP),
            leftEyebrowBottom = points(FaceContour.LEFT_EYEBROW_BOTTOM),
            rightEyebrowTop = points(FaceContour.RIGHT_EYEBROW_TOP),
            rightEyebrowBottom = points(FaceContour.RIGHT_EYEBROW_BOTTOM),
            leftEye = points(FaceContour.LEFT_EYE),
            rightEye = points(FaceContour.RIGHT_EYE),
            noseBridge = points(FaceContour.NOSE_BRIDGE),
            noseBottom = points(FaceContour.NOSE_BOTTOM),
            upperLipTop = points(FaceContour.UPPER_LIP_TOP),
            upperLipBottom = points(FaceContour.UPPER_LIP_BOTTOM),
            lowerLipTop = points(FaceContour.LOWER_LIP_TOP),
            lowerLipBottom = points(FaceContour.LOWER_LIP_BOTTOM),
        )
    }
}
