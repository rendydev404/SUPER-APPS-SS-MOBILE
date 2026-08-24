package com.sukashawarma.superapp.domain.usecase

import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.optJsonArray
import com.sukashawarma.superapp.data.remote.optString
import com.sukashawarma.superapp.domain.util.JakartaTime

enum class NextAction { IN, OUT, DONE }

private const val GLOBAL_OUTLET_ID = "00000000-0000-0000-0000-000000000000"

/**
 * Cermin `decideAction`/`isClosingChecklistDone`/`isShiftClosed` di useClockKiosk.ts
 * (web). Gate "unfinished_orders" SENGAJA tidak dicek di sini — itu server-only di web
 * (dicek ulang saat submit), jadi klien tidak perlu query tambahan yang hasilnya
 * dibuang begitu server tetap memutuskan.
 */
object AttendanceGates {

    suspend fun decideAction(staffId: String): NextAction {
        val rows = Postgrest.select(
            "attendance",
            listOf(
                "outlet_staff_id" to "eq.$staffId",
                "ts_server" to "gte.${JakartaTime.todayStartIso()}",
                "ts_server" to "lte.${JakartaTime.todayEndIso()}",
                "select" to "type,status",
                "order" to "ts_server.desc"
            )
        )
        val hasIn = rows.any { it.asJsonObject.optString("type") == "in" }
        val hasOut = rows.any { it.asJsonObject.optString("type") == "out" }
        return when {
            !hasIn -> NextAction.IN
            !hasOut -> NextAction.OUT
            else -> NextAction.DONE
        }
    }

    /** True bila semua item wajib checklist fase "tutup" sudah dicentang hari ini,
     *  ATAU belum ada checklist tutup sama sekali (tidak menghalangi). */
    suspend fun isClosingChecklistDone(outletId: String): Boolean {
        val cats = Postgrest.select(
            "checklist_categories",
            listOf(
                "outlet_id" to "in.($outletId,$GLOBAL_OUTLET_ID)",
                "phase" to "eq.tutup",
                "select" to "id,checklist_items(id,is_required)"
            )
        )
        val requiredIds = cats.flatMap { cat ->
            cat.asJsonObject.optJsonArray("checklist_items")?.mapNotNull { item ->
                val obj = item.asJsonObject
                val required = obj.get("is_required")?.takeIf { !it.isJsonNull }?.asBoolean ?: false
                if (required) obj.optString("id") else null
            } ?: emptyList()
        }
        if (requiredIds.isEmpty()) return true

        val record = Postgrest.selectOne(
            "daily_checklist_records",
            listOf(
                "outlet_id" to "eq.$outletId",
                "date" to "eq.${JakartaTime.todayDateStr()}",
                "select" to "id"
            )
        ) ?: return false

        val recordId = record.optString("id") ?: return false
        val ticks = Postgrest.select("daily_checklist_ticks", listOf("record_id" to "eq.$recordId", "select" to "item_id"))
        val ticked = ticks.map { it.asJsonObject.optString("item_id") }.toSet()
        return requiredIds.all { it in ticked }
    }

    /** True bila TIDAK ada shift kasir 'open' di outlet ini — shift milik outlet,
     *  berlaku untuk siapa pun yang absen pulang, tanpa bypass (cermin web). */
    suspend fun isShiftClosed(outletId: String): Boolean {
        val open = Postgrest.selectOne("shifts", listOf("outlet_id" to "eq.$outletId", "status" to "eq.open", "select" to "id"))
        return open == null
    }
}
