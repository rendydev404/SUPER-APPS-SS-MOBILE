package com.sukashawarma.superapp.feature.distribusi.ui.detail

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.feature.distribusi.data.model.TandaTangan
import com.sukashawarma.superapp.feature.distribusi.ui.LayarGalat
import com.sukashawarma.superapp.feature.distribusi.ui.LayarMemuat
import com.sukashawarma.superapp.feature.distribusi.ui.LencanaStatus
import com.sukashawarma.superapp.feature.distribusi.ui.formatTanggal
import com.sukashawarma.superapp.presentation.theme.SukaGray500
import com.sukashawarma.superapp.presentation.theme.SukaOnSurface
import com.sukashawarma.superapp.presentation.theme.SukaSurface

private val MerahTeks = Color(0xFFB91C1C)

@Composable
fun DetailSuratJalanScreen(suratJalanId: String, onKeluar: () -> Unit) {
    val viewModel: DetailViewModel = viewModel(factory = DetailViewModel.Factory(suratJalanId))
    val state by viewModel.state.collectAsState()

    if (state.memuat) { LayarMemuat(); return }
    val detail = state.detail
    if (detail == null) {
        LayarGalat(state.error ?: "Dokumen tidak bisa dibuka.") { viewModel.muat() }
        return
    }

    LaunchedEffect(state.baris) {
        state.baris.mapNotNull { it.fotoPath }.forEach { viewModel.muatFoto(it) }
    }

    Column(Modifier.fillMaxSize().background(SukaSurface)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onKeluar) { Icon(Icons.Default.ArrowBack, "Kembali") }
            Column(Modifier.weight(1f)) {
                Text(
                    "SJ ${detail.nomorDokumen ?: detail.id.take(8).uppercase()}",
                    color = SukaOnSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    "${detail.namaOutlet ?: "Gudang Pusat"} • ${formatTanggal(detail.dibuatPada)}",
                    color = SukaGray500,
                    fontSize = 11.sp,
                )
            }
            LencanaStatus(detail.status, state.baris.any { it.bermasalah })
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Kode verifikasi hanya untuk pengawas — lihat DistribusiAkses.
            if (state.bolehLihatKode && detail.kodeVerifikasi != null) {
                item {
                    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = Color.White) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Kode Verifikasi", color = SukaGray500, fontSize = 10.sp)
                            Text(
                                detail.kodeVerifikasi,
                                color = SukaOnSurface,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                        }
                    }
                }
            }

            items(state.baris) { baris ->
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = Color.White) {
                    Column(Modifier.padding(12.dp)) {
                        Text(baris.nama, color = SukaOnSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "Dikirim ${baris.qtyDikirim} ${baris.satuan} • Diterima " +
                                "${baris.qtyTerima?.toString() ?: "-"} ${baris.satuan}",
                            color = if (baris.bermasalah) MerahTeks else SukaGray500,
                            fontSize = 12.sp,
                        )
                        if (!baris.catatan.isNullOrBlank()) {
                            Text(baris.catatan, color = SukaGray500, fontSize = 11.sp)
                        }
                        val bitmap = baris.fotoPath?.let { state.foto[it] }
                        if (bitmap != null) {
                            Spacer(Modifier.height(8.dp))
                            Image(
                                bitmap.asImageBitmap(),
                                contentDescription = "Foto bukti ${baris.nama}",
                                modifier = Modifier.fillMaxWidth().height(180.dp),
                            )
                        }
                    }
                }
            }

            item { BlokTandaTangan("Tanda Tangan Pengirim", detail.ttdPengirim) }
            item { BlokTandaTangan("Tanda Tangan Penerimaan", detail.ttdPenerimaan) }
        }
    }
}

@Composable
private fun BlokTandaTangan(judul: String, daftar: List<TandaTangan>) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = Color.White) {
        Column(Modifier.padding(12.dp)) {
            Text(judul, color = SukaOnSurface, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(6.dp))
            if (daftar.isEmpty()) {
                Text("Belum ada tanda tangan.", color = SukaGray500, fontSize = 11.sp)
            } else {
                daftar.forEach { ttd ->
                    Text(
                        "${ttd.peran}: ${ttd.namaPenandaTangan}",
                        color = SukaOnSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(formatTanggal(ttd.waktu), color = SukaGray500, fontSize = 10.sp)
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}
