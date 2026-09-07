package com.sukashawarma.superapp.feature.stok.ui.entri

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.sukashawarma.superapp.core.camera.KameraFotoSheet
import com.sukashawarma.superapp.feature.stok.data.EntriManualRepository
import com.sukashawarma.superapp.feature.stok.data.model.MonitoringRow
import com.sukashawarma.superapp.feature.stok.ui.HeaderStok

private val ORANGE = Color(0xFFEA580C)
private val SLATE400 = Color(0xFF94A3B8)
private val SLATE500 = Color(0xFF64748B)
private val SLATE900 = Color(0xFF0F172A)
private val GARIS = Color(0xFFE2E8F0)
private val HIJAU = Color(0xFF15803D)
private val MERAH = Color(0xFFB91C1C)

/**
 * Entri manual stok — cermin `ManualEntryForm.tsx` web.
 *
 * Tiga jenis dalam satu layar, dan perbedaannya bukan kosmetik: waste berhenti di
 * antrean persetujuan, sedangkan penyesuaian dan transfer keluar langsung memotong
 * saldo. Teks di bawah tombol kirim menyebutkan itu supaya pengguna tahu apa yang
 * akan terjadi sebelum menekannya.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntriManualScreen(
    onBack: () -> Unit,
    viewModel: EntriManualViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val konteks = LocalContext.current
    val lembarKamera = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var menungguIzin by remember { mutableStateOf(false) }
    val pemintaIzin = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { diberikan ->
        val diminta = menungguIzin
        menungguIzin = false
        if (diberikan && diminta) viewModel.bukaKamera()
    }

    fun mintaFoto() {
        val ada = ContextCompat.checkSelfPermission(konteks, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (ada) {
            viewModel.bukaKamera()
        } else {
            menungguIzin = true
            pemintaIzin.launch(Manifest.permission.CAMERA)
        }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFFF8FAFC))) {
        HeaderStok(
            judul = "Entri Manual Stok",
            subjudul = "Penyesuaian, waste & transfer keluar",
            onKembali = onBack,
        )

        if (state.memuat) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ORANGE)
            }
        } else {
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item { PemilihOutlet(state, viewModel) }
                item { PemilihJenis(state, viewModel) }
                item { PemilihBahan(state, viewModel) }

                if (state.bahanTerpilih != null) {
                    item { KolomJumlah(state, viewModel) }
                    if (state.butuhAlasan) item { KolomAlasan(state, viewModel) }
                    if (state.jenis != JenisEntri.WASTE) {
                        item { KolomCatatan(state, viewModel) }
                    }
                    if (state.butuhFoto) {
                        item { BagianFoto(state, ::mintaFoto) }
                    }
                }

                item { Kirim(state, viewModel) }

                val pesan = state.pesan ?: state.error
                if (pesan != null) {
                    item {
                        Text(
                            pesan,
                            color = if (state.pesan != null) HIJAU else MERAH,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }

    if (state.kameraTerbuka) {
        ModalBottomSheet(
            onDismissRequest = viewModel::tutupKamera,
            sheetState = lembarKamera,
            containerColor = Color.White,
        ) {
            Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
                Text(
                    "Foto bukti waste",
                    color = SLATE900,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.height(10.dp))
                KameraFotoSheet(
                    onDiambil = viewModel::simpanFoto,
                    onBatal = viewModel::tutupKamera,
                )
            }
        }
    }

    // Pesan sukses tidak boleh menetap sampai entri berikutnya — itu membuat
    // pengguna mengira entri kedua sudah tersimpan padahal belum ditekan.
    LaunchedEffect(state.pesan) {
        if (state.pesan != null) {
            kotlinx.coroutines.delay(4000)
            viewModel.bersihkanPesan()
        }
    }
}

@Composable
private fun Kartu(judul: String, isi: @Composable () -> Unit) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, GARIS),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                judul.uppercase(),
                color = SLATE400,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.7.sp,
            )
            Spacer(Modifier.height(9.dp))
            isi()
        }
    }
}

@Composable
private fun PemilihOutlet(state: EntriManualUiState, viewModel: EntriManualViewModel) {
    var terbuka by remember { mutableStateOf(false) }
    Kartu("Outlet") {
        Box {
            Surface(
                Modifier.fillMaxWidth().clickable(enabled = state.outlets.size > 1) { terbuka = true },
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, GARIS),
            ) {
                Row(
                    Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        state.outletTerpilih?.name ?: "Pilih outlet",
                        Modifier.weight(1f),
                        color = SLATE900,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (state.outlets.size > 1) {
                        Icon(Icons.Default.ArrowDropDown, null, tint = SLATE500)
                    }
                }
            }
            DropdownMenu(terbuka, { terbuka = false }) {
                state.outlets.forEach { outlet ->
                    DropdownMenuItem(
                        text = { Text(outlet.name, fontSize = 13.sp) },
                        onClick = { terbuka = false; viewModel.pilihOutlet(outlet) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PemilihJenis(state: EntriManualUiState, viewModel: EntriManualViewModel) {
    Kartu("Jenis entri") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            JenisEntri.entries.forEach { jenis ->
                val aktif = jenis == state.jenis
                Surface(
                    Modifier.weight(1f).clickable { viewModel.pilihJenis(jenis) },
                    shape = RoundedCornerShape(11.dp),
                    color = if (aktif) ORANGE else Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, if (aktif) ORANGE else GARIS),
                ) {
                    Text(
                        jenis.label,
                        Modifier.padding(vertical = 10.dp, horizontal = 4.dp).fillMaxWidth(),
                        color = if (aktif) Color.White else SLATE500,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Spacer(Modifier.height(9.dp))
        Text(
            when (state.jenis) {
                JenisEntri.PENYESUAIAN -> "Mengoreksi saldo tanpa opname. Langsung mengubah stok."
                JenisEntri.WASTE -> "Masuk antrean persetujuan. Stok baru dipotong setelah disetujui."
                JenisEntri.TRANSFER_KELUAR -> "Mengurangi stok outlet ini. Langsung mengubah saldo."
            },
            color = SLATE500,
            fontSize = 11.5.sp,
            lineHeight = 16.sp,
        )
    }
}

@Composable
private fun PemilihBahan(state: EntriManualUiState, viewModel: EntriManualViewModel) {
    Kartu("Bahan baku") {
        val terpilih = state.bahanTerpilih
        if (terpilih != null) {
            Surface(
                Modifier.fillMaxWidth().clickable { viewModel.pilihBahan(null) },
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFFF7ED),
                border = BorderStroke(1.dp, Color(0xFFFED7AA)),
            ) {
                Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            terpilih.itemName,
                            color = SLATE900,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "Saldo saat ini: ${terpilih.saldoRingkas()}",
                            color = SLATE500,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    Text("GANTI", color = ORANGE, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
        } else {
            OutlinedTextField(
                value = state.cari,
                onValueChange = viewModel::ubahCari,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Cari bahan…", fontSize = 12.5.sp, color = SLATE400) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = ORANGE,
                    unfocusedBorderColor = GARIS,
                ),
            )
            Spacer(Modifier.height(8.dp))
            val hasil = state.bahanTampil.take(12)
            if (hasil.isEmpty()) {
                Text("Tidak ada bahan yang cocok.", color = SLATE400, fontSize = 12.sp)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    hasil.forEach { row -> BarisPilihBahan(row) { viewModel.pilihBahan(row) } }
                }
                if (state.bahanTampil.size > hasil.size) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${state.bahanTampil.size - hasil.size} bahan lain — persempit dengan pencarian.",
                        color = SLATE400,
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun BarisPilihBahan(row: MonitoringRow, onKlik: () -> Unit) {
    Surface(
        Modifier.fillMaxWidth().clickable(onClick = onKlik),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFF8FAFC),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                row.itemName,
                Modifier.weight(1f),
                color = SLATE900,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(row.saldoRingkas(), color = SLATE500, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun KolomJumlah(state: EntriManualUiState, viewModel: EntriManualViewModel) {
    val meta = state.bahanTerpilih?.meta
    Kartu("Jumlah") {
        // Satuan yang faktornya tidak ada tidak ditawarkan: menawarkannya berarti
        // membiarkan pengguna memilih konversi yang tidak bisa dihitung.
        val pilihan = buildList {
            add(SatuanInput.BESAR to (meta?.satuan ?: "satuan"))
            if (meta?.satuanTengah != null && meta.faktorTengah != null) {
                add(SatuanInput.TENGAH to meta.satuanTengah!!)
            }
            if (meta?.satuanKecil != null && meta.faktorTampilan != null) {
                add(SatuanInput.KECIL to meta.satuanKecil!!)
            }
        }
        if (pilihan.size > 1) {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                pilihan.forEach { (satuan, label) ->
                    val aktif = satuan == state.satuanInput
                    Surface(
                        Modifier.clickable { viewModel.pilihSatuanInput(satuan) },
                        shape = RoundedCornerShape(50),
                        color = if (aktif) Color(0xFFFFF7ED) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (aktif) ORANGE else GARIS),
                    ) {
                        Text(
                            label,
                            Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
                            color = if (aktif) ORANGE else SLATE500,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        OutlinedTextField(
            value = state.jumlah,
            onValueChange = viewModel::ubahJumlah,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("0", fontSize = 13.sp, color = SLATE400) },
            suffix = { Text(state.labelSatuan, color = SLATE500, fontSize = 12.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = ORANGE,
                unfocusedBorderColor = GARIS,
            ),
        )
        val besar = state.jumlahBesar
        if (besar != null && state.satuanInput != SatuanInput.BESAR) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Tercatat sebagai ${String.format(java.util.Locale.US, "%.4f", besar).trimEnd('0').trimEnd('.')} ${meta?.satuan.orEmpty()}",
                color = SLATE500,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun KolomAlasan(state: EntriManualUiState, viewModel: EntriManualViewModel) {
    Kartu(if (state.jenis == JenisEntri.WASTE) "Alasan waste" else "Alasan penyesuaian") {
        if (state.jenis == JenisEntri.WASTE) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                EntriManualRepository.ALASAN_WASTE.forEach { alasan ->
                    val aktif = alasan == state.alasan
                    Surface(
                        Modifier.fillMaxWidth().clickable { viewModel.ubahAlasan(alasan) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (aktif) Color(0xFFFFF7ED) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (aktif) ORANGE else GARIS),
                    ) {
                        Text(
                            alasan,
                            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            color = if (aktif) ORANGE else SLATE500,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        } else {
            OutlinedTextField(
                value = state.alasan,
                onValueChange = viewModel::ubahAlasan,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Contoh: koreksi salah input opname", fontSize = 12.sp, color = SLATE400) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = ORANGE,
                    unfocusedBorderColor = GARIS,
                ),
            )
        }
    }
}

@Composable
private fun KolomCatatan(state: EntriManualUiState, viewModel: EntriManualViewModel) {
    Kartu("Keterangan tambahan") {
        OutlinedTextField(
            value = state.catatan,
            onValueChange = viewModel::ubahCatatan,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Opsional", fontSize = 12.sp, color = SLATE400) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = ORANGE,
                unfocusedBorderColor = GARIS,
            ),
        )
    }
}

@Composable
private fun BagianFoto(state: EntriManualUiState, onMintaFoto: () -> Unit) {
    Kartu("Foto bukti") {
        val url = state.fotoUrl
        Box(
            Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            when {
                state.mengunggahFoto -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 3.dp, color = ORANGE)
                    Spacer(Modifier.height(8.dp))
                    Text("Mengunggah…", color = SLATE500, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                url != null -> AsyncImage(
                    model = url,
                    contentDescription = "Foto bukti waste",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                )
                else -> Text("Belum ada foto", color = SLATE400, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = onMintaFoto,
            enabled = !state.mengunggahFoto,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        ) {
            Icon(
                if (url != null) Icons.Default.CheckCircle else Icons.Default.PhotoCamera,
                null,
                tint = if (url != null) HIJAU else ORANGE,
                modifier = Modifier.size(17.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (url != null) "Foto tersimpan — ambil ulang" else "Ambil foto bukti (wajib)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (url != null) HIJAU else SLATE900,
            )
        }
    }
}

@Composable
private fun Kirim(state: EntriManualUiState, viewModel: EntriManualViewModel) {
    Column {
        Button(
            onClick = viewModel::kirim,
            enabled = state.halangan == null && !state.menyimpan && !state.mengunggahFoto,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ORANGE),
        ) {
            Text(
                when {
                    state.menyimpan -> "Menyimpan…"
                    state.jenis == JenisEntri.WASTE -> "Kirim laporan waste"
                    else -> "Simpan ${state.jenis.label.lowercase()}"
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
            )
        }
        val halangan = state.halangan
        if (halangan != null) {
            Spacer(Modifier.height(8.dp))
            Text(halangan, color = SLATE500, fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
        }
    }
}
