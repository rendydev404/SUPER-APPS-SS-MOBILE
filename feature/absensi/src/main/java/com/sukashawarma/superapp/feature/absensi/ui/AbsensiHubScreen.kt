package com.sukashawarma.superapp.presentation.absensi

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sukashawarma.superapp.domain.model.ADMIN_OR_HR_ROLES
import com.sukashawarma.superapp.domain.model.ENROLL_ALLOWED_ROLES
import com.sukashawarma.superapp.domain.model.SPV_TIER_ROLES
import com.sukashawarma.superapp.domain.session.AppSession
import kotlinx.coroutines.launch

private data class NavItem(val label: String, val icon: ImageVector, val route: String)

/** Cermin `navItems` di apps/absensi/src/app/dashboard/layout.tsx — daftar menu
 *  tetap mengikuti role yang sama seperti halaman web. */
private fun navItemsFor(roleAllowed: Boolean, isAdminOrHr: Boolean, canEnroll: Boolean): List<NavItem> {
    if (roleAllowed) {
        return buildList {
            add(NavItem("Papan Kehadiran", Icons.Default.Dashboard, AbsensiRoutes.PAPAN))
            add(NavItem("Rekap & Riwayat", Icons.AutoMirrored.Filled.List, AbsensiRoutes.REKAP))
            add(NavItem("Monitor Checklist", Icons.Default.FactCheck, AbsensiRoutes.CHECKLIST_MONITOR))
            add(NavItem("Manajemen Checklist", Icons.Default.Rule, AbsensiRoutes.CHECKLIST_MANAGE))
            add(NavItem("Cuti", Icons.Default.CalendarMonth, AbsensiRoutes.CUTI))
            add(NavItem("Kasbon", Icons.Default.Payments, AbsensiRoutes.KASBON))
            if (canEnroll) add(NavItem("Enrollment Crew", Icons.Default.PersonAdd, AbsensiRoutes.ENROLL))
            if (isAdminOrHr) add(NavItem("Pengaturan Absensi", Icons.Default.Settings, AbsensiRoutes.PENGATURAN))
        }
    }
    if (canEnroll) {
        return listOf(NavItem("Enrollment Crew", Icons.Default.PersonAdd, AbsensiRoutes.ENROLL))
    }
    return listOf(
        NavItem("Cuti", Icons.Default.CalendarMonth, AbsensiRoutes.CUTI),
        NavItem("Kasbon", Icons.Default.Payments, AbsensiRoutes.KASBON),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AbsensiHubScreen(
    onNavigate: (String) -> Unit,
    onExit: () -> Unit,
    isSheetVisible: Boolean = true,
    onDismiss: () -> Unit = onExit,
    showBackground: Boolean = true,
) {
    val staff by AppSession.staff.collectAsState()
    val role = staff?.role
    val items = navItemsFor(
        roleAllowed = role in SPV_TIER_ROLES,
        isAdminOrHr = role in ADMIN_OR_HR_ROLES,
        canEnroll = role in ENROLL_ALLOWED_ROLES,
    )

    var sheetOpen by rememberSaveable { mutableStateOf(isSheetVisible) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    fun closeSheet(afterClosed: () -> Unit = {}) {
        coroutineScope.launch {
            sheetState.hide()
            sheetOpen = false
            afterClosed()
        }
    }

    LaunchedEffect(isSheetVisible) {
        sheetOpen = isSheetVisible
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (showBackground) {
            MoreLanding(
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (sheetOpen && isSheetVisible) {
            ModalBottomSheet(
                onDismissRequest = { closeSheet(onDismiss) },
                sheetState = sheetState,
                containerColor = Color.White,
                contentColor = Color(0xFF400A07),
                tonalElevation = 0.dp,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .size(width = 40.dp, height = 4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFFD1D5DB)),
                    )
                },
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                ) {
                    SheetHeader(itemCount = items.size)
                    HorizontalDivider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items.forEach { item ->
                            SheetMenuItem(
                                item = item,
                                onClick = { closeSheet { onNavigate(item.route) } },
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun MoreLanding(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
        ) {
            Text(
                text = "Lainnya",
                color = Color(0xFF400A07),
                fontSize = 28.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Akses cepat ke fitur absensi lainnya.",
                color = Color(0xFF6B7280),
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun SheetHeader(itemCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFFFE9DD)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.MoreHoriz,
                contentDescription = null,
                tint = Color(0xFFA23F00),
                modifier = Modifier.size(28.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Menu lainnya",
                color = Color(0xFF1A1C1E),
                fontSize = 22.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "$itemCount fitur siap digunakan",
                color = Color(0xFF635D59),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun SheetMenuItem(item: NavItem, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = tween(120),
        label = "moreItemScale",
    )
    val containerColor by animateColorAsState(
        targetValue = if (pressed) Color(0xFFFFF3EC) else Color(0xFFF9FAFB),
        animationSpec = tween(120),
        label = "moreItemColor",
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        color = containerColor,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFFFE9DD)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = Color(0xFFA23F00),
                    modifier = Modifier.size(22.dp),
                )
            }
            Text(
                text = item.label,
                color = Color(0xFF1A1C1E),
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Buka ${item.label}",
                tint = Color(0xFF9CA3AF),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
