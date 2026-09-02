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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.presentation.theme.StatusEmerald
import com.sukashawarma.superapp.presentation.theme.StatusRed
import com.sukashawarma.superapp.presentation.theme.SukaOnSurface
import com.sukashawarma.superapp.presentation.theme.SukaOnSurfaceVariant
import com.sukashawarma.superapp.presentation.theme.SukaOrange
import com.sukashawarma.superapp.presentation.theme.SukaSurface
import com.sukashawarma.superapp.presentation.theme.SukaSurfaceContainerLowest

/**
 * Kerangka dashboard mitra. SENGAJA minimal — KPI, omzet, ROI, tren, orderan, transfer,
 * tim, dan saran adalah sub-proyek 1–5, bukan sub-proyek ini. Nilai layar ini: alur
 * login-lalu-redirect bisa dites di HP nyata sebelum satu pun angka dibangun.
 */
@Composable
fun MitraDashboardScaffold(onLoggedOut: () -> Unit) {
    val staff by AppSession.staff.collectAsState()
    val profil by AppSession.mitraProfile.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SukaSurface)
            .padding(20.dp)
    ) {
        Text(
            "Halo, ${profil?.namaMitra ?: "Mitra"}",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = SukaOnSurface,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            staff?.outletName ?: "Outlet belum diketahui",
            fontSize = 14.sp,
            color = SukaOnSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))

        val aktif = profil?.isAktif == true
        Text(
            if (aktif) "Kemitraan aktif" else "Kemitraan tidak aktif",
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (aktif) StatusEmerald else StatusRed)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )

        Spacer(Modifier.height(28.dp))
        Text(
            "Ringkasan penjualan, bagi hasil, dan laporan outlet akan tampil di sini.",
            fontSize = 14.sp,
            color = SukaOnSurfaceVariant,
        )

        Spacer(Modifier.weight(1f))
        TextButton(onClick = { AppSession.signOut(); onLoggedOut() }) {
            Text("Keluar", color = SukaOrange, fontWeight = FontWeight.Bold)
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
        modifier = Modifier.fillMaxSize().background(SukaSurface),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(SukaSurfaceContainerLowest)
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(SukaOrange.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = SukaOrange, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(
                title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = SukaOnSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                message,
                fontSize = 14.sp,
                color = SukaOnSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            if (primaryLabel != null && onPrimary != null) {
                Button(
                    onClick = onPrimary,
                    enabled = !primaryLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SukaOrange,
                        disabledContainerColor = SukaOrange.copy(alpha = 0.5f),
                        disabledContentColor = Color.White,
                    ),
                ) {
                    if (primaryLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(primaryLabel, color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(4.dp))
            }
            TextButton(onClick = { AppSession.signOut(); onLoggedOut() }) {
                Text("Keluar", color = SukaOnSurfaceVariant)
            }
        }
    }
}
