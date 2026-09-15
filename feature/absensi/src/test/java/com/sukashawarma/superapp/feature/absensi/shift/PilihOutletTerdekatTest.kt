package com.sukashawarma.superapp.feature.absensi.shift

import com.sukashawarma.superapp.domain.gps.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PilihOutletTerdekatTest {

    private val titikBnr = LatLng(-6.6, 106.8)

    /** Jarak dibuat eksplisit per outlet supaya skenario seri tidak bergantung pembulatan haversine. */
    private fun pilih(vararg kandidat: Pair<KandidatOutlet, Double>): String? {
        val jarak = kandidat.associate { it.first.coords to it.second }
        return pilihOutletTerdekat(kandidat.map { it.first }) { jarak.getValue(it) }
    }

    private fun outlet(id: String, type: String?, lat: Double) = KandidatOutlet(id, type, LatLng(lat, 106.8))

    @Test
    fun `O1 outlet tes berkoordinat identik kalah dari BNR walau lebih dulu di daftar`() {
        val daftar = listOf(
            KandidatOutlet("outlet-tes", "test", titikBnr),
            KandidatOutlet("bnr", "outlet", titikBnr),
        )
        assertEquals("bnr", pilihOutletTerdekat(daftar, titikBnr))
    }

    @Test
    fun `O2 gudang menang dari marketplace berkoordinat sama`() {
        val daftar = listOf(
            KandidatOutlet("marketplace", "marketplace", titikBnr),
            KandidatOutlet("gudang", "gudang", titikBnr),
        )
        assertEquals("gudang", pilihOutletTerdekat(daftar, titikBnr))
    }

    @Test
    fun `O3 outlet tes tetap terpilih bila satu-satunya`() {
        assertEquals("outlet-tes", pilih(outlet("outlet-tes", "test", 1.0) to 20.0))
    }

    @Test
    fun `O4 outlet tes yang jelas lebih dekat tetap menang`() {
        assertEquals(
            "outlet-tes",
            pilih(outlet("bnr", "outlet", 1.0) to 50.0, outlet("outlet-tes", "test", 2.0) to 10.0),
        )
    }

    @Test
    fun `O5 outlet tanpa koordinat dilewati`() {
        val daftar = listOf(KandidatOutlet("pusat", "outlet", null), KandidatOutlet("bnr", "outlet", titikBnr))
        assertEquals("bnr", pilihOutletTerdekat(daftar, titikBnr))
        assertNull(pilihOutletTerdekat(listOf(KandidatOutlet("pusat", "outlet", null)), titikBnr))
    }

    @Test
    fun `selisih di dalam satu meter dianggap seri`() {
        assertEquals(
            "bnr",
            pilih(outlet("outlet-tes", "test", 1.0) to 10.0, outlet("bnr", "outlet", 2.0) to 10.8),
        )
    }
}
