package com.sukashawarma.superapp.presentation.absensi.pengaturan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PengaturanScreen(onExit: () -> Unit, viewModel: PengaturanViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()

    var jamMasuk by remember(state.loading) { mutableStateOf(state.jamMasuk) }
    var jamKeluar by remember(state.loading) { mutableStateOf(state.jamKeluar) }
    var toleransi by remember(state.loading) { mutableStateOf(state.toleransiMenit.toString()) }
    var radius by remember(state.loading) { mutableStateOf(state.radiusM.toString()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan Absensi") },
                navigationIcon = {
                    IconButton(onClick = onExit) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali") }
                }
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.loadError != null -> Text(
                    state.loadError ?: "",
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    color = MaterialTheme.colorScheme.error,
                )
                else -> Column(Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Jam Kerja", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = jamMasuk, onValueChange = { jamMasuk = it },
                            label = { Text("Jam masuk (HH:mm)") }, modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = jamKeluar, onValueChange = { jamKeluar = it },
                            label = { Text("Jam keluar (HH:mm)") }, modifier = Modifier.weight(1f),
                        )
                    }
                    OutlinedTextField(
                        value = toleransi, onValueChange = { toleransi = it.filter { c -> c.isDigit() } },
                        label = { Text("Toleransi keterlambatan (menit)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = radius, onValueChange = { radius = it.filter { c -> c.isDigit() } },
                        label = { Text("Radius geofence (meter)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (state.saveError != null) Text(state.saveError ?: "", color = MaterialTheme.colorScheme.error)
                    if (state.saved) Text("Pengaturan tersimpan.", color = Color(0xFF059669))

                    Button(
                        onClick = {
                            viewModel.update(
                                jamMasuk.trim(), jamKeluar.trim(),
                                toleransi.toIntOrNull() ?: 0, radius.toIntOrNull() ?: 0,
                            )
                        },
                        enabled = !state.saving,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (state.saving) CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                        else Text("Simpan Pengaturan")
                    }
                }
            }
        }
    }
}
