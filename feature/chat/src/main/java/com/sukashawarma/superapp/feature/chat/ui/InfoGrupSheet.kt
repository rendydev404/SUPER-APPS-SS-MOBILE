package com.sukashawarma.superapp.feature.chat.ui

import com.sukashawarma.superapp.core.ui.ios.BarisIos
import com.sukashawarma.superapp.core.ui.ios.LabelSeksiIos
import com.sukashawarma.superapp.core.ui.ios.LencanaIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.PemisahIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.TombolUtamaIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.ios.bayanganIos
import com.sukashawarma.superapp.core.ui.ios.permukaanIos
import com.sukashawarma.superapp.core.ui.ios.tekanIos
import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FormatColorReset
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sukashawarma.superapp.core.ui.AvatarStorage
import com.sukashawarma.superapp.core.ui.AvatarStaf
import com.sukashawarma.superapp.feature.chat.data.AnggotaGrup
import com.sukashawarma.superapp.feature.chat.data.ChatWallpaperPrefs
import com.sukashawarma.superapp.feature.chat.data.ChatWallpapers
import com.sukashawarma.superapp.feature.chat.data.PengaturanGrup
import com.sukashawarma.superapp.feature.chat.data.WallpaperLatarChat
import com.sukashawarma.superapp.feature.chat.data.labelRole

/**
 * Info grup bergaya iOS (Settings "inset grouped"): kartu putih membulat di
 * atas latar abu, label bagian huruf kapital kecil, pemisah tipis.
 *
 * Satu layar untuk semua orang — bukan dua. Yang bukan pengelola tetap bisa
 * membukanya dan membaca nama, foto, deskripsi, serta status mode pengumuman;
 * kolomnya saja yang tidak bisa disunting. Menyembunyikan halamannya sama
 * sekali hanya membuat pengguna bertanya-tanya ke mana perginya keterangan grup.
 */
// Lembar pengaturan memakai aksen aplikasi (bukan biru percakapan) supaya
// seragam dengan grup pengaturan di modul lain.
private val BiruIosInfo = WarnaIos.Aksen
private val AbuInfo = WarnaIos.LabelKedua
private val LatarGrup = WarnaIos.Latar
private val PemisahInfo = WarnaIos.Pemisah

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoGrupSheet(
    awal: PengaturanGrup,
    bolehSunting: Boolean,
    bolehUbahWallpaper: Boolean = false,
    menyimpan: Boolean,
    galat: String?,
    anggota: List<AnggotaGrup>,
    memuatAnggota: Boolean,
    userId: String,
    onKlikAnggota: (AnggotaGrup) -> Unit,
    onSimpan: (nama: String, deskripsi: String, hanyaAdmin: Boolean, fotoJpeg: ByteArray?, hapusFoto: Boolean, wallpaper: String) -> Unit,
    onGantiWallpaper: (idWallpaper: String, dimming: Float, untukSemua: Boolean) -> Unit = { _, _, _ -> },
    onTutup: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val konteks = LocalContext.current

    var nama by remember { mutableStateOf(awal.namaGrup) }
    var deskripsi by remember { mutableStateOf(awal.deskripsi) }
    var hanyaAdmin by remember { mutableStateOf(awal.hanyaAdmin) }
    var fotoBaru by remember { mutableStateOf<ByteArray?>(null) }
    var pratinjauBaru by remember { mutableStateOf<ImageBitmap?>(null) }
    var hapusFoto by remember { mutableStateOf(false) }
    var wallpaper by remember(awal.wallpaper) { mutableStateOf(awal.wallpaper) }
    var sheetPilihWallpaper by remember { mutableStateOf(false) }
    var revisiWallpaperLokal by remember { mutableIntStateOf(0) }
    val wallpaperPribadi = remember(revisiWallpaperLokal) { ChatWallpaperPrefs.getWallpaperTim(konteks) }
    val dimmingPribadi = remember(revisiWallpaperLokal) { ChatWallpaperPrefs.getDimmingTim(konteks) }
    val wallpaperAktif = if (bolehUbahWallpaper && wallpaperPribadi != ChatWallpaperPrefs.ID_BAWAAN) wallpaperPribadi else wallpaper
    // Tertutup saat dibuka: daftar anggota se-perusahaan jauh lebih panjang
    // daripada seluruh isi lembar ini digabung, dan menggulirinya untuk mencapai
    // tombol Simpan bukan yang dicari orang saat membuka info grup.
    var anggotaTerbuka by remember { mutableStateOf(false) }

    val pilihFoto = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val jpeg = FotoChat.bacaJpegKecil(konteks, uri) ?: return@rememberLauncherForActivityResult
        fotoBaru = jpeg
        pratinjauBaru = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)?.asImageBitmap()
        hapusFoto = false
    }

    val berubah = nama != awal.namaGrup || deskripsi != awal.deskripsi ||
        hanyaAdmin != awal.hanyaAdmin || fotoBaru != null || hapusFoto ||
        wallpaper != awal.wallpaper

    ModalBottomSheet(
        onDismissRequest = onTutup,
        sheetState = sheetState,
        containerColor = LatarGrup,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                // Daftar anggota membuat lembar ini jauh melampaui tinggi layar;
                // tanpa gulir, tombol Simpan dan sebagian anggota tak terjangkau.
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            FotoGrupBesar(
                pratinjauBaru = pratinjauBaru,
                pathSekarang = if (hapusFoto) null else awal.fotoGrup,
                nama = nama,
                bolehSunting = bolehSunting,
                onGanti = {
                    pilihFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
            )

            if (bolehSunting && (awal.fotoGrup != null || fotoBaru != null) && !hapusFoto) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Hapus foto",
                    fontSize = 15.sp,
                    color = WarnaIos.Merah,
                    modifier = Modifier
                        .clip(UkuranIos.SudutKontrol)
                        .clickable { fotoBaru = null; pratinjauBaru = null; hapusFoto = true }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }

            Spacer(Modifier.height(10.dp))
            if (!bolehSunting) {
                Text(nama, style = TipeIos.Judul2)
                Text(
                    deskripsi.ifBlank { "Tanpa deskripsi" },
                    style = TipeIos.Catatan,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, start = 12.dp, end = 12.dp),
                )
            }

            Spacer(Modifier.height(20.dp))

            if (bolehSunting) {
                LabelBagian("Nama grup")
                Kartu {
                    IsianBaris(nama, "Chat Tim", satuBaris = true) { nama = it.take(40) }
                }
                Spacer(Modifier.height(18.dp))
                LabelBagian("Deskripsi")
                Kartu {
                    IsianBaris(deskripsi, "Keterangan singkat grup", satuBaris = false) {
                        deskripsi = it.take(140)
                    }
                }
                Spacer(Modifier.height(18.dp))
                LabelBagian("Siapa yang boleh mengirim")
                Kartu {
                    BarisIos(
                        judul = "Mode pengumuman",
                        keterangan = "Hanya pengelola yang bisa mengirim.",
                        ikon = Icons.Filled.Campaign,
                        trailing = {
                            Switch(
                                checked = hanyaAdmin,
                                onCheckedChange = { hanyaAdmin = it },
                                colors = SwitchDefaults.colors(checkedTrackColor = WarnaIos.Hijau),
                            )
                        },
                    )
                }
            } else {
                LabelBagian("Aturan grup")
                Kartu {
                    BarisInfo(
                        ikon = Icons.Filled.Campaign,
                        judul = if (hanyaAdmin) "Mode pengumuman aktif" else "Semua anggota bisa mengirim",
                        isi = if (hanyaAdmin) "Hanya pengelola yang dapat mengirim pesan."
                        else "Semua orang di perusahaan dapat mengirim pesan.",
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            LabelBagian("Wallpaper Chat")
            Kartu {
                val namaWallpaper = remember(wallpaperAktif) {
                    when {
                        wallpaperAktif.startsWith("file:", ignoreCase = true) || wallpaperAktif.startsWith("/") -> "Foto Galeri Kustom"
                        wallpaperAktif.startsWith("color:", ignoreCase = true) -> {
                            ChatWallpapers.DAFTAR_WARNA.firstOrNull { it.id.equals(wallpaperAktif, ignoreCase = true) }?.nama ?: "Warna Solid"
                        }
                        else -> ChatWallpapers.cari(wallpaperAktif).nama
                    }
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(enabled = bolehUbahWallpaper) {
                            if (bolehUbahWallpaper) {
                                sheetPilihWallpaper = true
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(WarnaIos.Kartu)
                            .border(0.5.dp, PemisahInfo, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        WallpaperLatarChat(wallpaperId = wallpaperAktif, dimming = dimmingPribadi)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                namaWallpaper,
                                style = TipeIos.Isi,
                                modifier = Modifier.weight(1f, fill = false),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (!bolehUbahWallpaper) {
                                Spacer(Modifier.width(6.dp))
                                LencanaIos("Terkunci", NadaIos.NETRAL, ikon = IkonIos.Lock)
                            }
                        }
                        Text(
                            if (!bolehUbahWallpaper) "Hanya Developer yang dapat mengubah wallpaper chat"
                            else if (wallpaperPribadi != ChatWallpaperPrefs.ID_BAWAAN) "Kustom pribadi (hanya di HP ini)"
                            else "Wallpaper tim: $namaWallpaper",
                            style = TipeIos.Catatan,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (bolehUbahWallpaper) {
                        Text(
                            "Ganti",
                            style = TipeIos.Keterangan,
                            color = WarnaIos.LabelKedua,
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            IkonIos.ChevronRight,
                            contentDescription = null,
                            tint = WarnaIos.LabelKetiga,
                            modifier = Modifier.size(15.dp),
                        )
                    } else {
                        Icon(
                            IkonIos.Lock,
                            contentDescription = "Terkunci",
                            tint = WarnaIos.Abu,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            LabelBagian("Anggota")
            Kartu {
                KepalaAccordionAnggota(
                    jumlah = anggota.size,
                    memuat = memuatAnggota,
                    terbuka = anggotaTerbuka,
                    pratinjau = anggota,
                    onKlik = { anggotaTerbuka = !anggotaTerbuka },
                )
                AnimatedVisibility(
                    visible = anggotaTerbuka,
                    enter = fadeIn(tween(150)) + expandVertically(tween(220)),
                    exit = fadeOut(tween(120)) + shrinkVertically(tween(200)),
                ) {
                    Column {
                        when {
                            memuatAnggota && anggota.isEmpty() -> Row(
                                Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CircularProgressIndicator(
                                    color = BiruIosInfo,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(12.dp))
                                Text("Memuat anggota…", style = TipeIos.SubJudul)
                            }

                            anggota.isEmpty() -> Text(
                                "Daftar anggota belum tersedia.",
                                style = TipeIos.SubJudul,
                                modifier = Modifier.padding(16.dp),
                            )

                            // LazyColumn, BUKAN forEach di dalam Column.
                            //
                            // Lembar ini digulir dengan `verticalScroll`, yang
                            // menyusun seluruh anaknya sekaligus. Dengan daftar
                            // se-perusahaan, membuka accordion berarti menyusun
                            // ratusan baris DAN menembakkan ratusan permintaan
                            // foto dalam satu frame — persis jeda yang terasa.
                            // Dibatasi tingginya supaya punya batas terukur
                            // (LazyColumn menolak diukur dengan tinggi tak
                            // terhingga milik induk yang menggulir).
                            else -> LazyColumn(Modifier.heightIn(max = 340.dp)) {
                                items(
                                    count = anggota.size,
                                    key = { i -> anggota[i].id },
                                    contentType = { "anggota" },
                                ) { i ->
                                    val a = anggota[i]
                                    PemisahAnggota()
                                    BarisAnggota(
                                        anggota = a,
                                        akuSendiri = a.id == userId,
                                        onKlik = { onKlikAnggota(a) },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            LabelBagian("Tentang ruang ini")
            Kartu {
                BarisInfo(
                    ikon = Icons.Filled.Groups,
                    judul = "Pesan sementara (Reset 03:00 AM)",
                    isi = "Setiap pesan dan foto terhapus otomatis setiap pukul 03:00 AM WIB.",
                )
                awal.diubahOleh?.let {
                    Pemisah()
                    BarisInfo(ikon = null, judul = "Terakhir diubah", isi = it)
                }
            }

            galat?.let {
                Spacer(Modifier.height(14.dp))
                Text(it, style = TipeIos.Catatan, color = NadaIos.BAHAYA.teks, textAlign = TextAlign.Center)
            }

            if (bolehSunting) {
                Spacer(Modifier.height(22.dp))
                TombolUtamaIos(
                    teks = "Simpan",
                    onKlik = { onSimpan(nama, deskripsi, hanyaAdmin, fotoBaru, hapusFoto, wallpaper) },
                    aktif = berubah,
                    memuat = menyimpan,
                )
            }
        }
    }

    if (sheetPilihWallpaper && bolehUbahWallpaper) {
        PilihWallpaperSheet(
            wallpaperAwal = wallpaperAktif,
            dimmingAwal = dimmingPribadi,
            bisaTerapkanSemua = true,
            judul = "Wallpaper Chat Tim",
            onTerapkanLengkap = { baru, dimming, untukSemua ->
                if (bolehUbahWallpaper) {
                    if (untukSemua) {
                        wallpaper = baru
                        ChatWallpaperPrefs.resetWallpaperTim(konteks)
                        ChatWallpaperPrefs.setDimmingTim(konteks, dimming)
                        onGantiWallpaper(baru, dimming, true)
                    } else {
                        ChatWallpaperPrefs.setWallpaperTim(konteks, baru)
                        ChatWallpaperPrefs.setDimmingTim(konteks, dimming)
                        onGantiWallpaper(baru, dimming, false)
                    }
                    revisiWallpaperLokal++
                }
            },
            onResetBawaan = {
                if (bolehUbahWallpaper) {
                    wallpaper = "default"
                    ChatWallpaperPrefs.resetWallpaperTim(konteks)
                    ChatWallpaperPrefs.setDimmingTim(konteks, 0f)
                    onGantiWallpaper("default", 0f, true)
                    revisiWallpaperLokal++
                }
            },
            onTutup = { sheetPilihWallpaper = false },
        )
    }
}

@Composable
private fun FotoGrupBesar(
    pratinjauBaru: ImageBitmap?,
    pathSekarang: String?,
    nama: String,
    bolehSunting: Boolean,
    onGanti: () -> Unit,
) {
    Box(contentAlignment = Alignment.BottomEnd) {
        Box(
            Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(BiruIosInfo.copy(alpha = 0.14f))
                .then(if (bolehSunting) Modifier.clickable(onClick = onGanti) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            when {
                pratinjauBaru != null -> Image(
                    bitmap = pratinjauBaru,
                    contentDescription = "Foto grup baru",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(88.dp),
                )
                pathSekarang != null -> AsyncImage(
                    model = AvatarStorage.url(pathSekarang),
                    imageLoader = AvatarStorage.imageLoader(LocalContext.current),
                    contentDescription = "Foto grup $nama",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(88.dp),
                )
                else -> Icon(
                    Icons.Filled.Groups, null,
                    tint = BiruIosInfo, modifier = Modifier.size(44.dp),
                )
            }
        }
        if (bolehSunting) {
            Box(
                Modifier
                    .size(30.dp)
                    .bayanganIos(CircleShape, 4.dp)
                    .clip(CircleShape)
                    .background(BiruIosInfo)
                    .tekanIos(onGanti),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.PhotoCamera, "Ganti foto grup", tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
    }
}

/**
 * Kepala accordion anggota: ikon, jumlah, pratinjau wajah saat tertutup, dan
 * chevron yang berputar 90 derajat mengikuti keadaannya.
 *
 * Wajah hanya muncul saat TERTUTUP. Terbuka, daftarnya sendiri sudah
 * menampilkan tiap orang; membiarkan tumpukan itu tetap ada hanya membuat
 * kepalanya berebut perhatian dengan isinya.
 */
@Composable
private fun KepalaAccordionAnggota(
    jumlah: Int,
    memuat: Boolean,
    terbuka: Boolean,
    pratinjau: List<AnggotaGrup>,
    onKlik: () -> Unit,
) {
    val putaran by animateFloatAsState(
        targetValue = if (terbuka) 90f else 0f,
        animationSpec = tween(220),
        label = "putaranChevron",
    )
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onKlik)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(30.dp).clip(CircleShape).background(BiruIosInfo.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(IkonIos.Groups, null, tint = BiruIosInfo, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Anggota grup", style = TipeIos.Isi)
            Text(
                when {
                    memuat && jumlah == 0 -> "Memuat…"
                    jumlah == 0 -> "Belum tersedia"
                    else -> "$jumlah orang"
                },
                style = TipeIos.Catatan,
            )
        }
        AnimatedVisibility(
            visible = !terbuka && pratinjau.isNotEmpty(),
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(120)),
        ) {
            TumpukanWajahAnggota(pratinjau)
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            IkonIos.ChevronRight,
            if (terbuka) "Tutup daftar anggota" else "Buka daftar anggota",
            tint = WarnaIos.LabelKetiga,
            modifier = Modifier.size(15.dp).graphicsLayer { rotationZ = putaran },
        )
    }
}

/** Beberapa wajah pertama, saling menindih — ringkasan visual saat tertutup. */
@Composable
private fun TumpukanWajahAnggota(anggota: List<AnggotaGrup>) {
    val tampil = anggota.take(4)
    Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
        tampil.forEach { a ->
            Box(
                Modifier.size(26.dp).clip(CircleShape).background(Color.White).padding(1.dp),
            ) {
                AvatarStaf(
                    path = a.avatar,
                    nama = a.nama,
                    modifier = Modifier.size(24.dp),
                    ukuranHuruf = 10.sp,
                )
            }
        }
    }
}

/**
 * Satu baris anggota, bentuk daftar iOS: foto, nama, keterangan tipis di
 * bawahnya, dan chevron di ujung yang menandakan barisnya bisa dibuka.
 */
@Composable
private fun BarisAnggota(anggota: AnggotaGrup, akuSendiri: Boolean, onKlik: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onKlik)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val namaTampil = anggota.namaTampil
        AvatarStaf(
            path = anggota.avatar,
            nama = namaTampil,
            modifier = Modifier.size(38.dp),
            ukuranHuruf = 15.sp,
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    namaTampil,
                    style = TipeIos.Isi,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (akuSendiri) {
                    Spacer(Modifier.width(6.dp))
                    LencanaIos("Anda", NadaIos.AKSEN, titik = false)
                }
            }
            Text(
                listOfNotNull(labelRole(anggota.role), anggota.outlet?.takeIf { it.isNotBlank() })
                    .joinToString(" · "),
                style = TipeIos.Catatan,
                maxLines = 1,
            )
        }
        Icon(
            IkonIos.ChevronRight,
            null,
            tint = WarnaIos.LabelKetiga,
            modifier = Modifier.size(15.dp),
        )
    }
}

@Composable
private fun PemisahAnggota() {
    PemisahIos(inset = 66.dp)
}

@Composable
private fun LabelBagian(teks: String) {
    LabelSeksiIos(teks, Modifier.fillMaxWidth().padding(start = 16.dp, bottom = 7.dp))
}

@Composable
private fun Kartu(isi: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .permukaanIos(UkuranIos.SudutGrup),
    ) { isi() }
}

@Composable
private fun Pemisah() {
    PemisahIos(inset = 58.dp)
}

@Composable
private fun BarisInfo(
    ikon: androidx.compose.ui.graphics.vector.ImageVector?,
    judul: String,
    isi: String,
) {
    BarisIos(judul = judul, keterangan = isi, ikon = ikon)
}

@Composable
private fun IsianBaris(nilai: String, petunjuk: String, satuBaris: Boolean, onUbah: (String) -> Unit) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        if (nilai.isEmpty()) Text(petunjuk, style = TipeIos.Isi, color = WarnaIos.LabelKetiga)
        BasicTextField(
            value = nilai,
            onValueChange = onUbah,
            singleLine = satuBaris,
            maxLines = if (satuBaris) 1 else 3,
            textStyle = TipeIos.Isi,
            cursorBrush = androidx.compose.ui.graphics.SolidColor(BiruIosInfo),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
