package com.sukashawarma.superapp.domain.session

import com.sukashawarma.superapp.domain.model.MitraProfile
import com.sukashawarma.superapp.domain.model.Role
import com.sukashawarma.superapp.domain.model.StaffProfile

enum class StartDestination {
    LOGIN,
    HOME,
    MITRA_DASHBOARD,
    MITRA_NO_PROFILE,
    MITRA_LOAD_ERROR,
}

/**
 * Satu-satunya penentu tujuan setelah sesi siap. RootNav DAN callback login sama-sama
 * memakai ini; dua tempat menebak aturan role sendiri-sendiri adalah persis penyakit
 * yang pernah terjadi di guard approval apps/stok (lihat CLAUDE.md sesi 2026-07-20).
 *
 * Urutan pemeriksaan `mitraLoadFailed` SEBELUM profil-null itu disengaja: mitra dengan
 * sinyal jelek tidak boleh disuruh menelepon admin pusat.
 */
/**
 * true untuk ketiga tujuan area mitra (MITRA_DASHBOARD/MITRA_NO_PROFILE/MITRA_LOAD_ERROR).
 * Bentuk graph navigasi (rute mana yang didaftarkan) DAN start destination HARUS diturunkan
 * dari satu nilai yang sama — sebelumnya MainActivity menghitung ini sendiri lewat `||` inline
 * terpisah dari resolveStartDestination(), dan ketidaksepakatan keduanya pernah menyebabkan
 * crash navigasi produksi. Taruh di sini supaya dites, bukan cuma dibaca.
 */
val StartDestination.isMitraArea: Boolean
    get() = this == StartDestination.MITRA_DASHBOARD ||
        this == StartDestination.MITRA_NO_PROFILE ||
        this == StartDestination.MITRA_LOAD_ERROR

fun resolveStartDestination(
    staff: StaffProfile?,
    mitraProfile: MitraProfile?,
    mitraLoadFailed: Boolean,
): StartDestination {
    if (staff == null) return StartDestination.LOGIN
    if (staff.role != Role.MITRA) return StartDestination.HOME
    if (mitraLoadFailed) return StartDestination.MITRA_LOAD_ERROR
    if (mitraProfile == null) return StartDestination.MITRA_NO_PROFILE
    return StartDestination.MITRA_DASHBOARD
}
