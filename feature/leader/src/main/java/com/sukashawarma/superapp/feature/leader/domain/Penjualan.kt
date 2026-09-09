package com.sukashawarma.superapp.feature.leader.domain

/** Tahapan sebuah pesanan seperti tersimpan di kolom `orders.status`. */
enum class StatusPesanan(val label: String) {
    SELESAI("Selesai"),
    DIBATALKAN("Dibatalkan"),
    PROSES("Proses");

    companion object {
        /**
         * Hanya `completed` dan `cancelled` yang dikenali; sisanya — `pending`,
         * `preparing`, dan status baru apa pun yang muncul kelak — dibaca sebagai
         * "Proses". Cermin rantai `? :` di daftar transaksi web, dan sengaja
         * dipertahankan longgar: status baru di POS tidak boleh membuat baris
         * pesanan hilang dari layar leader.
         */
        fun dari(nilai: String?): StatusPesanan = when (nilai) {
            "completed" -> SELESAI
            "cancelled" -> DIBATALKAN
            else -> PROSES
        }
    }
}

/** Satu baris pada "Transaksi Terakhir Hari Ini". */
data class PesananTerbaru(
    val id: String,
    val nomor: Int?,
    val dibuatPada: String,
    val status: StatusPesanan,
    val total: Long,
    val promo: List<String>,
    val item: List<ItemPesanan>,
) {
    val jam: String get() = jamJakarta(dibuatPada)

    /** "2x Shawarma Jumbo, 1x Es Teh" — ringkasan isi pesanan pada baris kedua kartu. */
    val ringkasanItem: String
        get() = if (item.isEmpty()) {
            "Tidak ada detail item"
        } else {
            item.joinToString(", ") { "${it.jumlah}x ${it.nama}" }
        }
}

data class ItemPesanan(val nama: String, val jumlah: Int)

/**
 * Isi layar Penjualan & Target — cermin `app/dashboard/leader/sales/page.tsx` web.
 */
data class PenjualanHariIni(
    val omzet: Long,
    val target: Long,
    val pesanan: List<PesananTerbaru>,
) {
    /** Target nol berarti belum ditetapkan, bukan target yang mustahil dicapai. */
    val adaTarget: Boolean get() = target > 0

    val tercapai: Boolean get() = adaTarget && omzet >= target

    /**
     * Rasio pencapaian 0..1 untuk bilah progres. Dipotong di 1 supaya bilah tidak
     * meluber saat target terlampaui — persis `Math.min(..., 100)` di web.
     */
    val rasio: Float
        get() = if (!adaTarget) 0f else (omzet.toDouble() / target).coerceIn(0.0, 1.0).toFloat()

    /** Persen dengan satu angka di belakang koma, koma desimal Indonesia. */
    val persenTeks: String
        get() {
            if (!adaTarget) return "0,0"
            val persen = kotlin.math.round(rasio * 1000) / 10.0
            return persen.toString().replace('.', ',')
        }

    companion object {
        val KOSONG = PenjualanHariIni(omzet = 0L, target = 0L, pesanan = emptyList())
    }
}
