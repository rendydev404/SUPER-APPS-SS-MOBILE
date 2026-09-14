package com.sukashawarma.superapp.feature.profil.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.core.camera.KameraFotoSheet
import com.sukashawarma.superapp.core.camera.keJpeg
import com.sukashawarma.superapp.core.ui.AvatarStaf
import com.sukashawarma.superapp.domain.model.Role
import com.sukashawarma.superapp.domain.model.StaffProfile
import com.sukashawarma.superapp.presentation.theme.*

private const val SISI_AVATAR = 512

// iOS Human Interface Design Tokens
private val IosBackground = Color(0xFFF2F2F7) // Apple System Grouped Background
private val IosCardBackground = Color(0xFFFFFFFF)
private val IosSeparator = Color(0xFFE5E5EA) // 0.5dp Hairline separator
private val IosLabel = Color(0xFF1C1C1E) // Primary label
private val IosSecondaryLabel = Color(0xFF8E8E93) // Secondary label
private val IosSectionHeader = Color(0xFF6C6C70) // Section header
private val IosBorder = Color(0x14000000) // Subtle hairline border

// Suka Brand Accents
private val SukaOrange = Color(0xFFEA580C)
private val SukaOrangeLight = Color(0xFFF29744)
private val SukaOrangeBg = Color(0xFFFFF4EC)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilScreen(
    onExit: () -> Unit,
    tampilkanTombolKembali: Boolean = true,
    viewModel: ProfilViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val konteks = LocalContext.current

    var pilihanFotoTerbuka by rememberSaveable { mutableStateOf(false) }
    var kameraTerbuka by rememberSaveable { mutableStateOf(false) }

    var passwordBaru by rememberSaveable { mutableStateOf("") }
    var konfirmasiPassword by rememberSaveable { mutableStateOf("") }

    val galeri = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        val bitmap = uri?.let { bacaGambar(konteks.contentResolver, it) }
        if (bitmap != null) viewModel.simpanFoto(bitmap.keJpeg(SISI_AVATAR, 82))
    }

    val izinKamera = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { diberi -> if (diberi) kameraTerbuka = true }

    Scaffold(
        containerColor = IosBackground,
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding(),
        ) {
            // 1. Sleek Apple Navigation Bar
            IosNavigationBar(
                tampilkanKembali = tampilkanTombolKembali,
                onKembali = onExit
            )

            // 2. Apple ID Centered Profile Hero
            AppleIdHero(
                staff = state.staff,
                sibuk = state.mengurusFoto,
                onUbahFoto = { pilihanFotoTerbuka = true }
            )

            // Feedback Alert Banner (if any)
            AnimatedVisibility(state.pesan != null, enter = fadeIn(), exit = fadeOut()) {
                IosStatusBanner(state.pesan)
            }

            Spacer(Modifier.height(8.dp))

            // 3. Section: USERNAME (iOS Inset Grouped)
            IosSectionHeader(title = "USERNAME")
            IosGroupedCard {
                IosInputRow(
                    iconText = "@",
                    iconBgColor = Color(0xFF5856D6), // Apple System Indigo
                    label = "Username",
                    value = state.usernameTampilan,
                    onValueChange = viewModel::ubahUsername,
                    placeholder = "username",
                    showCheckmark = state.usernameTampilan.isNotBlank(),
                    imeAction = ImeAction.Done
                )
            }
            IosSectionFooter(
                text = "Dikosongkan berarti memakai nama resmi. Jika diisi, username ini akan tampil sebagai nama Anda di profil, beranda, dan percakapan chat."
            )

            Spacer(Modifier.height(4.dp))

            // Apple Capsule Action Button for Identity
            IosCapsuleButton(
                text = "Simpan Username",
                icon = Icons.Default.Save,
                loading = state.menyimpanIdentitas,
                enabled = state.identitasBerubah && !state.menyimpanIdentitas,
                onClick = viewModel::simpanIdentitas
            )

            Spacer(Modifier.height(18.dp))

            // 4. Section: DATA KEPEGAWAIAN (iOS Inset Grouped List)
            IosSectionHeader(
                title = "DATA KEPEGAWAIAN",
                trailingContent = {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFEBFBF2),
                        border = BorderStroke(0.5.dp, Color(0xFF34C759).copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Verified,
                                contentDescription = null,
                                tint = Color(0xFF34C759),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = "Resmi HR",
                                color = Color(0xFF248A3D),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            )

            val tierText = when (state.staff?.role) {
                Role.ADMIN, Role.OWNER -> "Executive"
                Role.AREA_MANAGER, Role.REGIONAL_MANAGER -> "Manager"
                Role.LEADER -> "Tier 1"
                else -> "Operational"
            }

            IosGroupedCard {
                // Tile 1: Nama Resmi
                IosInfoRow(
                    icon = Icons.Default.Badge,
                    iconBgColor = Color(0xFF34C759), // Apple Green
                    label = "Nama Resmi",
                    value = state.staff?.name ?: "-",
                    trailingCaption = "Sesuai KTP"
                )

                HorizontalDivider(
                    modifier = Modifier.padding(start = 58.dp),
                    color = IosSeparator,
                    thickness = 0.5.dp
                )

                // Tile 2: Jabatan
                IosInfoRow(
                    icon = Icons.Default.Star,
                    iconBgColor = Color(0xFFFF9500), // Apple Amber/Orange
                    label = "Jabatan",
                    value = state.staff?.roleRaw?.replace('_', ' ')?.uppercase() ?: "STAFF",
                    trailingTag = tierText,
                    trailingTagColor = Color(0xFFB45309),
                    trailingTagBg = Color(0xFFFFFBEB)
                )

                HorizontalDivider(
                    modifier = Modifier.padding(start = 58.dp),
                    color = IosSeparator,
                    thickness = 0.5.dp
                )

                // Tile 3: Unit Kerja
                IosInfoRow(
                    icon = Icons.Default.LocationOn,
                    iconBgColor = Color(0xFFFF3B30), // Apple Red
                    label = "Unit Kerja",
                    value = state.staff?.outletName ?: "Semua Outlet"
                )
            }
            IosSectionFooter(
                text = "Data kepegawaian resmi dikelola dan diverifikasi oleh Departemen HR Pusat.",
                icon = Icons.Default.Lock
            )

            Spacer(Modifier.height(18.dp))

            // 5. Section: KEAMANAN & KATA SANDI (iOS Inset Grouped)
            var lihatBaru by rememberSaveable { mutableStateOf(false) }
            var lihatKonfirmasi by rememberSaveable { mutableStateOf(false) }

            IosSectionHeader(title = "KEAMANAN & KATA SANDI")
            IosGroupedCard {
                // Row 1: Password Baru
                IosPasswordRow(
                    iconBgColor = Color(0xFF8E8E93),
                    label = "Password Baru",
                    value = passwordBaru,
                    onValueChange = { passwordBaru = it; viewModel.bersihkanPesan() },
                    placeholder = "Masukkan password baru",
                    terlihat = lihatBaru,
                    onToggleTerlihat = { lihatBaru = !lihatBaru },
                    imeAction = ImeAction.Next
                )

                HorizontalDivider(
                    modifier = Modifier.padding(start = 58.dp),
                    color = IosSeparator,
                    thickness = 0.5.dp
                )

                // Row 2: Konfirmasi Password
                IosPasswordRow(
                    iconBgColor = Color(0xFF8E8E93),
                    label = "Konfirmasi Password Baru",
                    value = konfirmasiPassword,
                    onValueChange = { konfirmasiPassword = it; viewModel.bersihkanPesan() },
                    placeholder = "Ulangi password baru",
                    terlihat = lihatKonfirmasi,
                    onToggleTerlihat = { lihatKonfirmasi = !lihatKonfirmasi },
                    imeAction = ImeAction.Done
                )
            }
            IosSectionFooter(
                text = "Gunakan minimal 6 karakter kombinasi huruf dan angka demi keamanan akun."
            )

            Spacer(Modifier.height(4.dp))

            // Apple Capsule Action Button for Password
            IosCapsuleButton(
                text = "Perbarui Password",
                icon = Icons.Default.Key,
                loading = state.menggantiPassword,
                enabled = !state.menggantiPassword && passwordBaru.isNotBlank() && konfirmasiPassword.isNotBlank(),
                onClick = {
                    viewModel.gantiPassword(passwordBaru, konfirmasiPassword) {
                        passwordBaru = ""
                        konfirmasiPassword = ""
                    }
                }
            )

            // 6. iOS Footer Meta
            val appVersion = remember(konteks) {
                try {
                    val pInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        konteks.packageManager.getPackageInfo(konteks.packageName, PackageManager.PackageInfoFlags.of(0))
                    } else {
                        @Suppress("DEPRECATION")
                        konteks.packageManager.getPackageInfo(konteks.packageName, 0)
                    }
                    pInfo.versionName ?: "0.1.0"
                } catch (e: Exception) {
                    "0.1.0"
                }
            }

            Spacer(Modifier.height(28.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SUKA SuperApp • v$appVersion",
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = IosSecondaryLabel,
                        letterSpacing = 0.3.sp
                    )
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Sistem Kepegawaian Terintegrasi",
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = IosSecondaryLabel.copy(alpha = 0.8f)
                    )
                )
            }
        }
    }

    // Modal Action Sheet Foto Profil (Authentic iOS Style)
    if (pilihanFotoTerbuka) {
        ModalBottomSheet(
            onDismissRequest = { pilihanFotoTerbuka = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 10.dp)
                        .size(width = 36.dp, height = 5.dp)
                        .background(Color(0xFFD1D1D6), RoundedCornerShape(2.5.dp))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "FOTO PROFIL",
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = IosSectionHeader,
                        letterSpacing = 0.6.sp
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )

                // Actions Group
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF9F9FB),
                    border = BorderStroke(0.5.dp, IosBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        IosSheetActionRow(
                            icon = Icons.Default.PhotoCamera,
                            text = "Ambil Foto Kamera",
                            onClick = {
                                pilihanFotoTerbuka = false
                                val punyaIzin = ContextCompat.checkSelfPermission(
                                    konteks, Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                                if (punyaIzin) kameraTerbuka = true
                                else izinKamera.launch(Manifest.permission.CAMERA)
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 50.dp),
                            color = IosSeparator,
                            thickness = 0.5.dp
                        )
                        IosSheetActionRow(
                            icon = Icons.Default.PhotoLibrary,
                            text = "Pilih dari Galeri",
                            onClick = {
                                pilihanFotoTerbuka = false
                                galeri.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        )
                        if (!state.staff?.avatarUrl.isNullOrBlank()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 50.dp),
                                color = IosSeparator,
                                thickness = 0.5.dp
                            )
                            IosSheetActionRow(
                                icon = Icons.Default.DeleteOutline,
                                text = "Hapus Foto Profil",
                                textColor = Color(0xFFFF3B30),
                                onClick = {
                                    pilihanFotoTerbuka = false
                                    viewModel.hapusFoto()
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Separate Apple Action Sheet "Batal" (Cancel) Pill
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF9F9FB),
                    border = BorderStroke(0.5.dp, IosBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { pilihanFotoTerbuka = false }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Batal",
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SukaOrange
                            )
                        )
                    }
                }
            }
        }
    }

    if (kameraTerbuka) {
        ModalBottomSheet(
            onDismissRequest = { kameraTerbuka = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
                KameraFotoSheet(
                    onDiambil = { bitmap ->
                        kameraTerbuka = false
                        viewModel.simpanFoto(bitmap.keJpeg(SISI_AVATAR, 82))
                    },
                    onBatal = { kameraTerbuka = false },
                    labelAmbil = "Pakai Foto Ini",
                    kameraDepan = true,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// iOS COMPONENT PRIMITIVES (HIG ALIGNED)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Top iOS Navigation Bar with subtle back chevron and SUKA Online Status Pill
 */
@Composable
private fun IosNavigationBar(
    tampilkanKembali: Boolean,
    onKembali: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (tampilkanKembali) {
            Surface(
                shape = CircleShape,
                color = Color.White,
                border = BorderStroke(0.5.dp, IosBorder),
                shadowElevation = 0.5.dp,
                modifier = Modifier.size(36.dp)
            ) {
                IconButton(onClick = onKembali) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = IosLabel,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        } else {
            Spacer(Modifier.size(36.dp))
        }

        Text(
            text = "Profil",
            style = TextStyle(
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = IosLabel,
                letterSpacing = (-0.3).sp
            ),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )

        // Status Pill
        Surface(
            shape = RoundedCornerShape(50),
            color = Color(0xFFEBFBF2),
            border = BorderStroke(0.5.dp, Color(0xFF34C759).copy(alpha = 0.35f)),
            modifier = Modifier.height(28.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(Color(0xFF34C759), CircleShape)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = "ONLINE",
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF248A3D),
                        letterSpacing = 0.5.sp
                    )
                )
            }
        }
    }
}

/**
 * Centered Apple ID Hero with large avatar, camera badge, name, username, and role
 */
@Composable
private fun AppleIdHero(
    staff: StaffProfile?,
    sibuk: Boolean,
    onUbahFoto: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Large Centered Avatar with Camera Badge
        Box(contentAlignment = Alignment.BottomEnd) {
            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 3.dp,
                border = BorderStroke(3.dp, Color.White),
                modifier = Modifier.size(92.dp)
            ) {
                AvatarStaf(
                    path = staff?.avatarUrl,
                    nama = staff?.namaTampil,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(enabled = !sibuk, onClick = onUbahFoto),
                    ukuranHuruf = 34.sp
                )
            }

            // Camera Badge Button
            Surface(
                shape = CircleShape,
                color = SukaOrange,
                border = BorderStroke(2.dp, Color.White),
                shadowElevation = 2.dp,
                modifier = Modifier
                    .size(30.dp)
                    .offset(x = 2.dp, y = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(enabled = !sibuk, onClick = onUbahFoto),
                    contentAlignment = Alignment.Center
                ) {
                    if (sibuk) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.PhotoCamera,
                            contentDescription = "Ubah Foto",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        val hasCustomUsername = !staff?.displayUsername.isNullOrBlank()
        Text(
            text = staff?.namaTampil ?: "-",
            style = TextStyle(
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = IosLabel,
                letterSpacing = (-0.4).sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(2.dp))

        // Bila username diisi, tampilkan nama resmi sebagai keterangan di bawahnya.
        val subtitleText = if (hasCustomUsername) staff?.name else staff?.username?.let { "@$it" }
        if (!subtitleText.isNullOrBlank()) {
            Text(
                text = subtitleText,
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = IosSecondaryLabel
                )
            )
        }

        Spacer(Modifier.height(8.dp))

        // Role Badge Pill
        Surface(
            shape = RoundedCornerShape(50),
            color = SukaOrangeBg,
            border = BorderStroke(0.8.dp, SukaOrangeLight.copy(alpha = 0.35f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = SukaOrange,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = staff?.roleRaw?.replace('_', ' ')?.uppercase() ?: "STAFF",
                    style = TextStyle(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SukaOrange,
                        letterSpacing = 0.5.sp
                    )
                )
            }
        }
    }
}

/**
 * Section Header (Uppercase, small, SF Pro style)
 */
@Composable
private fun IosSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = IosSectionHeader,
                letterSpacing = 0.6.sp
            )
        )
        trailingContent?.invoke()
    }
}

/**
 * Section Footnote (Muted grey, explanatory note)
 */
@Composable
private fun IosSectionFooter(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = IosSecondaryLabel,
                modifier = Modifier
                    .size(13.dp)
                    .offset(y = 1.dp)
            )
            Spacer(Modifier.width(5.dp))
        }
        Text(
            text = text,
            style = TextStyle(
                fontSize = 12.sp,
                color = IosSecondaryLabel,
                lineHeight = 16.sp
            )
        )
    }
}

/**
 * iOS Inset Grouped Card Container
 */
@Composable
private fun IosGroupedCard(
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

/**
 * iOS Grouped Input Row
 */
@Composable
private fun IosInputRow(
    icon: ImageVector? = null,
    iconBgColor: Color = SukaOrange,
    iconTintColor: Color = Color.White,
    iconText: String? = null,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    trailingTag: String? = null,
    showCheckmark: Boolean = false,
    imeAction: ImeAction = ImeAction.Next
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Squircle Icon
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(iconBgColor, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = iconTintColor, modifier = Modifier.size(16.dp))
            } else if (iconText != null) {
                Text(iconText, color = iconTintColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = TextStyle(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = IosSecondaryLabel
                )
            )
            Spacer(Modifier.height(2.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = IosLabel
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = imeAction),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(placeholder, color = Color(0xFFC7C7CC), fontSize = 15.sp, fontWeight = FontWeight.Normal)
                    }
                    innerTextField()
                }
            )
        }

        if (trailingTag != null) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = SukaOrangeBg,
                border = BorderStroke(0.5.dp, SukaOrangeLight.copy(alpha = 0.35f))
            ) {
                Text(
                    text = trailingTag,
                    color = SukaOrange,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                )
            }
        } else if (showCheckmark) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(Color(0xFFEBFBF2), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Valid",
                    tint = Color(0xFF34C759),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/**
 * iOS Grouped Password Row
 */
@Composable
private fun IosPasswordRow(
    iconBgColor: Color = Color(0xFF8E8E93),
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    terlihat: Boolean,
    onToggleTerlihat: () -> Unit,
    imeAction: ImeAction
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(iconBgColor, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = TextStyle(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = IosSecondaryLabel
                )
            )
            Spacer(Modifier.height(2.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = IosLabel
                ),
                visualTransformation = if (terlihat) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(placeholder, color = Color(0xFFC7C7CC), fontSize = 15.sp, fontWeight = FontWeight.Normal)
                    }
                    innerTextField()
                }
            )
        }

        IconButton(onClick = onToggleTerlihat, modifier = Modifier.size(30.dp)) {
            Icon(
                if (terlihat) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                contentDescription = "Toggle password",
                tint = IosSecondaryLabel,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * iOS Grouped Read-Only Info Row with single-line label & right-aligned truncated value
 */
@Composable
private fun IosInfoRow(
    icon: ImageVector,
    iconBgColor: Color,
    label: String,
    value: String,
    trailingTag: String? = null,
    trailingTagColor: Color = SukaOrange,
    trailingTagBg: Color = SukaOrangeBg,
    trailingCaption: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(iconBgColor, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }

        Spacer(Modifier.width(12.dp))

        // Label: Fixed single-line so it NEVER wraps
        Text(
            text = label,
            style = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = IosLabel
            ),
            maxLines = 1
        )

        Spacer(Modifier.width(10.dp))

        // Value & Trailing elements: Right aligned, neatly truncated
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                style = TextStyle(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    color = IosSecondaryLabel
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f, fill = false)
            )

            if (trailingCaption != null) {
                Spacer(Modifier.width(6.dp))
                Text(
                    text = trailingCaption,
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = IosSecondaryLabel.copy(alpha = 0.8f)
                    ),
                    maxLines = 1
                )
            } else if (trailingTag != null) {
                Spacer(Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = trailingTagBg,
                    border = BorderStroke(0.5.dp, trailingTagColor.copy(alpha = 0.25f))
                ) {
                    Text(
                        text = trailingTag,
                        color = trailingTagColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * Apple Primary Capsule Action Button with Tactile Press Scale
 */
@Composable
private fun IosCapsuleButton(
    modifier: Modifier = Modifier,
    text: String,
    icon: ImageVector? = null,
    loading: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.97f else 1f,
        animationSpec = tween(100),
        label = "buttonScale"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(48.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled && !loading,
                onClick = onClick
            ),
        shape = RoundedCornerShape(14.dp),
        color = if (enabled) SukaOrange else Color(0xFFE5E5EA),
        shadowElevation = if (enabled) 2.dp else 0.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (icon != null) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = if (enabled) Color.White else Color(0xFF8E8E93),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = text,
                        style = TextStyle(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (enabled) Color.White else Color(0xFF8E8E93),
                            letterSpacing = (-0.2).sp
                        )
                    )
                }
            }
        }
    }
}

/**
 * iOS Status Toast / Alert Banner
 */
@Composable
private fun IosStatusBanner(pesan: PesanProfil?) {
    val sukses = pesan is PesanProfil.Sukses
    val teks = when (pesan) {
        is PesanProfil.Sukses -> pesan.teks
        is PesanProfil.Galat -> pesan.teks
        null -> ""
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        color = if (sukses) Color(0xFFEBFBF2) else Color(0xFFFFECEB),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            0.5.dp,
            if (sukses) Color(0xFF34C759).copy(alpha = 0.35f) else Color(0xFFFF3B30).copy(alpha = 0.35f)
        ),
        shadowElevation = 0.5.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                if (sukses) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = if (sukses) Color(0xFF248A3D) else Color(0xFFD70015),
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = teks,
                style = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (sukses) Color(0xFF248A3D) else Color(0xFFD70015),
                    lineHeight = 18.sp
                )
            )
        }
    }
}

/**
 * iOS Action Sheet Row for Photo Dialog
 */
@Composable
private fun IosSheetActionRow(
    icon: ImageVector,
    text: String,
    textColor: Color = IosLabel,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = text,
            style = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = textColor
            )
        )
    }
}

private fun bacaGambar(resolver: android.content.ContentResolver, uri: Uri): Bitmap? = try {
    val batas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, batas) }
    var contoh = 1
    while (maxOf(batas.outWidth, batas.outHeight) / contoh > SISI_AVATAR * 2) contoh *= 2
    val opsi = BitmapFactory.Options().apply { inSampleSize = contoh }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opsi) }
} catch (e: Exception) {
    android.util.Log.e("ProfilScreen", "bacaGambar() gagal", e)
    null
}
