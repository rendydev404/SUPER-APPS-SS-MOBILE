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
import com.sukashawarma.superapp.feature.leader.ui.TujuanLeader
import com.sukashawarma.superapp.feature.manager.ManagerNavGraph
import com.sukashawarma.superapp.feature.manager.ui.TujuanManager
import com.sukashawarma.superapp.feature.stok.StokNavGraph
import com.sukashawarma.superapp.notif.ChatNotifikasi
import com.sukashawarma.superapp.notif.NotifikasiTujuan
import com.sukashawarma.superapp.notif.PettyCashAlarmManager
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
import androidx.compose.ui.platform.LocalContext
import com.sukashawarma.superapp.core.ui.keluarMaju
import com.sukashawarma.superapp.core.ui.keluarMundur
import com.sukashawarma.superapp.core.ui.masukMaju
import com.sukashawarma.superapp.core.ui.masukMundur
import com.sukashawarma.superapp.core.ui.PitaOffline
import com.sukashawarma.superapp.core.ui.popAman
import com.sukashawarma.superapp.core.ui.popKe
import com.sukashawarma.superapp.core.update.AppUpdateManager
import com.sukashawarma.superapp.core.update.AppUpdateRelauncher
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.sukashawarma.superapp.core.update.model.AppUpdateManifest
import com.sukashawarma.superapp.core.update.ui.AppUpdateIndicator
import com.sukashawarma.superapp.core.update.ui.AppUpdateSuccessIndicator
import com.sukashawarma.superapp.core.update.ui.DraggableUpdateOverlay
import kotlinx.coroutines.launch
import android.content.Context

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
        handleInstallStatus(intent)
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
        handleInstallStatus(intent)
    }

    private fun handleInstallStatus(intent: Intent?) {
        if (intent == null || !intent.hasExtra(android.content.pm.PackageInstaller.EXTRA_STATUS)) return
        val status = intent.getIntExtra(
            android.content.pm.PackageInstaller.EXTRA_STATUS,
            android.content.pm.PackageInstaller.STATUS_FAILURE
        )
        if (status == android.content.pm.PackageInstaller.STATUS_PENDING_USER_ACTION) {
            val confirmIntent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_INTENT)
            }
            if (confirmIntent != null) {
                confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(confirmIntent)
            }
        } else if (status == android.content.pm.PackageInstaller.STATUS_SUCCESS) {
            com.sukashawarma.superapp.core.update.AppUpdateManager.onUpdateSuccessfullyApplied()
        } else {
            com.sukashawarma.superapp.core.update.AppUpdateManager.handleInstallStatus(applicationContext, intent)
        }
    }

    private fun bacaTujuanNotifikasi(intent: Intent?) {
        if (intent?.getBooleanExtra(PettyCashAlarmManager.EXTRA_STOP_ALARM, false) == true ||
            intent?.getStringExtra(NotifikasiTujuan.EXTRA_RUTE) in setOf(
                NotifikasiTujuan.MANAGER_PETTY_CASH,
                NotifikasiTujuan.LEADER_PETTY_CASH
            )
        ) {
            PettyCashAlarmManager.hentikan(this)
        }
        val rute = intent?.getStringExtra(NotifikasiTujuan.EXTRA_RUTE)
        val areaId = intent?.getStringExtra(NotifikasiTujuan.EXTRA_AREA_ID)
        NotifikasiTujuan.set(rute, areaId)
        if (rute == NotifikasiTujuan.CHAT_PRIBADI) {
            NotifikasiTujuan.setPartner(
                intent.getStringExtra(NotifikasiTujuan.EXTRA_PARTNER_ID),
                intent.getStringExtra(NotifikasiTujuan.EXTRA_PARTNER_NAMA),
                intent.getStringExtra(NotifikasiTujuan.EXTRA_PARTNER_AVATAR),
            )
        }
    }

    override fun onResume() {
        super.onResume()
        AppUpdateRelauncher.onAppVisible(this)
        com.sukashawarma.superapp.core.update.AppUpdateManager.resumeAfterInstallPermission(this)
    }

    override fun onPause() {
        super.onPause()
        AppUpdateRelauncher.onAppHidden()
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
    val context = LocalContext.current

    val updateManifest by AppUpdateManager.availableUpdate.collectAsState()
    val recentlyInstalledVersion by AppUpdateManager.recentlyInstalledVersion.collectAsState()

    // Izin "Tampil di atas aplikasi lain" yang membuat aplikasi terbuka sendiri
    // setelah update terpasang. Ditawarkan sekali per versi; selama ditawarkan
    // atau user sedang di layar pengaturan, pemasangan ditahan supaya aplikasi
    // tidak keburu dimatikan installer sebelum izinnya sempat diaktifkan.
    var izinBukaOtomatis by remember { mutableStateOf(IzinBukaOtomatis.TIDAK_PERLU) }
    LaunchedEffect(updateManifest?.versionCode) {
        val versi = updateManifest?.versionCode ?: return@LaunchedEffect
        if (AppUpdateRelauncher.shouldOfferOverlayPermission(context, versi)) {
            izinBukaOtomatis = IzinBukaOtomatis.DITAWARKAN
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && izinBukaOtomatis == IzinBukaOtomatis.DI_PENGATURAN) {
                izinBukaOtomatis = IzinBukaOtomatis.TIDAK_PERLU
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    if (izinBukaOtomatis == IzinBukaOtomatis.DITAWARKAN) {
        val versi = updateManifest?.versionCode ?: 0
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Buka otomatis setelah update") },
            text = {
                Text(
                    "Aktifkan izin \"Tampil di atas aplikasi lain\" untuk SUKA Kerja Superapps " +
                        "agar aplikasi langsung terbuka lagi begitu update selesai dipasang."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    AppUpdateRelauncher.markOverlayOffered(context, versi)
                    izinBukaOtomatis = IzinBukaOtomatis.DI_PENGATURAN
                    val intent = AppUpdateRelauncher.overlayPermissionIntent(context)
                    runCatching { context.startActivity(intent) }
                        .onFailure { izinBukaOtomatis = IzinBukaOtomatis.TIDAK_PERLU }
                }) { Text("Aktifkan") }
            },
            dismissButton = {
                TextButton(onClick = {
                    AppUpdateRelauncher.markOverlayOffered(context, versi)
                    izinBukaOtomatis = IzinBukaOtomatis.TIDAK_PERLU
                }) { Text("Nanti") }
            },
        )
    }

    // Otomatis terapkan update begitu siap dipasang tanpa harus diklik user
    LaunchedEffect(izinBukaOtomatis) {
        AppUpdateManager.downloadState.collect { state ->
            if (state == AppUpdateManager.DownloadState.READY_TO_INSTALL &&
                izinBukaOtomatis == IzinBukaOtomatis.TIDAK_PERLU
            ) {
                kotlinx.coroutines.delay(1200)
                AppUpdateManager.installDownloadedApk(context)
            }
        }
    }

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
    var tujuanLeader by remember { mutableStateOf<TujuanLeader?>(null) }
    val ruteNotifikasi by NotifikasiTujuan.rute.collectAsState()
    val konteks = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(ruteNotifikasi, staff, isMitra) {
        if (ruteNotifikasi == null || staff == null) return@LaunchedEffect
        // Chat berlaku untuk setiap pemegang akun, termasuk mitra — jadi
        // ditangani sebelum penjaga isMitra di bawah.
        if (ruteNotifikasi == NotifikasiTujuan.CHAT ||
            ruteNotifikasi == NotifikasiTujuan.CHAT_AREA ||
            ruteNotifikasi == NotifikasiTujuan.CHAT_PRIBADI
        ) {
            NotifikasiTujuan.ambil()
            navController.navigate(Routes.CHAT)
            return@LaunchedEffect
        }
        if (isMitra) return@LaunchedEffect

        val rute = NotifikasiTujuan.ambil() ?: return@LaunchedEffect
        if (rute in setOf(NotifikasiTujuan.MANAGER_PETTY_CASH, NotifikasiTujuan.LEADER_PETTY_CASH)) {
            PettyCashAlarmManager.hentikan(konteks)
        }

        when (rute) {
            NotifikasiTujuan.MANAGER_PERSETUJUAN -> {
                tujuanManager = TujuanManager.PERSETUJUAN
                navController.navigate(Routes.MANAGER)
            }
            NotifikasiTujuan.MANAGER_WASTE -> {
                tujuanManager = TujuanManager.WASTE
                navController.navigate(Routes.MANAGER)
            }
            NotifikasiTujuan.MANAGER_PETTY_CASH -> {
                tujuanManager = TujuanManager.PETTY_CASH
                navController.navigate(Routes.MANAGER)
            }
            NotifikasiTujuan.LEADER_PETTY_CASH -> {
                tujuanLeader = TujuanLeader.PETTY_CASH
                navController.navigate(Routes.LEADER)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Di atas NavHost, bukan di dalam tiap layar: mode offline berlaku untuk seluruh
        // aplikasi, dan memasangnya per layar berarti layar yang terlupakan akan menampilkan
        // data cache tanpa satu pun tanda bahwa itu bukan keadaan terkini.
        PitaOffline()
        Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = routeFor(destination),
            // Tanpa ini Navigation Compose memakai silang-pudar 700 ms bawaannya —
            // pintu masuk tiap modul terasa berat. Geserannya dibagi bersama seluruh
            // NavHost aplikasi lewat core:ui supaya iramanya sama di mana pun.
            enterTransition = { masukMaju() },
            exitTransition = { keluarMaju() },
            popEnterTransition = { masukMundur() },
            popExitTransition = { keluarMundur() },
        ) {
            // Didaftarkan di luar percabangan mitra: profil adalah satu-satunya halaman
            // yang berlaku untuk SETIAP pemegang akun, termasuk mitra. Menaruhnya di
            // cabang non-mitra saja akan membuat navigate("profil") dari dashboard mitra
            // melempar IllegalArgumentException karena rutenya tidak ada di graph itu.
            composable(Routes.PROFIL) {
                ProfilScreen(onExit = { navController.popAman() })
            }

            // Chat Tim juga berlaku untuk setiap pemegang akun — alasan penempatan
            // yang sama dengan PROFIL di atas.
            composable(Routes.CHAT) {
                // Chat Area belum dirilis ke produksi: target area dari notifikasi diabaikan.
                val targetAreaId = remember {
                    NotifikasiTujuan.ambilArea().takeIf { com.sukashawarma.superapp.BuildConfig.DEBUG }
                }
                // Notifikasi pesan pribadi membuka LANGSUNG percakapan orangnya,
                // bukan mendarat di tab grup lalu menyuruh pengguna mencari sendiri.
                val targetPartner = remember { NotifikasiTujuan.ambilPartner() }
                ChatScreen(
                    onBack = { navController.popAman() },
                    terkunci = false,
                    tabAwal = when {
                        targetPartner != null -> com.sukashawarma.superapp.feature.chat.ui.pribadi.TabChatUtama.PRIBADI
                        targetAreaId != null -> com.sukashawarma.superapp.feature.chat.ui.pribadi.TabChatUtama.AREA
                        else -> com.sukashawarma.superapp.feature.chat.ui.pribadi.TabChatUtama.GRUP
                    },
                    targetAreaId = targetAreaId,
                    onAreaTerbuka = { areaId ->
                        com.sukashawarma.superapp.notif.ChatAreaNotifikasi.tutup(konteks, areaId)
                    },
                    targetPartner = targetPartner,
                    onPribadiTerbuka = { partnerId ->
                        com.sukashawarma.superapp.notif.ChatPribadiNotifikasi.tutup(konteks, partnerId)
                    },
                )
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
                    SettingsScreen(onBack = { navController.popAman() })
                }
                composable(Routes.ABSENSI) {
                    AbsensiNavGraph(onExit = { navController.popKe(Routes.HOME) })
                }
                composable(Routes.STOK) {
                    StokNavGraph(onExit = { navController.popKe(Routes.HOME) })
                }
                composable(Routes.DISTRIBUSI) {
                    DistribusiNavGraph(onExit = { navController.popKe(Routes.HOME) })
                }
                composable(Routes.LEADER) {
                    LaunchedEffect(Unit) { PettyCashAlarmManager.hentikan(konteks) }
                    LeaderNavGraph(
                        onExit = { navController.popKe(Routes.HOME) },
                        tujuanAwal = tujuanLeader,
                    )
                    LaunchedEffect(Unit) { tujuanLeader = null }
                }
                composable(Routes.MANAGER) {
                    LaunchedEffect(Unit) { PettyCashAlarmManager.hentikan(konteks) }
                    ManagerNavGraph(
                        onExit = { navController.popKe(Routes.HOME) },
                        tujuanAwal = tujuanManager,
                    )
                    // Dikosongkan setelah dipakai supaya membuka modul Manager lewat
                    // Beranda tetap mendarat di Overview seperti biasa.
                    LaunchedEffect(Unit) { tujuanManager = null }
                }
            }
        }

        // Overlay update non-intrusif di atas seluruh navigasi
        if (recentlyInstalledVersion != null) {
            DraggableUpdateOverlay {
                AppUpdateSuccessIndicator(
                    versionName = recentlyInstalledVersion!!,
                    onDismiss = { AppUpdateManager.acknowledgeRecentInstall(context) }
                )
            }
        } else updateManifest?.let { manifest ->
            OverlayUpdateOtomatis(manifest = manifest, context = context)
        }
        }
    }
}

/**
 * Overlay update yang mengisolasi observasi downloadProgress dan downloadState
 * agar lonjakan event saat mengunduh APK tidak memicu recomposition di seluruh RootNav.
 */
@Composable
private fun OverlayUpdateOtomatis(
    manifest: AppUpdateManifest,
    context: Context,
) {
    val downloadState by AppUpdateManager.downloadState.collectAsState()
    val downloadPayload by AppUpdateManager.downloadPayload.collectAsState()
    val downloadPayloadSizeBytes by AppUpdateManager.downloadPayloadSizeBytes.collectAsState()
    val downloadProgress by AppUpdateManager.downloadProgress.collectAsState()

    DraggableUpdateOverlay {
        AppUpdateIndicator(
            manifest = manifest,
            downloadState = downloadState,
            downloadPayload = downloadPayload,
            downloadPayloadSizeBytes = downloadPayloadSizeBytes,
            downloadProgress = downloadProgress,
            onAction = {
                when (downloadState) {
                    AppUpdateManager.DownloadState.IDLE,
                    AppUpdateManager.DownloadState.FAILED ->
                        AppUpdateManager.startDownload(context, manifest)
                    AppUpdateManager.DownloadState.READY_TO_INSTALL ->
                        AppUpdateManager.installDownloadedApk(context)
                    AppUpdateManager.DownloadState.AWAITING_USER_ACTION ->
                        AppUpdateManager.continueInstallWithUserAction(context)
                    AppUpdateManager.DownloadState.DOWNLOADING,
                    AppUpdateManager.DownloadState.INSTALLING -> Unit
                }
            }
        )
    }
}

private enum class IzinBukaOtomatis { TIDAK_PERLU, DITAWARKAN, DI_PENGATURAN }

/** Pemetaan tujuan ke string rute. Aturannya ada di resolveStartDestination, bukan di sini. */
private fun routeFor(destination: StartDestination): String = when (destination) {
    StartDestination.LOGIN -> Routes.LOGIN
    StartDestination.HOME -> Routes.HOME
    StartDestination.MITRA_DASHBOARD -> Routes.MITRA
    StartDestination.MITRA_NO_PROFILE -> Routes.MITRA_NO_PROFILE
    StartDestination.MITRA_LOAD_ERROR -> Routes.MITRA_LOAD_ERROR
}
