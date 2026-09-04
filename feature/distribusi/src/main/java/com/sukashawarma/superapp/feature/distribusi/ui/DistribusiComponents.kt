package com.sukashawarma.superapp.feature.distribusi.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sukashawarma.superapp.feature.distribusi.data.model.SuratJalanRingkas
import com.sukashawarma.superapp.feature.distribusi.domain.StatusSuratJalan
import com.sukashawarma.superapp.presentation.theme.SukaGray100
import com.sukashawarma.superapp.presentation.theme.SukaGray500
import com.sukashawarma.superapp.presentation.theme.SukaOnSurface
import com.sukashawarma.superapp.presentation.theme.SukaOrange
import com.sukashawarma.superapp.presentation.theme.SukaSurface
import java.time.OffsetDateTime
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val BULAN = arrayOf(
    "Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des",
)

/**
 * Timestamp PostgREST -> "4 Sep 2026".
 *
 * Menerima tiga bentuk yang benar-benar muncul di data: dengan offset, dengan
 * "Z", dan tanpa zona sama sekali pada baris lama. Bentuk yang tak dikenali
 * menghasilkan tanda hubung — satu baris berformat aneh tidak boleh membuat
 * seluruh daftar gagal dirender.
 */
fun formatTanggal(iso: String?): String {
    if (iso.isNullOrBlank()) return "-"
    val tanggal = try {
        OffsetDateTime.parse(iso).toLocalDate()
    } catch (e: Exception) {
        try {
            LocalDateTime.parse(iso, DateTimeFormatter.ISO_LOCAL_DATE_TIME).toLocalDate()
        } catch (e2: Exception) {
            return "-"
        }
    }
    return "${tanggal.dayOfMonth} ${BULAN[tanggal.monthValue - 1]} ${tanggal.year}"
}

private val HijauTeks = Color(0xFF0A7D2C)
private val HijauLatar = Color(0xFFE7F6EC)
private val BiruTeks = Color(0xFF1D4ED8)
private val BiruLatar = Color(0xFFE6EDFD)
private val MerahTeks = Color(0xFFB91C1C)
private val MerahLatar = Color(0xFFFDECEC)
private val AbuTeks = Color(0xFF6B7280)

@Composable
fun LencanaStatus(status: StatusSuratJalan?, adaSelisih: Boolean) {
    val (teks, warnaTeks, warnaLatar) = when {
        status == null -> Triple("Tidak Dikenal", AbuTeks, SukaGray100)
        adaSelisih && status.nilai.startsWith("diterima") ->
            Triple("Ada Selisih", MerahTeks, MerahLatar)
        status == StatusSuratJalan.SELESAI -> Triple(status.label, HijauTeks, HijauLatar)
        status == StatusSuratJalan.DITERIMA_LENGKAP -> Triple(status.label, HijauTeks, HijauLatar)
        status == StatusSuratJalan.DITERIMA_SEBAGIAN -> Triple(status.label, MerahTeks, MerahLatar)
        else -> Triple(status.label, BiruTeks, BiruLatar)
    }
    Surface(shape = RoundedCornerShape(50), color = warnaLatar) {
        Text(
            teks,
            Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
            color = warnaTeks,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

/**
 * Kartu satu surat jalan. `aksiLabel` dan `onAksi` mengisi tombol sekunder di
 * kaki kartu — dipakai dashboard untuk "Tutup Dokumen"; layar lain melewatkannya.
 */
@Composable
fun KartuSuratJalan(
    baris: SuratJalanRingkas,
    aksiLabel: String? = null,
    onKlik: () -> Unit,
    onAksi: (() -> Unit)? = null,
) {
    Surface(
        Modifier.fillMaxWidth().clickable(onClick = onKlik),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, SukaOrange.copy(alpha = 0.18f)),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "SJ ${baris.nomorDokumen ?: baris.id.take(8).uppercase()}",
                    Modifier.weight(1f),
                    color = SukaOnSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                LencanaStatus(baris.status, baris.adaSelisih)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                baris.namaOutlet ?: "Gudang Pusat",
                color = SukaGray500,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(formatTanggal(baris.dibuatPada), color = SukaGray500, fontSize = 11.sp)
            if (aksiLabel != null && onAksi != null) {
                Spacer(Modifier.height(10.dp))
                Button(onClick = onAksi, modifier = Modifier.fillMaxWidth()) {
                    Text(aksiLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Icon(Icons.Default.ChevronRight, null)
                }
            }
        }
    }
}

@Composable
fun LayarKosong(judul: String, keterangan: String) {
    Column(
        Modifier.fillMaxSize().background(SukaSurface).padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Default.Inbox, null, tint = SukaGray500, modifier = Modifier.height(44.dp))
        Spacer(Modifier.height(12.dp))
        Text(judul, color = SukaOnSurface, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(6.dp))
        Text(keterangan, color = SukaGray500, fontSize = 12.sp)
    }
}

@Composable
fun LayarGalat(pesan: String, onCobaLagi: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(SukaSurface).padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Default.WarningAmber, null, tint = MerahTeks, modifier = Modifier.height(44.dp))
        Spacer(Modifier.height(12.dp))
        Text(pesan, color = SukaOnSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(14.dp))
        Button(onClick = onCobaLagi) { Text("Coba Lagi") }
    }
}

@Composable
fun LayarMemuat() {
    Box(Modifier.fillMaxSize().background(SukaSurface), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = SukaOrange)
    }
}
