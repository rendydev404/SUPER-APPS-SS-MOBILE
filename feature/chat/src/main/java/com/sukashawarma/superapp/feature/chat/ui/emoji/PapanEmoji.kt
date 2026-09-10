package com.sukashawarma.superapp.feature.chat.ui.emoji

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardBackspace
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * Papan emoji bergaya Telegram: satu bilah kategori berbentuk pil, kisi yang
 * digulir menerus, dan tombol hapus mengambang. Bingkainya digambar dengan
 * palet iOS milik kita — rupa emoji-nya sendiri berasal dari font perangkat
 * (lihat catatan lisensi di DataEmoji.kt).
 *
 * Logika Telegram yang ditiru: papan ini MENGGANTIKAN papan ketik, bukan
 * menumpuk di atasnya, dan tingginya kira-kira setinggi papan ketik supaya
 * daftar pesan tidak melompat saat berpindah antar keduanya.
 */
private val BiruIosEmoji = Color(0xFF007AFF)
private val AbuIkon = Color(0xFF8E8E93)
private val LatarPapan = Color(0xFFF7F7F8)
private val GarisPapan = Color(0x1F3C3C43)

const val TINGGI_PAPAN_EMOJI_DP = 268

@Composable
fun PapanEmoji(
    onPilih: (String) -> Unit,
    onHapus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val konteks = LocalContext.current
    // Dibaca sekali saat papan dibuka: menyusun ulang daftar sambil pengguna
    // mengetik hanya akan membuat kisi melompat di bawah jarinya.
    val sering = remember { EmojiSering.ambil(konteks) }

    val kelompok = remember(sering) {
        if (sering.isEmpty()) KELOMPOK_EMOJI
        else listOf(KelompokEmoji("Sering", "🕘", sering)) + KELOMPOK_EMOJI
    }

    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()

    // Indeks awal tiap kelompok di dalam kisi datar, untuk melompat dan untuk
    // menandai tab mana yang sedang terlihat.
    val awalKelompok = remember(kelompok) {
        var jalan = 0
        kelompok.map { k ->
            val mulai = jalan
            jalan += k.isi.size
            mulai
        }
    }
    var tabAktif by remember { mutableIntStateOf(0) }
    LaunchedEffect(gridState, awalKelompok) {
        snapshotFlow { gridState.firstVisibleItemIndex }.collect { indeks ->
            tabAktif = awalKelompok.indexOfLast { it <= indeks }.coerceAtLeast(0)
        }
    }

    val semua = remember(kelompok) { kelompok.flatMap { it.isi } }

    Column(
        modifier
            .fillMaxWidth()
            .height(TINGGI_PAPAN_EMOJI_DP.dp)
            .background(LatarPapan),
    ) {
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(GarisPapan))

        Box(Modifier.weight(1f)) {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Adaptive(minSize = 44.dp),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 8.dp, end = 8.dp, top = 8.dp, bottom = 56.dp,
                ),
            ) {
                items(semua, key = { it + semua.indexOf(it) }) { emoji ->
                    Box(
                        Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                EmojiSering.catat(konteks, emoji)
                                onPilih(emoji)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(emoji, fontSize = 25.sp)
                    }
                }
            }

            // Tombol hapus mengambang di sudut, seperti papan emoji Telegram.
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 10.dp)
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable(onClick = onHapus),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardBackspace,
                    "Hapus satu karakter",
                    tint = AbuIkon,
                    modifier = Modifier.size(21.dp),
                )
            }
        }

        BilahKategori(
            kelompok = kelompok,
            aktif = tabAktif,
            onPilih = { i ->
                tabAktif = i
                scope.launch { gridState.scrollToItem(awalKelompok[i]) }
            },
        )
    }
}

@Composable
private fun BilahKategori(
    kelompok: List<KelompokEmoji>,
    aktif: Int,
    onPilih: (Int) -> Unit,
) {
    Column {
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(GarisPapan))
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 6.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            kelompok.forEachIndexed { i, k ->
                val terpilih = i == aktif
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (terpilih) Color(0x1F007AFF) else Color.Transparent)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onPilih(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(k.ikon, fontSize = 17.sp)
                }
            }
        }
    }
}
