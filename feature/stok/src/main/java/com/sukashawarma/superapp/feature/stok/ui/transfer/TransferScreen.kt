package com.sukashawarma.superapp.feature.stok.ui.transfer

import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.core.ui.ios.KartuIos
import com.sukashawarma.superapp.core.ui.ios.LencanaIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.feature.stok.data.StokRepository
import com.sukashawarma.superapp.feature.stok.domain.SaranTransfer
import com.sukashawarma.superapp.feature.stok.domain.TransferSuggester
import com.sukashawarma.superapp.feature.stok.domain.stokErrorMessage
import com.sukashawarma.superapp.feature.stok.ui.HeaderStok
import com.sukashawarma.superapp.feature.stok.ui.KeadaanGagal
import com.sukashawarma.superapp.feature.stok.ui.KeadaanKosong
import com.sukashawarma.superapp.feature.stok.ui.MemuatPenuh
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TransferUiState(
    val memuat: Boolean = true,
    val error: String? = null,
    val saran: List<SaranTransfer> = emptyList(),
)

class TransferViewModel : ViewModel() {

    private val _state = MutableStateFlow(TransferUiState())
    val state: StateFlow<TransferUiState> = _state

    fun muat() {
        viewModelScope.launch {
            _state.value = TransferUiState(memuat = true)
            try {
                val kritis = StokRepository.bahanTidakAmanLintasOutlet()
                val bahanUnik = kritis.map { it.bahanBakuId }.distinct().take(25)
                val semua = bahanUnik.map { StokRepository.monitoringLintasOutlet(it) }
                val saran = withContext(Dispatchers.Default) {
                    semua.flatMap { TransferSuggester.untukBahan(it) }
                }
                _state.value = TransferUiState(memuat = false, saran = saran)
            } catch (e: Exception) {
                _state.value = TransferUiState(memuat = false, error = stokErrorMessage(e))
            }
        }
    }

    fun cobaLagi() {
        StokRepository.invalidate()
        muat()
    }
}

@Composable
fun TransferScreen(onKeluar: () -> Unit, viewModel: TransferViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.muat() }

    Column(Modifier.fillMaxSize().background(WarnaIos.Latar)) {
        HeaderStok(
            judul = "Saran Transfer",
            subjudul = "Sekadar saran, tidak memindahkan stok",
            onKembali = onKeluar,
        )

        when {
            state.memuat -> MemuatPenuh()
            state.error != null -> KeadaanGagal(state.error!!, viewModel::cobaLagi)
            state.saran.isEmpty() -> KeadaanKosong(
                "Tidak ada outlet yang kelebihan stok untuk menutup kekurangan outlet lain saat ini."
            )
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = UkuranIos.TepiLayar, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(UkuranIos.JarakKartu),
            ) {
                items(
                    state.saran,
                    key = { "${it.bahanBakuId}|${it.dariOutletId}|${it.keOutletId}" },
                ) { KartuSaran(it) }
            }
        }
    }
}

@Composable
private fun KartuSaran(s: SaranTransfer) {
    KartuIos {
        Text(s.bahanNama, style = TipeIos.Utama, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth().clip(UkuranIos.SudutBlok).background(WarnaIos.Latar).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Dari", style = TipeIos.Kecil.copy(fontWeight = FontWeight.Medium))
                Text(
                    s.dariOutletNama,
                    style = TipeIos.Keterangan.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                Modifier.padding(horizontal = 8.dp).size(28.dp).clip(CircleShape).background(WarnaIos.Aksen.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(IkonIos.ArrowForward, null, tint = WarnaIos.Aksen, modifier = Modifier.size(15.dp))
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text("Ke", style = TipeIos.Kecil.copy(fontWeight = FontWeight.Medium))
                Text(
                    s.keOutletNama,
                    style = TipeIos.Keterangan.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Jumlah disarankan", Modifier.weight(1f), style = TipeIos.SubJudul)
            LencanaIos(s.qtyTampil, NadaIos.AKSEN, titik = false, ikon = IkonIos.SwapHoriz)
        }
    }
}
