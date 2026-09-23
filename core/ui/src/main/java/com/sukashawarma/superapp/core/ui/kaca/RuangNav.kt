package com.sukashawarma.superapp.core.ui.kaca

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Tinggi yang ditutupi kapsul [BilahTabKaca] di bawah layar, termasuk bilah navigasi
 * sistem.
 *
 * Di dalam [ShellKaca], isi layar digambar sampai ke dasar layar supaya terlihat di
 * balik kaca, jadi setiap layar yang ingin baris terakhirnya tetap terjangkau harus
 * menambahkan ruang ini sendiri — lewat [denganRuangNav] untuk daftar dan
 * [navigationBarsPaddingKaca] untuk bilah tombol yang dipatok di dasar layar.
 *
 * Di luar shell nilainya nol dan kedua helper kembali ke perilaku biasa, jadi layar
 * yang juga dibuka sebagai rute terpisah tidak terpengaruh.
 */
val LocalRuangNavKaca = compositionLocalOf { 0.dp }

/** Padding daftar ditambah ruang kapsul tab, supaya baris terakhir bisa digulir keluar dari balik kaca. */
@Composable
fun PaddingValues.denganRuangNav(): PaddingValues {
    val ruang = LocalRuangNavKaca.current
    if (ruang == 0.dp) return this
    val arah = LocalLayoutDirection.current
    return PaddingValues(
        start = calculateStartPadding(arah),
        top = calculateTopPadding(),
        end = calculateEndPadding(arah),
        bottom = calculateBottomPadding() + ruang,
    )
}

/**
 * Pengganti `navigationBarsPadding()` untuk bilah tombol yang dipatok di dasar layar:
 * di dalam shell kaca, bilahnya naik di atas kapsul tab; di luar shell, sama seperti
 * biasa.
 *
 * Jangan dipakai di dalam ModalBottomSheet — lembar menutupi kapsul, jadi ruangnya
 * hanya akan jadi celah kosong.
 */
fun Modifier.navigationBarsPaddingKaca(): Modifier = composed {
    val ruang: Dp = LocalRuangNavKaca.current
    if (ruang > 0.dp) padding(bottom = ruang) else navigationBarsPadding()
}
