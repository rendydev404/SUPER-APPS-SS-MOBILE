package com.sukashawarma.superapp.feature.stok.ui.vendor

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.core.camera.KameraFotoSheet
import com.sukashawarma.superapp.core.camera.keJpeg
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.feature.stok.data.CatatanTerimaVendor
import com.sukashawarma.superapp.feature.stok.domain.SatuanTingkat
import com.sukashawarma.superapp.feature.stok.domain.StokAkses
import com.sukashawarma.superapp.feature.stok.ui.HeaderStok
import com.sukashawarma.superapp.feature.stok.ui.PitaPesan
import com.sukashawarma.superapp.presentation.theme.SukaBrown
import com.sukashawarma.superapp.presentation.theme.SukaCream
import com.sukashawarma.superapp.presentation.theme.SukaGreen
import com.sukashawarma.superapp.presentation.theme.SukaOnSurface
import com.sukashawarma.superapp.presentation.theme.SukaOrange
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private val LATAR_HALAMAN = Color(0xFFFFF8F1)
private val BORDER_KARTU = Color(0xFFD9C2B2).copy(alpha = 0.5f)
private val TEKS_LABEL = Color(0xFF544437)

private fun formatRupiah(nilai: Double): String {
    val simbol = DecimalFormatSymbols(Locale("id", "ID"))
    val format = DecimalFormat("#,###", simbol)
    return "Rp " + format.format(nilai.toLong())
}

private fun formatAngka(nilai: Double): String {
    val simbol = DecimalFormatSymbols(Locale("id", "ID"))
    val format = DecimalFormat("#,##0.###", simbol)
    return format.format(nilai)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerimaVendorScreen(
    onBack: () -> Unit,
    viewModel: TerimaVendorViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val staff by AppSession.staff.collectAsState()
    val outletId = staff?.outletId
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

    Column(Modifier.fillMaxSize().background(LATAR_HALAMAN)) {
        HeaderStok(
            judul = "Terima dari Vendor",
            subjudul = "Catat sayur/bahan yang diantar langsung vendor ke outlet",
            onKembali = onBack,
            aksi = {
                IconButton(onClick = viewModel::muatRiwayat) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Muat Ulang Riwayat",
                        tint = Color(0xFF1E293B),
                    )
                }
            },
        )

        // Banner pesan
        state.pesanSukses?.let {
            PitaPesan(pesan = it, gagal = false, onTutup = viewModel::bersihkanPesan)
        }
        state.pesanError?.let {
            PitaPesan(pesan = it, gagal = true, onTutup = viewModel::bersihkanPesan)
        }
        state.pesanPeringatan?.let {
            PitaPesan(pesan = it, gagal = false, onTutup = viewModel::bersihkanPesan)
        }

        if (!StokAkses.bisaTerimaVendor(outletId)) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    "Akun tidak terhubung ke outlet mana pun.",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }
            return@Column
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                FormTerimaVendor(
                    state = state,
                    viewModel = viewModel,
                    onMintaFoto = ::mintaFoto,
                )
            }

            item {
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "7 HARI TERAKHIR",
                        color = TEKS_LABEL,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp,
                    )
                    if (state.memuatRiwayat) {
                        CircularProgressIndicator(
                            color = SukaBrown,
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                }
            }

            if (state.riwayat.isEmpty() && !state.memuatRiwayat) {
                item {
                    Surface(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, BORDER_KARTU),
                    ) {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text(
                                "Belum ada catatan 7 hari terakhir.",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            } else {
                items(state.riwayat, key = { it.id }) { catatan ->
                    KartuRiwayatTerima(
                        catatan = catatan,
                        onKoreksi = { viewModel.bukaDialogKoreksi(catatan) },
                    )
                }
            }
        }
    }

    // Modal dialog kamera
    if (state.kameraTerbuka) {
        ModalBottomSheet(
            onDismissRequest = viewModel::tutupKamera,
            sheetState = lembarKamera,
            containerColor = Color.White,
        ) {
            Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
                Text(
                    "Foto Bukti Terima / Timbangan",
                    color = SukaOnSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.height(10.dp))
                KameraFotoSheet(
                    onDiambil = { bitmap ->
                        viewModel.simpanFoto(bitmap.keJpeg())
                    },
                    onBatal = viewModel::tutupKamera,
                )
            }
        }
    }

    // Modal dialog koreksi kuantitas
    state.koreksiTarget?.let { target ->
        AlertDialog(
            onDismissRequest = viewModel::tutupDialogKoreksi,
            title = {
                Text(
                    "Koreksi Jumlah Terima",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = SukaOnSurface,
                )
            },
            text = {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Barang: ${target.bahanNama}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = SukaBrown,
                    )
                    Text(
                        "Jumlah yang benar (${target.satuan}):",
                        fontSize = 12.sp,
                        color = TEKS_LABEL,
                    )
                    OutlinedTextField(
                        value = state.koreksiQtyInput,
                        onValueChange = viewModel::ubahKoreksiQty,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SukaBrown,
                            unfocusedBorderColor = BORDER_KARTU,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                        ),
                    )
                    Text(
                        "Stok outlet akan disesuaikan secara otomatis.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = viewModel::simpanKoreksi,
                    enabled = !state.koreksiMenyimpan && state.koreksiQtyInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = SukaBrown),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    if (state.koreksiMenyimpan) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Simpan Koreksi", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::tutupDialogKoreksi) {
                    Text("Batal", color = Color.Gray)
                }
            },
        )
    }
}

@Composable
private fun FormTerimaVendor(
    state: TerimaVendorUiState,
    viewModel: TerimaVendorViewModel,
    onMintaFoto: () -> Unit,
) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, BORDER_KARTU),
    ) {
        Column(Modifier.fillMaxWidth()) {
            // Header kartu Bahan & Vendor
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF3FAF3))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .background(SukaGreen.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Eco,
                            contentDescription = null,
                            tint = SukaGreen,
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (state.memuatBahan) {
                            Text("Memuat data bahan...", fontSize = 13.sp, color = Color.Gray)
                        } else if (state.daftarBahan.size > 1) {
                            MenuPilihBahan(
                                daftar = state.daftarBahan,
                                terpilih = state.bahanTerpilih,
                                onPilih = viewModel::pilihBahan,
                            )
                        } else {
                            Text(
                                state.bahanTerpilih?.nama ?: "Tidak ada bahan drop-ship aktif",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = SukaOnSurface,
                            )
                        }

                        // Vendor section
                        if (state.memuatVendor) {
                            Text("Mengecek vendor pengantar...", fontSize = 11.sp, color = Color.Gray)
                        } else if (state.daftarVendor.isEmpty() && state.bahanTerpilih != null) {
                            Text(
                                "Bahan ini belum punya vendor. Minta Pusat mendaftarkannya di Katalog Harga Vendor.",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFDC2626),
                            )
                        } else if (state.daftarVendor.size > 1) {
                            MenuPilihVendor(
                                daftar = state.daftarVendor,
                                terpilih = state.vendorTerpilih,
                                onPilih = viewModel::pilihVendor,
                            )
                        } else if (state.vendorTerpilih != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    Icons.Default.LocalShipping,
                                    contentDescription = null,
                                    tint = TEKS_LABEL,
                                    modifier = Modifier.size(13.dp),
                                )
                                Text(
                                    "Diantar oleh: ${state.vendorTerpilih.supplierNama}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TEKS_LABEL,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }

            // Badan Formulir
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Selektor Tanggal
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "TANGGAL DITERIMA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TEKS_LABEL,
                        letterSpacing = 0.5.sp,
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        state.pilihanTanggal.forEach { p ->
                            val terpilih = state.tanggalTerpilih == p.value
                            Surface(
                                onClick = { viewModel.pilihTanggal(p.value) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                color = if (terpilih) SukaBrown else Color.White,
                                border = BorderStroke(1.dp, if (terpilih) SukaBrown else BORDER_KARTU),
                            ) {
                                Column(
                                    Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(
                                        p.label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (terpilih) Color.White else TEKS_LABEL,
                                    )
                                    Text(
                                        p.value.takeLast(5).replace("-", "/"),
                                        fontSize = 10.sp,
                                        color = if (terpilih) Color.White.copy(alpha = 0.75f) else Color.Gray,
                                    )
                                }
                            }
                        }
                    }
                }

                // Input Jumlah Diterima & Pilihan Satuan
                val bahan = state.bahanTerpilih
                if (bahan != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "JUMLAH DITERIMA",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TEKS_LABEL,
                            letterSpacing = 0.5.sp,
                        )

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedTextField(
                                value = state.qtyInput,
                                onValueChange = viewModel::ubahQty,
                                placeholder = { Text("Misal: 5", color = Color.Gray) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SukaOrange,
                                    unfocusedBorderColor = BORDER_KARTU,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                ),
                            )

                            // Dropdown satuan
                            MenuPilihSatuan(
                                bahan = bahan,
                                terpilih = state.tingkatSatuan,
                                onPilih = viewModel::pilihTingkatSatuan,
                            )
                        }

                        Text(
                            "Isi sesuai timbangan saat barang datang. Harga tidak perlu diisi — dikunci sistem dari katalog vendor.",
                            fontSize = 10.5.sp,
                            color = Color.Gray,
                            lineHeight = 14.sp,
                        )
                    }

                    // Error konversi satuan
                    state.konversiError?.let { err ->
                        Text(
                            err,
                            color = Color(0xFFDC2626),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    // Kartu Dampak Stok & Nilai Rupiah
                    if (state.qtyBesar > 0 && state.konversiError == null) {
                        val butuhKonfirmasi = state.butuhKonfirmasi
                        Surface(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = if (butuhKonfirmasi) Color(0xFFFEF2F2) else SukaCream,
                            border = BorderStroke(
                                1.dp,
                                if (butuhKonfirmasi) Color(0xFFFCA5A5) else Color(0xFFFED7AA),
                            ),
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text("Stok outlet bertambah", fontSize = 12.sp, color = TEKS_LABEL)
                                    Text(
                                        "+${formatAngka(state.qtyBesar)} ${bahan.satuan}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = SukaOnSurface,
                                    )
                                }
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text("Nilai (harga terkunci)", fontSize = 12.sp, color = TEKS_LABEL)
                                    Text(
                                        formatRupiah(state.nilaiRupiah),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = SukaOnSurface,
                                    )
                                }

                                if (butuhKonfirmasi) {
                                    Spacer(Modifier.height(4.dp))
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Checkbox(
                                            checked = state.konfirmasiLonjakan,
                                            onCheckedChange = viewModel::ubahKonfirmasi,
                                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFFDC2626)),
                                            modifier = Modifier.size(20.dp).padding(top = 2.dp),
                                        )
                                        Text(
                                            "Jumlah ini jauh di atas biasanya. Saya sudah cek satuannya (${bahan.satuan}, bukan " +
                                                (bahan.satuanKecil ?: bahan.satuanTengah ?: "satuan lain") +
                                                ") dan jumlahnya benar.",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFFB91C1C),
                                            lineHeight = 15.sp,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Catatan (opsional)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "CATATAN (OPSIONAL)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TEKS_LABEL,
                        letterSpacing = 0.5.sp,
                    )
                    OutlinedTextField(
                        value = state.catatanInput,
                        onValueChange = viewModel::ubahCatatan,
                        placeholder = { Text("Misal: dikirim jam 6 pagi", color = Color.Gray) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SukaOrange,
                            unfocusedBorderColor = BORDER_KARTU,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                        ),
                    )
                }

                // Foto Bukti (opsional)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "FOTO BUKTI TERIMA (OPSIONAL)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TEKS_LABEL,
                        letterSpacing = 0.5.sp,
                    )

                    if (state.fotoBytes != null) {
                        Surface(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = SukaCream,
                            border = BorderStroke(1.dp, Color(0xFFFED7AA)),
                        ) {
                            Row(
                                Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = SukaGreen,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Text(
                                        "Foto siap diunggah",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SukaOnSurface,
                                    )
                                }
                                IconButton(
                                    onClick = viewModel::hapusFoto,
                                    modifier = Modifier.size(24.dp),
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Hapus foto",
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    } else {
                        Surface(
                            onClick = onMintaFoto,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFCFAF8),
                            border = BorderStroke(1.dp, BORDER_KARTU),
                        ) {
                            Row(
                                Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = SukaBrown,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    "Ambil foto timbangan / nota antar",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TEKS_LABEL,
                                )
                            }
                        }
                    }
                }

                // Tombol Submit Catat Terima
                Button(
                    onClick = viewModel::simpan,
                    enabled = state.bolehSimpan,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SukaBrown,
                        disabledContainerColor = Color(0xFFD1D5DB),
                    ),
                ) {
                    if (state.menyimpan || state.mengunggahFoto) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                            )
                            Text(
                                if (state.mengunggahFoto) "Mengunggah foto…" else "Menyimpan…",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                            )
                        }
                    } else {
                        Text(
                            "Catat Terima",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuPilihBahan(
    daftar: List<com.sukashawarma.superapp.feature.stok.data.BahanDropShip>,
    terpilih: com.sukashawarma.superapp.feature.stok.data.BahanDropShip?,
    onPilih: (com.sukashawarma.superapp.feature.stok.data.BahanDropShip) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(10.dp),
            color = Color.White,
            border = BorderStroke(1.dp, BORDER_KARTU),
        ) {
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    terpilih?.nama ?: "Pilih Bahan...",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = SukaOnSurface,
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            daftar.forEach { b ->
                DropdownMenuItem(
                    text = { Text(b.nama, fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                    onClick = {
                        onPilih(b)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun MenuPilihVendor(
    daftar: List<com.sukashawarma.superapp.feature.stok.data.InfoVendorDropShip>,
    terpilih: com.sukashawarma.superapp.feature.stok.data.InfoVendorDropShip?,
    onPilih: (com.sukashawarma.superapp.feature.stok.data.InfoVendorDropShip) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(10.dp),
            color = Color.White,
            border = BorderStroke(1.dp, BORDER_KARTU),
        ) {
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    terpilih?.supplierNama ?: "Pilih Vendor Pengantar...",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = SukaOnSurface,
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            daftar.forEach { v ->
                DropdownMenuItem(
                    text = { Text(v.supplierNama, fontSize = 12.sp) },
                    onClick = {
                        onPilih(v)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun MenuPilihSatuan(
    bahan: com.sukashawarma.superapp.feature.stok.data.BahanDropShip,
    terpilih: SatuanTingkat,
    onPilih: (SatuanTingkat) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    val labelSatuan = when (terpilih) {
        SatuanTingkat.BESAR -> bahan.satuan
        SatuanTingkat.TENGAH -> bahan.satuanTengah ?: bahan.satuan
        SatuanTingkat.KECIL -> bahan.satuanKecil ?: bahan.satuan
    }

    Box {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            border = BorderStroke(1.dp, BORDER_KARTU),
            modifier = Modifier.height(54.dp),
        ) {
            Box(
                Modifier.padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    labelSatuan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = SukaOnSurface,
                )
            }
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(bahan.satuan, fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                onClick = {
                    onPilih(SatuanTingkat.BESAR)
                    expanded = false
                },
            )
            if (!bahan.satuanTengah.isNullOrBlank() && bahan.faktorTengah != null && bahan.faktorTengah > 0) {
                DropdownMenuItem(
                    text = { Text(bahan.satuanTengah, fontSize = 13.sp) },
                    onClick = {
                        onPilih(SatuanTingkat.TENGAH)
                        expanded = false
                    },
                )
            }
            if (!bahan.satuanKecil.isNullOrBlank() && bahan.faktorTampilan != null && bahan.faktorTampilan > 0) {
                DropdownMenuItem(
                    text = { Text(bahan.satuanKecil, fontSize = 13.sp) },
                    onClick = {
                        onPilih(SatuanTingkat.KECIL)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun KartuRiwayatTerima(
    catatan: CatatanTerimaVendor,
    onKoreksi: () -> Unit,
) {
    val (statusLabel, warnaLatar, warnaTeks) = when (catatan.status.lowercase()) {
        "disahkan" -> Triple("Disahkan", Color(0xFFECFDF5), Color(0xFF047857))
        "ditolak" -> Triple("Ditolak", Color(0xFFFEF2F2), Color(0xFFB91C1C))
        else -> Triple("Menunggu Nota", Color(0xFFFFFBEB), Color(0xFF92400E))
    }

    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, BORDER_KARTU),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "${formatAngka(catatan.qty)} ${catatan.satuan} · ${catatan.bahanNama}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = SukaOnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val tgl = catatan.tanggalTerima.takeLast(5).replace("-", "/")
                Text(
                    "$tgl · ${catatan.supplierNama}",
                    fontSize = 11.sp,
                    color = TEKS_LABEL.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = warnaLatar,
                ) {
                    Text(
                        statusLabel.uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = warnaTeks,
                    )
                }

                if (catatan.status.lowercase() == "dicatat") {
                    Text(
                        "Koreksi",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SukaOrange,
                        modifier = Modifier
                            .clickable(onClick = onKoreksi)
                            .padding(4.dp),
                    )
                }
            }
        }
    }
}
