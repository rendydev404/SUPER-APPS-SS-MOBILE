package com.sukashawarma.superapp.feature.stok.domain

import com.sukashawarma.superapp.domain.model.Role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gerbang peran modul Retur, diuji terhadap daftar yang ADA DI DALAM RPC-nya —
 * bukan terhadap tampilan. Kalau tes ini berubah, pastikan `approve_retur_by_manager`
 * dan `verifikasi_kitchen_dan_buat_sj` juga berubah, kalau tidak tombolnya akan
 * muncul lalu ditolak database.
 */
class ReturAksesTest {

    @Test
    fun `penyetuju klaim sama dengan daftar di approve_retur_by_manager`() {
        listOf(
            Role.AREA_MANAGER, Role.REGIONAL_MANAGER, Role.SPV,
            Role.ADMIN, Role.OWNER, Role.DEVELOPER,
        ).forEach {
            assertTrue("$it harus bisa menyetujui retur", ReturAkses.manager(it))
        }
        assertFalse(ReturAkses.manager(Role.CREW))
        assertFalse(ReturAkses.manager(Role.LEADER))
        assertFalse(ReturAkses.manager(Role.KITCHEN))
        assertFalse(ReturAkses.manager(null))
    }

    /**
     * Leader memegang outlet dan boleh MENGAJUKAN retur, tetapi tidak menyetujuinya —
     * seluruh alur web menuntut validasi di atas outlet, dan RPC menolaknya.
     */
    @Test
    fun `leader mengajukan tapi tidak menyetujui`() {
        assertFalse(ReturAkses.manager(Role.LEADER))
        assertTrue(ReturAkses.serahKurir(Role.LEADER, "outlet-1", "outlet-1"))
    }

    @Test
    fun `verifikasi fisik hanya gudang pusat`() {
        listOf(Role.KITCHEN, Role.ADMIN, Role.OWNER, Role.PURCHASING, Role.DEVELOPER).forEach {
            assertTrue("$it harus bisa menimbang fisik retur", ReturAkses.kitchen(it))
        }
        assertFalse(ReturAkses.kitchen(Role.CREW))
        assertFalse(ReturAkses.kitchen(Role.LEADER))
        assertFalse(ReturAkses.kitchen(Role.AREA_MANAGER))
        assertFalse(ReturAkses.kitchen(null))
    }

    @Test
    fun `serah kurir untuk outlet sendiri, manajer, dan gudang pusat`() {
        assertTrue(ReturAkses.serahKurir(Role.CREW, "outlet-1", "outlet-1"))
        assertFalse(ReturAkses.serahKurir(Role.CREW, "outlet-1", "outlet-2"))
        // Manajer dan gudang mendampingi outlet mana pun saat kurir datang.
        assertTrue(ReturAkses.serahKurir(Role.AREA_MANAGER, "outlet-1", "outlet-2"))
        assertTrue(ReturAkses.serahKurir(Role.KITCHEN, "outlet-1", null))
    }

    @Test
    fun `tab awal mengikuti antrean yang jadi tanggung jawab peran`() {
        assertEquals(TabRetur.PERSETUJUAN, ReturAkses.tabAwal(Role.AREA_MANAGER))
        assertEquals(TabRetur.KITCHEN, ReturAkses.tabAwal(Role.KITCHEN))
        assertEquals(TabRetur.AKTIF, ReturAkses.tabAwal(Role.CREW))
        assertEquals(TabRetur.AKTIF, ReturAkses.tabAwal(Role.LEADER))
        // Admin memenuhi kedua daftar; manajer didahulukan, sama seperti web.
        assertEquals(TabRetur.PERSETUJUAN, ReturAkses.tabAwal(Role.ADMIN))
    }

    @Test
    fun `lencana menghitung antrean yang berbeda per peran`() {
        val data = listOf(
            StatusRetur.DIAJUKAN to "outlet-1",
            StatusRetur.DIAJUKAN to "outlet-2",
            StatusRetur.DALAM_PENGIRIMAN to "outlet-1",
            StatusRetur.DITERIMA_KITCHEN to "outlet-2",
            StatusRetur.SELESAI to "outlet-1",
            StatusRetur.DITOLAK to "outlet-1",
        )
        assertEquals(2, LencanaRetur.hitung(data, Role.AREA_MANAGER, "outlet-1"))
        assertEquals(2, LencanaRetur.hitung(data, Role.KITCHEN, null))
        // Kru hanya menghitung tiket outletnya sendiri yang belum tuntas.
        assertEquals(2, LencanaRetur.hitung(data, Role.CREW, "outlet-1"))
        assertEquals(0, LencanaRetur.hitung(data, Role.CREW, "outlet-9"))
    }

    @Test
    fun `status tak dikenal jadi null, bukan melempar`() {
        assertNull(StatusRetur.dari("status_baru_dari_migration_berikutnya"))
        assertNull(StatusRetur.dari(null))
        assertEquals(StatusRetur.DIAJUKAN, StatusRetur.dari("diajukan"))
    }

    /** Stepper enam tahap: hanya SELESAI yang menyalakan semuanya, DITOLAK tidak ada. */
    @Test
    fun `langkah stepper sesuai urutan alur`() {
        assertEquals(0, StatusRetur.DIAJUKAN.langkah)
        assertEquals(1, StatusRetur.DISETUJUI_MANAGER.langkah)
        assertEquals(2, StatusRetur.DALAM_PENGIRIMAN.langkah)
        assertEquals(3, StatusRetur.DITERIMA_KITCHEN.langkah)
        assertEquals(4, StatusRetur.DIKIRIM_PENGGANTI.langkah)
        assertEquals(5, StatusRetur.SELESAI.langkah)
        assertEquals(-1, StatusRetur.DITOLAK.langkah)
        assertEquals(6, StatusRetur.LANGKAH.size)
    }

    @Test
    fun `hanya selesai dan ditolak yang masuk arsip`() {
        assertTrue(StatusRetur.SELESAI.tuntas)
        assertTrue(StatusRetur.DITOLAK.tuntas)
        StatusRetur.entries.filterNot { it.tuntas }.forEach {
            assertFalse("$it seharusnya masih dianggap berjalan", it.tuntas)
        }
    }

    @Test
    fun `hanya armada internal yang boleh tanpa nomor resi`() {
        assertFalse(JenisLogistik.INTERNAL.pihakKetiga)
        listOf(
            JenisLogistik.LALAMOVE, JenisLogistik.GOSEND,
            JenisLogistik.GRABEXPRESS, JenisLogistik.DELIVEREE, JenisLogistik.LAINNYA,
        ).forEach { assertTrue("$it wajib resi", it.pihakKetiga) }
        // Nilai asing jatuh ke internal, bukan melempar: kolomnya ber-CHECK di DB,
        // tetapi baris lama bisa saja diisi lewat jalur lain.
        assertEquals(JenisLogistik.INTERNAL, JenisLogistik.dari("kurir_gaib"))
    }
}
