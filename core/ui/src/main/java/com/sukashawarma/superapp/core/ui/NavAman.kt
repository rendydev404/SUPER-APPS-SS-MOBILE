package com.sukashawarma.superapp.core.ui

import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController

/**
 * Pembungkus navigasi yang tahan tekan berulang.
 *
 * Masalah yang diselesaikan: `popBackStack()` tidak punya rem. Layar yang sedang
 * keluar MASIH tersusun dan MASIH menerima sentuhan selama animasi keluarnya
 * berjalan, jadi menekan tombol kembali dua kali dengan cepat memanggil pop dua
 * kali — yang pertama membuang layar itu sendiri, yang kedua ikut membuang
 * Beranda. Tumpukan jadi kosong, NavHost tidak punya tujuan untuk digambar, dan
 * layarnya putih total padahal Activity masih hidup.
 */

/**
 * Aturan tunggal yang memutuskan apakah satu permintaan "kembali" boleh memotong
 * tumpukan. Dipisah dari [NavController] supaya bisa diuji tanpa Android.
 *
 * [adaTujuanSebelumnya] menutup jalan ke tumpukan kosong; [entriStabil] menutup
 * tekanan kedua yang datang sebelum pop pertama selesai beranimasi.
 */
fun bolehPop(adaTujuanSebelumnya: Boolean, entriStabil: Boolean): Boolean =
    adaTujuanSebelumnya && entriStabil

/**
 * Kembali satu langkah, sekali saja.
 *
 * Entri yang sedang berpindah berada di bawah RESUMED sampai animasinya tuntas,
 * jadi pemeriksaan siklus hidup inilah yang menelan tekanan beruntun. Pemeriksaan
 * [NavController.previousBackStackEntry] adalah lapis kedua: seandainya ada jalur
 * yang lolos dari yang pertama, tumpukan tetap tidak mungkin terkuras habis.
 */
fun NavController.popAman(): Boolean {
    val stabil = currentBackStackEntry
        ?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) == true
    if (!bolehPop(previousBackStackEntry != null, stabil)) return false
    return popBackStack()
}

/**
 * Kembali langsung ke [rute], berapa pun layar yang menumpuk di atasnya.
 *
 * Inilah arti tombol kembali di pojok kiri atas hampir di seluruh aplikasi:
 * "pulang ke daftar aplikasi", bukan "mundur satu langkah" — yang bisa mendarat
 * di Chat kalau modulnya tadi dibuka dari notifikasi.
 *
 * Pengecualiannya hanya layar yang dibuka untuk SATU objek atau sebagai lanjutan
 * satu alur — Detail Bahan, Detail Surat Jalan, Scan QR, Verifikasi. Di sana
 * "kembali" memang berarti kembali ke induknya, dan itu memakai [popAman].
 * Tombol kembali sistem (gestur/tombol perangkat) tidak ikut aturan ini: ia tetap
 * mundur selangkah seperti kebiasaan Android.
 *
 * Tidak perlu penjaga seperti [popAman]: pop berjangkar tidak bisa kebablasan.
 * Panggilan kedua saat sudah berada di [rute] hanya mengembalikan `false`.
 */
fun NavController.popKe(rute: String): Boolean = popBackStack(rute, inclusive = false)

/** Maju ke [rute] tanpa menumpuk salinan saat tombolnya ditekan berkali-kali. */
fun NavController.navigateSekali(rute: String) {
    if (currentDestination?.route == rute) return
    navigate(rute) { launchSingleTop = true }
}
