package com.sukashawarma.superapp.presentation.absensi.checklist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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

// Stitch Suka Culinary Design Tokens
private val StitchPrimary = Color(0xFF450700)
private val StitchPrimaryContainer = Color(0xFF6B1101)
private val StitchSecondary = Color(0xFF9C4400)
private val StitchSecondaryContainer = Color(0xFFFE8438)
private val StitchOnSecondaryContainer = Color(0xFF652A00)
private val StitchSurface = Color(0xFFF8F9FF)
private val StitchSurfaceContainerLowest = Color(0xFFFFFFFF)
private val StitchSurfaceContainerLow = Color(0xFFEFF4FF)
private val StitchSurfaceContainer = Color(0xFFE5EEFF)
private val StitchSurfaceContainerHigh = Color(0xFFDCE9FF)
private val StitchOnSurface = Color(0xFF0B1C30)
private val StitchOnSurfaceVariant = Color(0xFF57423D)
private val StitchOutlineVariant = Color(0xFFDEC0B9)
private val StitchError = Color(0xFFBA1A1A)
private val StitchErrorContainer = Color(0xFFFFDAD6)
private val StitchOnErrorContainer = Color(0xFF93000A)

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
    var selectedTab by remember { mutableStateOf(ChecklistTabFilter.ALL) }

    // Dialog state
    var showCreateDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<ManageChecklistItem?>(null) }
    var itemToDelete by remember { mutableStateOf<ManageChecklistItem?>(null) }

    Scaffold(
        containerColor = StitchSurface,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StitchSurface,
                    titleContentColor = StitchPrimary,
                    navigationIconContentColor = StitchOnSurface,
                    actionIconContentColor = StitchOnSurface
                ),
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                title = {
                    Text(
                        "Checklist Management",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchPrimary
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.load() }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = StitchPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                modifier = Modifier.padding(bottom = 12.dp, end = 8.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Tambah Checklist Baru", modifier = Modifier.size(28.dp))
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
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Active Checklists",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = StitchOnSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Kelola dan monitor daftar tugas operasional harian outlet.",
                    fontSize = 13.5.sp,
                    color = StitchOnSurfaceVariant,
                    lineHeight = 18.sp
                )
            }

            Spacer(Modifier.height(6.dp))

            // Tab Filter Row: All, Open, Closed
            val tabs = ChecklistTabFilter.entries
            val selectedTabIndex = tabs.indexOf(selectedTab)

            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = StitchSurface,
                contentColor = StitchPrimary,
                indicator = { tabPositions ->
                    if (selectedTabIndex < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            height = 2.5.dp,
                            color = StitchPrimary
                        )
                    }
                },
                divider = {
                    HorizontalDivider(color = StitchOutlineVariant.copy(alpha = 0.4f), thickness = 1.dp)
                },
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                tabs.forEachIndexed { index, tab ->
                    val isSelected = selectedTab == tab
                    val count = when (tab) {
                        ChecklistTabFilter.ALL -> state.items.size
                        ChecklistTabFilter.OPEN -> state.items.count { it.phase == ChecklistPhase.BUKA }
                        ChecklistTabFilter.CLOSED -> state.items.count { it.phase == ChecklistPhase.TUTUP }
                    }
                    Tab(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = tab.label,
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) StitchPrimary else StitchOnSurfaceVariant
                                )
                                if (count > 0) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isSelected) StitchPrimaryContainer.copy(alpha = 0.12f) else StitchSurfaceContainer,
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "$count",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) StitchPrimary else StitchOnSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Body Content
            when {
                state.loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = StitchPrimary, strokeWidth = 3.dp)
                    }
                }

                state.error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = StitchErrorContainer,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.PriorityHigh, contentDescription = null, tint = StitchError, modifier = Modifier.size(28.dp))
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(
                            state.error.orEmpty(),
                            color = StitchOnSurface,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.load() },
                            colors = ButtonDefaults.buttonColors(containerColor = StitchPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Coba Lagi")
                        }
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
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (filteredItems.isEmpty()) {
                            item(key = "empty_placeholder") {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 20.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    color = StitchSurfaceContainerLow,
                                    border = BorderStroke(1.dp, StitchSurfaceContainerHigh)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = StitchSurfaceContainer,
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Filled.Checklist,
                                                    contentDescription = null,
                                                    tint = StitchPrimary,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            "Belum Ada Checklist ${if (selectedTab != ChecklistTabFilter.ALL) selectedTab.label else ""}",
                                            fontWeight = FontWeight.Bold,
                                            color = StitchOnSurface,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            "Tambahkan tugas operasional dengan menekan tombol (+) di bawah.",
                                            color = StitchOnSurfaceVariant,
                                            fontSize = 13.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
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
 * Direct Checklist Item Card (No nested category clutter, clean Stitch style).
 */
@Composable
private fun DirectChecklistItemCard(
    item: ManageChecklistItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val isBuka = item.phase == ChecklistPhase.BUKA
    val accentColor = if (isBuka) StitchSecondaryContainer else StitchPrimary

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = StitchSurfaceContainerLow,
        border = BorderStroke(1.dp, StitchSurfaceContainerHigh.copy(alpha = 0.7f)),
        shadowElevation = 0.5.dp
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            // Left border accent bar (5dp)
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(accentColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                // Top Row: Task Name + Priority badge & Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = item.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = StitchOnSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Action Buttons (Edit & Delete)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Edit Button
                        Surface(
                            onClick = onEdit,
                            shape = CircleShape,
                            color = StitchSurfaceContainer,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Filled.Edit,
                                    contentDescription = "Edit Checklist",
                                    tint = StitchPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Delete Button
                        Surface(
                            onClick = onDelete,
                            shape = CircleShape,
                            color = StitchErrorContainer,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Delete Checklist",
                                    tint = StitchOnErrorContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = StitchOutlineVariant.copy(alpha = 0.35f), thickness = 0.75.dp)
                Spacer(Modifier.height(10.dp))

                // Footer Metadata Row: Phase Tag & Priority Tag
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Phase Tag
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isBuka) Color(0xFFFFF3E0) else StitchSurfaceContainer,
                        border = BorderStroke(0.5.dp, if (isBuka) StitchSecondaryContainer.copy(alpha = 0.4f) else StitchOutlineVariant.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isBuka) Icons.Filled.WbSunny else Icons.Filled.NightsStay,
                                contentDescription = null,
                                tint = if (isBuka) StitchSecondary else StitchPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (isBuka) "Buka (Opening)" else "Tutup (Closing)",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isBuka) StitchSecondary else StitchPrimary
                            )
                        }
                    }

                    // Priority Tag
                    if (item.isRequired) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = StitchErrorContainer,
                            border = BorderStroke(0.5.dp, StitchError.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = StitchError,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Wajib Dikerjakan",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StitchError
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = StitchSurfaceContainerLow,
                            border = BorderStroke(0.5.dp, StitchOutlineVariant.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "Opsional",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = StitchOnSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dashed "Create New Checklist" Placeholder Card.
 */
@Composable
private fun DashedCreateChecklistCard(onClick: () -> Unit) {
    val borderColor = StitchOutlineVariant
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
                    cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
                )
            }
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 20.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = StitchSurfaceContainer,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = null,
                        tint = StitchPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Text(
                "Tambah Checklist Baru",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = StitchPrimary
            )
        }
    }
}

/**
 * User-Friendly, Modern Modal Form Dialog for creating and editing Checklists.
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
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        modifier = Modifier.padding(vertical = 12.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = StitchSurfaceContainer,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Checklist,
                            contentDescription = null,
                            tint = StitchPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column {
                    Text(
                        title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchPrimary
                    )
                    Text(
                        subtitle,
                        fontSize = 12.sp,
                        color = StitchOnSurfaceVariant,
                        lineHeight = 16.sp
                    )
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
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = StitchOnSurface
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text("Contoh: Cek regulator gas LPG", fontSize = 13.5.sp, color = StitchOnSurfaceVariant.copy(alpha = 0.6f)) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StitchPrimary,
                            unfocusedBorderColor = StitchOutlineVariant,
                            focusedLabelColor = StitchPrimary,
                            cursorColor = StitchPrimary,
                            focusedContainerColor = StitchSurfaceContainerLow.copy(alpha = 0.4f),
                            unfocusedContainerColor = StitchSurfaceContainerLow.copy(alpha = 0.2f),
                        ),
                        trailingIcon = {
                            if (name.isNotBlank()) {
                                IconButton(onClick = { name = "" }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Filled.Close, contentDescription = "Clear", tint = StitchOnSurfaceVariant, modifier = Modifier.size(16.dp))
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
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = StitchOnSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Card 1: Buka Toko (Opening)
                        val isBuka = phase == ChecklistPhase.BUKA
                        Surface(
                            onClick = { phase = ChecklistPhase.BUKA },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isBuka) Color(0xFFFFF8F2) else StitchSurfaceContainerLow,
                            border = BorderStroke(
                                width = if (isBuka) 2.dp else 1.dp,
                                color = if (isBuka) StitchSecondaryContainer else StitchOutlineVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.WbSunny,
                                        contentDescription = null,
                                        tint = if (isBuka) StitchSecondary else StitchOnSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        "Buka",
                                        fontSize = 14.sp,
                                        fontWeight = if (isBuka) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isBuka) StitchSecondary else StitchOnSurface
                                    )
                                }
                                Text(
                                    "Sebelum buka toko",
                                    fontSize = 11.sp,
                                    color = StitchOnSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // Card 2: Tutup Toko (Closing)
                        val isTutup = phase == ChecklistPhase.TUTUP
                        Surface(
                            onClick = { phase = ChecklistPhase.TUTUP },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isTutup) Color(0xFFFCF0EE) else StitchSurfaceContainerLow,
                            border = BorderStroke(
                                width = if (isTutup) 2.dp else 1.dp,
                                color = if (isTutup) StitchPrimary else StitchOutlineVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.NightsStay,
                                        contentDescription = null,
                                        tint = if (isTutup) StitchPrimary else StitchOnSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        "Tutup",
                                        fontSize = 14.sp,
                                        fontWeight = if (isTutup) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isTutup) StitchPrimary else StitchOnSurface
                                    )
                                }
                                Text(
                                    "Saat closing toko",
                                    fontSize = 11.sp,
                                    color = StitchOnSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Section 3: Pengaturan Tugas Wajib / Prioritas
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isRequired) StitchErrorContainer.copy(alpha = 0.45f) else StitchSurfaceContainerLow,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isRequired) StitchError.copy(alpha = 0.35f) else StitchOutlineVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { isRequired = !isRequired }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isRequired) StitchError else StitchSurfaceContainer,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isRequired) Icons.Filled.Star else Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = if (isRequired) Color.White else StitchOnSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Tugas Wajib / Prioritas",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isRequired) StitchError else StitchOnSurface
                            )
                            Text(
                                "Harus dicentang staff saat operasional",
                                fontSize = 11.5.sp,
                                color = StitchOnSurfaceVariant
                            )
                        }

                        Switch(
                            checked = isRequired,
                            onCheckedChange = { isRequired = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = StitchError,
                                uncheckedThumbColor = StitchOnSurfaceVariant,
                                uncheckedTrackColor = StitchSurfaceContainer
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.trim(), phase, isRequired) },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = StitchPrimary,
                    disabledContainerColor = StitchOutlineVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    "Simpan Checklist",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Batal", color = StitchOnSurfaceVariant, fontWeight = FontWeight.Medium)
            }
        }
    )
}

/**
 * Clean & Friendly Confirmation Dialog for Deletion.
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
        shape = RoundedCornerShape(22.dp),
        containerColor = Color.White,
        icon = {
            Surface(
                shape = CircleShape,
                color = StitchErrorContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = null,
                        tint = StitchError,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        title = {
            Text(
                title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = StitchPrimary,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                message,
                fontSize = 13.sp,
                color = StitchOnSurfaceVariant,
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StitchError),
                modifier = Modifier.fillMaxWidth().height(44.dp)
            ) {
                Text("Hapus", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Batal", color = StitchOnSurfaceVariant)
            }
        }
    )
}
