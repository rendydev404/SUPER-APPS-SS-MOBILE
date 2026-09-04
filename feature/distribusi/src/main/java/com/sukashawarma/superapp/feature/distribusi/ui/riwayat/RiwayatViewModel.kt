package com.sukashawarma.superapp.feature.distribusi.ui.riwayat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.feature.distribusi.data.SuratJalanRepository
import com.sukashawarma.superapp.feature.distribusi.data.model.SuratJalanRingkas
import com.sukashawarma.superapp.feature.distribusi.domain.distribusiErrorMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class RiwayatUiState(
    val memuat: Boolean = true,
    val error: String? = null,
    val daftar: List<SuratJalanRingkas> = emptyList(),
)

class RiwayatViewModel : ViewModel() {

    private val _state = MutableStateFlow(RiwayatUiState())
    val state: StateFlow<RiwayatUiState> = _state

    init { muat() }

    fun muat(paksa: Boolean = false) {
        viewModelScope.launch {
            _state.value = _state.value.copy(memuat = true, error = null)
            if (paksa) SuratJalanRepository.invalidate()
            try {
                _state.value = RiwayatUiState(memuat = false, daftar = SuratJalanRepository.riwayat())
            } catch (e: Exception) {
                _state.value = _state.value.copy(memuat = false, error = distribusiErrorMessage(e))
            }
        }
    }

    /** Membersihkan galat setelah ditampilkan lewat snackbar — lihat RiwayatScreen. */
    fun bersihkanPesan() {
        _state.value = _state.value.copy(error = null)
    }
}
