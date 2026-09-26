package com.sukashawarma.superapp.presentation.absensi.cuti

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.layout.ContentScale
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.sukashawarma.superapp.core.camera.KameraFotoSheet
import java.io.File
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.core.ui.ios.AngkaIos
import com.sukashawarma.superapp.core.ui.ios.BlokAngkaIos
import com.sukashawarma.superapp.core.ui.ios.JudulSeksiIos
import com.sukashawarma.superapp.core.ui.ios.KartuIos
import com.sukashawarma.superapp.core.ui.ios.KeadaanIos
import com.sukashawarma.superapp.core.ui.ios.LencanaIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.SegmenIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.TombolBundarIos
import com.sukashawarma.superapp.core.ui.ios.TombolUtamaIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WadahSegmenIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.presentation.absensi.AbsensiShell
import com.sukashawarma.superapp.presentation.absensi.nadaStatusPengajuan
import com.sukashawarma.superapp.presentation.absensi.warnaBidangIsianIos
import com.sukashawarma.superapp.core.ui.kaca.denganRuangNav
import com.sukashawarma.superapp.domain.session.AppSession
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.RealtimeTables

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CutiScreen(
    onExit: () -> Unit,
    onNavigateTab: (Int) -> Unit = {},
    /** true = digambar sebagai halaman pager (tab Cuti milik Kantor Pusat), yang sudah
     *  dibungkus [AbsensiShell] oleh pager-nya; bungkus lagi di sini = bilah tab ganda. */
    sebagaiTab: Boolean = false,
    viewModel: CutiViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    RealtimeRefresh(RealtimeTables.LEAVE_REQUESTS) { viewModel.refresh() }
    // Pesan ini muncul setelah formulir tertutup, jadi ditampilkan sebagai toast alih-alih
    // di dalam formulir — yang sudah tidak ada lagi di layar saat pengajuan masuk antrean.
    val konteksAntrean = LocalContext.current
    LaunchedEffect(state.pesanAntrean) {
        val pesan = state.pesanAntrean ?: return@LaunchedEffect
        Toast.makeText(konteksAntrean, pesan, Toast.LENGTH_LONG).show()
        viewModel.clearPesanAntrean()
    }

    val staff by AppSession.staff.collectAsState()
    var showForm by remember { mutableStateOf(false) }

    // Cuti & Izin diakses dari tab "More" (index 3) di hub — bilah tab tetap tampil di
    // sini (bukan cuma di 4 tab utama) supaya user bisa lompat tab tanpa balik dulu.
    val isi: @Composable () -> Unit = {
        Scaffold(
            // Samakan dengan latar isi; warna bawaan tema (krem) tampil sebagai pita di belakang nav.
            containerColor = WarnaIos.Latar,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = WarnaIos.Latar),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(IkonIos.Storefront, contentDescription = null, tint = WarnaIos.Aksen, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("outlet tes", style = TipeIos.Keterangan.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold))
                        }
                    },
                    actions = {
                        Box(
                            modifier = Modifier.padding(end = 12.dp).size(36.dp).clip(CircleShape).background(WarnaIos.Aksen.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(staff?.name?.take(1)?.uppercase() ?: "?", color = NadaIos.AKSEN.teks, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        }
                    }
                )
            }
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize().background(WarnaIos.Latar)) {
                Column(Modifier.padding(horizontal = UkuranIos.TepiLayar).padding(top = 4.dp, bottom = 4.dp)) {
                    Text("Cuti & Izin", style = TipeIos.Judul1, modifier = Modifier.padding(horizontal = 4.dp))

                    Spacer(Modifier.height(16.dp))

                    TombolUtamaIos(teks = "Ajukan Cuti", onKlik = { showForm = true }, ikon = IkonIos.Add)

                    Spacer(Modifier.height(UkuranIos.JarakKartu))

                    // Ringkasan kuota — blok angka bersekat ala aplikasi Kesehatan iOS.
                    KartuIos(padding = PaddingValues(10.dp)) {
                        val used = state.rows.filter { it.status == "approved" }.sumOf { it.days }
                        BlokAngkaIos(
                            listOf(
                                AngkaIos("Total Kuota Tahunan", "12", "hari"),
                                AngkaIos("Cuti Terpakai", "$used", "hari"),
                            ),
                        )
                    }

                    JudulSeksiIos("Riwayat Cuti", modifier = Modifier.padding(bottom = 8.dp))
                }

                Box(Modifier.fillMaxSize()) {
                    when {
                        state.loading -> CircularProgressIndicator(
                            Modifier.align(Alignment.Center).size(28.dp),
                            color = WarnaIos.Aksen,
                            strokeWidth = 2.5.dp,
                        )
                        state.error != null -> KeadaanIos(
                            ikon = IkonIos.ErrorOutline,
                            judul = "Gagal memuat",
                            pesan = state.error ?: "",
                            nada = NadaIos.BAHAYA,
                            modifier = Modifier.align(Alignment.TopCenter),
                        )
                        state.rows.isEmpty() -> KeadaanIos(
                            ikon = IkonIos.CalendarMonth,
                            judul = "Belum ada pengajuan",
                            pesan = "Belum ada pengajuan cuti.",
                            modifier = Modifier.align(Alignment.TopCenter),
                        )
                        else -> LazyColumn(
                            contentPadding = PaddingValues(bottom = 24.dp, start = UkuranIos.TepiLayar, end = UkuranIos.TepiLayar, top = 4.dp).denganRuangNav(),
                            verticalArrangement = Arrangement.spacedBy(UkuranIos.JarakKartu),
                        ) {
                            items(state.rows, key = { it.id }) { row -> CutiItem(row) }
                        }
                    }
                }
            }
        }
    }
    if (sebagaiTab) isi() else AbsensiShell(selectedIndex = 3, onSelect = onNavigateTab) { isi() }

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
            onSubmit = { type, start, end, reason, bukti -> viewModel.submit(type, start, end, reason, bukti) },
        )
    }
}

@Composable
private fun CutiItem(row: CutiRequestRow) {
    val (nada, label) = statusNadaLabel(row.status)
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("id", "ID"))
    KartuIos {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(Modifier.weight(1f)) {
                Text(row.leaveType.replaceFirstChar { it.uppercase() }, style = TipeIos.Utama)
                Spacer(Modifier.height(3.dp))
                Text("${row.startDate.format(formatter)} - ${row.endDate.format(formatter)}", style = TipeIos.Catatan)
                if (row.reason.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(row.reason, style = TipeIos.SubJudul)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                LencanaIos(label, nada)
                Spacer(Modifier.height(8.dp))
                Text("${row.days} hari", style = TipeIos.Keterangan.copy(fontWeight = FontWeight.SemiBold))
            }
        }
    }
}

private fun statusNadaLabel(status: String): Pair<NadaIos, String> = nadaStatusPengajuan(status) to when (status) {
    "approved" -> "Disetujui"
    "rejected" -> "Ditolak"
    else -> "Menunggu"
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
    onSubmit: (String, LocalDate, LocalDate, String, File?) -> Unit,
) {
    var leaveType by remember { mutableStateOf(LEAVE_TYPE_LABELS.first().second) }
    val konteks = LocalContext.current
    // Foto bukti yang sudah dikompres & disimpan di filesDir (lihat BuktiCuti).
    var bukti by remember { mutableStateOf<File?>(null) }
    var memprosesBukti by remember { mutableStateOf(false) }
    var kameraBukti by remember { mutableStateOf(false) }
    val wajibBukti = leaveType == JENIS_WAJIB_BUKTI
    var reason by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf<LocalDate?>(null) }
    var endDate by remember { mutableStateOf<LocalDate?>(null) }
    var pickingStart by remember { mutableStateOf(false) }
    var pickingEnd by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("id", "ID")) }

    // Form ditutup TANPA mengajukan: foto bukti belum dipakai siapa pun, buang dari HP.
    // (Setelah berhasil diajukan form ditutup lewat jalur lain, dan berkasnya jangan
    // dihapus di sini — bisa jadi masih menunggu di antrean offline.)
    fun tutupTanpaAjukan() {
        bukti?.delete()
        onDismiss()
    }

    fun dismissAnimated() {
        scope.launch { sheetState.hide() }.invokeOnCompletion { if (!sheetState.isVisible) tutupTanpaAjukan() }
    }

    fun pakaiBukti(berkas: File?) {
        memprosesBukti = false
        if (berkas == null) {
            Toast.makeText(konteks, "Foto gagal dibaca. Coba foto lain.", Toast.LENGTH_SHORT).show()
            return
        }
        bukti?.delete()
        bukti = berkas
    }

    val galeriBukti = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            memprosesBukti = true
            scope.launch { pakaiBukti(BuktiCuti.dariUri(konteks, uri)) }
        }
    }
    val izinKamera = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { diberi ->
        if (diberi) kameraBukti = true
        else Toast.makeText(konteks, "Izin kamera diperlukan untuk memotret bukti.", Toast.LENGTH_SHORT).show()
    }

    ModalBottomSheet(
        onDismissRequest = { tutupTanpaAjukan() },
        sheetState = sheetState,
        containerColor = WarnaIos.Kartu,
        dragHandle = { BottomSheetDefaults.DragHandle(color = WarnaIos.LabelKetiga) },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                // Bagian bukti sakit + kamera membuat form lebih tinggi dari layar HP kecil.
                .verticalScroll(rememberScrollState())
                .padding(horizontal = UkuranIos.TepiLayar + 4.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header
            Box(Modifier.fillMaxWidth().padding(bottom = 20.dp), contentAlignment = Alignment.Center) {
                Text("Ajukan Cuti", style = TipeIos.Utama)
                TombolBundarIos(
                    IkonIos.Close,
                    "Tutup",
                    onKlik = { dismissAnimated() },
                    modifier = Modifier.align(Alignment.CenterEnd),
                    warnaIkon = WarnaIos.LabelKedua,
                )
            }

            // Jenis Cuti
            LabelBidang("Jenis Cuti")
            Spacer(Modifier.height(8.dp))
            WadahSegmenIos {
                LEAVE_TYPE_LABELS.forEach { (label, value) ->
                    SegmenIos(
                        label = label,
                        aktif = leaveType == value,
                        onKlik = { leaveType = value },
                        modifier = Modifier.weight(1f),
                        jarakSisi = 4.dp,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Tanggal Mulai
            LabelBidang("Tanggal Mulai")
            Spacer(Modifier.height(6.dp))
            DateInputField(
                icon = IkonIos.CalendarMonth,
                placeholder = "Pilih tanggal mulai",
                value = startDate?.format(dateFormatter),
                onClick = { pickingStart = true },
            )

            Spacer(Modifier.height(16.dp))

            // Tanggal Selesai
            LabelBidang("Tanggal Selesai")
            Spacer(Modifier.height(6.dp))
            DateInputField(
                icon = IkonIos.CalendarMonth,
                placeholder = "Pilih tanggal selesai",
                value = endDate?.format(dateFormatter),
                onClick = { pickingEnd = true },
            )

            Spacer(Modifier.height(16.dp))

            // Alasan
            LabelBidang("Alasan (Opsional)")
            Spacer(Modifier.height(6.dp))
            TextField(
                value = reason,
                onValueChange = { reason = it },
                placeholder = { Text("Tulis alasan Anda di sini...", color = WarnaIos.Abu) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = UkuranIos.SudutKontrol,
                colors = warnaBidangIsianIos(),
            )

            if (wajibBukti) {
                Spacer(Modifier.height(16.dp))
                LabelBidang("Bukti Sakit (Wajib)")
                Spacer(Modifier.height(2.dp))
                Text(
                    "Foto surat dokter atau obat yang diminum.",
                    style = TipeIos.Catatan.copy(color = WarnaIos.LabelKedua),
                )
                Spacer(Modifier.height(8.dp))
                val berkasBukti = bukti
                when {
                    kameraBukti -> KameraFotoSheet(
                        onDiambil = { foto ->
                            kameraBukti = false
                            memprosesBukti = true
                            scope.launch { pakaiBukti(BuktiCuti.dariBitmap(konteks, foto)) }
                        },
                        onBatal = { kameraBukti = false },
                        labelAmbil = "Pakai Foto Ini",
                    )
                    memprosesBukti -> Box(
                        Modifier.fillMaxWidth().height(120.dp).clip(UkuranIos.SudutKontrol).background(WarnaIos.Isian),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator(Modifier.size(24.dp), color = WarnaIos.Aksen, strokeWidth = 2.dp) }
                    berkasBukti != null -> {
                        AsyncImage(
                            model = berkasBukti,
                            contentDescription = "Foto bukti sakit",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(UkuranIos.SudutKontrol)
                                .background(WarnaIos.Isian),
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = { bukti?.delete(); bukti = null },
                                colors = ButtonDefaults.textButtonColors(contentColor = NadaIos.BAHAYA.teks),
                            ) { Text("Hapus Foto") }
                            TextButton(
                                onClick = { galeriBukti.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                colors = ButtonDefaults.textButtonColors(contentColor = WarnaIos.Aksen),
                            ) { Text("Ganti Foto") }
                        }
                    }
                    else -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TombolBukti(
                            ikon = IkonIos.PhotoCamera,
                            label = "Kamera",
                            modifier = Modifier.weight(1f),
                            onKlik = {
                                val punyaIzin = ContextCompat.checkSelfPermission(konteks, Manifest.permission.CAMERA) ==
                                    PackageManager.PERMISSION_GRANTED
                                if (punyaIzin) kameraBukti = true else izinKamera.launch(Manifest.permission.CAMERA)
                            },
                        )
                        TombolBukti(
                            ikon = IkonIos.Image,
                            label = "Galeri",
                            modifier = Modifier.weight(1f),
                            onKlik = { galeriBukti.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        )
                    }
                }
            }

            if (error != null) {
                Spacer(Modifier.height(12.dp))
                Text(error, style = TipeIos.Catatan.copy(color = NadaIos.BAHAYA.teks))
            }

            Spacer(Modifier.height(24.dp))

            // Actions
            val buktiLengkap = !wajibBukti || bukti != null
            val canSubmit = !submitting && !memprosesBukti && startDate != null && endDate != null && buktiLengkap
            if (wajibBukti && bukti == null && !memprosesBukti) {
                Text(
                    "Lampirkan foto bukti untuk mengajukan sakit.",
                    style = TipeIos.Catatan.copy(color = WarnaIos.LabelKedua),
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            TombolUtamaIos(
                teks = if (submitting) "Mengirim..." else "Ajukan",
                onKlik = {
                    if (startDate != null && endDate != null && buktiLengkap) {
                        onSubmit(leaveType, startDate!!, endDate!!, reason, bukti.takeIf { wajibBukti })
                    }
                },
                aktif = canSubmit,
            )
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = { dismissAnimated() },
                modifier = Modifier.fillMaxWidth().height(UkuranIos.TinggiTombol),
                shape = UkuranIos.SudutBlok,
                colors = ButtonDefaults.textButtonColors(contentColor = WarnaIos.Aksen),
            ) {
                Text("Batal", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
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
private fun TombolBukti(
    ikon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onKlik: () -> Unit,
) {
    Row(
        modifier
            .height(UkuranIos.TinggiTombol)
            .clip(UkuranIos.SudutKontrol)
            .background(WarnaIos.Isian)
            .clickable(onClick = onKlik),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(ikon, contentDescription = null, tint = WarnaIos.Aksen, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, style = TipeIos.Keterangan.copy(color = WarnaIos.Label, fontWeight = FontWeight.SemiBold))
    }
}

@Composable
private fun LabelBidang(teks: String) {
    Text(teks, style = TipeIos.Catatan.copy(color = WarnaIos.Label, fontWeight = FontWeight.SemiBold))
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
            .height(UkuranIos.TinggiTombol)
            .clip(UkuranIos.SudutKontrol)
            .background(WarnaIos.Isian)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = WarnaIos.Aksen, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            value ?: placeholder,
            style = TipeIos.Keterangan.copy(color = if (value != null) WarnaIos.Label else WarnaIos.Abu),
            modifier = Modifier.weight(1f),
        )
        Icon(IkonIos.ChevronRight, contentDescription = null, tint = WarnaIos.LabelKetiga, modifier = Modifier.size(15.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerModal(onDismiss: () -> Unit, onDatePicked: (Long) -> Unit) {
    val state = rememberDatePickerState()
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { state.selectedDateMillis?.let(onDatePicked) ?: onDismiss() }, colors = ButtonDefaults.textButtonColors(contentColor = WarnaIos.Aksen)) { Text("Pilih", fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = { TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = WarnaIos.LabelKedua)) { Text("Batal") } },
        colors = DatePickerDefaults.colors(containerColor = WarnaIos.Kartu),
    ) {
        DatePicker(
            state = state,
            colors = DatePickerDefaults.colors(
                containerColor = WarnaIos.Kartu,
                selectedDayContainerColor = WarnaIos.Aksen,
                todayDateBorderColor = WarnaIos.Aksen,
                todayContentColor = WarnaIos.Aksen,
                selectedYearContainerColor = WarnaIos.Aksen,
            ),
        )
    }
}
