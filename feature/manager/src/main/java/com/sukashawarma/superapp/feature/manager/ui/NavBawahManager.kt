package com.sukashawarma.superapp.feature.manager.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ContentPasteSearch
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sukashawarma.superapp.presentation.theme.SukaBrown
import com.sukashawarma.superapp.presentation.theme.SukaGray400
import com.sukashawarma.superapp.presentation.theme.SukaOrange

/**
 * Tujuan navigasi modul Manager.
 *
 * `rute` sengaja disimpan di sini, bukan di enum navigasi terpisah, supaya daftar
 * menu dan graph tidak bisa berselisih tentang nama rute.
 */
enum class TujuanManager(
    val rute: String,
    val label: String,
    val ikon: ImageVector,
) {
    OVERVIEW("manager_overview", "Overview", Icons.Default.Dashboard),
    LAPORAN("manager_laporan", "Laporan", Icons.Default.BarChart),
    SIDAK("manager_sidak", "Sidak Inventaris", Icons.Default.ContentPasteSearch),
    INVENTORI("manager_inventori", "Inventori", Icons.Default.Inventory2),
    RESEP("manager_resep", "Resep & HPP", Icons.Default.MenuBook),
    PERSETUJUAN("manager_persetujuan", "Persetujuan", Icons.Default.FactCheck),
    WASTE("manager_waste", "Waste Stok", Icons.Default.Delete),
    TIM("manager_tim", "Tim / Kru", Icons.Default.Groups),
    PETTY_CASH("manager_petty_cash", "Petty Cash", Icons.Default.Receipt),
}

/**
 * Empat tujuan yang muncul langsung di bilah bawah — cermin `PRIMARY_NAV_ITEMS`
 * di `ManagerLayout.tsx` web. Sisanya dijangkau lewat tombol "Menu".
 */
val TUJUAN_UTAMA = listOf(
    TujuanManager.OVERVIEW,
    TujuanManager.LAPORAN,
    TujuanManager.PERSETUJUAN,
    TujuanManager.PETTY_CASH,
)

/** Kelompok pada lembar menu, urut seperti `NAV_GROUPS` web. */
val KELOMPOK_MENU_MANAGER = listOf(
    "Menu Utama" to listOf(
        TujuanManager.OVERVIEW,
        TujuanManager.LAPORAN,
        TujuanManager.SIDAK,
        TujuanManager.INVENTORI,
    ),
    "Manajemen" to listOf(
        TujuanManager.RESEP,
        TujuanManager.PERSETUJUAN,
        TujuanManager.WASTE,
        TujuanManager.TIM,
        TujuanManager.PETTY_CASH,
    ),
)

/**
 * Bilah navigasi bawah, cermin nav bawah mobile `ManagerLayout.tsx`.
 *
 * Tombol aktif memakai lingkaran jingga penuh dengan label di bawahnya; tombol
 * kelima membuka lembar berisi seluruh tujuan. Tombol "Menu" ikut menyala saat
 * layar yang sedang terbuka bukan salah satu dari empat tujuan utama — tanpa itu,
 * membuka Waste Stok membuat seluruh bilah terlihat mati.
 */
@Composable
fun NavBawahManager(
    aktif: TujuanManager,
    jumlahPersetujuan: Int,
    onPilih: (TujuanManager) -> Unit,
    onBukaMenu: () -> Unit,
) {
    val menuMenyala = aktif !in TUJUAN_UTAMA
    Surface(
        color = Color.White,
        shadowElevation = 12.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top,
        ) {
            TUJUAN_UTAMA.forEach { tujuan ->
                TombolNav(
                    label = tujuan.label,
                    ikon = tujuan.ikon,
                    aktif = aktif == tujuan,
                    lencana = if (tujuan == TujuanManager.PERSETUJUAN) jumlahPersetujuan else 0,
                    modifier = Modifier.weight(1f),
                ) { onPilih(tujuan) }
            }
            TombolNav(
                label = "Menu",
                ikon = Icons.Default.Menu,
                aktif = menuMenyala,
                lencana = 0,
                modifier = Modifier.weight(1f),
                onKlik = onBukaMenu,
            )
        }
    }
}

@Composable
private fun TombolNav(
    label: String,
    ikon: ImageVector,
    aktif: Boolean,
    lencana: Int,
    modifier: Modifier = Modifier,
    onKlik: () -> Unit,
) {
    Column(
        modifier.clickable(onClick = onKlik).padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Box(
                Modifier
                    .size(38.dp)
                    .background(if (aktif) SukaOrange else Color.Transparent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    ikon,
                    null,
                    tint = if (aktif) Color.White else SukaGray400,
                    modifier = Modifier.size(19.dp),
                )
            }
            if (lencana > 0) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFDC2626),
                    border = BorderStroke(2.dp, Color.White),
                ) {
                    Text(
                        if (lencana > 9) "9+" else lencana.toString(),
                        Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            color = if (aktif) SukaOrange else SukaGray400,
            fontSize = 9.sp,
            fontWeight = if (aktif) FontWeight.Black else FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Isi lembar menu bawah — seluruh tujuan, dikelompokkan seperti sidebar web.
 *
 * [tujuanTerlihat] disaring pemanggil menurut role, jadi area manager tidak
 * melihat pintu ke Resep & HPP sama sekali.
 */
@Composable
fun IsiMenuManager(
    aktif: TujuanManager,
    tujuanTerlihat: Set<TujuanManager>,
    jumlahPersetujuan: Int,
    jumlahWaste: Int,
    onPilih: (TujuanManager) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        // Kelompok kosong disaring SEBELUM perulangan, bukan lewat `return@forEach`
        // di dalamnya: keluar-awal dari lambda inline yang memancarkan composable
        // adalah sumber kerusakan tabel slot yang sama seperti `return@Column`.
        KELOMPOK_MENU_MANAGER
            .map { (judul, isi) -> judul to isi.filter { it in tujuanTerlihat } }
            .filter { (_, terlihat) -> terlihat.isNotEmpty() }
            .forEach { (judul, terlihat) ->
            Text(
                judul.uppercase(),
                color = SukaGray400,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.9.sp,
            )
            Spacer(Modifier.height(10.dp))
            terlihat.forEach { tujuan ->
                BarisMenu(
                    tujuan = tujuan,
                    aktif = aktif == tujuan,
                    lencana = when (tujuan) {
                        TujuanManager.PERSETUJUAN -> jumlahPersetujuan
                        TujuanManager.WASTE -> jumlahWaste
                        else -> 0
                    },
                ) { onPilih(tujuan) }
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun BarisMenu(
    tujuan: TujuanManager,
    aktif: Boolean,
    lencana: Int,
    onKlik: () -> Unit,
) {
    Surface(
        Modifier.fillMaxWidth().clickable(onClick = onKlik),
        shape = RoundedCornerShape(16.dp),
        color = if (aktif) SukaOrange else Color.White,
        border = BorderStroke(1.dp, if (aktif) SukaOrange else GarisKartu),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                tujuan.ikon,
                null,
                tint = if (aktif) Color.White else SukaOrange,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                tujuan.label,
                Modifier.weight(1f),
                color = if (aktif) Color.White else SukaBrown,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (lencana > 0) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (aktif) Color.White else Color(0xFFDC2626),
                ) {
                    Text(
                        if (lencana > 9) "9+" else lencana.toString(),
                        Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        color = if (aktif) SukaOrange else Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
    }
}
