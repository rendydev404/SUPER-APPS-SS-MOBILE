package com.sukashawarma.superapp.feature.stok.data

import com.sukashawarma.superapp.feature.stok.data.model.StatusWaste
import com.sukashawarma.superapp.feature.stok.domain.UnitMeta
import com.sukashawarma.superapp.feature.stok.domain.decomposeTriUnit
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Skala satuan pada dua layar baru berlawanan arah, dan tertukarnya formatter adalah
 * bug 1000× yang sama seperti "11278 Kg" di SPVTable web dulu.
 *
 * - `stok_waste_reports.qty` -> satuan BESAR (WasteModal web membaginya sebelum simpan)
 * - `opname_item.qty_*`      -> satuan TERKECIL (OpnameForm menghitungnya begitu)
 *
 * Tes ini mengunci keduanya berdampingan supaya selisihnya terlihat kalau ada yang
 * memindahkan salah satu ke formatter yang lain.
 */
class RiwayatWasteTest {

    // AYAM: satuan besar Kg, satuan kecil Gram, 1 Kg = 1000 Gram.
    private val ayam = UnitMeta(
        satuan = "Kg",
        satuanTengah = null,
        satuanKecil = "Gram",
        faktorTengah = null,
        faktorTampilan = 1000.0,
    )

    private fun waste(qty: Double) = RiwayatWaste(
        id = "w1",
        namaBahan = "AYAM",
        qty = qty,
        alasan = "Basi / Expired",
        status = StatusWaste.PENDING,
        alasanPenolakan = null,
        fotoUrl = null,
        createdAt = null,
        meta = ayam,
    )

    @Test
    fun `waste pecahan kilogram tampil sebagai gram`() {
        // Justru kasus inilah yang di web tampil "0.25 Kg" — angka desimal mentah.
        assertEquals("250 Gram", waste(0.25).qtyLabel)
    }

    @Test
    fun `waste lebih dari satu kilogram tampil berjenjang`() {
        assertEquals("1 Kg · 250 Gram", waste(1.25).qtyLabel)
    }

    @Test
    fun `waste bulat tidak menumbuhkan bagian gram palsu`() {
        assertEquals("2 Kg", waste(2.0).qtyLabel)
    }

    @Test
    fun `waste nol tetap terbaca dengan satuan besarnya`() {
        assertEquals("0 Kg", waste(0.0).qtyLabel)
    }

    /**
     * Sisi seberangnya: angka opname yang nilainya 11278 adalah 11 Kg 278 Gram,
     * BUKAN 11278 Kg. Kalau baris ini mulai gagal, ada yang menukar `saldoIsGram`.
     */
    @Test
    fun `angka opname dibaca sebagai satuan terkecil bukan satuan besar`() {
        val gram = decomposeTriUnit(11278.0, true, null, null, "Gram", 1000.0)
        assertEquals(11.0, gram.besar, 0.0001)
        assertEquals(278.0, gram.kecil, 0.0001)

        // Angka yang sama lewat jalur waste berarti 11278 Kg — jauh berbeda, dan
        // itulah sebabnya kedua formatter tidak boleh dipakai bergantian.
        assertEquals(11278.0, waste(11278.0).let { decomposeTriUnit(it.qty, false, null, null, "Gram", 1000.0) }.besar, 0.0001)
    }
}
