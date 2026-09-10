package com.sukashawarma.superapp.feature.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sukashawarma.superapp.core.ui.AvatarStaf
import com.sukashawarma.superapp.feature.chat.data.ReaksiPesan

/**
 * Siapa memberi reaksi apa — seperti lembar reaksi WhatsApp, digambar dengan
 * gaya kita sendiri: tab pil ala iOS di atas, daftar nama di bawah.
 *
 * Foto pemberi reaksi TIDAK ikut disimpan di baris reaksi; yang ada hanya nama
 * (di-snapshot trigger). Avatar karena itu diambil dari daftar pesan bila orang
 * itu kebetulan pernah mengirim pesan yang masih hidup — kalau tidak, huruf
 * awal namanya yang tampil. Menyimpan path avatar per reaksi hanya akan
 * menduplikasi data yang berumur 24 jam.
 */
private val BiruIosReaksi = Color(0xFF007AFF)
private val AbuReaksi = Color(0xFF8E8E93)
private val LatarPil = Color(0xFFF2F2F7)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DaftarReaksiSheet(
    reaksi: List<ReaksiPesan>,
    avatarPerUser: Map<String, String?>,
    userId: String,
    onCabut: () -> Unit,
    onTutup: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val perEmoji = remember(reaksi) { reaksi.groupBy { it.emoji } }
    var tab by remember { mutableStateOf<String?>(null) }
    val tampil = remember(reaksi, tab) {
        if (tab == null) reaksi else reaksi.filter { it.emoji == tab }
    }

    ModalBottomSheet(
        onDismissRequest = onTutup,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            Text(
                "Reaksi",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black,
                modifier = Modifier.padding(start = 20.dp, bottom = 12.dp),
            )

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PilTab("Semua ${reaksi.size}", tab == null) { tab = null }
                perEmoji.forEach { (emoji, daftar) ->
                    PilTab("$emoji ${daftar.size}", tab == emoji) { tab = emoji }
                }
            }

            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.heightIn(max = 340.dp)) {
                items(tampil, key = { it.userId + it.emoji }) { r ->
                    val milikku = r.userId == userId
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .then(if (milikku) Modifier.clickable(onClick = onCabut) else Modifier)
                            .padding(horizontal = 20.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AvatarStaf(
                            path = avatarPerUser[r.userId],
                            nama = r.userName,
                            modifier = Modifier.size(38.dp),
                            ukuranHuruf = 15.sp,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (milikku) "Anda" else r.userName.ifBlank { "Tanpa Nama" },
                                fontSize = 15.sp,
                                color = Color.Black,
                            )
                            if (milikku) {
                                Text("Ketuk untuk menghapus reaksi", fontSize = 12.sp, color = AbuReaksi)
                            }
                        }
                        Text(r.emoji, fontSize = 21.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PilTab(label: String, aktif: Boolean, onKlik: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(15.dp))
            .background(if (aktif) BiruIosReaksi else LatarPil)
            .clickable(onClick = onKlik)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            label,
            fontSize = 13.sp,
            color = if (aktif) Color.White else Color.Black,
            fontWeight = if (aktif) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}
