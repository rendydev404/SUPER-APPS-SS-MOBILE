package com.sukashawarma.superapp.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Satu aksi tulis yang terjadi saat perangkat offline dan belum sampai ke server.
 *
 * Generalisasi dari [PendingAttendanceEntity] dan [PendingOpnameFinalizeEntity] yang lebih
 * dulu ada: dua tabel itu membuktikan polanya bekerja, tetapi setiap fitur baru berarti satu
 * tabel + satu DAO + satu migrasi lagi. Di sini payload-nya JSON, jadi menambah aksi cukup
 * mendaftarkan penangannya di Outbox.
 *
 * [id] adalah UUID yang dibuat klien dan dikirim ke server sebagai `client_op_id`. Itu yang
 * membuat percobaan ulang aman: kiriman yang sebenarnya sudah masuk sebelum koneksi putus
 * tidak akan tercatat dua kali. Aksi yang belum punya pelindung semacam ini di server TIDAK
 * boleh diantre (lihat matriks di rencana) — lebih baik ditolak di depan daripada menjadi
 * baris ganda yang harus dibereskan manual.
 *
 * [tsClientIso] adalah jam kejadian menurut perangkat, bukan jam tibanya di server. Untuk
 * absensi ini yang menentukan seseorang dicatat masuk jam berapa; server tetap memutuskan
 * sah atau tidaknya, tapi tidak boleh memakai waktu tiba sebagai waktu absen.
 *
 * [lampiranPath] menunjuk file lokal (mis. selfie) yang harus lebih dulu diunggah sebelum
 * payload dikirim. Tujuannya deterministik — lihat [lampiranTujuan] — supaya percobaan ulang
 * menimpa berkas yang sama, bukan menumpuk berkas yatim di storage.
 */
@Entity(tableName = "outbox", indices = [Index("status")])
data class OutboxEntity(
    @PrimaryKey val id: String,
    /** Nama aksi, dipakai Outbox untuk memilih penangan. Mis. "absensi", "waste", "kasbon". */
    val jenis: String,
    val payload: String,
    val tsClientIso: String,
    val createdAtMs: Long,
    val outletId: String? = null,
    val dibuatOleh: String? = null,
    val lampiranPath: String? = null,
    val lampiranBucket: String? = null,
    val lampiranTujuan: String? = null,
    val attemptCount: Int = 0,
    val lastError: String? = null,
    val status: String = STATUS_MENUNGGU,
) {
    companion object {
        const val STATUS_MENUNGGU = "MENUNGGU"

        /**
         * Server menolak aksi ini dengan alasan yang tidak akan berubah kalau diulang
         * (mis. plafon budget sudah terlewati), atau percobaan sudah habis. Barisnya
         * SENGAJA tidak dihapus: user harus diberi tahu kerjanya tidak masuk, bukan
         * dibiarkan mengira sudah tersimpan.
         */
        const val STATUS_GAGAL_PERMANEN = "GAGAL_PERMANEN"

        /** Di atas ini aksi dianggap tidak akan pernah berhasil sendiri. */
        const val BATAS_PERCOBAAN = 8
    }
}
