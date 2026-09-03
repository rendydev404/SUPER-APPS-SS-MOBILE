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
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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

@Composable
fun MonitoringScreen(
    onKeluar: () -> Unit,
    onBukaBahan: (outletId: String, bahanId: String, nama: String) -> Unit,
    onBukaProduksi: (outletId: String) -> Unit,
    onBukaTransfer: () -> Unit,
    viewModel: MonitoringViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()

    Column(Modifier.fillMaxSize().background(SukaSurface)) {
        Header(
            state = state,
            onKeluar = onKeluar,
            onSegarkan = viewModel::segarkan,
            onPilihOutlet = viewModel::pilihOutlet,
            onBukaProduksi = { state.outletTerpilih?.let { onBukaProduksi(it.id) } },
            onBukaTransfer = onBukaTransfer,
        )

        when {
            state.tidakBerhak -> KeadaanTidakBerhak(
                "Akun Anda belum terhubung dengan outlet mana pun. Hubungi admin atau regional manager."
            )
            state.memuat -> MemuatPenuh()
            state.error != null -> KeadaanGagal(state.error!!, viewModel::muatAwal)
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "kpi") { KartuRingkasan(state, viewModel::tekanKartu) }
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
    onBukaProduksi: () -> Unit,
    onBukaTransfer: () -> Unit,
) {
    var menuTerbuka by remember { mutableStateOf(false) }
    Box(
        Modifier.fillMaxWidth().background(
            Brush.verticalGradient(listOf(Color(0xFFEA580C), Color(0xFFF97316)))
        )
    ) {
        Column(Modifier.statusBarsPadding().padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onKeluar) {
                    Icon(Icons.Default.ArrowBack, "Kembali", tint = Color.White)
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        state.outletTerpilih?.name?.uppercase() ?: "OUTLET",
                        color = Color(0xFFFFEDD5),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "Saldo Stok Real-time",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
                IconButton(onClick = onSegarkan) {
                    Icon(Icons.Default.Refresh, "Segarkan", tint = Color.White)
                }
            }

            if (!state.tidakBerhak) {
                Row(
                    Modifier.padding(start = 8.dp, end = 8.dp, top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.White.copy(alpha = 0.20f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        ) {
                            Row(
                                Modifier
                                    .heightIn(min = 40.dp)
                                    .let { m ->
                                        if (state.tampilkanPemilihOutlet) {
                                            m.clickable { menuTerbuka = true }
                                        } else m
                                    }
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    state.outletTerpilih?.name ?: "Memuat outlet…",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (state.tampilkanPemilihOutlet) {
                                    Icon(
                                        Icons.Default.ArrowDropDown, "Ganti outlet",
                                        tint = Color.White, modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                        DropdownMenu(expanded = menuTerbuka, onDismissRequest = { menuTerbuka = false }) {
                            state.outlets.forEach { outlet ->
                                DropdownMenuItem(
                                    text = { Text(outlet.name, fontSize = 14.sp) },
                                    onClick = { menuTerbuka = false; onPilihOutlet(outlet) },
                                )
                            }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    TombolBulat(Icons.Default.Calculate, "Estimasi produksi", onBukaProduksi)
                    if (state.tampilkanPemilihOutlet) {
                        Spacer(Modifier.width(7.dp))
                        TombolBulat(Icons.Default.SwapHoriz, "Saran transfer", onBukaTransfer)
                    }
                }
            }
        }
    }
}

@Composable
private fun TombolBulat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    deskripsi: String,
    onClick: () -> Unit,
) {
    Surface(
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.20f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
        modifier = Modifier.size(40.dp),
    ) {
        IconButton(onClick = onClick) {
            Icon(icon, deskripsi, tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

// ----------------------------------------------------------------- kartu ringkasan

@Composable
private fun KartuRingkasan(state: MonitoringUiState, onTekan: (FilterKpi) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        KotakRingkasan(
            angka = state.jumlahKritis,
            label = "Kritis",
            warna = Color(0xFFDC2626),
            aktif = state.filter == FilterKpi.KRITIS,
            modifier = Modifier.weight(1f),
        ) { onTekan(FilterKpi.KRITIS) }

        KotakRingkasan(
            angka = state.jumlahSelisih,
            label = "Selisih",
            warna = Color(0xFFD97706),
            aktif = state.filter == FilterKpi.SELISIH,
            modifier = Modifier.weight(1f),
        ) { onTekan(FilterKpi.SELISIH) }

        // Aman sengaja tidak bisa ditekan, sama seperti web: tidak ada gunanya
        // menyaring daftar untuk hanya menampilkan yang tidak perlu ditindaklanjuti.
        KotakRingkasan(
            angka = state.jumlahAman,
            label = "Aman",
            warna = Color(0xFF168451),
            aktif = false,
            dapatDitekan = false,
            modifier = Modifier.weight(1f),
        ) {}
    }
}

@Composable
private fun KotakRingkasan(
    angka: Int,
    label: String,
    warna: Color,
    aktif: Boolean,
    modifier: Modifier = Modifier,
    dapatDitekan: Boolean = true,
    onTekan: () -> Unit,
) {
    val isi: @Composable () -> Unit = {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                angka.toString(),
                color = if (aktif) Color.White else warna,
                fontSize = 21.sp,
                fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                label.uppercase(),
                color = if (aktif) Color.White.copy(alpha = 0.9f) else SukaOnSurfaceVariant,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
            )
        }
    }
    val bentuk = RoundedCornerShape(18.dp)
    val isiWarna = if (aktif) warna else Color.White
    val garis = BorderStroke(1.dp, if (aktif) warna else Color(0xFFE7ECF2))

    if (dapatDitekan) {
        Surface(
            onClick = onTekan,
            modifier = modifier.height(74.dp),
            shape = bentuk,
            color = isiWarna,
            border = garis,
            shadowElevation = 1.dp,
            content = isi,
        )
    } else {
        Surface(
            modifier = modifier.height(74.dp),
            shape = bentuk,
            color = isiWarna,
            border = garis,
            shadowElevation = 1.dp,
            content = isi,
        )
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
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = state.cari,
            onValueChange = onCari,
            modifier = Modifier.weight(1f).height(56.dp),
            placeholder = {
                // Satu baris dipaksa: kolom ini sempit karena berbagi ruang dengan
                // dropdown urutan, dan teks yang membungkus membuat tingginya melar.
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
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = Color(0xFFF97316),
                unfocusedBorderColor = Color(0xFFE2E8F0),
            ),
        )

        Box {
            Surface(
                onClick = { menuUrutan = true },
                modifier = Modifier.height(56.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            ) {
                Row(
                    Modifier.fillMaxHeight().padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        state.urutan.label,
                        color = SukaOnSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                    Icon(Icons.Default.ArrowDropDown, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                }
            }
            DropdownMenu(expanded = menuUrutan, onDismissRequest = { menuUrutan = false }) {
                UrutanStok.entries.forEach { u ->
                    DropdownMenuItem(
                        text = { Text(u.label, fontSize = 14.sp) },
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
        Modifier.fillMaxWidth().padding(top = 6.dp, start = 2.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "${kategori.emoji} ${kategori.label.uppercase()}",
            Modifier.weight(1f),
            color = SukaOnSurface,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp,
        )
        Text("$jumlah Item", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(
                        row.itemName,
                        color = Color(0xFFEA580C),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn, null,
                            tint = Color(0xFFB6C0CC), modifier = Modifier.size(11.dp),
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            lokasiPenyimpanan(row.kategori, row.itemName),
                            color = SukaOnSurfaceVariant,
                            fontSize = 10.sp,
                        )
                        Text("  ·  ", color = Color(0xFFCBD5E1), fontSize = 10.sp)
                        Text(
                            "Min: ",
                            color = SukaOnSurfaceVariant,
                            fontSize = 10.sp,
                        )
                        Text(
                            "${formatAngkaStok(row.threshold ?: 0.0)} ${formatSatuan(row.satuan)}",
                            color = SukaOnSurface,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                StatusBadge(status)
            }

            Spacer(Modifier.height(11.dp))

            // Rincian tiga jenjang satuan — angkanya dihitung dengan rumus yang sama
            // persis dengan web, supaya saldo yang sama tidak tampil berbeda di HP.
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFFBF5), RoundedCornerShape(12.dp))
                    .padding(vertical = 9.dp),
            ) {
                KolomSatuan("Sat. Besar", formatAngkaStok(tri.besar), formatSatuan(row.meta.satuan), Modifier.weight(1f))
                KolomSatuan(
                    "Sat. Tengah",
                    if (row.meta.satuanTengah != null) formatAngkaStok(tri.tengah) else "—",
                    if (row.meta.satuanTengah != null) formatSatuan(row.meta.satuanTengah) else "",
                    Modifier.weight(1f),
                )
                KolomSatuan(
                    "Sat. Kecil",
                    if (row.meta.satuanKecil != null) formatAngkaStok(tri.kecil) else "—",
                    if (row.meta.satuanKecil != null) formatSatuan(row.meta.satuanKecil) else "",
                    Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun KolomSatuan(label: String, angka: String, satuan: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label.uppercase(),
            color = Color(0xFF9AA6B2),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(3.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(angka, color = SukaOnSurface, fontSize = 13.sp, fontWeight = FontWeight.Black)
            if (satuan.isNotEmpty()) {
                Spacer(Modifier.width(2.dp))
                Text(satuan, color = SukaOnSurfaceVariant, fontSize = 8.sp)
            }
        }
    }
}
