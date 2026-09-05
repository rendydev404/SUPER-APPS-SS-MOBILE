package com.sukashawarma.superapp.feature.manager.ui.laporan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.feature.manager.data.LaporanRepository
import com.sukashawarma.superapp.feature.manager.data.OutletPilihan
import com.sukashawarma.superapp.feature.manager.data.WasteRepository
import com.sukashawarma.superapp.feature.manager.domain.AnalitikLaporan
import com.sukashawarma.superapp.feature.manager.domain.FilterChannel
import com.sukashawarma.superapp.feature.manager.domain.FilterPembayaran
import com.sukashawarma.superapp.feature.manager.domain.PresetLaporan
import com.sukashawarma.superapp.feature.manager.domain.RentangTanggal
import com.sukashawarma.superapp.feature.manager.domain.susunAnalitikLaporan
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class LaporanUiState(
    val preset: PresetLaporan = PresetLaporan.HARI_INI,
    val kustom: RentangTanggal? = null,
    val channel: FilterChannel = FilterChannel.SEMUA,
    val pembayaran: FilterPembayaran = FilterPembayaran.SEMUA,
    val daftarOutlet: List<OutletPilihan> = emptyList(),
    val outletTerpilih: String? = null,
    val analitik: AnalitikLaporan = AnalitikLaporan.KOSONG,
    val memuat: Boolean = true,
    val galat: String? = null,
) {
    val rentang: RentangTanggal
        get() = if (preset == PresetLaporan.KUSTOM) {
            kustom ?: PresetLaporan.HARI_INI.rentang()
        } else {
            preset.rentang()
        }

    val labelRentang: String
        get() = if (preset == PresetLaporan.KUSTOM && kustom != null) {
            "${kustom.dari} - ${kustom.sampai}"
        } else {
            preset.label
        }

    val namaOutletTerpilih: String?
        get() = outletTerpilih?.let { id -> daftarOutlet.find { it.id == id }?.nama }
}

class LaporanViewModel : ViewModel() {

    private val _state = MutableStateFlow(LaporanUiState())
    val state: StateFlow<LaporanUiState> = _state

    private var pemuatan: Job? = null

    init {
        muatDaftarOutlet()
        muatUlang()
    }

    fun pilihPreset(preset: PresetLaporan) {
        // Memilih "Kustom" tanpa tanggal belum mengubah apa pun; layar membuka
        // pemilih tanggal dan pemuatan menunggu sampai rentangnya benar-benar ada.
        if (preset == PresetLaporan.KUSTOM && _state.value.kustom == null) {
            _state.value = _state.value.copy(preset = preset)
            return
        }
        if (_state.value.preset == preset) return
        _state.value = _state.value.copy(preset = preset)
        muatUlang()
    }

    fun pilihRentangKustom(dari: LocalDate, sampai: LocalDate) {
        val rentang = if (sampai.isBefore(dari)) RentangTanggal(sampai, dari) else RentangTanggal(dari, sampai)
        _state.value = _state.value.copy(preset = PresetLaporan.KUSTOM, kustom = rentang)
        muatUlang()
    }

    fun pilihChannel(channel: FilterChannel) {
        if (_state.value.channel == channel) return
        _state.value = _state.value.copy(channel = channel)
        muatUlang()
    }

    fun pilihPembayaran(pembayaran: FilterPembayaran) {
        if (_state.value.pembayaran == pembayaran) return
        _state.value = _state.value.copy(pembayaran = pembayaran)
        muatUlang()
    }

    fun pilihOutlet(outletId: String?) {
        if (_state.value.outletTerpilih == outletId) return
        _state.value = _state.value.copy(outletTerpilih = outletId)
        muatUlang()
    }

    private fun muatDaftarOutlet() {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(daftarOutlet = WasteRepository.outletTerakses())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Penyaring outlet adalah pelengkap; kegagalannya tidak boleh menutupi angka.
                android.util.Log.e("LaporanViewModel", "muatDaftarOutlet() gagal", e)
            }
        }
    }

    fun muatUlang() {
        pemuatan?.cancel()
        pemuatan = viewModelScope.launch {
            val awal = _state.value
            _state.value = awal.copy(memuat = true, galat = null)
            try {
                val pesanan = LaporanRepository.pesanan(
                    rentang = awal.rentang,
                    channel = awal.channel,
                    pembayaran = awal.pembayaran,
                    outletId = awal.outletTerpilih,
                )
                _state.value = _state.value.copy(
                    memuat = false,
                    galat = null,
                    analitik = susunAnalitikLaporan(pesanan),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("LaporanViewModel", "muatUlang() gagal", e)
                _state.value = _state.value.copy(memuat = false, galat = pesanGalat(e))
            }
        }
    }

    private fun pesanGalat(e: Exception): String = when (e) {
        is java.net.UnknownHostException ->
            "Tidak ada koneksi internet. Periksa jaringan Wi-Fi/data seluler Anda."
        is java.net.SocketTimeoutException ->
            "Server tidak merespons (koneksi lambat). Coba lagi."
        is java.io.IOException ->
            "Gagal terhubung ke server. Periksa koneksi internet."
        else -> "Gagal memuat laporan. Coba lagi."
    }
}
