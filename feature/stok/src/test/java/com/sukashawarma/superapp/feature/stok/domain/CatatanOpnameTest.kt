package com.sukashawarma.superapp.feature.stok.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Kolom `opname_item.catatan` dipakai untuk dua hal sekaligus, dan yang menentukan
 * mode-nya adalah prefiks `[RAW]`, bukan berhasil-tidaknya JSON diurai.
 */
class CatatanOpnameTest {

    @Test
    fun `catatan biasa dibaca apa adanya`() {
        val hasil = parseCatatanOpname("Timbangan agak goyang saat menghitung")
        assertEquals("Timbangan agak goyang saat menghitung", hasil.catatanBebas)
        assertNull(hasil.targetKitchen)
    }

    @Test
    fun `titipan mesin membawa target kitchen`() {
        // Bentuk asli dari OpnameForm web: prefiks, spasi, lalu JSON pendek.
        val hasil = parseCatatanOpname("""[RAW] {"t":"120 porsi"}""")
        assertEquals("120 porsi", hasil.targetKitchen)
        // Titipan mesin bukan catatan orang — tidak boleh bocor ke baris catatan.
        assertNull(hasil.catatanBebas)
    }

    @Test
    fun `titipan mesin dengan json rusak tetap bukan catatan orang`() {
        val hasil = parseCatatanOpname("""[RAW] {"t":""")
        assertNull(hasil.targetKitchen)
        // Inilah asimetri yang mudah "dirapikan" keliru: menampilkannya sebagai
        // catatan bebas hanya memunculkan kurung kurawal di layar.
        assertNull(hasil.catatanBebas)
    }

    @Test
    fun `field t kosong dianggap tidak ada target`() {
        val hasil = parseCatatanOpname("""[RAW] {"t":"  "}""")
        assertNull(hasil.targetKitchen)
    }

    @Test
    fun `catatan kosong dan null menghasilkan dua duanya kosong`() {
        listOf(null, "", "   ").forEach { masukan ->
            val hasil = parseCatatanOpname(masukan)
            assertNull("masukan=$masukan", hasil.targetKitchen)
            assertNull("masukan=$masukan", hasil.catatanBebas)
        }
    }
}

/**
 * Bulak-balik masukan berjenjang lewat kolom `opname_item.catatan`.
 *
 * Regresi dari keluhan 9 Sep 2026: "simpan draft lalu lanjutkan" mengembalikan satu
 * angka besar di kolom satuan terkecil, bukan angka yang tadi diketik kru. Penyebabnya
 * native tidak pernah menyimpan masukan mentah — hanya totalnya.
 */
class MasukanBerjenjangTest {

    @Test
    fun `masukan tiga jenjang kembali persis seperti diketik`() {
        val asli = MasukanBerjenjang(besar = "2", tengah = "3", kecil = "500")
        val catatan = bungkusCatatanOpname("2 Dus 3 Kg 500 Gram", "5 Dus", "-1 Dus", asli)
        assertEquals(asli, parseCatatanOpname(catatan).masukan)
    }

    /** Kolom kosong harus tetap kosong, bukan berubah jadi "0". */
    @Test
    fun `kolom kosong tidak berubah jadi nol`() {
        val asli = MasukanBerjenjang(besar = "", tengah = "", kecil = "750")
        val hasil = parseCatatanOpname(bungkusCatatanOpname("750 Gram", "1 Kg", "-250 Gram", asli)).masukan
        assertEquals("", hasil?.besar)
        assertEquals("", hasil?.tengah)
        assertEquals("750", hasil?.kecil)
    }

    /** Payload web asli (punya `t` dan `traw`) tetap terbaca native. */
    @Test
    fun `payload web dengan target kitchen tetap terbaca`() {
        val web = """[RAW] {"f":"1 Dus","s":"2 Dus","d":"-1 Dus","raw":{"besar":"1","tengah":"","kecil":""},"t":"5 Dus"}"""
        val hasil = parseCatatanOpname(web)
        assertEquals("5 Dus", hasil.targetKitchen)
        assertEquals(MasukanBerjenjang("1", "", ""), hasil.masukan)
    }

    /** Baris lama tanpa `raw` tidak boleh crash; pemulihannya jatuh ke qty_fisik. */
    @Test
    fun `payload tanpa raw menghasilkan masukan null`() {
        val lama = """[RAW] {"f":"1 Dus","s":"2 Dus","d":"-1 Dus"}"""
        assertNull(parseCatatanOpname(lama).masukan)
    }

    @Test
    fun `catatan bebas bukan RAW tidak dianggap masukan`() {
        val hasil = parseCatatanOpname("stok basah kena bocor")
        assertNull(hasil.masukan)
        assertEquals("stok basah kena bocor", hasil.catatanBebas)
    }

    /** Seluruh kolom kosong tidak layak disebut masukan tersimpan. */
    @Test
    fun `raw yang semua kolomnya kosong dianggap tidak ada`() {
        val kosong = """[RAW] {"raw":{"besar":"","tengah":"","kecil":""}}"""
        assertNull(parseCatatanOpname(kosong).masukan)
    }
}
