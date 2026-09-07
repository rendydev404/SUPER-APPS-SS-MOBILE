package com.sukashawarma.superapp.feature.manager.ui.monitoring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.feature.manager.data.DataMonitoring
import com.sukashawarma.superapp.feature.manager.data.MonitoringRepository
import com.sukashawarma.superapp.feature.manager.domain.FilterKru
import com.sukashawarma.superapp.feature.manager.domain.FilterStatusPos
import com.sukashawarma.superapp.feature.manager.domain.KartuMonitoring
import com.sukashawarma.superapp.feature.manager.domain.PresetPeriode
import com.sukashawarma.superapp.feature.manager.domain.RentangTanggal
import com.sukashawarma.superapp.feature.manager.domain.lolosFilterKru
import com.sukashawarma.superapp.feature.manager.domain.lolosFilterPos
import com.sukashawarma.superapp.feature.manager.domain.stafUntukOutlet
import com.sukashawarma.superapp.feature.manager.domain.susunKartuMonitoring
import com.sukashawarma.superapp.feature.manager.domain.tanggalDalamRentang
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class MonitoringUiState(
    val preset: PresetPeriode = PresetPeriode.HARI_INI,
    val kustom: RentangTanggal? = null,
    val filterPos: FilterStatusPos = FilterStatusPos.SEMUA,
    val filterKru: FilterKru = FilterKru.SEMUA,
    val outletTerpilih: String? = null,
    val data: DataMonitoring? = null,
    val memuat: Boolean = true,
    val galat: String? = null,
) {
    val rentang: RentangTanggal get() = kustom ?: preset.rentang()
    val memakaiKustom: Boolean get() = kustom != null
    val banyakHari: Boolean get() = rentang.jumlahHari > 1

    val daftarOutlet get() = data?.outlets.orEmpty()
    val namaOutletTerpilih: String?
        get() = outletTerpilih?.let { id -> daftarOutlet.find { it.id == id }?.nama }

    /**
     * Kartu yang tampil di layar, sudah tersaring.
     *
     * Dihitung dari state, bukan disimpan: hasilnya turunan penuh dari data mentah
     * dan tiga penyaring, dan menyimpannya berarti ada dua sumber kebenaran yang
     * bisa berselisih setiap kali salah satunya lupa diperbarui.
     */
    val kartu: List<KartuMonitoring>
        get() {
            val isi = data ?: return emptyList()
            val outlets = isi.outlets.filter { outletTerpilih == null || it.id == outletTerpilih }
            return tanggalDalamRentang(rentang).flatMap { tanggal ->
                outlets.mapNotNull { outlet ->
                    val kartu = susunKartuMonitoring(
                        outlet = outlet,
                        tanggal = tanggal,
                        staf = stafUntukOutlet(isi.staf, outlet.id),
                        absen = isi.absen,
                        opname = isi.opname,
                        outlets = isi.outlets,
                        wajibChecklist = isi.wajibChecklist[outlet.id].orEmpty(),
                        sudahDicentang = isi.sudahDicentang[outlet.id].orEmpty(),
                    )
                    if (!lolosFilterPos(kartu.statusPos, filterPos)) return@mapNotNull null

                    val kru = kartu.kru.filter { lolosFilterKru(it, filterKru) }
                    // Penyaring kru yang aktif tapi tidak menyisakan siapa pun berarti
                    // kartu itu tidak menjawab pertanyaan yang sedang diajukan.
                    if (filterKru != FilterKru.SEMUA && kru.isEmpty()) return@mapNotNull null

                    // Pada rentang banyak hari, hanya kru yang benar-benar tercatat hari
                    // itu yang ditampilkan — daftar lengkap 30 kali berturut-turut tidak
                    // memberi tahu apa pun.
                    val ditampilkan = if (banyakHari) {
                        kru.filter { it.jam.isNotBlank() }
                    } else {
                        kru
                    }
                    if (banyakHari && ditampilkan.isEmpty()) return@mapNotNull null

                    kartu.copy(kru = ditampilkan)
                }
            }
        }

    /** Kartu dikelompokkan per wilayah, seperti `groupedOutlets` di web. */
    val kartuPerWilayah: List<Pair<String, List<KartuMonitoring>>>
        get() = kartu.groupBy { it.outlet.wilayah }.toList()
}

class MonitoringViewModel : ViewModel() {

    private val _state = MutableStateFlow(MonitoringUiState())
    val state: StateFlow<MonitoringUiState> = _state

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
        val rentang = if (sampai.isBefore(dari)) RentangTanggal(sampai, dari) else RentangTanggal(dari, sampai)
        _state.value = _state.value.copy(kustom = rentang)
        muatUlang()
    }

    // Ketiga penyaring ini bekerja di memori: datanya sudah di tangan, jadi
    // menggantinya tidak perlu menyentuh jaringan.
    fun pilihFilterPos(filter: FilterStatusPos) {
        _state.value = _state.value.copy(filterPos = filter)
    }

    fun pilihFilterKru(filter: FilterKru) {
        _state.value = _state.value.copy(filterKru = filter)
    }

    fun pilihOutlet(outletId: String?) {
        _state.value = _state.value.copy(outletTerpilih = outletId)
    }

    fun muatUlang() {
        pemuatan?.cancel()
        pemuatan = viewModelScope.launch {
            val rentang = _state.value.rentang
            _state.value = _state.value.copy(memuat = true, galat = null)
            try {
                val data = MonitoringRepository.muat(rentang)
                _state.value = _state.value.copy(memuat = false, galat = null, data = data)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("MonitoringViewModel", "muatUlang() gagal", e)
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
        else -> "Gagal memuat data monitoring. Coba lagi."
    }
}
