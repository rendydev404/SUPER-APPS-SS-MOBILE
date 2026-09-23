package com.sukashawarma.superapp.presentation.absensi.papan

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.sukashawarma.superapp.core.ui.ios.BilahJudulIos
import com.sukashawarma.superapp.core.ui.ios.KartuIos
import com.sukashawarma.superapp.core.ui.ios.KeadaanIos
import com.sukashawarma.superapp.core.ui.ios.KolomCariIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.PemisahIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.TombolKapsulIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.ios.permukaanIos
import com.sukashawarma.superapp.core.ui.ios.tekanIos
import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.domain.util.JakartaTime
import com.sukashawarma.superapp.presentation.absensi.AbsensiShell
import com.sukashawarma.superapp.core.ui.kaca.denganRuangNav
import com.sukashawarma.superapp.presentation.absensi.rekap.selfiePublicUrl
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.RealtimeTables
import com.sukashawarma.superapp.core.ui.SukaDropdownHeader
import com.sukashawarma.superapp.core.ui.SukaDropdownMenu
import com.sukashawarma.superapp.core.ui.SukaDropdownMenuItem

// Warna status — palet sistem iOS; teks memakai varian gelap supaya terbaca di atas latar tipisnya.
private val TeksKuning = Color(0xFF946200)
private val TeksIndigo = Color(0xFF3634A3)

private val ID_LOCALE = Locale("id", "ID")
private val DateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", ID_LOCALE)

/** (titik, teks) per status — dipakai bareng oleh pil, dot avatar, dan legenda. */
private fun stateColors(state: BoardState): Pair<Color, Color> = when (state) {
    BoardState.MASUK, BoardState.KELUAR -> WarnaIos.Hijau to NadaIos.SUKSES.teks
    BoardState.TELAT_TOLERANSI -> WarnaIos.Kuning to TeksKuning
    BoardState.TELAT, BoardState.PULANG_TELAT, BoardState.LEBIH_AWAL -> WarnaIos.Oranye to NadaIos.PERINGATAN.teks
    BoardState.ALPHA -> WarnaIos.Merah to NadaIos.BAHAYA.teks
    BoardState.BELUM -> WarnaIos.Abu to NadaIos.NETRAL.teks
}

private fun stateIcon(state: BoardState): ImageVector = when (state) {
    BoardState.MASUK -> Icons.AutoMirrored.Filled.Login
    BoardState.TELAT, BoardState.TELAT_TOLERANSI, BoardState.PULANG_TELAT -> IkonIos.Schedule
    BoardState.KELUAR, BoardState.LEBIH_AWAL -> Icons.AutoMirrored.Filled.Logout
    BoardState.BELUM, BoardState.ALPHA -> IkonIos.MoreHoriz
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PapanKehadiranScreen(
    onExit: () -> Unit,
    onNavigateTab: (Int) -> Unit = {},
    viewModel: PapanKehadiranViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    RealtimeRefresh(RealtimeTables.ATTENDANCE) { viewModel.refresh() }
    var previewUrl by remember { mutableStateOf<String?>(null) }

    // Papan Kehadiran diakses dari tab "More" (index 3) — bilah tab tetap tampil supaya
    // user bisa lompat tab tanpa balik dulu, sama seperti Cuti & Kasbon.
    AbsensiShell(selectedIndex = 3, onSelect = onNavigateTab) {
        Scaffold(
            containerColor = WarnaIos.Latar,
            topBar = { BilahJudulIos(judul = "Papan Kehadiran", onKembali = onExit, garisBawah = false) },
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = UkuranIos.TepiLayar, end = UkuranIos.TepiLayar, top = 4.dp, bottom = 32.dp).denganRuangNav(),
                verticalArrangement = Arrangement.spacedBy(UkuranIos.JarakKartu),
            ) {
                item(key = "header") {
                    PapanHeader(
                        outletName = state.selectedOutletName,
                        lastRefresh = state.lastRefresh,
                        refreshing = state.refreshing,
                        showRefresh = state.selectedOutletId != null,
                        onRefresh = { viewModel.refresh() },
                    )
                }

                if (state.canChooseOutlet) {
                    item(key = "outlet_picker") {
                        OutletPickerCard(
                            outlets = state.outlets,
                            loading = state.loadingOutlets,
                            selectedId = state.selectedOutletId,
                            onSelect = { viewModel.selectOutlet(it) },
                        )
                    }
                }

                if (state.awaitingOutletChoice) {
                    item(key = "choose_outlet") {
                        KeadaanIos(
                            ikon = IkonIos.Storefront,
                            judul = "Pilih Outlet Dulu",
                            pesan = "Anda memantau seluruh outlet. Pilih salah satu di atas untuk melihat papan kehadirannya.",
                            nada = NadaIos.AKSEN,
                        )
                    }
                    return@LazyColumn
                }

                when {
                    state.loading -> item(key = "loading") {
                        Column(
                            Modifier.fillMaxWidth().padding(vertical = 60.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            CircularProgressIndicator(color = WarnaIos.Aksen, strokeWidth = 2.5.dp, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.height(14.dp))
                            Text("Memuat data kehadiran...", style = TipeIos.Catatan)
                        }
                    }

                    state.error != null -> item(key = "error") {
                        PapanErrorCard(message = state.error.orEmpty(), onRetry = { viewModel.load() })
                    }

                    else -> {
                        if (state.canSeeAlerts && state.alerts.isNotEmpty()) {
                            item(key = "alerts") { SecurityAlertCard(state.alerts) }
                        }

                        item(key = "summary") { AttendanceRateCard(state.summary) }

                        item(key = "list_header") {
                            ListHeader(
                                shown = state.filteredRows.size,
                                total = state.summary.total,
                                filter = state.filter,
                                onFilter = { viewModel.setFilter(it) },
                            )
                        }

                        item(key = "search") {
                            StaffSearchField(query = state.query, onQueryChange = { viewModel.setQuery(it) })
                        }

                        if (state.filteredRows.isEmpty()) {
                            item(key = "empty") {
                                KeadaanIos(
                                    ikon = IkonIos.Groups,
                                    judul = "Tidak Ada Data Staf",
                                    pesan = if (state.query.isBlank()) "Tidak ada staf yang sesuai dengan filter yang dipilih."
                                    else "Tidak ada staf bernama \"${state.query.trim()}\" pada filter ini.",
                                )
                            }
                        } else {
                            items(state.filteredRows, key = { it.staffId }) { row ->
                                StaffBoardCard(row = row, onPreview = { previewUrl = it })
                            }
                        }
                    }
                }
            }
        }
    }

    previewUrl?.let { url ->
        PhotoPreviewDialog(url = url, onDismiss = { previewUrl = null })
    }
}

/* ---------------------------------------------------------------- Header */

@Composable
private fun PapanHeader(
    outletName: String?,
    lastRefresh: String,
    refreshing: Boolean,
    showRefresh: Boolean,
    onRefresh: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                LocalDate.now(JakartaTime.ZONE).format(DateFormatter),
                style = TipeIos.Judul2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                outletName ?: "Pantau kehadiran tim hari ini",
                style = TipeIos.SubJudul,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (showRefresh) {
            Spacer(Modifier.width(8.dp))
            // Kapsul abu ala tombol toolbar iOS: ikon segarkan + jam muat terakhir.
            Row(
                modifier = Modifier
                    .height(34.dp)
                    .clip(UkuranIos.SudutKapsul)
                    .background(WarnaIos.Isian)
                    .tekanIos(onRefresh, aktif = !refreshing)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (refreshing) {
                    CircularProgressIndicator(color = WarnaIos.Aksen, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                } else {
                    Icon(IkonIos.Refresh, contentDescription = "Muat ulang", tint = WarnaIos.Aksen, modifier = Modifier.size(15.dp))
                }
                if (lastRefresh.isNotBlank()) {
                    Text(lastRefresh, color = WarnaIos.Aksen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/* --------------------------------------------------------- Security alert */

@Composable
private fun SecurityAlertCard(alerts: List<SecurityAlert>) {
    Column(Modifier.fillMaxWidth().permukaanIos(UkuranIos.SudutKartu)) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier.size(30.dp).clip(CircleShape).background(WarnaIos.Merah.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(IkonIos.WarningAmber, contentDescription = null, tint = WarnaIos.Merah, modifier = Modifier.size(17.dp))
            }
            Text(
                "Peringatan Keamanan: ${alerts.size} percobaan manipulasi lokasi",
                style = TipeIos.Keterangan.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = NadaIos.BAHAYA.teks),
            )
        }
        alerts.forEach { alert ->
            PemisahIos()
            Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        alert.staffName,
                        style = TipeIos.Keterangan.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(alert.time, style = TipeIos.Kecil)
                }
                Spacer(Modifier.height(2.dp))
                Text(alert.label, style = TipeIos.Catatan.copy(color = NadaIos.BAHAYA.teks, fontWeight = FontWeight.Medium))
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

/* -------------------------------------------------------- Attendance rate */

@Composable
private fun AttendanceRateCard(summary: BoardSummary) {
    val anim by animateFloatAsState(
        targetValue = if (summary.total > 0) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "papan-bar",
    )

    KartuIos {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Tingkat Kehadiran", style = TipeIos.Utama)
                Text("Persentase staf yang sudah hadir hari ini", style = TipeIos.Catatan)
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${summary.percent}", style = TipeIos.JudulBesar.copy(fontSize = 32.sp))
                Text("%", style = TipeIos.SubJudul.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.padding(bottom = 5.dp))
            }
        }

        Spacer(Modifier.height(12.dp))

        // Bar bertumpuk: hadir → telat toleransi → telat → alpha, sisanya track kosong.
        val segments = listOf(
            summary.fraction(summary.hadir) * anim to WarnaIos.Hijau,
            summary.fraction(summary.telatToleransi) * anim to WarnaIos.Kuning,
            summary.fraction(summary.telat) * anim to WarnaIos.Oranye,
            summary.fraction(summary.alpha) * anim to WarnaIos.Merah,
        )
        Row(
            Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(CircleShape)
                .background(WarnaIos.Isian),
        ) {
            var used = 0f
            segments.forEach { (fraction, color) ->
                if (fraction > 0.001f) {
                    used += fraction
                    Box(Modifier.fillMaxHeight().weight(fraction).background(color))
                }
            }
            val rest = 1f - used
            if (rest > 0.001f) Spacer(Modifier.weight(rest))
        }

        Spacer(Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LegendTile(Modifier.weight(1f), "Hadir", summary.hadir, WarnaIos.Hijau)
            LegendTile(Modifier.weight(1f), "Telat (Tol)", summary.telatToleransi, WarnaIos.Kuning)
            LegendTile(Modifier.weight(1f), "Telat", summary.telat, WarnaIos.Oranye)
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LegendTile(Modifier.weight(1f), "Belum", summary.belum, WarnaIos.Abu)
            LegendTile(Modifier.weight(1f), "Alpha", summary.alpha, WarnaIos.Merah)
            LegendTile(Modifier.weight(1f), "Total Staf", summary.total, WarnaIos.Indigo)
        }
    }
}

/** Petak legenda abu ala blok angka iOS: titik warna, angka, label. */
@Composable
private fun LegendTile(modifier: Modifier, label: String, value: Int, dot: Color) {
    Column(
        modifier
            .clip(UkuranIos.SudutBlok)
            .background(WarnaIos.Latar)
            .padding(horizontal = 10.dp, vertical = 9.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
            Text("$value", style = TipeIos.Angka.copy(fontSize = 18.sp))
        }
        Spacer(Modifier.height(2.dp))
        Text(label, style = TipeIos.Kecil.copy(fontWeight = FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/* ------------------------------------------------------------ List header */

@Composable
private fun ListHeader(shown: Int, total: Int, filter: BoardState?, onFilter: (BoardState?) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Daftar Staf", style = TipeIos.Judul3.copy(fontWeight = FontWeight.Bold))
            Text("$shown dari $total staf", style = TipeIos.Catatan)
        }
        StatusFilterMenu(selected = filter, onSelect = onFilter)
    }
}

@Composable
private fun StaffSearchField(query: String, onQueryChange: (String) -> Unit) {
    KolomCariIos(
        nilai = query,
        onUbah = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = "Cari nama staf",
    )
}

@Composable
private fun StatusFilterMenu(selected: BoardState?, onSelect: (BoardState?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        TombolKapsulIos(
            teks = selected?.filterLabel ?: "Semua Status",
            onKlik = { expanded = true },
            ikon = IkonIos.Tune,
            chevron = true,
        )
        SukaDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SukaDropdownHeader(title = "STATUS KEHADIRAN", onClose = { expanded = false })
            SukaDropdownMenuItem(
                text = "Semua Status",
                selected = (selected == null),
                onClick = { onSelect(null); expanded = false },
            )
            BoardState.entries.forEach { boardState ->
                val (dot, _) = stateColors(boardState)
                SukaDropdownMenuItem(
                    text = boardState.filterLabel,
                    selected = (boardState == selected),
                    leading = { Box(Modifier.size(9.dp).clip(CircleShape).background(dot)) },
                    onClick = { onSelect(boardState); expanded = false },
                )
            }
        }
    }
}

/* -------------------------------------------------------------- Staff row */

@Composable
private fun StaffBoardCard(row: StaffBoardRow, onPreview: (String) -> Unit) {
    val (dot, fg) = stateColors(row.state)
    val url = remember(row.selfiePath) { selfiePublicUrl(row.selfiePath) }

    KartuIos(padding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(WarnaIos.Aksen.copy(alpha = 0.14f))
                        .then(if (url != null) Modifier.clickable { onPreview(url) } else Modifier),
                    contentAlignment = Alignment.Center,
                ) {
                    if (url != null) {
                        AsyncImage(
                            model = url,
                            contentDescription = "Selfie ${row.name}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Text(
                            row.name.trim().take(1).uppercase(ID_LOCALE).ifBlank { "?" },
                            color = NadaIos.AKSEN.teks,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                        )
                    }
                }
                // Titik status di pojok avatar — penanda cepat sebelum membaca pil.
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp)
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(WarnaIos.Kartu)
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(dot)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        row.name,
                        style = TipeIos.Utama.copy(fontSize = 16.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (row.isManual) {
                        Text(
                            "Manual",
                            color = NadaIos.PERINGATAN.teks,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(UkuranIos.SudutKapsul)
                                .background(WarnaIos.Oranye.copy(alpha = 0.14f))
                                .padding(horizontal = 7.dp, vertical = 2.dp),
                        )
                    }
                }
                Text(row.roleLabel, style = TipeIos.Catatan, maxLines = 1)
                Spacer(Modifier.height(7.dp))
                // Lencana status gaya LencanaIos, tetapi berwarna per status (termasuk kuning
                // "telat toleransi" yang tidak punya padanan NadaIos).
                Row(
                    modifier = Modifier
                        .clip(UkuranIos.SudutKapsul)
                        .background(dot.copy(alpha = 0.16f))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(stateIcon(row.state), contentDescription = null, tint = fg, modifier = Modifier.size(13.dp))
                    Text(
                        boardPillLabel(row.state, row.time, row.delayMinutes),
                        color = fg,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/* ---------------------------------------------------------- Outlet picker */

@Composable
private fun OutletPickerCard(
    outlets: List<PapanOutletOption>,
    loading: Boolean,
    selectedId: String?,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val selected = outlets.find { it.id == selectedId }
    val filtered = outlets.filter { it.name.contains(query.trim(), ignoreCase = true) }

    LaunchedEffect(expanded) { if (!expanded) query = "" }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .permukaanIos(UkuranIos.SudutGrup)
                .then(
                    // Belum ada outlet terpilih: garis aksen tipis sebagai ajakan memilih.
                    if (selected == null) Modifier.border(1.dp, WarnaIos.Aksen.copy(alpha = 0.45f), UkuranIos.SudutGrup)
                    else Modifier
                )
                .clickable(enabled = !loading && outlets.isNotEmpty()) { expanded = true }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.size(30.dp).clip(CircleShape).background(WarnaIos.Aksen.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(IkonIos.Storefront, contentDescription = null, tint = WarnaIos.Aksen, modifier = Modifier.size(17.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = when {
                        loading -> "Memuat outlet..."
                        selected != null -> selected.name
                        else -> "Pilih outlet"
                    },
                    style = TipeIos.Isi.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (outlets.isEmpty() && !loading) "Tidak ada outlet yang bisa dilihat" else "Ketuk untuk ganti outlet",
                    style = TipeIos.Catatan,
                    maxLines = 1,
                )
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
                    nilai = query,
                    onUbah = { query = it },
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
                    if (filtered.isEmpty()) {
                        Text(
                            "Outlet tidak ditemukan",
                            style = TipeIos.Catatan,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 18.dp),
                        )
                    } else filtered.forEach { outlet ->
                        SukaDropdownMenuItem(
                            title = outlet.name,
                            selected = outlet.id == selectedId,
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
}

/* --------------------------------------------------------- Preview/states */

@Composable
private fun PhotoPreviewDialog(url: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(UkuranIos.SudutKartu)
                .background(Color.Black)
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = url,
                contentDescription = "Selfie ukuran penuh",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

@Composable
private fun PapanErrorCard(message: String, onRetry: () -> Unit) {
    val noOutlet = message.contains("cabang", ignoreCase = true)
    if (noOutlet) {
        KeadaanIos(
            ikon = IkonIos.Storefront,
            judul = "Cabang Belum Ditentukan",
            pesan = message,
            nada = NadaIos.AKSEN,
        )
    } else {
        KeadaanIos(
            ikon = IkonIos.ErrorOutline,
            judul = "Gagal memuat",
            pesan = message,
            nada = NadaIos.BAHAYA,
            teksAksi = "Coba Lagi",
            onAksi = onRetry,
        )
    }
}
