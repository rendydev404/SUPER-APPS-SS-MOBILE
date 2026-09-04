package com.sukashawarma.superapp.feature.stok.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Nilai harapan diambil dari `getDistribusiFactor`/`convertToDistribusiUnit`/
 * `convertToBaseUnit` di `apps/stok/src/lib/format/compositeUnit.ts`.
 */
class DistribusiUnitTest {

    @Test
    fun `tanpa satuan distribusi faktor satu`() {
        assertEquals(
            1.0,
            DistribusiUnit.faktor("Dus", "Pack", 20.0, "Lembar", 400.0, null),
            0.0001,
        )
    }

    @Test
    fun `distribusi sama dengan satuan besar faktor satu`() {
        assertEquals(
            1.0,
            DistribusiUnit.faktor("Dus", "Pack", 20.0, "Lembar", 400.0, "dus"),
            0.0001,
        )
    }

    @Test
    fun `distribusi pada satuan tengah memakai faktor tengah`() {
        // 1 Dus = 20 Pack; pesan dalam Pack.
        val f = DistribusiUnit.faktor("Dus", "Pack", 20.0, "Lembar", 400.0, "pack")
        assertEquals(20.0, f, 0.0001)
        // 0.5 Dus = 10 Pack; 10 Pack kembali jadi 0.5 Dus.
        assertEquals(10.0, DistribusiUnit.keDistribusi(0.5, f), 0.0001)
        assertEquals(0.5, DistribusiUnit.keBase(10.0, f), 0.0001)
    }

    @Test
    fun `distribusi pada satuan kecil memakai faktor tampilan`() {
        val f = DistribusiUnit.faktor("Dus", "Pack", 20.0, "Lembar", 400.0, "lembar")
        assertEquals(400.0, f, 0.0001)
    }

    @Test
    fun `pemetaan implisit kg atas gram`() {
        // SAOS CABE: satuan besar Karton, 1 Karton = 6000 Gram; pesan dalam kg.
        val f = DistribusiUnit.faktor("Karton", null, null, "Gram", 6000.0, "kg")
        assertEquals(6.0, f, 0.0001)
        // Diminta 12 kg -> tersimpan 2 Karton (satuan besar).
        assertEquals(2.0, DistribusiUnit.keBase(12.0, f), 0.0001)
    }

    @Test
    fun `gram ke besar membagi faktor tampilan`() {
        assertEquals(2.5, DistribusiUnit.gramKeBesar(2500.0, "Gram", 1000.0), 0.0001)
        // Tanpa satuan kecil, skala gram = skala besar.
        assertEquals(7.0, DistribusiUnit.gramKeBesar(7.0, null, null), 0.0001)
    }

    @Test
    fun `saldo ke besar menghormati penanda skala baris`() {
        val meta = UnitMeta(satuan = "Kg", satuanKecil = "Gram", faktorTampilan = 1000.0)
        assertEquals(3.0, DistribusiUnit.saldoKeBesar(3000.0, true, meta), 0.0001)
        assertEquals(3000.0, DistribusiUnit.saldoKeBesar(3000.0, false, meta), 0.0001)
    }
}
