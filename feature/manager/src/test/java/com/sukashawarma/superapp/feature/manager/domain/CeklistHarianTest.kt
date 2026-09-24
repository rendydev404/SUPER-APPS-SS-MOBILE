package com.sukashawarma.superapp.feature.manager.domain

import com.sukashawarma.superapp.domain.model.Role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CeklistHarianTest {

    private fun butir(kunci: String) = SEMUA_BUTIR.first { it.kunci == kunci }

    private fun semuaBaik(): Map<String, IsianCeklist> =
        SEMUA_BUTIR.associate { it.kunci to pilihNilai(it, IsianCeklist(), NilaiCeklist.BAIK) }

    private fun fotoLengkap(): Map<String, List<FotoCeklist>> =
        KATEGORI_CEKLIST.associate { it.kunci to listOf(FotoCeklist("u/${it.kunci}.jpg")) }

    @Test
    fun `kontrak sembilan butir sama dengan CHECK database`() {
        // submit_ceklist_harian menuntut tepat sembilan pasangan ini.
        assertEquals(
            listOf(
                "kebersihan", "stok", "seragam_crew",
                "rasa.sapi", "rasa.ayam", "rasa.kentang", "rasa.tum", "rasa.sayur",
                "peralatan",
            ),
            SEMUA_BUTIR.map { it.kunci },
        )
        assertEquals(5, KATEGORI_CEKLIST.size)
    }

    @Test
    fun `kunci bagian bebas sama dengan kategori foto di database`() {
        // ceklist_harian_foto_kategori_check di migrasi 20300240000000.
        assertEquals(listOf("online_review", "temuan", "perbaikan"), BagianBebas.entries.map { it.kunci })
    }

    @Test
    fun `memilih nilai mengisi kalimat baku`() {
        val stok = butir("stok")
        val hasil = pilihNilai(stok, IsianCeklist(), NilaiCeklist.PERHATIAN)
        assertEquals(NilaiCeklist.PERHATIAN, hasil.nilai)
        assertEquals("Kurang aman", hasil.keterangan)
    }

    @Test
    fun `berganti nilai mengganti kalimat baku tapi tidak menimpa ketikan sendiri`() {
        val stok = butir("stok")
        val baku = pilihNilai(stok, IsianCeklist(), NilaiCeklist.BAIK)
        assertEquals("Kurang aman", pilihNilai(stok, baku, NilaiCeklist.PERHATIAN).keterangan)

        val sendiri = IsianCeklist(NilaiCeklist.BAIK, "Daging tinggal 2 pack")
        assertEquals("Daging tinggal 2 pack", pilihNilai(stok, sendiri, NilaiCeklist.BURUK).keterangan)
    }

    @Test
    fun `halangan menyebut sub-item rasa yang belum dinilai`() {
        val isian = semuaBaik() - "rasa.tum" - "rasa.sayur"
        assertEquals("Rasa: Tum, Sayur belum dinilai.", halanganCeklist(isian, fotoLengkap()))
    }

    @Test
    fun `foto wajib per kategori`() {
        val foto = fotoLengkap() - "peralatan"
        assertEquals("Foto Peralatan belum ada.", halanganCeklist(semuaBaik(), foto))
        assertEquals(4, jumlahKategoriLengkap(semuaBaik(), foto))
        assertNull(halanganCeklist(semuaBaik(), fotoLengkap()))
    }

    @Test
    fun `nilai kategori rasa adalah butir terburuk`() {
        val rasa = KATEGORI_CEKLIST.first { it.kunci == "rasa" }
        val isian = semuaBaik() + ("rasa.ayam" to IsianCeklist(NilaiCeklist.BURUK, ""))
        assertEquals(NilaiCeklist.BURUK, nilaiKategori(rasa, isian))
        assertNull(nilaiKategori(rasa, isian - "rasa.sapi"))
    }

    @Test
    fun `ada masalah hanya bila ada nilai selain baik`() {
        assertFalse(adaMasalah(semuaBaik()))
        assertTrue(adaMasalah(semuaBaik() + ("stok" to IsianCeklist(NilaiCeklist.PERHATIAN, ""))))
    }

    @Test
    fun `hanya area manager yang mengisi, hanya RM admin owner yang meninjau`() {
        assertTrue(mengisiCeklist(Role.AREA_MANAGER))
        assertFalse(mengisiCeklist(Role.REGIONAL_MANAGER))
        assertTrue(meninjauCeklist(Role.REGIONAL_MANAGER))
        assertFalse(meninjauCeklist(Role.AREA_MANAGER))
    }

    @Test
    fun `jam jakarta dari cap waktu UTC`() {
        assertEquals("09.15", jamJakarta("2026-09-24T02:15:00+00:00"))
        assertEquals("", jamJakarta("bukan waktu"))
    }
}
