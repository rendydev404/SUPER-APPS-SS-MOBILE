package com.sukashawarma.superapp.feature.home.domain

import com.google.gson.JsonObject
import com.sukashawarma.superapp.data.remote.optDouble
import com.sukashawarma.superapp.data.remote.optInt
import com.sukashawarma.superapp.data.remote.optString
import com.sukashawarma.superapp.domain.model.Role
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToLong

/**
 * Skema insentif bulanan berbasis porsi terjual — cermin migrasi web
 * `20260826153000_new_pcs_based_bonus_system.sql`. Tiap skema punya RPC-nya
 * sendiri dan rumusnya dihitung DI SERVER; sisi native hanya memilih RPC mana
 * yang dipanggil dan baris mana yang milik pengguna.
 *
 * - Crew & leader: `get_monthly_crew_bonus` — (pcs outlet × Rp 100) ÷ kru aktif eligible.
 * - Area manager: `get_monthly_am_bonus` — pcs seluruh cabang binaan × Rp 50.
 * - Regional manager: `get_monthly_rm_bonus` — pcs semua cabang operasional × Rp 50.
 *
 * Role lain tidak punya skema, jadi kartunya tidak tampil sama sekali — bukan
 * tampil dengan angka nol, karena nol berarti "belum ada penjualan".
 */
enum class SkemaBonus(val rpc: String) {
    KRU("get_monthly_crew_bonus"),
    AREA_MANAGER("get_monthly_am_bonus"),
    REGIONAL_MANAGER("get_monthly_rm_bonus"),
}

fun skemaBonusUntuk(role: Role?): SkemaBonus? = when (role) {
    Role.CREW, Role.LEADER -> SkemaBonus.KRU
    Role.AREA_MANAGER -> SkemaBonus.AREA_MANAGER
    Role.REGIONAL_MANAGER -> SkemaBonus.REGIONAL_MANAGER
    else -> null
}

/** Isi kartu bonus di beranda, sudah siap tampil. */
data class BonusBulanan(
    val bulan: Int,
    val tahun: Int,
    /**
     * `null` = RPC tidak mengembalikan baris untuk pengguna ini: statusnya bukan
     * `active`, atau `outlet_staff.is_bonus_eligible` dimatikan admin. Berbeda
     * dari nol rupiah, yang berarti terdaftar tapi belum ada penjualan.
     */
    val nominal: Long?,
    val totalPcs: Long = 0,
    val tarif: Long = 0,
    /** Jumlah kru pembagi (skema KRU); 1 untuk AM/RM yang bonusnya tidak dibagi. */
    val pembagi: Int = 1,
    /** Cakupan yang dihitung: nama outlet, "6 cabang binaan", atau "Semua cabang operasional". */
    val cakupan: String = "",
) {
    val terdaftar: Boolean get() = nominal != null

    val labelBulan: String
        get() = Month.of(bulan).getDisplayName(TextStyle.FULL, LOKAL_ID) + " " + tahun

    /** Baris rumus di bawah nominal, mis. `3.200 pcs × Rp 100 ÷ 4 kru`. */
    val rumus: String
        get() {
            val dasar = "${ribuan(totalPcs)} pcs × ${rupiah(tarif)}"
            return if (pembagi > 1) "$dasar ÷ $pembagi kru" else dasar
        }
}

/**
 * Memilih baris milik pengguna dari hasil RPC dan menyusunnya menjadi [BonusBulanan].
 *
 * RPC bonus mengembalikan SEMUA staf dalam skema itu (desain web), jadi baris
 * milik sendiri dicari lewat kolom id — `crew_id` untuk skema kru, `staff_id`
 * untuk AM/RM. Nominal diambil apa adanya dari `total_bonus`; rumusnya tidak
 * dihitung ulang di sini supaya angka beranda tidak pernah berbeda dari
 * laporan bonus di web finance.
 */
fun susunBonusBulanan(
    skema: SkemaBonus,
    staffId: String,
    baris: List<JsonObject>,
    bulan: Int,
    tahun: Int,
): BonusBulanan {
    val kolomId = if (skema == SkemaBonus.KRU) "crew_id" else "staff_id"
    val milik = baris.firstOrNull { it.optString(kolomId) == staffId }
        ?: return BonusBulanan(bulan = bulan, tahun = tahun, nominal = null)

    val tarif = milik.optDouble("bonus_rate")?.roundToLong() ?: 0L
    val nominal = milik.optDouble("total_bonus")?.roundToLong() ?: 0L
    return when (skema) {
        SkemaBonus.KRU -> BonusBulanan(
            bulan = bulan,
            tahun = tahun,
            nominal = nominal,
            totalPcs = milik.optDouble("total_pcs_outlet")?.toLong() ?: 0L,
            tarif = tarif,
            pembagi = (milik.optInt("active_crew_count") ?: 1).coerceAtLeast(1),
            cakupan = milik.optString("outlet_name") ?: "",
        )
        SkemaBonus.AREA_MANAGER -> BonusBulanan(
            bulan = bulan,
            tahun = tahun,
            nominal = nominal,
            totalPcs = milik.optDouble("total_pcs")?.toLong() ?: 0L,
            tarif = tarif,
            cakupan = "${milik.optInt("managed_outlet_count") ?: 0} cabang binaan",
        )
        SkemaBonus.REGIONAL_MANAGER -> BonusBulanan(
            bulan = bulan,
            tahun = tahun,
            nominal = nominal,
            totalPcs = milik.optDouble("total_pcs_global")?.toLong() ?: 0L,
            tarif = tarif,
            cakupan = milik.optString("scope_description") ?: "Semua cabang operasional",
        )
    }
}

private val LOKAL_ID = Locale("id", "ID")

/** Pemisah ribuan gaya Indonesia — salinan `FormatRupiah.kt` modul Manager, alasan yang sama. */
fun ribuan(nilai: Long): String {
    val tanda = if (nilai < 0) "-" else ""
    val angka = kotlin.math.abs(nilai).toString()
    return tanda + angka.reversed().chunked(3).joinToString(".").reversed()
}

fun rupiah(nilai: Long): String =
    if (nilai < 0) "-Rp ${ribuan(-nilai)}" else "Rp ${ribuan(nilai)}"
