package com.sukashawarma.superapp.feature.leader.ui.ringkasan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.RealtimeTables
import com.sukashawarma.superapp.core.ui.ios.BarisIos
import com.sukashawarma.superapp.core.ui.ios.GrupIos
import com.sukashawarma.superapp.core.ui.ios.JudulSeksiIos
import com.sukashawarma.superapp.core.ui.ios.KartuIos
import com.sukashawarma.superapp.core.ui.ios.LabelSeksiIos
import com.sukashawarma.superapp.core.ui.ios.LencanaIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.PemisahIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.ios.tekanIos
import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.core.ui.kaca.denganRuangNav
import com.sukashawarma.superapp.core.ui.kaca.navigationBarsPaddingKaca
import com.sukashawarma.superapp.feature.leader.domain.RingkasanLeader
import com.sukashawarma.superapp.feature.leader.domain.cacah
import com.sukashawarma.superapp.feature.leader.domain.rupiah
import com.sukashawarma.superapp.feature.leader.domain.waktuJakarta
import com.sukashawarma.superapp.feature.leader.ui.BilahJudulLeader
import com.sukashawarma.superapp.feature.leader.ui.KartuAngka
import com.sukashawarma.superapp.feature.leader.ui.PanelKosong

/**
 * Ringkasan Leader — cermin `app/dashboard/leader/page.tsx` web.
 *
 * Angka omzet mencakup SELURUH cabang binaan; petty cash, stok, dan kehadiran hanya
 * cabang utama. Pembagian itu bukan kelalaian: saldo petty cash terikat satu shift
 * di satu outlet, jadi menjumlahkannya lintas cabang akan menghasilkan angka yang
 * tidak dipegang siapa pun. Web membaginya dengan cara yang sama.
 */
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
        containerColor = WarnaIos.Latar,
        snackbarHost = { SnackbarHost(snackbar, Modifier.navigationBarsPaddingKaca()) },
        topBar = { BilahJudulLeader("Ringkasan Leader", onExit, viewModel::muatUlang) },
    ) { padding ->
        val data = state.data
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
            item { Kepala(data, state.memuat) }
            if (!state.pertamaKali && !data.punyaOutlet) {
                item { PeringatanTanpaOutlet() }
            }
            item { KartuOmzet(data) }
            item { KartuPettyCash(data) }
            item { BarisStatKecil(data) }
            item { PanelPerCabang(data, onBukaPenjualan) }
            item { AksiCepat(onBukaPettyCash, onBukaStok) }
        }
    }
}

@Composable
private fun Kepala(data: RingkasanLeader, memuat: Boolean) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            LabelSeksiIos("Dashboard utama")
            Spacer(Modifier.height(2.dp))
            Text(data.judul, style = TipeIos.Judul1, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (memuat) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = WarnaIos.Aksen)
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
    KartuIos {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                Modifier.size(30.dp).clip(CircleShape).background(NadaIos.PERINGATAN.warna.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(IkonIos.WarningAmber, null, tint = NadaIos.PERINGATAN.warna, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    "Akun belum ditugaskan ke cabang",
                    style = TipeIos.Utama.copy(fontSize = 15.sp, color = NadaIos.PERINGATAN.teks),
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "Omzet POS di bawah tetap mengikuti hak akses akun Anda. Hubungi admin " +
                        "untuk penugasan outlet agar petty cash, stok, dan kehadiran ikut tampil.",
                    style = TipeIos.Catatan,
                    lineHeight = 18.sp,
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
        ikon = IkonIos.TrendingUp,
        warnaIkon = WarnaIos.Aksen,
        ukuranNilai = 30,
    )
}

@Composable
private fun KartuPettyCash(data: RingkasanLeader) {
    KartuAngka(
        judul = "Sisa Petty Cash",
        nilai = rupiah(data.sisaPettyCash),
        keterangan = if (data.adaShiftAktif) "Shift aktif" else "Tidak ada shift berjalan",
        ikon = IkonIos.Payments,
        warnaIkon = WarnaIos.Hijau,
        warnaNilai = if (data.pettyCashKritis) NadaIos.BAHAYA.teks else WarnaIos.Label,
        ukuranNilai = 30,
    ) {
        Column {
            if (data.pettyCashKritis) {
                LencanaIos("Kritis", NadaIos.BAHAYA)
            }
            // Penyesuaian admin diberitahukan apa adanya: saldo yang tiba-tiba
            // berbeda dari hitungan leader punya penjelasan, dan penjelasan itu
            // ada di catatan ini.
            data.shift?.disesuaikanPada?.let { pada ->
                if (data.pettyCashKritis) Spacer(Modifier.height(6.dp))
                val catatan = data.shift.catatanAdmin?.takeIf { it.isNotBlank() }
                Text(
                    "Disesuaikan admin ${waktuJakarta(pada)}" + (catatan?.let { ": $it" } ?: ""),
                    style = TipeIos.Catatan.copy(color = NadaIos.INFO.teks, fontWeight = FontWeight.Medium),
                    lineHeight = 17.sp,
                )
            }
        }
    }
}

@Composable
private fun BarisStatKecil(data: RingkasanLeader) {
    // Tinggi intrinsik menyamakan dua petak sebaris walau labelnya terbungkus dua
    // baris di salah satunya; tanpa itu grid 2×2 tampak bergerigi.
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KartuAngka(
                judul = "Rata-rata / Transaksi",
                nilai = rupiah(data.rataRataTransaksi),
                keterangan = "Omzet dibagi transaksi",
                ikon = IkonIos.ReceiptLong,
                warnaIkon = WarnaIos.Aksen,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                ukuranNilai = 20,
            )
            KartuAngka(
                judul = "Stok Cabang",
                nilai = "${cacah(data.stok.kritis)} Kritis",
                keterangan = if (data.stok.kritis == 0 && data.stok.menipis == 0) {
                    "Semua aman"
                } else {
                    "${cacah(data.stok.menipis)} menipis"
                },
                ikon = IkonIos.Inventory2,
                warnaIkon = WarnaIos.Oranye,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                ukuranNilai = 20,
            )
        }
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KartuAngka(
                judul = "Kehadiran",
                nilai = "${data.hadir}/${data.totalKru}",
                keterangan = "Tim hadir hari ini",
                ikon = IkonIos.Groups,
                warnaIkon = WarnaIos.Biru,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                ukuranNilai = 20,
            )
            KartuAngka(
                judul = "Cabang Binaan",
                nilai = cacah(data.jumlahCabang),
                keterangan = "Outlet dalam akses Anda",
                ikon = IkonIos.Storefront,
                warnaIkon = WarnaIos.AbuGelap,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                ukuranNilai = 20,
            )
        }
    }
}

@Composable
private fun PanelPerCabang(data: RingkasanLeader, onBukaPenjualan: () -> Unit) {
    Column {
        JudulSeksiIos("Omzet per Cabang", Modifier.padding(bottom = 8.dp)) {
            Text(
                "Detail",
                Modifier.tekanIos(onBukaPenjualan).padding(start = 8.dp),
                color = WarnaIos.Aksen,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        GrupIos {
            if (data.perOutlet.isEmpty()) {
                PanelKosong("Belum ada cabang binaan.", IkonIos.Storefront)
            } else {
                data.perOutlet.forEachIndexed { index, baris ->
                    if (index > 0) PemisahIos()
                    BarisIos(
                        judul = baris.nama,
                        keterangan = "${cacah(baris.transaksi)} transaksi hari ini",
                        trailing = {
                            Text(
                                rupiah(baris.omzet),
                                style = TipeIos.Keterangan.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 1,
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun AksiCepat(onBukaPettyCash: () -> Unit, onBukaStok: () -> Unit) {
    Column {
        JudulSeksiIos("Aksi Cepat", Modifier.padding(bottom = 8.dp))
        GrupIos {
            BarisIos(
                judul = "Top Up Petty Cash",
                keterangan = "Ajukan pencairan dana operasional",
                ikon = IkonIos.Payments,
                nadaIkon = NadaIos.AKSEN,
                onKlik = onBukaPettyCash,
            )
            PemisahIos(inset = 58.dp)
            BarisIos(
                judul = "Cek Stok Cabang",
                keterangan = "Pantau sisa bahan baku cabang",
                ikon = IkonIos.Inventory2,
                nadaIkon = NadaIos.PERINGATAN,
                onKlik = onBukaStok,
            )
        }
    }
}
