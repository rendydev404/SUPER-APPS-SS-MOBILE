package com.sukashawarma.superapp.feature.stok.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.RealtimeTables
import com.sukashawarma.superapp.core.ui.kaca.BarisMenuKaca
import com.sukashawarma.superapp.core.ui.kaca.BilahTabKaca
import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.core.ui.kaca.ItemTabKaca
import com.sukashawarma.superapp.core.ui.kaca.JudulKelompokMenuKaca
import com.sukashawarma.superapp.core.ui.kaca.LembarMenuKaca
import com.sukashawarma.superapp.core.ui.kaca.ShellKaca
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.feature.stok.data.MutasiRepository
import com.sukashawarma.superapp.feature.stok.data.ReturRepository
import com.sukashawarma.superapp.feature.stok.data.WasteApprovalAccess
import com.sukashawarma.superapp.feature.stok.domain.MutasiBadge
import com.sukashawarma.superapp.feature.stok.domain.LencanaRetur
import com.sukashawarma.superapp.feature.stok.domain.StokAkses
import com.sukashawarma.superapp.feature.stok.ui.area.HargaBahanScreen
import com.sukashawarma.superapp.feature.stok.ui.area.WasteApprovalScreen
import com.sukashawarma.superapp.feature.stok.ui.entri.EntriManualScreen
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
import com.sukashawarma.superapp.feature.stok.ui.retur.ReturScreen
import com.sukashawarma.superapp.feature.stok.ui.vendor.TerimaVendorScreen


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
    DASHBOARD("Dashboard", "Monitoring Stok", IkonIos.Dashboard),
    PERMINTAAN("Permintaan", "Permintaan Bahan", IkonIos.Assignment),
    OPNAME("Opname", "Stok Opname", IkonIos.Description),
    TERIMA_PO("Terima PO", "Penerimaan PO Supplier", IkonIos.LocalShipping),
    TERIMA_VENDOR("Terima Vendor", "Terima dari Vendor", IkonIos.LocalShipping),
    WASTE("Waste", "Persetujuan Waste", IkonIos.DeleteSweep),
    LEDGER("Ledger", "Buku Ledger Stok", IkonIos.MenuBook),
    MUTASI("Mutasi", "Mutasi Antar Outlet", IkonIos.SwapHoriz),
    RETUR("Retur", "Retur & Refund Bahan", IkonIos.Restore),
    ENTRI("Entri Manual", "Entri Manual & Lapor Waste", IkonIos.EditNote),
    PERSETUJUAN_OPNAME("Approval Opname", "Persetujuan Opname", IkonIos.FactCheck),
    HARGA("Harga Bahan", "Master Harga Bahan Baku", IkonIos.Sell),

    // Kelompok "ANALISIS & LAPORAN" di AppSidebar.tsx web. Label panjangnya disalin
    // apa adanya supaya orang yang berpindah dari browser ke HP mencari nama yang sama.
    NILAI_PERSEDIAAN("Nilai Stok", "Nilai Persediaan", IkonIos.Savings),
    HPP_MENU("HPP Menu", "HPP Setiap Menu", IkonIos.Calculate),
    LAPORAN_PENJUALAN("Penjualan", "Laporan Penjualan", IkonIos.TrendingUp),
    PLAFON("Plafon & Belanja", "Plafon & Belanja Outlet", IkonIos.AccountBalanceWallet),
    ARUS_BARANG("In/Out", "Inbound / Outbound", IkonIos.ImportExport),
    THRESHOLD("Threshold", "Pengaturan Threshold", IkonIos.Tune),
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
    val bolehVendor = StokAkses.bisaTerimaVendor(staff?.outletId)

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
        // Tanpa gerbang peran: web memasukkan `/stok/refund` ke kitchenAdminMoreItems,
        // leaderMoreItems, DAN crewMoreItems. Yang berbeda per peran hanya tab dan
        // tombol di dalamnya — lihat ReturAkses.
        add(TabStok.RETUR)
        add(TabStok.ENTRI)
        if (bolehWaste && TabStok.WASTE !in this) add(TabStok.WASTE)
        if (bolehPo && TabStok.TERIMA_PO !in this) add(TabStok.TERIMA_PO)
        if (bolehVendor && TabStok.TERIMA_VENDOR !in this) add(TabStok.TERIMA_VENDOR)
        add(TabStok.PERSETUJUAN_OPNAME)
        if (bolehHarga) add(TabStok.HARGA)

        // Kelompok "ANALISIS & LAPORAN" web, urutannya mengikuti AppSidebar.tsx & BottomNav.
        // Tiap menu punya daftar peran sendiri — lihat StokAkses.
        if (StokAkses.melihatPlafonBelanja(role)) add(TabStok.PLAFON)
        if (StokAkses.melihatNilaiPersediaan(role)) add(TabStok.NILAI_PERSEDIAAN)
        if (StokAkses.melihatHppMenu(role)) add(TabStok.HPP_MENU)
        if (StokAkses.melihatLaporanPenjualan(role)) add(TabStok.LAPORAN_PENJUALAN)
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

/**
 * Jumlah tiket retur yang menunggu tindakan pengguna — cermin `usePendingReturBadge`.
 *
 * Bentuknya sengaja sama dengan [lencanaMutasi], termasuk kegagalan yang diabaikan
 * diam-diam: lencana adalah petunjuk, dan pesan galat di bilah bawah yang selalu
 * terlihat lebih mengganggu daripada angka yang telat menyusul.
 *
 * Yang ditarik hanya status dan outlet tiap tiket, bukan seluruh kartu beserta
 * itemnya: angka ini hidup di bilah bawah SELURUH modul Stok, jadi ia ikut termuat
 * di layar mana pun yang sedang dibuka.
 */
@Composable
private fun lencanaRetur(): Int {
    val staff by AppSession.staff.collectAsState()
    val role = staff?.role
    val outletId = staff?.outletId
    var jumlah by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    suspend fun muat() {
        runCatching { ReturRepository.statusUntukBadge() }
            .onSuccess { jumlah = LencanaRetur.hitung(it, role, outletId) }
            .onFailure { android.util.Log.w("StokShell", "lencana retur gagal", it) }
    }

    LaunchedEffect(staff) {
        if (staff != null) muat()
    }
    RealtimeRefresh(RealtimeTables.RETUR) {
        if (staff != null) scope.launch { muat() }
    }
    return if (staff == null) 0 else jumlah
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StokShell(
    onKeluar: () -> Unit,
    onBukaBahan: (outletId: String, bahanId: String, nama: String) -> Unit,
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
    // mengubah tanda tangan bilah tab lagi.
    val lencana = mapOf(
        TabStok.MUTASI to lencanaMutasi(),
        TabStok.RETUR to lencanaRetur(),
    )

    // Tab bar kaca & lembar "Lainnya" dari design system bersama (core.ui.kaca).
    // Isi digambar sampai dasar layar di balik kapsul, jadi tiap layar memberi ruang
    // sendiri lewat `denganRuangNav()` / `navigationBarsPaddingKaca()`; blur latar
    // saat lembar terbuka diurus ShellKaca secara otomatis.
    val primary = utama
    val overflow = if (tabs.size > 5) tabs.drop(4) else emptyList()
    var menuLainnya by rememberSaveable { mutableStateOf(false) }
    val lembar = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ShellKaca(
        bilah = { latar ->
            // Angka pada "Lainnya" adalah jumlah seluruh tujuan di dalamnya: yang
            // tersembunyi di laci justru paling butuh ditunjuk.
            val item = primary.map { ItemTabKaca(it.label, it.icon, lencana[it] ?: 0) } +
                if (overflow.isEmpty()) emptyList()
                else listOf(ItemTabKaca("Lainnya", IkonIos.MoreHoriz, overflow.sumOf { lencana[it] ?: 0 }))
            // Tujuan di laci menyalakan slot "Lainnya".
            val indeks = when (tab) {
                in primary -> primary.indexOf(tab)
                in overflow -> primary.size
                else -> -1
            }
            BilahTabKaca(item, indeks, latar, onPilih = { i ->
                if (i < primary.size) tab = primary[i] else menuLainnya = true
            })
        },
    ) {
        when (tab) {
            TabStok.DASHBOARD -> MonitoringScreen(
                onKeluar = onKeluar,
                onBukaBahan = onBukaBahan,
                onBukaTransfer = onBukaTransfer,
            )
            TabStok.PERMINTAAN -> PermintaanScreen()
            TabStok.OPNAME -> OpnameScreen()
            TabStok.LEDGER -> LedgerScreen(onEntriManual = { tab = TabStok.ENTRI })
            TabStok.MUTASI -> MutasiScreen()
            TabStok.RETUR -> ReturScreen(onBack = { tab = tabs.first() })
            TabStok.ENTRI -> EntriManualScreen(onBack = { tab = tabs.first() })
            TabStok.PERSETUJUAN_OPNAME -> PersetujuanOpnameScreen(onBack = { tab = tabs.first() })
            TabStok.TERIMA_PO -> PenerimaanPoScreen(onBack = { tab = tabs.first() })
            TabStok.TERIMA_VENDOR ->
                if (StokAkses.bisaTerimaVendor(staff?.outletId)) TerimaVendorScreen(onBack = { tab = tabs.first() })
                else KeadaanTidakBerhak("Modul ini hanya untuk staf yang terhubung ke outlet.")
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
                else KeadaanTidakBerhak("Plafon belanja hanya untuk pimpinan, pengawas, dan kantor pusat.")
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

        // Lembar, bukan DropdownMenu: daftarnya sudah 6-10 tujuan untuk sebagian peran,
        // dan dropdown menggantung di pojok kanan bawah memotong labelnya sendiri.
        if (menuLainnya && overflow.isNotEmpty()) {
            LembarMenuKaca(onTutup = { menuLainnya = false }, sheetState = lembar, judul = "Menu Stok") {
                IsiMenuStok(tab, overflow, lencana) { tujuan ->
                    // Lembar ditutup dengan animasinya sendiri lebih dulu; menutup paksa
                    // bersamaan dengan perpindahan tab membuat isinya berkedip.
                    scope.launch { lembar.hide() }.invokeOnCompletion {
                        menuLainnya = false
                        tab = tujuan
                    }
                }
            }
        }
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
        TabStok.TERIMA_VENDOR,
        TabStok.RETUR,
        TabStok.LEDGER,
        TabStok.MUTASI,
        TabStok.ENTRI,
        TabStok.WASTE,
        TabStok.TERIMA_PO,
        TabStok.PERSETUJUAN_OPNAME,
    ),
    "Analisis & Laporan" to listOf(
        TabStok.PLAFON,
        TabStok.HARGA,
        TabStok.NILAI_PERSEDIAAN,
        TabStok.HPP_MENU,
        TabStok.LAPORAN_PENJUALAN,
        TabStok.ARUS_BARANG,
        TabStok.THRESHOLD,
    ),
)

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

    kelompok.forEach { (judul, terlihat) ->
        JudulKelompokMenuKaca(judul)
        terlihat.forEach { tujuan ->
            BarisMenuKaca(
                ikon = tujuan.icon,
                label = tujuan.labelPanjang,
                aktif = aktif == tujuan,
                lencana = lencana[tujuan] ?: 0,
                onKlik = { onPilih(tujuan) },
            )
        }
        Spacer(Modifier.height(14.dp))
    }
}
