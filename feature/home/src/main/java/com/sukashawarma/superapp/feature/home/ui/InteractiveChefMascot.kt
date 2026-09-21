package com.sukashawarma.superapp.presentation.home

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random


/**
 * Sensor gyro/akselerometer untuk efek 3D Parallax Tilt fisik HP.
 * Didesain secara lifecycle-aware: sensor otomatis dimatikan saat background
 * untuk menjamin zero battery drain.
 */
@Composable
private fun rememberDeviceTilt(): State<androidx.compose.ui.geometry.Offset> {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val tiltState = remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    DisposableEffect(lifecycleOwner, context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (sensorManager == null || sensor == null) {
            return@DisposableEffect onDispose {}
        }

        var smoothedX = 0f
        var smoothedY = 0f
        val filterAlpha = 0.14f // Low-pass filter smoothing

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                val rawX: Float
                val rawY: Float

                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    val rotationMatrix = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    rawX = (orientation[2] / (Math.PI / 4).toFloat()).coerceIn(-1f, 1f)
                    rawY = ((orientation[1] + (Math.PI / 5).toFloat()) / (Math.PI / 4).toFloat()).coerceIn(-1f, 1f)
                } else {
                    rawX = (-event.values[0] / 5f).coerceIn(-1f, 1f)
                    rawY = ((event.values[1] - 5f) / 5f).coerceIn(-1f, 1f)
                }

                smoothedX += filterAlpha * (rawX - smoothedX)
                smoothedY += filterAlpha * (rawY - smoothedY)

                tiltState.value = androidx.compose.ui.geometry.Offset(smoothedX, smoothedY)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
            } else if (event == Lifecycle.Event.ON_PAUSE) {
                sensorManager.unregisterListener(listener)
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            sensorManager.unregisterListener(listener)
        }
    }

    return tiltState
}

/**
 * Maskot 3D Chef Dinamis & Interaktif:
 * 1. Idle Floating & Breathing Motion (Gerak mengapung & bernapas halus di RenderNode GPU)
 * 2. Tap Interaction: Spring Bounce + Haptic Feedback + Contextual Speech Bubble
 * 3. 5x Combo Tap Easter Egg: Putaran ceria 360° + letupan konfeti & shawarma emoji
 * 4. Gyroscope 3D Parallax Tilt: Merespons kemiringan fisik perangkat Android
 */
@Composable
fun InteractiveChefMascot(
    modifier: Modifier = Modifier,
    userName: String,
    todayAttendance: TodayAttendance?,
    stokKritis: Int?,
    onSpeechTrigger: ((text: String, hasAction: Boolean) -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val deviceTilt by rememberDeviceTilt()

    // 1. Idle Floating & Breathing (GPU-accelerated, zero recomposition)
    val infiniteTransition = rememberInfiniteTransition(label = "chef_idle_anim")
    val idleFloatY by infiniteTransition.animateFloat(
        initialValue = -3.5f,
        targetValue = 3.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "chef_float_y"
    )
    val idleTiltZ by infiniteTransition.animateFloat(
        initialValue = -1.2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(3600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "chef_tilt_z"
    )

    // 2. Tap & Bounce Animation (Responsif, ringan & bouncy)
    val bounceScale = remember { Animatable(1f) }

    // 4. Easter egg lambaian tangan: dipicu tiga ketukan cepat. `lambai` berjalan
    //    0→1 selama satu lambaian penuh; sudutnya = amplitudo × sin(3 gelombang) ×
    //    selubung sin(π·t), jadi mulai dan berhenti dari 0° tanpa sentakan.
    val lambai = remember { Animatable(0f) }
    var pemicuLambai by remember { mutableStateOf(0) }
    val ketukanTerakhir = remember { ArrayDeque<Long>() }
    LaunchedEffect(pemicuLambai) {
        if (pemicuLambai == 0) return@LaunchedEffect
        lambai.snapTo(0f)
        lambai.animateTo(1f, tween(2400, easing = LinearEasing))
        lambai.snapTo(0f)
    }

    // 3. Kedipan mata: 0 = terbuka, 1 = tertutup. Jeda acak 2,5–5 detik supaya
    //    tidak terasa seperti metronom; sesekali kedip ganda seperti orang sungguhan.
    //    Nilainya hanya dibaca di fase draw, jadi tidak memicu recomposition.
    val blink = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(2500L, 5000L))
            repeat(if (Random.nextInt(100) < 20) 2 else 1) {
                blink.animateTo(1f, tween(70, easing = FastOutLinearInEasing))
                blink.animateTo(0f, tween(120, easing = LinearOutSlowInEasing))
                delay(90)
            }
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomEnd
    ) {
        // 1. Gambar Maskot 3D Chef — tiga lapis (badan, tangan, manset) dengan
        //    transformasi fisik yang SAMA supaya tetap satu benda saat mengapung/miring.
        //    Tangan dipotong dari gambar asli (scripts/maskot/pisah-lapisan-maskot.py)
        //    dan diputar pada pergelangan di dalam manset; manset digambar paling atas
        //    supaya sambungannya tidak pernah terlihat.
        val lapisMaskot = Modifier
            .size(width = 196.dp, height = 208.dp)
            .graphicsLayer {
                // Nilai sensor fisik
                val tX = deviceTilt.x
                val tY = deviceTilt.y

                // Translasi Parallax & Idle Floating
                translationY = idleFloatY + (tY * 6.dp.toPx())
                translationX = tX * 7.dp.toPx()

                // Rotasi Z: Idle tilt + Tilt sensor
                rotationZ = idleTiltZ + (tX * 2.2f)

                // Rotasi 3D Perspektif realistis
                rotationY = tX * 5.5f
                rotationX = -tY * 4.0f

                // Skala Pegas saat Disentuh
                scaleX = bounceScale.value
                scaleY = bounceScale.value
            }

        Box(
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                    scope.launch {
                        bounceScale.animateTo(1.08f, spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioMediumBouncy))
                        bounceScale.animateTo(1.0f, spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioLowBouncy))
                    }

                    // Tiga ketukan dalam 900 ms = lambaian. Ketukan biasa tetap bicara.
                    val kini = System.currentTimeMillis()
                    ketukanTerakhir.addLast(kini)
                    while (ketukanTerakhir.size > 3) ketukanTerakhir.removeFirst()
                    if (ketukanTerakhir.size == 3 && kini - ketukanTerakhir.first() <= 900L && !lambai.isRunning) {
                        ketukanTerakhir.clear()
                        pemicuLambai++
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSpeechTrigger?.invoke("Halo ${userName.trim().split(" ").firstOrNull() ?: userName}! 👋", false)
                        return@clickable
                    }

                    val msg = generateChefMessage(userName, todayAttendance, stokKritis)
                    val hasAction = todayAttendance == null && "absen" in msg.lowercase()
                    onSpeechTrigger?.invoke(msg, hasAction)
                }
        ) {
            Image(
                painter = painterResource(id = com.sukashawarma.superapp.feature.home.R.drawable.img_mascot_chef_body),
                contentDescription = "Maskot Chef Suka Shawarma Interaktif",
                contentScale = ContentScale.Fit,
                modifier = lapisMaskot.drawWithContent {
                    drawContent()
                    gambarKelopakMata(blink.value)
                }
            )
            Image(
                painter = painterResource(id = com.sukashawarma.superapp.feature.home.R.drawable.img_mascot_chef_hand),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = lapisMaskot.graphicsLayer {
                    val t = lambai.value
                    if (t > 0f && t < 1f) {
                        val selubung = sin(Math.PI * t).toFloat()
                        rotationZ = LAMBAI_AMPLITUDO * sin(2.0 * Math.PI * LAMBAI_GELOMBANG * t).toFloat() * selubung
                        transformOrigin = titikPergelangan(size.width, size.height)
                    }
                }
            )
            Image(
                painter = painterResource(id = com.sukashawarma.superapp.feature.home.R.drawable.img_mascot_chef_cuff),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = lapisMaskot
            )
        }
    }
}

/* ---------- Lambaian tangan ---------- */

private const val LAMBAI_AMPLITUDO = 13f   // derajat
private const val LAMBAI_GELOMBANG = 3f    // jumlah ayunan penuh per lambaian

/** Pusat pergelangan di dalam manset, piksel gambar asli — titik putar tangan. */
private const val PERGELANGAN_X_PX = 255f
private const val PERGELANGAN_Y_PX = 292f

/** Titik putar sebagai pecahan ukuran layer, memperhitungkan offset `ContentScale.Fit`. */
private fun titikPergelangan(lebar: Float, tinggi: Float): TransformOrigin {
    val skala = minOf(lebar / MASKOT_LEBAR_PX, tinggi / MASKOT_TINGGI_PX)
    val ofsX = (lebar - MASKOT_LEBAR_PX * skala) / 2f
    val ofsY = (tinggi - MASKOT_TINGGI_PX * skala) / 2f
    return TransformOrigin(
        (ofsX + PERGELANGAN_X_PX * skala) / lebar,
        (ofsY + PERGELANGAN_Y_PX * skala) / tinggi,
    )
}

/* ---------- Kedipan mata ---------- */

/** Ukuran piksel asli maskot (scripts/maskot/img_mascot_chef_header.png); mata Ukuran piksel asli `img_mascot_chef_header.png`; titik mata diukur dari gambar itu. pergelangan diukur dari situ. */
private const val MASKOT_LEBAR_PX = 909f
private const val MASKOT_TINGGI_PX = 749f

/** Elips mata dalam piksel gambar asli: pusat dan setengah sumbu. */
private class Mata(val cx: Float, val cy: Float, val rx: Float, val ry: Float, val kulit: Color)

// Warna kelopak = rata-rata kulit di sekeliling tiap mata (diukur dari gambar);
// mata kiri lebih gelap karena berada di sisi bayangan wajah.
private val MATA_KIRI = Mata(cx = 524f, cy = 186f, rx = 27f, ry = 16f, kulit = Color(0xFFE8955F))
private val MATA_KANAN = Mata(cx = 589f, cy = 180f, rx = 19f, ry = 15f, kulit = Color(0xFFF4AA79))

private val WARNA_BULU_MATA = Color(0xFF3A2418)

/**
 * Menggambar kelopak mata berwarna kulit yang turun dari atas menutupi bola mata.
 * Gambar ditata `ContentScale.Fit`, jadi posisi mata dihitung ulang dari skala
 * dan offset yang sama supaya kelopak tetap menempel di mata pada ukuran apa pun.
 */
private fun DrawScope.gambarKelopakMata(tutup: Float) {
    if (tutup <= 0.01f) return
    val skala = minOf(size.width / MASKOT_LEBAR_PX, size.height / MASKOT_TINGGI_PX)
    val ofsX = (size.width - MASKOT_LEBAR_PX * skala) / 2f
    val ofsY = (size.height - MASKOT_TINGGI_PX * skala) / 2f

    for (m in listOf(MATA_KIRI, MATA_KANAN)) {
        // Kelopak sedikit lebih besar dari mata agar tepi putihnya tidak mengintip.
        val rx = (m.rx + 3f) * skala
        val ry = (m.ry + 3f) * skala
        val cx = ofsX + m.cx * skala
        val atas = ofsY + (m.cy - m.ry - 3f) * skala
        val tinggi = ry * 2f * tutup
        drawOval(
            color = m.kulit,
            topLeft = Offset(cx - rx, atas),
            size = Size(rx * 2f, tinggi.coerceAtLeast(1f)),
        )
        // Lengkung bulu mata di tepi bawah kelopak, cembung ke bawah seperti mata
        // terpejam yang tersenyum — inilah yang membuatnya terbaca sebagai kedipan,
        // bukan sekadar bercak kulit.
        if (tutup > 0.55f) {
            val alpha = ((tutup - 0.55f) / 0.45f).coerceIn(0f, 1f)
            val lebar = rx * 1.5f
            val tinggiLengkung = ry * 0.9f
            drawArc(
                color = WARNA_BULU_MATA.copy(alpha = alpha),
                startAngle = 20f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(cx - lebar / 2f, atas + tinggi - tinggiLengkung),
                size = Size(lebar, tinggiLengkung),
                style = Stroke(width = 2.4f * skala, cap = StrokeCap.Round),
            )
        }
    }
}

/**
 * Balon obrolan kartun awan (Cloud Speech/Thought Bubble) dengan aset PNG mandiri,
 * efek z-index tinggi di atas teks username, dan ekor trail bulatan mengarah ke topi chef.
 */
@Composable
fun CartoonCloudSpeechBubble(
    text: String,
    hasAction: Boolean,
    onActionClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(138.dp)
            .height(84.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    if (hasAction) onActionClick() else onDismiss()
                }
            )
    ) {
        // 1. Gambar Aset PNG Awan Kartun Transparan Studio-Quality
        Image(
            painter = painterResource(id = com.sukashawarma.superapp.feature.home.R.drawable.img_cartoon_cloud_bubble),
            contentDescription = "Cartoon Cloud Speech Bubble",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Konten Teks & Text Link di dalam rongga awan putih bersih
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 12.dp, top = 11.dp, end = 32.dp, bottom = 11.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = text,
                fontSize = 8.sp,
                lineHeight = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            if (hasAction) {
                Spacer(Modifier.height(3.dp))
                Row(
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onActionClick
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Absen sekarang",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEA580C)
                    )
                    Spacer(Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color(0xFFEA580C),
                        modifier = Modifier.size(8.dp)
                    )
                }
            }
        }
    }
}

/**
 * Generator kata-kata kontekstual sesuai status absensi, stok kritis, dan jam kerja.
 */
private fun generateChefMessage(
    userName: String,
    att: TodayAttendance?,
    stokKritis: Int?
): String {
    val firstName = userName.trim().split(" ").firstOrNull() ?: userName
    val randomRoll = Random.nextInt(100)
    return when {
        att == null -> {
            listOf(
                "Halo $firstName! Yuk absen masuk dulu ✨",
                "Semangat shift baru! Absen dulu yuk $firstName! 🌯",
                "Udah siap hari ini? Absen dulu ya $firstName! 🔥",
                "Senyum dulu! Jangan lupa absen masuk ya 😊"
            ).random()
        }
        stokKritis != null && stokKritis > 0 && randomRoll < 35 -> {
            "Perhatian! Ada $stokKritis bahan menipis di gudang 📦⚠️"
        }
        att.type == "in" -> {
            listOf(
                "Semangat shift-nya $firstName! Shawarma terbaik menanti 🌯🔥",
                "Tips Chef: Jaga senyum ramah & saus garlic merata! 😄✨",
                "Kerja hebat hari ini! Layani pelanggan sepenuh hati 🙌",
                "Tetap terhidrasi ya $firstName, minum air putih dulu! 💧",
                "Shawarma lezat di tangan tim terbaik hari ini! 💪🌯"
            ).random()
        }
        else -> {
            "Kerja luar biasa hari ini, $firstName! Selamat istirahat! 🌙🎉"
        }
    }
}
