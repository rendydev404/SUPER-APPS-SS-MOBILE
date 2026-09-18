package com.sukashawarma.superapp.core.update

import android.util.Log
import com.sukashawarma.superapp.data.remote.SupabaseClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicInteger

/**
 * Saluran WebSocket khusus ke Supabase Realtime untuk pembaruan APK Superapp.
 * Berjalan independen sejak Application start tanpa memerlukan sesi login staf,
 * sehingga rilis/update dapat langsung dideteksi bahkan saat user berada di layar login.
 */
object UpdateRealtimeManager {
    private const val TAG = "SuperappUpdateRealtime"
    private const val TOPIC = "realtime:public:native_updates"

    /** Batas langkah tunda: 3 detik << 5 = 96 detik. */
    private const val MAKS_TUNDA_LANGKAH = 5
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refs = AtomicInteger(1)
    private val lock = Any()

    private var socket: WebSocket? = null
    private var heartbeatJob: Job? = null
    private var reconnectJob: Job? = null
    private var started = false
    @Volatile private var joined = false
    @Volatile private var lastFrameAt = 0L
    @Volatile private var joinRef: String? = null

    fun start() {
        synchronized(lock) {
            if (started) return
            started = true
        }
        connect()
    }

    fun reconnectNow() {
        synchronized(lock) {
            if (!started) return
            closeSocketLocked("reconnect")
        }
        connect()
    }

    private fun connect() {
        val request: Request
        synchronized(lock) {
            if (!started || socket != null) return
            joined = false
            lastFrameAt = System.currentTimeMillis()
            val anonKey = BuildConfig.SUPABASE_ANON_KEY
            val url = SupabaseClient.BASE_URL.replace("https://", "wss://") +
                "realtime/v1/websocket?apikey=$anonKey&vsn=1.0.0"
            request = Request.Builder().url(url).build()
            socket = SupabaseClient.okHttpClient.newWebSocket(request, listener)
        }
    }

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            if (!isCurrent(webSocket)) return
            val ref = refs.getAndIncrement().toString()
            joinRef = ref
            webSocket.send(
                UpdateRealtimeProtocol.joinMessage(TOPIC, ref, BuildConfig.SUPABASE_ANON_KEY)
            )
            startHeartbeat(webSocket)
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            if (!isCurrent(webSocket)) return
            lastFrameAt = System.currentTimeMillis()
            when (val event = UpdateRealtimeProtocol.parse(text)) {
                is UpdateRealtimeEvent.JoinReply -> {
                    if (event.ref != joinRef) return
                    if (event.successful && event.hasGlobalSettingsSubscription) {
                        joined = true
                        Log.i(TAG, "Realtime updater joined and global_settings subscribed")
                        AppUpdateManager.checkForUpdateAsync()
                    } else {
                        Log.w(TAG, "Realtime updater join did not confirm global_settings")
                        failAndReconnect(webSocket)
                    }
                }
                is UpdateRealtimeEvent.GlobalSettingChanged -> {
                    if (!joined) return
                    val record = JSONObject(event.record.toString())
                    AppUpdateManager.handleRealtimePayload(record)
                }
                UpdateRealtimeEvent.ChannelFailure -> failAndReconnect(webSocket)
                UpdateRealtimeEvent.Ignore -> Unit
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            if (!isCurrent(webSocket)) return
            Log.w(TAG, "Realtime updater socket failed", t)
            // 429 = batas laju sambungan Realtime Supabase. Mencoba lagi tiga
            // detik kemudian hanya memperpanjang penolakannya — dan socket
            // pembaruan aplikasi ikut menghabiskan jatah yang dipakai chat.
            if (response?.code == 429) gagalBeruntun = MAKS_TUNDA_LANGKAH
            failAndReconnect(webSocket)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            if (!isCurrent(webSocket)) return
            failAndReconnect(webSocket)
        }
    }

    /** Jumlah kegagalan beruntun; menentukan lama tunda sambungan ulang. */
    private var gagalBeruntun = 0

    private fun startHeartbeat(webSocket: WebSocket) {
        gagalBeruntun = 0

        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive && isCurrent(webSocket)) {
                delay(25_000)
                if (!isCurrent(webSocket)) return@launch
                if (System.currentTimeMillis() - lastFrameAt > 70_000) {
                    failAndReconnect(webSocket)
                    return@launch
                }
                webSocket.send(UpdateRealtimeProtocol.heartbeatMessage(refs.getAndIncrement().toString()))
            }
        }
    }

    private fun failAndReconnect(failedSocket: WebSocket) {
        synchronized(lock) {
            if (socket !== failedSocket) return
            closeSocketLocked("channel failure")
            if (reconnectJob?.isActive == true) return
            // Tunda berlipat: 3 detik, 6, 12, ... sampai 96 detik, plus sedikit
            // acak. Versi sebelumnya selalu tiga detik — pada jaringan atau
            // kuota yang sedang menolak, itu berarti ketukan tanpa henti
            // sepanjang aplikasi hidup.
            val langkah = gagalBeruntun.coerceAtMost(MAKS_TUNDA_LANGKAH)
            gagalBeruntun = langkah + 1
            val tunda = (3_000L shl langkah) + (0..1_500).random()
            reconnectJob = scope.launch {
                delay(tunda)
                connect()
            }
        }
    }

    private fun isCurrent(candidate: WebSocket): Boolean = synchronized(lock) {
        socket === candidate
    }

    private fun closeSocketLocked(reason: String) {
        val oldSocket = socket
        socket = null
        joined = false
        joinRef = null
        heartbeatJob?.cancel()
        heartbeatJob = null
        oldSocket?.close(1000, reason)
    }
}
