package com.sukashawarma.superapp.feature.stok.domain

import kotlin.math.abs

/**
 * Gerbang konfirmasi untuk form "terima barang PO" — port
 * `apps/stok/src/lib/stok/terimaGuard.ts`.
 *
 * Latar (16 September 2026): dua salah input penerimaan ditemukan dalam satu hari,
 * dan keduanya hanya bisa diperbaiki manual di database — karena
 * `verifikasi_terima_po` menghitung `GREATEST(0, qty_baru - qty_lama)`, jadi form
 * terima TIDAK PERNAH bisa mengurangi. Setiap kelebihan input jadi pekerjaan
 * koreksi tangan.
 *
 *   - KULIT 32, PO/KITCHEN/20260914/0001: 77 Pack diinput padahal yang datang
 *     KULIT 25. Stok gudang saat itu 2 Pack, jadi yang masuk 38x stok berjalan.
 *   - TEPUNG, PO/KITCHEN/20260907/0005: 100 Kg diinput dua kali padahal pesanannya
 *     cuma 100 Kg. Stoknya 136 Kg, jadi TIDAK tampak melompat — yang janggal justru
 *     akumulasinya melewati qty pesan.
 *
 * Satu aturan tidak cukup: masing-masing kejadian hanya tertangkap oleh satu aturan
 * yang berbeda. Keduanya ada di sini, dan dua-duanya ada di berkas tes.
 *
 * Akar yang sebenarnya lebih sederhana dari kedua aturan itu: form terima tidak
 * pernah menunjukkan AKIBATNYA ke stok. [stokSetelah] dipakai untuk itu, dan
 * ditampilkan SELALU — bukan cuma saat curiga.
 */
sealed interface PeringatanTerima {
    /** Jumlah yang datang berkali-kali lipat stok yang sekarang ada. */
    data class LompatanStok(val rasio: Double, val stokSebelum: Double) : PeringatanTerima

    /** Akumulasi penerimaan sudah melewati jumlah yang dipesan. */
    data class MelebihiPesanan(val kelebihan: Double) : PeringatanTerima
}

object GerbangTerimaPo {

    /**
     * Berapa kali lipat stok berjalan sebelum sebuah penerimaan dianggap melompat.
     *
     * 10x dipilih supaya jauh di atas irama restock normal (gudang menipis lalu
     * diisi 2-5x stok sisa) tapi masih menangkap KULIT 32 yang 38x. Peringatan yang
     * terlalu sering muncul akan berubah jadi klik refleks, dan itu lebih buruk
     * daripada tidak ada peringatan.
     */
    const val AMBANG_LOMPATAN_STOK = 10.0

    /**
     * Toleransi pembulatan float saat membandingkan akumulasi terima dengan qty
     * pesan. 0,001 satuan besar jauh di bawah apa pun yang bisa diketik orang, tapi
     * cukup untuk menyerap 0.1 + 0.2 != 0.3.
     */
    private const val TOLERANSI_FLOAT = 0.001

    /**
     * Peringatan yang perlu dikonfirmasi ulang sebelum penerimaan disimpan.
     * Daftar kosong = jalur mayoritas, form tidak boleh mengganggu.
     *
     * @param stokGudangBesar stok berjalan di Gudang Pusat pada satuan besar.
     *        `null` berarti BELUM DIKETAHUI — aturan lompatan dilewati, bukan
     *        ditebak sebagai nol.
     */
    fun cek(
        qtyDatang: Double,
        qtyPesan: Double,
        qtyTerimaSebelumnya: Double,
        stokGudangBesar: Double?,
    ): List<PeringatanTerima> {
        if (!(qtyDatang > 0.0)) return emptyList()
        val hasil = mutableListOf<PeringatanTerima>()

        if (stokGudangBesar != null && stokGudangBesar.isFinite() && stokGudangBesar > 0.0) {
            val rasio = qtyDatang / stokGudangBesar
            if (rasio > AMBANG_LOMPATAN_STOK) {
                hasil += PeringatanTerima.LompatanStok(rasio, stokGudangBesar)
            }
        }

        val kelebihan = qtyTerimaSebelumnya + qtyDatang - qtyPesan
        if (kelebihan > TOLERANSI_FLOAT) {
            hasil += PeringatanTerima.MelebihiPesanan(kelebihan)
        }
        return hasil
    }

    /** Stok gudang setelah penerimaan ini disimpan. `null` tetap `null`. */
    fun stokSetelah(stokGudangBesar: Double?, qtyDatang: Double): Double? {
        if (stokGudangBesar == null || !stokGudangBesar.isFinite()) return null
        return stokGudangBesar + (if (qtyDatang.isFinite()) qtyDatang else 0.0)
    }

    /** Kalimat siap tampil untuk sebuah peringatan. */
    fun pesan(peringatan: PeringatanTerima, satuan: String): String = when (peringatan) {
        is PeringatanTerima.LompatanStok ->
            "Jumlah ini ${angka(peringatan.rasio, 1)}x stok gudang yang sekarang " +
                "(${angka(peringatan.stokSebelum)} $satuan). " +
                "Pastikan tidak tertukar dengan bahan atau PO lain."

        is PeringatanTerima.MelebihiPesanan ->
            "Melebihi jumlah pesanan sebanyak ${angka(peringatan.kelebihan)} $satuan. " +
                "Pastikan vendor memang mengirim lebih, bukan kiriman yang sama tercatat dua kali."
    }

    private fun angka(nilai: Double, desimal: Int = 2): String {
        if (!nilai.isFinite()) return "0"
        if (abs(nilai % 1.0) < 1e-9) return nilai.toLong().toString()
        return String.format(java.util.Locale.US, "%.${desimal}f", nilai).trimEnd('0').trimEnd('.')
    }
}
