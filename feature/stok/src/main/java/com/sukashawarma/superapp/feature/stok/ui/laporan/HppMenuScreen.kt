package com.sukashawarma.superapp.feature.stok.ui.laporan

import com.sukashawarma.superapp.core.ui.ios.KartuIos
import com.sukashawarma.superapp.core.ui.ios.KolomCariIos
import com.sukashawarma.superapp.core.ui.ios.LabelSeksiIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.PemisahIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.TombolBundarIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.feature.stok.ui.BannerIos
import com.sukashawarma.superapp.feature.stok.ui.MemuatPenuh
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.width
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
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.RealtimeTables


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

    // HPP menu ikut bergeser begitu harga bahan diperbarui area manager.
    RealtimeRefresh(RealtimeTables.BAHAN_BAKU_HARGA, RealtimeTables.BAHAN_BAKU) { viewModel.muatUlang() }

    Column(Modifier.fillMaxSize().background(WarnaIos.Latar)) {
        HeaderStok(
            judul = "HPP Setiap Menu",
            subjudul = when {
                state.memuat -> "Memuat…"
                state.jumlahKritis > 0 -> "${state.menu.size} menu · ${state.jumlahKritis} food cost kritis"
                else -> "${state.menu.size} menu"
            },
            onKembali = onBack,
            aksi = {
                TombolBundarIos(IkonIos.Refresh, "Muat ulang", viewModel::muatUlang)
            },
        )

        when {
            state.memuat -> MemuatPenuh()
            state.error != null -> PesanKosongLaporan(state.error!!)
            else -> Column(Modifier.fillMaxSize()) {
                KolomCariIos(
                    nilai = state.cari,
                    onUbah = viewModel::ubahCari,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = UkuranIos.TepiLayar, vertical = 12.dp),
                    placeholder = "Cari menu atau kategori…",
                )
                if (state.tampil.isEmpty()) {
                    PesanKosongLaporan("Tidak ada menu yang cocok.")
                } else {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = UkuranIos.TepiLayar, end = UkuranIos.TepiLayar, bottom = 16.dp).denganRuangNav(),
                        verticalArrangement = Arrangement.spacedBy(UkuranIos.JarakKartu),
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
    val nada = when (menu.status) {
        StatusFoodCost.OPTIMAL -> NadaIos.SUKSES
        StatusFoodCost.WASPADA -> NadaIos.PERINGATAN
        StatusFoodCost.KRITIS -> NadaIos.BAHAYA
    }
    KartuIos(onKlik = onKlik) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(menu.menuNama, style = TipeIos.Utama, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(menu.kategori, style = TipeIos.Catatan)
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    if (menu.hargaJual > 0) "${menu.foodCostPersen.toInt()}%" else "—",
                    style = TipeIos.AngkaBesar.copy(fontSize = 22.sp, color = nada.teks),
                )
                Text("Food cost", style = TipeIos.Kecil)
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth().clip(UkuranIos.SudutBlok).background(WarnaIos.Latar).padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            KolomAngka("HPP", formatRupiah(menu.totalHpp), Modifier.weight(1f))
            Box(Modifier.width(0.5.dp).height(30.dp).background(WarnaIos.Pemisah))
            KolomAngka(
                "Harga jual",
                if (menu.hargaJual > 0) formatRupiah(menu.hargaJual) else "Belum diisi",
                Modifier.weight(1f),
            )
            Box(Modifier.width(0.5.dp).height(30.dp).background(WarnaIos.Pemisah))
            KolomAngka("Margin", formatRupiah(menu.marginRupiah), Modifier.weight(1f))
        }

        if (menu.hargaJual <= 0.0) {
            Spacer(Modifier.height(8.dp))
            BannerIos(
                "Menu ini belum punya harga jual, jadi food cost-nya tidak bisa dihitung.",
                NadaIos.PERINGATAN,
            )
        }

        if (terbuka) {
            Spacer(Modifier.height(14.dp))
            LabelSeksiIos("Susunan biaya", Modifier.padding(start = 4.dp))
            Spacer(Modifier.height(6.dp))
            if (menu.bahan.isNotEmpty()) {
                Column(Modifier.fillMaxWidth().clip(UkuranIos.SudutBlok).background(WarnaIos.Latar)) {
                    menu.bahan.forEachIndexed { i, bahan ->
                        if (i > 0) PemisahIos(inset = 12.dp)
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    bahan.nama,
                                    style = TipeIos.Keterangan.copy(fontSize = 15.sp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    "${formatAngkaStok(bahan.qtyPerPorsi)} ${bahan.satuanResep} · ${bahan.kontribusiPersen}%",
                                    style = TipeIos.Kecil,
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(formatRupiah(bahan.subtotal), color = WarnaIos.LabelKedua, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            if (menu.bahan.isEmpty()) {
                Text("Resep ini belum punya bahan, jadi HPP-nya nol.", style = TipeIos.Catatan)
            }
        }
    }
}

@Composable
private fun KolomAngka(label: String, nilai: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = TipeIos.Kecil.copy(fontWeight = FontWeight.Medium), maxLines = 1)
        Spacer(Modifier.height(2.dp))
        Text(
            nilai, color = WarnaIos.Label, fontSize = 14.sp, fontWeight = FontWeight.Bold,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}
