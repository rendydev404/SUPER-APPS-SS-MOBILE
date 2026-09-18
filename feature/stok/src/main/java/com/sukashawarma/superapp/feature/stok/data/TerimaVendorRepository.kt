package com.sukashawarma.superapp.feature.stok.data

import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.sukashawarma.superapp.core.storage.StorageUtil
import com.sukashawarma.superapp.data.remote.Postgrest
import com.sukashawarma.superapp.data.remote.SupabaseClient
import com.sukashawarma.superapp.data.remote.optBoolean
import com.sukashawarma.superapp.data.remote.optDouble
import com.sukashawarma.superapp.data.remote.optString
import com.sukashawarma.superapp.feature.stok.domain.FaktorBahan

data class BahanDropShip(
    val id: String,
    val nama: String,
    val satuan: String,
    val satuanTengah: String? = null,
    val satuanKecil: String? = null,
    override val faktorTengah: Double? = null,
    override val faktorTampilan: Double? = null,
    val dropShip: Boolean = true,
) : FaktorBahan

data class InfoVendorDropShip(
    val supplierId: String,
    val supplierNama: String,
    val hargaSnapshot: Double,
    val rataPakaiHarian: Double?,
)

data class CatatanTerimaVendor(
    val id: String,
    val tanggalTerima: String, // 'YYYY-MM-DD'
    val bahanNama: String,
    val satuan: String,
    val supplierNama: String,
    val qty: Double,
    val hargaSnapshot: Double,
    val status: String, // 'dicatat', 'disahkan', 'ditolak'
    val dicatatAt: String,
) {
    val totalNilai: Double get() = qty * hargaSnapshot
}

object TerimaVendorRepository {

    /**
     * Mengambil master bahan baku drop-ship yang aktif (misalnya Sayur lettuce).
     * Cermin `bahanDropShip(bahanBaku)` di web.
     */
    suspend fun daftarBahanDropShip(): List<BahanDropShip> {
        val res = Postgrest.select(
            "bahan_baku",
            listOf(
                "select" to "id,nama,satuan,satuan_tengah,satuan_kecil,faktor_tengah,faktor_tampilan,drop_ship",
                "is_active" to "eq.true",
                "drop_ship" to "eq.true",
                "order" to "nama.asc",
            ),
        )
        return res.mapNotNull { el ->
            val o = el.asJsonObject
            val id = o.optString("id") ?: return@mapNotNull null
            val nama = o.optString("nama") ?: return@mapNotNull null
            val satuan = o.optString("satuan") ?: "kg"
            BahanDropShip(
                id = id,
                nama = nama,
                satuan = satuan,
                satuanTengah = o.optString("satuan_tengah"),
                satuanKecil = o.optString("satuan_kecil"),
                faktorTengah = o.optDouble("faktor_tengah"),
                faktorTampilan = o.optDouble("faktor_tampilan"),
                dropShip = o.optBoolean("drop_ship", true),
            )
        }
    }

    /**
     * Memanggil RPC `info_terima_vendor(p_bahan_baku_id uuid)`.
     * Mengembalikan vendor pengantar aktif, harga katalog saat ini, dan rata pemakaian 7 hari outlet.
     */
    suspend fun infoTerimaVendor(bahanBakuId: String): List<InfoVendorDropShip> {
        val res = Postgrest.rpc(
            "info_terima_vendor",
            JsonObject().apply {
                addProperty("p_bahan_baku_id", bahanBakuId)
            },
        )
        if (!res.isJsonArray) return emptyList()
        return res.asJsonArray.mapNotNull { el ->
            val o = el.asJsonObject
            val supplierId = o.optString("supplier_id") ?: return@mapNotNull null
            val supplierNama = o.optString("supplier_nama") ?: return@mapNotNull null
            val harga = o.optDouble("harga_snapshot") ?: 0.0
            val rata = o.optDouble("rata_pakai_harian")
            InfoVendorDropShip(
                supplierId = supplierId,
                supplierNama = supplierNama,
                hargaSnapshot = harga,
                rataPakaiHarian = rata,
            )
        }
    }

    /**
     * Memanggil RPC `daftar_terima_vendor_saya(p_dari date, p_sampai date)`.
     * Mengembalikan riwayat penerimaan di outlet staf yang sedang login.
     */
    suspend fun riwayatTerimaVendorSaya(dari: String, sampai: String): List<CatatanTerimaVendor> {
        val res = Postgrest.rpc(
            "daftar_terima_vendor_saya",
            JsonObject().apply {
                addProperty("p_dari", dari)
                addProperty("p_sampai", sampai)
            },
        )
        if (!res.isJsonArray) return emptyList()
        return res.asJsonArray.mapNotNull { el ->
            val o = el.asJsonObject
            val id = o.optString("id") ?: return@mapNotNull null
            CatatanTerimaVendor(
                id = id,
                tanggalTerima = o.optString("tanggal_terima") ?: "",
                bahanNama = o.optString("bahan_nama") ?: "",
                satuan = o.optString("satuan") ?: "",
                supplierNama = o.optString("supplier_nama") ?: "",
                qty = o.optDouble("qty") ?: 0.0,
                hargaSnapshot = o.optDouble("harga_snapshot") ?: 0.0,
                status = o.optString("status") ?: "dicatat",
                dicatatAt = o.optString("dicatat_at") ?: "",
            )
        }
    }

    /**
     * Mencatat penerimaan langsung vendor ke outlet lewat RPC `catat_terima_vendor`.
     * Otomatis menambah saldo ledger stok outlet saat itu juga.
     */
    suspend fun catatTerimaVendor(
        bahanBakuId: String,
        supplierId: String,
        qtyBesar: Double,
        tanggal: String,
        catatan: String?,
        fotoUrl: String?,
    ): String {
        val body = JsonObject().apply {
            addProperty("p_bahan_baku_id", bahanBakuId)
            addProperty("p_supplier_id", supplierId)
            addProperty("p_qty", qtyBesar)
            addProperty("p_tanggal", tanggal)
            if (!catatan.isNullOrBlank()) addProperty("p_catatan", catatan.trim())
            else add("p_catatan", JsonNull.INSTANCE)
            if (!fotoUrl.isNullOrBlank()) addProperty("p_foto_url", fotoUrl.trim())
            else add("p_foto_url", JsonNull.INSTANCE)
        }
        val res = Postgrest.rpc("catat_terima_vendor", body)
        return if (res.isJsonPrimitive) res.asString else res.toString()
    }

    /**
     * Mengoreksi kuantitas barang yang telah dicatat selama statusnya masih 'dicatat'.
     * Trigger database otomatis menyesuaikan delta selisih stok outlet secara idempoten.
     */
    suspend fun koreksiTerimaVendor(id: String, qtyBaru: Double) {
        val body = JsonObject().apply {
            addProperty("p_id", id)
            addProperty("p_qty", qtyBaru)
        }
        Postgrest.rpc("koreksi_terima_vendor", body)
    }

    /**
     * Unggah foto bukti penerimaan fisik / timbangan ke bucket 'drop-ship'.
     */
    suspend fun unggahFotoBukti(outletId: String, jpegBytes: ByteArray): String {
        val path = "terima/$outletId/${System.currentTimeMillis()}.jpg"
        StorageUtil.uploadJpeg("drop-ship", path, jpegBytes)
        return "${SupabaseClient.BASE_URL}storage/v1/object/public/drop-ship/$path"
    }
}
