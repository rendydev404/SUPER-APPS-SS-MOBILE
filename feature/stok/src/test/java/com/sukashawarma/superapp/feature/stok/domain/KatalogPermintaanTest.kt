package com.sukashawarma.superapp.feature.stok.domain

import com.sukashawarma.superapp.domain.model.Role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Aturan diambil dari `allowedBahanBaku`/`addCriticalItem`/`pendingItemIds` di
 * `apps/stok/src/components/permintaan/PermintaanForm.tsx`.
 */
class KatalogPermintaanTest {

    @Test
    fun `aset dan perlengkapan tidak boleh diminta`() {
        assertFalse(KatalogPermintaan.bolehDiminta("ASET", "KOMPOR", katalogPenuh = true))
        assertFalse(KatalogPermintaan.bolehDiminta("Perlengkapan", "SAPU", katalogPenuh = true))
    }

    @Test
    fun `printer thermal dan id card dikecualikan by-name`() {
        assertFalse(KatalogPermintaan.bolehDiminta("OPERASIONAL", "PRINTER THERMAL", katalogPenuh = true))
        assertFalse(KatalogPermintaan.bolehDiminta("OPERASIONAL", "ID CARD", katalogPenuh = true))
    }

    @Test
    fun `bumbu hanya untuk katalog penuh kecuali bawang`() {
        assertFalse(KatalogPermintaan.bolehDiminta("BUMBU", "KETUMBAR", katalogPenuh = false))
        assertTrue(KatalogPermintaan.bolehDiminta("BUMBU", "BAWANG", katalogPenuh = false))
        assertTrue(KatalogPermintaan.bolehDiminta("BUMBU", "KETUMBAR", katalogPenuh = true))
    }

    @Test
    fun `role dapur memegang katalog penuh`() {
        assertTrue(KatalogPermintaan.katalogPenuh(Role.KITCHEN))
        assertTrue(KatalogPermintaan.katalogPenuh(Role.PURCHASING))
        assertFalse(KatalogPermintaan.katalogPenuh(Role.CREW))
        assertFalse(KatalogPermintaan.katalogPenuh(null))
    }

    @Test
    fun `kekurangan dihitung setelah saldo dinormalkan ke satuan besar`() {
        val meta = UnitMeta(satuan = "Kg", satuanKecil = "Gram", faktorTampilan = 1000.0)
        // Threshold 10 Kg, saldo baris gram-scale 4000 Gram = 4 Kg -> kurang 6 Kg.
        assertEquals(
            6.0,
            KatalogPermintaan.kekuranganBesar(10.0, 4000.0, saldoIsGram = true, meta = meta),
            0.0001,
        )
        // Web menghitung 10 - 4000 mentah (negatif -> minimal 1); di sini disengaja beda.
        assertEquals(
            6.0,
            KatalogPermintaan.kekuranganBesar(10.0, 4.0, saldoIsGram = false, meta = meta),
            0.0001,
        )
    }

    @Test
    fun `kekurangan minimal satu satuan besar`() {
        val meta = UnitMeta(satuan = "Kg")
        assertEquals(
            1.0,
            KatalogPermintaan.kekuranganBesar(10.0, 12.0, saldoIsGram = false, meta = meta),
            0.0001,
        )
    }

    @Test
    fun `saran qty dibulatkan ke atas pada satuan besar`() {
        assertEquals(2L, KatalogPermintaan.saranQty(1.2))
        assertEquals(2L, KatalogPermintaan.saranQty(2.0))
        // Kekurangan sangat kecil tetap menghasilkan satu satuan besar penuh.
        assertEquals(1L, KatalogPermintaan.saranQty(0.05))
    }

    @Test
    fun `jendela sembunyi dua belas jam`() {
        val now = 1_000_000_000_000L
        val sebelasJam = now - 11L * 60 * 60 * 1000
        val tigaBelasJam = now - 13L * 60 * 60 * 1000
        assertTrue(KatalogPermintaan.masihMenunggu(sebelasJam, now))
        assertFalse(KatalogPermintaan.masihMenunggu(tigaBelasJam, now))
        // Stempel tak terbaca dianggap masih menunggu, bukan dibebaskan.
        assertTrue(KatalogPermintaan.masihMenunggu(null, now))
    }
}
