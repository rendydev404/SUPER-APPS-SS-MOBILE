package com.sukashawarma.superapp.domain.face

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArcFaceSimilarityTransformTest {
    @Test
    fun `fits all five ArcFace landmarks with rotation scale and translation`() {
        val source = listOf(
            FacePoint(0f, 0f), FacePoint(1f, 0f), FacePoint(0f, 1f),
            FacePoint(2f, 1f), FacePoint(-1f, 2f),
        )
        // 90-degree clockwise rotation, scale 2, then translation (10, -4).
        val expected = source.map { point -> FacePoint(2f * point.y + 10f, -2f * point.x - 4f) }

        val transform = requireNotNull(ArcFaceSimilarityTransform.fit(source, expected))

        source.zip(expected).forEach { (from, to) ->
            val actual = transform.map(from)
            assertEquals(to.x, actual.x, 0.0001f)
            assertEquals(to.y, actual.y, 0.0001f)
        }
    }

    @Test
    fun `rejects degenerate landmarks`() {
        val identical = List(5) { FacePoint(4f, 7f) }
        assertNull(ArcFaceSimilarityTransform.fit(identical, identical))
    }
}
