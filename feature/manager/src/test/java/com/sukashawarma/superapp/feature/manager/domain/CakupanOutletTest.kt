package com.sukashawarma.superapp.feature.manager.domain

import com.sukashawarma.superapp.domain.model.Role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CakupanOutletTest {

    private data class Baris(val outletId: String, val nama: String)

    private val binaan = CakupanOutlet.Binaan(setOf("o1", "o2"))

    @Test
    fun `regional manager memegang seluruh cabang`() {
        assertEquals(CakupanOutlet.Semua, cakupanUntuk(Role.REGIONAL_MANAGER, emptySet()))
    }

    /** Ini inti perbaikannya: AM dibatasi outlet binaannya, bukan seluruh cabang. */
    @Test
    fun `area manager dibatasi outlet binaannya`() {
        val cakupan = cakupanUntuk(Role.AREA_MANAGER, setOf("o1", "o2"))
        assertEquals(CakupanOutlet.Binaan(setOf("o1", "o2")), cakupan)
        assertTrue(cakupan.mencakup("o1"))
        assertFalse(cakupan.mencakup("o3"))
    }

    /**
     * Area manager yang belum ditugaskan ke outlet mana pun TIDAK melihat apa-apa.
     * Membuka seluruh cabang saat pemetaannya kosong adalah kegagalan yang justru
     * paling berbahaya, karena tidak terlihat seperti galat.
     */
    @Test
    fun `binaan kosong tidak berarti seluruh cabang`() {
        val cakupan = cakupanUntuk(Role.AREA_MANAGER, emptySet())
        assertFalse(cakupan.mencakup("o1"))
        assertEquals(emptyList<Baris>(), saringCakupan(listOf(Baris("o1", "A")), cakupan) { it.outletId })
    }

    @Test
    fun `role tanpa sesi diperlakukan sebagai binaan kosong`() {
        assertFalse(cakupanUntuk(null, emptySet()).mencakup("o1"))
    }

    @Test
    fun `role pusat lain juga memegang seluruh cabang`() {
        listOf(Role.ADMIN, Role.OWNER, Role.ADMIN_HR, Role.ADMIN_FINANCE, Role.KITCHEN, Role.PURCHASING, Role.SPV)
            .forEach { assertEquals("role $it", CakupanOutlet.Semua, cakupanUntuk(it, emptySet())) }
    }

    /** Cermin `accessible_outlet_ids()`: AM masuk cabang staff_outlets, bukan cabang semua-outlet. */
    @Test
    fun `area manager dan leader tidak termasuk role seluruh outlet`() {
        assertFalse(Role.AREA_MANAGER in ROLE_SELURUH_OUTLET)
        assertFalse(Role.LEADER in ROLE_SELURUH_OUTLET)
        assertFalse(Role.CREW in ROLE_SELURUH_OUTLET)
        assertTrue(Role.REGIONAL_MANAGER in ROLE_SELURUH_OUTLET)
    }

    @Test
    fun `penyaring membuang baris di luar cakupan`() {
        val daftar = listOf(Baris("o1", "Empang"), Baris("o3", "Sentul"), Baris("o2", "Bcc"))
        assertEquals(
            listOf("Empang", "Bcc"),
            saringCakupan(daftar, binaan) { it.outletId }.map { it.nama },
        )
    }

    @Test
    fun `penyaring melewatkan semuanya untuk cakupan seluruh outlet`() {
        val daftar = listOf(Baris("o1", "Empang"), Baris("o9", "Surabaya"))
        assertEquals(daftar, saringCakupan(daftar, CakupanOutlet.Semua) { it.outletId })
    }
}
