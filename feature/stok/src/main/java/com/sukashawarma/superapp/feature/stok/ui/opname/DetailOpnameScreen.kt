package com.sukashawarma.superapp.feature.stok.ui.opname

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.RealtimeTables
import com.sukashawarma.superapp.data.remote.optJsonObject
import com.sukashawarma.superapp.data.remote.optString
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.feature.stok.data.BahanAktifRingkas
import com.sukashawarma.superapp.feature.stok.data.OpnameRepository
import com.sukashawarma.superapp.feature.stok.data.model.OpnameItemDetail
import com.sukashawarma.superapp.feature.stok.data.model.StatusOpname
import com.sukashawarma.superapp.feature.stok.domain.Selisih
import com.sukashawarma.superapp.feature.stok.domain.StokAkses
import com.sukashawarma.superapp.feature.stok.domain.UnitMeta
import com.sukashawarma.superapp.feature.stok.domain.decomposeTriUnit
import com.sukashawarma.superapp.feature.stok.domain.formatAngkaStok
import com.sukashawarma.superapp.feature.stok.domain.parseCatatanOpname
import com.sukashawarma.superapp.feature.stok.domain.stokErrorMessage
import com.sukashawarma.superapp.feature.stok.ui.HeaderStok
import com.sukashawarma.superapp.feature.stok.ui.KeadaanGagal
import com.sukashawarma.superapp.feature.stok.ui.KeadaanKosong
import com.sukashawarma.superapp.feature.stok.ui.MemuatPenuh
import com.sukashawarma.superapp.presentation.theme.SukaOnSurface
import com.sukashawarma.superapp.presentation.theme.SukaOnSurfaceVariant
import com.sukashawarma.superapp.presentation.theme.SukaSurface
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

private val AMBER_BG = Color(0xFFFFFBEB)
private val AMBER_LINE = Color(0xFFFDE68A)
private val AMBER_TEKS = Color(0xFF92400E)
private val MERAH = Color(0xFFDC2626)
private val MERAH_BG = Color(0xFFFEF2F2)
private val MERAH_LINE = Color(0xFFFECACA)
private val HIJAU = Color(0xFF168451)
private val SLATE400 = Color(0xFF94A3B8)
private val SLATE500 = Color(0xFF64748B)
private val GARIS = Color(0xFFF1F5F9)

data class DetailOpnameUiState(
    val memuat: Boolean = true,
    val error: String? = null,
    val tanggal: String? = null,
    val tipe: String? = null,
    val status: StatusOpname = StatusOpname.DRAFT,
    val pembuat: String? = null,
    val catatanOpname: String? = null,
    val catatanPersetujuan: String? = null,
    val item: List<OpnameItemDetail> = emptyList(),
    val pemakaian: Map<String, Double> = emptyMap(),
    val belumDihitung: List<BahanAktifRingkas> = emptyList(),
    val tabBelumDihitung: Boolean = false,
) {
    val terhitung: List<OpnameItemDetail> get() = item.filter { it.terhitung }

    val jumlahPas: Int get() = terhitung.count { it.selisih == 0.0 }

    /** Yang ditandai tapi selisihnya nol tidak dihitung dua kali. */
    val jumlahFlagged: Int get() = terhitung.count { it.selisih != 0.0 && it.flagged }

    val jumlahToleransi: Int get() = terhitung.size - jumlahPas - jumlahFlagged
}

/**
 * Detail satu opname — cermin `components/stok/OpnameDetail.tsx`.
 *
 * Seluruhnya baca-saja. Penyuntingan tetap lewat formulir opname, dan `selisih`
 * memang tidak bisa ditulis dari mana pun: kolomnya generated stored di database.
 */
class DetailOpnameViewModel : ViewModel() {
    private val _state = MutableStateFlow(DetailOpnameUiState())
    val state: StateFlow<DetailOpnameUiState> = _state

    private var idTerakhir: String? = null

    fun muat(opnameId: String) {
        idTerakhir = opnameId
        viewModelScope.launch {
            _state.value = _state.value.copy(memuat = true, error = null)
            try {
                val kepala = OpnameRepository.kepala(opnameId)
                    ?: error("Opname tidak ditemukan atau di luar cakupan outlet Anda.")

                val outletId = kepala.optString("outlet_id").orEmpty()
                val tanggal = kepala.optString("tanggal")

                val item = OpnameRepository.detailItem(opnameId)
                // Pemakaian hanya bisa dijumlah kalau tanggal dan outletnya diketahui;
                // kegagalannya tidak boleh menjatuhkan layar, cuma menghilangkan kolom.
                val pemakaian = if (outletId.isNotBlank() && !tanggal.isNullOrBlank()) {
                    runCatching { OpnameRepository.pemakaianHarian(outletId, tanggal) }
                        .getOrDefault(emptyMap())
                } else {
                    emptyMap()
                }
                val dihitung = item.map { it.bahanBakuId }.toSet()
                val belum = runCatching { OpnameRepository.bahanAktifRingkas() }
                    .getOrDefault(emptyList())
                    .filter { it.id !in dihitung }

                _state.value = _state.value.copy(
                    memuat = false,
                    tanggal = tanggal,
                    tipe = kepala.optString("tipe"),
                    status = StatusOpname.dari(kepala.optString("status")),
                    pembuat = kepala.optJsonObject("outlet_staff")?.optString("name"),
                    catatanOpname = kepala.optString("notes")?.takeIf { it.isNotBlank() },
                    catatanPersetujuan = kepala.optString("approval_notes")?.takeIf { it.isNotBlank() },
                    item = item,
                    pemakaian = pemakaian,
                    belumDihitung = belum,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    memuat = false,
                    error = e.message ?: stokErrorMessage(e),
                )
            }
        }
    }

    fun pilihTab(belumDihitung: Boolean) {
        _state.value = _state.value.copy(tabBelumDihitung = belumDihitung)
    }

    fun muatUlang() { idTerakhir?.let { muat(it) } }
}

@Composable
fun DetailOpnameScreen(
    opnameId: String,
    onKembali: () -> Unit,
    viewModel: DetailOpnameViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val peran = AppSession.staff.collectAsState().value?.role
    val bolehAnalisis = StokAkses.melihatAnalisisSelisih(peran)

    LaunchedEffect(opnameId) { viewModel.muat(opnameId) }
    RealtimeRefresh(RealtimeTables.OPNAME, RealtimeTables.OPNAME_ITEM) { viewModel.muatUlang() }

    Column(Modifier.fillMaxSize().background(SukaSurface)) {
        HeaderStok(
            judul = "Detail Opname Stok",
            subjudul = tanggalPanjang(state.tanggal),
            onKembali = onKembali,
        )
        Box(Modifier.fillMaxSize().navigationBarsPadding()) {
            when {
                state.memuat && state.item.isEmpty() -> MemuatPenuh()
                state.error != null -> KeadaanGagal(state.error!!, viewModel::muatUlang)
                else -> Isi(state, bolehAnalisis, viewModel)
            }
        }
    }
}

@Composable
private fun Isi(
    state: DetailOpnameUiState,
    bolehAnalisis: Boolean,
    viewModel: DetailOpnameViewModel,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "kepala") { KartuKepala(state) }

        if (state.status == StatusOpname.PENDING_APPROVAL) {
            item(key = "pita-pending") {
                Pita(AMBER_BG, AMBER_LINE, AMBER_TEKS, "Menunggu Persetujuan Leader")
            }
        }
        if (state.status == StatusOpname.REJECTED) {
            item(key = "pita-tolak") {
                Pita(
                    MERAH_BG, MERAH_LINE, MERAH,
                    "Ditolak" + (state.catatanPersetujuan?.let { " — $it" } ?: ""),
                )
            }
        }

        item(key = "statistik") { KartuStatistik(state) }
        item(key = "tab") { BarisTab(state, viewModel) }

        if (state.tabBelumDihitung) {
            if (state.belumDihitung.isEmpty()) {
                item(key = "belum-kosong") {
                    KeadaanKosong("Semua bahan aktif sudah dihitung pada opname ini.")
                }
            } else {
                items(state.belumDihitung, key = { "b-${it.id}" }) { BarisBelumDihitung(it) }
            }
        } else {
            if (state.item.isEmpty()) {
                item(key = "item-kosong") { KeadaanKosong("Opname ini belum punya baris hitung.") }
            } else {
                items(state.item, key = { it.id }) { baris ->
                    BarisItem(baris, state.pemakaian[baris.bahanBakuId], bolehAnalisis)
                }
            }
        }
    }
}

@Composable
private fun KartuKepala(state: DetailOpnameUiState) {
    val warna = when (state.status) {
        StatusOpname.FINALIZED, StatusOpname.APPROVED -> HIJAU
        StatusOpname.PENDING_APPROVAL -> Color(0xFFC27A12)
        StatusOpname.REJECTED -> MERAH
        StatusOpname.DRAFT -> SLATE500
    }
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, GARIS),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    labelTipe(state.tipe),
                    color = SukaOnSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                )
                Text(
                    state.pembuat?.let { "Dibuat oleh $it" } ?: "Pembuat tidak tercatat",
                    color = SukaOnSurfaceVariant, fontSize = 10.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                if (state.catatanOpname != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(state.catatanOpname, color = SLATE500, fontSize = 10.sp)
                }
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = warna.copy(alpha = 0.10f),
                border = BorderStroke(1.dp, warna.copy(alpha = 0.28f)),
            ) {
                Text(
                    state.status.label,
                    Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    color = warna, fontSize = 10.sp, fontWeight = FontWeight.Black,
                )
            }
        }
    }
}

@Composable
private fun KartuStatistik(state: DetailOpnameUiState) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, GARIS),
    ) {
        Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            SelStat("Pas", state.jumlahPas.toString(), HIJAU, Modifier.weight(1f))
            SelStat("Dalam toleransi", state.jumlahToleransi.toString(), SLATE500, Modifier.weight(1f))
            SelStat("Di luar toleransi", state.jumlahFlagged.toString(), Color(0xFFC2410C), Modifier.weight(1f))
        }
    }
}

@Composable
private fun SelStat(judul: String, nilai: String, warna: Color, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(nilai, color = warna, fontSize = 17.sp, fontWeight = FontWeight.Black)
        Text(judul, color = SLATE400, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BarisTab(state: DetailOpnameUiState, viewModel: DetailOpnameViewModel) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Tab("Dihitung (${state.item.size})", !state.tabBelumDihitung) { viewModel.pilihTab(false) }
        Tab("Belum Dihitung (${state.belumDihitung.size})", state.tabBelumDihitung) { viewModel.pilihTab(true) }
    }
}

@Composable
private fun Tab(teks: String, aktif: Boolean, onKlik: () -> Unit) {
    Surface(
        Modifier.clickable(onClick = onKlik),
        shape = RoundedCornerShape(50),
        color = if (aktif) Color(0xFFEA580C).copy(alpha = 0.12f) else Color.White,
        border = BorderStroke(1.dp, if (aktif) Color(0xFFEA580C).copy(alpha = 0.35f) else GARIS),
    ) {
        Text(
            teks,
            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = if (aktif) Color(0xFFC2410C) else SLATE500,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun Pita(latar: Color, garis: Color, teksWarna: Color, teks: String) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = latar,
        border = BorderStroke(1.dp, garis),
    ) {
        Text(
            teks,
            Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            color = teksWarna, fontSize = 11.sp, fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun BarisItem(it: OpnameItemDetail, pemakaian: Double?, bolehAnalisis: Boolean) {
    val catatan = parseCatatanOpname(it.catatan)
    val persen = Selisih.persen(it.selisih, it.qtySystem)
    val ambang = Selisih.ambangPersen(it.meta.satuan, it.meta.satuanKecil)

    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, if (it.flagged) MERAH_LINE else GARIS),
    ) {
        Column(Modifier.padding(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    it.namaTampil,
                    Modifier.weight(1f),
                    color = SukaOnSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                if (bolehAnalisis && it.terhitung) {
                    Text(
                        persen.teks,
                        color = when {
                            persen.nol -> HIJAU
                            it.flagged -> MERAH
                            else -> SLATE500
                        },
                        fontSize = 11.sp, fontWeight = FontWeight.Black,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            BarisAngka("Sistem", labelGram(it.qtySystem, it.meta), SLATE500)
            BarisAngka(
                "Fisik",
                if (it.terhitung) labelGram(it.qtyFisik!!, it.meta) else "Belum terhitung",
                if (it.terhitung) SukaOnSurface else SLATE400,
            )
            if (it.terhitung) {
                BarisAngka(
                    "Selisih",
                    labelGram(it.selisih, it.meta),
                    when {
                        it.selisih == 0.0 -> HIJAU
                        it.selisih < 0 -> MERAH
                        else -> Color(0xFFC2410C)
                    },
                )
            }
            if (pemakaian != null && pemakaian > 0.0) {
                BarisAngka("Pemakaian resep", labelGram(pemakaian, it.meta), SLATE500)
            }
            if (bolehAnalisis && it.terhitung) {
                Spacer(Modifier.height(6.dp))
                HorizontalDivider(color = GARIS)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Toleransi $ambang%" + if (it.flagged) " · di luar toleransi" else "",
                    color = if (it.flagged) Color(0xFFC2410C) else SLATE400,
                    fontSize = 9.sp, fontWeight = FontWeight.Bold,
                )
            }
            if (catatan.targetKitchen != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Target Kitchen: ${catatan.targetKitchen}",
                    color = SLATE500, fontSize = 9.sp,
                )
            }
            if (catatan.catatanBebas != null) {
                Spacer(Modifier.height(6.dp))
                Text(catatan.catatanBebas, color = SLATE500, fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun BarisAngka(label: String, nilai: String, warna: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
        Text(label, Modifier.weight(1f), color = SLATE400, fontSize = 10.sp)
        Text(nilai, color = warna, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BarisBelumDihitung(b: BahanAktifRingkas) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, GARIS),
    ) {
        Row(Modifier.padding(horizontal = 13.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                b.nama,
                Modifier.weight(1f),
                color = SukaOnSurface, fontSize = 11.sp, fontWeight = FontWeight.Medium,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(b.kategori, color = SLATE400, fontSize = 9.sp)
        }
    }
}

// ------------------------------------------------------------------ pemformat

/**
 * Label qty opname pada satuan terkecil, mis. "11 Kg · 278 Gram".
 *
 * `qty_fisik`, `qty_system`, `selisih`, dan jumlah pemakaian dari `ledger_stok`
 * SEMUANYA gram-scale. Jangan pakai varian satuan-besar seperti pada riwayat waste —
 * itu bug 1000× yang sama seperti "11278 Kg" di SPVTable web dulu.
 *
 * Tandanya dipasang di depan seluruh rangkaian dan angkanya dipecah dari nilai
 * mutlak, persis `formatTriUnitSaldoFromGram` — memecah angka negatif langsung
 * membuat bagian tengah dan kecilnya ikut negatif dan terbaca janggal.
 */
private fun labelGram(qty: Double, meta: UnitMeta): String {
    val tanda = if (qty < 0) "-" else ""
    val p = decomposeTriUnit(
        abs(qty), true, meta.satuanTengah, meta.faktorTengah, meta.satuanKecil, meta.faktorTampilan,
    )
    val bagian = listOf(
        p.besar to meta.satuan,
        p.tengah to meta.satuanTengah,
        p.kecil to meta.satuanKecil,
    ).filter { it.first != 0.0 && !it.second.isNullOrBlank() }
        .joinToString(" · ") { "${formatAngkaStok(it.first)} ${it.second}" }

    return if (bagian.isBlank()) "0 ${meta.satuanKecil ?: meta.satuan.orEmpty()}".trim()
    else "$tanda$bagian"
}

private fun labelTipe(tipe: String?): String = when (tipe) {
    "harian" -> "Opname Harian"
    "mingguan" -> "Opname Mingguan"
    "ad_hoc" -> "Opname Ad Hoc"
    else -> "Opname"
}

/** "Senin, 7 September 2026" — bentuk panjang yang dipakai web di kepala halaman. */
private fun tanggalPanjang(tanggal: String?): String {
    if (tanggal.isNullOrBlank()) return "-"
    return runCatching {
        LocalDate.parse(tanggal.take(10))
            .format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale("id", "ID")))
    }.getOrDefault(tanggal)
}
