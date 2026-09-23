package com.sukashawarma.superapp.feature.stok.ui.entri

import com.sukashawarma.superapp.core.ui.kaca.navigationBarsPaddingKaca
import com.sukashawarma.superapp.core.ui.ios.KartuIos
import com.sukashawarma.superapp.core.ui.ios.LencanaIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.feature.stok.ui.BannerIos
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.RealtimeTables
import com.sukashawarma.superapp.feature.stok.data.RiwayatWaste
import com.sukashawarma.superapp.feature.stok.data.RiwayatWasteRepository
import com.sukashawarma.superapp.feature.stok.data.model.StatusWaste
import com.sukashawarma.superapp.feature.stok.domain.stokErrorMessage
import com.sukashawarma.superapp.feature.stok.ui.HeaderStok
import com.sukashawarma.superapp.feature.stok.ui.KeadaanGagal
import com.sukashawarma.superapp.feature.stok.ui.KeadaanKosong
import com.sukashawarma.superapp.feature.stok.ui.MemuatPenuh
import com.sukashawarma.superapp.feature.stok.ui.waktuSingkat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class RiwayatWasteUiState(
    val memuat: Boolean = true,
    val riwayat: List<RiwayatWaste> = emptyList(),
    val error: String? = null,
)

/**
 * Riwayat waste pelapor — cermin `app/stok/waste-history/page.tsx`.
 *
 * Layar ini melengkapi Entri Manual: di sana orang melapor bahan terbuang, di sini
 * ia melihat apakah laporannya disetujui atau ditolak. Tanpa layar ini pelapor tidak
 * punya cara apa pun mengetahui hasilnya, padahal alasan penolakan sudah tersimpan.
 */
class RiwayatWasteViewModel : ViewModel() {
    private val _state = MutableStateFlow(RiwayatWasteUiState())
    val state: StateFlow<RiwayatWasteUiState> = _state

    init { muatAwal() }

    fun muatAwal() {
        viewModelScope.launch {
            _state.value = _state.value.copy(memuat = true, error = null)
            try {
                _state.value = _state.value.copy(
                    memuat = false,
                    riwayat = RiwayatWasteRepository.milikSaya(),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    memuat = false,
                    error = e.message ?: stokErrorMessage(e),
                )
            }
        }
    }
}

@Composable
fun RiwayatWasteScreen(onBack: () -> Unit, viewModel: RiwayatWasteViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()

    // Baris berubah sendiri saat approver memutuskan — web memakai toast untuk itu,
    // di sini kartunya yang langsung berganti status.
    RealtimeRefresh(RealtimeTables.WASTE_REPORTS) { viewModel.muatAwal() }

    Column(Modifier.fillMaxSize().background(WarnaIos.Latar)) {
        HeaderStok(
            judul = "Riwayat Waste Saya",
            subjudul = "Status laporan bahan terbuang",
            onKembali = onBack,
        )
        Box(Modifier.fillMaxSize().navigationBarsPaddingKaca()) {
            when {
                state.memuat && state.riwayat.isEmpty() -> MemuatPenuh()
                state.error != null && state.riwayat.isEmpty() ->
                    KeadaanGagal(state.error!!, viewModel::muatAwal)
                state.riwayat.isEmpty() ->
                    KeadaanKosong("Belum ada laporan waste dari akun ini.")
                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = UkuranIos.TepiLayar, end = UkuranIos.TepiLayar, top = 12.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.riwayat, key = { it.id }) { KartuWaste(it) }
                }
            }
        }
    }
}

@Composable
private fun KartuWaste(w: RiwayatWaste) {
    val nada = when (w.status) {
        StatusWaste.APPROVED -> NadaIos.SUKSES
        StatusWaste.PENDING -> NadaIos.PERINGATAN
        StatusWaste.REJECTED -> NadaIos.BAHAYA
    }
    KartuIos(padding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    w.namaBahan,
                    style = TipeIos.Utama,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(waktuSingkat(w.createdAt), style = TipeIos.Catatan)
            }
            Spacer(Modifier.width(8.dp))
            LencanaIos(w.status.label, nada)
        }
        Spacer(Modifier.height(8.dp))
        Text("${w.qtyLabel} — ${w.alasan}", style = TipeIos.SubJudul)
        if (w.status == StatusWaste.REJECTED && w.alasanPenolakan != null) {
            Spacer(Modifier.height(8.dp))
            BannerIos("Alasan ditolak: ${w.alasanPenolakan}", NadaIos.BAHAYA)
        }
    }
}
