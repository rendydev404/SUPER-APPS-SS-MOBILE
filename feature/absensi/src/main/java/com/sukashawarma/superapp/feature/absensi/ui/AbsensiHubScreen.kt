package com.sukashawarma.superapp.presentation.absensi

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import com.sukashawarma.superapp.domain.model.ADMIN_OR_HR_ROLES
import com.sukashawarma.superapp.domain.model.ENROLL_ALLOWED_ROLES
import com.sukashawarma.superapp.domain.model.SPV_TIER_ROLES
import com.sukashawarma.superapp.domain.session.AppSession

private data class NavItem(val label: String, val icon: ImageVector, val route: String)

/** Cermin `navItems` di apps/absensi/src/app/dashboard/layout.tsx (web) — dua daftar
 *  berbeda untuk SPV-tier vs crew, supaya menu di native persis sama dengan web. */
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
    return listOf(
        NavItem("Cuti", Icons.Default.CalendarMonth, AbsensiRoutes.CUTI),
        NavItem("Kasbon", Icons.Default.Payments, AbsensiRoutes.KASBON),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AbsensiHubScreen(onNavigate: (String) -> Unit, onExit: () -> Unit) {
    val staff by AppSession.staff.collectAsState()
    val role = staff?.role
    val isSpvTier = role in SPV_TIER_ROLES
    val isAdminOrHr = role in ADMIN_OR_HR_ROLES
    val canEnroll = role in ENROLL_ALLOWED_ROLES

    val items = navItemsFor(isSpvTier, isAdminOrHr, canEnroll)

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.White),
                title = { 
                    Text(
                        "Lainnya", 
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        color = com.sukashawarma.superapp.presentation.theme.SukaInk
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onExit) { 
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Kembali",
                            tint = com.sukashawarma.superapp.presentation.theme.SukaOrange
                        ) 
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(com.sukashawarma.superapp.presentation.theme.SukaGray50),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items) { item ->
                androidx.compose.material3.Surface(
                    color = androidx.compose.ui.graphics.Color.White,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth().clickable { onNavigate(item.route) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    com.sukashawarma.superapp.presentation.theme.SukaOrange.copy(alpha = 0.1f), 
                                    androidx.compose.foundation.shape.CircleShape
                                ),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            Icon(
                                item.icon, 
                                contentDescription = null,
                                tint = com.sukashawarma.superapp.presentation.theme.SukaOrange,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                            color = com.sukashawarma.superapp.presentation.theme.SukaInk,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = com.sukashawarma.superapp.presentation.theme.SukaGray400,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

