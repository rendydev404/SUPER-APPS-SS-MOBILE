package com.sukashawarma.superapp.feature.chat.ui.pribadi

import com.sukashawarma.superapp.feature.chat.data.FavoritStiker
import com.sukashawarma.superapp.feature.chat.data.StikerKlipy
import com.sukashawarma.superapp.feature.chat.ui.stiker.PapanEmojiStiker
import com.sukashawarma.superapp.feature.chat.ui.stiker.TataPesanStiker
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.TombolBundarIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import com.sukashawarma.superapp.feature.chat.data.ChatWallpaperPrefs
import com.sukashawarma.superapp.feature.chat.data.ChatWallpapers
import com.sukashawarma.superapp.feature.chat.data.WallpaperLatarChat
import com.sukashawarma.superapp.feature.chat.ui.PilihWallpaperSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sukashawarma.superapp.feature.chat.data.ChatMediaStorage
import com.sukashawarma.superapp.core.camera.KameraFotoSheet
import com.sukashawarma.superapp.core.ui.AvatarStaf
import com.sukashawarma.superapp.core.ui.AvatarStorage
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.domain.util.JakartaTime
import com.sukashawarma.superapp.feature.chat.ChatKehadiran
import com.sukashawarma.superapp.feature.chat.data.PesanPribadi
import com.sukashawarma.superapp.feature.chat.data.PrivateChatRepository
import com.sukashawarma.superapp.feature.chat.ui.suara.BubbleSuara
import com.sukashawarma.superapp.feature.chat.ui.suara.PemutarSuara
import com.sukashawarma.superapp.feature.chat.ui.suara.PitaKomposerSuara
import com.sukashawarma.superapp.feature.chat.ui.suara.TombolMicChat
import com.sukashawarma.superapp.feature.chat.ui.suara.PerekamSuara
import com.sukashawarma.superapp.feature.chat.ui.suara.StatusRekam
import com.sukashawarma.superapp.feature.chat.ui.suara.rememberPerekamSuara
import com.sukashawarma.superapp.feature.chat.ui.suara.rememberStatusRekam
import com.sukashawarma.superapp.feature.chat.data.ReaksiPesan
import com.sukashawarma.superapp.feature.chat.ui.DaftarReaksiSheet
import com.sukashawarma.superapp.feature.chat.ui.FotoChat
import com.sukashawarma.superapp.feature.chat.ui.FotoTerpilih
import com.sukashawarma.superapp.feature.chat.ui.LembarPilihSumberFoto
import com.sukashawarma.superapp.feature.chat.ui.MenuPesanPopup
import com.sukashawarma.superapp.core.ui.PenampilFoto
import com.sukashawarma.superapp.feature.chat.ui.PratinjauKirimFoto
import com.sukashawarma.superapp.feature.chat.ui.emoji.PapanEmoji
import com.sukashawarma.superapp.feature.chat.ui.emoji.hapusSatuKarakter
import com.sukashawarma.superapp.feature.chat.domain.formatJamWib
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.format.DateTimeFormatter

private val BiruIos = WarnaIos.Biru
private val TeksSekunder = WarnaIos.Abu
private val LatarWallpaper = Color(0xFFEFE7DE)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayarChatPribadi(
    partnerId: String,
    partnerName: String,
    partnerAvatar: String?,
    onBack: () -> Unit,
    viewModel: PrivateChatDetailViewModel = remember(partnerId) {
        PrivateChatDetailViewModel(partnerId = partnerId)
    }
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    // Voice note
    val perekam = rememberPerekamSuara()
    val statusRekam = rememberStatusRekam()
    DisposableEffect(Unit) {
        onDispose { PemutarSuara.hentikan() }
    }
    // Catat percakapan ini sebagai yang sedang dibuka: push dari lawan bicara
    // yang sama tidak perlu berbunyi, sedangkan dari orang lain tetap tampil.
    DisposableEffect(partnerId) {
        ChatKehadiran.masukPribadi(partnerId)
        onDispose { ChatKehadiran.keluarPribadi(partnerId) }
    }

    // Alur foto WhatsApp style
    var lembarSumber by remember { mutableStateOf(false) }
    var kameraTerbuka by remember { mutableStateOf(false) }
    var fotoPratinjau by remember { mutableStateOf<FotoTerpilih?>(null) }
    var sedangKompres by remember { mutableStateOf(false) }
    var fotoDibuka by remember { mutableStateOf<Pair<String, String>?>(null) }

    // Menu reaksi & konteks
    var menuPesan by remember { mutableStateOf<PesanPribadi?>(null) }
    var konfirmasiHapus by remember { mutableStateOf<PesanPribadi?>(null) }
    var sheetReaksi by remember { mutableStateOf<PesanPribadi?>(null) }
    var pilihEmojiReaksi by remember { mutableStateOf<PesanPribadi?>(null) }

    // Sorot animasi jump to reply
    var sorotPesanId by remember { mutableStateOf<String?>(null) }
    var sorotKunci by remember { mutableIntStateOf(0) }

    val staff by AppSession.staff.collectAsState()
    val myId = staff?.id.orEmpty()
    val myAvatar = staff?.avatarUrl
    val isDeveloper = remember(staff) {
        ChatWallpapers.bolehUbahWallpaper(staff?.role, staff?.roleRaw)
    }

    fun siapkanFoto(kerja: suspend () -> ByteArray?) {
        sedangKompres = true
        scope.launch {
            val webp = withContext(Dispatchers.Default) { kerja() }
            sedangKompres = false
            if (webp == null) {
                Toast.makeText(context, "Foto tidak bisa dibaca. Coba foto lain.", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val bitmap = withContext(Dispatchers.Default) {
                BitmapFactory.decodeByteArray(webp, 0, webp.size)?.asImageBitmap()
            }
            if (bitmap == null) {
                Toast.makeText(context, "Foto tidak bisa ditampilkan.", Toast.LENGTH_SHORT).show()
                return@launch
            }
            fotoPratinjau = FotoTerpilih(bitmap, webp)
        }
    }

    val pilihFoto = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        siapkanFoto { FotoChat.kompres(context, uri) }
    }

    val izinKamera = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { diberi ->
        if (diberi) kameraTerbuka = true
        else Toast.makeText(context, "Izin kamera diperlukan untuk memotret.", Toast.LENGTH_SHORT).show()
    }

    // Auto-scroll ke pesan terbaru saat daftar bertambah
    LaunchedEffect(state.pesanList.size) {
        if (state.pesanList.isNotEmpty()) {
            listState.animateScrollToItem(state.pesanList.size)
        }
    }

    // Reset sorotan setelah durasi animasi selesai
    LaunchedEffect(sorotPesanId, sorotKunci) {
        if (sorotPesanId == null) return@LaunchedEffect
        delay(2100)
        sorotPesanId = null
    }

    LaunchedEffect(Unit) {
        viewModel.pesanGalat.collect { galat ->
            Toast.makeText(context, galat, Toast.LENGTH_SHORT).show()
        }
    }

    var sheetWallpaperPribadi by remember { mutableStateOf(false) }
    var revisiWallpaperPribadi by remember { mutableIntStateOf(0) }
    val wallpaperPribadiAktif = remember(revisiWallpaperPribadi, isDeveloper) {
        if (isDeveloper) {
            ChatWallpaperPrefs.getWallpaperPribadi(context)
        } else {
            val wallpaperTim = ChatWallpaperPrefs.getWallpaperTim(context)
            if (wallpaperTim != ChatWallpaperPrefs.ID_BAWAAN) wallpaperTim else ChatWallpaperPrefs.ID_DEFAULT
        }
    }
    val dimmingPribadiAktif = remember(revisiWallpaperPribadi, isDeveloper) {
        if (isDeveloper) {
            ChatWallpaperPrefs.getDimmingPribadi(context)
        } else {
            ChatWallpaperPrefs.getDimmingTim(context)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AvatarStaf(
                            path = partnerAvatar,
                            nama = partnerName,
                            modifier = Modifier.size(38.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = partnerName,
                                style = TipeIos.Utama,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "Obrolan Pribadi",
                                style = TipeIos.Kecil,
                            )
                        }
                    }
                },
                navigationIcon = {
                    TombolBundarIos(
                        IkonIos.ArrowBack,
                        "Kembali",
                        onBack,
                        Modifier.padding(start = 10.dp, end = 6.dp),
                    )
                },
                actions = {
                    if (isDeveloper) {
                        TombolBundarIos(
                            Icons.Filled.Wallpaper,
                            "Ganti Wallpaper",
                            { sheetWallpaperPribadi = true },
                            Modifier.padding(end = 12.dp),
                        )
                    } else {
                        TombolBundarIos(
                            IkonIos.Lock,
                            "Wallpaper Terkunci",
                            {
                                Toast.makeText(context, "Wallpaper chat dikunci oleh Developer.", Toast.LENGTH_SHORT).show()
                            },
                            Modifier.padding(end = 12.dp),
                            warnaIkon = WarnaIos.Abu,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WarnaIos.Kartu)
            )
        },
        modifier = Modifier.imePadding()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(LatarWallpaper)
        ) {
            // Daftar Pesan Chat Pribadi
            Box(modifier = Modifier.weight(1f)) {
                WallpaperLatarChat(
                    wallpaperId = wallpaperPribadiAktif,
                    dimming = dimmingPribadiAktif,
                )
                if (state.memuat && state.pesanList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BiruIos)
                    }
                } else if (state.pesanList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Belum ada pesan hari ini.\nKirim pesan untuk memulai obrolan!",
                            fontSize = 13.sp,
                            color = TeksSekunder,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        // Info Pesan Sementara ala WhatsApp
                        item(key = "info_pesan_sementara") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp, horizontal = 20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFE5E5EA).copy(alpha = 0.92f),
                                    shadowElevation = 0.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = TeksSekunder,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Pesan di obrolan ini otomatis terhapus setiap 03:00 WIB",
                                            fontSize = 11.5.sp,
                                            color = WarnaIos.AbuGelap,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }
                        }

                        items(state.pesanList, key = { it.id }) { pesan ->
                            val isMe = pesan.senderId == myId
                            val reaksiPesan = state.reaksi[pesan.id].orEmpty()

                            BarisBubblePribadi(
                                pesan = pesan,
                                isMe = isMe,
                                partnerName = partnerName,
                                reaksi = reaksiPesan,
                                myId = myId,
                                disorot = pesan.id == sorotPesanId,
                                sorotKunci = if (pesan.id == sorotPesanId) sorotKunci else 0,
                                onBalas = { viewModel.setBalas(it) },
                                onPutarSuara = { viewModel.tandaiSuaraDiputar(it) },
                                onTekanLama = { menuPesan = it },
                                onLompatKe = { idAsal ->
                                    val idx = state.pesanList.indexOfFirst { it.id == idAsal }
                                    if (idx >= 0) {
                                        scope.launch { listState.animateScrollToItem(idx + 1) }
                                        sorotPesanId = idAsal
                                        sorotKunci++
                                    } else {
                                        Toast.makeText(context, "Pesan aslinya sudah tidak ada.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onKetukReaksi = { sheetReaksi = it },
                                onKlikFoto = { url -> fotoDibuka = Pair(url, if (isMe) "Anda" else partnerName) }
                            )
                        }
                    }
                }
            }

            // Preview Kutipan Balasan (Jika aktif)
            val balas = state.balasTarget
            if (balas != null) {
                KartuBalasComposer(
                    target = balas,
                    namaBalasan = if (balas.senderId == myId) "Anda" else partnerName,
                    onTutup = { viewModel.batalBalas() }
                )
            }

            // Komposer Chat Input Bar ala WhatsApp / iOS
            KomposerChatPribadi(
                sedangMerekam = perekam.sedangMerekam,
                statusRekam = statusRekam,
                perekam = perekam,
                sedangKompres = sedangKompres,
                mengirim = state.mengirim,
                onBukaLampiran = { lembarSumber = true },
                onKirimTeks = { teks -> viewModel.kirim(body = teks) },
                onKirimSuara = { berkas, durasiMs, wave -> viewModel.kirimSuara(berkas, durasiMs, wave) },
                userId = myId,
                onKirimStiker = { viewModel.kirimStiker(it) },
            )
        }
    }

    // Modal pilih galeri / kamera
    if (lembarSumber) {
        LembarPilihSumberFoto(
            onGaleri = {
                lembarSumber = false
                pilihFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onKamera = {
                lembarSumber = false
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    kameraTerbuka = true
                } else {
                    izinKamera.launch(Manifest.permission.CAMERA)
                }
            },
            onTutup = { lembarSumber = false }
        )
    }

    // Kamera Sheet bila memilih kamera
    if (kameraTerbuka) {
        Dialog(onDismissRequest = { kameraTerbuka = false }) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White)
                    .padding(14.dp)
            ) {
                KameraFotoSheet(
                    onDiambil = { bitmap ->
                        kameraTerbuka = false
                        siapkanFoto { FotoChat.kompres(bitmap) }
                    },
                    onBatal = { kameraTerbuka = false },
                    labelAmbil = "Ambil"
                )
            }
        }
    }

    // Pratinjau Layar Penuh sebelum kirim foto (WhatsApp style)
    fotoPratinjau?.let { foto ->
        PratinjauKirimFoto(
            foto = foto,
            namaBalasan = state.balasTarget?.let { t ->
                if (t.senderId == myId) "Anda" else partnerName
            },
            onKirim = { keterangan ->
                viewModel.kirim(body = keterangan, fotoWebp = foto.webp)
                fotoPratinjau = null
            },
            onBatal = { fotoPratinjau = null }
        )
    }

    // Penampil Foto Layar Penuh (saat foto di chat diketuk)
    fotoDibuka?.let { (url, judul) ->
        PenampilFoto(
            url = url,
            judul = judul,
            keterangan = "Foto di Obrolan Pribadi",
            onTutup = { fotoDibuka = null }
        )
    }

    // Menu Tekan Lama (Reaksi Emoji Cepat & Opsi Pesan)
    menuPesan?.let { pesan ->
        val isMe = pesan.senderId == myId
        val emojiTerpilih = state.reaksi[pesan.id]?.firstOrNull { it.userId == myId }?.emoji

        MenuPesanPopup(
            milikSendiri = isMe,
            emojiTerpilih = emojiTerpilih,
            bolehHapus = isMe && pesan.deletedAtMs == null,
            bolehSunting = false,
            bolehInfo = false,
            // Teks pesan stiker hanya cadangan untuk app lama — tidak untuk disalin.
            adaTeks = pesan.body.isNotBlank() && pesan.stickerUrl == null,
            onEmoji = { emoji ->
                viewModel.toggleReaksi(pesan, emoji)
                menuPesan = null
            },
            onSemuaEmoji = {
                pilihEmojiReaksi = pesan
                menuPesan = null
            },
            onBalas = {
                viewModel.setBalas(pesan)
                menuPesan = null
            },
            onInfo = {},
            onSunting = {},
            onSalin = {
                clipboard.setText(AnnotatedString(pesan.body))
                menuPesan = null
            },
            onHapus = {
                konfirmasiHapus = pesan
                menuPesan = null
            },
            onTutup = { menuPesan = null },
            // Favorit disimpan lokal per akun; dibaca ulang di sini supaya labelnya
            // tepat walau papan stiker belum pernah dibuka sejak app dijalankan.
            labelFavorit = pesan.stickerUrl?.let { url ->
                FavoritStiker.muat(context, myId)
                if (FavoritStiker.ada(url)) "Hapus dari favorit" else "Simpan ke favorit"
            },
            onFavorit = {
                pesan.stickerUrl?.let { url ->
                    val kini = FavoritStiker.alihkan(context, myId, FavoritStiker.dariPesan(url))
                    Toast.makeText(context, if (kini) "Disimpan ke favorit" else "Dihapus dari favorit", Toast.LENGTH_SHORT).show()
                }
                menuPesan = null
            },
            bubble = {
                BubblePribadi(
                    pesan = pesan,
                    isMe = isMe,
                    partnerName = partnerName,
                    disorot = false,
                    onLompatKe = {},
                    onKlikFoto = {},
                    onTekanLama = {}
                )
            }
        )
    }

    // Lembar Papan Emoji Lengkap untuk Reaksi
    pilihEmojiReaksi?.let { pesan ->
        LembarSemuaEmoji(
            onPilih = { emoji ->
                viewModel.toggleReaksi(pesan, emoji)
                pilihEmojiReaksi = null
            },
            onTutup = { pilihEmojiReaksi = null }
        )
    }

    // Lembar Daftar Reaksi yang Masuk di Pesan
    sheetReaksi?.let { pesan ->
        val daftar = state.reaksi[pesan.id].orEmpty()
        if (daftar.isEmpty()) {
            sheetReaksi = null
        } else {
            DaftarReaksiSheet(
                reaksi = daftar,
                avatarPerUser = mapOf(
                    partnerId to partnerAvatar,
                    myId to myAvatar
                ),
                userId = myId,
                onCabut = {
                    val emojiku = daftar.firstOrNull { it.userId == myId }?.emoji
                    if (emojiku != null) viewModel.toggleReaksi(pesan, emojiku)
                    sheetReaksi = null
                },
                onTutup = { sheetReaksi = null }
            )
        }
    }

    // Dialog Konfirmasi Hapus Pesan
    konfirmasiHapus?.let { pesan ->
        AlertDialog(
            onDismissRequest = { konfirmasiHapus = null },
            title = { Text("Hapus pesan?") },
            text = { Text("Pesan ini akan dihapus untuk semua orang di obrolan ini.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.hapusPesan(pesan)
                        konfirmasiHapus = null
                    }
                ) {
                    Text("Hapus", color = WarnaIos.Merah, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { konfirmasiHapus = null }) {
                    Text("Batal")
                }
            }
        )
    }

    if (sheetWallpaperPribadi && isDeveloper) {
        PilihWallpaperSheet(
            wallpaperAwal = wallpaperPribadiAktif,
            dimmingAwal = dimmingPribadiAktif,
            bisaTerapkanSemua = false,
            judul = "Wallpaper Obrolan Pribadi",
            onTerapkanLengkap = { idBaru, dimming, _ ->
                if (isDeveloper) {
                    ChatWallpaperPrefs.setWallpaperPribadi(context, idBaru)
                    ChatWallpaperPrefs.setDimmingPribadi(context, dimming)
                    revisiWallpaperPribadi++
                    Toast.makeText(context, "Wallpaper obrolan pribadi diterapkan.", Toast.LENGTH_SHORT).show()
                }
            },
            onResetBawaan = {
                if (isDeveloper) {
                    ChatWallpaperPrefs.resetWallpaperPribadi(context)
                    revisiWallpaperPribadi++
                    Toast.makeText(context, "Wallpaper kembali ke bawaan.", Toast.LENGTH_SHORT).show()
                }
            },
            onTutup = { sheetWallpaperPribadi = false },
        )
    }
}

/**
 * Baris pembungkus bubble dengan gestur seret ke kanan untuk membalas pesan (swipe-to-reply).
 */
@Composable
private fun BarisBubblePribadi(
    pesan: PesanPribadi,
    isMe: Boolean,
    partnerName: String,
    reaksi: List<ReaksiPesan>,
    myId: String,
    disorot: Boolean,
    sorotKunci: Any,
    onBalas: (PesanPribadi) -> Unit,
    onTekanLama: (PesanPribadi) -> Unit,
    onLompatKe: (String) -> Unit,
    onKetukReaksi: (PesanPribadi) -> Unit,
    onKlikFoto: (String) -> Unit,
    onPutarSuara: (PesanPribadi) -> Unit,
) {
    val terhapus = pesan.deletedAtMs != null
    val haptic = LocalHapticFeedback.current

    var geser by remember { mutableStateOf(0f) }
    val geserAnim by animateFloatAsState(geser, label = "geserBalasPribadi")
    var sudahHaptic by remember { mutableStateOf(false) }
    val ambang = 150f

    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .then(
                if (terhapus) Modifier else Modifier.pointerInput(pesan.id) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (geser >= ambang) onBalas(pesan)
                            geser = 0f
                            sudahHaptic = false
                        },
                        onDragCancel = {
                            geser = 0f
                            sudahHaptic = false
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        geser = (geser + dragAmount).coerceIn(0f, ambang * 1.3f)
                        if (geser >= ambang && !sudahHaptic) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            sudahHaptic = true
                        }
                    }
                }
            )
    ) {
        // Ikon balas yang muncul di belakang saat digeser
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Reply,
            contentDescription = null,
            tint = TeksSekunder,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp)
                .size(22.dp)
                .alpha((geserAnim / ambang).coerceIn(0f, 1f))
        )

        Row(
            Modifier
                .fillMaxWidth()
                .offset { androidx.compose.ui.unit.IntOffset(geserAnim.toInt(), 0) },
            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
                BubblePribadi(
                    pesan = pesan,
                    isMe = isMe,
                    partnerName = partnerName,
                    disorot = disorot,
                    sorotKunci = sorotKunci,
                    onLompatKe = onLompatKe,
                    onKlikFoto = onKlikFoto,
                    onTekanLama = { if (!terhapus) onTekanLama(pesan) },
                    onPutarSuara = onPutarSuara,
                )

                if (reaksi.isNotEmpty() && !terhapus) {
                    KepingReaksi(
                        reaksi = reaksi,
                        userId = myId,
                        modifier = Modifier
                            .offset(y = (-4).dp)
                            .padding(horizontal = 6.dp)
                            .clickable { onKetukReaksi(pesan) }
                    )
                }
            }
        }
    }
}

/**
 * Komponen Bubble Pesan Pribadi.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BubblePribadi(
    pesan: PesanPribadi,
    isMe: Boolean,
    partnerName: String,
    disorot: Boolean = false,
    sorotKunci: Any = Unit,
    onLompatKe: (String) -> Unit,
    onKlikFoto: (String) -> Unit,
    onTekanLama: () -> Unit,
    onPutarSuara: (PesanPribadi) -> Unit = {},
) {
    val terhapus = pesan.deletedAtMs != null
    val jamFormatted = formatJamWib(pesan.createdAtMs)

    // Abu terang untuk pesan sendiri, abu lebih gelap untuk lawan bicara; biru
    // dipakai hanya sebagai aksen (centang, kutipan, tombol suara).
    val bubbleColor = if (isMe) Color(0xFFF4F4F7) else Color(0xFFDEDEE4)
    val textColor = Color(0xFF1C1C1E)
    val timeColor = Color(0xFF8E8E93)
    val denyut = if (disorot) denyutSorot(sorotKunci) else null

    // Stiker tanpa gelembung. `return` aman di badan fungsi composable (bukan lambda).
    val stiker = pesan.stickerUrl
    if (stiker != null && !terhapus) {
        TataPesanStiker(
            url = stiker,
            milikSendiri = isMe,
            jam = jamFormatted,
            onTekanLama = onTekanLama,
            kepala = {
                if (pesan.replyToId != null || !pesan.replyToSnippet.isNullOrBlank()) {
                    KutipanBalasanPribadi(
                        nama = pesan.replyToName ?: if (isMe) "Anda" else partnerName,
                        snippet = pesan.replyToSnippet.orEmpty(),
                        diBubbleSendiri = isMe,
                        onKlik = pesan.replyToId?.let { targetId -> { onLompatKe(targetId) } },
                        modifier = Modifier
                            .widthIn(max = 220.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(bubbleColor)
                            .padding(bottom = 2.dp),
                    )
                }
            },
            status = if (!isMe) null else {
                { KomponenCentangPribadi(status = pesan.statusCentang, warnaAbu = Color(0xFFA0A0A8)) }
            },
        )
        return
    }

    val bentuk = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = if (isMe) 16.dp else 4.dp,
        bottomEnd = if (isMe) 4.dp else 16.dp,
    )

    Column(
        modifier = Modifier
            .widthIn(max = 290.dp)
            .then(
                if (denyut == null) Modifier else Modifier.graphicsLayer {
                    val s = denyut.skala.value
                    scaleX = s
                    scaleY = s
                    translationX = denyut.getarX.value
                }
            )
            .clip(bentuk)
            .background(bubbleColor)
            .then(
                if (denyut == null) Modifier else Modifier.drawWithContent {
                    drawContent()
                    val maju = denyut.glow.value
                    if (maju <= 0f) return@drawWithContent
                    val garis = bentuk.createOutline(size, layoutDirection, this)

                    drawOutline(
                        garis,
                        Color(0xFFFF9500),
                        alpha = 0.32f * maju,
                        style = Stroke(width = 8.dp.toPx()),
                    )
                    drawOutline(
                        garis,
                        Color(0xFFFF9500),
                        alpha = 0.16f * maju,
                    )
                    drawOutline(
                        garis,
                        Color(0xFFFF6600),
                        alpha = maju,
                        style = Stroke(width = 2.8.dp.toPx()),
                    )
                }
            )
            .combinedClickable(
                onClick = {},
                onLongClick = onTekanLama
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        if (terhapus) {
            NisanPesanPribadi(
                jam = jamFormatted,
                diBubbleSendiri = isMe
            )
        } else {
            // Kotak kutipan jika pesan membalas pesan lain
            if (pesan.replyToId != null || !pesan.replyToSnippet.isNullOrBlank()) {
                val namaKutipan = pesan.replyToName ?: if (isMe) "Anda" else partnerName
                KutipanBalasanPribadi(
                    nama = namaKutipan,
                    snippet = pesan.replyToSnippet.orEmpty(),
                    diBubbleSendiri = isMe,
                    onKlik = pesan.replyToId?.let { targetId -> { onLompatKe(targetId) } },
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            // Pesan suara
            if (!pesan.audioPath.isNullOrBlank()) {
                BubbleSuara(
                    audioPath = pesan.audioPath,
                    audioMs = pesan.audioMs,
                    audioWave = pesan.audioWave,
                    milikSendiri = isMe,
                    // Satu penanda untuk dua sisi: di bubble sendiri berarti
                    // lawan bicara sudah mendengar, di bubble lawan berarti
                    // saya sendiri yang sudah mendengar.
                    sudahDiputar = pesan.audioPlayedAtMs != null,
                    onMulaiPutar = { onPutarSuara(pesan) },
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }

            // Thumbnail Foto
            if (!pesan.imagePath.isNullOrBlank()) {
                val context = LocalContext.current
                val url = PrivateChatRepository.urlFoto(pesan.imagePath)
                val request = remember(url) {
                    ImageRequest.Builder(context)
                        .data(url)
                        .size(720, 720)
                        .crossfade(true)
                        .build()
                }
                AsyncImage(
                    model = request,
                    imageLoader = ChatMediaStorage.imageLoader(context),
                    contentDescription = "Foto obrolan",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 140.dp, max = 280.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { url?.let { onKlikFoto(it) } }
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Teks Pesan
            if (pesan.body.isNotBlank()) {
                Text(
                    text = pesan.body,
                    fontSize = 15.sp,
                    color = textColor
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Jam dan Centang
            Row(
                modifier = Modifier.align(Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = jamFormatted,
                    fontSize = 10.5.sp,
                    color = timeColor
                )
                if (isMe) {
                    Spacer(modifier = Modifier.width(4.dp))
                    KomponenCentangPribadi(
                        status = pesan.statusCentang,
                        warnaAbu = Color(0xFFA0A0A8)
                    )
                }
            }
        }
    }
}

/**
 * Kutipan pesan yang dibalas (Reply card di dalam bubble).
 */
@Composable
private fun KutipanBalasanPribadi(
    nama: String,
    snippet: String,
    diBubbleSendiri: Boolean,
    modifier: Modifier = Modifier,
    onKlik: (() -> Unit)? = null,
) {
    val latar = Color(0x0F000000)
    val warnaBar = BiruIos
    val warnaJudul = BiruIos
    val warnaIsi = TeksSekunder

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .then(if (onKlik != null) Modifier.clickable(onClick = onKlik) else Modifier)
            .background(latar)
            .heightIn(min = 36.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(3.dp)
                .heightIn(min = 36.dp)
                .background(warnaBar)
        )
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = nama,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = warnaJudul,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (snippet.isBlank()) {
                    Icon(
                        imageVector = Icons.Filled.PhotoCamera,
                        contentDescription = null,
                        tint = warnaIsi,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    text = snippet.ifBlank { "Foto" },
                    fontSize = 11.5.sp,
                    color = warnaIsi,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Tampilan nisan pesan yang telah dihapus.
 */
@Composable
private fun NisanPesanPribadi(jam: String, diBubbleSendiri: Boolean) {
    val warna = TeksSekunder
    val warnaJam = TeksSekunder
    Row(verticalAlignment = Alignment.Bottom) {
        Icon(
            imageVector = Icons.Filled.Block,
            contentDescription = null,
            tint = warnaJam,
            modifier = Modifier.padding(end = 5.dp, bottom = 2.dp).size(14.dp)
        )
        Text(
            text = "Pesan ini telah dihapus",
            fontSize = 14.sp,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
            color = warna,
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(Modifier.width(6.dp))
        Text(jam, fontSize = 10.sp, color = warnaJam, modifier = Modifier.padding(top = 2.dp))
    }
}

/**
 * Keping reaksi emoji di bawah bubble pesan.
 */
@Composable
private fun KepingReaksi(
    reaksi: List<ReaksiPesan>,
    userId: String,
    modifier: Modifier = Modifier,
) {
    val perEmoji = remember(reaksi) { reaksi.groupBy { it.emoji } }
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        perEmoji.forEach { (emoji, daftar) ->
            val milikku = daftar.any { it.userId == userId }
            Row(
                Modifier
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (milikku) Color(0xFFD8E7FB) else Color(0xFFF0F0F2))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(emoji, fontSize = 12.sp)
                if (daftar.size > 1) {
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = daftar.size.toString(),
                        fontSize = 11.sp,
                        color = if (milikku) BiruIos else TeksSekunder,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Kartu preview balasan yang muncul di atas kolom input saat sedang membalas.
 */
@Composable
private fun KartuBalasComposer(
    target: PesanPribadi,
    namaBalasan: String,
    onTutup: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(WarnaIos.Latar)
            .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(3.dp)
                .height(36.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(BiruIos)
        )
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        ) {
            Text(
                text = "Membalas $namaBalasan",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = BiruIos,
                maxLines = 1
            )
            Text(
                text = if (target.body.isNotBlank()) target.body else "📷 Foto",
                fontSize = 12.sp,
                color = TeksSekunder,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onTutup) {
            Icon(Icons.Filled.Close, "Batal membalas", tint = TeksSekunder)
        }
    }
}

/**
 * Lembar popup pemilih seluruh emoji untuk reaksi.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LembarSemuaEmoji(onPilih: (String) -> Unit, onTutup: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onTutup,
        containerColor = WarnaIos.Latar,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(Modifier.navigationBarsPadding()) {
            Text(
                text = "Pilih reaksi",
                style = TipeIos.Utama,
                modifier = Modifier.padding(start = 20.dp, bottom = 8.dp)
            )
            PapanEmoji(
                onPilih = onPilih,
                onHapus = {}
            )
        }
    }
}

@Stable
private class SorotAnimasiState(
    val skala: Animatable<Float, *>,
    val glow: Animatable<Float, *>,
    val getarX: Animatable<Float, *>,
)

/**
 * Animasi sorot denyut taktil saat melompat ke pesan yang dikutip.
 */
@Composable
private fun denyutSorot(kunci: Any): SorotAnimasiState {
    val skala = remember(kunci) { Animatable(1f) }
    val glow = remember(kunci) { Animatable(0f) }
    val getarX = remember(kunci) { Animatable(0f) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(kunci) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)

        launch {
            skala.animateTo(
                targetValue = 1.065f,
                animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing)
            )
            skala.animateTo(
                targetValue = 1.03f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
            delay(1200)
            skala.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
            )
        }

        launch {
            val langkah = floatArrayOf(-6f, 6f, -4.5f, 4.5f, -2.5f, 2.5f, -1f, 1f, 0f)
            for (offset in langkah) {
                getarX.animateTo(
                    targetValue = offset,
                    animationSpec = tween(durationMillis = 30, easing = LinearEasing)
                )
            }
        }

        launch {
            glow.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing)
            )
            delay(1200)
            glow.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
            )
        }
    }

    return remember(kunci) { SorotAnimasiState(skala, glow, getarX) }
}

@Composable
private fun KomposerChatPribadi(
    sedangMerekam: Boolean,
    statusRekam: StatusRekam,
    perekam: PerekamSuara,
    sedangKompres: Boolean,
    mengirim: Boolean,
    onBukaLampiran: () -> Unit,
    onKirimTeks: (String) -> Unit,
    onKirimSuara: (java.io.File, Int, String) -> Unit,
    userId: String,
    onKirimStiker: (StikerKlipy) -> Unit,
    modifier: Modifier = Modifier,
) {
    var inputTeks by remember { mutableStateOf("") }
    var papanEmoji by remember { mutableStateOf(false) }
    val papanKetik = LocalSoftwareKeyboardController.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(WarnaIos.Latar)
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(WarnaIos.Pemisah))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            if (!sedangMerekam && !statusRekam.membuang) {
                IconButton(
                    onClick = {
                        papanEmoji = false
                        onBukaLampiran()
                    },
                    enabled = !sedangKompres
                ) {
                    if (sedangKompres) {
                        CircularProgressIndicator(color = BiruIos, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    } else {
                        Icon(
                            imageVector = Icons.Filled.AttachFile,
                            contentDescription = "Lampirkan foto",
                            tint = BiruIos,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.width(8.dp))
            }

            if (sedangMerekam || statusRekam.membuang) {
                PitaKomposerSuara(
                    perekam = perekam,
                    status = statusRekam,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 2.dp)
                )
            } else Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 2.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(WarnaIos.Kartu)
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 14.dp, top = 9.dp, bottom = 9.dp)
                    ) {
                        if (inputTeks.isEmpty()) {
                            Text("Ketik pesan…", fontSize = 15.sp, color = TeksSekunder)
                        }
                        BasicTextField(
                            value = inputTeks,
                            onValueChange = { inputTeks = it },
                            textStyle = TextStyle(fontSize = 15.sp, color = WarnaIos.Label),
                            cursorBrush = SolidColor(BiruIos),
                            maxLines = 5,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Tombol Emoji di dalam kolom ketik
                    Box(
                        modifier = Modifier
                            .padding(end = 4.dp, bottom = 3.dp)
                            .size(34.dp)
                            .clip(CircleShape)
                            .clickable {
                                if (papanEmoji) {
                                    papanEmoji = false
                                    papanKetik?.show()
                                } else {
                                    papanKetik?.hide()
                                    papanEmoji = true
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (papanEmoji) Icons.Filled.Keyboard else Icons.Filled.EmojiEmotions,
                            contentDescription = if (papanEmoji) "Tutup papan emoji" else "Buka papan emoji",
                            tint = if (papanEmoji) BiruIos else TeksSekunder,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            val bolehKirim = inputTeks.isNotBlank() && !mengirim
            // Kotak kosong = mikrofon, terisi = tombol kirim.
            if (inputTeks.isBlank()) {
                TombolMicChat(
                    perekam = perekam,
                    status = statusRekam,
                    onHasil = { hasil ->
                        papanEmoji = false
                        onKirimSuara(hasil.berkas, hasil.durasiMs, hasil.wave)
                    },
                    aktif = !mengirim,
                )
            } else {
                IconButton(
                    onClick = {
                        if (bolehKirim) {
                            val teks = inputTeks
                            inputTeks = ""
                            onKirimTeks(teks)
                        }
                    },
                    enabled = bolehKirim,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (bolehKirim) BiruIos else WarnaIos.LabelKetiga)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Kirim",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (papanEmoji) {
            PapanEmojiStiker(
                userId = userId,
                onPilihEmoji = { emoji -> inputTeks += emoji },
                onHapus = { inputTeks = hapusSatuKarakter(inputTeks) },
                onPilihStiker = onKirimStiker,
            )
        }
    }
}
