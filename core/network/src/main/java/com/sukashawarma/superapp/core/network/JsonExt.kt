package com.sukashawarma.superapp.data.remote

import com.google.gson.JsonObject

/**
 * Helper baca-aman JsonObject (Gson) — dipakai lintas ~40 layar yang membaca
 * respons Postgrest, jadi satu tempat, bukan private-per-file berulang.
 */
fun JsonObject.optString(key: String): String? =
    get(key)?.takeIf { !it.isJsonNull }?.asString

fun JsonObject.optBoolean(key: String, default: Boolean = false): Boolean =
    get(key)?.takeIf { !it.isJsonNull }?.asBoolean ?: default

fun JsonObject.optDouble(key: String): Double? =
    get(key)?.takeIf { !it.isJsonNull }?.asDouble

fun JsonObject.optInt(key: String): Int? =
    get(key)?.takeIf { !it.isJsonNull }?.asInt

fun JsonObject.optJsonObject(key: String): JsonObject? =
    get(key)?.takeIf { it.isJsonObject }?.asJsonObject

fun JsonObject.optJsonArray(key: String): com.google.gson.JsonArray? =
    get(key)?.takeIf { it.isJsonArray }?.asJsonArray
