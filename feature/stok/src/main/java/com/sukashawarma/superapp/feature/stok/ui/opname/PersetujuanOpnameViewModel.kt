package com.sukashawarma.superapp.feature.stok.ui.opname

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.feature.stok.data.OpnameRepository
import com.sukashawarma.superapp.feature.stok.data.StokRepository
import com.sukashawarma.superapp.feature.stok.data.model.OpnameHeader
import com.sukashawarma.superapp.feature.stok.domain.Approver
import com.sukashawarma.superapp.feature.stok.domain.stokErrorMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class PersetujuanOpnameUiState(
    val antrean: List<OpnameHeader> = emptyList(),
    val bolehMemutuskan: Boolean = false,
    val memuat: Boolean = true,
    /** id opname yang sedang diproses, supaya hanya tombolnya yang terkunci. */
    val sedangDiproses: String? = null,
    /** id opname yang kotak alasan penolakannya sedang terbuka. */
    val menolak: String? = null,
    val alasanTolak: String = "",
    val pesan: String? = null,
    val error: String? = null,
)

/**
 * Antrean persetujuan opname — cermin `app/stok/opname-approval/page.tsx` web.
 *
 * Opname masuk ke sini lewat `set_opname_pending`, dan `approve_opname` langsung
 * memfinalisasi begitu disetujui — jadi tidak ada tahap terpisah "setujui lalu
 * finalisasi" seperti yang mungkin diduga dari namanya.
 */
class PersetujuanOpnameViewModel : ViewModel() {

    private val _state = MutableStateFlow(PersetujuanOpnameUiState())
    val state: StateFlow<PersetujuanOpnameUiState> = _state

    init {
        muatUlang()
    }

    fun muatUlang() {
        viewModelScope.launch {
            _state.value = _state.value.copy(memuat = true, error = null)
            try {
                // Cakupan outlet diambil dari daftar yang sama dengan layar lain, yang
                // sumbernya `accessible_outlet_ids()` — bukan daftar role di aplikasi.
                val outletIds = StokRepository.accessibleOutlets().map { it.id }
                val antrean = OpnameRepository.menungguPersetujuan(outletIds)
                _state.value = _state.value.copy(
                    memuat = false,
                    antrean = antrean,
                    bolehMemutuskan = Approver.bolehApproveOpname(AppSession.staff.value?.role),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("PersetujuanOpnameVM", "muatUlang() gagal", e)
                _state.value = _state.value.copy(memuat = false, error = stokErrorMessage(e))
            }
        }
    }

    fun bukaPenolakan(opnameId: String) {
        _state.value = _state.value.copy(menolak = opnameId, alasanTolak = "")
    }

    fun tutupPenolakan() {
        _state.value = _state.value.copy(menolak = null, alasanTolak = "")
    }

    fun ubahAlasanTolak(teks: String) {
        _state.value = _state.value.copy(alasanTolak = teks)
    }

    fun bersihkanPesan() {
        _state.value = _state.value.copy(pesan = null, error = null)
    }

    fun setujui(opname: OpnameHeader) {
        val pengguna = AppSession.staff.value?.id ?: return
        if (_state.value.sedangDiproses != null) return
        viewModelScope.launch {
            _state.value = _state.value.copy(sedangDiproses = opname.id, error = null, pesan = null)
            try {
                OpnameRepository.setujui(opname.id, pengguna)
                StokRepository.invalidate()
                _state.value = _state.value.copy(
                    sedangDiproses = null,
                    // Barisnya dibuang lokal supaya antrean langsung terlihat berkurang,
                    // tidak menunggu putaran muat ulang.
                    antrean = _state.value.antrean.filterNot { it.id == opname.id },
                    pesan = "Opname ${opname.outletName.orEmpty()} disetujui dan difinalisasi.",
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("PersetujuanOpnameVM", "setujui() gagal", e)
                _state.value = _state.value.copy(sedangDiproses = null, error = stokErrorMessage(e))
            }
        }
    }

    fun tolak(opname: OpnameHeader) {
        val pengguna = AppSession.staff.value?.id ?: return
        val alasan = _state.value.alasanTolak.trim()
        if (alasan.isEmpty()) {
            _state.value = _state.value.copy(error = "Alasan penolakan wajib diisi.")
            return
        }
        if (_state.value.sedangDiproses != null) return
        viewModelScope.launch {
            _state.value = _state.value.copy(sedangDiproses = opname.id, error = null, pesan = null)
            try {
                OpnameRepository.tolak(opname.id, pengguna, alasan)
                StokRepository.invalidate()
                _state.value = _state.value.copy(
                    sedangDiproses = null,
                    menolak = null,
                    alasanTolak = "",
                    antrean = _state.value.antrean.filterNot { it.id == opname.id },
                    pesan = "Opname ditolak. Kru perlu menghitung ulang.",
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("PersetujuanOpnameVM", "tolak() gagal", e)
                _state.value = _state.value.copy(sedangDiproses = null, error = stokErrorMessage(e))
            }
        }
    }
}
