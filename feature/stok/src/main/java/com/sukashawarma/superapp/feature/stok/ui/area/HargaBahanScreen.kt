package com.sukashawarma.superapp.feature.stok.ui.area

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.feature.stok.data.*
import com.sukashawarma.superapp.feature.stok.domain.*
import com.sukashawarma.superapp.feature.stok.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.Locale
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.RealtimeTables

private fun money(value: Double?) = value?.let(::formatRupiah) ?: "—"
private fun percent(value: Double?) = value?.let { String.format(Locale("id", "ID"), "%+.1f%%", it * 100) } ?: "Pembelian Pertama"


/**
 * Rona kartu mengikuti arah harga, bukan kategori: pengelola area membuka layar ini
 * untuk mencari kenaikan, jadi warnanya harus menjawab itu dari satu kali lirik.
 */
private data class PriceTone(val strip: Color, val line: Color, val chip: Color, val chipBorder: Color, val text: Color, val dot: Color)

private fun tone(row: MaterialPrice): PriceTone = when {
    row.latest == null -> PriceTone(Slate50, Slate200, Slate100, Slate200, Slate700, Slate400)
    row.percent == null -> PriceTone(Amber50, Amber100, Amber100, Amber300, Amber800, Amber500)
    row.percent!! > 0 -> PriceTone(Rose50, Rose100, Rose100, Rose200, Rose700, Rose500)
    row.percent!! < 0 -> PriceTone(Emerald50, Emerald100, Emerald100, Emerald200, Emerald700, Emerald500)
    else -> PriceTone(Slate50, Slate200, Slate100, Slate200, Slate700, Slate400)
}

private fun statusLabel(row: MaterialPrice): String = when {
    row.latest == null -> "Harga Master"
    row.percent == null -> "Pembelian Pertama"
    row.percent!! > 0 -> "${percent(row.percent)} Naik"
    row.percent!! < 0 -> "${percent(row.percent)} Turun"
    else -> "Stabil"
}

private data class CategoryTone(val dot: Color, val bg: Color, val border: Color, val text: Color, val badge: Color)

private val categoryTones = listOf(
    CategoryTone(Color(0xFF7C3AED), Color(0xFFF5F3FF), Color(0xFFE0E7FF), Color(0xFF2E1065), Color(0xFFDDD6FE)),
    CategoryTone(Color(0xFF2563EB), Color(0xFFEFF6FF), Color(0xFFDBEAFE), Color(0xFF172554), Color(0xFFBFDBFE)),
    CategoryTone(Color(0xFF059669), Color(0xFFECFDF5), Color(0xFFD1FAE5), Color(0xFF064E3B), Color(0xFFA7F3D0)),
    CategoryTone(Color(0xFFD97706), Color(0xFFFFFBEB), Color(0xFFFEF3C7), Color(0xFF451A03), Color(0xFFFDE68A)),
    CategoryTone(Color(0xFFE11D48), Color(0xFFFFF1F2), Color(0xFFFFE4E6), Color(0xFF4C0519), Color(0xFFFECDD3)),
    CategoryTone(Color(0xFF0891B2), Color(0xFFECFEFF), Color(0xFFCFFAFE), Color(0xFF083344), Color(0xFFA5F3FC)),
)

@Composable
private fun Choice(label: String, options: List<String>, active: Boolean = false, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(10.dp),
            color = if (active) Orange50 else Color.White,
            border = BorderStroke(1.dp, if (active) Orange200 else Slate200),
        ) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(label, color = if (active) Orange600 else Slate700, fontSize = 12.sp, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.ExpandMore, null, tint = if (active) Orange600 else Slate400, modifier = Modifier.size(14.dp))
            }
        }
        DropdownMenu(expanded, { expanded = false }) {
            options.forEachIndexed { i, name -> DropdownMenuItem(text = { Text(name) }, onClick = { expanded = false; onSelect(i) }) }
        }
    }
}

@Composable
private fun StatusPill(label: String, count: Int, selected: Boolean, dot: Color?, border: Color, text: Color, badge: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) Orange500 else Color.White,
        border = BorderStroke(1.dp, if (selected) Orange500 else border),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (dot != null && !selected) {
                Box(Modifier.size(8.dp).background(dot, CircleShape))
                Spacer(Modifier.width(6.dp))
            }
            Text(label, color = if (selected) Color.White else text, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold)
            Spacer(Modifier.width(6.dp))
            Box(
                Modifier.background(if (selected) Color.White.copy(alpha = 0.25f) else badge, CircleShape)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text("$count", color = if (selected) Color.White else text, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}


@Composable
private fun KpiStrip(total: Int, naik: Int, turun: Int, stabil: Int) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(1.dp, Slate200)) {
        Row(Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            KpiCell("Total Item", "$total", "Semua bahan aktif", Color(0xFF312E81), Slate900, Slate400)
            Box(Modifier.height(38.dp).width(1.dp).background(Slate100))
            KpiCell("Harga Naik", "$naik", "Perlu mitigasi", Color(0xFF881337), Rose600, Rose500)
            Box(Modifier.height(38.dp).width(1.dp).background(Slate100))
            KpiCell("Harga Turun", "$turun", "Hemat belanja", Color(0xFF064E3B), Emerald700, Emerald600)
            Box(Modifier.height(38.dp).width(1.dp).background(Slate100))
            KpiCell("Stabil", "$stabil", "Tetap konsisten", Slate700, Slate700, Slate400)
        }
    }
}


@Composable
private fun MaterialPriceCard(row: MaterialPrice, onHistory: () -> Unit) {
    val tone = tone(row)
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(1.dp, Slate200)) {
        Column {
            Row(
                Modifier.fillMaxWidth().background(tone.strip).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(50), color = tone.chip, border = BorderStroke(1.dp, tone.chipBorder)) {
                        Row(Modifier.padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(6.dp).background(tone.dot, CircleShape))
                            Spacer(Modifier.width(4.dp))
                            Text(statusLabel(row), color = tone.text, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.width(6.dp))
                    MiniBadge(row.category.uppercase(), Slate100, Slate500)
                    Spacer(Modifier.width(4.dp))
                    MiniBadge(row.unit, Slate100, Slate500)
                }
                Text(row.latest?.let { tanggalSingkat(it.date) } ?: "-", color = Slate400, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(row.name, color = Slate900, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 19.sp)
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = Slate50, border = BorderStroke(1.dp, Slate100)) {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("HARGA BELI TERAKHIR", color = Slate400, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                            Spacer(Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(money(row.effective), color = Slate900, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                                Text(" / ${row.unit}", color = Slate500, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                            }
                            if (row.latest == null && row.master != null) {
                                Text("Dari harga master", color = Slate400, fontSize = 9.sp)
                            }
                        }
                        Box(Modifier.height(38.dp).width(1.dp).background(Slate200))
                        Column(Modifier.weight(1f).padding(start = 10.dp), horizontalAlignment = Alignment.End) {
                            Text("SEBELUMNYA", color = Slate400, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                            Spacer(Modifier.height(2.dp))
                            val previous = row.previous
                            if (previous == null) {
                                Text("— (Belum ada data)", color = Slate400, fontSize = 11.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.End)
                            } else {
                                Text(money(previous.price), color = Slate700, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                row.delta?.let { delta ->
                                    Text(
                                        "(${if (delta > 0) "+" else ""}${formatRupiah(delta)})",
                                        color = if (delta > 0) Rose600 else if (delta < 0) Emerald600 else Slate500,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = Slate50, border = BorderStroke(1.dp, Slate100)) {
                    Column(Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Vendor:", color = Slate400, fontSize = 11.sp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                row.latest?.vendor?.takeIf { it.isNotBlank() } ?: "Belum ada PO",
                                color = Slate700, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Box(Modifier.fillMaxWidth().height(1.dp).background(Slate200))
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Nomor PO", color = Slate400, fontSize = 10.sp)
                            Text(
                                row.latest?.number?.takeIf { it.isNotBlank() } ?: "-",
                                color = Slate700, fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                Surface(
                    onClick = onHistory,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Orange50,
                    border = BorderStroke(1.dp, Orange200),
                ) {
                    Row(Modifier.padding(vertical = 10.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BarChart, null, tint = Orange600, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Riwayat Harga", color = Orange600, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryHeader(name: String, count: Int, closed: Boolean, tone: CategoryTone, onToggle: () -> Unit) {
    Surface(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = tone.bg,
        border = BorderStroke(1.dp, tone.border),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(tone.dot, CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(
                name.uppercase(), color = tone.text, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                letterSpacing = 0.4.sp, modifier = Modifier.weight(1f, fill = false),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(8.dp))
            Box(Modifier.background(tone.badge, CircleShape).padding(horizontal = 8.dp, vertical = 2.dp)) {
                Text("$count item", color = tone.dot, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.weight(1f))
            Text(if (closed) "Buka" else "Tutup", color = tone.dot, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Icon(
                if (closed) Icons.Default.ExpandMore else Icons.Default.ExpandLess, null,
                tint = tone.dot, modifier = Modifier.size(16.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HargaBahanScreen(onBack: () -> Unit, vm: HargaBahanViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    RealtimeRefresh(RealtimeTables.BAHAN_BAKU_HARGA, RealtimeTables.PURCHASE_ORDER, RealtimeTables.BAHAN_BAKU) { vm.refresh() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(vm, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            vm.refresh()
            awaitCancellation()
        }
    }
    var search by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("Semua Kategori") }
    var status by rememberSaveable { mutableStateOf("all") }
    var sort by rememberSaveable { mutableStateOf(3) }
    var ascending by rememberSaveable { mutableStateOf(false) }
    var grouped by rememberSaveable { mutableStateOf(true) }
    var closed by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var exportText by rememberSaveable { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) scope.launch {
            try {
                withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri)?.use { it.write(exportText.toByteArray(Charsets.UTF_8)) } ?: error("File tidak dapat dibuka") }
                message = "CSV berhasil disimpan"
            } catch (_: Exception) { message = "Gagal menyimpan CSV. Silakan coba lagi." }
        }
    }
    val categories = remember(state.items) { listOf("Semua Kategori") + state.items.map { it.category }.distinct().sorted() }
    val filtered = remember(state.items, search, category, status, sort, ascending) {
        val rows = state.items.filter { row ->
            (category == "Semua Kategori" || row.category == category) && row.matchesStatus(status) &&
                (search.isBlank() || listOf(row.name, row.latest?.vendor.orEmpty(), row.latest?.number.orEmpty()).any { it.contains(search.trim(), true) })
        }
        val comparator = when (sort) {
            0 -> compareBy<MaterialPrice> { it.name.lowercase() }
            1 -> compareBy { it.latest?.date.orEmpty() }
            2 -> compareBy { it.effective ?: 0.0 }
            else -> compareBy { it.percent ?: -999.0 }
        }
        rows.sortedWith(if (ascending) comparator else comparator.reversed())
    }
    val groups = remember(filtered, grouped) { if (grouped) filtered.groupBy { it.category }.toSortedMap() else mapOf("" to filtered) }
    Column(Modifier.fillMaxSize().background(Slate50)) {
        HeaderStok("Master Harga Bahan Baku", "Pantau pergerakan harga beli dari vendor", onBack) {
            IconButton(onClick = { vm.refresh() }, enabled = !state.loading) { Icon(Icons.Default.Refresh, "Perbarui data", tint = Color.White) }
            IconButton(
                onClick = { exportText = priceCsv(filtered); export.launch("laporan_harga_bahan_baku_${LocalDate.now()}.csv") },
                enabled = filtered.isNotEmpty(),
            ) { Icon(Icons.Default.Download, "Unduh rekap", tint = if (filtered.isEmpty()) Color(0x66FFFFFF) else Color.White) }
        }
        message?.let { PitaPesan(it, false) { message = null } }
        when {
            state.loading && state.items.isEmpty() -> MemuatPenuh()
            state.error != null -> KeadaanGagal(state.error!!, { vm.refresh() })
            else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatusPill("Semua", state.items.size, status == "all", null, Slate200, Slate700, Slate100) { status = "all" }
                        StatusPill("Harga Naik", state.items.count { it.matchesStatus("naik") }, status == "naik", Rose500, Rose200, Rose700, Rose100) { status = "naik" }
                        StatusPill("Harga Turun", state.items.count { it.matchesStatus("turun") }, status == "turun", Emerald500, Emerald200, Emerald700, Emerald100) { status = "turun" }
                        StatusPill("Stabil", state.items.count { it.matchesStatus("stabil") }, status == "stabil", Slate400, Slate200, Slate700, Slate100) { status = "stabil" }
                    }
                }
                item {
                    KpiStrip(
                        state.items.size,
                        state.items.count { it.matchesStatus("naik") },
                        state.items.count { it.matchesStatus("turun") },
                        state.items.count { it.matchesStatus("stabil") },
                    )
                }
                item {
                    OutlinedTextField(
                        search, { search = it }, Modifier.fillMaxWidth(),
                        placeholder = { Text("Cari bahan baku, vendor, atau nomor PO...", fontSize = 12.sp, color = Slate400) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = Slate400, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
                            focusedBorderColor = Orange500, unfocusedBorderColor = Slate200,
                        ),
                    )
                }
                item {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Choice(category, categories, category != "Semua Kategori") { category = categories[it] }
                        val days = listOf(7, 30, 90, null)
                        Choice(state.days?.let { "$it Hari Terakhir" } ?: "Semua Riwayat", listOf("7 Hari Terakhir", "30 Hari Terakhir", "90 Hari Terakhir", "Semua Riwayat")) { vm.refresh(days[it]) }
                        val fields = listOf("Nama Bahan Baku", "Tanggal PO / Vendor Terakhir", "Harga Terakhir", "Perubahan Harga")
                        Choice("Urut: ${fields[sort]}", fields) { sort = it; ascending = false }
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            onClick = { ascending = !ascending },
                            shape = RoundedCornerShape(10.dp), color = Slate100,
                        ) {
                            Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(if (ascending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward, null, tint = Slate700, modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (ascending) "Menaik" else "Menurun", color = Slate700, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            onClick = { grouped = !grouped },
                            shape = RoundedCornerShape(10.dp), color = if (grouped) Orange50 else Slate100,
                            border = if (grouped) BorderStroke(1.dp, Orange200) else null,
                        ) {
                            Text(
                                "Per Kategori",
                                Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                color = if (grouped) Orange600 else Slate700, fontSize = 11.sp, fontWeight = FontWeight.Medium,
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        if (grouped) Text(
                            if (closed.isEmpty()) "Tutup Semua" else "Buka Semua",
                            Modifier.clickable { closed = if (closed.isEmpty()) categories else emptyList() },
                            color = Orange600, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                item {
                    Text(
                        "Menampilkan ${filtered.size} bahan baku dalam ${groups.keys.count { it.isNotBlank() }} kategori",
                        color = Slate500, fontSize = 11.sp,
                    )
                }
                if (filtered.isEmpty()) item { KeadaanKosong("Tidak ada bahan baku yang sesuai filter.") }
                groups.entries.forEachIndexed { index, (name, rows) ->
                    if (grouped) item(key = "category:$name") {
                        CategoryHeader(name, rows.size, name in closed, categoryTones[index % categoryTones.size]) {
                            closed = if (name in closed) closed - name else closed + name
                        }
                    }
                    if (!grouped || name !in closed) items(rows, key = { it.id }) { row ->
                        MaterialPriceCard(row) { vm.detail(row) }
                    }
                }
            }
        }
    }
    state.detail?.let { detail ->
        ModalBottomSheet(
            onDismissRequest = { vm.detail(null) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Slate50,
        ) {
            LazyColumn(
                Modifier.fillMaxWidth().fillMaxHeight(0.92f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MiniBadge(detail.category.uppercase(), Slate100, Slate500)
                            Spacer(Modifier.width(4.dp))
                            MiniBadge(detail.unit, Slate100, Slate500)
                            Spacer(Modifier.weight(1f))
                            Surface(
                                onClick = { vm.detail(null) },
                                shape = CircleShape, color = Slate100,
                            ) { Icon(Icons.Default.Close, "Tutup", tint = Slate500, modifier = Modifier.padding(6.dp).size(16.dp)) }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(detail.name, color = Slate900, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 24.sp)
                        Text("Riwayat harga beli dari vendor", color = Slate500, fontSize = 11.sp)
                    }
                }
                item {
                    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(1.dp, Slate200)) {
                        Row(Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            KpiCell("Harga Master", money(detail.master), "Acuan sistem", Slate700, Slate900, Slate400)
                            Box(Modifier.height(38.dp).width(1.dp).background(Slate100))
                            KpiCell("PO Terakhir", money(detail.latest?.price), "Harga beli", Color(0xFF7C2D12), Orange600, Slate400)
                            Box(Modifier.height(38.dp).width(1.dp).background(Slate100))
                            KpiCell("Transaksi", "${state.history.size}", "PO tercatat", Slate700, Slate900, Slate400)
                        }
                    }
                }
                when {
                    state.historyLoading -> item {
                        Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Orange500)
                        }
                    }
                    state.historyError != null -> item { KeadaanGagal(state.historyError!!, { vm.detail(detail) }) }
                    state.history.isEmpty() -> item { KeadaanKosong("Belum ada riwayat transaksi pembelian PO yang tercatat.") }
                    else -> {
                        item { PriceChartCard(state.history.reversed()) }
                        item {
                            Text(
                                "RIWAYAT PEMBELIAN",
                                color = Slate400, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp,
                            )
                        }
                        itemsIndexed(state.history, key = { _, row -> row.id }) { index, row ->
                            val previous = state.history.getOrNull(index + 1)
                            val diff = previous?.price?.takeIf { it > 0 }?.let { (row.price - it) / it }
                            HistoryCard(row, diff, detail.unit)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(row: PricePurchase, diff: Double?, unit: String) {
    val strip: Color; val chip: Color; val chipBorder: Color; val text: Color; val dot: Color
    when {
        diff == null -> { strip = Amber50; chip = Amber100; chipBorder = Amber300; text = Amber800; dot = Amber500 }
        diff > 0 -> { strip = Rose50; chip = Rose100; chipBorder = Rose200; text = Rose700; dot = Rose500 }
        diff < 0 -> { strip = Emerald50; chip = Emerald100; chipBorder = Emerald200; text = Emerald700; dot = Emerald500 }
        else -> { strip = Slate50; chip = Slate100; chipBorder = Slate200; text = Slate700; dot = Slate400 }
    }
    val label = when {
        diff == null -> "Pembelian Pertama"
        diff > 0 -> "${percent(diff)} Naik"
        diff < 0 -> "${percent(diff)} Turun"
        else -> "Harga Tetap"
    }
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(1.dp, Slate200)) {
        Column {
            Row(
                Modifier.fillMaxWidth().background(strip).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(shape = RoundedCornerShape(50), color = chip, border = BorderStroke(1.dp, chipBorder)) {
                    Row(Modifier.padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).background(dot, CircleShape))
                        Spacer(Modifier.width(4.dp))
                        Text(label, color = text, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.weight(1f))
                Text(tanggalSingkat(row.date), color = Slate400, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(money(row.price), color = Slate900, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                        Text(" / $unit", color = Slate500, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                    }
                    Text(
                        row.vendor.takeIf { it.isNotBlank() } ?: "Vendor tidak tercatat",
                        color = Slate700, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = Slate50, border = BorderStroke(1.dp, Slate100)) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        HistoryMetaRow("Nomor PO", row.number.takeIf { it.isNotBlank() } ?: "-")
                        HistoryMetaRow("Kuantitas", "${formatAngkaStok(row.qty)} $unit")
                        Box(Modifier.fillMaxWidth().height(1.dp).background(Slate200))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total belanja", color = Slate500, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            Text(money(row.subtotal), color = Slate900, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryMetaRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Slate400, fontSize = 10.sp)
        Text(value, color = Slate700, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * Satu titik harga tidak boleh tampil sebagai kanvas kosong — kasus paling umum di
 * layar ini adalah bahan yang baru sekali dibeli. Rentang datar digambar sebagai
 * garis mendatar di tengah, bukan dibagi nol.
 */
@Composable
private fun PriceChartCard(rows: List<PricePurchase>) {
    val min = rows.minOf { it.price }
    val max = rows.maxOf { it.price }
    val flat = max - min < 0.01
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(1.dp, Slate200)) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Pergerakan Harga Beli", color = Slate900, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        if (flat) "Tetap di ${money(min)}" else "${money(min)} — ${money(max)}",
                        color = Slate500, fontSize = 11.sp,
                    )
                }
                MiniBadge("${rows.size} transaksi", Orange50, Orange600, Orange200)
            }
            Spacer(Modifier.height(12.dp))
            Canvas(Modifier.fillMaxWidth().height(150.dp)) {
                val span = (max - min).takeIf { it > 0.01 } ?: 1.0
                val top = 8f
                val usable = size.height - top - 8f
                val points = rows.mapIndexed { index, row ->
                    val x = if (rows.size == 1) size.width / 2 else index * size.width / (rows.size - 1)
                    val y = if (flat) top + usable / 2 else top + usable - ((row.price - min) / span * usable).toFloat()
                    Offset(x, y)
                }
                repeat(4) { line ->
                    val y = top + usable * line / 3
                    drawLine(Slate100, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                }
                if (points.size > 1) {
                    val area = androidx.compose.ui.graphics.Path().apply {
                        moveTo(points.first().x, size.height)
                        points.forEach { lineTo(it.x, it.y) }
                        lineTo(points.last().x, size.height)
                        close()
                    }
                    drawPath(area, Brush.verticalGradient(listOf(Orange500.copy(alpha = 0.22f), Color.Transparent)))
                    points.zipWithNext().forEach { (a, b) -> drawLine(Orange500, a, b, 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round) }
                } else {
                    val y = points.first().y
                    drawLine(Orange500.copy(alpha = 0.35f), Offset(0f, y), Offset(size.width, y), 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                }
                points.forEach {
                    drawCircle(Color.White, 5.5.dp.toPx(), it)
                    drawCircle(Orange600, 4.dp.toPx(), it, style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(tanggalSingkat(rows.first().date), color = Slate400, fontSize = 10.sp)
                if (rows.size > 1) Text(tanggalSingkat(rows.last().date), color = Slate400, fontSize = 10.sp)
            }
        }
    }
}

internal fun priceCsv(rows: List<MaterialPrice>): String {
    fun cell(value: Any?) = "\"${value?.toString().orEmpty().replace("\"", "\"\"")}\""
    val header = listOf("Kode SKU", "Nama Bahan Baku", "Kategori", "Satuan", "Vendor Terakhir", "No PO Terakhir", "Tgl PO Terakhir", "Harga Beli Terakhir (Rp)", "Harga Pembelian Sebelumnya (Rp)", "Selisih Nominal vs Prev (Rp)", "Selisih % vs Prev")
    return "﻿" + (listOf(header.joinToString(",", transform = ::cell)) + rows.map { row ->
        listOf("", row.name, row.category, row.unit, row.latest?.vendor, row.latest?.number, row.latest?.date, row.latest?.price,
            row.previous?.price, row.delta, row.percent?.let { String.format(Locale.US, "%.2f%%", it * 100) }).joinToString(",", transform = ::cell)
    }).joinToString("\r\n")
}
