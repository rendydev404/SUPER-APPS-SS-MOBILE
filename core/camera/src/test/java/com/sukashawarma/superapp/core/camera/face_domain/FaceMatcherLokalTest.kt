package com.sukashawarma.superapp.domain.face

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pencocokan ini menggantikan server saat internet mati. Kalau perilakunya menyimpang dari
 * RPC `match_face_mobile`, orang yang dikenali saat offline bisa ditolak saat online tanpa
 * ada yang bisa menjelaskan kenapa.
 */
class FaceMatcherLokalTest {

    private fun vektor(vararg nilai: Float) = nilai

    private val ana = KandidatWajah("ana", "Ana", vektor(1f, 0f, 0f))
    private val budi = KandidatWajah("budi", "Budi", vektor(0f, 1f, 0f))

    @Test
    fun `wajah identik dikenali`() {
        val hasil = FaceMatcherLokal.cocokkan(vektor(1f, 0f, 0f), listOf(ana, budi))
        assertEquals("ana", hasil?.staffId)
        assertEquals(1.0, hasil!!.similarity, 1e-9)
    }

    @Test
    fun `wajah asing ditolak, bukan dipaksakan ke kandidat terbaik`() {
        // Tegak lurus terhadap keduanya: similarity 0, jauh di bawah ambang.
        assertNull(FaceMatcherLokal.cocokkan(vektor(0f, 0f, 1f), listOf(ana, budi)))
    }

    /**
     * Probe dengan cosine similarity [sim] terhadap vektor satuan (1, 0). Membuat
     * similarity PERSIS sama dengan ambang lewat Float tidak bisa diandalkan, jadi yang
     * diuji adalah sisi kiri dan kanan batasnya, bukan titik batasnya sendiri.
     */
    private fun probeDenganSimilarity(sim: Double) =
        vektor(sim.toFloat(), kotlin.math.sqrt(1 - sim * sim).toFloat())

    @Test
    fun `sedikit di atas ambang diterima`() {
        val kandidat = listOf(KandidatWajah("x", "X", vektor(1f, 0f)))
        val hasil = FaceMatcherLokal.cocokkan(probeDenganSimilarity(FaceMatcherLokal.AMBANG + 0.01), kandidat)
        assertTrue("harusnya dikenali, dapat $hasil", hasil != null)
    }

    @Test
    fun `sedikit di bawah ambang ditolak`() {
        val kandidat = listOf(KandidatWajah("x", "X", vektor(1f, 0f)))
        assertNull(FaceMatcherLokal.cocokkan(probeDenganSimilarity(FaceMatcherLokal.AMBANG - 0.01), kandidat))
    }

    @Test
    fun `mode satu-lawan-satu hanya membandingkan staf yang dikunci`() {
        // Perangkat personal: wajah orang lain tidak boleh membuka absen pemilik HP,
        // dan sebaliknya wajah pemilik HP tidak boleh mengabsenkan orang lain.
        val hasil = FaceMatcherLokal.cocokkan(vektor(1f, 0f, 0f), listOf(ana, budi), lockToStaffId = "budi")
        assertNull(hasil)
    }

    @Test
    fun `descriptor berbeda dimensi dilewati, bukan bikin gagal`() {
        // Staf yang enroll dengan model lama masih punya baris di server. Server melewatinya
        // (CONTINUE); kalau di sini justru melempar, satu baris usang membuat seluruh outlet
        // tidak bisa absen offline.
        val modelLama = KandidatWajah("lama", "Lama", vektor(1f, 0f))
        val hasil = FaceMatcherLokal.cocokkan(vektor(1f, 0f, 0f), listOf(modelLama, ana))
        assertEquals("ana", hasil?.staffId)
    }

    @Test
    fun `daftar kandidat kosong menghasilkan null`() {
        // Terjadi saat descriptor belum sempat disinkronkan. Harus jadi penolakan biasa,
        // bukan crash di depan orang yang sedang absen.
        assertNull(FaceMatcherLokal.cocokkan(vektor(1f, 0f, 0f), emptyList()))
    }

    @Test
    fun `descriptor nol tidak dibagi nol`() {
        val nol = KandidatWajah("nol", "Nol", vektor(0f, 0f, 0f))
        assertNull(FaceMatcherLokal.cocokkan(vektor(1f, 0f, 0f), listOf(nol)))
        assertNull(FaceMatcherLokal.cocokkan(vektor(0f, 0f, 0f), listOf(ana)))
    }
}
