package com.sukashawarma.superapp.feature.manager.ui.inventaris

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.feature.manager.data.SidakRepository
import com.sukashawarma.superapp.feature.manager.domain.ItemSidak
import com.sukashawarma.superapp.feature.manager.domain.LaporanSidak
import com.sukashawarma.superapp.feature.manager.domain.OutletSidak
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Foto yang sedang dibuka besar di dialog. */
data class FotoLaporan(val url: String?, val namaItem: String, val outlet: String, val memuat: Boolean)

data class LaporanInventarisUiState(
    val outlets: List<OutletSidak> = emptyList(),
    /** outlet_id -> laporan terbaru outlet itu. */
    val laporan: Map<String, LaporanSidak> = emptyMap(),
    val outletDibuka: String? = null,
    val pencarian: String = "",
    val foto: FotoLaporan? = null,
    val memuat: Boolean = true,
    val galat: String? = null,
) {
    /** Hanya outlet yang sudah punya laporan — sama seperti katalog admin web. */
    val outletBerlaporan: List<OutletSidak>
        get() {
            val kueri = pencarian.trim().lowercase()
            return outlets.filter { laporan.containsKey(it.id) }
                .filter { kueri.isEmpty() || it.nama.lowercase().contains(kueri) }
        }

    val laporanDibuka: LaporanSidak? get() = outletDibuka?.let { laporan[it] }

    val namaOutletDibuka: String
        get() = outlets.find { it.id == outletDibuka }?.nama ?: "Outlet"

    /** Item laporan yang tampil, tersaring oleh pencarian yang sama. */
    val itemTerlihat: List<ItemSidak>
        get() {
            val isi = laporanDibuka?.items.orEmpty()
            val kueri = pencarian.trim().lowercase()
            if (kueri.isEmpty()) return isi
            return isi.filter { "${it.nama} ${it.subsection} ${it.kondisi}".lowercase().contains(kueri) }
        }
}

/**
 * Laporan Inventaris — cermin `dashboard/reports` web, untuk peran yang membaca
 * (regional manager, admin, owner).
 *
 * Datanya sama persis dengan yang dipakai Sidak: laporan aset TERBARU tiap
 * outlet beserta itemnya. Karena itu pembacaannya menumpang
 * [SidakRepository.muat] alih-alih menyalin lima query yang sama — satu sumber,
 * satu aturan cakupan outlet.
 */
class LaporanInventarisViewModel : ViewModel() {

    private val _state = MutableStateFlow(LaporanInventarisUiState())
    val state: StateFlow<LaporanInventarisUiState> = _state

    private var pemuatan: Job? = null

    init {
        muatUlang()
    }

    fun muatUlang() {
        pemuatan?.cancel()
        pemuatan = viewModelScope.launch {
            _state.value = _state.value.copy(memuat = true, galat = null)
            try {
                val data = SidakRepository.muat()
                _state.value = _state.value.copy(
                    memuat = false,
                    outlets = data.outlets,
                    laporan = data.laporan,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("LaporanInventarisVM", "muatUlang() gagal", e)
                _state.value = _state.value.copy(
                    memuat = false,
                    galat = "Gagal memuat laporan inventaris. Coba lagi.",
                )
            }
        }
    }

    fun bukaOutlet(outletId: String) {
        _state.value = _state.value.copy(outletDibuka = outletId, pencarian = "")
    }

    fun tutupOutlet() {
        _state.value = _state.value.copy(outletDibuka = null, pencarian = "")
    }

    fun ubahPencarian(teks: String) {
        _state.value = _state.value.copy(pencarian = teks)
    }

    fun bukaFoto(item: ItemSidak) {
        val outlet = _state.value.namaOutletDibuka
        _state.value = _state.value.copy(foto = FotoLaporan(null, item.nama, outlet, memuat = true))
        viewModelScope.launch {
            val url = SidakRepository.urlFoto(item.fotoPath)
            // Dialog bisa sudah ditutup atau berpindah item saat tanda tangan tiba.
            if (_state.value.foto?.namaItem != item.nama) return@launch
            _state.value = _state.value.copy(foto = FotoLaporan(url, item.nama, outlet, memuat = false))
        }
    }

    fun tutupFoto() {
        _state.value = _state.value.copy(foto = null)
    }

    fun tutupKabar() {
        _state.value = _state.value.copy(galat = null)
    }
}
