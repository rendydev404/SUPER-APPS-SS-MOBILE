package com.sukashawarma.superapp.feature.chat.ui.pribadi

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
            .background(Color(0xFFF2F2F7))
    ) {
        if (memuat && percakapanList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF007AFF))
            }
        } else if (percakapanList.isEmpty()) {
            // Tampilan kosong
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE5E5EA)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = null,
                        tint = Color(0xFF8E8E93),
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Belum Ada Obrolan Pribadi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1C1C1E)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Mulai obrolan 1-on-1 dengan rekan kerja Anda. Pesan otomatis terhapus setiap 03:00 AM WIB.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8E8E93),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                androidx.compose.material3.Button(
                    onClick = onMulaiChatBaru,
                    shape = RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF007AFF)
                    )
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Kirim Pesan Baru", fontWeight = FontWeight.SemiBold)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
            ) {
                items(percakapanList, key = { it.partnerId }) { item ->
                    BarisPercakapanPribadi(
                        item = item,
                        onClick = { onBukaChat(item.partnerId, item.namaTampil, item.partnerAvatar) }
                    )
                    HorizontalDivider(
                        color = Color(0xFFF2F2F7),
                        thickness = 1.dp,
                        modifier = Modifier.padding(start = 72.dp)
                    )
                }
            }
        }

        // FAB Tambah Chat Baru (Floating Button Biru)
        FloatingActionButton(
            onClick = onMulaiChatBaru,
            containerColor = Color(0xFF007AFF),
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
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = Color(0xFF1C1C1E),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Text(
                    text = jamFormatted,
                    fontSize = 12.sp,
                    color = if (item.unreadCount > 0) Color(0xFF007AFF) else Color(0xFF8E8E93),
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
                    fontSize = 13.sp,
                    color = if (item.unreadCount > 0) Color(0xFF1C1C1E) else Color(0xFF8E8E93),
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
                            .background(Color(0xFF007AFF)),
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
