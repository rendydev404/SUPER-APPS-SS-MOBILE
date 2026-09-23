package com.sukashawarma.superapp.core.ui.ios

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sukashawarma.superapp.core.ui.kaca.TokenKaca

/**
 * Palet design system "iOS" Suka Shawarma — warna sistem iOS (varian terang).
 *
 * Satu sumber warna untuk semua layar yang memakai komponen di package ini, supaya
 * modul dengan tata letak berbeda tetap terlihat satu aplikasi. Aksennya sama dengan
 * [TokenKaca.Aksen] agar kartu, tab bar kaca, dan lembar menu serasi.
 */
object WarnaIos {
    /** Latar halaman (systemGroupedBackground). */
    val Latar = Color(0xFFF2F2F7)
    /** Permukaan kartu & grup (secondarySystemGroupedBackground). */
    val Kartu = Color.White
    val Label = Color(0xFF1C1C1E)
    /** Teks sekunder — 60%, kontras ~4.6:1 di atas putih. */
    val LabelKedua = Color(0xFF3C3C43).copy(alpha = 0.60f)
    val LabelKetiga = Color(0xFF3C3C43).copy(alpha = 0.30f)
    /** Isian kontrol: kolom cari, tombol abu, blok angka (tertiarySystemFill). */
    val Isian = Color(0xFF767680).copy(alpha = 0.12f)
    /** Garis pemisah hairline (separator). */
    val Pemisah = Color(0xFF3C3C43).copy(alpha = 0.18f)
    val Aksen = TokenKaca.Aksen

    val Merah = Color(0xFFFF3B30)
    val Oranye = Color(0xFFFF9500)
    val Kuning = Color(0xFFFFCC00)
    val Hijau = Color(0xFF34C759)
    val Mint = Color(0xFF00C7BE)
    val Biru = Color(0xFF007AFF)
    val Indigo = Color(0xFF5856D6)
    val Ungu = Color(0xFFAF52DE)
    val Abu = Color(0xFF8E8E93)
    val AbuGelap = Color(0xFF48484A)
}

/**
 * Nada warna semantik. Tiap nada punya warna isian (titik, ikon, latar tipis) dan
 * warna teks yang lebih gelap — teks berwarna sistem iOS murni terlalu pucat untuk
 * dibaca di atas latarnya sendiri.
 */
enum class NadaIos(val warna: Color, val teks: Color) {
    NETRAL(WarnaIos.Abu, Color(0xFF636366)),
    AKSEN(WarnaIos.Aksen, Color(0xFFC2410C)),
    SUKSES(WarnaIos.Hijau, Color(0xFF248A3D)),
    PERINGATAN(WarnaIos.Oranye, Color(0xFFC93400)),
    BAHAYA(WarnaIos.Merah, Color(0xFFD70015)),
    INFO(WarnaIos.Biru, Color(0xFF0040DD)),
    UNGU(WarnaIos.Ungu, Color(0xFF8944AB)),
}

/** Skala tipografi iOS (Dynamic Type ukuran "Large"). */
object TipeIos {
    val JudulBesar = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp, color = WarnaIos.Label)
    val Judul1 = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp, color = WarnaIos.Label)
    val Judul2 = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp, color = WarnaIos.Label)
    val Judul3 = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.4).sp, color = WarnaIos.Label)
    val Utama = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp, color = WarnaIos.Label)
    val Isi = TextStyle(fontSize = 17.sp, letterSpacing = (-0.3).sp, color = WarnaIos.Label)
    val Keterangan = TextStyle(fontSize = 16.sp, letterSpacing = (-0.2).sp, color = WarnaIos.Label)
    val SubJudul = TextStyle(fontSize = 15.sp, letterSpacing = (-0.2).sp, color = WarnaIos.LabelKedua)
    val Catatan = TextStyle(fontSize = 13.sp, color = WarnaIos.LabelKedua)
    val Kecil = TextStyle(fontSize = 12.sp, color = WarnaIos.LabelKedua)
    val Angka = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp, color = WarnaIos.Label)
    val AngkaBesar = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp, color = WarnaIos.Label)
}

/** Radius & jarak baku. */
object UkuranIos {
    val SudutKartu = RoundedCornerShape(20.dp)
    val SudutGrup = RoundedCornerShape(16.dp)
    val SudutPetak = RoundedCornerShape(16.dp)
    val SudutKontrol = RoundedCornerShape(12.dp)
    val SudutBlok = RoundedCornerShape(14.dp)
    val SudutKapsul = RoundedCornerShape(50)

    /** Jarak tepi layar ke kartu. */
    val TepiLayar = 16.dp
    /** Jarak antar kartu dalam daftar. */
    val JarakKartu = 12.dp
    val PaddingKartu = 16.dp
    val TinggiKontrol = 40.dp
    val TinggiTombol = 50.dp
}
