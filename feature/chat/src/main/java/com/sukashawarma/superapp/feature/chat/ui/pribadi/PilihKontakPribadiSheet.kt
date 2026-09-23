package com.sukashawarma.superapp.feature.chat.ui.pribadi

import androidx.compose.foundation.lazy.itemsIndexed
import com.sukashawarma.superapp.core.ui.ios.KeadaanIos
import com.sukashawarma.superapp.core.ui.ios.KolomCariIos
import com.sukashawarma.superapp.core.ui.ios.LabelSeksiIos
import com.sukashawarma.superapp.core.ui.ios.PemisahIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.TombolBundarIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sukashawarma.superapp.core.ui.AvatarStaf
import com.sukashawarma.superapp.feature.chat.data.AnggotaGrup
import com.sukashawarma.superapp.feature.chat.data.labelRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PilihKontakPribadiSheet(
    kontakList: List<AnggotaGrup>,
    memuat: Boolean,
    onPilihKontak: (AnggotaGrup) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    var query by remember { mutableStateOf("") }

    val filtered = remember(kontakList, query) {
        if (query.isBlank()) kontakList
        else {
            val q = query.trim().lowercase()
            kontakList.filter {
                it.nama.lowercase().contains(q) ||
                it.outlet?.lowercase()?.contains(q) == true ||
                it.role?.lowercase()?.contains(q) == true
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = WarnaIos.Latar,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Chat Pribadi Baru",
                        style = TipeIos.Judul2,
                    )
                    Text(
                        text = "Pilih staf atau rekan kerja untuk mulai mengobrol",
                        style = TipeIos.Catatan,
                    )
                }
                TombolBundarIos(IkonIos.Close, "Tutup", onDismiss, warnaIkon = WarnaIos.LabelKedua)
            }

            Spacer(modifier = Modifier.height(12.dp))

            KolomCariIos(
                nilai = query,
                onUbah = { query = it },
                placeholder = "Cari nama, cabang, atau jabatan...",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (memuat) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = WarnaIos.Biru)
                }
            } else if (filtered.isEmpty()) {
                KeadaanIos(
                    ikon = if (query.isNotBlank()) IkonIos.Search else IkonIos.Person,
                    judul = if (query.isNotBlank()) "Tidak ada kontak yang cocok dengan '$query'" else "Belum ada kontak tersedia",
                    pesan = "",
                )
            } else {
                LabelSeksiIos(
                    "Kontak (${filtered.size})",
                    Modifier.padding(start = 16.dp, bottom = 7.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(UkuranIos.SudutGrup)
                        .background(WarnaIos.Kartu)
                ) {
                    itemsIndexed(filtered, key = { _, it -> it.id }) { i, kontak ->
                        BarisKontak(kontak = kontak, onClick = { onPilihKontak(kontak) })
                        if (i < filtered.lastIndex) PemisahIos(inset = 70.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun BarisKontak(
    kontak: AnggotaGrup,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        AvatarStaf(
            path = kontak.avatar,
            nama = kontak.nama,
            modifier = Modifier.size(44.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = kontak.nama,
                style = TipeIos.Utama,
                fontSize = 16.sp,
            )
            val peran = labelRole(kontak.role)
            val outlet = kontak.outlet?.takeIf { it.isNotBlank() } ?: "Pusat"
            Text(
                text = "$peran • $outlet",
                style = TipeIos.Catatan,
            )
        }
    }
}
