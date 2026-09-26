package com.sukashawarma.superapp.feature.stok.ui.pantau

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.feature.stok.data.PantauOutletRepository
import com.sukashawarma.superapp.feature.stok.data.StokRepository
import com.sukashawarma.superapp.feature.stok.domain.FilterPantau
import com.sukashawarma.superapp.feature.stok.domain.PantauOutlet
import com.sukashawarma.superapp.feature.stok.domain.PapanPantau
import com.sukashawarma.superapp.feature.stok.domain.RingkasOutlet
import com.sukashawarma.superapp.feature.stok.domain.UnitScale
import com.sukashawarma.superapp.feature.stok.domain.WilayahOutlet
import com.sukashawarma.superapp.feature.stok.domain.stokErrorMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PantauUiState(
    val memuat: Boolean = true,
    /** Pemuatan ulang di latar (polling / tarik-segarkan) — papan lama tetap tampil. */
    val menyegarkan: Boolean = false,
    val error: String? = null,
    val tidakBerhak: Boolean = false,
    val papan: PapanPantau = PapanPantau.KOSONG,
    val filter: FilterPantau = FilterPantau.SEMUA,
    val cari: String = "",
    /** Waktu data terakhir berhasil dimuat (epoch ms), untuk label "Diperbarui HH:mm". */
    val diperbarui: Long? = null,
) {
    /**
     * Outlet tersaring, dikelompokkan per wilayah. Dihitung sekali per perubahan state
     * (lazy di dalam data class), bukan tiap rekomposisi.
     */
    val perWilayah: List<Pair<WilayahOutlet, List<RingkasOutlet>>> by lazy {
        PantauOutlet.saring(papan.outlets, filter, cari)
            .groupBy { it.wilayah }
            .toSortedMap(compareBy { it.ordinal })
            .map { it.key to it.value }
    }
}

class PantauOutletViewModel : ViewModel() {

    private val _state = MutableStateFlow(PantauUiState())
    val state: StateFlow<PantauUiState> = _state

    private var muatJob: Job? = null

    init { muat(paksa = false) }

    /**
     * @param paksa buang cache saldo lebih dulu (tarik-segarkan / tombol segarkan / polling).
     *   Tanpa paksa, data yang masih dalam TTL dipakai ulang tanpa menyentuh jaringan.
     */
    fun muat(paksa: Boolean) {
        if (muatJob?.isActive == true) return
        muatJob = viewModelScope.launch {
            val adaData = _state.value.diperbarui != null
            _state.value = _state.value.copy(
                memuat = !adaData,
                menyegarkan = adaData,
                error = if (adaData) _state.value.error else null,
            )
            try {
                if (paksa) PantauOutletRepository.invalidateSaldo()
                val outlets = StokRepository.accessibleOutlets()
                if (outlets.isEmpty()) {
                    _state.value = _state.value.copy(memuat = false, menyegarkan = false, tidakBerhak = true)
                    return@launch
                }
                val nama = outlets.associate { it.id to it.name }
                val marquee = outlets.associate { it.id to it.marqueeWarningThreshold }
                val baris = PantauOutletRepository.semuaOutlet(nama)
                // Normalisasi satuan & agregasi ±1.150 baris: di luar main thread.
                val papan = withContext(Dispatchers.Default) {
                    PantauOutlet.susun(
                        baris = baris,
                        outlets = outlets.map { it.id to it.name },
                        marquee = { marquee[it] ?: UnitScale.DEFAULT_MARQUEE_WARNING },
                    )
                }
                _state.value = _state.value.copy(
                    memuat = false,
                    menyegarkan = false,
                    error = null,
                    tidakBerhak = false,
                    papan = papan,
                    diperbarui = System.currentTimeMillis(),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Gagal saat menyegarkan: papan lama tetap tampil, pesan hanya di bilah.
                _state.value = _state.value.copy(memuat = false, menyegarkan = false, error = stokErrorMessage(e))
            }
        }
    }

    fun segarkan() {
        muatJob?.cancel()
        muat(paksa = true)
    }

    fun pilihFilter(filter: FilterPantau) {
        val sekarang = _state.value.filter
        _state.value = _state.value.copy(filter = if (sekarang == filter) FilterPantau.SEMUA else filter)
    }

    fun ubahCari(teks: String) {
        _state.value = _state.value.copy(cari = teks)
    }
}
