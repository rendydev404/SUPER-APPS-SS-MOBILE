package com.sukashawarma.superapp.feature.distribusi.domain

import org.junit.Assert.assertEquals
import org.junit.Test

private data class BarisUji(
    override val status: StatusSuratJalan?,
    override val namaOutlet: String?,
    override val adaSelisih: Boolean,
) : BarisRingkasan

class RingkasanDistribusiTest {

    @Test
    fun `hitungan per status memisahkan keenam nilai`() {
        val baris = listOf(
            BarisUji(StatusSuratJalan.DRAFT, "A", false),
            BarisUji(StatusSuratJalan.DIKIRIM, "A", false),
            BarisUji(StatusSuratJalan.DIKIRIM_LENGKAP, "A", false),
            BarisUji(StatusSuratJalan.DITERIMA_LENGKAP, "A", false),
            BarisUji(StatusSuratJalan.DITERIMA_SEBAGIAN, "A", true),
            BarisUji(StatusSuratJalan.SELESAI, "A", false),
        )
        val hitungan = RingkasanDistribusi.hitungStatus(baris)
        assertEquals(1, hitungan.draft)
        // `dikirim` dan `dikirim_lengkap` sama-sama "dalam transit".
        assertEquals(2, hitungan.dikirim)
        assertEquals(2, hitungan.diterima)
        assertEquals(1, hitungan.selesai)
    }

    @Test
    fun `status null tidak dihitung di mana pun`() {
        val hitungan = RingkasanDistribusi.hitungStatus(listOf(BarisUji(null, "A", false)))
        assertEquals(HitunganStatus(0, 0, 0, 0), hitungan)
    }

    @Test
    fun `akurasi seratus persen bila belum ada yang terverifikasi`() {
        val baris = listOf(
            BarisUji(StatusSuratJalan.DRAFT, "A", false),
            BarisUji(StatusSuratJalan.DIKIRIM, "A", false),
        )
        assertEquals(100, RingkasanDistribusi.tingkatAkurasi(baris))
    }

    @Test
    fun `akurasi seratus persen bila daftar kosong`() {
        assertEquals(100, RingkasanDistribusi.tingkatAkurasi(emptyList()))
    }

    @Test
    fun `akurasi membandingkan yang bermasalah terhadap yang terverifikasi`() {
        val baris = listOf(
            BarisUji(StatusSuratJalan.SELESAI, "A", false),
            BarisUji(StatusSuratJalan.DITERIMA_LENGKAP, "A", false),
            BarisUji(StatusSuratJalan.DITERIMA_SEBAGIAN, "A", true),
            BarisUji(StatusSuratJalan.DITERIMA_LENGKAP, "A", false),
            // Yang belum terverifikasi tidak ikut menghitung penyebut.
            BarisUji(StatusSuratJalan.DIKIRIM, "A", false),
        )
        // 4 terverifikasi, 1 bermasalah -> 3/4 = 75%
        assertEquals(75, RingkasanDistribusi.tingkatAkurasi(baris))
    }

    @Test
    fun `akurasi tidak pernah negatif`() {
        val baris = listOf(
            BarisUji(StatusSuratJalan.DITERIMA_SEBAGIAN, "A", true),
            BarisUji(StatusSuratJalan.DIKIRIM, "A", true),
        )
        // 1 terverifikasi, 2 bermasalah -> hasil mentahnya negatif, dijepit ke 0.
        assertEquals(0, RingkasanDistribusi.tingkatAkurasi(baris))
    }

    @Test
    fun `rincian outlet mengelompokkan dan mengurutkan menurun`() {
        val baris = listOf(
            BarisUji(StatusSuratJalan.DIKIRIM, "Outlet B", false),
            BarisUji(StatusSuratJalan.DIKIRIM, "Outlet A", false),
            BarisUji(StatusSuratJalan.SELESAI, "Outlet A", false),
            BarisUji(StatusSuratJalan.DITERIMA_SEBAGIAN, "Outlet A", true),
        )
        val rincian = RingkasanDistribusi.rincianOutlet(baris, "Gudang Pusat")
        assertEquals(2, rincian.size)
        assertEquals("Outlet A", rincian[0].nama)
        assertEquals(3, rincian[0].total)
        // Aktif = dalam transit atau sudah diterima tapi belum ditutup.
        assertEquals(2, rincian[0].aktif)
        assertEquals(1, rincian[0].bermasalah)
        assertEquals("Outlet B", rincian[1].nama)
        assertEquals(1, rincian[1].total)
    }

    @Test
    fun `outlet tanpa nama memakai nama bawaan`() {
        val rincian = RingkasanDistribusi.rincianOutlet(
            listOf(BarisUji(StatusSuratJalan.DIKIRIM, null, false)),
            "Gudang Pusat",
        )
        assertEquals("Gudang Pusat", rincian[0].nama)
    }

    @Test
    fun `rincian outlet dipotong pada batas maksimum`() {
        val baris = (1..10).map { BarisUji(StatusSuratJalan.DIKIRIM, "Outlet $it", false) }
        assertEquals(6, RingkasanDistribusi.rincianOutlet(baris, "Gudang Pusat").size)
        assertEquals(3, RingkasanDistribusi.rincianOutlet(baris, "Gudang Pusat", maksimum = 3).size)
    }
}
