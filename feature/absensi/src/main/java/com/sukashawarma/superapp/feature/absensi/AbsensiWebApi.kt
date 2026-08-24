package com.sukashawarma.superapp.data.remote

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.sukashawarma.superapp.core.network.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

data class SubmitAttendanceResult(
    val ok: Boolean,
    val status: String?,
    val reason: String?,
    val message: String?,
    val tsServerIso: String?,
    val attendanceId: String?,
)

data class FaceMatchResult(
    val ok: Boolean,
    val staffId: String?,
    val name: String?,
    val similarity: Double,
    val bestSimilarity: Double,
    /** Descriptor kandidat yang cocok — dipakai client untuk verifikasi ulang di fase
     *  liveness (bukan dipersist). Web masih mengembalikan ini mentah (temuan A2, belum
     *  diperbaiki di backend) — jangan tambah pemakaian baru di luar yang sudah ada. */
    val descriptor: List<Float>?,
    val reason: String?,
)

/**
 * Panggilan ke route Next.js `apps/absensi` (BUKAN Edge Function) — kontrak endpoint
 * ini yang masih live hari ini. Rencana Fase 2 (task 2.4/2.6/2.7 di plan) memindahkan
 * ini ke Edge Function dengan verifikasi JWT (temuan A1/A2 belum diperbaiki di backend
 * saat kode ini ditulis) — begitu itu selesai, cuma base URL di sini yang berubah,
 * kontrak payload/response tetap sama.
 */
object AbsensiWebApi {
    private val jsonMedia = "application/json".toMediaType()
    private val client get() = SupabaseClient.okHttpClient
    private val baseUrl get() = BuildConfig.ABSENSI_WEB_URL.trimEnd('/')

    private suspend fun postJson(path: String, body: JsonObject): JsonObject = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("$baseUrl$path")
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
            .post(body.toString().toRequestBody(jsonMedia))
            .build()
        client.newCall(req).execute().use { resp ->
            val bodyStr = resp.body?.string().orEmpty()
            if (bodyStr.isBlank()) JsonObject() else JsonParser.parseString(bodyStr).asJsonObject
        }
    }

    suspend fun submitAttendance(payload: JsonObject): SubmitAttendanceResult {
        val res = postJson("/api/submit-attendance", payload)
        return SubmitAttendanceResult(
            ok = res.optBoolean("ok"),
            status = res.optString("status"),
            reason = res.optString("reason"),
            message = res.optString("message"),
            tsServerIso = res.optString("ts_server"),
            attendanceId = res.optString("attendance_id"),
        )
    }

    suspend fun faceMatch(descriptor: List<Float>, outletId: String, lockToStaffId: String?): FaceMatchResult {
        val body = JsonObject().apply {
            add("descriptor", JsonArray().apply { descriptor.forEach { add(it) } })
            addProperty("outletId", outletId)
            if (lockToStaffId != null) addProperty("lockToStaffId", lockToStaffId) else add("lockToStaffId", com.google.gson.JsonNull.INSTANCE)
        }
        val res = postJson("/api/face-match", body)
        val descArr = res.get("descriptor")?.takeIf { it.isJsonArray }?.asJsonArray
        return FaceMatchResult(
            ok = res.optBoolean("ok"),
            staffId = res.optString("staffId"),
            name = res.optString("name"),
            similarity = res.optDouble("similarity") ?: 0.0,
            bestSimilarity = res.optDouble("bestSimilarity") ?: 0.0,
            descriptor = descArr?.map { it.asFloat },
            reason = res.optString("reason"),
        )
    }
}

