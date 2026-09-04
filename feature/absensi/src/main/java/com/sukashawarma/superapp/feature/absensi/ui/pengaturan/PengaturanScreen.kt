package com.sukashawarma.superapp.presentation.absensi.pengaturan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ShareLocation
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

internal val StitchBackground = Color(0xFFF9F9FC)
internal val StitchSurfaceLow = Color(0xFFF3F3F6)
internal val StitchSurfaceVariant = Color(0xFFE2E2E5)
internal val StitchOnSurface = Color(0xFF1A1C1E)
internal val StitchSecondary = Color(0xFF635D59)
internal val StitchTertiary = Color(0xFF5E5E5E)
internal val StitchPrimary = Color(0xFFA23F00)
internal val StitchPrimaryContainer = Color(0xFFF27A3D)
internal val StitchDanger = Color(0xFFB3261E)
internal val StitchSuccess = Color(0xFF16803C)

private enum class WorkTimeTarget(val title: String) {
    MASUK("Pilih jam masuk"),
    KELUAR("Pilih jam keluar"),
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PengaturanScreen(onExit: () -> Unit, viewModel: PengaturanViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()

    var jamMasuk by remember(state.loading) { mutableStateOf(state.jamMasuk) }
    var jamKeluar by remember(state.loading) { mutableStateOf(state.jamKeluar) }
    var toleransi by remember(state.loading) { mutableStateOf(state.toleransiMenit.toString()) }
    var radius by remember(state.loading) { mutableStateOf(state.radiusM.toString()) }
    var timePickerTarget by remember { mutableStateOf<WorkTimeTarget?>(null) }

    // Jadwal khusus per outlet: draft form yang sedang dibuka, plus dialog konfirmasi hapus.
    var jadwalDraft by remember { mutableStateOf<JadwalDraft?>(null) }
    var jadwalTimeTarget by remember { mutableStateOf<JadwalTimeTarget?>(null) }
    var hapusJadwal by remember { mutableStateOf<OutletSchedule?>(null) }
    var konfirmasiResetSemua by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = StitchBackground,
        topBar = {
            Column {
                TopAppBar(
                    modifier = Modifier.height(80.dp),
                    title = {
                        Text(
                            text = "Pengaturan",
                            color = StitchOnSurface,
                            fontSize = 28.sp,
                            lineHeight = 36.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp,
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onExit,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(StitchSurfaceLow),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kembali",
                                tint = StitchOnSurface,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = StitchBackground,
                        scrolledContainerColor = StitchBackground,
                    ),
                )
                HorizontalDivider(color = StitchSurfaceVariant)
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
        ) {
            when {
                state.loading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = StitchPrimary,
                )

                state.loadError != null -> Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = state.loadError.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                    )
                    Button(
                        onClick = viewModel::load,
                        colors = ButtonDefaults.buttonColors(containerColor = StitchPrimary),
                    ) {
                        Text("Coba lagi")
                    }
                }

                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    Text(
                        text = "Aturan pusat ini berlaku untuk seluruh outlet aktif.",
                        color = StitchSecondary,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 4.dp),
                    )

                    StitchSection(title = "Jam Kerja", progress = 0.33f) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            StitchTimeCard(
                                value = jamMasuk,
                                onClick = { timePickerTarget = WorkTimeTarget.MASUK },
                                label = "Masuk",
                                icon = Icons.Default.WbTwilight,
                                iconBackground = StitchPrimary.copy(alpha = 0.15f),
                                modifier = Modifier.weight(1f),
                            )
                            StitchTimeCard(
                                value = jamKeluar,
                                onClick = { timePickerTarget = WorkTimeTarget.KELUAR },
                                label = "Keluar",
                                icon = Icons.Default.Nightlight,
                                iconBackground = StitchSecondary.copy(alpha = 0.10f),
                                iconTint = StitchSecondary,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    StitchSection(title = "Toleransi", progress = 0.50f) {
                        StitchMetricCard(
                            label = "Keterlambatan",
                            value = toleransi,
                            onValueChange = { toleransi = it.filter(Char::isDigit) },
                            suffix = "menit",
                            icon = Icons.Default.Timer,
                        )
                        Spacer(Modifier.height(12.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = StitchSurfaceLow,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, StitchSurfaceVariant),
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = StitchPrimary,
                                    modifier = Modifier.size(20.dp),
                                )
                                Text(
                                    text = "Waktu tambahan sebelum karyawan dianggap terlambat.",
                                    color = StitchTertiary,
                                    fontSize = 14.sp,
                                    lineHeight = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }

                    StitchSection(title = "Lokasi", progress = 0.25f) {
                        StitchMetricCard(
                            label = "Radius Geofence",
                            value = radius,
                            onValueChange = { radius = it.filter(Char::isDigit) },
                            suffix = "meter",
                            icon = Icons.Default.ShareLocation,
                        )
                    }

                    StitchSection(title = "Jadwal Khusus Outlet", progress = 0.75f) {
                        JadwalKhususList(
                            state = state,
                            onAdd = {
                                viewModel.clearJadwalMessage()
                                jadwalDraft = JadwalDraft(
                                    jamMasuk = jamMasuk,
                                    jamKeluar = jamKeluar,
                                    toleransi = toleransi,
                                    radius = radius,
                                    mode = state.globalMode,
                                )
                            },
                            onEdit = {
                                viewModel.clearJadwalMessage()
                                jadwalDraft = it.toDraft()
                            },
                            onDelete = { hapusJadwal = it },
                            onResetAll = { konfirmasiResetSemua = true },
                        )
                    }

                    if (state.saveError != null) {
                        Text(
                            text = state.saveError.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    if (state.saved) {
                        Text(
                            text = "Pengaturan tersimpan.",
                            color = Color(0xFF16803C),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    SaveSettingsButton(
                        saving = state.saving,
                        onClick = {
                            viewModel.update(
                                jamMasuk.trim(),
                                jamKeluar.trim(),
                                toleransi.toIntOrNull() ?: 0,
                                radius.toIntOrNull() ?: 0,
                            )
                        },
                    )
                }
            }
        }
    }

    timePickerTarget?.let { target ->
        val currentValue = if (target == WorkTimeTarget.MASUK) jamMasuk else jamKeluar
        WorkTimePickerDialog(
            title = target.title,
            currentValue = currentValue,
            onDismiss = { timePickerTarget = null },
            onConfirm = { selectedTime ->
                if (target == WorkTimeTarget.MASUK) {
                    jamMasuk = selectedTime
                } else {
                    jamKeluar = selectedTime
                }
                timePickerTarget = null
            },
        )
    }

    jadwalDraft?.let { draft ->
        JadwalKhususDialog(
            draft = draft,
            outletTersedia = state.outletsTanpaJadwal,
            saving = state.savingJadwal,
            error = state.jadwalError,
            onDraftChange = { jadwalDraft = it },
            onPickTime = { jadwalTimeTarget = it },
            onDismiss = {
                jadwalDraft = null
                viewModel.clearJadwalMessage()
            },
            onConfirm = {
                viewModel.saveJadwalKhusus(
                    outletId = draft.outletId,
                    jamMasuk = draft.jamMasuk,
                    jamKeluar = draft.jamKeluar,
                    toleransiMenit = draft.toleransi.toIntOrNull() ?: 0,
                    radiusM = draft.radius.toIntOrNull() ?: 0,
                    mode = draft.mode,
                    onSuccess = { jadwalDraft = null },
                )
            },
        )
    }

    // Time picker milik dialog jadwal khusus — terpisah dari picker aturan pusat di atas.
    jadwalTimeTarget?.let { target ->
        val draft = jadwalDraft ?: return@let
        WorkTimePickerDialog(
            title = target.title,
            currentValue = if (target == JadwalTimeTarget.MASUK) draft.jamMasuk else draft.jamKeluar,
            onDismiss = { jadwalTimeTarget = null },
            onConfirm = { selectedTime ->
                jadwalDraft = if (target == JadwalTimeTarget.MASUK) {
                    draft.copy(jamMasuk = selectedTime)
                } else {
                    draft.copy(jamKeluar = selectedTime)
                }
                jadwalTimeTarget = null
            },
        )
    }

    hapusJadwal?.let { jadwal ->
        KonfirmasiHapusDialog(
            title = "Hapus jadwal khusus?",
            message = "${jadwal.outletName} akan kembali mengikuti aturan jam kerja pusat.",
            confirmLabel = "Hapus",
            onDismiss = { hapusJadwal = null },
            onConfirm = {
                viewModel.deleteJadwalKhusus(jadwal.outletId)
                hapusJadwal = null
            },
        )
    }

    if (konfirmasiResetSemua) {
        KonfirmasiHapusDialog(
            title = "Reset semua jadwal khusus?",
            message = "Seluruh jadwal khusus outlet akan dihapus dan semua outlet serentak mengikuti aturan pusat. Tindakan ini tidak bisa dibatalkan.",
            confirmLabel = "Reset Semua",
            onDismiss = { konfirmasiResetSemua = false },
            onConfirm = {
                viewModel.deleteJadwalKhusus(null)
                konfirmasiResetSemua = false
            },
        )
    }
}

@Composable
private fun StitchSection(
    title: String,
    progress: Float,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp)
            .stitchProgressRail(progress),
    ) {
        Column(
            modifier = Modifier.padding(start = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = {
                Text(
                    text = title,
                    color = StitchOnSurface,
                    fontSize = 22.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.2).sp,
                )
                content()
            },
        )
    }
}

private fun Modifier.stitchProgressRail(progress: Float): Modifier = drawBehind {
    val railWidth = 4.dp.toPx()
    val radius = railWidth / 2f
    drawRoundRect(
        color = StitchSurfaceLow,
        size = androidx.compose.ui.geometry.Size(railWidth, size.height),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
    )
    drawRoundRect(
        color = StitchPrimary.copy(alpha = if (progress < 0.4f) 0.30f else 0.60f),
        size = androidx.compose.ui.geometry.Size(railWidth, size.height * progress.coerceIn(0f, 1f)),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
    )
}

@Composable
private fun StitchTimeCard(
    value: String,
    onClick: () -> Unit,
    label: String,
    icon: ImageVector,
    iconBackground: Color,
    modifier: Modifier = Modifier,
    iconTint: Color = StitchPrimary,
) {
    val shape = RoundedCornerShape(16.dp)

    Surface(
        modifier = modifier
            .clickable(role = Role.Button, onClick = onClick)
            .shadow(6.dp, shape),
        shape = shape,
        color = Color.White,
        border = BorderStroke(1.dp, StitchSurfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            StitchIconTile(
                icon = icon,
                background = iconBackground,
                tint = iconTint,
                size = 40.dp,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = label,
                color = StitchSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                color = StitchOnSurface,
                style = TextStyle(
                    color = StitchOnSurface,
                    fontSize = 22.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.Bold,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun WorkTimePickerDialog(
    title: String,
    currentValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val parts = currentValue.split(":")
    val initialHour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 8
    val initialMinute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
    val timeState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    color = StitchOnSurface,
                    fontSize = 22.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Gunakan format 24 jam",
                    color = StitchSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        },
        text = {
            TimePicker(
                state = timeState,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = StitchSecondary),
            ) {
                Text("Batal")
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm("%02d:%02d".format(timeState.hour, timeState.minute))
                },
                colors = ButtonDefaults.textButtonColors(contentColor = StitchPrimary),
            ) {
                Text("Pilih", fontWeight = FontWeight.Bold)
            }
        },
    )
}

@Composable
private fun StitchMetricCard(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    suffix: String,
    icon: ImageVector,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .shadow(6.dp, shape),
        shape = shape,
        color = Color.White,
        border = BorderStroke(2.dp, if (focused) StitchPrimary else Color.Transparent),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 24.dp, y = (-24).dp)
                    .size(96.dp)
                    .background(StitchPrimary.copy(alpha = 0.05f), CircleShape),
            )
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top,
            ) {
                StitchIconTile(
                    icon = icon,
                    background = StitchPrimary.copy(alpha = 0.15f),
                    tint = StitchPrimary,
                    size = 48.dp,
                    iconSize = 28.dp,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        color = StitchOnSurface,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Surface(
                            color = StitchSurfaceLow,
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            BasicTextField(
                                value = value,
                                onValueChange = onValueChange,
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = StitchOnSurface,
                                    fontSize = 22.sp,
                                    lineHeight = 30.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                cursorBrush = SolidColor(StitchPrimary),
                                modifier = Modifier
                                    .width(if (suffix == "meter") 80.dp else 64.dp)
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                        Text(
                            text = suffix,
                            color = StitchSecondary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StitchIconTile(
    icon: ImageVector,
    background: Color,
    tint: Color,
    size: Dp,
    iconSize: Dp = 24.dp,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
private fun SaveSettingsButton(saving: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(StitchPrimary, StitchPrimaryContainer)))
            .alpha(if (saving) 0.72f else 1f)
            .clickable(enabled = !saving, role = Role.Button, onClick = onClick)
            .semantics { role = Role.Button },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (saving) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color.White,
                strokeWidth = 2.5.dp,
            )
        } else {
            Icon(
                imageVector = Icons.Default.Save,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Simpan Pengaturan",
            color = Color.White,
            fontSize = 18.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
