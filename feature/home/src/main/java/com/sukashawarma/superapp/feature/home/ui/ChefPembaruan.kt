package com.sukashawarma.superapp.presentation.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
import kotlin.math.cos
import kotlin.math.sin

/** Keadaan yang diperankan chef roket di layar wajib update. */
enum class ModeChef {
    /** Belum dipasang: melayang santai di tempat (idle). */
    DIAM,

    /** Sedang dipasang: bodi bergetar di tempat & api berkobar bergerak dinamis, lalu meluncur ke atas. */
    SIBUK,

    /** Siap dipasang (kompatibilitas). */
    SIAP,

    /** Unduhan gagal: roket merunduk sedikit dengan kepulan asap dingin. */
    GAGAL,
}

/** Dimensi rasio asli maskot chef roket tanpa api (scripts/maskot/img_mascot_chef_rocket.png). */
internal const val MASKOT_ROKET_LEBAR = 881f
internal const val MASKOT_ROKET_TINGGI = 879f

/**
 * Maskot chef roket 3D beranimasi untuk layar wajib update.
 *
 * 1. Saat belum pasang: Animasi IDLE melayang santai naik-turun halus dengan api tenang.
 * 2. Saat sedang dipasang: Maskot bergerak di tempat (engine rumble frekuensi tinggi + goyang bodi)
 *    dan efek api bergerak dinamis (lidah api menari meliuk-liuk, berkobar, dan semburan percikan bara api).
 * 3. Saat dipasang: Meluncur ke atas (blast off) melesat menembus layar atas dengan akselerasi penuh,
 *    lalu memanggil [onMeluncurSelesai] agar aplikasi dapat ditutup dengan mulus.
 */
@Composable
fun ChefPembaruan(
    mode: ModeChef,
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") warnaLatar: Color = Color.White,
    onMeluncurSelesai: (() -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val transisi = rememberInfiniteTransition(label = "chef_roket_loop")

    // Melayang vertikal naik-turun halus (IDLE)
    val apung by transisi.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "apung_roket",
    )

    // Jam siklus berkelanjutan untuk flicker api dan getaran mesin
    val jamApi by transisi.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
        ),
        label = "jam_api",
    )

    // Goyangan santai saat IDLE
    val goyangSantai by transisi.animateFloat(
        initialValue = -1.2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "goyang_santai",
    )

    val pantul = remember { Animatable(1f) }
    val meluncurOffset = remember { Animatable(0f) }
    val meluncurScaleY = remember { Animatable(1f) }
    val meluncurScaleX = remember { Animatable(1f) }
    val intensitasApi = remember { Animatable(0.55f) }

    var sedangMemanaskan by remember { mutableStateOf(false) }
    var sedangMeluncur by remember { mutableStateOf(false) }

    // Rangkaian animasi transisi saat mode berubah
    LaunchedEffect(mode) {
        if (mode == ModeChef.SIBUK) {
            sedangMemanaskan = true
            sedangMeluncur = false
            meluncurOffset.snapTo(0f)
            meluncurScaleY.snapTo(1f)
            meluncurScaleX.snapTo(1f)
            intensitasApi.animateTo(1.6f, tween(300))

            // Fase 1: Bergerak di tempat dengan getaran kencang & api bergerak dinamis (1.6 detik)
            delay(1600)

            // Fase 2: Kompresi tenaga persiapan meluncur (200 ms)
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            launch { meluncurScaleY.animateTo(0.92f, tween(180)) }
            launch { meluncurScaleX.animateTo(1.06f, tween(180)) }
            delay(200)

            // Fase 3: Meluncur ke atas! (Blast off)
            sedangMemanaskan = false
            sedangMeluncur = true
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)

            launch {
                meluncurScaleY.animateTo(1.24f, tween(250))
                meluncurScaleY.animateTo(1.0f, tween(450))
            }
            launch {
                meluncurScaleX.animateTo(0.86f, tween(250))
                meluncurScaleX.animateTo(1.0f, tween(450))
            }

            // Melesat ke atas menembus dan menghilang melewati atas layar
            meluncurOffset.animateTo(
                targetValue = -1800f,
                animationSpec = tween(750, easing = FastOutLinearInEasing),
            )

            // Fase 4: Setelah roket keluar layar, panggil callback untuk menutup APK
            delay(150)
            onMeluncurSelesai?.invoke()
        } else {
            sedangMemanaskan = false
            sedangMeluncur = false
            meluncurOffset.snapTo(0f)
            meluncurScaleY.snapTo(1f)
            meluncurScaleX.snapTo(1f)
            intensitasApi.animateTo(if (mode == ModeChef.GAGAL) 0.15f else 0.55f, tween(400))
        }
    }

    val lapis = Modifier
        .fillMaxSize()
        .graphicsLayer {
            // Getaran mesin (engine rumble) saat sedang dipasang bergerak di tempat
            val rumbleX = if (sedangMemanaskan) (sin(jamApi * 55f * PI.toFloat()) * 2.4f * density) else 0f
            val rumbleY = if (sedangMemanaskan) (cos(jamApi * 46f * PI.toFloat()) * 1.8f * density) else 0f
            val goyangTempat = if (sedangMemanaskan) (sin(jamApi * 14f * PI.toFloat()) * 3.5f) else 0f

            // Geser sedikit ke kanan menyusuri sumbu roket saat meluncur
            val luncurX = if (sedangMeluncur) (-meluncurOffset.value * 0.22f * density) else 0f

            translationX = rumbleX + luncurX
            translationY = when {
                sedangMeluncur -> meluncurOffset.value * density
                sedangMemanaskan -> rumbleY
                else -> apung * 6.5f * density
            }

            rotationZ = when {
                sedangMeluncur -> -5.5f
                sedangMemanaskan -> -2.5f + goyangTempat
                mode == ModeChef.GAGAL -> 8f
                else -> goyangSantai
            }

            scaleX = pantul.value * meluncurScaleX.value
            scaleY = pantul.value * meluncurScaleY.value

            // Fade out saat mendekati ujung langit
            if (meluncurOffset.value < -1400f) {
                alpha = ((1800f + meluncurOffset.value) / 400f).coerceIn(0f, 1f)
            }
        }

    Box(
        modifier = modifier
            .aspectRatio(MASKOT_ROKET_LEBAR / MASKOT_ROKET_TINGGI)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                if (!sedangMeluncur && !sedangMemanaskan) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    scope.launch {
                        pantul.animateTo(1.07f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
                        pantul.animateTo(1f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium))
                    }
                }
            },
    ) {
        // Foto maskot chef memeluk roket 3D
        Image(
            painter = painterResource(R.drawable.img_mascot_chef_rocket),
            contentDescription = "Maskot Chef Memeluk Roket",
            contentScale = ContentScale.Fit,
            modifier = lapis,
        )

        // Efek visual dinamis: api bergerak & partikel semburan roket
        Canvas(modifier = lapis) {
            val px = PetaPikselRoket(size.width, size.height)

            if (intensitasApi.value > 0.1f) {
                gambarApiBergerak(
                    px = px,
                    jam = jamApi,
                    kekuatan = intensitasApi.value,
                    sedangMeluncur = sedangMeluncur,
                )
            }

            if (mode == ModeChef.GAGAL) {
                gambarAsapMati(px, jamApi)
            }
        }
    }
}

/* ---------------------------------------------------------------- pemetaan piksel */

/** Mengubah koordinat piksel gambar asli ke koordinat layar layer (ContentScale.Fit). */
private class PetaPikselRoket(lebar: Float, tinggi: Float) {
    val skala = minOf(lebar / MASKOT_ROKET_LEBAR, tinggi / MASKOT_TINGGI_PX_ROKET)
    private val ofsX = (lebar - MASKOT_ROKET_LEBAR * skala) / 2f
    private val ofsY = (tinggi - MASKOT_ROKET_TINGGI * skala) / 2f

    fun titik(x: Float, y: Float) = Offset(ofsX + x * skala, ofsY + y * skala)

    companion object {
        private const val MASKOT_TINGGI_PX_ROKET = MASKOT_ROKET_TINGGI
    }
}

/* ---------------------------------------------------------------- efek api bergerak */

private val WarnaIntiApi = Color(0xFFFFF9C4)
private val WarnaApiKuning = Color(0xFFFFD54F)
private val WarnaApiOranye = Color(0xFFFF7043)
private val WarnaApiMerah = Color(0xFFFF3D00)
private val WarnaAsapKelabu = Color(0xFF9E9E9E)

/**
 * Efek semburan api bergerak dinamis:
 * - Aura panas radial di lubang nozzle mesin
 * - 3 lapisan lidah api (merah-oranye, oranye-kuning, putih-inti) yang meliuk, menari, dan memanjang dinamis
 * - Semburan hujan partikel bara api berkecepatan tinggi
 */
private fun DrawScope.gambarApiBergerak(
    px: PetaPikselRoket,
    jam: Float,
    kekuatan: Float,
    sedangMeluncur: Boolean,
) {
    val nozzleX = 457f
    val nozzleY = 771f
    val pusatNozzle = px.titik(nozzleX, nozzleY)

    // Arah semburan api: tegak lurus corong nozzle roket
    val dirX = -0.450f
    val dirY = 0.893f
    val perpX = -dirY
    val perpY = dirX

    val faktorLuncur = if (sedangMeluncur) 2.2f else 1.0f

    // 1. Inti Aura Panas Mesin di Nozzle
    val denyutInti = sin(PI * ((jam * 4f) % 1f)).toFloat()
    val radiusAura = (65f + 25f * denyutInti) * px.skala * kekuatan * (if (sedangMeluncur) 1.5f else 1.0f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                WarnaIntiApi.copy(alpha = (0.75f * kekuatan).coerceAtMost(1f)),
                WarnaApiKuning.copy(alpha = (0.50f * kekuatan).coerceAtMost(0.9f)),
                WarnaApiOranye.copy(alpha = (0.25f * kekuatan).coerceAtMost(0.7f)),
                Color.Transparent,
            ),
            center = pusatNozzle,
            radius = radiusAura,
        ),
        radius = radiusAura,
        center = pusatNozzle,
    )

    // 2. Lidah-lidah Api Dinamis yang Bergerak & Meliuk
    // Lidah 1: Api Luar (Merah - Oranye)
    val panjangLuar = (240f + 65f * sin(jam * 18f * PI.toFloat())) * px.skala * kekuatan * faktorLuncur
    val lebarLuar = (44f + 8f * cos(jam * 16f * PI.toFloat())) * px.skala * kekuatan
    val liukLuar = sin(jam * 20f * PI.toFloat()) * 16f * px.skala * kekuatan
    gambarLidahApi(
        pusatNozzle = pusatNozzle,
        dirX = dirX, dirY = dirY,
        perpX = perpX, perpY = perpY,
        panjang = panjangLuar,
        lebar = lebarLuar,
        liuk = liukLuar,
        warnaPangkal = WarnaApiOranye,
        warnaUjung = WarnaApiMerah.copy(alpha = 0.85f),
    )

    // Lidah 2: Api Tengah (Oranye - Kuning Emas)
    val panjangTengah = (180f + 48f * sin((jam * 24f + 1.2f) * PI.toFloat())) * px.skala * kekuatan * faktorLuncur
    val lebarTengah = (32f + 6f * sin(jam * 22f * PI.toFloat())) * px.skala * kekuatan
    val liukTengah = cos(jam * 26f * PI.toFloat()) * 12f * px.skala * kekuatan
    gambarLidahApi(
        pusatNozzle = pusatNozzle,
        dirX = dirX, dirY = dirY,
        perpX = perpX, perpY = perpY,
        panjang = panjangTengah,
        lebar = lebarTengah,
        liuk = liukTengah,
        warnaPangkal = WarnaApiKuning,
        warnaUjung = WarnaApiOranye.copy(alpha = 0.9f),
    )

    // Lidah 3: Api Inti Terpanas (Putih - Kuning)
    val panjangInti = (110f + 32f * sin((jam * 30f + 2.4f) * PI.toFloat())) * px.skala * kekuatan * faktorLuncur
    val lebarInti = (20f + 4f * cos(jam * 28f * PI.toFloat())) * px.skala * kekuatan
    val liukInti = sin(jam * 32f * PI.toFloat()) * 6f * px.skala * kekuatan
    gambarLidahApi(
        pusatNozzle = pusatNozzle,
        dirX = dirX, dirY = dirY,
        perpX = perpX, perpY = perpY,
        panjang = panjangInti,
        lebar = lebarInti,
        liuk = liukInti,
        warnaPangkal = Color.White,
        warnaUjung = WarnaIntiApi,
    )

    // 3. Semburan Partikel Bara Api Berkecepatan Tinggi
    val jumlahPartikel = if (kekuatan > 1f) 14 else 8
    for (i in 0 until jumlahPartikel) {
        val progress = (jam * (if (sedangMeluncur) 3.5f else 2.5f) + i.toFloat() / jumlahPartikel) % 1f
        val jarak = (30f + (if (sedangMeluncur) 420f else 280f) * progress) * px.skala
        val sebaran = sin(i * 4.3f + jam * 8f) * (14f + 36f * progress) * px.skala

        val pxPos = pusatNozzle.x + (dirX * jarak + perpX * sebaran)
        val pyPos = pusatNozzle.y + (dirY * jarak + perpY * sebaran)

        val alpha = (sin(PI * progress).toFloat() * 0.95f * kekuatan.coerceAtMost(1.2f)).coerceIn(0f, 1f)
        val radius = (3.5f + (1f - progress) * 5.5f) * px.skala * (if (sedangMeluncur) 1.4f else 1.0f)

        val warna = when {
            progress < 0.25f -> Color.White
            progress < 0.55f -> WarnaIntiApi
            progress < 0.80f -> WarnaApiKuning
            else -> WarnaApiMerah
        }

        drawCircle(
            color = warna.copy(alpha = alpha),
            radius = radius,
            center = Offset(pxPos, pyPos),
        )
    }
}

/** Menggambar lidah api meliuk menggunakan kurva Bézier halus. */
private fun DrawScope.gambarLidahApi(
    pusatNozzle: Offset,
    dirX: Float, dirY: Float,
    perpX: Float, perpY: Float,
    panjang: Float,
    lebar: Float,
    liuk: Float,
    warnaPangkal: Color,
    warnaUjung: Color,
) {
    val kiriPangkal = Offset(pusatNozzle.x - perpX * lebar * 0.5f, pusatNozzle.y - perpY * lebar * 0.5f)
    val kananPangkal = Offset(pusatNozzle.x + perpX * lebar * 0.5f, pusatNozzle.y + perpY * lebar * 0.5f)

    val ujung = Offset(
        pusatNozzle.x + dirX * panjang + perpX * liuk,
        pusatNozzle.y + dirY * panjang + perpY * liuk,
    )

    val ctrlKiri = Offset(
        pusatNozzle.x + dirX * (panjang * 0.45f) - perpX * (lebar * 0.75f) + perpX * (liuk * 0.4f),
        pusatNozzle.y + dirY * (panjang * 0.45f) - perpY * (lebar * 0.75f) + perpY * (liuk * 0.4f),
    )
    val ctrlKanan = Offset(
        pusatNozzle.x + dirX * (panjang * 0.45f) + perpX * (lebar * 0.75f) + perpX * (liuk * 0.4f),
        pusatNozzle.y + dirY * (panjang * 0.45f) + perpY * (lebar * 0.75f) + perpY * (liuk * 0.4f),
    )

    val jalur = Path().apply {
        moveTo(kiriPangkal.x, kiriPangkal.y)
        quadraticBezierTo(ctrlKiri.x, ctrlKiri.y, ujung.x, ujung.y)
        quadraticBezierTo(ctrlKanan.x, ctrlKanan.y, kananPangkal.x, kananPangkal.y)
        close()
    }

    drawPath(
        path = jalur,
        brush = Brush.linearGradient(
            colors = listOf(warnaPangkal, warnaUjung, Color.Transparent),
            start = pusatNozzle,
            end = ujung,
        ),
    )
}

/** Kepulan asap abu-abu halus jika pengunduhan gagal. */
private fun DrawScope.gambarAsapMati(px: PetaPikselRoket, jam: Float) {
    val pusatNozzle = px.titik(457f, 771f)
    for (i in 0..2) {
        val f = (jam + i * 0.33f) % 1f
        val naik = 70f * f * px.skala
        val geser = sin(2 * PI * (f + i * 0.2f)).toFloat() * 20f * px.skala
        val r = (16f + 26f * f) * px.skala
        val alpha = ((1f - f) * 0.45f).coerceIn(0f, 1f)
        drawCircle(
            color = WarnaAsapKelabu.copy(alpha = alpha),
            radius = r,
            center = Offset(pusatNozzle.x + geser, pusatNozzle.y - naik),
        )
    }
}
