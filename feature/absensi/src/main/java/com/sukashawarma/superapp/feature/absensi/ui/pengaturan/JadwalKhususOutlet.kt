package com.sukashawarma.superapp.presentation.absensi.pengaturan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Draft form jadwal khusus satu outlet. `editing` menentukan outlet masih bisa diganti
 * atau tidak — sama seperti modal web: mode "add" memakai dropdown outlet, mode "edit"
 * mengunci outletnya (satu baris konfigurasi per outlet).
 */
internal data class JadwalDraft(
    val outletId: String = "",
    val outletName: String = "",
    val jamMasuk: String,
    val jamKeluar: String,
    val toleransi: String,
    val radius: String,
    val mode: String = "auto",
    val editing: Boolean = false,
)

internal fun OutletSchedule.toDraft() = JadwalDraft(
    outletId = outletId,
    outletName = outletName,
    jamMasuk = jamMasuk,
    jamKeluar = jamKeluar,
    toleransi = toleransiMenit.toString(),
    radius = radiusM.toString(),
    mode = mode,
    editing = true,
)

/* ------------------------------------------------------------------ Daftar */

@Composable
internal fun JadwalKhususList(
    state: PengaturanUiState,
    onAdd: () -> Unit,
    onEdit: (OutletSchedule) -> Unit,
    onDelete: (OutletSchedule) -> Unit,
    onResetAll: () -> Unit,
) {
    Text(
        text = "Outlet di bawah ini punya jam kerja sendiri dan tidak mengikuti aturan pusat.",
        color = StitchTertiary,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium,
    )

    when {
        state.loadingJadwal && state.jadwalKhusus.isEmpty() -> Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = StitchPrimary, strokeWidth = 2.5.dp)
        }

        state.jadwalKhusus.isEmpty() -> Surface(
            modifier = Modifier.fillMaxWidth(),
            color = StitchSurfaceLow,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, StitchSurfaceVariant),
        ) {
            Text(
                text = "Belum ada jadwal khusus. Semua outlet mengikuti aturan pusat.",
                color = StitchTertiary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
            )
        }

        else -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.jadwalKhusus.forEach { jadwal ->
                JadwalCard(
                    jadwal = jadwal,
                    enabled = !state.savingJadwal,
                    onEdit = { onEdit(jadwal) },
                    onDelete = { onDelete(jadwal) },
                )
            }
        }
    }

    val bisaTambah = state.outletsTanpaJadwal.isNotEmpty() && !state.savingJadwal
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (bisaTambah) StitchPrimary.copy(alpha = 0.12f) else StitchSurfaceLow)
            .clickable(enabled = bisaTambah, role = Role.Button, onClick = onAdd),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            tint = if (bisaTambah) StitchPrimary else StitchSecondary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (state.outlets.isEmpty()) "Memuat daftar outlet..."
            else if (bisaTambah) "Tambah Jadwal Khusus"
            else "Semua outlet sudah punya jadwal khusus",
            color = if (bisaTambah) StitchPrimary else StitchSecondary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
        )
    }

    if (state.jadwalKhusus.isNotEmpty()) {
        TextButton(
            onClick = onResetAll,
            enabled = !state.savingJadwal,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.textButtonColors(contentColor = StitchDanger),
        ) {
            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Reset Semua Jadwal Khusus", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }

    state.jadwalError?.let {
        Text(text = it, color = StitchDanger, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
    state.jadwalMessage?.let {
        Text(text = it, color = StitchSuccess, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun JadwalCard(
    jadwal: OutletSchedule,
    enabled: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Surface(
        modifier = Modifier.fillMaxWidth().shadow(6.dp, shape),
        shape = shape,
        color = Color.White,
        border = BorderStroke(1.dp, StitchSurfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.Storefront,
                    contentDescription = null,
                    tint = StitchPrimary,
                    modifier = Modifier.padding(top = 2.dp).size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = jadwal.outletName,
                        color = StitchOnSurface,
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Toleransi ${jadwal.toleransiMenit}m · Radius ${jadwal.radiusM}m",
                        color = StitchSecondary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onEdit, enabled = enabled, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Ubah jadwal khusus", tint = StitchSecondary, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onDelete, enabled = enabled, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Hapus jadwal khusus", tint = StitchDanger, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                JamChip("Masuk", jadwal.jamMasuk, Icons.Default.WbTwilight, Modifier.weight(1f))
                JamChip("Keluar", jadwal.jamKeluar, Icons.Default.Nightlight, Modifier.weight(1f))
            }

            Spacer(Modifier.height(10.dp))
            Surface(
                color = if (jadwal.manual) StitchSurfaceLow else StitchPrimary.copy(alpha = 0.10f),
                shape = RoundedCornerShape(8.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (jadwal.manual) Icons.Default.ToggleOn else Icons.Default.Bolt,
                        contentDescription = null,
                        tint = if (jadwal.manual) StitchSecondary else StitchPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (jadwal.manual) "Kamera manual (oleh SPV)" else "Kamera otomatis (ikut shift)",
                        color = if (jadwal.manual) StitchSecondary else StitchPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun JamChip(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = StitchSurfaceLow,
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = StitchSecondary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Column {
                Text(label, color = StitchSecondary, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                Text(value, color = StitchOnSurface, fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/* ------------------------------------------------------------------- Dialog */

@Composable
internal fun JadwalKhususDialog(
    draft: JadwalDraft,
    outletTersedia: List<OutletOption>,
    saving: Boolean,
    error: String?,
    onDraftChange: (JadwalDraft) -> Unit,
    onPickTime: (JadwalTimeTarget) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        modifier = Modifier.imePadding(),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (draft.editing) "Ubah Jadwal Khusus" else "Tambah Jadwal Khusus",
                    color = StitchOnSurface,
                    fontSize = 22.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Jam kerja khusus untuk satu outlet, menimpa aturan pusat.",
                    color = StitchSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (draft.editing) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = StitchSurfaceLow,
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.Storefront, contentDescription = null, tint = StitchSecondary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(draft.outletName, color = StitchOnSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    OutletPicker(
                        outlets = outletTersedia,
                        selectedId = draft.outletId,
                        enabled = !saving,
                        onSelect = { onDraftChange(draft.copy(outletId = it.id, outletName = it.name)) },
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    JamPickerField(
                        label = "Masuk",
                        value = draft.jamMasuk,
                        icon = Icons.Default.WbTwilight,
                        onClick = { onPickTime(JadwalTimeTarget.MASUK) },
                        modifier = Modifier.weight(1f),
                    )
                    JamPickerField(
                        label = "Keluar",
                        value = draft.jamKeluar,
                        icon = Icons.Default.Nightlight,
                        onClick = { onPickTime(JadwalTimeTarget.KELUAR) },
                        modifier = Modifier.weight(1f),
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField(
                        label = "Toleransi",
                        value = draft.toleransi,
                        suffix = "menit",
                        onValueChange = { onDraftChange(draft.copy(toleransi = it.filter(Char::isDigit))) },
                        modifier = Modifier.weight(1f),
                    )
                    NumberField(
                        label = "Radius",
                        value = draft.radius,
                        suffix = "meter",
                        onValueChange = { onDraftChange(draft.copy(radius = it.filter(Char::isDigit))) },
                        modifier = Modifier.weight(1f),
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Mode kamera kiosk",
                        color = StitchOnSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    ModeOption(
                        title = "Otomatis (ikut shift)",
                        subtitle = "Kamera aktif sendiri saat jendela jam shift dimulai.",
                        icon = Icons.Default.Bolt,
                        selected = draft.mode == "auto",
                        onClick = { onDraftChange(draft.copy(mode = "auto")) },
                    )
                    ModeOption(
                        title = "Manual (oleh SPV)",
                        subtitle = "Kamera dibuka atau dikunci manual oleh leader/admin.",
                        icon = Icons.Default.ToggleOn,
                        selected = draft.mode == "manual",
                        onClick = { onDraftChange(draft.copy(mode = "manual")) },
                    )
                }

                error?.let {
                    Text(text = it, color = StitchDanger, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !saving,
                colors = ButtonDefaults.textButtonColors(contentColor = StitchSecondary),
            ) {
                Text("Batal")
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !saving && draft.outletId.isNotBlank(),
                colors = ButtonDefaults.textButtonColors(contentColor = StitchPrimary),
            ) {
                if (saving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = StitchPrimary, strokeWidth = 2.dp)
                } else {
                    Text("Simpan", fontWeight = FontWeight.Bold)
                }
            }
        },
    )
}

internal enum class JadwalTimeTarget(val title: String) {
    MASUK("Pilih jam masuk outlet"),
    KELUAR("Pilih jam keluar outlet"),
}

@Composable
private fun OutletPicker(
    outlets: List<OutletOption>,
    selectedId: String,
    enabled: Boolean,
    onSelect: (OutletOption) -> Unit,
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
                .clickable(enabled = enabled && outlets.isNotEmpty()) { expanded = true },
            shape = RoundedCornerShape(14.dp),
            color = StitchSurfaceLow,
            border = BorderStroke(1.dp, if (selected == null) StitchPrimary.copy(alpha = 0.45f) else StitchSurfaceVariant),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Storefront, contentDescription = null, tint = StitchSecondary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = selected?.name ?: "Pilih outlet",
                        color = if (selected == null) StitchSecondary else StitchOnSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (outlets.isEmpty()) "Semua outlet sudah punya jadwal khusus" else "Ketuk untuk memilih",
                        color = StitchTertiary,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                    )
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Pilih outlet",
                    tint = StitchSecondary,
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(280.dp).heightIn(max = 400.dp),
        ) {
            Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    placeholder = { Text("Cari outlet", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Hapus pencarian")
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StitchPrimary,
                        unfocusedBorderColor = StitchSurfaceVariant,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                    ),
                )
                Spacer(Modifier.height(6.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    if (filtered.isEmpty()) {
                        Text(
                            "Outlet tidak ditemukan",
                            color = StitchSecondary,
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
                                    color = if (outlet.id == selectedId) StitchPrimary else StitchOnSurface,
                                )
                            },
                            leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(20.dp)) },
                            onClick = { onSelect(outlet); expanded = false },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun JamPickerField(
    label: String,
    value: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = StitchSurfaceLow,
        border = BorderStroke(1.dp, StitchSurfaceVariant),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = StitchSecondary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(label, color = StitchSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Text(value, color = StitchOnSurface, fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    suffix: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(label, color = StitchSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = StitchSurfaceLow,
            border = BorderStroke(1.dp, StitchSurfaceVariant),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = StitchOnSurface,
                        fontSize = 20.sp,
                        lineHeight = 28.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    cursorBrush = SolidColor(StitchPrimary),
                    modifier = Modifier.weight(1f),
                )
                Text(suffix, color = StitchSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ModeOption(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(role = Role.RadioButton, onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) StitchPrimary.copy(alpha = 0.08f) else StitchSurfaceLow,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) StitchPrimary else StitchSurfaceVariant),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) StitchPrimary else StitchSurfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = if (selected) Color.White else StitchSecondary, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = StitchOnSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = StitchTertiary, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

/* -------------------------------------------------------- Dialog konfirmasi */

@Composable
internal fun KonfirmasiHapusDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = {
            Text(title, color = StitchOnSurface, fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Text(message, color = StitchSecondary, fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium)
        },
        dismissButton = {
            TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = StitchSecondary)) {
                Text("Batal")
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, colors = ButtonDefaults.textButtonColors(contentColor = StitchDanger)) {
                Text(confirmLabel, fontWeight = FontWeight.Bold)
            }
        },
    )
}
