package com.sukashawarma.superapp.feature.stok.domain

import com.sukashawarma.superapp.domain.model.Role
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StokAksesTest {

    /**
     * Inti perbaikan gerbang akses: role yang berhak menyetujui permintaan HARUS
     * bisa membuka modulnya. Sebelum ini kedua himpunan tidak beririsan sama sekali,
     * sehingga tombol setujui tidak pernah bisa muncul untuk siapa pun.
     */
    @Test
    fun `semua penyetuju permintaan termasuk kelompok pusat`() {
        listOf(Role.KITCHEN, Role.ADMIN_FINANCE, Role.ADMIN, Role.OWNER, Role.PURCHASING).forEach { role ->
            assertTrue("$role harus bisa membuka modul Stok", StokAkses.pusat(role))
            assertTrue("$role harus boleh menyetujui permintaan", Approver.bolehApprovePermintaan(role))
        }
    }

    /** Permintaan pengguna: role pusat tidak mendapat Dashboard monitoring. */
    @Test
    fun `role pusat tidak melihat dashboard`() {
        assertFalse(StokAkses.melihatDashboard(Role.KITCHEN))
        assertFalse(StokAkses.melihatDashboard(Role.ADMIN))
        assertFalse(StokAkses.melihatDashboard(Role.OWNER))
        assertFalse(StokAkses.melihatDashboard(Role.PURCHASING))
        assertFalse(StokAkses.melihatDashboard(Role.ADMIN_FINANCE))
        assertFalse(StokAkses.melihatDashboard(Role.DEVELOPER))
    }

    @Test
    fun `role outlet tetap melihat dashboard`() {
        listOf(Role.CREW, Role.LEADER, Role.SPV, Role.AREA_MANAGER, Role.REGIONAL_MANAGER).forEach { role ->
            assertTrue("$role kehilangan dashboard", StokAkses.melihatDashboard(role))
        }
    }

    @Test
    fun `pengawas outlet bukan role pusat dan sebaliknya`() {
        assertTrue(StokAkses.pengawas(Role.AREA_MANAGER))
        assertFalse(StokAkses.pusat(Role.AREA_MANAGER))
        assertTrue(StokAkses.pusat(Role.KITCHEN))
        assertFalse(StokAkses.pengawas(Role.KITCHEN))
        // Crew tidak masuk keduanya — dia hanya memegang outletnya sendiri.
        assertTrue(StokAkses.operasional(Role.CREW))
    }

    /** Cermin web: penerimaan PO untuk pusat dan pengawas, inbound/outbound hanya kitchen. */
    @Test
    fun `penerimaan po lebih luas daripada inbound outbound`() {
        assertTrue(StokAkses.melihatPenerimaanPo(Role.KITCHEN))
        assertTrue(StokAkses.melihatPenerimaanPo(Role.LEADER))
        assertFalse(StokAkses.melihatPenerimaanPo(Role.CREW))

        assertTrue(StokAkses.melihatInboundOutbound(Role.KITCHEN))
        assertFalse(StokAkses.melihatInboundOutbound(Role.ADMIN))
        assertFalse(StokAkses.melihatInboundOutbound(Role.LEADER))
    }

    @Test
    fun `tanpa sesi tidak ada kelompok yang cocok`() {
        assertFalse(StokAkses.pusat(null))
        assertFalse(StokAkses.pengawas(null))
        assertFalse(StokAkses.melihatPenerimaanPo(null))
    }

    /**
     * Pengaturan threshold hanya untuk admin.
     *
     * Ini bukan sekadar kerapian tampilan seperti gerbang lain di kelas ini:
     * `outlet_reorder_point` tidak punya policy RLS sama sekali, jadi tidak ada
     * lapisan di database yang akan menolak peran yang salah. Tes ini yang menjaga
     * daftar itu tidak melebar diam-diam.
     */
    @Test
    fun `hanya admin yang boleh mengatur threshold`() {
        assertTrue(StokAkses.melihatThreshold(Role.ADMIN))

        listOf(
            Role.OWNER, Role.KITCHEN, Role.PURCHASING, Role.ADMIN_FINANCE, Role.DEVELOPER,
            Role.REGIONAL_MANAGER, Role.AREA_MANAGER, Role.LEADER, Role.SPV, Role.CREW,
        ).forEach { role ->
            assertFalse("$role seharusnya tidak bisa mengatur threshold", StokAkses.melihatThreshold(role))
        }
        assertFalse(StokAkses.melihatThreshold(null))
    }

    /**
     * Analisis selisih di Detail Opname — cermin `canViewThresholdAndLoss` web.
     *
     * Pengawas outlet sengaja TIDAK masuk walau merekalah yang menyetujui opname:
     * yang digerbangi cuma penafsiran angkanya, sedangkan qty sistem, fisik, dan
     * selisihnya tetap terbaca semua peran.
     */
    @Test
    fun `analisis selisih hanya untuk kantor dan gudang pusat`() {
        listOf(
            Role.KITCHEN, Role.ADMIN, Role.ADMIN_FINANCE, Role.OWNER,
            Role.DEVELOPER, Role.PURCHASING,
        ).forEach { role ->
            assertTrue("$role kehilangan analisis selisih", StokAkses.melihatAnalisisSelisih(role))
        }

        listOf(Role.LEADER, Role.SPV, Role.AREA_MANAGER, Role.REGIONAL_MANAGER, Role.CREW).forEach { role ->
            assertFalse("$role seharusnya tidak melihat analisis selisih", StokAkses.melihatAnalisisSelisih(role))
        }
        assertFalse(StokAkses.melihatAnalisisSelisih(null))
    }
}
