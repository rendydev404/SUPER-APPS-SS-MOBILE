package com.sukashawarma.superapp.feature.leader.ui.penjualan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.RealtimeTables
import com.sukashawarma.superapp.feature.leader.domain.PenjualanHariIni
import com.sukashawarma.superapp.feature.leader.domain.PesananTerbaru
import com.sukashawarma.superapp.feature.leader.domain.StatusPesanan
import com.sukashawarma.superapp.feature.leader.domain.rupiah
import com.sukashawarma.superapp.feature.leader.ui.BarProgres
import com.sukashawarma.superapp.feature.leader.ui.BiruGaris
import com.sukashawarma.superapp.feature.leader.ui.BiruLatar
import com.sukashawarma.superapp.feature.leader.ui.BiruTeks
import com.sukashawarma.superapp.feature.leader.ui.GarisKartu
import com.sukashawarma.superapp.feature.leader.ui.HijauGaris
import com.sukashawarma.superapp.feature.leader.ui.HijauLatar
import com.sukashawarma.superapp.feature.leader.ui.HijauTeks
import com.sukashawarma.superapp.feature.leader.ui.KartuPanel
import com.sukashawarma.superapp.feature.leader.ui.MerahGaris
import com.sukashawarma.superapp.feature.leader.ui.MerahLatar
import com.sukashawarma.superapp.feature.leader.ui.MerahTeks
import com.sukashawarma.superapp.feature.leader.ui.PanelKosong
import com.sukashawarma.superapp.feature.leader.ui.PemilihOutlet
import com.sukashawarma.superapp.feature.leader.ui.PilStatus
import com.sukashawarma.superapp.presentation.theme.SukaBrown
import com.sukashawarma.superapp.presentation.theme.SukaCream
import com.sukashawarma.superapp.presentation.theme.SukaGray400
import com.sukashawarma.superapp.presentation.theme.SukaOrange

/**
 * Penjualan & Target — cermin `app/dashboard/leader/sales/page.tsx` web.
 *
 * Daftar transaksinya memuat SEMUA status, bukan hanya yang selesai: layar ini
 * dipakai memantau antrean yang sedang berjalan, sedangkan angka omzet di atasnya
 * tetap hanya menghitung pesanan `completed`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PenjualanScreen(
    onExit: () -> Unit,
    viewModel: PenjualanViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    // `orders` dan `order_items` sama-sama ada di publication realtime, jadi pesanan
    // baru maupun perubahan isinya sama-sama menggerakkan layar ini.
    RealtimeRefresh(RealtimeTables.ORDERS, RealtimeTables.ORDER_ITEMS) { viewModel.muatUlang() }

    LaunchedEffect(state.galat) {
        state.galat?.let {
            snackbar.showSnackbar(it)
            viewModel.tutupKabar()
        }
    }

    Scaffold(
        containerColor = SukaCream,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Penjualan & Target",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = SukaBrown,
                        maxLines = 1,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = SukaBrown)
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::muatUlang) {
                        Icon(Icons.Default.Refresh, "Muat ulang", tint = SukaBrown)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                PemilihOutlet(state.cabang, state.outletTerpilih) { viewModel.pilihOutlet(it) }
            }
            item { PanelOmzet(state) }
            item {
                Text(
                    "TRANSAKSI TERAKHIR HARI INI",
                    color = SukaBrown,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp,
                )
            }
            if (state.data.pesanan.isEmpty()) {
                item {
                    KartuPanel {
                        PanelKosong(
                            if (state.memuat) "Memuat transaksi…" else "Belum ada transaksi hari ini."
                        )
                    }
                }
            }
            items(state.data.pesanan, key = { it.id }) { KartuPesanan(it) }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun PanelOmzet(state: PenjualanUiState) {
    val data = state.data
    KartuPanel {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    "TOTAL PENJUALAN HARI INI",
                    color = SukaGray400,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.7.sp,
                )
                state.namaCabang?.let {
                    Spacer(Modifier.height(2.dp))
                    Text(it, color = SukaGray400, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (state.memuat) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = SukaOrange)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            rupiah(data.omzet),
            color = SukaBrown,
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        // if/else, bukan keluar-awal: `return@KartuPanel` dari lambda inline yang
        // memancarkan composable adalah cara paling andal merusak tabel slot Compose.
        if (!data.adaTarget) {
            // Target nol berarti snapshot harian belum ditulis cron, BUKAN target nol
            // yang sudah tercapai. Menggambar bilah penuh di sini akan menipu.
            Spacer(Modifier.height(12.dp))
            Text(
                "Target harian belum ditentukan untuk cabang ini.",
                color = SukaGray400,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        } else {
            Spacer(Modifier.height(10.dp))
            PilStatus(
                teks = if (data.tercapai) "Target tercapai" else "On track",
                latar = if (data.tercapai) HijauLatar else BiruLatar,
                garis = if (data.tercapai) HijauGaris else BiruGaris,
                warnaTeks = if (data.tercapai) HijauTeks else BiruTeks,
                ikon = Icons.Default.TrendingUp,
            )
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth()) {
                Text(
                    "Progres pencapaian",
                    Modifier.weight(1f),
                    color = SukaGray400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Target ${rupiah(data.target)}",
                    color = SukaBrown,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                )
            }
            Spacer(Modifier.height(8.dp))
            BarProgres(data.rasio, data.tercapai)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth()) {
                Text(
                    "0%",
                    Modifier.weight(1f),
                    color = SukaGray400,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "${data.persenTeks}%",
                    color = SukaBrown,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}

@Composable
private fun KartuPesanan(pesanan: PesananTerbaru) {
    KartuPanel {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SukaCream,
                border = BorderStroke(1.dp, GarisKartu),
            ) {
                Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) {
                    Text(
                        pesanan.jam,
                        color = SukaBrown,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        pesanan.nomor?.let { "#$it" } ?: "Pesanan",
                        color = SukaBrown,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Spacer(Modifier.width(8.dp))
                    BadgeStatusPesanan(pesanan.status)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    pesanan.ringkasanItem,
                    color = SukaGray400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp,
                )
                if (pesanan.promo.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocalOffer,
                            null,
                            tint = SukaOrange,
                            modifier = Modifier.size(12.dp),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            pesanan.promo.joinToString(", "),
                            color = SukaOrange,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(
                rupiah(pesanan.total),
                color = SukaBrown,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun BadgeStatusPesanan(status: StatusPesanan) {
    val (latar, garis, teks) = when (status) {
        StatusPesanan.SELESAI -> Triple(HijauLatar, HijauGaris, HijauTeks)
        StatusPesanan.DIBATALKAN -> Triple(MerahLatar, MerahGaris, MerahTeks)
        StatusPesanan.PROSES -> Triple(BiruLatar, BiruGaris, BiruTeks)
    }
    PilStatus(status.label, latar, garis, teks)
}
