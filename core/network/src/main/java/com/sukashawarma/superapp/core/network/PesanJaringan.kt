package com.sukashawarma.superapp.data.remote

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Klasifikasi kegagalan jaringan, satu tempat untuk seluruh aplikasi.
 *
 * Sebelumnya tangga `when (e)` yang sama disalin di `feature/leader/ui/PesanGalat.kt`,
 * `feature/distribusi/domain/DistribusiError.kt`, `feature/stok/domain/...stokErrorMessage`,
 * dan di hampir setiap ViewModel manager. Selama hanya soal kalimat, duplikasi itu tidak
 * berbahaya. Mode offline mengubahnya: cache dan antrean tulis harus bisa memutuskan
 * "ini gangguan jaringan, tahan dan coba lagi" versus "ini penolakan server, jangan diulang",
 * dan keputusan itu tidak boleh berbeda-beda per modul.
 */

/**
 * Benar bila kegagalan berasal dari jaringan, bukan dari server yang menolak.
 *
 * Ini pemisah utama mode offline:
 * - true  -> data boleh diambil dari cache, aksi boleh masuk antrean;
 * - false -> server sudah menjawab dan jawabannya "tidak". Mengulanginya nanti hanya
 *            menghasilkan penolakan yang sama, dan menyajikan cache di sini berarti
 *            menyembunyikan penolakan itu dari pengguna.
 *
 * 5xx dihitung sebagai gangguan sementara: server sedang bermasalah, bukan permintaannya
 * yang salah. 408 dan 429 juga, karena keduanya secara eksplisit meminta dicoba lagi.
 */
fun adalahGalatJaringan(e: Throwable): Boolean = when (e) {
    is Postgrest.PostgrestException -> e.code >= 500 || e.code == 408 || e.code == 429
    is UnknownHostException, is SocketTimeoutException, is SSLException -> true
    is IOException -> true
    else -> false
}

/**
 * Kalimat untuk kegagalan memuat data. [apa] melengkapi kalimat terakhir:
 * "Gagal memuat <apa>."
 *
 * Kata-katanya sengaja dipertahankan persis seperti versi lama di feature/leader agar
 * penyatuan ini tidak diam-diam mengubah apa yang dibaca crew di lapangan.
 */
fun pesanGalatJaringan(e: Throwable, apa: String): String = when (e) {
    is UnknownHostException ->
        "Tidak ada koneksi internet. Periksa jaringan Wi-Fi/data seluler Anda."
    is SocketTimeoutException ->
        "Server tidak merespons (koneksi lambat). Coba lagi."
    is SSLException ->
        "Gagal membangun koneksi aman. Pastikan tanggal & waktu perangkat Anda benar."
    is IOException ->
        "Gagal terhubung ke server. Periksa koneksi internet."
    else -> "Gagal memuat $apa. Coba lagi."
}
