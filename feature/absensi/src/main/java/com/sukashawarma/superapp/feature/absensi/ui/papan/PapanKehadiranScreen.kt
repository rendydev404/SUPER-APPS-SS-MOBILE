package com.sukashawarma.superapp.presentation.absensi.papan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.presentation.absensi.AbsensiBottomNav
import com.sukashawarma.superapp.presentation.theme.StatusAmber
import com.sukashawarma.superapp.presentation.theme.StatusEmerald
import com.sukashawarma.superapp.presentation.theme.StatusRed
import com.sukashawarma.superapp.presentation.theme.SukaOnSurface
import com.sukashawarma.superapp.presentation.theme.SukaOnSurfaceVariant
import com.sukashawarma.superapp.presentation.theme.SukaOrange
import com.sukashawarma.superapp.presentation.theme.SukaSurface
import com.sukashawarma.superapp.presentation.theme.SukaSurfaceContainer
import com.sukashawarma.superapp.presentation.theme.SukaSurfaceContainerHighest
import com.sukashawarma.superapp.presentation.theme.SukaSurfaceContainerLowest
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFmt = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.of("Asia/Jakarta"))
private fun fmtTime(iso: String?): String? = iso?.let { runCatching { timeFmt.format(Instant.parse(it)) }.getOrNull() }

private enum class BoardTab(val label: String) { SEMUA("Semua"), HADIR("Hadir"), BELUM("Belum") }

/**
 * Papan Kehadiran — mengikuti desain Stitch "Attendance Board - Grouped List Variant"
 * (project 16991912726833518585, screen c42414a3112345a7ba8e2123aa5d4272): kartu ringkasan
 * dengan progress bar, pencarian karyawan, tab filter Semua/Hadir/Belum, dan daftar
 * dikelompokkan dalam satu kartu (bukan kartu terpisah per baris).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PapanKehadiranScreen(
    onExit: () -> Unit,
    onNavigateTab: (Int) -> Unit = {},
    viewModel: PapanKehadiranViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    var query by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf(BoardTab.SEMUA) }

    Scaffold(
        containerColor = SukaSurface,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                title = { Text("Papan Kehadiran", fontWeight = FontWeight.Bold, color = SukaOrange) },
                navigationIcon = {
                    IconButton(onClick = onExit) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali") }
                },
                actions = {
                    IconButton(onClick = { viewModel.load() }) { Icon(Icons.Filled.Refresh, contentDescription = "Muat ulang") }
                }
            )
        },
        // Papan Kehadiran diakses dari tab "More" (index 3) — bottom nav tetap tampil supaya
        // user bisa lompat tab tanpa balik dulu, sama seperti Cuti & Kasbon.
        bottomBar = { AbsensiBottomNav(selectedIndex = 3, onSelect = onNavigateTab) },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize().background(SukaSurface)) {
            when {
                state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.error != null -> Text(
                    state.error ?: "",
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    color = MaterialTheme.colorScheme.error,
                )
                state.rows.isEmpty() -> Text(
                    "Belum ada staff aktif di outlet ini.",
                    modifier = Modifier.align(Alignment.Center),
                    color = SukaOnSurfaceVariant,
                )
                else -> {
                    val filtered = state.rows
                        .filter { row ->
                            when (tab) {
                                BoardTab.SEMUA -> true
                                BoardTab.HADIR -> row.state != StaffAttendanceState.BELUM_ABSEN
                                BoardTab.BELUM -> row.state == StaffAttendanceState.BELUM_ABSEN
                            }
                        }
                        .filter { it.name.contains(query, ignoreCase = true) }

                    Column(Modifier.fillMaxSize()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            SummaryCard(state.rows)
                            SearchField(query = query, onQueryChange = { query = it })
                            BoardTabs(selected = tab, onSelect = { tab = it })
                        }

                        LazyColumn(Modifier.weight(1f)) {
                            if (filtered.isEmpty()) {
                                item {
                                    Text(
                                        "Tidak ada karyawan yang cocok.",
                                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                                        color = SukaOnSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            } else {
                                item {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        color = SukaSurfaceContainerLowest,
                                        border = BorderStroke(1.dp, SukaSurfaceContainerHighest),
                                    ) {
                                        Column {
                                            filtered.forEachIndexed { index, row ->
                                                StaffBoardItem(row)
                                                if (index != filtered.lastIndex) {
                                                    HorizontalDivider(color = SukaSurfaceContainerHighest)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            item { Spacer(Modifier.height(24.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(rows: List<StaffBoardRow>) {
    val hadir = rows.count { it.state != StaffAttendanceState.BELUM_ABSEN }
    val total = rows.size
    val progress = if (total > 0) hadir.toFloat() / total.toFloat() else 0f
    val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = progress,
        animationSpec = androidx.compose.animation.core.tween(700, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "boardProgress",
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SukaSurfaceContainerLowest,
        border = BorderStroke(1.dp, SukaSurfaceContainerHighest),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Kehadiran Hari Ini", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SukaOnSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(hadir.toString(), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = SukaOrange)
                    Spacer(Modifier.width(4.dp))
                    Text("/ $total hadir", fontSize = 14.sp, color = SukaOnSurfaceVariant)
                }
            }
            Box(
                modifier = Modifier
                    .width(110.dp)
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(SukaSurfaceContainerHighest)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(50))
                        .background(SukaOrange)
                )
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Cari karyawan...", color = SukaOnSurfaceVariant) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = SukaOnSurfaceVariant) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SukaOrange,
            unfocusedBorderColor = SukaSurfaceContainerHighest,
            focusedContainerColor = SukaSurfaceContainerLowest,
            unfocusedContainerColor = SukaSurfaceContainerLowest,
            cursorColor = SukaOrange,
        ),
    )
}

@Composable
private fun BoardTabs(selected: BoardTab, onSelect: (BoardTab) -> Unit) {
    Row(Modifier.fillMaxWidth()) {
        BoardTab.entries.forEach { t ->
            val isSelected = t == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSelect(t) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    t.label,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) SukaOrange else SukaOnSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(if (isSelected) SukaOrange else Color.Transparent)
                )
            }
        }
    }
}

@Composable
private fun StaffBoardItem(row: StaffBoardRow) {
    val (color, label) = when (row.state) {
        StaffAttendanceState.BELUM_ABSEN -> StatusRed to "Belum Absen"
        StaffAttendanceState.SUDAH_MASUK -> StatusEmerald to "Masuk ${fmtTime(row.inTime) ?: ""}"
        StaffAttendanceState.SUDAH_PULANG -> StatusAmber to "Pulang ${fmtTime(row.outTime) ?: ""}"
    }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) { Text(row.name.firstOrNull()?.uppercase() ?: "?", color = color, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(12.dp))
            Text(row.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = SukaOnSurface)
        }
        Box(
            Modifier.clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = 0.15f)).padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}
