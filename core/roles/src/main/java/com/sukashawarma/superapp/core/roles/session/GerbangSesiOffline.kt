package com.sukashawarma.superapp.domain.session

import kotlin.math.abs

/**
 * Aturan boleh-tidaknya sebuah sesi dibuka dari snapshot lokal, tanpa server.
 *
 * Dipisahkan dari [AppSession] karena ini gerbang keamanan, bukan sekadar alur: ia yang
 * menentukan berapa lama sebuah perangkat yang hilang masih bisa membuka profil staf, dan
 * apakah profil milik orang lain bisa terbuka di perangkat yang dipakai bergantian. Di dalam
 * AppSession, aturan itu terkubur bersama Base64, SharedPreferences, dan panggilan jaringan
 * sehingga tidak bisa diuji sama sekali.
 */
sealed interface KeputusanSesiOffline {
    data object Boleh : KeputusanSesiOffline
    data class Tolak(val pesan: String) : KeputusanSesiOffline
}

object GerbangSesiOffline {

    /**
     * Toleransi jam perangkat yang meleset. Di bawah ini dianggap ketidakakuratan biasa;
     * di atasnya dianggap jam yang tidak bisa dipercaya.
     */
    const val TOLERANSI_MUNDUR_MS = 24L * 60 * 60 * 1000

    /**
     * [userIdTerverifikasi] adalah pemilik sesi yang sudah dipastikan lewat jalur lain —
     * sidik jari yang cocok, atau token yang baru saja diterbitkan server.
     */
    fun putuskan(
        snapshotUserId: String?,
        userIdTerverifikasi: String,
        terakhirOnlineMs: Long,
        sekarangMs: Long,
        umurMaksMs: Long,
    ): KeputusanSesiOffline {
        if (snapshotUserId.isNullOrBlank()) {
            return KeputusanSesiOffline.Tolak(
                "Tidak ada koneksi internet, dan perangkat ini belum punya data sesi tersimpan. " +
                    "Hubungkan ke internet sekali untuk masuk."
            )
        }
        if (snapshotUserId != userIdTerverifikasi) {
            return KeputusanSesiOffline.Tolak(
                "Data sesi tersimpan milik akun lain. Login dengan password."
            )
        }

        val umur = sekarangMs - terakhirOnlineMs

        // Jam mundur jauh berarti batas 7 hari bisa diperpanjang tanpa batas hanya dengan
        // mengubah tanggal perangkat — persis hal yang batas itu seharusnya cegah.
        if (umur < -TOLERANSI_MUNDUR_MS) {
            return KeputusanSesiOffline.Tolak(
                "Tanggal & waktu perangkat tidak wajar. Perbaiki dulu di pengaturan perangkat."
            )
        }

        if (abs(umur) > umurMaksMs) {
            return KeputusanSesiOffline.Tolak(
                "Perangkat sudah lebih dari 7 hari tanpa internet. " +
                    "Hubungkan ke internet sekali untuk melanjutkan."
            )
        }

        return KeputusanSesiOffline.Boleh
    }
}
