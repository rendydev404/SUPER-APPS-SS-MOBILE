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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.domain.model.Role
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.presentation.components.FaceCameraPreview
import com.sukashawarma.superapp.presentation.theme.StatusEmerald
import com.sukashawarma.superapp.presentation.theme.StatusRed
import com.sukashawarma.superapp.presentation.theme.SukaOnSurface
import com.sukashawarma.superapp.presentation.theme.SukaOnSurfaceVariant
import com.sukashawarma.superapp.presentation.theme.SukaOrange
import com.sukashawarma.superapp.presentation.theme.SukaSurface
import com.sukashawarma.superapp.presentation.theme.SukaSurfaceContainerHighest
import com.sukashawarma.superapp.presentation.theme.SukaSurfaceContainerLowest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnrollScreen(onExit: () -> Unit) {
    val context = LocalContext.current
    val viewModel: EnrollViewModel = viewModel(
        factory = EnrollViewModelFactory(context.applicationContext as android.app.Application)
    )
    val state by viewModel.state.collectAsState()
    val isRegionalManager = AppSession.staff.value?.role == Role.REGIONAL_MANAGER

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

    Scaffold(
        containerColor = SukaSurface,
        topBar = {
            TopAppBar(
                title = { Text("Enrollment Crew", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onExit) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            // Diri sendiri sudah punya kartunya sendiri di atas; dibiarkan ikut di daftar
            // crew hanya akan menampilkan orang yang sama dua kali.
            val others = state.crew.filterNot { it.id == state.self?.id }
            val notEnrolled = others.filterNot { it.alreadyEnrolled }
            val enrolled = others.filter { it.alreadyEnrolled }
            var pendingExpanded by rememberSaveable { mutableStateOf(true) }
            var enrolledExpanded by rememberSaveable { mutableStateOf(false) }

            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                state.self?.let { me ->
                    SelfEnrollmentCard(self = me, onSelect = { viewModel.selectStaff(me.id) })
                }

                OutletSelectionCard(
                    isRegionalManager = isRegionalManager,
                    outletName = AppSession.staff.value?.outletName,
                    outlets = state.outlets,
                    loading = state.loadingOutlets,
                    selectedId = state.selectedOutletId,
                    onSelect = viewModel::selectOutlet,
                )

                if (state.error != null) {
                    Text(state.error ?: "", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }

                if (state.loadingCrew && state.selectedOutletId != null) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 36.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF9A560C), modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                    }
                } else if (state.selectedOutletId != null) {
                    CrewEnrollmentSection(
                        title = "Belum Enroll",
                        subtitle = "${notEnrolled.size} Crew Members",
                        crew = notEnrolled,
                        expanded = pendingExpanded,
                        selectedId = state.selectedStaffId,
                        accent = Color(0xFFD65B5B),
                        onExpandedChange = { pendingExpanded = !pendingExpanded },
                        onSelect = viewModel::selectStaff,
                    )
                    CrewEnrollmentSection(
                        title = "Sudah Enroll",
                        subtitle = "${enrolled.size} Crew Members",
                        crew = enrolled,
                        expanded = enrolledExpanded,
                        selectedId = state.selectedStaffId,
                        accent = Color(0xFF6D9FA2),
                        onExpandedChange = { enrolledExpanded = !enrolledExpanded },
                        onSelect = viewModel::selectStaff,
                    )
                } else {
                    Text("Pilih outlet untuk memuat crew.", color = Color(0xFF68757A), fontSize = 14.sp)
                }
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
                modifier = Modifier.fillMaxSize(),
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
        ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali ke daftar crew", tint = Color.White) }

        Column(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 42.dp, start = 68.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("ENROLLMENT WAJAH", color = SukaOrange, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Text(crewName, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }

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
                modifier = Modifier.align(Alignment.BottomCenter),
            )
            EnrollStage.CAPTURING, EnrollStage.VERIFYING -> ProcessingEnrollmentCard(
                text = if (state.stage == EnrollStage.CAPTURING) "Mengambil foto otomatis…" else "Memeriksa kejernihan wajah…",
                modifier = Modifier.align(Alignment.BottomCenter),
            )
            EnrollStage.REVIEWING -> EnrollmentReviewCard(
                crewName = crewName,
                error = state.captureResult,
                onRetake = onRetake,
                onConfirm = onConfirm,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
            EnrollStage.SAVING -> ProcessingEnrollmentCard(
                text = "Mendaftarkan wajah secara aman…",
                modifier = Modifier.align(Alignment.BottomCenter),
            )
            EnrollStage.SUCCESS -> EnrollmentSuccessCard(
                crewName = crewName,
                onFinish = onFinish,
                modifier = Modifier.align(Alignment.BottomCenter),
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

@Composable
private fun EnrollmentReviewCard(
    crewName: String,
    error: String?,
    onRetake: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        shadowElevation = 18.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(SukaOrange.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.FaceRetouchingNatural, contentDescription = null, tint = SukaOrange)
            }
            Text("Konfirmasi wajah", color = SukaOnSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text("Apakah ini wajah $crewName?", color = SukaOnSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Pastikan foto jelas dan sesuai dengan crew yang dipilih.", color = Color(0xFF64748B), fontSize = 12.sp)
            if (error != null) Text(error, color = StatusRed, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onRetake,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(15.dp),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("Scan ulang")
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1.35f).height(52.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SukaOrange, contentColor = Color.White),
                ) {
                    Icon(Icons.Default.HowToReg, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("Ya, daftarkan", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun EnrollmentSuccessCard(crewName: String, onFinish: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(Modifier.size(58.dp).clip(CircleShape).background(StatusEmerald.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Verified, contentDescription = null, tint = StatusEmerald, modifier = Modifier.size(32.dp))
            }
            Text("Enrollment berhasil", color = SukaOnSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Wajah $crewName sudah siap digunakan untuk absensi.", color = Color(0xFF64748B), fontSize = 13.sp)
            Button(
                onClick = onFinish,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StatusEmerald),
            ) { Text("Selesai", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun CameraPermissionCard(onRequest: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(24.dp), color = Color(0xEEFFFFFF)) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Default.NoPhotography, contentDescription = null, tint = SukaOrange, modifier = Modifier.size(38.dp))
            Text("Kamera diperlukan", color = SukaOnSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("Izinkan kamera untuk memindai wajah crew secara otomatis.", color = Color(0xFF64748B), fontSize = 13.sp)
            Button(onClick = onRequest, colors = ButtonDefaults.buttonColors(containerColor = SukaOrange)) {
                Text("Izinkan kamera")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OutletSelectionCard(
    isRegionalManager: Boolean,
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

    Surface(shape = RoundedCornerShape(12.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5D7C6))) {
        Column(Modifier.padding(12.dp)) {
            Text("Outlet", color = Color(0xFF9A560C), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(5.dp))
            if (isRegionalManager) {
                Box {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !loading && outlets.isNotEmpty()) { expanded = true },
                        shape = RoundedCornerShape(12.dp),
                        color = SukaSurfaceContainerLowest,
                        border = BorderStroke(1.dp, SukaSurfaceContainerHighest),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(Icons.Default.Storefront, contentDescription = null, tint = SukaOrange, modifier = Modifier.size(21.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    if (loading) "Memuat outlet..." else selected?.name ?: "Pilih outlet",
                                    color = SukaOnSurface,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                )
                                Text("Ketuk untuk memilih outlet", color = SukaOnSurfaceVariant, fontSize = 11.sp, maxLines = 1)
                            }
                            Icon(
                                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Pilih outlet",
                                tint = SukaOnSurfaceVariant,
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.width(280.dp).heightIn(max = 430.dp),
                    ) {
                        Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                placeholder = { Text("Cari outlet", fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Hapus pencarian")
                                        }
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SukaOrange,
                                    unfocusedBorderColor = SukaSurfaceContainerHighest,
                                    focusedContainerColor = SukaSurfaceContainerLowest,
                                    unfocusedContainerColor = SukaSurfaceContainerLowest,
                                ),
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
                                        color = SukaOnSurfaceVariant,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 18.dp),
                                    )
                                } else filteredOutlets.forEach { outlet ->
                                    DropdownMenuItem(
                                        text = { Text(outlet.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) },
                                        leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                        onClick = { onSelect(outlet.id); expanded = false },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Text(outletName ?: "Outlet", color = Color(0xFF293235), fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(4.dp))
            }
        }
    }
}

/** Kartu enrollment diri sendiri. Sengaja berdiri di paling atas dan di LUAR seksi crew:
 *  ketersediaannya tidak boleh bergantung pada outlet yang sedang dipilih, karena SPV,
 *  Regional Manager, dan staff pusat sering tidak terdaftar di outlet yang mereka buka. */
@Composable
private fun SelfEnrollmentCard(self: EnrollCrewOption, onSelect: () -> Unit) {
    val accent = Color(0xFF9A560C)
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(34.dp).clip(CircleShape).background(accent.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Face, null, tint = accent, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Wajah Saya", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D282B))
                Text(
                    "${self.name} · ${if (self.alreadyEnrolled) "Sudah terdaftar, ketuk untuk perbarui" else "Belum terdaftar"}",
                    fontSize = 11.sp,
                    color = if (self.alreadyEnrolled) Color(0xFF718084) else Color(0xFFD65B5B),
                )
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = accent, modifier = Modifier.size(18.dp))
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
    accent: Color,
    onExpandedChange: () -> Unit,
    onSelect: (String?) -> Unit,
) {
    Surface(shape = RoundedCornerShape(12.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD7E0E0))) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onExpandedChange).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(30.dp).clip(CircleShape).background(accent.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                    Icon(if (title == "Belum Enroll") Icons.Default.PersonAdd else Icons.Default.Verified, null, tint = accent, modifier = Modifier.size(17.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D282B))
                    Text(subtitle, fontSize = 11.sp, color = Color(0xFF718084))
                }
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = Color(0xFF66777A))
            }
            if (expanded) {
                HorizontalDivider(color = Color(0xFFE0E8E8))
                if (crew.isEmpty()) {
                    Text("Tidak ada crew.", color = Color(0xFF718084), fontSize = 13.sp, modifier = Modifier.padding(16.dp))
                } else crew.forEach { member ->
                    CrewMemberRow(member, member.id == selectedId, accent, onSelect)
                }
            }
        }
    }
}

@Composable
private fun CrewMemberRow(member: EnrollCrewOption, selected: Boolean, accent: Color, onSelect: (String?) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(if (selected) accent.copy(alpha = 0.07f) else Color.Transparent)
            .clickable { onSelect(member.id) }.padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val initials = member.name.trim().split(Regex("\\s+")).mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("")
        Box(Modifier.size(34.dp).clip(CircleShape).background(Color(0xFFE8F1F1)), contentAlignment = Alignment.Center) {
            Text(initials.ifBlank { "-" }, color = Color(0xFF557073), fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(member.name, color = Color(0xFF273236), fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text("ID: ${member.id.take(8)} · ${if (member.alreadyEnrolled) "Terdaftar" else "Belum terdaftar"}", color = Color(0xFF718084), fontSize = 10.sp)
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color(0xFF758588), modifier = Modifier.size(18.dp))
    }
}
