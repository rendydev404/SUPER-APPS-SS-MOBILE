package com.sukashawarma.superapp.core.ui.kaca

import android.os.Build
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Token design system "kaca" Suka Shawarma — satu sumber untuk warna dan ukuran yang
 * dipakai [BilahTabKaca], [ShellKaca], dan [LembarMenuKaca] di semua modul.
 *
 * Tata letak tiap modul boleh berbeda; yang harus sama adalah bahasa visualnya:
 * aksen oranye yang sama, kapsul setinggi yang sama, dan blur setebal yang sama.
 */
object TokenKaca {
    /**
     * Oranye primary untuk tab/menu aktif. Dipilih `#EA580C`, bukan `SukaOrange`
     * (`#F29744`): label oranye pucat di atas kaca putih kontrasnya hanya ~2.3:1,
     * terlalu rendah untuk teks sekecil label tab.
     */
    val Aksen = Color(0xFFEA580C)

    /** Label & ikon tak aktif di atas kaca terang: hitam iOS 88%, kontras > 7:1. */
    val Label = Color(0xFF1C1C1E).copy(alpha = 0.88f)

    /** Teks utama di kartu menu. */
    val Teks = Color(0xFF0F172A)

    /** Judul kelompok menu dan teks sekunder. */
    val TeksKedua = Color(0xFF475569)

    val MerahLencana = Color(0xFFFF3B30)

    val TinggiKapsul: Dp = 68.dp
    val JarakKapsulBawah: Dp = 10.dp

    /** Jarak kosong di atas kapsul sebelum baris terakhir daftar, agar tidak menempel ke kaca. */
    val CelahAtasKapsul: Dp = 8.dp

    /** Radius blur latar saat [LembarMenuKaca] terbuka. */
    val BlurLembar: Dp = 28.dp

    /** RenderEffect (blur & pembiasan kaca) baru ada sejak Android 12. */
    val blurDidukung: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
}

/** Tinggi total yang ditutupi [BilahTabKaca], termasuk bilah navigasi sistem. */
@Composable
fun ruangBilahTabKaca(): Dp =
    TokenKaca.TinggiKapsul + TokenKaca.JarakKapsulBawah + TokenKaca.CelahAtasKapsul +
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

/** Angka lencana dipendekkan seperti web: di atas sembilan cukup "9+". */
fun teksLencanaKaca(jumlah: Int): String = if (jumlah > 9) "9+" else jumlah.toString()
