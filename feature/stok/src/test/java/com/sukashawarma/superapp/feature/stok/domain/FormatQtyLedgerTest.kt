package com.sukashawarma.superapp.feature.stok.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * `ledger_stok.qty` ditulis pada skala baris saldo, bukan selalu satuan terkecil.
 * Pernah dibaca seolah selalu terkecil, dan 50 kg tampil sebagai "4 Kg 2 Gram".
 */
class FormatQtyLedgerTest {

    private val mayonaise = UnitMeta(
        satuan = "Kg",
        satuanTengah = "Pouch",
        satuanKecil = "Gram",
        faktorTengah = 12.0,
        faktorTampilan = 1000.0,
    )

    @Test
    fun `baris satuan besar tidak dipecah berjenjang`() {
        assertEquals("-50 Kg", UnitScale.formatQtyLedger(-50.0, mayonaise, saldoIsGram = false))
    }

    @Test
    fun `baris satuan terkecil dipecah berjenjang`() {
        assertEquals("-4 Kg 2 Gram", UnitScale.formatQtyLedger(-4002.0, mayonaise, saldoIsGram = true))
    }

    /**
     * Sisa di bawah satu satuan besar turun ke jenjang tengah. Metadatanya data nyata
     * `bahan_baku` MAYONAISE: 1 Dus = 12 Kg = 12.000 Gram — `faktor_tengah` adalah
     * jumlah Kg per Dus, bukan gram per Kg.
     */
    @Test
    fun `sisa di bawah satu satuan besar turun ke jenjang tengah`() {
        val mayonaiseDus = UnitMeta(
            satuan = "Dus",
            satuanTengah = "Kg",
            satuanKecil = "Gram",
            faktorTengah = 12.0,
            faktorTampilan = 12000.0,
        )
        assertEquals("-1 Kg 50 Gram", UnitScale.formatQtyLedger(-1050.0, mayonaiseDus, saldoIsGram = true))
    }

    @Test
    fun `pergerakan masuk diberi tanda tambah`() {
        assertEquals("+3 Kg", UnitScale.formatQtyLedger(3.0, mayonaise, saldoIsGram = false))
    }

    @Test
    fun `faktor kosong tidak ditebak jadi satu`() {
        val tanpaFaktor = UnitMeta(satuan = "Kg", satuanKecil = "Gram")
        assertEquals("-50 Gram", UnitScale.formatQtyLedger(-50.0, tanpaFaktor, saldoIsGram = true))
    }

    @Test
    fun `bahan tanpa metadata tetap menampilkan angkanya`() {
        assertEquals("-50", UnitScale.formatQtyLedger(-50.0, null, saldoIsGram = false))
    }
}
