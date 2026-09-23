package com.sukashawarma.superapp.feature.chat.ui.area

import androidx.compose.foundation.lazy.itemsIndexed
import com.sukashawarma.superapp.core.ui.ios.LabelSeksiIos
import com.sukashawarma.superapp.core.ui.ios.LencanaIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.PemisahIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.ios.permukaanIos
import com.sukashawarma.superapp.core.ui.ios.tekanIos
import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.feature.chat.ui.pribadi.bentukBarisGrup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sukashawarma.superapp.core.ui.AvatarStaf
import com.sukashawarma.superapp.feature.chat.data.AnggotaArea
import com.sukashawarma.superapp.feature.chat.data.AreaInfo
import com.sukashawarma.superapp.feature.chat.data.labelRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetAnggotaArea(
    area: AreaInfo,
    anggota: List<AnggotaArea>,
    memuat: Boolean,
    onMulaiChatPribadi: (id: String, nama: String, avatar: String?) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    val amMembers = remember(anggota) { anggota.filter { it.isAreaManager } }
    val nonAmMembers = remember(anggota) { anggota.filter { !it.isAreaManager } }
    val anggotaPerOutlet = remember(nonAmMembers) {
        nonAmMembers.groupBy { it.outlet?.ifBlank { "Lainnya" } ?: "Pusat / Lainnya" }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = WarnaIos.Latar,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = UkuranIos.TepiLayar)
                .navigationBarsPadding()
        ) {
            // Header Sheet
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(WarnaIos.Oranye.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = IkonIos.Groups,
                        contentDescription = null,
                        tint = WarnaIos.Oranye,
                        modifier = Modifier.size(21.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Anggota ${area.namaArea}",
                        style = TipeIos.Judul3,
                    )
                    Text(
                        text = "${anggota.size} Anggota Tim (AM, Leader & Crew)",
                        style = TipeIos.Catatan,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (memuat) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = WarnaIos.Biru, modifier = Modifier.size(32.dp))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Seksi Area Manager (VIP Card)
                    item {
                        LabelSeksiIos(
                            "👑 Area Manager",
                            Modifier.padding(start = 16.dp, bottom = 7.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .permukaanIos(UkuranIos.SudutGrup)
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AvatarStaf(
                                    path = area.amAvatar,
                                    nama = area.amName,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = area.amName,
                                        style = TipeIos.Utama,
                                    )
                                    Text(
                                        text = "Pembina ${area.namaArea}",
                                        style = TipeIos.Catatan,
                                        color = NadaIos.PERINGATAN.teks,
                                    )
                                }

                                amMembers.firstOrNull()?.let { am ->
                                    TombolChatPribadiMini {
                                        onMulaiChatPribadi(am.id, am.namaTampil, am.avatar)
                                        onDismiss()
                                    }
                                }
                            }
                        }
                    }

                    // Seksi Kru & Leader per Outlet
                    anggotaPerOutlet.forEach { (outletNama, anggotaList) ->
                        item {
                            LabelSeksiIos(
                                "🏪 $outletNama (${anggotaList.size})",
                                Modifier.padding(start = 16.dp, top = 22.dp, bottom = 7.dp)
                            )
                        }

                        itemsIndexed(anggotaList, key = { _, it -> it.id }) { i, user ->
                            val terakhir = i == anggotaList.lastIndex
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(bentukBarisGrup(pertama = i == 0, terakhir = terakhir))
                                    .background(WarnaIos.Kartu)
                            ) {
                                BarisAnggotaArea(
                                    user = user,
                                    onChat = {
                                        onMulaiChatPribadi(user.id, user.namaTampil, user.avatar)
                                        onDismiss()
                                    }
                                )
                                if (!terakhir) PemisahIos(inset = 66.dp)
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun BarisAnggotaArea(
    user: AnggotaArea,
    onChat: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarStaf(
            path = user.avatar,
            nama = user.namaTampil,
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = user.namaTampil,
                    style = TipeIos.Isi,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (user.isLeader) {
                    Spacer(modifier = Modifier.width(6.dp))
                    LencanaIos("⭐ Leader", NadaIos.INFO, titik = false)
                }
            }
            Text(
                text = labelRole(user.role) + " • " + (user.outlet ?: "Outlet"),
                style = TipeIos.Catatan,
            )
        }

        TombolChatPribadiMini(onClick = onChat)
    }
}

@Composable
private fun TombolChatPribadiMini(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(UkuranIos.SudutKapsul)
            .background(WarnaIos.Biru.copy(alpha = 0.12f))
            .tekanIos(onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Chat,
                contentDescription = null,
                tint = WarnaIos.Biru,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Chat",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = WarnaIos.Biru
            )
        }
    }
}
