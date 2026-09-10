package com.sukashawarma.superapp.feature.chat.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Menu tekan-lama ala iOS: latar meredup dan mengabur, pesannya sendiri tetap
 * tajam dan mengambang, dengan baris emoji di atas dan daftar tindakan di bawah.
 *
 * Pengaburan latar dikerjakan PEMANGGIL (Modifier.blur pada isi chat), bukan di
 * sini — sebuah lapisan tidak bisa mengaburkan apa yang ada di belakangnya
 * sendiri. Di bawah Android 12 `Modifier.blur` tidak berefek, jadi scrim di sini
 * sengaja cukup gelap untuk berdiri sendiri tanpa blur.
 */

/** Reaksi cepat, urutan mengikuti kebiasaan WhatsApp. */
val EMOJI_REAKSI = listOf("👍", "❤️", "😂", "😮", "😢", "🙏")

private val PutihKartu = Color(0xFFF7F7F8)
private val AbuIkonMenu = Color(0xFF8E8E93)
private val TeksAksi = Color(0xFF000000)
private val MerahAksi = Color(0xFFFF3B30)
private val GarisAksi = Color(0x1F3C3C43)

@Composable
fun MenuPesanPopup(
    milikSendiri: Boolean,
    emojiTerpilih: String?,
    bolehHapus: Boolean,
    bolehSunting: Boolean,
    adaTeks: Boolean,
    onEmoji: (String) -> Unit,
    onSemuaEmoji: () -> Unit,
    onBalas: () -> Unit,
    onSunting: () -> Unit,
    onSalin: () -> Unit,
    onHapus: () -> Unit,
    onTutup: () -> Unit,
    bubble: @Composable () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val tampil = remember { MutableTransitionState(false).apply { targetState = true } }
    LaunchedEffect(Unit) { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }

    BackHandler(onBack = onTutup)

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x59000000))
            // Sentuhan di mana pun di luar kartu menutup menu, seperti iOS.
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTutup,
            ),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = if (milikSendiri) Alignment.End else Alignment.Start,
            verticalArrangement = Arrangement.Center,
        ) {
            AnimatedVisibility(
                visibleState = tampil,
                enter = fadeIn(tween(140)) + scaleIn(
                    initialScale = 0.86f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                ),
                exit = fadeOut(tween(90)) + scaleOut(targetScale = 0.9f),
            ) {
                Column(horizontalAlignment = if (milikSendiri) Alignment.End else Alignment.Start) {
                    BarisEmoji(
                        terpilih = emojiTerpilih,
                        onPilih = { emoji ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onEmoji(emoji)
                        },
                        onSemuaEmoji = onSemuaEmoji,
                    )
                    Spacer(Modifier.height(12.dp))

                    // Bubble aslinya digambar ulang di sini supaya terlihat
                    // terangkat dari daftar, bukan tertutup lapisan gelap.
                    Box(
                        Modifier.pointerInput(Unit) { /* sentuhan pada bubble tidak menutup menu */ },
                    ) { bubble() }

                    Spacer(Modifier.height(12.dp))
                    KartuAksi(
                        adaTeks = adaTeks,
                        bolehHapus = bolehHapus,
                        bolehSunting = bolehSunting,
                        onBalas = onBalas,
                        onSunting = onSunting,
                        onSalin = onSalin,
                        onHapus = onHapus,
                    )
                }
            }
        }
    }
}

@Composable
private fun BarisEmoji(terpilih: String?, onPilih: (String) -> Unit, onSemuaEmoji: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(26.dp))
            .background(Color.White)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EMOJI_REAKSI.forEach { emoji ->
            val aktif = emoji == terpilih
            val skala by animateFloatAsState(
                targetValue = if (aktif) 1.18f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "skala-$emoji",
            )
            Box(
                Modifier
                    .padding(horizontal = 2.dp)
                    .size(42.dp)
                    .graphicsLayer { scaleX = skala; scaleY = skala }
                    .clip(CircleShape)
                    .background(if (aktif) Color(0x1F007AFF) else Color.Transparent)
                    .clickable { onPilih(emoji) },
                contentAlignment = Alignment.Center,
            ) {
                Text(emoji, fontSize = 24.sp)
            }
        }
        // Enam pintasan tidak akan pernah cukup; tombol ini membuka papan emoji
        // yang sama dengan yang dipakai kotak ketik — logika Telegram.
        Box(
            Modifier
                .padding(start = 2.dp)
                .size(42.dp)
                .clip(CircleShape)
                .background(Color(0xFFF0F0F2))
                .clickable(onClick = onSemuaEmoji),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Add, "Emoji lainnya", tint = AbuIkonMenu, modifier = Modifier.size(21.dp))
        }
    }
}

@Composable
private fun KartuAksi(
    adaTeks: Boolean,
    bolehHapus: Boolean,
    bolehSunting: Boolean,
    onBalas: () -> Unit,
    onSunting: () -> Unit,
    onSalin: () -> Unit,
    onHapus: () -> Unit,
) {
    Column(
        Modifier
            .widthIn(min = 210.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(PutihKartu),
    ) {
        BarisAksi("Balas", Icons.AutoMirrored.Filled.Reply, TeksAksi, onBalas)
        if (bolehSunting) {
            Pemisah()
            BarisAksi("Edit", Icons.Filled.Edit, TeksAksi, onSunting)
        }
        if (adaTeks) {
            Pemisah()
            BarisAksi("Salin", Icons.Filled.ContentCopy, TeksAksi, onSalin)
        }
        if (bolehHapus) {
            Pemisah()
            BarisAksi("Hapus", Icons.Filled.Delete, MerahAksi, onHapus)
        }
    }
}

@Composable
private fun Pemisah() {
    Box(Modifier.fillMaxWidth().height(0.5.dp).background(GarisAksi))
}

@Composable
private fun BarisAksi(label: String, ikon: ImageVector, warna: Color, onKlik: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onKlik)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 16.sp, color = warna, fontWeight = FontWeight.Normal)
        Spacer(Modifier.width(24.dp))
        Icon(ikon, null, tint = warna, modifier = Modifier.size(19.dp))
    }
}
