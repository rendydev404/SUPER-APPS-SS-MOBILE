package com.sukashawarma.superapp.feature.stok

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
import com.sukashawarma.superapp.feature.stok.ui.detail.DetailBahanScreen
import com.sukashawarma.superapp.feature.stok.ui.StokShell
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
        enterTransition = { masukMaju() },
        exitTransition = { keluarMaju() },
        popEnterTransition = { masukMundur() },
        popExitTransition = { keluarMundur() },
    ) {
        composable(StokRoutes.MONITORING) {
            StokShell(
                onKeluar = onExit,
                onBukaBahan = { outletId, bahanId, nama ->
                    navController.navigateSekali(StokRoutes.detail(outletId, bahanId, nama))
                },
                onBukaTransfer = { navController.navigateSekali(StokRoutes.TRANSFER) },
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
                // Pengecualian: detail satu bahan, jadi "kembali" berarti kembali ke
                // daftarnya — bukan pulang ke Beranda seperti layar Stok lainnya.
                onKeluar = { navController.popAman() },
            )
        }

        composable(StokRoutes.TRANSFER) {
            TransferScreen(onKeluar = onExit)
        }
    }
}
