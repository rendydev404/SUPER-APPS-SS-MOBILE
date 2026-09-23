package com.sukashawarma.superapp.feature.manager.ui.persetujuan

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import com.sukashawarma.superapp.core.ui.SukaDropdownHeader
import com.sukashawarma.superapp.core.ui.SukaDropdownMenu
import com.sukashawarma.superapp.core.ui.SukaDropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.RealtimeTables
import com.sukashawarma.superapp.core.ui.ios.BilahJudulIos
import com.sukashawarma.superapp.core.ui.ios.IkonBulatIos
import com.sukashawarma.superapp.core.ui.ios.KeadaanIos
import com.sukashawarma.superapp.core.ui.ios.KolomCariIos
import com.sukashawarma.superapp.core.ui.ios.LabelSeksiIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.PemisahIos
import com.sukashawarma.superapp.core.ui.ios.SegmenIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.TombolBundarIos
import com.sukashawarma.superapp.core.ui.ios.TombolKapsulIos
import com.sukashawarma.superapp.core.ui.ios.TombolKeduaIos
import com.sukashawarma.superapp.core.ui.ios.TombolUtamaIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WadahSegmenIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.ios.warnaKolomIos
import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.core.ui.kaca.denganRuangNav
import com.sukashawarma.superapp.core.ui.kaca.navigationBarsPaddingKaca
import com.sukashawarma.superapp.feature.manager.domain.PengajuanBypass
import com.sukashawarma.superapp.feature.manager.domain.PengajuanVoid
import com.sukashawarma.superapp.feature.manager.domain.PesananSelesaiItem
import com.sukashawarma.superapp.feature.manager.domain.PresetPeriode
import com.sukashawarma.superapp.feature.manager.domain.TabPersetujuan
import com.sukashawarma.superapp.feature.manager.domain.cacah
import com.sukashawarma.superapp.feature.manager.domain.rupiah
import com.sukashawarma.superapp.feature.manager.domain.waktuRelatif
import com.sukashawarma.superapp.feature.manager.ui.ChipJingga
import com.sukashawarma.superapp.feature.manager.ui.KartuPanel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private val FILTER_PERIODE = listOf(
    PresetPeriode.HARI_INI to "Hari Ini",
    PresetPeriode.KEMARIN to "Kemarin",
    PresetPeriode.MINGGU to "7 Hari",
    PresetPeriode.BULAN to "30 Hari",
)

// Baris rincian pesanan: satu ukuran untuk qty, nama, dan subtotal supaya kolomnya
// sejajar; hanya warnanya yang membedakan nama dari angka pendamping.
private val TeksRincian = TextStyle(fontSize = 14.sp, color = WarnaIos.Label)
private val TeksRincianKedua = TextStyle(fontSize = 14.sp, color = WarnaIos.LabelKedua)

/**
 * Persetujuan & Pembatalan — cermin `app/approvals/` web.
 *
 * Seluruh tiga tab web ada di sini dan berfungsi penuh:
 * 1. "Void Transaksi": Antrean persetujuan void dari kasir via RPC `process_void_request`.
 * 2. "Bypass POS": Persetujuan bypass kuncian POS kasir langsung via update scoped RLS.
 * 3. "Pesanan Selesai": Pencarian & pembatalan paksa pesanan yang sudah selesai via RPC `force_cancel_order`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersetujuanScreen(
    onExit: () -> Unit,
    viewModel: PersetujuanViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    RealtimeRefresh(
        RealtimeTables.CANCELLATION_REQUESTS,
        RealtimeTables.BYPASS_REQUESTS,
        RealtimeTables.ORDERS,
    ) { viewModel.muatUlang(silent = true) }

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
                judul = "Persetujuan",
                onKembali = onExit,
                aksi = { TombolBundarIos(IkonIos.Refresh, "Muat ulang", viewModel::muatUlang) },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(
                start = UkuranIos.TepiLayar,
                end = UkuranIos.TepiLayar,
                top = 12.dp,
                bottom = 16.dp,
            ).denganRuangNav(),
            verticalArrangement = Arrangement.spacedBy(UkuranIos.JarakKartu),
        ) {
            item { PanelKepala(state, viewModel) }
            item { BarisTab(state, viewModel) }
            when (state.tab) {
                TabPersetujuan.VOID -> isiTabVoid(state, viewModel)
                TabPersetujuan.BYPASS -> isiTabBypass(state, viewModel)
                TabPersetujuan.BATAL_PAKSA -> isiTabPesananSelesai(state, viewModel)
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }

    state.targetBatal?.let { target ->
        DialogBatalPaksa(
            target = target,
            catatan = state.catatanBatal,
            sedangMembatalkan = state.sedangMembatalkan,
            onUbahCatatan = viewModel::ubahCatatanBatal,
            onKonfirmasi = viewModel::eksekusiBatalPaksa,
            onTutup = viewModel::tutupDialogBatal,
        )
    }
}

@Composable
private fun PanelKepala(state: PersetujuanUiState, viewModel: PersetujuanViewModel) {
    var dialogTanggal by remember { mutableStateOf(false) }

    KartuPanel {
        Text("Persetujuan & Pembatalan", style = TipeIos.Utama)
        Spacer(Modifier.height(2.dp))
        Text("Antrean pengajuan dari kasir outlet", style = TipeIos.Catatan)
        Spacer(Modifier.height(12.dp))
        // Deret periode bisa lebih lebar dari layar sempit (label kustom memuat dua
        // tanggal), jadi segmennya digulir alih-alih dipaksa berbagi lebar.
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogRentang(onTutup: () -> Unit, onPilih: (LocalDate, LocalDate) -> Unit) {
    val picker = rememberDateRangePickerState()
    val warnaKalender = DatePickerDefaults.colors(
        containerColor = WarnaIos.Kartu,
        selectedDayContainerColor = WarnaIos.Aksen,
        dayInSelectionRangeContainerColor = WarnaIos.Aksen.copy(alpha = 0.14f),
        todayDateBorderColor = WarnaIos.Aksen,
        todayContentColor = WarnaIos.Aksen,
    )
    DatePickerDialog(
        onDismissRequest = onTutup,
        confirmButton = {
            TextButton(
                onClick = {
                    val dari = picker.selectedStartDateMillis?.let(::tanggalDari)
                    val sampai = picker.selectedEndDateMillis?.let(::tanggalDari)
                    if (dari != null) onPilih(dari, sampai ?: dari)
                },
                enabled = picker.selectedStartDateMillis != null,
                colors = ButtonDefaults.textButtonColors(contentColor = WarnaIos.Aksen),
            ) { Text("Terapkan", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(
                onClick = onTutup,
                colors = ButtonDefaults.textButtonColors(contentColor = WarnaIos.Aksen),
            ) { Text("Batal", fontSize = 16.sp) }
        },
        shape = UkuranIos.SudutKartu,
        colors = warnaKalender,
    ) {
        DateRangePicker(
            state = picker,
            colors = warnaKalender,
            title = {
                Text(
                    "Pilih Rentang Tanggal",
                    Modifier.padding(start = 24.dp, top = 16.dp),
                    style = TipeIos.Utama,
                )
            },
        )
    }
}

private fun tanggalDari(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

@Composable
private fun BarisTab(state: PersetujuanUiState, viewModel: PersetujuanViewModel) {
    // Tiga label tab ini terlalu panjang untuk dibagi rata di ponsel 360dp tanpa
    // terpotong, jadi segmennya selebar isinya dan deretnya digulir.
    WadahSegmenIos(gulir = true) {
        SegmenIos(
            label = "Void Transaksi",
            aktif = state.tab == TabPersetujuan.VOID,
            onKlik = { viewModel.pilihTab(TabPersetujuan.VOID) },
            lencana = state.jumlahVoid.takeIf { it > 0 }?.let(::cacah),
        )
        SegmenIos(
            label = "Bypass POS",
            aktif = state.tab == TabPersetujuan.BYPASS,
            onKlik = { viewModel.pilihTab(TabPersetujuan.BYPASS) },
            lencana = state.jumlahBypass.takeIf { it > 0 }?.let(::cacah),
        )
        SegmenIos(
            label = "Pesanan Selesai",
            aktif = state.tab == TabPersetujuan.BATAL_PAKSA,
            onKlik = { viewModel.pilihTab(TabPersetujuan.BATAL_PAKSA) },
        )
    }
}

/* ----------------------------------------------------------------------- */
/* Potongan kartu bersama                                                   */
/* ----------------------------------------------------------------------- */

/** Baris pembuka kartu: lencana outlet lalu waktu relatif pengajuan. */
@Composable
private fun BarisOutletWaktu(outlet: String, dibuatPada: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ChipJingga(outlet)
        Spacer(Modifier.width(8.dp))
        Icon(IkonIos.Schedule, null, tint = WarnaIos.LabelKedua, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(4.dp))
        Text(waktuRelatif(dibuatPada), style = TipeIos.Kecil, maxLines = 1)
    }
}

/** Judul kartu "Void Transaksi #123": label hitam, nomor order beraksen. */
@Composable
private fun JudulNomorOrder(label: String, nomor: String) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(label, style = TipeIos.Utama)
        Text("#$nomor", style = TipeIos.Utama.copy(color = WarnaIos.Aksen))
    }
}

@Composable
private fun RincianItem(qty: String, nama: String, subtotal: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(qty, Modifier.width(32.dp), style = TeksRincianKedua)
        Text(nama, Modifier.weight(1f), style = TeksRincian, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.width(8.dp))
        Text(subtotal, style = TeksRincianKedua, maxLines = 1)
    }
}

@Composable
private fun BarisTotal(total: Long) {
    Spacer(Modifier.height(12.dp))
    PemisahIos(inset = 0.dp)
    Spacer(Modifier.height(10.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Total Nilai", Modifier.weight(1f), style = TipeIos.SubJudul)
        Text(rupiah(total), style = TipeIos.Angka, maxLines = 1)
    }
}

/** Pasangan tombol tolak/setujui. Selama diproses keduanya terkunci agar tidak terkirim dua kali. */
@Composable
private fun BarisTolakSetujui(sedangDiproses: Boolean, onTolak: () -> Unit, onSetujui: () -> Unit) {
    Spacer(Modifier.height(14.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        TombolKeduaIos(
            teks = "Tolak",
            onKlik = onTolak,
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

/* ----------------------------------------------------------------------- */
/* Tab void                                                                 */
/* ----------------------------------------------------------------------- */

private fun LazyListScope.isiTabVoid(state: PersetujuanUiState, viewModel: PersetujuanViewModel) {
    if (state.void.isEmpty()) {
        item { PanelAntreanKosong(state.memuat, "Tidak ada pengajuan void transaksi saat ini.") }
        return
    }
    items(state.void, key = { it.id }) { pengajuan ->
        KartuVoid(
            pengajuan = pengajuan,
            sedangDiproses = pengajuan.id in state.sedangDiproses,
            onSetujui = { viewModel.prosesVoid(pengajuan, setujui = true) },
            onTolak = { viewModel.prosesVoid(pengajuan, setujui = false) },
        )
    }
}

@Composable
private fun KartuVoid(
    pengajuan: PengajuanVoid,
    sedangDiproses: Boolean,
    onSetujui: () -> Unit,
    onTolak: () -> Unit,
) {
    KartuPanel {
        BarisOutletWaktu(pengajuan.outletNama, pengajuan.dibuatPada)

        Spacer(Modifier.height(10.dp))
        JudulNomorOrder("Void Transaksi ", pengajuan.nomorOrder)

        Spacer(Modifier.height(4.dp))
        Text(
            "Pelanggan: ${pengajuan.namaPelanggan} • Kasir: ${pengajuan.pemohon}",
            style = TipeIos.Catatan,
        )

        Spacer(Modifier.height(12.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(UkuranIos.SudutBlok)
                .background(WarnaIos.Latar)
                .padding(12.dp),
        ) {
            LabelSeksiIos("Alasan pembatalan")
            Spacer(Modifier.height(4.dp))
            Text(
                pengajuan.alasan.ifBlank { "Tanpa keterangan" },
                style = TipeIos.SubJudul.copy(color = WarnaIos.Label),
            )
        }

        if (pengajuan.items.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            LabelSeksiIos("Rincian pesanan")
            Spacer(Modifier.height(4.dp))
            pengajuan.items.forEach { item ->
                RincianItem("${item.qty}x", item.nama, rupiah(item.subtotal))
            }
        }

        BarisTotal(pengajuan.total)
        BarisTolakSetujui(sedangDiproses, onTolak = onTolak, onSetujui = onSetujui)
    }
}

/* ----------------------------------------------------------------------- */
/* Tab bypass                                                               */
/* ----------------------------------------------------------------------- */

private fun LazyListScope.isiTabBypass(state: PersetujuanUiState, viewModel: PersetujuanViewModel) {
    if (state.bypass.isEmpty()) {
        item {
            PanelAntreanKosong(
                state.memuat,
                "Tidak ada pengajuan bypass POS yang menunggu persetujuan.",
            )
        }
        return
    }
    items(state.bypass, key = { it.id }) { pengajuan ->
        KartuBypass(
            pengajuan = pengajuan,
            sedangDiproses = pengajuan.id in state.sedangDiproses,
            onSetujui = { viewModel.prosesBypass(pengajuan, setujui = true) },
            onTolak = { viewModel.prosesBypass(pengajuan, setujui = false) },
        )
    }
}

@Composable
private fun KartuBypass(
    pengajuan: PengajuanBypass,
    sedangDiproses: Boolean,
    onSetujui: () -> Unit,
    onTolak: () -> Unit,
) {
    KartuPanel {
        BarisOutletWaktu(pengajuan.outletNama, pengajuan.dibuatPada)

        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IkonBulatIos(IkonIos.Lock, WarnaIos.Oranye, ukuran = 28.dp)
            Spacer(Modifier.width(10.dp))
            Text("Bypass POS dari ${pengajuan.pemohon}", style = TipeIos.Utama)
        }

        Spacer(Modifier.height(12.dp))
        // Blok alasan bernada peringatan: bypass membuka kuncian POS, jadi dibedakan
        // dari alasan void yang netral.
        Column(
            Modifier
                .fillMaxWidth()
                .clip(UkuranIos.SudutBlok)
                .background(NadaIos.PERINGATAN.warna.copy(alpha = 0.10f))
                .padding(12.dp),
        ) {
            Text(
                "Alasan Bypass",
                style = TipeIos.Kecil.copy(color = NadaIos.PERINGATAN.teks, fontWeight = FontWeight.SemiBold),
            )
            Spacer(Modifier.height(4.dp))
            Text(pengajuan.alasan, style = TipeIos.SubJudul.copy(color = WarnaIos.Label))
        }

        BarisTolakSetujui(sedangDiproses, onTolak = onTolak, onSetujui = onSetujui)
    }
}

@Composable
private fun PanelAntreanKosong(memuat: Boolean, pesan: String) {
    KartuPanel {
        // Cabang ditulis eksplisit if/else, BUKAN keluar-awal `return@Column`.
        // `Column` adalah fungsi inline: keluar dari lambdanya setelah sudah
        // memancarkan composable meninggalkan pembukuan grup kompilator tidak
        // seimbang, dan begitu keadaannya berbalik, tabel slot dibaca dengan
        // indeks negatif -> ArrayIndexOutOfBoundsException di SlotTableKt.key.
        if (memuat) {
            PanelMemuat("Memuat antrean...")
        } else {
            KeadaanIos(
                ikon = IkonIos.CheckCircle,
                judul = "Antrean bersih",
                pesan = pesan,
                nada = NadaIos.SUKSES,
            )
        }
    }
}

/** Isi kartu saat data belum datang: pemutar kecil dan satu baris keterangan. */
@Composable
private fun PanelMemuat(teks: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 28.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(Modifier.size(16.dp), color = WarnaIos.Abu, strokeWidth = 2.dp)
        Spacer(Modifier.width(10.dp))
        Text(teks, style = TipeIos.SubJudul)
    }
}

/* ----------------------------------------------------------------------- */
/* Tab pesanan selesai (batal paksa)                                      */
/* ----------------------------------------------------------------------- */

private fun LazyListScope.isiTabPesananSelesai(
    state: PersetujuanUiState,
    viewModel: PersetujuanViewModel,
) {
    item { PanelKontrolPesananSelesai(state, viewModel) }

    if (state.outletTerpilih == null && state.daftarOutlet.isEmpty()) {
        item {
            PanelAntreanKosong(
                memuat = state.memuat,
                pesan = "Anda belum ditugaskan ke outlet mana pun.",
            )
        }
        return
    }

    if (state.memuatPesananSelesai) {
        item {
            PanelAntreanKosong(
                memuat = true,
                pesan = "Mencari pesanan selesai...",
            )
        }
        return
    }

    if (state.pesananSelesai.isEmpty()) {
        item {
            PanelPesananSelesaiKosong(state.kueriPencarian)
        }
        return
    }

    items(state.pesananSelesai, key = { it.id }) { pesanan ->
        KartuPesananSelesai(
            pesanan = pesanan,
            onBatal = { viewModel.bukaDialogBatal(pesanan) },
        )
    }
}

@Composable
private fun PanelKontrolPesananSelesai(
    state: PersetujuanUiState,
    viewModel: PersetujuanViewModel,
) {
    var menuOutlet by remember { mutableStateOf(false) }

    KartuPanel {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Cari Pesanan Selesai", style = TipeIos.Utama)
                Spacer(Modifier.height(2.dp))
                Text(
                    "Hanya pesanan berstatus selesai yang dapat dibatalkan di sini",
                    style = TipeIos.Catatan,
                )
            }
            if (state.daftarOutlet.size > 1) {
                Spacer(Modifier.width(10.dp))
                Box {
                    TombolKapsulIos(
                        teks = state.outletTerpilih?.nama ?: "Pilih Outlet",
                        onKlik = { menuOutlet = true },
                        modifier = Modifier.widthIn(max = 170.dp),
                        ikon = IkonIos.Storefront,
                        chevron = true,
                    )

                    SukaDropdownMenu(
                        expanded = menuOutlet,
                        onDismissRequest = { menuOutlet = false },
                    ) {
                        SukaDropdownHeader(title = "PILIH OUTLET", onClose = { menuOutlet = false })
                        state.daftarOutlet.forEach { outlet ->
                            SukaDropdownMenuItem(
                                text = outlet.nama,
                                selected = (outlet.id == state.outletTerpilih?.id),
                                onClick = {
                                    menuOutlet = false
                                    viewModel.pilihOutlet(outlet)
                                },
                            )
                        }
                    }
                }
            } else if (state.daftarOutlet.size == 1) {
                Spacer(Modifier.width(10.dp))
                ChipJingga(state.daftarOutlet[0].nama)
            }
        }

        Spacer(Modifier.height(12.dp))

        KolomCariIos(
            nilai = state.kueriPencarian,
            onUbah = viewModel::ubahKueri,
            modifier = Modifier.fillMaxWidth(),
            placeholder = "Cari nomor order atau nama pelanggan...",
        )
    }
}

@Composable
private fun KartuPesananSelesai(
    pesanan: PesananSelesaiItem,
    onBatal: () -> Unit,
) {
    KartuPanel {
        BarisOutletWaktu(pesanan.outletNama, pesanan.dibuatPada)

        Spacer(Modifier.height(10.dp))
        JudulNomorOrder("Pesanan ", pesanan.nomorOrder)

        Spacer(Modifier.height(4.dp))
        Text("Pelanggan: ${pesanan.namaPelanggan}", style = TipeIos.Catatan)

        if (pesanan.items.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            LabelSeksiIos("Rincian pesanan")
            Spacer(Modifier.height(4.dp))
            pesanan.items.forEach { item ->
                RincianItem("${item.qty}x", item.nama, rupiah(item.subtotal))
            }
        }

        BarisTotal(pesanan.total)

        Spacer(Modifier.height(14.dp))
        TombolKeduaIos(
            teks = "Batalkan Pesanan",
            onKlik = onBatal,
            ikon = IkonIos.Close,
            warna = WarnaIos.Merah,
        )
    }
}

@Composable
private fun PanelPesananSelesaiKosong(kueri: String) {
    KartuPanel {
        KeadaanIos(
            ikon = IkonIos.Search,
            judul = if (kueri.isNotBlank()) "Tidak Ditemukan" else "Belum Ada Pesanan",
            pesan = if (kueri.isNotBlank()) {
                "Tidak ada pesanan selesai yang cocok dengan pencarian \"$kueri\"."
            } else {
                "Tidak ada pesanan selesai pada periode dan outlet ini."
            },
        )
    }
}

@Composable
private fun DialogBatalPaksa(
    target: PesananSelesaiItem,
    catatan: String,
    sedangMembatalkan: Boolean,
    onUbahCatatan: (String) -> Unit,
    onKonfirmasi: () -> Unit,
    onTutup: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!sedangMembatalkan) onTutup() },
        confirmButton = {
            // Aksi destruktif ala alert iOS: teks merah, bukan tombol terisi.
            TextButton(
                onClick = onKonfirmasi,
                enabled = catatan.isNotBlank() && !sedangMembatalkan,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = WarnaIos.Merah,
                    disabledContentColor = WarnaIos.Merah.copy(alpha = 0.4f),
                ),
            ) {
                if (sedangMembatalkan) {
                    CircularProgressIndicator(
                        Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = WarnaIos.Merah,
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text("Ya, Batalkan", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onTutup,
                enabled = !sedangMembatalkan,
                colors = ButtonDefaults.textButtonColors(contentColor = WarnaIos.Aksen),
            ) {
                Text("Kembali", fontSize = 16.sp)
            }
        },
        icon = {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(NadaIos.BAHAYA.warna.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(IkonIos.WarningAmber, null, tint = NadaIos.BAHAYA.warna, modifier = Modifier.size(26.dp))
            }
        },
        title = {
            Text("Batalkan Pesanan #${target.nomorOrder}?", style = TipeIos.Utama)
        },
        text = {
            Column {
                Text(
                    "Pesanan yang berstatus selesai ini akan dibatalkan secara permanen. Catatan alasan wajib diisi.",
                    style = TipeIos.Catatan,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = catatan,
                    onValueChange = onUbahCatatan,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text("Contoh: order double input, duplikat dari #...", style = TipeIos.Catatan.copy(color = WarnaIos.Abu))
                    },
                    minLines = 3,
                    maxLines = 5,
                    shape = UkuranIos.SudutKontrol,
                    colors = warnaKolomIos(),
                )
            }
        },
        shape = UkuranIos.SudutKartu,
        containerColor = WarnaIos.Kartu,
    )
}
