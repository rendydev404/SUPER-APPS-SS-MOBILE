package com.sukashawarma.superapp.feature.distribusi.ui.inbox

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.feature.distribusi.ui.KartuSuratJalan
import com.sukashawarma.superapp.feature.distribusi.ui.LayarGalat
import com.sukashawarma.superapp.feature.distribusi.ui.LayarKosong
import com.sukashawarma.superapp.feature.distribusi.ui.LayarMemuat
import com.sukashawarma.superapp.feature.distribusi.ui.SegarkanSaatAktif
import com.sukashawarma.superapp.presentation.theme.SukaGray500
import com.sukashawarma.superapp.presentation.theme.SukaOnSurface
import com.sukashawarma.superapp.presentation.theme.SukaSurface

@Composable
fun InboxScreen(
    onKeluar: () -> Unit,
    onBukaScan: () -> Unit,
    onBukaDetail: (String) -> Unit,
    viewModel: InboxViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            // Tombol pindai hanya untuk yang berhak memverifikasi. Pengawas
            // membuka layar ini untuk memantau, bukan untuk menerima barang.
            if (state.bolehVerifikasi) {
                ExtendedFloatingActionButton(
                    onClick = onBukaScan,
                    icon = { Icon(Icons.Default.QrCodeScanner, null) },
                    text = { Text("Pindai QR") },
                )
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().background(SukaSurface).padding(padding)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onKeluar) { Icon(Icons.Default.ArrowBack, "Kembali") }
                Column {
                    Text(
                        "Penerimaan Barang",
                        color = SukaOnSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(state.namaOutlet, color = SukaGray500, fontSize = 11.sp)
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { viewModel.muat(paksa = true) }) {
                    Icon(Icons.Default.Refresh, "Segarkan")
                }
            }

            when {
                state.memuat && state.daftar.isEmpty() -> LayarMemuat()
                state.error != null && state.daftar.isEmpty() ->
                    LayarGalat(state.error!!) { viewModel.muat(paksa = true) }
                state.daftar.isEmpty() -> LayarKosong(
                    "Belum Ada Kiriman Masuk",
                    "Surat jalan yang dikirim gudang pusat akan muncul di sini.",
                )
                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.daftar, key = { it.id }) { baris ->
                        // Menekan kartu membuka DETAIL, bukan verifikasi. Jalan
                        // menuju verifikasi hanya lewat pemindai QR — itulah
                        // gerbang integritas dokumen fisiknya.
                        KartuSuratJalan(baris = baris, onKlik = { onBukaDetail(baris.id) })
                    }
                }
            }
        }
    }
}
