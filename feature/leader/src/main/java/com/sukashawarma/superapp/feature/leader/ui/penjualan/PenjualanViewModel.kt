package com.sukashawarma.superapp.feature.leader.ui.penjualan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.feature.leader.data.OutletLeader
import com.sukashawarma.superapp.feature.leader.data.OutletLeaderRepository
import com.sukashawarma.superapp.feature.leader.data.PenjualanRepository
import com.sukashawarma.superapp.feature.leader.domain.PenjualanHariIni
import com.sukashawarma.superapp.feature.leader.ui.pesanGalatMuat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class PenjualanUiState(
    val cabang: List<OutletLeader> = emptyList(),
    val outletTerpilih: String? = null,
    val data: PenjualanHariIni = PenjualanHariIni.KOSONG,
    val memuat: Boolean = true,
    val galat: String? = null,
) {
    val namaCabang: String?
        get() = outletTerpilih?.let { id -> cabang.find { it.id == id }?.nama }
}

/**
 * Penjualan & Target satu cabang — cermin `app/dashboard/leader/sales/page.tsx` web.
 */
class PenjualanViewModel : ViewModel() {

    private val _state = MutableStateFlow(PenjualanUiState())
    val state: StateFlow<PenjualanUiState> = _state

    private var pemuatan: Job? = null

    init {
        muatCabang()
    }

    private fun muatCabang() {
        viewModelScope.launch {
            try {
                val ids = OutletLeaderRepository.idTerakses()
                val daftar = OutletLeaderRepository.cabang(ids)
                val utama = OutletLeaderRepository.outletUtama(
                    ids,
                    OutletLeaderRepository.outletUtamaTerdaftar(),
                )
                // Cabang utama yang dipilih lebih dulu, bukan yang pertama menurut
                // abjad: itulah cabang yang sehari-hari dipegang leader ini.
                val terpilih = utama?.takeIf { id -> daftar.any { it.id == id } }
                    ?: daftar.firstOrNull()?.id
                _state.value = _state.value.copy(cabang = daftar, outletTerpilih = terpilih)
                if (terpilih != null) muatUlang() else _state.value = _state.value.copy(memuat = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("PenjualanViewModel", "muatCabang() gagal", e)
                _state.value = _state.value.copy(
                    memuat = false,
                    galat = pesanGalatMuat(e, "daftar cabang"),
                )
            }
        }
    }

    fun pilihOutlet(outletId: String) {
        if (_state.value.outletTerpilih == outletId) return
        // Data cabang lama dibuang seketika. Membiarkannya sambil memuat cabang baru
        // berarti angka cabang A tampil di bawah nama cabang B selama satu tarikan
        // jaringan — cukup lama untuk terbaca dan dipercaya.
        _state.value = _state.value.copy(
            outletTerpilih = outletId,
            data = PenjualanHariIni.KOSONG,
        )
        muatUlang()
    }

    fun muatUlang() {
        val outletId = _state.value.outletTerpilih ?: return
        pemuatan?.cancel()
        pemuatan = viewModelScope.launch {
            _state.value = _state.value.copy(memuat = true)
            try {
                _state.value = _state.value.copy(
                    memuat = false,
                    data = PenjualanRepository.muat(outletId),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("PenjualanViewModel", "muatUlang() gagal", e)
                _state.value = _state.value.copy(
                    memuat = false,
                    galat = pesanGalatMuat(e, "data penjualan"),
                )
            }
        }
    }

    fun tutupKabar() {
        _state.value = _state.value.copy(galat = null)
    }
}
