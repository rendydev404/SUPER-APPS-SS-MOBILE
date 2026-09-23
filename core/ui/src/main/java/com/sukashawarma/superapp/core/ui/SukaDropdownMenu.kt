package com.sukashawarma.superapp.core.ui

import android.content.Context
import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.kaca.IkonIos

/** Sudut menu bergaya pull-down menu iOS. */
val SukaDropdownShape = RoundedCornerShape(14.dp)

/**
 * Apakah perangkat sanggup mengaburkan jendela di belakang popup (Android 12+ dan
 * tidak dimatikan oleh mode hemat baterai/vendor). Dibaca sekali per menu.
 */
@Composable
private fun blurLintasJendelaAktif(): Boolean {
    val konteks = LocalContext.current
    return remember(konteks) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return@remember false
        val wm = konteks.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        wm?.isCrossWindowBlurEnabled == true
    }
}

/**
 * Blur & redup di belakang jendela popup menu.
 *
 * Dikerjakan oleh compositor sistem (FLAG_BLUR_BEHIND), bukan di aplikasi — nyaris
 * tanpa biaya CPU/GPU aplikasi, jadi menu tetap terbuka seketika di HP murah.
 *
 * Parameter jendela diambil dari root view popup (jendela yang benar-benar ditambahkan
 * ke WindowManager); view Compose di dalamnya bukan jendela, jadi layoutParams-nya
 * tidak berpengaruh apa-apa.
 */
@Composable
private fun TerapkanBlurBelakang(blur: Boolean, radiusBlurDp: Int, redup: Float) {
    val view = LocalView.current
    val radiusPx = with(LocalDensity.current) { radiusBlurDp.dp.roundToPx() }

    DisposableEffect(view, blur, radiusPx, redup) {
        fun terapkan() {
            try {
                val akar = view.rootView
                val wm = view.context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
                val params = akar.layoutParams as? WindowManager.LayoutParams ?: return
                params.flags = params.flags or WindowManager.LayoutParams.FLAG_DIM_BEHIND
                params.dimAmount = redup
                if (blur && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    params.flags = params.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
                    params.setBlurBehindRadius(radiusPx)
                }
                wm.updateViewLayout(akar, params)
            } catch (_: Throwable) {
                // Sebagian ROM vendor menolak perubahan parameter jendela popup; menu
                // tetap berfungsi, hanya tanpa blur.
            }
        }

        if (view.isAttachedToWindow) {
            terapkan()
            onDispose {}
        } else {
            val pendengar = object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) = terapkan()
                override fun onViewDetachedFromWindow(v: View) {}
            }
            view.addOnAttachStateChangeListener(pendengar)
            onDispose { view.removeOnAttachStateChangeListener(pendengar) }
        }
    }
}

/**
 * Pull-down menu bergaya iOS untuk seluruh Super App.
 *
 * Layar di belakangnya diburamkan & diredupkan tipis oleh sistem, menunya sendiri
 * berupa material putih tembus dengan sudut 14dp, bayangan lebar yang lembut, dan
 * garis pemisah hairline antar baris. Animasi buka/tutup memakai bawaan DropdownMenu
 * (skala + pudar dari titik jangkar) — ringan karena hanya transform & alpha.
 *
 * API tetap sama dengan versi sebelumnya, jadi semua pemakai ikut berubah tampilannya.
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
    val blur = blurLintasJendelaAktif()
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
                .widthIn(min = 230.dp, max = 300.dp)
                .shadow(
                    elevation = 24.dp,
                    shape = SukaDropdownShape,
                    ambientColor = Color.Black.copy(alpha = 0.08f),
                    spotColor = Color.Black.copy(alpha = 0.22f),
                )
                .clip(SukaDropdownShape)
                // Di atas latar yang sudah buram, putih 88% sudah terbaca sebagai
                // material kaca; tanpa blur, dibuat hampir pekat agar teks di
                // belakang tidak menembus.
                .background(Color.White.copy(alpha = if (blur) 0.88f else 0.98f))
                .border(0.5.dp, Color.Black.copy(alpha = 0.08f), SukaDropdownShape),
            offset = offset,
            properties = properties,
            content = {
                TerapkanBlurBelakang(blur = blur, radiusBlurDp = 22, redup = if (blur) 0.10f else 0.20f)
                Column(Modifier.padding(vertical = 2.dp)) { content() }
            },
        )
    }
}

/**
 * Satu baris menu bergaya iOS: teks 16sp di kiri, ikon di kanan (seperti menu iOS),
 * centang beraksen untuk pilihan yang aktif, sorot abu tipis saat ditekan.
 *
 * Parameter sama dengan versi sebelumnya; [leadingIcon] kini digambar di KANAN
 * mengikuti tata letak menu iOS, dan diganti centang bila [selected].
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
    val judul = title.ifEmpty { text }
    val interaksi = remember { MutableInteractionSource() }
    val ditekan by interaksi.collectIsPressedAsState()

    Row(
        modifier
            .fillMaxWidth()
            .background(if (ditekan) Color.Black.copy(alpha = 0.06f) else Color.Transparent)
            .clickable(interaksi, indication = null, role = Role.Button, onClick = onClick)
            .drawBehind {
                // Pemisah hairline di atas setiap baris. Baris pertama menempel di tepi
                // atas menu, jadi garisnya tertutup bingkai dan tidak terlihat ganda.
                drawLine(
                    WarnaIos.Pemisah,
                    Offset(0f, 0f),
                    Offset(size.width, 0f),
                    strokeWidth = 0.5.dp.toPx(),
                )
            }
            .heightIn(min = 44.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(10.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                judul,
                color = if (selected) WarnaIos.Aksen else WarnaIos.Label,
                fontSize = 16.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                letterSpacing = (-0.2).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    color = WarnaIos.LabelKedua,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (badgeText != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                badgeText,
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(WarnaIos.Aksen.copy(alpha = 0.12f))
                    .padding(horizontal = 7.dp, vertical = 2.dp),
                color = WarnaIos.Aksen,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        when {
            trailing != null -> {
                Spacer(Modifier.width(10.dp))
                trailing()
            }
            selected -> {
                Spacer(Modifier.width(10.dp))
                Icon(IkonIos.Check, "Terpilih", tint = WarnaIos.Aksen, modifier = Modifier.size(18.dp))
            }
            leadingIcon != null -> {
                Spacer(Modifier.width(10.dp))
                Icon(leadingIcon, null, tint = WarnaIos.Label, modifier = Modifier.size(19.dp))
            }
        }
    }
}

/**
 * Judul kecil di atas menu, seperti judul menu iOS: teks abu 13sp. Tombol tutup
 * bulat opsional tetap disediakan untuk layar yang memakainya.
 */
@Composable
fun SukaDropdownHeader(
    title: String,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 10.dp, top = 9.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title.lowercase().replaceFirstChar { it.titlecase() },
            Modifier.weight(1f),
            color = WarnaIos.LabelKedua,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (onClose != null) {
            Box(
                Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(WarnaIos.Isian)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Icon(IkonIos.Close, "Tutup", tint = WarnaIos.LabelKedua, modifier = Modifier.size(12.dp))
            }
        }
    }
}

/**
 * Pemicu dropdown bergaya iOS: label kecil abu di atas, kotak isian abu tanpa garis
 * tepi, nilai 16sp, dan chevron atas-bawah beraksen.
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
    Column(modifier) {
        if (label.isNotEmpty()) {
            Text(
                label,
                Modifier.padding(start = 4.dp, bottom = 6.dp),
                color = WarnaIos.LabelKedua,
                fontSize = 13.sp,
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (expanded) WarnaIos.Aksen.copy(alpha = 0.10f) else WarnaIos.Isian)
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) {
                Icon(leadingIcon, null, tint = WarnaIos.Aksen, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(
                value,
                Modifier.weight(1f),
                color = WarnaIos.Label,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(IkonIos.ArrowDropDown, null, tint = WarnaIos.Aksen, modifier = Modifier.size(16.dp))
        }
    }
}
