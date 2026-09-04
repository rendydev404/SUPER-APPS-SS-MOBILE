package com.sukashawarma.superapp.feature.stok.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Konversi satuan pesan dan normalisasi skala saldo — nilai harapan dari
 * `getDistribusiFactor`/`convertToBaseUnit`/`convertGramToBesar` di
 * `apps/stok/src/lib/format/compositeUnit.ts`.
 */
class DistribusiUnitTest {

    /**
     * Faktor bahan nyata di produksi, dipakai dua arah: qty pesanan -> qty tersimpan,
     * dan qty pesanan -> skala harga. `harga_beli` berharga per satuan BESAR, jadi
     * BAWANG 1 kg bernilai 650.000 / 20 = 32.500, bukan 650.000.
     */
    @Test
    fun `faktor dan skala harga bahan produksi`() {
        // BAWANG: besar Bal, tengah Kg (20 per Bal), pesan kg, harga 650.000 per Bal.
        val bawang = DistribusiUnit.faktor("Bal", "Kg", 20.0, "Gram", 20000.0, "kg")
        assertEquals(20.0, bawang, 0.0001)
        assertEquals(0.05, DistribusiUnit.keBase(1.0, bawang), 0.0001)
        assertEquals(32_500.0, DistribusiUnit.keBase(1.0, bawang) * 650_000.0, 0.01)

        // KEJU: besar Dus, tengah Pack (24 per Dus), pesan pack, harga 289.056 per Dus.
        val keju = DistribusiUnit.faktor("Dus", "Pack", 24.0, "Lembar", 240.0, "pack")
        assertEquals(24.0, keju, 0.0001)
        assertEquals(12_044.0, DistribusiUnit.keBase(1.0, keju) * 289_056.0, 0.01)

        // KERTAS STRUK: besar pack, kecil Roll (10 per pack), pesan roll.
        assertEquals(10.0, DistribusiUnit.faktor("pack", null, null, "Roll", 10.0, "roll"), 0.0001)

        // AYAM: satuan pesan sama dengan satuan besar — tanpa konversi.
        val ayam = DistribusiUnit.faktor("Kg", null, null, "Gram", 1000.0, "kg")
        assertEquals(1.0, ayam, 0.0001)
        assertEquals(53_500.0, DistribusiUnit.keBase(1.0, ayam) * 53_500.0, 0.01)
    }

    @Test
    fun `tanpa satuan pesan faktornya satu`() {
        assertEquals(1.0, DistribusiUnit.faktor("Dus", "Pack", 20.0, "Lembar", 400.0, null), 0.0001)
        assertEquals(1.0, DistribusiUnit.faktor("Dus", "Pack", 20.0, "Lembar", 400.0, "dus"), 0.0001)
    }

    @Test
    fun `pemetaan implisit kg atas gram`() {
        // SAOS CABE: besar Karton, 1 Karton = 6000 Gram; pesan kg -> faktor 6.
        val f = DistribusiUnit.faktor("Karton", null, null, "Gram", 6000.0, "kg")
        assertEquals(6.0, f, 0.0001)
        assertEquals(2.0, DistribusiUnit.keBase(12.0, f), 0.0001)
    }

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
