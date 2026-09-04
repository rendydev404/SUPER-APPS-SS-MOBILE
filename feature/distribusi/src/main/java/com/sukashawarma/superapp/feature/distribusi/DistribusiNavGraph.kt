package com.sukashawarma.superapp.feature.distribusi

import androidx.compose.animation.AnimatedContentTransitionScope
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
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Start,
                tween(280, easing = FastOutSlowInEasing),
            ) + fadeIn(tween(280))
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Start,
                tween(280, easing = FastOutSlowInEasing),
            ) + fadeOut(tween(280))
        },
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                tween(280, easing = FastOutSlowInEasing),
            ) + fadeIn(tween(280))
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                tween(280, easing = FastOutSlowInEasing),
            ) + fadeOut(tween(280))
        },
    ) {
        composable(DistribusiRoutes.DASHBOARD) {
            DashboardScreen(
                onKeluar = onExit,
                onBukaInbox = { navController.navigate(DistribusiRoutes.INBOX) },
                onBukaRiwayat = { navController.navigate(DistribusiRoutes.RIWAYAT) },
                onBukaDetail = { id -> navController.navigate(DistribusiRoutes.detail(id)) },
            )
        }

        composable(DistribusiRoutes.INBOX) {
            InboxScreen(
                onKeluar = { navController.popBackStack() },
                onBukaScan = { navController.navigate(DistribusiRoutes.SCAN) },
                onBukaDetail = { id -> navController.navigate(DistribusiRoutes.detail(id)) },
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
                        popUpTo(DistribusiRoutes.DASHBOARD)
                    }
                },
            )
        }

        composable(DistribusiRoutes.RIWAYAT) {
            RiwayatScreen(
                onKeluar = { navController.popBackStack() },
                onBukaDetail = { id -> navController.navigate(DistribusiRoutes.detail(id)) },
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
