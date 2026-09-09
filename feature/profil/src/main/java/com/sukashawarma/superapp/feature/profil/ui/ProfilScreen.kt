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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.core.camera.KameraFotoSheet
import com.sukashawarma.superapp.core.camera.keJpeg
import com.sukashawarma.superapp.core.ui.AvatarStaf
import com.sukashawarma.superapp.domain.model.StaffProfile
import com.sukashawarma.superapp.presentation.theme.*

/** Sisi foto profil yang disimpan. Avatar tidak pernah tampil lebih besar dari
 *  ~120dp di mana pun, jadi 512px sudah lebih dari cukup — dan jauh lebih cepat
 *  diunggah dari jaringan seluler outlet daripada foto kamera penuh. */
private const val SISI_AVATAR = 512

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilScreen(
    onExit: () -> Unit,
    /** Sebagian pemanggil menempatkan layar ini sebagai tab (tanpa tombol kembali),
     *  sebagian lagi sebagai halaman tersendiri. */
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
        containerColor = SukaSurface,
        topBar = {
            TopAppBar(
                title = { Text("Profil Saya", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                navigationIcon = {
                    if (tampilkanTombolKembali) {
                        IconButton(onClick = onExit) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SukaSurface),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding(),
        ) {
            KepalaProfil(
                staff = state.staff,
                sibuk = state.mengurusFoto,
                onUbahFoto = { pilihanFotoTerbuka = true },
            )

            AnimatedVisibility(state.pesan != null, enter = fadeIn(), exit = fadeOut()) {
                SpandukPesan(state.pesan)
            }

            KartuIdentitas(
                state = state,
                onNamaChange = viewModel::ubahNama,
                onUsernameChange = viewModel::ubahUsername,
                onSimpan = viewModel::simpanIdentitas,
            )

            KartuAkun(state.staff)

            KartuPassword(
                sedangGanti = state.menggantiPassword,
                passwordBaru = passwordBaru,
                konfirmasi = konfirmasiPassword,
                onPasswordBaruChange = { passwordBaru = it; viewModel.bersihkanPesan() },
                onKonfirmasiChange = { konfirmasiPassword = it; viewModel.bersihkanPesan() },
                onSimpan = {
                    viewModel.gantiPassword(passwordBaru, konfirmasiPassword) {
                        passwordBaru = ""
                        konfirmasiPassword = ""
                    }
                },
            )
            Spacer(Modifier.height(28.dp))
        }
    }

    if (pilihanFotoTerbuka) {
        ModalBottomSheet(onDismissRequest = { pilihanFotoTerbuka = false }, containerColor = Color.White) {
            Column(Modifier.padding(bottom = 24.dp)) {
                Text(
                    "Foto profil",
                    Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    color = SukaGray500,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                BarisPilihan(Icons.Default.PhotoCamera, "Ambil foto") {
                    pilihanFotoTerbuka = false
                    val punyaIzin = ContextCompat.checkSelfPermission(
                        konteks, Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                    if (punyaIzin) kameraTerbuka = true
                    else izinKamera.launch(Manifest.permission.CAMERA)
                }
                BarisPilihan(Icons.Default.PhotoLibrary, "Pilih dari galeri") {
                    pilihanFotoTerbuka = false
                    // Photo picker sistem: tidak menuntut izin READ_MEDIA_IMAGES sama
                    // sekali, karena pengguna sendiri yang memilih berkasnya.
                    galeri.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
                if (!state.staff?.avatarUrl.isNullOrBlank()) {
                    BarisPilihan(Icons.Default.DeleteOutline, "Hapus foto", warna = StatusRed) {
                        pilihanFotoTerbuka = false
                        viewModel.hapusFoto()
                    }
                }
            }
        }
    }

    if (kameraTerbuka) {
        ModalBottomSheet(onDismissRequest = { kameraTerbuka = false }, containerColor = Color.White) {
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

@Composable
private fun KepalaProfil(staff: StaffProfile?, sibuk: Boolean, onUbahFoto: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(SukaOrange.copy(alpha = 0.09f))
            .padding(vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            AvatarStaf(
                path = staff?.avatarUrl,
                nama = staff?.namaTampil,
                modifier = Modifier.size(104.dp).clickable(enabled = !sibuk, onClick = onUbahFoto),
                ukuranHuruf = 38.sp,
            )
            Surface(shape = CircleShape, color = SukaOrange, shadowElevation = 2.dp) {
                Box(Modifier.size(32.dp).clickable(enabled = !sibuk, onClick = onUbahFoto), contentAlignment = Alignment.Center) {
                    if (sibuk) {
                        CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.PhotoCamera, "Ubah foto profil", tint = Color.White, modifier = Modifier.size(17.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            staff?.namaTampil ?: "-",
            color = SukaInk,
            fontWeight = FontWeight.Bold,
            fontSize = 19.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        staff?.displayUsername?.takeIf { it.isNotBlank() }?.let {
            Text("@$it", color = SukaGray500, fontSize = 12.sp)
        }
        Spacer(Modifier.height(8.dp))
        Surface(color = SukaOrange.copy(alpha = 0.16f), shape = RoundedCornerShape(50)) {
            Text(
                staff?.roleRaw?.replace('_', ' ')?.lowercase()?.replaceFirstChar { it.titlecase() } ?: "Staff",
                Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                color = SukaInk,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun SpandukPesan(pesan: PesanProfil?) {
    val sukses = pesan is PesanProfil.Sukses
    val teks = when (pesan) {
        is PesanProfil.Sukses -> pesan.teks
        is PesanProfil.Galat -> pesan.teks
        null -> ""
    }
    Surface(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        color = if (sukses) StatusEmerald.copy(alpha = 0.10f) else StatusRed.copy(alpha = 0.08f),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                if (sukses) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                null,
                tint = if (sukses) StatusEmerald else StatusRed,
                modifier = Modifier.size(18.dp),
            )
            Text(teks, color = if (sukses) StatusEmerald else StatusRed, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun KartuIdentitas(
    state: ProfilUiState,
    onNamaChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onSimpan: () -> Unit,
) {
    Kartu(Icons.Default.Badge, "Identitas Tampilan") {
        IsianTeks(
            label = "Nama tampilan",
            placeholder = state.staff?.name ?: "Nama panggilan Anda",
            nilai = state.namaTampilan,
            onChange = onNamaChange,
            imeAction = ImeAction.Next,
        )
        Text(
            "Dikosongkan berarti memakai nama kepegawaian Anda.",
            Modifier.padding(top = 4.dp),
            color = SukaGray500,
            fontSize = 10.sp,
        )
        Spacer(Modifier.height(14.dp))
        IsianTeks(
            label = "Username tampilan",
            placeholder = "mis. budi.kasir",
            nilai = state.usernameTampilan,
            onChange = onUsernameChange,
            imeAction = ImeAction.Done,
            prefiks = "@",
        )
        Text(
            "Hanya untuk ditampilkan. Username login Anda tidak ikut berubah.",
            Modifier.padding(top = 4.dp),
            color = SukaGray500,
            fontSize = 10.sp,
        )
        Spacer(Modifier.height(16.dp))
        TombolUtama(
            teks = "Simpan Perubahan",
            ikon = Icons.Default.Save,
            sedangJalan = state.menyimpanIdentitas,
            aktif = state.identitasBerubah && !state.menyimpanIdentitas,
            onClick = onSimpan,
        )
    }
}

/** Data yang hanya bisa diubah admin/HR lewat web. Ditampilkan supaya staff bisa
 *  memastikan datanya benar dan tahu apa yang harus dilaporkan bila salah — bukan
 *  disembunyikan hanya karena tidak bisa disunting di sini. */
@Composable
private fun KartuAkun(staff: StaffProfile?) {
    Kartu(Icons.Default.ManageAccounts, "Data Kepegawaian") {
        BarisData("Nama kepegawaian", staff?.name)
        BarisData("Username login", staff?.username)
        BarisData("Outlet", staff?.outletName ?: "Semua Outlet")
        BarisData("Status", staff?.status?.replace('_', ' '))
        Text(
            "Perubahan data di atas hanya bisa dilakukan admin atau HR.",
            Modifier.padding(top = 10.dp),
            color = SukaGray500,
            fontSize = 10.sp,
            lineHeight = 15.sp,
        )
    }
}

@Composable
private fun KartuPassword(
    sedangGanti: Boolean,
    passwordBaru: String,
    konfirmasi: String,
    onPasswordBaruChange: (String) -> Unit,
    onKonfirmasiChange: (String) -> Unit,
    onSimpan: () -> Unit,
) {
    var lihatBaru by rememberSaveable { mutableStateOf(false) }
    var lihatKonfirmasi by rememberSaveable { mutableStateOf(false) }

    Kartu(Icons.Default.LockReset, "Ganti Password") {
        IsianPassword(
            label = "Password baru",
            placeholder = "Masukkan password baru",
            nilai = passwordBaru,
            terlihat = lihatBaru,
            onChange = onPasswordBaruChange,
            onToggle = { lihatBaru = !lihatBaru },
            imeAction = ImeAction.Next,
        )
        Spacer(Modifier.height(12.dp))
        IsianPassword(
            label = "Konfirmasi password baru",
            placeholder = "Ulangi password baru",
            nilai = konfirmasi,
            terlihat = lihatKonfirmasi,
            onChange = onKonfirmasiChange,
            onToggle = { lihatKonfirmasi = !lihatKonfirmasi },
            imeAction = ImeAction.Done,
        )
        Spacer(Modifier.height(16.dp))
        TombolUtama(
            teks = "Simpan Password Baru",
            ikon = Icons.Default.Save,
            sedangJalan = sedangGanti,
            aktif = !sedangGanti && passwordBaru.isNotBlank() && konfirmasi.isNotBlank(),
            onClick = onSimpan,
        )
    }
}

// ── Potongan tampilan yang dipakai berulang ────────────────────────────────

@Composable
private fun Kartu(ikon: ImageVector, judul: String, isi: @Composable ColumnScope.() -> Unit) {
    Surface(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        color = Color.White,
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(24.dp).background(SukaOrange.copy(alpha = 0.14f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(ikon, null, tint = SukaOrange, modifier = Modifier.size(15.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(judul, color = SukaInk, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            HorizontalDivider(Modifier.padding(vertical = 10.dp), color = SukaGray200)
            isi()
        }
    }
}

@Composable
private fun BarisData(label: String, nilai: String?) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
        Text(label, Modifier.width(130.dp), color = SukaGray500, fontSize = 11.sp)
        Text(
            nilai?.takeIf { it.isNotBlank() } ?: "-",
            Modifier.weight(1f),
            color = SukaInk,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun BarisPilihan(ikon: ImageVector, teks: String, warna: Color = SukaInk, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(ikon, null, tint = warna, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Text(teks, color = warna, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun TombolUtama(
    teks: String,
    ikon: ImageVector,
    sedangJalan: Boolean,
    aktif: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = aktif,
        modifier = Modifier.fillMaxWidth().height(44.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = SukaOrange,
            contentColor = Color.White,
            disabledContainerColor = SukaOrange.copy(alpha = 0.38f),
            disabledContentColor = Color.White.copy(alpha = 0.82f),
        ),
    ) {
        if (sedangJalan) {
            CircularProgressIndicator(Modifier.size(21.dp), color = Color.White, strokeWidth = 2.5.dp)
        } else {
            Icon(ikon, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(teks, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
private fun IsianTeks(
    label: String,
    placeholder: String,
    nilai: String,
    onChange: (String) -> Unit,
    imeAction: ImeAction,
    prefiks: String? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, color = SukaInk, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        OutlinedTextField(
            value = nilai,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = SukaInk, fontSize = 14.sp),
            placeholder = { Text(placeholder, color = SukaGray400, fontSize = 12.sp) },
            prefix = prefiks?.let { { Text(it, color = SukaGray500, fontSize = 14.sp) } },
            keyboardOptions = KeyboardOptions(imeAction = imeAction),
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SukaOrange,
                unfocusedBorderColor = SukaGray200,
                cursorColor = SukaOrange,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
            ),
        )
    }
}

@Composable
private fun IsianPassword(
    label: String,
    placeholder: String,
    nilai: String,
    terlihat: Boolean,
    onChange: (String) -> Unit,
    onToggle: () -> Unit,
    imeAction: ImeAction,
) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, color = SukaInk, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        OutlinedTextField(
            value = nilai,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = SukaInk, fontSize = 14.sp),
            placeholder = { Text(placeholder, color = SukaGray400, fontSize = 12.sp) },
            visualTransformation = if (terlihat) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = onToggle) {
                    Icon(
                        if (terlihat) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        if (terlihat) "Sembunyikan password" else "Tampilkan password",
                        tint = SukaGray500,
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SukaOrange,
                unfocusedBorderColor = SukaGray200,
                cursorColor = SukaOrange,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
            ),
        )
    }
}

/**
 * Membaca gambar pilihan galeri sambil langsung menyusutkannya.
 *
 * `inSampleSize` dihitung dulu lewat pembacaan header (`inJustDecodeBounds`) supaya
 * foto 12 MP dari galeri tidak pernah utuh di memori — itu jalur OutOfMemory yang
 * nyata di HP outlet kelas bawah, dan tidak ada gunanya karena hasilnya toh
 * disusutkan ke [SISI_AVATAR] sesudahnya.
 */
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
