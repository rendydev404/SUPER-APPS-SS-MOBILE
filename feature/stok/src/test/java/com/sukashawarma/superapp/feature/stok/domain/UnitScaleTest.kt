package com.sukashawarma.superapp.feature.stok.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UnitScaleTest {

    private val kg = UnitMeta(
        satuan = "kg",
        satuanTengah = "pak",
        satuanKecil = "gr",
        faktorTengah = 250.0,
        faktorTampilan = 1000.0,
    )

    @Test
    fun `konversi besar ke kecil dan kembali tidak kehilangan nilai`() {
        val kecil = UnitScale.smallestFromBesar(3.5, kg)
        assertEquals(3500.0, kecil!!, 0.0001)
        assertEquals(3.5, UnitScale.besarFromSmallest(kecil, kg)!!, 0.0001)
    }

    @Test
    fun `faktor null tidak diam-diam dianggap satu`() {
        val tanpaFaktor = kg.copy(faktorTampilan = null)
        assertNull(UnitScale.smallestFromBesar(5.0, tanpaFaktor))
        assertNull(UnitScale.besarFromSmallest(5.0, tanpaFaktor))
    }

    @Test
    fun `faktor nol dan negatif ditolak`() {
        assertNull(UnitScale.smallestFromBesar(5.0, kg.copy(faktorTampilan = 0.0)))
        assertNull(UnitScale.smallestFromBesar(5.0, kg.copy(faktorTampilan = -3.0)))
    }

    @Test
    fun `saldo small-scale dipakai apa adanya`() {
        assertEquals(4200.0, UnitScale.normalizeSaldo(4200.0, saldoIsGram = true, meta = kg)!!, 0.0001)
    }

    @Test
    fun `saldo legacy dinaikkan ke satuan terkecil`() {
        assertEquals(4200.0, UnitScale.normalizeSaldo(4.2, saldoIsGram = false, meta = kg)!!, 0.0001)
    }

    /**
     * Inti dari keputusan menghitung status sendiri: stok fisik yang sama harus
     * menghasilkan status yang sama, tidak peduli baris mana yang sudah melewati
     * opname modern dan mana yang belum.
     */
    @Test
    fun `stok fisik identik dengan skala berbeda menghasilkan status sama`() {
        val threshold = 5.0 // 5 kg = 5000 gr
        val modern = UnitScale.status(
            saldoNorm = UnitScale.normalizeSaldo(3000.0, saldoIsGram = true, meta = kg),
            thresholdNorm = UnitScale.normalizeThreshold(threshold, kg),
        )
        val legacy = UnitScale.status(
            saldoNorm = UnitScale.normalizeSaldo(3.0, saldoIsGram = false, meta = kg),
            thresholdNorm = UnitScale.normalizeThreshold(threshold, kg),
        )
        assertEquals(StokStatus.WARNING, modern)
        assertEquals(modern, legacy)
    }

    @Test
    fun `saldo di bawah setengah threshold berstatus kritis`() {
        val status = UnitScale.status(
            saldoNorm = 2000.0,
            thresholdNorm = 5000.0,
        )
        assertEquals(StokStatus.BELOW, status)
    }

    @Test
    fun `porsi menipis membuat status kritis walau saldo di atas setengah threshold`() {
        val status = UnitScale.status(
            saldoNorm = 4000.0,
            thresholdNorm = 5000.0,
            porsiTersisa = 3,
            marqueeWarning = 7,
        )
        assertEquals(StokStatus.BELOW, status)
    }

    @Test
    fun `skala tak diketahui menghasilkan status unknown bukan tebakan`() {
        assertEquals(StokStatus.UNKNOWN, UnitScale.status(saldoNorm = null, thresholdNorm = 100.0))
        assertEquals(StokStatus.UNKNOWN, UnitScale.status(saldoNorm = 100.0, thresholdNorm = null))
    }

    @Test
    fun `format berjenjang memecah ke besar tengah kecil`() {
        assertEquals("2 kg 1 pak 30 gr", UnitScale.formatBerjenjang(2280.0, kg))
    }

    @Test
    fun `format berjenjang menyerah bila faktor tidak memadai`() {
        assertNull(UnitScale.formatBerjenjang(2280.0, kg.copy(faktorTampilan = null)))
    }
}
