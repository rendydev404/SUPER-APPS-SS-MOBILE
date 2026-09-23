package com.sukashawarma.superapp.feature.stok.ui.opname

import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.core.ui.kaca.denganRuangNav
import com.sukashawarma.superapp.core.ui.ios.KartuIos
import com.sukashawarma.superapp.core.ui.ios.KeadaanIos
import com.sukashawarma.superapp.core.ui.ios.LencanaIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.TombolBundarIos
import com.sukashawarma.superapp.core.ui.ios.TombolKeduaIos
import com.sukashawarma.superapp.core.ui.ios.TombolUtamaIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.RealtimeTables
import com.sukashawarma.superapp.feature.stok.data.model.OpnameHeader
import com.sukashawarma.superapp.feature.stok.ui.HeaderStok

/**
 * Persetujuan opname — cermin `app/stok/opname-approval/page.tsx` web.
 *
 * Yang masuk sini hanya opname berstatus `pending_approval`, yaitu opname yang
 * kru ajukan karena ada item di luar toleransi. Menyetujui berarti sekaligus
 * memfinalisasi: `approve_opname` menulis selisihnya ke ledger.
 */
@Composable
fun PersetujuanOpnameScreen(
    onBack: () -> Unit,
    viewModel: PersetujuanOpnameViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()

    RealtimeRefresh(RealtimeTables.OPNAME) { viewModel.muatUlang() }

    Column(Modifier.fillMaxSize().background(WarnaIos.Latar)) {
        HeaderStok(
            judul = "Persetujuan Opname",
            subjudul = if (state.memuat) "Memuat…" else "${state.antrean.size} menunggu keputusan",
            onKembali = onBack,
            aksi = {
                TombolBundarIos(IkonIos.Refresh, "Muat ulang", viewModel::muatUlang)
            },
        )

        val pesan = state.pesan ?: state.error
        if (pesan != null) {
            val nada = if (state.pesan != null) NadaIos.SUKSES else NadaIos.BAHAYA
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = UkuranIos.TepiLayar, vertical = 10.dp)
                    .background(nada.warna.copy(alpha = 0.12f), UkuranIos.SudutKontrol)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    pesan,
                    Modifier.weight(1f),
                    style = TipeIos.Catatan.copy(color = nada.teks, fontWeight = FontWeight.SemiBold),
                )
                Text(
                    "Tutup",
                    Modifier.padding(start = 8.dp),
                    style = TipeIos.Catatan.copy(fontWeight = FontWeight.SemiBold),
                )
            }
        }

        when {
            state.memuat -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = WarnaIos.Aksen)
            }
            state.antrean.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                KeadaanIos(
                    ikon = IkonIos.FactCheck,
                    judul = "Tidak ada opname yang menunggu",
                    pesan = "Opname hanya masuk antrean ini kalau kru mengajukannya karena ada selisih di luar toleransi.",
                )
            }
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(UkuranIos.TepiLayar).denganRuangNav(),
                verticalArrangement = Arrangement.spacedBy(UkuranIos.JarakKartu),
            ) {
                items(state.antrean, key = { it.id }) { opname ->
                    KartuAntreanOpname(opname, state, viewModel)
                }
            }
        }
    }
}

@Composable
private fun KartuAntreanOpname(
    opname: OpnameHeader,
    state: PersetujuanOpnameUiState,
    viewModel: PersetujuanOpnameViewModel,
) {
    val diproses = state.sedangDiproses == opname.id
    KartuIos {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    opname.outletName ?: "Outlet",
                    style = TipeIos.Utama,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(opname.tanggal, opname.creatorName).joinToString(" · "),
                    style = TipeIos.Catatan,
                )
            }
            if (opname.jumlahFlagged > 0) {
                Spacer(Modifier.width(8.dp))
                LencanaIos("${opname.jumlahFlagged} di luar toleransi", NadaIos.BAHAYA)
            }
        }

        Spacer(Modifier.height(10.dp))
        Text("${opname.jumlahItem} item dihitung", style = TipeIos.Catatan)

        if (!state.bolehMemutuskan) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Anda bisa melihat antrean ini, tetapi keputusannya di tangan leader atau kantor pusat.",
                style = TipeIos.Catatan,
            )
        } else if (state.menolak == opname.id) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.alasanTolak,
                onValueChange = viewModel::ubahAlasanTolak,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Alasan penolakan (wajib)", style = TipeIos.SubJudul.copy(color = WarnaIos.Abu)) },
                shape = UkuranIos.SudutKontrol,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = WarnaIos.Kartu,
                    unfocusedContainerColor = WarnaIos.Kartu,
                    // Merah, bukan aksen: kolom ini hanya muncul saat hendak menolak.
                    focusedBorderColor = WarnaIos.Merah,
                    unfocusedBorderColor = WarnaIos.Pemisah,
                    cursorColor = WarnaIos.Merah,
                ),
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TombolKeduaIos(
                    "Batal",
                    viewModel::tutupPenolakan,
                    Modifier.weight(1f),
                    warna = WarnaIos.AbuGelap,
                )
                TombolUtamaIos(
                    if (diproses) "Memproses…" else "Tolak opname",
                    { viewModel.tolak(opname) },
                    Modifier.weight(1f),
                    aktif = !diproses,
                    warna = WarnaIos.Merah,
                )
            }
        } else {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TombolKeduaIos(
                    "Tolak",
                    { viewModel.bukaPenolakan(opname.id) },
                    Modifier.weight(1f),
                    aktif = !diproses,
                    warna = WarnaIos.Merah,
                )
                // Porsi lebih lebar: label 17sp tombol iOS tidak muat di setengah kartu.
                TombolUtamaIos(
                    "Setujui & finalisasi",
                    { viewModel.setujui(opname) },
                    Modifier.weight(1.7f),
                    memuat = diproses,
                )
            }
        }
    }
}
