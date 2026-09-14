package com.sukashawarma.superapp.domain.gps

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class LatLng(val lat: Double, val lng: Double)

/** Cermin persis apps/absensi/src/lib/gps.ts (web) — jangan menyimpang, ini
 *  keputusan keamanan (radius geofence & ambang akurasi), bukan detail kosmetik. */
object GpsMath {
    const val GEOFENCE_RADIUS_M = 100.0
    const val MAX_GPS_ACCURACY_M = 150.0
    const val MAX_REASONABLE_SPEED_KMH = 160.0
    private const val EARTH_RADIUS_M = 6_371_000.0

    fun haversineMeters(a: LatLng, b: LatLng): Double {
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLng = Math.toRadians(b.lng - a.lng)
        val h = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) * sin(dLng / 2).pow(2)
        return 2 * EARTH_RADIUS_M * asin(min(1.0, sqrt(h)))
    }

    fun isGpsAccuracyAcceptable(accuracyM: Float): Boolean = accuracyM <= MAX_GPS_ACCURACY_M

    /** True bila dalam radius geofence, dengan toleransi akurasi (dist - accuracy <= radius). */
    fun isWithinGeofence(
        outlet: LatLng,
        device: LatLng,
        accuracyM: Float,
        radiusM: Double = GEOFENCE_RADIUS_M,
    ): Boolean {
        val dist = haversineMeters(outlet, device)
        val adjusted = (dist - accuracyM).coerceAtLeast(0.0)
        return adjusted <= radiusM
    }

    /** Cermin calculateSpeedKmH web — deteksi teleportasi dibandingkan absen terakhir. */
    fun speedKmh(prev: LatLng, prevMs: Long, curr: LatLng, currMs: Long): Double {
        val hours = (currMs - prevMs) / 3_600_000.0
        if (hours <= 0) return 0.0
        return (haversineMeters(prev, curr) / 1000.0) / hours
    }

    /**
     * Format jarak agar mudah dibaca: ubah meter menjadi kilometer jika >= 1000 m.
     * Contoh: 8502 m -> "8.5 km", 1000 m -> "1 km", 250 m -> "250 m".
     */
    fun formatDistance(meters: Double, withSpace: Boolean = true): String {
        val sep = if (withSpace) " " else ""
        return if (meters >= 1000.0) {
            val km = meters / 1000.0
            val text = String.format(java.util.Locale.US, "%.1f", km).removeSuffix(".0")
            "$text${sep}km"
        } else {
            "${meters.toInt()}${sep}m"
        }
    }

    /** Jarak dalam format ringkas tanpa spasi (misal: "8.5km", "250m"). */
    fun formatDistanceShort(meters: Double): String = formatDistance(meters, withSpace = false)
}

