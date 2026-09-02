package com.sukashawarma.superapp.domain.model

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MitraProfileTest {

    private fun row(json: String) = JsonParser.parseString(json).asJsonObject

    @Test
    fun `baris lengkap dipetakan penuh`() {
        val p = MitraProfile.fromRow(row("""
            {
              "id": "p-1",
              "user_id": "u-1",
              "nama_mitra": "Mitra Paledang",
              "outlet_ids": ["o-1", "o-2"],
              "profit_sharing_pct": 50,
              "bank_name": "BCA",
              "bank_account_number": "123",
              "bank_account_holder": "Bapak Anis",
              "no_pks": "PKS/2026/001",
              "tanggal_pks": "2026-01-01",
              "tanggal_berakhir_pks": "2027-01-01",
              "status": "aktif"
            }
        """))!!

        assertEquals("p-1", p.id)
        assertEquals("u-1", p.userId)
        assertEquals("Mitra Paledang", p.namaMitra)
        assertEquals(listOf("o-1", "o-2"), p.outletIds)
        assertEquals(50.0, p.profitSharingPct!!, 0.001)
        assertEquals("BCA", p.bankName)
        assertEquals("Bapak Anis", p.bankAccountHolder)
        assertTrue(p.isAktif)
    }

    @Test
    fun `mitra satu outlet tetap menghasilkan list berisi satu`() {
        val p = MitraProfile.fromRow(row("""
            {"id":"p-2","user_id":"u-2","nama_mitra":"Mitra Sentul","outlet_ids":["o-9"],"status":"aktif"}
        """))!!
        assertEquals(listOf("o-9"), p.outletIds)
    }

    @Test
    fun `kolom opsional yang null tidak bikin crash`() {
        val p = MitraProfile.fromRow(row("""
            {
              "id":"p-3","user_id":"u-3","nama_mitra":"Mitra Ciseeng","outlet_ids":[],
              "profit_sharing_pct":null,"bank_name":null,"no_pks":null,"status":null
            }
        """))!!
        assertEquals(emptyList<String>(), p.outletIds)
        assertNull(p.profitSharingPct)
        assertNull(p.bankName)
        assertEquals("", p.status)
        assertFalse(p.isAktif)
    }

    @Test
    fun `baris tanpa id atau user_id ditolak`() {
        assertNull(MitraProfile.fromRow(row("""{"user_id":"u-4","nama_mitra":"X","outlet_ids":[]}""")))
        assertNull(MitraProfile.fromRow(row("""{"id":"p-4","nama_mitra":"X","outlet_ids":[]}""")))
    }

    @Test
    fun `status selain aktif berarti tidak aktif`() {
        val p = MitraProfile.fromRow(row("""
            {"id":"p-5","user_id":"u-5","nama_mitra":"Y","outlet_ids":["o-1"],"status":"nonaktif"}
        """))!!
        assertFalse(p.isAktif)
    }
}
