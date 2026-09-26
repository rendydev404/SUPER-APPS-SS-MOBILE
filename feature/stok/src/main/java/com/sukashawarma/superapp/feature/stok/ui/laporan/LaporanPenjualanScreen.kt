package com.sukashawarma.superapp.feature.stok.ui.laporan

import com.sukashawarma.superapp.core.ui.ios.GrupIos
import com.sukashawarma.superapp.core.ui.ios.JudulSeksiIos
import com.sukashawarma.superapp.core.ui.ios.KartuIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.PemisahIos
import com.sukashawarma.superapp.core.ui.ios.SegmenIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.TombolBundarIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WadahSegmenIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.feature.stok.ui.MemuatPenuh
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.core.ui.kaca.denganRuangNav
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.RealtimeTables


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

    // Omzet & porsi terjual bergerak tiap kasir menutup pesanan. Layar Laporan
    // milik Manager sudah berlangganan keduanya; layar ini tertinggal.
    RealtimeRefresh(RealtimeTables.ORDERS, RealtimeTables.ORDER_ITEMS, jedaMinimumMs = 30_000L) { viewModel.muatUlang() }
    val r = state.ringkas

    Column(Modifier.fillMaxSize().background(WarnaIos.Latar)) {
        HeaderStok(
            judul = "Laporan Penjualan",
            subjudul = if (state.memuat) "Memuat…" else "${r.jumlahOrder} order selesai",
            onKembali = onBack,
            aksi = {
                TombolBundarIos(IkonIos.Refresh, "Muat ulang", viewModel::muatUlang)
            },
        )

        BarisPeriode(state.periode, viewModel::pilihPeriode)

        when {
            state.memuat -> MemuatPenuh()
            state.error != null -> PesanKosongLaporan(state.error!!)
            r.jumlahOrder == 0 -> PesanKosongLaporan(
                if (r.jumlahBatal > 0) "Tidak ada pesanan selesai pada periode ini (${r.jumlahBatal} dibatalkan)."
                else "Belum ada pesanan pada periode ini."
            )
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = UkuranIos.TepiLayar, vertical = 12.dp).denganRuangNav(),
                verticalArrangement = Arrangement.spacedBy(UkuranIos.JarakKartu),
            ) {
                item { KartuOmzet(r) }
                if (r.perKanal.isNotEmpty()) item { KartuKanal(r.perKanal, r.omzetBersih) }
                if (r.perOutlet.isNotEmpty()) item { KartuOutlet(r.perOutlet) }
                if (r.menuTerlaris.isNotEmpty()) item { JudulSeksi("Menu terlaris") }
                items(r.menuTerlaris, key = { it.nama }) { BarisMenu(it) }
            }
        }
    }
}

/** Pilihan periode sebagai kontrol segmen iOS. */
@Composable
private fun BarisPeriode(aktif: PeriodeLaporan, onPilih: (PeriodeLaporan) -> Unit) {
    WadahSegmenIos(Modifier.padding(horizontal = UkuranIos.TepiLayar, vertical = 8.dp)) {
        PeriodeLaporan.entries.forEach { periode ->
            SegmenIos(periode.label, periode == aktif, { onPilih(periode) }, Modifier.weight(1f), jarakSisi = 6.dp)
        }
    }
}

@Composable
private fun KartuOmzet(r: RingkasPenjualan) {
    KartuIos {
        Text("Omzet bersih", style = TipeIos.SubJudul)
        Spacer(Modifier.height(4.dp))
        Text(formatRupiah(r.omzetBersih), style = TipeIos.JudulBesar.copy(fontSize = 30.sp))
        Spacer(Modifier.height(2.dp))
        Text(
            "Kotor ${formatRupiah(r.omzetKotor)} · potongan ${formatRupiah(r.potongan)}",
            style = TipeIos.Catatan,
        )

        Spacer(Modifier.height(14.dp))
        Column(
            Modifier.fillMaxWidth().clip(UkuranIos.SudutBlok).background(WarnaIos.Latar).padding(vertical = 10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AngkaGelap("Order", r.jumlahOrder.toString(), Modifier.weight(1f))
                Box(Modifier.width(0.5.dp).height(30.dp).background(WarnaIos.Pemisah))
                AngkaGelap("Rata-rata", formatRupiah(r.rataRataOrder), Modifier.weight(1f))
            }
            Box(Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth().height(0.5.dp).background(WarnaIos.Pemisah))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AngkaGelap(
                    "Jam tersibuk",
                    r.jamTersibuk?.let { "%02d:00".format(it) } ?: "—",
                    Modifier.weight(1f),
                )
                Box(Modifier.width(0.5.dp).height(30.dp).background(WarnaIos.Pemisah))
                AngkaGelap("Dibatalkan", r.jumlahBatal.toString(), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AngkaGelap(label: String, nilai: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = TipeIos.Kecil.copy(fontWeight = FontWeight.Medium))
        Spacer(Modifier.height(2.dp))
        Text(
            nilai, color = WarnaIos.Label, fontSize = 17.sp, fontWeight = FontWeight.Bold,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun KartuKanal(baris: List<BarisKanal>, omzetTotal: Double) {
    GrupIos(judul = "Per kanal") {
        baris.forEachIndexed { i, b ->
            if (i > 0) PemisahIos()
            Row(Modifier.padding(horizontal = 16.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(b.kanal.label, style = TipeIos.Keterangan.copy(fontWeight = FontWeight.SemiBold))
                    Text("${b.jumlahOrder} order · ${porsiPersen(b.omzet, omzetTotal)}", style = TipeIos.Catatan)
                }
                Text(formatRupiah(b.omzet), color = NadaIos.AKSEN.teks, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun KartuOutlet(baris: List<BarisOutletJual>) {
    GrupIos(judul = "Per outlet") {
        baris.forEachIndexed { i, b ->
            if (i > 0) PemisahIos(inset = 50.dp)
            Row(Modifier.padding(horizontal = 16.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(24.dp).clip(CircleShape).background(WarnaIos.Isian),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("${i + 1}", color = WarnaIos.LabelKedua, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        b.nama, style = TipeIos.Keterangan.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Text("${b.jumlahOrder} order · ${formatAngkaStok(b.porsi)} porsi", style = TipeIos.Catatan)
                }
                Spacer(Modifier.width(8.dp))
                Text(formatRupiah(b.omzet), color = WarnaIos.Label, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun JudulSeksi(teks: String) {
    JudulSeksiIos(teks)
}

@Composable
private fun BarisMenu(m: BarisMenuLaris) {
    KartuIos(padding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(m.nama, style = TipeIos.Keterangan.copy(fontWeight = FontWeight.SemiBold), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${formatAngkaStok(m.qty)} terjual", style = TipeIos.Catatan)
            }
            Spacer(Modifier.width(8.dp))
            Text(formatRupiah(m.omzet), color = NadaIos.AKSEN.teks, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** Porsi kontribusi terhadap total; total nol tidak pernah dijadikan pembagi. */
private fun porsiPersen(nilai: Double, total: Double): String =
    if (total <= 0.0) "—" else "%.0f%%".format(nilai / total * 100)
