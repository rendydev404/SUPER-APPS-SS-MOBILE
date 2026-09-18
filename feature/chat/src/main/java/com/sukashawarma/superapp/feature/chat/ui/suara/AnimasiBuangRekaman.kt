package com.sukashawarma.superapp.feature.chat.ui.suara

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Merah = Color(0xFFFF3B30)

private val UKURAN_TONG = 32.dp
private val UKURAN_MIC = 22.dp

/** Lama lemparan mikrofon, dari lepas landas sampai tenggelam di dalam tong. */
private const val DURASI_LEMPAR_MS = 620

/**
 * Animasi membuang rekaman ala WhatsApp: tutup tong membuka, mikrofon
 * DILEMPARKAN melengkung ke dalam tong, tutupnya menutup dengan pantulan, tong
 * bergoyang meredam, lalu semuanya memudar.
 *
 * GERAKANNYA SATU TARIKAN, BUKAN TIGA POTONG.
 * Versi sebelumnya membagi lemparan menjadi fase "melayang" lalu fase "jatuh";
 * di antara keduanya mikrofon berhenti sesaat, dan itulah yang membuatnya
 * terasa kaku. Sekarang posisinya diturunkan dari satu nilai waktu tunggal
 * lewat lintasan parabola — mendatar melambat, menegak dipercepat gravitasi —
 * sehingga tidak ada satu pun titik di mana geraknya berhenti atau berganti
 * aturan.
 *
 * Tutup dan goyangan tong memakai spring, bukan tween: keduanya benda yang
 * dilepas dan memantul, dan tween yang berhenti tepat di tujuan selalu terbaca
 * sebagai gerakan mekanis.
 *
 * Seluruh nilai animasi dibaca DI DALAM `graphicsLayer` dan lambda gambar, jadi
 * tiap frame hanya menggambar ulang — tanpa recomposition, tanpa relayout.
 *
 * [onSelesai] dipanggil setelah animasi habis, supaya pemanggil mengembalikan
 * kolom ketik seperti semula.
 */
@Composable
fun AnimasiBuangRekaman(
    modifier: Modifier = Modifier,
    onSelesai: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    val lempar = remember { Animatable(0f) }      // 0 = di tangan, 1 = di dasar tong
    val bukaTutup = remember { Animatable(0f) }   // 0 = tertutup, 1 = terbuka penuh
    val goyang = remember { Animatable(0f) }      // derajat, meredam sendiri
    val pudar = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        // 1. Tutup terlempar terbuka — pegas, jadi ada sedikit ayunan berlebih.
        launch {
            bukaTutup.animateTo(
                1f,
                spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
            )
        }

        // 2. Lemparan: waktunya LINIER, lintasannya yang melengkung. Easing
        //    ditaruh di rumus posisi, bukan di waktu, supaya percepatan turunnya
        //    terasa seperti gravitasi alih-alih animasi yang direm di ujung.
        launch {
            delay(60)
            lempar.animateTo(1f, tween(DURASI_LEMPAR_MS, easing = LinearEasing))
        }

        // 3. Tutup menutup tepat saat mikrofon melewati mulut tong.
        delay(60 + (DURASI_LEMPAR_MS * 0.82f).toLong())
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        launch {
            bukaTutup.animateTo(
                0f,
                spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessMediumLow),
            )
        }

        // 4. Hentakan mendarat: pegas dari 0 ke 0 dengan kecepatan awal, jadi
        //    tongnya bergoyang lalu diam sendiri — bukan tiga tween berurutan
        //    yang tersendat di tiap pergantian arah.
        launch {
            goyang.animateTo(
                targetValue = 0f,
                animationSpec = spring(dampingRatio = 0.22f, stiffness = 700f),
                initialVelocity = 52f,
            )
        }

        delay(260)
        pudar.animateTo(0f, tween(220, easing = LinearEasing))
        onSelesai()
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .graphicsLayer { alpha = pudar.value }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        val lebarPx = with(density) { maxWidth.toPx() }
        val tongPx = with(density) { UKURAN_TONG.toPx() }
        val micPx = with(density) { UKURAN_MIC.toPx() }

        // Titik berangkat: ujung kanan pita, tempat tombol mikrofon tadi berada.
        val mulaiX = lebarPx - micPx
        // Titik tujuan: tengah mulut tong.
        val akhirX = (tongPx - micPx) / 2f

        // Lapisan mikrofon DI BAWAH tong: begitu turun melewati mulutnya, badan
        // tong yang terisi putih menutupinya — tertelan, bukan sekadar memudar.
        Icon(
            imageVector = Icons.Filled.Mic,
            contentDescription = null,
            tint = Merah,
            modifier = Modifier
                .size(UKURAN_MIC)
                .zIndex(0f)
                .graphicsLayer {
                    val t = lempar.value

                    // Mendatar: melambat halus (ease-out kubik) tanpa pernah berhenti.
                    val mendatar = 1f - (1f - t) * (1f - t) * (1f - t)
                    translationX = mulaiX + (akhirX - mulaiX) * mendatar

                    // Menegak: parabola lemparan. Naik dulu karena kecepatan awal
                    // ke atas, lalu dipercepat turun sampai tenggelam sedalam
                    // 0,95 tinggi tong pada akhir lintasan.
                    val naikAwal = -2.6f * tongPx
                    val gravitasi = 3.55f * tongPx
                    translationY = gravitasi * t * t + naikAwal * t

                    // Mengecil dan berputar mengikuti waktu yang sama — satu
                    // sumber gerak untuk seluruh benda.
                    val kecil = 1f - 0.55f * t * t
                    scaleX = kecil
                    scaleY = kecil
                    rotationZ = 78f * t * t
                },
        )

        Box(
            modifier = Modifier
                .size(UKURAN_TONG)
                .zIndex(1f),
        ) {
            TongSampah(
                buka = { bukaTutup.value },
                goyang = { goyang.value },
                modifier = Modifier.size(UKURAN_TONG),
            )
        }
    }
}

/**
 * Tong sampah dengan tutup berengsel.
 *
 * Nilainya diterima sebagai lambda, bukan Float: dibaca di dalam blok gambar,
 * perubahannya hanya memicu fase draw — Canvas-nya sendiri tidak pernah
 * disusun ulang selama animasi berjalan.
 */
@Composable
private fun TongSampah(
    buka: () -> Float,
    goyang: () -> Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val garis = w * 0.085f

        rotate(degrees = goyang(), pivot = Offset(w / 2f, h)) {
            val kiriAtas = w * 0.17f
            val kananAtas = w * 0.83f
            val kiriBawah = w * 0.27f
            val kananBawah = w * 0.73f
            val atas = h * 0.32f
            val bawah = h * 0.95f

            val badan = Path().apply {
                moveTo(kiriAtas, atas)
                lineTo(kiriBawah, bawah)
                lineTo(kananBawah, bawah)
                lineTo(kananAtas, atas)
                close()
            }
            // Isi putih dulu: inilah yang menelan mikrofon saat ia jatuh masuk.
            drawPath(badan, Color.White)
            drawPath(badan, Merah, style = Stroke(width = garis))

            drawLine(
                Merah,
                Offset(w * 0.40f, atas + h * 0.12f),
                Offset(w * 0.42f, bawah - h * 0.09f),
                strokeWidth = garis * 0.8f,
                cap = StrokeCap.Round,
            )
            drawLine(
                Merah,
                Offset(w * 0.60f, atas + h * 0.12f),
                Offset(w * 0.58f, bawah - h * 0.09f),
                strokeWidth = garis * 0.8f,
                cap = StrokeCap.Round,
            )

            // Tutup + pegangannya, berputar pada engsel di ujung kiri.
            rotate(degrees = -48f * buka(), pivot = Offset(w * 0.10f, atas)) {
                drawLine(
                    Merah,
                    Offset(w * 0.08f, atas),
                    Offset(w * 0.92f, atas),
                    strokeWidth = garis,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    Merah,
                    Offset(w * 0.38f, atas - h * 0.14f),
                    Offset(w * 0.62f, atas - h * 0.14f),
                    strokeWidth = garis,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    Merah,
                    Offset(w * 0.38f, atas - h * 0.14f),
                    Offset(w * 0.38f, atas),
                    strokeWidth = garis * 0.8f,
                )
                drawLine(
                    Merah,
                    Offset(w * 0.62f, atas - h * 0.14f),
                    Offset(w * 0.62f, atas),
                    strokeWidth = garis * 0.8f,
                )
            }
        }
    }
}
