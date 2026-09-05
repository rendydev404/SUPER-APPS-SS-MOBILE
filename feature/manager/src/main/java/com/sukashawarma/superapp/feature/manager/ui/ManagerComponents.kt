package com.sukashawarma.superapp.feature.manager.ui

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
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
import com.sukashawarma.superapp.feature.manager.domain.Perubahan
import com.sukashawarma.superapp.presentation.theme.SukaBrown
import com.sukashawarma.superapp.presentation.theme.SukaGray400
import com.sukashawarma.superapp.presentation.theme.SukaOrange

/**
 * Potongan tampilan yang dipakai lebih dari satu layar modul Manager.
 *
 * Nilai warna dan sudut di sini adalah terjemahan langsung kelas Tailwind versi
 * web (`rounded-3xl` = 24dp, `border-suka-brown/10`, dst) supaya dua layar yang
 * menampilkan angka yang sama juga terlihat sama.
 */

/** Garis batas kartu web: `border-suka-brown/10`. */
val GarisKartu = SukaBrown.copy(alpha = 0.10f)

val HijauLatar = Color(0xFFD1FAE5)
val HijauGaris = Color(0xFFA7F3D0)
val HijauTeks = Color(0xFF065F46)
val MerahLatar = Color(0xFFFEE2E2)
val MerahGaris = Color(0xFFFECACA)
val MerahTeks = Color(0xFFB91C1C)

/** Kartu putih bersudut 24dp — bentuk dasar setiap panel di dashboard web. */
@Composable
fun KartuPanel(
    modifier: Modifier = Modifier,
    isi: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        border = BorderStroke(1.dp, GarisKartu),
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.padding(20.dp), content = isi)
    }
}

/** Judul bagian: teks kecil huruf besar dengan hitungan di kanan, seperti header panel web. */
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
    Spacer(Modifier.height(16.dp))
}

/**
 * Kartu KPI: judul, ikon berwarna, angka besar, lalu satu keterangan di kaki kartu.
 * [kaki] dibiarkan bebas karena tiap KPI menutupnya dengan hal berbeda — ada yang
 * badge perubahan, ada yang tautan ke layar waste.
 */
@Composable
fun KartuKpi(
    judul: String,
    nilai: String,
    satuan: String? = null,
    ikon: ImageVector,
    warnaIkon: Color,
    warnaNilai: Color = SukaBrown,
    kaki: @Composable () -> Unit,
) {
    KartuPanel {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                judul.uppercase(),
                Modifier.weight(1f),
                color = SukaGray400,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp,
            )
            Box(
                Modifier.size(44.dp).background(warnaIkon.copy(alpha = 0.10f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(ikon, null, tint = warnaIkon, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                nilai,
                color = warnaNilai,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (satuan != null) {
                Spacer(Modifier.width(6.dp))
                Text(
                    satuan,
                    Modifier.padding(bottom = 4.dp),
                    color = SukaGray400,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = GarisKartu)
        Spacer(Modifier.height(10.dp))
        kaki()
    }
}

/** Badge "+12,5% dari periode sebelumnya" — hijau saat naik, merah saat turun. */
@Composable
fun BadgePerubahan(perubahan: Perubahan, keterangan: String = "dari periode sebelumnya") {
    val naik = perubahan.naik
    Surface(
        shape = RoundedCornerShape(50),
        color = if (naik) HijauLatar else MerahLatar,
        border = BorderStroke(1.dp, if (naik) HijauGaris else MerahGaris),
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (naik) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                null,
                tint = if (naik) HijauTeks else MerahTeks,
                modifier = Modifier.size(13.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                "${if (naik) "+" else "-"}${perubahan.besaranTeks}% $keterangan",
                color = if (naik) HijauTeks else MerahTeks,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

/** Keterangan kaki kartu yang netral — pil abu-abu bertulisan pendek. */
@Composable
fun PilKeterangan(teks: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = SukaBrown.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, GarisKartu),
    ) {
        Text(
            teks,
            Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            color = SukaBrown.copy(alpha = 0.7f),
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

/** Bilah proporsi omzet. Tiga teratas memakai gradasi jingga, sisanya cokelat pudar. */
@Composable
fun BarProgres(
    rasio: Float,
    modifier: Modifier = Modifier,
    tinggi: Int = 8,
    sorot: Boolean = true,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(tinggi.dp)
            .clip(RoundedCornerShape(50))
            .background(SukaBrown.copy(alpha = 0.10f)),
    ) {
        Box(
            Modifier
                .fillMaxWidth(rasio.coerceIn(0f, 1f))
                .height(tinggi.dp)
                .clip(RoundedCornerShape(50))
                .background(
                    if (sorot) {
                        Brush.horizontalGradient(listOf(SukaOrange, Color(0xFFF59E0B)))
                    } else {
                        Brush.horizontalGradient(listOf(SukaBrown.copy(alpha = 0.3f), SukaBrown.copy(alpha = 0.3f)))
                    }
                ),
        )
    }
}

/** Lencana peringkat: emas, perak, perunggu, lalu netral — urutan warna sama dengan web. */
@Composable
fun LencanaPeringkat(peringkat: Int, ukuran: Int = 26) {
    val (latar, teks, garis) = when (peringkat) {
        1 -> Triple(Color(0xFFF59E0B), Color.White, Color(0xFFD97706))
        2 -> Triple(Color(0xFF334155), Color.White, Color(0xFF1E293B))
        3 -> Triple(Color(0xFF92400E), Color.White, Color(0xFF78350F))
        else -> Triple(SukaBrown.copy(alpha = 0.05f), SukaBrown.copy(alpha = 0.6f), GarisKartu)
    }
    Surface(
        shape = CircleShape,
        color = latar,
        border = BorderStroke(1.dp, garis),
        modifier = Modifier.size(ukuran.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text("#$peringkat", color = teks, fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
    }
}

/** Chip kecil berlatar jingga muda — dipakai untuk nama zona dan label role. */
@Composable
fun ChipJingga(teks: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = SukaOrange.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, SukaOrange.copy(alpha = 0.20f)),
    ) {
        Text(
            teks,
            Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            color = SukaOrange,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.6.sp,
        )
    }
}

/** Baris kosong yang seragam untuk panel tanpa data. */
@Composable
fun PanelKosong(teks: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
        Text(teks, color = SukaGray400, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

/** Ringkasan mini di dalam panel zona: label kecil, angka, dan keterangan. */
@Composable
fun KartuRingkasZona(
    label: String,
    nilai: String,
    keterangan: String,
    ikon: ImageVector,
    warna: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = warna.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, warna.copy(alpha = 0.20f)),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    label.uppercase(),
                    Modifier.weight(1f),
                    color = warna,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.6.sp,
                )
                Icon(ikon, null, tint = warna, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(
                nilai,
                color = SukaBrown,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(keterangan, color = warna.copy(alpha = 0.85f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** Jarak antar panel di dashboard — `space-y-6` web. */
@Composable
fun JarakPanel() = Spacer(Modifier.height(16.dp))
