package com.sukashawarma.superapp.feature.distribusi.domain

import java.time.LocalDate
import java.time.LocalTime

/**
 * Sapaan dan tanggal panjang untuk kepala dashboard.
 *
 * Ambang jamnya cermin `greeting` di `app/dashboard/page.tsx`: crew yang
 * membandingkan layar HP dengan layar laptop harus melihat sapaan yang sama
 * pada jam yang sama.
 */
private val HARI = arrayOf(
    "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu",
)

private val BULAN_PANJANG = arrayOf(
    "Januari", "Februari", "Maret", "April", "Mei", "Juni",
    "Juli", "Agustus", "September", "Oktober", "November", "Desember",
)

fun salamUntukJam(jam: Int): String = when {
    jam < 11 -> "Selamat Pagi"
    jam < 15 -> "Selamat Siang"
    jam < 18 -> "Selamat Sore"
    else -> "Selamat Malam"
}

fun salamSekarang(): String = salamUntukJam(LocalTime.now().hour)

/** "Jumat, 4 September 2026" — bentuk yang dipakai banner web. */
fun tanggalPanjang(tanggal: LocalDate): String {
    val hari = HARI[tanggal.dayOfWeek.value - 1]
    val bulan = BULAN_PANJANG[tanggal.monthValue - 1]
    return "$hari, ${tanggal.dayOfMonth} $bulan ${tanggal.year}"
}

fun tanggalPanjangHariIni(): String = tanggalPanjang(LocalDate.now())
