package com.sukashawarma.superapp.feature.manager.ui.ceklist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.sukashawarma.superapp.core.ui.ios.LencanaIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.ios.tekanIos
import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.feature.manager.domain.BagianBebas
import com.sukashawarma.superapp.feature.manager.domain.FotoCeklist
import com.sukashawarma.superapp.feature.manager.domain.NilaiCeklist

/** Potongan tampilan yang dipakai layar isian AM dan layar pemantauan RM. */

fun ikonKategori(kunci: String): ImageVector = when (kunci) {
    "kebersihan" -> IkonIos.DeleteSweep
    "stok" -> IkonIos.Inventory2
    "seragam_crew" -> IkonIos.Groups
    "rasa" -> IkonIos.Eco
    "peralatan" -> IkonIos.Settings
    else -> IkonIos.Checklist
}

fun ikonBagian(bagian: BagianBebas): ImageVector = when (bagian) {
    BagianBebas.ONLINE_REVIEW -> IkonIos.Visibility
    BagianBebas.TEMUAN -> IkonIos.WarningAmber
    BagianBebas.PERBAIKAN -> IkonIos.Settings
}

fun warnaBagian(bagian: BagianBebas): Color = when (bagian) {
    BagianBebas.ONLINE_REVIEW -> WarnaIos.Ungu
    BagianBebas.TEMUAN -> WarnaIos.Oranye
    BagianBebas.PERBAIKAN -> WarnaIos.Biru
}

fun nadaNilai(nilai: NilaiCeklist?): NadaIos = when (nilai) {
    NilaiCeklist.BAIK -> NadaIos.SUKSES
    NilaiCeklist.PERHATIAN -> NadaIos.PERINGATAN
    NilaiCeklist.BURUK -> NadaIos.BAHAYA
    null -> NadaIos.NETRAL
}

private fun ikonNilai(nilai: NilaiCeklist): ImageVector = when (nilai) {
    NilaiCeklist.BAIK -> IkonIos.Check
    NilaiCeklist.PERHATIAN -> IkonIos.WarningAmber
    NilaiCeklist.BURUK -> IkonIos.Close
}

/**
 * Tiga tombol nilai berdampingan, masing-masing setinggi jempol (48dp).
 *
 * Tombol yang terpilih terisi warna penuh; yang lain berisi tipis bernada sama,
 * supaya warnanya sudah terbaca SEBELUM diketuk — AM yang berdiri di dapur tidak
 * perlu membaca labelnya.
 */
@Composable
fun PilihanNilai(terpilih: NilaiCeklist?, onPilih: (NilaiCeklist) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NilaiCeklist.entries.forEach { nilai ->
            val nada = nadaNilai(nilai)
            val aktif = nilai == terpilih
            Row(
                Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .tekanIos({ onPilih(nilai) })
                    .clip(UkuranIos.SudutKontrol)
                    .background(if (aktif) nada.warna else nada.warna.copy(alpha = 0.10f))
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    IkonIos.aktif(ikonNilai(nilai)),
                    null,
                    tint = if (aktif) Color.White else nada.teks,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    nilai.label,
                    color = if (aktif) Color.White else nada.teks,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Lencana nilai ringkas: "Baik", "Perhatian", "Buruk", atau "Belum". */
@Composable
fun LencanaNilai(nilai: NilaiCeklist?) {
    LencanaIos(nilai?.label ?: "Belum", nadaNilai(nilai))
}

/**
 * Deret foto satu kategori, bisa digulir menyamping.
 *
 * [onTambah] null = hanya-baca (layar RM). [onHapus] null = tanpa tombol hapus.
 * [onPerluUrl] dipanggil untuk foto yang belum punya URL, sekali per foto yang tampil.
 */
@Composable
fun DeretFoto(
    foto: List<FotoCeklist>,
    mengunggah: Int = 0,
    ukuran: Dp = 88.dp,
    onPerluUrl: (FotoCeklist) -> Unit,
    onBuka: (FotoCeklist) -> Unit,
    onHapus: ((FotoCeklist) -> Unit)? = null,
    onTambah: (() -> Unit)? = null,
    onGaleri: (() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        foto.forEach { f ->
            LaunchedEffect(f.path, f.url) { if (f.url == null) onPerluUrl(f) }
            Box(Modifier.size(ukuran)) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(UkuranIos.SudutBlok)
                        .background(WarnaIos.Isian)
                        .tekanIos({ onBuka(f) }),
                    contentAlignment = Alignment.Center,
                ) {
                    if (f.url != null) {
                        AsyncImage(
                            model = f.url,
                            contentDescription = "Foto bukti",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = WarnaIos.Aksen)
                    }
                }
                if (onHapus != null) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                            .tekanIos({ onHapus(f) }),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(IkonIos.Close, "Hapus foto", tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
        repeat(mengunggah) {
            Box(
                Modifier.size(ukuran).clip(UkuranIos.SudutBlok).background(WarnaIos.Isian),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = WarnaIos.Aksen)
            }
        }
        if (onTambah != null) {
            val kosong = foto.isEmpty() && mengunggah == 0
            PetakTambahFoto(IkonIos.PhotoCamera, if (kosong) "Ambil foto" else "Kamera", kosong, ukuran, onTambah)
            if (onGaleri != null) {
                PetakTambahFoto(IkonIos.Image, "Galeri", kosong, ukuran, onGaleri)
            }
        }
    }
}

@Composable
private fun PetakTambahFoto(ikon: ImageVector, label: String, sorot: Boolean, ukuran: Dp, onKlik: () -> Unit) {
    val warna = if (sorot) WarnaIos.Aksen else WarnaIos.LabelKedua
    Column(
        Modifier
            .size(ukuran)
            .clip(UkuranIos.SudutBlok)
            .background(if (sorot) WarnaIos.Aksen.copy(alpha = 0.10f) else WarnaIos.Isian)
            .border(1.dp, warna.copy(alpha = 0.35f), UkuranIos.SudutBlok)
            .tekanIos(onKlik),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(ikon, null, tint = warna, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, color = warna, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    }
}

/** Foto dibuka besar, latar gelap; ketuk di mana saja untuk menutup. */
@Composable
fun DialogFoto(url: String?, judul: String, onTutup: () -> Unit) {
    Dialog(onDismissRequest = onTutup, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
                .tekanIos(onTutup, skalaTekan = 1f),
            contentAlignment = Alignment.Center,
        ) {
            if (url != null) {
                AsyncImage(
                    model = url,
                    contentDescription = judul,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f),
                )
            } else {
                CircularProgressIndicator(color = Color.White)
            }
            Text(
                judul,
                Modifier.align(Alignment.TopCenter).padding(top = 48.dp, start = 24.dp, end = 24.dp),
                color = Color.White,
                style = TipeIos.Utama.copy(color = Color.White),
            )
            Text(
                "Ketuk untuk menutup",
                Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
            )
        }
    }
}

/** Titik-titik nilai lima kategori — ringkasan sekilas di kartu daftar RM. */
@Composable
fun TitikKategori(nilai: List<Pair<String, NilaiCeklist?>>) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        nilai.forEach { (kunci, n) ->
            val nada = nadaNilai(n)
            Box(
                Modifier.size(26.dp).clip(CircleShape).background(nada.warna.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(ikonKategori(kunci), null, tint = nada.teks, modifier = Modifier.size(14.dp))
            }
        }
    }
}
