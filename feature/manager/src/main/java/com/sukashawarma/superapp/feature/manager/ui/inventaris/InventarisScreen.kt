package com.sukashawarma.superapp.feature.manager.ui.inventaris

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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.sukashawarma.superapp.feature.manager.domain.IsianItem
import com.sukashawarma.superapp.feature.manager.domain.ItemMaster
import com.sukashawarma.superapp.feature.manager.domain.KondisiAset
import com.sukashawarma.superapp.feature.manager.domain.ModeItem
import com.sukashawarma.superapp.feature.manager.domain.halanganItem
import com.sukashawarma.superapp.feature.manager.domain.itemLengkap
import com.sukashawarma.superapp.feature.manager.domain.nilaiPenilaian
import com.sukashawarma.superapp.feature.manager.ui.BarProgres
import com.sukashawarma.superapp.feature.manager.ui.GarisKartu
import com.sukashawarma.superapp.feature.manager.ui.HijauGaris
import com.sukashawarma.superapp.feature.manager.ui.HijauLatar
import com.sukashawarma.superapp.feature.manager.ui.HijauTeks
import com.sukashawarma.superapp.feature.manager.ui.KartuPanel
import com.sukashawarma.superapp.feature.manager.ui.MerahGaris
import com.sukashawarma.superapp.feature.manager.ui.MerahLatar
import com.sukashawarma.superapp.feature.manager.ui.MerahTeks
import com.sukashawarma.superapp.feature.manager.ui.PanelKosong
import com.sukashawarma.superapp.presentation.theme.SukaBrown
import com.sukashawarma.superapp.presentation.theme.SukaCream
import com.sukashawarma.superapp.presentation.theme.SukaGray400
import com.sukashawarma.superapp.presentation.theme.SukaOrange
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private val PASIR = Color(0xFFF5D6A0)
private val KREM_KOLOM = Color(0xFFFFFAF5)
private val GARIS_KOLOM = Color(0xFFD9A06B).copy(alpha = 0.45f)

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
        containerColor = SukaCream,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Inventaris Outlet",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = SukaBrown,
                        maxLines = 1,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { if (state.outletDiisi != null) viewModel.tutupForm() else onExit() },
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = SukaBrown)
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::muatUlang) {
                        Icon(Icons.Default.Refresh, "Muat ulang", tint = SukaBrown)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.memuat -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SukaOrange)
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
            containerColor = Color.White,
        ) {
            Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
                Text(
                    state.master.find { it.id == itemKamera }?.nama ?: "Foto bukti",
                    color = SukaBrown,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(10.dp))
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
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { PanelSambutanInventaris() }

        if (state.outlets.isEmpty()) {
            item {
                PanelKosong("Tidak ada outlet binaan yang bisa diisi inventarisnya.")
            }
        } else {
            item {
                Text(
                    "PILIH OUTLET",
                    color = SukaBrown,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp,
                )
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
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = SukaBrown,
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("Data inventaris outlet", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(8.dp))
            Text(
                "Catat setiap aset sesuai kondisi sebenarnya. Setiap item wajib difoto. " +
                    "Data tersimpan sebagai database inventaris dan dapat diperbarui jika ada perubahan.",
                color = Color(0xFFFFE7D0),
                fontSize = 12.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun KartuOutletInventaris(nama: String, tersimpan: Boolean, onKlik: () -> Unit) {
    Surface(
        Modifier.fillMaxWidth().clickable(onClick = onKlik),
        shape = RoundedCornerShape(20.dp),
        color = if (tersimpan) HijauLatar else Color.White,
        border = BorderStroke(1.dp, if (tersimpan) HijauGaris else GarisKartu),
        shadowElevation = 2.dp,
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(38.dp).clip(CircleShape)
                    .background(if (tersimpan) HijauGaris else SukaOrange.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Storefront,
                    null,
                    tint = if (tersimpan) HijauTeks else SukaOrange,
                    modifier = Modifier.size(19.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    nama,
                    color = SukaBrown,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (tersimpan) "Inventaris tersimpan" else "Belum ada laporan",
                    color = if (tersimpan) HijauTeks else SukaGray400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                if (tersimpan) "EDIT" else "ISI",
                color = if (tersimpan) HijauTeks else SukaOrange,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.6.sp,
            )
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
            CircularProgressIndicator(color = SukaOrange)
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
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
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
                Text(
                    "PROGRESS PENGISIAN",
                    color = SukaOrange,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.7.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Inventaris ${state.namaOutletDiisi}",
                    color = SukaBrown,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${state.kemajuan}%", color = SukaBrown, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text(
                    "$lengkap dari ${state.master.size} item siap",
                    color = SukaGray400,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        BarProgres(rasio = state.kemajuan / 100f, tinggi = 10)
        Spacer(Modifier.height(8.dp))
        Text(
            if (state.kemajuan == 100) "Siap disimpan" else "Lengkapi jumlah dan foto setiap item",
            color = SukaGray400,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
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
                Text(
                    "LANGKAH PENGISIAN",
                    color = SukaOrange,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.7.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    kelompok.getOrNull(state.langkah)?.first ?: "Pilih area inventaris",
                    color = SukaBrown,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                "${(state.langkah + 1).coerceAtMost(kelompok.size)} / ${kelompok.size}",
                color = SukaGray400,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            kelompok.forEachIndexed { indeks, (_, isi) ->
                val selesai = isi.all { itemLengkap(it, state.isian[it.id]) }
                val aktif = indeks == state.langkah
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                selesai -> SukaBrown
                                aktif -> Color(0xFFFFF3E4)
                                else -> Color.White
                            }
                        )
                        .clickable { onPilih(indeks) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (selesai) {
                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    } else {
                        Text(
                            "${indeks + 1}",
                            color = if (aktif) SukaBrown else SukaGray400,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
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
        OutlinedButton(
            onClick = { onPilih(state.langkah - 1) },
            enabled = state.langkah > 0,
            modifier = Modifier.weight(1f),
        ) { Text("Sebelumnya", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        Button(
            onClick = { onPilih(state.langkah + 1) },
            enabled = state.langkah < total - 1,
            colors = ButtonDefaults.buttonColors(containerColor = SukaBrown),
            modifier = Modifier.weight(1f),
        ) { Text("Berikutnya", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun KolomPencarianItem(state: InventarisUiState, onUbah: (String) -> Unit) {
    val jumlah = state.kelompokTerlihat.sumOf { it.second.size }
    Column {
        OutlinedTextField(
            value = state.pencarian,
            onValueChange = onUbah,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Search, null, tint = SukaOrange) },
            placeholder = { Text("Cari dari semua item inventaris...", fontSize = 13.sp) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (state.pencarian.isBlank()) {
                "Cari nama item atau subbagian untuk menampilkan hasil dari seluruh area."
            } else {
                "$jumlah item ditemukan dari semua area."
            },
            color = SukaGray400,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun JudulSubsection(subsection: String, jumlah: Int) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = PASIR,
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(subsection, color = SukaBrown, fontSize = 14.sp, fontWeight = FontWeight.Black)
            Text(
                "$jumlah item · foto wajib per item",
                color = SukaBrown.copy(alpha = 0.65f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/**
 * Satu kartu aset — tata letaknya mengikuti `<article>` di form web: garis jingga
 * di tepi kiri, judul beserta lencana penilaian, lalu kolom-kolom berlabel di
 * atasnya, dan ditutup foto bukti berikut pratinjaunya.
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

    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, GarisKartu),
        shadowElevation = 2.dp,
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(4.dp).fillMaxHeight().background(SukaOrange))
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.nama,
                        Modifier.weight(1f),
                        color = SukaBrown,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Spacer(Modifier.width(8.dp))
                    Lencana(
                        teks = penilaian.replace('_', ' '),
                        latar = if (sesuai) HijauLatar else Color(0xFFFFEDD5),
                        garis = if (sesuai) HijauGaris else Color(0xFFFED7AA),
                        teksWarna = if (sesuai) HijauTeks else Color(0xFF9A3412),
                    )
                }
                if (item.catatanWajib) {
                    Spacer(Modifier.height(8.dp))
                    Lencana("Catatan wajib", MerahLatar, MerahGaris, MerahTeks)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    if (item.catatanWajib) {
                        "Catat total unit dan ukuran freezer yang tersedia (400L, 600L, atau 750L)."
                    } else {
                        item.targetTeks
                    },
                    color = SukaGray400,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
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
                            colors = CheckboxDefaults.colors(checkedColor = SukaBrown),
                        )
                        Text("Barang tersedia", color = SukaBrown, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Jumlah",
                            color = SukaBrown,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.width(12.dp))
                        KolomTeks(
                            nilai = isian.jumlah,
                            onUbah = { viewModel.ubahJumlah(item.id, it) },
                            modifier = Modifier.width(110.dp),
                            angka = true,
                        )
                        if (!item.satuan.isNullOrBlank()) {
                            Spacer(Modifier.width(10.dp))
                            Text(item.satuan, color = SukaGray400, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Kondisi", color = SukaBrown, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(12.dp))
                    PilihanKondisi(isian.kondisi) { viewModel.ubahKondisi(item.id, it) }
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
                        color = MerahTeks,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.Medium,
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
                    Text(halangan, color = MerahTeks, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
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
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFF3F4F6)),
            contentAlignment = Alignment.Center,
        ) {
            val url = isian.fotoUrl
            when {
                mengunggah -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp, color = SukaOrange)
                    Spacer(Modifier.height(8.dp))
                    Text("Mengunggah foto...", color = SukaGray400, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                url != null -> AsyncImage(
                    model = url,
                    contentDescription = "Foto bukti",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = SukaOrange)
                    Spacer(Modifier.height(8.dp))
                    Text("Menyiapkan pratinjau...", color = SukaGray400, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TombolFoto(adaFoto: Boolean, mengunggah: Boolean, onKlik: () -> Unit) {
    OutlinedButton(
        onClick = onKlik,
        enabled = !mengunggah,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
    ) {
        if (mengunggah) {
            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = SukaOrange)
            Spacer(Modifier.width(8.dp))
            Text("Mengunggah foto...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        } else {
            Icon(
                if (adaFoto) Icons.Default.CheckCircle else Icons.Default.PhotoCamera,
                null,
                tint = if (adaFoto) HijauTeks else SukaOrange,
                modifier = Modifier.size(17.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (adaFoto) "Foto tersimpan — ambil ulang" else "Ambil foto bukti (wajib)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (adaFoto) HijauTeks else SukaBrown,
            )
        }
    }
}

/** Label kecil di atas kolomnya, bentuk yang dipakai seluruh form web. */
@Composable
private fun KolomBerlabel(label: String, isi: @Composable () -> Unit) {
    Text(label, color = SukaBrown, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(5.dp))
    isi()
}

/** Kolom isian bergaya form web: latar krem, garis cokelat muda, sudut membulat. */
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
            { Text(it, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        },
        trailingIcon = akhiran?.let { { Text(it, color = SukaGray400, fontSize = 12.sp) } },
        keyboardOptions = KeyboardOptions(
            keyboardType = if (angka) KeyboardType.Decimal else KeyboardType.Text,
        ),
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
        shape = RoundedCornerShape(14.dp),
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
            placeholder = { Text("dd/mm/yyyy", fontSize = 12.sp) },
            trailingIcon = { Icon(Icons.Default.CalendarMonth, null, tint = SukaGray400) },
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
            shape = RoundedCornerShape(14.dp),
            colors = warnaKolom(false),
        )
        // Kolomnya dimatikan supaya papan ketik tidak muncul; lapisan klik ini yang
        // membuka kalender, karena kolom `enabled = false` tidak menerima klik.
        Box(Modifier.matchParentSize().clickable { terbuka = true })
    }

    if (terbuka) {
        val keadaan = rememberDatePickerState(initialSelectedDateMillis = isoKeMillis(iso))
        DatePickerDialog(
            onDismissRequest = { terbuka = false },
            confirmButton = {
                TextButton(onClick = {
                    terbuka = false
                    keadaan.selectedDateMillis?.let { onPilih(millisKeIso(it)) }
                }) { Text("Pilih") }
            },
            dismissButton = {
                TextButton(onClick = {
                    terbuka = false
                    onPilih("")
                }) { Text("Kosongkan") }
            },
        ) { DatePicker(state = keadaan) }
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

@Composable
private fun warnaKolom(galat: Boolean) = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = if (galat) Color(0xFFFEF2F2) else KREM_KOLOM,
    unfocusedContainerColor = if (galat) Color(0xFFFEF2F2) else KREM_KOLOM,
    disabledContainerColor = KREM_KOLOM,
    focusedBorderColor = SukaOrange,
    unfocusedBorderColor = if (galat) MerahGaris else GARIS_KOLOM,
    disabledBorderColor = GARIS_KOLOM,
    disabledTextColor = SukaBrown,
    disabledPlaceholderColor = SukaGray400,
    disabledTrailingIconColor = SukaGray400,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PilihanKondisi(terpilih: KondisiAset, onPilih: (KondisiAset) -> Unit) {
    var terbuka by remember { mutableStateOf(false) }
    Box {
        Surface(
            Modifier.clickable { terbuka = true },
            shape = RoundedCornerShape(14.dp),
            color = KREM_KOLOM,
            border = BorderStroke(1.dp, GARIS_KOLOM),
        ) {
            Row(
                Modifier.padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(terpilih.label, color = SukaBrown, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.Default.ArrowDropDown, null, tint = SukaBrown)
            }
        }
        DropdownMenu(expanded = terbuka, onDismissRequest = { terbuka = false }) {
            KondisiAset.entries.forEach { kondisi ->
                DropdownMenuItem(
                    text = { Text(kondisi.label, fontSize = 13.sp) },
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
        Text(
            "CATATAN LAPORAN",
            color = SukaOrange,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.7.sp,
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = catatan,
            onValueChange = onUbah,
            placeholder = { Text("Catatan umum untuk seluruh laporan (opsional)", fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth().height(96.dp),
            shape = RoundedCornerShape(14.dp),
        )
    }
}

@Composable
private fun TombolKirim(state: InventarisUiState, onKirim: () -> Unit) {
    val sudahAda = state.sudahTersimpan.contains(state.outletDiisi.orEmpty())
    Column {
        Button(
            onClick = onKirim,
            enabled = state.siapKirim,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SukaOrange),
        ) {
            Text(
                when {
                    state.mengirim -> "Menyimpan data inventaris..."
                    state.mengunggah.isNotEmpty() -> "Menunggu foto selesai diunggah..."
                    sudahAda -> "Simpan perubahan ${state.namaOutletDiisi}"
                    else -> "Simpan inventaris ${state.namaOutletDiisi}"
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        val halangan = state.halangan
        if (halangan != null) {
            Spacer(Modifier.height(8.dp))
            Text(halangan, color = MerahTeks, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PanelBerhasil(namaOutlet: String, onLanjut: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(72.dp).clip(CircleShape).background(HijauLatar),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.CheckCircle, null, tint = HijauTeks, modifier = Modifier.size(38.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text("Inventaris tersimpan", color = SukaBrown, fontSize = 19.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(6.dp))
        Text(
            "Data inventaris $namaOutlet sudah masuk ke database dan bisa diperbarui kapan saja.",
            color = SukaGray400,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onLanjut,
            colors = ButtonDefaults.buttonColors(containerColor = SukaBrown),
            shape = RoundedCornerShape(16.dp),
        ) { Text("Kembali ke daftar outlet", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun Lencana(teks: String, latar: Color, garis: Color, teksWarna: Color) {
    Surface(shape = RoundedCornerShape(50), color = latar, border = BorderStroke(1.dp, garis)) {
        Text(
            teks.uppercase(),
            Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
            color = teksWarna,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp,
        )
    }
}
