package com.sukashawarma.superapp.feature.manager.ui.hpp

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.feature.manager.domain.BarisResep
import com.sukashawarma.superapp.feature.manager.domain.KelompokMenu
import com.sukashawarma.superapp.feature.manager.domain.MenuHpp
import com.sukashawarma.superapp.feature.manager.domain.ResepMenu
import com.sukashawarma.superapp.feature.manager.domain.RingkasanHpp
import com.sukashawarma.superapp.feature.manager.domain.cacah
import com.sukashawarma.superapp.feature.manager.domain.rupiah
import com.sukashawarma.superapp.feature.manager.ui.BarProgres
import com.sukashawarma.superapp.feature.manager.ui.GarisKartu
import com.sukashawarma.superapp.feature.manager.ui.HijauGaris
import com.sukashawarma.superapp.feature.manager.ui.HijauLatar
import com.sukashawarma.superapp.feature.manager.ui.HijauTeks
import com.sukashawarma.superapp.feature.manager.ui.KartuPanel
import com.sukashawarma.superapp.feature.manager.ui.MerahGaris
import com.sukashawarma.superapp.feature.manager.ui.MerahLatar
import com.sukashawarma.superapp.feature.manager.ui.MerahTeks
import com.sukashawarma.superapp.feature.manager.ui.PanelKosong
import com.sukashawarma.superapp.presentation.theme.SukaBrown
import com.sukashawarma.superapp.presentation.theme.SukaCream
import com.sukashawarma.superapp.presentation.theme.SukaGray400
import com.sukashawarma.superapp.presentation.theme.SukaOrange

private val AMBER_LATAR = Color(0xFFFEF3C7)
private val AMBER_GARIS = Color(0xFFFCD34D)
private val AMBER_TEKS = Color(0xFF78350F)

private fun persenTeks(nilai: Double?): String =
    nilai?.let { String.format(java.util.Locale.US, "%.1f%%", it) } ?: "—"

/**
 * Resep & HPP — cermin `app/resep/` web.
 *
 * Layar ini KHUSUS regional manager: `app/resep/page.tsx` me-redirect area manager
 * ke beranda, dan navigasi web menandainya `excludedRoles: ['area_manager']`.
 * Gerbangnya ditegakkan di graph navigasi, bukan di sini.
 *
 * Seluruhnya baca-saja. Penyuntingan resep tetap di web — policy tulis
 * `resep`/`resep_item` hanya untuk admin, bukan manajer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HppScreen(
    onExit: () -> Unit,
    viewModel: HppViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        containerColor = SukaCream,
        topBar = {
            TopAppBar(
                title = {
                    Text("Resep & HPP", fontWeight = FontWeight.Black, fontSize = 17.sp, color = SukaBrown, maxLines = 1)
                },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = SukaBrown)
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::muatUlang) {
                        Icon(Icons.Default.Refresh, "Muat ulang", tint = SukaBrown)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { PanelKepala(state, viewModel) }
            if (state.galat != null) {
                item { PanelGalat(state.galat!!) }
            }
            when (state.tab) {
                TabHpp.ANALISIS -> isiAnalisis(state)
                TabHpp.RESEP -> isiResep(state, viewModel)
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }

    state.menuTerbuka?.let { menu ->
        DialogResep(menu, state.resepTerbuka, viewModel::tutupResep)
    }
}

@Composable
private fun PanelKepala(state: HppUiState, viewModel: HppViewModel) {
    KartuPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Resep & HPP", color = SukaBrown, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(3.dp))
                Text(
                    "Pantau Bill of Materials dan Harga Pokok Penjualan semua menu.",
                    color = SukaGray400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 16.sp,
                )
            }
            if (state.memuat) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = SukaOrange)
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(
            Modifier.fillMaxWidth().background(SukaBrown.copy(alpha = 0.04f), RoundedCornerShape(14.dp)).padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TabHpp.entries.forEach { tab ->
                TombolTab(
                    label = if (tab == TabHpp.RESEP) "${tab.label} (${state.semuaMenu.size})" else tab.label,
                    ikon = if (tab == TabHpp.ANALISIS) Icons.Default.TrendingUp else Icons.Default.MenuBook,
                    terpilih = state.tab == tab,
                    modifier = Modifier.weight(1f),
                ) { viewModel.pilihTab(tab) }
            }
        }
    }
}

@Composable
private fun TombolTab(
    label: String,
    ikon: androidx.compose.ui.graphics.vector.ImageVector,
    terpilih: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(11.dp),
        color = if (terpilih) SukaOrange else Color.Transparent,
    ) {
        Row(
            Modifier.padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                ikon,
                null,
                tint = if (terpilih) Color.White else SukaBrown.copy(alpha = 0.7f),
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                label,
                color = if (terpilih) Color.White else SukaBrown.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PanelGalat(pesan: String) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MerahLatar,
        border = BorderStroke(1.dp, MerahGaris),
    ) {
        Text(pesan, Modifier.padding(14.dp), color = MerahTeks, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

/* ----------------------------------------------------------------------- */
/* Tab analisis                                                             */
/* ----------------------------------------------------------------------- */

private fun LazyListScope.isiAnalisis(state: HppUiState) {
    val r = state.ringkasan
    item { KartuRingkasan(r) }
    if (r.perluDitengok.isEmpty()) {
        item {
            KartuPanel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TrendingUp, null, tint = HijauTeks, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(9.dp))
                    Text(
                        if (state.memuat) {
                            "Memuat katalog menu..."
                        } else {
                            "Tidak ada menu dengan foodcost di atas 40%."
                        },
                        color = SukaBrown,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        return
    }

    item {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
            Icon(Icons.Default.WarningAmber, null, tint = AMBER_TEKS, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(7.dp))
            Text(
                "PERLU DITENGOK — FOODCOST DI ATAS 40%",
                color = SukaBrown.copy(alpha = 0.6f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.7.sp,
            )
        }
    }
    items(r.perluDitengok, key = { "tengok-${it.id}" }) { menu -> KartuMenu(menu, onKlik = null) }
}

@Composable
private fun KartuRingkasan(r: RingkasanHpp) {
    KartuPanel {
        Text(
            "ANALISIS HPP & DISTRIBUSI",
            color = SukaGray400,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.7.sp,
        )
        Spacer(Modifier.height(12.dp))
        Row {
            AngkaRingkas("Menu terdaftar", cacah(r.jumlahMenu), Modifier.weight(1f))
            AngkaRingkas("Punya HPP", cacah(r.jumlahBerResep), Modifier.weight(1f))
        }
        Spacer(Modifier.height(14.dp))
        HorizontalDivider(color = GarisKartu)
        Spacer(Modifier.height(14.dp))

        Text(
            "RATA-RATA FOODCOST",
            color = SukaGray400,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.7.sp,
        )
        Spacer(Modifier.height(5.dp))
        Text(
            persenTeks(r.rataRataFoodcost),
            color = if ((r.rataRataFoodcost ?: 0.0) > 40.0) MerahTeks else HijauTeks,
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
        )
        Spacer(Modifier.height(6.dp))
        BarProgres(
            rasio = ((r.rataRataFoodcost ?: 0.0) / 100).toFloat(),
            tinggi = 6,
            sorot = (r.rataRataFoodcost ?: 0.0) <= 40.0,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Dihitung dari menu yang punya harga jual dan HPP",
            color = SukaGray400,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
        )

        r.foodcostTertinggi?.let {
            Spacer(Modifier.height(14.dp))
            BarisEkstrem("Foodcost tertinggi", it, MerahTeks)
        }
        r.foodcostTerendah?.let {
            Spacer(Modifier.height(8.dp))
            BarisEkstrem("Foodcost terendah", it, HijauTeks)
        }
    }
}

@Composable
private fun AngkaRingkas(label: String, nilai: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label.uppercase(), color = SukaGray400, fontSize = 9.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(4.dp))
        Text(nilai, color = SukaBrown, fontSize = 20.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun BarisEkstrem(label: String, menu: MenuHpp, warna: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, color = SukaGray400, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(
                menu.nama,
                color = SukaBrown,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(persenTeks(menu.foodcostPersen), color = warna, fontSize = 14.sp, fontWeight = FontWeight.Black)
    }
}

/* ----------------------------------------------------------------------- */
/* Tab resep BOM                                                            */
/* ----------------------------------------------------------------------- */

private fun LazyListScope.isiResep(state: HppUiState, viewModel: HppViewModel) {
    item { PanelPenyaringMenu(state, viewModel) }

    if (state.menuTerlihat.isEmpty()) {
        item {
            KartuPanel {
                PanelKosong(
                    if (state.memuat) "Memuat katalog menu..." else "Tidak ada menu yang cocok."
                )
            }
        }
        return
    }

    items(state.menuTerlihat, key = { it.id }) { menu ->
        KartuMenu(menu, onKlik = { viewModel.bukaResep(menu) })
    }
}

@Composable
private fun PanelPenyaringMenu(state: HppUiState, viewModel: HppViewModel) {
    KartuPanel {
        OutlinedTextField(
            value = state.pencarian,
            onValueChange = viewModel::ubahPencarian,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Cari menu atau kategori...", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = SukaGray400, modifier = Modifier.size(18.dp)) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
        )
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ChipKelompok("Semua (${state.semuaMenu.size})", state.kelompok == null) {
                viewModel.pilihKelompok(null)
            }
            state.kelompokTersedia.forEach { k ->
                ChipKelompok(
                    "${k.ikon} ${k.singkat} (${state.jumlahDalamKelompok(k)})",
                    state.kelompok == k,
                ) { viewModel.pilihKelompok(k) }
            }
        }
    }
}

@Composable
private fun ChipKelompok(label: String, aktif: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (aktif) SukaOrange else Color.White,
        border = BorderStroke(1.dp, if (aktif) SukaOrange else SukaBrown.copy(alpha = 0.15f)),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            label,
            Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            color = if (aktif) Color.White else SukaBrown.copy(alpha = 0.75f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
        )
    }
}

@Composable
private fun KartuMenu(menu: MenuHpp, onKlik: (() -> Unit)?) {
    val modifier = if (onKlik != null) Modifier.clickable(onClick = onKlik) else Modifier
    Surface(
        modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        border = BorderStroke(1.dp, GarisKartu),
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(
                        menu.nama,
                        color = SukaBrown,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        lineHeight = 18.sp,
                    )
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${menu.kelompok.ikon} ${menu.kelompok.singkat}",
                            color = SukaGray400,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        if (menu.paket) {
                            Spacer(Modifier.width(6.dp))
                            LencanaKecil("PAKET", SukaOrange.copy(alpha = 0.10f), SukaOrange)
                        }
                        if (!menu.tersedia) {
                            Spacer(Modifier.width(6.dp))
                            LencanaKecil("NONAKTIF", SukaBrown.copy(alpha = 0.05f), SukaGray400)
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(rupiah(menu.hargaJual), color = SukaBrown, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    Text("harga jual", color = SukaGray400, fontSize = 9.sp, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = GarisKartu)
            Spacer(Modifier.height(10.dp))

            // Cabang ditulis eksplisit if/else, BUKAN keluar-awal `return@Column`.
            // `Column` adalah fungsi inline: keluar dari lambdanya setelah sudah
            // memancarkan composable meninggalkan pembukuan grup kompilator tidak
            // seimbang, dan begitu keadaannya berbalik, tabel slot dibaca dengan
            // indeks negatif -> ArrayIndexOutOfBoundsException di SlotTableKt.key.
            val hpp = menu.hpp
            if (hpp == null) {
                Text(
                    if (menu.paket) "Komponen paket belum punya HPP" else "Resep belum tersedia",
                    color = SukaGray400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                Row {
                    KolomAngka("HPP", rupiah(hpp), SukaBrown, Modifier.weight(1f))
                    KolomAngka(
                        "Margin",
                        rupiah(menu.marginRp ?: 0L),
                        if ((menu.marginRp ?: 0L) >= 0) HijauTeks else MerahTeks,
                        Modifier.weight(1f),
                    )
                    KolomAngka(
                        "Foodcost",
                        persenTeks(menu.foodcostPersen),
                        if (menu.foodcostTinggi) MerahTeks else HijauTeks,
                        Modifier.weight(1f),
                    )
                }

                if (menu.hppOverride != null || menu.parsial) {
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (menu.hppOverride != null) {
                            LencanaKecil("HPP MANUAL", AMBER_LATAR, AMBER_TEKS)
                        }
                        if (menu.parsial) {
                            LencanaKecil("HPP PARSIAL", MerahLatar, MerahTeks)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KolomAngka(label: String, nilai: String, warna: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label.uppercase(), color = SukaGray400, fontSize = 8.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(3.dp))
        Text(
            nilai,
            color = warna,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LencanaKecil(teks: String, latar: Color, warna: Color) {
    Surface(shape = RoundedCornerShape(6.dp), color = latar) {
        Text(
            teks,
            Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            color = warna,
            fontSize = 8.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.4.sp,
        )
    }
}

@Composable
private fun DialogResep(menu: MenuHpp, resep: ResepMenu?, onTutup: () -> Unit) {
    Dialog(onDismissRequest = onTutup) {
        Surface(shape = RoundedCornerShape(24.dp), color = Color.White) {
            Column(Modifier.heightIn(max = 520.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            menu.nama,
                            color = SukaBrown,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "${menu.kelompok.ikon} ${menu.kelompok.singkat}",
                            color = SukaGray400,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    IconButton(onClick = onTutup) {
                        Icon(Icons.Default.Close, "Tutup", tint = SukaBrown)
                    }
                }
                HorizontalDivider(color = GarisKartu)

                Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                    when {
                        menu.paket -> Text(
                            "Menu paket. HPP-nya dirakit dari HPP tiap komponen, " +
                                "bukan dari resep bahan sendiri.",
                            color = SukaGray400,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        resep == null || resep.baris.isEmpty() -> Text(
                            "Resep untuk menu ini belum diisi.",
                            color = SukaGray400,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        else -> {
                            resep.baris.forEach { baris -> BarisBahan(baris) }
                            Spacer(Modifier.height(10.dp))
                            HorizontalDivider(color = GarisKartu)
                            Spacer(Modifier.height(10.dp))
                            BarisTotal("Total bahan", rupiah(resep.totalBahan), SukaBrown)
                            if (resep.buffer > 0) {
                                Spacer(Modifier.height(4.dp))
                                BarisTotal("Buffer", rupiah(resep.buffer), SukaGray400)
                            }
                            Spacer(Modifier.height(6.dp))
                            BarisTotal("Total HPP", rupiah(resep.totalHpp), SukaOrange, besar = true)
                            if (resep.adaBahanTanpaHarga) {
                                Spacer(Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = AMBER_LATAR,
                                    border = BorderStroke(1.dp, AMBER_GARIS),
                                ) {
                                    Text(
                                        "Sebagian bahan belum punya harga beli, jadi HPP di atas " +
                                            "masih lebih rendah dari yang sebenarnya.",
                                        Modifier.padding(10.dp),
                                        color = AMBER_TEKS,
                                        fontSize = 10.sp,
                                        lineHeight = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }

                    if (menu.hppOverride != null) {
                        Spacer(Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = HijauLatar,
                            border = BorderStroke(1.dp, HijauGaris),
                        ) {
                            Text(
                                "HPP yang dipakai POS ditulis manual: ${rupiah(menu.hppOverride!!)}.",
                                Modifier.padding(10.dp),
                                color = HijauTeks,
                                fontSize = 10.sp,
                                lineHeight = 15.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BarisBahan(baris: BarisResep) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                baris.bahanNama,
                color = SukaBrown,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${baris.qtyTeks} ${baris.satuan}",
                color = SukaGray400,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Spacer(Modifier.width(8.dp))
        if (baris.adaHarga) {
            Text(rupiah(baris.subtotal), color = SukaBrown, fontSize = 12.sp, fontWeight = FontWeight.Black)
        } else {
            Box(
                Modifier.background(AMBER_LATAR, RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text("tanpa harga", color = AMBER_TEKS, fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun BarisTotal(label: String, nilai: String, warna: Color, besar: Boolean = false) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            Modifier.weight(1f),
            color = if (besar) SukaBrown else SukaGray400,
            fontSize = if (besar) 12.sp else 11.sp,
            fontWeight = if (besar) FontWeight.Black else FontWeight.Bold,
        )
        Text(
            nilai,
            color = warna,
            fontSize = if (besar) 16.sp else 12.sp,
            fontWeight = FontWeight.Black,
        )
    }
}
