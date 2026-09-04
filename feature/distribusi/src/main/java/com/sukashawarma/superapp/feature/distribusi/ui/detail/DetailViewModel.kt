package com.sukashawarma.superapp.feature.distribusi.ui.detail

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.feature.distribusi.data.FotoBuktiStore
import com.sukashawarma.superapp.feature.distribusi.data.SuratJalanRepository
import com.sukashawarma.superapp.feature.distribusi.data.model.SuratJalanDetail
import com.sukashawarma.superapp.feature.distribusi.domain.DistribusiAkses
import com.sukashawarma.superapp.feature.distribusi.domain.SatuanDistribusi
import com.sukashawarma.superapp.feature.distribusi.domain.distribusiErrorMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Satu baris item, sudah dalam satuan distribusi dan siap dirender. */
data class BarisItemDetail(
    val nama: String,
    val qtyDikirim: Long,
    val qtyTerima: Long?,
    val satuan: String,
    val kondisi: String?,
    val catatan: String?,
    val fotoPath: String?,
    val bermasalah: Boolean,
)

data class DetailUiState(
    val memuat: Boolean = true,
    val error: String? = null,
    val detail: SuratJalanDetail? = null,
    val baris: List<BarisItemDetail> = emptyList(),
    val bolehLihatKode: Boolean = false,
    val foto: Map<String, Bitmap> = emptyMap(),
)

class DetailViewModel(private val suratJalanId: String) : ViewModel() {

    class Factory(private val suratJalanId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DetailViewModel(suratJalanId) as T
    }

    private val _state = MutableStateFlow(DetailUiState())
    val state: StateFlow<DetailUiState> = _state

    init { muat() }

    fun muat() {
        viewModelScope.launch {
            _state.value = _state.value.copy(memuat = true, error = null)
            try {
                val detail = SuratJalanRepository.detail(suratJalanId)
                if (detail == null) {
                    _state.value = _state.value.copy(
                        memuat = false,
                        error = "Surat jalan tidak ditemukan.",
                    )
                    return@launch
                }
                _state.value = _state.value.copy(
                    memuat = false,
                    detail = detail,
                    bolehLihatKode = DistribusiAkses.bolehLihatKodeVerifikasi(
                        AppSession.staff.value?.role
                    ),
                    baris = detail.items.map { item ->
                        val meta = item.bahan
                        val kurang = item.qtyTerima != null && item.qtyTerima < item.qtyDikirim
                        BarisItemDetail(
                            nama = meta?.nama ?: "Bahan tidak dikenal",
                            qtyDikirim = if (meta == null) Math.round(item.qtyDikirim)
                            else SatuanDistribusi.keTampilan(item.qtyDikirim, meta),
                            qtyTerima = item.qtyTerima?.let {
                                if (meta == null) Math.round(it)
                                else SatuanDistribusi.keTampilan(it, meta)
                            },
                            satuan = meta?.let { SatuanDistribusi.satuanTampil(it) } ?: "unit",
                            kondisi = item.kondisi,
                            catatan = item.catatan,
                            fotoPath = item.fotoPath,
                            bermasalah = item.kondisi == "rusak" || kurang,
                        )
                    },
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(memuat = false, error = distribusiErrorMessage(e))
            }
        }
    }

    /**
     * Foto diambil sesuai permintaan, bukan sekaligus saat layar dibuka: satu
     * surat jalan bisa memuat belasan foto, dan menariknya semua di jaringan
     * outlet akan membuat layar terasa macet.
     *
     * Kegagalan satu foto sengaja diabaikan diam-diam — foto yang hilang tidak
     * boleh menutup akses ke sisa dokumen.
     */
    fun muatFoto(path: String) {
        if (_state.value.foto.containsKey(path)) return
        viewModelScope.launch {
            try {
                val bytes = FotoBuktiStore.ambil(path) ?: return@launch
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@launch
                _state.value = _state.value.copy(foto = _state.value.foto + (path to bitmap))
            } catch (e: Exception) {
                // diabaikan dengan sengaja, lihat komentar di atas
            }
        }
    }
}
