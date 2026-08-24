package com.sukashawarma.superapp.presentation.absensi.enroll

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Enrollment Crew") },
                navigationIcon = {
                    IconButton(onClick = onExit) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Box(Modifier.padding(16.dp)) {
                CrewDropdown(
                    crew = state.crew,
                    loading = state.loadingCrew,
                    selectedId = state.selectedStaffId,
                    onSelect = { viewModel.selectStaff(it) },
                )
            }

            if (state.error != null) {
                Text(state.error ?: "", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            }

            if (state.selectedStaffId != null) {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    if (hasCameraPermission) {
                        EnrollCameraPreview(modifier = Modifier.fillMaxSize()) { imageCapture = it }
                    } else {
                        Text("Izin kamera dibutuhkan.", modifier = Modifier.align(Alignment.Center))
                    }

                    Box(
                        Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(alpha = 0.55f)).padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (state.captureResult != null) {
                                Text(
                                    state.captureResult ?: "",
                                    color = if (state.captureOk) Color(0xFF34D399) else Color(0xFFF87171),
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                            Button(
                                onClick = {
                                    val cap = imageCapture ?: return@Button
                                    cap.captureJpeg(executor) { result ->
                                        result.onSuccess { bytes -> viewModel.onPhotoCaptured(bytes) }
                                    }
                                },
                                enabled = !state.capturing && imageCapture != null,
                            ) {
                                if (state.capturing) {
                                    CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                                } else {
                                    Icon(Icons.Filled.Camera, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Ambil Foto")
                                }
                            }
                        }
                    }
                }
            } else {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Pilih crew yang akan didaftarkan.")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CrewDropdown(
    crew: List<EnrollCrewOption>,
    loading: Boolean,
    selectedId: String?,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = crew.find { it.id == selectedId }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = if (loading) "Memuat..." else selected?.name ?: "Pilih crew",
            onValueChange = {},
            readOnly = true,
            label = { Text("Crew") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            crew.forEach { c ->
                DropdownMenuItem(
                    text = { Text(c.name + if (c.alreadyEnrolled) " (sudah terdaftar)" else "") },
                    onClick = { onSelect(c.id); expanded = false },
                )
            }
        }
    }
}
