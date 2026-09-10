package com.sukashawarma.superapp.feature.chat.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.sukashawarma.superapp.core.camera.KameraFotoSheet
import com.sukashawarma.superapp.core.ui.AvatarStaf
import com.sukashawarma.superapp.core.ui.AvatarStorage
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.feature.chat.data.ChatRepository
import com.sukashawarma.superapp.feature.chat.data.PesanChat
import com.sukashawarma.superapp.feature.chat.domain.ItemChat
import com.sukashawarma.superapp.feature.chat.domain.PosisiGrup
import com.sukashawarma.superapp.feature.chat.domain.indeksWarnaNama
import com.sukashawarma.superapp.feature.chat.domain.labelPengetik
import com.sukashawarma.superapp.feature.chat.domain.snippetPesan
import com.sukashawarma.superapp.feature.chat.domain.susunItemChat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/* ---------- Palet estetika iOS (iMessage) ---------- */

private val LatarChat = Color(0xFFFFFFFF)
private val LatarBar = Color(0xFFF7F7F8)
private val GarisTipis = Color(0x2E3C3C43)
private val BiruIos = Color(0xFF007AFF)
private val BubbleSendiri = BiruIos
private val BubbleLawan = Color(0xFFE9E9EB)
private val TeksUtama = Color(0xFF000000)
private val TeksSekunder = Color(0xFF8E8E93)
private val LatarBanner = Color(0xFFFEF7DC)
private val TeksBanner = Color(0xFF6B5D2E)
private val Merah = Color(0xFFFF3B30)

/** Warna nama pengirim di grup — satu warna tetap per orang, seperti WA. */
private val WarnaNama = listOf(
    Color(0xFFE542A3), Color(0xFF1F7AEC), Color(0xFFFA6533), Color(0xFF009688),
    Color(0xFF9C27B0), Color(0xFFD32F2F), Color(0xFF7CB342), Color(0xFFFF9800),
)

private fun warnaNama(senderId: String): Color = WarnaNama[indeksWarnaNama(senderId, WarnaNama.size)]

private val formatJam = SimpleDateFormat("HH:mm", Locale("id", "ID"))

/* ---------- Layar ---------- */

@Composable
fun ChatScreen(
    onBack: () -> Unit,
    viewModel: ChatViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val staff by AppSession.staff.collectAsState()
    val userId = staff?.id.orEmpty()

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Alur foto ala WhatsApp: lampiran -> pilih sumber -> pratinjau penuh -> kirim.
    var lembarSumber by remember { mutableStateOf(false) }
    var kameraTerbuka by remember { mutableStateOf(false) }
    var fotoPratinjau by remember { mutableStateOf<FotoTerpilih?>(null) }
    var sedangKompres by remember { mutableStateOf(false) }

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

    // Daftar item (bubble + pemisah), terbaru DULU karena LazyColumn reverseLayout.
    val itemTampil = remember(state.pesan, userId) {
        susunItemChat(state.pesan, userId, System.currentTimeMillis()).reversed()
    }

    // Auto-ikut ke bawah saat pesan baru datang dan pengguna memang sedang di bawah;
    // kalau sedang membaca ke atas, jangan menyeret paksa — cukup badge pesan baru.
    var jumlahTerlihat by remember { mutableIntStateOf(0) }
    var pesanBaruBelumDilihat by remember { mutableIntStateOf(0) }
    val diBawah by remember {
        androidx.compose.runtime.derivedStateOf {
            listState.firstVisibleItemIndex <= 1 && listState.firstVisibleItemScrollOffset < 240
        }
    }
    LaunchedEffect(state.pesan.size, state.tertunda.size) {
        val total = state.pesan.size + state.tertunda.size
        if (total > jumlahTerlihat) {
            if (diBawah || state.tertunda.isNotEmpty()) {
                listState.animateScrollToItem(0)
                pesanBaruBelumDilihat = 0
            } else {
                pesanBaruBelumDilihat += total - jumlahTerlihat
            }
        }
        jumlahTerlihat = total
    }
    LaunchedEffect(diBawah) { if (diBawah) pesanBaruBelumDilihat = 0 }

    BackHandler(onBack = onBack)

    Box(Modifier.fillMaxSize().background(LatarChat)) {
    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding(),
    ) {
        HeaderChat(
            subtitle = labelPengetik(state.namaPengetik)
                ?: "Pesan hilang otomatis setelah 24 jam",
            subtitleAktif = state.namaPengetik.isNotEmpty(),
            onBack = onBack,
        )

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.memuat -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BiruIos, strokeWidth = 3.dp, modifier = Modifier.size(30.dp))
                }
                state.galat != null && state.pesan.isEmpty() -> KeadaanGalat(state.galat!!) { viewModel.muatUlang(awal = true) }
                else -> LazyColumn(
                    state = listState,
                    reverseLayout = true,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                ) {
                    // Indikator mengetik hidup DI DALAM daftar (reverseLayout:
                    // indeks 0 = paling bawah), bukan sebagai baris terpisah di
                    // atas komposer. Itu membuatnya ikut mengalir bersama pesan
                    // seperti WhatsApp, dan berhenti mendorong komposer naik-turun
                    // setiap kali ada yang mulai/berhenti mengetik.
                    //
                    // Item-nya SELALU ada (isinya yang beranimasi masuk/keluar);
                    // menyisipkan-menghapus item di indeks 0 akan membuat daftar
                    // tersentak setiap sinyal typing datang.
                    item(key = "pengetik") {
                        BarisPengetik(state.namaPengetik)
                    }

                    // Kiriman tertunda selalu paling baru, jadi ditaruh paling awal.
                    items(state.tertunda.asReversed().size, key = { i -> state.tertunda.asReversed()[i].kunci }) { i ->
                        BubbleTertunda(
                            kiriman = state.tertunda.asReversed()[i],
                            onUlangi = viewModel::ulangi,
                            onBatal = viewModel::batalkanKiriman,
                        )
                    }
                    items(
                        itemTampil.size,
                        key = { i ->
                            when (val item = itemTampil[i]) {
                                is ItemChat.Bubble -> item.pesan.id
                                is ItemChat.Pemisah -> "pemisah-${item.label}"
                            }
                        },
                    ) { i ->
                        when (val item = itemTampil[i]) {
                            is ItemChat.Pemisah -> PemisahTanggal(item.label)
                            is ItemChat.Bubble -> BarisBubble(
                                item = item,
                                onBalas = { viewModel.setBalas(it) },
                                onHapus = { viewModel.hapus(it) },
                                onLompatKe = { idAsal ->
                                    val idx = itemTampil.indexOfFirst { it is ItemChat.Bubble && it.pesan.id == idAsal }
                                    if (idx >= 0) scope.launch { listState.animateScrollToItem(idx) }
                                },
                            )
                        }
                    }
                    if (itemTampil.isEmpty() && state.tertunda.isEmpty()) {
                        item { KeadaanKosong() }
                    }
                    item { BannerSementara() }
                }
            }

            // Tombol lompat ke bawah + badge pesan baru, gaya iOS.
            androidx.compose.animation.AnimatedVisibility(
                visible = !diBawah && !state.memuat,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
                modifier = Modifier.align(Alignment.BottomEnd).padding(14.dp),
            ) {
                TombolKeBawah(pesanBaruBelumDilihat) {
                    scope.launch { listState.animateScrollToItem(0) }
                }
            }
        }

        state.balasTarget?.let { target ->
            KartuBalasComposer(target) { viewModel.setBalas(null) }
        }

        KomposerChat(
            sedangKompres = sedangKompres,
            onKetik = viewModel::ketikan,
            onKirim = viewModel::kirimTeks,
            onLampiran = { lembarSumber = true },
        )
    }

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
            onTutup = { lembarSumber = false },
        )
    }

    if (kameraTerbuka) {
        Dialog(onDismissRequest = { kameraTerbuka = false }) {
            Box(Modifier.clip(RoundedCornerShape(18.dp)).background(Color.White).padding(14.dp)) {
                KameraFotoSheet(
                    onDiambil = { bitmap ->
                        kameraTerbuka = false
                        siapkanFoto { FotoChat.kompres(bitmap) }
                    },
                    onBatal = { kameraTerbuka = false },
                    labelAmbil = "Ambil",
                )
            }
        }
    }

    // Pratinjau penuh: lapisan paling atas, menutupi chat seperti WhatsApp.
    fotoPratinjau?.let { foto ->
        PratinjauKirimFoto(
            foto = foto,
            namaBalasan = state.balasTarget?.senderName,
            onKirim = { keterangan ->
                viewModel.kirimFoto(foto.webp, keterangan)
                fotoPratinjau = null
            },
            onBatal = { fotoPratinjau = null },
        )
    }
    }
}

/**
 * Baris "sedang mengetik" di dasar daftar pesan: bubble tiga titik plus namanya,
 * muncul dan hilang dengan animasi halus.
 */
@Composable
private fun BarisPengetik(nama: List<String>) {
    androidx.compose.animation.AnimatedVisibility(
        visible = nama.isNotEmpty(),
        enter = fadeIn(tween(180)) + expandVertically(tween(220)),
        exit = fadeOut(tween(140)) + shrinkVertically(tween(200)),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 36.dp, top = 3.dp, bottom = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BubbleTigaTitik()
            Spacer(Modifier.width(8.dp))
            Text(
                labelPengetik(nama).orEmpty(),
                fontSize = 11.5.sp,
                color = TeksSekunder,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/* ---------- Header ---------- */

@Composable
private fun HeaderChat(subtitle: String, subtitleAktif: Boolean, onBack: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(LatarBar).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBackIos, "Kembali", tint = BiruIos)
            }
            Box(
                Modifier.size(38.dp).clip(CircleShape).background(Color(0xFFD8E7FB)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Groups, null, tint = BiruIos, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Chat Tim", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TeksUtama)
                Text(
                    subtitle,
                    fontSize = 12.sp,
                    color = if (subtitleAktif) BiruIos else TeksSekunder,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(GarisTipis))
    }
}

/* ---------- Baris bubble + swipe-untuk-balas ---------- */

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BarisBubble(
    item: ItemChat.Bubble,
    onBalas: (PesanChat) -> Unit,
    onHapus: (PesanChat) -> Unit,
    onLompatKe: (String) -> Unit,
) {
    val p = item.pesan
    val haptic = LocalHapticFeedback.current
    val clipboard = LocalClipboardManager.current

    var geser by remember { mutableStateOf(0f) }
    val geserAnim by animateFloatAsState(geser, label = "geserBalas")
    var sudahHaptic by remember { mutableStateOf(false) }
    val ambang = 150f

    var menuTerbuka by remember { mutableStateOf(false) }
    var konfirmasiHapus by remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = if (item.posisi == PosisiGrup.TENGAH || item.posisi == PosisiGrup.AKHIR) 1.dp else 3.dp)
            .pointerInput(p.id) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (geser >= ambang) onBalas(p)
                        geser = 0f
                        sudahHaptic = false
                    },
                    onDragCancel = { geser = 0f; sudahHaptic = false },
                ) { change, dragAmount ->
                    change.consume()
                    // Hanya seret ke kanan — gestur balas WhatsApp.
                    geser = (geser + dragAmount).coerceIn(0f, ambang * 1.3f)
                    if (geser >= ambang && !sudahHaptic) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        sudahHaptic = true
                    }
                }
            },
    ) {
        // Ikon balas yang muncul di belakang selama digeser.
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
                // Avatar hanya di bubble pertama grup; sisanya diberi ruang kosong
                // selebar avatar supaya bubble-nya tetap sejajar.
                Box(Modifier.width(36.dp), contentAlignment = Alignment.BottomCenter) {
                    if (item.tampilkanIdentitas) {
                        AvatarStaf(
                            path = p.senderAvatar,
                            nama = p.senderName,
                            modifier = Modifier.size(30.dp),
                            ukuranHuruf = 13.sp,
                        )
                    }
                }
                Spacer(Modifier.width(6.dp))
            }

            Box {
                Bubble(
                    item = item,
                    onLompatKe = onLompatKe,
                    modifier = Modifier.combinedClickable(
                        onClick = {},
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuTerbuka = true
                        },
                    ),
                )
                DropdownMenu(expanded = menuTerbuka, onDismissRequest = { menuTerbuka = false }) {
                    DropdownMenuItem(
                        text = { Text("Balas") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Reply, null) },
                        onClick = { menuTerbuka = false; onBalas(p) },
                    )
                    if (p.body.isNotBlank()) {
                        DropdownMenuItem(
                            text = { Text("Salin") },
                            leadingIcon = { Icon(Icons.Filled.ContentCopy, null) },
                            onClick = {
                                menuTerbuka = false
                                clipboard.setText(AnnotatedString(p.body))
                            },
                        )
                    }
                    if (item.milikSendiri) {
                        DropdownMenuItem(
                            text = { Text("Hapus", color = Merah) },
                            leadingIcon = { Icon(Icons.Filled.Delete, null, tint = Merah) },
                            onClick = { menuTerbuka = false; konfirmasiHapus = true },
                        )
                    }
                }
            }
        }
    }

    if (konfirmasiHapus) {
        AlertDialog(
            onDismissRequest = { konfirmasiHapus = false },
            title = { Text("Hapus pesan?") },
            text = { Text("Pesan dihapus untuk semua orang di ruang ini.") },
            confirmButton = {
                TextButton(onClick = { konfirmasiHapus = false; onHapus(p) }) { Text("Hapus", color = Merah) }
            },
            dismissButton = {
                TextButton(onClick = { konfirmasiHapus = false }) { Text("Batal") }
            },
        )
    }
}

/** Bentuk sudut asimetris ala iMessage: sisi "ekor" grup lebih siku. */
private fun bentukBubble(milikSendiri: Boolean, posisi: PosisiGrup): RoundedCornerShape {
    val besar = 18.dp
    val kecil = 5.dp
    val atasNyambung = posisi == PosisiGrup.TENGAH || posisi == PosisiGrup.AKHIR
    val bawahNyambung = posisi == PosisiGrup.AWAL || posisi == PosisiGrup.TENGAH
    return if (milikSendiri) RoundedCornerShape(
        topStart = besar,
        topEnd = if (atasNyambung) kecil else besar,
        bottomEnd = if (bawahNyambung) kecil else besar,
        bottomStart = besar,
    ) else RoundedCornerShape(
        topStart = if (atasNyambung) kecil else besar,
        topEnd = besar,
        bottomEnd = besar,
        bottomStart = if (bawahNyambung) kecil else besar,
    )
}

@Composable
private fun Bubble(
    item: ItemChat.Bubble,
    onLompatKe: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = item.pesan
    val warnaBubble = if (item.milikSendiri) BubbleSendiri else BubbleLawan
    val warnaTeks = if (item.milikSendiri) Color.White else TeksUtama

    Column(
        modifier
            .widthIn(max = 290.dp)
            .clip(bentukBubble(item.milikSendiri, item.posisi))
            .background(warnaBubble)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        if (item.tampilkanIdentitas) {
            Text(
                p.senderName,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = warnaNama(p.senderId),
                modifier = Modifier.padding(bottom = 2.dp),
            )
        }

        if (p.replyToId != null || p.replyToSnippet != null) {
            KutipanReply(
                nama = p.replyToName ?: "Pesan",
                snippet = p.replyToSnippet.orEmpty(),
                diBubbleSendiri = item.milikSendiri,
                modifier = Modifier
                    .padding(bottom = 4.dp)
                    .clickable(enabled = p.replyToId != null) { p.replyToId?.let(onLompatKe) },
            )
        }

        if (p.imagePath != null) {
            AsyncImage(
                model = ChatRepository.urlFoto(p.imagePath),
                imageLoader = AvatarStorage.imageLoader(LocalContext.current),
                contentDescription = "Foto dari ${p.senderName}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .padding(bottom = if (p.body.isNotBlank()) 4.dp else 0.dp)
                    .width(240.dp)
                    .heightIn(min = 140.dp, max = 320.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (item.milikSendiri) Color(0x33FFFFFF) else Color(0x11000000)),
            )
        }

        Row(verticalAlignment = Alignment.Bottom) {
            if (p.body.isNotBlank()) {
                Text(
                    p.body,
                    fontSize = 16.sp,
                    color = warnaTeks,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                formatJam.format(Date(p.createdAtMs)),
                fontSize = 10.5.sp,
                color = if (item.milikSendiri) Color(0xB3FFFFFF) else TeksSekunder,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun KutipanReply(
    nama: String,
    snippet: String,
    diBubbleSendiri: Boolean,
    modifier: Modifier = Modifier,
) {
    val latar = if (diBubbleSendiri) Color(0x2EFFFFFF) else Color(0x0F000000)
    val warnaBar = if (diBubbleSendiri) Color.White else BiruIos
    Row(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(latar)
            .heightIn(min = 34.dp),
    ) {
        Box(Modifier.width(3.dp).heightIn(min = 34.dp).background(warnaBar))
        Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text(
                nama,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (diBubbleSendiri) Color.White else BiruIos,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                snippet.ifBlank { "📷 Foto" },
                fontSize = 12.sp,
                color = if (diBubbleSendiri) Color(0xCCFFFFFF) else TeksSekunder,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/* ---------- Kiriman tertunda (jam pasir / gagal) ---------- */

@Composable
private fun BubbleTertunda(
    kiriman: KirimanTertunda,
    onUlangi: (String) -> Unit,
    onBatal: (String) -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (kiriman.gagal) {
            Icon(
                Icons.Filled.ErrorOutline, "Gagal terkirim",
                tint = Merah,
                modifier = Modifier.padding(end = 6.dp).size(20.dp),
            )
        }
        Box {
            Column(
                Modifier
                    .widthIn(max = 290.dp)
                    .clip(bentukBubble(milikSendiri = true, posisi = PosisiGrup.TUNGGAL))
                    .background(if (kiriman.gagal) BubbleSendiri.copy(alpha = 0.55f) else BubbleSendiri.copy(alpha = 0.8f))
                    .clickable { if (kiriman.gagal) menu = true }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                kiriman.replyTo?.let {
                    KutipanReply(
                        nama = it.senderName,
                        snippet = snippetPesan(it.body, it.imagePath),
                        diBubbleSendiri = true,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                kiriman.fotoWebp?.let { bytes ->
                    val bmp = remember(kiriman.kunci) {
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                    }
                    if (bmp != null) {
                        Image(
                            bitmap = bmp,
                            contentDescription = "Foto sedang dikirim",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .padding(bottom = if (kiriman.body.isNotBlank()) 4.dp else 0.dp)
                                .width(240.dp)
                                .heightIn(min = 140.dp, max = 320.dp)
                                .clip(RoundedCornerShape(12.dp)),
                        )
                    }
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    if (kiriman.body.isNotBlank()) {
                        Text(kiriman.body, fontSize = 16.sp, color = Color.White, modifier = Modifier.weight(1f, fill = false))
                        Spacer(Modifier.width(6.dp))
                    }
                    if (kiriman.gagal) {
                        Text("Gagal — ketuk untuk opsi", fontSize = 10.5.sp, color = Color(0xFFFFD7D5))
                    } else {
                        Icon(Icons.Filled.Schedule, "Sedang dikirim", tint = Color(0xB3FFFFFF), modifier = Modifier.size(12.dp))
                    }
                }
                // Alasan gagal ditampilkan apa adanya: tanpa ini, unggahan yang
                // ditolak server hanya terlihat sebagai bubble pucat tanpa sebab.
                kiriman.alasanGagal?.let { alasan ->
                    Text(
                        alasan,
                        fontSize = 10.sp,
                        color = Color(0xFFFFD7D5),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("Coba kirim lagi") }, onClick = { menu = false; onUlangi(kiriman.kunci) })
                DropdownMenuItem(
                    text = { Text("Buang", color = Merah) },
                    onClick = { menu = false; onBatal(kiriman.kunci) },
                )
            }
        }
    }
}

/* ---------- Pernak-pernik daftar ---------- */

@Composable
private fun PemisahTanggal(label: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
        Text(
            label,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            color = TeksSekunder,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFF2F2F7))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun BannerSementara() {
    Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
        Text(
            "🕒 Pesan di ruang ini otomatis terhapus setelah 24 jam",
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            color = TeksBanner,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(LatarBanner)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun KeadaanKosong() {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("👋", fontSize = 40.sp)
        Spacer(Modifier.height(8.dp))
        Text("Belum ada pesan hari ini", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TeksUtama)
        Text("Jadilah yang pertama menyapa tim!", fontSize = 13.sp, color = TeksSekunder)
    }
}

@Composable
private fun KeadaanGalat(pesan: String, onCoba: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.ErrorOutline, null, tint = TeksSekunder, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(10.dp))
        Text(pesan, fontSize = 14.sp, color = TeksSekunder, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onCoba) { Text("Coba lagi", color = BiruIos) }
    }
}

@Composable
private fun TombolKeBawah(badge: Int, onClick: () -> Unit) {
    Box {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White)
                .clickable(onClick = onClick),
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

/* ---------- Composer ---------- */

@Composable
private fun KartuBalasComposer(target: PesanChat, onTutup: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(LatarBar)
            .padding(start = 12.dp, end = 4.dp, top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(3.dp).height(36.dp).clip(RoundedCornerShape(2.dp)).background(BiruIos))
        Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
            Text("Membalas ${target.senderName}", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = BiruIos, maxLines = 1)
            Text(
                snippetPesan(target.body, target.imagePath),
                fontSize = 12.5.sp, color = TeksSekunder, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onTutup) { Icon(Icons.Filled.Close, "Batal membalas", tint = TeksSekunder) }
    }
}

@Composable
private fun KomposerChat(
    sedangKompres: Boolean,
    onKetik: (String) -> Unit,
    onKirim: (String) -> Unit,
    onLampiran: () -> Unit,
) {
    var teks by remember { mutableStateOf("") }
    Column(Modifier.fillMaxWidth().background(LatarBar)) {
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(GarisTipis))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            IconButton(onClick = onLampiran, enabled = !sedangKompres) {
                if (sedangKompres) {
                    CircularProgressIndicator(color = BiruIos, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                    Icon(Icons.Filled.AttachFile, "Lampirkan foto", tint = BiruIos, modifier = Modifier.size(24.dp))
                }
            }
            Box(
                Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                if (teks.isEmpty()) {
                    Text("Ketik pesan…", fontSize = 16.sp, color = TeksSekunder)
                }
                BasicTextField(
                    value = teks,
                    onValueChange = { teks = it; onKetik(it) },
                    textStyle = TextStyle(fontSize = 16.sp, color = TeksUtama),
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.width(6.dp))
            val bisaKirim = teks.isNotBlank()
            Box(
                Modifier
                    .padding(bottom = 6.dp)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (bisaKirim) BiruIos else Color(0xFFC7C7CC))
                    .clickable(enabled = bisaKirim) {
                        onKirim(teks)
                        teks = ""
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.ArrowUpward, "Kirim", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}

/* ---------- Indikator mengetik (bubble tiga titik) ---------- */

/**
 * Tiga titik memantul bergiliran, seperti gelembung "typing" WhatsApp.
 *
 * Jeda antar titik datang dari [StartOffset] pada `infiniteRepeatable`, BUKAN
 * dari `delayMillis` di dalam `tween`: delay di dalam tween ikut diulang tiap
 * siklus, sehingga ketiga titik berhenti bersamaan lalu bergerak bersamaan —
 * itulah yang membuat versi sebelumnya terlihat berkedut, bukan mengalir.
 */
@Composable
fun BubbleTigaTitik(modifier: Modifier = Modifier) {
    val transisi = rememberInfiniteTransition(label = "pengetik")
    Row(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(BubbleLawan)
            .padding(horizontal = 13.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { i ->
            val maju by transisi.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(420, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(i * 140),
                ),
                label = "titik$i",
            )
            Box(
                Modifier
                    .padding(horizontal = 2.5.dp)
                    .size(7.dp)
                    .graphicsLayer {
                        translationY = -maju * 4.dp.toPx()
                        alpha = 0.32f + maju * 0.68f
                    }
                    .clip(CircleShape)
                    .background(TeksSekunder),
            )
        }
    }
}
