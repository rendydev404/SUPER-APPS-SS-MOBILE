package com.sukashawarma.superapp.feature.leader.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RingkasanTest {

    private fun pesanan(outletId: String, total: Long, jam: String) =
        PesananSelesai(outletId, total, "2026-09-08T${jam}:00.000+07:00")

    @Test
    fun `cabang tanpa transaksi tetap muncul dengan nol`() {
        val hasil = susunPerOutlet(
            outletIds = listOf("a", "b"),
            namaOutlet = mapOf("a" to "Cabang A", "b" to "Cabang B"),
            pesanan = listOf(pesanan("a", 50_000, "09:00")),
        )
        assertEquals(2, hasil.size)
        val b = hasil.first { it.id == "b" }
        assertEquals(0L, b.omzet)
        assertEquals(0, b.transaksi)
    }

    @Test
    fun `rincian diurutkan omzet terbesar lebih dulu`() {
        val hasil = susunPerOutlet(
            outletIds = listOf("kecil", "besar"),
            namaOutlet = mapOf("kecil" to "Kecil", "besar" to "Besar"),
            pesanan = listOf(
                pesanan("kecil", 10_000, "08:00"),
                pesanan("besar", 90_000, "08:10"),
                pesanan("besar", 10_000, "08:20"),
            ),
        )
        assertEquals(listOf("besar", "kecil"), hasil.map { it.id })
        assertEquals(100_000L, hasil[0].omzet)
        assertEquals(2, hasil[0].transaksi)
    }

    @Test
    fun `nama cabang yang tak dikenal jatuh ke label netral, bukan id mentah`() {
        val hasil = susunPerOutlet(listOf("x"), emptyMap(), emptyList())
        assertEquals("Cabang", hasil.single().nama)
    }

    /**
     * Urutan daftar dari server tidak dijamin. Mengambil baris terakhir alih-alih cap
     * waktu terbesar pernah membuat "terakhir 08.14" tampil pada pukul sebelas.
     */
    @Test
    fun `jam terakhir dipilih dari cap waktu terbesar, bukan baris terakhir`() {
        val hasil = jamTransaksiTerakhir(
            listOf(
                pesanan("a", 1, "11:30"),
                pesanan("a", 1, "08:14"),
            )
        )
        assertEquals("11:30", hasil)
    }

    @Test
    fun `belum ada transaksi berarti tidak ada jam terakhir`() {
        assertNull(jamTransaksiTerakhir(emptyList()))
    }

    @Test
    fun `rata-rata transaksi nol saat belum ada transaksi, bukan pembagian nol`() {
        val kosong = RingkasanLeader.KOSONG
        assertEquals(0L, kosong.rataRataTransaksi)
    }

    @Test
    fun `rata-rata membagi omzet dengan jumlah transaksi`() {
        val data = RingkasanLeader.KOSONG.copy(omzetHariIni = 300_000, jumlahTransaksi = 4)
        assertEquals(75_000L, data.rataRataTransaksi)
    }

    @Test
    fun `saldo di bawah ambang dan saldo minus sama-sama kritis`() {
        assertTrue(RingkasanLeader.KOSONG.copy(sisaPettyCash = 149_999).pettyCashKritis)
        assertTrue(RingkasanLeader.KOSONG.copy(sisaPettyCash = -1).pettyCashKritis)
    }

    @Test
    fun `saldo tepat di ambang belum kritis`() {
        val data = RingkasanLeader.KOSONG.copy(
            sisaPettyCash = RingkasanLeader.AMBANG_PETTY_CASH_KRITIS,
        )
        assertTrue(!data.pettyCashKritis)
    }

    @Test
    fun `judul memakai nama cabang saat leader memegang satu outlet`() {
        val data = RingkasanLeader.KOSONG.copy(namaOutletUtama = "Cabang Empang", jumlahCabang = 1)
        assertEquals("Cabang Empang", data.judul)
    }

    @Test
    fun `judul menyebut cacah saat leader membina lebih dari satu cabang`() {
        val data = RingkasanLeader.KOSONG.copy(namaOutletUtama = "Cabang Empang", jumlahCabang = 3)
        assertEquals("3 Cabang Binaan", data.judul)
    }
}
