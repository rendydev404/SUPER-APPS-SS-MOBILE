package com.sukashawarma.superapp.feature.leader.ui.ringkasan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.feature.leader.data.RingkasanRepository
import com.sukashawarma.superapp.feature.leader.domain.RingkasanLeader
import com.sukashawarma.superapp.feature.leader.ui.pesanGalatMuat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class RingkasanUiState(
    val data: RingkasanLeader = RingkasanLeader.KOSONG,
    val memuat: Boolean = true,
    val galat: String? = null,
    /** true hanya sebelum pemuatan pertama selesai — memisahkan "belum tahu" dari "memang nol". */
    val pertamaKali: Boolean = true,
)

class RingkasanViewModel : ViewModel() {

    private val _state = MutableStateFlow(RingkasanUiState())
    val state: StateFlow<RingkasanUiState> = _state

    private var pemuatan: Job? = null

    init {
        muatUlang()
    }

    /**
     * Pemuatan sebelumnya dibatalkan lebih dulu. Tanpa itu, dua event realtime yang
     * datang beruntun bisa membuat balasan yang lebih lambat menimpa yang lebih baru.
     */
    fun muatUlang() {
        pemuatan?.cancel()
        pemuatan = viewModelScope.launch {
            _state.value = _state.value.copy(memuat = true, galat = null)
            try {
                _state.value = RingkasanUiState(
                    data = RingkasanRepository.muat(),
                    memuat = false,
                    pertamaKali = false,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("RingkasanViewModel", "muatUlang() gagal", e)
                _state.value = _state.value.copy(
                    memuat = false,
                    pertamaKali = false,
                    galat = pesanGalatMuat(e, "ringkasan cabang"),
                )
            }
        }
    }
}
