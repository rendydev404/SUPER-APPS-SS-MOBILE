package com.sukashawarma.superapp.feature.stok.ui.opname

import com.sukashawarma.superapp.core.ui.kaca.navigationBarsPaddingKaca
import com.sukashawarma.superapp.core.ui.ios.AngkaIos
import com.sukashawarma.superapp.core.ui.ios.BlokAngkaIos
import com.sukashawarma.superapp.core.ui.ios.KartuIos
import com.sukashawarma.superapp.core.ui.ios.LencanaIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.PemisahIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.ios.bayanganIos
import com.sukashawarma.superapp.core.ui.ios.tekanIos
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

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

    Column(Modifier.fillMaxSize().background(WarnaIos.Latar)) {
        HeaderStok(
            judul = "Detail Opname Stok",
            subjudul = tanggalPanjang(state.tanggal),
            onKembali = onKembali,
        )
        Box(Modifier.fillMaxSize().navigationBarsPaddingKaca()) {
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
        contentPadding = PaddingValues(start = UkuranIos.TepiLayar, end = UkuranIos.TepiLayar, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(UkuranIos.JarakKartu),
    ) {
        item(key = "kepala") { KartuKepala(state) }

        if (state.status == StatusOpname.PENDING_APPROVAL) {
            item(key = "pita-pending") {
                Pita(NadaIos.PERINGATAN, "Menunggu Persetujuan Leader")
            }
        }
        if (state.status == StatusOpname.REJECTED) {
            item(key = "pita-tolak") {
                Pita(
                    NadaIos.BAHAYA,
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

private fun StatusOpname.nadaIos(): NadaIos = when (this) {
    StatusOpname.FINALIZED, StatusOpname.APPROVED -> NadaIos.SUKSES
    StatusOpname.PENDING_APPROVAL -> NadaIos.PERINGATAN
    StatusOpname.REJECTED -> NadaIos.BAHAYA
    StatusOpname.DRAFT -> NadaIos.NETRAL
}

@Composable
private fun KartuKepala(state: DetailOpnameUiState) {
    KartuIos {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(labelTipe(state.tipe), style = TipeIos.Utama)
                Text(
                    state.pembuat?.let { "Dibuat oleh $it" } ?: "Pembuat tidak tercatat",
                    style = TipeIos.Catatan,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                if (state.catatanOpname != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(state.catatanOpname, style = TipeIos.Catatan)
                }
            }
            Spacer(Modifier.width(8.dp))
            LencanaIos(state.status.label, state.status.nadaIos())
        }
    }
}

@Composable
private fun KartuStatistik(state: DetailOpnameUiState) {
    KartuIos(padding = PaddingValues(12.dp)) {
        BlokAngkaIos(
            listOf(
                AngkaIos("Pas", state.jumlahPas.toString()),
                AngkaIos("Dalam toleransi", state.jumlahToleransi.toString()),
                AngkaIos("Di luar toleransi", state.jumlahFlagged.toString(), negatif = state.jumlahFlagged > 0),
            ),
        )
    }
}

@Composable
private fun BarisTab(state: DetailOpnameUiState, viewModel: DetailOpnameViewModel) {
    // Segmented control ala iOS: satu wadah abu, segmen aktif terangkat putih.
    Row(
        Modifier
            .fillMaxWidth()
            .background(WarnaIos.Isian, UkuranIos.SudutKontrol)
            .padding(2.dp),
    ) {
        Tab("Dihitung (${state.item.size})", !state.tabBelumDihitung, Modifier.weight(1f)) { viewModel.pilihTab(false) }
        Tab("Belum Dihitung (${state.belumDihitung.size})", state.tabBelumDihitung, Modifier.weight(1f)) { viewModel.pilihTab(true) }
    }
}

@Composable
private fun Tab(teks: String, aktif: Boolean, modifier: Modifier = Modifier, onKlik: () -> Unit) {
    val bentuk = RoundedCornerShape(10.dp)
    Box(
        modifier
            .then(if (aktif) Modifier.bayanganIos(bentuk, 2.dp).background(WarnaIos.Kartu, bentuk) else Modifier)
            .tekanIos(onKlik)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            teks,
            style = TipeIos.Catatan.copy(
                color = WarnaIos.Label,
                fontWeight = if (aktif) FontWeight.SemiBold else FontWeight.Normal,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun Pita(nada: NadaIos, teks: String) {
    Text(
        teks,
        Modifier
            .fillMaxWidth()
            .background(nada.warna.copy(alpha = 0.12f), UkuranIos.SudutKontrol)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        style = TipeIos.Catatan.copy(color = nada.teks, fontWeight = FontWeight.SemiBold),
    )
}

@Composable
private fun BarisItem(it: OpnameItemDetail, pemakaian: Double?, bolehAnalisis: Boolean) {
    val catatan = parseCatatanOpname(it.catatan)
    val persen = Selisih.persen(it.selisih, it.qtySystem)
    val ambang = Selisih.ambangPersen(it.meta.satuan, it.meta.satuanKecil)

    // Garis merah dipertahankan hanya untuk baris di luar toleransi — penanda
    // semantik, bukan garis tepi dekoratif kartu.
    val tepi = if (it.flagged) {
        Modifier.border(1.dp, WarnaIos.Merah.copy(alpha = 0.4f), UkuranIos.SudutKartu)
    } else {
        Modifier
    }

    KartuIos(tepi) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                it.namaTampil,
                Modifier.weight(1f),
                style = TipeIos.Utama,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            if (bolehAnalisis && it.terhitung) {
                Spacer(Modifier.width(8.dp))
                LencanaIos(
                    persen.teks,
                    when {
                        persen.nol -> NadaIos.SUKSES
                        it.flagged -> NadaIos.BAHAYA
                        else -> NadaIos.NETRAL
                    },
                    titik = false,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        BarisAngka("Sistem", labelGram(it.qtySystem, it.meta), WarnaIos.LabelKedua)
        BarisAngka(
            "Fisik",
            if (it.terhitung) labelGram(it.qtyFisik!!, it.meta) else "Belum terhitung",
            if (it.terhitung) WarnaIos.Label else WarnaIos.LabelKetiga,
        )
        if (it.terhitung) {
            BarisAngka(
                "Selisih",
                labelGram(it.selisih, it.meta),
                when {
                    it.selisih == 0.0 -> NadaIos.SUKSES.teks
                    it.selisih < 0 -> NadaIos.BAHAYA.teks
                    else -> NadaIos.PERINGATAN.teks
                },
            )
        }
        if (pemakaian != null && pemakaian > 0.0) {
            BarisAngka("Pemakaian resep", labelGram(pemakaian, it.meta), WarnaIos.LabelKedua)
        }
        if (bolehAnalisis && it.terhitung) {
            Spacer(Modifier.height(8.dp))
            PemisahIos(inset = 0.dp)
            Spacer(Modifier.height(8.dp))
            Text(
                "Toleransi $ambang%" + if (it.flagged) " · di luar toleransi" else "",
                style = TipeIos.Kecil.copy(
                    color = if (it.flagged) NadaIos.PERINGATAN.teks else WarnaIos.LabelKedua,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
        if (catatan.targetKitchen != null) {
            Spacer(Modifier.height(6.dp))
            Text("Target Kitchen: ${catatan.targetKitchen}", style = TipeIos.Kecil)
        }
        if (catatan.catatanBebas != null) {
            Spacer(Modifier.height(6.dp))
            Text(catatan.catatanBebas, style = TipeIos.Kecil)
        }
    }
}

@Composable
private fun BarisAngka(label: String, nilai: String, warna: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, Modifier.weight(1f), style = TipeIos.Catatan)
        Text(nilai, style = TipeIos.Catatan.copy(color = warna, fontWeight = FontWeight.SemiBold))
    }
}

@Composable
private fun BarisBelumDihitung(b: BahanAktifRingkas) {
    KartuIos(padding = PaddingValues(horizontal = UkuranIos.PaddingKartu, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                b.nama,
                Modifier.weight(1f),
                style = TipeIos.Keterangan,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(8.dp))
            Text(b.kategori, style = TipeIos.Catatan)
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
