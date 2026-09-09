package com.sukashawarma.superapp.feature.leader.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sukashawarma.superapp.presentation.theme.SukaGray400
import com.sukashawarma.superapp.presentation.theme.SukaOrange

/**
 * Tujuan navigasi modul Leader.
 *
 * Empat, persis seperti isi kelompok "Leader Dashboard" di `navConfig.ts` web —
 * dan karena tepat empat, seluruhnya muat di bilah bawah tanpa perlu lembar "Menu"
 * seperti modul Manager.
 *
 * `rute` disimpan di enum ini, bukan di daftar terpisah, supaya bilah navigasi dan
 * graph tidak bisa berselisih tentang nama rute.
 */
enum class TujuanLeader(
    val rute: String,
    val label: String,
    val labelPanjang: String,
    val ikon: ImageVector,
) {
    RINGKASAN("leader_ringkasan", "Overview", "Ringkasan Leader", Icons.Default.Dashboard),
    PETTY_CASH("leader_petty_cash", "Petty Cash", "Top Up Petty Cash", Icons.Default.Payments),
    PENJUALAN("leader_penjualan", "Penjualan", "Penjualan & Target", Icons.Default.TrackChanges),
    STOK("leader_stok", "Stok", "Stok Cabang", Icons.Default.Inventory2),
}

/**
 * Bilah navigasi bawah modul Leader.
 *
 * [jumlahAksi] adalah pengajuan yang menunggu diserahkan ke crew. Lencananya ada di
 * sini karena itu satu-satunya hal di modul ini yang menuntut tindakan leader dan
 * bisa datang kapan saja dari meja area manager — sisanya hanya pemantauan.
 */
@Composable
fun NavBawahLeader(
    aktif: TujuanLeader,
    jumlahAksi: Int,
    onPilih: (TujuanLeader) -> Unit,
) {
    Surface(
        color = Color.White,
        shadowElevation = 12.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top,
        ) {
            TujuanLeader.entries.forEach { tujuan ->
                TombolNav(
                    label = tujuan.label,
                    ikon = tujuan.ikon,
                    aktif = aktif == tujuan,
                    lencana = if (tujuan == TujuanLeader.PETTY_CASH) jumlahAksi else 0,
                    modifier = Modifier.weight(1f),
                ) { onPilih(tujuan) }
            }
        }
    }
}

@Composable
private fun TombolNav(
    label: String,
    ikon: ImageVector,
    aktif: Boolean,
    lencana: Int,
    modifier: Modifier = Modifier,
    onKlik: () -> Unit,
) {
    Column(
        modifier.clickable(onClick = onKlik).padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Box(
                Modifier
                    .size(38.dp)
                    .background(if (aktif) SukaOrange else Color.Transparent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    ikon,
                    null,
                    tint = if (aktif) Color.White else SukaGray400,
                    modifier = Modifier.size(19.dp),
                )
            }
            if (lencana > 0) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFDC2626),
                    border = BorderStroke(2.dp, Color.White),
                ) {
                    Text(
                        if (lencana > 9) "9+" else lencana.toString(),
                        Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            color = if (aktif) SukaOrange else SukaGray400,
            fontSize = 9.sp,
            fontWeight = if (aktif) FontWeight.Black else FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}
