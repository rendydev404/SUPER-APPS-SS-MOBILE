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
