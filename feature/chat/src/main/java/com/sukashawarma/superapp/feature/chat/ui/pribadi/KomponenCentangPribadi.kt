package com.sukashawarma.superapp.feature.chat.ui.pribadi

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sukashawarma.superapp.feature.chat.data.StatusCentangPribadi

private val WARNA_CENTANG_ABU = Color(0xFF8E8E93)
/** Aksen biru yang sama dengan seluruh layar chat, bukan biru WhatsApp:
 *  gelembungnya kini abu, jadi centangnya ikut memakai satu aksen. */
private val WARNA_CENTANG_BIRU = Color(0xFF007AFF)

/**
 * Indikator centang status pesan pribadi ala WhatsApp iOS.
 */
@Composable
fun KomponenCentangPribadi(
    status: StatusCentangPribadi,
    modifier: Modifier = Modifier,
    warnaAbu: Color = WARNA_CENTANG_ABU,
) {
    when (status) {
        StatusCentangPribadi.MENGIRIM -> {
            Icon(
                imageVector = Icons.Outlined.Schedule,
                contentDescription = "Sedang mengirim",
                tint = warnaAbu,
                modifier = modifier.size(11.dp)
            )
        }
        StatusCentangPribadi.TERKIRIM -> {
            // Centang satu abu-abu
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Terkirim",
                tint = warnaAbu,
                modifier = modifier.size(13.dp)
            )
        }
        StatusCentangPribadi.TERSAMPAIKAN -> {
            // Centang dua abu-abu
            CentangGanda(warna = warnaAbu, modifier = modifier)
        }
        StatusCentangPribadi.DIBACA -> {
            // Centang dua biru
            CentangGanda(warna = WARNA_CENTANG_BIRU, modifier = modifier)
        }
    }
}

@Composable
private fun CentangGanda(warna: Color, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.CenterStart) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = warna,
            modifier = Modifier.size(13.dp)
        )
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Tersampaikan",
            tint = warna,
            modifier = Modifier
                .offset(x = 4.dp)
                .size(13.dp)
        )
    }
}
