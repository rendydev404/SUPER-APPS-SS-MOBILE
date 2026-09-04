package com.sukashawarma.superapp.feature.stok.ui.permintaan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.sukashawarma.superapp.feature.stok.data.model.BahanBaku
import com.sukashawarma.superapp.feature.stok.data.model.BudgetStatus
import com.sukashawarma.superapp.feature.stok.data.model.Permintaan
import com.sukashawarma.superapp.feature.stok.data.model.SaranPermintaan
import com.sukashawarma.superapp.feature.stok.data.model.StatusPermintaan
import com.sukashawarma.superapp.feature.stok.domain.Budget
import com.sukashawarma.superapp.feature.stok.domain.BudgetVarian
import com.sukashawarma.superapp.feature.stok.domain.formatAngkaStok
import com.sukashawarma.superapp.feature.stok.domain.formatRupiah as formatRp
import com.sukashawarma.superapp.feature.stok.domain.formatSatuan
import com.sukashawarma.superapp.feature.stok.domain.formatTriUnitAdaptif
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
import kotlin.math.ceil

private val Oranye = Color(0xFFEA580C)
private val Merah = Color(0xFFDC2626)
private val Hijau = Color(0xFF168451)
private val Kuning = Color(0xFFC27A12)
private val Abu = Color(0xFF64748B)

/**
 * Kategori tampilan satu bahan. Menyamakan kosong dan null menjadi "LAIN-LAIN"
 * sama seperti RPC estimasi (`COALESCE(NULLIF(kategori,''),'LAIN-LAIN')`) — kalau
 * berbeda, subtotal per kategori tidak akan pernah ketemu kuncinya.
 */
private fun kategoriTampil(kategori: String?): String =
    kategori?.takeIf { it.isNotBlank() } ?: "LAIN-LAIN"

private fun warnaStatus(status: StatusPermintaan): Color = when (status) {
    StatusPermintaan.DISETUJUI -> Hijau
    StatusPermintaan.MENUNGGU -> Kuning
    StatusPermintaan.DITOLAK -> Merah
    StatusPermintaan.DIBATALKAN -> Abu
}

/**
 * Badge sisa budget — cermin `BudgetBadge.tsx`. Disembunyikan bila outlet tak
 * punya plafon. Tidak pernah memblokir pengiriman: web pun menandainya
 * "Tahap Developer (Bisa Diabaikan)", keputusan tetap di approver.
 */
@Composable
private fun BadgeBudget(
    status: BudgetStatus?,
    proyeksi: Double = 0.0,
    ringkas: Boolean = false,
) {
    if (status == null) return
    val varian = Budget.varian(status.hasConfig, status.nominal, status.terpakai, proyeksi)
    if (varian == BudgetVarian.TERSEMBUNYI) return

    val warna = when (varian) {
        BudgetVarian.HIJAU -> Hijau
        BudgetVarian.ORANYE -> Kuning
        else -> Merah
    }
    val sisaProyeksi = status.sisa - proyeksi

    if (ringkas) {
        val label = when (varian) {
            BudgetVarian.MERAH ->
                "Melebihi Budget" + if (sisaProyeksi < 0) " +${formatRp(-sisaProyeksi)}" else ""
            BudgetVarian.ORANYE -> "Mendekati Budget"
            else -> "Dalam Budget"
        }
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = warna.copy(alpha = 0.10f),
            border = BorderStroke(1.dp, warna.copy(alpha = 0.28f)),
        ) {
            Text(
                label,
                Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                color = warna, fontSize = 9.sp, fontWeight = FontWeight.Black,
            )
        }
        return
    }

    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = warna.copy(alpha = 0.07f),
        border = BorderStroke(1.dp, warna.copy(alpha = 0.25f)),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                "Sisa Budget ${Budget.labelPeriode(status.periodType, status.customDays)}: " +
                    formatRp(maxOf(0.0, sisaProyeksi)) + " dari ${formatRp(status.nominal)}",
                color = warna, fontSize = 11.sp, fontWeight = FontWeight.Bold, lineHeight = 15.sp,
            )
            Text(
                "Tahap Developer (Bisa Diabaikan)",
                color = Color(0xFF92400E), fontSize = 8.sp, fontWeight = FontWeight.Black,
            )
            if (proyeksi > 0) {
                Text(
                    "(Termasuk estimasi keranjang saat ini: -${formatRp(proyeksi)})",
                    color = SukaOnSurfaceVariant, fontSize = 10.sp,
                )
            }
            if (sisaProyeksi < 0) {
                Spacer(Modifier.height(5.dp))
                Text(
                    "⚠️ Saldo tidak mencukupi (minus ${formatRp(-sisaProyeksi)}). " +
                        "Pengajuan tetap dapat dilakukan selama tahap developer.",
                    color = Merah, fontSize = 10.sp, lineHeight = 14.sp, fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

/**
 * Qty permintaan yang TERSIMPAN, siap tampil. Nilainya selalu pada satuan besar dan
 * bisa pecahan bila permintaan dibuat dari web (0,2083 Dus = 5 Pack), jadi ditampilkan
 * berjenjang — bukan dibulatkan jadi "1 Dus" yang menyesatkan.
 */
private fun qtyTersimpanTeks(qtyBase: Double, bahan: BahanBaku?, satuanCadangan: String?): String {
    val meta = bahan?.meta ?: return "${formatAngkaStok(qtyBase)} ${formatSatuan(satuanCadangan)}".trim()
    return formatTriUnitAdaptif(qtyBase, saldoIsGram = false, meta = meta)
}

@Composable
fun PermintaanScreen(viewModel: PermintaanViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()

    when {
        state.approveUntuk != null -> LayarPersetujuan(state, state.approveUntuk!!, viewModel)
        state.tinjauTerbuka -> LayarTinjau(state, viewModel)
        else -> LayarUtama(state, viewModel)
    }

    if (state.nudgeTerbuka) DialogNudge(state, viewModel)
    if (state.konfirmasiTerbuka) DialogKonfirmasi(state, viewModel)
}

// ============================================================== layar utama

@Composable
private fun LayarUtama(state: PermintaanUiState, viewModel: PermintaanViewModel) {
    Column(Modifier.fillMaxSize().background(SukaSurface)) {
        HeaderStok(
            judul = "Permintaan Bahan Baku",
            subjudul = "Alur distribusi Kitchen & Outlet",
        )
        state.pesan?.let { PitaPesan(it, false, viewModel::bersihkanPesan) }
        state.error?.let { PitaPesan(it, true, viewModel::bersihkanPesan) }

        when {
            state.tidakBerhak -> KeadaanTidakBerhak("Akun Anda belum terhubung dengan outlet mana pun.")
            state.memuat -> MemuatPenuh()
            state.error != null && state.katalog.isEmpty() && state.daftarReview.isEmpty() ->
                KeadaanGagal(state.error, viewModel::muatAwal)
            state.modeAntrean -> AntreanPersetujuan(state, viewModel)
            else -> {
                if (state.outlets.size > 1) {
                    PemilihOutlet(state.outlets, state.outletTerpilih, viewModel::pilihOutlet)
                }
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
                when (state.tab) {
                    TabPermintaan.BUAT -> KontenKatalog(state, viewModel)
                    TabPermintaan.RIWAYAT -> KontenRiwayat(state, viewModel)
                }
            }
        }
    }
}

// ================================================================= katalog

@Composable
private fun KontenKatalog(state: PermintaanUiState, viewModel: PermintaanViewModel) {
    val pending = state.pendingItemIds
    val saranBoleh = state.saranBoleh
    val saranMap = saranBoleh.associateBy { it.bahanBakuId }
    val terfilter = state.katalogTerfilter
    val belumDitambah = saranBoleh.count { (state.keranjang[it.bahanBakuId] ?: 0L) <= 0L }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            if (pending.isNotEmpty()) {
                item(key = "pending-alert") {
                    Surface(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFFFFBEB),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                    ) {
                        Text(
                            "${pending.size} item bahan baku sedang menunggu persetujuan kitchen. " +
                                "Bahan tersebut otomatis disembunyikan agar tidak terduplikasi.",
                            Modifier.padding(12.dp),
                            color = Color(0xFF92400E), fontSize = 11.sp, lineHeight = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            if (state.budget != null) {
                item(key = "budget") {
                    BadgeBudget(status = state.budget, proyeksi = state.estimasi.totalNilai)
                }
            }

            item(key = "cari") {
                OutlinedTextField(
                    value = state.cari,
                    onValueChange = viewModel::ubahCari,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text("Cari nama bahan baku atau kategori…", fontSize = 12.sp, color = Color(0xFF94A3B8))
                    },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (state.cari.isNotEmpty()) {
                            IconButton(onClick = { viewModel.ubahCari("") }) {
                                Icon(Icons.Default.Close, "Hapus", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color(0xFFF97316),
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                    ),
                )
            }

            item(key = "kategori-chips") {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ChipKategori(
                        label = "Semua (${state.katalogBoleh.count { it.id !in pending }})",
                        aktif = state.kategoriTerpilih == "all",
                        warnaAktif = Color(0xFF3E2A20),
                    ) { viewModel.pilihKategori("all") }
                    if (saranBoleh.isNotEmpty()) {
                        ChipKategori(
                            label = "🔥 Kritis (${saranBoleh.size})",
                            aktif = state.kategoriTerpilih == "kritis",
                            warnaAktif = Merah,
                        ) { viewModel.pilihKategori("kritis") }
                    }
                    state.kategoriList.forEach { kat ->
                        ChipKategori(
                            label = kat,
                            aktif = state.kategoriTerpilih == kat,
                            warnaAktif = Color(0xFF3E2A20),
                        ) { viewModel.pilihKategori(kat) }
                    }
                }
            }

            if (belumDitambah > 0) {
                item(key = "tambah-kritis") {
                    Button(
                        onClick = viewModel::tambahSemuaKritis,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Merah),
                        shape = RoundedCornerShape(13.dp),
                    ) {
                        Text(
                            "＋ Tambah Semua Kritis ($belumDitambah)",
                            fontSize = 12.sp, fontWeight = FontWeight.Black,
                        )
                    }
                }
            }

            if (terfilter.isEmpty()) {
                item(key = "kosong") {
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "Tidak ada bahan baku yang sesuai",
                            color = SukaOnSurface, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold,
                        )
                        Text(
                            "Coba ubah kata kunci atau filter kategori.",
                            color = SukaOnSurfaceVariant, fontSize = 11.sp,
                        )
                        if (state.cari.isNotEmpty() || state.kategoriTerpilih != "all") {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Reset Filter",
                                Modifier.clickable(onClick = viewModel::resetFilter),
                                color = Oranye, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            } else {
                // Kelompokkan per kategori seperti web; filter kritis/kategori jadi satu grup.
                val grup: List<Pair<String, List<BahanBaku>>> = when (state.kategoriTerpilih) {
                    "kritis" -> listOf("Bahan Baku Stok Kritis" to terfilter)
                    "all" -> terfilter.groupBy { kategoriTampil(it.kategori) }.toList()
                    else -> listOf(state.kategoriTerpilih to terfilter)
                }
                grup.forEach { (kategori, daftar) ->
                    item(key = "header-$kategori") {
                        Row(
                            Modifier.fillMaxWidth().padding(top = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier.size(width = 8.dp, height = 14.dp)
                                    .background(
                                        if (kategori == "Bahan Baku Stok Kritis") Merah else Oranye,
                                        RoundedCornerShape(50),
                                    )
                            )
                            Spacer(Modifier.width(7.dp))
                            Text(
                                kategori.uppercase(),
                                Modifier.weight(1f),
                                color = SukaOnSurface, fontSize = 11.sp,
                                fontWeight = FontWeight.Black, letterSpacing = 0.5.sp,
                            )
                            Text("${daftar.size} item", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    items(daftar, key = { it.id }) { bahan ->
                        KartuBahanKatalog(
                            bahan = bahan,
                            saran = saranMap[bahan.id],
                            qty = state.keranjang[bahan.id] ?: 0L,
                            viewModel = viewModel,
                        )
                    }
                }
            }
        }

        val jumlahItem = state.keranjangItems.size
        if (jumlahItem > 0) {
            BarKeranjang(jumlahItem, state.estimasi.totalNilai, viewModel::bukaTinjau)
        }
    }
}

@Composable
private fun ChipKategori(label: String, aktif: Boolean, warnaAktif: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (aktif) warnaAktif else Color.White,
        border = BorderStroke(1.dp, if (aktif) warnaAktif else Color(0xFFE2E8F0)),
    ) {
        Text(
            label,
            Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
            color = if (aktif) Color.White else SukaOnSurfaceVariant,
            fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1,
        )
    }
}

@Composable
private fun KartuBahanKatalog(
    bahan: BahanBaku,
    saran: SaranPermintaan?,
    qty: Long,
    viewModel: PermintaanViewModel,
) {
    val kritis = saran != null
    val ditambah = qty > 0L
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(
            1.dp,
            when {
                ditambah -> Oranye.copy(alpha = 0.55f)
                kritis -> Merah.copy(alpha = 0.30f)
                else -> Color(0xFFF1F5F9)
            },
        ),
    ) {
        Column(Modifier.padding(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    (bahan.kategori ?: "BAHAN BAKU").uppercase(),
                    color = Color(0xFF94A3B8), fontSize = 9.sp, fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp, modifier = Modifier.weight(1f),
                )
                if (kritis) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Merah.copy(alpha = 0.10f),
                        border = BorderStroke(1.dp, Merah.copy(alpha = 0.28f)),
                    ) {
                        Text(
                            "🔥 KRITIS",
                            Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            color = Merah, fontSize = 9.sp, fontWeight = FontWeight.Black,
                        )
                    }
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(
                bahan.nama,
                color = SukaOnSurface, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            if (saran != null) {
                Text(
                    "Sisa: ${formatTriUnitAdaptif(saran.currentQty, saran.saldoIsGram, bahan.meta)}",
                    color = Merah, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                )
            } else {
                Text(
                    "Satuan Pesan: ${formatSatuan(bahan.satuanPesan)}",
                    color = SukaOnSurfaceVariant, fontSize = 11.sp,
                )
            }
            Spacer(Modifier.height(10.dp))
            if (!ditambah) {
                OutlinedButton(
                    onClick = {
                        if (saran != null) viewModel.tambahKritis(saran)
                        else viewModel.ubahKeranjang(bahan.id, 1L)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (kritis) Merah.copy(alpha = 0.4f) else Color(0xFFE2E8F0)),
                ) {
                    Icon(Icons.Default.Add, null, tint = if (kritis) Merah else Oranye, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        if (kritis) "Rekomendasi (${formatSatuan(bahan.satuanPesan)})"
                        else "Tambah (${formatSatuan(bahan.satuanPesan)})",
                        color = if (kritis) Merah else Oranye,
                        fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    )
                }
            } else {
                StepperQty(
                    qty = qty,
                    satuan = formatSatuan(bahan.satuanPesan),
                    onMinus = { viewModel.ubahKeranjang(bahan.id, -1L) },
                    onPlus = { viewModel.ubahKeranjang(bahan.id, 1L) },
                    onSet = { viewModel.setKeranjang(bahan.id, it) },
                )
            }
        }
    }
}

@Composable
private fun StepperQty(
    qty: Long,
    satuan: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    onSet: (String) -> Unit,
    diSorot: Boolean = false,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (diSorot) Color(0xFFFFF7ED) else Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, if (diSorot) Color(0xFFFDBA74) else Color(0xFFE2E8F0)),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(3.dp)) {
            Surface(
                onClick = onMinus,
                shape = RoundedCornerShape(9.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            ) {
                Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                    Icon(
                        if (qty <= 1L) Icons.Default.Delete else Icons.Default.Remove,
                        if (qty <= 1L) "Hapus" else "Kurangi",
                        tint = if (qty <= 1L) Merah else SukaOnSurface,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
            Row(
                Modifier.weight(1f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.foundation.text.BasicTextField(
                    value = if (qty > 0L) qty.toString() else "",
                    onValueChange = onSet,
                    modifier = Modifier.width(30.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = if (diSorot) Color(0xFFEA580C) else SukaOnSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                    ),
                )
                // softWrap dimatikan: label satuan pernah terpotong jadi dua baris ("Ba"/"l")
                // ketika kolomnya sempit, dan satuan yang terbelah lebih buruk daripada
                // satuan yang terpangkas.
                Text(
                    satuan,
                    color = SukaOnSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, softWrap = false, overflow = TextOverflow.Visible,
                )
            }
            Surface(
                onClick = onPlus,
                shape = RoundedCornerShape(9.dp),
                color = Oranye,
            ) {
                Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Add, "Tambah", tint = Color.White, modifier = Modifier.size(15.dp))
                }
            }
        }
    }
}

@Composable
private fun BarKeranjang(jumlahItem: Int, estimasi: Double, onBuka: () -> Unit) {
    Surface(color = Color(0xFF3E2A20), shadowElevation = 10.dp) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onBuka)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(38.dp).background(Oranye, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.ShoppingCart, null, tint = Color.White, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "$jumlahItem BAHAN DI KERANJANG",
                    color = Color(0xFFFFEDD5), fontSize = 9.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
                )
                Text(
                    if (estimasi > 0) "Est. ${formatRp(estimasi)}" else "Tinjau & Kirim",
                    color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold,
                )
            }
            Surface(shape = RoundedCornerShape(11.dp), color = Color.White.copy(alpha = 0.15f)) {
                Text(
                    "Tinjau →",
                    Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black,
                )
            }
        }
    }
}

// ================================================================== tinjau

@Composable
private fun LayarTinjau(state: PermintaanUiState, viewModel: PermintaanViewModel) {
    val items = state.keranjangItems
    val estimasi = state.estimasi
    Column(Modifier.fillMaxSize().background(SukaSurface)) {
        HeaderStok(
            judul = "Tinjau Permintaan",
            subjudul = "Periksa daftar & jumlah bahan sebelum dikirim",
            onKembali = viewModel::tutupTinjau,
        )
        state.pesan?.let { PitaPesan(it, false, viewModel::bersihkanPesan) }
        state.error?.let { PitaPesan(it, true, viewModel::bersihkanPesan) }

        if (items.isEmpty()) {
            Box(Modifier.weight(1f)) {
                KeadaanKosong("Belum ada bahan baku yang dipilih. Kembali ke katalog untuk menambah.")
            }
        } else {
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                if (state.budget != null) {
                    item(key = "tinjau-budget") {
                        BadgeBudget(status = state.budget, proyeksi = estimasi.totalNilai)
                    }
                }
                val perKategori = items.groupBy { kategoriTampil(it.bahan.kategori) }
                perKategori.forEach { (kategori, baris) ->
                    item(key = "tinjau-header-$kategori") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                kategori.uppercase(),
                                Modifier.weight(1f),
                                color = SukaOnSurface, fontSize = 11.sp,
                                fontWeight = FontWeight.Black, letterSpacing = 0.5.sp,
                            )
                            val subtotal = estimasi.kategoriNilai[kategori] ?: 0.0
                            if (subtotal > 0) {
                                Text(
                                    "Subtotal: ${formatRp(subtotal)}",
                                    color = Oranye, fontSize = 10.sp, fontWeight = FontWeight.Black,
                                )
                            }
                        }
                    }
                    items(baris, key = { "tinjau-${it.bahan.id}" }) { b ->
                        Surface(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                        ) {
                            Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        b.bahan.nama,
                                        color = SukaOnSurface, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    )
                                    // Satuan pesan ditulis terpisah di sini: di dalam stepper
                                    // labelnya terjepit dan sempat tak terbaca sama sekali,
                                    // padahal ini satu-satunya penanda "1 ini maksudnya apa".
                                    Text(
                                        "Satuan pesan: ${formatSatuan(b.bahan.satuanPesan)}",
                                        color = SukaOnSurfaceVariant, fontSize = 10.sp,
                                    )
                                    if (estimasi.itemTanpaHarga.contains(b.bahan.id)) {
                                        Text("Harga belum diset", color = Color(0xFF94A3B8), fontSize = 9.sp)
                                    }
                                }
                                Spacer(Modifier.width(10.dp))
                                Box(Modifier.width(152.dp)) {
                                    StepperQty(
                                        qty = b.qty,
                                        satuan = formatSatuan(b.bahan.satuanPesan),
                                        onMinus = { viewModel.ubahKeranjang(b.bahan.id, -1L) },
                                        onPlus = { viewModel.ubahKeranjang(b.bahan.id, 1L) },
                                        onSet = { viewModel.setKeranjang(b.bahan.id, it) },
                                    )
                                }
                            }
                        }
                    }
                }
                if (estimasi.totalNilai > 0) {
                    item(key = "tinjau-total") {
                        Surface(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFFFF7ED),
                            border = BorderStroke(1.dp, Color(0xFFFED7AA)),
                        ) {
                            Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "TOTAL ESTIMASI PESANAN",
                                        color = SukaOnSurfaceVariant, fontSize = 9.sp,
                                        fontWeight = FontWeight.Black, letterSpacing = 0.5.sp,
                                    )
                                    Text("${items.size} item bahan baku", color = SukaOnSurfaceVariant, fontSize = 10.sp)
                                }
                                Text(
                                    formatRp(estimasi.totalNilai),
                                    color = Oranye, fontSize = 16.sp, fontWeight = FontWeight.Black,
                                )
                            }
                        }
                    }
                }
            }
            Surface(color = Color.White, shadowElevation = 8.dp) {
                Row(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = viewModel::tutupTinjau,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(13.dp),
                    ) {
                        Text("＋ Tambah Bahan Lain", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Hijau)
                    }
                    Button(
                        onClick = viewModel::mulaiKirim,
                        modifier = Modifier.weight(1f),
                        enabled = !state.mengirim && items.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = Oranye),
                        shape = RoundedCornerShape(13.dp),
                    ) {
                        Text(
                            if (state.mengirim) "Mengirim…" else "Kirim ${items.size} Bahan",
                            fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

// ================================================================== riwayat

@Composable
private fun KontenRiwayat(state: PermintaanUiState, viewModel: PermintaanViewModel) {
    val daftar = state.riwayatTerfilter
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        item(key = "riwayat-cari") {
            OutlinedTextField(
                value = state.cariRiwayat,
                onValueChange = viewModel::ubahCariRiwayat,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("Cari kode (#REQ-…), pemohon, atau bahan…", fontSize = 12.sp, color = Color(0xFF94A3B8))
                },
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
        }
        item(key = "riwayat-filter") {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ChipKategori(
                    label = "Semua (${state.daftarOutlet.size})",
                    aktif = state.filterStatus == null,
                    warnaAktif = Color(0xFF3E2A20),
                ) { viewModel.pilihFilterStatus(null) }
                StatusPermintaan.entries.forEach { st ->
                    val jumlah = state.daftarOutlet.count { it.status == st }
                    ChipKategori(
                        label = "${st.label} ($jumlah)",
                        aktif = state.filterStatus == st,
                        warnaAktif = warnaStatus(st),
                    ) { viewModel.pilihFilterStatus(st) }
                }
            }
        }
        if (state.daftarOutlet.isEmpty()) {
            item(key = "riwayat-kosong") {
                KeadaanKosong("Permintaan bahan baku yang Anda buat akan tercatat dan ditampilkan di sini.")
            }
        } else if (daftar.isEmpty()) {
            item(key = "riwayat-tanpa-hasil") {
                KeadaanKosong("Tidak ada riwayat permintaan yang cocok dengan filter.")
            }
        } else {
            items(daftar, key = { it.id }) { p ->
                KartuRiwayat(p, state.bahanMap)
            }
        }
    }
}

@Composable
private fun KartuRiwayat(p: Permintaan, bahanMap: Map<String, BahanBaku>) {
    val warna = warnaStatus(p.status)
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = warna.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, warna.copy(alpha = 0.28f)),
                ) {
                    Text(
                        p.status.label.uppercase(),
                        Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        color = warna, fontSize = 9.sp, fontWeight = FontWeight.Black,
                    )
                }
                Spacer(Modifier.width(7.dp))
                Text(p.kodeReq, color = SukaOnSurface, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.weight(1f))
                Text(waktuSingkat(p.createdAt), color = SukaOnSurfaceVariant, fontSize = 10.sp)
            }
            p.pembuatNama?.let {
                Spacer(Modifier.height(4.dp))
                Text("Dibuat oleh: $it", color = SukaOnSurfaceVariant, fontSize = 10.sp)
            }
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
            ) {
                Column(Modifier.fillMaxWidth().padding(10.dp)) {
                    Text(
                        "${p.items.size} ITEM BAHAN BAKU",
                        color = Color(0xFF94A3B8), fontSize = 9.sp,
                        fontWeight = FontWeight.Black, letterSpacing = 0.5.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    p.items.forEach { item ->
                        val bahan = bahanMap[item.bahanBakuId]
                        val diminta = qtyTersimpanTeks(item.qtyDiminta, bahan, item.satuan)
                        val disetujui = item.qtyDisetujui?.let { qtyTersimpanTeks(it, bahan, item.satuan) }
                        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            Text(
                                item.namaBahan ?: item.bahanBakuId,
                                Modifier.weight(1f),
                                color = SukaOnSurfaceVariant, fontSize = 11.sp,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                buildString {
                                    append(diminta)
                                    if (disetujui != null && disetujui != diminta) {
                                        append(" → $disetujui")
                                    }
                                },
                                color = if (disetujui != null && disetujui != diminta) Oranye else SukaOnSurface,
                                fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
            if (p.status == StatusPermintaan.DITOLAK && !p.catatanKitchen.isNullOrBlank()) {
                Spacer(Modifier.height(7.dp))
                Text(
                    "Alasan Penolakan: ${p.catatanKitchen}",
                    color = Merah, fontSize = 10.sp, lineHeight = 14.sp,
                )
            }
            if (p.status == StatusPermintaan.DIBATALKAN && !p.catatanKitchen.isNullOrBlank()) {
                Spacer(Modifier.height(7.dp))
                Text(
                    "Catatan Pembatalan: ${p.catatanKitchen}",
                    color = Abu, fontSize = 10.sp, lineHeight = 14.sp,
                )
            }
        }
    }
}

// ================================================================== antrean

@Composable
private fun AntreanPersetujuan(state: PermintaanUiState, viewModel: PermintaanViewModel) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        item(key = "antrean-judul") {
            Text(
                "ANTREAN PERSETUJUAN",
                color = Oranye, fontSize = 11.sp,
                fontWeight = FontWeight.Black, letterSpacing = 1.sp,
            )
        }
        if (!state.bolehApprove) {
            item(key = "mode-pantau") {
                Surface(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFDF6EC),
                    border = BorderStroke(1.dp, Color(0xFFE7D8C6)),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            "👁️ Mode pantau — tanpa hak persetujuan",
                            color = SukaOnSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Anda bisa melihat antrean permintaan, tetapi keputusan persetujuan ada di Gudang Pusat (kitchen).",
                            color = SukaOnSurfaceVariant, fontSize = 11.sp, lineHeight = 15.sp,
                        )
                    }
                }
            }
        }
        if (state.daftarReview.isEmpty()) {
            item(key = "antrean-kosong") {
                KeadaanKosong("Tidak ada permintaan bahan baku yang menunggu persetujuan.")
            }
        } else {
            items(state.daftarReview, key = { it.id }) { p ->
                KartuAntrean(
                    p = p,
                    bahanMap = state.bahanMap,
                    budget = state.budgetPerOutlet[p.outletId],
                    estimasi = state.estimasiPerPermintaan[p.id] ?: 0.0,
                    onBuka = { viewModel.bukaApprove(p) },
                )
            }
        }
    }
}

@Composable
private fun KartuAntrean(
    p: Permintaan,
    bahanMap: Map<String, BahanBaku>,
    budget: BudgetStatus?,
    estimasi: Double,
    onBuka: () -> Unit,
) {
    Surface(
        Modifier.fillMaxWidth().clickable(onClick = onBuka),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFFFEDD5)) {
                    Text(
                        "MENUNGGU PERSETUJUAN",
                        Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        color = Oranye, fontSize = 8.sp, fontWeight = FontWeight.Black,
                    )
                }
                Spacer(Modifier.width(7.dp))
                Text(p.kodeReq, color = SukaOnSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("Periksa →", color = Oranye, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            if (budget != null) {
                Spacer(Modifier.height(5.dp))
                BadgeBudget(status = budget, proyeksi = estimasi, ringkas = true)
            }
            Spacer(Modifier.height(7.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    p.outletName ?: p.outletId,
                    color = SukaOnSurface, fontSize = 14.sp, fontWeight = FontWeight.Black,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                )
                p.pembuatNama?.let {
                    Text("Oleh: $it", color = SukaOnSurfaceVariant, fontSize = 10.sp)
                }
            }
            Text(
                "${p.items.size} jenis bahan baku · ${waktuSingkat(p.createdAt)}",
                color = SukaOnSurfaceVariant, fontSize = 10.sp,
            )
            if (p.omzetTarget > 0) {
                Spacer(Modifier.height(5.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Hijau.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, Hijau.copy(alpha = 0.25f)),
                ) {
                    Text(
                        "Potensi Omzet: ${formatRp(p.omzetTarget)}",
                        Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        color = Hijau, fontSize = 9.sp, fontWeight = FontWeight.Black,
                    )
                }
            }
            Spacer(Modifier.height(7.dp))
            p.items.take(3).forEach { item ->
                val bahan = bahanMap[item.bahanBakuId]
                Row(
                    Modifier.fillMaxWidth()
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                        .padding(horizontal = 9.dp, vertical = 5.dp),
                ) {
                    Text(
                        item.namaBahan ?: item.bahanBakuId,
                        Modifier.weight(1f),
                        color = SukaOnSurfaceVariant, fontSize = 11.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        qtyTersimpanTeks(item.qtyDiminta, bahan, item.satuan),
                        color = SukaOnSurface, fontSize = 11.sp, fontWeight = FontWeight.Black,
                    )
                }
                Spacer(Modifier.height(3.dp))
            }
            if (p.items.size > 3) {
                Text("+${p.items.size - 3} item lainnya…", color = Color(0xFF94A3B8), fontSize = 10.sp)
            }
        }
    }
}

// ============================================================== persetujuan

@Composable
private fun LayarPersetujuan(
    state: PermintaanUiState,
    p: Permintaan,
    viewModel: PermintaanViewModel,
) {
    var alasan by remember(p.id) { mutableStateOf("") }
    val adaLebih = viewModel.adaLebihStokGudang()

    Column(Modifier.fillMaxSize().background(SukaSurface)) {
        HeaderStok(
            judul = "Persetujuan Permintaan",
            subjudul = "${p.outletName ?: "Outlet"} · ${waktuSingkat(p.createdAt)}",
            onKembali = viewModel::tutupApprove,
        )
        state.pesan?.let { PitaPesan(it, false, viewModel::bersihkanPesan) }
        state.error?.let { PitaPesan(it, true, viewModel::bersihkanPesan) }

        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            if (p.targetJual.isNotEmpty()) {
                item(key = "target") {
                    Surface(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFFDF6EC),
                        border = BorderStroke(1.dp, Color(0xFFE7D8C6)),
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                "TARGET PENJUALAN",
                                color = Color(0xFF701604), fontSize = 10.sp,
                                fontWeight = FontWeight.Black, letterSpacing = 0.5.sp,
                            )
                            Spacer(Modifier.height(5.dp))
                            p.targetJual.forEach { t ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                                    Text(
                                        "${formatAngkaStok(t.qty)}x ${t.nama}",
                                        Modifier.weight(1f),
                                        color = SukaOnSurfaceVariant, fontSize = 11.sp,
                                    )
                                    Text(formatRp(t.omzet), color = SukaOnSurface, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            Spacer(Modifier.height(5.dp))
                            Row(Modifier.fillMaxWidth()) {
                                Text("Estimasi Omzet", Modifier.weight(1f), color = SukaOnSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(formatRp(p.omzetTarget), color = Hijau, fontSize = 11.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }

            items(p.items, key = { it.bahanBakuId }) { item ->
                val bahan = state.bahanMap[item.bahanBakuId]
                val satuan = formatSatuan(bahan?.satuanPesan ?: item.satuan)
                val qty = state.qtySetuju[item.bahanBakuId] ?: 0L
                val gudangBesar = viewModel.stokGudangBesar(item.bahanBakuId)
                val lebih = gudangBesar != null && viewModel.qtySetujuBase(item.bahanBakuId) > gudangBesar
                Surface(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, if (lebih) Color(0xFFFDBA74) else Color(0xFFF1F5F9)),
                ) {
                    Column(Modifier.padding(13.dp)) {
                        Text(
                            item.namaBahan ?: item.bahanBakuId,
                            color = SukaOnSurface, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        state.kebutuhanTarget[item.bahanBakuId]?.let { kebutuhan ->
                            Text(
                                "HPP Penggunaan: ${String.format(java.util.Locale.US, "%.2f", kebutuhan)} ${formatSatuan(item.satuan)} · " +
                                    "Pembulatan: ${ceil(kebutuhan).toLong()} ${formatSatuan(item.satuan)}",
                                color = SukaOnSurfaceVariant, fontSize = 9.sp,
                            )
                        }
                        Text(
                            "Diminta: ${qtyTersimpanTeks(item.qtyDiminta, bahan, item.satuan)}",
                            color = Color(0xFF701604), fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        )
                        val ccOutlet = state.stokOutlet[item.bahanBakuId]
                        val ccGudang = state.stokGudang[item.bahanBakuId]
                        when {
                            state.memuatCrosscheck -> Text("Memuat stok…", color = Color(0xFF94A3B8), fontSize = 10.sp)
                            ccOutlet == null && ccGudang == null ->
                                Text("(Stok tidak dapat dimuat)", color = Color(0xFF94A3B8), fontSize = 10.sp)
                            else -> {
                                val meta = bahan?.meta
                                val outletTeks = ccOutlet?.let {
                                    if (meta != null) formatTriUnitAdaptif(it.currentQty, it.saldoIsGram, meta)
                                    else "${formatAngkaStok(it.currentQty)} ${formatSatuan(item.satuan)}"
                                } ?: "-"
                                val gudangTeks = ccGudang?.let {
                                    if (meta != null) formatTriUnitAdaptif(it.currentQty, it.saldoIsGram, meta)
                                    else "${formatAngkaStok(it.currentQty)} ${formatSatuan(item.satuan)}"
                                } ?: "-"
                                Text(
                                    "Stok Outlet: $outletTeks | Stok Gudang: $gudangTeks",
                                    color = SukaOnSurfaceVariant, fontSize = 10.sp,
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (lebih) {
                                Text("⚠️", fontSize = 12.sp)
                                Spacer(Modifier.width(6.dp))
                            }
                            Box(Modifier.weight(1f)) {
                                StepperQty(
                                    qty = qty,
                                    satuan = satuan,
                                    onMinus = { viewModel.ubahQtySetuju(item.bahanBakuId, -1L) },
                                    onPlus = { viewModel.ubahQtySetuju(item.bahanBakuId, 1L) },
                                    onSet = { viewModel.setQtySetuju(item.bahanBakuId, it) },
                                    diSorot = lebih,
                                )
                            }
                        }
                    }
                }
            }

            item(key = "catatan-nol") {
                Text(
                    "Set qty 0 untuk menolak item tertentu",
                    color = Color(0xFF94A3B8), fontSize = 10.sp,
                )
            }

            val totalNilai = state.estimasiSetuju.totalNilai
            if (totalNilai > 0) {
                item(key = "total-nilai") {
                    Surface(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFDF6EC),
                    ) {
                        Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Total Nilai Permintaan",
                                    color = SukaOnSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    "Tahap Developer (Bisa Diabaikan)",
                                    color = Color(0xFF92400E), fontSize = 8.sp, fontWeight = FontWeight.Black,
                                )
                            }
                            Text(
                                formatRp(totalNilai),
                                color = Color(0xFF701604), fontSize = 13.sp, fontWeight = FontWeight.Black,
                            )
                        }
                    }
                }
            }

            if (state.budgetPerOutlet[p.outletId] != null) {
                item(key = "budget-approve") {
                    BadgeBudget(
                        status = state.budgetPerOutlet[p.outletId],
                        proyeksi = state.estimasiSetuju.totalNilai,
                    )
                }
            }

            val pecahan = viewModel.bahanDimintaPecahan()
            if (pecahan.isNotEmpty()) {
                item(key = "peringatan-pecahan") {
                    Surface(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFFFBEB),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                    ) {
                        Text(
                            "⚠️ ${pecahan.joinToString(", ")} diminta dalam satuan lebih kecil " +
                                "(dibuat dari web). Aplikasi ini hanya bisa menyetujui satuan besar penuh, " +
                                "jadi jumlah di atas LEBIH BANYAK dari yang diminta. Proses lewat web bila " +
                                "ingin mengirim persis sejumlah permintaannya.",
                            Modifier.padding(11.dp),
                            color = Color(0xFF92400E), fontSize = 11.sp, lineHeight = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            if (adaLebih) {
                item(key = "peringatan-gudang") {
                    Surface(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFFF7ED),
                        border = BorderStroke(1.dp, Color(0xFFFED7AA)),
                    ) {
                        Text(
                            "⚠️ Beberapa item melebihi stok gudang. Mohon periksa kembali.",
                            Modifier.padding(11.dp),
                            color = Color(0xFFC2410C), fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            if (!state.bolehApprove) {
                item(key = "pantau-info") {
                    Surface(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF5EDE3),
                        border = BorderStroke(1.dp, Color(0xFFE7D8C6)),
                    ) {
                        Text(
                            "👁️ Anda hanya memantau. Keputusan setujui/tolak ada di Gudang Pusat (kitchen), " +
                                "admin, atau owner — sebab persetujuan langsung menerbitkan surat jalan.",
                            Modifier.padding(11.dp),
                            color = SukaOnSurfaceVariant, fontSize = 11.sp, lineHeight = 15.sp,
                        )
                    }
                }
            }

            item(key = "alasan") {
                OutlinedTextField(
                    value = alasan,
                    onValueChange = { alasan = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text("Alasan penolakan (wajib jika menolak seluruh permintaan)", fontSize = 12.sp)
                    },
                    singleLine = false,
                    enabled = !state.mengirim,
                    shape = RoundedCornerShape(12.dp),
                )
            }
        }

        Surface(color = Color.White, shadowElevation = 8.dp) {
            Row(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = viewModel::tutupApprove,
                    modifier = Modifier.weight(1f),
                    enabled = !state.mengirim,
                    shape = RoundedCornerShape(13.dp),
                ) { Text("Batal", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SukaOnSurfaceVariant) }
                OutlinedButton(
                    onClick = { viewModel.tolak(alasan) },
                    modifier = Modifier.weight(1f),
                    enabled = !state.mengirim && state.bolehApprove,
                    shape = RoundedCornerShape(13.dp),
                    border = BorderStroke(1.dp, Merah.copy(alpha = 0.4f)),
                ) { Text("Tolak", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Merah) }
                Button(
                    onClick = viewModel::setujui,
                    modifier = Modifier.weight(1f),
                    enabled = !state.mengirim && state.bolehApprove,
                    colors = ButtonDefaults.buttonColors(containerColor = Hijau),
                    shape = RoundedCornerShape(13.dp),
                ) {
                    Text(
                        if (state.mengirim) "Memproses…" else "Setujui",
                        fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

// ================================================================== dialog

@Composable
private fun DialogNudge(state: PermintaanUiState, viewModel: PermintaanViewModel) {
    AlertDialog(
        onDismissRequest = viewModel::tutupNudge,
        title = {
            Text("Masih ada permintaan yang menunggu", fontSize = 15.sp, fontWeight = FontWeight.Black)
        },
        text = {
            Text(
                "Anda memiliki ${state.pendingItemIds.size} item bahan baku lain yang masih menunggu " +
                    "persetujuan kitchen. Mau gabungkan dengan bahan lain dulu atau kirim sekarang?",
                fontSize = 12.sp, lineHeight = 17.sp,
            )
        },
        confirmButton = {
            Button(
                onClick = viewModel::lanjutKirimDariNudge,
                colors = ButtonDefaults.buttonColors(containerColor = Oranye),
                shape = RoundedCornerShape(11.dp),
            ) { Text("Kirim Sekarang", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            OutlinedButton(
                onClick = viewModel::tambahDuluDariNudge,
                shape = RoundedCornerShape(11.dp),
            ) { Text("Tambah Dulu", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SukaOnSurfaceVariant) }
        },
    )
}

@Composable
private fun DialogKonfirmasi(state: PermintaanUiState, viewModel: PermintaanViewModel) {
    val items = state.keranjangItems
    val estimasi = state.estimasi
    AlertDialog(
        onDismissRequest = { if (!state.mengirim) viewModel.tutupKonfirmasi() },
        title = { Text("Kirim Permintaan Bahan?", fontSize = 15.sp, fontWeight = FontWeight.Black) },
        text = {
            Column {
                Text(
                    "Total ${items.size} item bahan baku akan diajukan ke Kitchen / Gudang.",
                    fontSize = 12.sp, lineHeight = 16.sp,
                )
                Spacer(Modifier.height(9.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                ) {
                    items.take(8).forEach { b ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            Text(
                                b.bahan.nama,
                                Modifier.weight(1f),
                                fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                "${b.qty} ${formatSatuan(b.bahan.satuanPesan)}",
                                fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                            )
                        }
                    }
                    if (items.size > 8) {
                        Text("+${items.size - 8} bahan lain", color = Color(0xFF94A3B8), fontSize = 10.sp)
                    }
                }
                if (estimasi.totalNilai > 0) {
                    Spacer(Modifier.height(7.dp))
                    estimasi.kategoriNilai.forEach { (kat, nilai) ->
                        Row(Modifier.fillMaxWidth()) {
                            Text(kat, Modifier.weight(1f), fontSize = 10.sp, color = SukaOnSurfaceVariant)
                            Text(formatRp(nilai), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        Text("Total Estimasi", Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            formatRp(estimasi.totalNilai),
                            fontSize = 12.sp, fontWeight = FontWeight.Black, color = Oranye,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = viewModel::kirimPermintaan,
                enabled = !state.mengirim,
                colors = ButtonDefaults.buttonColors(containerColor = Oranye),
                shape = RoundedCornerShape(11.dp),
            ) {
                Text(
                    if (state.mengirim) "Mengirim…" else "Ya, Kirim Sekarang",
                    fontSize = 12.sp, fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = viewModel::tutupKonfirmasi,
                enabled = !state.mengirim,
                shape = RoundedCornerShape(11.dp),
            ) { Text("Batal", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SukaOnSurfaceVariant) }
        },
    )
}
