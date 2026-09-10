package com.sukashawarma.superapp.feature.chat.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextDecoration
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
import com.sukashawarma.superapp.feature.chat.ChatBacaan
import com.sukashawarma.superapp.feature.chat.ChatKehadiran
import com.sukashawarma.superapp.feature.chat.data.AnggotaGrup
import com.sukashawarma.superapp.feature.chat.data.ChatRepository
import com.sukashawarma.superapp.feature.chat.data.PesanChat
import com.sukashawarma.superapp.feature.chat.data.ReaksiPesan
import com.sukashawarma.superapp.feature.chat.data.Sebutan
import com.sukashawarma.superapp.feature.chat.domain.ItemChat
import com.sukashawarma.superapp.feature.chat.domain.cariKueriSebutan
import com.sukashawarma.superapp.feature.chat.domain.rentangSebutan
import com.sukashawarma.superapp.feature.chat.domain.sebutanTerpakai
import com.sukashawarma.superapp.feature.chat.domain.sisipkanSebutan
import com.sukashawarma.superapp.feature.chat.domain.PosisiGrup
import com.sukashawarma.superapp.feature.chat.domain.indeksWarnaNama
import com.sukashawarma.superapp.feature.chat.domain.MAKS_WAJAH_PENGETIK
import com.sukashawarma.superapp.feature.chat.domain.Pengetik
import com.sukashawarma.superapp.feature.chat.domain.labelPengetik
import com.sukashawarma.superapp.feature.chat.domain.labelPengetikPendek
import com.sukashawarma.superapp.feature.chat.domain.snippetPesan
import com.sukashawarma.superapp.feature.chat.domain.susunItemChat
import com.sukashawarma.superapp.feature.chat.ui.emoji.PapanEmoji
import com.sukashawarma.superapp.feature.chat.ui.emoji.hapusSatuKarakter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

/** Aksen merek. Dipakai untuk hal yang menuntut perhatian: sorotan lompatan,
 *  mode sunting, dan tombol '@'. */
private val Oranye = Color(0xFFEA580C)

/** Warna nama pengirim di grup — satu warna tetap per orang, seperti WA. */
/** Cermin batas di `chat_edit_pesan`. Di sini hanya menentukan apakah tombolnya
 *  TAMPIL; yang menolak sungguhan tetap database. */
private const val BATAS_SUNTING_MS = 15L * 60 * 1000

private val WarnaNama = listOf(
    Color(0xFFE542A3), Color(0xFF1F7AEC), Color(0xFFFA6533), Color(0xFF009688),
    Color(0xFF9C27B0), Color(0xFFD32F2F), Color(0xFF7CB342), Color(0xFFFF9800),
)

private fun warnaNama(senderId: String): Color = WarnaNama[indeksWarnaNama(senderId, WarnaNama.size)]

// Jam bubble kini diformat sekali per pesan di `susunItemChat`, bukan di dalam
// composable. SimpleDateFormat juga tidak aman dipakai bersama antar-thread,
// jadi menyimpannya sebagai objek bersama seperti dulu memang salah tempat.

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

    // Menu tekan-lama, konfirmasi hapus, dan pengaturan grup.
    var menuPesan by remember { mutableStateOf<PesanChat?>(null) }
    var konfirmasiHapus by remember { mutableStateOf<PesanChat?>(null) }
    var sheetReaksi by remember { mutableStateOf<PesanChat?>(null) }
    var pilihEmojiReaksi by remember { mutableStateOf<PesanChat?>(null) }
    var sheetInfo by remember { mutableStateOf(false) }
    // Siapa yang sedang dibuka kartu profilnya. Menyimpan id + snapshot nama/foto
    // dari pesan, bukan objek anggotanya: orang yang sudah non-aktif tidak ada di
    // daftar anggota, tapi pesannya masih ada dan fotonya tetap harus bisa dibuka.
    var profilDibuka by remember { mutableStateOf<Triple<String, String, String?>?>(null) }
    // Pesan yang sedang disorot setelah melompat dari kartu kutipan.
    var sorotPesanId by remember { mutableStateOf<String?>(null) }
    // Foto yang sedang dibuka layar penuh: URL siap muat + judulnya.
    var fotoDibuka by remember { mutableStateOf<Pair<String, String>?>(null) }

    // Sorotan padam sendiri. Membiarkannya menyala terus membuat pesan itu
    // tampak "terpilih" selamanya, padahal ia hanya sedang ditunjukkan.
    LaunchedEffect(sorotPesanId) {
        if (sorotPesanId == null) return@LaunchedEffect
        // Lebih lama dari denyutnya (220 + 1000 + 420 ms) supaya gelembungnya
        // sempat memudar keluar; dibuang lebih cepat, sorotannya lenyap
        // mendadak di tengah animasi.
        kotlinx.coroutines.delay(1_900)
        sorotPesanId = null
    }
    var menyimpanPengaturan by remember { mutableStateOf(false) }
    var galatPengaturan by remember { mutableStateOf<String?>(null) }
    val clipboard = LocalClipboardManager.current

    // Nama grup dititipkan ke penanda kehadiran supaya notifikasi memakai judul
    // percakapan yang sama dengan yang dilihat pengguna di dalam aplikasi.
    LaunchedEffect(state.pengaturan.namaGrup) {
        ChatKehadiran.catatNamaGrup(state.pengaturan.namaGrup)
    }

    // Selama menu tekan-lama terbuka, isi chat diburamkan supaya pesan yang
    // dipilih menonjol. Modifier.blur baru bekerja di Android 12+; di bawah itu
    // scrim gelap milik menu yang menanggung seluruh pemisahannya.
    val buramkan = menuPesan != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    // Notifikasi pesan masuk ditahan selama layar ini TERLIHAT — pesan yang
    // sedang dibaca tidak perlu diumumkan lagi di baki notifikasi.
    //
    // Penandanya mengikuti DAUR HIDUP, bukan komposisi. `DisposableEffect`
    // saja tidak cukup: menekan Home tidak membuang composable ini, jadi
    // penandanya akan tetap menyala selama layar chat menjadi layar terakhir —
    // dan notifikasi ikut dibungkam selamanya walau aplikasi sudah lama
    // ditinggalkan. Itu persis bug yang membuat push terlihat "tidak jalan".
    // Izin notifikasi diperiksa ulang tiap kali layar kembali terlihat: pengguna
    // bisa saja baru menyalakannya dari Setelan lalu kembali ke sini.
    var notifikasiMati by remember { mutableStateOf(false) }
    fun periksaIzinNotifikasi() {
        notifikasiMati = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
    }

    val pemilikDaurHidup = LocalLifecycleOwner.current
    DisposableEffect(pemilikDaurHidup) {
        val pengamat = LifecycleEventObserver { _, peristiwa ->
            when (peristiwa) {
                Lifecycle.Event.ON_RESUME -> {
                    ChatKehadiran.masuk()
                    periksaIzinNotifikasi()
                }
                // Ditandai saat MENINGGALKAN layar, bukan saat masuk: pesan yang
                // tiba selagi percakapan terbuka juga sudah terbaca, dan menandai
                // di awal saja akan menyisakan lencana untuk pesan-pesan itu.
                Lifecycle.Event.ON_PAUSE -> {
                    ChatKehadiran.keluar()
                    ChatBacaan.tandaiDibaca(context)
                }
                else -> Unit
            }
        }
        pemilikDaurHidup.lifecycle.addObserver(pengamat)
        ChatKehadiran.masuk()
        periksaIzinNotifikasi()
        onDispose {
            pemilikDaurHidup.lifecycle.removeObserver(pengamat)
            ChatKehadiran.keluar()
            ChatBacaan.tandaiDibaca(context)
        }
    }

    // Daftar item (bubble + pemisah), terbaru DULU karena LazyColumn reverseLayout.
    val itemTampil = remember(state.pesan, userId) {
        susunItemChat(state.pesan, userId, System.currentTimeMillis()).asReversed()
    }
    // `asReversed()` di sini SEKALI, bukan di dalam lambda item: dipanggil di
    // sana, satu pandangan baru dialokasikan untuk tiap petak yang digambar.
    val tertundaTerbalik = remember(state.tertunda) { state.tertunda.asReversed() }

    // Batas baca DIBACA SEKALI saat layar dibuka, bukan setiap penyusunan ulang.
    // Meninggalkan layar akan memajukan penandanya (lihat DisposableEffect di
    // atas); kalau nilainya dibaca ulang terus, garis "belum dibaca" akan
    // bergeser sendiri selagi percakapan masih dilihat.
    val batasBacaMs = remember { ChatBacaan.terakhirDibaca(context) }

    // Pesan orang lain yang datang setelah penanda baca. `indexOfLast` karena
    // daftar ini terbalik: yang PALING TUA justru berada di ujung belakang, dan
    // itulah tempat percakapan harus dilanjutkan.
    val indeksBelumDibaca = remember(itemTampil, batasBacaMs, userId) {
        itemTampil.indexOfLast {
            it is ItemChat.Bubble && it.pesan.senderId != userId && it.pesan.createdAtMs > batasBacaMs
        }
    }
    val jumlahBelumDibaca = remember(itemTampil, batasBacaMs, userId) {
        itemTampil.count {
            it is ItemChat.Bubble && it.pesan.senderId != userId && it.pesan.createdAtMs > batasBacaMs
        }
    }
    // Sebutan belum dibaca yang paling tua — sasaran tombol '@'.
    val indeksSebutanku = remember(itemTampil, batasBacaMs, userId) {
        if (userId.isBlank()) -1 else itemTampil.indexOfLast {
            it is ItemChat.Bubble && it.pesan.senderId != userId &&
                it.pesan.createdAtMs > batasBacaMs &&
                it.pesan.mentions.any { m -> m.id == userId }
        }
    }
    var sebutanSudahDilihat by remember { mutableStateOf(false) }

    // Auto-ikut ke bawah saat pesan baru datang dan pengguna memang sedang di bawah;
    // kalau sedang membaca ke atas, jangan menyeret paksa — cukup badge pesan baru.
    var jumlahTerlihat by remember { mutableIntStateOf(0) }
    /** Sudahkah layar diposisikan ke tempat pembacaan terakhir. */
    var sudahPosisiAwal by remember { mutableStateOf(false) }
    var pesanBaruBelumDilihat by remember { mutableIntStateOf(0) }
    val diBawah by remember {
        androidx.compose.runtime.derivedStateOf {
            listState.firstVisibleItemIndex <= 1 && listState.firstVisibleItemScrollOffset < 240
        }
    }
    LaunchedEffect(state.pesan.size, state.tertunda.size) {
        val total = state.pesan.size + state.tertunda.size

        // PEMUATAN PERTAMA: lanjutkan dari tempat terakhir ditinggalkan, bukan
        // diseret ke dasar percakapan. Orang yang meninggalkan 40 pesan belum
        // dibaca lalu dilempar ke pesan paling baru harus menggulir mundur
        // sendiri untuk tahu apa yang ia lewatkan — dan dengan chat yang hilang
        // tiap 24 jam, yang terlewat itu tidak akan pernah dibacanya lagi.
        if (!sudahPosisiAwal && total > 0) {
            sudahPosisiAwal = true
            jumlahTerlihat = total
            if (indeksBelumDibaca > 0) {
                // `scrollToItem`, bukan animasi: ini menempatkan layar sebelum
                // pengguna sempat melihatnya, bukan perjalanan yang perlu
                // diikuti mata.
                listState.scrollToItem(indeksBelumDibaca)
                pesanBaruBelumDilihat = jumlahBelumDibaca
            }
            return@LaunchedEffect
        }

        if (total > jumlahTerlihat) {
            if (diBawah || state.tertunda.isNotEmpty()) {
                listState.gulirHalusKe(0)
                pesanBaruBelumDilihat = 0
            } else {
                pesanBaruBelumDilihat += total - jumlahTerlihat
            }
        }
        jumlahTerlihat = total
    }
    LaunchedEffect(diBawah) { if (diBawah) pesanBaruBelumDilihat = 0 }

    // Isi kotak ketik hidup di sini, bukan di dalam komposer: papan emoji harus
    // bisa menyisipkan dan menghapus karakter pada teks yang sama.
    //
    // Disimpan di dalam pemegang, bukan sebagai `var teks by remember` langsung:
    // membaca state di badan ChatScreen membuat SETIAP ketukan tombol menyusun
    // ulang seluruh layar — header, daftar, komposer. Dengan pemegang ini,
    // pembacaannya terjadi di dalam komposer saja, dan pemanggil lain cukup
    // menyentuhnya dari dalam lambda (yang berjalan saat diklik, bukan saat
    // disusun).
    val ketikan = remember { IsiKetikan() }
    var papanEmoji by remember { mutableStateOf(false) }
    val papanKetik = LocalSoftwareKeyboardController.current

    // Tombol kembali menutup papan emoji dulu — persis seperti ia menutup papan
    // ketik, bukan langsung meninggalkan percakapan.
    BackHandler(enabled = state.suntingTarget != null) {
        viewModel.setSunting(null)
        ketikan.teks = ""
    }
    BackHandler(enabled = papanEmoji) { papanEmoji = false }
    BackHandler(enabled = !papanEmoji, onBack = onBack)

    Box(Modifier.fillMaxSize().background(LatarChat)) {
    Column(
        Modifier
            .fillMaxSize()
            .then(if (buramkan) Modifier.blur(10.dp) else Modifier)
            .imePadding()
            .navigationBarsPadding(),
    ) {
        HeaderChat(
            judul = state.pengaturan.namaGrup,
            fotoGrup = state.pengaturan.fotoGrup,
            // Header memakai versi PENDEK: ruangnya sempit dan sudah berbagi
            // dengan nama grup, jadi menyebut dua nama di sana justru terpotong.
            // Daftar nama lengkapnya ada di baris pengetik di dasar percakapan.
            subtitle = labelPengetikPendek(state.pengetik)
                ?: if (state.pengaturan.hanyaAdmin) "Mode pengumuman aktif"
                else "Pesan hilang otomatis setelah 24 jam",
            subtitleAktif = state.pengetik.isNotEmpty(),
            onBukaInfo = { galatPengaturan = null; sheetInfo = true; viewModel.muatAnggota() },
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
                    item(key = "pengetik", contentType = "pengetik") {
                        BarisPengetik(state.pengetik)
                    }

                    // Kiriman tertunda selalu paling baru, jadi ditaruh paling awal.
                    items(
                        count = tertundaTerbalik.size,
                        key = { i -> tertundaTerbalik[i].kunci },
                        contentType = { "tertunda" },
                    ) { i ->
                        BubbleTertunda(
                            kiriman = tertundaTerbalik[i],
                            onUlangi = viewModel::ulangi,
                            onBatal = viewModel::batalkanKiriman,
                        )
                    }
                    items(
                        count = itemTampil.size,
                        key = { i ->
                            when (val item = itemTampil[i]) {
                                is ItemChat.Bubble -> item.pesan.id
                                is ItemChat.Pemisah -> "pemisah-${item.label}"
                            }
                        },
                        // Bubble dan pemisah tanggal berbeda bentuk. Dengan
                        // penanda jenis ini, Compose memakai ulang petak yang
                        // sejenis alih-alih menyusun ulang dari nol tiap kali
                        // daftar digulir.
                        contentType = { i ->
                            when (itemTampil[i]) {
                                is ItemChat.Bubble -> "bubble"
                                is ItemChat.Pemisah -> "pemisah"
                            }
                        },
                    ) { i ->
                        when (val item = itemTampil[i]) {
                            is ItemChat.Pemisah -> PemisahTanggal(item.label)
                            is ItemChat.Bubble -> BarisBubble(
                                item = item,
                                reaksi = state.reaksi[item.pesan.id].orEmpty(),
                                userId = userId,
                                onBalas = { viewModel.setBalas(it) },
                                onTekanLama = { menuPesan = it },
                                onKetukReaksi = { sheetReaksi = it },
                                disorot = item.pesan.id == sorotPesanId,
                                onKlikFoto = { pesan ->
                                    ChatRepository.urlFoto(pesan.imagePath)?.let { url ->
                                        fotoDibuka = url to pesan.senderName
                                    }
                                },
                                onKlikPengirim = { pesan ->
                                    profilDibuka = Triple(pesan.senderId, pesan.senderName, pesan.senderAvatar)
                                    viewModel.muatAnggota()
                                },
                                onKlikSebutan = { orang ->
                                    // Wajahnya sengaja null: yang tersimpan di
                                    // pesan hanya id dan nama saat disebut.
                                    // Kartu profil menariknya sendiri dari
                                    // daftar anggota, sehingga foto yang tampil
                                    // selalu yang terbaru, bukan yang lama.
                                    profilDibuka = Triple(orang.id, orang.nama, null)
                                    viewModel.muatAnggota()
                                },
                                onLompatKe = { idAsal ->
                                    val idx = itemTampil.indexOfFirst { it is ItemChat.Bubble && it.pesan.id == idAsal }
                                    if (idx >= 0) {
                                        scope.launch { listState.gulirHalusKe(idx) }
                                        sorotPesanId = idAsal
                                    } else {
                                        // Pesan asli bisa sudah dihapus atau lewat
                                        // 24 jam. Diam saja membuat ketukan terasa
                                        // rusak, padahal jawabannya sederhana.
                                        Toast.makeText(
                                            context,
                                            "Pesan aslinya sudah tidak ada.",
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    }
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

            // Tombol melayang, gaya iOS: '@' di atas, lompat-ke-bawah di bawah.
            Column(
                Modifier.align(Alignment.BottomEnd).padding(14.dp),
                horizontalAlignment = Alignment.End,
            ) {
                // Tombol '@' hanya lahir bila memang ada sebutan yang belum
                // dibaca. Di grup se-perusahaan, disebut namanya jarang terjadi —
                // dan tombol yang selalu ada akan berhenti berarti.
                androidx.compose.animation.AnimatedVisibility(
                    visible = indeksSebutanku >= 0 && !sebutanSudahDilihat && !state.memuat,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 },
                ) {
                    TombolKeSebutan {
                        sebutanSudahDilihat = true
                        val sasaran = itemTampil.getOrNull(indeksSebutanku)
                        scope.launch { listState.gulirHalusKe(indeksSebutanku) }
                        if (sasaran is ItemChat.Bubble) sorotPesanId = sasaran.pesan.id
                    }
                }
                Spacer(Modifier.height(10.dp))
                androidx.compose.animation.AnimatedVisibility(
                    visible = !diBawah && !state.memuat,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 },
                ) {
                    TombolKeBawah(pesanBaruBelumDilihat) {
                        scope.launch { listState.gulirHalusKe(0) }
                    }
                }
            }
        }

        if (notifikasiMati) {
            PitaNotifikasiMati {
                // Dialog izin Android berhenti muncul setelah dua penolakan,
                // jadi satu-satunya jalan yang pasti berhasil adalah halaman
                // setelan notifikasi aplikasi ini.
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }

        state.suntingTarget?.let { target ->
            KartuSuntingComposer(target) {
                viewModel.setSunting(null)
                ketikan.teks = ""
            }
        }

        state.balasTarget?.let { target ->
            KartuBalasComposer(target) { viewModel.setBalas(null) }
        }

        if (state.bolehKirim) {
            KomposerChat(
                ketikan = ketikan,
                anggota = state.anggota,
                userId = userId,
                onMulaiMenyebut = { viewModel.muatAnggota() },
                papanEmojiTerbuka = papanEmoji,
                sedangKompres = sedangKompres,
                // Isi kotak ketik ditulis komposer sendiri (kursornya harus
                // ikut terjaga); yang sampai ke sini hanya kabar 'ada yang
                // mengetik' untuk disiarkan ke anggota lain.
                onTeksBerubah = { viewModel.ketikan(it) },
                onToggleEmoji = {
                    if (papanEmoji) {
                        papanEmoji = false
                        papanKetik?.show()
                    } else {
                        // Papan emoji MENGGANTIKAN papan ketik, tidak menumpuk
                        // di atasnya — kalau keduanya terbuka, daftar pesan
                        // terdorong sampai tak tersisa.
                        papanKetik?.hide()
                        papanEmoji = true
                    }
                },
                onFokusIsian = { papanEmoji = false },
                onKirim = {
                    // Menyunting TIDAK mengubah daftar sebutan: siapa yang
                    // tersebut sudah ditetapkan saat pesan terkirim, dan
                    // menyebut orang lewat suntingan tidak boleh dipakai untuk
                    // membangunkan orang berulang kali (aturan yang sama
                    // dipegang trigger push di server).
                    if (state.suntingTarget != null) viewModel.simpanSuntingan(ketikan.teks)
                    else viewModel.kirimTeks(ketikan.teks, sebutanTerpakai(ketikan.teks, ketikan.kandidat))
                    ketikan.teks = ""
                },
                modeSunting = state.suntingTarget != null,
                onLampiran = { papanEmoji = false; lembarSumber = true },
            )
            if (papanEmoji) {
                PapanEmoji(
                    onPilih = { ketikan.sisip(it) },
                    onHapus = { ketikan.teks = hapusSatuKarakter(ketikan.teks) },
                )
            }
        } else {
            PitaModePengumuman()
        }
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

    menuPesan?.let { pesan ->
        val item = itemTampil.filterIsInstance<ItemChat.Bubble>().firstOrNull { it.pesan.id == pesan.id }
        if (item == null) {
            menuPesan = null
        } else {
            MenuPesanPopup(
                milikSendiri = item.milikSendiri,
                emojiTerpilih = state.reaksi[pesan.id]?.firstOrNull { it.userId == userId }?.emoji,
                bolehHapus = item.milikSendiri,
                // Foto pun boleh disunting — yang berubah keterangannya.
                bolehSunting = item.milikSendiri &&
                    System.currentTimeMillis() - pesan.createdAtMs <= BATAS_SUNTING_MS,
                adaTeks = pesan.body.isNotBlank(),
                onEmoji = { emoji ->
                    viewModel.toggleReaksi(pesan, emoji)
                    menuPesan = null
                },
                onSemuaEmoji = { pilihEmojiReaksi = pesan; menuPesan = null },
                onBalas = { viewModel.setBalas(pesan); menuPesan = null },
                onSunting = {
                    ketikan.teks = pesan.body
                    viewModel.setSunting(pesan)
                    menuPesan = null
                },
                onSalin = {
                    clipboard.setText(AnnotatedString(pesan.body))
                    menuPesan = null
                },
                onHapus = { konfirmasiHapus = pesan; menuPesan = null },
                onTutup = { menuPesan = null },
                bubble = {
                    // Bubble yang sama persis, tanpa identitas berulang supaya
                    // fokusnya pada isi pesan yang sedang dipilih.
                    Bubble(item = item.copy(tampilkanIdentitas = false), onLompatKe = {})
                },
            )
        }
    }

    konfirmasiHapus?.let { pesan ->
        AlertDialog(
            onDismissRequest = { konfirmasiHapus = null },
            title = { Text("Hapus pesan?") },
            text = { Text("Pesan dihapus untuk semua orang di ruang ini.") },
            confirmButton = {
                TextButton(onClick = { konfirmasiHapus = null; viewModel.hapus(pesan) }) {
                    Text("Hapus", color = Merah)
                }
            },
            dismissButton = { TextButton(onClick = { konfirmasiHapus = null }) { Text("Batal") } },
        )
    }

    pilihEmojiReaksi?.let { pesan ->
        LembarSemuaEmoji(
            onPilih = { emoji ->
                viewModel.toggleReaksi(pesan, emoji)
                pilihEmojiReaksi = null
            },
            onTutup = { pilihEmojiReaksi = null },
        )
    }

    sheetReaksi?.let { pesan ->
        val daftar = state.reaksi[pesan.id].orEmpty()
        if (daftar.isEmpty()) {
            sheetReaksi = null
        } else {
            DaftarReaksiSheet(
                reaksi = daftar,
                // Avatar tidak disimpan per reaksi; diambil dari pesan yang masih
                // hidup bila orangnya pernah mengirim, selain itu huruf awal nama.
                avatarPerUser = remember(state.pesan) {
                    state.pesan.associate { it.senderId to it.senderAvatar }
                },
                userId = userId,
                onCabut = {
                    val emojiku = daftar.firstOrNull { it.userId == userId }?.emoji
                    if (emojiku != null) viewModel.toggleReaksi(pesan, emojiku)
                    sheetReaksi = null
                },
                onTutup = { sheetReaksi = null },
            )
        }
    }

    if (sheetInfo) {
        InfoGrupSheet(
            awal = state.pengaturan,
            bolehSunting = state.pengelola,
            menyimpan = menyimpanPengaturan,
            galat = galatPengaturan,
            anggota = state.anggota,
            memuatAnggota = state.memuatAnggota,
            userId = userId,
            onKlikAnggota = { profilDibuka = Triple(it.id, it.nama, it.avatar) },
            onSimpan = { nama, deskripsi, hanyaAdmin, fotoJpeg, hapusFoto ->
                menyimpanPengaturan = true
                viewModel.simpanPengaturan(nama, deskripsi, hanyaAdmin, fotoJpeg, hapusFoto) { galat ->
                    menyimpanPengaturan = false
                    galatPengaturan = galat
                    if (galat == null) {
                        sheetInfo = false
                        Toast.makeText(context, "Pengaturan grup tersimpan.", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onTutup = { sheetInfo = false },
        )
    }

    fotoDibuka?.let { (url, judul) ->
        PenampilFoto(
            url = url,
            judul = judul,
            keterangan = "Foto di Chat Tim",
            onTutup = { fotoDibuka = null },
        )
    }

    profilDibuka?.let { (id, namaCadangan, avatarCadangan) ->
        ProfilAnggotaSheet(
            anggota = state.anggota.firstOrNull { it.id == id },
            namaCadangan = namaCadangan,
            avatarCadangan = avatarCadangan,
            memuat = state.memuatAnggota,
            akuSendiri = id == userId,
            onTutup = { profilDibuka = null },
        )
    }
    }
}

/**
 * Papan emoji lengkap sebagai lembar, untuk memilih reaksi di luar enam
 * pintasan. Papan yang sama dengan yang dipakai kotak ketik — satu tampilan,
 * dua tempat pemakaian.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LembarSemuaEmoji(onPilih: (String) -> Unit, onTutup: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onTutup,
        containerColor = Color(0xFFF7F7F8),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Column(Modifier.navigationBarsPadding()) {
            Text(
                "Pilih reaksi",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TeksUtama,
                modifier = Modifier.padding(start = 20.dp, bottom = 8.dp),
            )
            PapanEmoji(
                onPilih = onPilih,
                // Papan reaksi tidak punya teks untuk dihapus.
                onHapus = {},
            )
        }
    }
}

/**
 * Pita peringatan saat izin notifikasi belum diberikan.
 *
 * Tanpa ini, notifikasi yang dibuang sistem tidak meninggalkan jejak apa pun di
 * layar — pengguna hanya merasa "chat tidak memberi tahu apa-apa" dan tidak
 * punya cara menemukan sebabnya.
 */
@Composable
private fun PitaNotifikasiMati(onAktifkan: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(LatarBanner)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.NotificationsOff,
            null,
            tint = TeksBanner,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            "Notifikasi mati, Anda tidak akan tahu ada pesan baru.",
            fontSize = 12.5.sp,
            color = TeksBanner,
            modifier = Modifier.weight(1f),
        )
        Text(
            "Aktifkan",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = BiruIos,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onAktifkan)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

/** Pengganti kotak ketik saat mode pengumuman menyala bagi yang bukan pengelola. */
@Composable
private fun PitaModePengumuman() {
    Column(Modifier.fillMaxWidth().background(LatarBar)) {
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(GarisTipis))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Filled.Campaign, null, tint = TeksSekunder, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "Hanya pengelola yang dapat mengirim pesan",
                fontSize = 13.sp,
                color = TeksSekunder,
            )
        }
    }
}

/**
 * Denyut sorotan: 0 -> 1 (muncul), ditahan, lalu 1 -> 0 (padam).
 *
 * Berdiri sebagai composable sendiri supaya state animasinya lahir dan mati
 * bersama sorotannya — bukan menumpang di setiap gelembung yang kebetulan
 * tersusun.
 *
 * SATU float untuk seluruh efek. Nilainya dipakai bersama oleh cincin, kilau
 * latar, dan pembesaran halus, sehingga ketiganya bergerak sebagai satu benda —
 * dan yang dibayar tetap satu animasi, pada satu gelembung, sekali lompatan.
 * Padamnya ikut dianimasikan; versi sebelumnya hanya memudar masuk lalu hilang
 * begitu saja, dan justru kepergian mendadak itulah yang terlihat patah.
 */
@Composable
private fun denyutSorot(): Animatable<Float, *> {
    val maju = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        maju.animateTo(1f, tween(220, easing = FastOutSlowInEasing))
        kotlinx.coroutines.delay(1_000)
        maju.animateTo(0f, tween(420, easing = FastOutSlowInEasing))
    }
    return maju
}

/** Sisa jarak yang masih dianimasikan setelah lompatan instan. */
private const val EKOR_GULIR = 10

/**
 * Menggulir ke [indeks] tanpa membekukan layar.
 *
 * `animateScrollToItem` MENYUSUN SETIAP ITEM yang dilewatinya. Melompat dari
 * dasar percakapan ke pesan yang dikutip seratus baris di atas berarti menyusun
 * seratus gelembung dalam satu burst — dan tiap gelembung membawa dua animasi,
 * satu penangan gestur, dan permintaan gambar avatar. Terukur di perangkat:
 * frame persentil ke-99 melonjak ke 700ms, sementara menggulir manual pada
 * daftar yang sama hanya 65ms.
 *
 * Karena itu jarak jauh ditempuh dengan `scrollToItem` yang langsung meloncat
 * (hanya menyusun satu layar penuh di tujuan), lalu sisa [EKOR_GULIR] item
 * dianimasikan supaya gerakannya tetap terbaca sebagai perpindahan, bukan
 * teleportasi.
 */
private suspend fun LazyListState.gulirHalusKe(indeks: Int) {
    val sasaran = indeks.coerceAtLeast(0)
    val jarak = kotlin.math.abs(sasaran - firstVisibleItemIndex)
    if (jarak > EKOR_GULIR) {
        val dekat = if (sasaran > firstVisibleItemIndex) sasaran - EKOR_GULIR else sasaran + EKOR_GULIR
        scrollToItem(dekat.coerceAtLeast(0))
    }
    animateScrollToItem(sasaran)
}

/**
 * Baris "sedang mengetik" di dasar daftar pesan: bubble tiga titik plus namanya,
 * muncul dan hilang dengan animasi halus.
 */
@Composable
private fun BarisPengetik(orang: List<Pengetik>) {
    // Daftar terakhir yang tidak kosong ditahan selama animasi keluar. Tanpa itu,
    // isinya berubah jadi kosong sebelum baris selesai menyusut, dan yang terlihat
    // adalah wajah-wajah lenyap mendadak lalu kotak kosong ikut mengecil.
    var terakhirTampak by remember { mutableStateOf(orang) }
    if (orang.isNotEmpty()) terakhirTampak = orang

    androidx.compose.animation.AnimatedVisibility(
        visible = orang.isNotEmpty(),
        enter = fadeIn(tween(180)) + expandVertically(tween(220)),
        exit = fadeOut(tween(140)) + shrinkVertically(tween(200)),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TumpukanWajahPengetik(terakhirTampak)
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

/**
 * Wajah para pengetik, ditumpuk saling menindih seperti daftar peserta.
 *
 * Dibatasi [MAKS_WAJAH_PENGETIK]; sisanya diringkas jadi lingkaran "+N".
 * Lebarnya karena itu punya batas atas yang pasti, sehingga baris ini tidak
 * pernah mendorong kalimat di sebelahnya keluar layar berapa pun jumlah orang
 * yang mengetik bersamaan.
 */
@Composable
private fun TumpukanWajahPengetik(orang: List<Pengetik>) {
    val tampil = orang.take(MAKS_WAJAH_PENGETIK)
    val sisa = orang.size - tampil.size
    val diameter = 22.dp
    val tindih = 7.dp

    Row(horizontalArrangement = Arrangement.spacedBy(-tindih)) {
        tampil.forEach { p ->
            Box(
                Modifier
                    .size(diameter)
                    // Cincin putih tipis memisahkan wajah yang saling menindih;
                    // tanpa itu dua foto gelap berdempet terlihat seperti satu
                    // gumpalan.
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(1.dp),
            ) {
                AvatarStaf(
                    path = p.avatar,
                    nama = p.nama,
                    modifier = Modifier.size(diameter - 2.dp),
                    ukuranHuruf = 9.sp,
                )
            }
        }
        if (sisa > 0) {
            Box(
                Modifier
                    .size(diameter)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(1.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.fillMaxSize().clip(CircleShape).background(Color(0xFFD8E7FB)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "+$sisa",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BiruIos,
                    )
                }
            }
        }
    }
}

/* ---------- Header ---------- */

@Composable
private fun HeaderChat(
    judul: String,
    fotoGrup: String?,
    subtitle: String,
    subtitleAktif: Boolean,
    onBukaInfo: () -> Unit,
    onBack: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().background(LatarBar).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 2.dp, end = 12.dp, top = 4.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBackIos, "Kembali", tint = BiruIos)
            }
            // Seluruh blok identitas grup dapat diketuk untuk membuka info —
            // kebiasaan iOS Messages, dan sekaligus membuat pengaturan bisa
            // ditemukan tanpa ikon gerigi tambahan di pojok.
            Row(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onBukaInfo)
                    .padding(vertical = 4.dp, horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFD8E7FB)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (fotoGrup != null) {
                        AsyncImage(
                            model = AvatarStorage.url(fotoGrup),
                            imageLoader = AvatarStorage.imageLoader(LocalContext.current),
                            contentDescription = "Foto grup $judul",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(36.dp),
                        )
                    } else {
                        Icon(Icons.Filled.Groups, null, tint = BiruIos, modifier = Modifier.size(21.dp))
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        judul,
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TeksUtama,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        subtitle,
                        fontSize = 11.5.sp,
                        color = if (subtitleAktif) BiruIos else TeksSekunder,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    Icons.Filled.ChevronRight,
                    null,
                    tint = Color(0xFFC7C7CC),
                    modifier = Modifier.size(20.dp),
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
    reaksi: List<ReaksiPesan>,
    userId: String,
    onBalas: (PesanChat) -> Unit,
    onTekanLama: (PesanChat) -> Unit,
    onLompatKe: (String) -> Unit,
    onKetukReaksi: (PesanChat) -> Unit,
    onKlikPengirim: (PesanChat) -> Unit,
    onKlikSebutan: (Sebutan) -> Unit,
    onKlikFoto: (PesanChat) -> Unit,
    disorot: Boolean,
) {
    val p = item.pesan
    // Nisan tidak bisa dibalas, ditanggapi, disalin, atau disunting — tidak ada
    // lagi isinya untuk diperlakukan begitu. Gestur dan menunya dimatikan di
    // sini, bukan disembunyikan satu per satu di dalam menu.
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
                    // Hanya seret ke kanan — gestur balas WhatsApp.
                    geser = (geser + dragAmount).coerceIn(0f, ambang * 1.3f)
                    if (geser >= ambang && !sudahHaptic) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        sudahHaptic = true
                    }
                }
            }),
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
                // Avatar dipasang di bubble TERAKHIR rentetan, sejajar dengan
                // ekornya — bukan di bubble pertama seperti sebelumnya. Ekor dan
                // wajah harus menunjuk satu sama lain; kalau terpisah, lengkungan
                // itu justru terlihat menuding ruang kosong.
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
                Bubble(
                    item = item,
                    onLompatKe = onLompatKe,
                    onKlikFoto = { onKlikFoto(p) },
                    onKlikSebutan = onKlikSebutan,
                    // Teks yang memuat sebutan menangani ketukannya sendiri,
                    // sehingga tekan-lama di atasnya tidak lagi sampai ke
                    // `combinedClickable` di bawah ini. Jalur itu karena itu
                    // diteruskan langsung ke bubble.
                    onTekanLama = { if (!terhapus) onTekanLama(p) },
                    disorot = disorot,
                    modifier = if (terhapus) Modifier else Modifier.combinedClickable(
                        onClick = {},
                        onLongClick = { onTekanLama(p) },
                    ),
                )
                if (reaksi.isNotEmpty() && !terhapus) {
                    KepingReaksi(
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
 * Keping reaksi di bawah bubble: satu keping per emoji dengan jumlahnya, dan
 * milik sendiri diberi lingkar biru supaya terlihat mana yang sudah dipilih.
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

@Composable
private fun Bubble(
    item: ItemChat.Bubble,
    onLompatKe: (String) -> Unit,
    onKlikFoto: () -> Unit = {},
    onKlikSebutan: (Sebutan) -> Unit = {},
    onTekanLama: () -> Unit = {},
    disorot: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val p = item.pesan
    val warnaBubble = if (item.milikSendiri) BubbleSendiri else BubbleLawan
    val warnaTeks = if (item.milikSendiri) Color.White else TeksUtama

    // Cincin oranye merek, bukan perubahan warna latar: gelembung sendiri sudah
    // biru pekat dan gelembung lawan abu muda, jadi satu warna latar yang sama
    // mustahil terlihat jelas di keduanya. Cincin di tepinya terbaca di atas
    // warna apa pun, dan memudar sendiri saat sorotannya padam.
    val bentuk = BentukGelembung(item.milikSendiri, item.posisi)
    // Animasinya HANYA dialokasikan untuk gelembung yang benar-benar disorot.
    // Versi sebelumnya memanggil animateColorAsState di SETIAP gelembung, jadi
    // setiap kali daftar melompat, belasan Animatable dan LaunchedEffect lahir
    // sekaligus hanya untuk menganimasikan warna transparan ke transparan.
    val denyut = if (disorot) denyutSorot() else null

    Column(
        modifier
            .widthIn(max = 290.dp + EKOR)
            // Denyutnya dibaca DI DALAM lambda gambar dan lapisan, bukan saat
            // penyusunan. Compose karena itu hanya menggambar ulang satu
            // gelembung tiap frame — tanpa menyusun ulang apa pun dan tanpa
            // mengukur ulang daftarnya. Itulah yang membuat efek sekaya ini
            // tetap gratis di HP lemah.
            .then(
                if (denyut == null) Modifier else Modifier.graphicsLayer {
                    val skala = 1f + 0.04f * denyut.value
                    scaleX = skala
                    scaleY = skala
                }
            )
            .clip(bentuk)
            .background(warnaBubble)
            .then(
                if (denyut == null) Modifier else Modifier.drawWithContent {
                    drawContent()
                    val maju = denyut.value
                    if (maju <= 0f) return@drawWithContent
                    val garis = bentuk.createOutline(size, layoutDirection, this)
                    // Kilau tipis di dalam, cincin tegas di tepi: yang pertama
                    // membuat gelembungnya terasa menyala, yang kedua membuat
                    // batasnya jelas di atas latar terang maupun gelap.
                    drawOutline(garis, Oranye, alpha = 0.14f * maju)
                    drawOutline(
                        garis,
                        Oranye,
                        alpha = maju,
                        style = Stroke(width = 2.5.dp.toPx()),
                    )
                }
            )
            // Ruang ekor ditambahkan ke sisi pengirim supaya isi pesan berhenti
            // tepat di tepi badan, bukan menindih lengkungan ekornya.
            .padding(
                start = if (item.milikSendiri) 12.dp else 12.dp + EKOR,
                end = if (item.milikSendiri) 12.dp + EKOR else 12.dp,
                top = 6.dp,
                bottom = 6.dp,
            ),
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

        // NISAN, sebagai CABANG — bukan `return` dari dalam lambda ini.
        //
        // Compose membungkus badan lambda composable dengan pasangan
        // startGroup/endGroup. Keluar lebih awal dari tengahnya membuat
        // endGroup-nya terlewat dan tumpukan komposernya rusak; gejalanya bukan
        // salah gambar melainkan IndexOutOfBoundsException di Stack.pop, satu
        // frame kemudian, jauh dari tempat kesalahannya.
        if (p.deletedAtMs != null) {
            NisanPesan(jam = item.jam, diBubbleSendiri = item.milikSendiri)
        } else {
            if (p.replyToId != null || p.replyToSnippet != null) {
                KutipanReply(
                    nama = p.replyToName ?: "Pesan",
                    snippet = p.replyToSnippet.orEmpty(),
                    fotoPath = p.replyToImage,
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
                        .background(if (item.milikSendiri) Color(0x33FFFFFF) else Color(0x11000000))
                        .clickable(onClick = onKlikFoto),
                )
            }

            Row(verticalAlignment = Alignment.Bottom) {
                if (p.body.isNotBlank()) {
                    TeksBubble(
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
                        color = if (item.milikSendiri) Color(0xB3FFFFFF) else TeksSekunder,
                        modifier = Modifier.padding(end = 5.dp, top = 2.dp),
                    )
                }
                Text(
                    item.jam,
                    fontSize = 10.5.sp,
                    color = if (item.milikSendiri) Color(0xB3FFFFFF) else TeksSekunder,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

/**
 * Yang tersisa dari pesan yang dihapus: keterangan bahwa ia pernah ada.
 *
 * Kutipan, foto, sebutan, dan penanda "diedit" tidak ikut digambar — tidak ada
 * lagi isinya untuk dirujuk.
 */
@Composable
private fun NisanPesan(jam: String, diBubbleSendiri: Boolean) {
    val warna = if (diBubbleSendiri) Color(0xCCFFFFFF) else TeksSekunder
    val warnaJam = if (diBubbleSendiri) Color(0xB3FFFFFF) else TeksSekunder
    Row(verticalAlignment = Alignment.Bottom) {
        Icon(
            Icons.Filled.Block,
            null,
            tint = warnaJam,
            modifier = Modifier.padding(end = 5.dp, bottom = 2.dp).size(14.dp),
        )
        Text(
            "Pesan ini telah dihapus",
            fontSize = 15.sp,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
            color = warna,
            modifier = Modifier.weight(1f, fill = false),
        )
        Spacer(Modifier.width(6.dp))
        Text(jam, fontSize = 10.5.sp, color = warnaJam, modifier = Modifier.padding(top = 2.dp))
    }
}

/**
 * Isi pesan, dengan @sebutan disorot dan bisa diketuk.
 *
 * Ketukan dan tekan-lama ditangani SATU pendeteksi gestur di sini. Kalau
 * ketukannya dipasang sendiri, ia akan menelan tekan-lama milik baris di
 * atasnya — dan menu pesan berhenti muncul justru di bagian bubble yang paling
 * sering ditekan orang, yaitu teksnya.
 */
@Composable
private fun TeksBubble(
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
        Text(body, fontSize = 16.sp, color = warnaTeks, modifier = modifier)
        return
    }

    // Di gelembung sendiri yang biru pekat, biru merek justru menghilang; yang
    // terbaca di sana adalah putih tebal bergaris bawah.
    val warnaSebutan = if (diBubbleSendiri) Color.White else BiruIos
    val teks = remember(body, rentang, diBubbleSendiri) {
        buildAnnotatedString {
            append(body)
            rentang.forEach { (r, _) ->
                addStyle(
                    SpanStyle(
                        color = warnaSebutan,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (diBubbleSendiri) TextDecoration.Underline else null,
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
        fontSize = 16.sp,
        color = warnaTeks,
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

@Composable
private fun KutipanReply(
    nama: String,
    snippet: String,
    fotoPath: String?,
    diBubbleSendiri: Boolean,
    modifier: Modifier = Modifier,
) {
    val latar = if (diBubbleSendiri) Color(0x2EFFFFFF) else Color(0x0F000000)
    val warnaBar = if (diBubbleSendiri) Color.White else BiruIos
    val warnaJudul = if (diBubbleSendiri) Color.White else BiruIos
    val warnaIsi = if (diBubbleSendiri) Color(0xCCFFFFFF) else TeksSekunder
    Row(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(latar)
            .heightIn(min = 38.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(3.dp).heightIn(min = 38.dp).background(warnaBar))
        Column(Modifier.weight(1f, fill = false).padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text(
                nama,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = warnaJudul,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Balasan atas foto harus tetap terbaca sebagai foto walau
                // keterangannya panjang — karena itu ikonnya berdiri sendiri di
                // depan teks, bukan disisipkan ke dalam kalimatnya.
                if (fotoPath != null) {
                    Icon(
                        Icons.Filled.PhotoCamera,
                        null,
                        tint = warnaIsi,
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    snippet.ifBlank { if (fotoPath != null) "Foto" else "" },
                    fontSize = 12.sp,
                    color = warnaIsi,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        // Gambar kecilnya di ujung kanan, seperti kartu kutipan WhatsApp.
        if (fotoPath != null) {
            AsyncImage(
                model = ChatRepository.urlFoto(fotoPath),
                imageLoader = AvatarStorage.imageLoader(LocalContext.current),
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
                    .clip(BentukGelembung(milikSendiri = true, posisi = PosisiGrup.TUNGGAL))
                    .background(if (kiriman.gagal) BubbleSendiri.copy(alpha = 0.55f) else BubbleSendiri.copy(alpha = 0.8f))
                    .clickable { if (kiriman.gagal) menu = true }
                    .padding(start = 12.dp, end = 12.dp + EKOR, top = 6.dp, bottom = 6.dp),
            ) {
                kiriman.replyTo?.let {
                    KutipanReply(
                        nama = it.senderName,
                        snippet = snippetPesan(it.body, it.imagePath),
                        fotoPath = it.imagePath,
                        diBubbleSendiri = true,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                kiriman.fotoWebp?.let { bytes ->
                    val bmp = remember(kiriman.kunci) {
                        // Dikecilkan saat di-decode, bukan dimuat penuh. Berkasnya
                        // 1600 px sementara petak ini hanya 240 dp; membaca penuh
                        // berarti membongkar bitmap belasan megabyte DI MAIN
                        // THREAD tepat saat bubble kiriman muncul.
                        val opsi = BitmapFactory.Options().apply { inSampleSize = 4 }
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opsi)?.asImageBitmap()
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
private fun TombolKeSebutan(onClick: () -> Unit) {
    // Berdenyut pelan supaya mata menemukannya tanpa perlu berkedip keras.
    // Satu animasi float untuk satu tombol — jauh lebih murah daripada warna
    // atau bayangan yang beranimasi, dan dibaca di dalam `graphicsLayer`
    // sehingga hanya lapisan gambarnya yang diperbarui, bukan susunannya.
    val transisi = rememberInfiniteTransition(label = "denyutSebutan")
    val denyut by transisi.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "denyut",
    )
    Box(
        Modifier
            .graphicsLayer {
                val skala = 1f + 0.06f * denyut
                scaleX = skala
                scaleY = skala
            }
            .size(40.dp)
            .clip(CircleShape)
            .background(Oranye)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.AlternateEmail,
            "Lompat ke pesan yang menyebut Anda",
            tint = Color.White,
            modifier = Modifier.size(21.dp),
        )
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

/**
 * Pemegang isi kotak ketik.
 *
 * Ditandai [Stable] supaya Compose tahu perubahannya diberitahukan lewat state
 * di dalamnya, sehingga hanya composable yang benar-benar membaca [teks] yang
 * ikut disusun ulang saat mengetik.
 */
@Stable
class IsiKetikan {
    /**
     * Isi BESERTA posisi kursor.
     *
     * Kursornya ikut disimpan karena @sebutan hanya bisa dikenali dari kata
     * tempat kursor berada — dan sebagai keuntungan lanjutan, emoji kini
     * tersisip di tempat kursor, bukan selalu di ujung teks.
     */
    var nilai by mutableStateOf(TextFieldValue(""))

    /**
     * Orang yang sempat dipilih dari pemilih sebutan.
     *
     * Sengaja BUKAN state: tidak ada yang menggambarnya, isinya hanya dibaca
     * sekali saat kirim, dan menjadikannya state berarti setiap pilihan memicu
     * penyusunan ulang tanpa alasan. Yang tidak lagi ada di teks disaring
     * [sebutanTerpakai] saat itu juga.
     */
    var kandidat: List<Sebutan> = emptyList()

    var teks: String
        get() = nilai.text
        set(baru) {
            nilai = TextFieldValue(baru, TextRange(baru.length))
            if (baru.isEmpty()) kandidat = emptyList()
        }

    /** Menyisipkan [potongan] di posisi kursor dan menaruh kursor sesudahnya. */
    fun sisip(potongan: String) {
        val n = nilai
        val mulai = n.selection.min
        val akhir = n.selection.max
        val baru = n.text.substring(0, mulai) + potongan + n.text.substring(akhir)
        nilai = TextFieldValue(baru, TextRange(mulai + potongan.length))
    }
}

/**
 * Bilah penanda mode sunting. Warnanya oranye merek, bukan biru seperti kartu
 * balas, supaya kedua keadaan itu tidak pernah tertukar sekilas — keduanya
 * memakai kotak ketik yang sama dan hanya bilah ini yang membedakannya.
 */
@Composable
private fun KartuSuntingComposer(target: PesanChat, onTutup: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(LatarBar)
            .padding(start = 12.dp, end = 4.dp, top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(3.dp).height(36.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFFEA580C)))
        Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Edit,
                    null,
                    tint = Color(0xFFEA580C),
                    modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "Mengedit pesan",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFEA580C),
                    maxLines = 1,
                )
            }
            Text(
                snippetPesan(target.body, target.imagePath),
                fontSize = 12.5.sp, color = TeksSekunder, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onTutup) { Icon(Icons.Filled.Close, "Batal mengedit", tint = TeksSekunder) }
    }
}

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
    ketikan: IsiKetikan,
    anggota: List<AnggotaGrup>,
    userId: String,
    onMulaiMenyebut: () -> Unit,
    papanEmojiTerbuka: Boolean,
    sedangKompres: Boolean,
    onTeksBerubah: (String) -> Unit,
    onToggleEmoji: () -> Unit,
    onFokusIsian: () -> Unit,
    onKirim: () -> Unit,
    onLampiran: () -> Unit,
    modeSunting: Boolean = false,
) {
    val nilai = ketikan.nilai
    val teks = nilai.text

    // Pemilih sebutan dihitung DI SINI, bukan di badan layar: yang membacanya
    // adalah isi kotak ketik, dan membacanya di atas sana berarti setiap ketukan
    // tombol menyusun ulang header dan seluruh daftar pesan.
    val kueri = remember(nilai) {
        if (modeSunting) null else cariKueriSebutan(nilai.text, nilai.selection.start)
    }
    // Daftar anggota baru ditarik saat seseorang benar-benar mengetik '@';
    // membacanya lebih awal berarti satu permintaan jaringan untuk setiap orang
    // yang cuma ingin membaca chat.
    LaunchedEffect(kueri != null) { if (kueri != null) onMulaiMenyebut() }

    val saran = remember(kueri, anggota) {
        val k = kueri ?: return@remember emptyList()
        val cari = k.kueri.trim().lowercase()
        anggota.asSequence()
            .filter { it.id != userId }
            .filter { cari.isEmpty() || it.nama.lowercase().contains(cari) ||
                it.username?.lowercase()?.contains(cari) == true }
            .take(30)
            .toList()
    }

    Column(Modifier.fillMaxWidth().background(LatarBar)) {
        if (kueri != null && saran.isNotEmpty()) {
            PemilihSebutan(saran) { orang ->
                val (baru, kursor) = sisipkanSebutan(nilai.text, kueri, orang.nama)
                ketikan.nilai = TextFieldValue(baru, TextRange(kursor))
                ketikan.kandidat = ketikan.kandidat + Sebutan(orang.id, orang.nama)
            }
        }
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(GarisTipis))
        Row(
            Modifier.fillMaxWidth().padding(start = 2.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
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
                    .background(Color.White),
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Box(Modifier.weight(1f).padding(start = 12.dp, top = 8.dp, bottom = 8.dp)) {
                        if (teks.isEmpty()) {
                            Text("Ketik pesan…", fontSize = 16.sp, color = TeksSekunder)
                        }
                        BasicTextField(
                            value = nilai,
                            onValueChange = { ketikan.nilai = it; onTeksBerubah(it.text) },
                            textStyle = TextStyle(fontSize = 16.sp, color = TeksUtama),
                            cursorBrush = SolidColor(BiruIos),
                            maxLines = 5,
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { if (it.isFocused) onFokusIsian() },
                        )
                    }
                    // Tombol emoji di DALAM kolom ketik, seperti Telegram, dan
                    // berganti jadi ikon papan ketik saat papan emoji terbuka —
                    // satu tombol untuk bolak-balik, bukan dua.
                    Box(
                        Modifier
                            .padding(end = 4.dp, bottom = 2.dp)
                            .size(34.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onToggleEmoji),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            if (papanEmojiTerbuka) Icons.Filled.Keyboard else Icons.Filled.EmojiEmotions,
                            if (papanEmojiTerbuka) "Tutup papan emoji" else "Buka papan emoji",
                            tint = if (papanEmojiTerbuka) BiruIos else TeksSekunder,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.width(6.dp))
            val bisaKirim = teks.isNotBlank()
            Box(
                Modifier
                    .padding(bottom = 6.dp)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            !bisaKirim -> Color(0xFFC7C7CC)
                            modeSunting -> Color(0xFFEA580C)
                            else -> BiruIos
                        }
                    )
                    .clickable(enabled = bisaKirim, onClick = onKirim),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (modeSunting) Icons.Filled.Check else Icons.Filled.ArrowUpward,
                    if (modeSunting) "Simpan suntingan" else "Kirim",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/**
 * Daftar nama yang muncul saat mengetik '@'.
 *
 * Dibatasi tingginya dan digulir sendiri: di grup se-perusahaan, '@' yang
 * ditekan tanpa huruf apa pun akan menyodorkan ratusan nama, dan panel yang
 * tumbuh sebebasnya akan menelan seluruh layar percakapan.
 */
@Composable
private fun PemilihSebutan(saran: List<AnggotaGrup>, onPilih: (AnggotaGrup) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(GarisTipis))
        LazyColumn(
            Modifier.fillMaxWidth().heightIn(max = 208.dp).background(LatarBar),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp),
        ) {
            items(count = saran.size, key = { saran[it].id }, contentType = { "sebutan" }) { i ->
                val orang = saran[i]
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onPilih(orang) }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AvatarStaf(
                        path = orang.avatar,
                        nama = orang.nama,
                        modifier = Modifier.size(32.dp).clip(CircleShape),
                        ukuranHuruf = 13.sp,
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            orang.nama,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = TeksUtama,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val bawah = orang.username?.let { "@" + it } ?: orang.outlet
                        if (!bawah.isNullOrBlank()) {
                            Text(
                                bawah,
                                fontSize = 12.sp,
                                color = TeksSekunder,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
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
