package com.sukashawarma.superapp.feature.stok.domain

import com.google.gson.JsonParseException
import com.sukashawarma.superapp.data.remote.Postgrest
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Kode SQLSTATE yang perlu dibedakan dari galat HTTP biasa.
 *
 * `57014` adalah statement timeout PostgreSQL: pekerjaan dibatalkan server di
 * tengah jalan. Bedanya dengan "server bermasalah" penting bagi pengguna —
 * datanya DIJAMIN tidak tersimpan, jadi mengulang aman dan justru dianjurkan,
 * bukan berisiko menggandakan.
 */
private const val SQLSTATE_STATEMENT_TIMEOUT = "57014"

/** Membaca `code` SQLSTATE dari badan balasan PostgREST, bila ada. */
private fun Postgrest.PostgrestException.sqlState(): String? = runCatching {
    com.google.gson.JsonParser.parseString(message.orEmpty())
        .asJsonObject.get("code")?.asString
}.getOrNull()

/**
 * Membaca `message` dari badan balasan PostgREST bila ada pesan spesifik dari server/database.
 * Pesan teknis internal PostgreSQL (seperti RLS mentah atau syntax error) disaring agar
 * pengguna tidak melihat teks teknis mentah.
 */
private fun Postgrest.PostgrestException.pesanRamahDb(): String? {
    val pesan = runCatching {
        com.google.gson.JsonParser.parseString(message.orEmpty())
            .asJsonObject.get("message")?.asString?.trim()
    }.getOrNull()?.takeIf { it.isNotBlank() } ?: return null

    val teknis = pesan.startsWith("new row violates row-level", ignoreCase = true) ||
        pesan.startsWith("permission denied", ignoreCase = true) ||
        pesan.startsWith("canceling statement", ignoreCase = true) ||
        pesan.startsWith("null value in column", ignoreCase = true)

    return if (teknis) null else pesan
}

/**
 * Pesan error yang user-facing, dibedakan per penyebab.
 *
 * Meringkas semua kegagalan menjadi "periksa koneksi internet" membuat orang
 * memperbaiki hal yang salah: jam perangkat yang meleset menggagalkan TLS, dan
 * penolakan RLS bukan masalah jaringan sama sekali. Pola ini mengikuti
 * `AppSession.networkErrorMessage` yang sudah dipakai di modul Absensi.
 */
fun stokErrorMessage(e: Throwable): String = when (e) {
    is Postgrest.PostgrestException -> {
        val ramah = e.pesanRamahDb()
        when {
            e.sqlState() == SQLSTATE_STATEMENT_TIMEOUT ->
                "Server terlalu lama memproses dan membatalkan permintaan. " +
                    "Data Anda BELUM tersimpan — silakan coba kirim lagi."
            e.code in 500..599 -> "Server sedang bermasalah. Coba lagi beberapa saat lagi."
            ramah != null -> ramah
            e.code == 401 || e.code == 403 -> "Anda tidak punya akses ke data outlet ini."
            e.code == 404 -> "Data yang diminta tidak ditemukan di server."
            else -> "Permintaan ditolak server (kode ${e.code})."
        }
    }
    is UnknownHostException ->
        "Tidak ada koneksi internet. Periksa jaringan Wi-Fi/data seluler Anda."
    is SocketTimeoutException ->
        "Server tidak merespons (koneksi lambat/timeout). Coba lagi."
    is SSLException ->
        "Gagal membangun koneksi aman ke server. Pastikan tanggal & waktu perangkat Anda benar."
    is IOException ->
        "Gagal terhubung ke server. Periksa koneksi internet."
    is JsonParseException ->
        "Server mengirim balasan yang tak dikenali. Coba lagi beberapa saat lagi."
    else -> "Terjadi kesalahan tak terduga. Coba lagi."
}
