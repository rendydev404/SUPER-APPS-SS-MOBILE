package com.sukashawarma.superapp.presentation.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.presentation.theme.SukaOnSurface
import com.sukashawarma.superapp.presentation.theme.SukaOnSurfaceVariant
import com.sukashawarma.superapp.presentation.theme.SukaOrange
import com.sukashawarma.superapp.presentation.theme.SukaPrimaryContainer
import com.sukashawarma.superapp.presentation.theme.SukaSurface
import com.sukashawarma.superapp.presentation.theme.SukaSurfaceContainer
import com.sukashawarma.superapp.presentation.theme.SukaSurfaceContainerLowest
import com.sukashawarma.superapp.presentation.theme.StatusAmber
import com.sukashawarma.superapp.presentation.theme.StatusEmerald
import com.sukashawarma.superapp.presentation.theme.StatusRed

/** Satu kartu modul di daftar aplikasi. Sekarang cuma Absensi yang aktif — tile lain
 *  ditambah begitu modulnya dibangun (POS, stok, distribusi), bukan sebelumnya. */
private data class ModuleTile(
    val label: String,
    val desc: String,
    val icon: ImageVector,
    val enabled: Boolean,
    val onClick: () -> Unit,
)

/**
 * Daftar aplikasi — mengikuti desain Stitch (project 16991912726833518585, screen
 * cffd26a704054786b84ea57283554d18, "Updated Dashboard"): hero gradient dengan status
 * absen hari ini, lalu grid kartu modul dengan ikon berwarna & micro-interaction saat ditekan.
 */
@Composable
fun HomeScreen(
    onOpenAbsensi: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val staff = state.staff

    Column(modifier = Modifier.fillMaxSize().background(SukaSurface)) {
        HomeHero(state = state, staff = staff, onLoggedOut = { viewModel.logout(); onLoggedOut() })

        Spacer(Modifier.height(20.dp))
        Text(
            "Aplikasi Anda",
            modifier = Modifier.padding(horizontal = 20.dp),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = SukaOnSurface,
        )
        Spacer(Modifier.height(12.dp))

        val tiles = listOf(
            ModuleTile("Absensi", "Presensi wajah, checklist, cuti & kasbon", Icons.Default.Schedule, true, onOpenAbsensi),
        )

        ModuleGrid(tiles)
    }
}

@Composable
private fun HomeHero(state: HomeUiState, staff: com.sukashawarma.superapp.domain.model.StaffProfile?, onLoggedOut: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(Brush.linearGradient(colors = listOf(SukaOrange, SukaPrimaryContainer)))
    ) {
        // Aksen dekoratif — lingkaran lembut di pojok, meniru "blur" pada desain tanpa
        // biaya render blur sungguhan (cukup gradient radial pudar).
        Box(
            modifier = Modifier
                .size(140.dp)
                .align(Alignment.TopEnd)
                .offset(x = 40.dp, y = (-40).dp)
                .background(
                    Brush.radialGradient(colors = listOf(Color.White.copy(alpha = 0.22f), Color.Transparent)),
                    CircleShape,
                )
        )

        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(56.dp).clip(CircleShape).background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        staff?.name?.firstOrNull()?.uppercase() ?: "?",
                        color = SukaOrange,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("${state.greeting}, ${staff?.name ?: ""}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(staff?.outletName ?: "Semua Outlet", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    IconButton(onClick = onLoggedOut) {
                        Icon(Icons.Default.Logout, contentDescription = "Keluar", tint = Color.White)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(state.dateLabel, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))
            AttendanceStatusPill(state)
        }
    }
}

@Composable
private fun AttendanceStatusPill(state: HomeUiState) {
    val att = state.todayAttendance
    val (bg, icon, label) = when {
        state.loadingAttendance -> Triple(Color.White.copy(alpha = 0.22f), Icons.Default.WatchLater, "Memuat status absen...")
        att == null -> Triple(StatusRed, Icons.Default.ErrorOutline, "Belum Absen Masuk")
        att.type == "in" -> Triple(StatusEmerald, Icons.Default.CheckCircle, "Absen Masuk tercatat")
        else -> Triple(StatusAmber, Icons.Default.CheckCircle, "Absen Pulang tercatat")
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg.copy(alpha = 0.9f))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ModuleGrid(tiles: List<ModuleTile>) {
    Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        tiles.forEach { tile -> ModuleCard(tile) }
    }
}

@Composable
private fun ModuleCard(tile: ModuleTile) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    // Micro-interaction: kartu sedikit mengecil saat ditekan — sentuhan modern yang sama
    // dipakai di elemen interaktif lain (mesh wajah, bingkai kamera) di app ini.
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, tween(120), label = "moduleCardScale")
    val borderColor by animateColorAsState(
        if (pressed) SukaOrange.copy(alpha = 0.4f) else SukaSurfaceContainer,
        tween(120),
        label = "moduleCardBorder",
    )

    Card(
        onClick = tile.onClick,
        enabled = tile.enabled,
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SukaSurfaceContainerLowest),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        interactionSource = interactionSource,
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(SukaSurfaceContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(tile.icon, contentDescription = null, tint = SukaOrange)
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(tile.label, fontWeight = FontWeight.Bold, color = SukaOnSurface, fontSize = 15.sp)
                Spacer(Modifier.height(2.dp))
                Text(tile.desc, fontSize = 13.sp, color = SukaOnSurfaceVariant)
            }
        }
    }
}
