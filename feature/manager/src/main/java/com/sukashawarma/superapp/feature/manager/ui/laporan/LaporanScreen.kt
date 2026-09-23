package com.sukashawarma.superapp.feature.manager.ui.laporan

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.sukashawarma.superapp.core.ui.SukaDropdownHeader
import com.sukashawarma.superapp.core.ui.SukaDropdownMenu
import com.sukashawarma.superapp.core.ui.SukaDropdownMenuItem
import com.sukashawarma.superapp.core.ui.SukaFilterDropdown
import com.sukashawarma.superapp.core.ui.ios.BilahJudulIos
import com.sukashawarma.superapp.core.ui.ios.IkonBulatIos
import com.sukashawarma.superapp.core.ui.ios.JudulSeksiIos
import com.sukashawarma.superapp.core.ui.ios.KartuIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.PanelGalatIos
import com.sukashawarma.superapp.core.ui.ios.PemisahIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.TombolBundarIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.core.ui.kaca.denganRuangNav
import com.sukashawarma.superapp.feature.manager.domain.AnalitikLaporan
import com.sukashawarma.superapp.feature.manager.domain.FilterChannel
import com.sukashawarma.superapp.feature.manager.domain.FilterPembayaran
import com.sukashawarma.superapp.feature.manager.domain.ItemTerjual
import com.sukashawarma.superapp.feature.manager.domain.PresetLaporan
import com.sukashawarma.superapp.feature.manager.domain.RincianPembayaran
import com.sukashawarma.superapp.feature.manager.domain.cacah
import com.sukashawarma.superapp.feature.manager.domain.rupiah
import com.sukashawarma.superapp.feature.manager.ui.BarProgres
import com.sukashawarma.superapp.feature.manager.ui.KartuKpi
import com.sukashawarma.superapp.feature.manager.ui.KartuPanel
import com.sukashawarma.superapp.feature.manager.ui.LencanaPeringkat
import com.sukashawarma.superapp.feature.manager.ui.PanelKosong
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Warna per metode bayar — urutannya cermin `PAYMENT_META` di `ReportsClient.tsx`
 * (tunai hijau, QRIS biru, kartu ungu), tetapi memakai warna sistem iOS supaya
 * cincin dan baris rinciannya serasi dengan lencana di layar lain.
 */
private fun warnaMetode(metode: String): Color = when (metode) {
    "cash" -> WarnaIos.Hijau
    "qris" -> WarnaIos.Biru
    "card" -> WarnaIos.Ungu
    else -> WarnaIos.Abu
}

private fun ikonMetode(metode: String): ImageVector = when (metode) {
    "cash" -> IkonIos.Payments
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

    RealtimeRefresh(RealtimeTables.ORDERS, RealtimeTables.ORDER_ITEMS) { viewModel.segarkanDariRealtime() }

    Scaffold(
        containerColor = WarnaIos.Latar,
        topBar = {
            BilahJudulIos(
                judul = "Laporan",
                onKembali = onExit,
                aksi = { TombolBundarIos(IkonIos.Refresh, "Muat ulang", viewModel::muatPaksa) },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(
                start = UkuranIos.TepiLayar,
                end = UkuranIos.TepiLayar,
                top = 12.dp,
                bottom = 16.dp,
            ).denganRuangNav(),
            verticalArrangement = Arrangement.spacedBy(UkuranIos.JarakKartu),
        ) {
            item { PanelPenyaring(state, viewModel) }
            if (state.galat != null) {
                item { PanelGalatIos(state.galat!!, viewModel::muatPaksa) }
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
        Text("Laporan & Analitik Cabang", style = TipeIos.Judul3)
        Spacer(Modifier.height(2.dp))
        Text("Insight performa bisnis secara real-time", style = TipeIos.Catatan)

        Spacer(Modifier.height(14.dp))
        PilihanTurun(
            ikon = IkonIos.CalendarMonth,
            nilai = state.labelRentang,
            pilihan = PresetLaporan.entries.map { it to it.label },
            onPilih = { preset ->
                viewModel.pilihPreset(preset)
                if (preset == PresetLaporan.KUSTOM) dialogTanggal = true
            },
        )
        Spacer(Modifier.height(8.dp))
        PilihanTurun(
            ikon = IkonIos.Storefront,
            nilai = state.channel.label,
            pilihan = FilterChannel.entries.map { it to it.label },
            onPilih = viewModel::pilihChannel,
        )
        Spacer(Modifier.height(8.dp))
        PilihanTurun(
            ikon = IkonIos.Payments,
            nilai = state.pembayaran.label,
            pilihan = FilterPembayaran.entries.map { it to it.label },
            onPilih = viewModel::pilihPembayaran,
        )
        Spacer(Modifier.height(8.dp))
        // Web sudah menyiapkan daftar outlet dan menerima `outlet_id` lewat URL, tapi
        // belum merender pemilihnya. Di layar sekecil ini penyaring itu justru yang
        // membuat laporan bisa dibaca per cabang, jadi dipasang di sini.
        PilihanTurun(
            ikon = IkonIos.Storefront,
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
        SukaFilterDropdown(
            label = "",
            value = nilai,
            expanded = terbuka,
            onClick = { terbuka = true },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = ikon,
        )
        SukaDropdownMenu(expanded = terbuka, onDismissRequest = { terbuka = false }) {
            SukaDropdownHeader(title = "PILIH OPSI", onClose = { terbuka = false })
            pilihan.forEach { (nilaiPilihan, label) ->
                SukaDropdownMenuItem(
                    text = label,
                    selected = (label == nilai),
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
    val warna = DatePickerDefaults.colors(
        containerColor = WarnaIos.Kartu,
        selectedDayContainerColor = WarnaIos.Aksen,
        todayDateBorderColor = WarnaIos.Aksen,
        dayInSelectionRangeContainerColor = WarnaIos.Aksen.copy(alpha = 0.14f),
    )
    DatePickerDialog(
        onDismissRequest = onTutup,
        confirmButton = {
            val bisa = picker.selectedStartDateMillis != null
            TextButton(
                onClick = {
                    val dari = picker.selectedStartDateMillis?.let(::tanggalDari)
                    val sampai = picker.selectedEndDateMillis?.let(::tanggalDari)
                    if (dari != null) onPilih(dari, sampai ?: dari)
                },
                enabled = bisa,
            ) {
                Text("Terapkan", color = if (bisa) WarnaIos.Aksen else WarnaIos.Abu, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onTutup) { Text("Batal", color = WarnaIos.Aksen) }
        },
        shape = UkuranIos.SudutKartu,
        colors = warna,
    ) {
        DateRangePicker(
            state = picker,
            title = {
                Text(
                    "Pilih Rentang Tanggal",
                    Modifier.padding(start = 24.dp, top = 16.dp),
                    style = TipeIos.Utama,
                )
            },
            colors = warna,
        )
    }
}

private fun tanggalDari(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

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
            IkonIos.Sell,
            WarnaIos.Merah,
        )
    }
    item {
        KartuAngka(
            "Subsidi Platform",
            rupiah(a.subsidiPlatform),
            "Info — ditanggung aplikasi, bukan potongan outlet",
            Icons.Default.CardGiftcard,
            WarnaIos.Oranye,
        )
    }
    item {
        KartuAngka(
            "Pesanan Sukses",
            cacah(a.pesananSukses),
            "Transaksi berhasil diproses",
            IkonIos.ShoppingBag,
            WarnaIos.Biru,
        )
    }
    item {
        KartuAngka(
            "Rata-rata / Order",
            rupiah(a.rataRataPerOrder),
            "Rata-rata belanja per pesanan",
            IkonIos.TrendingUp,
            WarnaIos.Ungu,
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
            IkonIos.Schedule,
            WarnaIos.Indigo,
        )
    }
}

/**
 * Omzet kotor tetap menjadi satu-satunya kartu berisi warna aksen: angka inilah
 * yang pertama dicari manajer, jadi ia dibedakan dari KPI pendamping yang putih.
 */
@Composable
private fun KartuOmzetKotor(a: AnalitikLaporan) {
    KartuIos(latar = WarnaIos.Aksen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(30.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(IkonIos.aktif(IkonIos.Payments), null, tint = Color.White, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(
                "Omzet Kotor",
                color = Color.White.copy(alpha = 0.92f),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            rupiah(a.omzetKotor),
            style = TipeIos.AngkaBesar.copy(color = Color.White, fontSize = 30.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "*Total penerimaan pesanan lunas",
            style = TipeIos.Catatan.copy(color = Color.White.copy(alpha = 0.8f)),
        )
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
    KartuKpi(
        judul = judul,
        nilai = nilai,
        ikon = ikon,
        warnaIkon = warna,
        kaki = { Text(keterangan, style = TipeIos.Catatan) },
    )
}

/* ----------------------------------------------------------------------- */
/* Status transaksi                                                         */
/* ----------------------------------------------------------------------- */

@Composable
private fun PanelStatusTransaksi(a: AnalitikLaporan) {
    KartuPanel {
        Text("Status Transaksi", style = TipeIos.Utama)
        Text("Pesanan lunas vs batal", style = TipeIos.Catatan)
        Spacer(Modifier.height(12.dp))
        // Dua status dalam satu blok abu bersekat, seperti daftar ringkas iOS —
        // perbandingan lunas vs batal terbaca sekali lirik.
        Column(Modifier.fillMaxWidth().clip(UkuranIos.SudutBlok).background(WarnaIos.Latar)) {
            BarisStatus(
                ikon = IkonIos.CheckCircle,
                judul = "Selesai",
                keterangan = "Pembayaran sukses",
                jumlah = a.pesananSukses,
                persen = a.persenSukses,
                nada = NadaIos.SUKSES,
            )
            PemisahIos(inset = 56.dp)
            BarisStatus(
                ikon = IkonIos.Close,
                judul = "Dibatalkan",
                keterangan = "Kedaluwarsa / batal kasir",
                jumlah = a.pesananBatal,
                persen = a.persenBatal,
                nada = NadaIos.BAHAYA,
            )
        }
    }
}

@Composable
private fun BarisStatus(
    ikon: ImageVector,
    judul: String,
    keterangan: String,
    jumlah: Int,
    persen: Int,
    nada: NadaIos,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IkonBulatIos(ikon, nada.warna)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(judul, style = TipeIos.Keterangan.copy(fontWeight = FontWeight.SemiBold))
            Text(keterangan, style = TipeIos.Catatan, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(cacah(jumlah), style = TipeIos.Angka)
            Text("$persen%", color = nada.teks, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/* ----------------------------------------------------------------------- */
/* Distribusi pembayaran                                                    */
/* ----------------------------------------------------------------------- */

@Composable
private fun PanelDistribusiPembayaran(a: AnalitikLaporan) {
    KartuPanel {
        Text("Distribusi Pembayaran", style = TipeIos.Utama)
        Text("Rincian per metode bayar", style = TipeIos.Catatan)
        Spacer(Modifier.height(16.dp))

        if (a.rincianPembayaran.isEmpty()) {
            PanelKosong("Belum ada data pembayaran.")
            return@KartuPanel
        }

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(Modifier.size(168.dp), contentAlignment = Alignment.Center) {
                CincinPembayaran(a.rincianPembayaran)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(cacah(a.pesananSukses), style = TipeIos.AngkaBesar)
                    Text("Total", style = TipeIos.Catatan)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Column(Modifier.fillMaxWidth().clip(UkuranIos.SudutBlok).background(WarnaIos.Latar)) {
            a.rincianPembayaran.forEachIndexed { i, rincian ->
                if (i > 0) PemisahIos(inset = 56.dp)
                BarisMetodeBayar(rincian, a.omzetKotor)
            }
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
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IkonBulatIos(ikonMetode(rincian.metode), warna)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                rincian.label,
                style = TipeIos.Keterangan.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text("${cacah(rincian.jumlah)} transaksi", style = TipeIos.Catatan)
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(rupiah(rincian.omzet), style = TipeIos.Keterangan.copy(fontWeight = FontWeight.SemiBold))
            Text(
                String.format(java.util.Locale.US, "%.1f%%", persen),
                color = warna,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/* ----------------------------------------------------------------------- */
/* Item yang terjual                                                        */
/* ----------------------------------------------------------------------- */

private fun LazyListScope.daftarItemTerjual(a: AnalitikLaporan) {
    if (a.daftarItem.isEmpty()) return

    item {
        Column {
            JudulSeksiIos("Item Yang Terjual")
            Text(
                "${cacah(a.itemTerjual)} porsi dari ${cacah(a.daftarItem.size)} menu",
                Modifier.padding(horizontal = 4.dp),
                style = TipeIos.Catatan,
            )
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
            LencanaPeringkat(peringkat)
            Spacer(Modifier.width(10.dp))
            Text(
                item.nama,
                Modifier.weight(1f),
                style = TipeIos.Keterangan.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(rupiah(item.omzet), style = TipeIos.Keterangan.copy(fontWeight = FontWeight.SemiBold))
                Text("${cacah(item.qty)} terjual", style = TipeIos.Catatan)
            }
        }
        Spacer(Modifier.height(10.dp))
        BarProgres(
            rasio = if (qtyTertinggi > 0) item.qty.toFloat() / qtyTertinggi else 0f,
            tinggi = 6,
            sorot = peringkat <= 3,
        )
    }
}
