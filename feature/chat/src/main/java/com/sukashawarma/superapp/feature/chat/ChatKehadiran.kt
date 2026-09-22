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
     * Id lawan bicara yang percakapan PRIBADI-nya sedang terbuka, atau null.
     *
     * Dibedakan dari [terbuka] karena aturannya berbeda: pesan pribadi hanya
     * boleh dibungkam bila percakapan dengan orang ITU yang sedang dilihat.
     * Pesan pribadi dari orang lain — atau yang tiba saat pengguna sedang di
     * tab grup — tetap harus berbunyi, persis WhatsApp yang hanya mendiamkan
     * obrolan yang sedang dibuka.
     */
    @Volatile
    var partnerTerbuka: String? = null
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

    fun masukPribadi(partnerId: String) {
        partnerTerbuka = partnerId
    }

    /** Hanya melepas bila yang keluar memang percakapan yang tercatat. */
    fun keluarPribadi(partnerId: String) {
        if (partnerTerbuka == partnerId) partnerTerbuka = null
    }
}
