package com.sukashawarma.superapp.presentation.absensi.kasbon

import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.core.ui.ios.BilahJudulIos
import com.sukashawarma.superapp.core.ui.ios.JudulSeksiIos
import com.sukashawarma.superapp.core.ui.ios.KartuIos
import com.sukashawarma.superapp.core.ui.ios.KeadaanIos
import com.sukashawarma.superapp.core.ui.ios.LencanaIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.SegmenIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.TombolUtamaIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WadahSegmenIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.ios.bayanganIos
import com.sukashawarma.superapp.core.ui.ios.permukaanIos
import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.presentation.absensi.AbsensiShell
import com.sukashawarma.superapp.presentation.absensi.nadaStatusPengajuan
import com.sukashawarma.superapp.core.ui.kaca.denganRuangNav
import java.text.NumberFormat
import java.util.Locale
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.RealtimeTables

private val rupiahFmt = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 }
private fun fmtRupiah(v: Double) = rupiahFmt.format(v)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KasbonScreen(
    onExit: () -> Unit,
    onNavigateTab: (Int) -> Unit = {},
    viewModel: KasbonViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    RealtimeRefresh(RealtimeTables.CASH_ADVANCES) { viewModel.refresh() }
    // Pesan ini muncul setelah formulir tertutup, jadi ditampilkan sebagai toast alih-alih
    // di dalam formulir — yang sudah tidak ada lagi di layar saat pengajuan masuk antrean.
    val konteksAntrean = LocalContext.current
    LaunchedEffect(state.pesanAntrean) {
        val pesan = state.pesanAntrean ?: return@LaunchedEffect
        Toast.makeText(konteksAntrean, pesan, Toast.LENGTH_LONG).show()
        viewModel.clearPesanAntrean()
    }

    var showForm by remember { mutableStateOf(false) }

    if (showForm) {
        var wasSubmitting by remember { mutableStateOf(false) }
        LaunchedEffect(state.submitting) {
            if (wasSubmitting && !state.submitting && state.submitError == null) showForm = false
            wasSubmitting = state.submitting
        }
        // Halaman penuh "Request Kasbon" mengikuti desain Stitch (project
        // 16991912726833518585, screen 057dfb051d6248b0a59696210f240291) — bukan dialog.
        KasbonFormScreen(
            submitting = state.submitting,
            error = state.submitError,
            onBack = { showForm = false; viewModel.clearSubmitError() },
            onSubmit = { amount, months, reason -> viewModel.submit(amount, months, reason) },
        )
    } else {
        KasbonListScreen(
            state = state,
            onExit = onExit,
            onNavigateTab = onNavigateTab,
            onAddClick = { showForm = true },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KasbonListScreen(
    state: KasbonUiState,
    onExit: () -> Unit,
    onNavigateTab: (Int) -> Unit,
    onAddClick: () -> Unit,
) {
    // Sama seperti Cuti & Izin — Kasbon diakses dari tab "More" (index 3), bilah tab
    // tetap tampil di sini supaya user bisa lompat tab tanpa balik dulu. Halaman formulir
    // "Request Kasbon" sengaja di luar kerangka ini: tombol kirimnya dipatok di dasar layar.
    AbsensiShell(selectedIndex = 3, onSelect = onNavigateTab) {
        Scaffold(
            containerColor = WarnaIos.Latar,
            topBar = { BilahJudulIos(judul = "Kasbon", onKembali = onExit, garisBawah = false) },
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize().background(WarnaIos.Latar)) {
                Column(Modifier.padding(horizontal = UkuranIos.TepiLayar).padding(top = 4.dp, bottom = 4.dp)) {
                    Text("Kasbon", style = TipeIos.Judul1, modifier = Modifier.padding(horizontal = 4.dp))

                    Spacer(Modifier.height(16.dp))

                    TombolUtamaIos(teks = "Ajukan Kasbon", onKlik = onAddClick, ikon = IkonIos.Add)

                    JudulSeksiIos("Riwayat Kasbon", modifier = Modifier.padding(top = 6.dp, bottom = 8.dp))
                }

                Box(Modifier.fillMaxSize()) {
                    when {
                        state.loading -> CircularProgressIndicator(
                            Modifier.align(Alignment.Center).size(28.dp),
                            color = WarnaIos.Aksen,
                            strokeWidth = 2.5.dp,
                        )
                        state.error != null -> KeadaanIos(
                            ikon = IkonIos.ErrorOutline,
                            judul = "Gagal memuat",
                            pesan = state.error ?: "",
                            nada = NadaIos.BAHAYA,
                            modifier = Modifier.align(Alignment.TopCenter),
                        )
                        state.rows.isEmpty() -> KeadaanIos(
                            ikon = IkonIos.AccountBalanceWallet,
                            judul = "Belum ada pengajuan",
                            pesan = "Belum ada pengajuan kasbon.",
                            modifier = Modifier.align(Alignment.TopCenter),
                        )
                        else -> LazyColumn(
                            contentPadding = PaddingValues(bottom = 24.dp, start = UkuranIos.TepiLayar, end = UkuranIos.TepiLayar, top = 4.dp).denganRuangNav(),
                            verticalArrangement = Arrangement.spacedBy(UkuranIos.JarakKartu),
                        ) {
                            items(state.rows, key = { it.id }) { row -> KasbonItem(row) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KasbonItem(row: KasbonRow) {
    val label = when (row.status) {
        "approved" -> "Disetujui"
        "rejected" -> "Ditolak"
        else -> "Menunggu"
    }
    KartuIos {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(Modifier.weight(1f)) {
                Text(fmtRupiah(row.amount), style = TipeIos.Angka)
                Spacer(Modifier.height(3.dp))
                Text("Sisa ${fmtRupiah(row.remaining)} · ${row.installmentMonths} bulan gaji", style = TipeIos.Catatan)
                if (row.reason.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(row.reason, style = TipeIos.SubJudul)
                }
            }
            Spacer(Modifier.width(12.dp))
            LencanaIos(label, nadaStatusPengajuan(row.status))
        }
    }
}

private val KASBON_TENORS = listOf(1 to "1 bulan gaji", 2 to "2 bulan gaji", 3 to "3 bulan gaji")

/**
 * Halaman "Request Kasbon" — mengikuti desain Stitch persis: top bar judul di tengah warna
 * primary + tombol back, field jumlah dengan prefix "Rp", pill tenor 3 pilihan, textarea
 * alasan, tombol submit pill penuh melekat di bawah.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KasbonFormScreen(
    submitting: Boolean,
    error: String?,
    onBack: () -> Unit,
    onSubmit: (Double, Int, String) -> Unit,
) {
    var amountText by remember { mutableStateOf("") }
    var months by remember { mutableStateOf(1) }
    var reason by remember { mutableStateOf("") }

    Scaffold(
        containerColor = WarnaIos.Latar,
        topBar = { BilahJudulIos(judul = "Request Kasbon", onKembali = onBack) },
        bottomBar = {
            Surface(color = WarnaIos.Latar, shadowElevation = 0.dp) {
                Column(Modifier.padding(horizontal = UkuranIos.TepiLayar, vertical = 12.dp)) {
                    TombolUtamaIos(
                        teks = if (submitting) "Mengirim..." else "Ajukan Kasbon",
                        onKlik = { amountText.toDoubleOrNull()?.let { onSubmit(it, months, reason) } },
                        aktif = !submitting && amountText.toDoubleOrNull() != null && amountText.toDoubleOrNull()!! > 0,
                    )
                }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .background(WarnaIos.Latar)
                .padding(horizontal = UkuranIos.TepiLayar, vertical = 16.dp)
        ) {
            // Jumlah
            LabelBidang("Jumlah (Rp)")
            Spacer(Modifier.height(7.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(UkuranIos.TinggiTombol + 6.dp)
                    .permukaanIos(UkuranIos.SudutGrup)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Rp", style = TipeIos.Isi.copy(color = WarnaIos.LabelKedua, fontWeight = FontWeight.Medium))
                Spacer(Modifier.width(8.dp))
                BasicAmountField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() } },
                )
            }

            Spacer(Modifier.height(22.dp))

            // Tenor
            LabelBidang("Pilih Tenor")
            Spacer(Modifier.height(7.dp))
            WadahSegmenIos {
                KASBON_TENORS.forEach { (m, label) ->
                    SegmenIos(
                        label = label,
                        aktif = months == m,
                        onKlik = { months = m },
                        modifier = Modifier.weight(1f),
                        jarakSisi = 6.dp,
                    )
                }
            }

            Spacer(Modifier.height(22.dp))

            // Alasan
            LabelBidang("Alasan Pengajuan")
            Spacer(Modifier.height(7.dp))
            TextField(
                value = reason,
                onValueChange = { reason = it },
                placeholder = { Text("Tulis alasan Anda di sini...", color = WarnaIos.Abu) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp)
                    .bayanganIos(UkuranIos.SudutGrup),
                shape = UkuranIos.SudutGrup,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = WarnaIos.Kartu,
                    unfocusedContainerColor = WarnaIos.Kartu,
                    disabledContainerColor = WarnaIos.Kartu,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    cursorColor = WarnaIos.Aksen,
                    focusedTextColor = WarnaIos.Label,
                    unfocusedTextColor = WarnaIos.Label,
                ),
            )

            if (error != null) {
                Spacer(Modifier.height(12.dp))
                Text(error, style = TipeIos.Catatan.copy(color = NadaIos.BAHAYA.teks))
            }
        }
    }
}

@Composable
private fun LabelBidang(teks: String) {
    Text(
        teks,
        style = TipeIos.Catatan.copy(color = WarnaIos.Label, fontWeight = FontWeight.SemiBold),
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

@Composable
private fun BasicAmountField(value: String, onValueChange: (String) -> Unit) {
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        textStyle = TipeIos.Angka,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(WarnaIos.Aksen),
        decorationBox = { inner ->
            if (value.isEmpty()) Text("0", style = TipeIos.Angka.copy(color = WarnaIos.LabelKetiga))
            inner()
        },
    )
}
