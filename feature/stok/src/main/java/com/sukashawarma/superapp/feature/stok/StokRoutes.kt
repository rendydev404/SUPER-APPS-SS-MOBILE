package com.sukashawarma.superapp.feature.stok

import android.net.Uri

/** Rute internal modul Stok — NavHost sendiri, dipasang di `Routes.STOK` (root). */
object StokRoutes {
    const val MONITORING = "stok/monitoring"

    private const val DETAIL_POLA = "stok/bahan"
    const val DETAIL = "$DETAIL_POLA/{outletId}/{bahanId}/{nama}"

    const val TRANSFER = "stok/transfer"

    /** Nama bahan bisa mengandung spasi dan garis miring, jadi wajib di-encode. */
    fun detail(outletId: String, bahanId: String, nama: String): String =
        "$DETAIL_POLA/$outletId/$bahanId/${Uri.encode(nama)}"
}
