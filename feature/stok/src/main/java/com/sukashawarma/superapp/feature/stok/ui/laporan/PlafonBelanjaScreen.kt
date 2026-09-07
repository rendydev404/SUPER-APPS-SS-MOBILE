package com.sukashawarma.superapp.feature.stok.ui.laporan

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.feature.stok.data.BudgetOutlet
import com.sukashawarma.superapp.feature.stok.data.BudgetOutletRepository
import com.sukashawarma.superapp.feature.stok.domain.Budget
import com.sukashawarma.superapp.feature.stok.domain.BudgetVarian
import com.sukashawarma.superapp.feature.stok.domain.formatRupiah
import com.sukashawarma.superapp.feature.stok.domain.stokErrorMessage
import com.sukashawarma.superapp.feature.stok.ui.HeaderStok
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private val ORANGE = Color(0xFFEA580C)
private val SLATE400 = Color(0xFF94A3B8)
private val SLATE500 = Color(0xFF64748B)
private val SLATE900 = Color(0xFF0F172A)
private val GARIS = Color(0xFFE2E8F0)
private val HIJAU = Color(0xFF15803D)
private val MERAH = Color(0xFFB91C1C)
private val AMBER = Color(0xFFB45309)

data class PlafonUiState(
    val outlets: List<BudgetOutlet> = emptyList(),
    val memuat: Boolean = true,
    val error: String? = null,
) {
    val totalPlafon: Double get() = outlets.sumOf { it.nominal }
    val totalTerpakai: Double get() = outlets.sumOf { it.terpakai }
    val jumlahLewat: Int get() = outlets.count { it.hasConfig && it.terpakai > it.nominal }
    val belumDikonfigurasi: Int get() = outlets.count { !it.hasConfig }
}

class PlafonBelanjaViewModel : ViewModel() {
    private val _state = MutableStateFlow(PlafonUiState())
    val state: StateFlow<PlafonUiState> = _state

    init { muatUlang() }

    fun muatUlang() {
        viewModelScope.launch {
            _state.value = _state.value.copy(memuat = true, error = null)
            try {
                _state.value = _state.value.copy(memuat = false, outlets = BudgetOutletRepository.daftar())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("PlafonBelanjaVM", "muatUlang() gagal", e)
                _state.value = _state.value.copy(memuat = false, error = stokErrorMessage(e))
            }
        }
    }
}

/**
 * Plafon & Belanja Outlet — cermin `BudgetOutletTabContent.tsx` web.
 *
 * Pengajuan dan persetujuan top-up sudah ada di layar Permintaan; halaman ini
 * menjawab pertanyaan yang berbeda, yaitu outlet mana yang belanjanya sudah
 * mendekati atau melewati plafon.
 */
@Composable
fun PlafonBelanjaScreen(
    onBack: () -> Unit,
    viewModel: PlafonBelanjaViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()

    Column(Modifier.fillMaxSize().background(Color(0xFFF8FAFC))) {
        HeaderStok(
            judul = "Plafon & Belanja Outlet",
            subjudul = when {
                state.memuat -> "Memuat…"
                state.jumlahLewat > 0 -> "${state.outlets.size} outlet · ${state.jumlahLewat} melewati plafon"
                else -> "${state.outlets.size} outlet"
            },
            onKembali = onBack,
            aksi = {
                IconButton(onClick = viewModel::muatUlang) {
                    Icon(Icons.Default.Refresh, "Muat ulang", tint = Color.White)
                }
            },
        )

        when {
            state.memuat -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ORANGE)
            }
            state.error != null -> PesanKosongLaporan(state.error!!)
            state.outlets.isEmpty() -> PesanKosongLaporan("Tidak ada outlet dalam cakupan Anda.")
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                item { RingkasPlafon(state) }
                items(state.outlets, key = { it.outletId }) { outlet -> KartuPlafon(outlet) }
            }
        }
    }
}

@Composable
private fun RingkasPlafon(state: PlafonUiState) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = SLATE900) {
        Column(Modifier.padding(18.dp)) {
            Text(
                "TOTAL BELANJA PERIODE BERJALAN",
                color = Color(0xFF94A3B8),
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                formatRupiah(state.totalTerpakai),
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                "dari plafon ${formatRupiah(state.totalPlafon)}",
                color = Color(0xFFCBD5E1),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
            )
            if (state.belumDikonfigurasi > 0) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "${state.belumDikonfigurasi} outlet belum punya plafon, jadi belanjanya tidak ikut dibatasi.",
                    color = Color(0xFFFCD34D),
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun KartuPlafon(outlet: BudgetOutlet) {
    val varian = Budget.varian(outlet.hasConfig, outlet.nominal, outlet.terpakai)
    val warna = when (varian) {
        BudgetVarian.MERAH -> MERAH
        BudgetVarian.ORANYE -> AMBER
        BudgetVarian.HIJAU -> HIJAU
        BudgetVarian.TERSEMBUNYI -> SLATE400
    }
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(15.dp),
        color = Color.White,
        border = BorderStroke(1.dp, GARIS),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        outlet.outletName,
                        color = SLATE900,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        if (outlet.hasConfig) {
                            "Plafon ${Budget.labelPeriode(outlet.periodType, outlet.customDays).lowercase()}: ${formatRupiah(outlet.nominal)}"
                        } else {
                            "Belum ada plafon"
                        },
                        color = SLATE400,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (outlet.hasConfig) {
                    Text(
                        "${outlet.persen.toInt()}%",
                        color = warna,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }

            if (outlet.hasConfig) {
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFF1F5F9)),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth((outlet.persen / 100.0).coerceIn(0.0, 1.0).toFloat())
                            .height(7.dp)
                            .clip(RoundedCornerShape(50))
                            .background(warna),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row {
                    Text(
                        "Terpakai ${formatRupiah(outlet.terpakai)}",
                        Modifier.weight(1f),
                        color = SLATE500,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        // Sisa negatif berarti sudah lewat plafon; menampilkannya
                        // sebagai "sisa minus" lebih jujur daripada memaksanya ke nol.
                        if (outlet.sisa < 0) "Lewat ${formatRupiah(-outlet.sisa)}"
                        else "Sisa ${formatRupiah(outlet.sisa)}",
                        color = if (outlet.sisa < 0) MERAH else SLATE500,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
