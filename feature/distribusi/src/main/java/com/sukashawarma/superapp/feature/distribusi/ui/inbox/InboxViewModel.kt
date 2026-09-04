package com.sukashawarma.superapp.feature.distribusi.ui.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.feature.distribusi.data.SuratJalanRepository
import com.sukashawarma.superapp.feature.distribusi.data.model.SuratJalanRingkas
import com.sukashawarma.superapp.feature.distribusi.domain.DistribusiAkses
import com.sukashawarma.superapp.feature.distribusi.domain.distribusiErrorMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class InboxUiState(
    val memuat: Boolean = true,
    val error: String? = null,
    val daftar: List<SuratJalanRingkas> = emptyList(),
    val bolehVerifikasi: Boolean = false,
    val namaOutlet: String = "",
)

class InboxViewModel : ViewModel() {

    private val _state = MutableStateFlow(InboxUiState())
    val state: StateFlow<InboxUiState> = _state

    init {
        val staff = AppSession.staff.value
        _state.value = _state.value.copy(
            bolehVerifikasi = DistribusiAkses.bolehVerifikasi(staff?.role),
            namaOutlet = staff?.outletName.orEmpty(),
        )
        muat()
    }

    fun muat(paksa: Boolean = false) {
        viewModelScope.launch {
            _state.value = _state.value.copy(memuat = true, error = null)
            if (paksa) SuratJalanRepository.invalidate()
            try {
                _state.value = _state.value.copy(memuat = false, daftar = SuratJalanRepository.inbox())
            } catch (e: Exception) {
                _state.value = _state.value.copy(memuat = false, error = distribusiErrorMessage(e))
            }
        }
    }

    fun bersihkanPesan() {
        _state.value = _state.value.copy(error = null)
    }
}
