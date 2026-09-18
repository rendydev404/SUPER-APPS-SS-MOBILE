package com.sukashawarma.superapp.feature.chat.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BiruPrimer = Color(0xFF0284C7)
private val EmasEnterprise = Color(0xFFD97706)
private val LatarEmasMuda = Color(0xFFFEF3C7)
private val GarisEmas = Color(0xFFFDE68A)
private val LatarAbu = Color(0xFFF8FAFC)
private val GarisBatas = Color(0xFFE2E8F0)
private val TeksGelap = Color(0xFF0F172A)
private val TeksPudar = Color(0xFF64748B)

/**
 * Layar antarmuka ketika ruang Chat Tim dalam status terkunci pada versi rilis.
 *
 * Menampilkan penawaran lisensi Enterprise Rp 50.000.000 dengan rincian fitur,
 * tombol checkout simulasi pembayaran lengkap, dan tombol kembali ke beranda.
 */
@Composable
fun LayarChatTerkunci(
    onBack: () -> Unit,
) {
    var sheetPembayaranTerbuka by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // Bar Atas
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFFF8FAFC),
            border = BorderStroke(0.5.dp, GarisBatas),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                        contentDescription = "Kembali ke Beranda",
                        tint = BiruPrimer,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(4.dp))
                Column {
                    Text(
                        text = "Chat Tim",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TeksGelap,
                    )
                    Text(
                        text = "Paket Enterprise Terkunci",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = EmasEnterprise,
                    )
                }
            }
        }

        // Konten Utama Paywall
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Kartu Utama Enterprise
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                border = BorderStroke(1.2.dp, GarisEmas),
                shadowElevation = 3.dp,
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Badge Ikon Gembok Emas
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0xFFFEF3C7), Color(0xFFFDE68A)),
                                ),
                                CircleShape,
                            )
                            .border(2.dp, Color(0xFFF59E0B), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Terkunci",
                            tint = Color(0xFFB45309),
                            modifier = Modifier.size(34.dp),
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = LatarEmasMuda,
                        border = BorderStroke(0.8.dp, GarisEmas),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                tint = Color(0xFF92400E),
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Lisensi Enterprise Diperlukan",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E),
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = "Chat Tim Terkunci",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TeksGelap,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(Modifier.height(18.dp))

                    // Kotak Keunggulan Fitur
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = LatarAbu,
                        border = BorderStroke(0.8.dp, GarisBatas),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            FiturPoin("Realtime Chat & Centang Dua Biru ala WhatsApp")
                            FiturPoin("Message Info (Lihat waktu dibaca per anggota tim)")
                            FiturPoin("Kirim Foto HD, Nota, & Lampiran Inventaris")
                            FiturPoin("Koordinasi Multi-Cabang & Jalur Server Terenkripsi")
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    // Bagian Harga Lisensi
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "BIAYA LISENSI SEUMUR HIDUP",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TeksPudar,
                            letterSpacing = 1.sp,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Rp 50.000.000",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0F172A),
                        )
                        Text(
                            text = "Sekali Bayar · Akses Selamanya Seluruh Kru",
                            fontSize = 11.5.sp,
                            color = Color(0xFF059669),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    // Tombol Aksi Beli Lisensi
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { sheetPembayaranTerbuka = true },
                        color = BiruPrimer,
                        shape = RoundedCornerShape(14.dp),
                        shadowElevation = 2.dp,
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Beli Lisensi & Buka Kunci (Rp 50 Jt)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Tombol Kembali ke Beranda
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, GarisBatas),
                    ) {
                        Text(
                            text = "Kembali ke Beranda",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TeksGelap,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Catatan Notifikasi
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFEFF6FF),
                border = BorderStroke(0.8.dp, Color(0xFFBFDBFE)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = BiruPrimer,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Notifikasi pesan baru dari tim tetap akan masuk dan berbunyi di perangkat Anda.",
                        fontSize = 12.sp,
                        color = Color(0xFF1E40AF),
                        lineHeight = 17.sp,
                    )
                }
            }
        }
    }

    // Lembar Simulasi Pembayaran
    if (sheetPembayaranTerbuka) {
        SheetSimulasiPembayaran(
            onDismiss = { sheetPembayaranTerbuka = false },
        )
    }
}

@Composable
private fun FiturPoin(teks: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(Color(0xFFDCFCE7), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color(0xFF16A34A),
                modifier = Modifier.size(13.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = teks,
            fontSize = 12.5.sp,
            color = TeksGelap,
            fontWeight = FontWeight.Medium,
        )
    }
}
