package com.sukashawarma.superapp.feature.distribusi.ui.riwayat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.feature.distribusi.ui.KartuSuratJalan
import com.sukashawarma.superapp.feature.distribusi.ui.LayarGalat
import com.sukashawarma.superapp.feature.distribusi.ui.LayarKosong
import com.sukashawarma.superapp.feature.distribusi.ui.LayarMemuat
import com.sukashawarma.superapp.feature.distribusi.ui.ShellDistribusi
import com.sukashawarma.superapp.feature.distribusi.ui.TabBawah
import com.sukashawarma.superapp.feature.distribusi.ui.SegarkanSaatAktif
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.ios.BilahJudulIos
import com.sukashawarma.superapp.core.ui.ios.TombolBundarIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.core.ui.RealtimeTables
import com.sukashawarma.superapp.core.ui.kaca.denganRuangNav
import com.sukashawarma.superapp.core.ui.kaca.navigationBarsPaddingKaca

@Composable
fun RiwayatScreen(
    onKeluar: () -> Unit,
    onBukaDetail: (String) -> Unit,
    onBukaDashboard: () -> Unit,
    onBukaScan: () -> Unit,
    onBukaBuat: () -> Unit = {},
    viewModel: RiwayatViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    RealtimeRefresh(RealtimeTables.SURAT_JALAN) { viewModel.muat(paksa = true) }
    val snackbarHostState = remember { SnackbarHostState() }

    SegarkanSaatAktif { viewModel.muat(paksa = true) }

    // Galat pemuatan pertama memakai LayarGalat layar penuh di bawah. Galat
    // yang muncul saat daftar sudah terisi (mis. penyegaran gagal) tidak
    // boleh diam-diam hilang — crew harus tahu datanya mungkin tidak mutakhir.
    LaunchedEffect(state.error) {
        val teks = state.error
        if (teks != null && state.daftar.isNotEmpty()) {
            snackbarHostState.showSnackbar(teks)
            viewModel.bersihkanPesan()
        }
    }

    ShellDistribusi(
        aktif = TabBawah.RIWAYAT,
        bolehVerifikasi = state.bolehVerifikasi,
        onDashboard = onBukaDashboard,
        onScan = onBukaScan,
        onRiwayat = {},
        bolehTerbitkan = state.pusat,
        onBuat = onBukaBuat,
    ) {
        Scaffold(
            containerColor = WarnaIos.Latar,
            snackbarHost = { SnackbarHost(snackbarHostState, Modifier.navigationBarsPaddingKaca()) },
        ) { padding ->
            Column(Modifier.fillMaxSize().background(WarnaIos.Latar).padding(padding)) {
                BilahJudulIos(
                    judul = if (state.pusat) "Semua Surat Jalan" else "Riwayat Penerimaan",
                    onKembali = onKeluar,
                ) {
                    TombolBundarIos(IkonIos.Refresh, "Segarkan", { viewModel.muat(paksa = true) })
                }

                when {
                    state.memuat && state.daftar.isEmpty() -> LayarMemuat()
                    state.error != null && state.daftar.isEmpty() ->
                        LayarGalat(state.error!!) { viewModel.muat(paksa = true) }
                    state.daftar.isEmpty() -> LayarKosong(
                        "Belum Ada Riwayat",
                        if (state.pusat) "Surat jalan yang diterbitkan Gudang Pusat akan tercatat di sini."
                        else "Penerimaan yang sudah diverifikasi dan ditandatangani akan tercatat di sini.",
                        ikon = IkonIos.History,
                    )
                    else -> LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = UkuranIos.TepiLayar,
                            end = UkuranIos.TepiLayar,
                            top = 12.dp,
                            bottom = 24.dp,
                        ).denganRuangNav(),
                        verticalArrangement = Arrangement.spacedBy(UkuranIos.JarakKartu),
                    ) {
                        items(state.daftar, key = { it.id }) { baris ->
                            KartuSuratJalan(baris = baris, onKlik = { onBukaDetail(baris.id) })
                        }
                    }
                }
            }
        }
    }
}
