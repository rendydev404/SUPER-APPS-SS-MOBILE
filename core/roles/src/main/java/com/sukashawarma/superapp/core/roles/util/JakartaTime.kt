package com.sukashawarma.superapp.domain.util

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/** Batas hari WIB — dipakai berulang di query "hari ini" (attendance, checklist,
 *  shift). Satu tempat supaya semua modul sepakat soal kapan "hari ini" berganti. */
object JakartaTime {
    val ZONE: ZoneId = ZoneId.of("Asia/Jakarta")

    fun now(): ZonedDateTime = ZonedDateTime.now(ZONE)

    fun todayStartIso(): String = LocalDate.now(ZONE).atStartOfDay(ZONE).toInstant().toString()

    fun todayEndIso(): String =
        LocalDate.now(ZONE).plusDays(1).atStartOfDay(ZONE).minusNanos(1).toInstant().toString()

    fun todayDateStr(): String = LocalDate.now(ZONE).toString() // YYYY-MM-DD
}
