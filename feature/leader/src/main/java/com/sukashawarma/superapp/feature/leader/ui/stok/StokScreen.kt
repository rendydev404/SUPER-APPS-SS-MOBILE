package com.sukashawarma.superapp.feature.leader.ui.stok

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.RealtimeTables
import com.sukashawarma.superapp.core.ui.ios.KartuIos
import com.sukashawarma.superapp.core.ui.ios.KolomCariIos
import com.sukashawarma.superapp.core.ui.ios.LencanaIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.PetakStatIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.core.ui.kaca.denganRuangNav
import com.sukashawarma.superapp.core.ui.kaca.navigationBarsPaddingKaca
import com.sukashawarma.superapp.feature.leader.domain.BahanCabang
import com.sukashawarma.superapp.feature.leader.domain.StatusStok
import com.sukashawarma.superapp.feature.leader.ui.BilahJudulLeader
import com.sukashawarma.superapp.feature.leader.ui.PanelKosong
import com.sukashawarma.superapp.feature.leader.ui.PemilihOutlet

/**
 * Stok Cabang — cermin `app/dashboard/leader/stock/page.tsx` web.
 *
 * Layar ini murni membaca, sama seperti web: tidak ada tombol "Minta Restock" yang
 * di sana memang tidak terhubung ke apa pun. Permintaan bahan sungguhan sudah punya
 * rumahnya sendiri di modul Stok native, dan menaruh tombol kedua yang tidak
 * mengirim apa-apa hanya akan membuat leader mengira permintaannya sudah masuk.
 *
 * Tampilannya meniru Dashboard Stok (`MonitoringScreen`) supaya leader yang membuka
 * kedua layar membaca kartu bahan yang sama — hanya bahasa visualnya yang ditiru,
 * bukan kodenya, karena kartu di sana bergantung pada model baris modul Stok.
 */
@Composable
fun StokScreen(
    onExit: () -> Unit,
    viewModel: StokViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    // Saldo bergerak tiap mutasi; dipicu oleh `stok_balance` pada publication realtime.
    RealtimeRefresh(RealtimeTables.STOK_BALANCE, jedaMinimumMs = 30_000L) { viewModel.muatUlang() }

    LaunchedEffect(state.galat) {
        state.galat?.let {
            snackbar.showSnackbar(it)
            viewModel.tutupKabar()
        }
    }

    Scaffold(
        containerColor = WarnaIos.Latar,
        snackbarHost = { SnackbarHost(snackbar, Modifier.navigationBarsPaddingKaca()) },
        topBar = { BilahJudulLeader("Stok Cabang", onExit, viewModel::muatUlang) },
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
            item(key = "cabang") {
                PemilihOutlet(state.cabang, state.outletTerpilih) { viewModel.pilihOutlet(it) }
            }
            item(key = "ringkas") { PanelRingkas(state) }
            item(key = "cari") {
                KolomCariIos(
                    state.pencarian,
                    { viewModel.cari(it) },
                    Modifier.fillMaxWidth().padding(top = 2.dp),
                    placeholder = "Cari bahan baku…",
                )
            }
            if (state.terlihat.isEmpty()) {
                item(key = "kosong") {
                    PanelKosong(
                        when {
                            state.memuat -> "Memuat stok cabang…"
                            state.pencarian.isNotBlank() -> "Bahan tidak ditemukan."
                            else -> "Belum ada data stok untuk cabang ini."
                        },
                        if (state.pencarian.isNotBlank()) IkonIos.Search else IkonIos.Inventory2,
                    )
                }
            }
            items(state.terlihat, key = { it.id }) { KartuBahan(it) }
        }
    }
}

/** Nada status versi iOS, disamakan dengan `StokStatus.nadaIos()` di Dashboard Stok. */
private fun StatusStok.nadaIos(): NadaIos = when (this) {
    StatusStok.KRITIS -> NadaIos.BAHAYA
    StatusStok.MENIPIS -> NadaIos.PERINGATAN
    StatusStok.AMAN -> NadaIos.SUKSES
}

@Composable
private fun PanelRingkas(state: StokUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Monitoring Stok", style = TipeIos.Judul3.copy(fontWeight = FontWeight.Bold))
                Text("${state.semua.size} bahan terpantau di cabang ini.", style = TipeIos.Catatan)
            }
            if (state.memuat) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = WarnaIos.Aksen)
            }
        }
        // Petak hanya informasi, tidak menyaring: layar web tidak punya filter status,
        // dan urutan daftar sudah menaruh yang paling genting di atas.
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PetakStatIos(
                label = "Kritis",
                nilai = state.jumlahKritis.toString(),
                ikon = IkonIos.WarningAmber,
                warna = WarnaIos.Merah,
                modifier = Modifier.weight(1f),
            )
            PetakStatIos(
                label = "Menipis",
                nilai = state.jumlahMenipis.toString(),
                ikon = IkonIos.HourglassEmpty,
                warna = WarnaIos.Oranye,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun KartuBahan(bahan: BahanCabang) {
    val nada = bahan.status.nadaIos()
    KartuIos {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(bahan.nama, style = TipeIos.Utama, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(3.dp))
                Text(
                    bahan.batasTeks?.let { "Batas minimal $it" } ?: "Batas minimal belum diatur",
                    style = TipeIos.Catatan,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(10.dp))
            LencanaIos(bahan.status.label, nada)
        }

        Spacer(Modifier.height(14.dp))

        // Blok abu seperti `BlokAngkaIos` di Dashboard Stok, tetapi satu kolom: saldo
        // sudah ditulis berjenjang dalam satu kalimat ("2 Kg 32 Gram"), dan warnanya
        // mengikuti tiga tingkat status — BlokAngkaIos hanya mengenal merah.
        Column(
            Modifier
                .fillMaxWidth()
                .clip(UkuranIos.SudutBlok)
                .background(WarnaIos.Latar)
                .padding(vertical = 10.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Sisa stok", style = TipeIos.Kecil.copy(fontWeight = FontWeight.Medium), maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Text(
                bahan.saldoTeks,
                style = TipeIos.Angka.copy(
                    color = if (bahan.status == StatusStok.AMAN) WarnaIos.Label else nada.teks,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
