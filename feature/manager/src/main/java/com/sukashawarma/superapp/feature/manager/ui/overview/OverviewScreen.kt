package com.sukashawarma.superapp.feature.manager.ui.overview

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.RealtimeTables
import com.sukashawarma.superapp.feature.manager.domain.ManagerAkses
import com.sukashawarma.superapp.feature.manager.domain.PerformaZona
import com.sukashawarma.superapp.feature.manager.domain.PeringkatOutlet
import com.sukashawarma.superapp.feature.manager.domain.PresetPeriode
import com.sukashawarma.superapp.feature.manager.domain.RingkasanArea
import com.sukashawarma.superapp.feature.manager.domain.StatusOutlet
import com.sukashawarma.superapp.feature.manager.domain.cacah
import com.sukashawarma.superapp.feature.manager.domain.rupiah
import com.sukashawarma.superapp.feature.manager.ui.BadgePerubahan
import com.sukashawarma.superapp.feature.manager.ui.BarProgres
import com.sukashawarma.superapp.feature.manager.ui.ChipJingga
import com.sukashawarma.superapp.feature.manager.ui.GarisKartu
import com.sukashawarma.superapp.feature.manager.ui.HijauGaris
import com.sukashawarma.superapp.feature.manager.ui.HijauLatar
import com.sukashawarma.superapp.feature.manager.ui.HijauTeks
import com.sukashawarma.superapp.feature.manager.ui.JudulPanel
import com.sukashawarma.superapp.feature.manager.ui.KartuKpi
import com.sukashawarma.superapp.feature.manager.ui.KartuPanel
import com.sukashawarma.superapp.feature.manager.ui.KartuRingkasZona
import com.sukashawarma.superapp.feature.manager.ui.LencanaPeringkat
import com.sukashawarma.superapp.feature.manager.ui.MerahGaris
import com.sukashawarma.superapp.feature.manager.ui.MerahLatar
import com.sukashawarma.superapp.feature.manager.ui.MerahTeks
import com.sukashawarma.superapp.feature.manager.ui.PanelKosong
import com.sukashawarma.superapp.feature.manager.ui.PilKeterangan
import com.sukashawarma.superapp.presentation.theme.SukaBrown
import com.sukashawarma.superapp.presentation.theme.SukaCream
import com.sukashawarma.superapp.presentation.theme.SukaGray400
import com.sukashawarma.superapp.presentation.theme.SukaOrange
import java.time.Instant
import java.time.ZoneOffset

/** Filter periode yang ditampilkan, urut sama dengan tombol segmented control web. */
private val FILTER = listOf(
    PresetPeriode.HARI_INI to "Hari Ini",
    PresetPeriode.KEMARIN to "Kemarin",
    PresetPeriode.MINGGU to "7 Hari",
    PresetPeriode.BULAN to "30 Hari",
)

/**
 * Ringkasan Area — layar utama modul Manager, cermin `app/page.tsx` web.
 *
 * Seluruh angka di sini bergerak sendiri: [RealtimeRefresh] memuat ulang begitu
 * salah satu tabel sumbernya berubah di server, jadi omzet bertambah saat kasir
 * menutup pesanan tanpa siapa pun menarik layar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    onExit: () -> Unit,
    viewModel: OverviewViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()

    RealtimeRefresh(
        RealtimeTables.ORDERS,
        RealtimeTables.ORDER_ITEMS,
        RealtimeTables.ATTENDANCE,
        RealtimeTables.OUTLETS,
        RealtimeTables.WASTE_REPORTS,
    ) { viewModel.muatUlang() }

    Scaffold(
        containerColor = SukaCream,
        topBar = {
            TopAppBar(
                title = {
                    Text("Ringkasan Area", fontWeight = FontWeight.Black, fontSize = 17.sp, color = SukaBrown)
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
            item { PanelPeriode(state, viewModel) }

            if (state.galat != null) {
                item { PanelGalat(state.galat!!, viewModel::muatUlang) }
            }

            item { KartuOmzet(state.ringkasan) }
            item { KartuTransaksi(state.ringkasan) }
            item { KartuPorsi(state.ringkasan) }
            item { KartuWaste(state.ringkasan) }
            item { KartuBonus(state.ringkasan) }

            if (ManagerAkses.melihatPerformaZona(state.role)) {
                item { PanelPerformaZona(state.ringkasan) }
            }

            item { PanelPeringkat(state.ringkasan) }
            item { PanelStatusOutlet(state.ringkasan) }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

/* ----------------------------------------------------------------------- */
/* Kepala layar: judul, role, dan pemilih periode                          */
/* ----------------------------------------------------------------------- */

@Composable
private fun PanelPeriode(state: OverviewUiState, viewModel: OverviewViewModel) {
    var dialogTanggal by remember { mutableStateOf(false) }

    KartuPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Ringkasan Area",
                color = SukaBrown,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.width(8.dp))
            ChipJingga("USER: ${labelRole(state.role?.value)}")
            Spacer(Modifier.weight(1f))
            if (state.memuat) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = SukaOrange)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Pantau performa pendapatan, transaksi, dan aktivitas cabang secara real-time",
            color = SukaGray400,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 16.sp,
        )
        Spacer(Modifier.height(14.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .background(SukaBrown.copy(alpha = 0.04f), RoundedCornerShape(16.dp))
                .padding(4.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            FILTER.forEach { (preset, label) ->
                TombolPeriode(
                    label = label,
                    aktif = !state.memakaiKustom && state.preset == preset,
                    onClick = { viewModel.pilihPreset(preset) },
                )
            }
            TombolPeriode(
                label = state.kustom?.let { "${it.dari} - ${it.sampai}" } ?: "Kustom",
                aktif = state.memakaiKustom,
                ikon = Icons.Default.CalendarMonth,
                onClick = { dialogTanggal = true },
            )
        }
    }

    if (dialogTanggal) {
        DialogRentangTanggal(
            onTutup = { dialogTanggal = false },
            onPilih = { dari, sampai ->
                viewModel.pilihRentangKustom(dari, sampai)
                dialogTanggal = false
            },
        )
    }
}

@Composable
private fun TombolPeriode(
    label: String,
    aktif: Boolean,
    ikon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (aktif) SukaOrange else Color.Transparent,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (ikon != null) {
                Icon(
                    ikon,
                    null,
                    tint = if (aktif) Color.White else SukaBrown.copy(alpha = 0.7f),
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(5.dp))
            }
            Text(
                label,
                color = if (aktif) Color.White else SukaBrown.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogRentangTanggal(
    onTutup: () -> Unit,
    onPilih: (java.time.LocalDate, java.time.LocalDate) -> Unit,
) {
    val picker = rememberDateRangePickerState()
    DatePickerDialog(
        onDismissRequest = onTutup,
        confirmButton = {
            TextButton(
                onClick = {
                    val dari = picker.selectedStartDateMillis?.let(::tanggalDariMillis)
                    val sampai = picker.selectedEndDateMillis?.let(::tanggalDariMillis)
                    // Memilih satu tanggal saja berarti rentang satu hari — lebih ramah
                    // daripada menonaktifkan tombol dan membiarkan pengguna menebak.
                    if (dari != null) onPilih(dari, sampai ?: dari)
                },
                enabled = picker.selectedStartDateMillis != null,
            ) { Text("Terapkan", fontWeight = FontWeight.ExtraBold) }
        },
        dismissButton = {
            TextButton(onClick = onTutup) { Text("Batal") }
        },
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

/** Pemilih tanggal bekerja di UTC; tanggal yang dipilih pengguna dibaca apa adanya. */
private fun tanggalDariMillis(millis: Long): java.time.LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

private fun labelRole(role: String?): String =
    (role ?: "regional_manager").replace('_', ' ').uppercase()

@Composable
private fun PanelGalat(pesan: String, onCoba: () -> Unit) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MerahLatar,
        border = BorderStroke(1.dp, MerahGaris),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(pesan, Modifier.weight(1f), color = MerahTeks, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onCoba) {
                Text("Coba Lagi", color = MerahTeks, fontWeight = FontWeight.Black, fontSize = 12.sp)
            }
        }
    }
}

/* ----------------------------------------------------------------------- */
/* Lima kartu KPI                                                          */
/* ----------------------------------------------------------------------- */

@Composable
private fun KartuOmzet(r: RingkasanArea) = KartuKpi(
    judul = "Gross Revenue",
    nilai = rupiah(r.omzet),
    ikon = Icons.Default.TrendingUp,
    warnaIkon = SukaOrange,
) { BadgePerubahan(r.perubahanOmzet) }

@Composable
private fun KartuTransaksi(r: RingkasanArea) = KartuKpi(
    judul = "Jumlah Transaksi",
    nilai = cacah(r.jumlahTransaksi),
    satuan = "order",
    ikon = Icons.Default.Schedule,
    warnaIkon = Color(0xFFD97706),
) { PilKeterangan("Selesai pada periode ini") }

@Composable
private fun KartuPorsi(r: RingkasanArea) = KartuKpi(
    judul = "Jumlah Item Terjual",
    nilai = cacah(r.jumlahItem),
    satuan = "porsi",
    ikon = Icons.Default.Checklist,
    warnaIkon = Color(0xFF059669),
) { PilKeterangan("Total produk pada periode ini") }

@Composable
private fun KartuWaste(r: RingkasanArea) = KartuKpi(
    judul = "Kerugian Waste",
    nilai = rupiah(r.kerugianWaste),
    ikon = Icons.Default.Delete,
    warnaIkon = Color(0xFFDC2626),
    warnaNilai = Color(0xFFDC2626),
) {
    if (r.wasteMenungguPersetujuan > 0) {
        Surface(
            shape = RoundedCornerShape(50),
            color = Color(0xFFFEF3C7),
            border = BorderStroke(1.dp, Color(0xFFFCD34D)),
        ) {
            Text(
                "${r.wasteMenungguPersetujuan} butuh approval",
                Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                color = Color(0xFF78350F),
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    } else {
        PilKeterangan("Tidak ada waste menunggu persetujuan")
    }
}

@Composable
private fun KartuBonus(r: RingkasanArea) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFFFFF9E5),
        border = BorderStroke(1.dp, Color(0xFFFCD34D)),
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFD97706)) {
                    Text(
                        "BONUS",
                        Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    "ESTIMASI INSENTIF",
                    Modifier.weight(1f),
                    color = Color(0xFF78350F),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp,
                )
                Box(
                    Modifier.size(44.dp).background(Color(0xFFF59E0B), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.WorkspacePremium,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                rupiah(r.estimasiBonus),
                color = Color(0xFF78350F),
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFF78350F).copy(alpha = 0.10f))
            Spacer(Modifier.height(10.dp))
            BadgePerubahan(r.perubahanBonus)
        }
    }
}

/* ----------------------------------------------------------------------- */
/* Performa Zona AM — hanya regional manager                                */
/* ----------------------------------------------------------------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PanelPerformaZona(r: RingkasanArea) {
    var zonaTerpilih by remember { mutableStateOf<String?>(null) }
    var pencarian by remember { mutableStateOf("") }
    var menuTerbuka by remember { mutableStateOf(false) }

    val zonaAktif = r.zona.find { it.zona == zonaTerpilih }

    KartuPanel {
        KartuRingkasZona(
            label = "Total Region Omzet",
            nilai = rupiah(r.totalOmzetSemuaZona),
            keterangan = "${r.zona.size} Zona • ${r.jumlahOutletDalamZona} Cabang Aktif",
            ikon = Icons.Default.WorkspacePremium,
            warna = Color(0xFFD97706),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        KartuRingkasZona(
            label = "Zona Tertinggi (Rank #1)",
            nilai = r.zonaTertinggi?.zona ?: "-",
            keterangan = r.zonaTertinggi?.let { zona ->
                val kontribusi = if (r.totalOmzetSemuaZona > 0) {
                    zona.totalOmzet.toDouble() / r.totalOmzetSemuaZona * 100
                } else 0.0
                "${rupiah(zona.totalOmzet)} • ${String.format(java.util.Locale.US, "%.1f", kontribusi)}% kontribusi"
            } ?: "Rp 0",
            ikon = Icons.Default.LocalFireDepartment,
            warna = Color(0xFF059669),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        KartuRingkasZona(
            label = "Rata-rata / Zona",
            nilai = rupiah(r.rataRataOmzetPerZona),
            keterangan = "Tolok ukur rata-rata wilayah",
            ikon = Icons.Default.BarChart,
            warna = Color(0xFF2563EB),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).background(SukaOrange, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.BarChart, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Performa Zona AM", color = SukaBrown, fontSize = 16.sp, fontWeight = FontWeight.Black)
                Text(
                    "Monitoring omzet aktual & kontribusi per-area manager",
                    color = SukaGray400,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = pencarian,
            onValueChange = { pencarian = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    if (zonaTerpilih == null) "Cari zona atau outlet..." else "Cari outlet...",
                    fontSize = 12.sp,
                )
            },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = SukaGray400, modifier = Modifier.size(18.dp)) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
        )
        Spacer(Modifier.height(8.dp))

        Box {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, SukaBrown.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth().clickable { menuTerbuka = true },
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        zonaAktif?.let { "Zona ${it.zona} (${it.outlets.size} Outlet)" }
                            ?: "Semua Zona (${r.zona.size} Area)",
                        Modifier.weight(1f),
                        color = SukaBrown,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Icon(Icons.Default.ArrowDropDown, null, tint = SukaBrown)
                }
            }
            DropdownMenu(expanded = menuTerbuka, onDismissRequest = { menuTerbuka = false }) {
                DropdownMenuItem(
                    text = { Text("Semua Zona (${r.zona.size} Area)", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    onClick = { zonaTerpilih = null; pencarian = ""; menuTerbuka = false },
                )
                r.zona.forEach { zona ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Zona ${zona.zona} (${zona.outlets.size} Outlet)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        },
                        onClick = { zonaTerpilih = zona.zona; pencarian = ""; menuTerbuka = false },
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        if (zonaAktif == null) {
            DaftarZona(r, pencarian) { zonaTerpilih = it; pencarian = "" }
        } else {
            DaftarOutletZona(zonaAktif, pencarian)
        }
    }
}

@Composable
private fun DaftarZona(r: RingkasanArea, pencarian: String, onPilih: (String) -> Unit) {
    val kueri = pencarian.trim().lowercase()
    val terlihat = if (kueri.isEmpty()) r.zona else r.zona.filter { zona ->
        zona.zona.lowercase().contains(kueri) ||
            zona.outlets.any { it.nama.lowercase().contains(kueri) }
    }

    if (terlihat.isEmpty()) {
        PanelKosong("Pencarian tidak menemukan data zona.")
        return
    }

    terlihat.forEach { zona ->
        val peringkat = r.zona.indexOfFirst { it.zona == zona.zona } + 1
        val kontribusi = if (r.totalOmzetSemuaZona > 0) {
            zona.totalOmzet.toDouble() / r.totalOmzetSemuaZona
        } else 0.0
        BarisZona(zona, peringkat, kontribusi) { onPilih(zona.zona) }
        Spacer(Modifier.height(10.dp))
    }

    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SukaBrown.copy(alpha = 0.03f),
        border = BorderStroke(1.dp, SukaBrown.copy(alpha = 0.15f)),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "TOTAL REGION (${r.zona.size} ZONA)",
                    color = SukaBrown.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp,
                )
                Text(
                    "${r.jumlahOutletDalamZona} outlet terdaftar",
                    color = SukaBrown.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(rupiah(r.totalOmzetSemuaZona), color = SukaOrange, fontSize = 15.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun BarisZona(zona: PerformaZona, peringkat: Int, kontribusi: Double, onClick: () -> Unit) {
    Surface(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, GarisKartu),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LencanaPeringkat(peringkat, ukuran = 28)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Zona ${zona.zona}",
                        color = SukaBrown,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "${zona.outlets.size} Outlet Aktif",
                        color = SukaBrown.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(rupiah(zona.totalOmzet), color = SukaBrown, fontSize = 15.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = GarisKartu)
            Spacer(Modifier.height(8.dp))
            Row {
                Text(
                    "Kontribusi Wilayah",
                    Modifier.weight(1f),
                    color = SukaGray400,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    "${String.format(java.util.Locale.US, "%.1f", kontribusi * 100)}%",
                    color = SukaBrown,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                )
            }
            Spacer(Modifier.height(5.dp))
            BarProgres(kontribusi.toFloat())
        }
    }
}

@Composable
private fun DaftarOutletZona(zona: PerformaZona, pencarian: String) {
    val kueri = pencarian.trim().lowercase()
    val outlets = zona.outlets
        .sortedByDescending { it.omzet }
        .filter { kueri.isEmpty() || it.nama.lowercase().contains(kueri) }

    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SukaOrange.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, SukaOrange.copy(alpha = 0.20f)),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Detail cabang dalam zona",
                    color = SukaBrown,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.height(3.dp))
                ChipJingga(zona.zona.uppercase())
            }
            Text(rupiah(zona.totalOmzet), color = SukaOrange, fontSize = 14.sp, fontWeight = FontWeight.Black)
        }
    }
    Spacer(Modifier.height(10.dp))

    if (outlets.isEmpty()) {
        PanelKosong("Tidak ada outlet yang cocok dengan pencarian.")
        return
    }

    outlets.forEachIndexed { index, outlet ->
        val porsi = if (zona.totalOmzet > 0) outlet.omzet.toDouble() / zona.totalOmzet else 0.0
        Surface(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(1.dp, GarisKartu),
        ) {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "#${index + 1}",
                        Modifier.width(26.dp),
                        color = SukaBrown.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Icon(
                        Icons.Default.Storefront,
                        null,
                        tint = SukaBrown.copy(alpha = 0.4f),
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        outlet.nama,
                        Modifier.weight(1f),
                        color = SukaBrown,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(rupiah(outlet.omzet), color = SukaBrown, fontSize = 13.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.height(8.dp))
                Row {
                    Text(
                        "Porsi dalam Zona",
                        Modifier.weight(1f),
                        color = SukaBrown.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "${String.format(java.util.Locale.US, "%.1f", porsi * 100)}%",
                        color = SukaBrown.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(5.dp))
                BarProgres(porsi.toFloat(), tinggi = 6)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

/* ----------------------------------------------------------------------- */
/* Ranking outlet                                                           */
/* ----------------------------------------------------------------------- */

@Composable
private fun PanelPeringkat(r: RingkasanArea) {
    var semua by remember { mutableStateOf(false) }

    KartuPanel {
        JudulPanel(
            "Ranking Outlet (Gross Revenue)",
            "${r.peringkat.size} outlet",
        )
        if (r.peringkat.isEmpty()) {
            PanelKosong("Belum ada data omzet untuk periode ini.")
            return@KartuPanel
        }

        val terlihat = if (semua) r.peringkat else r.peringkat.take(6)
        terlihat.forEachIndexed { index, outlet ->
            BarisPeringkat(outlet, index + 1, r.omzetTertinggi)
            Spacer(Modifier.height(12.dp))
        }

        if (r.peringkat.size > 6) {
            Surface(
                Modifier.fillMaxWidth().clickable { semua = !semua },
                shape = RoundedCornerShape(12.dp),
                color = SukaOrange.copy(alpha = 0.10f),
            ) {
                Row(
                    Modifier.padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (semua) "Sembunyikan Ranking" else "Tampilkan Semua (${r.peringkat.size} Outlet)",
                        color = SukaOrange,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        if (semua) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        null,
                        tint = SukaOrange,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun BarisPeringkat(outlet: PeringkatOutlet, peringkat: Int, omzetTertinggi: Long) {
    val rasio = if (omzetTertinggi > 0) outlet.omzet.toFloat() / omzetTertinggi else 0f
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LencanaPeringkat(peringkat)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    outlet.nama,
                    color = SukaBrown,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    outlet.zona,
                    color = SukaBrown.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(rupiah(outlet.omzet), color = SukaBrown, fontSize = 13.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(7.dp))
        // Bar minimum 1,5% supaya outlet beromzet nol tetap terlihat sebagai baris, bukan celah.
        BarProgres(maxOf(rasio, 0.015f), sorot = peringkat <= 3)
    }
}

/* ----------------------------------------------------------------------- */
/* Status outlet                                                            */
/* ----------------------------------------------------------------------- */

@Composable
private fun PanelStatusOutlet(r: RingkasanArea) {
    KartuPanel {
        JudulPanel("Status Outlet", "${r.jumlahCabang} cabang")
        if (r.statusOutlet.isEmpty()) {
            PanelKosong("Tidak ada outlet.")
            return@KartuPanel
        }

        val kelompok = r.statusOutlet.groupBy { it.zona }
        kelompok.forEach { (zona, outlets) ->
            if (kelompok.size > 1) {
                ChipJingga(zona.uppercase())
                Spacer(Modifier.height(10.dp))
            }
            outlets.forEachIndexed { index, outlet ->
                BarisStatusOutlet(outlet)
                if (index < outlets.lastIndex) {
                    HorizontalDivider(color = SukaBrown.copy(alpha = 0.05f))
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun BarisStatusOutlet(outlet: StatusOutlet) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            outlet.nama,
            Modifier.weight(1f),
            color = SukaBrown,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(8.dp))
        if (outlet.jamBuka != null) {
            Surface(
                shape = RoundedCornerShape(50),
                color = HijauLatar,
                border = BorderStroke(1.dp, HijauGaris),
            ) {
                Row(
                    Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(6.dp).background(Color(0xFF10B981), CircleShape))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        "BUKA - ${outlet.jamBuka}",
                        color = HijauTeks,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp,
                    )
                }
            }
        } else {
            Surface(
                shape = RoundedCornerShape(50),
                color = MerahLatar,
                border = BorderStroke(1.dp, MerahGaris),
            ) {
                Text(
                    "TUTUP",
                    Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    color = MerahTeks,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp,
                )
            }
        }
    }
}
