@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.sukashawarma.superapp.presentation.absensi

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sukashawarma.superapp.presentation.absensi.checklist.ChecklistManageScreen
import com.sukashawarma.superapp.presentation.absensi.checklist.ChecklistMonitorScreen
import com.sukashawarma.superapp.presentation.absensi.checklist.ChecklistScreen
import com.sukashawarma.superapp.presentation.absensi.clock.ClockScreen
import com.sukashawarma.superapp.presentation.absensi.cuti.CutiScreen
import com.sukashawarma.superapp.presentation.absensi.enroll.EnrollScreen
import com.sukashawarma.superapp.presentation.absensi.kasbon.KasbonScreen
import com.sukashawarma.superapp.presentation.absensi.papan.PapanKehadiranScreen
import com.sukashawarma.superapp.presentation.absensi.pengaturan.PengaturanScreen
import com.sukashawarma.superapp.presentation.absensi.profil.ProfilScreen
import com.sukashawarma.superapp.presentation.absensi.rekap.RekapScreen
import com.sukashawarma.superapp.presentation.components.ComingSoon
import com.sukashawarma.superapp.presentation.theme.SukaOnSurfaceVariant
import com.sukashawarma.superapp.presentation.theme.SukaOrange
import com.sukashawarma.superapp.presentation.theme.SukaSurfaceContainerHighest
import com.sukashawarma.superapp.presentation.theme.SukaSurfaceContainerLowest
import kotlinx.coroutines.launch

/**
 * Navigasi utama modul Absensi dengan dukungan gesture swipe antar tab (WhatsApp-style)
 * yang halus dan memungkinkan mengintip (*peeking*) halaman secara real-time.
 */
@Composable
fun AbsensiNavGraph(onExit: () -> Unit) {
    val navController = rememberNavController()
    // Tab yang diminta dari layar sub-route (mis. tombol bottom nav di CutiScreen) — dikonsumsi
    // oleh AbsensiMainPagerScreen begitu ia kembali ke komposisi setelah popBackStack ke MAIN.
    var pendingTab by remember { mutableStateOf<Int?>(null) }

    NavHost(
        navController = navController,
        startDestination = AbsensiRoutes.MAIN,
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(300))
        }
    ) {
        composable(AbsensiRoutes.MAIN) {
            AbsensiMainPagerScreen(
                onNavigateToSubRoute = { route -> navController.navigate(route) },
                onExit = onExit,
                pendingTab = pendingTab,
                onPendingTabConsumed = { pendingTab = null },
            )
        }
        composable(AbsensiRoutes.HUB) {
            AbsensiHubScreen(onNavigate = { navController.navigate(it) }, onExit = onExit)
        }
        composable(AbsensiRoutes.CLOCK) { ClockScreen(isActive = true, onExit = { navController.popBackStack() }) }
        composable(AbsensiRoutes.PAPAN) {
            PapanKehadiranScreen(
                onExit = { navController.popBackStack() },
                onNavigateTab = { index ->
                    pendingTab = index
                    navController.popBackStack(AbsensiRoutes.MAIN, inclusive = false)
                },
            )
        }
        composable(AbsensiRoutes.REKAP) { RekapScreen(onExit = { navController.popBackStack() }) }
        composable(AbsensiRoutes.CHECKLIST) { ChecklistScreen(onExit = { navController.popBackStack() }) }
        composable(AbsensiRoutes.CHECKLIST_MONITOR) { ChecklistMonitorScreen(onExit = { navController.popBackStack() }) }
        composable(AbsensiRoutes.CHECKLIST_MANAGE) { ChecklistManageScreen(onExit = { navController.popBackStack() }) }
        composable(AbsensiRoutes.CUTI) {
            CutiScreen(
                onExit = { navController.popBackStack() },
                onNavigateTab = { index ->
                    pendingTab = index
                    navController.popBackStack(AbsensiRoutes.MAIN, inclusive = false)
                },
            )
        }
        composable(AbsensiRoutes.KASBON) {
            KasbonScreen(
                onExit = { navController.popBackStack() },
                onNavigateTab = { index ->
                    pendingTab = index
                    navController.popBackStack(AbsensiRoutes.MAIN, inclusive = false)
                },
            )
        }
        composable(AbsensiRoutes.ENROLL) { EnrollScreen(onExit = { navController.popBackStack() }) }
        composable(AbsensiRoutes.PENGATURAN) { PengaturanScreen(onExit = { navController.popBackStack() }) }
        composable(AbsensiRoutes.PROFIL) { ProfilScreen(onExit = { navController.popBackStack() }) }
        composable(AbsensiRoutes.MANAJEMEN_KRU) { ComingSoon("Manajemen Kru") }
    }
}

/**
 * Kontainer 4 Tab Utama Absensi dengan HorizontalPager swipeable (WhatsApp-style drag & peek).
 */
private val ABSENSI_BOTTOM_TABS = listOf(
    Triple("Home", Icons.Default.Home, 0),
    Triple("Checklist", Icons.Default.Checklist, 1),
    Triple("Profile", Icons.Default.Person, 2),
    Triple("More", Icons.Default.MoreHoriz, 3)
)

/**
 * Bottom nav 4 tab yang sama dipakai di [AbsensiMainPagerScreen] (pager, `selectedIndex`
 * mengikuti halaman aktif) maupun di layar sub-route seperti [CutiScreen] (tak pernah punya
 * tab aktif sungguhan — kirim index tab asalnya, mis. 3 utk "More", supaya user tetap bisa
 * lompat ke tab lain tanpa balik dulu, dan bottom nav tidak pernah hilang begitu masuk ke
 * fitur turunan seperti Cuti & Izin).
 */
@Composable
fun AbsensiBottomNav(selectedIndex: Int, onSelect: (Int) -> Unit) {
    Surface(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = SukaSurfaceContainerLowest,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, SukaSurfaceContainerHighest)
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            tonalElevation = 0.dp
        ) {
            ABSENSI_BOTTOM_TABS.forEach { (label, icon, index) ->
                val isSelected = selectedIndex == index
                NavigationBarItem(
                    selected = isSelected,
                    onClick = { onSelect(index) },
                    icon = { Icon(icon, contentDescription = label) },
                    label = {
                        Text(
                            text = label,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SukaOrange,
                        selectedTextColor = SukaOrange,
                        indicatorColor = SukaOrange.copy(alpha = 0.12f),
                        unselectedIconColor = SukaOnSurfaceVariant,
                        unselectedTextColor = SukaOnSurfaceVariant
                    )
                )
            }
        }
    }
}

@Composable
fun AbsensiMainPagerScreen(
    onNavigateToSubRoute: (String) -> Unit,
    onExit: () -> Unit,
    pendingTab: Int? = null,
    onPendingTabConsumed: () -> Unit = {},
) {
    val pagerState = rememberPagerState(initialPage = 0) { 4 }
    val coroutineScope = rememberCoroutineScope()

    // Jika pengguna berada di tab 1, 2, atau 3, tombol back akan mengembalikan ke Tab 0 (Home).
    // Jika sudah di Tab 0, tombol back akan memanggil onExit() (keluar modul).
    BackHandler(enabled = pagerState.currentPage != 0) {
        coroutineScope.launch {
            pagerState.animateScrollToPage(
                page = 0,
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            )
        }
    }

    // Konsumsi permintaan pindah tab dari layar sub-route (mis. bottom nav CutiScreen).
    LaunchedEffect(pendingTab) {
        if (pendingTab != null) {
            pagerState.scrollToPage(pendingTab)
            onPendingTabConsumed()
        }
    }

    Scaffold(
        bottomBar = {
            AbsensiBottomNav(
                selectedIndex = pagerState.currentPage,
                onSelect = { index ->
                    if (pagerState.currentPage != index) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(
                                page = index,
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            )
                        }
                    }
                },
            )
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            userScrollEnabled = true,
            beyondBoundsPageCount = 1
        ) { page ->
            when (page) {
                0 -> ClockScreen(
                    isActive = pagerState.currentPage == 0 || pagerState.targetPage == 0,
                    onExit = onExit
                )
                1 -> ChecklistScreen(onExit = {
                    coroutineScope.launch { pagerState.animateScrollToPage(0) }
                })
                2 -> ProfilScreen(onExit = {
                    coroutineScope.launch { pagerState.animateScrollToPage(0) }
                })
                3 -> AbsensiHubScreen(
                    onNavigate = onNavigateToSubRoute,
                    onExit = {
                        coroutineScope.launch { pagerState.animateScrollToPage(0) }
                    }
                )
            }
        }
    }
}
