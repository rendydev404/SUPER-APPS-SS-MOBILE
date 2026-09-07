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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
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

private val ORANGE = Color(0xFFEA580C)
private val SLATE400 = Color(0xFF94A3B8)
private val SLATE500 = Color(0xFF64748B)
private val SLATE900 = Color(0xFF0F172A)
private val GARIS = Color(0xFFE2E8F0)
private val MERAH = Color(0xFFB91C1C)
private val HIJAU = Color(0xFF15803D)
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

    Column(Modifier.fillMaxSize().background(Color(0xFFF8FAFC))) {
        HeaderStok(
            judul = if (state.poDibuka != null) "Verifikasi Penerimaan" else "Penerimaan PO Supplier",
            subjudul = state.poDibuka?.nomorPo
                ?: if (state.memuat) "Memuat…" else "${state.daftar.size} PO menunggu barang",
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
        if (pesan != null) {
            Surface(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
                    .clickable { viewModel.bersihkanPesan() },
                shape = RoundedCornerShape(12.dp),
                color = if (state.pesan != null) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
            ) {
                Text(
                    pesan,
                    Modifier.padding(12.dp),
                    color = if (state.pesan != null) HIJAU else MERAH,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        when {
            state.memuat -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ORANGE)
            }
            state.poDibuka != null -> FormVerifikasi(state, viewModel)
            else -> DaftarPo(state, viewModel)
        }
    }
}

@Composable
private fun DaftarPo(state: PenerimaanPoUiState, viewModel: PenerimaanPoViewModel) {
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = state.cari,
            onValueChange = viewModel::ubahCari,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            placeholder = { Text("Cari nomor PO atau supplier…", fontSize = 12.5.sp, color = SLATE400) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = kolomWarna(),
        )

        if (state.daftarTampil.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Tidak ada PO yang menunggu",
                        color = SLATE900,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Yang tampil di sini hanya PO 30 hari terakhir berstatus dikirim ke supplier atau baru diterima sebagian.",
                        color = SLATE500,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                items(state.daftarTampil, key = { it.id }) { po ->
                    KartuPo(po) { viewModel.bukaPo(po) }
                }
            }
        }
    }
}

@Composable
private fun KartuPo(po: PoInbound, onKlik: () -> Unit) {
    Surface(
        Modifier.fillMaxWidth().clickable(onClick = onKlik),
        shape = RoundedCornerShape(15.dp),
        color = Color.White,
        border = BorderStroke(1.dp, GARIS),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    po.nomorPo,
                    color = SLATE900,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(po.supplierNama, po.tanggal?.take(10)).joinToString(" · "),
                    color = SLATE500,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            if (po.sebagian) {
                Surface(shape = RoundedCornerShape(50), color = AMBER_LATAR) {
                    Text(
                        "SEBAGIAN",
                        Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        color = AMBER_TEKS,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
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
        Column(Modifier.fillMaxSize()) {
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
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
                        color = if (aktif) warna.copy(alpha = 0.10f) else Color(0xFFF8FAFC),
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
