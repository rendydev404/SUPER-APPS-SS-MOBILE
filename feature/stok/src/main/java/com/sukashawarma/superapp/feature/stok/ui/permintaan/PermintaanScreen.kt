package com.sukashawarma.superapp.feature.stok.ui.permintaan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.feature.stok.data.model.Permintaan
import com.sukashawarma.superapp.feature.stok.data.model.StatusPermintaan
import com.sukashawarma.superapp.feature.stok.domain.formatAngkaStok
import com.sukashawarma.superapp.feature.stok.domain.formatSatuan
import com.sukashawarma.superapp.feature.stok.ui.HeaderStok
import com.sukashawarma.superapp.feature.stok.ui.KeadaanGagal
import com.sukashawarma.superapp.feature.stok.ui.KeadaanKosong
import com.sukashawarma.superapp.feature.stok.ui.KeadaanTidakBerhak
import com.sukashawarma.superapp.feature.stok.ui.MemuatPenuh
import com.sukashawarma.superapp.feature.stok.ui.PemilihOutlet
import com.sukashawarma.superapp.feature.stok.ui.PitaPesan
import com.sukashawarma.superapp.feature.stok.ui.waktuSingkat
import com.sukashawarma.superapp.presentation.theme.SukaOnSurface
import com.sukashawarma.superapp.presentation.theme.SukaOnSurfaceVariant
import com.sukashawarma.superapp.presentation.theme.SukaSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermintaanScreen(viewModel: PermintaanViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()

    if (state.formTerbuka) {
        FormPengajuan(state, viewModel)
    } else {
    Column(Modifier.fillMaxSize().background(SukaSurface)) {
        HeaderStok(judul = "Permintaan Bahan", subjudul = "Ajukan & pantau permintaan ke gudang")

        if (!state.tidakBerhak && state.outlets.size > 1) {
            PemilihOutlet(state.outlets, state.outletTerpilih, viewModel::pilihOutlet)
        }

        state.pesan?.let { PitaPesan(it, false, viewModel::bersihkanPesan) }
        state.error?.let { PitaPesan(it, true, viewModel::bersihkanPesan) }

        if (state.bolehReview) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TabPermintaan.entries.forEach { t ->
                    FilterChip(
                        selected = state.tab == t,
                        onClick = { viewModel.pilihTab(t) },
                        label = { Text(t.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        shape = RoundedCornerShape(50),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFFEDD5),
                            selectedLabelColor = Color(0xFFC2410C),
                        ),
                    )
                }
            }
        }

        if (!state.tidakBerhak && state.tab == TabPermintaan.OUTLET) {
            Button(
                onClick = viewModel::bukaForm,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(7.dp))
                Text("Buat Permintaan", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        val daftar = if (state.tab == TabPermintaan.OUTLET) state.daftarOutlet else state.daftarReview

        when {
            state.tidakBerhak -> KeadaanTidakBerhak("Akun Anda belum terhubung dengan outlet mana pun.")
            state.memuat -> MemuatPenuh()
            state.error != null && daftar.isEmpty() -> KeadaanGagal(state.error!!, viewModel::muatAwal)
            daftar.isEmpty() -> KeadaanKosong(
                if (state.tab == TabPermintaan.OUTLET) "Belum ada permintaan dari outlet ini."
                else "Tidak ada permintaan yang menunggu review."
            )
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                items(daftar, key = { it.id }) { p ->
                    KartuPermintaan(
                        p = p,
                        bisaDitindak = state.tab == TabPermintaan.REVIEW &&
                            state.bolehApprove &&
                            p.status == StatusPermintaan.MENUNGGU,
                        onTindak = { viewModel.bukaApprove(p) },
                    )
                }
            }
        }
    }

    val target = state.approveUntuk
    if (target != null) {
        LembarPersetujuan(state, target, viewModel)
    }
    }
}

@Composable
private fun KartuPermintaan(p: Permintaan, bisaDitindak: Boolean, onTindak: () -> Unit) {
    val warna = when (p.status) {
        StatusPermintaan.DISETUJUI -> Color(0xFF168451)
        StatusPermintaan.MENUNGGU -> Color(0xFFC27A12)
        StatusPermintaan.DITOLAK -> Color(0xFFDC2626)
        StatusPermintaan.DIBATALKAN -> Color(0xFF64748B)
    }
    Surface(
        Modifier.fillMaxWidth().let { if (bisaDitindak) it.clickable(onClick = onTindak) else it },
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        p.outletName ?: "Outlet",
                        color = SukaOnSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${waktuSingkat(p.createdAt)} · ${p.pembuatNama ?: "-"}",
                        color = SukaOnSurfaceVariant, fontSize = 10.sp,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = warna.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, warna.copy(alpha = 0.28f)),
                ) {
                    Text(
                        p.status.label,
                        Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        color = warna, fontSize = 10.sp, fontWeight = FontWeight.Black,
                    )
                }
            }
            Spacer(Modifier.height(9.dp))
            p.items.take(4).forEach { item ->
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text(
                        item.namaBahan ?: "-",
                        Modifier.weight(1f),
                        color = SukaOnSurfaceVariant, fontSize = 11.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        buildString {
                            append(formatAngkaStok(item.qtyDiminta))
                            item.qtyDisetujui?.let { append(" → ${formatAngkaStok(it)}") }
                            append(" ${formatSatuan(item.satuan)}")
                        },
                        color = SukaOnSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    )
                }
            }
            if (p.items.size > 4) {
                Text("+${p.items.size - 4} bahan lain", color = Color(0xFF94A3B8), fontSize = 10.sp)
            }
            if (!p.alasanPenolakan.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text("Alasan: ${p.alasanPenolakan}", color = Color(0xFFDC2626), fontSize = 10.sp)
            }
            if (bisaDitindak) {
                Spacer(Modifier.height(9.dp))
                Text("Ketuk untuk memproses", color = Color(0xFFEA580C), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormPengajuan(state: PermintaanUiState, viewModel: PermintaanViewModel) {
    Column(Modifier.fillMaxSize().background(SukaSurface)) {
        HeaderStok(
            judul = "Buat Permintaan",
            subjudul = "Bahan yang stoknya menipis atau kritis",
            onKembali = viewModel::tutupForm,
        )
        state.pesan?.let { PitaPesan(it, false, viewModel::bersihkanPesan) }
        state.error?.let { PitaPesan(it, true, viewModel::bersihkanPesan) }

        when {
            state.memuatSaran -> MemuatPenuh()
            state.saran.isEmpty() -> KeadaanKosong(
                "Tidak ada bahan yang stoknya menipis di outlet ini, jadi belum perlu permintaan."
            )
            else -> {
                LazyColumn(
                    Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    items(state.saran, key = { it.bahanBakuId }) { s ->
                        Surface(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                        ) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        s.itemName,
                                        color = Color(0xFFEA580C), fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        "Sisa ${formatAngkaStok(s.currentQty)} · batas ${formatAngkaStok(s.threshold)} ${formatSatuan(s.satuan)}",
                                        color = SukaOnSurfaceVariant, fontSize = 10.sp,
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                OutlinedTextField(
                                    value = state.jumlah[s.bahanBakuId].orEmpty(),
                                    onValueChange = { viewModel.ubahJumlah(s.bahanBakuId, it) },
                                    modifier = Modifier.width(92.dp),
                                    placeholder = { Text("0", fontSize = 13.sp, color = Color(0xFFCBD5E1)) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    shape = RoundedCornerShape(11.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color(0xFFFBFCFE),
                                        focusedBorderColor = Color(0xFFF97316),
                                        unfocusedBorderColor = Color(0xFFE2E8F0),
                                    ),
                                )
                            }
                        }
                    }
                }
                Surface(color = Color.White, shadowElevation = 8.dp) {
                    Button(
                        onClick = viewModel::kirimPermintaan,
                        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                        enabled = !state.mengirim,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                        shape = RoundedCornerShape(13.dp),
                    ) {
                        Text(
                            if (state.mengirim) "Mengirim…"
                            else "Kirim Permintaan (${state.terpilihUntukDiminta.size} bahan)",
                            fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LembarPersetujuan(
    state: PermintaanUiState,
    p: Permintaan,
    viewModel: PermintaanViewModel,
) {
    val sheetState = rememberModalBottomSheetState()
    var alasan by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = viewModel::tutupApprove, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 28.dp)) {
            Text(
                "Proses permintaan",
                color = SukaOnSurface, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold,
            )
            Text(
                "${p.outletName ?: "Outlet"} · ${waktuSingkat(p.createdAt)}",
                color = SukaOnSurfaceVariant, fontSize = 11.sp,
            )
            Spacer(Modifier.height(14.dp))

            p.items.forEach { item ->
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            item.namaBahan ?: "-",
                            color = SukaOnSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "Diminta ${formatAngkaStok(item.qtyDiminta)} ${formatSatuan(item.satuan)}",
                            color = SukaOnSurfaceVariant, fontSize = 10.sp,
                        )
                    }
                    OutlinedTextField(
                        value = state.qtySetuju[item.bahanBakuId].orEmpty(),
                        onValueChange = { viewModel.ubahQtySetuju(item.bahanBakuId, it) },
                        modifier = Modifier.width(92.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(11.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color(0xFFFBFCFE),
                            focusedBorderColor = Color(0xFFF97316),
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                        ),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = alasan,
                onValueChange = { alasan = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Alasan penolakan (wajib bila menolak)", fontSize = 12.sp) },
                singleLine = false,
                shape = RoundedCornerShape(12.dp),
            )

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { viewModel.tolak(alasan) },
                    modifier = Modifier.weight(1f),
                    enabled = !state.mengirim,
                    shape = RoundedCornerShape(13.dp),
                ) { Text("Tolak", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626)) }

                Button(
                    onClick = viewModel::setujui,
                    modifier = Modifier.weight(1f),
                    enabled = !state.mengirim,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF168451)),
                    shape = RoundedCornerShape(13.dp),
                ) { Text(if (state.mengirim) "Memproses…" else "Setujui", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}
