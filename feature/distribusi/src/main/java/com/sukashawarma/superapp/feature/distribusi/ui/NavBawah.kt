package com.sukashawarma.superapp.feature.distribusi.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sukashawarma.superapp.presentation.theme.SukaGray500
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
    Surface(color = Color.White, shadowElevation = 12.dp) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TombolNav("Dashboard", Icons.Default.Dashboard, aktif == TabBawah.DASHBOARD, onDashboard)
            if (bolehVerifikasi) {
                TombolNav("Scan QR", Icons.Default.QrCodeScanner, aktif == TabBawah.SCAN, onScan)
            }
            TombolNav("Riwayat", Icons.Default.History, aktif == TabBawah.RIWAYAT, onRiwayat)
        }
    }
}

@Composable
private fun TombolNav(teks: String, ikon: ImageVector, aktif: Boolean, onKlik: () -> Unit) {
    Surface(
        Modifier.clickable(onClick = onKlik),
        shape = RoundedCornerShape(14.dp),
        color = if (aktif) SukaOrange else Color.Transparent,
    ) {
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                ikon,
                null,
                tint = if (aktif) Color.White else SukaGray500,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.height(3.dp))
            Text(
                teks.uppercase(),
                color = if (aktif) Color.White else SukaGray500,
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.6.sp,
            )
        }
    }
}
