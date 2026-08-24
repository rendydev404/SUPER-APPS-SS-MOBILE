package com.sukashawarma.superapp.presentation.absensi.kasbon

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.presentation.absensi.AbsensiBottomNav
import com.sukashawarma.superapp.presentation.theme.StatusAmber
import com.sukashawarma.superapp.presentation.theme.StatusEmerald
import com.sukashawarma.superapp.presentation.theme.StatusRed
import com.sukashawarma.superapp.presentation.theme.SukaOnSurface
import com.sukashawarma.superapp.presentation.theme.SukaOnSurfaceVariant
import com.sukashawarma.superapp.presentation.theme.SukaPrimary
import com.sukashawarma.superapp.presentation.theme.SukaSurface
import com.sukashawarma.superapp.presentation.theme.SukaSurfaceContainer
import com.sukashawarma.superapp.presentation.theme.SukaSurfaceContainerLowest
import java.text.NumberFormat
import java.util.Locale

private val rupiahFmt = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 }
private fun fmtRupiah(v: Double) = rupiahFmt.format(v)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KasbonScreen(
    onExit: () -> Unit,
    onNavigateTab: (Int) -> Unit = {},
    viewModel: KasbonViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    var showForm by remember { mutableStateOf(false) }

    if (showForm) {
        var wasSubmitting by remember { mutableStateOf(false) }
        LaunchedEffect(state.submitting) {
            if (wasSubmitting && !state.submitting && state.submitError == null) showForm = false
            wasSubmitting = state.submitting
        }
        // Halaman penuh "Request Kasbon" mengikuti desain Stitch (project
        // 16991912726833518585, screen 057dfb051d6248b0a59696210f240291) — bukan dialog.
        KasbonFormScreen(
            submitting = state.submitting,
            error = state.submitError,
            onBack = { showForm = false; viewModel.clearSubmitError() },
            onSubmit = { amount, months, reason -> viewModel.submit(amount, months, reason) },
        )
    } else {
        KasbonListScreen(
            state = state,
            onExit = onExit,
            onNavigateTab = onNavigateTab,
            onAddClick = { showForm = true },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KasbonListScreen(
    state: KasbonUiState,
    onExit: () -> Unit,
    onNavigateTab: (Int) -> Unit,
    onAddClick: () -> Unit,
) {
    Scaffold(
        containerColor = SukaSurface,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                title = { Text("Kasbon", fontWeight = FontWeight.Bold, color = SukaOnSurface) },
                navigationIcon = {
                    IconButton(onClick = onExit) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali") }
                }
            )
        },
        // Sama seperti Cuti & Izin — Kasbon diakses dari tab "More" (index 3), bottom nav
        // tetap tampil di sini supaya user bisa lompat tab tanpa balik dulu.
        bottomBar = { AbsensiBottomNav(selectedIndex = 3, onSelect = onNavigateTab) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().background(SukaSurface)) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(SukaSurfaceContainer), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = SukaPrimary)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("Kasbon", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = SukaOnSurface)
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = onAddClick,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SukaPrimary)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Ajukan Kasbon", fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(24.dp))
                Text("Riwayat Kasbon", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = SukaOnSurface)
                Spacer(Modifier.height(16.dp))
            }

            Box(Modifier.fillMaxSize()) {
                when {
                    state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    state.error != null -> Text(
                        state.error ?: "",
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        color = MaterialTheme.colorScheme.error,
                    )
                    state.rows.isEmpty() -> Text("Belum ada pengajuan kasbon.", modifier = Modifier.align(Alignment.Center), color = SukaOnSurfaceVariant)
                    else -> LazyColumn(contentPadding = PaddingValues(bottom = 24.dp, start = 16.dp, end = 16.dp)) {
                        items(state.rows, key = { it.id }) { row -> KasbonItem(row) }
                    }
                }
            }
        }
    }
}

@Composable
private fun KasbonItem(row: KasbonRow) {
    val (color, label) = when (row.status) {
        "approved" -> StatusEmerald to "Disetujui"
        "rejected" -> StatusRed to "Ditolak"
        else -> StatusAmber to "Menunggu"
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    ) {
        Row(
            Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(Modifier.weight(1f)) {
                Text(fmtRupiah(row.amount), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SukaOnSurface)
                Spacer(Modifier.height(4.dp))
                Text("Sisa ${fmtRupiah(row.remaining)} · ${row.installmentMonths} bulan gaji", color = SukaOnSurfaceVariant, fontSize = 13.sp)
                if (row.reason.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(row.reason, color = SukaOnSurfaceVariant, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.width(12.dp))
            Box(Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private val KASBON_TENORS = listOf(1 to "1 bulan gaji", 2 to "2 bulan gaji", 3 to "3 bulan gaji")

/**
 * Halaman "Request Kasbon" — mengikuti desain Stitch persis: top bar judul di tengah warna
 * primary + tombol back, field jumlah dengan prefix "Rp", pill tenor 3 pilihan, textarea
 * alasan, tombol submit pill penuh melekat di bawah.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KasbonFormScreen(
    submitting: Boolean,
    error: String?,
    onBack: () -> Unit,
    onSubmit: (Double, Int, String) -> Unit,
) {
    var amountText by remember { mutableStateOf("") }
    var months by remember { mutableStateOf(1) }
    var reason by remember { mutableStateOf("") }

    Scaffold(
        containerColor = SukaSurface,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SukaSurface),
                title = {
                    Text(
                        "Request Kasbon",
                        color = SukaPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = SukaPrimary)
                    }
                },
            )
        },
        bottomBar = {
            Surface(color = SukaSurface, shadowElevation = 0.dp) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Button(
                        onClick = { amountText.toDoubleOrNull()?.let { onSubmit(it, months, reason) } },
                        enabled = !submitting && amountText.toDoubleOrNull() != null && amountText.toDoubleOrNull()!! > 0,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = SukaPrimary),
                    ) {
                        Text(if (submitting) "Mengirim..." else "Ajukan Kasbon", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .background(SukaSurface)
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            // Jumlah
            Text("Jumlah (Rp)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = SukaOnSurface)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SukaSurfaceContainerLowest)
                    .border(1.dp, SukaOnSurfaceVariant.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Rp", color = SukaOnSurfaceVariant, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.width(8.dp))
                BasicAmountField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() } },
                )
            }

            Spacer(Modifier.height(24.dp))

            // Tenor
            Text("Pilih Tenor", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = SukaOnSurface)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KASBON_TENORS.forEach { (m, label) ->
                    TenorPill(
                        label = label,
                        selected = months == m,
                        onClick = { months = m },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Alasan
            Text("Alasan Pengajuan", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = SukaOnSurface)
            Spacer(Modifier.height(8.dp))
            TextField(
                value = reason,
                onValueChange = { reason = it },
                placeholder = { Text("Tulis alasan Anda di sini...", color = SukaOnSurfaceVariant.copy(alpha = 0.6f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp)
                    .border(1.dp, SukaOnSurfaceVariant.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = SukaSurfaceContainerLowest,
                    unfocusedContainerColor = SukaSurfaceContainerLowest,
                    disabledContainerColor = SukaSurfaceContainerLowest,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    cursorColor = SukaPrimary,
                    focusedTextColor = SukaOnSurface,
                    unfocusedTextColor = SukaOnSurface,
                ),
            )

            if (error != null) {
                Spacer(Modifier.height(12.dp))
                Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun BasicAmountField(value: String, onValueChange: (String) -> Unit) {
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp, color = SukaOnSurface),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(SukaPrimary),
        decorationBox = { inner ->
            if (value.isEmpty()) Text("0", color = SukaOnSurfaceVariant.copy(alpha = 0.4f), fontSize = 18.sp)
            inner()
        },
    )
}

@Composable
private fun TenorPill(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (selected) Modifier.background(SukaPrimary)
                else Modifier
                    .background(SukaSurfaceContainerLowest)
                    .border(1.dp, SukaOnSurfaceVariant.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (selected) Color.White else SukaOnSurfaceVariant,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
    }
}
