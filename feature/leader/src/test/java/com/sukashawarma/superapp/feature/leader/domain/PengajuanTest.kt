package com.sukashawarma.superapp.feature.leader.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PengajuanTest {

    private fun pengajuan(status: StatusPengajuan) = Pengajuan(
        id = status.nilai,
        outletId = "outlet",
        outletNama = "Cabang",
        jumlah = 500_000,
        deskripsi = "Beli es kristal",
        status = status,
        dibuatPada = "2026-09-08T03:00:00.000+00:00",
        namaBank = "BCA",
        nomorRekening = "1234567890",
        atasNama = "Suka Shawarma",
        buktiTransferUrl = null,
    )

    private val semua = StatusPengajuan.entries.map { pengajuan(it) }

    /**
     * Ini bukan pilihan tampilan: `leader_forward_funds` menolak status lain apa pun.
     * Tombol yang muncul di kartu selain ini akan selalu berujung galat server.
     */
    @Test
    fun `hanya siap diserahkan yang menuntut tindakan leader`() {
        val butuh = semua.filter { it.butuhAksi }.map { it.status }
        assertEquals(listOf(StatusPengajuan.SIAP_DISERAHKAN), butuh)
    }

    @Test
    fun `filter butuh aksi hanya memuat satu status itu`() {
        val hasil = saringPengajuan(semua, FilterPengajuan.BUTUH_AKSI)
        assertEquals(listOf(StatusPengajuan.SIAP_DISERAHKAN), hasil.map { it.status })
    }

    /**
     * `pending` adalah bentuk lama dari pengajuan baru; sejak
     * `20300105000014_petty_cash_topup_leader_only` RPC melahirkannya sebagai
     * `forwarded_to_area_manager`. Keduanya menunggu meja yang sama.
     */
    @Test
    fun `pengajuan lama berstatus pending ikut terhitung menunggu AM`() {
        val hasil = saringPengajuan(semua, FilterPengajuan.MENUNGGU_AM).map { it.status }
        assertTrue(StatusPengajuan.DIAJUKAN in hasil)
        assertTrue(StatusPengajuan.DITERUSKAN_KE_AM in hasil)
        assertEquals(2, hasil.size)
    }

    @Test
    fun `selesai mencakup diserahkan ke crew maupun ditutup crew`() {
        val hasil = saringPengajuan(semua, FilterPengajuan.SELESAI).map { it.status }
        assertEquals(
            setOf(StatusPengajuan.DISERAHKAN_KE_CREW, StatusPengajuan.SELESAI),
            hasil.toSet(),
        )
    }

    @Test
    fun `ditolak tidak pernah masuk filter selesai`() {
        val hasil = saringPengajuan(semua, FilterPengajuan.SELESAI).map { it.status }
        assertTrue(StatusPengajuan.DITOLAK !in hasil)
    }

    @Test
    fun `status dari database dikenali lewat nilai kolomnya`() {
        assertEquals(StatusPengajuan.SIAP_DISERAHKAN, StatusPengajuan.dari("forwarded_by_area_manager"))
        assertNull(StatusPengajuan.dari("status_yang_belum_ada"))
        assertNull(StatusPengajuan.dari(null))
    }

    // ------------------------------------------------------------------- form

    private val formLengkap = FormTopup(
        outletId = "outlet",
        nominal = "500000",
        keperluan = "Beli es kristal",
        namaBank = "BCA",
        nomorRekening = "1234567890",
        atasNama = "Suka Shawarma",
    )

    @Test
    fun `form lengkap lolos validasi`() {
        assertNull(galatForm(formLengkap))
    }

    @Test
    fun `outlet kosong ditegur lebih dulu daripada nominal kosong`() {
        val galat = galatForm(formLengkap.copy(outletId = null, nominal = ""))
        assertNotNull(galat)
        assertTrue(galat!!.contains("outlet", ignoreCase = true))
    }

    @Test
    fun `nominal nol ditolak`() {
        assertNotNull(galatForm(formLengkap.copy(nominal = "0")))
    }

    @Test
    fun `keperluan berisi spasi saja dianggap kosong`() {
        assertNotNull(galatForm(formLengkap.copy(keperluan = "   ")))
    }

    /**
     * Rekening wajib karena RPC menuliskannya balik ke `outlets` dan finance
     * mentransfer ke sana — pengajuan tanpa rekening adalah dana yang tak bisa
     * dikirim ke mana pun.
     */
    @Test
    fun `rekening tidak lengkap ditolak walau nominal dan alasan sudah benar`() {
        assertNotNull(galatForm(formLengkap.copy(nomorRekening = "")))
        assertNotNull(galatForm(formLengkap.copy(namaBank = "")))
        assertNotNull(galatForm(formLengkap.copy(atasNama = "")))
    }

    @Test
    fun `nominal membaca angka saja dan mengabaikan pemisah yang ikut terketik`() {
        assertEquals(1_250_000L, FormTopup(nominal = "1.250.000").nominalAngka)
        assertNull(FormTopup(nominal = "").nominalAngka)
        assertNull(FormTopup(nominal = "abc").nominalAngka)
    }

    @Test
    fun `nominal ditampilkan berpemisah ribuan`() {
        assertEquals("1.250.000", nominalTerformat("1250000"))
        assertEquals("", nominalTerformat(""))
    }
}
