package com.sukashawarma.superapp.feature.manager.domain

import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

private val BULAN_SINGKAT = arrayOf(
    "Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
    "Jul", "Agu", "Sep", "Okt", "Nov", "Des",
)

/**
 * `2026-09-05T01:15:00+00:00` menjadi `5 Sep 2026 08.15 WIB`.
 *
 * Cap waktu dari server selalu UTC, sedangkan yang dibandingkan pengguna adalah jam
 * operasional outlet. Mengandalkan zona waktu perangkat berarti manajer yang sedang
 * di luar negeri melihat jam yang berbeda dari krunya untuk kejadian yang sama.
 *
 * String yang tak terbaca dikembalikan apa adanya — lebih baik menampilkan bentuk
 * mentah daripada tanggal karangan.
 */
fun waktuJakarta(iso: String): String = try {
    val t = OffsetDateTime.parse(iso).atZoneSameInstant(ZONA_JAKARTA)
    val jam = t.hour.toString().padStart(2, '0')
    val menit = t.minute.toString().padStart(2, '0')
    "${t.dayOfMonth} ${BULAN_SINGKAT[t.monthValue - 1]} ${t.year} $jam.$menit WIB"
} catch (e: DateTimeParseException) {
    iso
}

/** Bentuk pendek tanpa tahun dan tanpa penanda zona — untuk sudut kartu yang sempit. */
fun waktuJakartaRingkas(iso: String): String = try {
    val t = OffsetDateTime.parse(iso).atZoneSameInstant(ZONA_JAKARTA)
    val jam = t.hour.toString().padStart(2, '0')
    val menit = t.minute.toString().padStart(2, '0')
    "${t.dayOfMonth} ${BULAN_SINGKAT[t.monthValue - 1]} $jam.$menit"
} catch (e: DateTimeParseException) {
    iso
}
