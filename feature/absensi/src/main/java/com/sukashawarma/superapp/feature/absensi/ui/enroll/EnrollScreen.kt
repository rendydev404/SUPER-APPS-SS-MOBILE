package com.sukashawarma.superapp.presentation.absensi.enroll

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.presentation.components.FaceCameraPreview
import com.sukashawarma.superapp.presentation.theme.StatusEmerald
import com.sukashawarma.superapp.presentation.theme.SukaOrange
import com.sukashawarma.superapp.core.ui.ios.BarisIos
import com.sukashawarma.superapp.core.ui.ios.BilahJudulIos
import com.sukashawarma.superapp.core.ui.ios.GrupIos
import com.sukashawarma.superapp.core.ui.ios.KartuIos
import com.sukashawarma.superapp.core.ui.ios.KeadaanIos
import com.sukashawarma.superapp.core.ui.ios.KolomCariIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.PemisahIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.TombolKeduaIos
import com.sukashawarma.superapp.core.ui.ios.TombolUtamaIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.ios.permukaanIos
import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.core.ui.SukaDropdownHeader
import com.sukashawarma.superapp.core.ui.SukaDropdownMenu
import com.sukashawarma.superapp.core.ui.SukaDropdownMenuItem
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.RealtimeTables
import com.sukashawarma.superapp.core.ui.kaca.LocalRuangNavKaca

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnrollScreen(onExit: () -> Unit) {
    val context = LocalContext.current
    val viewModel: EnrollViewModel = viewModel(
        factory = EnrollViewModelFactory(context.applicationContext as android.app.Application)
    )
    val state by viewModel.state.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }
    LaunchedEffect(Unit) { if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA) }

    var imageCapture by remember { mutableStateOf<androidx.camera.core.ImageCapture?>(null) }
    val executor = remember { ContextCompat.getMainExecutor(context) }
    // Diri sendiri tidak selalu ada di `crew` (outlet lain / tanpa outlet), jadi ikut
    // dicari di sini — tanpa ini kamera tidak pernah terbuka untuk kartu "Wajah Saya".
    val selectedCrew = (state.crew + listOfNotNull(state.self)).find { it.id == state.selectedStaffId }

    LaunchedEffect(state.selectedStaffId) { imageCapture = null }
    LaunchedEffect(state.captureRequestId) {
        if (state.captureRequestId == null) return@LaunchedEffect
        imageCapture?.captureJpeg(executor) { result ->
            result.fold(
                onSuccess = viewModel::onPhotoCaptured,
                onFailure = { viewModel.onCaptureFailed() },
            )
        } ?: viewModel.onCaptureFailed()
    }

    if (selectedCrew != null) {
        BackHandler(enabled = state.stage != EnrollStage.SAVING) { viewModel.closeCamera() }
        FullScreenEnrollCamera(
            crewName = selectedCrew.name,
            hasCameraPermission = hasCameraPermission,
            state = state,
            onCaptureReady = { imageCapture = it },
            onFrame = viewModel::onScanFrame,
            onRequestCameraPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            onRetake = viewModel::retakePhoto,
            onConfirm = viewModel::confirmEnrollment,
            onFinish = viewModel::finishEnrollment,
            onBack = viewModel::closeCamera,
        )
        return
    }

    // Satu wajah yang baru didaftarkan di HP lain memindahkan orangnya dari
    // "Belum Enroll" ke "Sudah Enroll". Tanpa ini, dua supervisor yang mendaftarkan
    // crew bersamaan akan saling memanggil orang yang sudah selesai.
    RealtimeRefresh(RealtimeTables.OUTLET_STAFF) { viewModel.segarkanCrew() }

    Scaffold(
        containerColor = WarnaIos.Latar,
        topBar = { BilahJudulIos(judul = "Enrollment Crew", onKembali = onExit) }
    ) { padding ->
        // Diri sendiri sudah punya kartunya sendiri di atas; dibiarkan ikut di daftar
        // crew hanya akan menampilkan orang yang sama dua kali.
        val others = state.crew.filterNot { it.id == state.self?.id }
        val notEnrolled = others.filterNot { it.alreadyEnrolled }
        val enrolled = others.filter { it.alreadyEnrolled }
        var pendingExpanded by rememberSaveable { mutableStateOf(true) }
        var enrolledExpanded by rememberSaveable { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                // Dua akordeon yang terbuka bersamaan, apalagi di outlet berkru banyak,
                // melampaui tinggi layar. Tanpa gulir, kartu terakhir hanya terpotong di
                // tepi bawah dan crew di dalamnya tidak bisa dijangkau sama sekali.
                // Gulir dipasang sebelum padding supaya jarak bawahnya ikut bergulir dan
                // baris terakhir tidak mepet ke bilah navigasi.
                .verticalScroll(rememberScrollState())
                .padding(horizontal = UkuranIos.TepiLayar, vertical = 16.dp)
                // Ruang kapsul tab kaca saat tampil sebagai tab pager; nol di rute sendiri.
                .padding(bottom = LocalRuangNavKaca.current),
            verticalArrangement = Arrangement.spacedBy(UkuranIos.JarakKartu),
        ) {
            state.self?.let { me ->
                SelfEnrollmentCard(self = me, onSelect = { viewModel.selectStaff(me.id) })
            }

            OutletSelectionCard(
                bisaPilihOutlet = state.canChooseOutlet,
                outletName = AppSession.staff.value?.outletName,
                outlets = state.outlets,
                loading = state.loadingOutlets,
                selectedId = state.selectedOutletId,
                onSelect = viewModel::selectOutlet,
            )

            if (state.error != null) {
                Text(state.error ?: "", style = TipeIos.Catatan.copy(color = NadaIos.BAHAYA.teks), modifier = Modifier.padding(horizontal = 4.dp))
            }

            if (state.loadingCrew && state.selectedOutletId != null) {
                Box(Modifier.fillMaxWidth().padding(vertical = 36.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = WarnaIos.Aksen, modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
                }
            } else if (state.selectedOutletId != null) {
                CrewEnrollmentSection(
                    title = "Belum Enroll",
                    subtitle = "${notEnrolled.size} Crew Members",
                    crew = notEnrolled,
                    expanded = pendingExpanded,
                    selectedId = state.selectedStaffId,
                    nada = NadaIos.BAHAYA,
                    onExpandedChange = { pendingExpanded = !pendingExpanded },
                    onSelect = viewModel::selectStaff,
                )
                CrewEnrollmentSection(
                    title = "Sudah Enroll",
                    subtitle = "${enrolled.size} Crew Members",
                    crew = enrolled,
                    expanded = enrolledExpanded,
                    selectedId = state.selectedStaffId,
                    nada = NadaIos.SUKSES,
                    onExpandedChange = { enrolledExpanded = !enrolledExpanded },
                    onSelect = viewModel::selectStaff,
                )
            } else {
                KeadaanIos(
                    ikon = IkonIos.Storefront,
                    judul = "Belum ada outlet",
                    pesan = "Pilih outlet untuk memuat crew.",
                )
            }
        }
    }
}

@Composable
private fun FullScreenEnrollCamera(
    crewName: String,
    hasCameraPermission: Boolean,
    state: EnrollUiState,
    onCaptureReady: (androidx.camera.core.ImageCapture) -> Unit,
    onFrame: (com.sukashawarma.superapp.data.face.FrameFaceResult) -> Unit,
    onRequestCameraPermission: () -> Unit,
    onRetake: () -> Unit,
    onConfirm: () -> Unit,
    onFinish: () -> Unit,
    onBack: () -> Unit,
) {
    val cameraActive = state.stage == EnrollStage.SCANNING || state.stage == EnrollStage.CAPTURING
    var lastFrameMs by remember { mutableStateOf(0L) }
    val previewBitmap = state.previewBitmap

    Box(Modifier.fillMaxSize().background(Color(0xFF071018))) {
        if (previewBitmap != null && state.stage >= EnrollStage.REVIEWING) {
            Image(
                bitmap = previewBitmap.asImageBitmap(),
                contentDescription = "Foto wajah $crewName yang akan didaftarkan",
                contentScale = ContentScale.Crop,
                // Kamera depan yang live ditampilkan seperti cermin (PreviewView), sedangkan
                // hasil jepretannya tidak — tanpa ini gambar "melompat" terbalik saat berpindah
                // dari kamera ke pratinjau (tangan kanan pindah ke kiri). Dibalik HANYA di
                // tampilan: foto yang diunggah & descriptor wajah tetap arah aslinya, jadi
                // pencocokan saat absen tidak terpengaruh.
                modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = -1f },
            )
        } else if (hasCameraPermission) {
            FaceCameraPreview(
                modifier = Modifier.fillMaxSize(),
                isActive = cameraActive,
                needsCrop = { false },
                onFrame = { frame ->
                    val now = System.currentTimeMillis()
                    if (now - lastFrameMs >= 100L) {
                        lastFrameMs = now
                        onFrame(frame)
                    }
                },
                onImageCaptureReady = onCaptureReady,
            )
        }

        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.64f),
                    0.26f to Color.Transparent,
                    0.66f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.82f),
                ),
            ),
        )

        if (hasCameraPermission && state.stage == EnrollStage.SCANNING) {
            EnrollmentScannerOverlay(
                progress = state.scanProgress,
                faceAccepted = state.scanHint == EnrollScanHint.HOLD_STILL,
                modifier = Modifier.fillMaxSize(),
            )
        }

        IconButton(
            onClick = onBack,
            enabled = state.stage != EnrollStage.SAVING,
            modifier = Modifier.align(Alignment.TopStart).padding(top = 38.dp, start = 16.dp)
                .clip(CircleShape).background(Color.Black.copy(alpha = 0.48f)),
        ) { Icon(IkonIos.ArrowBack, "Kembali ke daftar crew", tint = Color.White, modifier = Modifier.size(20.dp)) }

        Column(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 42.dp, start = 68.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Enrollment Wajah", color = WarnaIos.Aksen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(crewName, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }

        // Sebagai tab pager, kamera tampil di dalam shell kaca dan kapsul tab mengambang
        // di atas dasar layar — kartu aksi naik di atasnya supaya tombolnya tetap bisa
        // disentuh. Di rute ENROLL yang berdiri sendiri ruangnya nol.
        val dasar = Modifier.align(Alignment.BottomCenter).padding(bottom = LocalRuangNavKaca.current)
        if (!hasCameraPermission) {
            CameraPermissionCard(
                onRequest = onRequestCameraPermission,
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
            )
        } else when (state.stage) {
            EnrollStage.SCANNING -> ScanGuidanceCard(
                hint = state.scanHint,
                progress = state.scanProgress,
                error = state.captureResult,
                modifier = dasar,
            )
            EnrollStage.CAPTURING, EnrollStage.VERIFYING -> ProcessingEnrollmentCard(
                text = if (state.stage == EnrollStage.CAPTURING) "Mengambil foto otomatis…" else "Memeriksa kejernihan wajah…",
                modifier = dasar,
            )
            EnrollStage.REVIEWING -> EnrollmentReviewCard(
                crewName = crewName,
                error = state.captureResult,
                onRetake = onRetake,
                onConfirm = onConfirm,
                modifier = dasar,
            )
            EnrollStage.SAVING -> ProcessingEnrollmentCard(
                text = "Mendaftarkan wajah secara aman…",
                modifier = dasar,
            )
            EnrollStage.SUCCESS -> EnrollmentSuccessCard(
                crewName = crewName,
                onFinish = onFinish,
                modifier = dasar,
            )
        }
    }
}

@Composable
private fun EnrollmentScannerOverlay(
    progress: Float,
    faceAccepted: Boolean,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "enrollScanner")
    val sweep by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1_900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scannerSweep",
    )
    val pulse by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "scannerPulse",
    )
    val accent = if (faceAccepted) StatusEmerald else Color(0xFF38BDF8)

    Canvas(modifier) {
        val ovalWidth = size.width * 0.72f
        val ovalHeight = size.height * 0.46f
        val left = (size.width - ovalWidth) / 2f
        val top = size.height * 0.19f
        val ovalSize = Size(ovalWidth, ovalHeight)

        drawOval(
            color = Color.White.copy(alpha = 0.18f),
            topLeft = Offset(left, top),
            size = ovalSize,
            style = Stroke(width = 2.dp.toPx()),
        )
        drawArc(
            color = accent.copy(alpha = 0.95f),
            startAngle = -90f,
            sweepAngle = 360f * progress.coerceIn(0f, 1f),
            useCenter = false,
            topLeft = Offset(left, top),
            size = ovalSize,
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
        )

        val y = top + ovalHeight * (sweep + 1f) / 2f
        val halfAtY = ovalWidth / 2f * kotlin.math.sqrt((1f - sweep * sweep).coerceAtLeast(0f))
        val centerX = size.width / 2f
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, accent.copy(alpha = 0.75f * pulse), Color.Transparent),
                startX = centerX - halfAtY,
                endX = centerX + halfAtY,
            ),
            start = Offset(centerX - halfAtY, y),
            end = Offset(centerX + halfAtY, y),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )

        val cornerLength = 24.dp.toPx()
        val cornerStroke = 4.dp.toPx()
        val cornerColor = accent.copy(alpha = 0.65f + 0.35f * pulse)
        listOf(
            Offset(left, top) to Offset(1f, 1f),
            Offset(left + ovalWidth, top) to Offset(-1f, 1f),
            Offset(left, top + ovalHeight) to Offset(1f, -1f),
            Offset(left + ovalWidth, top + ovalHeight) to Offset(-1f, -1f),
        ).forEach { (origin, direction) ->
            drawLine(cornerColor, origin, Offset(origin.x + direction.x * cornerLength, origin.y), cornerStroke, StrokeCap.Round)
            drawLine(cornerColor, origin, Offset(origin.x, origin.y + direction.y * cornerLength), cornerStroke, StrokeCap.Round)
        }
    }
}

@Composable
private fun ScanGuidanceCard(
    hint: EnrollScanHint,
    progress: Float,
    error: String?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xE6101720),
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AnimatedContent(targetState = hint, label = "scanHint") { currentHint ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(
                        if (currentHint == EnrollScanHint.HOLD_STILL) Icons.Default.CheckCircle else Icons.Default.Face,
                        contentDescription = null,
                        tint = if (currentHint == EnrollScanHint.HOLD_STILL) StatusEmerald else Color(0xFF38BDF8),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(currentHint.message, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                color = StatusEmerald,
                trackColor = Color.White.copy(alpha = 0.14f),
            )
            Text("Tidak perlu menekan tombol — foto diambil otomatis saat wajah siap.", color = Color.White.copy(alpha = 0.66f), fontSize = 12.sp)
            if (error != null) Text(error, color = Color(0xFFFCA5A5), fontSize = 12.sp)
        }
    }
}

@Composable
private fun ProcessingEnrollmentCard(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xEE101720),
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator(Modifier.size(24.dp), color = SukaOrange, strokeWidth = 2.5.dp)
            Spacer(Modifier.width(14.dp))
            Text(text, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
    }
}

/** Lembar putih iOS di dasar layar kamera — sudut atas membulat, bayangan lembut. */
private val BentukLembarBawah = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

@Composable
private fun EnrollmentReviewCard(
    crewName: String,
    error: String?,
    onRetake: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .permukaanIos(BentukLembarBawah)
            .padding(horizontal = 20.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(WarnaIos.Aksen.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.FaceRetouchingNatural, contentDescription = null, tint = WarnaIos.Aksen)
        }
        Text("Konfirmasi wajah", style = TipeIos.Catatan.copy(fontWeight = FontWeight.SemiBold))
        Text("Apakah ini wajah $crewName?", style = TipeIos.Judul3, textAlign = TextAlign.Center)
        Text("Pastikan foto jelas dan sesuai dengan crew yang dipilih.", style = TipeIos.Catatan, textAlign = TextAlign.Center)
        if (error != null) Text(error, style = TipeIos.Catatan.copy(color = NadaIos.BAHAYA.teks), textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TombolKeduaIos(
                teks = "Scan ulang",
                onKlik = onRetake,
                ikon = IkonIos.Refresh,
                modifier = Modifier.weight(1f),
            )
            TombolUtamaIos(
                teks = "Ya, daftarkan",
                onKlik = onConfirm,
                ikon = Icons.Default.HowToReg,
                modifier = Modifier.weight(1.35f),
            )
        }
    }
}

@Composable
private fun EnrollmentSuccessCard(crewName: String, onFinish: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .permukaanIos(BentukLembarBawah)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(60.dp).clip(CircleShape).background(WarnaIos.Hijau.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Verified, contentDescription = null, tint = WarnaIos.Hijau, modifier = Modifier.size(32.dp))
        }
        Text("Enrollment berhasil", style = TipeIos.Judul3)
        Text("Wajah $crewName sudah siap digunakan untuk absensi.", style = TipeIos.Catatan, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        TombolUtamaIos(teks = "Selesai", onKlik = onFinish, warna = WarnaIos.Hijau)
    }
}

@Composable
private fun CameraPermissionCard(onRequest: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .permukaanIos(UkuranIos.SudutKartu)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(60.dp).clip(CircleShape).background(WarnaIos.Aksen.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.NoPhotography, contentDescription = null, tint = WarnaIos.Aksen, modifier = Modifier.size(30.dp))
        }
        Text("Kamera diperlukan", style = TipeIos.Utama)
        Text("Izinkan kamera untuk memindai wajah crew secara otomatis.", style = TipeIos.Catatan, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        TombolUtamaIos(teks = "Izinkan kamera", onKlik = onRequest)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OutletSelectionCard(
    bisaPilihOutlet: Boolean,
    outletName: String?,
    outlets: List<EnrollOutletOption>,
    loading: Boolean,
    selectedId: String?,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val selected = outlets.find { it.id == selectedId }
    val filteredOutlets = outlets.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }

    LaunchedEffect(expanded) {
        if (!expanded) searchQuery = ""
    }

    GrupIos(judul = "Outlet") {
            if (bisaPilihOutlet) {
                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !loading && outlets.isNotEmpty()) { expanded = true }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(Modifier.size(30.dp).clip(CircleShape).background(WarnaIos.Aksen.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                            Icon(IkonIos.Storefront, contentDescription = null, tint = WarnaIos.Aksen, modifier = Modifier.size(17.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (loading) "Memuat outlet..." else selected?.name ?: "Pilih outlet",
                                style = TipeIos.Isi.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 1,
                            )
                            Text("Ketuk untuk memilih outlet", style = TipeIos.Catatan, maxLines = 1)
                        }
                        Icon(
                            if (expanded) IkonIos.ExpandLess else IkonIos.ExpandMore,
                            contentDescription = "Pilih outlet",
                            tint = WarnaIos.LabelKetiga,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    SukaDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.width(300.dp).heightIn(max = 440.dp),
                    ) {
                        SukaDropdownHeader(
                            title = "PILIH OUTLET",
                            onClose = { expanded = false },
                        )
                        Column(Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
                            KolomCariIos(
                                nilai = searchQuery,
                                onUbah = { searchQuery = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = "Cari outlet...",
                            )
                            Spacer(Modifier.height(6.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 330.dp)
                                    .verticalScroll(rememberScrollState()),
                            ) {
                                if (filteredOutlets.isEmpty()) {
                                    Text(
                                        "Outlet tidak ditemukan",
                                        style = TipeIos.Catatan,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 18.dp),
                                    )
                                } else filteredOutlets.forEach { outlet ->
                                    SukaDropdownMenuItem(
                                        title = outlet.name,
                                        selected = outlet.id == selected?.id,
                                        leadingIcon = IkonIos.Storefront,
                                        onClick = {
                                            onSelect(outlet.id)
                                            expanded = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                BarisIos(judul = outletName ?: "Outlet", ikon = IkonIos.Storefront)
            }
    }
}

/** Kartu enrollment diri sendiri. Sengaja berdiri di paling atas dan di LUAR seksi crew:
 *  ketersediaannya tidak boleh bergantung pada outlet yang sedang dipilih, karena SPV,
 *  Regional Manager, dan staff pusat sering tidak terdaftar di outlet yang mereka buka. */
@Composable
private fun SelfEnrollmentCard(self: EnrollCrewOption, onSelect: () -> Unit) {
    KartuIos(onKlik = onSelect, padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(WarnaIos.Aksen.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Face, null, tint = WarnaIos.Aksen, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Wajah Saya", style = TipeIos.Utama)
                Text(
                    "${self.name} · ${if (self.alreadyEnrolled) "Sudah terdaftar, ketuk untuk perbarui" else "Belum terdaftar"}",
                    style = TipeIos.Catatan.copy(color = if (self.alreadyEnrolled) WarnaIos.LabelKedua else NadaIos.BAHAYA.teks),
                )
            }
            Icon(IkonIos.ChevronRight, null, tint = WarnaIos.LabelKetiga, modifier = Modifier.size(15.dp))
        }
    }
}

@Composable
private fun CrewEnrollmentSection(
    title: String,
    subtitle: String,
    crew: List<EnrollCrewOption>,
    expanded: Boolean,
    selectedId: String?,
    nada: NadaIos,
    onExpandedChange: () -> Unit,
    onSelect: (String?) -> Unit,
) {
    Column(Modifier.fillMaxWidth().permukaanIos(UkuranIos.SudutGrup)) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onExpandedChange).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(30.dp).clip(CircleShape).background(nada.warna.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                Icon(if (title == "Belum Enroll") Icons.Default.PersonAdd else Icons.Default.Verified, null, tint = nada.warna, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = TipeIos.Utama)
                Text(subtitle, style = TipeIos.Catatan)
            }
            Icon(if (expanded) IkonIos.ExpandLess else IkonIos.ExpandMore, null, tint = WarnaIos.LabelKetiga, modifier = Modifier.size(16.dp))
        }
        if (expanded) {
            PemisahIos()
            if (crew.isEmpty()) {
                Text("Tidak ada crew.", style = TipeIos.Catatan, modifier = Modifier.padding(16.dp))
            } else crew.forEachIndexed { i, member ->
                if (i > 0) PemisahIos(inset = 62.dp)
                CrewMemberRow(member, member.id == selectedId, nada, onSelect)
            }
        }
    }
}

@Composable
private fun CrewMemberRow(member: EnrollCrewOption, selected: Boolean, nada: NadaIos, onSelect: (String?) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(if (selected) nada.warna.copy(alpha = 0.07f) else Color.Transparent)
            .clickable { onSelect(member.id) }.padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val initials = member.name.trim().split(Regex("\\s+")).mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("")
        Box(Modifier.size(34.dp).clip(CircleShape).background(WarnaIos.Isian), contentAlignment = Alignment.Center) {
            Text(initials.ifBlank { "-" }, color = WarnaIos.AbuGelap, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(member.name, style = TipeIos.Isi.copy(fontSize = 16.sp))
            Text("ID: ${member.id.take(8)} · ${if (member.alreadyEnrolled) "Terdaftar" else "Belum terdaftar"}", style = TipeIos.Kecil)
        }
        Icon(IkonIos.ChevronRight, null, tint = WarnaIos.LabelKetiga, modifier = Modifier.size(15.dp))
    }
}
