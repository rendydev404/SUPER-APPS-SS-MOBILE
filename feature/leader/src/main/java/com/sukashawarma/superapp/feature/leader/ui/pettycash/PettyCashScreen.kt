package com.sukashawarma.superapp.feature.leader.ui.pettycash

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.sukashawarma.superapp.feature.leader.data.OutletLeader
import com.sukashawarma.superapp.feature.leader.domain.FilterPengajuan
import com.sukashawarma.superapp.feature.leader.domain.Pengajuan
import com.sukashawarma.superapp.feature.leader.domain.StatusPengajuan
import com.sukashawarma.superapp.feature.leader.domain.nominalTerformat
import com.sukashawarma.superapp.feature.leader.domain.rupiah
import com.sukashawarma.superapp.feature.leader.ui.AmberGaris
import com.sukashawarma.superapp.feature.leader.ui.AmberLatar
import com.sukashawarma.superapp.feature.leader.ui.AmberTeks
import com.sukashawarma.superapp.feature.leader.ui.BiruGaris
import com.sukashawarma.superapp.feature.leader.ui.BiruLatar
import com.sukashawarma.superapp.feature.leader.ui.BiruTeks
import com.sukashawarma.superapp.feature.leader.ui.GarisKartu
import com.sukashawarma.superapp.feature.leader.ui.HijauGaris
import com.sukashawarma.superapp.feature.leader.ui.HijauLatar
import com.sukashawarma.superapp.feature.leader.ui.HijauTeks
import com.sukashawarma.superapp.feature.leader.ui.KartuPanel
import com.sukashawarma.superapp.feature.leader.ui.MerahGaris
import com.sukashawarma.superapp.feature.leader.ui.MerahLatar
import com.sukashawarma.superapp.feature.leader.ui.MerahTeks
import com.sukashawarma.superapp.feature.leader.ui.PanelKosong
import com.sukashawarma.superapp.feature.leader.ui.PilStatus
import com.sukashawarma.superapp.presentation.theme.SukaBrown
import com.sukashawarma.superapp.presentation.theme.SukaCream
import com.sukashawarma.superapp.presentation.theme.SukaGray400
import com.sukashawarma.superapp.presentation.theme.SukaOrange

/**
 * Top Up Petty Cash — cermin `app/dashboard/leader/petty-cash/page.tsx` web.
 *
 * Dua tindakan di layar ini memanggil RPC yang sama dengan web
 * (`create_petty_cash_topup` dan `leader_forward_funds`), keduanya `SECURITY DEFINER`
 * yang memeriksa role dan urutan status di dalam database. Layar ini tidak
 * memutuskan sendiri boleh-tidaknya sebuah pengajuan berpindah tahap.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PettyCashScreen(
    onExit: () -> Unit,
    viewModel: PettyCashViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    RealtimeRefresh(RealtimeTables.PETTY_CASH_TOPUPS) { viewModel.muatUlang() }

    LaunchedEffect(state.kabar, state.galat) {
        val pesan = state.kabar ?: state.galat
        if (pesan != null) {
            snackbar.showSnackbar(pesan)
            viewModel.tutupKabar()
        }
    }

    Scaffold(
        containerColor = SukaCream,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Top Up Petty Cash",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = SukaBrown,
                        maxLines = 1,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = SukaBrown)
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::muatUlang) {
                        Icon(Icons.Default.Refresh, "Muat ulang", tint = SukaBrown)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { TombolBukaForm(state, viewModel) }
            if (state.formTerbuka) {
                item { FormPengajuan(state, viewModel) }
            }
            item { BarisFilter(state, viewModel) }
            if (state.terlihat.isEmpty()) {
                item { KartuPanel { PanelKosong("Tidak ada pengajuan pada filter ini.") } }
            }
            items(state.terlihat, key = { it.id }) { pengajuan ->
                KartuPengajuan(
                    pengajuan = pengajuan,
                    sedangDiproses = pengajuan.id in state.sedangDiproses,
                    onSerahkan = { viewModel.serahkan(pengajuan) },
                    onLihatBukti = { viewModel.bukaBukti(pengajuan.buktiTransferUrl) },
                )
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }

    state.buktiTerbuka?.let { url ->
        DialogBukti(url) { viewModel.bukaBukti(null) }
    }
}

@Composable
private fun TombolBukaForm(state: PettyCashUiState, viewModel: PettyCashViewModel) {
    Button(
        onClick = { viewModel.bukaForm(!state.formTerbuka) },
        Modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (state.formTerbuka) Color.White else SukaBrown,
            contentColor = if (state.formTerbuka) SukaBrown else Color.White,
        ),
        border = BorderStroke(1.dp, if (state.formTerbuka) GarisKartu else SukaBrown),
    ) {
        Icon(
            if (state.formTerbuka) Icons.Default.Close else Icons.Default.Add,
            null,
            Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            if (state.formTerbuka) "Tutup Form" else "Form Pengajuan Baru",
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
private fun FormPengajuan(state: PettyCashUiState, viewModel: PettyCashViewModel) {
    val form = state.form
    KartuPanel {
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

        Spacer(Modifier.height(20.dp))
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

        Spacer(Modifier.height(20.dp))
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

        Spacer(Modifier.height(18.dp))
        Button(
            onClick = viewModel::kirim,
            Modifier.fillMaxWidth().height(50.dp),
            enabled = !state.mengirim,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SukaOrange, contentColor = Color.White),
        ) {
            if (state.mengirim) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Mengirim…", fontSize = 13.sp, fontWeight = FontWeight.Black)
            } else {
                Icon(Icons.Default.Send, null, Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Kirim Pengajuan", fontSize = 13.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun LabelLangkah(judul: String, keterangan: String) {
    Text(judul, color = SukaBrown, fontSize = 14.sp, fontWeight = FontWeight.Black)
    Spacer(Modifier.height(2.dp))
    Text(
        keterangan,
        color = SukaGray400,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 15.sp,
    )
}

@Composable
private fun KartuPilihCabang(outlet: OutletLeader, terpilih: Boolean, onKlik: () -> Unit) {
    Surface(
        Modifier.width(210.dp).clickable(onClick = onKlik),
        shape = RoundedCornerShape(16.dp),
        color = if (terpilih) SukaBrown else Color.White,
        border = BorderStroke(1.dp, if (terpilih) SukaBrown else GarisKartu),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    outlet.nama,
                    Modifier.weight(1f),
                    color = if (terpilih) Color.White else SukaBrown,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (terpilih) {
                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(15.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            // Rekening kosong ditandai di sini, bukan hanya saat tombol kirim ditekan:
            // cabang tanpa rekening berarti form harus diisi manual lebih dulu.
            Text(
                if (outlet.punyaRekening) {
                    "${outlet.namaBank} · ${outlet.nomorRekening}"
                } else {
                    "Belum ada rekening tersimpan"
                },
                color = when {
                    !outlet.punyaRekening -> MerahTeks
                    terpilih -> Color.White.copy(alpha = 0.7f)
                    else -> SukaGray400
                },
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
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
            label.uppercase(),
            color = SukaGray400,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.7.sp,
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = nilai,
            onValueChange = onUbah,
            modifier = Modifier.fillMaxWidth().then(
                if (barisBanyak) Modifier.heightIn(min = 100.dp) else Modifier
            ),
            singleLine = !barisBanyak,
            placeholder = { Text(petunjuk, color = SukaGray400, fontSize = 13.sp) },
            prefix = awalan?.let {
                { Text(it, color = SukaGray400, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = if (angka) KeyboardType.Number else KeyboardType.Text,
            ),
            shape = RoundedCornerShape(14.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedIndicatorColor = SukaOrange,
                unfocusedIndicatorColor = GarisKartu,
                focusedTextColor = SukaBrown,
                unfocusedTextColor = SukaBrown,
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
        aktif && mendesak -> HijauTeks
        aktif -> SukaBrown
        mendesak -> HijauLatar
        else -> Color.White
    }
    val warnaTeks = when {
        aktif -> Color.White
        mendesak -> HijauTeks
        else -> SukaBrown
    }
    Surface(
        Modifier.clickable(onClick = onKlik),
        shape = RoundedCornerShape(50),
        color = latar,
        border = BorderStroke(1.dp, if (mendesak && !aktif) HijauGaris else GarisKartu),
    ) {
        Text(
            teks,
            Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            color = warnaTeks,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
        )
    }
}

@Composable
private fun KartuPengajuan(
    pengajuan: Pengajuan,
    sedangDiproses: Boolean,
    onSerahkan: () -> Unit,
    onLihatBukti: () -> Unit,
) {
    KartuPanel {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    pengajuan.outletNama,
                    color = SukaBrown,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    pengajuan.waktuTeks,
                    color = SukaGray400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            BadgeStatus(pengajuan.status)
        }

        Spacer(Modifier.height(12.dp))
        Text(
            rupiah(pengajuan.jumlah),
            color = SukaBrown,
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        pengajuan.deskripsi?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(4.dp))
            Text(
                it,
                color = SukaGray400,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 17.sp,
            )
        }

        pengajuan.rekeningTeks?.let { rekening ->
            Spacer(Modifier.height(12.dp))
            Surface(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = SukaCream,
                border = BorderStroke(1.dp, GarisKartu),
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AccountBalance,
                        null,
                        tint = SukaGray400,
                        modifier = Modifier.size(17.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            rekening,
                            color = SukaBrown,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                        )
                        pengajuan.atasNama?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                "a.n $it",
                                color = SukaGray400,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }

        if (pengajuan.butuhAksi || pengajuan.buktiTransferUrl != null) {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (pengajuan.butuhAksi) {
                    Button(
                        onClick = onSerahkan,
                        Modifier.weight(1f).height(44.dp),
                        enabled = !sedangDiproses,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SukaOrange,
                            contentColor = Color.White,
                        ),
                    ) {
                        if (sedangDiproses) {
                            CircularProgressIndicator(
                                Modifier.size(15.dp),
                                strokeWidth = 2.dp,
                                color = Color.White,
                            )
                        } else {
                            Text(
                                "Terima & Serahkan ke Crew",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                            )
                        }
                    }
                }
                if (pengajuan.buktiTransferUrl != null) {
                    Surface(
                        Modifier.height(44.dp).clickable(onClick = onLihatBukti),
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, GarisKartu),
                    ) {
                        Row(
                            Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.Image,
                                null,
                                tint = SukaBrown,
                                modifier = Modifier.size(15.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Bukti",
                                color = SukaBrown,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BadgeStatus(status: StatusPengajuan) {
    val (latar, garis, teks) = when (status) {
        StatusPengajuan.DIAJUKAN, StatusPengajuan.DITERUSKAN_KE_AM ->
            Triple(AmberLatar, AmberGaris, AmberTeks)
        StatusPengajuan.DI_FINANCE ->
            Triple(BiruLatar, BiruGaris, BiruTeks)
        StatusPengajuan.DISETUJUI_FINANCE, StatusPengajuan.DITERUSKAN_FINANCE,
        StatusPengajuan.DISERAHKAN_KE_CREW ->
            Triple(HijauLatar, HijauGaris, HijauTeks)
        // Satu-satunya status yang menuntut tindakan diberi warna merek, bukan
        // warna status, supaya terlihat berbeda dari kartu yang cuma perlu dibaca.
        StatusPengajuan.SIAP_DISERAHKAN ->
            Triple(SukaOrange, SukaOrange, Color.White)
        StatusPengajuan.SELESAI ->
            Triple(SukaBrown, SukaBrown, Color.White)
        StatusPengajuan.DITOLAK ->
            Triple(MerahLatar, MerahGaris, MerahTeks)
    }
    PilStatus(status.label, latar, garis, teks)
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
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White) {
            Column {
                Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Bukti Transfer",
                        Modifier.weight(1f),
                        color = SukaBrown,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                    )
                    IconButton(onClick = onTutup) {
                        Icon(Icons.Default.Close, "Tutup", tint = SukaBrown)
                    }
                }
                Box(
                    Modifier.fillMaxWidth().heightIn(min = 240.dp, max = 460.dp).background(SukaCream),
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
