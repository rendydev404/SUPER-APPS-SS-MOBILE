package com.sukashawarma.superapp.presentation.absensi.papan

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.sukashawarma.superapp.domain.util.JakartaTime
import com.sukashawarma.superapp.presentation.absensi.AbsensiBottomNav
import com.sukashawarma.superapp.presentation.absensi.rekap.selfiePublicUrl
import java.time.LocalDate
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

// Warna status — dipetakan dari kelas Tailwind di halaman web.
private val DotGreen = Color(0xFF059669)
private val DotYellow = Color(0xFFFACC15)
private val DotAmber = Color(0xFFF59E0B)
private val DotGray = Color(0xFFD1D5DB)
private val DotRed = Color(0xFFEF4444)
private val DotIndigo = Color(0xFF6366F1)

private val TextGreen = Color(0xFF15803D)
private val BgGreen = Color(0xFFECFDF5)
private val TextYellow = Color(0xFFA16207)
private val BgYellow = Color(0xFFFEFCE8)
private val TextAmber = Color(0xFFB45309)
private val BgAmber = Color(0xFFFFFBEB)
private val TextGray = Color(0xFF374151)
private val BgGray = Color(0xFFF9FAFB)
private val TextRed = Color(0xFFB91C1C)
private val BgRed = Color(0xFFFEF2F2)
private val TextIndigo = Color(0xFF4338CA)
private val BgIndigo = Color(0xFFEEF2FF)

private val ID_LOCALE = Locale("id", "ID")
private val DateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", ID_LOCALE)

/** (titik, teks, latar) per status — dipakai bareng oleh pil, dot avatar, dan legenda. */
private fun stateColors(state: BoardState): Triple<Color, Color, Color> = when (state) {
    BoardState.MASUK, BoardState.KELUAR -> Triple(DotGreen, TextGreen, BgGreen)
    BoardState.TELAT_TOLERANSI -> Triple(DotYellow, TextYellow, BgYellow)
    BoardState.TELAT, BoardState.PULANG_TELAT, BoardState.LEBIH_AWAL -> Triple(DotAmber, TextAmber, BgAmber)
    BoardState.ALPHA -> Triple(DotRed, TextRed, BgRed)
    BoardState.BELUM -> Triple(DotGray, TextGray, BgGray)
}

private fun stateIcon(state: BoardState): ImageVector = when (state) {
    BoardState.MASUK -> Icons.AutoMirrored.Filled.Login
    BoardState.TELAT, BoardState.TELAT_TOLERANSI, BoardState.PULANG_TELAT -> Icons.Filled.Schedule
    BoardState.KELUAR, BoardState.LEBIH_AWAL -> Icons.AutoMirrored.Filled.Logout
    BoardState.BELUM, BoardState.ALPHA -> Icons.Filled.MoreHoriz
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PapanKehadiranScreen(
    onExit: () -> Unit,
    onNavigateTab: (Int) -> Unit = {},
    viewModel: PapanKehadiranViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    var previewUrl by remember { mutableStateOf<String?>(null) }

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
                    Text("Papan Kehadiran", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = StitchPrimary)
                },
            )
        },
        // Papan Kehadiran diakses dari tab "More" (index 3) — bottom nav tetap tampil supaya
        // user bisa lompat tab tanpa balik dulu, sama seperti Cuti & Kasbon.
        bottomBar = { AbsensiBottomNav(selectedIndex = 3, onSelect = onNavigateTab) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "header") {
                PapanHeader(
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
                        message = "Anda memantau seluruh outlet. Pilih salah satu di atas untuk melihat papan kehadirannya.",
                    )
                }
                return@LazyColumn
            }

            when {
                state.loading -> item(key = "loading") {
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 60.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator(color = StitchPrimary, strokeWidth = 3.dp)
                        Spacer(Modifier.height(14.dp))
                        Text("Memuat data kehadiran...", fontSize = 13.sp, color = StitchOnSurfaceVariant)
                    }
                }

                state.error != null -> item(key = "error") {
                    PapanErrorCard(message = state.error.orEmpty(), onRetry = { viewModel.load() })
                }

                else -> {
                    if (state.canSeeAlerts && state.alerts.isNotEmpty()) {
                        item(key = "alerts") { SecurityAlertCard(state.alerts) }
                    }

                    item(key = "summary") { AttendanceRateCard(state.summary) }

                    item(key = "list_header") {
                        ListHeader(
                            shown = state.filteredRows.size,
                            total = state.summary.total,
                            filter = state.filter,
                            onFilter = { viewModel.setFilter(it) },
                        )
                    }

                    item(key = "search") {
                        StaffSearchField(query = state.query, onQueryChange = { viewModel.setQuery(it) })
                    }

                    if (state.filteredRows.isEmpty()) {
                        item(key = "empty") {
                            EmptyCard(
                                icon = { Icon(Icons.Filled.Group, null, tint = StitchPrimary, modifier = Modifier.size(26.dp)) },
                                title = "Tidak Ada Data Staf",
                                message = if (state.query.isBlank()) "Tidak ada staf yang sesuai dengan filter yang dipilih."
                                else "Tidak ada staf bernama \"${state.query.trim()}\" pada filter ini.",
                            )
                        }
                    } else {
                        items(state.filteredRows, key = { it.staffId }) { row ->
                            StaffBoardCard(row = row, onPreview = { previewUrl = it })
                        }
                    }
                }
            }
        }
    }

    previewUrl?.let { url ->
        PhotoPreviewDialog(url = url, onDismiss = { previewUrl = null })
    }
}

/* ---------------------------------------------------------------- Header */

@Composable
private fun PapanHeader(
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
                Icon(Icons.Filled.Group, contentDescription = null, tint = StitchPrimary, modifier = Modifier.size(21.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                LocalDate.now(JakartaTime.ZONE).format(DateFormatter),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = StitchOnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                outletName ?: "Pantau kehadiran tim hari ini",
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

/* --------------------------------------------------------- Security alert */

@Composable
private fun SecurityAlertCard(alerts: List<SecurityAlert>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = BgRed,
        border = BorderStroke(1.dp, DotRed.copy(alpha = 0.35f)),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Warning, contentDescription = null, tint = DotRed, modifier = Modifier.size(18.dp))
                Text(
                    "Peringatan Keamanan: ${alerts.size} percobaan manipulasi lokasi",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextRed,
                )
            }
            Spacer(Modifier.height(10.dp))
            alerts.forEach { alert ->
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = StitchSurfaceContainerLowest,
                    border = BorderStroke(1.dp, DotRed.copy(alpha = 0.18f)),
                ) {
                    Column(Modifier.padding(horizontal = 11.dp, vertical = 9.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                alert.staffName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = StitchOnSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Text(alert.time, fontSize = 10.5.sp, color = StitchOnSurfaceVariant)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(alert.label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextRed)
                    }
                }
            }
        }
    }
}

/* -------------------------------------------------------- Attendance rate */

@Composable
private fun AttendanceRateCard(summary: BoardSummary) {
    val anim by animateFloatAsState(
        targetValue = if (summary.total > 0) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "papan-bar",
    )

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
                Column(Modifier.weight(1f)) {
                    Text("Tingkat Kehadiran", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = StitchOnSurface)
                    Text("Persentase staf yang sudah hadir hari ini", fontSize = 12.sp, color = StitchOnSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${summary.percent}", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = StitchOnSurface)
                    Text("%", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = StitchOnSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            // Bar bertumpuk: hadir → telat toleransi → telat → alpha, sisanya track kosong.
            val segments = listOf(
                summary.fraction(summary.hadir) * anim to DotGreen,
                summary.fraction(summary.telatToleransi) * anim to DotYellow,
                summary.fraction(summary.telat) * anim to DotAmber,
                summary.fraction(summary.alpha) * anim to DotRed,
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(CircleShape)
                    .background(StitchSurfaceContainerLow),
            ) {
                var used = 0f
                segments.forEach { (fraction, color) ->
                    if (fraction > 0.001f) {
                        used += fraction
                        Box(Modifier.fillMaxHeight().weight(fraction).background(color))
                    }
                }
                val rest = 1f - used
                if (rest > 0.001f) Spacer(Modifier.weight(rest))
            }

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LegendTile(Modifier.weight(1f), "Hadir", summary.hadir, DotGreen, TextGreen, BgGreen)
                LegendTile(Modifier.weight(1f), "Telat (Tol)", summary.telatToleransi, DotYellow, TextYellow, BgYellow)
                LegendTile(Modifier.weight(1f), "Telat", summary.telat, DotAmber, TextAmber, BgAmber)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LegendTile(Modifier.weight(1f), "Belum", summary.belum, DotGray, TextGray, BgGray)
                LegendTile(Modifier.weight(1f), "Alpha", summary.alpha, DotRed, TextRed, BgRed)
                LegendTile(Modifier.weight(1f), "Total Staf", summary.total, DotIndigo, TextIndigo, BgIndigo)
            }
        }
    }
}

@Composable
private fun LegendTile(modifier: Modifier, label: String, value: Int, dot: Color, fg: Color, bg: Color) {
    Surface(modifier = modifier, shape = RoundedCornerShape(14.dp), color = bg, border = BorderStroke(1.dp, dot.copy(alpha = 0.25f))) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 9.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
                Text("$value", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = StitchOnSurface)
            }
            Spacer(Modifier.height(4.dp))
            Text(label, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/* ------------------------------------------------------------ List header */

@Composable
private fun ListHeader(shown: Int, total: Int, filter: BoardState?, onFilter: (BoardState?) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(shape = RoundedCornerShape(11.dp), color = StitchSurfaceContainer, modifier = Modifier.size(36.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Group, contentDescription = null, tint = StitchPrimary, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("Daftar Staf", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = StitchOnSurface)
            Text("$shown dari $total staf", fontSize = 11.5.sp, color = StitchOnSurfaceVariant)
        }
        StatusFilterMenu(selected = filter, onSelect = onFilter)
    }
}

@Composable
private fun StaffSearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
        placeholder = { Text("Cari nama staf", fontSize = 14.sp, color = StitchOnSurfaceVariant) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = StitchOnSurfaceVariant, modifier = Modifier.size(19.dp)) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Clear, contentDescription = "Hapus pencarian", tint = StitchOnSurfaceVariant)
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
}

@Composable
private fun StatusFilterMenu(selected: BoardState?, onSelect: (BoardState?) -> Unit) {
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
                Text(
                    selected?.filterLabel ?: "Semua Status",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = StitchOnSurface,
                    maxLines = 1,
                )
                Icon(Icons.Filled.ExpandMore, contentDescription = null, tint = StitchOnSurfaceVariant, modifier = Modifier.size(16.dp))
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = {
                    Text(
                        "Semua Status",
                        fontSize = 14.sp,
                        fontWeight = if (selected == null) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected == null) StitchPrimary else StitchOnSurface,
                    )
                },
                onClick = { onSelect(null); expanded = false },
            )
            BoardState.entries.forEach { boardState ->
                val (dot, _, _) = stateColors(boardState)
                DropdownMenuItem(
                    text = {
                        Text(
                            boardState.filterLabel,
                            fontSize = 14.sp,
                            fontWeight = if (boardState == selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (boardState == selected) StitchPrimary else StitchOnSurface,
                        )
                    },
                    leadingIcon = { Box(Modifier.size(9.dp).clip(CircleShape).background(dot)) },
                    onClick = { onSelect(boardState); expanded = false },
                )
            }
        }
    }
}

/* -------------------------------------------------------------- Staff row */

@Composable
private fun StaffBoardCard(row: StaffBoardRow, onPreview: (String) -> Unit) {
    val (dot, fg, bg) = stateColors(row.state)
    val url = remember(row.selfiePath) { selfiePublicUrl(row.selfiePath) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = StitchSurfaceContainerLowest,
        border = BorderStroke(1.dp, StitchSurfaceContainerHigh.copy(alpha = 0.7f)),
        shadowElevation = 0.5.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(StitchSecondaryContainer.copy(alpha = 0.18f))
                        .then(if (url != null) Modifier.clickable { onPreview(url) } else Modifier),
                    contentAlignment = Alignment.Center,
                ) {
                    if (url != null) {
                        AsyncImage(
                            model = url,
                            contentDescription = "Selfie ${row.name}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Text(
                            row.name.trim().take(1).uppercase(ID_LOCALE).ifBlank { "?" },
                            color = StitchPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                        )
                    }
                }
                // Titik status di pojok avatar — penanda cepat sebelum membaca pil.
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 3.dp, y = 3.dp)
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(StitchSurfaceContainerLowest)
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(dot)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        row.name,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchOnSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (row.isManual) {
                        Surface(shape = RoundedCornerShape(6.dp), color = BgAmber, border = BorderStroke(1.dp, DotAmber.copy(alpha = 0.4f))) {
                            Text(
                                "Manual",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextAmber,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(row.roleLabel, fontSize = 11.5.sp, color = StitchOnSurfaceVariant, maxLines = 1)
                Spacer(Modifier.height(7.dp))
                Surface(shape = RoundedCornerShape(9.dp), color = bg, border = BorderStroke(1.dp, dot.copy(alpha = 0.3f))) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(stateIcon(row.state), contentDescription = null, tint = fg, modifier = Modifier.size(13.dp))
                        Text(
                            boardPillLabel(row.state, row.time, row.delayMinutes),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = fg,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

/* ---------------------------------------------------------- Outlet picker */

@Composable
private fun OutletPickerCard(
    outlets: List<PapanOutletOption>,
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

/* --------------------------------------------------------- Preview/states */

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
private fun PapanErrorCard(message: String, onRetry: () -> Unit) {
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
