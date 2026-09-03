package com.sukashawarma.superapp.feature.stok.ui.detail

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.feature.stok.data.model.LedgerEntry
import com.sukashawarma.superapp.feature.stok.data.model.MonitoringRow
import com.sukashawarma.superapp.feature.stok.domain.UnitScale
import com.sukashawarma.superapp.feature.stok.domain.toLongString
import com.sukashawarma.superapp.feature.stok.ui.KeadaanGagal
import com.sukashawarma.superapp.feature.stok.ui.KeadaanKosong
import com.sukashawarma.superapp.feature.stok.ui.MemuatPenuh
import com.sukashawarma.superapp.feature.stok.ui.StatusBadge
import com.sukashawarma.superapp.presentation.theme.SukaOnSurface
import com.sukashawarma.superapp.presentation.theme.SukaOnSurfaceVariant
import com.sukashawarma.superapp.presentation.theme.SukaSurface

@Composable
fun DetailBahanScreen(
    outletId: String,
    bahanId: String,
    namaAwal: String,
    onKeluar: () -> Unit,
    viewModel: DetailBahanViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(outletId, bahanId) { viewModel.muat(outletId, bahanId) }

    Column(Modifier.fillMaxSize().background(SukaSurface)) {
        Box(
            Modifier.fillMaxWidth().background(
                Brush.verticalGradient(listOf(Color(0xFFEA580C), Color(0xFFF97316)))
            )
        ) {
            Row(
                Modifier.statusBarsPadding().padding(start = 8.dp, end = 16.dp, top = 6.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onKeluar) {
                    Icon(Icons.Default.ArrowBack, "Kembali", tint = Color.White)
                }
                Text(
                    state.baris?.itemName ?: namaAwal,
                    Modifier.weight(1f),
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        when {
            state.memuat -> MemuatPenuh()
            state.error != null -> KeadaanGagal(state.error!!, viewModel::cobaLagi)
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                state.baris?.let { row ->
                    item(key = "ringkasan") { KartuRingkasan(row) }
                    item(key = "satuan") { KartuSatuan(row) }
                }
                item(key = "judul-mutasi") {
                    Text(
                        "Riwayat mutasi",
                        color = SukaOnSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                if (state.mutasi.isEmpty()) {
                    item(key = "mutasi-kosong") {
                        KeadaanKosong("Belum ada mutasi tercatat untuk bahan ini di outlet ini.")
                    }
                } else {
                    items(state.mutasi, key = { it.id }) { BarisMutasi(it) }
                }
            }
        }
    }
}

@Composable
private fun KartuRingkasan(row: MonitoringRow) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Saldo saat ini", Modifier.weight(1f), color = SukaOnSurfaceVariant, fontSize = 12.sp)
                StatusBadge(row.status())
            }
            Spacer(Modifier.height(8.dp))
            Text(row.saldoTampil, color = SukaOnSurface, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(Modifier.height(12.dp))
            BarisInfo("Outlet", row.outletName)
            row.kategori?.let { BarisInfo("Kategori", it) }
            BarisInfo(
                "Batas aman",
                row.thresholdNorm?.let { UnitScale.formatBerjenjang(it, row.meta) ?: it.toLongString() }
                    ?: "belum diatur",
            )
            BarisInfo("Opname terakhir", row.lastOpnameDate ?: "belum pernah")
        }
    }
}

/**
 * Rincian satuan sengaja ditampilkan lengkap. Status di aplikasi ini dihitung ulang
 * pada satuan terkecil, sehingga bisa berbeda dari tampilan web untuk bahan yang
 * faktor konversinya bukan 1. Dengan angka dan faktornya terlihat, selisih itu dapat
 * ditelusuri, bukan menjadi perdebatan tanpa dasar.
 */
@Composable
private fun KartuSatuan(row: MonitoringRow) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("Satuan & konversi", color = SukaOnSurface, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(10.dp))
            BarisInfo("Satuan besar", row.meta.satuan ?: "-")
            BarisInfo("Satuan tengah", row.meta.satuanTengah ?: "-")
            BarisInfo("Satuan kecil", row.meta.satuanKecil ?: "-")
            BarisInfo(
                "1 ${row.meta.satuan ?: "besar"}",
                row.meta.faktorTampilan?.let { "${it.toLongString()} ${row.meta.satuanKecil ?: "kecil"}" } ?: "-",
            )
            BarisInfo("Saldo tersimpan sebagai", if (row.saldoIsGram) "satuan terkecil" else "satuan besar")
            BarisInfo("Angka mentah di database", row.currentQty.toLongString())
            if (row.saldoNorm == null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "Faktor konversi bahan ini belum lengkap, sehingga status tidak dapat " +
                        "dihitung dengan pasti. Lengkapi data satuan bahan di aplikasi web.",
                    color = Color(0xFFC2410C),
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFF7ED), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                )
            }
        }
    }
}

@Composable
private fun BarisInfo(label: String, nilai: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, Modifier.weight(1f), color = SukaOnSurfaceVariant, fontSize = 12.sp)
        Text(
            nilai,
            color = SukaOnSurface,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun BarisMutasi(entry: LedgerEntry) {
    val warna = if (entry.menambah) Color(0xFF168451) else Color(0xFFDC2626)
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(36.dp).background(warna.copy(alpha = 0.10f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (entry.menambah) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    null, tint = warna, modifier = Modifier.size(17.dp),
                )
            }
            Spacer(Modifier.height(0.dp))
            Column(Modifier.weight(1f).padding(start = 11.dp)) {
                Text(entry.tipeLabel, color = SukaOnSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                entry.createdAt?.let {
                    Text(it.take(16).replace('T', ' '), color = Color(0xFF94A3B8), fontSize = 10.sp)
                }
                entry.catatan?.takeIf { it.isNotBlank() }?.let {
                    Text(it, color = SukaOnSurfaceVariant, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    (if (entry.menambah) "+" else "") + entry.qty.toLongString(),
                    color = warna, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
                )
                if (entry.saldoSebelum != null && entry.saldoSesudah != null) {
                    Text(
                        "${entry.saldoSebelum.toLongString()} → ${entry.saldoSesudah.toLongString()}",
                        color = Color(0xFF94A3B8), fontSize = 10.sp,
                    )
                }
            }
        }
    }
}
