package com.sukashawarma.superapp.feature.stok.ui.monitoring

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import com.sukashawarma.superapp.core.ui.SukaDropdownHeader
import com.sukashawarma.superapp.core.ui.SukaDropdownMenu
import com.sukashawarma.superapp.core.ui.SukaDropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.sukashawarma.superapp.feature.stok.ui.StatusBadge
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
    viewModel: MonitoringViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    RealtimeRefresh(RealtimeTables.STOK_BALANCE, RealtimeTables.LEDGER, RealtimeTables.BAHAN_BAKU) { viewModel.muatAwal() }

    Column(Modifier.fillMaxSize().background(Color(0xFFF8FAFC))) {
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
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
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

    Surface(
        color = Color.White,
        shadowElevation = 0.5.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier
                .statusBarsPadding()
                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onKeluar,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Kembali",
                        tint = Color(0xFF1E293B)
                    )
                }

                Spacer(Modifier.width(8.dp))

                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.heightIn(min = 38.dp)
                    ) {
                        Row(
                            Modifier
                                .let { m ->
                                    if (state.tampilkanPemilihOutlet) {
                                        m.clickable { menuTerbuka = true }
                                    } else m
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                state.outletTerpilih?.name?.uppercase() ?: "OUTLET",
                                color = Color(0xFF0F172A),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (state.tampilkanPemilihOutlet) {
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = "Ganti outlet",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    SukaDropdownMenu(
                        expanded = menuTerbuka,
                        onDismissRequest = { menuTerbuka = false }
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
                    IconButton(
                        onClick = onBukaTransfer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.SwapHoriz,
                            contentDescription = "Saran transfer",
                            tint = Color(0xFF1E293B)
                        )
                    }
                }

                IconButton(
                    onClick = onSegarkan,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Segarkan",
                        tint = Color(0xFF1E293B)
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------------------- filter pills

@Composable
private fun FilterPillsBar(
    state: MonitoringUiState,
    onTekan: (FilterKpi) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val semuaAktif = state.filter == FilterKpi.SEMUA
        FilterChipPill(
            label = "Semua",
            count = null,
            aktif = semuaAktif,
            warnaAksen = Color(0xFF0F172A),
            onClick = {
                if (!semuaAktif) {
                    onTekan(state.filter)
                }
            },
            modifier = Modifier.weight(1f)
        )

        val kritisAktif = state.filter == FilterKpi.KRITIS
        FilterChipPill(
            label = "Kritis",
            count = state.jumlahKritis,
            aktif = kritisAktif,
            warnaAksen = Color(0xFFDC2626),
            warnaBgMuted = Color(0xFFFEE2E2),
            onClick = { onTekan(FilterKpi.KRITIS) },
            modifier = Modifier.weight(1.1f)
        )

        val selisihAktif = state.filter == FilterKpi.SELISIH
        FilterChipPill(
            label = "Selisih",
            count = state.jumlahSelisih,
            aktif = selisihAktif,
            warnaAksen = Color(0xFFD97706),
            warnaBgMuted = Color(0xFFFEF3C7),
            onClick = { onTekan(FilterKpi.SELISIH) },
            modifier = Modifier.weight(1.1f)
        )

        FilterChipPill(
            label = "Aman",
            count = state.jumlahAman,
            aktif = false,
            warnaAksen = Color(0xFF16A34A),
            warnaBgMuted = Color(0xFFDCFCE7),
            dapatDitekan = false,
            onClick = {},
            modifier = Modifier.weight(1.1f)
        )
    }
}

@Composable
private fun FilterChipPill(
    label: String,
    count: Int?,
    aktif: Boolean,
    warnaAksen: Color,
    warnaBgMuted: Color = Color(0xFFF1F5F9),
    dapatDitekan: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = when {
        aktif -> warnaAksen
        else -> warnaBgMuted
    }
    val fg = when {
        aktif -> Color.White
        warnaAksen == Color(0xFF0F172A) -> Color(0xFF334155)
        else -> warnaAksen
    }

    Surface(
        onClick = onClick,
        enabled = dapatDitekan,
        shape = RoundedCornerShape(50),
        color = bg,
        border = if (aktif) null else BorderStroke(1.dp, warnaAksen.copy(alpha = 0.18f)),
        modifier = modifier.height(38.dp)
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                label,
                color = fg,
                fontSize = 12.sp,
                fontWeight = if (aktif) FontWeight.ExtraBold else FontWeight.Bold,
                maxLines = 1,
            )
            if (count != null) {
                Spacer(Modifier.width(5.dp))
                Surface(
                    shape = CircleShape,
                    color = if (aktif) Color.White.copy(alpha = 0.25f) else warnaAksen.copy(alpha = 0.15f),
                    modifier = Modifier.size(18.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            count.toString(),
                            color = fg,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

// --------------------------------------------------------------------- toolbar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BarisAlat(
    state: MonitoringUiState,
    onCari: (String) -> Unit,
    onUrutan: (UrutanStok) -> Unit,
) {
    var menuUrutan by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = state.cari,
            onValueChange = onCari,
            modifier = Modifier.weight(1f).height(48.dp),
            placeholder = {
                Text(
                    "Cari bahan baku…",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            leadingIcon = {
                Icon(Icons.Default.Search, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
            },
            trailingIcon = {
                if (state.cari.isNotEmpty()) {
                    IconButton(onClick = { onCari("") }) {
                        Icon(Icons.Default.Close, "Hapus pencarian", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = Color(0xFFEA580C),
                unfocusedBorderColor = Color(0xFFE2E8F0),
            ),
        )

        Box {
            Surface(
                onClick = { menuUrutan = true },
                modifier = Modifier.height(48.dp),
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            ) {
                Row(
                    Modifier.fillMaxHeight().padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        state.urutan.label,
                        color = Color(0xFF1E293B),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                    Spacer(Modifier.width(2.dp))
                    Icon(Icons.Default.ArrowDropDown, null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                }
            }
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
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp, start = 2.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "${kategori.emoji} ${kategori.label.uppercase()}",
            Modifier.weight(1f),
            color = Color(0xFF0F172A),
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.6.sp,
        )
        Surface(
            shape = RoundedCornerShape(50),
            color = Color(0xFFF1F5F9),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Text(
                "$jumlah Item",
                Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                color = Color(0xFF64748B),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
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

    Surface(
        Modifier
            .fillMaxWidth()
            .clickable { onBuka(row.outletId, row.bahanBakuId, row.itemName) },
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
        shadowElevation = 1.dp,
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        row.itemName,
                        color = Color(0xFF0F172A),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn, null,
                            tint = Color(0xFF94A3B8), modifier = Modifier.size(12.dp),
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            lokasiPenyimpanan(row.kategori, row.itemName),
                            color = Color(0xFF64748B),
                            fontSize = 11.sp,
                        )
                        Text("  ·  ", color = Color(0xFFCBD5E1), fontSize = 11.sp)
                        Text(
                            "Min: ",
                            color = Color(0xFF64748B),
                            fontSize = 11.sp,
                        )
                        Text(
                            "${formatAngkaStok(row.threshold ?: 0.0)} ${formatSatuan(row.satuan)}",
                            color = Color(0xFF334155),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                StatusBadge(status)
            }

            Spacer(Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFBF9F5),
                border = BorderStroke(1.dp, Color(0xFFF1EFE9)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    KolomSatuan(
                        "Sat. Besar",
                        formatAngkaStok(tri.besar),
                        formatSatuan(row.meta.satuan),
                        tri.besar < 0,
                        Modifier.weight(1f)
                    )

                    Box(
                        Modifier
                            .width(1.dp)
                            .height(26.dp)
                            .background(Color(0xFFE5E2DC))
                    )

                    KolomSatuan(
                        "Sat. Tengah",
                        if (row.meta.satuanTengah != null) formatAngkaStok(tri.tengah) else "—",
                        if (row.meta.satuanTengah != null) formatSatuan(row.meta.satuanTengah) else "",
                        tri.tengah < 0,
                        Modifier.weight(1f),
                    )

                    Box(
                        Modifier
                            .width(1.dp)
                            .height(26.dp)
                            .background(Color(0xFFE5E2DC))
                    )

                    KolomSatuan(
                        "Sat. Kecil",
                        if (row.meta.satuanKecil != null) formatAngkaStok(tri.kecil) else "—",
                        if (row.meta.satuanKecil != null) formatSatuan(row.meta.satuanKecil) else "",
                        tri.kecil < 0,
                        Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun KolomSatuan(
    label: String,
    angka: String,
    satuan: String,
    isMinus: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label.uppercase(),
            color = Color(0xFF64748B),
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(3.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                angka,
                color = if (isMinus) Color(0xFFDC2626) else Color(0xFF0F172A),
                fontSize = 14.sp,
                fontWeight = FontWeight.Black
            )
            if (satuan.isNotEmpty() && angka != "—") {
                Spacer(Modifier.width(3.dp))
                Text(
                    satuan,
                    color = if (isMinus) Color(0xFFDC2626).copy(alpha = 0.8f) else Color(0xFF64748B),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
