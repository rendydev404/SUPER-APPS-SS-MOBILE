package com.sukashawarma.superapp.feature.stok.domain

import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.math.sign
import kotlin.math.truncate

/** Saldo yang sudah dipecah menjadi tiga jenjang satuan, siap ditampilkan. */
data class TriUnit(val besar: Double, val tengah: Double, val kecil: Double)

/**
 * Pemecahan saldo ke satuan besar/tengah/kecil.
 *
 * Ini port langsung dari `decomposeTriUnitRaw` di `apps/stok/src/lib/format/compositeUnit.ts`,
 * termasuk perilakunya yang tidak simetris antara cabang `saldoIsGram` true dan false.
 * Disalin apa adanya dengan sengaja: angka di kartu harus sama persis dengan yang dilihat
 * orang di aplikasi web, dan "merapikan" logika ini berarti menciptakan angka kedua yang
 * berbeda untuk saldo yang sama.
 *
 * Catatan penting soal faktor: di sini `faktorTengah` berarti **berapa satuan tengah dalam
 * satu satuan besar** (bukan berapa satuan kecil per satuan tengah), karena web menghitung
 * `kecilPerTengah = faktorTampilan / faktorTengah`.
 */
fun decomposeTriUnit(
    qty: Double,
    saldoIsGram: Boolean,
    satuanTengah: String?,
    faktorTengah: Double?,
    satuanKecil: String?,
    faktorTampilan: Double?,
): TriUnit {
    var besar = qty
    var tengah = 0.0
    var kecil = 0.0

    val adaTengah = satuanTengah != null && faktorTengah != null && faktorTengah > 0.0
    val adaKecil = satuanKecil != null && faktorTampilan != null && faktorTampilan > 0.0

    if (saldoIsGram) {
        val sisaAwal = abs(qty)
        val tanda = if (qty < 0) -1.0 else 1.0

        if (adaTengah && adaKecil) {
            val kecilPerBesar = faktorTampilan!!
            val kecilPerTengah = faktorTampilan / faktorTengah!!
            val b = truncate(sisaAwal / kecilPerBesar)
            val sisa = sisaAwal - b * kecilPerBesar
            val t = truncate(sisa / kecilPerTengah)
            besar = b * tanda
            tengah = t
            kecil = bulatkan2(sisa - t * kecilPerTengah)
        } else if (adaKecil) {
            val b = truncate(sisaAwal / faktorTampilan!!)
            besar = b * tanda
            kecil = bulatkan2(sisaAwal - b * faktorTampilan)
        }
        return TriUnit(besar, tengah, kecil)
    }

    // saldoIsGram=false: qty dianggap berada pada satuan besar.
    if (adaTengah) {
        var utuh = truncate(qty)
        val sisaBesar = qty - utuh
        besar = utuh

        if (adaKecil) {
            val totalKecil = bulatkan2(sisaBesar * faktorTampilan!!)
            val kecilPerTengah = faktorTampilan / faktorTengah!!
            var totalTengah = truncate(totalKecil / kecilPerTengah)
            var sisaKecil = totalKecil - totalTengah * kecilPerTengah

            if (abs(sisaKecil) >= kecilPerTengah) {
                totalTengah += sign(sisaKecil)
                sisaKecil = 0.0
            }
            if (abs(totalTengah) >= faktorTengah) {
                utuh += sign(totalTengah)
                besar = utuh
                totalTengah = 0.0
            }
            tengah = abs(totalTengah)
            kecil = abs(bulatkan2(sisaKecil))
        } else {
            var sisaTengah = bulatkan2(sisaBesar * faktorTengah!!)
            if (abs(sisaTengah) >= faktorTengah) {
                besar = utuh + sign(sisaTengah)
                sisaTengah = 0.0
            }
            tengah = abs(sisaTengah)
        }
    } else if (adaKecil) {
        var utuh = truncate(qty)
        var sisa = bulatkan2((qty - utuh) * faktorTampilan!!)
        if (abs(sisa) >= faktorTampilan) {
            utuh += sign(sisa)
            sisa = 0.0
        }
        besar = utuh
        kecil = abs(sisa)
    }

    return TriUnit(besar, tengah, kecil)
}

private fun bulatkan2(nilai: Double): Double = (nilai * 100).roundToLong() / 100.0

/** Angka tanpa ekor desimal palsu; satu desimal saja bila memang pecahan — cermin `formatNum` web. */
fun formatAngkaStok(nilai: Double): String {
    if (!nilai.isFinite()) return "0"
    if (nilai % 1.0 == 0.0) return nilai.toLong().toString()
    return String.format(java.util.Locale.US, "%.1f", nilai)
}

/** Penyeragaman label satuan — cermin `formatUnit` di `CrewList.tsx`. */
fun formatSatuan(satuan: String?): String {
    val u = satuan?.trim().orEmpty()
    if (u.isEmpty()) return ""
    return when (u.lowercase()) {
        "gram", "gr" -> "Gr"
        "kg", "kilogram" -> "Kg"
        "lembar", "lbr" -> "Lbr"
        "bungkus", "bks" -> "Bks"
        "kompan", "jerigen" -> "Kompan"
        "tabung" -> "Tabung"
        "bal" -> "Bal"
        "dus" -> "Dus"
        "pack", "pck" -> "Pack"
        "roll" -> "Roll"
        "pcs", "biji" -> "Pcs"
        else -> u.replaceFirstChar { it.uppercase() }.let { it.first() + it.drop(1).lowercase() }
    }
}
