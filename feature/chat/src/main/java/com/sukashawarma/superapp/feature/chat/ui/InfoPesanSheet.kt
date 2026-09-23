package com.sukashawarma.superapp.feature.chat.ui

import com.sukashawarma.superapp.core.ui.ios.LabelSeksiIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.TombolBundarIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import com.sukashawarma.superapp.core.ui.kaca.IkonIos
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sukashawarma.superapp.core.ui.AvatarStaf
import com.sukashawarma.superapp.core.ui.AvatarStorage
import com.sukashawarma.superapp.feature.chat.data.ChatRepository
import com.sukashawarma.superapp.feature.chat.data.DetailInfoPesan
import com.sukashawarma.superapp.feature.chat.data.PembacaPesan
import com.sukashawarma.superapp.feature.chat.data.PesanChat
import com.sukashawarma.superapp.feature.chat.data.labelRole
import com.sukashawarma.superapp.feature.chat.ui.suara.BubbleSuara
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val BiruWaCentang = WarnaIos.Biru
private val AbuCentang = WarnaIos.Abu
private val LatarIosGrouped = WarnaIos.Latar
private val GarisPemisahIos = WarnaIos.Pemisah
private val SudutGrupInfo = UkuranIos.SudutGrup
private val SudutGrupInfoAtas = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
private val SudutGrupInfoBawah = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)

private val ZONA_WIB = ZoneId.of("Asia/Jakarta")
private val FORMAT_JAM = DateTimeFormatter.ofPattern("HH:mm", Locale("id", "ID"))
private val FORMAT_TANGGAL_JAM = DateTimeFormatter.ofPattern("d/M/yy, HH:mm", Locale("id", "ID"))

fun formatJamSaja(waktuMs: Long?): String {
    if (waktuMs == null || waktuMs <= 0L) return ""
    return Instant.ofEpochMilli(waktuMs).atZone(ZONA_WIB).format(FORMAT_JAM)
}

fun formatWaktuBaca(waktuMs: Long?): String {
    if (waktuMs == null || waktuMs <= 0L) return "—"
    val zdt = Instant.ofEpochMilli(waktuMs).atZone(ZONA_WIB)
    val hariIni = LocalDate.now(ZONA_WIB)
    val hariWaktu = zdt.toLocalDate()
    val jam = zdt.format(FORMAT_JAM)
    return when (hariWaktu) {
        hariIni -> "Hari ini, $jam"
        hariIni.minusDays(1) -> "Kemarin, $jam"
        else -> zdt.format(FORMAT_TANGGAL_JAM)
    }
}

/**
 * Lembar Info Pesan bergaya murni WhatsApp iOS (Inset Grouped Style):
 * - Bersih, rapi, dan tidak ramai
 * - Bubble pesan abu terang, sama dengan gelembung sendiri di layar chat
 * - Dua seksi grouped kartu putih: "DIBACA OLEH" dan "TERSAMPAIKAN KE"
 * - Baris pembaca ala iOS TableView Cell dengan inset divider
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoPesanSheet(
    pesan: PesanChat,
    detail: DetailInfoPesan?,
    memuat: Boolean,
    onTutup: () -> Unit,
    /** Khusus pesan suara: siapa saja yang sudah benar-benar mendengarkannya.
     *  Dibaca sudah ≠ didengar — itu sebabnya daftarnya berdiri sendiri. */
    pendengarSuara: List<PembacaPesan> = emptyList(),
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val daftarDibaca = detail?.dibaca.orEmpty()
    val daftarBelum = detail?.belumDibaca.orEmpty()
    val semuaSudahBaca = detail != null && daftarBelum.isEmpty() && daftarDibaca.isNotEmpty()

    ModalBottomSheet(
        onDismissRequest = onTutup,
        sheetState = sheetState,
        containerColor = LatarIosGrouped,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = null,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            // Drag Indicator khas iOS
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(width = 36.dp, height = 5.dp)
                        .clip(CircleShape)
                        .background(WarnaIos.LabelKetiga)
                )
            }

            // Header Bar Simpel & Elegan ala iOS
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
            ) {
                Text(
                    "Info Pesan",
                    style = TipeIos.Utama,
                    modifier = Modifier.align(Alignment.Center),
                )
                TombolBundarIos(
                    IkonIos.Close,
                    "Tutup",
                    onTutup,
                    Modifier.align(Alignment.CenterEnd),
                    warnaIkon = WarnaIos.LabelKedua,
                )
            }

            if (memuat && detail == null) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(26.dp),
                        strokeWidth = 2.5.dp,
                        color = BiruWaCentang,
                    )
                }
            } else {
                LazyColumn(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp, max = 560.dp),
                ) {
                    // 1. Bubble Pesan Asli WhatsApp iOS
                    item(key = "bubble_pesan") {
                        GelembungPesanWaIos(
                            pesan = pesan,
                            semuaSudahBaca = semuaSudahBaca,
                            sudahDidengar = pendengarSuara.isNotEmpty(),
                        )
                    }

                    // 2a. Seksi DIDENGARKAN OLEH — hanya untuk pesan suara.
                    if (pesan.audioPath != null) {
                        item(key = "header_dengar") {
                            LabelSeksiIos(
                                "DIDENGARKAN OLEH (${pendengarSuara.size})",
                                Modifier.padding(start = 32.dp, top = 14.dp, bottom = 7.dp),
                            )
                        }

                        if (pendengarSuara.isEmpty()) {
                            item(key = "empty_dengar") {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .clip(SudutGrupInfo)
                                        .background(WarnaIos.Kartu)
                                        .padding(vertical = 14.dp, horizontal = 16.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        "Belum ada yang memutar pesan suara ini",
                                        style = TipeIos.SubJudul,
                                    )
                                }
                            }
                        } else {
                            itemsIndexed(pendengarSuara, key = { _, it -> "dengar_" + it.userId }) { index, orang ->
                                val bentuk = when {
                                    pendengarSuara.size == 1 -> SudutGrupInfo
                                    index == 0 -> SudutGrupInfoAtas
                                    index == pendengarSuara.size - 1 -> SudutGrupInfoBawah
                                    else -> RoundedCornerShape(0.dp)
                                }
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .clip(bentuk)
                                        .background(WarnaIos.Kartu),
                                ) {
                                    BarisItemPembacaIos(
                                        pembaca = orang,
                                        waktuBaca = formatWaktuBaca(orang.readAtMs),
                                        sudahDibaca = true,
                                        semuaSudahBaca = semuaSudahBaca,
                                    )
                                    if (index < pendengarSuara.size - 1) {
                                        HorizontalDivider(
                                            thickness = 0.5.dp,
                                            color = GarisPemisahIos,
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(start = 68.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2. Seksi DIBACA OLEH
                    item(key = "header_dibaca") {
                        LabelSeksiIos(
                            "DIBACA OLEH (${daftarDibaca.size})",
                            Modifier.padding(start = 32.dp, top = 14.dp, bottom = 7.dp),
                        )
                    }

                    if (daftarDibaca.isEmpty()) {
                        item(key = "empty_dibaca") {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .clip(SudutGrupInfo)
                                    .background(WarnaIos.Kartu)
                                    .padding(vertical = 14.dp, horizontal = 16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "Belum ada yang membaca pesan ini",
                                    style = TipeIos.SubJudul,
                                )
                            }
                        }
                    } else {
                        itemsIndexed(daftarDibaca, key = { _, it -> "baca_" + it.userId }) { index, pembaca ->
                            val shape = when {
                                daftarDibaca.size == 1 -> SudutGrupInfo
                                index == 0 -> SudutGrupInfoAtas
                                index == daftarDibaca.size - 1 -> SudutGrupInfoBawah
                                else -> RoundedCornerShape(0.dp)
                            }
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .clip(shape)
                                    .background(WarnaIos.Kartu),
                            ) {
                                BarisItemPembacaIos(
                                    pembaca = pembaca,
                                    waktuBaca = formatWaktuBaca(pembaca.readAtMs),
                                    sudahDibaca = true,
                                    semuaSudahBaca = semuaSudahBaca,
                                )
                                if (index < daftarDibaca.size - 1) {
                                    HorizontalDivider(
                                        thickness = 0.5.dp,
                                        color = GarisPemisahIos,
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(start = 68.dp),
                                    )
                                }
                            }
                        }
                    }

                    // 3. Seksi TERSAMPAIKAN KE
                    item(key = "header_belum") {
                        LabelSeksiIos(
                            "TERSAMPAIKAN KE (${daftarBelum.size})",
                            Modifier.padding(start = 32.dp, top = 20.dp, bottom = 7.dp),
                        )
                    }

                    if (daftarBelum.isEmpty()) {
                        item(key = "empty_belum") {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .clip(SudutGrupInfo)
                                    .background(WarnaIos.Kartu)
                                    .padding(vertical = 14.dp, horizontal = 16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "Semua anggota telah membaca pesan ini",
                                    style = TipeIos.SubJudul,
                                )
                            }
                        }
                    } else {
                        itemsIndexed(daftarBelum, key = { _, it -> "belum_" + it.userId }) { index, pembaca ->
                            val shape = when {
                                daftarBelum.size == 1 -> SudutGrupInfo
                                index == 0 -> SudutGrupInfoAtas
                                index == daftarBelum.size - 1 -> SudutGrupInfoBawah
                                else -> RoundedCornerShape(0.dp)
                            }
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .clip(shape)
                                    .background(WarnaIos.Kartu),
                            ) {
                                BarisItemPembacaIos(
                                    pembaca = pembaca,
                                    waktuBaca = null,
                                    sudahDibaca = false,
                                )
                                if (index < daftarBelum.size - 1) {
                                    HorizontalDivider(
                                        thickness = 0.5.dp,
                                        color = GarisPemisahIos,
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(start = 68.dp),
                                    )
                                }
                            }
                        }
                    }

                    item(key = "bottom_space") {
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

/** Bubble Pesan Asli khas WhatsApp iOS */
@Composable
private fun GelembungPesanWaIos(
    pesan: PesanChat,
    semuaSudahBaca: Boolean,
    sudahDidengar: Boolean = false,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Column(
            Modifier
                .widthIn(min = 120.dp, max = 320.dp)
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomStart = 14.dp, bottomEnd = 2.dp))
                // Abu terang, sama dengan gelembung sendiri di layar chat —
                // lembar ini memperlihatkan pesan yang sama, jadi warnanya pun
                // harus sama.
                .background(Color(0xFFF4F4F7))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            // Kutipan balasan jika ada
            if (pesan.replyToName != null) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x12000000))
                        .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                ) {
                    Box(
                        Modifier
                            .width(3.dp)
                            .height(28.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(BiruWaCentang),
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            pesan.replyToName,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BiruWaCentang,
                        )
                        Text(
                            pesan.replyToSnippet.orEmpty(),
                            fontSize = 11.sp,
                            color = Color(0xFF666666),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            // Pesan suara jika ada
            if (pesan.audioPath != null) {
                BubbleSuara(
                    audioPath = pesan.audioPath,
                    audioMs = pesan.audioMs,
                    audioWave = pesan.audioWave,
                    milikSendiri = true,
                    sudahDiputar = sudahDidengar,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }

            // Foto jika ada
            val urlFoto = ChatRepository.urlFoto(pesan.imagePath)
            if (urlFoto != null) {
                val konteks = LocalContext.current
                AsyncImage(
                    model = urlFoto,
                    imageLoader = AvatarStorage.imageLoader(konteks),
                    contentDescription = "Foto pesan",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 160.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )
                Spacer(Modifier.height(4.dp))
            }

            // Teks pesan
            if (pesan.body.isNotBlank()) {
                Text(
                    pesan.body,
                    fontSize = 15.sp,
                    color = Color.Black,
                    lineHeight = 20.sp,
                )
                Spacer(Modifier.height(4.dp))
            }

            // Jam & Centang di pojok kanan bawah
            Row(
                Modifier.align(Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (pesan.editedAtMs != null) {
                    Text(
                        "diedit",
                        fontSize = 11.sp,
                        fontStyle = FontStyle.Italic,
                        color = Color(0xFF758696),
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    formatJamSaja(pesan.createdAtMs),
                    fontSize = 11.5.sp,
                    color = Color(0xFF758696),
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.Filled.DoneAll,
                    contentDescription = if (semuaSudahBaca) "Dibaca oleh semua" else "Tersampaikan",
                    tint = if (semuaSudahBaca) BiruWaCentang else AbuCentang,
                    modifier = Modifier.size(15.dp),
                )
            }
        }
    }
}

/** Baris Item Pembaca khas iOS TableView Cell */
@Composable
private fun BarisItemPembacaIos(
    pembaca: PembacaPesan,
    waktuBaca: String?,
    sudahDibaca: Boolean,
    semuaSudahBaca: Boolean = false,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvatarStaf(
            path = pembaca.avatarUrl,
            nama = pembaca.nama,
            modifier = Modifier.size(40.dp),
            ukuranHuruf = 15.sp,
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                pembaca.namaTampil,
                style = TipeIos.Isi,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val subLabel = listOfNotNull(
                pembaca.role?.let(::labelRole),
                pembaca.outletNama?.takeIf { it.isNotBlank() },
            ).joinToString(" • ")

            if (subLabel.isNotBlank()) {
                Text(
                    subLabel,
                    style = TipeIos.Catatan,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        if (sudahDibaca) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.DoneAll,
                    contentDescription = "Sudah dibaca",
                    tint = if (semuaSudahBaca) BiruWaCentang else AbuCentang,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    waktuBaca.orEmpty(),
                    fontSize = 13.sp,
                    color = AbuCentang,
                )
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.DoneAll,
                    contentDescription = "Tersampaikan",
                    tint = AbuCentang,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "—",
                    fontSize = 13.sp,
                    color = AbuCentang,
                )
            }
        }
    }
}
