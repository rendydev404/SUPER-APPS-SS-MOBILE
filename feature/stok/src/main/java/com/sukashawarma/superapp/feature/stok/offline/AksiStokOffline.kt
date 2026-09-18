package com.sukashawarma.superapp.feature.stok.offline

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.abaikanDuplikat
import com.sukashawarma.superapp.data.remote.SupabaseClient

/**
 * Aksi stok yang boleh menunggu di antrean perangkat saat internet mati.
 *
 * Yang TIDAK ada di sini juga disengaja:
 * - Membuat opname baru: butuh id header dari server sebelum itemnya bisa ditulis, dan id itu
 *   tidak ada saat offline. Melanjutkan opname yang drafnya sudah ada tetap bisa.
 * - Persetujuan waste, permintaan, mutasi, retur: dua approver yang menyetujui hal yang sama
 *   saat sama-sama offline akan sama-sama "berhasil" di layar masing-masing.
 * - Penerimaan PO dan terima vendor: memotong dan menambah stok dengan hitungan kumulatif
 *   yang dihitung server dari keadaan saat itu.
 */
object WasteOffline {
    const val JENIS = "waste"

    /** Bucket ini publik (lihat migrasi pembuatannya), jadi URL-nya bisa disimpan apa adanya. */
    const val BUCKET = "waste_evidence"

    fun payload(
        outletId: String,
        bahanBakuId: String,
        qtyBesar: Double,
        alasan: String,
        dilaporkanOleh: String,
    ): JsonObject = JsonObject().apply {
        addProperty("outlet_id", outletId)
        addProperty("bahan_baku_id", bahanBakuId)
        addProperty("qty", qtyBesar)
        addProperty("reason", alasan)
        addProperty("status", "PENDING")
        addProperty("reported_by", dilaporkanOleh)
    }

    /** Path tujuan foto. Deterministik terhadap [clientOpId] supaya percobaan ulang menimpa
     *  berkas yang sama alih-alih menumpuk foto yatim di storage. */
    fun tujuanFoto(outletId: String, clientOpId: String) = "$outletId/$clientOpId.jpg"

    /**
     * [lampiranUrl] berbentuk "bucket/path" dari StorageUtil. Kolom `photo_url` menyimpan URL
     * penuh, bukan path — mengisinya dengan path membuat fotonya gagal tampil di layar
     * persetujuan, baik di web maupun native.
     */
    suspend fun kirim(clientOpId: String, payload: JsonObject, lampiranUrl: String?) {
        val body = payload.deepCopy().apply {
            addProperty("client_op_id", clientOpId)
            if (lampiranUrl != null) {
                addProperty("photo_url", "${SupabaseClient.BASE_URL}storage/v1/object/public/$lampiranUrl")
            }
        }
        abaikanDuplikat { Postgrest.insert("stok_waste_reports", JsonArray().apply { add(body) }, returning = false) }
    }
}

object PermintaanOffline {
    const val JENIS = "permintaan"

    fun payload(
        outletId: String,
        dibuatOleh: String,
        items: List<Pair<String, Double>>,
    ): JsonObject = JsonObject().apply {
        addProperty("p_outlet_id", outletId)
        add(
            "p_items",
            JsonArray().apply {
                items.forEach { (bahanBakuId, qty) ->
                    add(
                        JsonObject().apply {
                            addProperty("bahan_baku_id", bahanBakuId)
                            addProperty("qty_diminta", qty)
                        }
                    )
                }
            }
        )
        addProperty("p_dibuat_oleh", dibuatOleh)
        add("p_target_metadata", JsonArray())
    }

    suspend fun kirim(clientOpId: String, payload: JsonObject, lampiranUrl: String?) {
        val body = payload.deepCopy().apply { addProperty("p_client_op_id", clientOpId) }
        Postgrest.rpc("buat_permintaan_svc", body)
    }
}
