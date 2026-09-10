package com.sukashawarma.superapp.feature.chat

/**
 * Menandai apakah layar chat sedang terbuka di depan mata pengguna.
 *
 * Dibaca penerima push (`SuperappMessagingService`) untuk tidak memunculkan
 * notifikasi pesan yang justru sedang dibaca — perilaku WhatsApp. Tanpa ini,
 * setiap pesan yang masuk saat percakapan berlangsung akan berbunyi dan
 * menumpuk di baki notifikasi.
 *
 * Sengaja `object` sederhana, bukan aliran: pembacanya di luar Compose dan
 * hanya butuh satu jawaban ya/tidak pada saat push tiba.
 */
object ChatKehadiran {
    @Volatile
    var terbuka: Boolean = false
        private set

    /**
     * Nama grup terakhir yang diketahui, dipakai sebagai judul percakapan di
     * notifikasi. Pengelola bisa menggantinya kapan saja, dan notifikasi tidak
     * punya jalur sendiri untuk membacanya dari server.
     */
    @Volatile
    var namaGrup: String = "Chat Tim"
        private set

    fun catatNamaGrup(nama: String) {
        if (nama.isNotBlank()) namaGrup = nama
    }

    fun masuk() {
        terbuka = true
    }

    fun keluar() {
        terbuka = false
    }
}
