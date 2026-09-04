package com.sukashawarma.superapp.feature.stok.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Normalisasi skala saldo — nilai harapan dari `convertGramToBesar` di
 * `apps/stok/src/lib/format/compositeUnit.ts`.
 *
 * Konversi satuan distribusi sudah tidak ada di sini: native memesan pada satuan besar
 * (lihat `BahanBaku.satuanPesan`), jadi tidak ada faktor yang perlu diterapkan.
 */
class DistribusiUnitTest {

    @Test
    fun `gram ke besar membagi faktor tampilan`() {
        assertEquals(2.5, DistribusiUnit.gramKeBesar(2500.0, "Gram", 1000.0), 0.0001)
        // Tanpa satuan kecil, skala gram = skala besar.
        assertEquals(7.0, DistribusiUnit.gramKeBesar(7.0, null, null), 0.0001)
        // Faktor tak dapat dipercaya -> nilai dipakai apa adanya, bukan ditebak.
        assertEquals(7.0, DistribusiUnit.gramKeBesar(7.0, "Gram", 0.0), 0.0001)
    }

    @Test
    fun `saldo ke besar menghormati penanda skala baris`() {
        val meta = UnitMeta(satuan = "Kg", satuanKecil = "Gram", faktorTampilan = 1000.0)
        assertEquals(3.0, DistribusiUnit.saldoKeBesar(3000.0, true, meta), 0.0001)
        assertEquals(3000.0, DistribusiUnit.saldoKeBesar(3000.0, false, meta), 0.0001)
    }

    /**
     * Qty permintaan yang tersimpan selalu pada satuan besar dan bisa pecahan bila
     * dibuat dari web. Ditampilkan berjenjang, angka itu harus terbaca sebagai jumlah
     * yang sebenarnya diminta — bukan dibulatkan menjadi satu satuan besar penuh.
     */
    @Test
    fun `qty pecahan dari web terbaca berjenjang`() {
        // KEJU: 1 Dus = 24 Pack = 240 Lembar. 5 Pack tersimpan sebagai 0,2083 Dus.
        val keju = UnitMeta(
            satuan = "Dus", satuanTengah = "Pack", satuanKecil = "Lembar",
            faktorTengah = 24.0, faktorTampilan = 240.0,
        )
        assertEquals("5 Pack", formatTriUnitAdaptif(5.0 / 24.0, saldoIsGram = false, meta = keju))

        // Pesanan native (satuan besar bulat) tetap tampil sederhana.
        assertEquals("2 Dus", formatTriUnitAdaptif(2.0, saldoIsGram = false, meta = keju))

        // BAWANG: 1 Bal = 20 Kg. Pesanan lama 1 kg tersimpan 0,05 Bal.
        val bawang = UnitMeta(
            satuan = "Bal", satuanTengah = "Kg", satuanKecil = "Gram",
            faktorTengah = 20.0, faktorTampilan = 20000.0,
        )
        assertEquals("1 Kg", formatTriUnitAdaptif(0.05, saldoIsGram = false, meta = bawang))
        assertEquals("1 Bal", formatTriUnitAdaptif(1.0, saldoIsGram = false, meta = bawang))
    }
}
