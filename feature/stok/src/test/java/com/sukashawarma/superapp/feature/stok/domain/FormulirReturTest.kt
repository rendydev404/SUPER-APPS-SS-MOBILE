package com.sukashawarma.superapp.feature.stok.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Aturan formulir pengajuan retur.
 *
 * [FormulirRetur.totalBesar] adalah bagian yang paling berbahaya di seluruh modul:
 * angkanya dikirim mentah ke `ajukan_retur_stok`, yang meneruskannya ke
 * `to_ledger_scale` lalu MEMOTONG SALDO OUTLET. Salah skala di sini bukan salah
 * tampilan, melainkan stok yang hilang ribuan kali lipat.
 */
class FormulirReturTest {

    @Test
    fun `dua kolom masukan digabung pada satuan besar`() {
        // 2 kg 500 gram, faktor 1000 -> 2,5 kg.
        assertEquals(2.5, FormulirRetur.totalBesar(2.0, 500.0, 1000.0), 1e-9)
        assertEquals(0.75, FormulirRetur.totalBesar(null, 750.0, 1000.0), 1e-9)
        assertEquals(3.0, FormulirRetur.totalBesar(3.0, null, 1000.0), 1e-9)
    }

    @Test
    fun `faktor bawaan seribu dipakai saat master bahan tidak menyimpannya`() {
        assertEquals(1.25, FormulirRetur.totalBesar(1.0, 250.0, null), 1e-9)
        // Faktor nol/negatif adalah data rusak, bukan alasan membagi dengan nol.
        assertEquals(1.25, FormulirRetur.totalBesar(1.0, 250.0, 0.0), 1e-9)
    }

    @Test
    fun `faktor selain seribu tetap dihormati`() {
        // Kulit: 1 pack = 20 lembar. 2 pack 10 lembar -> 2,5 pack.
        assertEquals(2.5, FormulirRetur.totalBesar(2.0, 10.0, 20.0), 1e-9)
    }

    @Test
    fun `hanya bahan core yang boleh diretur`() {
        assertTrue(FormulirRetur.refundable(true, "APA SAJA"))
        // Jaring kedua by-name, untuk baris yang kolomnya belum di-backfill.
        assertTrue(FormulirRetur.refundable(false, "ayam"))
        assertTrue(FormulirRetur.refundable(null, " Kulit 28 "))
        assertFalse(FormulirRetur.refundable(false, "SAOS SAMBAL"))
        assertFalse(FormulirRetur.refundable(null, "KULIT 30"))
    }

    @Test
    fun `alasan lainnya digabung dengan keterangannya`() {
        assertEquals("Basi / Bau Asam", FormulirRetur.alasanAkhir("Basi / Bau Asam", "diabaikan"))
        assertEquals("Lainnya: tekstur lembek", FormulirRetur.alasanAkhir("Lainnya", " tekstur lembek "))
        // Keterangan kosong tidak boleh menghasilkan "Lainnya: " menggantung.
        assertEquals("Lainnya", FormulirRetur.alasanAkhir("Lainnya", "   "))
    }

    @Test
    fun `halangan muncul berurutan seperti web`() {
        assertEquals(
            "Pilih bahan baku yang akan diretur.",
            FormulirRetur.halangan(false, 0.0, false, "Lainnya", ""),
        )
        assertEquals(
            "Kuantitas timbangan harus lebih besar dari 0.",
            FormulirRetur.halangan(true, 0.0, false, "Lainnya", ""),
        )
        assertEquals(
            "Foto bahan baku di atas timbangan wajib diunggah.",
            FormulirRetur.halangan(true, 1.0, false, "Lainnya", ""),
        )
        assertEquals(
            "Keterangan alasan lainnya wajib diisi.",
            FormulirRetur.halangan(true, 1.0, true, "Lainnya", "  "),
        )
    }

    @Test
    fun `formulir lengkap tidak punya halangan`() {
        assertNull(FormulirRetur.halangan(true, 2.5, true, "Basi / Bau Asam", ""))
        assertNull(FormulirRetur.halangan(true, 2.5, true, "Lainnya", "vacum sobek"))
    }

    /** Foto WAJIB: itu satu-satunya bukti yang dipegang AM/RM saat memutuskan. */
    @Test
    fun `tanpa foto klaim tidak bisa dikirim`() {
        assertNotNull(FormulirRetur.halangan(true, 5.0, false, "Basi / Bau Asam", ""))
    }
}
