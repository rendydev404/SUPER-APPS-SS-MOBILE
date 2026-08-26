package com.sukashawarma.superapp.presentation.absensi.enroll

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.domain.model.Role
import com.sukashawarma.superapp.domain.session.AppSession

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
    val selectedCrew = state.crew.find { it.id == state.selectedStaffId }

    if (selectedCrew != null) {
        FullScreenEnrollCamera(
            crewName = selectedCrew.name,
            hasCameraPermission = hasCameraPermission,
            capturing = state.capturing,
            captureResult = state.captureResult,
            captureOk = state.captureOk,
            onCaptureReady = { imageCapture = it },
            onCapture = {
                imageCapture?.captureJpeg(executor) { result ->
                    result.onSuccess(viewModel::onPhotoCaptured)
                }
            },
            onBack = viewModel::closeCamera,
        )
        return
    }

    Scaffold(
        containerColor = Color(0xFFF3FCFC),
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
            val notEnrolled = state.crew.filterNot { it.alreadyEnrolled }
            val enrolled = state.crew.filter { it.alreadyEnrolled }
            var pendingExpanded by rememberSaveable { mutableStateOf(true) }
            var enrolledExpanded by rememberSaveable { mutableStateOf(false) }

            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
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
    capturing: Boolean,
    captureResult: String?,
    captureOk: Boolean,
    onCaptureReady: (androidx.camera.core.ImageCapture) -> Unit,
    onCapture: () -> Unit,
    onBack: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (hasCameraPermission) {
            EnrollCameraPreview(modifier = Modifier.fillMaxSize(), onCaptureReady = onCaptureReady)
        } else {
            Text("Izin kamera dibutuhkan.", color = Color.White, modifier = Modifier.align(Alignment.Center))
        }
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopStart).padding(top = 38.dp, start = 16.dp)
                .clip(CircleShape).background(Color.Black.copy(alpha = 0.48f)),
        ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali ke daftar crew", tint = Color.White) }
        Surface(
            color = Color.Black.copy(alpha = 0.72f),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Enrollment wajah: $crewName", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("Pastikan wajah terlihat jelas dan menghadap kamera.", color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp)
                if (captureResult != null) Text(captureResult, color = if (captureOk) Color(0xFF34D399) else Color(0xFFF87171), fontSize = 13.sp)
                Button(onClick = onCapture, enabled = !capturing, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    if (capturing) CircularProgressIndicator(Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary)
                    else { Icon(Icons.Default.Camera, null); Spacer(Modifier.width(8.dp)); Text("Ambil Foto") }
                }
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
    val selected = outlets.find { it.id == selectedId }
    Surface(shape = RoundedCornerShape(12.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5D7C6))) {
        Column(Modifier.padding(12.dp)) {
            Text("Outlet", color = Color(0xFF9A560C), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(5.dp))
            if (isRegionalManager) {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = if (loading) "Memuat outlet..." else selected?.name ?: "Pilih outlet",
                        onValueChange = {}, readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF9A560C),
                            unfocusedBorderColor = Color(0xFF9A560C),
                        ),
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        outlets.forEach { outlet ->
                            DropdownMenuItem(text = { Text(outlet.name) }, onClick = { onSelect(outlet.id); expanded = false })
                        }
                    }
                }
            } else {
                Text(outletName ?: "Outlet", color = Color(0xFF293235), fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(4.dp))
            }
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
