package com.sukashawarma.superapp.feature.stok.domain

import com.sukashawarma.superapp.feature.stok.data.model.LedgerTransaksi
import com.sukashawarma.superapp.feature.stok.ui.ledger.KategoriLedger
import com.sukashawarma.superapp.feature.stok.ui.ledger.LedgerUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LedgerFilterTest {

    private val tOrder = LedgerTransaksi(
        transaksiKey = "t1",
        outletId = "out1",
        createdAt = "2026-09-17T10:00:00Z",
        jumlahBahan = 5,
        refOrderId = "order-123",
        refOpnameId = null,
        refShipmentId = null,
        refTransferId = null,
        singleBahanBakuId = null,
        singleTipe = "pemakaian",
        singleQty = -5.0,
        singleCatatan = "Penjualan Otomatis #42",
        singleSaldoSesudah = null,
        orderNumber = 42,
        orderItemsNames = "Original Sapi Jumbo, Teh Manis",
    )

    private val tInboundShipment = LedgerTransaksi(
        transaksiKey = "t2",
        outletId = "out1",
        createdAt = "2026-09-17T09:00:00Z",
        jumlahBahan = 1,
        refOrderId = null,
        refOpnameId = null,
        refShipmentId = "sj-001",
        refTransferId = null,
        singleBahanBakuId = "b1",
        singleTipe = "terima_kiriman",
        singleQty = 10.0,
        singleCatatan = "Terima dari Gudang",
        singleSaldoSesudah = 50.0,
        singleNamaBahan = "DAGING AYAM",
    )

    private val tVendor = LedgerTransaksi(
        transaksiKey = "t3",
        outletId = "out1",
        createdAt = "2026-09-17T08:00:00Z",
        jumlahBahan = 1,
        refOrderId = null,
        refOpnameId = null,
        refShipmentId = null,
        refTransferId = null,
        singleBahanBakuId = "b2",
        singleTipe = "pembelian_supplier",
        singleQty = 20.0,
        singleCatatan = "Terima langsung vendor",
        singleSaldoSesudah = 100.0,
        singleNamaBahan = "SAYUR LETTUCE",
    )

    private val tWaste = LedgerTransaksi(
        transaksiKey = "t4",
        outletId = "out1",
        createdAt = "2026-09-17T07:00:00Z",
        jumlahBahan = 1,
        refOrderId = null,
        refOpnameId = null,
        refShipmentId = null,
        refTransferId = null,
        singleBahanBakuId = "b1",
        singleTipe = "waste",
        singleQty = -2.0,
        singleCatatan = "Basi/Rusak",
        singleSaldoSesudah = 48.0,
        singleNamaBahan = "DAGING AYAM",
    )

    private val tWastePending = LedgerTransaksi(
        transaksiKey = "t5",
        outletId = "out1",
        createdAt = "2026-09-17T06:00:00Z",
        jumlahBahan = 1,
        refOrderId = null,
        refOpnameId = null,
        refShipmentId = null,
        refTransferId = null,
        singleBahanBakuId = "b3",
        singleTipe = "waste_pending",
        singleQty = -1.0,
        singleCatatan = "Jatuh ke lantai",
        singleSaldoSesudah = null,
        singleNamaBahan = "KULIT 25",
    )

    private val tOutboundTransfer = LedgerTransaksi(
        transaksiKey = "t6",
        outletId = "out1",
        createdAt = "2026-09-17T05:00:00Z",
        jumlahBahan = 1,
        refOrderId = null,
        refOpnameId = null,
        refShipmentId = null,
        refTransferId = "tr-001",
        singleBahanBakuId = "b1",
        singleTipe = "transfer_keluar",
        singleQty = -5.0,
        singleCatatan = "Transfer ke cabang lain",
        singleSaldoSesudah = 43.0,
        singleNamaBahan = "DAGING AYAM",
    )

    private val tOpname = LedgerTransaksi(
        transaksiKey = "t7",
        outletId = "out1",
        createdAt = "2026-09-17T04:00:00Z",
        jumlahBahan = 10,
        refOrderId = null,
        refOpnameId = "op-001",
        refShipmentId = null,
        refTransferId = null,
        singleBahanBakuId = null,
        singleTipe = "opname_selisih",
        singleQty = 0.0,
        singleCatatan = "Hasil opname bulanan",
        singleSaldoSesudah = null,
        opnameTanggal = "2026-09-17",
        opnameTipe = "HARIAN",
    )

    private val semuaTransaksi = listOf(
        tOrder, tInboundShipment, tVendor, tWaste, tWastePending, tOutboundTransfer, tOpname
    )

    @Test
    fun `kategori SEMUA mengembalikan seluruh transaksi`() {
        val state = LedgerUiState(transaksi = semuaTransaksi, filterAktif = KategoriLedger.SEMUA)
        assertEquals(7, state.transaksiTerfilter.size)
    }

    @Test
    fun `kategori MASUK menyaring shipment, transfer masuk, dan terima vendor`() {
        val state = LedgerUiState(transaksi = semuaTransaksi, filterAktif = KategoriLedger.MASUK)
        val hasil = state.transaksiTerfilter
        assertEquals(2, hasil.size)
        assertTrue(hasil.any { it.transaksiKey == "t2" })
        assertTrue(hasil.any { it.transaksiKey == "t3" })
    }

    @Test
    fun `kategori ORDER hanya menyaring transaksi penjualan atau order`() {
        val state = LedgerUiState(transaksi = semuaTransaksi, filterAktif = KategoriLedger.ORDER)
        val hasil = state.transaksiTerfilter
        assertEquals(1, hasil.size)
        assertEquals("t1", hasil[0].transaksiKey)
    }

    @Test
    fun `kategori WASTE menyaring waste dan waste pending`() {
        val state = LedgerUiState(transaksi = semuaTransaksi, filterAktif = KategoriLedger.WASTE)
        val hasil = state.transaksiTerfilter
        assertEquals(2, hasil.size)
        assertTrue(hasil.any { it.transaksiKey == "t4" })
        assertTrue(hasil.any { it.transaksiKey == "t5" })
    }

    @Test
    fun `kategori KELUAR menyaring transfer keluar dan pemakaian manual`() {
        val state = LedgerUiState(transaksi = semuaTransaksi, filterAktif = KategoriLedger.KELUAR)
        val hasil = state.transaksiTerfilter
        assertEquals(1, hasil.size)
        assertEquals("t6", hasil[0].transaksiKey)
    }

    @Test
    fun `kategori PENYESUAIAN menyaring opname dan adjustment`() {
        val state = LedgerUiState(transaksi = semuaTransaksi, filterAktif = KategoriLedger.PENYESUAIAN)
        val hasil = state.transaksiTerfilter
        assertEquals(1, hasil.size)
        assertEquals("t7", hasil[0].transaksiKey)
    }

    @Test
    fun `pencarian nama bahan menyaring dengan benar`() {
        val state = LedgerUiState(transaksi = semuaTransaksi, kataKunci = "lettuce")
        val hasil = state.transaksiTerfilter
        assertEquals(1, hasil.size)
        assertEquals("t3", hasil[0].transaksiKey)
    }

    @Test
    fun `pencarian nomor order menyaring transaksi order`() {
        val state = LedgerUiState(transaksi = semuaTransaksi, kataKunci = "42")
        val hasil = state.transaksiTerfilter
        assertEquals(1, hasil.size)
        assertEquals("t1", hasil[0].transaksiKey)
    }

    @Test
    fun `pencarian nama menu order menyaring transaksi order`() {
        val state = LedgerUiState(transaksi = semuaTransaksi, kataKunci = "sapi jumbo")
        val hasil = state.transaksiTerfilter
        assertEquals(1, hasil.size)
        assertEquals("t1", hasil[0].transaksiKey)
    }
}
