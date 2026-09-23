package com.sukashawarma.superapp.feature.leader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
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
import com.sukashawarma.superapp.core.ui.ios.BilahJudulIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.TombolBundarIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.ios.permukaanIos
import com.sukashawarma.superapp.core.ui.kaca.IkonIos

/**
 * Potongan tampilan yang dipakai lebih dari satu layar modul Leader.
 *
 * Seluruhnya disusun dari token design system iOS di `core.ui.ios`, jadi kartu di sini
 * serasi dengan modul lain walau tata letaknya tetap mengikuti halaman leader web.
 * Yang ada di berkas ini hanyalah bentuk yang TIDAK disediakan core — petak angka
 * berketerangan, bilah progres, dan keadaan kosong di dalam daftar.
 *
 * Sengaja tidak berbagi berkas dengan `feature/manager`: kedua modul tidak saling
 * bergantung, pola yang sama dipakai `ModulAkses` di beranda. Satu himpunan kecil
 * yang diduplikasi lebih murah daripada satu modul yang menarik seluruh modul lain
 * hanya demi sebuah kartu putih.
 */

/**
 * Bilah judul keempat layar Leader: kembali di kiri, muat ulang di kanan.
 *
 * Satu tempat supaya keempat tab tidak bisa berbeda tinggi atau letak tombolnya —
 * berpindah tab yang judulnya melompat terasa seperti membuka aplikasi lain.
 */
@Composable
fun BilahJudulLeader(judul: String, onKembali: () -> Unit, onMuatUlang: () -> Unit) {
    BilahJudulIos(judul = judul, onKembali = onKembali) {
        TombolBundarIos(IkonIos.Refresh, "Muat ulang", onMuatUlang)
    }
}

/**
 * Petak angka: ikon dalam lingkaran berwarna dan label, angka besar, lalu satu baris
 * keterangan — bahasa [com.sukashawarma.superapp.core.ui.ios.PetakStatIos], tetapi
 * angkanya di baris sendiri. Nominal rupiah terlalu lebar untuk diletakkan di samping
 * ikon pada petak selebar setengah layar, dan keterangannya membawa informasi yang
 * tidak boleh hilang ("3 menipis", "terakhir 14.05").
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
    warnaNilai: Color = WarnaIos.Label,
    ukuranNilai: Int = 26,
    tambahan: @Composable (() -> Unit)? = null,
) {
    Column(
        modifier
            .fillMaxWidth()
            .permukaanIos(UkuranIos.SudutPetak)
            .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(30.dp).clip(CircleShape).background(warnaIkon),
                contentAlignment = Alignment.Center,
            ) {
                Icon(IkonIos.aktif(ikon), null, tint = Color.White, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(
                judul,
                Modifier.weight(1f),
                color = WarnaIos.LabelKedua,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 17.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            nilai,
            style = TipeIos.AngkaBesar.copy(fontSize = ukuranNilai.sp, color = warnaNilai),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        Text(keterangan, style = TipeIos.Catatan, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (tambahan != null) {
            Spacer(Modifier.height(8.dp))
            tambahan()
        }
    }
}

/** Bilah pencapaian target. Hijau begitu target terlampaui, aksen selama masih di jalan. */
@Composable
fun BarProgres(rasio: Float, tercapai: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(UkuranIos.SudutKapsul)
            .background(WarnaIos.Isian),
    ) {
        Box(
            Modifier
                .fillMaxWidth(rasio.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(UkuranIos.SudutKapsul)
                .background(if (tercapai) WarnaIos.Hijau else WarnaIos.Aksen),
        )
    }
}

/**
 * Keadaan kosong yang seragam untuk daftar dan panel tanpa data.
 *
 * Versi ringkas `KeadaanIos`: hanya satu kalimat, karena di sini kekosongan adalah
 * bagian dari layar yang tetap berisi, bukan seluruh layar yang gagal.
 */
@Composable
fun PanelKosong(teks: String, ikon: ImageVector = IkonIos.Inbox) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(48.dp).clip(CircleShape).background(WarnaIos.Isian),
            contentAlignment = Alignment.Center,
        ) {
            Icon(ikon, null, tint = WarnaIos.Abu, modifier = Modifier.size(23.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(teks, style = TipeIos.SubJudul, textAlign = TextAlign.Center)
    }
}
