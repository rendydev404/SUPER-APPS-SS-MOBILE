package com.sukashawarma.superapp.feature.stok.domain

/**
 * Normalisasi skala saldo untuk modul stok.
 *
 * Catatan sejarah (2026-09-04): di sini dulu ada konversi satuan distribusi
 * (`faktor`/`keDistribusi`/`keBase`, port `getDistribusiFactor` web). Konversi itu
 * dibuang setelah diputuskan native memesan dalam SATUAN BESAR — lihat
 * `BahanBaku.satuanPesan`. Yang tersisa hanya normalisasi saldo, yang memang tidak
 * ada hubungannya dengan satuan pesan.
 */
object DistribusiUnit {

    /**
     * Saldo gram-scale -> satuan besar — cermin `convertGramToBesar` web. Tanpa
     * satuan kecil, gram dan besar adalah satuan yang sama sehingga tidak dikonversi.
     */
    fun gramKeBesar(qtyGram: Double, satuanKecil: String?, faktorTampilan: Double?): Double =
        if (satuanKecil != null && (faktorTampilan ?: 0.0) > 0.0) qtyGram / faktorTampilan!!
        else qtyGram

    /** Saldo apa adanya dari DB -> satuan besar, memakai penanda skala barisnya. */
    fun saldoKeBesar(saldo: Double, saldoIsGram: Boolean, meta: UnitMeta): Double =
        if (saldoIsGram) gramKeBesar(saldo, meta.satuanKecil, meta.faktorTampilan) else saldo
}

/**
 * Saldo siap tampil berjenjang, mis. "2 Dus 3 Pack 40 Gr" — padanan
 * `formatTriUnitSaldoAdaptive` web di atas [decomposeTriUnit] yang sudah ada.
 *
 * Dipakai juga untuk menampilkan qty permintaan yang TERSIMPAN: nilainya selalu pada
 * satuan besar dan bisa pecahan (permintaan dari web tersimpan mis. 0,2083 Dus untuk
 * 5 Pack). Ditampilkan berjenjang, angka itu terbaca "5 Pack" apa adanya alih-alih
 * dibulatkan menjadi "1 Dus" yang menyesatkan.
 */
fun formatTriUnitAdaptif(qty: Double, saldoIsGram: Boolean, meta: UnitMeta): String {
    val tri = decomposeTriUnit(
        qty = qty,
        saldoIsGram = saldoIsGram,
        satuanTengah = meta.satuanTengah,
        faktorTengah = meta.faktorTengah,
        satuanKecil = meta.satuanKecil,
        faktorTampilan = meta.faktorTampilan,
    )
    val bagian = mutableListOf<String>()
    if (tri.besar != 0.0 || (tri.tengah == 0.0 && tri.kecil == 0.0)) {
        bagian += "${formatAngkaStok(tri.besar)} ${formatSatuan(meta.satuan)}".trim()
    }
    if (tri.tengah != 0.0) bagian += "${formatAngkaStok(tri.tengah)} ${formatSatuan(meta.satuanTengah)}".trim()
    if (tri.kecil != 0.0) bagian += "${formatAngkaStok(tri.kecil)} ${formatSatuan(meta.satuanKecil)}".trim()
    return bagian.joinToString(" ")
}
