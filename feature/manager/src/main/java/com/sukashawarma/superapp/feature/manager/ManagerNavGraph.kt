package com.sukashawarma.superapp.feature.manager

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.feature.manager.domain.ManagerAkses
import com.sukashawarma.superapp.feature.manager.ui.laporan.LaporanScreen
import com.sukashawarma.superapp.feature.manager.ui.overview.OverviewScreen
import com.sukashawarma.superapp.feature.manager.ui.waste.WasteScreen

object ManagerRoutes {
    const val OVERVIEW = "manager_overview"
    const val WASTE = "manager_waste"
    const val LAPORAN = "manager_laporan"
}

/**
 * Graph modul Manager. Layar berikutnya (Persetujuan, Sidak Inventaris, Resep &
 * HPP, Tim, Petty Cash) cukup ditambahkan sebagai composable di sini.
 */
@Composable
fun ManagerNavGraph(onExit: () -> Unit) {
    val staff by AppSession.staff.collectAsState()

    // Gerbang kedua, setelah kartu modul di Beranda yang sudah disembunyikan. Web
    // menegakkan aturan ini dua kali juga (middleware lalu route API), dan alasannya
    // sama: kartu yang tersembunyi bukan kendali akses, hanya kerapian tampilan.
    val boleh = ManagerAkses.bolehMembuka(staff?.role)
    LaunchedEffect(boleh) { if (!boleh) onExit() }
    if (!boleh) return

    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = ManagerRoutes.OVERVIEW) {
        composable(ManagerRoutes.OVERVIEW) {
            OverviewScreen(
                onExit = onExit,
                onBukaWaste = { navController.navigate(ManagerRoutes.WASTE) },
                onBukaLaporan = { navController.navigate(ManagerRoutes.LAPORAN) },
            )
        }
        composable(ManagerRoutes.WASTE) {
            WasteScreen(onExit = { navController.popBackStack() })
        }
        composable(ManagerRoutes.LAPORAN) {
            LaporanScreen(onExit = { navController.popBackStack() })
        }
    }
}
