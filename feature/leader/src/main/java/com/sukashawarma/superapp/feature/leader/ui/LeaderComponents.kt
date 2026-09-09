package com.sukashawarma.superapp.feature.leader.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sukashawarma.superapp.presentation.theme.SukaBrown
import com.sukashawarma.superapp.presentation.theme.SukaGray400
import com.sukashawarma.superapp.presentation.theme.SukaOrange

/**
 * Potongan tampilan yang dipakai lebih dari satu layar modul Leader.
 *
 * Nilai warna dan sudutnya adalah terjemahan langsung kelas Tailwind halaman web
 * (`rounded-[20px]`, `ring-1 ring-slate-100`, dst) supaya dua layar yang
 * menampilkan angka yang sama juga terlihat sama.
 *
 * Sengaja tidak berbagi berkas dengan `feature/manager`: kedua modul tidak saling
 * bergantung, pola yang sama dipakai `ModulAkses` di beranda. Satu himpunan kecil
 * yang diduplikasi lebih murah daripada satu modul yang menarik seluruh modul lain
 * hanya demi sebuah kartu putih.
 */

/** Garis batas kartu: `ring-slate-100` web. */
val GarisKartu = SukaBrown.copy(alpha = 0.10f)

val HijauLatar = Color(0xFFD1FAE5)
val HijauGaris = Color(0xFFA7F3D0)
val HijauTeks = Color(0xFF065F46)
val MerahLatar = Color(0xFFFEE2E2)
val MerahGaris = Color(0xFFFECACA)
val MerahTeks = Color(0xFFB91C1C)
val AmberLatar = Color(0xFFFEF3C7)
val AmberGaris = Color(0xFFFCD34D)
val AmberTeks = Color(0xFF92400E)
val BiruLatar = Color(0xFFDBEAFE)
val BiruGaris = Color(0xFFBFDBFE)
val BiruTeks = Color(0xFF1D4ED8)

/** Kartu putih bersudut 20dp — bentuk dasar setiap panel di halaman leader web. */
@Composable
fun KartuPanel(
    modifier: Modifier = Modifier,
    isi: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, GarisKartu),
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.padding(18.dp), content = isi)
    }
}

/** Kepala panel: judul huruf besar dengan keterangan opsional di kanan. */
@Composable
fun JudulPanel(judul: String, keterangan: String? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            judul.uppercase(),
            Modifier.weight(1f),
            color = SukaBrown,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.8.sp,
        )
        if (keterangan != null) {
            Text(keterangan, color = SukaGray400, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
    Spacer(Modifier.height(12.dp))
    HorizontalDivider(color = GarisKartu)
    Spacer(Modifier.height(14.dp))
}

/**
 * Kartu angka: label kecil, ikon berwarna, angka besar, lalu satu baris keterangan.
 *
 * [warnaNilai] dipisah dari [warnaIkon] karena saldo petty cash yang kritis berubah
 * merah sementara ikonnya tetap hijau — persis seperti di web.
 */
@Composable
fun KartuAngka(
    judul: String,
    nilai: String,
    keterangan: String,
    ikon: ImageVector,
    warnaIkon: Color,
    modifier: Modifier = Modifier,
    warnaNilai: Color = SukaBrown,
    ukuranNilai: Int = 26,
    tambahan: @Composable (() -> Unit)? = null,
) {
    KartuPanel(modifier) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Text(
                judul.uppercase(),
                Modifier.weight(1f).padding(end = 8.dp),
                color = SukaGray400,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.7.sp,
            )
            Box(
                Modifier.size(34.dp).background(warnaIkon.copy(alpha = 0.10f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(ikon, null, tint = warnaIkon, modifier = Modifier.size(17.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            nilai,
            color = warnaNilai,
            fontSize = ukuranNilai.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            keterangan,
            color = SukaGray400,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (tambahan != null) {
            Spacer(Modifier.height(8.dp))
            tambahan()
        }
    }
}

/** Pil status berwarna — dipakai badge pengajuan, status pesanan, dan tingkat stok. */
@Composable
fun PilStatus(
    teks: String,
    latar: Color,
    garis: Color,
    warnaTeks: Color,
    ikon: ImageVector? = null,
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = latar,
        border = BorderStroke(1.dp, garis),
    ) {
        Row(
            Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (ikon != null) {
                Icon(ikon, null, tint = warnaTeks, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
            }
            Text(
                teks.uppercase(),
                color = warnaTeks,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.6.sp,
                maxLines = 1,
            )
        }
    }
}

/** Bilah pencapaian target. Hijau begitu target terlampaui, jingga selama masih di jalan. */
@Composable
fun BarProgres(rasio: Float, tercapai: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(RoundedCornerShape(50))
            .background(SukaBrown.copy(alpha = 0.08f)),
    ) {
        Box(
            Modifier
                .fillMaxWidth(rasio.coerceIn(0f, 1f))
                .height(12.dp)
                .clip(RoundedCornerShape(50))
                .background(
                    if (tercapai) {
                        Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFF059669)))
                    } else {
                        Brush.horizontalGradient(listOf(SukaOrange, Color(0xFFF59E0B)))
                    }
                ),
        )
    }
}

/** Baris kosong yang seragam untuk panel tanpa data. */
@Composable
fun PanelKosong(teks: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 28.dp), contentAlignment = Alignment.Center) {
        Text(
            teks,
            color = SukaGray400,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

/** Jarak antar panel — `space-y-6` web. */
@Composable
fun JarakPanel() = Spacer(Modifier.height(16.dp))
