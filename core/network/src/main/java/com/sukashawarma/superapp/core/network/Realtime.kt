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

    /**
     * Kunci internal channel broadcast di peta [wanted]/[joined]. Diberi awalan
     * supaya tidak mungkin bertabrakan dengan nama tabel postgres ("bc:" bukan
     * karakter yang sah untuk nama tabel), dan supaya [join] tahu harus memakai
     * config broadcast, bukan postgres_changes.
     */
    private const val BC_PREFIX = "bc:"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val events = MutableSharedFlow<String>(extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /** Pesan broadcast mentah: kunci channel ("bc:<nama>") -> payload {event, payload}. */
    private val broadcastEvents = MutableSharedFlow<Pair<String, JsonObject>>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val counter = AtomicInteger()

    private val lock = Any()
    private val wanted = mutableMapOf<String, Int>()
    private val joined = mutableSetOf<String>()
    /**
     * Server menolak dengan 429 (batas laju sambungan Realtime Supabase).
     *
     * Ditandai supaya percobaan berikutnya menunggu lama, bukan ikut menambah
     * ketukan pada pintu yang sedang menghitung ketukan. Tanpa ini, socket yang
     * ditolak akan dicoba lagi sedetik kemudian — dan penolakannya jadi awet
     * sendiri: realtime mati total, termasuk sinyal typing.
     */
    @Volatile
    private var kenaBatasLaju = false

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

    /**
     * Pesan broadcast dari channel bernama [channel] (mis. "chat-typing").
     *
     * Berbeda dengan [updates], aliran ini MEMBAWA payload: broadcast memang
     * ditujukan untuk sinyal ringan antar-klien (typing indicator, presence
     * sederhana) yang tidak pernah menyentuh database, jadi kekhawatiran
     * "payload realtime tidak melewati RLS" tidak berlaku — tidak ada baris
     * database yang diwakilinya. Emisinya objek `{event, payload}` mentah;
     * pemanggil memilah lewat `event`.
     *
     * Menumpang socket, mesin join, dan hitungan pemakai yang sama dengan
     * [updates] — channel di-join saat kolektor pertama datang dan ditinggal
     * saat kolektor terakhir pergi.
     */
    fun broadcasts(channel: String): Flow<JsonObject> {
        val key = BC_PREFIX + channel
        return broadcastEvents
            .onStart { retain(setOf(key)) }
            .onCompletion { release(setOf(key)) }
            .filter { it.first == key }
            .map { it.second }
    }

    /**
     * Kirim satu pesan broadcast ke [channel]. Best-effort dan sengaja lossy:
     * kalau socket sedang putus atau channel belum ter-join, pesan dibuang
     * diam-diam — sinyal semacam typing tidak berharga untuk diantre.
     * Kembalian true hanya berarti "sempat dikirim ke socket".
     */
    fun sendBroadcast(channel: String, event: String, payload: JsonObject): Boolean {
        val key = BC_PREFIX + channel
        val socket = synchronized(lock) { if (key in joined) live else null } ?: return false
        val bungkus = JsonObject().apply {
            addProperty("type", "broadcast")
            addProperty("event", event)
            add("payload", payload)
        }
        return socket.send(frame("broadcast", bungkus, topic(key)))
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
                // Naik berlipat, bukan linier, dan diberi sedikit acak supaya
                // beberapa perangkat yang putus bersamaan tidak kembali
                // mengetuk pada detik yang sama.
                val dasar = if (kenaBatasLaju) {
                    kenaBatasLaju = false
                    60_000L
                } else {
                    minOf(60_000L, 1_000L shl minOf(backoff, 6))
                }
                delay(dasar + (0..1_500).random())
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
                        "postgres_changes" -> {
                            val topic = data.get("topic")?.asString
                            val tableFromTopic = when {
                                topic == null -> null
                                topic.startsWith("realtime:native-bc-") -> null
                                topic.startsWith("realtime:native-") -> topic.removePrefix("realtime:native-")
                                else -> null
                            }
                            val tableFromPayload = data.getAsJsonObject("payload")?.getAsJsonObject("data")?.get("table")?.asString
                                ?: data.getAsJsonObject("payload")?.get("table")?.asString
                            val table = tableFromTopic ?: tableFromPayload ?: ANY
                            events.tryEmit(table)
                        }
                        "broadcast" -> {
                            // topic "realtime:native-bc-<nama>" -> kunci "bc:<nama>"
                            val nama = data.get("topic")?.asString?.removePrefix("realtime:native-bc-")
                            val payload = data.getAsJsonObject("payload")
                            if (nama != null && payload != null) {
                                broadcastEvents.tryEmit(BC_PREFIX + nama to payload)
                            }
                        }
                        "phx_error", "phx_close" -> {
                            // Kunci internal dipulihkan dari topic dengan aturan
                            // yang SAMA seperti cabang "broadcast" di atas:
                            // "realtime:native-bc-<nama>" -> "bc:<nama>".
                            //
                            // Sebelumnya awalannya disusun ulang dengan menempel
                            // BC_PREFIX ke nama yang sudah membawa "bc-", jadi
                            // kuncinya menjadi "bc:bc-<nama>" — tidak pernah cocok
                            // dengan apa pun. Akibatnya channel broadcast tidak
                            // pernah di-join ulang setelah error, sementara
                            // namanya tetap tertinggal di `joined`; `sendBroadcast`
                            // lalu merasa channel-nya hidup dan membuang sinyal
                            // typing ke channel yang sudah mati, tanpa satu pun
                            // gejala di layar.
                            val topic = data.get("topic")?.asString
                            val kunci = when {
                                topic == null -> null
                                topic.startsWith("realtime:native-bc-") ->
                                    BC_PREFIX + topic.removePrefix("realtime:native-bc-")
                                topic.startsWith("realtime:native-") ->
                                    topic.removePrefix("realtime:native-")
                                else -> null
                            }
                            when {
                                // Hanya kegagalan pada socket itu sendiri yang
                                // menjatuhkan sambungan.
                                topic == null || topic == "phoenix" -> close(mine, webSocket)

                                kunci != null -> synchronized(lock) {
                                    // Dilepas lebih dulu, apa pun hasilnya: channel
                                    // yang ditutup server tidak boleh dianggap hidup.
                                    joined.remove(kunci)
                                    if (kunci in wanted) join(kunci, webSocket)
                                }

                                // Topik asing — misalnya balasan atas frame
                                // `access_token` yang dikirim ke "realtime:any",
                                // yang memang tidak pernah di-join. Menutup socket
                                // karenanya akan memutus seluruh realtime setiap
                                // satu denyut jantung.
                                else -> Unit
                            }
                        }
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    if (response?.code == 429) kenaBatasLaju = true
                    close(mine, webSocket)
                }
                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) = close(mine, webSocket)
            },
        )
        try {
            while (true) {
                delay(HEARTBEAT_MS)
                val current = synchronized(lock) { if (generation != mine) null else live } ?: break
                current.send(frame("heartbeat", JsonObject(), "phoenix"))
                val token = SessionTokenHolder.accessToken
                if (!token.isNullOrBlank()) {
                    current.send(frame("access_token", tokenPayload(), "realtime:any"))
                    synchronized(lock) { joined.toList() }.forEach { table ->
                        current.send(frame("access_token", tokenPayload(), topic(table)))
                    }
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
        // Channel broadcast tidak mendaftar postgres_changes: tidak ada tabel
        // yang diwakilinya, dan server menolak join yang menyebut tabel fiktif.
        val config = if (table.startsWith(BC_PREFIX)) {
            """{"broadcast":{"self":false},"presence":{"key":""}}"""
        } else {
            """{"broadcast":{"self":false},"presence":{"key":""},"postgres_changes":[{"event":"*","schema":"public","table":"$table"}]}"""
        }
        val payload = JsonObject().apply {
            add("config", JsonParser.parseString(config))
            val token = SessionTokenHolder.accessToken
            if (!token.isNullOrBlank()) {
                addProperty("access_token", token)
            }
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

    /** "bc:chat-typing" -> "realtime:native-bc-chat-typing"; nama tabel tetap seperti semula. */
    private fun topic(table: String) =
        if (table.startsWith(BC_PREFIX)) "realtime:native-bc-${table.removePrefix(BC_PREFIX)}"
        else "realtime:native-$table"

    private fun tokenPayload() = JsonObject().apply {
        val token = SessionTokenHolder.accessToken
        if (!token.isNullOrBlank()) {
            addProperty("access_token", token)
        }
    }

    private fun frame(event: String, payload: JsonObject, topic: String) = JsonObject().apply {
        addProperty("topic", topic)
        addProperty("event", event)
        add("payload", payload)
        addProperty("ref", counter.incrementAndGet().toString())
    }.toString()
}
