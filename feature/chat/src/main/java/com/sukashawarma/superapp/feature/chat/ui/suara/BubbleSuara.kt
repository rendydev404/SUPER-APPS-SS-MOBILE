package com.sukashawarma.superapp.feature.chat.ui.suara

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LEBAR_GELOMBANG = 150.dp
private val TINGGI_GELOMBANG = 30.dp
private const val JUMLAH_BILAH_TAMPIL = 34

/**
 * Bubble pesan suara: tombol putar, gelombang yang terisi mengikuti posisi
 * dengan knop yang bisa ditarik, durasi berjalan, dan pengubah kecepatan.
 *
 * Gelombangnya digambar dengan SATU Canvas, bukan puluhan Box: posisinya dibaca
 * di dalam blok gambar, jadi tiap denyut pemutar hanya memicu fase draw — tanpa
 * recomposition dan tanpa relayout, walau ada belasan bubble suara di layar.
 *
 * [bolehPutar] false berarti pemutaran ditahan sampai pemanggil mengizinkan
 * (dipakai mode pantau developer, yang bertanya lebih dulu sebelum suara
 * terdengar keras di tempat yang salah).
 */
@Composable
fun BubbleSuara(
    audioPath: String,
    audioMs: Int?,
    audioWave: String?,
    milikSendiri: Boolean,
    modifier: Modifier = Modifier,
    bolehPutar: Boolean = true,
    onButuhIzin: () -> Unit = {},
    sudahDiputar: Boolean = false,
    onMulaiPutar: () -> Unit = {},
) {
    val kunciAktif by PemutarSuara.kunciAktif.collectAsState()
    val aktif = kunciAktif == audioPath

    if (aktif) {
        BubbleSuaraAktif(
            audioPath = audioPath,
            audioMs = audioMs,
            audioWave = audioWave,
            milikSendiri = milikSendiri,
            modifier = modifier,
            bolehPutar = bolehPutar,
            onButuhIzin = onButuhIzin,
            sudahDiputar = sudahDiputar,
            onMulaiPutar = onMulaiPutar,
        )
    } else {
        BubbleSuaraDiam(
            audioPath = audioPath,
            audioMs = audioMs,
            audioWave = audioWave,
            milikSendiri = milikSendiri,
            modifier = modifier,
            bolehPutar = bolehPutar,
            onButuhIzin = onButuhIzin,
            sudahDiputar = sudahDiputar,
            onMulaiPutar = onMulaiPutar,
        )
    }
}

@Composable
private fun BubbleSuaraAktif(
    audioPath: String,
    audioMs: Int?,
    audioWave: String?,
    milikSendiri: Boolean,
    modifier: Modifier = Modifier,
    bolehPutar: Boolean = true,
    onButuhIzin: () -> Unit = {},
    sudahDiputar: Boolean = false,
    onMulaiPutar: () -> Unit = {},
) {
    val context = LocalContext.current
    val status by PemutarSuara.status.collectAsState()

    val warnaUtama = Color(0xFF1C1C1E)
    val warnaSekunder = Color(0xFF7C7C83)
    val warnaTerisi = Color(0xFF007AFF)
    val warnaKosong = if (milikSendiri) Color(0xFFC6C6CE) else Color(0xFFB2B2BC)
    val latarTombol = if (sudahDiputar) Color(0xFF007AFF) else Color(0xFF9DC9FF)

    val durasiTotal = if (status.durasiMs > 0) status.durasiMs else audioMs ?: 0
    val tinggiBilah = remember(audioWave) { waveKeTinggi(audioWave, JUMLAH_BILAH_TAMPIL) }
    var geser by remember { mutableFloatStateOf(-1f) }

    val interaksi = remember { MutableInteractionSource() }
    val ditekan by interaksi.collectIsPressedAsState()
    val skalaTombol by animateFloatAsState(
        targetValue = if (ditekan) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 900f),
        label = "tekan-putar",
    )

    Row(
        modifier = modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .scale(skalaTombol)
                .size(40.dp)
                .clip(CircleShape)
                .background(latarTombol)
                .clickable(interactionSource = interaksi, indication = null) {
                    if (!bolehPutar) {
                        onButuhIzin()
                    } else {
                        PemutarSuara.putar(context, audioPath)
                        onMulaiPutar()
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            when {
                status.memuat -> CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp),
                )
                status.berjalan -> Icon(
                    imageVector = Icons.Filled.Pause,
                    contentDescription = "Jeda",
                    tint = Color.White,
                    modifier = Modifier.size(21.dp),
                )
                else -> Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Putar pesan suara",
                    tint = Color.White,
                    modifier = Modifier.size(21.dp),
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        Column {
            Gelombang(
                tinggi = tinggiBilah,
                kemajuan = {
                    when {
                        geser >= 0f -> geser
                        durasiTotal > 0 -> (status.posisiMs.toFloat() / durasiTotal).coerceIn(0f, 1f)
                        else -> 0f
                    }
                },
                warnaTerisi = warnaTerisi,
                warnaKosong = warnaKosong,
                tampilkanKnop = { true },
                onGeser = { rasio -> geser = rasio },
                onLepas = { rasio ->
                    geser = -1f
                    if (durasiTotal > 0) {
                        PemutarSuara.geser((rasio * durasiTotal).toInt())
                    }
                },
                modifier = Modifier
                    .width(LEBAR_GELOMBANG)
                    .height(TINGGI_GELOMBANG),
            )

            Spacer(Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = null,
                    tint = warnaSekunder,
                    modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    text = if (status.berjalan || status.posisiMs > 0) {
                        formatDurasiSuara(status.posisiMs)
                    } else {
                        formatDurasiSuara(durasiTotal)
                    },
                    fontSize = 12.sp,
                    color = warnaSekunder,
                )

                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .background(Color(0x14000000))
                        .clickable { PemutarSuara.gantiKecepatan() }
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = "${status.kecepatan.toString().removeSuffix(".0")}x",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = warnaUtama,
                    )
                }
            }
        }
    }
}

@Composable
private fun BubbleSuaraDiam(
    audioPath: String,
    audioMs: Int?,
    audioWave: String?,
    milikSendiri: Boolean,
    modifier: Modifier = Modifier,
    bolehPutar: Boolean = true,
    onButuhIzin: () -> Unit = {},
    sudahDiputar: Boolean = false,
    onMulaiPutar: () -> Unit = {},
) {
    val context = LocalContext.current
    val warnaSekunder = Color(0xFF7C7C83)
    val warnaTerisi = Color(0xFF007AFF)
    val warnaKosong = if (milikSendiri) Color(0xFFC6C6CE) else Color(0xFFB2B2BC)
    val latarTombol = if (sudahDiputar) Color(0xFF007AFF) else Color(0xFF9DC9FF)

    val durasiTotal = audioMs ?: 0
    val tinggiBilah = remember(audioWave) { waveKeTinggi(audioWave, JUMLAH_BILAH_TAMPIL) }
    var geser by remember { mutableFloatStateOf(-1f) }

    val interaksi = remember { MutableInteractionSource() }
    val ditekan by interaksi.collectIsPressedAsState()
    val skalaTombol by animateFloatAsState(
        targetValue = if (ditekan) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 900f),
        label = "tekan-putar-diam",
    )

    Row(
        modifier = modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .scale(skalaTombol)
                .size(40.dp)
                .clip(CircleShape)
                .background(latarTombol)
                .clickable(interactionSource = interaksi, indication = null) {
                    if (!bolehPutar) {
                        onButuhIzin()
                    } else {
                        PemutarSuara.putar(context, audioPath)
                        onMulaiPutar()
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "Putar pesan suara",
                tint = Color.White,
                modifier = Modifier.size(21.dp),
            )
        }

        Spacer(Modifier.width(10.dp))

        Column {
            Gelombang(
                tinggi = tinggiBilah,
                kemajuan = { if (geser >= 0f) geser else 0f },
                warnaTerisi = warnaTerisi,
                warnaKosong = warnaKosong,
                tampilkanKnop = { geser >= 0f },
                onGeser = { rasio -> geser = rasio },
                onLepas = { rasio ->
                    geser = -1f
                    if (durasiTotal <= 0) return@Gelombang
                    if (bolehPutar) {
                        PemutarSuara.putar(
                            context,
                            audioPath,
                            mulaiDariMs = (rasio * durasiTotal).toInt(),
                        )
                        onMulaiPutar()
                    } else {
                        onButuhIzin()
                    }
                },
                modifier = Modifier
                    .width(LEBAR_GELOMBANG)
                    .height(TINGGI_GELOMBANG),
            )

            Spacer(Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = null,
                    tint = warnaSekunder,
                    modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    text = formatDurasiSuara(durasiTotal),
                    fontSize = 12.sp,
                    color = warnaSekunder,
                )
            }
        }
    }
}

/**
 * Gelombang suara: bilah terisi sampai posisi putar, sisanya redup, dan knop
 * bundar di batasnya yang bisa ditarik untuk melompat.
 *
 * [kemajuan] sengaja lambda: nilainya berubah belasan kali per detik selama
 * memutar, dan membacanya sebagai parameter biasa berarti seluruh bubble
 * disusun ulang setiap kali.
 */
@Composable
private fun Gelombang(
    tinggi: List<Float>,
    kemajuan: () -> Float,
    warnaTerisi: Color,
    warnaKosong: Color,
    tampilkanKnop: () -> Boolean,
    onGeser: (Float) -> Unit,
    onLepas: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onLepas((offset.x / size.width.toFloat()).coerceIn(0f, 1f))
                }
            }
            .pointerInput(Unit) {
                var posisi = 0f
                detectHorizontalDragGestures(
                    onDragStart = { awal ->
                        posisi = (awal.x / size.width.toFloat()).coerceIn(0f, 1f)
                        onGeser(posisi)
                    },
                    onDragEnd = { onLepas(posisi) },
                    onDragCancel = { onLepas(posisi) },
                ) { perubahan, geserPx ->
                    perubahan.consume()
                    posisi = (posisi + geserPx / size.width.toFloat()).coerceIn(0f, 1f)
                    onGeser(posisi)
                }
            },
    ) {
        val jumlah = tinggi.size
        if (jumlah == 0) return@Canvas

        // Lebar bilah dan jaraknya diturunkan dari lebar yang tersedia, bukan
        // angka dp tetap: dengan begitu bilah terakhir selalu berhenti tepat di
        // tepi kanan, berapa pun jumlah bilahnya.
        val lebarBilah = size.width / (jumlah * 1.72f)
        val jarak = (size.width - lebarBilah * jumlah) / (jumlah - 1).coerceAtLeast(1)
        val maju = kemajuan().coerceIn(0f, 1f)
        val batas = maju * size.width
        val radius = CornerRadius(lebarBilah / 2f, lebarBilah / 2f)

        tinggi.forEachIndexed { i, nilai ->
            val x = i * (lebarBilah + jarak)
            val h = (size.height * nilai).coerceAtLeast(lebarBilah)
            drawRoundRect(
                color = if (x + lebarBilah / 2f <= batas) warnaTerisi else warnaKosong,
                topLeft = Offset(x, (size.height - h) / 2f),
                size = Size(lebarBilah, h),
                cornerRadius = radius,
            )
        }

        // Knop hanya untuk rekaman yang sedang dipegang pemutar; di bubble lain
        // ia cuma jadi titik tak bermakna yang menempel di ujung kiri.
        if (tampilkanKnop()) {
            drawCircle(
                color = warnaTerisi,
                radius = size.height * 0.18f,
                center = Offset(batas.coerceIn(0f, size.width), size.height / 2f),
            )
        }
    }
}
