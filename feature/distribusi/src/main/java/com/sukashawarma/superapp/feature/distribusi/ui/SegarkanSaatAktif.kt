package com.sukashawarma.superapp.feature.distribusi.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/**
 * Memuat ulang setiap kali layar kembali ke depan.
 *
 * Pengganti langganan Realtime yang dipakai web: `:core:realtime` native masih
 * kosong, dan `ON_RESUME` menangkap kasus yang paling sering terjadi di
 * lapangan — crew berpindah ke aplikasi lain sebentar lalu kembali, atau supir
 * baru saja tiba sementara layar dibiarkan terbuka.
 *
 * `rememberUpdatedState` dipakai supaya pengamat yang sudah terdaftar tetap
 * memanggil lambda versi terbaru tanpa perlu mendaftar ulang tiap recomposition.
 */
@Composable
fun SegarkanSaatAktif(onSegarkan: () -> Unit) {
    val terkini by rememberUpdatedState(onSegarkan)
    val pemilik = LocalLifecycleOwner.current
    DisposableEffect(pemilik) {
        val pengamat = LifecycleEventObserver { _, peristiwa ->
            if (peristiwa == Lifecycle.Event.ON_RESUME) terkini()
        }
        pemilik.lifecycle.addObserver(pengamat)
        onDispose { pemilik.lifecycle.removeObserver(pengamat) }
    }
}
