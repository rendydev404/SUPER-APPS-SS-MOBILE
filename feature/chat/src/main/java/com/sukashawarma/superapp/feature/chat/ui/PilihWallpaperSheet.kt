package com.sukashawarma.superapp.feature.chat.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatColorReset
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sukashawarma.superapp.feature.chat.data.ChatWallpapers
import com.sukashawarma.superapp.feature.chat.data.ItemWallpaper

private val BiruIos = Color(0xFF007AFF)
private val LatarGrup = Color(0xFFF2F2F7)
private val GarisTipis = Color(0x2E3C3C43)
private val AbuKeterangan = Color(0xFF8E8E93)

/**
 * Lembar pemilihan wallpaper obrolan gaya WhatsApp / Telegram.
 * Hanya dapat dibuka oleh pengelola grup (`developer`, `admin`, `admin_hr`).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PilihWallpaperSheet(
    wallpaperAwal: String,
    onTerapkan: (idWallpaper: String) -> Unit,
    onTutup: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var wallpaperTerpilih by remember { mutableStateOf(wallpaperAwal) }

    ModalBottomSheet(
        onDismissRequest = onTutup,
        sheetState = sheetState,
        containerColor = LatarGrup,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            // Header bilah
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onTutup) {
                    Icon(Icons.Filled.Close, contentDescription = "Tutup", tint = Color.Black)
                }
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Wallpaper Obrolan",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                    )
                    Text(
                        "Pilih latar belakang untuk seluruh anggota tim",
                        fontSize = 12.sp,
                        color = AbuKeterangan,
                    )
                }
                Button(
                    onClick = {
                        onTerapkan(wallpaperTerpilih)
                        onTutup()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BiruIos,
                        disabledContainerColor = BiruIos.copy(alpha = 0.4f),
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text("Terapkan", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(GarisTipis)
            )

            // Grid daftar wallpaper
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f, fill = false),
            ) {
                items(ChatWallpapers.DAFTAR, key = { it.id }) { item ->
                    KartuOpsiWallpaper(
                        item = item,
                        terpilih = item.id.equals(wallpaperTerpilih, ignoreCase = true),
                        onPilih = { wallpaperTerpilih = item.id },
                    )
                }
            }
        }
    }
}

@Composable
private fun KartuOpsiWallpaper(
    item: ItemWallpaper,
    terpilih: Boolean,
    onPilih: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onPilih),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Thumbnail kotak vertikal aspek rasio mirip layar HP (9:16)
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(9f / 14f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .then(
                    if (terpilih) Modifier.border(3.dp, BiruIos, RoundedCornerShape(12.dp))
                    else Modifier.border(1.dp, GarisTipis, RoundedCornerShape(12.dp))
                ),
        ) {
            val resId = item.drawableRes
            if (resId != null) {
                Image(
                    painter = painterResource(id = resId),
                    contentDescription = item.nama,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                // Tampilan Default Putih polos
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE5E5EA)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.FormatColorReset,
                            contentDescription = null,
                            tint = Color(0xFF8E8E93),
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Putih Bersih",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF6C6C70),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            // Lencana centang jika aktif
            if (terpilih) {
                Box(
                    Modifier
                        .padding(8.dp)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(BiruIos)
                        .align(Alignment.TopEnd),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = "Terpilih",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Text(
            text = item.nama,
            fontSize = 13.sp,
            fontWeight = if (terpilih) FontWeight.Bold else FontWeight.SemiBold,
            color = if (terpilih) BiruIos else Color.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Text(
            text = item.deskripsi,
            fontSize = 11.sp,
            color = AbuKeterangan,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}
