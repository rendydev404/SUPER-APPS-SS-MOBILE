package com.sukashawarma.superapp.feature.stok.ui.opname

import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.core.ui.kaca.denganRuangNav
import com.sukashawarma.superapp.core.ui.kaca.navigationBarsPaddingKaca
import com.sukashawarma.superapp.core.ui.ios.KartuIos
import com.sukashawarma.superapp.core.ui.ios.KolomCariIos
import com.sukashawarma.superapp.core.ui.ios.LencanaIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.TombolKeduaIos
import com.sukashawarma.superapp.core.ui.ios.TombolUtamaIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.feature.stok.data.model.OpnameHeader
import com.sukashawarma.superapp.feature.stok.data.model.OpnameItemRow
import com.sukashawarma.superapp.feature.stok.data.model.StatusOpname
import com.sukashawarma.superapp.feature.stok.domain.Selisih
import com.sukashawarma.superapp.feature.stok.domain.formatAngkaStok
import com.sukashawarma.superapp.feature.stok.domain.formatSatuan
import com.sukashawarma.superapp.feature.stok.ui.HeaderStok
import com.sukashawarma.superapp.feature.stok.ui.KeadaanGagal
import com.sukashawarma.superapp.feature.stok.ui.KeadaanKosong
import com.sukashawarma.superapp.feature.stok.ui.KeadaanTidakBerhak
import com.sukashawarma.superapp.feature.stok.ui.MemuatPenuh
import com.sukashawarma.superapp.feature.stok.ui.PemilihOutlet
import com.sukashawarma.superapp.feature.stok.ui.PitaPesan
import com.sukashawarma.superapp.feature.stok.ui.tanggalSingkat
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.RealtimeTables
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import com.sukashawarma.superapp.feature.stok.domain.Penurunan

@Composable
fun OpnameScreen(viewModel: OpnameViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    // SURAT_JALAN ikut dipantau supaya peringatan verifikasi hilang sendiri begitu
    // kru memverifikasi kirimannya.
    RealtimeRefresh(RealtimeTables.OPNAME, RealtimeTables.OPNAME_ITEM, RealtimeTables.SURAT_JALAN) {
        viewModel.muatAwal()
    }

    // Detail ditampilkan menukar isi layar, bukan lewat rute tersendiri: OpnameScreen
    // dirender langsung oleh StokShell sebagai tab, sehingga menambah rute berarti
    // menjahit callback menembus dua lapis untuk keuntungan yang tidak ada.
    var detailId by rememberSaveable { mutableStateOf<String?>(null) }
    BackHandler(enabled = detailId != null) { detailId = null }

    // Gerbang penurunan drastis digambar di atas apa pun yang sedang tampil.
    if (state.gerbangPenurunanTerbuka) {
        DialogPenurunanDrastis(
            penurunan = state.penurunan,
            menyimpan = state.menyimpan,
            onLewati = viewModel::lewatiPenurunan,
            onLanjut = viewModel::finalisasi,
            onBatal = viewModel::tutupGerbangPenurunan,
        )
    }

    val detailTerbuka = detailId
    if (detailTerbuka != null) {
        DetailOpnameScreen(opnameId = detailTerbuka, onKembali = { detailId = null })
    } else if (state.formTerbuka) {
        FormOpname(state, viewModel)
    } else {
    Column(Modifier.fillMaxSize().background(WarnaIos.Latar)) {
        HeaderStok(judul = "Stock Opname", subjudul = "Hitung fisik & rekonsiliasi stok")

        if (!state.tidakBerhak && state.outlets.size > 1) {
            PemilihOutlet(state.outlets, state.outletTerpilih, viewModel::pilihOutlet)
        }

        state.pesan?.let { PitaPesan(it, false, viewModel::bersihkanPesan) }

        if (!state.tidakBerhak) {
            if (state.terhalangSuratJalan) {
                PeringatanSuratJalan(
                    state.sjBelumDiverifikasi,
                    Modifier.padding(horizontal = UkuranIos.TepiLayar, vertical = 12.dp),
                )
            } else if (state.crewSudahOpname) {
                // Crew yang sudah opname hari ini: tampilkan pesan, bukan tombol.
                KartuIos(Modifier.padding(horizontal = UkuranIos.TepiLayar, vertical = 12.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            IkonIos.CheckCircle, null,
                            tint = WarnaIos.Hijau,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Opname hari ini sudah selesai",
                            style = TipeIos.Keterangan.copy(fontWeight = FontWeight.SemiBold),
                        )
                    }
                }
            } else {
                val adaDraft = state.riwayat.any { it.status == StatusOpname.DRAFT }
                val draftHeader = state.riwayat.firstOrNull { it.status == StatusOpname.DRAFT }
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = UkuranIos.TepiLayar, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    TombolUtamaIos(
                        if (adaDraft) "Lanjutkan Draft" else "Mulai Opname Hari Ini",
                        viewModel::bukaForm,
                        ikon = if (adaDraft) IkonIos.EditNote else IkonIos.Add,
                        // Oranye sistem membedakan "lanjutkan" dari "mulai" sekilas pandang.
                        warna = if (adaDraft) WarnaIos.Oranye else WarnaIos.Aksen,
                    )
                    if (adaDraft && draftHeader != null && draftHeader.jumlahItem > 0) {
                        Spacer(Modifier.height(6.dp))
                        Text("${draftHeader.jumlahItem} item tersimpan", style = TipeIos.Catatan)
                    }
                }
            }
        }

        when {
            state.tidakBerhak -> KeadaanTidakBerhak("Akun Anda belum terhubung dengan outlet mana pun.")
            state.memuat && state.outlets.isEmpty() -> MemuatPenuh()
            state.error != null -> KeadaanGagal(state.error!!, viewModel::muatAwal)
            state.riwayat.isEmpty() -> KeadaanKosong("Belum ada riwayat opname di outlet ini.")
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = UkuranIos.TepiLayar, end = UkuranIos.TepiLayar, bottom = 20.dp,
                ).denganRuangNav(),
                verticalArrangement = Arrangement.spacedBy(UkuranIos.JarakKartu),
            ) {
                items(state.riwayat, key = { it.id }) { h ->
                    KartuRiwayat(h, onKlik = { detailId = h.id })
                }
            }
        }
    }
    }
}

/**
 * Peringatan besar pengganti tombol opname selama masih ada surat jalan yang
 * belum diverifikasi. Sengaja memakai teks besar dan warna peringatan penuh —
 * pesannya harus terbaca sekilas oleh kru yang sedang memegang barang.
 */
@Composable
private fun PeringatanSuratJalan(nomor: List<String>, modifier: Modifier = Modifier) {
    val nada = NadaIos.PERINGATAN
    Column(
        modifier
            .fillMaxWidth()
            .background(nada.warna.copy(alpha = 0.12f), UkuranIos.SudutKartu)
            .border(2.dp, nada.warna, UkuranIos.SudutKartu)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(IkonIos.WarningAmber, null, tint = nada.warna, modifier = Modifier.size(44.dp))
        Spacer(Modifier.height(10.dp))
        Text(
            "Verifikasi Surat Jalan Dulu",
            style = TipeIos.Judul2.copy(color = nada.teks),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Opname belum bisa dilakukan. Masih ada ${nomor.size} surat jalan " +
                "yang belum diverifikasi di outlet ini.",
            style = TipeIos.Isi,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        nomor.forEach { no ->
            Text(
                no,
                style = TipeIos.Utama.copy(color = nada.teks),
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Buka menu Distribusi → Penerimaan Barang, verifikasi barangnya, lalu kembali ke sini.",
            style = TipeIos.Keterangan.copy(fontWeight = FontWeight.SemiBold),
            textAlign = TextAlign.Center,
        )
    }
}

private fun StatusOpname.nadaIos(): NadaIos = when (this) {
    StatusOpname.FINALIZED, StatusOpname.APPROVED -> NadaIos.SUKSES
    StatusOpname.PENDING_APPROVAL -> NadaIos.PERINGATAN
    StatusOpname.REJECTED -> NadaIos.BAHAYA
    StatusOpname.DRAFT -> NadaIos.NETRAL
}

@Composable
private fun KartuRiwayat(h: OpnameHeader, onKlik: () -> Unit) {
    KartuIos(onKlik = onKlik) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(tanggalSingkat(h.tanggal), style = TipeIos.Utama)
                Text(
                    listOfNotNull(h.tipe, h.creatorName).joinToString(" · ").ifBlank { "-" },
                    style = TipeIos.Catatan,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (h.jumlahItem > 0) {
                    Text(
                        "${h.jumlahItem} item" + if (h.jumlahFlagged > 0) " · ${h.jumlahFlagged} di luar toleransi" else "",
                        style = TipeIos.Catatan.copy(
                            color = if (h.jumlahFlagged > 0) NadaIos.PERINGATAN.teks else WarnaIos.LabelKedua,
                        ),
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            LencanaIos(h.status.label, h.status.nadaIos())
            Spacer(Modifier.width(6.dp))
            Icon(IkonIos.ChevronRight, null, tint = WarnaIos.LabelKetiga, modifier = Modifier.size(15.dp))
        }
    }
}

// -------------------------------------------------------------------- formulir

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormOpname(state: OpnameUiState, viewModel: OpnameViewModel) {
    Column(Modifier.fillMaxSize().background(WarnaIos.Latar)) {
        HeaderStok(
            judul = "Hitung Fisik",
            subjudul = "${state.jumlahTerisi} dari ${state.items.size} bahan terisi",
            onKembali = viewModel::tutupForm,
        )

        state.error?.let { PitaPesan(it, true, viewModel::bersihkanPesan) }
        state.pesan?.let { PitaPesan(it, false, viewModel::bersihkanPesan) }

        val terkunci = state.terkunci
        if (state.memuatForm) {
            MemuatPenuh()
        } else if (terkunci != null) {
            // Form sengaja tidak dirender sama sekali: menampilkan kolom isian yang
            // penyimpanannya pasti ditolak lebih buruk daripada tidak menampilkannya.
            KeadaanKosong(terkunci.pesan)
        } else {
        // Kiriman tiba saat form terbuka: hitungan tetap bisa disimpan sebagai draft,
        // hanya finalisasinya yang ditahan sampai surat jalan diverifikasi.
        if (state.terhalangSuratJalan) {
            PeringatanSuratJalan(
                state.sjBelumDiverifikasi,
                Modifier.padding(start = UkuranIos.TepiLayar, end = UkuranIos.TepiLayar, top = 10.dp),
            )
        }
        KolomCariIos(
            state.cari,
            viewModel::ubahCari,
            Modifier.fillMaxWidth().padding(horizontal = UkuranIos.TepiLayar, vertical = 10.dp),
            placeholder = "Cari bahan…",
        )

        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(start = UkuranIos.TepiLayar, end = UkuranIos.TepiLayar, top = 2.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(UkuranIos.JarakKartu),
        ) {
            items(state.itemTampil, key = { it.bahanBakuId }) { item ->
                BarisHitung(item, viewModel)
            }
        }

        Surface(color = WarnaIos.Kartu, shadowElevation = 8.dp) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPaddingKaca()
                    .padding(UkuranIos.TepiLayar),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TombolKeduaIos(
                    "Simpan Draft",
                    viewModel::simpanDraft,
                    Modifier.weight(1f),
                    aktif = !state.menyimpan,
                )
                TombolUtamaIos(
                    if (state.menyimpan) "Memproses…" else "Finalisasi",
                    viewModel::mintaFinalisasi,
                    Modifier.weight(1f),
                    aktif = !state.menyimpan && !state.terhalangSuratJalan,
                )
            }
        }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BarisHitung(item: OpnameItemRow, viewModel: OpnameViewModel) {
    val selisih = viewModel.selisih(item)
    val ditandai = viewModel.ditandai(item)
    val persen = Selisih.persen(selisih, item.qtySystemSmallest)

    // Garis tepi hanya untuk selisih di luar toleransi — satu-satunya keadaan yang
    // harus menarik mata. Baris tersimpan cukup ditandai lencana hijau.
    val tepiBahaya = if (item.adaMasukan && ditandai) {
        Modifier.border(1.dp, WarnaIos.Merah.copy(alpha = 0.45f), UkuranIos.SudutKartu)
    } else {
        Modifier
    }

    KartuIos(tepiBahaya) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    item.namaBahan,
                    style = TipeIos.Utama,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "Sistem: ${formatAngkaStok(item.qtySystemSmallest)} ${formatSatuan(item.meta.satuanKecil ?: item.meta.satuan)}" +
                        if (item.terukur) " · toleransi 5%" else " · toleransi 0%",
                    style = TipeIos.Catatan,
                )
                // Tanda visual: baris yang sudah tersimpan sebagai draft tidak polos.
                if (item.tersimpanDraft) {
                    Spacer(Modifier.height(5.dp))
                    LencanaIos("Tersimpan", NadaIos.SUKSES, ikon = IkonIos.Check)
                }
            }
            if (item.adaMasukan) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        (if (selisih > 0) "+" else "") + formatAngkaStok(selisih),
                        style = TipeIos.Angka.copy(color = if (ditandai) WarnaIos.Merah else NadaIos.SUKSES.teks),
                    )
                    Text(persen.teks, style = TipeIos.Kecil)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KolomAngka(
                label = formatSatuan(item.meta.satuan).ifBlank { "Besar" },
                nilai = item.besar,
                modifier = Modifier.weight(1f),
            ) { viewModel.ubahMasukan(item.bahanBakuId, besar = it) }

            if (item.meta.satuanTengah != null) {
                KolomAngka(
                    label = formatSatuan(item.meta.satuanTengah),
                    nilai = item.tengah,
                    modifier = Modifier.weight(1f),
                ) { viewModel.ubahMasukan(item.bahanBakuId, tengah = it) }
            }

            if (item.meta.satuanKecil != null) {
                KolomAngka(
                    label = formatSatuan(item.meta.satuanKecil),
                    nilai = item.kecil,
                    modifier = Modifier.weight(1f),
                ) { viewModel.ubahMasukan(item.bahanBakuId, kecil = it) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KolomAngka(
    label: String,
    nilai: String,
    modifier: Modifier = Modifier,
    onUbah: (String) -> Unit,
) {
    Column(modifier) {
        Text(
            label,
            style = TipeIos.Kecil.copy(fontWeight = FontWeight.Medium),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = nilai,
            // Hanya angka dan satu titik desimal yang diterima; menolak di sini lebih
            // baik daripada membiarkan teks tak terbaca lalu diam-diam dianggap nol.
            onValueChange = { teks ->
                if (teks.isEmpty() || teks.matches(Regex("^\\d*\\.?\\d*$"))) onUbah(teks)
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("0", style = TipeIos.Keterangan.copy(color = WarnaIos.LabelKetiga)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = UkuranIos.SudutKontrol,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = WarnaIos.Kartu,
                unfocusedContainerColor = WarnaIos.Kartu,
                focusedBorderColor = WarnaIos.Aksen,
                unfocusedBorderColor = WarnaIos.Pemisah,
                cursorColor = WarnaIos.Aksen,
            ),
        )
    }
}

/**
 * Konfirmasi terakhir sebelum opname difinalisasi — cermin modal penurunan drastis
 * di `OpnameForm.tsx` web.
 *
 * Yang ditawarkan bukan cuma "lanjut atau batal". Baris yang nol-nya mencurigakan
 * mendapat tombol "Belum dihitung", yang mengembalikannya ke keadaan kosong sehingga
 * saldo sistemnya tidak disentuh sama sekali. Tanpa jalan keluar itu, kru yang
 * mengetik 0 bermaksud "belum saya hitung" hanya punya satu pilihan: menekan lanjut.
 */
@Composable
private fun DialogPenurunanDrastis(
    penurunan: List<Penurunan>,
    menyimpan: Boolean,
    onLewati: (String) -> Unit,
    onLanjut: () -> Unit,
    onBatal: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!menyimpan) onBatal() },
        containerColor = WarnaIos.Kartu,
        shape = UkuranIos.SudutKartu,
        title = {
            Text("Periksa ${penurunan.size} bahan ini dulu", style = TipeIos.Judul3)
        },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "Hitungannya turun jauh di bawah catatan sistem. Finalisasi akan " +
                        "memotong stoknya sebanyak selisih itu, dan tidak bisa dibatalkan " +
                        "dari aplikasi.",
                    style = TipeIos.SubJudul,
                )
                penurunan.forEach { baris ->
                    val nada = if (baris.habisTotal) NadaIos.BAHAYA else NadaIos.PERINGATAN
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(nada.warna.copy(alpha = 0.10f), UkuranIos.SudutBlok)
                            .padding(12.dp),
                    ) {
                        Text(baris.calon.nama, style = TipeIos.Keterangan.copy(fontWeight = FontWeight.SemiBold))
                        Text(
                            if (baris.habisTotal) {
                                "Ditandai HABIS, padahal sistem masih mencatat stok."
                            } else {
                                "Turun jauh dari catatan sistem."
                            },
                            style = TipeIos.Catatan.copy(color = nada.teks),
                        )
                        if (baris.bolehLewati) {
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { onLewati(baris.calon.bahanBakuId) },
                                enabled = !menyimpan,
                                shape = UkuranIos.SudutKontrol,
                                border = BorderStroke(1.dp, WarnaIos.Pemisah),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = WarnaIos.Kartu,
                                    contentColor = WarnaIos.Aksen,
                                ),
                            ) {
                                Text("Belum dihitung, lewati", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onLanjut,
                enabled = !menyimpan,
                colors = ButtonDefaults.buttonColors(containerColor = WarnaIos.Aksen),
                shape = UkuranIos.SudutKontrol,
            ) {
                Text(
                    if (menyimpan) "Memproses…" else "Benar, finalisasi",
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onBatal,
                enabled = !menyimpan,
                shape = UkuranIos.SudutKontrol,
                border = BorderStroke(1.dp, WarnaIos.Pemisah),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = WarnaIos.Aksen),
            ) { Text("Periksa lagi", fontWeight = FontWeight.SemiBold) }
        },
    )
}
