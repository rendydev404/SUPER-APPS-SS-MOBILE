package com.sukashawarma.superapp.feature.distribusi.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PengirimanPusatTest {

    @Test
    fun `kategori dinormalisasi seperti web`() {
        assertEquals(PengirimanPusat.Kategori.ITEM_CORE, PengirimanPusat.kategori("Protein"))
        assertEquals(PengirimanPusat.Kategori.ITEM_CORE, PengirimanPusat.kategori("sayur"))
        assertEquals(PengirimanPusat.Kategori.BUMBU, PengirimanPusat.kategori("saus"))
        assertEquals(PengirimanPusat.Kategori.LAINNYA, PengirimanPusat.kategori("gas"))
        assertEquals(PengirimanPusat.Kategori.KEMASAN, PengirimanPusat.kategori("kemasan"))
        assertEquals(PengirimanPusat.Kategori.LAINNYA, PengirimanPusat.kategori(null))
    }

    @Test
    fun `varian lama peran admin tetap dihitung`() {
        assertTrue(PengirimanPusat.sudahTtdAdmin(listOf("Kitchen SPV")))
        assertTrue(PengirimanPusat.sudahTtdAdmin(listOf("Admin Gudang")))
        assertTrue(PengirimanPusat.sudahTtdAdmin(listOf(PengirimanPusat.PERAN_ADMIN)))
        assertFalse(PengirimanPusat.sudahTtdAdmin(listOf("Supir")))
    }

    @Test
    fun `vendor yang belum dipilih ditahan lebih dulu daripada tanda tangan`() {
        assertEquals(
            "Terdapat 2 bahan yang belum dipilih vendornya. Tentukan vendornya terlebih dahulu.",
            PengirimanPusat.alasanTidakBisaKirim(2, emptyList()),
        )
    }

    @Test
    fun `tanda tangan yang kurang disebut semua`() {
        assertEquals(
            "Tanda tangan yang masih diperlukan: Admin Gudang, Supir (Kurir)",
            PengirimanPusat.alasanTidakBisaKirim(0, emptyList()),
        )
        assertEquals(
            "Tanda tangan yang masih diperlukan: Supir (Kurir)",
            PengirimanPusat.alasanTidakBisaKirim(0, listOf("Admin Kitchen")),
        )
        assertNull(PengirimanPusat.alasanTidakBisaKirim(0, listOf("Admin Kitchen", "Supir")))
    }

    @Test
    fun `kode verifikasi tertutup selama draft dan setelah batal`() {
        assertFalse(PengirimanPusat.kodeTerbuka(StatusSuratJalan.DRAFT))
        assertFalse(PengirimanPusat.kodeTerbuka(StatusSuratJalan.DIBATALKAN))
        assertFalse(PengirimanPusat.kodeTerbuka(null))
        assertTrue(PengirimanPusat.kodeTerbuka(StatusSuratJalan.DIKIRIM))
    }

    @Test
    fun `angka numeric di pesan penjaga dirapikan`() {
        assertEquals(
            "Sisa Vendor A kg tinggal 12 kg, surat jalan butuh 15.5",
            PengirimanPusat.rapikanPesanKirim("Sisa Vendor A kg tinggal 12.000 kg, surat jalan butuh 15.500"),
        )
    }
}
