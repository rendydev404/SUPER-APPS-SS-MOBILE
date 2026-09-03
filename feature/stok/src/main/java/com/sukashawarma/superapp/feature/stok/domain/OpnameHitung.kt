package com.sukashawarma.superapp.feature.stok.domain

/**
 * Perhitungan angka opname — port dari `calculateTotalFisik` dan
 * `convertBesarToGram` di aplikasi web.
 */
object OpnameHitung {

    /**
     * Total hitungan fisik pada satuan terkecil, dari masukan tiga jenjang.
     *
     * Perhatikan konversi satuan tengah: `faktor_tengah` berarti **berapa satuan
     * tengah dalam satu satuan besar**, sehingga satu satuan tengah bernilai
     * `faktor_tampilan / faktor_tengah` satuan kecil. Dokumen analisis menuliskannya
     * sebagai `qty_tengah * faktor_tengah`, dan itu keliru — untuk bahan seperti ES
     * BATU (1 Bal = 10 Kg = 10.000 Gr) rumus dokumen menghasilkan 10 gram per kilo,
     * bukan 1.000.
     */
    fun totalFisikSmallest(
        besar: Double,
        tengah: Double,
        kecil: Double,
        meta: UnitMeta,
    ): Double {
        val besarDalamKecil = meta.faktorTampilan?.takeIf { it > 0.0 } ?: 1.0
        val tengahDalamKecil = meta.faktorTengah?.takeIf { it > 0.0 }
            ?.let { besarDalamKecil / it } ?: 1.0

        return when {
            meta.satuanTengah != null && meta.satuanKecil != null ->
                besar * besarDalamKecil + tengah * tengahDalamKecil + kecil
            meta.satuanKecil != null -> besar * besarDalamKecil + kecil
            else -> besar
        }
    }

    /**
     * Saldo sistem pada satuan terkecil.
     *
     * Baris yang sudah small-scale dipakai apa adanya; baris legacy dikalikan
     * `faktor_tampilan`, tetapi hanya bila bahan itu memang punya satuan kecil —
     * kalau tidak, angkanya dibiarkan karena satuan besar sudah satuan terkecilnya.
     */
    fun saldoSistemSmallest(saldo: Double, saldoIsGram: Boolean, meta: UnitMeta): Double {
        if (saldoIsGram) return saldo
        val faktor = meta.faktorTampilan?.takeIf { it > 0.0 }
        return if (meta.satuanKecil != null && faktor != null) saldo * faktor else saldo
    }
}
