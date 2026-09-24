package com.sukashawarma.superapp.feature.distribusi.domain

import kotlin.math.abs
import kotlin.math.floor

/**
 * Saldo satu vendor untuk satu bahan di Gudang Pusat, dari RPC
 * `saldo_vendor_gudang`. [sisa] dalam satuan yang disepakati pemanggil —
 * RPC mengirim satuan BESAR, form mengubahnya ke satuan distribusi dulu.
 */
data class SaldoVendor(
    val vendorId: String,
    val vendorNama: String,
    val sisa: Double,
    /** false = saldo vendor ini belum dihitung, jadi sisanya tidak dijaga. */
    val aktif: Boolean,
    /** Bahan ditandai multi-vendor walau baris saldonya baru satu. */
    val multi: Boolean = false,
)

data class Alokasi(val vendorId: String, val qty: Double)

/**
 * Cermin `lib/alokasiVendor.ts` web (salinan identik juga ada di app stok web).
 * Penjaga sebenarnya ada di DB (`sj_vendor_on_dikirim`); ini hanya untuk UI.
 */
object AlokasiVendor {

    private const val EPS = 1e-6

    /** Satu vendor -> langsung dia. Selain itu vendor aktif dengan sisa terbesar
     *  yang cukup menampung seluruh qty; tidak ada -> kosong, pengguna memilih. */
    fun alokasiAwal(qty: Double, vendors: List<SaldoVendor>): List<Alokasi> {
        if (vendors.size == 1) return listOf(Alokasi(vendors[0].vendorId, qty))
        val cukup = vendors
            .filter { it.aktif && it.sisa + EPS >= qty }
            .maxByOrNull { it.sisa }
        return if (cukup != null) listOf(Alokasi(cukup.vendorId, qty)) else emptyList()
    }

    fun validasi(qty: Double, alokasi: List<Alokasi>, vendors: List<SaldoVendor>): String? {
        if (vendors.size <= 1) return null
        if (alokasi.isEmpty()) return "Pilih vendor dulu"
        val dipakai = HashSet<String>()
        var total = 0.0
        for (a in alokasi) {
            val v = vendors.find { it.vendorId == a.vendorId } ?: return "Vendor tidak dikenal"
            if (!dipakai.add(a.vendorId)) return "${v.vendorNama} dipilih dua kali"
            if (!(a.qty > 0.0)) return "Jumlah ${v.vendorNama} harus lebih dari 0"
            if (v.aktif && a.qty > v.sisa + EPS) return "Sisa ${v.vendorNama} tinggal ${angka(v.sisa)}"
            total += a.qty
        }
        if (abs(total - qty) > EPS) {
            return "Jumlah per vendor (${angka(total)}) belum sama dengan ${angka(qty)}"
        }
        return null
    }

    /**
     * Saldo RPC (satuan besar) -> satuan distribusi, dibulatkan ke bawah tiga
     * desimal seperti `Math.floor(x * 1000) / 1000` di form web — dengan toleransi
     * kecil: tanpa itu 0.29 × 100 = 28.999999… jatuh ke 28.999 dan qty 29 yang sah
     * ditolak "Sisa … tinggal 28.999" (bug yang sama masih ada di web).
     */
    fun keSatuanDistribusi(v: SaldoVendor, faktor: Double): SaldoVendor =
        v.copy(sisa = floor(v.sisa * faktor * 1000.0 + 1e-6) / 1000.0)

    /** Bahan dianggap multi-vendor bila saldonya ≥ 2 baris atau ditandai `multi`
     *  — cermin `isMulti` di `SuratJalanDetail.tsx`. */
    fun multiVendor(vendors: List<SaldoVendor>): Boolean =
        vendors.size >= 2 || vendors.any { it.multi }

    /** Angka tanpa ekor ".0" — mirip cara JavaScript menampilkan number. */
    fun angka(x: Double): String =
        if (x == floor(x) && abs(x) < 1e15) x.toLong().toString() else x.toString()
}
