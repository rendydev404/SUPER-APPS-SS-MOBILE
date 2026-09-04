package com.sukashawarma.superapp.feature.distribusi.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class SatuanDistribusiTest {

    private fun meta(
        satuan: String,
        satuanDistribusi: String? = null,
        satuanTengah: String? = null,
        satuanKecil: String? = null,
        faktorTengah: Double? = null,
        faktorTampilan: Double? = null,
    ) = BahanBakuMeta(
        id = "b1",
        nama = "Uji",
        satuan = satuan,
        satuanDistribusi = satuanDistribusi,
        satuanTengah = satuanTengah,
        satuanKecil = satuanKecil,
        faktorTengah = faktorTengah,
        faktorTampilan = faktorTampilan,
        kategori = null,
    )

    @Test
    fun `tanpa satuan distribusi faktornya satu`() {
        assertEquals(1.0, SatuanDistribusi.faktor(meta("Dus")), 0.0001)
    }

    @Test
    fun `satuan distribusi sama dengan satuan dasar faktornya satu`() {
        assertEquals(1.0, SatuanDistribusi.faktor(meta("Dus", satuanDistribusi = "dus")), 0.0001)
    }

    @Test
    fun `satuan distribusi sama dengan satuan tengah memakai faktor tengah`() {
        val b = meta("Dus", satuanDistribusi = "Pack", satuanTengah = "Pack", faktorTengah = 24.0)
        assertEquals(24.0, SatuanDistribusi.faktor(b), 0.0001)
    }

    @Test
    fun `satuan distribusi sama dengan satuan kecil memakai faktor tampilan`() {
        val b = meta("Dus", satuanDistribusi = "Lembar", satuanKecil = "Lembar", faktorTampilan = 240.0)
        assertEquals(240.0, SatuanDistribusi.faktor(b), 0.0001)
    }

    /** Pemetaan implisit di web: satuan distribusi "kg" dengan satuan kecil "gram".
     *  1 karung = 25000 gram = 25 kg, jadi faktornya 25000/1000. */
    @Test
    fun `kg dengan satuan kecil gram membagi faktor tampilan seribu`() {
        val b = meta("Karung", satuanDistribusi = "kg", satuanKecil = "Gram", faktorTampilan = 25000.0)
        assertEquals(25.0, SatuanDistribusi.faktor(b), 0.0001)
    }

    @Test
    fun `satuan distribusi tak dikenali jatuh ke faktor satu`() {
        val b = meta("Dus", satuanDistribusi = "Palet", satuanTengah = "Pack", faktorTengah = 24.0)
        assertEquals(1.0, SatuanDistribusi.faktor(b), 0.0001)
    }

    /** Faktor null di database tidak boleh membuat konversi menebak. */
    @Test
    fun `faktor null jatuh ke satu`() {
        val b = meta("Dus", satuanDistribusi = "Pack", satuanTengah = "Pack", faktorTengah = null)
        assertEquals(1.0, SatuanDistribusi.faktor(b), 0.0001)
    }

    @Test
    fun `perbandingan satuan mengabaikan besar kecil huruf`() {
        val b = meta("Dus", satuanDistribusi = "PACK", satuanTengah = "pack", faktorTengah = 24.0)
        assertEquals(24.0, SatuanDistribusi.faktor(b), 0.0001)
    }

    @Test
    fun `ke tampilan mengalikan faktor lalu membulatkan`() {
        val b = meta("Dus", satuanDistribusi = "Pack", satuanTengah = "Pack", faktorTengah = 24.0)
        // 0,5 Dus = 12 Pack
        assertEquals(12L, SatuanDistribusi.keTampilan(0.5, b))
        // 0,2083 Dus = 4,999 Pack -> dibulatkan jadi 5, sama dengan Math.round di web
        assertEquals(5L, SatuanDistribusi.keTampilan(5.0 / 24.0, b))
    }

    @Test
    fun `ke dasar membagi faktor tanpa pembulatan`() {
        val b = meta("Dus", satuanDistribusi = "Pack", satuanTengah = "Pack", faktorTengah = 24.0)
        assertEquals(0.5, SatuanDistribusi.keDasar(12.0, b), 0.0001)
        assertEquals(5.0 / 24.0, SatuanDistribusi.keDasar(5.0, b), 0.000001)
    }

    @Test
    fun `bolak balik pada faktor bulat kembali ke nilai semula`() {
        val b = meta("Dus", satuanDistribusi = "Pack", satuanTengah = "Pack", faktorTengah = 24.0)
        val asal = 3.0
        val bolakBalik = SatuanDistribusi.keDasar(SatuanDistribusi.keTampilan(asal, b).toDouble(), b)
        assertEquals(asal, bolakBalik, 0.0001)
    }

    @Test
    fun `satuan tampil memakai satuan distribusi bila ada`() {
        assertEquals("Pack", SatuanDistribusi.satuanTampil(meta("Dus", satuanDistribusi = "Pack")))
        assertEquals("Dus", SatuanDistribusi.satuanTampil(meta("Dus")))
    }
}
