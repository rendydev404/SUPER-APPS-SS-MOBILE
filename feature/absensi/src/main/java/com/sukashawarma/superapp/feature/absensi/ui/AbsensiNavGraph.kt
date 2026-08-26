@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.sukashawarma.superapp.presentation.absensi

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp)
            .navigationBarsPadding(),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp),
        ) {
            // The base is one continuous surface. Only its top edge makes a liquid notch for
            // the floating selected item, so the navbar never splits at the bottom.
            val bubbleSize = 54.dp
            val navHeight = 66.dp
            // Only the tappable item rail is inset. The background itself is full width, which
            // keeps both outer corners and the crescent smooth on Home and More.
            val itemRailInset = 18.dp
            val tabWidth = (maxWidth - (itemRailInset * 2)) / ABSENSI_BOTTOM_TABS.size
            val bubbleX by androidx.compose.animation.core.animateDpAsState(
                targetValue = itemRailInset + (tabWidth * selectedIndex) + ((tabWidth - bubbleSize) / 2),
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
                label = "magicNavBubblePosition",
            )
            val bubbleCenter = bubbleX + (bubbleSize / 2)

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(navHeight)
                    .align(Alignment.BottomCenter),
                shape = LiquidBottomNavShape(
                    bubbleCenter = bubbleCenter,
                    bubbleSize = bubbleSize,
                    navTopOffset = 24.dp,
                    isEdgeTab = selectedIndex == 0 || selectedIndex == ABSENSI_BOTTOM_TABS.lastIndex,
                ),
                color = SukaSurfaceContainerLowest,
                shadowElevation = 10.dp,
                border = BorderStroke(1.dp, SukaSurfaceContainerHighest),
            ) {}

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(navHeight)
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = itemRailInset, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ABSENSI_BOTTOM_TABS.forEach { (label, icon, index) ->
                    FloatingNavItem(
                        label = label,
                        icon = icon,
                        selected = selectedIndex == index,
                        onClick = { onSelect(index) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .size(bubbleSize)
                    .align(Alignment.TopStart)
                    .offset(x = bubbleX),
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 10.dp,
                border = BorderStroke(1.dp, SukaOrange.copy(alpha = 0.16f)),
            ) {
                Icon(
                    imageVector = ABSENSI_BOTTOM_TABS[selectedIndex].second,
                    contentDescription = "Tab ${ABSENSI_BOTTOM_TABS[selectedIndex].first}",
                    tint = SukaOrange,
                    modifier = Modifier.padding(14.dp),
                )
            }
        }
    }
}

/** A continuous navigation bar whose top edge flows around the selected floating bubble. */
private class LiquidBottomNavShape(
    private val bubbleCenter: Dp,
    private val bubbleSize: Dp,
    private val navTopOffset: Dp,
    private val isEdgeTab: Boolean,
) : Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        with(density) {
            val outerRadius = 18.dp.toPx()
            // The cutout follows the lower arc of a circle, not a hand-shaped bezier. Its
            // radius is slightly larger than the floating bubble, which gives the clean
            // crescent / moon-shaped breathing space around it.
            val bubbleRadius = (bubbleSize / 2).toPx()
            // On the first/last tab the cutout is intentionally tighter. A wide crescent is
            // elegant in the middle but becomes a long, uneven wave near an outer corner.
            val cutoutRadius = bubbleRadius + (if (isEdgeTab) 2.dp else 5.dp).toPx()
            val cutoutCenterY = bubbleRadius - navTopOffset.toPx()
            val horizontalRadius = kotlin.math.sqrt(
                (cutoutRadius * cutoutRadius) - (cutoutCenterY * cutoutCenterY),
            )
            // Compose may ask for an outline before the bottom bar receives its final width.
            // On that transient tiny size, the crescent cannot fit; use a plain rectangle
            // rather than coercing with an invalid min/max range and crashing the app.
            if (size.width <= 0f || size.height <= 0f || horizontalRadius * 2f > size.width) {
                return Outline.Rectangle(Rect(0f, 0f, size.width.coerceAtLeast(0f), size.height.coerceAtLeast(0f)))
            }
            val center = bubbleCenter.toPx().coerceIn(horizontalRadius, size.width - horizontalRadius)
            val cutoutLeft = center - horizontalRadius
            val cutoutRight = center + horizontalRadius
            val arcStartAngle = 180f + Math.toDegrees(
                kotlin.math.asin((cutoutCenterY / cutoutRadius).toDouble()),
            ).toFloat()
            val arcSweep = -(180f + (2f * (arcStartAngle - 180f)))
            // Rounded shoulders blend the horizontal top edge into the circular cutout.  A
            // direct line-to-arc join would be geometrically correct but visually too sharp.
            val shoulderWidth = (if (isEdgeTab) 5.dp else 7.dp).toPx()
            val shoulderAngle = 16f
            val shoulderTangent = 4.dp.toPx()
            val visibleArcStart = arcStartAngle - shoulderAngle
            val visibleArcEnd = (arcStartAngle + arcSweep) + shoulderAngle
            fun circlePoint(angle: Float): Pair<Float, Float> {
                val radians = Math.toRadians(angle.toDouble())
                return Pair(
                    center + (cutoutRadius * kotlin.math.cos(radians)).toFloat(),
                    cutoutCenterY + (cutoutRadius * kotlin.math.sin(radians)).toFloat(),
                )
            }
            val (leftArcX, leftArcY) = circlePoint(visibleArcStart)
            val (rightArcX, rightArcY) = circlePoint(visibleArcEnd)
            val leftRadians = Math.toRadians(visibleArcStart.toDouble())
            val rightRadians = Math.toRadians(visibleArcEnd.toDouble())
            val cutoutBounds = Rect(
                left = center - cutoutRadius,
                top = cutoutCenterY - cutoutRadius,
                right = center + cutoutRadius,
                bottom = cutoutCenterY + cutoutRadius,
            )
            val path = Path().apply {
                moveTo(0f, outerRadius)
                quadraticBezierTo(0f, 0f, outerRadius, 0f)
                lineTo(cutoutLeft - shoulderWidth, 0f)
                cubicTo(
                    cutoutLeft - (shoulderWidth * 0.35f), 0f,
                    leftArcX - (kotlin.math.sin(leftRadians) * shoulderTangent).toFloat(),
                    leftArcY + (kotlin.math.cos(leftRadians) * shoulderTangent).toFloat(),
                    leftArcX,
                    leftArcY,
                )
                arcTo(cutoutBounds, visibleArcStart, visibleArcEnd - visibleArcStart, false)
                cubicTo(
                    rightArcX + (kotlin.math.sin(rightRadians) * shoulderTangent).toFloat(),
                    rightArcY - (kotlin.math.cos(rightRadians) * shoulderTangent).toFloat(),
                    cutoutRight + (shoulderWidth * 0.35f), 0f,
                    cutoutRight + shoulderWidth,
                    0f,
                )
                lineTo(size.width - outerRadius, 0f)
                quadraticBezierTo(size.width, 0f, size.width, outerRadius)
                lineTo(size.width, size.height - outerRadius)
                quadraticBezierTo(size.width, size.height, size.width - outerRadius, size.height)
                lineTo(outerRadius, size.height)
                quadraticBezierTo(0f, size.height, 0f, size.height - outerRadius)
                close()
            }
            return Outline.Generic(path)
        }
    }
}

@Composable
private fun FloatingNavItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor by animateColorAsState(
        if (selected) SukaOrange else SukaOnSurfaceVariant,
        label = "navContentColor",
    )
    val iconAlpha by animateFloatAsState(
        if (selected) 0f else 1f,
        animationSpec = tween(140),
        label = "navIconAlpha",
    )

    Column(
        modifier = modifier
            .height(54.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 3.dp, vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier
                .size(21.dp)
                .graphicsLayer {
                    alpha = iconAlpha
                },
        )
        Spacer(Modifier.size(1.dp))
        Text(
            text = label,
            color = contentColor,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
        )
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
                                animationSpec = tween(420, easing = FastOutSlowInEasing)
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
