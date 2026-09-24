package com.sukashawarma.superapp.feature.manager.ui.ceklist

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.RealtimeTables
import com.sukashawarma.superapp.core.ui.ios.BilahJudulIos
import com.sukashawarma.superapp.core.ui.ios.IkonBulatIos
import com.sukashawarma.superapp.core.ui.ios.KapsulPilihanIos
import com.sukashawarma.superapp.core.ui.ios.KartuIos
import com.sukashawarma.superapp.core.ui.ios.LencanaIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.TombolBundarIos
import com.sukashawarma.superapp.core.ui.ios.TombolKeduaIos
import com.sukashawarma.superapp.core.ui.ios.TombolUtamaIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.ios.warnaKolomIos
import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.core.ui.kaca.denganRuangNav
import com.sukashawarma.superapp.core.ui.kaca.navigationBarsPaddingKaca
import com.sukashawarma.superapp.feature.manager.data.OutletPilihan
import com.sukashawarma.superapp.feature.manager.domain.BagianBebas
import com.sukashawarma.superapp.feature.manager.domain.FilterCeklist
import com.sukashawarma.superapp.feature.manager.domain.FotoCeklist
import com.sukashawarma.superapp.feature.manager.domain.KATEGORI_CEKLIST
import com.sukashawarma.superapp.feature.manager.domain.LaporanCeklist
import com.sukashawarma.superapp.feature.manager.domain.NilaiCeklist
import com.sukashawarma.superapp.feature.manager.domain.jamJakarta
import com.sukashawarma.superapp.feature.manager.domain.keteranganTampil
import com.sukashawarma.superapp.feature.manager.domain.nilaiKategori
import com.sukashawarma.superapp.feature.manager.domain.tanggalPanjangIndonesia
import com.sukashawarma.superapp.feature.manager.domain.waktuJakartaRingkas
import com.sukashawarma.superapp.feature.manager.ui.BarProgres
import com.sukashawarma.superapp.feature.manager.ui.KartuPanel
import com.sukashawarma.superapp.feature.manager.ui.PanelKosong

/**
 * Ceklist Harian — jalur pemantauan regional manager.
 *
 * Tiga pertanyaan RM dijawab berurutan dari atas: berapa outlet sudah dicek,
 * mana yang bermasalah, dan mana yang belum ia tinjau. Kartu yang butuh tindakan
 * selalu naik ke atas daftar.
 */
@Composable
fun PantauCeklistScreen(
    onExit: () -> Unit,
    viewModel: PantauCeklistViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var fotoDibuka by remember { mutableStateOf<Pair<String?, String>?>(null) }

    RealtimeRefresh(RealtimeTables.CEKLIST_HARIAN) { viewModel.muatUlang(silent = true) }

    LaunchedEffect(state.galat, state.kabar) {
        val pesan = state.galat ?: state.kabar ?: return@LaunchedEffect
        snackbar.showSnackbar(pesan)
        viewModel.tutupKabar()
    }

    BackHandler(enabled = state.outletDibuka != null) { viewModel.tutup() }

    val laporan = state.laporanDibuka
    Scaffold(
        containerColor = WarnaIos.Latar,
        snackbarHost = { SnackbarHost(snackbar, Modifier.navigationBarsPaddingKaca()) },
        topBar = {
            BilahJudulIos(
                judul = if (laporan == null) "Pantau Ceklist Harian" else state.namaOutletDibuka,
                subjudul = if (laporan == null) {
                    tanggalPanjangIndonesia(state.tanggal)
                } else {
                    "AM ${laporan.namaAm} · ${jamJakarta(laporan.diperbaruiPada)}"
                },
                onKembali = { if (laporan != null) viewModel.tutup() else onExit() },
                aksi = {
                    if (laporan == null) {
                        TombolBundarIos(IkonIos.Refresh, "Muat ulang", { viewModel.muatUlang() })
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                laporan != null -> DetailLaporan(
                    state = state,
                    laporan = laporan,
                    viewModel = viewModel,
                    onBukaFoto = { url, judul -> fotoDibuka = url to judul },
                )
                state.memuat && state.outlets.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = WarnaIos.Aksen)
                }
                else -> DaftarPantau(state, viewModel)
            }
        }
    }

    fotoDibuka?.let { (url, judul) -> DialogFoto(url, judul) { fotoDibuka = null } }
}

/* --------------------------------- daftar ---------------------------------- */

@Composable
private fun DaftarPantau(state: PantauCeklistUiState, viewModel: PantauCeklistViewModel) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(UkuranIos.TepiLayar).denganRuangNav(),
        verticalArrangement = Arrangement.spacedBy(UkuranIos.JarakKartu),
    ) {
        item(key = "tanggal") { PemilihTanggal(state, viewModel::geserTanggal) }
        item(key = "ringkasan") { PanelRingkasan(state) }
        item(key = "filter") {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterCeklist.entries.forEach { f ->
                    val jumlah = when (f) {
                        FilterCeklist.SEMUA -> null
                        FilterCeklist.PERHATIAN -> state.jumlahPerhatian
                        FilterCeklist.BELUM_DICEK -> state.jumlahBelumDicek
                        FilterCeklist.BELUM_DITINJAU -> state.jumlahBelumDitinjau
                    }
                    if (f == FilterCeklist.SEMUA || (jumlah ?: 0) > 0 || state.filter == f) {
                        KapsulPilihanIos(
                            label = f.label,
                            aktif = state.filter == f,
                            onKlik = { viewModel.pilihFilter(f) },
                            jumlah = jumlah,
                        )
                    }
                }
            }
        }

        val daftar = state.outletTerlihat
        if (daftar.isEmpty()) {
            item(key = "kosong") {
                KartuPanel {
                    PanelKosong(
                        if (state.outlets.isEmpty()) "Belum ada outlet dalam cakupan Anda."
                        else "Tidak ada outlet pada filter ini.",
                    )
                }
            }
        } else {
            items(daftar, key = { it.id }) { outlet ->
                KartuPantau(outlet, state.laporan[outlet.id]) { viewModel.buka(outlet.id) }
            }
        }
    }
}

@Composable
private fun PemilihTanggal(state: PantauCeklistUiState, onGeser: (Long) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TombolBundarIos(IkonIos.ArrowBack, "Hari sebelumnya", { onGeser(-1) })
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (state.hariIni) "Hari ini" else "Riwayat", style = TipeIos.Catatan)
            Text(tanggalPanjangIndonesia(state.tanggal), style = TipeIos.Utama, maxLines = 1)
        }
        TombolBundarIos(IkonIos.ArrowForward, "Hari berikutnya", { onGeser(1) }, aktif = !state.hariIni)
    }
}

@Composable
private fun PanelRingkasan(state: PantauCeklistUiState) {
    val total = state.outlets.size
    KartuPanel {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("${state.jumlahDicek}", style = TipeIos.AngkaBesar)
            Text(" / $total outlet sudah dicek", Modifier.padding(bottom = 4.dp), style = TipeIos.Catatan)
        }
        Spacer(Modifier.height(8.dp))
        BarProgres(
            rasio = if (total == 0) 0f else state.jumlahDicek / total.toFloat(),
            tinggi = 8,
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AngkaMini("Perlu tindakan", state.jumlahPerhatian, NadaIos.PERINGATAN, Modifier.weight(1f))
            AngkaMini("Belum disetujui", state.jumlahBelumDitinjau, NadaIos.INFO, Modifier.weight(1f))
            AngkaMini("Belum dicek", state.jumlahBelumDicek, NadaIos.NETRAL, Modifier.weight(1f))
        }
    }
}

@Composable
private fun AngkaMini(label: String, nilai: Int, nada: NadaIos, modifier: Modifier) {
    Column(
        modifier
            .clip(UkuranIos.SudutBlok)
            .background(WarnaIos.Latar)
            .padding(10.dp),
    ) {
        Text("$nilai", style = TipeIos.Angka.copy(color = if (nilai > 0) nada.teks else WarnaIos.LabelKetiga))
        Text(label, style = TipeIos.Kecil, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun KartuPantau(outlet: OutletPilihan, laporan: LaporanCeklist?, onKlik: () -> Unit) {
    if (laporan == null) {
        KartuIos(padding = PaddingValues(horizontal = UkuranIos.PaddingKartu, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IkonBulatIos(IkonIos.Storefront, WarnaIos.Abu, ukuran = 36.dp, padat = false)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(outlet.nama, style = TipeIos.Utama, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Belum dicek AM", style = TipeIos.Catatan)
                }
                LencanaIos("Belum", NadaIos.NETRAL)
            }
        }
    } else {
        KartuLaporanPantau(outlet, laporan, onKlik)
    }
}

/**
 * Dipisah dari [KartuPantau] supaya cabang "belum dicek" tidak keluar lebih awal
 * dari badan composable — pola yang dulu merusak slot table layar Persetujuan.
 */
@Composable
private fun KartuLaporanPantau(outlet: OutletPilihan, laporan: LaporanCeklist, onKlik: () -> Unit) {
    val keseluruhan = laporan.nilaiKeseluruhan
    KartuIos(onKlik = onKlik, padding = PaddingValues(horizontal = UkuranIos.PaddingKartu, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IkonBulatIos(
                if (laporan.perluPerhatian) IkonIos.WarningAmber else IkonIos.CheckCircle,
                // Temuan tanpa penilaian buruk tetap diwarnai "perhatian", bukan hijau.
                when {
                    keseluruhan == NilaiCeklist.BURUK -> WarnaIos.Merah
                    laporan.perluPerhatian -> WarnaIos.Oranye
                    else -> WarnaIos.Hijau
                },
                ukuran = 36.dp,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(outlet.nama, style = TipeIos.Utama, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "AM ${laporan.namaAm} · ${jamJakarta(laporan.diperbaruiPada)}",
                    style = TipeIos.Catatan,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(6.dp))
            Icon(IkonIos.ChevronRight, null, tint = WarnaIos.LabelKetiga, modifier = Modifier.size(15.dp))
        }
        Spacer(Modifier.height(12.dp))
        TitikKategori(KATEGORI_CEKLIST.map { it.kunci to nilaiKategori(it, laporan.isian) })
        if (laporan.temuan.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Top) {
                Icon(IkonIos.WarningAmber, null, tint = NadaIos.PERINGATAN.teks, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    laporan.temuan.joinToString(" · "),
                    style = TipeIos.Catatan.copy(color = NadaIos.PERINGATAN.teks),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        PitaStatusPersetujuan(laporan)
    }
}

/**
 * Pita status persetujuan selebar kartu — sengaja besar dan berwarna penuh,
 * bukan lencana kecil, supaya RM yang menggulir puluhan outlet langsung bisa
 * memisahkan yang sudah beres dari yang masih menunggu tanpa membaca.
 */
@Composable
private fun PitaStatusPersetujuan(laporan: LaporanCeklist) {
    val setuju = laporan.sudahDitinjau
    val nada = if (setuju) NadaIos.SUKSES else NadaIos.PERINGATAN
    Row(
        Modifier
            .fillMaxWidth()
            .clip(UkuranIos.SudutKontrol)
            .background(nada.warna.copy(alpha = if (setuju) 0.16f else 0.12f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            IkonIos.aktif(if (setuju) IkonIos.CheckCircle else IkonIos.Schedule),
            null,
            tint = nada.teks,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (setuju) "DISETUJUI" else "MENUNGGU PERSETUJUAN",
                color = nada.teks,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.4.sp,
            )
            if (setuju) {
                Text(
                    "oleh ${laporan.namaPeninjau.orEmpty()} · ${waktuJakartaRingkas(laporan.ditinjauPada.orEmpty())}",
                    style = TipeIos.Kecil.copy(color = nada.teks),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Spanduk status di puncak halaman detail. Saat sudah disetujui, siapa, kapan,
 * dan tanggapannya langsung terbaca tanpa menggulir ke dasar halaman.
 */
@Composable
private fun SpandukPersetujuan(laporan: LaporanCeklist) {
    val setuju = laporan.sudahDitinjau
    val nada = if (setuju) NadaIos.SUKSES else NadaIos.PERINGATAN
    Column(
        Modifier
            .fillMaxWidth()
            .clip(UkuranIos.SudutKartu)
            .background(nada.warna.copy(alpha = 0.14f))
            .padding(UkuranIos.PaddingKartu),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IkonBulatIos(if (setuju) IkonIos.CheckCircle else IkonIos.Schedule, nada.warna, ukuran = 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (setuju) "Laporan sudah disetujui" else "Menunggu persetujuan Anda",
                    style = TipeIos.Judul3.copy(color = nada.teks, fontWeight = FontWeight.Bold),
                )
                Text(
                    if (setuju) {
                        "oleh ${laporan.namaPeninjau.orEmpty()} · ${waktuJakartaRingkas(laporan.ditinjauPada.orEmpty())}"
                    } else {
                        "Periksa laporan, lalu tekan Setujui laporan di bagian bawah."
                    },
                    style = TipeIos.Catatan.copy(color = nada.teks),
                )
            }
        }
        val tanggapan = laporan.tanggapanRm
        if (setuju && !tanggapan.isNullOrBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
                "💬 \"$tanggapan\"",
                style = TipeIos.Keterangan.copy(color = nada.teks),
            )
        }
    }
}

/* --------------------------------- detail ---------------------------------- */

@Composable
private fun DetailLaporan(
    state: PantauCeklistUiState,
    laporan: LaporanCeklist,
    viewModel: PantauCeklistViewModel,
    onBukaFoto: (String?, String) -> Unit,
) {
    fun denganUrl(foto: List<FotoCeklist>) = foto.map { it.copy(url = state.urlFoto[it.path]) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(UkuranIos.TepiLayar).denganRuangNav(),
        verticalArrangement = Arrangement.spacedBy(UkuranIos.JarakKartu),
    ) {
        item(key = "status") { SpandukPersetujuan(laporan) }
        items(KATEGORI_CEKLIST, key = { it.kunci }) { kategori ->
            KartuPanel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val nilai = nilaiKategori(kategori, laporan.isian)
                    IkonBulatIos(ikonKategori(kategori.kunci), nadaNilai(nilai).warna, ukuran = 32.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(kategori.label, Modifier.weight(1f), style = TipeIos.Judul3)
                    LencanaNilai(nilai)
                }
                kategori.butir.forEach { butir ->
                    val isian = laporan.isian[butir.kunci]
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            isian?.nilai?.emoji ?: "▫️",
                            Modifier.width(26.dp),
                            fontSize = 15.sp,
                        )
                        Column(Modifier.weight(1f)) {
                            if (kategori.punyaSubItem) {
                                Text(butir.label, style = TipeIos.Keterangan.copy(fontWeight = FontWeight.SemiBold))
                            }
                            Text(
                                keteranganTampil(isian).replaceFirstChar { it.uppercase() },
                                style = TipeIos.Keterangan.copy(color = nadaNilai(isian?.nilai).teks),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                DeretFoto(
                    foto = denganUrl(laporan.foto[kategori.kunci].orEmpty()),
                    ukuran = 96.dp,
                    onPerluUrl = { viewModel.pastikanUrlFoto(it.path) },
                    onBuka = { onBukaFoto(it.url, "${kategori.label} · ${state.namaOutletDibuka}") },
                )
            }
        }

        // Bagian bebas yang tidak diisi AM (tanpa teks dan tanpa foto) tidak ditampilkan.
        val bagianTerisi = BagianBebas.entries.filter {
            laporan.teks(it).isNotEmpty() || laporan.foto[it.kunci].orEmpty().isNotEmpty()
        }
        items(bagianTerisi, key = { it.kunci }) { bagian ->
            KartuPanel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IkonBulatIos(ikonBagian(bagian), warnaBagian(bagian), ukuran = 32.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(bagian.label, style = TipeIos.Judul3)
                }
                laporan.teks(bagian).forEach {
                    Spacer(Modifier.height(8.dp))
                    Row {
                        Text("•", Modifier.width(18.dp), style = TipeIos.Keterangan)
                        Text(it, style = TipeIos.Keterangan)
                    }
                }
                val foto = laporan.foto[bagian.kunci].orEmpty()
                if (foto.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    DeretFoto(
                        foto = denganUrl(foto),
                        ukuran = 96.dp,
                        onPerluUrl = { viewModel.pastikanUrlFoto(it.path) },
                        onBuka = { onBukaFoto(it.url, "${bagian.label} · ${state.namaOutletDibuka}") },
                    )
                }
            }
        }

        if (!laporan.catatan.isNullOrBlank()) {
            item(key = "catatan") {
                KartuPanel {
                    Text("Catatan", style = TipeIos.Utama)
                    Spacer(Modifier.height(4.dp))
                    Text(laporan.catatan, style = TipeIos.Keterangan)
                }
            }
        }

        item(key = "tinjau") { PanelTinjau(state, laporan, viewModel) }
    }
}

@Composable
private fun PanelTinjau(state: PantauCeklistUiState, laporan: LaporanCeklist, viewModel: PantauCeklistViewModel) {
    KartuPanel {
        if (laporan.sudahDitinjau) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IkonBulatIos(IkonIos.CheckCircle, WarnaIos.Hijau, ukuran = 30.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Sudah disetujui", style = TipeIos.Utama)
                    Text(
                        "${laporan.namaPeninjau.orEmpty()} · ${jamJakarta(laporan.ditinjauPada)}",
                        style = TipeIos.Catatan,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        } else {
            Text("Setujui laporan", style = TipeIos.Utama)
            Text("AM langsung mendapat notifikasi beserta tanggapan Anda.", style = TipeIos.Catatan)
            Spacer(Modifier.height(12.dp))
        }

        if (state.bolehMeninjau) {
            OutlinedTextField(
                value = state.tanggapan,
                onValueChange = viewModel::ubahTanggapan,
                placeholder = { Text("Tanggapan untuk AM (opsional)", fontSize = 14.sp) },
                modifier = Modifier.fillMaxWidth().height(96.dp),
                textStyle = LocalTextStyle.current.copy(fontSize = 15.sp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                shape = UkuranIos.SudutKontrol,
                colors = warnaKolomIos(),
            )
            Spacer(Modifier.height(12.dp))
            if (laporan.sudahDitinjau) {
                TombolKeduaIos("Perbarui tanggapan", onKlik = viewModel::tinjau, aktif = !state.meninjau)
            } else {
                TombolUtamaIos(
                    "Setujui laporan",
                    onKlik = viewModel::tinjau,
                    memuat = state.meninjau,
                    ikon = IkonIos.CheckCircle,
                )
            }
        } else if (!laporan.tanggapanRm.isNullOrBlank()) {
            Text(laporan.tanggapanRm, style = TipeIos.Keterangan)
        }
    }
}
