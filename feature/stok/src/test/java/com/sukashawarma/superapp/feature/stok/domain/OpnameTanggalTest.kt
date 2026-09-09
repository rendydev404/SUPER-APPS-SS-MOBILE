package com.sukashawarma.superapp.feature.stok.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Mengunci tabel pengecualian tanggal opname terhadap `opnameDate.ts` dan blok
 * pengecualian `createOrReuseOpnameDraftAction` di web.
 *
 * Yang diuji hanya keputusan murninya — "aturan mana yang berlaku" dan "berapa
 * jatahnya". Pemeriksaan berapa opname yang sudah finalized butuh database dan ada di
 * `OpnameRepository`; memisahkannya begini yang membuat bagian ini bisa diuji sama
 * sekali.
 */
class OpnameTanggalTest {

    private val CICURUG = "d9a2ef93-c298-4501-a471-1c5e2b3dff08"
    private val EMPANG = "550e8400-e29b-41d4-a716-446655440002"
    private val JATIWARINGIN = "550e8400-e29b-41d4-a716-446655440010"
    private val CIBINONG = "550e8400-e29b-41d4-a716-446655440014"
    private val OUTLET_TES = "eb174b2b-ff69-47eb-97af-b6c824d3ce4a"
    private val OUTLET_BIASA = "11111111-2222-3333-4444-555555555555"

    // ------------------------------------------------------------------ pengalihan

    @Test
    fun `outlet tanpa aturan tidak pernah dialihkan`() {
        assertNull(OpnameTanggal.pengalihanUntuk(OUTLET_BIASA, "2026-09-09"))
        assertNull(OpnameTanggal.pengalihanUntuk(OUTLET_BIASA, "2026-08-21"))
    }

    @Test
    fun `aturan hanya berlaku pada tanggal yang disebut`() {
        assertNull(OpnameTanggal.pengalihanUntuk(CICURUG, "2026-09-08"))
        assertNull(OpnameTanggal.pengalihanUntuk(CICURUG, "2026-09-10"))
    }

    /**
     * Cabang yang menjadi alasan seluruh berkas ini diport: aturan Cicurug menyebut
     * 9 September, tanggal yang masih hidup saat native diperiksa.
     */
    @Test
    fun `Cicurug dialihkan ke 2 September pada 3 dan 9 September`() {
        listOf("2026-09-03", "2026-09-09").forEach { hariIni ->
            val aturan = OpnameTanggal.pengalihanUntuk(CICURUG, hariIni)
            assertEquals("2026-09-02", aturan?.tanggalTujuan)
            assertEquals(1, aturan?.minimalFinalized)
        }
    }

    /** Empang butuh DUA opname finalized untuk menutup kuotanya, bukan satu. */
    @Test
    fun `Empang menuntut dua finalized pada tanggal tujuan`() {
        val aturan = OpnameTanggal.pengalihanUntuk(EMPANG, "2026-08-24")
        assertEquals("2026-08-23", aturan?.tanggalTujuan)
        assertEquals(2, aturan?.minimalFinalized)
    }

    @Test
    fun `lima outlet susulan dialihkan ke 5 September`() {
        listOf(
            CIBINONG,
            "550e8400-e29b-41d4-a716-446655440018",
            "550e8400-e29b-41d4-a716-446655440001",
            "550e8400-e29b-41d4-a716-446655440013",
            JATIWARINGIN,
        ).forEach { outlet ->
            assertEquals(
                "outlet $outlet",
                "2026-09-05",
                OpnameTanggal.pengalihanUntuk(outlet, "2026-09-06")?.tanggalTujuan,
            )
        }
    }

    /** Jatiwaringin punya dua aturan; yang 30 Agustus tidak boleh tertimpa yang 6 September. */
    @Test
    fun `Jatiwaringin tetap punya aturan 30 Agustus`() {
        assertEquals(
            "2026-08-29",
            OpnameTanggal.pengalihanUntuk(JATIWARINGIN, "2026-08-30")?.tanggalTujuan,
        )
    }

    // ---------------------------------------------------------------- jatah ganda

    @Test
    fun `outlet biasa hanya boleh satu opname sehari`() {
        assertEquals(1, OpnameTanggal.maksimalOpname(OUTLET_BIASA, "2026-09-09"))
        assertEquals(1, OpnameTanggal.maksimalOpname(CICURUG, "2026-09-09"))
    }

    @Test
    fun `outlet tes boleh berkali-kali pada tanggal berapa pun`() {
        assertEquals(999, OpnameTanggal.maksimalOpname(OUTLET_TES, "2026-09-09"))
        assertEquals(999, OpnameTanggal.maksimalOpname(OUTLET_TES, "2030-01-01"))
    }

    /**
     * Jatah ganda melekat pada tanggal EFEKTIF, bukan tanggal kalender. Cicurug
     * dialihkan pada 9 September ke 2 September, dan jatah gandanya ada di 2 September
     * itu — bukan di 9-nya.
     */
    @Test
    fun `jatah ganda Cicurug melekat pada tanggal tujuan`() {
        assertEquals(2, OpnameTanggal.maksimalOpname(CICURUG, "2026-09-02"))
        assertEquals(2, OpnameTanggal.maksimalOpname(CICURUG, "2026-09-03"))
        assertEquals(1, OpnameTanggal.maksimalOpname(CICURUG, "2026-09-09"))
    }

    @Test
    fun `jatah ganda outlet susulan berlaku pada 5 dan 6 September`() {
        assertEquals(2, OpnameTanggal.maksimalOpname(CIBINONG, "2026-09-05"))
        assertEquals(2, OpnameTanggal.maksimalOpname(CIBINONG, "2026-09-06"))
        assertEquals(1, OpnameTanggal.maksimalOpname(CIBINONG, "2026-09-07"))
    }

    /** Jatiwaringin masuk dua daftar jatah; yang berlaku harus yang terbesar, bukan yang pertama. */
    @Test
    fun `outlet dengan lebih dari satu aturan memakai jatah terbesar`() {
        assertEquals(2, OpnameTanggal.maksimalOpname(JATIWARINGIN, "2026-08-29"))
        assertEquals(2, OpnameTanggal.maksimalOpname(JATIWARINGIN, "2026-09-05"))
    }

    // ------------------------------------------------------------------ konsistensi

    /**
     * Setiap aturan pengalihan harus punya jatah ganda pada tanggal tujuannya.
     *
     * Tanpa itu, outlet dialihkan ke tanggal mundur lalu langsung terbentur "sudah
     * opname hari itu" — pengalihannya jadi sia-sia. Satu-satunya kekecualian yang sah
     * adalah aturan yang tanggal tujuannya memang belum pernah dipakai opname.
     */
    @Test
    fun `tanggal tujuan Empang Jatiwaringin Cicurug dan susulan punya jatah ganda`() {
        OpnameTanggal.PENGALIHAN
            .filter { it.outletId != "62a56103-2085-4dd5-9d25-a3c0cffc88ff" } // Cileungsi: sekali saja
            .filter { it.outletId != "550e8400-e29b-41d4-a716-446655440003" } // Paledang: sekali saja
            .forEach { aturan ->
                assertEquals(
                    "${aturan.namaOutlet} pada ${aturan.tanggalTujuan}",
                    2,
                    OpnameTanggal.maksimalOpname(aturan.outletId, aturan.tanggalTujuan),
                )
            }
    }
}
