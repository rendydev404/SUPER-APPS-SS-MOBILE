package com.sukashawarma.superapp.feature.stok.ui.pantau

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.core.ui.ios.BilahJudulIos
import com.sukashawarma.superapp.core.ui.ios.JudulSeksiIos
import com.sukashawarma.superapp.core.ui.ios.KartuIos
import com.sukashawarma.superapp.core.ui.ios.KolomCariIos
import com.sukashawarma.superapp.core.ui.ios.LencanaIos
import com.sukashawarma.superapp.core.ui.ios.PetakStatIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.TombolBundarIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.core.ui.kaca.denganRuangNav
import com.sukashawarma.superapp.feature.stok.data.model.MonitoringRow
import com.sukashawarma.superapp.feature.stok.domain.FilterPantau
import com.sukashawarma.superapp.feature.stok.domain.PantauOutlet
import com.sukashawarma.superapp.feature.stok.domain.RingkasOutlet
import com.sukashawarma.superapp.feature.stok.domain.StokStatus
import com.sukashawarma.superapp.feature.stok.domain.WilayahOutlet
import com.sukashawarma.superapp.feature.stok.domain.formatAngkaStok
import com.sukashawarma.superapp.feature.stok.domain.formatSatuan
import com.sukashawarma.superapp.feature.stok.ui.KeadaanGagal
import com.sukashawarma.superapp.feature.stok.ui.KeadaanKosong
import com.sukashawarma.superapp.feature.stok.ui.KeadaanTidakBerhak
import com.sukashawarma.superapp.feature.stok.ui.MemuatPenuh
import com.sukashawarma.superapp.feature.stok.ui.label
import com.sukashawarma.superapp.feature.stok.ui.nadaIos
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Selang polling saat layar terlihat — sama dengan fallback poll papan live web. */
private const val SELANG_POLLING_MS = 120_000L

/**
 * Papan pantau stok seluruh outlet untuk Gudang Pusat (role kitchen) — cermin
 * `SPVDashboard.tsx` + `LiveMonitoringPage.tsx` web.
 *
 * Menekan kartu outlet membuka layar Monitoring biasa untuk outlet itu (daftar bahan
 * per kategori, detail, riwayat ledger) — layar yang sama dengan yang dipakai outlet.
 */
@Composable
fun PantauOutletScreen(
    onKeluar: () -> Unit,
    onBukaOutlet: (outletId: String) -> Unit,
    onBukaTransfer: () -> Unit,
    viewModel: PantauOutletViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()

    // Polling jarang yang HANYA hidup saat layar terlihat (RESUMED). Begitu aplikasi ke
    // latar atau pindah tab, perulangan ini berhenti — tidak ada query yang berjalan
    // untuk layar yang tidak dilihat siapa pun. Sengaja bukan realtime: lihat
    // PantauOutletRepository.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val vm by rememberUpdatedState(viewModel)
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            // Kembali dari layar lain/latar/tab lain: muat tanpa paksa — cache (TTL 45 dtk)
            // dipakai bila masih segar, jadi bolak-balik ke detail outlet tidak menembak
            // query. Bertabrakan dengan pemuatan init? `muat` menolak job kedua.
            vm.muat(paksa = false)
            while (true) {
                delay(SELANG_POLLING_MS)
                vm.muat(paksa = true)
            }
        }
    }

    Column(Modifier.fillMaxSize().background(WarnaIos.Latar)) {
        BilahJudulIos(
            judul = "Monitoring Stok",
            subjudul = subjudul(state),
            onKembali = onKeluar,
        ) {
            TombolBundarIos(IkonIos.SwapHoriz, "Saran transfer", onBukaTransfer)
            if (state.menyegarkan) {
                Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = WarnaIos.Abu, strokeWidth = 2.dp)
                }
            } else {
                TombolBundarIos(IkonIos.Refresh, "Segarkan", viewModel::segarkan)
            }
        }

        when {
            state.tidakBerhak -> KeadaanTidakBerhak("Akun Anda belum memiliki akses ke outlet mana pun.")
            state.memuat -> MemuatPenuh()
            state.error != null && state.diperbarui == null -> KeadaanGagal(state.error!!, viewModel::segarkan)
            else -> Isi(state, viewModel, onBukaOutlet)
        }
    }
}

private fun subjudul(state: PantauUiState): String? {
    val waktu = state.diperbarui ?: return null
    val jam = SimpleDateFormat("HH:mm", Locale("id", "ID")).format(Date(waktu))
    val gagal = if (state.error != null) " · gagal menyegarkan" else ""
    return "${state.papan.outlets.size} outlet · Diperbarui $jam$gagal"
}

@Composable
private fun Isi(
    state: PantauUiState,
    viewModel: PantauOutletViewModel,
    onBukaOutlet: (String) -> Unit,
) {
    val papan = state.papan
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = UkuranIos.TepiLayar, end = UkuranIos.TepiLayar, top = 12.dp, bottom = 24.dp,
        ).denganRuangNav(),
        verticalArrangement = Arrangement.spacedBy(UkuranIos.JarakKartu),
    ) {
        item(key = "stat") { PetakStatus(state, viewModel::pilihFilter) }

        papan.hub?.let { hub ->
            item(key = "hub") { KartuHub(hub, onBukaOutlet) }
        }

        item(key = "prioritas") { KartuPrioritas(papan.prioritas, onBukaOutlet) }

        item(key = "cari") {
            KolomCariIos(state.cari, viewModel::ubahCari, placeholder = "Cari outlet")
        }

        if (state.perWilayah.isEmpty()) {
            item(key = "kosong") {
                KeadaanKosong(
                    if (state.cari.isNotBlank()) "Outlet tidak ditemukan."
                    else "Tidak ada outlet pada filter ini."
                )
            }
        } else {
            state.perWilayah.forEach { (wilayah, isi) ->
                item(key = "w-${wilayah.name}") { JudulWilayah(wilayah, isi) }
                items(isi, key = { it.outletId }) { KartuOutlet(it, onBukaOutlet) }
            }
        }
    }
}

// ------------------------------------------------------------------- ringkasan

@Composable
private fun PetakStatus(state: PantauUiState, onPilih: (FilterPantau) -> Unit) {
    val papan = state.papan
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PetakStatIos(
                label = "Semua Outlet",
                nilai = papan.outlets.size.toString(),
                ikon = IkonIos.Storefront,
                warna = WarnaIos.AbuGelap,
                aktif = state.filter == FilterPantau.SEMUA,
                onKlik = { if (state.filter != FilterPantau.SEMUA) onPilih(state.filter) },
                modifier = Modifier.weight(1f),
            )
            PetakStatIos(
                label = "Kritis",
                nilai = papan.jumlahKritis.toString(),
                ikon = IkonIos.WarningAmber,
                warna = WarnaIos.Merah,
                aktif = state.filter == FilterPantau.KRITIS,
                onKlik = { onPilih(FilterPantau.KRITIS) },
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PetakStatIos(
                label = "Menipis",
                nilai = papan.jumlahMenipis.toString(),
                ikon = IkonIos.Inventory2,
                warna = WarnaIos.Oranye,
                aktif = state.filter == FilterPantau.MENIPIS,
                onKlik = { onPilih(FilterPantau.MENIPIS) },
                modifier = Modifier.weight(1f),
            )
            PetakStatIos(
                label = "Aman",
                nilai = papan.jumlahAman.toString(),
                ikon = IkonIos.Check,
                warna = WarnaIos.Hijau,
                aktif = state.filter == FilterPantau.AMAN,
                onKlik = { onPilih(FilterPantau.AMAN) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Panel "Kitchen Pusat" web: stok sumber pasok tampil paling atas, terpisah dari grid. */
@Composable
private fun KartuHub(hub: RingkasOutlet, onBuka: (String) -> Unit) {
    KartuIos(onKlik = { onBuka(hub.outletId) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(34.dp).clip(CircleShape).background(WarnaIos.Biru),
                contentAlignment = Alignment.Center,
            ) {
                Icon(IkonIos.Inventory2, null, tint = Color.White, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Gudang Pusat", style = TipeIos.Utama)
                Text("Sumber pasok utama · ${hub.jumlahBahan} bahan", style = TipeIos.Catatan)
            }
            LencanaIos(hub.status.label(), hub.status.nadaIos())
        }
        if (hub.status != StokStatus.OK) {
            Spacer(Modifier.height(10.dp))
            Text(
                ringkasJumlah(hub),
                style = TipeIos.Catatan.copy(fontWeight = FontWeight.SemiBold, color = hub.status.nadaIos().teks),
            )
            Spacer(Modifier.height(6.dp))
            DaftarBahanRendah(hub.bahanRendah, maks = 3)
        }
    }
}

@Composable
private fun KartuPrioritas(prioritas: List<MonitoringRow>, onBuka: (String) -> Unit) {
    KartuIos(latar = if (prioritas.isEmpty()) WarnaIos.Kartu else WarnaIos.Merah.copy(alpha = 0.06f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                IkonIos.WarningAmber, null,
                tint = if (prioritas.isEmpty()) WarnaIos.Hijau else WarnaIos.Merah,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (prioritas.isEmpty()) "Semua outlet aman — tidak ada bahan kritis"
                else "Prioritas utama — bahan kritis",
                style = TipeIos.Keterangan.copy(fontWeight = FontWeight.SemiBold),
            )
        }
        prioritas.forEachIndexed { i, row ->
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${i + 1}",
                    style = TipeIos.Angka.copy(color = WarnaIos.Merah.copy(alpha = 0.55f)),
                    modifier = Modifier.width(24.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text(row.itemName, style = TipeIos.Keterangan, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(PantauOutlet.namaPendek(row.outletName), style = TipeIos.Catatan, maxLines = 1)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    saldoPerMin(row),
                    style = TipeIos.Catatan.copy(color = WarnaIos.Merah, fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(end = 4.dp),
                )
                TombolBundarIos(IkonIos.ChevronRight, "Buka outlet", { onBuka(row.outletId) })
            }
        }
    }
}

// --------------------------------------------------------------------- outlet

@Composable
private fun JudulWilayah(wilayah: WilayahOutlet, isi: List<RingkasOutlet>) {
    val bermasalah = isi.count { it.status == StokStatus.BELOW || it.status == StokStatus.WARNING }
    JudulSeksiIos(
        wilayah.label,
        keterangan = if (bermasalah > 0) "$bermasalah/${isi.size} perlu perhatian" else "${isi.size} outlet",
    )
}

@Composable
private fun KartuOutlet(outlet: RingkasOutlet, onBuka: (String) -> Unit) {
    KartuIos(onKlik = { onBuka(outlet.outletId) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(outlet.namaPendek, style = TipeIos.Utama, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${if (outlet.nama.uppercase().startsWith("MITRA")) "Mitra" else "Internal"} · " +
                        "${outlet.jumlahBahan} bahan · sehat ${outlet.persenSehat}%",
                    style = TipeIos.Catatan,
                    maxLines = 1,
                )
            }
            Spacer(Modifier.width(8.dp))
            LencanaIos(outlet.status.label(), outlet.status.nadaIos())
        }
        if (outlet.status == StokStatus.BELOW || outlet.status == StokStatus.WARNING) {
            Spacer(Modifier.height(10.dp))
            DaftarBahanRendah(outlet.bahanRendah, maks = 3)
        }
    }
}

@Composable
private fun DaftarBahanRendah(bahan: List<MonitoringRow>, maks: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        bahan.take(maks).forEach { row ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(UkuranIos.SudutKontrol)
                    .background(WarnaIos.Isian.copy(alpha = 0.08f))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    row.itemName,
                    style = TipeIos.Catatan.copy(color = WarnaIos.Label, fontWeight = FontWeight.Medium),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    saldoPerMin(row),
                    style = TipeIos.Catatan.copy(fontSize = 12.sp, color = WarnaIos.Merah, fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                )
            }
        }
        if (bahan.size > maks) {
            Text("+ ${bahan.size - maks} bahan lainnya", style = TipeIos.Kecil, modifier = Modifier.padding(start = 4.dp))
        }
    }
}

private fun ringkasJumlah(o: RingkasOutlet): String = listOfNotNull(
    o.jumlahKritis.takeIf { it > 0 }?.let { "$it kritis" },
    o.jumlahMenipis.takeIf { it > 0 }?.let { "$it menipis" },
).joinToString(" · ")

/** "2 dus 3 pak / min 5 dus" — saldo berjenjang dibanding batas minimum. */
private fun saldoPerMin(row: MonitoringRow): String =
    "${row.saldoTampil} / ${formatAngkaStok(row.threshold ?: 0.0)} ${formatSatuan(row.satuan)}".trim()
