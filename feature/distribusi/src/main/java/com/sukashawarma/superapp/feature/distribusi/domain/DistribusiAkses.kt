package com.sukashawarma.superapp.feature.distribusi.domain

import com.sukashawarma.superapp.domain.model.Role

/**
 * Satu-satunya tempat role dibedakan di modul Distribusi.
 *
 * Mengikuti `isPusat` di BottomNav.tsx dan SuratJalanDetail.tsx web:
 * AM adalah penerima outlet, sedangkan RM dan kitchen adalah pengelola pusat.
 */
object DistribusiAkses {

    /** Role yang modul Distribusi native-nya terbuka. `admin` dan `owner` tetap
     *  memakai versi web. */
    val ROLE_MODUL: Set<Role> = setOf(
        Role.CREW,
        Role.LEADER,
        Role.AREA_MANAGER,
        Role.REGIONAL_MANAGER,
        Role.KITCHEN,
    )

    fun bolehMembuka(role: Role?): Boolean = role in ROLE_MODUL

    /** Varian penerima web mencakup AM, crew, dan leader. */
    fun bolehVerifikasi(role: Role?): Boolean =
        role == Role.CREW || role == Role.LEADER || role == Role.AREA_MANAGER

    /**
     * Pengirim dari Gudang Pusat: membuat surat jalan, menandatanganinya bersama
     * supir, lalu mengirimkannya. Dari role native hanya kitchen — RPC
     * `create_surat_jalan_with_number` memang menolak RM.
     */
    fun bolehTerbitkan(role: Role?): Boolean = role == Role.KITCHEN

    /** `canCancelPO` web: kitchen, purchasing, admin, owner — dari native hanya kitchen. */
    fun bolehBatalkanDraft(role: Role?): Boolean = role == Role.KITCHEN

    /** Dari role native, RM dan kitchen masuk daftar `isPusat`/`isPusatSender` web. */
    fun bolehTutupDokumen(role: Role?): Boolean =
        role == Role.REGIONAL_MANAGER || role == Role.KITCHEN

    /** AM tidak melihat kode pengirim; penerimaan tetap melewati scan fisik. */
    fun bolehLihatKodeVerifikasi(role: Role?): Boolean = bolehTutupDokumen(role)
}
