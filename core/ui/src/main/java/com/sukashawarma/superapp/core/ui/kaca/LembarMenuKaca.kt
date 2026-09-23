package com.sukashawarma.superapp.core.ui.kaca

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Lembar menu bawah dengan latar blur tebal — pasangan [BilahTabKaca] untuk tombol
 * "Lainnya"/"Menu"/"More" di semua modul.
 *
 * Bukan kaca cair: di sini yang diburamkan adalah seluruh layar di belakang, dan
 * lembarnya putih tembus pandang di atasnya. Blur dinyalakan otomatis lewat
 * [ShellKaca] terdekat selama lembar ini ada di komposisi; di luar shell, atau di
 * Android < 12, lembarnya dibuat hampir pekat karena teks tajam di belakang akan
 * bertabrakan dengan isinya.
 *
 * Isi disusun dengan [JudulKelompokMenuKaca] dan [BarisMenuKaca]. Untuk berpindah
 * layar dari sebuah baris, sembunyikan dulu [sheetState] lalu panggil [onTutup]
 * setelah animasinya selesai — menutup paksa bersamaan dengan perpindahan layar
 * membuat isinya berkedip.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LembarMenuKaca(
    onTutup: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    judul: String? = null,
    isi: @Composable ColumnScope.() -> Unit,
) {
    val pengendali = LocalPengendaliBlurKaca.current
    DisposableEffect(pengendali) {
        pengendali?.let { it.jumlah += 1 }
        onDispose { pengendali?.let { it.jumlah -= 1 } }
    }
    val latarKabur = pengendali != null && TokenKaca.blurDidukung

    ModalBottomSheet(
        onDismissRequest = onTutup,
        sheetState = sheetState,
        containerColor = if (latarKabur) Color.White.copy(alpha = 0.62f) else Color(0xFFF8FAFC),
        scrimColor = Color(0xFF0F172A).copy(alpha = if (latarKabur) 0.12f else 0.32f),
        tonalElevation = 0.dp,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)) {
            if (judul != null) {
                Text(
                    judul,
                    Modifier.padding(bottom = 16.dp),
                    color = TokenKaca.Teks,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.3).sp,
                )
            }
            isi()
        }
        Spacer(Modifier.height(12.dp))
    }
}

/** Judul kelompok di dalam [LembarMenuKaca], huruf kapital kecil berjarak. */
@Composable
fun JudulKelompokMenuKaca(teks: String, modifier: Modifier = Modifier) {
    Text(
        teks.uppercase(),
        modifier.padding(bottom = 10.dp),
        color = TokenKaca.TeksKedua,
        fontSize = 11.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 0.9.sp,
    )
}

/**
 * Satu tujuan di [LembarMenuKaca]: kartu putih membulat, ikon beraksen tanpa kotak
 * warna, judul tebal, chevron. Tujuan yang sedang terbuka menjadi kartu aksen penuh.
 */
@Composable
fun BarisMenuKaca(
    ikon: ImageVector,
    label: String,
    onKlik: () -> Unit,
    modifier: Modifier = Modifier,
    aktif: Boolean = false,
    lencana: Int = 0,
    keterangan: String? = null,
    warnaAksen: Color = TokenKaca.Aksen,
) {
    Surface(
        modifier.fillMaxWidth().padding(bottom = 10.dp).clickable(onClick = onKlik),
        shape = RoundedCornerShape(18.dp),
        // Sedikit tembus pandang supaya kartu menyatu dengan latar buram di belakang
        // lembar, tapi masih cukup pekat untuk teks kontras tinggi.
        color = if (aktif) warnaAksen else Color.White.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, if (aktif) warnaAksen else Color(0xFFE2E8F0)),
        shadowElevation = if (aktif) 4.dp else 0.dp,
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = if (keterangan == null) 15.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (aktif) IkonIos.aktif(ikon) else ikon,
                null,
                tint = if (aktif) Color.White else warnaAksen,
                modifier = Modifier.size(23.dp),
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    label,
                    color = if (aktif) Color.White else TokenKaca.Teks,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (keterangan != null) {
                    Text(
                        keterangan,
                        color = if (aktif) Color.White.copy(alpha = 0.85f) else TokenKaca.TeksKedua,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (lencana > 0) {
                Spacer(Modifier.width(10.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    // Di baris aktif latarnya sudah beraksen, jadi lencana merah di
                    // atasnya nyaris tak terbaca; dibalik jadi putih dengan angka aksen.
                    color = if (aktif) Color.White else TokenKaca.MerahLencana,
                ) {
                    Text(
                        teksLencanaKaca(lencana),
                        Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        color = if (aktif) warnaAksen else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                IkonIos.ChevronRight,
                null,
                tint = if (aktif) Color.White else Color(0xFF94A3B8),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
