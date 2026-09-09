package com.sukashawarma.superapp.feature.leader.domain

/**
 * Tingkat kecukupan sebuah bahan di cabang.
 *
 * Nilainya diambil dari kolom `status` view `monitoring_view_scoped`, yang
 * menghitungnya dengan aturan yang sama seperti heuristik di halaman web:
 * `below` saat saldo di bawah setengah titik pesan ulang, `warning` saat masih di
 * bawah titik pesan ulang, `ok` selebihnya. Bedanya, di view angka pembandingnya
 * adalah `reorder_point` sungguhan milik bahan itu — bukan tebakan 10-atau-5
 * berdasarkan nama satuan seperti di web.
 */
enum class StatusStok(val label: String, val urutan: Int) {
    KRITIS("Kritis", 0),
    MENIPIS("Menipis", 1),
    AMAN("Aman", 2);

    companion object {
        /**
         * Status tak dikenal (termasuk null, saat view menambah nilai baru) dibaca
         * sebagai AMAN. Menandai yang tak dikenal sebagai kritis akan membanjiri
         * lencana merah dengan hal yang belum tentu bermasalah.
         */
        fun dariView(nilai: String?): StatusStok = when (nilai) {
            "below" -> KRITIS
            "warning" -> MENIPIS
            else -> AMAN
        }
    }
}

/** Satu bahan baku di cabang terpilih. */
data class BahanCabang(
    val id: String,
    val nama: String,
    val saldo: Double,
    val satuan: String,
    val batasMinimal: Double?,
    val status: StatusStok,
) {
    val saldoTeks: String get() = "${kuantitas(saldo)} $satuan".trim()

    /** Null saat bahan belum punya titik pesan ulang — layar menyebutnya "belum diatur". */
    val batasTeks: String?
        get() = batasMinimal?.let { "${kuantitas(it)} $satuan".trim() }
}

/**
 * Urutan tampil: yang paling genting lebih dulu, lalu abjad.
 *
 * Cermin pengurutan halaman web (`rank` kritis/menipis/aman, lalu
 * `localeCompare`). Perbandingan namanya memakai `compareTo` peka-huruf-besar
 * yang sama dengan sisa modul ini supaya urutannya tidak berubah antar perangkat
 * yang locale-nya berbeda.
 */
fun urutkanStok(daftar: List<BahanCabang>): List<BahanCabang> =
    daftar.sortedWith(compareBy({ it.status.urutan }, { it.nama.lowercase() }))

/** Penyaring kotak pencarian: cocok bila nama bahan mengandung [kunci], tanpa peduli huruf besar. */
fun saringStok(daftar: List<BahanCabang>, kunci: String): List<BahanCabang> {
    val q = kunci.trim()
    if (q.isEmpty()) return daftar
    return daftar.filter { it.nama.contains(q, ignoreCase = true) }
}
