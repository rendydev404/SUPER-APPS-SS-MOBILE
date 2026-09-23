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
import com.sukashawarma.superapp.core.ui.ios.BarisIos
import com.sukashawarma.superapp.core.ui.ios.LabelSeksiIos
import com.sukashawarma.superapp.core.ui.ios.LencanaIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.TombolBundarIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.ios.permukaanIos
import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.data.local.AuthPrefs
import com.sukashawarma.superapp.data.remote.SessionTokenHolder
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.presentation.login.BiometricAuth
import kotlinx.coroutines.launch

// Token lokal kini menunjuk ke design system iOS bersama (`core.ui.ios`) supaya
// Pengaturan satu bahasa dengan modul lain tanpa menyentuh setiap pemakaian.
private val IosBackground = WarnaIos.Latar
private val IosSecondaryLabel = WarnaIos.LabelKedua
private val SukaOrange = WarnaIos.Aksen

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
            // 1. Bilah navigasi iOS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TombolBundarIos(IkonIos.ArrowBack, "Kembali", onBack)

                Text(
                    text = "Pengaturan",
                    style = TipeIos.Utama,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )

                // Penyeimbang lebar tombol kembali supaya judul tepat di tengah
                Spacer(Modifier.size(38.dp))
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
            IosSettingsSectionHeader(title = "Keamanan & biometrik")

            // Rekomendasi Keamanan Banner (Gaya Apple iOS Suggestion Card)
            if (!biometricEnabled) {
                val nadaSaran = NadaIos.AKSEN
                Row(
                    modifier = Modifier
                        .padding(horizontal = UkuranIos.TepiLayar)
                        .padding(bottom = 10.dp)
                        .fillMaxWidth()
                        .background(nadaSaran.warna.copy(alpha = 0.10f), UkuranIos.SudutBlok)
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(nadaSaran.warna, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Saran Keamanan",
                            style = TipeIos.Catatan.copy(fontWeight = FontWeight.SemiBold, color = nadaSaran.teks)
                        )
                        Spacer(Modifier.height(1.dp))
                        Text(
                            text = "Aktifkan sidik jari di bawah untuk login instan tanpa ketik sandi.",
                            style = TipeIos.Catatan.copy(color = nadaSaran.teks, lineHeight = 17.sp)
                        )
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
                    // Ikon dalam lingkaran bernada, sama dengan `BarisIos`
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(WarnaIos.Aksen.copy(alpha = 0.14f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = WarnaIos.Aksen,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Login Sidik Jari",
                                style = TipeIos.Isi
                            )
                            if (!biometricEnabled) {
                                Spacer(Modifier.width(6.dp))
                                LencanaIos("Disarankan", NadaIos.AKSEN, titik = false)
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = if (biometricEnabled) "Aktif — masuk cepat otomatis dengan sensor perangkat."
                            else "Masuk instan tanpa perlu mengetik ulang kata sandi.",
                            style = TipeIos.Catatan
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
                            checkedTrackColor = WarnaIos.Hijau, // hijau sakelar iOS
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
                    trackColor = WarnaIos.Isian
                )
            }

            Spacer(Modifier.height(18.dp))

            // 3. Section: INFORMASI APLIKASI
            IosSettingsSectionHeader(title = "Tentang aplikasi")
            IosSettingsCard {
                // Versi Aplikasi saja
                BarisIos(
                    judul = "Versi Aplikasi",
                    ikon = Icons.Default.Info,
                    nadaIkon = NadaIos.INFO,
                    nilai = "v$appVersion"
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
                    style = TipeIos.Catatan.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "© 2026 PT Suka Shawarma Indonesia • All Rights Reserved",
                    style = TipeIos.Kecil.copy(color = WarnaIos.Abu)
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
    LabelSeksiIos(
        title,
        modifier
            .fillMaxWidth()
            .padding(start = 32.dp, end = 20.dp, top = 6.dp, bottom = 7.dp)
    )
}

@Composable
private fun IosSettingsSectionFooter(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = TipeIos.Catatan.copy(lineHeight = 18.sp),
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 32.dp, end = 32.dp, top = 7.dp, bottom = 6.dp)
    )
}

/** Wadah "inset grouped" iOS: permukaan putih sudut 16, bayangan lembut, tanpa garis tepi. */
@Composable
private fun IosSettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .padding(horizontal = UkuranIos.TepiLayar)
            .fillMaxWidth()
            .permukaanIos(UkuranIos.SudutGrup),
        content = content
    )
}

@Composable
private fun IosMessageBanner(
    text: String,
    isError: Boolean
) {
    val nada = if (isError) NadaIos.BAHAYA else NadaIos.SUKSES
    Row(
        modifier = Modifier
            .padding(horizontal = UkuranIos.TepiLayar, vertical = 6.dp)
            .fillMaxWidth()
            .background(nada.warna.copy(alpha = 0.12f), UkuranIos.SudutBlok)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            if (isError) IkonIos.ErrorOutline else IkonIos.CheckCircle,
            contentDescription = null,
            tint = nada.teks,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = text,
            style = TipeIos.Catatan.copy(color = nada.teks, fontWeight = FontWeight.Medium, lineHeight = 18.sp)
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
