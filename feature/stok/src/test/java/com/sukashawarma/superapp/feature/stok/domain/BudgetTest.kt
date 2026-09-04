package com.sukashawarma.superapp.feature.stok.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/** Ambang diambil dari `budgetBadgeVariant` di `apps/stok/src/lib/stok/budget.ts`. */
class BudgetTest {

    @Test
    fun `tanpa konfigurasi badge disembunyikan`() {
        assertEquals(BudgetVarian.TERSEMBUNYI, Budget.varian(false, 5_000_000.0, 0.0))
    }

    @Test
    fun `nominal nol selalu merah`() {
        assertEquals(BudgetVarian.MERAH, Budget.varian(true, 0.0, 0.0))
    }

    @Test
    fun `ambang delapan puluh dan seratus persen`() {
        assertEquals(BudgetVarian.HIJAU, Budget.varian(true, 100.0, 79.9))
        assertEquals(BudgetVarian.ORANYE, Budget.varian(true, 100.0, 80.0))
        assertEquals(BudgetVarian.ORANYE, Budget.varian(true, 100.0, 100.0))
        assertEquals(BudgetVarian.MERAH, Budget.varian(true, 100.0, 100.01))
    }

    @Test
    fun `proyeksi keranjang ikut dihitung`() {
        // Kasus nyata di web: plafon 5.000.000, terpakai 8.791, keranjang 922.215 -> hijau.
        assertEquals(BudgetVarian.HIJAU, Budget.varian(true, 5_000_000.0, 8_791.0, 922_215.0))
        assertEquals(BudgetVarian.MERAH, Budget.varian(true, 100.0, 50.0, 60.0))
    }

    @Test
    fun `label periode`() {
        assertEquals("Hari Ini", Budget.labelPeriode("harian", null))
        assertEquals("3 Hari Ini", Budget.labelPeriode("custom", 3))
        assertEquals("Periode Ini", Budget.labelPeriode("custom", null))
        assertEquals("", Budget.labelPeriode(null, 3))
    }

    @Test
    fun `maksimal top-up tidak pernah negatif`() {
        assertEquals(1_000_000.0, Budget.maksTopUp(5_000_000.0, 4_000_000.0), 0.0001)
        assertEquals(5_000_000.0, Budget.maksTopUp(5_000_000.0, -200.0), 0.0001)
        assertEquals(0.0, Budget.maksTopUp(1_000.0, 5_000.0), 0.0001)
    }
}
