package com.sukashawarma.superapp.feature.stok.ui.permintaan

import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.core.ui.kaca.denganRuangNav
import com.sukashawarma.superapp.core.ui.kaca.navigationBarsPaddingKaca
import com.sukashawarma.superapp.core.ui.ios.JudulSeksiIos
import com.sukashawarma.superapp.core.ui.ios.KartuIos
import com.sukashawarma.superapp.core.ui.ios.KeadaanIos
import com.sukashawarma.superapp.core.ui.ios.KolomCariIos
import com.sukashawarma.superapp.core.ui.ios.LabelSeksiIos
import com.sukashawarma.superapp.core.ui.ios.LencanaIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.SegmenIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.TombolUtamaIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WadahSegmenIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.ios.bayanganIos
import com.sukashawarma.superapp.core.ui.ios.tekanIos
import com.sukashawarma.superapp.core.ui.ios.warnaKolomIos
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.feature.stok.data.model.BahanBaku
import com.sukashawarma.superapp.feature.stok.data.model.BudgetStatus
import com.sukashawarma.superapp.feature.stok.data.model.Permintaan
import com.sukashawarma.superapp.feature.stok.data.model.SaranPermintaan
import com.sukashawarma.superapp.feature.stok.data.model.StatusPermintaan
import com.sukashawarma.superapp.feature.stok.domain.Budget
import com.sukashawarma.superapp.feature.stok.domain.BudgetVarian
import com.sukashawarma.superapp.feature.stok.domain.DistribusiUnit
import com.sukashawarma.superapp.feature.stok.domain.StatusTopUp
import com.sukashawarma.superapp.feature.stok.domain.formatAngkaStok
import com.sukashawarma.superapp.feature.stok.domain.formatRupiah as formatRp
import com.sukashawarma.superapp.feature.stok.domain.formatSatuan
import com.sukashawarma.superapp.feature.stok.domain.formatTriUnitAdaptif
import com.sukashawarma.superapp.feature.stok.ui.HeaderStok
import com.sukashawarma.superapp.feature.stok.ui.KeadaanGagal
import com.sukashawarma.superapp.feature.stok.ui.KeadaanKosong
import com.sukashawarma.superapp.feature.stok.ui.KeadaanTidakBerhak
import com.sukashawarma.superapp.feature.stok.ui.MemuatPenuh
import com.sukashawarma.superapp.feature.stok.ui.PemilihOutlet
import com.sukashawarma.superapp.feature.stok.ui.PitaPesan
import com.sukashawarma.superapp.feature.stok.ui.waktuSingkat
import kotlin.math.ceil
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.RealtimeTables

/**
 * Kategori tampilan satu bahan. Menyamakan kosong dan null menjadi "LAIN-LAIN"
 * sama seperti RPC estimasi (`COALESCE(NULLIF(kategori,''),'LAIN-LAIN')`) — kalau
 * berbeda, subtotal per kategori tidak akan pernah ketemu kuncinya.
 */
private fun kategoriTampil(kategori: String?): String =
    kategori?.takeIf { it.isNotBlank() } ?: "LAIN-LAIN"

private fun nadaStatus(status: StatusPermintaan): NadaIos = when (status) {
    StatusPermintaan.DISETUJUI -> NadaIos.SUKSES
    StatusPermintaan.MENUNGGU -> NadaIos.PERINGATAN
    StatusPermintaan.DITOLAK -> NadaIos.BAHAYA
    StatusPermintaan.DIBATALKAN -> NadaIos.NETRAL
}

private fun nadaTopUp(status: StatusTopUp): NadaIos = when (status) {
    StatusTopUp.DISETUJUI -> NadaIos.SUKSES
    StatusTopUp.DITOLAK -> NadaIos.BAHAYA
    StatusTopUp.MENUNGGU_FINANCE -> NadaIos.INFO
    StatusTopUp.MENUNGGU_AM -> NadaIos.PERINGATAN
}

/**
 * Judul grup katalog saat filter "kritis" aktif. Isinya bahan kritis DAN yang baru
 * menipis, jadi judulnya tidak boleh menjanjikan kritis semua.
 */
private const val JUDUL_PERLU_DIMINTA = "Bahan Baku Perlu Diminta"

// ------------------------------------------------------------- helper gaya iOS

/**
 * Tombol aksi 50dp untuk baris berisi dua-tiga tombol. [com.sukashawarma.superapp.core.ui.ios.TombolUtamaIos]
 * memakai label 17sp tanpa batas baris, yang terbelah dua di sepertiga lebar layar.
 * [terisi] false memberi gaya tombol sekunder (isian warna tipis).
 */
@Composable
private fun TombolAksi(
    teks: String,
    onKlik: () -> Unit,
    modifier: Modifier = Modifier,
    warna: Color = WarnaIos.Aksen,
    terisi: Boolean = true,
    aktif: Boolean = true,
    ikon: ImageVector? = null,
) {
    val latar = when {
        terisi && aktif -> warna
        terisi -> WarnaIos.Isian
        else -> warna.copy(alpha = if (aktif) 0.12f else 0.06f)
    }
    val warnaTeks = when {
        terisi && aktif -> Color.White
        terisi -> WarnaIos.Abu
        else -> warna.copy(alpha = if (aktif) 1f else 0.4f)
    }
    Row(
        modifier
            .height(UkuranIos.TinggiTombol)
            .background(latar, UkuranIos.SudutBlok)
            .tekanIos(onKlik, aktif = aktif)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (ikon != null) {
            Icon(ikon, null, tint = warnaTeks, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(
            teks,
            color = warnaTeks,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Blok keterangan bernada: isian tipis tanpa garis tepi, pengganti kotak info bergaris. */
@Composable
private fun BlokNada(nada: NadaIos, modifier: Modifier = Modifier, isi: @Composable () -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .background(nada.warna.copy(alpha = 0.10f), UkuranIos.SudutGrup)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) { isi() }
}

/**
 * Badge sisa budget — cermin `BudgetBadge.tsx`. Disembunyikan bila outlet tak
 * punya plafon. Tidak pernah memblokir pengiriman: web pun menandainya
 * "Tahap Developer (Bisa Diabaikan)", keputusan tetap di approver.
 */
@Composable
private fun BadgeBudget(
    status: BudgetStatus?,
    proyeksi: Double = 0.0,
    ringkas: Boolean = false,
    onTopUp: (() -> Unit)? = null,
) {
    if (status == null) return
    val varian = Budget.varian(status.hasConfig, status.nominal, status.terpakai, proyeksi)
    if (varian == BudgetVarian.TERSEMBUNYI) return

    val nada = when (varian) {
        BudgetVarian.HIJAU -> NadaIos.SUKSES
        BudgetVarian.ORANYE -> NadaIos.PERINGATAN
        else -> NadaIos.BAHAYA
    }
    val sisaProyeksi = status.sisa - proyeksi

    if (ringkas) {
        val label = when (varian) {
            BudgetVarian.MERAH ->
                "Melebihi Budget" + if (sisaProyeksi < 0) " +${formatRp(-sisaProyeksi)}" else ""
            BudgetVarian.ORANYE -> "Mendekati Budget"
            else -> "Dalam Budget"
        }
        LencanaIos(label, nada)
        return
    }

    BlokNada(nada) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Sisa Budget ${Budget.labelPeriode(status.periodType, status.customDays)}: " +
                        formatRp(maxOf(0.0, sisaProyeksi)) + " dari ${formatRp(status.nominal)}",
                    style = TipeIos.Catatan.copy(color = nada.teks, fontWeight = FontWeight.SemiBold),
                )
                Text(
                    "Tahap Developer (Bisa Diabaikan)",
                    style = TipeIos.Kecil.copy(color = NadaIos.PERINGATAN.teks, fontSize = 11.sp),
                )
            }
            if (onTopUp != null) {
                Spacer(Modifier.width(8.dp))
                Text(
                    "＋ Top-Up",
                    Modifier
                        .background(WarnaIos.Aksen, UkuranIos.SudutKapsul)
                        .tekanIos(onTopUp)
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, softWrap = false,
                )
            }
        }
        if (proyeksi > 0) {
            Text(
                "(Termasuk estimasi keranjang saat ini: -${formatRp(proyeksi)})",
                style = TipeIos.Kecil,
            )
        }
        if (sisaProyeksi < 0) {
            Spacer(Modifier.height(5.dp))
            Text(
                "⚠️ Saldo tidak mencukupi (minus ${formatRp(-sisaProyeksi)}). " +
                    "Pengajuan tetap dapat dilakukan selama tahap developer.",
                style = TipeIos.Kecil.copy(color = NadaIos.BAHAYA.teks, fontWeight = FontWeight.SemiBold),
            )
        }
    }
}

/**
 * Qty permintaan yang TERSIMPAN (selalu satuan besar) menjadi teks pada satuan pesan,
 * dibulatkan ke atas — cermin tampilan web.
 */
private fun qtyTersimpanTeks(qtyBase: Double, bahan: BahanBaku?, satuanCadangan: String?): String {
    val satuan = formatSatuan(bahan?.satuanPesan ?: satuanCadangan)
    if (bahan == null) return "${formatAngkaStok(qtyBase)} $satuan".trim()
    val dist = ceil(DistribusiUnit.keDistribusi(qtyBase, bahan.faktorDistribusi)).toLong()
    return "$dist $satuan".trim()
}

@Composable
fun PermintaanScreen(viewModel: PermintaanViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    RealtimeRefresh(RealtimeTables.PERMINTAAN) { viewModel.muatAwal() }
    // Saldo berubah di setiap transaksi kasir di semua outlet; dibatasi sekali per
    // 30 detik. Status permintaan di atas tetap seketika.
    RealtimeRefresh(RealtimeTables.STOK_BALANCE, jedaMinimumMs = 30_000L) { viewModel.muatAwal() }

    when {
        state.approveUntuk != null -> LayarPersetujuan(state, state.approveUntuk!!, viewModel)
        state.tinjauTerbuka -> LayarTinjau(state, viewModel)
        else -> LayarUtama(state, viewModel)
    }

    if (state.nudgeTerbuka) DialogNudge(state, viewModel)
    if (state.konfirmasiTerbuka) DialogKonfirmasi(state, viewModel)
    if (state.topUpTerbuka) DialogTopUp(state, viewModel)
}

// ============================================================== layar utama

@Composable
private fun LayarUtama(state: PermintaanUiState, viewModel: PermintaanViewModel) {
    Column(Modifier.fillMaxSize().background(WarnaIos.Latar)) {
        HeaderStok(
            judul = "Permintaan Bahan Baku",
            subjudul = "Alur distribusi Kitchen & Outlet",
        )
        state.pesan?.let { PitaPesan(it, false, viewModel::bersihkanPesan) }
        state.error?.let { PitaPesan(it, true, viewModel::bersihkanPesan) }

        when {
            state.tidakBerhak -> KeadaanTidakBerhak("Akun Anda belum terhubung dengan outlet mana pun.")
            state.memuat && state.outlets.isEmpty() -> MemuatPenuh()
            state.error != null && state.katalog.isEmpty() && state.daftarReview.isEmpty() ->
                KeadaanGagal(state.error, viewModel::muatAwal)
            else -> {
                if (state.outlets.size > 1) {
                    PemilihOutlet(state.outlets, state.outletTerpilih, viewModel::pilihOutlet)
                }
                // Antrean hanya tab tambahan: Buat Baru dan Riwayat tetap ada untuk
                // semua peran, termasuk leader/SPV yang memegang outletnya sendiri.
                val tabs = TabPermintaan.entries.filter {
                    it != TabPermintaan.ANTREAN || state.bolehAntrean
                }
                // Segmented control ala iOS: satu wadah abu, segmen aktif terangkat putih.
                WadahSegmenIos(Modifier.padding(horizontal = UkuranIos.TepiLayar, vertical = 10.dp)) {
                    tabs.forEach { t ->
                        val jumlah = if (t == TabPermintaan.ANTREAN) state.daftarReview.size else 0
                        SegmenIos(
                            if (jumlah > 0) "${t.label} ($jumlah)" else t.label,
                            state.tab == t,
                            { viewModel.pilihTab(t) },
                            Modifier.weight(1f),
                            jarakSisi = 4.dp,
                        )
                    }
                }
                when (state.tab) {
                    TabPermintaan.BUAT -> KontenKatalog(state, viewModel)
                    TabPermintaan.RIWAYAT -> KontenRiwayat(state, viewModel)
                    TabPermintaan.ANTREAN -> AntreanPersetujuan(state, viewModel)
                }
            }
        }
    }
}

// ================================================================= katalog

@Composable
private fun KontenKatalog(state: PermintaanUiState, viewModel: PermintaanViewModel) {
    val pending = state.pendingItemIds
    val saranBoleh = state.saranBoleh
    val saranMap = saranBoleh.associateBy { it.bahanBakuId }
    val terfilter = state.katalogTerfilter
    val belumDitambah = saranBoleh.count { (state.keranjang[it.bahanBakuId] ?: 0L) <= 0L }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(
                start = UkuranIos.TepiLayar, end = UkuranIos.TepiLayar, top = 4.dp, bottom = 16.dp,
            ).denganRuangNav(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (pending.isNotEmpty()) {
                item(key = "pending-alert") {
                    BlokNada(NadaIos.PERINGATAN) {
                        Text(
                            "${pending.size} item bahan baku sedang menunggu persetujuan kitchen. " +
                                "Bahan tersebut otomatis disembunyikan agar tidak terduplikasi.",
                            style = TipeIos.Catatan.copy(color = NadaIos.PERINGATAN.teks, fontWeight = FontWeight.Medium),
                        )
                    }
                }
            }

            if (state.budget != null) {
                item(key = "budget") {
                    BadgeBudget(
                        status = state.budget,
                        proyeksi = state.estimasi.totalNilai,
                        onTopUp = viewModel::bukaTopUp,
                    )
                }
            }
            if (state.daftarTopUp.isNotEmpty()) {
                item(key = "topup-riwayat") {
                    KartuTopUp(state, viewModel)
                }
            }

            item(key = "cari") {
                KolomCariIos(
                    state.cari,
                    viewModel::ubahCari,
                    Modifier.fillMaxWidth(),
                    placeholder = "Cari nama bahan baku atau kategori…",
                )
            }

            item(key = "kategori-chips") {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ChipKategori(
                        label = "Semua (${state.katalogBoleh.count { it.id !in pending }})",
                        aktif = state.kategoriTerpilih == "all",
                        warnaAktif = WarnaIos.Aksen,
                    ) { viewModel.pilihKategori("all") }
                    if (saranBoleh.isNotEmpty()) {
                        ChipKategori(
                            label = "🔥 Perlu Diminta (${saranBoleh.size})",
                            aktif = state.kategoriTerpilih == "kritis",
                            warnaAktif = WarnaIos.Merah,
                        ) { viewModel.pilihKategori("kritis") }
                    }
                    state.kategoriList.forEach { kat ->
                        ChipKategori(
                            label = kat,
                            aktif = state.kategoriTerpilih == kat,
                            warnaAktif = WarnaIos.Aksen,
                        ) { viewModel.pilihKategori(kat) }
                    }
                }
            }

            if (belumDitambah > 0) {
                item(key = "tambah-kritis") {
                    TombolUtamaIos(
                        "Tambah Semua yang Perlu ($belumDitambah)",
                        viewModel::tambahSemuaKritis,
                        ikon = IkonIos.Add,
                        warna = WarnaIos.Merah,
                    )
                }
            }

            if (terfilter.isEmpty()) {
                item(key = "kosong") {
                    val bisaReset = state.cari.isNotEmpty() || state.kategoriTerpilih != "all"
                    KeadaanIos(
                        ikon = IkonIos.Search,
                        judul = "Tidak ada bahan baku yang sesuai",
                        pesan = "Coba ubah kata kunci atau filter kategori.",
                        teksAksi = if (bisaReset) "Reset Filter" else null,
                        onAksi = if (bisaReset) viewModel::resetFilter else null,
                    )
                }
            } else {
                // Kelompokkan per kategori seperti web; filter kritis/kategori jadi satu grup.
                val grup: List<Pair<String, List<BahanBaku>>> = when (state.kategoriTerpilih) {
                    "kritis" -> listOf(JUDUL_PERLU_DIMINTA to terfilter)
                    "all" -> terfilter.groupBy { kategoriTampil(it.kategori) }.toList()
                    else -> listOf(state.kategoriTerpilih to terfilter)
                }
                grup.forEach { (kategori, daftar) ->
                    item(key = "header-$kategori") {
                        JudulSeksiIos(kategori, keterangan = "${daftar.size} item")
                    }
                    items(daftar, key = { it.id }) { bahan ->
                        KartuBahanKatalog(
                            bahan = bahan,
                            saran = saranMap[bahan.id],
                            qty = state.keranjang[bahan.id] ?: 0L,
                            viewModel = viewModel,
                        )
                    }
                }
            }
        }

        val jumlahItem = state.keranjangItems.size
        if (jumlahItem > 0) {
            BarKeranjang(jumlahItem, state.estimasi.totalNilai, viewModel::bukaTinjau)
        }
    }
}

/** Chip filter: aktif terisi warnanya, pasif isian abu — tanpa garis tepi. */
@Composable
private fun ChipKategori(label: String, aktif: Boolean, warnaAktif: Color, onClick: () -> Unit) {
    Text(
        label,
        Modifier
            .background(if (aktif) warnaAktif else WarnaIos.Isian, UkuranIos.SudutKapsul)
            .tekanIos(onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        color = if (aktif) Color.White else WarnaIos.Label,
        fontSize = 14.sp,
        fontWeight = if (aktif) FontWeight.SemiBold else FontWeight.Medium,
        maxLines = 1,
    )
}

@Composable
private fun KartuBahanKatalog(
    bahan: BahanBaku,
    saran: SaranPermintaan?,
    qty: Long,
    viewModel: PermintaanViewModel,
) {
    // Perlu diminta belum tentu kritis: yang baru menipis ikut ditawarkan di sini,
    // dan menyebutnya kritis membuat angka layar ini berselisih dengan Dashboard.
    val perluDiminta = saran != null
    val kritis = saran?.kritis(bahan.meta) == true
    val ditambah = qty > 0L

    // Garis tepi hanya sebagai penanda keadaan (di keranjang / perlu diminta),
    // bukan bingkai dekoratif kartu.
    val tepi = when {
        ditambah -> Modifier.border(1.5.dp, WarnaIos.Aksen.copy(alpha = 0.55f), UkuranIos.SudutKartu)
        perluDiminta -> Modifier.border(1.dp, WarnaIos.Merah.copy(alpha = 0.30f), UkuranIos.SudutKartu)
        else -> Modifier
    }

    KartuIos(tepi, padding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                bahan.kategori ?: "Bahan baku",
                style = TipeIos.Kecil.copy(fontWeight = FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (perluDiminta) {
                LencanaIos(
                    if (kritis) "Kritis" else "Menipis",
                    if (kritis) NadaIos.BAHAYA else NadaIos.PERINGATAN,
                )
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(
            bahan.nama,
            style = TipeIos.Utama,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        if (saran != null) {
            Text(
                "Sisa: ${formatTriUnitAdaptif(saran.currentQty, saran.saldoIsGram, bahan.meta)}",
                style = TipeIos.Catatan.copy(
                    color = if (kritis) NadaIos.BAHAYA.teks else NadaIos.PERINGATAN.teks,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
        } else {
            Text(
                "Satuan Pesan: ${formatSatuan(bahan.satuanPesan)}",
                style = TipeIos.Catatan,
            )
        }
        Spacer(Modifier.height(12.dp))
        if (!ditambah) {
            val warna = if (perluDiminta) WarnaIos.Merah else WarnaIos.Aksen
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(UkuranIos.TinggiKontrol)
                    .background(warna.copy(alpha = 0.12f), UkuranIos.SudutKontrol)
                    .tekanIos({
                        if (saran != null) viewModel.tambahKritis(saran)
                        else viewModel.ubahKeranjang(bahan.id, 1L)
                    }),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(IkonIos.Add, null, tint = warna, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    if (perluDiminta) "Rekomendasi (${formatSatuan(bahan.satuanPesan)})"
                    else "Tambah (${formatSatuan(bahan.satuanPesan)})",
                    color = if (perluDiminta) NadaIos.BAHAYA.teks else NadaIos.AKSEN.teks,
                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        } else {
            StepperQty(
                qty = qty,
                satuan = formatSatuan(bahan.satuanPesan),
                onMinus = { viewModel.ubahKeranjang(bahan.id, -1L) },
                onPlus = { viewModel.ubahKeranjang(bahan.id, 1L) },
                onSet = { viewModel.setKeranjang(bahan.id, it) },
            )
        }
    }
}

@Composable
private fun StepperQty(
    qty: Long,
    satuan: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    onSet: (String) -> Unit,
    diSorot: Boolean = false,
) {
    // Stepper ala iOS: wadah abu, tombol kurang putih terangkat, tombol tambah beraksen.
    Row(
        Modifier
            .fillMaxWidth()
            .background(
                if (diSorot) WarnaIos.Aksen.copy(alpha = 0.12f) else WarnaIos.Isian,
                UkuranIos.SudutKontrol,
            )
            .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val bentuk = RoundedCornerShape(10.dp)
        Box(
            Modifier
                .size(34.dp)
                .bayanganIos(bentuk, 2.dp)
                .background(WarnaIos.Kartu, bentuk)
                .tekanIos(onMinus),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (qty <= 1L) IkonIos.Delete else IkonIos.Remove,
                if (qty <= 1L) "Hapus" else "Kurangi",
                tint = if (qty <= 1L) WarnaIos.Merah else WarnaIos.Label,
                modifier = Modifier.size(16.dp),
            )
        }
        Row(
            Modifier.weight(1f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.foundation.text.BasicTextField(
                value = if (qty > 0L) qty.toString() else "",
                onValueChange = onSet,
                modifier = Modifier.width(34.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(WarnaIos.Aksen),
                textStyle = TipeIos.Utama.copy(
                    color = if (diSorot) NadaIos.AKSEN.teks else WarnaIos.Label,
                    textAlign = TextAlign.Center,
                ),
            )
            // softWrap dimatikan: label satuan pernah terpotong jadi dua baris ("Ba"/"l")
            // ketika kolomnya sempit, dan satuan yang terbelah lebih buruk daripada
            // satuan yang terpangkas.
            Text(
                satuan,
                style = TipeIos.Kecil.copy(fontWeight = FontWeight.Medium),
                maxLines = 1, softWrap = false, overflow = TextOverflow.Visible,
            )
        }
        Box(
            Modifier
                .size(34.dp)
                .background(WarnaIos.Aksen, RoundedCornerShape(10.dp))
                .tekanIos(onPlus),
            contentAlignment = Alignment.Center,
        ) {
            Icon(IkonIos.Add, "Tambah", tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun BarKeranjang(jumlahItem: Int, estimasi: Double, onBuka: () -> Unit) {
    Surface(color = WarnaIos.Kartu, shadowElevation = 10.dp) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onBuka)
                .navigationBarsPaddingKaca()
                .padding(horizontal = UkuranIos.TepiLayar, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(40.dp).background(WarnaIos.Aksen, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(IkonIos.ShoppingCart, null, tint = Color.White, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("$jumlahItem bahan di keranjang", style = TipeIos.Catatan)
                Text(
                    if (estimasi > 0) "Est. ${formatRp(estimasi)}" else "Tinjau & Kirim",
                    style = TipeIos.Utama,
                )
            }
            Row(
                Modifier
                    .background(WarnaIos.Aksen, UkuranIos.SudutKapsul)
                    .padding(start = 14.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Tinjau", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(2.dp))
                Icon(IkonIos.ChevronRight, null, tint = Color.White, modifier = Modifier.size(15.dp))
            }
        }
    }
}

// ================================================================== tinjau

@Composable
private fun LayarTinjau(state: PermintaanUiState, viewModel: PermintaanViewModel) {
    val items = state.keranjangItems
    val estimasi = state.estimasi
    Column(Modifier.fillMaxSize().background(WarnaIos.Latar)) {
        HeaderStok(
            judul = "Tinjau Permintaan",
            subjudul = "Periksa daftar & jumlah bahan sebelum dikirim",
            onKembali = viewModel::tutupTinjau,
        )
        state.pesan?.let { PitaPesan(it, false, viewModel::bersihkanPesan) }
        state.error?.let { PitaPesan(it, true, viewModel::bersihkanPesan) }

        if (items.isEmpty()) {
            Box(Modifier.weight(1f)) {
                KeadaanKosong("Belum ada bahan baku yang dipilih. Kembali ke katalog untuk menambah.")
            }
        } else {
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(UkuranIos.TepiLayar),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (state.budget != null) {
                    item(key = "tinjau-budget") {
                        BadgeBudget(status = state.budget, proyeksi = estimasi.totalNilai)
                    }
                }
                val perKategori = items.groupBy { kategoriTampil(it.bahan.kategori) }
                perKategori.forEach { (kategori, baris) ->
                    item(key = "tinjau-header-$kategori") {
                        val subtotal = estimasi.kategoriNilai[kategori] ?: 0.0
                        JudulSeksiIos(
                            kategori,
                            keterangan = if (subtotal > 0) "Subtotal: ${formatRp(subtotal)}" else null,
                        )
                    }
                    items(baris, key = { "tinjau-${it.bahan.id}" }) { b ->
                        KartuIos(padding = PaddingValues(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        b.bahan.nama,
                                        style = TipeIos.Utama.copy(fontSize = 16.sp),
                                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    )
                                    // Satuan pesan ditulis terpisah di sini: di dalam stepper
                                    // labelnya terjepit dan sempat tak terbaca sama sekali,
                                    // padahal ini satu-satunya penanda "1 ini maksudnya apa".
                                    Text(
                                        "Satuan pesan: ${formatSatuan(b.bahan.satuanPesan)}",
                                        style = TipeIos.Catatan,
                                    )
                                    if (estimasi.itemTanpaHarga.contains(b.bahan.id)) {
                                        Text("Harga belum diset", style = TipeIos.Kecil)
                                    }
                                }
                                Spacer(Modifier.width(10.dp))
                                Box(Modifier.width(152.dp)) {
                                    StepperQty(
                                        qty = b.qty,
                                        satuan = formatSatuan(b.bahan.satuanPesan),
                                        onMinus = { viewModel.ubahKeranjang(b.bahan.id, -1L) },
                                        onPlus = { viewModel.ubahKeranjang(b.bahan.id, 1L) },
                                        onSet = { viewModel.setKeranjang(b.bahan.id, it) },
                                    )
                                }
                            }
                        }
                    }
                }
                if (estimasi.totalNilai > 0) {
                    item(key = "tinjau-total") {
                        KartuIos {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    LabelSeksiIos("Total Estimasi Pesanan")
                                    Text("${items.size} item bahan baku", style = TipeIos.Catatan)
                                }
                                Text(
                                    formatRp(estimasi.totalNilai),
                                    style = TipeIos.Angka.copy(color = NadaIos.AKSEN.teks),
                                )
                            }
                        }
                    }
                }
            }
            Surface(color = WarnaIos.Kartu, shadowElevation = 8.dp) {
                Row(
                    Modifier.fillMaxWidth().navigationBarsPaddingKaca().padding(UkuranIos.TepiLayar),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    TombolAksi(
                        "Tambah Bahan Lain",
                        viewModel::tutupTinjau,
                        Modifier.weight(1f),
                        warna = WarnaIos.Hijau,
                        terisi = false,
                        ikon = IkonIos.Add,
                    )
                    TombolAksi(
                        if (state.mengirim) "Mengirim…" else "Kirim ${items.size} Bahan",
                        viewModel::mulaiKirim,
                        Modifier.weight(1f),
                        aktif = !state.mengirim && items.isNotEmpty(),
                    )
                }
            }
        }
    }
}

// ================================================================== riwayat

@Composable
private fun KontenRiwayat(state: PermintaanUiState, viewModel: PermintaanViewModel) {
    val daftar = state.riwayatTerfilter
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = UkuranIos.TepiLayar, end = UkuranIos.TepiLayar, top = 4.dp, bottom = 20.dp,
        ).denganRuangNav(),
        verticalArrangement = Arrangement.spacedBy(UkuranIos.JarakKartu),
    ) {
        item(key = "riwayat-cari") {
            KolomCariIos(
                state.cariRiwayat,
                viewModel::ubahCariRiwayat,
                Modifier.fillMaxWidth(),
                placeholder = "Cari kode (#REQ-…), pemohon, atau bahan…",
            )
        }
        item(key = "riwayat-filter") {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ChipKategori(
                    label = "Semua (${state.daftarOutlet.size})",
                    aktif = state.filterStatus == null,
                    warnaAktif = WarnaIos.Aksen,
                ) { viewModel.pilihFilterStatus(null) }
                StatusPermintaan.entries.forEach { st ->
                    val jumlah = state.daftarOutlet.count { it.status == st }
                    ChipKategori(
                        label = "${st.label} ($jumlah)",
                        aktif = state.filterStatus == st,
                        warnaAktif = nadaStatus(st).warna,
                    ) { viewModel.pilihFilterStatus(st) }
                }
            }
        }
        if (state.daftarOutlet.isEmpty()) {
            item(key = "riwayat-kosong") {
                KeadaanKosong("Permintaan bahan baku yang Anda buat akan tercatat dan ditampilkan di sini.")
            }
        } else if (daftar.isEmpty()) {
            item(key = "riwayat-tanpa-hasil") {
                KeadaanKosong("Tidak ada riwayat permintaan yang cocok dengan filter.")
            }
        } else {
            items(daftar, key = { it.id }) { p ->
                KartuRiwayat(p, state.bahanMap)
            }
        }
    }
}

@Composable
private fun KartuRiwayat(p: Permintaan, bahanMap: Map<String, BahanBaku>) {
    KartuIos {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                p.kodeReq,
                Modifier.weight(1f),
                style = TipeIos.Utama,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(8.dp))
            LencanaIos(p.status.label, nadaStatus(p.status))
        }
        Text(
            listOfNotNull(waktuSingkat(p.createdAt), p.pembuatNama?.let { "Dibuat oleh: $it" }).joinToString(" · "),
            style = TipeIos.Catatan,
        )
        Spacer(Modifier.height(10.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .background(WarnaIos.Latar, UkuranIos.SudutBlok)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Text(
                "${p.items.size} item bahan baku",
                style = TipeIos.Kecil.copy(fontWeight = FontWeight.Medium),
            )
            Spacer(Modifier.height(4.dp))
            p.items.forEach { item ->
                val bahan = bahanMap[item.bahanBakuId]
                val diminta = qtyTersimpanTeks(item.qtyDiminta, bahan, item.satuan)
                val disetujui = item.qtyDisetujui?.let { qtyTersimpanTeks(it, bahan, item.satuan) }
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text(
                        item.namaBahan ?: item.bahanBakuId,
                        Modifier.weight(1f),
                        style = TipeIos.Catatan.copy(color = WarnaIos.Label),
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        buildString {
                            append(diminta)
                            if (disetujui != null && disetujui != diminta) {
                                append(" → $disetujui")
                            }
                        },
                        style = TipeIos.Catatan.copy(
                            color = if (disetujui != null && disetujui != diminta) NadaIos.AKSEN.teks else WarnaIos.Label,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                }
            }
        }
        if (p.status == StatusPermintaan.DITOLAK && !p.catatanKitchen.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Alasan Penolakan: ${p.catatanKitchen}",
                style = TipeIos.Catatan.copy(color = NadaIos.BAHAYA.teks),
            )
        }
        if (p.status == StatusPermintaan.DIBATALKAN && !p.catatanKitchen.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Catatan Pembatalan: ${p.catatanKitchen}",
                style = TipeIos.Catatan,
            )
        }
    }
}

// ================================================================== antrean

@Composable
private fun AntreanPersetujuan(state: PermintaanUiState, viewModel: PermintaanViewModel) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = UkuranIos.TepiLayar, end = UkuranIos.TepiLayar, top = 0.dp, bottom = UkuranIos.TepiLayar,
        ).denganRuangNav(),
        verticalArrangement = Arrangement.spacedBy(UkuranIos.JarakKartu),
    ) {
        item(key = "antrean-judul") {
            JudulSeksiIos("Antrean Persetujuan", Modifier.padding(top = 0.dp))
        }
        if (!state.bolehApprove) {
            item(key = "mode-pantau") {
                BlokNada(NadaIos.INFO) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(IkonIos.Visibility, null, tint = NadaIos.INFO.teks, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Mode pantau — tanpa hak persetujuan",
                            style = TipeIos.Catatan.copy(color = NadaIos.INFO.teks, fontWeight = FontWeight.SemiBold),
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Anda bisa melihat antrean permintaan, tetapi keputusan persetujuan ada di Gudang Pusat (kitchen).",
                        style = TipeIos.Catatan,
                    )
                }
            }
        }
        if (state.daftarReview.isEmpty()) {
            item(key = "antrean-kosong") {
                KeadaanKosong("Tidak ada permintaan bahan baku yang menunggu persetujuan.")
            }
        } else {
            items(state.daftarReview, key = { it.id }) { p ->
                KartuAntrean(
                    p = p,
                    bahanMap = state.bahanMap,
                    budget = state.budgetPerOutlet[p.outletId],
                    estimasi = state.estimasiPerPermintaan[p.id] ?: 0.0,
                    returMenunggu = state.returPerOutlet[p.outletId] ?: 0,
                    onBuka = { viewModel.bukaApprove(p) },
                )
            }
        }
    }
}

/**
 * Penggantian retur yang bisa menumpang pengiriman ini — cermin banner ungu di
 * `ApprovalModal.tsx`.
 *
 * Yang dicentang akan diterbitkan Surat Jalan Penggantinya (Rp 0) bersamaan dengan
 * persetujuan permintaan, sehingga satu rute kurir mengantar dua dokumen sekaligus.
 */
@Composable
private fun PanelPenggantiRetur(state: PermintaanUiState, viewModel: PermintaanViewModel) {
    BlokNada(NadaIos.UNGU) {
        Text(
            "Ada ${state.returMenunggu.size} Penggantian Retur untuk Outlet Ini",
            style = TipeIos.SubJudul.copy(color = NadaIos.UNGU.teks, fontWeight = FontWeight.SemiBold),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Fisik retur sudah ditimbang di Kitchen. Centang untuk otomatis menerbitkan " +
                "Surat Jalan Pengganti (Rp 0) bersamaan dengan pengiriman ini.",
            style = TipeIos.Kecil.copy(color = NadaIos.UNGU.teks.copy(alpha = 0.85f)),
        )
        state.returMenunggu.forEach { retur ->
            val ikut = retur.id in state.returDisertakan
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    // Garis tepi ungu hanya penanda "ikut dikirim", bukan bingkai.
                    .then(if (ikut) Modifier.border(2.dp, WarnaIos.Ungu, UkuranIos.SudutKontrol) else Modifier)
                    .background(WarnaIos.Kartu, UkuranIos.SudutKontrol)
                    .tekanIos({ viewModel.ubahReturDisertakan(retur.id, !ikut) }, skalaTekan = 0.98f)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = ikut,
                    onCheckedChange = { viewModel.ubahReturDisertakan(retur.id, it) },
                    colors = CheckboxDefaults.colors(checkedColor = WarnaIos.Ungu),
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        retur.nomorRetur,
                        style = TipeIos.Catatan.copy(color = NadaIos.UNGU.teks, fontWeight = FontWeight.Bold),
                    )
                    Text(
                        retur.items.joinToString(", ") { item ->
                            val qty = item.qtyDiterimaKitchen ?: item.qtyKlaim
                            "${item.namaBahan ?: "Bahan"}: ${formatAngkaStok(qty)} ${formatSatuan(item.satuan)}".trim()
                        },
                        style = TipeIos.Catatan.copy(color = WarnaIos.Label),
                    )
                    retur.catatanKitchen?.takeIf { it.isNotBlank() }?.let {
                        Text("\"$it\"", style = TipeIos.Kecil)
                    }
                }
            }
        }
    }
}

@Composable
private fun KartuAntrean(
    p: Permintaan,
    bahanMap: Map<String, BahanBaku>,
    budget: BudgetStatus?,
    estimasi: Double,
    returMenunggu: Int,
    onBuka: () -> Unit,
) {
    KartuIos(onKlik = onBuka) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LencanaIos("Menunggu persetujuan", NadaIos.PERINGATAN)
            Spacer(Modifier.width(8.dp))
            Text(
                p.kodeReq,
                Modifier.weight(1f),
                style = TipeIos.Catatan.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text("Periksa", style = TipeIos.Catatan.copy(color = WarnaIos.Aksen, fontWeight = FontWeight.SemiBold))
            Icon(IkonIos.ChevronRight, null, tint = WarnaIos.Aksen, modifier = Modifier.size(15.dp))
        }
        if (budget != null) {
            Spacer(Modifier.height(6.dp))
            BadgeBudget(status = budget, proyeksi = estimasi, ringkas = true)
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                p.outletName ?: p.outletId,
                style = TipeIos.Utama,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
            )
            p.pembuatNama?.let {
                Spacer(Modifier.width(8.dp))
                Text("Oleh: $it", style = TipeIos.Catatan)
            }
        }
        Text(
            "${p.items.size} jenis bahan baku · ${waktuSingkat(p.createdAt)}",
            style = TipeIos.Catatan,
        )
        if (p.omzetTarget > 0 || returMenunggu > 0) {
            Spacer(Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (p.omzetTarget > 0) {
                    LencanaIos("Potensi Omzet: ${formatRp(p.omzetTarget)}", NadaIos.SUKSES, titik = false)
                }
                // Penyetuju perlu tahu SEBELUM membuka kartu bahwa outlet ini punya
                // penggantian retur yang bisa menumpang — itu yang menentukan apakah
                // permintaan ini layak disetujui sekarang atau menunggu digabung.
                if (returMenunggu > 0) {
                    LencanaIos("Ada $returMenunggu Pengganti Retur", NadaIos.UNGU, titik = false)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .background(WarnaIos.Latar, UkuranIos.SudutBlok)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            p.items.take(3).forEach { item ->
                val bahan = bahanMap[item.bahanBakuId]
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text(
                        item.namaBahan ?: item.bahanBakuId,
                        Modifier.weight(1f),
                        style = TipeIos.Catatan.copy(color = WarnaIos.Label),
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        qtyTersimpanTeks(item.qtyDiminta, bahan, item.satuan),
                        style = TipeIos.Catatan.copy(color = WarnaIos.Label, fontWeight = FontWeight.SemiBold),
                    )
                }
            }
            if (p.items.size > 3) {
                Text("+${p.items.size - 3} item lainnya…", style = TipeIos.Kecil)
            }
        }
    }
}

// ============================================================== persetujuan

@Composable
private fun LayarPersetujuan(
    state: PermintaanUiState,
    p: Permintaan,
    viewModel: PermintaanViewModel,
) {
    var alasan by remember(p.id) { mutableStateOf("") }
    val adaLebih = viewModel.adaLebihStokGudang()

    Column(Modifier.fillMaxSize().background(WarnaIos.Latar)) {
        HeaderStok(
            judul = "Persetujuan Permintaan",
            subjudul = "${p.outletName ?: "Outlet"} · ${waktuSingkat(p.createdAt)}",
            onKembali = viewModel::tutupApprove,
        )
        state.pesan?.let { PitaPesan(it, false, viewModel::bersihkanPesan) }
        state.error?.let { PitaPesan(it, true, viewModel::bersihkanPesan) }

        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(UkuranIos.TepiLayar),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (p.targetJual.isNotEmpty()) {
                item(key = "target") {
                    KartuIos {
                        LabelSeksiIos("Target Penjualan")
                        Spacer(Modifier.height(6.dp))
                        p.targetJual.forEach { t ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                Text(
                                    "${formatAngkaStok(t.qty)}x ${t.nama}",
                                    Modifier.weight(1f),
                                    style = TipeIos.Catatan.copy(color = WarnaIos.Label),
                                )
                                Text(
                                    formatRp(t.omzet),
                                    style = TipeIos.Catatan.copy(color = WarnaIos.Label, fontWeight = FontWeight.SemiBold),
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Box(Modifier.fillMaxWidth().height(0.5.dp).background(WarnaIos.Pemisah))
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Estimasi Omzet", Modifier.weight(1f), style = TipeIos.SubJudul.copy(fontWeight = FontWeight.SemiBold))
                            Text(formatRp(p.omzetTarget), style = TipeIos.Utama.copy(color = NadaIos.SUKSES.teks))
                        }
                    }
                }
            }

            if (state.returMenunggu.isNotEmpty()) {
                item(key = "retur-menunggu") { PanelPenggantiRetur(state, viewModel) }
            }

            items(p.items, key = { it.bahanBakuId }) { item ->
                val bahan = state.bahanMap[item.bahanBakuId]
                val satuan = formatSatuan(bahan?.satuanPesan ?: item.satuan)
                val qty = state.qtySetuju[item.bahanBakuId] ?: 0L
                val gudangBesar = viewModel.stokGudangBesar(item.bahanBakuId)
                val lebih = gudangBesar != null && viewModel.qtySetujuBase(item.bahanBakuId) > gudangBesar
                val tepi = if (lebih) {
                    Modifier.border(1.5.dp, WarnaIos.Oranye.copy(alpha = 0.6f), UkuranIos.SudutKartu)
                } else {
                    Modifier
                }
                KartuIos(tepi, padding = PaddingValues(14.dp)) {
                    Text(
                        item.namaBahan ?: item.bahanBakuId,
                        style = TipeIos.Utama.copy(fontSize = 16.sp),
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    state.kebutuhanTarget[item.bahanBakuId]?.let { kebutuhan ->
                        Text(
                            "HPP Penggunaan: ${String.format(java.util.Locale.US, "%.2f", kebutuhan)} ${formatSatuan(item.satuan)} · " +
                                "Pembulatan: ${ceil(kebutuhan).toLong()} ${formatSatuan(item.satuan)}",
                            style = TipeIos.Kecil,
                        )
                    }
                    Text(
                        "Diminta: ${qtyTersimpanTeks(item.qtyDiminta, bahan, item.satuan)}",
                        style = TipeIos.Catatan.copy(color = NadaIos.AKSEN.teks, fontWeight = FontWeight.SemiBold),
                    )
                    val ccOutlet = state.stokOutlet[item.bahanBakuId]
                    val ccGudang = state.stokGudang[item.bahanBakuId]
                    when {
                        state.memuatCrosscheck -> Text("Memuat stok…", style = TipeIos.Kecil)
                        ccOutlet == null && ccGudang == null ->
                            Text("(Stok tidak dapat dimuat)", style = TipeIos.Kecil)
                        else -> {
                            val meta = bahan?.meta
                            val outletTeks = ccOutlet?.let {
                                if (meta != null) formatTriUnitAdaptif(it.currentQty, it.saldoIsGram, meta)
                                else "${formatAngkaStok(it.currentQty)} ${formatSatuan(item.satuan)}"
                            } ?: "-"
                            val gudangTeks = ccGudang?.let {
                                if (meta != null) formatTriUnitAdaptif(it.currentQty, it.saldoIsGram, meta)
                                else "${formatAngkaStok(it.currentQty)} ${formatSatuan(item.satuan)}"
                            } ?: "-"
                            Text(
                                "Stok Outlet: $outletTeks | Stok Gudang: $gudangTeks",
                                style = TipeIos.Kecil,
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (lebih) {
                            Icon(IkonIos.WarningAmber, null, tint = WarnaIos.Oranye, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                        }
                        Box(Modifier.weight(1f)) {
                            StepperQty(
                                qty = qty,
                                satuan = satuan,
                                onMinus = { viewModel.ubahQtySetuju(item.bahanBakuId, -1L) },
                                onPlus = { viewModel.ubahQtySetuju(item.bahanBakuId, 1L) },
                                onSet = { viewModel.setQtySetuju(item.bahanBakuId, it) },
                                diSorot = lebih,
                            )
                        }
                    }
                }
            }

            item(key = "catatan-nol") {
                Text(
                    "Set qty 0 untuk menolak item tertentu",
                    Modifier.padding(horizontal = 4.dp),
                    style = TipeIos.Catatan,
                )
            }

            val totalNilai = state.estimasiSetuju.totalNilai
            if (totalNilai > 0) {
                item(key = "total-nilai") {
                    KartuIos {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Total Nilai Permintaan", style = TipeIos.SubJudul.copy(fontWeight = FontWeight.SemiBold))
                                Text(
                                    "Tahap Developer (Bisa Diabaikan)",
                                    style = TipeIos.Kecil.copy(color = NadaIos.PERINGATAN.teks, fontSize = 11.sp),
                                )
                            }
                            Text(formatRp(totalNilai), style = TipeIos.Angka)
                        }
                    }
                }
            }

            if (state.budgetPerOutlet[p.outletId] != null) {
                item(key = "budget-approve") {
                    BadgeBudget(
                        status = state.budgetPerOutlet[p.outletId],
                        proyeksi = state.estimasiSetuju.totalNilai,
                    )
                }
            }

            if (adaLebih) {
                item(key = "peringatan-gudang") {
                    BlokNada(NadaIos.PERINGATAN) {
                        Text(
                            "⚠️ Beberapa item melebihi stok gudang. Mohon periksa kembali.",
                            style = TipeIos.Catatan.copy(color = NadaIos.PERINGATAN.teks, fontWeight = FontWeight.SemiBold),
                        )
                    }
                }
            }

            if (!state.bolehApprove) {
                item(key = "pantau-info") {
                    BlokNada(NadaIos.INFO) {
                        Text(
                            "👁️ Anda hanya memantau. Keputusan setujui/tolak ada di Gudang Pusat (kitchen), " +
                                "admin, atau owner — sebab persetujuan langsung menerbitkan surat jalan.",
                            style = TipeIos.Catatan,
                        )
                    }
                }
            }

            item(key = "alasan") {
                OutlinedTextField(
                    value = alasan,
                    onValueChange = { alasan = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text("Alasan penolakan (wajib jika menolak seluruh permintaan)", color = WarnaIos.Abu)
                    },
                    singleLine = false,
                    enabled = !state.mengirim,
                    shape = UkuranIos.SudutKontrol,
                    colors = warnaKolomIos(),
                )
            }
        }

        Surface(color = WarnaIos.Kartu, shadowElevation = 8.dp) {
            Row(
                Modifier.fillMaxWidth().navigationBarsPaddingKaca().padding(UkuranIos.TepiLayar),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TombolAksi(
                    "Batal",
                    viewModel::tutupApprove,
                    Modifier.weight(1f),
                    warna = WarnaIos.AbuGelap,
                    terisi = false,
                    aktif = !state.mengirim,
                )
                TombolAksi(
                    "Tolak",
                    { viewModel.tolak(alasan) },
                    Modifier.weight(1f),
                    warna = WarnaIos.Merah,
                    terisi = false,
                    aktif = !state.mengirim && state.bolehApprove,
                )
                TombolAksi(
                    if (state.mengirim) "Memproses…" else "Setujui",
                    viewModel::setujui,
                    Modifier.weight(1f),
                    warna = WarnaIos.Hijau,
                    aktif = !state.mengirim && state.bolehApprove,
                )
            }
        }
    }
}

// ================================================================== dialog

/** Tombol konfirmasi dialog bergaya iOS: isian aksen, sudut kontrol. */
@Composable
private fun TombolDialogUtama(teks: String, onKlik: () -> Unit, aktif: Boolean = true) {
    Button(
        onClick = onKlik,
        enabled = aktif,
        colors = ButtonDefaults.buttonColors(containerColor = WarnaIos.Aksen),
        shape = UkuranIos.SudutKontrol,
    ) { Text(teks, fontWeight = FontWeight.SemiBold) }
}

/** Tombol batal dialog: garis hairline, teks abu. */
@Composable
private fun TombolDialogKedua(teks: String, onKlik: () -> Unit, aktif: Boolean = true) {
    OutlinedButton(
        onClick = onKlik,
        enabled = aktif,
        shape = UkuranIos.SudutKontrol,
        border = BorderStroke(1.dp, WarnaIos.Pemisah),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = WarnaIos.LabelKedua),
    ) { Text(teks, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun DialogNudge(state: PermintaanUiState, viewModel: PermintaanViewModel) {
    AlertDialog(
        onDismissRequest = viewModel::tutupNudge,
        containerColor = WarnaIos.Kartu,
        shape = UkuranIos.SudutKartu,
        title = {
            Text("Masih ada permintaan yang menunggu", style = TipeIos.Judul3)
        },
        text = {
            Text(
                "Anda memiliki ${state.pendingItemIds.size} item bahan baku lain yang masih menunggu " +
                    "persetujuan kitchen. Mau gabungkan dengan bahan lain dulu atau kirim sekarang?",
                style = TipeIos.SubJudul,
            )
        },
        confirmButton = {
            TombolDialogUtama("Kirim Sekarang", viewModel::lanjutKirimDariNudge)
        },
        dismissButton = {
            TombolDialogKedua("Tambah Dulu", viewModel::tambahDuluDariNudge)
        },
    )
}

/**
 * Riwayat pengajuan top-up saldo — cermin `OutletTopUpRequests` web, termasuk tombol
 * persetujuan dua tahap (AM lalu Finance) bagi role yang berwenang.
 */
@Composable
private fun KartuTopUp(state: PermintaanUiState, viewModel: PermintaanViewModel) {
    KartuIos(padding = PaddingValues(14.dp)) {
        LabelSeksiIos("Permintaan Top-Up Saldo")
        state.daftarTopUp.forEach { req ->
            val nada = nadaTopUp(req.status)
            Spacer(Modifier.height(10.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(WarnaIos.Latar, UkuranIos.SudutBlok)
                    .padding(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        formatRp(req.nominal),
                        Modifier.weight(1f),
                        style = TipeIos.Utama,
                    )
                    LencanaIos(req.status.label, nada)
                }
                Text(
                    "${req.kategoriPeriode.replaceFirstChar { it.uppercase() }} · " +
                        "${waktuSingkat(req.createdAt)} · oleh ${req.pemohonNama ?: "-"}",
                    style = TipeIos.Catatan,
                )
                req.amNama?.let { Text("Disetujui AM: $it", style = TipeIos.Catatan) }
                req.financeNama?.let { Text("Disetujui Finance: $it", style = TipeIos.Catatan) }
                if (!req.catatan.isNullOrBlank()) {
                    Text("Catatan: ${req.catatan}", style = TipeIos.Catatan.copy(color = NadaIos.BAHAYA.teks))
                }

                val bolehAm = state.bolehApproveAm && req.status == StatusTopUp.MENUNGGU_AM
                val bolehFinance = state.bolehApproveFinance && req.status == StatusTopUp.MENUNGGU_FINANCE
                if (bolehAm || bolehFinance) {
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TombolAksi(
                            "Tolak",
                            { viewModel.prosesTopUp(req.id, "reject") },
                            Modifier.weight(1f),
                            warna = WarnaIos.Merah,
                            terisi = false,
                            aktif = !state.memprosesTopUp,
                        )
                        TombolAksi(
                            if (bolehAm) "Setujui (AM)" else "Setujui (Finance)",
                            {
                                viewModel.prosesTopUp(
                                    req.id,
                                    if (bolehAm) "approve_am" else "approve_finance",
                                )
                            },
                            Modifier.weight(1f),
                            warna = if (bolehAm) WarnaIos.Aksen else WarnaIos.Hijau,
                            aktif = !state.memprosesTopUp,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Form pengajuan top-up — cermin `RequestTopUpModal` web, termasuk batas maksimal
 * (plafon − sisa) dan pilihan kategori weekday/weekend.
 */
@Composable
private fun DialogTopUp(state: PermintaanUiState, viewModel: PermintaanViewModel) {
    val budget = state.budget ?: return
    val maks = Budget.maksTopUp(budget.nominal, budget.sisa)
    var nominal by remember { mutableStateOf("") }
    var kategori by remember { mutableStateOf("weekday") }

    AlertDialog(
        onDismissRequest = { if (!state.memprosesTopUp) viewModel.tutupTopUp() },
        containerColor = WarnaIos.Kartu,
        shape = UkuranIos.SudutKartu,
        title = { Text("Form Pengajuan Top-Up", style = TipeIos.Judul3) },
        text = {
            Column {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(WarnaIos.Latar, UkuranIos.SudutBlok)
                        .padding(12.dp),
                ) {
                    BarisNilai("Plafon Maksimal", formatRp(budget.nominal))
                    BarisNilai("Sisa Saldo", formatRp(budget.sisa))
                    Spacer(Modifier.height(4.dp))
                    BarisNilai("Maksimal Pengajuan", formatRp(maks), tebal = true)
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = nominal,
                    onValueChange = { masukan ->
                        // Hanya digit, lalu dijepit ke batas maksimal seperti web.
                        val bersih = masukan.filter { it.isDigit() }.take(12)
                        val angka = bersih.toLongOrNull()
                        nominal = when {
                            bersih.isEmpty() -> ""
                            angka != null && angka > maks.toLong() -> maks.toLong().toString()
                            else -> bersih
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Masukkan nominal…", color = WarnaIos.Abu) },
                    prefix = { Text("Rp ", fontWeight = FontWeight.SemiBold) },
                    singleLine = true,
                    enabled = !state.memprosesTopUp,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = UkuranIos.SudutKontrol,
                    colors = warnaKolomIos(),
                )
                Spacer(Modifier.height(12.dp))
                LabelSeksiIos("Kategori Periode")
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("weekday" to "Weekday", "weekend" to "Weekend").forEach { (nilai, label) ->
                        ChipKategori(label = label, aktif = kategori == nilai, warnaAktif = WarnaIos.Aksen) {
                            kategori = nilai
                        }
                    }
                }
            }
        },
        confirmButton = {
            TombolDialogUtama(
                if (state.memprosesTopUp) "Memproses…" else "Kirim",
                { viewModel.ajukanTopUp(nominal.toLongOrNull() ?: 0L, kategori) },
                aktif = !state.memprosesTopUp && nominal.isNotEmpty(),
            )
        },
        dismissButton = {
            TombolDialogKedua("Batal", viewModel::tutupTopUp, aktif = !state.memprosesTopUp)
        },
    )
}

@Composable
private fun BarisNilai(label: String, nilai: String, tebal: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            label,
            Modifier.weight(1f),
            style = TipeIos.Catatan.copy(
                color = if (tebal) WarnaIos.Label else WarnaIos.LabelKedua,
                fontWeight = if (tebal) FontWeight.SemiBold else FontWeight.Normal,
            ),
        )
        Text(
            nilai,
            style = TipeIos.Catatan.copy(
                color = if (tebal) NadaIos.AKSEN.teks else WarnaIos.Label,
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}

@Composable
private fun DialogKonfirmasi(state: PermintaanUiState, viewModel: PermintaanViewModel) {
    val items = state.keranjangItems
    val estimasi = state.estimasi
    AlertDialog(
        onDismissRequest = { if (!state.mengirim) viewModel.tutupKonfirmasi() },
        containerColor = WarnaIos.Kartu,
        shape = UkuranIos.SudutKartu,
        title = { Text("Kirim Permintaan Bahan?", style = TipeIos.Judul3) },
        text = {
            Column {
                Text(
                    "Total ${items.size} item bahan baku akan diajukan ke Kitchen / Gudang.",
                    style = TipeIos.SubJudul,
                )
                Spacer(Modifier.height(10.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(WarnaIos.Latar, UkuranIos.SudutBlok)
                        .padding(12.dp),
                ) {
                    items.take(8).forEach { b ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            Text(
                                b.bahan.nama,
                                Modifier.weight(1f),
                                style = TipeIos.Catatan.copy(color = WarnaIos.Label),
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                "${b.qty} ${formatSatuan(b.bahan.satuanPesan)}",
                                style = TipeIos.Catatan.copy(color = WarnaIos.Label, fontWeight = FontWeight.SemiBold),
                            )
                        }
                    }
                    if (items.size > 8) {
                        Text("+${items.size - 8} bahan lain", style = TipeIos.Kecil)
                    }
                }
                if (estimasi.totalNilai > 0) {
                    Spacer(Modifier.height(8.dp))
                    estimasi.kategoriNilai.forEach { (kat, nilai) ->
                        Row(Modifier.fillMaxWidth()) {
                            Text(kat, Modifier.weight(1f), style = TipeIos.Kecil)
                            Text(formatRp(nilai), style = TipeIos.Kecil.copy(color = WarnaIos.Label, fontWeight = FontWeight.SemiBold))
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        Text("Total Estimasi", Modifier.weight(1f), style = TipeIos.Catatan.copy(color = WarnaIos.Label, fontWeight = FontWeight.SemiBold))
                        Text(
                            formatRp(estimasi.totalNilai),
                            style = TipeIos.Catatan.copy(color = NadaIos.AKSEN.teks, fontWeight = FontWeight.Bold),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TombolDialogUtama(
                if (state.mengirim) "Mengirim…" else "Ya, Kirim Sekarang",
                viewModel::kirimPermintaan,
                aktif = !state.mengirim,
            )
        },
        dismissButton = {
            TombolDialogKedua("Batal", viewModel::tutupKonfirmasi, aktif = !state.mengirim)
        },
    )
}
