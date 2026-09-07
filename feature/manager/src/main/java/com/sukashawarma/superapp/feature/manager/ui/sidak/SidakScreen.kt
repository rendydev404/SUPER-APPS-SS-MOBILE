package com.sukashawarma.superapp.feature.manager.ui.sidak

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.sukashawarma.superapp.feature.manager.domain.ItemSidak
import com.sukashawarma.superapp.feature.manager.domain.KeadaanSidak
import com.sukashawarma.superapp.feature.manager.domain.KeputusanSidak
import com.sukashawarma.superapp.feature.manager.domain.labelSection
import com.sukashawarma.superapp.feature.manager.domain.waktuJakarta
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

private val AMBER_LATAR = Color(0xFFFEF3C7)
private val AMBER_GARIS = Color(0xFFFCD34D)
private val AMBER_TEKS = Color(0xFF78350F)

/**
 * Sidak Inventaris — cermin `app/inventaris-sidak/` web, versi baca.
 *
 * Menampilkan laporan aset terbaru tiap outlet beserta hasil sidak yang sudah
 * tersimpan, dan memberi keputusan Baik/Rusak per item lalu menyimpannya.
 *
 * Penyimpanan lewat RPC `submit_sidak_inventaris` — `inventaris_sidak_reviews`
 * hanya punya policy SELECT, jadi tulisan langsung dari JWT pengguna ditolak.
 * RPC itu datang dari migrasi `20300131000000_submit_sidak_inventaris_rpc`;
 * selama belum dijalankan, tombol Simpan menjelaskan keadaannya.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SidakScreen(
    onExit: () -> Unit,
    viewModel: SidakViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.kabar, state.galat) {
        val pesan = state.kabar ?: state.galat
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
                    Text("Sidak Inventaris", fontWeight = FontWeight.Black, fontSize = 17.sp, color = SukaBrown, maxLines = 1)
                },
                navigationIcon = {
                    IconButton(onClick = onExit) {
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
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { PanelOutlet(state, viewModel) }
            if (!state.bolehMenyimpan) {
                item { CatatanTanpaWewenang() }
            }
            isiLaporan(state, viewModel)
            item { Spacer(Modifier.height(8.dp)) }
        }
    }

    state.foto?.let { foto ->
        DialogFoto(
            url = foto.url,
            memuat = foto.memuat,
            judul = foto.namaItem,
            outlet = foto.namaOutlet,
            onTutup = viewModel::tutupFoto,
        )
    }
}

@Composable
private fun PanelOutlet(state: SidakUiState, viewModel: SidakViewModel) {
    var menuOutlet by remember { mutableStateOf(false) }

    KartuPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Sidak Inventaris", color = SukaBrown, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(3.dp))
                Text(
                    "Cocokkan kondisi aset di outlet dengan laporan yang masuk.",
                    color = SukaGray400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 16.sp,
                )
            }
            if (state.memuat) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = SukaOrange)
            }
        }

        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = state.pencarian,
            onValueChange = viewModel::ubahPencarian,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Cari outlet...", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = SukaGray400, modifier = Modifier.size(18.dp)) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
        )

        Spacer(Modifier.height(10.dp))
        Box {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, SukaBrown.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth().clickable { menuOutlet = true },
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Storefront, null, tint = SukaOrange, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        state.namaOutlet(state.outletTerpilih),
                        Modifier.weight(1f),
                        color = SukaBrown,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Icon(Icons.Default.ArrowDropDown, null, tint = SukaBrown)
                }
            }
            DropdownMenu(expanded = menuOutlet, onDismissRequest = { menuOutlet = false }) {
                state.outletTerlihat.forEach { outlet ->
                    val keadaan = state.keadaan(outlet.id)
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(outlet.nama, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    keadaan.label,
                                    fontSize = 10.sp,
                                    color = when (keadaan) {
                                        KeadaanSidak.SUDAH_DISIDAK -> HijauTeks
                                        KeadaanSidak.MENUNGGU_SIDAK -> AMBER_TEKS
                                        KeadaanSidak.BELUM_ADA_LAPORAN -> SukaGray400
                                    },
                                )
                            }
                        },
                        onClick = { viewModel.pilihOutlet(outlet.id); menuOutlet = false },
                    )
                }
            }
        }

        state.outletTerpilih?.let { id ->
            Spacer(Modifier.height(10.dp))
            LencanaKeadaan(state.keadaan(id))
        }
    }
}

@Composable
private fun LencanaKeadaan(keadaan: KeadaanSidak) {
    val (latar, garis, teks) = when (keadaan) {
        KeadaanSidak.SUDAH_DISIDAK -> Triple(HijauLatar, HijauGaris, HijauTeks)
        KeadaanSidak.MENUNGGU_SIDAK -> Triple(AMBER_LATAR, AMBER_GARIS, AMBER_TEKS)
        KeadaanSidak.BELUM_ADA_LAPORAN -> Triple(
            SukaBrown.copy(alpha = 0.04f),
            GarisKartu,
            SukaGray400,
        )
    }
    Surface(shape = RoundedCornerShape(50), color = latar, border = BorderStroke(1.dp, garis)) {
        Text(
            keadaan.label.uppercase(),
            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            color = teks,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp,
        )
    }
}

@Composable
private fun CatatanTanpaWewenang() {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = AMBER_LATAR,
        border = BorderStroke(1.dp, AMBER_GARIS),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.Info, null, tint = AMBER_TEKS, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(9.dp))
            Text(
                "Laporan dan hasil sidak bisa ditinjau di sini, tetapi peran Anda " +
                    "tidak berwenang menyimpan hasil sidak.",
                color = AMBER_TEKS,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun PanelGalat(pesan: String) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MerahLatar,
        border = BorderStroke(1.dp, MerahGaris),
    ) {
        Text(pesan, Modifier.padding(14.dp), color = MerahTeks, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

private fun LazyListScope.isiLaporan(state: SidakUiState, viewModel: SidakViewModel) {
    val laporan = state.laporan
    if (laporan == null) {
        item {
            KartuPanel {
                PanelKosong(
                    if (state.memuat) {
                        "Memuat laporan inventaris..."
                    } else {
                        "Belum ada laporan inventaris untuk outlet ini."
                    }
                )
            }
        }
        return
    }

    item { PanelRingkasan(state) }
    item { BarisSection(state, viewModel) }
    items(state.itemTerlihat, key = { it.id }) { item ->
        KartuItem(
            item = item,
            keputusan = state.keputusan[item.id],
            bolehMemutuskan = state.bolehMenyimpan,
            onBukaFoto = { viewModel.bukaFoto(item) },
            onPutuskan = { pilihan -> viewModel.pilihKeputusan(item.id, pilihan) },
        )
    }
    if (state.bolehMenyimpan) {
        item { PanelSimpanSidak(state, viewModel) }
    } else {
        state.hasil?.catatan?.let { catatan ->
            item { PanelCatatanSidak(catatan) }
        }
    }
}

@Composable
private fun PanelSimpanSidak(state: SidakUiState, viewModel: SidakViewModel) {
    KartuPanel {
        Text(
            "CATATAN SIDAK",
            color = SukaGray400,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.6.sp,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = state.catatan,
            onValueChange = viewModel::ubahCatatan,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text("Temuan umum, lokasi aset, atau tindak lanjut (opsional)", fontSize = 11.sp)
            },
            minLines = 3,
            shape = RoundedCornerShape(12.dp),
        )

        Spacer(Modifier.height(12.dp))
        // Alasan belum bisa disimpan ditulis apa adanya. Tombol mati tanpa
        // keterangan memaksa pengguna menebak item mana yang terlewat.
        Text(
            state.halangan ?: "Semua item sudah diberi hasil.",
            color = if (state.halangan != null) AMBER_TEKS else HijauTeks,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 16.sp,
        )

        Spacer(Modifier.height(12.dp))
        Surface(
            Modifier.fillMaxWidth().clickable(enabled = state.siapDisimpan) { viewModel.simpan() },
            shape = RoundedCornerShape(14.dp),
            color = if (state.siapDisimpan) SukaOrange else SukaOrange.copy(alpha = 0.4f),
        ) {
            Row(
                Modifier.padding(vertical = 13.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (state.menyimpan) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(17.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    if (state.menyimpan) "Menyimpan..." else "Simpan hasil sidak",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}

@Composable
private fun PanelRingkasan(state: SidakUiState) {
    val laporan = state.laporan ?: return
    val ringkasan = state.ringkasan ?: return
    KartuPanel {
        Text(
            state.namaOutlet(state.outletTerpilih),
            color = SukaBrown,
            fontSize = 17.sp,
            fontWeight = FontWeight.Black,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            "Laporan AM · ${waktuJakarta(laporan.diperbaruiPada)}",
            color = SukaGray400,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${ringkasan.diperiksa}/${ringkasan.total} diperiksa",
                Modifier.weight(1f),
                color = SukaBrown,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                "${ringkasan.persen}%",
                color = SukaOrange,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
            )
        }
        Spacer(Modifier.height(6.dp))
        BarProgres(rasio = ringkasan.persen / 100f, tinggi = 6)

        Spacer(Modifier.height(10.dp))
        Text(
            if (ringkasan.bermasalah > 0) {
                "${ringkasan.bermasalah} item ditandai rusak"
            } else {
                "Belum ada item yang ditandai rusak"
            },
            color = if (ringkasan.bermasalah > 0) MerahTeks else SukaGray400,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
        laporan.catatan?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(8.dp))
            Text(
                "Catatan AM: $it",
                color = SukaBrown.copy(alpha = 0.75f),
                fontSize = 11.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun BarisSection(state: SidakUiState, viewModel: SidakViewModel) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ChipSection("Semua area", state.section == null) { viewModel.pilihSection(null) }
        state.sectionTersedia.forEach { section ->
            ChipSection(labelSection(section), state.section == section) {
                viewModel.pilihSection(section)
            }
        }
    }
}

@Composable
private fun ChipSection(label: String, aktif: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (aktif) SukaBrown else Color.White,
        border = BorderStroke(1.dp, if (aktif) SukaBrown else SukaBrown.copy(alpha = 0.15f)),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            label,
            Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            color = if (aktif) Color.White else SukaBrown.copy(alpha = 0.75f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
        )
    }
}

@Composable
private fun KartuItem(
    item: ItemSidak,
    keputusan: KeputusanSidak?,
    bolehMemutuskan: Boolean,
    onBukaFoto: () -> Unit,
    onPutuskan: (KeputusanSidak) -> Unit,
) {
    val (latar, garis, teks, label) = when (keputusan) {
        KeputusanSidak.BAIK -> Quad(HijauLatar, HijauGaris, HijauTeks, "BAIK")
        KeputusanSidak.RUSAK -> Quad(MerahLatar, MerahGaris, MerahTeks, "RUSAK")
        null -> Quad(SukaBrown.copy(alpha = 0.04f), GarisKartu, SukaGray400, "BELUM DICEK")
    }

    KartuPanel {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                Modifier.size(34.dp).background(latar, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    when (keputusan) {
                        KeputusanSidak.BAIK -> Icons.Default.CheckCircle
                        KeputusanSidak.RUSAK -> Icons.Default.WarningAmber
                        null -> Icons.Default.FactCheck
                    },
                    null,
                    tint = teks,
                    modifier = Modifier.size(17.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.nama,
                    color = SukaBrown,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 18.sp,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${labelSection(item.section)} · ${item.subsection}",
                    color = SukaGray400,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Surface(shape = RoundedCornerShape(50), color = latar, border = BorderStroke(1.dp, garis)) {
                Text(
                    label,
                    Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    color = teks,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.4.sp,
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Surface(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = SukaBrown.copy(alpha = 0.03f),
            border = BorderStroke(1.dp, GarisKartu),
        ) {
            Row(Modifier.padding(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("TARGET", color = SukaGray400, fontSize = 8.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(3.dp))
                    Text(item.targetTeks, color = SukaBrown, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Column(Modifier.weight(1f)) {
                    Text("TERINPUT", color = SukaGray400, fontSize = 8.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(3.dp))
                    Text(item.hasilTeks, color = SukaBrown, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        item.catatan?.let {
            Spacer(Modifier.height(8.dp))
            Text(
                "Catatan AM: $it",
                color = SukaGray400,
                fontSize = 10.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        if (bolehMemutuskan) {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TombolKeputusan(
                    label = "Baik",
                    ikon = Icons.Default.Check,
                    aktif = keputusan == KeputusanSidak.BAIK,
                    warna = HijauTeks,
                    latarAktif = Color(0xFF059669),
                    modifier = Modifier.weight(1f),
                ) { onPutuskan(KeputusanSidak.BAIK) }
                TombolKeputusan(
                    label = "Rusak",
                    ikon = Icons.Default.Close,
                    aktif = keputusan == KeputusanSidak.RUSAK,
                    warna = MerahTeks,
                    latarAktif = Color(0xFFDC2626),
                    modifier = Modifier.weight(1f),
                ) { onPutuskan(KeputusanSidak.RUSAK) }
            }
        }

        if (item.fotoPath.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = GarisKartu)
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.clickable(onClick = onBukaFoto),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.PhotoCamera, null, tint = SukaOrange, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(5.dp))
                Text(
                    "Lihat foto bukti",
                    color = SukaOrange,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}

/**
 * Tombol Baik/Rusak — cermin `DecisionButton` web, termasuk perilakunya: menekan
 * tombol yang sudah aktif membatalkan pilihannya.
 */
@Composable
private fun TombolKeputusan(
    label: String,
    ikon: androidx.compose.ui.graphics.vector.ImageVector,
    aktif: Boolean,
    warna: Color,
    latarAktif: Color,
    modifier: Modifier = Modifier,
    onKlik: () -> Unit,
) {
    Surface(
        modifier = modifier.clickable(onClick = onKlik),
        shape = RoundedCornerShape(12.dp),
        color = if (aktif) latarAktif else Color.White,
        border = BorderStroke(1.dp, if (aktif) latarAktif else warna.copy(alpha = 0.35f)),
    ) {
        Row(
            Modifier.padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                ikon,
                null,
                tint = if (aktif) Color.White else warna,
                modifier = Modifier.size(15.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                label,
                color = if (aktif) Color.White else warna,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

private data class Quad(val latar: Color, val garis: Color, val teks: Color, val label: String)

@Composable
private fun PanelCatatanSidak(catatan: String) {
    KartuPanel {
        Text(
            "CATATAN SIDAK",
            color = SukaGray400,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.6.sp,
        )
        Spacer(Modifier.height(6.dp))
        Text(catatan, color = SukaBrown, fontSize = 12.sp, lineHeight = 17.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun DialogFoto(
    url: String?,
    memuat: Boolean,
    judul: String,
    outlet: String,
    onTutup: () -> Unit,
) {
    Dialog(onDismissRequest = onTutup) {
        Surface(shape = RoundedCornerShape(24.dp), color = Color.White) {
            Column {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            judul,
                            color = SukaBrown,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(outlet, color = SukaGray400, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onTutup) {
                        Icon(Icons.Default.Close, "Tutup", tint = SukaBrown)
                    }
                }
                Box(
                    Modifier.fillMaxWidth().height(320.dp).background(Color(0xFF111827)),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        memuat -> CircularProgressIndicator(
                            Modifier.size(28.dp),
                            strokeWidth = 3.dp,
                            color = SukaOrange,
                        )
                        url == null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.ImageNotSupported,
                                null,
                                tint = Color(0xFFFDBA74),
                                modifier = Modifier.size(32.dp),
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Foto tidak dapat dimuat.",
                                color = Color(0xFFFED7AA),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        else -> AsyncImage(
                            model = url,
                            contentDescription = "Foto inventaris $judul di $outlet",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}
