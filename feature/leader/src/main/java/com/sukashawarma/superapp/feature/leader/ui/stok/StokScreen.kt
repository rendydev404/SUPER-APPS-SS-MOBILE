package com.sukashawarma.superapp.feature.leader.ui.stok

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
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
import com.sukashawarma.superapp.feature.leader.domain.BahanCabang
import com.sukashawarma.superapp.feature.leader.domain.StatusStok
import com.sukashawarma.superapp.feature.leader.ui.AmberGaris
import com.sukashawarma.superapp.feature.leader.ui.AmberLatar
import com.sukashawarma.superapp.feature.leader.ui.AmberTeks
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
 * Stok Cabang — cermin `app/dashboard/leader/stock/page.tsx` web.
 *
 * Layar ini murni membaca, sama seperti web: tidak ada tombol "Minta Restock" yang
 * di sana memang tidak terhubung ke apa pun. Permintaan bahan sungguhan sudah punya
 * rumahnya sendiri di modul Stok native, dan menaruh tombol kedua yang tidak
 * mengirim apa-apa hanya akan membuat leader mengira permintaannya sudah masuk.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StokScreen(
    onExit: () -> Unit,
    viewModel: StokViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    // Saldo bergerak tiap mutasi; `stok_balance` dan `ledger_stok` sama-sama ada di
    // publication realtime, sedangkan view yang dibaca layar ini berdiri di atasnya.
    RealtimeRefresh(RealtimeTables.STOK_BALANCE, RealtimeTables.LEDGER) { viewModel.muatUlang() }

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
                        "Stok Cabang",
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
            item { PanelRingkas(state) }
            item { KotakCari(state.pencarian) { viewModel.cari(it) } }
            if (state.terlihat.isEmpty()) {
                item {
                    KartuPanel {
                        PanelKosong(
                            when {
                                state.memuat -> "Memuat stok cabang…"
                                state.pencarian.isNotBlank() -> "Bahan tidak ditemukan."
                                else -> "Belum ada data stok untuk cabang ini."
                            }
                        )
                    }
                }
            }
            items(state.terlihat, key = { it.id }) { KartuBahan(it) }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun PanelRingkas(state: StokUiState) {
    KartuPanel {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "MONITORING STOK",
                    color = SukaBrown,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "${state.semua.size} bahan terpantau di cabang ini.",
                    color = SukaGray400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            if (state.memuat) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = SukaOrange)
            }
        }
        // Pil ringkasan hanya digambar saat ada yang perlu ditindak — cabang yang
        // seluruh stoknya aman tidak perlu dihiasi lencana bertuliskan nol.
        if (state.jumlahKritis > 0 || state.jumlahMenipis > 0) {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.jumlahKritis > 0) {
                    PilStatus(
                        "${state.jumlahKritis} Kritis",
                        MerahLatar,
                        MerahGaris,
                        MerahTeks,
                        Icons.Default.WarningAmber,
                    )
                }
                if (state.jumlahMenipis > 0) {
                    PilStatus(
                        "${state.jumlahMenipis} Menipis",
                        AmberLatar,
                        AmberGaris,
                        AmberTeks,
                        Icons.Default.WarningAmber,
                    )
                }
            }
        }
    }
}

@Composable
private fun KotakCari(nilai: String, onUbah: (String) -> Unit) {
    OutlinedTextField(
        value = nilai,
        onValueChange = onUbah,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text("Cari bahan baku…", color = SukaGray400, fontSize = 13.sp) },
        leadingIcon = { Icon(Icons.Default.Search, null, tint = SukaGray400, modifier = Modifier.size(18.dp)) },
        shape = RoundedCornerShape(16.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedIndicatorColor = SukaOrange,
            unfocusedIndicatorColor = GarisKartu,
            focusedTextColor = SukaBrown,
            unfocusedTextColor = SukaBrown,
        ),
    )
}

@Composable
private fun KartuBahan(bahan: BahanCabang) {
    KartuPanel {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    bahan.nama,
                    color = SukaBrown,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    bahan.batasTeks?.let { "Batas minimal $it" } ?: "Batas minimal belum diatur",
                    color = SukaGray400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            BadgeStatusStok(bahan.status)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "SISA STOK",
            color = SukaGray400,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.7.sp,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            bahan.saldoTeks,
            color = when (bahan.status) {
                StatusStok.KRITIS -> MerahTeks
                StatusStok.MENIPIS -> AmberTeks
                StatusStok.AMAN -> SukaBrown
            },
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun BadgeStatusStok(status: StatusStok) {
    val (latar, garis, teks) = when (status) {
        StatusStok.KRITIS -> Triple(MerahLatar, MerahGaris, MerahTeks)
        StatusStok.MENIPIS -> Triple(AmberLatar, AmberGaris, AmberTeks)
        StatusStok.AMAN -> Triple(HijauLatar, HijauGaris, HijauTeks)
    }
    val ikon = if (status == StatusStok.AMAN) Icons.Default.CheckCircle else Icons.Default.WarningAmber
    PilStatus(status.label, latar, garis, teks, ikon)
}
