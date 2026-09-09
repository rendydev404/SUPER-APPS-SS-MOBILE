package com.sukashawarma.superapp.feature.stok.ui.entri

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import com.sukashawarma.superapp.feature.stok.ui.HeaderStok

private val ORANGE = Color(0xFFEA580C)
private val SLATE400 = Color(0xFF94A3B8)
private val SLATE500 = Color(0xFF64748B)
private val SLATE900 = Color(0xFF0F172A)
private val GARIS = Color(0xFFE2E8F0)
private val HIJAU = Color(0xFF15803D)
private val HIJAU_LATAR = Color(0xFFECFDF5)
private val MERAH = Color(0xFFB91C1C)
private val MERAH_LATAR = Color(0xFFFEF2F2)
private val AMBER = Color(0xFFB45309)
private val AMBER_LATAR = Color(0xFFFFFBEB)
private val LATAR = Color(0xFFF8FAFC)

/** Ikon dan warna tiap jenis entri, dipisah dari enum supaya domain bebas Compose. */
private val JenisEntri.ikon: ImageVector
    get() = when (this) {
        JenisEntri.PENYESUAIAN -> Icons.Default.Tune
        JenisEntri.WASTE -> Icons.Default.DeleteSweep
        JenisEntri.TRANSFER_KELUAR -> Icons.Default.LocalShipping
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

private val JenisEntri.warnaAkibat: Color
    get() = if (this == JenisEntri.WASTE) AMBER else MERAH

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

    Column(Modifier.fillMaxSize().background(LATAR)) {
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
                CircularProgressIndicator(color = ORANGE)
            }
        } else {
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
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
            containerColor = Color.White,
        ) {
            Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
                Text("Foto bukti waste", color = SLATE900, fontSize = 14.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(10.dp))
                KameraFotoSheet(
                    onDiambil = viewModel::simpanFoto,
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
    Surface(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (sukses) HIJAU_LATAR else MERAH_LATAR,
        border = BorderStroke(1.dp, if (sukses) HIJAU.copy(alpha = 0.35f) else MERAH.copy(alpha = 0.35f)),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (sukses) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                null,
                tint = if (sukses) HIJAU else MERAH,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(9.dp))
            Text(
                teks,
                color = if (sukses) HIJAU else MERAH,
                fontSize = 12.5.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
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
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, if (selesai) HIJAU.copy(alpha = 0.30f) else GARIS),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(20.dp).background(if (selesai) HIJAU else SLATE400, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (selesai) {
                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(13.dp))
                    } else {
                        Text("$nomor", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
                Spacer(Modifier.width(9.dp))
                Text(
                    judul.uppercase(),
                    color = if (selesai) HIJAU else SLATE500,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.7.sp,
                )
            }
            Spacer(Modifier.height(11.dp))
            isi()
        }
    }
}

// ------------------------------------------------------------------ langkah-langkah

@Composable
private fun PemilihOutlet(nomor: Int, state: EntriManualUiState, viewModel: EntriManualViewModel) {
    var terbuka by remember { mutableStateOf(false) }
    KartuLangkah(nomor, "Outlet", state.outletTerpilih != null) {
        Box {
            Surface(
                Modifier.fillMaxWidth().clickable { terbuka = true },
                shape = RoundedCornerShape(12.dp),
                color = LATAR,
                border = BorderStroke(1.dp, GARIS),
            ) {
                Row(
                    Modifier.padding(horizontal = 13.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        state.outletTerpilih?.name ?: "Pilih outlet",
                        Modifier.weight(1f),
                        color = SLATE900,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Icon(Icons.Default.ArrowDropDown, null, tint = SLATE500)
                }
            }
            DropdownMenu(terbuka, { terbuka = false }) {
                state.outlets.forEach { outlet ->
                    DropdownMenuItem(
                        text = { Text(outlet.name, fontSize = 13.sp) },
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
        // Satu kartu penuh per jenis, bukan tiga chip sempit: label "Transfer keluar"
        // terpotong di layar kecil, dan akibat tiap pilihan tidak muat ditulis.
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            JenisEntri.entries.forEach { jenis ->
                val aktif = jenis == state.jenis
                Surface(
                    Modifier.fillMaxWidth().clickable { viewModel.pilihJenis(jenis) },
                    shape = RoundedCornerShape(13.dp),
                    color = if (aktif) Color(0xFFFFF7ED) else LATAR,
                    border = BorderStroke(if (aktif) 1.5.dp else 1.dp, if (aktif) ORANGE else GARIS),
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            jenis.ikon,
                            null,
                            tint = if (aktif) ORANGE else SLATE400,
                            modifier = Modifier.size(21.dp),
                        )
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                jenis.label,
                                color = if (aktif) SLATE900 else SLATE500,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(jenis.penjelasan, color = SLATE500, fontSize = 11.sp, lineHeight = 15.sp)
                        }
                        Spacer(Modifier.width(8.dp))
                        Pil(jenis.akibat, jenis.warnaAkibat)
                    }
                }
            }
        }
    }
}

@Composable
private fun Pil(teks: String, warna: Color) {
    Surface(shape = RoundedCornerShape(50), color = warna.copy(alpha = 0.11f)) {
        Text(
            teks,
            Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = warna,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 12.sp,
        )
    }
}

@Composable
private fun PemilihBahan(nomor: Int, state: EntriManualUiState, viewModel: EntriManualViewModel) {
    val terpilih = state.bahanTerpilih
    KartuLangkah(nomor, "Bahan baku", terpilih != null) {
        if (terpilih != null) {
            Surface(
                Modifier.fillMaxWidth().clickable { viewModel.pilihBahan(null) },
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFFF7ED),
                border = BorderStroke(1.dp, Color(0xFFFED7AA)),
            ) {
                Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            terpilih.itemName,
                            color = SLATE900,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Saldo sekarang ${terpilih.saldoRingkas()}",
                            color = SLATE500,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    Text("GANTI", color = ORANGE, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
        } else {
            OutlinedTextField(
                value = state.cari,
                onValueChange = viewModel::ubahCari,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Ketik nama bahan…", fontSize = 12.5.sp, color = SLATE400) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = SLATE400, modifier = Modifier.size(18.dp)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = ORANGE,
                    unfocusedBorderColor = GARIS,
                ),
            )
            Spacer(Modifier.height(9.dp))
            // Semua bahan ditampilkan, bukan dipotong 12 lalu diminta mengetik:
            // pengguna mengira daftarnya memang sedikit dan bahan yang dicari tidak ada.
            val hasil = state.bahanTampil
            if (hasil.isEmpty()) {
                Text("Tidak ada bahan yang cocok.", color = SLATE400, fontSize = 12.sp)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    hasil.forEach { row -> BarisPilihBahan(row) { viewModel.pilihBahan(row) } }
                }
            }
        }
    }
}

@Composable
private fun BarisPilihBahan(row: MonitoringRow, onKlik: () -> Unit) {
    Surface(
        Modifier.fillMaxWidth().clickable(onClick = onKlik),
        shape = RoundedCornerShape(10.dp),
        color = LATAR,
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                row.itemName,
                Modifier.weight(1f),
                color = SLATE900,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(8.dp))
            Text(row.saldoRingkas(), color = SLATE500, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
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
                    val warna = if (arah == ArahPenyesuaian.MASUK) HIJAU else MERAH
                    Surface(
                        Modifier.weight(1f).clickable { viewModel.pilihArah(arah) },
                        shape = RoundedCornerShape(11.dp),
                        color = if (aktif) warna else LATAR,
                        border = BorderStroke(1.dp, if (aktif) warna else GARIS),
                    ) {
                        Text(
                            "${arah.tanda} ${arah.label}",
                            Modifier.padding(vertical = 10.dp, horizontal = 6.dp).fillMaxWidth(),
                            color = if (aktif) Color.White else SLATE500,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            maxLines = 2,
                            lineHeight = 14.sp,
                        )
                    }
                }
            }
            Spacer(Modifier.height(11.dp))
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
            Text("Satuan yang dipakai mengetik", color = SLATE400, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(7.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                pilihan.forEach { (satuan, label) ->
                    val aktif = satuan == state.satuanInput
                    Surface(
                        Modifier.clickable { viewModel.pilihSatuanInput(satuan) },
                        shape = RoundedCornerShape(50),
                        color = if (aktif) Color(0xFFFFF7ED) else LATAR,
                        border = BorderStroke(1.dp, if (aktif) ORANGE else GARIS),
                    ) {
                        Text(
                            label,
                            Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
                            color = if (aktif) ORANGE else SLATE500,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        OutlinedTextField(
            value = state.jumlah,
            onValueChange = viewModel::ubahJumlah,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("0", fontSize = 15.sp, color = SLATE400) },
            suffix = { Text(state.labelSatuan, color = SLATE500, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = ORANGE,
                unfocusedBorderColor = GARIS,
            ),
        )
        val besar = state.jumlahBesar
        if (besar != null && state.satuanInput != SatuanInput.BESAR) {
            Spacer(Modifier.height(7.dp))
            Text(
                "Sama dengan ${angkaRapi(besar)} ${meta?.satuan.orEmpty()}",
                color = SLATE500,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
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
        sesudah == null -> SLATE400
        state.saldoJadiMinus -> MERAH
        (state.deltaNorm ?: 0.0) > 0 -> HIJAU
        else -> ORANGE
    }

    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (state.saldoJadiMinus) MERAH_LATAR else Color.White,
        border = BorderStroke(1.dp, if (state.saldoJadiMinus) MERAH.copy(alpha = 0.35f) else GARIS),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                "SETELAH DISIMPAN",
                color = SLATE400,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.7.sp,
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Sekarang", color = SLATE400, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        row.saldoRingkas(),
                        color = SLATE500,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(Icons.Default.ArrowForward, null, tint = SLATE400, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Menjadi", color = SLATE400, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        // Angka belum lengkap tampil "—", bukan angka lama: menampilkan
                        // saldo sekarang di kolom "menjadi" akan terbaca seperti hasil.
                        if (sesudah == null || sebelum == null) "—"
                        else UnitScale.formatBerjenjang(sesudah, row.meta) ?: angkaRapi(sesudah),
                        color = warna,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (state.saldoJadiMinus) {
                Spacer(Modifier.height(11.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.WarningAmber, null, tint = MERAH, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Saldo akan jadi minus. Periksa lagi satuan dan arahnya sebelum menyimpan.",
                        color = MERAH,
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            } else if (state.jenis == JenisEntri.WASTE) {
                Spacer(Modifier.height(9.dp))
                Text(
                    "Saldo baru berkurang setelah laporan ini disetujui.",
                    color = AMBER,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
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
    val fotoBeres = !state.butuhFoto || state.fotoUrl != null
    KartuLangkah(nomor, if (state.jenis == JenisEntri.WASTE) "Alasan & bukti" else "Alasan", alasanBeres && fotoBeres) {
        if (state.butuhAlasan) {
            if (state.jenis == JenisEntri.WASTE) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    EntriManualRepository.ALASAN_WASTE.forEach { alasan ->
                        val aktif = alasan == state.alasan
                        Surface(
                            Modifier.fillMaxWidth().clickable { viewModel.ubahAlasan(alasan) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (aktif) Color(0xFFFFF7ED) else LATAR,
                            border = BorderStroke(1.dp, if (aktif) ORANGE else GARIS),
                        ) {
                            Text(
                                alasan,
                                Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                                color = if (aktif) ORANGE else SLATE500,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    value = state.alasan,
                    onValueChange = viewModel::ubahAlasan,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text("Contoh: koreksi salah input opname", fontSize = 12.sp, color = SLATE400)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = ORANGE,
                        unfocusedBorderColor = GARIS,
                    ),
                )
            }
        }

        if (state.jenis != JenisEntri.WASTE) {
            Spacer(Modifier.height(10.dp))
            Text("Keterangan tambahan (opsional)", color = SLATE400, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = state.catatan,
                onValueChange = viewModel::ubahCatatan,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Boleh dikosongkan", fontSize = 12.sp, color = SLATE400) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = ORANGE,
                    unfocusedBorderColor = GARIS,
                ),
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
    val url = state.fotoUrl
    Text("Foto bukti (wajib)", color = SLATE400, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(7.dp))
    Box(
        Modifier.fillMaxWidth().height(170.dp).background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        when {
            state.mengunggahFoto -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 3.dp, color = ORANGE)
                Spacer(Modifier.height(8.dp))
                Text("Mengunggah…", color = SLATE500, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            url != null -> AsyncImage(
                model = url,
                contentDescription = "Foto bukti waste",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
            )
            else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.PhotoCamera, null, tint = SLATE400, modifier = Modifier.size(26.dp))
                Spacer(Modifier.height(7.dp))
                Text("Belum ada foto", color = SLATE400, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
    Spacer(Modifier.height(10.dp))
    OutlinedButton(
        onClick = onMintaFoto,
        enabled = !state.mengunggahFoto,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
    ) {
        Icon(
            if (url != null) Icons.Default.CheckCircle else Icons.Default.PhotoCamera,
            null,
            tint = if (url != null) HIJAU else ORANGE,
            modifier = Modifier.size(17.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            if (url != null) "Foto tersimpan — ambil ulang" else "Ambil foto sekarang",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (url != null) HIJAU else SLATE900,
        )
    }
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
    Surface(color = Color.White, shadowElevation = 12.dp) {
        Column(Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
            val halangan = state.halangan
            if (halangan != null) {
                Row(Modifier.padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ErrorOutline, null, tint = AMBER, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(halangan, color = AMBER, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, lineHeight = 15.sp)
                }
            }
            Button(
                onClick = viewModel::kirim,
                enabled = halangan == null && !state.menyimpan && !state.mengunggahFoto,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ORANGE,
                    disabledContainerColor = Color(0xFFE2E8F0),
                    disabledContentColor = SLATE400,
                ),
            ) {
                Text(
                    when {
                        state.menyimpan -> "Menyimpan…"
                        state.jenis == JenisEntri.WASTE -> "Kirim laporan waste"
                        else -> "Simpan ${state.jenis.label.lowercase()}"
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}

/** Angka tanpa ekor desimal palsu, maksimal empat digit di belakang koma. */
private fun angkaRapi(nilai: Double): String =
    String.format(java.util.Locale.US, "%.4f", nilai).trimEnd('0').trimEnd('.')
