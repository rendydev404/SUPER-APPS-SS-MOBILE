package com.sukashawarma.superapp.feature.stok.domain

import com.google.gson.JsonParseException
import com.sukashawarma.superapp.data.remote.Postgrest
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Pesan error yang user-facing, dibedakan per penyebab.
 *
 * Meringkas semua kegagalan menjadi "periksa koneksi internet" membuat orang
 * memperbaiki hal yang salah: jam perangkat yang meleset menggagalkan TLS, dan
 * penolakan RLS bukan masalah jaringan sama sekali. Pola ini mengikuti
 * `AppSession.networkErrorMessage` yang sudah dipakai di modul Absensi.
 */
fun stokErrorMessage(e: Throwable): String = when (e) {
    is Postgrest.PostgrestException -> when (e.code) {
        401, 403 -> "Anda tidak punya akses ke data outlet ini."
        404 -> "Data yang diminta tidak ditemukan di server."
        in 500..599 -> "Server sedang bermasalah. Coba lagi beberapa saat lagi."
        else -> "Permintaan ditolak server (kode ${e.code})."
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
