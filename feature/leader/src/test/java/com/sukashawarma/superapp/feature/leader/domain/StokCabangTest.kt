package com.sukashawarma.superapp.feature.leader.domain

import com.sukashawarma.superapp.feature.stok.data.model.MonitoringRow
import com.sukashawarma.superapp.feature.stok.domain.StokStatus
import com.sukashawarma.superapp.feature.stok.domain.UnitMeta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StokCabangTest {

    private fun bahan(nama: String, status: StatusStok, saldo: Double = 5.0, batas: Double? = 10.0) =
        BahanCabang(
            id = nama,
            nama = nama,
            saldo = saldo,
            satuan = "kg",
            batasMinimal = batas,
            status = status,
        )

    private val tepung = UnitMeta(satuan = "Kg", satuanKecil = "Gram", faktorTampilan = 1000.0)

    @Test
    fun `status skala dipetakan ke tiga tingkat yang dipakai layar`() {
        assertEquals(StatusStok.KRITIS, StatusStok.dariSkala(StokStatus.BELOW))
        assertEquals(StatusStok.MENIPIS, StatusStok.dariSkala(StokStatus.WARNING))
        assertEquals(StatusStok.AMAN, StatusStok.dariSkala(StokStatus.OK))
    }

    /** Faktor satuan kosong tidak boleh membanjiri lencana merah. */
    @Test
    fun `status tak dapat dihitung dibaca aman`() {
        assertEquals(StatusStok.AMAN, StatusStok.dariSkala(StokStatus.UNKNOWN))
    }

    private fun baris(qty: Double, isGram: Boolean, threshold: Double?, meta: UnitMeta, nama: String = "TEPUNG") =
        MonitoringRow(
            outletId = "o", outletName = "Outlet", bahanBakuId = "b", itemName = nama,
            currentQty = qty, threshold = threshold, statusView = null, isFlagged = false,
            saldoIsGram = isGram, lastOpnameDate = null, kategori = null, satuan = meta.satuan, meta = meta,
        )

    /**
     * Angka dan status HARUS identik dengan kartu di layar Monitoring modul Stok:
     * dihitung dari `MonitoringRow` yang sama, dipecah `decomposeTriUnit` yang sama.
     */
    @Test
    fun `saldo gram dinormalkan sebelum ditampilkan dan dibandingkan`() {
        val b = bahanCabang(baris(2032.0, true, 5.0, tepung))
        assertEquals("2 Kg 32 Gr", b.saldoTeks)
        assertEquals("5 Kg", b.batasTeks)
        assertEquals(StatusStok.KRITIS, b.status)
    }

    @Test
    fun `saldo satuan besar legacy tidak dikali dua kali`() {
        val b = bahanCabang(baris(4.0, false, 5.0, tepung))
        assertEquals("4 Kg", b.saldoTeks)
        assertEquals(StatusStok.MENIPIS, b.status)
    }

    @Test
    fun `faktor kosong menampilkan angka mentah seperti layar stok`() {
        val tanpaFaktor = tepung.copy(faktorTampilan = null)
        assertEquals("2032 Kg", bahanCabang(baris(2032.0, true, 5.0, tanpaFaktor)).saldoTeks)
        assertEquals(StatusStok.AMAN, bahanCabang(baris(2032.0, true, 5.0, tanpaFaktor)).status)
    }

    /** BAWANG: 1 Bal = 20 Kg = 20.000 Gram; 1.500 gram = 1 Kg 500 Gram, bukan "75 Kg". */
    @Test
    fun `saldo bersatuan tengah sama dengan layar stok`() {
        val bawang = UnitMeta(
            satuan = "Bal", satuanTengah = "Kg", satuanKecil = "Gram",
            faktorTengah = 20.0, faktorTampilan = 20000.0,
        )
        val b = bahanCabang(baris(1500.0, true, 1.0, bawang, nama = "BAWANG"))
        assertEquals("1 Kg 500 Gr", b.saldoTeks)
        assertEquals(StatusStok.KRITIS, b.status)
    }

    @Test
    fun `saldo nol tetap menulis satuan besarnya`() {
        assertEquals("0 Kg", bahanCabang(baris(0.0, true, 5.0, tepung)).saldoTeks)
    }

    /** Ambang porsi milik outlet ikut dipakai, sama seperti `MonitoringUiState.status`. */
    @Test
    fun `ambang marquee outlet diteruskan ke penentu status`() {
        val b = bahanCabang(baris(9000.0, true, 5.0, tepung), marqueeWarning = 3)
        assertEquals(StatusStok.AMAN, b.status)
    }

    /**
     * Kartu "Stok Cabang" di Ringkasan harus berbunyi sama dengan lencana layar Stok
     * Cabang — dulu ia mencacah bahan bersaldo > 0, angka yang tidak ada di layar mana pun.
     */
    @Test
    fun `ringkasan stok mencacah kritis dan menipis seperti layar stok`() {
        val r = ringkasStok(
            listOf(
                bahan("A", StatusStok.KRITIS),
                bahan("B", StatusStok.KRITIS),
                bahan("C", StatusStok.MENIPIS),
                bahan("D", StatusStok.AMAN),
            )
        )
        assertEquals(RingkasanStok(kritis = 2, menipis = 1, total = 4), r)
    }

    @Test
    fun `yang paling genting muncul lebih dulu`() {
        val hasil = urutkanStok(
            listOf(
                bahan("Aman A", StatusStok.AMAN),
                bahan("Kritis Z", StatusStok.KRITIS),
                bahan("Menipis M", StatusStok.MENIPIS),
            )
        )
        assertEquals(listOf("Kritis Z", "Menipis M", "Aman A"), hasil.map { it.nama })
    }

    @Test
    fun `dalam tingkat yang sama diurutkan abjad tanpa peduli huruf besar`() {
        val hasil = urutkanStok(
            listOf(
                bahan("zaitun", StatusStok.KRITIS),
                bahan("Ayam", StatusStok.KRITIS),
            )
        )
        assertEquals(listOf("Ayam", "zaitun"), hasil.map { it.nama })
    }

    @Test
    fun `pencarian mengabaikan besar kecil huruf dan spasi di tepi`() {
        val daftar = listOf(bahan("Daging Sapi", StatusStok.AMAN), bahan("Roti", StatusStok.AMAN))
        assertEquals(listOf("Daging Sapi"), saringStok(daftar, "  sapi ").map { it.nama })
    }

    @Test
    fun `kunci kosong mengembalikan seluruh daftar`() {
        val daftar = listOf(bahan("Daging", StatusStok.AMAN), bahan("Roti", StatusStok.AMAN))
        assertEquals(2, saringStok(daftar, "").size)
        assertEquals(2, saringStok(daftar, "   ").size)
    }

    @Test
    fun `saldo dan batas ditulis dengan satuannya`() {
        val b = bahan("Daging", StatusStok.AMAN, saldo = 12.5, batas = 5.0)
        assertEquals("12.5 kg", b.saldoTeks)
        assertEquals("5 kg", b.batasTeks)
    }

    @Test
    fun `bahan tanpa titik pesan ulang tidak mengarang batas minimal`() {
        assertNull(bahan("Daging", StatusStok.AMAN, batas = null).batasTeks)
    }
}
