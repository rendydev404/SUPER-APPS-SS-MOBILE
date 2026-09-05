package com.sukashawarma.superapp.feature.manager.ui.waste

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.RealtimeTables
import com.sukashawarma.superapp.domain.model.Role
import com.sukashawarma.superapp.feature.manager.domain.BahanTerbuang
import com.sukashawarma.superapp.feature.manager.domain.LaporanWaste
import com.sukashawarma.superapp.feature.manager.domain.MIN_ALASAN_PENOLAKAN
import com.sukashawarma.superapp.feature.manager.domain.PresetPeriode
import com.sukashawarma.superapp.feature.manager.domain.RingkasanWaste
import com.sukashawarma.superapp.feature.manager.domain.StatusWaste
import com.sukashawarma.superapp.feature.manager.domain.cacah
import com.sukashawarma.superapp.feature.manager.domain.rupiah
import com.sukashawarma.superapp.feature.manager.domain.waktuJakarta
import com.sukashawarma.superapp.feature.manager.domain.waktuJakartaRingkas
import com.sukashawarma.superapp.feature.manager.ui.ChipJingga
import com.sukashawarma.superapp.feature.manager.ui.GarisKartu
import com.sukashawarma.superapp.feature.manager.ui.HijauGaris
import com.sukashawarma.superapp.feature.manager.ui.HijauLatar
import com.sukashawarma.superapp.feature.manager.ui.HijauTeks
import com.sukashawarma.superapp.feature.manager.ui.KartuPanel
import com.sukashawarma.superapp.feature.manager.ui.MerahGaris
import com.sukashawarma.superapp.feature.manager.ui.MerahLatar
import com.sukashawarma.superapp.feature.manager.ui.MerahTeks
import com.sukashawarma.superapp.feature.manager.ui.PanelKosong
import com.sukashawarma.superapp.presentation.theme.SukaBrown
import com.sukashawarma.superapp.presentation.theme.SukaCream
import com.sukashawarma.superapp.presentation.theme.SukaGray400
import com.sukashawarma.superapp.presentation.theme.SukaOrange
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private val FILTER_PERIODE = listOf(
    PresetPeriode.HARI_INI to "Hari Ini",
    PresetPeriode.MINGGU to "7 Hari",
    PresetPeriode.BULAN to "30 Hari",
)

private val HIJAU_AKSI = Color(0xFF059669)

/**
 * Pengawasan Waste Stok — cermin `app/waste/` web.
 *
 * Dua tab seperti web: antrean yang menunggu keputusan, dan riwayat beserta
 * angka ringkasnya. Menyetujui memotong stok lewat trigger database yang sama
 * dengan yang dipakai web, jadi tidak ada logika pemotongan di sisi aplikasi.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WasteScreen(
    onExit: () -> Unit,
    viewModel: WasteViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    RealtimeRefresh(RealtimeTables.WASTE_REPORTS) { viewModel.muatUlang() }

    LaunchedEffect(state.kabar, state.galat) {
        val pesan = state.kabar ?: state.galat
        if (pesan != null) {
            snackbar.showSnackbar(pesan)
            viewModel.tutupKabar()
        }
    }

    Scaffold(
        containerColor = SukaCream,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Text("Waste Stok", fontWeight = FontWeight.Black, fontSize = 17.sp, color = SukaBrown)
                },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = SukaBrown)
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::muatUlang) {
                        Icon(Icons.Default.Refresh, "Muat ulang", tint = SukaBrown)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { PanelKepala(state, viewModel) }
            item { BarisTab(state, viewModel) }

            when (state.tab) {
                TabWaste.MENUNGGU -> isiTabMenunggu(state, viewModel)
                TabWaste.RIWAYAT -> isiTabRiwayat(state, viewModel)
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

/* ----------------------------------------------------------------------- */
/* Kepala: identitas wewenang dan penyaring outlet                          */
/* ----------------------------------------------------------------------- */

@Composable
private fun PanelKepala(state: WasteUiState, viewModel: WasteViewModel) {
    var menuOutlet by remember { mutableStateOf(false) }
    val seluruhOutlet = state.role == Role.REGIONAL_MANAGER

    KartuPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(38.dp).background(SukaOrange.copy(alpha = 0.10f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Delete, null, tint = SukaOrange, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Pengawasan Waste Stok",
                    color = SukaBrown,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "Wewenang: ${labelWewenang(state.role)}",
                    color = SukaGray400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (state.memuat) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = SukaOrange)
            }
        }
        Spacer(Modifier.height(10.dp))
        Surface(
            shape = RoundedCornerShape(50),
            color = if (seluruhOutlet) HijauLatar else Color(0xFFFEF3C7),
            border = BorderStroke(1.dp, if (seluruhOutlet) HijauGaris else Color(0xFFFCD34D)),
        ) {
            Text(
                if (seluruhOutlet) {
                    "Akses seluruh outlet"
                } else {
                    "${state.daftarOutlet.size} outlet binaan"
                },
                Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                color = if (seluruhOutlet) HijauTeks else Color(0xFF78350F),
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
            )
        }

        Spacer(Modifier.height(14.dp))
        Box {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = BorderStroke(1.dp, SukaBrown.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth().clickable { menuOutlet = true },
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Storefront, null, tint = SukaGray400, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        state.namaOutletTerpilih ?: if (seluruhOutlet) {
                            "Semua outlet aktif"
                        } else {
                            "Semua outlet binaan saya"
                        },
                        Modifier.weight(1f),
                        color = SukaBrown,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Icon(Icons.Default.ArrowDropDown, null, tint = SukaBrown)
                }
            }
            DropdownMenu(expanded = menuOutlet, onDismissRequest = { menuOutlet = false }) {
                DropdownMenuItem(
                    text = {
                        Text(
                            if (seluruhOutlet) "Semua outlet aktif" else "Semua outlet binaan saya",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    onClick = { viewModel.pilihOutlet(null); menuOutlet = false },
                )
                state.daftarOutlet.forEach { outlet ->
                    DropdownMenuItem(
                        text = { Text(outlet.nama, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        onClick = { viewModel.pilihOutlet(outlet.id); menuOutlet = false },
                    )
                }
            }
        }
    }
}

private fun labelWewenang(role: Role?): String = when (role) {
    Role.AREA_MANAGER -> "Area Manager (AM)"
    Role.REGIONAL_MANAGER -> "Regional Manager (RM)"
    else -> role?.value?.replace('_', ' ')?.uppercase() ?: "-"
}

@Composable
private fun BarisTab(state: WasteUiState, viewModel: WasteViewModel) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        TombolTab(
            terpilih = state.tab == TabWaste.MENUNGGU,
            ikon = Icons.Default.Schedule,
            label = "Menunggu",
            jumlah = state.menunggu.size,
            modifier = Modifier.weight(1f),
        ) { viewModel.pilihTab(TabWaste.MENUNGGU) }
        TombolTab(
            terpilih = state.tab == TabWaste.RIWAYAT,
            ikon = Icons.Default.BarChart,
            label = "Riwayat & Analitik",
            jumlah = 0,
            modifier = Modifier.weight(1f),
        ) { viewModel.pilihTab(TabWaste.RIWAYAT) }
    }
}

@Composable
private fun TombolTab(
    terpilih: Boolean,
    ikon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    jumlah: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (terpilih) SukaOrange else Color.Transparent,
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                ikon,
                null,
                tint = if (terpilih) Color.White else SukaBrown.copy(alpha = 0.7f),
                modifier = Modifier.size(15.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                label,
                color = if (terpilih) Color.White else SukaBrown.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (jumlah > 0) {
                Spacer(Modifier.width(5.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (terpilih) Color.White else Color(0xFFDC2626),
                ) {
                    Text(
                        jumlah.toString(),
                        Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                        color = if (terpilih) SukaOrange else Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
    }
}

/* ----------------------------------------------------------------------- */
/* Tab 1: antrean menunggu persetujuan                                      */
/* ----------------------------------------------------------------------- */

private fun LazyListScope.isiTabMenunggu(
    state: WasteUiState,
    viewModel: WasteViewModel,
) {
    if (state.menunggu.isEmpty()) {
        item { PanelAntreanBersih(state.memuat) }
        return
    }

    item {
        Text(
            "${state.menunggu.size} pengajuan menunggu tindakan Anda",
            color = SukaBrown.copy(alpha = 0.7f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
    items(state.menunggu, key = { it.id }) { laporan ->
        KartuAntrean(
            laporan = laporan,
            sedangDiproses = laporan.id in state.sedangDiproses,
            onSetujui = { viewModel.setujui(laporan) },
            onTolak = { alasan -> viewModel.tolak(laporan, alasan) },
        )
    }
}

@Composable
private fun PanelAntreanBersih(memuat: Boolean) {
    KartuPanel {
        if (memuat) {
            PanelKosong("Memuat antrean pengajuan waste...")
            return@KartuPanel
        }
        Column(
            Modifier.fillMaxWidth().padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(56.dp).background(HijauLatar, RoundedCornerShape(50)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.CheckCircle, null, tint = HijauTeks, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text("Semua pengajuan bersih!", color = SukaBrown, fontSize = 15.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(4.dp))
            Text(
                "Tidak ada pengajuan waste yang menunggu persetujuan saat ini.",
                color = SukaGray400,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun KartuAntrean(
    laporan: LaporanWaste,
    sedangDiproses: Boolean,
    onSetujui: () -> Unit,
    onTolak: (String) -> Unit,
) {
    var fotoTerbuka by remember { mutableStateOf(false) }
    var dialogTolak by remember { mutableStateOf(false) }

    KartuPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ChipJingga(laporan.outletNama)
            Spacer(Modifier.weight(1f))
            Text(
                waktuJakartaRingkas(laporan.dibuatPada),
                color = SukaGray400,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    laporan.bahanNama,
                    color = SukaBrown,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "Kuantitas: ${laporan.qtyTeks} ${laporan.satuan}",
                    color = SukaGray400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "ESTIMASI KERUGIAN",
                    color = SukaGray400,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    rupiah(laporan.nilai),
                    color = Color(0xFFDC2626),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Surface(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = SukaBrown.copy(alpha = 0.03f),
            border = BorderStroke(1.dp, GarisKartu),
        ) {
            Column(Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Default.WarningAmber,
                        null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Alasan: ${laporan.alasan}",
                        color = SukaBrown,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, null, tint = SukaGray400, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Pelapor: ${laporan.pelaporNama}",
                        color = SukaGray400,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        if (laporan.fotoUrl != null) {
            Surface(
                Modifier.fillMaxWidth().clickable { fotoTerbuka = true },
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, GarisKartu),
            ) {
                Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = laporan.fotoUrl,
                        contentDescription = "Bukti fisik ${laporan.bahanNama}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(46.dp)
                            .background(SukaBrown.copy(alpha = 0.05f), RoundedCornerShape(10.dp)),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Image, null, tint = SukaOrange, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(5.dp))
                            Text(
                                "Lihat foto bukti fisik",
                                color = SukaBrown,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Text("Ketuk untuk memperbesar", color = SukaGray400, fontSize = 9.sp)
                    }
                }
            }
        } else {
            Text(
                "* Tidak ada lampiran foto fisik",
                color = SukaGray400,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = GarisKartu)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(
                Modifier.weight(1f).clickable(enabled = !sedangDiproses) { dialogTolak = true },
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, MerahGaris),
            ) {
                Row(
                    Modifier.padding(vertical = 11.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Close, null, tint = MerahTeks, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Tolak", color = MerahTeks, fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
            }
            Surface(
                Modifier.weight(1f).clickable(enabled = !sedangDiproses, onClick = onSetujui),
                shape = RoundedCornerShape(12.dp),
                color = if (sedangDiproses) HIJAU_AKSI.copy(alpha = 0.5f) else HIJAU_AKSI,
            ) {
                Row(
                    Modifier.padding(vertical = 11.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (sedangDiproses) {
                        CircularProgressIndicator(
                            Modifier.size(15.dp),
                            strokeWidth = 2.dp,
                            color = Color.White,
                        )
                    } else {
                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(15.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                    Text("Setujui", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }

    if (fotoTerbuka && laporan.fotoUrl != null) {
        DialogFoto(laporan.fotoUrl, "${laporan.bahanNama} — ${laporan.outletNama}") { fotoTerbuka = false }
    }

    if (dialogTolak) {
        DialogTolak(
            laporan = laporan,
            onBatal = { dialogTolak = false },
            onKirim = { alasan -> dialogTolak = false; onTolak(alasan) },
        )
    }
}

@Composable
private fun DialogFoto(url: String, judul: String, onTutup: () -> Unit) {
    Dialog(onDismissRequest = onTutup) {
        Surface(shape = RoundedCornerShape(24.dp), color = Color.White) {
            Column {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        judul,
                        Modifier.weight(1f),
                        color = SukaBrown,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    IconButton(onClick = onTutup) {
                        Icon(Icons.Default.Close, "Tutup", tint = SukaBrown)
                    }
                }
                AsyncImage(
                    model = url,
                    contentDescription = "Bukti fisik waste",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().height(340.dp).background(Color(0xFF111827)),
                )
            }
        }
    }
}

@Composable
private fun DialogTolak(
    laporan: LaporanWaste,
    onBatal: () -> Unit,
    onKirim: (String) -> Unit,
) {
    var alasan by remember { mutableStateOf("") }
    val cukup = alasan.trim().length >= MIN_ALASAN_PENOLAKAN

    Dialog(onDismissRequest = onBatal) {
        Surface(shape = RoundedCornerShape(24.dp), color = Color.White) {
            Column(Modifier.padding(20.dp)) {
                Text("Tolak Pengajuan Waste", color = SukaBrown, fontSize = 15.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(12.dp))
                Surface(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MerahLatar.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MerahGaris),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            "Bahan: ${laporan.bahanNama} (${laporan.qtyTeks} ${laporan.satuan})",
                            color = SukaBrown,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            "Outlet: ${laporan.outletNama}",
                            color = SukaBrown,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            "Alasan kru: ${laporan.alasan}",
                            color = SukaBrown,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = alasan,
                    onValueChange = { alasan = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Alasan penolakan", fontSize = 12.sp) },
                    placeholder = {
                        Text("Cth: foto tidak jelas, sisa porsi masih bisa diolah", fontSize = 11.sp)
                    },
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Minimal $MIN_ALASAN_PENOLAKAN karakter.",
                    color = SukaGray400,
                    fontSize = 10.sp,
                )
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onBatal) {
                        Text("Batal", color = SukaBrown, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        Modifier.clickable(enabled = cukup) { onKirim(alasan.trim()) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (cukup) Color(0xFFDC2626) else Color(0xFFDC2626).copy(alpha = 0.4f),
                    ) {
                        Text(
                            "Konfirmasi Tolak",
                            Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }
        }
    }
}

/* ----------------------------------------------------------------------- */
/* Tab 2: riwayat dan analitik                                              */
/* ----------------------------------------------------------------------- */

private fun LazyListScope.isiTabRiwayat(
    state: WasteUiState,
    viewModel: WasteViewModel,
) {
    item { KartuRingkasanWaste(state.ringkasan) }
    item { PanelPenyaringRiwayat(state, viewModel) }

    if (state.riwayat.baris.isEmpty()) {
        item { KartuPanel { PanelKosong("Tidak ada riwayat waste pada periode ini.") } }
        return
    }

    items(state.riwayat.baris, key = { it.id }) { laporan -> KartuRiwayat(laporan) }
    item { BarisHalaman(state, viewModel) }
}

@Composable
private fun KartuRingkasanWaste(ringkasan: RingkasanWaste) {
    KartuPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "TOTAL KERUGIAN WASTE",
                    color = SukaGray400,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.7.sp,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    rupiah(ringkasan.totalNilai),
                    color = Color(0xFFDC2626),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "Dari laporan yang telah disetujui",
                    color = SukaGray400,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Box(
                Modifier.size(38.dp).background(MerahLatar, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.TrendingDown, null, tint = MerahTeks, modifier = Modifier.size(19.dp))
            }
        }

        Spacer(Modifier.height(14.dp))
        HorizontalDivider(color = GarisKartu)
        Spacer(Modifier.height(14.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "INSIDEN DILAPORKAN",
                    color = SukaGray400,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.7.sp,
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        cacah(ringkasan.totalInsiden),
                        color = SukaBrown,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        "kejadian",
                        Modifier.padding(bottom = 2.dp),
                        color = SukaGray400,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            if (ringkasan.jumlahMenunggu > 0) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFFFEF3C7),
                    border = BorderStroke(1.dp, Color(0xFFFCD34D)),
                ) {
                    Text(
                        "${ringkasan.jumlahMenunggu} menunggu",
                        Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        color = Color(0xFF78350F),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        HorizontalDivider(color = GarisKartu)
        Spacer(Modifier.height(14.dp))

        Text(
            "BAHAN PALING BANYAK TERBUANG",
            color = SukaGray400,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.7.sp,
        )
        Spacer(Modifier.height(8.dp))
        if (ringkasan.bahanTeratas.isEmpty()) {
            Text(
                "Belum ada data waste pada periode ini.",
                color = SukaGray400,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
        } else {
            ringkasan.bahanTeratas.forEachIndexed { index, bahan -> BarisBahanTerbuang(index + 1, bahan) }
        }
    }
}

@Composable
private fun BarisBahanTerbuang(nomor: Int, bahan: BahanTerbuang) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "$nomor.",
            Modifier.width(18.dp),
            color = SukaBrown.copy(alpha = 0.5f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
        )
        Text(
            bahan.nama,
            Modifier.weight(1f),
            color = SukaBrown,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "${bahan.qtyTeks} ${bahan.satuan}",
            color = SukaGray400,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            rupiah(bahan.nilai),
            color = Color(0xFFDC2626),
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
private fun PanelPenyaringRiwayat(state: WasteUiState, viewModel: WasteViewModel) {
    var dialogTanggal by remember { mutableStateOf(false) }
    var menuStatus by remember { mutableStateOf(false) }

    KartuPanel {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FILTER_PERIODE.forEach { (preset, label) ->
                TombolPeriodeKecil(
                    label = label,
                    aktif = !state.memakaiKustom && state.preset == preset,
                ) { viewModel.pilihPreset(preset) }
            }
            TombolPeriodeKecil(
                label = state.kustom?.let { "${it.dari} - ${it.sampai}" } ?: "Kustom",
                aktif = state.memakaiKustom,
                ikon = Icons.Default.CalendarMonth,
            ) { dialogTanggal = true }
        }

        Spacer(Modifier.height(12.dp))
        Box {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, SukaBrown.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth().clickable { menuStatus = true },
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Status: ${labelStatus(state.filterStatus)}",
                        Modifier.weight(1f),
                        color = SukaBrown,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Icon(Icons.Default.ArrowDropDown, null, tint = SukaBrown)
                }
            }
            DropdownMenu(expanded = menuStatus, onDismissRequest = { menuStatus = false }) {
                listOf(null, StatusWaste.DISETUJUI, StatusWaste.DITOLAK).forEach { status ->
                    DropdownMenuItem(
                        text = { Text(labelStatus(status), fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        onClick = { viewModel.pilihStatus(status); menuStatus = false },
                    )
                }
            }
        }
    }

    if (dialogTanggal) {
        DialogRentang(
            onTutup = { dialogTanggal = false },
            onPilih = { dari, sampai ->
                viewModel.pilihRentangKustom(dari, sampai)
                dialogTanggal = false
            },
        )
    }
}

private fun labelStatus(status: StatusWaste?): String = when (status) {
    StatusWaste.DISETUJUI -> "Hanya disetujui"
    StatusWaste.DITOLAK -> "Hanya ditolak"
    else -> "Disetujui & ditolak"
}

@Composable
private fun TombolPeriodeKecil(
    label: String,
    aktif: Boolean,
    ikon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (aktif) SukaOrange else Color.White,
        border = BorderStroke(1.dp, if (aktif) SukaOrange else SukaBrown.copy(alpha = 0.15f)),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (ikon != null) {
                Icon(
                    ikon,
                    null,
                    tint = if (aktif) Color.White else SukaBrown,
                    modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.width(5.dp))
            }
            Text(
                label,
                color = if (aktif) Color.White else SukaBrown,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogRentang(onTutup: () -> Unit, onPilih: (LocalDate, LocalDate) -> Unit) {
    val picker = rememberDateRangePickerState()
    DatePickerDialog(
        onDismissRequest = onTutup,
        confirmButton = {
            TextButton(
                onClick = {
                    val dari = picker.selectedStartDateMillis?.let(::tanggalDari)
                    val sampai = picker.selectedEndDateMillis?.let(::tanggalDari)
                    if (dari != null) onPilih(dari, sampai ?: dari)
                },
                enabled = picker.selectedStartDateMillis != null,
            ) { Text("Terapkan", fontWeight = FontWeight.ExtraBold) }
        },
        dismissButton = { TextButton(onClick = onTutup) { Text("Batal") } },
    ) {
        DateRangePicker(state = picker, title = {
            Text(
                "Pilih Rentang Tanggal",
                Modifier.padding(start = 24.dp, top = 16.dp),
                fontWeight = FontWeight.Black,
            )
        })
    }
}

private fun tanggalDari(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

@Composable
private fun KartuRiwayat(laporan: LaporanWaste) {
    var fotoTerbuka by remember { mutableStateOf(false) }
    val disetujui = laporan.status == StatusWaste.DISETUJUI

    KartuPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ChipJingga(laporan.outletNama)
            Spacer(Modifier.weight(1f))
            Surface(
                shape = RoundedCornerShape(50),
                color = if (disetujui) HijauLatar else MerahLatar,
                border = BorderStroke(1.dp, if (disetujui) HijauGaris else MerahGaris),
            ) {
                Text(
                    if (disetujui) "DISETUJUI" else "DITOLAK",
                    Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                    color = if (disetujui) HijauTeks else MerahTeks,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    laporan.bahanNama,
                    color = SukaBrown,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${laporan.qtyTeks} ${laporan.satuan} • ${waktuJakarta(laporan.dibuatPada)}",
                    color = SukaGray400,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                rupiah(laporan.nilai),
                color = if (disetujui) Color(0xFFDC2626) else SukaGray400,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
            )
        }

        Spacer(Modifier.height(10.dp))
        Text(
            "Alasan: ${laporan.alasan}",
            color = SukaBrown.copy(alpha = 0.8f),
            fontSize = 11.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium,
        )
        if (laporan.alasanPenolakan != null) {
            Spacer(Modifier.height(8.dp))
            Surface(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = MerahLatar.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, MerahGaris),
            ) {
                Text(
                    "Ditolak karena: ${laporan.alasanPenolakan}",
                    Modifier.padding(10.dp),
                    color = MerahTeks,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Pelapor: ${laporan.pelaporNama}" +
                    (laporan.penyetujuNama?.let { " • Diputuskan: $it" } ?: ""),
                Modifier.weight(1f),
                color = SukaGray400,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
            )
            if (laporan.fotoUrl != null) {
                Spacer(Modifier.width(8.dp))
                Row(
                    Modifier.clickable { fotoTerbuka = true },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Image, null, tint = SukaOrange, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Foto", color = SukaOrange, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }

    if (fotoTerbuka && laporan.fotoUrl != null) {
        DialogFoto(laporan.fotoUrl, "${laporan.bahanNama} — ${laporan.outletNama}") { fotoTerbuka = false }
    }
}

@Composable
private fun BarisHalaman(state: WasteUiState, viewModel: WasteViewModel) {
    val riwayat = state.riwayat
    KartuPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Halaman ${riwayat.halaman} dari ${riwayat.totalHalaman}",
                    color = SukaBrown,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    "${cacah(riwayat.totalBaris)} laporan pada periode ini",
                    color = SukaGray400,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            TombolHalaman(Icons.Default.ChevronLeft, "Halaman sebelumnya", riwayat.halaman > 1) {
                viewModel.pilihHalaman(riwayat.halaman - 1)
            }
            Spacer(Modifier.width(8.dp))
            TombolHalaman(
                Icons.Default.ChevronRight,
                "Halaman berikutnya",
                riwayat.halaman < riwayat.totalHalaman,
            ) { viewModel.pilihHalaman(riwayat.halaman + 1) }
        }
    }
}

@Composable
private fun TombolHalaman(
    ikon: androidx.compose.ui.graphics.vector.ImageVector,
    keterangan: String,
    aktif: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        Modifier.size(36.dp).clickable(enabled = aktif, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (aktif) SukaOrange.copy(alpha = 0.10f) else SukaBrown.copy(alpha = 0.03f),
        border = BorderStroke(1.dp, GarisKartu),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                ikon,
                keterangan,
                tint = if (aktif) SukaOrange else SukaGray400,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
