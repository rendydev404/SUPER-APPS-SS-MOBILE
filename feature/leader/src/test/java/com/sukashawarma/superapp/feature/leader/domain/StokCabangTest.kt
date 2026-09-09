package com.sukashawarma.superapp.feature.leader.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StokCabangTest {

    private fun bahan(nama: String, status: StatusStok, saldo: Double = 5.0, batas: Double? = 10.0) =
        BahanCabang(
            id = nama,
            nama = nama,
            saldo = saldo,
            satuan = "kg",
            batasMinimal = batas,
            status = status,
        )

    @Test
    fun `status view dipetakan ke tiga tingkat yang dipakai layar`() {
        assertEquals(StatusStok.KRITIS, StatusStok.dariView("below"))
        assertEquals(StatusStok.MENIPIS, StatusStok.dariView("warning"))
        assertEquals(StatusStok.AMAN, StatusStok.dariView("ok"))
    }

    /**
     * Nilai baru dari view tidak boleh membanjiri lencana merah dengan hal yang
     * belum tentu bermasalah.
     */
    @Test
    fun `status tak dikenal dan null dibaca aman`() {
        assertEquals(StatusStok.AMAN, StatusStok.dariView("status_baru"))
        assertEquals(StatusStok.AMAN, StatusStok.dariView(null))
    }

    @Test
    fun `yang paling genting muncul lebih dulu`() {
        val hasil = urutkanStok(
            listOf(
                bahan("Aman A", StatusStok.AMAN),
                bahan("Kritis Z", StatusStok.KRITIS),
                bahan("Menipis M", StatusStok.MENIPIS),
            )
        )
        assertEquals(listOf("Kritis Z", "Menipis M", "Aman A"), hasil.map { it.nama })
    }

    @Test
    fun `dalam tingkat yang sama diurutkan abjad tanpa peduli huruf besar`() {
        val hasil = urutkanStok(
            listOf(
                bahan("zaitun", StatusStok.KRITIS),
                bahan("Ayam", StatusStok.KRITIS),
            )
        )
        assertEquals(listOf("Ayam", "zaitun"), hasil.map { it.nama })
    }

    @Test
    fun `pencarian mengabaikan besar kecil huruf dan spasi di tepi`() {
        val daftar = listOf(bahan("Daging Sapi", StatusStok.AMAN), bahan("Roti", StatusStok.AMAN))
        assertEquals(listOf("Daging Sapi"), saringStok(daftar, "  sapi ").map { it.nama })
    }

    @Test
    fun `kunci kosong mengembalikan seluruh daftar`() {
        val daftar = listOf(bahan("Daging", StatusStok.AMAN), bahan("Roti", StatusStok.AMAN))
        assertEquals(2, saringStok(daftar, "").size)
        assertEquals(2, saringStok(daftar, "   ").size)
    }

    @Test
    fun `saldo dan batas ditulis dengan satuannya`() {
        val b = bahan("Daging", StatusStok.AMAN, saldo = 12.5, batas = 5.0)
        assertEquals("12,5 kg", b.saldoTeks)
        assertEquals("5 kg", b.batasTeks)
    }

    @Test
    fun `bahan tanpa titik pesan ulang tidak mengarang batas minimal`() {
        assertNull(bahan("Daging", StatusStok.AMAN, batas = null).batasTeks)
    }

    @Test
    fun `kuantitas membuang nol di belakang dan memakai koma desimal`() {
        assertEquals("12", kuantitas(12.0))
        assertEquals("12,5", kuantitas(12.5))
        assertEquals("12,35", kuantitas(12.346))
        assertEquals("0", kuantitas(0.0))
    }

    /**
     * `12.345` sebagai `Double` sebenarnya bernilai 12.34499999999999975…, jadi
     * pembulatannya turun ke 12,34 — bukan naik ke 12,35 seperti dugaan pertama.
     *
     * Dikunci di sini karena `toFixed(2)` di JavaScript membulatkan angka yang sama
     * ke atas, sehingga satu bahan bisa tertulis beda satu sen antara HP dan laptop.
     * Selisih itu tidak mengubah keputusan siapa pun soal stok, jadi tidak dikejar —
     * tapi kalau suatu saat ada yang melaporkannya, penjelasannya ada di sini dan
     * bukan misteri baru.
     */
    @Test
    fun `pecahan yang jatuh tepat di tengah mengikuti nilai Double sesungguhnya`() {
        assertEquals("12,34", kuantitas(12.345))
    }
}
