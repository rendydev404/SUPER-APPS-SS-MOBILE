package com.sukashawarma.superapp.domain.face

import kotlin.math.abs

/** A 2D point independent from Android graphics APIs, so alignment can be unit-tested on JVM. */
data class FacePoint(val x: Float, val y: Float)

/**
 * Least-squares similarity transform fitted across every available ArcFace landmark.
 *
 * Android's Matrix.setPolyToPoly accepts no more than four points. ArcFace uses five,
 * therefore this domain utility calculates the rotation, uniform scale, and translation
 * directly instead of discarding a landmark or relying on an Android API limitation.
 */
object ArcFaceSimilarityTransform {
    private const val MIN_VARIANCE = 1e-8

    data class Transform(
        val a: Float,
        val b: Float,
        val tx: Float,
        val ty: Float,
    ) {
        /** x' = ax - by + tx; y' = bx + ay + ty. */
        fun map(point: FacePoint): FacePoint = FacePoint(
            x = a * point.x - b * point.y + tx,
            y = b * point.x + a * point.y + ty,
        )

        /** Values in the row-major order required by android.graphics.Matrix.setValues(). */
        fun asAndroidMatrixValues(): FloatArray = floatArrayOf(
            a, -b, tx,
            b, a, ty,
            0f, 0f, 1f,
        )
    }

    /** Returns null for malformed or degenerate landmark sets. */
    fun fit(source: List<FacePoint>, target: List<FacePoint>): Transform? {
        if (source.size != target.size || source.size < 2) return null

        val count = source.size.toDouble()
        val sourceCenterX = source.sumOf { it.x.toDouble() } / count
        val sourceCenterY = source.sumOf { it.y.toDouble() } / count
        val targetCenterX = target.sumOf { it.x.toDouble() } / count
        val targetCenterY = target.sumOf { it.y.toDouble() } / count

        var denominator = 0.0
        var real = 0.0
        var imaginary = 0.0
        source.zip(target).forEach { (from, to) ->
            val sourceX = from.x - sourceCenterX
            val sourceY = from.y - sourceCenterY
            val targetX = to.x - targetCenterX
            val targetY = to.y - targetCenterY
            denominator += sourceX * sourceX + sourceY * sourceY
            real += sourceX * targetX + sourceY * targetY
            imaginary += sourceX * targetY - sourceY * targetX
        }
        if (abs(denominator) < MIN_VARIANCE) return null

        val a = real / denominator
        val b = imaginary / denominator
        val tx = targetCenterX - a * sourceCenterX + b * sourceCenterY
        val ty = targetCenterY - b * sourceCenterX - a * sourceCenterY
        return Transform(a.toFloat(), b.toFloat(), tx.toFloat(), ty.toFloat())
    }
}
