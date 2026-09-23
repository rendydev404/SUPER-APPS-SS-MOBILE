package com.sukashawarma.superapp.feature.manager.ui.monitoring

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.RealtimeTables
import com.sukashawarma.superapp.core.ui.SukaDropdownHeader
import com.sukashawarma.superapp.core.ui.SukaDropdownMenu
import com.sukashawarma.superapp.core.ui.SukaDropdownMenuItem
import com.sukashawarma.superapp.core.ui.SukaFilterDropdown
import com.sukashawarma.superapp.core.ui.ios.BilahJudulIos
import com.sukashawarma.superapp.core.ui.ios.IkonBulatIos
import com.sukashawarma.superapp.core.ui.ios.LabelSeksiIos
import com.sukashawarma.superapp.core.ui.ios.LencanaIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.PanelGalatIos
import com.sukashawarma.superapp.core.ui.ios.PemisahIos
import com.sukashawarma.superapp.core.ui.ios.SegmenIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.TombolBundarIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WadahSegmenIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.core.ui.kaca.denganRuangNav
import com.sukashawarma.superapp.feature.manager.domain.FilterKru
import com.sukashawarma.superapp.feature.manager.domain.FilterStatusPos
import com.sukashawarma.superapp.feature.manager.domain.KartuMonitoring
import com.sukashawarma.superapp.feature.manager.domain.KruMonitoring
import com.sukashawarma.superapp.feature.manager.domain.PresetPeriode
import com.sukashawarma.superapp.feature.manager.domain.StatusAbsen
import com.sukashawarma.superapp.feature.manager.domain.StatusPos
import com.sukashawarma.superapp.feature.manager.domain.tanggalPanjangIndonesia
import com.sukashawarma.superapp.feature.manager.ui.KartuPanel
import com.sukashawarma.superapp.feature.manager.ui.PanelKosong
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private val FILTER_PERIODE = listOf(
    PresetPeriode.KEMARIN to "Kemarin",
    PresetPeriode.HARI_INI to "Hari Ini",
    PresetPeriode.MINGGU to "7 Hari",
    PresetPeriode.BULAN to "30 Hari",
)

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
    ) { viewModel.muatUlang(silent = true) }

    Scaffold(
        containerColor = WarnaIos.Latar,
        topBar = {
            BilahJudulIos(
                judul = "Tim / Kru",
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
            item { PanelPenyaring(state, viewModel) }
            if (state.galat != null) {
                item { PanelGalatIos(state.galat!!, viewModel::muatUlang) }
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
        Text("Monitoring Aktivitas", style = TipeIos.Judul3)
        Spacer(Modifier.height(2.dp))
        Text(
            "Pantau status POS, absensi kru, jam opname, dan waktu tutup shift secara real-time",
            style = TipeIos.Catatan,
        )

        Spacer(Modifier.height(14.dp))
        // Periode dipilih lewat segmented control yang bisa digeser: label rentang
        // kustom bisa lebih panjang dari sisa lebar layar.
        WadahSegmenIos(gulir = true) {
            FILTER_PERIODE.forEach { (preset, label) ->
                SegmenIos(label, !state.memakaiKustom && state.preset == preset, {
                    viewModel.pilihPreset(preset)
                })
            }
            SegmenIos(
                state.kustom?.let { "${it.dari} - ${it.sampai}" } ?: "Kustom",
                state.memakaiKustom,
                { dialogTanggal = true },
                ikon = IkonIos.CalendarMonth,
            )
        }

        Spacer(Modifier.height(10.dp))
        PilihanTurun(
            ikon = IkonIos.Storefront,
            nilai = state.namaOutletTerpilih ?: "Semua Outlet",
            pilihan = listOf<Pair<String?, String>>(null to "Semua Outlet") +
                state.daftarOutlet.map { it.id to it.nama },
            onPilih = viewModel::pilihOutlet,
        )
        Spacer(Modifier.height(8.dp))
        PilihanTurun(
            ikon = IkonIos.Lock,
            nilai = state.filterPos.label,
            pilihan = FilterStatusPos.entries.map { it to it.label },
            onPilih = viewModel::pilihFilterPos,
        )
        Spacer(Modifier.height(8.dp))
        PilihanTurun(
            ikon = IkonIos.Groups,
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
private fun <T> PilihanTurun(
    ikon: ImageVector,
    nilai: String,
    pilihan: List<Pair<T, String>>,
    onPilih: (T) -> Unit,
) {
    var terbuka by remember { mutableStateOf(false) }
    Box {
        SukaFilterDropdown(
            label = "",
            value = nilai,
            expanded = terbuka,
            onClick = { terbuka = true },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = ikon,
        )
        SukaDropdownMenu(expanded = terbuka, onDismissRequest = { terbuka = false }) {
            SukaDropdownHeader(title = "PILIH OPSI", onClose = { terbuka = false })
            pilihan.forEach { (nilaiPilihan, label) ->
                SukaDropdownMenuItem(
                    text = label,
                    selected = (label == nilai),
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
    val warna = DatePickerDefaults.colors(
        containerColor = WarnaIos.Kartu,
        selectedDayContainerColor = WarnaIos.Aksen,
        todayDateBorderColor = WarnaIos.Aksen,
        dayInSelectionRangeContainerColor = WarnaIos.Aksen.copy(alpha = 0.14f),
    )
    DatePickerDialog(
        onDismissRequest = onTutup,
        confirmButton = {
            val bisa = picker.selectedStartDateMillis != null
            TextButton(
                onClick = {
                    val dari = picker.selectedStartDateMillis?.let(::tanggalDari)
                    val sampai = picker.selectedEndDateMillis?.let(::tanggalDari)
                    if (dari != null) onPilih(dari, sampai ?: dari)
                },
                enabled = bisa,
            ) {
                Text("Terapkan", color = if (bisa) WarnaIos.Aksen else WarnaIos.Abu, fontWeight = FontWeight.SemiBold)
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
            Row(
                Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LabelSeksiIos(wilayah, Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Text("${kartu.size} kartu", style = TipeIos.Catatan)
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
            IkonBulatIos(
                IkonIos.Storefront,
                if (kartu.jamOpname != null) WarnaIos.Biru else WarnaIos.Abu,
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    kartu.outlet.nama,
                    style = TipeIos.Utama,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (banyakHari) {
                    Text(tanggalPanjangIndonesia(kartu.tanggal), style = TipeIos.Catatan)
                }
            }
            Spacer(Modifier.width(8.dp))
            LencanaOpname(kartu.jamOpname)
        }

        Spacer(Modifier.height(12.dp))
        LencanaStatusPos(kartu.statusPos)

        if (kartu.jamBuka != null || kartu.jamTutup != null || kartu.jamOpname != null) {
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                kartu.jamBuka?.let {
                    PilJam(IkonIos.Schedule, "Buka: $it", NadaIos.SUKSES)
                }
                kartu.jamTutup?.let {
                    PilJam(IkonIos.Lock, "Tutup Shift: $it", NadaIos.NETRAL)
                }
                kartu.jamOpname?.let {
                    PilJam(IkonIos.Inventory2, "Opname: $it", NadaIos.INFO)
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        PemisahIos(inset = 0.dp)
        Spacer(Modifier.height(4.dp))

        if (kartu.kru.isEmpty()) {
            Column(
                Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(IkonIos.Groups, null, tint = WarnaIos.LabelKetiga, modifier = Modifier.size(28.dp))
                Spacer(Modifier.height(6.dp))
                Text("Tidak ada kru aktif pada tanggal ini", style = TipeIos.Catatan)
            }
        } else {
            kartu.kru.forEachIndexed { index, kru ->
                BarisKru(kru)
                if (index < kartu.kru.lastIndex) {
                    // Inset menyamai awal nama, seperti pemisah daftar kontak iOS.
                    PemisahIos(inset = 40.dp)
                }
            }
        }
    }
}

@Composable
private fun LencanaOpname(jam: String?) {
    val sudah = jam != null
    LencanaIos(
        if (sudah) "Opname $jam" else "Belum opname",
        if (sudah) NadaIos.INFO else NadaIos.NETRAL,
    )
}

@Composable
private fun LencanaStatusPos(status: StatusPos) {
    val nada = when (status) {
        StatusPos.Terbuka -> NadaIos.SUKSES
        StatusPos.MenungguAbsen -> NadaIos.BAHAYA
        is StatusPos.ChecklistBelumSelesai -> NadaIos.PERINGATAN
        is StatusPos.Tutup -> NadaIos.NETRAL
    }
    LencanaIos(
        status.label,
        nada,
        ikon = if (status.terbuka) Icons.Default.LockOpen else IkonIos.Lock,
    )
}

@Composable
private fun PilJam(ikon: ImageVector, teks: String, nada: NadaIos) {
    LencanaIos(teks, nada, ikon = ikon)
}

@Composable
private fun BarisKru(kru: KruMonitoring) {
    val nada = nadaStatusAbsen(kru.status)
    Row(
        Modifier.fillMaxWidth().padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(30.dp).clip(CircleShape).background(WarnaIos.Indigo.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(IkonIos.Person, null, tint = WarnaIos.Indigo, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                kru.nama,
                style = TipeIos.Keterangan.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                // Role mentah dari DB huruf kecil ("leader"); dikapitalkan huruf
                // pertamanya saja, bukan seluruhnya, supaya tidak berteriak.
                kru.role.lowercase().replaceFirstChar { it.titlecase() },
                style = TipeIos.Kecil.copy(color = NadaIos.AKSEN.teks, fontWeight = FontWeight.Medium),
                maxLines = 1,
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            LencanaIos(kru.status.label, nada)
            if (kru.jam.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(kru.jam, style = TipeIos.Kecil)
            }
        }
    }
}

private fun nadaStatusAbsen(status: StatusAbsen): NadaIos = when (status) {
    StatusAbsen.Hadir -> NadaIos.SUKSES
    StatusAbsen.Pulang -> NadaIos.NETRAL
    StatusAbsen.BelumAbsen -> NadaIos.BAHAYA
    // Absen di cabang lain diberi warna sendiri supaya tidak tertukar dengan hadir
    // di outlet ini — itu justru keadaan yang perlu diperhatikan manajer.
    is StatusAbsen.HadirDiOutletLain -> NadaIos.INFO
    is StatusAbsen.PulangDiOutletLain -> NadaIos.NETRAL
}
