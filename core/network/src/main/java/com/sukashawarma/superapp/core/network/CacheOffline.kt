package com.sukashawarma.superapp.data.remote

import android.content.Context
import android.util.Log
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.sukashawarma.superapp.data.local.AppDatabase
import com.sukashawarma.superapp.data.local.entity.CacheEntryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Hasil sebuah pembacaan, berikut asal-usulnya.
 *
 * [dariCache] dan [fetchedAtMs] ikut dibawa sampai ke UI dengan sengaja: data lama yang
 * ditampilkan tanpa keterangan lebih berbahaya daripada layar kosong — crew bisa mengambil
 * keputusan stok berdasarkan angka kemarin tanpa tahu itu angka kemarin.
 */
data class HasilCache<T>(
    val data: T,
    val fetchedAtMs: Long,
    val dariCache: Boolean,
) {
    fun <R> map(ubah: (T) -> R): HasilCache<R> = HasilCache(ubah(data), fetchedAtMs, dariCache)
}

/**
 * Cache baca sebagai JARING PENGAMAN, bukan sumber kebenaran.
 *
 * Setiap pembacaan tetap memanggil server lebih dulu; cache hanya menyala kalau panggilan itu
 * gagal karena jaringan. Konsekuensinya disengaja: saat online aplikasi tidak menjadi lebih
 * cepat sedikit pun, dan tidak ada satu pun layar yang bisa menampilkan data basi selama
 * sinyalnya masih ada. Itu harga yang dibayar agar mode offline tidak pernah diam-diam
 * menggantikan mode normal.
 *
 * Yang disimpan adalah JSON mentah dari PostgREST, bukan model domain. Dengan begitu tidak ada
 * serializer per fitur yang harus dirawat, dan bentuk yang dibaca dari cache persis sama dengan
 * yang dibaca dari jaringan — pemetaan ke model berjalan sesudahnya lewat [HasilCache.map],
 * satu jalur untuk keduanya.
 */
object CacheOffline {
    private const val TAG = "CacheOffline"

    /** Selaras dengan batas sesi offline: melewati ini, perangkat memang harus online dulu. */
    const val UMUR_MAKS_BAWAAN_MS = 7L * 24 * 60 * 60 * 1000

    /**
     * Balasan yang lebih besar dari ini tidak disalin.
     *
     * Batasnya bukan soal ruang disk melainkan soal apa yang masuk akal ditawarkan saat
     * offline: balasan sebesar ini datang dari layar laporan/analitik, dan angka laporan
     * yang basi tidak bisa dibedakan dari yang benar oleh siapa pun yang melihatnya.
     */
    private const val BATAS_SIMPAN_KARAKTER = 512 * 1024

    /**
     * Jumlah entri yang dipertahankan. Cukup untuk seluruh layar operasional yang wajar
     * dibuka dalam satu hari, dan tidak membiarkan query bertimestamp menumpuk tanpa batas.
     */
    private const val BATAS_ENTRI = 400

    /** Pemangkasan tidak perlu jalan di setiap penulisan; tiap 50 penulisan sudah cukup. */
    private const val PANGKAS_TIAP = 50
    private val penulisanSejakPangkas = java.util.concurrent.atomic.AtomicInteger(0)

    @Volatile private var db: AppDatabase? = null

    fun init(context: Context) {
        db = AppDatabase.get(context)
    }

    /** Baca daftar baris (`Postgrest.select`). */
    suspend fun bacaArray(
        kunci: String,
        scope: String? = null,
        umurMaksMs: Long = UMUR_MAKS_BAWAAN_MS,
        ambil: suspend () -> JsonArray,
    ): HasilCache<JsonArray> = baca(kunci, scope, umurMaksMs, { it }, { it.asJsonArray }, ambil)

    /**
     * Baca satu baris (`Postgrest.selectOne`).
     *
     * `null` DISIMPAN sebagai JSON null, bukan dianggap "tidak ada cache". "Baris ini memang
     * tidak ada" adalah jawaban yang sah dan berguna saat offline — mis. outlet belum punya
     * config absensi sendiri, sehingga kode pemanggil jatuh ke global_settings alih-alih
     * menunggu jaringan yang tidak akan datang.
     */
    suspend fun bacaObjek(
        kunci: String,
        scope: String? = null,
        umurMaksMs: Long = UMUR_MAKS_BAWAAN_MS,
        ambil: suspend () -> JsonObject?,
    ): HasilCache<JsonObject?> = baca(
        kunci, scope, umurMaksMs,
        keJson = { it ?: JsonNull.INSTANCE },
        dariJson = { if (it.isJsonNull) null else it.asJsonObject },
        ambil = ambil,
    )

    /** Baca hasil RPC apa pun. */
    suspend fun bacaElemen(
        kunci: String,
        scope: String? = null,
        umurMaksMs: Long = UMUR_MAKS_BAWAAN_MS,
        ambil: suspend () -> JsonElement,
    ): HasilCache<JsonElement> = baca(kunci, scope, umurMaksMs, { it }, { it }, ambil)

    private suspend fun <T> baca(
        kunci: String,
        scope: String?,
        umurMaksMs: Long,
        keJson: (T) -> JsonElement,
        dariJson: (JsonElement) -> T,
        ambil: suspend () -> T,
    ): HasilCache<T> {
        val hasil = try {
            ambil()
        } catch (e: Throwable) {
            // Penolakan server (403, 404, validasi) TIDAK boleh jatuh ke cache: menyajikan
            // data lama di situ berarti menyembunyikan penolakan yang perlu dilihat pengguna.
            if (!adalahGalatJaringan(e)) throw e
            val tersimpan = ambilDariCache(kunci)
                ?: throw e // Offline dan belum pernah berhasil sekali pun — tidak ada yang bisa ditawarkan.
            if (System.currentTimeMillis() - tersimpan.fetchedAtMs > umurMaksMs) throw e
            Log.i(TAG, "offline, memakai cache '$kunci' (umur ${System.currentTimeMillis() - tersimpan.fetchedAtMs} ms)")
            return HasilCache(dariJson(JsonParser.parseString(tersimpan.payload)), tersimpan.fetchedAtMs, dariCache = true)
        }

        val sekarang = System.currentTimeMillis()
        simpanKeCache(kunci, keJson(hasil).toString(), sekarang, scope)
        return HasilCache(hasil, sekarang, dariCache = false)
    }

    private suspend fun ambilDariCache(kunci: String): CacheEntryEntity? = withContext(Dispatchers.IO) {
        try {
            db?.cacheEntryDao()?.ambil(kunci)
        } catch (e: Exception) {
            Log.e(TAG, "gagal membaca cache '$kunci'", e)
            null
        }
    }

    /**
     * Kegagalan menulis cache tidak boleh menggagalkan pembacaan yang sudah berhasil —
     * datanya sudah di tangan pemanggil; cache cuma bonus untuk nanti.
     */
    private suspend fun simpanKeCache(kunci: String, payload: String, waktuMs: Long, scope: String?) =
        withContext(Dispatchers.IO) {
            if (payload.length > BATAS_SIMPAN_KARAKTER) {
                Log.i(TAG, "'$kunci' terlalu besar (${payload.length}), tidak disalin ke cache")
                return@withContext
            }
            try {
                val dao = db?.cacheEntryDao() ?: return@withContext
                dao.simpan(CacheEntryEntity(kunci, payload, waktuMs, scope))
                // compareAndSet, bukan sekadar increment lalu reset: dua penulisan yang
                // berbarengan tidak boleh sama-sama merasa gilirannya memangkas.
                val hitungan = penulisanSejakPangkas.incrementAndGet()
                if (hitungan >= PANGKAS_TIAP && penulisanSejakPangkas.compareAndSet(hitungan, 0)) {
                    dao.pangkas(BATAS_ENTRI)
                }
            } catch (e: Exception) {
                Log.e(TAG, "gagal menyimpan cache '$kunci'", e)
            }
        }

    /** Dipanggil saat staf pindah outlet — angka outlet lama tidak boleh terbawa. */
    suspend fun bersihkanSelainScope(scope: String) = withContext(Dispatchers.IO) {
        try {
            db?.cacheEntryDao()?.hapusSelainScope(scope)
        } catch (e: Exception) {
            Log.e(TAG, "gagal membersihkan cache di luar scope '$scope'", e)
        }
    }

    /** Dipanggil saat logout. Cache adalah data perusahaan, bukan milik perangkat. */
    suspend fun bersihkanSemua() = withContext(Dispatchers.IO) {
        try {
            db?.cacheEntryDao()?.hapusSemua()
        } catch (e: Exception) {
            Log.e(TAG, "gagal membersihkan cache", e)
        }
    }
}
