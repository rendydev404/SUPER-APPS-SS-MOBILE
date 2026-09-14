package com.sukashawarma.superapp.core.update.ui

import android.content.Context
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

internal data class NormalizedOverlayPosition(val x: Float, val y: Float) {
    fun clamped() = NormalizedOverlayPosition(x.coerceIn(0f, 1f), y.coerceIn(0f, 1f))
}

internal data class OverlayTravel(val maxX: Float, val maxY: Float)

internal fun overlayTravel(
    containerWidth: Int,
    containerHeight: Int,
    childWidth: Int,
    childHeight: Int,
    edgeMargin: Float
) = OverlayTravel(
    maxX = (containerWidth - childWidth - edgeMargin * 2).coerceAtLeast(0f),
    maxY = (containerHeight - childHeight - edgeMargin * 2).coerceAtLeast(0f)
)

internal fun denormalizeOverlayPosition(
    normalized: NormalizedOverlayPosition,
    travel: OverlayTravel,
    edgeMargin: Float
): Offset {
    val safe = normalized.clamped()
    return Offset(edgeMargin + safe.x * travel.maxX, edgeMargin + safe.y * travel.maxY)
}

internal fun normalizeOverlayPosition(
    position: Offset,
    travel: OverlayTravel,
    edgeMargin: Float
): NormalizedOverlayPosition = NormalizedOverlayPosition(
    x = if (travel.maxX == 0f) 0f else (position.x - edgeMargin) / travel.maxX,
    y = if (travel.maxY == 0f) 0f else (position.y - edgeMargin) / travel.maxY
).clamped()

internal fun clampOverlayPosition(
    position: Offset,
    travel: OverlayTravel,
    edgeMargin: Float
): Offset = Offset(
    x = position.x.coerceIn(edgeMargin, edgeMargin + travel.maxX),
    y = position.y.coerceIn(edgeMargin, edgeMargin + travel.maxY)
)

private object UpdateOverlayPositionStore {
    private const val PREFS = "superapp_update_indicator_position_v4"
    private const val X = "normalized_x"
    private const val Y = "normalized_y"

    fun load(context: Context): NormalizedOverlayPosition {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return NormalizedOverlayPosition(
            x = prefs.getFloat(X, 1.0f), // Pojok Kanan
            y = prefs.getFloat(Y, 1.0f)  // Bawah
        ).clamped()
    }

    fun save(context: Context, position: NormalizedOverlayPosition) {
        val safe = position.clamped()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putFloat(X, safe.x)
            .putFloat(Y, safe.y)
            .apply()
    }
}

/**
 * Layer overlay mengambang yang muncul di pojok kanan bawah secara default,
 * aman di atas bilah navigasi sistem (navigationBarsPadding), dan tetap dapat digeser
 * jika diinginkan oleh pengguna.
 */
@Composable
fun DraggableUpdateOverlay(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val edgeMarginPx = remember(density) { with(density) { 14.dp.toPx() } }

    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var childSize by remember { mutableStateOf(IntSize.Zero) }
    var normalizedPosition by remember {
        mutableStateOf(UpdateOverlayPositionStore.load(context))
    }
    var pixelPosition by remember { mutableStateOf<Offset?>(null) }
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(containerSize, childSize) {
        if (containerSize.width > 0 && containerSize.height > 0 &&
            childSize.width > 0 && childSize.height > 0
        ) {
            val travel = overlayTravel(
                containerWidth = containerSize.width,
                containerHeight = containerSize.height,
                childWidth = childSize.width,
                childHeight = childSize.height,
                edgeMargin = edgeMarginPx
            )
            pixelPosition = denormalizeOverlayPosition(normalizedPosition, travel, edgeMarginPx)
            isVisible = true
        }
    }

    val enterScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.85f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "overlay_scale"
    )
    val enterAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "overlay_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .onSizeChanged { containerSize = it }
    ) {
        val currentOffset = pixelPosition
        if (currentOffset != null) {
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            currentOffset.x.roundToInt(),
                            currentOffset.y.roundToInt()
                        )
                    }
                    .graphicsLayer {
                        scaleX = enterScale
                        scaleY = enterScale
                        alpha = enterAlpha
                    }
                    .onSizeChanged { childSize = it }
                    .pointerInput(containerSize, childSize, edgeMarginPx) {
                        if (containerSize.width <= 0 || containerSize.height <= 0 ||
                            childSize.width <= 0 || childSize.height <= 0
                        ) return@pointerInput

                        detectDragGestures(
                            onDragEnd = {
                                val current = pixelPosition ?: return@detectDragGestures
                                val travel = overlayTravel(
                                    containerWidth = containerSize.width,
                                    containerHeight = containerSize.height,
                                    childWidth = childSize.width,
                                    childHeight = childSize.height,
                                    edgeMargin = edgeMarginPx
                                )
                                val saved = normalizeOverlayPosition(current, travel, edgeMarginPx)
                                normalizedPosition = saved
                                UpdateOverlayPositionStore.save(context, saved)
                            }
                        ) { change, dragAmount ->
                            change.consume()
                            val travel = overlayTravel(
                                containerWidth = containerSize.width,
                                containerHeight = containerSize.height,
                                childWidth = childSize.width,
                                childHeight = childSize.height,
                                edgeMargin = edgeMarginPx
                            )
                            val base = pixelPosition ?: denormalizeOverlayPosition(
                                normalizedPosition,
                                travel,
                                edgeMarginPx
                            )
                            pixelPosition = clampOverlayPosition(
                                position = base + dragAmount,
                                travel = travel,
                                edgeMargin = edgeMarginPx
                            )
                        }
                    },
                content = content
            )
        } else {
            // Placeholder di pojok kanan bawah selama frame pertama pengukuran
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 14.dp, bottom = 14.dp)
                    .graphicsLayer { alpha = 0f }
                    .onSizeChanged { childSize = it },
                content = content
            )
        }
    }
}
