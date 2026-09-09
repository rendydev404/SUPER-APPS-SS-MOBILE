package com.sukashawarma.superapp.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import coil.ImageLoader
import coil.compose.AsyncImage
import com.sukashawarma.superapp.data.remote.SupabaseClient

/**
 * Foto profil staff, dengan huruf awal nama sebagai cadangan.
 *
 * Bucket `avatars` bersifat PRIVAT, jadi URL-nya tidak bisa dimuat Coil begitu saja:
 * endpoint `object/authenticated/...` menuntut Authorization. Alih-alih menandatangani
 * URL satu per satu (satu request jaringan tambahan per avatar, plus masa berlaku yang
 * harus diurus), Coil di sini dipasangi `SupabaseClient.okHttpClient` — klien yang sama
 * dengan sisa aplikasi, yang interceptor-nya sudah menyisipkan access token.
 */
object AvatarStorage {
    const val BUCKET = "avatars"

    /** Ubah path objek tersimpan jadi URL yang bisa dimuat. Menerima bentuk dengan
     *  maupun tanpa awalan "avatars/", karena kolom `avatar_url` menyimpan yang
     *  berawalan sementara pemanggil sering sudah memotongnya. */
    fun url(path: String?): String? {
        if (path.isNullOrBlank()) return null
        if (path.startsWith("http://") || path.startsWith("https://")) return path
        val objek = path.removePrefix("$BUCKET/")
        return "${SupabaseClient.BASE_URL}storage/v1/object/authenticated/$BUCKET/$objek"
    }
}

@Composable
fun AvatarStaf(
    path: String?,
    nama: String?,
    modifier: Modifier = Modifier,
    bentuk: Shape = CircleShape,
    warnaLatar: Color = Color(0x33EA580C),
    warnaHuruf: Color = Color(0xFF0F172A),
    ukuranHuruf: TextUnit = 18.sp,
) {
    val konteks = LocalContext.current
    // ImageLoader di-remember per pemanggil, bukan dibuat ulang tiap recomposition:
    // membuatnya di badan composable akan membuang cache memori Coil setiap kali
    // layar tergambar ulang.
    val pemuat = remember(konteks) {
        ImageLoader.Builder(konteks)
            .okHttpClient { SupabaseClient.okHttpClient }
            .build()
    }
    val url = AvatarStorage.url(path)

    Box(modifier.clip(bentuk).background(warnaLatar), contentAlignment = Alignment.Center) {
        Text(
            nama?.trim()?.firstOrNull()?.uppercase() ?: "?",
            color = warnaHuruf,
            fontWeight = FontWeight.Bold,
            fontSize = ukuranHuruf,
        )
        // Huruf awal digambar lebih dulu dan foto menimpanya: itu membuat avatar
        // tidak pernah berkedip kosong selagi foto dimuat atau ketika gagal dimuat.
        if (url != null) {
            AsyncImage(
                model = url,
                imageLoader = pemuat,
                contentDescription = "Foto profil ${nama.orEmpty()}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}
