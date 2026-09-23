package com.sukashawarma.superapp.core.ui.kaca

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

/**
 * Satu tombol di [BilahTabKaca].
 *
 * [ikon] sebaiknya dari [IkonIos]: bilah memakai varian terisinya ([IkonIos.aktif])
 * seperti tab bar iOS. [lencana] nol berarti tanpa lencana sama sekali.
 */
@Immutable
data class ItemTabKaca(
    val label: String,
    val ikon: ImageVector,
    val lencana: Int = 0,
)

/**
 * Tab bar "liquid glass" bergaya iOS 26 — kapsul kaca bening yang mengambang, isi di
 * belakangnya dibiaskan seperti lensa, dan tetes kaca di bawah tab aktif meluncur
 * memakai pegas sambil merenggang seperti air yang ditarik.
 *
 * Dipasang sebagai `bilah` [ShellKaca] supaya [latar] tersambung dengan isi layar.
 * Tanpa [latar] (null) bilah tetap tampil, hanya tanpa pembiasan isi.
 *
 * @param indeksAktif tab yang menyala; -1 bila tidak ada (mis. layar turunan).
 */
@Composable
fun BilahTabKaca(
    item: List<ItemTabKaca>,
    indeksAktif: Int,
    latar: LatarKaca?,
    onPilih: (Int) -> Unit,
    modifier: Modifier = Modifier,
    warnaAksen: Color = TokenKaca.Aksen,
) {
    val haptik = LocalHapticFeedback.current
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 14.dp, end = 14.dp, bottom = TokenKaca.JarakKapsulBawah),
    ) {
        val sisi = 5.dp
        val lebarSlot = (maxWidth - sisi * 2) / item.size.coerceAtLeast(1)
        val targetX = sisi + lebarSlot * indeksAktif.coerceAtLeast(0)
        val x by animateDpAsState(
            targetX,
            spring(dampingRatio = 0.62f, stiffness = 420f),
            label = "lensaX",
        )
        // Semakin jauh lensa dari tujuannya, semakin ia merenggang; saat tiba,
        // regangannya habis dan pegas di atas memberinya pantulan kecil.
        val regang = minOf(abs((targetX - x).value) * 0.5f, 26f).dp

        Box(
            Modifier
                .fillMaxWidth()
                .height(TokenKaca.TinggiKapsul)
                .kacaCair(latar),
        ) {
            // Tanpa tab aktif, tetes kaca tidak tergambar — lupakan juga batasnya
            // supaya pembesaran kuatnya tidak tertinggal di posisi lama.
            if (indeksAktif < 0 && latar != null) SideEffect { latar.lensa = Rect.Zero }
            if (indeksAktif >= 0) {
                Box(
                    Modifier
                        .offset(x = x - regang / 2)
                        .padding(horizontal = 2.dp, vertical = 6.dp)
                        .width(lebarSlot + regang - 4.dp)
                        .fillMaxHeight()
                        .lensaKaca(latar),
                )
            }
            Row(Modifier.fillMaxSize().padding(horizontal = sisi)) {
                item.forEachIndexed { i, it ->
                    SlotKaca(
                        ikon = it.ikon,
                        label = it.label,
                        terpilih = i == indeksAktif,
                        jumlah = it.lencana,
                        warnaAksen = warnaAksen,
                    ) {
                        if (i != indeksAktif) haptik.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onPilih(i)
                    }
                }
            }
        }
    }
}

/**
 * Satu slot di kapsul. Tanpa riak Material — iOS tidak memakainya; umpan baliknya
 * adalah ikon yang sedikit mengecil saat ditekan lalu memantul kembali, dan berubah
 * ke warna aksen selama jari masih menempel.
 *
 * Ikon selalu varian terisi dan berwarna gelap: kacanya bening dan layar aplikasi
 * kebanyakan kartu putih, jadi ikon putih ala mockup akan lenyap — alasan yang sama
 * dengan tab bar iOS 26 mode terang.
 */
@Composable
private fun RowScope.SlotKaca(
    ikon: ImageVector,
    label: String,
    terpilih: Boolean,
    jumlah: Int,
    warnaAksen: Color,
    onKlik: () -> Unit,
) {
    val interaksi = remember { MutableInteractionSource() }
    val ditekan by interaksi.collectIsPressedAsState()
    val skala by animateFloatAsState(
        if (ditekan) 0.86f else 1f,
        spring(dampingRatio = 0.5f, stiffness = 600f),
        label = "skalaSlot",
    )
    val warna by animateColorAsState(
        if (terpilih || ditekan) warnaAksen else TokenKaca.Label,
        label = "warnaSlot",
    )
    Column(
        Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(interaksi, indication = null, onClick = onKlik)
            .semantics {
                role = Role.Tab
                selected = terpilih
            }
            .graphicsLayer {
                scaleX = skala
                scaleY = skala
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        IkonBerlencanaKaca(IkonIos.aktif(ikon), label, warna, jumlah)
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            color = warna,
            fontSize = 11.sp,
            fontWeight = if (terpilih) FontWeight.Bold else FontWeight.SemiBold,
            letterSpacing = 0.1.sp,
            // Pendar putih tipis menjaga label gelap tetap terbaca saat yang lewat
            // di balik kaca kebetulan juga gelap.
            style = TextStyle(shadow = Shadow(Color.White.copy(alpha = 0.7f), Offset.Zero, 4f)),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Ikon dengan angka merah di pojok kanan atas.
 *
 * [jumlah] nol berarti TIDAK ada lencana sama sekali, bukan lencana bertuliskan "0":
 * nol adalah kabar baik dan tidak perlu menuntut perhatian.
 */
@Composable
fun IkonBerlencanaKaca(
    ikon: ImageVector,
    label: String?,
    warna: Color,
    jumlah: Int,
    ukuran: androidx.compose.ui.unit.Dp = 25.dp,
) {
    Box {
        Icon(ikon, label, tint = warna, modifier = Modifier.size(ukuran))
        if (jumlah > 0) {
            Surface(
                Modifier
                    .align(Alignment.TopEnd)
                    // Digeser keluar kotak ikon supaya tidak menutupi gambarnya.
                    .offset(x = 12.dp, y = (-6).dp),
                shape = RoundedCornerShape(50),
                color = TokenKaca.MerahLencana,
                border = BorderStroke(1.5.dp, Color.White),
            ) {
                Text(
                    teksLencanaKaca(jumlah),
                    Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
