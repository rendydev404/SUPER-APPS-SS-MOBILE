package com.sukashawarma.superapp.presentation.absensi.checklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonObject
import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.optBoolean
import com.sukashawarma.superapp.data.remote.optJsonArray
import com.sukashawarma.superapp.data.remote.optString
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.domain.util.JakartaTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ChecklistItemUi(val id: String, val name: String, val isRequired: Boolean, val ticked: Boolean)
data class ChecklistCategoryUi(val id: String, val name: String, val items: List<ChecklistItemUi>)

enum class ChecklistPhase(val value: String, val label: String) { BUKA("buka", "Buka"), TUTUP("tutup", "Tutup") }

data class ChecklistUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val phase: ChecklistPhase = ChecklistPhase.BUKA,
    val categories: List<ChecklistCategoryUi> = emptyList(),
    val saving: Boolean = false,
)

private const val GLOBAL_OUTLET_ID = "00000000-0000-0000-0000-000000000000"

/** Isi checklist harian buka/tutup outlet — pakai tabel yang sama seperti gate di
 *  [[AttendanceGates]] (checklist_categories/checklist_items/daily_checklist_records/
 *  daily_checklist_ticks), supaya centang di sini langsung memenuhi gate absen pulang. */
class ChecklistViewModel : ViewModel() {
    private val _state = MutableStateFlow(ChecklistUiState())
    val state: StateFlow<ChecklistUiState> = _state

    private var recordId: String? = null

    init { load() }

    fun setPhase(phase: ChecklistPhase) {
        if (_state.value.phase == phase) return
        _state.value = _state.value.copy(phase = phase, categories = emptyList(), error = null)
        load()
    }

    fun load() {
        val outletId = AppSession.staff.value?.outletId
        if (outletId == null) {
            _state.value = _state.value.copy(loading = false, error = "Akun tidak terhubung ke outlet.")
            return
        }
        _state.value = _state.value.copy(loading = true, error = null)
        val phase = _state.value.phase
        viewModelScope.launch {
            try {
                val cats = Postgrest.select(
                    "checklist_categories",
                    listOf(
                        "outlet_id" to "in.($outletId,$GLOBAL_OUTLET_ID)",
                        "phase" to "eq.${phase.value}",
                        "select" to "id,name,checklist_items(id,task_name,is_required)",
                        "order" to "name.asc",
                    )
                )

                val record = Postgrest.selectOne(
                    "daily_checklist_records",
                    listOf(
                        "outlet_id" to "eq.$outletId",
                        "date" to "eq.${JakartaTime.todayDateStr()}",
                        "select" to "id",
                    )
                )
                recordId = record?.optString("id")
                val ticked = recordId?.let { rid ->
                    Postgrest.select("daily_checklist_ticks", listOf("record_id" to "eq.$rid", "select" to "item_id"))
                        .map { it.asJsonObject.optString("item_id") }
                        .toSet()
                } ?: emptySet()

                val categories = cats.map { catEl ->
                    val cat = catEl.asJsonObject
                    val items = cat.optJsonArray("checklist_items")?.map { itemEl ->
                        val item = itemEl.asJsonObject
                        val id = item.optString("id") ?: ""
                        ChecklistItemUi(
                            id = id,
                            name = item.optString("task_name") ?: "-",
                            isRequired = item.optBoolean("is_required"),
                            ticked = id in ticked,
                        )
                    } ?: emptyList()
                    ChecklistCategoryUi(id = cat.optString("id") ?: "", name = cat.optString("name") ?: "-", items = items)
                }
                _state.value = _state.value.copy(loading = false, categories = categories)
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = "Gagal memuat checklist: ${e.message}")
            }
        }
    }

    fun toggleItem(itemId: String, checked: Boolean) {
        val outletId = AppSession.staff.value?.outletId ?: return
        val staffId = AppSession.staff.value?.id ?: return

        _state.value = _state.value.copy(
            categories = _state.value.categories.map { cat ->
                cat.copy(items = cat.items.map { if (it.id == itemId) it.copy(ticked = checked) else it })
            }
        )

        viewModelScope.launch {
            try {
                val rid = recordId ?: ensureRecord(outletId)
                if (checked) {
                    Postgrest.upsert(
                        "daily_checklist_ticks",
                        JsonObject().apply {
                            addProperty("record_id", rid)
                            addProperty("item_id", itemId)
                            addProperty("ticked_by", staffId)
                        },
                        onConflict = "record_id,item_id",
                    )
                } else {
                    Postgrest.delete("daily_checklist_ticks", listOf("record_id" to "eq.$rid", "item_id" to "eq.$itemId"))
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Gagal menyimpan centang: ${e.message}")
            }
        }
    }

    private suspend fun ensureRecord(outletId: String): String {
        recordId?.let { return it }
        val existing = Postgrest.selectOne(
            "daily_checklist_records",
            listOf("outlet_id" to "eq.$outletId", "date" to "eq.${JakartaTime.todayDateStr()}", "select" to "id")
        )
        if (existing != null) {
            val id = existing.optString("id")!!
            recordId = id
            return id
        }
        val created = Postgrest.insert(
            "daily_checklist_records",
            JsonObject().apply {
                addProperty("outlet_id", outletId)
                addProperty("date", JakartaTime.todayDateStr())
            }
        )
        val id = created[0].asJsonObject.optString("id")!!
        recordId = id
        return id
    }
}
