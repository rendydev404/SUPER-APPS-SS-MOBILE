package com.sukashawarma.superapp.feature.stok.domain

import kotlin.math.floor

/**
 * Metadata satuan satu bahan, disalin apa adanya dari `bahan_baku`.
 *
 * `faktorTampilan` = berapa satuan terkecil dalam satu satuan besar.
 * `faktorTengah`   = berapa satuan terkecil dalam satu satuan tengah.
 */
data class UnitMeta(
    val satuan: String? = null,
    val satuanTengah: String? = null,
    val satuanKecil: String? = null,
    val faktorTengah: Double? = null,
    val faktorTampilan: Double? = null,
)

enum class StokStatus { OK, WARNING, BELOW, UNKNOWN }

/**
 * Normalisasi satuan campuran.
 *
 * `stok_balance.saldo` bisa berada di satuan besar (baris legacy) atau di satuan
 * terkecil (setelah opname modern) — `saldo_is_gram` yang menandainya. Threshold
 * selalu disimpan pada satuan besar. Membandingkan keduanya mentah-mentah, seperti
 * yang dilakukan view monitoring, membuat status meleset sebesar faktor_tampilan
 * untuk bahan yang faktornya bukan 1.
 *
 * Semua fungsi di sini mengembalikan `null` bila faktor tidak dapat dipakai
 * (null, nol, atau negatif). Sengaja TIDAK jatuh ke asumsi 1: menganggap faktor
 * kosong sama dengan 1 akan menghasilkan angka yang kelihatan wajar padahal salah
 * ribuan kali lipat, dan itu tidak akan pernah ketahuan di layar.
 */
object UnitScale {

    /** Dipakai bila bahan tidak punya threshold sendiri — cermin fallback web. */
    const val DEFAULT_THRESHOLD = 10.0

    /** Batas porsi minimum sebelum bahan dianggap kritis, bila outlet tidak set. */
    const val DEFAULT_MARQUEE_WARNING = 7

    /** Faktor valid, atau null bila tidak dapat dipercaya. */
    private fun faktor(value: Double?): Double? =
        value?.takeIf { it.isFinite() && it > 0.0 }

    /** Satuan besar -> satuan terkecil. */
    fun smallestFromBesar(qtyBesar: Double, meta: UnitMeta): Double? =
        faktor(meta.faktorTampilan)?.let { qtyBesar * it }

    /** Satuan terkecil -> satuan besar. */
    fun besarFromSmallest(qtySmallest: Double, meta: UnitMeta): Double? =
        faktor(meta.faktorTampilan)?.let { qtySmallest / it }

    /** Satuan tengah -> satuan terkecil. */
    fun smallestFromTengah(qtyTengah: Double, meta: UnitMeta): Double? =
        faktor(meta.faktorTengah)?.let { qtyTengah * it }

    /**
     * Saldo apa adanya dari DB -> satuan terkecil, memakai penanda skala barisnya.
     * Bila baris sudah small-scale, nilainya dipakai langsung tanpa dikali apa pun.
     */
    fun normalizeSaldo(saldo: Double, saldoIsGram: Boolean, meta: UnitMeta): Double? =
        if (saldoIsGram) saldo else smallestFromBesar(saldo, meta)

    /** Threshold selalu tersimpan pada satuan besar, jadi selalu perlu dikonversi. */
    fun normalizeThreshold(threshold: Double?, meta: UnitMeta): Double? =
        smallestFromBesar(threshold ?: DEFAULT_THRESHOLD, meta)

    /**
     * Kebutuhan satu porsi resep, dinormalisasi ke satuan terkecil.
     *
     * Aturannya mengikuti `estimasi_produksi.ts` di aplikasi web: bila `resep_item.satuan`
     * berbeda dari satuan besar bahan, angka resep dianggap sudah berada pada satuan
     * terkecil; bila sama (atau salah satunya kosong), angka itu berada pada satuan besar
     * dan perlu dikalikan faktor.
     *
     * Faktor yang dipakai adalah `faktor_konversi` — faktor khusus resep/BOM — bukan
     * `faktor_tampilan` yang dipakai saldo dan opname. Keduanya sering bernilai sama,
     * tetapi menyamakannya begitu saja akan meleset pada bahan yang faktornya berbeda.
     */
    fun kebutuhanResepKeSmallest(
        qtyPerPorsi: Double,
        satuanResep: String?,
        satuanBesarBahan: String?,
        faktorKonversi: Double?,
    ): Double? {
        if (!qtyPerPorsi.isFinite() || qtyPerPorsi <= 0.0) return null
        val sudahSatuanKecil = satuanResep != null &&
            satuanBesarBahan != null &&
            !satuanResep.equals(satuanBesarBahan, ignoreCase = true)
        if (sudahSatuanKecil) return qtyPerPorsi
        return faktor(faktorKonversi)?.let { qtyPerPorsi * it }
    }

    /**
     * Status stok setelah kedua sisi berada pada satuan yang sama.
     *
     * `porsiTersisa` adalah hasil estimasi produksi bila tersedia; bahan yang
     * porsinya menipis dianggap kritis walaupun angka saldonya masih di atas
     * setengah threshold.
     */
    fun status(
        saldoNorm: Double?,
        thresholdNorm: Double?,
        porsiTersisa: Int? = null,
        marqueeWarning: Int = DEFAULT_MARQUEE_WARNING,
    ): StokStatus {
        if (saldoNorm == null || thresholdNorm == null) return StokStatus.UNKNOWN
        val porsiKritis = porsiTersisa != null && porsiTersisa < marqueeWarning
        return when {
            saldoNorm < thresholdNorm / 2.0 || porsiKritis -> StokStatus.BELOW
            saldoNorm < thresholdNorm -> StokStatus.WARNING
            else -> StokStatus.OK
        }
    }

    /**
     * Pecah jumlah satuan terkecil menjadi tampilan berjenjang, mis. "2 dus 3 pak 40 gr".
     * Mengembalikan null bila faktor tidak memadai, supaya pemanggil menampilkan
     * angka mentah dan bukan tebakan.
     */
    fun formatBerjenjang(qtySmallest: Double, meta: UnitMeta): String? {
        val fBesar = faktor(meta.faktorTampilan) ?: return null
        val bagian = mutableListOf<String>()
        var sisa = qtySmallest

        val besar = floor(sisa / fBesar)
        if (besar > 0) {
            bagian += "${besar.toLongString()} ${meta.satuan ?: "besar"}"
            sisa -= besar * fBesar
        }
        val fTengah = faktor(meta.faktorTengah)
        if (fTengah != null && fTengah < fBesar) {
            val tengah = floor(sisa / fTengah)
            if (tengah > 0) {
                bagian += "${tengah.toLongString()} ${meta.satuanTengah ?: "tengah"}"
                sisa -= tengah * fTengah
            }
        }
        if (sisa > 0.0001 || bagian.isEmpty()) {
            bagian += "${sisa.toLongString()} ${meta.satuanKecil ?: "kecil"}"
        }
        return bagian.joinToString(" ")
    }
}

/** Angka bulat tanpa ekor desimal palsu; desimal disimpan maksimal dua digit. */
internal fun Double.toLongString(): String =
    if (this % 1.0 == 0.0) toLong().toString()
    else String.format(java.util.Locale.US, "%.2f", this).trimEnd('0').trimEnd('.')
