package com.sukashawarma.superapp.feature.stok.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.feature.stok.data.StokRepository
import com.sukashawarma.superapp.feature.stok.data.model.LedgerEntry
import com.sukashawarma.superapp.feature.stok.data.model.MonitoringRow
import com.sukashawarma.superapp.feature.stok.domain.stokErrorMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class DetailUiState(
    val memuat: Boolean = true,
    val error: String? = null,
    val baris: MonitoringRow? = null,
    val mutasi: List<LedgerEntry> = emptyList(),
)

class DetailBahanViewModel : ViewModel() {

    private val _state = MutableStateFlow(DetailUiState())
    val state: StateFlow<DetailUiState> = _state

    private var outletId: String? = null
    private var bahanId: String? = null

    fun muat(outletId: String, bahanId: String) {
        this.outletId = outletId
        this.bahanId = bahanId
        viewModelScope.launch {
            _state.value = DetailUiState(memuat = true)
            try {
                // Baris monitoring diambil lintas outlet lalu disaring, karena hasilnya
                // sudah tersimpan di cache yang sama dengan yang dipakai saran transfer.
                val baris = StokRepository.monitoringLintasOutlet(bahanId)
                    .firstOrNull { it.outletId == outletId }
                val mutasi = StokRepository.ledger(outletId, bahanId)
                _state.value = DetailUiState(memuat = false, baris = baris, mutasi = mutasi)
            } catch (e: Exception) {
                _state.value = DetailUiState(memuat = false, error = stokErrorMessage(e))
            }
        }
    }

    fun cobaLagi() {
        val o = outletId ?: return
        val b = bahanId ?: return
        StokRepository.invalidate()
        muat(o, b)
    }
}
