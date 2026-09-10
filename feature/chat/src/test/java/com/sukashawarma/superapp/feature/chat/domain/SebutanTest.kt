package com.sukashawarma.superapp.feature.chat.domain

import com.sukashawarma.superapp.feature.chat.data.Sebutan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SebutanTest {

    @Test
    fun `at di awal kata membuka kueri`() {
        assertEquals(KueriSebutan(5, "bu"), cariKueriSebutan("Halo @bu", 8))
    }

    @Test
    fun `at di tengah kata bukan sebutan`() {
        assertNull(cariKueriSebutan("kirim ke budi@sukashawarma.com", 20))
    }

    @Test
    fun `kueri boleh mengandung spasi karena nama orang punya spasi`() {
        assertEquals(KueriSebutan(0, "Budi Har"), cariKueriSebutan("@Budi Har", 9))
    }

    @Test
    fun `kalimat panjang setelah at berhenti dianggap kueri`() {
        assertNull(cariKueriSebutan("@Budi sudah cek stok belum", 26))
    }

    @Test
    fun `sisip mengganti kueri dan menaruh kursor setelah spasi`() {
        val kueri = cariKueriSebutan("Halo @bu", 8)!!
        val (teks, kursor) = sisipkanSebutan("Halo @bu", kueri, "Budi Hartono")
        assertEquals("Halo @Budi Hartono ", teks)
        assertEquals(19, kursor)
    }

    @Test
    fun `sisip di tengah teks tidak merusak sisanya`() {
        val awal = "Halo @bu tolong cek"
        val kueri = cariKueriSebutan(awal, 8)!!
        val (teks, kursor) = sisipkanSebutan(awal, kueri, "Budi")
        assertEquals("Halo @Budi tolong cek", teks)
        assertEquals(11, kursor)
    }

    @Test
    fun `nama yang dihapus lagi tidak ikut terkirim`() {
        val kandidat = listOf(Sebutan("1", "Budi"), Sebutan("2", "Sari"))
        assertEquals(
            listOf(Sebutan("1", "Budi")),
            sebutanTerpakai("Halo @Budi apa kabar", kandidat),
        )
    }

    @Test
    fun `nama terpanjang menang supaya sorotannya tidak terpotong`() {
        val teks = "@Budi Hartono tolong"
        val hasil = rentangSebutan(teks, listOf(Sebutan("1", "Budi"), Sebutan("2", "Budi Hartono")))
        assertEquals(1, hasil.size)
        assertEquals("2", hasil[0].second.id)
        assertEquals(0..12, hasil[0].first)
    }

    @Test
    fun `nama yang sama disebut dua kali disorot dua kali`() {
        val hasil = rentangSebutan("@Budi dan @Budi", listOf(Sebutan("1", "Budi")))
        assertEquals(listOf(0..4, 10..14), hasil.map { it.first })
    }
}
