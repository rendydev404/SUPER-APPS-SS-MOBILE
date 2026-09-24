package com.sukashawarma.superapp.feature.distribusi.ui

import androidx.compose.runtime.Composable
import com.sukashawarma.superapp.core.ui.kaca.BilahTabKaca
import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.core.ui.kaca.ItemTabKaca
import com.sukashawarma.superapp.core.ui.kaca.ShellKaca

enum class TabBawah { DASHBOARD, SCAN, BUAT, RIWAYAT }

/**
 * Kerangka tiga layar ber-tab modul ini: isi layar dengan tab bar kaca di bawahnya,
 * cermin `BottomNav.tsx` varian outlet di web.
 *
 * Tiap tab tetap rute sendiri di NavHost, jadi setiap layar memasang shell ini
 * masing-masing, bukan satu shell di atas NavHost.
 *
 * Tab tengah hanya muncul untuk role yang berhak memverifikasi. Pengawas
 * membuka modul ini untuk memantau, bukan menerima barang, jadi menampilkan
 * pintu pindai kepada mereka hanya akan menyesatkan. Kitchen (pengirim) justru
 * mendapat "Buat SJ" di tengah — cermin varian `isPusat` `BottomNav.tsx`.
 */
@Composable
fun ShellDistribusi(
    aktif: TabBawah,
    bolehVerifikasi: Boolean,
    onDashboard: () -> Unit,
    onScan: () -> Unit,
    onRiwayat: () -> Unit,
    bolehTerbitkan: Boolean = false,
    onBuat: () -> Unit = {},
    isi: @Composable () -> Unit,
) {
    val tab = buildList {
        add(TabBawah.DASHBOARD)
        if (bolehVerifikasi) add(TabBawah.SCAN)
        if (bolehTerbitkan) add(TabBawah.BUAT)
        add(TabBawah.RIWAYAT)
    }
    val item = tab.map {
        when (it) {
            TabBawah.DASHBOARD -> ItemTabKaca("Dashboard", IkonIos.Dashboard)
            TabBawah.SCAN -> ItemTabKaca("Scan QR", IkonIos.QrCodeScanner)
            TabBawah.BUAT -> ItemTabKaca("Buat SJ", IkonIos.Add)
            TabBawah.RIWAYAT -> ItemTabKaca("Riwayat", IkonIos.History)
        }
    }
    ShellKaca(
        bilah = { latar ->
            // Tab aktif yang disembunyikan (inbox dibuka tanpa hak verifikasi)
            // jatuh ke -1: tidak ada tab yang menyala, bukan tab yang salah.
            BilahTabKaca(item, tab.indexOf(aktif), latar, onPilih = { i ->
                when (tab[i]) {
                    TabBawah.DASHBOARD -> onDashboard()
                    TabBawah.SCAN -> onScan()
                    TabBawah.BUAT -> onBuat()
                    TabBawah.RIWAYAT -> onRiwayat()
                }
            })
        },
        isi = isi,
    )
}
