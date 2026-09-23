package com.sukashawarma.superapp.feature.manager.ui.inventaris

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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.RealtimeTables
import com.sukashawarma.superapp.core.ui.ios.BilahJudulIos
import com.sukashawarma.superapp.core.ui.ios.IkonBulatIos
import com.sukashawarma.superapp.core.ui.ios.KartuIos
import com.sukashawarma.superapp.core.ui.ios.KolomCariIos
import com.sukashawarma.superapp.core.ui.ios.LencanaIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.TombolBundarIos
import com.sukashawarma.superapp.core.ui.ios.TombolKapsulIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.core.ui.kaca.denganRuangNav
import com.sukashawarma.superapp.core.ui.kaca.navigationBarsPaddingKaca
import com.sukashawarma.superapp.feature.manager.domain.ItemSidak
import com.sukashawarma.superapp.feature.manager.domain.LABEL_KONDISI
import com.sukashawarma.superapp.feature.manager.domain.waktuJakarta
import com.sukashawarma.superapp.feature.manager.ui.KartuPanel
import com.sukashawarma.superapp.feature.manager.ui.PanelKosong

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

    // Menunggu `plan/inventaris-realtime-publication.sql` dijalankan di Supabase;
    // sebelum itu langganan ini hidup tapi tidak pernah menerima event.
    RealtimeRefresh(RealtimeTables.INVENTARIS_SUBMISSIONS) { viewModel.muatUlang(silent = true) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.galat) {
        val pesan = state.galat
        if (pesan != null) {
            snackbar.showSnackbar(pesan)
            viewModel.tutupKabar()
        }
    }

    Scaffold(
        containerColor = WarnaIos.Latar,
        snackbarHost = { SnackbarHost(snackbar, Modifier.navigationBarsPaddingKaca()) },
        topBar = {
            BilahJudulIos(
                judul = "Laporan Inventaris",
                onKembali = { if (state.outletDibuka != null) viewModel.tutupOutlet() else onExit() },
                aksi = { TombolBundarIos(IkonIos.Refresh, "Muat ulang", viewModel::muatUlang) },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.memuat && state.outlets.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = WarnaIos.Aksen)
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
        contentPadding = PaddingValues(UkuranIos.TepiLayar).denganRuangNav(),
        verticalArrangement = Arrangement.spacedBy(UkuranIos.JarakKartu),
    ) {
        item {
            KartuPanel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IkonBulatIos(IkonIos.Assignment, WarnaIos.Aksen, ukuran = 36.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Pusat laporan inventaris", Modifier.weight(1f), style = TipeIos.Judul3)
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Pilih outlet untuk melihat laporan inventaris secara lengkap.",
                    style = TipeIos.Catatan.copy(lineHeight = 18.sp),
                )
            }
        }
        item {
            KolomCariIos(
                nilai = state.pencarian,
                onUbah = viewModel::ubahPencarian,
                modifier = Modifier.fillMaxWidth(),
                placeholder = "Cari outlet...",
            )
        }

        if (daftar.isEmpty()) {
            item { KartuPanel { PanelKosong("Belum ada laporan inventaris yang masuk.") } }
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
    KartuIos(
        onKlik = onKlik,
        padding = PaddingValues(horizontal = UkuranIos.PaddingKartu, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IkonBulatIos(IkonIos.Storefront, WarnaIos.Aksen, ukuran = 36.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(nama, style = TipeIos.Utama, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("$jumlahItem item · $berfoto berfoto", style = TipeIos.Catatan, maxLines = 1)
                if (diperbaruiPada.isNotBlank()) {
                    Text("Diperbarui ${waktuJakarta(diperbaruiPada)}", style = TipeIos.Kecil, maxLines = 1)
                }
            }
            Spacer(Modifier.width(8.dp))
            Icon(IkonIos.ChevronRight, null, tint = WarnaIos.LabelKetiga, modifier = Modifier.size(15.dp))
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
        contentPadding = PaddingValues(UkuranIos.TepiLayar).denganRuangNav(),
        verticalArrangement = Arrangement.spacedBy(UkuranIos.JarakKartu),
    ) {
        item {
            KartuPanel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IkonBulatIos(IkonIos.Storefront, WarnaIos.Aksen, ukuran = 36.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            state.namaOutletDibuka,
                            style = TipeIos.Judul3,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "${laporan?.items?.size ?: 0} item · diperbarui ${waktuJakarta(laporan?.diperbaruiPada.orEmpty())}",
                            style = TipeIos.Catatan,
                        )
                    }
                }
                val catatan = laporan?.catatan
                if (!catatan.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        catatan,
                        Modifier
                            .fillMaxWidth()
                            .clip(UkuranIos.SudutBlok)
                            .background(WarnaIos.Latar)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        style = TipeIos.Catatan.copy(color = WarnaIos.Label, lineHeight = 18.sp),
                    )
                }
            }
        }
        item {
            KolomCariIos(
                nilai = state.pencarian,
                onUbah = viewModel::ubahPencarian,
                modifier = Modifier.fillMaxWidth(),
                placeholder = "Cari item...",
            )
        }

        if (items.isEmpty()) {
            item { KartuPanel { PanelKosong("Tidak ada item yang cocok.") } }
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
                Text(item.nama, style = TipeIos.Utama, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(item.subsection, style = TipeIos.Catatan, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            LencanaIos(
                LABEL_KONDISI[item.kondisi] ?: item.kondisi,
                if (baik) NadaIos.SUKSES else NadaIos.BAHAYA,
            )
        }
        Spacer(Modifier.height(10.dp))
        BlokTargetHasil(item.targetTeks, item.hasilTeks)
        val catatan = item.catatan
        if (!catatan.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(catatan, style = TipeIos.Catatan.copy(color = WarnaIos.Label, lineHeight = 17.sp))
        }
        if (item.fotoPath.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            TombolKapsulIos("Lihat foto", onFoto, ikon = IkonIos.PhotoCamera)
        }
    }
}

/**
 * Target & hasil berdampingan dalam blok abu bersekat, meniru `BlokAngkaIos`.
 *
 * Tidak memakai `BlokAngkaIos` bersama karena nilainya teks bebas ("Ada / tidak
 * ada", "2–3 unit") yang terpotong di angka 20sp satu baris; di sini nilainya
 * 16sp dan boleh dua baris.
 */
@Composable
private fun BlokTargetHasil(target: String, hasil: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(UkuranIos.SudutBlok)
            .background(WarnaIos.Latar)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        KolomTargetHasil("Target", target, WarnaIos.LabelKedua, Modifier.weight(1f))
        Box(Modifier.width(0.5.dp).height(30.dp).background(WarnaIos.Pemisah))
        KolomTargetHasil("Hasil", hasil, WarnaIos.Label, Modifier.weight(1f))
    }
}

@Composable
private fun KolomTargetHasil(label: String, nilai: String, warna: Color, modifier: Modifier) {
    Column(modifier.padding(horizontal = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = TipeIos.Kecil.copy(fontWeight = FontWeight.Medium), maxLines = 1)
        Spacer(Modifier.height(2.dp))
        Text(
            nilai,
            style = TipeIos.Keterangan.copy(color = warna, fontWeight = FontWeight.SemiBold),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Dialog foto. Bingkai gelap di bawah foto sengaja dipertahankan — foto aset
 * dilihat paling jelas di atas latar netral gelap, bukan putih kartu.
 */
@Composable
private fun DialogFotoLaporan(foto: FotoLaporan, onTutup: () -> Unit) {
    Dialog(onDismissRequest = onTutup) {
        Column(Modifier.fillMaxWidth().clip(UkuranIos.SudutKartu).background(WarnaIos.Kartu)) {
            Row(
                Modifier.padding(start = UkuranIos.PaddingKartu, end = 12.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(foto.namaItem, style = TipeIos.Utama, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(foto.outlet, style = TipeIos.Catatan, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.width(8.dp))
                TombolBundarIos(IkonIos.Close, "Tutup", onTutup, warnaIkon = WarnaIos.LabelKedua)
            }
            Box(
                Modifier.fillMaxWidth().height(320.dp).background(Color(0xFF1C1C1E)),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    foto.memuat -> CircularProgressIndicator(
                        Modifier.size(28.dp),
                        strokeWidth = 2.5.dp,
                        color = WarnaIos.Aksen,
                    )
                    foto.url == null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            IkonIos.Image,
                            null,
                            tint = WarnaIos.Abu,
                            modifier = Modifier.size(32.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Foto tidak dapat dimuat.",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
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
