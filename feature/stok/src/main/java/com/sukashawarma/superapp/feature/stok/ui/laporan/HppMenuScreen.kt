package com.sukashawarma.superapp.feature.stok.ui.laporan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.sukashawarma.superapp.feature.stok.data.HppMenuRepository
import com.sukashawarma.superapp.feature.stok.domain.MenuHpp
import com.sukashawarma.superapp.feature.stok.domain.StatusFoodCost
import com.sukashawarma.superapp.feature.stok.domain.formatAngkaStok
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

data class HppMenuUiState(
    val menu: List<MenuHpp> = emptyList(),
    val cari: String = "",
    val dibuka: String? = null,
    val memuat: Boolean = true,
    val error: String? = null,
) {
    val tampil: List<MenuHpp>
        get() {
            val kueri = cari.trim().lowercase()
            if (kueri.isEmpty()) return menu
            return menu.filter {
                it.menuNama.lowercase().contains(kueri) || it.kategori.lowercase().contains(kueri)
            }
        }

    val jumlahKritis: Int get() = menu.count { it.status == StatusFoodCost.KRITIS }
}

class HppMenuViewModel : ViewModel() {
    private val _state = MutableStateFlow(HppMenuUiState())
    val state: StateFlow<HppMenuUiState> = _state

    init { muatUlang() }

    fun muatUlang() {
        viewModelScope.launch {
            _state.value = _state.value.copy(memuat = true, error = null)
            try {
                _state.value = _state.value.copy(memuat = false, menu = HppMenuRepository.muat())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("HppMenuVM", "muatUlang() gagal", e)
                _state.value = _state.value.copy(memuat = false, error = stokErrorMessage(e))
            }
        }
    }

    fun ubahCari(teks: String) { _state.value = _state.value.copy(cari = teks) }
    fun bukaTutup(id: String) {
        _state.value = _state.value.copy(dibuka = if (_state.value.dibuka == id) null else id)
    }
}

/**
 * Kalkulator HPP Menu — cermin `HPPMenuBoard.tsx` web.
 *
 * Angka yang dicari orang di sini bukan total HPP, melainkan food cost: berapa
 * persen harga jual yang habis untuk bahan. Karena itu persentasenya yang
 * ditonjolkan, bukan rupiahnya.
 */
@Composable
fun HppMenuScreen(
    onBack: () -> Unit,
    viewModel: HppMenuViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()

    Column(Modifier.fillMaxSize().background(Color(0xFFF8FAFC))) {
        HeaderStok(
            judul = "HPP Setiap Menu",
            subjudul = when {
                state.memuat -> "Memuat…"
                state.jumlahKritis > 0 -> "${state.menu.size} menu · ${state.jumlahKritis} food cost kritis"
                else -> "${state.menu.size} menu"
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
            else -> Column(Modifier.fillMaxSize()) {
                OutlinedTextField(
                    value = state.cari,
                    onValueChange = viewModel::ubahCari,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    placeholder = { Text("Cari menu atau kategori…", fontSize = 12.5.sp, color = SLATE400) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = ORANGE,
                        unfocusedBorderColor = GARIS,
                    ),
                )
                if (state.tampil.isEmpty()) {
                    PesanKosongLaporan("Tidak ada menu yang cocok.")
                } else {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(11.dp),
                    ) {
                        items(state.tampil, key = { it.resepId }) { menu ->
                            KartuHpp(
                                menu = menu,
                                terbuka = state.dibuka == menu.resepId,
                                onKlik = { viewModel.bukaTutup(menu.resepId) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KartuHpp(menu: MenuHpp, terbuka: Boolean, onKlik: () -> Unit) {
    val warna = when (menu.status) {
        StatusFoodCost.OPTIMAL -> Color(0xFF15803D)
        StatusFoodCost.WASPADA -> Color(0xFFB45309)
        StatusFoodCost.KRITIS -> Color(0xFFB91C1C)
    }
    Surface(
        Modifier.fillMaxWidth().clickable(onClick = onKlik),
        shape = RoundedCornerShape(15.dp),
        color = Color.White,
        border = BorderStroke(1.dp, GARIS),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        menu.menuNama,
                        color = SLATE900,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(menu.kategori, color = SLATE400, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        if (menu.hargaJual > 0) "${menu.foodCostPersen.toInt()}%" else "—",
                        color = warna,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Text("FOOD COST", color = SLATE400, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 0.6.sp)
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                KolomAngka("HPP", formatRupiah(menu.totalHpp), Modifier.weight(1f))
                KolomAngka(
                    "Harga jual",
                    if (menu.hargaJual > 0) formatRupiah(menu.hargaJual) else "Belum diisi",
                    Modifier.weight(1f),
                )
                KolomAngka("Margin", formatRupiah(menu.marginRupiah), Modifier.weight(1f))
            }

            if (menu.hargaJual <= 0.0) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Menu ini belum punya harga jual, jadi food cost-nya tidak bisa dihitung.",
                    color = Color(0xFFB45309),
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            if (terbuka) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "SUSUNAN BIAYA",
                    color = SLATE400,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.7.sp,
                )
                Spacer(Modifier.height(6.dp))
                menu.bahan.forEach { bahan ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                bahan.nama,
                                color = SLATE900,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                "${formatAngkaStok(bahan.qtyPerPorsi)} ${bahan.satuanResep} · ${bahan.kontribusiPersen}%",
                                color = SLATE400,
                                fontSize = 10.5.sp,
                            )
                        }
                        Text(
                            formatRupiah(bahan.subtotal),
                            color = SLATE500,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                if (menu.bahan.isEmpty()) {
                    Text(
                        "Resep ini belum punya bahan, jadi HPP-nya nol.",
                        color = SLATE400,
                        fontSize = 11.5.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun KolomAngka(label: String, nilai: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label.uppercase(), color = SLATE400, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 0.6.sp)
        Spacer(Modifier.height(2.dp))
        Text(nilai, color = SLATE900, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
