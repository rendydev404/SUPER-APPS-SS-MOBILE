package com.sukashawarma.superapp.data.remote

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.sukashawarma.superapp.core.network.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.atomic.AtomicInteger

/**
 * Satu koneksi Realtime untuk seluruh aplikasi.
 *
 * Sebelumnya tiap layar yang butuh pembaruan langsung membuka websocket-nya
 * sendiri. Dengan tiga modul yang semuanya perlu realtime, itu berarti belasan
 * socket paralel di satu perangkat — mahal di baterai dan gampang menabrak batas
 * koneksi. Di sini socketnya satu, dan tiap tabel jadi satu channel Phoenix yang
 * dihitung pemakainya: channel di-join saat layar pertama membutuhkannya dan
 * ditinggalkan saat layar terakhir pergi, jadi perangkat tidak menerima aliran
 * `ledger_stok` sepanjang hari hanya karena pernah membuka layar stok.
 *
 * Emisi sengaja tidak membawa isi baris. Payload realtime tidak melewati RLS
 * dengan aturan yang sama seperti query biasa, dan menyusun ulang state dari
 * potongan event adalah sumber bug yang sudah terbukti di web. Jadi flow ini
 * hanya berkata "ada yang berubah" dan layar memuat ulang lewat jalur baca
 * normalnya — satu sumber kebenaran, bukan dua.
 */
object Realtime {

    /** Penanda yang lolos semua filter — dipakai saat sambungan pulih. */
    private const val ANY = "*"

    private const val HEARTBEAT_MS = 25_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val events = MutableSharedFlow<String>(extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val counter = AtomicInteger()

    private val lock = Any()
    private val wanted = mutableMapOf<String, Int>()
    private val joined = mutableSetOf<String>()
    private var live: WebSocket? = null
    private var generation = 0
    private var pump: Job? = null

    /**
     * Mengalir setiap kali salah satu [tables] berubah di server, plus sekali
     * setiap kali sambungan (kembali) terbuka supaya perubahan yang terlewat saat
     * jaringan putus ikut terkejar.
     *
     * Debounce 250 ms menyatukan ledakan event: satu pesanan kasir menulis
     * belasan baris `ledger_stok` sekaligus, dan layar tidak perlu memuat ulang
     * belasan kali untuk itu.
     */
    @OptIn(FlowPreview::class)
    fun updates(vararg tables: String): Flow<Unit> {
        val keys = tables.toSet()
        return events
            .onStart { retain(keys) }
            .onCompletion { release(keys) }
            .filter { it == ANY || it in keys }
            .debounce(250)
            .map { }
    }

    private fun retain(tables: Set<String>) {
        val (socket, pending) = synchronized(lock) {
            tables.forEach { wanted[it] = (wanted[it] ?: 0) + 1 }
            startPumpLocked()
            live to (tables - joined)
        }
        socket?.let { open -> pending.forEach { join(it, open) } }
    }

    private fun release(tables: Set<String>) {
        val (socket, gone, shutdown) = synchronized(lock) {
            val dropped = mutableSetOf<String>()
            tables.forEach { table ->
                val left = (wanted[table] ?: 0) - 1
                if (left <= 0) { wanted.remove(table); dropped += table } else wanted[table] = left
            }
            if (wanted.isEmpty()) {
                val closing = live
                pump?.cancel(); pump = null; live = null; joined.clear(); generation += 1
                Triple(closing, emptySet<String>(), true)
            } else {
                Triple(live, dropped.toSet(), false)
            }
        }
        if (shutdown) { socket?.cancel(); return }
        socket?.let { open -> gone.forEach { leave(it, open) } }
    }

    private fun startPumpLocked() {
        if (pump != null) return
        pump = scope.launch {
            var backoff = 0
            while (isActive) {
                val opened = runConnection()
                if (synchronized(lock) { wanted.isEmpty() }) break
                backoff = if (opened) 0 else backoff + 1
                delay(minOf(30_000L, 1_000L * (backoff + 1)))
            }
        }
    }

    /** Hidup selama satu sambungan; kembali saat sambungan itu tutup. */
    private suspend fun runConnection(): Boolean {
        val mine = synchronized(lock) { generation += 1; generation }
        var everOpen = false
        val url = BuildConfig.SUPABASE_URL.replace("https://", "wss://") +
            "/realtime/v1/websocket?apikey=${BuildConfig.SUPABASE_ANON_KEY}&vsn=1.0.0"
        val socket = SupabaseClient.okHttpClient.newWebSocket(
            Request.Builder().url(url).build(),
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    everOpen = true
                    val pending = synchronized(lock) {
                        if (generation != mine) return
                        live = webSocket
                        joined.clear()
                        wanted.keys.toList()
                    }
                    pending.forEach { join(it, webSocket) }
                    events.tryEmit(ANY)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    val data = try { JsonParser.parseString(text).asJsonObject } catch (_: Exception) { return }
                    when (data.get("event")?.asString) {
                        "postgres_changes" -> events.tryEmit(
                            data.getAsJsonObject("payload")?.getAsJsonObject("data")?.get("table")?.asString ?: ANY
                        )
                        "phx_error", "phx_close" -> close(mine, webSocket)
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) = close(mine, webSocket)
                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) = close(mine, webSocket)
            },
        )
        try {
            while (true) {
                delay(HEARTBEAT_MS)
                val current = synchronized(lock) { if (generation != mine) null else live } ?: break
                current.send(frame("heartbeat", JsonObject(), "phoenix"))
                // Token berumur pendek; channel yang tidak diperbarui ditolak diam-diam
                // oleh server begitu token kedaluwarsa.
                synchronized(lock) { joined.toList() }.forEach { table ->
                    current.send(frame("access_token", tokenPayload(), topic(table)))
                }
            }
        } finally {
            close(mine, socket)
            socket.cancel()
        }
        return everOpen
    }

    private fun close(mine: Int, closed: WebSocket) {
        synchronized(lock) {
            if (generation != mine) return
            if (live !== closed && live != null) return
            live = null
            joined.clear()
        }
    }

    private fun join(table: String, socket: WebSocket) {
        synchronized(lock) {
            if (table in joined || table !in wanted) return
            joined += table
        }
        val payload = JsonObject().apply {
            add("config", JsonParser.parseString(
                """{"broadcast":{"self":false},"presence":{"key":""},"postgres_changes":[{"event":"*","schema":"public","table":"$table"}]}"""
            ))
            addProperty("access_token", SessionTokenHolder.accessToken)
        }
        socket.send(frame("phx_join", payload, topic(table)))
    }

    private fun leave(table: String, socket: WebSocket) {
        synchronized(lock) {
            if (table !in joined) return
            joined -= table
        }
        socket.send(frame("phx_leave", JsonObject(), topic(table)))
    }

    private fun topic(table: String) = "realtime:native-$table"

    private fun tokenPayload() = JsonObject().apply { addProperty("access_token", SessionTokenHolder.accessToken) }

    private fun frame(event: String, payload: JsonObject, topic: String) = JsonObject().apply {
        addProperty("topic", topic)
        addProperty("event", event)
        add("payload", payload)
        addProperty("ref", counter.incrementAndGet().toString())
    }.toString()
}
