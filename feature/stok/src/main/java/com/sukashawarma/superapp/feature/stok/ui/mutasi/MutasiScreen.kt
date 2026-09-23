package com.sukashawarma.superapp.feature.stok.ui.mutasi

import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.core.ui.kaca.denganRuangNav
import com.sukashawarma.superapp.core.ui.kaca.navigationBarsPaddingKaca
import com.sukashawarma.superapp.core.ui.ios.KartuIos
import com.sukashawarma.superapp.core.ui.ios.KolomCariIos
import com.sukashawarma.superapp.core.ui.ios.LabelSeksiIos
import com.sukashawarma.superapp.core.ui.ios.LencanaIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.PemisahIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.TombolBundarIos
import com.sukashawarma.superapp.core.ui.ios.TombolKeduaIos
import com.sukashawarma.superapp.core.ui.ios.TombolUtamaIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.ios.tekanIos
import com.sukashawarma.superapp.core.ui.ios.warnaKolomIos
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.feature.stok.data.model.Mutasi
import com.sukashawarma.superapp.feature.stok.data.model.StatusMutasi
import com.sukashawarma.superapp.feature.stok.domain.formatAngkaStok
import com.sukashawarma.superapp.feature.stok.domain.formatSatuan
import com.sukashawarma.superapp.feature.stok.ui.HeaderStok
import com.sukashawarma.superapp.feature.stok.ui.KeadaanGagal
import com.sukashawarma.superapp.feature.stok.ui.KeadaanKosong
import com.sukashawarma.superapp.feature.stok.ui.KeadaanTidakBerhak
import com.sukashawarma.superapp.feature.stok.ui.LembarPilihOutlet
import com.sukashawarma.superapp.feature.stok.ui.MemuatPenuh
import com.sukashawarma.superapp.feature.stok.ui.PemilihOutlet
import com.sukashawarma.superapp.feature.stok.ui.PitaPesan
import com.sukashawarma.superapp.feature.stok.ui.waktuSingkat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MutasiScreen(viewModel: MutasiViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()

    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            viewModel.segarkanManual()
        }
    }

    LaunchedEffect(state.memuat) {
        if (!state.memuat) {
            pullRefreshState.endRefresh()
        }
    }

    if (state.formTerbuka) {
        FormAjukan(state, viewModel)
    } else {
    Column(Modifier.fillMaxSize().background(WarnaIos.Latar)) {
        HeaderStok(
            judul = "Mutasi Antar Outlet",
            subjudul = "Kirim & terima transfer stok",
        ) {
            TombolBundarIos(IkonIos.Refresh, "Segarkan", viewModel::muatAwal)
        }

        if (!state.tidakBerhak && state.outlets.size > 1) {
            PemilihOutlet(state.outlets, state.outletTerpilih, viewModel::pilihOutlet)
        }

        state.pesan?.let { PitaPesan(it, false, viewModel::bersihkanPesan) }
        state.error?.let { PitaPesan(it, true, viewModel::bersihkanPesan) }

        if (!state.tidakBerhak) {
            TombolUtamaIos(
                "Ajukan Mutasi",
                viewModel::bukaForm,
                Modifier.padding(horizontal = UkuranIos.TepiLayar, vertical = 10.dp),
                ikon = IkonIos.Add,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(pullRefreshState.nestedScrollConnection)
        ) {
            when {
                state.tidakBerhak -> KeadaanTidakBerhak("Akun Anda belum terhubung dengan outlet mana pun.")
                state.memuat && state.outlets.isEmpty() -> MemuatPenuh()
                state.error != null && state.daftar.isEmpty() -> KeadaanGagal(state.error!!, viewModel::muatAwal)
                state.daftar.isEmpty() -> KeadaanKosong("Belum ada mutasi yang melibatkan outlet ini.")
                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = UkuranIos.TepiLayar, end = UkuranIos.TepiLayar, top = 2.dp, bottom = 20.dp,
                    ).denganRuangNav(),
                    verticalArrangement = Arrangement.spacedBy(UkuranIos.JarakKartu),
                ) {
                    items(state.daftar, key = { it.id }) { m ->
                        KartuMutasi(m) { viewModel.bukaDetail(m) }
                    }
                }
            }

            PullToRefreshContainer(
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                containerColor = WarnaIos.Kartu,
                contentColor = WarnaIos.Aksen,
            )
        }
    }

    val target = state.detailUntuk
    if (target != null) LembarTindakan(state, target, viewModel)
    }
}

private fun StatusMutasi.nadaIos(): NadaIos = when (this) {
    StatusMutasi.SELESAI -> NadaIos.SUKSES
    StatusMutasi.DITOLAK -> NadaIos.BAHAYA
    StatusMutasi.DIKIRIM -> NadaIos.INFO
    else -> NadaIos.PERINGATAN
}

@Composable
private fun KartuMutasi(m: Mutasi, onKlik: () -> Unit) {
    KartuIos(onKlik = onKlik) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        m.outletAsalNama ?: "-",
                        style = TipeIos.Utama,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Icon(
                        IkonIos.ArrowForward, null,
                        tint = WarnaIos.Aksen,
                        modifier = Modifier.padding(horizontal = 4.dp).size(14.dp),
                    )
                    Text(
                        m.outletTujuanNama ?: "-",
                        style = TipeIos.Utama,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    "${waktuSingkat(m.createdAt)} · ${m.items.size} bahan · ${m.pembuatNama ?: "-"}",
                    style = TipeIos.Catatan,
                )
            }
            Spacer(Modifier.width(8.dp))
            LencanaIos(m.status.label, m.status.nadaIos())
        }
        if (!m.catatanPenolakan.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Text("Alasan: ${m.catatanPenolakan}", style = TipeIos.Catatan.copy(color = NadaIos.BAHAYA.teks))
        }
    }
}

// Latar bernada untuk baris bahan terisi/berlebih. Warna padat, bukan alfa, supaya
// bayangan kartu tidak tembus dan warnanya sama di atas latar mana pun.
private val LATAR_TERISI = Color(0xFFFFF7ED)
private val LATAR_BAHAYA = Color(0xFFFEF2F2)

@Composable
private fun FormAjukan(state: MutasiUiState, viewModel: MutasiViewModel) {
    var catatanTerbuka by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(WarnaIos.Latar)) {
        HeaderStok(
            judul = "Ajukan Mutasi",
            subjudul = "Kirim stok ke outlet lain",
            onKembali = viewModel::tutupForm,
        )
        state.pesan?.let { PitaPesan(it, false, viewModel::bersihkanPesan) }
        state.error?.let { PitaPesan(it, true, viewModel::bersihkanPesan) }

        if (state.memuatForm) {
            MemuatPenuh()
        } else {
            IsiFormAjukan(state, viewModel, Modifier.weight(1f))
            BilahAjukan(state, viewModel, catatanTerbuka) { catatanTerbuka = it }
        }
    }
}

/**
 * Kartu rute dan daftar bahan dalam satu daftar yang sama.
 *
 * Sebelumnya rute dan kolom cari dipatok tetap di atas. Begitu papan ketik muncul,
 * tinggi yang tersisa habis oleh keduanya — daftar bahan menyusut jadi nol dan
 * bilah tombol terdorong keluar layar. Sekarang hanya bilah tombol yang dipatok;
 * kolom cari menempel di atas saat digulir supaya tetap terjangkau di daftar panjang.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun IsiFormAjukan(state: MutasiUiState, viewModel: MutasiViewModel, modifier: Modifier) {
    LazyColumn(
        modifier,
        contentPadding = PaddingValues(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { KartuRute(state, viewModel) }
        stickyHeader { KolomCariBahan(state, viewModel) }

        if (state.bahanTampil.isEmpty()) {
            item {
                Box(
                    Modifier.fillMaxWidth().padding(vertical = 34.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Tidak ada bahan yang cocok.", style = TipeIos.SubJudul)
                }
            }
        } else {
            items(state.bahanTampil, key = { it.bahanBakuId }) { b ->
                BarisBahanMutasi(
                    bahan = b,
                    nilai = state.jumlah[b.bahanBakuId].orEmpty(),
                    melebihi = b.bahanBakuId in state.melebihiSisa,
                    onUbah = { viewModel.ubahJumlah(b.bahanBakuId, it) },
                    onSeluruhSisa = { viewModel.isiSeluruhSisa(b.bahanBakuId) },
                    modifier = Modifier.padding(horizontal = UkuranIos.TepiLayar),
                )
            }
        }
    }
}

/**
 * Arah mutasi ditampilkan utuh, dari outlet asal ke outlet tujuan.
 *
 * Sebelumnya hanya ada satu dropdown tanpa label sama sekali, sehingga tidak ada
 * yang memberi tahu bahwa isinya outlet TUJUAN dan stok akan keluar dari outlet
 * yang sedang aktif — dua hal yang mahal kalau sampai tertukar.
 */
@Composable
private fun KartuRute(state: MutasiUiState, viewModel: MutasiViewModel) {
    var menu by remember { mutableStateOf(false) }
    val belumPilih = state.tujuanTerpilih == null

    KartuIos(Modifier.padding(horizontal = UkuranIos.TepiLayar, vertical = 12.dp)) {
        LabelSeksiIos("Dari")
        Spacer(Modifier.height(3.dp))
        Text(
            state.outletTerpilih?.name ?: "—",
            style = TipeIos.Utama,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )

        Row(Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).height(0.5.dp).background(WarnaIos.Pemisah))
            Icon(
                IkonIos.ArrowDownward, null,
                tint = WarnaIos.Aksen,
                modifier = Modifier.padding(horizontal = 8.dp).size(15.dp),
            )
            Box(Modifier.weight(1f).height(0.5.dp).background(WarnaIos.Pemisah))
        }

        LabelSeksiIos("Ke")
        Spacer(Modifier.height(5.dp))
        // Merah selama tujuan belum dipilih: satu-satunya isian wajib di form ini.
        val nada = if (belumPilih) NadaIos.BAHAYA else NadaIos.AKSEN
        Row(
            Modifier
                .fillMaxWidth()
                .background(nada.warna.copy(alpha = 0.12f), UkuranIos.SudutKontrol)
                .tekanIos({ menu = true })
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                state.tujuanTerpilih?.name ?: "Pilih outlet tujuan",
                Modifier.weight(1f),
                style = TipeIos.Keterangan.copy(color = nada.teks, fontWeight = FontWeight.SemiBold),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Icon(IkonIos.ArrowDropDown, null, tint = nada.teks, modifier = Modifier.size(16.dp))
        }
    }

    if (menu) {
        LembarPilihOutlet(
            outlets = state.outletTujuan,
            terpilih = state.tujuanTerpilih,
            onPilih = { menu = false; viewModel.pilihTujuan(it) },
            onTutup = { menu = false },
            judul = "Kirim ke outlet",
        )
    }
}

@Composable
private fun KolomCariBahan(state: MutasiUiState, viewModel: MutasiViewModel) {
    // Latar wajib buram: sebagai sticky header, baris bahan lewat persis di baliknya.
    Column(Modifier.fillMaxWidth().background(WarnaIos.Latar).padding(horizontal = UkuranIos.TepiLayar, vertical = 4.dp)) {
        KolomCariIos(state.cari, viewModel::ubahCari, Modifier.fillMaxWidth(), placeholder = "Cari bahan…")
        Spacer(Modifier.height(7.dp))
        Text(
            if (state.cari.isBlank()) {
                "${state.bahanTampil.size} bahan tersedia"
            } else {
                "${state.bahanTampil.size} bahan cocok"
            },
            Modifier.padding(start = 4.dp),
            style = TipeIos.Catatan,
        )
        Spacer(Modifier.height(4.dp))
    }
}

/**
 * Satu baris bahan beserta kolom jumlahnya.
 *
 * Baris yang sudah diisi diwarnai berbeda: di daftar puluhan bahan, baris terpilih
 * yang tampil identik dengan yang kosong membuat orang kehilangan jejak apa saja
 * yang sudah dimasukkan. Kelebihan sisa ditandai merah di tempat, bukan menunggu
 * pengajuan ditolak database.
 */
@Composable
private fun BarisBahanMutasi(
    bahan: BahanPilihan,
    nilai: String,
    melebihi: Boolean,
    onUbah: (String) -> Unit,
    onSeluruhSisa: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val habis = bahan.sisa <= 0.0
    val terisi = (nilai.toDoubleOrNull() ?: 0.0) > 0.0
    val satuan = formatSatuan(bahan.satuan)

    val tepi = when {
        melebihi -> Modifier.border(1.5.dp, WarnaIos.Merah, UkuranIos.SudutKartu)
        terisi -> Modifier.border(1.dp, WarnaIos.Aksen.copy(alpha = 0.35f), UkuranIos.SudutKartu)
        else -> Modifier
    }

    KartuIos(
        modifier.then(tepi),
        padding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        latar = when {
            melebihi -> LATAR_BAHAYA
            terisi -> LATAR_TERISI
            else -> WarnaIos.Kartu
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (terisi && !melebihi) {
                Icon(IkonIos.CheckCircle, null, tint = WarnaIos.Aksen, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    bahan.nama,
                    style = TipeIos.Utama.copy(fontSize = 16.sp, color = if (habis) WarnaIos.LabelKetiga else WarnaIos.Label),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (habis) "Stok habis" else "Sisa ${formatAngkaStok(bahan.sisa)} $satuan",
                        style = TipeIos.Catatan.copy(color = if (habis) NadaIos.BAHAYA.teks else WarnaIos.LabelKedua),
                    )
                    // Mengosongkan satu bahan dari outlet adalah alasan mutasi yang
                    // paling sering; mengetik ulang angka sisa yang panjang
                    // (mis. 7055) hanya mengundang salah ketik.
                    if (!habis) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Semua",
                            Modifier
                                .background(WarnaIos.Aksen.copy(alpha = 0.12f), UkuranIos.SudutKapsul)
                                .tekanIos(onSeluruhSisa)
                                .padding(horizontal = 9.dp, vertical = 3.dp),
                            style = TipeIos.Kecil.copy(color = NadaIos.AKSEN.teks, fontWeight = FontWeight.SemiBold),
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = nilai,
                onValueChange = onUbah,
                modifier = Modifier.width(80.dp),
                enabled = !habis,
                placeholder = { Text("0", color = WarnaIos.LabelKetiga) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = UkuranIos.SudutKontrol,
                colors = warnaKolomIos(galat = melebihi),
            )
            // Satuan ditulis di sebelah kolom: tanpa ini "5" bisa dibaca 5 Kg
            // atau 5 Gram, dan keduanya masuk akal untuk bahan yang sama.
            if (satuan.isNotBlank()) {
                Spacer(Modifier.width(7.dp))
                Text(
                    satuan,
                    Modifier.width(32.dp),
                    style = TipeIos.Catatan.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (melebihi) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(IkonIos.ErrorOutline, null, tint = WarnaIos.Merah, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(5.dp))
                Text(
                    "Melebihi sisa stok (${formatAngkaStok(bahan.sisa)} $satuan)",
                    style = TipeIos.Catatan.copy(color = NadaIos.BAHAYA.teks, fontWeight = FontWeight.SemiBold),
                )
            }
        }
    }
}

@Composable
private fun BilahAjukan(
    state: MutasiUiState,
    viewModel: MutasiViewModel,
    catatanTerbuka: Boolean,
    onCatatanTerbuka: (Boolean) -> Unit,
) {
    val terpilih = state.itemDiajukan
    val halangan = state.halanganAjukan
    val gawat = state.melebihiSisa.isNotEmpty()

    Surface(color = WarnaIos.Kartu, shadowElevation = 10.dp) {
        Column(
            Modifier
                .navigationBarsPaddingKaca()
                .padding(horizontal = UkuranIos.TepiLayar, vertical = 12.dp),
        ) {
            // Ringkasan pilihan: tanpa ini, memeriksa apa saja yang sudah diisi
            // berarti menggulir balik ke atas melewati puluhan baris.
            if (terpilih.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(terpilih, key = { it.first.bahanBakuId }) { (b, q) ->
                        Row(
                            Modifier
                                .background(WarnaIos.Aksen.copy(alpha = 0.12f), UkuranIos.SudutKapsul)
                                .padding(start = 11.dp, end = 7.dp, top = 6.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "${b.nama} ${formatAngkaStok(q)} ${formatSatuan(b.satuan)}".trim(),
                                style = TipeIos.Catatan.copy(color = NadaIos.AKSEN.teks, fontWeight = FontWeight.SemiBold),
                                maxLines = 1,
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                IkonIos.Close, "Hapus ${b.nama}",
                                tint = NadaIos.AKSEN.teks,
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { viewModel.hapusJumlah(b.bahanBakuId) },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            // Catatan jarang diisi, jadi ia tidak lagi menempati ruang di atas daftar
            // bahan — sekarang terlipat di sini dan mekar saat ditekan.
            if (catatanTerbuka) {
                OutlinedTextField(
                    value = state.catatan,
                    onValueChange = viewModel::ubahCatatan,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Catatan untuk penerima…", color = WarnaIos.Abu) },
                    singleLine = true,
                    shape = UkuranIos.SudutKontrol,
                    colors = warnaKolomIos(),
                )
                Spacer(Modifier.height(10.dp))
            } else {
                Row(
                    Modifier
                        .clickable { onCatatanTerbuka(true) }
                        .padding(bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(IkonIos.EditNote, null, tint = WarnaIos.Aksen, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        state.catatan.ifBlank { "Tambah catatan (opsional)" },
                        style = TipeIos.Catatan.copy(
                            color = if (state.catatan.isBlank()) WarnaIos.Aksen else WarnaIos.LabelKedua,
                        ),
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Alasan tombol terkunci ditulis apa adanya. Tombol mati tanpa keterangan
            // membuat orang menekan berulang kali sambil menebak apa yang kurang.
            if (halangan != null) {
                Row(Modifier.padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        IkonIos.ErrorOutline, null,
                        tint = if (gawat) WarnaIos.Merah else WarnaIos.Abu,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        halangan,
                        style = TipeIos.Catatan.copy(
                            color = if (gawat) NadaIos.BAHAYA.teks else WarnaIos.LabelKedua,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }
            }

            TombolUtamaIos(
                when {
                    state.memproses -> "Mengirim…"
                    terpilih.isEmpty() -> "Ajukan Mutasi"
                    else -> "Ajukan ${terpilih.size} bahan"
                },
                viewModel::ajukan,
                aktif = !state.memproses && halangan == null,
                ikon = IkonIos.LocalShipping,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LembarTindakan(state: MutasiUiState, m: Mutasi, viewModel: MutasiViewModel) {
    val sheetState = rememberModalBottomSheetState()
    var alasan by remember { mutableStateOf("") }
    var kondisi by remember { mutableStateOf("baik") }

    ModalBottomSheet(
        onDismissRequest = viewModel::tutupDetail,
        sheetState = sheetState,
        containerColor = WarnaIos.Latar,
    ) {
        Column(Modifier.fillMaxWidth().padding(start = UkuranIos.TepiLayar, end = UkuranIos.TepiLayar, bottom = 28.dp)) {
            Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "${m.outletAsalNama ?: "-"} → ${m.outletTujuanNama ?: "-"}",
                        style = TipeIos.Judul3,
                    )
                    Text(
                        "${m.status.label} · ${waktuSingkat(m.createdAt)}",
                        style = TipeIos.Catatan,
                    )
                    if (!m.catatan.isNullOrBlank()) {
                        Text(m.catatan, style = TipeIos.Catatan)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            val bisaUbahQty = m.status == StatusMutasi.MENUNGGU_PENGIRIMAN || m.status == StatusMutasi.DIKIRIM

            // Rincian bahan sebagai grup iOS: satu kartu, baris dipisah garis hairline.
            KartuIos(padding = PaddingValues(vertical = 4.dp)) {
                m.items.forEachIndexed { i, item ->
                    if (i > 0) PemisahIos()
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                item.namaBahan ?: "-",
                                style = TipeIos.Keterangan.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                buildString {
                                    append("Diajukan ${formatAngkaStok(item.qtyDiajukan)}")
                                    item.qtyDikirim?.let { append(" · dikirim ${formatAngkaStok(it)}") }
                                    item.qtyDiterima?.let { append(" · diterima ${formatAngkaStok(it)}") }
                                    append(" ${formatSatuan(item.satuan)}")
                                },
                                style = TipeIos.Catatan,
                            )
                        }
                        if (bisaUbahQty) {
                            Spacer(Modifier.width(8.dp))
                            OutlinedTextField(
                                value = state.qtyTindakan[item.id].orEmpty(),
                                onValueChange = { viewModel.ubahQtyTindakan(item.id, it) },
                                modifier = Modifier.width(88.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = UkuranIos.SudutKontrol,
                                colors = warnaKolomIos(),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            when (m.status) {
                StatusMutasi.MENUNGGU_PERSETUJUAN -> {
                    OutlinedTextField(
                        value = alasan,
                        onValueChange = { alasan = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Alasan penolakan (wajib bila menolak)", color = WarnaIos.Abu) },
                        shape = UkuranIos.SudutKontrol,
                        colors = warnaKolomIos(),
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TombolKeduaIos(
                            "Tolak",
                            { viewModel.setujui(false, alasan) },
                            Modifier.weight(1f),
                            aktif = !state.memproses,
                            warna = WarnaIos.Merah,
                        )
                        TombolUtamaIos(
                            "Setujui",
                            { viewModel.setujui(true) },
                            Modifier.weight(1f),
                            aktif = !state.memproses,
                            warna = WarnaIos.Hijau,
                        )
                    }
                }

                StatusMutasi.MENUNGGU_PENGIRIMAN -> {
                    OutlinedTextField(
                        value = state.kurir,
                        onValueChange = viewModel::ubahKurir,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Nama kurir / pengantar", color = WarnaIos.Abu) },
                        singleLine = true,
                        shape = UkuranIos.SudutKontrol,
                        colors = warnaKolomIos(),
                    )
                    Spacer(Modifier.height(12.dp))
                    TombolUtamaIos(
                        if (state.memproses) "Memproses…" else "Tandai Terkirim",
                        viewModel::kirim,
                        aktif = !state.memproses,
                    )
                }

                StatusMutasi.DIKIRIM -> {
                    // Kontrol bersegmen ala iOS untuk kondisi barang.
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(WarnaIos.Isian, UkuranIos.SudutKontrol)
                            .padding(2.dp),
                    ) {
                        listOf("baik", "rusak", "hilang").forEach { k ->
                            val aktif = kondisi == k
                            Text(
                                k.replaceFirstChar { it.uppercase() },
                                Modifier
                                    .weight(1f)
                                    .then(if (aktif) Modifier.background(WarnaIos.Kartu, UkuranIos.SudutKontrol) else Modifier)
                                    .tekanIos({ kondisi = k })
                                    .padding(vertical = 8.dp),
                                style = TipeIos.SubJudul.copy(
                                    color = if (aktif) NadaIos.AKSEN.teks else WarnaIos.LabelKedua,
                                    fontWeight = if (aktif) FontWeight.SemiBold else FontWeight.Normal,
                                ),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    TombolUtamaIos(
                        if (state.memproses) "Memproses…" else "Terima Barang",
                        { viewModel.terima(kondisi) },
                        aktif = !state.memproses,
                        warna = WarnaIos.Hijau,
                    )
                }

                else -> Text(
                    "Mutasi ini sudah selesai diproses.",
                    Modifier.padding(horizontal = 4.dp),
                    style = TipeIos.SubJudul,
                )
            }
        }
    }
}
