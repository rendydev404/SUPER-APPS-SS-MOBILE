package com.sukashawarma.superapp.presentation.absensi.checklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.optBoolean
import com.sukashawarma.superapp.data.remote.optJsonArray
import com.sukashawarma.superapp.data.remote.optString
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.domain.util.JakartaTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ChecklistPhaseProgress(val phase: ChecklistPhase, val done: Int, val total: Int, val requiredDone: Boolean)

data class ChecklistMonitorUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val progress: List<ChecklistPhaseProgress> = emptyList(),
)

private const val GLOBAL_OUTLET_ID = "00000000-0000-0000-0000-000000000000"

/** Ringkasan progres checklist hari ini (buka & tutup) untuk SPV/admin memantau
 *  outletnya — bacaan agregat dari tabel yang sama dipakai [[ChecklistViewModel]]
 *  dan gate [[AttendanceGates]]. */
class ChecklistMonitorViewModel : ViewModel() {
    private val _state = MutableStateFlow(ChecklistMonitorUiState())
    val state: StateFlow<ChecklistMonitorUiState> = _state

    init { load() }

    fun load() {
        val outletId = AppSession.staff.value?.outletId
        if (outletId == null) {
            _state.value = ChecklistMonitorUiState(loading = false, error = "Akun tidak terhubung ke outlet.")
            return
        }
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                val record = Postgrest.selectOne(
                    "daily_checklist_records",
                    listOf("outlet_id" to "eq.$outletId", "date" to "eq.${JakartaTime.todayDateStr()}", "select" to "id")
                )
                val ticked = record?.optString("id")?.let { rid ->
                    Postgrest.select("daily_checklist_ticks", listOf("record_id" to "eq.$rid", "select" to "item_id"))
                        .map { it.asJsonObject.optString("item_id") }
                        .toSet()
                } ?: emptySet()

                val progress = ChecklistPhase.entries.map { phase ->
                    val cats = Postgrest.select(
                        "checklist_categories",
                        listOf(
                            "outlet_id" to "in.($outletId,$GLOBAL_OUTLET_ID)",
                            "phase" to "eq.${phase.value}",
                            "select" to "id,checklist_items(id,is_required)",
                        )
                    )
                    val items = cats.flatMap { it.asJsonObject.optJsonArray("checklist_items")?.map { i -> i.asJsonObject } ?: emptyList() }
                    val requiredItems = items.filter { it.optBoolean("is_required") }
                    val doneCount = items.count { (it.optString("id") ?: "") in ticked }
                    val requiredDone = requiredItems.all { (it.optString("id") ?: "") in ticked }
                    ChecklistPhaseProgress(phase = phase, done = doneCount, total = items.size, requiredDone = requiredItems.isEmpty() || requiredDone)
                }
                _state.value = ChecklistMonitorUiState(loading = false, progress = progress)
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = "Gagal memuat monitor: ${e.message}")
            }
        }
    }
}
