package com.sukashawarma.superapp.feature.stok.domain

import kotlin.math.max

/** Warna badge budget — cermin `BudgetBadgeVariant` web. */
enum class BudgetVarian { TERSEMBUNYI, HIJAU, ORANYE, MERAH }

/**
 * Aturan tampilan budget — port `budgetBadgeVariant` (lib/stok/budget.ts) dan
 * `getPeriodLabel` (BudgetBadge.tsx). Murni visual: web pun tidak pernah memakai
 * ini untuk memblokir pengiriman/persetujuan — keputusan tetap di approver.
 */
object Budget {

    /**
     * `projectedAdd` = estimasi keranjang/permintaan yang belum disetujui, ditambahkan
     * ke `terpakai` untuk proyeksi "kalau ini juga disetujui".
     */
    fun varian(
        hasConfig: Boolean,
        nominal: Double,
        terpakai: Double,
        projectedAdd: Double = 0.0,
    ): BudgetVarian {
        if (!hasConfig) return BudgetVarian.TERSEMBUNYI
        if (nominal <= 0.0) return BudgetVarian.MERAH
        val persen = (terpakai + projectedAdd) / nominal * 100.0
        return when {
            persen > 100.0 -> BudgetVarian.MERAH
            persen >= 80.0 -> BudgetVarian.ORANYE
            else -> BudgetVarian.HIJAU
        }
    }

    fun labelPeriode(periodType: String?, customDays: Int?): String = when (periodType) {
        null -> ""
        "harian" -> "Hari Ini"
        "mingguan" -> "Minggu Ini"
        "bulanan" -> "Bulan Ini"
        "custom" -> if (customDays != null && customDays > 0) "$customDays Hari Ini" else "Periode Ini"
        else -> periodType
    }

    /** Batas nominal top-up yang boleh diajukan — cermin `maxRequest` di RequestTopUpModal. */
    fun maksTopUp(plafon: Double, sisa: Double): Double = max(0.0, plafon - max(0.0, sisa))
}
