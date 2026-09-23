package com.sukashawarma.superapp.data.remote

/**
 * Jeda eksponensial untuk pekerjaan latar yang terus mencoba mengirim ke server.
 *
 * Tanpa jeda, satu perangkat yang sesinya mati atau servernya menolak akan mengulang
 * request setiap kali ada pemicu (setiap fix GPS, setiap menit) sepanjang hari. Dikali
 * puluhan perangkat, itulah yang memenuhi API gateway dengan ribuan 401.
 */
object JedaCobaUlang {
    const val AWAL_MS = 30_000L
    const val MAKS_MS = 15 * 60_000L

    /** Jeda setelah [gagalBeruntun] kegagalan berturut-turut: 30 dtk, 1, 2, 4, 8, lalu 15 menit. */
    fun untuk(gagalBeruntun: Int, awalMs: Long = AWAL_MS, maksMs: Long = MAKS_MS): Long {
        if (gagalBeruntun <= 0) return 0L
        // Pangkat dibatasi supaya shift tidak meluap untuk kegagalan yang sangat panjang.
        val pangkat = (gagalBeruntun - 1).coerceAtMost(20)
        return (awalMs shl pangkat).coerceAtMost(maksMs)
    }
}
