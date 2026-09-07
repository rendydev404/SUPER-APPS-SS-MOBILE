package com.sukashawarma.superapp.feature.manager.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HppTest {

    private fun baris(
        qty: Double = 100.0,
        hargaBeli: Double = 47_800.0,
        qtyIsi: Double = 1000.0,
    ) = BarisResep("b1", "Ayam", qty, "gram", HargaBahan(hargaBeli, qtyIsi))

    private fun menu(
        nama: String = "Shawarma Ayam",
        harga: Long = 25_000,
        hppResep: Long? = 10_000,
        override: Long? = null,
        kelompok: KelompokMenu = KelompokMenu.AYAM,
        urutan: Int = 0,
    ) = MenuHpp(
        id = nama, nama = nama, kelompok = kelompok, kategoriAsli = null,
        hargaJual = harga, hppResep = hppResep, hppOverride = override,
        paket = false, parsial = false, tersedia = true, urutan = urutan,
    )

    /* ---------------- harga bahan ---------------- */

    /** Contoh dari komentar `lib/hpp.ts`: AYAM Rp47.800 per 1000 gram, dipakai 100 gram. */
    @Test
    fun `subtotal dihitung dari harga per kemasan dibagi isi kemasan`() {
        assertEquals(4_780L, baris().subtotal)
    }

    @Test
    fun `bahan tanpa harga bernilai nol dan ditandai`() {
        assertEquals(0L, baris(hargaBeli = 0.0).subtotal)
        assertFalse(baris(hargaBeli = 0.0).adaHarga)
        assertFalse(baris(qtyIsi = 0.0).adaHarga)
        assertTrue(baris().adaHarga)
    }

    @Test
    fun `qty bulat ditampilkan tanpa koma`() {
        assertEquals("100", baris(qty = 100.0).qtyTeks)
        assertEquals("2.5", baris(qty = 2.5).qtyTeks)
    }

    /* ---------------- resep ---------------- */

    @Test
    fun `total hpp adalah jumlah bahan ditambah buffer`() {
        val resep = ResepMenu("m1", true, 500, listOf(baris(), baris(qty = 50.0)))
        assertEquals(4_780L + 2_390L, resep.totalBahan)
        assertEquals(4_780L + 2_390L + 500L, resep.totalHpp)
    }

    @Test
    fun `buffer negatif tidak mengurangi hpp`() {
        val resep = ResepMenu("m1", true, -5_000, listOf(baris()))
        assertEquals(4_780L, resep.totalHpp)
    }

    @Test
    fun `resep dengan bahan tanpa harga ditandai`() {
        assertTrue(ResepMenu("m1", true, 0, listOf(baris(hargaBeli = 0.0))).adaBahanTanpaHarga)
        assertFalse(ResepMenu("m1", true, 0, listOf(baris())).adaBahanTanpaHarga)
    }

    /* ---------------- paket ---------------- */

    @Test
    fun `hpp paket menjumlahkan komponen dikali jumlahnya`() {
        val hasil = hitungHppPaket(listOf("a" to 2, "b" to 1), mapOf("a" to 5_000L, "b" to 3_000L))
        assertEquals(13_000L to false, hasil)
    }

    @Test
    fun `paket dengan komponen tak diketahui ditandai parsial`() {
        val hasil = hitungHppPaket(listOf("a" to 1, "b" to 1), mapOf("a" to 5_000L))
        assertEquals(5_000L to true, hasil)
    }

    @Test
    fun `paket tanpa komponen atau tanpa satu pun harga menghasilkan null`() {
        assertNull(hitungHppPaket(emptyList(), emptyMap()))
        assertNull(hitungHppPaket(listOf("a" to 1), emptyMap()))
    }

    /* ---------------- margin ---------------- */

    @Test
    fun `override menang atas hitungan resep`() {
        val m = menu(harga = 20_000, hppResep = 8_000, override = 9_000)
        assertEquals(9_000L, m.hpp)
        assertEquals(11_000L, m.marginRp)
        assertEquals(45.0, m.foodcostPersen!!, 0.001)
    }

    @Test
    fun `menu tanpa resep dan tanpa override tidak punya margin`() {
        val m = menu(hppResep = null)
        assertNull(m.hpp)
        assertNull(m.marginRp)
        assertNull(m.foodcostPersen)
        assertNull(m.marginPersen)
    }

    @Test
    fun `harga jual nol tidak membagi dengan nol`() {
        val m = menu(harga = 0, hppResep = 5_000)
        assertNull(m.foodcostPersen)
        assertNull(m.marginPersen)
        assertEquals(-5_000L, m.marginRp)
    }

    @Test
    fun `foodcost di atas empat puluh persen ditandai perlu ditengok`() {
        assertTrue(menu(harga = 10_000, hppResep = 4_500).foodcostTinggi)
        assertFalse(menu(harga = 10_000, hppResep = 4_000).foodcostTinggi)
    }

    /* ---------------- kelompok menu ---------------- */

    @Test
    fun `menu eksklusif tiktok masuk kelompok tiktok meski kategorinya sapi`() {
        assertEquals(
            KelompokMenu.TIKTOK,
            kelompokMenu("Shawarma Sapi Besar", "Original Sapi", false, listOf("tiktokgo")),
        )
    }

    /** Urutan pemeriksaan penting: TikTok diperiksa sebelum kategori nama dilihat. */
    @Test
    fun `menu sapi yang juga dijual di gofood tetap kelompok sapi`() {
        assertEquals(
            KelompokMenu.SAPI,
            kelompokMenu("Shawarma Sapi Besar", "Original Sapi", false, listOf("tiktokgo", "gofood")),
        )
    }

    @Test
    fun `kategori mengandung mix menang atas sapi dan ayam`() {
        assertEquals(KelompokMenu.MIX, kelompokMenu("Shawarma Mix", "Mix Sapi Ayam", false, emptyList()))
    }

    @Test
    fun `suka suka dikenali dari kategori maupun awalan nama`() {
        assertEquals(KelompokMenu.SUKA_SUKA, kelompokMenu("X", "Suka Suka Series", false, emptyList()))
        assertEquals(KelompokMenu.SUKA_SUKA, kelompokMenu("Suka Pedas", null, false, emptyList()))
        // "Suka Merdeka" dikecualikan, dan paket tidak pernah masuk Suka Suka.
        assertEquals(KelompokMenu.LAINNYA, kelompokMenu("Suka Merdeka", null, false, emptyList()))
        assertEquals(KelompokMenu.COMBO, kelompokMenu("Suka Hemat", null, true, emptyList()))
    }

    @Test
    fun `paket tanpa penanda lain masuk combo reguler`() {
        assertEquals(KelompokMenu.COMBO, kelompokMenu("Paket Hemat", null, true, emptyList()))
    }

    @Test
    fun `topping dan minuman dikenali dari kategori atau nama`() {
        assertEquals(KelompokMenu.TOPPING, kelompokMenu("Extra Keju", null, false, emptyList()))
        assertEquals(KelompokMenu.MINUMAN, kelompokMenu("Lemon Tea", null, false, emptyList()))
        assertEquals(KelompokMenu.MINUMAN, kelompokMenu("X", "Suka Drink", false, emptyList()))
    }

    @Test
    fun `menu tak dikenali jatuh ke kelompok lainnya`() {
        assertEquals(KelompokMenu.LAINNYA, kelompokMenu("Kerupuk", "Snack", false, emptyList()))
    }

    /* ---------------- ringkasan & urutan ---------------- */

    @Test
    fun `ringkasan menghitung rata-rata hanya dari menu yang punya foodcost`() {
        val r = ringkasHpp(
            listOf(
                menu(nama = "A", harga = 10_000, hppResep = 3_000),
                menu(nama = "B", harga = 10_000, hppResep = 5_000),
                menu(nama = "C", hppResep = null),
            ),
        )
        assertEquals(3, r.jumlahMenu)
        assertEquals(2, r.jumlahBerResep)
        assertEquals(40.0, r.rataRataFoodcost!!, 0.001)
        assertEquals("B", r.foodcostTertinggi?.nama)
        assertEquals("A", r.foodcostTerendah?.nama)
    }

    @Test
    fun `daftar perlu ditengok hanya berisi foodcost di atas ambang dan urut menurun`() {
        val r = ringkasHpp(
            listOf(
                menu(nama = "A", harga = 10_000, hppResep = 4_500),
                menu(nama = "B", harga = 10_000, hppResep = 6_000),
                menu(nama = "C", harga = 10_000, hppResep = 3_000),
            ),
        )
        assertEquals(listOf("B", "A"), r.perluDitengok.map { it.nama })
    }

    @Test
    fun `ringkasan daftar kosong tidak menghitung rata-rata`() {
        assertNull(ringkasHpp(emptyList()).rataRataFoodcost)
        assertNull(RingkasanHpp.KOSONG.foodcostTertinggi)
    }

    @Test
    fun `peringkat ukuran mengikuti urutan porsi`() {
        assertEquals(1, peringkatUkuran("Shawarma Sedang"))
        assertEquals(2, peringkatUkuran("Shawarma Besar"))
        assertEquals(3, peringkatUkuran("Shawarma Jumbo"))
        assertEquals(4, peringkatUkuran("Shawarma Reguler"))
        assertEquals(10, peringkatUkuran("Shawarma"))
    }

    @Test
    fun `urutan menu mengikuti kelompok lalu ukuran porsi`() {
        val daftar = listOf(
            menu(nama = "Ayam Besar", kelompok = KelompokMenu.AYAM),
            menu(nama = "Sapi Jumbo", kelompok = KelompokMenu.SAPI),
            menu(nama = "Sapi Sedang", kelompok = KelompokMenu.SAPI),
        ).sortedWith(URUTAN_MENU_HPP)
        assertEquals(listOf("Sapi Sedang", "Sapi Jumbo", "Ayam Besar"), daftar.map { it.nama })
    }
}
