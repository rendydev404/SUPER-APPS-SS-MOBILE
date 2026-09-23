package com.sukashawarma.superapp.feature.manager.ui.overview

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.WorkspacePremium
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.RealtimeTables
import com.sukashawarma.superapp.core.ui.SukaDropdownHeader
import com.sukashawarma.superapp.core.ui.SukaDropdownMenu
import com.sukashawarma.superapp.core.ui.SukaDropdownMenuItem
import com.sukashawarma.superapp.core.ui.SukaFilterDropdown
import com.sukashawarma.superapp.core.ui.ios.BilahJudulIos
import com.sukashawarma.superapp.core.ui.ios.IkonBulatIos
import com.sukashawarma.superapp.core.ui.ios.KolomCariIos
import com.sukashawarma.superapp.core.ui.ios.LabelSeksiIos
import com.sukashawarma.superapp.core.ui.ios.LencanaIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.PanelGalatIos
import com.sukashawarma.superapp.core.ui.ios.PemisahIos
import com.sukashawarma.superapp.core.ui.ios.SegmenIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.TombolBundarIos
import com.sukashawarma.superapp.core.ui.ios.TombolKeduaIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WadahSegmenIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.ios.tekanIos
import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.core.ui.kaca.denganRuangNav
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
import com.sukashawarma.superapp.feature.manager.ui.JudulPanel
import com.sukashawarma.superapp.feature.manager.ui.KartuKpi
import com.sukashawarma.superapp.feature.manager.ui.KartuPanel
import com.sukashawarma.superapp.feature.manager.ui.KartuRingkasZona
import com.sukashawarma.superapp.feature.manager.ui.LencanaPeringkat
import com.sukashawarma.superapp.feature.manager.ui.PanelKosong
import com.sukashawarma.superapp.feature.manager.ui.PilKeterangan
import java.time.Instant
import java.time.ZoneOffset

/** Filter periode yang ditampilkan, urut sama dengan tombol segmented control web. */
private val FILTER = listOf(
    PresetPeriode.HARI_INI to "Hari Ini",
    PresetPeriode.KEMARIN to "Kemarin",
    PresetPeriode.MINGGU to "7 Hari",
    PresetPeriode.BULAN to "30 Hari",
)

/** Emas insentif — satu-satunya warna di luar palet sistem, supaya kartu bonus langsung dikenali. */
private val Emas = Color(0xFFFFB300)

/**
 * Ringkasan Area — layar utama modul Manager, cermin `app/page.tsx` web.
 *
 * Seluruh angka di sini bergerak sendiri: [RealtimeRefresh] memuat ulang begitu
 * salah satu tabel sumbernya berubah di server, jadi omzet bertambah saat kasir
 * menutup pesanan tanpa siapa pun menarik layar.
 */
@Composable
fun OverviewScreen(
    onExit: () -> Unit,
    onBukaWaste: () -> Unit,
    viewModel: OverviewViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()

    RealtimeRefresh(
        RealtimeTables.ORDERS,
        RealtimeTables.ORDER_ITEMS,
        RealtimeTables.ATTENDANCE,
        RealtimeTables.OUTLETS,
        RealtimeTables.WASTE_REPORTS,
    ) { viewModel.segarkanDariRealtime() }

    Scaffold(
        containerColor = WarnaIos.Latar,
        topBar = {
            BilahJudulIos(
                "Ringkasan Area",
                onKembali = onExit,
                aksi = { TombolBundarIos(IkonIos.Refresh, "Muat ulang", viewModel::muatUlang) },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = UkuranIos.TepiLayar, vertical = 12.dp).denganRuangNav(),
            verticalArrangement = Arrangement.spacedBy(UkuranIos.JarakKartu),
        ) {
            item { PanelPeriode(state, viewModel) }

            if (state.galat != null) {
                item { PanelGalatIos(state.galat!!, viewModel::muatUlang) }
            }

            item { KartuBonus(state.ringkasan) }
            item { KartuOmzet(state.ringkasan) }
            item { KartuTransaksi(state.ringkasan) }
            item { KartuPorsi(state.ringkasan) }
            item { KartuWaste(state.ringkasan, onBukaWaste) }

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
            Text("Ringkasan Area", Modifier.weight(1f, fill = false), style = TipeIos.Judul2, maxLines = 1)
            Spacer(Modifier.width(8.dp))
            ChipJingga("User: ${labelRole(state.role?.value)}")
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Pantau performa pendapatan, transaksi, dan aktivitas cabang secara real-time",
            style = TipeIos.Catatan,
        )
        Spacer(Modifier.height(14.dp))

        WadahSegmenIos(gulir = true) {
            FILTER.forEach { (preset, label) ->
                SegmenIos(
                    label = label,
                    aktif = !state.memakaiKustom && state.preset == preset,
                    onKlik = { viewModel.pilihPreset(preset) },
                )
            }
            SegmenIos(
                label = state.kustom?.let { "${it.dari} - ${it.sampai}" } ?: "Kustom",
                aktif = state.memakaiKustom,
                ikon = IkonIos.CalendarMonth,
                onKlik = { dialogTanggal = true },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogRentangTanggal(
    onTutup: () -> Unit,
    onPilih: (java.time.LocalDate, java.time.LocalDate) -> Unit,
) {
    val picker = rememberDateRangePickerState()
    DatePickerDialog(
        onDismissRequest = onTutup,
        shape = UkuranIos.SudutKartu,
        colors = DatePickerDefaults.colors(containerColor = WarnaIos.Kartu),
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
            ) { Text("Terapkan", color = WarnaIos.Aksen, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onTutup) { Text("Batal", color = WarnaIos.Aksen) }
        },
    ) {
        DateRangePicker(state = picker, title = {
            Text(
                "Pilih Rentang Tanggal",
                Modifier.padding(start = 24.dp, top = 16.dp),
                style = TipeIos.Utama,
            )
        })
    }
}

/** Pemilih tanggal bekerja di UTC; tanggal yang dipilih pengguna dibaca apa adanya. */
private fun tanggalDariMillis(millis: Long): java.time.LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

/** "regional_manager" → "Regional Manager": lencana iOS tidak memakai huruf kapital semua. */
private fun labelRole(role: String?): String =
    (role ?: "regional_manager").split('_').joinToString(" ") { kata ->
        kata.replaceFirstChar { it.uppercaseChar() }
    }

/* ----------------------------------------------------------------------- */
/* Lima kartu KPI                                                          */
/* ----------------------------------------------------------------------- */

@Composable
private fun KartuOmzet(r: RingkasanArea) = KartuKpi(
    judul = "Gross Revenue",
    nilai = rupiah(r.omzet),
    ikon = IkonIos.TrendingUp,
    warnaIkon = WarnaIos.Aksen,
) { BadgePerubahan(r.perubahanOmzet) }

@Composable
private fun KartuTransaksi(r: RingkasanArea) = KartuKpi(
    judul = "Jumlah Transaksi",
    nilai = cacah(r.jumlahTransaksi),
    satuan = "order",
    ikon = IkonIos.Schedule,
    warnaIkon = WarnaIos.Oranye,
) { PilKeterangan("Selesai pada periode ini") }

@Composable
private fun KartuPorsi(r: RingkasanArea) = KartuKpi(
    judul = "Jumlah Item Terjual",
    nilai = cacah(r.jumlahItem),
    satuan = "porsi",
    ikon = IkonIos.Checklist,
    warnaIkon = WarnaIos.Hijau,
) { PilKeterangan("Total produk pada periode ini") }

@Composable
private fun KartuWaste(r: RingkasanArea, onBuka: () -> Unit) = KartuKpi(
    judul = "Kerugian Waste",
    nilai = rupiah(r.kerugianWaste),
    ikon = IkonIos.Delete,
    warnaIkon = WarnaIos.Merah,
    warnaNilai = NadaIos.BAHAYA.teks,
) {
    if (r.wasteMenungguPersetujuan > 0) {
        LencanaIos(
            "${r.wasteMenungguPersetujuan} butuh approval",
            NadaIos.PERINGATAN,
            Modifier.tekanIos(onBuka),
        )
    } else {
        LencanaIos(
            "Lihat detail waste →",
            NadaIos.NETRAL,
            Modifier.tekanIos(onBuka),
            titik = false,
        )
    }
}

@Composable
private fun KartuBonus(r: RingkasanArea) {
    KartuPanel {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IkonBulatIos(Icons.Default.WorkspacePremium, Emas)
            Spacer(Modifier.width(10.dp))
            Text(
                "Estimasi Insentif",
                Modifier.weight(1f),
                style = TipeIos.SubJudul.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            LencanaIos("Bonus", NadaIos.PERINGATAN, titik = false)
        }
        Spacer(Modifier.height(10.dp))
        Text(rupiah(r.estimasiBonus), style = TipeIos.AngkaBesar, maxLines = 1)
        Spacer(Modifier.height(10.dp))
        BadgePerubahan(r.perubahanBonus)
    }
}

/* ----------------------------------------------------------------------- */
/* Performa Zona AM — hanya regional manager                                */
/* ----------------------------------------------------------------------- */

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
            warna = Emas,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
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
            warna = WarnaIos.Hijau,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        KartuRingkasZona(
            label = "Rata-rata / Zona",
            nilai = rupiah(r.rataRataOmzetPerZona),
            keterangan = "Tolok ukur rata-rata wilayah",
            ikon = IkonIos.BarChart,
            warna = WarnaIos.Biru,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IkonBulatIos(IkonIos.BarChart, WarnaIos.Aksen, ukuran = 36.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Performa Zona AM", style = TipeIos.Utama)
                Text("Monitoring omzet aktual & kontribusi per-area manager", style = TipeIos.Catatan)
            }
        }
        Spacer(Modifier.height(12.dp))

        KolomCariIos(
            nilai = pencarian,
            onUbah = { pencarian = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = if (zonaTerpilih == null) "Cari zona atau outlet..." else "Cari outlet...",
        )
        Spacer(Modifier.height(8.dp))

        Box {
            SukaFilterDropdown(
                label = "",
                value = zonaAktif?.let { "Zona ${it.zona} (${it.outlets.size} Outlet)" }
                    ?: "Semua Zona (${r.zona.size} Area)",
                expanded = menuTerbuka,
                onClick = { menuTerbuka = true },
                modifier = Modifier.fillMaxWidth(),
            )
            SukaDropdownMenu(expanded = menuTerbuka, onDismissRequest = { menuTerbuka = false }) {
                SukaDropdownHeader(title = "PILIH ZONA", onClose = { menuTerbuka = false })
                SukaDropdownMenuItem(
                    text = "Semua Zona (${r.zona.size} Area)",
                    selected = (zonaTerpilih == null),
                    onClick = { zonaTerpilih = null; pencarian = ""; menuTerbuka = false },
                )
                r.zona.forEach { zona ->
                    SukaDropdownMenuItem(
                        text = "Zona ${zona.zona} (${zona.outlets.size} Outlet)",
                        selected = (zonaTerpilih == zona.zona),
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
        Spacer(Modifier.height(8.dp))
    }

    Row(
        Modifier.fillMaxWidth()
            .clip(UkuranIos.SudutBlok)
            .background(NadaIos.AKSEN.warna.copy(alpha = 0.10f))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Total Region (${r.zona.size} Zona)", style = TipeIos.Catatan.copy(fontWeight = FontWeight.SemiBold, color = WarnaIos.Label))
            Text("${r.jumlahOutletDalamZona} outlet terdaftar", style = TipeIos.Kecil)
        }
        Text(rupiah(r.totalOmzetSemuaZona), style = TipeIos.Utama.copy(color = NadaIos.AKSEN.teks), maxLines = 1)
    }
}

@Composable
private fun BarisZona(zona: PerformaZona, peringkat: Int, kontribusi: Double, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth()
            .tekanIos(onClick)
            .clip(UkuranIos.SudutBlok)
            .background(WarnaIos.Latar)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LencanaPeringkat(peringkat, ukuran = 28)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Zona ${zona.zona}",
                    style = TipeIos.Keterangan.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text("${zona.outlets.size} Outlet Aktif", style = TipeIos.Kecil)
            }
            Text(rupiah(zona.totalOmzet), style = TipeIos.Keterangan.copy(fontWeight = FontWeight.Bold), maxLines = 1)
            Spacer(Modifier.width(4.dp))
            Icon(IkonIos.ChevronRight, null, tint = WarnaIos.LabelKetiga, modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.height(10.dp))
        Row {
            Text("Kontribusi Wilayah", Modifier.weight(1f), style = TipeIos.Kecil)
            Text(
                "${String.format(java.util.Locale.US, "%.1f", kontribusi * 100)}%",
                style = TipeIos.Kecil.copy(color = WarnaIos.Label, fontWeight = FontWeight.SemiBold),
            )
        }
        Spacer(Modifier.height(5.dp))
        BarProgres(kontribusi.toFloat())
    }
}

@Composable
private fun DaftarOutletZona(zona: PerformaZona, pencarian: String) {
    val kueri = pencarian.trim().lowercase()
    val outlets = zona.outlets
        .sortedByDescending { it.omzet }
        .filter { kueri.isEmpty() || it.nama.lowercase().contains(kueri) }

    Row(
        Modifier.fillMaxWidth()
            .clip(UkuranIos.SudutBlok)
            .background(NadaIos.AKSEN.warna.copy(alpha = 0.10f))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Detail cabang dalam zona", style = TipeIos.Catatan.copy(fontWeight = FontWeight.SemiBold, color = WarnaIos.Label))
            Spacer(Modifier.height(4.dp))
            ChipJingga(zona.zona)
        }
        Text(rupiah(zona.totalOmzet), style = TipeIos.Utama.copy(color = NadaIos.AKSEN.teks), maxLines = 1)
    }
    Spacer(Modifier.height(8.dp))

    if (outlets.isEmpty()) {
        PanelKosong("Tidak ada outlet yang cocok dengan pencarian.")
        return
    }

    outlets.forEachIndexed { index, outlet ->
        val porsi = if (zona.totalOmzet > 0) outlet.omzet.toDouble() / zona.totalOmzet else 0.0
        Column(
            Modifier.fillMaxWidth()
                .clip(UkuranIos.SudutBlok)
                .background(WarnaIos.Latar)
                .padding(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "#${index + 1}",
                    Modifier.width(28.dp),
                    style = TipeIos.Kecil.copy(fontWeight = FontWeight.SemiBold),
                )
                Icon(IkonIos.Storefront, null, tint = WarnaIos.Abu, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    outlet.nama,
                    Modifier.weight(1f),
                    style = TipeIos.SubJudul.copy(color = WarnaIos.Label, fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(rupiah(outlet.omzet), style = TipeIos.SubJudul.copy(color = WarnaIos.Label, fontWeight = FontWeight.Bold), maxLines = 1)
            }
            Spacer(Modifier.height(8.dp))
            Row {
                Text("Porsi dalam Zona", Modifier.weight(1f), style = TipeIos.Kecil)
                Text("${String.format(java.util.Locale.US, "%.1f", porsi * 100)}%", style = TipeIos.Kecil)
            }
            Spacer(Modifier.height(5.dp))
            BarProgres(porsi.toFloat(), tinggi = 6)
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
            TombolKeduaIos(
                if (semua) "Sembunyikan Ranking" else "Tampilkan Semua (${r.peringkat.size} Outlet)",
                onKlik = { semua = !semua },
                ikon = if (semua) IkonIos.ExpandLess else IkonIos.ExpandMore,
            )
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
                    style = TipeIos.SubJudul.copy(color = WarnaIos.Label, fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(outlet.zona, style = TipeIos.Kecil)
            }
            Spacer(Modifier.width(8.dp))
            Text(rupiah(outlet.omzet), style = TipeIos.SubJudul.copy(color = WarnaIos.Label, fontWeight = FontWeight.Bold), maxLines = 1)
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
        kelompok.entries.forEachIndexed { urutan, (zona, outlets) ->
            if (kelompok.size > 1) {
                LabelSeksiIos(zona, Modifier.padding(top = if (urutan > 0) 12.dp else 0.dp, bottom = 2.dp))
            }
            outlets.forEachIndexed { index, outlet ->
                BarisStatusOutlet(outlet)
                if (index < outlets.lastIndex) PemisahIos(inset = 0.dp)
            }
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
            style = TipeIos.Keterangan,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(8.dp))
        if (outlet.jamBuka != null) {
            LencanaIos("Buka - ${outlet.jamBuka}", NadaIos.SUKSES)
        } else {
            LencanaIos("Tutup", NadaIos.BAHAYA)
        }
    }
}
