package com.sukashawarma.superapp.feature.manager.ui.waste

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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
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
import com.sukashawarma.superapp.core.ui.ios.AngkaIos
import com.sukashawarma.superapp.core.ui.ios.BilahJudulIos
import com.sukashawarma.superapp.core.ui.ios.BlokAngkaIos
import com.sukashawarma.superapp.core.ui.ios.IkonBulatIos
import com.sukashawarma.superapp.core.ui.ios.KeadaanIos
import com.sukashawarma.superapp.core.ui.ios.LabelSeksiIos
import com.sukashawarma.superapp.core.ui.ios.LencanaIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.PemisahIos
import com.sukashawarma.superapp.core.ui.ios.SegmenIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.TombolBundarIos
import com.sukashawarma.superapp.core.ui.ios.TombolKeduaIos
import com.sukashawarma.superapp.core.ui.ios.TombolUtamaIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WadahSegmenIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.ios.tekanIos
import com.sukashawarma.superapp.core.ui.ios.warnaKolomIos
import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.core.ui.kaca.denganRuangNav
import com.sukashawarma.superapp.core.ui.kaca.navigationBarsPaddingKaca
import com.sukashawarma.superapp.domain.model.Role
import com.sukashawarma.superapp.feature.manager.domain.BahanTerbuang
import com.sukashawarma.superapp.feature.manager.domain.LaporanWaste
import com.sukashawarma.superapp.feature.manager.domain.MIN_ALASAN_PENOLAKAN
import com.sukashawarma.superapp.feature.manager.domain.PresetPeriode
import com.sukashawarma.superapp.feature.manager.domain.RingkasanWaste
import com.sukashawarma.superapp.feature.manager.domain.StatusWaste
import com.sukashawarma.superapp.feature.manager.domain.cacah
import com.sukashawarma.superapp.feature.manager.domain.rupiah
import com.sukashawarma.superapp.feature.manager.domain.waktuJakarta
import com.sukashawarma.superapp.feature.manager.domain.waktuJakartaRingkas
import com.sukashawarma.superapp.feature.manager.ui.ChipJingga
import com.sukashawarma.superapp.feature.manager.ui.KartuPanel
import com.sukashawarma.superapp.feature.manager.ui.MerahLatar
import com.sukashawarma.superapp.feature.manager.ui.MerahTeks
import com.sukashawarma.superapp.feature.manager.ui.PanelKosong
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private val FILTER_PERIODE = listOf(
    PresetPeriode.HARI_INI to "Hari Ini",
    PresetPeriode.MINGGU to "7 Hari",
    PresetPeriode.BULAN to "30 Hari",
)

/**
 * Pengawasan Waste Stok — cermin `app/waste/` web.
 *
 * Dua tab seperti web: antrean yang menunggu keputusan, dan riwayat beserta
 * angka ringkasnya. Menyetujui memotong stok lewat trigger database yang sama
 * dengan yang dipakai web, jadi tidak ada logika pemotongan di sisi aplikasi.
 */
@Composable
fun WasteScreen(
    onExit: () -> Unit,
    viewModel: WasteViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    RealtimeRefresh(RealtimeTables.WASTE_REPORTS) { viewModel.muatUlang(silent = true) }

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
                judul = "Waste Stok",
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
            item { PanelKepala(state, viewModel) }
            item { BarisTab(state, viewModel) }

            when (state.tab) {
                TabWaste.MENUNGGU -> isiTabMenunggu(state, viewModel)
                TabWaste.RIWAYAT -> isiTabRiwayat(state, viewModel)
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

/* ----------------------------------------------------------------------- */
/* Kepala: identitas wewenang dan penyaring outlet                          */
/* ----------------------------------------------------------------------- */

@Composable
private fun PanelKepala(state: WasteUiState, viewModel: WasteViewModel) {
    var menuOutlet by remember { mutableStateOf(false) }
    val seluruhOutlet = state.role == Role.REGIONAL_MANAGER

    KartuPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IkonBulatIos(IkonIos.Delete, WarnaIos.Aksen, ukuran = 38.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Pengawasan Waste Stok", style = TipeIos.Utama)
                Spacer(Modifier.height(2.dp))
                Text("Wewenang: ${labelWewenang(state.role)}", style = TipeIos.Catatan)
            }
        }
        Spacer(Modifier.height(12.dp))
        LencanaIos(
            if (seluruhOutlet) "Akses seluruh outlet" else "${state.daftarOutlet.size} outlet binaan",
            if (seluruhOutlet) NadaIos.SUKSES else NadaIos.PERINGATAN,
        )

        Spacer(Modifier.height(14.dp))
        Box {
            SukaFilterDropdown(
                label = "",
                value = state.namaOutletTerpilih ?: if (seluruhOutlet) {
                    "Semua outlet aktif"
                } else {
                    "Semua outlet binaan saya"
                },
                expanded = menuOutlet,
                onClick = { menuOutlet = true },
                leadingIcon = IkonIos.Storefront,
            )
            SukaDropdownMenu(expanded = menuOutlet, onDismissRequest = { menuOutlet = false }) {
                SukaDropdownHeader(title = "PILIH OUTLET", onClose = { menuOutlet = false })
                SukaDropdownMenuItem(
                    text = if (seluruhOutlet) "Semua outlet aktif" else "Semua outlet binaan saya",
                    selected = (state.outletTerpilih == null),
                    onClick = { viewModel.pilihOutlet(null); menuOutlet = false },
                )
                state.daftarOutlet.forEach { outlet ->
                    SukaDropdownMenuItem(
                        text = outlet.nama,
                        selected = (state.outletTerpilih == outlet.id),
                        onClick = { viewModel.pilihOutlet(outlet.id); menuOutlet = false },
                    )
                }
            }
        }
    }
}

private fun labelWewenang(role: Role?): String = when (role) {
    Role.AREA_MANAGER -> "Area Manager (AM)"
    Role.REGIONAL_MANAGER -> "Regional Manager (RM)"
    else -> role?.value?.replace('_', ' ')?.uppercase() ?: "-"
}

@Composable
private fun BarisTab(state: WasteUiState, viewModel: WasteViewModel) {
    WadahSegmenIos {
        SegmenIos(
            label = "Menunggu",
            aktif = state.tab == TabWaste.MENUNGGU,
            onKlik = { viewModel.pilihTab(TabWaste.MENUNGGU) },
            modifier = Modifier.weight(1f),
            ikon = IkonIos.Schedule,
            // Merah, bukan aksen: hitungan ini adalah pekerjaan yang tertunda.
            lencana = state.menunggu.size.takeIf { it > 0 }?.toString(),
            warnaLencana = WarnaIos.Merah,
        )
        SegmenIos(
            label = "Riwayat & Analitik",
            aktif = state.tab == TabWaste.RIWAYAT,
            onKlik = { viewModel.pilihTab(TabWaste.RIWAYAT) },
            modifier = Modifier.weight(1f),
            ikon = IkonIos.BarChart,
        )
    }
}

/* ----------------------------------------------------------------------- */
/* Tab 1: antrean menunggu persetujuan                                      */
/* ----------------------------------------------------------------------- */

private fun LazyListScope.isiTabMenunggu(
    state: WasteUiState,
    viewModel: WasteViewModel,
) {
    if (state.menunggu.isEmpty()) {
        item { PanelAntreanBersih(state.memuat) }
        return
    }

    item {
        Text(
            "${state.menunggu.size} pengajuan menunggu tindakan Anda",
            Modifier.padding(horizontal = 4.dp),
            style = TipeIos.Catatan,
        )
    }
    items(state.menunggu, key = { it.id }) { laporan ->
        KartuAntrean(
            laporan = laporan,
            sedangDiproses = laporan.id in state.sedangDiproses,
            onSetujui = { viewModel.setujui(laporan) },
            onTolak = { alasan -> viewModel.tolak(laporan, alasan) },
        )
    }
}

@Composable
private fun PanelAntreanBersih(memuat: Boolean) {
    KartuPanel {
        if (memuat) {
            PanelKosong("Memuat antrean pengajuan waste...")
        } else {
            KeadaanIos(
                ikon = IkonIos.CheckCircle,
                judul = "Semua pengajuan bersih!",
                pesan = "Tidak ada pengajuan waste yang menunggu persetujuan saat ini.",
                nada = NadaIos.SUKSES,
            )
        }
    }
}

@Composable
private fun KartuAntrean(
    laporan: LaporanWaste,
    sedangDiproses: Boolean,
    onSetujui: () -> Unit,
    onTolak: (String) -> Unit,
) {
    var fotoTerbuka by remember { mutableStateOf(false) }
    var dialogTolak by remember { mutableStateOf(false) }

    KartuPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ChipJingga(laporan.outletNama)
            Spacer(Modifier.weight(1f))
            Text(waktuJakartaRingkas(laporan.dibuatPada), style = TipeIos.Kecil)
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(laporan.bahanNama, style = TipeIos.Utama)
                Spacer(Modifier.height(2.dp))
                Text("Kuantitas: ${laporan.qtyTeks} ${laporan.satuan}", style = TipeIos.Catatan)
            }
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text("Estimasi kerugian", style = TipeIos.Kecil)
                Spacer(Modifier.height(2.dp))
                Text(rupiah(laporan.nilai), style = TipeIos.Angka.copy(color = NadaIos.BAHAYA.teks), maxLines = 1)
            }
        }

        Spacer(Modifier.height(12.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(UkuranIos.SudutBlok)
                .background(WarnaIos.Latar)
                .padding(12.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    IkonIos.WarningAmber,
                    null,
                    tint = WarnaIos.Oranye,
                    modifier = Modifier.padding(top = 1.dp).size(15.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Alasan: ${laporan.alasan}",
                    style = TipeIos.Catatan.copy(color = WarnaIos.Label, lineHeight = 18.sp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(IkonIos.Person, null, tint = WarnaIos.LabelKedua, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(8.dp))
                Text("Pelapor: ${laporan.pelaporNama}", style = TipeIos.Catatan)
            }
        }

        Spacer(Modifier.height(10.dp))
        if (laporan.fotoUrl != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .tekanIos({ fotoTerbuka = true })
                    .clip(UkuranIos.SudutBlok)
                    .background(WarnaIos.Latar)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AsyncImage(
                    model = laporan.fotoUrl,
                    contentDescription = "Bukti fisik ${laporan.bahanNama}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(WarnaIos.Isian),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(IkonIos.Image, null, tint = WarnaIos.Aksen, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Lihat foto bukti fisik",
                            color = WarnaIos.Label,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Text("Ketuk untuk memperbesar", style = TipeIos.Kecil)
                }
                Icon(IkonIos.ChevronRight, null, tint = WarnaIos.LabelKetiga, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
            }
        } else {
            Text("* Tidak ada lampiran foto fisik", style = TipeIos.Kecil)
        }

        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TombolKeduaIos(
                teks = "Tolak",
                onKlik = { dialogTolak = true },
                modifier = Modifier.weight(1f),
                aktif = !sedangDiproses,
                ikon = IkonIos.Close,
                warna = WarnaIos.Merah,
            )
            TombolUtamaIos(
                teks = "Setujui",
                onKlik = onSetujui,
                modifier = Modifier.weight(1f),
                memuat = sedangDiproses,
                ikon = IkonIos.Check,
                warna = WarnaIos.Hijau,
            )
        }
    }

    if (fotoTerbuka && laporan.fotoUrl != null) {
        DialogFoto(laporan.fotoUrl, "${laporan.bahanNama} — ${laporan.outletNama}") { fotoTerbuka = false }
    }

    if (dialogTolak) {
        DialogTolak(
            laporan = laporan,
            onBatal = { dialogTolak = false },
            onKirim = { alasan -> dialogTolak = false; onTolak(alasan) },
        )
    }
}

@Composable
private fun DialogFoto(url: String, judul: String, onTutup: () -> Unit) {
    Dialog(onDismissRequest = onTutup) {
        Surface(shape = UkuranIos.SudutKartu, color = WarnaIos.Kartu) {
            Column {
                Row(
                    Modifier.padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        judul,
                        Modifier.weight(1f),
                        style = TipeIos.Utama.copy(fontSize = 15.sp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.width(8.dp))
                    TombolBundarIos(IkonIos.Close, "Tutup", onTutup, warnaIkon = WarnaIos.LabelKedua)
                }
                AsyncImage(
                    model = url,
                    contentDescription = "Bukti fisik waste",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().height(340.dp).background(Color(0xFF111827)),
                )
            }
        }
    }
}

@Composable
private fun DialogTolak(
    laporan: LaporanWaste,
    onBatal: () -> Unit,
    onKirim: (String) -> Unit,
) {
    var alasan by remember { mutableStateOf("") }
    val cukup = alasan.trim().length >= MIN_ALASAN_PENOLAKAN

    Dialog(onDismissRequest = onBatal) {
        Surface(shape = UkuranIos.SudutKartu, color = WarnaIos.Kartu) {
            Column(Modifier.padding(20.dp)) {
                Text("Tolak Pengajuan Waste", style = TipeIos.Utama)
                Spacer(Modifier.height(12.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(UkuranIos.SudutBlok)
                        .background(MerahLatar)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    val gaya = TipeIos.Catatan.copy(color = WarnaIos.Label, lineHeight = 18.sp)
                    Text("Bahan: ${laporan.bahanNama} (${laporan.qtyTeks} ${laporan.satuan})", style = gaya)
                    Text("Outlet: ${laporan.outletNama}", style = gaya)
                    Text("Alasan kru: ${laporan.alasan}", style = gaya)
                }
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = alasan,
                    onValueChange = { alasan = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Alasan penolakan", fontSize = 13.sp) },
                    placeholder = {
                        Text("Cth: foto tidak jelas, sisa porsi masih bisa diolah", fontSize = 13.sp)
                    },
                    minLines = 3,
                    shape = UkuranIos.SudutKontrol,
                    colors = warnaKolomIos(),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Minimal $MIN_ALASAN_PENOLAKAN karakter.",
                    Modifier.padding(start = 4.dp),
                    style = TipeIos.Kecil,
                )
                Spacer(Modifier.height(16.dp))
                // Ditumpuk seperti lembar aksi iOS: dua tombol 50dp berdampingan tidak
                // muat di lebar dialog tanpa memotong "Konfirmasi Tolak".
                TombolUtamaIos(
                    teks = "Konfirmasi Tolak",
                    onKlik = { onKirim(alasan.trim()) },
                    aktif = cukup,
                    warna = WarnaIos.Merah,
                )
                Spacer(Modifier.height(8.dp))
                TombolKeduaIos(teks = "Batal", onKlik = onBatal, warna = WarnaIos.AbuGelap)
            }
        }
    }
}

/* ----------------------------------------------------------------------- */
/* Tab 2: riwayat dan analitik                                              */
/* ----------------------------------------------------------------------- */

private fun LazyListScope.isiTabRiwayat(
    state: WasteUiState,
    viewModel: WasteViewModel,
) {
    item { KartuRingkasanWaste(state.ringkasan) }
    item { PanelPenyaringRiwayat(state, viewModel) }

    if (state.riwayat.baris.isEmpty()) {
        item { KartuPanel { PanelKosong("Tidak ada riwayat waste pada periode ini.") } }
        return
    }

    items(state.riwayat.baris, key = { it.id }) { laporan -> KartuRiwayat(laporan) }
    item { BarisHalaman(state, viewModel) }
}

@Composable
private fun KartuRingkasanWaste(ringkasan: RingkasanWaste) {
    KartuPanel {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text("Total kerugian waste", style = TipeIos.Catatan.copy(fontWeight = FontWeight.SemiBold))
                Spacer(Modifier.height(4.dp))
                Text(
                    rupiah(ringkasan.totalNilai),
                    style = TipeIos.AngkaBesar.copy(color = NadaIos.BAHAYA.teks),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text("Dari laporan yang telah disetujui", style = TipeIos.Kecil)
            }
            Spacer(Modifier.width(10.dp))
            IkonBulatIos(Icons.Default.TrendingDown, WarnaIos.Merah, ukuran = 34.dp)
        }

        Spacer(Modifier.height(14.dp))
        BlokAngkaIos(
            buildList {
                add(AngkaIos("Insiden dilaporkan", cacah(ringkasan.totalInsiden), "kejadian"))
                // Kolom menunggu hanya muncul bila ada, sama seperti lencana di web.
                if (ringkasan.jumlahMenunggu > 0) {
                    add(AngkaIos("Menunggu", cacah(ringkasan.jumlahMenunggu), "pengajuan"))
                }
            },
        )

        Spacer(Modifier.height(16.dp))
        LabelSeksiIos("Bahan paling banyak terbuang")
        Spacer(Modifier.height(6.dp))
        if (ringkasan.bahanTeratas.isEmpty()) {
            Text("Belum ada data waste pada periode ini.", style = TipeIos.Catatan)
        } else {
            ringkasan.bahanTeratas.forEachIndexed { index, bahan ->
                if (index > 0) PemisahIos(inset = 24.dp)
                BarisBahanTerbuang(index + 1, bahan)
            }
        }
    }
}

@Composable
private fun BarisBahanTerbuang(nomor: Int, bahan: BahanTerbuang) {
    Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "$nomor.",
            Modifier.width(24.dp),
            style = TipeIos.Catatan.copy(fontWeight = FontWeight.SemiBold),
        )
        Text(
            bahan.nama,
            Modifier.weight(1f),
            color = WarnaIos.Label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(8.dp))
        Text("${bahan.qtyTeks} ${bahan.satuan}", style = TipeIos.Catatan, maxLines = 1)
        Spacer(Modifier.width(8.dp))
        Text(
            rupiah(bahan.nilai),
            color = NadaIos.BAHAYA.teks,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun PanelPenyaringRiwayat(state: WasteUiState, viewModel: WasteViewModel) {
    var dialogTanggal by remember { mutableStateOf(false) }
    var menuStatus by remember { mutableStateOf(false) }

    KartuPanel {
        WadahSegmenIos(gulir = true) {
            FILTER_PERIODE.forEach { (preset, label) ->
                SegmenIos(
                    label = label,
                    aktif = !state.memakaiKustom && state.preset == preset,
                    onKlik = { viewModel.pilihPreset(preset) },
                )
            }
            SegmenIos(
                label = state.kustom?.let { "${it.dari} - ${it.sampai}" } ?: "Kustom",
                aktif = state.memakaiKustom,
                onKlik = { dialogTanggal = true },
                ikon = IkonIos.CalendarMonth,
            )
        }

        Spacer(Modifier.height(12.dp))
        Box {
            SukaFilterDropdown(
                label = "",
                value = "Status: ${labelStatus(state.filterStatus)}",
                expanded = menuStatus,
                onClick = { menuStatus = true },
            )
            SukaDropdownMenu(expanded = menuStatus, onDismissRequest = { menuStatus = false }) {
                SukaDropdownHeader(title = "STATUS WASTE", onClose = { menuStatus = false })
                listOf(null, StatusWaste.DISETUJUI, StatusWaste.DITOLAK).forEach { status ->
                    SukaDropdownMenuItem(
                        text = labelStatus(status),
                        selected = (state.filterStatus == status),
                        onClick = { viewModel.pilihStatus(status); menuStatus = false },
                    )
                }
            }
        }
    }

    if (dialogTanggal) {
        DialogRentang(
            onTutup = { dialogTanggal = false },
            onPilih = { dari, sampai ->
                viewModel.pilihRentangKustom(dari, sampai)
                dialogTanggal = false
            },
        )
    }
}

private fun labelStatus(status: StatusWaste?): String = when (status) {
    StatusWaste.DISETUJUI -> "Hanya disetujui"
    StatusWaste.DITOLAK -> "Hanya ditolak"
    else -> "Disetujui & ditolak"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogRentang(onTutup: () -> Unit, onPilih: (LocalDate, LocalDate) -> Unit) {
    val picker = rememberDateRangePickerState()
    val warna = DatePickerDefaults.colors(
        containerColor = WarnaIos.Kartu,
        selectedDayContainerColor = WarnaIos.Aksen,
        todayDateBorderColor = WarnaIos.Aksen,
        todayContentColor = WarnaIos.Aksen,
        dayInSelectionRangeContainerColor = WarnaIos.Aksen.copy(alpha = 0.14f),
    )
    val bisaTerapkan = picker.selectedStartDateMillis != null
    DatePickerDialog(
        onDismissRequest = onTutup,
        confirmButton = {
            TextButton(
                onClick = {
                    val dari = picker.selectedStartDateMillis?.let(::tanggalDari)
                    val sampai = picker.selectedEndDateMillis?.let(::tanggalDari)
                    if (dari != null) onPilih(dari, sampai ?: dari)
                },
                enabled = bisaTerapkan,
            ) {
                Text(
                    "Terapkan",
                    color = if (bisaTerapkan) WarnaIos.Aksen else WarnaIos.LabelKetiga,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onTutup) { Text("Batal", color = WarnaIos.Aksen) }
        },
        shape = UkuranIos.SudutKartu,
        colors = warna,
    ) {
        DateRangePicker(
            state = picker,
            title = {
                Text(
                    "Pilih Rentang Tanggal",
                    Modifier.padding(start = 24.dp, top = 16.dp),
                    style = TipeIos.Utama,
                )
            },
            colors = warna,
        )
    }
}

private fun tanggalDari(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

@Composable
private fun KartuRiwayat(laporan: LaporanWaste) {
    var fotoTerbuka by remember { mutableStateOf(false) }
    val disetujui = laporan.status == StatusWaste.DISETUJUI

    KartuPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ChipJingga(laporan.outletNama)
            Spacer(Modifier.weight(1f))
            LencanaIos(
                if (disetujui) "Disetujui" else "Ditolak",
                if (disetujui) NadaIos.SUKSES else NadaIos.BAHAYA,
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(laporan.bahanNama, style = TipeIos.Utama)
                Spacer(Modifier.height(2.dp))
                Text(
                    "${laporan.qtyTeks} ${laporan.satuan} • ${waktuJakarta(laporan.dibuatPada)}",
                    style = TipeIos.Catatan,
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                rupiah(laporan.nilai),
                color = if (disetujui) NadaIos.BAHAYA.teks else WarnaIos.LabelKedua,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }

        Spacer(Modifier.height(10.dp))
        Text(
            "Alasan: ${laporan.alasan}",
            style = TipeIos.Catatan.copy(color = WarnaIos.Label, lineHeight = 18.sp),
        )
        if (laporan.alasanPenolakan != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Ditolak karena: ${laporan.alasanPenolakan}",
                Modifier
                    .fillMaxWidth()
                    .clip(UkuranIos.SudutBlok)
                    .background(MerahLatar)
                    .padding(12.dp),
                style = TipeIos.Catatan.copy(color = MerahTeks, lineHeight = 18.sp),
            )
        }

        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Pelapor: ${laporan.pelaporNama}" +
                    (laporan.penyetujuNama?.let { " • Diputuskan: $it" } ?: ""),
                Modifier.weight(1f),
                style = TipeIos.Kecil,
            )
            if (laporan.fotoUrl != null) {
                Spacer(Modifier.width(8.dp))
                Row(
                    Modifier
                        .clip(UkuranIos.SudutKapsul)
                        .background(WarnaIos.Aksen.copy(alpha = 0.12f))
                        .tekanIos({ fotoTerbuka = true })
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(IkonIos.Image, null, tint = NadaIos.AKSEN.teks, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Foto", color = NadaIos.AKSEN.teks, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (fotoTerbuka && laporan.fotoUrl != null) {
        DialogFoto(laporan.fotoUrl, "${laporan.bahanNama} — ${laporan.outletNama}") { fotoTerbuka = false }
    }
}

@Composable
private fun BarisHalaman(state: WasteUiState, viewModel: WasteViewModel) {
    val riwayat = state.riwayat
    KartuPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Halaman ${riwayat.halaman} dari ${riwayat.totalHalaman}",
                    color = WarnaIos.Label,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text("${cacah(riwayat.totalBaris)} laporan pada periode ini", style = TipeIos.Kecil)
            }
            TombolBundarIos(
                IkonIos.ArrowBack,
                "Halaman sebelumnya",
                onKlik = { viewModel.pilihHalaman(riwayat.halaman - 1) },
                aktif = riwayat.halaman > 1,
            )
            Spacer(Modifier.width(8.dp))
            TombolBundarIos(
                IkonIos.ChevronRight,
                "Halaman berikutnya",
                onKlik = { viewModel.pilihHalaman(riwayat.halaman + 1) },
                aktif = riwayat.halaman < riwayat.totalHalaman,
            )
        }
    }
}

