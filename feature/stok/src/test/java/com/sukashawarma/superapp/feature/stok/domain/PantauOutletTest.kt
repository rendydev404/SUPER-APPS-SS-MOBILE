package com.sukashawarma.superapp.feature.stok.domain

import com.sukashawarma.superapp.domain.model.Role
import com.sukashawarma.superapp.feature.stok.data.model.MonitoringRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PantauOutletTest {

    private val meta = UnitMeta(
        satuan = "Kg", satuanTengah = null, satuanKecil = "gr",
        faktorTengah = null, faktorTampilan = 1000.0,
    )

    private fun row(
        outletId: String,
        outletName: String,
        bahan: String,
        qty: Double,
        threshold: Double = 10.0,
        saldoIsGram: Boolean = false,
    ) = MonitoringRow(
        outletId = outletId, outletName = outletName, bahanBakuId = bahan, itemName = bahan,
        currentQty = qty, threshold = threshold, statusView = null, isFlagged = false,
        saldoIsGram = saldoIsGram, lastOpnameDate = null, kategori = null, satuan = "Kg", meta = meta,
    )

    /** Daftar master diturunkan dari baris — semua outlet dalam fixture dianggap aktif. */
    private fun susunDari(baris: List<MonitoringRow>) =
        PantauOutlet.susun(baris, baris.map { it.outletId to it.outletName }.distinct())

    /**
     * Regresi data produksi 24 Sep 2026: JATIASIH sudah nonaktif tetapi saldonya masih
     * ada di view. Tanpa penyaringan, outlet itu muncul sebagai kartu "Outlet" kritis.
     */
    @Test
    fun `saldo outlet nonaktif diabaikan`() {
        val papan = PantauOutlet.susun(
            baris = listOf(
                row("a", "SUKA SHAWARMA BEJI", "Ayam", qty = 50.0),
                row("x", "Outlet", "Ayam", qty = 0.0),
            ),
            outlets = listOf("a" to "SUKA SHAWARMA BEJI"),
        )
        assertEquals(listOf("a"), papan.outlets.map { it.outletId })
        assertEquals(0, papan.jumlahKritis)
    }

    @Test
    fun `status outlet mengikuti bahan terparah`() {
        val papan = PantauOutlet.susun(
            baris = listOf(
                row("a", "SUKA SHAWARMA BEJI", "Ayam", qty = 2.0),     // < 5 -> kritis
                row("a", "SUKA SHAWARMA BEJI", "Roti", qty = 8.0),     // < 10 -> menipis
                row("b", "MITRA CIBINONG", "Ayam", qty = 8.0),         // menipis
                row("c", "SUKA SHAWARMA DRAMAGA", "Ayam", qty = 50.0), // aman
            ),
            outlets = listOf("a" to "SUKA SHAWARMA BEJI", "b" to "MITRA CIBINONG", "c" to "SUKA SHAWARMA DRAMAGA"),
        )
        val beji = papan.outlets.first { it.outletId == "a" }
        assertEquals(StokStatus.BELOW, beji.status)
        assertEquals(1, beji.jumlahKritis)
        assertEquals(1, beji.jumlahMenipis)
        assertEquals(listOf("Ayam", "Roti"), beji.bahanRendah.map { it.itemName })
        assertEquals(1, papan.jumlahKritis)
        assertEquals(1, papan.jumlahMenipis)
        assertEquals(1, papan.jumlahAman)
    }

    /**
     * Inti divergensi native: saldo gram-scale tidak boleh dibandingkan mentah dengan
     * threshold satuan besar. 3000 gr dengan min 10 Kg = kritis, bukan aman.
     */
    @Test
    fun `saldo gram dinormalisasi sebelum dibanding threshold`() {
        val papan = PantauOutlet.susun(
            baris = listOf(row("a", "SUKA SHAWARMA BEJI", "Ayam", qty = 3000.0, saldoIsGram = true)),
            outlets = listOf("a" to "SUKA SHAWARMA BEJI"),
        )
        assertEquals(StokStatus.BELOW, papan.outlets.single().status)
    }

    @Test
    fun `gudang pusat jadi hub dan kantor serta outlet uji disaring`() {
        val papan = susunDari(
            listOf(
                row("g", "GUDANG PUSAT (HQ)", "Ayam", qty = 1.0),
                row("k", "KANTOR PUSAT", "Ayam", qty = 1.0),
                row("t", "outlet tes", "Ayam", qty = 1.0),
                row("s", "GLOBAL OUTLET (SYSTEM)", "Ayam", qty = 1.0),
                row("a", "SUKA SHAWARMA BEJI", "Ayam", qty = 50.0),
            ),
        )
        assertEquals("g", papan.hub?.outletId)
        assertEquals(listOf("a"), papan.outlets.map { it.outletId })
        // Hub tidak ikut hitungan maupun prioritas lintas outlet.
        assertEquals(0, papan.jumlahKritis)
        assertTrue(papan.prioritas.isEmpty())
    }

    @Test
    fun `outlet tanpa baris saldo tetap tampil sebagai aman`() {
        val papan = PantauOutlet.susun(emptyList(), listOf("a" to "SUKA SHAWARMA BEJI"))
        val o = papan.outlets.single()
        assertEquals(StokStatus.OK, o.status)
        assertEquals(0, o.jumlahBahan)
        assertEquals(100, o.persenSehat)
        assertNull(papan.hub)
    }

    @Test
    fun `prioritas tiga bahan paling kritis lintas outlet`() {
        val papan = susunDari(
            listOf(
                row("a", "SUKA SHAWARMA BEJI", "A", qty = 4.0),
                row("a", "SUKA SHAWARMA BEJI", "B", qty = 1.0),
                row("b", "MITRA CIBINONG", "C", qty = 0.5),
                row("b", "MITRA CIBINONG", "D", qty = 3.0),
                row("b", "MITRA CIBINONG", "E", qty = 9.0), // menipis, bukan kandidat
            ),
        )
        assertEquals(listOf("C", "B", "D"), papan.prioritas.map { it.itemName })
    }

    @Test
    fun `urutan grid per wilayah lalu status`() {
        val papan = susunDari(
            listOf(
                row("d", "SUKA SHAWARMA BEJI", "A", qty = 50.0),        // Depok, aman
                row("b1", "SUKA SHAWARMA DRAMAGA", "A", qty = 50.0),    // Bogor, aman
                row("b2", "SUKA SHAWARMA EMPANG", "A", qty = 1.0),      // Bogor, kritis
            ),
        )
        assertEquals(listOf("b2", "b1", "d"), papan.outlets.map { it.outletId })
    }

    @Test
    fun `wilayah dan nama pendek`() {
        assertEquals(WilayahOutlet.BOGOR, PantauOutlet.wilayah("SUKA SHAWARMA BNR"))
        assertEquals(WilayahOutlet.BEKASI, PantauOutlet.wilayah("MITRA PEKAYON"))
        assertEquals(WilayahOutlet.TANGERANG, PantauOutlet.wilayah("SUKA SHAWARMA CIRENDEU"))
        assertEquals(WilayahOutlet.DEPOK, PantauOutlet.wilayah("SUKA SHAWARMA DEPOK SUKMAJAYA"))
        assertEquals(WilayahOutlet.JAKARTA, PantauOutlet.wilayah("SUKA SHAWARMA JAGAKARSA"))
        assertEquals(WilayahOutlet.PUSAT, PantauOutlet.wilayah("GUDANG SS ONLINE"))
        assertEquals("BEJI", PantauOutlet.namaPendek("SUKA SHAWARMA BEJI"))
        assertEquals("CIBINONG", PantauOutlet.namaPendek("MITRA CIBINONG"))
    }

    @Test
    fun `saring status dan cari nama`() {
        val papan = susunDari(
            listOf(
                row("a", "SUKA SHAWARMA BEJI", "A", qty = 1.0),
                row("b", "MITRA CIBINONG", "A", qty = 50.0),
            ),
        )
        assertEquals(listOf("a"), PantauOutlet.saring(papan.outlets, FilterPantau.KRITIS, "").map { it.outletId })
        assertEquals(listOf("b"), PantauOutlet.saring(papan.outlets, FilterPantau.SEMUA, "cibi").map { it.outletId })
        assertTrue(PantauOutlet.saring(papan.outlets, FilterPantau.MENIPIS, "").isEmpty())
    }

    @Test
    fun `hanya kitchen yang mendapat papan pantau semua outlet`() {
        assertTrue(StokAkses.melihatPantauSemuaOutlet(Role.KITCHEN))
        listOf(Role.CREW, Role.LEADER, Role.AREA_MANAGER, Role.ADMIN, Role.OWNER, null).forEach {
            assertFalse("$it tidak boleh", StokAkses.melihatPantauSemuaOutlet(it))
        }
    }
}
