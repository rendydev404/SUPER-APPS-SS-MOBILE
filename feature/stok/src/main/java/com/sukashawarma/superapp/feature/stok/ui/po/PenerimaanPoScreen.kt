package com.sukashawarma.superapp.feature.stok.ui.po

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.RealtimeTables
import com.sukashawarma.superapp.feature.stok.data.ItemPo
import com.sukashawarma.superapp.feature.stok.data.PoInbound
import com.sukashawarma.superapp.feature.stok.ui.HeaderStok
import com.sukashawarma.superapp.feature.stok.ui.KeadaanGagal
import com.sukashawarma.superapp.feature.stok.ui.MemuatPenuh
import com.sukashawarma.superapp.feature.stok.ui.PitaPesan
import com.sukashawarma.superapp.feature.stok.ui.tanggalSingkat
import com.sukashawarma.superapp.presentation.theme.SukaGray400
import com.sukashawarma.superapp.presentation.theme.SukaGray500
import com.sukashawarma.superapp.presentation.theme.SukaGreen
import com.sukashawarma.superapp.presentation.theme.SukaBrown
import com.sukashawarma.superapp.presentation.theme.SukaOnSurface
import com.sukashawarma.superapp.presentation.theme.SukaOrange
import com.sukashawarma.superapp.presentation.theme.SukaPrimaryContainer
import com.sukashawarma.superapp.presentation.theme.SukaSurface
import com.sukashawarma.superapp.presentation.theme.SukaSurfaceContainerHigh

private val ORANGE = SukaOrange
private val SLATE400 = SukaGray400
private val SLATE500 = SukaGray500
private val SLATE900 = SukaOnSurface
private val GARIS = SukaSurfaceContainerHigh
private val MERAH = Color(0xFFB91C1C)
private val HIJAU = SukaGreen
private val AMBER_LATAR = Color(0xFFFEF3C7)
private val AMBER_TEKS = Color(0xFF92400E)

/**
 * Penerimaan PO Supplier — cermin `app/stok/penerimaan-po/page.tsx` web.
 *
 * Dua keadaan: daftar PO yang barangnya masih di jalan, lalu pemeriksaan fisik
 * satu PO. Yang tersimpan bukan sekadar "diterima" — `verifikasi_terima_po`
 * menambah stok gudang dan mencatat harga beli aktual sekaligus.
 */
@Composable
fun PenerimaanPoScreen(
    onBack: () -> Unit,
    viewModel: PenerimaanPoViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()

    RealtimeRefresh(RealtimeTables.PURCHASE_ORDER) { viewModel.muatUlang() }
    BackHandler(enabled = state.poDibuka != null) { viewModel.tutupPo() }

    Column(Modifier.fillMaxSize().background(SukaSurface)) {
        HeaderStok(
            judul = if (state.poDibuka != null) "Verifikasi Penerimaan" else "Penerimaan PO Supplier",
            subjudul = state.poDibuka?.nomorPo
                ?: if (state.memuat) "Memuat…" else "${state.daftar.size} PO menunggu",
            onKembali = { if (state.poDibuka != null) viewModel.tutupPo() else onBack() },
            aksi = {
                if (state.poDibuka == null) {
                    IconButton(onClick = viewModel::muatUlang) {
                        Icon(Icons.Default.Refresh, "Muat ulang", tint = Color.White)
                    }
                }
            },
        )

        val pesan = state.pesan ?: state.error
        if (pesan != null && !(state.error != null && state.daftar.isEmpty())) {
            PitaPesan(
                pesan = pesan,
                gagal = state.error != null && state.pesan == null,
                onTutup = viewModel::bersihkanPesan,
            )
        }

        when {
            state.poDibuka != null -> FormVerifikasi(state, viewModel)
            state.memuat -> MemuatPenuh(Modifier.fillMaxSize())
            state.error != null && state.daftar.isEmpty() -> KeadaanGagal(
                pesan = state.error.orEmpty(),
                onCobaLagi = viewModel::muatUlang,
                modifier = Modifier.fillMaxSize(),
            )
            else -> DaftarPo(state, viewModel)
        }
    }
}

@Composable
private fun DaftarPo(state: PenerimaanPoUiState, viewModel: PenerimaanPoViewModel) {
    val daftar = state.daftarTampil
    val jumlahSebagian = daftar.count { it.sebagian }
    val sedangMencari = state.cari.isNotBlank()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "pencarian") {
            OutlinedTextField(
                value = state.cari,
                onValueChange = viewModel::ubahCari,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Cari nomor PO atau supplier", fontSize = 13.sp, color = SLATE400) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = ORANGE, modifier = Modifier.size(20.dp))
                },
                trailingIcon = if (state.cari.isNotBlank()) {
                    {
                        IconButton(onClick = { viewModel.ubahCari("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Hapus pencarian", tint = SLATE500)
                        }
                    }
                } else null,
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = kolomWarna(),
            )
        }

        item(key = "ringkasan") {
            RingkasanPenerimaan(
                total = daftar.size,
                jumlahSebagian = jumlahSebagian,
                sedangMencari = sedangMencari,
            )
        }

        if (daftar.isEmpty()) {
            item(key = "kosong") { KosongPo(sedangMencari) }
        } else {
            item(key = "judul-daftar") {
                Row(
                    Modifier.fillMaxWidth().padding(top = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (sedangMencari) "Hasil pencarian" else "Perlu diterima",
                            color = SLATE900,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                        )
                        Text(
                            if (sedangMencari) "Cocokkan nomor PO atau nama supplier"
                            else "Tap satu PO untuk mulai verifikasi barang",
                            color = SLATE500,
                            fontSize = 11.5.sp,
                        )
                    }
                    Surface(shape = RoundedCornerShape(50), color = SukaPrimaryContainer.copy(alpha = 0.16f)) {
                        Text(
                            "${daftar.size} PO",
                            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            color = ORANGE,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }

            items(daftar, key = { it.id }) { po ->
                KartuPo(po) { viewModel.bukaPo(po) }
            }
        }
    }
}

@Composable
private fun KartuPo(po: PoInbound, onKlik: () -> Unit) {
    Surface(
        Modifier.fillMaxWidth().clickable(onClick = onKlik),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, GARIS),
        shadowElevation = 1.dp,
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Box(
                Modifier.size(44.dp).background(SukaPrimaryContainer.copy(alpha = 0.15f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Inventory2, contentDescription = null, tint = ORANGE, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        po.nomorPo,
                        Modifier.weight(1f),
                        color = SLATE900,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SLATE400, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    po.supplierNama,
                    color = SLATE500,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = SLATE400, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.size(4.dp))
                    Text(tanggalSingkat(po.tanggal), color = SLATE500, fontSize = 11.sp)
                    Spacer(Modifier.weight(1f))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (po.sebagian) AMBER_LATAR else SukaGreen.copy(alpha = 0.10f),
                    ) {
                        Text(
                            if (po.sebagian) "Sebagian" else "Menunggu",
                            Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = if (po.sebagian) AMBER_TEKS else HIJAU,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RingkasanPenerimaan(total: Int, jumlahSebagian: Int, sedangMencari: Boolean) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = SukaBrown,
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(36.dp).background(Color.White.copy(alpha = 0.14f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Inventory2, contentDescription = null, tint = Color.White, modifier = Modifier.size(19.dp))
                }
                Spacer(Modifier.size(10.dp))
                Column {
                    Text(
                        if (sedangMencari) "Hasil antrean" else "Antrean penerimaan",
                        color = Color(0xFFFFE8D1),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        if (total == 0) "Tidak ada PO yang cocok" else "$total PO menunggu barang",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (sedangMencari) "Perbarui kata kunci untuk hasil lain"
                    else "Mulai dari PO paling atas",
                    Modifier.weight(1f),
                    color = Color(0xFFFFE8D1),
                    fontSize = 11.5.sp,
                )
                if (!sedangMencari && jumlahSebagian > 0) {
                    Surface(shape = RoundedCornerShape(50), color = Color.White.copy(alpha = 0.14f)) {
                        Text(
                            "$jumlahSebagian sebagian",
                            Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KosongPo(sedangMencari: Boolean) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, GARIS),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(52.dp).background(SukaSurfaceContainerHigh, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = HIJAU, modifier = Modifier.size(27.dp))
            }
            Spacer(Modifier.height(13.dp))
            Text(
                if (sedangMencari) "PO tidak ditemukan" else "Antrean sudah bersih",
                color = SLATE900,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (sedangMencari) "Coba nomor PO atau nama supplier lain."
                else "PO 30 hari terakhir yang masih menunggu barang akan muncul di sini.",
                color = SLATE500,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun FormVerifikasi(state: PenerimaanPoUiState, viewModel: PenerimaanPoViewModel) {
    if (state.memuatItem) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = ORANGE)
        }
    } else {
        val po = state.poDibuka ?: return
        Column(Modifier.fillMaxSize()) {
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "panduan-verifikasi") {
                    PanduanVerifikasi(po, state.item.size)
                }
                items(state.item, key = { it.id }) { baris ->
                    KartuItemPo(baris, state, viewModel)
                }
            }

            Surface(color = Color.White, shadowElevation = 8.dp) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    val halangan = state.halangan
                    if (halangan != null) {
                        Text(halangan, color = SLATE500, fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(8.dp))
                    }
                    Button(
                        onClick = viewModel::kirim,
                        enabled = halangan == null && !state.mengirim,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(13.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ORANGE),
                    ) {
                        Text(
                            if (state.mengirim) "Memproses…" else "Simpan penerimaan",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PanduanVerifikasi(po: PoInbound, jumlahBaris: Int) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = SukaPrimaryContainer.copy(alpha = 0.13f),
        border = BorderStroke(1.dp, SukaPrimaryContainer.copy(alpha = 0.28f)),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(38.dp).background(Color.White.copy(alpha = 0.72f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Inventory2, contentDescription = null, tint = ORANGE, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.size(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Cek barang yang datang", color = SLATE900, fontSize = 13.sp, fontWeight = FontWeight.Black)
                    Text(po.supplierNama, color = SLATE500, fontSize = 11.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Surface(shape = RoundedCornerShape(50), color = Color.White.copy(alpha = 0.75f)) {
                    Text(
                        "$jumlahBaris baris",
                        Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        color = ORANGE,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
            Spacer(Modifier.height(11.dp))
            Text(
                "Isi jumlah dan harga aktual. Tandai rusak bila perlu, lalu simpan setelah semua baris diperiksa.",
                color = SLATE500,
                fontSize = 11.5.sp,
                lineHeight = 17.sp,
            )
        }
    }
}

@Composable
private fun KartuItemPo(
    baris: ItemPo,
    state: PenerimaanPoUiState,
    viewModel: PenerimaanPoViewModel,
) {
    val isi = state.isianUntuk(baris.id)
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(15.dp),
        color = Color.White,
        border = BorderStroke(1.dp, GARIS),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                baris.nama,
                color = SLATE900,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                buildString {
                    append("Dipesan ${angka(baris.qtyPesan)} ${baris.satuan}")
                    if (baris.qtyTerimaSebelumnya > 0) {
                        append(" · sudah diterima ${angka(baris.qtyTerimaSebelumnya)}")
                    }
                },
                color = SLATE500,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
            )

            Spacer(Modifier.height(11.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedTextField(
                    value = isi.qtyDatang,
                    onValueChange = { viewModel.ubahQty(baris.id, it) },
                    modifier = Modifier.weight(1f),
                    label = { Text("Datang (${baris.satuan})", fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(11.dp),
                    colors = kolomWarna(),
                )
                OutlinedTextField(
                    value = isi.hargaTerima,
                    onValueChange = { viewModel.ubahHarga(baris.id, it) },
                    modifier = Modifier.weight(1f),
                    label = { Text("Harga satuan", fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(11.dp),
                    colors = kolomWarna(),
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("baik" to "Baik", "rusak" to "Rusak").forEach { (nilai, label) ->
                    val aktif = isi.kondisi == nilai
                    val warna = if (nilai == "rusak") MERAH else HIJAU
                    Surface(
                        Modifier.weight(1f).clickable { viewModel.ubahKondisi(baris.id, nilai) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (aktif) warna.copy(alpha = 0.10f) else SukaSurface,
                        border = BorderStroke(1.dp, if (aktif) warna else GARIS),
                    ) {
                        Text(
                            label,
                            Modifier.padding(vertical = 9.dp).fillMaxWidth(),
                            color = if (aktif) warna else SLATE500,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            if (isi.kondisi == "rusak") {
                Spacer(Modifier.height(9.dp))
                OutlinedTextField(
                    value = isi.catatan,
                    onValueChange = { viewModel.ubahCatatan(baris.id, it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Catatan kerusakan", fontSize = 12.sp, color = SLATE400) },
                    singleLine = true,
                    shape = RoundedCornerShape(11.dp),
                    colors = kolomWarna(),
                )
            }
        }
    }
}

@Composable
private fun kolomWarna() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    focusedBorderColor = ORANGE,
    unfocusedBorderColor = GARIS,
)

private fun angka(nilai: Double): String =
    if (nilai % 1.0 == 0.0) nilai.toLong().toString() else nilai.toString()
