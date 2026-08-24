package com.sukashawarma.superapp.presentation.absensi.rekap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.optString
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.domain.model.ADMIN_OR_HR_ROLES
import com.sukashawarma.superapp.domain.model.SPV_TIER_ROLES
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class AttendanceHistoryRow(
    val type: String,
    val status: String?,
    val tsServerIso: String,
    val staffName: String?,
)

data class RekapUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val rows: List<AttendanceHistoryRow> = emptyList(),
    val rangeDays: Int = 7,
    val isOutletWide: Boolean = false,
)

/** Riwayat absensi 7/30 hari terakhir — punya dua mode: staff biasa hanya lihat
 *  riwayat sendiri (outlet_staff_id = akun login), SPV-tier lihat seluruh outlet
 *  (cermin pembagian menu di [[AbsensiHubScreen]] antara list SPV vs crew). */
class RekapViewModel : ViewModel() {
    private val _state = MutableStateFlow(RekapUiState())
    val state: StateFlow<RekapUiState> = _state

    init {
        val staff = AppSession.staff.value
        val isSpvTier = staff?.role in SPV_TIER_ROLES || staff?.role in ADMIN_OR_HR_ROLES
        _state.value = _state.value.copy(isOutletWide = isSpvTier)
        load()
    }

    fun setRangeDays(days: Int) {
        _state.value = _state.value.copy(rangeDays = days)
        load()
    }

    fun load() {
        val staff = AppSession.staff.value
        if (staff == null) {
            _state.value = _state.value.copy(loading = false, error = "Sesi tidak valid.")
            return
        }
        _state.value = _state.value.copy(loading = true, error = null)
        val outletWide = _state.value.isOutletWide
        val rangeDays = _state.value.rangeDays
        viewModelScope.launch {
            try {
                val sinceIso = LocalDate.now(ZoneId.of("Asia/Jakarta"))
                    .minusDays(rangeDays.toLong() - 1)
                    .atStartOfDay(ZoneId.of("Asia/Jakarta"))
                    .toInstant()
                    .toString()

                val filters = mutableListOf(
                    "ts_server" to "gte.$sinceIso",
                    "order" to "ts_server.desc",
                    "limit" to "200",
                )
                if (outletWide) {
                    val outletId = staff.outletId
                    if (outletId != null) filters += "outlet_id" to "eq.$outletId"
                    // TIDAK di-embed `outlet_staff(name)` di sini — PostgREST butuh foreign
                    // key constraint yang terdaftar di schema cache-nya utk auto-join, dan
                    // antara attendance<->outlet_staff itu tidak ada (beda dgn attendance<->
                    // outlets yang FK-nya memang ada, dipakai di StaffRepository). Nama staff
                    // diambil terpisah di bawah lewat query kedua, di-gabung manual di client.
                    filters += "select" to "type,status,ts_server,outlet_staff_id"
                } else {
                    filters += "outlet_staff_id" to "eq.${staff.id}"
                    filters += "select" to "type,status,ts_server"
                }

                val rawRows = Postgrest.select("attendance", filters)

                val staffNameById: Map<String, String> = if (outletWide) {
                    val ids = rawRows.mapNotNull { it.asJsonObject.optString("outlet_staff_id") }.distinct()
                    if (ids.isEmpty()) emptyMap() else Postgrest.select(
                        "outlet_staff",
                        listOf("id" to "in.(${ids.joinToString(",")})", "select" to "id,name")
                    ).associate { el ->
                        val o = el.asJsonObject
                        (o.optString("id") ?: "") to (o.optString("name") ?: "")
                    }
                } else emptyMap()

                val rows = rawRows.map { el ->
                    val obj = el.asJsonObject
                    AttendanceHistoryRow(
                        type = obj.optString("type") ?: "",
                        status = obj.optString("status"),
                        tsServerIso = obj.optString("ts_server") ?: "",
                        staffName = obj.optString("outlet_staff_id")?.let { staffNameById[it] },
                    )
                }
                _state.value = _state.value.copy(loading = false, rows = rows)
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = "Gagal memuat riwayat: ${e.message}")
            }
        }
    }
}
