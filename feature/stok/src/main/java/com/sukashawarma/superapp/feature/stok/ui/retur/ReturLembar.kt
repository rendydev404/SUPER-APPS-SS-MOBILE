package com.sukashawarma.superapp.feature.stok.ui.retur

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.CircularProgressIndicator
import com.sukashawarma.superapp.core.ui.SukaDropdownHeader
import com.sukashawarma.superapp.core.ui.SukaDropdownMenu
import com.sukashawarma.superapp.core.ui.SukaDropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextAlign
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

// ============================================================ formulir klaim

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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
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
                            color = MerahTua, fontSize = 10.sp,
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
                            color = AmberTua, fontSize = 12.sp, fontWeight = FontWeight.Black,
                        )
                        Text(
                            "Stok outlet akan dipotong sebesar angka ini saat formulir diajukan.",
                            color = Abu500, fontSize = 10.sp, lineHeight = 14.sp,
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
                            placeholder = {
                                Text("Tuliskan alasan retur…", fontSize = 12.sp, color = Abu500)
                            },
                            singleLine = true,
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
                        placeholder = {
                            Text("Keterangan untuk Area Manager & Central Kitchen…", fontSize = 12.sp, color = Abu500)
                        },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            item {
                Surface(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = AmberMuda,
                    border = BorderStroke(1.dp, AmberGaris),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Prosedur Pengembalian", color = AmberTua, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        Text(
                            "Setelah dikirim, pengajuan direview Area Manager / Regional Manager. " +
                                "Jangan menyerahkan daging atau kulit ke kurir sebelum tiket berstatus \"Disetujui AM/RM\".",
                            color = AmberTua, fontSize = 10.sp, lineHeight = 14.sp,
                        )
                    }
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
    Surface(Modifier.fillMaxWidth(), color = Color.White, shadowElevation = 8.dp) {
        Column(Modifier.padding(16.dp)) {
            if (halangan != null) {
                Text(halangan, color = Abu500, fontSize = 10.5.sp, lineHeight = 14.sp)
                Spacer(Modifier.height(8.dp))
            }
            Surface(
                onClick = viewModel::kirimKlaim,
                enabled = halangan == null && !state.memproses && !state.mengunggahFoto,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = if (halangan == null && !state.memproses) AmberTua else Abu200,
            ) {
                Box(Modifier.padding(vertical = 13.dp), contentAlignment = Alignment.Center) {
                    if (state.memproses) {
                        CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text(
                            "Kirim Pengajuan Retur",
                            color = if (halangan == null) Color.White else Abu500,
                            fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
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
        containerColor = Color.White,
    ) {
        Column(
            Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp).heightIn(max = 620.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            JudulLembar("Persetujuan AM / RM", "Validasi Klaim ${tiket.nomorRetur} · ${tiket.outletName ?: "Outlet"}")

            tiket.items.forEach { item ->
                Surface(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = LatarRetur,
                    border = BorderStroke(1.dp, AmberGaris.copy(alpha = 0.6f)),
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "${item.namaBahan ?: "Bahan Baku"} — ${qtyTeks(item.qtyKlaim, item.satuan)}",
                            color = Abu900, fontSize = 13.sp, fontWeight = FontWeight.Black,
                        )
                        Text("Alasan: ${item.alasan}", color = AmberTua, fontSize = 11.sp)
                        if (!item.catatan.isNullOrBlank()) {
                            Text("\"${item.catatan}\"", color = Abu500, fontSize = 10.sp)
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
            }

            OutlinedTextField(
                value = state.catatanManager,
                onValueChange = viewModel::ubahCatatanManager,
                label = { Text("Catatan (wajib bila menolak)", fontSize = 11.sp) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                "Menolak tidak mengembalikan stok: potongan yang sudah tercatat dialihkan resmi " +
                    "menjadi beban waste outlet, dengan alasan di atas sebagai keterangannya.",
                color = Abu500, fontSize = 10.sp, lineHeight = 14.sp,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TombolLembar(
                    teks = "Tolak Klaim",
                    warna = MerahTua,
                    isian = false,
                    sibuk = state.memproses,
                    modifier = Modifier.weight(1f),
                ) { viewModel.putuskanManager(false) }
                TombolLembar(
                    teks = "Setujui",
                    warna = HijauTua,
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
        containerColor = Color.White,
    ) {
        Column(
            Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp),
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
                    label = { Text("Nomor Order / Resi ${state.kurirJenis.label} *", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            OutlinedTextField(
                value = state.kurirNama,
                onValueChange = viewModel::ubahKurirNama,
                label = { Text("Nama Supir / Kurir *", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = state.kurirKontak,
                    onValueChange = viewModel::ubahKurirKontak,
                    label = { Text("No. HP", fontSize = 11.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = state.kurirPlat,
                    onValueChange = viewModel::ubahKurirPlat,
                    label = { Text("Plat Nomor", fontSize = 11.sp) },
                    singleLine = true,
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
                warna = BiruTua,
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
        containerColor = Color.White,
    ) {
        Column(
            Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            JudulLembar(
                if (sudahDiterima) "Terbitkan Surat Jalan Pengganti" else "Verifikasi Fisik & Opsi Pengiriman",
                "Tiket ${tiket.nomorRetur} · ${tiket.outletName ?: "Outlet"}",
            )

            if (sudahDiterima) {
                Surface(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = AmberMuda,
                    border = BorderStroke(1.dp, AmberGaris),
                ) {
                    Text(
                        "Fisik sudah diterima dan ditimbang sebelumnya. Langkah berikutnya hanya menerbitkan Surat Jalan Pengganti.",
                        Modifier.padding(11.dp),
                        color = AmberTua, fontSize = 10.5.sp, lineHeight = 14.sp,
                    )
                }
            }

            tiket.items.forEach { item ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "${item.namaBahan ?: "Bahan"} — klaim outlet ${qtyTeks(item.qtyKlaim, item.satuan)}",
                        color = Abu900, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    )
                    OutlinedTextField(
                        value = state.timbangKitchen[item.id].orEmpty(),
                        onValueChange = { viewModel.ubahTimbang(item.id, it) },
                        label = {
                            Text("Timbang ulang gudang (${formatSatuan(item.satuan)})", fontSize = 11.sp)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            OutlinedTextField(
                value = state.catatanKitchen,
                onValueChange = viewModel::ubahCatatanKitchen,
                label = { Text("Catatan Gudang Pusat", fontSize = 11.sp) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            if (!sudahDiterima) {
                Text("Kapan barang pengganti dikirim?", color = Abu900, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                warna = if (sudahDiterima || state.kirimSekarang) BiruTua else UnguTua,
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
        Text(judul, color = Abu900, fontSize = 15.sp, fontWeight = FontWeight.Black)
        Text(sub, color = Abu500, fontSize = 11.sp)
    }
}

@Composable
private fun KartuForm(judul: String, wajib: Boolean, isi: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            if (wajib) "$judul *" else judul,
            color = Abu500, fontSize = 10.5.sp, fontWeight = FontWeight.Black,
        )
        Surface(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Abu200),
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) { isi() }
        }
    }
}

@Composable
private fun PilihanTurun(nilai: String, pilihan: List<Pair<String, String>>, onPilih: (String) -> Unit) {
    var terbuka by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        Surface(
            onClick = { terbuka = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Abu200),
        ) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(nilai, Modifier.weight(1f), color = Abu900, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.Default.ArrowDropDown, null, tint = Abu500, modifier = Modifier.size(18.dp))
            }
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
        label = { Text(label, fontSize = 10.sp) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}

@Composable
private fun BagianFotoRetur(url: String?, mengunggah: Boolean, keterangan: String, onAmbil: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(150.dp)
            .background(LatarRetur, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center,
    ) {
        when {
            mengunggah -> CircularProgressIndicator(color = AmberTua)
            url != null -> AsyncImage(
                model = url,
                contentDescription = "Bukti foto retur",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().background(LatarRetur, RoundedCornerShape(14.dp)),
            )
            else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.PhotoCamera, null, tint = Abu500, modifier = Modifier.size(26.dp))
                Spacer(Modifier.height(6.dp))
                Text("Belum ada foto", color = Abu500, fontSize = 11.sp)
            }
        }
    }
    Text(keterangan, color = Abu500, fontSize = 10.sp, lineHeight = 14.sp)
    Surface(
        onClick = onAmbil,
        enabled = !mengunggah,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (url != null) HijauTua.copy(alpha = 0.1f) else AmberTua,
        border = if (url != null) BorderStroke(1.dp, HijauTua.copy(alpha = 0.4f)) else null,
    ) {
        Row(
            Modifier.padding(vertical = 11.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (url != null) Icons.Default.CheckCircle else Icons.Default.PhotoCamera,
                null,
                tint = if (url != null) HijauTua else Color.White,
                modifier = Modifier.size(17.dp),
            )
            Spacer(Modifier.width(7.dp))
            Text(
                if (url != null) "Foto tersimpan — ambil ulang" else "Ambil foto sekarang",
                color = if (url != null) HijauTua else Color.White,
                fontSize = 12.sp, fontWeight = FontWeight.Bold,
            )
        }
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
    Surface(
        onClick = onKlik,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = if (terpilih) AmberMuda else Color.White,
        border = BorderStroke(if (terpilih) 2.dp else 1.dp, if (terpilih) AmberTua else Abu200),
    ) {
        Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(judul, color = Abu900, fontSize = 12.sp, fontWeight = FontWeight.Black)
            Text(keterangan, color = Abu500, fontSize = 9.5.sp, lineHeight = 13.sp)
        }
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
    Surface(
        onClick = onKlik,
        enabled = !sibuk,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = if (isian) warna else warna.copy(alpha = 0.08f),
        border = if (isian) null else BorderStroke(1.dp, warna.copy(alpha = 0.4f)),
    ) {
        Box(Modifier.padding(vertical = 13.dp), contentAlignment = Alignment.Center) {
            if (sibuk) {
                CircularProgressIndicator(
                    Modifier.size(18.dp),
                    color = if (isian) Color.White else warna,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    teks,
                    color = if (isian) Color.White else warna,
                    fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LembarKamera(state: ReturUiState, viewModel: ReturViewModel, judul: String) {
    if (!state.kameraTerbuka) return
    ModalBottomSheet(
        onDismissRequest = viewModel::tutupKamera,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
    ) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Text(judul, color = Abu900, fontSize = 14.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(10.dp))
            KameraFotoSheet(
                onDiambil = viewModel::simpanFoto,
                onBatal = viewModel::tutupKamera,
            )
        }
    }
}
