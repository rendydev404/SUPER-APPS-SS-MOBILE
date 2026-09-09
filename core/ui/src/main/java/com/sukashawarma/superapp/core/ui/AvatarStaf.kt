package com.sukashawarma.superapp.core.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.disk.DiskCache
import coil.memory.MemoryCache
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

    @Volatile
    private var pemuat: ImageLoader? = null

    /**
     * SATU ImageLoader untuk seluruh aplikasi.
     *
     * Ini bukan kerapian, ini syarat performa. Setiap ImageLoader membawa memory
     * cache sendiri yang ukurannya persentase dari heap, plus disk cache sendiri.
     * Versi sebelumnya membuatnya dengan `remember` di dalam composable — artinya
     * satu ImageLoader per PEMANGGIL. Di papan kehadiran berisi 30 kru itu jadi 30
     * cache paralel yang saling berebut heap, dan foto yang sama diunduh serta
     * di-decode ulang di tiap petak. Dengan satu instance, avatar yang sudah pernah
     * dimuat di Beranda langsung terpakai lagi di layar mana pun tanpa jaringan.
     *
     * Sengaja memakai applicationContext: menyimpan Activity di field `object`
     * (yang hidup selama proses) akan menahan seluruh Activity dari GC.
     */
    fun imageLoader(konteks: Context): ImageLoader = pemuat ?: synchronized(this) {
        pemuat ?: bangun(konteks.applicationContext).also { pemuat = it }
    }

    private fun bangun(konteks: Context): ImageLoader = ImageLoader.Builder(konteks)
        .callFactory { SupabaseClient.okHttpClient }
        // 10% heap: avatar itu bitmap kecil (maksimal 512px, ditampilkan <=120dp).
        // Default Coil 25% terlalu rakus untuk gambar sekecil ini dan menyisakan
        // lebih sedikit ruang untuk kamera dan daftar yang jadi inti aplikasi.
        .memoryCache {
            MemoryCache.Builder(konteks).maxSizePercent(0.10).build()
        }
        // Cache disk supaya avatar tidak diunduh ulang tiap kali app dibuka. 20 MB
        // menampung ratusan avatar pada ukuran ~40 KB.
        .diskCache {
            DiskCache.Builder()
                .directory(konteks.cacheDir.resolve("avatar_cache"))
                .maxSizeBytes(20L * 1024 * 1024)
                .build()
        }
        // Header dari Supabase Storage untuk endpoint `authenticated` bisa melarang
        // penyimpanan, yang berarti avatar diunduh ulang setiap kali layar dibuka.
        // Aman diabaikan DI SINI karena nama berkas avatar selalu UUID baru setiap
        // unggah: URL yang sama tidak pernah berubah isinya, jadi tidak ada versi
        // basi yang bisa tersangkut.
        .respectCacheHeaders(false)
        // Crossfade dimatikan: avatar muncul di daftar yang di-scroll, dan animasi
        // per petak membebani frame tanpa memberi manfaat yang terlihat.
        .crossfade(false)
        .build()
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
                imageLoader = AvatarStorage.imageLoader(konteks),
                contentDescription = "Foto profil ${nama.orEmpty()}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                // Avatar selalu digambar jauh lebih kecil dari sumbernya (512px ->
                // 54dp di Beranda). Penyaringan kualitas tinggi di sini hanya
                // membakar waktu GPU untuk perbedaan yang tidak terlihat pada
                // lingkaran seukuran kuku jari.
                filterQuality = FilterQuality.Low,
            )
        }
    }
}
