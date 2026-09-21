package com.sukashawarma.superapp.feature.home.domain

import com.google.gson.JsonParser
import com.sukashawarma.superapp.domain.model.Role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BonusBulananTest {

    private fun baris(json: String) =
        JsonParser.parseString(json).asJsonArray.map { it.asJsonObject }

    @Test
    fun `hanya crew leader AM dan RM yang punya skema bonus`() {
        assertEquals(SkemaBonus.KRU, skemaBonusUntuk(Role.CREW))
        assertEquals(SkemaBonus.KRU, skemaBonusUntuk(Role.LEADER))
        assertEquals(SkemaBonus.AREA_MANAGER, skemaBonusUntuk(Role.AREA_MANAGER))
        assertEquals(SkemaBonus.REGIONAL_MANAGER, skemaBonusUntuk(Role.REGIONAL_MANAGER))
        assertNull(skemaBonusUntuk(Role.ADMIN))
        assertNull(skemaBonusUntuk(Role.DEVELOPER))
        assertNull(skemaBonusUntuk(null))
    }

    @Test
    fun `skema kru memilih baris crew_id milik sendiri dan memakai total_bonus server`() {
        // Bentuk persis keluaran get_monthly_crew_bonus (dua kru di outlet yang sama).
        val rows = baris(
            """[
              {"crew_id":"a","crew_name":"Lain","role":"crew","outlet_name":"SS EMPANG",
               "total_pcs_outlet":3200,"active_crew_count":4,"bonus_rate":100.0,"total_bonus":80000},
              {"crew_id":"b","crew_name":"Saya","role":"leader","outlet_name":"SS EMPANG",
               "total_pcs_outlet":3200,"active_crew_count":4,"bonus_rate":100.0,"total_bonus":80000}
            ]"""
        )
        val b = susunBonusBulanan(SkemaBonus.KRU, "b", rows, 9, 2026)
        assertTrue(b.terdaftar)
        assertEquals(80_000L, b.nominal)
        assertEquals("3.200 pcs × Rp 100 ÷ 4 kru", b.rumus)
        assertEquals("SS EMPANG", b.cakupan)
        assertEquals("September 2026", b.labelBulan)
    }

    @Test
    fun `area manager memakai total_pcs dan jumlah cabang binaan`() {
        val rows = baris(
            """[{"staff_id":"am1","managed_outlet_count":6,"total_pcs":11540,
                 "bonus_rate":50.0,"total_bonus":577000.0}]"""
        )
        val b = susunBonusBulanan(SkemaBonus.AREA_MANAGER, "am1", rows, 9, 2026)
        assertEquals(577_000L, b.nominal)
        assertEquals("11.540 pcs × Rp 50", b.rumus)
        assertEquals("6 cabang binaan", b.cakupan)
    }

    @Test
    fun `regional manager memakai total_pcs_global`() {
        val rows = baris(
            """[{"staff_id":"rm1","scope_description":"Semua Cabang Operasional",
                 "total_pcs_global":34632,"bonus_rate":50.0,"total_bonus":1731600.0}]"""
        )
        val b = susunBonusBulanan(SkemaBonus.REGIONAL_MANAGER, "rm1", rows, 9, 2026)
        assertEquals(1_731_600L, b.nominal)
        assertEquals("34.632 pcs × Rp 50", b.rumus)
        assertEquals("Semua Cabang Operasional", b.cakupan)
    }

    @Test
    fun `staf yang tidak ada di hasil RPC dianggap tidak terdaftar bukan nol`() {
        val rows = baris("""[{"crew_id":"lain","total_bonus":50000}]""")
        val b = susunBonusBulanan(SkemaBonus.KRU, "saya", rows, 9, 2026)
        assertFalse(b.terdaftar)
        assertNull(b.nominal)
    }

    @Test
    fun `terdaftar tapi belum ada penjualan tetap nol rupiah`() {
        val rows = baris(
            """[{"crew_id":"saya","outlet_name":"KANTOR PUSAT","total_pcs_outlet":0,
                 "active_crew_count":1,"bonus_rate":100.0,"total_bonus":0}]"""
        )
        val b = susunBonusBulanan(SkemaBonus.KRU, "saya", rows, 9, 2026)
        assertTrue(b.terdaftar)
        assertEquals(0L, b.nominal)
        assertEquals("0 pcs × Rp 100", b.rumus)
    }

    @Test
    fun `format rupiah memakai titik ribuan`() {
        assertEquals("Rp 1.250.000", rupiah(1_250_000))
        assertEquals("Rp 0", rupiah(0))
        assertEquals("-Rp 500", rupiah(-500))
    }
}
