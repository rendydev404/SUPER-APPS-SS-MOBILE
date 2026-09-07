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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.sukashawarma.superapp.feature.manager.domain.PresetPeriode
import com.sukashawarma.superapp.feature.manager.domain.TabPersetujuan
import com.sukashawarma.superapp.feature.manager.domain.VOID_BISA_DIPROSES_NATIVE
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
 * Dua dari tiga tab web ada di sini. Tab "Bypass POS" berfungsi penuh. Tab "Void
 * Transaksi" hanya bisa dibaca: RLS tidak memberi jalur tulis kepada area maupun
 * regional manager, lihat [VOID_BISA_DIPROSES_NATIVE]. Tab "Pesanan Selesai"
 * (pembatalan paksa) sengaja tidak dibuat karena terhalang hal yang sama, dan
 * layar pencarian tanpa satu pun tindakan yang bisa dijalankan hanya menyesatkan.
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
    ) { viewModel.muatUlang() }

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
                TabPersetujuan.VOID -> isiTabVoid(state)
                TabPersetujuan.BYPASS -> isiTabBypass(state, viewModel)
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
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
            if (state.memuat) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = SukaOrange)
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
        Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp)).padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
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
            Modifier.padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                label,
                color = if (terpilih) Color.White else SukaBrown.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
            )
            if (jumlah > 0) {
                Spacer(Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (terpilih) Color.White.copy(alpha = 0.25f) else SukaOrange.copy(alpha = 0.12f),
                ) {
                    Text(
                        cacah(jumlah),
                        Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
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

private fun LazyListScope.isiTabVoid(state: PersetujuanUiState) {
    if (!VOID_BISA_DIPROSES_NATIVE) {
        item { CatatanVoidBacaSaja() }
    }
    if (state.void.isEmpty()) {
        item { PanelAntreanKosong(state.memuat, "Tidak ada pengajuan void transaksi saat ini.") }
        return
    }
    items(state.void, key = { it.id }) { pengajuan -> KartuVoid(pengajuan) }
}

@Composable
private fun CatatanVoidBacaSaja() {
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
                "Antrean ini bisa dipantau di sini, tetapi persetujuannya masih " +
                    "dikerjakan lewat dashboard web. Database belum membuka jalur tulis " +
                    "pembatalan pesanan untuk aplikasi manajer.",
                color = AMBER_TEKS,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun KartuVoid(pengajuan: PengajuanVoid) {
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
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = SukaOrange)
                Spacer(Modifier.height(12.dp))
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
