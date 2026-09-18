package com.sukashawarma.superapp.notif

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Pengelola efek getar berkelanjutan dan suara nada dering telepon untuk push notifikasi
 * darurat Petty Cash.
 *
 * Perilaku siklus:
 * 1. Aktif bergetar berirama + dering telepon selama 30 detik.
 * 2. Mati/jeda hening selama 2 detik.
 * 3. Kembali aktif 30 detik, jeda 2 detik, dan seterusnya berulang terus.
 * 4. Berhenti total hanya ketika notifikasi diklik atau pengguna membuka halaman Petty Cash.
 */
object PettyCashAlarmManager {

    private const val TAG = "PettyCashAlarm"

    const val EXTRA_STOP_ALARM = "stop_petty_cash_alarm"
    const val NOTIF_ID_PETTY_CASH = 9110

    @Volatile
    private var sedangAktif = false

    private val alarmScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var alarmJob: Job? = null

    private var vibrator: Vibrator? = null
    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null

    /**
     * Memulai siklus getaran berkelanjutan (30s hidup / 2s mati) dan nada dering telepon.
     * [onCycleRefresh] dipanggil pada setiap awal fase aktif untuk memastikan notifikasi
     * tetap segar dan berada di atas layar.
     */
    @Synchronized
    fun mulaiAlarm(context: Context, onCycleRefresh: (() -> Unit)? = null) {
        if (sedangAktif) {
            Log.d(TAG, "Alarm petty cash sudah berjalan aktif.")
            return
        }
        sedangAktif = true
        Log.i(TAG, "Memulai continuous alarm & getar petty cash (30s on / 2s off)...")

        try {
            // 1. Dapatkan WakeLock agar siklus tetap bekerja meski layar HP mati
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (powerManager != null && wakeLock == null) {
                wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "suka:PettyCashAlarmWakeLock"
                ).apply {
                    setReferenceCounted(false)
                    // Batas pengaman sistem 10 menit agar tidak menahan baterai selamanya
                    // jika device ditinggal tanpa tindakan sama sekali
                    acquire(10 * 60 * 1000L)
                }
            }

            // 2. Inisialisasi Vibrator
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            // 3. Jalankan siklus 30s aktif / 2s hening
            alarmJob?.cancel()
            alarmJob = alarmScope.launch {
                while (isActive && sedangAktif) {
                    Log.d(TAG, "Petty Cash Alarm: Fase aktif 30 detik (getar + dering telepon)")

                    // Nyalakan getar
                    mulaiGetaran()

                    // Nyalakan suara telepon bawaan
                    mulaiSuaraTelepon(context)

                    // Segarkan banner jika diperlukan
                    try {
                        onCycleRefresh?.invoke()
                    } catch (e: Exception) {
                        Log.w(TAG, "Gagal memanggil onCycleRefresh: ${e.message}")
                    }

                    // Berjalan selama 30 detik
                    delay(30_000L)
                    if (!isActive || !sedangAktif) break

                    Log.d(TAG, "Petty Cash Alarm: Fase jeda hening 2 detik")
                    hentikanGetaran()
                    hentikanSuaraSementara()

                    // Jeda mati selama 2 detik
                    delay(2_000L)
                    if (!isActive || !sedangAktif) break
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Gagal memulai getar/alarm petty cash", e)
        }
    }

    private fun mulaiGetaran() {
        try {
            vibrator?.let { vib ->
                if (vib.hasVibrator()) {
                    // Ritme panggilan telepon: jeda 0ms, getar 1000ms, jeda 500ms
                    val pattern = longArrayOf(0, 1000, 500)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val effect = VibrationEffect.createWaveform(pattern, 0)
                        val audioAttrs = AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                            .build()
                        @Suppress("DEPRECATION")
                        vib.vibrate(effect, audioAttrs)
                    } else {
                        @Suppress("DEPRECATION")
                        vib.vibrate(pattern, 0)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Gagal memulai vibrator: ${e.message}")
        }
    }

    private fun hentikanGetaran() {
        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.w(TAG, "Gagal membatalkan vibrator: ${e.message}")
        }
    }

    private fun mulaiSuaraTelepon(context: Context) {
        try {
            if (mediaPlayer == null) {
                // Gunakan nada dering telepon bawaan perangkat pengguna
                val soundUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    ?: return

                mediaPlayer = MediaPlayer().apply {
                    setDataSource(context, soundUri)
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    isLooping = true
                    prepare()
                }
            }
            mediaPlayer?.let { player ->
                if (!player.isPlaying) {
                    player.start()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Gagal memutar nada dering telepon: ${e.message}")
            try {
                mediaPlayer?.release()
                mediaPlayer = null
            } catch (_: Exception) {}
        }
    }

    private fun hentikanSuaraSementara() {
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.pause()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Gagal pause mediaPlayer: ${e.message}")
            try {
                mediaPlayer?.release()
                mediaPlayer = null
            } catch (_: Exception) {}
        }
    }

    private fun hentikanSuaraTotal() {
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            Log.w(TAG, "Gagal menghentikan total mediaPlayer: ${e.message}")
            mediaPlayer = null
        }
    }

    /**
     * Mematikan getaran dan alarm seketika secara total.
     * Dipanggil saat notifikasi diklik, tombol heningkan ditekan, atau pengguna membuka halaman petty cash.
     */
    @Synchronized
    fun hentikan(context: Context? = null) {
        if (!sedangAktif && vibrator == null && mediaPlayer == null && alarmJob == null) {
            return
        }
        sedangAktif = false
        Log.i(TAG, "Menghentikan total getar dan alarm petty cash.")

        try {
            alarmJob?.cancel()
            alarmJob = null
        } catch (e: Exception) {
            Log.w(TAG, "Gagal membatalkan alarmJob: ${e.message}")
        }

        hentikanGetaran()
        hentikanSuaraTotal()

        try {
            wakeLock?.let {
                if (it.isHeld) it.release()
            }
            wakeLock = null
        } catch (e: Exception) {
            Log.w(TAG, "Gagal melepas wakeLock: ${e.message}")
        }

        // Tutup notifikasi dari status bar / overlay
        context?.let { ctx ->
            try {
                NotificationManagerCompat.from(ctx).cancel(NOTIF_ID_PETTY_CASH)
            } catch (_: Exception) {}
        }
    }
}
