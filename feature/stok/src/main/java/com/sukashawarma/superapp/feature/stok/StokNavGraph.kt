package com.sukashawarma.superapp.feature.stok

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
import com.sukashawarma.superapp.feature.stok.ui.detail.DetailBahanScreen
import com.sukashawarma.superapp.feature.stok.ui.StokShell
import com.sukashawarma.superapp.feature.stok.ui.produksi.ProduksiScreen
import com.sukashawarma.superapp.feature.stok.ui.transfer.TransferScreen

/**
 * Navigasi modul Stok. Mengikuti pola yang sama dengan modul Absensi: satu NavHost
 * bersarang yang dipasang pada satu rute di NavHost root.
 */
@Composable
fun StokNavGraph(onExit: () -> Unit) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = StokRoutes.MONITORING,
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
        composable(StokRoutes.MONITORING) {
            StokShell(
                onKeluar = onExit,
                onBukaBahan = { outletId, bahanId, nama ->
                    navController.navigate(StokRoutes.detail(outletId, bahanId, nama))
                },
                onBukaProduksi = { outletId -> navController.navigate(StokRoutes.produksi(outletId)) },
                onBukaTransfer = { navController.navigate(StokRoutes.TRANSFER) },
            )
        }

        composable(
            StokRoutes.DETAIL,
            arguments = listOf(
                navArgument("outletId") { type = NavType.StringType },
                navArgument("bahanId") { type = NavType.StringType },
                navArgument("nama") { type = NavType.StringType },
            ),
        ) { entry ->
            DetailBahanScreen(
                outletId = entry.arguments?.getString("outletId").orEmpty(),
                bahanId = entry.arguments?.getString("bahanId").orEmpty(),
                namaAwal = entry.arguments?.getString("nama").orEmpty(),
                onKeluar = { navController.popBackStack() },
            )
        }

        composable(
            StokRoutes.PRODUKSI,
            arguments = listOf(navArgument("outletId") { type = NavType.StringType }),
        ) { entry ->
            ProduksiScreen(
                outletId = entry.arguments?.getString("outletId").orEmpty(),
                onKeluar = { navController.popBackStack() },
            )
        }

        composable(StokRoutes.TRANSFER) {
            TransferScreen(onKeluar = { navController.popBackStack() })
        }
    }
}
