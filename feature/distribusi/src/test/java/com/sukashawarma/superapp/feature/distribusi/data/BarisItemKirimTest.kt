package com.sukashawarma.superapp.feature.distribusi.data

import com.sukashawarma.superapp.feature.distribusi.domain.Alokasi
import com.sukashawarma.superapp.feature.distribusi.domain.BahanBakuMeta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class BarisItemKirimTest {

    /** 1 Dus = 10 Pack; dikirim dalam Pack. */
    private val kulit = BahanBakuMeta(
        id = "b-kulit", nama = "Kulit", satuan = "Dus", satuanDistribusi = "Pack",
        satuanTengah = "Pack", satuanKecil = null, faktorTengah = 10.0, faktorTampilan = null, kategori = null,
    )

    @Test
    fun `bahan satu vendor satu baris tanpa vendor_id dalam satuan dasar`() {
        val arr = PengirimanRepository.barisItem("sj1", listOf(MuatanKirim(kulit, 25.0, emptyList()))) { false }
        assertEquals(1, arr.size())
        val o = arr[0].asJsonObject
        assertEquals("sj1", o.get("surat_jalan_id").asString)
        assertEquals("b-kulit", o.get("bahan_baku_id").asString)
        assertEquals(2.5, o.get("qty_dikirim").asDouble, 1e-9)
        assertFalse(o.has("vendor_id"))
    }

    @Test
    fun `bahan multi vendor pecah satu baris per alokasi`() {
        val muatan = MuatanKirim(kulit, 30.0, listOf(Alokasi("va", 10.0), Alokasi("vb", 20.0)))
        val arr = PengirimanRepository.barisItem("sj1", listOf(muatan)) { true }
        assertEquals(2, arr.size())
        assertEquals("va", arr[0].asJsonObject.get("vendor_id").asString)
        assertEquals(1.0, arr[0].asJsonObject.get("qty_dikirim").asDouble, 1e-9)
        assertEquals("vb", arr[1].asJsonObject.get("vendor_id").asString)
        assertEquals(2.0, arr[1].asJsonObject.get("qty_dikirim").asDouble, 1e-9)
    }
}
