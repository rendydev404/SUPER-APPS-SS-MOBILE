package com.sukashawarma.superapp.feature.stok.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.IconButton
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sukashawarma.superapp.feature.stok.data.model.OutletRingkas
import com.sukashawarma.superapp.feature.stok.domain.StokStatus
import com.sukashawarma.superapp.presentation.theme.SukaOnSurface
import com.sukashawarma.superapp.presentation.theme.SukaOnSurfaceVariant
import com.sukashawarma.superapp.presentation.theme.SukaOrange

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

@Composable
fun StatusBadge(status: StokStatus, modifier: Modifier = Modifier) {
    val warna = status.warna()
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = warna.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, warna.copy(alpha = 0.28f)),
    ) {
        Row(
            Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(6.dp).background(warna, CircleShape))
            Spacer(Modifier.width(5.dp))
            Text(
                status.label().uppercase(),
                color = warna,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.3.sp,
            )
        }
    }
}

/**
 * Tiga keadaan dibedakan tegas, karena di lapangan ketiganya sering tertukar dan
 * menghasilkan tindakan yang salah: tidak ada data, gagal mengambil data, dan
 * tidak berhak melihat data adalah masalah yang berbeda.
 */
@Composable
fun KeadaanKosong(pesan: String, modifier: Modifier = Modifier) =
    KeadaanPesan(Icons.Default.Inbox, "Belum ada data", pesan, modifier)

@Composable
fun KeadaanTidakBerhak(pesan: String, modifier: Modifier = Modifier) =
    KeadaanPesan(Icons.Default.Lock, "Tidak ada akses", pesan, modifier)

@Composable
fun KeadaanGagal(pesan: String, onCobaLagi: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().heightIn(min = 240.dp).padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(56.dp).background(Color(0xFFFEF2F2), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.CloudOff, null, tint = Color(0xFFDC2626), modifier = Modifier.size(27.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text("Gagal memuat", color = SukaOnSurface, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(5.dp))
        Text(
            pesan,
            color = SukaOnSurfaceVariant,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onCobaLagi,
            colors = ButtonDefaults.buttonColors(containerColor = SukaOrange),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("Coba lagi", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
private fun KeadaanPesan(icon: ImageVector, judul: String, pesan: String, modifier: Modifier) {
    Column(
        modifier.fillMaxWidth().heightIn(min = 240.dp).padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(56.dp).background(Color(0xFFF1F5F9), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(27.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text(judul, color = SukaOnSurface, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(5.dp))
        Text(
            pesan,
            color = SukaOnSurfaceVariant,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun MemuatPenuh(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().heightIn(min = 240.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = SukaOrange)
    }
}

/**
 * Header oranye yang dipakai seluruh layar modul Stok, supaya tiap tab tidak
 * membangun bilah atasnya sendiri-sendiri dan ikut berubah kalau temanya berubah.
 */
@Composable
fun HeaderStok(
    judul: String,
    subjudul: String? = null,
    onKembali: (() -> Unit)? = null,
    aksi: @Composable RowScope.() -> Unit = {},
) {
    Box(
        Modifier.fillMaxWidth().background(
            Brush.verticalGradient(
                listOf(Color(0xFFEA580C), Color(0xFFF97316))
            )
        )
    ) {
        Row(
            Modifier
                .statusBarsPadding()
                .padding(start = if (onKembali != null) 8.dp else 16.dp, end = 8.dp, top = 6.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onKembali != null) {
                IconButton(onClick = onKembali) {
                    Icon(Icons.Default.ArrowBack, "Kembali", tint = Color.White)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(judul, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                if (subjudul != null) {
                    Text(subjudul, color = Color(0xFFFFEDD5), fontSize = 11.sp)
                }
            }
            aksi()
        }
    }
}

/**
 * Baris pemilih outlet, hanya berguna bila pengguna memegang lebih dari satu outlet.
 * Dipakai bersama oleh Ledger, Opname, Permintaan, dan Mutasi.
 */
@Composable
fun PemilihOutlet(
    outlets: List<OutletRingkas>,
    terpilih: OutletRingkas?,
    onPilih: (OutletRingkas) -> Unit,
) {
    var terbuka by remember { mutableStateOf(false) }
    Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
        Surface(
            onClick = { terbuka = true },
            shape = RoundedCornerShape(14.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE7ECF2)),
        ) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    terpilih?.name ?: "Pilih outlet",
                    color = SukaOnSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
                Icon(
                    Icons.Default.ArrowDropDown, null,
                    tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp),
                )
            }
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
        containerColor = Color.White,
    ) {
        Column(Modifier.fillMaxWidth().heightIn(max = 620.dp)) {
            Row(
                Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(judul, color = SukaOnSurface, fontSize = 16.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(9.dp))
                Text(
                    "${hasil.size}",
                    Modifier
                        .background(Color(0xFFFFF7ED), RoundedCornerShape(7.dp))
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                    color = SukaOrange, fontSize = 11.sp, fontWeight = FontWeight.Black,
                )
            }

            OutlinedTextField(
                value = cari,
                onValueChange = { cari = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                placeholder = { Text("Cari nama outlet…", fontSize = 13.sp, color = Color(0xFF94A3B8)) },
                leadingIcon = {
                    Icon(Icons.Default.Search, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                },
                trailingIcon = {
                    if (cari.isNotEmpty()) {
                        IconButton(onClick = { cari = "" }) {
                            Icon(
                                Icons.Default.Close, "Hapus pencarian",
                                tint = Color(0xFF94A3B8), modifier = Modifier.size(17.dp),
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(13.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = SukaOrange,
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                ),
            )
            Spacer(Modifier.height(10.dp))

            if (hasil.isEmpty()) {
                Box(
                    Modifier.fillMaxWidth().padding(vertical = 44.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Tidak ada outlet bernama \"$cari\".",
                        color = Color(0xFF94A3B8), fontSize = 13.sp,
                    )
                }
            } else {
                LazyColumn(
                    Modifier.weight(1f, fill = false),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    URUTAN_KELOMPOK.forEach { nama ->
                        val isi = kelompok[nama].orEmpty()
                        if (isi.isEmpty()) return@forEach
                        // Judul kelompok disembunyikan saat mencari: hasil sudah sedikit,
                        // dan pemisah malah memecah daftar pendek jadi kepingan.
                        if (cari.isBlank()) {
                            item(key = "judul-$nama") {
                                Text(
                                    nama.uppercase(),
                                    Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 6.dp),
                                    color = Color(0xFF94A3B8), fontSize = 10.sp,
                                    fontWeight = FontWeight.Black, letterSpacing = 0.9.sp,
                                )
                            }
                        }
                        items(isi, key = { it.id }) { o ->
                            BarisOutlet(o, o.id == terpilih?.id) { onPilih(o) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BarisOutlet(outlet: OutletRingkas, aktif: Boolean, onKlik: () -> Unit) {
    Surface(
        onClick = onKlik,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (aktif) Color(0xFFFFF7ED) else Color.White,
        border = BorderStroke(1.dp, if (aktif) Color(0xFFFED7AA) else Color(0xFFF1F5F9)),
    ) {
        Row(Modifier.padding(horizontal = 13.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                outlet.name,
                Modifier.weight(1f),
                color = if (aktif) SukaOrange else SukaOnSurface,
                fontSize = 13.5.sp,
                fontWeight = if (aktif) FontWeight.Black else FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (aktif) {
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.CheckCircle, "Terpilih", tint = SukaOrange, modifier = Modifier.size(19.dp))
            }
        }
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

/**
 * Pita pesan singkat di bawah header — dipakai untuk hasil aksi (berhasil/gagal)
 * agar pengguna tidak menebak apakah tombolnya bekerja.
 */
@Composable
fun PitaPesan(pesan: String, gagal: Boolean, onTutup: () -> Unit) {
    val warna = if (gagal) Color(0xFFDC2626) else Color(0xFF168451)
    Surface(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = warna.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, warna.copy(alpha = 0.25f)),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(pesan, Modifier.weight(1f), color = warna, fontSize = 12.sp, lineHeight = 17.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                "Tutup",
                Modifier.clickable(onClick = onTutup),
                color = warna,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
