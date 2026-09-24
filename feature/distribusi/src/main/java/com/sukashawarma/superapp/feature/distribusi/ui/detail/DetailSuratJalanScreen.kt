package com.sukashawarma.superapp.feature.distribusi.ui.detail

import android.graphics.Bitmap
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.RealtimeTables
import com.sukashawarma.superapp.core.ui.ios.AngkaIos
import com.sukashawarma.superapp.core.ui.ios.BilahJudulIos
import com.sukashawarma.superapp.core.ui.ios.BlokAngkaIos
import com.sukashawarma.superapp.core.ui.ios.IkonBulatIos
import com.sukashawarma.superapp.core.ui.ios.JudulSeksiIos
import com.sukashawarma.superapp.core.ui.ios.KartuIos
import com.sukashawarma.superapp.core.ui.ios.LencanaIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.PemisahIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.feature.distribusi.data.model.SuratJalanDetail
import com.sukashawarma.superapp.feature.distribusi.data.model.TandaTangan
import com.sukashawarma.superapp.feature.distribusi.domain.PengirimanPusat
import com.sukashawarma.superapp.feature.distribusi.domain.StatusSuratJalan
import com.sukashawarma.superapp.feature.distribusi.domain.bolehDitutup
import com.sukashawarma.superapp.feature.distribusi.ui.LayarGalat
import com.sukashawarma.superapp.feature.distribusi.ui.LayarMemuat
import com.sukashawarma.superapp.feature.distribusi.ui.LencanaStatus
import com.sukashawarma.superapp.feature.distribusi.ui.formatTanggal

@Composable
fun DetailSuratJalanScreen(
    suratJalanId: String,
    onKeluar: () -> Unit,
    /** Setelah dokumen dikirim atau dibatalkan — kembali ke daftar. */
    onSelesaiAksi: () -> Unit = onKeluar,
) {
    val viewModel: DetailViewModel = viewModel(factory = DetailViewModel.Factory(suratJalanId))
    val state by viewModel.state.collectAsState()
    RealtimeRefresh(RealtimeTables.SURAT_JALAN) { viewModel.muat() }
    val snackbar = remember { SnackbarHostState() }
    var mintaBatal by remember { mutableStateOf(false) }
    var mintaTutup by remember { mutableStateOf(false) }
    // Di luar LazyColumn dan saveable: menggulir daftar atau memutar layar tidak
    // boleh membuang goresan tanda tangan yang sedang dibuat.
    var ttdPeran by rememberSaveable { mutableStateOf<String?>(null) }

    // Tampilkan dulu, baru bersihkan. Membersihkan lebih dulu mengubah kunci efek,
    // efeknya dibatalkan, dan snackbar ikut tertutup sebelum sempat terbaca.
    LaunchedEffect(state.pesan, state.galatAksi) {
        val teks = state.galatAksi ?: state.pesan
        if (teks != null) {
            snackbar.showSnackbar(teks)
            viewModel.bersihkanPesan()
        }
    }
    // Form tanda tangan hanya ditutup setelah server benar-benar mencatat perannya.
    LaunchedEffect(state.peranTtd, ttdPeran) {
        val peran = ttdPeran ?: return@LaunchedEffect
        val tercatat = if (peran == PengirimanPusat.PERAN_ADMIN) PengirimanPusat.sudahTtdAdmin(state.peranTtd)
        else PengirimanPusat.sudahTtdSupir(state.peranTtd)
        if (tercatat || !state.draft) ttdPeran = null
    }
    LaunchedEffect(state.selesaiAksi) {
        if (state.selesaiAksi) {
            // Beri waktu snackbar sukses terbaca sebelum layar ditutup, seperti jeda 1 detik web.
            kotlinx.coroutines.delay(1000)
            onSelesaiAksi()
        }
    }

    if (state.memuat && state.detail == null) { LayarMemuat(); return }
    val detail = state.detail
    if (detail == null) {
        LayarGalat(state.error ?: "Dokumen tidak bisa dibuka.") { viewModel.muat() }
        return
    }

    LaunchedEffect(state.baris) {
        state.baris.mapNotNull { it.fotoPath }.forEach { viewModel.muatFoto(it) }
    }

    val jumlahBarang = state.baris.size
    val jumlahDiperiksa = state.baris.count { it.qtyTerima != null }
    val jumlahBermasalah = state.baris.count { it.bermasalah }
    val jumlahBukti = state.baris.count { it.fotoPath != null }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(WarnaIos.Latar)) {
            KepalaDetail(detail, state.baris.any { it.bermasalah }, onKeluar)

            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = UkuranIos.TepiLayar,
                    end = UkuranIos.TepiLayar,
                    top = 12.dp,
                    bottom = 28.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(UkuranIos.JarakKartu),
            ) {
                item(key = "ringkasan") {
                    RingkasanDetail(
                        jumlahBarang = jumlahBarang,
                        jumlahDiperiksa = jumlahDiperiksa,
                        jumlahBermasalah = jumlahBermasalah,
                        jumlahBukti = jumlahBukti,
                    )
                }

                if (detail.status == StatusSuratJalan.DIBATALKAN) {
                    item(key = "dibatalkan") { KartuDibatalkan(detail.catatan) }
                }

                if (state.bolehTutup && detail.status?.bolehDitutup == true) {
                    item(key = "verifikasi-akhir") { KartuVerifikasiAkhir(state) { mintaTutup = true } }
                }

                // Kode verifikasi hanya untuk pengawas — lihat DistribusiAkses — dan
                // baru bermakna setelah dikirim; saat draft web menampilkannya terkunci.
                if (state.bolehLihatKode && detail.kodeVerifikasi != null && PengirimanPusat.kodeTerbuka(detail.status)) {
                    item(key = "kode-verifikasi") {
                        KartuKodeVerifikasi(detail.kodeVerifikasi)
                    }
                }

                if (state.draft && state.bolehKelolaKirim) {
                    item(key = "vendor-draft") { KartuVendorDraft(state, viewModel) }
                }

                item(key = "judul-rincian") {
                    KepalaBagian(
                        judul = "Rincian barang",
                        keterangan = "Bandingkan jumlah kirim dengan jumlah yang diterima",
                        jumlah = "$jumlahBarang item",
                    )
                }

                itemsIndexed(state.baris) { indeks, baris ->
                    KartuItemDetail(
                        nomor = indeks + 1,
                        baris = baris,
                        bitmap = baris.fotoPath?.let { state.foto[it] },
                    )
                }

                item(key = "judul-persetujuan") {
                    KepalaBagian("Persetujuan", "Jejak tanda tangan dokumen", "2 tahap")
                }
                item(key = "ttd-pengirim") { BlokTandaTangan("Tanda Tangan Pengirim", detail.ttdPengirim) }
                if (state.draft) {
                    item(key = "aksi-draft") {
                        KartuPengirimanDraft(
                            state = state,
                            viewModel = viewModel,
                            onMulaiTtd = { ttdPeran = it },
                            onMintaBatal = { mintaBatal = true },
                        )
                    }
                }
                item(key = "ttd-penerimaan") { BlokTandaTangan("Tanda Tangan Penerimaan", detail.ttdPenerimaan) }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding())
    }

    ttdPeran?.let { peran ->
        LayarTtdPengirim(
            peran = peran,
            state = state,
            onSimpan = { nama, gambar -> viewModel.tandaTangan(peran, nama, gambar) },
            onTutup = { ttdPeran = null },
        )
    }

    if (mintaBatal) {
        DialogBatalkan(
            onKonfirmasi = { alasan ->
                mintaBatal = false
                viewModel.batalkan(alasan)
            },
            onTutup = { mintaBatal = false },
        )
    }
    if (mintaTutup) {
        DialogTutup(
            onKonfirmasi = {
                mintaTutup = false
                viewModel.tutupDokumen()
            },
            onTutup = { mintaTutup = false },
        )
    }
}

@Composable
private fun KepalaDetail(detail: SuratJalanDetail, adaSelisih: Boolean, onKeluar: () -> Unit) {
    BilahJudulIos(
        judul = "SJ ${detail.nomorDokumen ?: detail.id.take(8).uppercase()}",
        subjudul = "${detail.namaOutlet ?: "Gudang Pusat"} • ${formatTanggal(detail.dibuatPada)}",
        onKembali = onKeluar,
    ) {
        LencanaStatus(detail.status, adaSelisih)
    }
}

@Composable
private fun RingkasanDetail(
    jumlahBarang: Int,
    jumlahDiperiksa: Int,
    jumlahBermasalah: Int,
    jumlahBukti: Int,
) {
    KartuIos {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Ringkasan kiriman", style = TipeIos.Catatan)
                Text(
                    if (jumlahBermasalah == 0) "Kiriman terlihat sesuai" else "$jumlahBermasalah item perlu perhatian",
                    style = TipeIos.Utama.copy(
                        color = if (jumlahBermasalah == 0) WarnaIos.Label else NadaIos.BAHAYA.teks,
                    ),
                )
            }
            Spacer(Modifier.width(8.dp))
            LencanaIos("$jumlahBarang barang", NadaIos.NETRAL, titik = false, ikon = IkonIos.Inventory2)
        }
        Spacer(Modifier.height(14.dp))
        BlokAngkaIos(
            listOf(
                AngkaIos("Diperiksa", "$jumlahDiperiksa/$jumlahBarang"),
                AngkaIos("Perhatian", jumlahBermasalah.toString(), negatif = jumlahBermasalah > 0),
                AngkaIos("Bukti foto", jumlahBukti.toString()),
            ),
        )
    }
}

@Composable
private fun KartuKodeVerifikasi(kode: String) {
    KartuIos {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IkonBulatIos(IkonIos.Lock, NadaIos.AKSEN.warna, ukuran = 38.dp, padat = false)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Kode verifikasi", style = TipeIos.Catatan)
                Text(kode, style = TipeIos.Angka.copy(letterSpacing = 1.5.sp))
            }
            LencanaIos("Pengawas", NadaIos.UNGU, titik = false)
        }
    }
}

@Composable
private fun KepalaBagian(judul: String, keterangan: String, jumlah: String) {
    Column {
        JudulSeksiIos(judul, keterangan = jumlah)
        Text(keterangan, Modifier.padding(horizontal = 4.dp), style = TipeIos.Catatan)
    }
}

/** Strip keterangan berlatar tipis bernada di dalam kartu (peringatan, catatan). */
@Composable
private fun StripKeterangan(ikon: ImageVector, teks: String, nada: NadaIos, tebal: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(UkuranIos.SudutKontrol)
            .background(if (nada == NadaIos.NETRAL) WarnaIos.Latar else nada.warna.copy(alpha = 0.10f))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            ikon,
            contentDescription = null,
            tint = if (nada == NadaIos.NETRAL) WarnaIos.Abu else nada.teks,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            teks,
            color = if (nada == NadaIos.NETRAL) WarnaIos.LabelKedua else nada.teks,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontWeight = if (tebal) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun KartuItemDetail(nomor: Int, baris: BarisItemDetail, bitmap: Bitmap?) {
    KartuIos {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                Modifier.size(34.dp).clip(CircleShape).background(WarnaIos.Aksen.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    nomor.toString().padStart(2, '0'),
                    color = NadaIos.AKSEN.teks,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(baris.nama, style = TipeIos.Utama, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    "Satuan ${baris.satuan}" + (baris.vendorNama?.let { " · Vendor $it" } ?: ""),
                    style = TipeIos.Catatan,
                )
            }
            Spacer(Modifier.width(8.dp))
            LabelKondisi(baris)
        }

        Spacer(Modifier.height(14.dp))
        Text("Perbandingan jumlah", style = TipeIos.Catatan)
        Spacer(Modifier.height(6.dp))
        BlokAngkaIos(
            listOf(
                AngkaIos("Dikirim", baris.qtyDikirim.toString(), baris.satuan),
                AngkaIos(
                    "Diterima",
                    baris.qtyTerima?.toString() ?: "—",
                    baris.satuan,
                    negatif = baris.bermasalah,
                ),
            ),
        )

        if (baris.bermasalah) {
            Spacer(Modifier.height(10.dp))
            StripKeterangan(IkonIos.WarningAmber, pesanMasalah(baris), NadaIos.BAHAYA, tebal = true)
        }

        if (!baris.catatan.isNullOrBlank()) {
            Spacer(Modifier.height(10.dp))
            StripKeterangan(IkonIos.EditNote, baris.catatan, NadaIos.NETRAL, tebal = false)
        }

        Spacer(Modifier.height(14.dp))
        Text("Bukti foto", style = TipeIos.Catatan)
        Spacer(Modifier.height(6.dp))
        when {
            bitmap != null -> Image(
                bitmap.asImageBitmap(),
                contentDescription = "Foto bukti ${baris.nama}",
                modifier = Modifier.fillMaxWidth().height(200.dp).clip(UkuranIos.SudutBlok),
                contentScale = ContentScale.Crop,
            )
            baris.fotoPath != null -> Box(
                Modifier.fillMaxWidth().height(150.dp).clip(UkuranIos.SudutBlok).background(WarnaIos.Latar),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(IkonIos.PhotoCamera, contentDescription = null, tint = WarnaIos.Abu, modifier = Modifier.size(25.dp))
                    Spacer(Modifier.height(6.dp))
                    Text("Memuat bukti foto…", style = TipeIos.Catatan)
                }
            }
            else -> StripKeterangan(IkonIos.PhotoCamera, "Belum ada foto bukti", NadaIos.NETRAL, tebal = false)
        }
    }
}

@Composable
private fun LabelKondisi(baris: BarisItemDetail) {
    val rusak = baris.kondisi.equals("rusak", ignoreCase = true)
    val baik = baris.kondisi.equals("baik", ignoreCase = true)
    LencanaIos(
        when {
            rusak -> "Rusak"
            baik -> "Baik"
            else -> "Belum dicatat"
        },
        when {
            rusak -> NadaIos.BAHAYA
            baik -> NadaIos.SUKSES
            else -> NadaIos.NETRAL
        },
    )
}

private fun pesanMasalah(baris: BarisItemDetail): String {
    val kurang = baris.qtyTerima != null && baris.qtyTerima < baris.qtyDikirim
    return when {
        baris.kondisi.equals("rusak", ignoreCase = true) && kurang -> "Jumlah kurang dan kondisi rusak"
        baris.kondisi.equals("rusak", ignoreCase = true) -> "Kondisi barang ditandai rusak"
        kurang -> "Jumlah diterima lebih sedikit"
        else -> "Perlu perhatian pada barang ini"
    }
}

@Composable
private fun BlokTandaTangan(judul: String, daftar: List<TandaTangan>) {
    val sudahAda = daftar.isNotEmpty()
    KartuIos(padding = PaddingValues(0.dp)) {
        Row(
            Modifier.padding(UkuranIos.PaddingKartu),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IkonBulatIos(
                IkonIos.EditNote,
                (if (sudahAda) NadaIos.SUKSES else NadaIos.NETRAL).warna,
                ukuran = 38.dp,
                padat = false,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(judul, style = TipeIos.Utama)
                Text(
                    if (sudahAda) "Tanda tangan tercatat" else "Menunggu tanda tangan",
                    style = TipeIos.Catatan,
                )
            }
            Spacer(Modifier.width(8.dp))
            LencanaIos(
                if (sudahAda) "Lengkap" else "Belum ada",
                if (sudahAda) NadaIos.SUKSES else NadaIos.NETRAL,
            )
        }
        if (sudahAda) {
            daftar.forEach { ttd ->
                PemisahIos()
                Row(
                    Modifier.padding(horizontal = UkuranIos.PaddingKartu, vertical = 11.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        IkonIos.CheckCircle,
                        contentDescription = null,
                        tint = WarnaIos.Hijau,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            "${ttd.peran}: ${ttd.namaPenandaTangan}",
                            style = TipeIos.Keterangan.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium),
                        )
                        Text(formatTanggal(ttd.waktu), style = TipeIos.Catatan)
                    }
                }
            }
        }
    }
}
