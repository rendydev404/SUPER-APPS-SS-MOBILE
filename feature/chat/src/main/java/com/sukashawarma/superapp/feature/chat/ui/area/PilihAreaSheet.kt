package com.sukashawarma.superapp.feature.chat.ui.area

import androidx.compose.ui.text.style.TextOverflow
import com.sukashawarma.superapp.core.ui.ios.LencanaIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.ios.permukaanIos
import com.sukashawarma.superapp.core.ui.ios.tekanIos
import com.sukashawarma.superapp.core.ui.kaca.IkonIos
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sukashawarma.superapp.feature.chat.data.AreaInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PilihAreaSheet(
    areaAktif: AreaInfo,
    daftarArea: List<AreaInfo>,
    onPilihArea: (AreaInfo) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(WarnaIos.Biru.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = IkonIos.LocationOn,
                        contentDescription = null,
                        tint = WarnaIos.Biru,
                        modifier = Modifier.size(21.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Pilih Grup Area",
                        style = TipeIos.Judul3,
                    )
                    Text(
                        text = "Akses ruang komunikasi Area Manager & Crew",
                        style = TipeIos.Catatan,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // List of Area Cards
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(UkuranIos.JarakKartu),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(daftarArea, key = { it.areaId }) { area ->
                    val isDipilih = area.areaId == areaAktif.areaId
                    KartuItemArea(
                        area = area,
                        isDipilih = isDipilih,
                        onClick = {
                            onPilihArea(area)
                            onDismiss()
                        }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun KartuItemArea(
    area: AreaInfo,
    isDipilih: Boolean,
    onClick: () -> Unit,
) {
    // Pilihan aktif ditandai centang beraksen & isian aksen tipis, bukan garis
    // tepi tebal — gaya daftar pilihan iOS.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .permukaanIos(UkuranIos.SudutKartu, if (isDipilih) PilihanAktif else WarnaIos.Kartu)
            .tekanIos(onClick)
            .padding(UkuranIos.PaddingKartu)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar / Icon Area
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            if (isDipilih) listOf(WarnaIos.Aksen, Color(0xFFF97316))
                            else listOf(WarnaIos.Abu, WarnaIos.AbuGelap)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = area.amName.take(2).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = area.namaArea,
                        style = TipeIos.Utama,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    LencanaIos("👑 AM " + area.amName, NadaIos.PERINGATAN, titik = false)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Store,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = WarnaIos.Abu
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${area.outlets.size} Cabang: " + area.outlets.joinToString(", "),
                        style = TipeIos.Catatan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Checkmark indicator
            if (isDipilih) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(WarnaIos.Aksen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Dipilih",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

private val PilihanAktif = Color(0xFFFFF4EC)
