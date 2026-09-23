package com.sukashawarma.superapp.presentation.absensi.pengaturan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ShareLocation
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.core.ui.ios.BilahJudulIos
import com.sukashawarma.superapp.core.ui.ios.IkonBulatIos
import com.sukashawarma.superapp.core.ui.ios.JudulSeksiIos
import com.sukashawarma.superapp.core.ui.ios.KartuIos
import com.sukashawarma.superapp.core.ui.ios.KeadaanIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.TombolUtamaIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.ios.permukaanIos
import com.sukashawarma.superapp.core.ui.ios.tekanIos
import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.RealtimeTables

private enum class WorkTimeTarget(val title: String) {
    MASUK("Pilih jam masuk"),
    KELUAR("Pilih jam keluar"),
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PengaturanScreen(onExit: () -> Unit, viewModel: PengaturanViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()

    // Jam masuk, toleransi, dan radius geofence dipegang bersama satu outlet.
    // Tanpa ini, dua supervisor bisa menyimpan nilai yang saling menimpa sambil
    // sama-sama yakin sedang melihat angka terbaru.
    RealtimeRefresh(RealtimeTables.ATTENDANCE_CONFIG) { viewModel.load() }

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
        containerColor = WarnaIos.Latar,
        topBar = { BilahJudulIos(judul = "Pengaturan", onKembali = onExit) },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
        ) {
            when {
                state.loading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center).size(28.dp),
                    color = WarnaIos.Aksen,
                    strokeWidth = 2.5.dp,
                )

                state.loadError != null -> KeadaanIos(
                    ikon = IkonIos.ErrorOutline,
                    judul = "Gagal memuat",
                    pesan = state.loadError.orEmpty(),
                    nada = NadaIos.BAHAYA,
                    teksAksi = "Coba lagi",
                    onAksi = viewModel::load,
                    modifier = Modifier.align(Alignment.Center),
                )

                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                        .padding(horizontal = UkuranIos.TepiLayar, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(UkuranIos.JarakKartu),
                ) {
                    Text(
                        text = "Aturan pusat ini berlaku untuk seluruh outlet aktif.",
                        style = TipeIos.SubJudul,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )

                    SeksiPengaturan(title = "Jam Kerja") {
                        Row(horizontalArrangement = Arrangement.spacedBy(UkuranIos.JarakKartu)) {
                            StitchTimeCard(
                                value = jamMasuk,
                                onClick = { timePickerTarget = WorkTimeTarget.MASUK },
                                label = "Masuk",
                                icon = Icons.Default.WbTwilight,
                                nada = NadaIos.AKSEN,
                                modifier = Modifier.weight(1f),
                            )
                            StitchTimeCard(
                                value = jamKeluar,
                                onClick = { timePickerTarget = WorkTimeTarget.KELUAR },
                                label = "Keluar",
                                icon = Icons.Default.Nightlight,
                                nada = NadaIos.INFO,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    SeksiPengaturan(title = "Toleransi") {
                        StitchMetricCard(
                            label = "Keterlambatan",
                            value = toleransi,
                            onValueChange = { toleransi = it.filter(Char::isDigit) },
                            suffix = "menit",
                            icon = Icons.Default.Timer,
                            catatan = "Waktu tambahan sebelum karyawan dianggap terlambat.",
                        )
                    }

                    SeksiPengaturan(title = "Lokasi") {
                        StitchMetricCard(
                            label = "Radius Geofence",
                            value = radius,
                            onValueChange = { radius = it.filter(Char::isDigit) },
                            suffix = "meter",
                            icon = Icons.Default.ShareLocation,
                        )
                    }

                    SeksiPengaturan(title = "Jadwal Khusus Outlet") {
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
                            style = TipeIos.Catatan.copy(color = NadaIos.BAHAYA.teks, fontWeight = FontWeight.Medium),
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    }
                    if (state.saved) {
                        Text(
                            text = "Pengaturan tersimpan.",
                            style = TipeIos.Catatan.copy(color = NadaIos.SUKSES.teks, fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    }

                    Spacer(Modifier.height(4.dp))
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
                    Spacer(Modifier.height(12.dp))
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
                    pilihShiftAktif = draft.pilihShiftAktif,
                    shift2JamMasuk = draft.shift2JamMasuk,
                    shift2JamKeluar = draft.shift2JamKeluar,
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
            currentValue = draft.jam(target),
            onDismiss = { jadwalTimeTarget = null },
            onConfirm = { selectedTime ->
                jadwalDraft = draft.denganJam(target, selectedTime)
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

/** Judul seksi iOS + isinya; rel progres dekoratif lama dilepas supaya seragam dengan modul lain. */
@Composable
private fun SeksiPengaturan(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        JudulSeksiIos(title)
        content()
    }
}

@Composable
private fun StitchTimeCard(
    value: String,
    onClick: () -> Unit,
    label: String,
    icon: ImageVector,
    nada: NadaIos,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .permukaanIos(UkuranIos.SudutPetak)
            .tekanIos(onClick)
            .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 12.dp),
    ) {
        IkonBulatIos(icon, nada.warna, ukuran = 34.dp, padat = false)
        Spacer(Modifier.height(10.dp))
        Text(text = label, style = TipeIos.Catatan.copy(fontWeight = FontWeight.SemiBold))
        Text(text = value, style = TipeIos.AngkaBesar, modifier = Modifier.fillMaxWidth())
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
        shape = UkuranIos.SudutKartu,
        containerColor = WarnaIos.Kartu,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = title, style = TipeIos.Judul3)
                Text(text = "Gunakan format 24 jam", style = TipeIos.Catatan)
            }
        },
        text = {
            TimePicker(
                state = timeState,
                modifier = Modifier.fillMaxWidth(),
                colors = warnaPemilihJamIos(),
            )
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = WarnaIos.LabelKedua),
            ) {
                Text("Batal", fontSize = 16.sp)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm("%02d:%02d".format(timeState.hour, timeState.minute))
                },
                colors = ButtonDefaults.textButtonColors(contentColor = WarnaIos.Aksen),
            ) {
                Text("Pilih", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        },
    )
}

/** Warna TimePicker beraksen iOS — dipakai juga oleh dialog jam jadwal khusus. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun warnaPemilihJamIos() = TimePickerDefaults.colors(
    clockDialColor = WarnaIos.Latar,
    selectorColor = WarnaIos.Aksen,
    periodSelectorSelectedContainerColor = WarnaIos.Aksen.copy(alpha = 0.16f),
    timeSelectorSelectedContainerColor = WarnaIos.Aksen.copy(alpha = 0.16f),
    timeSelectorSelectedContentColor = NadaIos.AKSEN.teks,
    timeSelectorUnselectedContainerColor = WarnaIos.Latar,
    timeSelectorUnselectedContentColor = WarnaIos.Label,
)

@Composable
private fun StitchMetricCard(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    suffix: String,
    icon: ImageVector,
    catatan: String? = null,
) {
    var focused by remember { mutableStateOf(false) }

    KartuIos(modifier = Modifier.onFocusChanged { focused = it.hasFocus }) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IkonBulatIos(icon, NadaIos.AKSEN.warna, ukuran = 34.dp, padat = false)
            Text(text = label, style = TipeIos.Utama, modifier = Modifier.weight(1f))
            // Isian angka ala iOS: kotak abu; garis aksen tipis saat sedang diketik.
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TipeIos.Angka.copy(textAlign = TextAlign.Center),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                cursorBrush = SolidColor(WarnaIos.Aksen),
                modifier = Modifier
                    .width(if (suffix == "meter") 80.dp else 64.dp)
                    .clip(UkuranIos.SudutKontrol)
                    .background(WarnaIos.Isian)
                    .border(
                        width = 1.5.dp,
                        color = if (focused) WarnaIos.Aksen else Color.Transparent,
                        shape = UkuranIos.SudutKontrol,
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )
            Text(text = suffix, style = TipeIos.Catatan.copy(fontWeight = FontWeight.SemiBold))
        }
        if (catatan != null) {
            Spacer(Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = WarnaIos.Abu,
                    modifier = Modifier.size(16.dp),
                )
                Text(text = catatan, style = TipeIos.Catatan)
            }
        }
    }
}

@Composable
private fun SaveSettingsButton(saving: Boolean, onClick: () -> Unit) {
    TombolUtamaIos(
        teks = "Simpan Pengaturan",
        onKlik = onClick,
        memuat = saving,
        ikon = Icons.Default.Save,
    )
}
