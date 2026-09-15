package com.sukashawarma.superapp.core.update

import android.app.DownloadManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.sukashawarma.superapp.core.update.model.AppUpdateDelta
import com.sukashawarma.superapp.core.update.model.AppUpdateManifest
import com.sukashawarma.superapp.core.update.model.deltaFor
import com.sukashawarma.superapp.data.remote.Postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream

/**
 * Update Manager mandiri berbasis Delta Patch (Google Archive Patcher / file-by-file patching)
 * dan Silent PackageInstaller (Android 12+).
 *
 * Manifest versi terbaru disimpan di tabel `global_settings` (key `superapp_update`).
 * Pembaruan dideteksi secara push realtime lewat WebSocket Supabase Realtime ([UpdateRealtimeManager])
 * dengan REST query via [Postgrest] sebagai jaring pengaman bila perangkat sempat offline.
 */
object AppUpdateManager {

    enum class DownloadState {
        IDLE,
        DOWNLOADING,
        READY_TO_INSTALL,
        INSTALLING,
        AWAITING_USER_ACTION,
        FAILED
    }

    enum class DownloadPayload {
        FULL_APK,
        DELTA_PATCH
    }

    private val _downloadState = MutableStateFlow(DownloadState.IDLE)
    val downloadState = _downloadState.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0)
    val downloadProgress = _downloadProgress.asStateFlow()

    private val _downloadPayload = MutableStateFlow(DownloadPayload.FULL_APK)
    val downloadPayload = _downloadPayload.asStateFlow()

    private val _downloadPayloadSizeBytes = MutableStateFlow<Long?>(null)
    val downloadPayloadSizeBytes = _downloadPayloadSizeBytes.asStateFlow()

    private val _availableUpdate = MutableStateFlow<AppUpdateManifest?>(null)
    val availableUpdate = _availableUpdate.asStateFlow()

    private val _recentlyInstalledVersion = MutableStateFlow<String?>(null)
    val recentlyInstalledVersion = _recentlyInstalledVersion.asStateFlow()

    private var downloadId: Long = -1L
    private var pendingManifest: AppUpdateManifest? = null
    private var pendingUserAction: Intent? = null
    private var pendingDelta: AppUpdateDelta? = null
    private var forceFullForVersion: Int? = null
    private var receiverRegistered = false
    @Volatile private var processingDownloadedPayload = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var appContext: Context? = null
    private val gson = Gson()

    var currentVersionCode: Int = 1
        private set
    var currentVersionName: String = "1.0.0"
        private set

    /**
     * Inisialisasi awal saat Application start.
     * Mengenali apakah aplikasi baru saja di-update dengan membaca metadata package OS.
     */
    fun initialize(context: Context) {
        val appCtx = context.applicationContext
        this.appContext = appCtx

        val packageInfo = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appCtx.packageManager.getPackageInfo(appCtx.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                appCtx.packageManager.getPackageInfo(appCtx.packageName, 0)
            }
        }.getOrNull()

        if (packageInfo != null) {
            currentVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }
            currentVersionName = packageInfo.versionName.orEmpty().ifBlank { "1.0.0" }
        }

        cleanOldInstallers(appCtx)

        val prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getInt(KEY_ACKNOWLEDGED_VERSION, 0) == currentVersionCode) return

        val isPackageUpdate = packageInfo != null &&
            packageInfo.lastUpdateTime - packageInfo.firstInstallTime > 1_000L

        if (isPackageUpdate) {
            _recentlyInstalledVersion.value = currentVersionName
        } else {
            prefs.edit().putInt(KEY_ACKNOWLEDGED_VERSION, currentVersionCode).apply()
        }
    }

    fun acknowledgeRecentInstall(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_ACKNOWLEDGED_VERSION, currentVersionCode).apply()
        _recentlyInstalledVersion.value = null
        cleanOldInstallers(context)
    }

    /**
     * Membersihkan file installer APK (.apk) dan delta patch (.fbf/.partial) lama
     * dari folder `updates/` di penyimpanan internal/eksternal aplikasi.
     *
     * File hanya dihapus jika target versinya <= versi aplikasi yang sedang aktif berjalan,
     * sehingga memori HP kru tidak membengkak ratusan megabyte oleh file instalasi usang.
     */
    fun cleanOldInstallers(context: Context) {
        try {
            val updatesDir = File(context.getExternalFilesDir(null), "updates")
            if (!updatesDir.exists() || !updatesDir.isDirectory) return
            updatesDir.listFiles()?.forEach { file ->
                val name = file.name
                if (name.endsWith(".partial")) {
                    file.delete()
                    return@forEach
                }
                val isApkOrPatch = name.endsWith(".apk") || name.endsWith(".fbf")
                if (isApkOrPatch) {
                    val targetVersion = Regex("""(\d+)\.(apk|fbf)$""").find(name)?.groupValues?.get(1)?.toIntOrNull()
                    if (targetVersion == null || targetVersion <= currentVersionCode) {
                        if (file.delete()) {
                            android.util.Log.i("AppUpdateManager", "Cleaned up old installer: $name")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("AppUpdateManager", "Failed to clean old installers", e)
        }
    }

    /** REST check idempoten ke baris key='superapp_update' di global_settings. */
    suspend fun checkForUpdate() {
        try {
            val row = Postgrest.selectOne("global_settings", listOf("key" to "eq.superapp_update")) ?: return
            val valueObj = row.getAsJsonObject("value") ?: return
            val manifest = gson.fromJson(valueObj, AppUpdateManifest::class.java) ?: return
            applyIfNewer(manifest)
        } catch (e: Exception) {
            android.util.Log.e("AppUpdateManager", "checkForUpdate failed", e)
        }
    }

    fun checkForUpdateAsync() {
        scope.launch { checkForUpdate() }
    }

    /** Dipanggil saat event postgres_changes tiba dari Supabase Realtime. */
    fun handleRealtimePayload(record: JSONObject) {
        if (record.optString("key") != "superapp_update") return
        val v = record.optJSONObject("value") ?: return
        val manifest = AppUpdateManifest(
            versionCode = v.optInt("version_code"),
            versionName = v.optString("version_name"),
            apkUrl = v.optString("apk_url"),
            apkSha256 = v.optString("apk_sha256").ifBlank { null },
            apkSizeBytes = v.optLong("apk_size_bytes").takeIf { it > 0 },
            delta = v.optJSONObject("delta")?.toAppUpdateDelta(),
            deltas = v.optJSONArray("deltas")?.let { array ->
                buildList {
                    for (index in 0 until array.length()) {
                        array.optJSONObject(index)?.toAppUpdateDelta()?.let(::add)
                    }
                }
            }.orEmpty(),
            notes = if (v.isNull("notes")) null else v.optString("notes").ifBlank { null },
            mandatory = v.optBoolean("mandatory", false)
        )
        applyIfNewer(manifest)
    }

    private fun applyIfNewer(manifest: AppUpdateManifest) {
        if (manifest.versionCode <= currentVersionCode) return

        val context = appContext
        val isDifferentRelease = pendingManifest?.versionCode != manifest.versionCode
        if (isDifferentRelease) {
            if (downloadId != -1L && context != null) {
                val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                runCatching { manager.remove(downloadId) }
            }
            downloadId = -1L
            pendingUserAction = null
            pendingDelta = null
            forceFullForVersion = null
            processingDownloadedPayload = false
            pendingManifest = manifest
            _downloadProgress.value = 0
            _downloadPayloadSizeBytes.value = null
            _downloadState.value = DownloadState.IDLE
        }

        _availableUpdate.value = manifest

        if (context != null && _downloadState.value == DownloadState.IDLE) {
            startDownload(context, manifest)
        }
    }

    fun startDownload(context: Context, manifest: AppUpdateManifest) {
        if (_downloadState.value == DownloadState.DOWNLOADING || processingDownloadedPayload) return
        pendingManifest = manifest

        val updatesDir = File(context.getExternalFilesDir(null), "updates").apply { mkdirs() }
        val destFile = File(updatesDir, "suka-superapp-${manifest.versionCode}.apk")

        if (destFile.exists() && isMarkedComplete(context, manifest.versionCode)) {
            _downloadProgress.value = 100
            _downloadState.value = DownloadState.READY_TO_INSTALL
            return
        }

        val delta = manifest.deltaFor(currentVersionCode)?.takeIf {
            it.patchUrl.startsWith("https://") &&
                it.patchSha256.length == 64 &&
                it.patchSizeBytes > 0 &&
                (manifest.apkSizeBytes == null || it.patchSizeBytes < manifest.apkSizeBytes) &&
                forceFullForVersion != manifest.versionCode &&
                !manifest.apkSha256.isNullOrBlank()
        }
        pendingDelta = delta
        val payloadFile = if (delta != null) {
            File(updatesDir, "suka-superapp-${delta.baseVersionCode}-to-${manifest.versionCode}.fbf")
        } else {
            destFile
        }
        val payloadUrl = delta?.patchUrl ?: manifest.apkUrl
        _downloadPayload.value = if (delta != null) DownloadPayload.DELTA_PATCH else DownloadPayload.FULL_APK
        _downloadPayloadSizeBytes.value = delta?.patchSizeBytes ?: manifest.apkSizeBytes

        if (destFile.exists()) destFile.delete()
        if (payloadFile.exists()) payloadFile.delete()

        _downloadState.value = DownloadState.DOWNLOADING
        _downloadProgress.value = 0
        registerReceiver(context)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(payloadUrl))
            .setTitle("Update SUKA Superapp")
            .setDescription(
                if (delta != null) "Mengunduh patch hemat ${manifest.versionName}"
                else "Mengunduh versi ${manifest.versionName}"
            )
            .addRequestHeader(
                "User-Agent",
                "SukaSuperapp-Updater/$currentVersionName"
            )
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(payloadFile))
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        downloadId = downloadManager.enqueue(request)
        pollProgress(context, downloadManager)
    }

    private fun pollProgress(context: Context, downloadManager: DownloadManager) {
        scope.launch {
            var downloading = true
            while (downloading) {
                val cursor = downloadManager.query(DownloadManager.Query().setFilterById(downloadId))
                if (cursor != null && cursor.moveToFirst()) {
                    val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    val bytesIdx = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val totalIdx = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    val status = if (statusIdx >= 0) cursor.getInt(statusIdx) else -1
                    val bytes = if (bytesIdx >= 0) cursor.getLong(bytesIdx) else 0L
                    val total = if (totalIdx >= 0) cursor.getLong(totalIdx) else -1L

                    if (total > 0) {
                        _downloadProgress.value = ((bytes * 100) / total).toInt()
                    }

                    if (status == DownloadManager.STATUS_SUCCESSFUL || status == DownloadManager.STATUS_FAILED) {
                        downloading = false
                        if (status == DownloadManager.STATUS_FAILED) {
                            _downloadState.value = DownloadState.FAILED
                        } else {
                            markDownloadReady(context)
                        }
                    }
                }
                cursor?.close()
                if (downloading) delay(1000)
            }
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (id != downloadId) return

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val cursor = downloadManager.query(DownloadManager.Query().setFilterById(id))
            val status = cursor?.use {
                if (!it.moveToFirst()) return@use null
                val statusIdx = it.getColumnIndex(DownloadManager.COLUMN_STATUS)
                if (statusIdx >= 0) it.getInt(statusIdx) else null
            }

            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                markDownloadReady(context)
            } else if (status == DownloadManager.STATUS_FAILED) {
                _downloadState.value = DownloadState.FAILED
            }
        }
    }

    private fun markDownloadReady(context: Context) {
        if (processingDownloadedPayload ||
            _downloadState.value == DownloadState.READY_TO_INSTALL ||
            _downloadState.value == DownloadState.INSTALLING ||
            _downloadState.value == DownloadState.AWAITING_USER_ACTION
        ) return
        val manifest = pendingManifest ?: return
        processingDownloadedPayload = true
        scope.launch {
            var shouldFallbackToFull = false
            try {
                val updatesDir = File(context.getExternalFilesDir(null), "updates")
                val targetApk = File(updatesDir, "suka-superapp-${manifest.versionCode}.apk")
                if (_downloadPayload.value == DownloadPayload.DELTA_PATCH) {
                    val delta = requireNotNull(pendingDelta)
                    val patchFile = File(
                        updatesDir,
                        "suka-superapp-${delta.baseVersionCode}-to-${manifest.versionCode}.fbf"
                    )
                    require(patchFile.length() == delta.patchSizeBytes) {
                        "Downloaded delta size mismatch"
                    }
                    val actualPatchHash = ApkDeltaApplier.sha256(patchFile)
                    require(actualPatchHash.equals(delta.patchSha256, ignoreCase = true)) {
                        "Downloaded delta SHA-256 mismatch"
                    }
                    ApkDeltaApplier.apply(
                        installedApk = File(context.applicationInfo.sourceDir),
                        patchFile = patchFile,
                        outputApk = targetApk,
                        expectedBaseVersion = currentVersionCode,
                        expectedTargetVersion = manifest.versionCode,
                        expectedTargetSha256 = requireNotNull(manifest.apkSha256)
                    )
                    patchFile.delete()
                } else if (!manifest.apkSha256.isNullOrBlank()) {
                    val actualApkHash = ApkDeltaApplier.sha256(targetApk)
                    require(actualApkHash.equals(manifest.apkSha256, ignoreCase = true)) {
                        "Downloaded APK SHA-256 mismatch"
                    }
                }

                markComplete(context, manifest.versionCode)
                downloadId = -1L
                _downloadProgress.value = 100
                _downloadState.value = DownloadState.READY_TO_INSTALL

                // Auto-apply update secara mandiri jika izin instalasi sudah siap. Selama izin
                // buka-otomatis masih perlu ditawarkan, pemasangan diserahkan ke UI yang
                // menunggu jawaban user — kalau tidak, installer mematikan aplikasi duluan.
                scope.launch {
                    delay(1200)
                    if (_downloadState.value == DownloadState.READY_TO_INSTALL &&
                        canRequestInstall(context) &&
                        !AppUpdateRelauncher.shouldOfferOverlayPermission(context, manifest.versionCode)
                    ) {
                        installDownloadedApk(context)
                    }
                }
            } catch (error: Exception) {
                android.util.Log.e("AppUpdateManager", "Downloaded update validation failed", error)
                shouldFallbackToFull = _downloadPayload.value == DownloadPayload.DELTA_PATCH
                if (shouldFallbackToFull) {
                    val delta = pendingDelta
                    if (delta != null) {
                        File(
                            File(context.getExternalFilesDir(null), "updates"),
                            "suka-superapp-${delta.baseVersionCode}-to-${manifest.versionCode}.fbf"
                        ).delete()
                    }
                }
                if (!shouldFallbackToFull) _downloadState.value = DownloadState.FAILED
            } finally {
                processingDownloadedPayload = false
            }

            if (shouldFallbackToFull) {
                forceFullForVersion = manifest.versionCode
                pendingDelta = null
                _downloadState.value = DownloadState.IDLE
                _downloadProgress.value = 0
                startDownload(context.applicationContext, manifest)
            }
        }
    }

    private const val PREFS_NAME = "superapp_update_prefs"
    private const val KEY_ACKNOWLEDGED_VERSION = "acknowledged_superapp_version"

    private fun markComplete(context: Context, versionCode: Int) {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean("downloaded_$versionCode", true).apply()
    }

    private fun isMarkedComplete(context: Context, versionCode: Int): Boolean {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean("downloaded_$versionCode", false)
    }

    private fun registerReceiver(context: Context) {
        if (receiverRegistered) return
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.applicationContext.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.applicationContext.registerReceiver(receiver, filter)
        }
        receiverRegistered = true
    }

    fun canRequestInstall(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else true
    }

    fun installIntentSettings(context: Context): Intent {
        return Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }

    fun installDownloadedApk(context: Context) {
        val manifest = pendingManifest ?: return
        val updatesDir = File(context.getExternalFilesDir(null), "updates")
        val apkFile = File(updatesDir, "suka-superapp-${manifest.versionCode}.apk")
        if (!apkFile.exists()) {
            _downloadState.value = DownloadState.FAILED
            return
        }

        if (!canRequestInstall(context)) {
            pendingUserAction = null
            _downloadState.value = DownloadState.AWAITING_USER_ACTION
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            installSilently(context.applicationContext, apkFile, manifest.versionCode)
        } else {
            _downloadState.value = DownloadState.AWAITING_USER_ACTION
        }
    }

    private fun installSilently(context: Context, apkFile: File, versionCode: Int) {
        if (_downloadState.value == DownloadState.INSTALLING) return
        _downloadState.value = DownloadState.INSTALLING
        pendingUserAction = null

        scope.launch {
            val packageInstaller = context.packageManager.packageInstaller
            var sessionId: Int? = null
            try {
                val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
                    setAppPackageName(context.packageName)
                    setSize(apkFile.length())
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
                    }
                }

                sessionId = packageInstaller.createSession(params)
                packageInstaller.openSession(sessionId).use { session ->
                    FileInputStream(apkFile).use { input ->
                        session.openWrite("base.apk", 0, apkFile.length()).use { output ->
                            input.copyTo(output)
                            session.fsync(output)
                        }
                    }

                    val callbackIntent = Intent(context, UpdateInstallResultReceiver::class.java).apply {
                        action = UpdateInstallResultReceiver.ACTION_INSTALL_STATUS
                    }
                    val callback = PendingIntent.getBroadcast(
                        context,
                        0,
                        callbackIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
                    )
                    AppUpdateRelauncher.markPending(context)
                    session.commit(callback.intentSender)
                }

                scope.launch {
                    delay(120_000)
                    if (_downloadState.value == DownloadState.INSTALLING) {
                        _downloadState.value = DownloadState.FAILED
                    }
                }
            } catch (error: Exception) {
                sessionId?.let { id -> runCatching { packageInstaller.abandonSession(id) } }
                error.printStackTrace()
                _downloadState.value = DownloadState.FAILED
            }
        }
    }

    fun continueInstallWithUserAction(context: Context) {
        val confirmation = pendingUserAction
        if (confirmation != null) {
            confirmation.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(confirmation)
            return
        }

        if (!canRequestInstall(context)) {
            context.startActivity(installIntentSettings(context))
            return
        }

        val manifest = pendingManifest ?: return
        val apkFile = File(File(context.getExternalFilesDir(null), "updates"), "suka-superapp-${manifest.versionCode}.apk")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            installSilently(context.applicationContext, apkFile, manifest.versionCode)
        } else {
            launchLegacyInstaller(context, apkFile)
        }
    }

    fun resumeAfterInstallPermission(context: Context) {
        if (_downloadState.value == DownloadState.AWAITING_USER_ACTION &&
            pendingUserAction == null &&
            canRequestInstall(context)
        ) {
            installDownloadedApk(context)
        }
    }

    const val ACTION_INSTALL_STATUS = "com.sukashawarma.superapp.action.UPDATE_INSTALL_STATUS"

    fun onUpdateSuccessfullyApplied() {
        _downloadState.value = DownloadState.IDLE
        _availableUpdate.value = null
        _recentlyInstalledVersion.value = currentVersionName
    }

    fun handleInstallStatus(context: Context, intent: Intent) {
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_SUCCESS -> {
                onUpdateSuccessfullyApplied()
            }
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                pendingUserAction = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                _downloadState.value = DownloadState.AWAITING_USER_ACTION
            }
            else -> {
                intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)?.let {
                    android.util.Log.e("AppUpdateManager", "Install failed: $it")
                }
                AppUpdateRelauncher.clearPending(context)
                _downloadState.value = DownloadState.FAILED
            }
        }
    }

    private fun launchLegacyInstaller(context: Context, apkFile: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        AppUpdateRelauncher.markPending(context)
        context.startActivity(intent)
    }

    fun reset() {
        _downloadState.value = DownloadState.IDLE
        _downloadProgress.value = 0
        pendingUserAction = null
        pendingDelta = null
        _downloadPayloadSizeBytes.value = null
    }

    private fun JSONObject.toAppUpdateDelta(): AppUpdateDelta? {
        val baseVersionCode = optInt("base_version_code")
        val patchUrl = optString("patch_url")
        val patchSha256 = optString("patch_sha256")
        val patchSizeBytes = optLong("patch_size_bytes")
        if (baseVersionCode <= 0 || patchUrl.isBlank() || patchSha256.isBlank() || patchSizeBytes <= 0) {
            return null
        }
        return AppUpdateDelta(baseVersionCode, patchUrl, patchSha256, patchSizeBytes)
    }
}
