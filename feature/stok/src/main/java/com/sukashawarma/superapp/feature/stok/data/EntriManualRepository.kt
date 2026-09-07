package com.sukashawarma.superapp.feature.stok.data

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.sukashawarma.superapp.core.storage.StorageUtil
import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.SupabaseClient
import java.util.UUID

/**
 * Entri manual ledger dan pelaporan waste — cermin `ManualEntryForm.tsx` dan
 * `WasteModal.tsx` di web.
 *
 * Keduanya satu berkas karena di web pun satu formulir: penyesuaian dan transfer
 * keluar masuk langsung ke `ledger_stok`, sedangkan waste TIDAK — waste harus
 * melewati `stok_waste_reports` dan persetujuan, lalu trigger
 * `process_waste_report_approval` yang menuliskannya ke ledger. Menulis waste
 * langsung ke ledger dari sini akan memotong seluruh rantai persetujuan itu.
 *
 * Tidak ada service-role di sini dan memang tidak perlu: `waste_reports_insert`
 * berbunyi `TO authenticated WITH CHECK (outlet_id IN accessible_outlet_ids())`,
 * jadi JWT pengguna sudah cukup. Web memakai service-role hanya karena seluruh
 * Server Action-nya memang begitu.
 */
object EntriManualRepository {

    /** Bucket ini publik (lihat migrasi pembuatannya), jadi URL-nya bisa disimpan apa adanya. */
    private const val BUCKET_WASTE = "waste_evidence"

    /** Alasan waste — daftar yang sama persis dengan dropdown di `WasteModal.tsx`. */
    val ALASAN_WASTE = listOf(
        "Basi / Expired",
        "Jatuh / Tumpah",
        "Gosong / Rusak Masak",
        "Kualitas Buruk (dari supplier)",
        "Lainnya",
    )

    /**
     * Mengunggah foto bukti dan mengembalikan URL publiknya.
     *
     * Kolom `photo_url` menyimpan URL penuh, bukan path — itu yang dibaca layar
     * persetujuan waste, dan mengisinya dengan path akan membuat fotonya gagal
     * tampil di web maupun native.
     */
    suspend fun unggahBuktiWaste(outletId: String, bahanBakuId: String, jpeg: ByteArray): String {
        val path = "$outletId/$bahanBakuId/${System.currentTimeMillis()}-${UUID.randomUUID()}.jpg"
        StorageUtil.uploadJpeg(BUCKET_WASTE, path, jpeg)
        return "${SupabaseClient.BASE_URL}storage/v1/object/public/$BUCKET_WASTE/$path"
    }

    /**
     * Melaporkan waste. Statusnya selalu `PENDING`; yang memotong stok adalah
     * trigger saat laporan disetujui, bukan pemanggilan ini.
     *
     * [qtyBesar] WAJIB sudah dalam satuan besar bahan. Trigger persetujuan menulis
     * `-NEW.qty` ke ledger apa adanya, jadi mengirim angka dalam gram untuk bahan
     * ber-satuan kilogram akan memotong stok ribuan kali lipat.
     */
    suspend fun laporWaste(
        outletId: String,
        bahanBakuId: String,
        qtyBesar: Double,
        alasan: String,
        photoUrl: String,
        dilaporkanOleh: String,
    ) {
        Postgrest.insert(
            "stok_waste_reports",
            JsonArray().apply {
                add(
                    JsonObject().apply {
                        addProperty("outlet_id", outletId)
                        addProperty("bahan_baku_id", bahanBakuId)
                        addProperty("qty", qtyBesar)
                        addProperty("reason", alasan)
                        addProperty("photo_url", photoUrl)
                        addProperty("status", "PENDING")
                        addProperty("reported_by", dilaporkanOleh)
                    }
                )
            },
            returning = false,
        )
    }
}
