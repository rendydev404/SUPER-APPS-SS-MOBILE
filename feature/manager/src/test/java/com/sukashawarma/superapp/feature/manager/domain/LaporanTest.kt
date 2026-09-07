package com.sukashawarma.superapp.feature.manager.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class LaporanTest {

    private val kamis = LocalDate.of(2026, 9, 3)

    private fun pesanan(
        status: String = "completed",
        metode: String? = "cash",
        total: Long = 50_000,
        diskon: Long = 0,
        subsidi: Long = 0,
        jam: Int? = 12,
        items: List<ItemPesanan> = listOf(ItemPesanan("Shawarma", 1, 50_000)),
    ) = PesananLaporan(status, metode, total, diskon, subsidi, jam, items)

    /* ---------------- rentang ---------------- */

    @Test
    fun `preset laporan menghasilkan rentang yang sama dengan label yang dijanjikan`() {
        assertEquals(RentangTanggal(kamis, kamis), PresetLaporan.HARI_INI.rentang(kamis))
        assertEquals(
            RentangTanggal(LocalDate.of(2026, 9, 2), LocalDate.of(2026, 9, 2)),
            PresetLaporan.KEMARIN.rentang(kamis),
        )
        assertEquals(7L, PresetLaporan.TUJUH_HARI.rentang(kamis).jumlahHari)
        assertEquals(30L, PresetLaporan.TIGA_PULUH_HARI.rentang(kamis).jumlahHari)
    }

    @Test
    fun `semua waktu dimulai jauh sebelum baris tertua di database`() {
        val r = PresetLaporan.SEMUA_WAKTU.rentang(kamis)
        assertEquals(LocalDate.of(1970, 1, 1), r.dari)
        assertEquals(kamis, r.sampai)
    }

    /* ---------------- channel ---------------- */

    /** Satu ejaan saja akan menyembunyikan pesanan yang ditulis versi POS lain. */
    @Test
    fun `tiktok dicocokkan ke tiga ejaan yang pernah tersimpan`() {
        assertEquals(listOf("tiktokgo", "tiktok", "tiktok_go"), FilterChannel.TIKTOKGO.nilaiDb)
        assertTrue(FilterChannel.FOOD_APPS.nilaiDb.containsAll(FilterChannel.TIKTOKGO.nilaiDb))
    }

    @Test
    fun `hanya walk-in yang disaring lewat kolom kosong`() {
        assertTrue(FilterChannel.OFFLINE.kolomKosong)
        FilterChannel.entries.filter { it != FilterChannel.OFFLINE }
            .forEach { assertTrue("${it.kunci} tidak boleh pakai NULL", !it.kolomKosong) }
    }

    /* ---------------- nama menu ---------------- */

    @Test
    fun `varian menu digabung ke menu induk`() {
        assertEquals("Shawarma Beef", namaMenuPokok("Shawarma Beef | Pedas"))
        assertEquals("Shawarma Beef", namaMenuPokok("Shawarma Beef"))
        assertEquals("Item", namaMenuPokok(null))
        assertEquals("Item", namaMenuPokok(""))
    }

    /* ---------------- potongan ---------------- */

    @Test
    fun `potongan diambil dari selisih nilai item terhadap total`() {
        val p = pesanan(total = 45_000, items = listOf(ItemPesanan("A", 1, 50_000)))
        assertEquals(5_000L, potonganPesanan(p))
    }

    /** total_amount di atas nilai item bukan berarti potongan negatif. */
    @Test
    fun `potongan tidak pernah negatif`() {
        val p = pesanan(total = 60_000, items = listOf(ItemPesanan("A", 1, 50_000)))
        assertEquals(0L, potonganPesanan(p))
    }

    @Test
    fun `pesanan tanpa item jatuh ke kolom diskon dan subsidi`() {
        val p = pesanan(total = 40_000, diskon = 3_000, subsidi = 2_000, items = emptyList())
        assertEquals(5_000L, potonganPesanan(p))
    }

    /** Kalau kolom diskon ikut dipakai saat item ada, promo terhitung dua kali. */
    @Test
    fun `kolom diskon diabaikan ketika item tersedia`() {
        val p = pesanan(total = 45_000, diskon = 99_000, subsidi = 99_000, items = listOf(ItemPesanan("A", 1, 50_000)))
        assertEquals(5_000L, potonganPesanan(p))
    }

    /* ---------------- analitik ---------------- */

    @Test
    fun `omzet kotor adalah omzet bersih ditambah potongan`() {
        val a = susunAnalitikLaporan(
            listOf(pesanan(total = 45_000, items = listOf(ItemPesanan("A", 1, 50_000)))),
        )
        assertEquals(45_000L, a.omzetBersih)
        assertEquals(5_000L, a.potonganMerchant)
        assertEquals(50_000L, a.omzetKotor)
    }

    @Test
    fun `subsidi platform dilaporkan terpisah dan tidak menambah omzet`() {
        val a = susunAnalitikLaporan(
            listOf(pesanan(total = 50_000, subsidi = 8_000, items = listOf(ItemPesanan("A", 1, 50_000)))),
        )
        assertEquals(8_000L, a.subsidiPlatform)
        assertEquals(50_000L, a.omzetKotor)
        assertEquals(0L, a.potonganMerchant)
    }

    @Test
    fun `hanya pesanan selesai yang masuk hitungan uang`() {
        val a = susunAnalitikLaporan(
            listOf(
                pesanan(status = "completed", total = 50_000),
                pesanan(status = "cancelled", total = 90_000),
                pesanan(status = "pending", total = 70_000),
            ),
        )
        assertEquals(50_000L, a.omzetBersih)
        assertEquals(1, a.pesananSukses)
        assertEquals(1, a.pesananBatal)
    }

    @Test
    fun `persentase sukses dan batal hanya memperhitungkan yang selesai dan batal`() {
        val a = susunAnalitikLaporan(
            listOf(
                pesanan(status = "completed"),
                pesanan(status = "completed"),
                pesanan(status = "completed"),
                pesanan(status = "cancelled"),
                // Pesanan tertunda bukan kegagalan; ia belum diputuskan.
                pesanan(status = "pending"),
            ),
        )
        assertEquals(75, a.persenSukses)
        assertEquals(25, a.persenBatal)
    }

    @Test
    fun `periode tanpa pesanan tidak membagi dengan nol`() {
        val a = susunAnalitikLaporan(emptyList())
        assertEquals(0, a.persenSukses)
        assertEquals(0, a.persenBatal)
        assertEquals(0L, a.rataRataPerOrder)
        assertNull(a.jamTersibuk)
    }

    @Test
    fun `rata-rata per order dihitung dari omzet bersih`() {
        val a = susunAnalitikLaporan(
            listOf(pesanan(total = 30_000), pesanan(total = 45_000)),
        )
        assertEquals(37_500L, a.rataRataPerOrder)
    }

    @Test
    fun `rincian pembayaran dikelompokkan dan diurutkan dari omzet terbesar`() {
        val a = susunAnalitikLaporan(
            listOf(
                pesanan(metode = "cash", total = 20_000),
                pesanan(metode = "qris", total = 80_000),
                pesanan(metode = "cash", total = 30_000),
            ),
        )
        assertEquals(listOf("qris", "cash"), a.rincianPembayaran.map { it.metode })
        val tunai = a.rincianPembayaran.single { it.metode == "cash" }
        assertEquals(2, tunai.jumlah)
        assertEquals(50_000L, tunai.omzet)
        assertEquals("Tunai", tunai.label)
    }

    @Test
    fun `metode bayar kosong dikelompokkan sebagai lainnya`() {
        val a = susunAnalitikLaporan(listOf(pesanan(metode = null), pesanan(metode = "")))
        assertEquals(1, a.rincianPembayaran.size)
        assertEquals("Lainnya", a.rincianPembayaran.single().label)
        assertEquals(2, a.rincianPembayaran.single().jumlah)
    }

    @Test
    fun `sebaran per jam memakai jam Jakarta`() {
        val a = susunAnalitikLaporan(
            listOf(pesanan(jam = 8), pesanan(jam = 19), pesanan(jam = 19)),
        )
        assertEquals(1, a.perJam[8])
        assertEquals(2, a.perJam[19])
        assertEquals(19, a.jamTersibuk)
        assertEquals(24, a.perJam.size)
    }

    /** Dua jam berjumlah sama tidak boleh berganti-ganti tampil antar pemuatan. */
    @Test
    fun `jam tersibuk memilih yang paling awal saat seri`() {
        val a = susunAnalitikLaporan(listOf(pesanan(jam = 9), pesanan(jam = 17)))
        assertEquals(9, a.jamTersibuk)
    }

    @Test
    fun `item terjual dikelompokkan per menu induk dan diurutkan dari qty terbanyak`() {
        val a = susunAnalitikLaporan(
            listOf(
                pesanan(items = listOf(ItemPesanan("Shawarma", 2, 40_000), ItemPesanan("Teh", 5, 25_000))),
                pesanan(items = listOf(ItemPesanan("Shawarma", 1, 20_000))),
            ),
        )
        assertEquals(listOf("Teh", "Shawarma"), a.daftarItem.map { it.nama })
        val shawarma = a.daftarItem.single { it.nama == "Shawarma" }
        assertEquals(3, shawarma.qty)
        assertEquals(60_000L, shawarma.omzet)
        assertEquals(8, a.itemTerjual)
        assertEquals(5, a.qtyTertinggi)
    }

    @Test
    fun `daftar item kosong tidak membuat pembagi nol pada bilah`() {
        assertEquals(1, AnalitikLaporan.KOSONG.qtyTertinggi)
    }
}
