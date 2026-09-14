package com.sukashawarma.superapp.feature.chat.data

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sukashawarma.superapp.feature.chat.R

/**
 * Representasi satu opsi wallpaper obrolan tim.
 *
 * Nilai [id] disimpan di kolom `chat_settings.wallpaper`.
 * [drawableRes] null menandakan wallpaper "Default Putih" polos standar WhatsApp.
 */
data class ItemWallpaper(
    val id: String,
    val nama: String,
    val deskripsi: String,
    @DrawableRes val drawableRes: Int? = null,
)

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

    fun cari(id: String?): ItemWallpaper {
        val bersih = id?.trim().orEmpty()
        return DAFTAR.firstOrNull { it.id.equals(bersih, ignoreCase = true) } ?: DEFAULT
    }
}

/**
 * Komponen gambar latar belakang ruang obrolan.
 * Ditaruh di lapisan paling dasar sebelum [androidx.compose.foundation.lazy.LazyColumn] bubble pesan.
 */
@Composable
fun WallpaperLatarChat(
    wallpaperId: String?,
    modifier: Modifier = Modifier,
) {
    val wallpaper = ChatWallpapers.cari(wallpaperId)
    val resId = wallpaper.drawableRes
    if (resId != null) {
        Image(
            painter = painterResource(id = resId),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.fillMaxSize(),
        )
    } else {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.White),
        )
    }
}
