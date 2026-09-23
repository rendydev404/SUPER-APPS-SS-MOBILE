package com.sukashawarma.superapp.feature.stok.ui.ledger

import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.core.ui.kaca.denganRuangNav
import com.sukashawarma.superapp.core.ui.ios.KartuIos
import com.sukashawarma.superapp.core.ui.ios.KolomCariIos
import com.sukashawarma.superapp.core.ui.ios.LabelSeksiIos
import com.sukashawarma.superapp.core.ui.ios.LencanaIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.PemisahIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.TombolBundarIos
import com.sukashawarma.superapp.core.ui.ios.TombolKapsulIos
import com.sukashawarma.superapp.core.ui.ios.TombolUtamaIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.ios.permukaanIos
import com.sukashawarma.superapp.core.ui.ios.tekanIos
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.draw.clipToBounds
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * 6 Kategori filter pada ledger stok — cermin `FILTER_LABELS` di web.
 */
enum class KategoriLedger(val label: String, val icon: ImageVector) {
    SEMUA("Semua", IkonIos.Inventory2),
    MASUK("Masuk", IkonIos.ArrowDownward),
    ORDER("Order", IkonIos.Receipt),
    WASTE("Waste", IkonIos.Delete),
    KELUAR("Keluar", IkonIos.ArrowUpward),
    PENYESUAIAN("Penyesuaian", IkonIos.Scale);
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

    fun muatAwal(): Job {
        return viewModelScope.launch {
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

    fun segarkan(): Job {
        StokRepository.invalidate()
        return muatAwal()
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
    // Tutup indikator setelah Job refresh benar-benar selesai. Dulu memakai
    // LaunchedEffect(state.memuat), tapi StateFlow bisa melewatkan nilai true
    // (conflation) sehingga endRefresh tak pernah terpanggil dan indikator nyangkut.
    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            try {
                viewModel.segarkan().join()
            } finally {
                pullRefreshState.endRefresh()
            }
        }
    }

    Column(Modifier.fillMaxSize().background(WarnaIos.Latar)) {
        HeaderStok(judul = "Ledger Stok", subjudul = "Buku Kas & Riwayat Mutasi Bahan") {
            TombolBundarIos(IkonIos.Refresh, "Segarkan", viewModel::segarkan)
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
                        TombolUtamaIos(
                            "Buat Entri Manual",
                            onEntriManual,
                            Modifier.padding(horizontal = UkuranIos.TepiLayar, vertical = 8.dp),
                            ikon = IkonIos.Add,
                        )
                    }

                    // 2. Bar Pencarian
                    KolomCariIos(
                        state.kataKunci,
                        viewModel::ubahKataKunci,
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = UkuranIos.TepiLayar, vertical = 4.dp),
                        placeholder = "Cari nama bahan baku atau nomor order/opname…",
                    )

                    // 3. Filter Chips Row + Tombol Glosarium
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = UkuranIos.TepiLayar, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        KategoriLedger.entries.forEach { kat ->
                            ChipFilter(kat.label, kat.icon, kat == state.filterAktif) { viewModel.pilihFilter(kat) }
                        }

                        // Tombol Glosarium
                        TombolKapsulIos("Glosarium", viewModel::bukaGlosarium, ikon = IkonIos.MenuBook)
                    }

                    // 4. Daftar Transaksi (Pull-to-Refresh standar industri)
                    val terfilter = state.transaksiTerfilter
                    // clipToBounds wajib: PullToRefreshContainer M3 1.2 tetap menggambar
                    // lingkaran kartu di atas tepi Box saat diam; tanpa klip ia menutupi chip.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clipToBounds()
                            .nestedScroll(pullRefreshState.nestedScrollConnection)
                    ) {
                        if (terfilter.isEmpty() && !state.memuat) {
                            KeadaanKosong("Belum Ada Catatan Pergerakan")
                        } else {
                            LazyColumn(
                                Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    start = UkuranIos.TepiLayar, end = UkuranIos.TepiLayar, top = 4.dp, bottom = UkuranIos.TepiLayar,
                                ).denganRuangNav(),
                                verticalArrangement = Arrangement.spacedBy(UkuranIos.JarakKartu),
                            ) {
                                items(terfilter, key = { it.transaksiKey }) { t ->
                                    KartuTransaksi(t) { viewModel.bukaDetail(t) }
                                }
                                if (!state.habis && state.kataKunci.isEmpty() && state.filterAktif == KategoriLedger.SEMUA) {
                                    item(key = "lagi") {
                                        KartuIos(onKlik = { viewModel.muatLagi() }) {
                                            Text(
                                                if (state.memuatLagi) "Memuat…" else "Muat lebih banyak",
                                                Modifier.fillMaxWidth(),
                                                style = TipeIos.Isi.copy(color = WarnaIos.Aksen, fontWeight = FontWeight.SemiBold),
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
                            containerColor = WarnaIos.Kartu,
                            contentColor = WarnaIos.Aksen,
                        )
                    }
                }
            }
        }
    }

    // Modal BottomSheet Detail Transaksi
    val detailUntuk = state.detailUntuk
    if (detailUntuk != null) {
        ModalBottomSheet(
            onDismissRequest = viewModel::tutupDetail,
            sheetState = sheetState,
            containerColor = WarnaIos.Latar,
        ) {
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
        ModalBottomSheet(
            onDismissRequest = viewModel::tutupGlosarium,
            sheetState = glosariumSheetState,
            containerColor = WarnaIos.Latar,
        ) {
            GlosariumSatuanSheet(
                daftarBahan = state.daftarBahanGlosarium,
                memuat = state.memuatGlosarium,
                onTutup = viewModel::tutupGlosarium,
            )
        }
    }
}

/**
 * Chip filter kategori. Aktif terisi aksen; pasif berupa isian abu seperti
 * kontrol iOS lain — tanpa garis tepi dan tanpa huruf kapital.
 */
@Composable
private fun ChipFilter(teks: String, ikon: ImageVector, aktif: Boolean, onKlik: () -> Unit) {
    Row(
        Modifier
            .height(34.dp)
            .background(if (aktif) WarnaIos.Aksen else WarnaIos.Isian, UkuranIos.SudutKapsul)
            .tekanIos(onKlik)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(ikon, null, tint = if (aktif) Color.White else WarnaIos.LabelKedua, modifier = Modifier.size(14.dp))
        Text(
            teks,
            color = if (aktif) Color.White else WarnaIos.Label,
            fontSize = 14.sp,
            fontWeight = if (aktif) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

private fun contentModifier(desc: String): String = desc

data class VisualTransaksi(
    val icon: ImageVector,
    val iconColor: Color,
    val bgColor: Color,
    val borderColor: Color,
)

/** Visual per jenis transaksi. Latar selalu warna ikon tipis, seperti ikon baris iOS. */
private fun visual(icon: ImageVector, warna: Color) = VisualTransaksi(
    icon = icon,
    iconColor = warna,
    bgColor = warna.copy(alpha = 0.14f),
    borderColor = Color.Transparent,
)

private fun getTransaksiVisual(t: LedgerTransaksi): VisualTransaksi {
    if (t.refOrderId != null) {
        return visual(IkonIos.Receipt, WarnaIos.Biru)
    }
    if (t.refOpnameId != null) {
        return visual(IkonIos.Tune, WarnaIos.Oranye)
    }
    if (t.refShipmentId != null) {
        val isKirim = (t.singleQty ?: 0.0) < 0
        return visual(
            if (isKirim) IkonIos.ArrowUpward else IkonIos.ArrowDownward,
            if (isKirim) WarnaIos.Aksen else WarnaIos.Hijau,
        )
    }
    if (t.refTransferId != null) {
        return visual(IkonIos.SwapHoriz, WarnaIos.Aksen)
    }
    if (t.singleTipe == "terima_kiriman" || t.singleTipe == "transfer_masuk" || t.singleTipe == "pembelian_supplier") {
        return visual(IkonIos.ArrowDownward, WarnaIos.Hijau)
    }
    if (t.singleTipe == "waste" || t.singleTipe == "pemakaian") {
        return visual(IkonIos.Delete, WarnaIos.Merah)
    }
    if (t.singleTipe == "waste_pending") {
        return visual(IkonIos.HourglassEmpty, WarnaIos.Oranye)
    }
    if (t.singleTipe == "transfer_keluar") {
        return visual(IkonIos.ArrowUpward, WarnaIos.Aksen)
    }
    return visual(IkonIos.Scale, WarnaIos.Abu)
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

    KartuIos(onKlik = onKlik, padding = PaddingValues(horizontal = UkuranIos.PaddingKartu, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Ikon bundar bernada, bahasa ikon yang sama dengan baris iOS.
            Box(
                Modifier
                    .size(40.dp)
                    .background(visual.bgColor, CircleShape),
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
                    Text(
                        label.title,
                        style = TipeIos.Kecil.copy(color = visual.iconColor, fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Text(
                        "  ·  ${waktuRelatif(t.createdAt)}",
                        style = TipeIos.Kecil,
                        maxLines = 1,
                    )
                }

                Spacer(Modifier.height(2.dp))

                // Headline Nama Bahan / Menu Order / Keterangan
                val headline = if (isManual) {
                    t.singleNamaBahan ?: "Bahan Baku"
                } else {
                    label.subtitle ?: "${t.jumlahBahan} bahan"
                }

                Text(
                    headline,
                    style = TipeIos.Utama.copy(fontSize = 16.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                // Catatan jika manual
                if (isManual && !t.singleCatatan.isNullOrBlank()) {
                    val cleanCatatan = LedgerRepository.cleanCatatan(t.singleCatatan)
                    if (!cleanCatatan.isNullOrBlank()) {
                        Text(
                            cleanCatatan,
                            style = TipeIos.Catatan,
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
                        style = TipeIos.Keterangan.copy(
                            fontWeight = FontWeight.Bold,
                            color = when {
                                t.wastePending -> NadaIos.PERINGATAN.teks
                                menambah -> NadaIos.SUKSES.teks
                                else -> NadaIos.BAHAYA.teks
                            },
                        ),
                        textAlign = TextAlign.End,
                    )

                    Spacer(Modifier.height(4.dp))

                    if (t.wastePending) {
                        LencanaIos("Menunggu Verifikasi", NadaIos.PERINGATAN, titik = false)
                    } else if (t.singleSaldoSesudah != null) {
                        val saldoTeks = if (isDelivery && t.singleNamaBahan != null) {
                            DeliveryUnits.format(t.singleSaldoSesudah, t.singleNamaBahan, true)
                                ?: UnitScale.formatSaldoLedger(t.singleSaldoSesudah, t.singleMeta, t.singleSaldoIsGram)
                        } else {
                            UnitScale.formatSaldoLedger(t.singleSaldoSesudah, t.singleMeta, t.singleSaldoIsGram)
                        }
                        Text("Saldo: $saldoTeks", style = TipeIos.Kecil, textAlign = TextAlign.End)
                    }
                } else {
                    // Kejadian Gabungan (Composite)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Detail", style = TipeIos.Catatan)
                        Spacer(Modifier.width(2.dp))
                        Icon(IkonIos.ChevronRight, null, tint = WarnaIos.LabelKetiga, modifier = Modifier.size(15.dp))
                    }
                }
            }
        }
    }
}

/** Baris putih bersudut untuk isi lembar bawah — kartu tipis tanpa garis tepi. */
private fun Modifier.barisLembar(): Modifier =
    fillMaxWidth().background(WarnaIos.Kartu, UkuranIos.SudutBlok)

/** Kepala lembar bawah: judul, keterangan, tombol tutup bulat. */
@Composable
private fun KepalaLembar(judul: String, keterangan: String, onTutup: () -> Unit, ikon: ImageVector? = null) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (ikon != null) {
            Box(
                Modifier.size(36.dp).background(WarnaIos.Aksen.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(ikon, null, tint = WarnaIos.Aksen, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.width(10.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(judul, style = TipeIos.Judul3)
            Text(keterangan, style = TipeIos.Catatan)
        }
        Spacer(Modifier.width(8.dp))
        TombolBundarIos(IkonIos.Close, "Tutup", onTutup, warnaIkon = WarnaIos.LabelKedua)
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
            .padding(horizontal = UkuranIos.TepiLayar, vertical = 12.dp)
            .heightIn(max = 650.dp),
    ) {
        // Header Sheet
        KepalaLembar(label.title, waktuSingkat(t.createdAt), onTutup)

        Spacer(Modifier.height(14.dp))

        if (memuat) {
            Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = WarnaIos.Aksen, modifier = Modifier.size(32.dp))
            }
            return
        }

        LazyColumn(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // A. Kasus Transaksi ORDER / PENJUALAN
            if (t.refOrderId != null) {
                item {
                    LabelSeksiIos("Rincian Pesanan", Modifier.padding(start = 4.dp, bottom = 2.dp))
                }

                if (orderDetails.isEmpty()) {
                    item {
                        Text("Tidak ada data rincian pesanan.", style = TipeIos.SubJudul)
                    }
                } else {
                    items(orderDetails, key = { it.id }) { item ->
                        Row(
                            Modifier
                                .barisLembar()
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                item.menuItemName,
                                style = TipeIos.Keterangan.copy(fontWeight = FontWeight.SemiBold),
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(8.dp))
                            LencanaIos("${item.quantity} Porsi", NadaIos.AKSEN, titik = false)
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
                            style = TipeIos.SubJudul,
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
                                Row(
                                    Modifier.padding(start = 4.dp, top = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(IkonIos.FolderOpen, null, tint = WarnaIos.Abu, modifier = Modifier.size(14.dp))
                                    LabelSeksiIos(grup)
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
    val warna = if (menambah) NadaIos.SUKSES.teks else NadaIos.BAHAYA.teks

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
            .barisLembar()
            .padding(horizontal = 14.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            row.namaBahan ?: "(bahan tidak dikenal)",
            style = TipeIos.Keterangan.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                deltaTeks,
                style = TipeIos.Catatan.copy(color = warna, fontWeight = FontWeight.Bold),
            )
            if (sisaTeks != null) {
                Text(" → sisa $sisaTeks", style = TipeIos.Kecil)
            }
        }
    }
}

/** Satu baris label–nilai di kartu audit, dengan garis hairline di atasnya. */
@Composable
private fun BarisAudit(label: String, nilai: String, warna: Color = WarnaIos.Label, pemisah: Boolean = true) {
    if (pemisah) PemisahIos()
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = TipeIos.SubJudul)
        Spacer(Modifier.width(12.dp))
        Text(
            nilai,
            Modifier.weight(1f),
            style = TipeIos.SubJudul.copy(color = warna, fontWeight = FontWeight.SemiBold),
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun KartuAuditLengkap(audit: LedgerAuditDetail, isDelivery: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Rincian sebagai grup iOS: satu kartu, baris dipisah garis hairline.
        Column(Modifier.fillMaxWidth().permukaanIos(UkuranIos.SudutGrup)) {
            // Log ID & Tipe
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("ID Log", style = TipeIos.Kecil)
                    Text(
                        audit.id,
                        style = TipeIos.Kecil.copy(fontFamily = FontFamily.Monospace, color = WarnaIos.LabelKedua),
                    )
                }
                Spacer(Modifier.width(8.dp))
                LencanaIos(audit.tipe, NadaIos.NETRAL, titik = false)
            }

            // Waktu Transaksi
            BarisAudit("Waktu Transaksi", waktuSingkat(audit.createdAt))

            // Dibuat Oleh / Pelapor
            if (!audit.wasteReporterName.isNullOrBlank()) {
                BarisAudit("Dilaporkan Oleh", audit.wasteReporterName)
            }
            if (!audit.wasteApproverName.isNullOrBlank()) {
                BarisAudit("Disetujui Oleh", audit.wasteApproverName)
            }
            if (audit.wasteReporterName.isNullOrBlank() && !audit.genericCreatorName.isNullOrBlank()) {
                BarisAudit("Dibuat Oleh", audit.genericCreatorName)
            }

            // Bahan Baku
            BarisAudit("Bahan Baku", audit.namaBahan ?: "-")

            // Perubahan Stok
            val deltaTeks = if (isDelivery && audit.namaBahan != null) {
                DeliveryUnits.format(audit.qty, audit.namaBahan, false)
                    ?: UnitScale.formatQtyLedger(audit.qty, audit.meta, audit.saldoIsGram)
            } else {
                UnitScale.formatQtyLedger(audit.qty, audit.meta, audit.saldoIsGram)
            }
            BarisAudit(
                "Jumlah Perubahan",
                deltaTeks,
                if (audit.qty >= 0) NadaIos.SUKSES.teks else NadaIos.BAHAYA.teks,
            )

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
                BarisAudit("Mutasi Saldo", "$sblm → $sesudah")
            }
        }

        // Catatan
        if (!audit.catatan.isNullOrBlank()) {
            val bersih = LedgerRepository.cleanCatatan(audit.catatan)
            if (!bersih.isNullOrBlank()) {
                Column(Modifier.fillMaxWidth()) {
                    LabelSeksiIos("Catatan", Modifier.padding(start = 16.dp, bottom = 6.dp))
                    Text(
                        bersih,
                        modifier = Modifier
                            .fillMaxWidth()
                            .permukaanIos(UkuranIos.SudutGrup)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        style = TipeIos.SubJudul.copy(color = WarnaIos.Label),
                    )
                }
            }
        }

        // Foto Lampiran Waste (jika ada)
        if (!audit.wastePhotoUrl.isNullOrBlank()) {
            Column(Modifier.fillMaxWidth()) {
                LabelSeksiIos("Foto Lampiran Bukti", Modifier.padding(start = 16.dp, bottom = 6.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .permukaanIos(UkuranIos.SudutGrup, WarnaIos.Isian),
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
            .padding(horizontal = UkuranIos.TepiLayar, vertical = 12.dp)
            .heightIn(max = 600.dp),
    ) {
        KepalaLembar(
            "Glosarium Satuan Bahan Baku",
            "Konversi satuan besar → kecil, biar tidak salah hitung.",
            onTutup,
            ikon = IkonIos.MenuBook,
        )

        Spacer(Modifier.height(12.dp))

        KolomCariIos(cari, { cari = it }, Modifier.fillMaxWidth(), placeholder = "Cari nama bahan…")

        Spacer(Modifier.height(12.dp))

        if (memuat) {
            Box(Modifier.fillMaxWidth().padding(vertical = 30.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = WarnaIos.Aksen, modifier = Modifier.size(28.dp))
            }
        } else if (terfilter.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(vertical = 30.dp), contentAlignment = Alignment.Center) {
                Text("Tidak ada bahan yang cocok.", style = TipeIos.SubJudul)
            }
        } else {
            LazyColumn(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(terfilter, key = { it.id }) { b ->
                    Row(
                        Modifier
                            .barisLembar()
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            b.nama,
                            style = TipeIos.Keterangan.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            formatKonversiBahan(b),
                            style = TipeIos.Catatan.copy(color = NadaIos.AKSEN.teks, fontWeight = FontWeight.SemiBold),
                            textAlign = TextAlign.End,
                        )
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
