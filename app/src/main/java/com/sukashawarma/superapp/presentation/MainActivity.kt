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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.presentation.absensi.AbsensiNavGraph
import com.sukashawarma.superapp.presentation.home.HomeScreen
import com.sukashawarma.superapp.presentation.login.LoginScreen
import com.sukashawarma.superapp.presentation.theme.SukaSuperappTheme

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val ABSENSI = "absensi"
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

    LaunchedEffect(Unit) {
        AppSession.tryAutoLogin()
    }

    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    val startDestination = if (AppSession.staff.value != null) Routes.HOME else Routes.LOGIN

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.LOGIN) {
            LoginScreen(onLoggedIn = {
                navController.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } }
            })
        }
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
