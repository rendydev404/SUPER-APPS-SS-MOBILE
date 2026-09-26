package com.sukashawarma.superapp.feature.chat.ui.area

import com.sukashawarma.superapp.feature.chat.data.FavoritStiker
import com.sukashawarma.superapp.feature.chat.ui.stiker.TataPesanStiker
import com.sukashawarma.superapp.core.ui.ios.LencanaIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.TombolBundarIos
import com.sukashawarma.superapp.core.ui.ios.TombolKapsulIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.ios.permukaanIos
import com.sukashawarma.superapp.core.ui.ios.tekanIos
import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sukashawarma.superapp.core.camera.KameraFotoSheet
import com.sukashawarma.superapp.core.ui.AvatarStaf
import com.sukashawarma.superapp.domain.model.Role
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.feature.chat.data.AreaChatRepository
import com.sukashawarma.superapp.feature.chat.data.AreaInfo
import com.sukashawarma.superapp.feature.chat.data.ChatMediaStorage
import com.sukashawarma.superapp.feature.chat.data.ChatRepository
import com.sukashawarma.superapp.feature.chat.data.ChatWallpaperPrefs
import com.sukashawarma.superapp.feature.chat.data.ItemAreaChat
import com.sukashawarma.superapp.feature.chat.data.PesanAreaChat
import com.sukashawarma.superapp.feature.chat.data.ReaksiPesan
import com.sukashawarma.superapp.feature.chat.data.Sebutan
import com.sukashawarma.superapp.feature.chat.data.WallpaperLatarChat
import com.sukashawarma.superapp.feature.chat.data.susunItemAreaChat
import com.sukashawarma.superapp.feature.chat.domain.Pengetik
import com.sukashawarma.superapp.feature.chat.domain.PosisiGrup
import com.sukashawarma.superapp.feature.chat.domain.indeksWarnaNama
import com.sukashawarma.superapp.feature.chat.domain.labelPengetik
import com.sukashawarma.superapp.feature.chat.domain.rentangSebutan
import com.sukashawarma.superapp.feature.chat.ui.BentukGelembung
import com.sukashawarma.superapp.feature.chat.ui.BubbleTigaTitik
import com.sukashawarma.superapp.feature.chat.ui.DaftarReaksiSheet
import com.sukashawarma.superapp.feature.chat.ui.EKOR
import com.sukashawarma.superapp.feature.chat.ui.FotoChat
import com.sukashawarma.superapp.feature.chat.ui.LembarPilihSumberFoto
import com.sukashawarma.superapp.feature.chat.ui.MenuPesanPopup
import com.sukashawarma.superapp.core.ui.PenampilFoto
import com.sukashawarma.superapp.feature.chat.ui.emoji.PapanEmoji
import com.sukashawarma.superapp.feature.chat.ui.suara.BubbleSuara
import com.sukashawarma.superapp.feature.chat.ui.suara.PemutarSuara
import com.sukashawarma.superapp.feature.chat.ui.suara.PitaKomposerSuara
import com.sukashawarma.superapp.feature.chat.ui.suara.TombolMicChat
import com.sukashawarma.superapp.feature.chat.ui.suara.rememberPerekamSuara
import com.sukashawarma.superapp.feature.chat.ui.suara.rememberStatusRekam
import kotlinx.coroutines.launch

/* ---------- Skema Warna Standar WhatsApp / iOS Chat ---------- */
private val BubbleSendiri = Color(0xFFF4F4F7)
private val BubbleLawan = Color(0xFFDEDEE4)
private val TeksUtama = WarnaIos.Label
private val TeksSekunder = WarnaIos.Abu
private val BiruIos = WarnaIos.Biru
private val Oranye = WarnaIos.Aksen
private val OranyeMenyala = Color(0xFFFF8A00)

private val WarnaNama = listOf(
    Color(0xFFE542A3), Color(0xFF1F7AEC), Color(0xFFE0651A), Color(0xFF00897B),
    Color(0xFF8E24AA), Color(0xFFD32F2F), Color(0xFF558B2F), Color(0xFFB8860B),
    Color(0xFF00838F), Color(0xFF5E35B1), Color(0xFFC2185B), Color(0xFF2E7D32),
    Color(0xFF6D4C41), Color(0xFF3949AB), Color(0xFFAD1457), Color(0xFF0277BD),
)

private fun warnaNama(senderId: String): Color =
    WarnaNama[indeksWarnaNama(senderId, WarnaNama.size)]

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayarChatArea(
    onMulaiChatPribadi: (id: String, nama: String, avatar: String?) -> Unit,
    modifier: Modifier = Modifier,
    vm: AreaChatViewModel = viewModel(),
    initialAreaId: String? = null,
    onAreaTerbuka: ((String) -> Unit)? = null,
) {
    val state by vm.state.collectAsState()
    val staff by AppSession.staff.collectAsState()
    val myId = staff?.id.orEmpty()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    LaunchedEffect(initialAreaId) {
        if (!initialAreaId.isNullOrBlank()) {
            val target = state.daftarArea.find { it.areaId == initialAreaId }
            if (target != null) {
                vm.gantiArea(target)
            }
        }
    }

    LaunchedEffect(state.areaAktif.areaId) {
        onAreaTerbuka?.invoke(state.areaAktif.areaId)
    }

    var sheetPilihArea by remember { mutableStateOf(false) }
    var sheetAnggotaArea by remember { mutableStateOf(false) }
    var lembarSumber by remember { mutableStateOf(false) }
    var kameraTerbuka by remember { mutableStateOf(false) }
    var sedangKompres by remember { mutableStateOf(false) }
    var fotoDitinjau by remember { mutableStateOf<String?>(null) }
    var menuPesan by remember { mutableStateOf<PesanAreaChat?>(null) }
    var konfirmasiHapus by remember { mutableStateOf<PesanAreaChat?>(null) }
    var sheetReaksi by remember { mutableStateOf<Pair<String, List<ReaksiPesan>>?>(null) }
    var pilihEmojiReaksi by remember { mutableStateOf<PesanAreaChat?>(null) }

    var sorotPesanId by remember { mutableStateOf<String?>(null) }
    var sorotKunci by remember { mutableIntStateOf(0) }

    val wallpaperEfektif = remember {
        val tersimpan = ChatWallpaperPrefs.getWallpaperTim(context)
        if (tersimpan == ChatWallpaperPrefs.ID_BAWAAN || tersimpan.isBlank() || tersimpan.equals("default", ignoreCase = true)) {
            "warm_ivory"
        } else {
            tersimpan
        }
    }
    val dimmingEfektif = remember { ChatWallpaperPrefs.getDimmingTim(context) }

    // Hentikan audio jika layar ditutup
    DisposableEffect(Unit) {
        onDispose { PemutarSuara.hentikan() }
    }

    // Daftar item chat disusun dengan reverseLayout (terbaru di indeks 0)
    val itemTampil = remember(state.pesan, myId) {
        susunItemAreaChat(state.pesan, myId).asReversed()
    }

    // Deteksi posisi scroll untuk tombol gulir ke bawah
    val diBawah by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex <= 1 && listState.firstVisibleItemScrollOffset < 240
        }
    }
    var pesanBaruBelumDilihat by remember { mutableIntStateOf(0) }
    var jumlahTerlihat by remember { mutableIntStateOf(0) }

    LaunchedEffect(state.pesan.size) {
        val total = state.pesan.size
        if (total > jumlahTerlihat) {
            if (diBawah) {
                listState.animateScrollToItem(0)
                pesanBaruBelumDilihat = 0
            } else {
                pesanBaruBelumDilihat += total - jumlahTerlihat
            }
        }
        jumlahTerlihat = total
    }

    LaunchedEffect(diBawah) {
        if (diBawah) pesanBaruBelumDilihat = 0
    }

    // Photo picker launcher
    val pickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                sedangKompres = true
                try {
                    val webp = FotoChat.kompres(context, uri)
                    if (webp != null) {
                        val remotePath = AreaChatRepository.unggahFoto(myId, webp)
                        vm.kirimPesan(imagePath = remotePath)
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Gagal mengunggah foto: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    sedangKompres = false
                }
            }
        }
    }

    val buramkan = menuPesan != null

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WarnaIos.Latar)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (buramkan) Modifier.blur(10.dp) else Modifier)
        ) {
            // 1. Header Glassmorphism Modern Grup Area
            HeaderAreaModern(
                area = state.areaAktif,
                jumlahAnggota = state.anggota.size,
                bolehGantiArea = state.bolehGantiArea,
                onKlikGantiArea = { sheetPilihArea = true },
                onKlikAnggota = { sheetAnggotaArea = true },
                isAreaManager = staff?.role == Role.AREA_MANAGER ||
                    staff?.roleRaw?.equals("area_manager", ignoreCase = true) == true,
                outletSaya = staff?.outletName
            )

            // 2. Daftar Pesan Chat Area (Reverse Layout & Ringan)
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                WallpaperLatarChat(wallpaperId = wallpaperEfektif, dimming = dimmingEfektif)

                if (state.memuat && state.pesan.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BiruIos, modifier = Modifier.size(36.dp))
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        reverseLayout = true,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        // Indikator mengetik di indeks 0 (paling bawah di reverseLayout)
                        item(key = "pengetik", contentType = "pengetik") {
                            BarisPengetikArea(state.pengetik)
                        }

                        // Pesan dan pemisah tanggal
                        items(
                            count = itemTampil.size,
                            key = { i ->
                                when (val itm = itemTampil[i]) {
                                    is ItemAreaChat.Bubble -> itm.pesan.id
                                    is ItemAreaChat.Pemisah -> "pemisah-${itm.label}"
                                }
                            },
                            contentType = { i ->
                                when (itemTampil[i]) {
                                    is ItemAreaChat.Bubble -> "bubble"
                                    is ItemAreaChat.Pemisah -> "pemisah"
                                }
                            }
                        ) { i ->
                            when (val itm = itemTampil[i]) {
                                is ItemAreaChat.Pemisah -> PemisahTanggalArea(itm.label)
                                is ItemAreaChat.Bubble -> {
                                    BarisBubbleArea(
                                        item = itm,
                                        reaksi = state.reaksi[itm.pesan.id].orEmpty(),
                                        userId = myId,
                                        onBalas = { vm.setBalas(it) },
                                        onTekanLama = { menuPesan = it },
                                        onKetukReaksi = { sheetReaksi = it.id to state.reaksi[it.id].orEmpty() },
                                        onKlikPengirim = { pesan ->
                                            if (pesan.senderId != myId) {
                                                onMulaiChatPribadi(pesan.senderId, pesan.senderName, pesan.senderAvatar)
                                            }
                                        },
                                        onKlikSebutan = { sebutan ->
                                            if (sebutan.id != myId) {
                                                onMulaiChatPribadi(sebutan.id, sebutan.nama, null)
                                            }
                                        },
                                        onKlikFoto = { fotoDitinjau = it.imagePath },
                                        disorot = itm.pesan.id == sorotPesanId,
                                        sorotKunci = if (itm.pesan.id == sorotPesanId) sorotKunci else 0,
                                        onLompatKe = { idAsal ->
                                            val idx = itemTampil.indexOfFirst {
                                                it is ItemAreaChat.Bubble && it.pesan.id == idAsal
                                            }
                                            if (idx >= 0) {
                                                scope.launch { listState.animateScrollToItem(idx) }
                                                sorotPesanId = idAsal
                                                sorotKunci++
                                            } else {
                                                Toast.makeText(context, "Pesan aslinya sudah tidak ada.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        // Banner info di paling awal percakapan (ujung atas)
                        item(key = "banner_info") {
                            BannerInfoArea(area = state.areaAktif)
                        }
                    }

                    // Tombol mengambang kembali ke bawah
                    if (!diBawah) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 16.dp, bottom = 12.dp)
                        ) {
                            TombolKeBawah(badge = pesanBaruBelumDilihat) {
                                scope.launch { listState.animateScrollToItem(0) }
                            }
                        }
                    }
                }
            }

            // 3. Autocomplete Sebutan (@)
            AnimatedVisibility(
                visible = state.sebutanAktif.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(WarnaIos.Kartu)
                        .drawBehind {
                            drawLine(WarnaIos.Pemisah, Offset(0f, 0f), Offset(size.width, 0f), 0.5.dp.toPx())
                        }
                        .padding(8.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        state.sebutanAktif.forEach { user ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(UkuranIos.SudutKontrol)
                                    .clickable { vm.sisipkanSebutan(user) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AvatarStaf(path = user.avatar, nama = user.namaTampil, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = user.namaTampil,
                                    style = TipeIos.Keterangan,
                                    fontWeight = FontWeight.Medium,
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "(${user.outlet ?: "Staf"})",
                                    style = TipeIos.Catatan,
                                )
                            }
                        }
                    }
                }
            }

            // 4. Pratinjau Balas Pesan (Kartu Balas Composer)
            state.balasPesan?.let { balas ->
                KartuBalasComposerArea(
                    target = balas,
                    onTutup = { vm.setBalas(null) }
                )
            }

            // 5. Kotak Input Pesan Modern
            BarInputArea(
                teks = state.inputTeks,
                sedangMengirim = state.sedangMengirim || sedangKompres,
                onTeksChange = { vm.setInputTeks(it) },
                onKirim = { vm.kirimPesan() },
                onBukaLampiran = { lembarSumber = true },
                onRekamSelesai = { audioPath, audioMs, waveform ->
                    vm.kirimPesan(audioPath = audioPath, audioMs = audioMs, audioWave = waveform)
                },
                myId = myId
            )
        }

        // Popup Menu Pesan Modern ala WhatsApp / iOS
        menuPesan?.let { pesan ->
            val itm = itemTampil.filterIsInstance<ItemAreaChat.Bubble>().firstOrNull { it.pesan.id == pesan.id }
            if (itm == null) {
                menuPesan = null
            } else {
                val bolehHapus = itm.milikSendiri ||
                    staff?.role == Role.AREA_MANAGER ||
                    staff?.roleRaw?.equals("area_manager", ignoreCase = true) == true ||
                    staff?.roleRaw?.equals("developer", ignoreCase = true) == true ||
                    staff?.roleRaw?.equals("superadmin", ignoreCase = true) == true

                MenuPesanPopup(
                    milikSendiri = itm.milikSendiri,
                    emojiTerpilih = state.reaksi[pesan.id]?.firstOrNull { it.userId == myId }?.emoji,
                    bolehHapus = bolehHapus,
                    bolehSunting = false,
                    bolehInfo = false,
                    // Teks pesan stiker hanya cadangan untuk app lama — tidak untuk disalin.
                    adaTeks = pesan.body.isNotBlank() && pesan.stickerUrl == null,
                    onEmoji = { emoji ->
                        vm.toggleReaksi(pesan.id, emoji)
                        menuPesan = null
                    },
                    onSemuaEmoji = {
                        pilihEmojiReaksi = pesan
                        menuPesan = null
                    },
                    onBalas = {
                        vm.setBalas(pesan)
                        menuPesan = null
                    },
                    onSunting = {},
                    onSalin = {
                        clipboard.setText(AnnotatedString(pesan.body))
                        Toast.makeText(context, "Pesan disalin", Toast.LENGTH_SHORT).show()
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
                        BubbleArea(
                            item = itm.copy(tampilkanIdentitas = false),
                            onLompatKe = {},
                        )
                    }
                )
            }
        }
    }

    // Dialog Konfirmasi Hapus Pesan
    konfirmasiHapus?.let { pesan ->
        AlertDialog(
            onDismissRequest = { konfirmasiHapus = null },
            title = { Text(if (pesan.senderId == myId) "Hapus pesan?" else "Hapus pesan ${pesan.senderName}?") },
            text = {
                Text("Pesan akan dihapus untuk semua orang di grup area ini.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.hapusPesan(pesan.id)
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

    // Modal Sheet Pilih Semua Emoji
    pilihEmojiReaksi?.let { pesan ->
        LembarSemuaEmoji(
            onPilih = { emoji ->
                vm.toggleReaksi(pesan.id, emoji)
                pilihEmojiReaksi = null
            },
            onTutup = { pilihEmojiReaksi = null }
        )
    }

    // Modal Sheet Pilih Area (Khusus RM, Dev, Owner, Admin)
    if (sheetPilihArea) {
        PilihAreaSheet(
            areaAktif = state.areaAktif,
            daftarArea = state.daftarArea,
            onPilihArea = { vm.gantiArea(it) },
            onDismiss = { sheetPilihArea = false }
        )
    }

    // Modal Sheet Anggota Area
    if (sheetAnggotaArea) {
        SheetAnggotaArea(
            area = state.areaAktif,
            anggota = state.anggota,
            memuat = state.memuatAnggota,
            onMulaiChatPribadi = onMulaiChatPribadi,
            onDismiss = { sheetAnggotaArea = false }
        )
    }

    // Lampiran foto
    if (lembarSumber) {
        LembarPilihSumberFoto(
            onGaleri = {
                lembarSumber = false
                pickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onKamera = {
                lembarSumber = false
                kameraTerbuka = true
            },
            onTutup = { lembarSumber = false }
        )
    }

    // Kamera foto
    if (kameraTerbuka) {
        Dialog(onDismissRequest = { kameraTerbuka = false }) {
            Box(Modifier.clip(RoundedCornerShape(18.dp)).background(Color.White).padding(14.dp)) {
                KameraFotoSheet(
                    onDiambil = { bitmap ->
                        kameraTerbuka = false
                        scope.launch {
                            sedangKompres = true
                            try {
                                val webp = FotoChat.kompres(bitmap)
                                if (webp != null) {
                                    val remotePath = AreaChatRepository.unggahFoto(myId, webp)
                                    vm.kirimPesan(imagePath = remotePath)
                                }
                            } catch (_: Exception) {
                                Toast.makeText(context, "Gagal mengunggah foto kamera", Toast.LENGTH_SHORT).show()
                            } finally {
                                sedangKompres = false
                            }
                        }
                    },
                    onBatal = { kameraTerbuka = false },
                    labelAmbil = "Ambil",
                )
            }
        }
    }

    // Fullscreen Penampil Foto
    fotoDitinjau?.let { fotoUrl ->
        PenampilFoto(
            url = ChatRepository.urlFoto(fotoUrl),
            judul = "Foto Area",
            onTutup = { fotoDitinjau = null }
        )
    }

    // Dialog Reaksi Sheet
    sheetReaksi?.let { (msgId, list) ->
        val avatarMap = remember(state.pesan) {
            state.pesan.associate { it.senderId to it.senderAvatar }
        }
        DaftarReaksiSheet(
            reaksi = list,
            avatarPerUser = avatarMap,
            userId = myId,
            onCabut = { vm.toggleReaksi(msgId, "") },
            onTutup = { sheetReaksi = null }
        )
    }
}

/**
 * Header Glassmorphism untuk Grup Area.
 */
@Composable
private fun HeaderAreaModern(
    area: AreaInfo,
    jumlahAnggota: Int,
    bolehGantiArea: Boolean,
    isAreaManager: Boolean,
    outletSaya: String?,
    onKlikGantiArea: () -> Unit,
    onKlikAnggota: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(WarnaIos.Kartu)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Identitas Area (Avatar + Judul + Subtitle) - seluruh area dapat diklik untuk membuka sheet anggota
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onKlikAnggota)
                    .padding(vertical = 4.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar Area
                val inisial = if (area.namaArea.isNotBlank()) {
                    area.namaArea.filter { it.isLetterOrDigit() }.take(2).uppercase()
                } else "AR"

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF1E3A8A), Color(0xFF2563EB))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = inisial,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = area.namaArea,
                            style = TipeIos.Utama,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        LencanaIos(
                            teks = if (isAreaManager) "👑 Anda (AM)" else "👑 AM",
                            nada = if (isAreaManager) NadaIos.INFO else NadaIos.PERINGATAN,
                            titik = false,
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    val amText = if (area.amName.isNotBlank()) " · AM: ${area.amName}" else ""
                    Text(
                        text = "${area.outlets.size} Cabang · $jumlahAnggota Anggota$amText",
                        style = TipeIos.Kecil,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Tombol Switch Area (Bagi role yang berhak)
            if (bolehGantiArea) {
                TombolKapsulIos(
                    teks = "Ganti",
                    onKlik = onKlikGantiArea,
                    ikon = IkonIos.SwapHoriz,
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Tombol Detail / Anggota
            TombolBundarIos(IkonIos.Groups, "Daftar Anggota", onKlikAnggota)
        }
        HorizontalDivider(thickness = 0.5.dp, color = WarnaIos.Pemisah)
    }
}

/**
 * Banner Informasi Area di paling atas chat.
 */
@Composable
private fun BannerInfoArea(area: AreaInfo) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .permukaanIos(UkuranIos.SudutGrup, WarnaIos.Kartu.copy(alpha = 0.95f))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = WarnaIos.Biru,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Ruang Obrolan Resmi ${area.namaArea}",
                        style = TipeIos.Catatan,
                        fontWeight = FontWeight.SemiBold,
                        color = WarnaIos.Label,
                        textAlign = TextAlign.Center
                    )
                }

                if (area.ringkasanOutlet.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Mencakup outlet: ${area.ringkasanOutlet}",
                        style = TipeIos.Kecil,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "🔒 Enkripsi Area · Terhapus otomatis setiap 03:00 WIB",
                    style = TipeIos.Kecil,
                    fontSize = 11.sp,
                    color = WarnaIos.Abu,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Pemisah Tanggal Cerdas.
 */
@Composable
private fun PemisahTanggalArea(label: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
        Text(
            label,
            style = TipeIos.Kecil,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clip(UkuranIos.SudutKapsul)
                .background(WarnaIos.Latar)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

/**
 * Satu baris bubble dengan swipe-to-reply, avatar di posisi akhir rentetan,
 * dan keping reaksi emoji.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BarisBubbleArea(
    item: ItemAreaChat.Bubble,
    reaksi: List<ReaksiPesan>,
    userId: String,
    onBalas: (PesanAreaChat) -> Unit,
    onTekanLama: (PesanAreaChat) -> Unit,
    onLompatKe: (String) -> Unit,
    onKetukReaksi: (PesanAreaChat) -> Unit,
    onKlikPengirim: (PesanAreaChat) -> Unit,
    onKlikSebutan: (Sebutan) -> Unit,
    onKlikFoto: (PesanAreaChat) -> Unit,
    disorot: Boolean,
    sorotKunci: Any = Unit,
) {
    val p = item.pesan
    val terhapus = p.deletedAtMs != null
    val haptic = LocalHapticFeedback.current

    var geser by remember { mutableStateOf(0f) }
    val geserAnim by animateFloatAsState(geser, label = "geserBalas")
    var sudahHaptic by remember { mutableStateOf(false) }
    val ambang = 150f

    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = if (item.posisi == PosisiGrup.TENGAH || item.posisi == PosisiGrup.AKHIR) 1.dp else 3.dp)
            .then(if (terhapus) Modifier else Modifier.pointerInput(p.id) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (geser >= ambang) onBalas(p)
                        geser = 0f
                        sudahHaptic = false
                    },
                    onDragCancel = { geser = 0f; sudahHaptic = false },
                ) { change, dragAmount ->
                    change.consume()
                    geser = (geser + dragAmount).coerceIn(0f, ambang * 1.3f)
                    if (geser >= ambang && !sudahHaptic) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        sudahHaptic = true
                    }
                }
            }),
    ) {
        // Ikon balas yang muncul di belakang saat diseret ke kanan
        Icon(
            Icons.AutoMirrored.Filled.Reply,
            null,
            tint = TeksSekunder,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp)
                .size(22.dp)
                .alpha((geserAnim / ambang).coerceIn(0f, 1f)),
        )

        Row(
            Modifier
                .fillMaxWidth()
                .offset { androidx.compose.ui.unit.IntOffset(geserAnim.toInt(), 0) },
            horizontalArrangement = if (item.milikSendiri) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Bottom,
        ) {
            if (!item.milikSendiri) {
                // Avatar dipasang di bubble TERAKHIR rentetan (sejajar ekor gelembung)
                Box(Modifier.width(34.dp), contentAlignment = Alignment.BottomCenter) {
                    if (item.posisi == PosisiGrup.TUNGGAL || item.posisi == PosisiGrup.AKHIR) {
                        AvatarStaf(
                            path = p.senderAvatar,
                            nama = p.senderName,
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .clickable { onKlikPengirim(p) },
                            ukuranHuruf = 13.sp,
                        )
                    }
                }
            }

            Column(horizontalAlignment = if (item.milikSendiri) Alignment.End else Alignment.Start) {
                BubbleArea(
                    item = item,
                    onLompatKe = onLompatKe,
                    onKlikFoto = { onKlikFoto(p) },
                    onKlikSebutan = onKlikSebutan,
                    onTekanLama = { if (!terhapus) onTekanLama(p) },
                    disorot = disorot,
                    sorotKunci = sorotKunci,
                    modifier = if (terhapus) Modifier else Modifier.pointerInput(p.id) {
                        detectTapGestures(
                            onLongPress = { onTekanLama(p) }
                        )
                    },
                )
                if (reaksi.isNotEmpty() && !terhapus) {
                    KepingReaksiArea(
                        reaksi = reaksi,
                        userId = userId,
                        modifier = Modifier
                            .offset(y = (-6).dp)
                            .padding(horizontal = 6.dp)
                            .clickable { onKetukReaksi(p) },
                    )
                }
            }
        }
    }
}

/**
 * Komponen Bubble Area yang dioptimalkan dengan BentukGelembung dan denyutSorot GPU.
 */
@Composable
private fun BubbleArea(
    item: ItemAreaChat.Bubble,
    onLompatKe: (String) -> Unit,
    onKlikFoto: () -> Unit = {},
    onKlikSebutan: (Sebutan) -> Unit = {},
    onTekanLama: () -> Unit = {},
    disorot: Boolean = false,
    sorotKunci: Any = Unit,
    modifier: Modifier = Modifier,
) {
    val p = item.pesan
    val warnaBubble = if (item.milikSendiri) BubbleSendiri else BubbleLawan
    val warnaTeks = TeksUtama

    val bentuk = BentukGelembung(item.milikSendiri, item.posisi)
    val denyut = if (disorot) denyutSorot(sorotKunci) else null

    // Kotak ketik area belum punya papan stiker, tetapi kolomnya sudah ada di tabel:
    // pesan stiker yang masuk (mis. dari klien versi berikutnya) tetap tampil benar,
    // bukan sebagai teks cadangan "Stiker". `return` aman di badan fungsi.
    val stiker = p.stickerUrl
    if (stiker != null && p.deletedAtMs == null) {
        TataPesanStiker(
            url = stiker,
            milikSendiri = item.milikSendiri,
            jam = item.jam,
            onTekanLama = onTekanLama,
            kepala = {
                if (item.tampilkanIdentitas) {
                    Text(
                        text = p.senderName,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = warnaNama(p.senderId),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                }
                if (p.replyToId != null || p.replyToSnippet != null) {
                    KutipanReplyArea(
                        nama = p.replyToName ?: "Pesan",
                        snippet = p.replyToSnippet.orEmpty(),
                        fotoPath = p.replyToImage,
                        onKlik = p.replyToId?.let { id -> { onLompatKe(id) } },
                        modifier = Modifier
                            .widthIn(max = 220.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(warnaBubble)
                            .padding(6.dp),
                    )
                }
            },
        )
        return
    }

    Column(
        modifier
            .widthIn(max = 290.dp + EKOR)
            .then(
                if (denyut == null) Modifier else Modifier.graphicsLayer {
                    val s = denyut.skala.value
                    scaleX = s
                    scaleY = s
                    translationX = denyut.getarX.value
                }
            )
            .clip(bentuk)
            .background(warnaBubble)
            .then(
                if (denyut == null) Modifier else Modifier.drawWithContent {
                    drawContent()
                    val maju = denyut.glow.value
                    if (maju <= 0f) return@drawWithContent
                    val garis = bentuk.createOutline(size, layoutDirection, this)

                    // 1. Aura cahaya bloom glow
                    drawOutline(
                        garis,
                        OranyeMenyala,
                        alpha = 0.32f * maju,
                        style = Stroke(width = 8.dp.toPx()),
                    )
                    // 2. Kilau hangat dalam isi bubble
                    drawOutline(
                        garis,
                        OranyeMenyala,
                        alpha = 0.16f * maju,
                    )
                    // 3. Cincin tegas batas luar
                    drawOutline(
                        garis,
                        Oranye,
                        alpha = maju,
                        style = Stroke(width = 2.8.dp.toPx()),
                    )
                }
            )
            .padding(
                start = if (item.milikSendiri) 12.dp else 12.dp + EKOR,
                end = if (item.milikSendiri) 12.dp + EKOR else 12.dp,
                top = 6.dp,
                bottom = 6.dp,
            ),
    ) {
        // Identitas pengirim pada bubble pertama grup
        if (item.tampilkanIdentitas) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 3.dp),
            ) {
                Text(
                    text = p.senderName,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = warnaNama(p.senderId),
                )
                Spacer(Modifier.width(5.dp))

                // Role & Outlet badges
                if (p.isAreaManager) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFFEF3C7))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "👑 AM",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB45309)
                        )
                    }
                } else if (p.isLeader) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFE0F2FE))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "⭐ " + (p.outletName ?: "Leader"),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0369A1)
                        )
                    }
                } else {
                    p.outletName?.let { ot ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFF3F4F6))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = ot,
                                fontSize = 9.sp,
                                color = Color(0xFF4B5563)
                            )
                        }
                    }
                }
            }
        }

        // Jika terhapus (Nisan)
        if (p.deletedAtMs != null) {
            NisanPesanArea(
                jam = item.jam,
                olehPengelola = p.deletedByName,
                diBubbleSendiri = item.milikSendiri,
            )
        } else {
            // Kutipan balasan jika ada (clickable untuk melompat ke pesan asli)
            if (p.replyToId != null || p.replyToSnippet != null) {
                KutipanReplyArea(
                    nama = p.replyToName ?: "Pesan",
                    snippet = p.replyToSnippet.orEmpty(),
                    fotoPath = p.replyToImage,
                    onKlik = p.replyToId?.let { id -> { onLompatKe(id) } },
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }

            // Pesan Suara
            if (p.audioPath != null) {
                BubbleSuara(
                    audioPath = p.audioPath,
                    audioMs = p.audioMs,
                    audioWave = p.audioWave,
                    milikSendiri = item.milikSendiri,
                    onMulaiPutar = {},
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }

            // Pesan Foto
            if (p.imagePath != null) {
                val context = LocalContext.current
                val request = remember(p.imagePath) {
                    ImageRequest.Builder(context)
                        .data(ChatRepository.urlFoto(p.imagePath))
                        .size(720, 720)
                        .crossfade(true)
                        .build()
                }
                AsyncImage(
                    model = request,
                    imageLoader = ChatMediaStorage.imageLoader(context),
                    contentDescription = "Foto",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .padding(bottom = if (p.body.isNotBlank()) 4.dp else 0.dp)
                        .width(240.dp)
                        .heightIn(min = 140.dp, max = 320.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x11000000))
                        .clickable(onClick = onKlikFoto),
                )
            }

            // Teks Pesan & Jam
            Row(
                modifier = if (p.body.isBlank()) Modifier.align(Alignment.End) else Modifier,
                verticalAlignment = Alignment.Bottom,
            ) {
                if (p.body.isNotBlank()) {
                    TeksBubbleArea(
                        body = p.body,
                        sebutan = p.mentions,
                        warnaTeks = warnaTeks,
                        diBubbleSendiri = item.milikSendiri,
                        onKlikSebutan = onKlikSebutan,
                        onTekanLama = onTekanLama,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(6.dp))
                }
                if (p.editedAtMs != null) {
                    Text(
                        "diedit",
                        fontSize = 10.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = TeksSekunder,
                        modifier = Modifier.padding(end = 5.dp, top = 2.dp),
                    )
                }
                Text(
                    item.jam,
                    fontSize = 10.5.sp,
                    color = TeksSekunder,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

/**
 * Tanda nisan untuk pesan yang telah dihapus.
 */
@Composable
private fun NisanPesanArea(jam: String, olehPengelola: String?, diBubbleSendiri: Boolean) {
    Row(verticalAlignment = Alignment.Bottom) {
        Icon(
            Icons.Filled.Block,
            null,
            tint = TeksSekunder,
            modifier = Modifier.padding(end = 5.dp, bottom = 2.dp).size(14.dp),
        )
        Text(
            if (olehPengelola != null) "Pesan ini dihapus oleh $olehPengelola"
            else "Pesan ini telah dihapus",
            fontSize = 14.sp,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
            color = TeksSekunder,
            modifier = Modifier.weight(1f, fill = false),
        )
        Spacer(Modifier.width(6.dp))
        Text(jam, fontSize = 10.5.sp, color = TeksSekunder, modifier = Modifier.padding(top = 2.dp))
    }
}

/**
 * Teks bubble interaktif dengan sorotan @sebutan.
 */
@Composable
private fun TeksBubbleArea(
    body: String,
    sebutan: List<Sebutan>,
    warnaTeks: Color,
    diBubbleSendiri: Boolean,
    onKlikSebutan: (Sebutan) -> Unit,
    onTekanLama: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rentang = remember(body, sebutan) { rentangSebutan(body, sebutan) }
    if (rentang.isEmpty()) {
        Text(
            text = body,
            fontSize = 15.sp,
            color = warnaTeks,
            lineHeight = 20.sp,
            modifier = modifier.pointerInput(Unit) {
                detectTapGestures(onLongPress = { onTekanLama() })
            }
        )
        return
    }

    val teks = remember(body, rentang, diBubbleSendiri) {
        buildAnnotatedString {
            append(body)
            rentang.forEach { (r, _) ->
                addStyle(
                    SpanStyle(
                        color = BiruIos,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    r.first,
                    r.last + 1,
                )
            }
        }
    }

    var tataLetak by remember { mutableStateOf<TextLayoutResult?>(null) }
    Text(
        teks,
        fontSize = 15.sp,
        color = warnaTeks,
        lineHeight = 20.sp,
        onTextLayout = { tataLetak = it },
        modifier = modifier.pointerInput(rentang) {
            detectTapGestures(
                onLongPress = { onTekanLama() },
                onTap = { posisi ->
                    val letak = tataLetak ?: return@detectTapGestures
                    val offset = letak.getOffsetForPosition(posisi)
                    rentang.firstOrNull { offset in it.first }?.let { onKlikSebutan(it.second) }
                },
            )
        },
    )
}

/**
 * Kartu kutipan reply di dalam bubble.
 */
@Composable
private fun KutipanReplyArea(
    nama: String,
    snippet: String,
    fotoPath: String?,
    modifier: Modifier = Modifier,
    onKlik: (() -> Unit)? = null,
) {
    Row(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .then(if (onKlik != null) Modifier.clickable(onClick = onKlik) else Modifier)
            .background(Color(0x0F000000))
            .heightIn(min = 38.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(3.dp).heightIn(min = 38.dp).background(BiruIos))
        Column(Modifier.weight(1f, fill = false).padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text(
                nama,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = BiruIos,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (fotoPath != null) {
                    Icon(
                        Icons.Filled.PhotoCamera,
                        null,
                        tint = TeksSekunder,
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    snippet.ifBlank { if (fotoPath != null) "Foto" else "" },
                    fontSize = 11.5.sp,
                    color = TeksSekunder,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (fotoPath != null) {
            AsyncImage(
                model = ChatRepository.urlFoto(fotoPath),
                imageLoader = ChatMediaStorage.imageLoader(LocalContext.current),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(38.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0x1A000000)),
            )
        }
    }
}

/**
 * Keping reaksi emoji di bawah bubble.
 */
@Composable
private fun KepingReaksiArea(
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
                    .border(
                        width = if (milikku) 1.dp else 0.5.dp,
                        color = if (milikku) BiruIos else Color(0xFFE5E7EB),
                        shape = RoundedCornerShape(11.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(emoji, fontSize = 12.sp)
                if (daftar.size > 1) {
                    Spacer(Modifier.width(3.dp))
                    Text(
                        daftar.size.toString(),
                        fontSize = 11.sp,
                        color = if (milikku) BiruIos else TeksSekunder,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

/**
 * Baris Pengetik Area di dalam LazyColumn (reverseLayout indeks 0).
 */
@Composable
private fun BarisPengetikArea(pengetik: List<Pengetik>) {
    var terakhirTampak by remember { mutableStateOf(pengetik) }
    if (pengetik.isNotEmpty()) terakhirTampak = pengetik

    AnimatedVisibility(
        visible = pengetik.isNotEmpty(),
        enter = fadeIn(tween(180)) + expandVertically(tween(220)),
        exit = fadeOut(tween(140)) + shrinkVertically(tween(200)),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TumpukanWajahPengetikArea(terakhirTampak)
            Spacer(Modifier.width(8.dp))
            BubbleTigaTitik()
            Spacer(Modifier.width(8.dp))
            Text(
                labelPengetik(terakhirTampak).orEmpty(),
                fontSize = 11.5.sp,
                color = TeksSekunder,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TumpukanWajahPengetikArea(orang: List<Pengetik>) {
    val tampil = orang.take(3)
    val sisa = orang.size - tampil.size
    val diameter = 22.dp
    val tindih = 7.dp

    Row(horizontalArrangement = Arrangement.spacedBy(-tindih)) {
        tampil.forEach { p ->
            Box(
                Modifier
                    .size(diameter)
                    .clip(CircleShape)
                    .border(1.5.dp, Color.White, CircleShape),
            ) {
                AvatarStaf(
                    path = p.avatar,
                    nama = p.nama,
                    modifier = Modifier.fillMaxSize(),
                    ukuranHuruf = 9.sp,
                )
            }
        }
        if (sisa > 0) {
            Box(
                Modifier
                    .size(diameter)
                    .clip(CircleShape)
                    .background(Color(0xFF8E8E93))
                    .border(1.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("+$sisa", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Banner komposer saat sedang membalas pesan.
 */
@Composable
private fun KartuBalasComposerArea(target: PesanAreaChat, onTutup: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(WarnaIos.Kartu)
            .padding(start = 12.dp, end = 4.dp, top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(3.dp).height(36.dp).clip(RoundedCornerShape(2.dp)).background(BiruIos))
        Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
            Text("Membalas ${target.senderName}", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = BiruIos, maxLines = 1)
            Text(
                target.body.ifBlank {
                    if (target.imagePath != null) "Foto"
                    else if (target.audioPath != null) "Pesan suara"
                    else ""
                },
                fontSize = 12.sp, color = TeksSekunder, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onTutup) { Icon(Icons.Filled.Close, "Batal membalas", tint = TeksSekunder) }
    }
}

/**
 * Tombol mengambang kembali ke paling bawah (pesan terbaru).
 */
@Composable
private fun TombolKeBawah(badge: Int, onClick: () -> Unit) {
    Box {
        Box(
            Modifier
                .size(40.dp)
                .permukaanIos(CircleShape)
                .tekanIos(onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.KeyboardArrowDown, "Ke pesan terbaru", tint = BiruIos)
        }
        if (badge > 0) {
            Text(
                if (badge > 99) "99+" else badge.toString(),
                fontSize = 10.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .clip(CircleShape)
                    .background(BiruIos)
                    .padding(horizontal = 5.dp, vertical = 1.dp),
            )
        }
    }
}

/**
 * Bottom sheet untuk memilih seluruh emoji sebagai reaksi.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LembarSemuaEmoji(onPilih: (String) -> Unit, onTutup: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onTutup,
        containerColor = WarnaIos.Latar,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Column(Modifier.navigationBarsPadding()) {
            Text(
                "Pilih reaksi",
                style = TipeIos.Utama,
                modifier = Modifier.padding(start = 20.dp, bottom = 8.dp),
            )
            PapanEmoji(
                onPilih = onPilih,
                onHapus = {},
            )
        }
    }
}

/**
 * Baris Input Pesan Chat Area.
 */
@Composable
private fun BarInputArea(
    teks: String,
    sedangMengirim: Boolean,
    onTeksChange: (String) -> Unit,
    onKirim: () -> Unit,
    onBukaLampiran: () -> Unit,
    onRekamSelesai: (audioPath: String, audioMs: Int, waveform: String) -> Unit,
    myId: String,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val perekam = rememberPerekamSuara()
    val statusRekam = rememberStatusRekam()

    Surface(
        color = WarnaIos.Kartu,
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (perekam.sedangMerekam || statusRekam.membuang) {
                PitaKomposerSuara(
                    perekam = perekam,
                    status = statusRekam,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 2.dp)
                )
            } else {
                // Tombol Lampiran
                IconButton(
                    onClick = onBukaLampiran,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = "Lampiran",
                        tint = WarnaIos.Biru
                    )
                }

                // Input Teks
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(WarnaIos.Isian)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    if (teks.isEmpty()) {
                        Text(
                            text = "Ketik pesan ke Tim Area...",
                            style = TipeIos.Keterangan,
                            color = WarnaIos.Abu
                        )
                    }
                    BasicTextField(
                        value = teks,
                        onValueChange = onTeksChange,
                        textStyle = TipeIos.Keterangan,
                        cursorBrush = SolidColor(WarnaIos.Biru),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Tombol Kirim / Perekam Suara
            if (teks.isNotBlank() || statusRekam.terkunci) {
                IconButton(
                    onClick = {
                        if (statusRekam.terkunci) {
                            perekam.selesai()?.let { hasil ->
                                scope.launch {
                                    try {
                                        val bytes = hasil.berkas.readBytes()
                                        val remotePath = AreaChatRepository.unggahSuara(myId, bytes)
                                        onRekamSelesai(remotePath, hasil.durasiMs, hasil.wave)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "Gagal mengunggah rekaman suara", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            statusRekam.reset()
                        } else {
                            onKirim()
                        }
                    },
                    enabled = !sedangMengirim,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(BiruIos)
                ) {
                    if (sedangMengirim) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Kirim",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else {
                // Tombol Voice Note via TombolMicChat
                TombolMicChat(
                    perekam = perekam,
                    status = statusRekam,
                    onHasil = { hasil ->
                        scope.launch {
                            try {
                                val bytes = hasil.berkas.readBytes()
                                val remotePath = AreaChatRepository.unggahSuara(myId, bytes)
                                onRekamSelesai(remotePath, hasil.durasiMs, hasil.wave)
                            } catch (_: Exception) {
                                Toast.makeText(context, "Gagal mengunggah rekaman suara", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
        }
    }
}

/**
 * State pemegang animasi denyut sorot pada tingkat GPU.
 */
@Stable
private class SorotAnimasiState(
    val skala: Animatable<Float, *>,
    val glow: Animatable<Float, *>,
    val getarX: Animatable<Float, *>,
)

/**
 * Animasi sorotan interaktif saat melompat ke pesan yang dikutip (reply):
 * 1. Membesar responsif (1.0f -> 1.065f -> 1.03f), bertahan sejenak, lalu kembali ke 1.0f normal.
 * 2. Gerak getar taktil (micro-vibration jiggle ke kiri-kanan secara cepat + haptic feedback).
 * 3. Menyala (glow & bloom aura hangat oranye merek di sekeliling dan dalam gelembung).
 *
 * Seluruh nilai animasi dibaca murni di dalam graphicsLayer dan drawWithContent
 * pada fase render GPU, sehingga 100% bebas dari recomposition dan bebas frame drop.
 */
@Composable
private fun denyutSorot(kunci: Any): SorotAnimasiState {
    val skala = remember(kunci) { Animatable(1f) }
    val glow = remember(kunci) { Animatable(0f) }
    val getarX = remember(kunci) { Animatable(0f) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(kunci) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)

        // 1. Membesar responsif (overshoot pop)
        launch {
            skala.animateTo(
                targetValue = 1.065f,
                animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
            )
            skala.animateTo(
                targetValue = 1.03f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            )
            kotlinx.coroutines.delay(1_200)
            skala.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
            )
        }

        // 2. Gerak getar taktil (micro-vibration jiggle)
        launch {
            val langkah = floatArrayOf(-6f, 6f, -4.5f, 4.5f, -2.5f, 2.5f, -1f, 1f, 0f)
            for (offset in langkah) {
                getarX.animateTo(
                    targetValue = offset,
                    animationSpec = tween(durationMillis = 30, easing = LinearEasing),
                )
            }
        }

        // 3. Efek Menyala (Glow & Bloom Aura)
        launch {
            glow.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
            )
            kotlinx.coroutines.delay(1_200)
            glow.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
            )
        }
    }

    return remember(kunci) { SorotAnimasiState(skala, glow, getarX) }
}
