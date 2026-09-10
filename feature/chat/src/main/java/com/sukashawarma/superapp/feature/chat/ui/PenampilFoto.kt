package com.sukashawarma.superapp.feature.chat.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.sukashawarma.superapp.core.ui.AvatarStorage

/**
 * Penampil foto layar penuh, dipakai bersama oleh foto profil dan foto di dalam
 * percakapan — seperti membuka gambar di WhatsApp.
 *
 * Digambar sebagai [Dialog], bukan lapisan biasa di dalam layar. Dua alasannya:
 * pemanggil dari dalam `ModalBottomSheet` (kartu profil) hidup di jendela
 * terpisah, jadi lapisan biasa justru akan tertutup lembar itu sendiri; dan
 * sebagai dialog ia otomatis menutupi bilah sistem dengan benar dari mana pun
 * dibuka.
 *
 * [url] sudah berupa URL siap muat. Kedua bucket-nya privat, jadi pemuatnya
 * memakai `AvatarStorage.imageLoader` yang membawa token sesi — ImageLoader
 * biasa akan menerima 401 dan menampilkan kotak kosong.
 */
@Composable
fun PenampilFoto(
    url: String?,
    judul: String,
    keterangan: String? = null,
    onTutup: () -> Unit,
) {
    if (url == null) return
    BackHandler(onBack = onTutup)

    Dialog(
        onDismissRequest = onTutup,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        var skala by remember { mutableFloatStateOf(1f) }
        var geser by remember { mutableStateOf(Offset.Zero) }
        val transform = rememberTransformableState { ubahSkala, ubahGeser, _ ->
            skala = (skala * ubahSkala).coerceIn(1f, 4f)
            // Geseran dikunci balik ke tengah begitu skalanya kembali 1x. Tanpa
            // itu foto bisa terdorong keluar layar tanpa cara mengembalikannya.
            geser = if (skala <= 1f) Offset.Zero else geser + ubahGeser
        }

        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0xFF0B0B0F))
                .transformable(transform),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = url,
                imageLoader = AvatarStorage.imageLoader(LocalContext.current),
                contentDescription = judul,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 76.dp)
                    .graphicsLayer {
                        scaleX = skala
                        scaleY = skala
                        translationX = geser.x
                        translationY = geser.y
                    },
            )

            Row(
                Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0x66000000))
                        .clickable(onClick = onTutup),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Close, "Tutup foto", tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        judul,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    keterangan?.takeIf { it.isNotBlank() }?.let {
                        Text(it, fontSize = 12.sp, color = Color(0xCCFFFFFF), maxLines = 1)
                    }
                }
            }
        }
    }
}
