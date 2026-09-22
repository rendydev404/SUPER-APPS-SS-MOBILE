package com.sukashawarma.superapp.feature.leader.domain

import com.sukashawarma.superapp.feature.stok.domain.StokStatus
import com.sukashawarma.superapp.feature.stok.domain.UnitMeta
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

    private val tepung = UnitMeta(satuan = "Kg", satuanKecil = "Gram", faktorTampilan = 1000.0)

    @Test
    fun `status skala dipetakan ke tiga tingkat yang dipakai layar`() {
        assertEquals(StatusStok.KRITIS, StatusStok.dariSkala(StokStatus.BELOW))
        assertEquals(StatusStok.MENIPIS, StatusStok.dariSkala(StokStatus.WARNING))
        assertEquals(StatusStok.AMAN, StatusStok.dariSkala(StokStatus.OK))
    }

    /** Faktor satuan kosong tidak boleh membanjiri lencana merah. */
    @Test
    fun `status tak dapat dihitung dibaca aman`() {
        assertEquals(StatusStok.AMAN, StatusStok.dariSkala(StokStatus.UNKNOWN))
    }

    /**
     * Bug yang dilaporkan: saldo 2032 tersimpan dalam gram (`saldo_is_gram`), dulu
     * tampil "2032 Kg" berstatus aman. Setelah dinormalkan: 2 Kg 32 Gram, dan
     * dengan batas 5 Kg itu kritis (di bawah setengahnya).
     */
    @Test
    fun `saldo gram dinormalkan sebelum ditampilkan dan dibandingkan`() {
        val b = bahanCabang("t", "TEPUNG", 2032.0, saldoIsGram = true, threshold = 5.0, meta = tepung)
        assertEquals("2 Kg 32 Gram", b.saldoTeks)
        assertEquals("5 Kg", b.batasTeks)
        assertEquals(StatusStok.KRITIS, b.status)
    }

    @Test
    fun `saldo satuan besar legacy tidak dikali dua kali`() {
        val b = bahanCabang("t", "TEPUNG", 4.0, saldoIsGram = false, threshold = 5.0, meta = tepung)
        assertEquals("4 Kg", b.saldoTeks)
        assertEquals(StatusStok.MENIPIS, b.status)
    }

    @Test
    fun `faktor kosong menampilkan angka mentah dengan satuan skala barisnya`() {
        val tanpaFaktor = tepung.copy(faktorTampilan = null)
        val gram = bahanCabang("t", "TEPUNG", 2032.0, saldoIsGram = true, threshold = 5.0, meta = tanpaFaktor)
        assertEquals("2032 Gram", gram.saldoTeks)
        assertEquals("5 Kg", gram.batasTeks)
        val besar = bahanCabang("t", "TEPUNG", 3.0, saldoIsGram = false, threshold = 5.0, meta = tanpaFaktor)
        assertEquals("3 Kg", besar.saldoTeks)
    }

    /**
     * Bug yang dilaporkan kedua: bahan bersatuan tengah tampil ngawur. BAWANG
     * (1 Bal = 20 Kg = 20.000 Gram) bersaldo 1.500 gram dulu tertulis "75 Kg",
     * padahal layar Stok menulis 0 Bal 1 Kg 500 Gram.
     */
    @Test
    fun `saldo bersatuan tengah sama dengan layar stok`() {
        val bawang = UnitMeta(
            satuan = "Bal", satuanTengah = "Kg", satuanKecil = "Gram",
            faktorTengah = 20.0, faktorTampilan = 20000.0,
        )
        val b = bahanCabang("b", "BAWANG", 1500.0, saldoIsGram = true, threshold = 1.0, meta = bawang)
        assertEquals("1 Kg 500 Gram", b.saldoTeks)
        assertEquals(StatusStok.KRITIS, b.status)
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
