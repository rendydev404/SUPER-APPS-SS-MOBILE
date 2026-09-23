package com.sukashawarma.superapp.feature.stok.ui.po

import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.core.ui.kaca.denganRuangNav
import com.sukashawarma.superapp.core.ui.kaca.navigationBarsPaddingKaca
import com.sukashawarma.superapp.core.ui.ios.IkonBulatIos
import com.sukashawarma.superapp.core.ui.ios.JudulSeksiIos
import com.sukashawarma.superapp.core.ui.ios.KartuIos
import com.sukashawarma.superapp.core.ui.ios.KeadaanIos
import com.sukashawarma.superapp.core.ui.ios.KolomCariIos
import com.sukashawarma.superapp.core.ui.ios.LencanaIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.TombolBundarIos
import com.sukashawarma.superapp.core.ui.ios.TombolUtamaIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.ios.tekanIos
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.RealtimeTables
import com.sukashawarma.superapp.feature.stok.data.ItemPo
import com.sukashawarma.superapp.feature.stok.data.PoInbound
import com.sukashawarma.superapp.feature.stok.ui.HeaderStok
import com.sukashawarma.superapp.feature.stok.ui.KeadaanGagal
import com.sukashawarma.superapp.feature.stok.ui.MemuatPenuh
import com.sukashawarma.superapp.feature.stok.ui.PitaPesan
import com.sukashawarma.superapp.feature.stok.ui.tanggalSingkat
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import com.sukashawarma.superapp.feature.stok.domain.GerbangTerimaPo
import com.sukashawarma.superapp.feature.stok.domain.PeringatanTerima
import androidx.compose.material3.OutlinedButton

/**
 * Penerimaan PO Supplier — cermin `app/stok/penerimaan-po/page.tsx` web.
 *
 * Dua keadaan: daftar PO yang barangnya masih di jalan, lalu pemeriksaan fisik
 * satu PO. Yang tersimpan bukan sekadar "diterima" — `verifikasi_terima_po`
 * menambah stok gudang dan mencatat harga beli aktual sekaligus.
 */
@Composable
fun PenerimaanPoScreen(
    onBack: () -> Unit,
    viewModel: PenerimaanPoViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()

    if (state.konfirmasi.isNotEmpty()) {
        DialogKonfirmasiTerima(
            konfirmasi = state.konfirmasi,
            mengirim = state.mengirim,
            onLanjut = viewModel::kirim,
            onBatal = viewModel::tutupKonfirmasi,
        )
    }

    RealtimeRefresh(RealtimeTables.PURCHASE_ORDER) { viewModel.muatUlang() }
    BackHandler(enabled = state.poDibuka != null) { viewModel.tutupPo() }

    Column(Modifier.fillMaxSize().background(WarnaIos.Latar)) {
        HeaderStok(
            judul = if (state.poDibuka != null) "Verifikasi Penerimaan" else "Penerimaan PO Supplier",
            subjudul = state.poDibuka?.nomorPo
                ?: if (state.memuat) "Memuat…" else "${state.daftar.size} PO menunggu",
            onKembali = { if (state.poDibuka != null) viewModel.tutupPo() else onBack() },
            aksi = {
                if (state.poDibuka == null) {
                    TombolBundarIos(IkonIos.Refresh, "Muat ulang", viewModel::muatUlang)
                }
            },
        )

        val pesan = state.pesan ?: state.error
        if (pesan != null && !(state.error != null && state.daftar.isEmpty())) {
            PitaPesan(
                pesan = pesan,
                gagal = state.error != null && state.pesan == null,
                onTutup = viewModel::bersihkanPesan,
            )
        }

        when {
            state.poDibuka != null -> FormVerifikasi(state, viewModel)
            state.memuat -> MemuatPenuh(Modifier.fillMaxSize())
            state.error != null && state.daftar.isEmpty() -> KeadaanGagal(
                pesan = state.error.orEmpty(),
                onCobaLagi = viewModel::muatUlang,
                modifier = Modifier.fillMaxSize(),
            )
            else -> DaftarPo(state, viewModel)
        }
    }
}

@Composable
private fun DaftarPo(state: PenerimaanPoUiState, viewModel: PenerimaanPoViewModel) {
    val daftar = state.daftarTampil
    val jumlahSebagian = daftar.count { it.sebagian }
    val sedangMencari = state.cari.isNotBlank()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = UkuranIos.TepiLayar, end = UkuranIos.TepiLayar, top = 12.dp, bottom = 28.dp,
        ).denganRuangNav(),
        verticalArrangement = Arrangement.spacedBy(UkuranIos.JarakKartu),
    ) {
        item(key = "pencarian") {
            KolomCariIos(
                state.cari,
                viewModel::ubahCari,
                Modifier.fillMaxWidth(),
                placeholder = "Cari nomor PO atau supplier",
            )
        }

        item(key = "ringkasan") {
            RingkasanPenerimaan(
                total = daftar.size,
                jumlahSebagian = jumlahSebagian,
                sedangMencari = sedangMencari,
            )
        }

        if (daftar.isEmpty()) {
            item(key = "kosong") { KosongPo(sedangMencari) }
        } else {
            item(key = "judul-daftar") {
                Column {
                    JudulSeksiIos(
                        if (sedangMencari) "Hasil pencarian" else "Perlu diterima",
                        keterangan = "${daftar.size} PO",
                    )
                    Text(
                        if (sedangMencari) "Cocokkan nomor PO atau nama supplier"
                        else "Tap satu PO untuk mulai verifikasi barang",
                        Modifier.padding(horizontal = 4.dp),
                        style = TipeIos.Catatan,
                    )
                }
            }

            items(daftar, key = { it.id }) { po ->
                KartuPo(po) { viewModel.bukaPo(po) }
            }
        }
    }
}

@Composable
private fun KartuPo(po: PoInbound, onKlik: () -> Unit) {
    KartuIos(onKlik = onKlik) {
        Row(verticalAlignment = Alignment.Top) {
            IkonBulatIos(IkonIos.Inventory2, NadaIos.AKSEN.warna, ukuran = 40.dp, padat = false)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        po.nomorPo,
                        Modifier.weight(1f),
                        style = TipeIos.Utama,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Icon(IkonIos.ChevronRight, contentDescription = null, tint = WarnaIos.LabelKetiga, modifier = Modifier.size(15.dp))
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    po.supplierNama,
                    style = TipeIos.SubJudul,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(IkonIos.Schedule, contentDescription = null, tint = WarnaIos.Abu, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(tanggalSingkat(po.tanggal), style = TipeIos.Catatan)
                    Spacer(Modifier.weight(1f))
                    LencanaIos(
                        if (po.sebagian) "Sebagian" else "Menunggu",
                        if (po.sebagian) NadaIos.PERINGATAN else NadaIos.SUKSES,
                    )
                }
            }
        }
    }
}

@Composable
private fun RingkasanPenerimaan(total: Int, jumlahSebagian: Int, sedangMencari: Boolean) {
    KartuIos {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IkonBulatIos(IkonIos.Inventory2, NadaIos.AKSEN.warna, ukuran = 44.dp, padat = false)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(if (sedangMencari) "Hasil antrean" else "Antrean penerimaan", style = TipeIos.Catatan)
                Text(
                    if (total == 0) "Tidak ada PO yang cocok" else "$total PO menunggu barang",
                    style = TipeIos.Judul3,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (sedangMencari) "Perbarui kata kunci untuk hasil lain"
                else "Mulai dari PO paling atas",
                Modifier.weight(1f),
                style = TipeIos.Catatan,
            )
            if (!sedangMencari && jumlahSebagian > 0) {
                LencanaIos("$jumlahSebagian sebagian", NadaIos.PERINGATAN)
            }
        }
    }
}

@Composable
private fun KosongPo(sedangMencari: Boolean) {
    KartuIos(padding = PaddingValues(0.dp)) {
        KeadaanIos(
            ikon = IkonIos.CheckCircle,
            judul = if (sedangMencari) "PO tidak ditemukan" else "Antrean sudah bersih",
            pesan = if (sedangMencari) "Coba nomor PO atau nama supplier lain."
            else "PO 30 hari terakhir yang masih menunggu barang akan muncul di sini.",
            nada = if (sedangMencari) NadaIos.NETRAL else NadaIos.SUKSES,
        )
    }
}

@Composable
private fun FormVerifikasi(state: PenerimaanPoUiState, viewModel: PenerimaanPoViewModel) {
    if (state.memuatItem) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = WarnaIos.Aksen)
        }
    } else {
        val po = state.poDibuka ?: return
        Column(Modifier.fillMaxSize()) {
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(UkuranIos.TepiLayar),
                verticalArrangement = Arrangement.spacedBy(UkuranIos.JarakKartu),
            ) {
                item(key = "panduan-verifikasi") {
                    PanduanVerifikasi(po, state.item.size)
                }
                items(state.item, key = { it.id }) { baris ->
                    KartuItemPo(baris, state, viewModel)
                }
            }

            Surface(color = WarnaIos.Kartu, shadowElevation = 8.dp) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPaddingKaca()
                        .padding(UkuranIos.TepiLayar),
                ) {
                    val halangan = state.halangan
                    if (halangan != null) {
                        Text(halangan, style = TipeIos.Catatan)
                        Spacer(Modifier.height(8.dp))
                    }
                    TombolUtamaIos(
                        if (state.mengirim) "Memproses…" else "Simpan penerimaan",
                        viewModel::mintaKirim,
                        aktif = halangan == null && !state.mengirim,
                    )
                }
            }
        }
    }
}

@Composable
private fun PanduanVerifikasi(po: PoInbound, jumlahBaris: Int) {
    KartuIos {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IkonBulatIos(IkonIos.Inventory2, NadaIos.AKSEN.warna, ukuran = 40.dp, padat = false)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Cek barang yang datang", style = TipeIos.Utama)
                Text(po.supplierNama, style = TipeIos.Catatan, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            LencanaIos("$jumlahBaris baris", NadaIos.AKSEN, titik = false)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Isi jumlah dan harga aktual. Tandai rusak bila perlu, lalu simpan setelah semua baris diperiksa.",
            style = TipeIos.Catatan,
        )
    }
}

@Composable
private fun KartuItemPo(
    baris: ItemPo,
    state: PenerimaanPoUiState,
    viewModel: PenerimaanPoViewModel,
) {
    val isi = state.isianUntuk(baris.id)
    KartuIos {
        Text(
            baris.nama,
            style = TipeIos.Utama,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            buildString {
                append("Dipesan ${angka(baris.qtyPesan)} ${baris.satuan}")
                if (baris.qtyTerimaSebelumnya > 0) {
                    append(" · sudah diterima ${angka(baris.qtyTerimaSebelumnya)}")
                }
            },
            style = TipeIos.Catatan,
        )

        // Akibat ke stok gudang ditampilkan SELALU, bukan cuma saat curiga.
        // Formulir ini dulu tidak pernah menunjukkan akibat apa pun, dan itulah
        // yang membuat salah input 16 September 2026 lolos tanpa terasa.
        val stokSekarang = state.stokBesar(baris.bahanBakuId)
        val stokNanti = state.stokSetelah(baris)
        if (stokSekarang != null && stokNanti != null) {
            Text(
                "Stok gudang ${angka(stokSekarang)} → ${angka(stokNanti)} ${baris.satuan}",
                style = TipeIos.Catatan,
            )
        }

        val peringatan = state.peringatan(baris)
        peringatan.forEach { p ->
            Spacer(Modifier.height(6.dp))
            Text(
                GerbangTerimaPo.pesan(p, baris.satuan),
                Modifier
                    .fillMaxWidth()
                    .background(NadaIos.PERINGATAN.warna.copy(alpha = 0.12f), UkuranIos.SudutKontrol)
                    .padding(10.dp),
                style = TipeIos.Catatan.copy(color = NadaIos.PERINGATAN.teks, fontWeight = FontWeight.Medium),
            )
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = isi.qtyDatang,
                onValueChange = { viewModel.ubahQty(baris.id, it) },
                modifier = Modifier.weight(1f),
                label = { Text("Datang (${baris.satuan})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = UkuranIos.SudutKontrol,
                colors = kolomWarna(),
            )
            OutlinedTextField(
                value = isi.hargaTerima,
                onValueChange = { viewModel.ubahHarga(baris.id, it) },
                modifier = Modifier.weight(1f),
                label = { Text("Harga satuan") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = UkuranIos.SudutKontrol,
                colors = kolomWarna(),
            )
        }

        Spacer(Modifier.height(10.dp))
        // Kontrol bersegmen ala iOS: satu wadah abu, pilihan aktif terangkat berwarna.
        Row(
            Modifier
                .fillMaxWidth()
                .background(WarnaIos.Isian, UkuranIos.SudutKontrol)
                .padding(2.dp),
        ) {
            listOf("baik" to "Baik", "rusak" to "Rusak").forEach { (nilai, label) ->
                val aktif = isi.kondisi == nilai
                val nada = if (nilai == "rusak") NadaIos.BAHAYA else NadaIos.SUKSES
                Text(
                    label,
                    Modifier
                        .weight(1f)
                        .then(
                            if (aktif) Modifier.background(WarnaIos.Kartu, UkuranIos.SudutKontrol) else Modifier,
                        )
                        .tekanIos({ viewModel.ubahKondisi(baris.id, nilai) })
                        .padding(vertical = 8.dp),
                    style = TipeIos.SubJudul.copy(
                        color = if (aktif) nada.teks else WarnaIos.LabelKedua,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    textAlign = TextAlign.Center,
                )
            }
        }

        if (isi.kondisi == "rusak") {
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = isi.catatan,
                onValueChange = { viewModel.ubahCatatan(baris.id, it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Catatan kerusakan", color = WarnaIos.Abu) },
                singleLine = true,
                shape = UkuranIos.SudutKontrol,
                colors = kolomWarna(),
            )
        }
    }
}

@Composable
private fun kolomWarna() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = WarnaIos.Kartu,
    unfocusedContainerColor = WarnaIos.Kartu,
    focusedBorderColor = WarnaIos.Aksen,
    unfocusedBorderColor = WarnaIos.Pemisah,
    focusedLabelColor = WarnaIos.Aksen,
    cursorColor = WarnaIos.Aksen,
)

private fun angka(nilai: Double): String =
    if (nilai % 1.0 == 0.0) nilai.toLong().toString() else nilai.toString()

/**
 * Konfirmasi terakhir sebelum penerimaan disimpan.
 *
 * Bukan penolakan: vendor memang kadang mengirim lebih, dan gudang memang kadang
 * kosong melompong sebelum restock. Yang dituntut hanya satu kali baca ulang,
 * karena `verifikasi_terima_po` memakai GREATEST(0, qty_baru - qty_lama) sehingga
 * kelebihan input tidak bisa diperbaiki lewat formulir ini.
 */
@Composable
private fun DialogKonfirmasiTerima(
    konfirmasi: List<Pair<ItemPo, List<PeringatanTerima>>>,
    mengirim: Boolean,
    onLanjut: () -> Unit,
    onBatal: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!mengirim) onBatal() },
        containerColor = WarnaIos.Kartu,
        shape = UkuranIos.SudutKartu,
        title = {
            Text("Periksa ${konfirmasi.size} baris ini dulu", style = TipeIos.Judul3)
        },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "Jumlah yang salah di sini tidak bisa dikurangi lagi lewat aplikasi — " +
                        "perbaikannya harus lewat database.",
                    style = TipeIos.SubJudul,
                )
                konfirmasi.forEach { (baris, peringatan) ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(NadaIos.PERINGATAN.warna.copy(alpha = 0.10f), UkuranIos.SudutBlok)
                            .padding(12.dp),
                    ) {
                        Text(baris.nama, style = TipeIos.Keterangan.copy(fontWeight = FontWeight.SemiBold))
                        peringatan.forEach { p ->
                            Spacer(Modifier.height(4.dp))
                            Text(
                                GerbangTerimaPo.pesan(p, baris.satuan),
                                style = TipeIos.Catatan.copy(color = NadaIos.PERINGATAN.teks),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onLanjut,
                enabled = !mengirim,
                shape = UkuranIos.SudutKontrol,
                colors = ButtonDefaults.buttonColors(containerColor = WarnaIos.Aksen),
            ) {
                Text(
                    if (mengirim) "Mengirim…" else "Benar, simpan",
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onBatal,
                enabled = !mengirim,
                shape = UkuranIos.SudutKontrol,
                border = BorderStroke(1.dp, WarnaIos.Pemisah),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = WarnaIos.Aksen),
            ) { Text("Periksa lagi", fontWeight = FontWeight.SemiBold) }
        },
    )
}
