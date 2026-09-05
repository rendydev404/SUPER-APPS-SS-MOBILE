package com.sukashawarma.superapp.feature.manager.ui.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.domain.model.Role
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.feature.manager.data.ManagerRepository
import com.sukashawarma.superapp.feature.manager.domain.PresetPeriode
import com.sukashawarma.superapp.feature.manager.domain.RentangTanggal
import com.sukashawarma.superapp.feature.manager.domain.RingkasanArea
import com.sukashawarma.superapp.feature.manager.domain.hitungKerugianWaste
import com.sukashawarma.superapp.feature.manager.domain.rentangSebelumnya
import com.sukashawarma.superapp.feature.manager.domain.susunRingkasanArea
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class OverviewUiState(
    val preset: PresetPeriode = PresetPeriode.HARI_INI,
    /** Terisi saat pengguna memilih rentang sendiri; saat itu [preset] diabaikan. */
    val kustom: RentangTanggal? = null,
    val memuat: Boolean = true,
    val galat: String? = null,
    val ringkasan: RingkasanArea = RingkasanArea.KOSONG,
    val role: Role? = null,
    val nama: String? = null,
) {
    val rentang: RentangTanggal get() = kustom ?: preset.rentang()
    val memakaiKustom: Boolean get() = kustom != null
}

class OverviewViewModel : ViewModel() {

    private val _state = MutableStateFlow(
        OverviewUiState(
            role = AppSession.staff.value?.role,
            nama = AppSession.staff.value?.name,
        )
    )
    val state: StateFlow<OverviewUiState> = _state

    /**
     * Pemuatan yang sedang berjalan. Realtime bisa memanggil [muatUlang] beberapa kali
     * beruntun saat kasir menutup banyak pesanan sekaligus; tanpa pembatalan ini,
     * balasan lama yang datang belakangan bisa menimpa balasan baru.
     */
    private var pemuatan: Job? = null

    init {
        muatUlang()
    }

    fun pilihPreset(preset: PresetPeriode) {
        if (_state.value.preset == preset && !_state.value.memakaiKustom) return
        _state.value = _state.value.copy(preset = preset, kustom = null)
        muatUlang()
    }

    fun pilihRentangKustom(dari: LocalDate, sampai: LocalDate) {
        // Rentang terbalik akan menghasilkan query yang selalu kosong, bukan galat —
        // jadi tukar di sini supaya layar tidak diam-diam menampilkan nol.
        val rentang = if (sampai.isBefore(dari)) RentangTanggal(sampai, dari) else RentangTanggal(dari, sampai)
        _state.value = _state.value.copy(kustom = rentang)
        muatUlang()
    }

    fun muatUlang() {
        pemuatan?.cancel()
        pemuatan = viewModelScope.launch {
            val sekarang = _state.value
            val rentang = sekarang.rentang
            _state.value = sekarang.copy(memuat = true, galat = null)
            try {
                val data = ManagerRepository.muatRingkasan(rentang, rentangSebelumnya(rentang))
                val staff = AppSession.staff.value
                _state.value = _state.value.copy(
                    memuat = false,
                    galat = null,
                    role = staff?.role,
                    nama = staff?.name,
                    ringkasan = susunRingkasanArea(
                        outlets = data.outlets,
                        pesanan = data.pesanan,
                        pesananSebelumnya = data.pesananSebelumnya,
                        absenMasuk = data.absenMasuk,
                        pemetaanAm = data.pemetaanAreaManager,
                        kerugianWaste = hitungKerugianWaste(data.waste, data.hargaBahan),
                        wasteMenungguPersetujuan = data.wasteMenungguPersetujuan,
                        rolePengguna = staff?.role,
                        namaPengguna = staff?.name,
                    ),
                )
            } catch (e: CancellationException) {
                // Pemuatan yang digantikan pemuatan lebih baru — bukan kegagalan.
                throw e
            } catch (e: Exception) {
                android.util.Log.e("OverviewViewModel", "muatUlang() gagal", e)
                _state.value = _state.value.copy(memuat = false, galat = pesanGalat(e))
            }
        }
    }

    /** Sama seperti AppSession: pesan dibedakan per penyebab supaya pengguna tidak
     *  disuruh memeriksa jaringan padahal masalahnya bukan di sana. */
    private fun pesanGalat(e: Exception): String = when (e) {
        is java.net.UnknownHostException ->
            "Tidak ada koneksi internet. Periksa jaringan Wi-Fi/data seluler Anda."
        is java.net.SocketTimeoutException ->
            "Server tidak merespons (koneksi lambat). Coba lagi."
        is java.io.IOException ->
            "Gagal terhubung ke server. Periksa koneksi internet."
        else -> "Gagal memuat ringkasan area. Coba lagi."
    }
}
