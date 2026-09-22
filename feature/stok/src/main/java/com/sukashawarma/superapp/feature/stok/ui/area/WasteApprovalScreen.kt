package com.sukashawarma.superapp.feature.stok.ui.area

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.RealtimeTables
import com.sukashawarma.superapp.core.ui.SukaDropdownHeader
import com.sukashawarma.superapp.core.ui.SukaDropdownMenu
import com.sukashawarma.superapp.core.ui.SukaDropdownMenuItem
import com.sukashawarma.superapp.feature.stok.data.WasteHistoryItem
import com.sukashawarma.superapp.feature.stok.data.WasteReview
import com.sukashawarma.superapp.feature.stok.domain.formatAngkaStok
import com.sukashawarma.superapp.feature.stok.ui.*

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

private data class FotoBuktiPreview(
    val name: String,
    val outlet: String,
    val photo: String?,
    val quantityLabel: String,
)

@Composable
fun WasteApprovalScreen(onBack: () -> Unit, vm: WasteApprovalViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    RealtimeRefresh(RealtimeTables.WASTE_REPORTS, RealtimeTables.STOK_BALANCE) { vm.refresh() }
    var photoPreview by remember { mutableStateOf<FotoBuktiPreview?>(null) }
    var menuOutletTerbuka by remember { mutableStateOf(false) }

    val namaOutletTerpilih = state.outlets.find { it.id == state.selectedOutletId }?.name ?: "Semua Outlet Binaan"

    Column(Modifier.fillMaxSize().background(Slate50)) {
        HeaderStok("Persetujuan Waste", "Kelola dan tinjau laporan waste dari outlet", onBack) {
            IconButton(onClick = vm::refresh, enabled = !state.loading && !state.busy && !state.historyLoading) {
                Icon(Icons.Default.Refresh, "Perbarui data", tint = Color(0xFF1E293B))
            }
        }

        // Outlet Selector (filter outlet binaan untuk AM/RM)
        if (state.outlets.isNotEmpty()) {
            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                Surface(
                    onClick = { menuOutletTerbuka = true },
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Slate200),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Storefront, null, tint = Orange500, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text("OUTLET BINAAN", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Slate400, letterSpacing = 0.5.sp)
                            Text(namaOutletTerpilih, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Slate900, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Icon(Icons.Default.ArrowDropDown, "Pilih outlet", tint = Slate500)
                    }
                }

                SukaDropdownMenu(
                    expanded = menuOutletTerbuka,
                    onDismissRequest = { menuOutletTerbuka = false }
                ) {
                    SukaDropdownHeader(title = "PILIH OUTLET BINAAN", onClose = { menuOutletTerbuka = false })
                    SukaDropdownMenuItem(
                        text = "Semua Outlet Binaan",
                        selected = (state.selectedOutletId == null),
                        onClick = {
                            menuOutletTerbuka = false
                            vm.selectOutlet(null)
                        }
                    )
                    state.outlets.forEach { outlet ->
                        SukaDropdownMenuItem(
                            text = outlet.name,
                            selected = (state.selectedOutletId == outlet.id),
                            onClick = {
                                menuOutletTerbuka = false
                                vm.selectOutlet(outlet.id)
                            }
                        )
                    }
                }
            }
        }

        // Tab Row: Menunggu vs Riwayat
        TabRow(
            selectedTabIndex = if (state.tab == WasteApprovalTab.MENUNGGU) 0 else 1,
            containerColor = Color.White,
            contentColor = Orange500,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[if (state.tab == WasteApprovalTab.MENUNGGU) 0 else 1]),
                    color = Orange500
                )
            }
        ) {
            Tab(
                selected = state.tab == WasteApprovalTab.MENUNGGU,
                onClick = { vm.selectTab(WasteApprovalTab.MENUNGGU) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Menunggu",
                            fontWeight = if (state.tab == WasteApprovalTab.MENUNGGU) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp,
                            color = if (state.tab == WasteApprovalTab.MENUNGGU) Orange600 else Slate500
                        )
                        if (state.reports.isNotEmpty()) {
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (state.tab == WasteApprovalTab.MENUNGGU) Orange500 else Slate200,
                            ) {
                                Text(
                                    "${state.reports.size}",
                                    color = if (state.tab == WasteApprovalTab.MENUNGGU) Color.White else Slate700,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            )
            Tab(
                selected = state.tab == WasteApprovalTab.RIWAYAT,
                onClick = { vm.selectTab(WasteApprovalTab.RIWAYAT) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Riwayat",
                            fontWeight = if (state.tab == WasteApprovalTab.RIWAYAT) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp,
                            color = if (state.tab == WasteApprovalTab.RIWAYAT) Orange600 else Slate500
                        )
                        if (state.history.isNotEmpty()) {
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (state.tab == WasteApprovalTab.RIWAYAT) Slate700 else Slate200,
                            ) {
                                Text(
                                    "${state.history.size}",
                                    color = if (state.tab == WasteApprovalTab.RIWAYAT) Color.White else Slate700,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            )
        }

        state.message?.let { PitaPesan(it, false, vm::clearMessage) }
        state.error?.let { PitaPesan(it, true, vm::clearMessage) }
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Orange500, trackColor = Orange50)

        when (state.tab) {
            WasteApprovalTab.MENUNGGU -> {
                when {
                    state.loading -> MemuatPenuh()
                    state.error != null && state.reports.isEmpty() -> KeadaanGagal(state.error!!, vm::refresh)
                    state.reports.isEmpty() -> KeadaanKosong("Semua Bersih!\nTidak ada laporan waste yang menunggu persetujuan saat ini.")
                    else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        item {
                            val berisiko = state.reports.count { it.deficit }
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    WasteSummaryFilterCard(
                                        title = "Menunggu",
                                        value = "${state.reports.size}",
                                        caption = "Perlu ditinjau",
                                        titleColor = Color(0xFF7C2D12),
                                        valueColor = Slate900,
                                        captionColor = Slate400,
                                        isSelected = state.pendingFilter == WastePendingFilter.SEMUA,
                                        activeBgColor = Orange50,
                                        activeBorderColor = Orange500,
                                        onClick = { vm.setPendingFilter(WastePendingFilter.SEMUA) }
                                    )
                                    WasteSummaryFilterCard(
                                        title = "Berisiko",
                                        value = "$berisiko",
                                        caption = "Stok defisit",
                                        titleColor = Color(0xFF881337),
                                        valueColor = if (berisiko > 0) Rose600 else Slate900,
                                        captionColor = Rose500,
                                        isSelected = state.pendingFilter == WastePendingFilter.BERISIKO,
                                        activeBgColor = Rose50,
                                        activeBorderColor = Rose600,
                                        onClick = { vm.setPendingFilter(WastePendingFilter.BERISIKO) }
                                    )
                                    WasteSummaryFilterCard(
                                        title = "Outlet",
                                        value = "${state.reports.map { it.outletId }.distinct().size}",
                                        caption = "Melapor waste",
                                        titleColor = Slate700,
                                        valueColor = Slate900,
                                        captionColor = Slate400,
                                        isSelected = false,
                                        activeBgColor = Slate100,
                                        activeBorderColor = Slate400,
                                        onClick = {}
                                    )
                                }
                                if (state.pendingFilter == WastePendingFilter.BERISIKO) {
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Menampilkan $berisiko laporan berisiko stok negatif",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Rose700
                                        )
                                        TextButton(
                                            onClick = { vm.setPendingFilter(WastePendingFilter.SEMUA) },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                        ) {
                                            Text("Tampilkan Semua", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Orange600)
                                        }
                                    }
                                }
                            }
                        }
                        if (state.filteredReports.isEmpty()) {
                            item {
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "Tidak ada laporan yang berisiko stok negatif.",
                                        color = Slate500,
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = { vm.setPendingFilter(WastePendingFilter.SEMUA) },
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, Orange500)
                                    ) {
                                        Text("Tampilkan Semua Laporan", color = Orange600, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        } else {
                            items(state.filteredReports, key = { it.id }) { report ->
                                WasteCard(
                                    report = report,
                                    busy = state.busy,
                                    onPhoto = {
                                        photoPreview = FotoBuktiPreview(report.name, report.outlet, report.photo, report.quantityLabel)
                                    },
                                    onApprove = { vm.approve(report) },
                                    onReject = { vm.reject(report) },
                                )
                            }
                        }
                    }
                }
            }
            WasteApprovalTab.RIWAYAT -> {
                when {
                    state.historyLoading -> MemuatPenuh()
                    state.historyError != null && state.history.isEmpty() -> KeadaanGagal(state.historyError!!, vm::loadHistory)
                    state.history.isEmpty() -> KeadaanKosong("Belum ada riwayat waste untuk outlet yang dipilih.")
                    else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        item {
                            val disetujui = state.history.count { it.isApproved }
                            val ditolak = state.history.count { it.isRejected }
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    WasteSummaryFilterCard(
                                        title = "Semua",
                                        value = "${state.history.size}",
                                        caption = "Total riwayat",
                                        titleColor = Slate700,
                                        valueColor = Slate900,
                                        captionColor = Slate400,
                                        isSelected = state.historyFilter == WasteHistoryStatusFilter.SEMUA,
                                        activeBgColor = Slate100,
                                        activeBorderColor = Slate700,
                                        onClick = { vm.setHistoryFilter(WasteHistoryStatusFilter.SEMUA) }
                                    )
                                    WasteSummaryFilterCard(
                                        title = "Disetujui",
                                        value = "$disetujui",
                                        caption = "Stok dipotong",
                                        titleColor = Emerald700,
                                        valueColor = Emerald600,
                                        captionColor = Slate400,
                                        isSelected = state.historyFilter == WasteHistoryStatusFilter.DISETUJUI,
                                        activeBgColor = Emerald50,
                                        activeBorderColor = Emerald600,
                                        onClick = { vm.setHistoryFilter(WasteHistoryStatusFilter.DISETUJUI) }
                                    )
                                    WasteSummaryFilterCard(
                                        title = "Ditolak",
                                        value = "$ditolak",
                                        caption = "Tidak disetujui",
                                        titleColor = Rose700,
                                        valueColor = Rose600,
                                        captionColor = Slate400,
                                        isSelected = state.historyFilter == WasteHistoryStatusFilter.DITOLAK,
                                        activeBgColor = Rose50,
                                        activeBorderColor = Rose600,
                                        onClick = { vm.setHistoryFilter(WasteHistoryStatusFilter.DITOLAK) }
                                    )
                                }
                                if (state.historyFilter != WasteHistoryStatusFilter.SEMUA) {
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = when (state.historyFilter) {
                                                WasteHistoryStatusFilter.DISETUJUI -> "Menampilkan $disetujui laporan disetujui"
                                                WasteHistoryStatusFilter.DITOLAK -> "Menampilkan $ditolak laporan ditolak"
                                                else -> ""
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Slate600
                                        )
                                        TextButton(
                                            onClick = { vm.setHistoryFilter(WasteHistoryStatusFilter.SEMUA) },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                        ) {
                                            Text("Tampilkan Semua", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Orange600)
                                        }
                                    }
                                }
                            }
                        }
                        if (state.filteredHistory.isEmpty()) {
                            item {
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "Tidak ada laporan waste dengan status ${state.historyFilter.label}.",
                                        color = Slate500,
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = { vm.setHistoryFilter(WasteHistoryStatusFilter.SEMUA) },
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, Orange500)
                                    ) {
                                        Text("Tampilkan Semua Riwayat", color = Orange600, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        } else {
                            items(state.filteredHistory, key = { it.id }) { item ->
                                WasteHistoryCard(
                                    item = item,
                                    onPhoto = {
                                        photoPreview = FotoBuktiPreview(item.bahanName, item.outletName, item.photoUrl, item.quantityLabel)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    photoPreview?.let { data ->
        DialogFotoBukti(data) { photoPreview = null }
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

@Composable
private fun WasteHistoryCard(
    item: WasteHistoryItem,
    onPhoto: () -> Unit
) {
    val isApproved = item.isApproved
    val stripColor = if (isApproved) Emerald50 else Rose50
    val borderChipColor = if (isApproved) Emerald200 else Rose200
    val bgChipColor = if (isApproved) Emerald100 else Rose100
    val textChipColor = if (isApproved) Emerald700 else Rose700
    val dotChipColor = if (isApproved) Emerald500 else Rose500
    val statusLabel = if (isApproved) "DISETUJUI" else "DITOLAK"

    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Slate200)
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().background(stripColor).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusChip(statusLabel, bgChipColor, borderChipColor, textChipColor, dotChipColor)
                Spacer(Modifier.weight(1f))
                Text(
                    waktuSingkat(item.updatedAt ?: item.createdAt),
                    color = Slate400,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Column {
                    Text(
                        item.bahanName,
                        color = Slate900,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 19.sp
                    )
                    Text(
                        item.outletName.ifBlank { "Outlet tidak tercatat" },
                        color = Slate500,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Surface(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Slate50,
                    border = BorderStroke(1.dp, Slate100)
                ) {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("JUMLAH WASTE", color = Slate400, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                            Spacer(Modifier.height(2.dp))
                            Text(item.quantityLabel, color = if (isApproved) Emerald700 else Rose600, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 19.sp)
                        }
                        Box(Modifier.height(34.dp).width(1.dp).background(Slate200))
                        Column(Modifier.weight(1f).padding(start = 10.dp)) {
                            Text("ALASAN LAPORAN", color = Slate400, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                            Spacer(Modifier.height(2.dp))
                            Text(item.reason.ifBlank { "—" }, color = Slate700, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }

                // Pelapor info
                Surface(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Slate50,
                    border = BorderStroke(1.dp, Slate100)
                ) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        MetaRow("Pelapor", item.reporterName.ifBlank { "—" })
                    }
                }

                // Decider Info (Who approved or rejected)
                if (isApproved) {
                    Surface(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Emerald50,
                        border = BorderStroke(1.dp, Emerald200)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = Emerald600, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("DISETUJUI OLEH", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Emerald700, letterSpacing = 0.5.sp)
                                Text(
                                    item.deciderName?.takeIf { it.isNotBlank() } ?: "Approver",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Slate900
                                )
                            }
                        }
                    }
                } else {
                    Surface(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Rose50,
                        border = BorderStroke(1.dp, Rose200)
                    ) {
                        Column(
                            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Close, null, tint = Rose600, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text("DITOLAK OLEH", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Rose700, letterSpacing = 0.5.sp)
                                    Text(
                                        item.deciderName?.takeIf { it.isNotBlank() } ?: "Approver",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Slate900
                                    )
                                }
                            }
                            if (!item.rejectionReason.isNullOrBlank()) {
                                Box(Modifier.fillMaxWidth().height(1.dp).background(Rose200))
                                Text(
                                    "Alasan penolakan: ${item.rejectionReason}",
                                    color = Rose700,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }

                if (!item.photoUrl.isNullOrBlank()) {
                    Surface(
                        onClick = onPhoto,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Slate200),
                    ) {
                        Row(
                            Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Image, null, tint = Slate500, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Lihat Foto Bukti", color = Slate700, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
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
private fun DialogFotoBukti(preview: FotoBuktiPreview, onTutup: () -> Unit) {
    Dialog(onDismissRequest = onTutup) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White) {
            Column {
                Row(Modifier.padding(start = 16.dp, end = 6.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(preview.name, color = Slate900, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            preview.outlet.ifBlank { "Outlet tidak tercatat" },
                            color = Slate500, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(onClick = onTutup) { Icon(Icons.Default.Close, "Tutup foto bukti", tint = Slate500) }
                }
                SubcomposeAsyncImage(
                    model = preview.photo,
                    contentDescription = "Foto bukti waste ${preview.name}",
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
                    Text(preview.quantityLabel, color = Rose600, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun RowScope.WasteSummaryFilterCard(
    title: String,
    value: String,
    caption: String,
    titleColor: Color,
    valueColor: Color,
    captionColor: Color,
    isSelected: Boolean,
    activeBgColor: Color,
    activeBorderColor: Color,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) activeBgColor else Color.White,
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) activeBorderColor else Slate200
        ),
    ) {
        Column(
            Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isSelected) {
                    Box(
                        Modifier
                            .size(6.dp)
                            .background(activeBorderColor, CircleShape)
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    title,
                    color = if (isSelected) activeBorderColor else titleColor,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(value, color = valueColor, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Text(
                caption,
                color = if (isSelected) activeBorderColor.copy(alpha = 0.85f) else captionColor,
                fontSize = 9.sp,
                textAlign = TextAlign.Center,
                lineHeight = 11.sp,
                maxLines = 2
            )
        }
    }
}
