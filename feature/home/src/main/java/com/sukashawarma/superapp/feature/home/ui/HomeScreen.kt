package com.sukashawarma.superapp.presentation.home

import android.widget.Toast
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
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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

// Canonical Apple iOS Color Palette
private val IosSystemBg = Color(0xFFF2F2F7) // iOS System Grouped Background
private val IosCardBg = Color.White
private val IosHairline = Color(0x18000000) // 0.5pt subtle hairline border
private val IosTextPrimary = Color(0xFF1C1C1E) // iOS Label
private val IosTextSecondary = Color(0xFF8E8E93) // iOS Secondary Label
private val IosChevronColor = Color(0xFFC7C7CC) // iOS Disclosure Indicator
private val IosBadgeRed = Color(0xFFFF3B30) // Apple System Red

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

    val isBiometricAvailable = remember(activity) {
        BiometricAuth.isAvailable(activity)
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

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .background(IosSystemBg)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            // 1. iOS Top Navigation Bar (Status Capsule + Controls)
            IosTopNavBar(
                onOpenSettings = onOpenSettings,
                onLoggedOut = { viewModel.logout(); onLoggedOut() },
                highlightSettings = showSettingsHighlight
            )

            Spacer(Modifier.height(14.dp))

            // 2. iOS Large Title Header
            IosLargeTitleHeader(
                dateLabel = state.dateLabel,
                userName = staff?.namaTampil ?: "Pengguna",
                greeting = state.greeting
            )

            Spacer(Modifier.height(16.dp))

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
                Spacer(Modifier.height(14.dp))
            }

            // 3. Apple ID / Staff Profile Widget Card
            IosProfileWidget(
                staff = staff,
                onOpenProfil = onOpenProfil
            )

        Spacer(Modifier.height(14.dp))

        // 4. iOS Live Activity Attendance Widget
        IosAttendanceWidget(
            state = state,
            onOpenAbsensi = onOpenAbsensi
        )

        Spacer(Modifier.height(24.dp))

        // 5. iOS Section Header: Aplikasi Operasional
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "APLIKASI OPERASIONAL",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = IosTextSecondary
            )
            Surface(
                shape = RoundedCornerShape(50),
                color = SukaOrange.copy(alpha = 0.12f)
            ) {
                Text(
                    text = "TERSEDIA",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.5.dp),
                    color = Color(0xFFEA580C),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.6.sp
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // 6. iOS SpringBoard / App Library Bento Container
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

        Spacer(Modifier.height(36.dp))
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
 * Trademark Apple iOS Large Title Header (like App Store Today / Apple Fitness).
 */
@Composable
private fun IosLargeTitleHeader(
    dateLabel: String,
    userName: String,
    greeting: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // iOS Uppercase Date & Greeting Micro-Line
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = dateLabel.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                color = IosTextSecondary
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "•",
                fontSize = 11.sp,
                color = IosTextSecondary
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector = getGreetingIconVector(greeting),
                contentDescription = null,
                tint = Color(0xFFEA580C),
                modifier = Modifier.size(12.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = greeting,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFEA580C)
            )
        }
        Spacer(Modifier.height(3.dp))
        // iOS SF Pro Large Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Halo, $userName",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.6).sp,
                color = IosTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "👋",
                fontSize = 22.sp
            )
        }
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
        color = IosCardBg,
        border = BorderStroke(0.5.dp, IosHairline),
        shadowElevation = 1.5.dp
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
        shape = RoundedCornerShape(22.dp),
        color = IosCardBg,
        border = BorderStroke(0.5.dp, IosHairline),
        shadowElevation = 1.5.dp
    ) {
        Column(Modifier.padding(16.dp)) {
            // Widget Top Row: Orange glyph + Title + Status Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = FaceRecognitionIcon,
                    contentDescription = null,
                    tint = Color(0xFFEA580C),
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "KEHADIRAN HARI INI",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = Color(0xFFEA580C)
                )

                Spacer(Modifier.weight(1f))

                Surface(
                    shape = RoundedCornerShape(50),
                    color = status.second.copy(alpha = 0.10f),
                    border = BorderStroke(0.5.dp, status.second.copy(alpha = 0.25f))
                ) {
                    Text(
                        text = if (att == null) "Belum Masuk" else "Tercatat",
                        color = status.second,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.5.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF2F2F7))
            Spacer(Modifier.height(12.dp))

            // Main Content Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Chromatic Icon Squircle
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(status.second.copy(alpha = 0.12f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = status.first,
                        contentDescription = null,
                        tint = status.second,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        text = status.third,
                        color = IosTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.2).sp
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = if (att == null) "Ketuk untuk mencatat kehadiran" else "Kehadiran sudah tercatat dengan rapi",
                        color = IosTextSecondary,
                        fontSize = 11.5.sp
                    )
                }

                Spacer(Modifier.width(8.dp))

                // Apple-style CTA Button Capsule
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFFEA580C)
                ) {
                    Row(
                        modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (att == null) "Absen" else "Lihat",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Apple Callout Quote
            Text(
                text = "Jaga kedisiplinan dan tetap semangat hari ini.",
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8F9FA), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                color = IosTextSecondary,
                fontSize = 10.5.sp
            )
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

        // 4. Stok (iOS Sapphire Indigo Gradient)
        if (staff?.role in STOK_ROLES) {
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
        if (staff?.role in DISTRIBUSI_ROLES) {
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
        if (staff?.role in MANAGER_ROLES) {
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

    // Pada varian release, modul operasional selain Absensi dikunci (disable + ikon gembok)
    val isRelease = !com.sukashawarma.superapp.feature.home.BuildConfig.DEBUG
    val displayedApps = apps.map { app ->
        if (isRelease && app.id != "absensi") {
            app.copy(
                isLocked = true,
                statusText = "Terkunci",
                statusTextColor = Color(0xFF64748B),
                statusBgColor = Color(0xFFF1F5F9),
                onClick = {}
            )
        } else {
            app
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = IosCardBg,
        border = BorderStroke(0.5.dp, IosHairline),
        shadowElevation = 1.5.dp
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 18.dp)) {
            displayedApps.chunked(4).forEachIndexed { index, rowApps ->
                if (index > 0) Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    rowApps.forEach { app ->
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            AppTileButton(app)
                        }
                    }
                    repeat(4 - rowApps.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/**
 * Authentic Apple iOS SpringBoard Squircle App Icon Button.
 */
@Composable
private fun AppTileButton(item: AppTileItem) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && !item.isLocked) 0.88f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "ios_app_icon_scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
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
            )
            .padding(vertical = 4.dp, horizontal = 2.dp)
    ) {
        // Continuous Squircle Icon Container (iOS App Icon)
        Box(
            modifier = Modifier.size(56.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(item.gradientColors.first, item.gradientColors.second)
                        )
                    )
                    .border(
                        BorderStroke(0.8.dp, Color.White.copy(alpha = 0.35f)),
                        RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (item.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = item.iconColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            // Lock badge, Apple Red Badge, or Status Dot
            if (item.isLocked) {
                Box(
                    modifier = Modifier
                        .size(19.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 3.dp, y = (-3).dp)
                        .background(Color(0xFF475569), CircleShape)
                        .border(1.5.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Terkunci",
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                }
            } else if (item.badge > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp)
                        .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
                        .background(IosBadgeRed, RoundedCornerShape(9.dp))
                        .border(2.dp, Color.White, RoundedCornerShape(9.dp))
                        .padding(horizontal = 4.5.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = buildString {
                            if (item.badgeSebutan) append("@")
                            append(if (item.badge > 99) "99+" else item.badge.toString())
                        },
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(11.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = (-1).dp)
                        .background(item.dotColor, CircleShape)
                        .border(2.dp, Color.White, CircleShape)
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        // Title Label
        Text(
            text = item.label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = IosTextPrimary,
            letterSpacing = (-0.2).sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(3.dp))

        // Status Pill Badge (iOS Style)
        Surface(
            shape = RoundedCornerShape(50),
            color = item.statusBgColor
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.isLocked) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = item.statusTextColor,
                        modifier = Modifier.size(9.dp)
                    )
                    Spacer(Modifier.width(2.5.dp))
                }
                Text(
                    text = item.statusText,
                    color = item.statusTextColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 11.sp,
                    maxLines = 1
                )
            }
        }
    }
}
