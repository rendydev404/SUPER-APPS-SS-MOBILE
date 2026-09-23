package com.sukashawarma.superapp.feature.stok.ui.entri

import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.core.ui.kaca.navigationBarsPaddingKaca
import com.sukashawarma.superapp.core.ui.ios.KartuIos
import com.sukashawarma.superapp.core.ui.ios.KolomCariIos
import com.sukashawarma.superapp.core.ui.ios.LabelSeksiIos
import com.sukashawarma.superapp.core.ui.ios.LencanaIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.PemisahIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.TombolKeduaIos
import com.sukashawarma.superapp.core.ui.ios.TombolUtamaIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.ios.tekanIos
import com.sukashawarma.superapp.core.ui.ios.warnaKolomIos
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import com.sukashawarma.superapp.core.ui.SukaDropdownHeader
import com.sukashawarma.superapp.core.ui.SukaDropdownMenu
import com.sukashawarma.superapp.core.ui.SukaDropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.sukashawarma.superapp.core.camera.KameraFotoSheet
import com.sukashawarma.superapp.feature.stok.data.EntriManualRepository
import com.sukashawarma.superapp.feature.stok.data.model.MonitoringRow
import com.sukashawarma.superapp.feature.stok.domain.UnitScale
import com.sukashawarma.superapp.feature.stok.ui.BannerIos
import com.sukashawarma.superapp.feature.stok.ui.HeaderStok
import com.sukashawarma.superapp.feature.stok.ui.KapsulFilter

/** Ikon dan warna tiap jenis entri, dipisah dari enum supaya domain bebas Compose. */
private val JenisEntri.ikon: ImageVector
    get() = when (this) {
        JenisEntri.PENYESUAIAN -> IkonIos.Tune
        JenisEntri.WASTE -> IkonIos.DeleteSweep
        JenisEntri.TRANSFER_KELUAR -> IkonIos.LocalShipping
    }

private val JenisEntri.penjelasan: String
    get() = when (this) {
        JenisEntri.PENYESUAIAN -> "Mengoreksi saldo yang salah, tanpa menunggu opname."
        JenisEntri.WASTE -> "Melaporkan bahan terbuang, rusak, atau kedaluwarsa."
        JenisEntri.TRANSFER_KELUAR -> "Mencatat bahan yang dikirim keluar dari outlet ini."
    }

/** Kalimat konsekuensi — ini yang paling menentukan pilihan, jadi ditonjolkan. */
private val JenisEntri.akibat: String
    get() = when (this) {
        JenisEntri.WASTE -> "Perlu disetujui dulu"
        else -> "Stok langsung berubah"
    }

private val JenisEntri.nadaAkibat: NadaIos
    get() = if (this == JenisEntri.WASTE) NadaIos.PERINGATAN else NadaIos.BAHAYA

/**
 * Entri manual stok — cermin `ManualEntryForm.tsx` web.
 *
 * Layar ini menulis langsung ke saldo, jadi susunannya dibuat berurutan dan tiap
 * langkah bernomor: orang harus tahu persis apa yang akan terjadi sebelum menekan
 * kirim. Dua hal yang paling menentukan sengaja tidak disembunyikan sebagai teks
 * kecil — akibat tiap jenis entri (langsung memotong vs menunggu persetujuan) dan
 * pratinjau saldo sesudah entri.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntriManualScreen(
    onBack: () -> Unit,
    viewModel: EntriManualViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val konteks = LocalContext.current
    val lembarKamera = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var menungguIzin by remember { mutableStateOf(false) }
    val pemintaIzin = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { diberikan ->
        val diminta = menungguIzin
        menungguIzin = false
        if (diberikan && diminta) viewModel.bukaKamera()
    }

    fun mintaFoto() {
        val ada = ContextCompat.checkSelfPermission(konteks, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (ada) {
            viewModel.bukaKamera()
        } else {
            menungguIzin = true
            pemintaIzin.launch(Manifest.permission.CAMERA)
        }
    }

    // Outlet hanya jadi langkah tersendiri kalau memang ada yang bisa dipilih.
    val adaPilihanOutlet = state.outlets.size > 1
    var no = 0
    val noOutlet = if (adaPilihanOutlet) ++no else 0
    val noJenis = ++no
    val noBahan = ++no
    val noJumlah = ++no
    val noRincian = ++no

    Column(Modifier.fillMaxSize().background(WarnaIos.Latar)) {
        HeaderStok(
            judul = "Entri Manual Stok",
            subjudul = "Penyesuaian, waste & transfer keluar",
            onKembali = onBack,
        )

        // Banner ditaruh di atas, bukan di ujung daftar: pesan sukses yang muncul
        // di bawah layar sering tidak pernah terlihat sama sekali.
        val pesan = state.pesan ?: state.error
        if (pesan != null) {
            Banner(teks = pesan, sukses = state.pesan != null)
        }

        if (state.memuat) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(30.dp), color = WarnaIos.Abu, strokeWidth = 2.5.dp)
            }
        } else {
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(start = UkuranIos.TepiLayar, end = UkuranIos.TepiLayar, top = 12.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(UkuranIos.JarakKartu),
            ) {
                if (adaPilihanOutlet) {
                    item { PemilihOutlet(noOutlet, state, viewModel) }
                }
                item { PemilihJenis(noJenis, state, viewModel) }
                item { PemilihBahan(noBahan, state, viewModel) }

                if (state.bahanTerpilih != null) {
                    item { KolomJumlah(noJumlah, state, viewModel) }
                    item { PratinjauDampak(state) }

                    if (state.butuhAlasan || state.butuhFoto || state.jenis != JenisEntri.WASTE) {
                        item { KartuRincian(noRincian, state, viewModel, ::mintaFoto) }
                    }
                }
            }

            // Tombol kirim tidak ikut ter-scroll. Di form sepanjang ini, tombol yang
            // harus dicari dulu membuat orang mengira formnya belum selesai.
            BilahKirim(state, viewModel)
        }
    }

    if (state.kameraTerbuka) {
        ModalBottomSheet(
            onDismissRequest = viewModel::tutupKamera,
            sheetState = lembarKamera,
            containerColor = WarnaIos.Kartu,
        ) {
            Column(Modifier.padding(horizontal = UkuranIos.TepiLayar).padding(bottom = 24.dp)) {
                Text("Foto bukti waste", style = TipeIos.Utama)
                Spacer(Modifier.height(10.dp))
                KameraFotoSheet(
                    // cacheDir dikirim dari layar: ViewModel ini ViewModel biasa, bukan
                    // AndroidViewModel, jadi tidak punya Context sendiri.
                    onDiambil = { bitmap -> viewModel.simpanFoto(bitmap, konteks.cacheDir) },
                    onBatal = viewModel::tutupKamera,
                )
            }
        }
    }

    // Pesan sukses tidak boleh menetap sampai entri berikutnya — itu membuat
    // pengguna mengira entri kedua sudah tersimpan padahal belum ditekan.
    LaunchedEffect(state.pesan) {
        if (state.pesan != null) {
            kotlinx.coroutines.delay(4000)
            viewModel.bersihkanPesan()
        }
    }
}

// ------------------------------------------------------------------ potongan umum

@Composable
private fun Banner(teks: String, sukses: Boolean) {
    BannerIos(
        teks,
        if (sukses) NadaIos.SUKSES else NadaIos.BAHAYA,
        Modifier.padding(horizontal = UkuranIos.TepiLayar, vertical = 10.dp),
        ikon = if (sukses) IkonIos.CheckCircle else IkonIos.ErrorOutline,
    )
}

/**
 * Kartu satu langkah. Nomor dan centang di judul membuat alurnya terbaca sekali
 * lihat: mana yang sudah beres, mana yang masih menunggu diisi.
 */
@Composable
private fun KartuLangkah(
    nomor: Int,
    judul: String,
    selesai: Boolean,
    isi: @Composable () -> Unit,
) {
    KartuIos {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(24.dp).clip(CircleShape).background(if (selesai) WarnaIos.Hijau else WarnaIos.Abu),
                contentAlignment = Alignment.Center,
            ) {
                if (selesai) {
                    Icon(IkonIos.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                } else {
                    Text("$nomor", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(10.dp))
            Text(judul, style = TipeIos.Utama)
        }
        Spacer(Modifier.height(12.dp))
        isi()
    }
}

/** Satu baris pilihan di dalam daftar abu bersekat — pola daftar pilihan iOS. */
@Composable
private fun BarisPilihan(
    teks: String,
    aktif: Boolean,
    onKlik: () -> Unit,
    nilai: String? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onKlik)
            .heightIn(min = 44.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            teks,
            Modifier.weight(1f),
            style = TipeIos.Keterangan.copy(
                fontSize = 15.sp,
                color = if (aktif) WarnaIos.Aksen else WarnaIos.Label,
                fontWeight = if (aktif) FontWeight.SemiBold else FontWeight.Normal,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (nilai != null) {
            Spacer(Modifier.width(8.dp))
            Text(nilai, style = TipeIos.Catatan)
        }
        if (aktif) {
            Spacer(Modifier.width(8.dp))
            Icon(IkonIos.Check, null, tint = WarnaIos.Aksen, modifier = Modifier.size(17.dp))
        }
    }
}

// ------------------------------------------------------------------ langkah-langkah

@Composable
private fun PemilihOutlet(nomor: Int, state: EntriManualUiState, viewModel: EntriManualViewModel) {
    var terbuka by remember { mutableStateOf(false) }
    KartuLangkah(nomor, "Outlet", state.outletTerpilih != null) {
        Box {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(UkuranIos.TinggiKontrol + 4.dp)
                    .clip(UkuranIos.SudutKontrol)
                    .background(WarnaIos.Isian)
                    .tekanIos({ terbuka = true })
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(IkonIos.Storefront, null, tint = WarnaIos.Aksen, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    state.outletTerpilih?.name ?: "Pilih outlet",
                    Modifier.weight(1f),
                    style = TipeIos.Keterangan.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(IkonIos.ArrowDropDown, null, tint = WarnaIos.Aksen, modifier = Modifier.size(16.dp))
            }
            SukaDropdownMenu(terbuka, { terbuka = false }) {
                SukaDropdownHeader(title = "PILIH OUTLET", onClose = { terbuka = false })
                state.outlets.forEach { outlet ->
                    SukaDropdownMenuItem(
                        text = outlet.name,
                        selected = (state.outletTerpilih?.id == outlet.id),
                        onClick = { terbuka = false; viewModel.pilihOutlet(outlet) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PemilihJenis(nomor: Int, state: EntriManualUiState, viewModel: EntriManualViewModel) {
    KartuLangkah(nomor, "Mau mencatat apa?", true) {
        // Satu baris penuh per jenis, bukan tiga chip sempit: label "Transfer keluar"
        // terpotong di layar kecil, dan akibat tiap pilihan tidak muat ditulis.
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            JenisEntri.entries.forEach { jenis ->
                val aktif = jenis == state.jenis
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(UkuranIos.SudutBlok)
                        .background(if (aktif) WarnaIos.Aksen.copy(alpha = 0.10f) else WarnaIos.Latar)
                        .tekanIos({ viewModel.pilihJenis(jenis) }, skalaTekan = 0.98f)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(34.dp).clip(CircleShape).background(if (aktif) WarnaIos.Aksen else WarnaIos.Isian),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            jenis.ikon,
                            null,
                            tint = if (aktif) Color.White else WarnaIos.LabelKedua,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            jenis.label,
                            style = TipeIos.Keterangan.copy(fontWeight = FontWeight.SemiBold),
                        )
                        Spacer(Modifier.height(1.dp))
                        Text(jenis.penjelasan, style = TipeIos.Catatan.copy(lineHeight = 17.sp))
                        Spacer(Modifier.height(6.dp))
                        LencanaIos(jenis.akibat, jenis.nadaAkibat, titik = false)
                    }
                    if (aktif) {
                        Spacer(Modifier.width(8.dp))
                        Icon(IkonIos.CheckCircle, "Terpilih", tint = WarnaIos.Aksen, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PemilihBahan(nomor: Int, state: EntriManualUiState, viewModel: EntriManualViewModel) {
    val terpilih = state.bahanTerpilih
    KartuLangkah(nomor, "Bahan baku", terpilih != null) {
        if (terpilih != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(UkuranIos.SudutBlok)
                    .background(WarnaIos.Aksen.copy(alpha = 0.10f))
                    .tekanIos({ viewModel.pilihBahan(null) }, skalaTekan = 0.98f)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        terpilih.itemName,
                        style = TipeIos.Keterangan.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text("Saldo sekarang ${terpilih.saldoRingkas()}", style = TipeIos.Catatan)
                }
                Text("Ganti", color = WarnaIos.Aksen, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        } else {
            KolomCariIos(
                nilai = state.cari,
                onUbah = viewModel::ubahCari,
                modifier = Modifier.fillMaxWidth(),
                placeholder = "Ketik nama bahan…",
            )
            Spacer(Modifier.height(10.dp))
            // Semua bahan ditampilkan, bukan dipotong 12 lalu diminta mengetik:
            // pengguna mengira daftarnya memang sedikit dan bahan yang dicari tidak ada.
            val hasil = state.bahanTampil
            if (hasil.isEmpty()) {
                Text("Tidak ada bahan yang cocok.", style = TipeIos.Catatan)
            } else {
                Column(Modifier.fillMaxWidth().clip(UkuranIos.SudutBlok).background(WarnaIos.Latar)) {
                    hasil.forEachIndexed { i, row ->
                        if (i > 0) PemisahIos(inset = 12.dp)
                        BarisPilihBahan(row) { viewModel.pilihBahan(row) }
                    }
                }
            }
        }
    }
}

@Composable
private fun BarisPilihBahan(row: MonitoringRow, onKlik: () -> Unit) {
    BarisPilihan(row.itemName, aktif = false, onKlik = onKlik, nilai = row.saldoRingkas())
}

@Composable
private fun KolomJumlah(nomor: Int, state: EntriManualUiState, viewModel: EntriManualViewModel) {
    val meta = state.bahanTerpilih?.meta
    val terisi = (state.jumlahAngka ?: 0.0) > 0.0
    KartuLangkah(nomor, "Berapa banyak?", terisi) {
        if (state.jenis == JenisEntri.PENYESUAIAN) {
            // Tanpa pilihan arah, penyesuaian hanya bisa menambah stok — dan
            // pengguna tidak punya cara tahu itu. Lihat `adjDirection` di web.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ArahPenyesuaian.entries.forEach { arah ->
                    val aktif = arah == state.arah
                    val warna = if (arah == ArahPenyesuaian.MASUK) WarnaIos.Hijau else WarnaIos.Merah
                    Box(
                        Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp)
                            .clip(UkuranIos.SudutKontrol)
                            .background(if (aktif) warna else WarnaIos.Isian)
                            .tekanIos({ viewModel.pilihArah(arah) })
                            .padding(vertical = 8.dp, horizontal = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "${arah.tanda} ${arah.label}",
                            color = if (aktif) Color.White else WarnaIos.Label,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            lineHeight = 17.sp,
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // Satuan yang faktornya tidak ada tidak ditawarkan: menawarkannya berarti
        // membiarkan pengguna memilih konversi yang tidak bisa dihitung.
        val pilihan = buildList {
            add(SatuanInput.BESAR to (meta?.satuan ?: "satuan"))
            if (meta?.satuanTengah != null && meta.faktorTengah != null) {
                add(SatuanInput.TENGAH to meta.satuanTengah!!)
            }
            if (meta?.satuanKecil != null && meta.faktorTampilan != null) {
                add(SatuanInput.KECIL to meta.satuanKecil!!)
            }
        }
        if (pilihan.size > 1) {
            Text("Satuan yang dipakai mengetik", style = TipeIos.Catatan)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pilihan.forEach { (satuan, label) ->
                    KapsulFilter(label, satuan == state.satuanInput, { viewModel.pilihSatuanInput(satuan) })
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = state.jumlah,
            onValueChange = viewModel::ubahJumlah,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("0", fontSize = 17.sp) },
            suffix = { Text(state.labelSatuan, color = WarnaIos.LabelKedua, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            textStyle = TipeIos.Isi,
            shape = UkuranIos.SudutKontrol,
            colors = warnaKolomIos(),
        )
        val besar = state.jumlahBesar
        if (besar != null && state.satuanInput != SatuanInput.BESAR) {
            Spacer(Modifier.height(8.dp))
            Text("Sama dengan ${angkaRapi(besar)} ${meta?.satuan.orEmpty()}", style = TipeIos.Catatan)
        }
    }
}

/**
 * Pratinjau saldo sesudah entri — cermin teks "-> Target:" di web.
 *
 * Ini pengaman terpenting di layar ini: salah satuan atau salah arah baru
 * kelihatan jelas ketika angka sesudahnya ditampilkan, bukan dari angka yang
 * baru saja diketik.
 */
@Composable
private fun PratinjauDampak(state: EntriManualUiState) {
    val row = state.bahanTerpilih ?: return
    val sesudah = state.saldoSesudahNorm
    val sebelum = row.saldoNorm

    val warna = when {
        sesudah == null -> WarnaIos.Abu
        state.saldoJadiMinus -> NadaIos.BAHAYA.teks
        (state.deltaNorm ?: 0.0) > 0 -> NadaIos.SUKSES.teks
        else -> NadaIos.AKSEN.teks
    }

    KartuIos {
        LabelSeksiIos("Setelah disimpan")
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(UkuranIos.SudutBlok)
                .background(if (state.saldoJadiMinus) WarnaIos.Merah.copy(alpha = 0.10f) else WarnaIos.Latar)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Sekarang", style = TipeIos.Kecil.copy(fontWeight = FontWeight.Medium))
                Spacer(Modifier.height(2.dp))
                Text(
                    row.saldoRingkas(),
                    color = WarnaIos.LabelKedua,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(IkonIos.ArrowForward, null, tint = WarnaIos.Abu, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Menjadi", style = TipeIos.Kecil.copy(fontWeight = FontWeight.Medium))
                Spacer(Modifier.height(2.dp))
                Text(
                    // Angka belum lengkap tampil "—", bukan angka lama: menampilkan
                    // saldo sekarang di kolom "menjadi" akan terbaca seperti hasil.
                    if (sesudah == null || sebelum == null) "—"
                    else UnitScale.formatBerjenjang(sesudah, row.meta) ?: angkaRapi(sesudah),
                    style = TipeIos.Angka.copy(fontSize = 18.sp, color = warna),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (state.saldoJadiMinus) {
            Spacer(Modifier.height(10.dp))
            BannerIos(
                "Saldo akan jadi minus. Periksa lagi satuan dan arahnya sebelum menyimpan.",
                NadaIos.BAHAYA,
                ikon = IkonIos.WarningAmber,
            )
        } else if (state.jenis == JenisEntri.WASTE) {
            Spacer(Modifier.height(10.dp))
            BannerIos(
                "Saldo baru berkurang setelah laporan ini disetujui.",
                NadaIos.PERINGATAN,
                ikon = IkonIos.HourglassEmpty,
            )
        }
    }
}

@Composable
private fun KartuRincian(
    nomor: Int,
    state: EntriManualUiState,
    viewModel: EntriManualViewModel,
    onMintaFoto: () -> Unit,
) {
    val alasanBeres = !state.butuhAlasan || state.alasan.isNotBlank()
    val fotoBeres = !state.butuhFoto || state.fotoBukti != null
    KartuLangkah(nomor, if (state.jenis == JenisEntri.WASTE) "Alasan & bukti" else "Alasan", alasanBeres && fotoBeres) {
        if (state.butuhAlasan) {
            if (state.jenis == JenisEntri.WASTE) {
                Column(Modifier.fillMaxWidth().clip(UkuranIos.SudutBlok).background(WarnaIos.Latar)) {
                    EntriManualRepository.ALASAN_WASTE.forEachIndexed { i, alasan ->
                        if (i > 0) PemisahIos(inset = 12.dp)
                        BarisPilihan(alasan, alasan == state.alasan, { viewModel.ubahAlasan(alasan) })
                    }
                }
            } else {
                OutlinedTextField(
                    value = state.alasan,
                    onValueChange = viewModel::ubahAlasan,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text("Contoh: koreksi salah input opname", fontSize = 15.sp)
                    },
                    textStyle = TipeIos.Keterangan,
                    shape = UkuranIos.SudutKontrol,
                    colors = warnaKolomIos(),
                )
            }
        }

        if (state.jenis != JenisEntri.WASTE) {
            Spacer(Modifier.height(12.dp))
            Text("Keterangan tambahan (opsional)", style = TipeIos.Catatan)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = state.catatan,
                onValueChange = viewModel::ubahCatatan,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Boleh dikosongkan", fontSize = 15.sp) },
                textStyle = TipeIos.Keterangan,
                shape = UkuranIos.SudutKontrol,
                colors = warnaKolomIos(),
            )
        }

        if (state.butuhFoto) {
            Spacer(Modifier.height(12.dp))
            BagianFoto(state, onMintaFoto)
        }
    }
}

@Composable
private fun BagianFoto(state: EntriManualUiState, onMintaFoto: () -> Unit) {
    // Berkas lokal, bukan URL: fotonya baru diunggah saat laporan dikirim. Coil bisa
    // menampilkan File apa adanya, jadi pratinjaunya tetap sama seperti sebelumnya.
    val url = state.fotoBukti
    Text("Foto bukti (wajib)", style = TipeIos.Catatan)
    Spacer(Modifier.height(8.dp))
    Box(
        Modifier.fillMaxWidth().height(170.dp).clip(UkuranIos.SudutBlok).background(WarnaIos.Latar),
        contentAlignment = Alignment.Center,
    ) {
        when {
            state.mengunggahFoto -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.5.dp, color = WarnaIos.Abu)
                Spacer(Modifier.height(8.dp))
                Text("Menyimpan…", style = TipeIos.Catatan)
            }
            url != null -> AsyncImage(
                model = url,
                contentDescription = "Foto bukti waste",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(IkonIos.PhotoCamera, null, tint = WarnaIos.Abu, modifier = Modifier.size(28.dp))
                Spacer(Modifier.height(7.dp))
                Text("Belum ada foto", style = TipeIos.Catatan)
            }
        }
    }
    Spacer(Modifier.height(10.dp))
    TombolKeduaIos(
        if (url != null) "Foto tersimpan — ambil ulang" else "Ambil foto sekarang",
        onMintaFoto,
        Modifier.height(44.dp),
        aktif = !state.mengunggahFoto,
        ikon = if (url != null) IkonIos.CheckCircle else IkonIos.PhotoCamera,
        warna = if (url != null) WarnaIos.Hijau else WarnaIos.Aksen,
    )
}

/**
 * Bilah kirim yang menempel di bawah.
 *
 * Alasan tombol belum bisa ditekan ditulis DI ATAS tombol, bukan di bawahnya:
 * pengguna menekan tombol mati lalu mencari penjelasannya, dan penjelasan yang
 * berada di bawah lipatan layar tidak pernah terbaca.
 */
@Composable
private fun BilahKirim(state: EntriManualUiState, viewModel: EntriManualViewModel) {
    Column(Modifier.fillMaxWidth().background(WarnaIos.Kartu)) {
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(WarnaIos.Pemisah))
        Column(Modifier.navigationBarsPaddingKaca().padding(horizontal = UkuranIos.TepiLayar, vertical = 12.dp)) {
            val halangan = state.halangan
            if (halangan != null) {
                Row(Modifier.padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(IkonIos.ErrorOutline, null, tint = WarnaIos.Oranye, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(halangan, style = TipeIos.Catatan.copy(color = NadaIos.PERINGATAN.teks, fontWeight = FontWeight.Medium))
                }
            }
            TombolUtamaIos(
                when {
                    state.menyimpan -> "Menyimpan…"
                    state.jenis == JenisEntri.WASTE -> "Kirim laporan waste"
                    else -> "Simpan ${state.jenis.label.lowercase()}"
                },
                viewModel::kirim,
                aktif = halangan == null && !state.menyimpan && !state.mengunggahFoto,
            )
        }
    }
}

/** Angka tanpa ekor desimal palsu, maksimal empat digit di belakang koma. */
private fun angkaRapi(nilai: Double): String =
    String.format(java.util.Locale.US, "%.4f", nilai).trimEnd('0').trimEnd('.')
