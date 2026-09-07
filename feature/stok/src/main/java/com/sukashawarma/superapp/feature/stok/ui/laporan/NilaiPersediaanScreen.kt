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
import com.sukashawarma.superapp.feature.stok.data.NilaiPersediaanRepository
import com.sukashawarma.superapp.feature.stok.domain.RingkasNilaiOutlet
import com.sukashawarma.superapp.feature.stok.domain.StatusNilai
import com.sukashawarma.superapp.feature.stok.domain.TotalNilai
import com.sukashawarma.superapp.feature.stok.domain.formatRupiah
import com.sukashawarma.superapp.feature.stok.domain.ringkasNilaiPerOutlet
import com.sukashawarma.superapp.feature.stok.domain.stokErrorMessage
import com.sukashawarma.superapp.feature.stok.domain.totalNilai
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
private val AMBER = Color(0xFF92400E)
private val AMBER_LATAR = Color(0xFFFEF3C7)

data class NilaiPersediaanUiState(
    val outlets: List<RingkasNilaiOutlet> = emptyList(),
    val total: TotalNilai = TotalNilai(),
    val dibuka: String? = null,
    val memuat: Boolean = true,
    val error: String? = null,
)

class NilaiPersediaanViewModel : ViewModel() {
    private val _state = MutableStateFlow(NilaiPersediaanUiState())
    val state: StateFlow<NilaiPersediaanUiState> = _state

    init { muatUlang() }

    fun muatUlang() {
        viewModelScope.launch {
            _state.value = _state.value.copy(memuat = true, error = null)
            try {
                val outlets = ringkasNilaiPerOutlet(NilaiPersediaanRepository.muat())
                _state.value = _state.value.copy(
                    memuat = false,
                    outlets = outlets,
                    total = totalNilai(outlets),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("NilaiPersediaanVM", "muatUlang() gagal", e)
                _state.value = _state.value.copy(memuat = false, error = stokErrorMessage(e))
            }
        }
    }

    fun bukaTutup(outletId: String) {
        _state.value = _state.value.copy(
            dibuka = if (_state.value.dibuka == outletId) null else outletId,
        )
    }
}

/**
 * Nilai Persediaan — cermin `NilaiPersediaanBoard.tsx` web.
 *
 * Yang membedakan halaman ini dari sekadar "total stok kali harga" adalah tiga
 * tingkat keyakinan yang dipisah: nilai pasti, nilai yang skalanya belum
 * dipastikan opname, dan bahan yang harga atau isi kemasannya belum diisi.
 * Menggabungkan ketiganya menjadi satu angka akan menyembunyikan seberapa banyak
 * dari total itu sebenarnya tebakan.
 */
@Composable
fun NilaiPersediaanScreen(
    onBack: () -> Unit,
    viewModel: NilaiPersediaanViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    RealtimeRefresh(RealtimeTables.STOK_BALANCE, RealtimeTables.BAHAN_BAKU_HARGA) { viewModel.muatUlang() }

    Column(Modifier.fillMaxSize().background(Color(0xFFF8FAFC))) {
        HeaderStok(
            judul = "Nilai Persediaan",
            subjudul = if (state.memuat) "Memuat…" else "${state.outlets.size} outlet",
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
            state.outlets.isEmpty() -> PesanKosongLaporan("Belum ada data nilai persediaan.")
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { KartuTotalNilai(state.total) }
                items(state.outlets, key = { it.outletId }) { outlet ->
                    KartuOutletNilai(
                        outlet = outlet,
                        terbuka = state.dibuka == outlet.outletId,
                        onKlik = { viewModel.bukaTutup(outlet.outletId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun KartuTotalNilai(total: TotalNilai) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = SLATE900) {
        Column(Modifier.padding(18.dp)) {
            Text(
                "NILAI PERSEDIAAN SELURUH OUTLET",
                color = Color(0xFF94A3B8),
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                formatRupiah(total.total),
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Rentang ${formatRupiah(total.batasBawah)} – ${formatRupiah(total.batasAtas)}",
                color = Color(0xFFCBD5E1),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
            )

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AngkaKecil("Pasti", formatRupiah(total.nilaiPasti), Modifier.weight(1f))
                AngkaKecil("Belum pasti", formatRupiah(total.nilaiBelumPasti), Modifier.weight(1f))
            }
            if (total.jumlahBelumPasti > 0 || total.jumlahDataKurang > 0) {
                Spacer(Modifier.height(12.dp))
                Text(
                    buildString {
                        if (total.jumlahBelumPasti > 0) {
                            append("${total.jumlahBelumPasti} bahan skalanya belum dipastikan opname")
                        }
                        if (total.jumlahDataKurang > 0) {
                            if (isNotEmpty()) append(" · ")
                            append("${total.jumlahDataKurang} bahan belum punya harga atau isi kemasan")
                        }
                    },
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
private fun AngkaKecil(label: String, nilai: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label.uppercase(), color = Color(0xFF94A3B8), fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.6.sp)
        Spacer(Modifier.height(3.dp))
        Text(nilai, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun KartuOutletNilai(outlet: RingkasNilaiOutlet, terbuka: Boolean, onKlik: () -> Unit) {
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
                        outlet.outlet,
                        color = SLATE900,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${outlet.jumlahBahan} bahan",
                        color = SLATE400,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    formatRupiah(outlet.total),
                    color = SLATE900,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                )
            }

            if (outlet.jumlahBelumPasti > 0 || outlet.jumlahDataKurang > 0) {
                Spacer(Modifier.height(8.dp))
                Surface(shape = RoundedCornerShape(8.dp), color = AMBER_LATAR) {
                    Text(
                        listOfNotNull(
                            outlet.jumlahBelumPasti.takeIf { it > 0 }?.let { "$it skala belum pasti" },
                            outlet.jumlahDataKurang.takeIf { it > 0 }?.let { "$it data belum lengkap" },
                        ).joinToString(" · "),
                        Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        color = AMBER,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            if (terbuka) {
                Spacer(Modifier.height(12.dp))
                // Sepuluh bahan termahal saja: sisanya jarang mengubah keputusan,
                // dan menampilkan ratusan baris di dalam kartu justru mengubur
                // yang penting.
                outlet.items.take(10).forEach { baris ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                baris.bahan,
                                color = SLATE900,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (baris.status != StatusNilai.PASTI) {
                                Text(
                                    baris.status.label,
                                    color = AMBER,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                        Text(
                            formatRupiah(baris.nilai),
                            color = SLATE500,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                if (outlet.items.size > 10) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${outlet.items.size - 10} bahan lain tidak ditampilkan.",
                        color = SLATE400,
                        fontSize = 10.5.sp,
                    )
                }
            }
        }
    }
}

@Composable
internal fun PesanKosongLaporan(teks: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            teks,
            color = SLATE500,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 19.sp,
        )
    }
}
