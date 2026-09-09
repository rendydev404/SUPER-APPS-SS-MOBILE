package com.sukashawarma.superapp.feature.leader.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sukashawarma.superapp.feature.leader.data.OutletLeader
import com.sukashawarma.superapp.presentation.theme.SukaBrown
import com.sukashawarma.superapp.presentation.theme.SukaGray400
import com.sukashawarma.superapp.presentation.theme.SukaOrange

/**
 * Pemilih cabang untuk layar Penjualan dan Stok.
 *
 * Tidak digambar sama sekali saat leader hanya membina satu cabang — cermin
 * `outlets.length > 1` di kedua halaman web. Pemilih dengan satu pilihan hanyalah
 * baris yang memakan ruang dan tidak pernah dipakai.
 */
@Composable
fun PemilihOutlet(
    daftar: List<OutletLeader>,
    terpilih: String?,
    onPilih: (String) -> Unit,
) {
    if (daftar.size <= 1) return
    var terbuka by remember { mutableStateOf(false) }
    val nama = daftar.find { it.id == terpilih }?.nama ?: "Pilih cabang"

    KartuPanel {
        Row(
            Modifier.fillMaxWidth().clickable { terbuka = true },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Storefront, null, tint = SukaGray400, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "PILIH CABANG",
                    color = SukaGray400,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.7.sp,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    nama,
                    color = SukaBrown,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(Icons.Default.ArrowDropDown, null, tint = SukaOrange)
        }
        Box {
            DropdownMenu(expanded = terbuka, onDismissRequest = { terbuka = false }) {
                daftar.forEach { outlet ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                outlet.nama,
                                fontSize = 13.sp,
                                fontWeight = if (outlet.id == terpilih) FontWeight.Black else FontWeight.Medium,
                                color = if (outlet.id == terpilih) SukaOrange else SukaBrown,
                            )
                        },
                        onClick = {
                            terbuka = false
                            onPilih(outlet.id)
                        },
                    )
                }
            }
        }
    }
}
