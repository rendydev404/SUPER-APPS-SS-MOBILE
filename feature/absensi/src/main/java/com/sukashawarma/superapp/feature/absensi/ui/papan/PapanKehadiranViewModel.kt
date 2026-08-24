package com.sukashawarma.superapp.presentation.absensi.papan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.optString
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.domain.util.JakartaTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class StaffAttendanceState { BELUM_ABSEN, SUDAH_MASUK, SUDAH_PULANG }

data class StaffBoardRow(
    val staffId: String,
    val name: String,
    val state: StaffAttendanceState,
    val inTime: String?,
    val outTime: String?,
)

data class PapanKehadiranUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val rows: List<StaffBoardRow> = emptyList(),
)

/** Papan status kehadiran hari ini untuk satu outlet — gabungan `outlet_staff` (daftar
 *  aktif) dengan `attendance` (rekaman hari ini), pakai tabel & kolom yang sama seperti
 *  yang sudah dipakai StaffRepository/AttendanceGates/HomeViewModel. */
class PapanKehadiranViewModel : ViewModel() {
    private val _state = MutableStateFlow(PapanKehadiranUiState())
    val state: StateFlow<PapanKehadiranUiState> = _state

    init { load() }

    fun load() {
        val outletId = AppSession.staff.value?.outletId
        if (outletId == null) {
            _state.value = PapanKehadiranUiState(loading = false, error = "Akun tidak terhubung ke outlet.")
            return
        }
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                val staffRows = Postgrest.select(
                    "outlet_staff",
                    listOf(
                        "outlet_id" to "eq.$outletId",
                        "status" to "eq.active",
                        "select" to "id,name",
                        "order" to "name.asc",
                    )
                )
                val attRows = Postgrest.select(
                    "attendance",
                    listOf(
                        "outlet_id" to "eq.$outletId",
                        "ts_server" to "gte.${JakartaTime.todayStartIso()}",
                        "ts_server" to "lte.${JakartaTime.todayEndIso()}",
                        "select" to "outlet_staff_id,type,ts_server",
                        "order" to "ts_server.asc",
                    )
                )
                val byStaff = attRows.groupBy { it.asJsonObject.optString("outlet_staff_id") }

                val rows = staffRows.map { s ->
                    val obj = s.asJsonObject
                    val id = obj.optString("id") ?: ""
                    val name = obj.optString("name") ?: "-"
                    val events = byStaff[id].orEmpty().map { it.asJsonObject }
                    val inEvt = events.firstOrNull { it.optString("type") == "in" }
                    val outEvt = events.firstOrNull { it.optString("type") == "out" }
                    val state = when {
                        outEvt != null -> StaffAttendanceState.SUDAH_PULANG
                        inEvt != null -> StaffAttendanceState.SUDAH_MASUK
                        else -> StaffAttendanceState.BELUM_ABSEN
                    }
                    StaffBoardRow(
                        staffId = id,
                        name = name,
                        state = state,
                        inTime = inEvt?.optString("ts_server"),
                        outTime = outEvt?.optString("ts_server"),
                    )
                }
                _state.value = PapanKehadiranUiState(loading = false, rows = rows)
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = "Gagal memuat data: ${e.message}")
            }
        }
    }
}
