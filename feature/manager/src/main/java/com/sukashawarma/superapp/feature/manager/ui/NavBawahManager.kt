package com.sukashawarma.superapp.feature.manager.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.sukashawarma.superapp.core.ui.kaca.BarisMenuKaca
import com.sukashawarma.superapp.core.ui.kaca.BilahTabKaca
import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.core.ui.kaca.ItemTabKaca
import com.sukashawarma.superapp.core.ui.kaca.JudulKelompokMenuKaca
import com.sukashawarma.superapp.core.ui.kaca.LatarKaca

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
    /** Label pendek untuk bilah bawah bila [label] terlalu panjang untuk satu slot. */
    val labelTab: String = label,
) {
    OVERVIEW("manager_overview", "Overview", IkonIos.Dashboard),
    CEKLIST("manager_ceklist", "Ceklist Harian", IkonIos.Checklist, labelTab = "Ceklist"),
    LAPORAN("manager_laporan", "Laporan", IkonIos.BarChart),
    SIDAK("manager_sidak", "Sidak Inventaris", IkonIos.ContentPasteSearch),
    INVENTORI("manager_inventori", "Inventori", IkonIos.Inventory2),
    RESEP("manager_resep", "Resep & HPP", IkonIos.MenuBook),
    PERSETUJUAN("manager_persetujuan", "Persetujuan", IkonIos.FactCheck),
    WASTE("manager_waste", "Waste Stok", IkonIos.Delete),
    TIM("manager_tim", "Tim / Kru", IkonIos.Groups),
    PETTY_CASH("manager_petty_cash", "Petty Cash", IkonIos.Receipt),
}

/**
 * Empat tujuan yang muncul langsung di bilah bawah. Sisanya dijangkau lewat
 * tombol "Menu".
 *
 * Berbeda dari `PRIMARY_NAV_ITEMS` web, sengaja: atas permintaan pemilik produk
 * (24 Sep 2026) Ceklist Harian menggantikan Laporan di sini karena dipakai AM
 * setiap hari dan dipantau RM setiap hari. Laporan tetap ada di lembar Menu.
 */
val TUJUAN_UTAMA = listOf(
    TujuanManager.OVERVIEW,
    TujuanManager.CEKLIST,
    TujuanManager.PERSETUJUAN,
    TujuanManager.PETTY_CASH,
)

/** Kelompok pada lembar menu, urut seperti `NAV_GROUPS` web. */
val KELOMPOK_MENU_MANAGER = listOf(
    "Menu Utama" to listOf(
        TujuanManager.OVERVIEW,
        TujuanManager.CEKLIST,
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
 * Bilah navigasi bawah, cermin nav bawah mobile `ManagerLayout.tsx`, dalam wujud
 * tab bar kaca bersama (core.ui.kaca) supaya sama dengan modul lain.
 *
 * Slot kelima membuka lembar berisi tujuan lainnya. Slot "Menu" ikut menyala saat
 * layar yang sedang terbuka bukan salah satu dari empat tujuan utama — tanpa itu,
 * membuka Waste Stok membuat seluruh bilah terlihat mati.
 */
@Composable
fun NavBawahManager(
    aktif: TujuanManager,
    jumlahPersetujuan: Int,
    jumlahCeklist: Int,
    latar: LatarKaca?,
    onPilih: (TujuanManager) -> Unit,
    onBukaMenu: () -> Unit,
) {
    val item = TUJUAN_UTAMA.map { tujuan ->
        ItemTabKaca(
            tujuan.labelTab,
            tujuan.ikon,
            when (tujuan) {
                TujuanManager.PERSETUJUAN -> jumlahPersetujuan
                TujuanManager.CEKLIST -> jumlahCeklist
                else -> 0
            },
        )
    } + ItemTabKaca("Menu", IkonIos.Menu)
    val indeks = TUJUAN_UTAMA.indexOf(aktif).let { if (it < 0) TUJUAN_UTAMA.size else it }

    BilahTabKaca(item, indeks, latar, onPilih = { i ->
        if (i < TUJUAN_UTAMA.size) onPilih(TUJUAN_UTAMA[i]) else onBukaMenu()
    })
}

/**
 * Isi lembar menu bawah — tujuan yang TIDAK ada di bilah bawah, dikelompokkan seperti sidebar web.
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
    jumlahCeklist: Int,
    onPilih: (TujuanManager) -> Unit,
) {
    // Kelompok kosong disaring SEBELUM perulangan, bukan lewat `return@forEach`
    // di dalamnya: keluar-awal dari lambda inline yang memancarkan composable
    // adalah sumber kerusakan tabel slot yang sama seperti `return@Column`.
    // Tujuan yang sudah ada di bilah bawah tidak diulang di sini — permintaan
    // pemilik produk (24 Sep 2026) supaya lembar Menu tidak dobel dan pendek.
    KELOMPOK_MENU_MANAGER
        .map { (judul, isi) -> judul to isi.filter { it in tujuanTerlihat && it !in TUJUAN_UTAMA } }
        .filter { (_, terlihat) -> terlihat.isNotEmpty() }
        .forEach { (judul, terlihat) ->
            JudulKelompokMenuKaca(judul)
            terlihat.forEach { tujuan ->
                BarisMenuKaca(
                    ikon = tujuan.ikon,
                    label = tujuan.label,
                    aktif = aktif == tujuan,
                    lencana = when (tujuan) {
                        TujuanManager.PERSETUJUAN -> jumlahPersetujuan
                        TujuanManager.WASTE -> jumlahWaste
                        TujuanManager.CEKLIST -> jumlahCeklist
                        else -> 0
                    },
                    onKlik = { onPilih(tujuan) },
                )
            }
            Spacer(Modifier.height(14.dp))
        }
}
