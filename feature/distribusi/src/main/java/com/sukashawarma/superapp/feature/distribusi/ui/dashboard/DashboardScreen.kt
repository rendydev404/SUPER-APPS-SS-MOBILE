package com.sukashawarma.superapp.feature.distribusi.ui.dashboard

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.feature.distribusi.data.model.RentangTanggal
import com.sukashawarma.superapp.feature.distribusi.data.model.SuratJalanRingkas
import com.sukashawarma.superapp.feature.distribusi.domain.bolehDitutup
import com.sukashawarma.superapp.feature.distribusi.domain.salamSekarang
import com.sukashawarma.superapp.feature.distribusi.domain.tanggalPanjangHariIni
import com.sukashawarma.superapp.feature.distribusi.ui.KartuSuratJalan
import com.sukashawarma.superapp.feature.distribusi.ui.LayarGalat
import com.sukashawarma.superapp.feature.distribusi.ui.LayarKosong
import com.sukashawarma.superapp.feature.distribusi.ui.LayarMemuat
import com.sukashawarma.superapp.feature.distribusi.ui.NavBawah
import com.sukashawarma.superapp.feature.distribusi.ui.TabBawah
import com.sukashawarma.superapp.feature.distribusi.ui.SegarkanSaatAktif
import com.sukashawarma.superapp.presentation.theme.SukaGray500
import com.sukashawarma.superapp.presentation.theme.SukaOnSurface
import com.sukashawarma.superapp.presentation.theme.SukaOrange
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.RealtimeTables

// Palet banner dan kartu statistik, cermin kelas Tailwind di dashboard web.
private val KrimLatar = Color(0xFFFFF8F1)
private val Cokelat = Color(0xFF701604)
private val CokelatTua = Color(0xFF4D1003)
private val Amber = Color(0xFFB45309)
private val AmberLatar = Color(0xFFFFFBEB)
private val AmberIkon = Color(0xFFFDE68A)
private val Biru = Color(0xFF1D4ED8)
private val BiruLatar = Color(0xFFEFF6FF)
private val BiruIkon = Color(0xFFDBEAFE)
private val Ungu = Color(0xFF6D28D9)
private val UnguLatar = Color(0xFFF5F3FF)
private val UnguIkon = Color(0xFFEDE9FE)
private val Hijau = Color(0xFF047857)
private val HijauLatar = Color(0xFFECFDF5)
private val HijauIkon = Color(0xFFD1FAE5)
private val Merah = Color(0xFFB91C1C)
private val MerahLatar = Color(0xFFFEF2F2)
private val MerahIkon = Color(0xFFFEE2E2)

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
    val halamanSuratJalan = halamanSuratJalan(state.terlihat, state.halamanAktif)
    // Indeks item daftar konstan untuk dua susunan: dengan atau tanpa filter outlet.
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
        if (state.terlihat.isNotEmpty()) daftarState.animateScrollToItem(indeksDaftar)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavBawah(
                aktif = TabBawah.DASHBOARD,
                bolehVerifikasi = state.bolehVerifikasi,
                onDashboard = {},
                onScan = onBukaScan,
                onRiwayat = onBukaRiwayat,
            )
        },
    ) { paddingValues ->
        Column(Modifier.fillMaxSize().padding(paddingValues).background(KrimLatar)) {
            KepalaDashboard(
                namaOutlet = state.namaOutlet,
                namaPengguna = state.namaPengguna,
                onKeluar = onKeluar,
                onSegarkan = { viewModel.muat(paksa = true) },
            )

            LazyColumn(
                Modifier.fillMaxSize(),
                state = daftarState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
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
                    OutlinedTextField(
                        value = state.cari,
                        onValueChange = viewModel::ubahCari,
                        label = { Text("Cari nomor SJ atau outlet") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (state.rincianOutlet.size > 1) {
                    item {
                        Text(
                            "Filter Outlet",
                            color = SukaGray500,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            state.rincianOutlet.forEach { outlet ->
                                PilOutlet(
                                    teks = "${outlet.nama} (${outlet.total})",
                                    aktif = state.outletTerpilih == outlet.nama,
                                ) { viewModel.pilihOutlet(outlet.nama) }
                            }
                        }
                    }
                }

                if (state.terlihat.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().height(220.dp)) {
                            LayarKosong(
                                "Tidak Ada Surat Jalan",
                                "Tidak ada yang cocok dengan filter ini.",
                            )
                        }
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

    konfirmasiTutup?.let { baris ->
        AlertDialog(
            onDismissRequest = { konfirmasiTutup = null },
            title = { Text("Tutup dokumen?") },
            text = {
                Text(
                    "Surat jalan ${baris.nomorDokumen ?: baris.id.take(8)} akan ditandai selesai " +
                        "dan tidak bisa dibuka kembali dari aplikasi.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.tutupDokumen(baris)
                    konfirmasiTutup = null
                }) { Text("Tutup Dokumen") }
            },
            dismissButton = {
                TextButton(onClick = { konfirmasiTutup = null }) { Text("Batal") }
            },
        )
    }
}

@Composable
private fun KepalaDaftarSuratJalan(halaman: HalamanSuratJalan) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, SukaOrange.copy(alpha = 0.22f)),
        shadowElevation = 2.dp,
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(13.dp),
                color = SukaOrange.copy(alpha = 0.16f),
                modifier = Modifier.size(42.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        tint = Cokelat,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Spacer(Modifier.size(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Daftar Surat Jalan",
                    color = SukaOnSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    "Terbaru di atas • 5 dokumen per halaman",
                    color = SukaGray500,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Cokelat.copy(alpha = 0.08f),
            ) {
                Text(
                    "${halaman.totalBaris} SJ",
                    Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                    color = Cokelat,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}

@Composable
private fun KontrolPaginationSuratJalan(
    halaman: HalamanSuratJalan,
    onPilihHalaman: (Int) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Cokelat,
        shadowElevation = 3.dp,
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                "NAVIGASI DAFTAR",
                color = Color.White.copy(alpha = 0.66f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TombolArahHalaman(
                    ikon = Icons.Default.ChevronLeft,
                    keterangan = "Halaman sebelumnya",
                    aktif = halaman.nomor > 1,
                    onKlik = { onPilihHalaman(halaman.nomor - 1) },
                )
                Spacer(Modifier.size(10.dp))
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Surface(shape = RoundedCornerShape(12.dp), color = SukaOrange) {
                        Text(
                            "HALAMAN ${halaman.nomor} / ${halaman.totalHalaman}",
                            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.4.sp,
                        )
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "Menampilkan ${halaman.urutanMulai}–${halaman.urutanAkhir} dari ${halaman.totalBaris} SJ",
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(Modifier.size(10.dp))
                TombolArahHalaman(
                    ikon = Icons.Default.ChevronRight,
                    keterangan = "Halaman berikutnya",
                    aktif = halaman.nomor < halaman.totalHalaman,
                    onKlik = { onPilihHalaman(halaman.nomor + 1) },
                )
            }
        }
    }
}

@Composable
private fun TombolArahHalaman(
    ikon: ImageVector,
    keterangan: String,
    aktif: Boolean,
    onKlik: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .size(38.dp)
            .clickable(enabled = aktif, onClick = onKlik),
        shape = CircleShape,
        color = if (aktif) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.06f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color.White.copy(alpha = if (aktif) 0.2f else 0.08f),
        ),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                ikon,
                contentDescription = keterangan,
                tint = Color.White.copy(alpha = if (aktif) 1f else 0.35f),
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/** Kepala putih menempel, cermin sticky header web. */
@Composable
private fun KepalaDashboard(
    namaOutlet: String,
    namaPengguna: String,
    onKeluar: () -> Unit,
    onSegarkan: () -> Unit,
) {
    Surface(color = Color.White, shadowElevation = 1.dp) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onKeluar) {
                Icon(Icons.Default.ArrowBack, "Kembali", tint = SukaOnSurface)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "Pusat Komando Distribusi",
                    color = Cokelat,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "OUTLET SUPPLY UNIT • ${namaOutlet.ifBlank { "OUTLET" }.uppercase()}",
                    color = SukaGray500,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onSegarkan) {
                Icon(Icons.Default.Refresh, "Segarkan", tint = SukaGray500)
            }
            // Inisial nama, pengganti komponen Avatar web.
            Box(
                Modifier.size(36.dp).background(SukaOrange.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    namaPengguna.take(2).uppercase().ifBlank { "?" },
                    color = Cokelat,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                )
            }
            Spacer(Modifier.size(8.dp))
        }
    }
}

/** Banner cokelat bergradasi berisi sapaan, judul, filter tanggal, dan aksi utama. */
@Composable
private fun BannerHero(
    namaOutlet: String,
    rentang: RentangTanggal,
    bolehVerifikasi: Boolean,
    onUbahRentang: (RentangTanggal) -> Unit,
    onScan: () -> Unit,
) {
    Surface(shape = RoundedCornerShape(24.dp), color = Color.Transparent) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(listOf(Cokelat, CokelatTua, Cokelat)),
                    RoundedCornerShape(24.dp),
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.15f),
                ) {
                    Text(
                        salamSekarang().uppercase(),
                        Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = AmberIkon,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                    )
                }
                Spacer(Modifier.size(8.dp))
                Text(
                    "• ${tanggalPanjangHariIni()}",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Text(
                "Penerimaan Logistik ${namaOutlet.ifBlank { "Outlet" }}",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 27.sp,
            )

            Text(
                "Pastikan barang fisik yang tiba dicocokkan dengan manifes surat jalan " +
                    "dan bubuhkan tanda tangan penerima sebelum dimasukkan ke kartu stok outlet.",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp,
                lineHeight = 18.sp,
            )

            // Pil filter tanggal di dalam wadah gelap, seperti di web.
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.Black.copy(alpha = 0.3f),
            ) {
                Row(
                    Modifier.padding(5.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    RentangTanggal.entries.forEach { pilihan ->
                        PilRentang(
                            teks = pilihan.label,
                            aktif = rentang == pilihan,
                            modifier = Modifier.weight(1f),
                        ) { onUbahRentang(pilihan) }
                    }
                }
            }

            if (bolehVerifikasi) {
                Surface(
                    Modifier.fillMaxWidth().clickable(onClick = onScan),
                    shape = RoundedCornerShape(16.dp),
                    color = SukaOrange,
                ) {
                    Row(
                        Modifier.padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            "SCAN QR KEDATANGAN",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                        )
                    }
                }
            }
        }
    }
}

/** Lima kartu statistik dalam dua kolom, urutan sama dengan HUD web. */
@Composable
private fun GridStatistik(state: DashboardUiState, onPilihTab: (TabStatus) -> Unit) {
    val adaSelisih = state.semua.count { it.adaSelisih }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KartuStatistik(
                label = "Siap Kirim",
                nilai = state.hitungan.draft.toString(),
                keterangan = "Draft pesanan pending",
                ikon = Icons.Default.Description,
                warna = Amber,
                warnaLatar = AmberLatar,
                warnaIkon = AmberIkon,
                aktif = state.tab == TabStatus.DRAFT,
                modifier = Modifier.weight(1f),
            ) { onPilihTab(if (state.tab == TabStatus.DRAFT) TabStatus.SEMUA else TabStatus.DRAFT) }

            KartuStatistik(
                label = "Dalam Transit",
                nilai = state.hitungan.dikirim.toString(),
                keterangan = "Sedang di jalan menuju outlet",
                ikon = Icons.Default.LocalShipping,
                warna = Biru,
                warnaLatar = BiruLatar,
                warnaIkon = BiruIkon,
                aktif = state.tab == TabStatus.DIKIRIM,
                modifier = Modifier.weight(1f),
            ) { onPilihTab(if (state.tab == TabStatus.DIKIRIM) TabStatus.SEMUA else TabStatus.DIKIRIM) }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KartuStatistik(
                label = "Tiba di Outlet",
                nilai = state.hitungan.diterima.toString(),
                keterangan = "Menunggu hitung fisik & TTD",
                ikon = Icons.Default.Schedule,
                warna = Ungu,
                warnaLatar = UnguLatar,
                warnaIkon = UnguIkon,
                aktif = state.tab == TabStatus.BELUM_VERIF,
                modifier = Modifier.weight(1f),
            ) {
                onPilihTab(
                    if (state.tab == TabStatus.BELUM_VERIF) TabStatus.SEMUA else TabStatus.BELUM_VERIF,
                )
            }

            KartuStatistik(
                label = "Tervalidasi",
                nilai = state.hitungan.selesai.toString(),
                keterangan = "Selesai & stok otomatis update",
                ikon = Icons.Default.CheckCircle,
                warna = Hijau,
                warnaLatar = HijauLatar,
                warnaIkon = HijauIkon,
                aktif = state.tab == TabStatus.SELESAI,
                modifier = Modifier.weight(1f),
            ) { onPilihTab(if (state.tab == TabStatus.SELESAI) TabStatus.SEMUA else TabStatus.SELESAI) }
        }

        KartuStatistik(
            label = if (adaSelisih > 0) "$adaSelisih Selisih" else "Zero Selisih",
            nilai = "${state.akurasi}%",
            keterangan = if (adaSelisih > 0) "Perlu investigasi SPV" else "Tingkat akurasi sempurna",
            ikon = if (adaSelisih > 0) Icons.Default.WarningAmber else Icons.Default.VerifiedUser,
            warna = if (adaSelisih > 0) Merah else Hijau,
            warnaLatar = if (adaSelisih > 0) MerahLatar else HijauLatar,
            warnaIkon = if (adaSelisih > 0) MerahIkon else HijauIkon,
            aktif = state.tab == TabStatus.SELISIH,
            modifier = Modifier.fillMaxWidth(),
        ) { onPilihTab(if (state.tab == TabStatus.SELISIH) TabStatus.SEMUA else TabStatus.SELISIH) }
    }
}

@Composable
private fun KartuStatistik(
    label: String,
    nilai: String,
    keterangan: String,
    ikon: ImageVector,
    warna: Color,
    warnaLatar: Color,
    warnaIkon: Color,
    aktif: Boolean,
    modifier: Modifier = Modifier,
    onKlik: () -> Unit,
) {
    Surface(
        modifier.clickable(onClick = onKlik),
        shape = RoundedCornerShape(20.dp),
        color = if (aktif) warnaLatar else Color.White,
        border = androidx.compose.foundation.BorderStroke(
            if (aktif) 2.dp else 1.dp,
            if (aktif) warna.copy(alpha = 0.5f) else SukaOrange.copy(alpha = 0.15f),
        ),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Surface(
                    Modifier.weight(1f, fill = false),
                    shape = RoundedCornerShape(8.dp),
                    color = warnaLatar,
                ) {
                    Text(
                        label.uppercase(),
                        Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = warna,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier.size(30.dp).background(warnaIkon, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(ikon, null, tint = warna, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(nilai, color = warna, fontSize = 26.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(2.dp))
            Text(
                keterangan,
                color = SukaGray500,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 13.sp,
            )
        }
    }
}

@Composable
private fun PilRentang(
    teks: String,
    aktif: Boolean,
    modifier: Modifier = Modifier,
    onKlik: () -> Unit,
) {
    Surface(
        modifier.clickable(onClick = onKlik),
        shape = RoundedCornerShape(12.dp),
        color = if (aktif) SukaOrange else Color.Transparent,
    ) {
        Text(
            teks.uppercase(),
            Modifier.padding(vertical = 8.dp),
            color = if (aktif) Color.White else Color.White.copy(alpha = 0.7f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun PilOutlet(teks: String, aktif: Boolean, onKlik: () -> Unit) {
    Surface(
        Modifier.fillMaxWidth().clickable(onClick = onKlik),
        shape = RoundedCornerShape(12.dp),
        color = if (aktif) SukaOrange else Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, SukaOrange.copy(alpha = 0.15f)),
    ) {
        Text(
            teks,
            Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            color = if (aktif) Color.White else SukaOnSurface,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
