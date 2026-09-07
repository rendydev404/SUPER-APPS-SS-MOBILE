package com.sukashawarma.superapp.feature.manager.ui.persetujuan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.feature.manager.data.PersetujuanRepository
import com.sukashawarma.superapp.feature.manager.domain.PengajuanBypass
import com.sukashawarma.superapp.feature.manager.domain.PengajuanVoid
import com.sukashawarma.superapp.feature.manager.domain.PresetPeriode
import com.sukashawarma.superapp.feature.manager.domain.RentangTanggal
import com.sukashawarma.superapp.feature.manager.domain.TabPersetujuan
import com.sukashawarma.superapp.feature.manager.domain.saringPeriode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class PersetujuanUiState(
    val tab: TabPersetujuan = TabPersetujuan.VOID,
    val preset: PresetPeriode = PresetPeriode.HARI_INI,
    val kustom: RentangTanggal? = null,
    val semuaVoid: List<PengajuanVoid> = emptyList(),
    val semuaBypass: List<PengajuanBypass> = emptyList(),
    val memuat: Boolean = true,
    val sedangDiproses: Set<String> = emptySet(),
    val galat: String? = null,
    val kabar: String? = null,
) {
    val rentang: RentangTanggal get() = kustom ?: preset.rentang()
    val memakaiKustom: Boolean get() = kustom != null

    val void: List<PengajuanVoid> get() = saringPeriode(semuaVoid, rentang) { it.dibuatPada }
    val bypass: List<PengajuanBypass> get() = saringPeriode(semuaBypass, rentang) { it.dibuatPada }

    /** Lencana tab memakai jumlah SEBELUM penyaring periode, seperti web. */
    val jumlahVoid: Int get() = semuaVoid.size
    val jumlahBypass: Int get() = semuaBypass.size
}

class PersetujuanViewModel : ViewModel() {

    private val _state = MutableStateFlow(PersetujuanUiState())
    val state: StateFlow<PersetujuanUiState> = _state

    private var pemuatan: Job? = null

    init {
        muatUlang()
    }

    fun pilihTab(tab: TabPersetujuan) {
        if (_state.value.tab == tab) return
        _state.value = _state.value.copy(tab = tab)
    }

    // Penyaring periode bekerja di memori: kedua antrean hanya berisi pengajuan yang
    // masih menunggu, jadi seluruhnya sudah di tangan.
    fun pilihPreset(preset: PresetPeriode) {
        _state.value = _state.value.copy(preset = preset, kustom = null)
    }

    fun pilihRentangKustom(dari: LocalDate, sampai: LocalDate) {
        val rentang = if (sampai.isBefore(dari)) RentangTanggal(sampai, dari) else RentangTanggal(dari, sampai)
        _state.value = _state.value.copy(kustom = rentang)
    }

    fun tutupKabar() {
        _state.value = _state.value.copy(kabar = null, galat = null)
    }

    fun muatUlang() {
        pemuatan?.cancel()
        pemuatan = viewModelScope.launch {
            _state.value = _state.value.copy(memuat = true, galat = null)
            try {
                val data = PersetujuanRepository.muat()
                _state.value = _state.value.copy(
                    memuat = false,
                    galat = null,
                    semuaVoid = data.void,
                    semuaBypass = data.bypass,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("PersetujuanViewModel", "muatUlang() gagal", e)
                _state.value = _state.value.copy(memuat = false, galat = pesanGalat(e))
            }
        }
    }

    fun prosesBypass(pengajuan: PengajuanBypass, setujui: Boolean) {
        if (pengajuan.id in _state.value.sedangDiproses) return
        viewModelScope.launch {
            _state.value = _state.value.copy(
                sedangDiproses = _state.value.sedangDiproses + pengajuan.id,
            )
            try {
                val kalah = PersetujuanRepository.prosesBypass(pengajuan.id, setujui)
                if (kalah != null) {
                    _state.value = _state.value.copy(galat = kalah)
                } else {
                    _state.value = _state.value.copy(
                        // Baris dibuang lebih dulu supaya tombol tidak bisa ditekan dua
                        // kali selagi pemuatan ulang masih berjalan.
                        semuaBypass = _state.value.semuaBypass.filterNot { it.id == pengajuan.id },
                        kabar = if (setujui) {
                            "Pengajuan bypass POS disetujui."
                        } else {
                            "Pengajuan bypass POS ditolak."
                        },
                    )
                }
                muatUlang()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("PersetujuanViewModel", "prosesBypass() gagal", e)
                _state.value = _state.value.copy(galat = pesanGalat(e))
            } finally {
                _state.value = _state.value.copy(
                    sedangDiproses = _state.value.sedangDiproses - pengajuan.id,
                )
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
        else -> "Gagal memuat antrean persetujuan. Coba lagi."
    }
}
