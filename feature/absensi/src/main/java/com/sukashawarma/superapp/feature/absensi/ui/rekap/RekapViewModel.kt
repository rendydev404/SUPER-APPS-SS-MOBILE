package com.sukashawarma.superapp.presentation.absensi.rekap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.SupabaseClient
import com.sukashawarma.superapp.data.remote.optBoolean
import com.sukashawarma.superapp.data.remote.optString
import com.sukashawarma.superapp.domain.model.Role
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.domain.util.JakartaTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ID_LOCALE = Locale("id", "ID")
private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", ID_LOCALE)
private val TIME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", ID_LOCALE)

/** Rentang siap-pakai di header — cermin `PERIOD_OPTIONS` halaman web. */
enum class RekapPeriod(val label: String) {
    HARI_INI("Hari Ini"),
    KEMARIN("Kemarin"),
    BULAN_INI("Bulan Ini"),
    KUSTOM("Kustom"),
}

/** Filter "Ringkasan Karyawan" — sama daftarnya dengan Select status di web. */
enum class RekapStatusFilter(val label: String) {
    SEMUA("Semua Status"),
    HADIR("Hadir"),
    TELAT("Telat"),
    TELAT_TOLERANSI("Telat (Toleransi)"),
    ALPHA("Alpha"),
    PULANG_CEPAT("Pulang Cepat"),
}

data class RekapOutletOption(val id: String, val name: String)

data class AttendanceRow(
    val id: String,
    val type: String,
    val tsServer: ZonedDateTime,
    val status: String,
    val selfiePath: String?,
    val staffId: String,
    val staffName: String,
    val telatMenit: Int?,
    val isManual: Boolean,
) {
    val isAlpha: Boolean get() = status == "alpha"
    val date: LocalDate get() = tsServer.toLocalDate()
    val jam: String get() = tsServer.format(TIME_FMT)
    val tanggal: String get() = tsServer.format(DATE_FMT)
}

data class StaffSummary(
    val staffId: String,
    val name: String,
    val totalMasuk: Int,
    val totalTelat: Int,
    val totalTelatToleransi: Int,
    val totalAlpha: Int,
    val totalCepat: Int,
    val latestPhotoPath: String?,
    val latestIn: AttendanceRow?,
    val latestOut: AttendanceRow?,
    val rows: List<AttendanceRow>,
)

/** Satu hari dalam panel detail karyawan: absen masuk, pulang, atau ditandai alpha. */
data class RekapDayDetail(
    val date: LocalDate,
    val label: String,
    val masuk: AttendanceRow?,
    val pulang: AttendanceRow?,
    val alpha: AttendanceRow?,
) {
    val isAlpha: Boolean get() = masuk == null && alpha != null
}

data class RekapGlobalSummary(
    val masuk: Int = 0,
    val telat: Int = 0,
    val alpha: Int = 0,
    val cepat: Int = 0,
)

data class RekapUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val period: RekapPeriod = RekapPeriod.HARI_INI,
    val customStart: LocalDate = LocalDate.now(JakartaTime.ZONE).withDayOfMonth(1),
    val customEnd: LocalDate = LocalDate.now(JakartaTime.ZONE),
    val statusFilter: RekapStatusFilter = RekapStatusFilter.SEMUA,
    val rows: List<AttendanceRow> = emptyList(),
    val allSummaries: List<StaffSummary> = emptyList(),
    /** Regional manager memantau semua outlet, jadi outlet dipilih dulu baru data dimuat. */
    val canChooseOutlet: Boolean = false,
    val loadingOutlets: Boolean = false,
    val outlets: List<RekapOutletOption> = emptyList(),
    val selectedOutletId: String? = null,
    val selectedOutletName: String? = null,
) {
    val awaitingOutletChoice: Boolean get() = canChooseOutlet && selectedOutletId == null

    val summaries: List<StaffSummary>
        get() = when (statusFilter) {
            RekapStatusFilter.SEMUA -> allSummaries
            RekapStatusFilter.HADIR -> allSummaries.filter { it.totalMasuk > 0 }
            RekapStatusFilter.TELAT -> allSummaries.filter { it.totalTelat > 0 }
            RekapStatusFilter.TELAT_TOLERANSI -> allSummaries.filter { it.totalTelatToleransi > 0 }
            RekapStatusFilter.ALPHA -> allSummaries.filter { it.totalAlpha > 0 }
            RekapStatusFilter.PULANG_CEPAT -> allSummaries.filter { it.totalCepat > 0 }
        }

    /** Angka kartu atas dihitung dari daftar yang SUDAH difilter — sama seperti web. */
    val globalSummary: RekapGlobalSummary
        get() = summaries.fold(RekapGlobalSummary()) { acc, s ->
            RekapGlobalSummary(
                masuk = acc.masuk + s.totalMasuk,
                telat = acc.telat + s.totalTelat + s.totalTelatToleransi,
                alpha = acc.alpha + s.totalAlpha,
                cepat = acc.cepat + s.totalCepat,
            )
        }

    val startDate: LocalDate
        get() {
            val today = LocalDate.now(JakartaTime.ZONE)
            return when (period) {
                RekapPeriod.HARI_INI -> today
                RekapPeriod.KEMARIN -> today.minusDays(1)
                RekapPeriod.BULAN_INI -> today.withDayOfMonth(1)
                RekapPeriod.KUSTOM -> customStart
            }
        }

    val endDate: LocalDate
        get() {
            val today = LocalDate.now(JakartaTime.ZONE)
            return when (period) {
                RekapPeriod.HARI_INI -> today
                RekapPeriod.KEMARIN -> today.minusDays(1)
                RekapPeriod.BULAN_INI -> today
                RekapPeriod.KUSTOM -> customEnd
            }
        }

    val periodLabel: String
        get() = when (period) {
            RekapPeriod.KUSTOM ->
                if (startDate == endDate) startDate.format(DATE_FMT)
                else "${startDate.format(DATE_FMT)} – ${endDate.format(DATE_FMT)}"
            RekapPeriod.BULAN_INI -> "${startDate.format(DATE_FMT)} – ${endDate.format(DATE_FMT)}"
            else -> startDate.format(DATE_FMT)
        }
}

/** Teks status yang dibaca user — cermin `formatStatusText` di halaman web. */
fun rekapStatusText(status: String): String = when (status) {
    "telat" -> "Masuk Telat"
    "telat_toleransi" -> "Telat (Toleransi)"
    "lebih_awal" -> "Pulang Cepat"
    "pulang_telat" -> "Pulang Lama"
    "tepat" -> "Tepat Waktu"
    "alpha" -> "Alpha"
    else -> status
}

/** `attendance.selfie_url` menyimpan path objek di bucket `selfies` (bukan URL penuh) —
 *  lihat RPC `submit_attendance`. Ubah jadi URL publik yang bisa dimuat Coil. */
fun selfiePublicUrl(path: String?): String? {
    if (path.isNullOrBlank()) return null
    if (path.startsWith("http://") || path.startsWith("https://")) return path
    return "${SupabaseClient.BASE_URL}storage/v1/object/public/selfies/${path.removePrefix("selfies/")}"
}

private const val GLOBAL_OUTLET_ID = "00000000-0000-0000-0000-000000000000"

/**
 * Rekap & riwayat kehadiran satu outlet — versi native dari halaman web
 * `dashboard/rekap`, termasuk baris "alpha virtual": staf aktif yang tidak punya
 * absen masuk di suatu hari dihitung alpha di client (persis seperti
 * `app/api/attendance/rekap/route.ts`), karena tabel `attendance` tidak menyimpan
 * ketidakhadiran.
 *
 * Beda tipis yang disengaja dari web: pengelompokan hari memakai tanggal WIB hasil
 * konversi timestamp, bukan prefiks string ISO (yang di web berarti tanggal UTC dan
 * meleset untuk absen sebelum jam 07:00 WIB).
 */
class RekapViewModel : ViewModel() {
    private val _state = MutableStateFlow(RekapUiState())
    val state: StateFlow<RekapUiState> = _state

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
                        RekapOutletOption(id = o.optString("id") ?: "", name = o.optString("name") ?: "-")
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
            allSummaries = emptyList(),
            error = null,
        )
        load()
    }

    fun setPeriod(period: RekapPeriod) {
        if (_state.value.period == period) return
        _state.value = _state.value.copy(period = period)
        if (_state.value.selectedOutletId != null) load()
    }

    fun setCustomRange(start: LocalDate, end: LocalDate) {
        val today = LocalDate.now(JakartaTime.ZONE)
        val safeEnd = if (end.isAfter(today)) today else end
        val safeStart = if (start.isAfter(safeEnd)) safeEnd else start
        _state.value = _state.value.copy(
            period = RekapPeriod.KUSTOM,
            customStart = safeStart,
            customEnd = safeEnd,
        )
        if (_state.value.selectedOutletId != null) load()
    }

    fun setStatusFilter(filter: RekapStatusFilter) {
        _state.value = _state.value.copy(statusFilter = filter)
    }

    /** Riwayat per hari untuk panel detail satu karyawan — cermin `detailByDate` web. */
    fun detailByDate(summary: StaffSummary): List<RekapDayDetail> {
        val byDate = summary.rows.groupBy { it.date }
        return byDate.entries
            .sortedByDescending { it.key }
            .map { (date, rows) ->
                RekapDayDetail(
                    date = date,
                    label = date.format(DATE_FMT),
                    masuk = rows.firstOrNull { it.type == "in" && !it.isAlpha },
                    pulang = rows.firstOrNull { it.type == "out" },
                    alpha = rows.firstOrNull { it.isAlpha },
                )
            }
    }

    /** CSV dengan header Indonesia — format identik `attendanceToCsv` di web. */
    fun buildCsv(): String {
        fun esc(v: String) = if (v.any { it == '"' || it == ',' || it == '\n' }) "\"${v.replace("\"", "\"\"")}\"" else v
        val header = "Nama,Tipe,Jam,Status"
        val body = _state.value.rows.map { r ->
            listOf(
                r.staffName,
                if (r.type == "in") "Masuk" else "Keluar",
                if (r.isAlpha) "-" else "${r.tanggal} ${r.jam}",
                rekapStatusText(r.status),
            ).joinToString(",") { esc(it) }
        }
        return (listOf(header) + body).joinToString("\n")
    }

    fun csvFileName(): String {
        val s = _state.value
        return "rekap-${s.startDate}_${s.endDate}.csv"
    }

    fun load() {
        val outletId = _state.value.selectedOutletId
        if (outletId == null) {
            if (_state.value.canChooseOutlet) {
                _state.value = _state.value.copy(loading = false)
                return
            }
            _state.value = _state.value.copy(
                loading = false,
                error = "Akun Anda belum terhubung dengan cabang manapun. Hubungi admin untuk pengaturan penempatan.",
            )
            return
        }
        val start = _state.value.startDate
        val end = _state.value.endDate
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                val activeStaff = loadActiveStaff(outletId)
                val nameById = activeStaff.associate { it.id to it.name }

                val startIso = start.atStartOfDay(JakartaTime.ZONE).toInstant().toString()
                val endIso = end.plusDays(1).atStartOfDay(JakartaTime.ZONE).minusNanos(1).toInstant().toString()

                val raw = Postgrest.select(
                    "attendance",
                    listOf(
                        "outlet_id" to "eq.$outletId",
                        "ts_server" to "gte.$startIso",
                        "ts_server" to "lte.$endIso",
                        // Nama staf TIDAK di-embed: FK attendance<->outlet_staff tidak terdaftar
                        // di schema cache PostgREST, jadi digabung manual di bawah (sama seperti
                        // route API web yang juga memetakan nama sendiri).
                        "select" to "id,type,ts_server,status,selfie_url,outlet_staff_id,telat_menit,is_manual_button",
                        "order" to "ts_server.desc",
                    )
                )

                val dbRows = raw.mapNotNull { el ->
                    val o = el.asJsonObject
                    val staffId = o.optString("outlet_staff_id") ?: return@mapNotNull null
                    val ts = parseJakarta(o.optString("ts_server")) ?: return@mapNotNull null
                    AttendanceRow(
                        id = o.optString("id") ?: "",
                        type = o.optString("type") ?: "in",
                        tsServer = ts,
                        status = o.optString("status") ?: "tepat",
                        selfiePath = o.optString("selfie_url"),
                        staffId = staffId,
                        staffName = nameById[staffId] ?: "-",
                        telatMenit = o.get("telat_menit")?.takeIf { !it.isJsonNull }?.asInt,
                        isManual = o.optBoolean("is_manual_button"),
                    )
                }

                val rows = (dbRows + virtualAlphas(dbRows, activeStaff, start, end))
                    .sortedByDescending { it.tsServer }

                _state.value = _state.value.copy(
                    loading = false,
                    rows = rows,
                    allSummaries = buildSummaries(rows),
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = "Gagal memuat rekap: ${e.message}")
            }
        }
    }

    /** Staf aktif outlet ini: penempatan utama + penempatan tambahan (`staff_outlets`). */
    private suspend fun loadActiveStaff(outletId: String): List<RekapOutletOption> {
        val staff = linkedMapOf<String, String>()
        Postgrest.select(
            "outlet_staff",
            listOf("outlet_id" to "eq.$outletId", "status" to "eq.active", "select" to "id,name", "order" to "name.asc")
        ).forEach { el ->
            val o = el.asJsonObject
            val id = o.optString("id") ?: return@forEach
            staff[id] = o.optString("name") ?: "-"
        }
        runCatching {
            Postgrest.select(
                "staff_outlets",
                listOf("outlet_id" to "eq.$outletId", "select" to "staff_id,outlet_staff!inner(id,name,status)")
            ).forEach { el ->
                val s = el.asJsonObject.getAsJsonObject("outlet_staff") ?: return@forEach
                if (s.optString("status") != "active") return@forEach
                val id = s.optString("id") ?: return@forEach
                if (id !in staff) staff[id] = s.optString("name") ?: "-"
            }
        }
        return staff.map { (id, name) -> RekapOutletOption(id, name) }
    }

    /** Tabel `attendance` tidak menyimpan ketidakhadiran, jadi "alpha" dibangkitkan di sini:
     *  tiap staf aktif yang tidak punya absen masuk pada suatu hari (hari mendatang dilewati). */
    private fun virtualAlphas(
        rows: List<AttendanceRow>,
        activeStaff: List<RekapOutletOption>,
        start: LocalDate,
        end: LocalDate,
    ): List<AttendanceRow> {
        val today = LocalDate.now(JakartaTime.ZONE)
        val result = mutableListOf<AttendanceRow>()
        var day = start
        while (!day.isAfter(end)) {
            if (day.isAfter(today)) break
            val checkedIn = rows.filter { it.type == "in" && it.date == day }.map { it.staffId }.toSet()
            activeStaff.forEach { staff ->
                if (staff.id !in checkedIn) {
                    result += AttendanceRow(
                        id = "virtual-alpha-${staff.id}-$day",
                        type = "in",
                        tsServer = day.atTime(23, 59, 59).atZone(JakartaTime.ZONE),
                        status = "alpha",
                        selfiePath = null,
                        staffId = staff.id,
                        staffName = staff.name,
                        telatMenit = null,
                        isManual = false,
                    )
                }
            }
            day = day.plusDays(1)
        }
        return result
    }

    private fun buildSummaries(rows: List<AttendanceRow>): List<StaffSummary> {
        data class Acc(
            var masuk: Int = 0,
            var telat: Int = 0,
            var telatToleransi: Int = 0,
            var alpha: Int = 0,
            var cepat: Int = 0,
            var photo: String? = null,
            var latestIn: AttendanceRow? = null,
            var latestOut: AttendanceRow? = null,
            val rows: MutableList<AttendanceRow> = mutableListOf(),
        )

        val map = linkedMapOf<String, Acc>()
        rows.forEach { r ->
            val acc = map.getOrPut(r.staffId) { Acc() }
            acc.rows += r
            if (!r.selfiePath.isNullOrBlank() && acc.photo == null) acc.photo = r.selfiePath
            if (r.type == "in" && !r.isAlpha && acc.latestIn == null) acc.latestIn = r
            if (r.type == "out" && acc.latestOut == null) acc.latestOut = r

            if (r.type == "in" && !r.isAlpha) acc.masuk++
            when (r.status) {
                "telat", "pulang_telat" -> acc.telat++
                "telat_toleransi" -> acc.telatToleransi++
                "alpha" -> acc.alpha++
                "lebih_awal" -> acc.cepat++
            }
        }

        return map.map { (staffId, acc) ->
            StaffSummary(
                staffId = staffId,
                name = acc.rows.firstOrNull()?.staffName ?: "-",
                totalMasuk = acc.masuk,
                totalTelat = acc.telat,
                totalTelatToleransi = acc.telatToleransi,
                totalAlpha = acc.alpha,
                totalCepat = acc.cepat,
                latestPhotoPath = acc.photo,
                latestIn = acc.latestIn,
                latestOut = acc.latestOut,
                rows = acc.rows,
            )
        }.sortedBy { it.name.lowercase(ID_LOCALE) }
    }

    private fun parseJakarta(iso: String?): ZonedDateTime? {
        if (iso.isNullOrBlank()) return null
        return runCatching { OffsetDateTime.parse(iso).atZoneSameInstant(JakartaTime.ZONE) }.getOrNull()
    }
}
