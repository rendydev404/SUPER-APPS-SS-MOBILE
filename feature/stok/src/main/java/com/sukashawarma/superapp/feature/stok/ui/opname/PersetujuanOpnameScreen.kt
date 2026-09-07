package com.sukashawarma.superapp.feature.stok.ui.opname

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.RealtimeTables
import com.sukashawarma.superapp.feature.stok.data.model.OpnameHeader
import com.sukashawarma.superapp.feature.stok.ui.HeaderStok

private val ORANGE = Color(0xFFEA580C)
private val SLATE400 = Color(0xFF94A3B8)
private val SLATE500 = Color(0xFF64748B)
private val SLATE900 = Color(0xFF0F172A)
private val GARIS = Color(0xFFE2E8F0)
private val MERAH = Color(0xFFB91C1C)
private val MERAH_LATAR = Color(0xFFFEE2E2)
private val HIJAU = Color(0xFF15803D)

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

    Column(Modifier.fillMaxSize().background(Color(0xFFF8FAFC))) {
        HeaderStok(
            judul = "Persetujuan Opname",
            subjudul = if (state.memuat) "Memuat…" else "${state.antrean.size} menunggu keputusan",
            onKembali = onBack,
            aksi = {
                IconButton(onClick = viewModel::muatUlang) {
                    Icon(Icons.Default.Refresh, "Muat ulang", tint = Color.White)
                }
            },
        )

        val pesan = state.pesan ?: state.error
        if (pesan != null) {
            Surface(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                shape = RoundedCornerShape(12.dp),
                color = if (state.pesan != null) Color(0xFFDCFCE7) else MERAH_LATAR,
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        pesan,
                        Modifier.weight(1f),
                        color = if (state.pesan != null) HIJAU else MERAH,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "TUTUP",
                        Modifier.padding(start = 8.dp),
                        color = SLATE500,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }

        when {
            state.memuat -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ORANGE)
            }
            state.antrean.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Tidak ada opname yang menunggu",
                        color = SLATE900,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Opname hanya masuk antrean ini kalau kru mengajukannya karena ada selisih di luar toleransi.",
                        color = SLATE500,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
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
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, GARIS),
    ) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        opname.outletName ?: "Outlet",
                        color = SLATE900,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        listOfNotNull(opname.tanggal, opname.creatorName).joinToString(" · "),
                        color = SLATE500,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                if (opname.jumlahFlagged > 0) {
                    Surface(shape = RoundedCornerShape(50), color = MERAH_LATAR) {
                        Text(
                            "${opname.jumlahFlagged} DI LUAR TOLERANSI",
                            Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            color = MERAH,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                "${opname.jumlahItem} item dihitung",
                color = SLATE400,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
            )

            if (!state.bolehMemutuskan) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Anda bisa melihat antrean ini, tetapi keputusannya di tangan leader atau kantor pusat.",
                    color = SLATE500,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                )
            } else if (state.menolak == opname.id) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.alasanTolak,
                    onValueChange = viewModel::ubahAlasanTolak,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Alasan penolakan (wajib)", fontSize = 12.sp, color = SLATE400) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = MERAH,
                        unfocusedBorderColor = GARIS,
                    ),
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    OutlinedButton(
                        onClick = viewModel::tutupPenolakan,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                    ) { Text("Batal", fontSize = 12.5.sp, fontWeight = FontWeight.Bold) }
                    Button(
                        onClick = { viewModel.tolak(opname) },
                        enabled = !diproses,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MERAH),
                    ) {
                        Text(
                            if (diproses) "Memproses…" else "Tolak opname",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            } else {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.bukaPenolakan(opname.id) },
                        enabled = !diproses,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                    ) { Text("Tolak", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = MERAH) }
                    Button(
                        onClick = { viewModel.setujui(opname) },
                        enabled = !diproses,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ORANGE),
                    ) {
                        if (diproses) {
                            CircularProgressIndicator(
                                Modifier.size(15.dp),
                                strokeWidth = 2.dp,
                                color = Color.White,
                            )
                            Spacer(Modifier.width(7.dp))
                        }
                        Text(
                            if (diproses) "Memproses…" else "Setujui & finalisasi",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
