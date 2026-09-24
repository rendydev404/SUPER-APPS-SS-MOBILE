package com.sukashawarma.superapp.feature.distribusi

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sukashawarma.superapp.core.ui.keluarMaju
import com.sukashawarma.superapp.core.ui.keluarMundur
import com.sukashawarma.superapp.core.ui.masukMaju
import com.sukashawarma.superapp.core.ui.masukMundur
import com.sukashawarma.superapp.core.ui.navigateSekali
import com.sukashawarma.superapp.core.ui.popAman
import com.sukashawarma.superapp.feature.distribusi.ui.buat.BuatSuratJalanScreen
import com.sukashawarma.superapp.feature.distribusi.ui.dashboard.DashboardScreen
import com.sukashawarma.superapp.feature.distribusi.ui.detail.DetailSuratJalanScreen
import com.sukashawarma.superapp.feature.distribusi.ui.inbox.InboxScreen
import com.sukashawarma.superapp.feature.distribusi.ui.riwayat.RiwayatScreen
import com.sukashawarma.superapp.feature.distribusi.ui.scan.ScanQrScreen
import com.sukashawarma.superapp.feature.distribusi.ui.verifikasi.VerifikasiScreen

/**
 * Navigasi modul Distribusi. Pola yang sama dengan Absensi dan Stok: satu
 * NavHost bersarang yang dipasang pada satu rute di NavHost root.
 */
@Composable
fun DistribusiNavGraph(onExit: () -> Unit) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = DistribusiRoutes.DASHBOARD,
        enterTransition = {
            // Kamera tidak ikut beranimasi dengan halaman sebelumnya. CameraX
            // adalah AndroidView yang berat dan overlap selama animasi menjadi
            // sumber frame drop pada perangkat dengan resource terbatas.
            if (targetState.destination.route == DistribusiRoutes.SCAN) {
                EnterTransition.None
            } else {
                masukMaju()
            }
        },
        exitTransition = {
            if (targetState.destination.route == DistribusiRoutes.SCAN ||
                initialState.destination.route == DistribusiRoutes.SCAN
            ) {
                ExitTransition.None
            } else {
                keluarMaju()
            }
        },
        popEnterTransition = {
            if (targetState.destination.route == DistribusiRoutes.SCAN) {
                EnterTransition.None
            } else {
                masukMundur()
            }
        },
        popExitTransition = {
            if (initialState.destination.route == DistribusiRoutes.SCAN) {
                ExitTransition.None
            } else {
                keluarMundur()
            }
        },
    ) {
        composable(DistribusiRoutes.DASHBOARD) {
            DashboardScreen(
                onKeluar = onExit,
                onBukaScan = { navController.navigateSekali(DistribusiRoutes.SCAN) },
                onBukaRiwayat = { navController.navigateSekali(DistribusiRoutes.RIWAYAT) },
                onBukaDetail = { id -> navController.navigateSekali(DistribusiRoutes.detail(id)) },
                onBukaBuat = { navController.navigateSekali(DistribusiRoutes.BUAT) },
            )
        }

        composable(DistribusiRoutes.BUAT) {
            BuatSuratJalanScreen(
                onKeluar = { navController.popAman() },
                onDibuat = { id ->
                    // Form dikeluarkan dari tumpukan: Kembali dari detail draft
                    // mendarat di dashboard, bukan di form kosong yang bisa
                    // membuat surat jalan ganda.
                    navController.navigate(DistribusiRoutes.detail(id)) {
                        launchSingleTop = true
                        popUpTo(DistribusiRoutes.BUAT) { inclusive = true }
                    }
                },
                onBukaDashboard = { navController.popBackStack(DistribusiRoutes.DASHBOARD, false) },
                onBukaRiwayat = {
                    navController.navigate(DistribusiRoutes.RIWAYAT) {
                        launchSingleTop = true
                        popUpTo(DistribusiRoutes.DASHBOARD)
                    }
                },
            )
        }

        composable(DistribusiRoutes.INBOX) {
            InboxScreen(
                onKeluar = onExit,
                onBukaScan = { navController.navigateSekali(DistribusiRoutes.SCAN) },
                onBukaDetail = { id -> navController.navigateSekali(DistribusiRoutes.detail(id)) },
                // Nav bawah berpindah antar-tab, bukan menumpuk layar: kembali
                // ke dashboard memakai popBackStack karena dashboard adalah akar.
                onBukaDashboard = { navController.popBackStack(DistribusiRoutes.DASHBOARD, false) },
                onBukaRiwayat = { navController.navigateSekali(DistribusiRoutes.RIWAYAT) },
            )
        }

        composable(DistribusiRoutes.SCAN) {
            ScanQrScreen(
                // Pengecualian: pemindai dan dua layar di bawah adalah lanjutan satu
                // alur atas satu surat jalan. "Kembali" di sini berarti membatalkan
                // langkahnya, bukan pulang ke Beranda.
                onKeluar = { navController.popAman() },
                onTerbuka = { id ->
                    // Pemindai dikeluarkan dari tumpukan: menekan Kembali dari
                    // layar verifikasi harus mendarat di inbox, bukan menyalakan
                    // kamera lagi.
                    navController.navigate(DistribusiRoutes.verifikasi(id)) {
                        launchSingleTop = true
                        popUpTo(DistribusiRoutes.SCAN) { inclusive = true }
                    }
                },
            )
        }

        composable(
            DistribusiRoutes.VERIFIKASI,
            arguments = listOf(navArgument("suratJalanId") { type = NavType.StringType }),
        ) { entry ->
            VerifikasiScreen(
                suratJalanId = entry.arguments?.getString("suratJalanId").orEmpty(),
                onKeluar = { navController.popAman() },
                onSelesai = {
                    navController.navigate(DistribusiRoutes.RIWAYAT) {
                        launchSingleTop = true
                        popUpTo(DistribusiRoutes.DASHBOARD)
                    }
                },
            )
        }

        composable(DistribusiRoutes.RIWAYAT) {
            RiwayatScreen(
                onKeluar = onExit,
                onBukaDetail = { id -> navController.navigateSekali(DistribusiRoutes.detail(id)) },
                onBukaDashboard = { navController.popBackStack(DistribusiRoutes.DASHBOARD, false) },
                onBukaScan = { navController.navigateSekali(DistribusiRoutes.SCAN) },
                onBukaBuat = {
                    navController.navigate(DistribusiRoutes.BUAT) {
                        launchSingleTop = true
                        popUpTo(DistribusiRoutes.DASHBOARD)
                    }
                },
            )
        }

        composable(
            DistribusiRoutes.DETAIL,
            arguments = listOf(navArgument("suratJalanId") { type = NavType.StringType }),
        ) { entry ->
            DetailSuratJalanScreen(
                suratJalanId = entry.arguments?.getString("suratJalanId").orEmpty(),
                onKeluar = { navController.popAman() },
            )
        }
    }
}
