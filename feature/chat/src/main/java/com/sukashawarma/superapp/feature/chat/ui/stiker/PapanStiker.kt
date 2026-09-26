package com.sukashawarma.superapp.feature.chat.ui.stiker

import android.widget.Toast
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.sukashawarma.superapp.feature.chat.data.FavoritStiker
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.repeatCount
import com.sukashawarma.superapp.feature.chat.data.HalamanStiker
import com.sukashawarma.superapp.feature.chat.data.KlipyStiker
import com.sukashawarma.superapp.feature.chat.data.StikerKlipy
import com.sukashawarma.superapp.feature.chat.data.StikerMedia
import com.sukashawarma.superapp.feature.chat.ui.emoji.PapanEmoji
import com.sukashawarma.superapp.feature.chat.ui.emoji.TINGGI_PAPAN_EMOJI_DP
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private val BiruIos = Color(0xFF007AFF)
private val AbuIkon = Color(0xFF8E8E93)
private val LatarPapan = Color(0xFFF7F7F8)
private val GarisPapan = Color(0x1F3C3C43)
private val LatarPetak = Color(0x0F000000)

/**
 * Papan pengganti papan ketik dengan dua tab: Emoji (papan lama, tidak diubah) dan
 * Stiker (KLIPY). Tinggi keduanya sama supaya daftar pesan tidak melompat saat
 * berpindah tab.
 */
@Composable
fun PapanEmojiStiker(
    userId: String,
    onPilihEmoji: (String) -> Unit,
    onHapus: () -> Unit,
    onPilihStiker: (StikerKlipy) -> Unit,
    modifier: Modifier = Modifier,
) {
    var tabStiker by rememberSaveable { mutableStateOf(false) }
    Column(modifier.fillMaxWidth().background(LatarPapan)) {
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(GarisPapan))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            Row(
                Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .background(Color(0x14767680))
                    .padding(2.dp),
            ) {
                TombolTab("Emoji", terpilih = !tabStiker) { tabStiker = false }
                TombolTab("Stiker", terpilih = tabStiker) { tabStiker = true }
            }
        }
        if (tabStiker) {
            PanelStiker(userId = userId, onPilih = onPilihStiker)
        } else {
            PapanEmoji(onPilih = onPilihEmoji, onHapus = onHapus)
        }
    }
}

@Composable
private fun TombolTab(label: String, terpilih: Boolean, onKlik: () -> Unit) {
    Box(
        Modifier
            .width(84.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(if (terpilih) Color.White else Color.Transparent)
            .clickable(onClick = onKlik)
            .padding(vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = if (terpilih) FontWeight.SemiBold else FontWeight.Normal,
            color = if (terpilih) Color.Black else AbuIkon,
        )
    }
}

/**
 * Halaman pertama trending disimpan per proses selama [UMUR_CACHE_MS]: papan ini
 * dibuka-tutup berkali-kali dalam satu obrolan, dan tiap pembukaan tidak perlu
 * memanggil API lagi.
 */
private object CacheTrending {
    private const val UMUR_CACHE_MS = 10 * 60 * 1000L
    private var isi: HalamanStiker? = null
    private var waktuMs = 0L

    fun ambil(): HalamanStiker? = isi?.takeIf { System.currentTimeMillis() - waktuMs < UMUR_CACHE_MS }
    fun simpan(h: HalamanStiker) { isi = h; waktuMs = System.currentTimeMillis() }
}

@Composable
private fun PanelStiker(userId: String, onPilih: (StikerKlipy) -> Unit) {
    // Cabang, bukan `return` dari dalam lambda Box: keluar di tengah lambda composable
    // melewatkan endGroup dan merusak tumpukan komposer (lihat catatan NISAN di ChatScreen).
    Box(Modifier.fillMaxWidth().height(TINGGI_PAPAN_EMOJI_DP.dp)) {
        if (KlipyStiker.aktif) {
            IsiPanelStiker(userId, onPilih)
        } else {
            Keterangan("Stiker belum dikonfigurasi.\nHubungi developer untuk mengaktifkannya.")
        }
    }
}

@Composable
private fun IsiPanelStiker(userId: String, onPilih: (StikerKlipy) -> Unit) {
    val konteks = LocalContext.current
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(userId) { FavoritStiker.muat(konteks, userId) }
    val favorit by FavoritStiker.isi.collectAsState()
    val urlFavorit = remember(favorit) { favorit.mapTo(HashSet()) { it.urlKirim } }
    var tabFavorit by rememberSaveable { mutableStateOf(false) }
    val gridFavorit = rememberLazyGridState()

    // Tekan-lama di petak mana pun: simpan/hapus favorit, dengan getar + toast supaya
    // jelas terjadi sesuatu (petak Trending hanya berubah sebatas tanda bintang kecil).
    fun alihkanFavorit(s: StikerKlipy) {
        val kiniFavorit = FavoritStiker.alihkan(konteks, userId, s)
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        Toast.makeText(
            konteks,
            if (kiniFavorit) "Disimpan ke favorit" else "Dihapus dari favorit",
            Toast.LENGTH_SHORT,
        ).show()
    }
    var kueri by rememberSaveable { mutableStateOf("") }
    // Kueri yang benar-benar dimuat — tertinggal dari [kueri] selama jeda ketik.
    var kueriAktif by rememberSaveable { mutableStateOf("") }
    var isi by remember { mutableStateOf<List<StikerKlipy>>(emptyList()) }
    var halaman by remember { mutableIntStateOf(1) }
    var adaLagi by remember { mutableStateOf(false) }
    var memuat by remember { mutableStateOf(false) }
    var galat by remember { mutableStateOf<String?>(null) }
    // Menandai permintaan terbaru: hasil permintaan lama yang datang terlambat
    // (pengguna sudah mengetik kueri lain) dibuang, tidak menimpa kisi.
    var generasi by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val grid = rememberLazyGridState()

    fun muat(hal: Int) {
        val gen = if (hal == 1) ++generasi else generasi
        val q = kueriAktif
        memuat = true
        galat = null
        scope.launch {
            val hasil = runCatching {
                if (q.isBlank()) {
                    if (hal == 1) CacheTrending.ambil() ?: KlipyStiker.trending(1, userId).also(CacheTrending::simpan)
                    else KlipyStiker.trending(hal, userId)
                } else {
                    KlipyStiker.cari(q, hal, userId)
                }
            }
            if (gen != generasi) return@launch
            memuat = false
            hasil.onSuccess { h ->
                // Stiker yang sama bisa muncul lagi di halaman berikutnya; kunci
                // LazyGrid wajib unik, jadi duplikat dibuang.
                isi = if (hal == 1) h.isi.distinctBy { it.urlKirim }
                else (isi + h.isi).distinctBy { it.urlKirim }
                halaman = hal
                adaLagi = h.adaLagi && h.isi.isNotEmpty()
            }.onFailure {
                if (hal == 1) isi = emptyList()
                galat = "Stiker gagal dimuat. Periksa koneksi lalu coba lagi."
            }
        }
    }

    // Jeda ketik: pencarian baru dikirim setelah pengguna berhenti mengetik,
    // supaya satu kata tidak menjadi lima panggilan API.
    LaunchedEffect(kueri) {
        val bersih = kueri.trim()
        if (bersih == kueriAktif) return@LaunchedEffect
        if (bersih.isNotEmpty() && bersih.length < 2) return@LaunchedEffect
        delay(450)
        kueriAktif = bersih
    }
    LaunchedEffect(kueriAktif) {
        muat(1)
        // Hanya bila kisi sudah tampil. scrollToItem pada kisi yang belum pernah
        // di-layout MENUNGGU layout pertamanya — saat daftar masih kosong kisinya
        // tidak disusun sama sekali, jadi baris ini dulu menggantung selamanya dan
        // muat(1) di bawahnya tidak pernah jalan (tab Stiker kosong terus).
        if (grid.layoutInfo.totalItemsCount > 0) grid.scrollToItem(0)
    }
    // Muat halaman berikutnya saat enam petak terakhir mulai terlihat.
    val perluLagi by remember {
        derivedStateOf {
            val terakhir = grid.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            adaLagi && !memuat && terakhir >= isi.size - 6
        }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { perluLagi }.distinctUntilChanged().collect { if (it) muat(halaman + 1) }
    }

    Column(Modifier.fillMaxSize()) {
        KolomCari(kueri = kueri, onUbah = {
            kueri = it.take(50)
            // Mengetik selalu berarti mencari di KLIPY, bukan di daftar favorit.
            if (it.isNotBlank()) tabFavorit = false
        })
        if (kueri.isBlank()) {
            Row(
                Modifier.padding(start = 10.dp, top = 2.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ChipSumber("Trending", terpilih = !tabFavorit) { tabFavorit = false }
                ChipSumber("★ Favorit", terpilih = tabFavorit) { tabFavorit = true }
            }
        }
        Box(Modifier.fillMaxWidth().weight(1f)) {
            when {
                tabFavorit && favorit.isEmpty() -> Keterangan(
                    "Belum ada stiker favorit.\nTekan lama sebuah stiker untuk menyimpannya.",
                )
                tabFavorit -> KisiStiker(
                    daftar = favorit,
                    state = gridFavorit,
                    urlFavorit = urlFavorit,
                    onPilih = onPilih,
                    onTekanLama = ::alihkanFavorit,
                )
                isi.isEmpty() && memuat -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(24.dp), color = BiruIos, strokeWidth = 2.dp)
                }
                isi.isEmpty() && galat != null -> Keterangan(galat!!, onCobaLagi = { muat(1) })
                isi.isEmpty() && !memuat -> Keterangan(
                    if (kueriAktif.isBlank()) "Belum ada stiker." else "Tidak ada stiker untuk \"$kueriAktif\".",
                )
                else -> KisiStiker(
                    daftar = isi,
                    state = grid,
                    urlFavorit = urlFavorit,
                    onPilih = onPilih,
                    onTekanLama = ::alihkanFavorit,
                )
            }
        }
        Text(
            "Stiker oleh KLIPY",
            fontSize = 10.sp,
            color = AbuIkon,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth().padding(end = 10.dp, bottom = 3.dp),
        )
    }
}

@Composable
private fun KolomCari(kueri: String, onUbah: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 10.dp, end = 10.dp, top = 2.dp, bottom = 2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x1F767680))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Search, null, tint = AbuIkon, modifier = Modifier.size(17.dp))
        Box(Modifier.padding(start = 6.dp).weight(1f)) {
            if (kueri.isEmpty()) Text("Cari stiker", fontSize = 15.sp, color = AbuIkon)
            BasicTextField(
                value = kueri,
                onValueChange = onUbah,
                singleLine = true,
                textStyle = TextStyle(fontSize = 15.sp, color = Color.Black),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ChipSumber(label: String, terpilih: Boolean, onKlik: () -> Unit) {
    Text(
        label,
        fontSize = 12.5.sp,
        fontWeight = if (terpilih) FontWeight.SemiBold else FontWeight.Normal,
        color = if (terpilih) Color.White else AbuIkon,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (terpilih) BiruIos else Color(0x14767680))
            .clickable(onClick = onKlik)
            .padding(horizontal = 12.dp, vertical = 4.dp),
    )
}

@Composable
private fun KisiStiker(
    daftar: List<StikerKlipy>,
    state: LazyGridState,
    urlFavorit: Set<String>,
    onPilih: (StikerKlipy) -> Unit,
    onTekanLama: (StikerKlipy) -> Unit,
) {
    LazyVerticalGrid(
        state = state,
        columns = GridCells.Fixed(4),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Kunci urlKirim, bukan slug: favorit yang disimpan dari pesan tidak punya slug
        // asli, dan urlKirim unik untuk setiap stiker.
        items(daftar, key = { it.urlKirim }, contentType = { "stiker" }) { s ->
            PetakStiker(
                stiker = s,
                favorit = s.urlKirim in urlFavorit,
                onKlik = { onPilih(s) },
                onTekanLama = { onTekanLama(s) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PetakStiker(
    stiker: StikerKlipy,
    favorit: Boolean,
    onKlik: () -> Unit,
    onTekanLama: () -> Unit,
) {
    val konteks = LocalContext.current
    // Diminta seukuran petak (bukan ukuran asli berkas) supaya bitmap di memori kecil.
    val sisiPx = with(LocalDensity.current) { 80.dp.roundToPx() }
    val permintaan = remember(stiker.urlPratinjau) {
        ImageRequest.Builder(konteks)
            .data(stiker.urlPratinjau)
            .size(sisiPx)
            .build()
    }
    Box(
        Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(LatarPetak)
            .combinedClickable(onClick = onKlik, onLongClick = onTekanLama),
    ) {
        AsyncImage(
            model = permintaan,
            imageLoader = StikerMedia.imageLoader(konteks),
            contentDescription = stiker.judul.ifBlank { "Stiker" },
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().padding(4.dp),
        )
        if (favorit) {
            Text(
                "★",
                fontSize = 11.sp,
                color = Color(0xFFFFB400),
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 2.dp, end = 4.dp),
            )
        }
    }
}

@Composable
private fun Keterangan(teks: String, onCobaLagi: (() -> Unit)? = null) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(teks, fontSize = 14.sp, color = AbuIkon, textAlign = TextAlign.Center)
        if (onCobaLagi != null) {
            Text(
                "Coba lagi",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = BiruIos,
                modifier = Modifier.padding(top = 10.dp).clickable(onClick = onCobaLagi),
            )
        }
    }
}

/**
 * Stiker di dalam daftar pesan: tanpa bubble, seperti WhatsApp.
 *
 * Animasi diputar tiga kali lalu berhenti (hemat baterai di obrolan yang ramai);
 * ketuk stiker untuk memantulkan dan memutarnya lagi. Karena berkas sudah ada di
 * cache memori/disk, memutar ulang tidak mengunduh lagi.
 *
 * Ketuk DAN tekan-lama ditangani di sini dalam satu `combinedClickable`: bila
 * pemanggil memasang penangan tekan-lama sendiri di luar, ketukan di dalam akan
 * menelan gesturnya dan menu pesan tidak pernah terbuka.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BubbleStiker(
    url: String,
    modifier: Modifier = Modifier,
    ukuran: Dp = 140.dp,
    onTekanLama: () -> Unit = {},
) {
    val konteks = LocalContext.current
    val sisiPx = with(LocalDensity.current) { ukuran.roundToPx() }
    var putaran by remember { mutableIntStateOf(0) }
    val pantul = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    // `putaran` masuk ke kunci memori, jadi setiap ketukan menghasilkan drawable baru
    // yang mulai dari bingkai pertama — tanpa itu Coil memberi drawable yang sama,
    // yang sudah selesai berputar. Kunci disk tetap URL: berkasnya tidak diunduh ulang.
    val permintaan = remember(url, putaran) {
        ImageRequest.Builder(konteks)
            .data(url)
            .size(sisiPx)
            .repeatCount(2)
            .memoryCacheKey(if (putaran == 0) url else "$url#$putaran")
            .diskCacheKey(url)
            .build()
    }
    AsyncImage(
        model = permintaan,
        imageLoader = StikerMedia.imageLoader(konteks),
        contentDescription = "Stiker",
        contentScale = ContentScale.Fit,
        modifier = modifier
            .size(ukuran)
            .graphicsLayer {
                scaleX = pantul.value
                scaleY = pantul.value
            }
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onLongClick = onTekanLama,
                onClick = {
                    putaran++
                    scope.launch {
                        pantul.snapTo(0.85f)
                        pantul.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMediumLow))
                    }
                },
            ),
    )
}

/**
 * Kerangka satu pesan stiker, dipakai grup, area, dan chat pribadi: [kepala] untuk
 * nama/kutipan balasan, lalu stikernya, lalu kapsul jam + [status] (centang). Kapsul
 * berlatar tipis supaya jam tetap terbaca di atas wallpaper apa pun, karena stiker
 * tidak punya bubble yang menampung jamnya.
 */
@Composable
fun TataPesanStiker(
    url: String,
    milikSendiri: Boolean,
    jam: String,
    modifier: Modifier = Modifier,
    kepala: (@Composable () -> Unit)? = null,
    status: (@Composable () -> Unit)? = null,
    onTekanLama: () -> Unit = {},
) {
    Column(modifier, horizontalAlignment = if (milikSendiri) Alignment.End else Alignment.Start) {
        kepala?.invoke()
        BubbleStiker(url = url, onTekanLama = onTekanLama)
        Row(
            Modifier
                .padding(top = 2.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0x99FFFFFF))
                .padding(horizontal = 6.dp, vertical = 1.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(jam, fontSize = 10.5.sp, color = Color(0xFF6B6B70))
            if (status != null) {
                Box(Modifier.padding(start = 3.dp)) { status() }
            }
        }
    }
}
