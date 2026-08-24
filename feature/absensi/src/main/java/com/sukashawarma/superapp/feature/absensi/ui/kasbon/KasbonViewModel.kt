package com.sukashawarma.superapp.presentation.absensi.kasbon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonObject
import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.optDouble
import com.sukashawarma.superapp.data.remote.optInt
import com.sukashawarma.superapp.data.remote.optString
import com.sukashawarma.superapp.domain.session.AppSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class KasbonRow(
    val id: String,
    val amount: Double,
    val remaining: Double,
    val reason: String,
    val status: String,
    val installmentMonths: Int,
)

data class KasbonUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val submitting: Boolean = false,
    val submitError: String? = null,
    val rows: List<KasbonRow> = emptyList(),
)

/** Pengajuan & riwayat kasbon staff sendiri — tabel `cash_advances`, FK `staff_id` ->
 *  `outlet_staff.id` (dikonfirmasi via introspeksi PostgREST OpenAPI, sama seperti
 *  [[CutiViewModel]]/leave_requests). `remaining` diisi server/proses pembayaran
 *  (cash_advance_payments) — saat submit awal remaining = amount. */
class KasbonViewModel : ViewModel() {
    private val _state = MutableStateFlow(KasbonUiState())
    val state: StateFlow<KasbonUiState> = _state

    init { load() }

    fun load() {
        val staffId = AppSession.staff.value?.id
        if (staffId == null) {
            _state.value = KasbonUiState(loading = false, error = "Sesi tidak valid.")
            return
        }
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                val rows = Postgrest.select(
                    "cash_advances",
                    listOf(
                        "staff_id" to "eq.$staffId",
                        "select" to "id,amount,remaining,reason,status,installment_months",
                        "order" to "created_at.desc",
                    )
                ).map { el ->
                    val o = el.asJsonObject
                    KasbonRow(
                        id = o.optString("id") ?: "",
                        amount = o.optDouble("amount") ?: 0.0,
                        remaining = o.optDouble("remaining") ?: 0.0,
                        reason = o.optString("reason") ?: "",
                        status = o.optString("status") ?: "pending",
                        installmentMonths = o.optInt("installment_months") ?: 1,
                    )
                }
                _state.value = KasbonUiState(loading = false, rows = rows)
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = "Gagal memuat riwayat kasbon: ${e.message}")
            }
        }
    }

    fun submit(amount: Double, installmentMonths: Int, reason: String) {
        val staffId = AppSession.staff.value?.id
        if (staffId == null) {
            _state.value = _state.value.copy(submitError = "Sesi tidak valid.")
            return
        }
        if (amount <= 0) {
            _state.value = _state.value.copy(submitError = "Jumlah kasbon harus lebih dari 0.")
            return
        }
        _state.value = _state.value.copy(submitting = true, submitError = null)
        viewModelScope.launch {
            try {
                Postgrest.insert(
                    "cash_advances",
                    JsonObject().apply {
                        addProperty("staff_id", staffId)
                        addProperty("amount", amount)
                        addProperty("remaining", amount)
                        addProperty("reason", reason)
                        addProperty("status", "pending")
                        addProperty("installment_months", installmentMonths)
                    }
                )
                _state.value = _state.value.copy(submitting = false)
                load()
            } catch (e: Exception) {
                _state.value = _state.value.copy(submitting = false, submitError = "Gagal mengajukan kasbon: ${e.message}")
            }
        }
    }

    fun clearSubmitError() {
        _state.value = _state.value.copy(submitError = null)
    }
}
