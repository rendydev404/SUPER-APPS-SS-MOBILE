package com.sukashawarma.superapp.feature.leader.ui.stok

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.feature.leader.data.OutletLeader
import com.sukashawarma.superapp.feature.leader.data.OutletLeaderRepository
import com.sukashawarma.superapp.feature.leader.data.StokCabangRepository
import com.sukashawarma.superapp.feature.leader.domain.BahanCabang
import com.sukashawarma.superapp.feature.leader.domain.StatusStok
import com.sukashawarma.superapp.feature.leader.domain.saringStok
import com.sukashawarma.superapp.feature.leader.ui.pesanGalatMuat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class StokUiState(
    val cabang: List<OutletLeader> = emptyList(),
    val outletTerpilih: String? = null,
    val semua: List<BahanCabang> = emptyList(),
    val pencarian: String = "",
    val memuat: Boolean = true,
    val galat: String? = null,
) {
    val terlihat: List<BahanCabang> get() = saringStok(semua, pencarian)

    // Lencana ringkasan dihitung dari SELURUH isi cabang, bukan dari hasil
    // pencarian: "3 kritis" harus tetap berbunyi tiga walau kotak cari sedang
    // menyaring satu nama.
    val jumlahKritis: Int get() = semua.count { it.status == StatusStok.KRITIS }
    val jumlahMenipis: Int get() = semua.count { it.status == StatusStok.MENIPIS }
}

/**
 * Stok Cabang — cermin `app/dashboard/leader/stock/page.tsx` web, dengan sumber data
 * yang berbeda (lihat catatan di `StokCabangRepository`).
 */
class StokViewModel : ViewModel() {

    private val _state = MutableStateFlow(StokUiState())
    val state: StateFlow<StokUiState> = _state

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
                val terpilih = utama?.takeIf { id -> daftar.any { it.id == id } }
                    ?: daftar.firstOrNull()?.id
                _state.value = _state.value.copy(cabang = daftar, outletTerpilih = terpilih)
                if (terpilih != null) muatUlang() else _state.value = _state.value.copy(memuat = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("LeaderStokViewModel", "muatCabang() gagal", e)
                _state.value = _state.value.copy(
                    memuat = false,
                    galat = pesanGalatMuat(e, "daftar cabang"),
                )
            }
        }
    }

    fun pilihOutlet(outletId: String) {
        if (_state.value.outletTerpilih == outletId) return
        _state.value = _state.value.copy(outletTerpilih = outletId, semua = emptyList())
        muatUlang()
    }

    /** Pencarian bekerja di memori — seluruh isi cabang sudah ada di tangan. */
    fun cari(kunci: String) {
        _state.value = _state.value.copy(pencarian = kunci)
    }

    fun muatUlang() {
        val outletId = _state.value.outletTerpilih ?: return
        pemuatan?.cancel()
        pemuatan = viewModelScope.launch {
            _state.value = _state.value.copy(memuat = true)
            try {
                _state.value = _state.value.copy(
                    memuat = false,
                    semua = StokCabangRepository.bahan(outletId),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("LeaderStokViewModel", "muatUlang() gagal", e)
                _state.value = _state.value.copy(
                    memuat = false,
                    galat = pesanGalatMuat(e, "stok cabang"),
                )
            }
        }
    }

    fun tutupKabar() {
        _state.value = _state.value.copy(galat = null)
    }
}
