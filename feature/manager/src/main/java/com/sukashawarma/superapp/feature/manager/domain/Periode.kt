package com.sukashawarma.superapp.feature.manager.domain

import java.time.LocalDate
import java.time.ZoneId

/** Zona waktu operasional perusahaan — semua batas hari dihitung di sini, bukan di zona perangkat. */
val ZONA_JAKARTA: ZoneId = ZoneId.of("Asia/Jakarta")

/**
 * Rentang tanggal inklusif, `dari`..`sampai`, dalam tanggal lokal Jakarta.
 *
 * Disimpan sebagai LocalDate (bukan String) supaya perbandingan dan aritmetika
 * tanggal tidak jatuh ke perbandingan leksikografis string seperti di web.
 */
data class RentangTanggal(val dari: LocalDate, val sampai: LocalDate) {
    /** Jumlah hari inklusif — dipakai menggeser rentang ke periode sebelumnya. */
    val jumlahHari: Long get() = java.time.temporal.ChronoUnit.DAYS.between(dari, sampai) + 1
}

/**
 * Preset filter periode. Cermin `Preset` di `lib/period.ts` web, termasuk dua
 * pasangan yang sengaja duplikat di sana (`week`/`7d` dan `month`/`30d`) supaya
 * tautan lama tetap berlaku.
 */
enum class PresetPeriode(val kunci: String, private val rentangHari: Long) {
    HARI_INI("today", 0),
    KEMARIN("yesterday", 0),
    MINGGU("week", 6),
    BULAN("month", 29),
    TUJUH_HARI("7d", 6),
    TIGA_PULUH_HARI("30d", 29),
    BULAN_INI("this_month", 0);

    companion object {
        /** Nilai tak dikenal jatuh ke HARI_INI — sama seperti guard `includes(period)` di web. */
        fun dari(kunci: String?): PresetPeriode =
            entries.find { it.kunci == kunci } ?: HARI_INI
    }

    fun rentang(hariIni: LocalDate = LocalDate.now(ZONA_JAKARTA)): RentangTanggal = when (this) {
        KEMARIN -> hariIni.minusDays(1).let { RentangTanggal(it, it) }
        BULAN_INI -> RentangTanggal(hariIni.withDayOfMonth(1), hariIni)
        else -> RentangTanggal(hariIni.minusDays(rentangHari), hariIni)
    }
}

/**
 * Rentang pembanding: sama panjang, tepat menempel sebelum [rentang].
 *
 * Dipakai untuk badge "% dari periode sebelumnya" pada kartu KPI. Panjangnya
 * ikut rentang utama, jadi filter 30 hari dibandingkan dengan 30 hari sebelumnya
 * — bukan dengan kemarin.
 */
fun rentangSebelumnya(rentang: RentangTanggal): RentangTanggal {
    val hari = rentang.jumlahHari
    return RentangTanggal(rentang.dari.minusDays(hari), rentang.dari.minusDays(1))
}

/** Awal hari Jakarta dalam bentuk yang diterima PostgREST (`2026-09-05T00:00:00+07:00`). */
fun RentangTanggal.awalIso(): String = "${dari}T00:00:00+07:00"

/** Akhir hari Jakarta, inklusif sampai milidetik terakhir — cermin batas `lte` di web. */
fun RentangTanggal.akhirIso(): String = "${sampai}T23:59:59.999+07:00"
