package com.sukashawarma.superapp.domain.usecase

import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.optJsonArray
import com.sukashawarma.superapp.data.remote.optString
import com.sukashawarma.superapp.domain.util.JakartaTime
import com.sukashawarma.superapp.feature.absensi.shift.ShiftConfig
import com.sukashawarma.superapp.feature.absensi.shift.ShiftOption
import com.sukashawarma.superapp.feature.absensi.shift.shiftOptions

enum class NextAction { IN, OUT, DONE }

private const val GLOBAL_OUTLET_ID = "00000000-0000-0000-0000-000000000000"

/**
 * Cermin gate clock-out di useClockKiosk.ts (web).
 *
 * Gate di client ini hanya untuk memberi feedback lebih awal dan pesan yang jelas.
 * RPC `submit_attendance` tetap menjadi penjaga otoritatif saat data berubah di
 * antara pengecekan client dan submit.
 */
object AttendanceGates {

    private val UNFINISHED_ORDER_STATUSES = setOf("pending", "preparing", "ready")

    suspend fun decideAction(
        staffId: String,
        pendingDao: com.sukashawarma.superapp.data.local.dao.PendingAttendanceDao? = null
    ): NextAction {
        val pendingLatest = try {
            pendingDao?.getLatestForStaff(staffId)
        } catch (_: Exception) {
            null
        }

        return try {
            // Cek riwayat absensi staf dalam 18 jam terakhir untuk mendukung shift malam yang melewati tengah malam.
            val cutoffIso = java.time.Instant.now().minus(18, java.time.temporal.ChronoUnit.HOURS).toString()
            val recentRows = Postgrest.select(
                "attendance",
                listOf(
                    "outlet_staff_id" to "eq.$staffId",
                    "ts_server" to "gte.$cutoffIso",
                    "select" to "type,status,ts_server",
                    "order" to "ts_server.desc",
                    "limit" to "5",
                )
            )
            val latest = recentRows.firstOrNull()?.asJsonObject
            val serverTsMs = latest?.optString("ts_server")?.let {
                runCatching { java.time.Instant.parse(it).toEpochMilli() }.getOrNull()
            } ?: 0L

            // Jika antrean lokal belum tersinkronisasi dan lebih baru daripada data server,
            // hormati status lokal agar user tidak diminta absen masuk ulang saat online pulih.
            if (pendingLatest != null && pendingLatest.createdAtMs > serverTsMs) {
                return when (pendingLatest.type) {
                    "in" -> NextAction.OUT
                    "out" -> NextAction.DONE
                    else -> NextAction.IN
                }
            }

            val latestType = latest?.optString("type")

            // Jika absen terakhir dalam 18 jam adalah 'in', maka aksi berikutnya adalah 'out' (absen pulang).
            if (latestType == "in") {
                return NextAction.OUT
            }

            val todayStart = JakartaTime.todayStartIso()
            val todayEnd = JakartaTime.todayEndIso()
            val todayRows = recentRows.filter {
                val ts = it.asJsonObject.optString("ts_server") ?: ""
                ts >= todayStart && ts <= todayEnd
            }
            val hasInToday = todayRows.any { it.asJsonObject.optString("type") == "in" }
            val hasOutToday = todayRows.any { it.asJsonObject.optString("type") == "out" }

            when {
                hasInToday && hasOutToday -> NextAction.DONE
                !hasInToday -> NextAction.IN
                else -> NextAction.OUT
            }
        } catch (_: Exception) {
            // Fallback offline: periksa antrean lokal Room
            when (pendingLatest?.type) {
                "in" -> NextAction.OUT
                "out" -> NextAction.DONE
                else -> NextAction.IN
            }
        }
    }

    /**
     * Pilihan shift outlet, atau null bila outlet satu shift. Dibaca lewat RPC karena RLS
     * `oac_read_own_outlet` hanya membuka config outlet utama, sementara leader dan kru
     * multi-outlet bisa absen di outlet lain. Melempar exception bila gagal dimuat.
     */
    suspend fun loadShiftOptions(outletId: String, role: String? = null): List<ShiftOption>? {
        val body = com.google.gson.JsonObject().apply { addProperty("p_outlet_id", outletId) }
        val cfg = Postgrest.rpc("attendance_shift_config", body).takeIf { it.isJsonObject }?.asJsonObject
            ?: return null
        return shiftOptions(
            ShiftConfig(
                jamMasuk = cfg.optString("jam_masuk"),
                jamKeluar = cfg.optString("jam_keluar"),
                pilihShiftAktif = cfg.get("pilih_shift_aktif")?.takeIf { !it.isJsonNull }?.asBoolean,
                shift2JamMasuk = cfg.optString("shift2_jam_masuk"),
                shift2JamKeluar = cfg.optString("shift2_jam_keluar"),
            ),
            role = role,
        )
    }

    /** Jam pulang shift dari absen masuk TERBARU staff (bukan alpha, ≤ 20 jam) — sama dengan
     *  jendela yang dipakai server untuk membaca jejak shift. Null = tanpa jejak shift. */
    suspend fun latestInShiftJamKeluar(staffId: String): String? {
        val cutoffIso = java.time.Instant.now().minus(20, java.time.temporal.ChronoUnit.HOURS).toString()
        return Postgrest.selectOne(
            "attendance",
            listOf(
                "outlet_staff_id" to "eq.$staffId",
                "type" to "eq.in",
                "status" to "neq.alpha",
                "ts_server" to "gte.$cutoffIso",
                "select" to "shift_jam_keluar",
                "order" to "ts_server.desc",
                "limit" to "1",
            ),
        )?.optString("shift_jam_keluar")
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

    /**
     * True bila POS Native masih memiliki order berjalan di outlet ini.
     *
     * `cancellation_status = approved` diperlakukan sebagai batal, walaupun
     * beberapa row lama masih menyimpan `status` pending/preparing/ready.
     */
    suspend fun hasUnfinishedOrders(outletId: String): Boolean {
        val rows = Postgrest.select(
            "orders",
            listOf(
                "outlet_id" to "eq.$outletId",
                "status" to "in.(${UNFINISHED_ORDER_STATUSES.joinToString(",")})",
                "select" to "status,cancellation_status",
                // Satu outlet normalnya jauh di bawah angka ini; tetap batasi
                // agar gate tidak menarik histori order yang tidak relevan.
                "limit" to "1000",
            ),
        )
        return rows.any { row ->
            val obj = row.asJsonObject
            val status = obj.optString("status")?.lowercase()
            val cancellationStatus = obj.optString("cancellation_status")?.lowercase()
            status != null && status in UNFINISHED_ORDER_STATUSES && cancellationStatus != "approved"
        }
    }
}
