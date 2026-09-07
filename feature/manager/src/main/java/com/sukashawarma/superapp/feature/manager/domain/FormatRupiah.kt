package com.sukashawarma.superapp.feature.manager.domain

/**
 * Pemisah ribuan bergaya Indonesia: `1250000` menjadi `1.250.000`.
 *
 * Ditulis sendiri, bukan lewat `NumberFormat` bawaan, karena bentuk keluarannya
 * berbeda antar versi Android (ada yang menyisipkan spasi tak-putus, ada yang
 * tidak) — sementara angka di layar ini dibandingkan langsung dengan layar web
 * oleh orang yang sama, jadi bentuknya harus tetap.
 */
fun ribuan(nilai: Long): String {
    val tanda = if (nilai < 0) "-" else ""
    val angka = kotlin.math.abs(nilai).toString()
    return tanda + angka.reversed().chunked(3).joinToString(".").reversed()
}

/** Rupiah tanpa desimal: `Rp 1.250.000`. Tanda minus mendahului `Rp`, bukan angkanya. */
fun rupiah(nilai: Long): String =
    if (nilai < 0) "-Rp ${ribuan(-nilai)}" else "Rp ${ribuan(nilai)}"

/** Bilangan cacah dengan pemisah ribuan — untuk jumlah transaksi dan porsi. */
fun cacah(nilai: Int): String = ribuan(nilai.toLong())
