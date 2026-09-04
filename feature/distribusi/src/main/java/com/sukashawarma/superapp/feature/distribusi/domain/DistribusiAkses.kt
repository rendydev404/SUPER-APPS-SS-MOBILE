package com.sukashawarma.superapp.feature.distribusi.domain

import com.sukashawarma.superapp.domain.model.Role

/**
 * Satu-satunya tempat role dibedakan di modul Distribusi.
 *
 * CAKUPAN OUTLET TIDAK DIPUTUSKAN DI SINI. Outlet mana yang terlihat oleh
 * seorang pengguna ditentukan RPC `accessible_outlet_ids()` di database, supaya
 * perubahan kebijakan tidak menuntut rilis APK baru. Yang ada di file ini hanya
 * kemampuan — dua hal saja, dan keduanya betul-betul berbeda antar role.
 */
object DistribusiAkses {

    /** Role yang modul Distribusi native-nya terbuka. `kitchen` dan `admin`
     *  sengaja tidak masuk: penerbitan surat jalan tetap di web. */
    val ROLE_MODUL: Set<Role> = setOf(
        Role.CREW,
        Role.LEADER,
        Role.AREA_MANAGER,
        Role.REGIONAL_MANAGER,
    )

    fun bolehMembuka(role: Role?): Boolean = role in ROLE_MODUL

    /** Menerima barang adalah pekerjaan orang yang berdiri di outlet. */
    fun bolehVerifikasi(role: Role?): Boolean =
        role == Role.CREW || role == Role.LEADER

    /** Menutup dokumen jadi `selesai` adalah pekerjaan pengawas. */
    fun bolehTutupDokumen(role: Role?): Boolean =
        role == Role.AREA_MANAGER || role == Role.REGIONAL_MANAGER

    /** Kode/QR verifikasi hanya boleh dilihat pengawas — lihat komentar
     *  gerbang QR di spec §3. */
    fun bolehLihatKodeVerifikasi(role: Role?): Boolean = bolehTutupDokumen(role)
}
