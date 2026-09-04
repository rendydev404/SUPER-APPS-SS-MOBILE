package com.sukashawarma.superapp.feature.distribusi.domain

import com.google.gson.JsonParser
import com.sukashawarma.superapp.data.remote.Postgrest

/**
 * Menerjemahkan kegagalan jaringan dan kegagalan PostgREST menjadi kalimat
 * Indonesia yang menyebut tindakan yang bisa diambil pengguna. Mengikuti pola
 * `networkErrorMessage` di `AppSession`.
 *
 * RPC distribusi (`sign_receipt_surat_jalan`, `finalize_surat_jalan_and_ledger`)
 * sudah melempar pesan berbahasa Indonesia yang ditujukan ke pengguna, jadi
 * pesan itu diteruskan apa adanya alih-alih ditimpa kalimat generik.
 */
fun distribusiErrorMessage(e: Throwable): String = when (e) {
    is Postgrest.PostgrestException -> when (e.code) {
        401 -> "Sesi Anda berakhir. Silakan masuk kembali."
        403 -> "Anda tidak punya akses ke outlet ini. Hubungi atasan bila ini keliru."
        else -> pesanDariBody(e.message) ?: "Server sedang bermasalah. Coba lagi beberapa saat lagi."
    }
    is java.net.UnknownHostException ->
        "Tidak ada koneksi internet. Periksa jaringan Wi-Fi atau data seluler Anda."
    is java.net.SocketTimeoutException -> "Server tidak merespons. Coba lagi."
    is javax.net.ssl.SSLException ->
        "Gagal membangun koneksi aman. Pastikan tanggal dan waktu perangkat Anda benar."
    is java.io.IOException -> "Gagal terhubung ke server. Periksa koneksi internet."
    else -> "Terjadi kesalahan tak terduga. Coba lagi."
}

/** PostgREST membalas JSON `{"code":..,"message":..,"details":..}`. Bila bukan
 *  JSON atau tanpa `message`, kembalikan null supaya pemanggil memakai kalimat
 *  generiknya sendiri — jangan pernah menampilkan potongan JSON mentah. */
private fun pesanDariBody(body: String?): String? {
    if (body.isNullOrBlank()) return null
    return try {
        val pesan = JsonParser.parseString(body).asJsonObject.get("message")?.asString
        pesan?.takeIf { it.isNotBlank() }
    } catch (e: Exception) {
        null
    }
}
