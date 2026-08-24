package com.sukashawarma.superapp.presentation.absensi.checklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonObject
import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.optBoolean
import com.sukashawarma.superapp.data.remote.optJsonArray
import com.sukashawarma.superapp.data.remote.optString
import com.sukashawarma.superapp.domain.session.AppSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ManageItem(val id: String, val name: String, val isRequired: Boolean)
data class ManageCategory(val id: String, val name: String, val phase: ChecklistPhase, val items: List<ManageItem>)

data class ChecklistManageUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val categories: List<ManageCategory> = emptyList(),
    val saving: Boolean = false,
)

/** CRUD kategori & item checklist milik outlet sendiri (bukan item global lintas outlet
 *  `outlet_id = GLOBAL`, itu di luar wewenang SPV outlet — sengaja tak ditampilkan di sini). */
class ChecklistManageViewModel : ViewModel() {
    private val _state = MutableStateFlow(ChecklistManageUiState())
    val state: StateFlow<ChecklistManageUiState> = _state

    init { load() }

    fun load() {
        val outletId = AppSession.staff.value?.outletId
        if (outletId == null) {
            _state.value = ChecklistManageUiState(loading = false, error = "Akun tidak terhubung ke outlet.")
            return
        }
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                val cats = Postgrest.select(
                    "checklist_categories",
                    listOf(
                        "outlet_id" to "eq.$outletId",
                        "select" to "id,name,phase,checklist_items(id,task_name,is_required)",
                        "order" to "phase.asc,name.asc",
                    )
                )
                val categories = cats.map { catEl ->
                    val cat = catEl.asJsonObject
                    val phaseVal = cat.optString("phase")
                    val items = cat.optJsonArray("checklist_items")?.map { itemEl ->
                        val item = itemEl.asJsonObject
                        ManageItem(item.optString("id") ?: "", item.optString("task_name") ?: "-", item.optBoolean("is_required"))
                    } ?: emptyList()
                    ManageCategory(
                        id = cat.optString("id") ?: "",
                        name = cat.optString("name") ?: "-",
                        phase = ChecklistPhase.entries.find { it.value == phaseVal } ?: ChecklistPhase.BUKA,
                        items = items,
                    )
                }
                _state.value = ChecklistManageUiState(loading = false, categories = categories)
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = "Gagal memuat: ${e.message}")
            }
        }
    }

    fun addCategory(name: String, phase: ChecklistPhase) {
        val outletId = AppSession.staff.value?.outletId ?: return
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                Postgrest.insert(
                    "checklist_categories",
                    JsonObject().apply {
                        addProperty("outlet_id", outletId)
                        addProperty("name", name)
                        addProperty("phase", phase.value)
                    }
                )
                load()
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Gagal menambah kategori: ${e.message}")
            }
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            try {
                Postgrest.delete("checklist_categories", listOf("id" to "eq.$categoryId"))
                load()
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Gagal menghapus kategori: ${e.message}")
            }
        }
    }

    fun addItem(categoryId: String, name: String, isRequired: Boolean) {
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                Postgrest.insert(
                    "checklist_items",
                    JsonObject().apply {
                        addProperty("category_id", categoryId)
                        addProperty("task_name", name)
                        addProperty("is_required", isRequired)
                    }
                )
                load()
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Gagal menambah item: ${e.message}")
            }
        }
    }

    fun deleteItem(itemId: String) {
        viewModelScope.launch {
            try {
                Postgrest.delete("checklist_items", listOf("id" to "eq.$itemId"))
                load()
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Gagal menghapus item: ${e.message}")
            }
        }
    }
}
