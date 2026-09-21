package com.sukashawarma.superapp.feature.chat.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FormatColorReset
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sukashawarma.superapp.feature.chat.data.ChatWallpaperPrefs
import com.sukashawarma.superapp.feature.chat.data.ChatWallpapers
import com.sukashawarma.superapp.feature.chat.data.ItemWallpaper
import com.sukashawarma.superapp.feature.chat.data.WallpaperLatarChat
import com.sukashawarma.superapp.feature.chat.data.WarnaWallpaper
import java.io.File

private val BiruIos = Color(0xFF007AFF)
private val LatarGrup = Color(0xFFF2F2F7)
private val GarisTipis = Color(0x2E3C3C43)
private val AbuKeterangan = Color(0xFF8E8E93)

/**
 * Lembar pemilihan dan kustomisasi wallpaper obrolan lengkap.
 *
 * Mendukung:
 * 1. Foto Kustom dari galeri perangkat (disimpan lokal di internal storage).
 * 2. Warna Solid Minimalis (palet modern ala WhatsApp & iOS).
 * 3. Preset Ilustrasi Bawaan (10 ilustrasi seni).
 * 4. Slider Peredup (Dimming 0%-80%) untuk kontras teks obrolan.
 * 5. Pilihan lingkup penerapan: Hanya untuk Saya vs Seluruh Tim (khusus pengelola).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PilihWallpaperSheet(
    wallpaperAwal: String,
    dimmingAwal: Float = 0f,
    bisaTerapkanSemua: Boolean = false,
    judul: String = "Wallpaper Obrolan",
    onTerapkanLengkap: (idWallpaper: String, dimming: Float, untukSemua: Boolean) -> Unit,
    onResetBawaan: (() -> Unit)? = null,
    onTutup: () -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var wallpaperTerpilih by remember { mutableStateOf(wallpaperAwal) }
    var dimmingTerpilih by remember { mutableFloatStateOf(dimmingAwal.coerceIn(0f, 0.8f)) }
    var terapkanUntukSemua by remember { mutableStateOf(bisaTerapkanSemua) }
    var fotoKustomTerakhir by remember {
        mutableStateOf(ChatWallpaperPrefs.getFotoKustomTerakhir(context))
    }

    // Tab navigasi: 0: Galeri, 1: Warna Solid, 2: Ilustrasi
    var tabAktif by remember {
        val tabAwal = when {
            wallpaperAwal.startsWith("file:", ignoreCase = true) || wallpaperAwal.startsWith("/") -> 0
            wallpaperAwal.startsWith("color:", ignoreCase = true) -> 1
            else -> 2
        }
        mutableIntStateOf(tabAwal)
    }

    // Peluncur galeri untuk memilih foto kustom
    val launcherGaleri = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val pathHasil = ChatWallpaperPrefs.simpanFotoKustom(context, uri)
            if (pathHasil != null) {
                fotoKustomTerakhir = pathHasil
                wallpaperTerpilih = pathHasil
                // Beri sedikit dimming bawaan untuk foto galeri agar teks tetap jelas
                if (dimmingTerpilih < 0.15f) dimmingTerpilih = 0.20f
                Toast.makeText(context, "Foto berhasil dipilih sebagai wallpaper.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Gagal memproses foto dari galeri.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onTutup,
        sheetState = sheetState,
        containerColor = LatarGrup,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            // Header bilah aksi
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onTutup) {
                    Icon(Icons.Filled.Close, contentDescription = "Tutup", tint = Color.Black)
                }
                Spacer(Modifier.width(6.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        judul,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                    )
                    Text(
                        "Kustomisasi foto galeri, warna, atau ilustrasi",
                        fontSize = 11.5.sp,
                        color = AbuKeterangan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Button(
                    onClick = {
                        onTerapkanLengkap(wallpaperTerpilih, dimmingTerpilih, terapkanUntukSemua)
                        onTutup()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BiruIos,
                        disabledContainerColor = BiruIos.copy(alpha = 0.4f),
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text("Terapkan", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(GarisTipis)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            ) {
                // 1. Kotak Pratinjau Obrolan Langsung (Live Preview)
                item {
                    PratinjauObrolanMini(
                        wallpaperId = wallpaperTerpilih,
                        dimming = dimmingTerpilih,
                    )
                    Spacer(Modifier.height(14.dp))
                }

                // 2. Tab Bar Kategori
                item {
                    TabRow(
                        selectedTabIndex = tabAktif,
                        containerColor = Color.White,
                        contentColor = BiruIos,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[tabAktif]),
                                color = BiruIos,
                                height = 2.5.dp,
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .border(0.5.dp, GarisTipis, RoundedCornerShape(10.dp)),
                    ) {
                        Tab(
                            selected = tabAktif == 0,
                            onClick = { tabAktif = 0 },
                            text = { Text("Galeri Saya", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                            icon = { Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, Modifier.size(17.dp)) }
                        )
                        Tab(
                            selected = tabAktif == 1,
                            onClick = { tabAktif = 1 },
                            text = { Text("Warna Solid", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                            icon = { Icon(Icons.Filled.Palette, contentDescription = null, Modifier.size(17.dp)) }
                        )
                        Tab(
                            selected = tabAktif == 2,
                            onClick = { tabAktif = 2 },
                            text = { Text("Ilustrasi", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                            icon = { Icon(Icons.Filled.Image, contentDescription = null, Modifier.size(17.dp)) }
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                }

                // 3. Konten masing-masing tab
                when (tabAktif) {
                    0 -> {
                        // TAB GALERI FOTO KUSTOM
                        item {
                            KontenTabGaleri(
                                fotoTersimpan = fotoKustomTerakhir,
                                terpilih = wallpaperTerpilih.startsWith("file:", ignoreCase = true) || wallpaperTerpilih.startsWith("/"),
                                onBukaGaleri = {
                                    launcherGaleri.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                onPilihFotoTersimpan = {
                                    fotoKustomTerakhir?.let { wallpaperTerpilih = it }
                                },
                                onHapusFotoKustom = {
                                    ChatWallpaperPrefs.hapusFotoKustom(context)
                                    fotoKustomTerakhir = null
                                    if (wallpaperTerpilih.startsWith("file:", ignoreCase = true)) {
                                        wallpaperTerpilih = "default"
                                    }
                                    Toast.makeText(context, "Foto kustom dihapus.", Toast.LENGTH_SHORT).show()
                                },
                            )
                        }
                    }
                    1 -> {
                        // TAB WARNA SOLID
                        item {
                            KontenTabWarnaSolid(
                                warnaTerpilih = wallpaperTerpilih,
                                onPilihWarna = { idWarna -> wallpaperTerpilih = idWarna },
                            )
                        }
                    }
                    2 -> {
                        // TAB ILUSTRASI BAWAAN
                        item {
                            KontenTabIlustrasi(
                                wallpaperTerpilih = wallpaperTerpilih,
                                onPilihWallpaper = { idWp -> wallpaperTerpilih = idWp },
                            )
                        }
                    }
                }

                // 4. Slider Peredup Wallpaper (Dimming)
                item {
                    Spacer(Modifier.height(18.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(0.5.dp, GarisTipis, RoundedCornerShape(12.dp)),
                    ) {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "Peredup Wallpaper",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.Black,
                                )
                                Text(
                                    "${(dimmingTerpilih * 100).toInt()}%",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BiruIos,
                                )
                            }
                            Text(
                                "Gelapkan wallpaper agar tulisan pesan tetap nyaman dan mudah dibaca.",
                                fontSize = 11.5.sp,
                                color = AbuKeterangan,
                                modifier = Modifier.padding(top = 2.dp, bottom = 4.dp),
                            )
                            Slider(
                                value = dimmingTerpilih,
                                onValueChange = { dimmingTerpilih = it },
                                valueRange = 0f..0.8f,
                                colors = SliderDefaults.colors(
                                    thumbColor = BiruIos,
                                    activeTrackColor = BiruIos,
                                    inactiveTrackColor = Color(0xFFE5E5EA),
                                ),
                            )
                        }
                    }
                }

                // 5. Opsi Lingkup Terapkan (Khusus Pengelola Grup)
                if (bisaTerapkanSemua) {
                    item {
                        Spacer(Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(0.5.dp, GarisTipis, RoundedCornerShape(12.dp)),
                        ) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { terapkanUntukSemua = !terapkanUntukSemua }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "Terapkan untuk Seluruh Tim",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.Black,
                                    )
                                    Text(
                                        if (terapkanUntukSemua) "Akan mengubah wallpaper bawaan grup untuk semua staf."
                                        else "Hanya diterapkan untuk tampilan Anda di ponsel ini.",
                                        fontSize = 11.5.sp,
                                        color = AbuKeterangan,
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Switch(
                                    checked = terapkanUntukSemua,
                                    onCheckedChange = { terapkanUntukSemua = it },
                                    colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF34C759)),
                                )
                            }
                        }
                    }
                }

                // 6. Tombol Kembalikan ke Bawaan
                item {
                    Spacer(Modifier.height(14.dp))
                    OutlinedButton(
                        onClick = {
                            wallpaperTerpilih = "default"
                            dimmingTerpilih = 0f
                            onResetBawaan?.invoke()
                            onTutup()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF3B30)),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFFF3B30).copy(alpha = 0.3f))),
                    ) {
                        Icon(Icons.Filled.RestartAlt, contentDescription = null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Kembalikan ke Default Putih", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

/**
 * Overload kemudahan untuk panggilan kompatibilitas lama.
 */
@Composable
fun PilihWallpaperSheet(
    wallpaperAwal: String,
    onTerapkan: (idWallpaper: String) -> Unit,
    onTutup: () -> Unit,
) {
    PilihWallpaperSheet(
        wallpaperAwal = wallpaperAwal,
        dimmingAwal = 0f,
        bisaTerapkanSemua = false,
        onTerapkanLengkap = { id, _, _ -> onTerapkan(id) },
        onTutup = onTutup,
    )
}

/**
 * Kartu pratinjau mini ruang obrolan dengan bubble contoh di atas wallpaper.
 */
@Composable
private fun PratinjauObrolanMini(
    wallpaperId: String,
    dimming: Float,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .border(1.dp, GarisTipis, RoundedCornerShape(14.dp)),
    ) {
        Box(Modifier.fillMaxSize()) {
            // Latar wallpaper dengan peredup aktif
            WallpaperLatarChat(
                wallpaperId = wallpaperId,
                dimming = dimming,
            )

            // Simulasi dua bubble pesan obrolan
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // Bubble Masuk (Kiri)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp, 12.dp, 12.dp, 2.dp),
                        color = Color.White,
                        shadowElevation = 1.dp,
                    ) {
                        Text(
                            "Halo! Gimana tampilan wallpapernya? ✨",
                            fontSize = 11.5.sp,
                            color = Color(0xFF1C1C1E),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }

                // Bubble Keluar (Kanan)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp, 12.dp, 2.dp, 12.dp),
                        color = Color(0xFFDCF8C6), // Hijau bubble WhatsApp
                        shadowElevation = 1.dp,
                    ) {
                        Text(
                            "Keren banget, tulisan tetap terbaca jelas! 👍",
                            fontSize = 11.5.sp,
                            color = Color(0xFF1C1C1E),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Konten Tab 0: Galeri Foto Kustom
 */
@Composable
private fun KontenTabGaleri(
    fotoTersimpan: String?,
    terpilih: Boolean,
    onBukaGaleri: () -> Unit,
    onPilihFotoTersimpan: () -> Unit,
    onHapusFotoKustom: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(0.5.dp, GarisTipis, RoundedCornerShape(14.dp))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (fotoTersimpan != null) {
            // Tampilkan pratinjau foto yang sudah dipilih
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onPilihFotoTersimpan)
                    .background(if (terpilih) BiruIos.copy(alpha = 0.08f) else Color(0xFFF9F9FB))
                    .border(
                        width = if (terpilih) 2.dp else 1.dp,
                        color = if (terpilih) BiruIos else GarisTipis,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val cleanPath = fotoTersimpan.removePrefix("file:")
                AsyncImage(
                    model = File(cleanPath),
                    contentDescription = "Foto Kustom",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Foto Galeri Anda",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                        )
                        if (terpilih) {
                            Spacer(Modifier.width(6.dp))
                            Box(
                                Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(BiruIos),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                    Text(
                        "Foto tersimpan di memori aman aplikasi",
                        fontSize = 11.5.sp,
                        color = AbuKeterangan,
                    )
                }
                IconButton(onClick = onHapusFotoKustom) {
                    Icon(Icons.Filled.DeleteOutline, "Hapus Foto", tint = Color(0xFFFF3B30), modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.height(10.dp))
        }

        // Tombol Buka Galeri
        Button(
            onClick = onBukaGaleri,
            colors = ButtonDefaults.buttonColors(containerColor = BiruIos),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                if (fotoTersimpan != null) "Ganti dengan Foto Lain" else "Pilih Foto dari Galeri HP",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
        }
    }
}

/**
 * Konten Tab 1: Palet Warna Solid
 */
@Composable
private fun KontenTabWarnaSolid(
    warnaTerpilih: String,
    onPilihWarna: (String) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, GarisTipis, RoundedCornerShape(14.dp)),
    ) {
        Column(Modifier.padding(12.dp)) {
            val daftar = ChatWallpapers.DAFTAR_WARNA
            // Tampilkan dalam baris 3 kolom
            val barisList = daftar.chunked(3)
            barisList.forEachIndexed { indeksBaris, itemTrio ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    itemTrio.forEach { item ->
                        val aktif = item.id.equals(warnaTerpilih, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onPilihWarna(item.id) }
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(item.color)
                                        .border(
                                            width = if (aktif) 3.dp else 1.dp,
                                            color = if (aktif) BiruIos else GarisTipis,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (aktif) {
                                        Icon(
                                            Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = if (item.gelap) Color.White else Color.Black,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    item.nama,
                                    fontSize = 11.sp,
                                    fontWeight = if (aktif) FontWeight.Bold else FontWeight.Normal,
                                    color = if (aktif) BiruIos else Color(0xFF3C3C43),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                    // Isi spacer jika baris terakhir kurang dari 3 item
                    repeat(3 - itemTrio.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
                if (indeksBaris < barisList.lastIndex) {
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

/**
 * Konten Tab 2: Ilustrasi Bawaan
 */
@Composable
private fun KontenTabIlustrasi(
    wallpaperTerpilih: String,
    onPilihWallpaper: (String) -> Unit,
) {
    val items = ChatWallpapers.DAFTAR
    val barisList = items.chunked(2)

    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        barisList.forEach { pair ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                pair.forEach { item ->
                    Box(Modifier.weight(1f)) {
                        KartuOpsiWallpaper(
                            item = item,
                            terpilih = item.id.equals(wallpaperTerpilih, ignoreCase = true),
                            onPilih = { onPilihWallpaper(item.id) },
                        )
                    }
                }
                if (pair.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun KartuOpsiWallpaper(
    item: ItemWallpaper,
    terpilih: Boolean,
    onPilih: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onPilih),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Thumbnail kotak vertikal aspek rasio mirip layar HP (9:14)
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(9f / 13f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .then(
                    if (terpilih) Modifier.border(3.dp, BiruIos, RoundedCornerShape(12.dp))
                    else Modifier.border(1.dp, GarisTipis, RoundedCornerShape(12.dp))
                ),
        ) {
            val resId = item.drawableRes
            if (resId != null) {
                Image(
                    painter = painterResource(id = resId),
                    contentDescription = item.nama,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                // Tampilan Default Putih polos
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE5E5EA)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.FormatColorReset,
                            contentDescription = null,
                            tint = Color(0xFF8E8E93),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Putih Bersih",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF6C6C70),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            // Lencana centang jika aktif
            if (terpilih) {
                Box(
                    Modifier
                        .padding(8.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(BiruIos)
                        .align(Alignment.TopEnd),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = "Terpilih",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(5.dp))
        Text(
            text = item.nama,
            fontSize = 12.5.sp,
            fontWeight = if (terpilih) FontWeight.Bold else FontWeight.SemiBold,
            color = if (terpilih) BiruIos else Color.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Text(
            text = item.deskripsi,
            fontSize = 10.5.sp,
            color = AbuKeterangan,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}
