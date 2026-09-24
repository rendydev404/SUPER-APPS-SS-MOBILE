package com.sukashawarma.superapp.core.update.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sukashawarma.superapp.core.update.AppUpdateManager.DownloadPayload
import com.sukashawarma.superapp.core.update.AppUpdateManager.DownloadState
import com.sukashawarma.superapp.core.update.model.AppUpdateManifest
import java.util.Locale

// Warna disamakan dengan design system iOS aplikasi (core.ui.ios.WarnaIos);
// modul ini tidak bergantung pada :core:ui, jadi nilainya disalin di sini.
private val Latar = Color.White
private val Label = Color(0xFF1C1C1E)
private val LabelKedua = Color(0xFF3C3C43).copy(alpha = 0.60f)
private val LabelKetiga = Color(0xFF3C3C43).copy(alpha = 0.30f)
private val Isian = Color(0xFF767680).copy(alpha = 0.12f)
private val Aksen = Color(0xFFEA580C)
private val Hijau = Color(0xFF34C759)
private val Merah = Color(0xFFFF3B30)

/**
 * Layar penghalang saat ada versi baru: menutupi seluruh aplikasi sampai update
 * terpasang. Tidak ada tombol "Nanti" — setiap rilis wajib dipasang.
 *
 * Sengaja tenang dan sederhana: ilustrasi (maskot chef beranimasi, dipasok
 * pemanggil), satu bilah kemajuan tipis, teks yang jelas, dan satu tombol.
 *
 * @param ilustrasi digambar di atas judul; modul ini tidak memegang aset maskot.
 * @param pratinjau true di build debug — menampilkan tautan keluar pratinjau.
 */
@Composable
fun LayarWajibUpdate(
    manifest: AppUpdateManifest,
    versiSekarang: String,
    ilustrasi: @Composable () -> Unit,
    state: DownloadState,
    progress: Int,
    payload: DownloadPayload,
    ukuranPayload: Long?,
    onAksi: () -> Unit,
    pratinjau: Boolean = false,
    onTutupPratinjau: () -> Unit = {},
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Latar)
            // Menelan sentuhan supaya tidak ada yang tembus ke aplikasi di belakang.
            .pointerInput(Unit) { detectTapGestures { } }
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(Modifier.height(24.dp))
            Box(Modifier.fillMaxWidth(0.72f), contentAlignment = Alignment.Center) { ilustrasi() }
            Spacer(Modifier.height(20.dp))
            BilahKemajuan(state, progress)
            Spacer(Modifier.height(28.dp))
            Teks(state, manifest.versionName)
            Spacer(Modifier.height(12.dp))
            // Baris polos tanpa animasi: angka unduhan berubah tiap persen, dan
            // menaruhnya di judul yang ber-crossfade membuat teks berkedip.
            val persen = progress.coerceIn(0, 100)
            val infoUkuran = ukuranPayload?.let { total ->
                if (state == DownloadState.DOWNLOADING) {
                    "${formatUkuran(total * persen / 100)} / ${formatUkuran(total)}  ·  $persen%"
                } else {
                    formatUkuran(total)
                }
            }
            Text(
                "$versiSekarang  →  ${manifest.versionName}" + (infoUkuran?.let { "  ·  $it" } ?: ""),
                color = if (state == DownloadState.DOWNLOADING) LabelKedua else LabelKetiga,
                fontSize = 13.sp,
            )
            val catatan = manifest.notes?.lines()
                ?.map { it.trim().trimStart('-', '•', '*').trim() }
                ?.filter { it.isNotEmpty() }
                .orEmpty()
            if (catatan.isNotEmpty()) {
                Spacer(Modifier.height(36.dp))
                Catatan(catatan)
            }
            Spacer(Modifier.height(24.dp))
        }

        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Tombol(state, onAksi)
            Spacer(Modifier.height(12.dp))
            Text(
                if (payload == DownloadPayload.DELTA_PATCH) "Unduhan hemat kuota · data Anda tetap aman"
                else "Data dan login Anda tetap aman",
                color = LabelKetiga,
                fontSize = 12.sp,
            )
            if (pratinjau) {
                Text(
                    "Tutup pratinjau",
                    color = Aksen,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onTutupPratinjau)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    }
}

/**
 * Bilah kemajuan tipis di bawah maskot. Kosong saat belum mulai, terisi saat
 * mengunduh, penuh hijau saat siap, dan berupa segmen yang bergeser saat memasang.
 */
@Composable
private fun BilahKemajuan(state: DownloadState, progress: Int) {
    val isi by animateFloatAsState(
        when (state) {
            DownloadState.DOWNLOADING, DownloadState.FAILED -> progress.coerceIn(0, 100) / 100f
            DownloadState.READY_TO_INSTALL, DownloadState.AWAITING_USER_ACTION -> 1f
            else -> 0f
        },
        tween(600, easing = FastOutSlowInEasing),
        label = "isi",
    )
    val warna = when (state) {
        DownloadState.READY_TO_INSTALL, DownloadState.AWAITING_USER_ACTION -> Hijau
        DownloadState.FAILED -> Merah
        else -> Aksen
    }
    val geser by rememberInfiniteTransition(label = "pasang").animateFloat(
        -0.35f, 1f,
        infiniteRepeatable(tween(1_100, easing = LinearEasing)),
        label = "geser",
    )
    val terlihat by animateFloatAsState(if (state == DownloadState.IDLE) 0f else 1f, tween(300), label = "terlihat")

    Canvas(
        Modifier
            .width(180.dp)
            .height(4.dp)
            .graphicsLayer { alpha = terlihat },
    ) {
        val r = size.height / 2
        drawRoundRect(Isian, cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r))
        if (state == DownloadState.INSTALLING) {
            val lebar = size.width * 0.35f
            val x = size.width * geser
            val kiri = x.coerceAtLeast(0f)
            val kanan = (x + lebar).coerceAtMost(size.width)
            if (kanan > kiri) {
                drawRoundRect(
                    Aksen,
                    topLeft = Offset(kiri, 0f),
                    size = Size(kanan - kiri, size.height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r),
                )
            }
        } else if (isi > 0f) {
            drawRoundRect(
                warna,
                size = Size(size.width * isi, size.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r),
            )
        }
    }
}

private data class Salinan(val judul: String, val isi: String)

private fun salinan(state: DownloadState, versi: String): Salinan = when (state) {
    DownloadState.IDLE -> Salinan("Pembaruan tersedia", "Pasang versi $versi untuk melanjutkan.")
    DownloadState.DOWNLOADING -> Salinan("Mengunduh pembaruan", "Pemasangan dimulai otomatis setelah selesai.")
    DownloadState.READY_TO_INSTALL -> Salinan("Siap dipasang", "Unduhan selesai dan sudah diverifikasi.")
    DownloadState.INSTALLING -> Salinan("Memasang", "Aplikasi akan terbuka lagi dengan versi baru.")
    DownloadState.AWAITING_USER_ACTION -> Salinan("Perlu persetujuan", "Ketuk Lanjutkan, lalu pilih Pasang atau Izinkan.")
    DownloadState.FAILED -> Salinan("Unduhan terhenti", "Periksa koneksi internet, lalu coba lagi.")
}

@Composable
private fun Teks(state: DownloadState, versi: String) {
    AnimatedContent(
        targetState = salinan(state, versi),
        transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) },
        label = "teks",
    ) { s ->
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                s.judul,
                color = Label,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.4).sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(s.isi, color = LabelKedua, fontSize = 15.sp, textAlign = TextAlign.Center, lineHeight = 21.sp)
        }
    }
}

@Composable
private fun Catatan(baris: List<String>) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            "YANG BARU",
            color = LabelKetiga,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.6.sp,
        )
        baris.forEach {
            Spacer(Modifier.height(10.dp))
            Row {
                Text("—", color = LabelKetiga, fontSize = 15.sp)
                Spacer(Modifier.width(10.dp))
                Text(it, color = Label, fontSize = 15.sp, lineHeight = 21.sp)
            }
        }
    }
}

@Composable
private fun Tombol(state: DownloadState, onAksi: () -> Unit) {
    val sibuk = state == DownloadState.DOWNLOADING || state == DownloadState.INSTALLING
    val teks = when (state) {
        DownloadState.IDLE -> "Perbarui"
        DownloadState.DOWNLOADING -> "Mengunduh…"
        DownloadState.READY_TO_INSTALL -> "Pasang"
        DownloadState.INSTALLING -> "Memasang…"
        DownloadState.AWAITING_USER_ACTION -> "Lanjutkan"
        DownloadState.FAILED -> "Coba lagi"
    }
    val interaksi = remember { MutableInteractionSource() }
    val ditekan by interaksi.collectIsPressedAsState()
    val skala by animateFloatAsState(if (ditekan) 0.97f else 1f, spring(dampingRatio = 0.6f), label = "tekan")

    Box(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .graphicsLayer { scaleX = skala; scaleY = skala }
            .clip(RoundedCornerShape(14.dp))
            .background(if (sibuk) Isian else Aksen)
            .clickable(interaksi, indication = null, enabled = !sibuk, onClick = onAksi),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = teks,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) },
            label = "tombol",
        ) { t ->
            Text(
                t,
                color = if (sibuk) LabelKedua else Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private fun formatUkuran(bytes: Long): String {
    val mb = bytes / 1_048_576.0
    return if (mb >= 1) String.format(Locale("id", "ID"), "%.1f MB", mb)
    else String.format(Locale("id", "ID"), "%.0f KB", bytes / 1024.0)
}
