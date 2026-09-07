package com.sukashawarma.superapp.feature.stok.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.feature.stok.data.WasteApprovalAccess
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
        BottomNavStok(tab, tabs) { tab = it }
    }
}

@Composable
private fun BottomNavStok(aktif: TabStok, tabs: List<TabStok>, onPilih: (TabStok) -> Unit) {
    var moreOpen by rememberSaveable { mutableStateOf(false) }
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
                    Icon(t.icon, t.label, tint = warna, modifier = Modifier.size(21.dp))
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
                    Icon(Icons.Default.MoreHoriz, "Lainnya", tint = color, modifier = Modifier.size(21.dp))
                    Spacer(Modifier.height(3.dp))
                    Text("Lainnya", color = color, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
                DropdownMenu(moreOpen, { moreOpen = false }) {
                    overflow.forEach { destination ->
                        DropdownMenuItem(
                            text = { Text(destination.labelPanjang) },
                            leadingIcon = { Icon(destination.icon, null) },
                            onClick = { moreOpen = false; onPilih(destination) },
                        )
                    }
                }
            }
        }
    }
}
