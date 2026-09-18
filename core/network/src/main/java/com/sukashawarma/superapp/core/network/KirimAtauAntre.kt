package com.sukashawarma.superapp.data.remote

import android.util.Log
import com.google.gson.JsonObject
import com.sukashawarma.superapp.data.local.entity.OutboxEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.util.UUID

/** Apa yang sebenarnya terjadi pada sebuah aksi tulis, supaya UI bisa jujur soal itu. */
enum class HasilAksi {
    /** Sudah sampai di server dan diterima. */
    TERKIRIM,

    /** Tersimpan di perangkat, akan dikirim sendiri begitu ada sinyal. */
    MASUK_ANTREAN,
}

/**
 * Lampiran foto bukti yang harus diunggah sebelum payload dikirim.
 *
 * [berkasLokal] sudah harus ada di disk saat fungsi ini dipanggil — bukan ByteArray di
 * memori. Aksi yang mengantre bisa menunggu berjam-jam melewati proses yang dimatikan
 * sistem, dan byte di memori tidak selamat dari itu.
 */
data class LampiranOutbox(
    val berkasLokal: File,
    val bucket: String,
    /** Path tujuan di dalam bucket. Harus deterministik agar percobaan ulang menimpa. */
    val tujuan: String,
)

/**
 * Kirim sekarang kalau bisa, antre kalau tidak.
 *
 * [kirim] adalah SATU-SATUNYA tempat logika pengiriman aksi ini ditulis: fungsi yang sama
 * dipakai di sini untuk jalur online dan didaftarkan ke [Outbox] untuk jalur antrean. Kalau
 * keduanya ditulis terpisah, perbedaan sekecil apa pun di antaranya baru ketahuan berhari-
 * hari kemudian, pada kiriman yang sudah telanjur berbeda isinya dari yang dilihat crew.
 *
 * [clientOpId] dikirim ke server sebagai kunci idempotensi dan tetap sama di setiap
 * percobaan — itu yang membuat kiriman ulang tidak jadi baris dobel.
 *
 * Penolakan server (validasi, RLS, plafon) dilempar apa adanya, TIDAK diantre: mengantre
 * sesuatu yang sudah ditolak hanya menunda kabar buruknya, dan crew kehilangan kesempatan
 * memperbaiki isian selagi masih di depan layar.
 */
suspend fun kirimAtauAntre(
    jenis: String,
    payload: JsonObject,
    kirim: suspend (clientOpId: String, payload: JsonObject, lampiranUrl: String?) -> Unit,
    clientOpId: String = UUID.randomUUID().toString(),
    outletId: String? = null,
    dibuatOleh: String? = null,
    lampiran: LampiranOutbox? = null,
): HasilAksi {
    val tsClient = Instant.now().toString()

    if (NetworkMonitor.isOnline.value) {
        try {
            val url = unggah(lampiran)
            kirim(clientOpId, payload, url)
            lampiran?.berkasLokal?.delete()
            return HasilAksi.TERKIRIM
        } catch (e: Throwable) {
            if (!adalahGalatJaringan(e)) throw e
            Log.i("KirimAtauAntre", "'$jenis' gagal karena jaringan, masuk antrean", e)
        }
    }

    Outbox.antre(
        OutboxEntity(
            id = clientOpId,
            jenis = jenis,
            payload = payload.toString(),
            tsClientIso = tsClient,
            createdAtMs = System.currentTimeMillis(),
            outletId = outletId,
            dibuatOleh = dibuatOleh,
            lampiranPath = lampiran?.berkasLokal?.absolutePath,
            lampiranBucket = lampiran?.bucket,
            lampiranTujuan = lampiran?.tujuan,
        )
    )
    return HasilAksi.MASUK_ANTREAN
}

/**
 * Menelan penolakan "kunci sudah dipakai" sebagai keberhasilan.
 *
 * Skenarionya nyata dan tidak jarang: kiriman sudah tercatat di server, lalu sinyal putus
 * sebelum jawabannya sampai. Klien menganggapnya gagal dan mengantrekan ulang dengan
 * `client_op_id` yang sama, dan server menolaknya lewat indeks unik. Tanpa pembungkus ini,
 * penolakan itu dihitung sebagai kegagalan permanen — pengguna diberi tahu kasbonnya gagal
 * padahal justru sudah masuk.
 *
 * Yang ditelan hanya pelanggaran keunikan (409). Penolakan lain tetap dilempar.
 */
suspend fun abaikanDuplikat(blok: suspend () -> Unit) {
    try {
        blok()
    } catch (e: Postgrest.PostgrestException) {
        val duplikat = e.code == 409 && e.message.orEmpty().contains("duplicate key", ignoreCase = true)
        if (!duplikat) throw e
        Log.i("KirimAtauAntre", "kiriman ini ternyata sudah ada di server, dianggap berhasil")
    }
}

/**
 * Memakai hook [Outbox.unggahLampiran] yang sama dengan jalur antrean, bukan memanggil
 * `core:storage` langsung — modul itu bergantung pada modul ini, jadi arah panggilannya
 * akan melingkar.
 */
private suspend fun unggah(lampiran: LampiranOutbox?): String? {
    if (lampiran == null) return null
    val unggahLampiran = Outbox.unggahLampiran ?: error("Outbox.unggahLampiran belum dipasang")
    val bytes = withContext(Dispatchers.IO) { lampiran.berkasLokal.readBytes() }
    return unggahLampiran(lampiran.bucket, lampiran.tujuan, bytes)
}
