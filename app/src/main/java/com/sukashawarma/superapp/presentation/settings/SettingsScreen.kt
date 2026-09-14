package com.sukashawarma.superapp.presentation.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sukashawarma.superapp.data.local.AuthPrefs
import com.sukashawarma.superapp.data.remote.SessionTokenHolder
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.presentation.login.BiometricAuth
import kotlinx.coroutines.launch

// iOS Human Interface Design Tokens
private val IosBackground = Color(0xFFF2F2F7) // Apple System Grouped Background
private val IosCardBackground = Color(0xFFFFFFFF)
private val IosSeparator = Color(0xFFE5E5EA) // 0.5dp Hairline separator
private val IosLabel = Color(0xFF1C1C1E) // Primary label
private val IosSecondaryLabel = Color(0xFF8E8E93) // Secondary caption
private val IosSectionHeader = Color(0xFF6C6C70) // Section header
private val IosBorder = Color(0x14000000) // Hairline border

// SUKA Signature Brand Accents
private val SukaOrange = Color(0xFFEA580C)
private val SukaOrangeBg = Color(0xFFFFF4EC)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val scope = rememberCoroutineScope()
    var biometricEnabled by remember {
        mutableStateOf(AuthPrefs.isBiometricEnabledFor(AppSession.staff.value?.id))
    }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var isErrorMessage by remember { mutableStateOf(false) }

    val appVersion = remember(context) {
        try {
            val pInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            pInfo.versionName ?: "0.1.0"
        } catch (e: Exception) {
            "0.1.0"
        }
    }

    Scaffold(
        containerColor = IosBackground,
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Sleek iOS Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    border = BorderStroke(0.5.dp, IosBorder),
                    shadowElevation = 0.5.dp,
                    modifier = Modifier.size(36.dp)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = IosLabel,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Text(
                    text = "Pengaturan",
                    style = TextStyle(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = IosLabel,
                        letterSpacing = (-0.3).sp
                    ),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )

                // Spacer for visual symmetry with the back button
                Spacer(Modifier.size(36.dp))
            }

            Spacer(Modifier.height(8.dp))

            // Feedback Alert Banner
            AnimatedVisibility(message != null, enter = fadeIn(), exit = fadeOut()) {
                message?.let {
                    IosMessageBanner(
                        text = it,
                        isError = isErrorMessage
                    )
                }
            }

            // 2. Section: KEAMANAN & BIOMETRIK
            IosSettingsSectionHeader(title = "KEAMANAN & BIOMETRIK")

            // Rekomendasi Keamanan Banner (Gaya Apple iOS Suggestion Card)
            if (!biometricEnabled) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFFF7ED),
                    border = BorderStroke(0.6.dp, Color(0xFFFFD8B2)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(Color(0xFFFFEDD5), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = null,
                                tint = SukaOrange,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Saran Keamanan",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF9A3412)
                            )
                            Spacer(Modifier.height(1.dp))
                            Text(
                                text = "Aktifkan sidik jari di bawah untuk login instan tanpa ketik sandi.",
                                fontSize = 11.5.sp,
                                color = Color(0xFFC2410C),
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }

            IosSettingsCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Squircle Fingerprint Icon dengan gradient halus iOS
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    listOf(Color(0xFFEA580C), Color(0xFFF97316))
                                ),
                                shape = RoundedCornerShape(9.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Login Sidik Jari",
                                style = TextStyle(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = IosLabel
                                )
                            )
                            if (!biometricEnabled) {
                                Spacer(Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFFFF2E8),
                                    border = BorderStroke(0.5.dp, Color(0xFFFFD5C0))
                                ) {
                                    Text(
                                        text = "Disarankan",
                                        color = SukaOrange,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = if (biometricEnabled) "Aktif — masuk cepat otomatis dengan sensor perangkat."
                            else "Masuk instan tanpa perlu mengetik ulang kata sandi.",
                            style = TextStyle(
                                fontSize = 12.sp,
                                color = IosSecondaryLabel,
                                lineHeight = 16.sp
                            )
                        )
                    }

                    Spacer(Modifier.width(10.dp))

                    // Apple Style Switch
                    Switch(
                        checked = biometricEnabled,
                        onCheckedChange = { checked ->
                            if (busy) return@Switch
                            if (!checked) {
                                AuthPrefs.disableBiometric()
                                biometricEnabled = false
                                isErrorMessage = false
                                message = "Login sidik jari dinonaktifkan."
                            } else {
                                val currentActivity = activity
                                val userId = AppSession.staff.value?.id
                                val refreshToken = SessionTokenHolder.refreshToken
                                if (currentActivity == null || userId == null || refreshToken.isNullOrBlank()) {
                                    isErrorMessage = true
                                    message = "Sesi aktif tidak tersedia. Silakan login ulang."
                                } else if (!BiometricAuth.isAvailable(currentActivity)) {
                                    isErrorMessage = true
                                    message = "Perangkat belum memiliki biometrik yang dapat digunakan."
                                } else {
                                    busy = true
                                    scope.launch {
                                        val authenticated = BiometricAuth.authenticate(
                                            currentActivity,
                                            "Aktifkan login sidik jari"
                                        )
                                        if (authenticated) {
                                            AuthPrefs.enableBiometric(userId, refreshToken)
                                            biometricEnabled = true
                                            isErrorMessage = false
                                            message = "Login sidik jari berhasil diaktifkan!"
                                        } else {
                                            isErrorMessage = true
                                            message = "Verifikasi sidik jari dibatalkan."
                                        }
                                        busy = false
                                    }
                                }
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF34C759), // iOS Native Green
                            checkedBorderColor = Color.Transparent,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFE9E9EB),
                            uncheckedBorderColor = Color.Transparent
                        ),
                        enabled = !busy
                    )
                }
            }
            IosSettingsSectionFooter(
                text = "Biometrik memudahkan Anda masuk kembali ke aplikasi dengan cepat dan aman tanpa perlu mengetik ulang kata sandi."
            )

            if (busy) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    color = SukaOrange,
                    trackColor = Color(0xFFE5E5EA)
                )
            }

            Spacer(Modifier.height(18.dp))

            // 3. Section: INFORMASI APLIKASI
            IosSettingsSectionHeader(title = "TENTANG APLIKASI")
            IosSettingsCard {
                // Versi Aplikasi saja
                IosSettingsInfoRow(
                    icon = Icons.Default.Info,
                    iconBgColor = Color(0xFF007AFF), // Apple Blue
                    label = "Versi Aplikasi",
                    value = "v$appVersion"
                )
            }
            IosSettingsSectionFooter(
                text = "Aplikasi Resmi Operasional & Kepegawaian PT Suka Shawarma Indonesia."
            )

            Spacer(Modifier.height(36.dp))

            // Footer Copyright Meta
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SUKA SuperApp for Android",
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = IosSecondaryLabel,
                        letterSpacing = 0.2.sp
                    )
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "© 2026 PT Suka Shawarma Indonesia • All Rights Reserved",
                    style = TextStyle(
                        fontSize = 10.5.sp,
                        color = IosSecondaryLabel.copy(alpha = 0.8f)
                    )
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// iOS COMPONENT PRIMITIVES
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun IosSettingsSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = TextStyle(
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = IosSectionHeader,
            letterSpacing = 0.6.sp
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
    )
}

@Composable
private fun IosSettingsSectionFooter(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = TextStyle(
            fontSize = 12.sp,
            color = IosSecondaryLabel,
            lineHeight = 16.sp
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
    )
}

@Composable
private fun IosSettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = IosCardBackground,
        border = BorderStroke(0.5.dp, IosBorder),
        shadowElevation = 0.8.dp,
        content = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                content = content
            )
        }
    )
}

@Composable
private fun IosSettingsInfoRow(
    icon: ImageVector,
    iconBgColor: Color,
    label: String,
    value: String,
    showGreenDot: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(iconBgColor, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(17.dp)
            )
        }

        Spacer(Modifier.width(14.dp))

        Text(
            text = label,
            style = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = IosLabel
            ),
            modifier = Modifier.weight(1f)
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showGreenDot) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(Color(0xFF34C759), CircleShape)
                )
                Spacer(Modifier.width(5.dp))
            }
            Text(
                text = value,
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = IosSecondaryLabel
                )
            )
        }
    }
}

@Composable
private fun IosMessageBanner(
    text: String,
    isError: Boolean
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        color = if (isError) Color(0xFFFFECEB) else Color(0xFFEBFBF2),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            0.5.dp,
            if (isError) Color(0xFFFF3B30).copy(alpha = 0.35f) else Color(0xFF34C759).copy(alpha = 0.35f)
        ),
        shadowElevation = 0.5.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                if (isError) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (isError) Color(0xFFD70015) else Color(0xFF248A3D),
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = text,
                style = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isError) Color(0xFFD70015) else Color(0xFF248A3D),
                    lineHeight = 18.sp
                )
            )
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
