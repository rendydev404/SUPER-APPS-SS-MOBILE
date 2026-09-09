package com.sukashawarma.superapp.feature.leader.domain

import com.sukashawarma.superapp.domain.model.Role
import com.sukashawarma.superapp.feature.leader.data.OutletLeaderRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LeaderAksesTest {

    @Test
    fun `hanya leader yang boleh membuka modul`() {
        assertTrue(LeaderAkses.bolehMembuka(Role.LEADER))
        Role.entries.filter { it != Role.LEADER }.forEach { role ->
            assertFalse("$role seharusnya ditolak", LeaderAkses.bolehMembuka(role))
        }
    }

    /**
     * Area manager punya pintunya sendiri di modul Manager. Membuka dua pintu ke
     * pekerjaan yang sama membuat dua layar berselisih tentang siapa yang sedang
     * memegang sebuah pengajuan.
     */
    @Test
    fun `area manager tidak ikut masuk lewat pintu leader`() {
        assertFalse(LeaderAkses.bolehMembuka(Role.AREA_MANAGER))
    }

    @Test
    fun `sesi tanpa role ditolak`() {
        assertFalse(LeaderAkses.bolehMembuka(null))
    }

    // ------------------------------------------------------ pemilihan outlet

    @Test
    fun `outlet pada baris staf menang bila memang terakses`() {
        val hasil = OutletLeaderRepository.outletUtama(listOf("a", "b"), outletStaf = "b")
        assertEquals("b", hasil)
    }

    @Test
    fun `outlet staf yang tidak terakses kalah dari cabang pertama`() {
        val hasil = OutletLeaderRepository.outletUtama(listOf("a", "b"), outletStaf = "z")
        assertEquals("a", hasil)
    }

    /**
     * Cadangan terakhir, sama seperti `primaryOutletId` web: lebih baik mencoba
     * outlet yang tercatat pada baris staf daripada tidak menampilkan apa pun. RPC
     * saldo tetap menolaknya kalau memang di luar cakupan, dan penolakan itu tampil
     * sebagai pesan — bukan sebagai nol yang meyakinkan.
     */
    @Test
    fun `tanpa cakupan sama sekali, outlet staf tetap dicoba`() {
        val hasil = OutletLeaderRepository.outletUtama(emptyList(), outletStaf = "z")
        assertEquals("z", hasil)
    }

    @Test
    fun `akun tanpa cabang dan tanpa baris staf tidak punya outlet utama`() {
        assertNull(OutletLeaderRepository.outletUtama(emptyList(), outletStaf = null))
    }
}
