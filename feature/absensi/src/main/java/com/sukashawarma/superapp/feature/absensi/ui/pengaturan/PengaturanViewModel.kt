package com.sukashawarma.superapp.presentation.absensi.pengaturan

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonParser
import com.google.gson.JsonObject
import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.optInt
import com.sukashawarma.superapp.data.remote.optString
import com.sukashawarma.superapp.domain.session.AppSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException

/** Outlet yang bisa dipilih saat menambah jadwal khusus. */
data class OutletOption(val id: String, val name: String)

/** Satu baris `outlet_attendance_config` — jadwal yang menimpa aturan pusat untuk outlet ini. */
data class OutletSchedule(
    val outletId: String,
    val outletName: String,
    val jamMasuk: String,
    val jamKeluar: String,
    val toleransiMenit: Int,
    val radiusM: Int,
    val mode: String,
) {
    val manual: Boolean get() = mode == "manual"
}

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
    /** Mode kamera aturan pusat. Dibaca saja (panel pusat belum punya field ini), dipakai
     *  sebagai default saat menambah jadwal khusus — sama seperti modal di web. */
    val globalMode: String = "auto",
    /* --- Jadwal khusus per outlet --- */
    val loadingJadwal: Boolean = false,
    val jadwalError: String? = null,
    val jadwalMessage: String? = null,
    val savingJadwal: Boolean = false,
    val outlets: List<OutletOption> = emptyList(),
    val jadwalKhusus: List<OutletSchedule> = emptyList(),
) {
    /** Outlet yang belum punya jadwal khusus — hanya ini yang boleh ditambahkan (1 baris/outlet). */
    val outletsTanpaJadwal: List<OutletOption>
        get() = outlets.filter { opt -> jadwalKhusus.none { it.outletId == opt.id } }
}

/** Pengaturan absensi pusat yang berlaku ke seluruh outlet, plus daftar jadwal khusus
 * per outlet yang menimpanya. Hanya admin, HR, dan regional manager yang bisa masuk rute
 * ini (gate di [[AbsensiHubScreen]]/ADMIN_OR_HR_ROLES) — backend menegakkan ulang lewat
 * RPC di `plan/jadwal-khusus-outlet.sql`. */
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
                val globalRow = Postgrest.selectOne(
                    "global_settings",
                    listOf("key" to "eq.global_attendance_config", "select" to "value"),
                )
                val globalValue = globalRow?.get("value")?.takeIf { it.isJsonObject }?.asJsonObject
                val row = if (globalValue == null) {
                    Postgrest.selectOne(
                        "outlet_attendance_config",
                        listOf("outlet_id" to "eq.$outletId", "select" to "jam_masuk,jam_keluar,toleransi_menit,radius_m"),
                    )
                } else null
                if (globalValue != null || row != null) {
                    _state.value = _state.value.copy(
                        loading = false,
                        loadError = null,
                        jamMasuk = (globalValue?.optString("jam_masuk") ?: row?.optString("jam_masuk") ?: "08:00:00").take(5),
                        jamKeluar = (globalValue?.optString("jam_keluar") ?: row?.optString("jam_keluar") ?: "17:00:00").take(5),
                        toleransiMenit = globalValue?.optInt("toleransi_menit") ?: row?.optInt("toleransi_menit") ?: 15,
                        radiusM = globalValue?.optInt("radius_m") ?: row?.optInt("radius_m") ?: 100,
                        globalMode = globalValue?.optString("absen_window_mode")
                            ?.takeIf { it == "auto" || it == "manual" } ?: "auto",
                    )
                } else {
                    _state.value = _state.value.copy(loading = false)
                }
            } catch (e: Exception) {
                Log.e("PengaturanViewModel", "Gagal memuat konfigurasi absensi", e)
                _state.value = _state.value.copy(loading = false, loadError = friendlyError(e, loading = true))
            }
        }
        loadJadwalKhusus()
    }

    fun update(jamMasuk: String, jamKeluar: String, toleransiMenit: Int, radiusM: Int) {
        val outletId = AppSession.staff.value?.outletId
        if (outletId == null) {
            _state.value = _state.value.copy(
                saving = false,
                saved = false,
                saveError = "Akun ini belum terhubung ke outlet. Silakan login ulang atau hubungi admin.",
            )
            return
        }

        val normalizedJamMasuk = normalizeTime(jamMasuk)
        val normalizedJamKeluar = normalizeTime(jamKeluar)
        val validationError = validate(normalizedJamMasuk, normalizedJamKeluar, toleransiMenit, radiusM)
        if (validationError != null) {
            _state.value = _state.value.copy(saving = false, saved = false, saveError = validationError)
            return
        }
        val safeJamMasuk = normalizedJamMasuk ?: return
        val safeJamKeluar = normalizedJamKeluar ?: return

        _state.value = _state.value.copy(saving = true, saved = false, saveError = null)
        viewModelScope.launch {
            try {
                Postgrest.rpc(
                    "save_global_attendance_config",
                    JsonObject().apply {
                        addProperty("p_jam_masuk", "$safeJamMasuk:00")
                        addProperty("p_jam_keluar", "$safeJamKeluar:00")
                        addProperty("p_toleransi_menit", toleransiMenit)
                        addProperty("p_radius_m", radiusM)
                    },
                )
                _state.value = _state.value.copy(
                    saving = false, saved = true,
                    jamMasuk = safeJamMasuk,
                    jamKeluar = safeJamKeluar,
                    toleransiMenit = toleransiMenit,
                    radiusM = radiusM,
                )
            } catch (e: Exception) {
                Log.e("PengaturanViewModel", "Gagal menyimpan konfigurasi absensi untuk outlet=$outletId", e)
                _state.value = _state.value.copy(saving = false, saved = false, saveError = friendlyError(e))
            }
        }
    }

    /* ------------------------------------------------ Jadwal khusus per outlet */

    /** Daftar outlet tidak dibatasi di client — RLS backend yang menentukan mana yang
     *  boleh dilihat (sama seperti [[RekapViewModel]]). Daftar pengecualiannya sendiri
     *  lewat RPC karena RLS `oac_read_own_outlet` hanya membuka outlet sendiri. */
    fun loadJadwalKhusus() {
        _state.value = _state.value.copy(loadingJadwal = true, jadwalError = null)
        viewModelScope.launch {
            // Dua request terpisah: kalau RPC jadwal gagal (mis. belum dipasang), daftar
            // outlet yang sudah berhasil dimuat jangan ikut hilang.
            try {
                val outletRows = Postgrest.select(
                    "outlets",
                    listOf(
                        "is_active" to "eq.true",
                        "id" to "neq.$GLOBAL_OUTLET_ID",
                        "select" to "id,name",
                        "order" to "name.asc",
                    ),
                )
                _state.value = _state.value.copy(
                    outlets = outletRows.map { el ->
                        val o = el.asJsonObject
                        OutletOption(id = o.optString("id").orEmpty(), name = o.optString("name") ?: "-")
                    },
                )
            } catch (e: Exception) {
                Log.e("PengaturanViewModel", "Gagal memuat daftar outlet", e)
                _state.value = _state.value.copy(jadwalError = friendlyError(e, loading = true))
            }

            try {
                val configRows = Postgrest.rpc("list_outlet_attendance_config").asJsonArray
                _state.value = _state.value.copy(
                    loadingJadwal = false,
                    jadwalKhusus = configRows.map { el ->
                        val o = el.asJsonObject
                        OutletSchedule(
                            outletId = o.optString("outlet_id").orEmpty(),
                            outletName = o.optString("outlet_name") ?: "Outlet tidak dikenal",
                            jamMasuk = (o.optString("jam_masuk") ?: "00:00:00").take(5),
                            jamKeluar = (o.optString("jam_keluar") ?: "00:00:00").take(5),
                            toleransiMenit = o.optInt("toleransi_menit") ?: 0,
                            radiusM = o.optInt("radius_m") ?: 0,
                            mode = o.optString("absen_window_mode") ?: "auto",
                        )
                    },
                )
            } catch (e: Exception) {
                Log.e("PengaturanViewModel", "Gagal memuat jadwal khusus outlet", e)
                _state.value = _state.value.copy(loadingJadwal = false, jadwalError = friendlyError(e, loading = true))
            }
        }
    }

    fun saveJadwalKhusus(
        outletId: String,
        jamMasuk: String,
        jamKeluar: String,
        toleransiMenit: Int,
        radiusM: Int,
        mode: String,
        onSuccess: () -> Unit = {},
    ) {
        if (outletId.isBlank()) {
            _state.value = _state.value.copy(jadwalError = "Pilih outlet terlebih dahulu.", jadwalMessage = null)
            return
        }
        val normalizedJamMasuk = normalizeTime(jamMasuk)
        val normalizedJamKeluar = normalizeTime(jamKeluar)
        val validationError = validate(normalizedJamMasuk, normalizedJamKeluar, toleransiMenit, radiusM)
        if (validationError != null) {
            _state.value = _state.value.copy(jadwalError = validationError, jadwalMessage = null)
            return
        }
        val safeJamMasuk = normalizedJamMasuk ?: return
        val safeJamKeluar = normalizedJamKeluar ?: return

        _state.value = _state.value.copy(savingJadwal = true, jadwalError = null, jadwalMessage = null)
        viewModelScope.launch {
            try {
                Postgrest.rpc(
                    "save_outlet_attendance_config",
                    JsonObject().apply {
                        addProperty("p_outlet_id", outletId)
                        addProperty("p_jam_masuk", "$safeJamMasuk:00")
                        addProperty("p_jam_keluar", "$safeJamKeluar:00")
                        addProperty("p_toleransi_menit", toleransiMenit)
                        addProperty("p_radius_m", radiusM)
                        addProperty("p_absen_window_mode", mode)
                    },
                )
                _state.value = _state.value.copy(
                    savingJadwal = false,
                    jadwalMessage = "Jadwal khusus outlet tersimpan.",
                )
                onSuccess()
                loadJadwalKhusus()
            } catch (e: Exception) {
                Log.e("PengaturanViewModel", "Gagal menyimpan jadwal khusus outlet=$outletId", e)
                _state.value = _state.value.copy(savingJadwal = false, jadwalError = friendlyError(e))
            }
        }
    }

    /** [outletId] null = reset semua pengecualian, seluruh outlet balik ke aturan pusat. */
    fun deleteJadwalKhusus(outletId: String?) {
        _state.value = _state.value.copy(savingJadwal = true, jadwalError = null, jadwalMessage = null)
        viewModelScope.launch {
            try {
                // Reset semua pakai RPC tersendiri tanpa argumen. Mengirim
                // `{"p_outlet_id": null}` ditolak PostgREST (null di-cast ke uuid) —
                // itu sebabnya tombol reset dulu selalu gagal.
                if (outletId == null) {
                    Postgrest.rpc("reset_outlet_attendance_config")
                } else {
                    Postgrest.rpc(
                        "delete_outlet_attendance_config",
                        JsonObject().apply { addProperty("p_outlet_id", outletId) },
                    )
                }
                _state.value = _state.value.copy(
                    savingJadwal = false,
                    jadwalMessage = if (outletId == null) {
                        "Semua jadwal khusus dihapus. Semua outlet mengikuti aturan pusat."
                    } else {
                        "Jadwal khusus dihapus. Outlet mengikuti aturan pusat."
                    },
                )
                loadJadwalKhusus()
            } catch (e: Exception) {
                Log.e("PengaturanViewModel", "Gagal menghapus jadwal khusus outlet=$outletId", e)
                _state.value = _state.value.copy(savingJadwal = false, jadwalError = friendlyError(e))
            }
        }
    }

    fun clearJadwalMessage() {
        _state.value = _state.value.copy(jadwalMessage = null, jadwalError = null)
    }

    /* ----------------------------------------------------------------- Helper */

    private fun validate(jamMasuk: String?, jamKeluar: String?, toleransiMenit: Int, radiusM: Int): String? = when {
        jamMasuk == null -> "Jam masuk tidak valid. Pilih jam masuk dengan format HH:mm."
        jamKeluar == null -> "Jam keluar tidak valid. Pilih jam keluar dengan format HH:mm."
        toleransiMenit < 0 -> "Toleransi tidak boleh kurang dari 0 menit."
        radiusM <= 0 -> "Radius geofence harus lebih besar dari 0 meter."
        else -> null
    }

    private fun normalizeTime(value: String): String? {
        val parts = value.trim().split(":")
        if (parts.size < 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return "%02d:%02d".format(hour, minute)
    }

    private fun friendlyError(error: Exception, loading: Boolean = false): String {
        val detail = error.message.orEmpty().lowercase()
        return when {
            error is Postgrest.PostgrestException && error.code == 401 ->
                "Sesi login sudah berakhir. Silakan login ulang."
            error is Postgrest.PostgrestException && (error.code == 403 || "42501" in detail || "row-level security" in detail) ->
                if (loading) {
                    "Akun ini tidak memiliki izin membaca pengaturan absensi. Gunakan akun admin, HR, atau regional manager."
                } else {
                    "Akun ini tidak memiliki izin mengubah pengaturan absensi. Gunakan akun admin, HR, atau regional manager."
                }
            error is Postgrest.PostgrestException && (error.code == 404 || "pgrst202" in detail) ->
                "Fungsi jadwal khusus belum terpasang di server. Jalankan plan/jadwal-khusus-outlet.sql di Supabase."
            error is IOException ->
                "Tidak dapat terhubung ke server. Periksa koneksi internet lalu coba lagi."
            // Layar ini khusus admin, jadi pesan mentah dari server lebih berguna
            // daripada "coba lagi" yang menyembunyikan penyebabnya.
            loading ->
                "Gagal memuat pengaturan. ${serverDetail(error)}".trim()
            else ->
                "Gagal menyimpan pengaturan. ${serverDetail(error)}".trim()
        }
    }

    /** Ambil field `message`/`hint` dari body error PostgREST supaya terbaca di layar. */
    private fun serverDetail(error: Exception): String {
        val raw = error.message?.trim().orEmpty()
        if (raw.isEmpty()) return "Coba lagi."
        val parsed = runCatching {
            JsonParser.parseString(raw).asJsonObject.optString("message")
        }.getOrNull()
        return (parsed ?: raw).take(200)
    }

    private companion object {
        const val GLOBAL_OUTLET_ID = "00000000-0000-0000-0000-000000000000"
    }
}
