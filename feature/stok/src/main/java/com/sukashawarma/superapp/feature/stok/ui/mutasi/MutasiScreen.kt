package com.sukashawarma.superapp.feature.stok.ui.mutasi

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import com.sukashawarma.superapp.presentation.theme.SukaOnSurface
import com.sukashawarma.superapp.presentation.theme.SukaOnSurfaceVariant
import com.sukashawarma.superapp.presentation.theme.SukaSurface
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.RealtimeTables

@Composable
fun MutasiScreen(viewModel: MutasiViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    RealtimeRefresh(RealtimeTables.LEDGER, RealtimeTables.STOK_BALANCE) { viewModel.muatAwal() }

    if (state.formTerbuka) {
        FormAjukan(state, viewModel)
    } else {
    Column(Modifier.fillMaxSize().background(SukaSurface)) {
        HeaderStok(judul = "Mutasi Antar Outlet", subjudul = "Kirim & terima transfer stok")

        if (!state.tidakBerhak && state.outlets.size > 1) {
            PemilihOutlet(state.outlets, state.outletTerpilih, viewModel::pilihOutlet)
        }

        state.pesan?.let { PitaPesan(it, false, viewModel::bersihkanPesan) }
        state.error?.let { PitaPesan(it, true, viewModel::bersihkanPesan) }

        if (!state.tidakBerhak) {
            Button(
                onClick = viewModel::bukaForm,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(7.dp))
                Text("Ajukan Mutasi", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        when {
            state.tidakBerhak -> KeadaanTidakBerhak("Akun Anda belum terhubung dengan outlet mana pun.")
            state.memuat && state.outlets.isEmpty() -> MemuatPenuh()
            state.error != null && state.daftar.isEmpty() -> KeadaanGagal(state.error!!, viewModel::muatAwal)
            state.daftar.isEmpty() -> KeadaanKosong("Belum ada mutasi yang melibatkan outlet ini.")
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                items(state.daftar, key = { it.id }) { m ->
                    KartuMutasi(m) { viewModel.bukaDetail(m) }
                }
            }
        }
    }

    val target = state.detailUntuk
    if (target != null) LembarTindakan(state, target, viewModel)
    }
}

@Composable
private fun KartuMutasi(m: Mutasi, onKlik: () -> Unit) {
    val warna = when (m.status) {
        StatusMutasi.SELESAI -> Color(0xFF168451)
        StatusMutasi.DITOLAK -> Color(0xFFDC2626)
        StatusMutasi.DIKIRIM -> Color(0xFF2563EB)
        else -> Color(0xFFC27A12)
    }
    Surface(
        Modifier.fillMaxWidth().clickable(onClick = onKlik),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            m.outletAsalNama ?: "-",
                            color = SukaOnSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Icon(
                            Icons.Default.ArrowForward, null,
                            tint = Color(0xFFEA580C),
                            modifier = Modifier.size(14.dp).padding(horizontal = 2.dp),
                        )
                        Text(
                            m.outletTujuanNama ?: "-",
                            color = SukaOnSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }
                    Text(
                        "${waktuSingkat(m.createdAt)} · ${m.items.size} bahan · ${m.pembuatNama ?: "-"}",
                        color = SukaOnSurfaceVariant, fontSize = 10.sp,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = warna.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, warna.copy(alpha = 0.28f)),
                ) {
                    Text(
                        m.status.label,
                        Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        color = warna, fontSize = 9.sp, fontWeight = FontWeight.Black,
                    )
                }
            }
            if (!m.catatanPenolakan.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text("Alasan: ${m.catatanPenolakan}", color = Color(0xFFDC2626), fontSize = 10.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
// Palet form pengajuan. Sebelumnya hex disebar inline di tiap composable, jadi
// mengubah satu warna berarti berburu ke belasan tempat.
private val ORANGE = Color(0xFFEA580C)
private val ORANGE_LATAR = Color(0xFFFFF7ED)
private val ORANGE_GARIS = Color(0xFFFED7AA)
private val SLATE400 = Color(0xFF94A3B8)
private val SLATE500 = Color(0xFF64748B)
private val GARIS = Color(0xFFE2E8F0)
private val GARIS_TIPIS = Color(0xFFF1F5F9)
private val MERAH = Color(0xFFB91C1C)
private val MERAH_LATAR = Color(0xFFFEF2F2)
private val ISIAN_KOSONG = Color(0xFFFBFCFE)

@Composable
private fun FormAjukan(state: MutasiUiState, viewModel: MutasiViewModel) {
    var catatanTerbuka by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(SukaSurface)) {
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
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { KartuRute(state, viewModel) }
        stickyHeader { KolomCariBahan(state, viewModel) }

        if (state.bahanTampil.isEmpty()) {
            item {
                Box(
                    Modifier.fillMaxWidth().padding(vertical = 34.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Tidak ada bahan yang cocok.", color = SLATE400, fontSize = 13.sp)
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
                    modifier = Modifier.padding(horizontal = 16.dp),
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

    Surface(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, GARIS),
    ) {
        Column(Modifier.padding(14.dp)) {
            LabelKecil("DARI")
            Spacer(Modifier.height(3.dp))
            Text(
                state.outletTerpilih?.name ?: "—",
                color = SukaOnSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )

            Row(Modifier.padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f).height(1.dp).background(GARIS_TIPIS))
                Icon(
                    Icons.Default.ArrowDownward, null,
                    tint = ORANGE,
                    modifier = Modifier.padding(horizontal = 8.dp).size(15.dp),
                )
                Box(Modifier.weight(1f).height(1.dp).background(GARIS_TIPIS))
            }

            LabelKecil("KE")
            Spacer(Modifier.height(3.dp))
            Surface(
                onClick = { menu = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(11.dp),
                color = if (belumPilih) MERAH_LATAR else ORANGE_LATAR,
                border = BorderStroke(1.dp, if (belumPilih) MERAH.copy(alpha = 0.3f) else ORANGE_GARIS),
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        state.tujuanTerpilih?.name ?: "Pilih outlet tujuan",
                        Modifier.weight(1f),
                        color = if (belumPilih) MERAH else ORANGE,
                        fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Icon(
                        Icons.Default.ArrowDropDown, null,
                        tint = if (belumPilih) MERAH else ORANGE,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
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
private fun LabelKecil(teks: String) {
    Text(teks, color = SLATE400, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.9.sp)
}

@Composable
private fun KolomCariBahan(state: MutasiUiState, viewModel: MutasiViewModel) {
    // Latar wajib buram: sebagai sticky header, baris bahan lewat persis di baliknya.
    Column(Modifier.background(SukaSurface).padding(horizontal = 16.dp, vertical = 2.dp)) {
        OutlinedTextField(
            value = state.cari,
            onValueChange = viewModel::ubahCari,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Cari bahan…", fontSize = 13.sp, color = SLATE400) },
            leadingIcon = {
                Icon(Icons.Default.Search, null, tint = SLATE400, modifier = Modifier.size(18.dp))
            },
            trailingIcon = {
                if (state.cari.isNotEmpty()) {
                    IconButton(onClick = { viewModel.ubahCari("") }) {
                        Icon(
                            Icons.Default.Close, "Hapus pencarian",
                            tint = SLATE400, modifier = Modifier.size(17.dp),
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(13.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = ORANGE,
                unfocusedBorderColor = GARIS,
            ),
        )
        Spacer(Modifier.height(7.dp))
        Text(
            if (state.cari.isBlank()) {
                "${state.bahanTampil.size} bahan tersedia"
            } else {
                "${state.bahanTampil.size} bahan cocok"
            },
            color = SLATE400, fontSize = 11.sp, fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(6.dp))
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

    Surface(
        modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = when {
            melebihi -> MERAH_LATAR
            terisi -> ORANGE_LATAR
            else -> Color.White
        },
        border = BorderStroke(
            if (terisi || melebihi) 1.5.dp else 1.dp,
            when {
                melebihi -> MERAH
                terisi -> ORANGE_GARIS
                else -> GARIS_TIPIS
            },
        ),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (terisi && !melebihi) {
                    Icon(Icons.Default.CheckCircle, null, tint = ORANGE, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(7.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        bahan.nama,
                        color = if (habis) SLATE400 else SukaOnSurface,
                        fontSize = 13.5.sp, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (habis) "Stok habis" else "Sisa ${formatAngkaStok(bahan.sisa)} $satuan",
                            color = if (habis) MERAH else SukaOnSurfaceVariant,
                            fontSize = 11.5.sp, fontWeight = FontWeight.Medium,
                        )
                        // Mengosongkan satu bahan dari outlet adalah alasan mutasi yang
                        // paling sering; mengetik ulang angka sisa yang panjang
                        // (mis. 7055) hanya mengundang salah ketik.
                        if (!habis) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Semua",
                                Modifier
                                    .clickable(onClick = onSeluruhSisa)
                                    .background(Color.White, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 7.dp, vertical = 2.dp),
                                color = ORANGE, fontSize = 10.5.sp, fontWeight = FontWeight.Black,
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
                    placeholder = { Text("0", fontSize = 13.sp, color = Color(0xFFCBD5E1)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(11.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = if (terisi) Color.White else ISIAN_KOSONG,
                        focusedBorderColor = if (melebihi) MERAH else ORANGE,
                        unfocusedBorderColor = if (melebihi) MERAH else GARIS,
                    ),
                )
                // Satuan ditulis di sebelah kolom: tanpa ini "5" bisa dibaca 5 Kg
                // atau 5 Gram, dan keduanya masuk akal untuk bahan yang sama.
                if (satuan.isNotBlank()) {
                    Spacer(Modifier.width(7.dp))
                    Text(
                        satuan,
                        Modifier.width(32.dp),
                        color = SLATE500, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (melebihi) {
                Spacer(Modifier.height(7.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ErrorOutline, null, tint = MERAH, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        "Melebihi sisa stok (${formatAngkaStok(bahan.sisa)} $satuan)",
                        color = MERAH, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                    )
                }
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

    Surface(color = Color.White, shadowElevation = 10.dp) {
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 11.dp),
        ) {
            // Ringkasan pilihan: tanpa ini, memeriksa apa saja yang sudah diisi
            // berarti menggulir balik ke atas melewati puluhan baris.
            if (terpilih.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(terpilih, key = { it.first.bahanBakuId }) { (b, q) ->
                        Surface(
                            shape = RoundedCornerShape(9.dp),
                            color = ORANGE_LATAR,
                            border = BorderStroke(1.dp, ORANGE_GARIS),
                        ) {
                            Row(
                                Modifier.padding(start = 9.dp, end = 5.dp, top = 5.dp, bottom = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "${b.nama} ${formatAngkaStok(q)} ${formatSatuan(b.satuan)}".trim(),
                                    color = ORANGE, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.Close, "Hapus ${b.nama}",
                                    tint = ORANGE,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { viewModel.hapusJumlah(b.bahanBakuId) },
                                )
                            }
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
                    placeholder = { Text("Catatan untuk penerima…", fontSize = 12.5.sp, color = SLATE400) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = ORANGE,
                        unfocusedBorderColor = GARIS,
                    ),
                )
                Spacer(Modifier.height(10.dp))
            } else {
                Row(
                    Modifier
                        .clickable { onCatatanTerbuka(true) }
                        .padding(bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.EditNote, null, tint = SLATE400, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        state.catatan.ifBlank { "Tambah catatan (opsional)" },
                        color = if (state.catatan.isBlank()) SLATE400 else SLATE500,
                        fontSize = 12.sp, fontWeight = FontWeight.Medium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Alasan tombol terkunci ditulis apa adanya. Tombol mati tanpa keterangan
            // membuat orang menekan berulang kali sambil menebak apa yang kurang.
            if (halangan != null) {
                Row(Modifier.padding(bottom = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.ErrorOutline, null,
                        tint = if (gawat) MERAH else SLATE400,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        halangan,
                        color = if (gawat) MERAH else SLATE500,
                        fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Button(
                onClick = viewModel::ajukan,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !state.memproses && halangan == null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ORANGE,
                    disabledContainerColor = GARIS,
                    disabledContentColor = SLATE400,
                ),
                shape = RoundedCornerShape(13.dp),
            ) {
                Icon(Icons.Default.LocalShipping, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    when {
                        state.memproses -> "Mengirim…"
                        terpilih.isEmpty() -> "Ajukan Mutasi"
                        else -> "Ajukan ${terpilih.size} bahan"
                    },
                    fontSize = 14.5.sp, fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LembarTindakan(state: MutasiUiState, m: Mutasi, viewModel: MutasiViewModel) {
    val sheetState = rememberModalBottomSheetState()
    var alasan by remember { mutableStateOf("") }
    var kondisi by remember { mutableStateOf("baik") }

    ModalBottomSheet(onDismissRequest = viewModel::tutupDetail, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 28.dp)) {
            Text(
                "${m.outletAsalNama ?: "-"} → ${m.outletTujuanNama ?: "-"}",
                color = SukaOnSurface, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
            )
            Text(
                "${m.status.label} · ${waktuSingkat(m.createdAt)}",
                color = SukaOnSurfaceVariant, fontSize = 11.sp,
            )
            if (!m.catatan.isNullOrBlank()) {
                Text(m.catatan, color = SukaOnSurfaceVariant, fontSize = 11.sp)
            }
            Spacer(Modifier.height(12.dp))

            val bisaUbahQty = m.status == StatusMutasi.MENUNGGU_PENGIRIMAN || m.status == StatusMutasi.DIKIRIM

            m.items.forEach { item ->
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            item.namaBahan ?: "-",
                            color = SukaOnSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            buildString {
                                append("Diajukan ${formatAngkaStok(item.qtyDiajukan)}")
                                item.qtyDikirim?.let { append(" · dikirim ${formatAngkaStok(it)}") }
                                item.qtyDiterima?.let { append(" · diterima ${formatAngkaStok(it)}") }
                                append(" ${formatSatuan(item.satuan)}")
                            },
                            color = SukaOnSurfaceVariant, fontSize = 10.sp,
                        )
                    }
                    if (bisaUbahQty) {
                        OutlinedTextField(
                            value = state.qtyTindakan[item.id].orEmpty(),
                            onValueChange = { viewModel.ubahQtyTindakan(item.id, it) },
                            modifier = Modifier.width(88.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(11.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color(0xFFFBFCFE),
                                focusedBorderColor = Color(0xFFF97316),
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                            ),
                        )
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
                        placeholder = { Text("Alasan penolakan (wajib bila menolak)", fontSize = 12.sp) },
                        shape = RoundedCornerShape(12.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.setujui(false, alasan) },
                            modifier = Modifier.weight(1f),
                            enabled = !state.memproses,
                            shape = RoundedCornerShape(13.dp),
                        ) { Text("Tolak", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626)) }
                        Button(
                            onClick = { viewModel.setujui(true) },
                            modifier = Modifier.weight(1f),
                            enabled = !state.memproses,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF168451)),
                            shape = RoundedCornerShape(13.dp),
                        ) { Text("Setujui", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                    }
                }

                StatusMutasi.MENUNGGU_PENGIRIMAN -> {
                    OutlinedTextField(
                        value = state.kurir,
                        onValueChange = viewModel::ubahKurir,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Nama kurir / pengantar", fontSize = 12.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = viewModel::kirim,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.memproses,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                        shape = RoundedCornerShape(13.dp),
                    ) {
                        Text(
                            if (state.memproses) "Memproses…" else "Tandai Terkirim",
                            fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        )
                    }
                }

                StatusMutasi.DIKIRIM -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("baik", "rusak", "hilang").forEach { k ->
                            OutlinedButton(
                                onClick = { kondisi = k },
                                shape = RoundedCornerShape(50),
                            ) {
                                Text(
                                    k.replaceFirstChar { it.uppercase() },
                                    fontSize = 12.sp,
                                    fontWeight = if (kondisi == k) FontWeight.Black else FontWeight.Normal,
                                    color = if (kondisi == k) Color(0xFFEA580C) else SukaOnSurfaceVariant,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.terima(kondisi) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.memproses,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF168451)),
                        shape = RoundedCornerShape(13.dp),
                    ) {
                        Text(
                            if (state.memproses) "Memproses…" else "Terima Barang",
                            fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        )
                    }
                }

                else -> Text(
                    "Mutasi ini sudah selesai diproses.",
                    color = SukaOnSurfaceVariant, fontSize = 12.sp,
                )
            }
        }
    }
}
