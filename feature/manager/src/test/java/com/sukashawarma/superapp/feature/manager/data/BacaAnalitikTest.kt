package com.sukashawarma.superapp.feature.manager.data

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pemetaan balasan `native_laporan_analitik` ke model layar.
 *
 * Yang dijaga di sini adalah hal-hal yang TIDAK boleh dititipkan ke server:
 * panjang daftar per jam (layar mengindeksnya dengan jam mentah) dan aturan jam
 * tersibuk.
 */
class BacaAnalitikTest {

    private fun baca(json: String) =
        LaporanRepository.bacaAnalitik(JsonParser.parseString(json).asJsonObject)

    @Test
    fun `angka pokok dibaca apa adanya`() {
        val a = baca(
            """
            {"omzet_bersih":1500000,"omzet_kotor":1650000,"potongan_merchant":150000,
             "subsidi_platform":25000,"pesanan_sukses":40,"pesanan_batal":10,
             "item_terjual":95,"rata_rata_per_order":37500,
             "rincian_pembayaran":[{"metode":"qris","jumlah":25,"omzet":1000000}],
             "per_jam":[], "daftar_item":[{"nama":"Original Sapi","qty":30,"omzet":900000}]}
            """,
        )
        assertEquals(1_500_000L, a.omzetBersih)
        assertEquals(1_650_000L, a.omzetKotor)
        assertEquals(40, a.pesananSukses)
        assertEquals(10, a.pesananBatal)
        assertEquals(80, a.persenSukses)
        assertEquals("QRIS", a.rincianPembayaran.first().label)
        assertEquals("Original Sapi", a.daftarItem.first().nama)
    }

    @Test
    fun `per jam selalu 24 slot walau server mengirim kurang`() {
        val a = baca("""{"per_jam":[3,5]}""")
        assertEquals(24, a.perJam.size)
        assertEquals(listOf(3, 5), a.perJam.take(2))
        assertEquals(0, a.perJam[23])
    }

    @Test
    fun `jam tersibuk adalah yang pertama mencapai puncak`() {
        // Jam 9 dan 18 sama-sama 12. Yang lebih awal yang dipilih, supaya dua
        // pemuatan berturut-turut tidak menampilkan jam berbeda.
        val perJam = MutableList(24) { 0 }
        perJam[9] = 12
        perJam[18] = 12
        val a = baca("""{"per_jam":${perJam.joinToString(",", "[", "]")}}""")
        assertEquals(9, a.jamTersibuk)
    }

    @Test
    fun `periode tanpa transaksi tidak punya jam tersibuk`() {
        val a = baca("""{"per_jam":${List(24) { 0 }.joinToString(",", "[", "]")}}""")
        assertEquals(null, a.jamTersibuk)
        assertEquals(0, a.persenSukses)
    }

    @Test
    fun `metode bayar tak dikenal tetap diberi label`() {
        val a = baca("""{"rincian_pembayaran":[{"metode":"unknown","jumlah":2,"omzet":50000}]}""")
        assertEquals("Lainnya", a.rincianPembayaran.first().label)
    }
}
