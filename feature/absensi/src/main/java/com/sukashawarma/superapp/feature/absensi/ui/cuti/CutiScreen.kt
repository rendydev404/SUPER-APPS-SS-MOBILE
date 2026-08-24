package com.sukashawarma.superapp.presentation.absensi.cuti

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Storefront
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
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.presentation.theme.StatusAmber
import com.sukashawarma.superapp.presentation.theme.StatusEmerald
import com.sukashawarma.superapp.presentation.theme.StatusRed
import com.sukashawarma.superapp.presentation.theme.SukaOnSurface
import com.sukashawarma.superapp.presentation.theme.SukaOnSurfaceVariant
import com.sukashawarma.superapp.presentation.theme.SukaOrange
import com.sukashawarma.superapp.presentation.theme.SukaPrimary
import com.sukashawarma.superapp.presentation.theme.SukaSurface
import com.sukashawarma.superapp.presentation.theme.SukaSurfaceContainer
import com.sukashawarma.superapp.presentation.theme.SukaSurfaceContainerLowest
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CutiScreen(
    onExit: () -> Unit,
    onNavigateTab: (Int) -> Unit = {},
    viewModel: CutiViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val staff by AppSession.staff.collectAsState()
    var showForm by remember { mutableStateOf(false) }

    Scaffold(
        // Cuti & Izin diakses dari tab "More" (index 3) di hub — bottom nav tetap tampil di
        // sini (bukan cuma di 4 tab utama) supaya user bisa lompat tab tanpa balik dulu.
        bottomBar = { AbsensiBottomNav(selectedIndex = 3, onSelect = onNavigateTab) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Storefront, contentDescription = null, tint = SukaOrange)
                        Spacer(Modifier.width(8.dp))
                        Text("outlet tes", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                },
                actions = {
                    Box(modifier = Modifier.padding(end = 16.dp).size(36.dp).background(SukaOrange, CircleShape), contentAlignment = Alignment.Center) {
                        Text(staff?.name?.take(1)?.uppercase() ?: "?", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().background(SukaSurface)) {
            Column(Modifier.padding(16.dp)) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(SukaSurfaceContainer), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = SukaPrimary)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("Cuti & Izin", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = SukaOnSurface)
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = { showForm = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SukaPrimary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Ajukan Cuti", fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(24.dp))

                // Stats Cards
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Card(
                        Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Total Kuota Tahunan", color = SukaOnSurfaceVariant, fontSize = 12.sp)
                            Spacer(Modifier.height(4.dp))
                            Text("12 hari", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = SukaOnSurface)
                        }
                    }
                    Card(
                        Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Cuti Terpakai", color = SukaOnSurfaceVariant, fontSize = 12.sp)
                            Spacer(Modifier.height(4.dp))
                            val used = state.rows.filter { it.status == "approved" }.sumOf { it.days }
                            Text("$used hari", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = SukaOnSurface)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                Text("Riwayat Cuti", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = SukaOnSurface)
                Spacer(Modifier.height(16.dp))
            }

            Box(Modifier.fillMaxSize()) {
                when {
                    state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    state.error != null -> Text(
                        state.error ?: "",
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        color = MaterialTheme.colorScheme.error,
                    )
                    state.rows.isEmpty() -> Text("Belum ada pengajuan cuti.", modifier = Modifier.align(Alignment.Center), color = SukaOnSurfaceVariant)
                    else -> LazyColumn(contentPadding = PaddingValues(bottom = 24.dp, start = 16.dp, end = 16.dp)) {
                        items(state.rows, key = { it.id }) { row -> CutiItem(row) }
                    }
                }
            }
        }
    }

    if (showForm) {
        var wasSubmitting by remember { mutableStateOf(false) }
        LaunchedEffect(state.submitting) {
            if (wasSubmitting && !state.submitting && state.submitError == null) showForm = false
            wasSubmitting = state.submitting
        }
        CutiFormSheet(
            submitting = state.submitting,
            error = state.submitError,
            onDismiss = { showForm = false; viewModel.clearSubmitError() },
            onSubmit = { type, start, end, reason -> viewModel.submit(type, start, end, reason) },
        )
    }
}

@Composable
private fun CutiItem(row: CutiRequestRow) {
    val (color, label) = statusColorLabel(row.status)
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("id", "ID"))
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    ) {
        Row(
            Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(Modifier.weight(1f)) {
                Text(row.leaveType.replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SukaOnSurface)
                Spacer(Modifier.height(4.dp))
                Text("${row.startDate.format(formatter)} - ${row.endDate.format(formatter)}", color = SukaOnSurfaceVariant, fontSize = 13.sp)
                if (row.reason.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(row.reason, color = SukaOnSurfaceVariant, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Box(Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(6.dp))
                Text("${row.days} hari", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = SukaOnSurface)
            }
        }
    }
}

private fun statusColorLabel(status: String): Pair<Color, String> = when (status) {
    "approved" -> StatusEmerald to "Disetujui"
    "rejected" -> StatusRed to "Ditolak"
    else -> StatusAmber to "Menunggu"
}

private val LEAVE_TYPE_LABELS = LEAVE_TYPES.map { it.replaceFirstChar(Char::uppercase) to it }

/**
 * Bottom sheet "Ajukan Cuti" — mengikuti desain Stitch (project 16991912726833518585,
 * screen 1d8f30c703d741eab8e54bc59b4e64d6): handle drag, header judul tengah + tombol
 * tutup, pill jenis cuti, field tanggal bergaya filled dengan ikon, alasan opsional,
 * tombol Ajukan penuh + Batal teks-saja.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CutiFormSheet(
    submitting: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSubmit: (String, LocalDate, LocalDate, String) -> Unit,
) {
    var leaveType by remember { mutableStateOf(LEAVE_TYPE_LABELS.first().second) }
    var reason by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf<LocalDate?>(null) }
    var endDate by remember { mutableStateOf<LocalDate?>(null) }
    var pickingStart by remember { mutableStateOf(false) }
    var pickingEnd by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("id", "ID")) }

    fun dismissAnimated() {
        scope.launch { sheetState.hide() }.invokeOnCompletion { if (!sheetState.isVisible) onDismiss() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SukaSurfaceContainerLowest,
        dragHandle = { BottomSheetDefaults.DragHandle(color = SukaOnSurfaceVariant.copy(alpha = 0.35f)) },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header
            Box(Modifier.fillMaxWidth().padding(bottom = 24.dp), contentAlignment = Alignment.Center) {
                Text("Ajukan Cuti", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SukaOnSurface)
                IconButton(onClick = { dismissAnimated() }, modifier = Modifier.align(Alignment.CenterEnd).size(40.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup", tint = SukaOnSurfaceVariant)
                }
            }

            // Jenis Cuti
            Text(
                "Jenis Cuti",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = SukaOnSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            ) {
                LEAVE_TYPE_LABELS.forEach { (label, value) ->
                    LeaveTypePill(label = label, selected = leaveType == value, onClick = { leaveType = value })
                }
            }

            Spacer(Modifier.height(24.dp))

            // Tanggal Mulai
            Text("Tanggal Mulai", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = SukaOnSurface)
            Spacer(Modifier.height(4.dp))
            DateInputField(
                icon = Icons.Default.CalendarToday,
                placeholder = "Pilih tanggal mulai",
                value = startDate?.format(dateFormatter),
                onClick = { pickingStart = true },
            )

            Spacer(Modifier.height(16.dp))

            // Tanggal Selesai
            Text("Tanggal Selesai", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = SukaOnSurface)
            Spacer(Modifier.height(4.dp))
            DateInputField(
                icon = Icons.Default.Event,
                placeholder = "Pilih tanggal selesai",
                value = endDate?.format(dateFormatter),
                onClick = { pickingEnd = true },
            )

            Spacer(Modifier.height(16.dp))

            // Alasan
            Text("Alasan (Opsional)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = SukaOnSurface)
            Spacer(Modifier.height(4.dp))
            TextField(
                value = reason,
                onValueChange = { reason = it },
                placeholder = { Text("Tulis alasan Anda di sini...", color = SukaOnSurfaceVariant) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = SukaSurfaceContainer,
                    unfocusedContainerColor = SukaSurfaceContainer,
                    disabledContainerColor = SukaSurfaceContainer,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    cursorColor = SukaPrimary,
                    focusedTextColor = SukaOnSurface,
                    unfocusedTextColor = SukaOnSurface,
                ),
            )

            if (error != null) {
                Spacer(Modifier.height(12.dp))
                Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }

            Spacer(Modifier.height(24.dp))

            // Actions
            val canSubmit = !submitting && startDate != null && endDate != null
            Button(
                onClick = { if (startDate != null && endDate != null) onSubmit(leaveType, startDate!!, endDate!!, reason) },
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SukaPrimary),
            ) {
                Text(if (submitting) "Mengirim..." else "Ajukan", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = { dismissAnimated() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = SukaPrimary),
            ) {
                Text("Batal", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (pickingStart) {
        DatePickerModal(onDismiss = { pickingStart = false }) { millis ->
            startDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
            pickingStart = false
        }
    }
    if (pickingEnd) {
        DatePickerModal(onDismiss = { pickingEnd = false }) { millis ->
            endDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
            pickingEnd = false
        }
    }
}

@Composable
private fun LeaveTypePill(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) SukaPrimary else SukaSurfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(
            label,
            color = if (selected) Color.White else SukaOnSurfaceVariant,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun DateInputField(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    placeholder: String,
    value: String?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SukaSurfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = SukaOnSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            value ?: placeholder,
            color = if (value != null) SukaOnSurface else SukaOnSurfaceVariant,
            fontSize = 16.sp,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerModal(onDismiss: () -> Unit, onDatePicked: (Long) -> Unit) {
    val state = rememberDatePickerState()
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { state.selectedDateMillis?.let(onDatePicked) ?: onDismiss() }, colors = ButtonDefaults.textButtonColors(contentColor = SukaPrimary)) { Text("Pilih") }
        },
        dismissButton = { TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = SukaOnSurfaceVariant)) { Text("Batal") } },
    ) { DatePicker(state = state) }
}
