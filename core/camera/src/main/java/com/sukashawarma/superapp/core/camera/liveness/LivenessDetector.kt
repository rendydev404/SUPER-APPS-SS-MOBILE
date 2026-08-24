package com.sukashawarma.superapp.domain.liveness

enum class Challenge(val label: String) {
    TURN_LEFT("Tolehkan kepala ke kiri"),
    TURN_RIGHT("Tolehkan kepala ke kanan"),
}

fun pickChallenge(random: () -> Float = { Math.random().toFloat() }): Challenge =
    if (random() < 0.5f) Challenge.TURN_LEFT else Challenge.TURN_RIGHT

/** Sinyal minimal dari analyzer kamera — dipisah dari tipe ML Kit supaya detector ini
 *  murni & bisa diuji tanpa Android. yawDeg: turunan dari ML Kit headEulerAngleY, sudah
 *  disesuaikan tandanya di FaceDetectionAnalyzer supaya cocok sudut pandang USER (mirrored,
 *  sama seperti yang dilihat di preview kamera depan) — negatif = menoleh ke kiri (kiri
 *  user sendiri), positif = ke kanan. */
data class FaceSignal(val yawDeg: Float, val faceCount: Int)

/**
 * Port persis dari lib/face/liveness.ts (web): dua fase — fase 0 tunggu GERAKAN
 * tantangan, fase 1 tunggu kembali FRONTAL. Lolos HANYA saat kembali frontal (bukan
 * saat menoleh) supaya descriptor yang dibandingkan tetap descriptor frontal, konsisten
 * dengan enrollment yang juga frontal-only.
 */
class LivenessDetector(private val challenge: Challenge) {
    private var phase = 0
    private var passed = false

    companion object {
        private const val TURN_THRESHOLD_DEG = 15f
        private const val FRONTAL_THRESHOLD_DEG = 8f
    }

    /** Kembalikan true begitu lolos (idempoten — panggilan berikut tetap true). */
    fun feed(signal: FaceSignal): Boolean {
        if (passed) return true
        if (signal.faceCount != 1) return false // wajah hilang/lebih dari satu -> pemanggil yang membatalkan

        val facingLeft = signal.yawDeg < -TURN_THRESHOLD_DEG
        val facingRight = signal.yawDeg > TURN_THRESHOLD_DEG
        val isFrontal = kotlin.math.abs(signal.yawDeg) < FRONTAL_THRESHOLD_DEG

        if (phase == 0) {
            val acted = when (challenge) {
                Challenge.TURN_LEFT -> facingLeft
                Challenge.TURN_RIGHT -> facingRight
            }
            if (!acted) return false
            phase = 1
        }

        if (isFrontal) passed = true
        return passed
    }
}
