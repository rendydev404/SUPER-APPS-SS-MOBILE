package com.sukashawarma.superapp.feature.distribusi.ui.buat

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.core.ui.ios.BarisIos
import com.sukashawarma.superapp.core.ui.ios.BilahJudulIos
import com.sukashawarma.superapp.core.ui.ios.GrupIos
import com.sukashawarma.superapp.core.ui.ios.JudulSeksiIos
import com.sukashawarma.superapp.core.ui.ios.KapsulPilihanIos
import com.sukashawarma.superapp.core.ui.ios.KartuIos
import com.sukashawarma.superapp.core.ui.ios.KeadaanIos
import com.sukashawarma.superapp.core.ui.ios.KolomCariIos
import com.sukashawarma.superapp.core.ui.ios.LencanaIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.PanelGalatIos
import com.sukashawarma.superapp.core.ui.ios.PemisahIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.TombolBundarIos
import com.sukashawarma.superapp.core.ui.ios.TombolKapsulIos
import com.sukashawarma.superapp.core.ui.ios.TombolKeduaIos
import com.sukashawarma.superapp.core.ui.ios.TombolUtamaIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.ios.warnaKolomIos
import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.core.ui.kaca.denganRuangNav
import com.sukashawarma.superapp.core.ui.kaca.navigationBarsPaddingKaca
import com.sukashawarma.superapp.feature.distribusi.domain.AlokasiVendor
import com.sukashawarma.superapp.feature.distribusi.domain.BahanBakuMeta
import com.sukashawarma.superapp.feature.distribusi.domain.PengirimanPusat
import com.sukashawarma.superapp.feature.distribusi.domain.SaldoVendor
import com.sukashawarma.superapp.feature.distribusi.domain.SatuanDistribusi
import com.sukashawarma.superapp.feature.distribusi.ui.LayarGalat
import com.sukashawarma.superapp.feature.distribusi.ui.LayarMemuat
import com.sukashawarma.superapp.feature.distribusi.ui.ShellDistribusi
import com.sukashawarma.superapp.feature.distribusi.ui.TabBawah

private val PRESET_QTY = listOf(1, 5, 10, 20, 50)

/**
 * Form buat surat jalan untuk kitchen — cermin `SuratJalanForm.tsx`. Setelah
 * tersimpan, layar pindah ke detail untuk tanda tangan dan pengiriman.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuatSuratJalanScreen(
    onKeluar: () -> Unit,
    onDibuat: (String) -> Unit,
    onBukaDashboard: () -> Unit,
    onBukaRiwayat: () -> Unit,
    viewModel: BuatSuratJalanViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var pilihOutlet by remember { mutableStateOf(false) }
    var pilihBahan by remember { mutableStateOf(false) }

    LaunchedEffect(state.pesan, state.error) {
        val teks = state.error ?: state.pesan
        if (teks != null) {
            snackbar.showSnackbar(teks)
            viewModel.bersihkanPesan()
        }
    }
    LaunchedEffect(state.dibuatId) {
        state.dibuatId?.let {
            viewModel.sudahPindah()
            onDibuat(it)
        }
    }

    if (!state.bolehTerbitkan) {
        Box(Modifier.fillMaxSize().background(WarnaIos.Latar), contentAlignment = Alignment.Center) {
            KeadaanIos(
                IkonIos.Lock,
                "Akses Ditolak",
                "Hanya Gudang Pusat (Kitchen) yang dapat membuat Surat Jalan baru.",
                teksAksi = "Kembali",
                onAksi = onKeluar,
            )
        }
        return
    }

    ShellDistribusi(
        aktif = TabBawah.BUAT,
        bolehVerifikasi = false,
        bolehTerbitkan = true,
        onDashboard = onBukaDashboard,
        onScan = {},
        onBuat = {},
        onRiwayat = onBukaRiwayat,
    ) {
        Scaffold(
            containerColor = WarnaIos.Latar,
            snackbarHost = { SnackbarHost(snackbar, Modifier.navigationBarsPaddingKaca()) },
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).background(WarnaIos.Latar)) {
                BilahJudulIos(
                    judul = "Buat Surat Jalan",
                    subjudul = "Kirim bahan dari Gudang Pusat ke outlet",
                    onKembali = onKeluar,
                )
                when {
                    state.memuatReferensi -> LayarMemuat()
                    state.galatReferensi != null -> LayarGalat(state.galatReferensi!!) { viewModel.muatReferensi() }
                    else -> IsiForm(
                        state = state,
                        viewModel = viewModel,
                        onPilihOutlet = { pilihOutlet = true },
                        onPilihBahan = { pilihBahan = true },
                    )
                }
            }
        }
    }

    if (pilihOutlet) {
        ModalBottomSheet(
            onDismissRequest = { pilihOutlet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = WarnaIos.Latar,
        ) {
            LembarOutlet(state) {
                viewModel.pilihOutlet(it)
                pilihOutlet = false
            }
        }
    }
    if (pilihBahan) {
        ModalBottomSheet(
            onDismissRequest = { pilihBahan = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = WarnaIos.Latar,
        ) {
            LembarBahan(state, viewModel) {
                viewModel.pilihBahan(it)
                pilihBahan = false
            }
        }
    }
}

@Composable
private fun IsiForm(
    state: BuatUiState,
    viewModel: BuatSuratJalanViewModel,
    onPilihOutlet: () -> Unit,
    onPilihBahan: () -> Unit,
) {
    val outlet = state.outlet.find { it.id == state.outletId }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = UkuranIos.TepiLayar,
            end = UkuranIos.TepiLayar,
            top = 12.dp,
            bottom = 24.dp,
        ).denganRuangNav(),
        verticalArrangement = Arrangement.spacedBy(UkuranIos.JarakKartu),
    ) {
        item(key = "outlet") {
            GrupIos(judul = "Outlet tujuan") {
                BarisIos(
                    judul = outlet?.nama ?: "Pilih outlet tujuan",
                    keterangan = outlet?.alamat,
                    ikon = IkonIos.Storefront,
                    nadaIkon = if (outlet == null) NadaIos.NETRAL else NadaIos.AKSEN,
                    onKlik = onPilihOutlet,
                )
            }
        }

        item(key = "tambah") { KartuTambahBarang(state, viewModel, onPilihBahan) }

        item(key = "judul-muatan") {
            JudulSeksiIos("Daftar muatan", keterangan = "${state.muatan.size} item")
        }

        if (state.galatVendor != null) {
            item(key = "galat-vendor") {
                PanelGalatIos(
                    "Daftar vendor Gudang Pusat gagal dimuat: ${state.galatVendor}",
                    onCoba = viewModel::muatUlangVendor,
                )
            }
        }

        if (state.muatan.isEmpty()) {
            item(key = "kosong") {
                KeadaanIos(IkonIos.Inventory2, "Belum ada barang", "Pilih bahan lalu tambahkan ke daftar kirim.")
            }
        } else {
            items(state.muatan, key = { it.bahan.id }) { baris ->
                KartuMuatan(baris, state, viewModel)
            }
        }

        item(key = "simpan") {
            TombolUtamaIos(
                if (state.menyimpan) "Menyimpan..." else "Buat Surat Jalan",
                viewModel::simpan,
                aktif = state.bisaSimpan,
                memuat = state.menyimpan,
                ikon = IkonIos.CheckCircle,
            )
        }
        if (!state.vendorSiap && state.galatVendor == null && state.muatan.isNotEmpty()) {
            item(key = "memuat-vendor") {
                Text(
                    "Memuat saldo vendor Gudang Pusat…",
                    Modifier.padding(horizontal = 4.dp),
                    style = TipeIos.Catatan,
                )
            }
        }
    }
}

@Composable
private fun KartuTambahBarang(state: BuatUiState, viewModel: BuatSuratJalanViewModel, onPilihBahan: () -> Unit) {
    val bahan = state.bahanTerpilih
    KartuIos {
        Text("Tambah barang", style = TipeIos.Utama)
        Spacer(Modifier.height(10.dp))
        if (bahan == null) {
            TombolKeduaIos("Pilih bahan baku", onPilihBahan, ikon = IkonIos.Search)
            return@KartuIos
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(bahan.nama, style = TipeIos.Isi)
                Text("Satuan kirim: ${SatuanDistribusi.satuanTampil(bahan)}", style = TipeIos.Catatan)
            }
            TombolBundarIos(IkonIos.Close, "Ganti bahan", { viewModel.pilihBahan(null) })
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = state.qtyTeks,
            onValueChange = viewModel::ubahQty,
            label = { Text("Jumlah (${SatuanDistribusi.satuanTampil(bahan)})") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = UkuranIos.SudutKontrol,
            colors = warnaKolomIos(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PRESET_QTY.forEach { n -> TombolKapsulIos("+$n", { viewModel.tambahPreset(n) }) }
        }
        Spacer(Modifier.height(12.dp))
        TombolUtamaIos("Tambah ke daftar", viewModel::tambahKeMuatan, ikon = IkonIos.Add)
    }
}

@Composable
private fun KartuMuatan(baris: BarisMuatan, state: BuatUiState, viewModel: BuatSuratJalanViewModel) {
    val satuan = SatuanDistribusi.satuanTampil(baris.bahan)
    val vendors = state.vendorUntuk(baris.bahan.id)
    val galat = state.galatAlokasi(baris)
    KartuIos {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(baris.bahan.nama, style = TipeIos.Utama)
                Text("${AlokasiVendor.angka(baris.qty)} $satuan", style = TipeIos.Catatan)
            }
            TombolBundarIos(IkonIos.Delete, "Hapus", { viewModel.hapusMuatan(baris.bahan.id) }, warnaIkon = WarnaIos.Merah)
        }
        if (vendors.size >= 2) {
            Spacer(Modifier.height(12.dp))
            PilihVendor(baris, vendors, satuan, viewModel)
        }
        if (galat != null) {
            Spacer(Modifier.height(8.dp))
            Text(galat, style = TipeIos.Catatan.copy(color = NadaIos.BAHAYA.teks))
        }
    }
}

/**
 * Cermin `PilihVendorBahan.tsx`: satu vendor (pilihan tunggal) atau dipecah ke
 * beberapa vendor dengan jumlah masing-masing. Vendor yang saldonya dihitung
 * dan sudah habis tidak bisa dipilih.
 */
@Composable
private fun PilihVendor(
    baris: BarisMuatan,
    vendors: List<SaldoVendor>,
    satuan: String,
    viewModel: BuatSuratJalanViewModel,
) {
    var pecah by rememberSaveable(baris.bahan.id) { mutableStateOf(baris.alokasi.size > 1) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Vendor Gudang Pusat", Modifier.weight(1f), style = TipeIos.Catatan)
        TombolKapsulIos(
            if (pecah) "Satu vendor saja" else "+ Pecah vendor",
            {
                if (pecah) {
                    baris.alokasi.firstOrNull()?.let { viewModel.pilihVendorTunggal(baris.bahan.id, it.vendorId) }
                }
                pecah = !pecah
            },
        )
    }
    Spacer(Modifier.height(6.dp))
    Column(Modifier.fillMaxWidth().background(WarnaIos.Latar, UkuranIos.SudutBlok)) {
        vendors.forEachIndexed { i, v ->
            if (i > 0) PemisahIos()
            val habis = v.aktif && v.sisa <= 0.0
            val dipilih = baris.alokasi.find { it.vendorId == v.vendorId }
            val keterangan = when {
                !v.aktif -> "Saldo belum dihitung"
                habis -> "Habis"
                else -> "Sisa ${AlokasiVendor.angka(v.sisa)} $satuan"
            }
            BarisIos(
                judul = v.vendorNama,
                keterangan = keterangan,
                onKlik = if (habis && dipilih == null) null else {
                    {
                        if (pecah) viewModel.alihVendorPecah(baris.bahan.id, v.vendorId)
                        else viewModel.pilihVendorTunggal(baris.bahan.id, v.vendorId)
                    }
                },
                chevron = false,
                trailing = {
                    if (dipilih != null) {
                        Icon(IkonIos.CheckCircle, "Dipilih", tint = WarnaIos.Aksen, modifier = Modifier.size(20.dp))
                    } else if (habis) {
                        LencanaIos("Habis", NadaIos.NETRAL, titik = false)
                    }
                },
            )
            if (pecah && dipilih != null) {
                var teks by remember(baris.bahan.id, v.vendorId) { mutableStateOf(AlokasiVendor.angka(dipilih.qty)) }
                // Qty bisa diubah dari luar (bahan yang sama ditambah lagi). Isian
                // disamakan hanya bila angkanya memang beda, supaya ketikan setengah
                // jadi seperti "1." atau isian kosong tidak ditimpa.
                LaunchedEffect(dipilih.qty) {
                    if ((teks.toDoubleOrNull() ?: 0.0) != dipilih.qty) teks = AlokasiVendor.angka(dipilih.qty)
                }
                OutlinedTextField(
                    value = teks,
                    onValueChange = {
                        teks = it.replace(',', '.').filter { c -> c.isDigit() || c == '.' }
                        viewModel.ubahQtyVendor(baris.bahan.id, v.vendorId, teks)
                    },
                    label = { Text("Jumlah dari ${v.vendorNama} ($satuan)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = UkuranIos.SudutKontrol,
                    colors = warnaKolomIos(),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun LembarOutlet(state: BuatUiState, onPilih: (String) -> Unit) {
    var cari by remember { mutableStateOf("") }
    val kunci = cari.trim().lowercase()
    val daftar = state.outlet.filter { kunci.isEmpty() || it.nama.lowercase().contains(kunci) }
    Column(Modifier.fillMaxHeight(0.85f).padding(horizontal = UkuranIos.TepiLayar)) {
        Text("Pilih outlet tujuan", style = TipeIos.Utama)
        Spacer(Modifier.height(10.dp))
        KolomCariIos(cari, { cari = it }, Modifier.fillMaxWidth(), placeholder = "Cari outlet")
        Spacer(Modifier.height(10.dp))
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                GrupIos {
                    daftar.forEachIndexed { i, o ->
                        if (i > 0) PemisahIos()
                        BarisIos(
                            judul = o.nama,
                            keterangan = o.alamat,
                            onKlik = { onPilih(o.id) },
                            chevron = false,
                            trailing = if (o.id == state.outletId) {
                                { Icon(IkonIos.Check, "Terpilih", tint = WarnaIos.Aksen, modifier = Modifier.size(18.dp)) }
                            } else {
                                null
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LembarBahan(state: BuatUiState, viewModel: BuatSuratJalanViewModel, onPilih: (BahanBakuMeta) -> Unit) {
    val daftar = state.bahanTersaring
    Column(Modifier.fillMaxHeight(0.9f).padding(horizontal = UkuranIos.TepiLayar)) {
        Text("Pilih bahan baku", style = TipeIos.Utama)
        Spacer(Modifier.height(10.dp))
        KolomCariIos(state.cari, viewModel::ubahCari, Modifier.fillMaxWidth(), placeholder = "Cari bahan")
        Spacer(Modifier.height(10.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PengirimanPusat.Kategori.entries.forEach { k ->
                KapsulPilihanIos(k.label, state.kategori == k, { viewModel.ubahKategori(k) })
            }
        }
        Spacer(Modifier.height(10.dp))
        if (daftar.isEmpty()) {
            KeadaanIos(IkonIos.Search, "Tidak ditemukan", "Tidak ada bahan yang cocok.")
            return
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                GrupIos {
                    daftar.forEachIndexed { i, b ->
                        if (i > 0) PemisahIos()
                        val dimuat = state.muatan.find { it.bahan.id == b.id }
                        BarisIos(
                            judul = b.nama,
                            keterangan = "Satuan kirim: ${SatuanDistribusi.satuanTampil(b)}",
                            onKlik = { onPilih(b) },
                            chevron = false,
                            nilai = dimuat?.let { "${AlokasiVendor.angka(it.qty)} di daftar" },
                        )
                    }
                }
            }
        }
    }
}
