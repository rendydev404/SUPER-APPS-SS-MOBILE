package com.sukashawarma.superapp.feature.stok.ui.laporan

import com.sukashawarma.superapp.core.ui.ios.KartuIos
import com.sukashawarma.superapp.core.ui.ios.KolomCariIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.TombolBundarIos
import com.sukashawarma.superapp.core.ui.ios.TombolKapsulIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.ios.permukaanIos
import com.sukashawarma.superapp.core.ui.ios.tekanIos
import com.sukashawarma.superapp.feature.stok.ui.MemuatPenuh
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import com.sukashawarma.superapp.core.ui.SukaDropdownHeader
import com.sukashawarma.superapp.core.ui.SukaDropdownMenu
import com.sukashawarma.superapp.core.ui.SukaDropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.feature.stok.data.ArusBarangRepository
import com.sukashawarma.superapp.feature.stok.data.BarisArusBarang
import com.sukashawarma.superapp.feature.stok.data.StokRepository
import com.sukashawarma.superapp.feature.stok.data.model.OutletRingkas
import com.sukashawarma.superapp.feature.stok.domain.UnitScale
import com.sukashawarma.superapp.feature.stok.domain.formatAngkaStok
import com.sukashawarma.superapp.feature.stok.domain.stokErrorMessage
import com.sukashawarma.superapp.feature.stok.ui.HeaderStok
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs


data class ArusBarangUiState(
    val outlets: List<OutletRingkas> = emptyList(),
    val outletTerpilih: OutletRingkas? = null,
    val baris: List<BarisArusBarang> = emptyList(),
    val cari: String = "",
    val halaman: Int = 0,
    val adaLagi: Boolean = false,
    val memuat: Boolean = true,
    val memuatLagi: Boolean = false,
    val error: String? = null,
) {
    val tampil: List<BarisArusBarang>
        get() {
            val kueri = cari.trim().lowercase()
            if (kueri.isEmpty()) return baris
            return baris.filter {
                listOfNotNull(it.bahanNama, it.kategori, it.catatan, it.tujuanOutlet, it.nomorSj, it.nomorPo, it.supplier)
                    .any { teks -> teks.lowercase().contains(kueri) }
            }
        }
}

/**
 * Inbound / Outbound gudang pusat — cermin `hooks/useInboundOutbound.ts` dan
 * `InboundOutboundList.tsx`.
 *
 * View `inbound_outbound_feed` sudah meratakan ledger, surat jalan, dan PO
 * menjadi satu arus, jadi yang perlu dilakukan layar ini hanya membedakan arah:
 * qty positif berarti barang masuk gudang, negatif berarti keluar ke outlet.
 */
class ArusBarangViewModel : ViewModel() {
    private val _state = MutableStateFlow(ArusBarangUiState())
    val state: StateFlow<ArusBarangUiState> = _state

    init { muatAwal() }

    fun muatAwal() {
        viewModelScope.launch {
            _state.value = _state.value.copy(memuat = true, error = null)
            try {
                val outlets = StokRepository.accessibleOutlets()
                // Gudang pusat ditaruh paling depan: halaman ini memang tentang
                // arus barang di sana.
                val urut = outlets.sortedByDescending { it.name.uppercase().contains("GUDANG PUSAT") }
                val pilihan = _state.value.outletTerpilih ?: urut.firstOrNull()
                _state.value = _state.value.copy(outlets = urut, outletTerpilih = pilihan)
                if (pilihan != null) muatHalaman(pilihan.id, 0)
                else _state.value = _state.value.copy(memuat = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(memuat = false, error = stokErrorMessage(e))
            }
        }
    }

    private suspend fun muatHalaman(outletId: String, halaman: Int) {
        try {
            val hasil = ArusBarangRepository.daftar(outletId, halaman)
            _state.value = _state.value.copy(
                memuat = false,
                memuatLagi = false,
                halaman = halaman,
                baris = if (halaman == 0) hasil else _state.value.baris + hasil,
                adaLagi = hasil.size == ArusBarangRepository.PAGE_SIZE,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("ArusBarangVM", "muatHalaman() gagal", e)
            _state.value = _state.value.copy(memuat = false, memuatLagi = false, error = stokErrorMessage(e))
        }
    }

    fun pilihOutlet(outlet: OutletRingkas) {
        if (outlet.id == _state.value.outletTerpilih?.id) return
        _state.value = _state.value.copy(outletTerpilih = outlet, memuat = true, baris = emptyList())
        viewModelScope.launch { muatHalaman(outlet.id, 0) }
    }

    fun muatLagi() {
        val outletId = _state.value.outletTerpilih?.id ?: return
        if (_state.value.memuatLagi || !_state.value.adaLagi) return
        _state.value = _state.value.copy(memuatLagi = true)
        viewModelScope.launch { muatHalaman(outletId, _state.value.halaman + 1) }
    }

    fun ubahCari(teks: String) { _state.value = _state.value.copy(cari = teks) }
}

@Composable
fun ArusBarangScreen(
    onBack: () -> Unit,
    viewModel: ArusBarangViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    Column(Modifier.fillMaxSize().background(WarnaIos.Latar)) {
        HeaderStok(
            judul = "Inbound / Outbound",
            subjudul = state.outletTerpilih?.name ?: "Arus barang gudang",
            onKembali = onBack,
        ) {
            TombolBundarIos(IkonIos.Refresh, "Segarkan", viewModel::muatAwal)
        }

        if (state.memuat) {
            MemuatPenuh()
        } else {
            Column(Modifier.fillMaxSize()) {
                PemilihOutletArus(state, viewModel)
                KolomCariIos(
                    nilai = state.cari,
                    onUbah = viewModel::ubahCari,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = UkuranIos.TepiLayar),
                    placeholder = "Cari bahan, nomor SJ, atau PO…",
                )
                Spacer(Modifier.height(12.dp))

                if (state.error != null) {
                    PesanKosongLaporan(state.error!!)
                } else if (state.tampil.isEmpty()) {
                    PesanKosongLaporan("Belum ada arus barang pada outlet ini.")
                } else {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = UkuranIos.TepiLayar, end = UkuranIos.TepiLayar, bottom = 16.dp).denganRuangNav(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(state.tampil, key = { it.id }) { baris -> KartuArus(baris) }
                        if (state.adaLagi && state.cari.isBlank()) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(top = 4.dp), contentAlignment = Alignment.Center) {
                                    TombolKapsulIos(
                                        if (state.memuatLagi) "Memuat…" else "Muat lebih banyak",
                                        viewModel::muatLagi,
                                        ikon = IkonIos.ExpandMore,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PemilihOutletArus(state: ArusBarangUiState, viewModel: ArusBarangViewModel) {
    var terbuka by remember { mutableStateOf(false) }
    val bisaPilih = state.outlets.size > 1
    Box(Modifier.padding(horizontal = UkuranIos.TepiLayar, vertical = 12.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .permukaanIos(UkuranIos.SudutGrup)
                .then(if (bisaPilih) Modifier.tekanIos({ terbuka = true }) else Modifier)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(30.dp).clip(CircleShape).background(WarnaIos.Aksen.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(IkonIos.Storefront, null, tint = WarnaIos.Aksen, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(
                state.outletTerpilih?.name ?: "Pilih outlet",
                Modifier.weight(1f),
                style = TipeIos.Keterangan.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (bisaPilih) Icon(IkonIos.ArrowDropDown, null, tint = WarnaIos.Aksen, modifier = Modifier.size(16.dp))
        }
        SukaDropdownMenu(terbuka, { terbuka = false }) {
            SukaDropdownHeader(title = "PILIH OUTLET", onClose = { terbuka = false })
            state.outlets.forEach { outlet ->
                SukaDropdownMenuItem(
                    text = outlet.name,
                    selected = (state.outletTerpilih?.id == outlet.id),
                    onClick = { terbuka = false; viewModel.pilihOutlet(outlet) },
                )
            }
        }
    }
}

@Composable
private fun KartuArus(baris: BarisArusBarang) {
    val nada = if (baris.masuk) NadaIos.SUKSES else NadaIos.BAHAYA
    KartuIos(padding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                Modifier.size(34.dp).clip(CircleShape).background(nada.warna.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (baris.masuk) IkonIos.ArrowDownward else IkonIos.ArrowUpward,
                    if (baris.masuk) "Masuk" else "Keluar",
                    tint = nada.warna,
                    modifier = Modifier.size(17.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    baris.bahanNama,
                    style = TipeIos.Keterangan.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(
                        baris.kategori.takeIf { it.isNotBlank() },
                        baris.supplier,
                        baris.tujuanOutlet,
                        baris.nomorPo,
                        baris.nomorSj,
                    ).joinToString(" · "),
                    style = TipeIos.Catatan,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!baris.catatan.isNullOrBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(baris.catatan!!, style = TipeIos.Catatan.copy(color = WarnaIos.Label), maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    (if (baris.masuk) "+" else "−") + jumlahTampil(baris),
                    color = nada.teks,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
                if (baris.saldoSesudah != null) {
                    Text("sisa ${formatAngkaStok(baris.saldoSesudah!!)}", style = TipeIos.Kecil)
                }
            }
        }
    }
}

/** Jumlah dalam bentuk berjenjang bila faktornya memadai, angka mentah bila tidak. */
private fun jumlahTampil(baris: BarisArusBarang): String {
    val nilai = abs(baris.qty)
    return UnitScale.formatBerjenjang(nilai, baris.meta)
        ?: "${formatAngkaStok(nilai)} ${baris.meta.satuan.orEmpty()}".trim()
}
