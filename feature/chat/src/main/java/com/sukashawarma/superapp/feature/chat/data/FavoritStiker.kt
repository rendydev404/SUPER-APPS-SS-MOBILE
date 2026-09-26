package com.sukashawarma.superapp.feature.chat.data

import android.content.Context
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Stiker favorit, disimpan LOKAL di HP (SharedPreferences), terpisah per akun supaya
 * dua orang yang bergantian login di HP outlet yang sama tidak berbagi favorit.
 *
 * Sengaja tidak di database (keputusan pemilik, 24 Sep 2026): favorit hilang bila app
 * di-uninstall, datanya dihapus, atau pindah HP.
 *
 * Kunci setiap favorit adalah [StikerKlipy.urlKirim] — satu-satunya yang juga dimiliki
 * pesan stiker di percakapan (pesan tidak menyimpan slug), jadi stiker yang disimpan
 * dari papan pilihan dan dari pesan tidak tercatat dua kali.
 */
object FavoritStiker {
    private const val NAMA_PREFS = "stiker_favorit"
    private const val MAKS = 60

    private val _isi = MutableStateFlow<List<StikerKlipy>>(emptyList())
    /** Terbaru di depan. */
    val isi: StateFlow<List<StikerKlipy>> = _isi

    private var userDimuat: String? = null

    /** Memuat favorit akun [userId]. Murah dipanggil berulang: hanya membaca prefs
     *  saat akun berganti. */
    @Synchronized
    fun muat(konteks: Context, userId: String) {
        if (userId.isBlank() || userId == userDimuat) return
        val mentah = prefs(konteks).getString(kunci(userId), null)
        val daftar = runCatching { dariJson(mentah) }.getOrNull().orEmpty()
        // Disaring lagi saat dibaca: prefs bisa diubah di HP yang di-root, dan URL di luar
        // klipy.com akan ditolak database saat dikirim.
        _isi.value = daftar.filter { KlipyStiker.urlSah(it.urlKirim) && KlipyStiker.urlSah(it.urlPratinjau) }
        userDimuat = userId
    }

    fun ada(urlKirim: String): Boolean = _isi.value.any { it.urlKirim == urlKirim }

    /** Menambah bila belum ada, menghapus bila sudah ada. Mengembalikan true bila kini
     *  termasuk favorit. */
    @Synchronized
    fun alihkan(konteks: Context, userId: String, stiker: StikerKlipy): Boolean {
        if (userId.isBlank() || !KlipyStiker.urlSah(stiker.urlKirim)) return false
        muat(konteks, userId)
        val sudahAda = ada(stiker.urlKirim)
        val baru = if (sudahAda) {
            _isi.value.filterNot { it.urlKirim == stiker.urlKirim }
        } else {
            (listOf(stiker) + _isi.value).take(MAKS)
        }
        _isi.value = baru
        prefs(konteks).edit().putString(kunci(userId), keJson(baru)).apply()
        return !sudahAda
    }

    /** Stiker dari pesan di percakapan hanya punya satu URL (tanpa slug/pratinjau). */
    fun dariPesan(urlStiker: String): StikerKlipy =
        StikerKlipy(slug = urlStiker, judul = "", urlPratinjau = urlStiker, urlKirim = urlStiker, rasio = 1f)

    // Kunci JSON ditulis tangan, bukan Gson reflektif: favorit yang sudah tersimpan tetap
    // terbaca walau field StikerKlipy diganti nama atau R8 kelak diaktifkan di build rilis.
    internal fun keJson(daftar: List<StikerKlipy>): String = JsonArray().apply {
        daftar.forEach { s ->
            add(JsonObject().apply {
                addProperty("slug", s.slug)
                addProperty("judul", s.judul)
                addProperty("pratinjau", s.urlPratinjau)
                addProperty("kirim", s.urlKirim)
                addProperty("rasio", s.rasio)
            })
        }
    }.toString()

    internal fun dariJson(json: String?): List<StikerKlipy> {
        if (json.isNullOrBlank()) return emptyList()
        return JsonParser.parseString(json).asJsonArray.mapNotNull { el ->
            val o = el.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
            fun teks(k: String) = o.get(k)?.takeIf { it.isJsonPrimitive }?.asString
            val kirim = teks("kirim") ?: return@mapNotNull null
            StikerKlipy(
                slug = teks("slug") ?: kirim,
                judul = teks("judul").orEmpty(),
                urlPratinjau = teks("pratinjau") ?: kirim,
                urlKirim = kirim,
                rasio = o.get("rasio")?.takeIf { it.isJsonPrimitive }?.asFloat ?: 1f,
            )
        }
    }

    private fun prefs(konteks: Context) =
        konteks.applicationContext.getSharedPreferences(NAMA_PREFS, Context.MODE_PRIVATE)

    private fun kunci(userId: String) = "u_$userId"
}
