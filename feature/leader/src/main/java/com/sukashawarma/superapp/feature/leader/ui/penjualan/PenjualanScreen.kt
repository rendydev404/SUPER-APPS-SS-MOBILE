package com.sukashawarma.superapp.feature.leader.ui.penjualan

import androidx.compose.foundation.background
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.RealtimeTables
import com.sukashawarma.superapp.core.ui.ios.JudulSeksiIos
import com.sukashawarma.superapp.core.ui.ios.KartuIos
import com.sukashawarma.superapp.core.ui.ios.LencanaIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.core.ui.kaca.denganRuangNav
import com.sukashawarma.superapp.core.ui.kaca.navigationBarsPaddingKaca
import com.sukashawarma.superapp.feature.leader.domain.PesananTerbaru
import com.sukashawarma.superapp.feature.leader.domain.StatusPesanan
import com.sukashawarma.superapp.feature.leader.domain.rupiah
import com.sukashawarma.superapp.feature.leader.ui.BarProgres
import com.sukashawarma.superapp.feature.leader.ui.BilahJudulLeader
import com.sukashawarma.superapp.feature.leader.ui.PanelKosong
import com.sukashawarma.superapp.feature.leader.ui.PemilihOutlet

/**
 * Penjualan & Target — cermin `app/dashboard/leader/sales/page.tsx` web.
 *
 * Daftar transaksinya memuat SEMUA status, bukan hanya yang selesai: layar ini
 * dipakai memantau antrean yang sedang berjalan, sedangkan angka omzet di atasnya
 * tetap hanya menghitung pesanan `completed`.
 */
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
        containerColor = WarnaIos.Latar,
        snackbarHost = { SnackbarHost(snackbar, Modifier.navigationBarsPaddingKaca()) },
        topBar = { BilahJudulLeader("Penjualan & Target", onExit, viewModel::muatUlang) },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(
                start = UkuranIos.TepiLayar,
                end = UkuranIos.TepiLayar,
                top = 12.dp,
                bottom = 16.dp,
            ).denganRuangNav(),
            verticalArrangement = Arrangement.spacedBy(UkuranIos.JarakKartu),
        ) {
            item {
                PemilihOutlet(state.cabang, state.outletTerpilih) { viewModel.pilihOutlet(it) }
            }
            item { PanelOmzet(state) }
            item { JudulSeksiIos("Transaksi Terakhir Hari Ini") }
            if (state.data.pesanan.isEmpty()) {
                item {
                    PanelKosong(
                        if (state.memuat) "Memuat transaksi…" else "Belum ada transaksi hari ini.",
                        IkonIos.Receipt,
                    )
                }
            }
            items(state.data.pesanan, key = { it.id }) { KartuPesanan(it) }
        }
    }
}

@Composable
private fun PanelOmzet(state: PenjualanUiState) {
    val data = state.data
    KartuIos {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Total Penjualan Hari Ini",
                    style = TipeIos.SubJudul.copy(fontWeight = FontWeight.SemiBold),
                )
                state.namaCabang?.let {
                    Spacer(Modifier.height(1.dp))
                    Text(it, style = TipeIos.Catatan, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (state.memuat) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = WarnaIos.Aksen)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            rupiah(data.omzet),
            style = TipeIos.AngkaBesar.copy(fontSize = 32.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        // if/else, bukan keluar-awal: `return@KartuIos` dari lambda inline yang
        // memancarkan composable adalah cara paling andal merusak tabel slot Compose.
        if (!data.adaTarget) {
            // Target nol berarti snapshot harian belum ditulis cron, BUKAN target nol
            // yang sudah tercapai. Menggambar bilah penuh di sini akan menipu.
            Spacer(Modifier.height(10.dp))
            Text("Target harian belum ditentukan untuk cabang ini.", style = TipeIos.Catatan)
        } else {
            Spacer(Modifier.height(10.dp))
            LencanaIos(
                teks = if (data.tercapai) "Target tercapai" else "On track",
                nada = if (data.tercapai) NadaIos.SUKSES else NadaIos.INFO,
                ikon = IkonIos.TrendingUp,
            )
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Text("Progres pencapaian", Modifier.weight(1f), style = TipeIos.Catatan)
                Text(
                    "Target ${rupiah(data.target)}",
                    style = TipeIos.Catatan.copy(color = WarnaIos.Label, fontWeight = FontWeight.SemiBold),
                )
            }
            Spacer(Modifier.height(8.dp))
            BarProgres(data.rasio, data.tercapai)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth()) {
                Text("0%", Modifier.weight(1f), style = TipeIos.Kecil)
                Text(
                    "${data.persenTeks}%",
                    style = TipeIos.Kecil.copy(color = WarnaIos.Label, fontWeight = FontWeight.SemiBold),
                )
            }
        }
    }
}

@Composable
private fun KartuPesanan(pesanan: PesananTerbaru) {
    KartuIos {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Box(
                Modifier.size(48.dp).clip(UkuranIos.SudutBlok).background(WarnaIos.Latar),
                contentAlignment = Alignment.Center,
            ) {
                Text(pesanan.jam, style = TipeIos.Catatan.copy(color = WarnaIos.Label, fontWeight = FontWeight.SemiBold))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(pesanan.nomor?.let { "#$it" } ?: "Pesanan", style = TipeIos.Utama)
                    Spacer(Modifier.width(8.dp))
                    BadgeStatusPesanan(pesanan.status)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    pesanan.ringkasanItem,
                    style = TipeIos.Catatan,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp,
                )
                if (pesanan.promo.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(IkonIos.Sell, null, tint = WarnaIos.Aksen, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(
                            pesanan.promo.joinToString(", "),
                            style = TipeIos.Kecil.copy(color = NadaIos.AKSEN.teks, fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(
                rupiah(pesanan.total),
                style = TipeIos.Keterangan.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun BadgeStatusPesanan(status: StatusPesanan) {
    val nada = when (status) {
        StatusPesanan.SELESAI -> NadaIos.SUKSES
        StatusPesanan.DIBATALKAN -> NadaIos.BAHAYA
        StatusPesanan.PROSES -> NadaIos.INFO
    }
    LencanaIos(status.label, nada)
}
