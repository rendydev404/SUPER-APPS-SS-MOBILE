package com.sukashawarma.superapp.presentation.absensi.checklist

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.core.ui.ios.JudulSeksiIos
import com.sukashawarma.superapp.core.ui.ios.KartuIos
import com.sukashawarma.superapp.core.ui.ios.KeadaanIos
import com.sukashawarma.superapp.core.ui.ios.LencanaIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.SegmenIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.TombolBundarIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WadahSegmenIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.presentation.absensi.KartuMemuatIos
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.RealtimeTables
import com.sukashawarma.superapp.core.ui.kaca.denganRuangNav

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistScreen(onExit: () -> Unit, viewModel: ChecklistViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    RealtimeRefresh(RealtimeTables.CHECKLIST_RECORDS, RealtimeTables.CHECKLIST_TICKS) { viewModel.refresh() }
    val staff by AppSession.staff.collectAsState()

    Scaffold(
        containerColor = WarnaIos.Latar,
        topBar = {
            ChecklistTopBar(
                outletName = staff?.outletName ?: "Outlet",
                staffInitial = staff?.name?.take(1)?.uppercase() ?: "?",
            )
        },
    ) { padding ->
        val totalTasks = state.categories.sumOf { it.items.size }
        val completedTasks = state.categories.sumOf { category -> category.items.count { it.ticked } }
        val progress = if (totalTasks == 0) 0f else completedTasks.toFloat() / totalTasks

        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(
                start = UkuranIos.TepiLayar,
                end = UkuranIos.TepiLayar,
                top = 8.dp,
                bottom = 32.dp,
            ).denganRuangNav(),
            verticalArrangement = Arrangement.spacedBy(UkuranIos.JarakKartu),
        ) {
            item(key = "title") { ChecklistHeader() }
            item(key = "progress") {
                ProgressCard(
                    phase = state.phase,
                    progress = progress,
                    completed = completedTasks,
                    total = totalTasks,
                    onPhaseSelected = viewModel::setPhase,
                )
            }

            when {
                state.loading -> item(key = "loading") { KartuMemuatIos() }
                state.error != null -> item(key = "error") {
                    KeadaanIos(
                        ikon = IkonIos.ErrorOutline,
                        judul = "Gagal memuat",
                        pesan = state.error.orEmpty(),
                        nada = NadaIos.BAHAYA,
                        teksAksi = "Coba lagi",
                        onAksi = viewModel::load,
                    )
                }
                state.categories.isEmpty() -> item(key = "empty") {
                    KeadaanIos(
                        ikon = IkonIos.Checklist,
                        judul = "Belum ada tugas",
                        pesan = "Belum ada tugas ${state.phase.label.lowercase()} outlet hari ini.",
                    )
                }
                else -> state.categories.forEach { category ->
                    item(key = "category-${category.id}") {
                        CategoryHeader(
                            name = category.name,
                            selesai = category.items.count { it.ticked },
                            total = category.items.size,
                        )
                    }
                    items(category.items, key = { it.id }) { checklistItem ->
                        ChecklistTaskCard(
                            item = checklistItem,
                            onCheckedChange = { checked -> viewModel.toggleItem(checklistItem.id, checked) },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChecklistTopBar(outletName: String, staffInitial: String) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = WarnaIos.Latar),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    IkonIos.Storefront,
                    contentDescription = null,
                    tint = WarnaIos.Aksen,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(outletName, style = TipeIos.Keterangan.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold))
            }
        },
        actions = {
            // Tombol notifikasi memang belum punya tujuan (sama seperti sebelumnya).
            TombolBundarIos(Icons.Default.NotificationsNone, "Notifikasi", onKlik = { }, warnaIkon = WarnaIos.Label)
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(WarnaIos.Aksen.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(staffInitial, color = NadaIos.AKSEN.teks, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        },
    )
}

@Composable
private fun ChecklistHeader() {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        Text("Tutup/Buka Outlet", style = TipeIos.Judul1)
        Spacer(Modifier.height(2.dp))
        Text(
            "Hari ini, ${SimpleDateFormat("d MMMM yyyy", Locale("id", "ID")).format(Date())}",
            style = TipeIos.SubJudul,
        )
    }
}

@Composable
private fun ProgressCard(
    phase: ChecklistPhase,
    progress: Float,
    completed: Int,
    total: Int,
    onPhaseSelected: (ChecklistPhase) -> Unit,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "checklistProgress",
    )

    KartuIos {
        // Segmented control iOS menggantikan dua tombol bertumpuk — pilihan fase tetap sama.
        WadahSegmenIos {
            ChecklistPhase.entries.forEach { option ->
                SegmenIos(
                    label = "${option.label} Outlet",
                    aktif = phase == option,
                    onKlik = { onPhaseSelected(option) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .semantics { contentDescription = "Progress ${(progress * 100).toInt()} persen" },
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxSize(),
                    color = WarnaIos.Aksen,
                    trackColor = WarnaIos.Isian,
                    strokeWidth = 8.dp,
                )
                Text("${(progress * 100).toInt()}%", style = TipeIos.Utama.copy(fontSize = 16.sp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("Progress ${phase.label} Outlet", style = TipeIos.Utama)
                Spacer(Modifier.height(2.dp))
                Text("$completed/$total tugas selesai", style = TipeIos.Catatan)
            }
        }
    }
}

@Composable
private fun CategoryHeader(name: String, selesai: Int, total: Int) {
    JudulSeksiIos(name, keterangan = "$selesai/$total")
}

@Composable
private fun ChecklistTaskCard(item: ChecklistItemUi, onCheckedChange: (Boolean) -> Unit) {
    // Seluruh kartu menjadi kotak centang (sebelumnya kartu & Checkbox sama-sama membalik
    // status); lingkaran di kanan hanya penanda visual bergaya iOS.
    KartuIos(
        modifier = Modifier.semantics {
            stateDescription = if (item.ticked) "Selesai" else "Belum"
        },
        onKlik = { onCheckedChange(!item.ticked) },
        padding = PaddingValues(horizontal = UkuranIos.PaddingKartu, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (item.isRequired) {
                    LencanaIos("Wajib", NadaIos.AKSEN, titik = false)
                }
                Text(
                    item.name,
                    style = TipeIos.Keterangan.copy(
                        color = if (item.ticked) WarnaIos.LabelKedua else WarnaIos.Label,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
            Spacer(Modifier.width(12.dp))
            CentangBulat(item.ticked)
        }
    }
}

/** Lingkaran centang ala aplikasi Pengingat iOS. */
@Composable
private fun CentangBulat(tercentang: Boolean) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .then(
                if (tercentang) Modifier.background(WarnaIos.Aksen)
                else Modifier.border(1.5.dp, WarnaIos.LabelKetiga, CircleShape)
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (tercentang) {
            Icon(IkonIos.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
        }
    }
}
