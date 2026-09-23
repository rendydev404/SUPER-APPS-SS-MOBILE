package com.sukashawarma.superapp.core.ui.kaca

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.sukashawarma.superapp.presentation.theme.SukaSurface

/**
 * Penghitung lembar yang sedang meminta latar diburamkan.
 *
 * Hitungan, bukan boolean: dua lembar bisa bertumpuk sesaat (satu menutup sementara
 * yang lain membuka), dan boolean akan mematikan blur di tengah transisi itu.
 */
@Stable
class PengendaliBlurKaca internal constructor() {
    internal var jumlah by mutableIntStateOf(0)
}

/**
 * Pengendali blur milik [ShellKaca] terdekat; null di luar shell. [LembarMenuKaca]
 * memakainya untuk menyalakan blur latar tanpa perlu diatur oleh pemanggil.
 */
val LocalPengendaliBlurKaca = staticCompositionLocalOf<PengendaliBlurKaca?> { null }

/**
 * Kerangka layar modul dengan tab bar kaca mengambang.
 *
 * - [isi] digambar sampai dasar layar — terlihat terbias di balik kapsul — dan
 *   menerima [LocalRuangNavKaca] supaya daftarnya bisa memberi ruang di bawah lewat
 *   [denganRuangNav] / [navigationBarsPaddingKaca].
 * - [bilah] biasanya [BilahTabKaca]; ia menerima [LatarKaca] yang menghubungkannya
 *   dengan isi, dan diletakkan di dasar layar.
 * - Selama ada [LembarMenuKaca] terbuka di dalamnya, seluruh shell (isi + bilah)
 *   diburamkan tebal. Lembar itu hidup di jendelanya sendiri, jadi tidak ikut kabur.
 *
 * Tata letak isi bebas — pager, NavHost, atau `when` biasa; shell hanya mengurus
 * lapisan kaca dan blur di sekelilingnya.
 */
@Composable
fun ShellKaca(
    modifier: Modifier = Modifier,
    latarBelakang: Color = SukaSurface,
    tampilkanBilah: Boolean = true,
    bilah: @Composable (LatarKaca) -> Unit,
    isi: @Composable () -> Unit,
) {
    val latarKaca = remember { LatarKaca() }
    val pengendali = remember { PengendaliBlurKaca() }
    val ruangNav = if (tampilkanBilah) ruangBilahTabKaca() else 0.dp

    val blur by animateDpAsState(
        if (pengendali.jumlah > 0 && TokenKaca.blurDidukung) TokenKaca.BlurLembar else 0.dp,
        tween(durationMillis = 280),
        label = "blurShellKaca",
    )

    CompositionLocalProvider(LocalPengendaliBlurKaca provides pengendali) {
        Box(
            modifier
                .fillMaxSize()
                .graphicsLayer {
                    val r = blur.toPx()
                    renderEffect = if (r > 0f) BlurEffect(r, r, TileMode.Clamp) else null
                }
                .background(latarBelakang),
        ) {
            Box(Modifier.fillMaxSize().sumberKaca(latarKaca)) {
                CompositionLocalProvider(LocalRuangNavKaca provides ruangNav) {
                    isi()
                }
            }
            if (tampilkanBilah) {
                Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) { bilah(latarKaca) }
            }
        }
    }
}
