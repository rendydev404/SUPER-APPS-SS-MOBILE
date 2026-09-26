package com.sukashawarma.superapp.feature.manager.ui.waste

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.domain.model.Role
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.feature.manager.data.HalamanRiwayat
import com.sukashawarma.superapp.feature.manager.data.OutletPilihan
import com.sukashawarma.superapp.feature.manager.data.WasteRepository
import com.sukashawarma.superapp.feature.manager.domain.LaporanWaste
import com.sukashawarma.superapp.feature.manager.domain.PresetPeriode
import com.sukashawarma.superapp.feature.manager.domain.RentangTanggal
import com.sukashawarma.superapp.feature.manager.domain.RingkasanWaste
import com.sukashawarma.superapp.feature.manager.domain.StatusWaste
import com.sukashawarma.superapp.feature.manager.domain.halanganMemproses
import com.sukashawarma.superapp.feature.manager.domain.susunRingkasanWaste
import com.sukashawarma.superapp.feature.manager.domain.validasiAlasanPenolakan
import com.sukashawarma.superapp.feature.manager.data.ManagerRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class TabWaste { MENUNGGU, RIWAYAT }

data class WasteUiState(
    val tab: TabWaste = TabWaste.MENUNGGU,
    val daftarOutlet: List<OutletPilihan> = emptyList(),
    /** null berarti seluruh outlet yang boleh dilihat pengguna. */
    val outletTerpilih: String? = null,
    val preset: PresetPeriode = PresetPeriode.HARI_INI,
    val kustom: RentangTanggal? = null,
    val filterStatus: StatusWaste? = null,
    val menunggu: List<LaporanWaste> = emptyList(),
    val riwayat: HalamanRiwayat = HalamanRiwayat(emptyList(), 0, 1, 1),
    val ringkasan: RingkasanWaste = RingkasanWaste.KOSONG,
    val memuat: Boolean = true,
    /** Id laporan yang tombolnya sedang menunggu jawaban server. */
    val sedangDiproses: Set<String> = emptySet(),
    val galat: String? = null,
    val kabar: String? = null,
    val role: Role? = null,
) {
    val rentang: RentangTanggal get() = kustom ?: preset.rentang()
    val memakaiKustom: Boolean get() = kustom != null
    val namaOutletTerpilih: String?
        get() = outletTerpilih?.let { id -> daftarOutlet.find { it.id == id }?.nama }
}

class WasteViewModel : ViewModel() {

    private val _state = MutableStateFlow(WasteUiState(role = AppSession.staff.value?.role))
    val state: StateFlow<WasteUiState> = _state

    private var pemuatan: Job? = null

    init {
        muatDaftarOutlet()
        muatUlang()
    }

    fun pilihTab(tab: TabWaste) {
        if (_state.value.tab == tab) return
        _state.update { it.copy(tab = tab) }
    }

    fun pilihOutlet(outletId: String?) {
        if (_state.value.outletTerpilih == outletId) return
        // Halaman dikembalikan ke awal: halaman 5 dari penyaring lama hampir pasti
        // tidak ada di penyaring baru, dan layar kosong terlihat seperti galat.
        _state.update { it.copy(outletTerpilih = outletId, riwayat = it.riwayat.copy(halaman = 1)) }
        muatUlang()
    }

    fun pilihPreset(preset: PresetPeriode) {
        _state.update {
            it.copy(preset = preset, kustom = null, riwayat = it.riwayat.copy(halaman = 1))
        }
        muatUlang()
    }

    fun pilihRentangKustom(dari: LocalDate, sampai: LocalDate) {
        val rentang = if (sampai.isBefore(dari)) RentangTanggal(sampai, dari) else RentangTanggal(dari, sampai)
        _state.update { it.copy(kustom = rentang, riwayat = it.riwayat.copy(halaman = 1)) }
        muatUlang()
    }

    fun pilihStatus(status: StatusWaste?) {
        if (_state.value.filterStatus == status) return
        _state.update { it.copy(filterStatus = status, riwayat = it.riwayat.copy(halaman = 1)) }
        muatUlang()
    }

    fun pilihHalaman(halaman: Int) {
        val batas = _state.value.riwayat.totalHalaman
        val tujuan = halaman.coerceIn(1, maxOf(1, batas))
        if (tujuan == _state.value.riwayat.halaman) return
        _state.update { it.copy(riwayat = it.riwayat.copy(halaman = tujuan)) }
        muatHalamanRiwayat(tujuan)
    }

    private fun muatHalamanRiwayat(halaman: Int) {
        viewModelScope.launch {
            val awal = _state.value
            try {
                val riwayatBaru = WasteRepository.riwayat(
                    rentang = awal.rentang,
                    outletId = awal.outletTerpilih,
                    status = awal.filterStatus,
                    halaman = halaman,
                )
                _state.update { it.copy(riwayat = riwayatBaru) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("WasteViewModel", "muatHalamanRiwayat() gagal", e)
            }
        }
    }

    fun tutupKabar() {
        _state.update { it.copy(kabar = null, galat = null) }
    }

    private fun muatDaftarOutlet() {
        viewModelScope.launch {
            try {
                val outlet = WasteRepository.outletTerakses()
                _state.update { it.copy(daftarOutlet = outlet) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Daftar outlet hanya mengisi penyaring; kegagalannya tidak boleh
                // menutupi antrean yang justru jadi tujuan utama layar ini.
                android.util.Log.e("WasteViewModel", "muatDaftarOutlet() gagal", e)
            }
        }
    }

    fun muatUlang(silent: Boolean = false) {
        val sudahAdaData = _state.value.menunggu.isNotEmpty() || _state.value.riwayat.baris.isNotEmpty()
        val senyap = silent || sudahAdaData
        if (!senyap) {
            ManagerRepository.bersihkanCacheHarga()
        }
        pemuatan?.cancel()
        pemuatan = viewModelScope.launch {
            val awal = _state.value
            if (!senyap) {
                _state.update { it.copy(memuat = true, galat = null) }
            }
            try {
                val outletId = awal.outletTerpilih
                val rentang = awal.rentang

                coroutineScope {
                    val menungguAsync = async { WasteRepository.menunggu(outletId) }
                    val riwayatAsync = async {
                        WasteRepository.riwayat(
                            rentang = rentang,
                            outletId = outletId,
                            status = awal.filterStatus,
                            halaman = awal.riwayat.halaman,
                        )
                    }
                    val disetujuiAsync = async { WasteRepository.disetujuiPada(rentang, outletId) }

                    val menunggu = menungguAsync.await()
                    val riwayat = riwayatAsync.await()
                    val disetujui = disetujuiAsync.await()

                    _state.update {
                        it.copy(
                            memuat = false,
                            galat = null,
                            role = AppSession.staff.value?.role,
                            menunggu = menunggu,
                            riwayat = riwayat,
                            // Jumlah menunggu diambil dari antrean yang baru saja dibaca, bukan
                            // query hitung terpisah: dua angka dari dua permintaan berbeda pernah
                            // membuat badge dan isi daftar tidak sepakat di layar yang sama.
                            ringkasan = susunRingkasanWaste(disetujui, menunggu.size),
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("WasteViewModel", "muatUlang() gagal", e)
                if (!senyap) {
                    _state.update { it.copy(memuat = false, galat = pesanGalat(e)) }
                }
            } finally {
                _state.update { if (it.memuat) it.copy(memuat = false) else it }
            }
        }
    }

    fun setujui(laporan: LaporanWaste) = proses(laporan, setujui = true, alasan = null)

    fun tolak(laporan: LaporanWaste, alasan: String) {
        val keluhan = validasiAlasanPenolakan(alasan)
        if (keluhan != null) {
            _state.update { it.copy(galat = keluhan) }
            return
        }
        proses(laporan, setujui = false, alasan = alasan)
    }

    private fun proses(laporan: LaporanWaste, setujui: Boolean, alasan: String?) {
        val staff = AppSession.staff.value
        val halangan = halanganMemproses(staff?.role, staff?.id, laporan)
        if (halangan != null || staff == null) {
            _state.update { it.copy(galat = halangan ?: "Sesi tidak valid, silakan login ulang.") }
            return
        }
        // Kunci dipasang sebelum coroutine dimulai supaya ketukan ganda yang cepat
        // tidak sempat mengirim dua RPC.
        var lolos = false
        _state.update {
            lolos = laporan.id !in it.sedangDiproses
            if (lolos) it.copy(sedangDiproses = it.sedangDiproses + laporan.id) else it
        }
        if (!lolos) return

        viewModelScope.launch {
            try {
                val kalah = WasteRepository.proses(laporan.id, setujui, staff.id, alasan)
                if (kalah != null) {
                    _state.update { it.copy(galat = kalah) }
                } else {
                    _state.update { s ->
                        s.copy(
                            // Baris dibuang dari antrean lebih dulu supaya tombol tidak bisa
                            // ditekan dua kali sementara pemuatan ulang masih berjalan.
                            menunggu = s.menunggu.filterNot { it.id == laporan.id },
                            kabar = if (setujui) {
                                "Laporan waste disetujui. Stok otomatis terpotong."
                            } else {
                                "Laporan waste ditolak."
                            },
                        )
                    }
                }
                muatUlang()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("WasteViewModel", "proses() gagal", e)
                _state.update { it.copy(galat = pesanGalat(e)) }
            } finally {
                _state.update { it.copy(sedangDiproses = it.sedangDiproses - laporan.id) }
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
        else -> "Gagal memuat data waste. Coba lagi."
    }
}
