package com.sukashawarma.superapp.feature.manager.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PersetujuanTest {

    @Test
    fun `tab persetujuan memiliki tiga tab dengan label yang tepat`() {
        val tabs = TabPersetujuan.entries
        assertEquals(3, tabs.size)
        assertEquals("Void Transaksi", TabPersetujuan.VOID.label)
        assertEquals("Bypass POS", TabPersetujuan.BYPASS.label)
        assertEquals("Pesanan Selesai", TabPersetujuan.BATAL_PAKSA.label)
    }

    @Test
    fun `pesanan selesai menyimpan informasi transaksi dan rincian item dengan benar`() {
        val item1 = ItemPesananVoid(nama = "Shawarma Beef Large", qty = 2, subtotal = 70000L)
        val item2 = ItemPesananVoid(nama = "Ice Lemon Tea", qty = 2, subtotal = 20000L)
        val pesanan = PesananSelesaiItem(
            id = "order-123",
            outletId = "outlet-empang",
            outletNama = "SS EMPANG",
            nomorOrder = "1042",
            namaPelanggan = "Ahmad Dani",
            total = 90000L,
            dibuatPada = "2026-09-15T12:30:00+07:00",
            items = listOf(item1, item2),
        )

        assertEquals("order-123", pesanan.id)
        assertEquals("outlet-empang", pesanan.outletId)
        assertEquals("SS EMPANG", pesanan.outletNama)
        assertEquals("1042", pesanan.nomorOrder)
        assertEquals("Ahmad Dani", pesanan.namaPelanggan)
        assertEquals(90000L, pesanan.total)
        assertEquals(2, pesanan.items.size)
        assertEquals(70000L, pesanan.items[0].subtotal)
    }

    @Test
    fun `saringPeriode menyaring pesanan selesai berdasarkan rentang tanggal`() {
        val pesanan1 = PesananSelesaiItem(
            id = "o1",
            outletId = "out1",
            outletNama = "SS Sentul",
            nomorOrder = "1",
            namaPelanggan = "Budi",
            total = 50000L,
            dibuatPada = "2026-09-10T10:00:00+07:00",
        )
        val pesanan2 = PesananSelesaiItem(
            id = "o2",
            outletId = "out1",
            outletNama = "SS Sentul",
            nomorOrder = "2",
            namaPelanggan = "Siti",
            total = 35000L,
            dibuatPada = "2026-09-15T14:00:00+07:00",
        )

        val rentang = RentangTanggal(LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 16))
        val hasil = saringPeriode(listOf(pesanan1, pesanan2), rentang) { it.dibuatPada }

        assertEquals(1, hasil.size)
        assertEquals("o2", hasil[0].id)
    }
}