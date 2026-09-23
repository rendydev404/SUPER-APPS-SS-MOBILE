package com.sukashawarma.superapp.feature.manager.ui.hpp

import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.RealtimeTables
import com.sukashawarma.superapp.core.ui.ios.AngkaIos
import com.sukashawarma.superapp.core.ui.ios.BarisIos
import com.sukashawarma.superapp.core.ui.ios.BilahJudulIos
import com.sukashawarma.superapp.core.ui.ios.BlokAngkaIos
import com.sukashawarma.superapp.core.ui.ios.GrupIos
import com.sukashawarma.superapp.core.ui.ios.IkonBulatIos
import com.sukashawarma.superapp.core.ui.ios.KapsulPilihanIos
import com.sukashawarma.superapp.core.ui.ios.KartuIos
import com.sukashawarma.superapp.core.ui.ios.KolomCariIos
import com.sukashawarma.superapp.core.ui.ios.LabelSeksiIos
import com.sukashawarma.superapp.core.ui.ios.LencanaIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.PanelGalatIos
import com.sukashawarma.superapp.core.ui.ios.PemisahIos
import com.sukashawarma.superapp.core.ui.ios.SegmenIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.TombolBundarIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WadahSegmenIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.core.ui.kaca.denganRuangNav
import com.sukashawarma.superapp.feature.manager.domain.BarisResep
import com.sukashawarma.superapp.feature.manager.domain.MenuHpp
import com.sukashawarma.superapp.feature.manager.domain.ResepMenu
import com.sukashawarma.superapp.feature.manager.domain.RingkasanHpp
import com.sukashawarma.superapp.feature.manager.domain.cacah
import com.sukashawarma.superapp.feature.manager.domain.rupiah
import com.sukashawarma.superapp.feature.manager.ui.BarProgres
import com.sukashawarma.superapp.feature.manager.ui.JudulPanel
import com.sukashawarma.superapp.feature.manager.ui.KartuPanel
import com.sukashawarma.superapp.feature.manager.ui.PanelKosong

private fun persenTeks(nilai: Double?): String =
    nilai?.let { String.format(java.util.Locale.US, "%.1f%%", it) } ?: "—"

/** Warna angka sehat/bermasalah — teks nada iOS, lebih gelap dari warna isiannya agar terbaca. */
private val TeksBaik = NadaIos.SUKSES.teks
private val TeksBuruk = NadaIos.BAHAYA.teks

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

    // Sama dengan HPP Menu di modul Stok: resep dinilai ulang tiap harga bahan berubah.
    RealtimeRefresh(RealtimeTables.BAHAN_BAKU_HARGA, RealtimeTables.BAHAN_BAKU) { viewModel.muatUlang(silent = true) }

    Scaffold(
        containerColor = WarnaIos.Latar,
        topBar = {
            BilahJudulIos(
                judul = "Resep & HPP",
                onKembali = onExit,
                aksi = { TombolBundarIos(IkonIos.Refresh, "Muat ulang", viewModel::muatUlang) },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(
                start = UkuranIos.TepiLayar,
                end = UkuranIos.TepiLayar,
                top = 12.dp,
                bottom = 16.dp,
            ).denganRuangNav(),
            verticalArrangement = Arrangement.spacedBy(UkuranIos.JarakKartu),
        ) {
            item { PanelKepala(state, viewModel) }
            if (state.galat != null) {
                item { PanelGalatIos(state.galat!!) }
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
        Text("Resep & HPP", style = TipeIos.Judul3)
        Spacer(Modifier.height(2.dp))
        Text("Pantau Bill of Materials dan Harga Pokok Penjualan semua menu.", style = TipeIos.Catatan)

        Spacer(Modifier.height(14.dp))
        WadahSegmenIos {
            TabHpp.entries.forEach { tab ->
                SegmenIos(
                    label = if (tab == TabHpp.RESEP) "${tab.label} (${state.semuaMenu.size})" else tab.label,
                    aktif = state.tab == tab,
                    onKlik = { viewModel.pilihTab(tab) },
                    modifier = Modifier.weight(1f),
                    ikon = if (tab == TabHpp.ANALISIS) IkonIos.TrendingUp else IkonIos.MenuBook,
                )
            }
        }
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
                    IkonBulatIos(IkonIos.TrendingUp, WarnaIos.Hijau, ukuran = 26.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (state.memuat) {
                            "Memuat katalog menu..."
                        } else {
                            "Tidak ada menu dengan foodcost di atas 40%."
                        },
                        style = TipeIos.SubJudul.copy(color = WarnaIos.Label),
                    )
                }
            }
        }
        return
    }

    item {
        Row(
            Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(IkonIos.WarningAmber, null, tint = NadaIos.PERINGATAN.warna, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
            LabelSeksiIos("Perlu ditengok — foodcost di atas 40%")
        }
    }
    items(r.perluDitengok, key = { "tengok-${it.id}" }) { menu -> KartuMenu(menu, onKlik = null) }
}

@Composable
private fun KartuRingkasan(r: RingkasanHpp) {
    val rataRata = r.rataRataFoodcost ?: 0.0
    KartuPanel {
        JudulPanel("Analisis HPP & Distribusi")
        BlokAngkaIos(
            listOf(
                AngkaIos("Menu terdaftar", cacah(r.jumlahMenu)),
                AngkaIos("Punya HPP", cacah(r.jumlahBerResep)),
            ),
        )

        Spacer(Modifier.height(16.dp))
        Text("Rata-rata foodcost", style = TipeIos.Catatan.copy(fontWeight = FontWeight.SemiBold))
        Spacer(Modifier.height(2.dp))
        Text(
            persenTeks(r.rataRataFoodcost),
            style = TipeIos.AngkaBesar.copy(color = if (rataRata > 40.0) TeksBuruk else TeksBaik),
        )
        Spacer(Modifier.height(8.dp))
        BarProgres(
            rasio = (rataRata / 100).toFloat(),
            tinggi = 6,
            sorot = rataRata <= 40.0,
        )
        Spacer(Modifier.height(6.dp))
        Text("Dihitung dari menu yang punya harga jual dan HPP", style = TipeIos.Kecil)

        if (r.foodcostTertinggi != null || r.foodcostTerendah != null) {
            Spacer(Modifier.height(14.dp))
            Column(Modifier.fillMaxWidth().clip(UkuranIos.SudutBlok).background(WarnaIos.Latar)) {
                r.foodcostTertinggi?.let {
                    BarisEkstrem("Foodcost tertinggi", it, TeksBuruk)
                }
                if (r.foodcostTertinggi != null && r.foodcostTerendah != null) {
                    PemisahIos(inset = 14.dp)
                }
                r.foodcostTerendah?.let {
                    BarisEkstrem("Foodcost terendah", it, TeksBaik)
                }
            }
        }
    }
}

@Composable
private fun BarisEkstrem(label: String, menu: MenuHpp, warna: Color) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = TipeIos.Kecil)
            Text(
                menu.nama,
                style = TipeIos.Keterangan.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(persenTeks(menu.foodcostPersen), color = warna, fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
        KolomCariIos(
            nilai = state.pencarian,
            onUbah = viewModel::ubahPencarian,
            modifier = Modifier.fillMaxWidth(),
            placeholder = "Cari menu atau kategori...",
        )
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            KapsulPilihanIos("Semua (${state.semuaMenu.size})", state.kelompok == null, {
                viewModel.pilihKelompok(null)
            })
            state.kelompokTersedia.forEach { k ->
                KapsulPilihanIos(
                    "${k.ikon} ${k.singkat} (${state.jumlahDalamKelompok(k)})",
                    state.kelompok == k,
                    { viewModel.pilihKelompok(k) },
                )
            }
        }
    }
}

@Composable
private fun KartuMenu(menu: MenuHpp, onKlik: (() -> Unit)?) {
    KartuIos(onKlik = onKlik) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(menu.nama, style = TipeIos.Utama)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${menu.kelompok.ikon} ${menu.kelompok.singkat}", style = TipeIos.Catatan)
                    if (menu.paket) {
                        Spacer(Modifier.width(6.dp))
                        LencanaIos("Paket", NadaIos.AKSEN, titik = false)
                    }
                    if (!menu.tersedia) {
                        Spacer(Modifier.width(6.dp))
                        LencanaIos("Nonaktif", NadaIos.NETRAL, titik = false)
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(rupiah(menu.hargaJual), style = TipeIos.Keterangan.copy(fontWeight = FontWeight.SemiBold))
                Text("harga jual", style = TipeIos.Kecil)
            }
        }

        Spacer(Modifier.height(12.dp))

        // Cabang ditulis eksplisit if/else, BUKAN keluar-awal `return@Column`.
        // `Column` adalah fungsi inline: keluar dari lambdanya setelah sudah
        // memancarkan composable meninggalkan pembukuan grup kompilator tidak
        // seimbang, dan begitu keadaannya berbalik, tabel slot dibaca dengan
        // indeks negatif -> ArrayIndexOutOfBoundsException di SlotTableKt.key.
        val hpp = menu.hpp
        if (hpp == null) {
            Text(
                if (menu.paket) "Komponen paket belum punya HPP" else "Resep belum tersedia",
                style = TipeIos.Catatan,
            )
        } else {
            BlokTigaAngka(
                Triple("HPP", rupiah(hpp), WarnaIos.Label),
                Triple(
                    "Margin",
                    rupiah(menu.marginRp ?: 0L),
                    if ((menu.marginRp ?: 0L) >= 0) TeksBaik else TeksBuruk,
                ),
                Triple(
                    "Foodcost",
                    persenTeks(menu.foodcostPersen),
                    if (menu.foodcostTinggi) TeksBuruk else TeksBaik,
                ),
            )

            if (menu.hppOverride != null || menu.parsial) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (menu.hppOverride != null) {
                        LencanaIos("HPP manual", NadaIos.PERINGATAN, titik = false)
                    }
                    if (menu.parsial) {
                        LencanaIos("HPP parsial", NadaIos.BAHAYA, titik = false)
                    }
                }
            }
        }
    }
}

/**
 * Blok abu bersekat tiga angka HPP / Margin / Foodcost.
 *
 * Bukan [BlokAngkaIos]: angka rupiah di sepertiga lebar kartu terpotong pada
 * ukuran 20sp miliknya, dan margin perlu hijau saat untung — BlokAngkaIos hanya
 * mengenal hitam dan merah.
 */
@Composable
private fun BlokTigaAngka(vararg kolom: Triple<String, String, Color>) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(UkuranIos.SudutBlok)
            .background(WarnaIos.Latar)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        kolom.forEachIndexed { i, (label, nilai, warna) ->
            if (i > 0) Box(Modifier.width(0.5.dp).height(30.dp).background(WarnaIos.Pemisah))
            Column(
                Modifier.weight(1f).padding(horizontal = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(label, style = TipeIos.Kecil.copy(fontWeight = FontWeight.Medium), maxLines = 1)
                Spacer(Modifier.height(2.dp))
                Text(
                    nilai,
                    color = warna,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun DialogResep(menu: MenuHpp, resep: ResepMenu?, onTutup: () -> Unit) {
    Dialog(onDismissRequest = onTutup) {
        Surface(shape = UkuranIos.SudutKartu, color = WarnaIos.Latar) {
            Column(Modifier.heightIn(max = 520.dp)) {
                Row(
                    Modifier.padding(start = 16.dp, end = 12.dp, top = 14.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            menu.nama,
                            style = TipeIos.Utama,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text("${menu.kelompok.ikon} ${menu.kelompok.singkat}", style = TipeIos.Catatan)
                    }
                    Spacer(Modifier.width(8.dp))
                    TombolBundarIos(IkonIos.Close, "Tutup", onTutup, warnaIkon = WarnaIos.LabelKedua)
                }
                PemisahIos(inset = 0.dp)

                Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                    when {
                        menu.paket -> Text(
                            "Menu paket. HPP-nya dirakit dari HPP tiap komponen, " +
                                "bukan dari resep bahan sendiri.",
                            style = TipeIos.SubJudul,
                        )
                        resep == null || resep.baris.isEmpty() -> Text(
                            "Resep untuk menu ini belum diisi.",
                            style = TipeIos.SubJudul,
                        )
                        else -> {
                            GrupIos {
                                resep.baris.forEachIndexed { i, baris ->
                                    if (i > 0) PemisahIos()
                                    BarisBahan(baris)
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            GrupIos {
                                BarisIos("Total bahan", nilai = rupiah(resep.totalBahan))
                                if (resep.buffer > 0) {
                                    PemisahIos()
                                    BarisIos("Buffer", nilai = rupiah(resep.buffer))
                                }
                                PemisahIos()
                                BarisTotalHpp(rupiah(resep.totalHpp))
                            }
                            if (resep.adaBahanTanpaHarga) {
                                Spacer(Modifier.height(12.dp))
                                CatatanBernada(
                                    "Sebagian bahan belum punya harga beli, jadi HPP di atas " +
                                        "masih lebih rendah dari yang sebenarnya.",
                                    NadaIos.PERINGATAN,
                                )
                            }
                        }
                    }

                    if (menu.hppOverride != null) {
                        Spacer(Modifier.height(12.dp))
                        CatatanBernada(
                            "HPP yang dipakai POS ditulis manual: ${rupiah(menu.hppOverride!!)}.",
                            NadaIos.SUKSES,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BarisBahan(baris: BarisResep) {
    BarisIos(
        judul = baris.bahanNama,
        keterangan = "${baris.qtyTeks} ${baris.satuan}",
        nilai = if (baris.adaHarga) rupiah(baris.subtotal) else null,
        trailing = if (baris.adaHarga) {
            null
        } else {
            { LencanaIos("tanpa harga", NadaIos.PERINGATAN, titik = false) }
        },
    )
}

/** Baris penutup rincian: total HPP ditebalkan beraksen karena itu angka yang dicari. */
@Composable
private fun BarisTotalHpp(nilai: String) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Total HPP", Modifier.weight(1f), style = TipeIos.Utama)
        Text(nilai, color = WarnaIos.Aksen, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

/** Catatan kecil berlatar nada tipis, pengganti kotak berbingkai di dalam dialog. */
@Composable
private fun CatatanBernada(teks: String, nada: NadaIos) {
    Text(
        teks,
        Modifier
            .fillMaxWidth()
            .clip(UkuranIos.SudutBlok)
            .background(nada.warna.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        style = TipeIos.Catatan.copy(color = nada.teks, lineHeight = 18.sp),
    )
}
