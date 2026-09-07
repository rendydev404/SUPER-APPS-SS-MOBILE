package com.sukashawarma.superapp.feature.manager.domain

import com.sukashawarma.superapp.domain.model.Role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InventarisTest {

    private fun item(
        id: String = "i1",
        nama: String = "Kompor",
        mode: ModeItem = ModeItem.JUMLAH,
        targetQty: Double? = 2.0,
        targetMin: Double? = null,
        targetMax: Double? = null,
        subsection: String = "Peralatan",
        urutan: Int = 1,
    ) = ItemMaster(id, "interior", subsection, nama, mode, targetQty, targetMin, targetMax, "unit", urutan)

    private fun isian(
        jumlah: String = "2",
        ada: Boolean = true,
        catatan: String = "",
        foto: String? = "u/drafts/o1/i1.webp",
    ) = IsianItem(jumlah = jumlah, ada = ada, catatan = catatan, fotoPath = foto)

    /* ---------------- mode & kondisi ---------------- */

    @Test
    fun `mode dan kondisi dipetakan dari nilai database`() {
        assertEquals(ModeItem.KEBERADAAN, ModeItem.dari("presence"))
        assertEquals(ModeItem.RENTANG, ModeItem.dari("range"))
        // Nilai tak dikenal jatuh ke jumlah, bukan meledak di tengah form.
        assertEquals(ModeItem.JUMLAH, ModeItem.dari("entah"))
        assertEquals(KondisiAset.RUSAK, KondisiAset.dari("rusak"))
        assertEquals(KondisiAset.BAIK, KondisiAset.dari(null))
    }

    /* ---------------- label target ---------------- */

    @Test
    fun `label target mengikuti mode itemnya`() {
        assertEquals("Target min. 2 unit", item().targetTeks)
        assertEquals(
            "Target 3–5 unit",
            item(mode = ModeItem.RENTANG, targetQty = null, targetMin = 3.0, targetMax = 5.0).targetTeks,
        )
        assertEquals("Wajib tersedia", item(mode = ModeItem.KEBERADAAN).targetTeks)
    }

    /* ---------------- penilaian otomatis ---------------- */

    @Test
    fun `mode jumlah sesuai bila mencapai target`() {
        assertEquals("sesuai", nilaiPenilaian(item(targetQty = 2.0), isian(jumlah = "2")))
        assertEquals("sesuai", nilaiPenilaian(item(targetQty = 2.0), isian(jumlah = "5")))
        assertEquals("kurang", nilaiPenilaian(item(targetQty = 2.0), isian(jumlah = "1")))
    }

    @Test
    fun `jumlah kosong dihitung kurang bukan nol`() {
        assertEquals("kurang", nilaiPenilaian(item(), isian(jumlah = "")))
    }

    @Test
    fun `mode rentang menilai batas bawah dan atas`() {
        val rentang = item(mode = ModeItem.RENTANG, targetQty = null, targetMin = 3.0, targetMax = 5.0)
        assertEquals("sesuai", nilaiPenilaian(rentang, isian(jumlah = "3")))
        assertEquals("sesuai", nilaiPenilaian(rentang, isian(jumlah = "5")))
        assertEquals("di_luar_target", nilaiPenilaian(rentang, isian(jumlah = "2")))
        assertEquals("di_luar_target", nilaiPenilaian(rentang, isian(jumlah = "6")))
    }

    @Test
    fun `mode keberadaan tidak melihat angka sama sekali`() {
        val ada = item(mode = ModeItem.KEBERADAAN)
        assertEquals("sesuai", nilaiPenilaian(ada, isian(jumlah = "", ada = true)))
        assertEquals("tidak_ada", nilaiPenilaian(ada, isian(jumlah = "99", ada = false)))
    }

    /* ---------------- kelengkapan ---------------- */

    @Test
    fun `item lengkap butuh angka sah dan foto`() {
        assertTrue(itemLengkap(item(), isian()))
        assertFalse(itemLengkap(item(), isian(jumlah = "")))
        assertFalse(itemLengkap(item(), isian(foto = null)))
        assertFalse(itemLengkap(item(), null))
    }

    @Test
    fun `item keberadaan lengkap tanpa angka asal ada foto`() {
        assertTrue(itemLengkap(item(mode = ModeItem.KEBERADAAN), isian(jumlah = "")))
        assertFalse(itemLengkap(item(mode = ModeItem.KEBERADAAN), isian(jumlah = "", foto = null)))
    }

    /** Aturan ini juga ada di server; menyalinnya ke sini supaya ketahuan sebelum kirim. */
    @Test
    fun `freezer wajib diberi catatan`() {
        val freezer = item(nama = "FREEZER 600L")
        assertTrue(freezer.catatanWajib)
        assertEquals("Catatan freezer wajib diisi", halanganItem(freezer, isian(catatan = "")))
        assertNull(halanganItem(freezer, isian(catatan = "600L, bunga es tipis")))
        assertFalse(item(nama = "Kompor").catatanWajib)
    }

    @Test
    fun `halangan item menyebut sebab yang paling awal`() {
        assertEquals("Belum diisi", halanganItem(item(), null))
        assertEquals("Jumlah belum diisi", halanganItem(item(), isian(jumlah = "")))
        assertEquals("Foto belum ada", halanganItem(item(), isian(foto = null)))
        assertNull(halanganItem(item(), isian()))
    }

    /* ---------------- kelompok & kemajuan ---------------- */

    @Test
    fun `item dikelompokkan per subsection mengikuti urutan`() {
        val hasil = kelompokPerSubsection(
            listOf(
                item(id = "b", subsection = "Utilitas", urutan = 20),
                item(id = "a", subsection = "Peralatan", urutan = 10),
                item(id = "c", subsection = "Peralatan", urutan = 15),
            ),
        )
        assertEquals(listOf("Peralatan", "Utilitas"), hasil.map { it.first })
        assertEquals(listOf("a", "c"), hasil.first().second.map { it.id })
    }

    @Test
    fun `kemajuan dihitung dari item yang lengkap`() {
        val items = listOf(item(id = "a"), item(id = "b"), item(id = "c"), item(id = "d"))
        val isi = mapOf("a" to isian(), "b" to isian(), "c" to isian(foto = null))
        assertEquals(50, kemajuanIsian(items, isi))
        assertEquals(0, kemajuanIsian(emptyList(), isi))
        assertEquals(100, kemajuanIsian(items, items.associate { it.id to isian() }))
    }

    /* ---------------- halangan kirim ---------------- */

    @Test
    fun `halangan kirim menyebut nama item dan jumlah sisanya`() {
        val items = listOf(item(id = "a", nama = "Kompor"), item(id = "b", nama = "Wajan"))
        assertEquals(
            "Jumlah belum diisi: Kompor — dan 1 item lain.",
            halanganKirim(items, emptyMap()).let { it?.replace("Belum diisi", "Jumlah belum diisi") },
        )
    }

    @Test
    fun `satu item bermasalah tidak menyebut sisa`() {
        val items = listOf(item(id = "a", nama = "Kompor"), item(id = "b", nama = "Wajan"))
        val isi = mapOf("a" to isian(), "b" to isian(foto = null))
        assertEquals("Foto belum ada: Wajan.", halanganKirim(items, isi))
    }

    @Test
    fun `laporan lengkap tidak punya halangan`() {
        val items = listOf(item(id = "a"), item(id = "b"))
        assertNull(halanganKirim(items, items.associate { it.id to isian() }))
    }

    @Test
    fun `master kosong ditolak dengan sebab yang jelas`() {
        assertEquals("Master inventaris belum termuat.", halanganKirim(emptyList(), emptyMap()))
    }

    /* ---------------- peran ---------------- */

    /** Cermin `isReportViewer` web: RM dan admin dialihkan ke laporan, AM mengisi. */
    @Test
    fun `area manager mengisi sedangkan regional manager melihat laporan`() {
        assertTrue(mengisiInventaris(Role.AREA_MANAGER))
        assertFalse(mengisiInventaris(Role.REGIONAL_MANAGER))
        assertFalse(mengisiInventaris(Role.ADMIN))
        assertFalse(mengisiInventaris(null))

        assertTrue(melihatLaporanInventaris(Role.REGIONAL_MANAGER))
        assertFalse(melihatLaporanInventaris(Role.AREA_MANAGER))
    }
}
