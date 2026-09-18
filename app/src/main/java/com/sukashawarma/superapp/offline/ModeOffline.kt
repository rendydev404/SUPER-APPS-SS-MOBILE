package com.sukashawarma.superapp.offline

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.sukashawarma.superapp.R
import com.sukashawarma.superapp.core.storage.StorageUtil
import com.sukashawarma.superapp.data.local.AppDatabase
import com.sukashawarma.superapp.data.local.entity.OutboxEntity
import com.sukashawarma.superapp.data.remote.CacheOffline
import com.sukashawarma.superapp.data.remote.NetworkMonitor
import com.sukashawarma.superapp.data.remote.Outbox
import com.sukashawarma.superapp.domain.session.AppSession
import com.sukashawarma.superapp.feature.absensi.offline.CutiOffline
import com.sukashawarma.superapp.feature.absensi.offline.KasbonOffline
import com.sukashawarma.superapp.feature.stok.offline.PermintaanOffline
import com.sukashawarma.superapp.feature.stok.offline.WasteOffline
import com.sukashawarma.superapp.notif.SuperappMessagingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

/**
 * Menyalakan mode offline dari satu tempat: cache baca, antrean tulis, pengunggah lampiran,
 * pemicu sinkronisasi, dan pemberitahuan saat sebuah aksi menyerah.
 *
 * Dipasang di layer `app` karena di sinilah semua modul bertemu. `core:network` tidak boleh
 * memanggil `core:storage` (modul itu justru bergantung padanya) dan tidak boleh mengenal
 * notifikasi — sama seperti `AppSession.onSignOut` yang dipasang dari sini alih-alih membuat
 * `core:roles` mengenal pelacakan lokasi.
 */
object ModeOffline {

    fun pasang(context: Context, lingkup: CoroutineScope) {
        val app = context.applicationContext
        CacheOffline.init(app)
        Outbox.init(app)

        // Semua lampiran antrean adalah foto bukti (selfie absen, waste, terima barang),
        // jadi satu jalur JPEG cukup. uploadJpeg memakai upsert, yang memang diperlukan:
        // path tujuannya deterministik supaya percobaan ulang menimpa, bukan menumpuk.
        Outbox.unggahLampiran = { bucket, path, bytes -> StorageUtil.uploadJpeg(bucket, path, bytes) }

        Outbox.onGagalPermanen = { item -> beritahuGagal(app, item) }
        Outbox.onAntreBaru = { OutboxWorker.jadwalkan(app) }

        daftarkanPenangan()

        picuSinkronSaatOnline(app, lingkup)
    }

    /**
     * Daftar tunggal semua aksi yang boleh menunggu di perangkat.
     *
     * Didaftarkan di sini, sekali, saat Application start — bukan saat layarnya dibuka.
     * [OutboxWorker] bisa berjalan tanpa ada satu layar pun yang pernah tampil, dan aksi yang
     * penangannya belum terdaftar akan dilewati terus sampai ada yang kebetulan membuka layar
     * yang tepat.
     *
     * Satu berkas yang memuat semuanya juga membuat pertanyaan "apa saja yang bisa offline"
     * punya satu jawaban yang bisa dibaca, bukan tersebar di enam modul.
     */
    private fun daftarkanPenangan() {
        // `item.id` ADALAH kunci idempotensinya: id baris antrean dibuat sekali saat aksinya
        // terjadi dan tidak pernah berubah, jadi berapa kali pun kiriman ini diulang, server
        // menerima kunci yang sama dan mengenali kiriman kedua sebagai yang itu-itu juga.
        Outbox.daftarkan(KasbonOffline.JENIS) { item, payload, url -> KasbonOffline.kirim(item.id, payload, url) }
        Outbox.daftarkan(CutiOffline.JENIS) { item, payload, url -> CutiOffline.kirim(item.id, payload, url) }
        Outbox.daftarkan(WasteOffline.JENIS) { item, payload, url -> WasteOffline.kirim(item.id, payload, url) }
        Outbox.daftarkan(PermintaanOffline.JENIS) { item, payload, url -> PermintaanOffline.kirim(item.id, payload, url) }
    }

    /**
     * Dua pemicu yang saling menutupi lubang masing-masing:
     * - selama proses hidup, perubahan [NetworkMonitor.isOnline] langsung menguras antrean;
     * - [OutboxWorker] menangani kasus proses sudah mati sebelum sinyal kembali.
     *
     * `drop(1)` membuang nilai awal StateFlow: saat aplikasi dibuka dalam keadaan online,
     * pengurasan sudah dijadwalkan lewat worker di bawah, jadi tidak perlu dua kali.
     */
    private fun picuSinkronSaatOnline(app: Context, lingkup: CoroutineScope) {
        OutboxWorker.jadwalkan(app)
        lingkup.launch {
            NetworkMonitor.isOnline
                .drop(1)
                .filter { it }
                .collect {
                    // Urutannya penting: antrean dikirim memakai token sesi, dan sesi yang
                    // sedang berjalan dari snapshot belum punya token yang sah. Memulihkan
                    // sesi lebih dulu membuat seluruh antrean tidak langsung kena 401.
                    AppSession.pulihkanSesiSaatOnline()
                    Outbox.flush()
                }
        }
    }

    /**
     * Dipanggil saat logout. Cache dan descriptor wajah adalah data perusahaan yang kebetulan
     * menumpang di perangkat, jadi ikut pergi bersama sesinya.
     *
     * [Outbox] SENGAJA tidak ikut dibersihkan: isinya kerja yang sudah dilakukan orang dan
     * belum ada di mana pun selain perangkat ini. Menghapusnya saat logout berarti menghapus
     * absen atau catatan waste seseorang hanya karena ia keluar dari akun.
     */
    fun bersihkanDataOffline(app: Context, lingkup: CoroutineScope) {
        lingkup.launch {
            CacheOffline.bersihkanSemua()
            AppDatabase.get(app).faceDescriptorDao().hapusSemua()
        }
    }

    /**
     * Aksi yang menyerah TIDAK boleh hilang diam-diam.
     *
     * Antrean absensi yang lama membuang kiriman setelah lima percobaan tanpa memberi tahu
     * siapa pun — crew tetap melihat "(Tersimpan offline)" di layar padahal absennya tidak
     * pernah sampai. Notifikasi ini menutup celah itu: barisnya juga tetap ada di tabel
     * `outbox` dengan status GAGAL_PERMANEN supaya bisa ditinjau.
     */
    private fun beritahuGagal(app: Context, item: OutboxEntity) {
        // Izinnya baru ada sejak Android 13. Di bawah itu checkSelfPermission justru menjawab
        // DENIED untuk izin yang tidak dikenal platform, yang kalau dipercaya akan membungkam
        // notifikasi di semua perangkat lama.
        val butuhIzin = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
        if (butuhIzin &&
            ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        SuperappMessagingService.siapkanSaluran(app)
        val notifikasi = NotificationCompat.Builder(app, SuperappMessagingService.SALURAN)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Gagal disinkronkan: ${item.jenis}")
            .setContentText(item.lastError ?: "Data yang tersimpan offline ditolak server.")
            .setStyle(NotificationCompat.BigTextStyle().bigText(item.lastError.orEmpty()))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        app.getSystemService(NotificationManager::class.java)
            ?.notify(item.id.hashCode(), notifikasi)
    }
}
