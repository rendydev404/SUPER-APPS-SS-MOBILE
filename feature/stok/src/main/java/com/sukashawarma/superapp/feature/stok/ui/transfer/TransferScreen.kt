package com.sukashawarma.superapp.feature.stok.ui.transfer

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.sukashawarma.superapp.feature.stok.ui.KeadaanGagal
import com.sukashawarma.superapp.feature.stok.ui.KeadaanKosong
import com.sukashawarma.superapp.feature.stok.ui.MemuatPenuh
import com.sukashawarma.superapp.presentation.theme.SukaOnSurface
import com.sukashawarma.superapp.presentation.theme.SukaOnSurfaceVariant
import com.sukashawarma.superapp.presentation.theme.SukaSurface
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

    Column(Modifier.fillMaxSize().background(SukaSurface)) {
        Box(
            Modifier.fillMaxWidth().background(
                Brush.verticalGradient(listOf(Color(0xFFEA580C), Color(0xFFF97316)))
            )
        ) {
            Row(
                Modifier.statusBarsPadding().padding(start = 8.dp, end = 16.dp, top = 6.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onKeluar) {
                    Icon(Icons.Default.ArrowBack, "Kembali", tint = Color.White)
                }
                Column(Modifier.weight(1f)) {
                    Text("Saran Transfer", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Sekadar saran, tidak memindahkan stok", color = Color(0xFFFFEDD5), fontSize = 11.sp)
                }
            }
        }

        when {
            state.memuat -> MemuatPenuh()
            state.error != null -> KeadaanGagal(state.error!!, viewModel::cobaLagi)
            state.saran.isEmpty() -> KeadaanKosong(
                "Tidak ada outlet yang kelebihan stok untuk menutup kekurangan outlet lain saat ini."
            )
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
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
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                s.bahanNama,
                color = SukaOnSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("DARI", color = Color(0xFF94A3B8), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(
                        s.dariOutletNama,
                        color = SukaOnSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    Icons.Default.ArrowForward, null,
                    tint = Color(0xFFEA580C),
                    modifier = Modifier.size(18.dp).padding(horizontal = 2.dp),
                )
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text("KE", color = Color(0xFF94A3B8), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(
                        s.keOutletNama,
                        color = SukaOnSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                s.qtyTampil,
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFF7ED), RoundedCornerShape(11.dp))
                    .padding(vertical = 9.dp),
                color = Color(0xFFC2410C),
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}
