package com.sukashawarma.superapp.presentation.absensi.clock

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonObject
import com.sukashawarma.superapp.data.location.LocationRepository
import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.optString
import com.sukashawarma.superapp.domain.gps.GpsMath
import com.sukashawarma.superapp.domain.gps.LatLng
import com.sukashawarma.superapp.domain.model.Role
import com.sukashawarma.superapp.domain.session.AppSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AttendanceOutlet(
    val id: String,
    val name: String,
    val lat: Double?,
    val lng: Double?,
    /** Jarak perangkat ke outlet ini, diisi setelah GPS didapat. */
    val distanceM: Double? = null,
) {
    val coords: LatLng? get() = if (lat != null && lng != null) LatLng(lat, lng) else null
}

data class AttendanceOutletsUiState(
    val loading: Boolean = true,
    val locating: Boolean = false,
    val error: String? = null,
    val outlets: List<AttendanceOutlet> = emptyList(),
    val selectedId: String? = null,
    /** Outlet aktif dipilihkan sistem dari GPS, bukan oleh user. */
    val autoDetected: Boolean = false,
) {
    val selected: AttendanceOutlet? get() = outlets.find { it.id == selectedId }
    val hasChoice: Boolean get() = outlets.size > 1
}

/** Jarak dalam teks pendek — cermin `formatDistanceMeters(m, true)` di web. */
fun formatDistanceShort(meters: Double): String = if (meters >= 1000) {
    val km = meters / 1000.0
    "${trimZero(km)}km"
} else {
    "${trimZero(meters)}m"
}

private fun trimZero(value: Double): String {
    val text = String.format(java.util.Locale.US, "%.1f", value)
    return text.removeSuffix(".0")
}

/**
 * Role yang boleh absen di outlet mana pun meski `staff_outlets` cuma berisi satu baris —
 * cermin daftar role di route web `/api/staff-outlets`. Untuk role lain, outlet yang boleh
 * dipakai murni berasal dari penempatan: `outlet_staff.outlet_id` + baris `staff_outlets`.
 *
 * Ini hanya menentukan APA yang muncul di picker. Otoritas tetap di server: RPC
 * `submit_attendance` menolak dengan alasan `cross_outlet` kalau staf tidak berhak, dan
 * memeriksa ulang geofence terhadap koordinat outlet yang dipilih.
 */
private val ALL_OUTLET_ROLES = setOf(
    Role.SPV, Role.OWNER, Role.ADMIN, Role.ADMIN_HR,
    Role.KORLAP, Role.REGIONAL_MANAGER, Role.AREA_MANAGER,
)

/**
 * Menentukan di outlet mana user boleh absen dan outlet mana yang sedang aktif —
 * cermin blok "Leader / Multi-outlet Support — Seamless Auto-Detect" di
 * `features/clock/AttendanceKioskPanel.tsx` (web).
 *
 * Alurnya: kumpulkan outlet yang terhubung ke user, ambil satu fix GPS, urutkan dari yang
 * terdekat, lalu kunci otomatis ke outlet terdekat. Begitu user menggeser pilihan sendiri,
 * deteksi otomatis berhenti mengambil alih sampai layar dibuka ulang.
 *
 * Dipisah dari [[ClockViewModel]] karena ClockViewModel dibuat ulang setiap kali outlet
 * berganti (di-key oleh outletId) — pemilihan outlet harus hidup lebih lama dari itu.
 */
class AttendanceOutletsViewModel(application: Application) : AndroidViewModel(application) {

    private val locationRepository = LocationRepository(application)

    // Diisi dari penempatan utama sejak awal supaya layar absen langsung bisa jalan sambil
    // daftar outlet dimuat — cermin `selectedOutletId || outletStaff?.outlet_id` di web.
    private val _state = MutableStateFlow(
        AttendanceOutletsUiState(selectedId = AppSession.staff.value?.outletId)
    )
    val state: StateFlow<AttendanceOutletsUiState> = _state

    /** Setelah user memilih sendiri, jangan pernah dipindah diam-diam oleh GPS. */
    private var manualSelection = false

    init { load() }

    fun load() {
        val staff = AppSession.staff.value
        if (staff == null) {
            _state.value = AttendanceOutletsUiState(loading = false, error = "Sesi tidak valid.")
            return
        }
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                val outlets = loadAccessibleOutlets(staff.id, staff.outletId, staff.role)
                if (outlets.isEmpty()) {
                    _state.value = _state.value.copy(loading = false, outlets = emptyList(), selectedId = null)
                    return@launch
                }
                val preferred = outlets.firstOrNull { it.id == staff.outletId }?.id ?: outlets.first().id
                _state.value = _state.value.copy(
                    loading = false,
                    outlets = outlets,
                    selectedId = if (manualSelection) _state.value.selectedId ?: preferred else preferred,
                )
                if (outlets.size > 1) locateAndSort()
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    error = "Daftar outlet belum dapat dimuat. Coba lagi.",
                )
            }
        }
    }

    fun selectOutlet(outletId: String) {
        if (_state.value.selectedId == outletId) return
        manualSelection = true
        _state.value = _state.value.copy(selectedId = outletId, autoDetected = false)
    }

    /** Dipakai tombol "deteksi ulang": kembalikan kendali ke GPS. */
    fun redetect() {
        manualSelection = false
        locateAndSort()
    }

    /**
     * Satu fix GPS untuk mengukur jarak ke tiap outlet, lalu urutkan dari terdekat.
     * Outlet tanpa koordinat (mis. Kantor Pusat) ikut terdaftar tapi tidak pernah menang
     * sebagai "terdekat" karena jaraknya tidak bisa dihitung.
     */
    private fun locateAndSort() {
        _state.value = _state.value.copy(locating = true)
        viewModelScope.launch {
            val fix = locationRepository.fastFix() ?: locationRepository.preciseFix()
            if (fix == null) {
                _state.value = _state.value.copy(locating = false)
                return@launch
            }
            val device = LatLng(fix.lat, fix.lng)
            val measured = _state.value.outlets
                .map { outlet ->
                    val coords = outlet.coords
                    outlet.copy(distanceM = coords?.let { GpsMath.haversineMeters(it, device) })
                }
                .sortedBy { it.distanceM ?: Double.MAX_VALUE }

            val nearest = measured.firstOrNull { it.distanceM != null }
            val autoPick = !manualSelection && nearest != null

            _state.value = _state.value.copy(
                locating = false,
                outlets = measured,
                selectedId = if (autoPick) nearest!!.id else _state.value.selectedId,
                autoDetected = autoPick,
            )
        }
    }

    /**
     * Outlet yang terhubung ke user — urutan sumbernya sama dengan route web
     * `/api/staff-outlets`: penempatan tambahan (`staff_outlets`), lalu penempatan utama
     * (`outlet_staff.outlet_id`), lalu — khusus role pengawas yang penempatannya belum
     * diisi — seluruh outlet aktif.
     */
    private suspend fun loadAccessibleOutlets(
        staffId: String,
        primaryOutletId: String?,
        role: Role?,
    ): List<AttendanceOutlet> {
        val list = LinkedHashMap<String, AttendanceOutlet>()

        // 1. Penempatan tambahan. Embed FK dicoba dulu; kalau schema cache PostgREST tidak
        //    mengenali relasinya, jatuh ke dua query terpisah seperti fallback di web.
        val assigned = runCatching {
            Postgrest.select(
                "staff_outlets",
                listOf(
                    "staff_id" to "eq.$staffId",
                    "select" to "outlet_id,outlets!staff_outlets_outlet_id_fkey(id,name,lat,lng)",
                ),
            )
        }.getOrNull()

        assigned?.forEach { element ->
            val embedded = element.asJsonObject.getAsJsonObject("outlets") ?: return@forEach
            embedded.toOutlet()?.let { list[it.id] = it }
        }

        if (list.isEmpty()) {
            val ids = runCatching {
                Postgrest.select("staff_outlets", listOf("staff_id" to "eq.$staffId", "select" to "outlet_id"))
                    .mapNotNull { it.asJsonObject.optString("outlet_id") }
            }.getOrDefault(emptyList())
            if (ids.isNotEmpty()) {
                Postgrest.select(
                    "outlets",
                    listOf("id" to "in.(${ids.joinToString(",")})", "select" to "id,name,lat,lng"),
                ).forEach { element ->
                    element.asJsonObject.toOutlet()?.let { list[it.id] = it }
                }
            }
        }

        // 2. Penempatan utama.
        if (primaryOutletId != null && primaryOutletId !in list) {
            runCatching {
                Postgrest.selectOne("outlets", listOf("id" to "eq.$primaryOutletId", "select" to "id,name,lat,lng"))
            }.getOrNull()?.toOutlet()?.let { list[it.id] = it }
        }

        // 3. Role pengawas yang belum punya penempatan tambahan boleh absen di mana saja.
        if (role in ALL_OUTLET_ROLES && list.size <= 1) {
            Postgrest.select(
                "outlets",
                listOf("is_active" to "eq.true", "select" to "id,name,lat,lng", "order" to "name.asc"),
            ).forEach { element ->
                element.asJsonObject.toOutlet()?.let { list.putIfAbsent(it.id, it) }
            }
        }

        return list.values.toList()
    }

    private fun JsonObject.toOutlet(): AttendanceOutlet? {
        val id = optString("id") ?: return null
        return AttendanceOutlet(
            id = id,
            name = optString("name") ?: "Outlet",
            lat = get("lat")?.takeIf { !it.isJsonNull }?.asDouble,
            lng = get("lng")?.takeIf { !it.isJsonNull }?.asDouble,
        )
    }
}

class AttendanceOutletsViewModelFactory(
    private val application: Application,
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
        AttendanceOutletsViewModel(application) as T
}
