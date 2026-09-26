package com.sukashawarma.superapp.feature.stok.ui.detail

import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.core.ui.ios.BarisIos
import com.sukashawarma.superapp.core.ui.ios.GrupIos
import com.sukashawarma.superapp.core.ui.ios.JudulSeksiIos
import com.sukashawarma.superapp.core.ui.ios.KartuIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.PemisahIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.TombolBundarIos
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.feature.stok.data.model.LedgerEntry
import com.sukashawarma.superapp.feature.stok.data.model.MonitoringRow
import com.sukashawarma.superapp.feature.stok.domain.UnitScale
import com.sukashawarma.superapp.feature.stok.domain.toLongString
import com.sukashawarma.superapp.feature.stok.ui.BannerIos
import com.sukashawarma.superapp.feature.stok.ui.HeaderStok
import com.sukashawarma.superapp.feature.stok.ui.KeadaanGagal
import com.sukashawarma.superapp.feature.stok.ui.KeadaanKosong
import com.sukashawarma.superapp.feature.stok.ui.MemuatPenuh
import com.sukashawarma.superapp.feature.stok.ui.StatusBadge
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.RealtimeTables

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
    RealtimeRefresh(RealtimeTables.STOK_BALANCE, jedaMinimumMs = 15_000L) { viewModel.muat(outletId, bahanId) }

    Column(Modifier.fillMaxSize().background(WarnaIos.Latar)) {
        HeaderStok(
            judul = state.baris?.itemName ?: namaAwal,
            subjudul = "Detail saldo & riwayat mutasi",
            onKembali = onKeluar,
        ) {
            TombolBundarIos(IkonIos.Refresh, "Segarkan", viewModel::cobaLagi)
        }

        when {
            state.memuat && state.baris == null -> MemuatPenuh()
            state.error != null -> KeadaanGagal(state.error!!, viewModel::cobaLagi)
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = UkuranIos.TepiLayar, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(UkuranIos.JarakKartu),
            ) {
                state.baris?.let { row ->
                    item(key = "ringkasan") { KartuRingkasan(row) }
                    item(key = "satuan") { KartuSatuan(row) }
                }
                item(key = "judul-mutasi") {
                    JudulSeksiIos("Riwayat mutasi")
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
    KartuIos {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Saldo saat ini", Modifier.weight(1f), style = TipeIos.SubJudul)
            StatusBadge(row.status())
        }
        Spacer(Modifier.height(4.dp))
        Text(row.saldoTampil, style = TipeIos.AngkaBesar.copy(fontSize = 30.sp))
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(WarnaIos.Pemisah))
        Spacer(Modifier.height(8.dp))
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

/**
 * Rincian satuan sengaja ditampilkan lengkap. Status di aplikasi ini dihitung ulang
 * pada satuan terkecil, sehingga bisa berbeda dari tampilan web untuk bahan yang
 * faktor konversinya bukan 1. Dengan angka dan faktornya terlihat, selisih itu dapat
 * ditelusuri, bukan menjadi perdebatan tanpa dasar.
 */
@Composable
private fun KartuSatuan(row: MonitoringRow) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GrupIos(judul = "Satuan & konversi") {
            BarisIos("Satuan besar", nilai = row.meta.satuan ?: "-")
            PemisahIos()
            BarisIos("Satuan tengah", nilai = row.meta.satuanTengah ?: "-")
            PemisahIos()
            BarisIos("Satuan kecil", nilai = row.meta.satuanKecil ?: "-")
            PemisahIos()
            BarisIos(
                "1 ${row.meta.satuan ?: "besar"}",
                nilai = row.meta.faktorTampilan?.let { "${it.toLongString()} ${row.meta.satuanKecil ?: "kecil"}" } ?: "-",
            )
            PemisahIos()
            BarisIos("Saldo tersimpan sebagai", nilai = if (row.saldoIsGram) "satuan terkecil" else "satuan besar")
            PemisahIos()
            BarisIos("Angka mentah di database", nilai = row.currentQty.toLongString())
        }
        if (row.saldoNorm == null) {
            BannerIos(
                "Faktor konversi bahan ini belum lengkap, sehingga status tidak dapat " +
                    "dihitung dengan pasti. Lengkapi data satuan bahan di aplikasi web.",
                NadaIos.PERINGATAN,
                ikon = IkonIos.WarningAmber,
            )
        }
    }
}

@Composable
private fun BarisInfo(label: String, nilai: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(label, style = TipeIos.SubJudul)
        Spacer(Modifier.width(12.dp))
        Text(
            nilai,
            Modifier.weight(1f),
            style = TipeIos.Keterangan.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium),
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun BarisMutasi(entry: LedgerEntry) {
    val nada = if (entry.menambah) NadaIos.SUKSES else NadaIos.BAHAYA
    KartuIos(padding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(nada.warna.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (entry.menambah) IkonIos.ArrowUpward else IkonIos.ArrowDownward,
                    null, tint = nada.warna, modifier = Modifier.size(17.dp),
                )
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(entry.tipeLabel, style = TipeIos.Keterangan.copy(fontWeight = FontWeight.SemiBold))
                entry.createdAt?.let {
                    Text(it.take(16).replace('T', ' '), style = TipeIos.Kecil)
                }
                entry.catatan?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = TipeIos.Catatan, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    (if (entry.menambah) "+" else "") + entry.qty.toLongString(),
                    color = nada.teks, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                )
                if (entry.saldoSebelum != null && entry.saldoSesudah != null) {
                    Text(
                        "${entry.saldoSebelum.toLongString()} → ${entry.saldoSesudah.toLongString()}",
                        style = TipeIos.Kecil,
                    )
                }
            }
        }
    }
}
