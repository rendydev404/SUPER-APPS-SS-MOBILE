package com.sukashawarma.superapp.feature.manager.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MonitoringTest {

    private val kamis = LocalDate.of(2026, 9, 3)
    private val empang = OutletMonitoring("o1", "SUKA SHAWARMA EMPANG", "Bogor")
    private val sentul = OutletMonitoring("o2", "SUKA SHAWARMA SENTUL", null)
    private val outlets = listOf(empang, sentul)

    private val leader = StafMonitoring("s1", "Budi", "leader", "o1")
    private val kru = StafMonitoring("s2", "Ani", "crew", "o1")
    private val spv = StafMonitoring("s3", "Citra", "spv", null)

    private fun absen(
        outletId: String = "o1",
        stafId: String = "s1",
        tipe: String = "in",
        tanggal: LocalDate? = kamis,
        jam: String = "08.00",
        ts: String = "2026-09-03T01:00:00+00:00",
    ) = AbsenBaris(outletId, stafId, tipe, tanggal, jam, ts)

    private fun kartu(
        staf: List<StafMonitoring> = listOf(leader, kru),
        absensi: List<AbsenBaris> = emptyList(),
        opname: List<OpnameBaris> = emptyList(),
        wajib: List<String> = emptyList(),
        dicentang: Set<String> = emptySet(),
    ) = susunKartuMonitoring(empang, kamis, staf, absensi, opname, outlets, wajib, dicentang)

    /* ---------------- nama outlet ---------------- */

    @Test
    fun `awalan nama jaringan dibuang dari nama pendek`() {
        assertEquals("EMPANG", empang.namaPendek)
        assertEquals("BOGOR", OutletMonitoring("x", "MITRA BOGOR", null).namaPendek)
        assertEquals("SS CIBUBUR", OutletMonitoring("x", "SS CIBUBUR", null).namaPendek)
    }

    @Test
    fun `outlet tanpa region masuk kelompok penampung`() {
        assertEquals("Bogor", empang.wilayah)
        assertEquals("Lain-lain / Unassigned", sentul.wilayah)
        assertEquals("Lain-lain / Unassigned", OutletMonitoring("x", "X", "  ").wilayah)
    }

    /* ---------------- status POS ---------------- */

    @Test
    fun `tanpa absen POS terkunci menunggu absen`() {
        assertEquals(StatusPos.MenungguAbsen, kartu().statusPos)
        assertFalse(kartu().statusPos.terbuka)
    }

    @Test
    fun `ada yang masuk dan tanpa checklist wajib membuat POS terbuka`() {
        val k = kartu(absensi = listOf(absen()))
        assertEquals(StatusPos.Terbuka, k.statusPos)
        assertTrue(k.statusPos.terbuka)
        assertEquals("08.00", k.jamBuka)
    }

    @Test
    fun `checklist wajib yang belum tuntas mengunci POS beserta kemajuannya`() {
        val k = kartu(
            absensi = listOf(absen()),
            wajib = listOf("i1", "i2", "i3"),
            dicentang = setOf("i1"),
        )
        assertEquals(StatusPos.ChecklistBelumSelesai(1, 3), k.statusPos)
        assertEquals("Terkunci - Checklist Belum Selesai (1/3)", k.statusPos.label)
    }

    @Test
    fun `checklist tuntas membuka POS`() {
        val k = kartu(
            absensi = listOf(absen()),
            wajib = listOf("i1", "i2"),
            dicentang = setOf("i1", "i2"),
        )
        assertEquals(StatusPos.Terbuka, k.statusPos)
    }

    /**
     * Urutan pemeriksaan penting: outlet yang sudah tutup tidak boleh dilaporkan
     * terbuka hanya karena tadi pagi ada yang absen masuk.
     */
    @Test
    fun `semua kru pulang mengunci POS meski checklist sudah tuntas`() {
        val k = kartu(
            absensi = listOf(
                absen(stafId = "s1", tipe = "in", ts = "2026-09-03T01:00:00+00:00"),
                absen(stafId = "s1", tipe = "out", jam = "21.30", ts = "2026-09-03T14:30:00+00:00"),
            ),
            wajib = listOf("i1"),
            dicentang = setOf("i1"),
        )
        assertEquals(StatusPos.Tutup("21.30"), k.statusPos)
        assertEquals("Terkunci - Tutup Shift (21.30)", k.statusPos.label)
    }

    @Test
    fun `satu kru pulang tapi yang lain masih masuk tidak menutup POS`() {
        val k = kartu(
            absensi = listOf(
                absen(stafId = "s1", tipe = "out", ts = "2026-09-03T14:00:00+00:00"),
                absen(stafId = "s2", tipe = "in", ts = "2026-09-03T01:00:00+00:00"),
            ),
        )
        assertEquals(StatusPos.Terbuka, k.statusPos)
    }

    @Test
    fun `jam buka adalah absen masuk paling awal dan jam tutup absen pulang paling akhir`() {
        val k = kartu(
            absensi = listOf(
                absen(stafId = "s2", tipe = "in", jam = "09.30", ts = "2026-09-03T02:30:00+00:00"),
                absen(stafId = "s1", tipe = "in", jam = "07.15", ts = "2026-09-03T00:15:00+00:00"),
                absen(stafId = "s1", tipe = "out", jam = "20.00", ts = "2026-09-03T13:00:00+00:00"),
                absen(stafId = "s2", tipe = "out", jam = "22.10", ts = "2026-09-03T15:10:00+00:00"),
            ),
        )
        assertEquals("07.15", k.jamBuka)
        assertEquals("22.10", k.jamTutup)
    }

    @Test
    fun `absen hari lain tidak ikut dihitung`() {
        val k = kartu(absensi = listOf(absen(tanggal = kamis.minusDays(1))))
        assertEquals(StatusPos.MenungguAbsen, k.statusPos)
        assertNull(k.jamBuka)
    }

    /* ---------------- opname ---------------- */

    @Test
    fun `opname hari itu ditampilkan jamnya`() {
        val k = kartu(opname = listOf(OpnameBaris("o1", kamis, "06.45")))
        assertEquals("06.45", k.jamOpname)
    }

    @Test
    fun `opname outlet lain atau hari lain tidak diambil`() {
        assertNull(kartu(opname = listOf(OpnameBaris("o2", kamis, "06.45"))).jamOpname)
        assertNull(kartu(opname = listOf(OpnameBaris("o1", kamis.minusDays(1), "06.45"))).jamOpname)
    }

    /* ---------------- status kru ---------------- */

    @Test
    fun `kru tanpa absen ditandai belum absen`() {
        val k = kartu()
        assertEquals(StatusAbsen.BelumAbsen, k.kru.single { it.id == "s1" }.status)
    }

    @Test
    fun `kru dengan absen masuk di outlet ini ditandai hadir`() {
        val k = kartu(absensi = listOf(absen(stafId = "s1", jam = "08.05")))
        val budi = k.kru.single { it.id == "s1" }
        assertEquals(StatusAbsen.Hadir, budi.status)
        assertEquals("08.05", budi.jam)
    }

    /** Leader yang berpindah cabang harus tetap terlihat, bukan dilaporkan bolos. */
    @Test
    fun `absen di cabang lain ditandai beserta nama cabangnya`() {
        val k = kartu(absensi = listOf(absen(outletId = "o2", stafId = "s1", jam = "08.20")))
        val budi = k.kru.single { it.id == "s1" }
        assertEquals(StatusAbsen.HadirDiOutletLain("SENTUL"), budi.status)
        assertEquals("Hadir di SENTUL", budi.status.label)
        assertTrue(budi.status.diCabangLain)
    }

    @Test
    fun `absen di outlet ini menang atas absen di cabang lain`() {
        val k = kartu(
            absensi = listOf(
                absen(outletId = "o2", stafId = "s1", jam = "08.20", ts = "2026-09-03T01:20:00+00:00"),
                absen(outletId = "o1", stafId = "s1", jam = "10.00", ts = "2026-09-03T03:00:00+00:00"),
            ),
        )
        assertEquals(StatusAbsen.Hadir, k.kru.single { it.id == "s1" }.status)
    }

    @Test
    fun `ketukan terakhir yang menentukan status di outlet ini`() {
        val k = kartu(
            absensi = listOf(
                absen(stafId = "s1", tipe = "in", ts = "2026-09-03T01:00:00+00:00"),
                absen(stafId = "s1", tipe = "out", jam = "17.00", ts = "2026-09-03T10:00:00+00:00"),
            ),
        )
        val budi = k.kru.single { it.id == "s1" }
        assertEquals(StatusAbsen.Pulang, budi.status)
        assertEquals("17.00", budi.jam)
    }

    /* ---------------- urutan & keanggotaan ---------------- */

    @Test
    fun `kru diurutkan spv lalu leader lalu crew lalu alfabetis`() {
        val k = susunKartuMonitoring(
            empang, kamis,
            listOf(kru, spv, leader, StafMonitoring("s4", "Adi", "crew", "o1")),
            emptyList(), emptyList(), outlets, emptyList(), emptySet(),
        )
        assertEquals(listOf("Citra", "Budi", "Adi", "Ani"), k.kru.map { it.nama })
    }

    @Test
    fun `spv muncul di semua outlet sedangkan kru hanya di outlet induknya`() {
        val semua = listOf(leader, kru, spv, StafMonitoring("s5", "Dewi", "crew", "o2"))
        assertEquals(listOf("Budi", "Ani", "Citra"), stafUntukOutlet(semua, "o1").map { it.nama })
        assertEquals(listOf("Citra", "Dewi"), stafUntukOutlet(semua, "o2").map { it.nama })
    }

    /* ---------------- penyaring ---------------- */

    @Test
    fun `penyaring status POS memilah terbuka dan terkunci`() {
        assertTrue(lolosFilterPos(StatusPos.Terbuka, FilterStatusPos.SEMUA))
        assertTrue(lolosFilterPos(StatusPos.Terbuka, FilterStatusPos.TERBUKA))
        assertFalse(lolosFilterPos(StatusPos.Terbuka, FilterStatusPos.TERKUNCI))
        assertTrue(lolosFilterPos(StatusPos.MenungguAbsen, FilterStatusPos.TERKUNCI))
        assertTrue(lolosFilterPos(StatusPos.Tutup("20.00"), FilterStatusPos.TERKUNCI))
        assertTrue(lolosFilterPos(StatusPos.ChecklistBelumSelesai(1, 2), FilterStatusPos.TERKUNCI))
    }

    @Test
    fun `hadir di cabang lain tetap dihitung hadir oleh penyaring kru`() {
        val diLuar = KruMonitoring("s1", "Budi", "leader", StatusAbsen.HadirDiOutletLain("SENTUL"), "08.20")
        assertTrue(lolosFilterKru(diLuar, FilterKru.HADIR))
        assertFalse(lolosFilterKru(diLuar, FilterKru.BELUM_HADIR))
    }

    @Test
    fun `pulang tidak dihitung sebagai hadir`() {
        val pulang = KruMonitoring("s1", "Budi", "leader", StatusAbsen.Pulang, "17.00")
        assertFalse(lolosFilterKru(pulang, FilterKru.HADIR))
        assertTrue(lolosFilterKru(pulang, FilterKru.BELUM_HADIR))
    }

    /* ---------------- rentang tanggal ---------------- */

    @Test
    fun `tanggal dalam rentang diurutkan dari yang terbaru`() {
        val hasil = tanggalDalamRentang(RentangTanggal(kamis.minusDays(2), kamis))
        assertEquals(listOf(kamis, kamis.minusDays(1), kamis.minusDays(2)), hasil)
    }

    @Test
    fun `rentang satu hari menghasilkan satu tanggal`() {
        assertEquals(listOf(kamis), tanggalDalamRentang(RentangTanggal(kamis, kamis)))
    }
}
