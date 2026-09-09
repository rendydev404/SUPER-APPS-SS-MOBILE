package com.sukashawarma.superapp.feature.manager

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.RealtimeTables
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.feature.manager.domain.ManagerAkses
import com.sukashawarma.superapp.feature.manager.ui.IsiMenuManager
import com.sukashawarma.superapp.feature.manager.ui.LencanaNavViewModel
import com.sukashawarma.superapp.feature.manager.ui.NavBawahManager
import com.sukashawarma.superapp.feature.manager.ui.TujuanManager
import com.sukashawarma.superapp.feature.manager.domain.mengisiInventaris
import com.sukashawarma.superapp.feature.manager.ui.hpp.HppScreen
import com.sukashawarma.superapp.feature.manager.ui.inventaris.InventarisScreen
import com.sukashawarma.superapp.feature.manager.ui.inventaris.LaporanInventarisScreen
import com.sukashawarma.superapp.feature.manager.ui.laporan.LaporanScreen
import com.sukashawarma.superapp.feature.manager.ui.monitoring.MonitoringScreen
import com.sukashawarma.superapp.feature.manager.ui.overview.OverviewScreen
import com.sukashawarma.superapp.feature.manager.ui.persetujuan.PersetujuanScreen
import com.sukashawarma.superapp.feature.manager.ui.pettycash.PettyCashScreen
import com.sukashawarma.superapp.feature.manager.ui.sidak.SidakScreen
import com.sukashawarma.superapp.feature.manager.ui.waste.WasteScreen
import kotlinx.coroutines.launch

/**
 * Graph modul Manager beserta nav bawahnya.
 *
 * Bentuknya mengikuti `ManagerLayout.tsx` web: empat tujuan utama langsung di
 * bilah bawah, sisanya lewat lembar "Menu". Bilahnya hidup di sini, bukan di tiap
 * layar, supaya berpindah tab tidak menggambar ulang navigasinya — dan supaya
 * tidak ada layar yang bisa lupa memasangnya.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerNavGraph(onExit: () -> Unit, tujuanAwal: TujuanManager? = null) {
    val staff by AppSession.staff.collectAsState()

    // Gerbang kedua, setelah kartu modul di Beranda yang sudah disembunyikan. Web
    // menegakkan aturan ini dua kali juga (middleware lalu route API), dan alasannya
    // sama: kartu yang tersembunyi bukan kendali akses, hanya kerapian tampilan.
    val boleh = ManagerAkses.bolehMembuka(staff?.role)
    LaunchedEffect(boleh) { if (!boleh) onExit() }
    if (!boleh) return

    val navController = rememberNavController()
    val lencanaVm: LencanaNavViewModel = viewModel()
    val lencana by lencanaVm.lencana.collectAsState()
    val entry by navController.currentBackStackEntryAsState()
    val aktif = TujuanManager.entries.find { it.rute == entry?.destination?.route }
        ?: TujuanManager.OVERVIEW

    // Resep & HPP hanya untuk regional manager — area manager tidak diberi jalannya
    // sama sekali, cermin redirect di `app/resep/page.tsx`.
    val bolehResep = ManagerAkses.melihatResepHpp(staff?.role)
    val tujuanTerlihat = TujuanManager.entries
        .filter { it != TujuanManager.RESEP || bolehResep }
        .toSet()

    var menuTerbuka by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    RealtimeRefresh(
        RealtimeTables.CANCELLATION_REQUESTS,
        RealtimeTables.WASTE_REPORTS,
    ) { lencanaVm.muatUlang() }

    /**
     * Berpindah tujuan tanpa menumpuk riwayat. Tanpa `popUpTo`, menekan Kembali
     * setelah berkeliling lima tab akan memutar ulang seluruh kunjungan itu satu
     * per satu sebelum akhirnya keluar dari modul.
     */
    fun pindah(tujuan: TujuanManager) {
        if (tujuan == aktif) return
        navController.navigate(tujuan.rute) {
            popUpTo(TujuanManager.OVERVIEW.rute) { inclusive = false }
            launchSingleTop = true
        }
    }

    /**
     * Tujuan dari notifikasi yang diketuk.
     *
     * Dibuka lewat [pindah], bukan dengan menukar `startDestination`: Overview tetap
     * jadi akar, sehingga menekan Kembali dari sini mendarat di Overview seperti
     * dari tab mana pun — bukan langsung terlempar keluar modul.
     *
     * Tujuan yang tidak boleh dilihat peran ini diabaikan; notifikasi datang dari
     * jaringan dan tidak berwenang membuka layar yang digerbangi.
     */
    LaunchedEffect(tujuanAwal) {
        if (tujuanAwal != null && tujuanAwal in tujuanTerlihat) pindah(tujuanAwal)
    }

    Scaffold(
        bottomBar = {
            NavBawahManager(
                aktif = aktif,
                jumlahPersetujuan = lencana.persetujuan,
                onPilih = { pindah(it) },
                onBukaMenu = { menuTerbuka = true },
            )
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = TujuanManager.OVERVIEW.rute,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding()),
        ) {
            composable(TujuanManager.OVERVIEW.rute) {
                OverviewScreen(
                    onExit = onExit,
                    onBukaWaste = { pindah(TujuanManager.WASTE) },
                )
            }
            composable(TujuanManager.LAPORAN.rute) {
                LaporanScreen(onExit = { pindah(TujuanManager.OVERVIEW) })
            }
            composable(TujuanManager.PERSETUJUAN.rute) {
                PersetujuanScreen(onExit = { pindah(TujuanManager.OVERVIEW) })
            }
            composable(TujuanManager.PETTY_CASH.rute) {
                PettyCashScreen(onExit = { pindah(TujuanManager.OVERVIEW) })
            }
            composable(TujuanManager.WASTE.rute) {
                WasteScreen(onExit = { pindah(TujuanManager.OVERVIEW) })
            }
            composable(TujuanManager.TIM.rute) {
                MonitoringScreen(onExit = { pindah(TujuanManager.OVERVIEW) })
            }
            composable(TujuanManager.SIDAK.rute) {
                SidakScreen(onExit = { pindah(TujuanManager.OVERVIEW) })
            }
            composable(TujuanManager.INVENTORI.rute) {
                // Cermin `isReportViewer` web: area manager mengisi form, regional
                // manager membuka laporannya. Bukan dua menu — satu tujuan yang
                // wajahnya ditentukan peran, persis seperti `/dashboard` di sana.
                if (mengisiInventaris(staff?.role)) {
                    InventarisScreen(onExit = { pindah(TujuanManager.OVERVIEW) })
                } else {
                    LaporanInventarisScreen(onExit = { pindah(TujuanManager.OVERVIEW) })
                }
            }
            if (bolehResep) {
                composable(TujuanManager.RESEP.rute) {
                    HppScreen(onExit = { pindah(TujuanManager.OVERVIEW) })
                }
            }
        }
    }

    if (menuTerbuka) {
        ModalBottomSheet(
            onDismissRequest = { menuTerbuka = false },
            sheetState = sheetState,
        ) {
            IsiMenuManager(
                aktif = aktif,
                tujuanTerlihat = tujuanTerlihat,
                jumlahPersetujuan = lencana.persetujuan,
                jumlahWaste = lencana.waste,
                onPilih = { tujuan ->
                    // Lembar ditutup dengan animasinya sendiri lebih dulu; menutup
                    // paksa bersamaan dengan navigasi membuat isinya berkedip.
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        menuTerbuka = false
                        pindah(tujuan)
                    }
                },
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}
