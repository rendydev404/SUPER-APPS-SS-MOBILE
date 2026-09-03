package com.sukashawarma.superapp.presentation.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.presentation.theme.*

private data class ModuleTile(
    val label: String,
    val desc: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val stats: List<Triple<String, String, Color>>,
)

/**
 * Role yang boleh membuka modul Stok — cermin `isLeaderOrSPV` di `BottomNav.tsx` web,
 * dikurangi `spv` yang sudah tidak dipakai lagi (digantikan `regional_manager`).
 *
 * Perbedaan antar role ini hanya pada cakupan outlet, bukan pada fitur: leader dan
 * area manager memegang beberapa outlet binaan lewat `staff_outlets`, regional manager
 * memegang seluruh outlet, dan crew hanya outletnya sendiri. Pembedaan itu sudah
 * ditangani `accessible_outlet_ids()` di database, jadi tidak ada cabang role di sini.
 */
private val STOK_ROLES = setOf(
    com.sukashawarma.superapp.domain.model.Role.CREW,
    com.sukashawarma.superapp.domain.model.Role.LEADER,
    com.sukashawarma.superapp.domain.model.Role.AREA_MANAGER,
    com.sukashawarma.superapp.domain.model.Role.REGIONAL_MANAGER,
)

@Composable
fun HomeScreen(
    onOpenAbsensi: () -> Unit,
    onOpenStok: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val staff = state.staff
    Column(Modifier.fillMaxSize().background(SukaSurface).verticalScroll(rememberScrollState())) {
        HomeHero(state, staff, { viewModel.logout(); onLoggedOut() })
        AttendanceSummaryCard(state)
        Column(Modifier.padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(24.dp))
            Text("Aplikasi Anda", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = SukaOnSurface)
            Spacer(Modifier.height(4.dp))
            Text("Semua kebutuhan operasional dalam satu tempat", fontSize = 12.sp, color = SukaOnSurfaceVariant)
            Spacer(Modifier.height(14.dp))
            ModuleCard(
                ModuleTile(
                    "Absensi",
                    "Presensi wajah, checklist, cuti & kasbon",
                    Icons.Default.Fingerprint,
                    onOpenAbsensi,
                    listOf(
                        Triple("PRESENSI", "Biometrik", SukaOnSurface),
                        Triple("CHECKLIST", "Tersedia", Color(0xFFEA580C)),
                        Triple("CUTI", "Tersedia", Color(0xFF168451)),
                    ),
                )
            )
            if (staff?.role in STOK_ROLES) {
                Spacer(Modifier.height(14.dp))
                ModuleCard(
                    ModuleTile(
                        "Stok",
                        "Pantau saldo bahan, riwayat mutasi & estimasi produksi",
                        Icons.Default.Inventory2,
                        onOpenStok,
                        listOf(
                            Triple("MONITORING", "Realtime", SukaOnSurface),
                            Triple("RIWAYAT", "Tersedia", Color(0xFFEA580C)),
                            Triple("PRODUKSI", "Estimasi", Color(0xFF168451)),
                        ),
                    )
                )
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun HomeHero(state: HomeUiState, staff: com.sukashawarma.superapp.domain.model.StaffProfile?, onLoggedOut: () -> Unit) {
    Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color(0xFFEA580C), Color(0xFFF97316), SukaSurface)))) {
        Box(Modifier.size(220.dp).align(Alignment.TopEnd).offset(x = 72.dp, y = (-78).dp).background(Color.White.copy(alpha = 0.10f), CircleShape))
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(50), color = Color.White.copy(alpha = 0.22f)) {
                    Row(Modifier.padding(start = 6.dp, end = 10.dp, top = 5.dp, bottom = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("SUKA", Modifier.background(Color.White, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 4.dp), color = Color(0xFFEA580C), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp)
                        Spacer(Modifier.width(6.dp))
                        Box(Modifier.size(6.dp).background(Color(0xFF34D399), CircleShape))
                        Spacer(Modifier.width(4.dp))
                        Text("Online", color = Color.White.copy(alpha = 0.92f), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.weight(1f))
                Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.18f)) {
                    IconButton(onClick = onLoggedOut) { Icon(Icons.Default.Logout, "Keluar", tint = Color.White, modifier = Modifier.size(17.dp)) }
                }
            }
            Spacer(Modifier.height(18.dp))
            Surface(shape = RoundedCornerShape(18.dp), color = Color.White.copy(alpha = 0.12f), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(54.dp).background(Color.White, RoundedCornerShape(15.dp)), contentAlignment = Alignment.Center) {
                        Text(staff?.name?.firstOrNull()?.uppercase() ?: "?", color = Color(0xFFEA580C), fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${state.greeting},", color = Color(0xFFFFEDD5), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Text(staff?.name ?: "Pengguna", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, null, tint = Color(0xFFFFD5B5), modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(3.dp))
                            Text(staff?.outletName ?: "Semua Outlet", color = Color(0xFFFFEDD5), fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AttendanceSummaryCard(state: HomeUiState) {
    val att = state.todayAttendance
    val status = when {
        state.loadingAttendance -> Triple(Icons.Default.WatchLater, Color(0xFF64748B), "Memuat status")
        att == null -> Triple(Icons.Default.ErrorOutline, Color(0xFFDC2626), "Belum absen masuk")
        att.type == "in" -> Triple(Icons.Default.CheckCircle, Color(0xFF168451), "Absen masuk")
        else -> Triple(Icons.Default.CheckCircle, Color(0xFFC27A12), "Absen pulang")
    }
    Surface(Modifier.padding(horizontal = 20.dp).offset(y = (-8).dp), shape = RoundedCornerShape(24.dp), color = Color.White, border = BorderStroke(1.dp, Color(0xFFF1F5F9)), shadowElevation = 5.dp) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarToday, null, tint = Color(0xFFF97316), modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text(state.dateLabel, Modifier.weight(1f), color = SukaOnSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFFFF7ED), border = BorderStroke(1.dp, Color(0xFFFFEDD5))) {
                    Text("Hari ini", Modifier.padding(horizontal = 9.dp, vertical = 5.dp), color = Color(0xFFC2410C), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.Top) {
                Box(Modifier.size(40.dp).background(status.second.copy(alpha = 0.10f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                    Icon(status.first, null, tint = status.second, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(status.third, color = SukaOnSurface, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    Text(if (att == null) "Jangan lupa catat kehadiranmu hari ini" else "Kehadiranmu hari ini sudah tercatat", color = SukaOnSurfaceVariant, fontSize = 11.sp, lineHeight = 16.sp)
                }
            }
            Spacer(Modifier.height(13.dp))
            Text("Jaga kedisiplinan dan tetap semangat hari ini.", Modifier.fillMaxWidth().background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp)).padding(11.dp), color = Color(0xFF64748B), fontSize = 10.sp, lineHeight = 15.sp)
        }
    }
}

@Composable
private fun ModuleCard(tile: ModuleTile) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.975f else 1f, tween(120), label = "moduleCardScale")
    Card(onClick = tile.onClick, Modifier.fillMaxWidth().graphicsLayer(scaleX = scale, scaleY = scale), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFF1F5F9)), elevation = CardDefaults.cardElevation(defaultElevation = 3.dp), interactionSource = interactionSource) {
        Column(Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth().padding(bottom = 15.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(56.dp).background(Brush.linearGradient(listOf(Color(0xFFFFF7ED), Color(0xFFFFEDD5))), RoundedCornerShape(17.dp)), contentAlignment = Alignment.Center) {
                    Icon(tile.icon, null, tint = Color(0xFFEA580C), modifier = Modifier.size(29.dp))
                }
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text(tile.label, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = SukaOnSurface)
                    Spacer(Modifier.height(3.dp))
                    Text(tile.desc, fontSize = 11.sp, lineHeight = 15.sp, color = Color(0xFF64748B))
                }
                Surface(shape = CircleShape, color = Color(0xFFF8FAFC), border = BorderStroke(1.dp, Color(0xFFF1F5F9)), modifier = Modifier.size(36.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.ArrowForward, "Buka ${tile.label}", tint = Color(0xFF94A3B8), modifier = Modifier.size(17.dp)) }
                }
            }
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Row(Modifier.fillMaxWidth().padding(top = 13.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                tile.stats.forEach { (label, value, color) -> LauncherStat(label, value, color) }
            }
        }
    }
}

@Composable
private fun LauncherStat(label: String, value: String, color: Color) {
    Column(Modifier.widthIn(min = 72.dp), horizontalAlignment = Alignment.Start) {
        Text(label, color = Color(0xFF94A3B8), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.7.sp)
        Spacer(Modifier.height(3.dp))
        Text(value, color = color, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
    }
}
