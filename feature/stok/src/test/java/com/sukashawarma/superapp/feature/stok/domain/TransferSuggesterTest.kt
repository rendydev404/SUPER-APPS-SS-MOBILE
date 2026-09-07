package com.sukashawarma.superapp.feature.stok.domain

import com.sukashawarma.superapp.feature.stok.data.model.MonitoringRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TransferSuggesterTest {

    /** Faktor 1 supaya angka uji terbaca langsung sebagai satuan terkecil. */
    private val meta = UnitMeta(satuan = "pcs", satuanKecil = "pcs", faktorTampilan = 1.0)

    private fun baris(outlet: String, saldo: Double, threshold: Double = 100.0) = MonitoringRow(
        outletId = outlet,
        outletName = "Outlet $outlet",
        bahanBakuId = "bahan-1",
        itemName = "Roti Pita",
        currentQty = saldo,
        threshold = threshold,
        statusView = null,
        isFlagged = false,
        saldoIsGram = true,
        lastOpnameDate = null,
        kategori = null,
        satuan = "pcs",
        meta = meta,
    )

    @Test
    fun `surplus dialokasikan ke outlet yang kekurangan`() {
        val saran = TransferSuggester.untukBahan(
            listOf(baris("A", 180.0), baris("B", 60.0)),
        )
        assertEquals(1, saran.size)
        assertEquals("A", saran.first().dariOutletId)
        assertEquals("B", saran.first().keOutletId)
        assertEquals(40.0, saran.first().qtyNorm, 0.0001)
    }

    @Test
    fun `donor tidak pernah didorong ke bawah threshold sendiri`() {
        // A hanya punya surplus 20; kebutuhan B adalah 70. Yang disarankan maksimal 20.
        val saran = TransferSuggester.untukBahan(
            listOf(baris("A", 120.0), baris("B", 30.0)),
        )
        assertEquals(20.0, saran.sumOf { it.qtyNorm }, 0.0001)
    }

    @Test
    fun `tanpa donor tidak ada saran`() {
        val saran = TransferSuggester.untukBahan(
            listOf(baris("A", 40.0), baris("B", 30.0)),
        )
        assertTrue(saran.isEmpty())
    }

    @Test
    fun `tanpa penerima tidak ada saran`() {
        val saran = TransferSuggester.untukBahan(
            listOf(baris("A", 400.0), baris("B", 300.0)),
        )
        assertTrue(saran.isEmpty())
    }

    @Test
    fun `baris dengan faktor tidak dapat dipercaya dikeluarkan dari perhitungan`() {
        val rusak = baris("C", 500.0).copy(
            saldoIsGram = false,
            meta = UnitMeta(faktorTampilan = null),
        )
        val saran = TransferSuggester.untukBahan(listOf(rusak, baris("B", 30.0)))
        assertTrue(saran.isEmpty())
    }

    @Test
    fun `satu outlet saja tidak menghasilkan saran`() {
        assertTrue(TransferSuggester.untukBahan(listOf(baris("A", 10.0))).isEmpty())
    }

    /**
     * Cermin urutan penerima di `transferSuggestion.ts`: `below` sebelum `warning`.
     *
     * Sebelum perbaikan ini native hanya mengurutkan dari kekurangan terbesar, sehingga
     * outlet yang cuma menipis bisa menghabiskan surplus donor lebih dulu.
     */
    @Test
    fun `outlet kritis dilayani sebelum outlet yang sekadar menipis`() {
        // Threshold 100. C tinggal 10 (kritis, di bawah separuh threshold) dan butuh 90.
        // B tinggal 55 (menipis) dan butuh 45. Donor A hanya punya surplus 50.
        val saran = TransferSuggester.untukBahan(
            listOf(baris("A", 150.0), baris("B", 55.0), baris("C", 10.0)),
        )
        assertEquals("C", saran.first().keOutletId)
        assertEquals(50.0, saran.first().qtyNorm, 0.0001)
        // Surplus habis di C, jadi B tidak kebagian sama sekali.
        assertEquals(1, saran.size)
    }

    /**
     * Donor yang dilewati untuk satu penerima harus tetap tersedia bagi penerima
     * berikutnya. Versi lama memakai satu penunjuk yang hanya maju, sehingga donor
     * bisa hilang di tengah jalan.
     */
    @Test
    fun `sisa surplus donor tetap terpakai untuk penerima berikutnya`() {
        // A surplus 100. Dua penerima masing-masing butuh 40 dan 30 — keduanya
        // harus terlayani dari donor yang sama.
        val saran = TransferSuggester.untukBahan(
            listOf(baris("A", 200.0), baris("B", 60.0), baris("C", 70.0)),
        )
        assertEquals(2, saran.size)
        assertTrue(saran.all { it.dariOutletId == "A" })
        assertEquals(70.0, saran.sumOf { it.qtyNorm }, 0.0001)
    }
}
