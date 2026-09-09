package com.sukashawarma.superapp.feature.stok.ui.area

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import com.sukashawarma.superapp.feature.stok.data.WasteReview
import com.sukashawarma.superapp.feature.stok.domain.formatAngkaStok
import com.sukashawarma.superapp.feature.stok.ui.*
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.RealtimeTables

/**
 * Rona kartu menjawab satu pertanyaan yang menentukan keputusan approver: apakah
 * menyetujui laporan ini membuat stok outlet menjadi negatif.
 */
private data class WasteTone(val strip: Color, val chip: Color, val border: Color, val text: Color, val dot: Color, val label: String)

private fun tone(report: WasteReview): WasteTone = when {
    report.balance == null -> WasteTone(Amber50, Amber100, Amber300, Amber800, Amber500, "Skala Tidak Pasti")
    report.deficit -> WasteTone(Rose50, Rose100, Rose200, Rose700, Rose500, "Stok Akan Negatif")
    else -> WasteTone(Emerald50, Emerald100, Emerald200, Emerald700, Emerald500, "Saldo Mencukupi")
}

@Composable
fun WasteApprovalScreen(onBack: () -> Unit, vm: WasteApprovalViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    RealtimeRefresh(RealtimeTables.WASTE_REPORTS, RealtimeTables.STOK_BALANCE) { vm.refresh() }
    var photoPreview by remember { mutableStateOf<WasteReview?>(null) }
    Column(Modifier.fillMaxSize().background(Slate50)) {
        HeaderStok("Persetujuan Waste", "Kelola dan tinjau laporan waste dari outlet", onBack) {
            IconButton(onClick = vm::refresh, enabled = !state.loading && !state.busy) { Icon(Icons.Default.Refresh, "Perbarui data", tint = Color.White) }
        }
        state.message?.let { PitaPesan(it, false, vm::clearMessage) }
        state.error?.let { PitaPesan(it, true, vm::clearMessage) }
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Orange500, trackColor = Orange50)
        when {
            state.loading -> MemuatPenuh()
            state.error != null && state.reports.isEmpty() -> KeadaanGagal(state.error!!, vm::refresh)
            state.reports.isEmpty() -> KeadaanKosong("Semua Bersih!\nTidak ada laporan waste yang menunggu persetujuan saat ini.")
            else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(1.dp, Slate200)) {
                        Row(Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            KpiCell("Menunggu", "${state.reports.size}", "Perlu ditinjau", Color(0xFF7C2D12), Slate900, Slate400)
                            KpiDivider()
                            val berisiko = state.reports.count { it.deficit }
                            KpiCell("Berisiko", "$berisiko", "Stok jadi negatif", Color(0xFF881337), if (berisiko > 0) Rose600 else Slate900, Rose500)
                            KpiDivider()
                            KpiCell("Outlet", "${state.reports.map { it.outletId }.distinct().size}", "Melapor waste", Slate700, Slate900, Slate400)
                        }
                    }
                }
                items(state.reports, key = { it.id }) { report ->
                    WasteCard(
                        report = report,
                        busy = state.busy,
                        onPhoto = { photoPreview = report },
                        onApprove = { vm.approve(report) },
                        onReject = { vm.reject(report) },
                    )
                }
            }
        }
    }
    photoPreview?.let { report ->
        DialogFotoBukti(report) { photoPreview = null }
    }
    state.confirmation?.let { report ->
        AlertDialog(
            onDismissRequest = vm::dismiss,
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White,
            title = { Text("Tetap setujui waste?", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Slate900) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = Rose50, border = BorderStroke(1.dp, Rose200)) {
                        Text(
                            if (report.balance == null) "Saldo tidak dapat dikonversi ke satuan bahan. Pastikan jumlah waste ${report.quantityLabel} sudah benar."
                            else "Qty waste ${report.quantityLabel} lebih besar dari saldo ${formatAngkaStok(report.balance)} ${report.meta.satuan.orEmpty()}. Saldo akan menjadi negatif.",
                            Modifier.padding(12.dp), color = Rose700, fontSize = 12.sp, lineHeight = 17.sp,
                        )
                    }
                    state.error?.let { Text(it, color = Rose600, fontSize = 12.sp) }
                }
            },
            confirmButton = {
                TextButton(onClick = { vm.decide(report) }, enabled = !state.busy) {
                    Text("Tetap Setujui", color = Rose600, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = vm::dismiss, enabled = !state.busy) { Text("Batal", color = Slate500) } },
        )
    }
    state.rejecting?.let { report ->
        var reason by rememberSaveable(report.id) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = vm::dismiss,
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White,
            title = { Text("Tolak Laporan", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Slate900) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Berikan alasan penolakan waste ${report.name}.", color = Slate500, fontSize = 12.sp)
                    OutlinedTextField(
                        reason, { reason = it },
                        Modifier.fillMaxWidth(),
                        placeholder = { Text("Alasan penolakan", fontSize = 12.sp, color = Slate400) },
                        minLines = 3, enabled = !state.busy,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Slate50, unfocusedContainerColor = Slate50,
                            focusedBorderColor = Orange500, unfocusedBorderColor = Slate200,
                        ),
                    )
                    state.error?.let { Text(it, color = Rose600, fontSize = 12.sp) }
                }
            },
            confirmButton = {
                TextButton(onClick = { vm.decide(report, reason) }, enabled = reason.isNotBlank() && !state.busy) {
                    Text("Tolak Laporan", color = if (reason.isBlank()) Slate400 else Rose600, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = vm::dismiss, enabled = !state.busy) { Text("Batal", color = Slate500) } },
        )
    }
}

@Composable
private fun WasteCard(report: WasteReview, busy: Boolean, onPhoto: () -> Unit, onApprove: () -> Unit, onReject: () -> Unit) {
    val tone = tone(report)
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(1.dp, Slate200)) {
        Column {
            Row(
                Modifier.fillMaxWidth().background(tone.strip).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusChip(tone.label, tone.chip, tone.border, tone.text, tone.dot)
                Spacer(Modifier.weight(1f))
                Text(waktuSingkat(report.date), color = Slate400, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Column {
                    Text(report.name, color = Slate900, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 19.sp)
                    Text(
                        report.outlet.ifBlank { "Outlet tidak tercatat" },
                        color = Slate500, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = Slate50, border = BorderStroke(1.dp, Slate100)) {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("JUMLAH WASTE", color = Slate400, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                            Spacer(Modifier.height(2.dp))
                            Text(report.quantityLabel, color = Rose600, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 20.sp)
                        }
                        Box(Modifier.height(38.dp).width(1.dp).background(Slate200))
                        Column(Modifier.weight(1f).padding(start = 10.dp), horizontalAlignment = Alignment.End) {
                            Text("SALDO SAAT INI", color = Slate400, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                            Spacer(Modifier.height(2.dp))
                            if (report.balance == null) {
                                Text("Tidak diketahui", color = Amber800, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                            } else {
                                Text(
                                    "${formatAngkaStok(report.balance)} ${report.meta.satuan.orEmpty()}",
                                    color = if (report.deficit) Rose600 else Slate900,
                                    fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.End,
                                )
                            }
                        }
                    }
                }
                if (report.deficit) Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = tone.strip, border = BorderStroke(1.dp, tone.border)) {
                    Text(
                        if (report.balance == null) "Saldo tidak dapat dikonversi. Periksa stok sebelum menyetujui."
                        else "Menyetujui laporan ini membuat stok outlet menjadi negatif.",
                        Modifier.padding(10.dp), color = tone.text, fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold,
                    )
                }
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = Slate50, border = BorderStroke(1.dp, Slate100)) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        MetaRow("Pelapor", report.reporter.ifBlank { "—" })
                        Box(Modifier.fillMaxWidth().height(1.dp).background(Slate200))
                        MetaRow("Alasan waste", report.reason.ifBlank { "—" })
                    }
                }
                if (!report.photo.isNullOrBlank()) Surface(
                    onClick = onPhoto,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Slate200),
                ) {
                    Row(Modifier.padding(vertical = 10.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Image, null, tint = Slate500, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Lihat Foto Bukti", color = Slate700, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onApprove, enabled = !busy, modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600, disabledContainerColor = Slate200),
                        contentPadding = PaddingValues(vertical = 11.dp),
                    ) {
                        Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Setujui", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onReject, enabled = !busy, modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Rose200),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Rose50, contentColor = Rose600),
                        contentPadding = PaddingValues(vertical = 11.dp),
                    ) {
                        Icon(Icons.Default.Close, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Tolak", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Bukti waste ditinjau di dalam aplikasi: melempar approver ke browser memutus
 * alur persetujuan dan menyembunyikan konteks kartu yang sedang dinilai.
 */
@Composable
private fun DialogFotoBukti(report: WasteReview, onTutup: () -> Unit) {
    Dialog(onDismissRequest = onTutup) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White) {
            Column {
                Row(Modifier.padding(start = 16.dp, end = 6.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(report.name, color = Slate900, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            report.outlet.ifBlank { "Outlet tidak tercatat" },
                            color = Slate500, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(onClick = onTutup) { Icon(Icons.Default.Close, "Tutup foto bukti", tint = Slate500) }
                }
                SubcomposeAsyncImage(
                    model = report.photo,
                    contentDescription = "Foto bukti waste ${report.name}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().height(340.dp).background(Slate900),
                    loading = {
                        Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Orange500) }
                    },
                    error = {
                        Box(Modifier.fillMaxSize().padding(24.dp), Alignment.Center) {
                            Text(
                                "Foto bukti tidak dapat dimuat.",
                                color = Slate400, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                            )
                        }
                    },
                )
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Jumlah waste", Modifier.weight(1f), color = Slate500, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Text(report.quantityLabel, color = Rose600, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}
