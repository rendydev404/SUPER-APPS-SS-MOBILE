package com.sukashawarma.superapp.feature.stok.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Port `apps/stok/src/lib/stok/zeroGuard.test.ts`, kasus per kasus.
 *
 * Angkanya dari kejadian nyata 5-7 September 2026: tiga outlet mengetik "0 Roll"
 * untuk FOIL saat opname, menghapus stok yang sebenarnya masih ada — Pajajaran
 * kehilangan 75 Roll dalam satu kali finalisasi.
 */
class GerbangNolOpnameTest {

    // --------------------------------------------------- nol yang mencurigakan

    @Test
    fun `FOIL Pajajaran fisik nol sementara sistem masih 57005 cm`() {
        assertTrue(GerbangNolOpname.nolMencurigakan(0.0, 57005.0, 760.0))
    }

    @Test
    fun `sisa di bawah satu satuan menengah tidak mengganggu`() {
        assertFalse(GerbangNolOpname.nolMencurigakan(0.0, 500.0, 760.0))
    }

    @Test
    fun `tepat satu satuan menengah sudah dihitung berarti`() {
        assertTrue(GerbangNolOpname.nolMencurigakan(0.0, 760.0, 760.0))
    }

    @Test
    fun `sistem minus tidak dikonfirmasi karena mengisi nol justru memperbaiki`() {
        assertFalse(GerbangNolOpname.nolMencurigakan(0.0, -2660.0, 760.0))
    }

    @Test
    fun `sistem sudah nol berarti tidak ada yang terhapus`() {
        assertFalse(GerbangNolOpname.nolMencurigakan(0.0, 0.0, 760.0))
    }

    @Test
    fun `fisik yang terisi tidak pernah ditandai`() {
        assertFalse(GerbangNolOpname.nolMencurigakan(12.0, 57005.0, 760.0))
    }

    @Test
    fun `POLYBAG faktor sembilan menandai sembilan tapi tidak delapan`() {
        assertTrue(GerbangNolOpname.nolMencurigakan(0.0, 9.0, 9.0))
        assertFalse(GerbangNolOpname.nolMencurigakan(0.0, 8.0, 9.0))
    }

    @Test
    fun `tanpa faktor konversi ambangnya satu satuan`() {
        assertTrue(GerbangNolOpname.nolMencurigakan(0.0, 1.0, null))
        assertFalse(GerbangNolOpname.nolMencurigakan(0.0, 0.5, null))
        // Faktor nol tidak dipercaya sebagai faktor, jadi jatuh ke ambang 1.
        assertTrue(GerbangNolOpname.nolMencurigakan(0.0, 3.0, 0.0))
    }

    // ------------------------------------------- gerbang sebelum finalisasi

    private fun calon(
        nama: String,
        fisik: Double,
        sistem: Double,
        faktor: Double?,
        ditandai: Boolean = true,
    ) = CalonPenurunan(
        bahanBakuId = nama.lowercase(),
        nama = nama,
        qtyFisik = fisik,
        qtySystem = sistem,
        faktorKonversi = faktor,
        ditandai = ditandai,
    )

    @Test
    fun `hanya penurunan yang ditandai yang masuk gerbang`() {
        val hasil = GerbangNolOpname.daftarPenurunan(
            listOf(
                calon("FOIL", 0.0, 57005.0, 760.0),
                // Naik, bukan turun: janggal juga tapi tidak menghapus apa pun.
                calon("AYAM", 9000.0, 4444.0, 1000.0),
                // Turun tapi masih dalam toleransi, jadi tidak ditandai.
                calon("SAPI", 4000.0, 4100.0, 1000.0, ditandai = false),
                calon("TUTUP PACK", 0.0, 2280.0, 1000.0),
            ),
        )
        assertEquals(listOf("FOIL", "TUTUP PACK"), hasil.map { it.calon.nama })
    }

    @Test
    fun `yang habis total ditaruh paling atas`() {
        val hasil = GerbangNolOpname.daftarPenurunan(
            listOf(
                calon("SAPI", 1000.0, 9000.0, 1000.0),
                calon("FOIL", 0.0, 57005.0, 760.0),
            ),
        )
        assertEquals(listOf("FOIL", "SAPI"), hasil.map { it.calon.nama })
        assertTrue(hasil[0].habisTotal)
        assertFalse(hasil[1].habisTotal)
    }

    @Test
    fun `hanya nol mencurigakan yang boleh dilewati`() {
        val hasil = GerbangNolOpname.daftarPenurunan(
            listOf(
                calon("FOIL", 0.0, 57005.0, 760.0),
                // Turun drastis tapi bukan nol: tidak ada jalan keluar "lewati",
                // karena angkanya memang hasil hitungan kru.
                calon("SAPI", 1000.0, 9000.0, 1000.0),
                // Nol, tapi sisanya di bawah satu satuan menengah.
                calon("GAS", 0.0, 330.0, 3000.0),
            ),
        )
        assertEquals(listOf(true, false, false), hasil.map { it.bolehLewati })
    }

    @Test
    fun `tanpa penurunan apa pun gerbangnya kosong`() {
        val hasil = GerbangNolOpname.daftarPenurunan(
            listOf(calon("AYAM", 9000.0, 4444.0, 1000.0)),
        )
        assertTrue(hasil.isEmpty())
    }
}
