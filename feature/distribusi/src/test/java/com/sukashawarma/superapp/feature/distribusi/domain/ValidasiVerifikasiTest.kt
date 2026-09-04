package com.sukashawarma.superapp.feature.distribusi.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidasiVerifikasiTest {

    private fun isian(
        qtyTerima: Double? = 10.0,
        kondisi: KondisiItem = KondisiItem.BAIK,
        catatan: String = "",
        fotoPath: String? = "sj1/item1.jpg",
    ) = IsianVerifikasi(qtyTerima, kondisi, catatan, fotoPath)

    private fun pesanTolak(hasil: HasilValidasi): String =
        (hasil as HasilValidasi.Tolak).pesan

    @Test
    fun `kondisi baik dengan qty pas lolos`() {
        assertEquals(
            HasilValidasi.Lolos,
            ValidasiVerifikasi.konfirmasiKondisi(isian(qtyTerima = 10.0), 10L),
        )
    }

    @Test
    fun `qty kosong ditolak`() {
        val hasil = ValidasiVerifikasi.konfirmasiKondisi(isian(qtyTerima = null), 10L)
        assertTrue(hasil is HasilValidasi.Tolak)
        assertEquals("Isi jumlah fisik yang diterima terlebih dahulu.", pesanTolak(hasil))
    }

    /** Nol sah untuk item yang tidak sesuai (barang tidak sampai sama sekali),
     *  tapi tidak masuk akal untuk item yang dinyatakan baik. */
    @Test
    fun `qty nol ditolak untuk kondisi baik`() {
        val hasil = ValidasiVerifikasi.konfirmasiKondisi(isian(qtyTerima = 0.0), 10L)
        assertTrue(hasil is HasilValidasi.Tolak)
        assertEquals("Isi jumlah fisik yang diterima terlebih dahulu.", pesanTolak(hasil))
    }

    @Test
    fun `qty nol diterima untuk kondisi tidak sesuai bila ada catatan`() {
        val hasil = ValidasiVerifikasi.konfirmasiKondisi(
            isian(qtyTerima = 0.0, kondisi = KondisiItem.TIDAK_SESUAI, catatan = "Barang tidak sampai"),
            10L,
        )
        assertEquals(HasilValidasi.Lolos, hasil)
    }

    @Test
    fun `qty negatif ditolak`() {
        val hasil = ValidasiVerifikasi.konfirmasiKondisi(isian(qtyTerima = -1.0), 10L)
        assertTrue(hasil is HasilValidasi.Tolak)
        assertEquals("Jumlah terima tidak boleh kurang dari 0.", pesanTolak(hasil))
    }

    @Test
    fun `qty melebihi kiriman ditolak`() {
        val hasil = ValidasiVerifikasi.konfirmasiKondisi(isian(qtyTerima = 11.0), 10L)
        assertTrue(hasil is HasilValidasi.Tolak)
        assertEquals("Jumlah terima tidak boleh melebihi jumlah yang dikirim.", pesanTolak(hasil))
    }

    @Test
    fun `qty sama dengan kiriman diterima`() {
        assertEquals(
            HasilValidasi.Lolos,
            ValidasiVerifikasi.konfirmasiKondisi(isian(qtyTerima = 10.0), 10L),
        )
    }

    @Test
    fun `tidak sesuai tanpa catatan ditolak`() {
        val hasil = ValidasiVerifikasi.konfirmasiKondisi(
            isian(qtyTerima = 8.0, kondisi = KondisiItem.TIDAK_SESUAI, catatan = "   "),
            10L,
        )
        assertTrue(hasil is HasilValidasi.Tolak)
        assertEquals("Item tidak sesuai wajib disertai catatan alasan.", pesanTolak(hasil))
    }

    @Test
    fun `tidak sesuai dengan catatan lolos`() {
        val hasil = ValidasiVerifikasi.konfirmasiKondisi(
            isian(qtyTerima = 8.0, kondisi = KondisiItem.TIDAK_SESUAI, catatan = "2 dus penyok"),
            10L,
        )
        assertEquals(HasilValidasi.Lolos, hasil)
    }

    @Test
    fun `lanjut tanpa foto ditolak`() {
        val hasil = ValidasiVerifikasi.bolehLanjut(isian(fotoPath = null))
        assertTrue(hasil is HasilValidasi.Tolak)
        assertEquals("Foto bukti wajib diambil sebelum lanjut ke item berikutnya.", pesanTolak(hasil))
    }

    @Test
    fun `lanjut dengan foto lolos`() {
        assertEquals(HasilValidasi.Lolos, ValidasiVerifikasi.bolehLanjut(isian()))
    }

    @Test
    fun `kondisi dipetakan ke nilai kolom database`() {
        assertEquals("baik", KondisiItem.BAIK.nilaiDb)
        assertEquals("rusak", KondisiItem.TIDAK_SESUAI.nilaiDb)
    }
}
