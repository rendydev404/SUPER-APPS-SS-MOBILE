package com.sukashawarma.superapp.feature.chat.ui.suara

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.platform.LocalContext
import java.io.File

/** Jumlah bilah waveform yang disimpan bersama pesan. */
const val JUMLAH_BILAH_SUARA = 48

/** Rekaman lebih panjang dari ini dipotong otomatis, seperti batas praktis WhatsApp. */
const val DURASI_REKAM_MAKS_MS = 5 * 60 * 1000

/** Rekaman lebih pendek dari ini dianggap salah tekan dan dibuang. */
const val DURASI_REKAM_MIN_MS = 700

/**
 * Perekam suara satu-berkas untuk voice note chat.
 *
 * Merekam ke cache aplikasi sebagai M4A (AAC 32 kbps mono, 24 kHz) — format yang
 * sama dengan yang dipakai WhatsApp untuk pesan suara: satu menit bicara kurang
 * dari 250 KB, jadi ringan diunggah dari jaringan outlet.
 *
 * Amplitudo dicuplik selama merekam dan diringkas jadi [JUMLAH_BILAH_SUARA] digit
 * '0'-'9'. Bentuk gelombang di bubble penerima karenanya adalah suara yang
 * sebenarnya, bukan hiasan acak, dan penerima tidak perlu mengunduh berkasnya
 * lebih dulu hanya untuk menggambar.
 */
class PerekamSuara(private val context: Context) {

    data class Hasil(val berkas: File, val durasiMs: Int, val wave: String)

    private var recorder: MediaRecorder? = null
    private var berkas: File? = null
    /** Awal segmen yang sedang berjalan; berubah setiap kali rekaman dilanjutkan. */
    private var mulaiSegmenMs: Long = 0L
    /** Total durasi segmen-segmen yang sudah selesai (sebelum jeda terakhir). */
    private var akumulasiMs: Int = 0
    private val sampel = mutableListOf<Int>()

    /** Cuplikan amplitudo terakhir (0..9) untuk animasi saat merekam. */
    val level: SnapshotStateList<Int> = mutableStateListOf()

    var sedangMerekam by mutableStateOf(false)
        private set

    /** Rekaman ditahan sementara; mikrofon berhenti tapi berkasnya tetap hidup. */
    var terjeda by mutableStateOf(false)
        private set

    var durasiMs by mutableIntStateOf(0)
        private set

    /** @return true bila perekaman benar-benar dimulai. */
    fun mulai(): Boolean {
        if (sedangMerekam) return true
        val target = File(context.cacheDir, "vn-${System.currentTimeMillis()}.m4a")
        val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        return try {
            mr.setAudioSource(MediaRecorder.AudioSource.MIC)
            mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mr.setAudioChannels(1)
            mr.setAudioSamplingRate(24_000)
            mr.setAudioEncodingBitRate(32_000)
            mr.setOutputFile(target.absolutePath)
            mr.prepare()
            mr.start()
            recorder = mr
            berkas = target
            mulaiSegmenMs = System.currentTimeMillis()
            akumulasiMs = 0
            terjeda = false
            durasiMs = 0
            sampel.clear()
            level.clear()
            sedangMerekam = true
            true
        } catch (e: Exception) {
            android.util.Log.e("PerekamSuara", "gagal mulai merekam", e)
            runCatching { mr.release() }
            target.delete()
            recorder = null
            berkas = null
            sedangMerekam = false
            terjeda = false
            false
        }
    }

    /**
     * Tahan rekaman tanpa menutup berkasnya — seperti tombol jeda WhatsApp saat
     * perekaman terkunci. Bagian yang sudah direkam tetap utuh dan bisa
     * dilanjutkan; yang berhenti hanya mikrofonnya.
     */
    fun jeda() {
        val mr = recorder ?: return
        if (terjeda) return
        val berhasil = runCatching { mr.pause() }.isSuccess
        if (!berhasil) return
        akumulasiMs += (System.currentTimeMillis() - mulaiSegmenMs).toInt()
        terjeda = true
        durasiMs = akumulasiMs
    }

    /** Lanjutkan rekaman yang sedang dijeda. */
    fun lanjut() {
        val mr = recorder ?: return
        if (!terjeda) return
        val berhasil = runCatching { mr.resume() }.isSuccess
        if (!berhasil) return
        mulaiSegmenMs = System.currentTimeMillis()
        terjeda = false
    }

    /** Dipanggil berkala dari UI: mencatat amplitudo dan memperbarui durasi. */
    fun denyut() {
        val mr = recorder ?: return
        // Saat dijeda, durasi dan waveform ikut membeku: menambah bilah senyap
        // akan membuat gelombangnya berisi jeda yang tidak pernah terekam.
        if (terjeda) return
        durasiMs = akumulasiMs + (System.currentTimeMillis() - mulaiSegmenMs).toInt()
        val amp = runCatching { mr.maxAmplitude }.getOrDefault(0)
        val skala = amplitudoKeDigit(amp)
        sampel += skala
        level += skala
        if (level.size > 40) level.removeAt(0)
    }

    /**
     * Hentikan dan simpan. Mengembalikan null bila rekaman gagal atau terlalu
     * pendek — dan berkasnya ikut dibuang, bukan ditinggal menumpuk di cache.
     */
    fun selesai(): Hasil? {
        val mr = recorder ?: return null
        val target = berkas
        val lama = if (terjeda) akumulasiMs
        else akumulasiMs + (System.currentTimeMillis() - mulaiSegmenMs).toInt()
        recorder = null
        berkas = null
        sedangMerekam = false
        terjeda = false

        val berhasil = runCatching { mr.stop() }.isSuccess
        runCatching { mr.release() }

        if (!berhasil || target == null || !target.exists() || lama < DURASI_REKAM_MIN_MS) {
            target?.delete()
            return null
        }
        return Hasil(target, lama, ringkasWave(sampel))
    }

    /** Batal: rekaman dihentikan dan berkasnya dihapus. */
    fun batal() {
        val mr = recorder ?: return
        val target = berkas
        recorder = null
        berkas = null
        sedangMerekam = false
        terjeda = false
        runCatching { mr.stop() }
        runCatching { mr.release() }
        target?.delete()
    }
}

@Composable
fun rememberPerekamSuara(): PerekamSuara {
    val context = LocalContext.current
    val perekam = remember { PerekamSuara(context) }
    // Keluar layar saat masih merekam tidak boleh meninggalkan mikrofon menyala.
    DisposableEffect(perekam) {
        onDispose { perekam.batal() }
    }
    return perekam
}

/** MediaRecorder memberi amplitudo 0..32767; telinga membacanya logaritmik. */
internal fun amplitudoKeDigit(amp: Int): Int {
    if (amp <= 0) return 0
    val db = 20.0 * kotlin.math.log10(amp.toDouble() / 32767.0)
    // -45 dB ke atas dipetakan ke 0..9; di bawah itu praktis senyap.
    val norm = ((db + 45.0) / 45.0).coerceIn(0.0, 1.0)
    return (norm * 9).toInt()
}

/** Ringkas seluruh cuplikan jadi [JUMLAH_BILAH_SUARA] digit, rata-rata per blok. */
internal fun ringkasWave(sampel: List<Int>): String {
    if (sampel.isEmpty()) return ""
    val hasil = StringBuilder(JUMLAH_BILAH_SUARA)
    val lebar = sampel.size.toDouble() / JUMLAH_BILAH_SUARA
    for (i in 0 until JUMLAH_BILAH_SUARA) {
        val dari = (i * lebar).toInt()
        val sampai = ((i + 1) * lebar).toInt().coerceAtLeast(dari + 1).coerceAtMost(sampel.size)
        if (dari >= sampel.size) break
        val rata = sampel.subList(dari, sampai).average()
        hasil.append(rata.toInt().coerceIn(0, 9))
    }
    return hasil.toString()
}

/** Ubah string wave jadi tinggi bilah 0f..1f. Kosong = bilah rata sedang. */
fun waveKeTinggi(wave: String?, jumlah: Int = JUMLAH_BILAH_SUARA): List<Float> {
    val bersih = wave?.filter { it.isDigit() }.orEmpty()
    if (bersih.isEmpty()) return List(jumlah) { 0.45f }
    return List(jumlah) { i ->
        val idx = (i.toDouble() / jumlah * bersih.length).toInt().coerceIn(0, bersih.length - 1)
        // Minimal 0.12 supaya bagian senyap tetap terlihat sebagai garis, bukan hilang.
        ((bersih[idx] - '0') / 9f).coerceIn(0.12f, 1f)
    }
}

/** "0:07" / "1:23" — format durasi voice note. */
fun formatDurasiSuara(ms: Int): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "${total / 60}:${(total % 60).toString().padStart(2, '0')}"
}
