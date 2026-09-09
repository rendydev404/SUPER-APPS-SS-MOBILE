package com.sukashawarma.superapp.feature.distribusi.ui.dashboard

import com.sukashawarma.superapp.feature.distribusi.data.model.SuratJalanRingkas
import com.sukashawarma.superapp.feature.distribusi.domain.StatusSuratJalan
import org.junit.Assert.assertEquals
import org.junit.Test

class SaringDaftarTest {

    private fun baris(
        id: String,
        status: StatusSuratJalan,
        outlet: String? = "Outlet A",
        nomor: String? = "SJ-001",
        selisih: Boolean = false,
        dibuatPada: String? = "2026-09-04T00:00:00Z",
    ) = SuratJalanRingkas(id, "o1", status, outlet, nomor, dibuatPada, selisih)

    private val sumber = listOf(
        baris("1", StatusSuratJalan.DRAFT, nomor = "SJ-001"),
        baris("2", StatusSuratJalan.DIKIRIM, nomor = "SJ-002"),
        baris("3", StatusSuratJalan.DIKIRIM_LENGKAP, nomor = "SJ-003"),
        baris("4", StatusSuratJalan.DITERIMA_LENGKAP, nomor = "SJ-004"),
        baris("5", StatusSuratJalan.DITERIMA_SEBAGIAN, nomor = "SJ-005", selisih = true),
        baris("6", StatusSuratJalan.SELESAI, outlet = "Outlet B", nomor = "SJ-006"),
    )

    @Test
    fun `tab semua tidak menyaring apa pun`() {
        assertEquals(6, saringDaftar(sumber, TabStatus.SEMUA, null, "").size)
    }

    @Test
    fun `tab dikirim mencakup dikirim lengkap`() {
        val hasil = saringDaftar(sumber, TabStatus.DIKIRIM, null, "")
        assertEquals(listOf("2", "3"), hasil.map { it.id })
    }

    @Test
    fun `tab belum verifikasi hanya yang sudah diterima tapi belum ditutup`() {
        val hasil = saringDaftar(sumber, TabStatus.BELUM_VERIF, null, "")
        assertEquals(listOf("4", "5"), hasil.map { it.id })
    }

    @Test
    fun `tab selisih hanya yang bermasalah`() {
        assertEquals(listOf("5"), saringDaftar(sumber, TabStatus.SELISIH, null, "").map { it.id })
    }

    @Test
    fun `filter outlet cocok persis nama`() {
        assertEquals(listOf("6"), saringDaftar(sumber, TabStatus.SEMUA, "Outlet B", "").map { it.id })
    }

    @Test
    fun `pencarian mencocokkan nomor dokumen tanpa peduli besar kecil huruf`() {
        assertEquals(listOf("4"), saringDaftar(sumber, TabStatus.SEMUA, null, "sj-004").map { it.id })
    }

    @Test
    fun `pencarian juga mencocokkan nama outlet`() {
        assertEquals(listOf("6"), saringDaftar(sumber, TabStatus.SEMUA, null, "outlet b").map { it.id })
    }

    @Test
    fun `pencarian yang hanya spasi diabaikan`() {
        assertEquals(6, saringDaftar(sumber, TabStatus.SEMUA, null, "   ").size)
    }

    @Test
    fun `tab dan pencarian digabung, bukan saling menggantikan`() {
        val hasil = saringDaftar(sumber, TabStatus.BELUM_VERIF, null, "SJ-005")
        assertEquals(listOf("5"), hasil.map { it.id })
    }

    @Test
    fun `hasil selalu diurutkan dari surat jalan terbaru`() {
        val sumberTidakUrut = listOf(
            baris("lama", StatusSuratJalan.DRAFT, dibuatPada = "2026-09-01T08:00:00Z"),
            baris("tanpa-tanggal", StatusSuratJalan.DRAFT, dibuatPada = null),
            baris("baru", StatusSuratJalan.DRAFT, dibuatPada = "2026-09-07T08:00:00Z"),
        )

        assertEquals(
            listOf("baru", "lama", "tanpa-tanggal"),
            saringDaftar(sumberTidakUrut, TabStatus.SEMUA, null, "").map { it.id },
        )
    }

    @Test
    fun `pagination membatasi daftar pada lima surat jalan per halaman`() {
        val daftar = (1..12).map { nomor ->
            baris(
                id = nomor.toString(),
                status = StatusSuratJalan.DRAFT,
                dibuatPada = "2026-09-${nomor.toString().padStart(2, '0')}T08:00:00Z",
            )
        }

        val halamanDua = halamanSuratJalan(daftar, 2)
        assertEquals(3, halamanDua.totalHalaman)
        assertEquals(listOf("6", "7", "8", "9", "10"), halamanDua.baris.map { it.id })
        assertEquals(6, halamanDua.urutanMulai)
        assertEquals(10, halamanDua.urutanAkhir)

        val halamanTerakhir = halamanSuratJalan(daftar, 99)
        assertEquals(3, halamanTerakhir.nomor)
        assertEquals(listOf("11", "12"), halamanTerakhir.baris.map { it.id })
    }

    @Test
    fun `refresh mempertahankan halaman aktif dan clamp bila halaman hilang`() {
        val daftar = (1..12).map { nomor ->
            baris(
                id = nomor.toString(),
                status = StatusSuratJalan.DRAFT,
                dibuatPada = "2026-09-${nomor.toString().padStart(2, '0')}T08:00:00Z",
            )
        }

        assertEquals(2, halamanAktifSetelahMuat(daftar, 2))
        assertEquals(3, halamanAktifSetelahMuat(daftar, 99))
    }
}
