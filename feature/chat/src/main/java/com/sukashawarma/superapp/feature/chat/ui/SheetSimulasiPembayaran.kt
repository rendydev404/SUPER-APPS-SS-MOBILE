package com.sukashawarma.superapp.feature.chat.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val BiruPrimer = Color(0xFF0284C7)
private val EmasEnterprise = Color(0xFFD97706)
private val LatarAbu = Color(0xFFF8FAFC)
private val GarisBatas = Color(0xFFE2E8F0)
private val TeksGelap = Color(0xFF0F172A)
private val TeksPudar = Color(0xFF64748B)

private enum class SaluranPembayaran(val label: String) {
    VIRTUAL_ACCOUNT("Virtual Account"),
    QRIS("QRIS"),
    KARTU_KREDIT("Kartu Kredit/Debit"),
}

private data class OpsiBank(
    val kode: String,
    val nama: String,
    val nomorVa: String,
    val warnaAksen: Color,
)

private val DAFTAR_BANK = listOf(
    OpsiBank("BCA", "BCA Virtual Account", "70012 0812 8829 4019", Color(0xFF005BAA)),
    OpsiBank("MANDIRI", "Mandiri Virtual Account", "88908 0812 8829 4019", Color(0xFF003D79)),
    OpsiBank("BRI", "BRI BRIVA", "12800 0812 8829 4019", Color(0xFF00529C)),
    OpsiBank("BNI", "BNI Virtual Account", "98801 0812 8829 4019", Color(0xFFF15A24)),
    OpsiBank("BSI", "BSI Virtual Account", "71100 0812 8829 4019", Color(0xFF00A39D)),
)

/**
 * Modal lembar pembayaran tagihan lisensi Chat Tim Rp 50.000.000.
 *
 * Desain dibuat setara Payment Gateway resmi (Midtrans/Xendit/DOKU) dengan
 * rincian invoice, countdown timer, dan seluruh kanal pembayaran.
 * Pengecekan pembayaran selalu mengembalikan penolakan bahwa mutasi belum
 * masuk, sehingga fitur chat tetap terkunci aman di versi rilis.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetSimulasiPembayaran(
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var tabAktif by remember { mutableStateOf(SaluranPembayaran.VIRTUAL_ACCOUNT) }
    var bankTerpilih by remember { mutableStateOf(DAFTAR_BANK.first()) }
    var nomorKartu by remember { mutableStateOf("") }
    var masaBerlaku by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var namaKartu by remember { mutableStateOf("") }

    var sedangMemverifikasi by remember { mutableStateOf(false) }
    var dialogGalatTerbuka by remember { mutableStateOf(false) }
    var petunjukTerbuka by remember { mutableStateOf(false) }

    // Hitung mundur waktu pembayaran simulasi (23 jam 59 menit)
    var detikTersisa by remember { mutableIntStateOf(86395) }
    LaunchedEffect(Unit) {
        while (detikTersisa > 0) {
            delay(1000)
            detikTersisa--
        }
    }
    val formatTimer = remember(detikTersisa) {
        val jam = detikTersisa / 3600
        val menit = (detikTersisa % 3600) / 60
        val detik = detikTersisa % 60
        String.format("%02d:%02d:%02d", jam, menit, detik)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = {
            Surface(
                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                color = Color(0xFFCBD5E1),
                shape = RoundedCornerShape(2.dp),
            ) {
                Spacer(Modifier.size(width = 38.dp, height = 4.dp))
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            // Header Payment Gateway
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFF0F172A), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = EmasEnterprise,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "SUKA PAYMENT GATEWAY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TeksPudar,
                            letterSpacing = 1.sp,
                        )
                        Text(
                            text = "Secure Checkout 256-bit SSL",
                            fontSize = 10.sp,
                            color = Color(0xFF059669),
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFEF3C7),
                    border = BorderStroke(0.8.dp, Color(0xFFFDE68A)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = Color(0xFF92400E),
                            modifier = Modifier.size(12.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = formatTimer,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E),
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Kartu Ringkasan Tagihan
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = LatarAbu,
                border = BorderStroke(1.dp, GarisBatas),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "No. Invoice",
                            fontSize = 12.sp,
                            color = TeksPudar,
                        )
                        Text(
                            text = "INV/SS/2026/ENT-89218",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TeksGelap,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Item Pembelian",
                            fontSize = 12.sp,
                            color = TeksPudar,
                        )
                        Text(
                            text = "Lisensi Chat Tim Enterprise Lifetime",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TeksGelap,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.8.dp)
                            .background(GarisBatas),
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Total Pembayaran",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TeksGelap,
                        )
                        Text(
                            text = "Rp 50.000.000",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BiruPrimer,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Tab Pilihan Metode Pembayaran
            Text(
                text = "PILIH METODE PEMBAYARAN",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TeksPudar,
                letterSpacing = 0.8.sp,
            )
            Spacer(Modifier.height(8.dp))

            TabRow(
                selectedTabIndex = tabAktif.ordinal,
                containerColor = Color.White,
                contentColor = BiruPrimer,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[tabAktif.ordinal]),
                        color = BiruPrimer,
                        height = 2.5.dp,
                    )
                },
            ) {
                SaluranPembayaran.values().forEach { tab ->
                    Tab(
                        selected = tabAktif == tab,
                        onClick = { tabAktif = tab },
                        text = {
                            Text(
                                text = tab.label,
                                fontSize = 12.sp,
                                fontWeight = if (tabAktif == tab) FontWeight.Bold else FontWeight.Normal,
                                color = if (tabAktif == tab) BiruPrimer else TeksPudar,
                            )
                        },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Konten Masing-masing Tab
            when (tabAktif) {
                SaluranPembayaran.VIRTUAL_ACCOUNT -> {
                    KontenVirtualAccount(
                        bankTerpilih = bankTerpilih,
                        onPilihBank = { bankTerpilih = it },
                        onSalinVa = {
                            clipboardManager.setText(AnnotatedString(bankTerpilih.nomorVa.replace(" ", "")))
                            Toast.makeText(context, "Nomor Virtual Account disalin", Toast.LENGTH_SHORT).show()
                        },
                        onSalinJumlah = {
                            clipboardManager.setText(AnnotatedString("50000000"))
                            Toast.makeText(context, "Nominal Rp 50.000.000 disalin", Toast.LENGTH_SHORT).show()
                        },
                        petunjukTerbuka = petunjukTerbuka,
                        onTogglePetunjuk = { petunjukTerbuka = !petunjukTerbuka },
                    )
                }
                SaluranPembayaran.QRIS -> {
                    KontenQris(
                        onSalinJumlah = {
                            clipboardManager.setText(AnnotatedString("50000000"))
                            Toast.makeText(context, "Nominal Rp 50.000.000 disalin", Toast.LENGTH_SHORT).show()
                        },
                    )
                }
                SaluranPembayaran.KARTU_KREDIT -> {
                    KontenKartuKredit(
                        nomorKartu = nomorKartu,
                        onNomorKartuUbah = { if (it.length <= 19) nomorKartu = it },
                        masaBerlaku = masaBerlaku,
                        onMasaBerlakuUbah = { if (it.length <= 5) masaBerlaku = it },
                        cvv = cvv,
                        onCvvUbah = { if (it.length <= 3) cvv = it },
                        namaKartu = namaKartu,
                        onNamaKartuUbah = { namaKartu = it },
                    )
                }
            }

            Spacer(Modifier.height(22.dp))

            // Tombol Utama "Cek Status Pembayaran" / "Saya Sudah Bayar"
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(enabled = !sedangMemverifikasi) {
                        sedangMemverifikasi = true
                        scope.launch {
                            delay(2000)
                            sedangMemverifikasi = false
                            dialogGalatTerbuka = true
                        }
                    },
                color = BiruPrimer,
                shape = RoundedCornerShape(14.dp),
                shadowElevation = 2.dp,
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    if (sedangMemverifikasi) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.5.dp,
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "Menghubungkan ke Bank…",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                            )
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Saya Sudah Bayar (Cek Status)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Tombol Tutup
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Batal & Kembali ke Layar Terkunci",
                    fontSize = 13.sp,
                    color = TeksPudar,
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(Modifier.height(18.dp))
        }
    }

    // Dialog Hasil Verifikasi Pembayaran (Selalu Belum Terdeteksi)
    if (dialogGalatTerbuka) {
        AlertDialog(
            onDismissRequest = { dialogGalatTerbuka = false },
            icon = {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(Color(0xFFFEF2F2), CircleShape)
                        .border(1.5.dp, Color(0xFFFCA5A5), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(30.dp),
                    )
                }
            },
            title = {
                Text(
                    text = "Pembayaran Belum Diterima",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TeksGelap,
                    textAlign = TextAlign.Center,
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Sistem kliring perbankan otomatis belum mendeteksi mutasi dana masuk sebesar Rp 50.000.000 untuk invoice #INV/SS/2026/ENT-89218.",
                        fontSize = 13.sp,
                        color = Color(0xFF475569),
                        textAlign = TextAlign.Center,
                        lineHeight = 19.sp,
                    )
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = LatarAbu,
                        border = BorderStroke(0.8.dp, GarisBatas),
                    ) {
                        Column(Modifier.padding(10.dp)) {
                            Text(
                                text = "• Mohon pastikan transfer nominal pas Rp 50.000.000",
                                fontSize = 11.5.sp,
                                color = TeksPudar,
                            )
                            Text(
                                text = "• Transfer antar-bank membutuhkan waktu 5-15 menit",
                                fontSize = 11.5.sp,
                                color = TeksPudar,
                            )
                            Text(
                                text = "• Hubungi Finance / Owner bila dana sudah terpotong",
                                fontSize = 11.5.sp,
                                color = TeksPudar,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { dialogGalatTerbuka = false },
                ) {
                    Text(
                        text = "Mengerti",
                        fontWeight = FontWeight.Bold,
                        color = BiruPrimer,
                    )
                }
            },
        )
    }
}

@Composable
private fun KontenVirtualAccount(
    bankTerpilih: OpsiBank,
    onPilihBank: (OpsiBank) -> Unit,
    onSalinVa: () -> Unit,
    onSalinJumlah: () -> Unit,
    petunjukTerbuka: Boolean,
    onTogglePetunjuk: () -> Unit,
) {
    Column {
        Text(
            text = "Pilih Bank Tujuan:",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = TeksPudar,
        )
        Spacer(Modifier.height(8.dp))

        // Pilihan Bank
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DAFTAR_BANK.take(3).forEach { bank ->
                val terpilih = bank.kode == bankTerpilih.kode
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onPilihBank(bank) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (terpilih) bank.warnaAksen.copy(alpha = 0.08f) else Color.White,
                    border = BorderStroke(
                        if (terpilih) 1.5.dp else 1.dp,
                        if (terpilih) bank.warnaAksen else GarisBatas,
                    ),
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = bank.kode,
                            fontSize = 13.sp,
                            fontWeight = if (terpilih) FontWeight.Bold else FontWeight.Medium,
                            color = if (terpilih) bank.warnaAksen else TeksGelap,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DAFTAR_BANK.drop(3).forEach { bank ->
                val terpilih = bank.kode == bankTerpilih.kode
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onPilihBank(bank) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (terpilih) bank.warnaAksen.copy(alpha = 0.08f) else Color.White,
                    border = BorderStroke(
                        if (terpilih) 1.5.dp else 1.dp,
                        if (terpilih) bank.warnaAksen else GarisBatas,
                    ),
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = bank.kode,
                            fontSize = 13.sp,
                            fontWeight = if (terpilih) FontWeight.Bold else FontWeight.Medium,
                            color = if (terpilih) bank.warnaAksen else TeksGelap,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Nomor Virtual Account Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = Color.White,
            border = BorderStroke(1.dp, GarisBatas),
        ) {
            Column(Modifier.padding(14.dp)) {
                Text(
                    text = "Nomor Virtual Account",
                    fontSize = 11.sp,
                    color = TeksPudar,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = bankTerpilih.nomorVa,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TeksGelap,
                        fontFamily = FontFamily.Monospace,
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier.clickable { onSalinVa() },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Salin",
                                tint = BiruPrimer,
                                modifier = Modifier.size(13.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Salin",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BiruPrimer,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.6.dp)
                        .background(GarisBatas),
                )
                Spacer(Modifier.height(10.dp))

                Text(
                    text = "Nama Akun",
                    fontSize = 11.sp,
                    color = TeksPudar,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "PT SUKA KULINER NUSANTARA - CHAT",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TeksGelap,
                )

                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.6.dp)
                        .background(GarisBatas),
                )
                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "Nominal Transfer",
                            fontSize = 11.sp,
                            color = TeksPudar,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = "Rp 50.000.000",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = BiruPrimer,
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier.clickable { onSalinJumlah() },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Salin Nominal",
                                tint = BiruPrimer,
                                modifier = Modifier.size(13.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Salin Nominal",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BiruPrimer,
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Petunjuk Pembayaran Expandable
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { onTogglePetunjuk() },
            shape = RoundedCornerShape(12.dp),
            color = LatarAbu,
            border = BorderStroke(0.8.dp, GarisBatas),
        ) {
            Column(Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Petunjuk Transfer ${bankTerpilih.nama}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TeksGelap,
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = TeksPudar,
                        modifier = Modifier.size(12.dp),
                    )
                }

                AnimatedVisibility(visible = petunjukTerbuka) {
                    Column(Modifier.padding(top = 10.dp)) {
                        Text(
                            text = "1. Buka aplikasi M-Banking atau ATM ${bankTerpilih.kode}\n" +
                                "2. Pilih menu Transfer > Virtual Account\n" +
                                "3. Masukkan nomor VA: ${bankTerpilih.nomorVa.replace(" ", "")}\n" +
                                "4. Pastikan tagihan muncul Rp 50.000.000 a.n. PT SUKA KULINER NUSANTARA\n" +
                                "5. Konfirmasi pembayaran dan simpan bukti transaksi.",
                            fontSize = 11.5.sp,
                            color = Color(0xFF475569),
                            lineHeight = 18.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KontenQris(
    onSalinJumlah: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(1.dp, GarisBatas),
            shadowElevation = 1.dp,
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Header QRIS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "QRIS",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFDC2626),
                        letterSpacing = 1.5.sp,
                    )
                    Text(
                        text = "NMID: ID1020039281729",
                        fontSize = 10.sp,
                        color = TeksPudar,
                        fontFamily = FontFamily.Monospace,
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Kode QR Mock Realistis
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    QrisCanvasPattern()
                    // Logo tengah QRIS
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color.White, RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode2,
                            contentDescription = null,
                            tint = BiruPrimer,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "PT SUKA KULINER NUSANTARA",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TeksGelap,
                )
                Text(
                    text = "Mendukung BCA, Mandiri, GoPay, OVO, DANA, ShopeePay",
                    fontSize = 10.5.sp,
                    color = TeksPudar,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.clickable { onSalinJumlah() },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Salin Nominal",
                            tint = BiruPrimer,
                            modifier = Modifier.size(13.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Salin Nominal Rp 50.000.000",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = BiruPrimer,
                        )
                    }
                }
            }
        }
    }
}

/** Pola QR Canvas sederhana namun presisi untuk ilusi QRIS yang meyakinkan. */
@Composable
private fun QrisCanvasPattern() {
    Canvas(modifier = Modifier.size(170.dp)) {
        val step = size.width / 21f
        // Sudut QR kiri atas
        drawRect(Color.Black, Offset(0f, 0f), Size(step * 7, step * 7))
        drawRect(Color.White, Offset(step, step), Size(step * 5, step * 5))
        drawRect(Color.Black, Offset(step * 2, step * 2), Size(step * 3, step * 3))

        // Sudut QR kanan atas
        drawRect(Color.Black, Offset(size.width - step * 7, 0f), Size(step * 7, step * 7))
        drawRect(Color.White, Offset(size.width - step * 6, step), Size(step * 5, step * 5))
        drawRect(Color.Black, Offset(size.width - step * 5, step * 2), Size(step * 3, step * 3))

        // Sudut QR kiri bawah
        drawRect(Color.Black, Offset(0f, size.height - step * 7), Size(step * 7, step * 7))
        drawRect(Color.White, Offset(step, size.height - step * 6), Size(step * 5, step * 5))
        drawRect(Color.Black, Offset(step * 2, size.height - step * 5), Size(step * 3, step * 3))

        // Pola data acak di tengah
        val seed = 42
        for (x in 8..12) {
            for (y in 0..6) {
                if ((x * 7 + y * 13 + seed) % 3 == 0) {
                    drawRect(Color.Black, Offset(x * step, y * step), Size(step, step))
                }
            }
        }
        for (x in 0..20) {
            for (y in 8..20) {
                if (x < 7 && y > 13) continue
                if ((x * 11 + y * 17 + seed) % 2 == 0) {
                    drawRect(Color.Black, Offset(x * step, y * step), Size(step, step))
                }
            }
        }
    }
}

@Composable
private fun KontenKartuKredit(
    nomorKartu: String,
    onNomorKartuUbah: (String) -> Unit,
    masaBerlaku: String,
    onMasaBerlakuUbah: (String) -> Unit,
    cvv: String,
    onCvvUbah: (String) -> Unit,
    namaKartu: String,
    onNamaKartuUbah: (String) -> Unit,
) {
    Column {
        OutlinedTextField(
            value = nomorKartu,
            onValueChange = onNomorKartuUbah,
            label = { Text("Nomor Kartu (16 Digit)") },
            placeholder = { Text("4111 2222 3333 4444") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.CreditCard,
                    contentDescription = null,
                    tint = BiruPrimer,
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BiruPrimer,
                unfocusedBorderColor = GarisBatas,
            ),
            singleLine = true,
        )

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(
                value = masaBerlaku,
                onValueChange = onMasaBerlakuUbah,
                label = { Text("Masa Berlaku") },
                placeholder = { Text("MM/YY") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BiruPrimer,
                    unfocusedBorderColor = GarisBatas,
                ),
                singleLine = true,
            )

            OutlinedTextField(
                value = cvv,
                onValueChange = onCvvUbah,
                label = { Text("CVV") },
                placeholder = { Text("123") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = TeksPudar,
                        modifier = Modifier.size(16.dp),
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BiruPrimer,
                    unfocusedBorderColor = GarisBatas,
                ),
                singleLine = true,
            )
        }

        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = namaKartu,
            onValueChange = onNamaKartuUbah,
            label = { Text("Nama Pemegang Kartu") },
            placeholder = { Text("SESUAI KARTU") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BiruPrimer,
                unfocusedBorderColor = GarisBatas,
            ),
            singleLine = true,
        )
    }
}
