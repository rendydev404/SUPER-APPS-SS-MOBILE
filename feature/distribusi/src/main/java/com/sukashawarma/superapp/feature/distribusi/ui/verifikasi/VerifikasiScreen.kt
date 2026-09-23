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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.core.ui.ios.BarisIos
import com.sukashawarma.superapp.core.ui.ios.BilahJudulIos
import com.sukashawarma.superapp.core.ui.ios.GrupIos
import com.sukashawarma.superapp.core.ui.ios.KartuIos
import com.sukashawarma.superapp.core.ui.ios.LencanaIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.PemisahIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.TombolKeduaIos
import com.sukashawarma.superapp.core.ui.ios.TombolUtamaIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.ios.warnaKolomIos
import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.feature.distribusi.data.SuratJalanRepository
import com.sukashawarma.superapp.feature.distribusi.domain.KondisiItem
import com.sukashawarma.superapp.feature.distribusi.ui.LayarGalat
import com.sukashawarma.superapp.feature.distribusi.ui.LayarKosong
import com.sukashawarma.superapp.feature.distribusi.ui.LayarMemuat
import com.sukashawarma.superapp.feature.distribusi.ui.ttd.TandaTanganCanvas

/** Jarak isi langkah verifikasi — sama dengan layar lain modul ini. */
private val PaddingLangkah = PaddingValues(
    start = UkuranIos.TepiLayar,
    end = UkuranIos.TepiLayar,
    top = 12.dp,
    bottom = 28.dp,
)

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
                ikon = IkonIos.Lock,
            ); return
        }
        state.terkunci -> {
            LayarKosong(
                "Verifikasi Terkunci",
                "Pindai kode QR pada lembar surat jalan fisik yang dibawa kurir terlebih dahulu.",
                ikon = IkonIos.QrCodeScanner,
                nada = NadaIos.PERINGATAN,
            ); return
        }
        state.sudahDiverifikasi -> {
            LayarKosong(
                "Sudah Diverifikasi",
                "Surat jalan ini sudah pernah diverifikasi. Lihat detailnya di Riwayat.",
                ikon = IkonIos.CheckCircle,
                nada = NadaIos.SUKSES,
            ); return
        }
        state.error != null && state.detail == null -> {
            LayarGalat(state.error!!) { viewModel.muat() }; return
        }
        state.items.isEmpty() -> {
            LayarKosong("Tidak Ada Item", "Surat jalan ini tidak memuat item apa pun."); return
        }
    }

    Column(Modifier.fillMaxSize().background(WarnaIos.Latar)) {
        BilahJudulIos(
            judul = "SJ ${state.detail?.nomorDokumen ?: ""}",
            subjudul = when (state.langkah) {
                LangkahVerifikasi.KARTU ->
                    "Item ${state.indeksItem + 1} dari ${state.items.size}"
                LangkahVerifikasi.RINGKASAN -> "Ringkasan"
                LangkahVerifikasi.TTD -> "Tanda tangan penerimaan"
            },
            onKembali = { if (state.langkah == LangkahVerifikasi.KARTU && state.indeksItem == 0) onKeluar() else viewModel.mundur() },
            garisBawah = false,
        )

        LinearProgressIndicator(
            progress = { (state.indeksItem + 1f) / state.items.size },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = UkuranIos.TepiLayar)
                .height(4.dp)
                .clip(UkuranIos.SudutKapsul),
            color = WarnaIos.Aksen,
            trackColor = WarnaIos.Isian,
        )

        state.error?.let {
            Text(
                it,
                Modifier.padding(horizontal = UkuranIos.TepiLayar, vertical = 8.dp),
                color = NadaIos.BAHAYA.teks,
                fontSize = 13.sp,
            )
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
        contentPadding = PaddingLangkah,
        verticalArrangement = Arrangement.spacedBy(UkuranIos.JarakKartu),
    ) {
        item {
            KartuIos {
                Text(
                    item.item.bahan?.nama ?: "Bahan tidak dikenal",
                    style = TipeIos.Judul3,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Dikirim ${item.qtyDikirimTampil} ${item.satuan}",
                    style = TipeIos.SubJudul,
                )
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
                shape = UkuranIos.SudutKontrol,
                colors = warnaKolomIos(),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item {
            TombolKeduaIos(
                "Sesuai Kirim (${item.qtyDikirimTampil} ${item.satuan})",
                viewModel::samakanQty,
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TombolKondisi("Baik", isian.kondisi == KondisiItem.BAIK, WarnaIos.Hijau, Modifier.weight(1f)) {
                    viewModel.ubahKondisi(KondisiItem.BAIK)
                }
                TombolKondisi("Tidak Sesuai", isian.kondisi == KondisiItem.TIDAK_SESUAI, WarnaIos.Merah, Modifier.weight(1f)) {
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
                    shape = UkuranIos.SudutKontrol,
                    colors = warnaKolomIos(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        item {
            TombolUtamaIos(
                if (state.kondisiTerkonfirmasi) "Kondisi Terkonfirmasi" else "Konfirmasi Kondisi",
                viewModel::konfirmasiKondisi,
                aktif = !state.kondisiTerkonfirmasi,
                ikon = if (state.kondisiTerkonfirmasi) IkonIos.CheckCircle else null,
            )
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
                TombolKeduaIos(
                    when {
                        state.mengunggahFoto -> "Mengunggah foto..."
                        isian.fotoPath != null -> "Foto bukti tersimpan — Ambil Ulang"
                        else -> "Ambil Foto Bukti (wajib)"
                    },
                    { kameraTerbuka = true },
                    aktif = !state.mengunggahFoto,
                )
            }
        }

        item {
            TombolUtamaIos(
                if (state.indeksItem + 1 >= state.items.size) "Lanjut ke Ringkasan" else "Item Berikutnya",
                viewModel::lanjut,
                aktif = state.kondisiTerkonfirmasi && isian.fotoPath != null,
            )
        }
    }
}

@Composable
private fun Ringkasan(state: VerifikasiUiState, viewModel: VerifikasiViewModel) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingLangkah,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(state.items, key = { it.item.id }) { tampil ->
            val isian = state.isian[tampil.item.id]
            val tidakSesuai = isian?.kondisi == KondisiItem.TIDAK_SESUAI ||
                (isian?.qtyTerima ?: 0.0) < tampil.qtyDikirimTampil
            KartuIos(padding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        tampil.item.bahan?.nama ?: "-",
                        Modifier.weight(1f),
                        style = TipeIos.Utama,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${isian?.qtyTerima?.toLong() ?: 0} / ${tampil.qtyDikirimTampil} ${tampil.satuan}",
                        style = TipeIos.Keterangan.copy(
                            color = if (tidakSesuai) NadaIos.BAHAYA.teks else WarnaIos.LabelKedua,
                        ),
                    )
                }
                if (!isian?.catatan.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(isian!!.catatan, style = TipeIos.Catatan)
                }
            }
        }
        item {
            Spacer(Modifier.height(4.dp))
            TombolUtamaIos("Lanjut ke Tanda Tangan", viewModel::keTandaTangan)
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
        contentPadding = PaddingLangkah,
        verticalArrangement = Arrangement.spacedBy(UkuranIos.JarakKartu),
    ) {
        items(state.ttdPenerimaan) { ttd ->
            KartuIos(padding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(IkonIos.CheckCircle, null, tint = WarnaIos.Hijau, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "${ttd.peran}: ${ttd.namaPenandaTangan}",
                        Modifier.weight(1f),
                        style = TipeIos.Keterangan.copy(fontSize = 15.sp),
                    )
                }
            }
        }

        if (peranAktif == null) {
            item {
                // Dua pintu tanda tangan dalam satu grup ala Pengaturan iOS. Baris yang
                // sudah ditandatangani tidak bisa ditekan — setara tombol nonaktif lama.
                GrupIos {
                    BarisIos(
                        judul = if (sudahCrew) "Crew Penerima sudah tanda tangan" else "Tanda Tangan Crew Penerima",
                        ikon = IkonIos.Person,
                        nadaIkon = if (sudahCrew) NadaIos.SUKSES else NadaIos.AKSEN,
                        onKlik = if (!sudahCrew) {
                            { peranAktif = SuratJalanRepository.PERAN_CREW }
                        } else {
                            null
                        },
                        trailing = if (sudahCrew) {
                            { LencanaIos("Selesai", NadaIos.SUKSES, titik = false) }
                        } else {
                            null
                        },
                    )
                    PemisahIos(inset = 58.dp)
                    BarisIos(
                        judul = if (sudahSupir) "Supir sudah tanda tangan" else "Tanda Tangan Supir",
                        ikon = IkonIos.LocalShipping,
                        nadaIkon = if (sudahSupir) NadaIos.SUKSES else NadaIos.AKSEN,
                        onKlik = if (!sudahSupir) {
                            { peranAktif = SuratJalanRepository.PERAN_SUPIR }
                        } else {
                            null
                        },
                        trailing = if (sudahSupir) {
                            { LencanaIos("Selesai", NadaIos.SUKSES, titik = false) }
                        } else {
                            null
                        },
                    )
                }
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
                        shape = UkuranIos.SudutKontrol,
                        colors = warnaKolomIos(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                item {
                    GrupIos {
                        BarisIos(judul = "Nama: ${state.namaCrew}", ikon = IkonIos.Person)
                    }
                }
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
            TombolUtamaIos(
                if (state.memfinalisasi) "Menyimpan..." else "Selesaikan Penerimaan",
                viewModel::finalisasi,
                aktif = state.ttdLengkap && !state.memfinalisasi,
            )
        }
        if (!state.ttdLengkap) {
            item {
                Text(
                    "Kedua tanda tangan wajib lengkap sebelum penerimaan bisa diselesaikan.",
                    Modifier.padding(horizontal = 4.dp),
                    style = TipeIos.Catatan,
                )
            }
        }
    }
}

/** Pilihan kondisi: terisi warnanya saat terpilih, isian tipis saat tidak. */
@Composable
private fun TombolKondisi(teks: String, aktif: Boolean, warna: Color, modifier: Modifier, onKlik: () -> Unit) {
    if (aktif) TombolUtamaIos(teks, onKlik, modifier, warna = warna)
    else TombolKeduaIos(teks, onKlik, modifier, warna = warna)
}
