package com.sukashawarma.superapp.presentation.absensi.pengaturan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonObject
import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.optInt
import com.sukashawarma.superapp.data.remote.optString
import com.sukashawarma.superapp.domain.session.AppSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class PengaturanUiState(
    val loading: Boolean = true,
    val loadError: String? = null,
    val saving: Boolean = false,
    val saveError: String? = null,
    val saved: Boolean = false,
    val jamMasuk: String = "08:00",
    val jamKeluar: String = "17:00",
    val toleransiMenit: Int = 15,
    val radiusM: Int = 100,
)

/** Pengaturan jam & toleransi absen per outlet — tabel `outlet_attendance_config`
 *  (1 baris per outlet_id, dikonfirmasi via introspeksi PostgREST OpenAPI). Hanya
 *  admin/HR yang bisa masuk rute ini (gate di [[AbsensiHubScreen]]/ADMIN_OR_HR_ROLES). */
class PengaturanViewModel : ViewModel() {
    private val _state = MutableStateFlow(PengaturanUiState())
    val state: StateFlow<PengaturanUiState> = _state

    init { load() }

    fun load() {
        val outletId = AppSession.staff.value?.outletId
        if (outletId == null) {
            _state.value = PengaturanUiState(loading = false, loadError = "Akun tidak terhubung ke outlet.")
            return
        }
        _state.value = _state.value.copy(loading = true, loadError = null)
        viewModelScope.launch {
            try {
                val row = Postgrest.selectOne(
                    "outlet_attendance_config",
                    listOf("outlet_id" to "eq.$outletId", "select" to "jam_masuk,jam_keluar,toleransi_menit,radius_m")
                )
                if (row != null) {
                    _state.value = PengaturanUiState(
                        loading = false,
                        jamMasuk = (row.optString("jam_masuk") ?: "08:00:00").take(5),
                        jamKeluar = (row.optString("jam_keluar") ?: "17:00:00").take(5),
                        toleransiMenit = row.optInt("toleransi_menit") ?: 15,
                        radiusM = row.optInt("radius_m") ?: 100,
                    )
                } else {
                    _state.value = _state.value.copy(loading = false)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, loadError = "Gagal memuat pengaturan: ${e.message}")
            }
        }
    }

    fun update(jamMasuk: String, jamKeluar: String, toleransiMenit: Int, radiusM: Int) {
        val outletId = AppSession.staff.value?.outletId ?: return
        _state.value = _state.value.copy(saving = true, saved = false, saveError = null)
        viewModelScope.launch {
            try {
                Postgrest.upsert(
                    "outlet_attendance_config",
                    JsonObject().apply {
                        addProperty("outlet_id", outletId)
                        addProperty("jam_masuk", "$jamMasuk:00")
                        addProperty("jam_keluar", "$jamKeluar:00")
                        addProperty("toleransi_menit", toleransiMenit)
                        addProperty("radius_m", radiusM)
                    },
                    onConflict = "outlet_id",
                )
                _state.value = _state.value.copy(
                    saving = false, saved = true,
                    jamMasuk = jamMasuk, jamKeluar = jamKeluar, toleransiMenit = toleransiMenit, radiusM = radiusM,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(saving = false, saveError = "Gagal menyimpan: ${e.message}")
            }
        }
    }
}
