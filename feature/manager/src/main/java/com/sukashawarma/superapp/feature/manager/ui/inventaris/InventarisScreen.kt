package com.sukashawarma.superapp.feature.manager.ui.inventaris

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.sukashawarma.superapp.core.camera.KameraFotoSheet
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.RealtimeTables
import com.sukashawarma.superapp.core.ui.SukaDropdownHeader
import com.sukashawarma.superapp.core.ui.SukaDropdownMenu
import com.sukashawarma.superapp.core.ui.SukaDropdownMenuItem
import com.sukashawarma.superapp.core.ui.SukaFilterDropdown
import com.sukashawarma.superapp.core.ui.ios.BilahJudulIos
import com.sukashawarma.superapp.core.ui.ios.IkonBulatIos
import com.sukashawarma.superapp.core.ui.ios.KartuIos
import com.sukashawarma.superapp.core.ui.ios.KeadaanIos
import com.sukashawarma.superapp.core.ui.ios.KolomCariIos
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
import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.core.ui.kaca.denganRuangNav
import com.sukashawarma.superapp.core.ui.kaca.navigationBarsPaddingKaca
import com.sukashawarma.superapp.feature.manager.domain.IsianItem
import com.sukashawarma.superapp.feature.manager.domain.ItemMaster
import com.sukashawarma.superapp.feature.manager.domain.KondisiAset
import com.sukashawarma.superapp.feature.manager.domain.ModeItem
import com.sukashawarma.superapp.feature.manager.domain.halanganItem
import com.sukashawarma.superapp.feature.manager.domain.itemLengkap
import com.sukashawarma.superapp.feature.manager.domain.nilaiPenilaian
import com.sukashawarma.superapp.feature.manager.ui.BarProgres
import com.sukashawarma.superapp.feature.manager.ui.JudulPanel
import com.sukashawarma.superapp.feature.manager.ui.KartuPanel
import com.sukashawarma.superapp.feature.manager.ui.PanelKosong
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Label kecil di atas/di samping kolom formulir — hitam semibold, bukan abu, supaya terbaca sebagai label. */
private val GayaLabelKolom = TipeIos.Catatan.copy(color = WarnaIos.Label, fontWeight = FontWeight.SemiBold)

/** Teks galat/halangan di bawah kolom — merah bernada iOS. */
private val GayaGalat = TipeIos.Catatan.copy(color = NadaIos.BAHAYA.teks, fontWeight = FontWeight.Medium)

/**
 * Inventaris Outlet — cermin `apps/inventori` web, jalur pengisian.
 *
 * Satu layar dengan dua keadaan, persis seperti web yang memakai halaman yang
 * sama untuk `/dashboard` dan `/dashboard/edit/[outletId]`: daftar outlet lalu
 * form bertahap per area. Setiap item wajib berfoto.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventarisScreen(
    onExit: () -> Unit,
    viewModel: InventarisViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()

    // Menunggu `plan/inventaris-realtime-publication.sql` dijalankan di Supabase;
    // sebelum itu langganan ini hidup tapi tidak pernah menerima event.
    RealtimeRefresh(
        RealtimeTables.INVENTARIS_SUBMISSIONS,
        RealtimeTables.INVENTARIS_MASTER_ITEMS,
    ) { viewModel.muatUlang(silent = true) }
    val snackbar = remember { SnackbarHostState() }
    val konteks = LocalContext.current
    val lembarKamera = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var itemMenungguIzin by remember { mutableStateOf<String?>(null) }
    val pemintaIzin = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { diberikan ->
        val item = itemMenungguIzin
        itemMenungguIzin = null
        if (diberikan && item != null) viewModel.bukaKamera(item)
    }

    // Kamera hanya boleh dibuka setelah izin ada; tanpa penjaga ini lembar
    // kamera muncul kosong dan pengguna mengira aplikasinya rusak.
    fun mintaFoto(itemId: String) {
        val ada = ContextCompat.checkSelfPermission(konteks, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (ada) {
            viewModel.bukaKamera(itemId)
        } else {
            itemMenungguIzin = itemId
            pemintaIzin.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(state.galat, state.kabar) {
        val pesan = state.galat ?: state.kabar
        if (pesan != null) {
            snackbar.showSnackbar(pesan)
            viewModel.tutupKabar()
        }
    }

    Scaffold(
        containerColor = WarnaIos.Latar,
        snackbarHost = { SnackbarHost(snackbar, Modifier.navigationBarsPaddingKaca()) },
        topBar = {
            BilahJudulIos(
                judul = "Inventaris Outlet",
                onKembali = { if (state.outletDiisi != null) viewModel.tutupForm() else onExit() },
                aksi = { TombolBundarIos(IkonIos.Refresh, "Muat ulang", viewModel::muatUlang) },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.memuat && state.outlets.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = WarnaIos.Aksen)
                }
                state.berhasilDikirim -> PanelBerhasil(
                    namaOutlet = state.namaOutletDiisi,
                    onLanjut = viewModel::tutupForm,
                )
                state.outletDiisi == null -> DaftarOutletInventaris(state, viewModel::mulaiIsi)
                else -> FormInventaris(state, viewModel) { id -> mintaFoto(id) }
            }
        }
    }

    val itemKamera = state.kameraUntuk
    if (itemKamera != null) {
        ModalBottomSheet(
            onDismissRequest = viewModel::tutupKamera,
            sheetState = lembarKamera,
            containerColor = WarnaIos.Kartu,
        ) {
            Column(Modifier.padding(horizontal = UkuranIos.TepiLayar).padding(bottom = 24.dp)) {
                Text(
                    state.master.find { it.id == itemKamera }?.nama ?: "Foto bukti",
                    style = TipeIos.Utama,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(12.dp))
                KameraFotoSheet(
                    onDiambil = { bitmap -> viewModel.simpanFoto(itemKamera, bitmap) },
                    onBatal = viewModel::tutupKamera,
                )
            }
        }
    }
}

/* ------------------------------ daftar outlet ------------------------------ */

@Composable
private fun DaftarOutletInventaris(
    state: InventarisUiState,
    onPilih: (String) -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(UkuranIos.TepiLayar).denganRuangNav(),
        verticalArrangement = Arrangement.spacedBy(UkuranIos.JarakKartu),
    ) {
        item { PanelSambutanInventaris() }

        if (state.outlets.isEmpty()) {
            item {
                KartuPanel { PanelKosong("Tidak ada outlet binaan yang bisa diisi inventarisnya.") }
            }
        } else {
            item {
                LabelSeksiIos("Pilih outlet", Modifier.padding(start = 16.dp, top = 4.dp))
            }
            items(state.outlets, key = { it.id }) { outlet ->
                KartuOutletInventaris(
                    nama = outlet.nama,
                    tersimpan = outlet.id in state.sudahTersimpan,
                    onKlik = { onPilih(outlet.id) },
                )
            }
        }
    }
}

@Composable
private fun PanelSambutanInventaris() {
    KartuPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IkonBulatIos(IkonIos.Inventory2, WarnaIos.Aksen, ukuran = 36.dp)
            Spacer(Modifier.width(12.dp))
            Text("Data inventaris outlet", Modifier.weight(1f), style = TipeIos.Judul3)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Catat setiap aset sesuai kondisi sebenarnya. Setiap item wajib difoto. " +
                "Data tersimpan sebagai database inventaris dan dapat diperbarui jika ada perubahan.",
            style = TipeIos.Catatan.copy(lineHeight = 18.sp),
        )
    }
}

@Composable
private fun KartuOutletInventaris(nama: String, tersimpan: Boolean, onKlik: () -> Unit) {
    KartuIos(
        onKlik = onKlik,
        padding = PaddingValues(horizontal = UkuranIos.PaddingKartu, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IkonBulatIos(IkonIos.Storefront, if (tersimpan) WarnaIos.Hijau else WarnaIos.Aksen, ukuran = 36.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(nama, style = TipeIos.Utama, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    if (tersimpan) "Inventaris tersimpan" else "Belum ada laporan",
                    style = TipeIos.Catatan.copy(color = if (tersimpan) NadaIos.SUKSES.teks else WarnaIos.LabelKedua),
                )
            }
            Spacer(Modifier.width(8.dp))
            LencanaIos(
                if (tersimpan) "Edit" else "Isi",
                if (tersimpan) NadaIos.SUKSES else NadaIos.AKSEN,
                titik = false,
            )
            Spacer(Modifier.width(6.dp))
            Icon(IkonIos.ChevronRight, null, tint = WarnaIos.LabelKetiga, modifier = Modifier.size(15.dp))
        }
    }
}

/* --------------------------------- form ---------------------------------- */

@Composable
private fun FormInventaris(
    state: InventarisUiState,
    viewModel: InventarisViewModel,
    onMintaFoto: (String) -> Unit,
) {
    if (state.memuatForm) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = WarnaIos.Aksen)
        }
    } else {
        IsiFormInventaris(state, viewModel, onMintaFoto)
    }
}

/**
 * Dipisah dari [FormInventaris] supaya cabang "sedang memuat" tidak pernah
 * keluar lebih awal dari satu badan composable yang sama — pola itu yang dulu
 * merusak slot table dan menjatuhkan layar Persetujuan.
 */
@Composable
private fun IsiFormInventaris(
    state: InventarisUiState,
    viewModel: InventarisViewModel,
    onMintaFoto: (String) -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(UkuranIos.TepiLayar).denganRuangNav(),
        verticalArrangement = Arrangement.spacedBy(UkuranIos.JarakKartu),
    ) {
        item { PanelKemajuan(state) }
        item { PanelLangkah(state, viewModel::pilihLangkah) }
        item { KolomPencarianItem(state, viewModel::ubahPencarian) }

        state.kelompokTerlihat.forEach { (subsection, isi) ->
            item(key = "judul-$subsection") { JudulSubsection(subsection, isi.size) }
            items(isi, key = { it.id }) { item ->
                KartuItemInventaris(
                    item = item,
                    isian = state.isianUntuk(item.id),
                    mengunggah = item.id in state.mengunggah,
                    viewModel = viewModel,
                    onMintaFoto = onMintaFoto,
                )
            }
        }

        if (state.pencarian.isBlank()) {
            item { NavigasiLangkah(state, viewModel::pilihLangkah) }
        }
        item { PanelCatatanLaporan(state.catatan, viewModel::ubahCatatan) }
        item { TombolKirim(state, viewModel::kirim) }
    }
}

@Composable
private fun PanelKemajuan(state: InventarisUiState) {
    val lengkap = state.master.count { itemLengkap(it, state.isian[it.id]) }
    KartuPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                LabelSeksiIos("Progress pengisian")
                Spacer(Modifier.height(2.dp))
                Text(
                    "Inventaris ${state.namaOutletDiisi}",
                    style = TipeIos.Utama,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text("${state.kemajuan}%", style = TipeIos.AngkaBesar, maxLines = 1)
                Text("$lengkap dari ${state.master.size} item siap", style = TipeIos.Kecil, maxLines = 1)
            }
        }
        Spacer(Modifier.height(12.dp))
        BarProgres(rasio = state.kemajuan / 100f, tinggi = 8)
        Spacer(Modifier.height(8.dp))
        val siap = state.kemajuan == 100
        Text(
            if (siap) "Siap disimpan" else "Lengkapi jumlah dan foto setiap item",
            style = TipeIos.Catatan.copy(
                color = if (siap) NadaIos.SUKSES.teks else WarnaIos.LabelKedua,
                fontWeight = if (siap) FontWeight.SemiBold else FontWeight.Normal,
            ),
        )
    }
}

/** Bulatan langkah per area — cermin `FormStepper` web. */
@Composable
private fun PanelLangkah(state: InventarisUiState, onPilih: (Int) -> Unit) {
    val kelompok = state.kelompok
    KartuPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                LabelSeksiIos("Langkah pengisian")
                Spacer(Modifier.height(2.dp))
                Text(
                    kelompok.getOrNull(state.langkah)?.first ?: "Pilih area inventaris",
                    style = TipeIos.Utama,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            LencanaIos(
                "${(state.langkah + 1).coerceAtMost(kelompok.size)} / ${kelompok.size}",
                NadaIos.NETRAL,
                titik = false,
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            kelompok.forEachIndexed { indeks, (_, isi) ->
                val selesai = isi.all { itemLengkap(it, state.isian[it.id]) }
                val aktif = indeks == state.langkah
                // Urutan prioritas warna sama dengan sebelumnya: selesai menang atas aktif.
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                selesai -> WarnaIos.Hijau
                                aktif -> WarnaIos.Aksen
                                else -> WarnaIos.Isian
                            }
                        )
                        .tekanIos({ onPilih(indeks) }),
                    contentAlignment = Alignment.Center,
                ) {
                    if (selesai) {
                        Icon(IkonIos.aktif(IkonIos.Check), null, tint = Color.White, modifier = Modifier.size(16.dp))
                    } else {
                        Text(
                            "${indeks + 1}",
                            color = if (aktif) Color.White else WarnaIos.LabelKedua,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NavigasiLangkah(state: InventarisUiState, onPilih: (Int) -> Unit) {
    val total = state.kelompok.size
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TombolKeduaIos(
            "Sebelumnya",
            onKlik = { onPilih(state.langkah - 1) },
            aktif = state.langkah > 0,
            modifier = Modifier.weight(1f),
        )
        TombolUtamaIos(
            "Berikutnya",
            onKlik = { onPilih(state.langkah + 1) },
            aktif = state.langkah < total - 1,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun KolomPencarianItem(state: InventarisUiState, onUbah: (String) -> Unit) {
    val jumlah = state.kelompokTerlihat.sumOf { it.second.size }
    Column {
        KolomCariIos(
            nilai = state.pencarian,
            onUbah = onUbah,
            modifier = Modifier.fillMaxWidth(),
            placeholder = "Cari dari semua item inventaris...",
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (state.pencarian.isBlank()) {
                "Cari nama item atau subbagian untuk menampilkan hasil dari seluruh area."
            } else {
                "$jumlah item ditemukan dari semua area."
            },
            Modifier.padding(horizontal = 4.dp),
            style = TipeIos.Kecil,
        )
    }
}

/** Judul area ala judul seksi iOS — teks di atas latar halaman, bukan blok berwarna. */
@Composable
private fun JudulSubsection(subsection: String, jumlah: Int) {
    Column(Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 10.dp)) {
        Text(subsection, style = TipeIos.Judul3.copy(fontWeight = FontWeight.Bold))
        Text("$jumlah item · foto wajib per item", style = TipeIos.Catatan)
    }
}

/**
 * Satu kartu aset — urutannya mengikuti `<article>` di form web: judul beserta
 * lencana penilaian, lalu kolom-kolom berlabel di atasnya, dan ditutup foto
 * bukti berikut pratinjaunya. Garis jingga tepi kiri web sengaja tidak dibawa;
 * kartu iOS dibedakan lewat bayangan, bukan garis.
 */
@Composable
private fun KartuItemInventaris(
    item: ItemMaster,
    isian: IsianItem,
    mengunggah: Boolean,
    viewModel: InventarisViewModel,
    onMintaFoto: (String) -> Unit,
) {
    // URL bertanda tangan hanya diminta untuk area yang sedang terbuka. Meminta
    // seluruh 87 sekaligus saat form dibuka membuat pembukaan form tersendat
    // padahal fotonya belum tentu dilihat.
    LaunchedEffect(item.id, isian.fotoPath) { viewModel.pastikanUrlFoto(item.id) }

    val penilaian = nilaiPenilaian(item, isian)
    val sesuai = penilaian == "sesuai"

    KartuPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(item.nama, Modifier.weight(1f), style = TipeIos.Utama)
            Spacer(Modifier.width(8.dp))
            LencanaIos(
                penilaian.replace('_', ' ').replaceFirstChar { it.uppercase() },
                if (sesuai) NadaIos.SUKSES else NadaIos.PERINGATAN,
            )
        }
        if (item.catatanWajib) {
            Spacer(Modifier.height(8.dp))
            LencanaIos("Catatan wajib", NadaIos.BAHAYA, titik = false, ikon = IkonIos.EditNote)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            if (item.catatanWajib) {
                "Catat total unit dan ukuran freezer yang tersedia (400L, 600L, atau 750L)."
            } else {
                item.targetTeks
            },
            style = TipeIos.Catatan.copy(lineHeight = 17.sp),
        )

        Spacer(Modifier.height(14.dp))
        if (item.mode == ModeItem.KEBERADAAN) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = isian.ada,
                    onCheckedChange = { ada ->
                        viewModel.ubahAda(item.id, ada)
                        // Menghilangkan barang tanpa mengubah kondisinya menyisakan
                        // "Baik" pada aset yang tidak ada — web pun ikut menurunkannya.
                        if (!ada) viewModel.ubahKondisi(item.id, KondisiAset.TIDAK_ADA)
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor = WarnaIos.Aksen,
                        uncheckedColor = WarnaIos.Abu,
                        checkmarkColor = Color.White,
                    ),
                )
                Text("Barang tersedia", style = TipeIos.Keterangan)
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Jumlah", Modifier.width(64.dp), style = GayaLabelKolom)
                KolomTeks(
                    nilai = isian.jumlah,
                    onUbah = { viewModel.ubahJumlah(item.id, it) },
                    modifier = Modifier.width(110.dp),
                    angka = true,
                )
                if (!item.satuan.isNullOrBlank()) {
                    Spacer(Modifier.width(10.dp))
                    Text(item.satuan, style = TipeIos.Catatan)
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Kondisi", Modifier.width(64.dp), style = GayaLabelKolom)
            PilihanKondisi(isian.kondisi, Modifier.weight(1f)) { viewModel.ubahKondisi(item.id, it) }
        }

        Spacer(Modifier.height(14.dp))
        KolomBerlabel("Tanggal pembelian") {
            KolomTanggal(isian.tanggalBeli) { viewModel.ubahTanggalBeli(item.id, it) }
        }
        Spacer(Modifier.height(10.dp))
        KolomBerlabel("Merek") {
            KolomTeks(
                nilai = isian.merek,
                onUbah = { viewModel.ubahMerek(item.id, it) },
                petunjuk = "Contoh: Modena, Maspion, Samsung",
            )
        }
        Spacer(Modifier.height(10.dp))
        KolomBerlabel("Harga") {
            KolomTeks(
                nilai = isian.harga,
                onUbah = { viewModel.ubahHarga(item.id, it) },
                petunjuk = "Contoh: 2500000",
                angka = true,
            )
        }
        Spacer(Modifier.height(10.dp))
        KolomBerlabel("Depresiasi (%/tahun)") {
            KolomTeks(
                nilai = isian.depresiasi,
                onUbah = { viewModel.ubahDepresiasi(item.id, it) },
                petunjuk = "Contoh: 10",
                angka = true,
                akhiran = "%",
            )
        }

        Spacer(Modifier.height(12.dp))
        KolomTeks(
            nilai = isian.catatan,
            onUbah = { viewModel.ubahCatatanItem(item.id, it) },
            petunjuk = if (item.catatanWajib) {
                "Wajib: contoh 400L 1 unit, 600L 1 unit, 750L tidak ada"
            } else {
                "Catatan item (opsional)"
            },
            galat = item.catatanWajib && isian.catatan.isBlank(),
        )
        if (item.catatanWajib && isian.catatan.isBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Tuliskan ukuran freezer dan kondisinya agar data aset lengkap.",
                Modifier.padding(horizontal = 4.dp),
                style = GayaGalat,
            )
        }

        Spacer(Modifier.height(14.dp))
        PratinjauFoto(isian, mengunggah)
        Spacer(Modifier.height(10.dp))
        TombolFoto(
            adaFoto = isian.adaFoto,
            mengunggah = mengunggah,
            onKlik = { onMintaFoto(item.id) },
        )

        val halangan = halanganItem(item, isian)
        if (halangan != null) {
            Spacer(Modifier.height(8.dp))
            Text(halangan, Modifier.padding(horizontal = 4.dp), style = GayaGalat.copy(fontWeight = FontWeight.SemiBold))
        }
    }
}

/**
 * Pratinjau foto bukti.
 *
 * Tiga keadaan sengaja dibedakan; menyatukannya jadi sekadar "ada foto" membuat
 * pengguna tidak tahu apakah fotonya benar-benar sampai ke server: sedang
 * diunggah, sudah tersimpan tetapi gambarnya belum siap, dan gambarnya siap.
 */
@Composable
private fun PratinjauFoto(isian: IsianItem, mengunggah: Boolean) {
    if (isian.adaFoto || mengunggah) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(190.dp)
                .clip(UkuranIos.SudutBlok)
                .background(WarnaIos.Latar),
            contentAlignment = Alignment.Center,
        ) {
            val url = isian.fotoUrl
            when {
                mengunggah -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.5.dp, color = WarnaIos.Aksen)
                    Spacer(Modifier.height(8.dp))
                    Text("Mengunggah foto...", style = TipeIos.Kecil)
                }
                url != null -> AsyncImage(
                    model = url,
                    contentDescription = "Foto bukti",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = WarnaIos.Aksen)
                    Spacer(Modifier.height(8.dp))
                    Text("Menyiapkan pratinjau...", style = TipeIos.Kecil)
                }
            }
        }
    }
}

/** Tombol foto bergaya tombol sekunder iOS: hijau bila foto sudah ada, aksen bila belum. */
@Composable
private fun TombolFoto(adaFoto: Boolean, mengunggah: Boolean, onKlik: () -> Unit) {
    TombolSatuBaris(
        teks = when {
            mengunggah -> "Mengunggah foto..."
            adaFoto -> "Foto tersimpan — ambil ulang"
            else -> "Ambil foto bukti (wajib)"
        },
        onKlik = onKlik,
        aktif = !mengunggah,
        memuat = mengunggah,
        terisi = false,
        warna = if (adaFoto) WarnaIos.Hijau else WarnaIos.Aksen,
        warnaTeks = if (adaFoto) NadaIos.SUKSES.teks else WarnaIos.Aksen,
        ikon = if (adaFoto) IkonIos.CheckCircle else IkonIos.PhotoCamera,
    )
}

/** Label kecil di atas kolomnya, bentuk yang dipakai seluruh form web. */
@Composable
private fun KolomBerlabel(label: String, isi: @Composable () -> Unit) {
    Text(label, Modifier.padding(start = 4.dp), style = GayaLabelKolom)
    Spacer(Modifier.height(6.dp))
    isi()
}

/** Kolom isian formulir iOS: latar putih, garis hairline, sudut 12. */
@Composable
private fun KolomTeks(
    nilai: String,
    onUbah: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    petunjuk: String? = null,
    angka: Boolean = false,
    akhiran: String? = null,
    galat: Boolean = false,
) {
    OutlinedTextField(
        value = nilai,
        onValueChange = onUbah,
        modifier = modifier,
        placeholder = petunjuk?.let {
            { Text(it, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        },
        trailingIcon = akhiran?.let { { Text(it, color = WarnaIos.LabelKedua, fontSize = 14.sp) } },
        keyboardOptions = KeyboardOptions(
            keyboardType = if (angka) KeyboardType.Decimal else KeyboardType.Text,
        ),
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(fontSize = 15.sp),
        shape = UkuranIos.SudutKontrol,
        isError = galat,
        colors = warnaKolom(galat),
    )
}

/**
 * Tanggal dipilih lewat kalender, bukan diketik.
 *
 * Yang tampil dd/mm/yyyy seperti web, yang dikirim tetap ISO — kolom
 * `purchase_date` bertipe `date`, jadi teks bebas baru ditolak server setelah
 * seluruh form terisi.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KolomTanggal(iso: String, onPilih: (String) -> Unit) {
    var terbuka by remember { mutableStateOf(false) }
    val tampil = remember(iso) { tanggalTampil(iso) }

    Box {
        OutlinedTextField(
            value = tampil,
            onValueChange = {},
            readOnly = true,
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("dd/mm/yyyy", fontSize = 14.sp) },
            trailingIcon = { Icon(IkonIos.CalendarMonth, null, modifier = Modifier.size(20.dp)) },
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 15.sp),
            shape = UkuranIos.SudutKontrol,
            colors = warnaKolom(false),
        )
        // Kolomnya dimatikan supaya papan ketik tidak muncul; lapisan klik ini yang
        // membuka kalender, karena kolom `enabled = false` tidak menerima klik.
        Box(Modifier.matchParentSize().clip(UkuranIos.SudutKontrol).clickable { terbuka = true })
    }

    if (terbuka) {
        val keadaan = rememberDatePickerState(initialSelectedDateMillis = isoKeMillis(iso))
        val warnaKalender = DatePickerDefaults.colors(
            containerColor = WarnaIos.Kartu,
            selectedDayContainerColor = WarnaIos.Aksen,
            selectedYearContainerColor = WarnaIos.Aksen,
            todayContentColor = WarnaIos.Aksen,
            todayDateBorderColor = WarnaIos.Aksen,
        )
        DatePickerDialog(
            onDismissRequest = { terbuka = false },
            confirmButton = {
                TextButton(onClick = {
                    terbuka = false
                    keadaan.selectedDateMillis?.let { onPilih(millisKeIso(it)) }
                }) { Text("Pilih", color = WarnaIos.Aksen, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = {
                    terbuka = false
                    onPilih("")
                }) { Text("Kosongkan", color = WarnaIos.Merah) }
            },
            shape = UkuranIos.SudutKartu,
            colors = warnaKalender,
        ) { DatePicker(state = keadaan, colors = warnaKalender) }
    }
}

private fun tanggalTampil(iso: String): String = runCatching {
    val tanggal = LocalDate.parse(iso)
    "%02d/%02d/%04d".format(tanggal.dayOfMonth, tanggal.monthValue, tanggal.year)
}.getOrDefault("")

private fun isoKeMillis(iso: String): Long? = runCatching {
    LocalDate.parse(iso).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}.getOrNull()

/** DatePicker memberi tengah malam UTC; membacanya di zona lain menggeser sehari. */
private fun millisKeIso(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()

/**
 * Warna kolom versi lokal, bukan [warnaKolomIos] bersama: kolom tanggal di sini
 * sengaja `enabled = false` (supaya papan ketik tidak muncul) sehingga butuh warna
 * nonaktif yang tetap terbaca, dan kolom catatan wajib memakai keadaan galat Material.
 */
@Composable
private fun warnaKolom(galat: Boolean) = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = WarnaIos.Kartu,
    unfocusedContainerColor = WarnaIos.Kartu,
    disabledContainerColor = WarnaIos.Kartu,
    errorContainerColor = WarnaIos.Merah.copy(alpha = 0.05f),
    focusedBorderColor = if (galat) WarnaIos.Merah else WarnaIos.Aksen,
    unfocusedBorderColor = if (galat) WarnaIos.Merah.copy(alpha = 0.6f) else WarnaIos.Pemisah,
    disabledBorderColor = WarnaIos.Pemisah,
    errorBorderColor = WarnaIos.Merah.copy(alpha = 0.6f),
    cursorColor = WarnaIos.Aksen,
    errorCursorColor = WarnaIos.Merah,
    focusedTextColor = WarnaIos.Label,
    unfocusedTextColor = WarnaIos.Label,
    disabledTextColor = WarnaIos.Label,
    errorTextColor = WarnaIos.Label,
    focusedPlaceholderColor = WarnaIos.Abu,
    unfocusedPlaceholderColor = WarnaIos.Abu,
    disabledPlaceholderColor = WarnaIos.Abu,
    errorPlaceholderColor = WarnaIos.Abu,
    focusedTrailingIconColor = WarnaIos.LabelKedua,
    unfocusedTrailingIconColor = WarnaIos.LabelKedua,
    disabledTrailingIconColor = WarnaIos.LabelKedua,
    errorTrailingIconColor = WarnaIos.Merah,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PilihanKondisi(
    terpilih: KondisiAset,
    modifier: Modifier = Modifier,
    onPilih: (KondisiAset) -> Unit,
) {
    var terbuka by remember { mutableStateOf(false) }
    Box(modifier) {
        SukaFilterDropdown(
            label = "",
            value = terpilih.label,
            expanded = terbuka,
            onClick = { terbuka = true },
            modifier = Modifier.fillMaxWidth(),
        )
        SukaDropdownMenu(expanded = terbuka, onDismissRequest = { terbuka = false }) {
            SukaDropdownHeader(title = "KONDISI ASET", onClose = { terbuka = false })
            KondisiAset.entries.forEach { kondisi ->
                SukaDropdownMenuItem(
                    text = kondisi.label,
                    selected = (kondisi == terpilih),
                    onClick = {
                        terbuka = false
                        onPilih(kondisi)
                    },
                )
            }
        }
    }
}

@Composable
private fun PanelCatatanLaporan(catatan: String, onUbah: (String) -> Unit) {
    KartuPanel {
        JudulPanel("Catatan laporan")
        OutlinedTextField(
            value = catatan,
            onValueChange = onUbah,
            placeholder = { Text("Catatan umum untuk seluruh laporan (opsional)", fontSize = 14.sp) },
            modifier = Modifier.fillMaxWidth().height(96.dp),
            textStyle = LocalTextStyle.current.copy(fontSize = 15.sp),
            shape = UkuranIos.SudutKontrol,
            colors = warnaKolomIos(),
        )
    }
}

@Composable
private fun TombolKirim(state: InventarisUiState, onKirim: () -> Unit) {
    val sudahAda = state.sudahTersimpan.contains(state.outletDiisi.orEmpty())
    Column {
        TombolSatuBaris(
            teks = when {
                state.mengirim -> "Menyimpan data inventaris..."
                state.mengunggah.isNotEmpty() -> "Menunggu foto selesai diunggah..."
                sudahAda -> "Simpan perubahan ${state.namaOutletDiisi}"
                else -> "Simpan inventaris ${state.namaOutletDiisi}"
            },
            onKlik = onKirim,
            aktif = state.siapKirim,
            memuat = state.mengirim,
            terisi = true,
            warna = WarnaIos.Aksen,
        )
        val halangan = state.halangan
        if (halangan != null) {
            Spacer(Modifier.height(8.dp))
            Text(halangan, Modifier.padding(horizontal = 4.dp), style = GayaGalat.copy(fontWeight = FontWeight.SemiBold))
        }
    }
}

@Composable
private fun PanelBerhasil(namaOutlet: String, onLanjut: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(UkuranIos.TepiLayar), contentAlignment = Alignment.Center) {
        KeadaanIos(
            ikon = IkonIos.CheckCircle,
            judul = "Inventaris tersimpan",
            pesan = "Data inventaris $namaOutlet sudah masuk ke database dan bisa diperbarui kapan saja.",
            nada = NadaIos.SUKSES,
            teksAksi = "Kembali ke daftar outlet",
            onAksi = onLanjut,
        )
    }
}

/**
 * Tombol iOS setinggi [UkuranIos.TinggiTombol] yang teksnya dipaksa satu baris.
 *
 * `TombolUtamaIos`/`TombolKeduaIos` bersama tidak memotong teks dan tidak bisa
 * memutar pemutar sambil tetap menampilkan teks; di sini teks tombol memuat nama
 * outlet yang panjangnya tak tentu dan status unggah harus tetap terbaca.
 * [terisi] = tombol utama (latar warna penuh), selain itu tombol sekunder (isian tipis).
 */
@Composable
private fun TombolSatuBaris(
    teks: String,
    onKlik: () -> Unit,
    aktif: Boolean,
    memuat: Boolean,
    terisi: Boolean,
    warna: Color,
    warnaTeks: Color = warna,
    ikon: ImageVector? = null,
) {
    val latar = when {
        terisi && aktif -> warna
        terisi -> WarnaIos.Isian
        else -> warna.copy(alpha = if (aktif) 0.12f else 0.06f)
    }
    val tinta = when {
        terisi && aktif -> Color.White
        terisi -> WarnaIos.Abu
        aktif -> warnaTeks
        else -> warnaTeks.copy(alpha = 0.5f)
    }
    Row(
        Modifier
            .fillMaxWidth()
            .height(UkuranIos.TinggiTombol)
            .clip(UkuranIos.SudutBlok)
            .background(latar)
            .tekanIos(onKlik, aktif = aktif)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (memuat) {
            CircularProgressIndicator(Modifier.size(18.dp), color = tinta, strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
        } else if (ikon != null) {
            Icon(ikon, null, tint = tinta, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            teks,
            color = tinta,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
