package com.sukashawarma.superapp.core.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.navigation.NavBackStackEntry

/**
 * Transisi bersama seluruh NavHost aplikasi.
 *
 * Bawaan Navigation Compose adalah silang-pudar 700 ms — itulah kelambatan yang
 * terasa saat membuka modul dari Beranda. Diganti geseran murni tanpa pudar:
 * pudar membuat dua halaman saling tembus dan justru terbaca lambat, sedangkan
 * geseran memberi arah yang jelas dalam waktu seperempatnya.
 *
 * Kurvanya berangkat cepat lalu mengerem panjang, jadi pendek tanpa terasa kaku;
 * langkah mundur sengaja lebih singkat karena tujuannya sudah pernah dilihat.
 */
private val TEGAS = CubicBezierEasing(0.2f, 0f, 0f, 1f)

const val DURASI_MAJU_MS = 240
const val DURASI_MUNDUR_MS = 200

private typealias Transisi = AnimatedContentTransitionScope<NavBackStackEntry>

/** Halaman baru masuk dari kanan. */
fun Transisi.masukMaju(): EnterTransition =
    slideIntoContainer(SlideDirection.Start, tween(DURASI_MAJU_MS, easing = TEGAS))

/** Halaman lama terdorong ke kiri. */
fun Transisi.keluarMaju(): ExitTransition =
    slideOutOfContainer(SlideDirection.Start, tween(DURASI_MAJU_MS, easing = TEGAS))

/** Halaman sebelumnya kembali masuk dari kiri. */
fun Transisi.masukMundur(): EnterTransition =
    slideIntoContainer(SlideDirection.End, tween(DURASI_MUNDUR_MS, easing = TEGAS))

/** Halaman yang ditinggalkan keluar ke kanan. */
fun Transisi.keluarMundur(): ExitTransition =
    slideOutOfContainer(SlideDirection.End, tween(DURASI_MUNDUR_MS, easing = TEGAS))
