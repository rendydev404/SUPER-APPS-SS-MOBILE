package com.sukashawarma.superapp.presentation.absensi.checklist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Warning
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
import com.sukashawarma.superapp.domain.util.JakartaTime
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

// Stitch Suka Culinary Design Tokens (samakan dengan ChecklistManageScreen)
private val StitchPrimary = Color(0xFF450700)
private val StitchSecondaryContainer = Color(0xFFFE8438)
private val StitchSurface = Color(0xFFF8F9FF)
private val StitchSurfaceContainerLowest = Color(0xFFFFFFFF)
private val StitchSurfaceContainerLow = Color(0xFFEFF4FF)
private val StitchSurfaceContainer = Color(0xFFE5EEFF)
private val StitchSurfaceContainerHigh = Color(0xFFDCE9FF)
private val StitchOnSurface = Color(0xFF0B1C30)
private val StitchOnSurfaceVariant = Color(0xFF57423D)
private val StitchOutlineVariant = Color(0xFFDEC0B9)
private val StitchError = Color(0xFFBA1A1A)
private val StitchErrorContainer = Color(0xFFFFDAD6)

// Status accents
private val StatusGreen = Color(0xFF059669)
private val StatusGreenSoft = Color(0xFFE7F7F0)
private val StatusAmber = Color(0xFFD97706)
private val StatusRed = Color(0xFFDC2626)
private val StatusRedSoft = Color(0xFFFEE2E2)

private val LongDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale("id", "ID"))
private val ShortDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale("id", "ID"))

private val ChecklistPhase.monitorLabel: String
    get() = if (this == ChecklistPhase.BUKA) "Buka Toko" else "Tutup Toko"

private val ChecklistPhase.accent: Color
    get() = if (this == ChecklistPhase.BUKA) StitchSecondaryContainer else StitchPrimary

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
    val phase = state.phase
    val summary = remember(state.categories, phase) { state.summaryOf(phase) }
    val phaseCategories = remember(state.categories, phase) { state.categoriesOf(phase) }
    val collapsed = remember { mutableStateMapOf<String, Boolean>() }
    var showDatePicker by remember { mutableStateOf(false) }

    // Kategori dilipat per outlet+tanggal, jadi jangan bawa state lipatan lintas konteks.
    LaunchedEffect(state.selectedOutletId, state.date) { collapsed.clear() }

    Scaffold(
        containerColor = StitchSurface,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StitchSurface,
                    titleContentColor = StitchPrimary,
                    navigationIconContentColor = StitchOnSurface,
                    actionIconContentColor = StitchOnSurface,
                ),
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                title = {
                    Text(
                        "Monitor Checklist",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchPrimary,
                    )
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
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
                    EmptyCard(
                        icon = { Icon(Icons.Filled.Storefront, null, tint = StitchPrimary, modifier = Modifier.size(26.dp)) },
                        title = "Pilih Outlet Dulu",
                        message = "Anda memantau seluruh outlet. Pilih salah satu di atas untuk melihat progres checklistnya.",
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
                        CircularProgressIndicator(color = StitchPrimary, strokeWidth = 3.dp)
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
                            EmptyCard(
                                icon = { Icon(Icons.Filled.Checklist, null, tint = StitchPrimary, modifier = Modifier.size(26.dp)) },
                                title = "Belum Ada Template Checklist",
                                message = "Buat dulu daftar tugasnya di menu Manajemen Checklist.",
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
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(shape = RoundedCornerShape(14.dp), color = StitchSurfaceContainer, modifier = Modifier.size(42.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Checklist, contentDescription = null, tint = StitchPrimary, modifier = Modifier.size(21.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = date.format(LongDateFormatter),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = StitchOnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = outletName ?: "Progres checklist operasional",
                fontSize = 12.5.sp,
                color = StitchOnSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (showRefresh) {
            Spacer(Modifier.width(8.dp))
            Surface(
                onClick = onRefresh,
                enabled = !refreshing,
                shape = RoundedCornerShape(12.dp),
                color = StitchSurfaceContainerLowest,
                border = BorderStroke(1.dp, StitchOutlineVariant.copy(alpha = 0.5f)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (refreshing) {
                        CircularProgressIndicator(color = StitchPrimary, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                    } else {
                        Icon(Icons.Filled.Refresh, contentDescription = "Muat ulang", tint = StitchOnSurfaceVariant, modifier = Modifier.size(15.dp))
                    }
                    if (lastRefresh.isNotBlank()) {
                        Text(lastRefresh, fontSize = 11.5.sp, color = StitchOnSurfaceVariant, fontWeight = FontWeight.Medium)
                    }
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
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !loading && outlets.isNotEmpty()) { expanded = true },
            shape = RoundedCornerShape(14.dp),
            color = StitchSurfaceContainerLowest,
            border = BorderStroke(1.dp, if (selected == null) StitchSecondaryContainer.copy(alpha = 0.55f) else StitchSurfaceContainerHigh),
            shadowElevation = 0.5.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Filled.Storefront, contentDescription = null, tint = StitchSecondaryContainer, modifier = Modifier.size(21.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = when {
                            loading -> "Memuat outlet..."
                            selected != null -> selected.name
                            else -> "Pilih outlet"
                        },
                        color = StitchOnSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (outlets.isEmpty() && !loading) "Tidak ada outlet yang bisa dipantau" else "Ketuk untuk ganti outlet",
                        color = StitchOnSurfaceVariant,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                    )
                }
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = "Pilih outlet",
                    tint = StitchOnSurfaceVariant,
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(290.dp).heightIn(max = 430.dp),
        ) {
            Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    placeholder = { Text("Cari outlet", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Hapus pencarian")
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StitchSecondaryContainer,
                        unfocusedBorderColor = StitchSurfaceContainerHigh,
                        focusedContainerColor = StitchSurfaceContainerLowest,
                        unfocusedContainerColor = StitchSurfaceContainerLowest,
                    ),
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
                            color = StitchOnSurfaceVariant,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 18.dp),
                        )
                    } else filtered.forEach { outlet ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    outlet.name,
                                    fontSize = 14.sp,
                                    fontWeight = if (outlet.id == selectedId) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (outlet.id == selectedId) StitchSecondaryContainer else StitchOnSurface,
                                )
                            },
                            leadingIcon = { Icon(Icons.Filled.Storefront, contentDescription = null, modifier = Modifier.size(20.dp)) },
                            onClick = { onSelect(outlet.id); expanded = false },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
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

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = StitchSurfaceContainerLowest,
        border = BorderStroke(1.dp, StitchSurfaceContainerHigh),
        shadowElevation = 0.5.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DateStepButton(icon = Icons.Filled.ChevronLeft, description = "Hari sebelumnya", enabled = true, onClick = onPrev)

            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onOpenPicker)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = StitchPrimary, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        relativeDateLabel(date),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchOnSurface,
                        maxLines = 1,
                    )
                    Text(
                        if (isToday) date.format(ShortDateFormatter) else "Ketuk untuk pilih tanggal",
                        fontSize = 11.sp,
                        color = StitchOnSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }

            if (!isToday) {
                Surface(
                    onClick = onToday,
                    shape = RoundedCornerShape(9.dp),
                    color = StitchSecondaryContainer.copy(alpha = 0.14f),
                ) {
                    Text(
                        "Hari Ini",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                    )
                }
                Spacer(Modifier.width(2.dp))
            }

            DateStepButton(
                icon = Icons.Filled.ChevronRight,
                description = "Hari berikutnya",
                enabled = !isToday,
                onClick = onNext,
            )
        }
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
            tint = if (enabled) StitchOnSurface else StitchOutlineVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(22.dp),
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
                Text("Tampilkan", color = if (valid) StitchPrimary else StitchOnSurfaceVariant, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal", color = StitchOnSurfaceVariant) }
        },
        colors = DatePickerDefaults.colors(containerColor = StitchSurfaceContainerLowest),
    ) {
        DatePicker(
            state = pickerState,
            title = {
                Text(
                    "Pilih tanggal",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = StitchOnSurfaceVariant,
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp),
                )
            },
            headline = {
                Text(
                    picked?.format(LongDateFormatter) ?: "-",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = StitchOnSurface,
                    modifier = Modifier.padding(start = 24.dp, bottom = 8.dp),
                )
            },
            showModeToggle = false,
            colors = DatePickerDefaults.colors(
                containerColor = StitchSurfaceContainerLowest,
                selectedDayContainerColor = StitchPrimary,
                todayDateBorderColor = StitchSecondaryContainer,
            ),
        )
        if (!valid && picked != null) {
            Text(
                "Tanggal belum lewat — pilih hari ini atau sebelumnya.",
                fontSize = 12.sp,
                color = StatusAmber,
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
    val barColor = if (progress == 100) StatusGreen else phase.accent

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = StitchSurfaceContainerLowest,
        border = BorderStroke(1.dp, StitchSurfaceContainerHigh.copy(alpha = 0.7f)),
        shadowElevation = 0.5.dp,
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Progress ${phase.monitorLabel}", fontSize = 13.sp, color = StitchOnSurfaceVariant)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("$progress", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = StitchOnSurface)
                    Text("%", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = StitchOnSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
                }
            }

            Spacer(Modifier.height(10.dp))

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(StitchSurfaceContainerLow),
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

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(modifier = Modifier.weight(1f), label = "Tugas selesai") {
                    FractionText(summary.tickedItems, summary.totalItems, StitchOnSurface)
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
                            if (ok) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                            contentDescription = null,
                            tint = if (ok) StatusGreen else StatusAmber,
                            modifier = Modifier.size(15.dp),
                        )
                        Text(
                            text = if (ok) "Siap ${if (phase == ChecklistPhase.BUKA) "Buka" else "Tutup"}" else "Belum Siap",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (ok) StatusGreen else StatusAmber,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(modifier: Modifier = Modifier, label: String, value: @Composable () -> Unit) {
    Surface(modifier = modifier, shape = RoundedCornerShape(14.dp), color = StitchSurfaceContainerLow) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(modifier = Modifier.height(22.dp), contentAlignment = Alignment.Center) { value() }
            Spacer(Modifier.height(2.dp))
            Text(label, fontSize = 11.sp, color = StitchOnSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun FractionText(done: Int, total: Int, color: Color) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text("$done", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = color)
        Text("/$total", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = StitchOnSurfaceVariant, modifier = Modifier.padding(bottom = 1.dp))
    }
}

/* ------------------------------------------------------------------ Tabs */

@Composable
private fun PhaseTabs(selected: ChecklistPhase, onSelect: (ChecklistPhase) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = StitchSurfaceContainer.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, StitchSurfaceContainerHigh),
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ChecklistPhase.entries.forEach { phase ->
                val active = phase == selected
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = if (active) StitchSurfaceContainerLowest else Color.Transparent,
                    shadowElevation = if (active) 1.dp else 0.dp,
                    onClick = { onSelect(phase) },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            if (phase == ChecklistPhase.BUKA) Icons.Filled.WbSunny else Icons.Filled.NightsStay,
                            contentDescription = null,
                            tint = if (active) phase.accent else StitchOnSurfaceVariant,
                            modifier = Modifier.size(17.dp),
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            phase.monitorLabel,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (active) phase.accent else StitchOnSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/* -------------------------------------------------------------- Category */

@Composable
private fun CategoryCard(category: MonitorCategory, expanded: Boolean, onToggle: () -> Unit) {
    val done = category.allDone
    val requiredComplete = category.requiredDone == category.requiredTotal

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = StitchSurfaceContainerLowest,
        border = BorderStroke(1.dp, if (done) StatusGreen.copy(alpha = 0.4f) else StitchSurfaceContainerHigh.copy(alpha = 0.7f)),
        shadowElevation = 0.5.dp,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (done) StatusGreenSoft else StitchSurfaceContainerLow.copy(alpha = 0.6f))
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                done -> StatusGreen
                                requiredComplete -> StatusAmber
                                else -> StitchSecondaryContainer
                            }
                        )
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        category.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (done) StatusGreen else StitchOnSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(5.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Pill(
                            text = "${category.tickedCount}/${category.items.size} selesai",
                            fg = if (done) StatusGreen else StitchSecondaryContainer,
                            bg = if (done) StatusGreenSoft else StitchSecondaryContainer.copy(alpha = 0.14f),
                        )
                        if (category.requiredTotal > 0) {
                            Pill(
                                text = "${category.requiredDone}/${category.requiredTotal} wajib",
                                fg = if (requiredComplete) StatusGreen else StatusRed,
                                bg = if (requiredComplete) StatusGreenSoft else StatusRedSoft,
                            )
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Tutup" else "Buka",
                    tint = StitchOnSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    category.items.forEachIndexed { index, item ->
                        if (index > 0) {
                            HorizontalDivider(color = StitchOutlineVariant.copy(alpha = 0.25f), thickness = 1.dp)
                        }
                        ItemRow(item)
                    }
                    if (category.items.isEmpty()) {
                        Text(
                            "Kategori ini belum punya tugas.",
                            fontSize = 12.5.sp,
                            color = StitchOnSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
                        )
                    }
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
            .background(if (ticked) StatusGreenSoft.copy(alpha = 0.45f) else Color.Transparent)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (ticked) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (ticked) StatusGreen else StitchOutlineVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(21.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                item.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (ticked) StitchOnSurfaceVariant else StitchOnSurface,
                textDecoration = if (ticked) TextDecoration.LineThrough else null,
            )
            Spacer(Modifier.height(2.dp))
            if (ticked) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Person, contentDescription = null, tint = StitchOnSurfaceVariant, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        item.tickedBy.orEmpty(),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = StitchPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(" · ${item.tickedAt}", fontSize = 11.5.sp, color = StitchOnSurfaceVariant)
                }
            } else {
                Text("Belum dikerjakan", fontSize = 11.5.sp, color = StitchOnSurfaceVariant.copy(alpha = 0.75f))
            }
        }
        Spacer(Modifier.width(8.dp))
        when {
            ticked -> Pill(text = "✓ Done", fg = StatusGreen, bg = StatusGreenSoft)
            item.isRequired -> Pill(text = "Wajib", fg = StatusRed, bg = StatusRedSoft)
        }
    }
}

@Composable
private fun Pill(text: String, fg: Color, bg: Color) {
    Surface(shape = RoundedCornerShape(8.dp), color = bg) {
        Text(
            text,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Medium,
            color = fg,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
        )
    }
}

/* ------------------------------------------------------- Banner & states */

@Composable
private fun AllDoneBanner(phase: ChecklistPhase) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = StatusGreen,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
            Spacer(Modifier.height(8.dp))
            Text("Tugas ${phase.monitorLabel} Selesai!", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(4.dp))
            Text(
                if (phase == ChecklistPhase.BUKA) "Outlet siap beroperasi penuh."
                else "Outlet siap ditutup dan kru bisa pulang.",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun EmptyCard(icon: @Composable () -> Unit, title: String, message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        shape = RoundedCornerShape(18.dp),
        color = StitchSurfaceContainerLow,
        border = BorderStroke(1.dp, StitchSurfaceContainerHigh),
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(shape = CircleShape, color = StitchSurfaceContainer, modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) { icon() }
            }
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = StitchOnSurface, textAlign = TextAlign.Center)
            Text(message, fontSize = 13.sp, color = StitchOnSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun DashedEmptyCard(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = StitchSurfaceContainerLow.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, StitchOutlineVariant.copy(alpha = 0.45f)),
    ) {
        Text(
            message,
            fontSize = 13.sp,
            color = StitchOnSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 34.dp, horizontal = 20.dp),
        )
    }
}

@Composable
private fun MonitorErrorCard(message: String, onRetry: () -> Unit) {
    val noOutlet = message.contains("cabang", ignoreCase = true)
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        shape = RoundedCornerShape(18.dp),
        color = StitchSurfaceContainerLow,
        border = BorderStroke(1.dp, StitchSurfaceContainerHigh),
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = if (noOutlet) StitchSurfaceContainer else StitchErrorContainer,
                modifier = Modifier.size(52.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (noOutlet) Icons.Filled.Store else Icons.Filled.PriorityHigh,
                        contentDescription = null,
                        tint = if (noOutlet) StitchPrimary else StitchError,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
            if (noOutlet) {
                Text("Cabang Belum Ditentukan", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = StitchOnSurface)
            }
            Text(message, color = StitchOnSurfaceVariant, fontSize = 13.sp, textAlign = TextAlign.Center)
            if (!noOutlet) {
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = StitchPrimary),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Coba Lagi")
                }
            }
        }
    }
}
