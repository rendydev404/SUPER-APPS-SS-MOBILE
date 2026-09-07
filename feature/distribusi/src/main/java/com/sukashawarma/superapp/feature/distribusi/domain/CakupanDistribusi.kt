package com.sukashawarma.superapp.feature.distribusi.domain

import com.sukashawarma.superapp.domain.model.Role

enum class DaftarDistribusi { DASHBOARD, INBOX, RIWAYAT }

/** Filter tampilan AM dari hooks web, di dalam cakupan yang diizinkan server.
 * Dashboard/riwayat tanpa outlet kosong; inbox tanpa outlet memakai cakupan RLS.
 * Filter dihitung ulang per permintaan agar perubahan outlet sesi langsung berlaku.
 */
fun cakupanDistribusi(
    role: Role?,
    outletSesi: String?,
    diizinkanServer: List<String>,
    daftar: DaftarDistribusi,
): List<String> {
    if (!DistribusiAkses.bolehMembuka(role)) return emptyList()
    if (role != Role.AREA_MANAGER) return diizinkanServer
    val outlet = outletSesi?.takeIf { it.isNotBlank() }
    if (outlet != null) return diizinkanServer.filter { it == outlet }
    return if (daftar == DaftarDistribusi.INBOX) diizinkanServer else emptyList()
}
