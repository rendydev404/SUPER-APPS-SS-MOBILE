package com.sukashawarma.superapp.feature.distribusi.ui.detail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sukashawarma.superapp.core.ui.ios.BarisIos
import com.sukashawarma.superapp.core.ui.ios.BilahJudulIos
import com.sukashawarma.superapp.core.ui.ios.GrupIos
import com.sukashawarma.superapp.core.ui.ios.KapsulPilihanIos
import com.sukashawarma.superapp.core.ui.ios.KartuIos
import com.sukashawarma.superapp.core.ui.ios.LencanaIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.PemisahIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.TombolKapsulIos
import com.sukashawarma.superapp.core.ui.ios.TombolKeduaIos
import com.sukashawarma.superapp.core.ui.ios.TombolUtamaIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.ios.warnaKolomIos
import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.feature.distribusi.domain.AlokasiVendor
import com.sukashawarma.superapp.feature.distribusi.domain.PengirimanPusat
import com.sukashawarma.superapp.feature.distribusi.ui.ttd.TandaTanganCanvas

/*
 * Aksi sisi pengirim (kitchen) di layar detail — cermin `SignatureFlow.tsx` dan
 * blok aksi pusat `SuratJalanDetail.tsx` web.
 */

/** Vendor per item untuk bahan multi-vendor selama dokumen masih draft. */
@Composable
fun KartuVendorDraft(state: DetailUiState, viewModel: DetailViewModel) {
    val multi = state.baris.filter { AlokasiVendor.multiVendor(state.saldoVendor[it.bahanId].orEmpty()) }
    if (multi.isEmpty()) return
    val belum = state.vendorBelumDipilih
    GrupIos(
        judul = "Vendor Gudang Pusat",
        catatan = if (belum.isEmpty()) null
        else "${belum.size} bahan multi-vendor belum dipilih vendornya. Surat jalan tidak bisa dikirim sebelum ini lengkap.",
    ) {
        multi.forEachIndexed { i, baris ->
            if (i > 0) PemisahIos()
            var buka by remember(baris.itemId) { mutableStateOf(false) }
            val vendors = state.saldoVendor[baris.bahanId].orEmpty()
            val memproses = state.memproses == "vendor:${baris.itemId}"
            BarisIos(
                judul = baris.nama,
                keterangan = "${baris.qtyDikirim} ${baris.satuan}",
                chevron = false,
                trailing = {
                    Box {
                        TombolKapsulIos(
                            when {
                                memproses -> "Menyimpan..."
                                baris.vendorNama != null -> baris.vendorNama
                                else -> "Pilih vendor"
                            },
                            { if (state.memproses == null) buka = true },
                            chevron = true,
                        )
                        DropdownMenu(expanded = buka, onDismissRequest = { buka = false }) {
                            vendors.forEach { v ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(v.vendorNama, style = TipeIos.Isi)
                                            Text(
                                                if (v.aktif) "Sisa ${AlokasiVendor.angka(v.sisa)} ${baris.satuan}"
                                                else "Saldo belum dihitung",
                                                style = TipeIos.Catatan,
                                            )
                                        }
                                    },
                                    onClick = {
                                        buka = false
                                        if (v.vendorId != baris.vendorId) viewModel.ubahVendor(baris.itemId, v.vendorId)
                                    },
                                )
                            }
                        }
                    }
                },
            )
        }
    }
}

/**
 * Dua tanda tangan pengirim (Admin Gudang, lalu Supir), tombol Kirim, dan
 * tombol Batalkan Draft. Form goresannya dibuka sebagai layar penuh lewat
 * [onMulaiTtd] — bukan di dalam daftar, yang membuangnya saat digulir.
 */
@Composable
fun KartuPengirimanDraft(
    state: DetailUiState,
    viewModel: DetailViewModel,
    onMulaiTtd: (String) -> Unit,
    onMintaBatal: () -> Unit,
) {
    val peran = state.peranTtd
    val sudahAdmin = PengirimanPusat.sudahTtdAdmin(peran)
    val sudahSupir = PengirimanPusat.sudahTtdSupir(peran)

    Column(verticalArrangement = Arrangement.spacedBy(UkuranIos.JarakKartu)) {
        if (!state.bolehKelolaKirim) {
            KartuIos {
                Text("Menunggu Gudang Pusat", style = TipeIos.Utama)
                Text("Surat jalan ini masih draft dan belum dikirim.", style = TipeIos.Catatan)
            }
            return@Column
        }
        GrupIos(judul = "Tanda tangan pengirim") {
            BarisTtd(
                judul = if (sudahAdmin) "Admin Gudang sudah tanda tangan" else "Tanda Tangan Admin Gudang",
                sudah = sudahAdmin,
                ikon = IkonIos.Person,
                onKlik = { onMulaiTtd(PengirimanPusat.PERAN_ADMIN) },
            )
            PemisahIos(inset = 58.dp)
            BarisTtd(
                judul = if (sudahSupir) "Supir sudah tanda tangan" else "Tanda Tangan Supir (Kurir)",
                sudah = sudahSupir,
                ikon = IkonIos.LocalShipping,
                onKlik = { onMulaiTtd(PengirimanPusat.PERAN_SUPIR) },
            )
        }

        val alasan = state.alasanTidakBisaKirim
        TombolUtamaIos(
            if (state.memproses == "kirim") "Mengirim..." else "Kirim Surat Jalan",
            viewModel::kirim,
            aktif = alasan == null && state.memproses == null,
            memuat = state.memproses == "kirim",
            ikon = IkonIos.LocalShipping,
        )
        Text(
            alasan ?: "Setelah dikirim, stok Gudang Pusat dipotong dan outlet menerima notifikasi.",
            Modifier.padding(horizontal = 4.dp),
            style = TipeIos.Catatan,
        )
        if (state.bolehBatalkan) {
            TombolKeduaIos(
                if (state.memproses == "batal") "Membatalkan..." else "Batalkan Draft",
                onMintaBatal,
                aktif = state.memproses == null,
                ikon = IkonIos.Close,
                warna = WarnaIos.Merah,
            )
        }
    }
}

/** Layar penuh untuk menggores tanda tangan pengirim, di atas layar detail. */
@Composable
fun LayarTtdPengirim(
    peran: String,
    state: DetailUiState,
    onSimpan: (String, String) -> Unit,
    onTutup: () -> Unit,
) {
    BackHandler(onBack = onTutup)
    Column(
        Modifier
            .fillMaxSize()
            .background(WarnaIos.Latar)
            // Menelan sentuhan supaya tidak tembus ke layar detail di bawahnya.
            .pointerInput(Unit) { detectTapGestures { } },
    ) {
        BilahJudulIos(
            judul = if (peran == PengirimanPusat.PERAN_ADMIN) "Tanda Tangan Admin Gudang" else "Tanda Tangan Supir",
            onKembali = onTutup,
        )
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(UkuranIos.TepiLayar),
        ) {
            FormTtdPengirim(peran, state, onSimpan, onTutup)
            if (state.memproses == "ttd") {
                Spacer(Modifier.height(10.dp))
                Text("Menyimpan tanda tangan…", style = TipeIos.Catatan)
            }
        }
    }
}

@Composable
private fun BarisTtd(judul: String, sudah: Boolean, ikon: ImageVector, onKlik: () -> Unit) {
    BarisIos(
        judul = judul,
        ikon = ikon,
        nadaIkon = if (sudah) NadaIos.SUKSES else NadaIos.AKSEN,
        onKlik = if (sudah) null else onKlik,
        trailing = if (sudah) {
            { LencanaIos("Selesai", NadaIos.SUKSES, titik = false) }
        } else {
            null
        },
    )
}

/**
 * Nama penanda tangan + kanvas. Admin dipilih dari staf ber-role kitchen; supir
 * internal dari seluruh staf aktif, supir eksternal (vendor/Lalamove) diketik bebas.
 */
@Composable
private fun FormTtdPengirim(
    peran: String,
    state: DetailUiState,
    onSimpan: (String, String) -> Unit,
    onBatal: () -> Unit,
) {
    val admin = peran == PengirimanPusat.PERAN_ADMIN
    val namaSaya = AppSession.staff.value?.name.orEmpty()
    var eksternal by rememberSaveable { mutableStateOf(false) }
    var nama by rememberSaveable(peran) {
        mutableStateOf(if (admin && namaSaya in state.namaStafKitchen) namaSaya else "")
    }
    val sumber = if (admin) state.namaStafKitchen else state.namaSemuaStaf
    val kunci = nama.trim().lowercase()
    val saran = if (eksternal || kunci.isEmpty()) {
        if (admin) sumber.take(6) else emptyList()
    } else {
        sumber.filter { it.lowercase().contains(kunci) && !it.equals(nama.trim(), ignoreCase = true) }.take(6)
    }

    KartuIos {
        if (!admin) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KapsulPilihanIos("Supir internal", !eksternal, { eksternal = false })
                KapsulPilihanIos("Vendor / Lalamove", eksternal, { eksternal = true })
            }
            Spacer(Modifier.height(10.dp))
        }
        OutlinedTextField(
            value = nama,
            onValueChange = { nama = it },
            label = { Text(if (admin) "Nama admin gudang" else if (eksternal) "Nama supir / kurir eksternal" else "Nama supir") },
            singleLine = true,
            shape = UkuranIos.SudutKontrol,
            colors = warnaKolomIos(),
            modifier = Modifier.fillMaxWidth(),
        )
        if (saran.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                saran.forEach { s -> KapsulPilihanIos(s, s == nama, { nama = s }) }
            }
        }
        Spacer(Modifier.height(12.dp))
        TandaTanganCanvas(
            onSelesai = { gambar -> onSimpan(nama, gambar) },
            onBatal = onBatal,
        )
    }
}

/** Verifikasi akhir pusat untuk dokumen yang sudah diterima outlet. */
@Composable
fun KartuVerifikasiAkhir(state: DetailUiState, onMintaTutup: () -> Unit) {
    KartuIos {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(IkonIos.FactCheck, null, tint = WarnaIos.Aksen, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Verifikasi Akhir", style = TipeIos.Utama)
                Text(
                    "Outlet sudah memverifikasi penerimaan. Tutup dokumen bila hasilnya sudah dicek pusat.",
                    style = TipeIos.Catatan,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        TombolUtamaIos(
            if (state.memproses == "tutup") "Menyimpan..." else "Tandai Selesai",
            onMintaTutup,
            aktif = state.memproses == null,
            memuat = state.memproses == "tutup",
            ikon = IkonIos.CheckCircle,
        )
    }
}

@Composable
fun KartuDibatalkan(catatan: String?) {
    KartuIos {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(IkonIos.Close, null, tint = NadaIos.BAHAYA.teks, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text("Surat jalan ini dibatalkan", style = TipeIos.Utama.copy(color = NadaIos.BAHAYA.teks))
        }
        if (!catatan.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(catatan, style = TipeIos.Catatan)
        }
    }
}

@Composable
fun DialogBatalkan(onKonfirmasi: (String) -> Unit, onTutup: () -> Unit) {
    var alasan by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onTutup,
        containerColor = WarnaIos.Kartu,
        shape = UkuranIos.SudutKartu,
        title = { Text("Batalkan draft?", style = TipeIos.Utama) },
        text = {
            Column {
                Text(
                    "Dokumen akan berstatus dibatalkan dan permintaan bahan yang terkait ikut dibatalkan.",
                    style = TipeIos.SubJudul,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = alasan,
                    onValueChange = { alasan = it },
                    label = { Text("Alasan (opsional)") },
                    shape = UkuranIos.SudutKontrol,
                    colors = warnaKolomIos(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onKonfirmasi(alasan) }) {
                Text("Batalkan Draft", color = WarnaIos.Merah, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = { TextButton(onClick = onTutup) { Text("Kembali", color = WarnaIos.Aksen) } },
    )
}

@Composable
fun DialogTutup(onKonfirmasi: () -> Unit, onTutup: () -> Unit) {
    AlertDialog(
        onDismissRequest = onTutup,
        containerColor = WarnaIos.Kartu,
        shape = UkuranIos.SudutKartu,
        title = { Text("Verifikasi akhir?", style = TipeIos.Utama) },
        text = {
            Text(
                "Surat jalan akan ditandai selesai dan tidak bisa dibuka kembali dari aplikasi.",
                style = TipeIos.SubJudul,
            )
        },
        confirmButton = {
            TextButton(onClick = onKonfirmasi) {
                Text("Tandai Selesai", color = WarnaIos.Aksen, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = { TextButton(onClick = onTutup) { Text("Batal", color = WarnaIos.Aksen) } },
    )
}
