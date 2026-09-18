package com.sukashawarma.superapp.feature.manager.ui.persetujuan

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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import com.sukashawarma.superapp.core.ui.SukaDropdownHeader
import com.sukashawarma.superapp.core.ui.SukaDropdownMenu
import com.sukashawarma.superapp.core.ui.SukaDropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.RealtimeTables
import com.sukashawarma.superapp.feature.manager.domain.PengajuanBypass
import com.sukashawarma.superapp.feature.manager.domain.PengajuanVoid
import com.sukashawarma.superapp.feature.manager.domain.PesananSelesaiItem
import com.sukashawarma.superapp.feature.manager.domain.PresetPeriode
import com.sukashawarma.superapp.feature.manager.domain.TabPersetujuan
import com.sukashawarma.superapp.feature.manager.domain.cacah
import com.sukashawarma.superapp.feature.manager.domain.rupiah
import com.sukashawarma.superapp.feature.manager.domain.waktuRelatif
import com.sukashawarma.superapp.feature.manager.ui.ChipJingga
import com.sukashawarma.superapp.feature.manager.ui.GarisKartu
import com.sukashawarma.superapp.feature.manager.ui.HijauLatar
import com.sukashawarma.superapp.feature.manager.ui.HijauTeks
import com.sukashawarma.superapp.feature.manager.ui.KartuPanel
import com.sukashawarma.superapp.feature.manager.ui.MerahGaris
import com.sukashawarma.superapp.feature.manager.ui.MerahLatar
import com.sukashawarma.superapp.feature.manager.ui.MerahTeks
import com.sukashawarma.superapp.presentation.theme.SukaBrown
import com.sukashawarma.superapp.presentation.theme.SukaCream
import com.sukashawarma.superapp.presentation.theme.SukaGray400
import com.sukashawarma.superapp.presentation.theme.SukaOrange
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private val FILTER_PERIODE = listOf(
    PresetPeriode.HARI_INI to "Hari Ini",
    PresetPeriode.KEMARIN to "Kemarin",
    PresetPeriode.MINGGU to "7 Hari",
    PresetPeriode.BULAN to "30 Hari",
)

private val AMBER_LATAR = Color(0xFFFEF3C7)
private val AMBER_GARIS = Color(0xFFFCD34D)
private val AMBER_TEKS = Color(0xFF78350F)

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
        containerColor = SukaCream,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Text("Persetujuan", fontWeight = FontWeight.Black, fontSize = 17.sp, color = SukaBrown, maxLines = 1)
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Persetujuan & Pembatalan",
                    color = SukaBrown,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "Antrean pengajuan dari kasir outlet",
                    color = SukaGray400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FILTER_PERIODE.forEach { (preset, label) ->
                ChipPeriode(label, !state.memakaiKustom && state.preset == preset) {
                    viewModel.pilihPreset(preset)
                }
            }
            ChipPeriode(
                state.kustom?.let { "${it.dari} - ${it.sampai}" } ?: "Kustom",
                state.memakaiKustom,
                Icons.Default.CalendarMonth,
            ) { dialogTanggal = true }
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

@Composable
private fun ChipPeriode(
    label: String,
    aktif: Boolean,
    ikon: ImageVector? = null,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (aktif) SukaOrange else Color.White,
        border = BorderStroke(1.dp, if (aktif) SukaOrange else SukaBrown.copy(alpha = 0.15f)),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (ikon != null) {
                Icon(ikon, null, tint = if (aktif) Color.White else SukaBrown, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(5.dp))
            }
            Text(
                label,
                color = if (aktif) Color.White else SukaBrown,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogRentang(onTutup: () -> Unit, onPilih: (LocalDate, LocalDate) -> Unit) {
    val picker = rememberDateRangePickerState()
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
            ) { Text("Terapkan", fontWeight = FontWeight.ExtraBold) }
        },
        dismissButton = { TextButton(onClick = onTutup) { Text("Batal") } },
    ) {
        DateRangePicker(state = picker, title = {
            Text(
                "Pilih Rentang Tanggal",
                Modifier.padding(start = 24.dp, top = 16.dp),
                fontWeight = FontWeight.Black,
            )
        })
    }
}

private fun tanggalDari(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

@Composable
private fun BarisTab(state: PersetujuanUiState, viewModel: PersetujuanViewModel) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TombolTab(
            terpilih = state.tab == TabPersetujuan.VOID,
            label = "Void Transaksi",
            jumlah = state.jumlahVoid,
            modifier = Modifier.weight(1f),
        ) { viewModel.pilihTab(TabPersetujuan.VOID) }
        TombolTab(
            terpilih = state.tab == TabPersetujuan.BYPASS,
            label = "Bypass POS",
            jumlah = state.jumlahBypass,
            modifier = Modifier.weight(1f),
        ) { viewModel.pilihTab(TabPersetujuan.BYPASS) }
        TombolTab(
            terpilih = state.tab == TabPersetujuan.BATAL_PAKSA,
            label = "Pesanan Selesai",
            jumlah = 0,
            modifier = Modifier.weight(1f),
        ) { viewModel.pilihTab(TabPersetujuan.BATAL_PAKSA) }
    }
}

@Composable
private fun TombolTab(
    terpilih: Boolean,
    label: String,
    jumlah: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (terpilih) SukaOrange else Color.Transparent,
    ) {
        Row(
            Modifier.padding(horizontal = 4.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                label,
                color = if (terpilih) Color.White else SukaBrown.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
            )
            if (jumlah > 0) {
                Spacer(Modifier.width(4.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (terpilih) Color.White.copy(alpha = 0.25f) else SukaOrange.copy(alpha = 0.12f),
                ) {
                    Text(
                        cacah(jumlah),
                        Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                        color = if (terpilih) Color.White else SukaOrange,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            ChipJingga(pengajuan.outletNama)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.Schedule, null, tint = SukaGray400, modifier = Modifier.size(11.dp))
            Spacer(Modifier.width(3.dp))
            Text(
                waktuRelatif(pengajuan.dibuatPada),
                color = SukaGray400,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text("Void Transaksi ", color = SukaBrown, fontSize = 16.sp, fontWeight = FontWeight.Black)
            Text("#${pengajuan.nomorOrder}", color = SukaOrange, fontSize = 16.sp, fontWeight = FontWeight.Black)
        }

        Spacer(Modifier.height(6.dp))
        Text(
            "Pelanggan: ${pengajuan.namaPelanggan} • Kasir: ${pengajuan.pemohon}",
            color = SukaGray400,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 16.sp,
        )

        Spacer(Modifier.height(12.dp))
        Surface(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = SukaBrown.copy(alpha = 0.03f),
            border = BorderStroke(1.dp, GarisKartu),
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(
                    "ALASAN PEMBATALAN",
                    color = SukaGray400,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.6.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    pengajuan.alasan.ifBlank { "Tanpa keterangan" },
                    color = SukaBrown,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        if (pengajuan.items.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(
                "RINCIAN PESANAN",
                color = SukaGray400,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.6.sp,
            )
            Spacer(Modifier.height(6.dp))
            pengajuan.items.forEach { item ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Text(
                        "${item.qty}x",
                        Modifier.width(28.dp),
                        color = SukaGray400,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        item.nama,
                        Modifier.weight(1f),
                        color = SukaBrown,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        rupiah(item.subtotal),
                        color = SukaGray400,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = GarisKartu)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "TOTAL NILAI",
                Modifier.weight(1f),
                color = SukaGray400,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.6.sp,
            )
            Text(rupiah(pengajuan.total), color = SukaBrown, fontSize = 19.sp, fontWeight = FontWeight.Black)
        }

        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TombolAksi(
                label = "Tolak",
                ikon = Icons.Default.Close,
                latar = Color.White,
                teks = MerahTeks,
                garis = MerahGaris,
                sedangDiproses = sedangDiproses,
                modifier = Modifier.weight(1f),
                onClick = onTolak,
            )
            TombolAksi(
                label = "Setujui",
                ikon = Icons.Default.Check,
                latar = Color(0xFFD97706),
                teks = Color.White,
                garis = Color(0xFFD97706),
                sedangDiproses = sedangDiproses,
                modifier = Modifier.weight(1f),
                onClick = onSetujui,
            )
        }
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            ChipJingga(pengajuan.outletNama)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.Schedule, null, tint = SukaGray400, modifier = Modifier.size(11.dp))
            Spacer(Modifier.width(3.dp))
            Text(
                waktuRelatif(pengajuan.dibuatPada),
                color = SukaGray400,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.VpnKey, null, tint = AMBER_TEKS, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(7.dp))
            Text(
                "Bypass POS dari ${pengajuan.pemohon}",
                color = SukaBrown,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 20.sp,
            )
        }

        Spacer(Modifier.height(12.dp))
        Surface(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = AMBER_LATAR.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, AMBER_GARIS),
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(
                    "ALASAN BYPASS",
                    color = AMBER_TEKS.copy(alpha = 0.75f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.6.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    pengajuan.alasan,
                    color = SukaBrown,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TombolAksi(
                label = "Tolak",
                ikon = Icons.Default.Close,
                latar = Color.White,
                teks = MerahTeks,
                garis = MerahGaris,
                sedangDiproses = sedangDiproses,
                modifier = Modifier.weight(1f),
                onClick = onTolak,
            )
            TombolAksi(
                label = "Setujui",
                ikon = Icons.Default.Check,
                latar = Color(0xFFD97706),
                teks = Color.White,
                garis = Color(0xFFD97706),
                sedangDiproses = sedangDiproses,
                modifier = Modifier.weight(1f),
                onClick = onSetujui,
            )
        }
    }
}

@Composable
private fun TombolAksi(
    label: String,
    ikon: ImageVector,
    latar: Color,
    teks: Color,
    garis: Color,
    sedangDiproses: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.clickable(enabled = !sedangDiproses, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (sedangDiproses) latar.copy(alpha = 0.55f) else latar,
        border = BorderStroke(1.dp, garis),
    ) {
        Row(
            Modifier.padding(vertical = 11.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (sedangDiproses) {
                CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = teks)
            } else {
                Icon(ikon, null, tint = teks, modifier = Modifier.size(15.dp))
            }
            Spacer(Modifier.width(6.dp))
            Text(label, color = teks, fontSize = 12.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun PanelAntreanKosong(memuat: Boolean, pesan: String) {
    KartuPanel {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Cabang ditulis eksplisit if/else, BUKAN keluar-awal `return@Column`.
            // `Column` adalah fungsi inline: keluar dari lambdanya setelah sudah
            // memancarkan composable meninggalkan pembukuan grup kompilator tidak
            // seimbang, dan begitu keadaannya berbalik, tabel slot dibaca dengan
            // indeks negatif -> ArrayIndexOutOfBoundsException di SlotTableKt.key.
            if (memuat) {
                Text("Memuat antrean...", color = SukaGray400, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            } else {
                Box(
                    Modifier.size(52.dp).background(HijauLatar, RoundedCornerShape(50)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.CheckCircle, null, tint = HijauTeks, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text("Antrean bersih", color = SukaBrown, fontSize = 15.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(4.dp))
                Text(pesan, color = SukaGray400, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
        }
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
                Text(
                    "Cari Pesanan Selesai",
                    color = SukaBrown,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Hanya pesanan berstatus selesai yang dapat dibatalkan di sini",
                    color = SukaGray400,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            if (state.daftarOutlet.size > 1) {
                Box {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SukaOrange.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, SukaOrange.copy(alpha = 0.3f)),
                        modifier = Modifier.clickable { menuOutlet = true },
                    ) {
                        Row(
                            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.Storefront,
                                null,
                                tint = SukaOrange,
                                modifier = Modifier.size(13.dp),
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                state.outletTerpilih?.nama ?: "Pilih Outlet",
                                color = SukaBrown,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(end = 2.dp),
                            )
                            Icon(
                                Icons.Default.ArrowDropDown,
                                null,
                                tint = SukaBrown,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }

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
                ChipJingga(state.daftarOutlet[0].nama)
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = state.kueriPencarian,
            onValueChange = viewModel::ubahKueri,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text("Cari nomor order atau nama pelanggan...", fontSize = 12.sp, color = SukaGray400)
            },
            leadingIcon = {
                Icon(Icons.Default.Search, null, tint = SukaGray400, modifier = Modifier.size(18.dp))
            },
            trailingIcon = {
                if (state.kueriPencarian.isNotEmpty()) {
                    IconButton(onClick = { viewModel.ubahKueri("") }) {
                        Icon(Icons.Default.Close, "Hapus pencarian", tint = SukaGray400, modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SukaOrange,
                unfocusedBorderColor = SukaBrown.copy(alpha = 0.15f),
                cursorColor = SukaOrange,
            ),
        )
    }
}

@Composable
private fun KartuPesananSelesai(
    pesanan: PesananSelesaiItem,
    onBatal: () -> Unit,
) {
    KartuPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ChipJingga(pesanan.outletNama)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.Schedule, null, tint = SukaGray400, modifier = Modifier.size(11.dp))
            Spacer(Modifier.width(3.dp))
            Text(
                waktuRelatif(pesanan.dibuatPada),
                color = SukaGray400,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text("Pesanan ", color = SukaBrown, fontSize = 16.sp, fontWeight = FontWeight.Black)
            Text("#${pesanan.nomorOrder}", color = SukaOrange, fontSize = 16.sp, fontWeight = FontWeight.Black)
        }

        Spacer(Modifier.height(4.dp))
        Text(
            "Pelanggan: ${pesanan.namaPelanggan}",
            color = SukaGray400,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )

        if (pesanan.items.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(
                "RINCIAN PESANAN",
                color = SukaGray400,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.6.sp,
            )
            Spacer(Modifier.height(6.dp))
            pesanan.items.forEach { item ->
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text(
                        "${item.qty}x",
                        Modifier.width(28.dp),
                        color = SukaGray400,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        item.nama,
                        Modifier.weight(1f),
                        color = SukaBrown,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        rupiah(item.subtotal),
                        color = SukaGray400,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = GarisKartu)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "TOTAL NILAI",
                Modifier.weight(1f),
                color = SukaGray400,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.6.sp,
            )
            Text(rupiah(pesanan.total), color = SukaBrown, fontSize = 18.sp, fontWeight = FontWeight.Black)
        }

        Spacer(Modifier.height(14.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onBatal),
            shape = RoundedCornerShape(12.dp),
            color = MerahLatar,
            border = BorderStroke(1.dp, MerahGaris),
        ) {
            Row(
                Modifier.padding(vertical = 11.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Cancel, null, tint = MerahTeks, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(7.dp))
                Text("Batalkan Pesanan", color = MerahTeks, fontSize = 12.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun PanelPesananSelesaiKosong(kueri: String) {
    KartuPanel {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(52.dp).background(SukaCream, RoundedCornerShape(50)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Search, null, tint = SukaGray400, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(
                if (kueri.isNotBlank()) "Tidak Ditemukan" else "Belum Ada Pesanan",
                color = SukaBrown,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (kueri.isNotBlank()) {
                    "Tidak ada pesanan selesai yang cocok dengan pencarian \"$kueri\"."
                } else {
                    "Tidak ada pesanan selesai pada periode dan outlet ini."
                },
                color = SukaGray400,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
        }
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
            Surface(
                modifier = Modifier.clickable(
                    enabled = catatan.isNotBlank() && !sedangMembatalkan,
                    onClick = onKonfirmasi,
                ),
                shape = RoundedCornerShape(10.dp),
                color = if (catatan.isNotBlank() && !sedangMembatalkan) Color(0xFFDC2626) else Color(0xFFDC2626).copy(alpha = 0.4f),
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (sedangMembatalkan) {
                        CircularProgressIndicator(
                            Modifier.size(13.dp),
                            strokeWidth = 2.dp,
                            color = Color.White,
                        )
                    } else {
                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                    Text("Ya, Batalkan", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onTutup,
                enabled = !sedangMembatalkan,
            ) {
                Text("Kembali", color = SukaBrown, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        },
        icon = {
            Box(
                Modifier.size(48.dp).background(MerahLatar, RoundedCornerShape(50)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.WarningAmber, null, tint = MerahTeks, modifier = Modifier.size(26.dp))
            }
        },
        title = {
            Text(
                "Batalkan Pesanan #${target.nomorOrder}?",
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                color = SukaBrown,
            )
        },
        text = {
            Column {
                Text(
                    "Pesanan yang berstatus selesai ini akan dibatalkan secara permanen. Catatan alasan wajib diisi.",
                    fontSize = 12.sp,
                    color = SukaGray400,
                    lineHeight = 17.sp,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = catatan,
                    onValueChange = onUbahCatatan,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text("Contoh: order double input, duplikat dari #...", fontSize = 11.sp, color = SukaGray400)
                    },
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MerahTeks,
                        unfocusedBorderColor = SukaBrown.copy(alpha = 0.2f),
                        cursorColor = MerahTeks,
                    ),
                )
            }
        },
        shape = RoundedCornerShape(18.dp),
        containerColor = Color.White,
    )
}

