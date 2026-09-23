package com.sukashawarma.superapp.feature.chat.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.TombolBundarIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.feature.chat.ui.pribadi.TabChatUtama

/**
 * Bar navigasi tab chat modern dengan standar iOS / Telegram:
 * - Baris 1: Header App Bar (Tombol Kembali 48dp, Judul Layar, Subjudul konteks aktif)
 * - Baris 2: Segmented Control Pill Bar penuh layar yang proporsional tanpa teks terpotong
 * - Hairline divider pemisah di bagian dasar
 */
@Composable
fun BarNavigasiTabChat(
    tabAktif: TabChatUtama,
    unreadPribadi: Int,
    isDeveloper: Boolean,
    onPilihTab: (TabChatUtama) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    tampilkanArea: Boolean = true,
) {
    Surface(
        color = WarnaIos.Kartu,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            // Baris 1: Top Navigation Bar dengan Back Button & Context Title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TombolBundarIos(IkonIos.ArrowBack, "Kembali ke Beranda", onBack)

                Spacer(modifier = Modifier.width(10.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Ruang Obrolan",
                        style = TipeIos.Utama,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = when (tabAktif) {
                            TabChatUtama.GRUP -> "Obrolan Tim Suka Shawarma"
                            TabChatUtama.AREA -> "Koordinasi Area Manager & Kru"
                            TabChatUtama.PRIBADI -> "Percakapan Langsung 1-on-1"
                            TabChatUtama.PANTAU_DEV -> "Mode Audit Pengawasan Sistem"
                        },
                        style = TipeIos.Kecil,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Baris 2: Full-Width Modern Segmented Control Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 8.dp, top = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(WarnaIos.Isian)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tab 1: Grup Tim (Global)
                    PillTab(
                        judul = "🏢 Tim",
                        aktif = tabAktif == TabChatUtama.GRUP,
                        onClick = { onPilihTab(TabChatUtama.GRUP) },
                        modifier = Modifier.weight(1f)
                    )

                    // Tab 2: Grup Area (Area Manager & Crew) — disembunyikan di rilis produksi
                    if (tampilkanArea) {
                        PillTab(
                            judul = "📍 Area",
                            aktif = tabAktif == TabChatUtama.AREA,
                            badge = "NEW",
                            warnaBadge = WarnaIos.Biru,
                            onClick = { onPilihTab(TabChatUtama.AREA) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Tab 3: Chat Pribadi (1-on-1)
                    PillTab(
                        judul = "💬 Pribadi",
                        badge = if (unreadPribadi > 0) unreadPribadi.toString() else null,
                        warnaBadge = WarnaIos.Hijau,
                        aktif = tabAktif == TabChatUtama.PRIBADI,
                        onClick = { onPilihTab(TabChatUtama.PRIBADI) },
                        modifier = Modifier.weight(1f)
                    )

                    // Tab 4: Pantau Developer (Hanya tampil jika role developer)
                    if (isDeveloper) {
                        PillTab(
                            judul = "🕵️ Pantau",
                            aktif = tabAktif == TabChatUtama.PANTAU_DEV,
                            badge = "DEV",
                            warnaBadge = WarnaIos.Oranye,
                            onClick = { onPilihTab(TabChatUtama.PANTAU_DEV) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Garis tipis pemisah bawah
            HorizontalDivider(
                thickness = 0.5.dp,
                color = WarnaIos.Pemisah
            )
        }
    }
}

@Composable
private fun PillTab(
    judul: String,
    aktif: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null,
    warnaBadge: Color = WarnaIos.Hijau,
) {
    val bgColor by animateColorAsState(
        targetValue = if (aktif) WarnaIos.Kartu else Color.Transparent,
        animationSpec = tween(150),
        label = "tabBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (aktif) WarnaIos.Label else WarnaIos.LabelKedua,
        animationSpec = tween(150),
        label = "tabText"
    )

    val shape = RoundedCornerShape(9.dp)

    Box(
        modifier = modifier
            .then(
                if (aktif) Modifier.shadow(1.dp, shape)
                else Modifier
            )
            .clip(shape)
            .background(bgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 7.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = judul,
                fontSize = 13.sp,
                fontWeight = if (aktif) FontWeight.SemiBold else FontWeight.Medium,
                letterSpacing = (-0.1).sp,
                color = textColor,
                maxLines = 1,
                softWrap = false
            )

            if (badge != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(warnaBadge)
                        .padding(horizontal = 4.5.dp, vertical = 1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badge,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}
