package com.sukashawarma.superapp.presentation.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sukashawarma.superapp.data.local.AuthPrefs
import com.sukashawarma.superapp.data.remote.SessionTokenHolder
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.presentation.login.BiometricAuth
import com.sukashawarma.superapp.presentation.theme.SukaOnSurface
import com.sukashawarma.superapp.presentation.theme.SukaOnSurfaceVariant
import com.sukashawarma.superapp.presentation.theme.SukaOrange
import com.sukashawarma.superapp.presentation.theme.SukaSurface
import kotlinx.coroutines.launch

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

    Scaffold(
        containerColor = SukaSurface,
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Keamanan akun", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = SukaOnSurface)
            Text("Atur cara aplikasi membuka sesi akun Anda.", color = SukaOnSurfaceVariant, fontSize = 13.sp)

            Surface(shape = RoundedCornerShape(20.dp), color = Color.White) {
                Row(
                    Modifier.fillMaxWidth().padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Fingerprint, null, tint = SukaOrange, modifier = Modifier.size(30.dp))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Login dengan sidik jari", fontWeight = FontWeight.Bold, color = SukaOnSurface)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (biometricEnabled) "Aktif — sidik jari diperlukan saat sesi dibuka kembali."
                            else "Aktifkan agar tidak perlu mengetik password setiap kali login.",
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = SukaOnSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = biometricEnabled,
                        onCheckedChange = { checked ->
                            if (busy) return@Switch
                            if (!checked) {
                                AuthPrefs.disableBiometric()
                                biometricEnabled = false
                                message = "Login sidik jari dinonaktifkan."
                            } else {
                                val currentActivity = activity
                                val userId = AppSession.staff.value?.id
                                val refreshToken = SessionTokenHolder.refreshToken
                                if (currentActivity == null || userId == null || refreshToken.isNullOrBlank()) {
                                    message = "Sesi aktif tidak tersedia. Silakan login ulang."
                                } else if (!BiometricAuth.isAvailable(currentActivity)) {
                                    message = "Perangkat belum memiliki biometrik yang dapat digunakan."
                                } else {
                                    busy = true
                                    scope.launch {
                                        val authenticated = BiometricAuth.authenticate(currentActivity, "Aktifkan login sidik jari")
                                        if (authenticated) {
                                            AuthPrefs.enableBiometric(userId, refreshToken)
                                            biometricEnabled = true
                                            message = "Login sidik jari berhasil diaktifkan."
                                        }
                                        busy = false
                                    }
                                }
                            }
                        },
                        enabled = !busy,
                    )
                }
            }

            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = SukaOrange)
            message?.let { Text(it, color = SukaOnSurfaceVariant, fontSize = 12.sp) }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
