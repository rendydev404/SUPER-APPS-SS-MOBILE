package com.sukashawarma.superapp.feature.stok.ui.vendor

import com.sukashawarma.superapp.core.ui.ios.JudulSeksiIos
import com.sukashawarma.superapp.core.ui.ios.KartuIos
import com.sukashawarma.superapp.core.ui.ios.LabelSeksiIos
import com.sukashawarma.superapp.core.ui.ios.LencanaIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.TombolBundarIos
import com.sukashawarma.superapp.core.ui.ios.TombolKeduaIos
import com.sukashawarma.superapp.core.ui.ios.TombolUtamaIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.ios.tekanIos
import com.sukashawarma.superapp.core.ui.ios.warnaKolomIos
import com.sukashawarma.superapp.feature.stok.ui.BannerIos
import com.sukashawarma.superapp.feature.stok.ui.BarisRincianIos
import com.sukashawarma.superapp.core.ui.SukaDropdownHeader
import com.sukashawarma.superapp.core.ui.SukaDropdownMenu
import com.sukashawarma.superapp.core.ui.SukaDropdownMenuItem
import com.sukashawarma.superapp.feature.stok.ui.KeadaanTidakBerhak
import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.core.ui.kaca.denganRuangNav
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

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

    Column(Modifier.fillMaxSize().background(WarnaIos.Latar)) {
        HeaderStok(
            judul = "Terima dari Vendor",
            subjudul = "Catat sayur/bahan yang diantar langsung vendor ke outlet",
            onKembali = onBack,
            aksi = {
                TombolBundarIos(IkonIos.Refresh, "Muat Ulang Riwayat", viewModel::muatRiwayat)
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
            KeadaanTidakBerhak("Akun tidak terhubung ke outlet mana pun.")
            return@Column
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = UkuranIos.TepiLayar, end = UkuranIos.TepiLayar, top = 12.dp, bottom = 32.dp).denganRuangNav(),
            verticalArrangement = Arrangement.spacedBy(UkuranIos.JarakKartu),
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
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    JudulSeksiIos("7 hari terakhir", Modifier.weight(1f))
                    if (state.memuatRiwayat) {
                        CircularProgressIndicator(
                            color = WarnaIos.Abu,
                            modifier = Modifier.padding(bottom = 6.dp, end = 4.dp).size(16.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                }
            }

            if (state.riwayat.isEmpty() && !state.memuatRiwayat) {
                item {
                    KartuIos(padding = PaddingValues(24.dp)) {
                        Text(
                            "Belum ada catatan 7 hari terakhir.",
                            Modifier.fillMaxWidth(),
                            style = TipeIos.SubJudul,
                            textAlign = TextAlign.Center,
                        )
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
            containerColor = WarnaIos.Kartu,
        ) {
            Column(Modifier.padding(horizontal = UkuranIos.TepiLayar).padding(bottom = 24.dp)) {
                Text("Foto Bukti Terima / Timbangan", style = TipeIos.Utama)
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
            shape = UkuranIos.SudutKartu,
            containerColor = WarnaIos.Kartu,
            title = { Text("Koreksi Jumlah Terima", style = TipeIos.Utama) },
            text = {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Barang: ${target.bahanNama}",
                        style = TipeIos.Keterangan.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                    )
                    Text("Jumlah yang benar (${target.satuan}):", style = TipeIos.Catatan)
                    OutlinedTextField(
                        value = state.koreksiQtyInput,
                        onValueChange = viewModel::ubahKoreksiQty,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TipeIos.Isi,
                        shape = UkuranIos.SudutKontrol,
                        colors = warnaKolomIos(),
                    )
                    Text("Stok outlet akan disesuaikan secara otomatis.", style = TipeIos.Kecil)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::simpanKoreksi,
                    enabled = !state.koreksiMenyimpan && state.koreksiQtyInput.isNotBlank(),
                ) {
                    if (state.koreksiMenyimpan) {
                        CircularProgressIndicator(
                            color = WarnaIos.Aksen,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            "Simpan Koreksi",
                            color = if (state.koreksiQtyInput.isNotBlank()) WarnaIos.Aksen else WarnaIos.LabelKetiga,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::tutupDialogKoreksi) {
                    Text("Batal", color = WarnaIos.LabelKedua, fontSize = 16.sp)
                }
            },
        )
    }
}

/** Label kecil di atas satu bagian formulir, bergaya label seksi iOS. */
@Composable
private fun LabelBagian(teks: String) {
    LabelSeksiIos(teks, Modifier.padding(start = 4.dp))
}

@Composable
private fun FormTerimaVendor(
    state: TerimaVendorUiState,
    viewModel: TerimaVendorViewModel,
    onMintaFoto: () -> Unit,
) {
    KartuIos(padding = PaddingValues(0.dp)) {
        // Kepala kartu: bahan & vendor
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(WarnaIos.Hijau.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    IkonIos.Eco,
                    contentDescription = null,
                    tint = WarnaIos.Hijau,
                    modifier = Modifier.size(21.dp),
                )
            }

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (state.memuatBahan) {
                    Text("Memuat data bahan...", style = TipeIos.SubJudul)
                } else if (state.daftarBahan.size > 1) {
                    MenuPilihBahan(
                        daftar = state.daftarBahan,
                        terpilih = state.bahanTerpilih,
                        onPilih = viewModel::pilihBahan,
                    )
                } else {
                    Text(
                        state.bahanTerpilih?.nama ?: "Tidak ada bahan drop-ship aktif",
                        style = TipeIos.Utama,
                    )
                }

                // Bagian vendor
                if (state.memuatVendor) {
                    Text("Mengecek vendor pengantar...", style = TipeIos.Catatan)
                } else if (state.daftarVendor.isEmpty() && state.bahanTerpilih != null) {
                    Text(
                        "Bahan ini belum punya vendor. Minta Pusat mendaftarkannya di Katalog Harga Vendor.",
                        style = TipeIos.Catatan.copy(color = NadaIos.BAHAYA.teks, fontWeight = FontWeight.Medium),
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
                            IkonIos.LocalShipping,
                            contentDescription = null,
                            tint = WarnaIos.Abu,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            "Diantar oleh: ${state.vendorTerpilih.supplierNama}",
                            style = TipeIos.Catatan,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        Box(Modifier.fillMaxWidth().padding(start = 16.dp).height(0.5.dp).background(WarnaIos.Pemisah))

        // Badan formulir
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Pemilih tanggal
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                LabelBagian("Tanggal diterima")
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.pilihanTanggal.forEach { p ->
                        val terpilih = state.tanggalTerpilih == p.value
                        Column(
                            Modifier
                                .weight(1f)
                                .clip(UkuranIos.SudutKontrol)
                                .background(if (terpilih) WarnaIos.Aksen else WarnaIos.Isian)
                                .tekanIos({ viewModel.pilihTanggal(p.value) })
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                p.label,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (terpilih) Color.White else WarnaIos.Label,
                            )
                            Text(
                                p.value.takeLast(5).replace("-", "/"),
                                fontSize = 11.sp,
                                color = if (terpilih) Color.White.copy(alpha = 0.8f) else WarnaIos.LabelKedua,
                            )
                        }
                    }
                }
            }

            // Jumlah diterima & pilihan satuan
            val bahan = state.bahanTerpilih
            if (bahan != null) {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    LabelBagian("Jumlah diterima")

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = state.qtyInput,
                            onValueChange = viewModel::ubahQty,
                            placeholder = { Text("Misal: 5", fontSize = 17.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            textStyle = TipeIos.Isi,
                            shape = UkuranIos.SudutKontrol,
                            colors = warnaKolomIos(),
                        )

                        // Pemilih satuan
                        MenuPilihSatuan(
                            bahan = bahan,
                            terpilih = state.tingkatSatuan,
                            onPilih = viewModel::pilihTingkatSatuan,
                        )
                    }

                    Text(
                        "Isi sesuai timbangan saat barang datang. Harga tidak perlu diisi — dikunci sistem dari katalog vendor.",
                        style = TipeIos.Kecil.copy(lineHeight = 16.sp),
                    )
                }

                // Error konversi satuan
                state.konversiError?.let { err ->
                    BannerIos(err, NadaIos.BAHAYA, ikon = IkonIos.ErrorOutline)
                }

                // Dampak stok & nilai rupiah
                if (state.qtyBesar > 0 && state.konversiError == null) {
                    val butuhKonfirmasi = state.butuhKonfirmasi
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(UkuranIos.SudutBlok)
                            .background(if (butuhKonfirmasi) WarnaIos.Merah.copy(alpha = 0.10f) else WarnaIos.Latar)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        BarisRincianIos("Stok outlet bertambah", "+${formatAngka(state.qtyBesar)} ${bahan.satuan}", tebal = true)
                        BarisRincianIos("Nilai (harga terkunci)", formatRupiah(state.nilaiRupiah), tebal = true)

                        if (butuhKonfirmasi) {
                            Spacer(Modifier.height(4.dp))
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Checkbox(
                                    checked = state.konfirmasiLonjakan,
                                    onCheckedChange = viewModel::ubahKonfirmasi,
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = WarnaIos.Merah,
                                        uncheckedColor = NadaIos.BAHAYA.teks,
                                    ),
                                    modifier = Modifier.size(20.dp).padding(top = 2.dp),
                                )
                                Text(
                                    "Jumlah ini jauh di atas biasanya. Saya sudah cek satuannya (${bahan.satuan}, bukan " +
                                        (bahan.satuanKecil ?: bahan.satuanTengah ?: "satuan lain") +
                                        ") dan jumlahnya benar.",
                                    style = TipeIos.Catatan.copy(
                                        color = NadaIos.BAHAYA.teks,
                                        fontWeight = FontWeight.Medium,
                                        lineHeight = 18.sp,
                                    ),
                                )
                            }
                        }
                    }
                }
            }

            // Catatan (opsional)
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                LabelBagian("Catatan (opsional)")
                OutlinedTextField(
                    value = state.catatanInput,
                    onValueChange = viewModel::ubahCatatan,
                    placeholder = { Text("Misal: dikirim jam 6 pagi", fontSize = 15.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TipeIos.Keterangan,
                    shape = UkuranIos.SudutKontrol,
                    colors = warnaKolomIos(),
                )
            }

            // Foto bukti (opsional)
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                LabelBagian("Foto bukti terima (opsional)")

                if (state.fotoBytes != null) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(UkuranIos.SudutKontrol)
                            .background(WarnaIos.Hijau.copy(alpha = 0.10f))
                            .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.size(22.dp).clip(CircleShape).background(WarnaIos.Hijau),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(IkonIos.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Foto siap diunggah",
                            Modifier.weight(1f),
                            style = TipeIos.Keterangan.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = NadaIos.SUKSES.teks),
                        )
                        TombolBundarIos(IkonIos.Delete, "Hapus foto", viewModel::hapusFoto, warnaIkon = WarnaIos.Merah)
                    }
                } else {
                    TombolKeduaIos(
                        "Ambil foto timbangan / nota antar",
                        onMintaFoto,
                        Modifier.height(44.dp),
                        ikon = IkonIos.CameraAlt,
                    )
                }
            }

            // Tombol simpan
            TombolUtamaIos(
                when {
                    state.mengunggahFoto -> "Mengunggah foto…"
                    state.menyimpan -> "Menyimpan…"
                    else -> "Catat Terima"
                },
                viewModel::simpan,
                aktif = state.bolehSimpan,
            )
        }
    }
}

/** Pemicu menu pilihan di kepala formulir — kapsul abu ala "pull-down button" iOS. */
@Composable
private fun PemicuMenu(teks: String, onKlik: () -> Unit, besar: Boolean) {
    Row(
        Modifier
            .clip(UkuranIos.SudutKontrol)
            .background(WarnaIos.Isian)
            .tekanIos(onKlik)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            teks,
            Modifier.weight(1f, fill = false),
            style = TipeIos.Keterangan.copy(
                fontSize = if (besar) 16.sp else 14.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(4.dp))
        Icon(IkonIos.ArrowDropDown, null, tint = WarnaIos.Aksen, modifier = Modifier.size(14.dp))
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
        PemicuMenu(terpilih?.nama ?: "Pilih Bahan...", { expanded = true }, besar = true)
        SukaDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SukaDropdownHeader(title = "PILIH BAHAN", onClose = { expanded = false })
            daftar.forEach { b ->
                SukaDropdownMenuItem(
                    text = b.nama,
                    selected = terpilih?.id == b.id,
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
        PemicuMenu(terpilih?.supplierNama ?: "Pilih Vendor Pengantar...", { expanded = true }, besar = false)
        SukaDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SukaDropdownHeader(title = "PILIH VENDOR", onClose = { expanded = false })
            daftar.forEach { v ->
                SukaDropdownMenuItem(
                    text = v.supplierNama,
                    selected = terpilih == v,
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
        Row(
            Modifier
                .height(56.dp)
                .clip(UkuranIos.SudutKontrol)
                .background(WarnaIos.Isian)
                .tekanIos({ expanded = true })
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(labelSatuan, style = TipeIos.Keterangan.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold))
            Spacer(Modifier.width(4.dp))
            Icon(IkonIos.ArrowDropDown, null, tint = WarnaIos.Aksen, modifier = Modifier.size(14.dp))
        }

        SukaDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SukaDropdownHeader(title = "PILIH SATUAN", onClose = { expanded = false })
            SukaDropdownMenuItem(
                text = bahan.satuan,
                selected = terpilih == SatuanTingkat.BESAR,
                onClick = {
                    onPilih(SatuanTingkat.BESAR)
                    expanded = false
                },
            )
            if (!bahan.satuanTengah.isNullOrBlank() && bahan.faktorTengah != null && bahan.faktorTengah > 0) {
                SukaDropdownMenuItem(
                    text = bahan.satuanTengah,
                    selected = terpilih == SatuanTingkat.TENGAH,
                    onClick = {
                        onPilih(SatuanTingkat.TENGAH)
                        expanded = false
                    },
                )
            }
            if (!bahan.satuanKecil.isNullOrBlank() && bahan.faktorTampilan != null && bahan.faktorTampilan > 0) {
                SukaDropdownMenuItem(
                    text = bahan.satuanKecil,
                    selected = terpilih == SatuanTingkat.KECIL,
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
    val (statusLabel, nada) = when (catatan.status.lowercase()) {
        "disahkan" -> "Disahkan" to NadaIos.SUKSES
        "ditolak" -> "Ditolak" to NadaIos.BAHAYA
        else -> "Menunggu Nota" to NadaIos.PERINGATAN
    }

    KartuIos(padding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "${formatAngka(catatan.qty)} ${catatan.satuan} · ${catatan.bahanNama}",
                    style = TipeIos.Keterangan.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val tgl = catatan.tanggalTerima.takeLast(5).replace("-", "/")
                Text(
                    "$tgl · ${catatan.supplierNama}",
                    style = TipeIos.Catatan,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LencanaIos(statusLabel, nada)
                if (catatan.status.lowercase() == "dicatat") {
                    Text(
                        "Koreksi",
                        Modifier
                            .clip(UkuranIos.SudutKapsul)
                            .tekanIos(onKoreksi)
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        color = WarnaIos.Aksen,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
