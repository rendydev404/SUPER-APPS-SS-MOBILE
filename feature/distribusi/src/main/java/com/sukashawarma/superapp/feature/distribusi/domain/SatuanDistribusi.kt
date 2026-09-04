package com.sukashawarma.superapp.feature.distribusi.domain

/**
 * Metadata satuan satu bahan baku, sebagaimana dibutuhkan modul Distribusi.
 * Hanya kolom yang benar-benar dipakai konversi dan tampilan yang diambil.
 */
data class BahanBakuMeta(
    val id: String,
    val nama: String,
    val satuan: String,
    val satuanDistribusi: String?,
    val satuanTengah: String?,
    val satuanKecil: String?,
    val faktorTengah: Double?,
    val faktorTampilan: Double?,
    val kategori: String?,
)

/**
 * Jembatan antara satuan dasar (yang disimpan database) dan satuan distribusi
 * (yang dilihat manusia). Cermin `getDistribusiFactor` di `SuratJalanForm.tsx`
 * dan blok `useMemo` di `VerifikasiForm.tsx` — angka yang ditulis native untuk
 * masukan yang sama HARUS identik dengan angka yang ditulis web, karena keduanya
 * mengalir ke `ledger_stok` yang sama.
 */
object SatuanDistribusi {

    /**
     * Hitung faktor konversi satuan dasar → satuan distribusi.
     *
     * Faktor yang bernilai nol atau negatif diperlakukan sebagai "tidak tersetel",
     * serupa dengan truthiness JavaScript di web (`getDistribusiFactor` di
     * `SuratJalanForm.tsx`). Hal ini mencegah pembagian dengan nol di `keDasar()`
     * yang akan menghasilkan `Infinity` atau `NaN` di ledger_stok produksi.
     */
    fun faktor(b: BahanBakuMeta): Double {
        val dist = b.satuanDistribusi ?: return 1.0
        if (dist.equals(b.satuan, ignoreCase = true)) return 1.0

        val tengah = b.faktorTengah?.takeIf { it > 0.0 }
        if (dist.equals(b.satuanTengah, ignoreCase = true) && tengah != null) {
            return tengah
        }
        val kecil = b.faktorTampilan?.takeIf { it > 0.0 }
        if (dist.equals(b.satuanKecil, ignoreCase = true) && kecil != null) {
            return kecil
        }
        // Pemetaan implisit yang ada di web: satuan distribusi "kg" sementara
        // satuan kecilnya "gram". Faktor tampilan dinyatakan dalam gram, jadi
        // harus dibagi seribu dulu untuk mendapatkan faktor per kilogram.
        val implisit = b.faktorTampilan?.takeIf { it > 0.0 }
        if (dist.equals("kg", ignoreCase = true) &&
            b.satuanKecil.equals("gram", ignoreCase = true) &&
            implisit != null
        ) {
            return implisit / 1000.0
        }
        return 1.0
    }

    /**
     * Satuan dasar -> satuan distribusi untuk ditampilkan. Dibulatkan, meniru
     * `Math.round` di web, supaya angka di HP identik dengan angka di dokumen
     * cetak dan di layar web.
     */
    fun keTampilan(qtyDasar: Double, b: BahanBakuMeta): Long = Math.round(qtyDasar * faktor(b))

    /** Satuan distribusi -> satuan dasar untuk ditulis ke database. Tidak
     *  dibulatkan: pembulatan di sini akan menggeser saldo ledger. */
    fun keDasar(qtyTampilan: Double, b: BahanBakuMeta): Double = qtyTampilan / faktor(b)

    /** Label satuan yang ditampilkan di samping angka. */
    fun satuanTampil(b: BahanBakuMeta): String =
        b.satuanDistribusi?.takeIf { it.isNotBlank() } ?: b.satuan
}
