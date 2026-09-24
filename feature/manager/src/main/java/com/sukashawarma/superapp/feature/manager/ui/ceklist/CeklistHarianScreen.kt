package com.sukashawarma.superapp.feature.manager.ui.ceklist

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.core.camera.KameraFotoSheet
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.RealtimeTables
import com.sukashawarma.superapp.core.ui.ios.BilahJudulIos
import com.sukashawarma.superapp.core.ui.ios.IkonBulatIos
import com.sukashawarma.superapp.core.ui.ios.KartuIos
import com.sukashawarma.superapp.core.ui.ios.KeadaanIos
import com.sukashawarma.superapp.core.ui.ios.LabelSeksiIos
import com.sukashawarma.superapp.core.ui.ios.LencanaIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.PanelGalatIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.TombolBundarIos
import com.sukashawarma.superapp.core.ui.ios.TombolKeduaIos
import com.sukashawarma.superapp.core.ui.ios.TombolUtamaIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.ios.tekanIos
import com.sukashawarma.superapp.core.ui.ios.warnaKolomIos
import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.core.ui.kaca.denganRuangNav
import com.sukashawarma.superapp.core.ui.kaca.navigationBarsPaddingKaca
import com.sukashawarma.superapp.feature.manager.domain.FOTO_MAKS_PER_KATEGORI
import com.sukashawarma.superapp.feature.manager.domain.KATEGORI_CEKLIST
import com.sukashawarma.superapp.feature.manager.domain.BagianBebas
import com.sukashawarma.superapp.feature.manager.domain.KategoriCeklist
import com.sukashawarma.superapp.feature.manager.domain.LaporanCeklist
import com.sukashawarma.superapp.feature.manager.domain.kategoriLengkap
import com.sukashawarma.superapp.feature.manager.domain.tanggalPanjangIndonesia
import com.sukashawarma.superapp.feature.manager.domain.jamJakarta
import com.sukashawarma.superapp.feature.manager.ui.BarProgres
import com.sukashawarma.superapp.feature.manager.ui.KartuPanel
import com.sukashawarma.superapp.feature.manager.ui.PanelKosong
import kotlinx.coroutines.launch

/**
 * Ceklist Harian — jalur pengisian area manager.
 *
 * Dirancang untuk dipakai sambil berdiri di outlet dengan satu tangan: satu
 * ketukan per penilaian (kalimat keterangan terisi sendiri), tombol "Semua
 * sesuai" untuk hari yang lancar, foto langsung dari kamera per kategori, dan
 * tombol kirim yang selalu menyebut apa yang masih kurang.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CeklistHarianScreen(
    onExit: () -> Unit,
    viewModel: CeklistHarianViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val konteks = LocalContext.current
    val lembarKamera = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var fotoDibuka by remember { mutableStateOf<Pair<String?, String>?>(null) }

    // Layar AM ikut bergerak begitu RM meninjau — tanggapannya muncul tanpa muat ulang.
    RealtimeRefresh(RealtimeTables.CEKLIST_HARIAN) { viewModel.muatUlang(silent = true) }

    var kategoriMenungguIzin by rememberSaveable { mutableStateOf<String?>(null) }
    val pemintaIzin = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        val k = kategoriMenungguIzin
        kategoriMenungguIzin = null
        if (ok && k != null) viewModel.bukaKamera(k)
    }

    // Galeri hanya untuk kategori yang mengizinkannya (screenshot ulasan online).
    var kategoriGaleri by rememberSaveable { mutableStateOf<String?>(null) }
    val pemilihGaleri = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val k = kategoriGaleri
        kategoriGaleri = null
        if (uri != null && k != null) viewModel.simpanFotoGaleri(k, uri, konteks.contentResolver)
    }

    fun pilihGaleri(kategori: String) {
        kategoriGaleri = kategori
        pemilihGaleri.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    fun mintaFoto(kategori: String) {
        val ada = ContextCompat.checkSelfPermission(konteks, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (ada) {
            viewModel.bukaKamera(kategori)
        } else {
            kategoriMenungguIzin = kategori
            pemintaIzin.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(state.galat) {
        val pesan = state.galat ?: return@LaunchedEffect
        snackbar.showSnackbar(pesan)
        viewModel.tutupKabar()
    }

    // Kembali dari form kembali ke daftar outlet, bukan keluar modul.
    BackHandler(enabled = state.outletDiisi != null) { viewModel.tutupForm() }

    Scaffold(
        containerColor = WarnaIos.Latar,
        snackbarHost = { SnackbarHost(snackbar, Modifier.navigationBarsPaddingKaca()) },
        topBar = {
            BilahJudulIos(
                judul = if (state.outletDiisi == null) "Ceklist Harian" else state.namaOutletDiisi,
                subjudul = tanggalPanjangIndonesia(state.tanggal),
                onKembali = { if (state.outletDiisi != null) viewModel.tutupForm() else onExit() },
                aksi = {
                    if (state.outletDiisi == null) {
                        TombolBundarIos(IkonIos.Refresh, "Muat ulang", { viewModel.muatUlang() })
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.memuat && state.outlets.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = WarnaIos.Aksen)
                }
                state.terkirim -> PanelTerkirim(
                    namaOutlet = state.namaOutletDiisi,
                    onSelesai = viewModel::tutupForm,
                )
                state.outletDiisi == null -> DaftarOutletCeklist(state, viewModel::mulaiIsi) { viewModel.muatUlang() }
                else -> FormCeklist(
                    state = state,
                    viewModel = viewModel,
                    onMintaFoto = ::mintaFoto,
                    onGaleri = ::pilihGaleri,
                    onBukaFoto = { url, judul -> fotoDibuka = url to judul },
                )
            }
        }
    }

    val kategoriKamera = state.kameraUntuk
    if (kategoriKamera != null) {
        val kategori = KATEGORI_CEKLIST.find { it.kunci == kategoriKamera }
        val bagian = BagianBebas.entries.find { it.kunci == kategoriKamera }
        ModalBottomSheet(
            onDismissRequest = viewModel::tutupKamera,
            sheetState = lembarKamera,
            containerColor = WarnaIos.Kartu,
        ) {
            Column(Modifier.padding(horizontal = UkuranIos.TepiLayar).padding(bottom = 24.dp)) {
                Text("Foto ${kategori?.label ?: bagian?.label.orEmpty()}", style = TipeIos.Utama)
                Text(kategori?.petunjukFoto ?: "Opsional", style = TipeIos.Catatan)
                Spacer(Modifier.height(12.dp))
                KameraFotoSheet(
                    onDiambil = { bitmap -> viewModel.simpanFoto(kategoriKamera, bitmap) },
                    onBatal = viewModel::tutupKamera,
                )
            }
        }
    }

    fotoDibuka?.let { (url, judul) -> DialogFoto(url, judul) { fotoDibuka = null } }
}

/* ------------------------------ daftar outlet ------------------------------ */

@Composable
private fun DaftarOutletCeklist(
    state: CeklistHarianUiState,
    onPilih: (String) -> Unit,
    onCoba: () -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(UkuranIos.TepiLayar).denganRuangNav(),
        verticalArrangement = Arrangement.spacedBy(UkuranIos.JarakKartu),
    ) {
        item { PanelHariIni(state) }

        when {
            state.outlets.isEmpty() && state.galatMuat == null && !state.memuat -> item {
                KartuPanel { PanelKosong("Belum ada outlet binaan. Hubungi admin untuk penugasan outlet.") }
            }
            state.outlets.isEmpty() -> item {
                PanelGalatIos("Daftar outlet belum termuat.", onCoba)
            }
            else -> {
                item { LabelSeksiIos("Pilih outlet yang dikunjungi", Modifier.padding(start = 16.dp, top = 4.dp)) }
                items(state.outlets, key = { it.id }) { outlet ->
                    KartuOutletCeklist(
                        nama = outlet.nama,
                        laporan = state.laporan[outlet.id],
                        onKlik = { onPilih(outlet.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PanelHariIni(state: CeklistHarianUiState) {
    val total = state.outlets.size
    val selesai = state.jumlahSelesai
    KartuPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IkonBulatIos(IkonIos.Checklist, WarnaIos.Aksen, ukuran = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Kunjungan hari ini", style = TipeIos.Utama)
                Text(
                    "5 kategori wajib foto · langsung terkirim ke Regional Manager",
                    style = TipeIos.Catatan,
                )
            }
        }
        if (total > 0) {
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$selesai", style = TipeIos.AngkaBesar)
                Text(" / $total outlet sudah dicek", Modifier.padding(bottom = 4.dp), style = TipeIos.Catatan)
            }
            Spacer(Modifier.height(8.dp))
            BarProgres(rasio = selesai / total.toFloat(), tinggi = 8)
        }
    }
}

@Composable
private fun KartuOutletCeklist(nama: String, laporan: LaporanCeklist?, onKlik: () -> Unit) {
    KartuIos(onKlik = onKlik, padding = PaddingValues(horizontal = UkuranIos.PaddingKartu, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IkonBulatIos(
                if (laporan != null) IkonIos.CheckCircle else IkonIos.Storefront,
                if (laporan != null) WarnaIos.Hijau else WarnaIos.Aksen,
                ukuran = 38.dp,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(nama, style = TipeIos.Utama, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    when {
                        laporan == null -> "Belum dicek hari ini"
                        laporan.sudahDitinjau -> "Terkirim ${jamJakarta(laporan.diperbaruiPada)} · disetujui RM"
                        else -> "Terkirim ${jamJakarta(laporan.diperbaruiPada)} · menunggu RM"
                    },
                    style = TipeIos.Catatan.copy(
                        color = if (laporan != null) NadaIos.SUKSES.teks else WarnaIos.LabelKedua,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            LencanaIos(
                if (laporan != null) "Edit" else "Mulai",
                if (laporan != null) NadaIos.SUKSES else NadaIos.AKSEN,
                titik = false,
            )
            Spacer(Modifier.width(6.dp))
            Icon(IkonIos.ChevronRight, null, tint = WarnaIos.LabelKetiga, modifier = Modifier.size(15.dp))
        }
        val tanggapan = laporan?.tanggapanRm
        if (!tanggapan.isNullOrBlank()) {
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 50.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(IkonIos.EditNote, null, tint = NadaIos.INFO.teks, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "${laporan.namaPeninjau ?: "RM"}: $tanggapan",
                    style = TipeIos.Catatan.copy(color = NadaIos.INFO.teks, lineHeight = 17.sp),
                )
            }
        }
    }
}

/* ---------------------------------- form ----------------------------------- */

@Composable
private fun FormCeklist(
    state: CeklistHarianUiState,
    viewModel: CeklistHarianViewModel,
    onMintaFoto: (String) -> Unit,
    onGaleri: (String) -> Unit,
    onBukaFoto: (String?, String) -> Unit,
) {
    val daftar = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.weight(1f),
            state = daftar,
            contentPadding = PaddingValues(UkuranIos.TepiLayar),
            verticalArrangement = Arrangement.spacedBy(UkuranIos.JarakKartu),
        ) {
            item(key = "kemajuan") { PanelKemajuan(state, viewModel::semuaBaik) }
            items(KATEGORI_CEKLIST, key = { it.kunci }) { kategori ->
                KartuKategori(kategori, state, viewModel, onMintaFoto, onBukaFoto)
            }
            items(BagianBebas.entries, key = { it.kunci }) { bagian ->
                KartuBagianBebas(
                    bagian = bagian,
                    state = state,
                    viewModel = viewModel,
                    ingatkan = bagian == BagianBebas.TEMUAN && state.perluTemuan,
                    onMintaFoto = onMintaFoto,
                    onGaleri = onGaleri,
                    onBukaFoto = onBukaFoto,
                )
            }
            item(key = "catatan") {
                KartuPanel {
                    Text("Catatan tambahan", style = TipeIos.Utama)
                    Text("Opsional", style = TipeIos.Catatan)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = state.catatan,
                        onValueChange = viewModel::ubahCatatan,
                        placeholder = { Text("Hal lain yang perlu diketahui RM", fontSize = 14.sp) },
                        modifier = Modifier.fillMaxWidth().height(96.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 15.sp),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        shape = UkuranIos.SudutKontrol,
                        colors = warnaKolomIos(),
                    )
                }
            }
        }

        // Tombol kirim menempel di bawah — selalu terlihat, jadi AM tidak perlu
        // menggulir ke dasar untuk tahu apa yang masih kurang.
        BilahKirim(state) {
            if (state.halangan != null) {
                // Lompat ke kategori pertama yang belum beres.
                val indeks = KATEGORI_CEKLIST.indexOfFirst { !kategoriLengkap(it, state.isian, state.foto) }
                if (indeks >= 0) scope.launch { daftar.animateScrollToItem(indeks + 1) }
            }
            viewModel.kirim()
        }
    }
}

@Composable
private fun PanelKemajuan(state: CeklistHarianUiState, onSemuaBaik: () -> Unit) {
    val total = KATEGORI_CEKLIST.size
    val lengkap = state.kategoriLengkap
    val adaBelumDinilai = KATEGORI_CEKLIST.any { k -> k.butir.any { state.isian[it.kunci]?.nilai == null } }
    KartuPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                LabelSeksiIos("Kelengkapan")
                Text(
                    if (lengkap == total) "Siap dikirim" else "$lengkap dari $total kategori lengkap",
                    style = TipeIos.Utama.copy(color = if (lengkap == total) NadaIos.SUKSES.teks else WarnaIos.Label),
                )
            }
            Text("${lengkap * 100 / total}%", style = TipeIos.AngkaBesar)
        }
        Spacer(Modifier.height(10.dp))
        BarProgres(rasio = lengkap / total.toFloat(), tinggi = 8)
        if (state.laporanLama?.sudahDitinjau == true) {
            Spacer(Modifier.height(10.dp))
            Text(
                "Ceklist ini sudah disetujui RM. Menyimpan perubahan akan mengirim ulang untuk disetujui.",
                style = TipeIos.Catatan.copy(color = NadaIos.PERINGATAN.teks),
            )
        }
        if (adaBelumDinilai) {
            Spacer(Modifier.height(12.dp))
            TombolKeduaIos(
                "Semua sesuai — tandai Baik",
                onKlik = onSemuaBaik,
                ikon = IkonIos.CheckCircle,
                warna = WarnaIos.Hijau,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Hanya mengisi yang belum dinilai. Foto tetap wajib per kategori.",
                style = TipeIos.Kecil,
            )
        }
    }
}

@Composable
private fun KartuKategori(
    kategori: KategoriCeklist,
    state: CeklistHarianUiState,
    viewModel: CeklistHarianViewModel,
    onMintaFoto: (String) -> Unit,
    onBukaFoto: (String?, String) -> Unit,
) {
    val lengkap = kategoriLengkap(kategori, state.isian, state.foto)
    val foto = state.foto[kategori.kunci].orEmpty()
    val mengunggah = state.mengunggah[kategori.kunci] ?: 0

    KartuPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IkonBulatIos(ikonKategori(kategori.kunci), if (lengkap) WarnaIos.Hijau else WarnaIos.Aksen, ukuran = 34.dp)
            Spacer(Modifier.width(10.dp))
            Text(kategori.label, Modifier.weight(1f), style = TipeIos.Judul3)
            if (lengkap) {
                LencanaIos("Lengkap", NadaIos.SUKSES, ikon = IkonIos.Check)
            }
        }

        kategori.butir.forEachIndexed { i, butir ->
            val isian = state.isian[butir.kunci]
            Spacer(Modifier.height(if (i == 0) 14.dp else 18.dp))
            if (kategori.punyaSubItem) {
                Text(butir.label, style = TipeIos.Keterangan.copy(fontWeight = FontWeight.SemiBold))
                Spacer(Modifier.height(8.dp))
            }
            PilihanNilai(isian?.nilai) { viewModel.nilai(butir, it) }
            if (isian?.nilai != null) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = isian.keterangan,
                    onValueChange = { viewModel.keterangan(butir, it) },
                    placeholder = { Text("Keterangan", fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 15.sp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    shape = UkuranIos.SudutKontrol,
                    colors = warnaKolomIos(),
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        BarisFoto(
            kunci = kategori.kunci,
            judul = kategori.petunjukFoto,
            wajib = true,
            bolehGaleri = false,
            label = kategori.label,
            state = state,
            viewModel = viewModel,
            onMintaFoto = onMintaFoto,
            onGaleri = {},
            onBukaFoto = onBukaFoto,
        )
        if (foto.isEmpty() && mengunggah == 0) {
            Spacer(Modifier.height(6.dp))
            Text("Foto wajib", style = TipeIos.Kecil.copy(color = NadaIos.BAHAYA.teks, fontWeight = FontWeight.SemiBold))
        }
    }
}

/**
 * Judul, hitungan, dan deret foto satu kategori/bagian — satu bentuk yang sama
 * untuk kategori penilaian (wajib) dan bagian bebas (opsional).
 */
@Composable
private fun BarisFoto(
    kunci: String,
    judul: String,
    wajib: Boolean,
    bolehGaleri: Boolean,
    label: String,
    state: CeklistHarianUiState,
    viewModel: CeklistHarianViewModel,
    onMintaFoto: (String) -> Unit,
    onGaleri: (String) -> Unit,
    onBukaFoto: (String?, String) -> Unit,
) {
    val foto = state.foto[kunci].orEmpty()
    val mengunggah = state.mengunggah[kunci] ?: 0
    val masihMuat = foto.size + mengunggah < FOTO_MAKS_PER_KATEGORI
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(judul, Modifier.weight(1f), style = TipeIos.Catatan.copy(fontWeight = FontWeight.SemiBold))
        Text(
            "${foto.size}/$FOTO_MAKS_PER_KATEGORI",
            style = TipeIos.Kecil.copy(
                color = if (wajib && foto.isEmpty()) NadaIos.BAHAYA.teks else WarnaIos.LabelKedua,
            ),
        )
    }
    Spacer(Modifier.height(8.dp))
    DeretFoto(
        foto = foto,
        mengunggah = mengunggah,
        onPerluUrl = { viewModel.pastikanUrlFoto(kunci, it.path) },
        onBuka = { onBukaFoto(it.url, label) },
        onHapus = { viewModel.hapusFoto(kunci, it.path) },
        onTambah = if (masihMuat) {
            { onMintaFoto(kunci) }
        } else {
            null
        },
        onGaleri = if (bolehGaleri && masihMuat) {
            { onGaleri(kunci) }
        } else {
            null
        },
    )
}

/**
 * Online Review, Temuan, dan Perbaikan: baris teks bebas yang bisa ditambah dan
 * dihapus, plus foto opsional. Tidak ada penilaian — cukup ditulis apa adanya.
 */
@Composable
private fun KartuBagianBebas(
    bagian: BagianBebas,
    state: CeklistHarianUiState,
    viewModel: CeklistHarianViewModel,
    ingatkan: Boolean,
    onMintaFoto: (String) -> Unit,
    onGaleri: (String) -> Unit,
    onBukaFoto: (String?, String) -> Unit,
) {
    val isi = state.teks(bagian)
    KartuPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IkonBulatIos(ikonBagian(bagian), warnaBagian(bagian), ukuran = 34.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(bagian.label, style = TipeIos.Judul3)
                Text("Opsional", style = TipeIos.Catatan)
            }
        }
        if (ingatkan) {
            Spacer(Modifier.height(10.dp))
            Text(
                "Ada penilaian Perhatian/Buruk — catat temuannya supaya RM tahu apa masalahnya.",
                style = TipeIos.Catatan.copy(color = NadaIos.PERINGATAN.teks),
            )
        }
        isi.forEachIndexed { i, teks ->
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = teks,
                    onValueChange = { viewModel.ubahBaris(bagian, i, it) },
                    placeholder = { Text(bagian.contoh, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    modifier = Modifier.weight(1f),
                    textStyle = LocalTextStyle.current.copy(fontSize = 15.sp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    shape = UkuranIos.SudutKontrol,
                    colors = warnaKolomIos(),
                )
                Spacer(Modifier.width(8.dp))
                TombolBundarIos(IkonIos.Close, "Hapus", { viewModel.hapusBaris(bagian, i) }, warnaIkon = WarnaIos.Merah)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .tekanIos({ viewModel.tambahBaris(bagian) })
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(IkonIos.Add, null, tint = WarnaIos.Aksen, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(bagian.teksTambah, color = WarnaIos.Aksen, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(12.dp))
        BarisFoto(
            kunci = bagian.kunci,
            judul = if (bagian.bolehGaleri) "Foto / screenshot (opsional)" else "Foto (opsional)",
            wajib = false,
            bolehGaleri = bagian.bolehGaleri,
            label = bagian.label,
            state = state,
            viewModel = viewModel,
            onMintaFoto = onMintaFoto,
            onGaleri = onGaleri,
            onBukaFoto = onBukaFoto,
        )
    }
}

@Composable
private fun BilahKirim(state: CeklistHarianUiState, onKirim: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = UkuranIos.TepiLayar)
            .padding(top = 8.dp)
            // Bilah ini di dalam ShellKaca: beri ruang di atas kapsul nav bawah.
            .navigationBarsPaddingKaca(),
    ) {
        val halangan = state.halangan
        Text(
            when {
                state.sedangMengunggah -> "Menunggu foto selesai diunggah..."
                halangan != null -> halangan
                state.laporanLama != null -> "Semua lengkap. Simpan perubahan untuk RM."
                else -> "Semua lengkap. Siap dikirim ke Regional Manager."
            },
            Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            style = TipeIos.Catatan.copy(
                color = if (halangan == null && !state.sedangMengunggah) NadaIos.SUKSES.teks else NadaIos.PERINGATAN.teks,
                fontWeight = FontWeight.SemiBold,
            ),
            maxLines = 2,
        )
        TombolUtamaIos(
            if (state.laporanLama != null) "Simpan perubahan" else "Kirim ke Regional Manager",
            onKlik = onKirim,
            // Tetap bisa diketuk saat belum lengkap: ketukannya menggulir ke bagian
            // yang kurang, jauh lebih jelas daripada tombol mati tanpa penjelasan.
            aktif = !state.mengirim && !state.sedangMengunggah,
            memuat = state.mengirim,
            ikon = IkonIos.CheckCircle,
            warna = if (state.halangan == null) WarnaIos.Aksen else WarnaIos.Abu,
        )
    }
}

@Composable
private fun PanelTerkirim(namaOutlet: String, onSelesai: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(UkuranIos.TepiLayar).navigationBarsPaddingKaca(),
        verticalArrangement = Arrangement.Center,
    ) {
        KeadaanIos(
            ikon = IkonIos.CheckCircle,
            judul = "Ceklist terkirim",
            pesan = "Ceklist $namaOutlet sudah masuk dan Regional Manager sudah menerima notifikasinya.",
            nada = NadaIos.SUKSES,
        )
        TombolUtamaIos("Kembali ke daftar outlet", onKlik = onSelesai)
    }
}
