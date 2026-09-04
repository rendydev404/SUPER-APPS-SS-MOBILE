package com.sukashawarma.superapp.feature.distribusi.ui.verifikasi

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
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.sukashawarma.superapp.feature.distribusi.data.SuratJalanRepository
import com.sukashawarma.superapp.feature.distribusi.domain.KondisiItem
import com.sukashawarma.superapp.feature.distribusi.ui.LayarGalat
import com.sukashawarma.superapp.feature.distribusi.ui.LayarKosong
import com.sukashawarma.superapp.feature.distribusi.ui.LayarMemuat
import com.sukashawarma.superapp.feature.distribusi.ui.ttd.TandaTanganCanvas
import com.sukashawarma.superapp.presentation.theme.SukaGray500
import com.sukashawarma.superapp.presentation.theme.SukaOnSurface
import com.sukashawarma.superapp.presentation.theme.SukaOrange
import com.sukashawarma.superapp.presentation.theme.SukaSurface

private val MerahTeks = Color(0xFFB91C1C)

@Composable
fun VerifikasiScreen(
    suratJalanId: String,
    onKeluar: () -> Unit,
    onSelesai: () -> Unit,
) {
    val viewModel: VerifikasiViewModel = viewModel(
        factory = VerifikasiViewModel.Factory(suratJalanId),
    )
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.selesai) { if (state.selesai) onSelesai() }

    when {
        state.memuat -> { LayarMemuat(); return }
        state.tidakBerhak -> {
            LayarKosong(
                "Tidak Berwenang",
                "Verifikasi penerimaan dikerjakan crew atau leader di outlet tujuan.",
            ); return
        }
        state.terkunci -> {
            LayarKosong(
                "Verifikasi Terkunci",
                "Pindai kode QR pada lembar surat jalan fisik yang dibawa kurir terlebih dahulu.",
            ); return
        }
        state.sudahDiverifikasi -> {
            LayarKosong(
                "Sudah Diverifikasi",
                "Surat jalan ini sudah pernah diverifikasi. Lihat detailnya di Riwayat.",
            ); return
        }
        state.error != null && state.detail == null -> {
            LayarGalat(state.error!!) { viewModel.muat() }; return
        }
        state.items.isEmpty() -> {
            LayarKosong("Tidak Ada Item", "Surat jalan ini tidak memuat item apa pun."); return
        }
    }

    Column(Modifier.fillMaxSize().background(SukaSurface)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { if (state.langkah == LangkahVerifikasi.KARTU && state.indeksItem == 0) onKeluar() else viewModel.mundur() }) {
                Icon(Icons.Default.ArrowBack, "Kembali")
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "SJ ${state.detail?.nomorDokumen ?: ""}",
                    color = SukaOnSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    when (state.langkah) {
                        LangkahVerifikasi.KARTU ->
                            "Item ${state.indeksItem + 1} dari ${state.items.size}"
                        LangkahVerifikasi.RINGKASAN -> "Ringkasan"
                        LangkahVerifikasi.TTD -> "Tanda tangan penerimaan"
                    },
                    color = SukaGray500,
                    fontSize = 11.sp,
                )
            }
        }

        LinearProgressIndicator(
            progress = { (state.indeksItem + 1f) / state.items.size },
            modifier = Modifier.fillMaxWidth(),
            color = SukaOrange,
        )

        state.error?.let {
            Text(it, Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = MerahTeks, fontSize = 12.sp)
        }

        when (state.langkah) {
            LangkahVerifikasi.KARTU -> KartuItem(state, viewModel)
            LangkahVerifikasi.RINGKASAN -> Ringkasan(state, viewModel)
            LangkahVerifikasi.TTD -> LangkahTtd(state, viewModel)
        }
    }
}

@Composable
private fun KartuItem(state: VerifikasiUiState, viewModel: VerifikasiViewModel) {
    val item = state.itemAktif ?: return
    val isian = state.isianAktif
    var kameraTerbuka by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Color.White) {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        item.item.bahan?.nama ?: "Bahan tidak dikenal",
                        color = SukaOnSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Dikirim ${item.qtyDikirimTampil} ${item.satuan}",
                        color = SukaGray500,
                        fontSize = 12.sp,
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = isian.qtyTerima?.let {
                    if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
                } ?: "",
                onValueChange = viewModel::ubahQty,
                label = { Text("Jumlah diterima (${item.satuan})") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item {
            OutlinedButton(onClick = viewModel::samakanQty, modifier = Modifier.fillMaxWidth()) {
                Text("Sesuai Kirim (${item.qtyDikirimTampil} ${item.satuan})")
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TombolKondisi("Baik", isian.kondisi == KondisiItem.BAIK, Modifier.weight(1f)) {
                    viewModel.ubahKondisi(KondisiItem.BAIK)
                }
                TombolKondisi("Tidak Sesuai", isian.kondisi == KondisiItem.TIDAK_SESUAI, Modifier.weight(1f)) {
                    viewModel.ubahKondisi(KondisiItem.TIDAK_SESUAI)
                }
            }
        }

        if (isian.kondisi == KondisiItem.TIDAK_SESUAI) {
            item {
                OutlinedTextField(
                    value = isian.catatan,
                    onValueChange = viewModel::ubahCatatan,
                    label = { Text("Catatan alasan (wajib)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        item {
            Button(
                onClick = viewModel::konfirmasiKondisi,
                enabled = !state.kondisiTerkonfirmasi,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.kondisiTerkonfirmasi) "Kondisi Terkonfirmasi" else "Konfirmasi Kondisi")
            }
        }

        item {
            if (kameraTerbuka) {
                FotoCameraSheet(
                    onDiambil = { bitmap ->
                        kameraTerbuka = false
                        viewModel.unggahFoto(bitmap)
                    },
                    onBatal = { kameraTerbuka = false },
                )
            } else {
                OutlinedButton(
                    onClick = { kameraTerbuka = true },
                    enabled = !state.mengunggahFoto,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        when {
                            state.mengunggahFoto -> "Mengunggah foto..."
                            isian.fotoPath != null -> "Foto bukti tersimpan — Ambil Ulang"
                            else -> "Ambil Foto Bukti (wajib)"
                        }
                    )
                }
            }
        }

        item {
            Button(
                onClick = viewModel::lanjut,
                enabled = state.kondisiTerkonfirmasi && isian.fotoPath != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.indeksItem + 1 >= state.items.size) "Lanjut ke Ringkasan" else "Item Berikutnya")
            }
        }
    }
}

@Composable
private fun Ringkasan(state: VerifikasiUiState, viewModel: VerifikasiViewModel) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(state.items, key = { it.item.id }) { tampil ->
            val isian = state.isian[tampil.item.id]
            val tidakSesuai = isian?.kondisi == KondisiItem.TIDAK_SESUAI ||
                (isian?.qtyTerima ?: 0.0) < tampil.qtyDikirimTampil
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = Color.White) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        tampil.item.bahan?.nama ?: "-",
                        color = SukaOnSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "${isian?.qtyTerima?.toLong() ?: 0} / ${tampil.qtyDikirimTampil} ${tampil.satuan}",
                        color = if (tidakSesuai) MerahTeks else SukaGray500,
                        fontSize = 12.sp,
                    )
                    if (!isian?.catatan.isNullOrBlank()) {
                        Text(isian!!.catatan, color = SukaGray500, fontSize = 11.sp)
                    }
                }
            }
        }
        item {
            Button(onClick = viewModel::keTandaTangan, modifier = Modifier.fillMaxWidth()) {
                Text("Lanjut ke Tanda Tangan")
            }
        }
    }
}

@Composable
private fun LangkahTtd(state: VerifikasiUiState, viewModel: VerifikasiViewModel) {
    var peranAktif by remember { mutableStateOf<String?>(null) }
    var namaSupir by remember { mutableStateOf("") }

    val sudahCrew = state.ttdPenerimaan.any { it.peran == SuratJalanRepository.PERAN_CREW }
    val sudahSupir = state.ttdPenerimaan.any { it.peran == SuratJalanRepository.PERAN_SUPIR }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(state.ttdPenerimaan) { ttd ->
            Text(
                "${ttd.peran}: ${ttd.namaPenandaTangan}",
                color = SukaOnSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        if (peranAktif == null) {
            item {
                OutlinedButton(
                    onClick = { peranAktif = SuratJalanRepository.PERAN_CREW },
                    enabled = !sudahCrew,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (sudahCrew) "Crew Penerima sudah tanda tangan" else "Tanda Tangan Crew Penerima") }
            }
            item {
                OutlinedButton(
                    onClick = { peranAktif = SuratJalanRepository.PERAN_SUPIR },
                    enabled = !sudahSupir,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (sudahSupir) "Supir sudah tanda tangan" else "Tanda Tangan Supir") }
            }
        } else {
            val peran = peranAktif!!
            // Nama crew diambil dari sesi dan tidak bisa diubah: yang menerima
            // barang adalah orang yang sedang login. Nama supir diketik karena
            // dia bukan pengguna aplikasi.
            if (peran == SuratJalanRepository.PERAN_SUPIR) {
                item {
                    OutlinedTextField(
                        value = namaSupir,
                        onValueChange = { namaSupir = it },
                        label = { Text("Nama supir / kurir") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                item { Text("Nama: ${state.namaCrew}", color = SukaOnSurface, fontSize = 12.sp) }
            }
            item {
                TandaTanganCanvas(
                    onSelesai = { gambar ->
                        val nama = if (peran == SuratJalanRepository.PERAN_CREW) state.namaCrew else namaSupir
                        viewModel.tandaTangan(peran, nama, gambar)
                        peranAktif = null
                    },
                    onBatal = { peranAktif = null },
                )
            }
        }

        item {
            Button(
                onClick = viewModel::finalisasi,
                enabled = state.ttdLengkap && !state.memfinalisasi,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.memfinalisasi) "Menyimpan..." else "Selesaikan Penerimaan")
            }
        }
        if (!state.ttdLengkap) {
            item {
                Text(
                    "Kedua tanda tangan wajib lengkap sebelum penerimaan bisa diselesaikan.",
                    color = SukaGray500,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

@Composable
private fun TombolKondisi(teks: String, aktif: Boolean, modifier: Modifier, onKlik: () -> Unit) {
    if (aktif) Button(onClick = onKlik, modifier = modifier) { Text(teks) }
    else OutlinedButton(onClick = onKlik, modifier = modifier) { Text(teks) }
}
