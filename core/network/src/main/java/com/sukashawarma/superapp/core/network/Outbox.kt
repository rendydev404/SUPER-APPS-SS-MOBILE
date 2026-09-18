package com.sukashawarma.superapp.data.remote

import android.content.Context
import android.util.Log
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.sukashawarma.superapp.data.local.AppDatabase
import com.sukashawarma.superapp.data.local.entity.OutboxEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Antrean aksi tulis yang terjadi saat offline, plus mesin yang mengirimkannya sendiri
 * begitu sinyal kembali.
 *
 * Generalisasi dari dua antrean yang lebih dulu ada (`pending_attendance`,
 * `pending_opname_finalize`). Yang berubah bukan idenya, melainkan siapa yang menjalankannya:
 * antrean absensi hari ini hanya terkuras kalau ada orang membuka layar Clock, sehingga absen
 * yang tersimpan offline bisa menginap berjam-jam di perangkat. Di sini pengurasan dipicu
 * [NetworkMonitor] dan WorkManager, jadi tidak bergantung pada layar mana pun yang dibuka.
 *
 * Pengunggahan berkas dipasang dari layer `app` lewat [unggahLampiran] alih-alih memanggil
 * `core:storage` langsung — modul itu sudah bergantung pada `core:network`, jadi memanggilnya
 * dari sini akan melingkar. Pola hook yang sama sudah dipakai `SupabaseClient.onRefreshNeeded`
 * dan `AppSession.onSignOut`.
 */
object Outbox {
    private const val TAG = "Outbox"

    /**
     * Dilempar penangan ketika server menolak dengan alasan yang tidak akan berubah kalau
     * diulang — plafon budget terlewati, opname sudah difinalisasi orang lain, peran tidak
     * berwenang. Bedanya dengan kegagalan jaringan: yang ini tidak pernah dicoba lagi, dan
     * penggunanya harus diberi tahu.
     */
    class TolakPermanen(message: String) : Exception(message)

    /**
     * Pengirim satu jenis aksi. Melempar [TolakPermanen] bila server menolak permanen,
     * melempar galat jaringan biasa bila cuma belum sampai.
     *
     * [lampiranUrl] adalah path objek storage hasil unggahan (mis. "selfies/<outlet>/<id>.jpg"),
     * atau null bila aksi ini memang tidak berlampiran. Penangan yang menentukan ke field mana
     * path itu dipasang, karena setiap RPC menamainya berbeda.
     */
    fun interface Penangan {
        suspend fun kirim(item: OutboxEntity, payload: JsonObject, lampiranUrl: String?)
    }

    @Volatile private var db: AppDatabase? = null
    private val penangan = mutableMapOf<String, Penangan>()
    private val kunciFlush = Mutex()

    /**
     * Dipasang dari layer `app`: `Outbox.unggahLampiran = { bucket, path, bytes ->
     * StorageUtil.uploadJpeg(bucket, path, bytes) }`.
     */
    @Volatile
    var unggahLampiran: (suspend (bucket: String, path: String, bytes: ByteArray) -> String)? = null

    /** Dipanggil saat sebuah aksi menyerah. Layer `app` memakainya untuk memunculkan notifikasi. */
    @Volatile
    var onGagalPermanen: ((OutboxEntity) -> Unit)? = null

    fun init(context: Context) {
        db = AppDatabase.get(context)
    }

    fun daftarkan(jenis: String, handler: Penangan) {
        penangan[jenis] = handler
    }

    /**
     * Masukkan satu aksi ke antrean. Pemanggil sudah menganggap aksinya "tersimpan" setelah
     * ini — maka kegagalan di sini harus dilempar, bukan ditelan: lebih baik crew tahu
     * catatannya tidak tersimpan daripada mengira sudah.
     */
    suspend fun antre(item: OutboxEntity) = withContext(Dispatchers.IO) {
        val dao = db?.outboxDao() ?: error("Outbox belum di-init")
        dao.insert(item)
    }

    fun jumlahMenunggu(): Flow<Int> =
        db?.outboxDao()?.countFlow(OutboxEntity.STATUS_MENUNGGU) ?: flowOf(0)

    fun gagalPermanen(): Flow<List<OutboxEntity>> =
        db?.outboxDao()?.gagalFlow(OutboxEntity.STATUS_GAGAL_PERMANEN) ?: flowOf(emptyList())

    suspend fun hapus(id: String) {
        withContext(Dispatchers.IO) { db?.outboxDao()?.delete(id) }
    }

    /**
     * Kirim semua yang tertunda, berurutan sesuai waktu kejadian.
     *
     * Berurutan, bukan paralel: dua aksi di modul yang sama bisa saling bergantung (hitungan
     * opname lalu finalisasinya), dan mengirimnya serentak berarti finalisasi bisa tiba lebih
     * dulu daripada angkanya.
     *
     * [kunciFlush] mencegah dua pemicu — NetworkMonitor dan WorkManager — menguras antrean yang
     * sama bersamaan. Tanpa itu satu aksi bisa terkirim dua kali dalam jendela sempit antara
     * "berhasil di server" dan "barisnya dihapus", dan tidak semua aksi punya pelindung duplikat
     * di server.
     */
    suspend fun flush(): Unit = kunciFlush.withLock {
        if (!NetworkMonitor.isOnline.value) return@withLock
        val dao = db?.outboxDao() ?: return@withLock

        val antrean = withContext(Dispatchers.IO) { dao.menunggu(OutboxEntity.STATUS_MENUNGGU) }
        for (item in antrean) {
            if (!NetworkMonitor.isOnline.value) return@withLock

            val handler = penangan[item.jenis]
            if (handler == null) {
                // Terjadi bila APK diturunkan versinya sementara antreannya sudah berisi jenis
                // baru. Dibiarkan menunggu, bukan dibuang: versi berikutnya akan mengenalinya.
                Log.w(TAG, "tidak ada penangan untuk jenis '${item.jenis}', dilewati")
                continue
            }

            try {
                val lampiranUrl = unggahLampiranBila(item)
                val payload = JsonParser.parseString(item.payload).asJsonObject
                handler.kirim(item, payload, lampiranUrl)
                withContext(Dispatchers.IO) {
                    dao.delete(item.id)
                    item.lampiranPath?.let { File(it).delete() }
                }
            } catch (e: TolakPermanen) {
                menyerah(item, e.message)
            } catch (e: Throwable) {
                if (!adalahGalatJaringan(e)) {
                    menyerah(item, e.message)
                    continue
                }
                withContext(Dispatchers.IO) { dao.markFailedAttempt(item.id, e.message) }
                if (item.attemptCount + 1 >= OutboxEntity.BATAS_PERCOBAAN) {
                    menyerah(item, e.message)
                } else {
                    // Sinyal putus lagi di tengah antrean — sisanya percuma dicoba sekarang.
                    Log.i(TAG, "flush berhenti di '${item.jenis}': ${e.message}")
                    return@withLock
                }
            }
        }
    }

    /**
     * Lampiran diunggah lebih dulu karena payload menyebut path-nya.
     *
     * Path tujuannya deterministik (mengandung id outbox), jadi percobaan ulang MENIMPA berkas
     * yang sama alih-alih menumpuk berkas yatim di storage — pola yang sudah dipakai selfie
     * absensi.
     */
    private suspend fun unggahLampiranBila(item: OutboxEntity): String? {
        val path = item.lampiranPath ?: return null
        val bucket = item.lampiranBucket ?: return null
        val tujuan = item.lampiranTujuan ?: return null
        val berkas = File(path)
        if (!berkas.exists()) {
            // Cache perangkat bisa dibersihkan sistem. Aksinya tetap dikirim tanpa foto —
            // kehilangan bukti foto jauh lebih ringan daripada kehilangan catatannya.
            Log.w(TAG, "lampiran '$path' sudah tidak ada, aksi dikirim tanpa lampiran")
            return null
        }
        val unggah = unggahLampiran ?: error("Outbox.unggahLampiran belum dipasang")
        return unggah(bucket, tujuan, withContext(Dispatchers.IO) { berkas.readBytes() })
    }

    private suspend fun menyerah(item: OutboxEntity, alasan: String?) {
        Log.e(TAG, "aksi '${item.jenis}' (${item.id}) menyerah: $alasan")
        withContext(Dispatchers.IO) {
            db?.outboxDao()?.tandaiStatus(item.id, OutboxEntity.STATUS_GAGAL_PERMANEN, alasan)
        }
        onGagalPermanen?.invoke(item.copy(status = OutboxEntity.STATUS_GAGAL_PERMANEN, lastError = alasan))
    }
}
