package com.sukashawarma.superapp.feature.stok.ui.entri

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.sukashawarma.superapp.presentation.theme.SukaOnSurface
import com.sukashawarma.superapp.presentation.theme.SukaOnSurfaceVariant
import com.sukashawarma.superapp.presentation.theme.SukaSurface
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

    Column(Modifier.fillMaxSize().background(SukaSurface)) {
        HeaderStok(
            judul = "Riwayat Waste Saya",
            subjudul = "Status laporan bahan terbuang",
            onKembali = onBack,
        )
        Box(Modifier.fillMaxSize().navigationBarsPadding()) {
            when {
                state.memuat && state.riwayat.isEmpty() -> MemuatPenuh()
                state.error != null && state.riwayat.isEmpty() ->
                    KeadaanGagal(state.error!!, viewModel::muatAwal)
                state.riwayat.isEmpty() ->
                    KeadaanKosong("Belum ada laporan waste dari akun ini.")
                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    items(state.riwayat, key = { it.id }) { KartuWaste(it) }
                }
            }
        }
    }
}

@Composable
private fun KartuWaste(w: RiwayatWaste) {
    val warna = when (w.status) {
        StatusWaste.APPROVED -> Color(0xFF168451)
        StatusWaste.PENDING -> Color(0xFFC27A12)
        StatusWaste.REJECTED -> Color(0xFFDC2626)
    }
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        w.namaBahan,
                        color = SukaOnSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        waktuSingkat(w.createdAt),
                        color = SukaOnSurfaceVariant,
                        fontSize = 10.sp,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = warna.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, warna.copy(alpha = 0.28f)),
                ) {
                    Text(
                        w.status.label,
                        Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        color = warna, fontSize = 10.sp, fontWeight = FontWeight.Black,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "${w.qtyLabel} — ${w.alasan}",
                color = SukaOnSurfaceVariant,
                fontSize = 11.sp,
            )
            if (w.status == StatusWaste.REJECTED && w.alasanPenolakan != null) {
                Spacer(Modifier.height(6.dp))
                Surface(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFEF2F2),
                    border = BorderStroke(1.dp, Color(0xFFFECACA)),
                ) {
                    Text(
                        "Alasan ditolak: ${w.alasanPenolakan}",
                        Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        color = Color(0xFFB91C1C),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}
