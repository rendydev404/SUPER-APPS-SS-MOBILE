package com.sukashawarma.superapp.data.location

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.domain.session.AppSession

/** Satu titik posisi siap kirim. Disimpan sebagai JSON di antrean supaya bentuk yang
 *  di-retry persis sama dengan yang gagal dikirim — tidak ada konversi ulang. */
data class TrackPoint(
    val lat: Double,
    val lng: Double,
    val accuracyM: Float,
    val speedMps: Float,
    val headingDeg: Float,
    val altitudeM: Double,
    val batteryPct: Int,
    val isCharging: Boolean,
    val isMock: Boolean,
    val isMoving: Boolean,
    val provider: String,
    val recordedAt: String,
)

/**
 * Penulis posisi ke Supabase. Diletakkan di core:location (bukan core:network) supaya
 * service dan repositori tinggal bersebelahan; core:location -> core:network/core:roles
 * tidak membentuk siklus karena kedua modul itu tidak tahu-menahu soal lokasi.
 */
object LocationTrackingRepository {
    private const val TABLE_LIVE = "staff_live_locations"
    private const val TABLE_TRAIL = "staff_location_trails"

    class NoStaffSessionException : Exception("Sesi staff tidak tersedia")

    private fun trailJson(p: TrackPoint, staffId: String, outletId: String?): JsonObject =
        JsonObject().apply {
            addProperty("outlet_staff_id", staffId)
            if (outletId != null) addProperty("outlet_id", outletId) else add("outlet_id", com.google.gson.JsonNull.INSTANCE)
            addProperty("lat", p.lat)
            addProperty("lng", p.lng)
            addProperty("accuracy_m", p.accuracyM)
            addProperty("speed_mps", p.speedMps)
            addProperty("heading_deg", p.headingDeg)
            addProperty("battery_pct", p.batteryPct)
            addProperty("is_mock", p.isMock)
            addProperty("recorded_at", p.recordedAt)
        }

    private fun liveJson(p: TrackPoint, staffId: String, outletId: String?, deviceName: String): JsonObject =
        trailJson(p, staffId, outletId).apply {
            addProperty("altitude_m", p.altitudeM)
            addProperty("is_charging", p.isCharging)
            addProperty("is_moving", p.isMoving)
            addProperty("provider", p.provider)
            addProperty("device_name", deviceName)
            addProperty("updated_at", p.recordedAt)
        }

    fun encode(p: TrackPoint): String = JsonObject().apply {
        addProperty("lat", p.lat)
        addProperty("lng", p.lng)
        addProperty("accuracy_m", p.accuracyM)
        addProperty("speed_mps", p.speedMps)
        addProperty("heading_deg", p.headingDeg)
        addProperty("altitude_m", p.altitudeM)
        addProperty("battery_pct", p.batteryPct)
        addProperty("is_charging", p.isCharging)
        addProperty("is_mock", p.isMock)
        addProperty("is_moving", p.isMoving)
        addProperty("provider", p.provider)
        addProperty("recorded_at", p.recordedAt)
    }.toString()

    fun decode(raw: String): TrackPoint? = try {
        val o = JsonParser.parseString(raw).asJsonObject
        TrackPoint(
            lat = o.get("lat").asDouble,
            lng = o.get("lng").asDouble,
            accuracyM = o.get("accuracy_m").asFloat,
            speedMps = o.get("speed_mps").asFloat,
            headingDeg = o.get("heading_deg").asFloat,
            altitudeM = o.get("altitude_m").asDouble,
            batteryPct = o.get("battery_pct").asInt,
            isCharging = o.get("is_charging").asBoolean,
            isMock = o.get("is_mock").asBoolean,
            isMoving = o.get("is_moving").asBoolean,
            provider = o.get("provider").asString,
            recordedAt = o.get("recorded_at").asString,
        )
    } catch (e: Exception) {
        // Baris antrean rusak (mis. skema lama) dibuang diam-diam; melempar di sini akan
        // membuat seluruh batch ikut gagal dan antrean macet selamanya.
        null
    }

    /**
     * Kirim satu batch. Trail di-insert semua (append-only), live-location hanya titik
     * TERBARU — menulis titik lama ke tabel live justru akan memundurkan posisi di dashboard
     * saat antrean offline baru menyusul.
     */
    suspend fun push(points: List<TrackPoint>, deviceName: String) {
        if (points.isEmpty()) return
        val staff = AppSession.staff.value ?: throw NoStaffSessionException()
        val staffId = staff.id
        val outletId = staff.outletId

        val trails = JsonArray().apply {
            points.forEach { add(trailJson(it, staffId, outletId)) }
        }
        // ignore-duplicates + unique(outlet_staff_id, recorded_at): batch yang gagal di
        // langkah upsert live-location di bawah akan di-retry UTUH, termasuk titik trail
        // yang sudah masuk. Tanpa ini setiap retry menaburkan titik kembar di peta.
        Postgrest.upsert(
            TABLE_TRAIL,
            trails,
            onConflict = "outlet_staff_id,recorded_at",
            ignoreDuplicates = true,
        )

        val latest = points.maxByOrNull { it.recordedAt } ?: points.last()
        Postgrest.upsert(TABLE_LIVE, liveJson(latest, staffId, outletId, deviceName), onConflict = "outlet_staff_id")
    }
}
