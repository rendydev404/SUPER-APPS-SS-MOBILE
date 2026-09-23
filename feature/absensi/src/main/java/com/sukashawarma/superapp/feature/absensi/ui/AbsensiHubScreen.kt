package com.sukashawarma.superapp.presentation.absensi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.kaca.BarisMenuKaca
import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import com.sukashawarma.superapp.core.ui.kaca.JudulKelompokMenuKaca
import com.sukashawarma.superapp.core.ui.kaca.LembarMenuKaca
import com.sukashawarma.superapp.domain.model.ADMIN_OR_HR_ROLES
import com.sukashawarma.superapp.domain.model.CHECKLIST_MANAGE_ROLES
import com.sukashawarma.superapp.domain.model.ENROLL_ALLOWED_ROLES
import com.sukashawarma.superapp.domain.model.SPV_TIER_ROLES
import com.sukashawarma.superapp.domain.session.AppSession
import kotlinx.coroutines.launch

/** [kelompok] hanya menata judul di lembar menu — gerbang aksesnya tetap [navItemsFor]. */
private data class NavItem(val label: String, val icon: ImageVector, val route: String, val kelompok: String)

private const val KELOMPOK_PANTAU = "Pemantauan"
private const val KELOMPOK_AJU = "Pengajuan"
private const val KELOMPOK_KELOLA = "Pengelolaan"

/** Cermin `navItems` di apps/absensi/src/app/dashboard/layout.tsx — daftar menu
 *  tetap mengikuti role yang sama seperti halaman web.
 *  Manajemen Checklist hanya untuk RM, Admin, dan Admin HR; Leader dan AM hanya Monitoring. */
private fun navItemsFor(
    roleAllowed: Boolean,
    isAdminOrHr: Boolean,
    canEnroll: Boolean,
    canManageChecklist: Boolean,
): List<NavItem> {
    if (roleAllowed) {
        return buildList {
            add(NavItem("Papan Kehadiran", IkonIos.Dashboard, AbsensiRoutes.PAPAN, KELOMPOK_PANTAU))
            add(NavItem("Rekap & Riwayat", IkonIos.ListBullet, AbsensiRoutes.REKAP, KELOMPOK_PANTAU))
            add(NavItem("Monitor Tutup/Buka Outlet", IkonIos.FactCheck, AbsensiRoutes.CHECKLIST_MONITOR, KELOMPOK_PANTAU))
            if (canManageChecklist) {
                add(NavItem("Kelola Tutup/Buka Outlet", IkonIos.Rule, AbsensiRoutes.CHECKLIST_MANAGE, KELOMPOK_PANTAU))
            }
            add(NavItem("Cuti", IkonIos.CalendarMonth, AbsensiRoutes.CUTI, KELOMPOK_AJU))
            add(NavItem("Kasbon", IkonIos.Payments, AbsensiRoutes.KASBON, KELOMPOK_AJU))
            if (canEnroll) add(NavItem("Enrollment Crew", IkonIos.PersonAdd, AbsensiRoutes.ENROLL, KELOMPOK_KELOLA))
            if (isAdminOrHr) add(NavItem("Pengaturan Absensi", IkonIos.Settings, AbsensiRoutes.PENGATURAN, KELOMPOK_KELOLA))
        }
    }
    if (canEnroll) {
        return listOf(NavItem("Enrollment Crew", IkonIos.PersonAdd, AbsensiRoutes.ENROLL, KELOMPOK_KELOLA))
    }
    return listOf(
        NavItem("Cuti", IkonIos.CalendarMonth, AbsensiRoutes.CUTI, KELOMPOK_AJU),
        NavItem("Kasbon", IkonIos.Payments, AbsensiRoutes.KASBON, KELOMPOK_AJU),
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
        canManageChecklist = role in CHECKLIST_MANAGE_ROLES,
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

        // Lembar menu kaca bersama (core.ui.kaca). Latar di belakangnya hanya ikut
        // diburamkan bila layar ini tersusun di dalam ShellKaca — yaitu dari pager utama;
        // rute HUB yang berdiri sendiri mendapat lembar pekat, dan itu disengaja.
        if (sheetOpen && isSheetVisible) {
            LembarMenuKaca(
                onTutup = { closeSheet(onDismiss) },
                sheetState = sheetState,
                judul = "Menu Absensi",
            ) {
                // Kelompok disusun dari urutan `navItemsFor`, jadi tidak ada kelompok kosong
                // yang perlu disaring di dalam perulangan composable.
                val kelompok = items.groupBy { it.kelompok }
                // Tetap bisa digulir: peran SPV-tier mendapat sampai delapan tujuan, dan di
                // layar pendek lembar setinggi itu akan terpotong tanpa bisa dijangkau.
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    kelompok.forEach { (judul, isi) ->
                        JudulKelompokMenuKaca(judul)
                        isi.forEach { item ->
                            BarisMenuKaca(
                                ikon = item.icon,
                                label = item.label,
                                onKlik = { closeSheet { onNavigate(item.route) } },
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MoreLanding(modifier: Modifier = Modifier) {
    // Latar & judul besar mengikuti design system iOS supaya halaman di balik lembar
    // menu kaca serasi dengan layar modul lain.
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WarnaIos.Latar),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = UkuranIos.TepiLayar + 4.dp, vertical = 20.dp),
        ) {
            Text(text = "Lainnya", style = TipeIos.JudulBesar)
            Spacer(Modifier.height(4.dp))
            Text(text = "Akses cepat ke fitur absensi lainnya.", style = TipeIos.SubJudul)
        }
    }
}
