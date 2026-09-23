package com.sukashawarma.superapp.feature.stok.ui.laporan

import com.sukashawarma.superapp.core.ui.ios.KartuIos
import com.sukashawarma.superapp.core.ui.ios.KeadaanIos
import com.sukashawarma.superapp.core.ui.ios.LencanaIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.PemisahIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.TombolBundarIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.feature.stok.ui.BannerIos
import com.sukashawarma.superapp.feature.stok.ui.MemuatPenuh
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.ui.text.style.TextAlign
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
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

    Column(Modifier.fillMaxSize().background(WarnaIos.Latar)) {
        HeaderStok(
            judul = "Nilai Persediaan",
            subjudul = if (state.memuat) "Memuat…" else "${state.outlets.size} outlet",
            onKembali = onBack,
            aksi = {
                TombolBundarIos(IkonIos.Refresh, "Muat ulang", viewModel::muatUlang)
            },
        )

        when {
            state.memuat -> MemuatPenuh()
            state.error != null -> PesanKosongLaporan(state.error!!)
            state.outlets.isEmpty() -> PesanKosongLaporan("Belum ada data nilai persediaan.")
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = UkuranIos.TepiLayar, vertical = 12.dp).denganRuangNav(),
                verticalArrangement = Arrangement.spacedBy(UkuranIos.JarakKartu),
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
    KartuIos {
        Text("Nilai persediaan seluruh outlet", style = TipeIos.SubJudul)
        Spacer(Modifier.height(4.dp))
        Text(formatRupiah(total.total), style = TipeIos.JudulBesar.copy(fontSize = 30.sp))
        Spacer(Modifier.height(2.dp))
        Text(
            "Rentang ${formatRupiah(total.batasBawah)} – ${formatRupiah(total.batasAtas)}",
            style = TipeIos.Catatan,
        )

        Spacer(Modifier.height(14.dp))
        Row(
            Modifier.fillMaxWidth().clip(UkuranIos.SudutBlok).background(WarnaIos.Latar).padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AngkaKecil("Pasti", formatRupiah(total.nilaiPasti), Modifier.weight(1f))
            Box(Modifier.width(0.5.dp).height(30.dp).background(WarnaIos.Pemisah))
            AngkaKecil("Belum pasti", formatRupiah(total.nilaiBelumPasti), Modifier.weight(1f))
        }
        if (total.jumlahBelumPasti > 0 || total.jumlahDataKurang > 0) {
            Spacer(Modifier.height(10.dp))
            BannerIos(
                buildString {
                    if (total.jumlahBelumPasti > 0) {
                        append("${total.jumlahBelumPasti} bahan skalanya belum dipastikan opname")
                    }
                    if (total.jumlahDataKurang > 0) {
                        if (isNotEmpty()) append(" · ")
                        append("${total.jumlahDataKurang} bahan belum punya harga atau isi kemasan")
                    }
                },
                NadaIos.PERINGATAN,
                ikon = IkonIos.WarningAmber,
            )
        }
    }
}

@Composable
private fun AngkaKecil(label: String, nilai: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = TipeIos.Kecil.copy(fontWeight = FontWeight.Medium))
        Spacer(Modifier.height(2.dp))
        Text(
            nilai,
            color = WarnaIos.Label, fontSize = 16.sp, fontWeight = FontWeight.Bold,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun KartuOutletNilai(outlet: RingkasNilaiOutlet, terbuka: Boolean, onKlik: () -> Unit) {
    KartuIos(onKlik = onKlik) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(outlet.outlet, style = TipeIos.Utama, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${outlet.jumlahBahan} bahan", style = TipeIos.Catatan)
            }
            Spacer(Modifier.width(8.dp))
            Text(formatRupiah(outlet.total), style = TipeIos.Angka.copy(fontSize = 17.sp))
            Spacer(Modifier.width(4.dp))
            Icon(
                if (terbuka) IkonIos.ExpandLess else IkonIos.ExpandMore, null,
                tint = WarnaIos.LabelKetiga, modifier = Modifier.size(18.dp),
            )
        }

        if (outlet.jumlahBelumPasti > 0 || outlet.jumlahDataKurang > 0) {
            Spacer(Modifier.height(8.dp))
            LencanaIos(
                listOfNotNull(
                    outlet.jumlahBelumPasti.takeIf { it > 0 }?.let { "$it skala belum pasti" },
                    outlet.jumlahDataKurang.takeIf { it > 0 }?.let { "$it data belum lengkap" },
                ).joinToString(" · "),
                NadaIos.PERINGATAN,
            )
        }

        if (terbuka) {
            Spacer(Modifier.height(12.dp))
            // Sepuluh bahan termahal saja: sisanya jarang mengubah keputusan,
            // dan menampilkan ratusan baris di dalam kartu justru mengubur
            // yang penting.
            Column(Modifier.fillMaxWidth().clip(UkuranIos.SudutBlok).background(WarnaIos.Latar)) {
                outlet.items.take(10).forEachIndexed { i, baris ->
                    if (i > 0) PemisahIos(inset = 12.dp)
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                baris.bahan,
                                style = TipeIos.Keterangan.copy(fontSize = 15.sp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (baris.status != StatusNilai.PASTI) {
                                Text(baris.status.label, style = TipeIos.Kecil.copy(color = NadaIos.PERINGATAN.teks, fontWeight = FontWeight.Medium))
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(formatRupiah(baris.nilai), color = WarnaIos.LabelKedua, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            if (outlet.items.size > 10) {
                Spacer(Modifier.height(6.dp))
                Text("${outlet.items.size - 10} bahan lain tidak ditampilkan.", style = TipeIos.Kecil)
            }
        }
    }
}

/** Keadaan kosong/gagal bersama untuk layar laporan — gaya [KeadaanIos] tanpa judul terpisah. */
@Composable
internal fun PesanKosongLaporan(teks: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(56.dp).clip(CircleShape).background(WarnaIos.Isian),
                contentAlignment = Alignment.Center,
            ) {
                Icon(IkonIos.Inbox, null, tint = WarnaIos.Abu, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(
                teks,
                style = TipeIos.SubJudul.copy(lineHeight = 21.sp),
                textAlign = TextAlign.Center,
            )
        }
    }
}
