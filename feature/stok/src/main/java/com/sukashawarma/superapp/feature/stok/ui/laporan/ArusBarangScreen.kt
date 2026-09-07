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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.RealtimeTables
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

private val ORANGE = Color(0xFFEA580C)
private val SLATE400 = Color(0xFF94A3B8)
private val SLATE500 = Color(0xFF64748B)
private val SLATE900 = Color(0xFF0F172A)
private val GARIS = Color(0xFFE2E8F0)
private val HIJAU = Color(0xFF15803D)
private val HIJAU_LATAR = Color(0xFFDCFCE7)
private val MERAH = Color(0xFFB91C1C)
private val MERAH_LATAR = Color(0xFFFEE2E2)

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
    RealtimeRefresh(RealtimeTables.LEDGER) { viewModel.muatAwal() }

    Column(Modifier.fillMaxSize().background(Color(0xFFF8FAFC))) {
        HeaderStok(
            judul = "Inbound / Outbound",
            subjudul = state.outletTerpilih?.name ?: "Arus barang gudang",
            onKembali = onBack,
        )

        if (state.memuat) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ORANGE)
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                PemilihOutletArus(state, viewModel)
                OutlinedTextField(
                    value = state.cari,
                    onValueChange = viewModel::ubahCari,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    placeholder = { Text("Cari bahan, nomor SJ, atau PO…", fontSize = 12.5.sp, color = SLATE400) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = ORANGE,
                        unfocusedBorderColor = GARIS,
                    ),
                )
                Spacer(Modifier.height(12.dp))

                if (state.error != null) {
                    PesanKosongLaporan(state.error!!)
                } else if (state.tampil.isEmpty()) {
                    PesanKosongLaporan("Belum ada arus barang pada outlet ini.")
                } else {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        items(state.tampil, key = { it.id }) { baris -> KartuArus(baris) }
                        if (state.adaLagi && state.cari.isBlank()) {
                            item {
                                TextButton(
                                    onClick = viewModel::muatLagi,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        if (state.memuatLagi) "Memuat…" else "Muat lebih banyak",
                                        color = ORANGE,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
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
    Box(Modifier.padding(16.dp)) {
        Surface(
            Modifier.fillMaxWidth().clickable(enabled = state.outlets.size > 1) { terbuka = true },
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            border = BorderStroke(1.dp, GARIS),
        ) {
            Row(Modifier.padding(horizontal = 13.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    state.outletTerpilih?.name ?: "Pilih outlet",
                    Modifier.weight(1f),
                    color = SLATE900,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (state.outlets.size > 1) Icon(Icons.Default.ArrowDropDown, null, tint = SLATE500)
            }
        }
        DropdownMenu(terbuka, { terbuka = false }) {
            state.outlets.forEach { outlet ->
                DropdownMenuItem(
                    text = { Text(outlet.name, fontSize = 13.sp) },
                    onClick = { terbuka = false; viewModel.pilihOutlet(outlet) },
                )
            }
        }
    }
}

@Composable
private fun KartuArus(baris: BarisArusBarang) {
    val warna = if (baris.masuk) HIJAU else MERAH
    val latar = if (baris.masuk) HIJAU_LATAR else MERAH_LATAR
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(13.dp),
        color = Color.White,
        border = BorderStroke(1.dp, GARIS),
    ) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.Top) {
            Box(
                Modifier.size(32.dp).clip(CircleShape).background(latar),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (baris.masuk) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    if (baris.masuk) "Masuk" else "Keluar",
                    tint = warna,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    baris.bahanNama,
                    color = SLATE900,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
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
                    color = SLATE400,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!baris.catatan.isNullOrBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(baris.catatan!!, color = SLATE500, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.width(9.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    (if (baris.masuk) "+" else "−") + jumlahTampil(baris),
                    color = warna,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Black,
                )
                if (baris.saldoSesudah != null) {
                    Text(
                        "sisa ${formatAngkaStok(baris.saldoSesudah!!)}",
                        color = SLATE400,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                    )
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
