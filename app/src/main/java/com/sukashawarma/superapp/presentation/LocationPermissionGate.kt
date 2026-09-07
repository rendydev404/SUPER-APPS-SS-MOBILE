package com.sukashawarma.superapp.presentation

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.sukashawarma.superapp.data.location.LocationTracking

/**
 * Pelacakan lokasi kerja tidak punya sakelar di dalam app: dialog izin Android ADALAH
 * persetujuannya. Begitu staff menekan "Izinkan", pelacakan menyala sendiri dan tetap
 * menyala setiap kali app dibuka; staff yang ingin mematikannya mencabut izin lokasi
 * lewat Setelan sistem, tempat Android sendiri menyediakan kontrolnya.
 *
 * Komposabel ini tidak menggambar apa pun — hanya efek samping. Dipasang di root supaya
 * berjalan di layar mana pun, dan hanya setelah sesi staff ada (tanpa `outlet_staff_id`
 * tidak ada tujuan pengiriman posisi).
 */
@Composable
fun LocationPermissionGate(signedIn: Boolean) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Satu kali per proses. Tanpa penjaga ini, setiap recomposition atau kembali dari
    // layar Setelan akan memicu permintaan izin lagi.
    val asked = remember { booleanArrayOf(false) }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        // Notifikasi ditolak tidak memblokir apa pun: service tetap wajib memasang
        // notifikasi foreground, sistem hanya menyembunyikannya dari staff.
        LocationTracking.start(context)
    }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (!granted) return@rememberLauncherForActivityResult
        LocationTracking.start(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !LocationTracking.hasNotificationPermission(context)
        ) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(signedIn) {
        if (!signedIn) return@LaunchedEffect
        if (LocationTracking.hasForegroundPermission(context)) {
            // Izin sudah ada: pelacakan menyala tanpa mengganggu staff sama sekali. Ini
            // juga jalur pemulihan setelah HP restart atau service dibunuh OEM — membuka
            // app adalah satu-satunya konteks yang PASTI boleh memulai foreground service
            // lokasi di semua versi Android.
            LocationTracking.start(context)
            return@LaunchedEffect
        }
        if (asked[0]) return@LaunchedEffect
        asked[0] = true
        locationLauncher.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
    }

    // Staff bisa memberi izin dari layar Setelan lalu kembali ke app; tanpa pemeriksaan
    // ulang saat resume, pelacakan baru menyala pada pembukaan app berikutnya.
    DisposableEffect(lifecycleOwner, signedIn) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && signedIn &&
                LocationTracking.hasForegroundPermission(context)
            ) {
                LocationTracking.start(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
