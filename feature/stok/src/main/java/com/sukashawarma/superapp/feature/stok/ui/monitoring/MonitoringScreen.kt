package com.sukashawarma.superapp.feature.stok.ui.monitoring

import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.core.ui.ios.AngkaIos
import com.sukashawarma.superapp.core.ui.ios.BlokAngkaIos
import com.sukashawarma.superapp.core.ui.ios.JudulSeksiIos
import com.sukashawarma.superapp.core.ui.ios.KartuIos
import com.sukashawarma.superapp.core.ui.ios.KolomCariIos
import com.sukashawarma.superapp.core.ui.ios.LencanaIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.PetakStatIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.TombolBundarIos
import com.sukashawarma.superapp.core.ui.ios.TombolKapsulIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.kaca.denganRuangNav
import androidx.compose.foundation.background
import com.sukashawarma.superapp.feature.stok.ui.label
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.sukashawarma.superapp.core.ui.SukaDropdownHeader
import com.sukashawarma.superapp.core.ui.SukaDropdownMenu
import com.sukashawarma.superapp.core.ui.SukaDropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.feature.stok.data.model.MonitoringRow
import com.sukashawarma.superapp.feature.stok.data.model.OutletRingkas
import com.sukashawarma.superapp.feature.stok.domain.KategoriStok
import com.sukashawarma.superapp.feature.stok.domain.StokStatus
import com.sukashawarma.superapp.feature.stok.domain.decomposeTriUnit
import com.sukashawarma.superapp.feature.stok.domain.formatAngkaStok
import com.sukashawarma.superapp.feature.stok.domain.formatSatuan
import com.sukashawarma.superapp.feature.stok.domain.lokasiPenyimpanan
import com.sukashawarma.superapp.feature.stok.ui.KeadaanGagal
import com.sukashawarma.superapp.feature.stok.ui.KeadaanKosong
import com.sukashawarma.superapp.feature.stok.ui.KeadaanTidakBerhak
import com.sukashawarma.superapp.feature.stok.ui.MemuatPenuh
import com.sukashawarma.superapp.presentation.theme.SukaOnSurface
import com.sukashawarma.superapp.presentation.theme.SukaOnSurfaceVariant
import com.sukashawarma.superapp.presentation.theme.SukaSurface
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.RealtimeTables

@Composable
fun MonitoringScreen(
    onKeluar: () -> Unit,
    onBukaBahan: (outletId: String, bahanId: String, nama: String) -> Unit,
    onBukaTransfer: () -> Unit,
    outletAwalId: String? = null,
    // Sengaja tidak masuk kunci ViewModel: nilainya dikosongkan setelah dipakai,
    // dan kunci yang berubah akan membuang filter yang baru saja dinyalakan.
    filterAwal: FilterKpi = FilterKpi.SEMUA,
    viewModel: MonitoringViewModel = viewModel(key = "monitoring|$outletAwalId") {
        MonitoringViewModel(outletAwalId, filterAwal)
    },
) {
    val state by viewModel.state.collectAsState()
    RealtimeRefresh(RealtimeTables.STOK_BALANCE, RealtimeTables.BAHAN_BAKU, jedaMinimumMs = 30_000L) { viewModel.muatAwal() }

    Column(Modifier.fillMaxSize().background(WarnaIos.Latar)) {
        Header(
            state = state,
            onKeluar = onKeluar,
            onSegarkan = viewModel::segarkan,
            onPilihOutlet = viewModel::pilihOutlet,
            onBukaTransfer = onBukaTransfer,
        )

        when {
            state.tidakBerhak -> KeadaanTidakBerhak(
                "Akun Anda belum terhubung dengan outlet mana pun. Hubungi admin atau regional manager."
            )
            state.memuat && state.outlets.isEmpty() -> MemuatPenuh()
            state.error != null -> KeadaanGagal(state.error!!, viewModel::muatAwal)
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = UkuranIos.TepiLayar, end = UkuranIos.TepiLayar, top = 12.dp, bottom = 24.dp).denganRuangNav(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "kpi") { FilterPillsBar(state, viewModel::tekanKartu) }
                item(key = "toolbar") {
                    BarisAlat(state, viewModel::ubahCari, viewModel::ubahUrutan)
                }

                if (state.kosongSetelahDisaring) {
                    item(key = "kosong") {
                        KeadaanKosong(
                            if (state.cari.isNotBlank()) "Bahan baku tidak ditemukan."
                            else "Tidak ada bahan baku pada filter ini."
                        )
                    }
                } else {
                    state.perKategori.forEach { (kategori, isi) ->
                        item(key = "judul-${kategori.kunci}") {
                            JudulKategori(kategori, isi.size)
                        }
                        items(isi, key = { "${kategori.kunci}|${it.bahanBakuId}" }) { row ->
                            KartuBahan(row, state.status(row), onBukaBahan)
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------- gaya iOS

/** Nada status versi iOS; hanya di layar ini, supaya [StokStatus.warna] di layar lain tetap. */
private fun StokStatus.nadaIos(): NadaIos = when (this) {
    StokStatus.OK -> NadaIos.SUKSES
    StokStatus.WARNING -> NadaIos.PERINGATAN
    StokStatus.BELOW -> NadaIos.BAHAYA
    StokStatus.UNKNOWN -> NadaIos.NETRAL
}

// ------------------------------------------------------------------------ header

@Composable
private fun Header(
    state: MonitoringUiState,
    onKeluar: () -> Unit,
    onSegarkan: () -> Unit,
    onPilihOutlet: (OutletRingkas) -> Unit,
    onBukaTransfer: () -> Unit,
) {
    var menuTerbuka by remember { mutableStateOf(false) }

    Surface(color = WarnaIos.Latar, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TombolBundarIos(IkonIos.ArrowBack, "Kembali", onKeluar)

            Spacer(Modifier.width(8.dp))

            // Pemilih outlet bergaya "pull-down button" iOS: kapsul abu dengan
            // chevron atas-bawah, bukan kotak bergaris.
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Surface(
                    shape = UkuranIos.SudutKapsul,
                    color = WarnaIos.Isian,
                    modifier = Modifier.heightIn(min = 36.dp),
                ) {
                    Row(
                        Modifier
                            .let { m -> if (state.tampilkanPemilihOutlet) m.clickable { menuTerbuka = true } else m }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            state.outletTerpilih?.name ?: "Outlet",
                            style = TipeIos.Keterangan.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (state.tampilkanPemilihOutlet) {
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                IkonIos.ArrowDropDown,
                                contentDescription = "Ganti outlet",
                                tint = WarnaIos.Aksen,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }

                SukaDropdownMenu(
                    expanded = menuTerbuka,
                    onDismissRequest = { menuTerbuka = false },
                ) {
                    SukaDropdownHeader(title = "PILIH OUTLET", onClose = { menuTerbuka = false })
                    state.outlets.forEach { outlet ->
                        SukaDropdownMenuItem(
                            text = outlet.name,
                            selected = (state.outletTerpilih?.id == outlet.id),
                            onClick = { menuTerbuka = false; onPilihOutlet(outlet) },
                        )
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            if (state.tampilkanPemilihOutlet) {
                TombolBundarIos(IkonIos.SwapHoriz, "Saran transfer", onBukaTransfer)
                Spacer(Modifier.width(8.dp))
            }
            TombolBundarIos(IkonIos.Refresh, "Segarkan", onSegarkan)
        }
    }
}

// ------------------------------------------------------------------ petak KPI

/**
 * Empat petak ringkasan ala "smart list" aplikasi Pengingat iOS. Perilakunya sama
 * dengan pil lama — Aman hanya informasi, tidak bisa ditekan.
 */
@Composable
private fun FilterPillsBar(
    state: MonitoringUiState,
    onTekan: (FilterKpi) -> Unit,
) {
    val semuaAktif = state.filter == FilterKpi.SEMUA
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PetakStatIos(
                label = "Semua",
                nilai = state.semua.size.toString(),
                ikon = IkonIos.Inventory2,
                warna = WarnaIos.AbuGelap,
                aktif = semuaAktif,
                // Sama seperti pil lama: menekan "Semua" mematikan filter yang aktif.
                onKlik = { if (!semuaAktif) onTekan(state.filter) },
                modifier = Modifier.weight(1f),
            )
            PetakStatIos(
                label = "Kritis",
                nilai = state.jumlahKritis.toString(),
                ikon = IkonIos.WarningAmber,
                warna = WarnaIos.Merah,
                aktif = state.filter == FilterKpi.KRITIS,
                onKlik = { onTekan(FilterKpi.KRITIS) },
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PetakStatIos(
                label = "Selisih",
                nilai = state.jumlahSelisih.toString(),
                ikon = IkonIos.ImportExport,
                warna = WarnaIos.Oranye,
                aktif = state.filter == FilterKpi.SELISIH,
                onKlik = { onTekan(FilterKpi.SELISIH) },
                modifier = Modifier.weight(1f),
            )
            PetakStatIos(
                label = "Aman",
                nilai = state.jumlahAman.toString(),
                ikon = IkonIos.Check,
                warna = WarnaIos.Hijau,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// --------------------------------------------------------------------- toolbar

@Composable
private fun BarisAlat(
    state: MonitoringUiState,
    onCari: (String) -> Unit,
    onUrutan: (UrutanStok) -> Unit,
) {
    var menuUrutan by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().padding(top = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        KolomCariIos(state.cari, onCari, Modifier.weight(1f), placeholder = "Cari bahan baku")

        Box {
            TombolKapsulIos(state.urutan.label, { menuUrutan = true }, ikon = IkonIos.Tune)
            SukaDropdownMenu(expanded = menuUrutan, onDismissRequest = { menuUrutan = false }) {
                SukaDropdownHeader(title = "URUTKAN BERDASARKAN", onClose = { menuUrutan = false })
                UrutanStok.entries.forEach { u ->
                    SukaDropdownMenuItem(
                        text = u.label,
                        selected = (state.urutan == u),
                        onClick = { menuUrutan = false; onUrutan(u) },
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------ isi daftar

@Composable
private fun JudulKategori(kategori: KategoriStok, jumlah: Int) {
    JudulSeksiIos("${kategori.emoji}  ${kategori.label}", keterangan = "$jumlah item")
}

@Composable
private fun KartuBahan(
    row: MonitoringRow,
    status: StokStatus,
    onBuka: (String, String, String) -> Unit,
) {
    val tri = decomposeTriUnit(
        qty = row.currentQty,
        saldoIsGram = row.saldoIsGram,
        satuanTengah = row.meta.satuanTengah,
        faktorTengah = row.meta.faktorTengah,
        satuanKecil = row.meta.satuanKecil,
        faktorTampilan = row.meta.faktorTampilan,
    )

    KartuIos(onKlik = { onBuka(row.outletId, row.bahanBakuId, row.itemName) }) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(row.itemName, style = TipeIos.Utama, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(IkonIos.LocationOn, null, tint = WarnaIos.Abu, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(
                        lokasiPenyimpanan(row.kategori, row.itemName),
                        style = TipeIos.Catatan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Text("  ·  ", style = TipeIos.Catatan.copy(color = WarnaIos.LabelKetiga))
                    Text(
                        "Min ${formatAngkaStok(row.threshold ?: 0.0)} ${formatSatuan(row.satuan)}",
                        style = TipeIos.Catatan,
                        maxLines = 1,
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            LencanaIos(status.label(), status.nadaIos())
        }

        Spacer(Modifier.height(14.dp))

        BlokAngkaIos(
            listOf(
                AngkaIos("Besar", formatAngkaStok(tri.besar), formatSatuan(row.meta.satuan), tri.besar < 0),
                AngkaIos(
                    "Tengah",
                    if (row.meta.satuanTengah != null) formatAngkaStok(tri.tengah) else "—",
                    if (row.meta.satuanTengah != null) formatSatuan(row.meta.satuanTengah) else "",
                    tri.tengah < 0,
                ),
                AngkaIos(
                    "Kecil",
                    if (row.meta.satuanKecil != null) formatAngkaStok(tri.kecil) else "—",
                    if (row.meta.satuanKecil != null) formatSatuan(row.meta.satuanKecil) else "",
                    tri.kecil < 0,
                ),
            ),
        )
    }
}
