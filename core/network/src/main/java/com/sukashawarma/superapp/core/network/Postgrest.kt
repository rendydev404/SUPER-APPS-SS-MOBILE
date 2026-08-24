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

    suspend fun select(table: String, params: List<Pair<String, String>> = emptyList()): JsonArray {
        val req = Request.Builder().url(urlFor(table, params)).get().build()
        val body = execute(req)
        return if (body.isBlank()) JsonArray() else JsonParser.parseString(body).asJsonArray
    }

    suspend fun selectOne(table: String, params: List<Pair<String, String>>): JsonObject? {
        val arr = select(table, params + ("limit" to "1"))
        return if (arr.size() > 0) arr[0].asJsonObject else null
    }

    suspend fun insert(table: String, body: JsonElement, returning: Boolean = true): JsonArray {
        val req = Request.Builder()
            .url(urlFor(table))
            .post(body.toString().toRequestBody(jsonMedia))
            .header("Prefer", if (returning) "return=representation" else "return=minimal")
            .build()
        val res = execute(req)
        return if (res.isBlank()) JsonArray() else JsonParser.parseString(res).asJsonArray
    }

    suspend fun upsert(table: String, body: JsonElement, onConflict: String? = null, ignoreDuplicates: Boolean = false): JsonArray {
        val params = onConflict?.let { listOf("on_conflict" to it) } ?: emptyList()
        val prefer = buildString {
            append("resolution=")
            append(if (ignoreDuplicates) "ignore-duplicates" else "merge-duplicates")
            append(",return=representation")
        }
        val req = Request.Builder()
            .url(urlFor(table, params))
            .post(body.toString().toRequestBody(jsonMedia))
            .header("Prefer", prefer)
            .build()
        val res = execute(req)
        return if (res.isBlank()) JsonArray() else JsonParser.parseString(res).asJsonArray
    }

    suspend fun update(table: String, params: List<Pair<String, String>>, patch: JsonElement): JsonArray {
        val req = Request.Builder()
            .url(urlFor(table, params))
            .patch(patch.toString().toRequestBody(jsonMedia))
            .header("Prefer", "return=representation")
            .build()
        val res = execute(req)
        return if (res.isBlank()) JsonArray() else JsonParser.parseString(res).asJsonArray
    }

    suspend fun delete(table: String, params: List<Pair<String, String>>) {
        val req = Request.Builder().url(urlFor(table, params)).delete().build()
        execute(req)
    }

    /** RPC — kembalikan JsonElement mentah (bisa array, object, atau scalar). */
    suspend fun rpc(fn: String, body: JsonElement = JsonObject()): JsonElement {
        val req = Request.Builder()
            .url(urlFor("rpc/$fn"))
            .post(body.toString().toRequestBody(jsonMedia))
            .build()
        val res = execute(req)
        return if (res.isBlank()) JsonObject() else JsonParser.parseString(res)
    }
}
