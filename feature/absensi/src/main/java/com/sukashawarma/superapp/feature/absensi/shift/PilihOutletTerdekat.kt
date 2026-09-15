package com.sukashawarma.superapp.feature.absensi.shift

import com.sukashawarma.superapp.domain.gps.GpsMath
import com.sukashawarma.superapp.domain.gps.LatLng
import kotlin.math.abs

/** Outlet yang tidak dipakai berjualan — kalah saat jaraknya seri dengan outlet operasional. */
private val NON_OPERASIONAL = setOf("test", "marketplace")
private const val TOLERANSI_SERI_M = 1.0

data class KandidatOutlet(val id: String, val type: String?, val coords: LatLng?)

/**
 * Outlet terdekat dari posisi perangkat — cermin `pilihOutletTerdekat.ts` (web).
 *
 * `outlet tes` menyalin koordinat BNR persis. Dengan perbandingan `<` yang ketat, jarak
 * yang seri dimenangkan urutan daftar, sehingga staff di BNR diam-diam aktif di outlet
 * tes — dan config dua shift BNR tak pernah terbaca. Saat jarak seri (≤ 1 m), outlet
 * operasional menang atas outlet tes/marketplace.
 */
fun pilihOutletTerdekat(list: List<KandidatOutlet>, perangkat: LatLng): String? =
    pilihOutletTerdekat(list) { GpsMath.haversineMeters(it, perangkat) }

internal fun pilihOutletTerdekat(list: List<KandidatOutlet>, jarakKe: (LatLng) -> Double): String? {
    var terbaikId: String? = null
    var terbaikJarak = 0.0
    var terbaikOperasional = false
    for (o in list) {
        val coords = o.coords ?: continue
        val jarak = jarakKe(coords)
        val operasional = o.type !in NON_OPERASIONAL
        val ganti = terbaikId == null ||
            jarak < terbaikJarak - TOLERANSI_SERI_M ||
            (abs(jarak - terbaikJarak) <= TOLERANSI_SERI_M && operasional && !terbaikOperasional)
        if (ganti) {
            terbaikId = o.id
            terbaikJarak = jarak
            terbaikOperasional = operasional
        }
    }
    return terbaikId
}
