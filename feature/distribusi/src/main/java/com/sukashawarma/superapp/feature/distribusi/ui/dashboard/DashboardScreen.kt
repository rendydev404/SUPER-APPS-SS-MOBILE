package com.sukashawarma.superapp.feature.distribusi.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.feature.distribusi.data.model.RentangTanggal
import com.sukashawarma.superapp.feature.distribusi.data.model.SuratJalanRingkas
import com.sukashawarma.superapp.feature.distribusi.domain.bolehDitutup
import com.sukashawarma.superapp.feature.distribusi.ui.KartuSuratJalan
import com.sukashawarma.superapp.feature.distribusi.ui.LayarGalat
import com.sukashawarma.superapp.feature.distribusi.ui.LayarKosong
import com.sukashawarma.superapp.feature.distribusi.ui.LayarMemuat
import com.sukashawarma.superapp.feature.distribusi.ui.SegarkanSaatAktif
import com.sukashawarma.superapp.presentation.theme.SukaGray500
import com.sukashawarma.superapp.presentation.theme.SukaOnSurface
import com.sukashawarma.superapp.presentation.theme.SukaOrange
import com.sukashawarma.superapp.presentation.theme.SukaSurface

@Composable
fun DashboardScreen(
    onKeluar: () -> Unit,
    onBukaInbox: () -> Unit,
    onBukaRiwayat: () -> Unit,
    onBukaDetail: (String) -> Unit,
    viewModel: DashboardViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    var konfirmasiTutup by remember { mutableStateOf<SuratJalanRingkas?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    SegarkanSaatAktif { viewModel.muat(paksa = true) }

    if (state.memuat && state.semua.isEmpty()) { LayarMemuat(); return }
    if (state.error != null && state.semua.isEmpty()) {
        LayarGalat(state.error!!) { viewModel.muat(paksa = true) }
        return
    }

    // Setelah daftar terisi, galat/pesan lewat snackbar — bukan LayarGalat layar
    // penuh, yang hanya dipakai untuk kegagalan pemuatan awal di atas.
    LaunchedEffect(state.pesan, state.error) {
        val teks = state.pesan ?: state.error
        if (teks != null) {
            snackbarHostState.showSnackbar(teks)
            viewModel.bersihkanPesan()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
    Column(Modifier.fillMaxSize().padding(paddingValues).background(SukaSurface)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onKeluar) { Icon(Icons.Default.ArrowBack, "Kembali") }
            Column(Modifier.weight(1f)) {
                Text("Distribusi", color = SukaOnSurface, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                Text(state.namaPengguna, color = SukaGray500, fontSize = 11.sp)
            }
            IconButton(onClick = { viewModel.muat(paksa = true) }) {
                Icon(Icons.Default.Refresh, "Segarkan")
            }
            IconButton(onClick = onBukaInbox) { Icon(Icons.Default.Inbox, "Inbox penerimaan") }
            IconButton(onClick = onBukaRiwayat) { Icon(Icons.Default.History, "Riwayat") }
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KartuAngka("Dikirim", state.hitungan.dikirim, Modifier.weight(1f))
                    KartuAngka("Diterima", state.hitungan.diterima, Modifier.weight(1f))
                    KartuAngka("Selesai", state.hitungan.selesai, Modifier.weight(1f))
                    KartuAngka("Akurasi", state.akurasi, Modifier.weight(1f), akhiran = "%")
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(RentangTanggal.entries) { rentang ->
                        Pil(rentang.label, state.rentang == rentang) { viewModel.ubahRentang(rentang) }
                    }
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(TabStatus.entries) { tab ->
                        Pil(tab.label, state.tab == tab) { viewModel.ubahTab(tab) }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = state.cari,
                    onValueChange = viewModel::ubahCari,
                    label = { Text("Cari nomor SJ atau outlet") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (state.rincianOutlet.size > 1) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(state.rincianOutlet) { outlet ->
                            Pil(
                                "${outlet.nama} (${outlet.total})",
                                state.outletTerpilih == outlet.nama,
                            ) { viewModel.pilihOutlet(outlet.nama) }
                        }
                    }
                }
            }

            if (state.terlihat.isEmpty()) {
                item {
                    Column(Modifier.fillMaxWidth().height(220.dp)) {
                        LayarKosong("Tidak Ada Surat Jalan", "Tidak ada yang cocok dengan filter ini.")
                    }
                }
            } else {
                items(state.terlihat, key = { it.id }) { baris ->
                    val bolehTutup = state.bolehTutupDokumen && baris.status?.bolehDitutup == true
                    val sedangDiproses = state.sedangMenutup == baris.id
                    KartuSuratJalan(
                        baris = baris,
                        aksiLabel = if (!bolehTutup) null else if (sedangDiproses) "Menutup..." else "Tutup Dokumen",
                        onKlik = { onBukaDetail(baris.id) },
                        onAksi = if (bolehTutup && !sedangDiproses) ({ konfirmasiTutup = baris }) else null,
                    )
                }
            }
        }
    }
    }

    konfirmasiTutup?.let { baris ->
        AlertDialog(
            onDismissRequest = { konfirmasiTutup = null },
            title = { Text("Tutup dokumen?") },
            text = {
                Text(
                    "Surat jalan ${baris.nomorDokumen ?: baris.id.take(8)} akan ditandai selesai " +
                        "dan tidak bisa dibuka kembali dari aplikasi."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.tutupDokumen(baris)
                    konfirmasiTutup = null
                }) { Text("Tutup Dokumen") }
            },
            dismissButton = {
                TextButton(onClick = { konfirmasiTutup = null }) { Text("Batal") }
            },
        )
    }
}

@Composable
private fun KartuAngka(label: String, nilai: Int, modifier: Modifier = Modifier, akhiran: String = "") {
    Surface(modifier, shape = RoundedCornerShape(14.dp), color = Color.White) {
        Column(Modifier.padding(10.dp)) {
            Text("$nilai$akhiran", color = SukaOnSurface, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(2.dp))
            Text(label, color = SukaGray500, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun Pil(teks: String, aktif: Boolean, onKlik: () -> Unit) {
    Surface(
        Modifier.clickable(onClick = onKlik),
        shape = RoundedCornerShape(50),
        color = if (aktif) SukaOrange else Color.White,
    ) {
        Text(
            teks,
            Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            color = if (aktif) Color.White else SukaOnSurface,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
