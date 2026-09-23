package com.sukashawarma.superapp.presentation.absensi.pengaturan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import com.sukashawarma.superapp.core.ui.SukaDropdownMenu
import com.sukashawarma.superapp.core.ui.SukaDropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sukashawarma.superapp.core.ui.ios.AngkaIos
import com.sukashawarma.superapp.core.ui.ios.BlokAngkaIos
import com.sukashawarma.superapp.core.ui.ios.IkonBulatIos
import com.sukashawarma.superapp.core.ui.ios.KartuIos
import com.sukashawarma.superapp.core.ui.ios.KolomCariIos
import com.sukashawarma.superapp.core.ui.ios.LabelSeksiIos
import com.sukashawarma.superapp.core.ui.ios.LencanaIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.PemisahIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.TombolKeduaIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.ios.permukaanIos
import com.sukashawarma.superapp.core.ui.ios.tekanIos
import com.sukashawarma.superapp.core.ui.ios.warnaSaklarIos
import com.sukashawarma.superapp.core.ui.kaca.IkonIos

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
    val pilihShiftAktif: Boolean = false,
    val shift2JamMasuk: String = SHIFT2_DEFAULT_MASUK,
    val shift2JamKeluar: String = SHIFT2_DEFAULT_KELUAR,
) {
    fun jam(target: JadwalTimeTarget): String = when (target) {
        JadwalTimeTarget.MASUK -> jamMasuk
        JadwalTimeTarget.KELUAR -> jamKeluar
        JadwalTimeTarget.SHIFT2_MASUK -> shift2JamMasuk
        JadwalTimeTarget.SHIFT2_KELUAR -> shift2JamKeluar
    }

    fun denganJam(target: JadwalTimeTarget, value: String): JadwalDraft = when (target) {
        JadwalTimeTarget.MASUK -> copy(jamMasuk = value)
        JadwalTimeTarget.KELUAR -> copy(jamKeluar = value)
        JadwalTimeTarget.SHIFT2_MASUK -> copy(shift2JamMasuk = value)
        JadwalTimeTarget.SHIFT2_KELUAR -> copy(shift2JamKeluar = value)
    }
}

/** Nilai awal Shift 2 saat toggle pertama kali dinyalakan — sama dengan SHIFT2_DEFAULT web. */
private const val SHIFT2_DEFAULT_MASUK = "13:00"
private const val SHIFT2_DEFAULT_KELUAR = "22:00"

internal fun OutletSchedule.toDraft() = JadwalDraft(
    outletId = outletId,
    outletName = outletName,
    jamMasuk = jamMasuk,
    jamKeluar = jamKeluar,
    toleransi = toleransiMenit.toString(),
    radius = radiusM.toString(),
    mode = mode,
    editing = true,
    pilihShiftAktif = pilihShiftAktif,
    shift2JamMasuk = shift2JamMasuk ?: SHIFT2_DEFAULT_MASUK,
    shift2JamKeluar = shift2JamKeluar ?: SHIFT2_DEFAULT_KELUAR,
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
        style = TipeIos.Catatan,
        modifier = Modifier.padding(horizontal = 4.dp),
    )

    when {
        state.loadingJadwal && state.jadwalKhusus.isEmpty() -> Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = WarnaIos.Aksen, strokeWidth = 2.5.dp)
        }

        state.jadwalKhusus.isEmpty() -> KartuIos {
            Text(
                text = "Belum ada jadwal khusus. Semua outlet mengikuti aturan pusat.",
                style = TipeIos.SubJudul,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            )
        }

        else -> Column(verticalArrangement = Arrangement.spacedBy(UkuranIos.JarakKartu)) {
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
    TombolKeduaIos(
        teks = if (state.outlets.isEmpty()) "Memuat daftar outlet..."
        else if (bisaTambah) "Tambah Jadwal Khusus"
        else "Semua outlet sudah punya jadwal khusus",
        onKlik = onAdd,
        aktif = bisaTambah,
        ikon = IkonIos.Add,
    )

    if (state.jadwalKhusus.isNotEmpty()) {
        TextButton(
            onClick = onResetAll,
            enabled = !state.savingJadwal,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.textButtonColors(contentColor = WarnaIos.Merah),
        ) {
            Icon(IkonIos.Delete, contentDescription = null, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(6.dp))
            Text("Reset Semua Jadwal Khusus", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }

    state.jadwalError?.let {
        Text(text = it, style = TipeIos.Catatan.copy(color = NadaIos.BAHAYA.teks, fontWeight = FontWeight.Medium))
    }
    state.jadwalMessage?.let {
        Text(text = it, style = TipeIos.Catatan.copy(color = NadaIos.SUKSES.teks, fontWeight = FontWeight.SemiBold))
    }
}

@Composable
private fun JadwalCard(
    jadwal: OutletSchedule,
    enabled: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    KartuIos {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IkonBulatIos(IkonIos.Storefront, NadaIos.AKSEN.warna, ukuran = 32.dp, padat = false)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = jadwal.outletName,
                    style = TipeIos.Utama,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Toleransi ${jadwal.toleransiMenit}m · Radius ${jadwal.radiusM}m",
                    style = TipeIos.Catatan,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onEdit, enabled = enabled, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "Ubah jadwal khusus", tint = WarnaIos.Aksen, modifier = Modifier.size(19.dp))
            }
            IconButton(onClick = onDelete, enabled = enabled, modifier = Modifier.size(36.dp)) {
                Icon(IkonIos.Delete, contentDescription = "Hapus jadwal khusus", tint = WarnaIos.Merah, modifier = Modifier.size(19.dp))
            }
        }

        Spacer(Modifier.height(12.dp))
        if (jadwal.pilihShiftAktif) {
            BlokAngkaIos(
                listOf(
                    AngkaIos("Shift 1", "${jadwal.jamMasuk} – ${jadwal.jamKeluar}"),
                    AngkaIos("Shift 2", "${jadwal.shift2JamMasuk ?: "-"} – ${jadwal.shift2JamKeluar ?: "-"}"),
                ),
            )
        } else {
            BlokAngkaIos(
                listOf(
                    AngkaIos("Masuk", jadwal.jamMasuk),
                    AngkaIos("Keluar", jadwal.jamKeluar),
                ),
            )
        }

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            LencanaIos(
                teks = if (jadwal.manual) "Kamera manual (oleh SPV)" else "Kamera otomatis (ikut shift)",
                nada = if (jadwal.manual) NadaIos.NETRAL else NadaIos.AKSEN,
                ikon = if (jadwal.manual) Icons.Default.ToggleOn else Icons.Default.Bolt,
            )
            if (jadwal.pilihShiftAktif) {
                LencanaIos(teks = "2 Shift", nada = NadaIos.UNGU, titik = false)
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
        shape = UkuranIos.SudutKartu,
        containerColor = WarnaIos.Latar,
        modifier = Modifier.imePadding(),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (draft.editing) "Ubah Jadwal Khusus" else "Tambah Jadwal Khusus",
                    style = TipeIos.Judul3,
                )
                Text(
                    text = "Jam kerja khusus untuk satu outlet, menimpa aturan pusat.",
                    style = TipeIos.Catatan,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (draft.editing) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .permukaanIos(UkuranIos.SudutGrup)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(IkonIos.Storefront, contentDescription = null, tint = WarnaIos.Aksen, modifier = Modifier.size(19.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(draft.outletName, style = TipeIos.Utama.copy(fontSize = 15.sp))
                    }
                } else {
                    OutletPicker(
                        outlets = outletTersedia,
                        selectedId = draft.outletId,
                        enabled = !saving,
                        onSelect = { onDraftChange(draft.copy(outletId = it.id, outletName = it.name)) },
                    )
                }

                PilihShiftToggle(
                    aktif = draft.pilihShiftAktif,
                    enabled = !saving,
                    onToggle = { onDraftChange(draft.copy(pilihShiftAktif = !draft.pilihShiftAktif)) },
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (draft.pilihShiftAktif) {
                        LabelSeksiIos("Shift 1", Modifier.padding(start = 4.dp))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        JamPickerField(
                            label = "Masuk",
                            value = draft.jamMasuk,
                            icon = Icons.Default.WbTwilight,
                            onClick = { onPickTime(JadwalTimeTarget.MASUK) },
                            modifier = Modifier.weight(1f),
                        )
                        JamPickerField(
                            label = if (draft.pilihShiftAktif) "Pulang" else "Keluar",
                            value = draft.jamKeluar,
                            icon = Icons.Default.Nightlight,
                            onClick = { onPickTime(JadwalTimeTarget.KELUAR) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                if (draft.pilihShiftAktif) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LabelSeksiIos("Shift 2", Modifier.padding(start = 4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            JamPickerField(
                                label = "Masuk",
                                value = draft.shift2JamMasuk,
                                icon = Icons.Default.WbTwilight,
                                onClick = { onPickTime(JadwalTimeTarget.SHIFT2_MASUK) },
                                modifier = Modifier.weight(1f),
                            )
                            JamPickerField(
                                label = "Pulang",
                                value = draft.shift2JamKeluar,
                                icon = Icons.Default.Nightlight,
                                onClick = { onPickTime(JadwalTimeTarget.SHIFT2_KELUAR) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Text(
                            text = "Crew akan melihat pilihan: ${draft.jamMasuk} – ${draft.jamKeluar} atau " +
                                "${draft.shift2JamMasuk} – ${draft.shift2JamKeluar}",
                            style = TipeIos.Catatan,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
                    LabelSeksiIos("Mode kamera kiosk", Modifier.padding(start = 4.dp))
                    // Dua pilihan radio dalam satu grup inset iOS, dipisah hairline.
                    Column(Modifier.fillMaxWidth().permukaanIos(UkuranIos.SudutGrup)) {
                        ModeOption(
                            title = "Otomatis (ikut shift)",
                            subtitle = "Kamera aktif sendiri saat jendela jam shift dimulai.",
                            icon = Icons.Default.Bolt,
                            selected = draft.mode == "auto",
                            onClick = { onDraftChange(draft.copy(mode = "auto")) },
                        )
                        PemisahIos(inset = 56.dp)
                        ModeOption(
                            title = "Manual (oleh SPV)",
                            subtitle = "Kamera dibuka atau dikunci manual oleh leader/admin.",
                            icon = Icons.Default.ToggleOn,
                            selected = draft.mode == "manual",
                            onClick = { onDraftChange(draft.copy(mode = "manual")) },
                        )
                    }
                }

                error?.let {
                    Text(text = it, style = TipeIos.Catatan.copy(color = NadaIos.BAHAYA.teks, fontWeight = FontWeight.Medium))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !saving,
                colors = ButtonDefaults.textButtonColors(contentColor = WarnaIos.LabelKedua),
            ) {
                Text("Batal", fontSize = 16.sp)
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !saving && draft.outletId.isNotBlank(),
                colors = ButtonDefaults.textButtonColors(contentColor = WarnaIos.Aksen),
            ) {
                if (saving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = WarnaIos.Aksen, strokeWidth = 2.dp)
                } else {
                    Text("Simpan", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        },
    )
}

internal enum class JadwalTimeTarget(val title: String) {
    MASUK("Pilih jam masuk outlet"),
    KELUAR("Pilih jam keluar outlet"),
    SHIFT2_MASUK("Pilih jam masuk Shift 2"),
    SHIFT2_KELUAR("Pilih jam pulang Shift 2"),
}

/** Toggle "Crew Pilih Shift Sebelum Absen" — hanya ada di jadwal khusus cabang, tidak di aturan pusat. */
@Composable
private fun PilihShiftToggle(aktif: Boolean, enabled: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .permukaanIos(UkuranIos.SudutGrup)
            .clickable(enabled = enabled, role = Role.Switch, onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Crew Pilih Shift Sebelum Absen", style = TipeIos.Utama.copy(fontSize = 15.sp))
            Text(
                "Untuk cabang dengan dua jam kerja. Crew wajib memilih shift sebelum absen masuk.",
                style = TipeIos.Kecil,
            )
        }
        Spacer(Modifier.width(10.dp))
        Switch(
            checked = aktif,
            onCheckedChange = null,
            enabled = enabled,
            colors = warnaSaklarIos(),
        )
    }
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .permukaanIos(UkuranIos.SudutGrup)
                .then(
                    // Outlet wajib dipilih dulu — garis aksen tipis menandai isian yang kosong.
                    if (selected == null) Modifier.border(1.dp, WarnaIos.Aksen.copy(alpha = 0.45f), UkuranIos.SudutGrup)
                    else Modifier
                )
                .clickable(enabled = enabled && outlets.isNotEmpty()) { expanded = true }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(IkonIos.Storefront, contentDescription = null, tint = WarnaIos.Aksen, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = selected?.name ?: "Pilih outlet",
                    style = TipeIos.Keterangan.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selected == null) WarnaIos.LabelKedua else WarnaIos.Label,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (outlets.isEmpty()) "Semua outlet sudah punya jadwal khusus" else "Ketuk untuk memilih",
                    style = TipeIos.Kecil,
                    maxLines = 1,
                )
            }
            Icon(
                if (expanded) IkonIos.ExpandLess else IkonIos.ExpandMore,
                contentDescription = "Pilih outlet",
                tint = WarnaIos.LabelKedua,
                modifier = Modifier.size(18.dp),
            )
        }

        SukaDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(280.dp).heightIn(max = 400.dp),
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
                        .heightIn(max = 300.dp)
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
                            onClick = { onSelect(outlet); expanded = false },
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
    Column(
        modifier = modifier
            .permukaanIos(UkuranIos.SudutGrup)
            .tekanIos(onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = WarnaIos.Aksen, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(5.dp))
            Text(label, style = TipeIos.Kecil.copy(fontWeight = FontWeight.SemiBold))
        }
        Text(value, style = TipeIos.Angka)
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
        Text(label, style = TipeIos.Kecil.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.padding(start = 4.dp))
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .permukaanIos(UkuranIos.SudutGrup)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TipeIos.Angka,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                cursorBrush = SolidColor(WarnaIos.Aksen),
                modifier = Modifier.weight(1f),
            )
            Text(suffix, style = TipeIos.Kecil.copy(fontWeight = FontWeight.SemiBold))
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (selected) WarnaIos.Aksen else WarnaIos.Isian),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = if (selected) Color.White else WarnaIos.LabelKedua, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = TipeIos.Utama.copy(fontSize = 15.sp))
            Text(subtitle, style = TipeIos.Kecil)
        }
        if (selected) {
            Spacer(Modifier.width(8.dp))
            Icon(IkonIos.Check, contentDescription = null, tint = WarnaIos.Aksen, modifier = Modifier.size(18.dp))
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
        shape = UkuranIos.SudutKartu,
        containerColor = WarnaIos.Kartu,
        title = {
            Text(title, style = TipeIos.Judul3)
        },
        text = {
            Text(message, style = TipeIos.SubJudul)
        },
        dismissButton = {
            TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = WarnaIos.Aksen)) {
                Text("Batal", fontSize = 16.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, colors = ButtonDefaults.textButtonColors(contentColor = WarnaIos.Merah)) {
                Text(confirmLabel, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        },
    )
}
