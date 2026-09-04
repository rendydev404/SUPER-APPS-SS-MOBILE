package com.sukashawarma.superapp.presentation.absensi.papan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.optBoolean
import com.sukashawarma.superapp.data.remote.optInt
import com.sukashawarma.superapp.data.remote.optString
import com.sukashawarma.superapp.domain.model.Role
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.domain.util.JakartaTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ID_LOCALE = Locale("id", "ID")
private val CLOCK_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", ID_LOCALE)
private val CLOCK_SEC_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss", ID_LOCALE)

/** Status papan — cermin `BoardState` di `features/board/board.ts`. */
enum class BoardState(val key: String, val filterLabel: String) {
    MASUK("masuk", "Masuk Tepat"),
    TELAT_TOLERANSI("telat_toleransi", "Telat (Toleransi)"),
    TELAT("telat", "Masuk Telat"),
    BELUM("belum", "Belum Hadir"),
    ALPHA("alpha", "Alpha"),
    KELUAR("keluar", "Pulang Tepat"),
    LEBIH_AWAL("lebih_awal", "Pulang Cepat"),
    PULANG_TELAT("pulang_telat", "Pulang Lama"),
}

/** Teks pil di kartu staf — cermin map `PILL` di halaman web. */
fun boardPillLabel(state: BoardState, time: String?, delayMinutes: Int?): String = when (state) {
    BoardState.MASUK -> "Masuk ${time.orEmpty()}".trim()
    BoardState.TELAT_TOLERANSI -> "Telat (Toleransi) ${delayMinutes?.let { "$it mnt" } ?: time.orEmpty()}".trim()
    BoardState.TELAT -> "Masuk Telat ${delayMinutes?.let { "$it mnt" } ?: time.orEmpty()}".trim()
    BoardState.KELUAR -> "Pulang ${time.orEmpty()}".trim()
    BoardState.LEBIH_AWAL -> "Pulang Cepat ${time.orEmpty()}".trim()
    BoardState.PULANG_TELAT -> "Pulang Lama ${delayMinutes?.let { "$it mnt" } ?: time.orEmpty()}".trim()
    BoardState.BELUM -> "Belum Hadir"
    BoardState.ALPHA -> "Alpha"
}

data class PapanOutletOption(val id: String, val name: String)

data class StaffBoardRow(
    val staffId: String,
    val name: String,
    val role: String,
    val state: BoardState,
    val time: String?,
    val selfiePath: String?,
    val delayMinutes: Int?,
    val isManual: Boolean,
) {
    /** "admin_hr" -> "Admin Hr" — role mentah tidak enak dibaca di kartu. */
    val roleLabel: String
        get() = role.split("_").filter { it.isNotBlank() }.joinToString(" ") { part ->
            part.replaceFirstChar { it.titlecase(ID_LOCALE) }
        }.ifBlank { "-" }
}

data class BoardSummary(
    val hadir: Int = 0,
    val telat: Int = 0,
    val telatToleransi: Int = 0,
    val belum: Int = 0,
    val alpha: Int = 0,
    val total: Int = 0,
) {
    val hadirTotal: Int get() = hadir + telat + telatToleransi
    val percent: Int get() = if (total > 0) Math.round(hadirTotal * 100f / total) else 0
    fun fraction(count: Int): Float = if (total > 0) count.toFloat() / total else 0f
}

data class SecurityAlert(
    val id: String,
    val staffName: String,
    val status: String,
    val time: String,
) {
    val label: String
        get() = if (status == "fake_gps_blocked") "Mock Provider (Fake GPS)" else "Perpindahan Tidak Wajar"
}

private data class BoardConfig(
    val jamMasuk: String = "08:00",
    val jamKeluar: String = "16:00",
    val toleransiMenit: Int = 15,
)

data class PapanKehadiranUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
    val rows: List<StaffBoardRow> = emptyList(),
    val summary: BoardSummary = BoardSummary(),
    val alerts: List<SecurityAlert> = emptyList(),
    val canSeeAlerts: Boolean = false,
    val filter: BoardState? = null,
    val query: String = "",
    val lastRefresh: String = "",
    /** Regional manager memantau semua outlet, jadi outlet dipilih dulu baru data dimuat. */
    val canChooseOutlet: Boolean = false,
    val loadingOutlets: Boolean = false,
    val outlets: List<PapanOutletOption> = emptyList(),
    val selectedOutletId: String? = null,
    val selectedOutletName: String? = null,
) {
    val awaitingOutletChoice: Boolean get() = canChooseOutlet && selectedOutletId == null
    val filteredRows: List<StaffBoardRow>
        get() = rows
            .filter { filter == null || it.state == filter }
            .filter { query.isBlank() || it.name.contains(query.trim(), ignoreCase = true) }
}

private const val GLOBAL_OUTLET_ID = "00000000-0000-0000-0000-000000000000"

/** Role yang boleh melihat banner peringatan keamanan — cermin `isSpvOrAdmin` di web. */
private val ALERT_ROLES = setOf(
    Role.SPV, Role.OWNER, Role.ADMIN, Role.ADMIN_HR, Role.REGIONAL_MANAGER, Role.AREA_MANAGER,
)

/**
 * Papan kehadiran hari ini untuk satu outlet — versi native dari halaman web
 * `dashboard/papan-kehadiran`. Penentuan status per staf mengikuti `computeBoard`
 * di `features/board/board.ts` persis: rekaman `out` menang atas `in`, dan staf
 * yang belum absen berubah dari "Belum Hadir" jadi "Alpha" begitu lewat
 * jam_masuk + toleransi.
 */
class PapanKehadiranViewModel : ViewModel() {
    private val _state = MutableStateFlow(PapanKehadiranUiState())
    val state: StateFlow<PapanKehadiranUiState> = _state

    init {
        val staff = AppSession.staff.value
        val canSeeAlerts = staff?.role in ALERT_ROLES
        if (staff?.role == Role.REGIONAL_MANAGER) {
            _state.value = _state.value.copy(canChooseOutlet = true, canSeeAlerts = canSeeAlerts, loading = false)
            loadOutlets()
        } else {
            _state.value = _state.value.copy(
                canSeeAlerts = canSeeAlerts,
                selectedOutletId = staff?.outletId,
                selectedOutletName = staff?.outletName,
            )
            load()
        }
    }

    /** Daftar outlet tidak dibatasi di client — RLS backend yang menentukan mana yang
     *  boleh dilihat (sama seperti [[EnrollViewModel]] & [[ChecklistMonitorViewModel]]). */
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
                _state.value = _state.value.copy(
                    loadingOutlets = false,
                    outlets = rows.map { el ->
                        val o = el.asJsonObject
                        PapanOutletOption(id = o.optString("id") ?: "", name = o.optString("name") ?: "-")
                    },
                )
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
        _state.value = _state.value.copy(
            selectedOutletId = outletId,
            selectedOutletName = _state.value.outlets.find { it.id == outletId }?.name,
            rows = emptyList(),
            summary = BoardSummary(),
            alerts = emptyList(),
            error = null,
        )
        load()
    }

    fun setFilter(filter: BoardState?) {
        _state.value = _state.value.copy(filter = filter)
    }

    fun setQuery(query: String) {
        _state.value = _state.value.copy(query = query)
    }

    fun refresh() = load(silent = true)

    fun load(silent: Boolean = false) {
        val outletId = _state.value.selectedOutletId
        if (outletId == null) {
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
        val today = LocalDate.now(JakartaTime.ZONE)
        viewModelScope.launch {
            try {
                val staffList = loadActiveStaff(outletId)
                val config = loadConfig(outletId)

                val startIso = today.atStartOfDay(JakartaTime.ZONE).toInstant().toString()
                val endIso = today.plusDays(1).atStartOfDay(JakartaTime.ZONE).minusNanos(1).toInstant().toString()

                val attRows = Postgrest.select(
                    "attendance",
                    listOf(
                        "outlet_id" to "eq.$outletId",
                        "ts_server" to "gte.$startIso",
                        "ts_server" to "lte.$endIso",
                        "select" to "outlet_staff_id,type,status,ts_server,selfie_url,telat_menit,is_manual_button",
                        "order" to "ts_server.asc",
                    )
                ).map { it.asJsonObject }

                val rows = computeBoard(staffList, attRows, config, today)
                val summary = summarize(rows, staffList.size)

                val alerts = if (_state.value.canSeeAlerts) {
                    loadSecurityAlerts(outletId, startIso, endIso, staffList.associate { it.id to it.name })
                } else emptyList()

                _state.value = _state.value.copy(
                    loading = false,
                    refreshing = false,
                    error = null,
                    rows = rows,
                    summary = summary,
                    alerts = alerts,
                    lastRefresh = JakartaTime.now().format(CLOCK_SEC_FMT),
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    refreshing = false,
                    error = "Gagal memuat papan kehadiran: ${e.message}",
                )
            }
        }
    }

    /** Staf aktif outlet ini: penempatan utama + penempatan tambahan (`staff_outlets`). */
    private suspend fun loadActiveStaff(outletId: String): List<PapanStaff> {
        val staff = linkedMapOf<String, PapanStaff>()
        Postgrest.select(
            "outlet_staff",
            listOf("outlet_id" to "eq.$outletId", "status" to "eq.active", "select" to "id,name,role", "order" to "name.asc")
        ).forEach { el ->
            val o = el.asJsonObject
            val id = o.optString("id") ?: return@forEach
            staff[id] = PapanStaff(id, o.optString("name") ?: "-", o.optString("role") ?: "")
        }
        runCatching {
            Postgrest.select(
                "staff_outlets",
                listOf("outlet_id" to "eq.$outletId", "select" to "staff_id,outlet_staff!inner(id,name,role,status)")
            ).forEach { el ->
                val s = el.asJsonObject.getAsJsonObject("outlet_staff") ?: return@forEach
                if (s.optString("status") != "active") return@forEach
                val id = s.optString("id") ?: return@forEach
                if (id !in staff) staff[id] = PapanStaff(id, s.optString("name") ?: "-", s.optString("role") ?: "")
            }
        }
        return staff.values.toList()
    }

    /** Config outlet, jatuh ke `global_settings`, lalu ke default — urutan sama dengan
     *  route API web dan [[ClockViewModel]]. */
    private suspend fun loadConfig(outletId: String): BoardConfig {
        val outletCfg = runCatching {
            Postgrest.selectOne(
                "outlet_attendance_config",
                listOf("outlet_id" to "eq.$outletId", "select" to "jam_masuk,jam_keluar,toleransi_menit"),
            )
        }.getOrNull()
        if (outletCfg != null && !outletCfg.optString("jam_masuk").isNullOrBlank()) {
            return BoardConfig(
                jamMasuk = outletCfg.optString("jam_masuk") ?: "08:00",
                jamKeluar = outletCfg.optString("jam_keluar") ?: "16:00",
                toleransiMenit = outletCfg.optInt("toleransi_menit") ?: 15,
            )
        }
        val globalCfg = runCatching {
            Postgrest.selectOne("global_settings", listOf("key" to "eq.global_attendance_config", "select" to "value"))
        }.getOrNull()?.get("value")?.takeIf { it.isJsonObject }?.asJsonObject
        return BoardConfig(
            jamMasuk = globalCfg?.optString("jam_masuk") ?: "08:00",
            jamKeluar = globalCfg?.optString("jam_keluar") ?: "16:00",
            toleransiMenit = globalCfg?.optInt("toleransi_menit") ?: 15,
        )
    }

    private suspend fun loadSecurityAlerts(
        outletId: String,
        startIso: String,
        endIso: String,
        nameById: Map<String, String>,
    ): List<SecurityAlert> = runCatching {
        Postgrest.select(
            "attendance",
            listOf(
                "outlet_id" to "eq.$outletId",
                "status" to "in.(fake_gps_blocked,teleportation_blocked)",
                "ts_server" to "gte.$startIso",
                "ts_server" to "lte.$endIso",
                "select" to "id,outlet_staff_id,status,ts_server",
                "order" to "ts_server.desc",
            )
        ).mapNotNull { el ->
            val o = el.asJsonObject
            val ts = parseJakarta(o.optString("ts_server")) ?: return@mapNotNull null
            SecurityAlert(
                id = o.optString("id") ?: "",
                staffName = nameById[o.optString("outlet_staff_id")] ?: "Staf",
                status = o.optString("status") ?: "",
                time = ts.format(CLOCK_FMT),
            )
        }
    }.getOrDefault(emptyList())

    private data class PapanStaff(val id: String, val name: String, val role: String)

    /** Terjemahan langsung `computeBoard` web: `out` menang atas `in`, sisanya
     *  "belum"/"alpha" tergantung sudah lewat jam_masuk + toleransi atau belum. */
    private fun computeBoard(
        staffList: List<PapanStaff>,
        records: List<com.google.gson.JsonObject>,
        config: BoardConfig,
        today: LocalDate,
    ): List<StaffBoardRow> {
        val byStaff = records.groupBy { it.optString("outlet_staff_id") }
        val deadline = today.atTime(parseTime(config.jamMasuk))
            .plusMinutes(config.toleransiMenit.toLong())
            .atZone(JakartaTime.ZONE)
        val isPastDeadline = JakartaTime.now().isAfter(deadline)

        return staffList.map { s ->
            val recs = byStaff[s.id].orEmpty().sortedBy { it.optString("ts_server").orEmpty() }
            val inRec = recs.firstOrNull { it.optString("type") == "in" }
            val outRec = recs.lastOrNull { it.optString("type") == "out" }

            when {
                outRec != null -> {
                    val status = outRec.optString("status")
                    val state = when (status) {
                        "lebih_awal" -> BoardState.LEBIH_AWAL
                        "pulang_telat" -> BoardState.PULANG_TELAT
                        else -> BoardState.KELUAR
                    }
                    StaffBoardRow(
                        staffId = s.id,
                        name = s.name,
                        role = s.role,
                        state = state,
                        time = parseJakarta(outRec.optString("ts_server"))?.format(CLOCK_FMT),
                        selfiePath = outRec.optString("selfie_url"),
                        delayMinutes = if (state == BoardState.KELUAR) null else outRec.optInt("telat_menit"),
                        isManual = outRec.optBoolean("is_manual_button"),
                    )
                }

                inRec != null -> {
                    val state = when (inRec.optString("status")) {
                        "telat" -> BoardState.TELAT
                        "telat_toleransi" -> BoardState.TELAT_TOLERANSI
                        else -> BoardState.MASUK
                    }
                    StaffBoardRow(
                        staffId = s.id,
                        name = s.name,
                        role = s.role,
                        state = state,
                        time = parseJakarta(inRec.optString("ts_server"))?.format(CLOCK_FMT),
                        selfiePath = inRec.optString("selfie_url"),
                        delayMinutes = if (state == BoardState.MASUK) null else inRec.optInt("telat_menit"),
                        isManual = inRec.optBoolean("is_manual_button"),
                    )
                }

                else -> StaffBoardRow(
                    staffId = s.id,
                    name = s.name,
                    role = s.role,
                    state = if (isPastDeadline) BoardState.ALPHA else BoardState.BELUM,
                    time = null,
                    selfiePath = null,
                    delayMinutes = null,
                    isManual = false,
                )
            }
        }
    }

    private fun summarize(rows: List<StaffBoardRow>, total: Int) = BoardSummary(
        hadir = rows.count { it.state == BoardState.MASUK || it.state == BoardState.KELUAR },
        telat = rows.count { it.state == BoardState.TELAT },
        telatToleransi = rows.count { it.state == BoardState.TELAT_TOLERANSI },
        belum = rows.count { it.state == BoardState.BELUM },
        alpha = rows.count { it.state == BoardState.ALPHA },
        total = total,
    )

    private fun parseTime(value: String): LocalTime =
        runCatching { LocalTime.parse(value.take(5)) }.getOrDefault(LocalTime.of(8, 0))

    private fun parseJakarta(iso: String?): ZonedDateTime? {
        if (iso.isNullOrBlank()) return null
        return runCatching { OffsetDateTime.parse(iso).atZoneSameInstant(JakartaTime.ZONE) }.getOrNull()
    }
}
