package com.sukashawarma.superapp.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sukashawarma.superapp.domain.model.Role
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.domain.session.StartDestination
import com.sukashawarma.superapp.domain.session.resolveStartDestination
import com.sukashawarma.superapp.presentation.absensi.AbsensiNavGraph
import com.sukashawarma.superapp.presentation.home.HomeScreen
import com.sukashawarma.superapp.presentation.login.LoginScreen
import com.sukashawarma.superapp.presentation.mitra.MitraDashboardScaffold
import com.sukashawarma.superapp.presentation.mitra.MitraLoadErrorScreen
import com.sukashawarma.superapp.presentation.mitra.MitraNoProfileScreen
import com.sukashawarma.superapp.presentation.theme.SukaSuperappTheme
import kotlinx.coroutines.launch

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val ABSENSI = "absensi"
    const val MITRA = "mitra"
    const val MITRA_NO_PROFILE = "mitra_no_profile"
    const val MITRA_LOAD_ERROR = "mitra_load_error"
}

@dagger.hilt.android.AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SukaSuperappTheme {
                RootNav()
            }
        }
    }
}

@Composable
private fun RootNav() {
    val navController = rememberNavController()
    val loading by AppSession.loading.collectAsState()
    val staff by AppSession.staff.collectAsState()
    val mitraProfile by AppSession.mitraProfile.collectAsState()
    val mitraLoadFailed by AppSession.mitraLoadFailed.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        AppSession.tryAutoLogin()
    }

    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    val destination = resolveStartDestination(staff, mitraProfile, mitraLoadFailed)
    val isMitra = staff?.role == Role.MITRA

    NavHost(navController = navController, startDestination = routeFor(destination)) {
        composable(Routes.LOGIN) {
            LoginScreen(onLoggedIn = {
                // Tujuan dihitung ulang dari sesi yang BARU terisi — jangan menebak di sini.
                val target = routeFor(
                    resolveStartDestination(
                        AppSession.staff.value,
                        AppSession.mitraProfile.value,
                        AppSession.mitraLoadFailed.value,
                    )
                )
                navController.navigate(target) { popUpTo(Routes.LOGIN) { inclusive = true } }
            })
        }

        if (isMitra) {
            // HOME & ABSENSI sengaja TIDAK didaftarkan untuk mitra — tak ada jalan ke sana
            // lewat Back maupun deep link. Cermin route-guard web (RoleContext.tsx).
            composable(Routes.MITRA) {
                MitraDashboardScaffold(onLoggedOut = {
                    navController.navigate(Routes.LOGIN) { popUpTo(0) }
                })
            }
            composable(Routes.MITRA_NO_PROFILE) {
                MitraNoProfileScreen(onLoggedOut = {
                    navController.navigate(Routes.LOGIN) { popUpTo(0) }
                })
            }
            composable(Routes.MITRA_LOAD_ERROR) {
                MitraLoadErrorScreen(
                    onRetry = { scope.launch { AppSession.tryAutoLogin() } },
                    onLoggedOut = {
                        navController.navigate(Routes.LOGIN) { popUpTo(0) }
                    },
                )
            }
        } else {
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenAbsensi = { navController.navigate(Routes.ABSENSI) },
                    onLoggedOut = { navController.navigate(Routes.LOGIN) { popUpTo(0) } }
                )
            }
            composable(Routes.ABSENSI) {
                AbsensiNavGraph(onExit = { navController.popBackStack() })
            }
        }
    }
}

/** Pemetaan tujuan ke string rute. Aturannya ada di resolveStartDestination, bukan di sini. */
private fun routeFor(destination: StartDestination): String = when (destination) {
    StartDestination.LOGIN -> Routes.LOGIN
    StartDestination.HOME -> Routes.HOME
    StartDestination.MITRA_DASHBOARD -> Routes.MITRA
    StartDestination.MITRA_NO_PROFILE -> Routes.MITRA_NO_PROFILE
    StartDestination.MITRA_LOAD_ERROR -> Routes.MITRA_LOAD_ERROR
}
