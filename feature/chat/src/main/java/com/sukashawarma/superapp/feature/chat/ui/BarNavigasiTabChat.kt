package com.sukashawarma.superapp.feature.chat.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sukashawarma.superapp.feature.chat.ui.pribadi.TabChatUtama

@Composable
fun BarNavigasiTabChat(
    tabAktif: TabChatUtama,
    unreadPribadi: Int,
    isDeveloper: Boolean,
    onPilihTab: (TabChatUtama) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali ke Beranda",
                    tint = Color(0xFF007AFF)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // iOS Segmented Control Bar
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFE5E5EA))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Tab 1: Grup Tim
                PillTab(
                    judul = "Grup Tim",
                    aktif = tabAktif == TabChatUtama.GRUP,
                    onClick = { onPilihTab(TabChatUtama.GRUP) },
                    modifier = Modifier.weight(1f)
                )

                // Tab 2: Chat Pribadi
                PillTab(
                    judul = "Pribadi",
                    badge = if (unreadPribadi > 0) unreadPribadi.toString() else null,
                    aktif = tabAktif == TabChatUtama.PRIBADI,
                    onClick = { onPilihTab(TabChatUtama.PRIBADI) },
                    modifier = Modifier.weight(1f)
                )

                // Tab 3: Pantau Developer (Hanya tampil jika role developer)
                if (isDeveloper) {
                    PillTab(
                        judul = "🕵️ Pantau",
                        aktif = tabAktif == TabChatUtama.PANTAU_DEV,
                        badge = "DEV",
                        warnaBadge = Color(0xFFFF9500),
                        onClick = { onPilihTab(TabChatUtama.PANTAU_DEV) },
                        modifier = Modifier.weight(1.1f)
                    )
                }
            }
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
    warnaBadge: Color = Color(0xFF34C759),
) {
    val bgColor by animateColorAsState(
        targetValue = if (aktif) Color.White else Color.Transparent,
        label = "tabBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (aktif) Color(0xFF1C1C1E) else Color(0xFF8E8E93),
        label = "tabText"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = judul,
            fontSize = 13.sp,
            fontWeight = if (aktif) FontWeight.Bold else FontWeight.Medium,
            color = textColor
        )

        if (badge != null) {
            Spacer(modifier = Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(warnaBadge)
                    .padding(horizontal = 5.dp, vertical = 1.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badge,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
