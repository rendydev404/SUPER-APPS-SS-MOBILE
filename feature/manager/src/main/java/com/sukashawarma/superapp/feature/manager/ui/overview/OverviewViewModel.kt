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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
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
    private var penyegaran: Job? = null

    private companion object {
        /**
         * Jeda peredam sebelum langganan realtime memuat ulang ringkasan.
         *
         * `orders` berdetak tiap kali kasir mana pun menutup pesanan; saat jam sibuk
         * itu bisa beberapa kali per detik lintas outlet. Tanpa peredam, satu ledakan
         * transaksi berubah menjadi belasan pemuatan penuh beruntun.
         */
        const val JEDA_REALTIME_MS = 2_000L
    }

    init {
        muatUlang()
    }

    fun pilihPreset(preset: PresetPeriode) {
        if (_state.value.preset == preset && !_state.value.memakaiKustom) return
        _state.update { it.copy(preset = preset, kustom = null) }
        muatUlang()
    }

    /** Muat ulang atas permintaan langganan realtime, diredam dan tanpa indikator. */
    fun segarkanDariRealtime() {
        penyegaran?.cancel()
        penyegaran = viewModelScope.launch {
            delay(JEDA_REALTIME_MS)
            penyegaran = null
            muatUlang(silent = true)
        }
    }

    fun pilihRentangKustom(dari: LocalDate, sampai: LocalDate) {
        // Rentang terbalik akan menghasilkan query yang selalu kosong, bukan galat —
        // jadi tukar di sini supaya layar tidak diam-diam menampilkan nol.
        val rentang = if (sampai.isBefore(dari)) RentangTanggal(sampai, dari) else RentangTanggal(dari, sampai)
        _state.update { it.copy(kustom = rentang) }
        muatUlang()
    }

    fun muatUlang(silent: Boolean = false) {
        // Perbandingan dengan KOSONG, bukan dengan null: `ringkasan` tidak pernah null,
        // jadi pemeriksaan lama selalu benar dan menelan indikator memuat serta
        // spanduk galat untuk layar yang sebenarnya masih kosong.
        val sudahAdaData = _state.value.ringkasan != RingkasanArea.KOSONG
        val senyap = silent || sudahAdaData
        pemuatan?.cancel()
        pemuatan = viewModelScope.launch {
            val rentang = _state.value.rentang
            if (!senyap) {
                _state.update { it.copy(memuat = true, galat = null) }
            }
            try {
                val data = ManagerRepository.muatRingkasan(rentang, rentangSebelumnya(rentang))
                val staff = AppSession.staff.value
                _state.update {
                    it.copy(
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
                }
            } catch (e: CancellationException) {
                // Pemuatan yang digantikan pemuatan lebih baru — bukan kegagalan.
                throw e
            } catch (e: Exception) {
                android.util.Log.e("OverviewViewModel", "muatUlang() gagal", e)
                if (!senyap) {
                    _state.update { it.copy(memuat = false, galat = pesanGalat(e)) }
                }
            } finally {
                _state.update { if (it.memuat) it.copy(memuat = false) else it }
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
