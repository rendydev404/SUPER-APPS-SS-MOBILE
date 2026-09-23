package com.sukashawarma.superapp.feature.chat.ui

import com.sukashawarma.superapp.core.ui.ios.BarisIos
import com.sukashawarma.superapp.core.ui.ios.GrupIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.PemisahIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Alur kirim foto meniru WhatsApp: ketuk lampiran -> pilih sumber -> layar
 * pratinjau penuh berlatar hitam tempat keterangan diketik -> kirim.
 *
 * Yang membedakannya dari dialog kecil sebelumnya: fotonya terlihat sebesar
 * mungkin sebelum dikirim, dan keterangan diketik di tempat yang sama dengan
 * tombol kirim — bukan di kotak dialog yang memotong gambar jadi kartu kecil.
 */

private val BiruIosLampiran = WarnaIos.Biru
private val AbuTeks = WarnaIos.Abu

/** Foto yang sudah dikompres dan siap dikirim, beserta pratinjaunya. */
data class FotoTerpilih(
    val pratinjau: ImageBitmap,
    val webp: ByteArray,
) {
    // ByteArray membuat equals/hashCode bawaan data class membandingkan acuan;
    // dibiarkan begitu memang cukup di sini (dipakai sebagai state sekali pakai),
    // tapi override-nya ditulis eksplisit supaya tidak ada yang mengandalkan
    // perbandingan isi secara tak sengaja.
    override fun equals(other: Any?) = this === other
    override fun hashCode() = System.identityHashCode(this)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LembarPilihSumberFoto(
    onGaleri: () -> Unit,
    onKamera: () -> Unit,
    onTutup: () -> Unit,
) {
    val state = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onTutup,
        sheetState = state,
        containerColor = WarnaIos.Latar,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = UkuranIos.TepiLayar)
                .padding(bottom = 32.dp),
        ) {
            Text(
                "Kirim foto",
                style = TipeIos.Utama,
                modifier = Modifier.padding(start = 4.dp, bottom = 12.dp),
            )
            // Lembar aksi ala iOS: dua baris dalam satu grup, bukan petak ikon.
            GrupIos {
                BarisIos(
                    judul = "Galeri",
                    ikon = Icons.Filled.PhotoLibrary,
                    nadaIkon = NadaIos.UNGU,
                    onKlik = onGaleri,
                )
                PemisahIos(inset = 58.dp)
                BarisIos(
                    judul = "Kamera",
                    ikon = Icons.Filled.PhotoCamera,
                    nadaIkon = NadaIos.BAHAYA,
                    onKlik = onKamera,
                )
            }
        }
    }
}

/**
 * Layar pratinjau penuh sebelum foto dikirim. Digambar sebagai lapisan di atas
 * layar chat (bukan Dialog) supaya papan ketik dan inset sistem berperilaku
 * sama persis dengan komposer chat di bawahnya.
 */
@Composable
fun PratinjauKirimFoto(
    foto: FotoTerpilih,
    namaBalasan: String?,
    onKirim: (keterangan: String) -> Unit,
    onBatal: () -> Unit,
) {
    var keterangan by remember { mutableStateOf("") }
    BackHandler(onBack = onBatal)

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B0F))
            // Menelan sentuhan supaya tidak tembus ke daftar chat di belakangnya.
            .pointerInput(Unit) {},
    ) {
        Image(
            bitmap = foto.pratinjau,
            contentDescription = "Pratinjau foto yang akan dikirim",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().padding(bottom = 96.dp, top = 56.dp),
        )

        Box(
            Modifier
                .statusBarsPadding()
                .padding(6.dp)
                .size(38.dp)
                .clip(CircleShape)
                .background(Color(0x66000000))
                .clickable(onClick = onBatal),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Close, "Batalkan kirim foto", tint = Color.White, modifier = Modifier.size(21.dp))
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding(),
        ) {
            namaBalasan?.let {
                Text(
                    "Membalas $it",
                    fontSize = 12.sp,
                    color = Color(0xCCFFFFFF),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 20.dp, bottom = 6.dp),
                )
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xE6202024))
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                ) {
                    if (keterangan.isEmpty()) {
                        Text("Tambahkan keterangan…", fontSize = 15.sp, color = AbuTeks)
                    }
                    BasicTextField(
                        value = keterangan,
                        onValueChange = { keterangan = it },
                        textStyle = TextStyle(fontSize = 15.sp, color = Color.White),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(BiruIosLampiran),
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(BiruIosLampiran)
                        .clickable { onKirim(keterangan) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Send, "Kirim foto", tint = Color.White, modifier = Modifier.size(21.dp))
                }
            }
        }
    }
}
