package com.sukashawarma.superapp.presentation.absensi.checklist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.core.ui.RealtimeRefresh
import com.sukashawarma.superapp.core.ui.RealtimeTables
import com.sukashawarma.superapp.core.ui.ios.BilahJudulIos
import com.sukashawarma.superapp.core.ui.ios.IkonBulatIos
import com.sukashawarma.superapp.core.ui.ios.KartuIos
import com.sukashawarma.superapp.core.ui.ios.KeadaanIos
import com.sukashawarma.superapp.core.ui.ios.LencanaIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.PemisahIos
import com.sukashawarma.superapp.core.ui.ios.SegmenIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.TombolBundarIos
import com.sukashawarma.superapp.core.ui.ios.TombolUtamaIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WadahSegmenIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.ios.tekanIos
import com.sukashawarma.superapp.core.ui.ios.warnaKolomIos
import com.sukashawarma.superapp.core.ui.ios.warnaSaklarIos
import com.sukashawarma.superapp.core.ui.kaca.IkonIos

private enum class ChecklistTabFilter(val label: String) {
    ALL("All"),
    OPEN("Open"),
    CLOSED("Closed")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistManageScreen(
    onExit: () -> Unit,
    viewModel: ChecklistManageViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()

    // Master checklist disunting dari beberapa perangkat sekaligus; tanpa ini
    // dua penyunting saling menimpa tanpa pernah melihat daftar yang sama.
    RealtimeRefresh(RealtimeTables.CHECKLIST_ITEMS) { viewModel.load() }
    var selectedTab by remember { mutableStateOf(ChecklistTabFilter.ALL) }

    // Dialog state
    var showCreateDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<ManageChecklistItem?>(null) }
    var itemToDelete by remember { mutableStateOf<ManageChecklistItem?>(null) }

    Scaffold(
        containerColor = WarnaIos.Latar,
        topBar = {
            BilahJudulIos(judul = "Checklist Management", onKembali = onExit) {
                TombolBundarIos(IkonIos.MoreHoriz, "More", onKlik = { viewModel.load() })
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = WarnaIos.Aksen,
                contentColor = Color.White,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                modifier = Modifier.padding(bottom = 12.dp, end = 8.dp)
            ) {
                Icon(IkonIos.Add, contentDescription = "Tambah Checklist Baru", modifier = Modifier.size(26.dp))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header: Title and Subtitle
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = UkuranIos.TepiLayar + 4.dp, vertical = 8.dp)
            ) {
                Text(text = "Active Checklists", style = TipeIos.Judul1)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Kelola dan monitor daftar tugas operasional harian outlet.",
                    style = TipeIos.SubJudul,
                )
            }

            Spacer(Modifier.height(6.dp))

            // Filter All / Open / Closed — segmented control iOS, hitungan di lencana kecil.
            WadahSegmenIos(Modifier.padding(horizontal = UkuranIos.TepiLayar)) {
                ChecklistTabFilter.entries.forEach { tab ->
                    val count = when (tab) {
                        ChecklistTabFilter.ALL -> state.items.size
                        ChecklistTabFilter.OPEN -> state.items.count { it.phase == ChecklistPhase.BUKA }
                        ChecklistTabFilter.CLOSED -> state.items.count { it.phase == ChecklistPhase.TUTUP }
                    }
                    SegmenIos(
                        label = tab.label,
                        aktif = selectedTab == tab,
                        onKlik = { selectedTab = tab },
                        modifier = Modifier.weight(1f),
                        lencana = if (count > 0) "$count" else null,
                        warnaLencana = if (selectedTab == tab) WarnaIos.Aksen else WarnaIos.Abu.copy(alpha = 0.35f),
                    )
                }
            }

            Spacer(Modifier.height(UkuranIos.JarakKartu))

            // Body Content
            when {
                state.loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = WarnaIos.Aksen, strokeWidth = 2.5.dp, modifier = Modifier.size(28.dp))
                    }
                }

                state.error != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        KeadaanIos(
                            ikon = IkonIos.ErrorOutline,
                            judul = "Gagal memuat",
                            pesan = state.error.orEmpty(),
                            nada = NadaIos.BAHAYA,
                            teksAksi = "Coba Lagi",
                            onAksi = { viewModel.load() },
                        )
                    }
                }

                else -> {
                    val filteredItems = remember(state.items, selectedTab) {
                        when (selectedTab) {
                            ChecklistTabFilter.ALL -> state.items
                            ChecklistTabFilter.OPEN -> state.items.filter { it.phase == ChecklistPhase.BUKA }
                            ChecklistTabFilter.CLOSED -> state.items.filter { it.phase == ChecklistPhase.TUTUP }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(start = UkuranIos.TepiLayar, end = UkuranIos.TepiLayar, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(UkuranIos.JarakKartu)
                    ) {
                        if (filteredItems.isEmpty()) {
                            item(key = "empty_placeholder") {
                                KeadaanIos(
                                    ikon = IkonIos.Checklist,
                                    judul = "Belum Ada Checklist ${if (selectedTab != ChecklistTabFilter.ALL) selectedTab.label else ""}",
                                    pesan = "Tambahkan tugas operasional dengan menekan tombol (+) di bawah.",
                                    nada = NadaIos.AKSEN,
                                )
                            }
                        } else {
                            items(filteredItems, key = { it.id }) { item ->
                                DirectChecklistItemCard(
                                    item = item,
                                    onEdit = { itemToEdit = item },
                                    onDelete = { itemToDelete = item }
                                )
                            }
                        }

                        // Dashed "Create New Checklist" Placeholder Card
                        item(key = "create_new_card") {
                            DashedCreateChecklistCard(onClick = { showCreateDialog = true })
                        }
                    }
                }
            }
        }
    }

    // Modal Form Dialog: Add Checklist
    if (showCreateDialog) {
        val initialPhase = when (selectedTab) {
            ChecklistTabFilter.CLOSED -> ChecklistPhase.TUTUP
            else -> ChecklistPhase.BUKA
        }
        ChecklistFormDialog(
            title = "Tambah Checklist Baru",
            subtitle = "Tentukan nama tugas operasional dan waktu pelaksanaan",
            initialName = "",
            initialPhase = initialPhase,
            initialRequired = true,
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, phase, isRequired ->
                viewModel.addChecklist(name, phase, isRequired)
                showCreateDialog = false
            }
        )
    }

    // Modal Form Dialog: Edit Checklist
    itemToEdit?.let { item ->
        ChecklistFormDialog(
            title = "Edit Checklist",
            subtitle = "Perbarui detail checklist operasional",
            initialName = item.name,
            initialPhase = item.phase,
            initialRequired = item.isRequired,
            onDismiss = { itemToEdit = null },
            onConfirm = { name, phase, isRequired ->
                viewModel.updateChecklist(item, name, phase, isRequired)
                itemToEdit = null
            }
        )
    }

    // Confirmation Dialog: Delete Checklist
    itemToDelete?.let { item ->
        ConfirmDeleteDialog(
            title = "Hapus Checklist?",
            message = "Apakah Anda yakin ingin menghapus checklist \"${item.name}\"?",
            onDismiss = { itemToDelete = null },
            onConfirm = {
                viewModel.deleteChecklist(item.id)
                itemToDelete = null
            }
        )
    }
}

/**
 * Kartu satu item checklist — kartu putih iOS dengan lencana fase dan prioritas.
 */
@Composable
private fun DirectChecklistItemCard(
    item: ManageChecklistItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val isBuka = item.phase == ChecklistPhase.BUKA

    KartuIos(padding = PaddingValues(start = UkuranIos.PaddingKartu, end = 12.dp, top = 14.dp, bottom = 14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = item.name,
                style = TipeIos.Utama,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(top = 6.dp, end = 10.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TombolBundarIos(Icons.Filled.Edit, "Edit Checklist", onKlik = onEdit)
                TombolBundarIos(IkonIos.Delete, "Delete Checklist", onKlik = onDelete, warnaIkon = WarnaIos.Merah)
            }
        }

        Spacer(Modifier.height(12.dp))
        PemisahIos(inset = 0.dp)
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LencanaIos(
                teks = if (isBuka) "Buka (Opening)" else "Tutup (Closing)",
                nada = if (isBuka) NadaIos.AKSEN else NadaIos.INFO,
                ikon = if (isBuka) Icons.Filled.WbSunny else Icons.Filled.NightsStay,
            )
            if (item.isRequired) {
                LencanaIos(teks = "Wajib Dikerjakan", nada = NadaIos.BAHAYA, ikon = Icons.Filled.Star)
            } else {
                LencanaIos(teks = "Opsional", nada = NadaIos.NETRAL, titik = false)
            }
        }
    }
}

/**
 * Kartu ajakan "Tambah Checklist Baru" bergaris putus-putus.
 */
@Composable
private fun DashedCreateChecklistCard(onClick: () -> Unit) {
    val borderColor = WarnaIos.LabelKetiga
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 100.dp)
            .drawBehind {
                val stroke = Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                )
                drawRoundRect(
                    color = borderColor,
                    style = stroke,
                    cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx())
                )
            }
            .clip(UkuranIos.SudutKartu)
            .tekanIos(onClick)
            .padding(vertical = 20.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IkonBulatIos(IkonIos.Add, NadaIos.AKSEN.warna, ukuran = 38.dp, padat = false)
            Text(
                "Tambah Checklist Baru",
                color = WarnaIos.Aksen,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/**
 * Formulir tambah/ubah checklist dalam dialog bergaya iOS.
 */
@Composable
private fun ChecklistFormDialog(
    title: String,
    subtitle: String,
    initialName: String,
    initialPhase: ChecklistPhase,
    initialRequired: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, ChecklistPhase, Boolean) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var phase by remember { mutableStateOf(initialPhase) }
    var isRequired by remember { mutableStateOf(initialRequired) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = UkuranIos.SudutKartu,
        containerColor = WarnaIos.Kartu,
        modifier = Modifier.padding(vertical = 12.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IkonBulatIos(IkonIos.Checklist, NadaIos.AKSEN.warna, ukuran = 44.dp, padat = false)
                Column {
                    Text(title, style = TipeIos.Judul3)
                    Text(subtitle, style = TipeIos.Catatan)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Checklist Name Input
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Nama Checklist / Tugas",
                        style = TipeIos.Catatan.copy(color = WarnaIos.Label, fontWeight = FontWeight.SemiBold),
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text("Contoh: Cek regulator gas LPG", fontSize = 14.sp) },
                        singleLine = true,
                        shape = UkuranIos.SudutKontrol,
                        colors = warnaKolomIos(),
                        trailingIcon = {
                            if (name.isNotBlank()) {
                                IconButton(onClick = { name = "" }, modifier = Modifier.size(24.dp)) {
                                    Icon(IkonIos.Close, contentDescription = "Clear", tint = WarnaIos.Abu, modifier = Modifier.size(15.dp))
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Section 2: Waktu Pelaksanaan (Fase Buka / Tutup)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Waktu Pelaksanaan",
                        style = TipeIos.Catatan.copy(color = WarnaIos.Label, fontWeight = FontWeight.SemiBold),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PilihanFase(
                            judul = "Buka",
                            keterangan = "Sebelum buka outlet",
                            ikon = Icons.Filled.WbSunny,
                            terpilih = phase == ChecklistPhase.BUKA,
                            onKlik = { phase = ChecklistPhase.BUKA },
                            modifier = Modifier.weight(1f),
                        )
                        PilihanFase(
                            judul = "Tutup",
                            keterangan = "Saat closing/tutup outlet",
                            ikon = Icons.Filled.NightsStay,
                            terpilih = phase == ChecklistPhase.TUTUP,
                            onKlik = { phase = ChecklistPhase.TUTUP },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                // Section 3: Pengaturan Tugas Wajib / Prioritas
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(UkuranIos.SudutBlok)
                        .background(WarnaIos.Latar)
                        .tekanIos({ isRequired = !isRequired }, skalaTekan = 0.99f)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        Modifier.size(32.dp).clip(CircleShape).background(if (isRequired) WarnaIos.Merah else WarnaIos.Isian),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (isRequired) Icons.Filled.Star else IkonIos.Check,
                            contentDescription = null,
                            tint = if (isRequired) Color.White else WarnaIos.LabelKedua,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Tugas Wajib / Prioritas",
                            style = TipeIos.Keterangan.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                        )
                        Text("Harus dicentang staff saat operasional", style = TipeIos.Kecil)
                    }

                    Switch(
                        checked = isRequired,
                        onCheckedChange = { isRequired = it },
                        colors = warnaSaklarIos(),
                    )
                }
            }
        },
        confirmButton = {
            TombolUtamaIos(
                teks = "Simpan Checklist",
                onKlik = { onConfirm(name.trim(), phase, isRequired) },
                aktif = name.isNotBlank(),
            )
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Batal", color = WarnaIos.Aksen, fontSize = 16.sp)
            }
        }
    )
}

/** Satu pilihan fase di formulir: isian abu, aksen tipis + garis aksen saat terpilih. */
@Composable
private fun PilihanFase(
    judul: String,
    keterangan: String,
    ikon: androidx.compose.ui.graphics.vector.ImageVector,
    terpilih: Boolean,
    onKlik: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(UkuranIos.SudutBlok)
            .background(if (terpilih) WarnaIos.Aksen.copy(alpha = 0.10f) else WarnaIos.Latar)
            .border(1.5.dp, if (terpilih) WarnaIos.Aksen else Color.Transparent, UkuranIos.SudutBlok)
            .tekanIos(onKlik)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                ikon,
                contentDescription = null,
                tint = if (terpilih) WarnaIos.Aksen else WarnaIos.LabelKedua,
                modifier = Modifier.size(18.dp)
            )
            Text(
                judul,
                color = if (terpilih) NadaIos.AKSEN.teks else WarnaIos.Label,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(keterangan, style = TipeIos.Kecil, textAlign = TextAlign.Center)
    }
}

/**
 * Dialog konfirmasi hapus bergaya iOS.
 */
@Composable
private fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = UkuranIos.SudutKartu,
        containerColor = WarnaIos.Kartu,
        icon = { IkonBulatIos(IkonIos.Delete, NadaIos.BAHAYA.warna, ukuran = 48.dp, padat = false) },
        title = {
            Text(title, style = TipeIos.Judul3, textAlign = TextAlign.Center)
        },
        text = {
            Text(message, style = TipeIos.SubJudul, textAlign = TextAlign.Center)
        },
        confirmButton = {
            TombolUtamaIos(teks = "Hapus", onKlik = onConfirm, warna = WarnaIos.Merah)
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Batal", color = WarnaIos.Aksen, fontSize = 16.sp)
            }
        }
    )
}
