package com.sukashawarma.superapp.feature.chat

import android.content.Context
import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.feature.chat.data.ChatRepository
import java.time.Instant

/**
 * Penanda "sudah dibaca sampai kapan" untuk Chat Tim, beserta penghitung pesan
 * yang belum dibaca — sumber angka pada lencana merah di Beranda.
 *
 * DISIMPAN LOKAL, BUKAN DI SERVER. Status baca itu milik satu perangkat: HP yang
 * dipakai membaca percakapan tidak ada urusannya dengan tablet yang tergeletak
 * di kantor. Menyimpannya di server berarti satu tabel lagi yang harus dijaga,
 * plus baris per orang per perangkat, demi angka yang tidak berharga bila
 * hilang.
 */
object ChatBacaan {

    private const val BERKAS = "chat_bacaan"
    private const val KUNCI = "terakhir_dibaca_ms"

    /** Batas hitungan. Di atas ini lencana cukup menulis "99+", jadi tidak ada
     *  gunanya menarik lebih banyak baris hanya untuk dijumlahkan. */
    const val MAKS_HITUNG = 100

    /**
     * Kapan terakhir percakapan ini dibaca di perangkat ini.
     *
     * Pemasangan baru dianggap sudah membaca semuanya (penanda = sekarang).
     * Kalau tidak, orang yang baru memasang aplikasi langsung disambut lencana
     * berisi seluruh percakapan hari itu — kabar yang tidak pernah ia lewatkan
     * karena memang belum punya aplikasinya.
     */
    fun terakhirDibaca(context: Context): Long {
        val prefs = context.getSharedPreferences(BERKAS, Context.MODE_PRIVATE)
        val tersimpan = prefs.getLong(KUNCI, 0L)
        if (tersimpan > 0L) return tersimpan
        val sekarang = System.currentTimeMillis()
        prefs.edit().putLong(KUNCI, sekarang).apply()
        return sekarang
    }

    fun tandaiDibaca(context: Context, sampaiMs: Long = System.currentTimeMillis()) {
        val prefs = context.getSharedPreferences(BERKAS, Context.MODE_PRIVATE)
        // Tidak pernah mundur. Sinyal yang datang terlambat (mis. penanda dari
        // pemuatan lama) tidak boleh membangkitkan lagi lencana yang sudah
        // dipadamkan pengguna dengan membuka percakapannya.
        if (sampaiMs > prefs.getLong(KUNCI, 0L)) {
            prefs.edit().putLong(KUNCI, sampaiMs).apply()
        }
    }

    /**
     * Jumlah pesan orang lain yang datang setelah penanda baca.
     *
     * Hanya kolom `id` yang diminta dan dibatasi [MAKS_HITUNG] baris: yang
     * dibutuhkan cuma cacahnya, dan Beranda memanggil ini setiap kali ada pesan
     * masuk. Menarik isi pesannya di sini berarti membayar seluruh percakapan
     * hanya untuk menggambar satu angka kecil.
     */
    suspend fun hitungBelumDibaca(context: Context, userId: String): Int {
        if (userId.isBlank()) return 0
        val batas = Instant.ofEpochMilli(terakhirDibaca(context)).toString()
        return Postgrest.select(
            ChatRepository.TABLE,
            listOf(
                "select" to "id",
                "created_at" to "gt.$batas",
                "sender_id" to "neq.$userId",
                "limit" to MAKS_HITUNG.toString(),
            ),
        ).size()
    }
}
