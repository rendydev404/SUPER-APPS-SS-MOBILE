package com.sukashawarma.superapp.feature.chat.ui.pribadi

import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.itemsIndexed
import com.sukashawarma.superapp.core.ui.ios.KeadaanIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.PemisahIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sukashawarma.superapp.core.ui.AvatarStaf
import com.sukashawarma.superapp.domain.util.JakartaTime
import com.sukashawarma.superapp.feature.chat.data.PercakapanPribadiItem
import com.sukashawarma.superapp.feature.chat.ui.suara.formatDurasiSuara
import com.sukashawarma.superapp.feature.chat.domain.formatJamWib
import java.time.Instant
import java.time.format.DateTimeFormatter

@Composable
fun TabPercakapanPribadi(
    percakapanList: List<PercakapanPribadiItem>,
    memuat: Boolean,
    onBukaChat: (partnerId: String, partnerName: String, partnerAvatar: String?) -> Unit,
    onMulaiChatBaru: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WarnaIos.Latar)
    ) {
        if (memuat && percakapanList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = WarnaIos.Biru)
            }
        } else if (percakapanList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                KeadaanIos(
                    ikon = Icons.AutoMirrored.Filled.Chat,
                    judul = "Belum Ada Obrolan Pribadi",
                    pesan = "Mulai obrolan 1-on-1 dengan rekan kerja Anda. Pesan otomatis terhapus setiap 03:00 AM WIB.",
                    nada = NadaIos.INFO,
                    teksAksi = "Kirim Pesan Baru",
                    onAksi = onMulaiChatBaru,
                )
            }
        } else {
            // Grup "inset grouped" dibentuk per baris (sudut atas di baris pertama,
            // sudut bawah di baris terakhir). Dulu seluruh LazyColumn yang di-clip
            // putih, sehingga kartu putih ikut memanjang sampai dasar layar walau
            // obrolannya hanya dua.
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = UkuranIos.TepiLayar,
                    end = UkuranIos.TepiLayar,
                    top = 12.dp,
                    bottom = 96.dp,
                ),
            ) {
                itemsIndexed(percakapanList, key = { _, it -> it.partnerId }) { i, item ->
                    val terakhir = i == percakapanList.lastIndex
                    Column(
                        Modifier
                            .clip(bentukBarisGrup(pertama = i == 0, terakhir = terakhir))
                            .background(WarnaIos.Kartu)
                    ) {
                        BarisPercakapanPribadi(
                            item = item,
                            onClick = { onBukaChat(item.partnerId, item.namaTampil, item.partnerAvatar) }
                        )
                        if (!terakhir) PemisahIos(inset = 74.dp)
                    }
                }
            }
        }

        // FAB Tambah Chat Baru (Floating Button Biru)
        FloatingActionButton(
            onClick = onMulaiChatBaru,
            containerColor = WarnaIos.Biru,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Icon(imageVector = Icons.Default.Edit, contentDescription = "Chat Baru", modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun BarisPercakapanPribadi(
    item: PercakapanPribadiItem,
    onClick: () -> Unit,
) {
    val jamFormatted = formatJamWib(item.lastMessageAtMs)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        AvatarStaf(
            path = item.partnerAvatar,
            nama = item.partnerName,
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Konten Nama & Pesan
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.namaTampil,
                    style = TipeIos.Utama,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Text(
                    text = jamFormatted,
                    style = TipeIos.Catatan,
                    color = if (item.unreadCount > 0) WarnaIos.Biru else WarnaIos.Abu,
                    fontWeight = if (item.unreadCount > 0) FontWeight.Bold else FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Jika pengirim terakhir adalah diri sendiri, tampilkan centang status
                if (item.isSelfLastSender) {
                    KomponenCentangPribadi(
                        status = item.statusCentang,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }

                val previewTeks = when {
                    item.lastMessageHasAudio ->
                        "🎤 Pesan suara" + (item.lastMessageAudioMs?.let { " (${formatDurasiSuara(it)})" } ?: "")
                    item.lastMessageHasImage && item.lastMessageBody.isNotBlank() -> "📷 ${item.lastMessageBody}"
                    item.lastMessageHasImage -> "📷 Foto"
                    else -> item.lastMessageBody
                }

                Text(
                    text = previewTeks,
                    style = TipeIos.SubJudul,
                    fontSize = 14.sp,
                    color = if (item.unreadCount > 0) WarnaIos.Label else WarnaIos.LabelKedua,
                    fontWeight = if (item.unreadCount > 0) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (item.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(WarnaIos.Biru),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (item.unreadCount > 99) "99+" else item.unreadCount.toString(),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Bentuk satu baris di dalam grup "inset grouped" yang digulir LazyColumn:
 * hanya baris pertama dan terakhir yang bersudut, agar tumpukan baris terbaca
 * sebagai satu kartu. Objek bentuknya tetap (bukan dibuat per komposisi).
 */
internal fun bentukBarisGrup(pertama: Boolean, terakhir: Boolean): Shape = when {
    pertama && terakhir -> SudutGrupTunggal
    pertama -> SudutGrupAtas
    terakhir -> SudutGrupBawah
    else -> RectangleShape
}

private val SudutGrupTunggal = RoundedCornerShape(16.dp)
private val SudutGrupAtas = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
private val SudutGrupBawah = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
