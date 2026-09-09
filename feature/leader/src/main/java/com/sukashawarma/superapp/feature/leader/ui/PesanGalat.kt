package com.sukashawarma.superapp.feature.leader.ui

import com.sukashawarma.superapp.data.remote.Postgrest

/**
 * Menerjemahkan kegagalan jaringan menjadi kalimat yang menyebut penyebab
 * sebenarnya.
 *
 * Dulu seluruh exception jatuh ke satu pesan "periksa koneksi internet" di beberapa
 * layar, sehingga begitu penyebabnya BUKAN internet — jam perangkat salah sampai TLS
 * gagal, misalnya — pengguna diarahkan memeriksa hal yang keliru. Rinciannya tetap
 * di Logcat lewat `Log.e` di pemanggil, bukan diperlihatkan ke layar.
 *
 * [apa] melengkapi kalimat terakhir: "Gagal memuat <apa>."
 */
fun pesanGalatMuat(e: Exception, apa: String): String = when (e) {
    is java.net.UnknownHostException ->
        "Tidak ada koneksi internet. Periksa jaringan Wi-Fi/data seluler Anda."
    is java.net.SocketTimeoutException ->
        "Server tidak merespons (koneksi lambat). Coba lagi."
    is javax.net.ssl.SSLException ->
        "Gagal membangun koneksi aman. Pastikan tanggal & waktu perangkat Anda benar."
    is java.io.IOException ->
        "Gagal terhubung ke server. Periksa koneksi internet."
    else -> "Gagal memuat $apa. Coba lagi."
}

/**
 * Pesan untuk tindakan yang ditolak database.
 *
 * RPC petty cash melempar kalimat Postgres yang benar-benar berguna — status sudah
 * berpindah, role tidak berwenang, rekening belum lengkap — jadi maksudnya
 * diteruskan alih-alih ditelan menjadi "gagal memproses". Yang tidak diteruskan
 * adalah teks mentahnya: "Top up is not ready for Leader forwarding (status: ...)"
 * tidak memberi tahu pengguna apa yang harus dilakukan berikutnya.
 */
fun pesanGalatAksi(e: Exception, apa: String): String = when {
    e is Postgrest.PostgrestException -> {
        val isi = e.message.orEmpty()
        when {
            "not ready for" in isi ->
                "Pengajuan ini sudah diproses di tempat lain. Muat ulang untuk melihat status terbarunya."
            "Not authorized" in isi ->
                "Peran Anda tidak berwenang melakukan tindakan ini."
            "Hanya Leader ke atas" in isi ->
                "Peran Anda tidak berwenang mengajukan petty cash."
            "Nominal pengajuan tidak valid" in isi ->
                "Nominal pengajuan tidak valid."
            "Tidak berwenang melihat saldo" in isi ->
                "Anda tidak punya akses ke cabang ini."
            else -> "Gagal $apa. Coba lagi."
        }
    }
    else -> pesanGalatMuat(e, apa)
}
