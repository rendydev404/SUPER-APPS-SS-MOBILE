package com.sukashawarma.superapp.data.remote

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Klien PostgREST/RPC generik lewat OkHttp — 1 kelas menggantikan puluhan interface
 * Retrofit satu-endpoint-per-query, yang tak masuk akal untuk ~40 layar lintas modul.
 *
 * Params berupa List<Pair>, BUKAN Map: filter range (mis. `ts_server=gte.X` DAN
 * `ts_server=lte.Y` sekaligus) butuh key yang sama muncul dua kali — Map tidak bisa
 * merepresentasikan itu (key kedua diam-diam menimpa yang pertama).
 */
object Postgrest {
    private val client get() = SupabaseClient.okHttpClient
    private val jsonMedia = "application/json".toMediaType()

    class PostgrestException(val code: Int, message: String) : Exception(message)

    private fun urlFor(path: String, params: List<Pair<String, String>> = emptyList()): okhttp3.HttpUrl {
        val builder = "${SupabaseClient.BASE_URL}rest/v1/$path".toHttpUrl().newBuilder()
        params.forEach { (k, v) -> builder.addQueryParameter(k, v) }
        return builder.build()
    }

    private suspend fun execute(request: Request): String = withContext(Dispatchers.IO) {
        client.newCall(request).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw PostgrestException(resp.code, body.ifBlank { resp.message })
            body
        }
    }

    /**
     * Penguraian JSON dijalankan di luar thread pemanggil.
     *
     * [execute] hanya memindahkan bagian JARINGAN ke IO; hasilnya kembali
     * sebagai String dan diurai di context pemanggil — yang untuk
     * `viewModelScope.launch` berarti MAIN THREAD. Balasan berisi ratusan baris
     * (daftar chat, ledger, laporan) karena itu mengurai di thread UI dan
     * membuat frame jatuh persis saat data datang. Satu pembungkus ini
     * memindahkan semuanya ke Default.
     */
    private suspend fun <T> parse(blok: suspend () -> T): T = withContext(Dispatchers.Default) { blok() }

    /**
     * Tabel yang TIDAK boleh disajikan dari cache.
     *
     * Chat dikecualikan karena riwayat percakapan lama yang tampil tanpa beda apa pun dari
     * yang baru itu menyesatkan: orang akan mengira pesan terakhir yang mereka lihat adalah
     * pesan terakhir yang ada, lalu mengambil keputusan atas dasar itu. Chat memang
     * online-only, dan lebih baik terlihat jelas kosong daripada terlihat terkini.
     */
    private val TANPA_CACHE = setOf(
        "chat_messages",
        "chat_message_reactions",
        "chat_settings",
        "private_chat_messages",
    )

    /**
     * Pembacaan tabel, dengan cache sebagai jaring pengaman saat jaringan mati.
     *
     * Dipasang DI SINI, bukan satu per satu di tiap repository: puluhan layar memanggil
     * fungsi ini, dan memasangnya per pemanggil berarti layar yang terlewat tetap jadi jalan
     * buntu saat offline tanpa ada yang menyadarinya sampai ada crew yang mengeluh.
     *
     * Kuncinya memuat seluruh parameter query, termasuk filter `outlet_id`. Itu yang membuat
     * data outlet lain tidak mungkin tertukar saat staf berpindah outlet — tanpa perlu
     * membersihkan apa pun.
     *
     * Saat online perilakunya sama persis seperti sebelumnya: server tetap dipanggil, dan
     * tidak ada satu pun layar yang bisa menampilkan angka basi selama sinyalnya masih ada.
     * [rpc] sengaja TIDAK ikut di-cache — hasil hitungan server (laporan, saldo, HPP) yang
     * basi tidak bisa dibedakan dari yang benar oleh siapa pun yang melihatnya.
     */
    suspend fun select(table: String, params: List<Pair<String, String>> = emptyList()): JsonArray {
        if (table in TANPA_CACHE) return selectLangsung(table, params)
        val kunci = "select:$table?" + params.joinToString("&") { "${it.first}=${it.second}" }
        return CacheOffline.bacaArray(kunci, umurMaksMs = CacheOffline.UMUR_MAKS_BAWAAN_MS) {
            selectLangsung(table, params)
        }.data
    }

    private suspend fun selectLangsung(table: String, params: List<Pair<String, String>>): JsonArray = parse {
        val req = Request.Builder().url(urlFor(table, params)).get().build()
        val body = execute(req)
        if (body.isBlank()) JsonArray() else JsonParser.parseString(body).asJsonArray
    }

    data class PageResponse(val data: JsonArray, val totalCount: Int)

    /**
     * Membaca tabel terpaginasi sekaligus mendapatkan total baris dari header Content-Range PostgREST.
     * Menggunakan Prefer: count=exact, sehingga database menghitung total baris di sisi server
     * tanpa perlu mentransfer seluruh ID atau melakukan query loop terpisah.
     */
    suspend fun selectWithCount(
        table: String,
        params: List<Pair<String, String>> = emptyList(),
    ): PageResponse = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url(urlFor(table, params))
            .header("Prefer", "count=exact")
            .get()
            .build()
        client.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw PostgrestException(resp.code, body.ifBlank { resp.message })
            val data = if (body.isBlank()) JsonArray() else JsonParser.parseString(body).asJsonArray
            val rangeHeader = resp.header("Content-Range")
            val total = rangeHeader?.substringAfterLast('/')?.trim()?.toIntOrNull() ?: data.size()
            PageResponse(data, total)
        }
    }

    /**
     * Menghitung total baris yang cocok dengan kriteria filter secara native di server.
     * Menggunakan limit=0 dan Prefer: count=exact sehingga transfer data berupa 0 baris JSON
     * dan total baris langsung didapat dari header Content-Range.
     */
    suspend fun count(
        table: String,
        params: List<Pair<String, String>> = emptyList(),
    ): Int = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url(urlFor(table, params + listOf("select" to "id", "limit" to "0")))
            .header("Prefer", "count=exact")
            .get()
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                val body = resp.body?.string().orEmpty()
                throw PostgrestException(resp.code, body.ifBlank { resp.message })
            }
            val rangeHeader = resp.header("Content-Range")
            rangeHeader?.substringAfterLast('/')?.trim()?.toIntOrNull() ?: 0
        }
    }

    suspend fun selectOne(table: String, params: List<Pair<String, String>>): JsonObject? {
        val arr = select(table, params + ("limit" to "1"))
        return if (arr.size() > 0) arr[0].asJsonObject else null
    }

    suspend fun insert(table: String, body: JsonElement, returning: Boolean = true): JsonArray = parse {
        val req = Request.Builder()
            .url(urlFor(table))
            .post(body.toString().toRequestBody(jsonMedia))
            .header("Prefer", if (returning) "return=representation" else "return=minimal")
            .build()
        val res = execute(req)
        if (res.isBlank()) JsonArray() else JsonParser.parseString(res).asJsonArray
    }

    suspend fun upsert(table: String, body: JsonElement, onConflict: String? = null, ignoreDuplicates: Boolean = false, returning: Boolean = true): JsonArray {
        val params = onConflict?.let { listOf("on_conflict" to it) } ?: emptyList()
        val prefer = buildString {
            append("resolution=")
            append(if (ignoreDuplicates) "ignore-duplicates" else "merge-duplicates")
            append(if (returning) ",return=representation" else ",return=minimal")
        }
        val req = Request.Builder()
            .url(urlFor(table, params))
            .post(body.toString().toRequestBody(jsonMedia))
            .header("Prefer", prefer)
            .build()
        val res = execute(req)
        return if (res.isBlank()) JsonArray() else JsonParser.parseString(res).asJsonArray
    }

    suspend fun update(table: String, params: List<Pair<String, String>>, patch: JsonElement): JsonArray = parse {
        val req = Request.Builder()
            .url(urlFor(table, params))
            .patch(patch.toString().toRequestBody(jsonMedia))
            .header("Prefer", "return=representation")
            .build()
        val res = execute(req)
        if (res.isBlank()) JsonArray() else JsonParser.parseString(res).asJsonArray
    }

    suspend fun delete(table: String, params: List<Pair<String, String>>) {
        val req = Request.Builder().url(urlFor(table, params)).delete().build()
        execute(req)
    }

    /** RPC — kembalikan JsonElement mentah (bisa array, object, atau scalar). */
    suspend fun rpc(fn: String, body: JsonElement = JsonObject()): JsonElement = parse {
        val req = Request.Builder()
            .url(urlFor("rpc/$fn"))
            .post(body.toString().toRequestBody(jsonMedia))
            .build()
        val res = execute(req)
        if (res.isBlank()) JsonObject() else JsonParser.parseString(res)
    }
}
