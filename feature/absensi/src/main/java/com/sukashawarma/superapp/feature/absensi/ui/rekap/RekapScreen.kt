package com.sukashawarma.superapp.presentation.absensi.rekap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dtFmt = DateTimeFormatter.ofPattern("d MMM, HH:mm").withZone(ZoneId.of("Asia/Jakarta"))
private fun fmtDateTime(iso: String): String = runCatching { dtFmt.format(Instant.parse(iso)) }.getOrDefault(iso)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RekapScreen(onExit: () -> Unit, viewModel: RekapViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rekap & Riwayat") },
                navigationIcon = {
                    IconButton(onClick = onExit) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            RangeSelector(selected = state.rangeDays, onSelect = { viewModel.setRangeDays(it) })
            Box(Modifier.fillMaxSize()) {
                when {
                    state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    state.error != null -> Text(
                        state.error ?: "",
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        color = MaterialTheme.colorScheme.error,
                    )
                    state.rows.isEmpty() -> Text("Belum ada riwayat pada rentang ini.", modifier = Modifier.align(Alignment.Center))
                    else -> LazyColumn {
                        items(state.rows) { row -> HistoryItem(row) }
                    }
                }
            }
        }
    }
}

@Composable
private fun RangeSelector(selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(7 to "7 Hari", 30 to "30 Hari").forEach { (days, label) ->
            FilterChip(selected = selected == days, onClick = { onSelect(days) }, label = { Text(label) })
        }
    }
}

@Composable
private fun HistoryItem(row: AttendanceHistoryRow) {
    val (color, label) = if (row.type == "in") Color(0xFF059669) to "Masuk" else Color(0xFFD97706) to "Pulang"
    ListItem(
        headlineContent = { Text(row.staffName?.let { "$it — $label" } ?: label) },
        supportingContent = { Text(fmtDateTime(row.tsServerIso)) },
        trailingContent = {
            Box(
                Modifier.clip(RoundedCornerShape(999.dp)).background(color.copy(alpha = 0.15f)).padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(row.status ?: label, color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
    )
    Divider()
}
