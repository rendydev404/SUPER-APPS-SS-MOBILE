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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sukashawarma.superapp.feature.chat.data.PengaturanGrup

/**
 * Pengaturan grup untuk developer, admin, dan HR.
 *
 * Mode pengumuman ditegakkan database (trigger `chat_messages_fill_sender`),
 * bukan hanya dengan menyembunyikan kotak ketik di sini — menyembunyikan tombol
 * bukan penjagaan.
 */
private val BiruIosSet = Color(0xFF007AFF)
private val AbuSet = Color(0xFF8E8E93)
private val LatarIsian = Color(0xFFF2F2F7)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PengaturanGrupSheet(
    awal: PengaturanGrup,
    menyimpan: Boolean,
    galat: String?,
    onSimpan: (nama: String, deskripsi: String, hanyaAdmin: Boolean) -> Unit,
    onTutup: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var nama by remember { mutableStateOf(awal.namaGrup) }
    var deskripsi by remember { mutableStateOf(awal.deskripsi) }
    var hanyaAdmin by remember { mutableStateOf(awal.hanyaAdmin) }

    ModalBottomSheet(
        onDismissRequest = onTutup,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(42.dp).clip(CircleShape).background(Color(0xFFD8E7FB)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Groups, null, tint = BiruIosSet, modifier = Modifier.size(23.dp))
                }
                Spacer(Modifier.size(12.dp))
                Column {
                    Text("Pengaturan Grup", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                    Text("Hanya developer, admin, dan HR", fontSize = 12.sp, color = AbuSet)
                }
            }

            Spacer(Modifier.height(20.dp))
            Label("Nama grup")
            Isian(nama, "Chat Tim", satuBaris = true) { nama = it.take(40) }

            Spacer(Modifier.height(14.dp))
            Label("Deskripsi")
            Isian(deskripsi, "Keterangan singkat grup", satuBaris = false) { deskripsi = it.take(140) }

            Spacer(Modifier.height(18.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(LatarIsian)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Campaign, null, tint = BiruIosSet, modifier = Modifier.size(21.dp))
                Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Mode pengumuman", fontSize = 15.sp, color = Color.Black)
                    Text(
                        "Hanya pengelola yang bisa mengirim pesan.",
                        fontSize = 12.sp, color = AbuSet,
                    )
                }
                Switch(
                    checked = hanyaAdmin,
                    onCheckedChange = { hanyaAdmin = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF34C759)),
                )
            }

            galat?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, fontSize = 12.5.sp, color = Color(0xFFFF3B30))
            }

            Spacer(Modifier.height(22.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(13.dp))
                    .background(if (menyimpan) BiruIosSet.copy(alpha = 0.5f) else BiruIosSet)
                    .clickable(enabled = !menyimpan) { onSimpan(nama, deskripsi, hanyaAdmin) }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (menyimpan) "Menyimpan…" else "Simpan",
                    color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun Label(teks: String) {
    Text(
        teks.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = AbuSet,
        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
    )
}

@Composable
private fun Isian(nilai: String, petunjuk: String, satuBaris: Boolean, onUbah: (String) -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LatarIsian)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        if (nilai.isEmpty()) Text(petunjuk, fontSize = 15.sp, color = AbuSet)
        BasicTextField(
            value = nilai,
            onValueChange = onUbah,
            singleLine = satuBaris,
            maxLines = if (satuBaris) 1 else 3,
            textStyle = TextStyle(fontSize = 15.sp, color = Color.Black),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(BiruIosSet),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
