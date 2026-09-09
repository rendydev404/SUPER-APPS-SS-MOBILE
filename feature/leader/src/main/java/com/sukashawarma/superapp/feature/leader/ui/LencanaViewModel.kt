package com.sukashawarma.superapp.feature.leader.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.feature.leader.domain.StatusPengajuan
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Angka merah pada tab Petty Cash: pengajuan yang dananya sudah cair dan tinggal
 * diserahkan ke crew.
 *
 * Hidup terpisah dari layar Petty Cash supaya lencananya tetap benar tanpa layar itu
 * pernah dibuka — justru itulah gunanya: memberi tahu ada pekerjaan di balik tab
 * yang sedang tidak dilihat.
 *
 * Tanpa batas tanggal, sengaja: pengajuan yang menggantung sejak minggu lalu adalah
 * yang paling perlu terlihat.
 */
class LencanaViewModel : ViewModel() {

    private val _jumlahAksi = MutableStateFlow(0)
    val jumlahAksi: StateFlow<Int> = _jumlahAksi

    init {
        muatUlang()
    }

    fun muatUlang() {
        viewModelScope.launch {
            try {
                _jumlahAksi.value = Postgrest.select(
                    "petty_cash_topups",
                    listOf(
                        "select" to "id",
                        "status" to "eq.${StatusPengajuan.SIAP_DISERAHKAN.nilai}",
                    ),
                ).size()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("LeaderLencanaViewModel", "muatUlang() gagal", e)
                // Lencana yang gagal dimuat dibiarkan pada angka terakhirnya. Memaksanya
                // ke nol akan menyatakan "tidak ada pekerjaan" atas dasar kegagalan
                // jaringan — kebalikan dari yang mungkin benar.
            }
        }
    }
}
