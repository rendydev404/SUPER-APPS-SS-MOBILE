package com.sukashawarma.superapp.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test

/**
 * Merekam jalur kode yang dilewati saat aplikasi dibuka dingin.
 *
 * CAKUPANNYA SENGAJA BERHENTI DI LAYAR LOGIN, dan itu bukan kelalaian: sesi
 * hanya hidup di memori, jadi setiap start dingin selalu mendarat di layar login
 * dan tidak ada cara sah melewatinya tanpa menanam kredensial di repositori.
 *
 * Bagian inilah yang paling mahal: pemuatan kelas aplikasi, inisialisasi
 * Compose, tema, navigasi, dan Material3 — semuanya dipakai ulang oleh SETIAP
 * layar sesudahnya, termasuk chat.
 */
class PembangkitBaselineProfile {

    private val paket = "com.sukashawarma.superapp"

    @get:Rule
    val aturan = BaselineProfileRule()

    @Test
    fun bukaAplikasi() = aturan.collect(packageName = paket) {
        pressHome()

        // Sengaja TIDAK memakai startActivityAndWait(). Fungsi itu menunggu
        // konfirmasi peluncuran lewat `dumpsys gfxinfo ... framestats`, dan pada
        // Android 16 pemeriksaan itu gagal ("Unable to confirm activity launch
        // completion") walaupun aplikasinya jelas terbuka di layar. Meluncurkan
        // sendiri lalu menunggu jendelanya muncul memberi hasil rekaman yang
        // sama, karena profilnya dikumpulkan ART, bukan oleh statistik frame.
        device.executeShellCommand("am start -n $paket/.presentation.MainActivity")
        device.wait(Until.hasObject(By.pkg(paket).depth(0)), 15_000)
        device.waitForIdle()

        // Memberi ruang bagi komposisi pertama benar-benar selesai sebelum
        // perekaman ditutup.
        Thread.sleep(3_000)
    }
}
