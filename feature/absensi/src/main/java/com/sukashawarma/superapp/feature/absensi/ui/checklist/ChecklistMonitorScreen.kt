package com.sukashawarma.superapp.presentation.absensi.checklist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.core.ui.ios.BilahJudulIos
import com.sukashawarma.superapp.core.ui.ios.KartuIos
import com.sukashawarma.superapp.core.ui.ios.KeadaanIos
import com.sukashawarma.superapp.core.ui.ios.KolomCariIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.PemisahIos
import com.sukashawarma.superapp.core.ui.ios.SegmenIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WadahSegmenIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.ios.permukaanIos
import com.sukashawarma.superapp.core.ui.ios.tekanIos
import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.domain.util.JakartaTime
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.RealtimeTables
import com.sukashawarma.superapp.core.ui.kaca.denganRuangNav
import com.sukashawarma.superapp.core.ui.SukaDropdownHeader
import com.sukashawarma.superapp.core.ui.SukaDropdownMenu
import com.sukashawarma.superapp.core.ui.SukaDropdownMenuItem

// Warna status — palet sistem iOS.
private val StatusGreen = NadaIos.SUKSES.teks
private val StatusAmber = NadaIos.PERINGATAN.teks
private val StatusRed = NadaIos.BAHAYA.teks

private val LongDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale("id", "ID"))
private val ShortDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale("id", "ID"))

private val ChecklistPhase.monitorLabel: String
    get() = if (this == ChecklistPhase.BUKA) "Buka Outlet" else "Tutup Outlet"

private val ChecklistPhase.accent: Color
    get() = if (this == ChecklistPhase.BUKA) WarnaIos.Aksen else WarnaIos.Indigo

/** "Hari Ini" / "Kemarin" / tanggal pendek — label yang paling cepat dibaca manager. */
private fun relativeDateLabel(date: LocalDate): String {
    val today = LocalDate.now(JakartaTime.ZONE)
    return when (date) {
        today -> "Hari Ini"
        today.minusDays(1) -> "Kemarin"
        else -> date.format(ShortDateFormatter)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistMonitorScreen(
    onExit: () -> Unit,
    viewModel: ChecklistMonitorViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    RealtimeRefresh(RealtimeTables.CHECKLIST_RECORDS, RealtimeTables.CHECKLIST_TICKS) { viewModel.refresh() }
    val phase = state.phase
    val summary = remember(state.categories, phase) { state.summaryOf(phase) }
    val phaseCategories = remember(state.categories, phase) { state.categoriesOf(phase) }
    val collapsed = remember { mutableStateMapOf<String, Boolean>() }
    var showDatePicker by remember { mutableStateOf(false) }

    // Kategori dilipat per outlet+tanggal, jadi jangan bawa state lipatan lintas konteks.
    LaunchedEffect(state.selectedOutletId, state.date) { collapsed.clear() }

    Scaffold(
        containerColor = WarnaIos.Latar,
        topBar = { BilahJudulIos(judul = "Monitor Checklist", onKembali = onExit, garisBawah = false) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = UkuranIos.TepiLayar, end = UkuranIos.TepiLayar, top = 4.dp, bottom = 32.dp).denganRuangNav(),
            verticalArrangement = Arrangement.spacedBy(UkuranIos.JarakKartu),
        ) {
            item(key = "header") {
                MonitorHeader(
                    date = state.date,
                    outletName = state.selectedOutletName,
                    lastRefresh = state.lastRefresh,
                    refreshing = state.refreshing,
                    showRefresh = state.selectedOutletId != null,
                    onRefresh = { viewModel.refresh() },
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
                        pesan = "Anda memantau seluruh outlet. Pilih salah satu di atas untuk melihat progres checklistnya.",
                        nada = NadaIos.AKSEN,
                    )
                }
                return@LazyColumn
            }

            item(key = "date_filter") {
                DateFilterCard(
                    date = state.date,
                    onPrev = { viewModel.setDate(state.date.minusDays(1)) },
                    onNext = { viewModel.setDate(state.date.plusDays(1)) },
                    onToday = { viewModel.setDate(LocalDate.now(JakartaTime.ZONE)) },
                    onOpenPicker = { showDatePicker = true },
                )
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

                state.error != null -> item(key = "error") {
                    MonitorErrorCard(message = state.error.orEmpty(), onRetry = { viewModel.load() })
                }

                else -> {
                    item(key = "summary") { SummaryCard(phase = phase, summary = summary) }

                    if (state.categories.isNotEmpty()) {
                        item(key = "tabs") {
                            PhaseTabs(selected = phase, onSelect = { viewModel.setPhase(it) })
                        }
                    }

                    when {
                        state.categories.isEmpty() -> item(key = "empty_all") {
                            KeadaanIos(
                                ikon = IkonIos.Checklist,
                                judul = "Belum Ada Template Checklist",
                                pesan = "Buat dulu daftar tugasnya di menu Manajemen Checklist.",
                                nada = NadaIos.AKSEN,
                            )
                        }

                        phaseCategories.isEmpty() -> item(key = "empty_phase") {
                            DashedEmptyCard("Tidak ada tugas ${phase.monitorLabel.lowercase(Locale.getDefault())}.")
                        }

                        else -> items(phaseCategories, key = { it.id }) { category ->
                            CategoryCard(
                                category = category,
                                expanded = collapsed[category.id] != true,
                                onToggle = { collapsed[category.id] = collapsed[category.id] != true },
                            )
                        }
                    }

                    if (summary.totalItems > 0 && summary.progress == 100) {
                        item(key = "all_done") { AllDoneBanner(phase) }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        MonitorDatePickerDialog(
            initial = state.date,
            onDismiss = { showDatePicker = false },
            onConfirm = { picked ->
                viewModel.setDate(picked)
                showDatePicker = false
            },
        )
    }
}

/* ---------------------------------------------------------------- Header */

@Composable
private fun MonitorHeader(
    date: LocalDate,
    outletName: String?,
    lastRefresh: String,
    refreshing: Boolean,
    showRefresh: Boolean,
    onRefresh: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = date.format(LongDateFormatter),
                style = TipeIos.Judul3.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = outletName ?: "Progres checklist operasional",
                style = TipeIos.SubJudul,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (showRefresh) {
            Spacer(Modifier.width(8.dp))
            // Kapsul abu ala tombol toolbar iOS: ikon segarkan + jam muat terakhir.
            Row(
                modifier = Modifier
                    .height(34.dp)
                    .clip(UkuranIos.SudutKapsul)
                    .background(WarnaIos.Isian)
                    .tekanIos(onRefresh, aktif = !refreshing)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (refreshing) {
                    CircularProgressIndicator(color = WarnaIos.Aksen, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                } else {
                    Icon(IkonIos.Refresh, contentDescription = "Muat ulang", tint = WarnaIos.Aksen, modifier = Modifier.size(15.dp))
                }
                if (lastRefresh.isNotBlank()) {
                    Text(lastRefresh, color = WarnaIos.Aksen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/* ---------------------------------------------------------- Outlet picker */

@Composable
private fun OutletPickerCard(
    outlets: List<MonitorOutletOption>,
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
                    text = if (outlets.isEmpty() && !loading) "Tidak ada outlet yang bisa dipantau" else "Ketuk untuk ganti outlet",
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
            modifier = Modifier.width(300.dp).heightIn(max = 440.dp),
        ) {
            SukaDropdownHeader(
                title = "PILIH OUTLET",
                onClose = { expanded = false },
            )
            Column(Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
                KolomCariIos(
                    nilai = query,
                    onUbah = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "Cari outlet...",
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
                            title = outlet.name,
                            selected = outlet.id == selectedId,
                            leadingIcon = IkonIos.Storefront,
                            onClick = {
                                onSelect(outlet.id)
                                expanded = false
                            },
                        )
                    }
                }
            }
        }
    }
}

/* ------------------------------------------------------------ Date filter */

@Composable
private fun DateFilterCard(
    date: LocalDate,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onOpenPicker: () -> Unit,
) {
    val today = LocalDate.now(JakartaTime.ZONE)
    val isToday = date == today

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .permukaanIos(UkuranIos.SudutGrup)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DateStepButton(icon = Icons.Filled.ChevronLeft, description = "Hari sebelumnya", enabled = true, onClick = onPrev)

        Row(
            modifier = Modifier
                .weight(1f)
                .clip(UkuranIos.SudutKontrol)
                .clickable(onClick = onOpenPicker)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(IkonIos.CalendarMonth, contentDescription = null, tint = WarnaIos.Aksen, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    relativeDateLabel(date),
                    style = TipeIos.Keterangan.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                )
                Text(
                    if (isToday) date.format(ShortDateFormatter) else "Ketuk untuk pilih tanggal",
                    style = TipeIos.Kecil,
                    maxLines = 1,
                )
            }
        }

        if (!isToday) {
            Text(
                "Hari Ini",
                color = WarnaIos.Aksen,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(UkuranIos.SudutKapsul)
                    .background(WarnaIos.Aksen.copy(alpha = 0.12f))
                    .tekanIos(onToday)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
            Spacer(Modifier.width(2.dp))
        }

        DateStepButton(
            icon = IkonIos.ChevronRight,
            description = "Hari berikutnya",
            enabled = !isToday,
            onClick = onNext,
        )
    }
}

@Composable
private fun DateStepButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(38.dp)) {
        Icon(
            icon,
            contentDescription = description,
            tint = if (enabled) WarnaIos.Aksen else WarnaIos.LabelKetiga,
            modifier = Modifier.size(20.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonitorDatePickerDialog(
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
                Text("Tampilkan", color = if (valid) WarnaIos.Aksen else WarnaIos.LabelKetiga, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal", color = WarnaIos.LabelKedua) }
        },
        colors = DatePickerDefaults.colors(containerColor = WarnaIos.Kartu),
    ) {
        DatePicker(
            state = pickerState,
            title = {
                Text(
                    "Pilih tanggal",
                    style = TipeIos.Catatan.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp),
                )
            },
            headline = {
                Text(
                    picked?.format(LongDateFormatter) ?: "-",
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
        if (!valid && picked != null) {
            Text(
                "Tanggal belum lewat — pilih hari ini atau sebelumnya.",
                style = TipeIos.Kecil.copy(color = StatusAmber),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
            )
        }
    }
}

/* --------------------------------------------------------------- Summary */

@Composable
private fun SummaryCard(phase: ChecklistPhase, summary: PhaseSummary) {
    val progress = summary.progress
    val animated by animateFloatAsState(
        targetValue = progress / 100f,
        animationSpec = tween(durationMillis = 700),
        label = "checklist-progress",
    )
    val barColor = if (progress == 100) WarnaIos.Hijau else phase.accent

    KartuIos {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Progress ${phase.monitorLabel}", style = TipeIos.Utama, modifier = Modifier.padding(bottom = 4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$progress", style = TipeIos.JudulBesar.copy(fontSize = 30.sp))
                Text("%", style = TipeIos.SubJudul.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.padding(bottom = 5.dp))
            }
        }

        Spacer(Modifier.height(10.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(CircleShape)
                .background(WarnaIos.Isian),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(animated.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(barColor),
            )
        }

        Spacer(Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile(modifier = Modifier.weight(1f), label = "Tugas selesai") {
                FractionText(summary.tickedItems, summary.totalItems, WarnaIos.Label)
            }
            StatTile(modifier = Modifier.weight(1f), label = "Wajib selesai") {
                FractionText(
                    summary.tickedRequired,
                    summary.totalRequired,
                    if (summary.allRequiredDone) StatusGreen else StatusRed,
                )
            }
            StatTile(modifier = Modifier.weight(1f), label = "Status") {
                val ok = summary.allRequiredDone
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        if (ok) IkonIos.CheckCircle else IkonIos.WarningAmber,
                        contentDescription = null,
                        tint = if (ok) WarnaIos.Hijau else WarnaIos.Oranye,
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        text = if (ok) "Siap ${if (phase == ChecklistPhase.BUKA) "Buka" else "Tutup"}" else "Belum Siap",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (ok) StatusGreen else StatusAmber,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** Petak abu ala blok angka iOS. */
@Composable
private fun StatTile(modifier: Modifier = Modifier, label: String, value: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .clip(UkuranIos.SudutBlok)
            .background(WarnaIos.Latar)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.height(24.dp), contentAlignment = Alignment.Center) { value() }
        Spacer(Modifier.height(2.dp))
        Text(label, style = TipeIos.Kecil.copy(fontWeight = FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun FractionText(done: Int, total: Int, color: Color) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text("$done", style = TipeIos.Angka.copy(fontSize = 18.sp, color = color))
        Text("/$total", style = TipeIos.Kecil.copy(fontWeight = FontWeight.Medium), modifier = Modifier.padding(bottom = 2.dp))
    }
}

/* ------------------------------------------------------------------ Tabs */

@Composable
private fun PhaseTabs(selected: ChecklistPhase, onSelect: (ChecklistPhase) -> Unit) {
    WadahSegmenIos {
        ChecklistPhase.entries.forEach { phase ->
            SegmenIos(
                label = phase.monitorLabel,
                aktif = phase == selected,
                onKlik = { onSelect(phase) },
                modifier = Modifier.weight(1f),
                ikon = if (phase == ChecklistPhase.BUKA) Icons.Filled.WbSunny else Icons.Filled.NightsStay,
            )
        }
    }
}

/* -------------------------------------------------------------- Category */

@Composable
private fun CategoryCard(category: MonitorCategory, expanded: Boolean, onToggle: () -> Unit) {
    val done = category.allDone
    val requiredComplete = category.requiredDone == category.requiredTotal

    Column(Modifier.fillMaxWidth().permukaanIos(UkuranIos.SudutKartu)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            done -> WarnaIos.Hijau
                            requiredComplete -> WarnaIos.Oranye
                            else -> WarnaIos.Aksen
                        }
                    )
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    category.name,
                    style = TipeIos.Utama.copy(color = if (done) StatusGreen else WarnaIos.Label),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(5.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Pill(
                        text = "${category.tickedCount}/${category.items.size} selesai",
                        nada = if (done) NadaIos.SUKSES else NadaIos.AKSEN,
                    )
                    if (category.requiredTotal > 0) {
                        Pill(
                            text = "${category.requiredDone}/${category.requiredTotal} wajib",
                            nada = if (requiredComplete) NadaIos.SUKSES else NadaIos.BAHAYA,
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                if (expanded) IkonIos.ExpandLess else IkonIos.ExpandMore,
                contentDescription = if (expanded) "Tutup" else "Buka",
                tint = WarnaIos.LabelKetiga,
                modifier = Modifier.size(16.dp),
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column {
                PemisahIos(inset = 0.dp)
                category.items.forEachIndexed { index, item ->
                    if (index > 0) PemisahIos(inset = 49.dp)
                    ItemRow(item)
                }
                if (category.items.isEmpty()) {
                    Text(
                        "Kategori ini belum punya tugas.",
                        style = TipeIos.Catatan,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ItemRow(item: MonitorItem) {
    val ticked = item.ticked
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (ticked) IkonIos.CheckCircle else Icons.Filled.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (ticked) WarnaIos.Hijau else WarnaIos.LabelKetiga,
            modifier = Modifier.size(21.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                item.name,
                style = TipeIos.Keterangan.copy(
                    fontSize = 15.sp,
                    color = if (ticked) WarnaIos.LabelKedua else WarnaIos.Label,
                    textDecoration = if (ticked) TextDecoration.LineThrough else null,
                ),
            )
            Spacer(Modifier.height(2.dp))
            if (ticked) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Person, contentDescription = null, tint = WarnaIos.Abu, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        item.tickedBy.orEmpty(),
                        style = TipeIos.Kecil.copy(color = NadaIos.AKSEN.teks, fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(" · ${item.tickedAt}", style = TipeIos.Kecil)
                }
            } else {
                Text("Belum dikerjakan", style = TipeIos.Kecil)
            }
        }
        Spacer(Modifier.width(8.dp))
        when {
            ticked -> Pill(text = "✓ Done", nada = NadaIos.SUKSES)
            item.isRequired -> Pill(text = "Wajib", nada = NadaIos.BAHAYA)
        }
    }
}

/** Kapsul kecil bernada — versi ringkas LencanaIos untuk baris padat. */
@Composable
private fun Pill(text: String, nada: NadaIos) {
    Text(
        text,
        color = nada.teks,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        modifier = Modifier
            .clip(UkuranIos.SudutKapsul)
            .background(nada.warna.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/* ------------------------------------------------------- Banner & states */

@Composable
private fun AllDoneBanner(phase: ChecklistPhase) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .permukaanIos(UkuranIos.SudutKartu, WarnaIos.Hijau)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(IkonIos.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
        Spacer(Modifier.height(8.dp))
        Text("Tugas ${phase.monitorLabel} Selesai!", style = TipeIos.Utama.copy(color = Color.White))
        Spacer(Modifier.height(4.dp))
        Text(
            if (phase == ChecklistPhase.BUKA) "Outlet siap beroperasi penuh."
            else "Outlet siap ditutup dan kru bisa pulang.",
            style = TipeIos.Catatan.copy(color = Color.White.copy(alpha = 0.9f)),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DashedEmptyCard(message: String) {
    KartuIos {
        Text(
            message,
            style = TipeIos.SubJudul,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp, horizontal = 4.dp),
        )
    }
}

@Composable
private fun MonitorErrorCard(message: String, onRetry: () -> Unit) {
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
