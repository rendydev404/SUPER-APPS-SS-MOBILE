package com.sukashawarma.superapp.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Build
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await

data class DeviceFix(
    val lat: Double,
    val lng: Double,
    val accuracyM: Float,
    /** `Location.isFromMockProvider()` — pengganti heuristik akurasi 1.0/0.0 di web (A4).
     *  API resmi Android, tidak bisa dipalsukan lewat payload seperti `is_mock` di web. */
    val isMock: Boolean,
    val timestampMs: Long,
)

/**
 * Cermin dua-probe di useClockKiosk.ts (web): probe cepat (low accuracy, cache boleh basi)
 * untuk UX instan, lalu probe presisi. Di native kita punya `getCurrentLocation` yang sudah
 * digabung; dua panggilan prioritas berbeda dipertahankan supaya perilaku tetap sama.
 */
class LocationRepository(context: Context) {
    private val client = LocationServices.getFusedLocationProviderClient(context.applicationContext)

    @SuppressLint("MissingPermission")
    suspend fun fastFix(): DeviceFix? = fix(Priority.PRIORITY_BALANCED_POWER_ACCURACY)

    @SuppressLint("MissingPermission")
    suspend fun preciseFix(): DeviceFix? = fix(Priority.PRIORITY_HIGH_ACCURACY)

    @SuppressLint("MissingPermission")
    private suspend fun fix(priority: Int): DeviceFix? {
        try {
            val request = CurrentLocationRequest.Builder()
                .setPriority(priority)
                .setGranularity(Granularity.GRANULARITY_FINE)
                .setMaxUpdateAgeMillis(if (priority == Priority.PRIORITY_BALANCED_POWER_ACCURACY) 30_000L else 0L)
                .build()
            val location: Location = client.getCurrentLocation(request, null).await() ?: return null
            return DeviceFix(
                lat = location.latitude,
                lng = location.longitude,
                accuracyM = location.accuracy,
                isMock = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) location.isMock else location.isFromMockProvider,
                timestampMs = location.time,
            )
        } catch (e: SecurityException) {
            return null
        }
    }
}
