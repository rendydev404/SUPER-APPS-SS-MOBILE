package com.sukashawarma.superapp.feature.stok.domain

import com.sukashawarma.superapp.domain.model.Role
import kotlin.math.max

/** Tahap pengajuan top-up saldo — cermin kolom `status` di `outlet_budget_topup_requests`. */
enum class StatusTopUp(val nilai: String, val label: String) {
    MENUNGGU_AM("pending_am", "Menunggu AM"),
    MENUNGGU_FINANCE("pending_finance", "Menunggu Finance"),
    DISETUJUI("approved", "Disetujui"),
    DITOLAK("rejected", "Ditolak");

    companion object {
        fun dari(nilai: String?): StatusTopUp =
            entries.firstOrNull { it.nilai == nilai } ?: MENUNGGU_AM
    }
}

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

    // ------------------------------------------------------------------ top-up
    //
    // Matriks role menyalin `OutletTopUpRequests.tsx`. Ini hanya untuk menyembunyikan
    // tombol; keputusan sebenarnya dijaga `approve_budget_topup_scoped` di database.

    private val TOPUP_AM = setOf(Role.ADMIN, Role.OWNER, Role.DEVELOPER)
    private val TOPUP_FINANCE = setOf(Role.ADMIN_FINANCE, Role.OWNER, Role.DEVELOPER)

    fun bolehApproveAm(role: Role?): Boolean = role != null && role in TOPUP_AM
    fun bolehApproveFinance(role: Role?): Boolean = role != null && role in TOPUP_FINANCE

    /** Penolakan boleh dilakukan siapa pun yang berwenang pada salah satu tahap. */
    fun bolehTolakTopUp(role: Role?): Boolean = bolehApproveAm(role) || bolehApproveFinance(role)

    /**
     * Batas nominal top-up — cermin `maxRequest` di RequestTopUpModal
     * (`plafon - max(0, sisa)`). Database mengulang pemeriksaan yang sama.
     */
    fun maksTopUp(plafon: Double, sisa: Double): Double = max(0.0, plafon - max(0.0, sisa))
}
