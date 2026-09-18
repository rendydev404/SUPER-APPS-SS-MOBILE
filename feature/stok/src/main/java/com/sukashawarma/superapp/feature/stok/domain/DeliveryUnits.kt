package com.sukashawarma.superapp.feature.stok.domain

import java.util.Locale
import kotlin.math.round

/**
 * Satuan khusus pengiriman dan surat jalan — cermin `DELIVERY_UNITS` di web (`LedgerList.tsx`).
 *
 * Mengonversi nilai satuan besar ke satuan distribusi/kemasan riil saat barang
 * dikirim atau diterima lewat surat jalan, agar kru melihat "12 kg Mayonaise"
 * atau "24 pack Keju", bukan angka mentah internal gudang.
 */
object DeliveryUnits {

    data class Rule(val label: String, val factorFromLarge: Double)

    private val MAPPING = mapOf(
        "SAOS CABE" to Rule("kg", 16.5),
        "SAOS TOMAT" to Rule("kg", 16.5),
        "SAOS SAMYANG" to Rule("kg", 20.0),
        "MAYONAISE" to Rule("kg", 12.0),
        "MAYONES" to Rule("kg", 12.0),
        "KULIT 25" to Rule("pack", 1.0),
        "KULIT 28" to Rule("pack", 1.0),
        "KULIT 32" to Rule("pack", 1.0),
        "AYAM" to Rule("kg", 1.0),
        "SAPI" to Rule("pcs", 1.0),
        "KENTANG" to Rule("kg", 4.0),
        "KEJU" to Rule("pack", 24.0),
        "TUM" to Rule("kg", 1.0),
        "BAWANG" to Rule("kg", 1.0),
        "TEPUNG" to Rule("kg", 1.0),
        "MINYAK SAYUR" to Rule("kompan", 1.0),
        "MINYAK" to Rule("kompan", 1.0),
        "FOIL" to Rule("roll", 24.0),
        "FOIL (48)" to Rule("roll", 48.0),
        "SARUNG TANGAN BENING" to Rule("pack", 1.0),
        "HAND GLOVE" to Rule("pack", 1.0),
        "KERTAS STRUK" to Rule("roll", 1.0),
        "THERMAL STRUK" to Rule("roll", 1.0),
        "PLASTIK BENING" to Rule("pack", 5.0),
        "PLASTIK BESAR" to Rule("pack", 5.0),
        "PLASTIK KECIL" to Rule("pack", 5.0),
        "POLYBAG" to Rule("pack", 5.0),
        "PLASTIK MERAH" to Rule("pack", 5.0),
        "PAPER WRAP" to Rule("pack", 1.0),
        "POWDER TEH" to Rule("kg", 1.0),
        "POWDER JERUK" to Rule("kg", 1.0),
        "CUP" to Rule("pcs", 1.0),
        "TUTUP" to Rule("pcs", 1.0),
        "SEDOTAN" to Rule("pack", 1.0),
        "STIKER" to Rule("lembar", 100.0),
        "MIE" to Rule("bungkus", 40.0),
        "SAYUR" to Rule("kg", 1.0),
        "ES BATU CRYSTAL" to Rule("bal", 1.0),
        "ES BATU" to Rule("bal", 1.0)
    )

    /**
     * Konversi angka ke satuan pengiriman bila nama bahan terdaftar.
     * Mengembalikan null bila tidak terdaftar agar pemanggil dapat fallback ke UnitScale.
     */
    fun format(qty: Double, bahanName: String?, isSaldo: Boolean = false): String? {
        if (bahanName.isNullOrBlank()) return null
        val rule = MAPPING[bahanName.trim().uppercase(Locale.ROOT)] ?: return null
        val converted = round(qty * rule.factorFromLarge * 100.0) / 100.0
        val sign = if (!isSaldo && converted > 0) "+" else ""
        val formattedNum = if (converted % 1.0 == 0.0) {
            converted.toLong().toString()
        } else {
            String.format(Locale.US, "%.2f", converted).trimEnd('0').trimEnd('.')
        }
        return "$sign$formattedNum ${rule.label}"
    }

    /**
     * Cek apakah suatu bahan memiliki mapping satuan pengiriman.
     */
    fun hasMapping(bahanName: String?): Boolean {
        if (bahanName.isNullOrBlank()) return false
        return MAPPING.containsKey(bahanName.trim().uppercase(Locale.ROOT))
    }
}
