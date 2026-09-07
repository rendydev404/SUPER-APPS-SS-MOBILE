package com.sukashawarma.superapp.feature.manager.ui.monitoring

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.RealtimeTables
import com.sukashawarma.superapp.feature.manager.domain.FilterKru
import com.sukashawarma.superapp.feature.manager.domain.FilterStatusPos
import com.sukashawarma.superapp.feature.manager.domain.KartuMonitoring
import com.sukashawarma.superapp.feature.manager.domain.KruMonitoring
import com.sukashawarma.superapp.feature.manager.domain.PresetPeriode
import com.sukashawarma.superapp.feature.manager.domain.StatusAbsen
import com.sukashawarma.superapp.feature.manager.domain.StatusPos
import com.sukashawarma.superapp.feature.manager.domain.tanggalPanjangIndonesia
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

private val FILTER_PERIODE = listOf(
    PresetPeriode.KEMARIN to "Kemarin",
    PresetPeriode.HARI_INI to "Hari Ini",
    PresetPeriode.MINGGU to "7 Hari",
    PresetPeriode.BULAN to "30 Hari",
)

private val BIRU_LATAR = Color(0xFFDBEAFE)
private val BIRU_GARIS = Color(0xFFBFDBFE)
private val BIRU_TEKS = Color(0xFF1E40AF)
private val ABU_LATAR = Color(0xFFF1F5F9)
private val ABU_GARIS = Color(0xFFE2E8F0)
private val ABU_TEKS = Color(0xFF334155)
private val JINGGA_LATAR = Color(0xFFFFEDD5)
private val JINGGA_GARIS = Color(0xFFFDBA74)
private val JINGGA_TEKS = Color(0xFF9A3412)

/**
 * Monitoring Aktivitas — cermin tab "POS & Crew" di `app/team/` web.
 *
 * Tab "Live Location" versi web belum ada di sini: ia bersandar pada Supabase
 * Realtime Presence (channel `room:crew_location`) yang belum didukung
 * `core:network`, dan pada peta yang belum menjadi dependensi aplikasi ini.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitoringScreen(
    onExit: () -> Unit,
    viewModel: MonitoringViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()

    RealtimeRefresh(
        RealtimeTables.ATTENDANCE,
        RealtimeTables.CHECKLIST_TICKS,
        RealtimeTables.CHECKLIST_RECORDS,
        RealtimeTables.OPNAME,
        RealtimeTables.OUTLET_STAFF,
    ) { viewModel.muatUlang() }

    Scaffold(
        containerColor = SukaCream,
        topBar = {
            TopAppBar(
                title = { Text("Tim / Kru", fontWeight = FontWeight.Black, fontSize = 17.sp, color = SukaBrown, maxLines = 1) },
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
            item { PanelPenyaring(state, viewModel) }
            if (state.galat != null) {
                item { PanelGalat(state.galat!!, viewModel::muatUlang) }
            }
            daftarKartu(state)
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun PanelPenyaring(state: MonitoringUiState, viewModel: MonitoringViewModel) {
    var dialogTanggal by remember { mutableStateOf(false) }

    KartuPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Monitoring Aktivitas", color = SukaBrown, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(3.dp))
                Text(
                    "Pantau status POS, absensi kru, jam opname, dan waktu tutup shift secara real-time",
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

        Spacer(Modifier.height(10.dp))
        PilihanTurun(
            ikon = Icons.Default.Storefront,
            nilai = state.namaOutletTerpilih ?: "Semua Outlet",
            pilihan = listOf<Pair<String?, String>>(null to "Semua Outlet") +
                state.daftarOutlet.map { it.id to it.nama },
            onPilih = viewModel::pilihOutlet,
        )
        Spacer(Modifier.height(8.dp))
        PilihanTurun(
            ikon = Icons.Default.Lock,
            nilai = state.filterPos.label,
            pilihan = FilterStatusPos.entries.map { it to it.label },
            onPilih = viewModel::pilihFilterPos,
        )
        Spacer(Modifier.height(8.dp))
        PilihanTurun(
            ikon = Icons.Default.Groups,
            nilai = state.filterKru.label,
            pilihan = FilterKru.entries.map { it to it.label },
            onPilih = viewModel::pilihFilterKru,
        )
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

@Composable
private fun <T> PilihanTurun(
    ikon: ImageVector,
    nilai: String,
    pilihan: List<Pair<T, String>>,
    onPilih: (T) -> Unit,
) {
    var terbuka by remember { mutableStateOf(false) }
    Box {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            border = BorderStroke(1.dp, SukaBrown.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth().clickable { terbuka = true },
        ) {
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(ikon, null, tint = SukaOrange, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    nilai,
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
        DropdownMenu(expanded = terbuka, onDismissRequest = { terbuka = false }) {
            pilihan.forEach { (nilaiPilihan, label) ->
                DropdownMenuItem(
                    text = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    onClick = { terbuka = false; onPilih(nilaiPilihan) },
                )
            }
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
private fun PanelGalat(pesan: String, onCoba: () -> Unit) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MerahLatar,
        border = BorderStroke(1.dp, MerahGaris),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(pesan, Modifier.weight(1f), color = MerahTeks, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = onCoba) {
                Text("Coba Lagi", color = MerahTeks, fontWeight = FontWeight.Black, fontSize = 12.sp)
            }
        }
    }
}

private fun LazyListScope.daftarKartu(state: MonitoringUiState) {
    val perWilayah = state.kartuPerWilayah
    if (perWilayah.isEmpty()) {
        item {
            KartuPanel {
                PanelKosong(
                    if (state.memuat) {
                        "Memuat papan monitoring..."
                    } else {
                        "Tidak ada outlet yang cocok dengan penyaring saat ini."
                    }
                )
            }
        }
        return
    }

    perWilayah.forEach { (wilayah, kartu) ->
        item(key = "wilayah-$wilayah") {
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    wilayah.uppercase(),
                    color = SukaBrown.copy(alpha = 0.55f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp,
                )
                Spacer(Modifier.width(8.dp))
                HorizontalDivider(Modifier.weight(1f), color = GarisKartu)
                Spacer(Modifier.width(8.dp))
                Text(
                    "${kartu.size} kartu",
                    color = SukaGray400,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        kartu.forEach { isi ->
            item(key = "${isi.outlet.id}-${isi.tanggal}") {
                KartuOutlet(isi, state.banyakHari)
            }
        }
    }
}

@Composable
private fun KartuOutlet(kartu: KartuMonitoring, banyakHari: Boolean) {
    KartuPanel {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                Icons.Default.Storefront,
                null,
                tint = if (kartu.jamOpname != null) Color(0xFF3B82F6) else SukaGray400,
                modifier = Modifier.size(19.dp),
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    kartu.outlet.nama,
                    color = SukaBrown,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 19.sp,
                )
                if (banyakHari) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        tanggalPanjangIndonesia(kartu.tanggal),
                        color = SukaGray400,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            LencanaOpname(kartu.jamOpname)
        }

        Spacer(Modifier.height(12.dp))
        LencanaStatusPos(kartu.statusPos)

        if (kartu.jamBuka != null || kartu.jamTutup != null || kartu.jamOpname != null) {
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                kartu.jamBuka?.let {
                    PilJam(Icons.Default.Schedule, "Buka: $it", HijauLatar, HijauGaris, HijauTeks)
                }
                kartu.jamTutup?.let {
                    PilJam(Icons.Default.Lock, "Tutup Shift: $it", ABU_LATAR, ABU_GARIS, ABU_TEKS)
                }
                kartu.jamOpname?.let {
                    PilJam(Icons.Default.Inventory2, "Opname: $it", BIRU_LATAR, BIRU_GARIS, BIRU_TEKS)
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = GarisKartu)
        Spacer(Modifier.height(8.dp))

        if (kartu.kru.isEmpty()) {
            Column(
                Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Default.Groups, null, tint = GarisKartu, modifier = Modifier.size(28.dp))
                Spacer(Modifier.height(6.dp))
                Text(
                    "Tidak ada kru aktif pada tanggal ini",
                    color = SukaGray400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        } else {
            kartu.kru.forEachIndexed { index, kru ->
                BarisKru(kru)
                if (index < kartu.kru.lastIndex) {
                    HorizontalDivider(color = SukaBrown.copy(alpha = 0.04f))
                }
            }
        }
    }
}

@Composable
private fun LencanaOpname(jam: String?) {
    val sudah = jam != null
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (sudah) BIRU_LATAR else ABU_LATAR,
        border = BorderStroke(1.dp, if (sudah) BIRU_GARIS else ABU_GARIS),
    ) {
        Text(
            if (sudah) "OPNAME $jam" else "BELUM OPNAME",
            Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            color = if (sudah) BIRU_TEKS else SukaGray400,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.4.sp,
        )
    }
}

@Composable
private fun LencanaStatusPos(status: StatusPos) {
    val (latar, garis, teks) = when (status) {
        StatusPos.Terbuka -> Triple(HijauLatar, HijauGaris, HijauTeks)
        StatusPos.MenungguAbsen -> Triple(MerahLatar, MerahGaris, MerahTeks)
        is StatusPos.ChecklistBelumSelesai -> Triple(JINGGA_LATAR, JINGGA_GARIS, JINGGA_TEKS)
        is StatusPos.Tutup -> Triple(ABU_LATAR, ABU_GARIS, ABU_TEKS)
    }
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = latar,
        border = BorderStroke(1.dp, garis),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (status.terbuka) Icons.Default.LockOpen else Icons.Default.Lock,
                null,
                tint = teks,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(7.dp))
            Text(
                status.label,
                color = teks,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PilJam(ikon: ImageVector, teks: String, latar: Color, garis: Color, warna: Color) {
    Surface(shape = RoundedCornerShape(8.dp), color = latar, border = BorderStroke(1.dp, garis)) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(ikon, null, tint = warna, modifier = Modifier.size(11.dp))
            Spacer(Modifier.width(4.dp))
            Text(teks, color = warna, fontSize = 10.sp, fontWeight = FontWeight.Black, maxLines = 1)
        }
    }
}

@Composable
private fun BarisKru(kru: KruMonitoring) {
    val (latar, garis, teks, titik) = warnaStatusAbsen(kru.status)
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(28.dp).background(Color(0xFFEEF2FF), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Person, null, tint = Color(0xFF6366F1), modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(
                kru.nama,
                color = SukaBrown,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                kru.role.uppercase(),
                color = SukaOrange,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.4.sp,
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Surface(shape = RoundedCornerShape(6.dp), color = latar, border = BorderStroke(1.dp, garis)) {
                Row(
                    Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(5.dp).background(titik, CircleShape))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        kru.status.label.uppercase(),
                        color = teks,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                    )
                }
            }
            if (kru.jam.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(kru.jam, color = SukaGray400, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private data class WarnaStatus(
    val latar: Color,
    val garis: Color,
    val teks: Color,
    val titik: Color,
)

private fun warnaStatusAbsen(status: StatusAbsen): WarnaStatus = when (status) {
    StatusAbsen.Hadir -> WarnaStatus(HijauLatar, HijauGaris, HijauTeks, Color(0xFF10B981))
    StatusAbsen.Pulang -> WarnaStatus(ABU_LATAR, ABU_GARIS, ABU_TEKS, Color(0xFF94A3B8))
    StatusAbsen.BelumAbsen -> WarnaStatus(MerahLatar, MerahGaris, MerahTeks, Color(0xFFEF4444))
    // Absen di cabang lain diberi warna sendiri supaya tidak tertukar dengan hadir
    // di outlet ini — itu justru keadaan yang perlu diperhatikan manajer.
    is StatusAbsen.HadirDiOutletLain -> WarnaStatus(BIRU_LATAR, BIRU_GARIS, BIRU_TEKS, Color(0xFF3B82F6))
    is StatusAbsen.PulangDiOutletLain -> WarnaStatus(ABU_LATAR, ABU_GARIS, ABU_TEKS, Color(0xFF94A3B8))
}
