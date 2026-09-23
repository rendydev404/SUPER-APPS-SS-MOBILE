package com.sukashawarma.superapp.presentation.absensi.rekap

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.sukashawarma.superapp.core.ui.ios.BilahJudulIos
import com.sukashawarma.superapp.core.ui.ios.KartuIos
import com.sukashawarma.superapp.core.ui.ios.KeadaanIos
import com.sukashawarma.superapp.core.ui.ios.KolomCariIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.PemisahIos
import com.sukashawarma.superapp.core.ui.ios.SegmenIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.TombolBundarIos
import com.sukashawarma.superapp.core.ui.ios.TombolKapsulIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WadahSegmenIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.ios.permukaanIos
import com.sukashawarma.superapp.core.ui.ios.tekanIos
import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.domain.util.JakartaTime
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.RealtimeTables
import com.sukashawarma.superapp.core.ui.SukaDropdownHeader
import com.sukashawarma.superapp.core.ui.SukaDropdownMenu
import com.sukashawarma.superapp.core.ui.SukaDropdownMenuItem

/** Teks kuning gelap untuk "telat toleransi" — NadaIos tidak punya nada kuning. */
private val TeksKuning = Color(0xFF946200)

private val ID_LOCALE = Locale("id", "ID")
private val ShortDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", ID_LOCALE)

/** Warna pil status — dipetakan dari `status` attendance seperti StatusPill di web. */
private fun statusColors(status: String): Pair<Color, Color> = when (status) {
    "tepat" -> NadaIos.SUKSES.pil()
    "telat", "pulang_telat" -> NadaIos.PERINGATAN.pil()
    "telat_toleransi" -> TeksKuning to WarnaIos.Kuning.copy(alpha = 0.18f)
    "alpha" -> NadaIos.BAHAYA.pil()
    "lebih_awal" -> NadaIos.INFO.pil()
    else -> NadaIos.NETRAL.pil()
}

/** (teks, latar) lencana bernada, sama dengan resep LencanaIos. */
private fun NadaIos.pil(): Pair<Color, Color> = teks to warna.copy(alpha = 0.14f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RekapScreen(onExit: () -> Unit, viewModel: RekapViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    RealtimeRefresh(RealtimeTables.ATTENDANCE) { viewModel.refresh() }
    val context = LocalContext.current

    var selectedStaff by remember { mutableStateOf<StaffSummary?>(null) }
    var previewUrl by remember { mutableStateOf<String?>(null) }
    var pickingCustomStart by remember { mutableStateOf(false) }
    var pickingCustomEnd by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = WarnaIos.Latar,
        topBar = { BilahJudulIos(judul = "Rekap & Riwayat", onKembali = onExit, garisBawah = false) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = UkuranIos.TepiLayar, end = UkuranIos.TepiLayar, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(UkuranIos.JarakKartu),
        ) {
            item(key = "header") {
                RekapHeader(
                    periodLabel = state.periodLabel,
                    outletName = state.selectedOutletName,
                    canExport = state.rows.isNotEmpty(),
                    onExport = { shareCsv(context, viewModel.csvFileName(), viewModel.buildCsv()) },
                )
            }

            if (state.canChooseOutlet) {
                item(key = "outlet_picker") {
                    OutletPickerCard(
                        outlets = state.outlets,
                        loading = state.loadingOutlets,
                        selectedId = state.selectedOutletId,
                        onSelect = { viewModel.selectOutlet(it) },
                    )
                }
            }

            if (state.awaitingOutletChoice) {
                item(key = "choose_outlet") {
                    KeadaanIos(
                        ikon = IkonIos.Storefront,
                        judul = "Pilih Outlet Dulu",
                        pesan = "Anda memantau seluruh outlet. Pilih salah satu di atas untuk melihat rekap kehadirannya.",
                        nada = NadaIos.AKSEN,
                    )
                }
                return@LazyColumn
            }

            item(key = "period") {
                PeriodFilter(
                    selected = state.period,
                    start = state.startDate,
                    end = state.endDate,
                    onSelect = { viewModel.setPeriod(it) },
                    onPickStart = { pickingCustomStart = true },
                    onPickEnd = { pickingCustomEnd = true },
                )
            }

            when {
                state.error != null -> item(key = "error") {
                    RekapErrorCard(message = state.error.orEmpty(), onRetry = { viewModel.load() })
                }

                else -> {
                    item(key = "stats") { GlobalSummaryGrid(state.globalSummary, state.loading) }

                    item(key = "list_header") {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                "Ringkasan Karyawan",
                                style = TipeIos.Judul3.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            StatusFilterMenu(
                                selected = state.statusFilter,
                                onSelect = { viewModel.setStatusFilter(it) },
                            )
                        }
                    }

                    when {
                        state.loading -> item(key = "loading") {
                            Box(
                                Modifier.fillMaxWidth().padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(color = WarnaIos.Aksen, strokeWidth = 2.5.dp, modifier = Modifier.size(28.dp))
                            }
                        }

                        state.summaries.isEmpty() -> item(key = "empty") {
                            KeadaanIos(
                                ikon = IkonIos.Person,
                                judul = "Belum Ada Data",
                                pesan = "Tidak ada aktivitas absensi di periode ini.",
                            )
                        }

                        else -> items(state.summaries, key = { it.staffId }) { staff ->
                            StaffSummaryCard(staff = staff, onClick = { selectedStaff = staff })
                        }
                    }
                }
            }
        }
    }

    selectedStaff?.let { staff ->
        StaffDetailSheet(
            staff = staff,
            days = viewModel.detailByDate(staff),
            onDismiss = { selectedStaff = null },
            onPreview = { previewUrl = it },
        )
    }

    previewUrl?.let { url ->
        PhotoPreviewDialog(url = url, onDismiss = { previewUrl = null })
    }

    if (pickingCustomStart) {
        RekapDatePickerDialog(
            title = "Tanggal mulai",
            initial = state.customStart,
            onDismiss = { pickingCustomStart = false },
            onConfirm = {
                viewModel.setCustomRange(it, state.customEnd)
                pickingCustomStart = false
            },
        )
    }
    if (pickingCustomEnd) {
        RekapDatePickerDialog(
            title = "Tanggal akhir",
            initial = state.customEnd,
            onDismiss = { pickingCustomEnd = false },
            onConfirm = {
                viewModel.setCustomRange(state.customStart, it)
                pickingCustomEnd = false
            },
        )
    }
}

/* ---------------------------------------------------------------- Header */

@Composable
private fun RekapHeader(
    periodLabel: String,
    outletName: String?,
    canExport: Boolean,
    onExport: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                periodLabel,
                style = TipeIos.Judul3.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                outletName ?: "Ringkasan & detail kehadiran per karyawan",
                style = TipeIos.SubJudul,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        // Kapsul aksen ala tombol toolbar iOS; abu dan tak bisa ditekan saat belum ada data.
        Row(
            modifier = Modifier
                .height(34.dp)
                .clip(UkuranIos.SudutKapsul)
                .background(if (canExport) WarnaIos.Aksen else WarnaIos.Isian)
                .tekanIos(onExport, aktif = canExport)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(
                IkonIos.Download,
                contentDescription = "Export CSV",
                tint = if (canExport) Color.White else WarnaIos.Abu,
                modifier = Modifier.size(15.dp),
            )
            Text(
                "CSV",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (canExport) Color.White else WarnaIos.Abu,
            )
        }
    }
}

/* ---------------------------------------------------------- Outlet picker */

@Composable
private fun OutletPickerCard(
    outlets: List<RekapOutletOption>,
    loading: Boolean,
    selectedId: String?,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val selected = outlets.find { it.id == selectedId }
    val filtered = outlets.filter { it.name.contains(query.trim(), ignoreCase = true) }

    LaunchedEffect(expanded) { if (!expanded) query = "" }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .permukaanIos(UkuranIos.SudutGrup)
                .then(
                    // Belum ada outlet terpilih: garis aksen tipis sebagai ajakan memilih.
                    if (selected == null) Modifier.border(1.dp, WarnaIos.Aksen.copy(alpha = 0.45f), UkuranIos.SudutGrup)
                    else Modifier
                )
                .clickable(enabled = !loading && outlets.isNotEmpty()) { expanded = true }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.size(30.dp).clip(CircleShape).background(WarnaIos.Aksen.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(IkonIos.Storefront, contentDescription = null, tint = WarnaIos.Aksen, modifier = Modifier.size(17.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = when {
                        loading -> "Memuat outlet..."
                        selected != null -> selected.name
                        else -> "Pilih outlet"
                    },
                    style = TipeIos.Isi.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (outlets.isEmpty() && !loading) "Tidak ada outlet yang bisa dilihat" else "Ketuk untuk ganti outlet",
                    style = TipeIos.Catatan,
                    maxLines = 1,
                )
            }
            Icon(
                if (expanded) IkonIos.ExpandLess else IkonIos.ExpandMore,
                contentDescription = "Pilih outlet",
                tint = WarnaIos.LabelKetiga,
                modifier = Modifier.size(16.dp),
            )
        }

        SukaDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(290.dp).heightIn(max = 430.dp),
        ) {
            Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                KolomCariIos(
                    nilai = query,
                    onUbah = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "Cari outlet",
                )
                Spacer(Modifier.height(6.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 330.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    if (filtered.isEmpty()) {
                        Text(
                            "Outlet tidak ditemukan",
                            style = TipeIos.Catatan,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 18.dp),
                        )
                    } else filtered.forEach { outlet ->
                        SukaDropdownMenuItem(
                            text = outlet.name,
                            selected = (outlet.id == selectedId),
                            leadingIcon = IkonIos.Storefront,
                            onClick = { onSelect(outlet.id); expanded = false },
                        )
                    }
                }
            }
        }
    }
}

/* ----------------------------------------------------------- Period filter */

@Composable
private fun PeriodFilter(
    selected: RekapPeriod,
    start: LocalDate,
    end: LocalDate,
    onSelect: (RekapPeriod) -> Unit,
    onPickStart: () -> Unit,
    onPickEnd: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        WadahSegmenIos {
            RekapPeriod.entries.forEach { period ->
                SegmenIos(
                    label = period.label,
                    aktif = period == selected,
                    onKlik = { onSelect(period) },
                    modifier = Modifier.weight(1f),
                    jarakSisi = 4.dp,
                )
            }
        }

        if (selected == RekapPeriod.KUSTOM) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DateFieldButton(modifier = Modifier.weight(1f), label = "Dari", date = start, onClick = onPickStart)
                DateFieldButton(modifier = Modifier.weight(1f), label = "Sampai", date = end, onClick = onPickEnd)
            }
        }
    }
}

@Composable
private fun DateFieldButton(modifier: Modifier = Modifier, label: String, date: LocalDate, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .permukaanIos(UkuranIos.SudutGrup)
            .tekanIos(onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(IkonIos.CalendarMonth, contentDescription = null, tint = WarnaIos.Aksen, modifier = Modifier.size(18.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = TipeIos.Kecil)
            Text(
                date.format(ShortDateFormatter),
                style = TipeIos.Keterangan.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RekapDatePickerDialog(
    title: String,
    initial: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    val picked = pickerState.selectedDateMillis?.let {
        Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
    }
    val valid = picked != null && !picked.isAfter(LocalDate.now(JakartaTime.ZONE))

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { picked?.let(onConfirm) }, enabled = valid) {
                Text("Terapkan", color = if (valid) WarnaIos.Aksen else WarnaIos.LabelKetiga, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal", color = WarnaIos.LabelKedua) } },
        colors = DatePickerDefaults.colors(containerColor = WarnaIos.Kartu),
    ) {
        DatePicker(
            state = pickerState,
            title = {
                Text(
                    title,
                    style = TipeIos.Catatan.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp),
                )
            },
            headline = {
                Text(
                    picked?.format(ShortDateFormatter) ?: "-",
                    style = TipeIos.Judul3,
                    modifier = Modifier.padding(start = 24.dp, bottom = 8.dp),
                )
            },
            showModeToggle = false,
            colors = DatePickerDefaults.colors(
                containerColor = WarnaIos.Kartu,
                selectedDayContainerColor = WarnaIos.Aksen,
                todayDateBorderColor = WarnaIos.Aksen,
                todayContentColor = WarnaIos.Aksen,
            ),
        )
    }
}

/* ---------------------------------------------------------- Global summary */

@Composable
private fun GlobalSummaryGrid(summary: RekapGlobalSummary, loading: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(Modifier.weight(1f), "Kehadiran (Masuk)", summary.masuk, loading, Icons.AutoMirrored.Filled.Login, NadaIos.SUKSES)
            StatCard(Modifier.weight(1f), "Terlambat", summary.telat, loading, IkonIos.Schedule, NadaIos.PERINGATAN)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(Modifier.weight(1f), "Alpha / Tidak Hadir", summary.alpha, loading, IkonIos.Close, NadaIos.BAHAYA)
            StatCard(Modifier.weight(1f), "Pulang Cepat", summary.cepat, loading, Icons.AutoMirrored.Filled.Logout, NadaIos.INFO)
        }
    }
}

/** Petak ringkasan ala "smart list" Pengingat iOS; label boleh dua baris supaya tidak terpotong. */
@Composable
private fun StatCard(
    modifier: Modifier,
    label: String,
    value: Int,
    loading: Boolean,
    icon: ImageVector,
    nada: NadaIos,
) {
    Column(
        modifier
            .permukaanIos(UkuranIos.SudutPetak)
            .padding(start = 12.dp, end = 14.dp, top = 12.dp, bottom = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                Modifier.size(30.dp).clip(CircleShape).background(nada.warna),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.weight(1f))
            Text(if (loading) "-" else "$value", style = TipeIos.AngkaBesar, maxLines = 1)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            color = WarnaIos.LabelKedua,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/* ------------------------------------------------------------ Status filter */

@Composable
private fun StatusFilterMenu(selected: RekapStatusFilter, onSelect: (RekapStatusFilter) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        TombolKapsulIos(
            teks = selected.label,
            onKlik = { expanded = true },
            ikon = IkonIos.Tune,
            chevron = true,
        )
        SukaDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SukaDropdownHeader(title = "FILTER STATUS", onClose = { expanded = false })
            RekapStatusFilter.entries.forEach { filter ->
                SukaDropdownMenuItem(
                    text = filter.label,
                    selected = (filter == selected),
                    onClick = { onSelect(filter); expanded = false },
                )
            }
        }
    }
}

/* -------------------------------------------------------------- Staff list */

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StaffSummaryCard(staff: StaffSummary, onClick: () -> Unit) {
    KartuIos(onKlik = onClick, padding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StaffAvatar(name = staff.name, photoPath = staff.latestPhotoPath, size = 46)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    staff.name,
                    style = TipeIos.Utama.copy(fontSize = 16.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    CountText("${staff.totalMasuk} Hadir", NadaIos.SUKSES.teks)
                    if (staff.totalTelat > 0) CountText("${staff.totalTelat} Telat", NadaIos.PERINGATAN.teks)
                    if (staff.totalTelatToleransi > 0) CountText("${staff.totalTelatToleransi} Telat (Tol)", TeksKuning)
                    if (staff.totalAlpha > 0) CountText("${staff.totalAlpha} Alpha", NadaIos.BAHAYA.teks)
                    if (staff.totalCepat > 0) CountText("${staff.totalCepat} Plg Cepat", NadaIos.INFO.teks)
                }
                if (staff.latestIn != null || staff.latestOut != null) {
                    Spacer(Modifier.height(7.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        staff.latestIn?.let { ClockChip(isIn = true, row = it) }
                        staff.latestOut?.let { ClockChip(isIn = false, row = it) }
                    }
                }
            }
            Spacer(Modifier.width(6.dp))
            Icon(
                IkonIos.ChevronRight,
                contentDescription = "Lihat detail ${staff.name}",
                tint = WarnaIos.LabelKetiga,
                modifier = Modifier.size(15.dp),
            )
        }
    }
}

@Composable
private fun CountText(text: String, color: Color) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = color, maxLines = 1)
}

@Composable
private fun ClockChip(isIn: Boolean, row: AttendanceRow) {
    Row(
        modifier = Modifier
            .clip(UkuranIos.SudutKapsul)
            .background(WarnaIos.Isian)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            if (isIn) Icons.AutoMirrored.Filled.Login else Icons.AutoMirrored.Filled.Logout,
            contentDescription = null,
            tint = if (isIn) WarnaIos.Hijau else WarnaIos.Oranye,
            modifier = Modifier.size(12.dp),
        )
        Text(if (isIn) "In:" else "Out:", style = TipeIos.Kecil)
        Text(row.jam, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = WarnaIos.Label)
        row.telatMenit?.takeIf { it > 0 }?.let { menit ->
            Text(
                if (isIn) "Telat ${menit}m" else "Cepat ${menit}m",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = NadaIos.BAHAYA.teks,
            )
        }
    }
}

@Composable
private fun StaffAvatar(name: String, photoPath: String?, size: Int) {
    val url = remember(photoPath) { selfiePublicUrl(photoPath) }
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(WarnaIos.Aksen.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.trim().take(1).uppercase(ID_LOCALE).ifBlank { "?" },
            color = NadaIos.AKSEN.teks,
            fontWeight = FontWeight.SemiBold,
            fontSize = (size / 2.6f).sp,
        )
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = "Foto $name",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

/* ------------------------------------------------------------ Detail sheet */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StaffDetailSheet(
    staff: StaffSummary,
    days: List<RekapDayDetail>,
    onDismiss: () -> Unit,
    onPreview: (String) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = WarnaIos.Latar,
        dragHandle = { BottomSheetDefaults.DragHandle(color = WarnaIos.LabelKetiga) },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = UkuranIos.TepiLayar + 2.dp)
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StaffAvatar(name = staff.name, photoPath = staff.latestPhotoPath, size = 42)
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(staff.name, style = TipeIos.Utama, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Detail Kehadiran", style = TipeIos.Catatan)
            }
            TombolBundarIos(IkonIos.Close, "Tutup", onKlik = onDismiss, warnaIkon = WarnaIos.LabelKedua)
        }

        if (days.isEmpty()) {
            Text(
                "Tidak ada riwayat detail.",
                style = TipeIos.SubJudul,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(40.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp),
                contentPadding = PaddingValues(start = UkuranIos.TepiLayar, end = UkuranIos.TepiLayar, top = 4.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(UkuranIos.JarakKartu),
            ) {
                items(days, key = { it.date.toString() }) { day ->
                    DayDetailCard(day = day, onPreview = onPreview)
                }
            }
        }
    }
}

@Composable
private fun DayDetailCard(day: RekapDayDetail, onPreview: (String) -> Unit) {
    KartuIos(padding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(day.label, style = TipeIos.Keterangan.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold))
            if (day.isAlpha) StatusPill("alpha", null)
        }
        PemisahIos(inset = 0.dp)

        if (!day.isAlpha) {
            Spacer(Modifier.height(12.dp))
            AttendanceDetailRow(isIn = true, row = day.masuk, onPreview = onPreview)
            Spacer(Modifier.height(10.dp))
            PemisahIos(inset = 51.dp)
            Spacer(Modifier.height(10.dp))
            AttendanceDetailRow(isIn = false, row = day.pulang, onPreview = onPreview)
        }
    }
}

@Composable
private fun AttendanceDetailRow(isIn: Boolean, row: AttendanceRow?, onPreview: (String) -> Unit) {
    val url = remember(row?.selfiePath) { selfiePublicUrl(row?.selfiePath) }

    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(UkuranIos.SudutKontrol)
                .background(WarnaIos.Isian)
                .then(if (url != null) Modifier.clickable { onPreview(url) } else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            if (url != null) {
                AsyncImage(
                    model = url,
                    contentDescription = if (isIn) "Selfie masuk" else "Selfie pulang",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    if (isIn) Icons.AutoMirrored.Filled.Login else Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = WarnaIos.LabelKetiga,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Icon(
                        if (isIn) Icons.AutoMirrored.Filled.Login else Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = if (isIn) WarnaIos.Hijau else WarnaIos.Oranye,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(if (isIn) "Masuk" else "Pulang", style = TipeIos.Keterangan.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold))
                }
                row?.let { StatusPill(it.status, it.telatMenit) }
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (row != null) {
                    Text(
                        row.jam,
                        style = TipeIos.Keterangan.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                        modifier = Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .background(WarnaIos.Isian)
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                    )
                    if (row.isManual) {
                        Text(
                            "Manual",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NadaIos.PERINGATAN.teks,
                            modifier = Modifier
                                .clip(UkuranIos.SudutKapsul)
                                .background(WarnaIos.Oranye.copy(alpha = 0.14f))
                                .padding(horizontal = 7.dp, vertical = 2.dp),
                        )
                    }
                } else {
                    Text(
                        if (isIn) "Belum / Tidak ada data" else "Belum Absen Pulang",
                        style = TipeIos.Catatan,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusPill(status: String, telatMenit: Int?) {
    val (fg, bg) = statusColors(status)
    val suffix = telatMenit?.takeIf { it > 0 && status != "tepat" && status != "alpha" }?.let { " ${it}m" } ?: ""
    Text(
        rekapStatusText(status) + suffix,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = fg,
        maxLines = 1,
        modifier = Modifier
            .clip(UkuranIos.SudutKapsul)
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/* --------------------------------------------------------- Photo preview */

@Composable
private fun PhotoPreviewDialog(url: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(UkuranIos.SudutKartu)
                .background(Color.Black)
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = url,
                contentDescription = "Selfie ukuran penuh",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

/* ------------------------------------------------------------- Empty/error */

@Composable
private fun RekapErrorCard(message: String, onRetry: () -> Unit) {
    val noOutlet = message.contains("cabang", ignoreCase = true)
    if (noOutlet) {
        KeadaanIos(
            ikon = IkonIos.Storefront,
            judul = "Cabang Belum Ditentukan",
            pesan = message,
            nada = NadaIos.AKSEN,
        )
    } else {
        KeadaanIos(
            ikon = IkonIos.ErrorOutline,
            judul = "Gagal memuat",
            pesan = message,
            nada = NadaIos.BAHAYA,
            teksAksi = "Coba Lagi",
            onAksi = onRetry,
        )
    }
}

/* -------------------------------------------------------------- CSV export */

/** Web mengunduh CSV langsung ke folder Download browser; di Android padanan yang
 *  wajar adalah menulis ke cache lalu membuka sheet "bagikan/simpan" sistem. */
private fun shareCsv(context: Context, fileName: String, csv: String) {
    val dir = File(context.cacheDir, "exports").apply { mkdirs() }
    val file = File(dir, fileName)
    file.writeText(csv)

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, fileName)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Simpan / bagikan rekap"))
}
