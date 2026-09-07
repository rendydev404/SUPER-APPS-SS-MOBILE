package com.sukashawarma.superapp.presentation.home

import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sukashawarma.superapp.presentation.theme.*

/** Satu angka pada kaki kartu modul. [menonjol] mewarnainya dengan aksen modul,
 *  dipakai untuk angka yang menuntut tindakan (stok kritis, kiriman menunggu). */
private data class Sorotan(val label: String, val nilai: String, val menonjol: Boolean = false)

private data class ModuleTile(
    val label: String,
    val desc: String,
    val icon: ImageVector,
    /** Warna identitas modul: bidang ikon, lencana, dan angka yang menonjol. */
    val aksen: Color,
    val onClick: () -> Unit,
    val sorotan: List<Sorotan>,
    /** Pil kecil di sebelah judul; hanya muncul kalau ada yang perlu dikerjakan. */
    val lencana: String? = null,
    val memuat: Boolean = false,
)

/** Angka yang belum termuat tampil "—", bukan "0": nol adalah kabar baik dan tidak
 *  boleh tertukar dengan "belum tahu". */
private fun Int?.atau(): String = this?.toString() ?: "—"

private val AKSEN_ABSENSI = Color(0xFFEA580C)
private val AKSEN_STOK = Color(0xFF0EA5E9)
private val AKSEN_DISTRIBUSI = Color(0xFF6366F1)
private val AKSEN_MANAGER = Color(0xFFE11D48)
private val AKSEN_POS = Color(0xFF059669)

@Composable
fun HomeScreen(
    onOpenAbsensi: () -> Unit,
    onOpenStok: () -> Unit,
    onOpenDistribusi: () -> Unit,
    onOpenManager: () -> Unit,
    onLoggedOut: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val staff = state.staff
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.pesanPos.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }
    Column(Modifier.fillMaxSize().background(SukaSurface).verticalScroll(rememberScrollState())) {
        HomeHero(state, staff, { viewModel.logout(); onLoggedOut() }, onOpenSettings)
        AttendanceSummaryCard(state)
        Column(Modifier.padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(24.dp))
            Text("Aplikasi Anda", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = SukaOnSurface)
            Spacer(Modifier.height(4.dp))
            Text("Semua kebutuhan operasional dalam satu tempat", fontSize = 12.sp, color = SukaOnSurfaceVariant)
            Spacer(Modifier.height(14.dp))
            ModuleCard(
                ModuleTile(
                    label = "Absensi",
                    desc = "Presensi wajah, checklist, cuti & kasbon",
                    icon = Icons.Default.Fingerprint,
                    aksen = AKSEN_ABSENSI,
                    onClick = onOpenAbsensi,
                    memuat = state.loadingAttendance,
                    lencana = if (!state.loadingAttendance && state.todayAttendance == null) "Belum absen" else null,
                    sorotan = listOf(
                        Sorotan(
                            "HARI INI",
                            when (state.todayAttendance?.type) {
                                "in" -> "Sudah masuk"
                                null -> "Belum absen"
                                else -> "Sudah pulang"
                            },
                            menonjol = state.todayAttendance == null,
                        ),
                        Sorotan("JAM", state.jamAbsen ?: "—"),
                        Sorotan("CUTI & KASBON", "Tersedia"),
                    ),
                )
            )
            if (staff?.role in STOK_ROLES) {
                // Role pusat tidak terikat outlet, jadi angka saldo tidak ada artinya
                // buat mereka — kartunya menyebut pekerjaan yang memang mereka lakukan.
                val pusat = staff?.role in STOK_ROLES_PUSAT
                Spacer(Modifier.height(14.dp))
                ModuleCard(
                    ModuleTile(
                        label = "Stok",
                        desc = if (pusat) {
                            "Setujui permintaan bahan, terima PO & pantau harga"
                        } else {
                            "Pantau saldo bahan, riwayat mutasi & estimasi produksi"
                        },
                        icon = Icons.Default.Inventory2,
                        aksen = AKSEN_STOK,
                        onClick = onOpenStok,
                        memuat = state.memuatSorotan,
                        lencana = if (pusat) null
                            else state.stokKritis?.takeIf { it > 0 }?.let { "$it kritis" },
                        sorotan = if (pusat) {
                            listOf(
                                Sorotan("PERMINTAAN", "Antrean"),
                                Sorotan("PO", "Penerimaan"),
                                Sorotan("HARGA", "Master"),
                            )
                        } else {
                            listOf(
                                Sorotan("KRITIS", state.stokKritis.atau(), menonjol = (state.stokKritis ?: 0) > 0),
                                Sorotan("MENIPIS", state.stokMenipis.atau()),
                                Sorotan("PRODUKSI", "Estimasi"),
                            )
                        },
                    )
                )
            }
            if (staff?.role in DISTRIBUSI_ROLES) {
                Spacer(Modifier.height(14.dp))
                ModuleCard(
                    ModuleTile(
                        label = "Distribusi",
                        desc = "Terima kiriman, verifikasi barang & riwayat surat jalan",
                        icon = Icons.Default.LocalShipping,
                        aksen = AKSEN_DISTRIBUSI,
                        onClick = onOpenDistribusi,
                        memuat = state.memuatSorotan,
                        lencana = state.kirimanMenunggu?.takeIf { it > 0 }?.let { "$it menunggu" },
                        sorotan = listOf(
                            Sorotan(
                                "MENUNGGU",
                                state.kirimanMenunggu.atau(),
                                menonjol = (state.kirimanMenunggu ?: 0) > 0,
                            ),
                            Sorotan("PENERIMAAN", "Scan QR"),
                            Sorotan("VERIFIKASI", "Per Item"),
                        ),
                    )
                )
            }
            if (staff?.role in MANAGER_ROLES) {
                Spacer(Modifier.height(14.dp))
                ModuleCard(
                    ModuleTile(
                        label = "Manager",
                        desc = "Ringkasan area, performa zona & peringkat outlet",
                        icon = Icons.Default.Insights,
                        aksen = AKSEN_MANAGER,
                        onClick = onOpenManager,
                        memuat = state.memuatSorotan,
                        lencana = state.wasteMenunggu?.takeIf { it > 0 }?.let { "$it waste" },
                        sorotan = listOf(
                            Sorotan(
                                "WASTE",
                                state.wasteMenunggu.atau(),
                                menonjol = (state.wasteMenunggu ?: 0) > 0,
                            ),
                            Sorotan("OMZET", "Realtime"),
                            Sorotan("ZONA AM", "Peringkat"),
                        ),
                    )
                )
            }
            Spacer(Modifier.height(14.dp))
            ModuleCard(
                ModuleTile(
                    label = "POS",
                    desc = "Kasir, pesanan & cetak struk di aplikasi POS",
                    icon = Icons.Default.PointOfSale,
                    aksen = AKSEN_POS,
                    onClick = { viewModel.bukaPos(context) },
                    memuat = state.membukaPos,
                    sorotan = listOf(
                        Sorotan("KASIR", "Aplikasi POS"),
                        Sorotan("MASUK", "Tanpa login"),
                        Sorotan("STRUK", "Cetak"),
                    ),
                )
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun HomeHero(state: HomeUiState, staff: com.sukashawarma.superapp.domain.model.StaffProfile?, onLoggedOut: () -> Unit, onOpenSettings: () -> Unit) {
    Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color(0xFFEA580C), Color(0xFFF97316), SukaSurface)))) {
        Box(Modifier.size(220.dp).align(Alignment.TopEnd).offset(x = 72.dp, y = (-78).dp).background(Color.White.copy(alpha = 0.10f), CircleShape))
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 24.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, "Pengaturan", tint = Color.White, modifier = Modifier.size(17.dp)) }
                }
                Spacer(Modifier.width(8.dp))
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

/**
 * Kartu modul: identitas warna per modul, angka hidup di kaki kartu, dan lencana
 * yang hanya muncul kalau ada yang perlu dikerjakan.
 *
 * Sengaja hemat: satu animasi tekan (skala + dorongan panah) yang hanya hidup
 * selama jari menyentuh, tanpa animasi berulang dan tanpa bayangan besar. Kartu
 * ini muncul empat sampai lima kali di layar yang bisa di-scroll, jadi apa pun
 * yang berjalan terus-menerus akan terasa di perangkat kelas bawah.
 */
@Composable
private fun ModuleCard(tile: ModuleTile) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.975f else 1f, tween(120), label = "moduleCardScale")
    val dorongPanah by animateDpAsState(if (pressed) 3.dp else 0.dp, tween(120), label = "moduleCardArrow")
    val perluTindakan = tile.lencana != null

    Card(
        onClick = tile.onClick,
        modifier = Modifier.fillMaxWidth().graphicsLayer(scaleX = scale, scaleY = scale),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        // Kartu yang menuntut tindakan dibingkai warnanya sendiri, jadi terbaca
        // dari ujung mata tanpa perlu membaca angkanya lebih dulu.
        border = BorderStroke(1.dp, if (perluTindakan) tile.aksen.copy(alpha = 0.35f) else Color(0xFFF1F5F9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        interactionSource = interactionSource,
    ) {
        Box(
            Modifier.background(
                // Sapuan tipis dari sudut ikon; gradasi linear jauh lebih murah
                // daripada bayangan berwarna atau blur.
                Brush.linearGradient(
                    listOf(tile.aksen.copy(alpha = if (perluTindakan) 0.10f else 0.05f), Color.Transparent),
                )
            )
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(54.dp)
                            .background(
                                Brush.linearGradient(listOf(tile.aksen, tile.aksen.copy(alpha = 0.72f))),
                                RoundedCornerShape(18.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(tile.icon, null, tint = Color.White, modifier = Modifier.size(27.dp))
                    }
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(tile.label, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = SukaOnSurface)
                            if (tile.lencana != null) {
                                Spacer(Modifier.width(7.dp))
                                Lencana(tile.lencana, tile.aksen)
                            }
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(tile.desc, fontSize = 11.sp, lineHeight = 15.sp, color = Color(0xFF64748B))
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier
                            .offset(x = dorongPanah)
                            .size(36.dp)
                            .background(tile.aksen.copy(alpha = 0.10f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.ArrowForward,
                            "Buka ${tile.label}",
                            tint = tile.aksen,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                }
                Spacer(Modifier.height(15.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Row(Modifier.fillMaxWidth().padding(top = 13.dp)) {
                    tile.sorotan.forEach { sorotan ->
                        SorotanKolom(sorotan, tile.aksen, tile.memuat, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/** Pil kecil penanda ada yang perlu dikerjakan. */
@Composable
private fun Lencana(teks: String, aksen: Color) {
    Surface(shape = RoundedCornerShape(50), color = aksen.copy(alpha = 0.12f)) {
        Text(
            teks,
            Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            color = aksen,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

@Composable
private fun SorotanKolom(sorotan: Sorotan, aksen: Color, memuat: Boolean, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            sorotan.label,
            color = Color(0xFF94A3B8),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.7.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        if (memuat) {
            // Balok diam, bukan shimmer: satu animasi berulang per sorotan berarti
            // belasan animasi berjalan bersamaan di beranda.
            Box(Modifier.height(11.dp).fillMaxWidth(0.66f).background(Color(0xFFEEF2F6), RoundedCornerShape(4.dp)))
        } else {
            Text(
                sorotan.nilai,
                color = if (sorotan.menonjol) aksen else SukaOnSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
