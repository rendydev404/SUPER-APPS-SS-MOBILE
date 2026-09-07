package com.sukashawarma.superapp.feature.stok.ui.laporan

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import com.sukashawarma.superapp.feature.stok.data.LaporanPenjualanRepository
import com.sukashawarma.superapp.feature.stok.data.RentangLaporan
import com.sukashawarma.superapp.feature.stok.domain.BarisKanal
import com.sukashawarma.superapp.feature.stok.domain.BarisMenuLaris
import com.sukashawarma.superapp.feature.stok.domain.BarisOutletJual
import com.sukashawarma.superapp.feature.stok.domain.RingkasPenjualan
import com.sukashawarma.superapp.feature.stok.domain.formatAngkaStok
import com.sukashawarma.superapp.feature.stok.domain.formatRupiah
import com.sukashawarma.superapp.feature.stok.domain.ringkasPenjualan
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

/** Preset rentang — cermin tombol periode di `laporan-penjualan/page.tsx`. */
enum class PeriodeLaporan(val label: String) {
    HARI_INI("Hari Ini"),
    TUJUH_HARI("7 Hari"),
    BULAN_INI("Bulan Ini");

    fun rentang(): RentangLaporan = when (this) {
        HARI_INI -> RentangLaporan.hariIni()
        TUJUH_HARI -> RentangLaporan.tujuhHari()
        BULAN_INI -> RentangLaporan.bulanIni()
    }
}

data class LaporanPenjualanUiState(
    val periode: PeriodeLaporan = PeriodeLaporan.HARI_INI,
    val ringkas: RingkasPenjualan = RingkasPenjualan(),
    val memuat: Boolean = true,
    val error: String? = null,
)

class LaporanPenjualanViewModel : ViewModel() {
    private val _state = MutableStateFlow(LaporanPenjualanUiState())
    val state: StateFlow<LaporanPenjualanUiState> = _state

    init { muatUlang() }

    fun pilihPeriode(periode: PeriodeLaporan) {
        if (_state.value.periode == periode) return
        _state.value = _state.value.copy(periode = periode)
        muatUlang()
    }

    fun muatUlang() {
        viewModelScope.launch {
            _state.value = _state.value.copy(memuat = true, error = null)
            try {
                val data = LaporanPenjualanRepository.muat(_state.value.periode.rentang())
                _state.value = _state.value.copy(
                    memuat = false,
                    ringkas = ringkasPenjualan(data.pesanan, data.namaOutlet),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("LaporanPenjualanVM", "muatUlang() gagal", e)
                _state.value = _state.value.copy(memuat = false, error = stokErrorMessage(e))
            }
        }
    }
}

/**
 * Laporan Penjualan — cermin `app/stok/laporan-penjualan/page.tsx`.
 *
 * Seluruh agregasinya sudah ada di `domain/LaporanPenjualan.kt`; layar ini hanya
 * menyajikannya. Yang perlu dijaga di sini: omzet KOTOR dan BERSIH ditampilkan
 * berdampingan, tidak dipilih salah satu. Selisih keduanya adalah potongan dan
 * subsidi promo, dan menyembunyikan salah satunya membuat orang membandingkan
 * angka yang berbeda dasar tanpa sadar.
 */
@Composable
fun LaporanPenjualanScreen(
    onBack: () -> Unit,
    viewModel: LaporanPenjualanViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val r = state.ringkas

    Column(Modifier.fillMaxSize().background(Color(0xFFF8FAFC))) {
        HeaderStok(
            judul = "Laporan Penjualan",
            subjudul = if (state.memuat) "Memuat…" else "${r.jumlahOrder} order selesai",
            onKembali = onBack,
            aksi = {
                IconButton(onClick = viewModel::muatUlang) {
                    Icon(Icons.Default.Refresh, "Muat ulang", tint = Color.White)
                }
            },
        )

        BarisPeriode(state.periode, viewModel::pilihPeriode)

        when {
            state.memuat -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ORANGE)
            }
            state.error != null -> PesanKosongLaporan(state.error!!)
            r.jumlahOrder == 0 -> PesanKosongLaporan(
                if (r.jumlahBatal > 0) "Tidak ada pesanan selesai pada periode ini (${r.jumlahBatal} dibatalkan)."
                else "Belum ada pesanan pada periode ini."
            )
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { KartuOmzet(r) }
                if (r.perKanal.isNotEmpty()) item { KartuKanal(r.perKanal, r.omzetBersih) }
                if (r.perOutlet.isNotEmpty()) item { KartuOutlet(r.perOutlet) }
                if (r.menuTerlaris.isNotEmpty()) item { JudulSeksi("MENU TERLARIS") }
                items(r.menuTerlaris, key = { it.nama }) { BarisMenu(it) }
            }
        }
    }
}

@Composable
private fun BarisPeriode(aktif: PeriodeLaporan, onPilih: (PeriodeLaporan) -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PeriodeLaporan.entries.forEach { p ->
            val terpilih = p == aktif
            Surface(
                shape = RoundedCornerShape(50),
                color = if (terpilih) Color(0xFFFFEDD5) else Color(0xFFF1F5F9),
                modifier = Modifier.clickable { onPilih(p) },
            ) {
                Text(
                    p.label,
                    Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    color = if (terpilih) Color(0xFFC2410C) else SLATE500,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun KartuOmzet(r: RingkasPenjualan) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = SLATE900) {
        Column(Modifier.padding(18.dp)) {
            Text(
                "OMZET BERSIH",
                color = SLATE400,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                formatRupiah(r.omzetBersih),
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Kotor ${formatRupiah(r.omzetKotor)} · potongan ${formatRupiah(r.potongan)}",
                color = Color(0xFFCBD5E1),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
            )

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AngkaGelap("Order", r.jumlahOrder.toString(), Modifier.weight(1f))
                AngkaGelap("Rata-rata", formatRupiah(r.rataRataOrder), Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AngkaGelap(
                    "Jam tersibuk",
                    r.jamTersibuk?.let { "%02d:00".format(it) } ?: "—",
                    Modifier.weight(1f),
                )
                AngkaGelap("Dibatalkan", r.jumlahBatal.toString(), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AngkaGelap(label: String, nilai: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label.uppercase(), color = SLATE400, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
        Spacer(Modifier.height(3.dp))
        Text(nilai, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun KartuKanal(baris: List<BarisKanal>, omzetTotal: Double) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = Color.White) {
        Column(Modifier.padding(16.dp)) {
            Text("PER KANAL", color = SLATE400, fontSize = 9.5.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp)
            Spacer(Modifier.height(10.dp))
            baris.forEachIndexed { i, b ->
                if (i > 0) HorizontalDivider(Modifier.padding(vertical = 9.dp), color = GARIS)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(b.kanal.label, color = SLATE900, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "${b.jumlahOrder} order · ${porsiPersen(b.omzet, omzetTotal)}",
                            color = SLATE500,
                            fontSize = 11.sp,
                        )
                    }
                    Text(formatRupiah(b.omzet), color = ORANGE, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun KartuOutlet(baris: List<BarisOutletJual>) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = Color.White) {
        Column(Modifier.padding(16.dp)) {
            Text("PER OUTLET", color = SLATE400, fontSize = 9.5.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp)
            Spacer(Modifier.height(10.dp))
            baris.forEachIndexed { i, b ->
                if (i > 0) HorizontalDivider(Modifier.padding(vertical = 9.dp), color = GARIS)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${i + 1}",
                        Modifier.width(22.dp),
                        color = SLATE400,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Column(Modifier.weight(1f)) {
                        Text(b.nama, color = SLATE900, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "${b.jumlahOrder} order · ${formatAngkaStok(b.porsi)} porsi",
                            color = SLATE500,
                            fontSize = 11.sp,
                        )
                    }
                    Text(formatRupiah(b.omzet), color = SLATE900, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun JudulSeksi(teks: String) {
    Text(teks, color = SLATE400, fontSize = 9.5.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp)
}

@Composable
private fun BarisMenu(m: BarisMenuLaris) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = Color.White) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(m.nama, color = SLATE900, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text("${formatAngkaStok(m.qty)} terjual", color = SLATE500, fontSize = 11.sp)
            }
            Text(formatRupiah(m.omzet), color = ORANGE, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

/** Porsi kontribusi terhadap total; total nol tidak pernah dijadikan pembagi. */
private fun porsiPersen(nilai: Double, total: Double): String =
    if (total <= 0.0) "—" else "%.0f%%".format(nilai / total * 100)
