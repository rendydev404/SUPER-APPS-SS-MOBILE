package com.sukashawarma.superapp.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Meminta izin notifikasi untuk SETIAP pengguna yang masuk, terlepas dari izin
 * lokasi.
 *
 * Sebelumnya `POST_NOTIFICATIONS` hanya diminta di dalam alur izin lokasi, dan
 * hanya setelah lokasi DISETUJUI (lihat `LocationPermissionGate`). Siapa pun
 * yang menolak lokasi karena itu tidak pernah ditanyai soal notifikasi, dan
 * setiap notifikasi — termasuk pesan chat — dibuang diam-diam oleh pemeriksaan
 * izin di `NotificationManagerCompat`. Gejalanya terlihat seperti "push tidak
 * jalan" padahal server sudah mengirim dengan benar.
 *
 * Notifikasi kini punya gerbangnya sendiri karena ia memang kebutuhan yang
 * berdiri sendiri: chat berguna bagi semua orang, pelacakan lokasi tidak.
 *
 * Tidak menggambar apa pun — hanya efek samping, dipasang di root.
 */
@Composable
fun IzinNotifikasiGate(signedIn: Boolean) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = LocalContext.current
    // Satu kali per proses. Android sendiri berhenti menampilkan dialognya
    // setelah pengguna menolak dua kali, jadi meminta berulang hanya membuang
    // recomposition tanpa pernah muncul lagi.
    val sudahDiminta = remember { booleanArrayOf(false) }

    val peminta = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Ditolak tidak memblokir apa pun; layar chat yang mengingatkan. */ }

    LaunchedEffect(signedIn) {
        if (!signedIn || sudahDiminta[0]) return@LaunchedEffect
        val punya = ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (punya) return@LaunchedEffect
        sudahDiminta[0] = true
        peminta.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
