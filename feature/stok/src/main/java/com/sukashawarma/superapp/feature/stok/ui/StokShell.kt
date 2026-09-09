package com.sukashawarma.superapp.feature.stok.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlinx.coroutines.launch
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.RealtimeTables
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.feature.stok.data.MutasiRepository
import com.sukashawarma.superapp.feature.stok.data.WasteApprovalAccess
import com.sukashawarma.superapp.feature.stok.domain.MutasiBadge
import com.sukashawarma.superapp.feature.stok.domain.StokAkses
import com.sukashawarma.superapp.feature.stok.ui.area.HargaBahanScreen
import com.sukashawarma.superapp.feature.stok.ui.area.WasteApprovalScreen
import com.sukashawarma.superapp.feature.stok.ui.entri.EntriManualScreen
import com.sukashawarma.superapp.feature.stok.ui.entri.RiwayatWasteScreen
import com.sukashawarma.superapp.feature.stok.ui.laporan.ArusBarangScreen
import com.sukashawarma.superapp.feature.stok.ui.laporan.HppMenuScreen
import com.sukashawarma.superapp.feature.stok.ui.laporan.LaporanPenjualanScreen
import com.sukashawarma.superapp.feature.stok.ui.laporan.NilaiPersediaanScreen
import com.sukashawarma.superapp.feature.stok.ui.laporan.PlafonBelanjaScreen
import com.sukashawarma.superapp.feature.stok.ui.laporan.ThresholdScreen
import com.sukashawarma.superapp.feature.stok.ui.ledger.LedgerScreen
import com.sukashawarma.superapp.feature.stok.ui.monitoring.MonitoringScreen
import com.sukashawarma.superapp.feature.stok.ui.mutasi.MutasiScreen
import com.sukashawarma.superapp.feature.stok.ui.opname.OpnameScreen
import com.sukashawarma.superapp.feature.stok.ui.opname.PersetujuanOpnameScreen
import com.sukashawarma.superapp.feature.stok.ui.permintaan.PermintaanScreen
import com.sukashawarma.superapp.feature.stok.ui.po.PenerimaanPoScreen

/**
 * Tujuan di modul Stok. Label pendek untuk bilah bawah, label panjang untuk lembar
 * "Lainnya" — persis pola `BottomNav.tsx` web, yang memakai `shortLabel` di bilah dan
 * nama lengkap di laci.
 */
private enum class TabStok(
    val label: String,
    val labelPanjang: String,
    val icon: ImageVector,
) {
    DASHBOARD("Dashboard", "Monitoring Stok", Icons.Default.Dashboard),
    PERMINTAAN("Permintaan", "Permintaan Bahan", Icons.Default.Assignment),
    OPNAME("Opname", "Stok Opname", Icons.Default.Description),
    TERIMA_PO("Terima PO", "Penerimaan PO Supplier", Icons.Default.LocalShipping),
    WASTE("Waste", "Persetujuan Waste", Icons.Default.DeleteSweep),
    LEDGER("Ledger", "Buku Ledger Stok", Icons.Default.MenuBook),
    MUTASI("Mutasi", "Mutasi Antar Outlet", Icons.Default.SwapHoriz),
    ENTRI("Entri Manual", "Entri Manual & Lapor Waste", Icons.Default.EditNote),
    RIWAYAT_WASTE("Riwayat Waste", "Riwayat Waste Saya", Icons.Default.History),
    PERSETUJUAN_OPNAME("Approval Opname", "Persetujuan Opname", Icons.Default.FactCheck),
    HARGA("Harga Bahan", "Master Harga Bahan Baku", Icons.Default.Sell),

    // Kelompok "ANALISIS & LAPORAN" di AppSidebar.tsx web. Label panjangnya disalin
    // apa adanya supaya orang yang berpindah dari browser ke HP mencari nama yang sama.
    NILAI_PERSEDIAAN("Nilai Stok", "Nilai Persediaan", Icons.Default.Savings),
    HPP_MENU("HPP Menu", "HPP Setiap Menu", Icons.Default.Calculate),
    LAPORAN_PENJUALAN("Penjualan", "Laporan Penjualan", Icons.Default.TrendingUp),
    PLAFON("Plafon", "Plafon & Belanja Outlet", Icons.Default.AccountBalanceWallet),
    ARUS_BARANG("In/Out", "Inbound / Outbound", Icons.Default.ImportExport),
    THRESHOLD("Threshold", "Pengaturan Threshold", Icons.Default.Tune),
}

/**
 * Susunan tab per kelompok peran — cermin `primaryTabs` dan `moreItems` di web.
 *
 * Empat pertama muncul di bilah bawah, sisanya di "Lainnya". Urutannya bukan selera:
 * yang paling sering dipakai kelompok itu ditaruh di depan, sama seperti web.
 */
@Composable
private fun tujuanUntukPeran(): List<TabStok> {
    val staff by AppSession.staff.collectAsState()
    val role = staff?.role
    val bolehHarga = StokAkses.melihatHargaBahan(role)
    val bolehWaste = WasteApprovalAccess.allowed(role)
    val bolehPo = StokAkses.melihatPenerimaanPo(role)

    return buildList {
        if (StokAkses.melihatDashboard(role)) add(TabStok.DASHBOARD)
        add(TabStok.PERMINTAAN)
        add(TabStok.OPNAME)
        when {
            StokAkses.pusat(role) -> {
                if (bolehPo) add(TabStok.TERIMA_PO)
                add(TabStok.LEDGER)
            }
            StokAkses.pengawas(role) -> {
                if (bolehWaste) add(TabStok.WASTE)
                add(TabStok.LEDGER)
            }
            else -> {
                add(TabStok.LEDGER)
                add(TabStok.MUTASI)
            }
        }
        // Sisanya masuk laci, tanpa mengulang yang sudah di bilah.
        if (TabStok.MUTASI !in this) add(TabStok.MUTASI)
        add(TabStok.ENTRI)
        // Pasangan Entri Manual: di sana orang melapor waste, di sini ia melihat
        // hasilnya. Tanpa gerbang peran, sama seperti Entri Manual — kueri-nya sudah
        // dibatasi ke laporan milik akun yang sedang masuk.
        add(TabStok.RIWAYAT_WASTE)
        if (bolehWaste && TabStok.WASTE !in this) add(TabStok.WASTE)
        if (bolehPo && TabStok.TERIMA_PO !in this) add(TabStok.TERIMA_PO)
        add(TabStok.PERSETUJUAN_OPNAME)
        if (bolehHarga) add(TabStok.HARGA)

        // Kelompok "ANALISIS & LAPORAN" web, urutannya mengikuti AppSidebar.tsx.
        // Tiap menu punya daftar peran sendiri — lihat StokAkses.
        if (StokAkses.melihatNilaiPersediaan(role)) add(TabStok.NILAI_PERSEDIAAN)
        if (StokAkses.melihatHppMenu(role)) add(TabStok.HPP_MENU)
        if (StokAkses.melihatLaporanPenjualan(role)) add(TabStok.LAPORAN_PENJUALAN)
        if (StokAkses.melihatPlafonBelanja(role)) add(TabStok.PLAFON)
        if (StokAkses.melihatInboundOutbound(role)) add(TabStok.ARUS_BARANG)
        if (StokAkses.melihatThreshold(role)) add(TabStok.THRESHOLD)
    }
}

/**
 * Jumlah mutasi yang menunggu tindakan pengguna — cermin `useMutasiBadge` web.
 *
 * Dihitung di shell, bukan di dalam layar Mutasi, karena justru gunanya ketika
 * layar itu SEDANG TIDAK dibuka: crew tidak punya cara lain mengetahui ada kiriman
 * masuk selain membuka lacinya satu per satu.
 *
 * Lingkupnya `staff.outletId`, dan untuk peran pusat nilai itu null sehingga seluruh
 * outlet ikut terhitung. Itu disengaja dan harus tetap sejalan dengan layar Mutasi,
 * yang membuka peran pusat pada cakupan "Semua Outlet" — lihat [SEMUA_OUTLET].
 * Pernah tidak sejalan, dan akibatnya lencana menunjukkan 9+ sementara halamannya
 * kosong karena hanya menampilkan satu outlet.
 *
 * Kegagalan diabaikan diam-diam dan angkanya bertahan di nilai sebelumnya. Badge
 * adalah petunjuk, bukan data: memunculkan pesan galat di bilah bawah yang selalu
 * terlihat akan lebih mengganggu daripada angka yang telat menyusul.
 */
@Composable
private fun lencanaMutasi(): Int {
    val staff by AppSession.staff.collectAsState()
    val role = staff?.role
    val outletId = staff?.outletId
    var jumlah by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    suspend fun muat() {
        runCatching { MutasiRepository.ringkasUntukBadge(outletId) }
            .onSuccess { jumlah = MutasiBadge.hitung(it, role, outletId).total }
            .onFailure { android.util.Log.w("StokShell", "lencana mutasi gagal", it) }
    }

    // Seluruh `staff` jadi kunci, bukan role+outletId saja: sesi yang berganti dari
    // null ke terisi bisa membawa role dan outlet yang sama-sama null, dan kunci
    // yang lebih sempit tidak akan menyadarinya lalu diam di angka nol.
    LaunchedEffect(staff) {
        if (staff != null) muat()
    }
    RealtimeRefresh(RealtimeTables.MUTASI) {
        // Scope milik composable, bukan MainScope(): scope lepas tidak pernah ikut
        // dibatalkan saat layar hilang, jadi tiap event akan menumpuk pekerjaan yang
        // menulis ke state yang sudah tidak dipakai.
        if (staff != null) scope.launch { muat() }
    }
    return if (staff == null) 0 else jumlah
}

@Composable
fun StokShell(
    onKeluar: () -> Unit,
    onBukaBahan: (outletId: String, bahanId: String, nama: String) -> Unit,
    onBukaProduksi: (outletId: String) -> Unit,
    onBukaTransfer: () -> Unit,
) {
    val tabs = tujuanUntukPeran()
    val staff by AppSession.staff.collectAsState()
    val peran = staff?.role
    val hargaAccess = StokAkses.melihatHargaBahan(peran)
    val wasteAccess = WasteApprovalAccess.allowed(peran)

    // Tab awal adalah yang pertama tersedia: role pusat tidak punya Dashboard, jadi
    // membukanya di Permintaan, bukan di layar kosong.
    var tab by rememberSaveable { mutableStateOf(tabs.firstOrNull() ?: TabStok.PERMINTAAN) }
    if (tab !in tabs) tab = tabs.firstOrNull() ?: TabStok.PERMINTAAN

    val utama = if (tabs.size > 5) tabs.take(4) else tabs
    BackHandler(enabled = tab !in utama) { tab = tabs.first() }

    // Peta, bukan satu angka: web memberi lencana pada beberapa tujuan sekaligus
    // (permintaan, waste, terima PO), dan bentuk ini menampung tambahan itu tanpa
    // mengubah tanda tangan BottomNavStok lagi.
    val lencana = mapOf(TabStok.MUTASI to lencanaMutasi())

    Column(Modifier.fillMaxSize().background(com.sukashawarma.superapp.presentation.theme.SukaSurface)) {
        Box(Modifier.weight(1f)) {
            when (tab) {
                TabStok.DASHBOARD -> MonitoringScreen(
                    onKeluar = onKeluar,
                    onBukaBahan = onBukaBahan,
                    onBukaProduksi = onBukaProduksi,
                    onBukaTransfer = onBukaTransfer,
                )
                TabStok.PERMINTAAN -> PermintaanScreen()
                TabStok.OPNAME -> OpnameScreen()
                TabStok.LEDGER -> LedgerScreen()
                TabStok.MUTASI -> MutasiScreen()
                TabStok.ENTRI -> EntriManualScreen(onBack = { tab = tabs.first() })
                TabStok.RIWAYAT_WASTE -> RiwayatWasteScreen(onBack = { tab = tabs.first() })
                TabStok.PERSETUJUAN_OPNAME -> PersetujuanOpnameScreen(onBack = { tab = tabs.first() })
                TabStok.TERIMA_PO -> PenerimaanPoScreen(onBack = { tab = tabs.first() })
                TabStok.HARGA -> if (hargaAccess) HargaBahanScreen(onBack = { tab = tabs.first() })
                    else KeadaanTidakBerhak("Modul ini hanya untuk pemegang master harga.")
                TabStok.WASTE -> if (wasteAccess) WasteApprovalScreen(onBack = { tab = tabs.first() })
                    else KeadaanTidakBerhak("Modul ini hanya untuk penyetuju waste.")

                // Gerbang diperiksa ulang di sini, bukan hanya saat menyusun daftar tab:
                // `tab` dipulihkan dari rememberSaveable dan bisa menunjuk tujuan yang
                // sudah tidak boleh dibuka kalau peran pengguna berubah.
                TabStok.NILAI_PERSEDIAAN ->
                    if (StokAkses.melihatNilaiPersediaan(peran)) NilaiPersediaanScreen(onBack = { tab = tabs.first() })
                    else KeadaanTidakBerhak("Nilai persediaan hanya untuk kantor dan gudang pusat.")
                TabStok.HPP_MENU ->
                    if (StokAkses.melihatHppMenu(peran)) HppMenuScreen(onBack = { tab = tabs.first() })
                    else KeadaanTidakBerhak("Kalkulator HPP hanya untuk pengelola menu.")
                TabStok.LAPORAN_PENJUALAN ->
                    if (StokAkses.melihatLaporanPenjualan(peran)) LaporanPenjualanScreen(onBack = { tab = tabs.first() })
                    else KeadaanTidakBerhak("Laporan penjualan hanya untuk kantor dan gudang pusat.")
                TabStok.PLAFON ->
                    if (StokAkses.melihatPlafonBelanja(peran)) PlafonBelanjaScreen(onBack = { tab = tabs.first() })
                    else KeadaanTidakBerhak("Plafon belanja hanya untuk kantor dan gudang pusat.")
                TabStok.ARUS_BARANG ->
                    if (StokAkses.melihatInboundOutbound(peran)) ArusBarangScreen(onBack = { tab = tabs.first() })
                    else KeadaanTidakBerhak("Arus barang hanya untuk staff gudang pusat.")

                // Gerbang kedua ini menanggung lebih banyak daripada yang lain:
                // `outlet_reorder_point` tidak punya policy RLS, jadi tidak ada
                // lapisan di belakangnya yang akan menolak peran yang salah.
                TabStok.THRESHOLD ->
                    if (StokAkses.melihatThreshold(peran)) ThresholdScreen(onBack = { tab = tabs.first() })
                    else KeadaanTidakBerhak("Pengaturan threshold hanya untuk admin.")
            }
        }
        BottomNavStok(tab, tabs, lencana) { tab = it }
    }
}

/**
 * Kelompok pada lembar "Lainnya", urut seperti `AppSidebar.tsx` web.
 *
 * Daftar ini hanya menata tampilan — bukan gerbang akses. Yang menentukan sebuah
 * tujuan muncul tetap `tujuanUntukPeran`; di sini isinya cuma disaring menurut apa
 * yang sudah lolos ke `overflow`.
 */
private val KELOMPOK_MENU_STOK = listOf(
    "Operasional" to listOf(
        TabStok.LEDGER,
        TabStok.MUTASI,
        TabStok.ENTRI,
        TabStok.RIWAYAT_WASTE,
        TabStok.WASTE,
        TabStok.TERIMA_PO,
        TabStok.PERSETUJUAN_OPNAME,
    ),
    "Analisis & Laporan" to listOf(
        TabStok.HARGA,
        TabStok.NILAI_PERSEDIAAN,
        TabStok.HPP_MENU,
        TabStok.LAPORAN_PENJUALAN,
        TabStok.PLAFON,
        TabStok.ARUS_BARANG,
        TabStok.THRESHOLD,
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomNavStok(
    aktif: TabStok,
    tabs: List<TabStok>,
    lencana: Map<TabStok, Int>,
    onPilih: (TabStok) -> Unit,
) {
    var moreOpen by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    val primary = if (tabs.size > 5) tabs.take(4) else tabs
    val overflow = if (tabs.size > 5) tabs.drop(4) else emptyList()
    Column {
        HorizontalDivider(color = Color(0xFFF1F5F9))
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color.White)
                .navigationBarsPadding()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            primary.forEach { t ->
                val terpilih = t == aktif
                val warna = if (terpilih) Color(0xFFEA580C) else Color(0xFF94A3B8)
                Column(
                    Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clickable { onPilih(t) }
                        .padding(horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    IkonBerlencana(t.icon, t.label, warna, lencana[t] ?: 0)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        t.label,
                        color = warna,
                        fontSize = 10.sp,
                        fontWeight = if (terpilih) FontWeight.Black else FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            if (overflow.isNotEmpty()) Box(Modifier.weight(1f)) {
                val selected = aktif in overflow
                val color = if (selected) Color(0xFFEA580C) else Color(0xFF94A3B8)
                Column(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { moreOpen = true },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    // Angka pada "Lainnya" adalah jumlah seluruh tujuan di dalamnya:
                    // yang tersembunyi di laci justru paling butuh ditunjuk.
                    IkonBerlencana(
                        Icons.Default.MoreHoriz,
                        "Lainnya",
                        color,
                        overflow.sumOf { lencana[it] ?: 0 },
                    )
                    Spacer(Modifier.height(3.dp))
                    Text("Lainnya", color = color, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    // Lembar, bukan DropdownMenu: daftarnya sudah 6-10 tujuan untuk sebagian peran,
    // dan dropdown menggantung di pojok kanan bawah memotong labelnya sendiri —
    // terlihat pada tangkapan layar peran pengawas. Bentuknya disamakan dengan
    // lembar menu modul Manager supaya berpindah modul tidak berarti belajar ulang.
    if (moreOpen && overflow.isNotEmpty()) {
        ModalBottomSheet(onDismissRequest = { moreOpen = false }, sheetState = sheetState) {
            IsiMenuStok(aktif, overflow, lencana) { tujuan ->
                // Lembar ditutup dengan animasinya sendiri lebih dulu; menutup paksa
                // bersamaan dengan perpindahan tab membuat isinya berkedip.
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    moreOpen = false
                    onPilih(tujuan)
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

/**
 * Isi lembar "Lainnya" — tujuan overflow dikelompokkan seperti sidebar web.
 *
 * Tujuan yang belum tercatat di [KELOMPOK_MENU_STOK] tidak dibuang diam-diam,
 * melainkan dikumpulkan di kelompok terakhir. Menambah entri `TabStok` tanpa
 * mendaftarkannya di sini seharusnya membuat menunya terlihat salah tempat, bukan
 * membuatnya lenyap dari aplikasi.
 */
@Composable
private fun IsiMenuStok(
    aktif: TabStok,
    overflow: List<TabStok>,
    lencana: Map<TabStok, Int>,
    onPilih: (TabStok) -> Unit,
) {
    val terkelompok = KELOMPOK_MENU_STOK.flatMap { it.second }.toSet()
    val sisa = overflow.filter { it !in terkelompok }

    // Kelompok kosong disaring SEBELUM perulangan, bukan lewat `return@forEach` di
    // dalamnya: keluar-awal dari lambda inline yang memancarkan composable merusak
    // tabel slot Compose.
    val kelompok = KELOMPOK_MENU_STOK
        .map { (judul, isi) -> judul to isi.filter { it in overflow } }
        .filter { (_, terlihat) -> terlihat.isNotEmpty() }
        .plus(if (sisa.isEmpty()) emptyList() else listOf("Lainnya" to sisa))

    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        kelompok.forEach { (judul, terlihat) ->
            Text(
                judul.uppercase(),
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.9.sp,
            )
            Spacer(Modifier.height(12.dp))
            terlihat.forEach { tujuan ->
                BarisMenuStok(tujuan, aktif == tujuan, lencana[tujuan] ?: 0) { onPilih(tujuan) }
                Spacer(Modifier.height(10.dp))
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
private fun BarisMenuStok(tujuan: TabStok, aktif: Boolean, jumlah: Int, onKlik: () -> Unit) {
    val oranye = Color(0xFFEA580C)
    Surface(
        Modifier.fillMaxWidth().clickable(onClick = onKlik),
        shape = RoundedCornerShape(18.dp),
        color = if (aktif) oranye else Color.White,
        border = BorderStroke(1.dp, if (aktif) oranye else Color(0xFFF1F5F9)),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 17.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                tujuan.icon,
                null,
                tint = if (aktif) Color.White else oranye,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(14.dp))
            Text(
                tujuan.labelPanjang,
                Modifier.weight(1f),
                color = if (aktif) Color.White else Color(0xFF701604),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (jumlah > 0) {
                Spacer(Modifier.width(10.dp))
                Surface(
                    shape = RoundedCornerShape(9.dp),
                    // Di baris terpilih latarnya sudah oranye, jadi lencana merah di
                    // atasnya nyaris tak terbaca; dibalik jadi putih dengan angka oranye.
                    color = if (aktif) Color.White else MERAH_LENCANA,
                ) {
                    Text(
                        teksLencana(jumlah),
                        Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        color = if (aktif) Color(0xFFEA580C) else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
    }
}

private val MERAH_LENCANA = Color(0xFFEF4444)

/** Angka lencana dipendekkan seperti web: di atas sembilan cukup "9+". */
private fun teksLencana(jumlah: Int): String = if (jumlah > 9) "9+" else jumlah.toString()

/**
 * Ikon tab dengan angka merah di pojok kanan atas.
 *
 * [jumlah] nol berarti TIDAK ada lencana sama sekali, bukan lencana bertuliskan "0":
 * nol adalah kabar baik dan tidak perlu menuntut perhatian.
 */
@Composable
private fun IkonBerlencana(
    ikon: ImageVector,
    label: String,
    warna: Color,
    jumlah: Int,
) {
    Box {
        Icon(ikon, label, tint = warna, modifier = Modifier.size(21.dp))
        if (jumlah > 0) {
            Surface(
                Modifier
                    .align(Alignment.TopEnd)
                    // Digeser keluar kotak ikon supaya tidak menutupi gambarnya;
                    // ikonnya 21dp, jadi lencana yang duduk di dalam akan menelan
                    // sebagian besar bentuk yang justru jadi penanda tab.
                    .offset(x = 7.dp, y = (-5).dp),
                shape = RoundedCornerShape(9.dp),
                color = MERAH_LENCANA,
                border = BorderStroke(1.5.dp, Color.White),
            ) {
                Text(
                    teksLencana(jumlah),
                    Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
