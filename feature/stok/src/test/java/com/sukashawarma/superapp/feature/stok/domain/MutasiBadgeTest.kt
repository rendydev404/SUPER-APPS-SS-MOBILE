package com.sukashawarma.superapp.feature.stok.domain

import com.sukashawarma.superapp.domain.model.Role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Port dari `apps/stok/src/lib/stok/mutasiBadge.test.ts`, kasus per kasus dengan
 * data contoh yang sama — supaya angka badge native dan web tidak bisa berpisah
 * diam-diam.
 */
class MutasiBadgeTest {

    private val contoh = listOf(
        MutasiRingkas("1", "menunggu_persetujuan", "outlet-a", "outlet-b"),
        MutasiRingkas("2", "menunggu_pengiriman", "outlet-a", "outlet-b"),
        MutasiRingkas("3", "dikirim", "outlet-a", "outlet-b"),
        MutasiRingkas("4", "dikirim", "outlet-b", "outlet-c"),
    )

    @Test
    fun `pengawas dan pusat boleh menyetujui mutasi`() {
        assertTrue(MutasiBadge.bolehMenyetujui(Role.ADMIN))
        assertTrue(MutasiBadge.bolehMenyetujui(Role.SPV))
        assertTrue(MutasiBadge.bolehMenyetujui(Role.KITCHEN))
        assertTrue(MutasiBadge.bolehMenyetujui(Role.LEADER))
        assertTrue(MutasiBadge.bolehMenyetujui(Role.OWNER))
        assertTrue(MutasiBadge.bolehMenyetujui(Role.DEVELOPER))
    }

    @Test
    fun `crew dan sesi kosong tidak boleh menyetujui`() {
        assertFalse(MutasiBadge.bolehMenyetujui(Role.CREW))
        assertFalse(MutasiBadge.bolehMenyetujui(Role.KIOSK))
        assertFalse(MutasiBadge.bolehMenyetujui(null))
    }

    @Test
    fun `penyetuju tanpa outlet melihat seluruh antrean`() {
        val n = MutasiBadge.hitung(contoh, Role.KITCHEN, null)
        assertEquals(1, n.menungguPersetujuan)
        assertEquals(1, n.menungguPengiriman)
        assertEquals(2, n.dikirim)
        assertEquals(4, n.total)
    }

    @Test
    fun `penyetuju yang terikat outlet-a`() {
        val n = MutasiBadge.hitung(contoh, Role.SPV, "outlet-a")
        assertEquals(1, n.menungguPersetujuan)
        assertEquals(1, n.menungguPengiriman)
        // outlet-a tidak menerima kiriman apa pun.
        assertEquals(0, n.dikirim)
        assertEquals(2, n.total)
    }

    @Test
    fun `crew outlet-a hanya menghitung yang harus ia kirim`() {
        val n = MutasiBadge.hitung(contoh, Role.CREW, "outlet-a")
        // Crew tidak menyetujui, jadi baris 1 tidak dihitung.
        assertEquals(0, n.menungguPersetujuan)
        assertEquals(1, n.menungguPengiriman)
        assertEquals(0, n.dikirim)
        assertEquals(1, n.total)
    }

    @Test
    fun `crew outlet-b hanya menghitung yang harus ia terima`() {
        val n = MutasiBadge.hitung(contoh, Role.CREW, "outlet-b")
        assertEquals(0, n.menungguPersetujuan)
        // Baris 4 berasal dari outlet-b tapi sudah dikirim, jadi bukan tugasnya lagi.
        assertEquals(0, n.menungguPengiriman)
        assertEquals(1, n.dikirim)
        assertEquals(1, n.total)
    }

    /**
     * Regresi dari kejadian nyata 9 September 2026: seorang area manager yang hanya
     * memegang "outlet tes" melihat lencana bernilai 27, lalu membuka halaman Mutasi
     * dan mendapatinya kosong. Ke-27 baris itu `menunggu_persetujuan` milik outlet
     * lain, dan aturan web menghitungnya untuk setiap penyetuju tanpa memandang outlet.
     *
     * Lencana tidak boleh menghitung baris yang tidak akan muncul di daftar.
     */
    @Test
    fun `penyetuju tidak menghitung persetujuan di luar outletnya`() {
        val luarOutlet = listOf(
            MutasiRingkas("x1", "menunggu_persetujuan", "outlet-lain", "outlet-lain-2"),
            MutasiRingkas("x2", "menunggu_persetujuan", "outlet-lain", "outlet-lain-3"),
        )
        val n = MutasiBadge.hitung(luarOutlet, Role.AREA_MANAGER, "outlet-tes")
        assertEquals(0, n.menungguPersetujuan)
        assertEquals(0, n.total)
    }

    /** Yang menyangkut outletnya tetap dihitung, baik sebagai asal maupun tujuan. */
    @Test
    fun `penyetuju tetap menghitung persetujuan yang menyangkut outletnya`() {
        val menyangkut = listOf(
            MutasiRingkas("y1", "menunggu_persetujuan", "outlet-tes", "outlet-lain"),
            MutasiRingkas("y2", "menunggu_persetujuan", "outlet-lain", "outlet-tes"),
            MutasiRingkas("y3", "menunggu_persetujuan", "outlet-lain", "outlet-lain-2"),
        )
        val n = MutasiBadge.hitung(menyangkut, Role.AREA_MANAGER, "outlet-tes")
        assertEquals(2, n.menungguPersetujuan)
    }

    @Test
    fun `perluTindakan ikut menolak persetujuan di luar outlet`() {
        val luar = MutasiRingkas("z", "menunggu_persetujuan", "outlet-lain", "outlet-lain-2")
        assertFalse(MutasiBadge.perluTindakan(luar, Role.AREA_MANAGER, "outlet-tes"))
        assertTrue(MutasiBadge.perluTindakan(luar, Role.AREA_MANAGER, null))
    }

    @Test
    fun `daftar kosong menghasilkan nol`() {
        val n = MutasiBadge.hitung(emptyList(), Role.ADMIN, "outlet-a")
        assertEquals(0, n.total)
        assertEquals(0, n.menungguPersetujuan)
        assertEquals(0, n.menungguPengiriman)
        assertEquals(0, n.dikirim)
    }

    /**
     * Status di luar tiga yang menuntut tindakan harus DIABAIKAN, bukan jatuh ke
     * `menunggu_persetujuan`. Ini yang membuat badge memakai string mentah alih-alih
     * `StatusMutasi.dari()`, yang memetakan nilai tak dikenal ke MENUNGGU_PERSETUJUAN.
     */
    @Test
    fun `status selesai ditolak dan tak dikenal tidak menambah angka`() {
        val selesai = listOf(
            MutasiRingkas("5", "selesai", "outlet-a", "outlet-b"),
            MutasiRingkas("6", "ditolak", "outlet-a", "outlet-b"),
            MutasiRingkas("7", "status_baru_yang_belum_ada", "outlet-a", "outlet-b"),
        )
        assertEquals(0, MutasiBadge.hitung(selesai, Role.ADMIN, null).total)
    }

    @Test
    fun `perluTindakan membedakan pengirim penerima dan penyetuju`() {
        val setujui = MutasiRingkas("a", "menunggu_persetujuan", "a", "b")
        val kirim = MutasiRingkas("b", "menunggu_pengiriman", "a", "b")
        val terima = MutasiRingkas("c", "dikirim", "a", "b")
        val rampung = MutasiRingkas("d", "selesai", "a", "b")

        assertTrue(MutasiBadge.perluTindakan(setujui, Role.SPV, "a"))
        assertFalse(MutasiBadge.perluTindakan(setujui, Role.CREW, "a"))

        assertTrue(MutasiBadge.perluTindakan(kirim, Role.CREW, "a"))
        assertFalse(MutasiBadge.perluTindakan(kirim, Role.CREW, "b"))

        assertTrue(MutasiBadge.perluTindakan(terima, Role.CREW, "b"))
        assertFalse(MutasiBadge.perluTindakan(terima, Role.CREW, "a"))

        assertFalse(MutasiBadge.perluTindakan(rampung, Role.ADMIN, "a"))
    }
}
