package com.sukashawarma.superapp.feature.distribusi.domain

import com.sukashawarma.superapp.domain.model.Role

/**
 * Satu-satunya tempat role dibedakan di modul Distribusi.
 *
 * Mengikuti `isPusat` di BottomNav.tsx dan SuratJalanDetail.tsx web:
 * AM adalah penerima outlet, sedangkan RM adalah pengelola pusat.
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

    /** Varian penerima web mencakup AM, crew, dan leader. */
    fun bolehVerifikasi(role: Role?): Boolean =
        role == Role.CREW || role == Role.LEADER || role == Role.AREA_MANAGER

    /** Dari role native, hanya RM masuk daftar `isPusat` web. */
    fun bolehTutupDokumen(role: Role?): Boolean =
        role == Role.REGIONAL_MANAGER

    /** AM tidak melihat kode pengirim; penerimaan tetap melewati scan fisik. */
    fun bolehLihatKodeVerifikasi(role: Role?): Boolean = bolehTutupDokumen(role)
}
