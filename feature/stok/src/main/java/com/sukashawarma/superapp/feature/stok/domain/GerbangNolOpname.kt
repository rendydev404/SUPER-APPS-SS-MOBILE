package com.sukashawarma.superapp.feature.stok.domain

/**
 * Gerbang konfirmasi sebelum opname difinalisasi — port
 * `apps/stok/src/lib/stok/zeroGuard.ts` beserta `hitungPenurunanDrastis()` di
 * `components/stok/OpnameForm.tsx`.
 *
 * Definisi yang berlaku (keputusan owner, 8 September 2026):
 *   - Bahan DIKOSONGKAN = belum dihitung -> dilewati, saldo sistem tidak berubah.
 *   - Bahan DIISI 0     = sudah dihitung, fisiknya habis -> saldo dijadikan nol.
 *
 * Masalahnya kru memakai "0" untuk keduanya. Pada 5-7 September 2026 tiga outlet
 * mengetik "0 Roll" untuk FOIL padahal barangnya masih ada; Pajajaran kehilangan
 * 75 Roll (~Rp660 ribu) dalam satu kali finalisasi, tanpa peringatan apa pun.
 *
 * Modul ini sengaja tidak tahu apa-apa soal Compose maupun Supabase supaya bisa
 * diuji sebagai aritmetika biasa.
 */

/** Satu baris opname yang hendak dinilai oleh gerbang. */
data class CalonPenurunan(
    val bahanBakuId: String,
    val nama: String,
    /** Hitungan kru, pada satuan terkecil. */
    val qtyFisik: Double,
    /** Saldo sistem, pada satuan terkecil — sudah sadar skala. */
    val qtySystem: Double,
    /**
     * `bahan_baku.faktor_konversi`: satuan terkecil per satu satuan yang berarti
     * (1 Roll = 760 cm, 1 Kg = 1000 gram). SENGAJA kolom ini, bukan
     * `faktor_tampilan` maupun `faktor_tengah` — web membaca `faktor_konversi`
     * di `OpnameForm.tsx`, dan ketiganya bisa berbeda untuk bahan yang sama.
     */
    val faktorKonversi: Double?,
    /** Hasil [Selisih.perluDitandai] untuk baris ini. */
    val ditandai: Boolean,
)

/**
 * Satu baris yang perlu dikonfirmasi ulang sebelum finalisasi.
 *
 * [habisTotal] menaikkannya ke urutan teratas; [bolehLewati] menentukan apakah
 * layar boleh menawarkan jalan keluar "kembalikan ke belum dihitung".
 */
data class Penurunan(
    val calon: CalonPenurunan,
    val habisTotal: Boolean,
    val bolehLewati: Boolean,
)

object GerbangNolOpname {

    /**
     * Ambang "stok berarti": satu satuan menengah. Faktor yang tidak dapat dipercaya
     * (null, nol, negatif) jatuh ke 1 satuan — bukan diabaikan, karena tanpa ambang
     * apa pun setiap saldo sekecil apa pun akan memicu konfirmasi.
     */
    fun ambangStokBerarti(faktorKonversi: Double?): Double =
        if (faktorKonversi != null && faktorKonversi > 0.0) faktorKonversi else 1.0

    /**
     * Kru menandai bahan habis (fisik 0) padahal sistem masih mencatat stok minimal
     * satu satuan menengah.
     *
     * Sisa recehan di bawah ambang (0,2 tabung gas, 0,1 galon) sengaja dilewatkan
     * supaya konfirmasinya jarang muncul dan tidak berubah jadi klik refleks.
     *
     * Saldo minus tidak pernah ditandai: mengisi 0 pada baris minus justru
     * mengembalikannya ke nol, dan itu memang perbaikan.
     */
    fun nolMencurigakan(qtyFisik: Double, qtySystem: Double, faktorKonversi: Double?): Boolean {
        if (qtyFisik != 0.0) return false
        return qtySystem >= ambangStokBerarti(faktorKonversi)
    }

    /**
     * Baris yang hitungan fisiknya turun drastis dari catatan sistem.
     *
     * Hanya PENURUNAN yang diperingatkan, bukan kenaikan: stok yang naik memang
     * janggal juga, tapi tidak menghapus apa pun — dan memperingatkan segalanya
     * membuat orang berhenti membaca peringatan.
     *
     * Yang habis total ditaruh di atas. Pengurutannya stabil, jadi dalam kelompok
     * yang sama urutan aslinya bertahan.
     */
    fun daftarPenurunan(calon: List<CalonPenurunan>): List<Penurunan> =
        calon
            .filter { it.ditandai && it.qtyFisik < it.qtySystem }
            .map {
                Penurunan(
                    calon = it,
                    habisTotal = it.qtyFisik == 0.0 && it.qtySystem > 0.0,
                    bolehLewati = nolMencurigakan(it.qtyFisik, it.qtySystem, it.faktorKonversi),
                )
            }
            .sortedByDescending { it.habisTotal }
}
