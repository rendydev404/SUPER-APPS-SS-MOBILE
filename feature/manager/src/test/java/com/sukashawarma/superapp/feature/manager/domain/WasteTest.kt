package com.sukashawarma.superapp.feature.manager.domain

import com.sukashawarma.superapp.domain.model.Role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class WasteTest {

    private fun laporan(
        id: String = "w1",
        bahanId: String = "b1",
        bahanNama: String = "Daging Shawarma",
        satuan: String = "Kg",
        qty: Double = 2.0,
        nilai: Long = 24_000,
        status: StatusWaste = StatusWaste.MENUNGGU,
        pelaporId: String? = "kru-1",
    ) = LaporanWaste(
        id = id,
        outletId = "o1",
        outletNama = "SS EMPANG",
        bahanId = bahanId,
        bahanNama = bahanNama,
        satuan = satuan,
        qty = qty,
        hargaBeli = 12_000.0,
        nilai = nilai,
        alasan = "Kedaluwarsa",
        fotoUrl = null,
        status = status,
        alasanPenolakan = null,
        pelaporId = pelaporId,
        pelaporNama = "Kru Outlet",
        penyetujuNama = null,
        dibuatPada = "2026-09-05T01:00:00+00:00",
    )

    @Test
    fun `status dipetakan dari nilai huruf besar di database`() {
        assertEquals(StatusWaste.MENUNGGU, StatusWaste.dari("PENDING"))
        assertEquals(StatusWaste.DISETUJUI, StatusWaste.dari("APPROVED"))
        assertEquals(StatusWaste.DITOLAK, StatusWaste.dari("REJECTED"))
        // Huruf kecil bukan nilai yang tersimpan; jangan diam-diam diterima.
        assertNull(StatusWaste.dari("pending"))
        assertNull(StatusWaste.dari(null))
    }

    @Test
    fun `nilai waste dibulatkan per baris`() {
        assertEquals(30_000L, nilaiWaste(2.5, 12_000.0))
        assertEquals(1L, nilaiWaste(0.5, 1.0))
        assertEquals(0L, nilaiWaste(3.0, 0.0))
    }

    @Test
    fun `qty bulat ditampilkan tanpa koma`() {
        assertEquals("2", laporan(qty = 2.0).qtyTeks)
        assertEquals("2.5", laporan(qty = 2.5).qtyTeks)
    }

    @Test
    fun `ringkasan menjumlahkan nilai dan menghitung insiden`() {
        val r = susunRingkasanWaste(
            listOf(
                laporan(id = "w1", nilai = 24_000),
                laporan(id = "w2", nilai = 6_000),
            ),
            jumlahMenunggu = 3,
        )
        assertEquals(30_000L, r.totalNilai)
        assertEquals(2, r.totalInsiden)
        assertEquals(3, r.jumlahMenunggu)
    }

    @Test
    fun `bahan digabung berdasarkan id lalu diurutkan dari nilai terbesar`() {
        val r = susunRingkasanWaste(
            listOf(
                laporan(id = "w1", bahanId = "b1", bahanNama = "Daging", qty = 1.0, nilai = 10_000),
                laporan(id = "w2", bahanId = "b2", bahanNama = "Roti", qty = 5.0, nilai = 50_000),
                laporan(id = "w3", bahanId = "b1", bahanNama = "Daging", qty = 2.0, nilai = 20_000),
            ),
            jumlahMenunggu = 0,
        )
        assertEquals(listOf("Roti", "Daging"), r.bahanTeratas.map { it.nama })
        val daging = r.bahanTeratas.single { it.nama == "Daging" }
        assertEquals(3.0, daging.qty, 0.001)
        assertEquals(30_000L, daging.nilai)
    }

    /** Dua bahan boleh bernama mirip; menggabungkannya lewat nama menyatukan yang berbeda. */
    @Test
    fun `bahan bernama sama tapi id berbeda tidak digabung`() {
        val r = susunRingkasanWaste(
            listOf(
                laporan(id = "w1", bahanId = "b1", bahanNama = "Saus", nilai = 5_000),
                laporan(id = "w2", bahanId = "b2", bahanNama = "Saus", nilai = 3_000),
            ),
            jumlahMenunggu = 0,
        )
        assertEquals(2, r.bahanTeratas.size)
    }

    @Test
    fun `bahan teratas dibatasi lima baris`() {
        val banyak = (1..8).map { laporan(id = "w$it", bahanId = "b$it", nilai = it * 1_000L) }
        assertEquals(5, susunRingkasanWaste(banyak, 0).bahanTeratas.size)
    }

    @Test
    fun `manajer boleh memproses laporan menunggu milik orang lain`() {
        assertNull(halanganMemproses(Role.AREA_MANAGER, "am-1", laporan()))
        assertNull(halanganMemproses(Role.REGIONAL_MANAGER, "rm-1", laporan()))
    }

    @Test
    fun `role di luar daftar ditolak`() {
        assertNotNull(halanganMemproses(Role.CREW, "kru-2", laporan()))
        assertNotNull(halanganMemproses(Role.LEADER, "l-1", laporan()))
        assertNotNull(halanganMemproses(null, null, laporan()))
    }

    @Test
    fun `laporan yang sudah diproses tidak bisa diproses lagi`() {
        assertNotNull(halanganMemproses(Role.AREA_MANAGER, "am-1", laporan(status = StatusWaste.DISETUJUI)))
        assertNotNull(halanganMemproses(Role.AREA_MANAGER, "am-1", laporan(status = StatusWaste.DITOLAK)))
    }

    @Test
    fun `pelapor tidak boleh menyetujui laporannya sendiri`() {
        assertNotNull(halanganMemproses(Role.AREA_MANAGER, "kru-1", laporan(pelaporId = "kru-1")))
    }

    /** Pelapor tak dikenal tidak boleh diperlakukan sebagai "sama dengan pengguna". */
    @Test
    fun `pelapor null tidak memblokir pemrosesan`() {
        assertNull(halanganMemproses(Role.AREA_MANAGER, null, laporan(pelaporId = null)))
    }

    @Test
    fun `alasan penolakan wajib minimal tiga karakter`() {
        assertNotNull(validasiAlasanPenolakan(""))
        assertNotNull(validasiAlasanPenolakan("  "))
        assertNotNull(validasiAlasanPenolakan("ok"))
        assertNull(validasiAlasanPenolakan("foto tidak jelas"))
        // Spasi di ujung tidak boleh dihitung sebagai isi.
        assertNotNull(validasiAlasanPenolakan(" a "))
    }
}
