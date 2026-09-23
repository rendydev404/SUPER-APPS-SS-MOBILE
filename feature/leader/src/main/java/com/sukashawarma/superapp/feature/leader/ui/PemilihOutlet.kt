package com.sukashawarma.superapp.feature.leader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import com.sukashawarma.superapp.core.ui.SukaDropdownHeader
import com.sukashawarma.superapp.core.ui.SukaDropdownMenu
import com.sukashawarma.superapp.core.ui.SukaDropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sukashawarma.superapp.core.ui.ios.KartuIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.feature.leader.data.OutletLeader

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

    KartuIos(onKlik = { terbuka = true }, padding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(34.dp).clip(CircleShape).background(WarnaIos.Aksen.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(IkonIos.Storefront, null, tint = WarnaIos.Aksen, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Pilih cabang", style = TipeIos.Catatan)
                Text(nama, style = TipeIos.Utama, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(IkonIos.ArrowDropDown, "Ganti cabang", tint = WarnaIos.Aksen, modifier = Modifier.size(18.dp))
        }
        Box {
            SukaDropdownMenu(expanded = terbuka, onDismissRequest = { terbuka = false }) {
                SukaDropdownHeader(title = "PILIH OUTLET", onClose = { terbuka = false })
                daftar.forEach { outlet ->
                    SukaDropdownMenuItem(
                        text = outlet.nama,
                        selected = (outlet.id == terpilih),
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
