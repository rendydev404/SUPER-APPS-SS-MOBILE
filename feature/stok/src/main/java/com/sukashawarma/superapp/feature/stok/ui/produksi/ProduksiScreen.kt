package com.sukashawarma.superapp.feature.stok.ui.produksi

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.feature.stok.data.StokRepository
import com.sukashawarma.superapp.feature.stok.domain.EstimasiPorsi
import com.sukashawarma.superapp.feature.stok.domain.ProduksiEstimator
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

data class ProduksiUiState(
    val memuat: Boolean = true,
    val error: String? = null,
    val estimasi: List<EstimasiPorsi> = emptyList(),
)

class ProduksiViewModel : ViewModel() {

    private val _state = MutableStateFlow(ProduksiUiState())
    val state: StateFlow<ProduksiUiState> = _state
    private var outletId: String? = null

    fun muat(outletId: String) {
        this.outletId = outletId
        viewModelScope.launch {
            _state.value = ProduksiUiState(memuat = true)
            try {
                val resep = StokRepository.resep(outletId)
                // Seluruh bahan outlet, bukan sehalaman: resep bisa memakai bahan mana pun,
                // dan bahan yang tak terambil akan salah dilaporkan sebagai nol porsi.
                val baris = StokRepository.monitoringOutlet(outletId)
                val hasil = withContext(Dispatchers.Default) {
                    ProduksiEstimator.estimasiSemua(
                        resep = resep,
                        saldoNormPerBahan = baris.associate { it.bahanBakuId to it.saldoNorm },
                        namaPerBahan = baris.associate { it.bahanBakuId to it.itemName },
                    )
                }
                _state.value = ProduksiUiState(memuat = false, estimasi = hasil)
            } catch (e: Exception) {
                _state.value = ProduksiUiState(memuat = false, error = stokErrorMessage(e))
            }
        }
    }

    fun cobaLagi() {
        val o = outletId ?: return
        StokRepository.invalidate()
        muat(o)
    }
}

@Composable
fun ProduksiScreen(
    outletId: String,
    onKeluar: () -> Unit,
    viewModel: ProduksiViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(outletId) { viewModel.muat(outletId) }

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
                    Text("Estimasi Produksi", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Porsi yang masih bisa dibuat", color = Color(0xFFFFEDD5), fontSize = 11.sp)
                }
            }
        }

        when {
            state.memuat -> MemuatPenuh()
            state.error != null -> KeadaanGagal(state.error!!, viewModel::cobaLagi)
            state.estimasi.isEmpty() -> KeadaanKosong("Belum ada resep aktif untuk outlet ini.")
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.estimasi, key = { it.resepId }) { KartuEstimasi(it) }
            }
        }
    }
}

@Composable
private fun KartuEstimasi(e: EstimasiPorsi) {
    val warna = when {
        e.porsi <= 0 -> Color(0xFFDC2626)
        e.porsi < 10 -> Color(0xFFC27A12)
        else -> Color(0xFF168451)
    }
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    e.resepNama,
                    color = SukaOnSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    e.bottleneckNama?.let { "Penghambat: $it" } ?: "Penghambat belum diketahui",
                    color = SukaOnSurfaceVariant,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!e.lengkap) {
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "Sebagian bahan belum diketahui saldonya — angka ini belum utuh.",
                        color = Color(0xFFC2410C),
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                    )
                }
            }
            Column(
                Modifier.widthIn(min = 74.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("${e.porsi}", color = warna, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                Text("porsi", color = Color(0xFF94A3B8), fontSize = 10.sp, textAlign = TextAlign.Center)
            }
        }
    }
}
