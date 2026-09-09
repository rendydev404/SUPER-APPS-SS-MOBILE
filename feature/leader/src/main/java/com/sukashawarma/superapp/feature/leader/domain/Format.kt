package com.sukashawarma.superapp.feature.leader.domain

import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeParseException

/** Zona waktu operasional perusahaan — semua batas hari dihitung di sini, bukan di zona perangkat. */
val ZONA_JAKARTA: ZoneId = ZoneId.of("Asia/Jakarta")

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

/** Bilangan cacah dengan pemisah ribuan — untuk jumlah transaksi. */
fun cacah(nilai: Int): String = ribuan(nilai.toLong())

/**
 * Kuantitas stok: dua angka di belakang koma, nol di belakang dibuang.
 *
 * `12.0` menjadi `12`, `12.5` menjadi `12,5`, `12.345` menjadi `12,35` — cermin
 * `Number(qty.toFixed(2))` di halaman stok web, dengan koma desimal Indonesia.
 */
fun kuantitas(nilai: Double): String {
    val dibulatkan = kotlin.math.round(nilai * 100) / 100
    val teks = if (dibulatkan == kotlin.math.floor(dibulatkan) && !dibulatkan.isInfinite()) {
        dibulatkan.toLong().toString()
    } else {
        dibulatkan.toString().trimEnd('0').trimEnd('.')
    }
    return teks.replace('.', ',')
}

/** Hari ini menurut jam Jakarta — dasar seluruh batas "hari ini" di modul ini. */
fun hariIniJakarta(): LocalDate = LocalDate.now(ZONA_JAKARTA)

/** Awal hari Jakarta dalam bentuk yang diterima PostgREST (`2026-09-05T00:00:00.000+07:00`). */
fun awalHariIso(tanggal: LocalDate): String = "${tanggal}T00:00:00.000+07:00"

/** Akhir hari Jakarta, inklusif sampai milidetik terakhir — cermin batas `lte` di web. */
fun akhirHariIso(tanggal: LocalDate): String = "${tanggal}T23:59:59.999+07:00"

/**
 * `2026-09-05T01:15:00+00:00` menjadi `08:15` waktu Jakarta.
 *
 * Cap waktu dari server selalu UTC, sedangkan yang dibandingkan pengguna adalah jam
 * operasional outlet. Mengandalkan zona waktu perangkat berarti leader yang sedang
 * bepergian melihat jam yang berbeda dari krunya untuk transaksi yang sama.
 *
 * String yang tak terbaca dikembalikan apa adanya — lebih baik menampilkan bentuk
 * mentah daripada jam karangan.
 */
fun jamJakarta(iso: String): String = try {
    val t = OffsetDateTime.parse(iso).atZoneSameInstant(ZONA_JAKARTA)
    "${t.hour.toString().padStart(2, '0')}:${t.minute.toString().padStart(2, '0')}"
} catch (e: DateTimeParseException) {
    iso
}

private val BULAN_SINGKAT = arrayOf(
    "Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
    "Jul", "Agu", "Sep", "Okt", "Nov", "Des",
)

/** `5 Sep 2026 08:15` — bentuk yang dipakai kepala kartu pengajuan, cermin `formatDateTime` web. */
fun waktuJakarta(iso: String): String = try {
    val t = OffsetDateTime.parse(iso).atZoneSameInstant(ZONA_JAKARTA)
    val jam = t.hour.toString().padStart(2, '0')
    val menit = t.minute.toString().padStart(2, '0')
    "${t.dayOfMonth} ${BULAN_SINGKAT[t.monthValue - 1]} ${t.year} $jam:$menit"
} catch (e: DateTimeParseException) {
    iso
}
