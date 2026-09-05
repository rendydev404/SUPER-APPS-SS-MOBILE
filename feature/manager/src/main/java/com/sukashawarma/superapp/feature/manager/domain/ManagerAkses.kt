package com.sukashawarma.superapp.feature.manager.domain

import com.sukashawarma.superapp.domain.model.Role

/**
 * Satu-satunya gerbang role modul Manager.
 *
 * Cermin `enforceAppAccess(request, 'manager')` di `middleware.ts` web yang
 * ditegakkan lagi oleh `app/api/monitoring/route.ts`: hanya regional manager dan
 * area manager yang boleh masuk, role lain ditolak sebelum layar dirender.
 *
 * Perbedaan keduanya di dalam modul hanya cakupan outlet, dan itu sudah
 * ditentukan `accessible_outlet_ids()` di database (RM = seluruh outlet, AM =
 * outlet binaan lewat `staff_outlets`). Jadi jangan menyaring outlet_id lagi di
 * klien — versi web memakai service-role key sehingga HARUS menyaring manual,
 * native memakai JWT pengguna sehingga RLS sudah melakukannya.
 */
object ManagerAkses {

    val ROLE_MODUL: Set<Role> = setOf(
        Role.AREA_MANAGER,
        Role.REGIONAL_MANAGER,
    )

    fun bolehMembuka(role: Role?): Boolean = role in ROLE_MODUL

    /**
     * Tabel "Performa Zona AM" hanya untuk regional manager — area manager
     * memegang satu zona saja, jadi tabel perbandingan antar-zona tak punya arti
     * baginya. Cermin `if (staff?.role === 'area_manager') return null` di
     * `app/page.tsx`.
     */
    fun melihatPerformaZona(role: Role?): Boolean = role == Role.REGIONAL_MANAGER
}
