package com.sukashawarma.superapp.feature.stok.ui.mutasi

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.sukashawarma.superapp.feature.stok.data.model.Mutasi
import com.sukashawarma.superapp.feature.stok.data.model.StatusMutasi
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

@Composable
fun MutasiScreen(viewModel: MutasiViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()

    if (state.formTerbuka) {
        FormAjukan(state, viewModel)
    } else {
    Column(Modifier.fillMaxSize().background(SukaSurface)) {
        HeaderStok(judul = "Mutasi Antar Outlet", subjudul = "Kirim & terima transfer stok")

        if (!state.tidakBerhak && state.outlets.size > 1) {
            PemilihOutlet(state.outlets, state.outletTerpilih, viewModel::pilihOutlet)
        }

        state.pesan?.let { PitaPesan(it, false, viewModel::bersihkanPesan) }
        state.error?.let { PitaPesan(it, true, viewModel::bersihkanPesan) }

        if (!state.tidakBerhak) {
            Button(
                onClick = viewModel::bukaForm,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(7.dp))
                Text("Ajukan Mutasi", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        when {
            state.tidakBerhak -> KeadaanTidakBerhak("Akun Anda belum terhubung dengan outlet mana pun.")
            state.memuat -> MemuatPenuh()
            state.error != null && state.daftar.isEmpty() -> KeadaanGagal(state.error!!, viewModel::muatAwal)
            state.daftar.isEmpty() -> KeadaanKosong("Belum ada mutasi yang melibatkan outlet ini.")
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                items(state.daftar, key = { it.id }) { m ->
                    KartuMutasi(m) { viewModel.bukaDetail(m) }
                }
            }
        }
    }

    val target = state.detailUntuk
    if (target != null) LembarTindakan(state, target, viewModel)
    }
}

@Composable
private fun KartuMutasi(m: Mutasi, onKlik: () -> Unit) {
    val warna = when (m.status) {
        StatusMutasi.SELESAI -> Color(0xFF168451)
        StatusMutasi.DITOLAK -> Color(0xFFDC2626)
        StatusMutasi.DIKIRIM -> Color(0xFF2563EB)
        else -> Color(0xFFC27A12)
    }
    Surface(
        Modifier.fillMaxWidth().clickable(onClick = onKlik),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            m.outletAsalNama ?: "-",
                            color = SukaOnSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Icon(
                            Icons.Default.ArrowForward, null,
                            tint = Color(0xFFEA580C),
                            modifier = Modifier.size(14.dp).padding(horizontal = 2.dp),
                        )
                        Text(
                            m.outletTujuanNama ?: "-",
                            color = SukaOnSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }
                    Text(
                        "${waktuSingkat(m.createdAt)} · ${m.items.size} bahan · ${m.pembuatNama ?: "-"}",
                        color = SukaOnSurfaceVariant, fontSize = 10.sp,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = warna.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, warna.copy(alpha = 0.28f)),
                ) {
                    Text(
                        m.status.label,
                        Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        color = warna, fontSize = 9.sp, fontWeight = FontWeight.Black,
                    )
                }
            }
            if (!m.catatanPenolakan.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text("Alasan: ${m.catatanPenolakan}", color = Color(0xFFDC2626), fontSize = 10.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormAjukan(state: MutasiUiState, viewModel: MutasiViewModel) {
    var menuTujuan by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(SukaSurface)) {
        HeaderStok(
            judul = "Ajukan Mutasi",
            subjudul = "Kirim stok ke outlet lain",
            onKembali = viewModel::tutupForm,
        )
        state.pesan?.let { PitaPesan(it, false, viewModel::bersihkanPesan) }
        state.error?.let { PitaPesan(it, true, viewModel::bersihkanPesan) }

        if (state.memuatForm) {
            MemuatPenuh()
        } else {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Box {
                Surface(
                    onClick = { menuTujuan = true },
                    shape = RoundedCornerShape(13.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE7ECF2)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            state.tujuanTerpilih?.name ?: "Pilih outlet tujuan",
                            Modifier.weight(1f),
                            color = SukaOnSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        )
                        Icon(Icons.Default.ArrowDropDown, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                    }
                }
                DropdownMenu(expanded = menuTujuan, onDismissRequest = { menuTujuan = false }) {
                    state.outletTujuan.forEach { o ->
                        DropdownMenuItem(
                            text = { Text(o.name, fontSize = 14.sp) },
                            onClick = { menuTujuan = false; viewModel.pilihTujuan(o) },
                        )
                    }
                }
            }
            Spacer(Modifier.height(9.dp))
            OutlinedTextField(
                value = state.catatan,
                onValueChange = viewModel::ubahCatatan,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Catatan (opsional)", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
                singleLine = true,
                shape = RoundedCornerShape(13.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Color(0xFFF97316),
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                ),
            )
            Spacer(Modifier.height(9.dp))
            OutlinedTextField(
                value = state.cari,
                onValueChange = viewModel::ubahCari,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Cari bahan…", fontSize = 13.sp, color = Color(0xFF94A3B8)) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp)) },
                singleLine = true,
                shape = RoundedCornerShape(13.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Color(0xFFF97316),
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                ),
            )
        }

        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.bahanTampil, key = { it.bahanBakuId }) { b ->
                Surface(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                b.nama,
                                color = SukaOnSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                "Sisa ${formatAngkaStok(b.sisa)} ${formatSatuan(b.satuan)}",
                                color = SukaOnSurfaceVariant, fontSize = 10.sp,
                            )
                        }
                        OutlinedTextField(
                            value = state.jumlah[b.bahanBakuId].orEmpty(),
                            onValueChange = { viewModel.ubahJumlah(b.bahanBakuId, it) },
                            modifier = Modifier.width(88.dp),
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
                onClick = viewModel::ajukan,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                enabled = !state.memproses,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                shape = RoundedCornerShape(13.dp),
            ) {
                Text(
                    if (state.memproses) "Mengirim…" else "Ajukan (${state.itemDiajukan.size} bahan)",
                    fontSize = 14.sp, fontWeight = FontWeight.Bold,
                )
            }
        }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LembarTindakan(state: MutasiUiState, m: Mutasi, viewModel: MutasiViewModel) {
    val sheetState = rememberModalBottomSheetState()
    var alasan by remember { mutableStateOf("") }
    var kondisi by remember { mutableStateOf("baik") }

    ModalBottomSheet(onDismissRequest = viewModel::tutupDetail, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 28.dp)) {
            Text(
                "${m.outletAsalNama ?: "-"} → ${m.outletTujuanNama ?: "-"}",
                color = SukaOnSurface, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
            )
            Text(
                "${m.status.label} · ${waktuSingkat(m.createdAt)}",
                color = SukaOnSurfaceVariant, fontSize = 11.sp,
            )
            if (!m.catatan.isNullOrBlank()) {
                Text(m.catatan, color = SukaOnSurfaceVariant, fontSize = 11.sp)
            }
            Spacer(Modifier.height(12.dp))

            val bisaUbahQty = m.status == StatusMutasi.MENUNGGU_PENGIRIMAN || m.status == StatusMutasi.DIKIRIM

            m.items.forEach { item ->
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            item.namaBahan ?: "-",
                            color = SukaOnSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            buildString {
                                append("Diajukan ${formatAngkaStok(item.qtyDiajukan)}")
                                item.qtyDikirim?.let { append(" · dikirim ${formatAngkaStok(it)}") }
                                item.qtyDiterima?.let { append(" · diterima ${formatAngkaStok(it)}") }
                                append(" ${formatSatuan(item.satuan)}")
                            },
                            color = SukaOnSurfaceVariant, fontSize = 10.sp,
                        )
                    }
                    if (bisaUbahQty) {
                        OutlinedTextField(
                            value = state.qtyTindakan[item.id].orEmpty(),
                            onValueChange = { viewModel.ubahQtyTindakan(item.id, it) },
                            modifier = Modifier.width(88.dp),
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

            Spacer(Modifier.height(14.dp))

            when (m.status) {
                StatusMutasi.MENUNGGU_PERSETUJUAN -> {
                    OutlinedTextField(
                        value = alasan,
                        onValueChange = { alasan = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Alasan penolakan (wajib bila menolak)", fontSize = 12.sp) },
                        shape = RoundedCornerShape(12.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.setujui(false, alasan) },
                            modifier = Modifier.weight(1f),
                            enabled = !state.memproses,
                            shape = RoundedCornerShape(13.dp),
                        ) { Text("Tolak", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626)) }
                        Button(
                            onClick = { viewModel.setujui(true) },
                            modifier = Modifier.weight(1f),
                            enabled = !state.memproses,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF168451)),
                            shape = RoundedCornerShape(13.dp),
                        ) { Text("Setujui", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                    }
                }

                StatusMutasi.MENUNGGU_PENGIRIMAN -> {
                    OutlinedTextField(
                        value = state.kurir,
                        onValueChange = viewModel::ubahKurir,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Nama kurir / pengantar", fontSize = 12.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = viewModel::kirim,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.memproses,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                        shape = RoundedCornerShape(13.dp),
                    ) {
                        Text(
                            if (state.memproses) "Memproses…" else "Tandai Terkirim",
                            fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        )
                    }
                }

                StatusMutasi.DIKIRIM -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("baik", "rusak", "hilang").forEach { k ->
                            OutlinedButton(
                                onClick = { kondisi = k },
                                shape = RoundedCornerShape(50),
                            ) {
                                Text(
                                    k.replaceFirstChar { it.uppercase() },
                                    fontSize = 12.sp,
                                    fontWeight = if (kondisi == k) FontWeight.Black else FontWeight.Normal,
                                    color = if (kondisi == k) Color(0xFFEA580C) else SukaOnSurfaceVariant,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.terima(kondisi) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.memproses,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF168451)),
                        shape = RoundedCornerShape(13.dp),
                    ) {
                        Text(
                            if (state.memproses) "Memproses…" else "Terima Barang",
                            fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        )
                    }
                }

                else -> Text(
                    "Mutasi ini sudah selesai diproses.",
                    color = SukaOnSurfaceVariant, fontSize = 12.sp,
                )
            }
        }
    }
}
