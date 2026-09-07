package com.sukashawarma.superapp.feature.manager.ui.inventaris

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.sukashawarma.superapp.feature.manager.domain.ItemSidak
import com.sukashawarma.superapp.feature.manager.domain.LABEL_KONDISI
import com.sukashawarma.superapp.feature.manager.domain.waktuJakarta
import com.sukashawarma.superapp.feature.manager.ui.GarisKartu
import com.sukashawarma.superapp.feature.manager.ui.HijauGaris
import com.sukashawarma.superapp.feature.manager.ui.HijauLatar
import com.sukashawarma.superapp.feature.manager.ui.HijauTeks
import com.sukashawarma.superapp.feature.manager.ui.KartuPanel
import com.sukashawarma.superapp.feature.manager.ui.MerahGaris
import com.sukashawarma.superapp.feature.manager.ui.MerahLatar
import com.sukashawarma.superapp.feature.manager.ui.MerahTeks
import com.sukashawarma.superapp.feature.manager.ui.PanelKosong
import com.sukashawarma.superapp.presentation.theme.SukaBrown
import com.sukashawarma.superapp.presentation.theme.SukaCream
import com.sukashawarma.superapp.presentation.theme.SukaGray400
import com.sukashawarma.superapp.presentation.theme.SukaOrange

/**
 * Laporan Inventaris — cermin `dashboard/reports` web.
 *
 * Peran yang tidak mengisi (regional manager, admin, owner) membuka Inventori
 * langsung ke sini, sama seperti web yang mengalihkan `isReportViewer` ke
 * halaman laporan.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaporanInventarisScreen(
    onExit: () -> Unit,
    viewModel: LaporanInventarisViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.galat) {
        val pesan = state.galat
        if (pesan != null) {
            snackbar.showSnackbar(pesan)
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
                        "Laporan Inventaris",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = SukaBrown,
                        maxLines = 1,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { if (state.outletDibuka != null) viewModel.tutupOutlet() else onExit() },
                    ) {
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
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.memuat -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SukaOrange)
                }
                state.outletDibuka == null -> DaftarLaporanOutlet(state, viewModel)
                else -> DetailLaporanOutlet(state, viewModel)
            }
        }
    }

    val foto = state.foto
    if (foto != null) {
        DialogFotoLaporan(foto, viewModel::tutupFoto)
    }
}

@Composable
private fun DaftarLaporanOutlet(
    state: LaporanInventarisUiState,
    viewModel: LaporanInventarisViewModel,
) {
    val daftar = state.outletBerlaporan
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = SukaBrown) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        "Pusat laporan inventaris",
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Pilih outlet untuk melihat laporan inventaris secara lengkap.",
                        color = Color(0xFFFFE7D0),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
        item {
            OutlinedTextField(
                value = state.pencarian,
                onValueChange = viewModel::ubahPencarian,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, null, tint = SukaOrange) },
                placeholder = { Text("Cari outlet...", fontSize = 13.sp) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
            )
        }

        if (daftar.isEmpty()) {
            item { PanelKosong("Belum ada laporan inventaris yang masuk.") }
        } else {
            items(daftar, key = { it.id }) { outlet ->
                val laporan = state.laporan[outlet.id]
                KartuLaporanOutlet(
                    nama = outlet.nama,
                    jumlahItem = laporan?.items?.size ?: 0,
                    berfoto = laporan?.items?.count { it.fotoPath.isNotBlank() } ?: 0,
                    diperbaruiPada = laporan?.diperbaruiPada.orEmpty(),
                    onKlik = { viewModel.bukaOutlet(outlet.id) },
                )
            }
        }
    }
}

@Composable
private fun KartuLaporanOutlet(
    nama: String,
    jumlahItem: Int,
    berfoto: Int,
    diperbaruiPada: String,
    onKlik: () -> Unit,
) {
    Surface(
        Modifier.fillMaxWidth().clickable(onClick = onKlik),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, GarisKartu),
        shadowElevation = 2.dp,
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(38.dp).clip(CircleShape).background(SukaOrange.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Storefront, null, tint = SukaOrange, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    nama,
                    color = SukaBrown,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "$jumlahItem item · $berfoto berfoto",
                    color = SukaGray400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
                if (diperbaruiPada.isNotBlank()) {
                    Text(
                        "Diperbarui ${waktuJakarta(diperbaruiPada)}",
                        color = SukaGray400,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailLaporanOutlet(
    state: LaporanInventarisUiState,
    viewModel: LaporanInventarisViewModel,
) {
    val laporan = state.laporanDibuka
    val items = state.itemTerlihat
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            KartuPanel {
                Text(
                    state.namaOutletDibuka,
                    color = SukaBrown,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${laporan?.items?.size ?: 0} item · diperbarui ${waktuJakarta(laporan?.diperbaruiPada.orEmpty())}",
                    color = SukaGray400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
                val catatan = laporan?.catatan
                if (!catatan.isNullOrBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(catatan, color = SukaBrown, fontSize = 12.sp, lineHeight = 17.sp)
                }
            }
        }
        item {
            OutlinedTextField(
                value = state.pencarian,
                onValueChange = viewModel::ubahPencarian,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, null, tint = SukaOrange) },
                placeholder = { Text("Cari item...", fontSize = 13.sp) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
            )
        }

        if (items.isEmpty()) {
            item { PanelKosong("Tidak ada item yang cocok.") }
        } else {
            items(items, key = { it.id }) { item ->
                BarisItemLaporan(item) { viewModel.bukaFoto(item) }
            }
        }
    }
}

@Composable
private fun BarisItemLaporan(item: ItemSidak, onFoto: () -> Unit) {
    val baik = item.kondisi == "baik"
    KartuPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    item.nama,
                    color = SukaBrown,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(item.subsection, color = SukaGray400, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = if (baik) HijauLatar else MerahLatar,
                border = BorderStroke(1.dp, if (baik) HijauGaris else MerahGaris),
            ) {
                Text(
                    (LABEL_KONDISI[item.kondisi] ?: item.kondisi).uppercase(),
                    Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                    color = if (baik) HijauTeks else MerahTeks,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row {
            Text("Target ${item.targetTeks}", Modifier.weight(1f), color = SukaGray400, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("Hasil ${item.hasilTeks}", color = SukaBrown, fontSize = 11.sp, fontWeight = FontWeight.Black)
        }
        val catatan = item.catatan
        if (!catatan.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(catatan, color = SukaBrown, fontSize = 11.sp, lineHeight = 16.sp)
        }
        if (item.fotoPath.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.clickable(onClick = onFoto),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.PhotoCamera, null, tint = SukaOrange, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Lihat foto", color = SukaOrange, fontSize = 11.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun DialogFotoLaporan(foto: FotoLaporan, onTutup: () -> Unit) {
    Dialog(onDismissRequest = onTutup) {
        Surface(shape = RoundedCornerShape(24.dp), color = Color.White) {
            Column {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            foto.namaItem,
                            color = SukaBrown,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(foto.outlet, color = SukaGray400, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onTutup) {
                        Icon(Icons.Default.Close, "Tutup", tint = SukaBrown)
                    }
                }
                Box(
                    Modifier.fillMaxWidth().height(320.dp).background(Color(0xFF111827)),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        foto.memuat -> CircularProgressIndicator(
                            Modifier.size(28.dp),
                            strokeWidth = 3.dp,
                            color = SukaOrange,
                        )
                        foto.url == null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.ImageNotSupported,
                                null,
                                tint = Color(0xFFFDBA74),
                                modifier = Modifier.size(32.dp),
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Foto tidak dapat dimuat.",
                                color = Color(0xFFFED7AA),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        else -> AsyncImage(
                            model = foto.url,
                            contentDescription = "Foto ${foto.namaItem} di ${foto.outlet}",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}
