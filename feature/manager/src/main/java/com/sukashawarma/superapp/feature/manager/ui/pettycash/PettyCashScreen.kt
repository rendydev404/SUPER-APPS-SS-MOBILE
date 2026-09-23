package com.sukashawarma.superapp.feature.manager.ui.pettycash

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import com.sukashawarma.superapp.core.ui.SukaDropdownHeader
import com.sukashawarma.superapp.core.ui.SukaDropdownMenu
import com.sukashawarma.superapp.core.ui.SukaDropdownMenuItem
import com.sukashawarma.superapp.core.ui.SukaFilterDropdown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.RealtimeTables
import com.sukashawarma.superapp.core.ui.ios.BilahJudulIos
import com.sukashawarma.superapp.core.ui.ios.KapsulPilihanIos
import com.sukashawarma.superapp.core.ui.ios.KeadaanIos
import com.sukashawarma.superapp.core.ui.ios.LabelSeksiIos
import com.sukashawarma.superapp.core.ui.ios.LencanaIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.PemisahIos
import com.sukashawarma.superapp.core.ui.ios.SegmenIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.TombolBundarIos
import com.sukashawarma.superapp.core.ui.ios.TombolKapsulIos
import com.sukashawarma.superapp.core.ui.ios.TombolKeduaIos
import com.sukashawarma.superapp.core.ui.ios.TombolUtamaIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WadahSegmenIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.ios.permukaanIos
import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.core.ui.kaca.denganRuangNav
import com.sukashawarma.superapp.core.ui.kaca.navigationBarsPaddingKaca
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
import com.sukashawarma.superapp.feature.manager.ui.KartuPanel
import com.sukashawarma.superapp.feature.manager.ui.PanelKosong

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

    RealtimeRefresh(RealtimeTables.PETTY_CASH_TOPUPS) { viewModel.muatUlang(silent = true) }

    LaunchedEffect(state.kabar, state.galat) {
        val pesan = state.kabar ?: state.galat
        if (pesan != null) {
            snackbar.showSnackbar(pesan)
            viewModel.tutupKabar()
        }
    }

    Scaffold(
        containerColor = WarnaIos.Latar,
        snackbarHost = { SnackbarHost(snackbar, Modifier.navigationBarsPaddingKaca()) },
        topBar = {
            BilahJudulIos(
                judul = "Petty Cash",
                onKembali = onExit,
                aksi = {
                    TombolBundarIos(IkonIos.Refresh, "Muat ulang", { viewModel.muatUlang(silent = true) })
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(
                start = UkuranIos.TepiLayar,
                end = UkuranIos.TepiLayar,
                top = 12.dp,
                bottom = 16.dp,
            ).denganRuangNav(),
            verticalArrangement = Arrangement.spacedBy(UkuranIos.JarakKartu),
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
        Text("Dana Operasional Cabang", style = TipeIos.Utama)
        Spacer(Modifier.height(2.dp))
        Text("Kelola dan setujui pengajuan petty cash dari tim outlet.", style = TipeIos.Catatan)
        Spacer(Modifier.height(12.dp))
        PilihanTurun(
            ikon = IkonIos.Storefront,
            nilai = state.namaOutletTerpilih ?: "Semua Outlet Binaan",
            pilihan = listOf<Pair<String?, String>>(null to "Semua Outlet Binaan") +
                state.daftarOutlet.map { it.id to it.nama },
            onPilih = viewModel::pilihOutlet,
        )
        Spacer(Modifier.height(8.dp))
        PilihanTurun(
            ikon = IkonIos.CalendarMonth,
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
        SukaFilterDropdown(
            label = "",
            value = nilai,
            expanded = terbuka,
            onClick = { terbuka = true },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = ikon,
        )
        SukaDropdownMenu(expanded = terbuka, onDismissRequest = { terbuka = false }) {
            SukaDropdownHeader(title = "PILIH OPSI", onClose = { terbuka = false })
            pilihan.forEach { (nilaiPilihan, textLabel) ->
                SukaDropdownMenuItem(
                    text = textLabel,
                    selected = (textLabel == nilai),
                    onClick = { terbuka = false; onPilih(nilaiPilihan) },
                )
            }
        }
    }
}

@Composable
private fun BarisTab(state: PettyCashUiState, viewModel: PettyCashViewModel) {
    val review = state.tab == TabPettyCash.REVIEW
    WadahSegmenIos {
        // Hitungan selalu tampil (termasuk nol) seperti sebelumnya; hanya tab aktif
        // yang lencananya beraksen supaya mata tidak ditarik ke dua angka sekaligus.
        SegmenIos(
            label = "Butuh Review",
            aktif = review,
            onKlik = { viewModel.pilihTab(TabPettyCash.REVIEW) },
            modifier = Modifier.weight(1f),
            lencana = cacah(state.jumlahReview),
            warnaLencana = if (review) WarnaIos.Aksen else WarnaIos.Abu,
        )
        SegmenIos(
            label = "Riwayat",
            aktif = !review,
            onKlik = { viewModel.pilihTab(TabPettyCash.RIWAYAT) },
            modifier = Modifier.weight(1f),
            lencana = cacah(state.jumlahRiwayat),
            warnaLencana = if (!review) WarnaIos.Aksen else WarnaIos.Abu,
        )
    }
}

@Composable
private fun BarisFilterStatus(state: PettyCashUiState, viewModel: PettyCashViewModel) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (state.tab == TabPettyCash.REVIEW) {
            FilterReview.entries.forEach { filter ->
                KapsulPilihanIos(filter.label, state.filterReview == filter, { viewModel.pilihFilterReview(filter) })
            }
        } else {
            FilterRiwayat.entries.forEach { filter ->
                KapsulPilihanIos(filter.label, state.filterRiwayat == filter, { viewModel.pilihFilterRiwayat(filter) })
            }
        }
    }
}

/**
 * Catatan untuk role yang boleh melihat tapi tidak boleh memproses — regional
 * manager termasuk di dalamnya. Web menampilkan tombolnya lalu membiarkan galat
 * server muncul; menjelaskannya lebih dulu menghemat satu tap yang pasti gagal.
 */
@Composable
private fun CatatanTanpaWewenang() {
    Row(
        Modifier
            .fillMaxWidth()
            .permukaanIos()
            .padding(horizontal = UkuranIos.PaddingKartu, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(30.dp).clip(CircleShape).background(NadaIos.PERINGATAN.warna.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(IkonIos.Lock, null, tint = NadaIos.PERINGATAN.warna, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(
            "Anda dapat memantau pengajuan di sini, tetapi persetujuan petty cash " +
                "dikerjakan oleh Area Manager outlet bersangkutan.",
            Modifier.weight(1f),
            style = TipeIos.Catatan.copy(color = NadaIos.PERINGATAN.teks),
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
        // Cabang ditulis eksplisit if/else, BUKAN keluar-awal `return@Column`.
        // `Column` adalah fungsi inline: keluar dari lambdanya setelah sudah
        // memancarkan composable meninggalkan pembukuan grup kompilator tidak
        // seimbang, dan begitu keadaannya berbalik, tabel slot dibaca dengan
        // indeks negatif -> ArrayIndexOutOfBoundsException di SlotTableKt.key.
        if (state.memuat && state.semua.isEmpty()) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 28.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(Modifier.size(16.dp), color = WarnaIos.Abu, strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text("Memuat pengajuan...", style = TipeIos.SubJudul)
            }
        } else if (state.tab == TabPettyCash.REVIEW) {
            KeadaanIos(
                ikon = IkonIos.CheckCircle,
                judul = "Semua selesai",
                pesan = "Tidak ada data yang sesuai filter saat ini.",
                nada = NadaIos.SUKSES,
            )
        } else {
            PanelKosong("Tidak ada riwayat yang ditemukan.")
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
            Icon(IkonIos.Schedule, null, tint = WarnaIos.LabelKedua, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(4.dp))
            Text(waktuRelatif(topup.dibuatPada), Modifier.weight(1f), style = TipeIos.Kecil, maxLines = 1)
            Text("ID ${topup.idRingkas}", style = TipeIos.Kecil, maxLines = 1)
        }

        Spacer(Modifier.height(10.dp))
        Text(alasan.ifBlank { "Tanpa keterangan" }, style = TipeIos.Utama)

        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(IkonIos.Person, null, tint = WarnaIos.LabelKedua, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(5.dp))
            Text(topup.pengajuNama, style = TipeIos.Catatan)
        }
        Spacer(Modifier.height(2.dp))
        Text(
            topup.rekeningTeks ?: "Informasi rekening tidak tersedia",
            style = TipeIos.Catatan.copy(
                color = if (topup.rekeningTeks != null) WarnaIos.Label else WarnaIos.LabelKedua,
            ),
        )

        if (catatanFinance != null) {
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(UkuranIos.SudutBlok)
                    .background(WarnaIos.Latar)
                    .padding(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(IkonIos.Description, null, tint = WarnaIos.Biru, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Column {
                    LabelSeksiIos("Catatan dari Finance")
                    Spacer(Modifier.height(3.dp))
                    Text(
                        rapikanCatatanFinance(catatanFinance),
                        style = TipeIos.SubJudul.copy(color = WarnaIos.Label, fontSize = 14.sp),
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        PemisahIos(inset = 0.dp)
        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (tab == TabPettyCash.REVIEW) topup.status.label else "Total Pengajuan",
                    style = TipeIos.Catatan,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(rupiah(topup.jumlah), style = TipeIos.Angka, maxLines = 1)
            }
            if (tab == TabPettyCash.RIWAYAT) {
                Spacer(Modifier.width(8.dp))
                LencanaStatus(topup.status)
            }
        }

        if (tab == TabPettyCash.REVIEW && aksi != AksiTopup.TIDAK_ADA && bolehMemproses) {
            Spacer(Modifier.height(14.dp))
            when (aksi) {
                AksiTopup.ACC_ATAU_TOLAK -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TombolKeduaIos(
                        teks = "Tolak",
                        onKlik = { onTolak() },
                        modifier = Modifier.weight(1f),
                        aktif = !sedangDiproses,
                        ikon = IkonIos.Close,
                        warna = WarnaIos.Merah,
                    )
                    TombolUtamaIos(
                        teks = "ACC",
                        onKlik = { onSetujui() },
                        modifier = Modifier.weight(1f),
                        memuat = sedangDiproses,
                        ikon = IkonIos.Check,
                        warna = WarnaIos.Hijau,
                    )
                }
                AksiTopup.SERAHKAN -> TombolUtamaIos(
                    teks = "Serahkan ke Leader",
                    onKlik = { onSerahkan() },
                    memuat = sedangDiproses,
                    ikon = IkonIos.ArrowForward,
                )
                AksiTopup.TIDAK_ADA -> Unit
            }
        }

        if (topup.buktiTransferUrl != null) {
            Spacer(Modifier.height(12.dp))
            TombolKapsulIos(
                teks = "Lihat bukti transfer",
                onKlik = { buktiTerbuka = true },
                ikon = IkonIos.Image,
            )
        }
    }

    if (buktiTerbuka && topup.buktiTransferUrl != null) {
        DialogBukti(topup.buktiTransferUrl) { buktiTerbuka = false }
    }
}

@Composable
private fun LencanaStatus(status: StatusTopup) {
    val nada = when (status) {
        StatusTopup.DITERUSKAN_KE_FINANCE -> NadaIos.PERINGATAN
        StatusTopup.DITOLAK -> NadaIos.BAHAYA
        else -> NadaIos.SUKSES
    }
    LencanaIos(status.label, nada)
}

@Composable
private fun DialogBukti(url: String, onTutup: () -> Unit) {
    Dialog(onDismissRequest = onTutup) {
        Column(Modifier.clip(UkuranIos.SudutKartu).background(WarnaIos.Kartu)) {
            Row(
                Modifier.padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Lampiran Bukti Transfer",
                    Modifier.weight(1f),
                    style = TipeIos.Utama,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                TombolBundarIos(IkonIos.Close, "Tutup", onTutup)
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
