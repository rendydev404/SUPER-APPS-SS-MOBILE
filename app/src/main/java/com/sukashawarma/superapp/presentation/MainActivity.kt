package com.sukashawarma.superapp.presentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sukashawarma.superapp.R
import com.sukashawarma.superapp.presentation.theme.SukaOrange
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.domain.session.StartDestination
import com.sukashawarma.superapp.domain.session.isMitraArea
import com.sukashawarma.superapp.domain.session.resolveStartDestination
import com.sukashawarma.superapp.feature.distribusi.DistribusiNavGraph
import com.sukashawarma.superapp.feature.leader.LeaderNavGraph
import com.sukashawarma.superapp.feature.manager.ManagerNavGraph
import com.sukashawarma.superapp.feature.manager.ui.TujuanManager
import com.sukashawarma.superapp.feature.stok.StokNavGraph
import com.sukashawarma.superapp.notif.ChatNotifikasi
import com.sukashawarma.superapp.notif.NotifikasiTujuan
import com.sukashawarma.superapp.presentation.absensi.AbsensiNavGraph
import com.sukashawarma.superapp.presentation.home.HomeScreen
import com.sukashawarma.superapp.presentation.login.LoginScreen
import com.sukashawarma.superapp.presentation.settings.SettingsScreen
import com.sukashawarma.superapp.presentation.mitra.MitraDashboardScaffold
import com.sukashawarma.superapp.presentation.mitra.MitraLoadErrorScreen
import com.sukashawarma.superapp.feature.chat.ui.ChatScreen
import com.sukashawarma.superapp.feature.profil.ui.ProfilScreen
import com.sukashawarma.superapp.presentation.mitra.MitraNoProfileScreen
import com.sukashawarma.superapp.presentation.theme.SukaSuperappTheme
import kotlinx.coroutines.launch

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val ABSENSI = "absensi"
    const val STOK = "stok"
    const val DISTRIBUSI = "distribusi"
    const val MANAGER = "manager"
    const val LEADER = "leader"
    const val MITRA = "mitra"
    const val MITRA_NO_PROFILE = "mitra_no_profile"
    const val MITRA_LOAD_ERROR = "mitra_load_error"
    const val SETTINGS = "settings"
    const val PROFIL = "profil"
    const val CHAT = "chat"
}

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bacaTujuanNotifikasi(intent)
        setContent {
            SukaSuperappTheme {
                RootNav()
            }
        }
    }

    /**
     * Notifikasi yang diketuk saat aplikasi MASIH hidup tidak melewati onCreate.
     * Tanpa jalur ini, mengetuk notifikasi hanya memunculkan layar terakhir dan
     * tujuannya hilang diam-diam.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        bacaTujuanNotifikasi(intent)
    }

    private fun bacaTujuanNotifikasi(intent: Intent?) {
        NotifikasiTujuan.set(intent?.getStringExtra(NotifikasiTujuan.EXTRA_RUTE))
    }

    /** Mengunci ulang aplikasi ketika Activity benar-benar ditutup, termasuk saat task
     * dihapus dari Recent Apps. Credential biometrik tetap dipertahankan oleh AuthPrefs;
     * recreate karena rotasi tidak dianggap logout. */
    override fun onDestroy() {
        if (isFinishing && !isChangingConfigurations) {
            AppSession.signOut()
        }
        super.onDestroy()
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



    if (loading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_app_logo),
                    contentDescription = "Logo Suka Shawarma",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(160.dp),
                )
                Spacer(modifier = Modifier.height(28.dp))
                CircularProgressIndicator(
                    color = SukaOrange,
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 3.dp
                )
            }
        }
        return
    }

    LocationPermissionGate(staff != null)
    IzinNotifikasiGate(staff != null)

    val destination = resolveStartDestination(staff, mitraProfile, mitraLoadFailed)
    val isMitra = destination.isMitraArea

    // Tujuan dari notifikasi yang diketuk. Ditahan sampai sesi benar-benar terbuka:
    // melompat ke halaman manajer sebelum login akan melewati layar login sama sekali.
    var tujuanManager by remember { mutableStateOf<TujuanManager?>(null) }
    val ruteNotifikasi by NotifikasiTujuan.rute.collectAsState()
    LaunchedEffect(ruteNotifikasi, staff, isMitra) {
        if (ruteNotifikasi == null || staff == null) return@LaunchedEffect
        // Chat berlaku untuk setiap pemegang akun, termasuk mitra — jadi
        // ditangani sebelum penjaga isMitra di bawah.
        if (ruteNotifikasi == NotifikasiTujuan.CHAT) {
            NotifikasiTujuan.ambil()
            navController.navigate(Routes.CHAT)
            return@LaunchedEffect
        }
        if (isMitra) return@LaunchedEffect
        val tujuan = when (NotifikasiTujuan.ambil()) {
            NotifikasiTujuan.MANAGER_PERSETUJUAN -> TujuanManager.PERSETUJUAN
            NotifikasiTujuan.MANAGER_WASTE -> TujuanManager.WASTE
            else -> null
        } ?: return@LaunchedEffect
        tujuanManager = tujuan
        navController.navigate(Routes.MANAGER)
    }

    NavHost(navController = navController, startDestination = routeFor(destination)) {
        // Didaftarkan di luar percabangan mitra: profil adalah satu-satunya halaman
        // yang berlaku untuk SETIAP pemegang akun, termasuk mitra. Menaruhnya di
        // cabang non-mitra saja akan membuat navigate("profil") dari dashboard mitra
        // melempar IllegalArgumentException karena rutenya tidak ada di graph itu.
        composable(Routes.PROFIL) {
            ProfilScreen(onExit = { navController.popBackStack() })
        }

        // Chat Tim juga berlaku untuk setiap pemegang akun — alasan penempatan
        // yang sama dengan PROFIL di atas.
        composable(Routes.CHAT) {
            // Membuka ruangnya berarti pesannya sudah terbaca; notifikasi
            // percakapan yang masih menggantung ditutup di sini.
            val konteks = androidx.compose.ui.platform.LocalContext.current
            LaunchedEffect(Unit) { ChatNotifikasi.tutup(konteks) }
            ChatScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.LOGIN) {
            // Sengaja TIDAK navigate() di sini. Saat callback ini jalan, recomposition
            // belum sempat berjalan, jadi graph di NavController MASIH graph sesi-kosong
            // (tanpa rute mitra) dan navigate("mitra") akan melempar IllegalArgumentException.
            // Begitu AppSession terisi, `destination` + `isMitra` berubah, NavHost menyusun
            // graph baru, dan setGraph memindahkan sendiri ke start destination yang baru.
            LoginScreen(onLoggedIn = {})
        }

        if (isMitra) {
            // HOME, ABSENSI & STOK sengaja TIDAK didaftarkan untuk mitra — tak ada jalan ke
            // sana lewat Back maupun deep link. Cermin route-guard web (RoleContext.tsx).
            composable(Routes.MITRA) {
                MitraDashboardScaffold(
                    onOpenProfil = { navController.navigate(Routes.PROFIL) },
                    onLoggedOut = {
                        navController.navigate(Routes.LOGIN) { popUpTo(0) }
                    },
                )
            }
            composable(Routes.MITRA_NO_PROFILE) {
                MitraNoProfileScreen(onLoggedOut = {
                    navController.navigate(Routes.LOGIN) { popUpTo(0) }
                })
            }
            composable(Routes.MITRA_LOAD_ERROR) {
                MitraLoadErrorScreen(
                    onRetry = { scope.launch { AppSession.retryLoadMitraProfile() } },
                    onLoggedOut = {
                        navController.navigate(Routes.LOGIN) { popUpTo(0) }
                    },
                )
            }
        } else {
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenAbsensi = { navController.navigate(Routes.ABSENSI) },
                    onOpenStok = { navController.navigate(Routes.STOK) },
                    onOpenDistribusi = { navController.navigate(Routes.DISTRIBUSI) },
                    onOpenManager = { navController.navigate(Routes.MANAGER) },
                    onOpenLeader = { navController.navigate(Routes.LEADER) },
                    onOpenChat = { navController.navigate(Routes.CHAT) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onOpenProfil = { navController.navigate(Routes.PROFIL) },
                    onLoggedOut = { navController.navigate(Routes.LOGIN) { popUpTo(0) } }
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.ABSENSI) {
                AbsensiNavGraph(onExit = { navController.popBackStack() })
            }
            composable(Routes.STOK) {
                StokNavGraph(onExit = { navController.popBackStack() })
            }
            composable(Routes.DISTRIBUSI) {
                DistribusiNavGraph(onExit = { navController.popBackStack() })
            }
            composable(Routes.LEADER) {
                LeaderNavGraph(onExit = { navController.popBackStack() })
            }
            composable(Routes.MANAGER) {
                ManagerNavGraph(
                    onExit = { navController.popBackStack() },
                    tujuanAwal = tujuanManager,
                )
                // Dikosongkan setelah dipakai supaya membuka modul Manager lewat
                // Beranda tetap mendarat di Overview seperti biasa.
                LaunchedEffect(Unit) { tujuanManager = null }
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
