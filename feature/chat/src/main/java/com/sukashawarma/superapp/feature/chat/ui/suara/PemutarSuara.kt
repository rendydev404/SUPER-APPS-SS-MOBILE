package com.sukashawarma.superapp.feature.chat.ui.suara

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.sukashawarma.superapp.data.remote.SupabaseClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File

/**
 * Pemutar voice note untuk seluruh layar chat.
 *
 * Satu objek bersama, bukan MediaPlayer per bubble: dua pesan suara yang
 * berbunyi bersamaan adalah bug yang mustahil dihindari kalau setiap bubble
 * memegang pemutarnya sendiri. Memulai pesan lain otomatis menghentikan yang
 * sedang berjalan.
 *
 * Berkasnya diunduh dulu ke cache lewat `SupabaseClient.okHttpClient`, BUKAN
 * diserahkan sebagai URL ke MediaPlayer: bucket `chat-media` privat, dan hanya
 * klien bersama itu yang membawa Authorization milik sesi aktif.
 */
object PemutarSuara {

    data class Status(
        /** Path audio yang sedang dipegang pemutar. null = tidak ada. */
        val kunci: String? = null,
        val memuat: Boolean = false,
        val berjalan: Boolean = false,
        val posisiMs: Int = 0,
        val durasiMs: Int = 0,
        val kecepatan: Float = 1f,
        val galat: String? = null,
    )

    private val _status = MutableStateFlow(Status())
    val status: StateFlow<Status> = _status

    private val _kunciAktif = MutableStateFlow<String?>(null)
    val kunciAktif: StateFlow<String?> = _kunciAktif

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var player: MediaPlayer? = null
    private var tugasDenyut: Job? = null
    private var tugasMuat: Job? = null

    /**
     * Putar (atau lanjutkan) rekaman pada [path].
     *
     * [path] adalah `audio_path` pesan — dipakai sekaligus sebagai nama berkas
     * cache, jadi rekaman yang sama tidak diunduh dua kali dalam satu sesi.
     */
    fun putar(context: Context, path: String, mulaiDariMs: Int = 0) {
        val s = _status.value
        if (s.kunci == path && player != null) {
            if (mulaiDariMs > 0) geser(mulaiDariMs)
            if (s.berjalan) {
                if (mulaiDariMs <= 0) jeda()
            } else {
                lanjut()
            }
            return
        }

        hentikan()
        _kunciAktif.value = path
        _status.value = Status(kunci = path, memuat = true, kecepatan = s.kecepatan)

        tugasMuat = scope.launch {
            val berkas = try {
                unduh(context.applicationContext, path)
            } catch (e: Exception) {
                android.util.Log.e("PemutarSuara", "gagal mengunduh voice note", e)
                _status.value = Status(galat = "Gagal memuat pesan suara.")
                return@launch
            }

            val mp = MediaPlayer()
            try {
                mp.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                mp.setDataSource(berkas.absolutePath)
                mp.prepare()
                mp.setOnCompletionListener { selesai() }
                terapkanKecepatan(mp, _status.value.kecepatan)
                // Menyentuh tengah gelombang pada rekaman yang belum diputar:
                // loncatannya dilakukan SEBELUM start, supaya tidak terdengar
                // sepersekian detik dari awal rekaman lebih dulu.
                if (mulaiDariMs > 0) runCatching { mp.seekTo(mulaiDariMs) }
                mp.start()
            } catch (e: Exception) {
                android.util.Log.e("PemutarSuara", "gagal memutar voice note", e)
                runCatching { mp.release() }
                _status.value = Status(galat = "Rekaman tidak bisa diputar.")
                return@launch
            }

            player = mp
            _status.value = _status.value.copy(
                memuat = false,
                berjalan = true,
                posisiMs = mulaiDariMs.coerceAtLeast(0),
                durasiMs = mp.duration.coerceAtLeast(0),
            )
            mulaiDenyut()
        }
    }

    fun jeda() {
        val mp = player ?: return
        runCatching { mp.pause() }
        tugasDenyut?.cancel()
        _status.value = _status.value.copy(berjalan = false)
    }

    private fun lanjut() {
        val mp = player ?: return
        runCatching {
            terapkanKecepatan(mp, _status.value.kecepatan)
            mp.start()
        }
        _status.value = _status.value.copy(berjalan = true)
        mulaiDenyut()
    }

    fun geser(ms: Int) {
        val mp = player ?: return
        val target = ms.coerceIn(0, _status.value.durasiMs)
        runCatching { mp.seekTo(target) }
        _status.value = _status.value.copy(posisiMs = target)
    }

    /** 1x → 1.5x → 2x → 1x, seperti tombol kecepatan WhatsApp. */
    fun gantiKecepatan(): Float {
        val baru = when (_status.value.kecepatan) {
            1f -> 1.5f
            1.5f -> 2f
            else -> 1f
        }
        _status.value = _status.value.copy(kecepatan = baru)
        player?.let { mp ->
            if (_status.value.berjalan) terapkanKecepatan(mp, baru)
        }
        return baru
    }

    /** Hentikan dan lepas pemutar — dipanggil saat keluar layar chat. */
    fun hentikan() {
        tugasMuat?.cancel()
        tugasDenyut?.cancel()
        player?.let { mp ->
            runCatching { mp.stop() }
            runCatching { mp.release() }
        }
        player = null
        _kunciAktif.value = null
        _status.value = Status(kecepatan = _status.value.kecepatan)
    }

    private fun selesai() {
        tugasDenyut?.cancel()
        _status.value = _status.value.copy(berjalan = false, posisiMs = _status.value.durasiMs)
    }

    private fun mulaiDenyut() {
        tugasDenyut?.cancel()
        tugasDenyut = scope.launch {
            while (true) {
                val mp = player ?: break
                val posisi = runCatching { mp.currentPosition }.getOrDefault(0)
                _status.value = _status.value.copy(posisiMs = posisi)
                delay(80)
            }
        }
    }

    private fun terapkanKecepatan(mp: MediaPlayer, kecepatan: Float) {
        runCatching {
            mp.playbackParams = mp.playbackParams.setSpeed(kecepatan)
        }
    }

    private suspend fun unduh(context: Context, path: String): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "voice-note").apply { mkdirs() }
        val berkas = File(dir, path.substringAfterLast('/').ifBlank { path.hashCode().toString() })
        if (berkas.exists() && berkas.length() > 0) return@withContext berkas

        val url = "${SupabaseClient.BASE_URL}storage/v1/object/authenticated/" +
            path.removePrefix("chat-media/").let { "chat-media/$it" }
        val req = Request.Builder().url(url).get().build()
        SupabaseClient.okHttpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw java.io.IOException("Unduh suara gagal (${resp.code})")
            }
            val isi = resp.body?.bytes() ?: throw java.io.IOException("Rekaman kosong.")
            berkas.writeBytes(isi)
        }
        berkas
    }
}
