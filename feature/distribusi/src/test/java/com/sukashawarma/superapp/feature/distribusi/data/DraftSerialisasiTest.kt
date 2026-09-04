package com.sukashawarma.superapp.feature.distribusi.data

import com.sukashawarma.superapp.feature.distribusi.domain.IsianVerifikasi
import com.sukashawarma.superapp.feature.distribusi.domain.KondisiItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DraftSerialisasiTest {

    @Test
    fun `draft bolak balik lewat json tanpa kehilangan isian`() {
        val draft = DraftVerifikasi(
            isian = mapOf(
                "item-1" to IsianVerifikasi(10.0, KondisiItem.BAIK, "", "sj1/item-1.jpg"),
                "item-2" to IsianVerifikasi(8.5, KondisiItem.TIDAK_SESUAI, "2 dus penyok", "sj1/item-2.jpg"),
            ),
            indeksItem = 1,
            langkah = "kartu",
            kondisiTerkonfirmasi = true,
        )
        val pulih = draftDariJson(draftKeJson(draft))
        assertEquals(draft, pulih)
    }

    @Test
    fun `qty null bertahan sebagai null, bukan berubah jadi nol`() {
        val draft = DraftVerifikasi(
            isian = mapOf("item-1" to IsianVerifikasi(null, KondisiItem.BAIK, "", null)),
            indeksItem = 0,
            langkah = "kartu",
            kondisiTerkonfirmasi = false,
        )
        val pulih = draftDariJson(draftKeJson(draft))
        assertNull(pulih!!.isian.getValue("item-1").qtyTerima)
    }

    @Test
    fun `json rusak menghasilkan null, bukan lemparan`() {
        assertNull(draftDariJson("{bukan json"))
        assertNull(draftDariJson(""))
    }

    @Test
    fun `draft kosong tetap bisa dipulihkan`() {
        val draft = DraftVerifikasi(emptyMap(), 0, "kartu", false)
        assertEquals(draft, draftDariJson(draftKeJson(draft)))
    }
}
