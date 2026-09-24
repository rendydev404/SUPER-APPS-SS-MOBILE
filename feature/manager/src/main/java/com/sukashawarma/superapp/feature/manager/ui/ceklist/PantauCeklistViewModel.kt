package com.sukashawarma.superapp.feature.manager.ui.ceklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.data.remote.adalahGalatJaringan
import com.sukashawarma.superapp.data.remote.pesanGalatJaringan
import com.sukashawarma.superapp.domain.model.Role
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.feature.manager.data.CeklistHarianRepository
import com.sukashawarma.superapp.feature.manager.data.OutletPilihan
import com.sukashawarma.superapp.feature.manager.domain.FilterCeklist
import com.sukashawarma.superapp.feature.manager.domain.LaporanCeklist
import com.sukashawarma.superapp.feature.manager.domain.meninjauCeklist
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

data class PantauCeklistUiState(
    val tanggal: LocalDate = CeklistHarianRepository.hariIni(),
    val outlets: List<OutletPilihan> = emptyList(),
    val laporan: Map<String, LaporanCeklist> = emptyMap(),
    val filter: FilterCeklist = FilterCeklist.SEMUA,
    /** Outlet yang laporannya sedang dibuka. */
    val outletDibuka: String? = null,
    val tanggapan: String = "",
    /** path foto -> URL bertanda tangan. */
    val urlFoto: Map<String, String> = emptyMap(),
    val memuat: Boolean = true,
    val meninjau: Boolean = false,
    val role: Role? = null,
    val galat: String? = null,
    /** Galat pemuatan — bertahan setelah snackbar [galat] ditutup. */
    val galatMuat: String? = null,
    val kabar: String? = null,
) {
    val hariIni: Boolean get() = tanggal == CeklistHarianRepository.hariIni()

    val bolehMeninjau: Boolean get() = meninjauCeklist(role)

    val jumlahDicek: Int get() = laporan.size
    val jumlahPerhatian: Int get() = laporan.values.count { it.perluPerhatian }
    val jumlahBelumDitinjau: Int get() = laporan.values.count { !it.sudahDitinjau }
    val jumlahBelumDicek: Int get() = outlets.count { it.id !in laporan }

    /**
     * Urutan daftar: yang butuh tindakan paling atas. Laporan bermasalah yang
     * belum ditinjau dulu, lalu laporan lain yang belum ditinjau, lalu yang sudah,
     * dan outlet yang belum dicek di dasar.
     */
    val outletTerlihat: List<OutletPilihan>
        get() = outlets
            .filter { o ->
                val l = laporan[o.id]
                when (filter) {
                    FilterCeklist.SEMUA -> true
                    FilterCeklist.PERHATIAN -> l?.perluPerhatian == true
                    FilterCeklist.BELUM_DICEK -> l == null
                    FilterCeklist.BELUM_DITINJAU -> l != null && !l.sudahDitinjau
                }
            }
            .sortedWith(
                compareBy<OutletPilihan> { o ->
                    val l = laporan[o.id]
                    when {
                        l == null -> 3
                        !l.sudahDitinjau && l.perluPerhatian -> 0
                        !l.sudahDitinjau -> 1
                        else -> 2
                    }
                }.thenBy { it.nama },
            )

    val laporanDibuka: LaporanCeklist? get() = outletDibuka?.let { laporan[it] }

    val namaOutletDibuka: String get() = outlets.find { it.id == outletDibuka }?.nama ?: "Outlet"

}

/**
 * Pemantauan ceklist harian oleh regional manager.
 *
 * Satu hari per tampilan, dengan geser tanggal ke belakang. Outlet yang belum
 * dicek ikut tampil — bagi RM, outlet yang TIDAK dikunjungi sama pentingnya
 * dengan temuan di outlet yang dikunjungi.
 */
class PantauCeklistViewModel : ViewModel() {

    private val _state = MutableStateFlow(PantauCeklistUiState(role = AppSession.staff.value?.role))
    val state: StateFlow<PantauCeklistUiState> = _state

    private var pemuatan: Job? = null
    private val urlDiminta: MutableSet<String> = ConcurrentHashMap.newKeySet()

    init {
        muatUlang()
    }

    fun muatUlang(silent: Boolean = false) {
        val tanggal = _state.value.tanggal
        pemuatan?.cancel()
        pemuatan = viewModelScope.launch {
            if (!silent) _state.update { it.copy(memuat = true, galat = null, galatMuat = null) }
            try {
                val data = CeklistHarianRepository.muatHari(tanggal)
                _state.update {
                    // Tanggal bisa sudah digeser lagi selagi permintaan berjalan.
                    if (it.tanggal != tanggal) it
                    else it.copy(memuat = false, outlets = data.outlets, laporan = data.laporan, galatMuat = null)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("PantauCeklistVM", "muatUlang() gagal", e)
                _state.update {
                    val pesan = if (silent) null else pesanGalatJaringan(e, "ceklist harian")
                    it.copy(memuat = false, galat = pesan, galatMuat = pesan ?: it.galatMuat)
                }
            }
        }
    }

    fun geserTanggal(hari: Long) {
        val baru = _state.value.tanggal.plusDays(hari)
        if (baru.isAfter(CeklistHarianRepository.hariIni())) return
        _state.update { it.copy(tanggal = baru, laporan = emptyMap(), outletDibuka = null) }
        muatUlang()
    }

    fun pilihFilter(filter: FilterCeklist) = _state.update { it.copy(filter = filter) }

    fun buka(outletId: String) {
        val l = _state.value.laporan[outletId] ?: return
        _state.update { it.copy(outletDibuka = outletId, tanggapan = l.tanggapanRm.orEmpty()) }
    }

    fun tutup() = _state.update { it.copy(outletDibuka = null, tanggapan = "") }

    fun ubahTanggapan(teks: String) = _state.update { it.copy(tanggapan = teks) }

    fun tutupKabar() = _state.update { it.copy(galat = null, kabar = null) }

    fun pastikanUrlFoto(path: String) {
        if (_state.value.urlFoto.containsKey(path) || !urlDiminta.add(path)) return
        viewModelScope.launch {
            val url = CeklistHarianRepository.urlFoto(path)
            if (url == null) {
                urlDiminta.remove(path)
                return@launch
            }
            _state.update { it.copy(urlFoto = it.urlFoto + (path to url)) }
        }
    }

    fun tinjau() {
        val awal = _state.value
        val laporan = awal.laporanDibuka ?: return
        if (awal.meninjau) return
        _state.update { it.copy(meninjau = true) }
        viewModelScope.launch {
            try {
                CeklistHarianRepository.tinjau(laporan.id, awal.tanggapan, laporan.diperbaruiPada, laporan.ditinjauPada)
                _state.update { it.copy(meninjau = false, outletDibuka = null, kabar = "Laporan disetujui. AM sudah diberi tahu.") }
                muatUlang(silent = true)
            } catch (e: CancellationException) {
                _state.update { it.copy(meninjau = false) }
                throw e
            } catch (e: Exception) {
                android.util.Log.e("PantauCeklistVM", "tinjau() gagal", e)
                if (CeklistHarianRepository.PESAN_VERSI_BERUBAH in e.message.orEmpty()) {
                    // AM mengirim ulang atau peninjau lain baru saja menanggapi: tampilkan
                    // versi terbaru dulu, jangan setujui isi yang belum dilihat.
                    _state.update {
                        it.copy(meninjau = false, galat = "Laporan ini baru saja diperbarui. Periksa lagi isinya sebelum menyetujui.")
                    }
                    muatUlang(silent = true)
                    return@launch
                }
                _state.update { it.copy(meninjau = false, galat = if (adalahGalatJaringan(e)) pesanGalatJaringan(e, "") else "Gagal menyetujui laporan. Coba lagi.") }
            }
        }
    }
}
