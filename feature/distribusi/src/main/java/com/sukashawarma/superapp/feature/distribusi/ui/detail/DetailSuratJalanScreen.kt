package com.sukashawarma.superapp.feature.distribusi.ui.detail

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.feature.distribusi.data.model.SuratJalanDetail
import com.sukashawarma.superapp.feature.distribusi.data.model.TandaTangan
import com.sukashawarma.superapp.feature.distribusi.ui.LayarGalat
import com.sukashawarma.superapp.feature.distribusi.ui.LayarMemuat
import com.sukashawarma.superapp.feature.distribusi.ui.LencanaStatus
import com.sukashawarma.superapp.feature.distribusi.ui.formatTanggal
import com.sukashawarma.superapp.presentation.theme.SukaBrown
import com.sukashawarma.superapp.presentation.theme.SukaGray100
import com.sukashawarma.superapp.presentation.theme.SukaGray200
import com.sukashawarma.superapp.presentation.theme.SukaGray500
import com.sukashawarma.superapp.presentation.theme.SukaGreen
import com.sukashawarma.superapp.presentation.theme.SukaOnSurface
import com.sukashawarma.superapp.presentation.theme.SukaOrange
import com.sukashawarma.superapp.presentation.theme.SukaPrimaryContainer
import com.sukashawarma.superapp.presentation.theme.SukaSurface
import com.sukashawarma.superapp.presentation.theme.SukaSurfaceContainer
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.RealtimeTables

private val MerahTeks = Color(0xFFB91C1C)
private val MerahLatar = Color(0xFFFDECEC)
private val HijauLatar = Color(0xFFE7F6EC)
private val AbuTeks = SukaGray500

@Composable
fun DetailSuratJalanScreen(suratJalanId: String, onKeluar: () -> Unit) {
    val viewModel: DetailViewModel = viewModel(factory = DetailViewModel.Factory(suratJalanId))
    val state by viewModel.state.collectAsState()
    RealtimeRefresh(RealtimeTables.SURAT_JALAN) { viewModel.muat() }

    if (state.memuat && state.detail == null) { LayarMemuat(); return }
    val detail = state.detail
    if (detail == null) {
        LayarGalat(state.error ?: "Dokumen tidak bisa dibuka.") { viewModel.muat() }
        return
    }

    LaunchedEffect(state.baris) {
        state.baris.mapNotNull { it.fotoPath }.forEach { viewModel.muatFoto(it) }
    }

    val jumlahBarang = state.baris.size
    val jumlahDiperiksa = state.baris.count { it.qtyTerima != null }
    val jumlahBermasalah = state.baris.count { it.bermasalah }
    val jumlahBukti = state.baris.count { it.fotoPath != null }

    Column(Modifier.fillMaxSize().background(SukaSurface)) {
        KepalaDetail(detail, state.baris.any { it.bermasalah }, onKeluar)

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "ringkasan") {
                RingkasanDetail(
                    jumlahBarang = jumlahBarang,
                    jumlahDiperiksa = jumlahDiperiksa,
                    jumlahBermasalah = jumlahBermasalah,
                    jumlahBukti = jumlahBukti,
                )
            }

            // Kode verifikasi hanya untuk pengawas — lihat DistribusiAkses.
            if (state.bolehLihatKode && detail.kodeVerifikasi != null) {
                item(key = "kode-verifikasi") {
                    KartuKodeVerifikasi(detail.kodeVerifikasi)
                }
            }

            item(key = "judul-rincian") {
                KepalaBagian(
                    judul = "Rincian barang",
                    keterangan = "Bandingkan jumlah kirim dengan jumlah yang diterima",
                    jumlah = "$jumlahBarang item",
                )
            }

            itemsIndexed(state.baris) { indeks, baris ->
                KartuItemDetail(
                    nomor = indeks + 1,
                    baris = baris,
                    bitmap = baris.fotoPath?.let { state.foto[it] },
                )
            }

            item(key = "judul-persetujuan") {
                KepalaBagian("Persetujuan", "Jejak tanda tangan dokumen", "2 tahap")
            }
            item(key = "ttd-pengirim") { BlokTandaTangan("Tanda Tangan Pengirim", detail.ttdPengirim) }
            item(key = "ttd-penerimaan") { BlokTandaTangan("Tanda Tangan Penerimaan", detail.ttdPenerimaan) }
        }
    }
}

@Composable
private fun KepalaDetail(detail: SuratJalanDetail, adaSelisih: Boolean, onKeluar: () -> Unit) {
    Surface(color = SukaSurface) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onKeluar) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = SukaOnSurface) }
            Column(Modifier.weight(1f)) {
                Text("SURAT JALAN", color = SukaGray500, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Text(
                    "SJ ${detail.nomorDokumen ?: detail.id.take(8).uppercase()}",
                    color = SukaOnSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = AbuTeks, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${detail.namaOutlet ?: "Gudang Pusat"} • ${formatTanggal(detail.dibuatPada)}",
                        color = SukaGray500,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            LencanaStatus(detail.status, adaSelisih)
        }
    }
}

@Composable
private fun RingkasanDetail(
    jumlahBarang: Int,
    jumlahDiperiksa: Int,
    jumlahBermasalah: Int,
    jumlahBukti: Int,
) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = SukaBrown,
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Ringkasan kiriman", color = Color(0xFFFFE8D1), fontSize = 11.sp, fontWeight = FontWeight.Black)
                    Text(
                        if (jumlahBermasalah == 0) "Kiriman terlihat sesuai" else "$jumlahBermasalah item perlu perhatian",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
                Surface(shape = RoundedCornerShape(50), color = Color.White.copy(alpha = 0.14f)) {
                    Text(
                        "$jumlahBarang barang",
                        Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KotakRingkasan("Diperiksa", "$jumlahDiperiksa/$jumlahBarang", Icons.Default.CheckCircle, SukaGreen)
                KotakRingkasan(
                    "Perhatian",
                    jumlahBermasalah.toString(),
                    if (jumlahBermasalah > 0) Icons.Default.WarningAmber else Icons.Default.CheckCircle,
                    if (jumlahBermasalah > 0) Color(0xFFFFB4A8) else SukaGreen,
                )
                KotakRingkasan("Bukti foto", jumlahBukti.toString(), Icons.Default.PhotoCamera, SukaOrange)
            }
        }
    }
}

@Composable
private fun RowScope.KotakRingkasan(judul: String, nilai: String, ikon: ImageVector, warna: Color) {
    Surface(
        Modifier.weight(1f),
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
    ) {
        Column(Modifier.padding(10.dp)) {
            Icon(ikon, contentDescription = null, tint = warna, modifier = Modifier.size(17.dp))
            Spacer(Modifier.height(6.dp))
            Text(judul, color = Color(0xFFFFE8D1), fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(nilai, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun KartuKodeVerifikasi(kode: String) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, SukaOrange.copy(alpha = 0.20f)),
    ) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).background(SukaPrimaryContainer.copy(alpha = 0.14f), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = SukaOrange, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text("Kode verifikasi", color = SukaGray500, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(kode, color = SukaOnSurface, fontSize = 20.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
            }
            Text("PENGAWAS", color = SukaBrown, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 0.6.sp)
        }
    }
}

@Composable
private fun KepalaBagian(judul: String, keterangan: String, jumlah: String) {
    Row(Modifier.fillMaxWidth().padding(top = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(judul, color = SukaOnSurface, fontSize = 16.sp, fontWeight = FontWeight.Black)
            Text(keterangan, color = SukaGray500, fontSize = 11.sp)
        }
        Surface(shape = RoundedCornerShape(50), color = SukaSurfaceContainer) {
            Text(jumlah, Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = SukaBrown, fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun KartuItemDetail(nomor: Int, baris: BarisItemDetail, bitmap: Bitmap?) {
    val warnaGaris = if (baris.bermasalah) MerahTeks.copy(alpha = 0.35f) else SukaGray200
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, warnaGaris),
        shadowElevation = 1.dp,
    ) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    Modifier.size(34.dp).background(SukaPrimaryContainer.copy(alpha = 0.14f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(nomor.toString().padStart(2, '0'), color = SukaBrown, fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        baris.nama,
                        color = SukaOnSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text("Satuan ${baris.satuan}", color = SukaGray500, fontSize = 10.5.sp)
                }
                LabelKondisi(baris)
            }

            Spacer(Modifier.height(13.dp))
            Text("Perbandingan jumlah", color = SukaGray500, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KotakJumlah("Dikirim", "${baris.qtyDikirim} ${baris.satuan}", SukaOnSurface)
                KotakJumlah(
                    "Diterima",
                    "${baris.qtyTerima?.toString() ?: "-"} ${baris.satuan}",
                    if (baris.bermasalah) MerahTeks else SukaGreen,
                )
            }

            if (baris.bermasalah) {
                Spacer(Modifier.height(10.dp))
                Surface(shape = RoundedCornerShape(12.dp), color = MerahLatar) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.WarningAmber, contentDescription = null, tint = MerahTeks, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(pesanMasalah(baris), color = MerahTeks, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (!baris.catatan.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Surface(shape = RoundedCornerShape(12.dp), color = SukaGray100) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = SukaGray500, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(baris.catatan, color = SukaGray500, fontSize = 11.sp, lineHeight = 16.sp)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("Bukti foto", color = SukaGray500, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            when {
                bitmap != null -> Image(
                    bitmap.asImageBitmap(),
                    contentDescription = "Foto bukti ${baris.nama}",
                    modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop,
                )
                baris.fotoPath != null -> Box(
                    Modifier.fillMaxWidth().height(150.dp).background(SukaSurfaceContainer, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = SukaGray500, modifier = Modifier.size(25.dp))
                        Spacer(Modifier.height(6.dp))
                        Text("Memuat bukti foto…", color = SukaGray500, fontSize = 11.sp)
                    }
                }
                else -> Surface(shape = RoundedCornerShape(12.dp), color = SukaGray100) {
                    Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = SukaGray500, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Belum ada foto bukti", color = SukaGray500, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.KotakJumlah(label: String, nilai: String, warna: Color) {
    Surface(
        Modifier.weight(1f),
        shape = RoundedCornerShape(14.dp),
        color = SukaSurface,
        border = BorderStroke(1.dp, SukaGray200),
    ) {
        Column(Modifier.padding(horizontal = 11.dp, vertical = 10.dp)) {
            Text(label, color = SukaGray500, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(3.dp))
            Text(nilai, color = warna, fontSize = 16.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun LabelKondisi(baris: BarisItemDetail) {
    val rusak = baris.kondisi.equals("rusak", ignoreCase = true)
    val baik = baris.kondisi.equals("baik", ignoreCase = true)
    val warna = when {
        rusak -> MerahTeks
        baik -> SukaGreen
        else -> SukaGray500
    }
    val latar = when {
        rusak -> MerahLatar
        baik -> HijauLatar
        else -> SukaGray100
    }
    Surface(shape = RoundedCornerShape(50), color = latar) {
        Text(
            when {
                rusak -> "Rusak"
                baik -> "Baik"
                else -> "Belum dicatat"
            },
            Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            color = warna,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
        )
    }
}

private fun pesanMasalah(baris: BarisItemDetail): String {
    val kurang = baris.qtyTerima != null && baris.qtyTerima < baris.qtyDikirim
    return when {
        baris.kondisi.equals("rusak", ignoreCase = true) && kurang -> "Jumlah kurang dan kondisi rusak"
        baris.kondisi.equals("rusak", ignoreCase = true) -> "Kondisi barang ditandai rusak"
        kurang -> "Jumlah diterima lebih sedikit"
        else -> "Perlu perhatian pada barang ini"
    }
}

@Composable
private fun BlokTandaTangan(judul: String, daftar: List<TandaTangan>) {
    val sudahAda = daftar.isNotEmpty()
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, SukaGray200),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(judul, color = SukaOnSurface, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        if (sudahAda) "Tanda tangan tercatat" else "Menunggu tanda tangan",
                        color = SukaGray500,
                        fontSize = 10.5.sp,
                    )
                }
                Surface(shape = RoundedCornerShape(50), color = if (sudahAda) HijauLatar else SukaGray100) {
                    Text(
                        if (sudahAda) "Lengkap" else "Belum ada",
                        Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        color = if (sudahAda) SukaGreen else SukaGray500,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
            if (sudahAda) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = SukaGray200)
                Spacer(Modifier.height(9.dp))
                daftar.forEachIndexed { indeks, ttd ->
                    if (indeks > 0) Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SukaGreen, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(7.dp))
                        Column {
                            Text("${ttd.peran}: ${ttd.namaPenandaTangan}", color = SukaOnSurface, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                            Text(formatTanggal(ttd.waktu), color = SukaGray500, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}
