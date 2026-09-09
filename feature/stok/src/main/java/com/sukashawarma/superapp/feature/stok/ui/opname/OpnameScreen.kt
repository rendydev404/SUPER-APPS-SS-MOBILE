package com.sukashawarma.superapp.feature.stok.ui.opname

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.feature.stok.data.model.OpnameHeader
import com.sukashawarma.superapp.feature.stok.data.model.OpnameItemRow
import com.sukashawarma.superapp.feature.stok.data.model.StatusOpname
import com.sukashawarma.superapp.feature.stok.domain.Selisih
import com.sukashawarma.superapp.feature.stok.domain.formatAngkaStok
import com.sukashawarma.superapp.feature.stok.domain.formatSatuan
import com.sukashawarma.superapp.feature.stok.ui.HeaderStok
import com.sukashawarma.superapp.feature.stok.ui.KeadaanGagal
import com.sukashawarma.superapp.feature.stok.ui.KeadaanKosong
import com.sukashawarma.superapp.feature.stok.ui.KeadaanTidakBerhak
import com.sukashawarma.superapp.feature.stok.ui.MemuatPenuh
import com.sukashawarma.superapp.feature.stok.ui.PemilihOutlet
import com.sukashawarma.superapp.feature.stok.ui.PitaPesan
import com.sukashawarma.superapp.feature.stok.ui.tanggalSingkat
import com.sukashawarma.superapp.presentation.theme.SukaOnSurface
import com.sukashawarma.superapp.presentation.theme.SukaOnSurfaceVariant
import com.sukashawarma.superapp.presentation.theme.SukaSurface
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.RealtimeTables

@Composable
fun OpnameScreen(viewModel: OpnameViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    RealtimeRefresh(RealtimeTables.OPNAME, RealtimeTables.OPNAME_ITEM) { viewModel.muatAwal() }

    // Detail ditampilkan menukar isi layar, bukan lewat rute tersendiri: OpnameScreen
    // dirender langsung oleh StokShell sebagai tab, sehingga menambah rute berarti
    // menjahit callback menembus dua lapis untuk keuntungan yang tidak ada.
    var detailId by rememberSaveable { mutableStateOf<String?>(null) }
    BackHandler(enabled = detailId != null) { detailId = null }

    val detailTerbuka = detailId
    if (detailTerbuka != null) {
        DetailOpnameScreen(opnameId = detailTerbuka, onKembali = { detailId = null })
    } else if (state.formTerbuka) {
        FormOpname(state, viewModel)
    } else {
    Column(Modifier.fillMaxSize().background(SukaSurface)) {
        HeaderStok(judul = "Stock Opname", subjudul = "Hitung fisik & rekonsiliasi stok")

        if (!state.tidakBerhak && state.outlets.size > 1) {
            PemilihOutlet(state.outlets, state.outletTerpilih, viewModel::pilihOutlet)
        }

        state.pesan?.let { PitaPesan(it, false, viewModel::bersihkanPesan) }

        if (!state.tidakBerhak) {
            if (state.crewSudahOpname) {
                // Crew yang sudah opname hari ini: tampilkan pesan, bukan tombol.
                Surface(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF94A3B8).copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, Color(0xFF94A3B8).copy(alpha = 0.25f)),
                ) {
                    Row(
                        Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            Icons.Default.CheckCircle, null,
                            tint = Color(0xFF168451),
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Opname hari ini sudah selesai",
                            color = Color(0xFF64748B),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            } else {
                val adaDraft = state.riwayat.any { it.status == StatusOpname.DRAFT }
                val draftHeader = state.riwayat.firstOrNull { it.status == StatusOpname.DRAFT }
                Button(
                    onClick = viewModel::bukaForm,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (adaDraft) Color(0xFFD97706) else Color(0xFFEA580C),
                    ),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(
                        if (adaDraft) Icons.Default.EditNote else Icons.Default.Add,
                        null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(7.dp))
                    Column {
                        Text(
                            if (adaDraft) "Lanjutkan Draft" else "Mulai Opname Hari Ini",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        )
                        if (adaDraft && draftHeader != null && draftHeader.jumlahItem > 0) {
                            Text(
                                "${draftHeader.jumlahItem} item tersimpan",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.80f),
                            )
                        }
                    }
                }
            }
        }

        when {
            state.tidakBerhak -> KeadaanTidakBerhak("Akun Anda belum terhubung dengan outlet mana pun.")
            state.memuat && state.outlets.isEmpty() -> MemuatPenuh()
            state.error != null -> KeadaanGagal(state.error!!, viewModel::muatAwal)
            state.riwayat.isEmpty() -> KeadaanKosong("Belum ada riwayat opname di outlet ini.")
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                items(state.riwayat, key = { it.id }) { h ->
                    KartuRiwayat(h, onKlik = { detailId = h.id })
                }
            }
        }
    }
    }
}

@Composable
private fun KartuRiwayat(h: OpnameHeader, onKlik: () -> Unit) {
    val warna = when (h.status) {
        StatusOpname.FINALIZED, StatusOpname.APPROVED -> Color(0xFF168451)
        StatusOpname.PENDING_APPROVAL -> Color(0xFFC27A12)
        StatusOpname.REJECTED -> Color(0xFFDC2626)
        StatusOpname.DRAFT -> Color(0xFF64748B)
    }
    Surface(
        Modifier.fillMaxWidth().clickable(onClick = onKlik),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    tanggalSingkat(h.tanggal),
                    color = SukaOnSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    listOfNotNull(h.tipe, h.creatorName).joinToString(" · ").ifBlank { "-" },
                    color = SukaOnSurfaceVariant,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (h.jumlahItem > 0) {
                    Text(
                        "${h.jumlahItem} item" + if (h.jumlahFlagged > 0) " · ${h.jumlahFlagged} di luar toleransi" else "",
                        color = if (h.jumlahFlagged > 0) Color(0xFFC2410C) else Color(0xFF94A3B8),
                        fontSize = 10.sp,
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = warna.copy(alpha = 0.10f),
                border = BorderStroke(1.dp, warna.copy(alpha = 0.28f)),
            ) {
                Text(
                    h.status.label,
                    Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    color = warna, fontSize = 10.sp, fontWeight = FontWeight.Black,
                )
            }
        }
    }
}

// -------------------------------------------------------------------- formulir

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormOpname(state: OpnameUiState, viewModel: OpnameViewModel) {
    Column(Modifier.fillMaxSize().background(SukaSurface)) {
        HeaderStok(
            judul = "Hitung Fisik",
            subjudul = "${state.jumlahTerisi} dari ${state.items.size} bahan terisi",
            onKembali = viewModel::tutupForm,
        )

        state.error?.let { PitaPesan(it, true, viewModel::bersihkanPesan) }
        state.pesan?.let { PitaPesan(it, false, viewModel::bersihkanPesan) }

        val terkunci = state.terkunci
        if (state.memuatForm) {
            MemuatPenuh()
        } else if (terkunci != null) {
            // Form sengaja tidak dirender sama sekali: menampilkan kolom isian yang
            // penyimpanannya pasti ditolak lebih buruk daripada tidak menampilkannya.
            KeadaanKosong(terkunci.pesan)
        } else {
        OutlinedTextField(
            value = state.cari,
            onValueChange = viewModel::ubahCari,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            placeholder = { Text("Cari bahan…", fontSize = 13.sp, color = Color(0xFF94A3B8)) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp)) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = Color(0xFFF97316),
                unfocusedBorderColor = Color(0xFFE2E8F0),
            ),
        )

        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            items(state.itemTampil, key = { it.bahanBakuId }) { item ->
                BarisHitung(item, viewModel)
            }
        }

        Surface(color = Color.White, shadowElevation = 8.dp) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = viewModel::simpanDraft,
                    modifier = Modifier.weight(1f),
                    enabled = !state.menyimpan,
                    shape = RoundedCornerShape(13.dp),
                ) { Text("Simpan Draft", fontSize = 13.sp, fontWeight = FontWeight.Bold) }

                Button(
                    onClick = viewModel::finalisasi,
                    modifier = Modifier.weight(1f),
                    enabled = !state.menyimpan,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                    shape = RoundedCornerShape(13.dp),
                ) {
                    Text(
                        if (state.menyimpan) "Memproses…" else "Finalisasi",
                        fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BarisHitung(item: OpnameItemRow, viewModel: OpnameViewModel) {
    val selisih = viewModel.selisih(item)
    val ditandai = viewModel.ditandai(item)
    val persen = Selisih.persen(selisih, item.qtySystemSmallest)

    // Warna border: merah bila selisih di luar toleransi, hijau bila sudah tersimpan
    // sebagai draft, abu-abu polos bila belum ada aksi apa-apa.
    val borderColor = when {
        item.adaMasukan && ditandai -> Color(0xFFDC2626).copy(alpha = 0.35f)
        item.tersimpanDraft -> Color(0xFF168451).copy(alpha = 0.30f)
        else -> Color(0xFFF1F5F9)
    }

    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (item.tersimpanDraft) Color(0xFFF0FDF4) else Color.White,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        item.namaBahan,
                        color = Color(0xFFEA580C),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "Sistem: ${formatAngkaStok(item.qtySystemSmallest)} ${formatSatuan(item.meta.satuanKecil ?: item.meta.satuan)}" +
                            if (item.terukur) " · toleransi 5%" else " · toleransi 0%",
                        color = SukaOnSurfaceVariant,
                        fontSize = 10.sp,
                    )
                    // Tanda visual: baris yang sudah tersimpan sebagai draft tidak polos.
                    if (item.tersimpanDraft) {
                        Text(
                            "✓ Tersimpan",
                            color = Color(0xFF168451),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                if (item.adaMasukan) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            (if (selisih > 0) "+" else "") + formatAngkaStok(selisih),
                            color = if (ditandai) Color(0xFFDC2626) else Color(0xFF168451),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Text(persen.teks, color = Color(0xFF94A3B8), fontSize = 9.sp)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KolomAngka(
                    label = formatSatuan(item.meta.satuan).ifBlank { "Besar" },
                    nilai = item.besar,
                    modifier = Modifier.weight(1f),
                ) { viewModel.ubahMasukan(item.bahanBakuId, besar = it) }

                if (item.meta.satuanTengah != null) {
                    KolomAngka(
                        label = formatSatuan(item.meta.satuanTengah),
                        nilai = item.tengah,
                        modifier = Modifier.weight(1f),
                    ) { viewModel.ubahMasukan(item.bahanBakuId, tengah = it) }
                }

                if (item.meta.satuanKecil != null) {
                    KolomAngka(
                        label = formatSatuan(item.meta.satuanKecil),
                        nilai = item.kecil,
                        modifier = Modifier.weight(1f),
                    ) { viewModel.ubahMasukan(item.bahanBakuId, kecil = it) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KolomAngka(
    label: String,
    nilai: String,
    modifier: Modifier = Modifier,
    onUbah: (String) -> Unit,
) {
    Column(modifier) {
        Text(
            label.uppercase(),
            color = Color(0xFF9AA6B2),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(3.dp))
        OutlinedTextField(
            value = nilai,
            // Hanya angka dan satu titik desimal yang diterima; menolak di sini lebih
            // baik daripada membiarkan teks tak terbaca lalu diam-diam dianggap nol.
            onValueChange = { teks ->
                if (teks.isEmpty() || teks.matches(Regex("^\\d*\\.?\\d*$"))) onUbah(teks)
            },
            modifier = Modifier.fillMaxWidth(),
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
