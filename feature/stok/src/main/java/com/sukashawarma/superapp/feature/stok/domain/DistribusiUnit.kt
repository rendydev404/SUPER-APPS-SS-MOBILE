package com.sukashawarma.superapp.feature.stok.domain

/**
 * Konversi satuan distribusi — port `getDistribusiFactor`, `convertToDistribusiUnit`,
 * dan `convertToBaseUnit` dari `apps/stok/src/lib/format/compositeUnit.ts`.
 *
 * `permintaan_bahan_item.qty_diminta`/`qty_disetujui` SELALU tersimpan pada satuan
 * besar (base), tetapi orang lapangan memesan dalam satuan distribusi (mis. "kg"
 * padahal satuan besarnya "Bal"). Faktor di sini = berapa satuan distribusi dalam
 * satu satuan besar.
 *
 * Perhatikan makna `faktorTengah` mengikuti web: berapa satuan TENGAH dalam satu
 * satuan besar (bukan satuan kecil per tengah) — lihat catatan di [decomposeTriUnit].
 */
object DistribusiUnit {

    fun faktor(
        satuan: String?,
        satuanTengah: String?,
        faktorTengah: Double?,
        satuanKecil: String?,
        faktorTampilan: Double?,
        satuanDistribusi: String?,
    ): Double {
        val dist = satuanDistribusi?.trim()?.lowercase().orEmpty()
        if (dist.isEmpty() || dist == satuan?.trim()?.lowercase()) return 1.0
        if (dist == satuanTengah?.trim()?.lowercase() && (faktorTengah ?: 0.0) > 0.0) {
            return faktorTengah!!
        }
        if (dist == satuanKecil?.trim()?.lowercase() && (faktorTampilan ?: 0.0) > 0.0) {
            return faktorTampilan!!
        }
        // Pemetaan implisit web: distribusi "kg" atas satuan kecil "gram".
        if (dist == "kg" && satuanKecil?.trim()?.lowercase() == "gram" && (faktorTampilan ?: 0.0) > 0.0) {
            return faktorTampilan!! / 1000.0
        }
        return 1.0
    }

    /** Satuan besar (base) -> satuan distribusi. */
    fun keDistribusi(qtyBase: Double, faktor: Double): Double = qtyBase * faktor

    /** Satuan distribusi -> satuan besar (base) — dipakai saat mengirim ke database. */
    fun keBase(qtyDistribusi: Double, faktor: Double): Double =
        if (faktor > 0.0) qtyDistribusi / faktor else qtyDistribusi

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
