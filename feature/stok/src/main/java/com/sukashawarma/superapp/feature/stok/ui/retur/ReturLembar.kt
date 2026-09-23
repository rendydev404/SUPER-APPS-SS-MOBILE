package com.sukashawarma.superapp.feature.stok.ui.retur

import com.sukashawarma.superapp.core.ui.ios.KartuIos
import com.sukashawarma.superapp.core.ui.ios.LabelSeksiIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.TombolKeduaIos
import com.sukashawarma.superapp.core.ui.ios.TombolUtamaIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.ios.tekanIos
import com.sukashawarma.superapp.core.ui.ios.warnaKolomIos
import com.sukashawarma.superapp.feature.stok.ui.BannerIos
import androidx.compose.ui.draw.clip
import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.core.ui.kaca.navigationBarsPaddingKaca
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import com.sukashawarma.superapp.core.ui.SukaDropdownHeader
import com.sukashawarma.superapp.core.ui.SukaDropdownMenu
import com.sukashawarma.superapp.core.ui.SukaDropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import coil.compose.AsyncImage
import com.sukashawarma.superapp.core.camera.KameraFotoSheet
import com.sukashawarma.superapp.feature.stok.domain.FormulirRetur
import com.sukashawarma.superapp.feature.stok.domain.JenisLogistik
import com.sukashawarma.superapp.feature.stok.domain.StatusRetur
import com.sukashawarma.superapp.feature.stok.domain.formatSatuan
import com.sukashawarma.superapp.feature.stok.ui.HeaderStok
import com.sukashawarma.superapp.feature.stok.ui.PitaPesan

/// ============================================================ formulir klaim

/**
 * Formulir pengajuan retur — cermin `components/refund/FormPengajuanRefund.tsx`.
 *
 * Satu bahan per tiket, sama seperti web: RPC menerima array item, tetapi formnya
 * hanya pernah mengirim satu. Menambah baris kedua di sini akan membuat tiket yang
 * tidak bisa ditampilkan utuh oleh browser.
 */
@Composable
fun FormReturScreen(state: ReturUiState, viewModel: ReturViewModel) {
    val mintaFoto = ingatPemintaKamera(viewModel)

    Column(Modifier.fillMaxSize().background(LatarRetur)) {
        HeaderStok(
            judul = "Ajukan Retur Bahan",
            subjudul = "Input berat timbangan & bukti foto untuk klaim ganti fisik",
            onKembali = viewModel::tutupLembar,
        )
        state.error?.let { PitaPesan(it, true, viewModel::bersihkanPesan) }

        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = UkuranIos.TepiLayar, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                KartuForm("Pilih Bahan Baku", wajib = true) {
                    PilihanTurun(
                        nilai = state.bahanTerpilih?.let { "${it.nama} (${formatSatuan(it.satuan).uppercase()})" }
                            ?: "-- Pilih Bahan Core --",
                        pilihan = state.katalogRefundable.map {
                            it.id to "${it.nama} (${formatSatuan(it.satuan).uppercase()})"
                        },
                        onPilih = viewModel::pilihBahan,
                    )
                    if (state.katalogRefundable.isEmpty()) {
                        Text(
                            "Katalog bahan core belum termuat. Tarik ulang halaman sebelumnya.",
                            style = TipeIos.Catatan.copy(color = MerahTua),
                        )
                    }
                }
            }

            state.bahanTerpilih?.let { bahan ->
                item {
                    KartuForm("Input Berat / Kuantitas Rusak", wajib = true) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            KolomAngka(
                                label = formatSatuan(bahan.satuan).uppercase().ifBlank { "BESAR" },
                                nilai = state.formQtyBesar,
                                onUbah = viewModel::ubahQtyBesar,
                                modifier = Modifier.weight(1f),
                            )
                            KolomAngka(
                                label = formatSatuan(bahan.satuanKecil).uppercase().ifBlank { "GRAM" },
                                nilai = state.formQtyKecil,
                                onUbah = viewModel::ubahQtyKecil,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Text(
                            "Total: ${qtyTeks(state.totalKlaimBesar, bahan.satuan)}",
                            color = AmberTua, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Stok outlet akan dipotong sebesar angka ini saat formulir diajukan.",
                            style = TipeIos.Catatan.copy(lineHeight = 18.sp),
                        )
                    }
                }
            }

            item {
                KartuForm("Kategori Kerusakan / Alasan", wajib = true) {
                    PilihanTurun(
                        nilai = state.formAlasan,
                        pilihan = FormulirRetur.ALASAN.map { it to it },
                        onPilih = viewModel::pilihAlasan,
                    )
                    if (state.formAlasan == FormulirRetur.ALASAN_LAINNYA) {
                        OutlinedTextField(
                            value = state.formAlasanLainnya,
                            onValueChange = viewModel::ubahAlasanLainnya,
                            placeholder = { Text("Tuliskan alasan retur…", fontSize = 15.sp) },
                            singleLine = true,
                            textStyle = TipeIos.Keterangan,
                            shape = UkuranIos.SudutKontrol,
                            colors = warnaKolomIos(),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            item {
                KartuForm("Foto Bahan Baku di Atas Timbangan", wajib = true) {
                    BagianFotoRetur(
                        url = state.formFotoUrl,
                        mengunggah = state.mengunggahFoto,
                        keterangan = "Pastikan fisik bahan rusak dan angka timbangan terlihat jelas dalam 1 foto.",
                        onAmbil = mintaFoto,
                    )
                }
            }

            item {
                KartuForm("Catatan Tambahan (Opsional)", wajib = false) {
                    OutlinedTextField(
                        value = state.formCatatan,
                        onValueChange = viewModel::ubahCatatanForm,
                        placeholder = { Text("Keterangan untuk Area Manager & Central Kitchen…", fontSize = 15.sp) },
                        minLines = 2,
                        textStyle = TipeIos.Keterangan,
                        shape = UkuranIos.SudutKontrol,
                        colors = warnaKolomIos(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(UkuranIos.SudutBlok)
                        .background(WarnaIos.Aksen.copy(alpha = 0.10f))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(IkonIos.ErrorOutline, null, tint = WarnaIos.Aksen, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Prosedur Pengembalian", style = TipeIos.Catatan.copy(color = AmberTua, fontWeight = FontWeight.SemiBold))
                    }
                    Text(
                        "Setelah dikirim, pengajuan direview Area Manager / Regional Manager. " +
                            "Jangan menyerahkan daging atau kulit ke kurir sebelum tiket berstatus \"Disetujui AM/RM\".",
                        style = TipeIos.Catatan.copy(color = AmberTua, lineHeight = 18.sp),
                    )
                }
            }
        }

        BilahKirimForm(state, viewModel)
    }

    LembarKamera(state, viewModel, "Foto bahan di atas timbangan")
}

@Composable
private fun BilahKirimForm(state: ReturUiState, viewModel: ReturViewModel) {
    val halangan = state.halanganForm
    Column(Modifier.fillMaxWidth().background(WarnaIos.Kartu)) {
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(WarnaIos.Pemisah))
        Column(Modifier.navigationBarsPaddingKaca().padding(horizontal = UkuranIos.TepiLayar, vertical = 12.dp)) {
            if (halangan != null) {
                Text(halangan, style = TipeIos.Catatan.copy(lineHeight = 18.sp))
                Spacer(Modifier.height(8.dp))
            }
            TombolUtamaIos(
                "Kirim Pengajuan Retur",
                viewModel::kirimKlaim,
                aktif = halangan == null && !state.memproses && !state.mengunggahFoto,
                memuat = state.memproses,
            )
        }
    }
}

// ================================================== lembar keputusan AM / RM

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LembarKeputusanManager(state: ReturUiState, viewModel: ReturViewModel) {
    val tiket = state.tiketAktif ?: return
    ModalBottomSheet(
        onDismissRequest = viewModel::tutupLembar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = WarnaIos.Kartu,
    ) {
        Column(
            Modifier.padding(horizontal = UkuranIos.TepiLayar).padding(bottom = 24.dp).heightIn(max = 620.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            JudulLembar("Persetujuan AM / RM", "Validasi Klaim ${tiket.nomorRetur} · ${tiket.outletName ?: "Outlet"}")

            tiket.items.forEach { item ->
                Column(
                    Modifier.fillMaxWidth().clip(UkuranIos.SudutBlok).background(WarnaIos.Latar).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        "${item.namaBahan ?: "Bahan Baku"} — ${qtyTeks(item.qtyKlaim, item.satuan)}",
                        style = TipeIos.Keterangan.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                    )
                    Text("Alasan: ${item.alasan}", style = TipeIos.Catatan.copy(color = AmberTua))
                    if (!item.catatan.isNullOrBlank()) {
                        Text("\"${item.catatan}\"", style = TipeIos.Kecil)
                    }
                    if (!item.fotoFisikUrl.isNullOrBlank()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Thumbnail(item.fotoFisikUrl, "Timbangan") {
                                viewModel.lihatFoto(
                                    "Bukti Timbangan: ${item.namaBahan ?: "Bahan"}",
                                    item.fotoFisikUrl,
                                )
                            }
                            // Dua kolom foto hanya dirender kalau berkasnya memang
                            // berbeda — web mengisi keduanya dengan foto yang sama.
                            if (item.fotoTerpisah) {
                                Thumbnail(item.fotoTimbanganUrl!!, "Fisik") {
                                    viewModel.lihatFoto(
                                        "Bukti Fisik: ${item.namaBahan ?: "Bahan"}",
                                        item.fotoTimbanganUrl,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = state.catatanManager,
                onValueChange = viewModel::ubahCatatanManager,
                label = { Text("Catatan (wajib bila menolak)", fontSize = 13.sp) },
                minLines = 2,
                textStyle = TipeIos.Keterangan,
                shape = UkuranIos.SudutKontrol,
                colors = warnaKolomIos(),
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                "Menolak tidak mengembalikan stok: potongan yang sudah tercatat dialihkan resmi " +
                    "menjadi beban waste outlet, dengan alasan di atas sebagai keterangannya.",
                style = TipeIos.Catatan.copy(lineHeight = 18.sp),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TombolLembar(
                    teks = "Tolak Klaim",
                    warna = WarnaIos.Merah,
                    isian = false,
                    sibuk = state.memproses,
                    modifier = Modifier.weight(1f),
                ) { viewModel.putuskanManager(false) }
                TombolLembar(
                    teks = "Setujui",
                    warna = WarnaIos.Hijau,
                    isian = true,
                    sibuk = state.memproses,
                    modifier = Modifier.weight(1f),
                ) { viewModel.putuskanManager(true) }
            }
        }
    }
}

// ================================================= lembar serah terima kurir

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LembarSerahKurir(state: ReturUiState, viewModel: ReturViewModel) {
    val tiket = state.tiketAktif ?: return
    val mintaFoto = ingatPemintaKamera(viewModel)

    ModalBottomSheet(
        onDismissRequest = viewModel::tutupLembar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = WarnaIos.Kartu,
    ) {
        Column(
            Modifier.padding(horizontal = UkuranIos.TepiLayar).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            JudulLembar("Serahkan Fisik ke Kurir", "Tiket ${tiket.nomorRetur}")

            PilihanTurun(
                nilai = state.kurirJenis.label,
                pilihan = JenisLogistik.entries.map { it.nilai to it.label },
                onPilih = { viewModel.pilihLogistik(JenisLogistik.dari(it)) },
            )

            if (state.kurirJenis.pihakKetiga) {
                OutlinedTextField(
                    value = state.kurirResi,
                    onValueChange = viewModel::ubahKurirResi,
                    label = { Text("Nomor Order / Resi ${state.kurirJenis.label} *", fontSize = 13.sp) },
                    singleLine = true,
                    textStyle = TipeIos.Keterangan,
                    shape = UkuranIos.SudutKontrol,
                    colors = warnaKolomIos(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            OutlinedTextField(
                value = state.kurirNama,
                onValueChange = viewModel::ubahKurirNama,
                label = { Text("Nama Supir / Kurir *", fontSize = 13.sp) },
                singleLine = true,
                textStyle = TipeIos.Keterangan,
                shape = UkuranIos.SudutKontrol,
                colors = warnaKolomIos(),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = state.kurirKontak,
                    onValueChange = viewModel::ubahKurirKontak,
                    label = { Text("No. HP", fontSize = 13.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    textStyle = TipeIos.Keterangan,
                    shape = UkuranIos.SudutKontrol,
                    colors = warnaKolomIos(),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = state.kurirPlat,
                    onValueChange = viewModel::ubahKurirPlat,
                    label = { Text("Plat Nomor", fontSize = 13.sp) },
                    singleLine = true,
                    textStyle = TipeIos.Keterangan,
                    shape = UkuranIos.SudutKontrol,
                    colors = warnaKolomIos(),
                    modifier = Modifier.weight(1f),
                )
            }

            BagianFotoRetur(
                url = state.kurirFotoUrl,
                mengunggah = state.mengunggahFoto,
                keterangan = "Foto paket saat diserahkan ke kurir (opsional, tapi sangat membantu bila paket hilang).",
                onAmbil = mintaFoto,
            )

            TombolLembar(
                teks = "Catat Serah Terima",
                warna = WarnaIos.Biru,
                isian = true,
                sibuk = state.memproses || state.mengunggahFoto,
                modifier = Modifier.fillMaxWidth(),
            ) { viewModel.kirimSerahTerima() }
        }
    }

    LembarKamera(state, viewModel, "Foto serah terima kurir")
}

// ================================================ lembar verifikasi kitchen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LembarVerifikasiKitchen(state: ReturUiState, viewModel: ReturViewModel) {
    val tiket = state.tiketAktif ?: return
    val sudahDiterima = tiket.status == StatusRetur.DITERIMA_KITCHEN

    ModalBottomSheet(
        onDismissRequest = viewModel::tutupLembar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = WarnaIos.Kartu,
    ) {
        Column(
            Modifier.padding(horizontal = UkuranIos.TepiLayar).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            JudulLembar(
                if (sudahDiterima) "Terbitkan Surat Jalan Pengganti" else "Verifikasi Fisik & Opsi Pengiriman",
                "Tiket ${tiket.nomorRetur} · ${tiket.outletName ?: "Outlet"}",
            )

            if (sudahDiterima) {
                BannerIos(
                    "Fisik sudah diterima dan ditimbang sebelumnya. Langkah berikutnya hanya menerbitkan Surat Jalan Pengganti.",
                    NadaIos.AKSEN,
                    ikon = IkonIos.CheckCircle,
                )
            }

            tiket.items.forEach { item ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "${item.namaBahan ?: "Bahan"} — klaim outlet ${qtyTeks(item.qtyKlaim, item.satuan)}",
                        style = TipeIos.Keterangan.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                    )
                    OutlinedTextField(
                        value = state.timbangKitchen[item.id].orEmpty(),
                        onValueChange = { viewModel.ubahTimbang(item.id, it) },
                        label = {
                            Text("Timbang ulang gudang (${formatSatuan(item.satuan)})", fontSize = 13.sp)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        textStyle = TipeIos.Keterangan,
                        shape = UkuranIos.SudutKontrol,
                        colors = warnaKolomIos(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            OutlinedTextField(
                value = state.catatanKitchen,
                onValueChange = viewModel::ubahCatatanKitchen,
                label = { Text("Catatan Gudang Pusat", fontSize = 13.sp) },
                minLines = 2,
                textStyle = TipeIos.Keterangan,
                shape = UkuranIos.SudutKontrol,
                colors = warnaKolomIos(),
                modifier = Modifier.fillMaxWidth(),
            )

            if (!sudahDiterima) {
                Text("Kapan barang pengganti dikirim?", style = TipeIos.Keterangan.copy(fontWeight = FontWeight.SemiBold))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PilihanWaktu(
                        judul = "Kirim Sekarang",
                        keterangan = "Terbitkan SJ Pengganti saat ini juga.",
                        terpilih = state.kirimSekarang,
                        modifier = Modifier.weight(1f),
                    ) { viewModel.pilihWaktuKirim(true) }
                    PilihanWaktu(
                        judul = "Gabung Nanti",
                        keterangan = "Simpan timbangan; pengganti menumpang kiriman reguler.",
                        terpilih = !state.kirimSekarang,
                        modifier = Modifier.weight(1f),
                    ) { viewModel.pilihWaktuKirim(false) }
                }
            }

            TombolLembar(
                teks = if (sudahDiterima || state.kirimSekarang) "Terbitkan SJ Pengganti" else "Simpan Hasil Timbang",
                warna = if (sudahDiterima || state.kirimSekarang) WarnaIos.Biru else WarnaIos.Ungu,
                isian = true,
                sibuk = state.memproses,
                modifier = Modifier.fillMaxWidth(),
            ) { viewModel.kirimVerifikasiKitchen() }
        }
    }
}

// ================================================================== pembantu

@Composable
private fun JudulLembar(judul: String, sub: String) {
    Column {
        Text(judul, style = TipeIos.Judul3.copy(fontWeight = FontWeight.Bold))
        Text(sub, style = TipeIos.Catatan)
    }
}

@Composable
private fun KartuForm(judul: String, wajib: Boolean, isi: @Composable () -> Unit) {
    Column {
        LabelSeksiIos(if (wajib) "$judul *" else judul, Modifier.padding(start = 16.dp, bottom = 7.dp))
        KartuIos(padding = PaddingValues(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { isi() }
        }
    }
}

@Composable
private fun PilihanTurun(nilai: String, pilihan: List<Pair<String, String>>, onPilih: (String) -> Unit) {
    var terbuka by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = UkuranIos.TinggiKontrol + 4.dp)
                .clip(UkuranIos.SudutKontrol)
                .background(WarnaIos.Isian)
                .tekanIos({ terbuka = true })
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(nilai, Modifier.weight(1f), style = TipeIos.Keterangan.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold))
            Icon(IkonIos.ArrowDropDown, null, tint = WarnaIos.Aksen, modifier = Modifier.size(16.dp))
        }
        SukaDropdownMenu(expanded = terbuka, onDismissRequest = { terbuka = false }) {
            SukaDropdownHeader(title = "PILIH OPSI", onClose = { terbuka = false })
            pilihan.forEach { (kunci, label) ->
                SukaDropdownMenuItem(
                    text = label,
                    selected = (label == nilai),
                    onClick = { terbuka = false; onPilih(kunci) },
                )
            }
        }
    }
}

@Composable
private fun KolomAngka(label: String, nilai: String, onUbah: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = nilai,
        onValueChange = onUbah,
        label = { Text(label, fontSize = 12.sp) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        textStyle = TipeIos.Isi,
        shape = UkuranIos.SudutKontrol,
        colors = warnaKolomIos(),
        modifier = modifier,
    )
}

@Composable
private fun BagianFotoRetur(url: String?, mengunggah: Boolean, keterangan: String, onAmbil: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(UkuranIos.SudutBlok)
            .background(WarnaIos.Latar),
        contentAlignment = Alignment.Center,
    ) {
        when {
            mengunggah -> CircularProgressIndicator(Modifier.size(26.dp), color = WarnaIos.Abu, strokeWidth = 2.5.dp)
            url != null -> AsyncImage(
                model = url,
                contentDescription = "Bukti foto retur",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(IkonIos.PhotoCamera, null, tint = WarnaIos.Abu, modifier = Modifier.size(28.dp))
                Spacer(Modifier.height(6.dp))
                Text("Belum ada foto", style = TipeIos.Catatan)
            }
        }
    }
    Text(keterangan, style = TipeIos.Catatan.copy(lineHeight = 18.sp))
    if (url != null) {
        TombolKeduaIos(
            "Foto tersimpan — ambil ulang",
            onAmbil,
            Modifier.height(44.dp),
            aktif = !mengunggah,
            ikon = IkonIos.CheckCircle,
            warna = WarnaIos.Hijau,
        )
    } else {
        TombolUtamaIos(
            "Ambil foto sekarang",
            onAmbil,
            Modifier.height(44.dp),
            aktif = !mengunggah,
            ikon = IkonIos.PhotoCamera,
        )
    }
}

@Composable
private fun PilihanWaktu(
    judul: String,
    keterangan: String,
    terpilih: Boolean,
    modifier: Modifier = Modifier,
    onKlik: () -> Unit,
) {
    Column(
        modifier
            .clip(UkuranIos.SudutBlok)
            .background(if (terpilih) WarnaIos.Aksen.copy(alpha = 0.12f) else WarnaIos.Latar)
            .tekanIos(onKlik)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                judul,
                Modifier.weight(1f),
                style = TipeIos.Keterangan.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (terpilih) AmberTua else WarnaIos.Label,
                ),
            )
            if (terpilih) Icon(IkonIos.CheckCircle, "Terpilih", tint = WarnaIos.Aksen, modifier = Modifier.size(18.dp))
        }
        Text(keterangan, style = TipeIos.Kecil.copy(lineHeight = 15.sp))
    }
}

@Composable
private fun TombolLembar(
    teks: String,
    warna: Color,
    isian: Boolean,
    sibuk: Boolean,
    modifier: Modifier = Modifier,
    onKlik: () -> Unit,
) {
    if (isian) {
        TombolUtamaIos(teks, onKlik, modifier, aktif = !sibuk, memuat = sibuk, warna = warna)
    } else {
        TombolKeduaIos(teks, onKlik, modifier, aktif = !sibuk, warna = warna)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LembarKamera(state: ReturUiState, viewModel: ReturViewModel, judul: String) {
    if (!state.kameraTerbuka) return
    ModalBottomSheet(
        onDismissRequest = viewModel::tutupKamera,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = WarnaIos.Kartu,
    ) {
        Column(Modifier.padding(horizontal = UkuranIos.TepiLayar).padding(bottom = 24.dp)) {
            Text(judul, style = TipeIos.Utama)
            Spacer(Modifier.height(10.dp))
            KameraFotoSheet(
                onDiambil = viewModel::simpanFoto,
                onBatal = viewModel::tutupKamera,
            )
        }
    }
}
