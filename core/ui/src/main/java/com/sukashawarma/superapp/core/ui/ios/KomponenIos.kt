package com.sukashawarma.superapp.core.ui.ios

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sukashawarma.superapp.core.ui.kaca.IkonIos

// ─────────────────────────────────────────────────────────────── modifier dasar

/**
 * Umpan balik tekan ala iOS: elemen sedikit mengecil lalu memantul, tanpa riak
 * Material yang asing di iOS. Hanya mengubah `graphicsLayer` (transform), jadi tidak
 * memicu ukur/tata letak ulang — aman dipakai di daftar panjang.
 */
@Composable
fun Modifier.tekanIos(
    onKlik: () -> Unit,
    aktif: Boolean = true,
    skalaTekan: Float = 0.97f,
    peran: Role? = Role.Button,
): Modifier {
    val interaksi = remember { MutableInteractionSource() }
    val ditekan by interaksi.collectIsPressedAsState()
    val skala by animateFloatAsState(
        if (ditekan) skalaTekan else 1f,
        spring(dampingRatio = 0.6f, stiffness = 700f),
        label = "tekanIos",
    )
    return this
        .graphicsLayer {
            scaleX = skala
            scaleY = skala
        }
        .clickable(interaksi, indication = null, enabled = aktif, role = peran, onClick = onKlik)
}

/**
 * Bayangan lembut kartu iOS: lebar dan tipis, bukan elevation Material yang tajam.
 * Bayangan outline dikerjakan RenderThread, jadi murah untuk kartu di daftar.
 */
fun Modifier.bayanganIos(bentuk: Shape = UkuranIos.SudutKartu, tinggi: Dp = 10.dp): Modifier = shadow(
    elevation = tinggi,
    shape = bentuk,
    ambientColor = Color.Black.copy(alpha = 0.04f),
    spotColor = Color.Black.copy(alpha = 0.10f),
)

/** Permukaan kartu iOS lengkap: bayangan lembut, sudut, latar putih. */
fun Modifier.permukaanIos(bentuk: Shape = UkuranIos.SudutKartu, latar: Color = WarnaIos.Kartu): Modifier =
    bayanganIos(bentuk).clip(bentuk).background(latar)

// ─────────────────────────────────────────────────────────────────────── kartu

/**
 * Kartu iOS — wadah dasar untuk hampir semua blok isi.
 *
 * @param onKlik null untuk kartu diam; bila ada, kartu memakai [tekanIos].
 */
@Composable
fun KartuIos(
    modifier: Modifier = Modifier,
    onKlik: (() -> Unit)? = null,
    padding: PaddingValues = PaddingValues(UkuranIos.PaddingKartu),
    latar: Color = WarnaIos.Kartu,
    isi: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .permukaanIos(UkuranIos.SudutKartu, latar)
            .then(if (onKlik != null) Modifier.tekanIos(onKlik) else Modifier)
            .padding(padding),
        content = isi,
    )
}

/**
 * Grup "inset grouped" iOS: satu kartu putih berisi beberapa [BarisIos] yang
 * dipisah garis hairline. Pemisah dipasang lewat [PemisahIos] di antara baris.
 */
@Composable
fun GrupIos(
    modifier: Modifier = Modifier,
    judul: String? = null,
    catatan: String? = null,
    isi: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth()) {
        if (judul != null) LabelSeksiIos(judul, Modifier.padding(start = 16.dp, bottom = 7.dp))
        Column(Modifier.fillMaxWidth().permukaanIos(UkuranIos.SudutGrup), content = isi)
        if (catatan != null) {
            Text(catatan, Modifier.padding(start = 16.dp, end = 16.dp, top = 7.dp), style = TipeIos.Catatan)
        }
    }
}

/** Garis pemisah hairline antar baris dalam [GrupIos]; [inset] menyamai awal teks baris. */
@Composable
fun PemisahIos(inset: Dp = 16.dp) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = inset)
            .height(0.5.dp)
            .background(WarnaIos.Pemisah),
    )
}

/**
 * Satu baris daftar iOS: ikon (opsional, dalam lingkaran bernada), judul, keterangan,
 * nilai di kanan, dan chevron bila bisa ditekan.
 */
@Composable
fun BarisIos(
    judul: String,
    modifier: Modifier = Modifier,
    keterangan: String? = null,
    ikon: ImageVector? = null,
    nadaIkon: NadaIos = NadaIos.AKSEN,
    nilai: String? = null,
    onKlik: (() -> Unit)? = null,
    chevron: Boolean = onKlik != null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onKlik != null) Modifier.clickable(onClick = onKlik) else Modifier)
            .heightIn(min = 48.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (ikon != null) {
            Box(
                Modifier.size(30.dp).clip(CircleShape).background(nadaIkon.warna.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(ikon, null, tint = nadaIkon.warna, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(judul, style = TipeIos.Isi, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (keterangan != null) {
                Text(keterangan, style = TipeIos.Catatan, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        if (nilai != null) {
            Spacer(Modifier.width(8.dp))
            Text(nilai, style = TipeIos.Keterangan.copy(color = WarnaIos.LabelKedua), maxLines = 1)
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
        if (chevron) {
            Spacer(Modifier.width(6.dp))
            Icon(IkonIos.ChevronRight, null, tint = WarnaIos.LabelKetiga, modifier = Modifier.size(15.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────── judul seksi

/** Judul seksi besar (Title 3) dengan keterangan abu di kanan, mis. "Food & Beverage · 18 item". */
@Composable
fun JudulSeksiIos(
    judul: String,
    modifier: Modifier = Modifier,
    keterangan: String? = null,
    aksi: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth().padding(top = 14.dp, bottom = 2.dp, start = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(judul, Modifier.weight(1f), style = TipeIos.Judul3.copy(fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (keterangan != null) Text(keterangan, style = TipeIos.SubJudul.copy(fontSize = 14.sp))
        if (aksi != null) aksi()
    }
}

/** Label seksi kecil huruf kapital di atas grup, seperti di aplikasi Pengaturan. */
@Composable
fun LabelSeksiIos(teks: String, modifier: Modifier = Modifier) {
    Text(teks.uppercase(), modifier, style = TipeIos.Catatan.copy(letterSpacing = 0.3.sp))
}

// ─────────────────────────────────────────────────────────────────────── lencana

/** Lencana kapsul iOS: isian tipis bernada, titik, teks berwarna — bukan huruf kapital. */
@Composable
fun LencanaIos(
    teks: String,
    nada: NadaIos,
    modifier: Modifier = Modifier,
    titik: Boolean = true,
    ikon: ImageVector? = null,
) {
    Row(
        modifier
            .clip(UkuranIos.SudutKapsul)
            .background(nada.warna.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            ikon != null -> {
                Icon(ikon, null, tint = nada.teks, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(5.dp))
            }
            titik -> {
                Box(Modifier.size(7.dp).clip(CircleShape).background(nada.warna))
                Spacer(Modifier.width(6.dp))
            }
        }
        Text(teks, color = nada.teks, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

// ───────────────────────────────────────────────────────────── statistik & angka

/**
 * Petak ringkasan ala "smart list" aplikasi Pengingat iOS: ikon dalam lingkaran
 * berwarna, angka besar di kanan, label di bawah. [aktif] mengisi petak dengan warnanya.
 */
@Composable
fun PetakStatIos(
    label: String,
    nilai: String,
    ikon: ImageVector,
    warna: Color,
    modifier: Modifier = Modifier,
    aktif: Boolean = false,
    onKlik: (() -> Unit)? = null,
) {
    val latar by animateColorAsState(if (aktif) warna else WarnaIos.Kartu, label = "latarPetak")
    val teks by animateColorAsState(if (aktif) Color.White else WarnaIos.Label, label = "teksPetak")
    Column(
        modifier
            .permukaanIos(UkuranIos.SudutPetak, latar)
            .then(if (onKlik != null) Modifier.tekanIos(onKlik) else Modifier)
            .padding(start = 12.dp, end = 14.dp, top = 12.dp, bottom = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                Modifier.size(30.dp).clip(CircleShape).background(if (aktif) Color.White else warna),
                contentAlignment = Alignment.Center,
            ) {
                Icon(IkonIos.aktif(ikon), null, tint = if (aktif) warna else Color.White, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.weight(1f))
            Text(nilai, style = TipeIos.AngkaBesar.copy(color = teks), maxLines = 1)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            color = if (aktif) Color.White.copy(alpha = 0.92f) else WarnaIos.LabelKedua,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Blok angka abu bersekat — beberapa [KolomAngkaIos] berdampingan, seperti
 * ringkasan di aplikasi Kesehatan/Cuaca iOS. Pemisah ditambahkan otomatis.
 */
@Composable
fun BlokAngkaIos(
    kolom: List<AngkaIos>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(UkuranIos.SudutBlok)
            .background(WarnaIos.Latar)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        kolom.forEachIndexed { i, k ->
            if (i > 0) Box(Modifier.width(0.5.dp).height(30.dp).background(WarnaIos.Pemisah))
            KolomAngkaIos(k, Modifier.weight(1f))
        }
    }
}

/** Satu angka di [BlokAngkaIos]. [negatif] mewarnai merah. Angka "—" tidak diberi satuan. */
data class AngkaIos(val label: String, val angka: String, val satuan: String = "", val negatif: Boolean = false)

@Composable
fun KolomAngkaIos(angka: AngkaIos, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(angka.label, style = TipeIos.Kecil.copy(fontWeight = FontWeight.Medium), textAlign = TextAlign.Center, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(angka.angka, style = TipeIos.Angka.copy(color = if (angka.negatif) WarnaIos.Merah else WarnaIos.Label), maxLines = 1)
            if (angka.satuan.isNotEmpty() && angka.angka != "—") {
                Spacer(Modifier.width(3.dp))
                Text(
                    angka.satuan,
                    Modifier.padding(bottom = 2.dp),
                    color = if (angka.negatif) WarnaIos.Merah.copy(alpha = 0.8f) else WarnaIos.LabelKedua,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────── kontrol

/** Kolom cari iOS: isian abu tanpa garis tepi, kaca pembesar abu, tombol hapus bulat. */
@Composable
fun KolomCariIos(
    nilai: String,
    onUbah: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Cari",
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    BasicTextField(
        value = nilai,
        onValueChange = onUbah,
        singleLine = true,
        textStyle = TipeIos.Keterangan,
        cursorBrush = SolidColor(WarnaIos.Aksen),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        modifier = modifier,
        decorationBox = { isi ->
            Row(
                Modifier
                    .height(UkuranIos.TinggiKontrol)
                    .clip(UkuranIos.SudutKontrol)
                    .background(WarnaIos.Isian)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(IkonIos.Search, null, tint = WarnaIos.Abu, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Box(Modifier.weight(1f)) {
                    if (nilai.isEmpty()) {
                        Text(placeholder, style = TipeIos.Keterangan.copy(color = WarnaIos.Abu), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    isi()
                }
                if (nilai.isNotEmpty()) {
                    Box(
                        Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(WarnaIos.Abu.copy(alpha = 0.7f))
                            .clickable { onUbah("") },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(IkonIos.Close, "Hapus", tint = Color.White, modifier = Modifier.size(11.dp))
                    }
                }
            }
        },
    )
}

/** Tombol ikon bulat ala toolbar iOS: lingkaran abu, ikon beraksen; [aktif] false meredupkannya. */
@Composable
fun TombolBundarIos(
    ikon: ImageVector,
    keterangan: String,
    onKlik: () -> Unit,
    modifier: Modifier = Modifier,
    warnaIkon: Color = WarnaIos.Aksen,
    aktif: Boolean = true,
) {
    Box(
        modifier.size(38.dp).clip(CircleShape).background(WarnaIos.Isian).tekanIos(onKlik, aktif = aktif),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            ikon,
            contentDescription = keterangan,
            tint = if (aktif) warnaIkon else WarnaIos.LabelKetiga,
            modifier = Modifier.size(19.dp),
        )
    }
}

/** Tombol kapsul abu berteks aksen — gaya "pull-down button" iOS, untuk pemicu menu/filter. */
@Composable
fun TombolKapsulIos(
    teks: String,
    onKlik: () -> Unit,
    modifier: Modifier = Modifier,
    ikon: ImageVector? = null,
    chevron: Boolean = false,
) {
    Row(
        modifier
            .height(UkuranIos.TinggiKontrol)
            .clip(UkuranIos.SudutKontrol)
            .background(WarnaIos.Isian)
            .tekanIos(onKlik)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (ikon != null) {
            Icon(ikon, null, tint = WarnaIos.Aksen, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(teks, color = WarnaIos.Aksen, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (chevron) {
            Spacer(Modifier.width(4.dp))
            Icon(IkonIos.ArrowDropDown, null, tint = WarnaIos.Aksen, modifier = Modifier.size(14.dp))
        }
    }
}

/**
 * Tombol aksi utama iOS: kapsul membulat terisi aksen, tinggi 50dp. [memuat] mengganti
 * teks dengan pemutar dan menonaktifkan tombol supaya tidak terkirim dua kali.
 */
@Composable
fun TombolUtamaIos(
    teks: String,
    onKlik: () -> Unit,
    modifier: Modifier = Modifier,
    aktif: Boolean = true,
    memuat: Boolean = false,
    ikon: ImageVector? = null,
    warna: Color = WarnaIos.Aksen,
) {
    val bisa = aktif && !memuat
    Row(
        modifier
            .fillMaxWidth()
            .height(UkuranIos.TinggiTombol)
            .clip(UkuranIos.SudutBlok)
            .background(if (bisa) warna else WarnaIos.Isian)
            .tekanIos(onKlik, aktif = bisa),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (memuat) {
            CircularProgressIndicator(Modifier.size(20.dp), color = WarnaIos.Abu, strokeWidth = 2.dp)
        } else {
            if (ikon != null) {
                Icon(ikon, null, tint = if (bisa) Color.White else WarnaIos.Abu, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(teks, color = if (bisa) Color.White else WarnaIos.Abu, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Tombol sekunder iOS: isian aksen tipis, teks aksen. Untuk aksi pendamping tombol utama. */
@Composable
fun TombolKeduaIos(
    teks: String,
    onKlik: () -> Unit,
    modifier: Modifier = Modifier,
    aktif: Boolean = true,
    ikon: ImageVector? = null,
    warna: Color = WarnaIos.Aksen,
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(UkuranIos.TinggiTombol)
            .clip(UkuranIos.SudutBlok)
            .background(warna.copy(alpha = if (aktif) 0.12f else 0.06f))
            .tekanIos(onKlik, aktif = aktif),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (ikon != null) {
            Icon(ikon, null, tint = warna.copy(alpha = if (aktif) 1f else 0.4f), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(teks, color = warna.copy(alpha = if (aktif) 1f else 0.4f), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ─────────────────────────────────────────────────────────────────── bilah judul

/**
 * Bilah judul iOS: tombol kembali bulat di kiri, judul di tengah, aksi bulat di kanan.
 * Latarnya sama dengan latar halaman, dengan garis hairline tipis di bawah.
 */
@Composable
fun BilahJudulIos(
    judul: String,
    modifier: Modifier = Modifier,
    subjudul: String? = null,
    onKembali: (() -> Unit)? = null,
    latar: Color = WarnaIos.Latar,
    garisBawah: Boolean = true,
    aksi: @Composable RowScope.() -> Unit = {},
) {
    Column(modifier.fillMaxWidth().background(latar)) {
        Row(
            Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .heightIn(min = 54.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onKembali != null) {
                TombolBundarIos(IkonIos.ArrowBack, "Kembali", onKembali)
                Spacer(Modifier.width(10.dp))
            } else {
                Spacer(Modifier.width(4.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(judul, style = TipeIos.Utama, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subjudul != null) {
                    Text(subjudul, style = TipeIos.Catatan, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, content = aksi)
        }
        if (garisBawah) {
            Box(Modifier.fillMaxWidth().height(0.5.dp).drawBehind {
                drawLine(WarnaIos.Pemisah, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = size.height)
            })
        }
    }
}

// ────────────────────────────────────────────────────────────────────── keadaan

/** Keadaan kosong/gagal/tanpa akses ala iOS: ikon dalam lingkaran, judul, pesan, aksi opsional. */
@Composable
fun KeadaanIos(
    ikon: ImageVector,
    judul: String,
    pesan: String,
    modifier: Modifier = Modifier,
    nada: NadaIos = NadaIos.NETRAL,
    teksAksi: String? = null,
    onAksi: (() -> Unit)? = null,
) {
    Column(
        modifier.fillMaxWidth().heightIn(min = 240.dp).padding(horizontal = 32.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(64.dp).clip(CircleShape).background(nada.warna.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(ikon, null, tint = nada.warna, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text(judul, style = TipeIos.Utama, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(pesan, style = TipeIos.SubJudul, textAlign = TextAlign.Center)
        if (teksAksi != null && onAksi != null) {
            Spacer(Modifier.height(16.dp))
            TombolKapsulIos(teksAksi, onAksi)
        }
    }
}
