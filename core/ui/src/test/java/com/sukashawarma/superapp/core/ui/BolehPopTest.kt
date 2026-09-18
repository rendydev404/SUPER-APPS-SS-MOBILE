package com.sukashawarma.superapp.core.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Penjaga regresi layar putih: tekan-beruntun tombol kembali pernah mengosongkan
 * tumpukan navigasi sampai NavHost tidak punya apa pun untuk digambar.
 */
class BolehPopTest {

    @Test
    fun `layar diam dengan tujuan di bawahnya boleh mundur`() {
        assertTrue(bolehPop(adaTujuanSebelumnya = true, entriStabil = true))
    }

    @Test
    fun `tekanan kedua saat animasi masih jalan diabaikan`() {
        assertFalse(bolehPop(adaTujuanSebelumnya = true, entriStabil = false))
    }

    @Test
    fun `layar terakhir tidak ikut dibuang`() {
        assertFalse(bolehPop(adaTujuanSebelumnya = false, entriStabil = true))
    }
}
