package com.sukashawarma.superapp.presentation.absensi.clock

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sukashawarma.superapp.feature.absensi.shift.ShiftOption
import com.sukashawarma.superapp.presentation.theme.SukaOrange

private val Ink = Color(0xFF11142D)
private val Muted = Color(0xFF6B7280)
private val Line = Color(0xFFE5E7EB)

private fun ikonShift(jamMasuk: String): ImageVector {
    val h = jamMasuk.take(2).toIntOrNull() ?: 0
    return when {
        h < 11 -> Icons.Default.WbSunny
        h < 15 -> Icons.Default.WbTwilight
        else -> Icons.Default.Bedtime
    }
}

/**
 * Modal wajib pilih shift — cermin `PilihShiftModal.tsx` (web) untuk mode pribadi: panel
 * dari bawah, tanpa tombol tutup, ketuk latar tidak menutup. Tombol kembali HP keluar
 * dari layar Absen (sama dengan tombol back browser di web), bukan melewati pilihan.
 */
@Composable
internal fun PilihShiftSheet(
    staffName: String?,
    options: List<ShiftOption>,
    onPick: (Int) -> Unit,
    onExit: () -> Unit,
) {
    Dialog(
        onDismissRequest = onExit,
        properties = DialogProperties(
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f)),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 448.dp)
                    .heightIn(max = maxHeight * 0.9f),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = Color.White,
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 24.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 10.dp, bottom = 18.dp)
                            .size(width = 44.dp, height = 5.dp)
                            .clip(CircleShape)
                            .background(Line)
                            .align(Alignment.CenterHorizontally),
                    )
                    Text(
                        text = "Halo, ${staffName.orEmpty()} 👋",
                        color = Muted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Pilih shift kamu hari ini",
                        color = Ink,
                        fontSize = 22.sp,
                        lineHeight = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Jam telat dan jam pulang dihitung dari shift yang kamu pilih.",
                        color = Muted,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                    )
                    Spacer(Modifier.height(20.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        options.forEach { opsi ->
                            TombolShift(opsi = opsi, onClick = { onPick(opsi.ke) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TombolShift(opsi: ShiftOption, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFFFF7ED),
        border = BorderStroke(1.5.dp, SukaOrange.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(SukaOrange.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(ikonShift(opsi.jamMasuk), contentDescription = null, tint = SukaOrange, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(opsi.nama, color = SukaOrange, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(opsi.rentang, color = Ink, fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    "Masuk ${opsi.jamMasuk} · Pulang ${opsi.jamKeluar}",
                    color = Muted,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SukaOrange)
        }
    }
}

/** Kartu "Shift Siang · 13:00 – 22:00" + tombol Ubah di atas kamera. */
@Composable
internal fun KartuShiftTerpilih(
    opsi: ShiftOption,
    ubahEnabled: Boolean,
    onUbah: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, SukaOrange.copy(alpha = 0.3f)),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(ikonShift(opsi.jamMasuk), contentDescription = null, tint = SukaOrange, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                text = "${opsi.nama} · ${opsi.rentang}",
                color = Ink,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onUbah, enabled = ubahEnabled) {
                Text("Ubah", color = if (ubahEnabled) SukaOrange else Muted, fontWeight = FontWeight.Bold)
            }
        }
    }
}
