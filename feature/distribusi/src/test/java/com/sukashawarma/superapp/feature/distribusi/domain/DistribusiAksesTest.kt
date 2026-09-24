package com.sukashawarma.superapp.feature.distribusi.domain

import com.sukashawarma.superapp.domain.model.Role
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DistribusiAksesTest {

    @Test
    fun `kelima role native boleh membuka modul`() {
        assertTrue(DistribusiAkses.bolehMembuka(Role.CREW))
        assertTrue(DistribusiAkses.bolehMembuka(Role.LEADER))
        assertTrue(DistribusiAkses.bolehMembuka(Role.AREA_MANAGER))
        assertTrue(DistribusiAkses.bolehMembuka(Role.REGIONAL_MANAGER))
        assertTrue(DistribusiAkses.bolehMembuka(Role.KITCHEN))
    }

    /** Admin dan owner tetap memakai versi web; modul native tidak untuk mereka. */
    @Test
    fun `admin, owner, dan mitra tidak membuka modul native`() {
        assertFalse(DistribusiAkses.bolehMembuka(Role.ADMIN))
        assertFalse(DistribusiAkses.bolehMembuka(Role.OWNER))
        assertFalse(DistribusiAkses.bolehMembuka(Role.MITRA))
    }

    @Test
    fun `role tak dikenal ditolak, bukan diloloskan`() {
        assertFalse(DistribusiAkses.bolehMembuka(null))
        assertFalse(DistribusiAkses.bolehVerifikasi(null))
        assertFalse(DistribusiAkses.bolehTerbitkan(null))
        assertFalse(DistribusiAkses.bolehBatalkanDraft(null))
        assertFalse(DistribusiAkses.bolehTutupDokumen(null))
        assertFalse(DistribusiAkses.bolehLihatKodeVerifikasi(null))
    }

    @Test
    fun `crew, leader, dan area manager yang memverifikasi penerimaan`() {
        assertTrue(DistribusiAkses.bolehVerifikasi(Role.CREW))
        assertTrue(DistribusiAkses.bolehVerifikasi(Role.LEADER))
        assertTrue(DistribusiAkses.bolehVerifikasi(Role.AREA_MANAGER))
        assertFalse(DistribusiAkses.bolehVerifikasi(Role.REGIONAL_MANAGER))
        assertFalse(DistribusiAkses.bolehVerifikasi(Role.KITCHEN))
    }

    /** RPC `create_surat_jalan_with_number` menolak RM, jadi hanya kitchen dari role native. */
    @Test
    fun `hanya kitchen yang menerbitkan dan membatalkan draft`() {
        assertTrue(DistribusiAkses.bolehTerbitkan(Role.KITCHEN))
        assertTrue(DistribusiAkses.bolehBatalkanDraft(Role.KITCHEN))
        listOf(Role.CREW, Role.LEADER, Role.AREA_MANAGER, Role.REGIONAL_MANAGER).forEach {
            assertFalse(DistribusiAkses.bolehTerbitkan(it))
            assertFalse(DistribusiAkses.bolehBatalkanDraft(it))
        }
    }

    @Test
    fun `regional manager dan kitchen yang menutup dokumen`() {
        assertTrue(DistribusiAkses.bolehTutupDokumen(Role.REGIONAL_MANAGER))
        assertTrue(DistribusiAkses.bolehTutupDokumen(Role.KITCHEN))
        assertFalse(DistribusiAkses.bolehTutupDokumen(Role.AREA_MANAGER))
        assertFalse(DistribusiAkses.bolehTutupDokumen(Role.CREW))
        assertFalse(DistribusiAkses.bolehTutupDokumen(Role.LEADER))
    }

    /** Kalau crew/leader/AM bisa membaca kode verifikasi di layar, gerbang scan QR kehilangan
     *  maknanya: AM tidak melihat kode pengirim; penerimaan tetap melewati scan fisik. */
    @Test
    fun `kode verifikasi disembunyikan dari crew, leader, dan area manager`() {
        assertFalse(DistribusiAkses.bolehLihatKodeVerifikasi(Role.CREW))
        assertFalse(DistribusiAkses.bolehLihatKodeVerifikasi(Role.LEADER))
        assertFalse(DistribusiAkses.bolehLihatKodeVerifikasi(Role.AREA_MANAGER))
        assertTrue(DistribusiAkses.bolehLihatKodeVerifikasi(Role.REGIONAL_MANAGER))
        assertTrue(DistribusiAkses.bolehLihatKodeVerifikasi(Role.KITCHEN))
    }
}
