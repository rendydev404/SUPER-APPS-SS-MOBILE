package com.sukashawarma.superapp.feature.leader.ui.ringkasan

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.RealtimeTables
import com.sukashawarma.superapp.feature.leader.domain.RingkasanLeader
import com.sukashawarma.superapp.feature.leader.domain.cacah
import com.sukashawarma.superapp.feature.leader.domain.rupiah
import com.sukashawarma.superapp.feature.leader.domain.waktuJakarta
import com.sukashawarma.superapp.feature.leader.ui.AmberGaris
import com.sukashawarma.superapp.feature.leader.ui.AmberLatar
import com.sukashawarma.superapp.feature.leader.ui.AmberTeks
import com.sukashawarma.superapp.feature.leader.ui.BiruTeks
import com.sukashawarma.superapp.feature.leader.ui.GarisKartu
import com.sukashawarma.superapp.feature.leader.ui.KartuAngka
import com.sukashawarma.superapp.feature.leader.ui.KartuPanel
import com.sukashawarma.superapp.feature.leader.ui.MerahGaris
import com.sukashawarma.superapp.feature.leader.ui.MerahLatar
import com.sukashawarma.superapp.feature.leader.ui.MerahTeks
import com.sukashawarma.superapp.feature.leader.ui.PanelKosong
import com.sukashawarma.superapp.feature.leader.ui.PilStatus
import com.sukashawarma.superapp.presentation.theme.SukaBrown
import com.sukashawarma.superapp.presentation.theme.SukaCream
import com.sukashawarma.superapp.presentation.theme.SukaGray400
import com.sukashawarma.superapp.presentation.theme.SukaOrange

private val HIJAU = Color(0xFF10B981)
private val AMBER = Color(0xFFF59E0B)
private val BIRU = Color(0xFF3B82F6)

/**
 * Ringkasan Leader — cermin `app/dashboard/leader/page.tsx` web.
 *
 * Angka omzet mencakup SELURUH cabang binaan; petty cash, stok, dan kehadiran hanya
 * cabang utama. Pembagian itu bukan kelalaian: saldo petty cash terikat satu shift
 * di satu outlet, jadi menjumlahkannya lintas cabang akan menghasilkan angka yang
 * tidak dipegang siapa pun. Web membaginya dengan cara yang sama.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RingkasanScreen(
    onExit: () -> Unit,
    onBukaPettyCash: () -> Unit,
    onBukaStok: () -> Unit,
    onBukaPenjualan: () -> Unit,
    viewModel: RingkasanViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    // Omzet bergerak begitu kasir menutup pesanan; saldo petty cash begitu dana
    // diserahkan; kehadiran begitu kru menempelkan wajahnya. Ketiganya menyusun satu
    // layar, jadi ketiganya memicu pemuatan ulang yang sama.
    RealtimeRefresh(
        RealtimeTables.ORDERS,
        RealtimeTables.PETTY_CASH_TOPUPS,
        RealtimeTables.ATTENDANCE,
    ) { viewModel.muatUlang() }

    LaunchedEffect(state.galat) {
        state.galat?.let { snackbar.showSnackbar(it) }
    }

    Scaffold(
        containerColor = SukaCream,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Ringkasan Leader",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = SukaBrown,
                        maxLines = 1,
                    )
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
        val data = state.data
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { Kepala(data, state.memuat) }
            if (!state.pertamaKali && !data.punyaOutlet) {
                item { PeringatanTanpaOutlet() }
            }
            item { KartuOmzet(data) }
            item { KartuPettyCash(data) }
            item { BarisStatKecil(data) }
            item { PanelPerCabang(data, onBukaPenjualan) }
            item { AksiCepat(onBukaPettyCash, onBukaStok) }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun Kepala(data: RingkasanLeader, memuat: Boolean) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                "DASHBOARD UTAMA",
                color = SukaOrange,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                data.judul,
                color = SukaBrown,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (memuat) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = SukaOrange)
        }
    }
}

/**
 * Peringatan akun tanpa cabang.
 *
 * Sengaja menjelaskan apa yang MASIH berjalan (omzet POS tetap mengikuti hak akses
 * akun) dan apa yang tidak, supaya leader tidak menyimpulkan seluruh layar rusak
 * hanya karena tiga kartu berisi nol.
 */
@Composable
private fun PeringatanTanpaOutlet() {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = AmberLatar,
        border = BorderStroke(1.dp, AmberGaris),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.WarningAmber, null, tint = AmberTeks, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    "Akun belum ditugaskan ke cabang",
                    color = AmberTeks,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "Omzet POS di bawah tetap mengikuti hak akses akun Anda. Hubungi admin " +
                        "untuk penugasan outlet agar petty cash, stok, dan kehadiran ikut tampil.",
                    color = AmberTeks.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 16.sp,
                )
            }
        }
    }
}

@Composable
private fun KartuOmzet(data: RingkasanLeader) {
    val keterangan = buildString {
        append("${cacah(data.jumlahTransaksi)} transaksi")
        data.jamTransaksiTerakhir?.let { append(" · terakhir $it") }
    }
    KartuAngka(
        judul = "Omzet POS Hari Ini",
        nilai = rupiah(data.omzetHariIni),
        keterangan = keterangan,
        ikon = Icons.Default.TrendingUp,
        warnaIkon = SukaOrange,
        ukuranNilai = 30,
    )
}

@Composable
private fun KartuPettyCash(data: RingkasanLeader) {
    KartuAngka(
        judul = "Sisa Petty Cash",
        nilai = rupiah(data.sisaPettyCash),
        keterangan = if (data.adaShiftAktif) "Shift aktif" else "Tidak ada shift berjalan",
        ikon = Icons.Default.Payments,
        warnaIkon = HIJAU,
        warnaNilai = if (data.pettyCashKritis) MerahTeks else SukaBrown,
        ukuranNilai = 30,
    ) {
        Column {
            if (data.pettyCashKritis) {
                PilStatus("Kritis", MerahLatar, MerahGaris, MerahTeks)
            }
            // Penyesuaian admin diberitahukan apa adanya: saldo yang tiba-tiba
            // berbeda dari hitungan leader punya penjelasan, dan penjelasan itu
            // ada di catatan ini.
            data.shift?.disesuaikanPada?.let { pada ->
                if (data.pettyCashKritis) Spacer(Modifier.height(6.dp))
                val catatan = data.shift.catatanAdmin?.takeIf { it.isNotBlank() }
                Text(
                    "Disesuaikan admin ${waktuJakarta(pada)}" + (catatan?.let { ": $it" } ?: ""),
                    color = BiruTeks,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 15.sp,
                )
            }
        }
    }
}

@Composable
private fun BarisStatKecil(data: RingkasanLeader) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            KartuAngka(
                judul = "Rata-rata / Transaksi",
                nilai = rupiah(data.rataRataTransaksi),
                keterangan = "Omzet dibagi transaksi",
                ikon = Icons.Default.ReceiptLong,
                warnaIkon = SukaOrange,
                modifier = Modifier.weight(1f),
                ukuranNilai = 18,
            )
            KartuAngka(
                judul = "Stok Cabang",
                nilai = cacah(data.jumlahItemStok),
                keterangan = "Bahan tersedia",
                ikon = Icons.Default.Inventory2,
                warnaIkon = AMBER,
                modifier = Modifier.weight(1f),
                ukuranNilai = 18,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            KartuAngka(
                judul = "Kehadiran",
                nilai = "${data.hadir}/${data.totalKru}",
                keterangan = "Tim hadir hari ini",
                ikon = Icons.Default.Groups,
                warnaIkon = BIRU,
                modifier = Modifier.weight(1f),
                ukuranNilai = 18,
            )
            KartuAngka(
                judul = "Cabang Binaan",
                nilai = cacah(data.jumlahCabang),
                keterangan = "Outlet dalam akses Anda",
                ikon = Icons.Default.Storefront,
                warnaIkon = SukaGray400,
                modifier = Modifier.weight(1f),
                ukuranNilai = 18,
            )
        }
    }
}

@Composable
private fun PanelPerCabang(data: RingkasanLeader, onBukaPenjualan: () -> Unit) {
    KartuPanel {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "OMZET PER CABANG",
                Modifier.weight(1f),
                color = SukaBrown,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp,
            )
            Text(
                "Detail",
                Modifier.clickable(onClick = onBukaPenjualan).padding(start = 8.dp),
                color = SukaOrange,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
            )
        }
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = GarisKartu)
        Spacer(Modifier.height(14.dp))
        if (data.perOutlet.isEmpty()) {
            PanelKosong("Belum ada cabang binaan.")
        } else {
            data.perOutlet.forEachIndexed { index, baris ->
                if (index > 0) {
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = GarisKartu)
                    Spacer(Modifier.height(10.dp))
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).padding(end = 10.dp)) {
                        Text(
                            baris.nama,
                            color = SukaBrown,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "${cacah(baris.transaksi)} transaksi hari ini",
                            color = SukaGray400,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    Text(
                        rupiah(baris.omzet),
                        color = SukaBrown,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun AksiCepat(onBukaPettyCash: () -> Unit, onBukaStok: () -> Unit) {
    Column {
        Text(
            "AKSI CEPAT",
            color = SukaBrown,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.8.sp,
        )
        Spacer(Modifier.height(10.dp))
        TombolAksi(
            judul = "Top Up Petty Cash",
            keterangan = "Ajukan pencairan dana operasional",
            ikon = Icons.Default.Payments,
            utama = true,
            onKlik = onBukaPettyCash,
        )
        Spacer(Modifier.height(10.dp))
        TombolAksi(
            judul = "Cek Stok Cabang",
            keterangan = "Pantau sisa bahan baku cabang",
            ikon = Icons.Default.Inventory2,
            utama = false,
            onKlik = onBukaStok,
        )
    }
}

@Composable
private fun TombolAksi(
    judul: String,
    keterangan: String,
    ikon: ImageVector,
    utama: Boolean,
    onKlik: () -> Unit,
) {
    Surface(
        Modifier.fillMaxWidth().clickable(onClick = onKlik),
        shape = RoundedCornerShape(18.dp),
        color = if (utama) SukaBrown else Color.White,
        border = BorderStroke(1.dp, if (utama) SukaBrown else GarisKartu),
        shadowElevation = 2.dp,
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                ikon,
                null,
                tint = if (utama) Color.White else SukaOrange,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    judul,
                    color = if (utama) Color.White else SukaBrown,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    keterangan,
                    color = if (utama) Color.White.copy(alpha = 0.7f) else SukaGray400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Box(
                Modifier
                    .size(30.dp)
                    .background(
                        if (utama) Color.White.copy(alpha = 0.15f) else SukaOrange.copy(alpha = 0.10f),
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    null,
                    tint = if (utama) Color.White else SukaOrange,
                    modifier = Modifier.size(15.dp),
                )
            }
        }
    }
}
