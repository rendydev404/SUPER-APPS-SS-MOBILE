package com.sukashawarma.superapp.presentation.absensi.enroll

import com.sukashawarma.superapp.data.face.NormalizedFaceBox
import kotlin.math.abs

/** Pesan yang tampil selama kamera mencari frame enrollment terbaik. */
enum class EnrollScanHint(val message: String) {
    FIND_FACE("Arahkan wajah ke area pemindaian"),
    ONE_FACE_ONLY("Pastikan hanya ada satu wajah"),
    FACE_FORWARD("Hadap lurus ke kamera"),
    MOVE_CLOSER("Dekatkan wajah sedikit"),
    MOVE_BACK("Mundur sedikit dari kamera"),
    CENTER_FACE("Posisikan wajah di tengah"),
    HOLD_STILL("Bagus, tahan sebentar…"),
}

data class EnrollScanFeedback(
    val hint: EnrollScanHint,
    val progress: Float,
    val placementAccepted: Boolean,
    val captureReady: Boolean = false,
)

/**
 * Penilai scan murni (tanpa Android UI) agar threshold kualitas dan hold-to-capture bisa
 * diuji deterministik. Capture baru dilepas setelah satu wajah frontal, cukup besar, dan
 * berada di tengah secara terus-menerus selama [holdDurationMs].
 */
class EnrollFaceScanTracker(
    private val holdDurationMs: Long = 1_400L,
) {
    private var stableSinceMs: Long? = null
    private var captureArmed = true

    fun update(
        faceCount: Int,
        faceBox: NormalizedFaceBox?,
        yawDeg: Float,
        nowMs: Long,
    ): EnrollScanFeedback {
        val placementHint = evaluatePlacement(faceCount, faceBox, yawDeg)
        if (placementHint != EnrollScanHint.HOLD_STILL) {
            stableSinceMs = null
            return EnrollScanFeedback(
                hint = placementHint,
                progress = 0f,
                placementAccepted = false,
            )
        }

        val startedAt = stableSinceMs ?: nowMs.also { stableSinceMs = it }
        val progress = ((nowMs - startedAt).toFloat() / holdDurationMs)
            .coerceIn(0f, 1f)
        val ready = captureArmed && progress >= 1f
        if (ready) captureArmed = false
        return EnrollScanFeedback(
            hint = EnrollScanHint.HOLD_STILL,
            progress = progress,
            placementAccepted = true,
            captureReady = ready,
        )
    }

    fun reset() {
        stableSinceMs = null
        captureArmed = true
    }

    companion object {
        internal fun evaluatePlacement(
            faceCount: Int,
            faceBox: NormalizedFaceBox?,
            yawDeg: Float,
        ): EnrollScanHint {
            if (faceCount == 0) return EnrollScanHint.FIND_FACE
            if (faceCount != 1) return EnrollScanHint.ONE_FACE_ONLY
            if (faceBox == null) return EnrollScanHint.FIND_FACE
            if (abs(yawDeg) > 10f) return EnrollScanHint.FACE_FORWARD
            if (faceBox.w < 0.30f || faceBox.h < 0.36f) return EnrollScanHint.MOVE_CLOSER
            if (faceBox.w > 0.76f || faceBox.h > 0.84f) return EnrollScanHint.MOVE_BACK
            if (abs(faceBox.cx - 0.5f) > 0.13f || abs(faceBox.cy - 0.46f) > 0.16f) {
                return EnrollScanHint.CENTER_FACE
            }
            return EnrollScanHint.HOLD_STILL
        }
    }
}
