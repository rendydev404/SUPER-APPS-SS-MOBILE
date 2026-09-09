package com.sukashawarma.superapp.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Mengunci pemisahan antara nama kepegawaian dan nama tampilan.
 *
 * Keduanya sengaja hidup berdampingan: `name` dipakai payroll, rekap absensi, dan
 * papan kehadiran di web, sementara `displayName` diatur staff sendiri. Kalau
 * suatu saat keduanya digabung — misalnya `namaTampil` diubah jadi selalu memakai
 * `displayName`, atau layar profil mulai menulis ke `name` — catatan HR ikut
 * berubah setiap kali seseorang mengganti nama panggilannya.
 */
class NamaTampilTest {

    private fun staf(nama: String, namaTampilan: String?) = StaffProfile(
        id = "1",
        outletId = null,
        outletName = null,
        name = nama,
        role = Role.CREW,
        roleRaw = "crew",
        status = "active",
        username = "budi",
        refPhotoUrl = null,
        allowManualButton = false,
        faceDescriptor = null,
        displayName = namaTampilan,
    )

    @Test
    fun `tanpa nama tampilan memakai nama kepegawaian`() {
        assertEquals("Budi Santoso", staf("Budi Santoso", null).namaTampil)
    }

    @Test
    fun `nama tampilan menang atas nama kepegawaian`() {
        assertEquals("Budi", staf("Budi Santoso", "Budi").namaTampil)
    }

    /** Server menyimpan string kosong sebagai NULL, tapi baris lama atau balasan
     *  yang belum dimuat ulang bisa membawa string kosong ke sini. Menampilkannya
     *  apa adanya akan membuat nama pengguna hilang dari layar. */
    @Test
    fun `nama tampilan kosong tidak menghapus nama di layar`() {
        assertEquals("Budi Santoso", staf("Budi Santoso", "   ").namaTampil)
    }

    /** Nama kepegawaian TIDAK ikut berubah ketika nama tampilan diatur — layar
     *  "Data Kepegawaian" harus tetap menampilkan yang asli. */
    @Test
    fun `nama kepegawaian tetap utuh`() {
        assertEquals("Budi Santoso", staf("Budi Santoso", "Budi").name)
    }
}
