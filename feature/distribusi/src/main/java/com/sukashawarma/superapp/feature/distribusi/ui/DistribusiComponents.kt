package com.sukashawarma.superapp.feature.distribusi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sukashawarma.superapp.core.ui.ios.KartuIos
import com.sukashawarma.superapp.core.ui.ios.KeadaanIos
import com.sukashawarma.superapp.core.ui.ios.LencanaIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.TombolKeduaIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.feature.distribusi.data.model.SuratJalanRingkas
import com.sukashawarma.superapp.feature.distribusi.domain.StatusSuratJalan
import java.time.OffsetDateTime
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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

@Composable
fun LencanaStatus(status: StatusSuratJalan?, adaSelisih: Boolean) {
    val (teks, nada) = when {
        status == null -> "Tidak Dikenal" to NadaIos.NETRAL
        adaSelisih && status.nilai.startsWith("diterima") -> "Ada Selisih" to NadaIos.BAHAYA
        status == StatusSuratJalan.SELESAI -> status.label to NadaIos.SUKSES
        status == StatusSuratJalan.DITERIMA_LENGKAP -> status.label to NadaIos.SUKSES
        status == StatusSuratJalan.DITERIMA_SEBAGIAN -> status.label to NadaIos.BAHAYA
        else -> status.label to NadaIos.INFO
    }
    LencanaIos(teks, nada)
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
    KartuIos(onKlik = onKlik) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(WarnaIos.Aksen.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(IkonIos.Description, null, tint = WarnaIos.Aksen, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "SJ ${baris.nomorDokumen ?: baris.id.take(8).uppercase()}",
                    style = TipeIos.Utama,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${baris.namaOutlet ?: "Gudang Pusat"} · ${formatTanggal(baris.dibuatPada)}",
                    style = TipeIos.Catatan,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            LencanaStatus(baris.status, baris.adaSelisih)
        }
        if (aksiLabel != null && onAksi != null) {
            Spacer(Modifier.height(12.dp))
            TombolKeduaIos(aksiLabel, onAksi, ikon = IkonIos.CheckCircle)
        }
    }
}

@Composable
fun LayarKosong(
    judul: String,
    keterangan: String,
    ikon: ImageVector = IkonIos.Inbox,
    nada: NadaIos = NadaIos.NETRAL,
) {
    Box(Modifier.fillMaxSize().background(WarnaIos.Latar), contentAlignment = Alignment.Center) {
        KeadaanIos(ikon, judul, keterangan, nada = nada)
    }
}

@Composable
fun LayarGalat(pesan: String, onCobaLagi: () -> Unit) {
    Box(Modifier.fillMaxSize().background(WarnaIos.Latar), contentAlignment = Alignment.Center) {
        KeadaanIos(
            IkonIos.CloudOff,
            "Gagal memuat",
            pesan,
            nada = NadaIos.BAHAYA,
            teksAksi = "Coba Lagi",
            onAksi = onCobaLagi,
        )
    }
}

@Composable
fun LayarMemuat() {
    Box(Modifier.fillMaxSize().background(WarnaIos.Latar), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = WarnaIos.Aksen)
    }
}
