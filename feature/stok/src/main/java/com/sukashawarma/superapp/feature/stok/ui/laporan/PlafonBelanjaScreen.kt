package com.sukashawarma.superapp.feature.stok.ui.laporan

import com.sukashawarma.superapp.core.ui.ios.BarisIos
import com.sukashawarma.superapp.core.ui.ios.GrupIos
import com.sukashawarma.superapp.core.ui.ios.JudulSeksiIos
import com.sukashawarma.superapp.core.ui.ios.KartuIos
import com.sukashawarma.superapp.core.ui.ios.LabelSeksiIos
import com.sukashawarma.superapp.core.ui.ios.LencanaIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.PemisahIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.TombolBundarIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.ios.permukaanIos
import com.sukashawarma.superapp.core.ui.ios.tekanIos
import com.sukashawarma.superapp.feature.stok.ui.BannerIos
import com.sukashawarma.superapp.feature.stok.ui.MemuatPenuh
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.lazy.itemsIndexed
import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.core.ui.kaca.denganRuangNav
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
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

    Column(Modifier.fillMaxSize().background(WarnaIos.Latar)) {
        HeaderStok(
            judul = "Plafon & Belanja Outlet",
            subjudul = when {
                state.memuat -> "Memuat…"
                state.jumlahLewat > 0 -> "${state.outlets.size} outlet · ${state.jumlahLewat} melewati plafon"
                else -> "${state.outlets.size} outlet binaan"
            },
            onKembali = onBack,
            aksi = {
                TombolBundarIos(IkonIos.Refresh, "Muat ulang", viewModel::muatUlang)
            },
        )

        when {
            state.memuat -> MemuatPenuh()
            state.error != null -> PesanKosongLaporan(state.error!!)
            state.outlets.isEmpty() -> PesanKosongLaporan("Tidak ada outlet dalam cakupan Anda.")
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = UkuranIos.TepiLayar, vertical = 12.dp).denganRuangNav(),
                verticalArrangement = Arrangement.spacedBy(UkuranIos.JarakKartu),
            ) {
                // 1. Ringkasan Plafon Global
                item { RingkasPlafon(state) }

                // 2. Judul Seksi Outlet Cards
                item {
                    JudulSeksiIos("Status plafon outlet", keterangan = "${state.outlets.size} outlet")
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
    KartuIos {
        Text("Total belanja periode berjalan", style = TipeIos.SubJudul)
        Spacer(Modifier.height(4.dp))
        Text(formatRupiah(state.totalTerpakai), style = TipeIos.JudulBesar.copy(fontSize = 30.sp))
        Text("dari plafon ${formatRupiah(state.totalPlafon)}", style = TipeIos.Catatan)
        if (state.belumDikonfigurasi > 0) {
            Spacer(Modifier.height(10.dp))
            BannerIos(
                "${state.belumDikonfigurasi} outlet belum punya plafon, jadi belanjanya tidak ikut dibatasi.",
                NadaIos.PERINGATAN,
                ikon = IkonIos.WarningAmber,
            )
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
    val nada = when (varian) {
        BudgetVarian.MERAH -> NadaIos.BAHAYA
        BudgetVarian.ORANYE -> NadaIos.PERINGATAN
        BudgetVarian.HIJAU -> NadaIos.SUKSES
        BudgetVarian.TERSEMBUNYI -> NadaIos.NETRAL
    }
    KartuIos(
        // Kartu terpilih ditandai isian aksen tipis, bukan bingkai tebal.
        latar = if (terpilih) Color(0xFFFFF4EC) else WarnaIos.Kartu,
        onKlik = onKlik,
        padding = PaddingValues(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        outlet.outletName,
                        Modifier.weight(1f, fill = false),
                        style = TipeIos.Utama,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (terpilih) {
                        Spacer(Modifier.width(6.dp))
                        LencanaIos("Riwayat Aktif", NadaIos.AKSEN, titik = false)
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    if (outlet.hasConfig) {
                        "Plafon ${Budget.labelPeriode(outlet.periodType, outlet.customDays).lowercase()}: ${formatRupiah(outlet.nominal)}"
                    } else {
                        "Belum ada plafon"
                    },
                    style = TipeIos.Catatan,
                )
            }
            if (outlet.hasConfig) {
                Spacer(Modifier.width(8.dp))
                Text("${outlet.persen.toInt()}%", style = TipeIos.Angka.copy(color = nada.teks))
            }
        }

        if (outlet.hasConfig) {
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(UkuranIos.SudutKapsul)
                    .background(WarnaIos.Isian),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth((outlet.persen / 100.0).coerceIn(0.0, 1.0).toFloat())
                        .height(6.dp)
                        .clip(UkuranIos.SudutKapsul)
                        .background(nada.warna),
                )
            }
            Spacer(Modifier.height(8.dp))
            Row {
                Text("Terpakai ${formatRupiah(outlet.terpakai)}", Modifier.weight(1f), style = TipeIos.Catatan)
                Text(
                    if (outlet.sisa < 0) "Lewat ${formatRupiah(-outlet.sisa)}"
                    else "Sisa ${formatRupiah(outlet.sisa)}",
                    color = if (outlet.sisa < 0) NadaIos.BAHAYA.teks else WarnaIos.LabelKedua,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(WarnaIos.Pemisah))
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (terpilih) "Sedang melihat riwayat belanja" else "Ketuk untuk melihat riwayat belanja",
                style = TipeIos.Catatan.copy(
                    color = if (terpilih) NadaIos.AKSEN.teks else WarnaIos.LabelKedua,
                    fontWeight = if (terpilih) FontWeight.SemiBold else FontWeight.Normal,
                ),
            )
            Icon(
                imageVector = IkonIos.ChevronRight,
                contentDescription = null,
                tint = if (terpilih) WarnaIos.Aksen else WarnaIos.LabelKetiga,
                modifier = Modifier.size(15.dp),
            )
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

    KartuIos {
        // Header Riwayat
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(WarnaIos.Aksen.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = IkonIos.History,
                    contentDescription = null,
                    tint = WarnaIos.Aksen,
                    modifier = Modifier.size(19.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(text = "Riwayat Belanja Outlet", style = TipeIos.Utama)
                Text(
                    text = selectedOutlet.outletName,
                    style = TipeIos.Catatan,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Statistik ringkas belanja outlet
        Row(
            Modifier.fillMaxWidth().clip(UkuranIos.SudutBlok).background(WarnaIos.Latar).padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(text = "Total belanja disetujui", style = TipeIos.Kecil.copy(fontWeight = FontWeight.Medium))
                Text(text = formatRupiah(totalNilai), style = TipeIos.Angka.copy(fontSize = 17.sp))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "Transaksi", style = TipeIos.Kecil.copy(fontWeight = FontWeight.Medium))
                Text(text = "${riwayat.size} kali", style = TipeIos.Angka.copy(fontSize = 17.sp, color = NadaIos.AKSEN.teks))
            }
        }

        Spacer(Modifier.height(12.dp))

        when {
            memuat -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        color = WarnaIos.Abu,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp,
                    )
                }
            }
            error != null -> {
                BannerIos(error, NadaIos.BAHAYA, ikon = IkonIos.ErrorOutline)
            }
            riwayat.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier.size(52.dp).clip(CircleShape).background(WarnaIos.Isian),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = IkonIos.ShoppingBag,
                            contentDescription = null,
                            tint = WarnaIos.Abu,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(text = "Belum ada transaksi belanja disetujui", style = TipeIos.Keterangan.copy(fontWeight = FontWeight.SemiBold))
                    Text(
                        text = "Permintaan bahan baku yang disetujui akan tercatat di sini.",
                        style = TipeIos.Catatan,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            else -> {
                Column(Modifier.fillMaxWidth().clip(UkuranIos.SudutBlok).background(WarnaIos.Latar)) {
                    riwayat.forEachIndexed { i, permintaan ->
                        if (i > 0) PemisahIos(inset = 12.dp)
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

@Composable
private fun KartuTransaksiBelanja(
    permintaan: Permintaan,
    onLihatDetail: () -> Unit,
) {
    val totalNilai = permintaan.items.sumOf { (it.qtyDisetujui ?: it.qtyDiminta) * (it.hargaSnapshot ?: 0.0) }
    val kode = "#REQ-${permintaan.id.take(6).uppercase()}"

    // Satu baris di dalam daftar abu bersekat — seluruh baris bisa diketuk.
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onLihatDetail)
            .padding(12.dp),
    ) {
        // Baris atas: Kode Permintaan & Badge Status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = IkonIos.ReceiptLong,
                    contentDescription = null,
                    tint = WarnaIos.LabelKedua,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(text = kode, style = TipeIos.Keterangan.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold))
            }
            LencanaIos("Disetujui", NadaIos.SUKSES)
        }

        Spacer(Modifier.height(6.dp))

        // Baris tengah: Pemohon & Waktu
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = IkonIos.Person,
                    contentDescription = null,
                    tint = WarnaIos.Abu,
                    modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(text = permintaan.pembuatNama ?: "Staff Outlet", style = TipeIos.Catatan.copy(color = WarnaIos.Label))
            }
            Text(text = waktuSingkat(permintaan.createdAt), style = TipeIos.Kecil)
        }

        Spacer(Modifier.height(8.dp))

        // Baris bawah: Jumlah Item, Total Nilai, Tombol Rincian
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(text = "${permintaan.items.size} jenis bahan", style = TipeIos.Kecil)
                Text(text = formatRupiah(totalNilai), color = WarnaIos.Label, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Row(
                modifier = Modifier
                    .clip(UkuranIos.SudutKapsul)
                    .background(WarnaIos.Aksen.copy(alpha = 0.12f))
                    .tekanIos(onLihatDetail)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = IkonIos.Visibility,
                    contentDescription = null,
                    tint = WarnaIos.Aksen,
                    modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(text = "Rincian", color = NadaIos.AKSEN.teks, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
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
        containerColor = WarnaIos.Latar,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = UkuranIos.TepiLayar, vertical = 10.dp),
        ) {
            // Header Modal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(text = "Rincian Belanja Bahan", style = TipeIos.Judul3.copy(fontWeight = FontWeight.Bold))
                    Text(text = "$kode · ${permintaan.outletName ?: "Outlet"}", style = TipeIos.Catatan)
                }
                TombolBundarIos(IkonIos.Close, "Tutup", onDismiss, warnaIkon = WarnaIos.LabelKedua)
            }

            Spacer(Modifier.height(14.dp))

            // Metadata
            GrupIos {
                BarisIos("Pemohon", nilai = permintaan.pembuatNama ?: "Staff Outlet")
                PemisahIos()
                BarisIos("Tanggal", nilai = waktuSingkat(permintaan.createdAt))
            }

            Spacer(Modifier.height(16.dp))
            LabelSeksiIos("Daftar bahan baku", Modifier.padding(start = 16.dp))
            Spacer(Modifier.height(7.dp))

            // Daftar Item Bahan
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .permukaanIos(UkuranIos.SudutGrup),
            ) {
                itemsIndexed(permintaan.items) { i, item ->
                    if (i > 0) PemisahIos()
                    BarisItemBelanja(item)
                }
            }

            Spacer(Modifier.height(14.dp))

            // Total Nilai Belanja
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .permukaanIos(UkuranIos.SudutGrup)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "Total Realisasi Belanja:", style = TipeIos.Keterangan.copy(fontWeight = FontWeight.SemiBold))
                Text(text = formatRupiah(totalNilai), style = TipeIos.Angka.copy(color = NadaIos.AKSEN.teks))
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

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = item.namaBahan ?: "(Tanpa Nama)", style = TipeIos.Keterangan.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold))
            Spacer(Modifier.height(1.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "$qtyDisetujui ${item.satuan ?: ""}", style = TipeIos.Catatan)
                if (harga > 0) {
                    Text(text = " · @ ${formatRupiah(harga)}", style = TipeIos.Catatan.copy(color = WarnaIos.Abu))
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(text = formatRupiah(subtotal), color = WarnaIos.Label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}
