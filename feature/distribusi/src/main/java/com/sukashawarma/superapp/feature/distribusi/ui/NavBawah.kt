package com.sukashawarma.superapp.feature.distribusi.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sukashawarma.superapp.presentation.theme.SukaBrown
import com.sukashawarma.superapp.presentation.theme.SukaInk
import com.sukashawarma.superapp.presentation.theme.SukaOrange

enum class TabBawah { DASHBOARD, SCAN, RIWAYAT }

/**
 * Nav bawah tiga tombol, cermin `BottomNav.tsx` varian outlet di web.
 *
 * Tombol tengah hanya muncul untuk role yang berhak memverifikasi. Pengawas
 * membuka modul ini untuk memantau, bukan menerima barang, jadi menampilkan
 * pintu pindai kepada mereka hanya akan menyesatkan.
 */
@Composable
fun NavBawah(
    aktif: TabBawah,
    bolehVerifikasi: Boolean,
    onDashboard: () -> Unit,
    onScan: () -> Unit,
    onRiwayat: () -> Unit,
) {
    Box(Modifier.fillMaxWidth().height(84.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = SukaBrown,
            shadowElevation = 12.dp,
        ) {}

        if (bolehVerifikasi) {
            Row(
                Modifier.fillMaxSize().padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    TombolNav(
                        "Dashboard",
                        Icons.Default.Dashboard,
                        aktif == TabBawah.DASHBOARD,
                        onDashboard,
                    )
                }
                Box(
                    Modifier.width(96.dp).fillMaxHeight(),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Text(
                        "SCAN QR",
                        Modifier.padding(bottom = 10.dp),
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp,
                    )
                }
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    TombolNav(
                        "Riwayat",
                        Icons.Default.History,
                        aktif == TabBawah.RIWAYAT,
                        onRiwayat,
                    )
                }
            }

            TombolScanMengambang(
                aktif = aktif == TabBawah.SCAN,
                onKlik = onScan,
                modifier = Modifier.align(Alignment.TopCenter).offset(y = (-29).dp),
            )
        } else {
            Row(
                Modifier.fillMaxSize().padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TombolNav(
                    "Dashboard",
                    Icons.Default.Dashboard,
                    aktif == TabBawah.DASHBOARD,
                    onDashboard,
                )
                TombolNav(
                    "Riwayat",
                    Icons.Default.History,
                    aktif == TabBawah.RIWAYAT,
                    onRiwayat,
                )
            }
        }
    }
}

@Composable
private fun TombolNav(teks: String, ikon: ImageVector, aktif: Boolean, onKlik: () -> Unit) {
    Surface(
        Modifier.clickable(onClick = onKlik, role = Role.Tab),
        shape = RoundedCornerShape(14.dp),
        color = if (aktif) SukaOrange else Color.Transparent,
    ) {
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                ikon,
                null,
                tint = Color.White.copy(alpha = if (aktif) 1f else 0.72f),
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.height(3.dp))
            Text(
                teks.uppercase(),
                color = Color.White.copy(alpha = if (aktif) 1f else 0.72f),
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.6.sp,
            )
        }
    }
}

/**
 * Pintu utama verifikasi dibuat seperti FAB di aplikasi m-banking: lebih
 * besar, selalu terlihat, dan sedikit keluar dari permukaan nav agar mudah
 * ditemukan tanpa mengorbankan dua tab pendamping.
 */
@Composable
private fun TombolScanMengambang(
    aktif: Boolean,
    onKlik: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .size(66.dp)
            .clickable(onClick = onKlik, role = Role.Button),
        shape = CircleShape,
        color = if (aktif) SukaOrange else SukaInk,
        shadowElevation = 12.dp,
        border = androidx.compose.foundation.BorderStroke(
            width = 3.dp,
            color = Color.White,
        ),
    ) {
        Column(
            Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                Icons.Default.QrCodeScanner,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(29.dp),
            )
        }
    }
}
