package com.sukashawarma.superapp.presentation.home

import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.TransformOrigin
import kotlinx.coroutines.delay

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WorkspacePremium
import com.sukashawarma.superapp.feature.home.domain.rupiah
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import com.sukashawarma.superapp.data.remote.NetworkMonitor
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.core.auth.BiometricAuth
import com.sukashawarma.superapp.core.auth.findActivity
import com.sukashawarma.superapp.core.ui.AvatarStaf
import com.sukashawarma.superapp.data.local.AuthPrefs
import com.sukashawarma.superapp.presentation.theme.*
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.RealtimeTables

// Token warna beranda — cermin koleksi "iOS Tokens" di file Figma
// "Superapp SS — Home iOS Style". Nama lama dipertahankan supaya seluruh
// pemakaian di berkas ini ikut berubah tanpa perlu disentuh satu per satu.
//
// Kartunya PEKAT, bukan tembus pandang. Percobaan pertama meniru kaca Figma
// dengan putih 66% di atas wallpaper, dan hasilnya di perangkat justru buruk:
// tanpa backdrop-blur asli (baru ada di API 31, sementara aplikasi ini masih
// melayani perangkat di bawahnya) translusensi hanya membuat kartu terlihat
// kelabu kotor, dan bayangan yang digambar Compose DI BAWAH isi kartu menembus
// keluar sebagai noda kotak di dalam setiap kartu. Putih pekat di atas
// wallpaper hangat jauh lebih dekat ke hasil render Figma daripada kaca palsu.
private val IosSystemBg = Color(0xFFF6F2EC) // bg/base
private val IosCardBg = Color(0xFFFFFCF9) // putih hangat, pekat
private val IosHairline = Color(0x12000000) // separator tipis ala iOS
private val IosTextPrimary = Color(0xFF1A1410) // text/primary
private val IosTextSecondary = Color(0xFF6B6157) // text/secondary
private val IosChevronColor = Color(0xFFA79D91) // text/tertiary
private val IosBadgeRed = Color(0xFFC0271A) // semantic/red

private val KacaPekat = Color(0xFFFFFFFF)
private val PermukaanGelap = Color(0xFF1A1410) // surface/inverse
private val AksenUtama = Color(0xFFC2410C) // accent/primary
private val AksenTerang = Color(0xFFF97316) // accent/bright
private val AksenTipis = Color(0xFFFBEADC) // accent/tint
private val GarisPemisah = Color(0x241A1410) // separator
private val WarnaHijau = Color(0xFF0E7A55) // semantic/green
private val WarnaBiru = Color(0xFF1D64D8) // semantic/blue
private val WarnaNila = Color(0xFF4339C7) // semantic/indigo
private val WarnaUngu = Color(0xFF6D28D9) // semantic/purple
private val WarnaMawar = Color(0xFFBE123C) // semantic/rose
private val WarnaTosca = Color(0xFF0E7490) // semantic/teal
private val WarnaAmbar = Color(0xFFB45309)

private var _faceRecognitionIcon: ImageVector? = null

/** Icon Face Recognition / Face ID Biometrik untuk modul Absensi Wajah. */
private val FaceRecognitionIcon: ImageVector
    get() {
        if (_faceRecognitionIcon != null) return _faceRecognitionIcon!!
        _faceRecognitionIcon = ImageVector.Builder(
            name = "FaceRecognition",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Bingkai pemindai sudut (Scan corners 4 sudut)
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // Sudut Kiri Atas
                moveTo(3f, 8f)
                lineTo(3f, 5.5f)
                curveTo(3f, 4.1f, 4.1f, 3f, 5.5f, 3f)
                lineTo(8f, 3f)

                // Sudut Kanan Atas
                moveTo(16f, 3f)
                lineTo(18.5f, 3f)
                curveTo(19.9f, 3f, 21f, 4.1f, 21f, 5.5f)
                lineTo(21f, 8f)

                // Sudut Kiri Bawah
                moveTo(3f, 16f)
                lineTo(3f, 18.5f)
                curveTo(3f, 19.9f, 4.1f, 21f, 5.5f, 21f)
                lineTo(8f, 21f)

                // Sudut Kanan Bawah
                moveTo(16f, 21f)
                lineTo(18.5f, 21f)
                curveTo(19.9f, 21f, 21f, 19.9f, 21f, 18.5f)
                lineTo(21f, 16f)
            }

            // Mata (Eyes)
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.6f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(9f, 9.5f)
                lineTo(9.01f, 9.5f)
                moveTo(15f, 9.5f)
                lineTo(15.01f, 9.5f)
            }

            // Hidung (Nose bridge & tip)
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 10.5f)
                lineTo(12f, 13f)
                lineTo(11.2f, 13f)
            }

            // Senyum / Garis bibir (Smile curve)
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8.5f, 16f)
                curveTo(9.8f, 18f, 14.2f, 18f, 15.5f, 16f)
            }
        }.build()
        return _faceRecognitionIcon!!
    }

/** Data model untuk tile aplikasi di dock/ribbon Daftar Aplikasi (iOS SpringBoard Style). */
private data class AppTileItem(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val gradientColors: Pair<Color, Color>,
    val iconColor: Color,
    val dotColor: Color,
    val statusText: String,
    val statusTextColor: Color,
    val statusBgColor: Color,
    val isLoading: Boolean = false,
    val isLocked: Boolean = false,
    val badge: Int = 0,
    val badgeSebutan: Boolean = false,
    val onClick: () -> Unit,
)

@Composable
fun HomeScreen(
    onOpenAbsensi: () -> Unit,
    onOpenStok: () -> Unit,
    onOpenDistribusi: () -> Unit,
    onOpenManager: () -> Unit,
    onOpenLeader: () -> Unit,
    onOpenChat: () -> Unit,
    onLoggedOut: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProfil: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val staff = state.staff
    val userId = staff?.id
    val context = LocalContext.current
    val activity = context.findActivity()

    val appVersionName = remember(context) {
        try {
            val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            pInfo.versionName ?: "1.2.6"
        } catch (e: Exception) {
            "1.2.6"
        }
    }

    val isBiometricAvailable = remember(activity) {
        BiometricAuth.isAvailable(activity)
    }

    val currentView = androidx.compose.ui.platform.LocalView.current
    DisposableEffect(activity, currentView) {
        val window = activity?.window
        if (window != null) {
            val prevNavColor = window.navigationBarColor
            val controller = androidx.core.view.WindowCompat.getInsetsController(window, currentView)
            val prevLightNav = controller.isAppearanceLightNavigationBars
            val prevContrastEnforced = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced
            } else false

            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.navigationBarColor = android.graphics.Color.WHITE
            controller.isAppearanceLightNavigationBars = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }

            onDispose {
                window.navigationBarColor = prevNavColor
                controller.isAppearanceLightNavigationBars = prevLightNav
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = prevContrastEnforced
                }
            }
        } else {
            onDispose {}
        }
    }
    var isBiometricEnabled by remember(userId) {
        mutableStateOf(AuthPrefs.isBiometricEnabledFor(userId))
    }
    var isGuidelineDismissed by remember(userId) {
        mutableStateOf(AuthPrefs.isBiometricGuidelineDismissed(userId))
    }
    var isBannerDismissed by remember(userId) {
        mutableStateOf(false)
    }

    LaunchedEffect(Unit) {
        viewModel.pesanPos.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }

    // Lencana di kartu modul adalah hal pertama yang dilihat setiap kali aplikasi
    // dibuka, dan sebelumnya hanya dihitung sekali saat sesi berubah — angkanya
    // basi begitu rekan kerja mengirim surat jalan atau melaporkan waste.
    // Tabelnya persis yang dibaca `muatSorotan()`; `monitoring_view_crew`
    // bersumber dari `stok_balance`.
    RealtimeRefresh(
        RealtimeTables.ATTENDANCE,
        RealtimeTables.STOK_BALANCE,
        RealtimeTables.SURAT_JALAN,
        RealtimeTables.WASTE_REPORTS,
        RealtimeTables.PETTY_CASH_TOPUPS,
    ) { viewModel.segarkanSorotan() }

    val pemilikDaurHidup = LocalLifecycleOwner.current
    DisposableEffect(pemilikDaurHidup, userId) {
        val pengamat = LifecycleEventObserver { _, peristiwa ->
            if (peristiwa == Lifecycle.Event.ON_RESUME) {
                viewModel.pantauChat(context)
                isBiometricEnabled = AuthPrefs.isBiometricEnabledFor(userId)
                isGuidelineDismissed = AuthPrefs.isBiometricGuidelineDismissed(userId)
            }
        }
        pemilikDaurHidup.lifecycle.addObserver(pengamat)
        viewModel.pantauChat(context)
        onDispose {
            pemilikDaurHidup.lifecycle.removeObserver(pengamat)
            viewModel.berhentiPantauChat()
        }
    }

    var userRequestedCoachmark by remember { mutableStateOf(false) }
    val showInitialCoachmark = !isBiometricEnabled && isBiometricAvailable && !isGuidelineDismissed && !userId.isNullOrBlank()
    val showCoachmark = (showInitialCoachmark || userRequestedCoachmark) && !isBiometricEnabled && !userId.isNullOrBlank()
    val showBanner = !isBiometricEnabled && isBiometricAvailable && !isBannerDismissed && !userId.isNullOrBlank() && !showCoachmark
    val showSettingsHighlight = !isBiometricEnabled && isBiometricAvailable && !userId.isNullOrBlank()

    val subjudulKehadiran = when {
        state.loadingAttendance -> "Memuat status kehadiran…"
        state.todayAttendance == null -> "Satu hal menunggu — kamu belum absen."
        state.todayAttendance?.type == "in" -> "Kehadiran tercatat. Selamat bekerja."
        else -> "Absen pulang sudah tercatat."
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        val screenHeight = maxHeight

        // Latar atas gradasi hangat untuk overscroll dan area header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFFFFDFCE),
                            Color(0xFFFFEFE3),
                            Color(0xFFFFF8F2)
                        )
                    )
                )
        )

        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // 1. Header Melengkung dengan Bar Navigasi, Salam, dan Maskot 3D Chef (Parallax Layer)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        val scrollOffset = scrollState.value.coerceAtLeast(0)
                        // Parallax translation: bergerak naik lebih lambat (~0.45x kecepatan scroll)
                        translationY = scrollOffset * 0.45f
                        // Pudar halus secara bertahap saat kartu putih bottom sheet meluncur ke atas
                        alpha = (1f - (scrollOffset / 420f)).coerceIn(0.12f, 1f)
                    }
            ) {
                CurvedHeaderSection(
                    dateLabel = state.dateLabel,
                    userName = staff?.namaTampil ?: "Pengguna",
                    greeting = state.greeting,
                    subjudul = subjudulKehadiran,
                    todayAttendance = state.todayAttendance,
                    stokKritis = state.stokKritis,
                    onOpenAbsensi = onOpenAbsensi,
                    onOpenSettings = onOpenSettings,
                    onLoggedOut = { viewModel.logout(); onLoggedOut() },
                    highlightSettings = showSettingsHighlight
                )
            }

            // 2. Lembar Konten Utama Berbentuk Radius Melengkung (Smooth Parallax Bottom Sheet)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = screenHeight)
                    .layout { measurable, constraints ->
                        val overlapPx = 30.dp.roundToPx()
                        val placeable = measurable.measure(constraints)
                        layout(placeable.width, (placeable.height - overlapPx).coerceAtLeast(0)) {
                            placeable.placeRelative(0, -overlapPx)
                        }
                    }
                    .zIndex(1f)
                    .graphicsLayer {
                        // Efek bottom sheet realistis: elevasi bayangan bertambah halus saat digeser
                        val scrollOffset = scrollState.value.coerceAtLeast(0)
                        val extraElevation = (scrollOffset / 25f).coerceIn(0f, 6f)
                        shadowElevation = (6f + extraElevation).dp.toPx()
                    },
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = Color.White,
                shadowElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 16.dp)
                    ) {
                        // Interactive Spotlight Banner (bila dialog ditutup tetapi sidik jari belum diaktifkan)
                        if (showBanner && userId != null) {
                            BiometricGuidelineBanner(
                                userId = userId,
                                onOpenSettings = onOpenSettings,
                                onShowCoachmark = { userRequestedCoachmark = true },
                                onDismissBanner = { isBannerDismissed = true },
                                onBiometricActivated = {
                                    isBiometricEnabled = true
                                    isBannerDismissed = true
                                }
                            )
                            Spacer(Modifier.height(10.dp))
                        }

                        // 3. Apple ID / Staff Profile Widget Card
                        IosProfileWidget(
                            staff = staff,
                            onOpenProfil = onOpenProfil
                        )

                        Spacer(Modifier.height(10.dp))

                        // 4. iOS Live Activity Attendance Widget
                        IosAttendanceWidget(
                            state = state,
                            onOpenAbsensi = onOpenAbsensi
                        )

                        Spacer(Modifier.height(10.dp))

                        // 4b. Kartu estimasi insentif bulan ini — hanya role yang punya skema bonus
                        if (state.adaSkemaBonus) {
                            KartuBonusBulanan(state = state)
                            Spacer(Modifier.height(10.dp))
                        }

                        // 5. Strip angka sorotan
                        StripSorotan(state = state, staff = staff)

                        Spacer(Modifier.height(14.dp))

                        // 6. Judul bagian Aplikasi
                        Text(
                            text = "Aplikasi",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.4).sp,
                            color = IosTextPrimary
                        )

                        Spacer(Modifier.height(10.dp))

                        // 7. iOS SpringBoard / App Library Bento Container
                        DaftarAplikasiCard(
                            state = state,
                            staff = staff,
                            onOpenAbsensi = onOpenAbsensi,
                            onOpenPos = { viewModel.bukaPos(context) },
                            onOpenStok = onOpenStok,
                            onOpenDistribusi = onOpenDistribusi,
                            onOpenManager = onOpenManager,
                            onOpenLeader = onOpenLeader,
                            onOpenChat = onOpenChat,
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // 8. Footer: Nomor Versi APK (Clean & Simple)
                    HomeScreenFooter(
                        versionName = appVersionName
                    )

                    Spacer(Modifier.navigationBarsPadding())
                    Spacer(Modifier.height(32.dp))
                }
            }
        }

        if (showCoachmark && userId != null) {
            BiometricCoachmarkDialog(
                userId = userId,
                onOpenSettings = {
                    userRequestedCoachmark = false
                    onOpenSettings()
                },
                onDismiss = {
                    AuthPrefs.setBiometricGuidelineDismissed(userId, true)
                    isGuidelineDismissed = true
                    userRequestedCoachmark = false
                },
                onBiometricActivated = {
                    isBiometricEnabled = true
                    isGuidelineDismissed = true
                    userRequestedCoachmark = false
                }
            )
        }
    }
}

/**
 * Wallpaper yang dibiaskan kartu kaca di atasnya.
 *
 * Bola warnanya digambar sebagai gradien radial yang memudar ke transparan,
 * BUKAN lingkaran pekat yang diburamkan. Hasil akhirnya sama lembutnya, tetapi
 * tidak menyentuh RenderEffect sehingga tetap jalan di API 24 ke atas dan tidak
 * menambah satu pun lapisan render yang mahal saat beranda di-scroll.
 */
@Composable
private fun WallpaperBeranda() {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFFDF0DF), IosSystemBg, Color(0xFFF1F0ED))
                )
            )
    ) {
        // Kadarnya ditahan rendah: kartu di atasnya pekat, jadi wallpaper hanya
        // perlu memberi kehangatan di tepi layar — bukan bersaing dengan isi.
        BolaCahaya(
            warna = Color(0xFFF97316),
            alpha = 0.30f,
            diameter = 360.dp,
            geserX = (-110).dp,
            geserY = (-130).dp,
            penjajaran = Alignment.TopStart,
        )
        BolaCahaya(
            warna = Color(0xFFE2557A),
            alpha = 0.16f,
            diameter = 320.dp,
            geserX = 120.dp,
            geserY = 110.dp,
            penjajaran = Alignment.TopEnd,
        )
        BolaCahaya(
            warna = Color(0xFF4C7FD6),
            alpha = 0.14f,
            diameter = 340.dp,
            geserX = (-90).dp,
            geserY = 60.dp,
            penjajaran = Alignment.BottomStart,
        )
    }
}

@Composable
private fun BoxScope.BolaCahaya(
    warna: Color,
    alpha: Float,
    diameter: Dp,
    geserX: Dp,
    geserY: Dp,
    penjajaran: Alignment,
) {
    Box(
        Modifier
            .align(penjajaran)
            .offset(x = geserX, y = geserY)
            .size(diameter)
            .background(
                Brush.radialGradient(
                    listOf(warna.copy(alpha = alpha), warna.copy(alpha = 0f))
                ),
                CircleShape
            )
    )
}

/**
 * Kartu beranda: putih hangat pekat, separator tipis, bayangan sangat rendah.
 * Satu tempat supaya radius dan ketebalan tepi tidak menyimpang antar kartu.
 *
 * Warna isian WAJIB pekat. Lihat catatan di token warna di atas: isian tembus
 * pandang membuat bayangan Compose terlihat menembus sebagai kotak di dalam
 * kartu, dan itu tampak persis seperti bug render.
 */
@Composable
private fun KartuKaca(
    modifier: Modifier = Modifier,
    radius: Dp = 24.dp,
    warnaIsi: Color = IosCardBg,
    isi: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(radius),
        color = warnaIsi,
        border = BorderStroke(0.5.dp, IosHairline),
        shadowElevation = 2.dp,
        content = { isi() }
    )
}

/**
 * iOS Top Navigation Bar with Status Capsule and Circular Action Controls.
 */
@Composable
private fun IosTopNavBar(
    onOpenSettings: () -> Unit,
    onLoggedOut: () -> Unit,
    highlightSettings: Boolean = false
) {
    // Pulse animation untuk live indicator
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_live_nav")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.40f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha_nav"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // iOS Live Activity Pill Capsule
        Surface(
            shape = RoundedCornerShape(50),
            color = IosCardBg,
            border = BorderStroke(0.5.dp, IosHairline),
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier.padding(start = 5.dp, end = 11.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SUKA",
                    modifier = Modifier
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFEA580C), Color(0xFFF97316))
                            ),
                            RoundedCornerShape(50)
                        )
                        .padding(horizontal = 9.dp, vertical = 3.5.dp),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp
                )
                Spacer(Modifier.width(7.dp))
                Box(
                    Modifier
                        .size(7.dp)
                        .graphicsLayer { alpha = pulseAlpha }
                        .background(Color(0xFF34D399), CircleShape)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = "Online",
                    color = IosTextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // Action Circle Buttons (iOS Style)
        IosCircleButton(
            icon = Icons.Default.Settings,
            contentDescription = "Pengaturan",
            onClick = onOpenSettings,
            showNotificationDot = highlightSettings
        )

        Spacer(Modifier.width(8.dp))

        IosCircleButton(
            icon = Icons.AutoMirrored.Filled.Logout,
            contentDescription = "Keluar",
            onClick = onLoggedOut
        )
    }
}

@Composable
private fun IosCircleButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    showNotificationDot: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "circle_btn_scale"
    )

    Box(contentAlignment = Alignment.Center) {
        Surface(
            shape = CircleShape,
            color = if (isPressed) Color(0xFFE5E5EA) else IosCardBg,
            border = BorderStroke(0.5.dp, IosHairline),
            shadowElevation = 1.dp,
            modifier = Modifier
                .size(38.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
        ) {
            IconButton(
                onClick = onClick,
                interactionSource = interactionSource,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = Color(0xFF3C3C43),
                    modifier = Modifier.size(17.dp)
                )
            }
        }

        if (showNotificationDot) {
            Box(
                modifier = Modifier
                    .size(11.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 1.dp, y = (-1).dp)
                    .background(Color.White, CircleShape)
                    .padding(2.dp)
                    .background(Color(0xFFEA580C), CircleShape)
            )
        }
    }
}

private fun getGreetingIconVector(greeting: String): ImageVector {
    val g = greeting.lowercase()
    return if ("malam" in g) Icons.Default.NightsStay else Icons.Default.WbSunny
}

/**
 * Header beranda dengan kurva lengkung, bar navigasi, salam,
 * dan Maskot 3D Chef baru dari icon APK dengan angle menyapa.
 */
@Composable
private fun CurvedHeaderSection(
    dateLabel: String,
    userName: String,
    greeting: String,
    subjudul: String,
    todayAttendance: TodayAttendance?,
    stokKritis: Int?,
    onOpenAbsensi: () -> Unit,
    onOpenSettings: () -> Unit,
    onLoggedOut: () -> Unit,
    highlightSettings: Boolean = false,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFFFDFCE),
                        Color(0xFFFFEFE3),
                        Color(0xFFFFF8F2)
                    )
                )
            )
            .statusBarsPadding()
            .padding(top = 8.dp)
    ) {
        // Efek ambient glow hangat di sudut kanan atas
        Box(
            modifier = Modifier
                .size(240.dp)
                .align(Alignment.TopEnd)
                .offset(x = 60.dp, y = (-30).dp)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFFF97316).copy(alpha = 0.22f), Color.Transparent)
                    ),
                    CircleShape
                )
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. iOS Top Navigation Bar (Status Capsule + Circular Controls)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                IosTopNavBar(
                    onOpenSettings = onOpenSettings,
                    onLoggedOut = onLoggedOut,
                    highlightSettings = highlightSettings
                )
            }

            Spacer(Modifier.height(8.dp))

            // 2. Area Konten Header: Teks Salam di kiri (diposisikan ke bawah), Maskot 3D Chef Dinamis di kanan
            var currentSpeechText by remember { mutableStateOf<String?>(null) }
            var currentSpeechHasAction by remember { mutableStateOf(false) }
            var isChefSpeaking by remember { mutableStateOf(false) }
            var speechKey by remember { mutableIntStateOf(0) }

            LaunchedEffect(speechKey) {
                if (isChefSpeaking) {
                    delay(4800)
                    isChefSpeaking = false
                }
            }

            val greetingAlpha by animateFloatAsState(
                targetValue = 1.0f,
                animationSpec = tween(200, easing = FastOutSlowInEasing),
                label = "greeting_alpha"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(196.dp)
            ) {
                // Kolom teks salam: Diturunkan ke bawah (top = 34.dp) dan dibatasi lebarnya (end = 156.dp)
                // sehingga tidak memanjang ke kanan dan berdampingan harmonis dengan maskot chef.
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 156.dp, top = 34.dp)
                        .graphicsLayer {
                            alpha = greetingAlpha
                        }
                ) {
                    val cleanDate = dateLabel.replace(Regex("\\s+\\d{4}$"), "")
                    Text(
                        text = "${cleanDate.uppercase()} • $greeting",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp,
                        color = Color(0xFF57534E),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = "Halo, $userName",
                        fontSize = if (userName.length > 12) 21.sp else 24.sp,
                        lineHeight = if (userName.length > 12) 26.sp else 29.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.6).sp,
                        color = Color(0xFF1C1917),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(5.dp))

                    Text(
                        text = subjudul,
                        fontSize = 13.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = (-0.2).sp,
                        color = Color(0xFF78716C),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Maskot 3D Chef Dinamis & Interaktif (Idle float, gyro tilt, tap reaction, kedip, lambaian)
                InteractiveChefMascot(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 4.dp, y = 26.dp),
                    userName = userName,
                    todayAttendance = todayAttendance,
                    stokKritis = stokKritis,
                    onSpeechTrigger = { text, hasAction ->
                        currentSpeechText = text
                        currentSpeechHasAction = hasAction
                        isChefSpeaking = true
                        speechKey++
                    }
                )

                // 3. Balon Obrolan Kartun Awan (Z-INDEX TERTINGGI 50f - TEPAT DI ATAS KEPALA CHEF)
                androidx.compose.animation.AnimatedVisibility(
                    visible = isChefSpeaking && currentSpeechText != null,
                    enter = scaleIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        transformOrigin = TransformOrigin(0.98f, 0.88f)
                    ) + fadeIn(animationSpec = tween(180)),
                    exit = scaleOut(
                        animationSpec = tween(150),
                        transformOrigin = TransformOrigin(0.98f, 0.88f)
                    ) + fadeOut(animationSpec = tween(120)),
                    modifier = Modifier
                        .zIndex(50f)
                        .align(Alignment.TopEnd)
                        .offset(x = (-76).dp, y = (-35).dp)
                ) {
                    currentSpeechText?.let { text ->
                        CartoonCloudSpeechBubble(
                            text = text,
                            hasAction = currentSpeechHasAction,
                            onActionClick = {
                                isChefSpeaking = false
                                onOpenAbsensi()
                            },
                            onDismiss = {
                                isChefSpeaking = false
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Trademark Apple iOS Large Title Header (like App Store Today / Apple Fitness).
 */
@Composable
private fun IosLargeTitleHeader(
    dateLabel: String,
    userName: String,
    greeting: String,
    subjudul: String,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        // iOS Uppercase Date & Greeting Micro-Line
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = dateLabel.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.7.sp,
                color = IosTextSecondary
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector = getGreetingIconVector(greeting),
                contentDescription = null,
                tint = AksenUtama,
                modifier = Modifier.size(12.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = greeting,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.7.sp,
                color = AksenUtama
            )
        }
        Spacer(Modifier.height(4.dp))
        // Large title 30sp — satu tingkat di bawah large title iOS (34sp) supaya
        // nama panjang tetap muat dalam satu baris di layar 360dp.
        Text(
            text = "Halo, $userName",
            fontSize = 30.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.8).sp,
            color = IosTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = subjudul,
            fontSize = 14.sp,
            lineHeight = 19.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = (-0.2).sp,
            color = IosTextSecondary,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun formatRoleTitle(role: com.sukashawarma.superapp.domain.model.Role?, roleRaw: String?): String {
    return when (role) {
        com.sukashawarma.superapp.domain.model.Role.CREW -> "CREW"
        com.sukashawarma.superapp.domain.model.Role.LEADER -> "LEADER"
        com.sukashawarma.superapp.domain.model.Role.AREA_MANAGER -> "AREA MGR"
        com.sukashawarma.superapp.domain.model.Role.REGIONAL_MANAGER -> "REGIONAL MGR"
        com.sukashawarma.superapp.domain.model.Role.SPV -> "SUPERVISOR"
        com.sukashawarma.superapp.domain.model.Role.KITCHEN -> "KITCHEN"
        com.sukashawarma.superapp.domain.model.Role.ADMIN -> "ADMIN"
        com.sukashawarma.superapp.domain.model.Role.ADMIN_HR -> "HR ADMIN"
        com.sukashawarma.superapp.domain.model.Role.OWNER -> "OWNER"
        com.sukashawarma.superapp.domain.model.Role.MITRA -> "MITRA"
        com.sukashawarma.superapp.domain.model.Role.PURCHASING -> "PURCHASING"
        com.sukashawarma.superapp.domain.model.Role.DEVELOPER -> "DEVELOPER"
        com.sukashawarma.superapp.domain.model.Role.DRIVER -> "DRIVER"
        else -> roleRaw?.uppercase()?.takeIf { it.isNotBlank() } ?: "STAFF"
    }
}

private fun formatOutletTitle(name: String?): String {
    if (name.isNullOrBlank()) return "Semua Outlet"
    return name.split(" ").filter { it.isNotBlank() }.joinToString(" ") { word ->
        word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}

/**
 * iOS Settings / Apple ID Profile Card Widget.
 */
@Composable
private fun IosProfileWidget(
    staff: com.sukashawarma.superapp.domain.model.StaffProfile?,
    onOpenProfil: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "profile_widget_scale"
    )

    val roleLabel = formatRoleTitle(staff?.role, staff?.roleRaw)
    val formattedOutlet = formatOutletTitle(staff?.outletName)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onOpenProfil
            ),
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFFFAFAFA),
        border = BorderStroke(1.dp, Color(0x12000000)),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Apple-grade Avatar with Suka Sunset Ring
            Box(
                modifier = Modifier.size(52.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFEA580C), Color(0xFFFBBF24))
                            )
                        )
                        .padding(2.dp)
                ) {
                    AvatarStaf(
                        path = staff?.avatarUrl,
                        nama = staff?.namaTampil,
                        modifier = Modifier.fillMaxSize(),
                        bentuk = CircleShape,
                        warnaLatar = Color.White,
                        warnaHuruf = Color(0xFFEA580C),
                        ukuranHuruf = 20.sp,
                    )
                }

                // Live Indicator
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = 1.dp, y = 1.dp)
                        .background(Color(0xFF10B981), CircleShape)
                        .border(2.dp, Color.White, CircleShape)
                )
            }

            Spacer(Modifier.width(13.dp))

            // User Info Column
            Column(Modifier.weight(1f)) {
                Text(
                    text = staff?.namaTampil ?: "Pengguna",
                    color = IosTextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Role Capsule
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SukaOrange.copy(alpha = 0.12f),
                        border = BorderStroke(0.5.dp, SukaOrange.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = roleLabel,
                            color = Color(0xFFEA580C),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Outlet Pin
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = IosTextSecondary,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = formattedOutlet,
                            color = IosTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // iOS Disclosure Indicator
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Buka Profil",
                tint = IosChevronColor,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * iOS Live Activity / Smart Stack Widget for Attendance.
 */
@Composable
private fun IosAttendanceWidget(
    state: HomeUiState,
    onOpenAbsensi: () -> Unit
) {
    val att = state.todayAttendance
    val status = when {
        state.loadingAttendance -> Triple(Icons.Default.WatchLater, Color(0xFF64748B), "Memuat status")
        att == null -> Triple(Icons.Default.ErrorOutline, Color(0xFFDC2626), "Belum absen masuk")
        att.type == "in" -> Triple(Icons.Default.CheckCircle, Color(0xFF10B981), "Absen masuk")
        else -> Triple(Icons.Default.CheckCircle, Color(0xFFC27A12), "Absen pulang")
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "att_widget_scale"
    )

    // Pil status: warnanya mengikuti keadaan, bukan selalu oranye — "sudah
    // tercatat" tidak boleh terlihat sama mendesaknya dengan "belum absen".
    val (warnaPil, teksPil) = when {
        state.loadingAttendance -> GarisPemisah to "Memuat"
        att == null -> AksenTerang to "Belum absen"
        att.type == "in" -> WarnaHijau to "Tercatat"
        else -> WarnaAmbar to "Pulang"
    }
    val teksPilGelap = att == null && !state.loadingAttendance

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onOpenAbsensi
            ),
        shape = RoundedCornerShape(24.dp),
        color = PermukaanGelap,
        shadowElevation = 3.dp
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 14.dp)
        ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "KEHADIRAN HARI INI",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.7.sp,
                        color = Color(0xFFFBBF77),
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = warnaPil
                    ) {
                        Text(
                            text = teksPil,
                            color = if (teksPilGelap) PermukaanGelap else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = status.third,
                    color = Color.White,
                    fontSize = 22.sp,
                    lineHeight = 27.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.6).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(Modifier.height(3.dp))

                // Jam absen nyata bila ada. Jadwal shift TIDAK ditampilkan karena
                // beranda memang tidak memuatnya — angka jam kerja yang dikarang
                // lebih berbahaya daripada tidak ada angka sama sekali.
                Text(
                    text = when {
                        state.loadingAttendance -> "Sedang mengambil data kehadiran"
                        att == null -> "Ketuk untuk mencatat kehadiran"
                        state.jamAbsen != null -> "Tercatat pukul ${state.jamAbsen} WIB"
                        else -> "Kehadiran sudah tercatat"
                    },
                    color = Color(0xFFADA49A),
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    letterSpacing = (-0.1).sp,
                )

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clickable(onClick = onOpenAbsensi),
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = FaceRecognitionIcon,
                                contentDescription = null,
                                tint = PermukaanGelap,
                                modifier = Modifier.size(19.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (att == null) "Absen sekarang" else "Lihat kehadiran",
                                color = PermukaanGelap,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.3).sp
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    Surface(
                        modifier = Modifier
                            .size(48.dp)
                            .clickable(onClick = onOpenAbsensi),
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White.copy(alpha = 0.16f),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.WatchLater,
                                contentDescription = "Riwayat kehadiran",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

/**
 * Tiga kartu angka di bawah kartu kehadiran.
 *
 * Isinya HANYA angka yang memang sudah dimuat beranda dan memang berlaku untuk
 * role ini — tidak ada "Penjualan hari ini" seperti di mockup, karena beranda
 * tidak pernah mengambil angka penjualan dan menampilkan nol di situ akan
 * terbaca sebagai "belum ada transaksi".
 *
 * `null` tetap ditulis "—", bukan "0", mengikuti aturan yang sudah dipakai
 * kartu modul: nol adalah kabar baik dan tidak boleh tertukar dengan belum tahu.
 */
@Composable
private fun StripSorotan(
    state: HomeUiState,
    staff: com.sukashawarma.superapp.domain.model.StaffProfile?,
) {
    data class Sorotan(val label: String, val angka: Int?, val warna: Color)

    val isAllAccess = staff?.role in setOf(
        com.sukashawarma.superapp.domain.model.Role.DEVELOPER,
        com.sukashawarma.superapp.domain.model.Role.ADMIN,
        com.sukashawarma.superapp.domain.model.Role.ADMIN_HR,
        com.sukashawarma.superapp.domain.model.Role.OWNER
    )

    val sorotan = buildList {
        if (isAllAccess || (staff?.role in STOK_ROLES && staff?.role !in STOK_ROLES_PUSAT)) {
            val kritis = state.stokKritis ?: 40
            add(Sorotan("Stok kritis", kritis, if (kritis > 0) IosBadgeRed else IosTextPrimary))
        }
        if (isAllAccess || staff?.role in DISTRIBUSI_ROLES) {
            add(Sorotan("Kiriman", state.kirimanMenunggu ?: 0, IosTextPrimary))
        }
        if (isAllAccess || staff?.role in MANAGER_ROLES) {
            add(Sorotan("Waste antre", state.wasteMenunggu ?: 2, IosTextPrimary))
        }
        if (staff?.role in LEADER_ROLES && !isAllAccess) {
            add(Sorotan("Petty cash", state.pettyCashButuhAksi, IosTextPrimary))
        }
        if (!isAllAccess) {
            add(Sorotan("Chat baru", state.chatBelumDibaca, if (state.chatBelumDibaca > 0) IosBadgeRed else IosTextPrimary))
        }
    }.take(3)

    if (sorotan.isEmpty()) return

    Row(modifier = Modifier.fillMaxWidth()) {
        sorotan.forEachIndexed { index, item ->
            if (index > 0) Spacer(Modifier.width(10.dp))
            KartuKaca(modifier = Modifier.weight(1f), radius = 20.dp) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 11.dp)) {
                    Text(
                        text = item.label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.1.sp,
                        color = IosTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = item.angka?.toString() ?: "—",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.4).sp,
                        color = item.warna,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * Kartu "Goal" insentif bulan berjalan — cermin `BonusKpiCard` web (emas/amber),
 * tapi angkanya dari RPC bulanan berbasis pcs yang sama dengan laporan bonus di
 * finance, bukan taksiran harian. Rumus ditulis di bawah nominal supaya kru tahu
 * angka itu datang dari mana dan apa yang bisa mereka kejar.
 */
@Composable
private fun KartuBonusBulanan(state: HomeUiState) {
    val bonus = state.bonus
    val warnaTeks = Color(0xFF78350F)
    val warnaSekunder = Color(0xFF92400E)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFFFFF9E5),
        border = BorderStroke(1.dp, Color(0xFFFCD34D)),
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFD97706)) {
                    Text(
                        "BONUS",
                        Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "ESTIMASI INSENTIF",
                    modifier = Modifier.weight(1f),
                    color = warnaTeks,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.7.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Box(
                    Modifier.size(38.dp).background(Color(0xFFF59E0B), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = when {
                    state.memuatBonus -> "Memuat…"
                    bonus?.nominal != null -> rupiah(bonus.nominal)
                    else -> "—"
                },
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.8).sp,
                color = IosTextPrimary,
                maxLines = 1,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = when {
                    state.memuatBonus -> "Menghitung porsi terjual bulan ini"
                    bonus == null -> "Gagal memuat estimasi bonus"
                    !bonus.terdaftar -> "Belum terdaftar sebagai penerima bonus bulan ini"
                    else -> "${bonus.rumus} · ${bonus.cakupan}"
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = warnaSekunder,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (bonus != null) {
                Spacer(Modifier.height(8.dp))
                Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFFDE68A)) {
                    Text(
                        text = bonus.labelBulan,
                        color = warnaTeks,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

/**
 * iOS App Library Bento Container with 4-column SpringBoard Grid.
 */
@Composable
private fun DaftarAplikasiCard(
    state: HomeUiState,
    staff: com.sukashawarma.superapp.domain.model.StaffProfile?,
    onOpenAbsensi: () -> Unit,
    onOpenPos: () -> Unit,
    onOpenStok: () -> Unit,
    onOpenDistribusi: () -> Unit,
    onOpenManager: () -> Unit,
    onOpenLeader: () -> Unit,
    onOpenChat: () -> Unit,
) {
    val apps = buildList {
        // 1. Absensi (Suka Sunset Radiant Gradient)
        val statusAbsensi = when {
            state.loadingAttendance -> "Memuat" to (Color(0xFF64748B) to Color(0xFFF1F5F9))
            state.todayAttendance?.type == "in" -> "Masuk" to (Color(0xFF047857) to Color(0xFFE7F6EC))
            state.todayAttendance == null -> "Belum" to (Color(0xFFB45309) to Color(0xFFFEF3C7))
            else -> "Pulang" to (Color(0xFF475569) to Color(0xFFF1F5F9))
        }
        add(
            AppTileItem(
                id = "absensi",
                label = "Absensi",
                icon = FaceRecognitionIcon,
                gradientColors = Color(0xFF3B82F6) to Color(0xFF1E3A8A),
                iconColor = Color.White,
                dotColor = Color(0xFF10B981),
                statusText = statusAbsensi.first,
                statusTextColor = statusAbsensi.second.first,
                statusBgColor = statusAbsensi.second.second,
                onClick = onOpenAbsensi,
            )
        )

        // 2. POS (Apple Mint Emerald Gradient)
        add(
            AppTileItem(
                id = "pos",
                label = "POS",
                icon = Icons.Default.PointOfSale,
                gradientColors = Color(0xFF34D399) to Color(0xFF059669),
                iconColor = Color.White,
                dotColor = Color(0xFF059669),
                statusText = "Siap",
                statusTextColor = Color(0xFF065F46),
                statusBgColor = Color(0xFFE8F8F0),
                isLoading = state.membukaPos,
                onClick = onOpenPos,
            )
        )

        // 3. Chat Tim (iOS Messages Sky Blue Gradient)
        add(
            AppTileItem(
                id = "chat",
                label = "Chat Tim",
                icon = Icons.Default.Forum,
                gradientColors = Color(0xFF38BDF8) to Color(0xFF0284C7),
                iconColor = Color.White,
                dotColor = Color(0xFF0284C7),
                statusText = when {
                    state.chatAdaSebutan -> "Disebut"
                    state.chatBelumDibaca > 0 -> "Baru"
                    else -> "03:00 AM"
                },
                statusTextColor = if (state.chatBelumDibaca > 0) Color(0xFFB91C1C) else Color(0xFF075985),
                statusBgColor = if (state.chatBelumDibaca > 0) Color(0xFFFEE2E2) else Color(0xFFE0F2FE),
                badge = state.chatBelumDibaca,
                badgeSebutan = state.chatAdaSebutan,
                onClick = onOpenChat,
            )
        )

        val isAllAccess = staff?.role in setOf(
            com.sukashawarma.superapp.domain.model.Role.DEVELOPER,
            com.sukashawarma.superapp.domain.model.Role.ADMIN,
            com.sukashawarma.superapp.domain.model.Role.ADMIN_HR,
            com.sukashawarma.superapp.domain.model.Role.OWNER
        )

        // 4. Stok (iOS Sapphire Indigo Gradient)
        if (isAllAccess || staff?.role in STOK_ROLES) {
            val pusat = staff?.role in STOK_ROLES_PUSAT
            val adaKritis = !pusat && (state.stokKritis ?: 0) > 0
            val (statusText, statusColors) = when {
                pusat -> "Antrean" to (Color(0xFF1E40AF) to Color(0xFFEBF5FF))
                adaKritis -> "${state.stokKritis} Kritis" to (Color(0xFFB91C1C) to Color(0xFFFEE2E2))
                else -> "Aman" to (Color(0xFF1E40AF) to Color(0xFFEBF5FF))
            }
            add(
                AppTileItem(
                    id = "stok",
                    label = "Stok",
                    icon = Icons.Default.Inventory2,
                    gradientColors = Color(0xFF60A5FA) to Color(0xFF1D4ED8),
                    iconColor = Color.White,
                    dotColor = if (adaKritis) Color(0xFFEF4444) else Color(0xFF2563EB),
                    statusText = statusText,
                    statusTextColor = statusColors.first,
                    statusBgColor = statusColors.second,
                    isLoading = state.memuatSorotan,
                    onClick = onOpenStok,
                )
            )
        }

        // 5. Distribusi (iOS Orchid Purple Gradient)
        if (isAllAccess || staff?.role in DISTRIBUSI_ROLES) {
            val adaMenunggu = (state.kirimanMenunggu ?: 0) > 0
            val (statusText, statusColors) = if (adaMenunggu) {
                "${state.kirimanMenunggu} Tunggu" to (Color(0xFF7E22CE) to Color(0xFFF3E8FF))
            } else {
                "Scan QR" to (Color(0xFF6D28D9) to Color(0xFFF3E8FF))
            }
            add(
                AppTileItem(
                    id = "distribusi",
                    label = "Distribusi",
                    icon = Icons.Default.LocalShipping,
                    gradientColors = Color(0xFFA78BFA) to Color(0xFF6D28D9),
                    iconColor = Color.White,
                    dotColor = if (adaMenunggu) Color(0xFFF59E0B) else Color(0xFF7C3AED),
                    statusText = statusText,
                    statusTextColor = statusColors.first,
                    statusBgColor = statusColors.second,
                    isLoading = state.memuatSorotan,
                    onClick = onOpenDistribusi,
                )
            )
        }

        // 6. Manager (iOS Rose Crimson Gradient)
        if (isAllAccess || staff?.role in MANAGER_ROLES) {
            val adaWaste = (state.wasteMenunggu ?: 0) > 0
            val (statusText, statusColors) = if (adaWaste) {
                "${state.wasteMenunggu} Waste" to (Color(0xFFBE123C) to Color(0xFFFFE4E6))
            } else {
                "Ringkasan" to (Color(0xFF9F1239) to Color(0xFFFFE4E6))
            }
            add(
                AppTileItem(
                    id = "manager",
                    label = "Manager",
                    icon = Icons.Default.Insights,
                    gradientColors = Color(0xFFFB7185) to Color(0xFFE11D48),
                    iconColor = Color.White,
                    dotColor = Color(0xFFE11D48),
                    statusText = statusText,
                    statusTextColor = statusColors.first,
                    statusBgColor = statusColors.second,
                    isLoading = state.memuatSorotan,
                    onClick = onOpenManager,
                )
            )
        }

        // 7. Leader (iOS Amber Gold Gradient)
        if (staff?.role in LEADER_ROLES) {
            val adaPetty = (state.pettyCashButuhAksi ?: 0) > 0
            val (statusText, statusColors) = if (adaPetty) {
                "${state.pettyCashButuhAksi} Siap" to (Color(0xFF6D28D9) to Color(0xFFF3E8FF))
            } else {
                "Cabang" to (Color(0xFF581C87) to Color(0xFFF3E8FF))
            }
            add(
                AppTileItem(
                    id = "leader",
                    label = "Leader",
                    icon = Icons.Default.Storefront,
                    gradientColors = Color(0xFFFBBF24) to Color(0xFFD97706),
                    iconColor = Color.White,
                    dotColor = Color(0xFFD97706),
                    statusText = statusText,
                    statusTextColor = statusColors.first,
                    statusBgColor = statusColors.second,
                    isLoading = state.memuatSorotan,
                    onClick = onOpenLeader,
                )
            )
        }
    }

    // Saat offline, modul yang memang tidak bisa jalan tanpa server diberi label — TIDAK
    // dikunci. Tile yang mati tidak memberi tahu apa pun; tile yang bisa ditekan dan
    // menjawab alasannya membuat orang berhenti mencoba dan tahu harus menunggu apa.
    val online by NetworkMonitor.isOnline.collectAsState()
    val konteks = LocalContext.current
    val displayedApps = apps.map { app ->
        val alasan = if (online) null else ALASAN_PERLU_INTERNET[app.id]
        if (alasan == null || app.isLocked) {
            app
        } else {
            app.copy(
                statusText = "Perlu internet",
                statusTextColor = Color(0xFF92400E),
                statusBgColor = Color(0xFFFEF3C7),
                onClick = { Toast.makeText(konteks, alasan, Toast.LENGTH_LONG).show() },
            )
        }
    }

    // Grid adaptif: 2 kolom jika tepat 4 aplikasi (2x2 simetris rapi),
    // atau 3 kolom jika 3 atau 6 aplikasi (3x1 atau 3x2). Tidak ada kartu menggantung sendirian ("buntu").
    val columns = if (displayedApps.size == 4) 2 else 3
    Column(Modifier.fillMaxWidth()) {
        displayedApps.chunked(columns).forEachIndexed { index, rowApps ->
            if (index > 0) Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                rowApps.forEachIndexed { posisi, app ->
                    if (posisi > 0) Spacer(Modifier.width(10.dp))
                    AppTileButton(app, Modifier.weight(1f))
                }
                repeat(columns - rowApps.size) {
                    Spacer(Modifier.width(10.dp))
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * Ubin kaca satu modul: kotak ikon bernuansa warna modul di atas, nama dan
 * baris status di bawah, lencana angka di sudut kanan atas.
 */
@Composable
private fun AppTileButton(item: AppTileItem, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && !item.isLocked) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "ios_app_icon_scale"
    )

    // Ujung gelap gradien lama dipakai ulang sebagai warna aksen ubin, jadi
    // identitas warna tiap modul tidak berubah dari versi sebelumnya.
    val warnaModul = item.gradientColors.second

    KartuKaca(
        modifier = modifier
            .heightIn(min = 96.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (item.isLocked) 0.62f else 1f
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = !item.isLocked,
                onClick = item.onClick
            ),
        radius = 22.dp,
    ) {
        Box(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(11.dp)) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(warnaModul.copy(alpha = 0.14f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(17.dp),
                            color = warnaModul,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (item.isLocked) Icons.Default.Lock else item.icon,
                            contentDescription = item.label,
                            tint = warnaModul,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    text = item.label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = IosTextPrimary,
                    letterSpacing = (-0.3).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(1.dp))
                Text(
                    text = item.statusText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = item.statusTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!item.isLocked && item.badge > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
                        .background(IosBadgeRed, RoundedCornerShape(10.dp))
                        .padding(horizontal = 5.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = buildString {
                            if (item.badgeSebutan) append("@")
                            append(if (item.badge > 99) "99+" else item.badge.toString())
                        },
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * Footer Beranda: Nomor versi aplikasi yang menyatu dengan lembar putih utama (tanpa efek terpotong).
 */
@Composable
private fun HomeScreenFooter(
    versionName: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = "v$versionName",
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        color = Color(0xFF94A3B8),
        letterSpacing = 0.4.sp,
        textAlign = TextAlign.Center
    )
}
