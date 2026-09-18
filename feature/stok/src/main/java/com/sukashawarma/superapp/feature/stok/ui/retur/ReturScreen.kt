package com.sukashawarma.superapp.feature.stok.ui.retur

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
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

// Palet retur mengikuti web: cokelat amber untuk identitas modul, dan warna status
// yang berbeda tiap tahap supaya kartu terbaca sekilas tanpa membaca labelnya.
internal val AmberTua = Color(0xFF92400E)
internal val AmberMuda = Color(0xFFFFFBEB)
internal val AmberGaris = Color(0xFFFDE68A)
internal val BiruTua = Color(0xFF1E40AF)
internal val UnguTua = Color(0xFF6B21A8)
internal val HijauTua = Color(0xFF065F46)
internal val MerahTua = Color(0xFFB91C1C)
internal val Abu900 = Color(0xFF0F172A)
internal val Abu500 = Color(0xFF64748B)
internal val Abu200 = Color(0xFFE2E8F0)
internal val LatarRetur = Color(0xFFFFF8F1)

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
            IconButton(onClick = viewModel::muatUlang) {
                Icon(Icons.Default.Refresh, "Muat ulang", tint = Color(0xFF1E293B))
            }
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
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
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
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        state.tabTerlihat.forEach { t ->
            val terpilih = state.tab == t
            val jumlah = state.jumlah(t)
            Surface(
                onClick = { viewModel.pilihTab(t) },
                shape = RoundedCornerShape(50),
                color = if (terpilih) Color(0xFF0F172A) else Color.White,
                border = BorderStroke(1.dp, if (terpilih) Color(0xFF0F172A) else Abu200),
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        t.label,
                        color = if (terpilih) Color.White else Color(0xFF64748B),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                    if (jumlah > 0) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            jumlah.toString(),
                            Modifier
                                .background(
                                    if (terpilih) Color.White.copy(alpha = 0.25f) else warnaLencana(t),
                                    CircleShape,
                                )
                                .padding(horizontal = 6.dp, vertical = 1.dp),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }
        }
    }
}

/** Antrean yang menuntut tindakan diberi warna mendesak, arsip tidak. */
private fun warnaLencana(tab: TabRetur) = when (tab) {
    TabRetur.PERSETUJUAN -> MerahTua
    TabRetur.KITCHEN -> UnguTua
    else -> Abu500
}

@Composable
private fun TombolAjukan(state: ReturUiState, viewModel: ReturViewModel) {
    // Akun pusat tidak terhubung ke satu outlet pun, jadi tidak ada outlet yang bisa
    // dipotong stoknya. Web menutupnya dengan halaman penjelasan; di sini tombolnya
    // yang tidak muncul, karena tombol yang selalu gagal lebih membingungkan.
    if (state.outletSaya == null) return
    Surface(
        onClick = viewModel::bukaForm,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(14.dp),
        color = AmberTua,
    ) {
        Row(
            Modifier.padding(vertical = 11.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(6.dp))
            Text("Ajukan Retur", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ================================================================ kartu tiket

@Composable
private fun KartuRetur(retur: Retur, state: ReturUiState, viewModel: ReturViewModel) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Abu200),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            KepalaKartu(retur)
            if (retur.status != StatusRetur.DITOLAK) StepperRetur(retur.status)
            retur.items.forEach { BarisItemRetur(it, viewModel) }
            if (!retur.driverNama.isNullOrBlank()) KartuKurir(retur, viewModel)
            if (retur.nomorSuratJalanPengganti != null) KartuSuratJalan(retur)
            if (retur.status == StatusRetur.DITERIMA_KITCHEN) BannerMenungguKirim(retur)
            if (!retur.catatanManager.isNullOrBlank()) {
                Text(
                    "Catatan AM: \"${retur.catatanManager}\"",
                    color = Abu500, fontSize = 11.sp, lineHeight = 15.sp,
                )
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
                color = Abu900, fontSize = 13.sp, fontWeight = FontWeight.Black,
            )
            Text(
                "Diajukan ${tanggalSingkat(retur.createdAt)} oleh ${retur.pembuatNama ?: "Kru"}",
                color = Abu500, fontSize = 10.sp,
            )
        }
        Spacer(Modifier.width(8.dp))
        val warna = warnaStatus(retur.status)
        Surface(
            shape = RoundedCornerShape(50),
            color = warna.copy(alpha = 0.10f),
            border = BorderStroke(1.dp, warna.copy(alpha = 0.30f)),
        ) {
            Text(
                retur.status.label,
                Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                color = warna, fontSize = 9.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.End,
            )
        }
    }
}

internal fun warnaStatus(status: StatusRetur) = when (status) {
    StatusRetur.DIAJUKAN, StatusRetur.DITERIMA_KITCHEN, StatusRetur.MENUNGGU_STOK -> AmberTua
    StatusRetur.DISETUJUI_MANAGER, StatusRetur.DALAM_PENGIRIMAN, StatusRetur.DIKIRIM_PENGGANTI -> BiruTua
    StatusRetur.SELESAI -> HijauTua
    StatusRetur.DITOLAK -> MerahTua
}

@Composable
private fun StepperRetur(status: StatusRetur) {
    val kini = status.langkah
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        StatusRetur.LANGKAH.forEachIndexed { idx, judul ->
            val lewat = kini >= idx
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(52.dp)) {
                Box(
                    Modifier.size(20.dp).background(if (lewat) AmberTua else Abu200, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${idx + 1}",
                        color = if (lewat) Color.White else Abu500,
                        fontSize = 9.sp, fontWeight = FontWeight.Black,
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    judul,
                    color = if (lewat) AmberTua else Abu500,
                    fontSize = 8.sp,
                    fontWeight = if (lewat) FontWeight.Black else FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
            }
        }
    }
}

@Composable
private fun BarisItemRetur(item: ReturItem, viewModel: ReturViewModel) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = LatarRetur,
        border = BorderStroke(1.dp, AmberGaris.copy(alpha = 0.6f)),
    ) {
        Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "${item.namaBahan ?: "Bahan"}  ·  ${qtyTeks(item.qtyKlaim, item.satuan)}",
                    color = Abu900, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold,
                )
                Text("Alasan: ${item.alasan}", color = AmberTua, fontSize = 11.sp, lineHeight = 15.sp)
                if (!item.catatan.isNullOrBlank()) {
                    Text("\"${item.catatan}\"", color = Abu500, fontSize = 10.sp)
                }
                item.qtyDiterimaKitchen?.let {
                    Text(
                        "Timbang Kitchen: ${qtyTeks(it, item.satuan)}",
                        color = UnguTua, fontSize = 11.sp, fontWeight = FontWeight.Bold,
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
}

@Composable
internal fun Thumbnail(url: String, label: String, onKlik: () -> Unit) {
    Box(
        Modifier
            .size(56.dp)
            .background(Abu200, RoundedCornerShape(12.dp))
            .clickable(onClick = onKlik),
    ) {
        AsyncImage(
            model = url,
            contentDescription = label,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().background(Abu200, RoundedCornerShape(12.dp)),
        )
        Text(
            label,
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.55f)),
            color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun KartuKurir(retur: Retur, viewModel: ReturViewModel) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFEFF6FF),
        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
    ) {
        Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.LocalShipping, null, tint = BiruTua, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    buildString {
                        append(retur.jenisLogistik.nilai.uppercase())
                        append(" · ")
                        append(retur.driverNama)
                        retur.driverPlat?.takeIf { it.isNotBlank() }?.let { append(" ($it)") }
                    },
                    color = BiruTua, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                )
                retur.nomorResi?.takeIf { it.isNotBlank() }?.let {
                    Text("Resi: $it", color = BiruTua.copy(alpha = 0.8f), fontSize = 10.sp)
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
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFECFDF5),
        border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
    ) {
        Column(Modifier.padding(11.dp)) {
            Text(
                "SURAT JALAN PENGGANTI (RESMI)",
                color = Abu500, fontSize = 9.sp, fontWeight = FontWeight.Black,
            )
            Text(
                retur.nomorSuratJalanPengganti.orEmpty(),
                color = HijauTua, fontSize = 13.sp, fontWeight = FontWeight.Black,
            )
            // Web menyediakan tombol cetak PDF di sini. Native tidak menirunya:
            // dokumen fisik SJ sudah punya rumahnya sendiri di modul Distribusi,
            // lengkap dengan tanda tangan dan kode verifikasi, dan menyalin
            // pratinjaunya ke sini hanya akan menghasilkan dokumen kedua yang
            // berbeda tampilan untuk nomor yang sama.
            Text(
                "Dokumen lengkapnya dibuka lewat modul Distribusi.",
                color = Abu500, fontSize = 10.sp,
            )
        }
    }
}

@Composable
private fun BannerMenungguKirim(retur: Retur) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF5F3FF),
        border = BorderStroke(1.dp, Color(0xFFDDD6FE)),
    ) {
        Column(Modifier.padding(11.dp)) {
            Text(
                "Fisik Ditimbang di Kitchen — Menunggu Pengiriman Reguler",
                color = UnguTua, fontSize = 11.sp, fontWeight = FontWeight.Bold, lineHeight = 15.sp,
            )
            Text(
                "Barang pengganti otomatis disertakan saat Gudang Pusat menyetujui permintaan stok reguler outlet ini di menu Permintaan.",
                color = UnguTua.copy(alpha = 0.85f), fontSize = 10.sp, lineHeight = 14.sp,
            )
            retur.catatanKitchen?.takeIf { it.isNotBlank() }?.let {
                Text(
                    "Catatan Kitchen: \"$it\"",
                    color = UnguTua, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun TombolAksi(retur: Retur, state: ReturUiState, viewModel: ReturViewModel) {
    val bolehSerah = ReturAkses.serahKurir(state.role, retur.outletId, state.outletSaya)
    val aksi = when {
        retur.status == StatusRetur.DIAJUKAN && state.bolehManager ->
            Triple("Review & Setujui", AmberTua) { viewModel.bukaApprove(retur) }

        retur.status == StatusRetur.DISETUJUI_MANAGER && bolehSerah ->
            Triple("Serahkan ke Kurir", BiruTua) { viewModel.bukaKurir(retur) }

        retur.status == StatusRetur.DITERIMA_KITCHEN && state.bolehKitchen ->
            Triple("Terbitkan SJ Pengganti", BiruTua) { viewModel.bukaKitchen(retur) }

        retur.status == StatusRetur.DALAM_PENGIRIMAN && state.bolehKitchen ->
            Triple("Timbang & Opsi Kirim", UnguTua) { viewModel.bukaKitchen(retur) }

        else -> null
    } ?: return

    Surface(
        onClick = aksi.third,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = aksi.second,
    ) {
        Text(
            aksi.first,
            Modifier.padding(vertical = 10.dp),
            color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

// =================================================================== lightbox

@Composable
private fun DialogFotoPenuh(judul: String, url: String, onTutup: () -> Unit) {
    Dialog(onDismissRequest = onTutup) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White) {
            Column {
                Row(
                    Modifier.padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        judul, Modifier.weight(1f),
                        color = Abu900, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                    IconButton(onClick = onTutup) { Icon(Icons.Default.Close, "Tutup", tint = Abu500) }
                }
                SubcomposeAsyncImage(
                    model = url,
                    contentDescription = judul,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().height(340.dp).background(Abu900),
                    loading = {
                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                            CircularProgressIndicator(color = AmberTua)
                        }
                    },
                    error = {
                        Box(Modifier.fillMaxSize().padding(24.dp), Alignment.Center) {
                            Text(
                                "Foto bukti tidak dapat dimuat.",
                                color = Abu500, fontSize = 12.sp, textAlign = TextAlign.Center,
                            )
                        }
                    },
                )
                Text(
                    "Perhatikan kondisi fisik bahan dan angka display timbangan digital dari outlet.",
                    Modifier.fillMaxWidth().padding(12.dp),
                    color = Abu500, fontSize = 10.sp, textAlign = TextAlign.Center, lineHeight = 14.sp,
                )
            }
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
