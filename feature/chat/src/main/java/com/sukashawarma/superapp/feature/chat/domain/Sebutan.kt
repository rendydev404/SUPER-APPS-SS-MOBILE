package com.sukashawarma.superapp.feature.chat.domain

import com.sukashawarma.superapp.feature.chat.data.Sebutan

/**
 * Aturan @sebutan: mengenali apa yang sedang diketik, menyisipkan nama yang
 * dipilih, dan menemukan kembali potongan teksnya untuk disorot.
 *
 * Semuanya fungsi murni di lapisan domain, bukan di dalam composable. Perilaku
 * yang menentukan di sini — di mana sebuah sebutan dimulai, apakah ia masih ada
 * setelah teksnya disunting — adalah hal yang paling mudah salah dan paling
 * murah diuji tanpa perangkat.
 */

/** Panjang maksimum kata yang masih dianggap sedang mencari nama. */
private const val MAKS_KUERI = 32

/** Sebuah nama boleh mengandung spasi ("Budi Hartono"), tapi tidak sekalimat. */
private const val MAKS_SPASI_KUERI = 2

/** Potongan "@..." yang sedang diketik tepat sebelum kursor. */
data class KueriSebutan(val mulai: Int, val kueri: String)

/**
 * Menemukan sebutan yang sedang diketik pada posisi [kursor], atau null bila
 * tidak ada.
 *
 * Pencarian mundur dari kursor, bukan maju dari awal teks: yang menentukan
 * hanyalah kata tempat kursor berada, dan sebutan lama di kalimat yang sama
 * tidak boleh ikut membuka daftar lagi.
 */
fun cariKueriSebutan(teks: String, kursor: Int): KueriSebutan? {
    if (kursor !in 0..teks.length) return null
    var i = kursor - 1
    var spasi = 0
    while (i >= 0) {
        val c = teks[i]
        if (c == '\n') return null
        if (c == '@') {
            // '@' hanya membuka daftar bila ia mengawali kata — kalau tidak,
            // setiap alamat surel yang diketik akan memunculkan pemilih nama.
            val sebelum = if (i == 0) ' ' else teks[i - 1]
            if (i != 0 && !sebelum.isWhitespace()) return null
            return KueriSebutan(i, teks.substring(i + 1, kursor))
        }
        if (c.isWhitespace()) {
            spasi++
            if (spasi > MAKS_SPASI_KUERI) return null
        }
        if (kursor - i > MAKS_KUERI) return null
        i--
    }
    return null
}

/**
 * Mengganti potongan "@kueri" dengan "@Nama " dan mengembalikan teks baru
 * beserta posisi kursor sesudahnya.
 *
 * Spasi di belakang sengaja ikut: tanpa itu huruf berikutnya menempel pada nama
 * dan sebutannya berhenti cocok saat dicari ulang oleh [sebutanTerpakai].
 */
fun sisipkanSebutan(teks: String, kueri: KueriSebutan, nama: String): Pair<String, Int> {
    val akhir = (kueri.mulai + 1 + kueri.kueri.length).coerceAtMost(teks.length)
    // Kalau di belakangnya sudah ada spasi, spasi itu yang dipakai — menambah
    // satu lagi meninggalkan celah ganda tiap kali menyebut orang di tengah
    // kalimat yang sudah tertulis.
    val ganti = if (teks.getOrNull(akhir) == ' ') "@$nama" else "@$nama "
    val baru = teks.substring(0, kueri.mulai) + ganti + teks.substring(akhir)
    return baru to (kueri.mulai + ganti.length + if (teks.getOrNull(akhir) == ' ') 1 else 0)
}

/**
 * Menyaring [kandidat] menjadi sebutan yang benar-benar masih ada di [teks].
 *
 * Dipanggil tepat sebelum kirim. Seseorang yang namanya dipilih lalu dihapus
 * lagi dari kotak ketik tidak boleh tetap menerima notifikasi hanya karena
 * pernah tersentuh pemilih.
 */
fun sebutanTerpakai(teks: String, kandidat: List<Sebutan>): List<Sebutan> =
    kandidat.distinctBy { it.id }.filter { teks.contains("@${it.nama}") }

/**
 * Letak setiap sebutan di dalam [teks], untuk disorot dan dijadikan sasaran
 * ketukan.
 *
 * Nama terpanjang dicocokkan lebih dulu supaya "@Budi Hartono" tidak terpotong
 * menjadi "@Budi" ketika keduanya sama-sama anggota grup. Rentang yang sudah
 * terpakai tidak boleh dipakai ulang, sehingga sorotannya tidak pernah tumpang
 * tindih.
 */
fun rentangSebutan(teks: String, sebutan: List<Sebutan>): List<Pair<IntRange, Sebutan>> {
    if (sebutan.isEmpty() || teks.isEmpty()) return emptyList()
    val hasil = mutableListOf<Pair<IntRange, Sebutan>>()
    val terpakai = BooleanArray(teks.length)
    sebutan.sortedByDescending { it.nama.length }.forEach { s ->
        val pola = "@${s.nama}"
        if (pola.length <= 1) return@forEach
        var dari = 0
        while (true) {
            val idx = teks.indexOf(pola, dari)
            if (idx < 0) break
            val akhir = idx + pola.length - 1
            if ((idx..akhir).none { terpakai[it] }) {
                for (k in idx..akhir) terpakai[k] = true
                hasil += (idx..akhir) to s
            }
            dari = idx + 1
        }
    }
    return hasil.sortedBy { it.first.first }
}
