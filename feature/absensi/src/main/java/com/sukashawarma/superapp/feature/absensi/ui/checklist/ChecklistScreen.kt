package com.sukashawarma.superapp.presentation.absensi.checklist

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Storefront
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.presentation.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistScreen(onExit: () -> Unit, viewModel: ChecklistViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val staff by AppSession.staff.collectAsState()

    Scaffold(
        containerColor = SukaCream,
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
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
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
                state.loading -> item(key = "loading") { LoadingCard() }
                state.error != null -> item(key = "error") {
                    MessageCard(
                        message = state.error.orEmpty(),
                        actionLabel = "Coba lagi",
                        onAction = viewModel::load,
                    )
                }
                state.categories.isEmpty() -> item(key = "empty") {
                    MessageCard(message = "Belum ada tugas ${state.phase.label.lowercase()} toko hari ini.")
                }
                else -> state.categories.forEach { category ->
                    item(key = "category-${category.id}") { CategoryHeader(category.name) }
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
        colors = TopAppBarDefaults.topAppBarColors(containerColor = SukaCream),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Storefront,
                    contentDescription = null,
                    tint = SukaOrange,
                    modifier = Modifier.size(19.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(outletName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = SukaInk)
            }
        },
        actions = {
            IconButton(onClick = { }) {
                Icon(Icons.Default.NotificationsNone, contentDescription = "Notifikasi", tint = SukaInk)
            }
            Surface(
                modifier = Modifier
                    .padding(end = 14.dp)
                    .size(38.dp),
                shape = CircleShape,
                color = SukaOrange.copy(alpha = 0.20f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(staffInitial, color = SukaInk, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        },
    )
}

@Composable
private fun ChecklistHeader() {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "Checklist Operasional",
            color = SukaInk,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Hari ini, ${SimpleDateFormat("d MMMM yyyy", Locale("id", "ID")).format(Date())}",
            color = SukaGray500,
            style = MaterialTheme.typography.bodyMedium,
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

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, SukaGray200),
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Progress ${phase.label} Toko",
                color = SukaInk,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .semantics { contentDescription = "Progress ${(progress * 100).toInt()} persen" },
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxSize(),
                    color = SukaOrange,
                    trackColor = SukaSurfaceContainerHighest,
                    strokeWidth = 9.dp,
                )
                Text(
                    "${(progress * 100).toInt()}%",
                    color = SukaOrange,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
            }
            Spacer(Modifier.height(12.dp))
            Text("$completed/$total tugas selesai", color = SukaGray500, fontSize = 12.sp)
            Spacer(Modifier.height(18.dp))

            ChecklistPhase.entries.forEachIndexed { index, option ->
                PhaseButton(
                    label = "${option.label} Toko",
                    selected = phase == option,
                    onClick = { onPhaseSelected(option) },
                )
                if (index < ChecklistPhase.entries.lastIndex) Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun PhaseButton(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SukaOrange, contentColor = Color.White),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        ) { Text(label, fontWeight = FontWeight.Bold) }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, SukaGray200),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = SukaGray100,
                contentColor = SukaGray700,
            ),
        ) { Text(label, fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun CategoryHeader(name: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(SukaOrange),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Checklist, contentDescription = null, tint = SukaInk, modifier = Modifier.size(20.dp))
        }
        Text(name, color = SukaInk, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        HorizontalDivider(modifier = Modifier.weight(1f), color = SukaOrange.copy(alpha = 0.35f))
    }
}

@Composable
private fun ChecklistTaskCard(item: ChecklistItemUi, onCheckedChange: (Boolean) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!item.ticked) },
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, SukaGray200),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(76.dp)
                    .background(if (item.ticked) SukaOrange.copy(alpha = 0.45f) else SukaOrange),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                if (item.isRequired) {
                    Surface(color = SukaOrange.copy(alpha = 0.14f), shape = RoundedCornerShape(50)) {
                        Text(
                            "Wajib",
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            color = SukaInk,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                Text(
                    item.name,
                    color = if (item.ticked) SukaGray400 else SukaInk,
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Checkbox(
                checked = item.ticked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.padding(end = 10.dp),
                colors = CheckboxDefaults.colors(
                    checkedColor = SukaOrange,
                    checkmarkColor = Color.White,
                    uncheckedColor = SukaGray400,
                ),
            )
        }
    }
}

@Composable
private fun LoadingCard() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, SukaGray200),
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = SukaOrange, strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
private fun MessageCard(message: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, SukaGray200),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(message, color = SukaGray500, textAlign = TextAlign.Center)
            if (actionLabel != null && onAction != null) {
                Button(onClick = onAction, colors = ButtonDefaults.buttonColors(containerColor = SukaOrange)) {
                    Text(actionLabel)
                }
            }
        }
    }
}
