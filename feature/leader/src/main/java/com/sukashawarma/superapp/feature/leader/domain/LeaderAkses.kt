package com.sukashawarma.superapp.feature.leader.domain

import com.sukashawarma.superapp.domain.model.Role

/**
 * Satu-satunya gerbang role modul Leader.
 *
 * Sempit dengan sengaja: di web, `NAV_GROUPS` menandai seluruh isi "Leader
 * Dashboard" dengan `roles: ['LEADER']`, dan `RoleContext.tsx` menegakkannya lagi
 * dengan me-redirect siapa pun ber-role LEADER yang keluar dari `/dashboard/leader`
 * kembali ke sana. Jadi halaman itu memang hanya punya satu penghuni.
 *
 * Area manager TIDAK masuk ke sini walau RPC petty cash menerimanya — ia punya
 * pintunya sendiri (`/dashboard/area-manager/petty-cash`, sudah ada di modul
 * Manager native). Membuka dua pintu ke pekerjaan yang sama hanya akan membuat dua
 * layar berselisih tentang siapa yang sedang memegang pengajuan.
 *
 * Cakupan outlet TIDAK ditentukan di sini. Itu urusan `accessible_outlet_ids()`
 * di database — leader bisa membina lebih dari satu cabang lewat `staff_outlets`,
 * dan halaman web pun membacanya dari fungsi yang sama.
 */
object LeaderAkses {

    val ROLE_MODUL: Set<Role> = setOf(Role.LEADER)

    fun bolehMembuka(role: Role?): Boolean = role in ROLE_MODUL
}
