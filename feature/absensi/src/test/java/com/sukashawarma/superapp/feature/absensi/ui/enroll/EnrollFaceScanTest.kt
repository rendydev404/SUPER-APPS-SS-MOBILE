package com.sukashawarma.superapp.presentation.absensi.enroll

import com.sukashawarma.superapp.data.face.NormalizedFaceBox
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EnrollFaceScanTest {
    private val centeredFace = NormalizedFaceBox(cx = 0.5f, cy = 0.46f, w = 0.48f, h = 0.58f)

    @Test
    fun `stable clear face requests exactly one automatic capture`() {
        val tracker = EnrollFaceScanTracker(holdDurationMs = 1_000L)

        val started = tracker.update(1, centeredFace, yawDeg = 2f, nowMs = 100L)
        val halfway = tracker.update(1, centeredFace, yawDeg = 1f, nowMs = 600L)
        val ready = tracker.update(1, centeredFace, yawDeg = 0f, nowMs = 1_100L)
        val nextFrame = tracker.update(1, centeredFace, yawDeg = 0f, nowMs = 1_200L)

        assertEquals(0f, started.progress)
        assertEquals(0.5f, halfway.progress)
        assertTrue(ready.captureReady)
        assertFalse(nextFrame.captureReady)
    }

    @Test
    fun `invalid frame resets hold progress`() {
        val tracker = EnrollFaceScanTracker(holdDurationMs = 1_000L)

        tracker.update(1, centeredFace, yawDeg = 0f, nowMs = 0L)
        val invalid = tracker.update(2, centeredFace, yawDeg = 0f, nowMs = 700L)
        val restarted = tracker.update(1, centeredFace, yawDeg = 0f, nowMs = 900L)

        assertEquals(EnrollScanHint.ONE_FACE_ONLY, invalid.hint)
        assertEquals(0f, invalid.progress)
        assertEquals(0f, restarted.progress)
        assertFalse(restarted.captureReady)
    }

    @Test
    fun `pose size and position produce clear guidance`() {
        assertEquals(
            EnrollScanHint.FACE_FORWARD,
            EnrollFaceScanTracker.evaluatePlacement(1, centeredFace, yawDeg = 18f),
        )
        assertEquals(
            EnrollScanHint.MOVE_CLOSER,
            EnrollFaceScanTracker.evaluatePlacement(
                1,
                centeredFace.copy(w = 0.2f, h = 0.3f),
                yawDeg = 0f,
            ),
        )
        assertEquals(
            EnrollScanHint.CENTER_FACE,
            EnrollFaceScanTracker.evaluatePlacement(
                1,
                centeredFace.copy(cx = 0.76f),
                yawDeg = 0f,
            ),
        )
    }

    @Test
    fun `reset rearms automatic capture`() {
        val tracker = EnrollFaceScanTracker(holdDurationMs = 100L)
        tracker.update(1, centeredFace, 0f, 0L)
        assertTrue(tracker.update(1, centeredFace, 0f, 100L).captureReady)

        tracker.reset()

        tracker.update(1, centeredFace, 0f, 200L)
        assertTrue(tracker.update(1, centeredFace, 0f, 300L).captureReady)
    }
}
