package com.sukashawarma.superapp.feature.stok.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sukashawarma.superapp.feature.stok.ui.ledger.LedgerScreen
import com.sukashawarma.superapp.feature.stok.ui.monitoring.MonitoringScreen
import com.sukashawarma.superapp.feature.stok.ui.mutasi.MutasiScreen
import com.sukashawarma.superapp.feature.stok.ui.opname.OpnameScreen
import com.sukashawarma.superapp.feature.stok.ui.permintaan.PermintaanScreen
import com.sukashawarma.superapp.presentation.theme.SukaOnSurface
import com.sukashawarma.superapp.presentation.theme.SukaOnSurfaceVariant
import com.sukashawarma.superapp.presentation.theme.SukaSurface

/**
 * Tab utama modul Stok — cermin `primaryTabs` di `BottomNav.tsx` web, dengan susunan
 * yang sama supaya orang tidak perlu menghafal dua peta navigasi berbeda.
 */
private enum class TabStok(val label: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard),
    PERMINTAAN("Permintaan", Icons.Default.Assignment),
    OPNAME("Opname", Icons.Default.Description),
    LEDGER("Ledger", Icons.Default.MenuBook),
    MUTASI("Mutasi", Icons.Default.SwapHoriz),
}

@Composable
fun StokShell(
    onKeluar: () -> Unit,
    onBukaBahan: (outletId: String, bahanId: String, nama: String) -> Unit,
    onBukaProduksi: (outletId: String) -> Unit,
    onBukaTransfer: () -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(TabStok.DASHBOARD) }

    Column(Modifier.fillMaxSize().background(SukaSurface)) {
        Box(Modifier.weight(1f)) {
            when (tab) {
                TabStok.DASHBOARD -> MonitoringScreen(
                    onKeluar = onKeluar,
                    onBukaBahan = onBukaBahan,
                    onBukaProduksi = onBukaProduksi,
                    onBukaTransfer = onBukaTransfer,
                )
                TabStok.PERMINTAAN -> PermintaanScreen()
                TabStok.OPNAME -> OpnameScreen()
                TabStok.LEDGER -> LedgerScreen()
                TabStok.MUTASI -> MutasiScreen()
            }
        }
        BottomNavStok(tab) { tab = it }
    }
}

@Composable
private fun BottomNavStok(aktif: TabStok, onPilih: (TabStok) -> Unit) {
    Column {
        HorizontalDivider(color = Color(0xFFF1F5F9))
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color.White)
                .navigationBarsPadding()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            TabStok.entries.forEach { t ->
                val terpilih = t == aktif
                val warna = if (terpilih) Color(0xFFEA580C) else Color(0xFF94A3B8)
                Column(
                    Modifier
                        .heightIn(min = 48.dp)
                        .clickable { onPilih(t) }
                        .padding(horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(t.icon, t.label, tint = warna, modifier = Modifier.size(21.dp))
                    Spacer(Modifier.height(3.dp))
                    Text(
                        t.label,
                        color = warna,
                        fontSize = 10.sp,
                        fontWeight = if (terpilih) FontWeight.Black else FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
