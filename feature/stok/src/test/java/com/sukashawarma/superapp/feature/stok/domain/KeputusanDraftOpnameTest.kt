package com.sukashawarma.superapp.feature.stok.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class KeputusanDraftOpnameTest {

    private fun putuskan(
        tipe: String = "harian",
        draft: String? = null,
        sudahAda: String? = null,
        final: Boolean = false,
        jumlah: Int = 0,
        maksimal: Int = 1,
    ) = putuskanDraftOpname(tipe, draft, sudahAda, final, jumlah, maksimal)

    /**
     * Regresi dari kejadian 9 Sep 2026 di outlet tes: opname `harian` sudah finalized,
     * lalu kru menyimpan draft `ad_hoc`. Karena pencarian lama menyaring `tipe=harian`,
     * draft itu tak terlihat dan setiap penekanan tombol membuat draft ad_hoc BARU.
     *
     * Draft berjalan harus menang atas segalanya, termasuk atas jatah opname ganda.
     */
    @Test
    fun `draft ad_hoc berjalan dilanjutkan walau harian sudah finalized`() {
        val hasil = putuskan(
            tipe = "harian",
            draft = "draft-adhoc",
            sudahAda = "harian-final",
            final = true,
            jumlah = 2,
            maksimal = 999,
        )
        assertEquals(KeputusanDraftOpname.Lanjutkan("draft-adhoc"), hasil)
    }

    @Test
    fun `draft berjalan dilanjutkan pada hari biasa`() {
        assertEquals(
            KeputusanDraftOpname.Lanjutkan("d1"),
            putuskan(draft = "d1", sudahAda = "d1"),
        )
    }

    @Test
    fun `belum ada apa pun hari ini maka buat baru dengan tipe yang diminta`() {
        assertEquals(KeputusanDraftOpname.BuatBaru("harian"), putuskan())
    }

    /** Outlet berjatah ganda: opname kedua dibuka sebagai ad_hoc, bukan harian. */
    @Test
    fun `finalized dan jatah masih sisa maka buat ad_hoc`() {
        assertEquals(
            KeputusanDraftOpname.BuatBaru("ad_hoc"),
            putuskan(sudahAda = "final-1", final = true, jumlah = 1, maksimal = 2),
        )
    }

    @Test
    fun `finalized dan jatah habis maka tampilkan apa adanya`() {
        assertEquals(
            KeputusanDraftOpname.Tampilkan("final-1"),
            putuskan(sudahAda = "final-1", final = true, jumlah = 2, maksimal = 2),
        )
    }

    /** Outlet biasa hanya sekali sehari: tidak ada cabang ad_hoc sama sekali. */
    @Test
    fun `outlet berjatah satu tidak pernah membuat opname kedua`() {
        assertEquals(
            KeputusanDraftOpname.Tampilkan("final-1"),
            putuskan(sudahAda = "final-1", final = true, jumlah = 1, maksimal = 1),
        )
    }

    /**
     * Opname yang ada tapi BELUM final dan bukan draft (mis. `pending_approval`)
     * dikembalikan apa adanya, bukan diduplikasi.
     */
    @Test
    fun `opname tidak final dan bukan draft ditampilkan apa adanya`() {
        assertEquals(
            KeputusanDraftOpname.Tampilkan("pending-1"),
            putuskan(sudahAda = "pending-1", final = false, maksimal = 999),
        )
    }
}
