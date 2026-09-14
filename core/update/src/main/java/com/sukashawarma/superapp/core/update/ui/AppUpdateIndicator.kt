package com.sukashawarma.superapp.core.update.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sukashawarma.superapp.core.update.AppUpdateManager
import com.sukashawarma.superapp.core.update.model.AppUpdateManifest
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * Custom cybernetic circular progress ring with sweep gradient arc,
 * pulsing plasma halo, and a glowing laser bead at the active progress tip.
 */
@Composable
fun CyberProgressRing(
    progress: Int,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    strokeWidth: Dp = 2.8.dp,
    accentColor: Color = Color(0xFFFF7A00)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cyber_ring")
    val sweepSpin by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep_spin"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0, 100) / 100f,
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "anim_progress"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size)
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()
            val canvasSize = this.size.minDimension
            val radius = (canvasSize - strokePx) / 2f
            val center = this.center

            // 1. Dark ambient background track
            drawCircle(
                color = Color(0x26FFFFFF),
                radius = radius,
                style = Stroke(width = strokePx)
            )

            // 2. Pulse radial glow under progress
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = glowAlpha * 0.35f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius * 1.35f
                )
            )

            // 3. Sweep progress arc
            val sweepAngle = animatedProgress * 360f
            if (sweepAngle > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color(0xFFFF3D00),
                            Color(0xFFFF9100),
                            Color(0xFFFFE082),
                            Color(0xFFFF9100),
                            Color(0xFFFF3D00)
                        ),
                        center = center
                    ),
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )

                // 4. Plasma laser bead at current progress tip
                val tipAngleRad = Math.toRadians((-90f + sweepAngle).toDouble())
                val tipX = center.x + radius * cos(tipAngleRad).toFloat()
                val tipY = center.y + radius * sin(tipAngleRad).toFloat()

                drawCircle(
                    color = Color.White,
                    radius = strokePx * 0.75f,
                    center = Offset(tipX, tipY)
                )
                drawCircle(
                    color = Color(0xFFFFE082).copy(alpha = glowAlpha),
                    radius = strokePx * 1.4f,
                    center = Offset(tipX, tipY)
                )
            } else {
                // When 0% or initializing, show rotating tracer bead
                val tracerRad = Math.toRadians((sweepSpin - 90f).toDouble())
                val tracerX = center.x + radius * cos(tracerRad).toFloat()
                val tracerY = center.y + radius * sin(tracerRad).toFloat()
                drawCircle(
                    color = Color(0xFFFF9100),
                    radius = strokePx * 0.8f,
                    center = Offset(tracerX, tracerY)
                )
            }
        }

        // 5. Monospace micro numeric percentage
        Text(
            text = "$progress",
            color = Color.White,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            letterSpacing = (-0.5).sp
        )
    }
}

/**
 * Dual counter-rotating radar indicator when applying patch/installing update.
 */
@Composable
fun InstallingRadarIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    strokeWidth: Dp = 2.6.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "installing_radar")
    val angle1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "radar_cw"
    )
    val angle2 by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)),
        label = "radar_ccw"
    )

    Canvas(modifier = modifier.size(size)) {
        val strokePx = strokeWidth.toPx()
        val canvasSize = this.size.minDimension
        val radius = (canvasSize - strokePx) / 2f

        drawCircle(
            color = Color(0x22FFFFFF),
            radius = radius,
            style = Stroke(width = strokePx)
        )

        // Outer clockwise arc (Emerald)
        drawArc(
            color = Color(0xFF10B981),
            startAngle = angle1,
            sweepAngle = 110f,
            useCenter = false,
            style = Stroke(width = strokePx, cap = StrokeCap.Round)
        )

        // Inner counter-clockwise arc (Orange/Amber)
        drawArc(
            color = Color(0xFFFF9100),
            startAngle = angle2,
            sweepAngle = 90f,
            useCenter = false,
            style = Stroke(width = strokePx * 0.8f, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun AppUpdateIndicator(
    manifest: AppUpdateManifest,
    downloadState: AppUpdateManager.DownloadState,
    downloadPayload: AppUpdateManager.DownloadPayload,
    downloadPayloadSizeBytes: Long?,
    downloadProgress: Int,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val actionable = downloadState == AppUpdateManager.DownloadState.AWAITING_USER_ACTION ||
        downloadState == AppUpdateManager.DownloadState.FAILED ||
        downloadState == AppUpdateManager.DownloadState.READY_TO_INSTALL ||
        downloadState == AppUpdateManager.DownloadState.IDLE

    val accent = when (downloadState) {
        AppUpdateManager.DownloadState.READY_TO_INSTALL -> Color(0xFF10B981) // Cyber Emerald
        AppUpdateManager.DownloadState.INSTALLING -> Color(0xFF10B981)
        AppUpdateManager.DownloadState.FAILED -> Color(0xFFEF4444) // Ruby Red
        AppUpdateManager.DownloadState.AWAITING_USER_ACTION -> Color(0xFFF59E0B) // Amber
        else -> Color(0xFFFF7A00) // Suka Electric Orange
    }

    val (primaryLabel, secondaryLabel) = when (downloadState) {
        AppUpdateManager.DownloadState.IDLE ->
            "v${manifest.versionName} Baru" to "Ketuk untuk unduh"
        AppUpdateManager.DownloadState.DOWNLOADING -> {
            val payloadName = if (downloadPayload == AppUpdateManager.DownloadPayload.DELTA_PATCH) "Patch" else "APK"
            val sizeStr = formatBytes(downloadPayloadSizeBytes)
            "v${manifest.versionName} • $payloadName" to (if (sizeStr.isNotEmpty()) "$sizeStr • $downloadProgress%" else "Mengunduh • $downloadProgress%")
        }
        AppUpdateManager.DownloadState.READY_TO_INSTALL ->
            "Update v${manifest.versionName}" to "Siap • Menerapkan..."
        AppUpdateManager.DownloadState.INSTALLING ->
            "Menerapkan Update" to "Sedang memasang..."
        AppUpdateManager.DownloadState.AWAITING_USER_ACTION ->
            "Izin Diperlukan" to "Ketuk untuk izinkan"
        AppUpdateManager.DownloadState.FAILED ->
            "Gagal Mengunduh" to "Ketuk untuk ulang"
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color(0xF20F172A), // Deep obsidian slate
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                listOf(
                    accent.copy(alpha = 0.7f),
                    Color(0x33FFFFFF),
                    accent.copy(alpha = 0.3f)
                )
            )
        ),
        shadowElevation = 10.dp,
        modifier = modifier
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(22.dp),
                spotColor = accent.copy(alpha = 0.5f)
            )
            .then(if (actionable) Modifier.clickable(onClick = onAction) else Modifier)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(start = 8.dp, end = 12.dp, top = 5.dp, bottom = 5.dp)
        ) {
            // Icon / Animated indicator (compact 28dp)
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(28.dp)) {
                when (downloadState) {
                    AppUpdateManager.DownloadState.DOWNLOADING -> {
                        CyberProgressRing(
                            progress = downloadProgress,
                            accentColor = accent
                        )
                    }
                    AppUpdateManager.DownloadState.INSTALLING -> {
                        InstallingRadarIndicator()
                    }
                    AppUpdateManager.DownloadState.READY_TO_INSTALL -> {
                        val infiniteTransition = rememberInfiniteTransition(label = "ready_pulse")
                        val pulseScale by infiniteTransition.animateFloat(
                            initialValue = 0.9f,
                            targetValue = 1.12f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(600, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "scale"
                        )
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(Color(0x3310B981))
                        ) {
                            Icon(
                                Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = Color(0xFF34D399),
                                modifier = Modifier
                                    .size(16.dp)
                                    .graphicsLayer {
                                        scaleX = pulseScale
                                        scaleY = pulseScale
                                    }
                            )
                        }
                    }
                    AppUpdateManager.DownloadState.AWAITING_USER_ACTION -> {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(Color(0x33F59E0B))
                        ) {
                            Icon(
                                Icons.Default.InstallMobile,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    AppUpdateManager.DownloadState.FAILED -> {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(Color(0x33EF4444))
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = null,
                                tint = Color(0xFFF87171),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    AppUpdateManager.DownloadState.IDLE -> {
                        val infiniteTransition = rememberInfiniteTransition(label = "idle_pulse")
                        val idleGlow by infiniteTransition.animateFloat(
                            initialValue = 0.4f,
                            targetValue = 0.95f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(900, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "glow"
                        )
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(Color(0x28FF7A00))
                        ) {
                            Icon(
                                Icons.Default.CloudDownload,
                                contentDescription = null,
                                tint = Color(0xFFFF9100).copy(alpha = idleGlow),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Compact kinetic typography
            Column(verticalArrangement = Arrangement.Center) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(accent)
                    )
                    Text(
                        text = primaryLabel,
                        color = Color.White,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.1.sp,
                        maxLines = 1
                    )
                }
                Spacer(Modifier.height(1.dp))
                Text(
                    text = secondaryLabel,
                    color = accent,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }

            // Action arrow indicator
            if (actionable) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = accent.copy(alpha = 0.8f),
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

@Composable
fun AppUpdateSuccessIndicator(
    versionName: String,
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    LaunchedEffect(versionName) {
        delay(SUCCESS_AUTO_DISMISS_MS)
        currentOnDismiss()
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color(0xF2064E3B), // Deep emerald obsidian
        border = BorderStroke(
            1.dp,
            Brush.linearGradient(
                listOf(
                    Color(0xFF34D399),
                    Color(0x44FFFFFF),
                    Color(0xFF059669)
                )
            )
        ),
        shadowElevation = 10.dp,
        modifier = modifier
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(22.dp),
                spotColor = Color(0x8010B981)
            )
            .clickable(onClick = onDismiss)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(start = 8.dp, end = 12.dp, top = 5.dp, bottom = 5.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0x3310B981))
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF34D399),
                    modifier = Modifier.size(16.dp)
                )
            }
            Column {
                Text(
                    text = "v$versionName Terpasang",
                    color = Color.White,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Aplikasi siap digunakan",
                    color = Color(0xFF6EE7B7),
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private const val SUCCESS_AUTO_DISMISS_MS = 4_000L

private fun formatBytes(bytes: Long?): String {
    if (bytes == null || bytes <= 0L) return ""
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) {
        String.format(Locale.US, "%.1fMB", mb)
    } else {
        String.format(Locale.US, "%.0fKB", kb)
    }
}
