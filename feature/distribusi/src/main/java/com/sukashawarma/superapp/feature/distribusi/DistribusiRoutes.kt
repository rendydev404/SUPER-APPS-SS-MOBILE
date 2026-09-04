package com.sukashawarma.superapp.feature.distribusi

/** Rute internal modul Distribusi — NavHost sendiri, dipasang di `Routes.DISTRIBUSI`. */
object DistribusiRoutes {
    const val DASHBOARD = "distribusi/dashboard"
    const val INBOX = "distribusi/inbox"
    const val SCAN = "distribusi/scan"
    const val RIWAYAT = "distribusi/riwayat"

    private const val VERIFIKASI_POLA = "distribusi/verifikasi"
    const val VERIFIKASI = "$VERIFIKASI_POLA/{suratJalanId}"

    private const val DETAIL_POLA = "distribusi/detail"
    const val DETAIL = "$DETAIL_POLA/{suratJalanId}"

    // Argumennya UUID, jadi tidak perlu di-encode.
    fun verifikasi(suratJalanId: String): String = "$VERIFIKASI_POLA/$suratJalanId"
    fun detail(suratJalanId: String): String = "$DETAIL_POLA/$suratJalanId"
}
