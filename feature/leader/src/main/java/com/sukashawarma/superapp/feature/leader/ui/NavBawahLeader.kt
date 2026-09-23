package com.sukashawarma.superapp.feature.leader.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.sukashawarma.superapp.core.ui.kaca.BilahTabKaca
import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.core.ui.kaca.ItemTabKaca
import com.sukashawarma.superapp.core.ui.kaca.LatarKaca

/**
 * Tujuan navigasi modul Leader.
 *
 * Empat, persis seperti isi kelompok "Leader Dashboard" di `navConfig.ts` web —
 * dan karena tepat empat, seluruhnya muat di bilah bawah tanpa perlu lembar "Menu"
 * seperti modul Manager.
 *
 * `rute` disimpan di enum ini, bukan di daftar terpisah, supaya bilah navigasi dan
 * graph tidak bisa berselisih tentang nama rute.
 */
enum class TujuanLeader(
    val rute: String,
    val label: String,
    val labelPanjang: String,
    val ikon: ImageVector,
) {
    RINGKASAN("leader_ringkasan", "Overview", "Ringkasan Leader", IkonIos.Dashboard),
    PETTY_CASH("leader_petty_cash", "Petty Cash", "Top Up Petty Cash", IkonIos.Payments),
    PENJUALAN("leader_penjualan", "Penjualan", "Penjualan & Target", IkonIos.TrackChanges),
    STOK("leader_stok", "Stok", "Stok Cabang", IkonIos.Inventory2),
}

/**
 * Bilah tab kaca modul Leader, dipasang sebagai `bilah` [com.sukashawarma.superapp.core.ui.kaca.ShellKaca].
 *
 * [jumlahAksi] adalah pengajuan yang menunggu diserahkan ke crew. Lencananya ada di
 * sini karena itu satu-satunya hal di modul ini yang menuntut tindakan leader dan
 * bisa datang kapan saja dari meja area manager — sisanya hanya pemantauan.
 */
@Composable
fun NavBawahLeader(
    aktif: TujuanLeader,
    jumlahAksi: Int,
    latar: LatarKaca,
    onPilih: (TujuanLeader) -> Unit,
) {
    val tujuan = TujuanLeader.entries
    BilahTabKaca(
        item = tujuan.map {
            ItemTabKaca(it.label, it.ikon, if (it == TujuanLeader.PETTY_CASH) jumlahAksi else 0)
        },
        indeksAktif = tujuan.indexOf(aktif),
        latar = latar,
        onPilih = { onPilih(tujuan[it]) },
    )
}
