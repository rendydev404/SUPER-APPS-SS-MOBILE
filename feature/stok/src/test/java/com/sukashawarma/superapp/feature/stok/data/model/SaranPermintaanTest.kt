package com.sukashawarma.superapp.feature.stok.data.model

import com.sukashawarma.superapp.feature.stok.domain.StokStatus
import com.sukashawarma.superapp.feature.stok.domain.UnitMeta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Mengunci pemisahan antara `status` milik view dan status yang dihitung aplikasi.
 *
 * `monitoring_view_crew` membandingkan `saldo` mentah dengan `threshold`, padahal
 * saldo bisa berada di satuan terkecil (`saldo_is_gram`) sementara threshold selalu
 * di satuan besar. Untuk bahan berfaktor bukan 1, perbandingan itu selalu meleset ke
 * arah yang sama: bahan yang benar-benar kritis dilaporkan `ok`. Dulu layar Permintaan
 * menyaring memakai kolom itu, sehingga bahan tersebut hilang dari katalog dan tidak
 * bisa diminta sama sekali.
 */
class SaranPermintaanTest {

    /** Bahan berfaktor besar: 1 kg = 1000 gr. */
    private val kg = UnitMeta(
        satuan = "kg",
        satuanTengah = "pak",
        satuanKecil = "gr",
        faktorTengah = 250.0,
        faktorTampilan = 1000.0,
    )

    private fun baris(
        saldo: Double,
        threshold: Double = 5.0,
        saldoIsGram: Boolean = true,
        statusView: String? = "ok",
    ) = SaranPermintaan(
        bahanBakuId = "b1",
        itemName = "Ayam",
        satuan = "kg",
        currentQty = saldo,
        saldoIsGram = saldoIsGram,
        threshold = threshold,
        statusView = statusView,
    )

    @Test
    fun `saldo gram di bawah separuh threshold terbaca kritis walau view bilang ok`() {
        // Threshold 5 kg = 5000 gr, separuhnya 2500 gr. Saldo 1000 gr jelas kritis.
        // View membandingkan 1000 dengan 2.5 lalu dengan 5, dan menyimpulkan 'ok'.
        val row = baris(saldo = 1000.0, statusView = "ok")
        assertEquals(StokStatus.BELOW, row.status(kg))
        assertTrue(row.perluDiminta(kg))
    }

    @Test
    fun `saldo gram di antara separuh dan penuh terbaca menipis walau view bilang ok`() {
        // 3000 gr: di atas separuh (2500) tapi di bawah threshold penuh (5000).
        val row = baris(saldo = 3000.0, statusView = "ok")
        assertEquals(StokStatus.WARNING, row.status(kg))
        assertTrue(row.perluDiminta(kg))
    }

    @Test
    fun `bahan yang memang aman tidak ikut ditawarkan`() {
        val row = baris(saldo = 8000.0, statusView = "ok")
        assertEquals(StokStatus.OK, row.status(kg))
        assertFalse(row.perluDiminta(kg))
    }

    @Test
    fun `status view below tetap dihormati walau hitungan saldo bilang aman`() {
        // Inilah aturan porsi resep (`marquee_warning_threshold`) yang hanya ada di
        // server. Menggabungkan kedua sumber tidak pernah menyembunyikan bahan.
        val row = baris(saldo = 8000.0, statusView = "below")
        assertEquals(StokStatus.OK, row.status(kg))
        assertTrue(row.perluDiminta(kg))
    }

    @Test
    fun `faktor satuan tak dapat dipercaya jatuh kembali ke status view`() {
        val tanpaFaktor = kg.copy(faktorTampilan = null)
        assertEquals(StokStatus.UNKNOWN, baris(saldo = 1000.0).status(tanpaFaktor))
        assertFalse(baris(saldo = 1000.0, statusView = "ok").perluDiminta(tanpaFaktor))
        assertTrue(baris(saldo = 1000.0, statusView = "warning").perluDiminta(tanpaFaktor))
    }

    @Test
    fun `saldo yang sudah di satuan besar tidak terpengaruh perbaikan ini`() {
        // saldo_is_gram = false: kedua sisi dikali faktor yang sama, rasionya tetap,
        // jadi hasilnya sama dengan yang dihitung view.
        val aman = baris(saldo = 8.0, saldoIsGram = false, statusView = "ok")
        val kritis = baris(saldo = 2.0, saldoIsGram = false, statusView = "below")
        assertEquals(StokStatus.OK, aman.status(kg))
        assertEquals(StokStatus.BELOW, kritis.status(kg))
    }
}
