package com.sukashawarma.superapp.feature.chat.ui.suara

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay

private val Merah = Color(0xFFFF3B30)
private val BiruIos = Color(0xFF007AFF)
private val Abu = Color(0xFF8E8E93)
private val AbuMuda = Color(0xFFC7C7CC)

/** Geser sejauh ini ke kiri = batal; ke atas = kunci (tangan boleh dilepas). */
private const val AMBANG_BATAL_PX = 220f
private const val AMBANG_KUNCI_PX = 120f

/**
 * State gestur perekaman yang dibagi antara tombol mikrofon dan pita rekaman.
 *
 * Keduanya composable terpisah karena pita menutupi kolom ketik sementara
 * tombolnya harus tetap terpasang untuk menerima sentuhan yang sedang berjalan —
 * melepas tombol saat pita muncul akan memutus gestur yang sedang ditahan.
 */
class StatusRekam {
    var geserX by mutableFloatStateOf(0f)
    var geserY by mutableFloatStateOf(0f)
    var terkunci by mutableStateOf(false)

    /** Sedang memainkan animasi "dibuang ke tong sampah". Bukan bagian dari
     *  [reset] — justru dinyalakan setelahnya, saat rekamannya sudah dibatalkan. */
    var membuang by mutableStateOf(false)

    val akanBatal: Boolean get() = geserX <= -AMBANG_BATAL_PX

    /** 0f..1f — seberapa dekat jari ke gembok, untuk animasi jalur kunci. */
    val majuKunci: Float get() = ((-geserY) / AMBANG_KUNCI_PX).coerceIn(0f, 1f)

    /** 0f..1f — seberapa dekat ke pembatalan, untuk memudarkan pita. */
    val majuBatal: Float get() = ((-geserX) / AMBANG_BATAL_PX).coerceIn(0f, 1f)

    fun reset() {
        geserX = 0f
        geserY = 0f
        terkunci = false
    }
}

@Composable
fun rememberStatusRekam(): StatusRekam = remember { StatusRekam() }

/**
 * Tombol mikrofon ala WhatsApp: tahan untuk merekam, geser ke kiri untuk batal,
 * geser ke atas untuk mengunci supaya tangan bisa dilepas.
 *
 * Setelah terkunci, tombol yang sama berubah menjadi tombol KIRIM — bukan
 * tombol mikrofon yang menganggur. Di posisi itulah jempol sudah berada, dan
 * menaruh tombol kirim di tempat lain berarti tangan harus pindah untuk
 * menyelesaikan rekaman yang baru saja dikunci di sana.
 *
 * Izin mikrofon diminta di sini, bukan di layar pemanggil: tombol inilah satu-
 * satunya pintu masuk perekaman, jadi menaruh permintaannya di tempat lain
 * hanya membuat dua layar mengulang logika yang sama.
 */
@Composable
fun TombolMicChat(
    perekam: PerekamSuara,
    status: StatusRekam,
    onHasil: (PerekamSuara.Hasil) -> Unit,
    modifier: Modifier = Modifier,
    aktif: Boolean = true,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val mintaIzin = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    // Denyut perekam: sumber durasi dan cuplikan amplitudo untuk waveform hidup.
    LaunchedEffect(perekam.sedangMerekam) {
        while (perekam.sedangMerekam) {
            perekam.denyut()
            if (perekam.durasiMs >= DURASI_REKAM_MAKS_MS) {
                perekam.selesai()?.let(onHasil)
                status.reset()
                break
            }
            delay(90)
        }
    }

    val merekamBebas = perekam.sedangMerekam && !status.terkunci

    // Tombol membesar saat ditahan dan mengikuti jari ke kiri: perubahan yang
    // langsung terasa, bukan tombol diam yang menyisakan tebakan apakah
    // rekamannya benar-benar jalan.
    val skala by animateFloatAsState(
        targetValue = if (merekamBebas) 1.45f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 380f),
        label = "skala-mic"
    )

    Box(contentAlignment = Alignment.Center) {
        // Jalur kunci: melayang di ATAS tombol selama menahan, persis WhatsApp.
        if (merekamBebas) {
            JalurKunci(
                maju = status.majuKunci,
                modifier = Modifier
                    .offset(y = (-64).dp)
                    .zIndex(2f)
            )
        }

        // Halo berdenyut di belakang tombol — penanda "sedang merekam" yang
        // terbaca walau jari menutupi tombolnya.
        if (perekam.sedangMerekam) {
            HaloRekam(terkunci = status.terkunci)
        }

        Box(
            modifier = modifier
                .graphicsLayer {
                    translationX = if (merekamBebas) status.geserX.coerceAtLeast(-160f) else 0f
                    scaleX = skala
                    scaleY = skala
                }
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    when {
                        status.terkunci -> BiruIos
                        perekam.sedangMerekam -> Merah
                        else -> Color.Transparent
                    }
                )
                .then(
                    // Terkunci = tombol kirim biasa. Gestur tahan-dan-geser
                    // dimatikan supaya sentuhan singkat tidak dianggap rekaman baru.
                    if (status.terkunci) {
                        Modifier.clickable {
                            perekam.selesai()?.let(onHasil)
                            status.reset()
                        }
                    } else {
                        Modifier.pointerInput(aktif) {
                            if (!aktif) return@pointerInput
                            awaitEachGesture {
                                val turun = awaitFirstDown(requireUnconsumed = false)

                                val punyaIzin = ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED

                                if (!punyaIzin) {
                                    mintaIzin.launch(Manifest.permission.RECORD_AUDIO)
                                    return@awaitEachGesture
                                }

                                status.reset()
                                if (!perekam.mulai()) return@awaitEachGesture
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                                var batal = false
                                var kunci = false
                                var sudahGetarKunci = false
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val perubahan = event.changes.firstOrNull { it.id == turun.id } ?: break
                                    val dx = perubahan.position.x - turun.position.x
                                    val dy = perubahan.position.y - turun.position.y
                                    status.geserX = dx.coerceAtMost(0f)
                                    status.geserY = dy.coerceAtMost(0f)

                                    // Getar sekali saat jari melewati separuh jalur
                                    // kunci: penanda bahwa menggeser ke atas memang
                                    // melakukan sesuatu.
                                    if (!sudahGetarKunci && status.majuKunci > 0.5f) {
                                        sudahGetarKunci = true
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }

                                    if (dy < -AMBANG_KUNCI_PX) {
                                        kunci = true
                                        break
                                    }
                                    if (status.akanBatal) {
                                        batal = true
                                        break
                                    }
                                    if (!perubahan.pressed) break
                                    perubahan.consume()
                                }

                                when {
                                    kunci -> {
                                        status.terkunci = true
                                        status.geserX = 0f
                                        status.geserY = 0f
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                    batal -> {
                                        perekam.batal()
                                        status.reset()
                                        status.membuang = true
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                    else -> {
                                        perekam.selesai()?.let(onHasil)
                                        status.reset()
                                    }
                                }
                            }
                        }
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (status.terkunci) Icons.Filled.ArrowUpward else Icons.Filled.Mic,
                contentDescription = when {
                    status.terkunci -> "Kirim pesan suara"
                    perekam.sedangMerekam -> "Lepas untuk mengirim"
                    else -> "Tahan untuk merekam pesan suara"
                },
                tint = if (perekam.sedangMerekam) Color.White else BiruIos,
                modifier = Modifier.size(if (status.terkunci) 20.dp else 23.dp),
            )
        }
    }
}

/** Pil vertikal berisi gembok di atas tombol mikrofon; ikut mengunci saat didekati. */
@Composable
private fun JalurKunci(maju: Float, modifier: Modifier = Modifier) {
    val transisi = rememberInfiniteTransition(label = "kunci")
    val naik by transisi.animateFloat(
        initialValue = 0f,
        targetValue = -5f,
        animationSpec = infiniteRepeatable(tween(750, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "panah-naik"
    )

    Column(
        modifier = modifier
            .width(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = if (maju > 0.85f) Icons.Filled.Lock else Icons.Filled.LockOpen,
            contentDescription = null,
            tint = if (maju > 0.5f) BiruIos else Abu,
            modifier = Modifier
                .size(18.dp)
                .scale(1f + maju * 0.25f),
        )
        Spacer(Modifier.height(4.dp))
        Icon(
            imageVector = Icons.Filled.KeyboardArrowUp,
            contentDescription = null,
            tint = AbuMuda,
            modifier = Modifier
                .size(16.dp)
                .offset(y = naik.dp)
                .alpha(1f - maju),
        )
    }
}

/** Lingkaran lembut yang mengembang-mengempis di belakang tombol saat merekam. */
@Composable
private fun HaloRekam(terkunci: Boolean) {
    val transisi = rememberInfiniteTransition(label = "halo")
    val besar by transisi.animateFloat(
        initialValue = 1f,
        targetValue = 1.9f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "halo-skala"
    )
    val pudar by transisi.animateFloat(
        initialValue = 0.28f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "halo-alpha"
    )

    Box(
        modifier = Modifier
            .size(44.dp)
            .scale(besar)
            .alpha(pudar)
            .clip(CircleShape)
            .background(if (terkunci) BiruIos else Merah)
    )
}

/**
 * Apa pun yang menempati kolom ketik selama urusan rekaman berlangsung: pita
 * perekaman, atau animasi membuang rekaman setelah dibatalkan.
 *
 * Disatukan di sini supaya kedua layar chat cukup menanyakan satu hal — sedang
 * merekam atau sedang membuang — tanpa masing-masing menyusun ulang aturan
 * kapan animasi tong sampah muncul dan kapan berhenti.
 */
@Composable
fun PitaKomposerSuara(
    perekam: PerekamSuara,
    status: StatusRekam,
    modifier: Modifier = Modifier,
) {
    if (status.membuang) {
        AnimasiBuangRekaman(modifier = modifier) { status.membuang = false }
    } else {
        PitaRekamSuara(
            perekam = perekam,
            status = status,
            onBatal = {
                perekam.batal()
                status.reset()
                status.membuang = true
            },
            modifier = modifier,
        )
    }
}

/**
 * Pita yang menutupi kolom ketik selama merekam.
 *
 * Isinya berubah menurut keadaan: menahan (geser untuk batal), hampir batal
 * (peringatan merah), atau terkunci (tombol hapus + penanda gembok, sementara
 * tombol kirim menempati bekas posisi mikrofon).
 */
@Composable
private fun PitaRekamSuara(
    perekam: PerekamSuara,
    status: StatusRekam,
    onBatal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val transisi = rememberInfiniteTransition(label = "kedip")
    val alphaTitik by transisi.animateFloat(
        initialValue = 1f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "alpha-titik"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .padding(start = 14.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (status.terkunci) {
            // Hapus rekaman — satu-satunya jalan keluar setelah tangan dilepas.
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0x1AFF3B30))
                    .clickable(onClick = onBatal),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Buang rekaman",
                    tint = Merah,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
        } else {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Merah.copy(alpha = if (perekam.terjeda) 0.35f else alphaTitik))
            )
            Spacer(Modifier.width(10.dp))
        }

        Text(
            text = formatDurasiSuara(perekam.durasiMs),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1C1C1E),
        )

        Spacer(Modifier.width(12.dp))

        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            when {
                status.terkunci && perekam.terjeda -> Text(
                    text = "Dijeda — ketuk mikrofon untuk lanjut",
                    fontSize = 12.sp,
                    color = Abu,
                    modifier = Modifier.fillMaxWidth(),
                )

                status.terkunci -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GelombangHidup(
                        level = perekam.level,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }

                status.majuBatal > 0.45f -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = null,
                        tint = Merah,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Lepas untuk batal",
                        fontSize = 13.sp,
                        color = Merah,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                else -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        // Petunjuknya ikut bergeser bersama jari: isyarat bahwa
                        // menggeser memang sedang menuju sesuatu.
                        .graphicsLayer { translationX = status.geserX * 0.35f }
                        .alpha(1f - status.majuBatal * 0.7f),
                ) {
                    Icon(
                        imageVector = Icons.Filled.ChevronLeft,
                        contentDescription = null,
                        tint = Abu,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "Geser untuk batal",
                        fontSize = 13.sp,
                        color = Abu,
                    )
                }
            }
        }

        if (status.terkunci) {
            Spacer(Modifier.width(8.dp))
            // Jeda & lanjutkan, seperti WhatsApp: rekaman panjang boleh
            // ditahan sebentar tanpa harus dikirim atau dibuang dulu.
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (perekam.terjeda) Color(0x1A007AFF) else Color(0x14000000))
                    .clickable { if (perekam.terjeda) perekam.lanjut() else perekam.jeda() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (perekam.terjeda) Icons.Filled.Mic else Icons.Filled.Pause,
                    contentDescription = if (perekam.terjeda) "Lanjutkan merekam" else "Jeda rekaman",
                    tint = BiruIos,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = if (perekam.terjeda) "Rekaman dijeda" else "Perekaman terkunci",
                tint = if (perekam.terjeda) Abu else BiruIos,
                modifier = Modifier.size(15.dp),
            )
        }
    }
}

/** Waveform yang tumbuh dari cuplikan amplitudo terakhir selama merekam. */
@Composable
private fun GelombangHidup(level: List<Int>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        val tampil = level.takeLast(30)
        tampil.forEach { nilai ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((5 + nilai * 2.6f).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF9E9EA4))
            )
        }
    }
}
