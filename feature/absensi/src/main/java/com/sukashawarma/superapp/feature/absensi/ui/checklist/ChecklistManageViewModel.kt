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

data class ManageChecklistItem(
    val id: String,
    val name: String,
    val phase: ChecklistPhase,
    val isRequired: Boolean,
    val categoryId: String,
    val categoryName: String = "",
)

data class ChecklistManageUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val items: List<ManageChecklistItem> = emptyList(),
    val saving: Boolean = false,
)

/**
 * CRUD checklist operasional harian outlet (Langsung item per fase Buka/Tutup tanpa kerumitan kategori).
 */
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
                val allItems = mutableListOf<ManageChecklistItem>()
                cats.forEach { catEl ->
                    val cat = catEl.asJsonObject
                    val catId = cat.optString("id") ?: ""
                    val catName = cat.optString("name") ?: ""
                    val phaseVal = cat.optString("phase")
                    val phase = ChecklistPhase.entries.find { it.value == phaseVal } ?: ChecklistPhase.BUKA
                    val items = cat.optJsonArray("checklist_items")?.map { itemEl ->
                        val item = itemEl.asJsonObject
                        ManageChecklistItem(
                            id = item.optString("id") ?: "",
                            name = item.optString("task_name") ?: "-",
                            phase = phase,
                            isRequired = item.optBoolean("is_required"),
                            categoryId = catId,
                            categoryName = catName,
                        )
                    } ?: emptyList()
                    allItems.addAll(items)
                }
                _state.value = ChecklistManageUiState(loading = false, items = allItems)
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = "Gagal memuat checklist: ${e.message}")
            }
        }
    }

    private suspend fun getOrCreateCategoryId(outletId: String, phase: ChecklistPhase): String {
        val existing = Postgrest.select(
            "checklist_categories",
            listOf(
                "outlet_id" to "eq.$outletId",
                "phase" to "eq.${phase.value}",
                "select" to "id,name",
                "limit" to "1"
            )
        )
        if (existing.size() > 0) {
            val catId = existing[0].asJsonObject.optString("id")
            if (!catId.isNullOrBlank()) return catId
        }
        // Buat kategori default untuk fase ini
        val defaultName = if (phase == ChecklistPhase.BUKA) "Checklist Buka" else "Checklist Tutup"
        val inserted = Postgrest.insert(
            "checklist_categories",
            JsonObject().apply {
                addProperty("outlet_id", outletId)
                addProperty("name", defaultName)
                addProperty("phase", phase.value)
            }
        )
        return if (inserted.size() > 0) inserted[0].asJsonObject.optString("id") ?: "" else ""
    }

    fun addChecklist(name: String, phase: ChecklistPhase, isRequired: Boolean) {
        val outletId = AppSession.staff.value?.outletId ?: return
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                val categoryId = getOrCreateCategoryId(outletId, phase)
                Postgrest.insert(
                    "checklist_items",
                    JsonObject().apply {
                        addProperty("category_id", categoryId)
                        addProperty("task_name", name.trim())
                        addProperty("is_required", isRequired)
                    }
                )
                load()
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Gagal menambah checklist: ${e.message}")
            }
        }
    }

    fun updateChecklist(item: ManageChecklistItem, newName: String, newPhase: ChecklistPhase, newIsRequired: Boolean) {
        val outletId = AppSession.staff.value?.outletId ?: return
        if (newName.isBlank()) return
        viewModelScope.launch {
            try {
                val targetCategoryId = if (newPhase != item.phase) {
                    getOrCreateCategoryId(outletId, newPhase)
                } else {
                    item.categoryId
                }
                Postgrest.update(
                    "checklist_items",
                    listOf("id" to "eq.${item.id}"),
                    JsonObject().apply {
                        addProperty("task_name", newName.trim())
                        addProperty("is_required", newIsRequired)
                        addProperty("category_id", targetCategoryId)
                    }
                )
                load()
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Gagal mengubah checklist: ${e.message}")
            }
        }
    }

    fun deleteChecklist(itemId: String) {
        viewModelScope.launch {
            try {
                Postgrest.delete("checklist_items", listOf("id" to "eq.$itemId"))
                load()
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Gagal menghapus checklist: ${e.message}")
            }
        }
    }
}


