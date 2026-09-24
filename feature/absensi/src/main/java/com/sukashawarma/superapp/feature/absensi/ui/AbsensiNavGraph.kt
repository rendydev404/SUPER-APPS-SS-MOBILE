@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.sukashawarma.superapp.presentation.absensi

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sukashawarma.superapp.core.ui.kaca.BilahTabKaca
import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.core.ui.kaca.ItemTabKaca
import com.sukashawarma.superapp.core.ui.kaca.LocalRuangNavKaca
import com.sukashawarma.superapp.core.ui.kaca.ShellKaca
import com.sukashawarma.superapp.core.ui.keluarMaju
import com.sukashawarma.superapp.core.ui.keluarMundur
import com.sukashawarma.superapp.core.ui.masukMaju
import com.sukashawarma.superapp.core.ui.masukMundur
import com.sukashawarma.superapp.core.ui.navigateSekali
import com.sukashawarma.superapp.presentation.absensi.checklist.ChecklistManageScreen
import com.sukashawarma.superapp.presentation.absensi.checklist.ChecklistMonitorScreen
import com.sukashawarma.superapp.presentation.absensi.checklist.ChecklistScreen
import com.sukashawarma.superapp.presentation.absensi.clock.ClockScreen
import com.sukashawarma.superapp.presentation.absensi.cuti.CutiScreen
import com.sukashawarma.superapp.presentation.absensi.enroll.EnrollScreen
import com.sukashawarma.superapp.presentation.absensi.kasbon.KasbonScreen
import com.sukashawarma.superapp.presentation.absensi.papan.PapanKehadiranScreen
import com.sukashawarma.superapp.presentation.absensi.pengaturan.PengaturanScreen
import com.sukashawarma.superapp.feature.profil.ui.ProfilScreen
import com.sukashawarma.superapp.presentation.absensi.rekap.RekapScreen
import com.sukashawarma.superapp.domain.model.CHECKLIST_MANAGE_ROLES
import com.sukashawarma.superapp.domain.model.Role
import com.sukashawarma.superapp.domain.model.SPV_TIER_ROLES
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.presentation.components.ComingSoon
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
        enterTransition = { masukMaju() },
        exitTransition = { keluarMaju() },
        popEnterTransition = { masukMundur() },
        popExitTransition = { keluarMundur() },
    ) {
        composable(AbsensiRoutes.MAIN) {
            AbsensiMainPagerScreen(
                onNavigateToSubRoute = { route -> navController.navigateSekali(route) },
                onExit = onExit,
                pendingTab = pendingTab,
                onPendingTabConsumed = { pendingTab = null },
            )
        }
        composable(AbsensiRoutes.HUB) {
            AbsensiHubScreen(onNavigate = { navController.navigateSekali(it) }, onExit = onExit)
        }
        composable(AbsensiRoutes.CLOCK) { ClockScreen(isActive = true, onExit = onExit) }
        composable(AbsensiRoutes.PAPAN) {
            PapanKehadiranScreen(
                onExit = onExit,
                onNavigateTab = { index ->
                    pendingTab = index
                    navController.popBackStack(AbsensiRoutes.MAIN, inclusive = false)
                },
            )
        }
        composable(AbsensiRoutes.REKAP) { RekapScreen(onExit = onExit) }
        composable(AbsensiRoutes.CHECKLIST) {
            val staff by AppSession.staff.collectAsState()
            if (staff?.role == Role.ADMIN_HR) {
                ChecklistMonitorScreen(onExit = onExit)
            } else {
                ChecklistScreen(onExit = onExit)
            }
        }
        composable(AbsensiRoutes.CHECKLIST_MONITOR) {
            val staff by AppSession.staff.collectAsState()
            if (staff?.role in SPV_TIER_ROLES) {
                ChecklistMonitorScreen(onExit = onExit)
            } else {
                LaunchedEffect(Unit) { navController.popBackStack() } // gerbang peran: pop mentah, entri belum RESUMED
            }
        }
        composable(AbsensiRoutes.CHECKLIST_MANAGE) {
            val staff by AppSession.staff.collectAsState()
            if (staff?.role in CHECKLIST_MANAGE_ROLES) {
                ChecklistManageScreen(onExit = onExit)
            } else {
                LaunchedEffect(Unit) { navController.popBackStack() } // gerbang peran: pop mentah, entri belum RESUMED
            }
        }
        composable(AbsensiRoutes.CUTI) {
            CutiScreen(
                onExit = onExit,
                onNavigateTab = { index ->
                    pendingTab = index
                    navController.popBackStack(AbsensiRoutes.MAIN, inclusive = false)
                },
            )
        }
        composable(AbsensiRoutes.KASBON) {
            KasbonScreen(
                onExit = onExit,
                onNavigateTab = { index ->
                    pendingTab = index
                    navController.popBackStack(AbsensiRoutes.MAIN, inclusive = false)
                },
            )
        }
        composable(AbsensiRoutes.ENROLL) { EnrollScreen(onExit = onExit) }
        composable(AbsensiRoutes.PENGATURAN) { PengaturanScreen(onExit = onExit) }
        composable(AbsensiRoutes.PROFIL) { ProfilScreen(onExit = onExit) }
        composable(AbsensiRoutes.MANAJEMEN_KRU) { ComingSoon("Manajemen Kru") }
    }
}

/**
 * Kontainer 4 Tab Utama Absensi dengan HorizontalPager swipeable (WhatsApp-style drag & peek).
 */
/** Role yang tab ke-3-nya berisi Enrollment Crew, bukan Profil: mereka yang memang bertugas
 *  mendaftarkan wajah kru. Crew (dan role lain) tetap mendapat tab Profile seperti biasa. */
private val ENROLL_TAB_ROLES = setOf(Role.LEADER, Role.AREA_MANAGER, Role.REGIONAL_MANAGER, Role.DEVELOPER)

/** Sumber tunggal isi bottom nav — jumlah tab tetap 4, hanya slot index 2 yang berganti,
 *  supaya indeks tab dan `pendingTab` dari layar sub-route tidak ikut bergeser.
 *  Untuk role HR, slot index 1 berganti dari Checklist ke Monitor; untuk staf Kantor Pusat
 *  (tidak ada outlet yang dibuka/ditutup) Checklist diganti Cuti. */
private fun absensiBottomTabs(enrollTab: Boolean, isHr: Boolean = false, kantorPusat: Boolean = false) = listOf(
    ItemTabKaca("Home", IkonIos.Home),
    when {
        isHr -> ItemTabKaca("Monitor", IkonIos.FactCheck)
        kantorPusat -> ItemTabKaca("Cuti", IkonIos.CalendarMonth)
        else -> ItemTabKaca("Checklist", IkonIos.Checklist)
    },
    if (enrollTab) ItemTabKaca("Enroll", IkonIos.PersonAdd)
    else ItemTabKaca("Profile", IkonIos.Person),
    ItemTabKaca("More", IkonIos.MoreHoriz),
)

/** Dibaca dari sesi aktif, bukan dioper lewat parameter — bottom nav dipakai di banyak
 *  layar sub-route dan semuanya harus menampilkan tab yang sama. */
@Composable
private fun isEnrollTabRole(): Boolean {
    val staff by AppSession.staff.collectAsState()
    return staff?.role in ENROLL_TAB_ROLES
}

@Composable
private fun isHrRole(): Boolean {
    val staff by AppSession.staff.collectAsState()
    return staff?.role == Role.ADMIN_HR
}

/** Kantor Pusat dikenali lewat slug outlet efektif, bukan `outlets.type` (Gudang Pusat
 *  juga bertipe `office` dan tetap butuh Checklist). */
@Composable
private fun isKantorPusat(): Boolean {
    val staff by AppSession.staff.collectAsState()
    return staff?.diKantorPusat == true
}

/**
 * Kerangka kaca dengan bilah 4 tab yang sama, dipakai di [AbsensiMainPagerScreen] (pager,
 * `selectedIndex` mengikuti halaman aktif) maupun di layar sub-route seperti [CutiScreen]
 * (tak pernah punya tab aktif sungguhan — kirim index tab asalnya, mis. 3 utk "More",
 * supaya user tetap bisa lompat ke tab lain tanpa balik dulu, dan bilah tab tidak pernah
 * hilang begitu masuk ke fitur turunan seperti Cuti & Izin).
 *
 * [isi] digambar sampai dasar layar di balik kapsul, jadi tiap layar di dalamnya memberi
 * ruang bawah sendiri lewat `denganRuangNav()` / `LocalRuangNavKaca`. Lembar menu kaca
 * yang tersusun di dalam [isi] otomatis memburamkan seluruh kerangka.
 */
@Composable
fun AbsensiShell(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    isi: @Composable () -> Unit,
) {
    val tabs = absensiBottomTabs(enrollTab = isEnrollTabRole(), isHr = isHrRole(), kantorPusat = isKantorPusat())
    ShellKaca(
        bilah = { latar -> BilahTabKaca(tabs, selectedIndex, latar, onPilih = onSelect) },
        isi = isi,
    )
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
    val enrollTab = isEnrollTabRole()
    val isHr = isHrRole()
    // HR tetap Monitor walau berkantor di pusat — mengikuti urutan `when` di absensiBottomTabs.
    val tabCuti = isKantorPusat() && !isHr
    var moreSheetVisible by rememberSaveable { mutableStateOf(false) }

    // Cuti di menu More sudah jadi tab sendiri bagi Kantor Pusat: pindah ke tab itu,
    // bukan membuka salinan layarnya dengan bilah tab yang menyorot "More".
    val bukaRute: (String) -> Unit = { route ->
        if (tabCuti && route == AbsensiRoutes.CUTI) {
            moreSheetVisible = false
            coroutineScope.launch { pagerState.animateScrollToPage(1) }
        } else {
            onNavigateToSubRoute(route)
        }
    }

    // More adalah quick action, bukan tab pager. Jadi sheet dibuka di atas
    // halaman aktif agar konten di belakangnya tidak ikut bergeser.
    BackHandler(enabled = moreSheetVisible) {
        moreSheetVisible = false
    }

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
            moreSheetVisible = false
            pagerState.scrollToPage(pendingTab)
            onPendingTabConsumed()
        }
    }

    AbsensiShell(
        selectedIndex = if (moreSheetVisible) 3 else pagerState.currentPage,
        onSelect = { index ->
            if (index == 3) {
                moreSheetVisible = true
            } else if (pagerState.currentPage != index) {
                moreSheetVisible = false
                coroutineScope.launch {
                    pagerState.animateScrollToPage(
                        page = index,
                        animationSpec = tween(420, easing = FastOutSlowInEasing)
                    )
                }
            }
        },
    ) {
        // Tanpa padding Scaffold: halaman sengaja digambar sampai dasar layar supaya
        // terlihat di balik kapsul kaca; masing-masing memberi ruang bawahnya sendiri.
        Box(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = true,
                beyondBoundsPageCount = 1
            ) { page ->
                when (page) {
                    0 -> ClockScreen(
                        isActive = !moreSheetVisible &&
                            (pagerState.currentPage == 0 || pagerState.targetPage == 0),
                        onExit = onExit,
                        // Absen hadir berlanjut ke checklist untuk kru. Role HR hanya memantau,
                        // dan Kantor Pusat tidak punya checklist, jadi keduanya tidak ditarik ke tab 1.
                        // Dijaga `currentPage == 0` supaya kru yang sudah telanjur
                        // menggeser ke tab lain tidak ditarik balik.
                        onAbsenMasukSelesai = {
                            if (pagerState.currentPage == 0 && !isHr && !tabCuti) {
                                coroutineScope.launch { pagerState.animateScrollToPage(1) }
                            }
                        },
                    )
                    1 -> when {
                        isHr -> ChecklistMonitorScreen(onExit = onExit)
                        tabCuti -> CutiScreen(onExit = onExit, sebagaiTab = true)
                        else -> ChecklistScreen(onExit = onExit)
                    }
                    // Slot yang sama dengan tab index 2 di bottom nav — ikut berganti isi
                    // supaya label tab dan halaman yang muncul selalu cocok.
                    2 -> if (enrollTab) {
                        EnrollScreen(onExit = onExit)
                    } else {
                        // Sebagai tab, layar ini tidak punya halaman induk untuk
                        // dituju — tombol kembali di sana hanya akan membingungkan.
                        // ProfilScreen milik modul lain dan tidak mengenal ruang kapsul, jadi
                        // halamannya diangkat utuh di atas kapsul. Ruangnya lalu dinolkan di
                        // dalam supaya tidak terhitung dua kali bila layar itu kelak memakainya.
                        Box(Modifier.fillMaxSize().padding(bottom = LocalRuangNavKaca.current)) {
                            CompositionLocalProvider(LocalRuangNavKaca provides 0.dp) {
                                ProfilScreen(
                                    onExit = {
                                        coroutineScope.launch { pagerState.animateScrollToPage(0) }
                                    },
                                    tampilkanTombolKembali = false,
                                )
                            }
                        }
                    }
                    3 -> AbsensiHubScreen(
                        onNavigate = bukaRute,
                        isSheetVisible = pagerState.currentPage == 3,
                        onDismiss = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(0)
                            }
                        },
                        onExit = {
                            coroutineScope.launch { pagerState.animateScrollToPage(0) }
                        }
                    )
                }
            }

            if (moreSheetVisible && pagerState.currentPage != 3) {
                AbsensiHubScreen(
                    onNavigate = { route ->
                        moreSheetVisible = false
                        bukaRute(route)
                    },
                    onExit = {
                        moreSheetVisible = false
                    },
                    isSheetVisible = true,
                    showBackground = false,
                    onDismiss = {
                        moreSheetVisible = false
                    },
                )
            }
        }
    }
}
