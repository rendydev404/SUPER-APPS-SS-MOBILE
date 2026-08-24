package com.sukashawarma.superapp.data.repository

import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.optBoolean
import com.sukashawarma.superapp.data.remote.optJsonObject
import com.sukashawarma.superapp.data.remote.optString
import com.sukashawarma.superapp.domain.model.Role
import com.sukashawarma.superapp.domain.model.StaffProfile
import com.sukashawarma.superapp.domain.util.JakartaTime

/**
 * Cermin `getOutletStaff()` di packages/auth/src/staff.ts (web). Termasuk override
 * outlet_id dari lokasi absen hari ini (mekanisme BKO/mutasi harian) — tanpa ini
 * staff yang absen di outlet lain akan melihat data outlet yang salah di sini.
 */
object StaffRepository {

    suspend fun getOutletStaff(userId: String): StaffProfile? {
        val row = Postgrest.selectOne(
            "outlet_staff",
            listOf(
                "id" to "eq.$userId",
                "select" to "id,outlet_id,name,role,status,ref_photo_url,username,allow_manual_button,outlets!outlet_staff_outlet_id_fkey(name)"
            )
        ) ?: return null

        var outletId = row.optString("outlet_id")
        var outletName = row.optJsonObject("outlets")?.optString("name")

        val todayStart = JakartaTime.todayStartIso()
        val attRow = Postgrest.select(
            "attendance",
            listOf(
                "outlet_staff_id" to "eq.$userId",
                "created_at" to "gte.$todayStart",
                "select" to "outlet_id,outlets(name)",
                "order" to "created_at.desc",
                "limit" to "1"
            )
        ).let { if (it.size() > 0) it[0].asJsonObject else null }

        attRow?.optString("outlet_id")?.let { attOutletId ->
            outletId = attOutletId
            attRow.optJsonObject("outlets")?.optString("name")?.let { outletName = it }
        }

        return StaffProfile(
            id = row.optString("id") ?: userId,
            outletId = outletId,
            outletName = outletName,
            name = row.optString("name") ?: "",
            role = Role.from(row.optString("role")),
            roleRaw = row.optString("role") ?: "",
            status = row.optString("status") ?: "",
            username = row.optString("username"),
            refPhotoUrl = row.optString("ref_photo_url"),
            allowManualButton = row.optBoolean("allow_manual_button"),
            faceDescriptor = null,
        )
    }
}
