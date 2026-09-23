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
import com.sukashawarma.superapp.core.ui.ios.LabelSeksiIos
import com.sukashawarma.superapp.core.ui.ios.LencanaIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.PemisahIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.TombolBundarIos
import com.sukashawarma.superapp.core.ui.ios.TombolUtamaIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.ios.permukaanIos
import com.sukashawarma.superapp.core.ui.ios.tekanIos
import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.domain.model.Role
import com.sukashawarma.superapp.domain.model.StaffProfile
import com.sukashawarma.superapp.presentation.theme.*

private const val SISI_AVATAR = 512

// Token lokal kini menunjuk ke design system iOS bersama (`core.ui.ios`) supaya
// halaman profil satu bahasa dengan modul lain tanpa menyentuh setiap pemakaian.
private val IosBackground = WarnaIos.Latar
private val IosCardBackground = WarnaIos.Kartu
private val IosSeparator = WarnaIos.Pemisah
private val IosLabel = WarnaIos.Label
private val IosSecondaryLabel = WarnaIos.LabelKedua

// Suka Brand Accents
private val SukaOrange = WarnaIos.Aksen

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
            IosSectionHeader(title = "Username")
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

            // Tombol aksi utama identitas
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
                title = "Data kepegawaian",
                trailingContent = {
                    LencanaIos("Resmi HR", NadaIos.SUKSES, ikon = Icons.Default.Verified)
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

                PemisahIos(inset = 58.dp)

                // Tile 2: Jabatan
                IosInfoRow(
                    icon = Icons.Default.Star,
                    iconBgColor = Color(0xFFFF9500), // Apple Amber/Orange
                    label = "Jabatan",
                    value = state.staff?.roleRaw?.replace('_', ' ')?.uppercase() ?: "STAFF",
                    trailingTag = tierText,
                    trailingTagNada = NadaIos.PERINGATAN
                )

                PemisahIos(inset = 58.dp)

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

            IosSectionHeader(title = "Keamanan & kata sandi")
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

                PemisahIos(inset = 58.dp)

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
                    style = TipeIos.Catatan.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Sistem Kepegawaian Terintegrasi",
                    style = TipeIos.Kecil.copy(color = WarnaIos.Abu)
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
                        .background(WarnaIos.LabelKetiga, RoundedCornerShape(2.5.dp))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
            ) {
                LabelSeksiIos("Foto profil", Modifier.padding(horizontal = 16.dp, vertical = 7.dp))

                // Kelompok aksi: grup abu ala action sheet iOS, tanpa garis tepi
                Surface(
                    shape = UkuranIos.SudutGrup,
                    color = IosBackground,
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
                        PemisahIos(inset = 50.dp)
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
                            PemisahIos(inset = 50.dp)
                            IosSheetActionRow(
                                icon = Icons.Default.DeleteOutline,
                                text = "Hapus Foto Profil",
                                textColor = WarnaIos.Merah,
                                onClick = {
                                    pilihanFotoTerbuka = false
                                    viewModel.hapusFoto()
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Tombol "Batal" terpisah ala action sheet iOS
                Surface(
                    shape = UkuranIos.SudutGrup,
                    color = IosBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .tekanIos({ pilihanFotoTerbuka = false })
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Batal",
                            style = TipeIos.Utama.copy(color = SukaOrange)
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
 * Bilah navigasi iOS: tombol kembali bulat, judul di tengah, lencana status di kanan.
 * Tidak memakai `BilahJudulIos` karena Scaffold sudah memberi padding status bar.
 */
@Composable
private fun IosNavigationBar(
    tampilkanKembali: Boolean,
    onKembali: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(84.dp), contentAlignment = Alignment.CenterStart) {
            if (tampilkanKembali) {
                TombolBundarIos(IkonIos.ArrowBack, "Kembali", onKembali)
            }
        }

        Text(
            text = "Profil",
            style = TipeIos.Utama,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )

        Box(Modifier.width(84.dp), contentAlignment = Alignment.CenterEnd) {
            LencanaIos("Online", NadaIos.SUKSES)
        }
    }
}

/**
 * Hero profil di tengah ala Apple ID: avatar besar, lencana kamera, nama, username, peran.
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
        Box(contentAlignment = Alignment.BottomEnd) {
            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 3.dp,
                border = BorderStroke(3.dp, Color.White),
                modifier = Modifier.size(96.dp)
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

            // Lencana kamera
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
                            IkonIos.PhotoCamera,
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
            style = TipeIos.Judul2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(2.dp))

        // Bila username diisi, tampilkan nama resmi sebagai keterangan di bawahnya.
        val subtitleText = if (hasCustomUsername) staff?.name else staff?.username?.let { "@$it" }
        if (!subtitleText.isNullOrBlank()) {
            Text(
                text = subtitleText,
                style = TipeIos.SubJudul
            )
        }

        Spacer(Modifier.height(10.dp))

        LencanaIos(
            teks = staff?.roleRaw?.replace('_', ' ')?.uppercase() ?: "STAFF",
            nada = NadaIos.AKSEN,
            ikon = Icons.Default.Star,
        )
    }
}

/**
 * Judul seksi kecil di atas grup, seperti di aplikasi Pengaturan iOS.
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
            .padding(start = 32.dp, end = 20.dp, top = 6.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        LabelSeksiIos(title)
        trailingContent?.invoke()
    }
}

/**
 * Catatan kaki abu di bawah grup.
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
            .padding(start = 32.dp, end = 32.dp, top = 7.dp, bottom = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = IosSecondaryLabel,
                modifier = Modifier
                    .size(13.dp)
                    .offset(y = 2.dp)
            )
            Spacer(Modifier.width(5.dp))
        }
        Text(
            text = text,
            style = TipeIos.Catatan.copy(lineHeight = 18.sp)
        )
    }
}

/**
 * Wadah "inset grouped" iOS — permukaan yang sama dengan `GrupIos`, dipakai
 * langsung karena judul seksinya di sini butuh lencana di kanan.
 */
@Composable
private fun IosGroupedCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .padding(horizontal = UkuranIos.TepiLayar)
            .fillMaxWidth()
            .permukaanIos(UkuranIos.SudutGrup, IosCardBackground),
        content = content
    )
}

/** Ikon baris ala Pengaturan iOS: kotak membulat berwarna, ikon/teks putih. */
@Composable
private fun IkonBaris(warna: Color, isi: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .background(warna, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
        content = isi
    )
}

/**
 * Baris isian dalam grup iOS.
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
        IkonBaris(iconBgColor) {
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
                style = TipeIos.Kecil.copy(fontWeight = FontWeight.Medium)
            )
            Spacer(Modifier.height(2.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = TipeIos.Isi,
                cursorBrush = androidx.compose.ui.graphics.SolidColor(WarnaIos.Aksen),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = imeAction),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(placeholder, style = TipeIos.Isi.copy(color = WarnaIos.LabelKetiga))
                    }
                    innerTextField()
                }
            )
        }

        if (trailingTag != null) {
            LencanaIos(trailingTag, NadaIos.AKSEN, titik = false)
        } else if (showCheckmark) {
            Icon(
                IkonIos.CheckCircle,
                contentDescription = "Valid",
                tint = WarnaIos.Hijau,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Baris kata sandi dalam grup iOS.
 */
@Composable
private fun IosPasswordRow(
    iconBgColor: Color = WarnaIos.Abu,
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
        IkonBaris(iconBgColor) {
            Icon(IkonIos.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = TipeIos.Kecil.copy(fontWeight = FontWeight.Medium)
            )
            Spacer(Modifier.height(2.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = TipeIos.Isi,
                cursorBrush = androidx.compose.ui.graphics.SolidColor(WarnaIos.Aksen),
                visualTransformation = if (terlihat) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(placeholder, style = TipeIos.Isi.copy(color = WarnaIos.LabelKetiga))
                    }
                    innerTextField()
                }
            )
        }

        IconButton(onClick = onToggleTerlihat, modifier = Modifier.size(30.dp)) {
            Icon(
                if (terlihat) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                contentDescription = "Toggle password",
                tint = WarnaIos.Abu,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Baris info baca-saja dalam grup iOS: label satu baris di kiri, nilai abu rata
 * kanan yang terpotong rapi. Tidak memakai `BarisIos` karena di sana nilai tidak
 * dibatasi lebarnya — outlet bernama panjang akan memaksa label turun baris.
 */
@Composable
private fun IosInfoRow(
    icon: ImageVector,
    iconBgColor: Color,
    label: String,
    value: String,
    trailingTag: String? = null,
    trailingTagNada: NadaIos = NadaIos.AKSEN,
    trailingCaption: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IkonBaris(iconBgColor) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }

        Spacer(Modifier.width(12.dp))

        // Label satu baris agar TIDAK pernah turun baris
        Text(
            text = label,
            style = TipeIos.Isi,
            maxLines = 1
        )

        Spacer(Modifier.width(10.dp))

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                style = TipeIos.Isi.copy(color = IosSecondaryLabel),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f, fill = false)
            )

            if (trailingCaption != null) {
                Spacer(Modifier.width(6.dp))
                Text(
                    text = trailingCaption,
                    style = TipeIos.Kecil.copy(color = WarnaIos.Abu),
                    maxLines = 1
                )
            } else if (trailingTag != null) {
                Spacer(Modifier.width(6.dp))
                LencanaIos(trailingTag, trailingTagNada, titik = false)
            }
        }
    }
}

/**
 * Tombol aksi utama iOS dengan jarak tepi layar.
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
    TombolUtamaIos(
        teks = text,
        onKlik = onClick,
        modifier = modifier.padding(horizontal = UkuranIos.TepiLayar),
        aktif = enabled,
        memuat = loading,
        ikon = icon,
    )
}

/**
 * Banner status iOS: isian tipis bernada, tanpa garis tepi.
 */
@Composable
private fun IosStatusBanner(pesan: PesanProfil?) {
    val sukses = pesan is PesanProfil.Sukses
    val nada = if (sukses) NadaIos.SUKSES else NadaIos.BAHAYA
    val teks = when (pesan) {
        is PesanProfil.Sukses -> pesan.teks
        is PesanProfil.Galat -> pesan.teks
        null -> ""
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = UkuranIos.TepiLayar, vertical = 6.dp)
            .clip(UkuranIos.SudutBlok)
            .background(nada.warna.copy(alpha = 0.12f))
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            if (sukses) IkonIos.CheckCircle else IkonIos.ErrorOutline,
            contentDescription = null,
            tint = nada.teks,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = teks,
            style = TipeIos.Catatan.copy(color = nada.teks, fontWeight = FontWeight.Medium, lineHeight = 18.sp)
        )
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
            .heightIn(min = 50.dp)
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
            style = TipeIos.Isi.copy(color = textColor)
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
