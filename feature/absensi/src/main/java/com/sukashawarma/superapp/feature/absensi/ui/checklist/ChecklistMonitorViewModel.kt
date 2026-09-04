package com.sukashawarma.superapp.presentation.absensi.checklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.optBoolean
import com.sukashawarma.superapp.data.remote.optJsonArray
import com.sukashawarma.superapp.data.remote.optString
import com.sukashawarma.superapp.domain.model.Role
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.domain.util.JakartaTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

data class MonitorOutletOption(val id: String, val name: String)

data class MonitorItem(
    val id: String,
    val name: String,
    val isRequired: Boolean,
    val tickedBy: String? = null,
    val tickedAt: String? = null,
) {
    val ticked: Boolean get() = tickedBy != null
}

data class MonitorCategory(
    val id: String,
    val name: String,
    val phase: ChecklistPhase,
    val items: List<MonitorItem>,
) {
    val tickedCount: Int get() = items.count { it.ticked }
    val requiredTotal: Int get() = items.count { it.isRequired }
    val requiredDone: Int get() = items.count { it.isRequired && it.ticked }
    val allDone: Boolean get() = items.isNotEmpty() && tickedCount == items.size
}

/** Angka ringkasan satu fase — cerminan blok perhitungan di halaman web
 *  `dashboard/checklist-monitor`. */
data class PhaseSummary(
    val totalItems: Int = 0,
    val totalRequired: Int = 0,
    val tickedItems: Int = 0,
    val tickedRequired: Int = 0,
) {
    val progress: Int get() = if (totalItems > 0) Math.round(tickedItems * 100f / totalItems) else 0
    val allRequiredDone: Boolean get() = totalRequired == 0 || tickedRequired == totalRequired
}

data class ChecklistMonitorUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
    val phase: ChecklistPhase = ChecklistPhase.BUKA,
    val categories: List<MonitorCategory> = emptyList(),
    val lastRefresh: String = "",
    /** Regional manager memantau semua outlet, jadi outlet dipilih dulu baru data dimuat. */
    val canChooseOutlet: Boolean = false,
    val loadingOutlets: Boolean = false,
    val outlets: List<MonitorOutletOption> = emptyList(),
    val selectedOutletId: String? = null,
    val selectedOutletName: String? = null,
    val date: LocalDate = LocalDate.now(JakartaTime.ZONE),
) {
    val awaitingOutletChoice: Boolean get() = canChooseOutlet && selectedOutletId == null
    val isToday: Boolean get() = date == LocalDate.now(JakartaTime.ZONE)

    fun categoriesOf(phase: ChecklistPhase): List<MonitorCategory> = categories.filter { it.phase == phase }

    fun summaryOf(phase: ChecklistPhase): PhaseSummary {
        val cats = categoriesOf(phase)
        return PhaseSummary(
            totalItems = cats.sumOf { it.items.size },
            totalRequired = cats.sumOf { it.requiredTotal },
            tickedItems = cats.sumOf { it.tickedCount },
            tickedRequired = cats.sumOf { it.requiredDone },
        )
    }
}

private const val GLOBAL_OUTLET_ID = "00000000-0000-0000-0000-000000000000"

/** Pantau progres checklist harian outlet (buka & tutup) berikut siapa yang mencentang
 *  dan jam berapa — versi native dari halaman web `dashboard/checklist-monitor`.
 *  Membaca tabel yang sama dengan [[ChecklistViewModel]] dan gate [[AttendanceGates]],
 *  jadi angkanya selalu sinkron dengan syarat absen pulang.
 *
 *  Regional manager memantau seluruh outlet: daftar outlet dimuat dulu dan data baru
 *  ditarik setelah satu outlet dipilih — pola yang sama dipakai [[EnrollViewModel]].
 *  Tanggal bisa digeser ke hari-hari sebelumnya lewat filter waktu di layar. */
class ChecklistMonitorViewModel : ViewModel() {
    private val _state = MutableStateFlow(ChecklistMonitorUiState())
    val state: StateFlow<ChecklistMonitorUiState> = _state

    /** Tab awal ditentukan sekali per outlet/tanggal, setelah datanya masuk (mengikuti web). */
    private var initialPhaseResolved = false

    init {
        val staff = AppSession.staff.value
        if (staff?.role == Role.REGIONAL_MANAGER) {
            _state.value = _state.value.copy(canChooseOutlet = true, loading = false)
            loadOutlets()
        } else {
            _state.value = _state.value.copy(
                selectedOutletId = staff?.outletId,
                selectedOutletName = staff?.outletName,
            )
            load()
        }
    }

    /** Daftar outlet tidak dibatasi di client — RLS backend yang menentukan outlet mana
     *  yang benar-benar boleh dilihat Regional Manager (sama seperti [[EnrollViewModel]]). */
    private fun loadOutlets() {
        _state.value = _state.value.copy(loadingOutlets = true, error = null)
        viewModelScope.launch {
            try {
                val rows = Postgrest.select(
                    "outlets",
                    listOf(
                        "is_active" to "eq.true",
                        "id" to "neq.$GLOBAL_OUTLET_ID",
                        "select" to "id,name",
                        "order" to "name.asc",
                    )
                )
                val outlets = rows.map { el ->
                    val o = el.asJsonObject
                    MonitorOutletOption(id = o.optString("id") ?: "", name = o.optString("name") ?: "-")
                }
                _state.value = _state.value.copy(loadingOutlets = false, outlets = outlets)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loadingOutlets = false,
                    error = "Gagal memuat daftar outlet: ${e.message}",
                )
            }
        }
    }

    fun selectOutlet(outletId: String) {
        if (_state.value.selectedOutletId == outletId) return
        initialPhaseResolved = false
        _state.value = _state.value.copy(
            selectedOutletId = outletId,
            selectedOutletName = _state.value.outlets.find { it.id == outletId }?.name,
            categories = emptyList(),
            error = null,
        )
        load()
    }

    fun setDate(date: LocalDate) {
        val today = LocalDate.now(JakartaTime.ZONE)
        val clamped = if (date.isAfter(today)) today else date
        if (_state.value.date == clamped) return
        initialPhaseResolved = false
        _state.value = _state.value.copy(date = clamped, categories = emptyList(), error = null)
        if (_state.value.selectedOutletId != null) load()
    }

    fun setPhase(phase: ChecklistPhase) {
        if (_state.value.phase != phase) _state.value = _state.value.copy(phase = phase)
    }

    fun refresh() = load(silent = true)

    fun load(silent: Boolean = false) {
        val outletId = _state.value.selectedOutletId
        if (outletId == null) {
            // Regional manager memang belum memilih outlet — bukan error, cukup diam.
            if (_state.value.canChooseOutlet) {
                _state.value = _state.value.copy(loading = false, refreshing = false)
                return
            }
            _state.value = _state.value.copy(
                loading = false,
                refreshing = false,
                error = "Akun Anda belum terhubung dengan cabang manapun. Hubungi admin untuk pengaturan penempatan.",
            )
            return
        }
        _state.value = _state.value.copy(loading = !silent, refreshing = silent, error = null)
        val date = _state.value.date
        viewModelScope.launch {
            try {
                val cats = Postgrest.select(
                    "checklist_categories",
                    listOf(
                        "outlet_id" to "in.($outletId,$GLOBAL_OUTLET_ID)",
                        "select" to "id,name,phase,checklist_items(id,task_name,is_required)",
                        "order" to "created_at.asc",
                    )
                )

                val staffNames = loadStaffNames(outletId)
                val ticks = loadTicks(outletId, date)

                val categories = cats.map { catEl ->
                    val cat = catEl.asJsonObject
                    // Web memperlakukan phase null sebagai "buka"; ikuti supaya hitungannya sama.
                    val phase = if (cat.optString("phase") == ChecklistPhase.TUTUP.value) {
                        ChecklistPhase.TUTUP
                    } else {
                        ChecklistPhase.BUKA
                    }
                    val items = cat.optJsonArray("checklist_items")?.map { itemEl ->
                        val item = itemEl.asJsonObject
                        val id = item.optString("id") ?: ""
                        val tick = ticks[id]
                        MonitorItem(
                            id = id,
                            name = item.optString("task_name") ?: "-",
                            isRequired = item.optBoolean("is_required"),
                            tickedBy = tick?.let { staffNames[it.first] ?: "Staf" },
                            tickedAt = tick?.second,
                        )
                    } ?: emptyList()
                    MonitorCategory(
                        id = cat.optString("id") ?: "",
                        name = cat.optString("name") ?: "-",
                        phase = phase,
                        items = items,
                    )
                }

                val next = _state.value.copy(
                    loading = false,
                    refreshing = false,
                    error = null,
                    categories = categories,
                    lastRefresh = JakartaTime.now().format(CLOCK_FMT),
                )
                _state.value = if (initialPhaseResolved || categories.isEmpty()) {
                    next
                } else {
                    initialPhaseResolved = true
                    next.copy(phase = resolveInitialPhase(next))
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    refreshing = false,
                    error = "Gagal memuat monitor: ${e.message}",
                )
            }
        }
    }

    /** Checklist buka sudah kelar (atau hari ini sudah lewat sore) → langsung tampilkan fase tutup. */
    private fun resolveInitialPhase(state: ChecklistMonitorUiState): ChecklistPhase {
        val buka = state.summaryOf(ChecklistPhase.BUKA)
        val bukaComplete = buka.totalItems > 0 && buka.tickedItems == buka.totalItems
        val sudahSore = state.isToday && JakartaTime.now().hour >= 15
        return if (bukaComplete || sudahSore) ChecklistPhase.TUTUP else ChecklistPhase.BUKA
    }

    /** item_id -> (ticked_by, jam WIB) */
    private suspend fun loadTicks(outletId: String, date: LocalDate): Map<String, Pair<String, String>> {
        val record = Postgrest.selectOne(
            "daily_checklist_records",
            listOf(
                "outlet_id" to "eq.$outletId",
                "date" to "eq.$date",
                "select" to "id",
            )
        ) ?: return emptyMap()
        val recordId = record.optString("id") ?: return emptyMap()

        return Postgrest.select(
            "daily_checklist_ticks",
            listOf("record_id" to "eq.$recordId", "select" to "item_id,ticked_by,ticked_at")
        ).mapNotNull { el ->
            val row = el.asJsonObject
            val itemId = row.optString("item_id") ?: return@mapNotNull null
            itemId to ((row.optString("ticked_by") ?: "") to formatJakartaClock(row.optString("ticked_at")))
        }.toMap()
    }

    /** Nama staf outlet ini: penempatan utama (`outlet_staff`) digabung penempatan
     *  tambahan (`staff_outlets`), persis seperti web. */
    private suspend fun loadStaffNames(outletId: String): Map<String, String> {
        val names = mutableMapOf<String, String>()
        Postgrest.select("outlet_staff", listOf("outlet_id" to "eq.$outletId", "select" to "id,name"))
            .forEach { el ->
                val row = el.asJsonObject
                val id = row.optString("id") ?: return@forEach
                names[id] = row.optString("name") ?: "Staf"
            }
        runCatching {
            Postgrest.select(
                "staff_outlets",
                listOf("outlet_id" to "eq.$outletId", "select" to "staff_id,outlet_staff!inner(id,name)")
            ).forEach { el ->
                val staff = el.asJsonObject.getAsJsonObject("outlet_staff") ?: return@forEach
                val id = staff.optString("id") ?: return@forEach
                if (id !in names) names[id] = staff.optString("name") ?: "Staf"
            }
        }
        return names
    }

    private companion object {
        val CLOCK_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        val SHORT_CLOCK_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        fun formatJakartaClock(iso: String?): String {
            if (iso.isNullOrBlank()) return "-"
            return runCatching {
                OffsetDateTime.parse(iso).atZoneSameInstant(JakartaTime.ZONE).format(SHORT_CLOCK_FMT)
            }.getOrElse { "-" }
        }
    }
}
