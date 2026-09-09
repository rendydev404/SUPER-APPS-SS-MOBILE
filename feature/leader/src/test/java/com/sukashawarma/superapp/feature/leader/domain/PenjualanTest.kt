package com.sukashawarma.superapp.feature.leader.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PenjualanTest {

    @Test
    fun `target nol berarti belum ditentukan, bukan target yang sudah tercapai`() {
        val data = PenjualanHariIni(omzet = 500_000, target = 0, pesanan = emptyList())
        assertFalse(data.adaTarget)
        assertFalse(data.tercapai)
        assertEquals(0f, data.rasio, 0.0001f)
    }

    @Test
    fun `rasio dipotong di seratus persen saat target terlampaui`() {
        val data = PenjualanHariIni(omzet = 3_000_000, target = 1_000_000, pesanan = emptyList())
        assertTrue(data.tercapai)
        assertEquals(1f, data.rasio, 0.0001f)
        assertEquals("100,0", data.persenTeks)
    }

    @Test
    fun `omzet tepat sama dengan target sudah dihitung tercapai`() {
        val data = PenjualanHariIni(omzet = 1_000_000, target = 1_000_000, pesanan = emptyList())
        assertTrue(data.tercapai)
    }

    @Test
    fun `persen ditulis satu angka di belakang koma dengan koma Indonesia`() {
        val data = PenjualanHariIni(omzet = 333_000, target = 1_000_000, pesanan = emptyList())
        assertEquals("33,3", data.persenTeks)
    }

    /**
     * Status baru di POS tidak boleh membuat baris pesanan hilang dari layar leader,
     * jadi apa pun selain dua yang dikenali dibaca sebagai "Proses".
     */
    @Test
    fun `status yang tak dikenal dibaca sebagai proses`() {
        assertEquals(StatusPesanan.SELESAI, StatusPesanan.dari("completed"))
        assertEquals(StatusPesanan.DIBATALKAN, StatusPesanan.dari("cancelled"))
        assertEquals(StatusPesanan.PROSES, StatusPesanan.dari("preparing"))
        assertEquals(StatusPesanan.PROSES, StatusPesanan.dari(null))
    }

    @Test
    fun `ringkasan item menyebut jumlah dan nama tiap menu`() {
        val pesanan = PesananTerbaru(
            id = "1",
            nomor = 12,
            dibuatPada = "2026-09-08T04:15:00.000+00:00",
            status = StatusPesanan.SELESAI,
            total = 75_000,
            promo = emptyList(),
            item = listOf(ItemPesanan("Shawarma Jumbo", 2), ItemPesanan("Es Teh", 1)),
        )
        assertEquals("2x Shawarma Jumbo, 1x Es Teh", pesanan.ringkasanItem)
    }

    @Test
    fun `pesanan tanpa rincian item tetap punya keterangan, bukan baris kosong`() {
        val pesanan = PesananTerbaru(
            id = "1",
            nomor = null,
            dibuatPada = "2026-09-08T04:15:00.000+00:00",
            status = StatusPesanan.PROSES,
            total = 0,
            promo = emptyList(),
            item = emptyList(),
        )
        assertEquals("Tidak ada detail item", pesanan.ringkasanItem)
    }

    /** Cap waktu server selalu UTC; yang dibaca leader adalah jam operasional outlet. */
    @Test
    fun `jam pesanan digeser ke waktu Jakarta`() {
        val pesanan = PesananTerbaru(
            id = "1",
            nomor = 1,
            dibuatPada = "2026-09-08T01:15:00+00:00",
            status = StatusPesanan.SELESAI,
            total = 0,
            promo = emptyList(),
            item = emptyList(),
        )
        assertEquals("08:15", pesanan.jam)
    }
}
