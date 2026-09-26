package com.sukashawarma.superapp.feature.chat.ui.developer

import com.sukashawarma.superapp.core.ui.ios.KeadaanIos
import com.sukashawarma.superapp.core.ui.ios.KolomCariIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.TombolBundarIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sukashawarma.superapp.core.ui.AvatarStorage
import com.sukashawarma.superapp.domain.util.JakartaTime
import com.sukashawarma.superapp.feature.chat.data.PesanPribadi
import com.sukashawarma.superapp.feature.chat.data.PrivateChatRepository
import com.sukashawarma.superapp.feature.chat.domain.formatJamWib
import com.sukashawarma.superapp.feature.chat.domain.labelTanggal
import com.sukashawarma.superapp.core.ui.PenampilFoto
import com.sukashawarma.superapp.feature.chat.ui.pribadi.KomponenCentangPribadi
import com.sukashawarma.superapp.feature.chat.ui.suara.BubbleSuara
import com.sukashawarma.superapp.feature.chat.ui.suara.PemutarSuara
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val WarnaWallpaper = Color(0xFFEFE7DE)
private val WarnaTeksUtama = WarnaIos.Label
private val WarnaTeksKedua = WarnaIos.Abu
private val WarnaBiru = WarnaIos.Biru

/** Satu baris pada daftar: pemisah tanggal atau bubble pesan. */
private sealed interface ItemPantau {
    data class Pemisah(val label: String) : ItemPantau
    data class Bubble(val pesan: PesanPribadi) : ItemPantau
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayarDetailPantauDev(
    userAId: String,
    userBId: String,
    userAName: String,
    userBName: String,
    onBack: () -> Unit,
    viewModel: DeveloperChatDetailViewModel = remember(userAId, userBId) {
        DeveloperChatDetailViewModel(
            userAId = userAId,
            userBId = userBId,
            userAName = userAName,
            userBName = userBName
        )
    }
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    var fotoDibuka by remember { mutableStateOf<Pair<String, String>?>(null) }
    var cariAktif by remember { mutableStateOf(false) }
    var kueri by remember { mutableStateOf("") }
    // Pesan suara di mode pantau baru boleh berbunyi setelah developer sadar
    // bahwa suaranya akan terdengar keras di ruangan tempat ia berada.
    var izinDengar by remember { mutableStateOf(false) }
    var tanyaDengar by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { PemutarSuara.hentikan() }
    }

    val hasilFilter = remember(state.pesanList, kueri) {
        val q = kueri.trim().lowercase(Locale.getDefault())
        if (q.isBlank()) state.pesanList else state.pesanList.filter { it.cocokDengan(q) }
    }

    val baris = remember(hasilFilter) { susunBarisPantau(hasilFilter) }

    // Saat tidak mencari, buka percakapan pada pesan terbaru seperti layar chat biasa.
    LaunchedEffect(baris.size, kueri.isBlank()) {
        if (kueri.isBlank() && baris.isNotEmpty()) {
            listState.animateScrollToItem(baris.size - 1)
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "$userAName & $userBName",
                                style = TipeIos.Utama,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "🕵️ Mode Pantau Siluman (Read-Only)",
                                style = TipeIos.Kecil,
                                color = NadaIos.PERINGATAN.teks,
                                fontWeight = FontWeight.Medium
                            )
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
                        TombolBundarIos(
                            if (cariAktif) IkonIos.Close else IkonIos.Search,
                            if (cariAktif) "Tutup pencarian" else "Cari pesan",
                            {
                                cariAktif = !cariAktif
                                if (!cariAktif) kueri = ""
                            },
                            Modifier.padding(end = 12.dp),
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = WarnaIos.Kartu)
                )

                AnimatedVisibility(visible = cariAktif) {
                    Column(modifier = Modifier.background(WarnaIos.Kartu)) {
                        KolomPencarianPesan(
                            kueri = kueri,
                            onKueriBerubah = { kueri = it },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        if (kueri.isNotBlank()) {
                            Text(
                                text = "${hasilFilter.size} pesan cocok dari ${state.pesanList.size}",
                                style = TipeIos.Kecil,
                                modifier = Modifier.padding(start = 18.dp, bottom = 8.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = WarnaIos.Pemisah, thickness = 0.5.dp)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(WarnaWallpaper)
        ) {
            SpandukSiluman()

            Box(modifier = Modifier.weight(1f)) {
                when {
                    state.memuat && state.pesanList.isEmpty() -> KotakTengah {
                        CircularProgressIndicator(color = WarnaBiru)
                    }

                    state.pesanList.isEmpty() -> KotakTengah {
                        KeadaanIos(
                            ikon = IkonIos.Inbox,
                            judul = "Belum ada pesan",
                            pesan = "Tidak ada pesan aktif antara kedua user.",
                        )
                    }

                    hasilFilter.isEmpty() -> KotakTengah {
                        KeadaanIos(
                            ikon = IkonIos.Search,
                            judul = "Tidak ditemukan",
                            pesan = "Tidak ada pesan yang memuat \"${kueri.trim()}\".",
                        )
                    }

                    else -> LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        items(
                            items = baris,
                            key = { item ->
                                when (item) {
                                    is ItemPantau.Pemisah -> "sep_${item.label}"
                                    is ItemPantau.Bubble -> item.pesan.id
                                }
                            }
                        ) { item ->
                            when (item) {
                                is ItemPantau.Pemisah -> PemisahTanggal(item.label)
                                is ItemPantau.Bubble -> {
                                    val isSenderA = item.pesan.senderId == userAId
                                    val senderLabel = if (isSenderA) userAName else userBName
                                    BubblePantauSiluman(
                                        pesan = item.pesan,
                                        isSenderA = isSenderA,
                                        labelPengirim = senderLabel,
                                        bolehDengar = izinDengar,
                                        onButuhIzinDengar = { tanyaDengar = true },
                                        onKlikFoto = { url -> fotoDibuka = Pair(url, senderLabel) }
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                            }
                        }
                    }
                }
            }

            if (tanyaDengar) {
                AlertDialog(
                    onDismissRequest = { tanyaDengar = false },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = null,
                            tint = WarnaIos.Oranye
                        )
                    },
                    title = { Text("Putar pesan suara?") },
                    text = {
                        Text(
                            "Rekaman akan berbunyi lewat speaker perangkat ini dan bisa terdengar " +
                                "orang di sekitar Anda. Pakai earphone lebih dulu. " +
                                "Memutarnya tetap siluman: centang biru dan status tersampaikan " +
                                "milik kedua staf tidak berubah.",
                            fontSize = 13.sp
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            izinDengar = true
                            tanyaDengar = false
                        }) {
                            Text("Saya pakai earphone", color = WarnaIos.Aksen, fontWeight = FontWeight.SemiBold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { tanyaDengar = false }) { Text("Batal", color = WarnaIos.Aksen) }
                    }
                )
            }

            fotoDibuka?.let { (url, judul) ->
                PenampilFoto(
                    url = url,
                    judul = judul,
                    keterangan = "Mode Pantau Developer",
                    onTutup = { fotoDibuka = null }
                )
            }
        }
    }
}

private fun PesanPribadi.cocokDengan(q: String): Boolean {
    fun String?.punya() = this?.lowercase(Locale.getDefault())?.contains(q) == true
    return body.punya() || replyToSnippet.punya() || replyToName.punya()
}

/** Sisipkan pemisah tanggal di antara pesan, seperti layar chat biasa. */
private fun susunBarisPantau(pesan: List<PesanPribadi>): List<ItemPantau> {
    val hariIni = LocalDate.now(JakartaTime.ZONE)
    val hasil = mutableListOf<ItemPantau>()
    var hariSebelumnya: LocalDate? = null
    pesan.forEach { p ->
        val hari = runCatching {
            Instant.ofEpochMilli(p.createdAtMs).atZone(JakartaTime.ZONE).toLocalDate()
        }.getOrNull()
        if (hari != null && hari != hariSebelumnya) {
            hasil += ItemPantau.Pemisah(labelTanggal(hari, hariIni))
            hariSebelumnya = hari
        }
        hasil += ItemPantau.Bubble(p)
    }
    return hasil
}

@Composable
private fun SpandukSiluman() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(WarnaIos.Label)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = IkonIos.Visibility,
            contentDescription = null,
            tint = WarnaIos.Kuning,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "Akses siluman — centang biru user tidak berubah.",
            style = TipeIos.Kecil,
            color = Color(0xFFAEAEB2)
        )
    }
}

@Composable
private fun KolomPencarianPesan(
    kueri: String,
    onKueriBerubah: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboard = LocalSoftwareKeyboardController.current

    KolomCariIos(
        nilai = kueri,
        onUbah = onKueriBerubah,
        placeholder = "Cari isi pesan atau kutipan balasan",
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun PemisahTanggal(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = TipeIos.Kecil,
            fontWeight = FontWeight.Medium,
            color = NadaIos.NETRAL.teks,
            modifier = Modifier
                .clip(UkuranIos.SudutKapsul)
                .background(Color(0xCCFFFFFF))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun KotakTengah(isi: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) { isi() }
}

@Composable
private fun BubblePantauSiluman(
    pesan: PesanPribadi,
    isSenderA: Boolean,
    labelPengirim: String,
    bolehDengar: Boolean,
    onButuhIzinDengar: () -> Unit,
    onKlikFoto: (String) -> Unit = {},
) {
    val terhapus = pesan.deletedAtMs != null
    val jamFormatted = formatJamWib(pesan.createdAtMs)

    // Mengikuti gaya baru chat: dua nuansa abu, biru hanya sebagai aksen —
    // di sini aksennya menandai siapa yang bicara, bukan latar gelembungnya.
    val bubbleColor = if (isSenderA) Color(0xFFF4F4F7) else Color(0xFFDEDEE4)
    val textColor = Color(0xFF1C1C1E)
    val labelColor = if (isSenderA) WarnaBiru else Color(0xFFC2185B)
    val timeColor = WarnaTeksKedua
    val alignment = if (isSenderA) Alignment.CenterEnd else Alignment.CenterStart
    val bentuk = if (isSenderA) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 5.dp)
    } else {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 5.dp, bottomEnd = 18.dp)
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Column(
            modifier = Modifier
                .padding(start = if (isSenderA) 52.dp else 0.dp, end = if (isSenderA) 0.dp else 52.dp)
                .clip(bentuk)
                .background(bubbleColor)
                .padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            Text(
                text = labelPengirim,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = labelColor
            )

            Spacer(modifier = Modifier.height(2.dp))

            if (terhapus) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Icon(
                        imageVector = Icons.Filled.Block,
                        contentDescription = null,
                        tint = timeColor,
                        modifier = Modifier
                            .padding(end = 5.dp, bottom = 2.dp)
                            .size(13.dp)
                    )
                    Text(
                        text = "Pesan ini telah dihapus",
                        fontSize = 12.sp,
                        fontStyle = FontStyle.Italic,
                        color = WarnaTeksKedua
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = jamFormatted, fontSize = 9.5.sp, color = timeColor)
                }
            } else {
                if (!pesan.replyToSnippet.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x0F000000))
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Column {
                            Text(
                                text = pesan.replyToName ?: "Balasan",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = WarnaBiru
                            )
                            Text(
                                text = pesan.replyToSnippet,
                                fontSize = 10.5.sp,
                                color = Color(0xFF666666),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                }

                if (!pesan.audioPath.isNullOrBlank()) {
                    BubbleSuara(
                        audioPath = pesan.audioPath,
                        audioMs = pesan.audioMs,
                        audioWave = pesan.audioWave,
                        milikSendiri = isSenderA,
                        bolehPutar = bolehDengar,
                        onButuhIzin = onButuhIzinDengar,
                        modifier = Modifier.padding(bottom = 2.dp),
                    )
                }

                if (!pesan.imagePath.isNullOrBlank()) {
                    val url = PrivateChatRepository.urlFoto(pesan.imagePath)
                    AsyncImage(
                        model = url,
                        imageLoader = AvatarStorage.imageLoader(LocalContext.current),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { url?.let { onKlikFoto(it) } }
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                }

                if (pesan.body.isNotBlank()) {
                    Text(
                        text = pesan.body,
                        fontSize = 14.sp,
                        color = textColor
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = jamFormatted,
                        fontSize = 10.sp,
                        color = timeColor
                    )
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
