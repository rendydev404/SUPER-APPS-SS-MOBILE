package com.sukashawarma.superapp.presentation.absensi.cuti

import com.sukashawarma.superapp.data.remote.LampiranOutbox
import java.io.File
import java.util.UUID
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonObject
import com.sukashawarma.superapp.data.remote.HasilAksi
import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.kirimAtauAntre
import com.sukashawarma.superapp.feature.absensi.offline.CutiOffline
import com.sukashawarma.superapp.data.remote.optInt
import com.sukashawarma.superapp.data.remote.optString
import com.sukashawarma.superapp.domain.session.AppSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class CutiRequestRow(
    val id: String,
    val leaveType: String,
    val startDate: String,
    val endDate: String,
    val days: Int,
    val reason: String,
    val status: String,
)

data class CutiUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val submitting: Boolean = false,
    val submitError: String? = null,
    /** Terisi saat pengajuan hanya tersimpan di perangkat, bukan sampai ke server. */
    val pesanAntrean: String? = null,
    val rows: List<CutiRequestRow> = emptyList(),
)

val LEAVE_TYPES = listOf("tahunan", "sakit", "izin", "lainnya")

/** Pengajuan & riwayat cuti staff sendiri — tabel `leave_requests`, FK `staff_id` ->
 *  `outlet_staff.id` (skema live, dikonfirmasi via introspeksi PostgREST OpenAPI —
 *  bukan tebakan). Approval dua-tingkat (status_spv lalu status) di luar cakupan
 *  layar ini (staff hanya submit & lihat status). */
class CutiViewModel : ViewModel() {
    private val _state = MutableStateFlow(CutiUiState())
    val state: StateFlow<CutiUiState> = _state

    init { load() }

    /** Muat senyap dipakai realtime: layar yang sudah berisi data tidak boleh
     *  berkedip jadi spinner tiap kali server berubah. */
    fun refresh() = load(silent = true)

    fun load(silent: Boolean = false) {
        val staffId = AppSession.staff.value?.id
        if (staffId == null) {
            _state.value = CutiUiState(loading = false, error = "Sesi tidak valid.")
            return
        }
        _state.value = _state.value.copy(loading = !silent, error = null)
        viewModelScope.launch {
            try {
                val rows = Postgrest.select(
                    "leave_requests",
                    listOf(
                        "staff_id" to "eq.$staffId",
                        "select" to "id,leave_type,start_date,end_date,days,reason,status",
                        "order" to "created_at.desc",
                    )
                ).map { el ->
                    val o = el.asJsonObject
                    CutiRequestRow(
                        id = o.optString("id") ?: "",
                        leaveType = o.optString("leave_type") ?: "-",
                        startDate = o.optString("start_date") ?: "-",
                        endDate = o.optString("end_date") ?: "-",
                        days = o.optInt("days") ?: 0,
                        reason = o.optString("reason") ?: "",
                        status = o.optString("status") ?: "pending",
                    )
                }
                _state.value = CutiUiState(loading = false, rows = rows)
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = "Gagal memuat riwayat cuti: ${e.message}")
            }
        }
    }

    fun submit(leaveType: String, startDate: LocalDate, endDate: LocalDate, reason: String, bukti: File? = null) {
        if (_state.value.submitting) return
        val staffId = AppSession.staff.value?.id
        if (staffId == null) {
            _state.value = _state.value.copy(submitError = "Sesi tidak valid.")
            return
        }
        if (endDate.isBefore(startDate)) {
            _state.value = _state.value.copy(submitError = "Tanggal selesai tidak boleh sebelum tanggal mulai.")
            return
        }
        // Dicek di sini juga, bukan hanya tombol yang dinonaktifkan di form: pemanggil
        // lain (atau form versi berikutnya) tidak boleh bisa melewatinya.
        if (leaveType == JENIS_WAJIB_BUKTI && (bukti == null || !bukti.exists())) {
            _state.value = _state.value.copy(
                submitError = "Pengajuan sakit wajib melampirkan foto surat dokter atau obat.",
            )
            return
        }
        val days = ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
        val clientOpId = UUID.randomUUID().toString()
        _state.value = _state.value.copy(submitting = true, submitError = null)
        viewModelScope.launch {
            try {
                val hasil = kirimAtauAntre(
                    jenis = CutiOffline.JENIS,
                    payload = CutiOffline.payload(
                        staffId, leaveType, startDate.toString(), endDate.toString(), days, reason,
                    ),
                    kirim = CutiOffline::kirim,
                    clientOpId = clientOpId,
                    dibuatOleh = staffId,
                    // Hanya pengajuan sakit yang membawa bukti; jenis lain tidak mengirim
                    // foto walau sempat dipilih sebelum jenisnya diganti.
                    lampiran = bukti?.takeIf { leaveType == JENIS_WAJIB_BUKTI }?.let {
                        LampiranOutbox(it, CutiOffline.BUCKET, CutiOffline.tujuanBukti(staffId, clientOpId))
                    },
                )
                _state.value = _state.value.copy(
                    submitting = false,
                    pesanAntrean = "Pengajuan tersimpan di HP dan akan terkirim otomatis begitu ada internet."
                        .takeIf { hasil == HasilAksi.MASUK_ANTREAN },
                )
                // Riwayat dimuat ulang hanya kalau benar-benar sampai server; saat offline
                // pemuatan itu akan gagal dan menimpa pesan di atas dengan pesan galat.
                if (hasil == HasilAksi.TERKIRIM) load()
            } catch (e: Exception) {
                _state.value = _state.value.copy(submitting = false, submitError = "Gagal mengajukan cuti: ${e.message}")
            }
        }
    }

    fun clearPesanAntrean() {
        _state.value = _state.value.copy(pesanAntrean = null)
    }

    fun clearSubmitError() {
        _state.value = _state.value.copy(submitError = null)
    }
}
