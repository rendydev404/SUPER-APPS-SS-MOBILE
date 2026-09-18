package com.sukashawarma.superapp.feature.chat.ui.developer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sukashawarma.superapp.core.ui.AvatarStaf
import com.sukashawarma.superapp.domain.util.JakartaTime
import com.sukashawarma.superapp.feature.chat.data.PercakapanPengawasanItem
import com.sukashawarma.superapp.feature.chat.data.labelRole
import com.sukashawarma.superapp.feature.chat.ui.suara.formatDurasiSuara
import com.sukashawarma.superapp.feature.chat.domain.formatJamWib
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

private val WarnaLatar = Color(0xFFF2F2F7)
private val WarnaKartu = Color.White
private val WarnaPemisah = Color(0xFFE5E5EA)
private val WarnaTeksUtama = Color(0xFF1C1C1E)
private val WarnaTeksKedua = Color(0xFF8E8E93)
private val WarnaBiru = Color(0xFF007AFF)

@Composable
fun LayarPantauChatDev(
    pengawasanList: List<PercakapanPengawasanItem>,
    memuat: Boolean,
    onBukaDetail: (item: PercakapanPengawasanItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    var kueri by remember { mutableStateOf("") }

    val hasilFilter = remember(pengawasanList, kueri) {
        val q = kueri.trim().lowercase(Locale.getDefault())
        if (q.isBlank()) pengawasanList else pengawasanList.filter { it.cocokDengan(q) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WarnaLatar)
    ) {
        SpandukPengawasan()

        KolomPencarian(
            kueri = kueri,
            onKueriBerubah = { kueri = it },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )

        when {
            memuat && pengawasanList.isEmpty() -> KotakPesan {
                CircularProgressIndicator(color = WarnaBiru)
            }

            pengawasanList.isEmpty() -> KotakPesan {
                TeksKosong("Belum ada obrolan pribadi antar staf pada siklus aktif hari ini.")
            }

            hasilFilter.isEmpty() -> KotakPesan {
                TeksKosong("Tidak ada percakapan yang cocok dengan \"${kueri.trim()}\".")
            }

            else -> {
                Text(
                    text = if (kueri.isBlank()) {
                        "${hasilFilter.size} PERCAKAPAN"
                    } else {
                        "${hasilFilter.size} DARI ${pengawasanList.size} PERCAKAPAN"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = WarnaTeksKedua,
                    modifier = Modifier.padding(start = 20.dp, bottom = 6.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)
                ) {
                    item {
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .background(
                                    WarnaKartu,
                                    RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                                )
                        )
                    }

                    itemsIndexed(
                        items = hasilFilter,
                        key = { _, it -> "${it.userAId}_${it.userBId}" }
                    ) { index, item ->
                        BarisPengawasanDev(
                            item = item,
                            tampilkanPemisah = index < hasilFilter.lastIndex,
                            onClick = { onBukaDetail(item) }
                        )
                    }

                    item {
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .background(
                                    WarnaKartu,
                                    RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                                )
                        )
                    }
                }
            }
        }
    }
}

private fun PercakapanPengawasanItem.cocokDengan(q: String): Boolean {
    fun String?.punya() = this?.lowercase(Locale.getDefault())?.contains(q) == true
    return userAName.punya() ||
        userBName.punya() ||
        labelRole(userARole).punya() ||
        labelRole(userBRole).punya() ||
        userAOutlet.punya() ||
        userBOutlet.punya() ||
        lastSenderName.punya() ||
        lastMessageBody.punya()
}

@Composable
private fun SpandukPengawasan() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1C1C1E))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Visibility,
            contentDescription = null,
            tint = Color(0xFFFFD60A),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = "MODE PENGAWASAN DEVELOPER",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD60A)
            )
            Text(
                text = "Siluman — centang biru staf tidak berubah.",
                fontSize = 11.sp,
                color = Color(0xFFAEAEB2)
            )
        }
    }
}

@Composable
private fun KolomPencarian(
    kueri: String,
    onKueriBerubah: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboard = LocalSoftwareKeyboardController.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFE3E3E8))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = WarnaTeksKedua,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))

        Box(modifier = Modifier.weight(1f)) {
            if (kueri.isEmpty()) {
                Text(
                    text = "Cari nama, jabatan, cabang, atau isi pesan",
                    fontSize = 14.sp,
                    color = WarnaTeksKedua,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BasicTextField(
                value = kueri,
                onValueChange = onKueriBerubah,
                singleLine = true,
                textStyle = TextStyle(fontSize = 14.sp, color = WarnaTeksUtama),
                cursorBrush = SolidColor(WarnaBiru),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (kueri.isNotEmpty()) {
            Icon(
                imageVector = Icons.Default.Cancel,
                contentDescription = "Hapus pencarian",
                tint = WarnaTeksKedua,
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .clickable { onKueriBerubah("") }
            )
        }
    }
}

@Composable
private fun KotakPesan(isi: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) { isi() }
}

@Composable
private fun TeksKosong(teks: String) {
    Text(
        text = teks,
        fontSize = 13.sp,
        color = WarnaTeksKedua,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun BarisPengawasanDev(
    item: PercakapanPengawasanItem,
    tampilkanPemisah: Boolean,
    onClick: () -> Unit,
) {
    val jamFormatted = formatJamWib(item.lastMessageAtMs)

    Column(modifier = Modifier.background(WarnaKartu)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(44.dp)) {
                AvatarStaf(
                    path = item.userAAvatar,
                    nama = item.userAName,
                    modifier = Modifier
                        .size(30.dp)
                        .align(Alignment.TopStart)
                        .clip(CircleShape),
                    ukuranHuruf = 13.sp
                )
                AvatarStaf(
                    path = item.userBAvatar,
                    nama = item.userBName,
                    modifier = Modifier
                        .size(30.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .border(2.dp, WarnaKartu, CircleShape),
                    ukuranHuruf = 13.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${item.userAName} ↔ ${item.userBName}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = WarnaTeksUtama,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = jamFormatted,
                        fontSize = 12.sp,
                        color = WarnaTeksKedua
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = "${labelRole(item.userARole)} · ${item.userAOutlet ?: "Pusat"}" +
                        "  ↔  ${labelRole(item.userBRole)} · ${item.userBOutlet ?: "Pusat"}",
                    fontSize = 11.sp,
                    color = WarnaTeksKedua,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val snippet = when {
                        item.lastHasAudio ->
                            "🎤 Pesan suara" + (item.lastAudioMs?.let { " (${formatDurasiSuara(it)})" } ?: "")
                        item.lastHasImage -> "📷 Foto"
                        else -> item.lastMessageBody
                    }
                    Text(
                        text = "${item.lastSenderName}: $snippet",
                        fontSize = 13.sp,
                        color = Color(0xFF636366),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .background(WarnaBiru)
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${item.totalMessages}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFFC7C7CC),
                modifier = Modifier.size(20.dp)
            )
        }

        if (tampilkanPemisah) {
            HorizontalDivider(
                color = WarnaPemisah,
                thickness = 0.7.dp,
                modifier = Modifier.padding(start = 72.dp)
            )
        }
    }
}
