package com.sukashawarma.superapp.feature.chat.data

import android.content.Context
import android.os.Build
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.sukashawarma.superapp.feature.chat.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/** Isi `body` pesan stiker. App lama & web menampilkan teks ini; push dan kutipan
 *  balasan juga memakainya, jadi server tidak perlu tahu soal stiker sama sekali. */
const val TEKS_CADANGAN_STIKER = "Stiker"

/** Satu stiker dari KLIPY. [urlPratinjau] kecil untuk kisi pilihan, [urlKirim] yang
 *  disimpan di pesan. [rasio] = lebar / tinggi, supaya kotak tidak melompat saat dimuat. */
data class StikerKlipy(
    val slug: String,
    val judul: String,
    val urlPratinjau: String,
    val urlKirim: String,
    val rasio: Float,
)

data class HalamanStiker(val isi: List<StikerKlipy>, val adaLagi: Boolean)

/**
 * Klien Sticker API KLIPY (https://docs.klipy.com). Stiker tidak diunduh ke storage
 * kita: pesan hanya menyimpan URL CDN KLIPY.
 *
 * Sengaja memakai OkHttpClient sendiri, bukan `SupabaseClient.okHttpClient`: kunci
 * API berada di path URL, dan klien Supabase mencatat URL lengkap di build debug.
 */
object KlipyStiker {
    private const val DASAR = "https://api.klipy.com/api/v1"
    private const val PER_HALAMAN = 24

    /** Harus sama persis dengan CHECK `*_stiker_klipy` di database (migrasi
     *  20300243000000) — URL lain akan ditolak server saat pesan disimpan. */
    private val POLA_URL_SAH = Regex("^https://([a-z0-9-]+\\.)*klipy\\.com/.*")
    private const val PANJANG_URL_MAKS = 500

    val aktif: Boolean get() = BuildConfig.KLIPY_API_KEY.isNotBlank()

    private val klien by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    fun urlSah(url: String?): Boolean =
        url != null && url.length <= PANJANG_URL_MAKS && POLA_URL_SAH.matches(url)

    /** `customer_id` yang diminta KLIPY untuk riwayat/iklan. Id staf di-hash supaya
     *  UUID asli tidak pernah keluar ke pihak ketiga. */
    fun idPelanggan(userId: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest("superapp-stiker:$userId".toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(32)

    suspend fun trending(halaman: Int, userId: String): HalamanStiker =
        ambil("trending", halaman, userId, kueri = null)

    suspend fun cari(kueri: String, halaman: Int, userId: String): HalamanStiker =
        ambil("search", halaman, userId, kueri = kueri.trim())

    private suspend fun ambil(jalur: String, halaman: Int, userId: String, kueri: String?): HalamanStiker =
        withContext(Dispatchers.IO) {
            check(aktif) { "Kunci API KLIPY belum diatur." }
            val url = "$DASAR/${BuildConfig.KLIPY_API_KEY}/stickers/$jalur".toHttpUrl().newBuilder()
                .addQueryParameter("page", halaman.toString())
                .addQueryParameter("per_page", PER_HALAMAN.toString())
                .addQueryParameter("customer_id", idPelanggan(userId))
                .apply { if (kueri != null) addQueryParameter("q", kueri) }
                .build()
            klien.newCall(Request.Builder().url(url).get().build()).execute().use { res ->
                // Pesan galat sengaja tidak memuat URL: di dalamnya ada kunci API.
                if (!res.isSuccessful) error("Server stiker menolak permintaan (HTTP ${res.code}).")
                parseHalaman(res.body?.string().orEmpty())
            }
        }

    /** Terpisah dari jaringan supaya bisa diuji dengan JSON contoh. */
    internal fun parseHalaman(json: String): HalamanStiker {
        val akar = JsonParser.parseString(json).asJsonObject
        val data = akar.getAsJsonObject("data") ?: return HalamanStiker(emptyList(), false)
        val isi = data.getAsJsonArray("data")?.mapNotNull { el ->
            val item = el.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
            // Slot iklan diselipkan KLIPY di antara stiker — bukan stiker, lewati.
            if (item.teks("type") == "ad") return@mapNotNull null
            parseStiker(item)
        }.orEmpty()
        val adaLagi = data.get("has_next")?.takeIf { !it.isJsonNull }?.asBoolean ?: false
        return HalamanStiker(isi, adaLagi)
    }

    private fun parseStiker(item: JsonObject): StikerKlipy? {
        val slug = item.teks("slug") ?: return null
        val berkas = item.getAsJsonObject("file") ?: return null
        // Pratinjau sekecil mungkin (kisi berisi puluhan stiker sekaligus); yang dikirim
        // satu tingkat lebih besar supaya tetap tajam di bubble ~140dp.
        val pratinjau = berkas.varian("xs") ?: berkas.varian("sm") ?: return null
        val kirim = berkas.varian("sm") ?: berkas.varian("md") ?: pratinjau
        if (!urlSah(pratinjau.url) || !urlSah(kirim.url)) return null
        val rasio = if (kirim.lebar > 0 && kirim.tinggi > 0) kirim.lebar.toFloat() / kirim.tinggi else 1f
        return StikerKlipy(
            slug = slug,
            judul = item.teks("title").orEmpty(),
            urlPratinjau = pratinjau.url,
            urlKirim = kirim.url,
            rasio = rasio.coerceIn(0.5f, 2f),
        )
    }

    private data class Varian(val url: String, val lebar: Int, val tinggi: Int)

    /** WebP lebih dulu (animasi, jauh lebih kecil dari GIF), GIF sebagai cadangan. */
    private fun JsonObject.varian(ukuran: String): Varian? {
        val tingkat = getAsJsonObject(ukuran) ?: return null
        for (format in listOf("webp", "gif")) {
            val f = tingkat.getAsJsonObject(format) ?: continue
            val url = f.teks("url") ?: continue
            return Varian(url, f.angka("width"), f.angka("height"))
        }
        return null
    }

    private fun JsonObject.teks(kunci: String): String? =
        get(kunci)?.takeIf { it.isJsonPrimitive }?.asString?.takeIf { it.isNotBlank() }

    private fun JsonObject.angka(kunci: String): Int =
        get(kunci)?.takeIf { it.isJsonPrimitive }?.asInt ?: 0
}

/**
 * ImageLoader khusus stiker: klien HTTP polos (CDN publik KLIPY tidak boleh menerima
 * token Supabase), dekoder animasi, dan cache disk sendiri supaya stiker yang sering
 * dipakai tidak diunduh ulang dan tidak menggusur cache foto chat.
 *
 * Android 9+ memutar WebP & GIF animasi lewat ImageDecoder. Android 8 hanya bisa
 * memutar GIF; WebP animasi tampil sebagai gambar diam.
 */
object StikerMedia {
    @Volatile
    private var pemuat: ImageLoader? = null

    fun imageLoader(konteks: Context): ImageLoader = pemuat ?: synchronized(this) {
        pemuat ?: bangun(konteks.applicationContext).also { pemuat = it }
    }

    private fun bangun(konteks: Context): ImageLoader = ImageLoader.Builder(konteks)
        .okHttpClient {
            OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build()
        }
        .components {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) add(ImageDecoderDecoder.Factory())
            else add(GifDecoder.Factory())
        }
        .memoryCache { MemoryCache.Builder(konteks).maxSizePercent(0.10).build() }
        .diskCache {
            DiskCache.Builder()
                .directory(konteks.cacheDir.resolve("stiker_cache"))
                .maxSizeBytes(60L * 1024 * 1024)
                .build()
        }
        // CDN KLIPY memberi header cache pendek; berkas stiker tidak pernah berubah
        // untuk URL yang sama, jadi cache disk dipakai terus.
        .respectCacheHeaders(false)
        .crossfade(false)
        .build()
}
