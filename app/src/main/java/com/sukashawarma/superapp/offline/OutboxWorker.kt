package com.sukashawarma.superapp.offline

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.sukashawarma.superapp.data.remote.Outbox
import java.util.concurrent.TimeUnit

/**
 * Menguras [Outbox] saat aplikasi TIDAK sedang dibuka.
 *
 * Selama proses hidup, pengurasan sudah dipicu `NetworkMonitor.isOnline`. Yang tidak
 * tercakup di situ justru kasus yang paling sering terjadi di lapangan: crew absen saat
 * sinyal mati, menutup aplikasi, lalu sinyal kembali sejam kemudian tanpa ada yang membuka
 * aplikasi lagi sampai besok pagi. Tanpa worker ini, absennya baru terkirim besok — dan
 * meski jamnya tetap benar ([OutboxEntity.tsClientIso]), atasannya tidak melihat apa pun
 * sepanjang hari itu.
 *
 * Dijadwalkan sebagai pekerjaan sekali jalan dengan `ExistingWorkPolicy.KEEP`, bukan periodik:
 * periodik minimal 15 menit dan tetap jalan meski antrean kosong, sedangkan yang dibutuhkan
 * cuma "coba lagi begitu ada jaringan".
 */
class OutboxWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            Outbox.flush()
            Result.success()
        } catch (e: Exception) {
            // Backoff WorkManager yang menentukan kapan dicoba lagi; kegagalan per-aksi
            // sudah dicatat sendiri oleh Outbox lewat attemptCount.
            Result.retry()
        }
    }

    companion object {
        private const val NAMA_KERJA = "outbox-sync"

        fun jadwalkan(context: Context) {
            val permintaan = OneTimeWorkRequestBuilder<OutboxWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context.applicationContext)
                .enqueueUniqueWork(NAMA_KERJA, ExistingWorkPolicy.KEEP, permintaan)
        }
    }
}
