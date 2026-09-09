package com.sukashawarma.superapp.feature.distribusi

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
            // Kamera tidak ikut cross-fade dengan halaman sebelumnya. CameraX
            // adalah AndroidView yang berat dan overlap selama animasi menjadi
            // sumber frame drop pada perangkat dengan resource terbatas.
            if (targetState.destination.route == DistribusiRoutes.SCAN) {
                EnterTransition.None
            } else {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    tween(180, easing = FastOutSlowInEasing),
                ) + fadeIn(tween(180))
            }
        },
        exitTransition = {
            if (targetState.destination.route == DistribusiRoutes.SCAN ||
                initialState.destination.route == DistribusiRoutes.SCAN
            ) {
                ExitTransition.None
            } else {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    tween(180, easing = FastOutSlowInEasing),
                ) + fadeOut(tween(180))
            }
        },
        popEnterTransition = {
            if (targetState.destination.route == DistribusiRoutes.SCAN) {
                EnterTransition.None
            } else {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    tween(180, easing = FastOutSlowInEasing),
                ) + fadeIn(tween(180))
            }
        },
        popExitTransition = {
            if (initialState.destination.route == DistribusiRoutes.SCAN) {
                ExitTransition.None
            } else {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    tween(180, easing = FastOutSlowInEasing),
                ) + fadeOut(tween(180))
            }
        },
    ) {
        composable(DistribusiRoutes.DASHBOARD) {
            DashboardScreen(
                onKeluar = onExit,
                onBukaScan = { navController.navigateSekali(DistribusiRoutes.SCAN) },
                onBukaRiwayat = { navController.navigateSekali(DistribusiRoutes.RIWAYAT) },
                onBukaDetail = { id -> navController.navigateSekali(DistribusiRoutes.detail(id)) },
            )
        }

        composable(DistribusiRoutes.INBOX) {
            InboxScreen(
                onKeluar = { navController.popBackStack() },
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
                onKeluar = { navController.popBackStack() },
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
                onKeluar = { navController.popBackStack() },
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
                onKeluar = { navController.popBackStack() },
                onBukaDetail = { id -> navController.navigateSekali(DistribusiRoutes.detail(id)) },
                onBukaDashboard = { navController.popBackStack(DistribusiRoutes.DASHBOARD, false) },
                onBukaScan = { navController.navigateSekali(DistribusiRoutes.SCAN) },
            )
        }

        composable(
            DistribusiRoutes.DETAIL,
            arguments = listOf(navArgument("suratJalanId") { type = NavType.StringType }),
        ) { entry ->
            DetailSuratJalanScreen(
                suratJalanId = entry.arguments?.getString("suratJalanId").orEmpty(),
                onKeluar = { navController.popBackStack() },
            )
        }
    }
}

/** Mencegah tap berulang menumpuk destination saat animasi belum selesai. */
private fun androidx.navigation.NavHostController.navigateSekali(route: String) {
    if (currentDestination?.route == route) return
    navigate(route) { launchSingleTop = true }
}
