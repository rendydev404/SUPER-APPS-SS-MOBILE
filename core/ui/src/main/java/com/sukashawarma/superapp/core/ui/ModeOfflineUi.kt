package com.sukashawarma.superapp.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sukashawarma.superapp.data.remote.NetworkMonitor
import com.sukashawarma.superapp.data.remote.Outbox

private val AmberTeks = Color(0xFF92400E)
private val AmberLatar = Color(0xFFFEF3C7)
private val AbuTeks = Color(0xFF64748B)

/**
 * Pita yang muncul di seluruh aplikasi selama perangkat offline.
 *
 * Tanpa ini, mode offline jadi berbahaya: layar tetap menampilkan angka dari cache dan aksi
 * tetap "berhasil" disimpan, tetapi tidak ada satu pun tanda bahwa yang dilihat bukan
 * keadaan terkini dan yang dikerjakan belum sampai ke siapa pun.
 *
 * Jumlah antrean tetap ditampilkan beberapa saat setelah kembali online — itu justru saat
 * paling berguna, karena angkanya sedang menyusut dan orang bisa melihat kerjanya terkirim.
 */
@Composable
fun PitaOffline(modifier: Modifier = Modifier) {
    val online by NetworkMonitor.isOnline.collectAsState()
    // remember: jumlahMenunggu() membangun Flow baru tiap panggilan, jadi tanpa ini
    // setiap recomposition memutus dan memasang ulang pengamatannya ke Room.
    val antrean = remember { Outbox.jumlahMenunggu() }
    val menunggu by antrean.collectAsState(initial = 0)

    AnimatedVisibility(
        visible = !online || menunggu > 0,
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier,
    ) {
        val pesan = when {
            !online && menunggu > 0 ->
                "Mode offline — $menunggu aksi menunggu sinkron. Data mungkin belum terbaru."
            !online ->
                "Mode offline — data yang tampil mungkin belum terbaru."
            else ->
                "Mengirim $menunggu aksi yang tersimpan offline…"
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AmberLatar)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Default.CloudOff,
                contentDescription = null,
                tint = AmberTeks,
                modifier = Modifier.size(16.dp),
            )
            Text(text = pesan, color = AmberTeks, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

/**
 * Keadaan untuk fitur yang memang tidak bisa dipakai tanpa internet.
 *
 * [alasan] wajib spesifik. "Gagal memuat" generik membuat orang mencoba lagi berkali-kali
 * untuk sesuatu yang tidak akan pernah berhasil selama sinyalnya mati; menyebut alasannya
 * membuat mereka tahu harus menunggu, bukan mengulang.
 */
@Composable
fun KeadaanPerluInternet(
    namaFitur: String,
    alasan: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = AmberLatar.copy(alpha = 0.6f),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Default.CloudOff,
                contentDescription = null,
                tint = AmberTeks,
                modifier = Modifier.size(32.dp),
            )
            Text(
                text = "$namaFitur butuh internet",
                color = AmberTeks,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(text = alasan, color = AbuTeks, fontSize = 13.sp)
        }
    }
}
