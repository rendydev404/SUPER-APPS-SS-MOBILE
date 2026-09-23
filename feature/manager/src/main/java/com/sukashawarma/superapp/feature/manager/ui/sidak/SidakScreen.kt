package com.sukashawarma.superapp.feature.manager.ui.sidak

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.RealtimeTables
import com.sukashawarma.superapp.core.ui.SukaDropdownHeader
import com.sukashawarma.superapp.core.ui.SukaDropdownMenu
import com.sukashawarma.superapp.core.ui.SukaDropdownMenuItem
import com.sukashawarma.superapp.core.ui.SukaFilterDropdown
import com.sukashawarma.superapp.core.ui.ios.BilahJudulIos
import com.sukashawarma.superapp.core.ui.ios.IkonBulatIos
import com.sukashawarma.superapp.core.ui.ios.KapsulPilihanIos
import com.sukashawarma.superapp.core.ui.ios.KolomCariIos
import com.sukashawarma.superapp.core.ui.ios.LabelSeksiIos
import com.sukashawarma.superapp.core.ui.ios.LencanaIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.PanelGalatIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.TombolBundarIos
import com.sukashawarma.superapp.core.ui.ios.TombolKapsulIos
import com.sukashawarma.superapp.core.ui.ios.TombolUtamaIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.ios.permukaanIos
import com.sukashawarma.superapp.core.ui.ios.tekanIos
import com.sukashawarma.superapp.core.ui.ios.warnaKolomIos
import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.core.ui.kaca.denganRuangNav
import com.sukashawarma.superapp.core.ui.kaca.navigationBarsPaddingKaca
import com.sukashawarma.superapp.feature.manager.domain.ItemSidak
import com.sukashawarma.superapp.feature.manager.domain.KeadaanSidak
import com.sukashawarma.superapp.feature.manager.domain.KeputusanSidak
import com.sukashawarma.superapp.feature.manager.domain.labelSection
import com.sukashawarma.superapp.feature.manager.domain.waktuJakarta
import com.sukashawarma.superapp.feature.manager.ui.BarProgres
import com.sukashawarma.superapp.feature.manager.ui.KartuPanel
import com.sukashawarma.superapp.feature.manager.ui.PanelKosong

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
@Composable
fun SidakScreen(
    onExit: () -> Unit,
    viewModel: SidakViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()

    // Menunggu `plan/inventaris-realtime-publication.sql` dijalankan di Supabase;
    // sebelum itu langganan ini hidup tapi tidak pernah menerima event.
    RealtimeRefresh(
        RealtimeTables.INVENTARIS_SUBMISSIONS,
        RealtimeTables.INVENTARIS_MASTER_ITEMS,
    ) { viewModel.muatUlang(silent = true) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.kabar, state.galat) {
        val pesan = state.kabar ?: state.galat
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
                judul = "Sidak Inventaris",
                onKembali = onExit,
                aksi = { TombolBundarIos(IkonIos.Refresh, "Muat ulang", viewModel::muatUlang) },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(UkuranIos.TepiLayar).denganRuangNav(),
            verticalArrangement = Arrangement.spacedBy(UkuranIos.JarakKartu),
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
        Text("Sidak Inventaris", style = TipeIos.Utama)
        Spacer(Modifier.height(2.dp))
        Text(
            "Cocokkan kondisi aset di outlet dengan laporan yang masuk.",
            style = TipeIos.Catatan,
        )

        Spacer(Modifier.height(14.dp))
        KolomCariIos(
            nilai = state.pencarian,
            onUbah = viewModel::ubahPencarian,
            modifier = Modifier.fillMaxWidth(),
            placeholder = "Cari outlet...",
        )

        Spacer(Modifier.height(10.dp))
        Box {
            SukaFilterDropdown(
                label = "",
                value = state.namaOutlet(state.outletTerpilih),
                expanded = menuOutlet,
                onClick = { menuOutlet = true },
                leadingIcon = IkonIos.Storefront,
            )
            SukaDropdownMenu(expanded = menuOutlet, onDismissRequest = { menuOutlet = false }) {
                SukaDropdownHeader(title = "PILIH OUTLET SIDAK", onClose = { menuOutlet = false })
                state.outletTerlihat.forEach { outlet ->
                    val keadaan = state.keadaan(outlet.id)
                    SukaDropdownMenuItem(
                        text = outlet.nama,
                        subtitle = keadaan.label,
                        selected = (state.outletTerpilih == outlet.id),
                        onClick = { viewModel.pilihOutlet(outlet.id); menuOutlet = false },
                    )
                }
            }
        }

        state.outletTerpilih?.let { id ->
            Spacer(Modifier.height(12.dp))
            LencanaKeadaan(state.keadaan(id))
        }
    }
}

@Composable
private fun LencanaKeadaan(keadaan: KeadaanSidak) {
    val nada = when (keadaan) {
        KeadaanSidak.SUDAH_DISIDAK -> NadaIos.SUKSES
        KeadaanSidak.MENUNGGU_SIDAK -> NadaIos.PERINGATAN
        KeadaanSidak.BELUM_ADA_LAPORAN -> NadaIos.NETRAL
    }
    LencanaIos(keadaan.label, nada)
}

/**
 * Pemberitahuan peran tanpa wewenang simpan. Bentuknya meniru [PanelGalatIos]
 * tapi bernada peringatan: ini bukan galat, hanya batas peran.
 */
@Composable
private fun CatatanTanpaWewenang() {
    Row(
        Modifier
            .fillMaxWidth()
            .permukaanIos()
            .padding(horizontal = UkuranIos.PaddingKartu, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(30.dp).clip(CircleShape).background(NadaIos.PERINGATAN.warna.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(IkonIos.Lock, null, tint = NadaIos.PERINGATAN.warna, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(
            "Laporan dan hasil sidak bisa ditinjau di sini, tetapi peran Anda " +
                "tidak berwenang menyimpan hasil sidak.",
            Modifier.weight(1f),
            style = TipeIos.Catatan.copy(color = NadaIos.PERINGATAN.teks, lineHeight = 18.sp),
        )
    }
}

@Composable
private fun PanelGalat(pesan: String) {
    PanelGalatIos(pesan)
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
        LabelSeksiIos("Catatan sidak")
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = state.catatan,
            onValueChange = viewModel::ubahCatatan,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text("Temuan umum, lokasi aset, atau tindak lanjut (opsional)", fontSize = 13.sp)
            },
            minLines = 3,
            shape = UkuranIos.SudutKontrol,
            colors = warnaKolomIos(),
        )

        Spacer(Modifier.height(12.dp))
        // Alasan belum bisa disimpan ditulis apa adanya. Tombol mati tanpa
        // keterangan memaksa pengguna menebak item mana yang terlewat.
        val terhalang = state.halangan != null
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                if (terhalang) IkonIos.WarningAmber else IkonIos.CheckCircle,
                null,
                tint = if (terhalang) NadaIos.PERINGATAN.warna else NadaIos.SUKSES.warna,
                modifier = Modifier.padding(top = 1.dp).size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                state.halangan ?: "Semua item sudah diberi hasil.",
                style = TipeIos.Catatan.copy(
                    color = if (terhalang) NadaIos.PERINGATAN.teks else NadaIos.SUKSES.teks,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 18.sp,
                ),
            )
        }

        Spacer(Modifier.height(14.dp))
        TombolUtamaIos(
            teks = if (state.menyimpan) "Menyimpan..." else "Simpan hasil sidak",
            onKlik = { viewModel.simpan() },
            aktif = state.siapDisimpan,
            memuat = state.menyimpan,
            ikon = IkonIos.Check,
        )
    }
}

@Composable
private fun PanelRingkasan(state: SidakUiState) {
    val laporan = state.laporan ?: return
    val ringkasan = state.ringkasan ?: return
    KartuPanel {
        Text(state.namaOutlet(state.outletTerpilih), style = TipeIos.Utama)
        Spacer(Modifier.height(2.dp))
        Text("Laporan AM · ${waktuJakarta(laporan.diperbaruiPada)}", style = TipeIos.Catatan)

        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                "${ringkasan.diperiksa}/${ringkasan.total} diperiksa",
                Modifier.weight(1f),
                color = WarnaIos.Label,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text("${ringkasan.persen}%", style = TipeIos.Angka.copy(color = WarnaIos.Aksen))
        }
        Spacer(Modifier.height(8.dp))
        BarProgres(rasio = ringkasan.persen / 100f, tinggi = 6)

        Spacer(Modifier.height(12.dp))
        if (ringkasan.bermasalah > 0) {
            LencanaIos(
                "${ringkasan.bermasalah} item ditandai rusak",
                NadaIos.BAHAYA,
                ikon = IkonIos.WarningAmber,
            )
        } else {
            Text("Belum ada item yang ditandai rusak", style = TipeIos.Catatan)
        }
        laporan.catatan?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(10.dp))
            Text(
                "Catatan AM: $it",
                Modifier
                    .fillMaxWidth()
                    .clip(UkuranIos.SudutBlok)
                    .background(WarnaIos.Latar)
                    .padding(12.dp),
                style = TipeIos.Catatan.copy(color = WarnaIos.Label, lineHeight = 18.sp),
            )
        }
    }
}

@Composable
private fun BarisSection(state: SidakUiState, viewModel: SidakViewModel) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        KapsulPilihanIos("Semua area", state.section == null, { viewModel.pilihSection(null) })
        state.sectionTersedia.forEach { section ->
            KapsulPilihanIos(labelSection(section), state.section == section, { viewModel.pilihSection(section) })
        }
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
    val gaya = when (keputusan) {
        KeputusanSidak.BAIK -> GAYA_BAIK
        KeputusanSidak.RUSAK -> GAYA_RUSAK
        null -> GAYA_BELUM
    }

    KartuPanel {
        Row(verticalAlignment = Alignment.Top) {
            IkonBulatIos(gaya.ikon, gaya.nada.warna, ukuran = 34.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.nama, style = TipeIos.Utama)
                Spacer(Modifier.height(2.dp))
                Text("${labelSection(item.section)} · ${item.subsection}", style = TipeIos.Catatan)
            }
            Spacer(Modifier.width(8.dp))
            LencanaIos(gaya.label, gaya.nada)
        }

        Spacer(Modifier.height(12.dp))
        // Blok abu dua kolom meniru BlokAngkaIos, tapi dengan teks biasa: nilainya
        // bisa berupa kalimat ("Ada / tidak ada") yang terpotong di gaya angka besar.
        Row(
            Modifier
                .fillMaxWidth()
                .clip(UkuranIos.SudutBlok)
                .background(WarnaIos.Latar)
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            KolomNilaiSidak("Target", item.targetTeks, Modifier.weight(1f))
            Box(Modifier.width(0.5.dp).height(30.dp).background(WarnaIos.Pemisah))
            KolomNilaiSidak("Terinput", item.hasilTeks, Modifier.weight(1f))
        }

        item.catatan?.let {
            Spacer(Modifier.height(8.dp))
            Text("Catatan AM: $it", style = TipeIos.Catatan.copy(lineHeight = 18.sp))
        }

        if (bolehMemutuskan) {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TombolKeputusan(
                    label = "Baik",
                    ikon = IkonIos.Check,
                    aktif = keputusan == KeputusanSidak.BAIK,
                    nada = NadaIos.SUKSES,
                    modifier = Modifier.weight(1f),
                ) { onPutuskan(KeputusanSidak.BAIK) }
                TombolKeputusan(
                    label = "Rusak",
                    ikon = IkonIos.Close,
                    aktif = keputusan == KeputusanSidak.RUSAK,
                    nada = NadaIos.BAHAYA,
                    modifier = Modifier.weight(1f),
                ) { onPutuskan(KeputusanSidak.RUSAK) }
            }
        }

        if (item.fotoPath.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            TombolKapsulIos("Lihat foto bukti", onBukaFoto, ikon = IkonIos.PhotoCamera)
        }
    }
}

@Composable
private fun KolomNilaiSidak(label: String, nilai: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(horizontal = 12.dp)) {
        Text(label, style = TipeIos.Kecil.copy(fontWeight = FontWeight.Medium), maxLines = 1)
        Spacer(Modifier.height(2.dp))
        Text(
            nilai,
            color = WarnaIos.Label,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Tombol Baik/Rusak — cermin `DecisionButton` web, termasuk perilakunya: menekan
 * tombol yang sudah aktif membatalkan pilihannya.
 *
 * Bukan TombolUtamaIos/TombolKeduaIos: keduanya setinggi 50dp dan teks tombol
 * kedua memakai warna isian murni, yang terlalu pucat untuk hijau sistem.
 */
@Composable
private fun TombolKeputusan(
    label: String,
    ikon: ImageVector,
    aktif: Boolean,
    nada: NadaIos,
    modifier: Modifier = Modifier,
    onKlik: () -> Unit,
) {
    val warnaIsi = if (aktif) Color.White else nada.teks
    Row(
        modifier
            .heightIn(min = 44.dp)
            .clip(UkuranIos.SudutBlok)
            .background(if (aktif) nada.warna else nada.warna.copy(alpha = 0.12f))
            .tekanIos(onKlik),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(ikon, null, tint = warnaIsi, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = warnaIsi, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Nada, label, dan ikon satu keadaan keputusan — dibuat sekali, bukan per komposisi item. */
private class GayaKeputusan(val nada: NadaIos, val label: String, val ikon: ImageVector)

private val GAYA_BAIK by lazy { GayaKeputusan(NadaIos.SUKSES, "Baik", IkonIos.Check) }
private val GAYA_RUSAK by lazy { GayaKeputusan(NadaIos.BAHAYA, "Rusak", IkonIos.WarningAmber) }
private val GAYA_BELUM by lazy { GayaKeputusan(NadaIos.NETRAL, "Belum dicek", IkonIos.FactCheck) }

@Composable
private fun PanelCatatanSidak(catatan: String) {
    KartuPanel {
        LabelSeksiIos("Catatan sidak")
        Spacer(Modifier.height(6.dp))
        Text(catatan, style = TipeIos.Keterangan.copy(fontSize = 15.sp, lineHeight = 21.sp))
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
        Surface(shape = UkuranIos.SudutKartu, color = WarnaIos.Kartu) {
            Column {
                Row(
                    Modifier.padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            judul,
                            style = TipeIos.Utama.copy(fontSize = 15.sp),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(outlet, style = TipeIos.Kecil)
                    }
                    Spacer(Modifier.width(8.dp))
                    TombolBundarIos(IkonIos.Close, "Tutup", onTutup, warnaIkon = WarnaIos.LabelKedua)
                }
                Box(
                    Modifier.fillMaxWidth().height(320.dp).background(Color(0xFF111827)),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        memuat -> CircularProgressIndicator(
                            Modifier.size(28.dp),
                            strokeWidth = 3.dp,
                            color = WarnaIos.Aksen,
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
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
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
