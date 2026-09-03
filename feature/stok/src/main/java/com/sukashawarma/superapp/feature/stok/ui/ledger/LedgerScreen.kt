package com.sukashawarma.superapp.feature.stok.ui.ledger

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.sukashawarma.superapp.feature.stok.data.LedgerRepository
import com.sukashawarma.superapp.feature.stok.data.StokRepository
import com.sukashawarma.superapp.feature.stok.data.model.LedgerDetailRow
import com.sukashawarma.superapp.feature.stok.data.model.LedgerTransaksi
import com.sukashawarma.superapp.feature.stok.data.model.OutletRingkas
import com.sukashawarma.superapp.feature.stok.domain.formatAngkaStok
import com.sukashawarma.superapp.feature.stok.domain.stokErrorMessage
import com.sukashawarma.superapp.feature.stok.ui.HeaderStok
import com.sukashawarma.superapp.feature.stok.ui.KeadaanGagal
import com.sukashawarma.superapp.feature.stok.ui.KeadaanKosong
import com.sukashawarma.superapp.feature.stok.ui.KeadaanTidakBerhak
import com.sukashawarma.superapp.feature.stok.ui.MemuatPenuh
import com.sukashawarma.superapp.feature.stok.ui.PemilihOutlet
import com.sukashawarma.superapp.feature.stok.ui.waktuSingkat
import com.sukashawarma.superapp.presentation.theme.SukaOnSurface
import com.sukashawarma.superapp.presentation.theme.SukaOnSurfaceVariant
import com.sukashawarma.superapp.presentation.theme.SukaSurface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class LedgerUiState(
    val memuat: Boolean = true,
    val memuatLagi: Boolean = false,
    val error: String? = null,
    val tidakBerhak: Boolean = false,
    val outlets: List<OutletRingkas> = emptyList(),
    val outletTerpilih: OutletRingkas? = null,
    val transaksi: List<LedgerTransaksi> = emptyList(),
    val habis: Boolean = false,
    val detailUntuk: LedgerTransaksi? = null,
    val detail: List<LedgerDetailRow> = emptyList(),
    val memuatDetail: Boolean = false,
)

class LedgerViewModel : ViewModel() {

    private val _state = MutableStateFlow(LedgerUiState())
    val state: StateFlow<LedgerUiState> = _state
    private var halaman = 0

    init { muatAwal() }

    fun muatAwal() {
        viewModelScope.launch {
            _state.value = _state.value.copy(memuat = true, error = null, tidakBerhak = false)
            try {
                val outlets = StokRepository.accessibleOutlets()
                if (outlets.isEmpty()) {
                    _state.value = _state.value.copy(memuat = false, tidakBerhak = true)
                    return@launch
                }
                val terpilih = _state.value.outletTerpilih?.let { lama ->
                    outlets.firstOrNull { it.id == lama.id }
                } ?: outlets.first()
                _state.value = _state.value.copy(outlets = outlets, outletTerpilih = terpilih)
                muatHalamanPertama()
            } catch (e: Exception) {
                _state.value = _state.value.copy(memuat = false, error = stokErrorMessage(e))
            }
        }
    }

    fun pilihOutlet(outlet: OutletRingkas) {
        if (outlet.id == _state.value.outletTerpilih?.id) return
        _state.value = _state.value.copy(outletTerpilih = outlet, transaksi = emptyList())
        viewModelScope.launch { muatHalamanPertama() }
    }

    private suspend fun muatHalamanPertama() {
        val outlet = _state.value.outletTerpilih ?: return
        halaman = 0
        _state.value = _state.value.copy(memuat = true, error = null, habis = false)
        try {
            val baris = LedgerRepository.daftar(outlet.id, 0)
            _state.value = _state.value.copy(
                memuat = false,
                transaksi = baris,
                habis = baris.size < LedgerRepository.PAGE_SIZE,
            )
        } catch (e: Exception) {
            _state.value = _state.value.copy(memuat = false, error = stokErrorMessage(e))
        }
    }

    fun muatLagi() {
        val s = _state.value
        if (s.memuat || s.memuatLagi || s.habis) return
        val outlet = s.outletTerpilih ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(memuatLagi = true)
            try {
                val berikut = LedgerRepository.daftar(outlet.id, halaman + 1)
                halaman += 1
                _state.value = _state.value.copy(
                    memuatLagi = false,
                    transaksi = _state.value.transaksi + berikut,
                    habis = berikut.size < LedgerRepository.PAGE_SIZE,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(memuatLagi = false, error = stokErrorMessage(e))
            }
        }
    }

    fun bukaDetail(t: LedgerTransaksi) {
        val outlet = _state.value.outletTerpilih ?: return
        _state.value = _state.value.copy(detailUntuk = t, detail = emptyList(), memuatDetail = true)
        viewModelScope.launch {
            try {
                // Kunci transaksi untuk waste pending bukan id ledger, jadi tidak ada
                // rinciannya — kartu ringkasnya sudah memuat seluruh informasi.
                val kunci = if (t.wastePending) null else t.transaksiKey
                val rows = if (kunci == null) emptyList()
                else LedgerRepository.detail(outlet.id, kunci)
                _state.value = _state.value.copy(detail = rows, memuatDetail = false)
            } catch (_: Exception) {
                _state.value = _state.value.copy(memuatDetail = false)
            }
        }
    }

    fun tutupDetail() {
        _state.value = _state.value.copy(detailUntuk = null, detail = emptyList())
    }

    fun segarkan() {
        StokRepository.invalidate()
        muatAwal()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerScreen(viewModel: LedgerViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val sheetState = rememberModalBottomSheetState()

    Column(Modifier.fillMaxSize().background(SukaSurface)) {
        HeaderStok(judul = "Buku Ledger Stok", subjudul = "Kartu stok masuk, keluar & saldo") {
            IconButton(onClick = viewModel::segarkan) {
                Icon(Icons.Default.Refresh, "Segarkan", tint = Color.White)
            }
        }

        if (!state.tidakBerhak && state.outlets.size > 1) {
            PemilihOutlet(state.outlets, state.outletTerpilih, viewModel::pilihOutlet)
        }

        when {
            state.tidakBerhak -> KeadaanTidakBerhak(
                "Akun Anda belum terhubung dengan outlet mana pun."
            )
            state.memuat -> MemuatPenuh()
            state.error != null -> KeadaanGagal(state.error!!, viewModel::muatAwal)
            state.transaksi.isEmpty() -> KeadaanKosong("Belum ada mutasi stok di outlet ini.")
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                items(state.transaksi, key = { it.transaksiKey }) { t ->
                    KartuTransaksi(t) { viewModel.bukaDetail(t) }
                }
                if (!state.habis) {
                    item(key = "lagi") {
                        Surface(
                            Modifier.fillMaxWidth().clickable { viewModel.muatLagi() },
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE7ECF2)),
                        ) {
                            Text(
                                if (state.memuatLagi) "Memuat…" else "Muat lebih banyak",
                                Modifier.fillMaxWidth().padding(14.dp),
                                color = Color(0xFFEA580C),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }

    val detailUntuk = state.detailUntuk
    if (detailUntuk != null) {
        ModalBottomSheet(onDismissRequest = viewModel::tutupDetail, sheetState = sheetState) {
            Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 28.dp)) {
                Text(judulTransaksi(detailUntuk), color = SukaOnSurface, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    waktuSingkat(detailUntuk.createdAt),
                    color = SukaOnSurfaceVariant,
                    fontSize = 11.sp,
                )
                Spacer(Modifier.height(14.dp))
                when {
                    state.memuatDetail -> Text("Memuat rincian…", color = SukaOnSurfaceVariant, fontSize = 12.sp)
                    state.detail.isEmpty() -> Text(
                        detailUntuk.singleCatatan ?: "Tidak ada rincian tambahan.",
                        color = SukaOnSurfaceVariant,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                    )
                    else -> state.detail.forEach { row -> BarisDetail(row) }
                }
            }
        }
    }
}

private fun judulTransaksi(t: LedgerTransaksi): String = when {
    t.wastePending -> "Waste menunggu persetujuan"
    t.orderNumber != null -> "Penjualan #${t.orderNumber}"
    t.opnameTanggal != null -> "Opname ${t.opnameTipe.orEmpty()} ${t.opnameTanggal}".trim()
    t.shipmentDestOutletName != null -> "Kirim surat jalan ke ${t.shipmentDestOutletName}"
    t.refTransferId != null -> "Mutasi antar outlet"
    t.gabungan -> "${t.jumlahBahan} bahan"
    else -> labelTipe(t.singleTipe)
}

private fun labelTipe(tipe: String?): String = when (tipe) {
    "terima_kiriman" -> "Terima kiriman"
    "pembelian_supplier" -> "Pembelian supplier"
    "transfer_masuk" -> "Transfer masuk"
    "pemakaian" -> "Pemakaian"
    "waste" -> "Waste"
    "waste_pending" -> "Waste menunggu persetujuan"
    "transfer_keluar" -> "Transfer keluar"
    "opname_selisih" -> "Selisih opname"
    "adjustment" -> "Penyesuaian"
    "rejected_kiriman" -> "Kiriman ditolak"
    null -> "Mutasi"
    else -> tipe.replace('_', ' ').replaceFirstChar { it.uppercase() }
}

@Composable
private fun KartuTransaksi(t: LedgerTransaksi, onKlik: () -> Unit) {
    val menambah = (t.singleQty ?: 0.0) >= 0
    val warna = when {
        t.wastePending -> Color(0xFFC27A12)
        menambah -> Color(0xFF168451)
        else -> Color(0xFFDC2626)
    }
    Surface(
        Modifier.fillMaxWidth().clickable(onClick = onKlik),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(36.dp).background(warna.copy(alpha = 0.10f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    when {
                        t.wastePending -> Icons.Default.HourglassEmpty
                        menambah -> Icons.Default.ArrowUpward
                        else -> Icons.Default.ArrowDownward
                    },
                    null, tint = warna, modifier = Modifier.size(17.dp),
                )
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    judulTransaksi(t),
                    color = SukaOnSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(waktuSingkat(t.createdAt), color = Color(0xFF94A3B8), fontSize = 10.sp)
                val jejak = t.orderItemsNames ?: t.singleCatatan
                if (!jejak.isNullOrBlank()) {
                    Text(
                        jejak,
                        color = SukaOnSurfaceVariant,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                if (t.gabungan) {
                    Text("${t.jumlahBahan} bahan", color = warna, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                } else {
                    Text(
                        (if (menambah) "+" else "") + formatAngkaStok(t.singleQty ?: 0.0),
                        color = warna, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun BarisDetail(row: LedgerDetailRow) {
    val menambah = row.qty >= 0
    val warna = if (menambah) Color(0xFF168451) else Color(0xFFDC2626)
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                row.namaBahan ?: "(bahan tidak dikenal)",
                color = SukaOnSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(labelTipe(row.tipe), color = SukaOnSurfaceVariant, fontSize = 10.sp)
            if (!row.catatan.isNullOrBlank()) {
                Text(row.catatan, color = Color(0xFF94A3B8), fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                (if (menambah) "+" else "") + formatAngkaStok(row.qty),
                color = warna, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold,
            )
            if (row.saldoSebelum != null && row.saldoSesudah != null) {
                Text(
                    "${formatAngkaStok(row.saldoSebelum)} → ${formatAngkaStok(row.saldoSesudah)}",
                    color = Color(0xFF94A3B8), fontSize = 10.sp,
                )
            }
        }
    }
}
