package com.sukashawarma.superapp.feature.stok.ui.mutasi

import com.sukashawarma.superapp.domain.model.Role
import com.sukashawarma.superapp.feature.stok.data.model.OutletRingkas
import com.sukashawarma.superapp.feature.stok.domain.MutasiBadge
import com.sukashawarma.superapp.feature.stok.domain.MutasiRingkas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Mengunci kesejalanan antara lencana mutasi di bilah bawah dan cakupan yang dibuka
 * layar Mutasi.
 *
 * Ini bukan uji hitung — itu sudah di `MutasiBadgeTest`. Yang dijaga di sini adalah
 * kesalahan yang benar-benar terjadi: lencana memakai lingkup null (seluruh outlet)
 * untuk peran pusat, sementara layar memaksa satu outlet, sehingga angkanya
 * menjanjikan pekerjaan yang halamannya tidak bisa tunjukkan.
 */
class CakupanMutasiTest {

    private val outletA = OutletRingkas("outlet-a", "Outlet A")
    private val outletB = OutletRingkas("outlet-b", "Outlet B")

    private val contoh = listOf(
        MutasiRingkas("1", "menunggu_persetujuan", "outlet-a", "outlet-b"),
        MutasiRingkas("2", "menunggu_persetujuan", "outlet-b", "outlet-c"),
        MutasiRingkas("3", "dikirim", "outlet-a", "outlet-b"),
    )

    @Test
    fun `SEMUA_OUTLET dikenali dan outlet asli tidak`() {
        assertTrue(SEMUA_OUTLET.adalahSemuaOutlet)
        assertFalse(outletA.adalahSemuaOutlet)
        assertFalse(null.adalahSemuaOutlet)
    }

    /**
     * Inti perbaikannya: lingkup null yang dipakai lencana harus menghasilkan angka
     * yang sama dengan yang dilihat layar saat membuka cakupan Semua Outlet.
     */
    @Test
    fun `lingkup lencana peran pusat sama dengan cakupan Semua Outlet`() {
        val lingkupLayar = SEMUA_OUTLET.id.ifEmpty { null }
        assertEquals(null, lingkupLayar)

        val lencana = MutasiBadge.hitung(contoh, Role.ADMIN, null)
        val layar = MutasiBadge.hitung(contoh, Role.ADMIN, lingkupLayar)
        assertEquals(lencana.total, layar.total)
        assertEquals(3, lencana.total)
    }

    /** Outlet konkret tetap menyaring seperti biasa — cakupan Semua tidak bocor ke sana. */
    @Test
    fun `outlet konkret tetap menyaring`() {
        assertEquals("outlet-a", outletA.id.ifEmpty { null })
        val n = MutasiBadge.hitung(contoh, Role.CREW, outletB.id)
        assertEquals(0, n.menungguPersetujuan)
        assertEquals(1, n.dikirim)
        assertEquals(1, n.total)
    }
}
