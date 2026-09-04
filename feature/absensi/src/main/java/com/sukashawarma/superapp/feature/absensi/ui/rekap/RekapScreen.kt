package com.sukashawarma.superapp.presentation.absensi.rekap

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.sukashawarma.superapp.domain.util.JakartaTime
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

// Stitch Suka Culinary Design Tokens (samakan dengan layar Absensi lainnya)
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

private val StatEmeraldBg = Color(0xFFECFDF5)
private val StatEmeraldFg = Color(0xFF047857)
private val StatEmeraldLine = Color(0xFFD1FAE5)
private val StatAmberBg = Color(0xFFFFFBEB)
private val StatAmberFg = Color(0xFFB45309)
private val StatAmberLine = Color(0xFFFDE68A)
private val StatRoseBg = Color(0xFFFFF1F2)
private val StatRoseFg = Color(0xFFBE123C)
private val StatRoseLine = Color(0xFFFECDD3)
private val StatSkyBg = Color(0xFFF0F9FF)
private val StatSkyFg = Color(0xFF0369A1)
private val StatSkyLine = Color(0xFFBAE6FD)

private val ID_LOCALE = Locale("id", "ID")
private val ShortDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", ID_LOCALE)

/** Warna pil status — dipetakan dari `status` attendance seperti StatusPill di web. */
private fun statusColors(status: String): Pair<Color, Color> = when (status) {
    "tepat" -> StatEmeraldFg to StatEmeraldBg
    "telat", "pulang_telat" -> StatAmberFg to StatAmberBg
    "telat_toleransi" -> Color(0xFF854D0E) to Color(0xFFFEF9C3)
    "alpha" -> StatRoseFg to StatRoseBg
    "lebih_awal" -> StatSkyFg to StatSkyBg
    else -> StitchOnSurfaceVariant to StitchSurfaceContainerLow
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RekapScreen(onExit: () -> Unit, viewModel: RekapViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    var selectedStaff by remember { mutableStateOf<StaffSummary?>(null) }
    var previewUrl by remember { mutableStateOf<String?>(null) }
    var pickingCustomStart by remember { mutableStateOf(false) }
    var pickingCustomEnd by remember { mutableStateOf(false) }

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
                    Text("Rekap & Riwayat", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = StitchPrimary)
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
                    EmptyCard(
                        icon = { Icon(Icons.Filled.Storefront, null, tint = StitchPrimary, modifier = Modifier.size(26.dp)) },
                        title = "Pilih Outlet Dulu",
                        message = "Anda memantau seluruh outlet. Pilih salah satu di atas untuk melihat rekap kehadirannya.",
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
                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Ringkasan Karyawan", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = StitchOnSurface)
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
                                CircularProgressIndicator(color = StitchPrimary, strokeWidth = 3.dp)
                            }
                        }

                        state.summaries.isEmpty() -> item(key = "empty") {
                            EmptyCard(
                                icon = { Icon(Icons.Filled.Person, null, tint = StitchPrimary, modifier = Modifier.size(26.dp)) },
                                title = "Belum Ada Data",
                                message = "Tidak ada aktivitas absensi di periode ini.",
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
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(shape = RoundedCornerShape(14.dp), color = StitchSurfaceContainer, modifier = Modifier.size(42.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = null, tint = StitchPrimary, modifier = Modifier.size(21.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                periodLabel,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = StitchOnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                outletName ?: "Ringkasan & detail kehadiran per karyawan",
                fontSize = 12.5.sp,
                color = StitchOnSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Surface(
            onClick = onExport,
            enabled = canExport,
            shape = RoundedCornerShape(12.dp),
            color = if (canExport) StitchPrimary else StitchSurfaceContainerLow,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(
                    Icons.Filled.Download,
                    contentDescription = "Export CSV",
                    tint = if (canExport) Color.White else StitchOnSurfaceVariant,
                    modifier = Modifier.size(15.dp),
                )
                Text(
                    "CSV",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (canExport) Color.White else StitchOnSurfaceVariant,
                )
            }
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
                        text = if (outlets.isEmpty() && !loading) "Tidak ada outlet yang bisa dilihat" else "Ketuk untuk ganti outlet",
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
                RekapPeriod.entries.forEach { period ->
                    val active = period == selected
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = if (active) StitchSurfaceContainerLowest else Color.Transparent,
                        shadowElevation = if (active) 1.dp else 0.dp,
                        onClick = { onSelect(period) },
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                period.label,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (active) StitchPrimary else StitchOnSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }
                }
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
    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = StitchSurfaceContainerLowest,
        border = BorderStroke(1.dp, StitchSurfaceContainerHigh),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = StitchPrimary, modifier = Modifier.size(17.dp))
            Column(Modifier.weight(1f)) {
                Text(label, fontSize = 10.5.sp, color = StitchOnSurfaceVariant)
                Text(
                    date.format(ShortDateFormatter),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = StitchOnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
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
                Text("Terapkan", color = if (valid) StitchPrimary else StitchOnSurfaceVariant, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal", color = StitchOnSurfaceVariant) } },
        colors = DatePickerDefaults.colors(containerColor = StitchSurfaceContainerLowest),
    ) {
        DatePicker(
            state = pickerState,
            title = {
                Text(
                    title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = StitchOnSurfaceVariant,
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp),
                )
            },
            headline = {
                Text(
                    picked?.format(ShortDateFormatter) ?: "-",
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
    }
}

/* ---------------------------------------------------------- Global summary */

@Composable
private fun GlobalSummaryGrid(summary: RekapGlobalSummary, loading: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(Modifier.weight(1f), "Kehadiran (Masuk)", summary.masuk, loading, StatEmeraldBg, StatEmeraldFg, StatEmeraldLine)
            StatCard(Modifier.weight(1f), "Terlambat", summary.telat, loading, StatAmberBg, StatAmberFg, StatAmberLine)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(Modifier.weight(1f), "Alpha / Tidak Hadir", summary.alpha, loading, StatRoseBg, StatRoseFg, StatRoseLine)
            StatCard(Modifier.weight(1f), "Pulang Cepat", summary.cepat, loading, StatSkyBg, StatSkyFg, StatSkyLine)
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier,
    label: String,
    value: Int,
    loading: Boolean,
    bg: Color,
    fg: Color,
    line: Color,
) {
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = bg, border = BorderStroke(1.dp, line)) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 13.dp)) {
            Text(
                label.uppercase(ID_LOCALE),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = fg.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (loading) "-" else "$value",
                fontSize = 27.sp,
                fontWeight = FontWeight.ExtraBold,
                color = fg,
            )
        }
    }
}

/* ------------------------------------------------------------ Status filter */

@Composable
private fun StatusFilterMenu(selected: RekapStatusFilter, onSelect: (RekapStatusFilter) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(11.dp),
            color = StitchSurfaceContainerLowest,
            border = BorderStroke(1.dp, StitchSurfaceContainerHigh),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Filled.Tune, contentDescription = null, tint = StitchOnSurfaceVariant, modifier = Modifier.size(15.dp))
                Text(selected.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = StitchOnSurface, maxLines = 1)
                Icon(Icons.Filled.ExpandMore, contentDescription = null, tint = StitchOnSurfaceVariant, modifier = Modifier.size(16.dp))
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            RekapStatusFilter.entries.forEach { filter ->
                DropdownMenuItem(
                    text = {
                        Text(
                            filter.label,
                            fontSize = 14.sp,
                            fontWeight = if (filter == selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (filter == selected) StitchPrimary else StitchOnSurface,
                        )
                    },
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = StitchSurfaceContainerLowest,
        border = BorderStroke(1.dp, StitchSurfaceContainerHigh.copy(alpha = 0.7f)),
        shadowElevation = 0.5.dp,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StaffAvatar(name = staff.name, photoPath = staff.latestPhotoPath, size = 46)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    staff.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = StitchOnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    CountText("${staff.totalMasuk} Hadir", StatEmeraldFg)
                    if (staff.totalTelat > 0) CountText("${staff.totalTelat} Telat", StatAmberFg)
                    if (staff.totalTelatToleransi > 0) CountText("${staff.totalTelatToleransi} Telat (Tol)", Color(0xFF854D0E))
                    if (staff.totalAlpha > 0) CountText("${staff.totalAlpha} Alpha", StatRoseFg)
                    if (staff.totalCepat > 0) CountText("${staff.totalCepat} Plg Cepat", StatSkyFg)
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
                Icons.Filled.ChevronRight,
                contentDescription = "Lihat detail ${staff.name}",
                tint = StitchOutlineVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun CountText(text: String, color: Color) {
    Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = color, maxLines = 1)
}

@Composable
private fun ClockChip(isIn: Boolean, row: AttendanceRow) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = StitchSurfaceContainerLow,
        border = BorderStroke(1.dp, StitchSurfaceContainerHigh.copy(alpha = 0.8f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                if (isIn) Icons.AutoMirrored.Filled.Login else Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                tint = if (isIn) StatEmeraldFg else StatAmberFg,
                modifier = Modifier.size(12.dp),
            )
            Text(if (isIn) "In:" else "Out:", fontSize = 11.sp, color = StitchOnSurfaceVariant)
            Text(row.jam, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StitchOnSurface)
            row.telatMenit?.takeIf { it > 0 }?.let { menit ->
                Text(
                    if (isIn) "Telat ${menit}m" else "Cepat ${menit}m",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = StatRoseFg,
                )
            }
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
            .background(StitchSecondaryContainer.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.trim().take(1).uppercase(ID_LOCALE).ifBlank { "?" },
            color = StitchPrimary,
            fontWeight = FontWeight.Bold,
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
        containerColor = StitchSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = StitchOutlineVariant) },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StaffAvatar(name = staff.name, photoPath = staff.latestPhotoPath, size = 42)
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(staff.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = StitchOnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Detail Kehadiran", fontSize = 12.sp, color = StitchOnSurfaceVariant)
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, contentDescription = "Tutup", tint = StitchOnSurfaceVariant)
            }
        }

        HorizontalDivider(color = StitchOutlineVariant.copy(alpha = 0.3f))

        if (days.isEmpty()) {
            Text(
                "Tidak ada riwayat detail.",
                fontSize = 13.sp,
                color = StitchOnSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(40.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = StitchSurfaceContainerLowest,
        border = BorderStroke(1.dp, StitchSurfaceContainerHigh.copy(alpha = 0.7f)),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    day.label.uppercase(ID_LOCALE),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = StitchOnSurfaceVariant,
                )
                if (day.isAlpha) StatusPill("alpha", null)
            }
            HorizontalDivider(color = StitchOutlineVariant.copy(alpha = 0.2f))

            if (!day.isAlpha) {
                Spacer(Modifier.height(12.dp))
                AttendanceDetailRow(isIn = true, row = day.masuk, onPreview = onPreview)
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = StitchOutlineVariant.copy(alpha = 0.2f))
                Spacer(Modifier.height(10.dp))
                AttendanceDetailRow(isIn = false, row = day.pulang, onPreview = onPreview)
            }
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
                .clip(RoundedCornerShape(12.dp))
                .background(StitchSurfaceContainerLow)
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
                    tint = StitchOutlineVariant,
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
                        tint = if (isIn) StatEmeraldFg else StatAmberFg,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(if (isIn) "Masuk" else "Pulang", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = StitchOnSurface)
                }
                row?.let { StatusPill(it.status, it.telatMenit) }
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (row != null) {
                    Surface(shape = RoundedCornerShape(7.dp), color = StitchSurfaceContainerLow) {
                        Text(
                            row.jam,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = StitchOnSurface,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        )
                    }
                    if (row.isManual) {
                        Surface(shape = RoundedCornerShape(6.dp), color = StatAmberBg, border = BorderStroke(1.dp, StatAmberLine)) {
                            Text(
                                "Manual",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatAmberFg,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            )
                        }
                    }
                } else {
                    Text(
                        if (isIn) "Belum / Tidak ada data" else "Belum Absen Pulang",
                        fontSize = 12.sp,
                        color = StitchOnSurfaceVariant.copy(alpha = 0.8f),
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
    Surface(shape = RoundedCornerShape(8.dp), color = bg) {
        Text(
            rekapStatusText(status) + suffix,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = fg,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
        )
    }
}

/* --------------------------------------------------------- Photo preview */

@Composable
private fun PhotoPreviewDialog(url: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
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
private fun RekapErrorCard(message: String, onRetry: () -> Unit) {
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
                        if (noOutlet) Icons.Filled.Storefront else Icons.Filled.PriorityHigh,
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
