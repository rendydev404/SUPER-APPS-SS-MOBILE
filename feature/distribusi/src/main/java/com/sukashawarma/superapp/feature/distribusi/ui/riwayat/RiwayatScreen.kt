package com.sukashawarma.superapp.feature.distribusi.ui.riwayat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.sukashawarma.superapp.presentation.theme.SukaOnSurface
import com.sukashawarma.superapp.presentation.theme.SukaSurface

@Composable
fun RiwayatScreen(
    onKeluar: () -> Unit,
    onBukaDetail: (String) -> Unit,
    viewModel: RiwayatViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

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
    ) { padding ->
        Column(Modifier.fillMaxSize().background(SukaSurface).padding(padding)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onKeluar) { Icon(Icons.Default.ArrowBack, "Kembali") }
                Text(
                    "Riwayat Penerimaan",
                    color = SukaOnSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }

            when {
                state.memuat && state.daftar.isEmpty() -> LayarMemuat()
                state.error != null && state.daftar.isEmpty() ->
                    LayarGalat(state.error!!) { viewModel.muat(paksa = true) }
                state.daftar.isEmpty() -> LayarKosong(
                    "Belum Ada Riwayat",
                    "Penerimaan yang sudah diverifikasi dan ditandatangani akan tercatat di sini.",
                )
                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.daftar, key = { it.id }) { baris ->
                        KartuSuratJalan(baris = baris, onKlik = { onBukaDetail(baris.id) })
                    }
                }
            }
        }
    }
}
