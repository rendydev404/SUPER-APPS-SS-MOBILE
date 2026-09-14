package com.sukashawarma.superapp.core.update.model

import com.google.gson.annotations.SerializedName

/**
 * Row `global_settings` key='superapp_update' — dipakai AppUpdateManager untuk cek
 * versi APK terbaru. Distribusi app ini mandiri (bukan Play Store),
 * jadi update dicek & dipasang sendiri dari dalam aplikasi. `value` di kolom
 * JSONB, jadi Gson bisa langsung memetakannya ke AppUpdateManifest.
 */
data class AppUpdateSettingDto(
    val key: String,
    val value: AppUpdateManifest?
)

data class AppUpdateManifest(
    @SerializedName("version_code") val versionCode: Int,
    @SerializedName("version_name") val versionName: String,
    @SerializedName("apk_url") val apkUrl: String,
    @SerializedName("apk_sha256") val apkSha256: String? = null,
    @SerializedName("apk_size_bytes") val apkSizeBytes: Long? = null,
    val delta: AppUpdateDelta? = null,
    val deltas: List<AppUpdateDelta> = emptyList(),
    val notes: String? = null,
    val mandatory: Boolean = false
)

fun AppUpdateManifest.deltaFor(baseVersionCode: Int): AppUpdateDelta? =
    deltas.firstOrNull { it.baseVersionCode == baseVersionCode }
        ?: delta?.takeIf { it.baseVersionCode == baseVersionCode }

data class AppUpdateDelta(
    @SerializedName("base_version_code") val baseVersionCode: Int,
    @SerializedName("patch_url") val patchUrl: String,
    @SerializedName("patch_sha256") val patchSha256: String,
    @SerializedName("patch_size_bytes") val patchSizeBytes: Long
)
