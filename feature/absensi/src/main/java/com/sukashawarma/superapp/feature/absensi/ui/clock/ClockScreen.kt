package com.sukashawarma.superapp.presentation.absensi.clock

import com.sukashawarma.superapp.core.ui.kaca.LatarKaca
import com.sukashawarma.superapp.core.ui.kaca.panelKaca
import com.sukashawarma.superapp.core.ui.kaca.rememberLatarKaca
import com.sukashawarma.superapp.core.ui.kaca.sumberKaca
import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.data.face.NormalizedFaceContours
import com.sukashawarma.superapp.data.face.NormalizedPoint
import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.optString
import com.sukashawarma.superapp.domain.util.JakartaTime
import com.sukashawarma.superapp.domain.model.ClockPhase
import com.sukashawarma.superapp.domain.model.Role
import com.sukashawarma.superapp.domain.gps.GpsMath
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.core.ui.SukaDropdownHeader
import com.sukashawarma.superapp.core.ui.SukaDropdownMenu
import com.sukashawarma.superapp.core.ui.SukaDropdownMenuItem
import com.sukashawarma.superapp.presentation.components.FaceCameraPreview
import com.sukashawarma.superapp.presentation.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.sukashawarma.superapp.feature.absensi.R
import com.sukashawarma.superapp.presentation.absensi.enroll.captureJpeg
import androidx.compose.ui.res.painterResource
import com.sukashawarma.superapp.core.ui.AvatarStaf
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.RealtimeTables
import com.sukashawarma.superapp.core.ui.kaca.LocalRuangNavKaca
import com.sukashawarma.superapp.core.ui.ios.JudulSeksiIos
import com.sukashawarma.superapp.core.ui.ios.KartuIos
import com.sukashawarma.superapp.core.ui.ios.KeadaanIos
import com.sukashawarma.superapp.core.ui.ios.KolomCariIos
import com.sukashawarma.superapp.core.ui.ios.LabelSeksiIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.PemisahIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.TombolKapsulIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.ios.permukaanIos
import com.sukashawarma.superapp.core.ui.kaca.IkonIos

/**
 * Panel absen.
 *
 * [onAbsenMasukSelesai] dipanggil sekali tiap absen MASUK yang berhasil, supaya
 * pemanggil bisa mengantar kru ke langkah berikutnya. Default kosong: layar ini juga
 * dipakai sebagai rute yang berdiri sendiri, dan di sana tidak ada checklist untuk
 * dituju.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClockScreen(
    isActive: Boolean = true,
    onExit: () -> Unit,
    onAbsenMasukSelesai: () -> Unit = {},
) {
    val context = LocalContext.current
    val staff by AppSession.staff.collectAsState()

    // Outlet mana yang boleh dipakai absen ditentukan oleh penempatan user, bukan role
    // semata — lihat [AttendanceOutletsViewModel]. ViewModel-nya terpisah karena
    // ClockViewModel di-recreate setiap outlet berganti.
    val outletsViewModel: AttendanceOutletsViewModel = viewModel(
        key = "attendance-outlets-${staff?.id}",
        factory = AttendanceOutletsViewModelFactory(context.applicationContext as android.app.Application),
    )
    val outletsState by outletsViewModel.state.collectAsState()
    var outletMenuExpanded by remember { mutableStateOf(false) }

    val outletId = outletsState.selectedId
    val selectedOutletName = outletsState.selected?.name ?: staff?.outletName
    val allowManual = staff?.allowManualButton == true

    // Selama daftar outlet masih dimuat, jangan tampilkan kartu "belum terhubung" — itu
    // bukan kesimpulan yang bisa diambil sebelum datanya masuk.
    if (outletId == null && outletsState.loading) {
        Scaffold(containerColor = WarnaIos.Latar) { padding ->
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = WarnaIos.Aksen, strokeWidth = 2.5.dp, modifier = Modifier.size(28.dp))
            }
        }
        return
    }

    if (outletId == null) {
        Scaffold(containerColor = WarnaIos.Latar) { padding ->
            Box(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(UkuranIos.TepiLayar),
                contentAlignment = Alignment.Center
            ) {
                KartuIos {
                    KeadaanIos(
                        ikon = IkonIos.Storefront,
                        judul = "Akun Belum Terhubung Outlet",
                        pesan = "Akun Anda belum terhubung ke outlet. Silakan hubungi admin atau SPV Anda.",
                        nada = NadaIos.AKSEN,
                    )
                }
            }
        }
        return
    }

    val viewModel: ClockViewModel = viewModel(
        key = "clock-${staff?.id}-$outletId",
        factory = ClockViewModelFactory(
            application = context.applicationContext as android.app.Application,
            outletId = outletId,
            lockToStaffId = staff?.id,
            staffRole = staff?.role?.value ?: staff?.roleRaw,
        )
    )
    val state by viewModel.state.collectAsState()
    RealtimeRefresh(RealtimeTables.ATTENDANCE) { viewModel.refreshAttendance() }

    // Jeda sebelum berpindah: pesan "Selamat bekerja!" dan ripple perayaannya (900 ms)
    // dibiarkan tuntas lebih dulu. Berpindah seketika membuat kru tidak sempat melihat
    // absennya benar-benar tercatat.
    val antarKeCeklis by rememberUpdatedState(onAbsenMasukSelesai)
    LaunchedEffect(viewModel) {
        viewModel.absenMasukBerhasil.collect {
            delay(1300)
            antarKeCeklis()
        }
    }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCameraPermission = permissions[Manifest.permission.CAMERA] ?: hasCameraPermission
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: hasLocationPermission

        if (hasLocationPermission) {
            viewModel.checkLocation()
        }
    }

    // Modal izin kamera/lokasi ditahan sampai shift dipilih, supaya dua dialog tidak
    // bertumpuk di atas modal pilih shift (§6.1 dokumen).
    var izinSudahDiminta by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.shiftContextReady, state.perluPilihShift) {
        if (izinSudahDiminta || !state.shiftContextReady || state.perluPilihShift) return@LaunchedEffect
        izinSudahDiminta = true
        val permissionsToRequest = mutableListOf<String>()
        if (!hasCameraPermission) permissionsToRequest.add(Manifest.permission.CAMERA)
        if (!hasLocationPermission) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    var lastFrameMs by remember { mutableStateOf(0L) }
    // Pulse dot animation (status badge)
    val infiniteTransition = rememberInfiniteTransition(label = "statusPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val todayFormatted = formatJakartaDate(JakartaTime.now().toLocalDate())
    val latestHistoryToday = state.attendanceHistory.firstOrNull {
        attendanceInstant(it.occurredAtIso)?.atZone(JakartaTime.ZONE)?.toLocalDate() == JakartaTime.now().toLocalDate()
    }
    val currentStatusText = when (latestHistoryToday?.type) {
        "in" -> "Sedang Bekerja"
        "out" -> "Shift Selesai"
        else -> "Belum Clock In"
    }

    val scrollState = rememberScrollState()
    // 0..1 halus mengikuti jarak scroll (bukan boolean on/off) supaya micro-animasi header
    // (radius, shadow, ukuran avatar) terasa nge-blend, bukan meloncat.
    val scrollProgress by remember { derivedStateOf { (scrollState.value / 80f).coerceIn(0f, 1f) } }

    Scaffold(
        containerColor = WarnaIos.Latar,
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(WarnaIos.Latar)
        ) {
            // Top App Bar — di LUAR area scroll (bukan child dari Column yang di-scroll)
            // supaya selalu ikut/menempel di atas, bukan ikut ter-scroll menghilang.
            TopBar(
                staffName = staff?.namaTampil ?: staff?.name,
                avatarPath = staff?.avatarUrl,
                outletName = selectedOutletName,
                shiftInfo = state.selectedShift?.let { "${it.nama} (${it.rentang})" },
                isOnline = state.isOnline,
                onBackClick = onExit,
                scrollProgress = scrollProgress,
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    if (outletsState.hasChoice || outletsState.error != null) {
                        AttendanceOutletSelector(
                            state = outletsState,
                            expanded = outletMenuExpanded,
                            onExpandedChange = { outletMenuExpanded = it },
                            onSelect = { outletsViewModel.selectOutlet(it) },
                            onRedetect = { outletsViewModel.redetect() },
                            onRetry = { outletsViewModel.load() },
                        )
                    }

                    val shiftTerpilih = state.selectedShift
                    if (shiftTerpilih != null && state.nextAction == com.sukashawarma.superapp.domain.usecase.NextAction.IN) {
                        KartuShiftTerpilih(
                            opsi = shiftTerpilih,
                            ubahEnabled = state.phase != ClockPhase.IDENTIFIED &&
                                state.phase != ClockPhase.LIVENESS &&
                                state.phase != ClockPhase.SUBMITTING,
                            onUbah = viewModel::changeShift,
                        )
                    }

                    // Camera Action Area
                    ActionArea(
                        isActive = isActive,
                        hasCameraPermission = hasCameraPermission,
                        allowManual = allowManual,
                        staff = staff,
                        state = state,
                        viewModel = viewModel,
                        lastFrameMs = lastFrameMs,
                        onUpdateLastFrameMs = { lastFrameMs = it }
                    )

                    // Status Card
                    StatusHariIni(todayFormatted, currentStatusText, pulseAlpha)

                    // History Section
                    HistorySection(
                        history = state.attendanceHistory,
                        isLoading = state.isAttendanceLoading,
                        error = state.attendanceError,
                        onRefresh = viewModel::refreshAttendance,
                    )

                    Spacer(Modifier.height(32.dp))
                }
                // Sebagai tab pager, riwayat terakhir berada di balik kapsul tab kaca;
                // di rute CLOCK yang berdiri sendiri nilainya nol.
                Spacer(Modifier.height(LocalRuangNavKaca.current))
            }
        }
    }

    val opsiShift = state.shiftOptions
    if (isActive && opsiShift != null && state.perluPilihShift) {
        PilihShiftSheet(
            staffName = staff?.name,
            options = opsiShift,
            onPick = viewModel::pickShift,
            onExit = onExit,
        )
    }
}

/**
 * Pemilih outlet absen. Tampil untuk siapa pun yang terhubung ke lebih dari satu outlet —
 * leader, area manager, regional manager, atau kru yang punya baris `staff_outlets`
 * tambahan. Outlet terdekat sudah dikunci otomatis oleh [AttendanceOutletsViewModel];
 * daftar ini untuk mengoreksi manual saat dua outlet berdekatan atau GPS meleset.
 */
@Composable
private fun AttendanceOutletSelector(
    state: AttendanceOutletsUiState,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (String) -> Unit,
    onRedetect: () -> Unit,
    onRetry: () -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredOutlets = state.outlets.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
    val selected = state.selected
    val nearestId = state.outlets.firstOrNull { it.distanceM != null }?.id

    LaunchedEffect(expanded) {
        if (!expanded) searchQuery = ""
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LabelSeksiIos("Lokasi absen", Modifier.weight(1f).padding(start = 4.dp))
            if (state.hasChoice && !state.autoDetected && !state.locating) {
                TextButton(
                    onClick = onRedetect,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                ) {
                    Text("Deteksi ulang", fontSize = 13.sp, color = WarnaIos.Aksen, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .permukaanIos(UkuranIos.SudutGrup)
                    .clickable(enabled = state.outlets.isNotEmpty()) { onExpandedChange(true) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                    Box(
                        Modifier.size(32.dp).clip(CircleShape).background(WarnaIos.Aksen.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = IkonIos.Storefront,
                            contentDescription = null,
                            tint = WarnaIos.Aksen,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selected?.name ?: if (state.loading) "Memuat outlet..." else "Pilih outlet",
                            style = TipeIos.Isi.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val selectedDistance = selected?.distanceM
                        Text(
                            text = when {
                                state.loading -> "Menyiapkan daftar outlet"
                                state.locating -> "Mendeteksi outlet terdekat..."
                                state.autoDetected && selectedDistance != null ->
                                    "Terdeteksi otomatis · " + formatDistanceShort(selectedDistance) + " dari Anda"
                                selectedDistance != null ->
                                    formatDistanceShort(selectedDistance) + " dari Anda"
                                else -> state.outlets.size.toString() + " outlet terhubung ke akun Anda"
                            },
                            style = TipeIos.Catatan,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (state.loading || state.locating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = WarnaIos.Aksen,
                        )
                    } else {
                        Icon(
                            imageVector = IkonIos.ExpandMore,
                            contentDescription = "Ganti outlet",
                            tint = WarnaIos.LabelKetiga,
                            modifier = Modifier.size(16.dp),
                        )
                    }
            }

            SukaDropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) },
                modifier = Modifier
                    .width(300.dp)
                    .heightIn(max = 430.dp),
            ) {
                Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                    if (state.outlets.size > 6) {
                        KolomCariIos(
                            nilai = searchQuery,
                            onUbah = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = "Cari outlet",
                        )
                        Spacer(Modifier.height(6.dp))
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 340.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        if (filteredOutlets.isEmpty()) {
                            Text(
                                "Outlet tidak ditemukan",
                                style = TipeIos.Catatan,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 18.dp),
                            )
                        } else filteredOutlets.forEach { outlet ->
                            val isSelected = outlet.id == state.selectedId
                            val outletDistance = outlet.distanceM
                            SukaDropdownMenuItem(
                                text = outlet.name,
                                subtitle = if (outletDistance != null) {
                                    formatDistanceShort(outletDistance) + " dari Anda"
                                } else {
                                    "Jarak belum terukur"
                                },
                                badgeText = if (outlet.id == nearestId) "Terdekat" else null,
                                selected = isSelected,
                                leadingIcon = IkonIos.Storefront,
                                onClick = {
                                    onSelect(outlet.id)
                                    onExpandedChange(false)
                                },
                            )
                        }
                    }
                }
            }
        }

        if (state.error != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = state.error,
                    style = TipeIos.Catatan.copy(color = NadaIos.BAHAYA.teks),
                    modifier = Modifier.weight(1f).padding(start = 4.dp),
                )
                TombolKapsulIos("Coba lagi", onRetry)
            }
        }
    }
}

/**
 * Posisi & ukuran wajah terdeteksi, dinormalisasi jadi fraksi 0..1 relatif terhadap
 * preview kamera. Dipakai [FaceMeshOverlay] supaya mesh mengikuti wajah asli tiap frame
 * tanpa perlu landmark/contour ML Kit (murni bounding box, tetap ringan).
 */
private data class FaceTrackData(
    val cx: Float,
    val cy: Float,
    val w: Float,
    val h: Float,
    val yawDeg: Float,
)

/**
 * Face-mesh interaktif: oval wajah + cincin kedalaman + spoke radial + titik fitur
 * (mata/hidung/mulut), semuanya mengikuti posisi/skala/rotasi wajah terdeteksi lewat
 * animasi spring (presisi & responsif tiap gerakan, tanpa jitter di sample rate rendah).
 * Semua digambar dari trigonometri sederhana di satu Canvas — tanpa bitmap/landmark
 * tambahan, jadi ringan di CPU/GPU.
 */
/**
 * Faktor lebar wajah relatif terhadap [v] (posisi vertikal, -1 dahi .. +1 dagu), meniru
 * proporsi wajah manusia: melebar di area pipi/tulang pipi (v sekitar -0.1..0.2), lalu
 * menyempit progresif ke dahi dan terutama ke dagu — bukan lingkaran/oval generik.
 */
private fun faceWidthFactor(v: Float): Float {
    val browToForehead = 1f - 0.30f * (v * v)
    val jawTaper = if (v > 0.3f) 1f - (v - 0.3f) * 1.05f else 1f
    return (browToForehead * jawTaper).coerceIn(0.22f, 1f)
}

private fun lerpF(a: Float, b: Float, t: Float) = a + (b - a) * t

private fun lerpPts(a: List<NormalizedPoint>, b: List<NormalizedPoint>, t: Float): List<NormalizedPoint> {
    if (a.size != b.size || a.isEmpty()) return b
    return List(a.size) { i -> NormalizedPoint(lerpF(a[i].x, b[i].x, t), lerpF(a[i].y, b[i].y, t)) }
}

/** Smoothing eksponensial titik-per-titik (bukan `animateFloatAsState` per titik — mahal
 *  utk ~100+ titik) supaya mesh mengikuti wajah secara presisi tapi tetap rapi/tidak jitter
 *  di sample rate ML Kit yang tak selalu stabil. */
private fun lerpContours(a: NormalizedFaceContours, b: NormalizedFaceContours, t: Float) = NormalizedFaceContours(
    faceOval = lerpPts(a.faceOval, b.faceOval, t),
    leftEyebrowTop = lerpPts(a.leftEyebrowTop, b.leftEyebrowTop, t),
    leftEyebrowBottom = lerpPts(a.leftEyebrowBottom, b.leftEyebrowBottom, t),
    rightEyebrowTop = lerpPts(a.rightEyebrowTop, b.rightEyebrowTop, t),
    rightEyebrowBottom = lerpPts(a.rightEyebrowBottom, b.rightEyebrowBottom, t),
    leftEye = lerpPts(a.leftEye, b.leftEye, t),
    rightEye = lerpPts(a.rightEye, b.rightEye, t),
    noseBridge = lerpPts(a.noseBridge, b.noseBridge, t),
    noseBottom = lerpPts(a.noseBottom, b.noseBottom, t),
    upperLipTop = lerpPts(a.upperLipTop, b.upperLipTop, t),
    upperLipBottom = lerpPts(a.upperLipBottom, b.upperLipBottom, t),
    lowerLipTop = lerpPts(a.lowerLipTop, b.lowerLipTop, t),
    lowerLipBottom = lerpPts(a.lowerLipBottom, b.lowerLipBottom, t),
)

private fun DrawScope.toPx(p: NormalizedPoint) = Offset(p.x * size.width, p.y * size.height)

private fun DrawScope.meshPolyline(pts: List<NormalizedPoint>, closed: Boolean, color: Color, strokePx: Float) {
    if (pts.size < 2) return
    val path = Path().apply {
        val first = toPx(pts[0])
        moveTo(first.x, first.y)
        for (i in 1 until pts.size) {
            val p = toPx(pts[i])
            lineTo(p.x, p.y)
        }
        if (closed) close()
    }
    drawPath(path, color = color, style = Stroke(width = strokePx))
}

/** Sambungkan dua kontur bersebelahan titik-demi-titik (proporsional thd ukurannya
 *  masing-masing) — inilah yang bikin efek "jaring segitiga" seperti face-mesh AR,
 *  dibangun dari titik kontur ASLI, bukan grid sintetis. */
private fun DrawScope.meshStrip(a: List<NormalizedPoint>, b: List<NormalizedPoint>, color: Color, strokePx: Float) {
    if (a.isEmpty() || b.isEmpty()) return
    val n = maxOf(a.size, b.size)
    for (i in 0 until n) {
        val pa = a[(i * a.size / n).coerceIn(0, a.size - 1)]
        val pb = b[(i * b.size / n).coerceIn(0, b.size - 1)]
        drawLine(color, toPx(pa), toPx(pb), strokeWidth = strokePx)
    }
}

private fun DrawScope.meshFan(ring: List<NormalizedPoint>, target: NormalizedPoint, color: Color, strokePx: Float, everyN: Int) {
    val targetPx = toPx(target)
    ring.forEachIndexed { i, p -> if (i % everyN == 0) drawLine(color, toPx(p), targetPx, strokeWidth = strokePx) }
}

private fun avgPoint(pts: List<NormalizedPoint>): NormalizedPoint? {
    if (pts.isEmpty()) return null
    var sx = 0f; var sy = 0f
    pts.forEach { sx += it.x; sy += it.y }
    return NormalizedPoint(sx / pts.size, sy / pts.size)
}

/**
 * Menggambar mesh dari titik kontur wajah ASLI (ML Kit `CONTOUR_MODE_ALL`) — outline wajah,
 * alis, mata, hidung, bibir semuanya presisi mengikuti bentuk wajah pengguna, disambung jadi
 * jaring segitiga/garis ala AR face-mesh (bukan oval prosedural).
 */
private fun DrawScope.drawRealFaceMesh(mesh: NormalizedFaceContours, color: Color, pulse: Float) {
    val lineAlpha = color.alpha * 0.4f
    val line = color.copy(alpha = lineAlpha)
    val thinLine = color.copy(alpha = lineAlpha * 0.55f)
    val strokePx = 1.dp.toPx()
    val thinStrokePx = 0.7.dp.toPx()

    val center = avgPoint(mesh.noseBottom) ?: avgPoint(mesh.faceOval)
    if (center != null && mesh.faceOval.isNotEmpty()) {
        meshFan(mesh.faceOval, center, thinLine, thinStrokePx, everyN = 3)
    }

    meshPolyline(mesh.faceOval, closed = true, color = line, strokePx = strokePx)
    meshPolyline(mesh.leftEyebrowTop, closed = false, color = line, strokePx = strokePx)
    meshPolyline(mesh.leftEyebrowBottom, closed = false, color = line, strokePx = strokePx)
    meshPolyline(mesh.rightEyebrowTop, closed = false, color = line, strokePx = strokePx)
    meshPolyline(mesh.rightEyebrowBottom, closed = false, color = line, strokePx = strokePx)
    meshStrip(mesh.leftEyebrowTop, mesh.leftEyebrowBottom, thinLine, thinStrokePx)
    meshStrip(mesh.rightEyebrowTop, mesh.rightEyebrowBottom, thinLine, thinStrokePx)
    meshStrip(mesh.leftEyebrowBottom, mesh.leftEye, thinLine, thinStrokePx)
    meshStrip(mesh.rightEyebrowBottom, mesh.rightEye, thinLine, thinStrokePx)

    meshPolyline(mesh.leftEye, closed = true, color = line, strokePx = strokePx)
    meshPolyline(mesh.rightEye, closed = true, color = line, strokePx = strokePx)

    meshPolyline(mesh.noseBridge, closed = false, color = line, strokePx = strokePx)
    meshPolyline(mesh.noseBottom, closed = false, color = line, strokePx = strokePx)
    meshStrip(mesh.noseBottom, mesh.upperLipTop, thinLine, thinStrokePx)

    meshPolyline(mesh.upperLipTop, closed = false, color = line, strokePx = strokePx)
    meshPolyline(mesh.upperLipBottom, closed = false, color = line, strokePx = strokePx)
    meshPolyline(mesh.lowerLipTop, closed = false, color = line, strokePx = strokePx)
    meshPolyline(mesh.lowerLipBottom, closed = false, color = line, strokePx = strokePx)
    meshStrip(mesh.upperLipTop, mesh.upperLipBottom, thinLine, thinStrokePx)
    meshStrip(mesh.lowerLipTop, mesh.lowerLipBottom, thinLine, thinStrokePx)
    meshStrip(mesh.upperLipBottom, mesh.lowerLipTop, thinLine, thinStrokePx)

    // Titik vertex — dot kecil tiap simpul mesh, sedikit berdenyut
    val dotAlpha = color.alpha * (0.55f + 0.35f * pulse)
    val dotColor = color.copy(alpha = dotAlpha)
    val dotRadius = 1.5.dp.toPx()
    val allPts = mesh.faceOval.asSequence() + mesh.leftEyebrowTop + mesh.leftEyebrowBottom +
        mesh.rightEyebrowTop + mesh.rightEyebrowBottom + mesh.leftEye + mesh.rightEye +
        mesh.noseBridge + mesh.noseBottom + mesh.upperLipTop + mesh.upperLipBottom +
        mesh.lowerLipTop + mesh.lowerLipBottom
    allPts.forEach { p -> drawCircle(color = dotColor, radius = dotRadius, center = toPx(p)) }
}

@Composable
private fun FaceMeshOverlay(
    faceTrack: FaceTrackData?,
    faceMesh: NormalizedFaceContours?,
    isDetecting: Boolean,
    accentColor: Color = SukaOrange,
    celebrate: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val hasFace = faceTrack != null
    val trackSpec = spring<Float>(dampingRatio = 0.9f, stiffness = 300f)
    // Fit inset: bounding box ML Kit sedikit lebih besar dari wajah asli (termasuk margin
    // rambut/dahi/rahang), jadi mesh dipersempit tipis agar menempel presisi ke wajah.
    val cx by animateFloatAsState(faceTrack?.cx ?: 0.5f, trackSpec, label = "meshCx")
    val cy by animateFloatAsState((faceTrack?.cy ?: 0.44f) - (faceTrack?.let { 0.03f } ?: 0f), trackSpec, label = "meshCy")
    val halfW by animateFloatAsState((faceTrack?.w ?: 0.46f) / 2f * 0.88f, trackSpec, label = "meshW")
    val halfH by animateFloatAsState((faceTrack?.h ?: 0.58f) / 2f * 0.92f, trackSpec, label = "meshH")
    val yaw by animateFloatAsState(faceTrack?.yawDeg ?: 0f, trackSpec, label = "meshYaw")
    val lockAlpha by animateFloatAsState(if (hasFace) 1f else 0.45f, tween(260), label = "meshLock")
    val meshColor by animateColorAsState(accentColor, tween(320), label = "meshAccent")
    // Crossfade: oval prosedural (dipakai sebelum wajah presisi terdeteksi) -> mesh titik
    // kontur ASLI (presisi ke bentuk wajah) begitu ML Kit mengembalikan data kontur.
    val meshPresence by animateFloatAsState(if (faceMesh != null) 1f else 0f, tween(280), label = "meshPresence")

    val infinite = rememberInfiniteTransition(label = "meshPulse")
    val pulse by infinite.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "meshPulseAlpha"
    )
    val sweepAngle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing), RepeatMode.Restart),
        label = "meshSweep"
    )

    // Ripple satu-kali saat status berubah (mis. baru selesai) — sinyal "modern" tanpa
    // menyembunyikan mesh yang tetap hidup di baliknya.
    val ripple = remember { Animatable(0f) }
    LaunchedEffect(celebrate) {
        if (celebrate) {
            ripple.snapTo(0f)
            ripple.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
        }
    }

    Canvas(modifier = modifier) {
        val cxPx = size.width * cx
        val cyPx = size.height * cy
        val rxPx = size.width * halfW
        val ryPx = size.height * halfH
        val yawShiftPx = (yaw / 45f).coerceIn(-1f, 1f) * rxPx * 0.18f

        fun pt(u: Float, v: Float): Offset {
            val taper = faceWidthFactor(v)
            val x = cxPx + u * rxPx * taper + yawShiftPx * (v * 0.5f + 0.5f)
            val y = cyPx + v * ryPx
            return Offset(x, y)
        }

        // Elemen prosedural memudar begitu mesh presisi (titik kontur asli) siap tampil.
        val proceduralAlpha = 1f - meshPresence
        val mc = meshColor.copy(alpha = lockAlpha * proceduralAlpha)

        // Outline wajah mengikuti proporsi (lebar di pipi, sempit di dahi & dagu)
        val outlinePts = (0 until 40).map { i ->
            val a = (i / 40f) * (2 * Math.PI).toFloat()
            pt(kotlin.math.cos(a), kotlin.math.sin(a))
        }
        val outlinePath = Path().apply {
            outlinePts.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }
            close()
        }
        drawPath(outlinePath, color = mc.copy(alpha = mc.alpha * 0.6f), style = Stroke(width = 1.4.dp.toPx()))

        // Cincin kedalaman (dotted), efek "mesh"
        listOf(0.4f, 0.66f, 0.9f).forEachIndexed { ringIdx, scale ->
            val ringAlpha = mc.alpha * (0.5f - ringIdx * 0.1f) * (0.6f + 0.4f * pulse)
            for (i in 0 until 28) {
                val a = (i / 28f) * (2 * Math.PI).toFloat()
                val p = pt(kotlin.math.cos(a) * scale, kotlin.math.sin(a) * scale)
                drawCircle(color = mc.copy(alpha = ringAlpha), radius = 1.6.dp.toPx(), center = p)
            }
        }

        // Spoke radial dari pusat ke outline
        for (i in 0 until 18) {
            val a = (i / 18f) * (2 * Math.PI).toFloat()
            val inner = pt(kotlin.math.cos(a) * 0.15f, kotlin.math.sin(a) * 0.15f)
            val outer = pt(kotlin.math.cos(a), kotlin.math.sin(a))
            drawLine(mc.copy(alpha = mc.alpha * 0.16f), inner, outer, strokeWidth = 0.8.dp.toPx())
        }

        // Titik fitur: alis, mata, hidung, mulut, rahang
        val browL = pt(-0.34f, -0.42f)
        val browR = pt(0.34f, -0.42f)
        val eyeL = pt(-0.32f, -0.15f)
        val eyeR = pt(0.32f, -0.15f)
        val nose = pt(0f, 0.16f)
        val mouth = pt(0f, 0.52f)
        val jawL = pt(-0.5f, 0.55f)
        val jawR = pt(0.5f, 0.55f)
        val chin = pt(0f, 0.92f)
        val features = listOf(browL, browR, eyeL, eyeR, nose, mouth, jawL, jawR, chin)
        features.forEach { p ->
            drawCircle(color = mc.copy(alpha = (0.7f + 0.3f * pulse) * lockAlpha * proceduralAlpha), radius = 2.4.dp.toPx(), center = p)
            drawCircle(color = mc.copy(alpha = 0.16f * lockAlpha * pulse * proceduralAlpha), radius = 8.dp.toPx(), center = p)
        }
        val featureAlpha = 0.35f * lockAlpha * proceduralAlpha
        drawLine(mc.copy(alpha = featureAlpha), browL, eyeL, strokeWidth = 1.dp.toPx())
        drawLine(mc.copy(alpha = featureAlpha), browR, eyeR, strokeWidth = 1.dp.toPx())
        drawLine(mc.copy(alpha = featureAlpha), eyeL, nose, strokeWidth = 1.dp.toPx())
        drawLine(mc.copy(alpha = featureAlpha), eyeR, nose, strokeWidth = 1.dp.toPx())
        drawLine(mc.copy(alpha = featureAlpha), nose, mouth, strokeWidth = 1.dp.toPx())
        drawLine(mc.copy(alpha = featureAlpha), jawL, chin, strokeWidth = 1.dp.toPx())
        drawLine(mc.copy(alpha = featureAlpha), jawR, chin, strokeWidth = 1.dp.toPx())

        // Mesh presisi dari titik kontur wajah ASLI — menggantikan oval prosedural begitu
        // ML Kit mengembalikan kontur (fade-in lewat meshPresence di atas).
        if (faceMesh != null && meshPresence > 0.01f) {
            drawRealFaceMesh(faceMesh, meshColor.copy(alpha = lockAlpha * meshPresence), pulse)
        }



        // Ripple selesai (sukses/gagal) — cincin membesar & memudar, tanpa menutupi mesh
        if (ripple.value > 0f) {
            val rippleProgress = ripple.value
            val rippleRx = rxPx * (1f + rippleProgress * 0.7f)
            val rippleRy = ryPx * (1f + rippleProgress * 0.7f)
            drawOval(
                color = mc.copy(alpha = (1f - rippleProgress) * 0.55f),
                topLeft = Offset(cxPx - rippleRx, cyPx - rippleRy),
                size = Size(rippleRx * 2f, rippleRy * 2f),
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

/**
 * Memastikan setiap nilai jarak/akurasi >= 1000 m dalam pesan UI ditampilkan dengan satuan km.
 * Contoh: "Di luar jangkauan (jarak 8502 m)" -> "Di luar jangkauan (jarak 8.5 km)"
 */
internal fun formatDistanceInMessage(message: String): String {
    val regex = Regex("""\b(\d+(?:\.\d+)?)\s*m\b""", RegexOption.IGNORE_CASE)
    return regex.replace(message) { match ->
        val meters = match.groupValues[1].toDoubleOrNull()
        if (meters != null && meters >= 1000.0) {
            GpsMath.formatDistance(meters)
        } else {
            match.value
        }
    }
}

/**
 * Modern non-camera state overlay matching the design system
 */
@Composable
private fun ClockModernOverlay(
    state: ClockUiState,
    allowManual: Boolean,
    staffId: String?,
    staffName: String?,
    onManualClick: () -> Unit,
    onRetryLocation: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (state.phase == ClockPhase.LOCATION_INVALID) Color(0xFFF1F4F5) else Color(0xFF1E293B))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        when (state.phase) {
            ClockPhase.LOCATING -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    LocationScanAnimation()
                    Text(
                        "Memeriksa Lokasi GPS...",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            ClockPhase.LOCATION_INVALID -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(25.dp)
                        )
                    }
                    Text(
                        "Di luar jangkauan",
                        color = Color(0xFF1E293B),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        formatDistanceInMessage(state.result?.message ?: "Lokasi Anda belum berada di area outlet."),
                        color = SukaOrange,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Silakan mendekat ke area kasir.\nPastikan GPS aktif dan akurat.",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(2.dp))
                    Button(
                        onClick = onRetryLocation,
                        colors = ButtonDefaults.buttonColors(containerColor = SukaOrange),
                        shape = RoundedCornerShape(11.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 9.dp),
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Coba Lagi")
                    }
                }
            }

            ClockPhase.LOCKED -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Text(
                        state.result?.message ?: "Absensi Terkunci",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            ClockPhase.SUBMITTING -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        color = SukaOrange,
                        modifier = Modifier.size(40.dp),
                        strokeWidth = 3.dp
                    )
                    Text(
                        "Menyimpan absensi...",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            ClockPhase.RESULT -> {
                val ok = state.result?.ok == true
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(if (ok) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (ok) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (ok) Color(0xFF10B981) else Color(0xFFEF4444),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Text(
                        state.result?.message ?: (if (ok) "Absensi Berhasil!" else "Absensi Gagal"),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            else -> {}
        }
    }
}

/**
 * Parses timestamp and checks if it's from today
 */

@Composable
private fun TopBar(
    staffName: String?,
    avatarPath: String?,
    outletName: String?,
    shiftInfo: String?,
    isOnline: Boolean = true,
    onBackClick: () -> Unit,
    scrollProgress: Float = 0f,
) {
    val cornerRadius by animateDpAsState(
        targetValue = lerp(26.dp, 0.dp, scrollProgress),
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "topBarCorner"
    )
    val elevation by animateDpAsState(
        targetValue = lerp(4.dp, 8.dp, scrollProgress),
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "topBarElevation"
    )
    val avatarSize by animateDpAsState(
        targetValue = lerp(44.dp, 36.dp, scrollProgress),
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "topBarAvatar"
    )

    // Jam digital real-time (WIB) yang otomatis diperbarui
    var currentTimeStr by remember {
        mutableStateOf(
            JakartaTime.now().format(DateTimeFormatter.ofPattern("HH:mm 'WIB'", Locale("id", "ID")))
        )
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(10_000)
            currentTimeStr = JakartaTime.now().format(DateTimeFormatter.ofPattern("HH:mm 'WIB'", Locale("id", "ID")))
        }
    }

    // Sapaan hangat dinamis berdasarkan waktu di Jakarta
    val currentHour = remember { JakartaTime.now().hour }
    val sapaanWaktu = when (currentHour) {
        in 4..10 -> "Selamat Pagi"
        in 11..14 -> "Selamat Siang"
        in 15..17 -> "Selamat Sore"
        else -> "Selamat Malam"
    }

    // Gradien hangat khas brand SUKA Shawarma (Suka Orange ke Deep Amber)
    val warmGradient = Brush.verticalGradient(
        listOf(
            Color(0xFFF29744), // Suka Orange
            Color(0xFFE86F21),
            Color(0xFFC7550C), // Deep warm spice
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation, RoundedCornerShape(bottomStart = cornerRadius, bottomEnd = cornerRadius))
            .clip(RoundedCornerShape(bottomStart = cornerRadius, bottomEnd = cornerRadius))
            .background(warmGradient)
    ) {
        // Lapisan Wallpaper Doodle Infinity (Mirip Chat Wallpaper SUKA Shawarma - Shawarma, Clock, Chef Hat, Spices)
        Image(
            painter = painterResource(com.sukashawarma.superapp.feature.absensi.R.drawable.bg_header_doodle_pattern),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .matchParentSize()
                .alpha(0.16f)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 20.dp,
                    top = 44.dp,
                    bottom = lerp(20.dp, 12.dp, scrollProgress)
                )
        ) {
            // Baris Atas: Tombol Back & Identitas Staf (Dikelompokkan Rapi di Kiri)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tombol Back frosted glass bulat
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f))
                        .clickable(onClick = onBackClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali ke daftar aplikasi",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Profil Avatar Staf bulat dengan border putih bersih
                Box(
                    modifier = Modifier.size(avatarSize),
                    contentAlignment = Alignment.Center
                ) {
                    AvatarStaf(
                        path = avatarPath,
                        nama = staffName,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .border(BorderStroke(2.dp, Color.White), CircleShape),
                        warnaLatar = Color.White.copy(alpha = 0.35f),
                        warnaHuruf = Color.White,
                    )
                    if (isOnline) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                                .border(BorderStroke(1.5.dp, Color.White), CircleShape)
                                .align(Alignment.BottomEnd)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Nama Modul & Status Online
                Column(
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "SUKA Kerja",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 1.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isOnline) Color(0xFF10B981) else Color.White.copy(alpha = 0.6f))
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (scrollProgress > 0.5f && !outletName.isNullOrBlank()) {
                                "Outlet $outletName"
                            } else if (isOnline) {
                                "Online"
                            } else {
                                "Offline"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.9f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Bagian Hero yang dapat mengerut halus saat di-scroll (Greeting & Pills)
            AnimatedVisibility(
                visible = scrollProgress < 0.5f,
                enter = expandVertically(animationSpec = tween(200)) + fadeIn(animationSpec = tween(150)),
                exit = shrinkVertically(animationSpec = tween(200)) + fadeOut(animationSpec = tween(150))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { alpha = (1f - scrollProgress * 2f).coerceIn(0f, 1f) }
                ) {
                    Spacer(modifier = Modifier.height(14.dp))

                    // Baris Tengah: Sapaan Leluasa & Outlet
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "$sapaanWaktu, ${staffName?.trim() ?: "Kru"}!",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            color = Color.White,
                            letterSpacing = (-0.3).sp,
                            lineHeight = 28.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Outlet ${outletName ?: "Suka Shawarma"}",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.92f),
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Baris Pills Frosted Glass (Jam Real-time & Info Shift)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Pill 1: Jam Live WIB
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.White.copy(alpha = 0.22f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = currentTimeStr,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Pill 2: Info Shift
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.White.copy(alpha = 0.22f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Event,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = shiftInfo ?: "Presensi Kehadiran",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GreetingSection(staffName: String?, outletName: String?) {
    // Sapaan telah terintegrasi langsung di Hero TopBar hangat di atas
}

@Composable
private fun StatusHariIni(todayFormatted: String, currentStatusText: String, pulseAlpha: Float) {
    KartuIos(padding = PaddingValues(horizontal = 18.dp, vertical = 16.dp)) {
        Text(text = "Status Hari ini", style = TipeIos.Catatan)
        Spacer(Modifier.height(2.dp))
        Text(text = todayFormatted, style = TipeIos.Utama)
        Spacer(Modifier.height(12.dp))
        // Lencana bernada aksen ala LencanaIos, dengan titik berdenyut sebagai penanda "live".
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(UkuranIos.SudutKapsul)
                .background(WarnaIos.Aksen.copy(alpha = 0.14f))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(WarnaIos.Aksen.copy(alpha = pulseAlpha))
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = currentStatusText,
                color = NadaIos.AKSEN.teks,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun HistorySection(
    history: List<AttendanceHistoryItem>,
    isLoading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        JudulSeksiIos("Riwayat Absensi Terakhir")

        if (isLoading) {
            KartuIos {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = WarnaIos.Aksen)
                }
            }
        } else if (error != null) {
            KartuIos {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(error, style = TipeIos.SubJudul, textAlign = TextAlign.Center)
                    TombolKapsulIos("Coba lagi", onRefresh)
                }
            }
        } else if (history.isEmpty()) {
            KartuIos {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Belum ada riwayat absensi", style = TipeIos.SubJudul)
                }
            }
        } else {
            // Satu grup inset iOS berisi baris-baris riwayat, dipisah garis hairline.
            Column(Modifier.fillMaxWidth().permukaanIos(UkuranIos.SudutKartu)) {
                history.forEachIndexed { index, item ->
                    val isClockIn = item.type == "in"
                    val (timeStr, relativeTag) = formatAttendanceTimeAndTag(item.occurredAtIso)
                    if (index > 0) PemisahIos(inset = 70.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background((if (isClockIn) WarnaIos.Hijau else WarnaIos.Merah).copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isClockIn) Icons.AutoMirrored.Filled.Login else Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = if (isClockIn) "Clock In" else "Clock Out",
                                    tint = if (isClockIn) NadaIos.SUKSES.teks else NadaIos.BAHAYA.teks,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = if (isClockIn) "Clock In" else "Clock Out",
                                    style = TipeIos.Utama.copy(fontSize = 16.sp),
                                )
                                Text(text = relativeTag, style = TipeIos.Catatan)
                            }
                        }
                        Text(
                            text = timeStr,
                            style = TipeIos.Keterangan.copy(fontWeight = FontWeight.SemiBold),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraFeedbackBanner(
    visible: Boolean,
    message: String?,
    latar: LatarKaca?,
    title: String = "Yuk, coba lagi",
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(140)) + scaleIn(tween(180), initialScale = 0.94f),
        exit = fadeOut(tween(100)) + scaleOut(tween(120), targetScale = 0.96f),
    ) {
        // Kaca cair yang sama dengan tab bar: preview kamera di belakangnya dibiaskan
        // (diperbesar di tengah, ditekuk di tepi) lewat `latar` milik ActionArea.
        Box(Modifier.fillMaxWidth().panelKaca(latar, sudut = 18.dp)) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFBBF24).copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.CenterFocusStrong,
                        contentDescription = null,
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(21.dp),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        style = TeksDiAtasKaca,
                    )
                    Text(
                        text = message.orEmpty(),
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        style = TeksDiAtasKaca,
                    )
                }
            }
        }
    }
}

/**
 * Teks putih di atas kaca bening yang melayang di atas kamera: kaca tidak menggelapkan
 * latar, jadi bayangan tipis inilah yang menjaga teks tetap terbaca saat kamera
 * menyorot sesuatu yang terang.
 */
private val TeksDiAtasKaca = androidx.compose.ui.text.TextStyle(
    shadow = androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha = 0.55f), androidx.compose.ui.geometry.Offset(0f, 1f), 6f),
)

private data class PillStatus(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val tint: Color,
    val text: String,
    val spinner: Boolean,
)

/**
 * Pill info status di bawah frame kamera — SATU-SATUNYA tempat status berubah jadi teks
 * (icon + warna + pesan). Mesh wajah TIDAK pernah ditutup/dihilangkan oleh pill ini, baik
 * saat sukses maupun gagal — hanya teksnya yang berganti dengan transisi halus.
 */
@Composable
private fun CameraStatusPill(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    latar: LatarKaca?,
    showSpinner: Boolean = false,
) {
    Box(Modifier.panelKaca(latar)) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedContent(
                targetState = showSpinner to icon,
                transitionSpec = { (fadeIn(tween(180)) + scaleIn(tween(180), initialScale = 0.7f)) togetherWith fadeOut(tween(120)) },
                label = "pillIcon",
            ) { (spinner, ic) ->
                if (spinner) {
                    CircularProgressIndicator(color = tint, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                } else {
                    Icon(ic, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
                }
            }
            AnimatedContent(
                targetState = text,
                transitionSpec = { (fadeIn(tween(180)) + scaleIn(tween(180), initialScale = 0.96f)) togetherWith fadeOut(tween(120)) },
                label = "pillText",
            ) { t ->
                Text(
                    text = t,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    style = TeksDiAtasKaca,
                )
            }
        }
    }
}

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
@Composable
private fun ActionArea(
    isActive: Boolean,
    hasCameraPermission: Boolean,
    allowManual: Boolean,
    staff: com.sukashawarma.superapp.domain.model.StaffProfile?,
    state: ClockUiState,
    viewModel: ClockViewModel,
    lastFrameMs: Long,
    onUpdateLastFrameMs: (Long) -> Unit
) {
    val context = LocalContext.current
    // Kamera tetap TER-MOUNT (bind CameraX jalan di background) sepanjang izin ada, TIDAK
    // digating oleh fase LOCATING/LOCATION_INVALID/LOCKED lagi — dulu kamera baru mulai
    // bind begitu fase itu selesai, jadi user selalu melihat jeda hitam (PreviewView kosong
    // menunggu frame pertama) tiap kali overlay lokasi hilang. Sekarang overlay lokasi cuma
    // MENUTUPI kamera secara visual, kamera sudah "panas" & streaming di baliknya, jadi begitu
    // overlay hilang preview sudah live — tanpa kerja ekstra (masih 1 kamera, tetap ringan).
    val cameraMounted = isActive && hasCameraPermission
    val showScanUi = cameraMounted && when (state.phase) {
        ClockPhase.LOCATING, ClockPhase.LOCATION_INVALID, ClockPhase.LOCKED -> false
        else -> true
    }
    var faceTrack by remember { mutableStateOf<FaceTrackData?>(null) }
    var smoothedMesh by remember { mutableStateOf<NormalizedFaceContours?>(null) }
    var lastMeshSeenMs by remember { mutableStateOf(0L) }
    var imageCapture by remember { mutableStateOf<androidx.camera.core.ImageCapture?>(null) }
    val cameraExecutor = remember { ContextCompat.getMainExecutor(context) }

    LaunchedEffect(state.selfieCaptureRequestId) {
        if (state.selfieCaptureRequestId != null) {
            imageCapture?.captureJpeg(cameraExecutor) { result -> viewModel.onSelfieCaptured(result.getOrNull()) }
                ?: viewModel.onSelfieCaptured(null)
        }
    }

    val resultOk = state.result?.ok
    val isLocationInvalid = state.phase == ClockPhase.LOCATION_INVALID
    // Warna bingkai kamera mengikuti status yang sama dengan mesh (oranye = memindai,
    // hijau/merah = hasil) — satu bahasa visual, bukan navy statis yang sama terus.
    val frameAccentTarget = when {
        state.phase == ClockPhase.RESULT && resultOk == true -> Color(0xFF10B981)
        state.phase == ClockPhase.RESULT && resultOk == false -> Color(0xFFEF4444)
        state.phase == ClockPhase.IDLE && resultOk == false -> Color(0xFFF59E0B)
        else -> SukaOrange
    }
    val frameAccent by animateColorAsState(frameAccentTarget, tween(320), label = "frameAccent")
    // Mesh menjadi pembeda visual dari bingkai: biru saat memindai, sementara hasil
    // tetap memakai hijau/merah agar status berhasil/gagal langsung terbaca.
    val meshAccent = when {
        state.phase == ClockPhase.RESULT && resultOk == true -> Color(0xFF10B981)
        state.phase == ClockPhase.RESULT && resultOk == false -> Color(0xFFEF4444)
        else -> Color(0xFF3B82F6)
    }
    // Kartu status di atas kamera memakai kaca cair yang sama dengan tab bar. Sumbernya
    // hanya preview + mesh (bukan kartu itu sendiri), supaya kaca tidak membiaskan dirinya.
    val latarKamera = rememberLatarKaca()
    val glowPulse by rememberInfiniteTransition(label = "frameGlow").animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "frameGlowAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
    ) {
        // Glow ambient di belakang frame — lingkaran radial lembut yang berdenyut pelan,
        // warnanya sama dengan status saat ini, memberi kedalaman tanpa perlu blur berat.
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(2.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(frameAccent.copy(alpha = 0.32f * glowPulse), Color.Transparent),
                        radius = 620f,
                    ),
                    RoundedCornerShape(34.dp)
                )
        )

        Surface(
            modifier = Modifier
                .matchParentSize()
                .padding(10.dp),
            shape = RoundedCornerShape(28.dp),
            color = if (isLocationInvalid) Color(0xFFF1F4F5) else Color.Black,
            border = if (isLocationInvalid) BorderStroke(1.dp, Color(0xFFD8E0E2)) else BorderStroke(
                1.5.dp,
                Brush.linearGradient(colors = listOf(frameAccent.copy(alpha = 0.95f), Color.White.copy(alpha = 0.18f), frameAccent.copy(alpha = 0.6f)))
            ),
            shadowElevation = 14.dp
        ) {
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().sumberKaca(latarKamera)) {
                if (cameraMounted) {
                    FaceCameraPreview(
                        modifier = Modifier.fillMaxSize(),
                        isActive = isActive,
                        needsCrop = { state.phase == ClockPhase.IDLE },
                        onFrame = { frame ->
                            faceTrack = frame.faceBox?.let { box ->
                                FaceTrackData(cx = box.cx, cy = box.cy, w = box.w, h = box.h, yawDeg = frame.signal.yawDeg)
                            }
                            val mesh = frame.faceMesh
                            if (mesh != null) {
                                smoothedMesh = smoothedMesh?.let { lerpContours(it, mesh, 0.4f) } ?: mesh
                                lastMeshSeenMs = System.currentTimeMillis()
                            } else if (System.currentTimeMillis() - lastMeshSeenMs > 600L) {
                                smoothedMesh = null
                            }

                            val now = System.currentTimeMillis()
                            val minIntervalMs = if (state.phase == ClockPhase.LIVENESS) 200L else 250L
                            if (now - lastFrameMs < minIntervalMs) return@FaceCameraPreview
                            onUpdateLastFrameMs(now)
                            if (state.phase == ClockPhase.IDLE) viewModel.onIdleFrame(frame)
                            else if (state.phase == ClockPhase.LIVENESS) viewModel.onLivenessFrame(frame)
                        },
                        onImageCaptureReady = { imageCapture = it },
                    )
                }

                if (showScanUi) {
                    // The preview remains mounted through feedback, submitting, and result states.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.32f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.52f)
                                    )
                                )
                            )
                    )

                    // Mesh SELALU tampil selama kamera hidup. Aksen pemindaian biru membuatnya
                    // mudah dibedakan dari bingkai oranye; hasil tetap hijau/merah.
                    FaceMeshOverlay(
                        faceTrack = faceTrack,
                        faceMesh = smoothedMesh,
                        isDetecting = state.phase == ClockPhase.IDLE || state.phase == ClockPhase.LIVENESS || state.phase == ClockPhase.SUBMITTING,
                        accentColor = meshAccent,
                        celebrate = state.phase == ClockPhase.RESULT,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            if (showScanUi) {
                CameraFeedbackBanner(
                    visible = state.phase == ClockPhase.IDLE && resultOk == false,
                    message = state.result?.message?.let { formatDistanceInMessage(it) },
                    latar = latarKamera,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(18.dp),
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val (pillIcon, pillTint, pillText, pillSpinner) = when {
                            state.phase == ClockPhase.IDENTIFIED ->
                                PillStatus(Icons.Filled.Face, SukaOrange, "Halo, ${state.whoName ?: "-"}", false)
                            state.phase == ClockPhase.LIVENESS ->
                                PillStatus(Icons.Filled.Face, SukaOrange, state.challenge?.label ?: "Ikuti instruksi", false)
                            state.phase == ClockPhase.SUBMITTING ->
                                PillStatus(Icons.Filled.Face, SukaOrange, "Menyimpan absensi…", true)
                            state.phase == ClockPhase.RESULT && resultOk == true ->
                                PillStatus(Icons.Filled.CheckCircle, Color(0xFF10B981), state.result?.message ?: "Absensi berhasil", false)
                            state.phase == ClockPhase.RESULT ->
                                PillStatus(Icons.Filled.ErrorOutline, Color(0xFFEF4444), state.result?.message?.let { formatDistanceInMessage(it) } ?: "Absensi gagal, coba lagi", false)
                            else ->
                                PillStatus(Icons.Filled.Face, SukaOrange, "Posisikan wajah di tengah", false)
                        }
                        CameraStatusPill(text = pillText, icon = pillIcon, tint = pillTint, latar = latarKamera, showSpinner = pillSpinner)

                        if (allowManual && staff?.id != null && state.phase == ClockPhase.IDLE) {
                            Button(
                                onClick = { staff?.let { viewModel.doSubmitManual(it.id, it.name) } },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White.copy(alpha = 0.25f),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                elevation = ButtonDefaults.buttonElevation(0.dp)
                            ) {
                                Icon(Icons.Filled.TouchApp, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Absen Manual", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }

            if (!showScanUi) {
                // Menutupi kamera secara visual saja (mis. selama cek lokasi) — kamera di
                // baliknya tetap mount & streaming, jadi tidak ada jeda hitam saat overlay ini
                // hilang nanti.
                ClockModernOverlay(
                    state = state,
                    allowManual = allowManual,
                    staffId = staff?.id,
                    staffName = staff?.name,
                    onManualClick = { staff?.let { viewModel.doSubmitManual(it.id, it.name) } },
                    onRetryLocation = { viewModel.checkLocation() }
                )
            }
        }
        }

        // Aksen sudut ala viewfinder kamera premium — nempel di tepi luar frame, warnanya
        // ikut `frameAccent`, jadi identitasnya beda dari kotak polos sebelumnya.
        ViewfinderCorners(color = frameAccent, alpha = 0.55f + 0.45f * glowPulse)
    }
}

/** Empat aksen sudut L kecil di tepi luar frame kamera — sentuhan "viewfinder" seperti
 *  kamera mirrorless modern, dibangun dari garis sederhana (murah), bukan gambar/aset. */
@Composable
private fun ViewfinderCorners(color: Color, alpha: Float, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val len = 22.dp.toPx()
        val stroke = 3.dp.toPx()
        val inset = 2.dp.toPx()
        val w = size.width
        val h = size.height
        val c = color.copy(alpha = alpha)

        // Top-left
        drawLine(c, Offset(inset, inset + len), Offset(inset, inset), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(c, Offset(inset, inset), Offset(inset + len, inset), strokeWidth = stroke, cap = StrokeCap.Round)
        // Top-right
        drawLine(c, Offset(w - inset, inset + len), Offset(w - inset, inset), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(c, Offset(w - inset, inset), Offset(w - inset - len, inset), strokeWidth = stroke, cap = StrokeCap.Round)
        // Bottom-left
        drawLine(c, Offset(inset, h - inset - len), Offset(inset, h - inset), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(c, Offset(inset, h - inset), Offset(inset + len, h - inset), strokeWidth = stroke, cap = StrokeCap.Round)
        // Bottom-right
        drawLine(c, Offset(w - inset, h - inset - len), Offset(w - inset, h - inset), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(c, Offset(w - inset, h - inset), Offset(w - inset - len, h - inset), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}


private val indonesianLocale = Locale("id", "ID")
private val jakartaDateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM uuuu", indonesianLocale)
private val jakartaShortDateFormatter = DateTimeFormatter.ofPattern("d MMM uuuu", indonesianLocale)
private val jakartaTimeFormatter = DateTimeFormatter.ofPattern("HH:mm 'WIB'", indonesianLocale)

private fun formatJakartaDate(date: LocalDate): String = date.format(jakartaDateFormatter)

/** Server mengirim `timestamptz`; konversi selalu dilakukan dari instant ke WIB, bukan
 * timezone default perangkat. Fallback OffsetDateTime menjaga kompatibilitas format ISO lain. */
private fun attendanceInstant(timestamp: String): Instant? = runCatching {
    Instant.parse(timestamp)
}.recoverCatching {
    OffsetDateTime.parse(timestamp).toInstant()
}.getOrNull()

/** Format data absensi otoritatif dalam zona Indonesia barat. */
private fun formatAttendanceTimeAndTag(timestamp: String): Pair<String, String> {
    val dateTime = attendanceInstant(timestamp)?.atZone(JakartaTime.ZONE)
        ?: return Pair("—", "Waktu tidak tersedia")
    val today = JakartaTime.now().toLocalDate()
    val date = dateTime.toLocalDate()
    val tag = when (date) {
        today -> "Hari ini"
        today.minusDays(1) -> "Kemarin"
        else -> date.format(jakartaShortDateFormatter)
    }
    return dateTime.format(jakartaTimeFormatter) to tag
}

/** Pemindaian lokasi berbasis Canvas: tiga pulse, sweep tipis, dan pin tengah. Semua
 * digambar satu Canvas sehingga ringan dibanding animasi gambar/lottie. */
@Composable
private fun LocationScanAnimation(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "locationScan")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart),
        label = "locationScanPulse",
    )
    val sweep by transition.animateFloat(
        initialValue = -90f,
        targetValue = 270f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
        label = "locationScanSweep",
    )
    val blue = Color(0xFF38BDF8)
    Box(modifier = modifier.size(92.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = size.minDimension * 0.16f
            repeat(3) { index ->
                val phase = (progress + index / 3f) % 1f
                val radius = baseRadius + phase * size.minDimension * 0.34f
                drawCircle(
                    color = blue.copy(alpha = (1f - phase) * 0.28f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx()),
                )
            }
            drawCircle(blue.copy(alpha = 0.16f), radius = baseRadius * 1.8f, center = center)
            drawArc(
                color = blue.copy(alpha = 0.9f),
                startAngle = sweep,
                sweepAngle = 58f,
                useCenter = false,
                topLeft = Offset(center.x - baseRadius * 2.35f, center.y - baseRadius * 2.35f),
                size = Size(baseRadius * 4.7f, baseRadius * 4.7f),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
            )
        }
        Icon(
            Icons.Default.MyLocation,
            contentDescription = null,
            tint = blue,
            modifier = Modifier.size(30.dp),
        )
    }
}

