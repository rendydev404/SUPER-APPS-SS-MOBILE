package com.sukashawarma.superapp.feature.stok.ui.area

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Palet dan potongan tampilan bersama untuk modul pengelola area. Dipisah supaya
 * layar harga dan layar waste tidak menyimpan dua salinan warna yang perlahan
 * berbeda — itu sudah terjadi pada modul lain sebelumnya.
 */
internal val Slate50 = Color(0xFFF8FAFC)
internal val Slate100 = Color(0xFFF1F5F9)
internal val Slate200 = Color(0xFFE2E8F0)
internal val Slate400 = Color(0xFF94A3B8)
internal val Slate500 = Color(0xFF64748B)
internal val Slate700 = Color(0xFF334155)
internal val Slate900 = Color(0xFF0F172A)
internal val Orange50 = Color(0xFFFFF7ED)
internal val Orange200 = Color(0xFFFED7AA)
internal val Orange500 = Color(0xFFF97316)
internal val Orange600 = Color(0xFFEA580C)
internal val Rose50 = Color(0xFFFFF1F2)
internal val Rose100 = Color(0xFFFFE4E6)
internal val Rose200 = Color(0xFFFECDD3)
internal val Rose500 = Color(0xFFF43F5E)
internal val Rose600 = Color(0xFFE11D48)
internal val Rose700 = Color(0xFFBE123C)
internal val Emerald50 = Color(0xFFECFDF5)
internal val Emerald100 = Color(0xFFD1FAE5)
internal val Emerald200 = Color(0xFFA7F3D0)
internal val Emerald500 = Color(0xFF10B981)
internal val Emerald600 = Color(0xFF059669)
internal val Emerald700 = Color(0xFF047857)
internal val Amber50 = Color(0xFFFFFBEB)
internal val Amber100 = Color(0xFFFEF3C7)
internal val Amber300 = Color(0xFFFCD34D)
internal val Amber500 = Color(0xFFF59E0B)
internal val Amber800 = Color(0xFF92400E)

@Composable
internal fun MiniBadge(text: String, background: Color, color: Color, border: Color? = null) {
    Box(
        Modifier
            .background(background, RoundedCornerShape(6.dp))
            .then(if (border != null) Modifier.border(1.dp, border, RoundedCornerShape(6.dp)) else Modifier)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(text, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

/** Lencana status berpita: titik berwarna + teks, dipakai di kepala setiap kartu. */
@Composable
internal fun StatusChip(label: String, chip: Color, border: Color, text: Color, dot: Color) {
    Surface(shape = RoundedCornerShape(50), color = chip, border = BorderStroke(1.dp, border)) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).background(dot, CircleShape))
            Spacer(Modifier.width(4.dp))
            Text(label, color = text, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun RowScope.KpiCell(title: String, value: String, caption: String, titleColor: Color, valueColor: Color, captionColor: Color) {
    Column(Modifier.weight(1f).padding(horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = titleColor, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        Text(value, color = valueColor, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        Text(caption, color = captionColor, fontSize = 9.sp, textAlign = TextAlign.Center, lineHeight = 11.sp, maxLines = 2)
    }
}

@Composable
internal fun KpiDivider() = Box(Modifier.height(38.dp).width(1.dp).background(Slate100))

/** Baris label kiri / nilai kanan di dalam kotak meta abu-abu. */
@Composable
internal fun MetaRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Slate400, fontSize = 10.sp)
        Text(
            value, color = Slate700, fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
            maxLines = 2, textAlign = TextAlign.End, modifier = Modifier.padding(start = 12.dp),
        )
    }
}
