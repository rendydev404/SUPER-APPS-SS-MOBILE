package com.sukashawarma.superapp.feature.manager.ui.laporan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.RealtimeTables
import com.sukashawarma.superapp.feature.manager.domain.AnalitikLaporan
import com.sukashawarma.superapp.feature.manager.domain.FilterChannel
import com.sukashawarma.superapp.feature.manager.domain.FilterPembayaran
import com.sukashawarma.superapp.feature.manager.domain.ItemTerjual
import com.sukashawarma.superapp.feature.manager.domain.PresetLaporan
import com.sukashawarma.superapp.feature.manager.domain.RincianPembayaran
import com.sukashawarma.superapp.feature.manager.domain.cacah
import com.sukashawarma.superapp.feature.manager.domain.rupiah
import com.sukashawarma.superapp.feature.manager.ui.BarProgres
import com.sukashawarma.superapp.feature.manager.ui.GarisKartu
import com.sukashawarma.superapp.feature.manager.ui.HijauGaris
import com.sukashawarma.superapp.feature.manager.ui.HijauLatar
import com.sukashawarma.superapp.feature.manager.ui.HijauTeks
import com.sukashawarma.superapp.feature.manager.ui.KartuPanel
import com.sukashawarma.superapp.feature.manager.ui.MerahGaris
import com.sukashawarma.superapp.feature.manager.ui.MerahLatar
import com.sukashawarma.superapp.feature.manager.ui.MerahTeks
import com.sukashawarma.superapp.feature.manager.ui.PanelKosong
import com.sukashawarma.superapp.presentation.theme.SukaBrown
import com.sukashawarma.superapp.presentation.theme.SukaCream
import com.sukashawarma.superapp.presentation.theme.SukaGray400
import com.sukashawarma.superapp.presentation.theme.SukaOrange
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Warna per metode bayar — cermin `PAYMENT_META` di `ReportsClient.tsx`. */
private fun warnaMetode(metode: String): Color = when (metode) {
    "cash" -> Color(0xFF10B981)
    "qris" -> Color(0xFF3B82F6)
    "card" -> Color(0xFF8B5CF6)
    else -> Color(0xFF6B7280)
}

private fun ikonMetode(metode: String): ImageVector = when (metode) {
    "cash" -> Icons.Default.Payments
    "qris" -> Icons.Default.QrCode2
    "card" -> Icons.Default.CreditCard
    else -> Icons.Default.Widgets
}

/**
 * Laporan & Analitik Cabang — cermin `app/reports/` web.
 *
 * Seluruhnya baca-saja. Angka uang di sini memakai acuan omzet kotor yang sama
 * dengan migrasi web 20300128000000, bukan menjumlahkan kolom diskon, supaya
 * laporan di HP dan di laptop tidak pernah berselisih untuk periode yang sama.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaporanScreen(
    onExit: () -> Unit,
    viewModel: LaporanViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()

    RealtimeRefresh(RealtimeTables.ORDERS, RealtimeTables.ORDER_ITEMS) { viewModel.muatUlang() }

    Scaffold(
        containerColor = SukaCream,
        topBar = {
            TopAppBar(
                title = { Text("Laporan", fontWeight = FontWeight.Black, fontSize = 17.sp, color = SukaBrown) },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = SukaBrown)
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::muatUlang) {
                        Icon(Icons.Default.Refresh, "Muat ulang", tint = SukaBrown)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { PanelPenyaring(state, viewModel) }
            if (state.galat != null) {
                item { PanelGalat(state.galat!!, viewModel::muatUlang) }
            }
            kartuKpi(state.analitik)
            item { PanelStatusTransaksi(state.analitik) }
            item { PanelDistribusiPembayaran(state.analitik) }
            daftarItemTerjual(state.analitik)
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

/* ----------------------------------------------------------------------- */
/* Penyaring                                                                */
/* ----------------------------------------------------------------------- */

@Composable
private fun PanelPenyaring(state: LaporanUiState, viewModel: LaporanViewModel) {
    var dialogTanggal by remember { mutableStateOf(false) }

    KartuPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Laporan & Analitik Cabang",
                    color = SukaBrown,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "Insight performa bisnis secara real-time",
                    color = SukaGray400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            if (state.memuat) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = SukaOrange)
            }
        }

        Spacer(Modifier.height(14.dp))
        PilihanTurun(
            ikon = Icons.Default.CalendarMonth,
            nilai = state.labelRentang,
            pilihan = PresetLaporan.entries.map { it to it.label },
            onPilih = { preset ->
                viewModel.pilihPreset(preset)
                if (preset == PresetLaporan.KUSTOM) dialogTanggal = true
            },
        )
        Spacer(Modifier.height(8.dp))
        PilihanTurun(
            ikon = Icons.Default.Storefront,
            nilai = state.channel.label,
            pilihan = FilterChannel.entries.map { it to it.label },
            onPilih = viewModel::pilihChannel,
        )
        Spacer(Modifier.height(8.dp))
        PilihanTurun(
            ikon = Icons.Default.Payments,
            nilai = state.pembayaran.label,
            pilihan = FilterPembayaran.entries.map { it to it.label },
            onPilih = viewModel::pilihPembayaran,
        )
        Spacer(Modifier.height(8.dp))
        // Web sudah menyiapkan daftar outlet dan menerima `outlet_id` lewat URL, tapi
        // belum merender pemilihnya. Di layar sekecil ini penyaring itu justru yang
        // membuat laporan bisa dibaca per cabang, jadi dipasang di sini.
        PilihanTurun(
            ikon = Icons.Default.Storefront,
            nilai = state.namaOutletTerpilih ?: "Semua Outlet",
            pilihan = listOf<Pair<String?, String>>(null to "Semua Outlet") +
                state.daftarOutlet.map { it.id to it.nama },
            onPilih = viewModel::pilihOutlet,
        )
    }

    if (dialogTanggal) {
        DialogRentang(
            onTutup = { dialogTanggal = false },
            onPilih = { dari, sampai ->
                viewModel.pilihRentangKustom(dari, sampai)
                dialogTanggal = false
            },
        )
    }
}

@Composable
private fun <T> PilihanTurun(
    ikon: ImageVector,
    nilai: String,
    pilihan: List<Pair<T, String>>,
    onPilih: (T) -> Unit,
) {
    var terbuka by remember { mutableStateOf(false) }
    Box {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            border = BorderStroke(1.dp, SukaBrown.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth().clickable { terbuka = true },
        ) {
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(ikon, null, tint = SukaOrange, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    nilai,
                    Modifier.weight(1f),
                    color = SukaBrown,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(Icons.Default.ArrowDropDown, null, tint = SukaBrown)
            }
        }
        DropdownMenu(expanded = terbuka, onDismissRequest = { terbuka = false }) {
            pilihan.forEach { (nilaiPilihan, label) ->
                DropdownMenuItem(
                    text = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    onClick = { terbuka = false; onPilih(nilaiPilihan) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogRentang(onTutup: () -> Unit, onPilih: (LocalDate, LocalDate) -> Unit) {
    val picker = rememberDateRangePickerState()
    DatePickerDialog(
        onDismissRequest = onTutup,
        confirmButton = {
            TextButton(
                onClick = {
                    val dari = picker.selectedStartDateMillis?.let(::tanggalDari)
                    val sampai = picker.selectedEndDateMillis?.let(::tanggalDari)
                    if (dari != null) onPilih(dari, sampai ?: dari)
                },
                enabled = picker.selectedStartDateMillis != null,
            ) { Text("Terapkan", fontWeight = FontWeight.ExtraBold) }
        },
        dismissButton = { TextButton(onClick = onTutup) { Text("Batal") } },
    ) {
        DateRangePicker(state = picker, title = {
            Text(
                "Pilih Rentang Tanggal",
                Modifier.padding(start = 24.dp, top = 16.dp),
                fontWeight = FontWeight.Black,
            )
        })
    }
}

private fun tanggalDari(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

@Composable
private fun PanelGalat(pesan: String, onCoba: () -> Unit) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MerahLatar,
        border = BorderStroke(1.dp, MerahGaris),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(pesan, Modifier.weight(1f), color = MerahTeks, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = onCoba) {
                Text("Coba Lagi", color = MerahTeks, fontWeight = FontWeight.Black, fontSize = 12.sp)
            }
        }
    }
}

/* ----------------------------------------------------------------------- */
/* Kartu KPI                                                                */
/* ----------------------------------------------------------------------- */

private fun LazyListScope.kartuKpi(a: AnalitikLaporan) {
    item { KartuOmzetKotor(a) }
    item {
        KartuAngka(
            "Potongan Merchant",
            rupiah(a.potonganMerchant),
            "Diskon yang ditanggung outlet",
            Icons.Default.LocalOffer,
            Color(0xFFF43F5E),
        )
    }
    item {
        KartuAngka(
            "Subsidi Platform",
            rupiah(a.subsidiPlatform),
            "Info — ditanggung aplikasi, bukan potongan outlet",
            Icons.Default.CardGiftcard,
            Color(0xFFF97316),
        )
    }
    item {
        KartuAngka(
            "Pesanan Sukses",
            cacah(a.pesananSukses),
            "Transaksi berhasil diproses",
            Icons.Default.ShoppingBag,
            Color(0xFF3B82F6),
        )
    }
    item {
        KartuAngka(
            "Rata-rata / Order",
            rupiah(a.rataRataPerOrder),
            "Rata-rata belanja per pesanan",
            Icons.Default.TrendingUp,
            Color(0xFF8B5CF6),
        )
    }
    item {
        KartuAngka(
            "Jam Tersibuk",
            // Jam hanya berarti kalau ada pesanan; tanpa itu "00:00" akan terbaca
            // sebagai fakta, padahal artinya tidak ada data.
            if (a.pesananSukses > 0 && a.jamTersibuk != null) {
                "${a.jamTersibuk.toString().padStart(2, '0')}:00"
            } else {
                "—"
            },
            "Jam dengan pesanan terbanyak",
            Icons.Default.Schedule,
            Color(0xFF6366F1),
        )
    }
}

@Composable
private fun KartuOmzetKotor(a: AnalitikLaporan) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = SukaOrange,
        shadowElevation = 3.dp,
    ) {
        Column(Modifier.padding(20.dp)) {
            Box(
                Modifier.size(40.dp).background(Color.White.copy(alpha = 0.20f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Payments, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "OMZET KOTOR",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                rupiah(a.omzetKotor),
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "*Total penerimaan pesanan lunas",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun KartuAngka(
    judul: String,
    nilai: String,
    keterangan: String,
    ikon: ImageVector,
    warna: Color,
) {
    KartuPanel {
        Box(
            Modifier.size(40.dp).background(warna.copy(alpha = 0.10f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(ikon, null, tint = warna, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(
            judul.uppercase(),
            color = SukaGray400,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.8.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            nilai,
            color = SukaBrown,
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(10.dp))
        Text(keterangan, color = SukaGray400, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

/* ----------------------------------------------------------------------- */
/* Status transaksi                                                         */
/* ----------------------------------------------------------------------- */

@Composable
private fun PanelStatusTransaksi(a: AnalitikLaporan) {
    KartuPanel {
        Text("Status Transaksi", color = SukaBrown, fontSize = 16.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(2.dp))
        Text(
            "Pesanan lunas vs batal",
            color = SukaGray400,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(14.dp))
        BarisStatus(
            ikon = Icons.Default.CheckCircle,
            judul = "Selesai",
            keterangan = "Pembayaran sukses",
            jumlah = a.pesananSukses,
            persen = a.persenSukses,
            latar = HijauLatar,
            garis = HijauGaris,
            teks = HijauTeks,
        )
        Spacer(Modifier.height(10.dp))
        BarisStatus(
            ikon = Icons.Default.Cancel,
            judul = "Dibatalkan",
            keterangan = "Kedaluwarsa / batal kasir",
            jumlah = a.pesananBatal,
            persen = a.persenBatal,
            latar = MerahLatar,
            garis = MerahGaris,
            teks = MerahTeks,
        )
    }
}

@Composable
private fun BarisStatus(
    ikon: ImageVector,
    judul: String,
    keterangan: String,
    jumlah: Int,
    persen: Int,
    latar: Color,
    garis: Color,
    teks: Color,
) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = latar,
        border = BorderStroke(1.dp, garis),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(ikon, null, tint = teks, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(judul, color = SukaBrown, fontSize = 13.sp, fontWeight = FontWeight.Black)
                Text(keterangan, color = teks, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(cacah(jumlah), color = SukaBrown, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Text("$persen%", color = teks, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/* ----------------------------------------------------------------------- */
/* Distribusi pembayaran                                                    */
/* ----------------------------------------------------------------------- */

@Composable
private fun PanelDistribusiPembayaran(a: AnalitikLaporan) {
    KartuPanel {
        Text("Distribusi Pembayaran", color = SukaBrown, fontSize = 16.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(2.dp))
        Text("Rincian per metode bayar", color = SukaGray400, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(16.dp))

        if (a.rincianPembayaran.isEmpty()) {
            PanelKosong("Belum ada data pembayaran.")
            return@KartuPanel
        }

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(Modifier.size(168.dp), contentAlignment = Alignment.Center) {
                CincinPembayaran(a.rincianPembayaran)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        cacah(a.pesananSukses),
                        color = SukaBrown,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Text("Total", color = SukaGray400, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        a.rincianPembayaran.forEach { rincian ->
            BarisMetodeBayar(rincian, a.omzetKotor)
            Spacer(Modifier.height(8.dp))
        }
    }
}

/**
 * Cincin proporsi jumlah transaksi per metode bayar — padanan donut Recharts di web.
 *
 * Digambar langsung dengan Canvas, bukan lewat pustaka grafik: satu cincin tidak
 * sepadan dengan menambah dependensi, dan sudut celah antar-potongan jadi bisa
 * disamakan persis dengan `paddingAngle` versi web.
 */
@Composable
private fun CincinPembayaran(rincian: List<RincianPembayaran>) {
    val total = rincian.sumOf { it.jumlah }.coerceAtLeast(1)
    Canvas(Modifier.size(168.dp)) {
        val tebal = 26.dp.toPx()
        val sisi = size.minDimension - tebal
        val kiriAtas = androidx.compose.ui.geometry.Offset(tebal / 2, tebal / 2)
        var mulai = -90f
        rincian.forEach { bagian ->
            val sudut = 360f * bagian.jumlah / total
            // Celah hanya masuk akal kalau potongannya cukup lebar; memaksakannya pada
            // potongan tipis justru menghapus potongan itu dari layar.
            val celah = if (sudut > 8f) 4f else 0f
            drawArc(
                color = warnaMetode(bagian.metode),
                startAngle = mulai + celah / 2,
                sweepAngle = sudut - celah,
                useCenter = false,
                topLeft = kiriAtas,
                size = Size(sisi, sisi),
                style = Stroke(width = tebal),
            )
            mulai += sudut
        }
    }
}

@Composable
private fun BarisMetodeBayar(rincian: RincianPembayaran, omzetKotor: Long) {
    val warna = warnaMetode(rincian.metode)
    val persen = if (omzetKotor > 0) rincian.omzet.toDouble() / omzetKotor * 100 else 0.0
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = SukaBrown.copy(alpha = 0.03f),
        border = BorderStroke(1.dp, GarisKartu),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(38.dp).background(warna.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(ikonMetode(rincian.metode), null, tint = warna, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(rincian.label, color = SukaBrown, fontSize = 13.sp, fontWeight = FontWeight.Black)
                Text(
                    "${cacah(rincian.jumlah)} transaksi",
                    color = SukaGray400,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(rupiah(rincian.omzet), color = SukaBrown, fontSize = 13.sp, fontWeight = FontWeight.Black)
                Text(
                    String.format(java.util.Locale.US, "%.1f%%", persen),
                    color = warna,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

/* ----------------------------------------------------------------------- */
/* Item yang terjual                                                        */
/* ----------------------------------------------------------------------- */

private fun LazyListScope.daftarItemTerjual(a: AnalitikLaporan) {
    if (a.daftarItem.isEmpty()) return

    item {
        KartuPanel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Item Yang Terjual", color = SukaBrown, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    Text(
                        "${cacah(a.itemTerjual)} porsi dari ${cacah(a.daftarItem.size)} menu",
                        color = SukaGray400,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = GarisKartu)
        }
    }

    itemsIndexed(a.daftarItem, key = { _, item -> item.nama }) { indeks, item ->
        BarisItemTerjual(item, indeks + 1, a.qtyTertinggi)
    }
}

@Composable
private fun BarisItemTerjual(item: ItemTerjual, peringkat: Int, qtyTertinggi: Int) {
    KartuPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "#$peringkat",
                Modifier.width(30.dp),
                color = if (peringkat <= 3) SukaOrange else SukaGray400,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                item.nama,
                Modifier.weight(1f),
                color = SukaBrown,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(rupiah(item.omzet), color = SukaBrown, fontSize = 12.sp, fontWeight = FontWeight.Black)
                Text(
                    "${cacah(item.qty)} terjual",
                    color = SukaGray400,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        BarProgres(
            rasio = if (qtyTertinggi > 0) item.qty.toFloat() / qtyTertinggi else 0f,
            tinggi = 5,
            sorot = peringkat <= 3,
        )
    }
}
