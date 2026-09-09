package com.sukashawarma.superapp.notif

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Tujuan yang diminta sebuah notifikasi saat diketuk.
 *
 * Notifikasi tiba lewat Intent, sedangkan navigasi hidup di dalam Compose. Titik
 * temunya di sini: [MainActivity] menaruh tujuannya, `RootNav` mengambilnya
 * sekali lalu berpindah.
 *
 * Nilainya dikosongkan begitu dibaca ([ambil]). Tanpa itu, tiap kali layar
 * disusun ulang — rotasi, kembali dari latar — aplikasi akan melompat lagi ke
 * halaman yang sama dan pengguna tidak bisa keluar dari sana.
 */
object NotifikasiTujuan {

    /** Nilai `route` pada payload FCM, mis. "manager_persetujuan". */
    private val _rute = MutableStateFlow<String?>(null)
    val rute: StateFlow<String?> = _rute

    fun set(nilai: String?) {
        if (!nilai.isNullOrBlank()) _rute.value = nilai
    }

    fun ambil(): String? {
        val nilai = _rute.value
        _rute.value = null
        return nilai
    }

    /** Nama extra pada Intent notifikasi. */
    const val EXTRA_RUTE = "notif_rute"

    /** Rute yang dikenali. Payload di luar daftar ini diabaikan, bukan dipercaya. */
    const val MANAGER_PERSETUJUAN = "manager_persetujuan"
    const val MANAGER_WASTE = "manager_waste"
}
