package com.sukashawarma.superapp.feature.chat.ui

import androidx.activity.compose.BackHandler
import com.sukashawarma.superapp.core.ui.ios.BilahJudulIos
import com.sukashawarma.superapp.core.ui.ios.KartuIos
import com.sukashawarma.superapp.core.ui.ios.LabelSeksiIos
import com.sukashawarma.superapp.core.ui.ios.LencanaIos
import com.sukashawarma.superapp.core.ui.ios.NadaIos
import com.sukashawarma.superapp.core.ui.ios.TipeIos
import com.sukashawarma.superapp.core.ui.ios.TombolKeduaIos
import com.sukashawarma.superapp.core.ui.ios.TombolUtamaIos
import com.sukashawarma.superapp.core.ui.ios.UkuranIos
import com.sukashawarma.superapp.core.ui.ios.WarnaIos
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
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
            .background(WarnaIos.Latar)
            .navigationBarsPadding(),
    ) {
        // Bar Atas
        BilahJudulIos(
            judul = "Chat Tim",
            subjudul = "Paket Enterprise Terkunci",
            onKembali = onBack,
        )

        // Konten Utama Paywall
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = UkuranIos.TepiLayar, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Kartu Utama Enterprise
            KartuIos(padding = PaddingValues(22.dp)) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Badge Ikon Gembok Emas
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(WarnaIos.Oranye.copy(alpha = 0.14f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Terkunci",
                            tint = NadaIos.PERINGATAN.teks,
                            modifier = Modifier.size(34.dp),
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    LencanaIos(
                        teks = "Lisensi Enterprise Diperlukan",
                        nada = NadaIos.PERINGATAN,
                        ikon = Icons.Default.WorkspacePremium,
                    )

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = "Chat Tim Terkunci",
                        style = TipeIos.Judul2,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(Modifier.height(18.dp))

                    // Kotak Keunggulan Fitur
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(UkuranIos.SudutBlok)
                            .background(WarnaIos.Latar)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        FiturPoin("Realtime Chat & Centang Dua Biru ala WhatsApp")
                        FiturPoin("Message Info (Lihat waktu dibaca per anggota tim)")
                        FiturPoin("Kirim Foto HD, Nota, & Lampiran Inventaris")
                        FiturPoin("Koordinasi Multi-Cabang & Jalur Server Terenkripsi")
                    }

                    Spacer(Modifier.height(18.dp))

                    // Bagian Harga Lisensi
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        LabelSeksiIos("Biaya lisensi seumur hidup")
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Rp 50.000.000",
                            style = TipeIos.AngkaBesar,
                        )
                        Text(
                            text = "Sekali Bayar · Akses Selamanya Seluruh Kru",
                            style = TipeIos.Catatan,
                            color = NadaIos.SUKSES.teks,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    // Tombol Aksi Beli Lisensi
                    TombolUtamaIos(
                        teks = "Beli Lisensi & Buka Kunci (Rp 50 Jt)",
                        onKlik = { sheetPembayaranTerbuka = true },
                    )

                    Spacer(Modifier.height(10.dp))

                    // Tombol Kembali ke Beranda
                    TombolKeduaIos(
                        teks = "Kembali ke Beranda",
                        onKlik = onBack,
                    )
                }
            }

            Spacer(Modifier.height(UkuranIos.JarakKartu))

            // Catatan Notifikasi
            KartuIos(padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(WarnaIos.Biru.copy(alpha = 0.14f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = WarnaIos.Biru,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Notifikasi pesan baru dari tim tetap akan masuk dan berbunyi di perangkat Anda.",
                        style = TipeIos.Catatan,
                        lineHeight = 18.sp,
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
                .background(WarnaIos.Hijau.copy(alpha = 0.16f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = NadaIos.SUKSES.teks,
                modifier = Modifier.size(13.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = teks,
            style = TipeIos.SubJudul,
            color = WarnaIos.Label,
        )
    }
}
