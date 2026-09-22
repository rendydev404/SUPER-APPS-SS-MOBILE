package com.sukashawarma.superapp.feature.stok.ui.ledger

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.RealtimeTables
import com.sukashawarma.superapp.feature.stok.data.LedgerRepository
import com.sukashawarma.superapp.feature.stok.data.PermintaanRepository
import com.sukashawarma.superapp.feature.stok.data.StokRepository
import com.sukashawarma.superapp.feature.stok.data.model.BahanBaku
import com.sukashawarma.superapp.feature.stok.data.model.LedgerAuditDetail
import com.sukashawarma.superapp.feature.stok.data.model.LedgerDetailRow
import com.sukashawarma.superapp.feature.stok.data.model.LedgerTransaksi
import com.sukashawarma.superapp.feature.stok.data.model.OrderItemRow
import com.sukashawarma.superapp.feature.stok.data.model.OutletRingkas
import com.sukashawarma.superapp.feature.stok.domain.DeliveryUnits
import com.sukashawarma.superapp.feature.stok.domain.UnitScale
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
import com.sukashawarma.superapp.presentation.theme.SukaOrange
import com.sukashawarma.superapp.presentation.theme.SukaSurface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * 6 Kategori filter pada ledger stok — cermin `FILTER_LABELS` di web.
 */
enum class KategoriLedger(val label: String, val icon: ImageVector) {
    SEMUA("Semua", Icons.Default.Inventory2),
    MASUK("Masuk", Icons.Default.ArrowDownward),
    ORDER("Order", Icons.Default.Receipt),
    WASTE("Waste", Icons.Default.Delete),
    KELUAR("Keluar", Icons.Default.ArrowUpward),
    PENYESUAIAN("Penyesuaian", Icons.Default.Scale);
}

data class LedgerUiState(
    val memuat: Boolean = true,
    val memuatLagi: Boolean = false,
    val error: String? = null,
    val tidakBerhak: Boolean = false,
    val outlets: List<OutletRingkas> = emptyList(),
    val outletTerpilih: OutletRingkas? = null,
    val transaksi: List<LedgerTransaksi> = emptyList(),
    val habis: Boolean = false,
    val kataKunci: String = "",
    val filterAktif: KategoriLedger = KategoriLedger.SEMUA,
    val detailUntuk: LedgerTransaksi? = null,
    val detail: List<LedgerDetailRow> = emptyList(),
    val orderDetails: List<OrderItemRow> = emptyList(),
    val singleAuditDetail: LedgerAuditDetail? = null,
    val memuatDetail: Boolean = false,
    val bukaGlosarium: Boolean = false,
    val daftarBahanGlosarium: List<BahanBaku> = emptyList(),
    val memuatGlosarium: Boolean = false,
) {
    val transaksiTerfilter: List<LedgerTransaksi> get() {
        val q = kataKunci.trim().lowercase(Locale.ROOT)
        return transaksi.filter { t ->
            val label = LedgerRepository.transaksiLabel(t)
            val titleMatch = label.title.lowercase(Locale.ROOT).contains(q)
            val subtitleMatch = label.subtitle?.lowercase(Locale.ROOT)?.contains(q) ?: false
            val namaBahanMatch = t.singleNamaBahan?.lowercase(Locale.ROOT)?.contains(q) ?: false
            val catatanMatch = t.singleCatatan?.lowercase(Locale.ROOT)?.contains(q) ?: false
            val orderMenuMatch = t.orderItemsNames?.lowercase(Locale.ROOT)?.contains(q) ?: false
            val matchesSearch = q.isEmpty() || titleMatch || subtitleMatch || namaBahanMatch || catatanMatch || orderMenuMatch

            val matchesFilter = when (filterAktif) {
                KategoriLedger.SEMUA -> true
                KategoriLedger.MASUK -> {
                    val isTerima = t.refShipmentId != null ||
                        t.singleTipe in listOf("terima_kiriman", "transfer_masuk", "pembelian_supplier")
                    t.refOrderId == null && isTerima
                }
                KategoriLedger.ORDER -> t.refOrderId != null
                KategoriLedger.WASTE -> t.singleTipe in listOf("waste", "waste_pending")
                KategoriLedger.KELUAR -> {
                    val isOutboundTransfer = t.refTransferId != null && t.singleTipe != "transfer_masuk"
                    t.refOrderId == null && (isOutboundTransfer || t.singleTipe in listOf("pemakaian", "transfer_keluar"))
                }
                KategoriLedger.PENYESUAIAN -> {
                    t.refOpnameId != null || t.singleTipe in listOf("adjustment", "opname_selisih")
                }
            }

            matchesSearch && matchesFilter
        }
    }
}

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

    fun ubahKataKunci(query: String) {
        _state.value = _state.value.copy(kataKunci = query)
    }

    fun pilihFilter(kategori: KategoriLedger) {
        _state.value = _state.value.copy(filterAktif = kategori)
    }

    fun bukaGlosarium() {
        _state.value = _state.value.copy(bukaGlosarium = true)
        if (_state.value.daftarBahanGlosarium.isEmpty()) {
            viewModelScope.launch {
                _state.value = _state.value.copy(memuatGlosarium = true)
                try {
                    val bahan = PermintaanRepository.bahanBaku()
                    _state.value = _state.value.copy(daftarBahanGlosarium = bahan, memuatGlosarium = false)
                } catch (_: Exception) {
                    _state.value = _state.value.copy(memuatGlosarium = false)
                }
            }
        }
    }

    fun tutupGlosarium() {
        _state.value = _state.value.copy(bukaGlosarium = false)
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
        _state.value = _state.value.copy(
            detailUntuk = t,
            detail = emptyList(),
            orderDetails = emptyList(),
            singleAuditDetail = null,
            memuatDetail = true,
        )
        viewModelScope.launch {
            try {
                val kunci = if (t.wastePending) null else t.transaksiKey

                // 1. Jika ini transaksi Order / Penjualan, ambil order_items (X Porsi) saja
                if (t.refOrderId != null) {
                    val orderRows = LedgerRepository.orderItems(t.refOrderId)
                    _state.value = _state.value.copy(
                        orderDetails = orderRows,
                        detail = emptyList(),
                        memuatDetail = false,
                    )
                    return@launch
                }

                // 2. Jika ini transaksi tunggal (manual / waste / adjustment), ambil audit detail lengkap
                if (!t.gabungan && !t.wastePending && kunci != null) {
                    val audit = LedgerRepository.auditDetail(outlet.id, kunci)
                    val detailRows = LedgerRepository.detail(outlet.id, kunci)
                    _state.value = _state.value.copy(
                        singleAuditDetail = audit,
                        detail = detailRows,
                        memuatDetail = false,
                    )
                    return@launch
                }

                // 3. Composite mutasi (Opname, Shipment, Transfer)
                val rows = if (kunci == null) emptyList() else LedgerRepository.detail(outlet.id, kunci)
                _state.value = _state.value.copy(detail = rows, memuatDetail = false)
            } catch (_: Exception) {
                _state.value = _state.value.copy(memuatDetail = false)
            }
        }
    }

    fun tutupDetail() {
        _state.value = _state.value.copy(
            detailUntuk = null,
            detail = emptyList(),
            orderDetails = emptyList(),
            singleAuditDetail = null,
        )
    }

    fun segarkan() {
        StokRepository.invalidate()
        muatAwal()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerScreen(
    onEntriManual: (() -> Unit)? = null,
    viewModel: LedgerViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    // Catatan Performa: Buku mutasi (858k+ baris) tidak mendengarkan realtime
    // agar tidak terjadi thundering herd / pemuatan berulang tiap kali kasir jualan.
    // Pengguna memuat data otomatis saat masuk layar, atau manual via tombol Segarkan di header.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val glosariumSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val pullRefreshState = rememberPullToRefreshState()
    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            viewModel.segarkan()
        }
    }
    LaunchedEffect(state.memuat) {
        if (!state.memuat) {
            pullRefreshState.endRefresh()
        }
    }

    Column(Modifier.fillMaxSize().background(SukaSurface)) {
        HeaderStok(judul = "Ledger Stok", subjudul = "Buku Kas & Riwayat Mutasi Bahan") {
            IconButton(onClick = viewModel::segarkan) {
                Icon(Icons.Default.Refresh, "Segarkan", tint = Color(0xFF1E293B))
            }
        }

        if (!state.tidakBerhak && state.outlets.size > 1) {
            PemilihOutlet(state.outlets, state.outletTerpilih, viewModel::pilihOutlet)
        }

        when {
            state.tidakBerhak -> KeadaanTidakBerhak("Akun Anda belum terhubung dengan outlet mana pun.")
            state.memuat && state.outlets.isEmpty() -> MemuatPenuh()
            state.error != null -> KeadaanGagal(state.error!!, viewModel::muatAwal)
            else -> {
                Column(Modifier.fillMaxSize()) {
                    // 1. Tombol Buat Entri Manual (persis web)
                    if (onEntriManual != null) {
                        Surface(
                            onClick = onEntriManual,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = SukaOrange,
                            shadowElevation = 1.dp,
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "BUAT ENTRI MANUAL",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp,
                                    )
                            }
                        }
                    }

                    // 2. Bar Pencarian
                    OutlinedTextField(
                        value = state.kataKunci,
                        onValueChange = viewModel::ubahKataKunci,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        placeholder = {
                            Text("Cari nama bahan baku atau nomor order/opname…", fontSize = 12.sp, color = Color(0xFF94A3B8))
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentModifier("Cari"), tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (state.kataKunci.isNotEmpty()) {
                                IconButton(onClick = { viewModel.ubahKataKunci("") }) {
                                    Icon(Icons.Default.Clear, "Hapus", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = SukaOrange,
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                        ),
                    )

                    // 3. Filter Chips Row + Tombol Glosarium
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        KategoriLedger.entries.forEach { kat ->
                            val aktif = kat == state.filterAktif
                            Surface(
                                onClick = { viewModel.pilihFilter(kat) },
                                shape = RoundedCornerShape(20.dp),
                                color = if (aktif) Color(0xFF0F172A) else Color.White,
                                border = BorderStroke(1.dp, if (aktif) Color(0xFF0F172A) else Color(0xFFE2E8F0)),
                                shadowElevation = if (aktif) 0.5.dp else 0.dp,
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(
                                        kat.icon,
                                        null,
                                        tint = if (aktif) Color.White else Color(0xFF64748B),
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Text(
                                        kat.label.uppercase(Locale.ROOT),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (aktif) Color.White else Color(0xFF475569),
                                        letterSpacing = 0.3.sp,
                                    )
                                }
                            }
                        }

                        // Tombol Glosarium
                        Surface(
                            onClick = viewModel::bukaGlosarium,
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFFDBA74)),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(Icons.Default.MenuBook, null, tint = SukaOrange, modifier = Modifier.size(14.dp))
                                Text(
                                    "GLOSARIUM",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SukaOrange,
                                    letterSpacing = 0.3.sp,
                                )
                            }
                        }
                    }

                    // 4. Daftar Transaksi (Pull-to-Refresh standar industri)
                    val terfilter = state.transaksiTerfilter
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(pullRefreshState.nestedScrollConnection)
                    ) {
                        if (terfilter.isEmpty() && !state.memuat) {
                            KeadaanKosong("Belum Ada Catatan Pergerakan")
                        } else {
                            LazyColumn(
                                Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(9.dp),
                            ) {
                                items(terfilter, key = { it.transaksiKey }) { t ->
                                    KartuTransaksi(t) { viewModel.bukaDetail(t) }
                                }
                                if (!state.habis && state.kataKunci.isEmpty() && state.filterAktif == KategoriLedger.SEMUA) {
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
                                                textAlign = TextAlign.Center,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        PullToRefreshContainer(
                            state = pullRefreshState,
                            modifier = Modifier.align(Alignment.TopCenter),
                            containerColor = Color.White,
                            contentColor = SukaOrange,
                        )
                    }
                }
            }
        }
    }

    // Modal BottomSheet Detail Transaksi
    val detailUntuk = state.detailUntuk
    if (detailUntuk != null) {
        ModalBottomSheet(onDismissRequest = viewModel::tutupDetail, sheetState = sheetState) {
            DetailBottomSheetContent(
                t = detailUntuk,
                memuat = state.memuatDetail,
                orderDetails = state.orderDetails,
                auditDetail = state.singleAuditDetail,
                detailRows = state.detail,
                onTutup = viewModel::tutupDetail,
            )
        }
    }

    // Modal Glosarium Satuan
    if (state.bukaGlosarium) {
        ModalBottomSheet(onDismissRequest = viewModel::tutupGlosarium, sheetState = glosariumSheetState) {
            GlosariumSatuanSheet(
                daftarBahan = state.daftarBahanGlosarium,
                memuat = state.memuatGlosarium,
                onTutup = viewModel::tutupGlosarium,
            )
        }
    }
}

private fun contentModifier(desc: String): String = desc

data class VisualTransaksi(
    val icon: ImageVector,
    val iconColor: Color,
    val bgColor: Color,
    val borderColor: Color,
)

private fun getTransaksiVisual(t: LedgerTransaksi): VisualTransaksi {
    if (t.refOrderId != null) {
        return VisualTransaksi(
            icon = Icons.Default.Receipt,
            iconColor = Color(0xFF2563EB),
            bgColor = Color(0xFFEFF6FF),
            borderColor = Color(0xFFDBEAFE),
        )
    }
    if (t.refOpnameId != null) {
        return VisualTransaksi(
            icon = Icons.Default.Tune,
            iconColor = Color(0xFFD97706),
            bgColor = Color(0xFFFFFBEB),
            borderColor = Color(0xFFFEF3C7),
        )
    }
    if (t.refShipmentId != null) {
        val isKirim = (t.singleQty ?: 0.0) < 0
        return VisualTransaksi(
            icon = if (isKirim) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
            iconColor = if (isKirim) Color(0xFFEA580C) else Color(0xFF168451),
            bgColor = if (isKirim) Color(0xFFFFF7ED) else Color(0xFFECFDF5),
            borderColor = if (isKirim) Color(0xFFFFEDD5) else Color(0xFFD1FAE5),
        )
    }
    if (t.refTransferId != null) {
        return VisualTransaksi(
            icon = Icons.Default.SwapHoriz,
            iconColor = Color(0xFFEA580C),
            bgColor = Color(0xFFFFF7ED),
            borderColor = Color(0xFFFFEDD5),
        )
    }
    if (t.singleTipe == "terima_kiriman" || t.singleTipe == "transfer_masuk" || t.singleTipe == "pembelian_supplier") {
        return VisualTransaksi(
            icon = Icons.Default.ArrowDownward,
            iconColor = Color(0xFF168451),
            bgColor = Color(0xFFECFDF5),
            borderColor = Color(0xFFD1FAE5),
        )
    }
    if (t.singleTipe == "waste" || t.singleTipe == "pemakaian") {
        return VisualTransaksi(
            icon = Icons.Default.Delete,
            iconColor = Color(0xFFDC2626),
            bgColor = Color(0xFFFEF2F2),
            borderColor = Color(0xFFFEE2E2),
        )
    }
    if (t.singleTipe == "waste_pending") {
        return VisualTransaksi(
            icon = Icons.Default.HourglassEmpty,
            iconColor = Color(0xFFD97706),
            bgColor = Color(0xFFFFFBEB),
            borderColor = Color(0xFFFEF3C7),
        )
    }
    if (t.singleTipe == "transfer_keluar") {
        return VisualTransaksi(
            icon = Icons.Default.ArrowUpward,
            iconColor = Color(0xFFEA580C),
            bgColor = Color(0xFFFFF7ED),
            borderColor = Color(0xFFFFEDD5),
        )
    }
    return VisualTransaksi(
        icon = Icons.Default.Scale,
        iconColor = Color(0xFF64748B),
        bgColor = Color(0xFFF8FAFC),
        borderColor = Color(0xFFE2E8F0),
    )
}

/**
 * Waktu relatif bergaya percakapan: misal "15 mnt lalu", "3 jam lalu", "Kemarin".
 */
private fun waktuRelatif(iso: String?): String {
    if (iso.isNullOrBlank()) return "-"
    return try {
        val instant = java.time.Instant.parse(iso)
        val now = java.time.Instant.now()
        val diffMins = java.time.Duration.between(instant, now).toMinutes()
        val diffHours = java.time.Duration.between(instant, now).toHours()
        val diffDays = java.time.Duration.between(instant, now).toDays()

        when {
            diffMins < 60 -> if (diffMins <= 0) "1 mnt lalu" else "$diffMins mnt lalu"
            diffHours < 24 -> "$diffHours jam lalu"
            diffDays == 1L -> "Kemarin"
            diffDays < 7L -> "$diffDays hr lalu"
            else -> waktuSingkat(iso)
        }
    } catch (_: Exception) {
        waktuSingkat(iso)
    }
}

@Composable
private fun KartuTransaksi(t: LedgerTransaksi, onKlik: () -> Unit) {
    val visual = getTransaksiVisual(t)
    val label = LedgerRepository.transaksiLabel(t)
    val isDelivery = t.refShipmentId != null || t.singleTipe == "transfer_keluar" ||
        t.singleCatatan?.contains("KIRIM SJ", ignoreCase = true) == true
    val isManual = !t.gabungan && !t.wastePending && t.refOrderId == null &&
        t.refOpnameId == null && t.refShipmentId == null && t.refTransferId == null
    val menambah = (t.singleQty ?: 0.0) >= 0

    Surface(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onKlik),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
        shadowElevation = 0.5.dp,
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            // Icon Kotak Visual
            Box(
                Modifier
                    .size(42.dp)
                    .background(visual.bgColor, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    visual.icon,
                    null,
                    tint = visual.iconColor,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(Modifier.width(12.dp))

            // Info Utama
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Badge Judul
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(0.5.dp, Color(0xFFE2E8F0)),
                    ) {
                        Text(
                            label.title.uppercase(Locale.ROOT),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = Color(0xFF475569),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp,
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        waktuRelatif(t.createdAt),
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp,
                    )
                }

                Spacer(Modifier.height(3.dp))

                // Headline Nama Bahan / Menu Order / Keterangan
                val headline = if (isManual) {
                    t.singleNamaBahan ?: "Bahan Baku"
                } else {
                    label.subtitle ?: "${t.jumlahBahan} bahan"
                }

                Text(
                    headline,
                    color = SukaOnSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                // Catatan jika manual
                if (isManual && !t.singleCatatan.isNullOrBlank()) {
                    val cleanCatatan = LedgerRepository.cleanCatatan(t.singleCatatan)
                    if (!cleanCatatan.isNullOrBlank()) {
                        Text(
                            cleanCatatan,
                            color = SukaOnSurfaceVariant,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Spacer(Modifier.width(10.dp))

            // Sisi Kanan: Delta & Saldo atau Tombol Detail
            Column(horizontalAlignment = Alignment.End) {
                if (isManual || t.wastePending) {
                    val deltaTeks = if (isDelivery && t.singleNamaBahan != null) {
                        DeliveryUnits.format(t.singleQty ?: 0.0, t.singleNamaBahan, false)
                            ?: UnitScale.formatQtyLedger(t.singleQty ?: 0.0, t.singleMeta, t.singleSaldoIsGram)
                    } else {
                        UnitScale.formatQtyLedger(t.singleQty ?: 0.0, t.singleMeta, t.singleSaldoIsGram)
                    }

                    Text(
                        deltaTeks,
                        color = if (t.wastePending) Color(0xFFD97706) else if (menambah) Color(0xFF168451) else Color(0xFFDC2626),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.End,
                    )

                    Spacer(Modifier.height(3.dp))

                    if (t.wastePending) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFFFFBEB),
                            border = BorderStroke(0.5.dp, Color(0xFFFDE68A)),
                        ) {
                            Text(
                                "Menunggu Verifikasi",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                color = Color(0xFFB45309),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    } else if (t.singleSaldoSesudah != null) {
                        val saldoTeks = if (isDelivery && t.singleNamaBahan != null) {
                            DeliveryUnits.format(t.singleSaldoSesudah, t.singleNamaBahan, true)
                                ?: UnitScale.formatSaldoLedger(t.singleSaldoSesudah, t.singleMeta, t.singleSaldoIsGram)
                        } else {
                            UnitScale.formatSaldoLedger(t.singleSaldoSesudah, t.singleMeta, t.singleSaldoIsGram)
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(0.5.dp, Color(0xFFE2E8F0)),
                        ) {
                            Text(
                                "Saldo: $saldoTeks",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                color = Color(0xFF64748B),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                } else {
                    // Kejadian Gabungan (Composite)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    ) {
                        Text(
                            "Detail ▾",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = Color(0xFF64748B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Konten BottomSheet Detail yang cerdas menyesuaikan jenis transaksi:
 * - Order: daftar menu pesanan porsi + breakdown bahan baku.
 * - Composite: grouped breakdown bahan + delta + sisa saldo.
 * - Single / Audit: audit lengkap log ledger (pelapor, foto waste, dsb.).
 */
@Composable
private fun DetailBottomSheetContent(
    t: LedgerTransaksi,
    memuat: Boolean,
    orderDetails: List<OrderItemRow>,
    auditDetail: LedgerAuditDetail?,
    detailRows: List<LedgerDetailRow>,
    onTutup: () -> Unit,
) {
    val label = LedgerRepository.transaksiLabel(t)
    val isDelivery = t.refShipmentId != null || t.singleTipe == "transfer_keluar" ||
        t.singleCatatan?.contains("KIRIM SJ", ignoreCase = true) == true

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .heightIn(max = 650.dp),
    ) {
        // Header Sheet
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    label.title,
                    color = SukaOnSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    waktuSingkat(t.createdAt),
                    color = SukaOnSurfaceVariant,
                    fontSize = 11.sp,
                )
            }
            IconButton(onClick = onTutup) {
                Icon(Icons.Default.Close, "Tutup", tint = Color(0xFF64748B))
            }
        }

        Spacer(Modifier.height(14.dp))

        if (memuat) {
            Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SukaOrange, modifier = Modifier.size(32.dp))
            }
            return
        }

        LazyColumn(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // A. Kasus Transaksi ORDER / PENJUALAN
            if (t.refOrderId != null) {
                item {
                    Text(
                        "RINCIAN PESANAN",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                    )
                    Spacer(Modifier.height(6.dp))
                }

                if (orderDetails.isEmpty()) {
                    item {
                        Text("Tidak ada data rincian pesanan.", color = SukaOnSurfaceVariant, fontSize = 12.sp)
                    }
                } else {
                    items(orderDetails, key = { it.id }) { item ->
                        Surface(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        ) {
                            Row(
                                Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    item.menuItemName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SukaOnSurface,
                                    modifier = Modifier.weight(1f),
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                ) {
                                    Text(
                                        "${item.quantity} Porsi",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF1E293B),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // B. Kasus Transaksi Tunggal (Audit Detail)
            else if (!t.gabungan && !t.wastePending && auditDetail != null) {
                item {
                    KartuAuditLengkap(audit = auditDetail, isDelivery = isDelivery)
                }
            }

            // C. Kasus Transaksi Gabungan / Opname / SJ / Transfer
            else {
                if (detailRows.isEmpty()) {
                    item {
                        Text(
                            t.singleCatatan ?: "Tidak ada rincian tambahan.",
                            color = SukaOnSurfaceVariant,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                        )
                    }
                } else {
                    // Grouping persis Web
                    val grouped = linkedMapOf<String, MutableList<LedgerDetailRow>>()
                    detailRows.forEach { r ->
                        var groupKey = "Detail Transaksi"
                        val catatan = r.catatan
                        if (!catatan.isNullOrBlank()) {
                            val match = Regex("""\((.*?)\)$""").find(catatan)
                            val extracted = match?.groupValues?.getOrNull(1)
                            groupKey = if (!extracted.isNullOrBlank()) {
                                LedgerRepository.cleanItemNames(extracted) ?: extracted
                            } else {
                                LedgerRepository.cleanCatatan(catatan) ?: catatan
                            }
                        }
                        grouped.getOrPut(groupKey) { mutableListOf() }.add(r)
                    }

                    val showGroupHeaders = grouped.size > 1 ||
                        (grouped.size == 1 && grouped.keys.first() != "Detail Transaksi" &&
                            !grouped.keys.first().startsWith("Penjualan Otomatis #"))

                    grouped.forEach { (grup, list) ->
                        if (showGroupHeaders) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.padding(top = 4.dp),
                                ) {
                                    Row(
                                        Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Icon(Icons.Default.FolderOpen, null, tint = Color(0xFF64748B), modifier = Modifier.size(13.dp))
                                        Text(
                                            grup.uppercase(Locale.ROOT),
                                            color = Color(0xFF475569),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                        )
                                    }
                                }
                            }
                        }

                        items(list, key = { it.id }) { row ->
                            BarisBahanBreakdown(row = row, isDelivery = isDelivery)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BarisBahanBreakdown(row: LedgerDetailRow, isDelivery: Boolean) {
    val menambah = row.qty >= 0
    val warna = if (menambah) Color(0xFF168451) else Color(0xFFDC2626)

    val deltaTeks = if (isDelivery && row.namaBahan != null) {
        DeliveryUnits.format(row.qty, row.namaBahan, false)
            ?: UnitScale.formatQtyLedger(row.qty, row.meta, row.saldoIsGram)
    } else {
        UnitScale.formatQtyLedger(row.qty, row.meta, row.saldoIsGram)
    }

    val sisaTeks = if (row.saldoSesudah != null) {
        if (isDelivery && row.namaBahan != null) {
            DeliveryUnits.format(row.saldoSesudah, row.namaBahan, true)
                ?: UnitScale.formatSaldoLedger(row.saldoSesudah, row.meta, row.saldoIsGram)
        } else {
            UnitScale.formatSaldoLedger(row.saldoSesudah, row.meta, row.saldoIsGram)
        }
    } else null

    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            row.namaBahan ?: "(bahan tidak dikenal)",
            color = SukaOnSurface,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                deltaTeks,
                color = warna,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            if (sisaTeks != null) {
                Text(
                    " → sisa $sisaTeks",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun KartuAuditLengkap(audit: LedgerAuditDetail, isDelivery: Boolean) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Log ID & Tipe
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("ID LOG", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF94A3B8))
                    Text(audit.id, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF64748B))
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFF1F5F9),
                    border = BorderStroke(0.5.dp, Color(0xFFCBD5E1)),
                ) {
                    Text(
                        audit.tipe.uppercase(Locale.ROOT),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF334155),
                    )
                }
            }

            Spacer(Modifier.height(2.dp))

            // Waktu Transaksi
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Waktu Transaksi", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                Text(waktuSingkat(audit.createdAt), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SukaOnSurface)
            }

            // Dibuat Oleh / Pelapor
            if (!audit.wasteReporterName.isNullOrBlank()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Dilaporkan Oleh", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                    Text(audit.wasteReporterName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SukaOnSurface)
                }
            }
            if (!audit.wasteApproverName.isNullOrBlank()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Disetujui Oleh", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                    Text(audit.wasteApproverName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SukaOnSurface)
                }
            }
            if (audit.wasteReporterName.isNullOrBlank() && !audit.genericCreatorName.isNullOrBlank()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Dibuat Oleh", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                    Text(audit.genericCreatorName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SukaOnSurface)
                }
            }

            // Bahan Baku
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Bahan Baku", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                Text(
                    audit.namaBahan ?: "-",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A),
                )
            }

            // Perubahan Stok
            val deltaTeks = if (isDelivery && audit.namaBahan != null) {
                DeliveryUnits.format(audit.qty, audit.namaBahan, false)
                    ?: UnitScale.formatQtyLedger(audit.qty, audit.meta, audit.saldoIsGram)
            } else {
                UnitScale.formatQtyLedger(audit.qty, audit.meta, audit.saldoIsGram)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Jumlah Perubahan", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                Text(
                    deltaTeks,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (audit.qty >= 0) Color(0xFF168451) else Color(0xFFDC2626),
                )
            }

            // Mutasi Saldo
            if (audit.saldoSebelum != null && audit.saldoSesudah != null) {
                val sblm = if (isDelivery && audit.namaBahan != null) {
                    DeliveryUnits.format(audit.saldoSebelum, audit.namaBahan, true)
                        ?: UnitScale.formatSaldoLedger(audit.saldoSebelum, audit.meta, audit.saldoIsGram)
                } else {
                    UnitScale.formatSaldoLedger(audit.saldoSebelum, audit.meta, audit.saldoIsGram)
                }
                val sesudah = if (isDelivery && audit.namaBahan != null) {
                    DeliveryUnits.format(audit.saldoSesudah, audit.namaBahan, true)
                        ?: UnitScale.formatSaldoLedger(audit.saldoSesudah, audit.meta, audit.saldoIsGram)
                } else {
                    UnitScale.formatSaldoLedger(audit.saldoSesudah, audit.meta, audit.saldoIsGram)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Mutasi Saldo", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                    Text("$sblm → $sesudah", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SukaOnSurface)
                }
            }

            // Catatan
            if (!audit.catatan.isNullOrBlank()) {
                val bersih = LedgerRepository.cleanCatatan(audit.catatan)
                if (!bersih.isNullOrBlank()) {
                    Column(Modifier.fillMaxWidth()) {
                        Text("Catatan", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(2.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        ) {
                            Text(
                                bersih,
                                modifier = Modifier.padding(8.dp),
                                fontSize = 11.sp,
                                color = SukaOnSurface,
                            )
                        }
                    }
                }
            }

            // Foto Lampiran Waste (jika ada)
            if (!audit.wastePhotoUrl.isNullOrBlank()) {
                Column(Modifier.fillMaxWidth()) {
                    Text("Foto Lampiran Bukti", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF1F5F9)),
                        contentAlignment = Alignment.Center,
                    ) {
                        AsyncImage(
                            model = audit.wastePhotoUrl,
                            contentDescription = "Bukti Waste",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Glosarium Satuan Bahan Baku — cermin `GlosariumSatuanModal.tsx` di web.
 */
@Composable
private fun GlosariumSatuanSheet(
    daftarBahan: List<BahanBaku>,
    memuat: Boolean,
    onTutup: () -> Unit,
) {
    var cari by remember { mutableStateOf("") }
    val terfilter = remember(daftarBahan, cari) {
        val q = cari.trim().lowercase(Locale.ROOT)
        daftarBahan.filter { b ->
            q.isEmpty() || b.nama.lowercase(Locale.ROOT).contains(q)
        }.sortedBy { it.nama }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .heightIn(max = 600.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.MenuBook, null, tint = SukaOrange, modifier = Modifier.size(22.dp))
                Column {
                    Text(
                        "Glosarium Satuan Bahan Baku",
                        color = Color(0xFF0F172A),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        "Konversi satuan besar → kecil, biar tidak salah hitung.",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp,
                    )
                }
            }
            IconButton(onClick = onTutup) {
                Icon(Icons.Default.Close, "Tutup", tint = Color(0xFF64748B))
            }
        }

        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = cari,
            onValueChange = { cari = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Cari nama bahan…", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp)) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = SukaOrange,
                unfocusedBorderColor = Color(0xFFE2E8F0),
            ),
        )

        Spacer(Modifier.height(10.dp))

        if (memuat) {
            Box(Modifier.fillMaxWidth().padding(vertical = 30.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SukaOrange, modifier = Modifier.size(28.dp))
            }
        } else if (terfilter.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(vertical = 30.dp), contentAlignment = Alignment.Center) {
                Text("Tidak ada bahan yang cocok.", color = Color(0xFF94A3B8), fontSize = 12.sp)
            }
        } else {
            LazyColumn(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(terfilter, key = { it.id }) { b ->
                    Surface(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFFF8F1),
                        border = BorderStroke(1.dp, Color(0xFFFED7AA).copy(alpha = 0.5f)),
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                b.nama.uppercase(Locale.ROOT),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E1B15),
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                formatKonversiBahan(b),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFEA580C),
                                textAlign = TextAlign.End,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatKonversiBahan(b: BahanBaku): String {
    val parts = mutableListOf("1 ${b.satuan ?: ""}".trim())
    if (!b.satuanTengah.isNullOrBlank() && b.faktorTengah != null && b.faktorTengah > 0) {
        val fTengahStr = if (b.faktorTengah % 1.0 == 0.0) b.faktorTengah.toLong().toString() else b.faktorTengah.toString()
        parts.add("$fTengahStr ${b.satuanTengah}")
    }
    if (!b.satuanKecil.isNullOrBlank() && b.faktorTampilan != null && b.faktorTampilan > 0) {
        val fTampilanStr = if (b.faktorTampilan % 1.0 == 0.0) b.faktorTampilan.toLong().toString() else b.faktorTampilan.toString()
        parts.add("$fTampilanStr ${b.satuanKecil}")
    }
    if (parts.size == 1) return "1 ${b.satuan ?: ""} (tanpa pecahan satuan)"
    return parts.joinToString(" = ")
}
