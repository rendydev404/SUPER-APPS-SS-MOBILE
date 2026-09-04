package com.sukashawarma.superapp.feature.distribusi.domain

import com.sukashawarma.superapp.domain.model.Role
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DistribusiAksesTest {

    @Test
    fun `keempat role native boleh membuka modul`() {
        assertTrue(DistribusiAkses.bolehMembuka(Role.CREW))
        assertTrue(DistribusiAkses.bolehMembuka(Role.LEADER))
        assertTrue(DistribusiAkses.bolehMembuka(Role.AREA_MANAGER))
        assertTrue(DistribusiAkses.bolehMembuka(Role.REGIONAL_MANAGER))
    }

    /** Kitchen dan admin tetap memakai versi web; modul native tidak untuk mereka. */
    @Test
    fun `kitchen dan admin tidak membuka modul native`() {
        assertFalse(DistribusiAkses.bolehMembuka(Role.KITCHEN))
        assertFalse(DistribusiAkses.bolehMembuka(Role.ADMIN))
        assertFalse(DistribusiAkses.bolehMembuka(Role.OWNER))
        assertFalse(DistribusiAkses.bolehMembuka(Role.MITRA))
    }

    @Test
    fun `role tak dikenal ditolak, bukan diloloskan`() {
        assertFalse(DistribusiAkses.bolehMembuka(null))
        assertFalse(DistribusiAkses.bolehVerifikasi(null))
        assertFalse(DistribusiAkses.bolehTutupDokumen(null))
        assertFalse(DistribusiAkses.bolehLihatKodeVerifikasi(null))
    }

    @Test
    fun `hanya crew dan leader yang memverifikasi penerimaan`() {
        assertTrue(DistribusiAkses.bolehVerifikasi(Role.CREW))
        assertTrue(DistribusiAkses.bolehVerifikasi(Role.LEADER))
        assertFalse(DistribusiAkses.bolehVerifikasi(Role.AREA_MANAGER))
        assertFalse(DistribusiAkses.bolehVerifikasi(Role.REGIONAL_MANAGER))
    }

    @Test
    fun `hanya area dan regional manager yang menutup dokumen`() {
        assertTrue(DistribusiAkses.bolehTutupDokumen(Role.AREA_MANAGER))
        assertTrue(DistribusiAkses.bolehTutupDokumen(Role.REGIONAL_MANAGER))
        assertFalse(DistribusiAkses.bolehTutupDokumen(Role.CREW))
        assertFalse(DistribusiAkses.bolehTutupDokumen(Role.LEADER))
    }

    /** Kalau crew bisa membaca kode verifikasi di layar, gerbang scan QR kehilangan
     *  maknanya: dia bisa membuka verifikasi tanpa memegang dokumen fisik. */
    @Test
    fun `kode verifikasi disembunyikan dari crew dan leader`() {
        assertFalse(DistribusiAkses.bolehLihatKodeVerifikasi(Role.CREW))
        assertFalse(DistribusiAkses.bolehLihatKodeVerifikasi(Role.LEADER))
        assertTrue(DistribusiAkses.bolehLihatKodeVerifikasi(Role.AREA_MANAGER))
        assertTrue(DistribusiAkses.bolehLihatKodeVerifikasi(Role.REGIONAL_MANAGER))
    }
}
