package com.sukashawarma.superapp.presentation.absensi.clock

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sukashawarma.superapp.feature.absensi.shift.ShiftOption
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.ios.permukaanIos
import com.sukashawarma.superapp.core.ui.ios.tekanIos
import com.sukashawarma.superapp.core.ui.kaca.IkonIos

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
                .background(Color.Black.copy(alpha = 0.45f)),
            contentAlignment = Alignment.BottomCenter,
        ) {
            // Lembar bawah iOS: latar abu grouped supaya kartu shift putih di atasnya
            // terbaca sebagai pilihan terpisah, bukan kotak bergaris.
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 448.dp)
                    .heightIn(max = maxHeight * 0.9f),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = WarnaIos.Latar,
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                        .padding(horizontal = UkuranIos.TepiLayar + 4.dp)
                        .padding(bottom = 24.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 8.dp, bottom = 18.dp)
                            .size(width = 36.dp, height = 5.dp)
                            .clip(CircleShape)
                            .background(WarnaIos.LabelKetiga)
                            .align(Alignment.CenterHorizontally),
                    )
                    Text(
                        text = "Halo, ${staffName.orEmpty()} 👋",
                        style = TipeIos.SubJudul.copy(fontWeight = FontWeight.SemiBold),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(text = "Pilih shift kamu hari ini", style = TipeIos.Judul2)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Jam telat dan jam pulang dihitung dari shift yang kamu pilih.",
                        style = TipeIos.SubJudul,
                    )
                    Spacer(Modifier.height(20.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(UkuranIos.JarakKartu)) {
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp)
            .permukaanIos()
            .tekanIos(onClick)
            .padding(horizontal = UkuranIos.PaddingKartu, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(WarnaIos.Aksen.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(ikonShift(opsi.jamMasuk), contentDescription = null, tint = WarnaIos.Aksen, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(opsi.nama, style = TipeIos.Catatan.copy(color = NadaIos.AKSEN.teks, fontWeight = FontWeight.SemiBold))
            Text(opsi.rentang, style = TipeIos.AngkaBesar)
            Text("Masuk ${opsi.jamMasuk} · Pulang ${opsi.jamKeluar}", style = TipeIos.Catatan)
        }
        Icon(IkonIos.ChevronRight, contentDescription = null, tint = WarnaIos.LabelKetiga, modifier = Modifier.size(16.dp))
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .permukaanIos(UkuranIos.SudutGrup)
            .padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(30.dp).clip(CircleShape).background(WarnaIos.Aksen.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(ikonShift(opsi.jamMasuk), contentDescription = null, tint = WarnaIos.Aksen, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = "${opsi.nama} · ${opsi.rentang}",
            style = TipeIos.Utama.copy(fontSize = 15.sp),
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onUbah, enabled = ubahEnabled) {
            Text(
                "Ubah",
                color = if (ubahEnabled) WarnaIos.Aksen else WarnaIos.LabelKetiga,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
