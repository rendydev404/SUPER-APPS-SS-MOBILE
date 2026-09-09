package com.sukashawarma.superapp.feature.leader

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.RealtimeTables
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.feature.leader.domain.LeaderAkses
import com.sukashawarma.superapp.feature.leader.ui.LencanaViewModel
import com.sukashawarma.superapp.feature.leader.ui.NavBawahLeader
import com.sukashawarma.superapp.feature.leader.ui.TujuanLeader
import com.sukashawarma.superapp.feature.leader.ui.penjualan.PenjualanScreen
import com.sukashawarma.superapp.feature.leader.ui.pettycash.PettyCashScreen
import com.sukashawarma.superapp.feature.leader.ui.ringkasan.RingkasanScreen
import com.sukashawarma.superapp.feature.leader.ui.stok.StokScreen

/**
 * Graph modul Leader beserta nav bawahnya.
 *
 * Bentuknya mengikuti kelompok "Leader Dashboard" di `navConfig.ts` web: empat
 * tujuan, seluruhnya langsung di bilah bawah. Bilahnya hidup di sini, bukan di tiap
 * layar, supaya berpindah tab tidak menggambar ulang navigasinya — dan supaya tidak
 * ada layar yang bisa lupa memasangnya.
 */
@Composable
fun LeaderNavGraph(onExit: () -> Unit) {
    val staff by AppSession.staff.collectAsState()

    // Gerbang kedua, setelah kartu modul di Beranda yang sudah disembunyikan. Web
    // menegakkan aturan ini dua kali juga (matriks `enforceAppAccess` lalu route-guard
    // di RoleContext), dan alasannya sama: kartu yang tersembunyi bukan kendali
    // akses, hanya kerapian tampilan.
    val boleh = LeaderAkses.bolehMembuka(staff?.role)
    LaunchedEffect(boleh) { if (!boleh) onExit() }
    if (!boleh) return

    val navController = rememberNavController()
    val lencanaVm: LencanaViewModel = viewModel()
    val jumlahAksi by lencanaVm.jumlahAksi.collectAsState()
    val entry by navController.currentBackStackEntryAsState()
    val aktif = TujuanLeader.entries.find { it.rute == entry?.destination?.route }
        ?: TujuanLeader.RINGKASAN

    RealtimeRefresh(RealtimeTables.PETTY_CASH_TOPUPS) { lencanaVm.muatUlang() }

    /**
     * Berpindah tujuan tanpa menumpuk riwayat. Tanpa `popUpTo`, menekan Kembali
     * setelah berkeliling empat tab akan memutar ulang seluruh kunjungan itu satu per
     * satu sebelum akhirnya keluar dari modul.
     */
    fun pindah(tujuan: TujuanLeader) {
        if (tujuan == aktif) return
        navController.navigate(tujuan.rute) {
            popUpTo(TujuanLeader.RINGKASAN.rute) { inclusive = false }
            launchSingleTop = true
        }
    }

    Scaffold(
        bottomBar = {
            NavBawahLeader(
                aktif = aktif,
                jumlahAksi = jumlahAksi,
                onPilih = { pindah(it) },
            )
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = TujuanLeader.RINGKASAN.rute,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding()),
        ) {
            composable(TujuanLeader.RINGKASAN.rute) {
                RingkasanScreen(
                    onExit = onExit,
                    onBukaPettyCash = { pindah(TujuanLeader.PETTY_CASH) },
                    onBukaStok = { pindah(TujuanLeader.STOK) },
                    onBukaPenjualan = { pindah(TujuanLeader.PENJUALAN) },
                )
            }
            composable(TujuanLeader.PETTY_CASH.rute) {
                PettyCashScreen(onExit = { pindah(TujuanLeader.RINGKASAN) })
            }
            composable(TujuanLeader.PENJUALAN.rute) {
                PenjualanScreen(onExit = { pindah(TujuanLeader.RINGKASAN) })
            }
            composable(TujuanLeader.STOK.rute) {
                StokScreen(onExit = { pindah(TujuanLeader.RINGKASAN) })
            }
        }
    }
}
