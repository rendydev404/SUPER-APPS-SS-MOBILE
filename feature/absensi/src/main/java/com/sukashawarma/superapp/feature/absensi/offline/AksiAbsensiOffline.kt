package com.sukashawarma.superapp.feature.absensi.offline

import com.sukashawarma.superapp.data.remote.SupabaseClient
import com.google.gson.JsonObject
import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.abaikanDuplikat

/**
 * Aksi absensi yang boleh menunggu di antrean perangkat saat internet mati.
 *
 * Keduanya dipilih karena memenuhi dua syarat sekaligus: server tidak perlu menghitung apa
 * pun dari keadaan terkini untuk menerimanya (tidak seperti persetujuan, yang bisa bentrok
 * dengan approver lain), dan keduanya punya kunci idempotensi sehingga kiriman ulang tidak
 * jadi kasbon atau cuti dobel — lihat `plan/offline-client-op-id.sql`.
 *
 * Fungsi kirim di sini adalah satu-satunya tempat pengiriman aksi ini ditulis: dipakai
 * jalur online lewat `kirimAtauAntre`, dan didaftarkan ke `Outbox` untuk jalur antrean.
 */
object KasbonOffline {
    const val JENIS = "kasbon"

    fun payload(
        staffId: String,
        amount: Double,
        installmentMonths: Int,
        reason: String,
    ): JsonObject = JsonObject().apply {
        addProperty("staff_id", staffId)
        addProperty("amount", amount)
        addProperty("remaining", amount)
        addProperty("reason", reason)
        addProperty("status", "pending")
        addProperty("installment_months", installmentMonths)
    }

    suspend fun kirim(clientOpId: String, payload: JsonObject, lampiranUrl: String?) {
        // deepCopy: payload yang sama dipakai lagi kalau kiriman ini gagal dan diulang,
        // jadi jangan disuntik in-place.
        val body = payload.deepCopy().apply { addProperty("client_op_id", clientOpId) }
        abaikanDuplikat { Postgrest.insert("cash_advances", body, returning = false) }
    }
}

object CutiOffline {
    const val JENIS = "cuti"

    /** Bucket yang sama dengan web (apps/absensi features/cuti/api.ts): publik, dan
     *  `attachment_url` menyimpan URL publik LENGKAP — bentuk yang dibaca dashboard HR. */
    const val BUCKET = "hr-attachments"

    /** Path di dalam bucket. Deterministik per pengajuan (clientOpId) supaya unggahan
     *  ulang dari antrean menimpa berkas yang sama, bukan menumpuk salinan. */
    fun tujuanBukti(staffId: String, clientOpId: String) = "${clientOpId}_$staffId.jpg"

    fun payload(
        staffId: String,
        leaveType: String,
        startDate: String,
        endDate: String,
        days: Int,
        reason: String,
    ): JsonObject = JsonObject().apply {
        addProperty("staff_id", staffId)
        addProperty("leave_type", leaveType)
        addProperty("start_date", startDate)
        addProperty("end_date", endDate)
        addProperty("days", days)
        addProperty("reason", reason)
        addProperty("status", "pending")
    }

    suspend fun kirim(clientOpId: String, payload: JsonObject, lampiranUrl: String?) {
        val body = payload.deepCopy().apply {
            addProperty("client_op_id", clientOpId)
            // Pengunggah lampiran mengembalikan "bucket/path"; web menyimpan URL publik.
            lampiranUrl?.let { addProperty("attachment_url", "${SupabaseClient.BASE_URL}storage/v1/object/public/$it") }
        }
        abaikanDuplikat { Postgrest.insert("leave_requests", body, returning = false) }
    }
}
