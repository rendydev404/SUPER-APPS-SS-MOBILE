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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.feature.stok.data.BudgetOutlet
import com.sukashawarma.superapp.feature.stok.data.BudgetOutletRepository
import com.sukashawarma.superapp.feature.stok.data.model.Permintaan
import com.sukashawarma.superapp.feature.stok.data.model.PermintaanItem
import com.sukashawarma.superapp.feature.stok.domain.Budget
import com.sukashawarma.superapp.feature.stok.domain.BudgetVarian
import com.sukashawarma.superapp.feature.stok.domain.formatRupiah
import com.sukashawarma.superapp.feature.stok.domain.stokErrorMessage
import com.sukashawarma.superapp.feature.stok.ui.HeaderStok
import com.sukashawarma.superapp.feature.stok.ui.waktuSingkat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.RealtimeTables

private val ORANGE = Color(0xFFEA580C)
private val ORANGE_SOFT = Color(0xFFFFF7ED)
private val SLATE400 = Color(0xFF94A3B8)
private val SLATE500 = Color(0xFF64748B)
private val SLATE700 = Color(0xFF334155)
private val SLATE900 = Color(0xFF0F172A)
private val GARIS = Color(0xFFE2E8F0)
private val HIJAU = Color(0xFF15803D)
private val HIJAU_SOFT = Color(0xFFDCFCE7)
private val MERAH = Color(0xFFB91C1C)
private val AMBER = Color(0xFFB45309)

data class PlafonUiState(
    val outlets: List<BudgetOutlet> = emptyList(),
    val memuat: Boolean = true,
    val error: String? = null,
    val selectedOutletId: String? = null,
    val memuatRiwayat: Boolean = false,
    val riwayatBelanja: List<Permintaan> = emptyList(),
    val errorRiwayat: String? = null,
    val rincianPermintaan: Permintaan? = null,
) {
    val totalPlafon: Double get() = outlets.sumOf { it.nominal }
    val totalTerpakai: Double get() = outlets.sumOf { it.terpakai }
    val jumlahLewat: Int get() = outlets.count { it.hasConfig && it.terpakai > it.nominal }
    val belumDikonfigurasi: Int get() = outlets.count { !it.hasConfig }
    val selectedOutlet: BudgetOutlet? get() = outlets.firstOrNull { it.outletId == selectedOutletId }
    val totalNilaiRiwayat: Double get() = riwayatBelanja.sumOf { p ->
        p.items.sumOf { (it.qtyDisetujui ?: it.qtyDiminta) * (it.hargaSnapshot ?: 0.0) }
    }
}

class PlafonBelanjaViewModel : ViewModel() {
    private val _state = MutableStateFlow(PlafonUiState())
    val state: StateFlow<PlafonUiState> = _state

    init { muatUlang() }

    fun muatUlang() {
        viewModelScope.launch {
            _state.value = _state.value.copy(memuat = true, error = null)
            try {
                val daftar = BudgetOutletRepository.daftar()
                val selected = _state.value.selectedOutletId?.takeIf { id -> daftar.any { it.outletId == id } }
                    ?: daftar.firstOrNull()?.outletId
                _state.value = _state.value.copy(
                    memuat = false,
                    outlets = daftar,
                    selectedOutletId = selected,
                )
                if (selected != null) {
                    muatRiwayatBelanja(selected)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("PlafonBelanjaVM", "muatUlang() gagal", e)
                _state.value = _state.value.copy(memuat = false, error = stokErrorMessage(e))
            }
        }
    }

    fun pilihOutlet(outletId: String) {
        if (_state.value.selectedOutletId == outletId) return
        _state.value = _state.value.copy(selectedOutletId = outletId)
        muatRiwayatBelanja(outletId)
    }

    fun bukaRincian(permintaan: Permintaan) {
        _state.value = _state.value.copy(rincianPermintaan = permintaan)
    }

    fun tutupRincian() {
        _state.value = _state.value.copy(rincianPermintaan = null)
    }

    private fun muatRiwayatBelanja(outletId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(memuatRiwayat = true, errorRiwayat = null)
            try {
                val riwayat = BudgetOutletRepository.riwayatBelanja(outletId)
                _state.value = _state.value.copy(memuatRiwayat = false, riwayatBelanja = riwayat)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("PlafonBelanjaVM", "muatRiwayatBelanja($outletId) gagal", e)
                _state.value = _state.value.copy(memuatRiwayat = false, errorRiwayat = stokErrorMessage(e))
            }
        }
    }
}

/**
 * Plafon & Belanja Outlet — cermin `BudgetOutletTabContent.tsx` & `OutletSpendingHistory.tsx` web.
 *
 * Menampilkan ringkasan batas plafon anggaran, persentase pemakaian, dan riwayat
 * pengadaan/belanja bahan baku yang telah disetujui per outlet binaan.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlafonBelanjaScreen(
    onBack: () -> Unit,
    viewModel: PlafonBelanjaViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()

    // Angka terpakai & sisa dihitung dari permintaan bahan pada periode berjalan
    // (lihat `get_outlet_budget_status_scoped`), jadi setiap permintaan baru
    // menggesernya. Perubahan plafon dan topup belum bisa diikuti — tabelnya
    // belum masuk publication realtime.
    RealtimeRefresh(RealtimeTables.PERMINTAAN) { viewModel.muatUlang() }

    Column(Modifier.fillMaxSize().background(Color(0xFFF8FAFC))) {
        HeaderStok(
            judul = "Plafon & Belanja Outlet",
            subjudul = when {
                state.memuat -> "Memuat…"
                state.jumlahLewat > 0 -> "${state.outlets.size} outlet · ${state.jumlahLewat} melewati plafon"
                else -> "${state.outlets.size} outlet binaan"
            },
            onKembali = onBack,
            aksi = {
                IconButton(onClick = viewModel::muatUlang) {
                    Icon(Icons.Default.Refresh, "Muat ulang", tint = Color(0xFF1E293B))
                }
            },
        )

        when {
            state.memuat -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ORANGE)
            }
            state.error != null -> PesanKosongLaporan(state.error!!)
            state.outlets.isEmpty() -> PesanKosongLaporan("Tidak ada outlet dalam cakupan Anda.")
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp),
            ) {
                // 1. Ringkasan Plafon Global
                item { RingkasPlafon(state) }

                // 2. Judul Seksi Outlet Cards
                item {
                    Text(
                        "STATUS PLAFON OUTLET",
                        color = SLATE500,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(top = 4.dp, start = 2.dp),
                    )
                }

                // 3. Kartu-kartu Outlet
                items(state.outlets, key = { it.outletId }) { outlet ->
                    KartuPlafon(
                        outlet = outlet,
                        terpilih = outlet.outletId == state.selectedOutletId,
                        onKlik = { viewModel.pilihOutlet(outlet.outletId) },
                    )
                }

                // 4. Seksi Riwayat Belanja untuk Outlet Terpilih
                item {
                    SeksiRiwayatBelanja(
                        selectedOutlet = state.selectedOutlet,
                        riwayat = state.riwayatBelanja,
                        memuat = state.memuatRiwayat,
                        error = state.errorRiwayat,
                        totalNilai = state.totalNilaiRiwayat,
                        onLihatDetail = { viewModel.bukaRincian(it) },
                    )
                }
            }
        }
    }

    // Modal Bottom Sheet Rincian Item Transaksi Belanja
    if (state.rincianPermintaan != null) {
        RincianBelanjaSheet(
            permintaan = state.rincianPermintaan!!,
            onDismiss = viewModel::tutupRincian,
        )
    }
}

@Composable
private fun RingkasPlafon(state: PlafonUiState) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = SLATE900) {
        Column(Modifier.padding(18.dp)) {
            Text(
                "TOTAL BELANJA PERIODE BERJALAN",
                color = Color(0xFF94A3B8),
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                formatRupiah(state.totalTerpakai),
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                "dari plafon ${formatRupiah(state.totalPlafon)}",
                color = Color(0xFFCBD5E1),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
            )
            if (state.belumDikonfigurasi > 0) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "${state.belumDikonfigurasi} outlet belum punya plafon, jadi belanjanya tidak ikut dibatasi.",
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
private fun KartuPlafon(
    outlet: BudgetOutlet,
    terpilih: Boolean,
    onKlik: () -> Unit,
) {
    val varian = Budget.varian(outlet.hasConfig, outlet.nominal, outlet.terpakai)
    val warna = when (varian) {
        BudgetVarian.MERAH -> MERAH
        BudgetVarian.ORANYE -> AMBER
        BudgetVarian.HIJAU -> HIJAU
        BudgetVarian.TERSEMBUNYI -> SLATE400
    }
    Surface(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onKlik),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(if (terpilih) 2.dp else 1.dp, if (terpilih) ORANGE else GARIS),
        shadowElevation = if (terpilih) 2.dp else 0.dp,
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            outlet.outletName,
                            color = SLATE900,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (terpilih) {
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = ORANGE_SOFT,
                                border = BorderStroke(0.5.dp, ORANGE.copy(alpha = 0.4f)),
                            ) {
                                Text(
                                    "Riwayat Aktif",
                                    color = ORANGE,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (outlet.hasConfig) {
                            "Plafon ${Budget.labelPeriode(outlet.periodType, outlet.customDays).lowercase()}: ${formatRupiah(outlet.nominal)}"
                        } else {
                            "Belum ada plafon"
                        },
                        color = SLATE400,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (outlet.hasConfig) {
                    Text(
                        "${outlet.persen.toInt()}%",
                        color = warna,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }

            if (outlet.hasConfig) {
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFF1F5F9)),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth((outlet.persen / 100.0).coerceIn(0.0, 1.0).toFloat())
                            .height(7.dp)
                            .clip(RoundedCornerShape(50))
                            .background(warna),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row {
                    Text(
                        "Terpakai ${formatRupiah(outlet.terpakai)}",
                        Modifier.weight(1f),
                        color = SLATE500,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        if (outlet.sisa < 0) "Lewat ${formatRupiah(-outlet.sisa)}"
                        else "Sisa ${formatRupiah(outlet.sisa)}",
                        color = if (outlet.sisa < 0) MERAH else SLATE500,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFF8FAFC))
            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (terpilih) "Sedang melihat riwayat belanja" else "Ketuk untuk melihat riwayat belanja",
                    color = if (terpilih) ORANGE else SLATE400,
                    fontSize = 10.5.sp,
                    fontWeight = if (terpilih) FontWeight.Bold else FontWeight.Medium,
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = if (terpilih) ORANGE else SLATE400,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun SeksiRiwayatBelanja(
    selectedOutlet: BudgetOutlet?,
    riwayat: List<Permintaan>,
    memuat: Boolean,
    error: String?,
    totalNilai: Double,
    onLihatDetail: (Permintaan) -> Unit,
) {
    if (selectedOutlet == null) return

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, GARIS),
    ) {
        Column(Modifier.padding(16.dp)) {
            // Header Riwayat
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(ORANGE_SOFT, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = ORANGE,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Riwayat Belanja Outlet",
                        color = SLATE900,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = selectedOutlet.outletName,
                        color = ORANGE,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Box Statistik Ringkas Belanja Outlet
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, GARIS),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "TOTAL BELANJA DISETUJUI",
                            color = SLATE500,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp,
                        )
                        Text(
                            text = formatRupiah(totalNilai),
                            color = SLATE900,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "TRANSAKSI",
                            color = SLATE500,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp,
                        )
                        Text(
                            text = "${riwayat.size} kali",
                            color = ORANGE,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            when {
                memuat -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            color = ORANGE,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp,
                        )
                    }
                }
                error != null -> {
                    Text(
                        text = error,
                        color = MERAH,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                }
                riwayat.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = null,
                            tint = SLATE400,
                            modifier = Modifier.size(32.dp),
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Belum ada transaksi belanja disetujui",
                            color = SLATE700,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Permintaan bahan baku yang disetujui akan tercatat di sini.",
                            color = SLATE400,
                            fontSize = 10.5.sp,
                        )
                    }
                }
                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        for (permintaan in riwayat) {
                            KartuTransaksiBelanja(
                                permintaan = permintaan,
                                onLihatDetail = { onLihatDetail(permintaan) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KartuTransaksiBelanja(
    permintaan: Permintaan,
    onLihatDetail: () -> Unit,
) {
    val totalNilai = permintaan.items.sumOf { (it.qtyDisetujui ?: it.qtyDiminta) * (it.hargaSnapshot ?: 0.0) }
    val kode = "#REQ-${permintaan.id.take(6).uppercase()}"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onLihatDetail),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, GARIS),
    ) {
        Column(Modifier.padding(12.dp)) {
            // Baris atas: Kode Permintaan & Badge Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = SLATE500,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = kode,
                        color = SLATE900,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = HIJAU_SOFT,
                ) {
                    Text(
                        text = "Disetujui",
                        color = HIJAU,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Baris tengah: Pemohon & Waktu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = SLATE400,
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = permintaan.pembuatNama ?: "Staff Outlet",
                        color = SLATE700,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    text = waktuSingkat(permintaan.createdAt),
                    color = SLATE400,
                    fontSize = 10.5.sp,
                )
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(Modifier.height(8.dp))

            // Baris bawah: Jumlah Item, Total Nilai, Tombol Rincian
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "${permintaan.items.size} jenis bahan",
                        color = SLATE500,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = formatRupiah(totalNilai),
                        color = SLATE900,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Black,
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ORANGE_SOFT,
                    border = BorderStroke(0.5.dp, ORANGE.copy(alpha = 0.3f)),
                    modifier = Modifier.clickable(onClick = onLihatDetail),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = ORANGE,
                            modifier = Modifier.size(12.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Rincian",
                            color = ORANGE,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Modal Bottom Sheet Rincian Transaksi Belanja — cermin `SpendingDetailModal.tsx` web.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RincianBelanjaSheet(
    permintaan: Permintaan,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val totalNilai = permintaan.items.sumOf { (it.qtyDisetujui ?: it.qtyDiminta) * (it.hargaSnapshot ?: 0.0) }
    val kode = "#REQ-${permintaan.id.take(6).uppercase()}"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp),
        ) {
            // Header Modal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Rincian Belanja Bahan",
                        color = SLATE900,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = "$kode · ${permintaan.outletName ?: "Outlet"}",
                        color = ORANGE,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFFF1F5F9), CircleShape),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = SLATE700,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Metadata Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, GARIS),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text("PEMOHON", color = SLATE400, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(permintaan.pembuatNama ?: "Staff Outlet", color = SLATE900, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("TANGGAL", color = SLATE400, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(waktuSingkat(permintaan.createdAt), color = SLATE900, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Text(
                "DAFTAR BAHAN BAKU",
                color = SLATE500,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.6.sp,
            )

            Spacer(Modifier.height(8.dp))

            // Daftar Item Bahan
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(permintaan.items) { item ->
                    BarisItemBelanja(item)
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = GARIS)
            Spacer(Modifier.height(12.dp))

            // Total Nilai Belanja
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Total Realisasi Belanja:",
                    color = SLATE700,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = formatRupiah(totalNilai),
                    color = ORANGE,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                )
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun BarisItemBelanja(item: PermintaanItem) {
    val qtyDisetujui = item.qtyDisetujui ?: item.qtyDiminta
    val harga = item.hargaSnapshot ?: 0.0
    val subtotal = qtyDisetujui * harga

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = item.namaBahan ?: "(Tanpa Nama)",
                    color = SLATE900,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$qtyDisetujui ${item.satuan ?: ""}",
                        color = SLATE500,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (harga > 0) {
                        Text(
                            text = " · @ ${formatRupiah(harga)}",
                            color = SLATE400,
                            fontSize = 10.5.sp,
                        )
                    }
                }
            }

            Text(
                text = formatRupiah(subtotal),
                color = SLATE900,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
