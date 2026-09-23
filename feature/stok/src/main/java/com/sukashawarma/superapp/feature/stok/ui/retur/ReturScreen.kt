package com.sukashawarma.superapp.feature.stok.ui.retur

import com.sukashawarma.superapp.core.ui.ios.KartuIos
import com.sukashawarma.superapp.core.ui.ios.LencanaIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.TombolBundarIos
import com.sukashawarma.superapp.core.ui.ios.TombolUtamaIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.ios.tekanIos
import com.sukashawarma.superapp.feature.stok.ui.KapsulFilter
import androidx.compose.ui.draw.clip
import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.core.ui.kaca.denganRuangNav
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.RealtimeTables
import com.sukashawarma.superapp.feature.stok.data.model.Retur
import com.sukashawarma.superapp.feature.stok.data.model.ReturItem
import com.sukashawarma.superapp.feature.stok.domain.ReturAkses
import com.sukashawarma.superapp.feature.stok.domain.StatusRetur
import com.sukashawarma.superapp.feature.stok.domain.TabRetur
import com.sukashawarma.superapp.feature.stok.domain.formatAngkaStok
import com.sukashawarma.superapp.feature.stok.domain.formatSatuan
import com.sukashawarma.superapp.feature.stok.ui.HeaderStok
import com.sukashawarma.superapp.feature.stok.ui.KeadaanGagal
import com.sukashawarma.superapp.feature.stok.ui.KeadaanKosong
import com.sukashawarma.superapp.feature.stok.ui.MemuatPenuh
import com.sukashawarma.superapp.feature.stok.ui.PitaPesan

/// Palet retur memakai token iOS: aksen oranye sebagai identitas modul, dan nada
// status yang berbeda tiap tahap supaya kartu terbaca sekilas tanpa membaca labelnya.
internal val AmberTua = NadaIos.AKSEN.teks
internal val AmberMuda = Color(0xFFFFF4EC)
internal val AmberGaris = WarnaIos.Aksen.copy(alpha = 0.30f)
internal val BiruTua = NadaIos.INFO.teks
internal val UnguTua = NadaIos.UNGU.teks
internal val HijauTua = NadaIos.SUKSES.teks
internal val MerahTua = NadaIos.BAHAYA.teks
internal val Abu900 = WarnaIos.Label
internal val Abu500 = WarnaIos.LabelKedua
internal val Abu200 = WarnaIos.Isian
internal val LatarRetur = WarnaIos.Latar

/**
 * Retur & Refund Bahan — cermin halaman `/stok/refund` web.
 *
 * Menunya terbuka untuk semua peran, persis seperti web yang memasukkannya ke tiga
 * daftar "Lainnya" sekaligus. Yang berbeda hanya tab dan tombol aksinya; lihat
 * [ReturAkses].
 */
@Composable
fun ReturScreen(onBack: () -> Unit, viewModel: ReturViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()

    // Tiket berpindah tangan antar peran (outlet -> manajer -> kurir -> gudang),
    // jadi layar ini hampir selalu terbuka di HP orang lain saat statusnya berubah.
    RealtimeRefresh(RealtimeTables.RETUR, RealtimeTables.SURAT_JALAN) { viewModel.muatUlang() }

    if (state.lembar == LembarRetur.FORM) {
        FormReturScreen(state, viewModel)
    } else {
        DaftarRetur(state, viewModel, onBack)
    }

    when (state.lembar) {
        LembarRetur.APPROVE -> LembarKeputusanManager(state, viewModel)
        LembarRetur.KURIR -> LembarSerahKurir(state, viewModel)
        LembarRetur.KITCHEN -> LembarVerifikasiKitchen(state, viewModel)
        else -> Unit
    }

    state.fotoBesar?.let { (judul, url) -> DialogFotoPenuh(judul, url, viewModel::tutupFoto) }

    // Pesan sukses tidak boleh menetap: kalau ia masih di layar saat orang membuka
    // pengajuan kedua, ia akan mengira yang kedua pun sudah terkirim.
    LaunchedEffect(state.pesan) {
        if (state.pesan != null) {
            kotlinx.coroutines.delay(4000)
            viewModel.bersihkanPesan()
        }
    }
}

@Composable
private fun DaftarRetur(state: ReturUiState, viewModel: ReturViewModel, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(LatarRetur)) {
        HeaderStok(
            judul = "Retur & Refund Bahan",
            subjudul = "Kompensasi ganti fisik 100% · Sapi, Ayam & Kulit",
            onKembali = onBack,
        ) {
            TombolBundarIos(IkonIos.Refresh, "Muat ulang", viewModel::muatUlang)
        }

        state.pesan?.let { PitaPesan(it, false, viewModel::bersihkanPesan) }
        state.error?.let { PitaPesan(it, true, viewModel::bersihkanPesan) }

        BarisTabRetur(state, viewModel)
        TombolAjukan(state, viewModel)

        when {
            state.memuat && state.daftar.isEmpty() -> MemuatPenuh()
            state.error != null && state.daftar.isEmpty() ->
                KeadaanGagal(state.error, viewModel::muatUlang)
            else -> {
                val isi = state.isi(state.tab)
                if (isi.isEmpty()) {
                    KeadaanKosong(pesanKosong(state.tab))
                } else {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = UkuranIos.TepiLayar, end = UkuranIos.TepiLayar, top = 12.dp, bottom = 20.dp).denganRuangNav(),
                        verticalArrangement = Arrangement.spacedBy(UkuranIos.JarakKartu),
                    ) {
                        items(isi, key = { it.id }) { KartuRetur(it, state, viewModel) }
                    }
                }
            }
        }
    }
}

private fun pesanKosong(tab: TabRetur) = when (tab) {
    TabRetur.PERSETUJUAN -> "Tidak ada pengajuan retur yang menunggu persetujuan Area Manager saat ini."
    TabRetur.KITCHEN -> "Tidak ada kiriman retur yang sedang menuju Central Kitchen."
    TabRetur.RIWAYAT -> "Belum ada arsip retur yang selesai atau ditolak."
    TabRetur.AKTIF -> "Saat ini tidak ada tiket pengembalian bahan yang sedang berjalan."
}

@Composable
private fun BarisTabRetur(state: ReturUiState, viewModel: ReturViewModel) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = UkuranIos.TepiLayar, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        state.tabTerlihat.forEach { t ->
            val jumlah = state.jumlah(t)
            KapsulFilter(
                teks = t.label,
                aktif = state.tab == t,
                onKlik = { viewModel.pilihTab(t) },
                jumlah = jumlah.takeIf { it > 0 },
                titik = if (jumlah > 0) warnaLencana(t) else null,
            )
        }
    }
}

/** Antrean yang menuntut tindakan diberi titik mendesak, arsip tidak. */
private fun warnaLencana(tab: TabRetur): Color? = when (tab) {
    TabRetur.PERSETUJUAN -> WarnaIos.Merah
    TabRetur.KITCHEN -> WarnaIos.Ungu
    else -> null
}

@Composable
private fun TombolAjukan(state: ReturUiState, viewModel: ReturViewModel) {
    // Akun pusat tidak terhubung ke satu outlet pun, jadi tidak ada outlet yang bisa
    // dipotong stoknya. Web menutupnya dengan halaman penjelasan; di sini tombolnya
    // yang tidak muncul, karena tombol yang selalu gagal lebih membingungkan.
    if (state.outletSaya == null) return
    TombolUtamaIos(
        "Ajukan Retur",
        viewModel::bukaForm,
        Modifier.padding(horizontal = UkuranIos.TepiLayar).height(46.dp),
        ikon = IkonIos.Add,
    )
}

// ================================================================ kartu tiket

@Composable
private fun KartuRetur(retur: Retur, state: ReturUiState, viewModel: ReturViewModel) {
    KartuIos {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            KepalaKartu(retur)
            if (retur.status != StatusRetur.DITOLAK) StepperRetur(retur.status)
            retur.items.forEach { BarisItemRetur(it, viewModel) }
            if (!retur.driverNama.isNullOrBlank()) KartuKurir(retur, viewModel)
            if (retur.nomorSuratJalanPengganti != null) KartuSuratJalan(retur)
            if (retur.status == StatusRetur.DITERIMA_KITCHEN) BannerMenungguKirim(retur)
            if (!retur.catatanManager.isNullOrBlank()) {
                Text("Catatan AM: \"${retur.catatanManager}\"", style = TipeIos.Catatan.copy(lineHeight = 18.sp))
            }
            TombolAksi(retur, state, viewModel)
        }
    }
}

@Composable
private fun KepalaKartu(retur: Retur) {
    Row(verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(
                "${retur.nomorRetur}  ·  ${retur.outletName ?: "Outlet"}",
                style = TipeIos.Utama.copy(fontSize = 16.sp),
            )
            Text(
                "Diajukan ${tanggalSingkat(retur.createdAt)} oleh ${retur.pembuatNama ?: "Kru"}",
                style = TipeIos.Catatan,
            )
        }
        Spacer(Modifier.width(8.dp))
        LencanaIos(retur.status.label, nadaStatus(retur.status))
    }
}

internal fun nadaStatus(status: StatusRetur): NadaIos = when (status) {
    StatusRetur.DIAJUKAN, StatusRetur.DITERIMA_KITCHEN, StatusRetur.MENUNGGU_STOK -> NadaIos.PERINGATAN
    StatusRetur.DISETUJUI_MANAGER, StatusRetur.DALAM_PENGIRIMAN, StatusRetur.DIKIRIM_PENGGANTI -> NadaIos.INFO
    StatusRetur.SELESAI -> NadaIos.SUKSES
    StatusRetur.DITOLAK -> NadaIos.BAHAYA
}

internal fun warnaStatus(status: StatusRetur) = nadaStatus(status).teks

@Composable
private fun StepperRetur(status: StatusRetur) {
    val kini = status.langkah
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        StatusRetur.LANGKAH.forEachIndexed { idx, judul ->
            val lewat = kini >= idx
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(56.dp)) {
                Box(
                    Modifier.size(24.dp).clip(CircleShape).background(if (lewat) WarnaIos.Aksen else WarnaIos.Isian),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${idx + 1}",
                        color = if (lewat) Color.White else WarnaIos.LabelKedua,
                        fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    judul,
                    color = if (lewat) NadaIos.AKSEN.teks else WarnaIos.LabelKedua,
                    fontSize = 10.sp,
                    fontWeight = if (lewat) FontWeight.SemiBold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    lineHeight = 12.sp,
                    maxLines = 2,
                )
            }
        }
    }
}

@Composable
private fun BarisItemRetur(item: ReturItem, viewModel: ReturViewModel) {
    Row(
        Modifier.fillMaxWidth().clip(UkuranIos.SudutBlok).background(WarnaIos.Latar).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                "${item.namaBahan ?: "Bahan"}  ·  ${qtyTeks(item.qtyKlaim, item.satuan)}",
                style = TipeIos.Keterangan.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
            )
            Text("Alasan: ${item.alasan}", style = TipeIos.Catatan.copy(color = AmberTua, lineHeight = 17.sp))
            if (!item.catatan.isNullOrBlank()) {
                Text("\"${item.catatan}\"", style = TipeIos.Kecil)
            }
            item.qtyDiterimaKitchen?.let {
                Text(
                    "Timbang Kitchen: ${qtyTeks(it, item.satuan)}",
                    style = TipeIos.Catatan.copy(color = UnguTua, fontWeight = FontWeight.SemiBold),
                )
            }
        }
        if (!item.fotoFisikUrl.isNullOrBlank()) {
            Spacer(Modifier.width(10.dp))
            Thumbnail(
                url = item.fotoFisikUrl,
                label = "Timbangan",
                onKlik = {
                    viewModel.lihatFoto(
                        "Bukti Timbangan: ${item.namaBahan ?: "Bahan Baku"} (${qtyTeks(item.qtyKlaim, item.satuan)})",
                        item.fotoFisikUrl,
                    )
                },
            )
        }
    }
}

@Composable
internal fun Thumbnail(url: String, label: String, onKlik: () -> Unit) {
    Box(
        Modifier
            .size(58.dp)
            .clip(UkuranIos.SudutKontrol)
            .background(WarnaIos.Isian)
            .tekanIos(onKlik),
    ) {
        AsyncImage(
            model = url,
            contentDescription = label,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Text(
            label,
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(vertical = 1.dp),
            color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
        )
    }
}

/** Blok bernada tipis di dalam kartu tiket — pengganti kotak berbingkai warna. */
@Composable
private fun BlokNada(nada: NadaIos, isi: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(UkuranIos.SudutBlok)
            .background(nada.warna.copy(alpha = 0.10f))
            .padding(12.dp),
    ) { isi() }
}

@Composable
private fun KartuKurir(retur: Retur, viewModel: ReturViewModel) {
    BlokNada(NadaIos.INFO) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(32.dp).clip(CircleShape).background(WarnaIos.Biru.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(IkonIos.LocalShipping, null, tint = WarnaIos.Biru, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    buildString {
                        append(retur.jenisLogistik.nilai.uppercase())
                        append(" · ")
                        append(retur.driverNama)
                        retur.driverPlat?.takeIf { it.isNotBlank() }?.let { append(" ($it)") }
                    },
                    style = TipeIos.Catatan.copy(color = BiruTua, fontWeight = FontWeight.SemiBold),
                )
                retur.nomorResi?.takeIf { it.isNotBlank() }?.let {
                    Text("Resi: $it", style = TipeIos.Kecil.copy(color = BiruTua.copy(alpha = 0.8f)))
                }
            }
            retur.fotoSerahTerimaUrl?.takeIf { it.isNotBlank() }?.let { url ->
                Spacer(Modifier.width(8.dp))
                Thumbnail(url, "Serah") {
                    viewModel.lihatFoto("Bukti Serah Terima Kurir - ${retur.driverNama}", url)
                }
            }
        }
    }
}

@Composable
private fun KartuSuratJalan(retur: Retur) {
    BlokNada(NadaIos.SUKSES) {
        Column {
            Text("Surat jalan pengganti (resmi)", style = TipeIos.Kecil.copy(fontWeight = FontWeight.Medium))
            Text(
                retur.nomorSuratJalanPengganti.orEmpty(),
                color = HijauTua, fontSize = 16.sp, fontWeight = FontWeight.Bold,
            )
            // Web menyediakan tombol cetak PDF di sini. Native tidak menirunya:
            // dokumen fisik SJ sudah punya rumahnya sendiri di modul Distribusi,
            // lengkap dengan tanda tangan dan kode verifikasi, dan menyalin
            // pratinjaunya ke sini hanya akan menghasilkan dokumen kedua yang
            // berbeda tampilan untuk nomor yang sama.
            Text("Dokumen lengkapnya dibuka lewat modul Distribusi.", style = TipeIos.Kecil)
        }
    }
}

@Composable
private fun BannerMenungguKirim(retur: Retur) {
    BlokNada(NadaIos.UNGU) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                "Fisik Ditimbang di Kitchen — Menunggu Pengiriman Reguler",
                style = TipeIos.Catatan.copy(color = UnguTua, fontWeight = FontWeight.SemiBold, lineHeight = 18.sp),
            )
            Text(
                "Barang pengganti otomatis disertakan saat Gudang Pusat menyetujui permintaan stok reguler outlet ini di menu Permintaan.",
                style = TipeIos.Kecil.copy(color = UnguTua.copy(alpha = 0.85f), lineHeight = 16.sp),
            )
            retur.catatanKitchen?.takeIf { it.isNotBlank() }?.let {
                Text("Catatan Kitchen: \"$it\"", style = TipeIos.Kecil.copy(color = UnguTua, fontWeight = FontWeight.SemiBold))
            }
        }
    }
}

@Composable
private fun TombolAksi(retur: Retur, state: ReturUiState, viewModel: ReturViewModel) {
    val bolehSerah = ReturAkses.serahKurir(state.role, retur.outletId, state.outletSaya)
    val aksi = when {
        retur.status == StatusRetur.DIAJUKAN && state.bolehManager ->
            Triple("Review & Setujui", WarnaIos.Aksen) { viewModel.bukaApprove(retur) }

        retur.status == StatusRetur.DISETUJUI_MANAGER && bolehSerah ->
            Triple("Serahkan ke Kurir", WarnaIos.Biru) { viewModel.bukaKurir(retur) }

        retur.status == StatusRetur.DITERIMA_KITCHEN && state.bolehKitchen ->
            Triple("Terbitkan SJ Pengganti", WarnaIos.Biru) { viewModel.bukaKitchen(retur) }

        retur.status == StatusRetur.DALAM_PENGIRIMAN && state.bolehKitchen ->
            Triple("Timbang & Opsi Kirim", WarnaIos.Ungu) { viewModel.bukaKitchen(retur) }

        else -> null
    } ?: return

    TombolUtamaIos(aksi.first, aksi.third, Modifier.height(44.dp), warna = aksi.second)
}

// =================================================================== lightbox

@Composable
private fun DialogFotoPenuh(judul: String, url: String, onTutup: () -> Unit) {
    Dialog(onDismissRequest = onTutup) {
        Column(Modifier.clip(UkuranIos.SudutKartu).background(WarnaIos.Kartu)) {
            Row(
                Modifier.padding(start = 16.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    judul, Modifier.weight(1f),
                    style = TipeIos.Keterangan.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(8.dp))
                TombolBundarIos(IkonIos.Close, "Tutup", onTutup, warnaIkon = WarnaIos.LabelKedua)
            }
            SubcomposeAsyncImage(
                model = url,
                contentDescription = judul,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().height(340.dp).background(Color.Black),
                loading = {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(28.dp), color = Color.White, strokeWidth = 2.5.dp)
                    }
                },
                error = {
                    Box(Modifier.fillMaxSize().padding(24.dp), Alignment.Center) {
                        Text(
                            "Foto bukti tidak dapat dimuat.",
                            color = WarnaIos.Abu, fontSize = 13.sp, textAlign = TextAlign.Center,
                        )
                    }
                },
            )
            Text(
                "Perhatikan kondisi fisik bahan dan angka display timbangan digital dari outlet.",
                Modifier.fillMaxWidth().padding(14.dp),
                style = TipeIos.Catatan.copy(lineHeight = 18.sp),
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ================================================================== pembantu

internal fun qtyTeks(nilai: Double, satuan: String?): String =
    "${formatAngkaStok(nilai)} ${formatSatuan(satuan)}".trim()

internal fun tanggalSingkat(iso: String?): String {
    if (iso.isNullOrBlank()) return "-"
    return try {
        val waktu = java.time.OffsetDateTime.parse(iso)
            .atZoneSameInstant(java.time.ZoneId.of("Asia/Jakarta"))
        val bulan = listOf(
            "Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
            "Jul", "Agu", "Sep", "Okt", "Nov", "Des",
        )[waktu.monthValue - 1]
        "${waktu.dayOfMonth} $bulan ${waktu.year}, %02d.%02d".format(waktu.hour, waktu.minute)
    } catch (_: Exception) {
        "-"
    }
}

/**
 * Meminta izin kamera lalu membuka lembar potret. Dipakai formulir pengajuan dan
 * lembar serah terima kurir, keduanya butuh alur izin yang sama.
 */
@Composable
internal fun ingatPemintaKamera(viewModel: ReturViewModel): () -> Unit {
    val konteks = LocalContext.current
    var menunggu by remember { mutableStateOf(false) }
    val peminta = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { diberikan ->
        val diminta = menunggu
        menunggu = false
        if (diberikan && diminta) viewModel.bukaKamera()
    }
    return {
        val ada = ContextCompat.checkSelfPermission(konteks, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (ada) {
            viewModel.bukaKamera()
        } else {
            menunggu = true
            peminta.launch(Manifest.permission.CAMERA)
        }
    }
}
