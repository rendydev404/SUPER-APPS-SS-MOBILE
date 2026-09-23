package com.sukashawarma.superapp.feature.stok.ui.area

import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.core.ui.kaca.denganRuangNav
import com.sukashawarma.superapp.core.ui.ios.KartuIos
import com.sukashawarma.superapp.core.ui.ios.KolomCariIos
import com.sukashawarma.superapp.core.ui.ios.LabelSeksiIos
import com.sukashawarma.superapp.core.ui.ios.LencanaIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.TombolBundarIos
import com.sukashawarma.superapp.core.ui.ios.TombolKeduaIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.ios.tekanIos
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import com.sukashawarma.superapp.core.ui.SukaDropdownHeader
import com.sukashawarma.superapp.core.ui.SukaDropdownMenu
import com.sukashawarma.superapp.core.ui.SukaDropdownMenuItem
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
 * Nada kartu mengikuti arah harga, bukan kategori: pengelola area membuka layar ini
 * untuk mencari kenaikan, jadi warnanya harus menjawab itu dari satu kali lirik.
 */
private fun nada(row: MaterialPrice): NadaIos = when {
    row.latest == null -> NadaIos.NETRAL
    row.percent == null -> NadaIos.PERINGATAN
    row.percent!! > 0 -> NadaIos.BAHAYA
    row.percent!! < 0 -> NadaIos.SUKSES
    else -> NadaIos.NETRAL
}

private fun statusLabel(row: MaterialPrice): String = when {
    row.latest == null -> "Harga Master"
    row.percent == null -> "Pembelian Pertama"
    row.percent!! > 0 -> "${percent(row.percent)} Naik"
    row.percent!! < 0 -> "${percent(row.percent)} Turun"
    else -> "Stabil"
}

/** Warna titik per kategori — diputar berurutan, cukup untuk membedakan kelompok. */
private val categoryDots = listOf(
    WarnaIos.Ungu, WarnaIos.Biru, WarnaIos.Hijau, WarnaIos.Oranye, WarnaIos.Merah, WarnaIos.Mint,
)

@Composable
private fun Choice(label: String, options: List<String>, active: Boolean = false, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        KapsulMenu(label, { expanded = true }, aktif = active)
        SukaDropdownMenu(expanded, { expanded = false }) {
            SukaDropdownHeader(title = "PILIH OPSI", onClose = { expanded = false })
            options.forEachIndexed { i, name ->
                SukaDropdownMenuItem(
                    text = name,
                    selected = (label == name),
                    onClick = { expanded = false; onSelect(i) },
                )
            }
        }
    }
}

@Composable
private fun KpiStrip(total: Int, naik: Int, turun: Int, stabil: Int) {
    KartuIos(padding = PaddingValues(vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            KpiCell("Total Item", "$total", "Semua bahan aktif", WarnaIos.LabelKedua, WarnaIos.Label, WarnaIos.LabelKedua)
            KpiDivider()
            KpiCell("Harga Naik", "$naik", "Perlu mitigasi", WarnaIos.LabelKedua, NadaIos.BAHAYA.teks, WarnaIos.LabelKedua)
            KpiDivider()
            KpiCell("Harga Turun", "$turun", "Hemat belanja", WarnaIos.LabelKedua, NadaIos.SUKSES.teks, WarnaIos.LabelKedua)
            KpiDivider()
            KpiCell("Stabil", "$stabil", "Tetap konsisten", WarnaIos.LabelKedua, WarnaIos.Label, WarnaIos.LabelKedua)
        }
    }
}


@Composable
private fun MaterialPriceCard(row: MaterialPrice, onHistory: () -> Unit) {
    KartuIos {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            LencanaIos(statusLabel(row), nada(row))
            Spacer(Modifier.weight(1f))
            Text(row.latest?.let { tanggalSingkat(it.date) } ?: "-", style = TipeIos.Catatan)
        }
        Spacer(Modifier.height(10.dp))
        Text(row.name, style = TipeIos.Utama, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            MiniBadge(row.category, WarnaIos.Isian, WarnaIos.LabelKedua)
            Spacer(Modifier.width(6.dp))
            MiniBadge(row.unit, WarnaIos.Isian, WarnaIos.LabelKedua)
        }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth().clip(UkuranIos.SudutBlok).background(WarnaIos.Latar).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Harga beli terakhir", style = TipeIos.Kecil.copy(fontWeight = FontWeight.Medium))
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(money(row.effective), style = TipeIos.Angka.copy(fontSize = 18.sp), maxLines = 1)
                    Text(" / ${row.unit}", Modifier.padding(bottom = 2.dp), style = TipeIos.Kecil)
                }
                if (row.latest == null && row.master != null) {
                    Text("Dari harga master", style = TipeIos.Kecil)
                }
            }
            Box(Modifier.height(36.dp).width(0.5.dp).background(WarnaIos.Pemisah))
            Column(Modifier.weight(1f).padding(start = 12.dp), horizontalAlignment = Alignment.End) {
                Text("Sebelumnya", style = TipeIos.Kecil.copy(fontWeight = FontWeight.Medium))
                Spacer(Modifier.height(2.dp))
                val previous = row.previous
                if (previous == null) {
                    Text("— (Belum ada data)", style = TipeIos.Catatan, textAlign = TextAlign.End)
                } else {
                    Text(money(previous.price), color = WarnaIos.Label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    row.delta?.let { delta ->
                        Text(
                            "(${if (delta > 0) "+" else ""}${formatRupiah(delta)})",
                            color = if (delta > 0) NadaIos.BAHAYA.teks else if (delta < 0) NadaIos.SUKSES.teks else WarnaIos.LabelKedua,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        BlokAbuIos {
            BarisRincianIos("Vendor", row.latest?.vendor?.takeIf { it.isNotBlank() } ?: "Belum ada PO", tebal = true)
            BarisRincianIos("Nomor PO", row.latest?.number?.takeIf { it.isNotBlank() } ?: "-")
        }
        Spacer(Modifier.height(12.dp))
        TombolKeduaIos("Riwayat Harga", onHistory, Modifier.height(42.dp), ikon = IkonIos.BarChart)
    }
}

/** Judul kategori yang bisa dilipat — bergaya judul seksi iOS, bukan kotak berwarna. */
@Composable
private fun CategoryHeader(name: String, count: Int, closed: Boolean, dot: Color, onToggle: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(UkuranIos.SudutKontrol)
            .tekanIos(onToggle)
            .padding(start = 4.dp, end = 4.dp, top = 10.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(dot))
        Spacer(Modifier.width(8.dp))
        Text(
            name, style = TipeIos.Judul3.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.weight(1f, fill = false),
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(8.dp))
        Text("$count item", style = TipeIos.SubJudul.copy(fontSize = 14.sp))
        Spacer(Modifier.weight(1f))
        Text(if (closed) "Buka" else "Tutup", color = WarnaIos.Aksen, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Icon(
            if (closed) IkonIos.ExpandMore else IkonIos.ExpandLess, null,
            tint = WarnaIos.Aksen, modifier = Modifier.size(16.dp),
        )
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
    Column(Modifier.fillMaxSize().background(WarnaIos.Latar)) {
        HeaderStok("Master Harga Bahan Baku", "Pantau pergerakan harga beli dari vendor", onBack) {
            TombolBundarIos(IkonIos.Refresh, "Perbarui data", { vm.refresh() }, aktif = !state.loading)
            TombolBundarIos(
                IkonIos.Download, "Unduh rekap",
                { exportText = priceCsv(filtered); export.launch("laporan_harga_bahan_baku_${LocalDate.now()}.csv") },
                aktif = filtered.isNotEmpty(),
            )
        }
        message?.let { PitaPesan(it, false) { message = null } }
        when {
            state.loading && state.items.isEmpty() -> MemuatPenuh()
            state.error != null -> KeadaanGagal(state.error!!, { vm.refresh() })
            else -> LazyColumn(
                contentPadding = PaddingValues(horizontal = UkuranIos.TepiLayar, vertical = 12.dp).denganRuangNav(),
                verticalArrangement = Arrangement.spacedBy(UkuranIos.JarakKartu),
            ) {
                item {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        KapsulFilter("Semua", status == "all", { status = "all" }, jumlah = state.items.size)
                        KapsulFilter("Harga Naik", status == "naik", { status = "naik" }, jumlah = state.items.count { it.matchesStatus("naik") }, titik = WarnaIos.Merah)
                        KapsulFilter("Harga Turun", status == "turun", { status = "turun" }, jumlah = state.items.count { it.matchesStatus("turun") }, titik = WarnaIos.Hijau)
                        KapsulFilter("Stabil", status == "stabil", { status = "stabil" }, jumlah = state.items.count { it.matchesStatus("stabil") }, titik = WarnaIos.Abu)
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
                    KolomCariIos(search, { search = it }, Modifier.fillMaxWidth(), placeholder = "Cari bahan baku, vendor, atau nomor PO...")
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
                        KapsulMenu(
                            if (ascending) "Menaik" else "Menurun",
                            { ascending = !ascending },
                            ikon = if (ascending) IkonIos.ArrowUpward else IkonIos.ArrowDownward,
                            chevron = false,
                        )
                        Spacer(Modifier.width(8.dp))
                        KapsulMenu("Per Kategori", { grouped = !grouped }, aktif = grouped, chevron = false)
                        Spacer(Modifier.weight(1f))
                        if (grouped) Text(
                            if (closed.isEmpty()) "Tutup Semua" else "Buka Semua",
                            Modifier
                                .clip(UkuranIos.SudutKapsul)
                                .tekanIos({ closed = if (closed.isEmpty()) categories else emptyList() })
                                .padding(horizontal = 6.dp, vertical = 6.dp),
                            color = WarnaIos.Aksen, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                item {
                    Text(
                        "Menampilkan ${filtered.size} bahan baku dalam ${groups.keys.count { it.isNotBlank() }} kategori",
                        Modifier.padding(horizontal = 4.dp),
                        style = TipeIos.Catatan,
                    )
                }
                if (filtered.isEmpty()) item { KeadaanKosong("Tidak ada bahan baku yang sesuai filter.") }
                groups.entries.forEachIndexed { index, (name, rows) ->
                    if (grouped) item(key = "category:$name") {
                        CategoryHeader(name, rows.size, name in closed, categoryDots[index % categoryDots.size]) {
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
            containerColor = WarnaIos.Latar,
        ) {
            LazyColumn(
                Modifier.fillMaxWidth().fillMaxHeight(0.92f),
                contentPadding = PaddingValues(start = UkuranIos.TepiLayar, end = UkuranIos.TepiLayar, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(UkuranIos.JarakKartu),
            ) {
                item {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MiniBadge(detail.category, WarnaIos.Isian, WarnaIos.LabelKedua)
                            Spacer(Modifier.width(6.dp))
                            MiniBadge(detail.unit, WarnaIos.Isian, WarnaIos.LabelKedua)
                            Spacer(Modifier.weight(1f))
                            TombolBundarIos(IkonIos.Close, "Tutup", { vm.detail(null) }, warnaIkon = WarnaIos.LabelKedua)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(detail.name, style = TipeIos.Judul2)
                        Text("Riwayat harga beli dari vendor", style = TipeIos.SubJudul)
                    }
                }
                item {
                    KartuIos(padding = PaddingValues(vertical = 12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            KpiCell("Harga Master", money(detail.master), "Acuan sistem", WarnaIos.LabelKedua, WarnaIos.Label, WarnaIos.LabelKedua)
                            KpiDivider()
                            KpiCell("PO Terakhir", money(detail.latest?.price), "Harga beli", WarnaIos.LabelKedua, NadaIos.AKSEN.teks, WarnaIos.LabelKedua)
                            KpiDivider()
                            KpiCell("Transaksi", "${state.history.size}", "PO tercatat", WarnaIos.LabelKedua, WarnaIos.Label, WarnaIos.LabelKedua)
                        }
                    }
                }
                when {
                    state.historyLoading -> item {
                        Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(Modifier.size(28.dp), color = WarnaIos.Abu, strokeWidth = 2.5.dp)
                        }
                    }
                    state.historyError != null -> item { KeadaanGagal(state.historyError!!, { vm.detail(detail) }) }
                    state.history.isEmpty() -> item { KeadaanKosong("Belum ada riwayat transaksi pembelian PO yang tercatat.") }
                    else -> {
                        item { PriceChartCard(state.history.reversed()) }
                        item {
                            LabelSeksiIos("Riwayat pembelian", Modifier.padding(start = 16.dp, top = 4.dp))
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
    val nada = when {
        diff == null -> NadaIos.PERINGATAN
        diff > 0 -> NadaIos.BAHAYA
        diff < 0 -> NadaIos.SUKSES
        else -> NadaIos.NETRAL
    }
    val label = when {
        diff == null -> "Pembelian Pertama"
        diff > 0 -> "${percent(diff)} Naik"
        diff < 0 -> "${percent(diff)} Turun"
        else -> "Harga Tetap"
    }
    KartuIos {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            LencanaIos(label, nada)
            Spacer(Modifier.weight(1f))
            Text(tanggalSingkat(row.date), style = TipeIos.Catatan)
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(money(row.price), style = TipeIos.Angka)
            Text(" / $unit", Modifier.padding(bottom = 2.dp), style = TipeIos.Kecil)
        }
        Text(
            row.vendor.takeIf { it.isNotBlank() } ?: "Vendor tidak tercatat",
            style = TipeIos.SubJudul,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(10.dp))
        BlokAbuIos {
            BarisRincianIos("Nomor PO", row.number.takeIf { it.isNotBlank() } ?: "-")
            BarisRincianIos("Kuantitas", "${formatAngkaStok(row.qty)} $unit")
            Box(Modifier.fillMaxWidth().height(0.5.dp).background(WarnaIos.Pemisah))
            BarisRincianIos("Total belanja", money(row.subtotal), tebal = true)
        }
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
    val garis = WarnaIos.Aksen
    val kisi = WarnaIos.Pemisah
    KartuIos {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Pergerakan Harga Beli", style = TipeIos.Utama)
                Text(
                    if (flat) "Tetap di ${money(min)}" else "${money(min)} — ${money(max)}",
                    style = TipeIos.Catatan,
                )
            }
            LencanaIos("${rows.size} transaksi", NadaIos.AKSEN, titik = false)
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
                drawLine(kisi, Offset(0f, y), Offset(size.width, y), 0.5.dp.toPx())
            }
            if (points.size > 1) {
                val area = androidx.compose.ui.graphics.Path().apply {
                    moveTo(points.first().x, size.height)
                    points.forEach { lineTo(it.x, it.y) }
                    lineTo(points.last().x, size.height)
                    close()
                }
                drawPath(area, Brush.verticalGradient(listOf(garis.copy(alpha = 0.20f), Color.Transparent)))
                points.zipWithNext().forEach { (a, b) -> drawLine(garis, a, b, 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round) }
            } else {
                val y = points.first().y
                drawLine(garis.copy(alpha = 0.35f), Offset(0f, y), Offset(size.width, y), 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            }
            points.forEach {
                drawCircle(Color.White, 5.5.dp.toPx(), it)
                drawCircle(garis, 4.dp.toPx(), it, style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(tanggalSingkat(rows.first().date), style = TipeIos.Kecil)
            if (rows.size > 1) Text(tanggalSingkat(rows.last().date), style = TipeIos.Kecil)
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
