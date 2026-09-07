package com.sukashawarma.superapp.feature.manager.domain

import com.sukashawarma.superapp.domain.model.Role
import com.sukashawarma.superapp.feature.manager.data.model.AbsenMasuk
import com.sukashawarma.superapp.feature.manager.data.model.OutletRingkas
import com.sukashawarma.superapp.feature.manager.data.model.PesananRingkas
import com.sukashawarma.superapp.feature.manager.data.model.WasteDisetujui
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RingkasanAreaTest {

    private val empang = OutletRingkas("o1", "SS EMPANG", aktif = true)
    private val sentul = OutletRingkas("o2", "SS SENTUL", aktif = true)
    private val pekayon = OutletRingkas("o3", "SS PEKAYON", aktif = true)

    private fun susun(
        outlets: List<OutletRingkas> = listOf(empang, sentul, pekayon),
        pesanan: List<PesananRingkas> = emptyList(),
        pesananSebelumnya: List<PesananRingkas> = emptyList(),
        absenMasuk: List<AbsenMasuk> = emptyList(),
        pemetaanAm: Map<String, String> = emptyMap(),
        kerugianWaste: Long = 0,
        wastePending: Int = 0,
        role: Role? = Role.REGIONAL_MANAGER,
        nama: String? = "Rina",
    ) = susunRingkasanArea(
        outlets, pesanan, pesananSebelumnya, absenMasuk, pemetaanAm,
        kerugianWaste, wastePending, role, nama,
    )

    @Test
    fun `omzet transaksi dan porsi dijumlahkan dari pesanan selesai`() {
        val r = susun(
            pesanan = listOf(
                PesananRingkas("o1", 50_000, 3),
                PesananRingkas("o1", 25_000, 1),
                PesananRingkas("o2", 30_000, 2),
            ),
        )
        assertEquals(105_000L, r.omzet)
        assertEquals(3, r.jumlahTransaksi)
        assertEquals(6, r.jumlahItem)
    }

    @Test
    fun `perubahan omzet dihitung terhadap periode pembanding`() {
        val r = susun(
            pesanan = listOf(PesananRingkas("o1", 120_000, 1)),
            pesananSebelumnya = listOf(PesananRingkas("o1", 100_000, 1)),
        )
        assertTrue(r.perubahanOmzet.naik)
        assertEquals("20.0", r.perubahanOmzet.besaranTeks)
    }

    @Test
    fun `omzet turun ditandai tidak naik`() {
        val r = susun(
            pesanan = listOf(PesananRingkas("o1", 80_000, 1)),
            pesananSebelumnya = listOf(PesananRingkas("o1", 100_000, 1)),
        )
        assertTrue(!r.perubahanOmzet.naik)
        assertEquals("20.0", r.perubahanOmzet.besaranTeks)
    }

    /** Outlet yang baru buka pernah menampilkan "Infinity%" di web sebelum guard ini ada. */
    @Test
    fun `pembanding nol tidak menghasilkan tak hingga`() {
        assertEquals(100.0, hitungPerubahan(50_000, 0).persen, 0.001)
        assertEquals(0.0, hitungPerubahan(0, 0).persen, 0.001)
    }

    @Test
    fun `bonus manajer lima puluh rupiah per porsi`() {
        val r = susun(pesanan = listOf(PesananRingkas("o1", 10_000, 40)))
        assertEquals(2_000L, r.estimasiBonus)
    }

    @Test
    fun `tarif bonus leader dan crew dua kali lipat manajer`() {
        assertEquals(50L, tarifBonus(Role.REGIONAL_MANAGER))
        assertEquals(50L, tarifBonus(Role.AREA_MANAGER))
        assertEquals(100L, tarifBonus(Role.LEADER))
        assertEquals(100L, tarifBonus(Role.CREW))
    }

    @Test
    fun `peringkat diurutkan dari omzet terbesar dan menyimpan omzet tertinggi`() {
        val r = susun(
            pesanan = listOf(
                PesananRingkas("o2", 90_000, 1),
                PesananRingkas("o1", 40_000, 1),
            ),
        )
        assertEquals(listOf("SS SENTUL", "SS EMPANG", "SS PEKAYON"), r.peringkat.map { it.nama })
        assertEquals(90_000L, r.omzetTertinggi)
    }

    @Test
    fun `outlet nonaktif tidak masuk peringkat tapi tetap terlihat di status outlet`() {
        val tutupPermanen = OutletRingkas("o4", "SS CIBUBUR", aktif = false)
        val r = susun(outlets = listOf(empang, tutupPermanen))
        assertEquals(listOf("SS EMPANG"), r.peringkat.map { it.nama })
        // Daftar status tetap alfabetis; nonaktif tidak digeser ke bawah.
        assertEquals(listOf("SS CIBUBUR", "SS EMPANG"), r.statusOutlet.map { it.nama })
        assertEquals(2, r.jumlahCabang)
    }

    @Test
    fun `outlet mitra selalu berada di bawah daftar status`() {
        val mitra = OutletRingkas("o5", "MITRA BOGOR", aktif = true)
        val r = susun(outlets = listOf(mitra, empang, sentul), pemetaanAm = mapOf("o5" to "Muchtar"))
        assertEquals(listOf("SS EMPANG", "SS SENTUL", "MITRA BOGOR"), r.statusOutlet.map { it.nama })
    }

    @Test
    fun `outlet dengan zona penampung dibuang dari peringkat dan status`() {
        val takDikenal = OutletRingkas("o9", "SS SURABAYA", aktif = true)
        val r = susun(outlets = listOf(empang, takDikenal))
        assertEquals(listOf("SS EMPANG"), r.peringkat.map { it.nama })
        assertEquals(listOf("SS EMPANG"), r.statusOutlet.map { it.nama })
        // Header "Status Outlet" tetap melaporkan seluruh cabang yang terbaca.
        assertEquals(2, r.jumlahCabang)
    }

    @Test
    fun `zona dikelompokkan per area manager dan diurutkan dari omzet terbesar`() {
        val r = susun(
            pesanan = listOf(
                PesananRingkas("o1", 10_000, 1), // Abu Bakar
                PesananRingkas("o2", 70_000, 1), // Muchtar
                PesananRingkas("o3", 20_000, 1), // Mulyadi
            ),
        )
        assertEquals(listOf("Muchtar", "Mulyadi", "Abu Bakar"), r.zona.map { it.zona })
        assertEquals(70_000L, r.zonaTertinggi?.totalOmzet)
        assertEquals(100_000L, r.totalOmzetSemuaZona)
        assertEquals(3, r.jumlahOutletDalamZona)
        assertEquals(33_333L, r.rataRataOmzetPerZona)
    }

    @Test
    fun `area manager melihat seluruh binaannya sebagai satu zona bernama dirinya`() {
        val r = susun(role = Role.AREA_MANAGER, nama = "Mulyadi", pesanan = listOf(PesananRingkas("o1", 5_000, 1)))
        assertEquals(listOf("Mulyadi"), r.zona.map { it.zona })
        assertEquals(3, r.zona.single().outlets.size)
    }

    @Test
    fun `outlet dengan absen masuk ditandai buka beserta jamnya`() {
        val r = susun(absenMasuk = listOf(AbsenMasuk("o1", "08.15")))
        val status = r.statusOutlet.associateBy { it.nama }
        assertEquals("08.15", status.getValue("SS EMPANG").jamBuka)
        assertNull(status.getValue("SS SENTUL").jamBuka)
    }

    @Test
    fun `kerugian waste dihitung per baris dari harga beli bahan`() {
        val rugi = hitungKerugianWaste(
            listOf(WasteDisetujui("b1", 2.5), WasteDisetujui("b2", 1.0)),
            mapOf("b1" to 12_000.0, "b2" to 7_500.0),
        )
        assertEquals(37_500L, rugi)
    }

    @Test
    fun `bahan tanpa harga tercatat dihitung nol bukan membuat total gagal`() {
        val rugi = hitungKerugianWaste(
            listOf(WasteDisetujui("b1", 2.0), WasteDisetujui("tanpa-harga", 99.0)),
            mapOf("b1" to 1_000.0),
        )
        assertEquals(2_000L, rugi)
    }

    @Test
    fun `ringkasan kosong tidak membagi dengan nol saat menghitung rata-rata zona`() {
        assertEquals(0L, RingkasanArea.KOSONG.rataRataOmzetPerZona)
        assertNull(RingkasanArea.KOSONG.zonaTertinggi)
    }
}
