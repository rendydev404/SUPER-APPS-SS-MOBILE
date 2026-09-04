package com.sukashawarma.superapp.feature.distribusi.ui.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.feature.distribusi.data.SuratJalanRepository
import com.sukashawarma.superapp.feature.distribusi.data.VerifikasiDraftStore
import com.sukashawarma.superapp.feature.distribusi.domain.bolehDiverifikasi
import com.sukashawarma.superapp.feature.distribusi.domain.distribusiErrorMessage
import com.sukashawarma.superapp.feature.distribusi.domain.sudahDiterima
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface HasilPindai {
    data object Menunggu : HasilPindai
    data class Terbuka(val suratJalanId: String) : HasilPindai
    data class Ditolak(val pesan: String) : HasilPindai
}

data class ScanUiState(
    val memproses: Boolean = false,
    val hasil: HasilPindai = HasilPindai.Menunggu,
    val kodeManual: String = "",
    val kameraGagal: String? = null,
)

class ScanQrViewModel : ViewModel() {

    private val _state = MutableStateFlow(ScanUiState())
    val state: StateFlow<ScanUiState> = _state

    fun ubahKodeManual(teks: String) {
        // Ketikan baru membatalkan pesan penolakan lama supaya tidak terlihat
        // masih berlaku untuk kode yang sedang diketik pengguna sekarang.
        _state.value = _state.value.copy(
            kodeManual = teks.uppercase(),
            hasil = HasilPindai.Menunggu,
        )
    }

    fun kirimKodeManual() {
        val kode = _state.value.kodeManual.trim()
        if (kode.isBlank()) {
            _state.value = _state.value.copy(
                hasil = HasilPindai.Ditolak("Ketik kode verifikasi terlebih dahulu."),
            )
            return
        }
        pindai(kode)
    }

    fun tandaiKameraGagal(pesan: String) {
        _state.value = _state.value.copy(kameraGagal = pesan)
    }

    /**
     * Satu pemindaian pada satu waktu. Tanpa penjaga `memproses`, penganalisis
     * kamera akan memicu belasan pencarian untuk satu kode yang sama dalam
     * sekejap, dan navigasi bisa terjadi dua kali.
     */
    fun pindai(kode: String) {
        if (_state.value.memproses) return
        if (_state.value.hasil is HasilPindai.Terbuka) return
        viewModelScope.launch {
            _state.value = _state.value.copy(memproses = true, hasil = HasilPindai.Menunggu)
            try {
                val sj = SuratJalanRepository.cariUntukVerifikasi(kode)
                val hasil = when {
                    sj == null -> HasilPindai.Ditolak(
                        "Kode \"$kode\" tidak ditemukan. Periksa lembar surat jalan."
                    )
                    sj.status?.sudahDiterima == true -> HasilPindai.Ditolak(
                        "Surat jalan ini sudah diverifikasi sebelumnya. Lihat di Riwayat."
                    )
                    sj.status?.bolehDiverifikasi != true -> HasilPindai.Ditolak(
                        "Surat jalan ini belum dikirim gudang pusat, jadi belum bisa diterima."
                    )
                    else -> {
                        VerifikasiDraftStore.tandaiTerbuka(sj.id)
                        HasilPindai.Terbuka(sj.id)
                    }
                }
                _state.value = _state.value.copy(memproses = false, hasil = hasil)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    memproses = false,
                    hasil = HasilPindai.Ditolak(distribusiErrorMessage(e)),
                )
            }
        }
    }
}
