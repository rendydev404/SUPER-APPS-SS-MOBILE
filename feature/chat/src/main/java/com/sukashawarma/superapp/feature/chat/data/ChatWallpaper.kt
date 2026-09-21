package com.sukashawarma.superapp.feature.chat.data

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.sukashawarma.superapp.core.ui.AvatarStorage
import com.sukashawarma.superapp.domain.model.Role
import com.sukashawarma.superapp.feature.chat.R
import java.io.File

/**
 * Representasi satu opsi wallpaper bawaan tim.
 *
 * Nilai [id] disimpan di kolom `chat_settings.wallpaper` atau SharedPreferences.
 * [drawableRes] null menandakan wallpaper "Default Putih" polos standar.
 */
data class ItemWallpaper(
    val id: String,
    val nama: String,
    val deskripsi: String,
    @DrawableRes val drawableRes: Int? = null,
)

/**
 * Representasi opsi wallpaper warna solid minimalis.
 */
data class WarnaWallpaper(
    val id: String,
    val nama: String,
    val color: Color,
    val gelap: Boolean = false,
)

/**
 * Tipe klasifikasi wallpaper yang didukung.
 */
sealed class TipeWallpaper {
    data class BawaanDrawable(@DrawableRes val resId: Int) : TipeWallpaper()
    data class WarnaSolid(val color: Color) : TipeWallpaper()
    data class BerkasLokal(val file: File) : TipeWallpaper()
    data class RemoteUrl(val url: String) : TipeWallpaper()
    object DefaultPutih : TipeWallpaper()
}

object ChatWallpapers {

    val DEFAULT = ItemWallpaper(
        id = "default",
        nama = "Default Putih",
        deskripsi = "Latar putih bersih bawaan sistem",
        drawableRes = null,
    )

    val DAFTAR: List<ItemWallpaper> = listOf(
        DEFAULT,
        ItemWallpaper(
            id = "warm_ivory",
            nama = "Warm Ivory Gourmet",
            deskripsi = "Doodle karamel keemasan ala WhatsApp klasik",
            drawableRes = R.drawable.wallpaper_warm_ivory,
        ),
        ItemWallpaper(
            id = "cyberpunk_neon",
            nama = "Cyberpunk Neon",
            deskripsi = "Garis neon menyala di atas suasana twilight",
            drawableRes = R.drawable.wallpaper_cyberpunk_neon,
        ),
        ItemWallpaper(
            id = "midnight_noir",
            nama = "Midnight Noir",
            deskripsi = "Doodle cyan gelap ala Telegram Dark",
            drawableRes = R.drawable.wallpaper_midnight_noir,
        ),
        ItemWallpaper(
            id = "japandi_linen",
            nama = "Japandi Linen Craft",
            deskripsi = "Tekstur kertas gandum dengan emboss origami zen",
            drawableRes = R.drawable.wallpaper_japandi_linen,
        ),
        ItemWallpaper(
            id = "cosmic_galaxy",
            nama = "Cosmic Galaxy",
            deskripsi = "Rasi bintang & debu galaksi magis",
            drawableRes = R.drawable.wallpaper_cosmic_galaxy,
        ),
        ItemWallpaper(
            id = "kawaii_clay",
            nama = "3D Kawaii Clay",
            deskripsi = "Gradasi pastel dengan figur tanah liat 3D ceria",
            drawableRes = R.drawable.wallpaper_kawaii_clay,
        ),
        ItemWallpaper(
            id = "blueprint_grid",
            nama = "Architectural Blueprint",
            deskripsi = "Skema teknis presisi tinggi pada kertas blueprint",
            drawableRes = R.drawable.wallpaper_blueprint_grid,
        ),
        ItemWallpaper(
            id = "zen_sage",
            nama = "Zen Sage Minimalist",
            deskripsi = "Pastel hijau matcha sejuk & segar",
            drawableRes = R.drawable.wallpaper_zen_sage,
        ),
        ItemWallpaper(
            id = "sunset_terracotta",
            nama = "Sunset Terracotta",
            deskripsi = "Gradasi watercolor hangat dengan fokus tengah",
            drawableRes = R.drawable.wallpaper_sunset_terracotta,
        ),
    )

    /**
     * Palet warna solid minimalis pilihan ala WhatsApp / iOS.
     */
    val DAFTAR_WARNA: List<WarnaWallpaper> = listOf(
        WarnaWallpaper("color:#0B141A", "WhatsApp Gelap", Color(0xFF0B141A), gelap = true),
        WarnaWallpaper("color:#054D44", "Teal Zamrud", Color(0xFF054D44), gelap = true),
        WarnaWallpaper("color:#0F172A", "Slate Navy", Color(0xFF0F172A), gelap = true),
        WarnaWallpaper("color:#18181B", "Charcoal Pekat", Color(0xFF18181B), gelap = true),
        WarnaWallpaper("color:#1F2937", "Abu Malam", Color(0xFF1F2937), gelap = true),
        WarnaWallpaper("color:#ECE5DD", "Krem Sand Klasik", Color(0xFFECE5DD), gelap = false),
        WarnaWallpaper("color:#E2ECE9", "Sage Sejuk", Color(0xFFE2ECE9), gelap = false),
        WarnaWallpaper("color:#E8EFE8", "Matcha Lembut", Color(0xFFE8EFE8), gelap = false),
        WarnaWallpaper("color:#F1EFF7", "Lavender Mist", Color(0xFFF1EFF7), gelap = false),
        WarnaWallpaper("color:#FCEEF0", "Mawar Pastel", Color(0xFFFCEEF0), gelap = false),
        WarnaWallpaper("color:#F8FAFC", "Putih Terang", Color(0xFFF8FAFC), gelap = false),
    )

    fun cari(id: String?): ItemWallpaper {
        val bersih = id?.trim().orEmpty()
        return DAFTAR.firstOrNull { it.id.equals(bersih, ignoreCase = true) } ?: DEFAULT
    }

    fun parseHexColor(hexStr: String): Color? {
        val clean = hexStr.removePrefix("#").trim()
        val intVal = clean.toLongOrNull(16) ?: return null
        return when (clean.length) {
            6 -> Color((0xFF000000L or intVal).toLong())
            8 -> Color(intVal)
            else -> null
        }
    }

    /**
     * Mengurai identifier wallpaper menjadi representasi render yang sesuai.
     */
    fun uraikan(id: String?): TipeWallpaper {
        val bersih = id?.trim().orEmpty()
        if (bersih.isBlank() || bersih.equals("default", ignoreCase = true) || bersih.equals("bawaan", ignoreCase = true)) {
            return TipeWallpaper.DefaultPutih
        }

        // Warna Solid, format "color:#RRGGBB"
        if (bersih.startsWith("color:", ignoreCase = true)) {
            val hex = bersih.removePrefix("color:").trim()
            val color = parseHexColor(hex)
            return if (color != null) {
                TipeWallpaper.WarnaSolid(color)
            } else {
                TipeWallpaper.DefaultPutih
            }
        }

        // Berkas foto kustom lokal di HP
        if (bersih.startsWith("file:", ignoreCase = true) || bersih.startsWith("/")) {
            val path = bersih.removePrefix("file:")
            val file = File(path)
            return if (file.exists() && file.isFile) {
                TipeWallpaper.BerkasLokal(file)
            } else {
                TipeWallpaper.DefaultPutih
            }
        }

        // URL remote (misal foto grup yang diunggah ke storage)
        if (bersih.startsWith("http://", ignoreCase = true) || bersih.startsWith("https://", ignoreCase = true)) {
            return TipeWallpaper.RemoteUrl(bersih)
        }

        // Path Supabase Storage (misal avatars/... foto grup/wallpaper tim)
        if (bersih.startsWith("avatars/", ignoreCase = true)) {
            val fullUrl = AvatarStorage.url(bersih)
            if (fullUrl != null) return TipeWallpaper.RemoteUrl(fullUrl)
        }

        // Preset ilustrasi bawaan
        val item = DAFTAR.firstOrNull { it.id.equals(bersih, ignoreCase = true) }
        val res = item?.drawableRes
        return if (res != null) {
            TipeWallpaper.BawaanDrawable(res)
        } else {
            TipeWallpaper.DefaultPutih
        }
    }

    /**
     * Hanya role DEVELOPER yang berhak mengganti dan mengedit wallpaper chat.
     * Untuk role lainnya (user biasa), pengaturan wallpaper dikunci.
     */
    fun bolehUbahWallpaper(role: Role?, roleRaw: String?): Boolean {
        return role == Role.DEVELOPER ||
            roleRaw?.equals("developer", ignoreCase = true) == true
    }
}

/**
 * Komponen gambar latar belakang ruang obrolan.
 * Ditaruh di lapisan paling dasar sebelum bubble pesan.
 *
 * @param wallpaperId ID wallpaper (preset, color:#..., file:..., atau remote URL).
 * @param dimming Nilai peredup overlay hitam (0.0f = transparan, hingga 0.8f = gelap) untuk meningkatkan kontras teks.
 */
@Composable
fun WallpaperLatarChat(
    wallpaperId: String?,
    dimming: Float = 0f,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val tipe = remember(wallpaperId) { ChatWallpapers.uraikan(wallpaperId) }

    Box(modifier = modifier.fillMaxSize()) {
        when (tipe) {
            is TipeWallpaper.BawaanDrawable -> {
                Image(
                    painter = painterResource(id = tipe.resId),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            is TipeWallpaper.WarnaSolid -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(tipe.color),
                )
            }
            is TipeWallpaper.BerkasLokal -> {
                AsyncImage(
                    model = tipe.file,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            is TipeWallpaper.RemoteUrl -> {
                AsyncImage(
                    model = tipe.url,
                    imageLoader = AvatarStorage.imageLoader(context),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            is TipeWallpaper.DefaultPutih -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFEFEFF4)),
                )
            }
        }

        // Lapisan peredup (dimming scrim) untuk menjamin keterbacaan teks pesan
        if (dimming > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = dimming.coerceIn(0f, 0.8f))),
            )
        }
    }
}
