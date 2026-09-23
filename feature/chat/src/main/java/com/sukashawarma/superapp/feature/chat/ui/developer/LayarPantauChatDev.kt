package com.sukashawarma.superapp.feature.chat.ui.developer

import com.sukashawarma.superapp.core.ui.ios.KeadaanIos
import com.sukashawarma.superapp.core.ui.ios.KolomCariIos
import com.sukashawarma.superapp.core.ui.ios.LabelSeksiIos
import com.sukashawarma.superapp.core.ui.ios.PemisahIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.ios.permukaanIos
import com.sukashawarma.superapp.core.ui.kaca.IkonIos
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

private val WarnaLatar = WarnaIos.Latar
private val WarnaKartu = WarnaIos.Kartu
private val WarnaTeksUtama = WarnaIos.Label
private val WarnaTeksKedua = WarnaIos.Abu
private val WarnaBiru = WarnaIos.Biru

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
                KeadaanIos(
                    ikon = IkonIos.Inbox,
                    judul = "Belum ada obrolan",
                    pesan = "Belum ada obrolan pribadi antar staf pada siklus aktif hari ini.",
                )
            }

            hasilFilter.isEmpty() -> KotakPesan {
                KeadaanIos(
                    ikon = IkonIos.Search,
                    judul = "Tidak ditemukan",
                    pesan = "Tidak ada percakapan yang cocok dengan \"${kueri.trim()}\".",
                )
            }

            else -> {
                LabelSeksiIos(
                    if (kueri.isBlank()) {
                        "${hasilFilter.size} percakapan"
                    } else {
                        "${hasilFilter.size} dari ${pengawasanList.size} percakapan"
                    },
                    Modifier.padding(start = 32.dp, bottom = 7.dp)
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
    // Kartu gelap sengaja dipertahankan: mode siluman harus terlihat berbeda
    // dari daftar biasa sekilas mata, supaya developer tidak lupa sedang mengintip.
    Row(
        modifier = Modifier
            .padding(start = UkuranIos.TepiLayar, end = UkuranIos.TepiLayar, top = 12.dp)
            .fillMaxWidth()
            .permukaanIos(UkuranIos.SudutGrup, WarnaIos.Label)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(WarnaIos.Kuning.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = IkonIos.Visibility,
                contentDescription = null,
                tint = WarnaIos.Kuning,
                modifier = Modifier.size(17.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = "Mode Pengawasan Developer",
                style = TipeIos.Utama,
                fontSize = 15.sp,
                color = WarnaIos.Kuning
            )
            Text(
                text = "Siluman — centang biru staf tidak berubah.",
                style = TipeIos.Catatan,
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

    KolomCariIos(
        nilai = kueri,
        onUbah = onKueriBerubah,
        placeholder = "Cari nama, jabatan, cabang, atau isi pesan",
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun KotakPesan(isi: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) { isi() }
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
                        style = TipeIos.Utama,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = jamFormatted,
                        style = TipeIos.Catatan,
                        color = WarnaTeksKedua
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = "${labelRole(item.userARole)} · ${item.userAOutlet ?: "Pusat"}" +
                        "  ↔  ${labelRole(item.userBRole)} · ${item.userBOutlet ?: "Pusat"}",
                    style = TipeIos.Kecil,
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
                        style = TipeIos.SubJudul,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(UkuranIos.SudutKapsul)
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
                imageVector = IkonIos.ChevronRight,
                contentDescription = null,
                tint = WarnaIos.LabelKetiga,
                modifier = Modifier.padding(start = 6.dp).size(15.dp)
            )
        }

        if (tampilkanPemisah) {
            PemisahIos(inset = 72.dp)
        }
    }
}
