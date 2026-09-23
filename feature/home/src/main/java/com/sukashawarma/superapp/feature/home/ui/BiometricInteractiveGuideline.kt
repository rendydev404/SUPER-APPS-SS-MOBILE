package com.sukashawarma.superapp.presentation.home

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sukashawarma.superapp.core.auth.BiometricAuth
import com.sukashawarma.superapp.core.auth.findActivity
import com.sukashawarma.superapp.core.ui.ios.LencanaIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.ios.bayanganIos
import com.sukashawarma.superapp.core.ui.ios.tekanIos
import com.sukashawarma.superapp.data.local.AuthPrefs
import com.sukashawarma.superapp.data.remote.SessionTokenHolder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val OrangePrimary = Color(0xFFEA580C)
private val OrangeSecondary = Color(0xFFF97316)
private val OrangeSoftBg = Color(0xFFFFF7ED)
private val CardBg = Color.White
private val TextPrimary = Color(0xFF1C1C1E)
private val TextSecondary = Color(0xFF6B7280)
private val HairlineBorder = Color(0x18000000)

/**
 * Modal dialog coachmark interaktif yang memandu pengguna mengaktifkan fitur sidik jari (biometrik)
 * dengan animasi scanner radar dan aktivasi 1-sentuhan (one-tap activation) langsung di tempat.
 */
@Composable
fun BiometricCoachmarkDialog(
    userId: String,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
    onBiometricActivated: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val scope = rememberCoroutineScope()

    var isActivating by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Animasi radar denyut lingkaran pemindai
    val infiniteTransition = rememberInfiniteTransition(label = "biometric_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

    Dialog(
        onDismissRequest = {
            if (!isActivating && !isSuccess) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.60f))
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = CardBg,
                shadowElevation = 16.dp,
                border = BorderStroke(1.dp, Color(0x14000000)),
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Tombol Tutup Silang di Kanan Atas
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            enabled = !isActivating,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Tutup Panduan",
                                tint = Color(0xFF9CA3AF)
                            )
                        }
                    }

                    // Spotlight Radar Pemindai Sidik Jari Interaktif
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(110.dp)
                            .padding(bottom = 6.dp)
                    ) {
                        if (!isSuccess) {
                            // Gelombang Radar 1
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .graphicsLayer {
                                        scaleX = pulseScale
                                        scaleY = pulseScale
                                        alpha = pulseAlpha
                                    }
                                    .background(
                                        OrangePrimary.copy(alpha = 0.35f),
                                        CircleShape
                                    )
                            )
                            // Gelombang Radar 2 (lingkaran statis lembut)
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .background(
                                        OrangeSoftBg,
                                        CircleShape
                                    )
                            )
                        }

                        // Badge Pemindai Tengah
                        Surface(
                            shape = CircleShape,
                            color = if (isSuccess) Color(0xFF10B981) else OrangePrimary,
                            shadowElevation = 6.dp,
                            modifier = Modifier.size(62.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                if (isSuccess) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Sukses",
                                        tint = Color.White,
                                        modifier = Modifier.size(36.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = "Sidik Jari",
                                        tint = Color.White,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Lencana Tagline
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isSuccess) Color(0xFFD1FAE5) else Color(0xFFFFEDD5),
                        border = BorderStroke(
                            0.8.dp,
                            if (isSuccess) Color(0xFF6EE7B7) else Color(0xFFFDBA74)
                        )
                    ) {
                        Text(
                            text = if (isSuccess) "AKTIF & SIAP DIGUNAKAN" else "✨ REKOMENDASI PENGGUNA",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = if (isSuccess) Color(0xFF047857) else Color(0xFFC2410C),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.6.sp
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // Judul Utama
                    Text(
                        text = if (isSuccess) "Sidik Jari Berhasil Diaktifkan!" else "Masuk Lebih Cepat & Aman",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(6.dp))

                    // Subtitle / Deskripsi
                    Text(
                        text = if (isSuccess) {
                            "Mulai sekarang Anda dapat langsung membuka akun cukup dengan menempelkan jari pada sensor perangkat."
                        } else {
                            "Hemat waktu kerja Anda. Cukup satu sentuhan jari untuk masuk aplikasi tanpa perlu mengetik ulang kata sandi."
                        },
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(Modifier.height(18.dp))

                    if (!isSuccess) {
                        // 3 Pilar Keunggulan Interaktif (Apple Style List)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF9FAFB), RoundedCornerShape(16.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            GuidelinePillRow(
                                icon = Icons.Default.FlashOn,
                                iconColor = Color(0xFFD97706),
                                iconBg = Color(0xFFFEF3C7),
                                title = "1 Detik Masuk Aplikasi",
                                subtitle = "Langsung akses Absensi, POS, & Stok tanpa jeda"
                            )
                            GuidelinePillRow(
                                icon = Icons.Default.Lock,
                                iconColor = Color(0xFF059669),
                                iconBg = Color(0xFFD1FAE5),
                                title = "Terenkripsi di Perangkat",
                                subtitle = "Kunci biometrik terlindungi hardware aman Android"
                            )
                            GuidelinePillRow(
                                icon = Icons.Default.Shield,
                                iconColor = OrangePrimary,
                                iconBg = OrangeSoftBg,
                                title = "Bebas Lupa Kata Sandi",
                                subtitle = "Aman dan tidak repot saat pergantian shift kerja"
                            )
                        }

                        if (errorMessage != null) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = errorMessage!!,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(Modifier.height(20.dp))

                        // Tombol Aksi Utama: "⚡ Aktifkan Sekarang" (One-Tap Direct Setup)
                        Button(
                            onClick = {
                                if (isActivating) return@Button
                                if (activity == null) {
                                    errorMessage = "Aktivitas tidak valid. Buka lewat Pengaturan."
                                    return@Button
                                }
                                val refreshToken = SessionTokenHolder.refreshToken
                                if (refreshToken.isNullOrBlank()) {
                                    errorMessage = "Sesi belum tersimpan penuh. Silakan login ulang atau buka Pengaturan."
                                    return@Button
                                }

                                isActivating = true
                                errorMessage = null
                                scope.launch {
                                    val authenticated = BiometricAuth.authenticate(
                                        activity = activity,
                                        title = "Aktifkan Login Sidik Jari",
                                        subtitle = "Tempelkan jari pada sensor untuk mengonfirmasi aktivasi"
                                    )
                                    if (authenticated) {
                                        AuthPrefs.enableBiometric(userId, refreshToken)
                                        isSuccess = true
                                        onBiometricActivated()
                                        delay(1500)
                                        onDismiss()
                                    } else {
                                        isActivating = false
                                        errorMessage = "Aktivasi sidik jari dibatalkan. Coba lagi kapan saja."
                                    }
                                }
                            },
                            enabled = !isActivating,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .shadow(6.dp, RoundedCornerShape(14.dp)),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = OrangePrimary,
                                contentColor = Color.White
                            )
                        ) {
                            if (isActivating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "Menunggu Sensor...",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Aktifkan Sekarang",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // Tombol Sekunder: "Buka Pengaturan" & "Nanti Saja"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = onDismiss,
                                enabled = !isActivating
                            ) {
                                Text(
                                    text = "Nanti Saja",
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            TextButton(
                                onClick = {
                                    onDismiss()
                                    onOpenSettings()
                                },
                                enabled = !isActivating
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = null,
                                        tint = OrangePrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "Buka Pengaturan",
                                        color = OrangePrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    } else {
                        // Tampilan Berhasil
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981),
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = "Selesai",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Baris item keunggulan dengan icon bulat kecil dan teks informatif.
 */
@Composable
private fun GuidelinePillRow(
    icon: ImageVector,
    iconColor: Color,
    iconBg: Color,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = iconBg,
            modifier = Modifier.size(34.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = TextSecondary,
                lineHeight = 15.sp
            )
        }
    }
}

/**
 * Banner kartu interaktif yang disematkan di beranda (HomeScreen) sebagai pengingat
 * jika dialog utama telah ditutup tetapi fitur sidik jari belum diaktifkan.
 */
@Composable
fun BiometricGuidelineBanner(
    userId: String,
    onOpenSettings: () -> Unit,
    onShowCoachmark: () -> Unit = onOpenSettings,
    onDismissBanner: () -> Unit,
    onBiometricActivated: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val scope = rememberCoroutineScope()
    var isActivating by remember { mutableStateOf(false) }

    // Banner duduk di antara kartu iOS beranda, jadi memakai permukaan yang sama:
    // sudut 20, bayangan lembut, tanpa garis tepi.
    Surface(
        shape = UkuranIos.SudutKartu,
        color = WarnaIos.Kartu,
        modifier = modifier
            .fillMaxWidth()
            .bayanganIos()
            .tekanIos(onShowCoachmark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Squircle Fingerprint with Apple gradient
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        brush = Brush.linearGradient(
                            listOf(OrangePrimary, OrangeSecondary)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Sidik Jari",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            // Teks Informasi
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Login Sidik Jari",
                        style = TipeIos.Utama.copy(fontSize = 15.sp),
                        maxLines = 1,
                    )
                    Spacer(Modifier.width(6.dp))
                    LencanaIos("Baru", NadaIos.AKSEN, titik = false)
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Akses kilat tanpa repot ketik ulang kata sandi.",
                    style = TipeIos.Catatan.copy(lineHeight = 16.sp),
                )
            }

            Spacer(Modifier.width(8.dp))

            // Tombol Aktifkan Cepat (Apple-style pill)
            Button(
                onClick = {
                    if (isActivating) return@Button
                    if (activity == null) {
                        onOpenSettings()
                        return@Button
                    }
                    val refreshToken = SessionTokenHolder.refreshToken
                    if (refreshToken.isNullOrBlank()) {
                        onOpenSettings()
                        return@Button
                    }
                    isActivating = true
                    scope.launch {
                        val authenticated = BiometricAuth.authenticate(
                            activity = activity,
                            title = "Aktifkan Login Sidik Jari",
                            subtitle = "Tempelkan jari Anda pada sensor"
                        )
                        if (authenticated) {
                            AuthPrefs.enableBiometric(userId, refreshToken)
                            Toast.makeText(context, "Sidik jari berhasil diaktifkan!", Toast.LENGTH_SHORT).show()
                            onBiometricActivated()
                        }
                        isActivating = false
                    }
                },
                enabled = !isActivating,
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OrangePrimary,
                    contentColor = Color.White
                ),
                modifier = Modifier.height(32.dp)
            ) {
                if (isActivating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = Color.White,
                        strokeWidth = 1.5.dp
                    )
                } else {
                    Text(
                        text = "Aktifkan",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.width(4.dp))

            // Tombol Dismiss Banner
            IconButton(
                onClick = onDismissBanner,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Tutup Banner",
                    tint = Color(0xFF9CA3AF),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
