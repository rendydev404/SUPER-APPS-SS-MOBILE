package com.sukashawarma.superapp.presentation.mitra

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sukashawarma.superapp.core.ui.ios.BarisIos
import com.sukashawarma.superapp.core.ui.ios.GrupIos
import com.sukashawarma.superapp.core.ui.ios.KartuIos
import com.sukashawarma.superapp.core.ui.ios.LencanaIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.PemisahIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.TombolKeduaIos
import com.sukashawarma.superapp.core.ui.ios.TombolUtamaIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.domain.session.AppSession

/**
 * Kerangka dashboard mitra. SENGAJA minimal — KPI, omzet, ROI, tren, orderan, transfer,
 * tim, dan saran adalah sub-proyek 1–5, bukan sub-proyek ini. Nilai layar ini: alur
 * login-lalu-redirect bisa dites di HP nyata sebelum satu pun angka dibangun.
 */
@Composable
fun MitraDashboardScaffold(onOpenProfil: () -> Unit, onLoggedOut: () -> Unit) {
    val staff by AppSession.staff.collectAsState()
    val profil by AppSession.mitraProfile.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarnaIos.Latar)
            .padding(horizontal = UkuranIos.TepiLayar, vertical = 20.dp)
    ) {
        Text(
            "Halo, ${profil?.namaMitra ?: "Mitra"}",
            style = TipeIos.Judul1,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            staff?.outletName ?: "Outlet belum diketahui",
            style = TipeIos.SubJudul,
        )
        Spacer(Modifier.height(10.dp))

        val aktif = profil?.isAktif == true
        LencanaIos(
            if (aktif) "Kemitraan aktif" else "Kemitraan tidak aktif",
            if (aktif) NadaIos.SUKSES else NadaIos.BAHAYA,
        )

        Spacer(Modifier.height(20.dp))
        KartuIos {
            Text("Dashboard mitra", style = TipeIos.Utama)
            Spacer(Modifier.height(4.dp))
            Text(
                "Ringkasan penjualan, bagi hasil, dan laporan outlet akan tampil di sini.",
                style = TipeIos.Catatan,
            )
        }

        Spacer(Modifier.weight(1f))
        // Mitra tidak punya modul Absensi maupun Beranda, jadi ini satu-satunya
        // jalan mereka ke halaman profil dan ganti password.
        GrupIos {
            BarisIos("Profil Saya", ikon = IkonIos.Person, onKlik = onOpenProfil)
            PemisahIos(inset = 58.dp)
            BarisIos(
                "Keluar",
                ikon = Icons.AutoMirrored.Filled.Logout,
                nadaIkon = NadaIos.BAHAYA,
                onKlik = { AppSession.signOut(); onLoggedOut() },
                chevron = false,
            )
        }
    }
}

@Composable
fun MitraNoProfileScreen(onLoggedOut: () -> Unit) {
    MitraMessageScreen(
        icon = Icons.Default.PersonOff,
        title = "Profil Mitra Belum Terdaftar",
        message = "Akun Anda belum dikaitkan dengan profil kemitraan. " +
            "Silakan hubungi admin pusat Suka Shawarma untuk proses aktivasi.",
        primaryLabel = null,
        onPrimary = null,
        onLoggedOut = onLoggedOut,
    )
}

@Composable
fun MitraLoadErrorScreen(onRetry: () -> Unit, onLoggedOut: () -> Unit) {
    // Dikoleksi di sini (bukan di MitraMessageScreen umum) karena hanya layar galat yang
    // punya tombol retry — layar "belum terdaftar" tak pernah retry.
    val retrying by AppSession.mitraRetrying.collectAsState()
    MitraMessageScreen(
        icon = Icons.Default.CloudOff,
        title = "Gagal Memuat Data Kemitraan",
        message = "Data profil Anda tidak bisa diambil saat ini. " +
            "Periksa koneksi internet, lalu coba lagi.",
        primaryLabel = "Coba Lagi",
        onPrimary = onRetry,
        primaryLoading = retrying,
        onLoggedOut = onLoggedOut,
    )
}

@Composable
private fun MitraMessageScreen(
    icon: ImageVector,
    title: String,
    message: String,
    primaryLabel: String?,
    onPrimary: (() -> Unit)?,
    onLoggedOut: () -> Unit,
    primaryLoading: Boolean = false,
) {
    Box(
        modifier = Modifier.fillMaxSize().background(WarnaIos.Latar),
        contentAlignment = Alignment.Center,
    ) {
        KartuIos(
            modifier = Modifier.padding(24.dp),
            padding = androidx.compose.foundation.layout.PaddingValues(24.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(WarnaIos.Aksen.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = WarnaIos.Aksen, modifier = Modifier.size(30.dp))
                }
                Spacer(Modifier.height(14.dp))
                Text(title, style = TipeIos.Utama, textAlign = TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                Text(message, style = TipeIos.SubJudul, textAlign = TextAlign.Center)
                Spacer(Modifier.height(20.dp))
                if (primaryLabel != null && onPrimary != null) {
                    TombolUtamaIos(primaryLabel, onPrimary, memuat = primaryLoading)
                    Spacer(Modifier.height(10.dp))
                }
                TombolKeduaIos(
                    "Keluar",
                    onKlik = { AppSession.signOut(); onLoggedOut() },
                    warna = WarnaIos.AbuGelap,
                )
            }
        }
    }
}
