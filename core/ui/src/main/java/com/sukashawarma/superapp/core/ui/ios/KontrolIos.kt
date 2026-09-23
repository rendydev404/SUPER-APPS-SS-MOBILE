package com.sukashawarma.superapp.core.ui.ios

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sukashawarma.superapp.core.ui.kaca.IkonIos

// ───────────────────────────────────────────────────────── segmented control

/**
 * Wadah segmented control iOS: isian abu, segmen terpilih berupa kapsul putih
 * berbayang. [gulir] untuk deret segmen yang bisa lebih lebar dari layar (mis. periode);
 * tanpa gulir, beri tiap [SegmenIos] `Modifier.weight(1f)` agar lebarnya dibagi rata.
 */
@Composable
fun WadahSegmenIos(
    modifier: Modifier = Modifier,
    gulir: Boolean = false,
    isi: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(UkuranIos.SudutKontrol)
            .background(WarnaIos.Isian)
            .padding(2.dp)
            .then(if (gulir) Modifier.horizontalScroll(rememberScrollState()) else Modifier),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = isi,
    )
}

/** Satu segmen dalam [WadahSegmenIos]. [lencana] menampilkan hitungan kecil di kanan label. */
@Composable
fun SegmenIos(
    label: String,
    aktif: Boolean,
    onKlik: () -> Unit,
    modifier: Modifier = Modifier,
    ikon: ImageVector? = null,
    lencana: String? = null,
    warnaLencana: Color = WarnaIos.Aksen,
    jarakSisi: Dp = 12.dp,
) {
    val bentuk = RoundedCornerShape(10.dp)
    Row(
        modifier
            .heightIn(min = 34.dp)
            // tekanIos di depan supaya kapsul putihnya ikut mengecil, bukan hanya teksnya.
            .tekanIos(onKlik, peran = androidx.compose.ui.semantics.Role.Tab)
            .then(if (aktif) Modifier.bayanganIos(bentuk, tinggi = 3.dp) else Modifier)
            .clip(bentuk)
            .background(if (aktif) WarnaIos.Kartu else Color.Transparent)
            .padding(horizontal = jarakSisi, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (ikon != null) {
            Icon(ikon, null, tint = if (aktif) WarnaIos.Aksen else WarnaIos.LabelKedua, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(5.dp))
        }
        Text(
            label,
            color = WarnaIos.Label,
            fontSize = 13.sp,
            fontWeight = if (aktif) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (lencana != null) {
            Spacer(Modifier.width(6.dp))
            Box(
                Modifier
                    .heightIn(min = 18.dp)
                    .clip(UkuranIos.SudutKapsul)
                    .background(warnaLencana)
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(lencana, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }
}

/**
 * Kapsul pilihan/filter: terisi aksen saat aktif, abu saat tidak — pengganti chip
 * Material. [jumlah] menampilkan hitungan di kanan label.
 */
@Composable
fun KapsulPilihanIos(
    label: String,
    aktif: Boolean,
    onKlik: () -> Unit,
    modifier: Modifier = Modifier,
    ikon: ImageVector? = null,
    jumlah: Int? = null,
    warnaAktif: Color = WarnaIos.Aksen,
) {
    Row(
        modifier
            .heightIn(min = 34.dp)
            .tekanIos(onKlik)
            .clip(UkuranIos.SudutKapsul)
            .background(if (aktif) warnaAktif else WarnaIos.Isian)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (ikon != null) {
            Icon(ikon, null, tint = if (aktif) Color.White else WarnaIos.LabelKedua, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(5.dp))
        }
        Text(
            label,
            color = if (aktif) Color.White else WarnaIos.Label,
            fontSize = 14.sp,
            fontWeight = if (aktif) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
        )
        if (jumlah != null) {
            Spacer(Modifier.width(6.dp))
            Text(
                jumlah.toString(),
                color = if (aktif) Color.White.copy(alpha = 0.85f) else WarnaIos.LabelKedua,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────── ikon & galat

/**
 * Ikon dalam lingkaran — penanda kartu ala aplikasi Pengingat iOS.
 *
 * @param padat true = lingkaran berwarna penuh dengan ikon putih; false = lingkaran
 *   berwarna tipis dengan ikon berwarna (untuk baris daftar yang lebih tenang).
 */
@Composable
fun IkonBulatIos(
    ikon: ImageVector,
    warna: Color,
    modifier: Modifier = Modifier,
    ukuran: Dp = 30.dp,
    padat: Boolean = true,
) {
    Box(
        modifier.size(ukuran).clip(CircleShape).background(if (padat) warna else warna.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (padat) IkonIos.aktif(ikon) else ikon,
            null,
            tint = if (padat) Color.White else warna,
            modifier = Modifier.size(ukuran * 0.56f),
        )
    }
}

/** Kartu galat bernada merah dengan tombol "Coba Lagi" opsional — untuk galat di dalam daftar. */
@Composable
fun PanelGalatIos(pesan: String, onCoba: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .permukaanIos()
            .padding(horizontal = UkuranIos.PaddingKartu, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IkonBulatIos(IkonIos.ErrorOutline, NadaIos.BAHAYA.warna, padat = false)
        Spacer(Modifier.width(12.dp))
        Text(pesan, Modifier.weight(1f), style = TipeIos.Catatan.copy(color = NadaIos.BAHAYA.teks))
        if (onCoba != null) {
            Spacer(Modifier.width(8.dp))
            TombolKapsulIos("Coba Lagi", onCoba)
        }
    }
}

// ────────────────────────────────────────────────────────── warna kontrol Material

/**
 * Warna `OutlinedTextField` gaya iOS: latar putih, garis hairline, fokus beraksen.
 * Dipakai supaya kolom formulir yang tetap Material (keyboard, validasi, format angka)
 * serasi dengan kartu iOS. [galat] mewarnai garis merah.
 */
@Composable
fun warnaKolomIos(galat: Boolean = false): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = if (galat) WarnaIos.Merah else WarnaIos.Aksen,
    unfocusedBorderColor = if (galat) WarnaIos.Merah.copy(alpha = 0.6f) else WarnaIos.Pemisah,
    focusedContainerColor = WarnaIos.Kartu,
    unfocusedContainerColor = WarnaIos.Kartu,
    disabledContainerColor = WarnaIos.Kartu,
    errorContainerColor = WarnaIos.Kartu,
    cursorColor = WarnaIos.Aksen,
    focusedLabelColor = WarnaIos.Aksen,
    unfocusedLabelColor = WarnaIos.LabelKedua,
    focusedTextColor = WarnaIos.Label,
    unfocusedTextColor = WarnaIos.Label,
    disabledTextColor = WarnaIos.Label,
    focusedPlaceholderColor = WarnaIos.Abu,
    unfocusedPlaceholderColor = WarnaIos.Abu,
    errorBorderColor = WarnaIos.Merah,
)

/** Warna saklar iOS: hijau saat menyala, abu saat mati. */
@Composable
fun warnaSaklarIos(): SwitchColors = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = WarnaIos.Hijau,
    checkedBorderColor = WarnaIos.Hijau,
    uncheckedThumbColor = Color.White,
    uncheckedTrackColor = WarnaIos.Isian,
    uncheckedBorderColor = Color.Transparent,
)
