package com.sukashawarma.superapp.feature.manager.domain

import com.sukashawarma.superapp.domain.model.Role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AreaManagerNamaTest {

    @Test
    fun `area manager melihat seluruh outlet binaannya sebagai zona dirinya`() {
        val zona = AreaManagerNama.untuk(
            outletId = "o1",
            namaOutlet = "SS PEKAYON",
            pemetaan = mapOf("o1" to "Mulyadi"),
            rolePengguna = Role.AREA_MANAGER,
            namaPengguna = "Budi",
        )
        assertEquals("Budi", zona)
    }

    @Test
    fun `area manager tanpa nama tetap dapat label yang bisa dibaca`() {
        val zona = AreaManagerNama.untuk("o1", "SS PEKAYON", emptyMap(), Role.AREA_MANAGER, "")
        assertEquals("Area Anda", zona)
    }

    @Test
    fun `pemetaan staff_outlets menang atas pencocokan nama outlet`() {
        // Nama outlet mengandung "EMPANG" yang di daftar cadangan milik Abu Bakar.
        val zona = AreaManagerNama.untuk(
            "o1", "SS EMPANG", mapOf("o1" to "Muchtar"), Role.REGIONAL_MANAGER, "Rina",
        )
        assertEquals("Muchtar", zona)
    }

    @Test
    fun `pemetaan berisi nama yang diabaikan jatuh ke pencocokan nama outlet`() {
        val zona = AreaManagerNama.untuk(
            "o1", "SS EMPANG", mapOf("o1" to "AREA MANAGER 3"), Role.REGIONAL_MANAGER, "Rina",
        )
        assertEquals("Abu Bakar", zona)
    }

    @Test
    fun `pencocokan nama outlet tidak peduli huruf besar kecil`() {
        assertEquals(
            "Tri Rizky",
            AreaManagerNama.untuk("o1", "ss kalisari", emptyMap(), Role.REGIONAL_MANAGER, "Rina"),
        )
    }

    @Test
    fun `outlet di luar semua daftar menjadi Lainnya`() {
        assertEquals(
            "Lainnya",
            AreaManagerNama.untuk("o1", "SS SURABAYA", emptyMap(), Role.REGIONAL_MANAGER, "Rina"),
        )
    }

    @Test
    fun `nama penampung dan akun sistem dianggap bukan zona`() {
        listOf(
            null, "", "   ", "Area Manager 2", "AM TEST", "admin", "Developer",
            "Lainnya", "LAIN-LAIN", "Other", "others",
        ).forEach { assertTrue("harus diabaikan: $it", AreaManagerNama.diabaikan(it)) }
    }

    @Test
    fun `nama orang sungguhan tidak diabaikan`() {
        listOf("Abu Bakar", "Muchtar", "Chairul Rizky", "Tri Rizky", "Mulyadi")
            .forEach { assertFalse("tidak boleh diabaikan: $it", AreaManagerNama.diabaikan(it)) }
    }
}
