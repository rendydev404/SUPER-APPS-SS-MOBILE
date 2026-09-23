package com.sukashawarma.superapp.feature.leader.ui.pettycash

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.RealtimeTables
import com.sukashawarma.superapp.core.ui.ios.KartuIos
import com.sukashawarma.superapp.core.ui.ios.LencanaIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.TombolBundarIos
import com.sukashawarma.superapp.core.ui.ios.TombolKeduaIos
import com.sukashawarma.superapp.core.ui.ios.TombolUtamaIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.ios.tekanIos
import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.core.ui.kaca.denganRuangNav
import com.sukashawarma.superapp.core.ui.kaca.navigationBarsPaddingKaca
import com.sukashawarma.superapp.feature.leader.data.OutletLeader
import com.sukashawarma.superapp.feature.leader.domain.FilterPengajuan
import com.sukashawarma.superapp.feature.leader.domain.Pengajuan
import com.sukashawarma.superapp.feature.leader.domain.StatusPengajuan
import com.sukashawarma.superapp.feature.leader.domain.nominalTerformat
import com.sukashawarma.superapp.feature.leader.domain.rupiah
import com.sukashawarma.superapp.feature.leader.ui.BilahJudulLeader
import com.sukashawarma.superapp.feature.leader.ui.PanelKosong

/**
 * Top Up Petty Cash — cermin `app/dashboard/leader/petty-cash/page.tsx` web.
 *
 * Dua tindakan di layar ini memanggil RPC yang sama dengan web
 * (`create_petty_cash_topup` dan `leader_forward_funds`), keduanya `SECURITY DEFINER`
 * yang memeriksa role dan urutan status di dalam database. Layar ini tidak
 * memutuskan sendiri boleh-tidaknya sebuah pengajuan berpindah tahap.
 */
@Composable
fun PettyCashScreen(
    onExit: () -> Unit,
    viewModel: PettyCashViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    RealtimeRefresh(RealtimeTables.PETTY_CASH_TOPUPS) { viewModel.muatUlang(silent = true) }

    LaunchedEffect(state.kabar, state.galat) {
        val pesan = state.kabar ?: state.galat
        if (pesan != null) {
            snackbar.showSnackbar(pesan)
            viewModel.tutupKabar()
        }
    }

    Scaffold(
        containerColor = WarnaIos.Latar,
        snackbarHost = { SnackbarHost(snackbar, Modifier.navigationBarsPaddingKaca()) },
        topBar = { BilahJudulLeader("Top Up Petty Cash", onExit) { viewModel.muatUlang() } },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(
                start = UkuranIos.TepiLayar,
                end = UkuranIos.TepiLayar,
                top = 12.dp,
                bottom = 16.dp,
            ).denganRuangNav(),
            verticalArrangement = Arrangement.spacedBy(UkuranIos.JarakKartu),
        ) {
            item { TombolBukaForm(state, viewModel) }
            if (state.formTerbuka) {
                item { FormPengajuan(state, viewModel) }
            }
            item { BarisFilter(state, viewModel) }
            if (state.terlihat.isEmpty()) {
                item { PanelKosong("Tidak ada pengajuan pada filter ini.", IkonIos.Payments) }
            }
            items(state.terlihat, key = { it.id }) { pengajuan ->
                KartuPengajuan(
                    pengajuan = pengajuan,
                    sedangDiproses = pengajuan.id in state.sedangDiproses,
                    onSerahkan = { viewModel.serahkan(pengajuan) },
                    onLihatBukti = { viewModel.bukaBukti(pengajuan.buktiTransferUrl) },
                )
            }
        }
    }

    state.buktiTerbuka?.let { url ->
        DialogBukti(url) { viewModel.bukaBukti(null) }
    }
}

@Composable
private fun TombolBukaForm(state: PettyCashUiState, viewModel: PettyCashViewModel) {
    if (state.formTerbuka) {
        TombolKeduaIos(
            "Tutup Form",
            onKlik = { viewModel.bukaForm(!state.formTerbuka) },
            ikon = IkonIos.Close,
        )
    } else {
        TombolUtamaIos(
            "Form Pengajuan Baru",
            onKlik = { viewModel.bukaForm(!state.formTerbuka) },
            ikon = IkonIos.Add,
        )
    }
}

@Composable
private fun FormPengajuan(state: PettyCashUiState, viewModel: PettyCashViewModel) {
    val form = state.form
    KartuIos {
        LabelLangkah("1. Pilih Cabang Tujuan", "Cabang yang memerlukan tambahan dana operasional.")
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            state.cabang.forEach { outlet ->
                KartuPilihCabang(outlet, outlet.id == form.outletId) { viewModel.pilihOutlet(outlet.id) }
            }
        }

        Spacer(Modifier.height(22.dp))
        LabelLangkah("2. Nominal & Keperluan", "Isi angka dan alasan yang jelas.")
        Spacer(Modifier.height(10.dp))
        KolomIsian(
            label = "Nominal Top Up",
            // Yang tampil sudah berpemisah ribuan, yang disimpan tetap angka mentah.
            // Memformat di state akan membuat "1.250.000" ikut terkirim ke RPC.
            nilai = nominalTerformat(form.nominal),
            awalan = "Rp",
            angka = true,
            petunjuk = "0",
        ) { viewModel.ubahForm(form.copy(nominal = it.filter { c -> c.isDigit() })) }
        Spacer(Modifier.height(12.dp))
        KolomIsian(
            label = "Alasan / Keperluan Operasional",
            nilai = form.keperluan,
            barisBanyak = true,
            petunjuk = "Contoh: beli es kristal, plastik besar…",
        ) { viewModel.ubahForm(form.copy(keperluan = it)) }

        Spacer(Modifier.height(22.dp))
        LabelLangkah(
            "3. Rekening Tujuan",
            "Finance mentransfer ke rekening ini, jadi wajib lengkap.",
        )
        Spacer(Modifier.height(10.dp))
        KolomIsian(label = "Nama Bank", nilai = form.namaBank, petunjuk = "BCA / Mandiri / BRI") {
            viewModel.ubahForm(form.copy(namaBank = it))
        }
        Spacer(Modifier.height(12.dp))
        KolomIsian(
            label = "No. Rekening",
            nilai = form.nomorRekening,
            angka = true,
            petunjuk = "1234567890",
        ) { viewModel.ubahForm(form.copy(nomorRekening = it)) }
        Spacer(Modifier.height(12.dp))
        KolomIsian(
            label = "Atas Nama",
            nilai = form.atasNama,
            petunjuk = "Nama pemilik rekening",
        ) { viewModel.ubahForm(form.copy(atasNama = it)) }

        Spacer(Modifier.height(20.dp))
        // `memuat` mematikan tombol selama RPC berjalan — sama dengan `enabled =
        // !mengirim` sebelumnya, supaya satu ketukan ganda tidak melahirkan dua pengajuan.
        TombolUtamaIos(
            "Kirim Pengajuan",
            onKlik = viewModel::kirim,
            memuat = state.mengirim,
            ikon = Icons.AutoMirrored.Filled.Send,
        )
    }
}

@Composable
private fun LabelLangkah(judul: String, keterangan: String) {
    Text(judul, style = TipeIos.Utama)
    Spacer(Modifier.height(2.dp))
    Text(keterangan, style = TipeIos.Catatan, lineHeight = 17.sp)
}

@Composable
private fun KartuPilihCabang(outlet: OutletLeader, terpilih: Boolean, onKlik: () -> Unit) {
    // Petak abu di atas kartu putih, menyala aksen saat terpilih — pola pilihan
    // bersegmen iOS, tanpa garis tepi.
    Column(
        Modifier
            .width(210.dp)
            .clip(UkuranIos.SudutPetak)
            .background(if (terpilih) WarnaIos.Aksen else WarnaIos.Latar)
            .tekanIos(onKlik)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                outlet.nama,
                Modifier.weight(1f),
                style = TipeIos.Utama.copy(fontSize = 15.sp, color = if (terpilih) Color.White else WarnaIos.Label),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (terpilih) {
                Icon(IkonIos.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        // Rekening kosong ditandai di sini, bukan hanya saat tombol kirim ditekan:
        // cabang tanpa rekening berarti form harus diisi manual lebih dulu.
        Text(
            if (outlet.punyaRekening) {
                "${outlet.namaBank} · ${outlet.nomorRekening}"
            } else {
                "Belum ada rekening tersimpan"
            },
            style = TipeIos.Kecil.copy(fontWeight = FontWeight.Medium),
            color = when {
                !outlet.punyaRekening && terpilih -> Color.White
                !outlet.punyaRekening -> NadaIos.BAHAYA.teks
                terpilih -> Color.White.copy(alpha = 0.8f)
                else -> WarnaIos.LabelKedua
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun KolomIsian(
    label: String,
    nilai: String,
    petunjuk: String = "",
    awalan: String? = null,
    angka: Boolean = false,
    barisBanyak: Boolean = false,
    onUbah: (String) -> Unit,
) {
    Column {
        Text(
            label,
            Modifier.padding(start = 4.dp),
            style = TipeIos.Catatan.copy(fontWeight = FontWeight.Medium),
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = nilai,
            onValueChange = onUbah,
            modifier = Modifier.fillMaxWidth().then(
                if (barisBanyak) Modifier.heightIn(min = 100.dp) else Modifier
            ),
            singleLine = !barisBanyak,
            textStyle = TipeIos.Keterangan,
            placeholder = { Text(petunjuk, style = TipeIos.Keterangan.copy(color = WarnaIos.Abu)) },
            prefix = awalan?.let {
                { Text(it, style = TipeIos.Keterangan.copy(color = WarnaIos.LabelKedua, fontWeight = FontWeight.SemiBold)) }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = if (angka) KeyboardType.Number else KeyboardType.Text,
            ),
            shape = UkuranIos.SudutKontrol,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedIndicatorColor = WarnaIos.Aksen,
                unfocusedIndicatorColor = WarnaIos.Pemisah,
                focusedTextColor = WarnaIos.Label,
                unfocusedTextColor = WarnaIos.Label,
                cursorColor = WarnaIos.Aksen,
            ),
        )
    }
}

@Composable
private fun BarisFilter(state: PettyCashUiState, viewModel: PettyCashViewModel) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterPengajuan.entries.forEach { filter ->
            val jumlah = when (filter) {
                FilterPengajuan.SEMUA -> state.jumlahSemua
                FilterPengajuan.BUTUH_AKSI -> state.jumlahButuhAksi
                FilterPengajuan.MENUNGGU_AM -> state.jumlahMenungguAm
                FilterPengajuan.SELESAI -> state.jumlahSelesai
            }
            PilFilter(
                teks = "${filter.label} ($jumlah)",
                aktif = state.filter == filter,
                // Pil "Action Leader" menyala hijau supaya satu-satunya tab yang
                // menuntut tindakan tidak tenggelam di antara tab pemantauan.
                mendesak = filter == FilterPengajuan.BUTUH_AKSI && jumlah > 0,
            ) { viewModel.pilihFilter(filter) }
        }
    }
}

@Composable
private fun PilFilter(teks: String, aktif: Boolean, mendesak: Boolean, onKlik: () -> Unit) {
    val latar = when {
        aktif && mendesak -> WarnaIos.Hijau
        aktif -> WarnaIos.Aksen
        mendesak -> NadaIos.SUKSES.warna.copy(alpha = 0.14f)
        else -> WarnaIos.Kartu
    }
    val warnaTeks = when {
        aktif -> Color.White
        mendesak -> NadaIos.SUKSES.teks
        else -> WarnaIos.Label
    }
    Box(
        Modifier
            .height(36.dp)
            .clip(UkuranIos.SudutKapsul)
            .background(latar)
            .tekanIos(onKlik)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(teks, color = warnaTeks, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun KartuPengajuan(
    pengajuan: Pengajuan,
    sedangDiproses: Boolean,
    onSerahkan: () -> Unit,
    onLihatBukti: () -> Unit,
) {
    KartuIos {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f).padding(end = 8.dp)) {
                Text(pengajuan.outletNama, style = TipeIos.Utama, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(pengajuan.waktuTeks, style = TipeIos.Catatan)
            }
            BadgeStatus(pengajuan.status)
        }

        Spacer(Modifier.height(12.dp))
        Text(
            rupiah(pengajuan.jumlah),
            style = TipeIos.AngkaBesar,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        pengajuan.deskripsi?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, style = TipeIos.SubJudul, lineHeight = 20.sp)
        }

        pengajuan.rekeningTeks?.let { rekening ->
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(UkuranIos.SudutBlok)
                    .background(WarnaIos.Latar)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(IkonIos.AccountBalanceWallet, null, tint = WarnaIos.Abu, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(rekening, style = TipeIos.Keterangan.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold))
                    pengajuan.atasNama?.takeIf { it.isNotBlank() }?.let {
                        Text("a.n $it", style = TipeIos.Catatan)
                    }
                }
            }
        }

        // Ditumpuk, bukan berdampingan: label "Terima & Serahkan ke Crew" pada ukuran
        // tombol iOS tidak muat satu baris bila berbagi lebar dengan tombol Bukti.
        if (pengajuan.butuhAksi || pengajuan.buktiTransferUrl != null) {
            Spacer(Modifier.height(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (pengajuan.butuhAksi) {
                    TombolUtamaIos(
                        "Terima & Serahkan ke Crew",
                        onKlik = onSerahkan,
                        memuat = sedangDiproses,
                    )
                }
                if (pengajuan.buktiTransferUrl != null) {
                    TombolKeduaIos("Bukti", onKlik = onLihatBukti, ikon = IkonIos.Image)
                }
            }
        }
    }
}

@Composable
private fun BadgeStatus(status: StatusPengajuan) {
    when (status) {
        StatusPengajuan.DIAJUKAN, StatusPengajuan.DITERUSKAN_KE_AM ->
            LencanaIos(status.label, NadaIos.PERINGATAN)
        StatusPengajuan.DI_FINANCE ->
            LencanaIos(status.label, NadaIos.INFO)
        StatusPengajuan.DISETUJUI_FINANCE, StatusPengajuan.DITERUSKAN_FINANCE,
        StatusPengajuan.DISERAHKAN_KE_CREW ->
            LencanaIos(status.label, NadaIos.SUKSES)
        // Satu-satunya status yang menuntut tindakan diberi warna merek dan ikon,
        // bukan titik status, supaya terlihat berbeda dari kartu yang cuma perlu dibaca.
        StatusPengajuan.SIAP_DISERAHKAN ->
            LencanaIos(status.label, NadaIos.AKSEN, ikon = IkonIos.ArrowForward)
        StatusPengajuan.SELESAI ->
            LencanaIos(status.label, NadaIos.NETRAL, ikon = IkonIos.CheckCircle)
        StatusPengajuan.DITOLAK ->
            LencanaIos(status.label, NadaIos.BAHAYA)
    }
}

/**
 * Bukti transfer versi layar penuh.
 *
 * Tanpa tombol unduh, tidak seperti web: berkasnya sudah ada di Supabase Storage dan
 * yang dibutuhkan leader di lapangan hanyalah melihatnya cukup besar untuk membaca
 * nominal dan jam transfer.
 */
@Composable
private fun DialogBukti(url: String, onTutup: () -> Unit) {
    Dialog(onDismissRequest = onTutup) {
        Surface(shape = UkuranIos.SudutKartu, color = WarnaIos.Kartu) {
            Column {
                Row(
                    Modifier.fillMaxWidth().padding(start = 18.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Bukti Transfer", Modifier.weight(1f), style = TipeIos.Utama)
                    TombolBundarIos(IkonIos.Close, "Tutup", onTutup, warnaIkon = WarnaIos.LabelKedua)
                }
                Box(
                    Modifier.fillMaxWidth().heightIn(min = 240.dp, max = 460.dp).background(WarnaIos.Latar),
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = url,
                        contentDescription = "Bukti transfer",
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.Fit,
                    )
                }
            }
        }
    }
}
