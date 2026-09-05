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
import com.sukashawarma.superapp.feature.manager.ui.overview.OverviewScreen

object ManagerRoutes {
    const val OVERVIEW = "manager_overview"
}

/**
 * Graph modul Manager. Baru berisi satu layar; NavHost tetap dipakai sejak awal
 * supaya layar berikutnya (Laporan, Persetujuan, Waste, Tim, Petty Cash) cukup
 * ditambahkan sebagai composable, bukan memicu penataan ulang navigasi.
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
            OverviewScreen(onExit = onExit)
        }
    }
}
