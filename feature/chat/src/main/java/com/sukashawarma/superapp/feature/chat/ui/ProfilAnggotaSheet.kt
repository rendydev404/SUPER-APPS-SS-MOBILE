package com.sukashawarma.superapp.feature.chat.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.sukashawarma.superapp.core.ui.AvatarStorage
import com.sukashawarma.superapp.core.ui.AvatarStaf
import com.sukashawarma.superapp.feature.chat.data.AnggotaGrup
import com.sukashawarma.superapp.feature.chat.data.labelRole

/**
 * Kartu profil satu anggota.
 *
 * Susunannya iOS — lembar membulat, foto besar di tengah, lalu kartu "inset
 * grouped" berisi baris keterangan — sementara warna aksennya milik kita
 * (oranye merek), bukan biru sistem. Biru tetap dipakai untuk hal yang bisa
 * ditekan; oranye untuk penanda identitas (cincin foto, lencana jabatan),
 * sehingga keduanya tidak berebut arti.
 *
 * Isinya HANYA yang dibuka `chat_daftar_anggota`. Tidak ada nomor telepon,
 * username login, atau data kepegawaian di sini karena server memang tidak
 * pernah mengirimkannya.
 */
private val OranyeMerek = Color(0xFFEA580C)
private val OranyeLembut = Color(0xFFFFF1E7)
private val AbuProfil = Color(0xFF8E8E93)
private val LatarProfil = Color(0xFFF2F2F7)
private val PemisahProfil = Color(0x1F3C3C43)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilAnggotaSheet(
    anggota: AnggotaGrup?,
    /** Nama cadangan bila orangnya tidak ada di daftar (mis. sudah non-aktif),
     *  diambil dari snapshot yang menempel di pesan. */
    namaCadangan: String,
    avatarCadangan: String?,
    memuat: Boolean,
    akuSendiri: Boolean,
    onTutup: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val nama = anggota?.nama ?: namaCadangan
    val avatar = anggota?.avatar ?: avatarCadangan
    var lihatFoto by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onTutup,
        sheetState = sheetState,
        containerColor = LatarProfil,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Cincin gradasi oranye: penanda identitas, bukan tombol.
            Box(
                Modifier
                    .size(104.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(OranyeMerek, Color(0xFFF59E0B)))
                    )
                    // Hanya bisa dibuka bila memang ada fotonya; membuka layar
                    // hitam berisi huruf awal nama bukan sesuatu yang berguna.
                    .then(
                        if (!avatar.isNullOrBlank()) Modifier.clickable { lihatFoto = true }
                        else Modifier
                    )
                    .padding(3.dp),
            ) {
                Box(Modifier.fillMaxWidth().clip(CircleShape).background(Color.White).padding(2.dp)) {
                    AvatarStaf(
                        path = avatar,
                        nama = nama,
                        modifier = Modifier.size(94.dp),
                        ukuranHuruf = 34.sp,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(
                nama,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                textAlign = TextAlign.Center,
            )
            if (akuSendiri) {
                Spacer(Modifier.height(4.dp))
                Text("Ini Anda", fontSize = 12.5.sp, color = AbuProfil)
            }

            Spacer(Modifier.height(20.dp))

            when {
                memuat && anggota == null -> {
                    CircularProgressIndicator(
                        color = OranyeMerek,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(26.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Memuat keterangan…", fontSize = 12.5.sp, color = AbuProfil)
                }

                anggota == null -> Text(
                    "Keterangan lengkapnya tidak tersedia. Orang ini mungkin sudah tidak aktif.",
                    fontSize = 12.5.sp,
                    color = AbuProfil,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                else -> Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White),
                ) {
                    BarisProfil(Icons.Filled.Badge, "Jabatan", labelRole(anggota.role))
                    anggota.outlet?.takeIf { it.isNotBlank() }?.let {
                        PemisahBaris()
                        BarisProfil(Icons.Filled.Storefront, "Outlet", it)
                    }
                    anggota.username?.takeIf { it.isNotBlank() }?.let {
                        PemisahBaris()
                        BarisProfil(Icons.Filled.AlternateEmail, "Username", "@$it")
                    }
                }
            }
        }
    }

    // Digambar sebagai Dialog, bukan lapisan biasa: ModalBottomSheet hidup di
    // jendela terpisah di atas isi Activity, jadi lapisan yang digambar di
    // bawahnya justru akan tertutup oleh lembar ini sendiri.
    if (lihatFoto && !avatar.isNullOrBlank()) {
        PenampilFotoProfil(path = avatar, nama = nama, onTutup = { lihatFoto = false })
    }
}

/**
 * Foto profil layar penuh, seperti membuka foto kontak di WhatsApp: latar gelap,
 * foto sebesar mungkin, bisa dicubit untuk diperbesar dan digeser.
 *
 * Skala dibatasi 1x-4x dan geseran dikunci kembali ke tengah begitu skalanya
 * balik ke 1x. Tanpa itu foto bisa terdorong keluar layar dan pengguna tidak
 * punya cara mengembalikannya.
 */
@Composable
private fun PenampilFotoProfil(path: String, nama: String, onTutup: () -> Unit) {
    BackHandler(onBack = onTutup)
    Dialog(
        onDismissRequest = onTutup,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        var skala by remember { mutableFloatStateOf(1f) }
        var geser by remember { mutableStateOf(Offset.Zero) }
        val transform = rememberTransformableState { ubahSkala, ubahGeser, _ ->
            skala = (skala * ubahSkala).coerceIn(1f, 4f)
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
                model = AvatarStorage.url(path),
                imageLoader = AvatarStorage.imageLoader(LocalContext.current),
                contentDescription = "Foto profil " + nama,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 72.dp)
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
                Text(
                    nama,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun BarisProfil(ikon: ImageVector, label: String, nilai: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(OranyeLembut),
            contentAlignment = Alignment.Center,
        ) {
            Icon(ikon, null, tint = OranyeMerek, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 14.5.sp, color = Color.Black)
        Spacer(Modifier.width(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text(nilai, fontSize = 14.5.sp, color = AbuProfil, textAlign = TextAlign.End)
        }
    }
}

@Composable
private fun PemisahBaris() {
    Box(Modifier.fillMaxWidth().padding(start = 56.dp).height(0.5.dp).background(PemisahProfil))
}
