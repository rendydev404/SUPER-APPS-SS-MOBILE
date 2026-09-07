package com.sukashawarma.superapp.feature.manager.ui.pettycash

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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.sukashawarma.superapp.feature.manager.domain.AksiTopup
import com.sukashawarma.superapp.feature.manager.domain.FilterReview
import com.sukashawarma.superapp.feature.manager.domain.FilterRiwayat
import com.sukashawarma.superapp.feature.manager.domain.FilterTanggal
import com.sukashawarma.superapp.feature.manager.domain.StatusTopup
import com.sukashawarma.superapp.feature.manager.domain.TopupPettyCash
import com.sukashawarma.superapp.feature.manager.domain.aksiUntuk
import com.sukashawarma.superapp.feature.manager.domain.cacah
import com.sukashawarma.superapp.feature.manager.domain.pisahCatatanFinance
import com.sukashawarma.superapp.feature.manager.domain.rapikanCatatanFinance
import com.sukashawarma.superapp.feature.manager.domain.rupiah
import com.sukashawarma.superapp.feature.manager.domain.waktuRelatif
import com.sukashawarma.superapp.feature.manager.ui.ChipJingga
import com.sukashawarma.superapp.feature.manager.ui.GarisKartu
import com.sukashawarma.superapp.feature.manager.ui.HijauGaris
import com.sukashawarma.superapp.feature.manager.ui.HijauLatar
import com.sukashawarma.superapp.feature.manager.ui.HijauTeks
import com.sukashawarma.superapp.feature.manager.ui.KartuPanel
import com.sukashawarma.superapp.feature.manager.ui.MerahGaris
import com.sukashawarma.superapp.feature.manager.ui.MerahLatar
import com.sukashawarma.superapp.feature.manager.ui.MerahTeks
import com.sukashawarma.superapp.presentation.theme.SukaBrown
import com.sukashawarma.superapp.presentation.theme.SukaCream
import com.sukashawarma.superapp.presentation.theme.SukaGray400
import com.sukashawarma.superapp.presentation.theme.SukaOrange

private val AMBER_LATAR = Color(0xFFFEF3C7)
private val AMBER_GARIS = Color(0xFFFCD34D)
private val AMBER_TEKS = Color(0xFF78350F)

/**
 * Dana Operasional Cabang — cermin `app/petty-cash/` web.
 *
 * Dua tindakan di layar ini memanggil RPC yang sama dengan web
 * (`area_manager_process_petty_cash` dan `area_manager_forward_funds`), keduanya
 * `SECURITY DEFINER` dan memeriksa role serta status di dalam database. Jadi layar
 * ini tidak menghitung sendiri boleh-tidaknya sebuah pengajuan berpindah tahap; ia
 * hanya menampilkan hasilnya.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PettyCashScreen(
    onExit: () -> Unit,
    viewModel: PettyCashViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    RealtimeRefresh(RealtimeTables.PETTY_CASH_TOPUPS) { viewModel.muatUlang() }

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
                title = { Text("Petty Cash", fontWeight = FontWeight.Black, fontSize = 17.sp, color = SukaBrown, maxLines = 1) },
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
            item { BarisFilterStatus(state, viewModel) }
            if (!state.bolehMemproses) {
                item { CatatanTanpaWewenang() }
            }
            daftarPengajuan(state, viewModel)
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun PanelKepala(state: PettyCashUiState, viewModel: PettyCashViewModel) {
    KartuPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Dana Operasional Cabang",
                    color = SukaBrown,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "Kelola dan setujui pengajuan petty cash dari tim outlet.",
                    color = SukaGray400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 16.sp,
                )
            }
            if (state.memuat) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = SukaOrange)
            }
        }
        Spacer(Modifier.height(14.dp))
        PilihanTurun(
            ikon = Icons.Default.Storefront,
            nilai = state.namaOutletTerpilih ?: "Semua Outlet Binaan",
            pilihan = listOf<Pair<String?, String>>(null to "Semua Outlet Binaan") +
                state.daftarOutlet.map { it.id to it.nama },
            onPilih = viewModel::pilihOutlet,
        )
        Spacer(Modifier.height(8.dp))
        PilihanTurun(
            ikon = Icons.Default.CalendarMonth,
            nilai = state.filterTanggal.label,
            pilihan = FilterTanggal.entries.map { it to it.label },
            onPilih = viewModel::pilihFilterTanggal,
        )
    }
}

@Composable
private fun <T> PilihanTurun(
    ikon: ImageVector,
    nilai: String,
    pilihan: List<Pair<T, String>>,
    onPilih: (T) -> Unit,
) {
    var terbuka by remember { mutableStateOf(false) }
    Box {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            border = BorderStroke(1.dp, SukaBrown.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth().clickable { terbuka = true },
        ) {
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(ikon, null, tint = SukaOrange, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    nilai,
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
        DropdownMenu(expanded = terbuka, onDismissRequest = { terbuka = false }) {
            pilihan.forEach { (nilaiPilihan, label) ->
                DropdownMenuItem(
                    text = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    onClick = { terbuka = false; onPilih(nilaiPilihan) },
                )
            }
        }
    }
}

@Composable
private fun BarisTab(state: PettyCashUiState, viewModel: PettyCashViewModel) {
    Row(
        Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp)).padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        TombolTab(
            terpilih = state.tab == TabPettyCash.REVIEW,
            label = "Butuh Review",
            jumlah = state.jumlahReview,
            modifier = Modifier.weight(1f),
        ) { viewModel.pilihTab(TabPettyCash.REVIEW) }
        TombolTab(
            terpilih = state.tab == TabPettyCash.RIWAYAT,
            label = "Riwayat",
            jumlah = state.jumlahRiwayat,
            modifier = Modifier.weight(1f),
        ) { viewModel.pilihTab(TabPettyCash.RIWAYAT) }
    }
}

@Composable
private fun TombolTab(
    terpilih: Boolean,
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
            Modifier.padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                label,
                color = if (terpilih) Color.White else SukaBrown.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.width(6.dp))
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (terpilih) Color.White.copy(alpha = 0.25f) else SukaBrown.copy(alpha = 0.07f),
            ) {
                Text(
                    cacah(jumlah),
                    Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                    color = if (terpilih) Color.White else SukaBrown.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}

@Composable
private fun BarisFilterStatus(state: PettyCashUiState, viewModel: PettyCashViewModel) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (state.tab == TabPettyCash.REVIEW) {
            FilterReview.entries.forEach { filter ->
                ChipFilter(filter.label, state.filterReview == filter) {
                    viewModel.pilihFilterReview(filter)
                }
            }
        } else {
            FilterRiwayat.entries.forEach { filter ->
                ChipFilter(filter.label, state.filterRiwayat == filter) {
                    viewModel.pilihFilterRiwayat(filter)
                }
            }
        }
    }
}

@Composable
private fun ChipFilter(label: String, aktif: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (aktif) SukaBrown else Color.White,
        border = BorderStroke(1.dp, if (aktif) SukaBrown else SukaBrown.copy(alpha = 0.15f)),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            label,
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            color = if (aktif) Color.White else SukaBrown.copy(alpha = 0.75f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

/**
 * Catatan untuk role yang boleh melihat tapi tidak boleh memproses — regional
 * manager termasuk di dalamnya. Web menampilkan tombolnya lalu membiarkan galat
 * server muncul; menjelaskannya lebih dulu menghemat satu tap yang pasti gagal.
 */
@Composable
private fun CatatanTanpaWewenang() {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = AMBER_LATAR,
        border = BorderStroke(1.dp, AMBER_GARIS),
    ) {
        Text(
            "Anda dapat memantau pengajuan di sini, tetapi persetujuan petty cash " +
                "dikerjakan oleh Area Manager outlet bersangkutan.",
            Modifier.padding(14.dp),
            color = AMBER_TEKS,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun LazyListScope.daftarPengajuan(state: PettyCashUiState, viewModel: PettyCashViewModel) {
    val daftar = if (state.tab == TabPettyCash.REVIEW) state.review else state.riwayat

    if (daftar.isEmpty()) {
        item { PanelKosongPettyCash(state) }
        return
    }

    items(daftar, key = { it.id }) { topup ->
        KartuPengajuan(
            topup = topup,
            tab = state.tab,
            bolehMemproses = state.bolehMemproses,
            sedangDiproses = topup.id in state.sedangDiproses,
            onTolak = { viewModel.tolak(topup) },
            onSetujui = { viewModel.setujui(topup) },
            onSerahkan = { viewModel.serahkan(topup) },
        )
    }
}

@Composable
private fun PanelKosongPettyCash(state: PettyCashUiState) {
    KartuPanel {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Cabang ditulis eksplisit if/else, BUKAN keluar-awal `return@Column`.
            // `Column` adalah fungsi inline: keluar dari lambdanya setelah sudah
            // memancarkan composable meninggalkan pembukuan grup kompilator tidak
            // seimbang, dan begitu keadaannya berbalik, tabel slot dibaca dengan
            // indeks negatif -> ArrayIndexOutOfBoundsException di SlotTableKt.key.
            if (state.memuat) {
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = SukaOrange)
                Spacer(Modifier.height(12.dp))
                Text("Memuat pengajuan...", color = SukaGray400, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            } else if (state.tab == TabPettyCash.REVIEW) {
                Box(
                    Modifier.size(52.dp).background(HijauLatar, RoundedCornerShape(50)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.CheckCircle, null, tint = HijauTeks, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text("Semua selesai", color = SukaBrown, fontSize = 15.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Tidak ada data yang sesuai filter saat ini.",
                    color = SukaGray400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
            } else {
                Text(
                    "Tidak ada riwayat yang ditemukan.",
                    color = SukaGray400,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun KartuPengajuan(
    topup: TopupPettyCash,
    tab: TabPettyCash,
    bolehMemproses: Boolean,
    sedangDiproses: Boolean,
    onTolak: () -> Unit,
    onSetujui: () -> Unit,
    onSerahkan: () -> Unit,
) {
    var buktiTerbuka by remember { mutableStateOf(false) }
    val (alasan, catatanFinance) = remember(topup.deskripsi) { pisahCatatanFinance(topup.deskripsi) }
    val aksi = aksiUntuk(topup.status)

    KartuPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ChipJingga(topup.outletNama)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.Schedule, null, tint = SukaGray400, modifier = Modifier.size(11.dp))
            Spacer(Modifier.width(3.dp))
            Text(
                waktuRelatif(topup.dibuatPada),
                Modifier.weight(1f),
                color = SukaGray400,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "ID ${topup.idRingkas}",
                color = SukaGray400.copy(alpha = 0.8f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        Spacer(Modifier.height(10.dp))
        Text(
            alasan.ifBlank { "Tanpa keterangan" },
            color = SukaBrown,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            lineHeight = 21.sp,
        )

        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Person, null, tint = SukaGray400, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(5.dp))
            Text(
                topup.pengajuNama,
                color = SukaGray400,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            topup.rekeningTeks ?: "Informasi rekening tidak tersedia",
            color = if (topup.rekeningTeks != null) SukaBrown.copy(alpha = 0.75f) else SukaGray400,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 16.sp,
        )

        if (catatanFinance != null) {
            Spacer(Modifier.height(12.dp))
            Surface(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = SukaBrown.copy(alpha = 0.03f),
                border = BorderStroke(1.dp, GarisKartu),
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Default.Description,
                        null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            "CATATAN DARI FINANCE",
                            color = SukaGray400,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.6.sp,
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            rapikanCatatanFinance(catatanFinance),
                            color = SukaBrown,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        HorizontalDivider(color = GarisKartu)
        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (tab == TabPettyCash.REVIEW) topup.status.label.uppercase() else "TOTAL PENGAJUAN",
                    color = SukaGray400,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.6.sp,
                )
                Spacer(Modifier.height(3.dp))
                Text(rupiah(topup.jumlah), color = SukaBrown, fontSize = 20.sp, fontWeight = FontWeight.Black)
            }
            if (tab == TabPettyCash.RIWAYAT) {
                LencanaStatus(topup.status)
            }
        }

        if (tab == TabPettyCash.REVIEW && aksi != AksiTopup.TIDAK_ADA && bolehMemproses) {
            Spacer(Modifier.height(12.dp))
            when (aksi) {
                AksiTopup.ACC_ATAU_TOLAK -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TombolAksi(
                        label = "Tolak",
                        ikon = Icons.Default.Close,
                        warnaLatar = Color.White,
                        warnaTeks = MerahTeks,
                        garis = MerahGaris,
                        sedangDiproses = sedangDiproses,
                        modifier = Modifier.weight(1f),
                    ) { onTolak() }
                    TombolAksi(
                        label = "ACC",
                        ikon = Icons.Default.Check,
                        warnaLatar = SukaBrown,
                        warnaTeks = Color.White,
                        garis = SukaBrown,
                        sedangDiproses = sedangDiproses,
                        modifier = Modifier.weight(1f),
                    ) { onSetujui() }
                }
                AksiTopup.SERAHKAN -> TombolAksi(
                    label = "Serahkan ke Leader",
                    ikon = Icons.AutoMirrored.Filled.ArrowForward,
                    warnaLatar = Color(0xFF1E293B),
                    warnaTeks = Color.White,
                    garis = Color(0xFF1E293B),
                    sedangDiproses = sedangDiproses,
                    modifier = Modifier.fillMaxWidth(),
                ) { onSerahkan() }
                AksiTopup.TIDAK_ADA -> Unit
            }
        }

        if (topup.buktiTransferUrl != null) {
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.clickable { buktiTerbuka = true },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Description, null, tint = SukaOrange, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(5.dp))
                Text(
                    "Lihat bukti transfer",
                    color = SukaOrange,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }

    if (buktiTerbuka && topup.buktiTransferUrl != null) {
        DialogBukti(topup.buktiTransferUrl) { buktiTerbuka = false }
    }
}

@Composable
private fun LencanaStatus(status: StatusTopup) {
    val (latar, garis, teks) = when (status) {
        StatusTopup.DITERUSKAN_KE_FINANCE -> Triple(AMBER_LATAR, AMBER_GARIS, AMBER_TEKS)
        StatusTopup.DITOLAK -> Triple(MerahLatar, MerahGaris, MerahTeks)
        else -> Triple(HijauLatar, HijauGaris, HijauTeks)
    }
    Surface(shape = RoundedCornerShape(50), color = latar, border = BorderStroke(1.dp, garis)) {
        Text(
            status.label.uppercase(),
            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            color = teks,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp,
        )
    }
}

@Composable
private fun TombolAksi(
    label: String,
    ikon: ImageVector,
    warnaLatar: Color,
    warnaTeks: Color,
    garis: Color,
    sedangDiproses: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.clickable(enabled = !sedangDiproses, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (sedangDiproses) warnaLatar.copy(alpha = 0.55f) else warnaLatar,
        border = BorderStroke(1.dp, garis),
    ) {
        Row(
            Modifier.padding(vertical = 11.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (sedangDiproses) {
                CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = warnaTeks)
            } else {
                Icon(ikon, null, tint = warnaTeks, modifier = Modifier.size(15.dp))
            }
            Spacer(Modifier.width(6.dp))
            Text(label, color = warnaTeks, fontSize = 12.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun DialogBukti(url: String, onTutup: () -> Unit) {
    Dialog(onDismissRequest = onTutup) {
        Surface(shape = RoundedCornerShape(24.dp), color = Color.White) {
            Column {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Lampiran Bukti Transfer",
                        Modifier.weight(1f),
                        color = SukaBrown,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                    )
                    IconButton(onClick = onTutup) {
                        Icon(Icons.Default.Close, "Tutup", tint = SukaBrown)
                    }
                }
                AsyncImage(
                    model = url,
                    contentDescription = "Bukti transfer",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().height(340.dp).background(Color(0xFF111827)),
                )
            }
        }
    }
}
