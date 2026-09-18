package com.sukashawarma.superapp.core.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.sukashawarma.superapp.presentation.theme.SukaBrown
import com.sukashawarma.superapp.presentation.theme.SukaOrange

import android.content.Context
import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView

val SukaDropdownShape = RoundedCornerShape(18.dp)

// iOS Specular Rim Border: Pantulan neon glowing listrik dalam primary color (Suka Orange)
// seperti glowing blue contour pada referensi iOS Apple tetapi menggunakan brand Suka Orange.
val SukaGlowBorderBrush = Brush.verticalGradient(
    listOf(
        Color(0xFFFFB37C), // Top electric amber highlight
        Color(0xFFFE8438), // Signature Suka Orange primary
        Color(0xFFEA580C), // Deep vibrant warm orange
        Color(0xFFFF9E58), // Bottom glowing rim
    )
)

// iOS Pure Frosted White Surface: Latar belakang putih bersih premium ala Apple iOS (100% opaque)
// Mencegah teks di layar belakang tembus, namun tetap mempertahankan estetika Apple iOS yang mewah
val SukaWhiteGlassBackgroundBrush = Brush.verticalGradient(
    listOf(
        Color(0xFFFFFFFF),
        Color(0xFFFCFCFD),
        Color(0xFFF9F9FA),
    )
)

// Alias kompatibilitas
val SukaGlassBorderBrush = SukaGlowBorderBrush
val SukaDarkGlassBackgroundBrush = SukaWhiteGlassBackgroundBrush
val SukaGlassBackgroundBrush = SukaWhiteGlassBackgroundBrush

/**
 * Efek blur & dim latar belakang tingkat window.
 * Mengaktifkan FLAG_DIM_BEHIND untuk memberikan kedalaman fokus (depth of field) estetis
 * seperti tampilan iPhone pada referensi, serta FLAG_BLUR_BEHIND di hardware yang mendukungnya.
 */
@Composable
private fun ApplyBlurBehind(blurRadiusDp: Int = 35, dimAmount: Float = 0.35f) {
    val view = LocalView.current
    val density = LocalDensity.current
    val blurRadiusPx = with(density) { blurRadiusDp.dp.roundToPx() }

    DisposableEffect(view, blurRadiusPx, dimAmount) {
        fun applyEffects() {
            try {
                val wm = view.context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                val params = view.layoutParams as? WindowManager.LayoutParams
                if (params != null && wm != null) {
                    var changed = false

                    // 1. Soft Backdrop Dim: Menghasilkan kedalaman fokus sinematik ala iOS
                    val dimFlag = WindowManager.LayoutParams.FLAG_DIM_BEHIND
                    if ((params.flags and dimFlag) == 0) {
                        params.flags = params.flags or dimFlag
                        changed = true
                    }
                    if (params.dimAmount != dimAmount) {
                        params.dimAmount = dimAmount
                        changed = true
                    }

                    // 2. Hardware Cross-Window Blur (Android 12+)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val blurFlag = WindowManager.LayoutParams.FLAG_BLUR_BEHIND
                        if ((params.flags and blurFlag) == 0) {
                            params.flags = params.flags or blurFlag
                            changed = true
                        }
                        if (params.blurBehindRadius != blurRadiusPx) {
                            params.setBlurBehindRadius(blurRadiusPx)
                            changed = true
                        }
                    }

                    if (changed) {
                        wm.updateViewLayout(view, params)
                    }
                }
            } catch (_: Throwable) {
                // Penanganan aman bila vendor ROM melarang modifikasi layoutParams
            }
        }

        if (view.isAttachedToWindow) {
            applyEffects()
        } else {
            val listener = object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {
                    applyEffects()
                }
                override fun onViewDetachedFromWindow(v: View) {}
            }
            view.addOnAttachStateChangeListener(listener)
            onDispose {
                view.removeOnAttachStateChangeListener(listener)
            }
        }
        onDispose {}
    }
}

/**
 * Dropdown Menu kustom SUKA SuperApp berlatar belakang iOS Dark Frosted Glass
 * dengan border neon berpendar (electric glow) warna primary Suka Orange dan sudut lengkung 18dp.
 */
@Composable
fun SukaDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 6.dp),
    properties: PopupProperties = PopupProperties(focusable = true),
    content: @Composable ColumnScope.() -> Unit,
) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            surface = Color.Transparent,
            surfaceTint = Color.Transparent,
            surfaceContainer = Color.Transparent,
            surfaceContainerHigh = Color.Transparent,
            surfaceContainerHighest = Color.Transparent,
            surfaceContainerLow = Color.Transparent,
            surfaceContainerLowest = Color.Transparent,
        ),
        shapes = MaterialTheme.shapes.copy(
            extraSmall = SukaDropdownShape,
            small = SukaDropdownShape,
            medium = SukaDropdownShape,
        ),
    ) {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            modifier = modifier
                .widthIn(min = 220.dp, max = 340.dp)
                // Outer Glow Bloom Aura dalam warna primary Suka Orange
                .shadow(
                    elevation = 18.dp,
                    shape = SukaDropdownShape,
                    spotColor = Color(0xFFFE8438),
                    ambientColor = Color(0x35FE8438),
                )
                .clip(SukaDropdownShape)
                .background(SukaWhiteGlassBackgroundBrush)
                .border(1.8.dp, SukaGlowBorderBrush, SukaDropdownShape),
            offset = offset,
            properties = properties,
            content = {
                ApplyBlurBehind(blurRadiusDp = 25, dimAmount = 0.16f)
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    content()
                }
            },
        )
    }
}

/**
 * Item interaktif di dalam SukaDropdownMenu.
 * Meniru row iOS Context Menu: tipografi Apple yang elegan, aksen Suka Orange saat aktif,
 * dan tactile spring scale feedback.
 */
@Composable
fun SukaDropdownMenuItem(
    text: String = "",
    title: String = "",
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    leadingIcon: ImageVector? = null,
    leading: (@Composable () -> Unit)? = null,
    subtitle: String? = null,
    badgeText: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val displayTitle = if (title.isNotEmpty()) title else text
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label = "item_press_scale",
    )

    val itemShape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .scale(scale)
            .clip(itemShape)
            .then(
                if (selected) {
                    Modifier.background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFFFE8438),
                                Color(0xFFEA580C),
                            )
                        )
                    )
                } else {
                    Modifier.background(
                        if (isPressed) Color(0xFFFE8438).copy(alpha = 0.08f)
                        else Color.Transparent
                    )
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 11.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (leading != null) {
                leading()
            } else if (leadingIcon != null) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(
                            if (selected) Color.White.copy(alpha = 0.22f)
                            else Color(0xFFFE8438).copy(alpha = 0.10f),
                            RoundedCornerShape(8.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = if (selected) Color.White else SukaOrange,
                        modifier = Modifier.size(17.dp),
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayTitle,
                    color = if (selected) Color.White else Color(0xFF1D1D1F),
                    fontSize = 13.5.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        color = if (selected) Color.White.copy(alpha = 0.85f)
                        else Color(0xFF86868B),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (badgeText != null) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (selected) Color.White.copy(alpha = 0.25f)
                    else SukaOrange.copy(alpha = 0.12f),
                ) {
                    Text(
                        badgeText,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = if (selected) Color.White else SukaOrange,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            if (trailing != null) {
                trailing()
            } else if (selected) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Terpilih",
                        tint = Color(0xFFEA580C),
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
        }
    }
}

/**
 * Header judul mini opsional di bagian atas SukaDropdownMenu.
 */
@Composable
fun SukaDropdownHeader(
    title: String,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title.uppercase(),
                color = SukaOrange,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                modifier = Modifier.weight(1f),
            )
            if (onClose != null) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .background(Color(0xFFF2F2F7), CircleShape)
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = Color(0xFF86868B),
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
        }
        HorizontalDivider(color = Color(0xFFEEEEF0), thickness = 0.8.dp)
        Spacer(Modifier.height(4.dp))
    }
}

/**
 * Kotak pemicu filter dropdown (Input Pod) bergaya kanonik SUKA SuperApp.
 * Menggantikan kartu pemicu klik mentahan dengan indikator panah berotasi.
 */
@Composable
fun SukaFilterDropdown(
    label: String,
    value: String,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "arrow_rot",
    )

    Column(modifier) {
        if (label.isNotEmpty()) {
            Text(
                label.uppercase(),
                color = SukaBrown.copy(alpha = 0.65f),
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp,
            )
            Spacer(Modifier.height(4.dp))
        }
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color.White,
            border = BorderStroke(
                1.dp,
                if (expanded) SukaOrange else SukaBrown.copy(alpha = 0.15f),
            ),
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        ) {
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (leadingIcon != null) {
                    Icon(leadingIcon, null, tint = SukaOrange, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    value,
                    Modifier.weight(1f),
                    color = SukaBrown,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    Icons.Default.ArrowDropDown,
                    null,
                    tint = if (expanded) SukaOrange else SukaBrown,
                    modifier = Modifier.rotate(rotation),
                )
            }
        }
    }
}
