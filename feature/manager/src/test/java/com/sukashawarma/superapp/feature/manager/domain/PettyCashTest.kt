package com.sukashawarma.superapp.feature.manager.domain

import com.sukashawarma.superapp.domain.model.Role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.OffsetDateTime

class PettyCashTest {

    private val kamis = LocalDate.of(2026, 9, 3)
    private val sekarang = OffsetDateTime.parse("2026-09-03T10:00:00+07:00")

    private fun topup(
        id: String = "aabbccdd-1111-2222-3333-444455556666",
        status: StatusTopup = StatusTopup.DIAJUKAN,
        dibuat: String = "2026-09-03T02:00:00+00:00",
        bank: String? = "BCA",
        rekening: String? = "1234567890",
        atasNama: String? = "Budi",
    ) = TopupPettyCash(
        id = id,
        outletId = "o1",
        outletNama = "SS EMPANG",
        jumlah = 500_000,
        deskripsi = "Beli gas",
        status = status,
        dibuatPada = dibuat,
        pengajuNama = "Leader Empang",
        namaBank = bank,
        nomorRekening = rekening,
        atasNama = atasNama,
        buktiTransferUrl = null,
    )

    /* ---------------- status & aksi ---------------- */

    @Test
    fun `status dipetakan dari nilai kolom apa adanya`() {
        assertEquals(StatusTopup.DIAJUKAN, StatusTopup.dari("pending"))
        assertEquals(StatusTopup.DISERAHKAN_AM, StatusTopup.dari("forwarded_by_area_manager"))
        assertNull(StatusTopup.dari("PENDING"))
        assertNull(StatusTopup.dari(null))
    }

    @Test
    fun `hanya status menunggu ACC yang menawarkan tombol ACC dan tolak`() {
        STATUS_MENUNGGU_ACC.forEach { assertEquals(AksiTopup.ACC_ATAU_TOLAK, aksiUntuk(it)) }
    }

    @Test
    fun `status siap diserahkan menawarkan tombol serahkan`() {
        STATUS_SIAP_DISERAHKAN.forEach { assertEquals(AksiTopup.SERAHKAN, aksiUntuk(it)) }
    }

    @Test
    fun `status yang sudah tuntas tidak menawarkan tindakan apa pun`() {
        listOf(
            StatusTopup.DITERUSKAN_KE_FINANCE,
            StatusTopup.DISERAHKAN_AM,
            StatusTopup.DISERAHKAN_LEADER,
            StatusTopup.SELESAI,
            StatusTopup.DITOLAK,
        ).forEach { assertEquals(AksiTopup.TIDAK_ADA, aksiUntuk(it)) }
    }

    /** RPC di database menolak regional_manager; tombolnya tidak boleh menjanjikan sebaliknya. */
    @Test
    fun `regional manager tidak boleh memproses petty cash`() {
        assertTrue(bolehMemprosesPettyCash(Role.AREA_MANAGER))
        assertFalse(bolehMemprosesPettyCash(Role.REGIONAL_MANAGER))
        assertFalse(bolehMemprosesPettyCash(Role.LEADER))
        assertFalse(bolehMemprosesPettyCash(null))
    }

    /* ---------------- catatan finance ---------------- */

    @Test
    fun `catatan finance bentuk sematan dipisahkan`() {
        val (alasan, catatan) = pisahCatatanFinance("Beli gas 📌 [Nota kurang jelas]")
        assertEquals("Beli gas", alasan)
        assertEquals("Nota kurang jelas", catatan)
    }

    @Test
    fun `catatan finance dalam kurung siku dipisahkan`() {
        val (alasan, catatan) = pisahCatatanFinance("Beli gas [Catatan Finance: Nota kurang jelas]")
        assertEquals("Beli gas", alasan)
        assertEquals("Nota kurang jelas", catatan)
    }

    @Test
    fun `catatan finance dalam kurung biasa dipisahkan`() {
        val (alasan, catatan) = pisahCatatanFinance("Beli gas (Catatan Finance: Nota kurang jelas)")
        assertEquals("Beli gas", alasan)
        assertEquals("Nota kurang jelas", catatan)
    }

    /** Data lama punya kurung yang tidak pernah ditutup; jangan sampai catatannya hilang. */
    @Test
    fun `kurung siku yang tidak ditutup tetap terbaca`() {
        val (alasan, catatan) = pisahCatatanFinance("Beli gas [Catatan Finance: Nota kurang jelas")
        assertEquals("Beli gas", alasan)
        assertEquals("Nota kurang jelas", catatan)
    }

    @Test
    fun `kurung biasa yang tidak ditutup tetap terbaca`() {
        val (alasan, catatan) = pisahCatatanFinance("Beli gas (Catatan Finance: Nota kurang jelas")
        assertEquals("Beli gas", alasan)
        assertEquals("Nota kurang jelas", catatan)
    }

    @Test
    fun `deskripsi tanpa catatan dikembalikan utuh`() {
        val (alasan, catatan) = pisahCatatanFinance("Beli gas untuk kompor")
        assertEquals("Beli gas untuk kompor", alasan)
        assertNull(catatan)
    }

    @Test
    fun `deskripsi kosong tidak membuat pemisahan gagal`() {
        assertEquals("" to null, pisahCatatanFinance(null))
        assertEquals("" to null, pisahCatatanFinance("   "))
    }

    @Test
    fun `awalan catatan finance yang berulang dibersihkan`() {
        assertEquals("Nota kurang jelas", rapikanCatatanFinance("Catatan Finance: Nota kurang jelas"))
        assertEquals("Nota kurang jelas", rapikanCatatanFinance("Catatan Finance: Catatan Finance: Nota kurang jelas"))
        assertEquals("Nota kurang jelas", rapikanCatatanFinance("Nota kurang jelas"))
    }

    /* ---------------- waktu relatif ---------------- */

    @Test
    fun `waktu relatif memakai durasi mentah seperti web`() {
        assertEquals("Baru saja", waktuRelatif("2026-09-03T09:30:00+07:00", sekarang))
        assertEquals("3 jam lalu", waktuRelatif("2026-09-03T07:00:00+07:00", sekarang))
        assertEquals("Kemarin", waktuRelatif("2026-09-02T09:00:00+07:00", sekarang))
        assertEquals("5 hari lalu", waktuRelatif("2026-08-29T09:00:00+07:00", sekarang))
    }

    /** Jam perangkat yang lebih lambat dari server tidak boleh menghasilkan angka minus. */
    @Test
    fun `waktu di masa depan dibaca sebagai baru saja`() {
        assertEquals("Baru saja", waktuRelatif("2026-09-03T12:00:00+07:00", sekarang))
    }

    @Test
    fun `cap waktu tak terbaca dikembalikan apa adanya`() {
        assertEquals("bukan tanggal", waktuRelatif("bukan tanggal", sekarang))
        assertNull(tanggalJakarta("bukan tanggal"))
    }

    /* ---------------- penyaring tanggal ---------------- */

    @Test
    fun `penyaring hari ini dan kemarin memakai hari kalender Jakarta`() {
        assertTrue(dalamRentangTanggal(kamis, FilterTanggal.HARI_INI, kamis))
        assertFalse(dalamRentangTanggal(kamis.minusDays(1), FilterTanggal.HARI_INI, kamis))
        assertTrue(dalamRentangTanggal(kamis.minusDays(1), FilterTanggal.KEMARIN, kamis))
    }

    /**
     * Batas web memang longgar sehari: "7 Hari Terakhir" mencakup selisih 0..7.
     * Dikunci di sini supaya tidak "diperbaiki" diam-diam dan bikin angka berselisih.
     */
    @Test
    fun `tujuh hari terakhir mencakup delapan hari kalender`() {
        assertTrue(dalamRentangTanggal(kamis.minusDays(7), FilterTanggal.TUJUH_HARI, kamis))
        assertFalse(dalamRentangTanggal(kamis.minusDays(8), FilterTanggal.TUJUH_HARI, kamis))
        assertTrue(dalamRentangTanggal(kamis.minusDays(30), FilterTanggal.TIGA_PULUH_HARI, kamis))
        assertFalse(dalamRentangTanggal(kamis.minusDays(31), FilterTanggal.TIGA_PULUH_HARI, kamis))
    }

    @Test
    fun `penyaring semua tanggal melewatkan apa pun termasuk tanggal tak terbaca`() {
        assertTrue(dalamRentangTanggal(null, FilterTanggal.SEMUA, kamis))
        assertFalse(dalamRentangTanggal(null, FilterTanggal.HARI_INI, kamis))
    }

    /* ---------------- penyaring daftar ---------------- */

    private val semuaStatus = StatusTopup.entries.mapIndexed { i, s ->
        topup(id = "id-$i", status = s)
    }

    @Test
    fun `tab review dan riwayat membagi habis seluruh status tanpa tumpang tindih`() {
        val review = saringReview(semuaStatus, FilterReview.SEMUA, FilterTanggal.SEMUA, kamis)
        val riwayat = saringRiwayat(semuaStatus, FilterRiwayat.SEMUA, FilterTanggal.SEMUA, kamis)
        assertEquals(semuaStatus.size, review.size + riwayat.size)
        assertTrue(review.map { it.id }.intersect(riwayat.map { it.id }.toSet()).isEmpty())
    }

    @Test
    fun `filter belum di-ACC hanya menyisakan yang menunggu keputusan`() {
        val hasil = saringReview(semuaStatus, FilterReview.BELUM_ACC, FilterTanggal.SEMUA, kamis)
        assertEquals(STATUS_MENUNGGU_ACC, hasil.map { it.status }.toSet())
    }

    @Test
    fun `filter siap diserahkan hanya menyisakan yang dananya sudah cair`() {
        val hasil = saringReview(semuaStatus, FilterReview.SIAP_DISERAHKAN, FilterTanggal.SEMUA, kamis)
        assertEquals(STATUS_SIAP_DISERAHKAN, hasil.map { it.status }.toSet())
    }

    @Test
    fun `filter selesai mencakup dua status yang sama-sama berarti tuntas`() {
        val hasil = saringRiwayat(semuaStatus, FilterRiwayat.SELESAI, FilterTanggal.SEMUA, kamis)
        assertEquals(
            setOf(StatusTopup.SELESAI, StatusTopup.DISERAHKAN_LEADER),
            hasil.map { it.status }.toSet(),
        )
    }

    @Test
    fun `filter ditolak dan proses finance menyaring satu status masing-masing`() {
        assertEquals(
            listOf(StatusTopup.DITOLAK),
            saringRiwayat(semuaStatus, FilterRiwayat.DITOLAK, FilterTanggal.SEMUA, kamis).map { it.status },
        )
        assertEquals(
            listOf(StatusTopup.DITERUSKAN_KE_FINANCE),
            saringRiwayat(semuaStatus, FilterRiwayat.PROSES_FINANCE, FilterTanggal.SEMUA, kamis).map { it.status },
        )
    }

    @Test
    fun `penyaring tanggal ikut berlaku di kedua tab`() {
        val lama = topup(id = "lama", status = StatusTopup.DIAJUKAN, dibuat = "2026-01-01T02:00:00+00:00")
        val daftar = listOf(topup(), lama)
        assertEquals(1, saringReview(daftar, FilterReview.SEMUA, FilterTanggal.HARI_INI, kamis).size)
        assertEquals(2, saringReview(daftar, FilterReview.SEMUA, FilterTanggal.SEMUA, kamis).size)
    }

    /* ---------------- tampilan ---------------- */

    @Test
    fun `rekening dirangkai lengkap dan hilang saat bank tidak diisi`() {
        assertEquals("BCA 1234567890 (a.n Budi)", topup().rekeningTeks)
        assertNull(topup(bank = null).rekeningTeks)
        assertEquals("BCA", topup(rekening = null, atasNama = null).rekeningTeks)
    }

    @Test
    fun `id ringkas memakai delapan karakter pertama`() {
        assertEquals("aabbccdd", topup().idRingkas)
    }
}
