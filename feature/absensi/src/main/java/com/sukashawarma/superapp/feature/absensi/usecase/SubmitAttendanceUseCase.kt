package com.sukashawarma.superapp.feature.absensi.usecase

import com.sukashawarma.superapp.data.local.entity.PendingAttendanceEntity
import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.SubmitAttendanceResult
import com.sukashawarma.superapp.data.remote.optString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Tabel `attendance` dikunci RLS: INSERT hanya boleh oleh `service_role` (lihat
 * migration `20260610000300_m1_attendance_rls.sql` — "Tulis attendance HANYA
 * via Edge Function (service role). Client tidak insert langsung"). Web patuh
 * lewat Next.js API route ber-service-role-key (server-side, key tak pernah
 * sampai ke browser). Native TIDAK BOLEH menaruh service_role key di APK (bisa
 * didekompilasi — itu master key seluruh database, bukan cuma tabel ini), jadi
 * dipanggil lewat RPC `submit_attendance` (SECURITY DEFINER, migration
 * `20260821090000_submit_attendance_rpc.sql`) yang memverifikasi
 * outlet_staff_id = auth.uid() si pemanggil, lalu mengerjakan seluruh business
 * logic (geofence, shift gate, config jam, status telat) satu-satunya tempat —
 * di database — supaya tidak ada lagi duplikasi query yang bisa drift dari web
 * (ini akar bug `outlets.radius_m` sebelumnya: reimplementasi manual di Kotlin
 * yang schema-nya beda dari yang dipakai web).
 */
object SubmitAttendanceUseCase {
    suspend operator fun invoke(entity: PendingAttendanceEntity): SubmitAttendanceResult = withContext(Dispatchers.IO) {
        try {
            val payloadObj = com.google.gson.JsonObject().apply {
                addProperty("id", entity.id)
                addProperty("outlet_id", entity.outletId)
                addProperty("type", entity.type)
                addProperty("ts_client", entity.tsClientIso)
                entity.gpsLat?.let { addProperty("gps_lat", it) }
                entity.gpsLng?.let { addProperty("gps_lng", it) }
                entity.gpsAccuracy?.let { addProperty("gps_accuracy", it) }
                entity.selfiePath?.let { addProperty("selfie_path", it) }
                addProperty("is_manual_button", entity.isManualButton)
            }
            val rpcBody = com.google.gson.JsonObject().apply { add("payload", payloadObj) }

            val res = Postgrest.rpc("submit_attendance", rpcBody).asJsonObject
            val ok = res.get("ok")?.takeIf { !it.isJsonNull }?.asBoolean ?: false
            val reason = res.optString("reason")
            val status = res.optString("status")
            val tsServer = res.optString("ts_server")
            val attendanceId = res.optString("attendance_id")

            if (ok) {
                SubmitAttendanceResult(ok = true, status = status, reason = status, message = null, tsServerIso = tsServer, attendanceId = attendanceId ?: entity.id)
            } else {
                SubmitAttendanceResult(ok = false, status = null, reason = reason ?: "internal_error", message = null, tsServerIso = null, attendanceId = null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            SubmitAttendanceResult(ok = false, status = null, reason = "internal_error", message = e.message, tsServerIso = null, attendanceId = null)
        }
    }
}
