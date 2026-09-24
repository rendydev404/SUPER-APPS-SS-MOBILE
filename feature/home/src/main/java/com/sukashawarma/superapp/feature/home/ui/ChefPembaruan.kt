package com.sukashawarma.superapp.presentation.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sukashawarma.superapp.feature.home.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** Keadaan yang diperankan chef di layar wajib update. */
enum class ModeChef {
    /** Update tersedia, belum diunduh: chef melambai mengajak. */
    DIAM,

    /** Sedang mengunduh atau memasang: chef bergoyang bersemangat, shawarma mengepul. */
    SIBUK,

    /** Siap dipasang: chef melambai dan lencana centang muncul. */
    SIAP,

    /** Unduhan gagal: chef tertunduk sedikit, tanpa kepulan. */
    GAGAL,
}

/**
 * Maskot chef beranimasi untuk layar wajib update.
 *
 * Memakai rig yang sama dengan [InteractiveChefMascot] — badan, tangan yang
 * berputar di pergelangan, manset, dan kelopak mata yang digambar — ditambah
 * efek vektor yang digambar langsung di Canvas: kepulan uap dari shawarma,
 * kilau di sekitar tangan yang melambai, dan lencana centang.
 *
 * Seluruhnya Compose murni (tanpa pustaka animasi pihak ketiga), dan semua nilai
 * animasi hanya dibaca di fase gambar/graphicsLayer, jadi tidak memicu
 * recomposition setiap frame.
 */
@Composable
fun ChefPembaruan(mode: ModeChef, modifier: Modifier = Modifier, warnaLatar: Color = Color.White) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val transisi = rememberInfiniteTransition(label = "chef_update")
    val apung by transisi.animateFloat(
        -1f, 1f,
        infiniteRepeatable(tween(if (mode == ModeChef.SIBUK) 700 else 2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "apung",
    )
    // Jam bersama untuk efek berulang (uap, kilau). Satu siklus = 2,4 detik.
    val jam by transisi.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(2400, easing = LinearEasing)),
        label = "jam",
    )

    val pantul = remember { Animatable(1f) }
    val lambai = remember { Animatable(0f) }
    val centang = remember { Animatable(0f) }
    val kedip = remember { Animatable(0f) }
    val tunduk by animateFloatAsState(if (mode == ModeChef.GAGAL) -5f else 0f, tween(600), label = "tunduk")
    val uap by animateFloatAsState(if (mode == ModeChef.SIBUK) 1f else 0f, tween(500), label = "uap")

    suspend fun lambaikan() {
        if (lambai.isRunning) return
        lambai.snapTo(0f)
        lambai.animateTo(1f, tween(1_800, easing = LinearEasing))
        lambai.snapTo(0f)
    }

    // Perilaku per keadaan.
    LaunchedEffect(mode) {
        when (mode) {
            ModeChef.DIAM -> {
                centang.snapTo(0f)
                delay(600)
                while (true) {
                    lambaikan()
                    delay(3_200)
                }
            }
            ModeChef.SIAP -> {
                launch { lambaikan() }
                centang.snapTo(0f)
                centang.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow))
            }
            else -> centang.animateTo(0f, tween(200))
        }
    }

    // Kedipan acak, sama seperti maskot di Beranda.
    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(2_200L, 4_500L))
            kedip.animateTo(1f, tween(70))
            kedip.animateTo(0f, tween(120))
        }
    }

    val lapis = Modifier
        .fillMaxSize()
        .graphicsLayer {
            translationY = apung * (if (mode == ModeChef.SIBUK) 3f else 6f) * density + (if (mode == ModeChef.GAGAL) 6f * density else 0f)
            rotationZ = tunduk
            scaleX = pantul.value
            scaleY = pantul.value
        }

    Box(
        modifier
            .aspectRatio(MASKOT_LEBAR_PX / MASKOT_TINGGI_PX)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                scope.launch {
                    pantul.animateTo(1.06f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
                    pantul.animateTo(1f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium))
                }
                scope.launch { lambaikan() }
            },
    ) {
        Image(
            painterResource(R.drawable.img_mascot_chef_body),
            contentDescription = "Maskot chef",
            contentScale = ContentScale.Fit,
            modifier = lapis.drawWithContent {
                drawContent()
                gambarKelopakMata(kedip.value)
            },
        )
        Image(
            painterResource(R.drawable.img_mascot_chef_hand),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = lapis.graphicsLayer {
                val t = lambai.value
                if (t > 0f && t < 1f) {
                    val selubung = sin(PI * t).toFloat()
                    rotationZ = LAMBAI_AMPLITUDO * 1.3f * sin(2.0 * PI * LAMBAI_GELOMBANG * t).toFloat() * selubung
                    transformOrigin = titikPergelangan(size.width, size.height)
                }
            },
        )
        Image(
            painterResource(R.drawable.img_mascot_chef_cuff),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = lapis,
        )

        // Lapisan efek vektor, mengikuti gerak badan yang sama.
        Canvas(lapis) {
            val px = PetaPiksel(size.width, size.height)
            if (uap > 0.01f) gambarUap(px, jam, uap)
            val t = lambai.value
            if (t > 0f && t < 1f) gambarKilau(px, jam, sin(PI * t).toFloat())
            if (centang.value > 0.01f) gambarCentang(px, centang.value)
        }

        // Gambar asli terpotong datar di perut; bagian bawahnya dileburkan ke
        // warna latar supaya chef terlihat "berdiri di balik" layar, bukan terpotong.
        Canvas(Modifier.fillMaxSize()) {
            val mulai = size.height * 0.72f
            // Chef melayang turun sampai ~12dp melewati batas bawah; gradasi dan
            // blok penutup diperpanjang supaya tepi potongan gambar tidak pernah muncul.
            val lebihan = 16.dp.toPx()
            drawRect(warnaLatar, topLeft = Offset(0f, size.height), size = androidx.compose.ui.geometry.Size(size.width, lebihan))
            drawRect(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(warnaLatar.copy(alpha = 0f), warnaLatar),
                    startY = mulai,
                    endY = size.height,
                ),
                topLeft = Offset(0f, mulai),
                size = androidx.compose.ui.geometry.Size(size.width, size.height - mulai),
            )
        }
    }
}

/* ---------------------------------------------------------------- efek vektor */

/** Mengubah koordinat piksel gambar asli ke koordinat layer (ContentScale.Fit). */
private class PetaPiksel(lebar: Float, tinggi: Float) {
    val skala = minOf(lebar / MASKOT_LEBAR_PX, tinggi / MASKOT_TINGGI_PX)
    private val ofsX = (lebar - MASKOT_LEBAR_PX * skala) / 2f
    private val ofsY = (tinggi - MASKOT_TINGGI_PX * skala) / 2f
    fun titik(x: Float, y: Float) = Offset(ofsX + x * skala, ofsY + y * skala)
}

private val WarnaUap = Color(0xFFA8968A)
private val WarnaKilau = Color(0xFFFFB547)
private val WarnaCentang = Color(0xFF34C759)

/**
 * Tiga gulungan uap berbentuk S yang naik dari ujung shawarma dan memudar —
 * "chef sedang memasak versi baru". Tiap gulungan berselisih sepertiga siklus.
 */
private fun DrawScope.gambarUap(px: PetaPiksel, jam: Float, kekuatan: Float) {
    val dasar = listOf(812f to 300f, 852f to 292f, 884f to 306f)
    dasar.forEachIndexed { i, (bx, by) ->
        val f = (jam + i / 3f) % 1f
        val naik = 150f * f
        val goyang = 10f * sin(2 * PI * (f * 1.5f + i * 0.3f)).toFloat()
        val alpha = (sin(PI * f).toFloat() * 0.9f * kekuatan).coerceIn(0f, 1f)
        val panjang = 46f
        val x = bx + goyang
        val y = by - naik
        val jalur = Path().apply {
            val a = px.titik(x, y)
            val c1 = px.titik(x + 12f, y - panjang * 0.33f)
            val c2 = px.titik(x - 12f, y - panjang * 0.66f)
            val b = px.titik(x, y - panjang)
            moveTo(a.x, a.y)
            cubicTo(c1.x, c1.y, c2.x, c2.y, b.x, b.y)
        }
        drawPath(
            jalur,
            WarnaUap.copy(alpha = alpha),
            style = Stroke(width = 10f * px.skala * (1f - f * 0.4f), cap = StrokeCap.Round),
        )
    }
}

/** Kilau bintang empat sudut yang berdenyut di sekitar telapak tangan saat melambai. */
private fun DrawScope.gambarKilau(px: PetaPiksel, jam: Float, kekuatan: Float) {
    val posisi = listOf(Triple(70f, 60f, 0f), Triple(330f, 60f, 0.33f), Triple(60f, 250f, 0.66f))
    posisi.forEach { (x, y, fase) ->
        val denyut = sin(PI * ((jam * 2f + fase) % 1f)).toFloat()
        val r = (20f + 26f * denyut) * px.skala * kekuatan
        if (r < 0.5f) return@forEach
        val p = px.titik(x, y)
        val bintang = Path().apply {
            moveTo(p.x, p.y - r)
            quadraticBezierTo(p.x, p.y, p.x + r, p.y)
            quadraticBezierTo(p.x, p.y, p.x, p.y + r)
            quadraticBezierTo(p.x, p.y, p.x - r, p.y)
            quadraticBezierTo(p.x, p.y, p.x, p.y - r)
            close()
        }
        drawPath(bintang, WarnaKilau.copy(alpha = (0.35f + 0.65f * denyut) * kekuatan))
    }
}

/** Lencana bulat hijau bercentang yang muncul memantul di samping topi chef. */
private fun DrawScope.gambarCentang(px: PetaPiksel, skalaLencana: Float) {
    val pusat = px.titik(700f, 80f)
    val r = 44f * px.skala * skalaLencana
    drawCircle(Color.White, radius = r + 5f * px.skala * skalaLencana, center = pusat)
    drawCircle(WarnaCentang, radius = r, center = pusat)
    val centang = Path().apply {
        moveTo(pusat.x - r * 0.42f, pusat.y + r * 0.02f)
        lineTo(pusat.x - r * 0.1f, pusat.y + r * 0.34f)
        lineTo(pusat.x + r * 0.45f, pusat.y - r * 0.3f)
    }
    drawPath(
        centang,
        Color.White,
        style = Stroke(width = r * 0.2f, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round),
    )
}
