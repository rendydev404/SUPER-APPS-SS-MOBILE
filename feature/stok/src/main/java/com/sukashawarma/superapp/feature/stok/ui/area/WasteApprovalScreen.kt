package com.sukashawarma.superapp.feature.stok.ui.area

import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.core.ui.kaca.denganRuangNav
import com.sukashawarma.superapp.core.ui.ios.KartuIos
import com.sukashawarma.superapp.core.ui.ios.KeadaanIos
import com.sukashawarma.superapp.core.ui.ios.LencanaIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.SegmenIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.TombolBundarIos
import com.sukashawarma.superapp.core.ui.ios.TombolKeduaIos
import com.sukashawarma.superapp.core.ui.ios.TombolUtamaIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WadahSegmenIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.ios.permukaanIos
import com.sukashawarma.superapp.core.ui.ios.tekanIos
import com.sukashawarma.superapp.core.ui.ios.warnaKolomIos
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
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
 * Nada kartu menjawab satu pertanyaan yang menentukan keputusan approver: apakah
 * menyetujui laporan ini membuat stok outlet menjadi negatif.
 */
private data class WasteTone(val nada: NadaIos, val label: String)

private fun tone(report: WasteReview): WasteTone = when {
    report.balance == null -> WasteTone(NadaIos.PERINGATAN, "Skala Tidak Pasti")
    report.deficit -> WasteTone(NadaIos.BAHAYA, "Stok Akan Negatif")
    else -> WasteTone(NadaIos.SUKSES, "Saldo Mencukupi")
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
    RealtimeRefresh(RealtimeTables.WASTE_REPORTS) { vm.refresh() }
    // Saldo berubah di setiap transaksi kasir di semua outlet; dibatasi sekali per
    // 30 detik. Antrean waste di atas tetap seketika.
    RealtimeRefresh(RealtimeTables.STOK_BALANCE, jedaMinimumMs = 30_000L) { vm.refresh() }
    var photoPreview by remember { mutableStateOf<FotoBuktiPreview?>(null) }
    var menuOutletTerbuka by remember { mutableStateOf(false) }

    val namaOutletTerpilih = state.outlets.find { it.id == state.selectedOutletId }?.name ?: "Semua Outlet Binaan"

    Column(Modifier.fillMaxSize().background(WarnaIos.Latar)) {
        HeaderStok("Persetujuan Waste", "Kelola dan tinjau laporan waste dari outlet", onBack) {
            TombolBundarIos(
                IkonIos.Refresh, "Perbarui data", vm::refresh,
                aktif = !state.loading && !state.busy && !state.historyLoading,
            )
        }

        // Pemilih outlet binaan untuk AM/RM
        if (state.outlets.isNotEmpty()) {
            Box(Modifier.fillMaxWidth().padding(start = UkuranIos.TepiLayar, end = UkuranIos.TepiLayar, top = 10.dp)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .permukaanIos(UkuranIos.SudutGrup)
                        .tekanIos({ menuOutletTerbuka = true })
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(30.dp).clip(CircleShape).background(WarnaIos.Aksen.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(IkonIos.Storefront, null, tint = WarnaIos.Aksen, modifier = Modifier.size(17.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Outlet binaan", style = TipeIos.Kecil)
                        Text(
                            namaOutletTerpilih,
                            style = TipeIos.Keterangan.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Icon(IkonIos.ArrowDropDown, "Pilih outlet", tint = WarnaIos.Aksen, modifier = Modifier.size(16.dp))
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

        // Segmen: Menunggu vs Riwayat
        WadahSegmenIos(Modifier.padding(start = UkuranIos.TepiLayar, end = UkuranIos.TepiLayar, top = 10.dp, bottom = 2.dp)) {
            val menunggu = state.tab == WasteApprovalTab.MENUNGGU
            SegmenIos(
                label = "Menunggu",
                aktif = menunggu,
                onKlik = { vm.selectTab(WasteApprovalTab.MENUNGGU) },
                modifier = Modifier.weight(1f),
                lencana = state.reports.size.takeIf { it > 0 }?.toString(),
                warnaLencana = if (menunggu) WarnaIos.Aksen else WarnaIos.Abu.copy(alpha = 0.35f),
                jarakSisi = 6.dp,
            )
            SegmenIos(
                label = "Riwayat",
                aktif = !menunggu,
                onKlik = { vm.selectTab(WasteApprovalTab.RIWAYAT) },
                modifier = Modifier.weight(1f),
                lencana = state.history.size.takeIf { it > 0 }?.toString(),
                warnaLencana = if (!menunggu) WarnaIos.Aksen else WarnaIos.Abu.copy(alpha = 0.35f),
                jarakSisi = 6.dp,
            )
        }

        state.message?.let { PitaPesan(it, false, vm::clearMessage) }
        state.error?.let { PitaPesan(it, true, vm::clearMessage) }
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = WarnaIos.Aksen, trackColor = WarnaIos.Isian)

        when (state.tab) {
            WasteApprovalTab.MENUNGGU -> {
                when {
                    state.loading -> MemuatPenuh()
                    state.error != null && state.reports.isEmpty() -> KeadaanGagal(state.error!!, vm::refresh)
                    state.reports.isEmpty() -> KeadaanKosong("Semua Bersih!\nTidak ada laporan waste yang menunggu persetujuan saat ini.")
                    else -> LazyColumn(
                        contentPadding = PaddingValues(horizontal = UkuranIos.TepiLayar, vertical = 12.dp).denganRuangNav(),
                        verticalArrangement = Arrangement.spacedBy(UkuranIos.JarakKartu),
                    ) {
                        item {
                            val berisiko = state.reports.count { it.deficit }
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    WasteSummaryFilterCard(
                                        title = "Menunggu",
                                        value = "${state.reports.size}",
                                        caption = "Perlu ditinjau",
                                        warna = WarnaIos.Aksen,
                                        isSelected = state.pendingFilter == WastePendingFilter.SEMUA,
                                        onClick = { vm.setPendingFilter(WastePendingFilter.SEMUA) }
                                    )
                                    WasteSummaryFilterCard(
                                        title = "Berisiko",
                                        value = "$berisiko",
                                        caption = "Stok defisit",
                                        warna = WarnaIos.Merah,
                                        valueColor = if (berisiko > 0) NadaIos.BAHAYA.teks else WarnaIos.Label,
                                        isSelected = state.pendingFilter == WastePendingFilter.BERISIKO,
                                        onClick = { vm.setPendingFilter(WastePendingFilter.BERISIKO) }
                                    )
                                    WasteSummaryFilterCard(
                                        title = "Outlet",
                                        value = "${state.reports.map { it.outletId }.distinct().size}",
                                        caption = "Melapor waste",
                                        warna = WarnaIos.AbuGelap,
                                        isSelected = false,
                                        onClick = null
                                    )
                                }
                                if (state.pendingFilter == WastePendingFilter.BERISIKO) {
                                    BarisFilterAktif(
                                        "Menampilkan $berisiko laporan berisiko stok negatif",
                                        NadaIos.BAHAYA.teks,
                                    ) { vm.setPendingFilter(WastePendingFilter.SEMUA) }
                                }
                            }
                        }
                        if (state.filteredReports.isEmpty()) {
                            item {
                                KeadaanIos(
                                    IkonIos.CheckCircle,
                                    "Tidak ada laporan yang berisiko stok negatif.",
                                    "",
                                    nada = NadaIos.SUKSES,
                                    teksAksi = "Tampilkan Semua Laporan",
                                    onAksi = { vm.setPendingFilter(WastePendingFilter.SEMUA) },
                                )
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
                    else -> LazyColumn(
                        contentPadding = PaddingValues(horizontal = UkuranIos.TepiLayar, vertical = 12.dp).denganRuangNav(),
                        verticalArrangement = Arrangement.spacedBy(UkuranIos.JarakKartu),
                    ) {
                        item {
                            val disetujui = state.history.count { it.isApproved }
                            val ditolak = state.history.count { it.isRejected }
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    WasteSummaryFilterCard(
                                        title = "Semua",
                                        value = "${state.history.size}",
                                        caption = "Total riwayat",
                                        warna = WarnaIos.AbuGelap,
                                        isSelected = state.historyFilter == WasteHistoryStatusFilter.SEMUA,
                                        onClick = { vm.setHistoryFilter(WasteHistoryStatusFilter.SEMUA) }
                                    )
                                    WasteSummaryFilterCard(
                                        title = "Disetujui",
                                        value = "$disetujui",
                                        caption = "Stok dipotong",
                                        warna = WarnaIos.Hijau,
                                        valueColor = NadaIos.SUKSES.teks,
                                        isSelected = state.historyFilter == WasteHistoryStatusFilter.DISETUJUI,
                                        onClick = { vm.setHistoryFilter(WasteHistoryStatusFilter.DISETUJUI) }
                                    )
                                    WasteSummaryFilterCard(
                                        title = "Ditolak",
                                        value = "$ditolak",
                                        caption = "Tidak disetujui",
                                        warna = WarnaIos.Merah,
                                        valueColor = NadaIos.BAHAYA.teks,
                                        isSelected = state.historyFilter == WasteHistoryStatusFilter.DITOLAK,
                                        onClick = { vm.setHistoryFilter(WasteHistoryStatusFilter.DITOLAK) }
                                    )
                                }
                                if (state.historyFilter != WasteHistoryStatusFilter.SEMUA) {
                                    BarisFilterAktif(
                                        when (state.historyFilter) {
                                            WasteHistoryStatusFilter.DISETUJUI -> "Menampilkan $disetujui laporan disetujui"
                                            WasteHistoryStatusFilter.DITOLAK -> "Menampilkan $ditolak laporan ditolak"
                                            else -> ""
                                        },
                                        WarnaIos.LabelKedua,
                                    ) { vm.setHistoryFilter(WasteHistoryStatusFilter.SEMUA) }
                                }
                            }
                        }
                        if (state.filteredHistory.isEmpty()) {
                            item {
                                KeadaanIos(
                                    IkonIos.Inbox,
                                    "Tidak ada laporan waste dengan status ${state.historyFilter.label}.",
                                    "",
                                    teksAksi = "Tampilkan Semua Riwayat",
                                    onAksi = { vm.setHistoryFilter(WasteHistoryStatusFilter.SEMUA) },
                                )
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
            shape = UkuranIos.SudutKartu,
            containerColor = WarnaIos.Kartu,
            title = { Text("Tetap setujui waste?", style = TipeIos.Utama) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    BannerIos(
                        if (report.balance == null) "Saldo tidak dapat dikonversi ke satuan bahan. Pastikan jumlah waste ${report.quantityLabel} sudah benar."
                        else "Qty waste ${report.quantityLabel} lebih besar dari saldo ${formatAngkaStok(report.balance)} ${report.meta.satuan.orEmpty()}. Saldo akan menjadi negatif.",
                        NadaIos.BAHAYA,
                        ikon = IkonIos.WarningAmber,
                    )
                    state.error?.let { Text(it, style = TipeIos.Catatan.copy(color = NadaIos.BAHAYA.teks)) }
                }
            },
            confirmButton = {
                TextButton(onClick = { vm.decide(report) }, enabled = !state.busy) {
                    Text("Tetap Setujui", color = WarnaIos.Merah, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = vm::dismiss, enabled = !state.busy) {
                    Text("Batal", color = WarnaIos.Aksen, fontSize = 16.sp)
                }
            },
        )
    }
    state.rejecting?.let { report ->
        var reason by rememberSaveable(report.id) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = vm::dismiss,
            shape = UkuranIos.SudutKartu,
            containerColor = WarnaIos.Kartu,
            title = { Text("Tolak Laporan", style = TipeIos.Utama) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Berikan alasan penolakan waste ${report.name}.", style = TipeIos.SubJudul)
                    OutlinedTextField(
                        reason, { reason = it },
                        Modifier.fillMaxWidth(),
                        placeholder = { Text("Alasan penolakan", fontSize = 15.sp) },
                        minLines = 3, enabled = !state.busy,
                        shape = UkuranIos.SudutKontrol,
                        colors = warnaKolomIos(),
                    )
                    state.error?.let { Text(it, style = TipeIos.Catatan.copy(color = NadaIos.BAHAYA.teks)) }
                }
            },
            confirmButton = {
                TextButton(onClick = { vm.decide(report, reason) }, enabled = reason.isNotBlank() && !state.busy) {
                    Text(
                        "Tolak Laporan",
                        color = if (reason.isBlank()) WarnaIos.LabelKetiga else WarnaIos.Merah,
                        fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = vm::dismiss, enabled = !state.busy) {
                    Text("Batal", color = WarnaIos.Aksen, fontSize = 16.sp)
                }
            },
        )
    }
}

/** Keterangan filter yang sedang aktif + tautan untuk kembali ke semua data. */
@Composable
private fun BarisFilterAktif(teks: String, warna: Color, onSemua: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(teks, Modifier.weight(1f), style = TipeIos.Catatan.copy(color = warna))
        Text(
            "Tampilkan Semua",
            Modifier
                .clip(UkuranIos.SudutKapsul)
                .tekanIos(onSemua)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            color = WarnaIos.Aksen, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
        )
    }
}

/** Dua angka berdampingan dalam blok abu — kiri rata kiri, kanan rata kanan. */
@Composable
private fun BlokDuaAngka(
    labelKiri: String,
    kiri: @Composable () -> Unit,
    labelKanan: String,
    kanan: @Composable () -> Unit,
    kananRataKanan: Boolean = true,
) {
    Row(
        Modifier.fillMaxWidth().clip(UkuranIos.SudutBlok).background(WarnaIos.Latar).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(labelKiri, style = TipeIos.Kecil.copy(fontWeight = FontWeight.Medium))
            Spacer(Modifier.height(2.dp))
            kiri()
        }
        Box(Modifier.height(34.dp).width(0.5.dp).background(WarnaIos.Pemisah))
        Column(
            Modifier.weight(1f).padding(start = 12.dp),
            horizontalAlignment = if (kananRataKanan) Alignment.End else Alignment.Start,
        ) {
            Text(labelKanan, style = TipeIos.Kecil.copy(fontWeight = FontWeight.Medium))
            Spacer(Modifier.height(2.dp))
            kanan()
        }
    }
}

@Composable
private fun WasteCard(report: WasteReview, busy: Boolean, onPhoto: () -> Unit, onApprove: () -> Unit, onReject: () -> Unit) {
    val tone = tone(report)
    KartuIos {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            LencanaIos(tone.label, tone.nada)
            Spacer(Modifier.weight(1f))
            Text(waktuSingkat(report.date), style = TipeIos.Catatan)
        }
        Spacer(Modifier.height(10.dp))
        Text(report.name, style = TipeIos.Utama)
        Text(
            report.outlet.ifBlank { "Outlet tidak tercatat" },
            style = TipeIos.Catatan,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(12.dp))
        BlokDuaAngka(
            "Jumlah waste",
            { Text(report.quantityLabel, style = TipeIos.Angka.copy(fontSize = 17.sp, color = NadaIos.BAHAYA.teks)) },
            "Saldo saat ini",
            {
                if (report.balance == null) {
                    Text("Tidak diketahui", color = NadaIos.PERINGATAN.teks, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
                } else {
                    Text(
                        "${formatAngkaStok(report.balance)} ${report.meta.satuan.orEmpty()}",
                        color = if (report.deficit) NadaIos.BAHAYA.teks else WarnaIos.Label,
                        fontSize = 15.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End,
                    )
                }
            },
        )
        if (report.deficit) {
            Spacer(Modifier.height(8.dp))
            BannerIos(
                if (report.balance == null) "Saldo tidak dapat dikonversi. Periksa stok sebelum menyetujui."
                else "Menyetujui laporan ini membuat stok outlet menjadi negatif.",
                tone.nada,
                ikon = IkonIos.WarningAmber,
            )
        }
        Spacer(Modifier.height(8.dp))
        BlokAbuIos {
            MetaRow("Pelapor", report.reporter.ifBlank { "—" })
            Box(Modifier.fillMaxWidth().height(0.5.dp).background(WarnaIos.Pemisah))
            MetaRow("Alasan waste", report.reason.ifBlank { "—" })
        }
        if (!report.photo.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            TombolKeduaIos("Lihat Foto Bukti", onPhoto, Modifier.height(42.dp), ikon = IkonIos.Image, warna = WarnaIos.AbuGelap)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TombolUtamaIos(
                "Setujui", onApprove, Modifier.weight(1f).height(46.dp),
                aktif = !busy, ikon = IkonIos.Check, warna = WarnaIos.Hijau,
            )
            TombolKeduaIos(
                "Tolak", onReject, Modifier.weight(1f).height(46.dp),
                aktif = !busy, ikon = IkonIos.Close, warna = WarnaIos.Merah,
            )
        }
    }
}

@Composable
private fun WasteHistoryCard(
    item: WasteHistoryItem,
    onPhoto: () -> Unit
) {
    val isApproved = item.isApproved
    val nada = if (isApproved) NadaIos.SUKSES else NadaIos.BAHAYA
    val statusLabel = if (isApproved) "Disetujui" else "Ditolak"

    KartuIos {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            LencanaIos(statusLabel, nada)
            Spacer(Modifier.weight(1f))
            Text(waktuSingkat(item.updatedAt ?: item.createdAt), style = TipeIos.Catatan)
        }
        Spacer(Modifier.height(10.dp))
        Text(item.bahanName, style = TipeIos.Utama)
        Text(
            item.outletName.ifBlank { "Outlet tidak tercatat" },
            style = TipeIos.Catatan,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(12.dp))
        BlokDuaAngka(
            "Jumlah waste",
            { Text(item.quantityLabel, style = TipeIos.Angka.copy(fontSize = 17.sp, color = nada.teks)) },
            "Alasan laporan",
            {
                Text(
                    item.reason.ifBlank { "—" }, color = WarnaIos.Label, fontSize = 13.sp,
                    fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
            },
            kananRataKanan = false,
        )
        Spacer(Modifier.height(8.dp))
        BlokAbuIos {
            MetaRow("Pelapor", item.reporterName.ifBlank { "—" })
        }
        Spacer(Modifier.height(8.dp))

        // Siapa yang menyetujui atau menolak
        Column(
            Modifier
                .fillMaxWidth()
                .clip(UkuranIos.SudutKontrol)
                .background(nada.warna.copy(alpha = 0.12f))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(24.dp).clip(CircleShape).background(nada.warna),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(if (isApproved) IkonIos.Check else IkonIos.Close, null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(if (isApproved) "Disetujui oleh" else "Ditolak oleh", style = TipeIos.Kecil.copy(color = nada.teks))
                    Text(
                        item.deciderName?.takeIf { it.isNotBlank() } ?: "Approver",
                        style = TipeIos.Keterangan.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                    )
                }
            }
            if (!isApproved && !item.rejectionReason.isNullOrBlank()) {
                Box(Modifier.fillMaxWidth().height(0.5.dp).background(nada.warna.copy(alpha = 0.3f)))
                Text(
                    "Alasan penolakan: ${item.rejectionReason}",
                    style = TipeIos.Catatan.copy(color = nada.teks, lineHeight = 18.sp),
                )
            }
        }

        if (!item.photoUrl.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            TombolKeduaIos("Lihat Foto Bukti", onPhoto, Modifier.height(42.dp), ikon = IkonIos.Image, warna = WarnaIos.AbuGelap)
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
        Column(Modifier.clip(UkuranIos.SudutKartu).background(WarnaIos.Kartu)) {
            Row(Modifier.padding(start = 16.dp, end = 10.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(preview.name, style = TipeIos.Utama, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        preview.outlet.ifBlank { "Outlet tidak tercatat" },
                        style = TipeIos.Catatan,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                TombolBundarIos(IkonIos.Close, "Tutup foto bukti", onTutup, warnaIkon = WarnaIos.LabelKedua)
            }
            SubcomposeAsyncImage(
                model = preview.photo,
                contentDescription = "Foto bukti waste ${preview.name}",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().height(340.dp).background(Color.Black),
                loading = {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(28.dp), color = Color.White, strokeWidth = 2.5.dp)
                    }
                },
                error = {
                    Box(Modifier.fillMaxSize().padding(24.dp), Alignment.Center) {
                        Text(
                            "Foto bukti tidak dapat dimuat.",
                            color = WarnaIos.Abu, fontSize = 13.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center,
                        )
                    }
                },
            )
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Jumlah waste", Modifier.weight(1f), style = TipeIos.SubJudul)
                Text(preview.quantityLabel, color = NadaIos.BAHAYA.teks, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Petak ringkasan-sekaligus-filter, bentuknya mengikuti petak "smart list" iOS
 * ([com.sukashawarma.superapp.core.ui.ios.PetakStatIos]) tapi tetap memuat keterangan
 * di bawah angka. [onClick] null berarti petak hanya informasi.
 */
@Composable
private fun RowScope.WasteSummaryFilterCard(
    title: String,
    value: String,
    caption: String,
    warna: Color,
    isSelected: Boolean,
    onClick: (() -> Unit)?,
    valueColor: Color = WarnaIos.Label,
) {
    val latar by animateColorAsState(if (isSelected) warna else WarnaIos.Kartu, label = "latarRingkasWaste")
    Column(
        Modifier
            .weight(1f)
            .permukaanIos(UkuranIos.SudutPetak, latar)
            .then(if (onClick != null) Modifier.tekanIos(onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(if (isSelected) Color.White else warna))
            Spacer(Modifier.width(6.dp))
            Text(
                title,
                color = if (isSelected) Color.White else WarnaIos.LabelKedua,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(value, style = TipeIos.AngkaBesar.copy(fontSize = 22.sp, color = if (isSelected) Color.White else valueColor), maxLines = 1)
        Text(
            caption,
            color = if (isSelected) Color.White.copy(alpha = 0.85f) else WarnaIos.LabelKedua,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            maxLines = 2,
        )
    }
}
