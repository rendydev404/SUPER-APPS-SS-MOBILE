package com.sukashawarma.superapp.feature.distribusi.ui.dashboard

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.RealtimeTables
import com.sukashawarma.superapp.core.ui.ios.BarisIos
import com.sukashawarma.superapp.core.ui.ios.BilahJudulIos
import com.sukashawarma.superapp.core.ui.ios.GrupIos
import com.sukashawarma.superapp.core.ui.ios.JudulSeksiIos
import com.sukashawarma.superapp.core.ui.ios.KartuIos
import com.sukashawarma.superapp.core.ui.ios.KeadaanIos
import com.sukashawarma.superapp.core.ui.ios.KolomCariIos
import com.sukashawarma.superapp.core.ui.ios.LabelSeksiIos
import com.sukashawarma.superapp.core.ui.ios.LencanaIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.PemisahIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.TombolBundarIos
import com.sukashawarma.superapp.core.ui.ios.TombolUtamaIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.ios.bayanganIos
import com.sukashawarma.superapp.core.ui.ios.permukaanIos
import com.sukashawarma.superapp.core.ui.ios.tekanIos
import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.core.ui.kaca.denganRuangNav
import com.sukashawarma.superapp.core.ui.kaca.navigationBarsPaddingKaca
import com.sukashawarma.superapp.feature.distribusi.data.model.RentangTanggal
import com.sukashawarma.superapp.feature.distribusi.data.model.SuratJalanRingkas
import com.sukashawarma.superapp.feature.distribusi.domain.bolehDitutup
import com.sukashawarma.superapp.feature.distribusi.domain.salamSekarang
import com.sukashawarma.superapp.feature.distribusi.domain.tanggalPanjangHariIni
import com.sukashawarma.superapp.feature.distribusi.ui.KartuSuratJalan
import com.sukashawarma.superapp.feature.distribusi.ui.LayarGalat
import com.sukashawarma.superapp.feature.distribusi.ui.LayarMemuat
import com.sukashawarma.superapp.feature.distribusi.ui.SegarkanSaatAktif
import com.sukashawarma.superapp.feature.distribusi.ui.ShellDistribusi
import com.sukashawarma.superapp.feature.distribusi.ui.TabBawah

@Composable
fun DashboardScreen(
    onKeluar: () -> Unit,
    onBukaScan: () -> Unit,
    onBukaRiwayat: () -> Unit,
    onBukaDetail: (String) -> Unit,
    viewModel: DashboardViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    RealtimeRefresh(RealtimeTables.SURAT_JALAN) { viewModel.muat(paksa = true) }
    var konfirmasiTutup by remember { mutableStateOf<SuratJalanRingkas?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val daftarState = rememberLazyListState()

    // LaunchedEffect ikut berjalan pada komposisi pertamanya. Composable ini baru
    // menyusun LazyColumn setelah data tiba, jadi jalan pertama itu jatuh persis
    // saat layar dibuka -- dan menggulirkan pengguna melewati banner, statistik,
    // dan kolom cari. Gulir hanya boleh terjadi karena pengguna pindah halaman.
    var gulirPertamaDilewati by remember { mutableStateOf(false) }
    val halamanSuratJalan = halamanSuratJalan(state.terlihat, state.halamanAktif)
    // Indeks item daftar konstan untuk dua susunan: dengan atau tanpa filter outlet.
    // Jumlah item di atas daftar sengaja dijaga sama saat tampilan diubah.
    val indeksDaftar = if (state.rincianOutlet.size > 1) 5 else 3

    SegarkanSaatAktif { viewModel.muat(paksa = true) }

    if (state.memuat && state.semua.isEmpty()) { LayarMemuat(); return }
    if (state.error != null && state.semua.isEmpty()) {
        LayarGalat(state.error!!) { viewModel.muat(paksa = true) }
        return
    }

    // Setelah daftar terisi, galat/pesan lewat snackbar — bukan LayarGalat layar
    // penuh, yang hanya dipakai untuk kegagalan pemuatan awal di atas.
    LaunchedEffect(state.pesan, state.error) {
        val teks = state.pesan ?: state.error
        if (teks != null) {
            snackbarHostState.showSnackbar(teks)
            viewModel.bersihkanPesan()
        }
    }

    LaunchedEffect(state.halamanAktif) {
        if (!gulirPertamaDilewati) {
            gulirPertamaDilewati = true
        } else if (state.terlihat.isNotEmpty()) {
            daftarState.animateScrollToItem(indeksDaftar)
        }
    }

    ShellDistribusi(
        aktif = TabBawah.DASHBOARD,
        bolehVerifikasi = state.bolehVerifikasi,
        onDashboard = {},
        onScan = onBukaScan,
        onRiwayat = onBukaRiwayat,
    ) {
        Scaffold(
            containerColor = WarnaIos.Latar,
            snackbarHost = { SnackbarHost(snackbarHostState, Modifier.navigationBarsPaddingKaca()) },
        ) { paddingValues ->
            Column(Modifier.fillMaxSize().padding(paddingValues).background(WarnaIos.Latar)) {
                KepalaDashboard(
                    namaOutlet = state.namaOutlet,
                    namaPengguna = state.namaPengguna,
                    onKeluar = onKeluar,
                    onSegarkan = { viewModel.muat(paksa = true) },
                )

                LazyColumn(
                    Modifier.fillMaxSize(),
                    state = daftarState,
                    contentPadding = PaddingValues(
                        start = UkuranIos.TepiLayar,
                        end = UkuranIos.TepiLayar,
                        top = 12.dp,
                        bottom = 24.dp,
                    ).denganRuangNav(),
                    verticalArrangement = Arrangement.spacedBy(UkuranIos.JarakKartu),
                ) {
                    item {
                        BannerHero(
                            namaOutlet = state.namaOutlet,
                            rentang = state.rentang,
                            bolehVerifikasi = state.bolehVerifikasi,
                            onUbahRentang = viewModel::ubahRentang,
                            onScan = onBukaScan,
                        )
                    }

                    item {
                        GridStatistik(
                            state = state,
                            onPilihTab = viewModel::ubahTab,
                        )
                    }

                    item {
                        KolomCariIos(
                            nilai = state.cari,
                            onUbah = viewModel::ubahCari,
                            placeholder = "Cari nomor SJ atau outlet",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    if (state.rincianOutlet.size > 1) {
                        item {
                            LabelSeksiIos("Filter Outlet", Modifier.padding(start = 16.dp, top = 4.dp))
                        }
                        item {
                            // Daftar pilihan ala Pengaturan iOS: satu grup, tanda centang
                            // pada outlet aktif — menekan lagi tetap memanggil pilihOutlet.
                            GrupIos {
                                state.rincianOutlet.forEachIndexed { i, outlet ->
                                    if (i > 0) PemisahIos()
                                    val aktif = state.outletTerpilih == outlet.nama
                                    BarisIos(
                                        judul = outlet.nama,
                                        nilai = outlet.total.toString(),
                                        onKlik = { viewModel.pilihOutlet(outlet.nama) },
                                        chevron = false,
                                        trailing = if (aktif) {
                                            {
                                                Icon(
                                                    IkonIos.Check,
                                                    contentDescription = "Terpilih",
                                                    tint = WarnaIos.Aksen,
                                                    modifier = Modifier.size(18.dp),
                                                )
                                            }
                                        } else {
                                            null
                                        },
                                    )
                                }
                            }
                        }
                    }

                    if (state.terlihat.isEmpty()) {
                        item {
                            KeadaanIos(
                                IkonIos.Inbox,
                                "Tidak Ada Surat Jalan",
                                "Tidak ada yang cocok dengan filter ini.",
                            )
                        }
                    } else {
                        item {
                            KepalaDaftarSuratJalan(halamanSuratJalan)
                        }

                        items(halamanSuratJalan.baris, key = { it.id }) { baris ->
                            val bolehTutup = state.bolehTutupDokumen && baris.status?.bolehDitutup == true
                            val sedangDiproses = state.sedangMenutup == baris.id
                            KartuSuratJalan(
                                baris = baris,
                                aksiLabel = when {
                                    !bolehTutup -> null
                                    sedangDiproses -> "Menutup..."
                                    else -> "Tutup Dokumen"
                                },
                                onKlik = { onBukaDetail(baris.id) },
                                onAksi = if (bolehTutup && !sedangDiproses) {
                                    { konfirmasiTutup = baris }
                                } else {
                                    null
                                },
                            )
                        }

                        item {
                            KontrolPaginationSuratJalan(
                                halaman = halamanSuratJalan,
                                onPilihHalaman = viewModel::pindahHalaman,
                            )
                        }
                    }
                }
            }
        }
    }

    konfirmasiTutup?.let { baris ->
        AlertDialog(
            onDismissRequest = { konfirmasiTutup = null },
            containerColor = WarnaIos.Kartu,
            shape = UkuranIos.SudutKartu,
            titleContentColor = WarnaIos.Label,
            textContentColor = WarnaIos.LabelKedua,
            title = { Text("Tutup dokumen?", style = TipeIos.Utama) },
            text = {
                Text(
                    "Surat jalan ${baris.nomorDokumen ?: baris.id.take(8)} akan ditandai selesai " +
                        "dan tidak bisa dibuka kembali dari aplikasi.",
                    style = TipeIos.SubJudul,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.tutupDokumen(baris)
                    konfirmasiTutup = null
                }) { Text("Tutup Dokumen", color = WarnaIos.Aksen, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { konfirmasiTutup = null }) {
                    Text("Batal", color = WarnaIos.Aksen)
                }
            },
        )
    }
}

@Composable
private fun KepalaDaftarSuratJalan(halaman: HalamanSuratJalan) {
    Column {
        JudulSeksiIos("Daftar Surat Jalan", keterangan = "${halaman.totalBaris} SJ")
        Text(
            "Terbaru di atas • 5 dokumen per halaman",
            Modifier.padding(horizontal = 4.dp),
            style = TipeIos.Catatan,
        )
    }
}

@Composable
private fun KontrolPaginationSuratJalan(
    halaman: HalamanSuratJalan,
    onPilihHalaman: (Int) -> Unit,
) {
    KartuIos(padding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TombolBundarIos(
                ikon = IkonIos.ArrowBack,
                keterangan = "Halaman sebelumnya",
                aktif = halaman.nomor > 1,
                onKlik = { onPilihHalaman(halaman.nomor - 1) },
            )
            Column(
                Modifier.weight(1f).padding(horizontal = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Halaman ${halaman.nomor} / ${halaman.totalHalaman}",
                    style = TipeIos.Utama,
                    maxLines = 1,
                )
                Text(
                    "Menampilkan ${halaman.urutanMulai}–${halaman.urutanAkhir} dari ${halaman.totalBaris} SJ",
                    style = TipeIos.Catatan,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TombolBundarIos(
                ikon = IkonIos.ArrowForward,
                keterangan = "Halaman berikutnya",
                aktif = halaman.nomor < halaman.totalHalaman,
                onKlik = { onPilihHalaman(halaman.nomor + 1) },
            )
        }
    }
}

/** Bilah judul iOS dengan inisial pengguna, pengganti komponen Avatar web. */
@Composable
private fun KepalaDashboard(
    namaOutlet: String,
    namaPengguna: String,
    onKeluar: () -> Unit,
    onSegarkan: () -> Unit,
) {
    BilahJudulIos(
        judul = "Pusat Komando Distribusi",
        subjudul = "Outlet Supply Unit • ${namaOutlet.ifBlank { "Outlet" }}",
        onKembali = onKeluar,
    ) {
        TombolBundarIos(IkonIos.Refresh, "Segarkan", onSegarkan)
        Box(
            Modifier.size(38.dp).clip(CircleShape).background(WarnaIos.Aksen.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                namaPengguna.take(2).uppercase().ifBlank { "?" },
                color = NadaIos.AKSEN.teks,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/** Kartu pembuka berisi sapaan, judul, filter tanggal, dan aksi utama. */
@Composable
private fun BannerHero(
    namaOutlet: String,
    rentang: RentangTanggal,
    bolehVerifikasi: Boolean,
    onUbahRentang: (RentangTanggal) -> Unit,
    onScan: () -> Unit,
) {
    KartuIos {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LencanaIos(salamSekarang(), NadaIos.AKSEN, titik = false)
            Spacer(Modifier.width(8.dp))
            Text(
                tanggalPanjangHariIni(),
                style = TipeIos.Catatan,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.height(10.dp))
        Text(
            "Penerimaan Logistik ${namaOutlet.ifBlank { "Outlet" }}",
            style = TipeIos.Judul2,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Pastikan barang fisik yang tiba dicocokkan dengan manifes surat jalan " +
                "dan bubuhkan tanda tangan penerima sebelum dimasukkan ke kartu stok outlet.",
            style = TipeIos.SubJudul,
        )

        Spacer(Modifier.height(14.dp))
        // Segmented control iOS: isian abu, segmen aktif berupa pil putih berbayang.
        Row(
            Modifier
                .fillMaxWidth()
                .clip(UkuranIos.SudutKontrol)
                .background(WarnaIos.Isian)
                .padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            RentangTanggal.entries.forEach { pilihan ->
                PilRentang(
                    teks = pilihan.label,
                    aktif = rentang == pilihan,
                    modifier = Modifier.weight(1f),
                ) { onUbahRentang(pilihan) }
            }
        }

        if (bolehVerifikasi) {
            Spacer(Modifier.height(14.dp))
            TombolUtamaIos("Scan QR Kedatangan", onScan, ikon = IkonIos.QrCodeScanner)
        }
    }
}

/** Lima petak statistik dalam dua kolom, urutan sama dengan HUD web. */
@Composable
private fun GridStatistik(state: DashboardUiState, onPilihTab: (TabStatus) -> Unit) {
    val adaSelisih = state.semua.count { it.adaSelisih }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PetakStatistik(
                label = "Siap Kirim",
                nilai = state.hitungan.draft.toString(),
                keterangan = "Draft pesanan pending",
                ikon = IkonIos.Description,
                warna = WarnaIos.Oranye,
                aktif = state.tab == TabStatus.DRAFT,
                modifier = Modifier.weight(1f),
            ) { onPilihTab(if (state.tab == TabStatus.DRAFT) TabStatus.SEMUA else TabStatus.DRAFT) }

            PetakStatistik(
                label = "Dalam Transit",
                nilai = state.hitungan.dikirim.toString(),
                keterangan = "Sedang di jalan menuju outlet",
                ikon = IkonIos.LocalShipping,
                warna = WarnaIos.Biru,
                aktif = state.tab == TabStatus.DIKIRIM,
                modifier = Modifier.weight(1f),
            ) { onPilihTab(if (state.tab == TabStatus.DIKIRIM) TabStatus.SEMUA else TabStatus.DIKIRIM) }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PetakStatistik(
                label = "Tiba di Outlet",
                nilai = state.hitungan.diterima.toString(),
                keterangan = "Menunggu hitung fisik & TTD",
                ikon = IkonIos.Schedule,
                warna = WarnaIos.Ungu,
                aktif = state.tab == TabStatus.BELUM_VERIF,
                modifier = Modifier.weight(1f),
            ) {
                onPilihTab(
                    if (state.tab == TabStatus.BELUM_VERIF) TabStatus.SEMUA else TabStatus.BELUM_VERIF,
                )
            }

            PetakStatistik(
                label = "Tervalidasi",
                nilai = state.hitungan.selesai.toString(),
                keterangan = "Selesai & stok otomatis update",
                ikon = IkonIos.CheckCircle,
                warna = WarnaIos.Hijau,
                aktif = state.tab == TabStatus.SELESAI,
                modifier = Modifier.weight(1f),
            ) { onPilihTab(if (state.tab == TabStatus.SELESAI) TabStatus.SEMUA else TabStatus.SELESAI) }
        }

        PetakStatistik(
            label = if (adaSelisih > 0) "$adaSelisih Selisih" else "Zero Selisih",
            nilai = "${state.akurasi}%",
            keterangan = if (adaSelisih > 0) "Perlu investigasi SPV" else "Tingkat akurasi sempurna",
            ikon = if (adaSelisih > 0) IkonIos.WarningAmber else IkonIos.FactCheck,
            warna = if (adaSelisih > 0) WarnaIos.Merah else WarnaIos.Hijau,
            aktif = state.tab == TabStatus.SELISIH,
            modifier = Modifier.fillMaxWidth(),
        ) { onPilihTab(if (state.tab == TabStatus.SELISIH) TabStatus.SEMUA else TabStatus.SELISIH) }
    }
}

/**
 * Petak ala PetakStatIos (smart list Pengingat iOS) ditambah satu baris keterangan:
 * keterangan tiap status adalah penjelasan bisnis yang ikut dari HUD web, jadi
 * tidak boleh hilang hanya karena komponen core tidak punya slotnya.
 */
@Composable
private fun PetakStatistik(
    label: String,
    nilai: String,
    keterangan: String,
    ikon: ImageVector,
    warna: Color,
    aktif: Boolean,
    modifier: Modifier = Modifier,
    onKlik: () -> Unit,
) {
    val latar by animateColorAsState(if (aktif) warna else WarnaIos.Kartu, label = "latarPetak")
    Column(
        modifier
            .permukaanIos(UkuranIos.SudutPetak, latar)
            .tekanIos(onKlik)
            .padding(start = 12.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                Modifier.size(30.dp).clip(CircleShape).background(if (aktif) Color.White else warna),
                contentAlignment = Alignment.Center,
            ) {
                Icon(IkonIos.aktif(ikon), null, tint = if (aktif) warna else Color.White, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.weight(1f))
            Text(
                nilai,
                style = TipeIos.AngkaBesar.copy(color = if (aktif) Color.White else WarnaIos.Label),
                maxLines = 1,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            color = if (aktif) Color.White.copy(alpha = 0.92f) else WarnaIos.LabelKedua,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            keterangan,
            style = TipeIos.Kecil.copy(color = if (aktif) Color.White.copy(alpha = 0.8f) else WarnaIos.LabelKedua),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PilRentang(
    teks: String,
    aktif: Boolean,
    modifier: Modifier = Modifier,
    onKlik: () -> Unit,
) {
    val bentuk = RoundedCornerShape(10.dp)
    Box(
        modifier
            .height(32.dp)
            // Bayangan segmen aktif dibuat tipis; bayangan kartu penuh terlalu berat
            // untuk pil setinggi 32dp.
            .then(
                if (aktif) Modifier.bayanganIos(bentuk, 3.dp).clip(bentuk).background(WarnaIos.Kartu)
                else Modifier.clip(bentuk),
            )
            .tekanIos(onKlik),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            teks,
            color = WarnaIos.Label,
            fontSize = 13.sp,
            fontWeight = if (aktif) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}
