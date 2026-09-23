package com.sukashawarma.superapp.feature.manager.ui

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sukashawarma.superapp.core.ui.ios.IkonBulatIos
import com.sukashawarma.superapp.core.ui.ios.LencanaIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.ios.permukaanIos
import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.feature.manager.domain.Perubahan

/**
 * Potongan tampilan yang dipakai lebih dari satu layar modul Manager.
 *
 * Semuanya dibangun di atas design system iOS (`core.ui.ios`) supaya dashboard
 * Manager terlihat satu keluarga dengan modul lain walau tata letaknya berbeda.
 * Nama dan parameter fungsi lama dipertahankan agar layar tidak perlu tahu
 * gayanya berganti.
 */

/**
 * Garis pemisah hairline. Namanya warisan dari garis tepi kartu gaya web; kartu
 * iOS tidak lagi bergaris tepi, jadi nilai ini kini hanya untuk pemisah di dalam kartu.
 */
val GarisKartu = WarnaIos.Pemisah

// Nada hijau/merah untuk angka naik-turun dan latar peringatan. Diturunkan dari
// [NadaIos] supaya merah "rugi" di Manager sama dengan merah "kritis" di Stok.
val HijauLatar = NadaIos.SUKSES.warna.copy(alpha = 0.12f)
val HijauGaris = NadaIos.SUKSES.warna.copy(alpha = 0.24f)
val HijauTeks = NadaIos.SUKSES.teks
val MerahLatar = NadaIos.BAHAYA.warna.copy(alpha = 0.10f)
val MerahGaris = NadaIos.BAHAYA.warna.copy(alpha = 0.22f)
val MerahTeks = NadaIos.BAHAYA.teks

/** Kartu putih iOS — bentuk dasar setiap panel dashboard: sudut 20, bayangan lembut, tanpa garis tepi. */
@Composable
fun KartuPanel(
    modifier: Modifier = Modifier,
    isi: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .permukaanIos()
            .padding(UkuranIos.PaddingKartu),
        content = isi,
    )
}

/** Judul panel: judul kartu iOS dengan hitungan abu di kanan. */
@Composable
fun JudulPanel(judul: String, keterangan: String? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(judul, Modifier.weight(1f), style = TipeIos.Utama, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (keterangan != null) {
            Spacer(Modifier.width(8.dp))
            Text(keterangan, style = TipeIos.Catatan, maxLines = 1)
        }
    }
    Spacer(Modifier.height(12.dp))
}

/**
 * Kartu KPI: ikon dalam lingkaran berwarna, label, angka besar, lalu satu keterangan
 * di kaki kartu. [kaki] dibiarkan bebas karena tiap KPI menutupnya dengan hal
 * berbeda — ada yang lencana perubahan, ada yang tautan ke layar waste.
 */
@Composable
fun KartuKpi(
    judul: String,
    nilai: String,
    satuan: String? = null,
    ikon: ImageVector,
    warnaIkon: Color,
    warnaNilai: Color = WarnaIos.Label,
    kaki: @Composable () -> Unit,
) {
    KartuPanel {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IkonBulatIos(ikon, warnaIkon)
            Spacer(Modifier.width(10.dp))
            Text(
                judul,
                Modifier.weight(1f),
                color = WarnaIos.LabelKedua,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                nilai,
                Modifier.weight(1f, fill = false),
                style = TipeIos.AngkaBesar.copy(color = warnaNilai),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (satuan != null) {
                Spacer(Modifier.width(5.dp))
                Text(satuan, Modifier.padding(bottom = 4.dp), style = TipeIos.Catatan, maxLines = 1)
            }
        }
        Spacer(Modifier.height(10.dp))
        kaki()
    }
}

/** Lencana "+12,5% dari periode sebelumnya" — hijau saat naik, merah saat turun. */
@Composable
fun BadgePerubahan(perubahan: Perubahan, keterangan: String = "dari periode sebelumnya") {
    val naik = perubahan.naik
    LencanaIos(
        "${if (naik) "+" else "-"}${perubahan.besaranTeks}% $keterangan",
        if (naik) NadaIos.SUKSES else NadaIos.BAHAYA,
        ikon = if (naik) IkonIos.ArrowUpward else IkonIos.ArrowDownward,
    )
}

/** Keterangan kaki kartu yang netral — lencana abu bertulisan pendek. */
@Composable
fun PilKeterangan(teks: String) {
    LencanaIos(teks, NadaIos.NETRAL, titik = false)
}

/**
 * Bilah proporsi. Tiga teratas memakai aksen, sisanya abu — warna padat, bukan
 * gradasi, supaya daftar panjang tidak membuat shader baru per baris.
 */
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
            .clip(UkuranIos.SudutKapsul)
            .background(WarnaIos.Isian),
    ) {
        Box(
            Modifier
                .fillMaxWidth(rasio.coerceIn(0f, 1f))
                .height(tinggi.dp)
                .clip(UkuranIos.SudutKapsul)
                .background(if (sorot) WarnaIos.Aksen else WarnaIos.Abu.copy(alpha = 0.45f)),
        )
    }
}

/** Lencana peringkat: emas, perak, perunggu, lalu netral — urutan sama dengan web. */
@Composable
fun LencanaPeringkat(peringkat: Int, ukuran: Int = 26) {
    val (latar, teks) = when (peringkat) {
        1 -> Color(0xFFFFB300) to Color.White
        2 -> WarnaIos.Abu to Color.White
        3 -> Color(0xFFB07A4F) to Color.White
        else -> WarnaIos.Isian to WarnaIos.LabelKedua
    }
    Box(
        Modifier.size(ukuran.dp).clip(CircleShape).background(latar),
        contentAlignment = Alignment.Center,
    ) {
        Text("#$peringkat", color = teks, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

/** Lencana beraksen — dipakai untuk nama zona dan label role. */
@Composable
fun ChipJingga(teks: String) {
    LencanaIos(teks, NadaIos.AKSEN, titik = false)
}

/** Baris kosong yang seragam untuk panel tanpa data. */
@Composable
fun PanelKosong(teks: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 28.dp), contentAlignment = Alignment.Center) {
        Text(teks, style = TipeIos.SubJudul, textAlign = TextAlign.Center)
    }
}

/** Ringkasan mini di dalam panel: blok abu berisi ikon bernada, label, angka, dan keterangan. */
@Composable
fun KartuRingkasZona(
    label: String,
    nilai: String,
    keterangan: String,
    ikon: ImageVector,
    warna: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .clip(UkuranIos.SudutBlok)
            .background(WarnaIos.Latar)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IkonBulatIos(ikon, warna, ukuran = 24.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                label,
                Modifier.weight(1f),
                style = TipeIos.Catatan.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(nilai, style = TipeIos.Angka, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(2.dp))
        Text(keterangan, style = TipeIos.Kecil, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** Jarak antar panel di dashboard. */
@Composable
fun JarakPanel() = Spacer(Modifier.height(UkuranIos.JarakKartu))
