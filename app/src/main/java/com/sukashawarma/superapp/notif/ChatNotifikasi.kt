package com.sukashawarma.superapp.notif

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.sukashawarma.superapp.R
import com.sukashawarma.superapp.data.remote.SupabaseClient
import com.sukashawarma.superapp.presentation.MainActivity
import okhttp3.Request

/**
 * Notifikasi khusus chat: gaya percakapan, bisa dibalas langsung dari bilah
 * notifikasi seperti WhatsApp, dan berbunyi memakai nada notifikasi bawaan
 * perangkat pengguna.
 *
 * SALURAN SENDIRI, bukan saluran "Pemberitahuan" umum yang dipakai kabar
 * operasional (persetujuan, waste, pesan dari owner). Dipisah karena keduanya
 * beda sifat: kabar operasional datang sesekali dan boleh menginterupsi, chat
 * datang sering dan pengguna harus bisa mengatur — atau membisukan — nadanya
 * sendiri tanpa ikut membisukan hal yang penting.
 *
 * MessagingStyle dipakai supaya sistem menumpuk beberapa pesan dalam satu
 * notifikasi percakapan (dan, di Android 11+, memenuhi syarat munculnya
 * gelembung percakapan) alih-alih menumpuk baris terpisah yang saling menimpa.
 */
object ChatNotifikasi {

    const val SALURAN = "suka_chat_pesan"

    /**
     * Satu id tetap: semua pesan chat menempati notifikasi percakapan yang sama.
     *
     * ID INI HARUS UNIK SE-APLIKASI. Nilai sebelumnya, 4711, ternyata sama
     * dengan `LocationTrackingService.NOTIF_ID`; keduanya lalu saling menimpa,
     * dan notifikasi chat bahkan ikut mewarisi flag ONGOING/FOREGROUND_SERVICE
     * milik layanan lokasi sehingga tidak bisa ditutup dan tidak berbunyi.
     *
     * Yang sudah dipakai: 4210 AbsenReminder, 4711 layanan lokasi.
     */
    const val ID_NOTIF = 5171

    /** Id pintasan percakapan; menautkan notifikasi ke identitas grup. */
    private const val ID_PINTASAN = "chat_tim"

    const val KUNCI_BALASAN = "balasan_chat"
    const val AKSI_BALAS = "com.sukashawarma.superapp.BALAS_CHAT"

    /** Riwayat singkat untuk MessagingStyle. Hanya di memori — notifikasi tidak
     *  perlu bertahan melewati matinya proses, dan isinya toh berumur 24 jam. */
    private val riwayat = ArrayDeque<Triple<String, String, Long>>()
    private const val MAKS_RIWAYAT = 6

    fun siapkanSaluran(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manajer = context.getSystemService(NotificationManager::class.java) ?: return
        if (manajer.getNotificationChannel(SALURAN) != null) return
        val saluran = NotificationChannel(
            SALURAN,
            "Pesan Chat",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Pesan baru di ruang Chat Tim."
            // Nada bawaan perangkat, bukan berkas suara milik aplikasi: yang
            // pengguna kenali sebagai 'ada pesan' adalah nada pilihannya sendiri.
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build(),
            )
            enableVibration(true)
            setShowBadge(true)
        }
        manajer.createNotificationChannel(saluran)
    }

    /** Nama grup terakhir yang dipakai, supaya balasan dari notifikasi tetap
     *  memakai judul percakapan yang sama. */
    @Volatile
    private var namaGrupTerakhir: String = "Chat Tim"

    /** Foto grup yang sudah diunduh, agar tidak diambil ulang tiap pesan. */
    @Volatile
    private var fotoGrupPathTerakhir: String? = null

    @Volatile
    private var fotoGrupBitmap: Bitmap? = null

    /** Menampilkan pesan baru, menumpuknya ke percakapan yang sedang tampil. */
    fun tampilkan(
        context: Context,
        pengirim: String,
        isi: String,
        namaGrup: String,
        fotoGrupPath: String? = null,
    ) {
        siapkanSaluran(context)
        namaGrupTerakhir = namaGrup

        synchronized(riwayat) {
            riwayat.addLast(Triple(pengirim, isi, System.currentTimeMillis()))
            while (riwayat.size > MAKS_RIWAYAT) riwayat.removeFirst()
        }

        val foto = fotoGrup(context, fotoGrupPath)
        pastikanPintasan(context, namaGrup, foto)

        val notif = bangun(context, namaGrup, foto)
            .setOnlyAlertOnce(false)
            .addAction(aksiTandaiDibaca(context))
            .build()

        if (!bolehTampil(context)) return
        NotificationManagerCompat.from(context).notify(ID_NOTIF, notif)
    }

    /** Kerangka notifikasi percakapan, dipakai jalur pesan masuk maupun balasan. */
    private fun bangun(
        context: Context,
        namaGrup: String,
        foto: Bitmap?,
    ): NotificationCompat.Builder {
        val aku = Person.Builder().setName("Anda").setKey("aku").build()
        val gaya = NotificationCompat.MessagingStyle(aku)
            .setConversationTitle(namaGrup)
            .setGroupConversation(true)
        synchronized(riwayat) {
            riwayat.forEach { (nama, teks, waktu) ->
                // Pesan sendiri ditandai dengan pengirim null: itu cara
                // MessagingStyle membedakan "saya" dari lawan bicara.
                val orang = if (nama == "Anda") null
                else Person.Builder().setName(nama).setKey(nama).build()
                gaya.addMessage(teks, waktu, orang)
            }
        }

        return NotificationCompat.Builder(context, SALURAN)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setLargeIcon(foto)
            .setStyle(gaya)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            // Menautkan notifikasi ke pintasan percakapan: Android 11+ akan
            // menaruhnya di bagian "Percakapan", lengkap dengan foto grup, dan
            // membuka pintu gelembung mengambang bila pengguna mengizinkannya.
            .setShortcutId(ID_PINTASAN)
            .setContentIntent(intentBuka(context))
            .addAction(aksiBalas(context))
    }

    /**
     * Foto grup untuk ikon besar notifikasi.
     *
     * Bucket `avatars` bersifat privat, jadi pengunduhannya memakai klien
     * OkHttp aplikasi yang sudah menyisipkan token sesi. Tanpa sesi hidup
     * (proses baru dibangunkan push) pengunduhan gagal dan notifikasinya
     * tampil tanpa foto — itu diterima, karena notifikasi tidak boleh menunggu
     * jaringan hanya demi sebuah lingkaran kecil.
     */
    private fun fotoGrup(context: Context, path: String?): Bitmap? {
        if (path.isNullOrBlank()) return null
        if (path == fotoGrupPathTerakhir && fotoGrupBitmap != null) return fotoGrupBitmap

        return try {
            val objek = path.removePrefix("avatars/")
            val url = "${SupabaseClient.BASE_URL}storage/v1/object/authenticated/avatars/$objek"
            val permintaan = Request.Builder().url(url).get().build()
            SupabaseClient.okHttpClient.newCall(permintaan).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val bytes = resp.body?.bytes() ?: return null
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.also {
                    fotoGrupBitmap = it
                    fotoGrupPathTerakhir = path
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("ChatNotifikasi", "foto grup gagal dimuat: ${e.message}")
            null
        }
    }

    /**
     * Pintasan percakapan yang menjadi identitas grup di mata sistem.
     *
     * Wajib `setLongLived`: tanpa itu Android membuang pintasannya dan
     * notifikasi kehilangan status percakapannya begitu daftar pintasan
     * disegarkan.
     */
    private fun pastikanPintasan(context: Context, namaGrup: String, foto: Bitmap?) {
        try {
            val orangGrup = Person.Builder()
                .setName(namaGrup)
                .setKey(ID_PINTASAN)
                .apply { if (foto != null) setIcon(IconCompat.createWithAdaptiveBitmap(foto)) }
                .build()

            val pintasan = ShortcutInfoCompat.Builder(context, ID_PINTASAN)
                .setShortLabel(namaGrup)
                .setLongLived(true)
                .setIntent(
                    Intent(context, MainActivity::class.java)
                        .setAction(Intent.ACTION_VIEW)
                        .putExtra(NotifikasiTujuan.EXTRA_RUTE, NotifikasiTujuan.CHAT)
                )
                .setPerson(orangGrup)
                .apply {
                    if (foto != null) setIcon(IconCompat.createWithAdaptiveBitmap(foto))
                    else setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
                }
                .setCategories(setOf("android.shortcut.conversation"))
                .build()

            ShortcutManagerCompat.pushDynamicShortcut(context, pintasan)
        } catch (e: Exception) {
            // Pintasan hanyalah penyempurna tampilan; kegagalannya tidak boleh
            // membuat notifikasinya sendiri batal muncul.
            android.util.Log.w("ChatNotifikasi", "pintasan percakapan gagal: ${e.message}")
        }
    }

    /**
     * Menyusun ulang notifikasi setelah balasan terkirim, dengan balasan itu
     * ikut tampil. Wajib: notifikasi yang punya RemoteInput akan terus
     * memperlihatkan pemintal "mengirim…" sampai diperbarui atau ditutup.
     */
    fun tampilkanBalasanTerkirim(context: Context, teks: String) {
        synchronized(riwayat) {
            riwayat.addLast(Triple("Anda", teks, System.currentTimeMillis()))
            while (riwayat.size > MAKS_RIWAYAT) riwayat.removeFirst()
        }
        val notif = bangun(context, namaGrupTerakhir, fotoGrupBitmap)
            // Balasan sendiri tidak perlu berbunyi lagi.
            .setSilent(true)
            .build()

        if (!bolehTampil(context)) return
        NotificationManagerCompat.from(context).notify(ID_NOTIF, notif)
    }

    /**
     * Penjaga izin, dengan JEJAK LOG.
     *
     * Versi sebelumnya hanya `return` diam-diam saat izin tidak ada. Akibatnya
     * notifikasi yang hilang tidak meninggalkan bekas apa pun di Logcat, dan
     * penelusurannya berakhir menebak-nebak ke arah server padahal masalahnya
     * ada di perangkat. Satu baris peringatan ini yang membedakan.
     */
    private fun bolehTampil(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        val punya = ActivityCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!punya) {
            android.util.Log.w(
                "ChatNotifikasi",
                "Notifikasi chat dibatalkan: izin POST_NOTIFICATIONS belum diberikan.",
            )
        }
        return punya
    }

    fun tutup(context: Context) {
        synchronized(riwayat) { riwayat.clear() }
        NotificationManagerCompat.from(context).cancel(ID_NOTIF)
    }

    /** Pesan galat singkat di notifikasi yang sama — dipakai saat balasan gagal. */
    fun tampilkanGagal(context: Context, alasan: String) {
        siapkanSaluran(context)
        val notif = NotificationCompat.Builder(context, SALURAN)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Balasan gagal terkirim")
            .setContentText(alasan)
            .setStyle(NotificationCompat.BigTextStyle().bigText(alasan))
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setSilent(true)
            .setContentIntent(intentBuka(context))
            .build()
        if (!bolehTampil(context)) return
        NotificationManagerCompat.from(context).notify(ID_NOTIF, notif)
    }

    private fun intentBuka(context: Context): PendingIntent {
        val tujuan = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(NotifikasiTujuan.EXTRA_RUTE, NotifikasiTujuan.CHAT)
        return PendingIntent.getActivity(
            context,
            NotifikasiTujuan.CHAT.hashCode(),
            tujuan,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun aksiBalas(context: Context): NotificationCompat.Action {
        val masukan = RemoteInput.Builder(KUNCI_BALASAN)
            .setLabel("Balas…")
            .build()

        // MUTABLE wajib: sistem menyisipkan teks balasan ke dalam Intent ini.
        // PendingIntent immutable akan membuat balasannya selalu kosong.
        val niat = Intent(context, BalasChatReceiver::class.java).setAction(AKSI_BALAS)
        val tertunda = PendingIntent.getBroadcast(
            context,
            1,
            niat,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )

        return NotificationCompat.Action.Builder(
            R.mipmap.ic_launcher, "Balas", tertunda,
        )
            .addRemoteInput(masukan)
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
            .setShowsUserInterface(false)
            .build()
    }

    private fun aksiTandaiDibaca(context: Context): NotificationCompat.Action {
        val niat = Intent(context, BalasChatReceiver::class.java)
            .setAction(BalasChatReceiver.AKSI_TANDAI_DIBACA)
        val tertunda = PendingIntent.getBroadcast(
            context,
            2,
            niat,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Action.Builder(
            R.mipmap.ic_launcher, "Tandai dibaca", tertunda,
        )
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ)
            .setShowsUserInterface(false)
            .build()
    }
}
