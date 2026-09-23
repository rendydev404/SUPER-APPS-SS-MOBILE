package com.sukashawarma.superapp.feature.stok.ui

import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.core.ui.ios.BilahJudulIos
import com.sukashawarma.superapp.core.ui.ios.KeadaanIos
import com.sukashawarma.superapp.core.ui.ios.KolomCariIos
import com.sukashawarma.superapp.core.ui.ios.LabelSeksiIos
import com.sukashawarma.superapp.core.ui.ios.LencanaIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.PemisahIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.ios.tekanIos
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.sukashawarma.superapp.feature.stok.data.model.OutletRingkas
import com.sukashawarma.superapp.feature.stok.domain.StokStatus

/** Warna badge status. Abu-abu dipakai saat skala satuan tak dapat dipercaya. */
fun StokStatus.warna(): Color = when (this) {
    StokStatus.OK -> Color(0xFF168451)
    StokStatus.WARNING -> Color(0xFFC27A12)
    StokStatus.BELOW -> Color(0xFFDC2626)
    StokStatus.UNKNOWN -> Color(0xFF64748B)
}

fun StokStatus.label(): String = when (this) {
    StokStatus.OK -> "Aman"
    StokStatus.WARNING -> "Menipis"
    StokStatus.BELOW -> "Kritis"
    StokStatus.UNKNOWN -> "Skala?"
}

/**
 * Nada iOS untuk status stok. Dipisah dari [warna] karena kode lain memakai warna
 * lama itu untuk angka dan grafik — lencana cukup memakai nada sistem iOS.
 */
internal fun StokStatus.nadaIos(): NadaIos = when (this) {
    StokStatus.OK -> NadaIos.SUKSES
    StokStatus.WARNING -> NadaIos.PERINGATAN
    StokStatus.BELOW -> NadaIos.BAHAYA
    StokStatus.UNKNOWN -> NadaIos.NETRAL
}

@Composable
fun StatusBadge(status: StokStatus, modifier: Modifier = Modifier) {
    LencanaIos(status.label(), status.nadaIos(), modifier)
}

/**
 * Tiga keadaan dibedakan tegas, karena di lapangan ketiganya sering tertukar dan
 * menghasilkan tindakan yang salah: tidak ada data, gagal mengambil data, dan
 * tidak berhak melihat data adalah masalah yang berbeda.
 */
@Composable
fun KeadaanKosong(pesan: String, modifier: Modifier = Modifier) =
    KeadaanIos(IkonIos.Inbox, "Belum ada data", pesan, modifier)

@Composable
fun KeadaanTidakBerhak(pesan: String, modifier: Modifier = Modifier) =
    KeadaanIos(IkonIos.Lock, "Tidak ada akses", pesan, modifier, nada = NadaIos.PERINGATAN)

@Composable
fun KeadaanGagal(pesan: String, onCobaLagi: () -> Unit, modifier: Modifier = Modifier) =
    KeadaanIos(
        IkonIos.CloudOff, "Gagal memuat", pesan, modifier,
        nada = NadaIos.BAHAYA, teksAksi = "Coba lagi", onAksi = onCobaLagi,
    )

@Composable
fun MemuatPenuh(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().heightIn(min = 240.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.size(30.dp), color = WarnaIos.Abu, strokeWidth = 2.5.dp)
    }
}

/**
 * Header seluruh layar modul Stok — bilah judul iOS dengan tombol kembali bulat.
 *
 * Aksi di kanan diberi warna konten aksen, supaya `IconButton` lama milik layar
 * yang belum dipindah ke tombol bulat tetap tampil serasi tanpa diubah satu per satu.
 */
@Composable
fun HeaderStok(
    judul: String,
    subjudul: String? = null,
    onKembali: (() -> Unit)? = null,
    aksi: @Composable RowScope.() -> Unit = {},
) {
    BilahJudulIos(
        judul = judul,
        subjudul = subjudul,
        onKembali = onKembali,
        aksi = {
            CompositionLocalProvider(LocalContentColor provides WarnaIos.Aksen) {
                aksi()
            }
        },
    )
}

/**
 * Pemilih outlet bergaya "pull-down button" iOS: kapsul abu dengan chevron.
 * Dipakai bersama oleh Ledger, Opname, Permintaan, dan Mutasi.
 */
@Composable
fun PemilihOutlet(
    outlets: List<OutletRingkas>,
    terpilih: OutletRingkas?,
    onPilih: (OutletRingkas) -> Unit,
) {
    var terbuka by remember { mutableStateOf(false) }
    Box(Modifier.padding(start = UkuranIos.TepiLayar, end = UkuranIos.TepiLayar, top = 10.dp, bottom = 4.dp)) {
        Row(
            Modifier
                .heightIn(min = 36.dp)
                .clip(UkuranIos.SudutKapsul)
                .background(WarnaIos.Isian)
                .tekanIos({ terbuka = true })
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(IkonIos.Storefront, null, tint = WarnaIos.Aksen, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(7.dp))
            Text(
                terpilih?.name ?: "Pilih outlet",
                style = TipeIos.Keterangan.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                // Batas lebar, bukan weight: kapsul harus tetap seukuran isinya.
                modifier = Modifier.widthIn(max = 240.dp),
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                IkonIos.ArrowDropDown,
                contentDescription = "Ganti outlet",
                tint = WarnaIos.Aksen,
                modifier = Modifier.size(16.dp),
            )
        }
    }
    if (terbuka) {
        LembarPilihOutlet(
            outlets = outlets,
            terpilih = terpilih,
            onPilih = { terbuka = false; onPilih(it) },
            onTutup = { terbuka = false },
        )
    }
}

/**
 * Pemilih outlet berbentuk lembar bawah dengan pencarian.
 *
 * Menggantikan `DropdownMenu` polos yang, dengan dua puluhan outlet, menutupi
 * hampir seluruh layar dan hanya bisa digulir — tidak ada cara mempersempit
 * selain membaca satu per satu sampai ketemu.
 *
 * Dipakai bersama oleh Ledger, Opname, Permintaan, dan Mutasi lewat
 * [PemilihOutlet], dan langsung oleh form pengajuan mutasi untuk memilih tujuan.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LembarPilihOutlet(
    outlets: List<OutletRingkas>,
    terpilih: OutletRingkas?,
    onPilih: (OutletRingkas) -> Unit,
    onTutup: () -> Unit,
    judul: String = "Pilih Outlet",
) {
    var cari by remember { mutableStateOf("") }
    val lembar = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val hasil = remember(outlets, cari) {
        val kata = cari.trim().lowercase()
        outlets
            .filter { kata.isEmpty() || it.name.lowercase().contains(kata) }
            .sortedBy { it.name.uppercase() }
    }
    val kelompok = remember(hasil) { hasil.groupBy { kelompokOutlet(it.name) } }

    ModalBottomSheet(
        onDismissRequest = onTutup,
        sheetState = lembar,
        containerColor = WarnaIos.Latar,
    ) {
        Column(Modifier.fillMaxWidth().heightIn(max = 620.dp)) {
            Row(
                Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(judul, Modifier.weight(1f), style = TipeIos.Judul3.copy(fontWeight = FontWeight.Bold))
                Text("${hasil.size} outlet", style = TipeIos.SubJudul)
            }

            KolomCariIos(
                nilai = cari,
                onUbah = { cari = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = UkuranIos.TepiLayar),
                placeholder = "Cari nama outlet…",
            )
            Spacer(Modifier.height(10.dp))

            if (hasil.isEmpty()) {
                Box(
                    Modifier.fillMaxWidth().padding(vertical = 44.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Tidak ada outlet bernama \"$cari\".", style = TipeIos.SubJudul)
                }
            } else {
                LazyColumn(
                    Modifier.weight(1f, fill = false),
                    contentPadding = PaddingValues(start = UkuranIos.TepiLayar, end = UkuranIos.TepiLayar, bottom = 24.dp),
                ) {
                    URUTAN_KELOMPOK.forEach { nama ->
                        val isi = kelompok[nama].orEmpty()
                        if (isi.isEmpty()) return@forEach
                        // Judul kelompok disembunyikan saat mencari: hasil sudah sedikit,
                        // dan pemisah malah memecah daftar pendek jadi kepingan.
                        if (cari.isBlank()) {
                            item(key = "judul-$nama") {
                                LabelSeksiIos(nama, Modifier.padding(start = 16.dp, top = 14.dp, bottom = 7.dp))
                            }
                        }
                        itemsIndexed(isi, key = { _, o -> o.id }) { i, o ->
                            BarisOutlet(
                                outlet = o,
                                aktif = o.id == terpilih?.id,
                                pertama = i == 0,
                                terakhir = i == isi.lastIndex,
                            ) { onPilih(o) }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Satu baris dalam grup "inset grouped". Grup disusun per baris (bukan satu
 * kartu berisi semua baris) supaya daftar tetap lazy — sudut hanya dibulatkan
 * di baris pertama dan terakhir kelompok.
 */
@Composable
private fun BarisOutlet(
    outlet: OutletRingkas,
    aktif: Boolean,
    pertama: Boolean,
    terakhir: Boolean,
    onKlik: () -> Unit,
) {
    val sudut = RoundedCornerShape(
        topStart = if (pertama) 16.dp else 0.dp,
        topEnd = if (pertama) 16.dp else 0.dp,
        bottomStart = if (terakhir) 16.dp else 0.dp,
        bottomEnd = if (terakhir) 16.dp else 0.dp,
    )
    Column(Modifier.fillMaxWidth().clip(sudut).background(WarnaIos.Kartu)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onKlik)
                .heightIn(min = 48.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                outlet.name,
                Modifier.weight(1f),
                style = if (aktif) TipeIos.Isi.copy(color = WarnaIos.Aksen, fontWeight = FontWeight.SemiBold) else TipeIos.Isi,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (aktif) {
                Spacer(Modifier.width(8.dp))
                Icon(IkonIos.Check, "Terpilih", tint = WarnaIos.Aksen, modifier = Modifier.size(18.dp))
            }
        }
        if (!terakhir) PemisahIos()
    }
}

private val URUTAN_KELOMPOK = listOf("Pusat & Gudang", "Outlet Suka Shawarma", "Mitra", "Lainnya")

/**
 * Kelompok tampilan berdasarkan awalan nama outlet.
 *
 * Murni untuk memudahkan memindai daftar panjang, bukan data — penamaan di
 * database memang berpola ("GUDANG …", "MITRA …", "SUKA SHAWARMA …"). Nama yang
 * tidak cocok jatuh ke "Lainnya", jadi outlet baru tetap muncul apa pun namanya.
 */
private fun kelompokOutlet(nama: String): String {
    val n = nama.uppercase()
    return when {
        n.startsWith("GUDANG") || n.startsWith("KANTOR") -> "Pusat & Gudang"
        n.startsWith("MITRA") -> "Mitra"
        n.startsWith("SUKA SHAWARMA") || n.startsWith("SS ") -> "Outlet Suka Shawarma"
        else -> "Lainnya"
    }
}

/**
 * Waktu ringkas dari stempel ISO milik PostgREST, mis. "03 Sep 14:42".
 * Nilai yang tidak terbaca dikembalikan apa adanya, bukan diganti tanggal palsu.
 */
fun waktuSingkat(iso: String?): String {
    if (iso.isNullOrBlank()) return "-"
    return try {
        val waktu = java.time.OffsetDateTime.parse(iso)
            .atZoneSameInstant(java.time.ZoneId.of("Asia/Jakarta"))
        waktu.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM HH:mm", java.util.Locale("id", "ID")))
    } catch (_: Exception) {
        iso.take(16).replace('T', ' ')
    }
}

/** Tanggal saja, mis. "03 Sep 2026". */
fun tanggalSingkat(nilai: String?): String {
    if (nilai.isNullOrBlank()) return "-"
    return try {
        java.time.LocalDate.parse(nilai.take(10))
            .format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy", java.util.Locale("id", "ID")))
    } catch (_: Exception) {
        nilai.take(10)
    }
}

// ─────────────────────────────────────────── pelengkap design system iOS modul Stok

/**
 * Kapsul filter bergaya segmen iOS: terisi aksen saat terpilih. [jumlah] tampil
 * redup di sebelah label; [titik] memberi penanda warna kategori saat tidak terpilih.
 */
@Composable
internal fun KapsulFilter(
    teks: String,
    aktif: Boolean,
    onKlik: () -> Unit,
    modifier: Modifier = Modifier,
    jumlah: Int? = null,
    titik: Color? = null,
) {
    Row(
        modifier
            .height(36.dp)
            .clip(UkuranIos.SudutKapsul)
            .background(if (aktif) WarnaIos.Aksen else WarnaIos.Isian)
            .tekanIos(onKlik)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (titik != null && !aktif) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(titik))
            Spacer(Modifier.width(6.dp))
        }
        Text(
            teks,
            color = if (aktif) Color.White else WarnaIos.Label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        if (jumlah != null) {
            Spacer(Modifier.width(6.dp))
            Text(
                "$jumlah",
                color = if (aktif) Color.White.copy(alpha = 0.85f) else WarnaIos.LabelKedua,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}

/**
 * Pemicu menu ala "pull-down button" iOS — sama dengan [TombolKapsulIos], ditambah
 * status [aktif] supaya filter yang sedang berlaku terlihat beda dari bawaan.
 */
@Composable
internal fun KapsulMenu(
    teks: String,
    onKlik: () -> Unit,
    modifier: Modifier = Modifier,
    aktif: Boolean = false,
    ikon: ImageVector? = null,
    chevron: Boolean = true,
) {
    Row(
        modifier
            .height(UkuranIos.TinggiKontrol)
            .clip(UkuranIos.SudutKontrol)
            .background(if (aktif) WarnaIos.Aksen.copy(alpha = 0.14f) else WarnaIos.Isian)
            .tekanIos(onKlik)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (ikon != null) {
            Icon(ikon, null, tint = WarnaIos.Aksen, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(
            teks,
            color = if (aktif) WarnaIos.Aksen else WarnaIos.Label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (chevron) {
            Spacer(Modifier.width(4.dp))
            Icon(IkonIos.ArrowDropDown, null, tint = WarnaIos.Aksen, modifier = Modifier.size(14.dp))
        }
    }
}

/**
 * Catatan berwarna di dalam kartu (peringatan, info, hasil) — isian nada tipis
 * tanpa garis tepi, pengganti kotak berbingkai warna-warni.
 */
@Composable
internal fun BannerIos(
    teks: String,
    nada: NadaIos,
    modifier: Modifier = Modifier,
    ikon: ImageVector? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(UkuranIos.SudutKontrol)
            .background(nada.warna.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        if (ikon != null) {
            Icon(ikon, null, tint = nada.warna, modifier = Modifier.padding(top = 1.dp).size(16.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(teks, style = TipeIos.Catatan.copy(color = nada.teks, fontWeight = FontWeight.Medium, lineHeight = 18.sp))
    }
}

/** Blok abu di dalam kartu untuk rincian label–nilai, seperti [BlokAngkaIos]. */
@Composable
internal fun BlokAbuIos(
    modifier: Modifier = Modifier,
    isi: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(UkuranIos.SudutBlok)
            .background(WarnaIos.Latar)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = isi,
    )
}

/** Satu baris label kiri – nilai kanan untuk [BlokAbuIos]. */
@Composable
internal fun BarisRincianIos(label: String, nilai: String, warnaNilai: Color = WarnaIos.Label, tebal: Boolean = false) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(label, style = TipeIos.Catatan)
        Spacer(Modifier.width(12.dp))
        Text(
            nilai,
            Modifier.weight(1f),
            color = warnaNilai,
            fontSize = 13.sp,
            fontWeight = if (tebal) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Pita pesan singkat di bawah header — dipakai untuk hasil aksi (berhasil/gagal)
 * agar pengguna tidak menebak apakah tombolnya bekerja.
 */
@Composable
fun PitaPesan(pesan: String, gagal: Boolean, onTutup: () -> Unit) {
    val nada = if (gagal) NadaIos.BAHAYA else NadaIos.SUKSES
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = UkuranIos.TepiLayar, vertical = 8.dp)
            .clip(UkuranIos.SudutKontrol)
            .background(nada.warna.copy(alpha = 0.12f))
            .padding(start = 12.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(22.dp).clip(CircleShape).background(nada.warna),
            contentAlignment = Alignment.Center,
        ) {
            Icon(if (gagal) IkonIos.Close else IkonIos.Check, null, tint = Color.White, modifier = Modifier.size(13.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(pesan, Modifier.weight(1f), style = TipeIos.Catatan.copy(color = nada.teks, lineHeight = 18.sp))
        Spacer(Modifier.width(4.dp))
        Text(
            "Tutup",
            Modifier
                .clip(UkuranIos.SudutKapsul)
                .tekanIos(onTutup)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            color = nada.teks,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
